package com.krishagni.catissueplus.core.de.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.QueryFolder;

public class QueryFolderSummary {
	private Long id;
	
	private String name;
	
	private UserSummary owner;
	
	private boolean sharedWithAll;

	private boolean allowEditsBySharedUsers;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public UserSummary getOwner() {
		return owner;
	}

	public void setOwner(UserSummary owner) {
		this.owner = owner;
	}

	public boolean isSharedWithAll() {
		return sharedWithAll;
	}

	public void setSharedWithAll(boolean sharedWithAll) {
		this.sharedWithAll = sharedWithAll;
	}

	public boolean isAllowEditsBySharedUsers() {
		return allowEditsBySharedUsers;
	}

	public void setAllowEditsBySharedUsers(boolean allowEditsBySharedUsers) {
		this.allowEditsBySharedUsers = allowEditsBySharedUsers;
	}

	public static QueryFolderSummary from(QueryFolder queryFolder){
		QueryFolderSummary folderSummary = new QueryFolderSummary();
		folderSummary.setId(queryFolder.getId());
		folderSummary.setName(queryFolder.getName());
		folderSummary.setOwner(UserSummary.from(queryFolder.getOwner()));
		folderSummary.setSharedWithAll(queryFolder.isSharedWithAll());
		folderSummary.setAllowEditsBySharedUsers(queryFolder.isAllowEditsBySharedUsers());
		return folderSummary;
	}
	
	public static List<QueryFolderSummary> from(Collection<QueryFolder> folders) {
		return Utility.nullSafeStream(folders).map(QueryFolderSummary::from).collect(Collectors.toList());
	}
}