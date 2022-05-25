package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListsFolder;
import com.krishagni.catissueplus.core.common.util.Utility;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpecimenListsFolderDetail extends SpecimenListsFolderSummary {
	private List<UserGroupSummary> userGroups;

	private List<Long> cartIds;

	public List<UserGroupSummary> getUserGroups() {
		return userGroups;
	}

	public void setUserGroups(List<UserGroupSummary> userGroups) {
		this.userGroups = userGroups;
	}

	public List<Long> getCartIds() {
		return cartIds;
	}

	public void setCartIds(List<Long> cartIds) {
		this.cartIds = cartIds;
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
