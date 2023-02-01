package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListItem;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListDigestItem;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListDao;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsCriteria;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.SubQuery;

public class SpecimenListDaoImpl extends AbstractDao<SpecimenList> implements SpecimenListDao {

	@Override
	public Class<SpecimenList> getType() {
		return SpecimenList.class;
	}

	@Override
	public List<SpecimenListSummary> getSpecimenLists(SpecimenListsCriteria crit) {
		Criteria<SpecimenList> query = createCriteria(SpecimenList.class, "l");
		List<SpecimenList> lists = query.add(query.in("l.id", getSpecimenListsQuery(crit, query)))
			.orderBy(query.desc("l.lastUpdatedOn"))
			.list(crit.startAt(), crit.maxResults());

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

		List<Object[]> rows = createNamedQuery(GET_LIST_SPECIMENS_COUNT, Object[].class)
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
		Criteria<SpecimenList> query = createCriteria(SpecimenList.class, "l");
		return query.add(query.in("l.id", getSpecimenListsQuery(crit, query))).getCount("l.id");
	}

	@Override
	public SpecimenList getSpecimenList(Long listId) {
		return getById(listId);
	}
	
	@Override
	public SpecimenList getSpecimenListByName(String name) {
		List<SpecimenList> result = createNamedQuery(GET_SPECIMEN_LIST_BY_NAME, SpecimenList.class)
			.setParameter("name", name)
			.list();
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	public SpecimenList getDefaultSpecimenList(Long userId) {
		return getSpecimenListByName(SpecimenList.getDefaultListName(userId));
	}

	@Override
	public int getListSpecimensCount(Long listId) {
		List<Object[]> rows = createNamedQuery(GET_LIST_SPECIMENS_COUNT, Object[].class)
			.setParameterList("listIds", Collections.singletonList(listId))
			.list();

		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}

		return ((Number)rows.iterator().next()[1]).intValue();
	}

	@Override
	public void deleteSpecimenList(SpecimenList list) {
		super.delete(list);
	}

	@Override
	public boolean isListSharedWithUser(Long listId, Long userId) {
		List<Long> sharedLists = getListsSharedWithUser(Collections.singletonList(listId), userId);
		return !sharedLists.isEmpty() && sharedLists.get(0).equals(listId);
	}

	public List<Long> getListsSharedWithUser(List<Long> listIds, Long userId) {
		return createNamedQuery(SHARED_WITH_USER, Long.class)
			.setParameterList("listIds", listIds)
			.setParameter("userId", userId)
			.list();
	}

	//
	// specimen list items
	//
	@Override
	public List<SpecimenListItem> getListItems(Long listId, List<Long> specimenIds) {
		Criteria<SpecimenListItem> query = createCriteria(SpecimenListItem.class, "li");
		return query.join("li.list", "list")
			.join("li.specimen", "specimen")
			.add(query.eq("list.id", listId))
			.add(query.in("specimen.id", specimenIds))
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
		return createNamedQuery(CLEAR_LIST_ITEMS)
			.setParameter("listId", listId)
			.setParameterList("specimenIds", specimenIds)
			.executeUpdate();
	}

	@Override
	public int clearList(Long listId) {
		return createNamedQuery(CLEAR_LIST)
			.setParameter("listId", listId)
			.executeUpdate();
	}

	@Override
	public List<Long> getSpecimenIdsInList(Long listId, List<Long> specimenIds) {
		Criteria<Long> query = createCriteria(SpecimenList.class, Long.class, "list");
		query.join("list.specimens", "item")
			.join("item.specimen", "specimen")
			.add(query.eq("list.id", listId))
			.select("specimen.id");

		applyIdsFilter(query, "specimen.id", specimenIds);
		return query.list();
	}

	@Override
	public void addChildSpecimens(Long listId, boolean oracle) {
		createNamedQuery(oracle ? ADD_CHILD_SPECIMENS_ORA : ADD_CHILD_SPECIMENS_MYSQL)
			.setParameter("listId", listId)
			.executeUpdate();
	}

