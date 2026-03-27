package com.krishagni.catissueplus.core.init;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

public class AppPropertyPlaceholderConfigurer extends PropertySourcesPlaceholderConfigurer {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Properties props = new Properties();
		props.putAll(AppProperties.getInstance().getProperties());

		String auditEnabled = props.getProperty("app.audit_enabled");
		if (StringUtils.isBlank(auditEnabled)) {
			props.setProperty("app.audit_enabled", "true");
		}

		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new PropertiesPropertySource("appProperties", props));
		setPropertySources(sources);

		super.postProcessBeanFactory(beanFactory);
	}
}
