
package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.PositionAssigner;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerSummary;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.administrative.repository.ContainerRestrictionsCriteria;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerDao;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolSite;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.BiospecimenDaoHelper;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class StorageContainerDaoImpl extends AbstractDao<StorageContainer> implements StorageContainerDao {

	@Override
	public Class<?> getType() {
		return StorageContainer.class;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<StorageContainer> getStorageContainers(StorageContainerListCriteria listCrit) {
		return new ListQueryBuilder(listCrit).query()
				.setFirstResult(listCrit.startAt())
				.setMaxResults(listCrit.maxResults())
				.list();
	}
	
	@Override
	public Long getStorageContainersCount(StorageContainerListCriteria listCrit) {
		return ((Number) new ListQueryBuilder(listCrit, true).query().uniqueResult()).longValue();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public StorageContainer getByName(String name) {
		List<StorageContainer> result = sessionFactory.getCurrentSession()
				.createCriteria(StorageContainer.class)
				.add(Restrictions.eq("name", name).ignoreCase())
				.add(Restrictions.eq("activityStatus", Status.ACTIVITY_STATUS_ACTIVE.getStatus()))
				.list();

		return result.isEmpty() ? null : result.iterator().next();
	}
	
	@SuppressWarnings("unchecked")
	public StorageContainer getByBarcode(String barcode) {		
		List<StorageContainer> result = sessionFactory.getCurrentSession()
				.createCriteria(StorageContainer.class)
				.add(Restrictions.eq("barcode", barcode).ignoreCase())
				.add(Restrictions.eq("activityStatus", Status.ACTIVITY_STATUS_ACTIVE.getStatus()))
				.list();

		return result.isEmpty() ? null : result.iterator().next();		
	}

	@Override
	public void delete(StorageContainerPosition position) {
		sessionFactory.getCurrentSession().delete(position);		
	}

	@Override
	public Map<String, Object> getContainerIds(String key, Object value) {
		return getObjectIds("containerId", key, value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getNonCompliantContainers(ContainerRestrictionsCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(StorageContainer.class)
			.createAlias("descendentContainers", "d")
			.add(Restrictions.eq("id", crit.containerId()))
			.setProjection(Projections.distinct(Projections.property("d.name")));

		Disjunction restriction = Restrictions.disjunction();
		if (isNotEmpty(crit.specimenClasses())) {
			query.createAlias("d.compAllowedSpecimenClasses", "spmnClass", JoinType.LEFT_OUTER_JOIN);
			restriction.add(
				Restrictions.conjunction()
					.add(Restrictions.isNotNull("spmnClass.id"))
					.add(Restrictions.not(Restrictions.in("spmnClass.id", getPvIds(crit.specimenClasses()))))
			);
		}

		query.createAlias("d.compAllowedSpecimenTypes", "spmnType", JoinType.LEFT_OUTER_JOIN);
		Junction notInTypes = Restrictions.conjunction();
		if (isNotEmpty(crit.specimenTypes())) {
			notInTypes.add(Restrictions.not(Restrictions.in("spmnType.id", getPvIds(crit.specimenTypes()))));
		}

		if (isNotEmpty(crit.specimenClasses())) {
			DetachedCriteria typesCrit = DetachedCriteria.forClass(PermissibleValue.class, "pv")
				.createAlias("pv.parent", "ppv")
				.add(Restrictions.eq("pv.attribute", "specimen_type"))
				.add(Restrictions.in("ppv.id", getPvIds(crit.specimenClasses())))
				.setProjection(Property.forName("pv.id"));
			notInTypes.add(Subqueries.propertyNotIn("spmnType.id", typesCrit));
		}

		restriction.add(
			Restrictions.conjunction()
				.add(Restrictions.isNotNull("spmnType.id"))
				.add(notInTypes)
		);

		if (isNotEmpty(crit.collectionProtocols())) {
			query.createAlias("d.compAllowedCps", "cp", JoinType.LEFT_OUTER_JOIN);
			restriction.add(
				Restrictions.conjunction()
					.add(Restrictions.isNotNull("cp.id"))
					.add(Restrictions.not(Restrictions.in("cp.id", crit.collectionProtocolIds())))
			);
		} else {
			DetachedCriteria siteCrit = DetachedCriteria.forClass(CollectionProtocolSite.class)
				.createAlias("collectionProtocol", "cp1")
				.createAlias("site", "site1")
				.add(Restrictions.eqProperty("cp1.id", "cp.id"))
				.setProjection(Property.forName("site1.id"));

			query.createAlias("d.compAllowedCps", "cp", JoinType.LEFT_OUTER_JOIN);
			restriction.add(
				Restrictions.conjunction()
					.add(Restrictions.isNotNull("cp.id"))
					.add(Subqueries.notIn(crit.siteId(), siteCrit))
			);
		}

		if (isNotEmpty(crit.distributionProtocols())) {
			query.createAlias("d.compAllowedDps", "dp", JoinType.LEFT_OUTER_JOIN);
			restriction.add(
				Restrictions.conjunction()
					.add(Restrictions.isNotNull("dp.id"))
					.add(Restrictions.not(Restrictions.in("dp.id", crit.distributionProtocolIds())))
			);
		}

		flush();
		return query.add(restriction).list();
	}

	@SuppressWarnings(value = "unchecked")
	@Override
	public List<String> getNonCompliantSpecimens(ContainerRestrictionsCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(StorageContainer.class)
			.createAlias("descendentContainers", "d")
			.createAlias("d.occupiedPositions", "pos")
			.createAlias("pos.occupyingSpecimen", "spmn")
			.createAlias("spmn.visit", "visit")
			.createAlias("visit.registration", "cpr")
			.createAlias("cpr.collectionProtocol", "cp")
			.add(Restrictions.eq("id", crit.containerId()))
			.setProjection(Projections.distinct(Projections.property("spmn.label")));

		Conjunction notInTypes = Restrictions.conjunction();
		if (isNotEmpty(crit.specimenClasses())) {
			query.createAlias("spmn.specimenClass", "spmnClass");
			notInTypes.add(Restrictions.not(Restrictions.in("spmnClass.id", getPvIds(crit.specimenClasses()))));
		}

		if (isNotEmpty(crit.specimenTypes())) {
			query.createAlias("spmn.specimenType", "spmnType");
			notInTypes.add(Restrictions.not(Restrictions.in("spmnType.id", getPvIds(crit.specimenTypes()))));
		}

		Disjunction restriction = Restrictions.disjunction();
		if (isNotEmpty(crit.specimenClasses()) || isNotEmpty(crit.specimenTypes())) {
			restriction.add(notInTypes);
		}

		if (isNotEmpty(crit.collectionProtocols())) {
			restriction.add(Restrictions.not(Restrictions.in("cp.id", crit.collectionProtocolIds())));
		} else {
			DetachedCriteria siteCrit = DetachedCriteria.forClass(CollectionProtocolSite.class)
				.createAlias("collectionProtocol", "cp1")
				.createAlias("site", "site1")
				.add(Restrictions.eqProperty("cp1.id", "cp.id"))
				.setProjection(Property.forName("site1.id"));

			query.createAlias("site", "contSite");
			restriction.add(Subqueries.propertyNotIn("contSite.id", siteCrit));
		}

		return query.add(restriction).list();
	}

	@SuppressWarnings(value = "unchecked")
	@Override
	public List<String> getNonCompliantDistributedSpecimens(ContainerRestrictionsCriteria crit) {
		if (isEmpty(crit.distributionProtocols())) {
			return Collections.emptyList();
		}

		return (List<String>) getCurrentSession().createCriteria(DistributionOrder.class, "order")
			.createAlias("order.distributionProtocol", "dp")
			.createAlias("order.orderItems", "orderItem")
			.createAlias("orderItem.specimen", "spmn")
			.createAlias("spmn.position", "pos")
			.createAlias("pos.container", "container")
			.createAlias("container.ancestorContainers", "ancestor")
			.add(Restrictions.eq("ancestor.id", crit.containerId()))
			.add(Restrictions.not(Restrictions.in("dp.id", crit.distributionProtocolIds())))
			.setProjection(Projections.distinct(Projections.property("spmn.label")))
			.list();
	}

	@Override
	public int getSpecimensCount(Long containerId) {
		Map<Long, Integer> counts = getSpecimensCount(Collections.singletonList(containerId));
		return counts.containsKey(containerId) ? counts.get(containerId) : 0;
	}

	@SuppressWarnings(value = "unchecked")
	@Override
	public Map<Long, Integer> getSpecimensCount(Collection<Long> containerIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_SPECIMENS_COUNT)
			.setParameterList("containerIds", containerIds)
			.list();

		return rows.stream().collect(Collectors.toMap(r -> ((Number)r[0]).longValue(), r -> ((Number)r[1]).intValue()));
	}

	@Override
	public List<Specimen> getSpecimens(SpecimenListCriteria crit, boolean orderByLocation) {
		Criteria query = getCurrentSession().createCriteria(Specimen.class, "specimen")
			.createAlias("specimen.position", "pos")
			.add(Subqueries.propertyIn("specimen.id", getContainerSpecimensListQuery(crit)))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults());

		if (orderByLocation) {
			query.createAlias("pos.container", "container")
				.addOrder(Order.asc("container.name"))
				.addOrder(Order.asc("pos.posTwoOrdinal"))
				.addOrder(Order.asc("pos.posOneOrdinal"));
		} else {
			query.addOrder(Order.asc("pos.id"));
		}

		return query.list();
	}

	@Override
	public Long getSpecimensCount(SpecimenListCriteria crit) {
		Number count = (Number) getContainerSpecimensListQuery(crit)
			.setProjection(Projections.rowCount())
			.getExecutableCriteria(getCurrentSession())
			.uniqueResult();
		return count.longValue();
	}

	@Override
	@SuppressWarnings(value = "unchecked")
	public Map<Long, Integer> getRootContainerSpecimensCount(Collection<Long> containerIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_ROOT_CONT_SPMNS_COUNT)
			.setParameterList("containerIds", containerIds)
			.list();
		return rows.stream().collect(Collectors.toMap(row -> (Long)row[0], row -> (Integer)row[1]));
	}

	@Override
	@SuppressWarnings(value = "unchecked")
	public Map<String, Integer> getSpecimensCountByType(Long containerId) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_SPMNS_CNT_BY_TYPE)
			.setLong("containerId", containerId)
			.list();
		return rows.stream().collect(Collectors.toMap(row -> (String)row[0], row -> (Integer)row[1]));
	}

	@Override
	@SuppressWarnings(value = "unchecked")
	public StorageContainerSummary getAncestorsHierarchy(Long containerId) {
		Set<Long> ancestorIds = new HashSet<>();
		Long rootId = null;

		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_ANCESTORS)
			.setLong("containerId", containerId)
			.list();
		for (Object[] row : rows) {
			ancestorIds.add((Long)row[0]);
			if (row[1] == null) {
				rootId = (Long)row[0];
			}
		}

		rows = getCurrentSession().getNamedQuery(GET_ROOT_AND_CHILD_CONTAINERS)
			.setLong("rootId", rootId)
			.setParameterList("parentIds", ancestorIds)
			.list();

		Map<Long, StorageContainerSummary> containersMap = new HashMap<>();
		for (Object[] row : rows) {
			StorageContainerSummary container = createContainer(row);
			containersMap.put(container.getId(), container);
		}

		linkParentChildContainers(containersMap);
		return sortChildContainers(containersMap.get(rootId));
	}

	@Override
	@SuppressWarnings(value = "unchecked")
	public List<StorageContainerSummary> getChildContainers(StorageContainer container) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_CHILD_CONTAINERS)
			.setLong("parentId", container.getId())
			.list();

		PositionAssigner assigner = container.getPositionAssigner();
		List<StorageContainerSummary> children = new ArrayList<>();
		for (Object[] row : rows) {
			StorageContainerSummary child = createContainer(row);
			StorageLocationSummary location = child.getStorageLocation();
			if (location != null && location.getPosition() != null) {
				int rowNo = (location.getPosition() - 1) / 10000 + 1, colNo = (location.getPosition() - 1) % 10000 + 1;
				location.setPosition(assigner.toPosition(container, rowNo, colNo));
			}

			children.add(child);
		}

		children.sort(this::comparePositions);
		return children;
	}

	@Override
	@SuppressWarnings(value = "unchecked")
	public List<StorageContainer> getDescendantContainers(StorageContainerListCriteria crit) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(StorageContainer.class, "cont")
			.setProjection(Projections.distinct(Projections.property("cont.id")));

		Criteria innerQuery = detachedCriteria.getExecutableCriteria(getCurrentSession())
			.createAlias("cont.ancestorContainers", "ancestors")
			.add(Restrictions.eq("ancestors.id", crit.parentContainerId()));

		if (crit.siteCps() != null && !crit.siteCps().isEmpty()) {
			innerQuery.createAlias("cont.site", "site")
				.createAlias("cont.allowedCps", "cp", JoinType.LEFT_OUTER_JOIN);

			boolean instituteAdded = false;
			Disjunction siteCpsCond = Restrictions.disjunction();
			for (SiteCpPair siteCp : crit.siteCps()) {
				Junction siteCpCond = Restrictions.conjunction();
				if (siteCp.getSiteId() != null) {
					siteCpCond.add(Restrictions.eq("site.id", siteCp.getSiteId()));
				} else {
					if (!instituteAdded) {
						innerQuery.createAlias("site.institute", "institute");
						instituteAdded = true;
					}

					siteCpCond.add(Restrictions.eq("institute.id", siteCp.getInstituteId()));
				}

				if (siteCp.getCpId() != null) {
					siteCpCond.add(Restrictions.or(
						Restrictions.isNull("cp.id"),
						Restrictions.eq("cp.id", siteCp.getCpId())
					));
				}

				siteCpsCond.add(siteCpCond);
			}

			innerQuery.add(siteCpsCond);
		}

		if (StringUtils.isNotBlank(crit.query())) {
			innerQuery.add(Restrictions.ilike("cont.name", crit.query(), crit.matchMode()));
		}

		return getCurrentSession().createCriteria(StorageContainer.class, "cont")
			.add(Subqueries.propertyIn("cont.id", detachedCriteria))
			.addOrder(Order.asc("cont.id"))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.list();
	}

	@Override
	public int deleteReservedPositions(List<String> reservationIds) {
		return getCurrentSession().getNamedQuery(DEL_POS_BY_RSV_ID)
			.setParameterList("reservationIds", reservationIds)
			.executeUpdate();
	}

	@Override
	public int deleteReservedPositionsOlderThan(Date expireTime) {
		return getCurrentSession().getNamedQuery(DEL_EXPIRED_RSV_POS)
			.setTimestamp("expireTime", expireTime)
			.executeUpdate();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getLeafContainerIds(Long containerId, int startAt, int maxContainers) {
		return getCurrentSession().getNamedQuery(GET_LEAF_CONTAINERS)
			.setParameter("containerId", containerId)
			.setFirstResult(startAt)
			.setMaxResults(maxContainers)
			.list();
	}

	//
	// [ { containerName : [ specimen labels ] } ]
	//
	@Override
	public Map<String, List<String>> getInaccessibleSpecimens(
		List<Long> containerIds, List<SiteCpPair> siteCps,
		boolean useMrnSites, int firstN) {

		DetachedCriteria validSpmnsQuery = DetachedCriteria.forClass(Specimen.class, "specimen")
			.setProjection(Projections.distinct(Projections.property("specimen.id")));

		Criteria query = validSpmnsQuery.getExecutableCriteria(getCurrentSession())
			.createAlias("specimen.position", "pos")
			.createAlias("pos.container", "container")
			.createAlias("container.ancestorContainers", "aContainer")
			.add(Restrictions.in("aContainer.id", containerIds));

		BiospecimenDaoHelper.getInstance().addSiteCpsCond(query, siteCps, useMrnSites, "visit");
		return getInvalidSpecimens(containerIds, validSpmnsQuery, firstN);
	}

	@Override
	public Map<String, List<String>> getInvalidSpecimensForSite(List<Long> containerIds, Long siteId, int firstN) {
		DetachedCriteria validSpmnsQuery = DetachedCriteria.forClass(Specimen.class, "specimen")
			.setProjection(Projections.distinct(Projections.property("specimen.id")));
		validSpmnsQuery.getExecutableCriteria(getCurrentSession())
			.createAlias("specimen.position", "pos")
			.createAlias("pos.container", "container")
			.createAlias("container.ancestorContainers", "aContainer")
			.createAlias("specimen.collectionProtocol", "cp")
			.createAlias("cp.sites", "cpSite")
			.createAlias("cpSite.site", "site")
			.add(Restrictions.in("aContainer.id", containerIds))
			.add(Restrictions.eq("site.id", siteId));
		return getInvalidSpecimens(containerIds, validSpmnsQuery, firstN);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, List<Long>> getDescendantContainerIds(Collection<Long> containerIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_DESCENDANT_CONTAINER_IDS)
			.setParameterList("containerIds", containerIds)
			.list();

		Map<Long, List<Long>> result = new HashMap<>();
		for (Object[] row : rows) {
			Long ancestorId = (Long) row[0];
			List<Long> descendantIds = result.get(ancestorId);
			if (descendantIds == null) {
				descendantIds = new ArrayList<>();
				result.put(ancestorId, descendantIds);
			}
			descendantIds.add((Long) row[1]);
		}

		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<StorageContainer> getShippedContainers(Collection<Long> containerIds) {
		return getCurrentSession().getNamedQuery(GET_SHIPPED_CONTAINERS)
			.setParameterList("containerIds", containerIds)
			.list();
	}

	@Override
	public List<StorageContainerSummary> getUtilisations(Collection<Long> containerIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_CONTAINER_UTILISATION)
			.setParameterList("containerIds", containerIds)
			.list();

		return rows.stream().map(
			row -> {
				int idx = -1;

				StorageContainerSummary summary = new StorageContainerSummary();
				summary.setId((Long) row[++idx]);
				summary.setName((String) row[++idx]);
				summary.setNoOfRows((Integer) row[++idx]);
				summary.setNoOfColumns((Integer) row[++idx]);
				summary.setUsedPositions((Integer) row[++idx]);
				return summary;
			}
		).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private Map<String, List<String>> getInvalidSpecimens(List<Long> containerIds, DetachedCriteria validSpmnsQuery, int firstN) {
		List<Object[]> rows = getCurrentSession().createCriteria(Specimen.class, "specimen")
			.createAlias("specimen.position", "pos")
			.createAlias("pos.container", "container")
			.createAlias("container.ancestorContainers", "aContainer")
			.setProjection(Projections.projectionList()
				.add(Projections.property("specimen.label"))
				.add(Projections.property("container.name")))
			.add(Subqueries.propertyNotIn("specimen.id", validSpmnsQuery))
			.add(Restrictions.in("aContainer.id", containerIds))
			.addOrder(Order.asc("container.name"))
			.setMaxResults(firstN > 0 ? firstN : 5)
			.list();

		Map<String, List<String>> result = new HashMap<>();
		for (Object[] row : rows) {
			List<String> spmnLabels = result.get((String) row[1]);
			if (spmnLabels == null) {
				spmnLabels = new ArrayList<>();
				result.put((String) row[1], spmnLabels);
			}

			spmnLabels.add((String) row[0]);
		}

		return result;
	}

	private StorageContainerSummary createContainer(Object[] row) {
		int idx = 0;

		StorageContainerSummary container = new StorageContainerSummary();
		container.setId((Long)row[idx++]);
		container.setName((String)row[idx++]);
		container.setDisplayName((String)row[idx++]);
		container.setNoOfRows((Integer)row[idx++]);
		container.setNoOfColumns((Integer)row[idx++]);
		container.setPositionAssignment((String)row[idx++]);

		if (row[idx] != null) {
			StorageLocationSummary location = new StorageLocationSummary();
			location.setId((Long)row[idx++]);

			if (row[idx] != null) {
				//
				// if not stored in dimensionless container
				//
				int rowNo = (Integer)row[idx++];
				int colNo = (Integer)row[idx++];
				location.setPosition((rowNo - 1) * 10000 + colNo);
				location.setPositionY((String)row[idx++]);
				location.setPositionX((String)row[idx++]);
			}

			container.setStorageLocation(location);
		}

		return container;
	}

	private void linkParentChildContainers(Map<Long, StorageContainerSummary> containersMap) {
		Map<Long, StorageContainer> objMap = new HashMap<>();

		for (StorageContainerSummary container : containersMap.values()) {
			StorageLocationSummary location = container.getStorageLocation();
			if (location == null) {
				// root container
				continue;
			}

			StorageContainerSummary parent = containersMap.get(location.getId());
			StorageContainer pcObj = objMap.get(parent.getId());
			if (pcObj == null) {
				pcObj = new StorageContainer();
				pcObj.setNoOfColumns(parent.getNoOfColumns());
				pcObj.setNoOfRows(parent.getNoOfRows());
				pcObj.setPositionAssignment(StorageContainer.PositionAssignment.valueOf(parent.getPositionAssignment()));
				objMap.put(parent.getId(), pcObj);
			}

			//
			// Get back actual position value based on parent container dimension
			//
			if (location.getPosition() != null) {
				int rowNo = (location.getPosition() - 1) / 10000 + 1, colNo = (location.getPosition() - 1) % 10000 + 1;
				location.setPosition(pcObj.getPositionAssigner().toPosition(pcObj, rowNo, colNo));
			}

			if (parent.getChildContainers() == null) {
				parent.setChildContainers(new ArrayList<>());
			}

			parent.getChildContainers().add(container);
		}
	}

	private StorageContainerSummary sortChildContainers(StorageContainerSummary container) {
		if (CollectionUtils.isEmpty(container.getChildContainers())) {
			return container;
		}

		Collections.sort(container.getChildContainers(), this::comparePositions);
		container.getChildContainers().forEach(this::sortChildContainers);
		return container;
	}

	private int comparePositions(StorageContainerSummary s1, StorageContainerSummary s2) {
		Integer p1 = s1.getStorageLocation().getPosition();
		Integer p2 = s2.getStorageLocation().getPosition();
		if (p1 != null && p2 != null) {
			return p1.compareTo(p2);
		} else if (p1 == null && p2 == null) {
			return s1.getId().compareTo(s2.getId());
		} else if (p1 == null) {
			return 1;
		} else if (p2 == null) {
			return -1;
		} else {
			return 0;
		}
	}

	private DetachedCriteria getContainerSpecimensListQuery(SpecimenListCriteria crit) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Specimen.class, "specimen")
			.setProjection(Projections.distinct(Projections.property("specimen.id")));
		Criteria query = detachedCriteria.getExecutableCriteria(getCurrentSession());


		query.createAlias("specimen.position", "pos")
			.createAlias("pos.container", "container")
			.createAlias("container.ancestorContainers", "anscestor")
			.add(Restrictions.eq("anscestor.id", crit.ancestorContainerId()));

		if (crit.containerId() != null) {
			query.add(Restrictions.eq("container.id", crit.containerId()));
		} else if (StringUtils.isNotBlank(crit.container())) {
			query.add(Restrictions.eq("container.name", crit.container()));
		}

		if (StringUtils.isNotBlank(crit.type())) {
			query.createAlias("specimen.specimenType", "typePv")
				.add(Restrictions.eq("typePv.value", crit.type()));
		}

		if (StringUtils.isNotBlank(crit.anatomicSite())) {
			query.createAlias("specimen.tissueSite", "anatomicPv")
				.add(Restrictions.eq("anatomicPv.value", crit.anatomicSite()));
		}

		String startAlias = "visit";
		if (StringUtils.isNotBlank(crit.ppid()) || crit.cpId() != null || StringUtils.isNotBlank(crit.cpShortTitle())) {
			query.createAlias("specimen.visit", "visit")
				.createAlias("visit.registration", "cpr");

			startAlias = "cp";

			if (StringUtils.isNotBlank(crit.ppid())) {
				query.add(Restrictions.ilike("cpr.ppid", crit.ppid(), crit.matchMode()));
			}

			if (crit.cpId() != null || StringUtils.isNotBlank(crit.cpShortTitle())) {
				query.createAlias("cpr.collectionProtocol", "cp");

				if (crit.cpId() != null) {
					query.add(Restrictions.eq("cp.id", crit.cpId()));
				} else {
					query.add(Restrictions.eq("cp.shortTitle", crit.cpShortTitle()));
				}

				startAlias = "cpSite";
			}
		}

		BiospecimenDaoHelper.getInstance().addSiteCpsCond(query, crit.siteCps(), crit.useMrnSites(), startAlias);
		return detachedCriteria;
	}

	private List<Long> getPvIds(Collection<PermissibleValue> pvs) {
		return Utility.nullSafeStream(pvs).map(PermissibleValue::getId).collect(Collectors.toList());
	}

	private class ListQueryBuilder {
		private StorageContainerListCriteria crit;
		
		private StringBuilder select = new StringBuilder();
		
		private StringBuilder from = new StringBuilder();
		
		private StringBuilder where = new StringBuilder();
		
		private Map<String, Object> params = new HashMap<String, Object>();
		
		public ListQueryBuilder(StorageContainerListCriteria crit) {
			prepareQuery(crit, false);
		}
		
		public ListQueryBuilder(StorageContainerListCriteria crit, boolean countReq) {
			prepareQuery(crit, countReq);
		}

		public Query query() {
			addIdsRestriction();
			addNotInIdsRestriction();
			addNameRestriction();		
			addSiteRestriction();
					
			addFreeContainersRestriction();
			addSpecimenRestriction();
			addCpRestriction();
			addDpRestriction();
			addStoreSpecimenRestriction();
			addUsageModeRestriction();
			addStatuses();
			
			addParentRestriction();
			addCanHoldRestriction();

			// permissions check
			addSiteCpRestriction();
			
			String hql = new StringBuilder(select)
				.append(" ").append(from)
				.append(" ").append(where)
				.append(" order by c.id asc")
				.toString();
			
			Query query = getCurrentSession().createQuery(hql);
			for (Map.Entry<String, Object> param : params.entrySet()) {
				if (param.getValue() instanceof Collection<?>) {
					query.setParameterList(param.getKey(), (Collection<?>)param.getValue());
				} else {
					query.setParameter(param.getKey(), param.getValue());
				}				
			}
			
			return query;
		}
		
		private void prepareQuery(StorageContainerListCriteria crit, boolean countReq) {
			this.crit = crit;

			select = new StringBuilder(countReq ? "select count(distinct c.id)" : "select distinct c");
			if (crit.hierarchical()) {
				from = new StringBuilder("from ").append(getType().getName()).append(" c join c.descendentContainers dc");
				where = new StringBuilder("where dc.activityStatus = :activityStatus");
			} else {
				from = new StringBuilder("from ").append(getType().getName()).append(" c");
				where = new StringBuilder("where c.activityStatus = :activityStatus");						
			}

			from.append(countReq ? " left join c.position pos " : " left join fetch c.position pos ");
			params.put("activityStatus", Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		}
		
		private void addAnd() {
			if (where.length() != 0) {
				where.append(" and ");
			}
		}

		private void addIdsRestriction() {
			if (CollectionUtils.isEmpty(crit.ids())) {
				return;
			}

			addAnd();
			where.append("c.id in :ids");
			params.put("ids", crit.ids());
		}

		private void addNotInIdsRestriction() {
			if (CollectionUtils.isEmpty(crit.notInIds())) {
				return;
			}

			addAnd();
			where.append("c.id not in :notInIds");
			params.put("notInIds", crit.notInIds());
		}

		private void addNameRestriction() {
			if (StringUtils.isNotBlank(crit.query())) {
				addAnd();
				where.append("(upper(c.name) like :name or upper(c.displayName) like :displayName)");
				params.put("name", "%" + crit.query().toUpperCase() + "%");
				params.put("displayName", "%" + crit.query().toUpperCase() + "%");
			}

			if (CollectionUtils.isNotEmpty(crit.names())) {
				addAnd();
				where.append("c.name in (:names)");
				params.put("names", crit.names());
			}

			if (CollectionUtils.isNotEmpty(crit.barcodes())) {
				addAnd();
				where.append("c.barcode in (:barcodes)");
				params.put("barcodes", crit.barcodes());
			}
		}

		private void addFreeContainersRestriction() {			
			if (!crit.onlyFreeContainers()) {
				return;
			}

			addAnd();
			if (crit.hierarchical()) {
				where.append("((dc.noOfRows is null and dc.noOfColumns is null)")
					.append("or")
					.append("(size(dc.occupiedPositions) - dc.noOfRows * dc.noOfColumns < 0))");
			} else {
				where.append("((c.noOfRows is null and c.noOfColumns is null)")
					.append("or")
					.append("(size(c.occupiedPositions) - c.noOfRows * c.noOfColumns < 0))");
			}
		}

		private void addSiteAlias() {
			from.append(" join c.site site");
		}

		private void addSiteRestriction() {
			if (StringUtils.isBlank(crit.siteName())) {
				return;
			}
			
			addSiteAlias();

			addAnd();
			where.append("site.name = :siteName");
			params.put("siteName", crit.siteName());
		}

		private void addParentRestriction() {
			if (!crit.topLevelContainers() && crit.parentContainerId() == null) {
				return;
			}
			
			if (crit.topLevelContainers()) {
				from.append(" left join c.parentContainer pc");			
			} else if (crit.parentContainerId() != null) {
				from.append(" join c.parentContainer pc");
			} 

			addAnd();
			Long parentId = crit.parentContainerId();
			if (parentId == null) {
				where.append("pc is null");
			} else {
				where.append("pc.id = :parentId");
				params.put("parentId", parentId);						
			}
		}

		private void addCanHoldRestriction() {
			if (StringUtils.isBlank(crit.canHold())) {
				return;
			}

			from.append(" left join c.type type");
			from.append(" left join type.canHold canHold");

			addAnd();
			where.append("(type is null or canHold.name = :canHold)");
			params.put("canHold", crit.canHold());
		}

		private void addSpecimenRestriction() {			
			String specimenClass = crit.specimenClass(), specimenType = crit.specimenType();			
			if (StringUtils.isBlank(specimenClass) && StringUtils.isBlank(specimenType)) {
				return;
			}
			
			addAnd();
			if (crit.hierarchical()) {
				from.append(" left join dc.compAllowedSpecimenClasses spmnClass")
					.append(" left join dc.compAllowedSpecimenTypes spmnType");
			} else {
				from.append(" left join c.compAllowedSpecimenClasses spmnClass")
					.append(" left join c.compAllowedSpecimenTypes spmnType");
			}

			where.append("(spmnClass.value = :specimenClass or spmnType.value = :specimenType)");
			params.put("specimenClass", specimenClass);
			params.put("specimenType", specimenType);
		}

		private void addCpAlias() {
			if (crit.hierarchical()) {
				from.append(" left join dc.compAllowedCps cp");
			} else {
				from.append(" left join c.compAllowedCps cp");
			}
		}

		private void addCpRestriction() {
			if (isEmpty(crit.cpIds()) && isEmpty(crit.cpShortTitles())) {
				return;
			}

			addCpAlias();

			addAnd();

			if (CollectionUtils.isNotEmpty(crit.cpIds())) {
				where.append("(cp is null or cp.id in (:cpIds))");
				params.put("cpIds", crit.cpIds());
			} else {
				where.append("(cp is null or cp.shortTitle in (:cpShortTitles))");
				params.put("cpShortTitles", crit.cpShortTitles());
			}
		}

		private void addDpRestriction() {
			if (isEmpty(crit.dpShortTitles())) {
				return;
			}

			if (crit.hierarchical()) {
				from.append(" left join dc.compAllowedDps dp");
			} else {
				from.append(" left join c.compAllowedDps dp");
			}

			addAnd();
			where.append("(dp is null or dp.shortTitle in (:dpShortTitles))");
			params.put("dpShortTitles", crit.dpShortTitles());
		}
		
		private void addStoreSpecimenRestriction() {			
			if (crit.storeSpecimensEnabled() == null) {
				return;
			}

			addAnd();
			if (crit.hierarchical()) {
				where.append("dc.storeSpecimenEnabled = :storeSpecimenEnabled");
			} else {
				where.append("c.storeSpecimenEnabled = :storeSpecimenEnabled");
			}
			
			params.put("storeSpecimenEnabled", crit.storeSpecimensEnabled());
		}

		private void addSiteCpRestriction() {
			if (CollectionUtils.isEmpty(crit.siteCps())) {
				return;
			}

			if (StringUtils.isBlank(crit.siteName())) {
				addSiteAlias();
			}

			if (CollectionUtils.isEmpty(crit.cpIds()) && CollectionUtils.isEmpty(crit.cpShortTitles())) {
				addCpAlias();
			}

			boolean instituteAliasAdded = false;

			List<String> disjunctions = new ArrayList<>();
			for (SiteCpPair siteCp : crit.siteCps()) {
				StringBuilder restriction = new StringBuilder();
				if (siteCp.getSiteId() != null) {
					restriction.append("(site.id = ").append(siteCp.getSiteId());
				} else {
					if (!instituteAliasAdded) {
						from.append(" join site.institute institute");
						instituteAliasAdded = true;
					}

					restriction.append("(institute.id = ").append(siteCp.getInstituteId());
				}

				if (siteCp.getCpId() != null) {
					restriction.append(" and (")
						.append("cp.id is null or ")
						.append("cp.id = ").append(siteCp.getCpId())
						.append(")");
				}

				restriction.append(")");
				disjunctions.add(restriction.toString());
			}

			addAnd();
			where.append("(").append(StringUtils.join(disjunctions, " or ")).append(")");
		}

		private void addUsageModeRestriction() {
			if (crit.usageMode() == null) {
				return;
			}

			addAnd();
			if (crit.hierarchical()) {
				where.append("dc.usedFor = :usedFor");
			} else {
				where.append("c.usedFor = :usedFor");
			}

			params.put("usedFor", crit.usageMode());
		}

		private void addStatuses() {
			if (crit.statuses() == null || crit.statuses().isEmpty()) {
				return;
			}

			String alias = crit.hierarchical() ? "dc" : "c";
			List<String> disjunctions = new ArrayList<>();
			for (StorageContainerListCriteria.Status status : crit.statuses()) {
				switch (status) {
					case AVAILABLE:
						disjunctions.add("(" + alias + ".status is null and " + alias + ".activityStatus = 'Active')");
						break;

					case CHECKED_OUT:
						disjunctions.add("(" + alias + ".status = 'CHECKED_OUT')");
						break;

					case ARCHIVED:
						disjunctions.add("(" + alias + ".status = 'Closed')");
						break;
				}
			}

			addAnd();
			where.append("(" + StringUtils.join(disjunctions, " or ") + ")");
		}
	}

	private static final String FQN = StorageContainer.class.getName();

	private static final String GET_SPECIMENS_COUNT = FQN + ".getSpecimensCount";

	private static final String GET_ANCESTORS = FQN + ".getAncestors";

	private static final String GET_ROOT_AND_CHILD_CONTAINERS = FQN + ".getRootAndChildContainers";

	private static final String GET_CHILD_CONTAINERS = FQN + ".getChildContainers";

	private static final String DEL_POS_BY_RSV_ID = FQN + ".deletePositionsByReservationIds";

	private static final String DEL_EXPIRED_RSV_POS = FQN + ".deleteReservationsOlderThanTime";

	private static final String GET_ROOT_CONT_SPMNS_COUNT = FQN + ".getRootContainerSpecimensCount";

	private static final String GET_SPMNS_CNT_BY_TYPE = FQN + ".getSpecimenCountsByType";

	private static final String GET_LEAF_CONTAINERS = FQN + ".getLeafContainerIds";

	private static final String GET_DESCENDANT_CONTAINER_IDS = FQN + ".getDescendantContainerIds";

	private static final String GET_SHIPPED_CONTAINERS = FQN + ".getShippedContainers";

	private static final String GET_CONTAINER_UTILISATION = FQN + ".getContainersUtilisation";
}
