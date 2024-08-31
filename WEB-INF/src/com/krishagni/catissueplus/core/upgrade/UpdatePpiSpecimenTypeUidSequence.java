package com.krishagni.catissueplus.core.upgrade;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import com.krishagni.catissueplus.core.common.util.LogUtil;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class UpdatePpiSpecimenTypeUidSequence implements CustomTaskChange {
	private static final LogUtil logger = LogUtil.getLogger(UpdatePpiSpecimenTypeUidSequence.class);

	@Override
	public void execute(Database database) throws CustomChangeException {
		JdbcConnection conn = (JdbcConnection) database.getConnection();

		try (
			Statement typesStmt = conn.createStatement();
			ResultSet typesRs = typesStmt.executeQuery(GET_TYPES);

			Statement keySeqStmt = conn.createStatement();
			ResultSet keySeqRs = keySeqStmt.executeQuery(GET_TOKEN_KEY_SEQS);

			PreparedStatement updateKeySeq = conn.prepareStatement(UPDATE_TOKEN_KEY_SEQ);
		) {
			Map<String, Long> typeIds = new HashMap<>();
			while (typesRs.next()) {
				typeIds.put(typesRs.getString(2), typesRs.getLong(1));
			}

			int count = 0;
			while (keySeqRs.next()) {
				Long keyId = keySeqRs.getLong(1);
				String keyValue = keySeqRs.getString(2);

				int lastIdx = keyValue.lastIndexOf('_');
				String type = keyValue.substring(lastIdx + 1);
				Long typeId = typeIds.get(type);
				if (typeId == null) {
					continue;
				}

				keyValue = keyValue.substring(0, lastIdx) + "_" + typeId.toString();
				updateKeySeq.setString(1, keyValue);
				updateKeySeq.setLong(2, keyId);
				updateKeySeq.addBatch();
				++count;

				if (count >= 50) {
					updateKeySeq.executeBatch();
					count = 0;
				}
			}

			if (count > 0) {
				updateKeySeq.executeBatch();
			}
		} catch (Exception e) {
			logger.error("Encountered error updating PPI_SPEC_TYPE_UID key values", e);
			throw new CustomChangeException(e);
		}

	}

	@Override
	public String getConfirmationMessage() {
		return "Updated PPI_SPEC_TYPE_UID key values";
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

	private static final String GET_TOKEN_KEY_SEQS =
		"select " +
		"  identifier, key_value " +
		"from " +
		"  key_seq_generator " +
		"where " +
		"  key_type = 'PPI_SPEC_TYPE_UID'";

	private static final String GET_TYPES =
		"select " +
		"  identifier, value " +
		"from " +
		"  catissue_permissible_value " +
		"where " +
		"  public_id = 'specimen_type' and " +
		"  parent_identifier is not null";

	private static final String UPDATE_TOKEN_KEY_SEQ =
		"update key_seq_generator set key_value = ? where identifier = ?";
}
