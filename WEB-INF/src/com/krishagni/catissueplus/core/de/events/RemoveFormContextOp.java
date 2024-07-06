package com.krishagni.catissueplus.core.de.events;


public class RemoveFormContextOp {
	public enum RemoveType { SOFT_REMOVE, HARD_REMOVE };

	private Long formContextId;
	
	private Long formId;
	
	private String entityType;
	
	private Long cpId;

	private Long entityId;
	
	private RemoveType removeType;

	public Long getFormContextId() {
		return formContextId;
	}

	public void setFormContextId(Long formContextId) {
		this.formContextId = formContextId;
	}

	public Long getFormId() {
		return formId;
	}

	public void setFormId(Long formId) {
		this.formId = formId;
	}
	
	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public RemoveType getRemoveType() {
		return removeType;
	}

	public void setRemoveType(RemoveType removeType) {
		this.removeType = removeType;
	}
}
