package com.krishagni.catissueplus.core.audit.repository.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.query.Query;
import org.hibernate.sql.JoinType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
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
import com.krishagni.catissueplus.core.common.util.LogUtil;

import edu.common.dynamicextensions.ndao.DbSettingsFactory;

public class AuditDaoImpl extends AbstractDao<UserApiCallLog> implements AuditDao {

	@Override
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
	public List<RevisionDetail> getRevisions(String auditTable, Long objectId) {
		String[] parts = auditTable.split(":");

		auditTable = parts[0];
		String idColumn = "IDENTIFIER";
		if (parts.length > 1 && StringUtils.isNotBlank(parts[1])) {
			idColumn = parts[1];
		}

		List<Object[]> rows = getCurrentSession().createSQLQuery(String.format(GET_REV_INFO_SQL, auditTable, idColumn, ""))
			.addScalar("rev", LongType.INSTANCE)
			.addScalar("revTime", TimestampType.INSTANCE)
			.addScalar("userId", LongType.INSTANCE)
			.addScalar("firstName", StringType.INSTANCE)
			.addScalar("lastName", StringType.INSTANCE)
			.addScalar("emailAddr", StringType.INSTANCE)
			.addScalar("loginName", StringType.INSTANCE)
			.setParameter("objectId", objectId)
			.list();

		return rows.stream().map(this::getRevisionInfo).collect(Collectors.toList());
	}

