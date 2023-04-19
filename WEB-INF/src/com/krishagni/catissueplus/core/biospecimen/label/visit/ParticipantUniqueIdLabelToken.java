package com.krishagni.catissueplus.core.biospecimen.label.visit;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class ParticipantUniqueIdLabelToken extends AbstractUniqueIdToken<Visit> {

	@Autowired
	private DaoFactory daoFactory;

	public ParticipantUniqueIdLabelToken() {
		super();
		this.type = "VISIT_NAME";
		this.name = "PUID";
	}

	@Override
	public Number getUniqueId(Visit visit, String... args) {
		Long pid = visit.getRegistration().getParticipant().getId();
		return daoFactory.getUniqueIdGenerator().getUniqueId(getTypeKey(), pid.toString());
	}
}
