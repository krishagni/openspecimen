package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenLabelPrintRule;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.PrintRuleConfig;
import com.krishagni.catissueplus.core.common.service.ChangeLogService;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public class DefaultSpecimenLabelPrinter extends AbstractLabelPrinter<Specimen> implements InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(DefaultSpecimenLabelPrinter.class);

	private ConfigurationService cfgSvc;

	private ChangeLogService changeLogSvc;

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setChangeLogSvc(ChangeLogService changeLogSvc) {
		this.changeLogSvc = changeLogSvc;
	}

	@Override
	protected boolean isApplicableFor(LabelPrintRule rule, Specimen specimen, User user, String ipAddr) {
		if (StringUtils.isBlank(specimen.getLabel()) || specimen.isMissedOrNotCollected()) {
			return false;
		}

		SpecimenLabelPrintRule spmnLabelPrintRule = (SpecimenLabelPrintRule) rule;
		return spmnLabelPrintRule.isApplicableFor(specimen, user, ipAddr);
	}

	@Override
	protected String getObjectType() {
		return "SPECIMEN";
	}

	@Override
	protected String getItemType() {
		return Specimen.getEntityName();
	}

	@Override
	protected String getItemLabel(Specimen specimen) {
		return specimen.getLabel();
	}

	@Override
	protected Long getItemId(Specimen specimen) {
		return specimen.getId();
	}

	@Override
	@PlusTransactional
	public void afterPropertiesSet()
	throws Exception {
		boolean dbMigrationDone = changeLogSvc.doesChangeLogExists(PR_MIGRATION_ID, PR_MIGRATION_AUTHOR, PR_MIGRATION_FILE);
		if (!dbMigrationDone && migrateRulesToDb()) {
			changeLogSvc.insertChangeLog(PR_MIGRATION_ID, PR_MIGRATION_AUTHOR, PR_MIGRATION_FILE);
		}

		removePrintRulesSetting();
		cfgSvc.registerChangeListener(ConfigParams.MODULE, (name, value) -> {
			if (StringUtils.isBlank(name)) {
				removePrintRulesSetting();
			}
		});
	}

	private boolean migrateRulesToDb() {
		FileDetail fileDetail = cfgSvc.getFileDetail(ConfigParams.MODULE, ConfigParams.SPECIMEN_LABEL_PRINT_RULES);
		if (fileDetail == null || fileDetail.getFileIn() == null) {
			return true;
		}

		List<SpecimenLabelPrintRule> rules = new ArrayList<>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(fileDetail.getFileIn()));

			String ruleLine = null;
			while ((ruleLine = reader.readLine()) != null) {
				SpecimenLabelPrintRule rule = parseRule(ruleLine);
				if (rule == null) {
					continue;
				}

				rules.add(rule);
				logger.info(String.format("Adding print rule: [%s]", rule));
			}

			saveToDb(rules);
			return true;
		} catch (Exception e) {
			logger.error("Error migrating print rules from file: " + fileDetail.getFilename(), e);
			return false;
		} finally {
			IOUtils.closeQuietly(fileDetail.getFileIn());
			IOUtils.closeQuietly(reader);
		}
	}

	//
	// Format of each rule
	// 	cp_short_title	visit_site	specimen_class	specimen_type
	//	user_login	ip_address	label_type	label_tokens	label_design
	//	printer_name	dir_path
	//
	private SpecimenLabelPrintRule parseRule(String ruleLine) {
		if (ruleLine.startsWith("#")) {
			return null;
		}

		String[] ruleLineFields = ruleLine.split("\\t");
		if (ruleLineFields.length < 12 || ruleLineFields.length > 13) {
			logger.error(String.format("Invalid rule [%s]. Expected variables: 12/13, Actual: [%d]", ruleLine, ruleLineFields.length));
			return null;
		}

		int idx = 0;
		SpecimenLabelPrintRule rule = new SpecimenLabelPrintRule();
		rule.setCps(getCps(ruleLineFields[idx++]));
		rule.setVisitSite(getSite(ruleLineFields[idx++]));
		idx++;
		rule.setSpecimenTypes(getSpecimenTypes(ruleLineFields[idx++]));
		rule.setUsers(getUsers(ruleLineFields[idx++]));

		if (!ruleLineFields[idx++].equals("*")) {
			rule.setIpAddressMatcher(new IpAddressMatcher(ruleLineFields[idx - 1]));
		}
		rule.setLabelType(ruleLineFields[idx++]);

		List<LabelTmplToken> tokens = new ArrayList<>();
		for (String labelToken : ruleLineFields[idx++].split(",")) {
			LabelTmplToken token = printLabelTokensRegistrar.getToken(labelToken);
			if (token == null) {
				String errorMsg = String.format("Invalid rule [%s]. Unknown token: [%s]", ruleLine, labelToken);
				throw new IllegalArgumentException(errorMsg);
			}

			tokens.add(token);
		}

		List<Pair<LabelTmplToken, List<String>>> dataTokens = new ArrayList<>();
		for (LabelTmplToken token : tokens) {
			dataTokens.add(Pair.make(token, new ArrayList<>()));
		}

		rule.setDataTokens(dataTokens);
		rule.setLabelDesign(ruleLineFields[idx++]);
		rule.setPrinterName(ruleLineFields[idx++]);
		rule.setCmdFilesDir(ruleLineFields[idx++]);

		if (!ruleLineFields[idx++].equals("*")) {
			rule.setCmdFileFmt(ruleLineFields[idx - 1]);
		}

		rule.setLineage(ruleLineFields.length > 12 ? ruleLineFields[idx++] : "*");
		return rule;
	}

	private void saveToDb(List<SpecimenLabelPrintRule> rules) {
		User systemUser = daoFactory.getUserDao().getSystemUser();
		int ruleIdx = 0;
		for (SpecimenLabelPrintRule rule : rules) {
			PrintRuleConfig ruleCfg = getPrintRuleConfig(rule, systemUser, ++ruleIdx);
			daoFactory.getPrintRuleConfigDao().saveOrUpdate(ruleCfg);
		}
	}

	private PrintRuleConfig getPrintRuleConfig(SpecimenLabelPrintRule rule, User systemUser, int ruleIdx) {
		PrintRuleConfig ruleCfg = new PrintRuleConfig();
		ruleCfg.setObjectType("SPECIMEN");
		ruleCfg.setRule(replaceWildcardsWithNull(rule));
		ruleCfg.setUpdatedBy(systemUser);
		ruleCfg.setUpdatedOn(Calendar.getInstance().getTime());
		ruleCfg.setActivityStatus("Active");
		ruleCfg.setDescription("Print rule " + ruleIdx);
		return ruleCfg;
	}

	private List<CollectionProtocol> getCps(String cpsList) {
		if (StringUtils.isBlank(cpsList) || cpsList.trim().equals("*")) {
			return Collections.emptyList();
		}

		return daoFactory.getCollectionProtocolDao().getCpsByShortTitle(Utility.csvToStringList(cpsList));
	}

	private Site getSite(String siteName) {
		if (StringUtils.isBlank(siteName) || siteName.trim().equals("*")) {
			return null;
		}

		return daoFactory.getSiteDao().getSiteByName(siteName);
	}

	private List<PermissibleValue> getSpecimenTypes(String typesList) {
		if (StringUtils.isBlank(typesList) || typesList.trim().equals("*")) {
			return Collections.emptyList();
		}

		return daoFactory.getPermissibleValueDao().getPvs("specimen_type", Utility.csvToStringList(typesList));
	}

	private List<User> getUsers(String usersList) {
		if (StringUtils.isBlank(usersList) || usersList.trim().equals("*")) {
			return Collections.emptyList();
		}

		return daoFactory.getUserDao().getUsers(Utility.csvToStringList(usersList), null);
	}

	private SpecimenLabelPrintRule replaceWildcardsWithNull(SpecimenLabelPrintRule rule) {
		rule.setLineage(replaceWildcardWithNull(rule.getLineage()));
		rule.setLabelType(replaceWildcardWithNull(rule.getLabelType()));
		rule.setLabelDesign(replaceWildcardWithNull(rule.getLabelDesign()));
		rule.setPrinterName(replaceWildcardWithNull(rule.getPrinterName()));
		return rule;
	}

	private String replaceWildcardWithNull(String input) {
		return StringUtils.equals(input, "*") ? null : input;
	}

	private List<String> replaceWildcardWithNull(List<String> input) {
		if (input == null) {
			return null;
		}

		return input.stream().filter(e -> e != null && !e.equals("*")).collect(Collectors.toList());
	}

	//
	// TODO: remove the config from database in v4.3
	//
	private void removePrintRulesSetting() {
		cfgSvc.removeSetting(ConfigParams.MODULE, ConfigParams.SPECIMEN_LABEL_PRINT_RULES);
	}

	private static final String PR_MIGRATION_ID = "Migration of specimen print rules to DB";

	private static final String PR_MIGRATION_AUTHOR = "$system";

	private static final String PR_MIGRATION_FILE = "specimen-print-rules.csv";
}
