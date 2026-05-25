package com.krishagni.catissueplus.core.upgrade;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class InitializeContainerTypeCapacity implements CustomTaskChange {
	@Override
	public String getConfirmationMessage() {
		return "Container type capacities initialized!";
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
	public void execute(Database database) throws CustomChangeException {
		try {
			JdbcConnection conn = (JdbcConnection) database.getConnection();
			initialiseCapacities(conn);
		} catch (Exception e) {
			throw new CustomChangeException("Error while initializing container type capacities", e);
		}
	}

	private void initialiseCapacities(JdbcConnection conn) throws Exception {
		Set<Long> processed = new HashSet<>();
		List<ContainerType> working = getStoreSpecimenTypes(conn);
		while (!working.isEmpty()) {
			ContainerType type = working.remove(0);
			if (!processed.add(type.id)) {
				continue;
			}

			Long capacity = getCapacity(type);
			updateCapacity(conn, type.id, capacity);
			if (capacity != null) {
				working.addAll(getActiveParentTypes(conn, type.id, capacity));
			}
		}
	}

	private List<ContainerType> getStoreSpecimenTypes(JdbcConnection conn) throws Exception {
		try (
			PreparedStatement stmt = conn.prepareStatement(GET_STORE_SPECIMEN_TYPES_SQL);
			ResultSet rs = stmt.executeQuery()
		) {
			List<ContainerType> types = new ArrayList<>();
			while (rs.next()) {
				types.add(getContainerType(rs, null));
			}

			return types;
		}
	}

	private List<ContainerType> getActiveParentTypes(JdbcConnection conn, Long typeId, Long childCapacity) throws Exception {
		try (PreparedStatement stmt = conn.prepareStatement(GET_ACTIVE_PARENT_TYPES_SQL)) {
			stmt.setLong(1, typeId);
			try (ResultSet rs = stmt.executeQuery()) {
				List<ContainerType> parents = new ArrayList<>();
				while (rs.next()) {
					parents.add(getContainerType(rs, childCapacity));
				}

				return parents;
			}
		}
	}

	private ContainerType getContainerType(ResultSet rs, Long childCapacity) throws Exception {
		ContainerType type = new ContainerType();
		type.id = rs.getLong("identifier");
		type.noOfRows = getInteger(rs, "no_of_rows");
		type.noOfColumns = getInteger(rs, "no_of_cols");
		type.storeSpecimens = rs.getBoolean("store_specimens");
		type.canHold = getLong(rs, "can_hold");
		type.childCapacity = childCapacity;
		return type;
	}

	private Long getCapacity(ContainerType type) {
		if (type.noOfRows == null || type.noOfColumns == null) {
			return null;
		}

		long positions = (long) type.noOfRows * type.noOfColumns;
		if (type.storeSpecimens) {
			return positions;
		} else if (type.childCapacity != null) {
			return positions * type.childCapacity;
		}

		return null;
	}

	private void updateCapacity(JdbcConnection conn, Long typeId, Long capacity) throws Exception {
		try (PreparedStatement stmt = conn.prepareStatement(UPDATE_CAPACITY_SQL)) {
			if (capacity == null) {
				stmt.setNull(1, java.sql.Types.BIGINT);
			} else {
				stmt.setLong(1, capacity);
			}

			stmt.setLong(2, typeId);
			stmt.executeUpdate();
		}
	}

	private Integer getInteger(ResultSet rs, String column) throws Exception {
		int value = rs.getInt(column);
		return rs.wasNull() ? null : value;
	}

	private Long getLong(ResultSet rs, String column) throws Exception {
		long value = rs.getLong(column);
		return rs.wasNull() ? null : value;
	}

	private static class ContainerType {
		private Long id;

		private Integer noOfRows;

		private Integer noOfColumns;

		private boolean storeSpecimens;

		private Long canHold;

		private Long childCapacity;
	}

	private static final String GET_STORE_SPECIMEN_TYPES_SQL =
		"select " +
		"  identifier, no_of_rows, no_of_cols, store_specimens, can_hold " +
		"from " +
		"  os_container_types " +
		"where " +
		"  activity_status != 'Disabled' and " +
		"  store_specimens = 1";

	private static final String GET_ACTIVE_PARENT_TYPES_SQL =
		"select " +
		"  identifier, no_of_rows, no_of_cols, store_specimens, can_hold " +
		"from " +
		"  os_container_types " +
		"where " +
		"  activity_status != 'Disabled' and " +
		"  can_hold = ?";

	private static final String UPDATE_CAPACITY_SQL =
		"update os_container_types set capacity = ? where identifier = ?";
}
