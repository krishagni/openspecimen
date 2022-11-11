package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;

public class ParentSpecimenCounterLabelToken extends AbstractSpecimenLabelToken {

	@Autowired
	private DaoFactory daoFactory;

	private String type;

	public ParentSpecimenCounterLabelToken() {
		this.name = "PSPEC_COUNTER";
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public boolean areArgsValid(String ...args) {
		if (args == null || args.length == 0) {
			return true;
		} else if (args.length != 1 || StringUtils.isBlank(args[0])) {
			return false;
		} else {
			try {
				Integer.parseInt(args[0]);
			} catch (NumberFormatException nfe) {
				return false;
			}

			return true;
		}
	}

	public String getLabelN(Specimen specimen, String ...args) {
		int fixedDigits = 0;
		if (args != null && args.length > 0 && args[0] != null) {
			fixedDigits = Integer.parseInt(args[0]);
		}

		return getLabel0(specimen, fixedDigits);
	}

	@Override
	public String getLabel(Specimen specimen) {
		return getLabel0(specimen, 0);
	}

	private String getLabel0(Specimen specimen, int fixedDigits) {
		if (specimen.getParentSpecimen() == null) {
			return null;
		}

		String parentLabel = specimen.getParentSpecimen().getLabel();
		Matcher matcher = LAST_DIGIT_PATTERN.matcher(parentLabel);

		String counter = "0";
		int matchIdx = parentLabel.length();
		if (matcher.find()) {
			counter = matcher.group(0);
			matchIdx = matcher.start(0);

			if (fixedDigits > 0) {
				if (counter.length() > fixedDigits) {
					counter = counter.substring(fixedDigits);
					matchIdx += fixedDigits;
				} else {
					counter = "0";
					matchIdx = parentLabel.length();
				}
			}
		}

		String pidStr = null;
		if (specimen.getCollectionProtocol().useLabelsAsSequenceKey()) {
			pidStr = specimen.getCpId() + "_" + specimen.getParentSpecimen().getLabel();
		} else {
			pidStr = specimen.getParentSpecimen().getId().toString();
		}

		String uniqueId = daoFactory.getUniqueIdGenerator().getUniqueId(getTypeKey(), pidStr, Long.parseLong(counter)).toString();
		if (uniqueId.length() < counter.length()) {
			uniqueId = StringUtils.leftPad(uniqueId, counter.length(), "0");
		}

		return parentLabel.substring(0, matchIdx) + uniqueId;
	}

	private String getTypeKey() {
		if (StringUtils.isBlank(type)) {
			return name;
		}

		return type + "_" + name;
	}

	private final static Pattern LAST_DIGIT_PATTERN = Pattern.compile("([0-9]+)$");
}
