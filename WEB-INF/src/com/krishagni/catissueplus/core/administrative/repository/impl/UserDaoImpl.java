
package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.ForgotPasswordToken;
import com.krishagni.catissueplus.core.administrative.domain.Password;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.UserUiState;
import com.krishagni.catissueplus.core.administrative.repository.UserDao;
import com.krishagni.catissueplus.core.administrative.repository.UserListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Conjunction;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Query;
import com.krishagni.catissueplus.core.common.repository.Restriction;
import com.krishagni.catissueplus.core.common.repository.SubQuery;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.FormCtxtSummary;

public class UserDaoImpl extends AbstractDao<User> implements UserDao {

	@Override
	public Class<?> getType() {
		return User.class;
	}

	public List<User> getUsers(UserListCriteria listCrit) {
		Criteria<User> query = getUsersListQuery(listCrit);
		listCrit.maxResults(CollectionUtils.isNotEmpty(listCrit.ids()) ? listCrit.ids().size() : listCrit.maxResults());
		return query.orderBy(query.asc("u.firstName"), query.asc("u.lastName"))
			.list(listCrit.startAt(), listCrit.maxResults());
	}
	
	public Long getUsersCount(UserListCriteria listCrit) {
		return getUsersListQuery(listCrit).getCount("u.id");
	}

	public List<User> getUsersByIds(Collection<Long> userIds) {
		return getUsersByIdsAndInstitute(userIds, null);
	}

	public List<User> getUsersByIdsAndInstitute(Collection<Long> userIds, Long instituteId) {
		Criteria<User> query = createCriteria(User.class, "u");
		query.add(query.in("u.id", userIds));
		
		if (instituteId != null) {
			query.join("u.institute", "inst")
				.add(query.eq("inst.id", instituteId));
		}
		
		return query.list();
	}

	public User getUser(String loginName, String domainName) {
		List<User> users = getUsers(Collections.singletonList(loginName), domainName);
		return users.isEmpty() ? null : users.get(0);
	}

	public List<User> getUsers(Collection<String> loginNames, String domainName) {
		Criteria<User> query = createCriteria(User.class, "u");
		query.add(query.in("u.loginName", loginNames));

		if (StringUtils.isNotBlank(domainName)) {
			query.join("u.authDomain", "domain")
				.add(query.eq("domain.name", domainName));
		}

		return query.list();
	}
	
	@Override
	public User getSystemUser() {
		return getUser(User.SYS_USER, User.DEFAULT_AUTH_DOMAIN);
	}
	
	public User getUserByEmailAddress(String emailAddress) {
		String hql = String.format(GET_USER_BY_EMAIL_HQL, " and activityStatus != 'Disabled'");
		List<User> users = executeGetUserByEmailAddressHql(hql, Collections.singletonList(emailAddress));
		return users.isEmpty() ? null : users.get(0);
	}

	public List<User> getUsersByEmailAddress(Collection<String> emailAddresses) {
		String hql = String.format(GET_USER_BY_EMAIL_HQL, " and activityStatus != 'Disabled'");
		return executeGetUserByEmailAddressHql(hql, emailAddresses);
	}
	
	public Boolean isUniqueLoginName(String loginName, String domainName) {
		return getUser(loginName, domainName) == null;
	}
	
	public Boolean isUniqueEmailAddress(String emailAddress) {
		String hql = String.format(GET_USER_BY_EMAIL_HQL, "");
		List<User> users = executeGetUserByEmailAddressHql(hql, Collections.singletonList(emailAddress));
		return users.isEmpty();
	}

	public List<DependentEntityDetail> getDependentEntities(Long userId) {
		List<Object[]> rows = createNamedQuery(GET_DEPENDENT_ENTITIES, Object[].class)
			.setParameter("userId", userId)
			.list();
		return getDependentEntities(rows);
	}

	public ForgotPasswordToken getFpToken(String token) {
		List<ForgotPasswordToken> result = createNamedQuery(GET_FP_TOKEN, ForgotPasswordToken.class)
			.setParameter("token", token)
			.list();
		return result.isEmpty() ? null : result.get(0);
	}

