package krishagni.catissueplus.upgrade;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;


import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.de.ui.UserControlFactory;

import au.com.bytecode.opencsv.CSVWriter;
import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.UserContext;
import edu.common.dynamicextensions.domain.nui.factory.ControlManager;
import edu.common.dynamicextensions.ndao.ColumnTypeHelper;
import edu.common.dynamicextensions.ndao.DbSettingsFactory;
import edu.common.dynamicextensions.ndao.JdbcDao;
import edu.common.dynamicextensions.ndao.JdbcDaoFactory;
import edu.common.dynamicextensions.ndao.ResultExtractor;
import edu.common.dynamicextensions.ndao.TransactionManager;
import edu.common.dynamicextensions.ndao.TransactionManager.Transaction;
import edu.common.dynamicextensions.nutility.IoUtil;

public class MigrateSpecimenEvents {
	private static final LogUtil logger = LogUtil.getLogger(MigrateSpecimenEvents.class);

	private static final int INSERT_BATCH_SIZE = 5000;
	
	private static CSVWriter eventsLog;
	
	private String eventName;
	
	private String eventFormDef;
	
	private String eventTable;
	
	private boolean systemEvent;
	
	
			
	@SuppressWarnings("unchecked")
	public static void main(String[] args) 
	throws Exception {
		LogUtil.setInfoLevel();
		logger.info("Migrating Specimen Events ...");

		if (args.length != 2) {
			logger.error("usage: MigrateSpecimenEvents <username> <config-file>");
			return;
		}

		DataSource ds = DbUtil.getDataSource();
		DbSettingsFactory.init(DbUtil.isMySQL() ? "MySQL" : "Oracle");

		JdbcDaoFactory.setDataSource(ds);
		TransactionManager.getInstance(ds, null);

		UserContext ctx = getUserCtxt(args[0]);
		if (ctx == null) {
			return;
		}
		
		eventsLog = new CSVWriter(new FileWriter("events-record-mapping.csv"));
		
		ControlManager.getInstance().registerFactory(UserControlFactory.getInstance());
		
		List<Map<String, String>> eventsInfo = 
				new ObjectMapper().readValue(new File(args[1]), List.class);		
		for (Map<String, String> eventInfo : eventsInfo) {
			try {
				MigrateSpecimenEvents migrator = new MigrateSpecimenEvents(eventInfo);
				migrator.migrate(ctx);				
			} catch (Exception e) {
			}
		}
		
		IoUtil.close(eventsLog);
	}

	private static UserContext getUserCtxt(final String username)
	throws Exception {
		final Long userId = UpgradeUtil.getUserId(username);
		if (userId == null) {
			logger.error("Invalid username: " + username);
			return null;
		}

		return new UserContext() {
			@Override
			public String getUserName() {
				return username;
			}

			@Override
			public Long getUserId() {
				return userId;
			}

			@Override
			public String getIpAddress() {
				return null;
			}
		};
	}

	public MigrateSpecimenEvents(Map<String, String> eventInfo) {
		eventName = eventInfo.get("name");
		eventFormDef = eventInfo.get("form");
		eventTable = eventInfo.get("dbTable");
		
		String systemEventStr = eventInfo.get("systemEvent");
		if (systemEventStr != null && systemEventStr.trim().equalsIgnoreCase("true")) {
			systemEvent = true;
		}
	}
	
	public void migrate(UserContext ctx) 
	throws Exception {
		try {
			long t1 = System.currentTimeMillis();
			logger.info("Migrating : " + eventName);

			if (doesFormExists(eventName)) {
				logger.info("Specimen event " + eventName + " already exists. "
						+ "Stopping migration for this event");
				return;
			}

			if (!systemEvent) {
				logger.info("Deleting old event entries from parent table");
				deleteParentEventEntries(eventTable);				
			}

			logger.info("Creating form for event: " + eventName);
			Long formId = createForm(ctx, eventFormDef);
			if (formId == null) {
				logger.error("Error creating form for event: " + eventName);
				throw new RuntimeException("Error creating form for event: " + eventName);
			}

			logger.info("Migrating records for event: " + eventName);
			migrateRecords(ctx, formId, eventTable);

			logger.info("Adjusting identifier column for event: " + eventName);
			
			long t2 = System.currentTimeMillis();
			if (!systemEvent) {
				adjustEventIdColumn(eventTable);
			} else {
				adjustSystemEventIdColumn(eventTable);
			}
			logger.info("Adjusting identifier column for event: " + eventName + " took " + timeDiff(t2));
			
			logger.info("Migrated : " + eventName + " in " + timeDiff(t1) + " ms");
		} catch (Exception e) {
			logger.error("Error migrating data for event: " + eventName, e);
			throw e;
		}
	}
	
	private long timeDiff(long time) {
		return System.currentTimeMillis() - time;
	}

