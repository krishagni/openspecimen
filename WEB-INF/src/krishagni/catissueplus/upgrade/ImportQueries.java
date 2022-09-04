package krishagni.catissueplus.upgrade;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;

import edu.common.dynamicextensions.ndao.JdbcDao;
import edu.common.dynamicextensions.ndao.JdbcDaoFactory;
import edu.common.dynamicextensions.ndao.ResultExtractor;
import edu.common.dynamicextensions.ndao.TransactionManager;
import edu.common.dynamicextensions.nutility.IoUtil;

public class ImportQueries {
	private static final LogUtil logger = LogUtil.getLogger(ImportQueries.class);
	
	public static void main(String[] args) 
	throws Exception {
		if (args.length != 3) {
			logger.error("usage: ImportQueries <username> <queries-def-dir> <query-folder-name>");
			logger.error("Exiting importing queries");
			return;
		}
		
		DataSource ds = DbUtil.getDataSource();
		JdbcDaoFactory.setDataSource(ds);
        TransactionManager.getInstance(ds, null);
		
        Long userId = getUserId(args[0]);
        if (userId == null) {
        	logger.error("Invalid username: " + args[0]);
        	return;
        }
                
        importQueries(userId, args[1], args[2]);        
	}
	
	private static Long getUserId(String username) 
	throws Exception {
		JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
		List<Object> params = new ArrayList<>();
		params.add(username);
		
		Long userId = jdbcDao.getResultSet(GET_USER_ID_SQL, params, rs -> rs.next() ? rs.getLong(1) : null);
		if (userId == null) {
			logger.error("No active user with login name: " + username);
			return null;
		}
		
		return userId;
	}
	
	private static void importQueries(Long userId, String queriesDir, String folderName) 
	throws Exception { 
		Long folderId = setupQueryFolder(userId, folderName);
		if (folderId == null) {
			logger.error("Error creating query folder. Exiting import queries.");
			return;
		}		

		Path queriesPath = Paths.get(queriesDir);
		if (!queriesPath.isAbsolute()) {
			logger.error("Queries directory must be absolute directory path");
			return;
		}


		queriesPath = queriesPath.normalize();
		for (String file : Objects.requireNonNull(queriesPath.toFile().list())) {
			Path filepath = queriesPath.resolve(file).normalize();
			if (!filepath.startsWith(queriesPath)) {
				logger.error("Possible path traversal problem: " + file);
				continue;
			}

			String filename = filepath.toString();
			logger.info("Importing query from file: " + filename);

			Object[] row = getQueryIdAndMd5Digest(filepath);
			byte[] content = getFileContent(filepath);
			String digest = Utility.getDigest(content);
			
			if (row == null) {
				insertQuery(folderId, userId, filename, content, digest);
			} else {
				Long queryId = (Long)row[0];
				String dbDigest = (String)row[1];
								
				if (dbDigest != null && dbDigest.equals(digest)) {
					logger.info("No change found in file " + file + " since last import");
					continue;
				}
				
				updateQuery(userId, queryId, filename, content, digest);				
			}
		}		
	}

	private static Long setupQueryFolder(Long userId, String folderName) {
		if (folderName == null || folderName.trim().isEmpty()) {
			logger.error("Invalid folder name (empty folder name)");
			return null;
		}
		
		Long folderId = getFolderIdByName(folderName);		
		if (folderId == null) {
			folderId = createQueryFolder(userId, folderName);
		} else if (!shareQueryFolderWithAll(folderId)) {
			folderId = null;
		}
		
		return folderId;
	}
				
	
	private static Object[] getQueryIdAndMd5Digest(Path file) {
		JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
		List<Object> params = new ArrayList<>();
		params.add(file.toString());
		params.add(file.toString());
		
		return jdbcDao.getResultSet(GET_QUERY_ID_AND_MD5_SQL, params, new ResultExtractor<Object[]>() {
			@Override
			public Object[] extract(ResultSet rs) throws SQLException {
				return rs.next() ? new Object[] {rs.getLong(1), rs.getString(2)} : null;
			}			
		});				
	}
	
