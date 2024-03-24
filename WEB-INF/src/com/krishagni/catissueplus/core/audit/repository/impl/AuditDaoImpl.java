package com.krishagni.catissueplus.core.audit.repository.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import com.krishagni.catissueplus.core.audit.domain.DeleteLog;
import com.krishagni.catissueplus.core.audit.domain.RevisionEntityRecord;
import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.events.AuditDetail;
import com.krishagni.catissueplus.core.audit.events.FormDataRevisionDetail;
import com.krishagni.catissueplus.core.audit.events.RevisionDetail;
import com.krishagni.catissueplus.core.audit.events.RevisionEntityRecordDetail;
import com.krishagni.catissueplus.core.audit.repository.AuditDao;
import com.krishagni.catissueplus.core.audit.repository.RevisionsListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Junction;
import com.krishagni.catissueplus.core.common.repository.Query;
import com.krishagni.catissueplus.core.common.util.LogUtil;

import edu.common.dynamicextensions.ndao.DbSettingsFactory;

public class AuditDaoImpl extends AbstractDao<UserApiCallLog> implements AuditDao {

	@Override
	public AuditDetail getAuditDetail(String auditTable, Long objectId) {
		RevisionDetail createRev = getRevisionInfo(getLatestRevisionInfo(auditTable, objectId, 0));
		RevisionDetail updateRev = getRevisionInfo(getLatestRevisionInfo(auditTable, objectId, 1));

		AuditDetail result = new AuditDetail();
		result.setCreatedOn(createRev.getChangedOn());
		result.setCreatedBy(createRev.getChangedBy());
		result.setLastUpdatedOn(updateRev.getChangedOn());
		result.setLastUpdatedBy(updateRev.getChangedBy());

		if (result.getLastUpdatedOn() != null) {
			result.setRevisionsCount(getRevisionsCount(auditTable, objectId));
		} else {
			result.setRevisionsCount(1);
		}

		return result;
	}

	@Override
	public List<RevisionDetail> getRevisions(String auditTable, Long objectId) {
		String[] parts = auditTable.split(":");

		auditTable = parts[0];
		String idColumn = "IDENTIFIER";
		if (parts.length > 1 && StringUtils.isNotBlank(parts[1])) {
			idColumn = parts[1];
		}

		List<Object[]> rows = createNativeQuery(String.format(GET_REV_INFO_SQL, auditTable, idColumn, ""), Object[].class)
			.addLongScalar("rev")
			.addTimestampScalar("revTime")
			.addStringScalar("ipAddress")
			.addLongScalar("userId")
			.addStringScalar("firstName")
			.addStringScalar("lastName")
			.addStringScalar("emailAddr")
			.addStringScalar("loginName")
			.addStringScalar("instituteName")
			.addStringScalar("domainName")
			.setParameter("objectId", objectId)
			.list();

		return rows.stream().map(this::getRevisionInfo).collect(Collectors.toList());
	}

	@Override
	public List<RevisionDetail> getRevisions(RevisionsListCriteria criteria) {
		Criteria<Object[]> query = createCriteria(RevisionEntityRecord.class, Object[].class, "re");
		query.join("re.revision", "r")
			.leftJoin("r.user", "u")
			.leftJoin("u.institute", "i")
			.leftJoin("u.authDomain", "d");

		buildRevisionsListQuery(query, criteria);
		setRevisionsListFields(query);

		query.addOrder(query.desc("re.id"));
		List<RevisionDetail> revisions = getRevisions(query.list(criteria.startAt(), criteria.maxResults()));
		for (RevisionDetail revision : revisions) {
			Map<String, List<RevisionEntityRecordDetail>> entitiesMap = new LinkedHashMap<>();
			for (RevisionEntityRecordDetail entity : revision.getRecords()) {
				List<RevisionEntityRecordDetail> entities = entitiesMap.computeIfAbsent(entity.getEntityName(), (k) -> new ArrayList<>());
				entities.add(entity);
			}

			if (criteria.includeModifiedProps()) {
				loadModifiedProps(revision, entitiesMap);
			}
		}

		return revisions;
	}

	@Override
	public List<String> getRevisionEntityNames() {
		return (List<String>) getCurrentSession().getNamedQuery(GET_ENTITY_NAMES).list();
	}

