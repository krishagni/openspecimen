package com.krishagni.catissueplus.core.administrative.repository;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class UserGroupListCriteria extends AbstractListCriteria<UserGroupListCriteria> {

	private boolean listAll;

	private String institute;

	private String site;

	private Long userId;

	private String userEmail;

	@Override
	public UserGroupListCriteria self() {
		return this;
	}

	public boolean listAll() {
		return listAll;
	}

	public UserGroupListCriteria listAll(boolean listAll) {
		this.listAll = listAll;
		return self();
	}

	public String institute() {
		return institute;
	}

	public UserGroupListCriteria institute(String institute) {
		this.institute = institute;
		return self();
	}

	public String site() {
		return site;
	}

	public UserGroupListCriteria site(String site) {
		this.site = site;
		return self();
	}

	public Long userId() {
		return userId;
	}

	public UserGroupListCriteria userId(Long userId) {
		this.userId = userId;
		return self();
	}

	public String userEmail() {
		return userEmail;
	}

	public UserGroupListCriteria userEmail(String userEmail) {
		this.userEmail = userEmail;
		return self();
	}
}
