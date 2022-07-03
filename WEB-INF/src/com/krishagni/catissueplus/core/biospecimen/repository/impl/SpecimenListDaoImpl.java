package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListItem;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListDao;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class SpecimenListDaoImpl extends AbstractDao<SpecimenList> implements SpecimenListDao {

	@Override
	public Class<SpecimenList> getType() {
		return SpecimenList.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SpecimenListSummary> getSpecimenLists(SpecimenListsCriteria crit) {
		List<SpecimenList> lists = getCurrentSession().createCriteria(SpecimenList.class, "l")
			.add(Subqueries.propertyIn("l.id", getSpecimenListsQuery(crit)))
			.addOrder(Order.desc("l.lastUpdatedOn"))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.list();

		List<SpecimenListSummary> results = new ArrayList<>();
		Map<Long, SpecimenListSummary> listMap = new HashMap<>();
		for (SpecimenList list : lists) {
			SpecimenListSummary summary = SpecimenListSummary.fromSpecimenList(list);
			results.add(summary);
			if (crit.includeStat()) {
				listMap.put(list.getId(), summary);
			}
		}

		if (listMap.isEmpty()) {
			return results;
		}

		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_LIST_SPECIMENS_COUNT)
			.setParameterList("listIds", listMap.keySet())
			.list();

		for (Object[] row : rows) {
			SpecimenListSummary summary = listMap.get((Long)row[0]);
			summary.setSpecimenCount(((Number)row[1]).intValue());
		}

		return results;
	}

	@Override
	public Long getSpecimenListsCount(SpecimenListsCriteria crit) {
		Number count = (Number) getCurrentSession().createCriteria(SpecimenList.class, "l")
			.add(Subqueries.propertyIn("l.id", getSpecimenListsQuery(crit)))
			.setProjection(Projections.rowCount())
			.uniqueResult();
		return count.longValue();
	}

	@Override
	public SpecimenList getSpecimenList(Long listId) {
		return getById(listId);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public SpecimenList getSpecimenListByName(String name) {
		List<SpecimenList> result = getCurrentSession()
				.getNamedQuery(GET_SPECIMEN_LIST_BY_NAME)
				.setString("name", name)
				.list();
		
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	public SpecimenList getDefaultSpecimenList(Long userId) {
		return getSpecimenListByName(SpecimenList.getDefaultListName(userId));
	}

	@SuppressWarnings("unchecked")
	@Override
	public int getListSpecimensCount(Long listId) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_LIST_SPECIMENS_COUNT)
			.setParameterList("listIds", Collections.singletonList(listId))
			.list();

		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}

		return ((Number)rows.iterator().next()[1]).intValue();
	}

	@Override
	public void deleteSpecimenList(SpecimenList list) {
		getCurrentSession().delete(list);
	}

	@Override
	public boolean isListSharedWithUser(Long listId, Long userId) {
		List<Long> sharedLists = getListsSharedWithUser(Collections.singletonList(listId), userId);
		return !sharedLists.isEmpty() && sharedLists.get(0).equals(listId);
	}

	public List<Long> getListsSharedWithUser(List<Long> listIds, Long userId) {
		return getCurrentSession().getNamedQuery(SHARED_WITH_USER)
			.setParameterList("listIds", listIds)
			.setParameter("userId", userId)
			.list();
	}

	//
	// specimen list items
	//
	@Override
	public List<SpecimenListItem> getListItems(Long listId, List<Long> specimenIds) {
		return getCurrentSession().createCriteria(SpecimenListItem.class, "li")
			.createAlias("li.list", "list")
			.createAlias("li.specimen", "specimen")
			.add(Restrictions.eq("list.id", listId))
			.add(Restrictions.in("specimen.id", specimenIds))
			.list();
	}

	@Override
	public void saveListItems(List<SpecimenListItem> items) {
		int i = 0;
		for (SpecimenListItem item : items) {
			getCurrentSession().saveOrUpdate(item);
			i++;

			if (i % 50 == 0) {
				getCurrentSession().flush();
				getCurrentSession().clear();
			}
		}
	}

	@Override
	public int deleteListItems(Long listId, List<Long> specimenIds) {
		List<SpecimenListItem> items = getListItems(listId, specimenIds);
		items.forEach(item -> getCurrentSession().delete(item));
		return items.size();
	}

	@Override
	public int clearList(Long listId) {
		Criteria query = getCurrentSession().createCriteria(SpecimenListItem.class, "li")
			.createAlias("li.list", "list")
			.addOrder(Order.asc("li.id"))
			.setMaxResults(50);

		int startAt = 0;
		while (true) {
			List<SpecimenListItem> items = query.setFirstResult(startAt).list();
			items.forEach(item -> getCurrentSession().delete(item));
			if (items.size() < 50) {
				break;
			}

			startAt += items.size();
		}

		return startAt;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Long> getSpecimenIdsInList(Long listId, List<Long> specimenIds) {
		Criteria query = getCurrentSession().createCriteria(SpecimenList.class, "list")
			.createAlias("list.specimens", "item")
			.createAlias("item.specimen", "specimen")
			.setProjection(Projections.property("specimen.id"))
			.add(Restrictions.eq("list.id", listId));
		applyIdsFilter(query, "specimen.id", specimenIds);
		return query.list();
	}

	@Override
	public void addChildSpecimens(Long listId, boolean oracle) {
		getCurrentSession().getNamedQuery(oracle ? ADD_CHILD_SPECIMENS_ORA : ADD_CHILD_SPECIMENS_MYSQL)
			.setParameter("listId", listId)
			.executeUpdate();
	}

	@Override
	@Deprecated
	public Map<Long, List<Specimen>> getListCpSpecimens(Long listId) {
		List<Object[]> rows = getCurrentSession()
			.getNamedQuery(GET_LIST_CP_SPECIMENS)
			.setLong("listId", listId)
			.list();

		Map<Long, List<Specimen>> cpSpecimens = new HashMap<Long, List<Specimen>>();
		for (Object[] row : rows) {
			Long cpId = (Long)row[0];
			Specimen specimen = (Specimen)row[1];

			List<Specimen> specimens = cpSpecimens.get(cpId);
			if (specimens == null) {
				specimens = new ArrayList<>();
				cpSpecimens.put(cpId, specimens);
			}

			specimens.add(specimen);
		}

		return cpSpecimens;
	}

	@Override
	@Deprecated
	public List<Long> getListSpecimensCpIds(Long listId) {
		return getCurrentSession().getNamedQuery(GET_LIST_SPMNS_CP_IDS)
			.setLong("listId", listId)
			.list();
	}

	private DetachedCriteria getSpecimenListsQuery(SpecimenListsCriteria crit) {
		DetachedCriteria query = DetachedCriteria.forClass(SpecimenList.class, "l")
			.setProjection(Projections.distinct(Projections.property("l.id")))
			.add(Restrictions.isNull("l.deletedOn"));

		if (crit.userId() != null || StringUtils.isNotBlank(crit.query())) {
			query.createAlias("l.owner", "owner");
		}

		if (crit.userId() != null) {
			query.createAlias("l.sharedWith", "sharedUser", JoinType.LEFT_OUTER_JOIN)
				.createAlias("l.sharedWithGroups", "sharedGroup", JoinType.LEFT_OUTER_JOIN)
				.createAlias("sharedGroup.users", "sharedGroupUser", JoinType.LEFT_OUTER_JOIN)
				.createAlias("l.folders", "folder", JoinType.LEFT_OUTER_JOIN)
				.createAlias("folder.owner", "folderOwner", JoinType.LEFT_OUTER_JOIN)
				.createAlias("folder.userGroups", "folderUserGroup", JoinType.LEFT_OUTER_JOIN)
				.createAlias("folderUserGroup.users", "folderUser", JoinType.LEFT_OUTER_JOIN)
				.add(
					Restrictions.disjunction(
						Restrictions.eq("owner.id", crit.userId()),
						Restrictions.eq("sharedUser.id", crit.userId()),
						Restrictions.eq("sharedGroupUser.id", crit.userId()),
						Restrictions.eq("folderOwner.id", crit.userId()),
						Restrictions.eq("folderUser.id", crit.userId())
					)
				);
		}

		if (crit.folderId() != null) {
			if (crit.userId() == null) {
				query.createAlias("l.folders", "folder");
			}

			query.add(Restrictions.eq("folder.id", crit.folderId()));
		}

		if (StringUtils.isNotBlank(crit.query())) {
			if (isMySQL()) {
				query.add(
					Restrictions.disjunction(
						Restrictions.like("l.name", crit.query(), MatchMode.ANYWHERE),
						Restrictions.like("owner.firstName", crit.query(), MatchMode.ANYWHERE),
						Restrictions.like("owner.lastName", crit.query(), MatchMode.ANYWHERE)
					)
				);
			} else {
				query.add(
					Restrictions.disjunction(
						Restrictions.like("l.name", crit.query(), MatchMode.ANYWHERE).ignoreCase(),
						Restrictions.like("owner.firstName", crit.query(), MatchMode.ANYWHERE).ignoreCase(),
						Restrictions.like("owner.lastName", crit.query(), MatchMode.ANYWHERE).ignoreCase()
					)
				);
			}
		}

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			query.add(Restrictions.in("l.id", crit.ids()));
		}

		if (CollectionUtils.isNotEmpty(crit.notInIds())) {
			query.add(Restrictions.not(Restrictions.in("l.id", crit.notInIds())));
		}

		return query;
	}

	private static final String FQN = SpecimenList.class.getName();

	private static final String GET_LIST_CP_SPECIMENS = FQN + ".getListCpSpecimens";

	private static final String GET_LIST_SPMNS_CP_IDS = FQN + ".getListSpecimensCpIds";

	private static final String GET_SPECIMEN_LIST_BY_NAME = FQN + ".getSpecimenListByName";

	private static final String GET_LIST_SPECIMENS_COUNT = FQN + ".getListSpecimensCount";

	private static final String SHARED_WITH_USER = FQN + ".sharedWithUser";

	private static final String ADD_CHILD_SPECIMENS_MYSQL = FQN + ".addChildSpecimensMySQL";

	private static final String ADD_CHILD_SPECIMENS_ORA = FQN + ".addChildSpecimensOracle";
}
