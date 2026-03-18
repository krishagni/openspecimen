package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Collections;
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
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
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

		Map<Long, Integer> result = rows.stream().collect(
			Collectors.toMap(
				row -> (Long) row[0],
				row -> (Integer) row[1],
				(oldVal, newVal) -> newVal
			)
		);
		groupIds.stream().filter(groupId -> !result.containsKey(groupId)).forEach(groupId -> result.put(groupId, 0));
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
	public List<Long> getGroupCpIds(Long groupId, Collection<Long> cpIds) {
		if (CollectionUtils.isEmpty(cpIds)) {
			return Collections.emptyList();
		}

		Criteria<Long> query = createCriteria(CollectionProtocolGroup.class, Long.class, "g")
			.join("g.cps", "cp");
		return query.add(query.eq("g.id", groupId))
			.add(query.in("cp.id", cpIds))
			.select("cp.id")
			.list();
	}

	@Override
	public List<String> getCpsUsedInOtherGroups(Long groupId, Collection<Long> cpIds) {
		if (CollectionUtils.isEmpty(cpIds)) {
			return Collections.emptyList();
		}

		Criteria<String> query = createCriteria(CollectionProtocolGroup.class, String.class, "g")
			.join("g.cps", "cp")
			.select("cp.shortTitle");

		return query.add(query.ne("g.id", groupId != null ? groupId : -1L))
			.add(query.in("cp.id", cpIds))
			.list();
	}

	@Override
	public int getCpsCount(Long groupId, Set<SiteCpPair> siteCps) {
		Criteria<Long> query = createCriteria(CollectionProtocolGroup.class, Long.class, "group")
			.join("group.cps", "cp");

		query.add(query.eq("group.id", groupId));
		if (CollectionUtils.isNotEmpty(siteCps)) {
			SubQuery<Long> allowedCps = BiospecimenDaoHelper.getInstance().getCpIdsFilter(query, siteCps);
			query.add(query.in("cp.id", allowedCps));
		}

		return query.getCount("cp.id").intValue();
	}

	@Override
	public void addCpsToGroup(Long groupId, Collection<Long> cpIds) {
		if (CollectionUtils.isEmpty(cpIds)) {
			return;
		}

		getCurrentSession().doWork(
			connection -> {
				try (PreparedStatement ps = connection.prepareStatement(INSERT_GROUP_CP)) {
					for (Long cpId : cpIds) {
						ps.setLong(1, groupId);
						ps.setLong(2, cpId);
						ps.addBatch();
					}

					ps.executeBatch();
				}
			}
		);
	}

	@Override
	public int removeCpsFromGroup(Long groupId, Collection<Long> cpIds) {
		if (CollectionUtils.isEmpty(cpIds)) {
			return 0;
		}

		return createNativeQuery(DELETE_GROUP_CPS)
			.setParameter("groupId", groupId)
			.setParameter("cpIds", cpIds)
			.executeUpdate();
	}

	@Override
	public int removeCpsFromGroup(Long groupId) {
		return createNativeQuery(DELETE_ALL_GROUP_CPS).setParameter("groupId", groupId).executeUpdate();
	}

	@Override
	public Set<Long> filterCps(Collection<Long> cpIds, Set<SiteCpPair> siteCps) {
		Criteria<Long> query = createCriteria(CollectionProtocol.class, Long.class, "cp");
		SubQuery<Long> allowedCps = BiospecimenDaoHelper.getInstance().getCpIdsFilter(query, siteCps);
		List<Long> result = query.add(query.in("cp.id", allowedCps))
			.add(query.in("cp.id", cpIds))
			.select("cp.id")
			.list();
		return new HashSet<>(result);
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
	public int deleteForms(Collection<Long> formIds) {
		return createNamedQuery(DELETE_FORMS_BY_ID)
			.setParameterList("formIds", formIds)
			.executeUpdate();
	}

	public int deleteForms(Long groupId) {
		return createNamedQuery(DELETE_ALL_FORMS)
			.setParameter("groupId", groupId)
			.executeUpdate();
	}

	private static final String FQN = CollectionProtocolGroup.class.getName();

	private static final String GET_BY_NAME = FQN + ".getByName";

	private static final String GET_GROUP_CPS_COUNT = FQN + ".getGroupCpsCount";

	private static final String GET_CP_FORMS_BY_ENTITY = FQN + ".getEntityFormsByCp";

	private static final String DELETE_FORMS_BY_ID = FQN + ".deleteFormsById";

	private static final String DELETE_ALL_FORMS = FQN + ".deleteAllForms";

	private static final String INSERT_GROUP_CP = "insert into os_cp_group_cps (group_id, cp_id) values (?, ?)";

	private static final String DELETE_GROUP_CPS = "delete from os_cp_group_cps where group_id = :groupId and cp_id in (:cpIds)";

	private static final String DELETE_ALL_GROUP_CPS = "delete from os_cp_group_cps where group_id = :groupId";
}
