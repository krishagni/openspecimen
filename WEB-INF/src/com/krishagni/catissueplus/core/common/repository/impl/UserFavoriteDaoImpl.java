package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.common.domain.UserFavorite;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.UserFavoriteDao;

public class UserFavoriteDaoImpl extends AbstractDao<UserFavorite> implements UserFavoriteDao {
	@Override
	public Class<UserFavorite> getType() {
		return UserFavorite.class;
	}

	@Override
	public List<UserFavorite> getFavorites(Long userId) {
		Criteria<UserFavorite> query = getFavoritesListQuery(userId);
		return query.orderBy(query.desc("f.id"))
			.list();
	}

	@Override
	public Long getFavoritesCount(Long userId) {
		return getFavoritesListQuery(userId).getCount("f.id");
	}

	private Criteria<UserFavorite> getFavoritesListQuery(Long userId) {
		Criteria<UserFavorite> query = createCriteria(UserFavorite.class, "f")
			.join("f.user", "user");
		return query.add(query.eq("user.id", userId));
	}
}