	@Override
	@Deprecated
	public Map<Long, List<Specimen>> getListCpSpecimens(Long listId) {
		List<Object[]> rows = createNamedQuery(GET_LIST_CP_SPECIMENS, Object[].class)
			.setParameter("listId", listId)
			.list();

		Map<Long, List<Specimen>> cpSpecimens = new HashMap<>();
		for (Object[] row : rows) {
			Long cpId = (Long)row[0];
			Specimen specimen = (Specimen)row[1];

			List<Specimen> specimens = cpSpecimens.computeIfAbsent(cpId, k -> new ArrayList<>());
			specimens.add(specimen);
		}

		return cpSpecimens;
	}

	@Override
	@Deprecated
	public List<Long> getListSpecimensCpIds(Long listId) {
		return createNamedQuery(GET_LIST_SPMNS_CP_IDS, Long.class)
			.setParameter("listId", listId)
			.list();
	}

	@Override
	public List<Long> getDigestEnabledLists() {
		return createNamedQuery(GET_DIGEST_ENABLED_LIST_IDS, Long.class).list();
	}

	@Override
	public List<SpecimenListDigestItem> getListDigest(Long listId, Date startTime, Date endTime) {
		List<Object[]> rows = createNamedQuery(GET_LIST_DIGEST, Object[].class)
			.setParameter("listId", listId)
			.setParameter("startTime", startTime)
			.setParameter("endTime", endTime)
			.list();

		List<SpecimenListDigestItem> result = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = -1;
			UserSummary user = new UserSummary();
			user.setFirstName((String) row[++idx]);
			user.setLastName((String) row[++idx]);
			user.setEmailAddress((String) row[++idx]);

			SpecimenListDigestItem item = new SpecimenListDigestItem();
			item.setUser(user);
			item.setCpShortTitle((String) row[++idx]);
			item.setAddedSpecimens((Long) row[++idx]);
			result.add(item);
		}

		return result;
	}

	private SubQuery<Long> getSpecimenListsQuery(SpecimenListsCriteria crit, Criteria<?> mainQuery) {
		SubQuery<Long> query = mainQuery.createSubQuery(SpecimenList.class, "l")
			.distinct().select("l.id");

		query.add(query.isNull("l.deletedOn"));
		if (crit.userId() != null || StringUtils.isNotBlank(crit.query())) {
			query.join("l.owner", "owner");
		}

		if (crit.userId() != null) {
			query.leftJoin("l.sharedWith", "sharedUser")
				.leftJoin("l.sharedWithGroups", "sharedGroup")
				.leftJoin("sharedGroup.users", "sharedGroupUser")
				.leftJoin("l.folders", "folder")
				.leftJoin("folder.owner", "folderOwner")
				.leftJoin("folder.userGroups", "folderUserGroup")
				.leftJoin("folderUserGroup.users", "folderUser")
				.add(
					query.or(
						query.eq("owner.id", crit.userId()),
						query.eq("sharedUser.id", crit.userId()),
						query.eq("sharedGroupUser.id", crit.userId()),
						query.eq("folderOwner.id", crit.userId()),
						query.eq("folderUser.id", crit.userId())
					)
				);
		}

		if (crit.folderId() != null) {
			if (crit.userId() == null) {
				query.join("l.folders", "folder");
			}

			query.add(query.eq("folder.id", crit.folderId()));
		}

		if (StringUtils.isNotBlank(crit.query())) {
			if (isMySQL()) {
				query.add(
					query.or(
						query.like("l.name", crit.query()),
						query.like("owner.firstName", crit.query()),
						query.like("owner.lastName", crit.query())
					)
				);
			} else {
				query.add(
					query.or(
						query.ilike("l.name", crit.query()),
						query.ilike("owner.firstName", crit.query()),
						query.ilike("owner.lastName", crit.query())
					)
				);
			}
		}

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			query.add(query.in("l.id", crit.ids()));
		}

		if (CollectionUtils.isNotEmpty(crit.notInIds())) {
			query.add(query.notIn("l.id", crit.notInIds()));
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

	private static final String CLEAR_LIST = FQN + ".clearList";

	private static final String CLEAR_LIST_ITEMS = FQN + ".clearListItems";

	private static final String GET_DIGEST_ENABLED_LIST_IDS = FQN + ".getDigestEnabledListIds";

	private static final String GET_LIST_DIGEST = FQN + ".getListDigest";
}
