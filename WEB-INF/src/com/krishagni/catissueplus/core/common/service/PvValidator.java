package com.krishagni.catissueplus.core.common.service;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.repository.PermissibleValueDao;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;

public class PvValidator {
	
	private static final PvValidator instance = new PvValidator();
	
	private DaoFactory daoFactory;
	 
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	public DaoFactory getDaoFactory() {
		return daoFactory;
	}
	
	public static PvValidator getInstance() {
		return instance;
	}
	
	public static boolean isValid(String attribute, String value) {
		return isValid(attribute, value, false);
	}
	
	public static boolean isValid(String attribute, String value, boolean leafCheck) {
		if (StringUtils.isBlank(value)) {
			return true;
		}
		
		return getInstance().getPvDao().exists(attribute, Collections.singleton(value), leafCheck);
	}
	
	public static boolean isValid(String attribute, String parentValue, String value) {
		if (StringUtils.isBlank(value)) {
			return true;
		}
		
		return getInstance().getPvDao().exists(attribute, parentValue, Collections.singleton(value));
	}

	public static boolean isValid(String attribute, int depth, String value) {
		return isValid(attribute, depth, value, false);
	}
	
	public static boolean isValid(String attribute, int depth, String value, boolean anyLevel) {
		if (StringUtils.isBlank(value)) {
			return true;
		}
		
		return getInstance().getPvDao().exists(attribute, depth, Collections.singleton(value), anyLevel);
	}

	public static boolean areValid(String attribute, Collection<String> values) {
		if (CollectionUtils.isEmpty(values)) {
			return true;
		}
		
		return getInstance().getPvDao().exists(attribute, values);
	}

	public static boolean areValid(String attribute, int depth, Collection<String> values) {
		if (CollectionUtils.isEmpty(values)) {
			return true;
		}
		
		return getInstance().getPvDao().exists(attribute, depth, values);
	}
	
	private PermissibleValueDao getPvDao() {
		return getDaoFactory().getPermissibleValueDao();
	}
}
