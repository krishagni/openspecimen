package com.krishagni.catissueplus.core.upgrade;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class InitializeSavedQueryCpId implements CustomTaskChange {
	@Override
	public String getConfirmationMessage() {
		return "All saved queries CP ID initialized!";
	}

	@Override
	public void setUp() throws SetupException {

	}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {

	}

	@Override
	public ValidationErrors validate(Database database) {
		return null;
	}

	@Override
	public void execute(Database database)
	throws CustomChangeException {
		try {
			DatabaseConnection conn = database.getConnection();
			boolean isOracle = conn.getDatabaseProductName().toLowerCase().contains("oracle");

			Set<Long> cpIds = getCpIds((JdbcConnection) conn);

			int startAt = 0, maxResults = 100;
			boolean endOfQueries = false;
			while (!endOfQueries) {
				List<Pair<Long, Long>> queryCpIds = getQueryCpIds((JdbcConnection) conn, startAt, maxResults, isOracle);
				endOfQueries = (queryCpIds.size() < maxResults);
				startAt += maxResults;

				updateQueryCpIds((JdbcConnection) conn, cpIds, queryCpIds);
			}
		} catch (Exception e) {
			throw new RuntimeException("Error running the migration", e);
		}
	}

	private Set<Long> getCpIds(JdbcConnection conn)
	throws Exception {
		try (
			PreparedStatement stmt = conn.prepareStatement(GET_CP_IDS_SQL);
			ResultSet rs = stmt.executeQuery()
		) {
			Set<Long> cpIds = new HashSet<>();
			while (rs.next()) {
				cpIds.add(rs.getLong(1));
			}

			return cpIds;
		}
	}

	private List<Pair<Long, Long>> getQueryCpIds(JdbcConnection conn, int startAt, int maxResults, boolean isOracle)
	throws Exception {
		String sql;
		if (isOracle) {
			sql = String.format(GET_QUERIES_ORA, startAt + maxResults, startAt + 1);
		} else {
			sql = String.format(GET_QUERIES_MYSQL, startAt, maxResults);
		}

		try (
			PreparedStatement pstmt = conn.prepareStatement(sql);
			ResultSet rs = pstmt.executeQuery()
		) {

			List<Pair<Long, Long>> result = new ArrayList<>();
			while (rs.next()) {
				Long queryId = rs.getLong(1);
				SavedQuery savedQuery = new SavedQuery();
				savedQuery.setQueryDefJson(rs.getString(2));
				result.add(Pair.make(queryId, savedQuery.getCpId()));
			}

			return result;
		}
	}

	private void updateQueryCpIds(JdbcConnection conn, Set<Long> cpIds, List<Pair<Long, Long>> queryCpIds)
	throws Exception {
		try (PreparedStatement pstmt = conn.prepareStatement(UPDATE_CP_ID_SQL)) {
			for (Pair<Long, Long> queryCpId : queryCpIds) {
				if (queryCpId.second() == null) {
					continue;
				}

				if (!cpIds.contains(queryCpId.second())) {
					logger.error("Query " + queryCpId.first() + " refers to a non-existent CP " + queryCpId.second());
					continue;
				}


				pstmt.setLong(1, queryCpId.second());
				pstmt.setLong(2, queryCpId.first());
				pstmt.addBatch();
			}

			pstmt.executeBatch();
		}
	}

	private static final LogUtil logger = LogUtil.getLogger(InitializeSavedQueryCpId.class);

	private static final String GET_CP_IDS_SQL = "select identifier from catissue_collection_protocol";

	private static final String GET_QUERIES_MYSQL = "select identifier, query_def from catissue_saved_queries limit %d, %d";

	private static final String GET_QUERIES_ORA = "select * from (select tab.*, rownum rnum from (select identifier, query_def from catissue_saved_queries) tab where rownum <= %d) where rnum >= %d";

	private static final String UPDATE_CP_ID_SQL = "update catissue_saved_queries set cp_id = ? where identifier = ?";
}
