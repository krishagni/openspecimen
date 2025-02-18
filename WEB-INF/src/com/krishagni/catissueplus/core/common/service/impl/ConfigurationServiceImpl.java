package com.krishagni.catissueplus.core.common.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.context.event.ContextRefreshedEvent;

import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PluginManager;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.domain.ConfigErrorCode;
import com.krishagni.catissueplus.core.common.domain.ConfigProperty;
import com.krishagni.catissueplus.core.common.domain.ConfigSetting;
import com.krishagni.catissueplus.core.common.domain.Module;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.ConfigSettingDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ConfigChangeListener;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.catissueplus.core.importer.services.ImportService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class ConfigurationServiceImpl implements ConfigurationService, InitializingBean, ApplicationListener<ContextRefreshedEvent> {
	private static final LogUtil logger = LogUtil.getLogger(ConfigurationServiceImpl.class);

	private static final Pattern DATE_FORMAT = Pattern.compile("(?<day>d+)|(?<month>M+)|(?<year>y+)|(?<week>EEE+)");
	
	private Map<String, List<ConfigChangeListener>> changeListeners = new ConcurrentHashMap<>();
	
	private Map<String, Map<String, ConfigSetting>> configSettings;

	private DaoFactory daoFactory;
	
	private MessageSource messageSource;

	private Properties appProps = new Properties();
		
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setAppProps(Properties appProps) {
		this.appProps = appProps;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ConfigSettingDetail>> getSettings(RequestEvent<String> req) {
		String module = req.getPayload();

		List<ConfigSetting> settings = new ArrayList<>();
		if (StringUtils.isBlank(module)) {
			for (Map<String, ConfigSetting> moduleSettings : configSettings.values()) {
				settings.addAll(moduleSettings.values());
			}
		} else {
			Map<String, ConfigSetting> moduleSettings = configSettings.get(module);
			if (moduleSettings != null) {
				settings.addAll(moduleSettings.values());
			}
		}
		
		return ResponseEvent.response(ConfigSettingDetail.from(settings));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ConfigSettingDetail> getSetting(RequestEvent<Pair<String, String>> req) {
		Pair<String, String> payload = req.getPayload();
		try {
			Map<String, ConfigSetting> moduleSettings = configSettings.get(payload.first());

			ConfigSetting setting;
			if (moduleSettings == null || (setting = moduleSettings.get(payload.second())) == null) {
				return ResponseEvent.response(new ConfigSettingDetail());
			}

			return ResponseEvent.response(ConfigSettingDetail.from(setting));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ConfigSettingDetail> saveSetting(RequestEvent<ConfigSettingDetail> req) {
		AccessCtrlMgr.getInstance().ensureUserIsAdmin();
		ConfigSettingDetail detail = req.getPayload();
		
		String module = detail.getModule();
		Map<String, ConfigSetting> moduleSettings = configSettings.get(module);
		if (moduleSettings == null || moduleSettings.isEmpty()) {
			return ResponseEvent.userError(ConfigErrorCode.MODULE_NOT_FOUND);
		}
		
		String prop = detail.getName();
		ConfigSetting existing = moduleSettings.get(prop);
		if (existing == null) {
			return ResponseEvent.userError(ConfigErrorCode.SETTING_NOT_FOUND);
		}
		
		String setting = detail.getValue();
		if (!isValidSetting(existing.getProperty(), setting)) {
			return ResponseEvent.userError(ConfigErrorCode.INVALID_SETTING_VALUE, setting);
		}
		
		boolean successful = false;
		try {
			ConfigSetting newSetting = createSetting(existing, setting);
			existing.setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());

			daoFactory.getConfigSettingDao().saveOrUpdate(existing);
			daoFactory.getConfigSettingDao().saveOrUpdate(newSetting);
			moduleSettings.put(prop, newSetting);
			
			notifyListeners(module, prop, setting);
			successful = true;
			return ResponseEvent.response(ConfigSettingDetail.from(moduleSettings.get(prop)));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			if (successful) {
				deleteOldSettingFile(existing);
			} else {
				existing.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
				moduleSettings.put(prop, existing);
			}
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<File> getSettingFile(RequestEvent<Pair<String, String>> req) {
		Pair<String, String> payload = req.getPayload();
		try {
			Map<String, ConfigSetting> moduleSettings = configSettings.get(payload.first());
			if (moduleSettings == null) {
				return ResponseEvent.userError(ConfigErrorCode.MODULE_NOT_FOUND);
			}

			ConfigSetting setting = moduleSettings.get(payload.second());
			if (setting == null) {
				return ResponseEvent.userError(ConfigErrorCode.SETTING_NOT_FOUND);
			}

			return ResponseEvent.response(getSettingFile(payload.first(), payload.second()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<String> uploadSettingFile(RequestEvent<FileDetail> req) {
		Date startTime = Calendar.getInstance().getTime();
		OutputStream out = null;

		try {
			FileDetail detail = req.getPayload();
			Map<String, Object> params = detail.getObjectProps();
			String module = null, property = null;
			if (params != null) {
				module = (String) params.get("module");
				property = (String) params.get("property");
			}

			if (StringUtils.isBlank(module)) {
				return ResponseEvent.userError(CommonErrorCode.INVALID_INPUT, "Module name is required");
			}

			if (StringUtils.isBlank(property)) {
				return ResponseEvent.userError(CommonErrorCode.INVALID_INPUT, "Property name is required");
			}

			Map<String, ConfigSetting> settings = configSettings.get(module);
			if (settings == null) {
				return ResponseEvent.userError(ConfigErrorCode.SETTING_NOT_FOUND);
			}

			ConfigSetting setting = settings.get(property);
			if (setting == null) {
				return ResponseEvent.userError(ConfigErrorCode.SETTING_NOT_FOUND);
			}

			if (!setting.getProperty().isFile()) {
				return ResponseEvent.userError(ConfigErrorCode.INVALID_SETTING_VALUE);
			}

			String filename = UUID.randomUUID() + "_" + detail.getFilename();
			File settingsDir = getSettingFilesDir();
			Path settingFile = settingsDir.toPath().resolve(filename);
			if (!settingFile.normalize().startsWith(settingsDir.toPath())) {
				return ResponseEvent.userError(CommonErrorCode.INVALID_INPUT, "Input filename contains path traversal characters");
			}

			out = new FileOutputStream(settingFile.toFile());
			IOUtils.copy(detail.getFileIn(), out);

			ImportService importSvc = OpenSpecimenAppCtxProvider.getBean("importObjectsSvc");
			Map<String, String> impParams = new HashMap<>();
			impParams.put("module", module);
			impParams.put("property", property);
			importSvc.saveJob("cfg_setting_file", "UPSERT", startTime, impParams);
			return ResponseEvent.response(filename);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ConfigSettingDetail>> getSettingHistory(RequestEvent<Pair<String, String>> req) {
		try {
			if (!AuthUtil.isAdmin()) {
				return ResponseEvent.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
			}

			Pair<String, String> payload = req.getPayload();
			Map<String, ConfigSetting> moduleSettings = configSettings.get(payload.first());
			if (moduleSettings == null || moduleSettings.get(payload.second()) == null) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<ConfigSetting> settings = daoFactory.getConfigSettingDao().getHistory(payload.first(), payload.second());
			return ResponseEvent.response(ConfigSettingDetail.from(settings, true));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public Integer getIntSetting(String module, String name, Integer... defValue) {
		String value = getStrSetting(module, name, (String)null);
		if (StringUtils.isBlank(value)) {
			return defValue != null && defValue.length > 0 ? defValue[0] : null;
		}
		
		return Integer.parseInt(value);
	}

	@Override
	@PlusTransactional
	public Double getFloatSetting(String module, String name, Double... defValue) {
		String value = getStrSetting(module, name, (String)null);
		if (StringUtils.isBlank(value)) {
			return defValue != null && defValue.length > 0 ? defValue[0] : null;
		}
		
		return Double.parseDouble(value);
	}

	@Override
	@PlusTransactional
	public String getStrSetting(String module, String name,	String... defValue) {
		Map<String, ConfigSetting> moduleSettings = configSettings.get(module);
		
		String value = null;
		if (moduleSettings != null) {
			ConfigSetting setting = moduleSettings.get(name);
			if (setting != null) {
				value = setting.getValue();

				if (setting.getProperty().isSecured() && value != null) {
					value = Utility.decrypt(value);
				}
			}
		}

		if (StringUtils.isBlank(value)) {
			value = defValue != null && defValue.length > 0 ? defValue[0] : null;
		} 
		
		return value;
	}
	
	@Override
	@PlusTransactional	
	public Character getCharSetting(String module, String name, Character... defValue) {
		String value = getStrSetting(module, name, (String)null);
		if (StringUtils.isBlank(value)) {
			return defValue != null && defValue.length > 0 ? defValue[0] : null;
		}
		
		return value.charAt(0);
	}

	@Override
	@PlusTransactional	
	public Boolean getBoolSetting(String module, String name, Boolean ... defValue) {
		String value = getStrSetting(module, name, (String)null);
		if (StringUtils.isBlank(value)) {
			return defValue != null && defValue.length > 0 ? defValue[0] : null;
		}
		
		return Boolean.parseBoolean(value);		
	}

	@Override
	public File getSettingFile(String module, String name, File... defValue) {
		String value = getStrSetting(module, name, (String)null);
		if (StringUtils.isBlank(value)) {
			return defValue != null && defValue.length > 0 ? defValue[0] : null;
		}

		File settingsDir = getSettingFilesDir();
		Path settingFile = settingsDir.toPath().resolve(value);
		if (!settingFile.normalize().startsWith(settingsDir.toPath())) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Setting file has one or more path traversal characters/sequences. Value: " + value);
		}

		File file = settingFile.toFile();
		if (!file.exists()) {
			value = value.split("_", 2)[1];
			throw OpenSpecimenException.userError(ConfigErrorCode.FILE_MOVED_OR_DELETED, value);
		}

		return file;
	}

	@Override
	public String getFileContent(String module, String name, File... defValue) {
		FileDetail fileDetail = null;
		try {
			fileDetail = getFileDetail(module, name, defValue);
			if (fileDetail != null && fileDetail.getFileIn() != null) {
				return IOUtils.toString(fileDetail.getFileIn(), Charset.defaultCharset());
			}

			return StringUtils.EMPTY;
		} catch (OpenSpecimenException ose) {
			throw ose;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			if (fileDetail != null) {
				IOUtils.closeQuietly(fileDetail.getFileIn());
			}
		}
	}

	@Override
	public FileDetail getFileDetail(String module, String name, File... defValue) {
		try {
			InputStream in = null;
			String contentType = null;
			String filename = null;

			String value = getStrSetting(module, name, (String)null);
			if (StringUtils.isBlank(value)) {
				if (defValue != null && defValue.length > 0 && defValue[0] != null) {
					contentType = Utility.getContentType(defValue[0]);
					filename = defValue[0].getName();
					in = new FileInputStream(defValue[0]);
				}
			} else if (value.startsWith("classpath:")) {
				filename = value.substring(value.lastIndexOf(File.separator) + 1);
				in = this.getClass().getResourceAsStream(value.substring(10));
			} else {
				File settingsDir = getSettingFilesDir();
				Path filePath = settingsDir.toPath().resolve(value);
				if (!filePath.normalize().startsWith(settingsDir.toPath())) {
					throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "The setting filename has one or more path traversal sequences. Value = " + value);
				}

				File file = filePath.toFile();
				contentType = Utility.getContentType(file);
				filename = file.getName().substring(file.getName().indexOf("_") + 1);
				in = new FileInputStream(file);
			}

			if (in == null) {
				return null;
			}

			FileDetail fileDetail = new FileDetail();
			fileDetail.setContentType(contentType);
			fileDetail.setFilename(filename);
			fileDetail.setFileIn(in);
			return fileDetail;
		} catch (Exception e) {
			OpenSpecimenException ose = null;
			if (e instanceof OpenSpecimenException) {
				ose = (OpenSpecimenException) e;
			} else {
				ose = OpenSpecimenException.serverError(e);
			}

			throw ose;
		}
	}

	@Override
	public void downloadSettingFile(String moduleName, String propertyName, HttpServletResponse httpResp) {
		Date startTime = Calendar.getInstance().getTime();
		FileDetail detail = getFileDetail(moduleName, propertyName);
		if (detail == null || detail.getFileIn() == null) {
			return;
		}

		Utility.sendToClient(httpResp, detail.getFilename(), detail.getContentType(), detail.getFileIn());

		ExportService exportSvc = OpenSpecimenAppCtxProvider.getBean("exportSvc");
		Map<String, String> params = new HashMap<>();
		params.put("module", moduleName);
		params.put("property", propertyName);
		exportSvc.saveJob("cfg_setting_file", startTime, params);
	}

	@Override
	@PlusTransactional
	public void reload() {
		Map<String, Map<String, ConfigSetting>> settingsMap = new ConcurrentHashMap<>();
		
		List<ConfigSetting> settings = daoFactory.getConfigSettingDao().getAllSettings();
		for (ConfigSetting setting : settings) {
			ConfigProperty prop = setting.getProperty();
			Hibernate.initialize(prop.getAllowedValues()); // pre-init

			Module module = prop.getModule();

			Map<String, ConfigSetting> moduleSettings = settingsMap.get(module.getName());
			if (moduleSettings == null) {
				moduleSettings = new ConcurrentHashMap<>();
				settingsMap.put(module.getName(), moduleSettings);
			}

			moduleSettings.put(prop.getName(), setting);
		}

		this.configSettings = settingsMap;
		
		for (List<ConfigChangeListener> listeners : changeListeners.values()) {
			for (ConfigChangeListener listener : listeners) {
				listener.onConfigChange(StringUtils.EMPTY, StringUtils.EMPTY);
			}			
		}
	}

	@Override
	public void registerChangeListener(String module, ConfigChangeListener callback) {
		List<ConfigChangeListener> listeners = changeListeners.computeIfAbsent(module, k -> new ArrayList<>());
		listeners.add(callback);
	}
	
	@Override
	public Map<String, Object> getLocaleSettings() {
		Map<String, Object> result = new HashMap<>();

		Locale locale = Locale.getDefault();
		result.put("locale", locale.toString());
		result.put("dateFmt", getDateFormat());
		result.put("timeFmt", getTimeFormat());
		result.put("deFeDateFmt", getDeFeDateFormat());
		result.put("deBeDateFmt", getDeDateFormat());
		result.put("utcOffset", Utility.getTimezoneOffset());

		return result;
	}
		
	@Override
	public String getDateFormat() {
		String dateFmt = getStrSetting("common", "date_format");
		if (StringUtils.isBlank(dateFmt)) {
			dateFmt = messageSource.getMessage("common_date_fmt", null, Locale.getDefault());
		}

		return dateFmt;
	}
	
	@Override
	public String getDeDateFormat() {
		String dateFmt = getStrSetting("common", "de_date_format");
		if (StringUtils.isBlank(dateFmt)) {
			dateFmt = messageSource.getMessage("common_de_be_date_fmt", null, Locale.getDefault());
		}

		return dateFmt;
	}

	@Override
	public String getTimeFormat() {
		String timeFmt = getStrSetting("common", "time_format");
		if (StringUtils.isBlank(timeFmt)) {
			timeFmt = messageSource.getMessage("common_time_fmt", null, Locale.getDefault());
		}

		return timeFmt;
	}

	@Override
	public String getDeDateTimeFormat() {
		return getDeDateFormat() + " " + getTimeFormat();
	}
	
	@Override
	public Map<String, String> getWelcomeVideoSettings() {
		Map<String, ConfigSetting> moduleConfig = configSettings.get("common");
		if (moduleConfig == null) {
			return Collections.emptyMap();
		}
		
		Map<String, String> result = new HashMap<String, String>();
		
		ConfigSetting source = moduleConfig.get("welcome_video_source");
		if (source != null) {
			result.put("welcome_video_source", source.getValue());
		}
		
		ConfigSetting url = moduleConfig.get("welcome_video_url");
		if (url != null) {
			result.put("welcome_video_url", url.getValue());
		}
		
		return result;
	}
	
	@Override
	public Map<String, Object> getAppProps() {
		Map<String, Object> props = new HashMap<>();
		props.put("plugins",                 PluginManager.getInstance().getPluginNames());
		props.put("pluginsDetail",           PluginManager.getInstance().getPlugins());
		props.put("build_version",           appProps.getProperty("buildinfo.version"));
		props.put("build_date",              appProps.getProperty("buildinfo.date"));
		props.put("build_commit_revision",   appProps.getProperty("buildinfo.commit_revision"));
		props.put("cp_coding_enabled",       getBoolSetting("biospecimen", "cp_coding_enabled", false));
		props.put("auto_empi_enabled",       isAutoEmpiEnabled());
		props.put("uid_mandatory",           getBoolSetting("biospecimen", "uid_mandatory", false));
		props.put("feedback_enabled",        getBoolSetting("common", "feedback_enabled", true));
		props.put("mrn_restriction_enabled", getBoolSetting("biospecimen", "mrn_restriction_enabled", false));
		props.put("deploy_env",              getStrSetting("common", "deploy_env"));
		props.put("user_sign_up",            getBoolSetting("administrative", "user_sign_up", true));
		props.put("forgot_password",         getBoolSetting("auth", "forgot_password", true));
		props.put("toast_disp_time",         getIntSetting("common", "toast_disp_time", 5));
		props.put("default_domain",          getStrSetting("auth", "default_domain"));
		props.put("not_specified",           getStrSetting("common", "not_specified_text"));
		props.put("searchDelay",             getIntSetting("common", "search_delay", 1000));
		props.put("allowHtmlMarkup",         getBoolSetting("common", "de_form_html_markup", true));
		props.put("auditEnabled",            appProps.getProperty("app.audit_enabled"));
		props.put("localAccountSignups",     getBoolSetting("administrative", "local_account_signups", true));
		props.put("caseInsensitiveSearch",   !Utility.isMySQL());
		return props;
	}

	@Override
	public String getDataDir() {		
		String dataDir = appProps.getProperty("app.data_dir");
		if (StringUtils.isBlank(dataDir)) {
			dataDir = ".";
		}
		
		return getStrSetting("common", "data_dir", dataDir);
	}
	
	@Override
	public Map<String, String> getPasswordSettings() {
		Map<String, ConfigSetting> moduleConfig = configSettings.get("auth");
		if (moduleConfig == null) {
			return Collections.emptyMap();
		}
		
		Map<String, String> result = new HashMap<>();
		
		ConfigSetting pattern = moduleConfig.get("password_pattern");
		if (pattern != null) {
			result.put("pattern", pattern.getValue());
		}
		
		ConfigSetting desc = moduleConfig.get("password_rule");
		if (desc != null) {
			result.put("desc", desc.getValue());
		}
		
		return result;
	}

	@Override
	public boolean isOracle() {
		return "oracle".equalsIgnoreCase(appProps.getProperty("database.type"));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		reload();
		
		setLocale();
		registerChangeListener("common", new ConfigChangeListener() {			
			@Override
			public void onConfigChange(String name, String value) {				
				if (name.equals("locale")) {
					setLocale();
				}
			}
		});
	}

	@Override
	@PlusTransactional
	public void onApplicationEvent(ContextRefreshedEvent event) {
		reload();
	}

	@Override
	public Map<String, String> getDeploymentSiteAssets() {
		Map<String, String> result = new HashMap<>();
		result.put("siteUrl", getDeploymentSiteUrl());
		result.put("siteLogo", getDeploymentSiteLogo());
		return result;
	}

	@Override
	public void removeSetting(String module, String property) {
		if (configSettings == null) {
			return;
		}

		Map<String, ConfigSetting> moduleSettings = configSettings.get(module);
		if (moduleSettings != null) {
			moduleSettings.remove(property);
		}
	}

	private boolean isValidSetting(ConfigProperty property, String setting) {
		if (StringUtils.isBlank(setting)) {
			return true;
		}
		
		Set<String> allowedValues = property.getAllowedValues();
		if (CollectionUtils.isNotEmpty(allowedValues) && !allowedValues.contains(setting)) {
			return false;
		}
				
		try {
			switch (property.getDataType()) {
				case BOOLEAN:
					Boolean.parseBoolean(setting);
					break;
					
				case FLOAT:
					Double.parseDouble(setting);
					break;
					
				case INT:
					Integer.parseInt(setting);
					break;
					
				case FILE:
					if (setting.startsWith("classpath:")) {
						try (InputStream in = getClass().getResourceAsStream(setting.substring(10))) {
							if (in == null) {
								return false;
							}
						} catch (Exception e) {
							return false;
						}
					} else {
						File settingsDir = getSettingFilesDir();
						Path filePath = settingsDir.toPath().resolve(setting);
						if (!filePath.normalize().startsWith(settingsDir.toPath())) {
							throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Setting filename contains one or more path traversal sequences. Value = " + setting);
						}

						File file = filePath.toFile();
						if (!file.exists()) {
							throw new FileNotFoundException("File at path " + file.getAbsolutePath() + " does not exists");
						}
					}
					break;

				default:
					break;
			}
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	private ConfigSetting createSetting(ConfigSetting existing, String value) {
		ConfigSetting newSetting = new ConfigSetting();
		newSetting.setProperty(existing.getProperty());
		newSetting.setActivatedBy(AuthUtil.getCurrentUser());
		newSetting.setActivationDate(Calendar.getInstance().getTime());
		newSetting.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		
		if (value != null && existing.getProperty().isSecured()) {
			value = Utility.encrypt(value);
		}
		
		newSetting.setValue(value);
		return newSetting;
	}

	private void notifyListeners(String module, String property, String setting) {
		List<ConfigChangeListener> toNotifyListeners = new ArrayList<>();
		List<ConfigChangeListener> listeners = changeListeners.get(module);
		if (listeners != null) {
			toNotifyListeners.addAll(listeners);
		}

		listeners = changeListeners.get("*");
		if (listeners != null) {
			toNotifyListeners.addAll(listeners);
		}

		toNotifyListeners.forEach(listener -> listener.onConfigChange(property, setting));
	}
	
	private void setLocale() {
		Locale existingLocale = Locale.getDefault();
		String setting = getStrSetting("common", "locale", existingLocale.toString());
		Locale newLocale = LocaleUtils.toLocale(setting);

		if (!existingLocale.equals(newLocale)) {
			Locale.setDefault(newLocale);
		}

		logger.info("App is using the locale: " + newLocale.toString());
	}
	
	private boolean isAutoEmpiEnabled() {
		return StringUtils.isNotBlank(getStrSetting("biospecimen", "mpi_format", "")) || 
				StringUtils.isNotBlank(getStrSetting("biospecimen", "mpi_generator", ""));
	}

	private File getSettingFilesDir() {
		try {
			Path dir = new File(getDataDir()).toPath().resolve("config-setting-files");
			if (!dir.normalize().startsWith(getDataDir())) {
				throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Relative data directory paths not allowed");
			}

			Files.createDirectories(dir);
			return dir.toFile();
		} catch (IOException e) {
			throw OpenSpecimenException.serverError(new RuntimeException("Error getting settings file directory.", e));
		}
	}

	private void deleteOldSettingFile(ConfigSetting oldSetting) {
		if (!oldSetting.getProperty().isFile() || StringUtils.isBlank(oldSetting.getValue())) {
			return;
		}

		File settingsDir = getSettingFilesDir();
		Path oldSettingFile = settingsDir.toPath().resolve(oldSetting.getValue());
		if (!oldSettingFile.normalize().startsWith(settingsDir.toPath())) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Filenames with path traversal characters not allowed");
		}

		try {
			Files.deleteIfExists(oldSettingFile);
		} catch (Exception e) {
			logger.error("Error deleting the old setting file: " + oldSettingFile.toString());
		}
	}

	private String getDeploymentSiteUrl() {
		Map<String, ConfigSetting> moduleConfig = configSettings.get("common");
		if (moduleConfig == null) {
			return null;
		}

		ConfigSetting url = moduleConfig.get("deployment_site_url");
		return url != null ? url.getValue() : null;
	}

	private String getDeploymentSiteLogo() {
		Map<String, ConfigSetting> moduleConfig = configSettings.get("common");
		if (moduleConfig == null) {
			return null;
		}

		String result = null;
		ConfigSetting logo = moduleConfig.get("deployment_site_logo");
		if (logo != null && StringUtils.isNotBlank(logo.getValue())) {
			ConfigSetting appUrl = moduleConfig.get("app_url");
			String prefix = "";
			if (appUrl != null && StringUtils.isNotBlank(appUrl.getValue())) {
				prefix = appUrl.getValue();
			}

			result = prefix + "/rest/ng/config-settings/deployment-site-logo";
		}

		return result;
	}

	private String getDeFeDateFormat() {
		String dateFmt       = getDeDateFormat();
		StringBuilder result = new StringBuilder();
		int lastIdx          = 0;

		Matcher matcher = DATE_FORMAT.matcher(dateFmt);
		while (matcher.find()) {
			String replacement = "";
			String match = matcher.group();

			if (matcher.group("day") != null) {
				if (match.length() < 2) {
					replacement = "d";
				} else {
					replacement = "dd";
				}
			}

			if (matcher.group("month") != null) {
				switch (match.length()) {
					case 1:
						replacement = "m";
						break;

					case 2:
						replacement = "mm";
						break;

					case 3:
						replacement = "M";
						break;

					default:
						replacement = "MM";
						break;
				}
			}

			if (matcher.group("year") != null) {
				switch (match.length()) {
					case 2:
						replacement = "yy";
						break;

					case 4:
					default:
						replacement = "yyyy";
						break;
				}
			}

			if (matcher.group("week") != null) {
				switch (match.length()) {
					case 3:
						replacement = "D";
						break;

					case 4:
					default:
						replacement = "DD";
						break;
				}
			}

			result.append(dateFmt, lastIdx, matcher.start());
			result.append(replacement);
			lastIdx = matcher.end();
		}

		return result.toString();
	}
}