package com.krishagni.catissueplus.core.init;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jndi.JndiTemplate;


import com.krishagni.catissueplus.core.common.util.LogUtil;

public class AppProperties implements FactoryBean<Properties> {
	private static final LogUtil logger         = LogUtil.getLogger(AppProperties.class);

	private static final String JNDI_PREFIX     = "java:/comp/env/";

	private static final String JNDI_CFG_PREFIX = JNDI_PREFIX + "config/";

	private static final String APP_PROPS       = "application.properties";

	private static final String VERSION_PROP    = "buildinfo.version";

	private static final String BUILD_DATE_PROP = "buildinfo.date";

	private static final String REVISION_PROP   = "buildinfo.commit_revision";

	private static final String DS_JNDI_PROP    = "datasource.jndi";

	private static final String DS_DIALECT_PROP = "datasource.dialect";

	private static final String DB_TYPE_PROP    = "database.type";

	private static final String MYSQL_TYPE      = "mysql";

	private static final String MYSQL_DIALECT   = "org.hibernate.dialect.MySQLDialect";

	private static final String ORA_TYPE        = "oracle";

	private static final String ORA_DIALECT     = "org.hibernate.dialect.Oracle10gDialect";

	private static final String DEF_CFG_LOC     = "%s" + File.separator + "conf" + File.separator + "%s.properties";

	private static final String NODE_NAME_PROP  = "node.name";

	private static AppProperties instance;

	private String tomcatDir;

	private String contextName;

	private Properties props;

	private AppProperties(String tomcatDir, String contextName) {
		this.tomcatDir = tomcatDir;
		this.contextName = contextName;
		this.props = loadProperties();
	}

	public Properties getProperties() {
		return props;
	}

	public String getBuildDate() {
		return props.getProperty(BUILD_DATE_PROP);
	}

	public String getBuildVersion() {
		return props.getProperty(VERSION_PROP);
	}

	public String getBuildRevision() {
		return props.getProperty(REVISION_PROP);
	}

	public String getNodeName() {
		return props.getProperty(NODE_NAME_PROP, "none");
	}

	@Override
	public Properties getObject() throws Exception {
		return props;
	}

	@Override
	public Class<?> getObjectType() {
		return Properties.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public static AppProperties getInstance() {
		return instance;
	}

	public static AppProperties getInstance(String tomcatDir, String contextName) {
		if (instance != null) {
			return instance;
		}

		instance = new AppProperties(tomcatDir, contextName);
		return instance;
	}

	private Properties loadProperties() {
		Properties properties = loadBuildProperties();
		properties.putAll(loadExternalConfig());

		String ds = properties.getProperty(DS_JNDI_PROP);
		if (StringUtils.isBlank(ds)) {
			logger.error("Data source is not specified. Application will be non-functional");
		} else if (!ds.startsWith(JNDI_PREFIX)) {
			logger.info("Data source JNDI name is not fully qualified: " + ds);
			ds = ds.trim();

			int idx = 0;
			while (ds.charAt(idx) == '/') {
				++idx;
			}

			ds = JNDI_PREFIX + ds.substring(idx);
			logger.info("Transformed the data source JNDI name to " + ds);
			properties.put(DS_JNDI_PROP, ds);
		}

		String dbType = properties.getProperty(DB_TYPE_PROP);
		if (MYSQL_TYPE.equalsIgnoreCase(dbType)) {
			logger.info("Recognised as using MySQL data source. Initialising the dialect to " + MYSQL_DIALECT);
			properties.put(DS_DIALECT_PROP, MYSQL_DIALECT);
		} else if (ORA_TYPE.equalsIgnoreCase(dbType)) {
			logger.info("Recognised as using Oracle data source. Initialising the dialect to " + ORA_DIALECT);
			properties.put(DS_DIALECT_PROP, ORA_DIALECT);
		} else {
			logger.error("Data source type is not specified. Application will be non-functional");
		}

		return properties;
	}

	private Properties loadBuildProperties() {
		InputStream in = null;

		try {
			in = this.getClass().getClassLoader().getResourceAsStream(APP_PROPS);

			Properties props = new Properties();
			props.load(in);
			return props;
		} catch (Exception e) {
			logger.error("Error loading build properties", e);
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private Properties loadExternalConfig() {
		InputStream in = null;

		try {
			String externalConfigPath = getExternalConfigPath();
			logger.info("Loading app configuration properties from " + externalConfigPath);

			in = new FileInputStream(externalConfigPath);

			Properties props = new Properties();
			props.load(in);

			props.remove(VERSION_PROP);
			props.remove(BUILD_DATE_PROP);
			props.remove(REVISION_PROP);
			return props;
		} catch (Exception e) {
			logger.error("Error loading external config properties for " + contextName, e);
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private String getExternalConfigPath() {
		String externalConfigPath = null;
		try {
			externalConfigPath = new JndiTemplate().lookup(JNDI_CFG_PREFIX + contextName, String.class);
		} catch (NameNotFoundException nfe) {
			externalConfigPath = String.format(DEF_CFG_LOC, tomcatDir, contextName);
		} catch (NamingException e) {
			throw new RuntimeException("Error doing config property resource lookup", e);
		}

		return externalConfigPath;
	}
}