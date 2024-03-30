package com.krishagni.catissueplus.core.biospecimen;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenTypeUnit;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;

@Configurable
public class SpecimenUtil {
	private static SpecimenUtil instance = null;

	@Autowired
	private DaoFactory daoFactory;

	public static SpecimenUtil getInstance() {
		if (instance == null || instance.daoFactory == null) {
			instance = new SpecimenUtil();
		}

		return instance;
	}

	public SpecimenTypeUnit getUnit(CollectionProtocol cp, PermissibleValue specimenClass, PermissibleValue type) {
		if (cp == null || specimenClass == null || type == null) {
			return null;
		}

		List<SpecimenTypeUnit> units = daoFactory.getSpecimenTypeUnitDao().getMatchingUnits(
			cp.getId(),
			specimenClass.getId(),
			type.getId()
		);

		SpecimenTypeUnit result = new SpecimenTypeUnit();
		result.setCp(cp);
		result.setSpecimenClass(specimenClass);
		result.setType(type);

		boolean qtyInit = false, concInit = false;
		for (SpecimenTypeUnit unit : units) {
			if (!qtyInit && unit.getQuantityUnit() != null) {
				result.setQuantityUnit(unit.getQuantityUnit());
				qtyInit = true;
			}

			if (!concInit && unit.getConcentrationUnit() != null) {
				result.setConcentrationUnit(unit.getConcentrationUnit());
				concInit = true;
			}

			if (qtyInit && concInit) {
				break;
			}
		}

		return result;
	}
}
