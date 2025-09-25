package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
}
