package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class SpecimenCpUniqueIdToken extends AbstractUniqueIdToken<Specimen> {

	@Autowired
	private DaoFactory daoFactory;

	public SpecimenCpUniqueIdToken() {
		this.name = "SPEC_CP_UID";
	}

	@Override
	public Number getUniqueId(Specimen specimen, String... args) {
		return daoFactory.getUniqueIdGenerator().getUniqueId(getTypeKey(), specimen.getCpId().toString());
	}
}