	public ForgotPasswordToken getFpTokenByUser(Long userId) {
		List<ForgotPasswordToken> result = createNamedQuery(GET_FP_TOKEN_BY_USER, ForgotPasswordToken.class)
			.setParameter("userId", userId)
			.list();
		return result.isEmpty() ? null : result.get(0);
	}
	
	@Override
	public void saveFpToken(ForgotPasswordToken token) {
		getCurrentSession().saveOrUpdate(token);
	}
	
	@Override
	public void deleteFpToken(ForgotPasswordToken token) {
		super.delete(token);
	}

	@Override
	public List<String> getActiveUsersEmailIds(Date startDate, Date endDate) {
		return createNamedQuery(GET_ACTIVE_USERS_EMAIL_IDS, String.class)
			.setParameter("startDate", startDate)
			.setParameter("endDate", endDate)
			.list();
	}

	@Override
	public Password getLatestPassword(Long userId) {
		Criteria<Password> query = createCriteria(Password.class, "p");
		List<Password> passwords = query.join("p.user", "user")
			.add(query.eq("user.id", userId))
			.addOrder(query.desc("p.updationDate"))
			.list(0, 1);
		return passwords.isEmpty() ? null : passwords.get(0);
	}

	@Override
	public List<Password> getPasswordsUpdatedBefore(Date updateDate) {
		List<Object[]> rows = createNamedQuery(GET_PASSWDS_UPDATED_BEFORE, Object[].class)
			.setParameter("updateDate", updateDate)
			.list();

		return rows.stream().map(row -> {
			int idx = 0;

			User user = new User();
			user.setId((Long)row[idx++]);
			user.setFirstName((String)row[idx++]);
			user.setLastName((String)row[idx++]);
			user.setEmailAddress((String)row[idx++]);

			Password password = new Password();
			password.setUser(user);
			password.setUpdationDate((Date)row[idx++]);

			return password;

		}).collect(Collectors.toList());
	}

	@Override
	public List<Password> getPasswords(Date fromDate, Date toDate, Long lastId, List<User> updatedBy) {
		Criteria<Password> query = createCriteria(Password.class, "password");
		query.leftJoin("password.updatedBy", "updatedBy")
			.addOrder(query.desc("password.updationDate"))
			.addOrder(query.desc("password.id"));

		if (fromDate != null) {
			query.add(query.ge("password.updationDate", fromDate));
		}

		if (toDate != null) {
			query.add(query.le("password.updationDate", toDate));
		}

		if (CollectionUtils.isNotEmpty(updatedBy)) {
			query.add(query.in("updatedBy.id", updatedBy.stream().map(User::getId).collect(Collectors.toList())));
		}

		if (lastId != null) {
			query.add(query.lt("password.id", lastId));
		}

		return query.list(0, 100);
	}

	@Override
	public List<User> getInactiveUsers(Date lastLoginTime) {
		return createNamedQuery(GET_INACTIVE_USERS, User.class)
			.setParameter("lastLoginTime", lastLoginTime)
			.list();
	}

