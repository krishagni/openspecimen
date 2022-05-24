package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListsFolder;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenListsFolderDetail extends SpecimenListsFolderSummary {
	private List<UserGroupSummary> userGroups;

	public List<UserGroupSummary> getUserGroups() {
		return userGroups;
	}

	public void setUserGroups(List<UserGroupSummary> userGroups) {
		this.userGroups = userGroups;
	}

	public static SpecimenListsFolderDetail from(SpecimenListsFolder folder) {
		SpecimenListsFolderDetail result = fromTo(folder, new SpecimenListsFolderDetail());
		result.setUserGroups(UserGroupSummary.from(folder.getUserGroups()));
		return result;
	}

	public static List<SpecimenListsFolderDetail> from(Collection<SpecimenListsFolder> folders) {
		return Utility.nullSafeStream(folders).map(SpecimenListsFolderDetail::from).collect(Collectors.toList());
	}
}
