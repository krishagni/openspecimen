package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;

public class VisitSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return Visit.getEntityName();
	}

	@Override
	protected String getQuery() {
		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps(null, false);
		if (CollectionUtils.isEmpty(siteCps)) {
			return null;
		}

		boolean useMrnSites = AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn();
		String joinCondition = useMrnSites ? PMI_JOIN_COND : "";
		String cpSiteClause  = getCpSiteClause("s",  siteCps);

		String whereClause;
		if (useMrnSites) {
			String pmiSiteClause = getCpSiteClause("ps", siteCps);
			whereClause = String.format(PMI_WHERE_COND, pmiSiteClause, cpSiteClause);
		} else {
			whereClause = cpSiteClause;
		}

		return String.format(QUERY, joinCondition, whereClause);
	}

	@Override
	protected String getEntityPropsQuery() {
		return ENTITY_PROPS_QUERY;
	}

	private String getCpSiteClause(String siteAlias, Collection<SiteCpPair> siteCps) {
		List<String> clauses = new ArrayList<>();
		for (SiteCpPair siteCp : siteCps) {
			String clause;
			if (siteCp.getSiteId() != null) {
				clause = siteAlias + ".identifier = " + siteCp.getSiteId();
			} else {
				clause = siteAlias + ".institute_id = " + siteCp.getInstituteId();
			}

			if (siteCp.getCpId() != null) {
				clause += " and cpr.collection_protocol_id = " + siteCp.getCpId();
			}

			clauses.add("(" + clause + ")");
		}

		return "(" + String.join(" or ", clauses) + ")";
	}

	private static final String QUERY =
		"select " +
		"  distinct k.identifier, k.entity, k.entity_id, k.name, k.value " +
		"from " +
		"  os_search_entity_keywords k " +
		"  inner join catissue_specimen_coll_group v on v.identifier = k.entity_id " +
		"  inner join catissue_coll_prot_reg cpr on cpr.identifier = v.collection_protocol_reg_id " +
		"  inner join catissue_site_cp cp_site on cp_site.collection_protocol_id = cpr.collection_protocol_id " +
		"  inner join catissue_site s on s.identifier = cp_site.site_id " +
		"  %s " +
		"where " +
		"  k.value like ? and " +
		"  k.identifier > ? and " +
		"  k.entity = 'visit' and " +
		"  k.status = 1 and " +
		"  v.activity_status != 'Disabled' and " +
		"  (%s) " +
		"order by " +
		"  k.identifier";

	private static final String PMI_JOIN_COND =
		"inner join catissue_participant p on p.identifier = cpr.participant_id " +
		"left join catissue_part_medical_id pmi on pmi.participant_id = p.identifier " +
		"left join catissue_site ps on ps.identifier = pmi.site_id ";

	private static final String PMI_WHERE_COND =
		"((ps.identifier is not null and %s) or (ps.identifier is null and %s))";

	private static final String ENTITY_PROPS_QUERY =
		"select " +
		"  visit.identifier as entityId, 'collection_protocol' as name, cp.short_title as value " +
		"from " +
		"  catissue_specimen_coll_group visit " +
		"  inner join catissue_coll_prot_reg cpr on cpr.identifier = visit.collection_protocol_reg_id " +
		"  inner join catissue_collection_protocol cp on cp.identifier = cpr.collection_protocol_id " +
		"where " +
		"  visit.identifier in (:entityIds)";
}