	@Override
	public Map<Long, Date> getLatestLoginTime(List<Long> userIds) {
		List<Object[]> rows = createNamedQuery(GET_LATEST_LOGIN_TIMES, Object[].class)
			.setParameterList("userIds", userIds)
			.list();

		Map<Long, Date> result = new HashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Date) row[1]);
		}

		return result;
	}

	@Override
	public int updateStatus(List<User> users, String status) {
		if (CollectionUtils.isEmpty(users)) {
			return 0;
		}

		return createNamedQuery(UPDATE_STATUS)
			.setParameter("activityStatus", status)
			.setParameterList("userIds", users.stream().map(User::getId).collect(Collectors.toList()))
			.executeUpdate();
	}

	@Override
	public List<User> getSuperAndInstituteAdmins(String instituteName) {
		UserListCriteria crit = new UserListCriteria().activityStatus("Active").type("SUPER");
		List<User> users = getUsers(crit);

		if (StringUtils.isNotBlank(instituteName)) {
			users.addAll(getUsers(crit.type("INSTITUTE").instituteName(instituteName)));
		}
		return users;
	}

	@Override
	public Map<String, Pair<Boolean, String>> getEmailIdStatuses(Collection<String> emailIds) {
		List<Object[]> rows = createNamedQuery(GET_EMAIL_ID_STATUSES, Object[].class)
			.setParameterList("emailIds", emailIds)
			.list();

		return rows.stream().collect(
			Collectors.toMap(
				row -> (String) row[0], // email ID
				row -> Pair.make((Boolean) row[1], (String) row[2]) // dnd, activityStatus
			)
		);
	}

	@Override
	public void saveUiState(UserUiState state) {
		getCurrentSession().saveOrUpdate(state);
	}

	@Override
	public UserUiState getState(Long userId) {
		return createNamedQuery(GET_STATE, UserUiState.class)
			.setParameter("userId", userId)
			.uniqueResult();
	}

	@Override
	public List<FormCtxtSummary> getForms(String entityType, Long userId) {
		List<Object[]> rows = createNamedQuery(GET_FORMS, Object[].class)
			.setParameter("entityType", entityType)
			.setParameter("userId", userId)
			.list();
		return getEntityForms(rows);
	}

	@Override
	public List<Map<String, Object>> getFormRecords(Long instituteId, Long formId, List<String> emailIds, int startAt, int maxResults) {
		String sql = getCurrentSession().getNamedQuery(GET_FORM_RECS).getQueryString();
		if (CollectionUtils.isNotEmpty(emailIds)) {
			int orderByIdx = sql.lastIndexOf("order by");
			sql = sql.substring(0, orderByIdx) + " and usr.email_address in (:emailIds) " + sql.substring(orderByIdx);
		}

		if (instituteId != null && instituteId != -1L) {
			int orderByIdx = sql.lastIndexOf("order by");
			sql = sql.substring(0, orderByIdx) + " and institute.identifier = " + instituteId + " " + sql.substring(orderByIdx);
		}

		Query<Object[]> query = createNativeQuery(sql, Object[].class)
			.setParameter("formId", formId)
			.setFirstResult(startAt)
			.setMaxResults(maxResults);
		if (CollectionUtils.isNotEmpty(emailIds)) {
			query.setParameterList("emailIds", emailIds);
		}

		return query.list().stream()
			.map(
				(row) -> {
					int idx = -1;
					Map<String, Object> record = new HashMap<>();
					record.put("instituteId", ((Number) row[++idx]).longValue());
					record.put("instituteName", row[++idx]);
					record.put("userId", ((Number) row[++idx]).longValue());
					record.put("emailAddress", row[++idx]);
					record.put("recordId", ((Number) row[++idx]).longValue());
					return record;
				}
			)
			.collect(Collectors.toList());
	}

	private Criteria<User> getUsersListQuery(UserListCriteria crit) {
		Criteria<User> query = createCriteria(User.class, "u");

		if (hasRoleRestrictions(crit) || hasResourceRestrictions(crit)) {
			SubQuery<Long> subQuery = query.createSubQuery(User.class, "u")
				.distinct().select("u.id");
			addSearchConditions(subQuery, crit);
			query.add(query.in("u.id", subQuery));
		} else {
			addSearchConditions(query, crit);
		}

		return query;
	}

	private List<String> excludeUsersList(boolean includeSysUser) {
		if (includeSysUser) {
			return Arrays.asList("public_catalog_user", "public_dashboard_user");
		} else {
			return Arrays.asList(User.SYS_USER, "public_catalog_user", "public_dashboard_user");
		}
	}

