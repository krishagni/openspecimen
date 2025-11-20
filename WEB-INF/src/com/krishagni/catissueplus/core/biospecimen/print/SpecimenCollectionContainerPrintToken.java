package com.krishagni.catissueplus.core.biospecimen.print;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;


public class SpecimenCollectionContainerPrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "specimen_collection_container";
	}

	@Override
	public String getReplacement(Object object) {
		Specimen specimen = (Specimen)object;
		while (specimen.getParentSpecimen() != null) {
			specimen = specimen.getParentSpecimen();
		}

		return specimen.getCollectionContainer().getValue();
	}

}
