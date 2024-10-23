package com.krishagni.catissueplus.core.de.events;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ListSavedQueriesCriteria extends AbstractListCriteria<ListSavedQueriesCriteria> {
	private Long cpId;

	private boolean countReq;

	private Long userId;

	private Long instituteId;

	private Long folderId;

	public Long cpId() {
		return cpId;
	}

	public ListSavedQueriesCriteria cpId(Long cpId) {
		this.cpId = cpId;
		return self();
	}

	public boolean countReq() {
		return countReq;
	}

	public ListSavedQueriesCriteria countReq(boolean countReq) {
		this.countReq = countReq;
		return self();
	}

	public Long userId() {
		return userId;
	}

	public ListSavedQueriesCriteria userId(Long userId) {
		this.userId = userId;
		return self();
	}

	public Long instituteId() {
		return instituteId;
	}

	public ListSavedQueriesCriteria instituteId(Long instituteId) {
		this.instituteId = instituteId;
		return self();
	}

	public Long folderId() {
		return folderId;
	}

	public ListSavedQueriesCriteria folderId(Long folderId) {
		this.folderId = folderId;
		return self();
	}

	@Override
	public ListSavedQueriesCriteria self() {
		return this;
	}		
}
