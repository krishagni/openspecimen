package com.krishagni.catissueplus.core.biospecimen.label.visit;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.AbstractCustomFieldLabelToken;

public class VisitCustomFieldLabelToken extends AbstractCustomFieldLabelToken {
	@Override
	protected BaseExtensionEntity getObject(Object object, String level) {
		Visit visit = (Visit) object;

		if (level.equals("visit")) {
			return visit;
		} else if (level.equals("cpr")) {
			return visit.getRegistration();
		} else if (level.equals("cp")) {
			return visit.getCollectionProtocol();
		}

		return null;
	}
}