	private boolean doesFormExists(String formName) {
		return JdbcDaoFactory.getJdbcDao().getResultSet(
				GET_FORM_ID_BY_NAME_SQL, Collections.singletonList(formName),
				new ResultExtractor<Boolean>() {
					@Override
					public Boolean extract(ResultSet rs) throws SQLException {
						return rs.next() && rs.getObject(1) != null;
					}
				});
	}

	private void deleteParentEventEntries(String table) {
		JdbcDaoFactory.getJdbcDao().executeDDL(String.format(DELETE_EVENT_ENTRIES_SQL, table));
	}

	private Long createForm(UserContext ctx, String formFile) throws Exception {
		Transaction txn = TransactionManager.getInstance().newTxn();
		try {
			Long formId = Container.createContainer(ctx, formFile, ".", false);
			Long formCtxId = insertFormCtx(formId);
			TransactionManager.getInstance().commit(txn);
			return formCtxId;
		} catch (Exception e) {
			TransactionManager.getInstance().rollback(txn);
			logger.error("Error creating form", e);
		}

		return null;
	}

	private Long insertFormCtx(Long formId) throws Exception {
		JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
		String sql = DbUtil.isOracle() ? INSERT_FORM_CTX_ORA_SQL : INSERT_FORM_CTX_MY_SQL;

		List<? extends Object> params = Arrays.asList(formId, "SpecimenEvent", -1, null, 1, systemEvent ? 1 : 0);
		Number id = jdbcDao.executeUpdateAndGetKey(sql, params, "IDENTIFIER");
		if (id == null) {
			logger.error("Error creating form context for form: " + formId);
			return null;
		}

		return id.longValue();
	}

	private void migrateRecords(UserContext ctx, Long formId, String table) 
	throws Exception {
		addEventIdColumn(table);

		boolean endOfRecords = false;
		int startRow = 0;
		while (!endOfRecords) {
			logger.info("Migrating records chunk (" + 
					startRow + ", " + (startRow + INSERT_BATCH_SIZE) + 
					") from " + table);

			Transaction txn = TransactionManager.getInstance().newTxn();
			JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();

			Map<Long, Long> recordsMap = getIdSpecimenIdRecs(table, startRow, INSERT_BATCH_SIZE, jdbcDao); // id -> specimen id
			if (recordsMap.isEmpty()) {
				endOfRecords = true;
			}

			insertAndUpdateNewRecordIds(ctx.getUserId(), formId, table,	recordsMap, jdbcDao);

			startRow += recordsMap.size();
			TransactionManager.getInstance().commit(txn);
		}

		logger.info("Migrated " + (startRow) + " records");
	}

	private String getIdSpecimenIdSql(String table, int startRow, int numRows) {
		String sql = "";
		if (DbUtil.isOracle()) {
			sql = String.format(GET_ID_AND_SPECIMEN_ID_ORA_SQL, table, startRow + numRows, startRow);
		} else {
			sql = String.format(GET_ID_AND_SPECIMEN_ID_MY_SQL, table, startRow,	numRows);
		}

		return sql;
	}

	private Map<Long, Long> getIdSpecimenIdRecs(String table, int startRow,	int batchSize, JdbcDao jdbcDao) {
		// identifier, specimen id
		String sql = getIdSpecimenIdSql(table, startRow, batchSize);
		return jdbcDao.getResultSet(sql, null,
				new ResultExtractor<Map<Long, Long>>() {
					@Override
					public Map<Long, Long> extract(ResultSet rs)
					throws SQLException {
						Map<Long, Long> result = new HashMap<Long, Long>();

						while (rs.next()) {
							result.put(rs.getLong(1), rs.getLong(2));
						}

						return result;
					}
				});
	}

	private void insertAndUpdateNewRecordIds(Long userId, Long formId, String table, Map<Long, Long> records, JdbcDao jdbcDao) 
	throws Exception {
		List<Object[]> inserts = new ArrayList<Object[]>();
		List<Object[]> updates = new ArrayList<Object[]>();

		Date time = Calendar.getInstance().getTime();
		for (Map.Entry<Long, Long> record : records.entrySet()) {
			Long recordId = getNextRecordId();
			inserts.add(new Object[] { formId, record.getValue(), recordId,	userId, time, "ACTIVE" });
			updates.add(new Object[] { recordId, record.getKey() });
		}

		if (inserts.isEmpty() || updates.isEmpty()) {
			return;
		}

		String insertSql = DbUtil.isMySQL() ? INSERT_FORM_RECORD_MY_SQL	: INSERT_FORM_RECORD_ORA_SQL;
		jdbcDao.batchUpdate(insertSql, inserts);

		String updateSql = String.format(UPDATE_EVENT_ID_SQL, table);
		jdbcDao.batchUpdate(updateSql, updates);
		
		log(updates);
	}
	
	private void log(List<Object[]> updates)  
	throws IOException { 
		for (Object[] row : updates) { // [ {new record ID, old event id
			eventsLog.writeNext(new String[] {eventName, row[1].toString(), row[0].toString(), "SUCCESS"});
		}
		
		eventsLog.flush();
	}

