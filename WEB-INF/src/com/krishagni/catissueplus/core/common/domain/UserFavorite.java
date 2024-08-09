package com.krishagni.catissueplus.core.common.domain;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

public class UserFavorite extends BaseEntity {
	private User user;

	private String title;

	private String viewUrl;

	private boolean oldView;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getViewUrl() {
		return viewUrl;
	}

	public void setViewUrl(String viewUrl) {
		this.viewUrl = viewUrl;
	}

	public boolean isOldView() {
		return oldView;
	}

	public void setOldView(boolean oldView) {
		this.oldView = oldView;
	}
}
