package com.krishagni.catissueplus.core.biospecimen.label.cpr;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;

public class CpCodePpidToken extends AbstractPpidToken {

	public CpCodePpidToken() {
		this.name = "CP_CODE";
	}
	
	@Override
	public String getLabel(CollectionProtocolRegistration cpr, String... args) {
		String code = cpr.getCollectionProtocol().getCode();
		if (StringUtils.isBlank(code)) {
			code = StringUtils.EMPTY;
		}

		return code;
	}

}
