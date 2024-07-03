package com.krishagni.catissueplus.core.common.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.domain.ConfigSetting;

public interface ConfigSettingDao extends Dao<ConfigSetting> {
	
	List<ConfigSetting> getAllSettings();
	
	List<ConfigSetting> getAllSettingsByModule(String moduleName);

	List<ConfigSetting> getSettingsLaterThan(Long settingId);

	List<ConfigSetting> getHistory(String module, String property);
}
