package com.krishagni.catissueplus.core.init;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class AppPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {
	protected String resolvePlaceholder(String placeholder, Properties props) {
		String result = AppProperties.getInstance().getProperties().getProperty(placeholder);
		if (placeholder.equals("app.audit_enabled")) {
			result = StringUtils.isBlank(result) ? "true" : result;
		}
		return result;
	}
}