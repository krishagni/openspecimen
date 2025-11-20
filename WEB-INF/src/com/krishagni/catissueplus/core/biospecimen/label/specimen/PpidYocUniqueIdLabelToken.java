package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class PpidYocUniqueIdLabelToken extends AbstractUniqueIdToken<Specimen> {

	@Autowired
	private DaoFactory daoFactory;
	
	public PpidYocUniqueIdLabelToken() {
		this.name = "PPI_YOC_UID";
	}


	@Override
	public Number getUniqueId(Specimen specimen, String... args) {
		while (specimen.isAliquot() || specimen.isDerivative()) {
			specimen = specimen.getParentSpecimen();
		}

		Calendar cal = Calendar.getInstance();
		if (specimen.getCollectionTime() != null) {
			cal.setTime(specimen.getCollectionTime());
		} else if (specimen.getCreatedOn() != null) {
			cal.setTime(specimen.getCreatedOn());
		}

		String ppid = specimen.getRegistration().getPpid();
		int yoc = cal.get(Calendar.YEAR);
		String key = ppid + "_" + yoc;
		return daoFactory.getUniqueIdGenerator().getUniqueId(getTypeKey(), key);
	}
}
