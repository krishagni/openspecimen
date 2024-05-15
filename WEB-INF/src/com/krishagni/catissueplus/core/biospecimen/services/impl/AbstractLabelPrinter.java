package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.TransactionAwareInterceptor;
import com.krishagni.catissueplus.core.common.TransactionEventListener;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobSavedEvent;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.catissueplus.core.common.domain.PrintItem;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.LabelPrinter;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public abstract class AbstractLabelPrinter<T> implements LabelPrinter<T>, TransactionEventListener {
	//
	// format: <entity_type>_<yyyyMMddHHmm>_<unique_os_run_num>_<copy>.txt
	// E.g. specimen_201604040807_1_1.txt, specimen_201604040807_1_2.txt, visit_201604040807_1_1.txt etc
	//
	private static final LogUtil logger = LogUtil.getLogger(AbstractLabelPrinter.class);

	private static final String LABEL_FILENAME_FMT = "%s_%s_%d_%d.%s";

	private static final String TSTAMP_FMT = "yyyyMMddHHmm";

	private final AtomicInteger uniqueNum = new AtomicInteger();

	protected DaoFactory daoFactory;

	protected LabelTmplTokenRegistrar printLabelTokensRegistrar;

	private TransactionAwareInterceptor transactionAwareInterceptor;

	private LabelPrintFileSpooler labelPrintFilesSpooler;

	private final ThreadLocal<Set<Long>> jobIds = ThreadLocal.withInitial(LinkedHashSet::new);

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setPrintLabelTokensRegistrar(LabelTmplTokenRegistrar printLabelTokensRegistrar) {
		this.printLabelTokensRegistrar = printLabelTokensRegistrar;
	}

	public void setTransactionAwareInterceptor(TransactionAwareInterceptor transactionAwareInterceptor) {
		this.transactionAwareInterceptor = transactionAwareInterceptor;
		if (transactionAwareInterceptor != null) {
			transactionAwareInterceptor.addListener(this);
		}
	}

	public void setLabelPrintFilesSpooler(LabelPrintFileSpooler labelPrintFilesSpooler) {
		this.labelPrintFilesSpooler = labelPrintFilesSpooler;
	}

	@Override
	public List<LabelTmplToken> getTokens() {
		return printLabelTokensRegistrar.getTokens();
	}

	@Override
	public LabelPrintJob print(List<PrintItem<T>> printItems) {
		try {
			List<? extends LabelPrintRule> rules = PrintRuleTxnCache.getInstance().getRules(getObjectType());
			if (rules == null) {
				return null;
			}

			String ipAddr = AuthUtil.getRemoteAddr();
			User currentUser = AuthUtil.getCurrentUser();
			String userPrinter = null;
			if (currentUser.getDefaultPrinter() != null) {
				userPrinter = currentUser.getDefaultPrinter().getValue();
			}

			LabelPrintJob job = new LabelPrintJob();
			job.setSubmissionDate(Calendar.getInstance().getTime());
			job.setSubmittedBy(currentUser);
			job.setItemType(getItemType());

			ObjectMapper mapper = new ObjectMapper();
			for (PrintItem<T> printItem : printItems) {
				boolean found = false;
				T obj = printItem.getObject();
				for (LabelPrintRule rule : rules) {
					if (!isApplicableFor(rule, obj, currentUser, ipAddr)) {
						continue;
					}

					Map<String, String> labelDataItems = rule.getDataItems(printItem);

					LabelPrintJobItem item = new LabelPrintJobItem();
					item.setJob(job);
					item.setPrinterName(userPrinter != null ? userPrinter : rule.getPrinterName());
					item.setItemLabel(getItemLabel(obj));
					item.setItemId(getItemId(obj));
					item.setCopies(printItem.getCopies());
					item.setStatus(LabelPrintJobItem.Status.QUEUED);
					item.setLabelType(rule.getLabelType());
					item.setLabelDesign(rule.getLabelDesign());
					item.setData(mapper.writeValueAsString(labelDataItems));
					item.setObject(obj);
					item.setDataItems(labelDataItems);
					item.setContent(mapper.writeValueAsString(generateCmdFileContent(item, rule, labelDataItems)));
					item.setCreateFile(rule.getCreateFile());

					job.getItems().add(item);
					found = true;
					break;
				}

				if (!found) {
					logger.warn("No print rule matched for the order item: " + getItemLabel(obj));
				}
			}

			if (job.getItems().isEmpty()) {
				return null;
			}

			daoFactory.getLabelPrintJobDao().saveOrUpdate(job, true);
			EventPublisher.getInstance().publish(new LabelPrintJobSavedEvent(job));
			jobIds.get().add(job.getId());
			return job;
		} catch (Exception e) {
			logger.error("Error printing distribution labels", e);
			throw OpenSpecimenException.serverError(e);
		}
	}

	@Override
	public void onFinishTransaction() {
		Set<Long> printJobIds = jobIds.get();
		jobIds.remove();
		printJobIds.forEach(labelPrintFilesSpooler::queueJob);
	}

	protected abstract boolean isApplicableFor(LabelPrintRule rule, T obj, User user, String ipAddr);

	protected abstract String getObjectType();

	protected abstract String getItemType();

	protected abstract String getItemLabel(T obj);

	protected abstract Long getItemId(T obj);

	private List<Map<String, Object>> generateCmdFileContent(LabelPrintJobItem jobItem, LabelPrintRule rule, Map<String, String> dataItems) {
		if (StringUtils.isBlank(rule.getCmdFilesDir()) || rule.getCmdFilesDir().trim().equals("*")) {
			return Collections.emptyList();
		}

		try {
			String content = switch (rule.getCmdFileFmt()) {
				case CSV -> getCommaSeparatedValueFields(dataItems, rule.getLineEnding());
				case KEY_VALUE -> getKeyValueFields(dataItems, false, rule.getLineEnding());
				case KEY_Q_VALUE -> getKeyValueFields(dataItems, true, rule.getLineEnding());
			};

			return generateCmdFileContent(jobItem, rule, content);
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	private String getCommaSeparatedValueFields(Map<String, String> dataItems, String lineEnding) {
		return Utility.stringListToCsv(dataItems.values(), true, ',', getLineEnding(lineEnding));
	}

	private String getKeyValueFields(Map<String, String> dataItems, boolean quotedValues, String lineEnding) {
		String fmt = "%s=%s" + getLineEnding(lineEnding);
		StringBuilder content = new StringBuilder();

		Function<String, String> transformFn = quotedValues ? Utility::getQuotedString : (v) -> v;
		dataItems.forEach((key, value) -> content.append(String.format(fmt, key, transformFn.apply(value))));
		if (!dataItems.isEmpty() && "LF".equals(lineEnding)) {
			content.deleteCharAt(content.length() - 1);
		}

		return content.toString();
	}

	private String getLineEnding(String lineEnding) {
		return "CRLF".equals(lineEnding) ? "\r\n" : "\n";
	}

	private List<Map<String, Object>> generateCmdFileContent(LabelPrintJobItem item, LabelPrintRule rule, String content) {
		String tstamp = new SimpleDateFormat(TSTAMP_FMT).format(item.getJob().getSubmissionDate());
		int labelCount = uniqueNum.incrementAndGet();

		List<Map<String, Object>> result = new ArrayList<>();
		for (int i = 0; i < item.getCopies(); ++i) {
			String filename = String.format(LABEL_FILENAME_FMT, item.getJob().getItemType(), tstamp, labelCount, (i + 1), rule.getFileExtn());

			Map<String, Object> label = new HashMap<>();
			label.put("dir", rule.getCmdFilesDir());
			label.put("file", filename);
			label.put("content", content);
			result.add(label);
		}

		return result;
	}
}
