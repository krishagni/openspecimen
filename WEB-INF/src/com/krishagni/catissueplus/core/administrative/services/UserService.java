
package com.krishagni.catissueplus.core.administrative.services;

import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.UserUiState;
import com.krishagni.catissueplus.core.administrative.events.AnnouncementDetail;
import com.krishagni.catissueplus.core.administrative.events.InstituteDetail;
import com.krishagni.catissueplus.core.administrative.events.PasswordDetails;
import com.krishagni.catissueplus.core.administrative.events.UserDetail;
import com.krishagni.catissueplus.core.administrative.repository.UserListCriteria;
import com.krishagni.catissueplus.core.common.events.BulkEntityDetail;
import com.krishagni.catissueplus.core.common.events.DeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.events.EntityFormRecords;
import com.krishagni.catissueplus.core.de.events.FormCtxtSummary;
import com.krishagni.catissueplus.core.de.events.FormRecordsList;
import com.krishagni.catissueplus.core.de.events.GetEntityFormRecordsOp;
import com.krishagni.catissueplus.core.de.events.GetFormRecordsListOp;
import com.krishagni.rbac.events.SubjectRoleDetail;

public interface UserService {
	public ResponseEvent<List<UserSummary>> getUsers(RequestEvent<UserListCriteria> req);
	
	public ResponseEvent<Long> getUsersCount(RequestEvent<UserListCriteria> req);
	
	public ResponseEvent<UserDetail> getUser(RequestEvent<Long> req);

	public ResponseEvent<UserDetail> createUser(RequestEvent<UserDetail> req);

	public ResponseEvent<UserDetail> updateUser(RequestEvent<UserDetail> req);
	
	public ResponseEvent<UserDetail> patchUser(RequestEvent<UserDetail> req);

	public ResponseEvent<UserDetail> updateStatus(RequestEvent<UserDetail> req);

	public ResponseEvent<Boolean> resetPassword(RequestEvent<PasswordDetails> req);

	public ResponseEvent<Boolean> changePassword(RequestEvent<PasswordDetails> req);

	public ResponseEvent<Boolean> forgotPassword(RequestEvent<UserDetail> req);

	public ResponseEvent<List<UserDetail>> bulkUpdateUsers(RequestEvent<BulkEntityDetail<UserDetail>> req);
	
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<Long> req);

	public ResponseEvent<UserDetail> deleteUser(RequestEvent<DeleteEntityOp> req);

	public ResponseEvent<List<SubjectRoleDetail>> getCurrentUserRoles();

	public ResponseEvent<UserUiState> getUiState();

	public ResponseEvent<UserUiState> saveUiState(RequestEvent<Map<String, Object>> req);

	public ResponseEvent<InstituteDetail> getInstitute(RequestEvent<Long> req);

	public ResponseEvent<Boolean> broadcastAnnouncement(RequestEvent<AnnouncementDetail> req);

	//
	// Returns list of forms for data entry given the user ID and entity type.
	//
	ResponseEvent<List<FormCtxtSummary>> getForms(RequestEvent<Map<String, Object>> req);

	//
	// Returns list of records of a given form for a given user.
	//
	ResponseEvent<EntityFormRecords> getFormRecords(RequestEvent<GetEntityFormRecordsOp> req);

	//
	// Returns list of records of all forms for a given user.
	//
	ResponseEvent<List<FormRecordsList>> getAllFormRecords(RequestEvent<GetFormRecordsListOp> req);
}
