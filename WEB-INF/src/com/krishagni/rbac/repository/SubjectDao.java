package com.krishagni.rbac.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.rbac.domain.Subject;
import com.krishagni.rbac.domain.SubjectAccess;
import com.krishagni.rbac.domain.SubjectRole;

public interface SubjectDao extends Dao<Subject> {		
	boolean canUserPerformOps(Long subjectId, String resource, String[] ops);
	
	List<SubjectAccess> getAccessList(Long subjectId, String resource, String[] ops);

	List<SubjectAccess> getAccessList(Long subjectId, String[] resources, String[] ops);
	
	List<SubjectAccess> getAccessList(Long subjectId, String resource, String[] ops, String[] siteNames);
	
	List<SubjectAccess> getAccessList(Long subjectId, Long cpId, String resource, String[] ops);

	List<SubjectAccess> getAccessList(Long subjectId, Long cpId, String[]resources, String[] ops);

	List<SubjectAccess> getAccessList(Long subjectId, String[] ops);

	List<Long> getSubjectIds(Long cpId, String resource, String[] ops);

	List<Long> getSubjectIds(Long instituteId, Long siteId, String resource, String[] ops);
	
	Integer removeRolesByCp(Long cpId);

	List<SubjectRole> getRoles(Long subjectId, String roleName);
}
