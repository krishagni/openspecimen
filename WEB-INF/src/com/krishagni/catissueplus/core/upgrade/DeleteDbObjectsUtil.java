
package com.krishagni.catissueplus.core.upgrade;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Statement;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;


import com.krishagni.catissueplus.core.common.util.LogUtil;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class DeleteDbObjectsUtil implements CustomTaskChange {

	private static final LogUtil logger = LogUtil.getLogger(DeleteDbObjectsUtil.class);

	private String file;

	private ResourceAccessor resourceAccessor;

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	@Override
	public String getConfirmationMessage() {
		return "Successfully deleted db objects from file: " + file;
	}

	@Override
	public void setUp() throws SetupException {

	}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {
		this.resourceAccessor = resourceAccessor;
	}

	@Override
	public ValidationErrors validate(Database database) {
		return new ValidationErrors();
	}

	@Override
	public void execute(Database database) throws CustomChangeException {
		InputStream in = null;
		BufferedReader reader = null;
		try {
			JdbcConnection conn = (JdbcConnection) database.getConnection();

			in = resourceAccessor.getResourcesAsStream(file).iterator().next();
			reader = new BufferedReader(new InputStreamReader(in));

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}

				String[] fields = line.split(",");
				String objectName = fields[0], type = null;
				if (fields.length > 1) {
					type = fields[1];
				}

				deleteObject(conn, type, objectName);
			}
		} catch (Exception e) {
			throw new CustomChangeException("Error deleting objects specified in file " + file, e);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(reader);
		}
	}

	private void deleteObject(JdbcConnection conn, String type, String objectName)
	throws Exception {
		if (StringUtils.isBlank(type) || type.equalsIgnoreCase("table")) {
			deleteTable(conn, objectName);
		} else if (type.equalsIgnoreCase("view")) {
			deleteView(conn, objectName);
		} else {
			logger.warn("Unknown DB object type" + type + ", for " + objectName);
		}
	}

	private void deleteTable(JdbcConnection conn, String tableName)
	throws Exception {
		if (isMySql(conn)) {
			deleteMySqlTable(conn, tableName);
		} else {
			deleteOracleTable(conn, tableName);
		}
	}

	private void deleteView(JdbcConnection conn, String viewName)
	throws Exception {
		executeUpdate(conn, String.format(DELETE_VIEW_SQL, viewName));
	}

	private void deleteMySqlTable(JdbcConnection conn, String tableName) {
		executeUpdate(conn, String.format(MYSQL_DELETE_TABLE_SQL, tableName));
	}

	private void deleteOracleTable(JdbcConnection conn, String tableName) {
		executeUpdate(conn, String.format(ORA_DELETE_TABLE_SQL, tableName));
	}

	private void executeUpdate(JdbcConnection conn, String sql) {
		Statement stmt = null;

		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			logger.info(String.format("Executing SQL: %s : failed", sql), e);
		} finally {
			close(stmt);
		}
	}

	private void close(Statement stmt) {
		try {
			if (stmt != null) {
				stmt.close();
			}
		} catch (Exception e) {

		}
	}

	public Boolean isMySql(JdbcConnection conn)
	throws Exception {
		return conn.getDatabaseProductName().equalsIgnoreCase("MySQL");
	}

	private String ORA_DELETE_TABLE_SQL = "drop table %s cascade constraints";

	private String MYSQL_DELETE_TABLE_SQL = "drop table %s";

	private String DELETE_VIEW_SQL = "drop view %s";
}
