package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class SpecimenTypeAbbrUniqueIdLabelToken extends AbstractUniqueIdToken<Specimen> {

	private static List<String> ALLOWED_TYPES = Arrays.asList("registration", "visit", "parent_specimen", "primary_specimen");

	private static final String ABBREVIATION = "abbreviation";

	@Autowired
	private DaoFactory daoFactory;

	public SpecimenTypeAbbrUniqueIdLabelToken() {
		this.name = "SP_TYPE_ABBR_UID";
	}

	public boolean areArgsValid(String... args) {
		if (!super.areArgsValid(args)) {
			return false;
		}

		String arg = getArg(1, args);
		if (StringUtils.isNotBlank(arg) && !arg.trim().equals("output_one") && !arg.trim().equals("default")) {
			return false;
		}

		arg = getArg(2, args);
		return StringUtils.isBlank(arg) || ALLOWED_TYPES.contains(arg.trim());
	}

	@Override
	public Number getUniqueId(Specimen specimen, String... args) {
		boolean useLabels = specimen.getCollectionProtocol().useLabelsAsSequenceKey();
		String groupId = null;
		String keyType;

		String arg = getArg(2, args);
		if (StringUtils.isBlank(arg)) {
			arg = "visit";
		}

		arg = arg.trim();
		switch (arg) {
			case "registration":
				keyType = "CPR_" + getTypeKey();
				if (useLabels) {
					groupId = specimen.getCpId() + "_" + specimen.getRegistration().getPpid();
				} else {
					groupId = specimen.getRegistration().getId().toString();
				}
				break;

			case "parent_specimen":
				keyType = "PARENT_SPMN_" + getTypeKey();
				if (specimen.getParentSpecimen() != null) {
					if (useLabels) {
						groupId = specimen.getCpId() + "_" + specimen.getParentSpecimen().getLabel();
					} else {
						groupId = specimen.getParentSpecimen().getId().toString();
					}
				} else {
					groupId = useLabels ? specimen.getLabel() : specimen.getId().toString();
				}
				break;

			case "primary_specimen":
				keyType = "PRIMARY_SPMN_" + getTypeKey();
				if (useLabels) {
					groupId = specimen.getCpId() + "_" + specimen.getPrimarySpecimen().getLabel();
				} else {
					groupId = specimen.getPrimarySpecimen().getId().toString();
				}
				break;

			case "visit":
			default:
				keyType = "VISIT_" + getTypeKey();
				if (useLabels) {
					groupId = specimen.getCpId() + "_" + specimen.getVisit().getName();
				} else {
					groupId = specimen.getVisit().getId().toString();
				}
				break;
		}

		String typeAbbr = getTypeAbbr(specimen);
		Long uniqueId = daoFactory.getUniqueIdGenerator().getUniqueId(keyType, groupId + "_" + typeAbbr);
		return uniqueId == 1L && !eqArg("output_one", 1, args) ? -1 : uniqueId;
	}

	private String getTypeAbbr(Specimen spmn) {
		String abbr = spmn.getSpecimenType().getProps().get(ABBREVIATION);
		return StringUtils.isBlank(abbr) ? StringUtils.EMPTY : abbr;
	}
}
