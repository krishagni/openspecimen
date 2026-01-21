package com.krishagni.catissueplus.core.auth.repository;

import java.util.Date;

import com.krishagni.catissueplus.core.auth.domain.OAuthState;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface OAuthStateDao extends Dao<OAuthState> {
	OAuthState getByState(String state);

	int deleteStatesOlderThan(Date time);
}
