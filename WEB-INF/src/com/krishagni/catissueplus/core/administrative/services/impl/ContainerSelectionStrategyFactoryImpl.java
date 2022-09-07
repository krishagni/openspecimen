package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionRule;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionStrategy;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionStrategyFactory;
import com.krishagni.catissueplus.core.common.util.LogUtil;

public class ContainerSelectionStrategyFactoryImpl implements ContainerSelectionStrategyFactory {
	private static final LogUtil logger = LogUtil.getLogger(ContainerSelectionStrategyFactoryImpl.class);

	private Map<String, Class> strategyClasses = new HashMap<>();

	private Map<String, Class> ruleClasses = new HashMap<>();

	@Override
	public ContainerSelectionStrategy getStrategy(String name) {
		return getInstance(strategyClasses, name);
	}

	@Override
	public List<String> getStrategyNames() {
		return new ArrayList<>(strategyClasses.keySet());
	}

	public void setStrategyClasses(Map<String, String> strategyClassNames) {
		registerClasses(strategyClassNames, "strategy", ContainerSelectionStrategy.class, strategyClasses);
	}

	@Override
	public ContainerSelectionRule getRule(String name) {
		return getInstance(ruleClasses, name);
	}

	@Override
	public List<String> getRuleNames() {
		return new ArrayList<>(ruleClasses.keySet());
	}

	public void setRuleClasses(Map<String, String> ruleClassNames) {
		registerClasses(ruleClassNames, "rule", ContainerSelectionRule.class, ruleClasses);
	}

	private <T> T getInstance(Map<String, Class> classMap, String name) {
		try {
			Class<T> klass = classMap.get(name);
			if (klass == null) {
				return null;
			}

			return klass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Error instantiating class", e);
		}
	}

	private void registerClasses(Map<String, String> nameImplMap, String category, Class assignableTo, Map<String, Class> output) {
		nameImplMap.forEach(
			(name, impl) -> {
				try {
					Class klass = Class.forName(impl);
					if (!assignableTo.isAssignableFrom(klass)) {
						logger.error(String.format("Invalid class implementation %s for %s %s", impl, category, name));
						return;
					}

					output.put(name, klass);
				} catch (Exception e) {
					logger.error(String.format("Couldn't register class implementation %s for %s %s", impl, category, name), e);
				}
			}
		);
	}
}