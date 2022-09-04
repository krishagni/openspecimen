package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerPositionDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class StorageContainerPositionDaoImpl extends AbstractDao<StorageContainerPosition> implements StorageContainerPositionDao {
	@Override
	public Class<StorageContainerPosition> getType() {
		return StorageContainerPosition.class;
	}

	@Override
	public Long getSpecimenIdByPosition(Long containerId, String row, String column) {
		List<Long> rows = createNamedQuery(GET_SPMN_ID_BY_POS, Long.class)
			.setParameter("containerId", containerId)
			.setParameter("rowLabel", row)
			.setParameter("colLabel", column)
			.list();
		return rows != null && !rows.isEmpty() ? rows.get(0) : null;
	}

	private static final String FQN = StorageContainerPosition.class.getName();

	private static final String GET_SPMN_ID_BY_POS = FQN + ".getSpecimenIdByPosition";
}
