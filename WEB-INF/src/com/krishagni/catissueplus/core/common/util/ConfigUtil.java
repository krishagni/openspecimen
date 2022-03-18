package com.krishagni.catissueplus.core.common.util;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.common.service.ConfigurationService;

@Configurable
public class ConfigUtil {
	private static ConfigUtil instance = null;
	
	@Autowired
	private ConfigurationService cfgSvc;
		
	public static ConfigUtil getInstance() {
		if (instance == null || instance.cfgSvc == null) {
			//
			// instance.cfgSvc == null is defensive check added, which is useful
			// when app is incorrectly wired
			//
			instance = new ConfigUtil();
		}
		
		return instance;
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
}
