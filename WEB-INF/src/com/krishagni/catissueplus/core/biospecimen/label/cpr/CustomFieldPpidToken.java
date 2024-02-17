package com.krishagni.catissueplus.core.biospecimen.label.cpr;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.common.AbstractCustomFieldLabelToken;

public class CustomFieldPpidToken extends AbstractCustomFieldLabelToken {

	@Override
	protected BaseExtensionEntity getObject(Object object, String level) {
		CollectionProtocolRegistration cpr = (CollectionProtocolRegistration) object;
		if (level.equals("cpr")) {
			return cpr;
		} else if (level.equals("cp")) {
			return cpr.getCollectionProtocol();
		}

		return null;
	}
}
