package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class PpidUniqueIdLabelToken extends AbstractUniqueIdToken<Specimen> {
	
	@Autowired
	private DaoFactory daoFactory;
	
	public PpidUniqueIdLabelToken() {
		this.name = "PPI_UID";
	}

	@Override
	public Number getUniqueId(Specimen specimen, String... args) {
		String ppid = specimen.getRegistration().getPpid();
		return daoFactory.getUniqueIdGenerator().getUniqueId(getTypeKey(), ppid);
	}
}
