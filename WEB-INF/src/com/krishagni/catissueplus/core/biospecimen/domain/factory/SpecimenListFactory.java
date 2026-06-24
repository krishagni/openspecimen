package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListDetail;

public interface SpecimenListFactory {
	public SpecimenList createSpecimenList(SpecimenListDetail details);
	
	public SpecimenList createSpecimenList(SpecimenList existing, SpecimenListDetail details);

	public SpecimenList createDefaultSpecimenList(User user);
}