	private Long getNextRecordId() {
		return JdbcDaoFactory.getJdbcDao().getNextId("RECORD_ID_SEQ");
	}

	private void addEventIdColumn(String table) {
		String sql = String.format(ADD_COL_SQL, table,	"EVENT_ID", ColumnTypeHelper.getIntegerColType());
		JdbcDaoFactory.getJdbcDao().executeDDL(sql);
	}

	private void adjustEventIdColumn(String table) {
		dropIdentifierColumn(table);
		renameEventIdColumn(table);
		addPkColumn(table, "identifier");
	}
	
	private void adjustSystemEventIdColumn(String table) {
		String addSpeColumnSql = String.format(ADD_COL_SQL, table, "SPE_ID", ColumnTypeHelper.getIntegerColType());
		JdbcDaoFactory.getJdbcDao().executeDDL(addSpeColumnSql);
		
		String updateSpeIdSql = String.format(UPDATE_SPE_ID_SQL, table);
		JdbcDaoFactory.getJdbcDao().executeUpdate(updateSpeIdSql, null);
		
		dropIdentifierColumn(table);
		renameEventIdColumn(table);
		addPkColumn(table, "SPE_ID");
		
		String indexName = table.substring(0, 22).toLowerCase() + "_id_idx";
		String createIdxSql = String.format(CREATE_IDX_SQL, indexName, table, "IDENTIFIER");
		JdbcDaoFactory.getJdbcDao().executeDDL(createIdxSql);								
	}

	private void dropIdentifierColumn(String table) {
		JdbcDaoFactory.getJdbcDao().executeDDL(String.format(DROP_ID_COL_SQL, table));
	}

	private void renameEventIdColumn(String table) {
		String sql = DbUtil.isOracle() ? RENAME_EVENT_ID_TO_ID_ORA_SQL : RENAME_EVENT_ID_TO_ID_MY_SQL;
		JdbcDaoFactory.getJdbcDao().executeDDL(String.format(sql, table));
	}

	private void addPkColumn(String table, String column) {
		String sql = String.format(ADD_PK_SQL, table, column);
		JdbcDaoFactory.getJdbcDao().executeDDL(sql);
	}

	private static final String INSERT_FORM_CTX_MY_SQL = "insert into catissue_form_context( "
			+ "  identifier, container_id, entity_type, cp_id, sort_order, is_multirecord, is_sys_form) "
			+ "values (" + "  default, ?, ?, ?, ?, ?, ?)";

	private static final String INSERT_FORM_CTX_ORA_SQL = "insert into catissue_form_context( "
			+ "  identifier, container_id, entity_type, cp_id, sort_order, is_multirecord, is_sys_form) "
			+ "values ("
			+ "  catissue_form_context_seq.nextval, ?, ?, ?, ?, ?, ?)";

	private static final String GET_ID_AND_SPECIMEN_ID_ORA_SQL = 
			"select * from " +
			"  (select tab.*, rownum rnum from " +
			"    (select identifier, specimen_id from %s order by identifier) tab " +
			"   where rownum <= %d) where rnum > %d";

	private static final String GET_ID_AND_SPECIMEN_ID_MY_SQL = 
			"select identifier, specimen_id from %s order by identifier limit %d, %d";

	private static final String INSERT_FORM_RECORD_MY_SQL = "insert into catissue_form_record_entry( "
			+ " identifier, form_ctxt_id, object_id, record_id, updated_by, update_time, activity_status) "
			+ "values (" + " default, ?, ?, ?, ?, ?, ?)";

	private static final String INSERT_FORM_RECORD_ORA_SQL = "insert into catissue_form_record_entry( "
			+ " identifier, form_ctxt_id, object_id, record_id, updated_by, update_time, activity_status) "
			+ "values ("
			+ " catissue_form_rec_entry_seq.nextval, ?, ?, ?, ?, ?, ?)";

	private static final String ADD_COL_SQL = "alter table %s add %s %s";

	private static final String UPDATE_EVENT_ID_SQL = "update %s set event_id = ? where identifier = ?";

	private static final String DROP_ID_COL_SQL = "alter table %s drop column identifier";

	private static final String RENAME_EVENT_ID_TO_ID_ORA_SQL = "alter table %s rename column EVENT_ID to IDENTIFIER";

	private static final String RENAME_EVENT_ID_TO_ID_MY_SQL = "alter table %s change EVENT_ID IDENTIFIER BIGINT";

	private static final String ADD_PK_SQL = "alter table %s add primary key(%s)";

	private static final String GET_FORM_ID_BY_NAME_SQL = "select identifier from dyextn_containers where name = ?";

	private static final String DELETE_EVENT_ENTRIES_SQL = 
			"delete from catissue_specimen_event_param where identifier in (select identifier from %s)";
	
	private static final String UPDATE_SPE_ID_SQL = 
			"update %s set spe_id = identifier";
	
	private static final String CREATE_IDX_SQL =
			"create index %s on %s(%s)";
}