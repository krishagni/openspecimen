package com.krishagni.catissueplus.core.de.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Restriction;
import com.krishagni.catissueplus.core.de.domain.QueryFolder;
import com.krishagni.catissueplus.core.de.repository.QueryFolderDao;

public class QueryFolderDaoImpl extends AbstractDao<QueryFolder> implements QueryFolderDao {
	@Override
	public Class<QueryFolder> getType() {
		return QueryFolder.class;
	}

	@Override
	public List<QueryFolder> getUserFolders(Long userId, Long instituteId) {
		Criteria<QueryFolder> query = createCriteria(QueryFolder.class, "qf")
			.join("qf.owner", "owner")
			.leftJoin("owner.institute", "institute");

		if (userId != null || instituteId != null) {
			query.leftJoin("qf.sharedWith", "sharedWith")
				.leftJoin("qf.sharedWithGroups", "sharedGroups")
				.leftJoin("sharedGroups.users", "groupUser");

			Restriction userCrit = null;
			if (userId != null) {
				userCrit = query.eq("owner.id", userId);
			} else {
				userCrit = query.or(
					query.isNull("institute.id"),
					query.eq("institute.id", instituteId)
				);
			}

			query.add(
				query.disjunction()
					.add(query.eq("qf.sharedWithAll", true))
					.add(userCrit)
					.add(query.eq("sharedWith.id", userId))
					.add(query.eq("groupUser.id", userId))
			);
		}

		return query.distinct().list();
	}

	@Override
	public QueryFolder getQueryFolder(Long folderId) {
		return getById(folderId);
	}
	
	@Override
	public void deleteFolder(QueryFolder folder) {
		super.delete(folder);
	}

	@Override
	public boolean isFolderSharedWithUser(Long folderId, Long userId, boolean forEdits) {
		Criteria<QueryFolder> query = createCriteria(QueryFolder.class, "qf")
			.leftJoin("qf.sharedWith", "su")
			.leftJoin("qf.sharedWithGroups", "sg")
			.leftJoin("sg.users", "gu");
		query.add(query.eq("qf.id", folderId))
			.add(
				query.or(
					query.eq("qf.sharedWithAll", true),
					query.eq("su.id", userId),
					query.eq("gu.id", userId)
				)
			);
		if (forEdits) {
			query.add(query.eq("qf.allowEditsBySharedUsers", true));
		}

		return query.getCount("qf.id") > 0L;
	}

	@Override
	public QueryFolder getByName(String name) {
		Criteria<QueryFolder> query = createCriteria(QueryFolder.class, "qf");
		query.add(query.eq("qf.name", name));
		return query.uniqueResult();
	}
}