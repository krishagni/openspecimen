package com.krishagni.catissueplus.core.common.service.impl;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.domain.ExtAppMessage;
import com.krishagni.catissueplus.core.common.domain.MessageLog;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.MessageLogCriteria;
import com.krishagni.catissueplus.core.common.service.MessageHandler;
import com.krishagni.catissueplus.core.common.service.MessageLogService;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public class MessageLogServiceImpl implements MessageLogService {
	private static final LogUtil logger = LogUtil.getLogger(MessageLogServiceImpl.class);

	private Map<String, MessageHandler> handlers = new HashMap<>();

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public MessageLog getMessageLog(Long id) {
		AccessCtrlMgr.getInstance().ensureUserIsAdmin();

		MessageLog log = daoFactory.getMessageLogDao().getById(id);
		if (log == null) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Invalid message ID " + id);
		}

		return log;
	}

	@Override
	public void registerHandler(String extAppName, MessageHandler handler) {
		handlers.put(extAppName, handler);
	}

	@Override
	public void retryPendingMessages() {
		try {
			started();

			MessageLogCriteria crit = new MessageLogCriteria()
				.processStatus(MessageLog.ProcessStatus.PENDING)
				.toDate(Calendar.getInstance().getTime())
				.maxRetries(getMaxRetries());

			int startAt = 0, maxLogs = 50;
			boolean endOfMsgLogs = false;
			while (!endOfMsgLogs) {
				List<MessageLog> msgLogs = getLogs(crit.startAt(startAt).maxResults(maxLogs));
				endOfMsgLogs = (msgLogs.size() < maxLogs);

				for (MessageLog msgLog : msgLogs) {
					String error = null;
					String recordIds = null;
					try {
						MessageHandler handler = handlers.get(msgLog.getExternalApp());
						if (handler == null) {
							logger.error("No handler registered to process messages of external app: " + msgLog.getExternalApp());
							continue;
						}

						recordIds = handler.process(msgLog);
					} catch (Exception e) {
						logger.error("Error while processing message with log id = " + msgLog.getId(), e);
						error = Utility.getErrorMessage(e);
					} finally {
						updateStatus(msgLog, recordIds, error);
					}
				}
			}
		} finally {
			finished();
		}
	}

	@Override
	public String processMessages(String extAppName, List<ExtAppMessage> messages) {
		MessageHandler handler = handlers.get(extAppName);
		if (handler == null) {
			logger.error("No handler registered to process messages of external app: " + extAppName);
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Unrecognised external app: " + extAppName);
		}

		for (ExtAppMessage message : messages) {
			if (StringUtils.isBlank(message.getMessage())) {
				continue;
			}

			MessageLog messageLog = message.toMessageLog();
			messageLog.setExternalApp(extAppName);
			handler.process(messageLog);
		}

		return UUID.randomUUID().toString();
	}

	private int getMaxRetries() {
		return ConfigUtil.getInstance().getIntSetting("common", "max_eapp_msg_retries", 5);
	}

	private void started() {
		handlers.values().forEach(MessageHandler::onStart);
	}

	private void finished() {
		handlers.values().forEach(MessageHandler::onComplete);
	}

	@PlusTransactional
	private List<MessageLog> getLogs(MessageLogCriteria crit) {
		return daoFactory.getMessageLogDao().getMessages(crit);
	}

	@PlusTransactional
	private void updateStatus(MessageLog msgLog, String recordIds, String error) {
		if (StringUtils.isNotBlank(recordIds) && StringUtils.isBlank(error)) {
			msgLog.setRecordId(recordIds);
			msgLog.setStatus(MessageLog.Status.SUCCESS);
			msgLog.setProcessStatus(MessageLog.ProcessStatus.PROCESSED);
		}

		msgLog.setError(error);
		msgLog.incrNoOfRetries();
		msgLog.setProcessTime(Calendar.getInstance().getTime());
		daoFactory.getMessageLogDao().saveOrUpdate(msgLog);
	}
}
