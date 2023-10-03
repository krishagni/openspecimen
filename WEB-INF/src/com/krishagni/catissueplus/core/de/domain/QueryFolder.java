package com.krishagni.catissueplus.core.de.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.de.repository.DaoFactory;

@Configurable
@Audited
public class QueryFolder extends BaseEntity {
	private String name;

	private User owner;
	
	private boolean sharedWithAll;

	private boolean allowEditsBySharedUsers;

	private Set<User> sharedWith = new HashSet<>();

	private Set<UserGroup> sharedWithGroups = new HashSet<>();

	private Set<SavedQuery> savedQueries = new HashSet<>();

	@Autowired
	private DaoFactory daoFactory;

	public static String getEntityName() {
		return "query_folder";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isSharedWithAll() {
		return sharedWithAll;
	}

	public void setSharedWithAll(Boolean sharedWithAll) {
		this.sharedWithAll = sharedWithAll;
	}

	public boolean isAllowEditsBySharedUsers() {
		return allowEditsBySharedUsers;
	}

	public void setAllowEditsBySharedUsers(boolean allowEditsBySharedUsers) {
		this.allowEditsBySharedUsers = allowEditsBySharedUsers;
	}

	@AuditJoinTable(name = "OS_QUERY_FOLDER_USERS_AUD")
	public Set<User> getSharedWith() {
		return sharedWith;
	}

	public void setSharedWith(Set<User> sharedWith) {
		this.sharedWith = sharedWith;
	}

	@AuditJoinTable(name = "OS_QUERY_FOLDER_USER_GRPS_AUD")
	public Set<UserGroup> getSharedWithGroups() {
		return sharedWithGroups;
	}

	public void setSharedWithGroups(Set<UserGroup> sharedWithGroups) {
		this.sharedWithGroups = sharedWithGroups;
	}

	public Set<User> getAllSharedUsers() {
		Set<User> users = new HashSet<>(getSharedWith());
		getSharedWithGroups().forEach(g -> users.addAll(g.getUsers()));
		return users;
	}

	@AuditJoinTable(name = "OS_QUERY_FOLDER_QUERIES_AUD")
	public Set<SavedQuery> getSavedQueries() {
		return savedQueries;
	}

	public void setSavedQueries(Set<SavedQuery> savedQueries) {
		this.savedQueries = savedQueries;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public void addQueries(List<SavedQuery> queries) {
		savedQueries.addAll(queries);
	}
	
	public void updateQueries(List<SavedQuery> queries) {
		savedQueries.retainAll(queries);

		for (SavedQuery query : queries) {
			if (!savedQueries.contains(query)) {
				savedQueries.add(query);
			}
		}
	}
	
	public void removeQueries(List<SavedQuery> queries) {
		savedQueries.removeAll(queries);
	}
	
	public void removeQueriesById(List<Long> queryIds) {
		Iterator<SavedQuery> iterator = savedQueries.iterator();
		while (iterator.hasNext()) {
			SavedQuery query = iterator.next();
			if (queryIds.contains(query.getId())) {
				iterator.remove();
			}
		}		
	}

	public Collection<User> addSharedUsers(Collection<User> users) {
		Set<User> addedUsers = new HashSet<>(users);
		addedUsers.removeAll(sharedWith);
		sharedWith.addAll(addedUsers);
		return addedUsers;
	}
	
	public void removeSharedUsers(Collection<User> users) {
		sharedWith.removeAll(users);
	}
	
	public Collection<User> updateSharedUsers(Collection<User> users) {
		Set<User> newUsers = new HashSet<>(users);
		newUsers.removeAll(sharedWith);
		
		sharedWith.retainAll(users);
		sharedWith.addAll(newUsers);
		return newUsers;
	}

	public Collection<UserGroup> addSharedGroups(Collection<UserGroup> groups) {
		Set<UserGroup> addedGrps = new HashSet<>(groups);
		addedGrps.removeAll(getSharedWithGroups());
		getSharedWithGroups().addAll(addedGrps);
		return addedGrps;
	}

	public void removeSharedGroups(Collection<UserGroup> groups) {
		getSharedWithGroups().removeAll(groups);
	}

	public Collection<UserGroup> updateSharedGroups(Collection<UserGroup> groups) {
		Set<UserGroup> newGroups = new HashSet<>(groups);
		newGroups.removeAll(getSharedWithGroups());

		getSharedWithGroups().retainAll(groups);
		getSharedWithGroups().addAll(groups);
		return newGroups;
	}
	
	public boolean canUserAccess(User user, boolean forEdits) {
		if (user == null || (forEdits && getOwner().isSysUser())) {
			return false;
		} else if (user.isAdmin()) {
			return true;
		} else if (user.isInstituteAdmin()) {
			return getOwner() != null && user.getInstitute().equals(getOwner().getInstitute());
		} else if (user.equals(getOwner())) {
			return true;
		} else {
			return daoFactory.getQueryFolderDao().isFolderSharedWithUser(getId(), user.getId(), forEdits);
		}
	}
		
	public void update(QueryFolder folder) {
		setName(folder.getName());

		getSavedQueries().retainAll(folder.getSavedQueries());
		getSavedQueries().addAll(folder.getSavedQueries());

		setSharedWithAll(folder.isSharedWithAll());
		setAllowEditsBySharedUsers(folder.isAllowEditsBySharedUsers());
		if (folder.isSharedWithAll()) {
			getSharedWith().clear();
			getSharedWithGroups().clear();
		} else {
			getSharedWith().retainAll(folder.getSharedWith());
			getSharedWith().addAll(folder.getSharedWith());

			getSharedWithGroups().retainAll(folder.getSharedWithGroups());
			getSharedWithGroups().addAll(folder.getSharedWithGroups());
		}		
	}
}
