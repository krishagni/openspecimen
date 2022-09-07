package com.krishagni.catissueplus.core.biospecimen.label.visit;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;

public class EventCodeLabelToken extends AbstractVisitLabelToken {

	public EventCodeLabelToken() {
		this.name = "EVENT_CODE";
	}

	@Override
	public String getLabel(Visit visit, String... args) {
		CollectionProtocolEvent cpe = visit.getCpEvent();
		return cpe != null && StringUtils.isNotBlank(cpe.getCode()) ? cpe.getCode() : StringUtils.EMPTY;
	}
}
