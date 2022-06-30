package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.envers.Audited;

import com.krishagni.catissueplus.core.biospecimen.domain.ConsentTier;

@Audited
public class DpConsentTier extends ConsentTier {
	private DistributionProtocol distributionProtocol;

	public DistributionProtocol getDistributionProtocol() {
		return distributionProtocol;
	}

	public void setDistributionProtocol(DistributionProtocol distributionProtocol) {
		this.distributionProtocol = distributionProtocol;
	}

	@Override
	protected Set<String> getAuditStringInclusionProps() {
		return Arrays.stream(new String[] { "id", "distributionProtocol", "statement", "activityStatus" })
			.collect(Collectors.toSet());
	}
}
