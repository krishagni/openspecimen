package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.events.ContainerCriteria;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionStrategy;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.TransactionCache;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
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
	
	@SuppressWarnings("unchecked")
	private List<Long> getLeastEmptyContainerIds(ContainerCriteria crit, Boolean aliquotsInSameContainer) {
		sessionFactory.getCurrentSession().flush();

		String sql = sessionFactory.getCurrentSession().createNamedQuery(GET_LEAST_EMPTY_CONTAINER_ID).getQueryString();
		int orderByIdx = sql.indexOf("order by");
		String beforeOrderBySql = sql.substring(0, orderByIdx);
		String orderByLaterSql  = sql.substring(orderByIdx);
		sql = beforeOrderBySql;

		sql += " and (" + getAccessRestrictions(crit) + ") ";
		if (crit.rule() != null) {
			sql += " and (" + crit.rule().getSql("c", crit.ruleParams()) + ") ";
		}

		sql += orderByLaterSql;
		return (List<Long>) sessionFactory.getCurrentSession().createNativeQuery(sql)
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
		List<String> accessRestrictions = new ArrayList<>();

		for (SiteCpPair siteCp : crit.siteCps()) {
			accessRestrictions.add("(c.site_id = " + siteCp.getSiteId() +
				" and " +
				"(allowed_cps.cp_id is null or allowed_cps.cp_id = " + siteCp.getCpId() + ")" +
				")"
			);
		}

		return StringUtils.join(accessRestrictions, " or ");
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

	private static final String GET_LEAST_EMPTY_CONTAINER_ID = StorageContainer.class.getName() + ".getLeastEmptyContainerId";
}