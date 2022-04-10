package com.krishagni.catissueplus.core.importer.domain;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum ImportJobErrorCode implements ErrorCode {
	NOT_FOUND,

	FAIL,
	
	ACCESS_DENIED,
			
	OUTPUT_FILE_NOT_CREATED,
	
	OBJ_SCHEMA_NOT_FOUND,

	RECORD_PARSE_ERROR,

	NOT_IN_PROGRESS,

	INVALID_DATE_FORMAT,

	INVALID_TIME_FORMAT,
	
	CSV_NOT_FOUND_IN_ZIP,

	INV_FILENAME;

	@Override
	public String code() {		
		return "IMPORT_JOB_" + this.name();
	}
}
