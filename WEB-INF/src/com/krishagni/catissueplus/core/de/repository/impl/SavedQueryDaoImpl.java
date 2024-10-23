package com.krishagni.catissueplus.core.de.repository.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Conjunction;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.events.ListSavedQueriesCriteria;
import com.krishagni.catissueplus.core.de.events.SavedQuerySummary;
import com.krishagni.catissueplus.core.de.repository.SavedQueryDao;

public class SavedQueryDaoImpl extends AbstractDao<SavedQuery> implements SavedQueryDao {

	private static final String FQN = SavedQuery.class.getName();
	
	private static final String GET_QUERIES_BY_ID = FQN + ".getQueriesByIds";
		
	@Override
	public Long getQueriesCount(ListSavedQueriesCriteria crit) {
		Criteria<Long> query = getSavedQueriesListQuery(crit, Long.class);
		return query.select(query.distinctCount("s.id")).uniqueResult();
	}

	@Override
	public List<SavedQuerySummary> getQueries(ListSavedQueriesCriteria crit) {
		Criteria<Object[]> query = getSavedQueriesListQuery(crit, Object[].class)
			.leftJoin("s.lastUpdatedBy", "m");
		addProjectionFields(query.addOrder(query.desc("s.id")));
		return getSavedQueries(query, crit.startAt(), crit.maxResults());
	}

	@Override
	public List<SavedQuerySummary> getQueries(Long userId, int startAt, int maxRecords, String ... searchString) {
		return getQueries(new ListSavedQueriesCriteria()
			.userId(userId)
			.query(searchString != null && searchString.length > 0 ? searchString[0] : null)
			.startAt(startAt)
			.maxResults(maxRecords));
	}

	@Override
	public SavedQuery getQuery(Long queryId) {
		Criteria<SavedQuery> query = createCriteria(SavedQuery.class, "s");
		List<SavedQuery> queries = query.add(query.eq("s.id", queryId))
			.add(query.isNull("s.deletedOn"))
			.list();
		return CollectionUtils.isEmpty(queries) ? null : queries.iterator().next();
	}

	@Override
	public List<SavedQuery> getQueriesByIds(List<Long> queries) {
		return createNamedQuery(GET_QUERIES_BY_ID, SavedQuery.class)
			.setParameterList("queryIds", queries)
			.list();
	}
	
	@Override
	public Long getQueriesCountByFolderId(Long folderId, String searchString) {
		Criteria<Long> query = createCriteria(SavedQuery.class, Long.class, "s");
		query.join("s.folders", "f")
			.add(query.isNull("s.deletedOn"))
			.add(query.eq("f.id", folderId))
			.select(query.distinctCount("s.id"));
		
		addSearchConditions(query, searchString);
		addActiveCpGroupCond(query);
		return query.uniqueResult();
	}

	@Override
	public List<SavedQuerySummary> getQueriesByFolderId(Long folderId, int startAt, int maxRecords, String searchString) {
		Criteria<Object[]> query = createCriteria(SavedQuery.class, Object[].class, "s");
		query.join("s.createdBy", "c")
			.join("s.folders", "f")
			.leftJoin("s.lastUpdatedBy", "m")
			.add(query.isNull("s.deletedOn"))
			.add(query.eq("f.id", folderId));

		addSearchConditions(query, searchString);
		addProjectionFields(query);
		query.addOrder(query.desc("s.id"));
		return getSavedQueries(query, startAt, maxRecords);
	}

	@Override
	public boolean isQuerySharedWithUser(Long queryId, Long userId, boolean forEdits) {
		Criteria<Long> query = createCriteria(SavedQuery.class, Long.class, "s");
		query.join("s.createdBy", "c")
			.join("s.folders", "f")
			.leftJoin("f.sharedWith", "su")
			.leftJoin("f.sharedWithGroups", "sg")
			.leftJoin("sg.users", "gu")
			.add(query.eq("s.id", queryId))
			.add(query.isNull("s.deletedOn"))
			.add(query.or(
				query.eq("f.sharedWithAll", true),
				query.eq("su.id", userId),
				query.eq("gu.id", userId)
			));

		if (forEdits) {
			query.add(query.eq("f.allowEditsBySharedUsers", true));
		}

		return query.select(query.count("s.id")).uniqueResult() > 0;
	}

	@Override
	public Map<String, Object> getQueryChangeLogDetails(String fileName) {
		List<Object[]> rows = createNamedQuery(GET_QUERY_ID_AND_MD5_SQL, Object[].class)
			.setParameter("fileName", fileName)
			.list();
		if (CollectionUtils.isEmpty(rows)) {
			return null;
		}
		
		Map<String, Object> result = new HashMap<>();
		Object[] row = rows.iterator().next();
		result.put("queryId", row[0]);
		result.put("md5Digest", row[1]);
		return result;
	}
	
