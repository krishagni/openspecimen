package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListsFolder;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsFolderDao;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsFoldersCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.SubQuery;

public class SpecimenListsFolderDaoImpl extends AbstractDao<SpecimenListsFolder> implements SpecimenListsFolderDao {
	@Override
	public Class<SpecimenListsFolder> getType() {
		return SpecimenListsFolder.class;
	}

	@Override
	public List<SpecimenListsFolder> getFolders(SpecimenListsFoldersCriteria criteria) {
		Criteria<SpecimenListsFolder> query = createCriteria(SpecimenListsFolder.class, "f");
		return query.add(query.in("f.id", getListQuery(criteria, query)))
			.addOrder(query.asc("f.name"))
			.list(criteria.startAt(), criteria.maxResults());
	}

	@Override
	public Long getFoldersCount(SpecimenListsFoldersCriteria criteria) {
		Criteria<SpecimenListsFolder> query = createCriteria(SpecimenListsFolder.class, "f");
		return query.add(query.in("f.id", getListQuery(criteria, query))).getCount("f.id");
	}

	@Override
	public Map<Long, Integer> getFolderCartsCount(Collection<Long> folderIds) {
		List<Object[]> rows = createNamedQuery(GET_FOLDER_CARTS_COUNT, Object[].class)
			.setParameterList("folderIds", folderIds)
			.list();

		Map<Long, Integer> result = new HashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Integer) row[1]);
		}

		return result;
	}

	@Override
	public boolean isAccessible(Long folderId, Long userId) {
		Criteria<SpecimenListsFolder> query = createCriteria(SpecimenListsFolder.class, "f");
		return !query.leftJoin("f.owner", "owner")
			.leftJoin("f.userGroups", "userGroup")
			.leftJoin("userGroup.users", "user")
			.add(query.eq("f.id", folderId))
			.add(query.or(query.eq("owner.id", userId), query.eq("user.id", userId)))
			.list().isEmpty();
	}

	private SubQuery<Long> getListQuery(SpecimenListsFoldersCriteria criteria, Criteria<?> mainQuery) {
		SubQuery<Long> query = mainQuery.createSubQuery(SpecimenListsFolder.class, "f")
			.distinct().select("f.id");

		if (StringUtils.isNotBlank(criteria.query())) {
			if (isMySQL()) {
				query.add(query.like("f.name", criteria.query()));
			} else {
				query.add(query.ilike("f.name", criteria.query()));
			}
		}

		if (criteria.userId() != null) {
			query.leftJoin("f.owner", "owner")
				.leftJoin("f.userGroups", "userGroup")
				.leftJoin("userGroup.users", "user")
				.add(
					query.or(
						query.eq("owner.id", criteria.userId()),
						query.eq("user.id", criteria.userId())
					)
				);
		}

		return query;
	}

	private static final String FQN = SpecimenListsFolder.class.getName();

	private static final String GET_FOLDER_CARTS_COUNT = FQN + ".getFolderCartsCount";
}
