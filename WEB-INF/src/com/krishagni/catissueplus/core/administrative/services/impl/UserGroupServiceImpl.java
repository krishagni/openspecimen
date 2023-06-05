package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.administrative.domain.UserGroupSavedEvent;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserGroupErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserGroupFactory;
import com.krishagni.catissueplus.core.administrative.events.UserGroupDetail;
import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.administrative.repository.UserGroupListCriteria;
import com.krishagni.catissueplus.core.administrative.services.UserGroupService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class UserGroupServiceImpl implements UserGroupService {

	private UserGroupFactory groupFactory;

	private DaoFactory daoFactory;

	public void setGroupFactory(UserGroupFactory groupFactory) {
		this.groupFactory = groupFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<UserGroupSummary>> getGroups(RequestEvent<UserGroupListCriteria> req) {
		try {
			UserGroupListCriteria crit = req.getPayload();
			if (!AuthUtil.isAdmin() && !crit.listAll()) {
				crit.institute(AuthUtil.getCurrentUserInstitute().getName());
			}

			List<UserGroup> groups = daoFactory.getUserGroupDao().getGroups(crit);
			List<UserGroupSummary> result = UserGroupSummary.from(groups);

			if (crit.includeStat()) {
				Set<Long> groupIds = result.stream().map(UserGroupSummary::getId).collect(Collectors.toSet());
				Map<Long, Integer> usersCount = daoFactory.getUserGroupDao().getGroupUsersCount(groupIds);
				result.forEach(gs -> gs.setNoOfUsers(usersCount.getOrDefault(gs.getId(), 0)));
			}

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getGroupsCount(RequestEvent<UserGroupListCriteria> req) {
		try {
			UserGroupListCriteria crit = req.getPayload();
			if (!AuthUtil.isAdmin() && !crit.listAll()) {
				crit.institute(AuthUtil.getCurrentUserInstitute().getName());
			}

			return ResponseEvent.response(daoFactory.getUserGroupDao().getGroupsCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<UserGroupDetail> getGroup(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			UserGroup group = getGroup(crit.getId(), crit.getName());
			return ResponseEvent.response(UserGroupDetail.from(group, crit.paramBoolean("includeUsers")));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<UserGroupDetail> createGroup(RequestEvent<UserGroupDetail> req) {
		try {
			UserGroup group = groupFactory.createGroup(req.getPayload());
			ensureCreateUpdateRights(group.getInstitute());
			ensureUniqueGroupName(null, group);
			daoFactory.getUserGroupDao().saveOrUpdate(group);
			publishGroupSavedEvent(group);
			return ResponseEvent.response(UserGroupDetail.from(group, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<UserGroupDetail> updateGroup(RequestEvent<UserGroupDetail> req) {
		try {
			UserGroupDetail input = req.getPayload();
			UserGroup existing = getGroup(input.getId(), input.getName());
			ensureCreateUpdateRights(existing.getInstitute());

			UserGroup group = groupFactory.createGroup(input, existing);
			ensureCreateUpdateRights(group.getInstitute());
			ensureUniqueGroupName(existing, group);

			existing.update(group);
			publishGroupSavedEvent(existing);
			return ResponseEvent.response(UserGroupDetail.from(existing, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<UserGroupDetail> deleteGroup(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			UserGroup group = getGroup(crit.getId(), crit.getName());
			ensureDeleteRights(group.getInstitute());
			group.delete();
			publishGroupSavedEvent(group);
			return ResponseEvent.response(UserGroupDetail.from(group, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<UserGroupDetail> addUsers(RequestEvent<UserGroupDetail> req) {
		try {
			UserGroupDetail input = req.getPayload();
			UserGroup group = getGroup(input.getId(), input.getName());
			ensureCreateUpdateRights(group.getInstitute());
			group.addUsers(groupFactory.getUsers(input.getUsers()));
			publishGroupSavedEvent(group);
			return ResponseEvent.response(UserGroupDetail.from(group, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<UserGroupDetail> removeUsers(RequestEvent<UserGroupDetail> req) {
		try {
			UserGroupDetail input = req.getPayload();
			UserGroup group = getGroup(input.getId(), input.getName());
			ensureCreateUpdateRights(group.getInstitute());
			group.removeUsers(groupFactory.getUsers(input.getUsers()));
			publishGroupSavedEvent(group);
			return ResponseEvent.response(UserGroupDetail.from(group, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> isMemberOf(RequestEvent<String> req) {
		try {
			String groupName = req.getPayload();
			return ResponseEvent.response(daoFactory.getUserGroupDao().isMemberOf(groupName, AuthUtil.getCurrentUser().getId()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private UserGroup getGroup(Long groupId, String groupName) {
		UserGroup result = null;
		Object key = null;

		if (groupId != null) {
			result = daoFactory.getUserGroupDao().getById(groupId);
			key = groupId;
		} else if (StringUtils.isNotBlank(groupName)) {
			result = daoFactory.getUserGroupDao().getByName(groupName);
			key = groupName;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(UserGroupErrorCode.ID_NAME_REQ);
		} else if (result == null) {
			throw OpenSpecimenException.userError(UserGroupErrorCode.NOT_FOUND, key);
		}

		return result;
	}

	private void ensureUniqueGroupName(UserGroup existing, UserGroup newGroup) {
		if (existing != null && existing.getName().equalsIgnoreCase(newGroup.getName())) {
			return;
		}

		UserGroup dbGroup = daoFactory.getUserGroupDao().getByName(newGroup.getName());
		if (dbGroup != null) {
			throw OpenSpecimenException.userError(UserGroupErrorCode.DUP_NAME, newGroup.getName());
		}
	}

	private void ensureCreateUpdateRights(Institute institute) {
		User user = new User();
		user.setInstitute(institute);
		AccessCtrlMgr.getInstance().ensureUpdateUserRights(user);
	}

	private void ensureDeleteRights(Institute institute) {
		User user = new User();
		user.setInstitute(institute);
		AccessCtrlMgr.getInstance().ensureDeleteUserRights(user);
	}

	private void publishGroupSavedEvent(UserGroup group) {
		EventPublisher.getInstance().publish(new UserGroupSavedEvent(group));
	}
}
