package com.krishagni.catissueplus.core.upgrade;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;


import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.util.LogUtil;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class UpdateTransferEventDisplayFormat implements CustomTaskChange {
	private static final LogUtil logger = LogUtil.getLogger(UpdateTransferEventDisplayFormat.class);

	@Override
	public void execute(Database database)
	throws CustomChangeException {
		JdbcConnection conn = (JdbcConnection) database.getConnection();

		try (
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(GET_TE_SQL);
			PreparedStatement pstmt = conn.prepareStatement(UPDATE_POS_SQL)
		) {
			int count = 0;
			while (rs.next()) {
				Object fromPosition = rs.getObject(2);
				Pair<String, String> from = null;
				if (fromPosition != null) {
					StorageContainer fromContainer = getContainer(rs, 3);
					from = toDisplayFormat(fromContainer, ((Number) fromPosition).intValue());
				}

				Object toPosition = rs.getObject(10);
				Pair<String, String> to = null;
				if (toPosition != null) {
					StorageContainer toContainer = getContainer(rs, 11);
					to = toDisplayFormat(toContainer, ((Number) toPosition).intValue());
				}

				if (from == null && to == null) {
					continue;
				}

				if (from != null) {
					pstmt.setString(1, from.first());
					pstmt.setString(2, from.second());
				} else {
					pstmt.setNull(1, Types.VARCHAR);
					pstmt.setNull(2, Types.VARCHAR);
				}

				if (to != null) {
					pstmt.setString(3, to.first());
					pstmt.setString(4, to.second());
				} else {
					pstmt.setNull(3, Types.VARCHAR);
					pstmt.setNull(4, Types.VARCHAR);
				}

				pstmt.setLong(5, rs.getLong(1));
				pstmt.addBatch();
				++count;

				if (count == 50) {
					pstmt.executeBatch();
					count = 0;
				}
			}

			if (count > 0) {
				pstmt.executeBatch();
			}
		} catch (Exception e) {
			logger.error("Encountered error updating transfer event from/to positions display formats", e);
			throw new CustomChangeException(e);
		}
	}

	@Override
	public String getConfirmationMessage() {
		return "Transfer events' row/column display names updated!";
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

	private StorageContainer getContainer(ResultSet rs, int startIdx)
	throws Exception {
		int idx = startIdx;
		StorageContainer container = new StorageContainer();
		container.setName(rs.getString(idx++));
		container.setNoOfRows(getInt(rs, idx++));
		container.setNoOfColumns(getInt(rs, idx++));
		container.setPositionLabelingMode(StorageContainer.PositionLabelingMode.valueOf(rs.getString(idx++)));
		container.setPositionAssignment(StorageContainer.PositionAssignment.valueOf(rs.getString(idx++)));
		container.setColumnLabelingScheme(rs.getString(idx++));
		container.setRowLabelingScheme(rs.getString(idx));
		return container;
	}

	private Integer getInt(ResultSet rs, int idx)
	throws Exception {
		Object num = rs.getObject(idx);
		if (num == null) {
			return null;
		}

		return ((Number) num).intValue();
	}

	private Pair<String, String> toDisplayFormat(StorageContainer container, Integer position) {
		if (container.isDimensionless()) {
			return null;
		}

		Pair<Integer, Integer> pos = container.getPositionAssigner().fromPosition(container, position);
		return Pair.make(container.toRowLabelingScheme(pos.first()), container.toColumnLabelingScheme(pos.second()));
	}


	private static final String GET_TE_SQL =
		"select " +
		"  te.identifier, " +
		"  te.from_position, cf.name, cf.no_of_rows, cf.no_of_cols, cf.pos_labeling_mode, cf.pos_assignment, " +
		"  cf.column_labeling_scheme, cf.row_labeling_scheme, " +
		"  te.to_position, ct.name, ct.no_of_rows, ct.no_of_cols, ct.pos_labeling_mode, ct.pos_assignment, " +
		"  ct.column_labeling_scheme, ct.row_labeling_scheme " +
		"from " +
		"  catissue_transfer_event_param te " +
		"  left join os_storage_containers cf on cf.identifier = te.from_storage_container_id " +
		"  left join os_storage_containers ct on ct.identifier = te.to_storage_container_id " +
		"order by " +
		"  cf.name, ct.name";

	private static final String UPDATE_POS_SQL =
		"update " +
		"  catissue_transfer_event_param " +
		"set " +
		"  from_row = ?, from_col = ?, " +
		"  to_row = ?, to_col = ? " +
		"where " +
		"  identifier = ?";
}
