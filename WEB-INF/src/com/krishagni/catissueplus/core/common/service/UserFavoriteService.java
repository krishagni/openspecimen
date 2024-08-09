package com.krishagni.catissueplus.core.common.service;

import java.util.List;

import com.krishagni.catissueplus.core.common.events.UserFavoriteDetail;

public interface UserFavoriteService {
	List<UserFavoriteDetail> getFavorites();

	List<UserFavoriteDetail> addFavorite(UserFavoriteDetail input);

	List<UserFavoriteDetail> deleteFavorite(Long favoriteId);
}
