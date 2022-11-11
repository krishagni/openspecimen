package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class CpPpidUniqueIdLabelToken extends AbstractUniqueIdToken<Specimen> {

	@Autowired
	private DaoFactory daoFactory;

	public CpPpidUniqueIdLabelToken() {
		this.name = "CP_PPI_UID";
	}

	@Override
	public Number getUniqueId(Specimen specimen, String... args) {
		String ppid = specimen.getVisit().getRegistration().getPpid();
		String cpId = specimen.getCollectionProtocol().getId().toString();
		return daoFactory.getUniqueIdGenerator().getUniqueId(getTypeKey(), cpId + "_" + ppid);
	}
}
