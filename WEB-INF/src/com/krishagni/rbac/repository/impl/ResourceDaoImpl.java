package com.krishagni.rbac.repository.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.rbac.domain.Resource;
import com.krishagni.rbac.repository.ResourceDao;
import com.krishagni.rbac.repository.ResourceListCriteria;

public class ResourceDaoImpl extends AbstractDao<Resource> implements ResourceDao {
	private static final String FQN = Resource.class.getName();
	
	private static final String GET_RESOURCE_BY_NAME = FQN + ".getResourceByName";

	@Override
	public Resource getResourceByName(String resourceName) {
		List<Resource> resources = createNamedQuery(GET_RESOURCE_BY_NAME, Resource.class)
			.setParameter("name" , resourceName)
			.list();
		return resources.isEmpty() ? null : resources.get(0);
	}

	@Override
	public Resource getResource(Long resourceId) {
		return getCurrentSession().get(Resource.class, resourceId);
	}

	@Override
	public List<Resource> getResources(ResourceListCriteria listCriteria) {
		Criteria<Resource> query = createCriteria(Resource.class, "r");
		if (StringUtils.isNotBlank(listCriteria.query())) {
			query.add(query.ilike("r.name", listCriteria.query()));
		}
		
		return query.list(listCriteria.startAt(), listCriteria.maxResults());
	}
}