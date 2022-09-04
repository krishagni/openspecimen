package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.AutoFreezerProvider;
import com.krishagni.catissueplus.core.administrative.repository.AutoFreezerProviderDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class AutoFreezerProviderDaoImpl extends AbstractDao<AutoFreezerProvider> implements AutoFreezerProviderDao {

	@Override
	public List<AutoFreezerProvider> getAutomatedFreezers() {
		return createCriteria(AutoFreezerProvider.class, "f").list();
	}

	@Override
	public AutoFreezerProvider getByName(String name) {
		return createNamedQuery(GET_BY_NAME, AutoFreezerProvider.class)
			.setParameter("name", name)
			.uniqueResult();
	}

	@Override
	public Class<?> getType() {
		return AutoFreezerProvider.class;
	}

	private static final String FQN = AutoFreezerProvider.class.getName();

	private static final String GET_BY_NAME = FQN + ".getByName";
}
