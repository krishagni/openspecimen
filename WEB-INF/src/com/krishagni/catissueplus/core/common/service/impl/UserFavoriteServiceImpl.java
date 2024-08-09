package com.krishagni.catissueplus.core.common.service.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.UserFavorite;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.errors.UserFavoriteError;
import com.krishagni.catissueplus.core.common.events.UserFavoriteDetail;
import com.krishagni.catissueplus.core.common.service.UserFavoriteService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class UserFavoriteServiceImpl implements UserFavoriteService {
	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public List<UserFavoriteDetail> getFavorites() {
		return getFavorites0();
	}

	@Override
	@PlusTransactional
	public List<UserFavoriteDetail> addFavorite(UserFavoriteDetail input) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		Long count = daoFactory.getUserFavoriteDao().getFavoritesCount(AuthUtil.getCurrentUser().getId());
		if (count >= 25L) {
			ose.addError(UserFavoriteError.MAX_LIMIT_REACHED, count);
		}

		if (StringUtils.isBlank(input.getTitle())) {
			ose.addError(UserFavoriteError.TITLE_REQ);
		}

		if (StringUtils.isBlank(input.getViewUrl())) {
			ose.addError(UserFavoriteError.VIEW_URL_REQ);
		}

		ose.checkAndThrow();

		UserFavorite favorite = new UserFavorite();
		favorite.setUser(AuthUtil.getCurrentUser());
		favorite.setTitle(input.getTitle());
		favorite.setViewUrl(input.getViewUrl());
		favorite.setOldView(input.isOldView());
		daoFactory.getUserFavoriteDao().saveOrUpdate(favorite, true);
		return getFavorites0();
	}

	@Override
	@PlusTransactional
	public List<UserFavoriteDetail> deleteFavorite(Long favoriteId) {
		UserFavorite favorite = daoFactory.getUserFavoriteDao().getById(favoriteId);
		if (favorite != null) {
			daoFactory.getUserFavoriteDao().delete(favorite);
		}

		return getFavorites0();
	}

	private List<UserFavoriteDetail> getFavorites0() {
		return UserFavoriteDetail.from(daoFactory.getUserFavoriteDao().getFavorites(AuthUtil.getCurrentUser().getId()));
	}
}
