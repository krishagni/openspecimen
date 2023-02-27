package com.krishagni.catissueplus.core.biospecimen.label.visit;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class PpidUniqueIdLabelToken extends AbstractUniqueIdToken<Visit> {

	@Autowired
	private DaoFactory daoFactory;

	public PpidUniqueIdLabelToken() {
		this.name = "PPI_UID";
	}

	@Override
	public Number getUniqueId(Visit visit, String... args) {
		Long cprId = visit.getRegistration().getId();
		return daoFactory.getUniqueIdGenerator().getUniqueId(getTypeKey(), cprId.toString());
	}
}
