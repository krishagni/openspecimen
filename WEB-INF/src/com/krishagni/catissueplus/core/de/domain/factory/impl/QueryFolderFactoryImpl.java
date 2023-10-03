package com.krishagni.catissueplus.core.de.domain.factory.impl;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.QueryFolder;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.domain.factory.QueryFolderFactory;
import com.krishagni.catissueplus.core.de.events.QueryFolderDetails;
import com.krishagni.catissueplus.core.de.events.SavedQuerySummary;
import com.krishagni.catissueplus.core.de.repository.DaoFactory;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;

public class QueryFolderFactoryImpl implements QueryFolderFactory {
	private DaoFactory daoFactory;
	
	private com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory bioSpmnDaoFactory;

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory getBioSpmnDaoFactory() {
		return bioSpmnDaoFactory;
	}

	public void setBioSpmnDaoFactory(com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory bioSpmnDaoFactory) {
		this.bioSpmnDaoFactory = bioSpmnDaoFactory;
	}

	@Override
	public QueryFolder createQueryFolder(QueryFolderDetails details) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		QueryFolder queryFolder = new QueryFolder();

		Long userId = details.getOwner() != null ? details.getOwner().getId() : null;
		setOwner(queryFolder, userId , ose);
		setName(queryFolder, details.getName(), ose);
		
		List<Long> queryIds = new ArrayList<>();
		for (SavedQuerySummary query : details.getQueries()) {
			queryIds.add(query.getId());
		}
		setQueries(queryFolder, queryIds, ose);

		queryFolder.setSharedWithAll(details.isSharedWithAll());
		queryFolder.setAllowEditsBySharedUsers(details.isAllowEditsBySharedUsers());
		setSharedUsers(queryFolder, userId, details.getSharedWith(), ose);
		setSharedUserGroups(queryFolder, details.getSharedWithGroups(), ose);
		ose.checkAndThrow();
		return queryFolder;
	}
	
	private void setOwner(QueryFolder folder, Long userId, OpenSpecimenException ose) {
		if (userId == null) {
			ose.addError(UserErrorCode.NOT_FOUND);			
		} else {
			User user = bioSpmnDaoFactory.getUserDao().getById(userId);
			if (user == null) {
				ose.addError(UserErrorCode.NOT_FOUND);
			} else {
				folder.setOwner(user);
			}						
		}		
	}
	
	private void setName(QueryFolder folder, String name, OpenSpecimenException ose) {
		if (StringUtils.isBlank(name)) {
			ose.addError(SavedQueryErrorCode.FOLDER_NAME_REQUIRED);
		} else {
			folder.setName(name);
		}		
	}
	
	private void setQueries(QueryFolder folder, List<Long> queryIds, OpenSpecimenException ose) {
		if (queryIds != null && !queryIds.isEmpty()) {
			List<SavedQuery> queries = daoFactory.getSavedQueryDao().getQueriesByIds(queryIds);
			if (queries.size() != queryIds.size()) {
				ose.addError(SavedQueryErrorCode.NOT_FOUND);
			} else {
				User user = AuthUtil.getCurrentUser();
				Set<SavedQuery> result = new HashSet<>();
				for (SavedQuery query : queries) {
					if (!query.canUpdateOrDelete(user)) {
						ose.addError(SavedQueryErrorCode.QUERIES_NOT_ACCESSIBLE);
					}

					result.add(query);
				}

				folder.setSavedQueries(result);
			}			
		} else {
			folder.getSavedQueries().clear();
		}
	}
	
	private void setSharedUsers(QueryFolder folder, Long ownerId, List<UserSummary> users, OpenSpecimenException ose) {
		if (!folder.isSharedWithAll() && CollectionUtils.isNotEmpty(users)) {
			List<Long> userIds = users.stream()
				.filter(u -> !u.getId().equals(ownerId))
				.map(UserSummary::getId)
				.collect(Collectors.toList());

			List<User> sharedUsers = bioSpmnDaoFactory.getUserDao().getUsersByIds(userIds);
			if (sharedUsers.size() != userIds.size()) {
				ose.addError(SavedQueryErrorCode.INVALID_SHARE_ACCESS_DETAILS);
			} else {
				folder.setSharedWith(new HashSet<User>(sharedUsers));
			}
		} else {
			folder.getSharedWith().clear();
		}
	}

	private void setSharedUserGroups(QueryFolder folder, List<UserGroupSummary> userGroups, OpenSpecimenException ose) {
		Set<Long> groupIds = Utility.nullSafeStream(userGroups).map(UserGroupSummary::getId).collect(Collectors.toSet());
		if (!folder.isSharedWithAll() && CollectionUtils.isNotEmpty(groupIds)) {
			List<UserGroup> sharedGroups = bioSpmnDaoFactory.getUserGroupDao().getByIds(groupIds);
			if (sharedGroups.size() != groupIds.size()) {
				ose.addError(SavedQueryErrorCode.INVALID_GROUPS_LIST);
			} else {
				folder.setSharedWithGroups(new HashSet<>(sharedGroups));
			}
		} else {
			folder.getSharedWithGroups().clear();
		}
	}
}
