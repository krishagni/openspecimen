package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListsFolder;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface SpecimenListsFolderDao extends Dao<SpecimenListsFolder> {
	List<SpecimenListsFolder> getFolders(SpecimenListsFoldersCriteria criteria);

	Long getFoldersCount(SpecimenListsFoldersCriteria criteria);

	Map<Long, Integer> getFolderCartsCount(Collection<Long> folderIds);

	boolean isAccessible(Long folderId, Long userId);

	List<Long> getCartIds(Long folderId);

	int addCarts(Long folderId, List<Long> cartIds);

	int removeCarts(Long folderId, List<Long> cartIds);
}
