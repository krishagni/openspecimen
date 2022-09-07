package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;

public class DistributionProtocolSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return DistributionProtocol.getEntityName();
	}

	@Override
	protected String getQuery() {
		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessDistributionProtocolSites();
		if (CollectionUtils.isEmpty(siteCps)) {
			return null;
		}

		List<String> clauses = new ArrayList<>();
		for (SiteCpPair siteCp : siteCps) {
			String clause = null;
			if (siteCp.getSiteId() == null) {
				clause = String.format(INSTITUTE_CLAUSE, siteCp.getInstituteId());
			} else {
				clause = String.format(SITE_CLAUSE, siteCp.getSiteId(), siteCp.getInstituteId());
			}

			clauses.add(clause);
		}

		return String.format(QUERY, String.join(" or ", clauses));
	}

	private static final String QUERY =
		"select " +
		"  distinct k.identifier, k.entity, k.entity_id, k.name, k.value " +
		"from " +
		"  os_search_entity_keywords k " +
		"  inner join os_distribution_protocol_sites dp_site on dp_site.distribution_protocol_id = k.entity_id " +
		"where " +
		"  k.value like ? and " +
		"  k.identifier > ? and " +
		"  k.entity = 'distribution_protocol' and " +
		"  k.status = 1 and " +
		"  (%s) " +
		"order by " +
		"  k.identifier";

	private static final String INSTITUTE_CLAUSE = "(dp_site.institute_id = %d)";

	private static final String SITE_CLAUSE =
		"((dp_site.site_id is not null and dp_site.site_id = %d) or (dp_site.site_id is null and dp_site.institute_id = %d))";
}
