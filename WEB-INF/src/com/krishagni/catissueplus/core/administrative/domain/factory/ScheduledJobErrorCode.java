package com.krishagni.catissueplus.core.administrative.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum ScheduledJobErrorCode implements ErrorCode {
	NOT_FOUND,
	
	NAME_REQUIRED,
	
	END_DATE_BEFORE_START_DATE,
	
	INVALID_REPEAT_SCHEDULE,
	
	START_DATE_REQUIRED,
	
	DUP_JOB_NAME,
	
	INVALID_TYPE, 
	
	JOB_RUN_NOT_FOUND,
	
	INVALID_RECIPIENT,
	
	INVALID_SCHEDULED_TIME,
	
	REPEAT_SCHEDULE_REQUIRED,
	
	RESULT_DATA_FILE_NOT_FOUND,
	
	RESULT_DATA_FILE_NOT_AVAILABLE,
	
	EXTERNAL_COMMAND_REQUIRED,
	
	TASK_IMPL_FQN_REQUIRED,
	
	INVALID_TASK_IMPL_FQN,

	QUERY_REQ,

	OP_NOT_ALLOWED,

	EXT_TASK_FORBIDDEN;
	
	@Override
	public String code() {
		return "SCHEDULED_JOB_" + this.name();
	}
}
