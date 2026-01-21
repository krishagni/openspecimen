package com.krishagni.catissueplus.core.auth.repository.impl;

import java.util.Date;

import com.krishagni.catissueplus.core.auth.domain.OAuthState;
import com.krishagni.catissueplus.core.auth.repository.OAuthStateDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class OAuthStateDaoImpl extends AbstractDao<OAuthState> implements OAuthStateDao {
	@Override
	public OAuthState getByState(String state) {
		Criteria<OAuthState> query = createCriteria(OAuthState.class, "state")
			.join("state.domain", "domain");
		return query.add(query.eq("state.state", state))
			.uniqueResult();
	}

	@Override
	public int deleteStatesOlderThan(Date time) {
		return getCurrentSession().getNamedQuery(DELETE_OLD_STATES)
			.setParameter("staleTime", time)
			.executeUpdate();
	}

	private static final String FQN = OAuthState.class.getName();

	private static final String DELETE_OLD_STATES = FQN + ".deleteOldStates";
}
