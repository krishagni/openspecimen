package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class VisitUidLabelToken extends AbstractUniqueIdToken<Specimen> {
	@Autowired
	private DaoFactory daoFactory;

	public VisitUidLabelToken() {
		this.name = "VISIT_UID";
	}

	@Override
	public Number getUniqueId(Specimen specimen, String... args) {
		String key = null;
		if (specimen.getCollectionProtocol().useLabelsAsSequenceKey()) {
			key = specimen.getCpId() + "_" + specimen.getVisit().getName();
		} else {
			key = specimen.getVisit().getId().toString();
		}

		return daoFactory.getUniqueIdGenerator().getUniqueId(getTypeKey(), key);
	}
}
