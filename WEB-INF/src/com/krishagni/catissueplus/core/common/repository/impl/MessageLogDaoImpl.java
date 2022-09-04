package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.domain.MessageLog;
import com.krishagni.catissueplus.core.common.events.MessageLogCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.MessageLogDao;

public class MessageLogDaoImpl extends AbstractDao<MessageLog> implements MessageLogDao {

	@Override
	public Class<MessageLog> getType() {
		return MessageLog.class;
	}

	@Override
	public List<MessageLog> getMessages(MessageLogCriteria crit) {
		Criteria<MessageLog> query = getQuery(crit);
		return query.orderBy(query.asc("log.id"))
			.list(crit.startAt(), crit.maxResults());
	}

	@Override
	public long getMessagesCount(MessageLogCriteria crit) {
		return getQuery(crit).getCount("log.id");
	}

	@Override
	public int deleteOldMessages(Date olderThanDt) {
		return createNamedQuery(DELETE_OLD_MSGS)
			.setParameter("olderThanDt", olderThanDt)
			.executeUpdate();
	}

	private Criteria<MessageLog> getQuery(MessageLogCriteria crit) {
		Criteria<MessageLog> query = createCriteria(MessageLog.class, "log");

		if (StringUtils.isNotBlank(crit.externalApp())) {
			query.add(query.eq("log.externalApp", crit.externalApp()));
		}

		if (CollectionUtils.isNotEmpty(crit.msgTypes())) {
			query.add(query.in("log.type", crit.msgTypes()));
		}

		if (crit.fromDate() != null) {
			query.add(query.ge("log.processTime", crit.fromDate()));
		}

		if (crit.toDate() != null) {
			query.add(query.le("log.processTime", crit.toDate()));
		}

		if (crit.maxRetries() != null) {
			query.add(
				query.or(
					query.isNull("log.noOfRetries"),
					query.lt("log.noOfRetries", crit.maxRetries())
				)
			);
		}

		if (crit.processStatus() != null) {
			query.add(query.eq("log.processStatus", crit.processStatus()));
		}

		return query;
	}

	private static final String FQN = MessageLog.class.getName();

	private static final String DELETE_OLD_MSGS = FQN + ".deleteOldMessages";
}
