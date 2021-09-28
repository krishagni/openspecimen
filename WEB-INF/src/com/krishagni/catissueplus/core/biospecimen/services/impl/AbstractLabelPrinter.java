package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.administrative.domain.UserGroupSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.TransactionAwareInterceptor;
import com.krishagni.catissueplus.core.common.TransactionEventListener;
import com.krishagni.catissueplus.core.common.domain.LabelPrintFileItem;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.catissueplus.core.common.domain.PrintItem;
import com.krishagni.catissueplus.core.common.domain.PrintRuleConfig;
import com.krishagni.catissueplus.core.common.domain.PrintRuleEvent;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EventCode;
import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;
import com.krishagni.catissueplus.core.common.repository.PrintRuleConfigsListCriteria;
import com.krishagni.catissueplus.core.common.service.LabelPrinter;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public abstract class AbstractLabelPrinter<T> implements LabelPrinter<T>, TransactionEventListener {
	//
	// format: <entity_type>_<yyyyMMddHHmm>_<unique_os_run_num>_<copy>.txt
	// E.g. specimen_201604040807_1_1.txt, specimen_201604040807_1_2.txt, visit_201604040807_1_1.txt etc
	//
	private static final Log logger = LogFactory.getLog(AbstractLabelPrinter.class);

	private static final String LABEL_FILENAME_FMT = "%s_%s_%d_%d.%s";

	private static final String TSTAMP_FMT = "yyyyMMddHHmm";

	private AtomicInteger uniqueNum = new AtomicInteger();

	protected List<? extends LabelPrintRule> rules = null;

	protected DaoFactory daoFactory;

	protected LabelTmplTokenRegistrar printLabelTokensRegistrar;

	private TransactionAwareInterceptor transactionAwareInterceptor;

	private LabelPrintFileSpooler labelPrintFilesSpooler;

	private ThreadLocal<Set<Long>> jobIds = new ThreadLocal<Set<Long>>() {
		@Override
		protected Set<Long> initialValue() {
			return new LinkedHashSet<>();
		}
	};

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
			if (rules == null) {
				synchronized (this) {
					if (rules == null) {
						loadRulesFromDb();
						if (rules == null) {
							return null;
						}
					}
				}
			}

			String ipAddr = AuthUtil.getRemoteAddr();
			User currentUser = AuthUtil.getCurrentUser();

			LabelPrintJob job = new LabelPrintJob();
			job.setSubmissionDate(Calendar.getInstance().getTime());
			job.setSubmittedBy(currentUser);
			job.setItemType(getItemType());

			List<Map<String, Object>> labelDataList = new ArrayList<>();
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
					item.setPrinterName(rule.getPrinterName());
					item.setItemLabel(getItemLabel(obj));
					item.setItemId(getItemId(obj));
					item.setCopies(printItem.getCopies());
					item.setStatus(LabelPrintJobItem.Status.QUEUED);
					item.setLabelType(rule.getLabelType());
					item.setData(new ObjectMapper().writeValueAsString(labelDataItems));
					item.setDataItems(labelDataItems);

					job.getItems().add(item);
					labelDataList.add(makeLabelData(item, rule, labelDataItems));

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
			generateCmdFiles(job, labelDataList);
			return job;
		} catch (Exception e) {
			logger.error("Error printing distribution labels", e);
			throw OpenSpecimenException.serverError(e);
		}
	}

	public void onApplicationEvent(OpenSpecimenEvent event) {
		EventCode code = event.getEventCode();
		if (code == PrintRuleEvent.CREATED || code == PrintRuleEvent.UPDATED || code == PrintRuleEvent.DELETED) {
			PrintRuleConfig ruleCfg = (PrintRuleConfig) event.getEventData();
			if (ruleCfg.getObjectType().equals(getObjectType())) {
				loadRulesFromDb();
			}
		} else if (event instanceof UserGroupSavedEvent && rules != null) {
			UserGroup group = ((UserGroupSavedEvent) event).getEventData();
			boolean cacheUpdated = false;

			for (LabelPrintRule rule : rules) {
				int idx = rule.getUserGroups().indexOf(group);
				if (idx == -1) {
					continue;
				}

				rule.getUserGroups().remove(idx);
				if (!group.isDeleted()) {
					rule.getUserGroups().add(group);
				}

				rule.recomputeEffectiveUsers();
				cacheUpdated = true;
			}

			if (cacheUpdated) {
				PrintRuleConfig ruleCfg = new PrintRuleConfig();
				ruleCfg.setObjectType(getObjectType());
				EventPublisher.getInstance().publish(PrintRuleEvent.CACHE_UPDATED, ruleCfg);
			}
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

	@PlusTransactional
	protected void loadRulesFromDb() {
		try {
			logger.info("Loading print rules from database for: " + getObjectType());
			rules = daoFactory.getPrintRuleConfigDao()
				.getPrintRules(new PrintRuleConfigsListCriteria().objectType(getObjectType()))
				.stream().map(PrintRuleConfig::getRule)
				.collect(Collectors.toList());
		} catch (Exception e) {
			logger.error("Error loading print rules for: " + getObjectType(), e);
			throw new RuntimeException("Error loading print rules for: " + getObjectType(), e);
		}
	}

	protected Map<String, Object> makeLabelData(LabelPrintJobItem item, LabelPrintRule rule, Map<String, String> dataItems) {
		Map<String, Object> labelData = new HashMap<>();
		labelData.put("jobItem", item);
		labelData.put("rule", rule);
		labelData.put("dataItems", dataItems);
		return labelData;
	}

	@SuppressWarnings("unchecked")
	protected void generateCmdFiles(LabelPrintJob job, List<Map<String, Object>> labelDataList) {
		jobIds.get().add(job.getId());

		List<Map<String, Object>> fileItems = new ArrayList<>();
		for (Map<String, Object> labelData : labelDataList) {
			generateCmdFileContent(
				(LabelPrintJobItem) labelData.get("jobItem"),
				(LabelPrintRule) labelData.get("rule"),
				(Map<String, String>) labelData.get("dataItems"),
				fileItems
			);

			if (fileItems.size() >= 25) {
				writeToDb(job, fileItems);
				fileItems.clear();
			}
		}

		if (!fileItems.isEmpty()) {
			writeToDb(job, fileItems);
		}
	}

	private void writeToDb(LabelPrintJob job, List<Map<String, Object>> items) {
		try {
			LabelPrintFileItem fileItem = new LabelPrintFileItem();
			fileItem.setJob(job);
			fileItem.setContent(new ObjectMapper().writeValueAsString(items));
			fileItem.setCreator(AuthUtil.getCurrentUser());
			fileItem.setCreationTime(Calendar.getInstance().getTime());
			daoFactory.getLabelPrintJobDao().savePrintFileItem(fileItem);
		} catch (Exception e) {
			throw new RuntimeException("Error spooling labels", e);
		}
	}

	private void generateCmdFileContent(LabelPrintJobItem jobItem, LabelPrintRule rule, Map<String, String> dataItems, List<Map<String, Object>> out) {
		if (StringUtils.isBlank(rule.getCmdFilesDir()) || rule.getCmdFilesDir().trim().equals("*")) {
			return;
		}

		try {
			String content = null;
			switch (rule.getCmdFileFmt()) {
				case CSV:
					content = getCommaSeparatedValueFields(dataItems, rule.getLineEnding());
					break;

				case KEY_VALUE:
					content = getKeyValueFields(dataItems, false, rule.getLineEnding());
					break;

				case KEY_Q_VALUE:
					content = getKeyValueFields(dataItems, true, rule.getLineEnding());
					break;
			}

			generateCmdFileContent(jobItem, rule, content, out);
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

	private void generateCmdFileContent(LabelPrintJobItem item, LabelPrintRule rule, String content, List<Map<String, Object>> out)
	throws IOException {
		String tstamp = new SimpleDateFormat(TSTAMP_FMT).format(item.getJob().getSubmissionDate());
		int labelCount = uniqueNum.incrementAndGet();

		for (int i = 0; i < item.getCopies(); ++i) {
			String filename = String.format(LABEL_FILENAME_FMT, item.getJob().getItemType(), tstamp, labelCount, (i + 1), rule.getFileExtn());

			Map<String, Object> label = new HashMap<>();
			label.put("file", new File(rule.getCmdFilesDir(), filename).getAbsolutePath());
			label.put("content", content);
			out.add(label);
		}
	}
}
