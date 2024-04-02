package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum SpecimenTypeUnitError implements ErrorCode {
	ID_REQ,

	NOT_FOUND,

	QTY_OR_CONC_UNIT_REQ,

	DUP;

	@Override
	public String code() {
		return "SPECIMEN_TYPE_UNIT_" + this.name();
	}
}
