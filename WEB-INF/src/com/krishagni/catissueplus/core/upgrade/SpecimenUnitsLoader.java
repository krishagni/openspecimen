package com.krishagni.catissueplus.core.upgrade;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.util.LogUtil;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class SpecimenUnitsLoader implements CustomTaskChange {
	private static final LogUtil logger = LogUtil.getLogger(SpecimenUnitsLoader.class);

	@Override
	public void execute(Database database) throws CustomChangeException {
		 JdbcConnection connection = (JdbcConnection) database.getConnection();
		 try {
			 boolean isOracle = database.getConnection().getDatabaseProductName().toLowerCase().contains("oracle");

			 Set<String> addedUnits = new HashSet<>();
			 List<Pair<String, String>> spmnTypeUnits = getSpecimenTypeUnits(connection);
			 for (Pair<String, String> spmnTypeUnit : spmnTypeUnits) {
				 String unit = StringUtils.isBlank(spmnTypeUnit.second()) ? spmnTypeUnit.first() : spmnTypeUnit.second();
				 if (StringUtils.isBlank(unit) || !addedUnits.add(unit)) {
					 continue;
				 }

				 insertUnit(connection, isOracle ? INSERT_UNIT_PV_ORA : INSERT_UNIT_PV_MYSQL, unit);
			 }

			 logger.info("Added " + addedUnits.size() + " specimen units.");
		 } catch (Exception e) {
			 if (e instanceof CustomChangeException) {
				 throw (CustomChangeException) e;
			 }

			 throw new CustomChangeException("Error loading the specimen unit PVs", e);
		 }
	}

	@Override
	public String getConfirmationMessage() {
		return "Specimen units successfully loaded in the PV table";
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

	private List<Pair<String, String>> getSpecimenTypeUnits(JdbcConnection connection)
	throws CustomChangeException {
		try (
			PreparedStatement pstmt = connection.prepareStatement(GET_SPMN_TYPE_UNITS_SQL);
			ResultSet rs = pstmt.executeQuery();
		) {
			List<Pair<String, String>> result = new ArrayList<>();
			while (rs.next()) {
				result.add(Pair.make(rs.getString(1), rs.getString(2)));
			}

			return result;
		} catch (Exception e) {
			throw new CustomChangeException("Error reading the specimen type units: ", e);
		}
	}

	private void insertUnit(JdbcConnection connection, String sql, String unit)
	throws CustomChangeException {
		try (
			PreparedStatement pstmt = connection.prepareStatement(sql);
		) {
			pstmt.setString(1, unit);
			pstmt.executeUpdate();
		} catch (Exception e) {
			throw new CustomChangeException("Error inserting the specimen  unit: " + unit, e);
		}
	}

	private static final String GET_SPMN_TYPE_UNITS_SQL =
		"select " +
		"  distinct u.value as unit_name, s.value as unit_symbol " +
		"from " +
		"  os_pv_props u " +
		"  left join os_pv_props s " +
		"    on s.pv_id = u.pv_id and " +
		"    (" +
		"      (u.name = 'quantity_unit' and s.name = 'quantity_display_unit') or " +
		"      (u.name = 'concentration_unit' and s.name = 'concentration_display_unit')" +
		"    ) " +
		"where " +
		"  u.name in ('quantity_unit', 'concentration_unit')";

	private static final String INSERT_UNIT_PV_MYSQL =
		"insert into " +
		"  catissue_permissible_value (public_id, value, activity_status) " +
		"values " +
		"  ('specimen_unit', ?, 'Active')";

	private static final String INSERT_UNIT_PV_ORA =
		"insert into " +
		"  catissue_permissible_value (identifier, public_id, value, activity_status) " +
		"values " +
		"  (os_specimen_type_units_seq.nextval, 'specimen_unit', ?, 'Active')";
}
