package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.biospecimen.repository.CollectionProtocolGroupDao;
import com.krishagni.catissueplus.core.biospecimen.repository.CpGroupListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.SubQuery;

public class CollectionProtocolGroupDaoImpl extends AbstractDao<CollectionProtocolGroup> implements CollectionProtocolGroupDao  {

	@Override
	public Class<CollectionProtocolGroup> getType() {
		return CollectionProtocolGroup.class;
	}

	@Override
	public List<CollectionProtocolGroup> getGroups(CpGroupListCriteria crit) {
		Criteria<CollectionProtocolGroup> query = createCriteria(CollectionProtocolGroup.class, "group");
		if (StringUtils.isNotBlank(crit.query())) {
			query.add(query.ilike("group.name", crit.query()));
		}

		if (CollectionUtils.isNotEmpty(crit.siteCps()) || StringUtils.isNotBlank(crit.cpShortTitle())) {
			SubQuery<Long> allowedGroups = query.createSubQuery(CollectionProtocolGroup.class, "ag")
				.join("ag.cps", "cp")
				.distinct().select("ag.id");

			if (CollectionUtils.isNotEmpty(crit.siteCps())) {
				SubQuery<Long> allowedCps = BiospecimenDaoHelper.getInstance().getCpIdsFilter(allowedGroups, crit.siteCps());
				allowedGroups.add(allowedGroups.in("cp.id", allowedCps));
			}

			if (StringUtils.isNotBlank(crit.cpShortTitle())) {
				allowedGroups.add(allowedGroups.eq("cp.shortTitle", crit.cpShortTitle()));
			}

			query.add(query.in("group.id", allowedGroups));
		}

		applyIdsFilter(query, "group.id", crit.ids());
		return query.addOrder(query.asc("group.name")).list(crit.startAt(), crit.maxResults());
	}

	@Override
	public CollectionProtocolGroup getByName(String name) {
		return createNamedQuery(GET_BY_NAME, CollectionProtocolGroup.class)
			.setParameter("name", name)
			.uniqueResult();
	}

	@Override
	public Map<Long, Integer> getCpsCount(Collection<Long> groupIds) {
		List<Object[]> rows = createNamedQuery(GET_GROUP_CPS_COUNT, Object[].class)
			.setParameterList("groupIds", groupIds)
			.list();

		Map<Long, Integer> result = new HashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Integer) row[1]);
		}

		return result;
	}

	@Override
	public List<String> getCpsUsedInOtherGroups(CollectionProtocolGroup group) {
		Criteria<String> query = createCriteria(CollectionProtocolGroup.class, String.class, "g")
			.join("g.cps", "cp")
			.select("cp.shortTitle");

		if (group.getId() != null) {
			query.add(query.ne("g.id", group.getId()));
		}

		List<Long> cpIds = group.getCps().stream().map(CollectionProtocol::getId).collect(Collectors.toList());
		return query.add(query.in("cp.id", cpIds)).list();
	}

	@Override
	public Map<Long, Set<Long>> getCpForms(List<Long> cpIds, String entityType) {
		List<Object[]> rows = createNamedQuery(GET_CP_FORMS_BY_ENTITY, Object[].class)
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
		createNamedQuery(DELETE_GROUP_FORMS, Integer.class)
			.setParameterList("formIds", formIds)
			.executeUpdate();
	}

	private static final String FQN = CollectionProtocolGroup.class.getName();

	private static final String GET_BY_NAME = FQN + ".getByName";

	private static final String GET_GROUP_CPS_COUNT = FQN + ".getGroupCpsCount";

	private static final String GET_CP_FORMS_BY_ENTITY = FQN + ".getEntityFormsByCp";

	private static final String DELETE_GROUP_FORMS = FQN + ".deleteGroupForms";
}