	@Override
	public void insertQueryChangeLog(String fileName, String digest, String status, Long queryId) {
		createNamedQuery(INSERT_QUERY_CHANGE_LOG_SQL)
			.setParameter("fileName", fileName)
			.setParameter("md5Digest", digest)
			.setParameter("status", status)
			.setParameter("queryId", queryId)
			.setParameter("executedOn", Calendar.getInstance().getTime())
			.executeUpdate();
	}
	
	private SavedQuerySummary getSavedQuerySummary(Object[] row) {
		SavedQuerySummary savedQuery = new SavedQuerySummary();
		savedQuery.setId((Long)row[0]);
		savedQuery.setTitle((String)row[1]);
		
		UserSummary createdBy = new UserSummary();
		createdBy.setId((Long)row[2]);
		createdBy.setFirstName((String)row[3]);
		createdBy.setLastName((String)row[4]);
		savedQuery.setCreatedBy(createdBy);
		
		UserSummary modifiedBy = new UserSummary();
		modifiedBy.setId((Long)row[5]);
		modifiedBy.setFirstName((String)row[6]);
		modifiedBy.setLastName((String)row[7]);
		savedQuery.setLastModifiedBy(modifiedBy);
		
		savedQuery.setLastModifiedOn((Date)row[8]);
		return savedQuery;		
	}
		
	private void addProjectionFields(Criteria<Object[]> query) {
		query.distinct().select(
			"s.id", "s.title", "c.id", "c.firstName", "c.lastName",
			"m.id", "m.firstName", "m.lastName", "s.lastUpdated"
		);
	}

	private List<SavedQuerySummary> getSavedQueries(Criteria<Object[]> query, int startAt, int maxRecords) {
		List<Object[]> rows = query.list(Math.max(startAt, 0), maxRecords);
		return rows.stream().map(this::getSavedQuerySummary).collect(Collectors.toList());
	}

	private <T> Criteria<T> getSavedQueriesListQuery(ListSavedQueriesCriteria crit, Class<T> returnType) {
		Criteria<T> query = createCriteria(SavedQuery.class, returnType, "s");
		query.join("s.createdBy", "c")
			.leftJoin("c.institute", "i") // system users do not have institute
			.leftJoin("s.folders", "f")
			.leftJoin("f.sharedWith", "su")
			.leftJoin("f.sharedWithGroups", "sg")
			.leftJoin("sg.users", "gu")
			.add(query.isNull("s.deletedOn"));

		if (crit.userId() != null || crit.instituteId() != null) {
			Conjunction creatorCond = query.conjunction();
			if (crit.userId() != null) {
				creatorCond.add(query.eq("c.id", crit.userId()));
			} else {
				creatorCond.add(query.eq("i.id", crit.instituteId()));
			}

			query.add(
				query.disjunction()
					.add(query.eq("f.sharedWithAll", true))
					.add(creatorCond)
					.add(query.eq("su.id", crit.userId()))
					.add(query.eq("gu.id", crit.userId()))
			);
		}

		if (crit.folderId() != null && crit.folderId() >= 0L) {
			query.add(query.eq("f.id", crit.folderId()));
		}

		addCpCondition(query, crit.cpId());
		addSearchConditions(query, crit.query());

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			query.add(query.in("s.id", crit.ids()));
		}

		if (CollectionUtils.isNotEmpty(crit.notInIds())) {
			query.add(query.not(query.in("s.id", crit.notInIds())));
		}

		addActiveCpGroupCond(query);
		return query;
	}

	private Criteria<?> addCpCondition(Criteria<?> query, Long cpId) {
		if (cpId == null) {
			return query;
		}

		return query.add(
			query.or(
				query.isNull("s.cpId"),
				query.eq("s.cpId", cpId)
			)
		);
	}

	private Criteria<?> addSearchConditions(Criteria<?> query, String searchTerm) {
		if (StringUtils.isBlank(searchTerm)) {
			return query;
		}

		Disjunction srchCond = query.disjunction();
		if (StringUtils.isNumeric(searchTerm)) {
			srchCond.add(query.eq("s.id", Long.parseLong(searchTerm)));
		}

		srchCond.add(query.ilike("s.title", searchTerm));
		return query.add(srchCond);
	}

	private Criteria<?> addActiveCpGroupCond(Criteria<?> query) {
		return query.leftJoin("s.cp", "cp")
			.leftJoin("s.cpGroup", "cpGroup")
			.add(query.or(query.isNull("cp.id"), query.ne("cp.activityStatus", "Disabled")))
			.add(query.or(query.isNull("cpGroup.id"), query.ne("cpGroup.activityStatus", "Disabled")));
	}

	private static final String INSERT_QUERY_CHANGE_LOG_SQL = FQN + ".insertQueryChangeLog"; 
	
	private static final String GET_QUERY_ID_AND_MD5_SQL = FQN + ".getQueryIdAndDigest"; 
}