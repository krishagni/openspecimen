package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import com.krishagni.catissueplus.core.biospecimen.domain.RequestManagerGroup;
import com.krishagni.catissueplus.core.biospecimen.repository.RequestManagerGroupDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class RequestManagerGroupDaoImpl extends AbstractDao<RequestManagerGroup> implements RequestManagerGroupDao {
	@Override
	public Class<RequestManagerGroup> getType() {
		return RequestManagerGroup.class;
	}

	@Override
	public RequestManagerGroup getByName(String name) {
		return createNamedQuery(GET_BY_NAME, RequestManagerGroup.class)
			.setParameter("name", name)
			.uniqueResult();
	}

	private static final String FQN = RequestManagerGroup.class.getName();

	private static final String GET_BY_NAME = FQN + ".getByName";
}
