package com.krishagni.catissueplus.rest.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.UserFavoriteDetail;
import com.krishagni.catissueplus.core.common.service.UserFavoriteService;

@Controller
@RequestMapping("/user-favorites")
public class UserFavoritesController {

	@Autowired
	private UserFavoriteService favoriteSvc;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserFavoriteDetail> getFavorites() {
		return favoriteSvc.getFavorites();
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserFavoriteDetail> addFavorite(@RequestBody UserFavoriteDetail input) {
		return favoriteSvc.addFavorite(input);
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserFavoriteDetail> addFavorite(@PathVariable("id") Long favoriteId) {
		return favoriteSvc.deleteFavorite(favoriteId);
	}
}
