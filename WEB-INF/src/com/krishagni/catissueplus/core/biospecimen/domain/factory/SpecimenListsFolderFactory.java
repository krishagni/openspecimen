package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListsFolder;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListsFolderDetail;

public interface SpecimenListsFolderFactory {
	SpecimenListsFolder createFolder(SpecimenListsFolderDetail input);
}
