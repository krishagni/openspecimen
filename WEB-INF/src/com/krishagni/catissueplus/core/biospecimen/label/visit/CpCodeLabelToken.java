package com.krishagni.catissueplus.core.biospecimen.label.visit;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Visit;

public class CpCodeLabelToken extends AbstractVisitLabelToken {

	public CpCodeLabelToken() {
		this.name = "CP_CODE";
	}

	@Override
	public String getLabel(Visit visit, String... args) {
		String code = visit.getCollectionProtocol().getCode();
		if (StringUtils.isBlank(code)) {
			code = StringUtils.EMPTY;
		}

		return code;
	}
}