	private static Long getFolderIdByName(String folderName) {
		JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
		return jdbcDao.getResultSet(
				GET_FOLDER_ID_BY_NAME_SQL, 
				Collections.singletonList(folderName), 
				new ResultExtractor<Long>() {
					@Override
					public Long extract(ResultSet rs) throws SQLException {
						return rs.next() ? rs.getLong(1) : null;
					}
				});	
	}
	
	private static boolean shareQueryFolderWithAll(Long folderId) {
		TransactionManager.Transaction txn = TransactionManager.getInstance().newTxn();
		try {
			JdbcDaoFactory.getJdbcDao().executeUpdate(
					SHARE_FOLDER_WITH_ALL_SQL, 
					Collections.singletonList(folderId));
			TransactionManager.getInstance().commit(txn);			
			return true;
		} catch (Exception e) {
			logger.error("Error sharing folder with all users: " + folderId, e);
			TransactionManager.getInstance().rollback(txn);
			return false;
		}
	}
	
	private static Long createQueryFolder(Long userId, String folderName) {
		TransactionManager.Transaction txn = TransactionManager.getInstance().newTxn();
		try {			
			JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
			String sql = DbUtil.isOracle() ? INSERT_QUERY_FOLDER_ORA_SQL : INSERT_QUERY_FOLDER_MY_SQL;			
			
			List<? extends Object> params = Arrays.asList(folderName, userId);						
			Number id = jdbcDao.executeUpdateAndGetKey(sql, params, "IDENTIFIER");
			if (id == null) {
				logger.error("Error creating query folder: " + folderName);
				TransactionManager.getInstance().rollback(txn);
				return null;
			}
						 
			TransactionManager.getInstance().commit(txn);
			return id.longValue();
		} catch (Exception e) {
			logger.error("Error creating query folder: " + folderName, e);
			TransactionManager.getInstance().rollback(txn);
			return null;
		}
	}
	
	private static byte[] getFileContent(Path filepath)
	throws Exception {
		FileInputStream fin = null;
		ByteArrayOutputStream bout = null;
		try {
			fin = new FileInputStream(filepath.toFile());
			bout = new ByteArrayOutputStream();
			IoUtil.copy(fin, bout);
			return bout.toByteArray();
		} finally {
			IoUtil.close(fin);
			IoUtil.close(bout);
		}
	}
	
