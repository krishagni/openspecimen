package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;

public class ContainerSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return StorageContainer.getEntityName();
	}

	@Override
	protected String getQuery() {
		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessContainerSiteCps();
		if (CollectionUtils.isEmpty(siteCps)) {
			return null;
		}

		String joinCondition = "";
		List<String> clauses = new ArrayList<>();
		for (SiteCpPair siteCp : siteCps) {
			String clause;
			if (siteCp.getSiteId() != null) {
				clause = "s.identifier = " + siteCp.getSiteId();
			} else {
				clause = "s.institute_id = " + siteCp.getInstituteId();
			}

			if (siteCp.getCpId() != null) {
				if (StringUtils.isBlank(joinCondition)) {
					joinCondition = ALLOWED_CPS;
				}

				clause += " and (cp.cp_id is null or cp.cp_id = " + siteCp.getCpId() + ")";
			}

			clauses.add("(" + clause + ")");
		}

		return String.format(QUERY, joinCondition, String.join(" or ", clauses));
	}

	private static final String QUERY =
		"select " +
		"  distinct k.identifier, k.entity, k.entity_id, k.name, k.value " +
		"from " +
		"  os_search_entity_keywords k " +
		"  inner join os_storage_containers c on c.identifier = k.entity_id " +
		"  inner join catissue_site s on s.identifier = c.site_id " +
		"  %s " +
		"where " +
		"  k.value like ? and " +
		"  k.identifier > ? and " +
		"  k.entity = 'storage_container' and " +
		"  k.status = 1 and " +
		"  (%s) " +
		"order by " +
		"  k.identifier";

	private static final String ALLOWED_CPS =
		"left join os_stor_container_comp_cps cp on cp.storage_container_id = c.identifier";
}