	@Override
	public List<FormDataRevisionDetail> getFormDataRevisions(RevisionsListCriteria criteria) {
		List<Object[]> rows = buildFormDataRevisionsQuery(criteria).list();

		List<FormDataRevisionDetail> result = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = 0;
			FormDataRevisionDetail detail = new FormDataRevisionDetail();
			detail.setId((Long) row[idx++]);
			detail.setTime((Date) row[idx++]);
			detail.setOp((String) row[idx++]);
			detail.setIpAddress((String) row[idx++]);
			detail.setEntityId((Long) row[idx++]);
			detail.setRecordId((Long) row[idx++]);
			detail.setData((String) row[idx++]);
			detail.setEntityType((String) row[idx++]);
			detail.setFormName((String) row[idx++]);

			UserSummary user = new UserSummary();
			user.setId((Long) row[idx++]);
			user.setFirstName((String) row[idx++]);
			user.setLastName((String) row[idx++]);
			user.setEmailAddress((String) row[idx++]);
			user.setLoginName((String) row[idx++]);
			user.setInstituteName((String) row[idx++]);
			user.setDomain((String) row[idx++]);
			detail.setUser(user);

			result.add(detail);
		}

		return result;
	}

	@Override
	public List<FormDataRevisionDetail> getFormRevisions(RevisionsListCriteria criteria) {
		List<Object[]> rows = buildFormRevisionsQuery(criteria).list();

		List<FormDataRevisionDetail> result = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = -1;
			FormDataRevisionDetail detail = new FormDataRevisionDetail();
			detail.setId((Long) row[++idx]);
			detail.setTime((Date) row[++idx]);
			detail.setIpAddress((String) row[++idx]);

			Integer opType = (Integer) row[++idx];
			detail.setOp(opType == 0 ? "INSERT" : opType == 1 ? "UPDATE" : "DELETE");

			detail.setRecordId((Long) row[++idx]);
			detail.setFormName((String) row[++idx]);
			detail.setData((String) row[++idx]);

			UserSummary user = new UserSummary();
			user.setId((Long) row[++idx]);
			user.setFirstName((String) row[++idx]);
			user.setLastName((String) row[++idx]);
			user.setEmailAddress((String) row[++idx]);
			user.setLoginName((String) row[++idx]);
			user.setInstituteName((String) row[++idx]);
			user.setDomain((String) row[++idx]);
			detail.setUser(user);
			result.add(detail);
		}

		return result;
	}

	@Override
	public Date getLatestApiCallTime(Long userId, String token) {
		List<Date> result = createNamedQuery(GET_LATEST_API_CALL_TIME, Date.class)
			.setParameter("userId", userId)
			.setParameter("authToken", token)
			.list();
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	public void saveOrUpdate(DeleteLog log) {
		getCurrentSession().saveOrUpdate(log);
	}

	@Override
	public List<UserApiCallLog> getApiCallLogs(RevisionsListCriteria crit) {
		Criteria<UserApiCallLog> query = createCriteria(UserApiCallLog.class, "log");
		query.join("log.user", "user")
			.join("log.loginAuditLog", "al")
			.addOrder(query.desc("log.id"));

		if (crit.lastId() != null) {
			query.add(query.lt("log.id", crit.lastId()));
		}

		if (crit.startDate() != null) {
			query.add(query.ge("log.callStartTime", crit.startDate()));
		}

		if (crit.endDate() != null) {
			query.add(query.le("log.callStartTime", crit.endDate()));
		}

		if (crit.userIds() != null && !crit.userIds().isEmpty()) {
			query.add(query.in("user.id", crit.userIds()));
		}

		return query.list(crit.startAt(), crit.maxResults());
	}

	private Object[] getLatestRevisionInfo(String auditTable, Long objectId, int revType) {
		String[] parts = auditTable.split(":");

		auditTable = parts[0];
		String idColumn = "IDENTIFIER";
		if (parts.length > 1 && StringUtils.isNotBlank(parts[1])) {
			idColumn = parts[1];
		}

		String sql = String.format(GET_REV_INFO_SQL, auditTable, idColumn, "and a.revtype = :revType");
		List<Object[]> rows = createNativeQuery(sql, Object[].class)
			.addLongScalar("rev")
			.addTimestampScalar("revTime")
			.addStringScalar("ipAddress")
			.addLongScalar("userId")
			.addStringScalar("firstName")
			.addStringScalar("lastName")
			.addStringScalar("emailAddr")
			.addStringScalar("loginName")
			.addStringScalar("instituteName")
			.addStringScalar("domainName")
			.setParameter("objectId", objectId)
			.setParameter("revType", revType)
			.setMaxResults(1)
			.list();
		return CollectionUtils.isEmpty(rows) ? null : rows.iterator().next();
	}

	private RevisionDetail getRevisionInfo(Object[] row) {
		RevisionDetail detail = new RevisionDetail();
		if (row == null) {
			return detail;
		}

		int idx = 0;
		detail.setRevisionId((Long) row[idx++]);
		detail.setChangedOn((Date) row[idx++]);
		detail.setIpAddress((String) row[idx++]);

		UserSummary user = new UserSummary();
		user.setId((Long)row[idx++]);
		user.setFirstName((String)row[idx++]);
		user.setLastName((String)row[idx++]);
		user.setEmailAddress((String)row[idx++]);
		user.setLoginName((String)row[idx++]);
		user.setInstituteName((String)row[idx++]);
		user.setDomain((String)row[idx++]);
		detail.setChangedBy(user);
		return detail;
	}

	private RevisionEntityRecordDetail getRevisionEntityInfo(Object[] row, int startIdx) {
		RevisionEntityRecordDetail detail = new RevisionEntityRecordDetail();
		detail.setId((Long) row[startIdx++]);
		detail.setType((Integer) row[startIdx++]);
		detail.setEntityName((String) row[startIdx++]);
		detail.setEntityId((Long) row[startIdx++]);
		return detail;
	}

	private Integer getRevisionsCount(String auditTable, Long objectId) {
		List<Integer> result = createNativeQuery(String.format(GET_REV_COUNT_SQL, auditTable), Integer.class)
			.addIntScalar("revisions")
			.setParameter("objectId", objectId)
			.list();
		return CollectionUtils.isEmpty(result) ? null : result.iterator().next();
	}

	private void buildRevisionsListQuery(Criteria<Object[]> query, RevisionsListCriteria criteria) {
		if (criteria.startDate() != null) {
			query.add(query.ge("r.revtstmp", criteria.startDate()));
		}

		if (criteria.endDate() != null) {
			query.add(query.le("r.revtstmp", criteria.endDate()));
		}

		if (CollectionUtils.isNotEmpty(criteria.userIds())) {
			query.add(query.in("u.id", criteria.userIds()));
		}

		if (criteria.lastId() != null) {
			query.add(query.lt("re.id", criteria.lastId()));
		}

		if (CollectionUtils.isNotEmpty(criteria.entities())) {
			Junction orCond = query.disjunction();
			for (String entity : criteria.entities()) {
				orCond.add(query.like("re.entityName", "%." + entity, false));
			}

			query.add(orCond);
		} else if (CollectionUtils.isNotEmpty(criteria.records())) {
			query.add(query.in("re.entityName", criteria.records()));
		}

		if (CollectionUtils.isNotEmpty(criteria.recordIds())) {
			query.add(query.in("re.entityId", criteria.recordIds()));
		}
	}

	private void setRevisionsListFields(Criteria<Object[]> query) {
		query.select("r.id", "r.revtstmp", "r.ipAddress",
			"u.id", "u.firstName", "u.lastName", "u.emailAddress", "u.loginName",
			"i.name", "d.name", "re.id", "re.type", "re.entityName", "re.entityId"
		);
	}

	private List<RevisionDetail> getRevisions(List<Object[]> rows) {
		RevisionDetail lastRevision = null;
		List<RevisionDetail> revisions = new ArrayList<>();

		for (Object[] row : rows) {
			Long revisionId = (Long) row[0];
			if (lastRevision == null || !lastRevision.getRevisionId().equals(revisionId)) {
				lastRevision = getRevisionInfo(row);
				lastRevision.setRecords(new ArrayList<>());
				revisions.add(lastRevision);
			}

			RevisionEntityRecordDetail entity = getRevisionEntityInfo(row, 10);
			lastRevision.addRecord(entity);
		}

		return revisions;
	}

	private Query<Object[]> buildFormDataRevisionsQuery(RevisionsListCriteria criteria) {
		String sql = String.format(GET_FORM_DATA_AUD_EVENTS_SQL, buildBaseFormDataRevisionsQuery(criteria), buildQueryRestrictions(criteria));
		Query<Object[]> query = createNativeQuery(sql, Object[].class)
			.addLongScalar("identifier")
			.addTimestampScalar("event_timestamp")
			.addStringScalar("event_type")
			.addStringScalar("ip_address")
			.addLongScalar("object_id")
			.addLongScalar("record_id")
			.addStringScalar("form_data")
			.addStringScalar("entity_type")
			.addStringScalar("caption")
			.addLongScalar("user_id")
			.addStringScalar("first_name")
			.addStringScalar("last_name")
			.addStringScalar("email_address")
			.addStringScalar("login_name")
			.addStringScalar("name")
			.addStringScalar("domain_name");

		if (CollectionUtils.isNotEmpty(criteria.userIds())) {
			query.setParameterList("userIds", criteria.userIds());
		}

		if (criteria.startDate() != null) {
			query.setParameter("startDate", criteria.startDate());
		}

		if (criteria.endDate() != null) {
			query.setParameter("endDate", criteria.endDate());
		}

		if (criteria.lastId() != null) {
			query.setParameter("lastId", criteria.lastId());
		}

		if (CollectionUtils.isNotEmpty(criteria.entities())) {
			query.setParameterList("entities", criteria.entities());
		} else if (CollectionUtils.isNotEmpty(criteria.records())) {
			query.setParameterList("formNames", criteria.records());
		}

		return query;
	}

	private Query<Object[]> buildFormRevisionsQuery(RevisionsListCriteria criteria) {
		List<String> whereClauses = new ArrayList<>();

		if (CollectionUtils.isNotEmpty(criteria.userIds())) {
			whereClauses.add("u.identifier in (:userIds)");
		}

		if (criteria.startDate() != null) {
			whereClauses.add("r.rev_time >= :startDate");
		}

		if (criteria.endDate() != null) {
			whereClauses.add("r.rev_time <= :endDate");
		}

		if (criteria.lastId() != null) {
			whereClauses.add("r.rev < :lastId");
		}

		if (CollectionUtils.isNotEmpty(criteria.entities())) {
			whereClauses.add("r.identifier in (" + GET_ENTITY_FORMS_SQL + ")");
		} else if (CollectionUtils.isNotEmpty(criteria.records())) {
			whereClauses.add("r.identifier in (" + GET_FORM_IDS_SQL + ")");
		}

		String result = GET_FORM_REVISIONS_SQL;
		if (!whereClauses.isEmpty()) {
			result += " where " + StringUtils.join(whereClauses, " and ");
		}

		result += " order by r.rev desc ";
		String sql = getLimitSql(result, criteria.startAt(), criteria.maxResults(), DbSettingsFactory.isOracle());
		Query<Object[]> query = createNativeQuery(sql, Object[].class)
			.addLongScalar("rev")
			.addTimestampScalar("rev_time")
			.addStringScalar("ip_address")
			.addIntScalar("rev_type")
			.addLongScalar("form_id")
			.addStringScalar("form_name")
			.addStringScalar("change_log")
			.addLongScalar("user_id")
			.addStringScalar("first_name")
			.addStringScalar("last_name")
			.addStringScalar("email_address")
			.addStringScalar("login_name")
			.addStringScalar("name")
			.addStringScalar("domain_name");

		if (CollectionUtils.isNotEmpty(criteria.userIds())) {
			query.setParameterList("userIds", criteria.userIds());
		}

		if (criteria.startDate() != null) {
			query.setParameter("startDate", criteria.startDate());
		}

		if (criteria.endDate() != null) {
			query.setParameter("endDate", criteria.endDate());
		}

		if (criteria.lastId() != null) {
			query.setParameter("lastId", criteria.lastId());
		}

		if (CollectionUtils.isNotEmpty(criteria.entities())) {
			query.setParameterList("entities", criteria.entities());
		} else if (CollectionUtils.isNotEmpty(criteria.records())) {
			query.setParameterList("formNames", criteria.records());
		}

		return query;
	}

	private String buildBaseFormDataRevisionsQuery(RevisionsListCriteria criteria) {
		List<String> whereClauses = new ArrayList<>();

		if (CollectionUtils.isNotEmpty(criteria.userIds())) {
			whereClauses.add("e.user_id in (:userIds)");
		}

		if (criteria.startDate() != null) {
			whereClauses.add("e.event_timestamp >= :startDate");
		}

		if (criteria.endDate() != null) {
			whereClauses.add("e.event_timestamp <= :endDate");
		}

		if (criteria.lastId() != null) {
			whereClauses.add("e.identifier < :lastId");
		}

		String result = GET_FORM_DATA_AUD_EVENTS_BASE_SQL;
		if (!whereClauses.isEmpty()) {
			result += " where " + StringUtils.join(whereClauses, " and ");
		}

		result += " order by e.identifier desc ";
		return getLimitSql(result, criteria.startAt(), criteria.maxResults(), DbSettingsFactory.isOracle());
	}

	private String buildQueryRestrictions(RevisionsListCriteria criteria) {
		if (CollectionUtils.isNotEmpty(criteria.entities())) {
			return "where fc.entity_type in :entities";
		} else if (CollectionUtils.isNotEmpty(criteria.records())) {
			return "where f.name in :formNames";
		} else {
			return StringUtils.EMPTY;
		}
	}

	private void loadModifiedProps(RevisionDetail revision, Map<String, List<RevisionEntityRecordDetail>> entitiesMap) {
		for (Map.Entry<String, List<RevisionEntityRecordDetail>> entity : entitiesMap.entrySet()) {
			try {
				Class<?> klass = Class.forName(entity.getKey());
				if (!BaseEntity.class.isAssignableFrom(klass)) {
					continue;
				}

				boolean anyModified = loadAddedEntityProps(revision, klass, entity.getValue());
				if (anyModified) {
					loadModifiedEntityProps(revision, klass, entity.getValue());
				}
			} catch (Exception e) {
				String msg = "Error: Couldn't load modified properties of: " + entity.getKey() + ": " + e.getMessage();
				entity.getValue().forEach(entityAudit -> entityAudit.setModifiedProps(msg));
				logger.error("Unknown audit entity class: " + entity.getKey(), e);
			}
		}
	}

	private boolean loadAddedEntityProps(RevisionDetail revision, Class<?> entityClass, List<RevisionEntityRecordDetail> entities) {
		boolean anyModified = false;

		AuditReader reader = AuditReaderFactory.get(getCurrentSession());
		for (RevisionEntityRecordDetail entity : entities) {
			if (entity.getType() != 0) {
				anyModified = true;
				continue;
			}

			long t1 = System.currentTimeMillis();
			BaseEntity record = (BaseEntity) reader.find(entityClass, entity.getEntityId(), revision.getRevisionId());
			long t2 = System.currentTimeMillis();

			entity.setModifiedProps(record.toAuditString());
			long t3 = System.currentTimeMillis();

			if (logger.isDebugEnabled()) {
				logger.debug("ADD: " + entityClass.getName() + ", retrieve time: " + (t2 - t1) + " ms, stringify time: " + (t3 - t2) + " ms");
			}
		}

		return anyModified;
	}

	private void loadModifiedEntityProps(RevisionDetail revision, Class<?> entityClass, List<RevisionEntityRecordDetail> entities) {
		long t1 = System.currentTimeMillis();
		List<Object[]> rows  = AuditReaderFactory.get(getCurrentSession())
			.createQuery().forRevisionsOfEntityWithChanges(entityClass, true)
			.add(AuditEntity.revisionNumber().eq(revision.getRevisionId()))
			.getResultList();
		long t2 = System.currentTimeMillis();

		if (rows.isEmpty()) {
			return;
		}

		Map<Long, RevisionEntityRecordDetail> entitiesMap = entities.stream()
			.collect(Collectors.toMap(RevisionEntityRecordDetail::getEntityId, e -> e));
		for (Object[] row : rows) {
			Object record = row[0];
			if (!(record instanceof BaseEntity)) {
				continue;
			}

			BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(record);
			Long id = (Long) bean.getPropertyValue("id");
			RevisionEntityRecordDetail entityAudit = entitiesMap.get(id);
			if (entityAudit != null) {
				Set<String> changedProps = (Set<String>) row[3];
				entityAudit.setModifiedProps(((BaseEntity) record).toAuditString(changedProps));
			}
		}

		long t3 = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			logger.debug("MOD: " + entityClass.getName() + ", retrieve time: " + (t2 - t1) + " ms, stringify time: " + (t3 - t2) + " ms");
		}

	}

	private static final LogUtil logger = LogUtil.getLogger(AuditDaoImpl.class);

	private static final String FQN = UserApiCallLog.class.getName();

	private static final String GET_REV_INFO_SQL =
		"select " +
		"  r.rev as rev, r.revtstmp as revTime, r.ip_address as ipAddress, r.user_id as userId, " +
		"  u.first_name as firstName, u.last_name as lastName, " +
		"  u.email_address as emailAddr, u.login_name as loginName, " +
		"  i.name as instituteName, d.domain_name as domainName " +
		"from " +
		"  os_revisions r " +
		"  inner join %s a on a.rev = r.rev " +
		"  inner join catissue_user u on u.identifier = r.user_id " +
		"  left join catissue_institution i on i.identifier = u.institute_id " +
		"  left join os_auth_domains d on d.identifier = u.domain_id " +
		"where " +
		"  a.%s = :objectId " +
		"  %s " +	// for additional constraints if any
		"order by " +
		"  r.revtstmp desc";

	private static final String GET_REV_COUNT_SQL =
		"select " +
		"  count(t.rev) as revisions " +
		"from " +
		"  %s t " +
		"where " +
		"  t.identifier = :objectId";

	private static final String GET_FORM_DATA_AUD_EVENTS_BASE_SQL =
		"select" +
		"  e.identifier, e.event_timestamp, e.ip_address, e.user_id, e.event_type, e.record_id, e.form_id, e.form_data " +
		"from " +
		"  dyextn_audit_events e";

	private static final String GET_FORM_DATA_AUD_EVENTS_SQL =
		"select" +
		"  t.identifier, t.event_timestamp, t.event_type, t.ip_address, fre.object_id, t.record_id, " +
		"  t.form_data, fc.entity_type, f.caption, " +
		"  t.user_id, u.first_name, u.last_name, u.email_address, u.login_name, i.name, d.domain_name " +
		"from " +
		"  (%s) t " +
		"  inner join dyextn_containers f on f.identifier = t.form_id " +
		"  inner join catissue_form_context fc on fc.container_id = f.identifier " +
		"  inner join catissue_form_record_entry fre on fre.record_id = t.record_id and fre.form_ctxt_id = fc.identifier " +
		"  inner join catissue_user u on u.identifier = t.user_id " +
		"  inner join catissue_institution i on i.identifier = u.institute_id " +
		"  inner join os_auth_domains d on d.identifier = u.domain_id " +
		"%s " +
		"order by " +
		" t.identifier desc";

	private static final String GET_FORM_REVISIONS_SQL =
		"select" +
		"  r.rev, r.rev_time, r.ip_address, r.rev_type, r.identifier as form_id, r.name as form_name, r.modified_props as change_log, " +
		"  r.rev_by as user_id, u.first_name, u.last_name, u.email_address, u.login_name, " +
		"  i.name, d.domain_name " +
		"from " +
		"  dyextn_containers_aud r" +
		"  inner join catissue_user u on u.identifier = r.rev_by " +
		"  inner join catissue_institution i on i.identifier = u.institute_id " +
		"  inner join os_auth_domains d on d.identifier = u.domain_id";

	private static final String GET_ENTITY_FORMS_SQL =
		"select" +
		"  fc.container_id " +
		"from " +
		"  catissue_form_context fc " +
		"where " +
		"  fc.entity_type in :entities and " +
		"  fc.deleted_on is null";

	private static final String GET_FORM_IDS_SQL = "select f.identifier from dyextn_containers f where f.name in :formNames";

	private static final String GET_LATEST_API_CALL_TIME = FQN + ".getLatestApiCallTime";

	private static final String GET_ENTITY_NAMES = "com.krishagni.catissueplus.core.audit.domain.RevisionEntityRecord.getEntityNames";
}
