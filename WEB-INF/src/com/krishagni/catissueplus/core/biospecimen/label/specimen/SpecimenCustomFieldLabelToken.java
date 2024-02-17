package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.AbstractCustomFieldLabelToken;

public class SpecimenCustomFieldLabelToken extends AbstractCustomFieldLabelToken {
	@Override
	protected BaseExtensionEntity getObject(Object object, String level) {
		Specimen specimen = (Specimen) object;

		if (level.equals("specimen")) {
			return specimen;
		} else if (level.equals("parentSpecimen")) {
			return specimen.getParentSpecimen();
		} else if (level.equals("visit")) {
			return specimen.getVisit();
		} else if (level.equals("cpr")) {
			return specimen.getRegistration();
		} else if (level.equals("cp")) {
			return specimen.getCollectionProtocol();
		}

		return null;
	}
}
