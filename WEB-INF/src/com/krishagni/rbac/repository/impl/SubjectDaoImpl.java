package com.krishagni.rbac.repository.impl;

import java.util.Arrays;
import java.util.List;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.rbac.domain.Subject;
import com.krishagni.rbac.domain.SubjectAccess;
import com.krishagni.rbac.domain.SubjectRole;
import com.krishagni.rbac.repository.SubjectDao;

public class SubjectDaoImpl extends AbstractDao<Subject> implements SubjectDao {
        
	@Override
	public Class<?> getType() {
		return Subject.class;
	}

	@Override
	public boolean canUserPerformOps(Long subjectId, String resource, String[] ops) {
		List<Long> rows = createNamedQuery(GET_ACCESS_LIST_COUNT, Long.class)
			.setParameter("subjectId", subjectId)
			.setParameter("resource", resource)
			.setParameterList("operations", Arrays.asList(ops))
			.list();
		return !rows.isEmpty() && (rows.iterator().next() > 0);
	}

	@Override
	public List<SubjectAccess> getAccessList(Long subjectId, String resource, String[] ops) {
		return getAccessList(subjectId, new String[] { resource }, ops);
	}

	@Override
	public List<SubjectAccess> getAccessList(Long subjectId, String[] resources, String[] ops) {
		return createNamedQuery(GET_ACCESS_LIST, SubjectAccess.class)
			.setParameter("subjectId", subjectId)
			.setParameterList("resources", Arrays.asList(resources))
			.setParameterList("operations", Arrays.asList(ops))
			.list();
	}

	@Override
	public List<SubjectAccess> getAccessList(Long subjectId, String resource, String[] ops, String[] siteNames) {
		return createNamedQuery(GET_ACCESS_LIST_BY_SITES, SubjectAccess.class)
			.setParameter("subjectId", subjectId)
			.setParameter("resource", resource)
			.setParameterList("operations", Arrays.asList(ops))
			.setParameterList("sites", Arrays.asList(siteNames))
			.list();
	}

	@Override	
	public List<SubjectAccess> getAccessList(Long subjectId, Long cpId, String resource, String[] ops) {
		return getAccessList(subjectId, cpId, new String[] { resource }, ops);
	}

	@Override
	public List<SubjectAccess> getAccessList(Long subjectId, Long cpId, String[] resources, String[] ops) {
		return createNamedQuery(GET_ACCESS_LIST_BY_CP, SubjectAccess.class)
			.setParameter("subjectId", subjectId)
			.setParameter("cpId", cpId)
			.setParameterList("resources", Arrays.asList(resources))
			.setParameterList("operations", Arrays.asList(ops))
			.list();
	}

	@Override
	public List<SubjectAccess> getAccessList(Long subjectId, String[] ops) {
		return createNamedQuery(GET_OP_ACCESS_LIST, SubjectAccess.class)
			.setParameter("subjectId", subjectId)
			.setParameterList("operations", Arrays.asList(ops))
			.list();
	}

	@Override
	public List<Long> getSubjectIds(Long cpId, String resource, String[] ops) {
		return createNamedQuery(GET_SUBJECT_IDS, Long.class)
			.setParameter("cpId", cpId)
			.setParameter("resource", resource)
			.setParameterList("operations", Arrays.asList(ops))
			.list();
	}

	@Override
	public List<Long> getSubjectIds(Long instituteId, Long siteId, String resource, String[] ops) {
		return createNamedQuery(GET_SITE_SUBJECT_IDS, Long.class)
			.setParameter("instituteId", instituteId)
			.setParameter("siteId", siteId)
			.setParameter("resource", resource)
			.setParameterList("operations", Arrays.asList(ops))
			.list();
	}

	@Override
	public Integer removeRolesByCp(Long cpId) {
		return createNamedQuery(REMOVE_ROLES_BY_CP)
			.setParameter("cpId", cpId)
			.executeUpdate();
	}

	@Override
	public List<SubjectRole> getRoles(Long subjectId, String roleName) {
		Criteria<SubjectRole> query = createCriteria(SubjectRole.class, "sr");
		return query.join("sr.subject", "subject")
			.join("sr.role", "role")
			.add(query.eq("subject.id", subjectId))
			.add(query.eq("role.name", roleName))
			.list();
	}

	private static final String FQN = Subject.class.getName();

	private static final String GET_ACCESS_LIST = FQN + ".getAccessList";
	
	private static final String GET_ACCESS_LIST_BY_SITES = FQN + ".getAccessListBySites";
	
	private static final String GET_ACCESS_LIST_BY_CP = FQN + ".getAccessListByCp";

	private static final String GET_ACCESS_LIST_COUNT = FQN + ".getAccessListCount";

	private static final String GET_OP_ACCESS_LIST = FQN + ".getOpAccessList";

	private static final String GET_SUBJECT_IDS = FQN + ".getSubjectIds";

	private static final String GET_SITE_SUBJECT_IDS = FQN + ".getSiteSubjectIds";
	
	private static final String REMOVE_ROLES_BY_CP = FQN + ".removeRolesByCp";
}
