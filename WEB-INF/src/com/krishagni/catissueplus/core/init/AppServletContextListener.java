package com.krishagni.catissueplus.core.init;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.krishagni.catissueplus.core.common.PluginManager;
import com.krishagni.catissueplus.core.common.util.ClassPathUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public class AppServletContextListener implements ServletContextListener {

	private static final LogUtil logger = LogUtil.getLogger(AppServletContextListener.class);

	private static final String DATA_DIR_PROP   = "app.data_dir";

	private static final String LOG_CONF_PROP   = "app.log_conf";

	private static final String DEF_LOG_CONF    = "/default-log4j.properties";

	private static final String PLUGIN_DIR_PROP = "plugin.dir";

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			if (Utility.isRootUser()) {
				System.err.println("****************************************");
				System.err.println("*        ERROR! ERROR!! ERROR!!!       *");
				System.err.println("****************************************");
				System.err.println("OpenSpecimen is started using root user.");
				System.err.println("Avoid using root user to run OpenSpecimen.");
				System.err.println("Instead consider using a regular user account.");
				System.err.println("Exiting");
				System.exit(1);
			}

			String rootDir = sce.getServletContext().getResource("/").getPath();
			String tomcatDir = new File(rootDir).getParentFile().getParentFile().getPath();

			Properties props = AppProperties.getInstance(tomcatDir, getContextName(sce)).getProperties();

			String msg = getWelcomeMessage();
			writeToConsole(msg);

			initLogging(props);
			writeToLogFile(msg);

			String pluginDir = props.getProperty(PLUGIN_DIR_PROP);
			if (StringUtils.isNotBlank(pluginDir)) {
				logger.info("Loading plugins from " + pluginDir);
				loadPluginResources(pluginDir);
			}
		} catch (Exception e) {
			logger.error("Error initializing app", e);
			throw new RuntimeException("Error initializing app", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// TODO Auto-generated method stub

	}

	private String getContextName(ServletContextEvent sce) {
		try {
			String contextPath = sce.getServletContext().getContextPath();
			return contextPath.substring(1); // context paths always start with /
		} catch (Exception e) {
			logger.error("Error obtaining webapp context name", e);
			throw new RuntimeException(e);
		}
	}

	private void initLogging(Properties props)
	throws Exception {
		URL url;
		String logConf = props.getProperty(LOG_CONF_PROP);
		if (StringUtils.isNotBlank(logConf)) {
			url = new File(logConf).toURI().toURL();
		} else {
			url = this.getClass().getResource(DEF_LOG_CONF);
		}

		String dataDir = props.getProperty(DATA_DIR_PROP);
		if (StringUtils.isBlank(dataDir)) {
			dataDir = ".";
		}

		String logDir = dataDir + File.separator + "logs";
		new File(logDir).mkdirs();
		System.setProperty("os_log_dir", logDir);

		String nodeName = AppProperties.getInstance().getNodeName();
		if (!"none".equals(nodeName)) {
			System.setProperty("os_log_file", "os." + nodeName.replaceAll("\\s+", "_"));
		} else {
			System.setProperty("os_log_file", "os");
		}

		LogUtil.configure(url);
		logger.info("Initialised logging configuration from following file: " + url);
	}

	private void loadPluginResources(String pluginDirPath) {
		File pluginDir = new File(pluginDirPath);
		if (!pluginDir.isDirectory()) {
			throw new RuntimeException("Invalid plugin directory: " + pluginDirPath);
		}

		List<File> files = Arrays.stream(Objects.requireNonNull(pluginDir.listFiles()))
			.sorted((f1, f2) -> ObjectUtils.compare(f1.getName(), f2.getName()))
			.collect(Collectors.toList());

		files.stream().filter(File::isDirectory).forEach(this::loadPluginResources);
		files.stream().filter(File::isFile).forEach(this::loadPlugin);
	}

	private void loadPluginResources(File pluginDir) {
		Arrays.stream(Objects.requireNonNull(pluginDir.listFiles()))
			.sorted((f1, f2) -> ObjectUtils.compare(f1.getName(), f2.getName()))
			.forEach(this::loadPlugin);
	}

	private void loadPlugin(File file) {
		if (file.isDirectory() || !file.getName().endsWith(".jar")) {
			return;
		}

		JarFile jarFile = null;
		try {
			logger.info("Loading plugin resource from: " + file.getAbsolutePath());
			jarFile = new JarFile(file);
			Attributes attrs = jarFile.getManifest().getMainAttributes();
			String pluginName = attrs.getValue("os-plugin-name");
			if (StringUtils.isNotBlank(pluginName)) {
//				Class<?> jarLoader = Class.forName("com.krishagni.openspecimen.JarLoader");
//				Method addToClassPath = jarLoader.getDeclaredMethod("addToClassPath", JarFile.class);
//				addToClassPath.invoke(null, jarFile);
				ClassPathUtil.addFile(file);
				PluginManager.getInstance().addPlugin(attrs);
			}
		} catch (Exception e) {
			logger.error("Error loading plugin resources from: ", e);
		} finally {
			IOUtils.closeQuietly(jarFile);
		}
	}

	private void writeToConsole(String msg) {
		System.out.println(msg);
	}

	private void writeToLogFile(String msg) {
		logger.info(msg);
	}

	private String getWelcomeMessage() {
		AppProperties appProps = AppProperties.getInstance();

		return
			"\n ***************************************************" +
			"\n OpenSpecimen, a Krishagni Product" +
			"\n Build Version : " + appProps.getBuildVersion() +
			"\n Build Date    : " + new Date(Long.parseLong(appProps.getBuildDate())) +
			"\n Commit        : " + appProps.getBuildRevision() +
			"\n Present Time  : " + Calendar.getInstance().getTime() +
			"\n ***************************************************";
	}
}
