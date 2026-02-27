package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.PickedSpecimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListItem;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimensPickList;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListDigestItem;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.PickListsCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListDao;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsCriteria;
import com.krishagni.catissueplus.core.common.Tuple;
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
		int deleted = createNamedQuery(CLEAR_LIST_ITEMS)
			.setParameter("listId", listId)
			.setParameterList("specimenIds", specimenIds)
			.executeUpdate();
		deletePickListItems(listId, specimenIds);
		return deleted;
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
	public void saveOrUpdate(SpecimensPickList session) {
		getCurrentSession().saveOrUpdate(session);
	}

	@Override
	public List<SpecimensPickList> getPickLists(PickListsCriteria crit) {
		Criteria<SpecimensPickList> query = createCriteria(SpecimensPickList.class, "list")
			.join("list.cart", "cart")
			.join("list.creator", "creator");

		if (crit.cartId() != null) {
			query.add(query.eq("cart.id", crit.cartId()));
		}

		if (StringUtils.isNotBlank(crit.cartName())) {
			query.add(query.eq("cart.name", crit.cartName()));
		}

		if (StringUtils.isNotBlank(crit.query())) {
			if (isMySQL()) {
				query.add(query.like("list.name", crit.query()));
			} else {
				query.add(query.ilike("list.name", crit.query().toLowerCase()));
			}
		}

		return query.list(crit.startAt(), crit.maxResults());
	}

	@Override
	public SpecimensPickList getPickList(Long cartId, Long pickListId) {
		Criteria<SpecimensPickList> query = createCriteria(SpecimensPickList.class, "pickList")
			.join("pickList.cart", "cart")
			.join("pickList.creator", "creator");
		return query.add(query.eq("pickList.id", pickListId))
			.add(query.eq("cart.id", cartId))
			.uniqueResult();
	}

	@Override
	public void deletePickList(Long cartId, Long pickListId) {
		getCurrentSession().getNamedQuery(DELETE_ALL_PICKED_ITEMS)
			.setParameter("pickListId", pickListId)
			.executeUpdate();

		getCurrentSession().getNamedQuery(DELETE_PICK_LIST)
			.setParameter("cartId", cartId)
			.setParameter("listId", pickListId)
			.executeUpdate();
	}

	@Override
	public List<Long> getSpecimenIdsInPickList(Long pickListId, List<Long> specimenIds) {
		Criteria<Long> query = createCriteria(PickedSpecimen.class, Long.class, "pickedSpmn");
		return query.join("pickedSpmn.specimen", "specimen")
			.join("pickedSpmn.pickList", "pickList")
			.add(query.eq("pickList.id", pickListId))
			.add(query.in("specimen.id", specimenIds))
			.select("specimen.id")
			.list();
	}

	@Override
	public Map<String, Long> getPickListSpecimensCount(Long pickListId) {
		return getPickListSpecimensCount(Collections.singletonList(pickListId)).getOrDefault(pickListId, Collections.emptyMap());
	}

	@Override
	public Map<Long, Map<String, Long>> getPickListSpecimensCount(Collection<Long> pickListIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_PICK_LIST_ITEMS_COUNT)
			.setParameterList("pickListIds", pickListIds)
			.list();
		if (rows.isEmpty()) {
			return Collections.emptyMap();
		}

		// {pickListId -> {total: <count>, picked: <count>}
		Map<Long, Map<String, Long>> result = new LinkedHashMap<>();
		for (Object[] row : rows) {
			Map<String, Long> counts = new LinkedHashMap<>();
			counts.put("total", (Long) row[1]);
			counts.put("picked", (Long) row[2]);

			result.put((Long) row[0], counts);
		}

		return result;
	}

	@Override
	public void savePickListItem(PickedSpecimen pickedSpecimen) {
		getCurrentSession().saveOrUpdate(pickedSpecimen);
	}

	@Override
	public int savePickListItems(SpecimensPickList pickList, User pickedBy, Date pickTime, List<Long> specimenIds) {
		int count = 0;
		for (Long specimenId : specimenIds) {
			Specimen specimen = new Specimen();
			specimen.setId(specimenId);

			PickedSpecimen pickedSpecimen = new PickedSpecimen();
			pickedSpecimen.setPickList(pickList);
			pickedSpecimen.setSpecimen(specimen);
			pickedSpecimen.setUpdater(pickedBy);
			pickedSpecimen.setUpdateTime(pickTime);
			getCurrentSession().saveOrUpdate(pickedSpecimen);

			++count;
			if (count % 25 == 0) {
				getCurrentSession().flush();
				getCurrentSession().clear();
			}
		}

		return count;
	}

	@Override
	public int deleteSpecimensFromPickList(Long pickListId, List<Long> specimenIds) {
		return getCurrentSession().getNamedQuery(DELETE_PICKED_ITEMS)
			.setParameter("pickListId", pickListId)
			.setParameterList("specimenIds", specimenIds)
			.executeUpdate();
	}

	@Override
	public List<Tuple> getInactivePickLists(Date activeBefore) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_INACTIVE_PICK_LISTS)
			.setParameter("cutOffDate", activeBefore)
			.list();

		if (rows.isEmpty()) {
			return Collections.emptyList();
		}

		List<Tuple> pickLists = new ArrayList<>();
		for (Object[] row : rows) {
			// {cartId, pickListId, lastAccessTime}
			pickLists.add(new Tuple((Long) row[0], (Long) row[1], (Date) row[2]));
		}

		return pickLists;
	}

	@Override
	public void deletePickListItems(Long cartId, Collection<Long> specimenIds) {
		getCurrentSession().getNamedQuery(DEL_PICK_LIST_ITEMS_BY_CART_N_SPMNS)
			.setParameter("cartId", cartId)
			.setParameterList("specimenIds", specimenIds)
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
			.select("l.id");

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

	private static final String PICK_LIST_FQN = SpecimensPickList.class.getName();

	private static final String DELETE_PICK_LIST = PICK_LIST_FQN + ".deletePickList";

	private static final String GET_INACTIVE_PICK_LISTS = PICK_LIST_FQN + ".getInactivePickLists";

	private static final String PICKED_ITEMS_FQN = PickedSpecimen.class.getName();

	private static final String DELETE_PICKED_ITEMS = PICKED_ITEMS_FQN + ".deletePickedItems";

	private static final String DELETE_ALL_PICKED_ITEMS = PICKED_ITEMS_FQN + ".deleteAllPickedItems";

	private static final String GET_PICK_LIST_ITEMS_COUNT = PICKED_ITEMS_FQN + ".getPickListItemsCount";

	private static final String DEL_PICK_LIST_ITEMS_BY_CART_N_SPMNS = PICKED_ITEMS_FQN + ".delPickListItemsByCartAndSpecimens";
}