package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenTypeUnit;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenTypeUnitDao;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenTypeUnitsListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class SpecimenTypeUnitDaoImpl extends AbstractDao<SpecimenTypeUnit> implements SpecimenTypeUnitDao {
	@Override
	public Class<SpecimenTypeUnit> getType() {
		return SpecimenTypeUnit.class;
	}

	@Override
	public List<SpecimenTypeUnit> getUnits(SpecimenTypeUnitsListCriteria crit) {
		Criteria<SpecimenTypeUnit> query = getQuery(crit);
		return query.addOrder(query.desc("unit.id")).list(crit.startAt(), crit.maxResults());
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

	private Criteria<SpecimenTypeUnit> getQuery(SpecimenTypeUnitsListCriteria crit) {
		Criteria<SpecimenTypeUnit> query = createCriteria(SpecimenTypeUnit.class, "unit")
			.leftJoin("unit.cp", "cp")
			.leftJoin("unit.specimenClass", "specimenClass")
			.leftJoin("unit.type", "type");

		if (StringUtils.isNotBlank(crit.cpShortTitle())) {
			if (crit.cpShortTitle().equals("allCps")) {
				query.add(query.isNull("cp.id"));
			} else {
				query.add(query.eq("cp.shortTitle", crit.cpShortTitle()));
			}
		}

		if (StringUtils.isNotBlank(crit.specimenClass())) {
			query.add(query.eq("specimenClass.value", crit.specimenClass()));
		}

		if (StringUtils.isNotBlank(crit.type())) {
			query.add(query.eq("type.value", crit.type()));
		}

		return query;
	}
}
