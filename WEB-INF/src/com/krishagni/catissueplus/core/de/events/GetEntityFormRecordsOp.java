package com.krishagni.catissueplus.core.de.events;

import java.util.function.Function;

public class GetEntityFormRecordsOp {
	private Long entityId;
	
	private Long formCtxtId;

        // input is entity type
        // output is true if access allowed. 
	private Function<String, Boolean> accessAllowed;

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public Long getFormCtxtId() {
		return formCtxtId;
	}

	public void setFormCtxtId(Long formCtxtId) {
		this.formCtxtId = formCtxtId;
	}

	public Function<String, Boolean> getAccessAllowed() {
		return accessAllowed;
	}

	public void setAccessAllowed(Function<String, Boolean> accessAllowed) {
		this.accessAllowed = accessAllowed;
	}
}
