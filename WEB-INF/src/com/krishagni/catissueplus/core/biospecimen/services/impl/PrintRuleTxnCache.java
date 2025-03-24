package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.TransactionalThreadLocals;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.PrintRuleConfig;
import com.krishagni.catissueplus.core.common.repository.PrintRuleConfigsListCriteria;
import com.krishagni.catissueplus.core.common.util.LogUtil;

@Configurable
public class PrintRuleTxnCache {
	private static final LogUtil logger = LogUtil.getLogger(PrintRuleTxnCache.class);

	private static final PrintRuleTxnCache instance = new PrintRuleTxnCache();

	private ThreadLocal<Map<String, List<? extends LabelPrintRule>>> rules = new ThreadLocal<>() {
		@Override
		protected Map<String, List<? extends LabelPrintRule>> initialValue() {
			TransactionalThreadLocals.getInstance().register(this);
			return new HashMap<>();
		}
	};

	@Autowired
	private DaoFactory daoFactory;

	public static PrintRuleTxnCache getInstance() {
		return instance;
	}

	public List<? extends LabelPrintRule> getRules(String objectType) {
		List<? extends LabelPrintRule> result = rules.get().get(objectType);
		if (result == null) {
			result = getRulesFromDb(objectType);
			if (result == null) {
				result = new ArrayList<>();
			}

			rules.get().put(objectType, result);
		}

		return result;
	}

	//
	// TODO: This needs to be refactored to get the rules in pages and stop when a first matching rule is found
	//
	@PlusTransactional
	private List<? extends LabelPrintRule> getRulesFromDb(String objectType) {
		try {
			logger.info("Loading print rules from database for: " + objectType);
			return daoFactory.getPrintRuleConfigDao()
				.getPrintRules(new PrintRuleConfigsListCriteria().objectType(objectType).maxResults(Integer.MAX_VALUE))
				.stream().map(PrintRuleConfig::getRule)
				.collect(Collectors.toList());
		} catch (Exception e) {
			logger.error("Error loading print rules for: " + objectType, e);
			throw new RuntimeException("Error loading print rules for: " + objectType, e);
		}
	}
}
