package com.krishagni.catissueplus.core.upgrade;

import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import com.krishagni.catissueplus.core.biospecimen.WorkflowConfigUtil;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class InitializeCpGroupWorkflowsInheritance implements CustomTaskChange {
	@Override
	public void execute(Database database) throws CustomChangeException {
		try {
			JdbcConnection connection = (JdbcConnection) database.getConnection();
			try (
				PreparedStatement selectStmt = connection.prepareStatement(GET_CP_WORKFLOWS);
				PreparedStatement updateStmt = connection.prepareStatement(UPDATE_INHERITANCE);
				PreparedStatement insertStmt = connection.prepareStatement(INSERT_CP_WORKFLOWS);
				ResultSet resultSet = selectStmt.executeQuery()
			) {
				while (resultSet.next()) {
					long cpId = resultSet.getLong("cp_id");
					Long groupId = resultSet.getLong("group_id");
					if (resultSet.wasNull()) {
						groupId = null;
					}

					String cpWorkflows = resultSet.getString("cp_workflows");
					String groupWorkflows = resultSet.getString("group_workflows");
					if (groupId == null) {
						if (cpWorkflows != null) {
							updateInheritance(updateStmt, cpId, null);
						}
					} else if (cpWorkflows == null) {
						insertWorkflows(insertStmt, cpId, groupWorkflows != null ? groupWorkflows : "[]");
					} else {
						updateInheritance(
							updateStmt,
							cpId,
							WorkflowConfigUtil.areSame(cpWorkflows, groupWorkflows)
						);
					}
				}
			}
		} catch (Exception e) {
			throw new CustomChangeException("Error initializing CP group workflows inheritance", e);
		}
	}

	@Override
	public String getConfirmationMessage() {
		return "Initialized CP group workflows inheritance.";
	}

	@Override
	public void setUp() throws SetupException {
	}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {
	}

	@Override
	public ValidationErrors validate(Database database) {
		return new ValidationErrors();
	}

	private void updateInheritance(PreparedStatement statement, long cpId, Boolean inherited)
	throws Exception {
		if (inherited == null) {
			statement.setNull(1, Types.BOOLEAN);
		} else {
			statement.setBoolean(1, inherited);
		}

		statement.setLong(2, cpId);
		statement.executeUpdate();
	}

	private void insertWorkflows(PreparedStatement statement, long cpId, String workflows)
	throws Exception {
		statement.setLong(1, cpId);
		statement.setCharacterStream(2, new StringReader(workflows), workflows.length());
		statement.setBoolean(3, true);
		statement.executeUpdate();
	}

	private static final String GET_CP_WORKFLOWS =
		"select " +
		"  cp.identifier as cp_id, " +
		"  cp.cp_group_id as group_id, " +
		"  cpw.workflows as cp_workflows, " +
		"  cpg.workflows as group_workflows " +
		"from " +
		"  catissue_collection_protocol cp " +
		"  left join os_cp_workflows cpw on cpw.cp_id = cp.identifier " +
		"  left join os_cp_groups cpg on cpg.identifier = cp.cp_group_id";

	private static final String UPDATE_INHERITANCE =
		"update os_cp_workflows set group_workflows_inherited = ? where cp_id = ?";

	private static final String INSERT_CP_WORKFLOWS =
		"insert into os_cp_workflows(cp_id, workflows, group_workflows_inherited) values (?, ?, ?)";
}
