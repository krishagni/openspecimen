package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.SessionFactory;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.events.ContainerCriteria;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionStrategy;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.TransactionCache;
import com.krishagni.catissueplus.core.common.util.Utility;

@Configurable
public class LeastEmptyContainerSelectionStrategy implements ContainerSelectionStrategy {
	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private DaoFactory daoFactory;

	@Override
	public StorageContainer getContainer(ContainerCriteria criteria, Boolean aliquotsInSameContainer) {
		String criteriaKey = getCriteriaKey(criteria);
		List<Long> containerIds = getRecentlyUsedContainers().get(criteriaKey);
		if (containerIds != null) {
			int reqPositions = criteria.getRequiredPositions(aliquotsInSameContainer);
			for (Long containerId : containerIds) {
				StorageContainer container = getContainersCache().get(containerId);
				if (container == null) {
					container = daoFactory.getStorageContainerDao().getById(containerId);
					getContainersCache().put(containerId, container);
				}

				if (!container.isArchived() && container.hasFreePositionsForReservation(reqPositions)) {
					return container;
				}
			}
		}

		criteria.numContainers(5);
		containerIds = getLeastEmptyContainerIds(criteria, aliquotsInSameContainer);
		if (CollectionUtils.isEmpty(containerIds)) {
			return null;
		}

		List<Long> cachedContainerIds = getRecentlyUsedContainers().computeIfAbsent(criteriaKey, (k) -> new ArrayList<>());
		for (Long containerId : containerIds) {
			if (cachedContainerIds.contains(containerId)) {
				continue;
			}

			cachedContainerIds.add(containerId);
		}

		StorageContainer container = daoFactory.getStorageContainerDao().getById(containerIds.get(0));
		getContainersCache().put(container.getId(), container);
		return container;
	}

	private List<Long> getLeastEmptyContainerIds(ContainerCriteria crit, Boolean aliquotsInSameContainer) {
		sessionFactory.getCurrentSession().flush();

		String restrictions = getAccessRestrictions(crit);
		if (crit.rule() != null) {
			restrictions = "(" + restrictions + ") and (" + crit.rule().getSql("c", crit.ruleParams()) + ")";
		}

		String sql = String.format(GET_LEAST_EMPTY_CONTAINER_ID_SQL, restrictions);
		return sessionFactory.getCurrentSession().createNativeQuery(sql, Long.class)
			.addScalar("containerId", StandardBasicTypes.LONG)
			.setParameter("cpId", crit.specimen().getCpId())
			.setParameter("specimenClass", crit.specimen().getSpecimenClass())
			.setParameter("specimenType", crit.specimen().getType())
			.setParameter("minFreeLocs", crit.getRequiredPositions(aliquotsInSameContainer))
			.setMaxResults(crit.numContainers())
			.list();
	}

	private String getCriteriaKey(ContainerCriteria crit) {
		String key = crit.specimen().getCpId() + ":" +
			crit.specimen().getSpecimenClass() + ":" +
			crit.specimen().getType() + ":" +
			getAccessRestrictions(crit) + ":" +
			(crit.rule() != null ? crit.rule().getSql("c", crit.ruleParams()) : "");
		return Utility.getDigest(key);
	}

	private String getAccessRestrictions(ContainerCriteria crit) {
		String condition = "(c.site_id = %d and (allowed_cps.cp_id is null or allowed_cps.cp_id = %d))";
		return crit.siteCps().stream()
			.map(siteCp -> String.format(condition, siteCp.getSiteId(), siteCp.getCpId()))
			.collect(Collectors.joining(" or "));
	}

	//
	// key - hash of the container criteria
	// value - list of container IDs satisfying the criteria
	//
	private Map<String, List<Long>> getRecentlyUsedContainers() {
		return TransactionCache.getInstance().get("recentlyUsedContainers", new HashMap<>());
	}

	private Map<Long, StorageContainer> getContainersCache() {
		return TransactionCache.getInstance().get("containersCache", new HashMap<>());
	}

	private static final String GET_LEAST_EMPTY_CONTAINER_ID_SQL =
		"select " +
		"  c.identifier as containerId " +
		"from " +
		"  os_storage_containers c " +
		"  left join os_stor_container_comp_cps allowed_cps on allowed_cps.storage_container_id = c.identifier " +
		"where " +
		"  c.store_specimens = 1 and " +
		"  c.activity_status = 'Active' and " +
		"  (c.status is null or c.status != 'CHECKED_OUT') and " +
		"  ( " +
		"    allowed_cps.cp_id = :cpId or " +
		"    ( " +
		"      allowed_cps.cp_id is null and " +
		"	   ( " +
		"	     select " +
		"	       count(*) " +
		"        from " +
		"          catissue_site_cp cp_site " +
		"        where " +
		"          cp_site.site_id = c.site_id and " +
		"          cp_site.collection_protocol_id = :cpId " +
		"      ) > 0 " +
		"    ) " +
		"  ) and " +
		"  ( " +
		"    ( " +
		"      select " +
		"        count(*) " +
		"      from " +
		"        os_stor_cont_comp_spec_classes cc " +
		"        inner join catissue_permissible_value pcc on pcc.identifier = cc.specimen_class_id " +
		"      where " +
		"        pcc.value = :specimenClass and " +
		"        storage_container_id = c.identifier " +
		"    ) > 0 " +
		"    or " +
		"    ( " +
		"	   select " +
		"	     count(*) " +
		"      from " +
		"        os_stor_cont_comp_spec_types ct " +
		"        inner join catissue_permissible_value pct on pct.identifier = ct.specimen_type_id " +
		"	   where " +
		"        pct.value = :specimenType and " +
		"        storage_container_id = c.identifier " +
		"    ) > 0 " +
		"  ) and " +
		"  (c.free_spaces > (:minFreeLocs - 1)) and " +
		"  (%s) " +
		"order by " +
		"  c.free_spaces, c.identifier ";
}