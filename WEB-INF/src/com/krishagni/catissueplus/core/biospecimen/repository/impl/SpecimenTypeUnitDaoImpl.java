package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.MutationQuery;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenTypeUnit;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenTypeUnitDao;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenTypeUnitsListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.util.Status;

public class SpecimenTypeUnitDaoImpl extends AbstractDao<SpecimenTypeUnit> implements SpecimenTypeUnitDao {
	@Override
	public Class<SpecimenTypeUnit> getType() {
		return SpecimenTypeUnit.class;
	}

	@Override
	public List<SpecimenTypeUnit> getUnits(SpecimenTypeUnitsListCriteria crit) {
		Criteria<SpecimenTypeUnit> query = getQuery(crit);
		return query.addOrder(crit.asc() ? query.asc("unit.id") : query.desc("unit.id"))
			.list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Long getUnitsCount(SpecimenTypeUnitsListCriteria crit) {
		return getQuery(crit).getCount("unit.id");
	}

	@Override
	public SpecimenTypeUnit getUnit(Long cpId, Long specimenClassId, Long typeId) {
		Criteria<SpecimenTypeUnit> query = createCriteria(SpecimenTypeUnit.class, "unit")
			.leftJoin("unit.cp", "cp")
			.leftJoin("unit.specimenClass", "specimenClass")
			.leftJoin("unit.type", "type");

		return query.add(cpId == null ? query.isNull("cp.id") : query.eq("cp.id", cpId))
			.add(query.eq("specimenClass.id", specimenClassId))
			.add(typeId == null ? query.isNull("type.id") : query.eq("type.id", typeId))
			.uniqueResult();
	}

	@Override
	public SpecimenTypeUnit getUnit(Long cpId, String cpShortTitle, String specimenClass, String type) {
		Criteria<SpecimenTypeUnit> query = createCriteria(SpecimenTypeUnit.class, "unit")
			.leftJoin("unit.cp", "cp")
			.leftJoin("unit.specimenClass", "specimenClass")
			.leftJoin("unit.type", "type");

		if ((cpId == null || cpId == -1L) && StringUtils.isBlank(cpShortTitle)) {
			query.add(query.isNull("cp.id"));
		} else if (cpId != null && cpId > 0L) {
			query.add(query.eq("cp.id", cpId));
		} else if (StringUtils.isNotBlank(cpShortTitle)) {
			query.add(query.eq("cp.shortTitle", cpShortTitle));
		}

		query.add(query.eq("specimenClass.value", specimenClass));

		if (StringUtils.isBlank(type)) {
			query.add(query.isNull("type.id"));
		} else {
			query.add(query.eq("type.value", type));
		}

		List<SpecimenTypeUnit> units = query.list();
		return units.isEmpty() ? null : units.get(0);
	}

	@Override
	public List<SpecimenTypeUnit> getMatchingUnits(Long cpId, Long specimenClassId, Long typeId) {
		Criteria<SpecimenTypeUnit> query = createCriteria(SpecimenTypeUnit.class, "unit")
			.leftJoin("unit.cp", "cp")
			.leftJoin("unit.specimenClass", "specimenClass")
			.leftJoin("unit.type", "type");

		List<SpecimenTypeUnit> units = query.add(query.or(query.isNull("cp.id"), query.eq("cp.id", cpId)))
			.add(query.eq("specimenClass.id", specimenClassId))
			.add(query.or(query.isNull("type.id"), query.eq("type.id", typeId)))
			.list();

		units.sort((u1, u2) -> rank(u1) - rank(u2));
		return units;
	}

	@Override
	public List<SpecimenTypeUnit> getMatchingUnits(String cpShortTitle, String specimenClass, String type) {
		Criteria<SpecimenTypeUnit> query = createCriteria(SpecimenTypeUnit.class, "unit")
			.leftJoin("unit.cp", "cp")
			.leftJoin("unit.specimenClass", "specimenClass")
			.leftJoin("unit.type", "type");

		List<SpecimenTypeUnit> units = query.add(query.or(query.isNull("cp.id"), query.eq("cp.shortTitle", cpShortTitle)))
			.add(query.eq("specimenClass.value", specimenClass))
			.add(query.or(query.isNull("type.value"), query.eq("type.value", type)))
			.list();
		units.sort((u1, u2) -> rank(u1) - rank(u2));
		return units;
	}

	@Override
	public void refreshQueryUnits(Long cpId, Long specimenClassId, Long typeId) {
		deleteQuerySpecimenUnits(cpId, specimenClassId, typeId);
		insertQuerySpecimenUnits(cpId, specimenClassId, typeId);
	}

	@Override
	public void addQueryUnitsForCp(Long cpId) {
		String sql = getInsertQueryUnitsSql("", getCpWhereFilter(cpId));
		getCurrentSession().createNativeMutationQuery(sql)
			.setParameter("cpId", cpId)
			.executeUpdate();
	}

	@Override
	public void addQueryUnitsForSpecimenType(Long specimenClassId, Long typeId) {
		insertQuerySpecimenUnits(null, specimenClassId, typeId);
	}

	@Override
	public int deleteQueryUnits() {
		return getCurrentSession().createNativeMutationQuery(DELETE_ALL_QUERY_UNITS_SQL).executeUpdate();
	}

	@Override
	public int addQueryUnits() {
		String sql = getInsertQueryUnitsSql("", "1 = 1");
		return getCurrentSession().createNativeMutationQuery(sql).executeUpdate();
	}

	private Criteria<SpecimenTypeUnit> getQuery(SpecimenTypeUnitsListCriteria crit) {
		Criteria<SpecimenTypeUnit> query = createCriteria(SpecimenTypeUnit.class, "unit")
			.leftJoin("unit.cp", "cp")
			.leftJoin("unit.specimenClass", "specimenClass")
			.leftJoin("unit.type", "type");

		if (crit.cpId() != null || StringUtils.isNotBlank(crit.cpShortTitle())) {
			if (crit.cpId() != null) {
				query.add(query.eq("cp.id", crit.cpId()));
			} else if (crit.cpShortTitle().equals("allCps")) {
				query.add(query.isNull("cp.id"));
			} else {
				query.add(query.eq("cp.shortTitle", crit.cpShortTitle()));
			}
		} else {
			query.add(
				query.or(
					query.isNull("cp.id"),
					query.ne("cp.activityStatus", Status.ACTIVITY_STATUS_DISABLED.getStatus())
				)
			);
		}

		if (StringUtils.isNotBlank(crit.specimenClass())) {
			query.add(query.eq("specimenClass.value", crit.specimenClass()));
		}

		if (StringUtils.isNotBlank(crit.type())) {
			query.add(query.eq("type.value", crit.type()));
		}

		if (crit.lastId() != null) {
			if (crit.asc()) {
				query.add(query.gt("unit.id", crit.lastId()));
			} else {
				query.add(query.lt("unit.id", crit.lastId()));
			}
		}

		return query;
	}

	private int rank(SpecimenTypeUnit unit) {
		if (unit.getCp() != null && unit.getType() != null) {
			return 0;
		} else if (unit.getCp() != null) {
			return 1;
		} else if (unit.getType() != null) {
			return 2;
		} else {
			return 3;
		}
	}

	private void deleteQuerySpecimenUnits(Long cpId, Long specimenClassId, Long typeId) {
		MutationQuery query = getCurrentSession().createNativeMutationQuery(
			String.format(
				DELETE_QUERY_SPMN_UNITS_SQL,
				getCpColumnFilter("cp_id", cpId),
				getTypeColumnFilter("type_id", typeId)
			)
		).setParameter("specimenClassId", specimenClassId);

		setScopeParameters(query, cpId, typeId);
		query.executeUpdate();
	}

	private void insertQuerySpecimenUnits(Long cpId, Long specimenClassId, Long typeId) {
		MutationQuery query = getCurrentSession().createNativeMutationQuery(
			getInsertQueryUnitsSql(getSpecimenTypeJoinFilter(specimenClassId, typeId), getCpWhereFilter(cpId))
		).setParameter("specimenClassId", specimenClassId);

		setScopeParameters(query, cpId, typeId);
		query.executeUpdate();
	}

	private String getCpColumnFilter(String column, Long cpId) {
		return cpId != null ? "and " + column + " = :cpId" : "";
	}

	private String getCpWhereFilter(Long cpId) {
		return cpId != null ? "cp.identifier = :cpId" : "1 = 1";
	}

	private String getTypeColumnFilter(String column, Long typeId) {
		return typeId != null ? "and " + column + " = :typeId" : "";
	}

	private String getSpecimenTypeJoinFilter(Long specimenClassId, Long typeId) {
		return "and st.parent_identifier = :specimenClassId " + getTypeColumnFilter("st.identifier", typeId);
	}

	private String getInsertQueryUnitsSql(String specimenTypeJoinFilter, String cpWhereFilter) {
		return String.format(INSERT_QUERY_UNITS_SQL, specimenTypeJoinFilter, cpWhereFilter);
	}

	private void setScopeParameters(MutationQuery query, Long cpId, Long typeId) {
		if (cpId != null) {
			query.setParameter("cpId", cpId);
		}

		if (typeId != null) {
			query.setParameter("typeId", typeId);
		}
	}

	private static final String DELETE_QUERY_SPMN_UNITS_SQL =
		"delete from " +
		"  os_query_spmn_units " +
		"where " +
		"  class_id = :specimenClassId " +
		"  %s " +
		"  %s";

	private static final String DELETE_ALL_QUERY_UNITS_SQL =
		"delete from os_query_spmn_units";

	private static final String INSERT_QUERY_UNITS_SQL =
		"insert into os_query_spmn_units (cp_id, class_id, type_id, unit_key, quantity_unit_id, conc_unit_id) " +
		"select " +
		"  cp.identifier, " +
		"  st.parent_identifier, " +
		"  st.identifier, " +
		"  concat(cp.identifier, concat(':', concat(st.parent_identifier, concat(':', st.identifier)))), " +
		"  coalesce(cp_type.quantity_unit_id, cp_class.quantity_unit_id, sys_type.quantity_unit_id, sys_class.quantity_unit_id), " +
		"  coalesce(cp_type.concentration_unit_id, cp_class.concentration_unit_id, sys_type.concentration_unit_id, sys_class.concentration_unit_id) " +
		"from " +
		"  catissue_collection_protocol cp " +
		"  inner join catissue_permissible_value st on " +
		"    st.public_id = 'specimen_type' and " +
		"    st.parent_identifier is not null " +
		"    %s " +
		"  left join os_specimen_type_units cp_type on " +
		"    cp_type.cp_id = cp.identifier and " +
		"    cp_type.specimen_class_id = st.parent_identifier and " +
		"    cp_type.specimen_type_id = st.identifier " +
		"  left join os_specimen_type_units cp_class on " +
		"    cp_class.cp_id = cp.identifier and " +
		"    cp_class.specimen_class_id = st.parent_identifier and " +
		"    cp_class.specimen_type_id is null " +
		"  left join os_specimen_type_units sys_type on " +
		"    sys_type.cp_id is null and " +
		"    sys_type.specimen_class_id = st.parent_identifier and " +
		"    sys_type.specimen_type_id = st.identifier " +
		"  left join os_specimen_type_units sys_class on " +
		"    sys_class.cp_id is null and " +
		"    sys_class.specimen_class_id = st.parent_identifier and " +
		"    sys_class.specimen_type_id is null " +
		"where " +
		"  %s";
}
