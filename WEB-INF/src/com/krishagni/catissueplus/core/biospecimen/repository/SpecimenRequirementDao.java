package com.krishagni.catissueplus.core.biospecimen.repository;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface SpecimenRequirementDao extends Dao<SpecimenRequirement> {
	SpecimenRequirement getSpecimenRequirement(Long id);

	int getSpecimensCount(Long srId);

	int getAllSpecimensCount(Long srId);

	SpecimenRequirement getByCpEventLabelAndSrCode(String cpShortTitle, String eventLabel, String code);

	SpecimenRequirement getByCpEventLabelAndSrCode(Long cpId, String eventLabel, String code);
}
