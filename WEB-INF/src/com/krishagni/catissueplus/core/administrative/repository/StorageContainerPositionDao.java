package com.krishagni.catissueplus.core.administrative.repository;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface StorageContainerPositionDao extends Dao<StorageContainerPosition> {
	Long getSpecimenIdByPosition(Long containerId, String row, String column);

	List<Object[]> getOccupiedPositionOrdinals(Long containerId);

	StorageContainerPosition getPosition(Long containerId, Integer posOneOrdinal, Integer posTwoOrdinal);

	StorageContainerPosition getReservedPosition(Long containerId, String row, String column, String reservationId);

	boolean hasPositionsBeyondCapacity(Long containerId, int maxRow, int maxCol);
}
