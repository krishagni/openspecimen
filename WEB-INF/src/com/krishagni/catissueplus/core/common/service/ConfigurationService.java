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
	
	ResponseEvent<List<ConfigSettingDetail>> getSettings(RequestEvent<String> req);

	ResponseEvent<ConfigSettingDetail> getSetting(RequestEvent<Pair<String, String>> req);
	
	ResponseEvent<ConfigSettingDetail> saveSetting(RequestEvent<ConfigSettingDetail> req);

	ResponseEvent<File> getSettingFile(RequestEvent<Pair<String, String>> req);

	ResponseEvent<String> uploadSettingFile(RequestEvent<FileDetail> req);

	ResponseEvent<List<ConfigSettingDetail>> getSettingHistory(RequestEvent<Pair<String, String>> req);
		
	//
	// Internal to app APIs
	//
	Integer getIntSetting(String module, String name, Integer ... defValue);
	
	Double getFloatSetting(String module, String name, Double ... defValue);

	String getStrSetting(String module, String name, String ... defValue);
	
	Character getCharSetting(String module, String name, Character... defValue);
	
	Boolean getBoolSetting(String module, String name, Boolean ... defValue);
	
	File getSettingFile(String module, String name, File ... defValue);

	String getFileContent(String module, String name, File... defValue);

	FileDetail getFileDetail(String module, String name, File... defValue);

	void downloadSettingFile(String moduleName, String propertyName, HttpServletResponse httpResp);

	void reload();
	
	void registerChangeListener(String module, ConfigChangeListener callback);
	
	Map<String, Object> getLocaleSettings();
	
	String getDateFormat();
	
	String getDeDateFormat();
	
	String getTimeFormat();
	
	String getDeDateTimeFormat();
	
	Map<String, String> getWelcomeVideoSettings();
	
	Map<String, Object> getAppProps();
	
	String getDataDir();
	
	Map<String, String> getPasswordSettings();

	boolean isOracle();

	Map<String, String> getDeploymentSiteAssets();

	void removeSetting(String module, String property);
}