	@Override
	public List<RevisionDetail> getRevisions(RevisionsListCriteria criteria) {
		Criteria query = getCurrentSession().createCriteria(RevisionEntityRecord.class, "re")
			.createAlias("re.revision", "r")
			.createAlias("r.user", "u", JoinType.LEFT_OUTER_JOIN);

		buildRevisionsListQuery(query, criteria);
		setRevisionsListFields(query);

		query.addOrder(Order.desc("re.id"))
			.setFirstResult(criteria.startAt())
			.setMaxResults(criteria.maxResults());

		List<RevisionDetail> revisions = getRevisions((List<Object[]>) query.list());
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
	public List<FormDataRevisionDetail> getFormDataRevisions(RevisionsListCriteria criteria) {
		List<Object[]> rows = buildFormDataRevisionsQuery(criteria).list();

		List<FormDataRevisionDetail> result = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = 0;
			FormDataRevisionDetail detail = new FormDataRevisionDetail();
			detail.setId((Long) row[idx++]);
			detail.setTime((Date) row[idx++]);
			detail.setOp((String) row[idx++]);
			detail.setEntityId((Long) row[idx++]);
			detail.setRecordId((Long) row[idx++]);
			detail.setEntityType((String) row[idx++]);
			detail.setFormName((String) row[idx++]);

			UserSummary user = new UserSummary();
			user.setId((Long) row[idx++]);
			user.setFirstName((String) row[idx++]);
			user.setLastName((String) row[idx++]);
			user.setEmailAddress((String) row[idx++]);
			user.setLoginName((String) row[idx++]);
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

			Integer opType = (Integer) row[++idx];
			detail.setOp(opType == 0 ? "INSERT" : opType == 1 ? "UPDATE" : "DELETE");

			detail.setRecordId((Long) row[++idx]);
			detail.setFormName((String) row[++idx]);

			UserSummary user = new UserSummary();
			user.setId((Long) row[++idx]);
			user.setFirstName((String) row[++idx]);
			user.setLastName((String) row[++idx]);
			user.setEmailAddress((String) row[++idx]);
			user.setLoginName((String) row[++idx]);
			detail.setUser(user);
			result.add(detail);
		}

		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Date getLatestApiCallTime(Long userId, String token) {
		List<Date> result = getCurrentSession().getNamedQuery(GET_LATEST_API_CALL_TIME)
			.setLong("userId", userId)
			.setString("authToken", token)
			.list();

		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	public void saveOrUpdate(DeleteLog log) {
		getCurrentSession().saveOrUpdate(log);
	}


	@SuppressWarnings("unchecked")
	private Object[] getLatestRevisionInfo(String auditTable, Long objectId, int revType) {
		String[] parts = auditTable.split(":");

		auditTable = parts[0];
		String idColumn = "IDENTIFIER";
		if (parts.length > 1 && StringUtils.isNotBlank(parts[1])) {
			idColumn = parts[1];
		}

		String sql = String.format(GET_REV_INFO_SQL, auditTable, idColumn, "and a.revtype = :revType");
		List<Object[]> rows = getCurrentSession().createSQLQuery(sql)
			.addScalar("rev", LongType.INSTANCE)
			.addScalar("revTime", TimestampType.INSTANCE)
			.addScalar("userId", LongType.INSTANCE)
			.addScalar("firstName", StringType.INSTANCE)
			.addScalar("lastName", StringType.INSTANCE)
			.addScalar("emailAddr", StringType.INSTANCE)
			.addScalar("loginName", StringType.INSTANCE)
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

		UserSummary user = new UserSummary();
		user.setId((Long)row[idx++]);
		user.setFirstName((String)row[idx++]);
		user.setLastName((String)row[idx++]);
		user.setEmailAddress((String)row[idx++]);
		user.setLoginName((String)row[idx++]);
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

	@SuppressWarnings("unchecked")
	private Integer getRevisionsCount(String auditTable, Long objectId) {
		List<Integer> result = getCurrentSession().createSQLQuery(String.format(GET_REV_COUNT_SQL, auditTable))
			.addScalar("revisions", IntegerType.INSTANCE)
			.setParameter("objectId", objectId)
			.list();

		return CollectionUtils.isEmpty(result) ? null : result.iterator().next();
	}

	private Criteria buildRevisionsListQuery(Criteria query, RevisionsListCriteria criteria) {
		if (criteria.startDate() != null) {
			query.add(Restrictions.ge("r.revtstmp", criteria.startDate()));
		}

		if (criteria.endDate() != null) {
			query.add(Restrictions.le("r.revtstmp", criteria.endDate()));
		}

		if (CollectionUtils.isNotEmpty(criteria.userIds())) {
			query.add(Restrictions.in("u.id", criteria.userIds()));
		}

		if (criteria.lastId() != null) {
			query.add(Restrictions.lt("re.id", criteria.lastId()));
		}

		if (CollectionUtils.isNotEmpty(criteria.entities())) {
			Junction orCond = Restrictions.disjunction();
			for (String entity : criteria.entities()) {
				orCond.add(Restrictions.like("re.entityName", entity, MatchMode.END));
			}

			query.add(orCond);
		}

		return query;
	}

	private Criteria setRevisionsListFields(Criteria query) {
		query.setProjection(
			Projections.projectionList()
				.add(Projections.property("r.id"))
				.add(Projections.property("r.revtstmp"))
				.add(Projections.property("u.id"))
				.add(Projections.property("u.firstName"))
				.add(Projections.property("u.lastName"))
				.add(Projections.property("u.emailAddress"))
				.add(Projections.property("u.loginName"))
				.add(Projections.property("re.id"))
				.add(Projections.property("re.type"))
				.add(Projections.property("re.entityName"))
				.add(Projections.property("re.entityId"))
		);

		return query;
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

			RevisionEntityRecordDetail entity = getRevisionEntityInfo(row, 7);
			lastRevision.addRecord(entity);
		}

		return revisions;
	}

	private Query buildFormDataRevisionsQuery(RevisionsListCriteria criteria) {
		String sql = String.format(GET_FORM_DATA_AUD_EVENTS_SQL, buildBaseFormDataRevisionsQuery(criteria), buildQueryRestrictions(criteria));
		Query query = getCurrentSession().createSQLQuery(sql)
			.addScalar("identifier", LongType.INSTANCE)
			.addScalar("event_timestamp", TimestampType.INSTANCE)
			.addScalar("event_type", StringType.INSTANCE)
			.addScalar("object_id", LongType.INSTANCE)
			.addScalar("record_id", LongType.INSTANCE)
			.addScalar("entity_type", StringType.INSTANCE)
			.addScalar("caption", StringType.INSTANCE)
			.addScalar("user_id", LongType.INSTANCE)
			.addScalar("first_name", StringType.INSTANCE)
			.addScalar("last_name", StringType.INSTANCE)
			.addScalar("email_address", StringType.INSTANCE)
			.addScalar("login_name", StringType.INSTANCE);

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
		}

		return query;
	}

	private Query buildFormRevisionsQuery(RevisionsListCriteria criteria) {
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

		String result = GET_FORM_REVISIONS_SQL;
		if (!whereClauses.isEmpty()) {
			result += " where " + StringUtils.join(whereClauses, " and ");
		}

		result += " order by r.rev desc ";
		String sql = getLimitSql(result, criteria.startAt(), criteria.maxResults(), DbSettingsFactory.isOracle());
		Query query = getCurrentSession().createSQLQuery(sql)
			.addScalar("rev", LongType.INSTANCE)
			.addScalar("rev_time", TimestampType.INSTANCE)
			.addScalar("rev_type", IntegerType.INSTANCE)
			.addScalar("form_id", LongType.INSTANCE)
			.addScalar("form_name", StringType.INSTANCE)
			.addScalar("user_id", LongType.INSTANCE)
			.addScalar("first_name", StringType.INSTANCE)
			.addScalar("last_name", StringType.INSTANCE)
			.addScalar("email_address", StringType.INSTANCE)
			.addScalar("login_name", StringType.INSTANCE);

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
		if (CollectionUtils.isEmpty(criteria.entities())) {
			return StringUtils.EMPTY;
		}

		return "where fc.entity_type in :entities";
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
		"  r.rev as rev, r.revtstmp as revTime, r.user_id as userId, " +
		"  u.first_name as firstName, u.last_name as lastName, " +
		"  u.email_address as emailAddr, u.login_name as loginName " +
		"from " +
		"  os_revisions r " +
		"  inner join %s a on a.rev = r.rev " +
		"  inner join catissue_user u on u.identifier = r.user_id " +
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
		"  e.identifier, e.event_timestamp, e.user_id, e.event_type, e.record_id, e.form_id " +
		"from " +
		"  dyextn_audit_events e";

	private static final String GET_FORM_DATA_AUD_EVENTS_SQL =
		"select" +
		"  t.identifier, t.event_timestamp, t.event_type, fre.object_id, t.record_id, " +
		"  fc.entity_type, f.caption, " +
		"  t.user_id, u.first_name, u.last_name, u.email_address, u.login_name " +
		"from " +
		"  (%s) t " +
		"  inner join dyextn_containers f on f.identifier = t.form_id " +
		"  inner join catissue_form_context fc on fc.container_id = f.identifier " +
		"  inner join catissue_form_record_entry fre on fre.record_id = t.record_id and fre.form_ctxt_id = fc.identifier " +
		"  inner join catissue_user u on u.identifier = t.user_id " +
		"%s " +
		"order by " +
		" t.identifier desc";

	private static final String GET_FORM_REVISIONS_SQL =
		"select" +
		"  r.rev, r.rev_time, r.rev_type, r.identifier as form_id, r.name as form_name, " +
		"  r.rev_by as user_id, u.first_name, u.last_name, u.email_address, u.login_name " +
		"from " +
		"  dyextn_containers_aud r" +
		"  inner join catissue_user u on u.identifier = r.rev_by ";

	private static final String GET_LATEST_API_CALL_TIME = FQN + ".getLatestApiCallTime";
}
