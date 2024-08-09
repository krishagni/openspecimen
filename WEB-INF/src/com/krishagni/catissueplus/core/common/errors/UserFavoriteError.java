package com.krishagni.catissueplus.core.common.errors;

public enum UserFavoriteError implements ErrorCode {

	MAX_LIMIT_REACHED,

	TITLE_REQ,

	VIEW_URL_REQ;

	@Override
	public String code() {
		return "USER_FAVORITE_" + name();
	}
}
