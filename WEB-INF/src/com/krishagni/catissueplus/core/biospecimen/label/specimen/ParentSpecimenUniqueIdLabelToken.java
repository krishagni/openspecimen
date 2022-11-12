package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class ParentSpecimenUniqueIdLabelToken extends AbstractUniqueIdToken<Specimen> {

	@Autowired
	private DaoFactory daoFactory;
	
	public ParentSpecimenUniqueIdLabelToken() {
		this.name = "PSPEC_UID";
	}

	@Override
	public Number getUniqueId(Specimen specimen, String... args) {
		if (specimen.getParentSpecimen() == null) {
			return null;
		}

		String pidStr = null;
		if (specimen.getCollectionProtocol().useLabelsAsSequenceKey()) {
			pidStr = specimen.getCpId() + "_" + specimen.getParentSpecimen().getLabel();
		} else {
			pidStr = specimen.getParentSpecimen().getId().toString();
		}

		return daoFactory.getUniqueIdGenerator().getUniqueId(getTypeKey(), pidStr);
	}
}
