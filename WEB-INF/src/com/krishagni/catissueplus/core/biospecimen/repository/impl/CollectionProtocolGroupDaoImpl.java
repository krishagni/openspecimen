package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.biospecimen.repository.CollectionProtocolGroupDao;
import com.krishagni.catissueplus.core.biospecimen.repository.CpGroupListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class CollectionProtocolGroupDaoImpl extends AbstractDao<CollectionProtocolGroup> implements CollectionProtocolGroupDao  {

	@Override
	public Class<CollectionProtocolGroup> getType() {
		return CollectionProtocolGroup.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<CollectionProtocolGroup> getGroups(CpGroupListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(CollectionProtocolGroup.class, "group")
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.addOrder(Order.asc("group.name"));

		if (StringUtils.isNotBlank(crit.query())) {
			query.add(Restrictions.ilike("group.name", crit.query(), MatchMode.ANYWHERE));
		}

		if (CollectionUtils.isNotEmpty(crit.siteCps()) || StringUtils.isNotBlank(crit.cpShortTitle())) {
			DetachedCriteria allowedGroups = DetachedCriteria.forClass(CollectionProtocolGroup.class, "ag")
				.setProjection(Projections.distinct(Projections.property("ag.id")))
				.createAlias("ag.cps", "cp");

			if (CollectionUtils.isNotEmpty(crit.siteCps())) {
				DetachedCriteria allowedCps = BiospecimenDaoHelper.getInstance().getCpIdsFilter(crit.siteCps());
				allowedGroups.add(Subqueries.propertyIn("cp.id", allowedCps));
			}

			if (StringUtils.isNotBlank(crit.cpShortTitle())) {
				allowedGroups.add(Restrictions.eq("cp.shortTitle", crit.cpShortTitle()));
			}

			query.add(Subqueries.propertyIn("group.id", allowedGroups));
		}

		return query.list();
	}

	@Override
	public CollectionProtocolGroup getByName(String name) {
		return (CollectionProtocolGroup) getCurrentSession().getNamedQuery(GET_BY_NAME)
			.setParameter("name", name)
			.uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, Integer> getCpsCount(Collection<Long> groupIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_GROUP_CPS_COUNT)
			.setParameterList("groupIds", groupIds)
			.list();

		Map<Long, Integer> result = new HashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Integer) row[1]);
		}

		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<String> getCpsUsedInOtherGroups(CollectionProtocolGroup group) {
		List<Long> cpIds = group.getCps().stream().map(CollectionProtocol::getId).collect(Collectors.toList());
		Criteria query = getCurrentSession().createCriteria(CollectionProtocolGroup.class, "g")
			.createAlias("g.cps", "cp")
			.add(Restrictions.in("cp.id", cpIds))
			.setProjection(Projections.property("cp.shortTitle"));
		if (group.getId() != null) {
			query.add(Restrictions.ne("g.id", group.getId()));
		}

		return (List<String>) query.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, Set<Long>> getCpForms(List<Long> cpIds, String entityType) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_CP_FORMS_BY_ENTITY)
			.setParameterList("cpIds", cpIds)
			.setParameter("entityType", entityType)
			.list();

		Map<Long, Set<Long>> cpForms = new HashMap<>();
		for (Object[] row : rows) {
			int idx = -1;
			Long cpId = (Long) row[++idx];
			Long formId = (Long) row[++idx];

			Set<Long> formIds = cpForms.computeIfAbsent(cpId, (k) -> new HashSet<>());
			formIds.add(formId);
		}

		return cpForms;
	}

	@Override
	public void deleteForms(Collection<Long> formIds) {
		getCurrentSession().getNamedQuery(DELETE_GROUP_FORMS)
			.setParameterList("formIds", formIds)
			.executeUpdate();
	}

	private static final String FQN = CollectionProtocolGroup.class.getName();

	private static final String GET_BY_NAME = FQN + ".getByName";

	private static final String GET_GROUP_CPS_COUNT = FQN + ".getGroupCpsCount";

	private static final String GET_CP_FORMS_BY_ENTITY = FQN + ".getEntityFormsByCp";

	private static final String DELETE_GROUP_FORMS = FQN + ".deleteGroupForms";
}
