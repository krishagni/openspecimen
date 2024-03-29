package com.krishagni.catissueplus.core.biospecimen.services;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.events.SpecimenTypeUnitDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenTypeUnitsListCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface SpecimenTypeUnitsService {
	ResponseEvent<List<SpecimenTypeUnitDetail>> getUnits(RequestEvent<SpecimenTypeUnitsListCriteria> req);

	ResponseEvent<Long> getUnitsCount(RequestEvent<SpecimenTypeUnitsListCriteria> req);

	ResponseEvent<SpecimenTypeUnitDetail> createUnit(RequestEvent<SpecimenTypeUnitDetail> req);

	ResponseEvent<SpecimenTypeUnitDetail> updateUnit(RequestEvent<SpecimenTypeUnitDetail> req);

	ResponseEvent<SpecimenTypeUnitDetail> deleteUnit(RequestEvent<SpecimenTypeUnitDetail> req);
}
