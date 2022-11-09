
package com.krishagni.catissueplus.core.common.domain;

public class KeySequence {
	private Long id;
	
	private String type;

	private String typeId;

	private Long sequence = 0L;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTypeId() {
		return typeId;
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}
	
	public Long getSequence() {
		return sequence;
	}

	public void setSequence(Long sequence) {
		this.sequence = (sequence == null) ? 0L : sequence;
	}

	public Long increment() {
		return ++sequence;
	}

	public Long incrementBy(int delta) {
		sequence += delta;
		return sequence;
	}
}