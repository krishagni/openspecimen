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

	@Override
	public List<Object[]> getOccupiedPositionOrdinals(Long containerId) {
		return createNamedQuery(GET_OCC_POS_ORDINALS, Object[].class)
			.setParameter("containerId", containerId)
			.list();
	}

	@Override
	public StorageContainerPosition getPosition(Long containerId, Integer posOneOrdinal, Integer posTwoOrdinal) {
		List<StorageContainerPosition> rows = createNamedQuery(GET_POS_BY_ORDINALS, StorageContainerPosition.class)
			.setParameter("containerId", containerId)
			.setParameter("posOne", posOneOrdinal)
			.setParameter("posTwo", posTwoOrdinal)
			.list();
		return rows != null && !rows.isEmpty() ? rows.get(0) : null;
	}

	@Override
	public StorageContainerPosition getReservedPosition(Long containerId, String row, String column, String reservationId) {
		List<StorageContainerPosition> rows = createNamedQuery(GET_RESERVED_POS, StorageContainerPosition.class)
			.setParameter("containerId", containerId)
			.setParameter("rowLabel", row)
			.setParameter("colLabel", column)
			.setParameter("reservationId", reservationId)
			.list();
		return rows != null && !rows.isEmpty() ? rows.get(0) : null;
	}

	@Override
	public boolean hasPositionsBeyondCapacity(Long containerId, int maxRow, int maxCol) {
		Long count = createNamedQuery(GET_POS_BEYOND_CAPACITY, Long.class)
			.setParameter("containerId", containerId)
			.setParameter("maxRow", maxRow)
			.setParameter("maxCol", maxCol)
			.uniqueResult();
		return count != null && count > 0;
	}

	private static final String FQN = StorageContainerPosition.class.getName();

	private static final String GET_SPMN_ID_BY_POS = FQN + ".getSpecimenIdByPosition";

	private static final String GET_OCC_POS_ORDINALS = FQN + ".getOccupiedPositionOrdinals";

	private static final String GET_POS_BY_ORDINALS = FQN + ".getPositionByOrdinals";

	private static final String GET_RESERVED_POS = FQN + ".getReservedPosition";

	private static final String GET_POS_BEYOND_CAPACITY = FQN + ".getPositionsBeyondCapacity";
}
