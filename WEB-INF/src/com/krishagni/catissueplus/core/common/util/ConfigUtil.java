package com.krishagni.catissueplus.core.common.util;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;

@Configurable
public class ConfigUtil {
	private static final String SETTING_CHANGED_TMPL = "config_setting_changed";

	private static ConfigUtil instance = null;
	
	@Autowired
	private ConfigurationService cfgSvc;

	@Autowired
	private DaoFactory daoFactory;
		
	public static ConfigUtil getInstance() {
		if (instance == null || instance.cfgSvc == null || instance.daoFactory == null) {
			//
			// instance.cfgSvc == null is defensive check added, which is useful
			// when app is incorrectly wired
			//
			instance = new ConfigUtil();
		}
		
		return instance;
	}

	public static void notifyChange(String setting) {
		notifyChange(setting, null);
	}

	public static void notifyChange(String setting, String value) {
		getInstance().notifySettingChange(setting, value);
	}
	
	public String getAppUrl() {
		return cfgSvc.getStrSetting("common", "app_url", "");
	}

	public String getDeployEnv() {
		return ConfigUtil.getInstance().getStrSetting("common", "deploy_env", "");
	}
	
	public String getDataDir() {
		return cfgSvc.getDataDir();
	}

	public File getReportsDir() {
		File dir = new File(getDataDir(), "reports");
		if (!dir.exists()) {
			dir.mkdirs();
		}

		return dir;
	}

	public File getTempDir() {
		File tmpDir = new File(getDataDir(), "tmp");
		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}

		return tmpDir;
	}
	
	public String getAdminEmailId() {
		return cfgSvc.getStrSetting("email", "admin_email_id", "");
	}

	public String getItAdminEmailId() {
		String emailId = cfgSvc.getStrSetting("email", "it_admin_email_id", "");
		if (StringUtils.isBlank(emailId)) {
			emailId = getAdminEmailId();
		}

		return emailId;
	}

	public String getDateFmt() {
		return cfgSvc.getDateFormat();
	}
	
	public String getDeDateFmt() {
		return cfgSvc.getDeDateFormat();
	}
	
	public String getTimeFmt() {
		return cfgSvc.getTimeFormat();
	}
	
	public String getDateTimeFmt() {
		return getDateFmt() + " " + getTimeFmt();
	}

	public String getStrSetting(String module, String name) {
		return getStrSetting(module, name, null);
	}

	public String getStrSetting(String module, String name, String defValue) {
		return cfgSvc.getStrSetting(module, name, defValue);
	}
	
	public Character getCharSetting(String module, String name, char defValue) {
		return cfgSvc.getCharSetting(module, name, defValue);
	}
	
	public Boolean getBoolSetting(String module, String name, Boolean defValue) {
		return cfgSvc.getBoolSetting(module, name, defValue);
	}
	
	public Integer getIntSetting(String module, String name, Integer defValue) {
		return cfgSvc.getIntSetting(module, name, defValue);
	}

	public File getFileSetting(String module, String name, File defValue) {
		return cfgSvc.getSettingFile(module, name, defValue);
	}

	public String getFileContent(String module, String name, File defValue) {
		return cfgSvc.getFileContent(module, name, defValue);
	}

	public boolean isOracle() {
		return cfgSvc.isOracle();
	}

	private void notifySettingChange(String setting, String value) {
		List<User> admins = daoFactory.getUserDao().getSuperAndInstituteAdmins(null);
		if (admins == null || admins.isEmpty()) {
			return;
		}

		Map<String, Object> props = new HashMap<>();
		User currentUser = AuthUtil.getCurrentUser();
		props.put("setting", setting);
		props.put("value", value);
		props.put("changedByName", currentUser != null ? currentUser.formattedName() : "System");
		props.put("changedOn", Calendar.getInstance().getTime());
		props.put("$subject", new String[] { setting });

		for (User admin : admins) {
			EmailUtil.getInstance().sendEmail(SETTING_CHANGED_TMPL, new String[] { admin.getEmailAddress() }, null, props);
		}
	}
}
