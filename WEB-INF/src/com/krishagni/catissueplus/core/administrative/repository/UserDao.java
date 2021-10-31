
package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.ForgotPasswordToken;
import com.krishagni.catissueplus.core.administrative.domain.Password;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.UserUiState;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.catissueplus.core.de.events.FormCtxtSummary;

public interface UserDao extends Dao<User> {
	List<User> getUsers(UserListCriteria criteria);
	
	Long getUsersCount(UserListCriteria criteria);

	List<User> getUsersByIds(Collection<Long> userIds);
	
	List<User> getUsersByIdsAndInstitute(Collection<Long> userIds, Long instituteId);
	
	User getUser(String loginName, String domain);

	List<User> getUsers(Collection<String> loginNames, String domain);
	
	User getSystemUser();
	
	User getUserByEmailAddress(String emailAddress);
	
	Boolean isUniqueLoginName(String loginName, String domainName);
	
	Boolean isUniqueEmailAddress(String emailAddress);
	
	List<DependentEntityDetail> getDependentEntities(Long userId);
	
	ForgotPasswordToken getFpToken(String token);
	
	ForgotPasswordToken getFpTokenByUser(Long userId);
	
	void saveFpToken(ForgotPasswordToken token);
	
	void deleteFpToken(ForgotPasswordToken token);

	List<String> getActiveUsersEmailIds(Date startDate, Date endDate);
	
	List<Password> getPasswordsUpdatedBefore(Date updateDate);
	
	List<User> getInactiveUsers(Date lastLoginTime);
	
	int updateStatus(List<User> users, String status);

	List<User> getSuperAndInstituteAdmins(String instituteName);

	void saveUiState(UserUiState state);

	UserUiState getState(Long userId);

	// [{emailId, DND}, {'abc@localhost', true}]
	Map<String, Boolean> getEmailIdDnds(Collection<String> emailIds);

	List<FormCtxtSummary> getForms(String entityType, Long userId);

	List<Map<String, Object>> getFormRecords(Long instituteId, Long formId, List<String> emailIds, int startAt, int maxResults);
}
