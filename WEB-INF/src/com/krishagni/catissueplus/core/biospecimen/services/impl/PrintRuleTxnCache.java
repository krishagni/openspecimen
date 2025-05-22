package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.TransactionCache;
import com.krishagni.catissueplus.core.common.domain.PrintRuleConfig;
import com.krishagni.catissueplus.core.common.repository.PrintRuleConfigsListCriteria;
import com.krishagni.catissueplus.core.common.util.LogUtil;

@Configurable
public class PrintRuleTxnCache {
	private static final LogUtil logger = LogUtil.getLogger(PrintRuleTxnCache.class);

	private static final PrintRuleTxnCache instance = new PrintRuleTxnCache();

	@Autowired
	private DaoFactory daoFactory;

	public static PrintRuleTxnCache getInstance() {
		return instance;
	}

	public List<PrintRuleConfig> getRules(String objectType) {
		List<PrintRuleConfig> result = getPrintRules().get(objectType);
		if (result == null) {
			result = getRulesFromDb(objectType);
			getPrintRules().put(objectType, result);
		}

		return result;
	}

	//
	// TODO: This needs to be refactored to get the rules in pages and stop when a first matching rule is found
	//
	@PlusTransactional
	private List<PrintRuleConfig> getRulesFromDb(String objectType) {
		try {
			logger.info("Loading print rules from database for: " + objectType);
			List<PrintRuleConfig> rules = daoFactory.getPrintRuleConfigDao()
				.getPrintRules(new PrintRuleConfigsListCriteria().objectType(objectType).maxResults(Integer.MAX_VALUE));
			rules.sort((r1, r2) -> {
				if (r1.getSortOrder() != null && r2.getSortOrder() != null) {
					return r1.getSortOrder().compareTo(r2.getSortOrder());
				} else if (r1.getSortOrder() != null) {
					return -1;
				} else if (r2.getSortOrder() != null) {
					return 1;
				} else {
					return -1 * r1.getId().compareTo(r2.getId());
				}
			});

			return rules;
		} catch (Exception e) {
			logger.error("Error loading print rules for: " + objectType, e);
			throw new RuntimeException("Error loading print rules for: " + objectType, e);
		}
	}

	private Map<String, List<PrintRuleConfig>> getPrintRules() {
		return TransactionCache.getInstance().get("printRules", new HashMap<>());
	}
}
