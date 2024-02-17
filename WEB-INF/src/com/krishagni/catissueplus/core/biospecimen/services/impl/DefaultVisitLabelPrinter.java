package com.krishagni.catissueplus.core.biospecimen.services.impl;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.LogUtil;

public class DefaultVisitLabelPrinter extends AbstractLabelPrinter<Visit> {
	private static final LogUtil logger = LogUtil.getLogger(DefaultVisitLabelPrinter.class);
	
	private ConfigurationService cfgSvc;
	
	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	@Override
	protected boolean isApplicableFor(LabelPrintRule rule, Visit visit, User user, String ipAddr) {
		if (StringUtils.isBlank(visit.getName()) || visit.isMissedOrNotCollected()) {
			return false;
		}

		VisitLabelPrintRule visitLabelPrintRule = (VisitLabelPrintRule) rule;
		return visitLabelPrintRule.isApplicableFor(visit, user, ipAddr);
	}

	@Override
	protected String getObjectType() {
		return "VISIT";
	}

	@Override
	protected String getItemType() {
		return Visit.getEntityName();
	}

	@Override
	protected String getItemLabel(Visit visit) {
		return visit.getName();
	}

	@Override
	protected Long getItemId(Visit visit) {
		return visit.getId();
	}

//	@Override
//	protected void loadRulesFromDb() {
//		reloadRules();
//	}

//	@Override
//	public void afterPropertiesSet() throws Exception {
//		this.rules = null;
//		cfgSvc.registerChangeListener(ConfigParams.MODULE, this);
//	}
	
//	@Override
//	public void onConfigChange(String name, String value) {
//		if (!name.equals(ConfigParams.VISIT_LABEL_PRINT_RULES)) {
//			return;
//		}
//
//		this.rules = null;
//	}
		
//	private void reloadRules() {
//		FileDetail fileDetail = cfgSvc.getFileDetail(ConfigParams.MODULE, ConfigParams.VISIT_LABEL_PRINT_RULES);
//		if (fileDetail == null || fileDetail.getFileIn() == null) {
//			this.rules = null;
//			return;
//		}
//
//		List<VisitLabelPrintRule> rules = new ArrayList<>();
//		BufferedReader reader = null;
//		try {
//			reader = new BufferedReader(new InputStreamReader(fileDetail.getFileIn()));
//
//			String ruleLine = null;
//			while ((ruleLine = reader.readLine()) != null) {
//				VisitLabelPrintRule rule = parseRule(ruleLine);
//				if (rule == null) {
//					continue;
//				}
//
//				rules.add(rule);
//				logger.info(String.format("Adding rule [%s]", rule));
//			}
//
//			this.rules = rules;
//		} catch (Exception e) {
//			logger.error("Error loading rules from file: " + fileDetail.getFilename(), e);
//		} finally {
//			IOUtils.closeQuietly(fileDetail.getFileIn());
//			IOUtils.closeQuietly(reader);
//		}
//	}
	
	//
	// Format of each rule
	// cp_short_title	visit_site	user_login	ip_address	label_type	label_tokens	label_design	printer_name	dir_path
	//
//	private VisitLabelPrintRule parseRule(String ruleLine) {
//		try {
//			if (ruleLine.startsWith("#")) {
//				return null;
//			}
//
//			String[] ruleLineFields = ruleLine.split("\\t");
//			if (ruleLineFields.length != 10) {
//				logger.error(String.format("Invalid rule [%s]. Expected variables: 10, Actual: [%d]", ruleLine, ruleLineFields.length));
//				return null;
//			}
//
//			int idx = 0;
//			VisitLabelPrintRule rule = new VisitLabelPrintRule();
//			rule.setCpShortTitle(ruleLineFields[idx++]);
//			rule.setVisitSite(ruleLineFields[idx++]);
//			rule.setUserLogin(getUser(ruleLineFields[idx++]));
//
//			if (!ruleLineFields[idx++].equals("*")) {
//				rule.setIpAddressMatcher(new IpAddressMatcher(ruleLineFields[idx - 1]));
//			}
//			rule.setLabelType(ruleLineFields[idx++]);
//
//			String[] labelTokens = ruleLineFields[idx++].split(",");
//			boolean badTokens = false;
//
//			List<LabelTmplToken> tokens = new ArrayList<>();
//			for (String labelToken : labelTokens) {
//				LabelTmplToken token = printLabelTokensRegistrar.getToken(labelToken);
//				if (token == null) {
//					logger.error(String.format("Invalid rule [%s]. Unknown token: [%s]", ruleLine, labelToken));
//					badTokens = true;
//					break;
//				}
//
//				tokens.add(token);
//			}
//
//			if (badTokens) {
//				return null;
//			}
//
//			List<Pair<LabelTmplToken, List<String>>> dataTokens = new ArrayList<>();
//			for (LabelTmplToken token : tokens) {
//				dataTokens.add(Pair.make(token, new ArrayList<>()));
//			}
//
//			rule.setDataTokens(dataTokens);
//			rule.setLabelDesign(ruleLineFields[idx++]);
//			rule.setPrinterName(ruleLineFields[idx++]);
//			rule.setCmdFilesDir(ruleLineFields[idx++]);
//
//			if (!ruleLineFields[idx++].equals("*")) {
//				rule.setCmdFileFmt(ruleLineFields[idx - 1]);
//			}
//
//			return rule;
//		} catch (Exception e) {
//			logger.error("Error parsing rule: " + ruleLine, e);
//		}
//
//		return null;
//	}

//	private User getUser(String loginName) {
//		if (StringUtils.isBlank(loginName) || loginName.equals("*")) {
//			return null;
//		}
//
//		return daoFactory.getUserDao().getUser(loginName, null);
//	}
}
