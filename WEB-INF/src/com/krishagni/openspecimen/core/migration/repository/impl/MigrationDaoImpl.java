package com.krishagni.openspecimen.core.migration.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.openspecimen.core.migration.domain.Migration;
import com.krishagni.openspecimen.core.migration.repository.MigrationDao;

public class MigrationDaoImpl extends AbstractDao<Migration> implements MigrationDao {

	@Override
	public Class<?> getType() {
		return Migration.class;
	}
	
	@Override
	public Migration getMigrationInfo(String name, String versionFrom,
			String versionTo) {
		List<Migration> result = getCurrentSession().createNamedQuery(GET_MIGRATION_INFO, Migration.class)
			.setParameter("name", name)
			.setParameter("versionFrom", versionFrom)
			.setParameter("versionTo", versionTo)
			.list();
		return result.isEmpty() ? null : result.get(0);
	}

	private static final String FQN = Migration.class.getName();
	
	private static final String GET_MIGRATION_INFO = FQN + ".getMigrationInfo";
}
