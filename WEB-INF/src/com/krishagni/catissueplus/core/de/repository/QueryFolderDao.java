package com.krishagni.catissueplus.core.de.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.catissueplus.core.de.domain.QueryFolder;

public interface QueryFolderDao extends Dao<QueryFolder> {		
	List<QueryFolder> getUserFolders(Long userId, Long instituteId);
	
	QueryFolder getQueryFolder(Long folderId);
	
	QueryFolder getByName(String name);
	
	void deleteFolder(QueryFolder folder);

	boolean isFolderSharedWithUser(Long folderId, Long userId, boolean forEdits);
}
