package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;

public class EventCodeLabelToken extends AbstractSpecimenLabelToken {

	public EventCodeLabelToken() {
		this.name = "EVENT_CODE";
	}

	@Override
	public String getLabel(Specimen specimen) {
		CollectionProtocolEvent cpe = specimen.getVisit().getCpEvent();
		return cpe != null && StringUtils.isNotBlank(cpe.getCode()) ? cpe.getCode() : StringUtils.EMPTY;
	}
}
