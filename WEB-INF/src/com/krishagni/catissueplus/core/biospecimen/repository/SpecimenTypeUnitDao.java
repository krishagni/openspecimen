package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenTypeUnit;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface SpecimenTypeUnitDao extends Dao<SpecimenTypeUnit> {
	List<SpecimenTypeUnit> getUnits(SpecimenTypeUnitsListCriteria crit);

	Long getUnitsCount(SpecimenTypeUnitsListCriteria crit);

	SpecimenTypeUnit getUnit(Long cpId, Long specimenClassId, Long typeId);

	List<SpecimenTypeUnit> getMatchingUnits(Long cpId, Long specimenClassId, Long typeId);

	List<SpecimenTypeUnit> getMatchingUnits(String cpShortTitle, String specimenClass, String type);
}
