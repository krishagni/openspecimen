package com.krishagni.catissueplus.core.common.events;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.common.domain.UserFavorite;
import com.krishagni.catissueplus.core.common.util.Utility;

public class UserFavoriteDetail {
	private Long id;

	private String title;

	private String viewUrl;

	private boolean oldView;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public static UserFavoriteDetail from(UserFavorite favorite) {
		UserFavoriteDetail result = new UserFavoriteDetail();
		result.setId(favorite.getId());
		result.setTitle(favorite.getTitle());
		result.setViewUrl(favorite.getViewUrl());
		result.setOldView(favorite.isOldView());
		return result;
	}

	public static List<UserFavoriteDetail> from(Collection<UserFavorite> favorites) {
		return Utility.nullSafeStream(favorites).map(UserFavoriteDetail::from).collect(Collectors.toList());
	}
}
