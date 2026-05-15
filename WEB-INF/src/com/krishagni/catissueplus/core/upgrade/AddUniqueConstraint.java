package com.krishagni.catissueplus.core.upgrade;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.util.LogUtil;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class AddUniqueConstraint implements CustomTaskChange {
	private static final LogUtil logger = LogUtil.getLogger(AddUniqueConstraint.class);

	private String tableName;

	private String columnNames;

	private String constraintName;

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public void setColumnNames(String columnNames) {
		this.columnNames = columnNames;
	}

	public void setConstraintName(String constraintName) {
		this.constraintName = constraintName;
	}

	@Override
	public void execute(Database database) throws CustomChangeException {
		try {
			List<String> columns = getColumnNames();
			String uniqueConstraintName = getConstraintName();

			logger.info("Checking whether unique constraint exists on " + tableName + "(" + columnNames + ")");
			JdbcConnection conn = (JdbcConnection) database.getConnection();
			Connection jdbcConn = conn.getUnderlyingConnection();

			if (hasUniqueIndex(database, jdbcConn, columns)) {
				logger.info("Unique constraint already exists on " + tableName + "(" + columnNames + "). Skipping.");
				return;
			}

			String sql = getAddConstraintSql(database, columns, uniqueConstraintName);
			logger.info("Adding unique constraint " + uniqueConstraintName + " on " + tableName + "(" + columnNames + ")");
			try (Statement stmt = conn.createStatement()) {
				stmt.executeUpdate(sql);
			}

			logger.info("Added unique constraint " + uniqueConstraintName + " on " + tableName + "(" + columnNames + ")");
		} catch (Exception e) {
			logger.error("Error adding unique constraint on " + tableName + "(" + columnNames + ")", e);
			throw new CustomChangeException(e);
		}
	}

	@Override
	public String getConfirmationMessage() {
		return "Added unique constraint on " + tableName + "(" + columnNames + ")";
	}

	@Override
	public void setUp() throws SetupException {

	}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {

	}

	@Override
	public ValidationErrors validate(Database database) {
		ValidationErrors errors = new ValidationErrors();
		errors.checkRequiredField("tableName", tableName);
		errors.checkRequiredField("columnNames", columnNames);

		if (StringUtils.isNotBlank(tableName) && !isValidIdentifier(tableName)) {
			errors.addError("Invalid tableName: " + tableName);
		}

		if (StringUtils.isNotBlank(constraintName) && !isValidIdentifier(constraintName)) {
			errors.addError("Invalid constraintName: " + constraintName);
		}

		if (StringUtils.isNotBlank(columnNames)) {
			List<String> columns = getColumnNames();
			if (columns.isEmpty()) {
				errors.addError("At least one columnName must be specified");
			}

			for (String columnName : columns) {
				if (!isValidIdentifier(columnName)) {
					errors.addError("Invalid columnName: " + columnName);
				}
			}
		}

		return errors;
	}

	private boolean hasUniqueIndex(Database database, Connection jdbcConn, List<String> expectedColumns)
	throws Exception {
		DatabaseMetaData metadata = jdbcConn.getMetaData();
		Set<TableRef> tables = getTableRefs(database, jdbcConn, metadata);

		for (TableRef table : tables) {
			List<String> uniqueColumns = getMatchingUniqueIndexColumns(metadata, table, expectedColumns);
			if (uniqueColumns != null) {
				logger.info("Found unique constraint/index on " + table.name + "(" + String.join(",", uniqueColumns) + ")");
				return true;
			}
		}

		return false;
	}

	private Set<TableRef> getTableRefs(Database database, Connection jdbcConn, DatabaseMetaData metadata)
	throws Exception {
		Set<TableRef> result = new LinkedHashSet<>();
		List<String> catalogs = getCatalogs(database, jdbcConn);
		List<String> schemas = getSchemas(database, metadata);
		List<String> names = getNameVariants(tableName);

		for (String catalog : catalogs) {
			for (String schema : schemas) {
				for (String name : names) {
					try (ResultSet rs = metadata.getTables(catalog, schema, name, new String[] {"TABLE"})) {
						while (rs.next()) {
							result.add(new TableRef(rs.getString("TABLE_CAT"), rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME")));
						}
					}
				}
			}
		}

		if (result.isEmpty()) {
			logger.warn("Could not find table " + tableName + " using database metadata. Trying supplied table name.");
			result.add(new TableRef(database.getDefaultCatalogName(), database.getDefaultSchemaName(), tableName));
		}

		return result;
	}

	private List<String> getMatchingUniqueIndexColumns(DatabaseMetaData metadata, TableRef table, List<String> expectedColumns)
	throws Exception {
		Map<String, TreeMap<Short, String>> uniqueIndexes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		try (ResultSet rs = metadata.getIndexInfo(table.catalog, table.schema, table.name, true, false)) {
			while (rs.next()) {
				boolean nonUnique = rs.getBoolean("NON_UNIQUE");
				short type = rs.getShort("TYPE");
				String indexName = rs.getString("INDEX_NAME");
				String columnName = rs.getString("COLUMN_NAME");
				short ordinalPosition = rs.getShort("ORDINAL_POSITION");

				if (nonUnique || type == DatabaseMetaData.tableIndexStatistic || StringUtils.isBlank(indexName) || StringUtils.isBlank(columnName)) {
					continue;
				}

				uniqueIndexes.computeIfAbsent(indexName, k -> new TreeMap<>()).put(ordinalPosition, columnName);
			}
		}

		for (TreeMap<Short, String> indexColumns : uniqueIndexes.values()) {
			List<String> columns = new ArrayList<>(indexColumns.values());
			if (hasSameColumns(columns, expectedColumns)) {
				return columns;
			}
		}

		return null;
	}

	private String getAddConstraintSql(Database database, List<String> columns, String uniqueConstraintName) {
		return "alter table " +
			database.escapeTableName(database.getDefaultCatalogName(), database.getDefaultSchemaName(), tableName) +
			" add constraint " +
			database.escapeConstraintName(uniqueConstraintName) +
			" unique (" +
			columns.stream()
				.map(column -> database.escapeColumnName(database.getDefaultCatalogName(), database.getDefaultSchemaName(), tableName, column))
				.collect(Collectors.joining(", ")) +
			")";
	}

	private List<String> getColumnNames() {
		return Arrays.stream(columnNames.split(","))
			.map(String::trim)
			.filter(StringUtils::isNotBlank)
			.collect(Collectors.toList());
	}

	private String getConstraintName() {
		return StringUtils.defaultIfBlank(constraintName, tableName + "_UQ");
	}

	private List<String> getCatalogs(Database database, Connection jdbcConn)
	throws Exception {
		Set<String> catalogs = new LinkedHashSet<>();
		catalogs.add(database.getDefaultCatalogName());
		catalogs.add(jdbcConn.getCatalog());
		catalogs.add(null);
		return new ArrayList<>(catalogs);
	}

	private List<String> getSchemas(Database database, DatabaseMetaData metadata)
	throws Exception {
		Set<String> schemas = new LinkedHashSet<>();
		schemas.add(database.getDefaultSchemaName());
		schemas.add(metadata.getUserName());
		schemas.add(StringUtils.upperCase(database.getDefaultSchemaName()));
		schemas.add(StringUtils.upperCase(metadata.getUserName()));
		schemas.add(null);
		return new ArrayList<>(schemas);
	}

	private List<String> getNameVariants(String name) {
		Set<String> names = new LinkedHashSet<>();
		names.add(name);
		names.add(StringUtils.upperCase(name));
		names.add(StringUtils.lowerCase(name));
		return new ArrayList<>(names);
	}

	private boolean hasSameColumns(List<String> columns, List<String> expectedColumns) {
		Set<String> normalizedColumns = normalize(columns);
		Set<String> normalizedExpectedColumns = normalize(expectedColumns);
		return normalizedColumns.size() == columns.size() &&
			normalizedExpectedColumns.size() == expectedColumns.size() &&
			normalizedColumns.equals(normalizedExpectedColumns);
	}

	private Set<String> normalize(List<String> values) {
		return values.stream()
			.map(StringUtils::upperCase)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private boolean isValidIdentifier(String name) {
		return name.matches("[A-Za-z][A-Za-z0-9_]*");
	}

	private static class TableRef {
		private String catalog;

		private String schema;

		private String name;

		private TableRef(String catalog, String schema, String name) {
			this.catalog = catalog;
			this.schema = schema;
			this.name = name;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof TableRef)) {
				return false;
			}

			TableRef other = (TableRef) obj;
			return StringUtils.equalsIgnoreCase(catalog, other.catalog) &&
				StringUtils.equalsIgnoreCase(schema, other.schema) &&
				StringUtils.equalsIgnoreCase(name, other.name);
		}

		@Override
		public int hashCode() {
			return StringUtils.upperCase(catalog + "." + schema + "." + name).hashCode();
		}
	}
}
