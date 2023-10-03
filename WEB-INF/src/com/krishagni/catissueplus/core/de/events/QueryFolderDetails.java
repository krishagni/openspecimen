package com.krishagni.catissueplus.core.de.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.domain.QueryFolder;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;

public class QueryFolderDetails extends QueryFolderSummary {

	private List<UserSummary> sharedWith = new ArrayList<>();

	private List<UserGroupSummary> sharedWithGroups = new ArrayList<>();

	private List<SavedQuerySummary> queries = new ArrayList<>();

	public List<UserSummary> getSharedWith() {
		return sharedWith;
	}

	public void setSharedWith(List<UserSummary> sharedWith) {
		this.sharedWith = sharedWith;
	}

	public List<UserGroupSummary> getSharedWithGroups() {
		return sharedWithGroups;
	}

	public void setSharedWithGroups(List<UserGroupSummary> sharedWithGroups) {
		this.sharedWithGroups = sharedWithGroups;
	}

	public List<SavedQuerySummary> getQueries() {
		return queries;
	}

	public void setQueries(List<SavedQuerySummary> queries) {
		this.queries = queries;
	}

	public static QueryFolderDetails from(QueryFolder folder) {
		return from(folder, true);
	}

	public static QueryFolderDetails from(QueryFolder folder, boolean includeQueries) {
		QueryFolderDetails fd = new QueryFolderDetails();
		fd.setId(folder.getId());
		fd.setName(folder.getName());
		fd.setOwner(UserSummary.from(folder.getOwner()));
		fd.setSharedWith(fromUsers(folder.getSharedWith()));
		fd.setSharedWithGroups(UserGroupSummary.from(folder.getSharedWithGroups()));
		fd.setSharedWithAll(folder.isSharedWithAll());
		fd.setAllowEditsBySharedUsers(folder.isAllowEditsBySharedUsers());
		if (includeQueries) {
			fd.setQueries(fromSavedQueries(folder.getSavedQueries()));
		}

		return fd;
	}

	private static List<SavedQuerySummary> fromSavedQueries(Set<SavedQuery> savedQueries) {
		return savedQueries.stream().filter(q -> q.getDeletedOn() == null)
			.map(SavedQuerySummary::fromSavedQuery)
			.collect(Collectors.toList());
	}

	private static List<UserSummary> fromUsers(Set<User> users) {
		return users.stream().map(UserSummary::from).collect(Collectors.toList());
	}
}