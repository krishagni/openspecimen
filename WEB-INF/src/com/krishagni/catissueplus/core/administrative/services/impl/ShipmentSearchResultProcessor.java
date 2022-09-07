package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.krishagni.catissueplus.core.administrative.domain.Shipment;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;

public class ShipmentSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return Shipment.getEntityName();
	}

	@Override
	protected String getQuery() {
		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessShipmentSites();
		if (CollectionUtils.isEmpty(siteCps)) {
			return null;
		}

		List<String> clauses = new ArrayList<>();
		for (SiteCpPair siteCp : siteCps) {
			String clause = null;
			if (siteCp.getSiteId() == null) {
				clause = String.format(INSTITUTE_CLAUSE, siteCp.getInstituteId(), siteCp.getInstituteId());
			} else {
				clause = String.format(SITE_CLAUSE, siteCp.getSiteId(), siteCp.getSiteId());
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
		"  inner join os_shipments s on s.identifier = k.entity_id " +
		"  left join catissue_site ss on ss.identifier = s.sending_site_id " +
		"  left join catissue_site rs on rs.identifier = s.receiving_site_id " +
		"where " +
		"  k.value like ? and " +
		"  k.identifier > ? and " +
		"  k.entity = 'shipment' and " +
		"  k.status = 1 and " +
		"  (%s) " +
		"order by " +
		"  k.identifier";

	private static final String INSTITUTE_CLAUSE =
		"((s.status != 'PENDING' and rs.institute_id = %d) or ss.institute_id = %d)";

	private static final String SITE_CLAUSE =
		"((s.status != 'PENDING' and rs.identifier = %d) or ss.identifier = %d)";
}
