package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.envers.Audited;

import com.krishagni.catissueplus.core.common.domain.ReportSettings;

@Audited
public class CpReportSettings extends ReportSettings {
	private CollectionProtocol cp;

	public CollectionProtocol getCp() {
		return cp;
	}

	public void setCp(CollectionProtocol cp) {
		this.cp = cp;
	}

	protected Set<String> getAuditStringInclusionProps() {
		return Arrays.stream(new String[] {"id", "cp", "enabled", "dataQuery", "configJson", "emailTmpl", "recipients", "activityStatus" })
			.collect(Collectors.toSet());
	}
}
