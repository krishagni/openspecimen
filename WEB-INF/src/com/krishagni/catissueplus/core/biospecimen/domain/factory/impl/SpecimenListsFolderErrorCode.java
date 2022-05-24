package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum SpecimenListsFolderErrorCode implements ErrorCode {
	ID_REQ,

	NAME_REQ,

	NOT_FOUND;

	@Override
	public String code() {
		return "SPECIMEN_LISTS_FOLDER_" + name();
	}
}
