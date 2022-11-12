package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class VisitSpecTypeLabelToken extends AbstractUniqueIdToken<Specimen> {

	@Autowired
	private DaoFactory daoFactory;

	public VisitSpecTypeLabelToken() {
		this.name = "VISIT_SP_TYPE_UID";
	}

	public boolean areArgsValid(String... args) {
		if (!super.areArgsValid(args)) {
			return false;
		}

		String arg = getArg(1, args);
		return StringUtils.isBlank(arg) || arg.trim().equals("output_one");
	}

	@Override
	public Number getUniqueId(Specimen specimen, String... args) {
		String visitName = specimen.getVisit().getName();
		String key = visitName + "_" + specimen.getSpecimenType().getId().toString();
		Long uniqueId = daoFactory.getUniqueIdGenerator().getUniqueId(getTypeKey(), key);
		return uniqueId == 1L && !eqArg("output_one", 1, args) ? -1 : uniqueId;
	}
}
