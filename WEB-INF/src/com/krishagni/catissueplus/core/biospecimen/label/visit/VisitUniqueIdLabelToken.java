package com.krishagni.catissueplus.core.biospecimen.label.visit;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class VisitUniqueIdLabelToken extends AbstractUniqueIdToken<Visit> {

	@Autowired
	private DaoFactory daoFactory;
			
	public VisitUniqueIdLabelToken() {
		this.name = "SYS_UID";
		this.useSysGenFastSetting = true;
	}

	@Override
	public Number getUniqueId(Visit visit, String... args) {
		return daoFactory.getUniqueIdGenerator().getUniqueId("Visit", getName());
	}
}