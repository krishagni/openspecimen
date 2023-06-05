package com.krishagni.catissueplus.core.administrative.services;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.events.UserGroupDetail;
import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.administrative.repository.UserGroupListCriteria;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface UserGroupService {
	ResponseEvent<List<UserGroupSummary>> getGroups(RequestEvent<UserGroupListCriteria> req);

	ResponseEvent<Long> getGroupsCount(RequestEvent<UserGroupListCriteria> req);

	ResponseEvent<UserGroupDetail> getGroup(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<UserGroupDetail> createGroup(RequestEvent<UserGroupDetail> req);

	ResponseEvent<UserGroupDetail> updateGroup(RequestEvent<UserGroupDetail> req);

	ResponseEvent<UserGroupDetail> deleteGroup(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<UserGroupDetail> addUsers(RequestEvent<UserGroupDetail> req);

	ResponseEvent<UserGroupDetail> removeUsers(RequestEvent<UserGroupDetail> req);

	ResponseEvent<Boolean> isMemberOf(RequestEvent<String> req);
}
