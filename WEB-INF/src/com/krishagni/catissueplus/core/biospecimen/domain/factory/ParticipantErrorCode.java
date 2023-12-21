
package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum ParticipantErrorCode implements ErrorCode {
	NOT_FOUND,
	
	INVALID_DEATH_DATE,
	
	INVALID_BIRTH_DATE,
	
	INVALID_RACE,
	
	INVALID_ETHNICITY,
	
	INVALID_UID,
	
	INVALID_MPI,
	
	UID_REQUIRED,
	
	DUP_UID,
	
	DUP_EMPI,

	INVALID_EMAIL_ID,
	
	INVALID_VITAL_STATUS,
	
	INVALID_GENDER,
	
	DUP_MRN,
	
	DUP_MRN_SITE,

	EMPI_MRN_DIFF,

	MRN_DIFF,
	
	EMPI_REQUIRED,
	
	REF_ENTITY_FOUND, 
	
	MANUAL_MPI_NOT_ALLOWED,
	
	CANNOT_UPDATE_PHI,

	LF_UPDATE_NOT_ALLOWED,

	INVALID_LOOKUP_FLOW,

	STAGED_ID_REQ,

	STAGED_NOT_FOUND,

	MULTI_MATCHES,

	NO_MRN_MATCH,

	TEXT_OPT_OUT;

	public String code() {
		return "PARTICIPANT_" + this.name();
	}
}