	private static SavedQuery getSavedQuery(byte[] content)
	throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(
			mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(Visibility.ANY)
				.withGetterVisibility(Visibility.NONE)
				.withSetterVisibility(Visibility.NONE)
				.withCreatorVisibility(Visibility.NONE));
		return mapper.readValue(content, SavedQuery.class);				
	}
	
	private static void insertQuery(Long queryFolderId, Long userId, String filename, byte[] queryContent, String md5) {
		 TransactionManager.Transaction txn = TransactionManager.getInstance().newTxn();
		 try {
			 //
			 // Step 1: Parse query file and get saved query object
			 //
			 SavedQuery query = getSavedQuery(queryContent);
			 JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
			 List<? extends Object> params = Arrays.asList(
					 query.getTitle(),
					 userId,
					 userId,
					 query.getQueryDefJson(),
					 new Timestamp(Calendar.getInstance().getTimeInMillis()));
			 
			 //
			 // Step 2: Save query
			 //
			 String sql = DbUtil.isOracle() ? INSERT_QUERY_ORA_SQL : (DbUtil.isMySQL() ? INSERT_QUERY_MY_SQL : null);
			 Number id = jdbcDao.executeUpdateAndGetKey(sql, params, "IDENTIFIER");
			 if (id == null) {
				 logger.error("Error saving query definition from file: " + filename);
				 TransactionManager.getInstance().rollback(txn);
				 return;
			 }
			 
			 //
			 // Step 3: Assign query to a folder
			 //
			 jdbcDao.executeUpdate(ADD_QUERY_TO_FOLDER_SQL, Arrays.asList(id.longValue(), queryFolderId));
			 
			 //
			 // Step 4: Record the change log
			 //
			 insertChangeLog(jdbcDao, filename, md5, "INSERTED", id);
			 TransactionManager.getInstance().commit(txn);			 			 			 
		 } catch (Exception e) {
			 logger.error("Error saving query definition from file: " + filename, e);
			 TransactionManager.getInstance().rollback(txn);
		 }
	}
	
	private static void updateQuery(Long userId, Long queryId, String filename, byte[] queryContent, String md5) {
		 TransactionManager.Transaction txn = TransactionManager.getInstance().newTxn();
		 try {
			 SavedQuery query = getSavedQuery(queryContent);
			 JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
			 List<? extends Object> params = Arrays.asList(
					 query.getTitle(),
					 userId,
					 query.getQueryDefJson(),
					 new Timestamp(Calendar.getInstance().getTimeInMillis()),
					 queryId);
			 
			 jdbcDao.executeUpdate(UPDATE_QUERY_SQL, params);
			 insertChangeLog(jdbcDao, filename, md5, "UPDATED", queryId);
			 TransactionManager.getInstance().commit(txn);			 			 			 
		 } catch (Exception e) {
			 logger.error("Error updating query " + queryId + " using definition from file: " + filename, e);
			 TransactionManager.getInstance().rollback(txn);
		 }
	}
		
	private static void insertChangeLog(JdbcDao jdbcDao, String filename, String md5, String status, Number id) {		
		jdbcDao.executeUpdate(
				INSERT_CHANGE_LOG_SQL, 
				Arrays.asList(filename, md5, status, Calendar.getInstance().getTime(), id));
	}
	
	private static final String GET_USER_ID_SQL = 
			"select" +
			"	identifier " +
			"from catissue_user " +
			"	where login_name = ? AND activity_status = 'Active'";
	
	private static final String GET_QUERY_ID_AND_MD5_SQL =
			"select " +
	        "  query_id, md5_digest " +
			"from " +
			"  catissue_import_queries_log ql " +
			"where " +
			"  ql.filename = ? and ql.executed_on in (" +
			"    select " +
			"      max(executed_on) " +
			"    from " +
			"      catissue_import_queries_log " +
			"    where " +
			"      filename = ? )";
	
	private static final String INSERT_QUERY_ORA_SQL = 
			"insert into catissue_saved_queries " +
			"  (identifier, title, created_by, last_updated_by, last_run_on, last_run_count, query_def, last_updated_on, deleted_on) " +
			"values " +
			"  (catissue_saved_queries_seq.nextval, ?, ?, ?, null, null, ?, ?, null)";

	private static final String INSERT_QUERY_MY_SQL = 
			"insert into catissue_saved_queries " +
			"  (identifier, title, created_by, last_updated_by, last_run_on, last_run_count, query_def, last_updated_on, deleted_on) " +
			"values " +
			"  (default, ?, ?, ?, null, null, ?, ?, null)";
		
	private static final String UPDATE_QUERY_SQL =
			"update catissue_saved_queries " +
		    "set title = ?, last_updated_by = ?, query_def = ?, last_updated_on = ? where identifier = ?";
	
	private static final String INSERT_CHANGE_LOG_SQL = 
			"insert into catissue_import_queries_log " +
			"  (filename, md5_digest, status, executed_on, query_id) " +
			"values " +
			"  (?, ?, ?, ?, ?)";
	
	private static final String GET_FOLDER_ID_BY_NAME_SQL =
			"select identifier from catissue_query_folders where name = ?";
	
	private static final String INSERT_QUERY_FOLDER_ORA_SQL = 
			"insert into catissue_query_folders " +
			"  (identifier, name, owner, shared_with_all) " +
			"values " +
			"  (catissue_query_folders_seq.nextval, ?, ?, 1)";
	
	private static final String INSERT_QUERY_FOLDER_MY_SQL = 
			"insert into catissue_query_folders " +
			"  (identifier, name, owner, shared_with_all) " +
			"values " +
			"  (default, ?, ?, 1)";
	
	private static final String ADD_QUERY_TO_FOLDER_SQL = 
			"insert into catissue_query_folder_queries(query_id, folder_id) values (?, ?)";
	
	private static final String SHARE_FOLDER_WITH_ALL_SQL = 
			"update catissue_query_folders set shared_with_all = 1 where identifier = ?";	
}
