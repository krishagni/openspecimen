
package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.ContainerType;
import com.krishagni.catissueplus.core.administrative.repository.ContainerTypeDao;
import com.krishagni.catissueplus.core.administrative.repository.ContainerTypeListCriteria;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class ContainerTypeDaoImpl extends AbstractDao<ContainerType> implements ContainerTypeDao {
	
	@Override
	public Class<?> getType() {
		return ContainerType.class;
	}

	@Override
	public List<ContainerType> getTypes(ContainerTypeListCriteria crit) {
		Criteria<ContainerType> query = getTypesListQuery(crit);
		return query.orderBy(query.asc("ct.name")).list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Long getTypesCount(ContainerTypeListCriteria crit) {
		return getTypesListQuery(crit).getCount("ct.id");
	}

	@Override
	public List<ContainerType> getByNames(Collection<String> names) {
		return createNamedQuery(GET_BY_NAMES, ContainerType.class)
			.setParameterList("names", names)
			.list();
	}

	@Override
	public ContainerType getByName(String name) {
		List<ContainerType> result = getByNames(Collections.singleton(name));
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	public List<Long> getLeafTypeIds() {
		return createNamedQuery(GET_LEAF_IDS, Long.class).list();
	}

	@Override
	public List<DependentEntityDetail> getDependentEntities(Long typeId) {
		List<Object[]> rows = createNamedQuery(GET_DEPENDENTS, Object[].class)
			.setParameter("typeId", typeId)
			.list();

		List<DependentEntityDetail> result = new ArrayList<>();
		for (Object[] row : rows) {
			DependentEntityDetail de = new DependentEntityDetail();
			de.setName((String) row[0]);
			de.setCount((Integer) row[1]);
			result.add(de);
		}

		return result;
	}

	private Criteria<ContainerType> getTypesListQuery(ContainerTypeListCriteria crit) {
		return addSearchConditions(createCriteria(ContainerType.class, "ct"), crit);
	}

	private Criteria<ContainerType> addSearchConditions(Criteria<ContainerType> query, ContainerTypeListCriteria crit) {
		addNameRestriction(query, crit.query());
		addCanHoldRestriction(query, crit.canHold());
		return query;
	}
	
	private void addNameRestriction(Criteria<ContainerType> query, String name) {
		if (StringUtils.isBlank(name)) {
			return;
		}
		
		query.add(query.ilike("ct.name", name));
	}
	
	private void addCanHoldRestriction(Criteria<ContainerType> query, List<String> canHold) {
		if (CollectionUtils.isEmpty(canHold)) {
			return;
		}
		
		query.createAlias("ct.canHold", "canHold")
			.add(query.in("canHold.name", canHold));
	}
	
	private static final String FQN = ContainerType.class.getName();
	
	private static final String GET_BY_NAMES = FQN + ".getByNames";

	private static final String GET_LEAF_IDS = FQN + ".getLeafTypeIds";

	private static final String GET_DEPENDENTS = FQN + ".getDependentEntities";
}
