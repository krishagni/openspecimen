package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.common.domain.ConfigSetting;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.ConfigSettingDao;

public class ConfigSettingDaoImpl extends AbstractDao<ConfigSetting> implements ConfigSettingDao {

	@Override
	public Class<ConfigSetting> getType() {
		return ConfigSetting.class;
	}

	@Override
	public List<ConfigSetting> getAllSettings() {
		return createNamedQuery(GET_ALL, ConfigSetting.class).list();
	}

	@Override
	public List<ConfigSetting> getAllSettingsByModule(String moduleName) {
		return createNamedQuery(GET_ALL_BY_MODULE, ConfigSetting.class)
			.setParameter("name", moduleName)
			.list();
	}

	@Override
	public List<ConfigSetting> getSettingsLaterThan(Long settingId) {
		return createNamedQuery(GET_ALL_LATER_THAN, ConfigSetting.class)
			.setParameter("settingId", settingId)
			.list();
	}

	@Override
	public List<ConfigSetting> getHistory(String module, String property) {
		return createNamedQuery(GET_ALL_PROP_SETTINGS, ConfigSetting.class)
			.setParameter("moduleName", module)
			.setParameter("propName", property)
			.list();
	}

	private static final String FQN = ConfigSetting.class.getName();
	
	private static final String GET_ALL = FQN + ".getAll";
	
	private static final String GET_ALL_BY_MODULE = FQN + ".getAllByModule";

	private static final String GET_ALL_LATER_THAN = FQN + ".getAllLaterThan";

	private static final String GET_ALL_PROP_SETTINGS = FQN + ".getAllPropertySettings";
}
