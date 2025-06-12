package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.common.PvAttributes;

@Configurable
public class SpecimenDisposalEvent extends SpecimenEvent {
	private PermissibleValue reason;

	public SpecimenDisposalEvent(Specimen specimen) {
		super(specimen);
	}

	public PermissibleValue getReason() {
		loadRecordIfNotLoaded();
		return reason;
	}

	public void setReason(PermissibleValue reason) {
		loadRecordIfNotLoaded();
		this.reason = reason;;
	}

	@Override
	protected Map<String, Object> getEventAttrs() {
		Map<String, Object> attrs = new HashMap<>();
		attrs.put("reason", reason != null ? reason.getId().toString() : null);
		return attrs;
	}

	@Override
	protected void setEventAttrs(Map<String, Object> attrValues) {
		String reasonIdStr = (String)attrValues.get("reason");
		if (StringUtils.isNotBlank(reasonIdStr)) {
			if (StringUtils.isNumeric(reasonIdStr)) {
				this.reason = daoFactory.getPermissibleValueDao().getById(Long.parseLong(reasonIdStr));
			} else {
				this.reason = daoFactory.getPermissibleValueDao().getByValue(PvAttributes.SPECIMEN_DISPOSE_REASON, reasonIdStr);
			}
		}
	}

	@Override
	public String getFormName() {
		return "SpecimenDisposalEvent";
	}
}