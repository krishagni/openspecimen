package com.krishagni.catissueplus.core.common.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.domain.UserFavorite;

public interface UserFavoriteDao extends Dao<UserFavorite> {
	List<UserFavorite> getFavorites(Long userId);

	Long getFavoritesCount(Long userId);
}
