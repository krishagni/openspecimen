package com.krishagni.catissueplus.core.common.service;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.events.ConfigSettingDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface ConfigurationService {
	
	public ResponseEvent<List<ConfigSettingDetail>> getSettings(RequestEvent<String> req);

	public ResponseEvent<ConfigSettingDetail> getSetting(RequestEvent<Pair<String, String>> req);
	
	public ResponseEvent<ConfigSettingDetail> saveSetting(RequestEvent<ConfigSettingDetail> req);

	public ResponseEvent<File> getSettingFile(RequestEvent<Pair<String, String>> req);

	public ResponseEvent<String> uploadSettingFile(RequestEvent<FileDetail> req);
		
	//
	// Internal to app APIs
	//
	public Integer getIntSetting(String module, String name, Integer ... defValue);
	
	public Double getFloatSetting(String module, String name, Double ... defValue);

	public String getStrSetting(String module, String name, String ... defValue);
	
	public Character getCharSetting(String module, String name, Character... defValue);
	
	public Boolean getBoolSetting(String module, String name, Boolean ... defValue);
	
	public File getSettingFile(String module, String name, File ... defValue);

	public String getFileContent(String module, String name, File... defValue);

	public FileDetail getFileDetail(String module, String name, File... defValue);

	void downloadSettingFile(String moduleName, String propertyName, HttpServletResponse httpResp);

	public void reload();
	
	public void registerChangeListener(String module, ConfigChangeListener callback);
	
	public Map<String, Object> getLocaleSettings();
	
	public String getDateFormat();
	
	public String getDeDateFormat();
	
	public String getTimeFormat();
	
	public String getDeDateTimeFormat();
	
	public Map<String, String> getWelcomeVideoSettings();
	
	public Map<String, Object> getAppProps();
	
	public String getDataDir();
	
	Map<String, String> getPasswordSettings();

	boolean isOracle();

	Map<String, String> getDeploymentSiteAssets();

	void removeSetting(String module, String property);
}