//	private List<User> executeGetUserByLoginNameHql(String hql, Collection<String> loginNames, String domainName) {
//		return createQuery(hql, User.class)
//			.setParameterList("loginNames", loginNames)
//			.setParameter("domainName", domainName)
//			.list();
//	}

	private List<User> executeGetUserByEmailAddressHql(String hql, Collection<String> emailAddresses) {
		return createQuery(hql, User.class)
			.setParameterList("emailAddresses", emailAddresses)
			.list();
	}
	
	private void addSearchConditions(AbstractCriteria<?, ?> query, UserListCriteria listCrit) {
		addNonSystemUserRestriction(query, listCrit.includeSysUser());

		String searchString = listCrit.query();
		if (StringUtils.isBlank(searchString)) {
			addNameRestriction(query, listCrit.name());
			addLoginNameRestriction(query, listCrit.loginName());
		} else {
			query.add(
				query.or(
					query.ilike("u.firstName", searchString),
					query.ilike("u.lastName",  searchString)
				)
			);
		}

		applyIdsFilter(query, "u.id", listCrit.ids());
		addActivityStatusRestriction(query, listCrit.activityStatus());
		addInstituteRestriction(query, listCrit.instituteName());
		addDomainRestriction(query, listCrit.domainName());
		addTypeRestriction(query, listCrit.type());
		addExcludeTypesRestriction(query, listCrit.excludeTypes());
		addActiveSinceRestriction(query, listCrit.activeSince());
		addRoleRestrictions(query, listCrit);
		addResourceRestrictions(query, listCrit);
		addGroupRestrictions(query, listCrit);
	}

	private void addNonSystemUserRestriction(AbstractCriteria<?, ?> query, boolean includeSysUser) {
		query.join("u.authDomain", "domain")
			.add( // not system user
				query.not(query.conjunction()
					.add(query.in("u.loginName", excludeUsersList(includeSysUser)))
					.add(query.eq("domain.name", User.DEFAULT_AUTH_DOMAIN))
					.getRestriction()
				)
			);
	}

	private void addNameRestriction(AbstractCriteria<?, ?> query, String name) {
		if (StringUtils.isBlank(name)) {
			return;
		}
		
		query.add(
			query.disjunction()
				.add(query.ilike("u.firstName", name))
				.add(query.ilike("u.lastName", name))
		);
	}
	
	private void addLoginNameRestriction(AbstractCriteria<?, ?> query, String loginName) {
		if (StringUtils.isBlank(loginName)) {
			return;
		}
		
		query.add(query.ilike("u.loginName", loginName));
	}
	
	private void addActivityStatusRestriction(AbstractCriteria<?, ?> query, String activityStatus) {
		if (StringUtils.isBlank(activityStatus)) {
			query.add(query.ne("u.activityStatus", Status.ACTIVITY_STATUS_CLOSED.getStatus()));
		} else if (!activityStatus.equalsIgnoreCase("all")) {
			query.add(query.eq("u.activityStatus", activityStatus));
		}
	}

	private void addTypeRestriction(AbstractCriteria<?, ?> query, String type) {
		if (StringUtils.isBlank(type)) {
			return;
		}

		query.add(query.eq("u.type", User.Type.valueOf(type)));
	}

	private void addExcludeTypesRestriction(AbstractCriteria<?, ?> query, List<String> excludeTypes) {
		if (CollectionUtils.isEmpty(excludeTypes)) {
			return;
		}

		List<User.Type> types = excludeTypes.stream().map(User.Type::valueOf).collect(Collectors.toList());
		query.add(query.not(query.in("u.type", types)));
	}
	
	private void addInstituteRestriction(AbstractCriteria<?, ?> query, String instituteName) {
		query.leftJoin("u.institute", "institute");
		if (StringUtils.isBlank(instituteName)) {
			return;
		}
		
		query.add(query.eq("institute.name", instituteName));
	}
	
	private void addDomainRestriction(AbstractCriteria<?, ?> query, String domainName) {
		if (StringUtils.isBlank(domainName)) {
			return;
		}

		query.add(query.eq("domain.name", domainName));
	}

	private void addActiveSinceRestriction(AbstractCriteria<?, ?> query, Date activeSince) {
		if (activeSince == null) {
			return;
		}

		query.add(query.ge("u.creationDate", Utility.chopTime(activeSince)));
	}

	private void addRoleRestrictions(AbstractCriteria<?, ?> query, UserListCriteria listCrit) {
		if (!hasRoleRestrictions(listCrit)) {
			return;
		}


		query.leftJoin("u.roles", "sr");

		Conjunction and = query.conjunction();
		if (CollectionUtils.isNotEmpty(listCrit.roleNames())) {
			query.leftJoin("sr.role", "role");
			and.add(query.in("role.name", listCrit.roleNames()));
		}

		if (StringUtils.isNotBlank(listCrit.siteName())) {
			and.add(addSiteRestriction(query, "sr", listCrit.siteName()));
		}

		if (StringUtils.isNotBlank(listCrit.cpShortTitle())) {
			and.add(addCpRestriction(query, "sr", listCrit.cpShortTitle()));
		}

		query.add(
			query.or(
				query.eq("u.type", User.Type.SUPER),
				query.and(query.isNotNull("sr.id"), and.getRestriction())
			)
		);
	}

	private void addResourceRestrictions(AbstractCriteria<?, ?> query, UserListCriteria listCrit) {
		if (!hasResourceRestrictions(listCrit)) {
			return;
		}

		query.leftJoin("u.acl", "acl");

		Conjunction and = query.conjunction();
		and.add(query.eq("acl.resource", listCrit.resourceName()));

		if (CollectionUtils.isNotEmpty(listCrit.opNames())) {
			and.add(query.in("acl.operation", listCrit.opNames()));
		}

		if (StringUtils.isNotBlank(listCrit.siteName())) {
			and.add(addSiteRestriction(query, "acl", listCrit.siteName()));
		}

		if (StringUtils.isNotBlank(listCrit.cpShortTitle())) {
			and.add(addCpRestriction(query, "acl", listCrit.cpShortTitle()));
		}

		query.add(
			query.or(
				query.eq("u.type", User.Type.SUPER),
				query.and(query.isNotNull("acl.id"), and.getRestriction())
			)
		);
	}

	private void addGroupRestrictions(AbstractCriteria<?, ?> query, UserListCriteria listCrit) {
		if (StringUtils.isBlank(listCrit.group())) {
			return;
		}

		query.join("u.groups", "group")
			.add(query.eq("group.name", listCrit.group()));
	}

	private Restriction addSiteRestriction(AbstractCriteria<?, ?> query, String alias, String siteName) {
		query.leftJoin(alias + ".site", "rs");

		SubQuery<Long> userInstituteSite = query.createSubQuery(Site.class, "uis")
			.join("uis.institute", "uii");

		userInstituteSite.add(userInstituteSite.eq(query.getProperty("institute.id"), userInstituteSite.getProperty("uii.id")))
			.add(userInstituteSite.eq("uis.name", siteName))
			.select("uis.id");

		return query.or(
			query.and(
				query.isNull("rs.id"),
				query.exists(userInstituteSite)
			),
			query.eq("rs.name", siteName)
		);
	}

	private Restriction addCpRestriction(AbstractCriteria<?, ?> query, String alias, String cpShortTitle) {
		query.leftJoin(alias + ".collectionProtocol", "rcp")
			.leftJoin(alias + ".site", "rsite");

		SubQuery<Long> userInstituteCp = query.createSubQuery(CollectionProtocol.class, "uicp")
			.join("uicp.sites", "uicps")
			.join("uicps.site", "uicpss")
			.join("uicpss.institute", "uicpi");

		userInstituteCp
			.add(
				userInstituteCp.or(
					userInstituteCp.and(
						query.isNull("rsite.id"),
						userInstituteCp.eq(query.getProperty("institute.id"), userInstituteCp.getProperty("uicpi.id"))
					),
					userInstituteCp.and(
						query.isNotNull("rsite.id"),
						userInstituteCp.eq(query.getProperty("rsite.id"), userInstituteCp.getProperty("uicpss.id"))
					)
				)
			)
			.add(userInstituteCp.eq("uicp.shortTitle", cpShortTitle))
			.select("uicp.id");

		return query.or(
			query.and(
				query.isNull("rcp.id"),
				query.exists(userInstituteCp)
			),
			query.eq("rcp.shortTitle", cpShortTitle)
		);
	}

	private boolean hasRoleRestrictions(UserListCriteria listCrit) {
		return CollectionUtils.isNotEmpty(listCrit.roleNames()) ||
			(StringUtils.isBlank(listCrit.resourceName()) && (StringUtils.isNotBlank(listCrit.cpShortTitle()) ||
				StringUtils.isNotBlank(listCrit.siteName())));
	}

	private boolean hasResourceRestrictions(UserListCriteria listCrit) {
		return CollectionUtils.isEmpty(listCrit.roleNames()) && StringUtils.isNotBlank(listCrit.resourceName());
	}

	private List<DependentEntityDetail> getDependentEntities(List<Object[]> rows) {
		List<DependentEntityDetail> dependentEntities = new ArrayList<>();
		
		for (Object[] row: rows) {
			String name = (String)row[0];
			int count = ((Number)row[1]).intValue();
			dependentEntities.add(DependentEntityDetail.from(name, count));
		}
		
		return dependentEntities;
 	}

	private List<FormCtxtSummary> getEntityForms(List<Object[]> rows) {
		Map<Long, FormCtxtSummary> formsMap = new LinkedHashMap<>();

		for (Object[] row : rows) {
			Long entityId = (Long)row[4];
			Long formId = (Long)row[1];

			FormCtxtSummary form = formsMap.get(formId);
			if (form != null && entityId == -1) {
				continue;
			}

			form = new FormCtxtSummary();
			form.setFormCtxtId((Long)row[0]);
			form.setFormId(formId);
			form.setFormName((String)row[2]);
			form.setFormCaption((String)row[3]);
			form.setEntityType((String)row[5]);
			form.setCreationTime((Date)row[6]);
			form.setModificationTime((Date)row[7]);

			UserSummary user = new UserSummary();
			user.setId((Long)row[8]);
			user.setFirstName((String)row[9]);
			user.setLastName((String)row[10]);
			form.setCreatedBy(user);

			form.setMultiRecord((Boolean)row[11]);
			form.setSysForm((Boolean)row[12]);
			form.setNoOfRecords((Integer)row[13]);
			formsMap.put(formId, form);
		}

		return new ArrayList<>(formsMap.values());
	}

	private static final String GET_USER_BY_EMAIL_HQL =
		"from " +
		"  com.krishagni.catissueplus.core.administrative.domain.User " +
		"where " +
		"  emailAddress in (:emailAddresses) %s";
	
	private static final String FQN = User.class.getName();

	private static final String GET_DEPENDENT_ENTITIES = FQN + ".getDependentEntities";

	private static final String TOKEN_FQN = ForgotPasswordToken.class.getName();
	
	private static final String GET_FP_TOKEN_BY_USER = TOKEN_FQN + ".getFpTokenByUser";
	
	private static final String GET_FP_TOKEN = TOKEN_FQN + ".getFpToken";

	private static final String GET_ACTIVE_USERS_EMAIL_IDS = FQN + ".getActiveUsersEmailIds";
	
	private static final String GET_PASSWDS_UPDATED_BEFORE = FQN + ".getPasswordsUpdatedBeforeDate";
	
	private static final String GET_INACTIVE_USERS = FQN + ".getInactiveUsers";

	private static final String GET_LATEST_LOGIN_TIMES = FQN + ".getLatestLoginTimes";
	
	private static final String UPDATE_STATUS = FQN + ".updateStatus";

	private static final String GET_EMAIL_ID_STATUSES = FQN + ".getEmailIdStatuses";

	private static final String GET_FORMS = FQN + ".getForms";

	private static final String GET_FORM_RECS = FQN + ".getFormRecords";

	private static final String GET_STATE = UserUiState.class.getName() + ".getState";
}
