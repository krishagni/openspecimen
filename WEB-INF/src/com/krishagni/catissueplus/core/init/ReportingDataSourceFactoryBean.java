package com.krishagni.catissueplus.core.init;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jndi.JndiTemplate;

import com.krishagni.catissueplus.core.common.util.LogUtil;

public class ReportingDataSourceFactoryBean implements FactoryBean<DataSource> {
	private static final LogUtil logger = LogUtil.getLogger(ReportingDataSourceFactoryBean.class);

	private static final String JNDI_PREFIX = "java:/comp/env/";

	private static final String REPORTING_DS_JNDI_PROP = "reporting.datasource.jndi";

	private Properties appProps;

	private DataSource reportingDataSource;

	public void setAppProps(Properties appProps) {
		this.appProps = appProps;
	}

	@Override
	public DataSource getObject() throws Exception {
		if (reportingDataSource != null) {
			return reportingDataSource;
		}

		String jndiName = appProps != null ? appProps.getProperty(REPORTING_DS_JNDI_PROP) : null;
		if (StringUtils.isBlank(jndiName)) {
			logger.info("Reporting data source is not configured. Query APIs will use the default data source.");
			return null;
		}

		jndiName = normaliseJndiName(jndiName);
		logger.info("Initialising reporting data source using JNDI name: " + jndiName);
		reportingDataSource = new JndiTemplate().lookup(jndiName, DataSource.class);
		return reportingDataSource;
	}

	@Override
	public Class<?> getObjectType() {
		return DataSource.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private String normaliseJndiName(String jndiName) {
		jndiName = jndiName.trim();
		if (jndiName.startsWith(JNDI_PREFIX)) {
			return jndiName;
		}

		int idx = 0;
		while (idx < jndiName.length() && jndiName.charAt(idx) == '/') {
			++idx;
		}

		return JNDI_PREFIX + jndiName.substring(idx);
	}
}
