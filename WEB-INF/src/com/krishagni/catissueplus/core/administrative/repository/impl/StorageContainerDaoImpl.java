
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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

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
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Conjunction;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.common.repository.Junction;
import com.krishagni.catissueplus.core.common.repository.Query;
import com.krishagni.catissueplus.core.common.repository.SubQuery;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public class StorageContainerDaoImpl extends AbstractDao<StorageContainer> implements StorageContainerDao {

	@Override
	public Class<?> getType() {
		return StorageContainer.class;
	}

	@Override
	public List<StorageContainer> getStorageContainers(StorageContainerListCriteria listCrit) {
		Query<StorageContainer> query = new ListQueryBuilder(listCrit).query(StorageContainer.class);
		return query.setFirstResult(listCrit.startAt())
			.setMaxResults(listCrit.maxResults())
			.list();
	}
	
	@Override
	public Long getStorageContainersCount(StorageContainerListCriteria listCrit) {
		return new ListQueryBuilder(listCrit, true).query(Long.class).uniqueResult();
	}
	
	@Override
	public StorageContainer getByName(String name) {
		Criteria<StorageContainer> query = createCriteria(StorageContainer.class, "s");
		return query.add(query.eq("s.name", name))
			.add(query.eq("s.activityStatus", Status.ACTIVITY_STATUS_ACTIVE.getStatus()))
			.uniqueResult();
	}

	public StorageContainer getByBarcode(String barcode) {
		Criteria<StorageContainer> query = createCriteria(StorageContainer.class, "s");
		return query.add(query.eq(query.lower("s.barcode"), barcode.toLowerCase()))
			.add(query.eq("s.activityStatus", Status.ACTIVITY_STATUS_ACTIVE.getStatus()))
			.uniqueResult();
	}

	@Override
	public void delete(StorageContainerPosition position) {
		super.delete(position);
	}

	@Override
	public Map<String, Object> getContainerIds(String key, Object value) {
		return getObjectIds("containerId", key, value);
	}

	@Override
	public List<String> getNonCompliantContainers(ContainerRestrictionsCriteria crit) {
		Criteria<String> query = createCriteria(StorageContainer.class, String.class, "s");
		query.join("s.descendentContainers", "d")
			.add(query.eq("s.id", crit.containerId()))
			.distinct().select("d.name");

		Disjunction restriction = query.disjunction();
		if (isNotEmpty(crit.specimenClasses())) {
			query.leftJoin("d.compAllowedSpecimenClasses", "spmnClass");
			restriction.add(
				query.conjunction()
					.add(query.isNotNull("spmnClass.id"))
					.add(query.notIn("spmnClass.id", getPvIds(crit.specimenClasses())))
			);
		}

		query.leftJoin("d.compAllowedSpecimenTypes", "spmnType");
		Junction notInTypes = query.conjunction();
		if (isNotEmpty(crit.specimenTypes())) {
			notInTypes.add(query.notIn("spmnType.id", getPvIds(crit.specimenTypes())));
		}

		if (isNotEmpty(crit.specimenClasses())) {
			SubQuery<Long> typesQuery = query.createSubQuery(PermissibleValue.class, "pv");
			typesQuery.join("pv.parent", "ppv")
				.add(typesQuery.eq("pv.attribute", "specimen_type"))
				.add(typesQuery.in("ppv.id", getPvIds(crit.specimenClasses())))
				.select("pv.id");
			notInTypes.add(query.notIn("spmnType.id", typesQuery));
		}

		restriction.add(
			query.conjunction()
				.add(query.isNotNull("spmnType.id"))
				.add(notInTypes)
		);

		if (isNotEmpty(crit.collectionProtocols())) {
			query.leftJoin("d.compAllowedCps", "cp");
			restriction.add(
				query.conjunction()
					.add(query.isNotNull("cp.id"))
					.add(query.notIn("cp.id", crit.collectionProtocolIds()))
			);
		} else {
			query.leftJoin("d.compAllowedCps", "cp");
			SubQuery<Long> siteQuery = query.createSubQuery(CollectionProtocolSite.class, "cpSite");
			siteQuery.join("cpSite.collectionProtocol", "cp1")
				.join("cpSite.site", "site1")
				.add(siteQuery.eq(siteQuery.getProperty("cp1.id"), query.getProperty("cp.id")))
				.select("site1.id");
			restriction.add(
				query.conjunction()
					.add(query.isNotNull("cp.id"))
					.add(query.not(query.valueIn(crit.siteId(), siteQuery)))
			);
		}

		if (isNotEmpty(crit.distributionProtocols())) {
			query.leftJoin("d.compAllowedDps", "dp");
			restriction.add(
				query.conjunction()
					.add(query.isNotNull("dp.id"))
					.add(query.notIn("dp.id", crit.distributionProtocolIds()))
			);
		}

		flush();
		return query.add(restriction).list();
	}

	@Override
	public List<String> getNonCompliantSpecimens(ContainerRestrictionsCriteria crit) {
		Criteria<String> query = createCriteria(StorageContainer.class, String.class, "s");
		query.join("s.descendentContainers", "d")
			.join("d.occupiedPositions", "pos")
			.join("pos.occupyingSpecimen", "spmn")
			.join("spmn.visit", "visit")
			.join("visit.registration", "cpr")
			.join("cpr.collectionProtocol", "cp")
			.add(query.eq("s.id", crit.containerId()))
			.distinct().select("spmn.label");

		Conjunction notInTypes = query.conjunction();
		if (isNotEmpty(crit.specimenClasses())) {
			query.join("spmn.specimenClass", "spmnClass");
			notInTypes.add(query.notIn("spmnClass.id", getPvIds(crit.specimenClasses())));
		}

		if (isNotEmpty(crit.specimenTypes())) {
			query.join("spmn.specimenType", "spmnType");
			notInTypes.add(query.notIn("spmnType.id", getPvIds(crit.specimenTypes())));
		}

		Disjunction restriction = query.disjunction();
		if (isNotEmpty(crit.specimenClasses()) || isNotEmpty(crit.specimenTypes())) {
			restriction.add(notInTypes);
		}

		if (isNotEmpty(crit.collectionProtocols())) {
			restriction.add(query.notIn("cp.id", crit.collectionProtocolIds()));
		} else {
			SubQuery<Long> siteQuery = query.createSubQuery(CollectionProtocolSite.class, "cpSite");
			siteQuery.join("cpSite.collectionProtocol", "cp1")
				.join("cpSite.site", "site1")
				.add(siteQuery.eq(siteQuery.getProperty("cp1.id"), query.getProperty("cp.id")))
				.select("site1.id");

			query.join("site", "contSite");
			restriction.add(query.notIn("contSite.id", siteQuery));
		}

		return query.add(restriction).list();
	}

	@Override
	public List<String> getNonCompliantDistributedSpecimens(ContainerRestrictionsCriteria crit) {
		if (isEmpty(crit.distributionProtocols())) {
			return Collections.emptyList();
		}

		Criteria<String> query = createCriteria(DistributionOrder.class, String.class, "order");
		return query.join("order.distributionProtocol", "dp")
			.join("order.orderItems", "orderItem")
			.join("orderItem.specimen", "spmn")
			.join("spmn.position", "pos")
			.join("pos.container", "container")
			.join("container.ancestorContainers", "ancestor")
			.add(query.eq("ancestor.id", crit.containerId()))
			.add(query.notIn("dp.id", crit.distributionProtocolIds()))
			.distinct().select("spmn.label")
			.list();
	}

	@Override
	public int getSpecimensCount(Long containerId) {
		Map<Long, Integer> counts = getSpecimensCount(Collections.singletonList(containerId));
		return counts.getOrDefault(containerId, 0);
	}

	@Override
	public Map<Long, Integer> getSpecimensCount(Collection<Long> containerIds) {
		List<Object[]> rows = createNamedQuery(GET_SPECIMENS_COUNT, Object[].class)
			.setParameterList("containerIds", containerIds)
			.list();

		return rows.stream().collect(Collectors.toMap(r -> ((Number)r[0]).longValue(), r -> ((Number)r[1]).intValue()));
	}

	@Override
	public List<Specimen> getSpecimens(SpecimenListCriteria crit, boolean orderByLocation) {
		Criteria<Specimen> query = createCriteria(Specimen.class, "specimen");
		query.join("specimen.position", "pos")
			.add(query.in("specimen.id", getContainerSpecimensListQuery(crit, query)));

		if (orderByLocation) {
			query.join("pos.container", "container")
				.addOrder(query.asc("container.name"))
				.addOrder(query.asc("pos.posTwoOrdinal"))
				.addOrder(query.asc("pos.posOneOrdinal"));
		} else {
			query.addOrder(query.asc("pos.id"));
		}

		return query.list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Long getSpecimensCount(SpecimenListCriteria crit) {
		Criteria<Long> query = createCriteria(Specimen.class, Long.class, "specimen");
		return query.add(query.in("specimen.id", getContainerSpecimensListQuery(crit, query)))
			.getCount("specimen.id");
	}

	@Override
	public Map<Long, Integer> getRootContainerSpecimensCount(Collection<Long> containerIds) {
		List<Object[]> rows = createNamedQuery(GET_ROOT_CONT_SPMNS_COUNT, Object[].class)
			.setParameterList("containerIds", containerIds)
			.list();
		return rows.stream().collect(Collectors.toMap(row -> (Long)row[0], row -> (Integer)row[1]));
	}

	@Override
	public Map<String, Integer> getSpecimensCountByType(Long containerId) {
		List<Object[]> rows = createNamedQuery(GET_SPMNS_CNT_BY_TYPE, Object[].class)
			.setParameter("containerId", containerId)
			.list();
		return rows.stream().collect(Collectors.toMap(row -> (String)row[0], row -> (Integer)row[1]));
	}

	@Override
	public StorageContainerSummary getAncestorsHierarchy(Long containerId) {
		Set<Long> ancestorIds = new HashSet<>();
		Long rootId = null;

		List<Object[]> rows = createNamedQuery(GET_ANCESTORS, Object[].class)
			.setParameter("containerId", containerId)
			.list();
		for (Object[] row : rows) {
			ancestorIds.add((Long)row[0]);
			if (row[1] == null) {
				rootId = (Long)row[0];
			}
		}

		rows = createNamedQuery(GET_ROOT_AND_CHILD_CONTAINERS, Object[].class)
			.setParameter("rootId", rootId)
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
	public List<StorageContainerSummary> getChildContainers(StorageContainer container) {
		List<Object[]> rows = createNamedQuery(GET_CHILD_CONTAINERS, Object[].class)
			.setParameter("parentId", container.getId())
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
	public List<StorageContainer> getDescendantContainers(StorageContainerListCriteria crit) {
		Criteria<StorageContainer> outerQuery = createCriteria(StorageContainer.class, "cont");
		SubQuery<Long> innerQuery = outerQuery.createSubQuery(StorageContainer.class, "cont")
			.distinct().select("cont.id");
		innerQuery.join("cont.ancestorContainers", "ancestors")
			.add(innerQuery.eq("ancestors.id", crit.parentContainerId()));

		if (crit.siteCps() != null && !crit.siteCps().isEmpty()) {
			innerQuery.join("cont.site", "site")
				.leftJoin("cont.allowedCps", "cp");

			boolean instituteAdded = false;
			Disjunction siteCpsCond = innerQuery.disjunction();
			for (SiteCpPair siteCp : crit.siteCps()) {
				Junction siteCpCond = innerQuery.conjunction();
				if (siteCp.getSiteId() != null) {
					siteCpCond.add(innerQuery.eq("site.id", siteCp.getSiteId()));
				} else {
					if (!instituteAdded) {
						innerQuery.join("site.institute", "institute");
						instituteAdded = true;
					}

					siteCpCond.add(innerQuery.eq("institute.id", siteCp.getInstituteId()));
				}

				if (siteCp.getCpId() != null) {
					siteCpCond.add(innerQuery.or(
						innerQuery.isNull("cp.id"),
						innerQuery.eq("cp.id", siteCp.getCpId())
					));
				}

				siteCpsCond.add(siteCpCond);
			}

			innerQuery.add(siteCpsCond);
		}

		if (StringUtils.isNotBlank(crit.query())) {
			innerQuery.add(innerQuery.ilike("cont.name", crit.query()));
		}

		return outerQuery.add(outerQuery.in("cont.id", innerQuery))
			.addOrder(outerQuery.asc("cont.id"))
			.list(crit.startAt(), crit.maxResults());
	}

	@Override
	public int deleteReservedPositions(List<String> reservationIds) {
		return createNamedQuery(DEL_POS_BY_RSV_ID)
			.setParameterList("reservationIds", reservationIds)
			.executeUpdate();
	}

	@Override
	public int deleteReservedPositionsOlderThan(Date expireTime) {
		return createNamedQuery(DEL_EXPIRED_RSV_POS)
			.setParameter("expireTime", expireTime)
			.executeUpdate();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getLeafContainerIds(Long containerId, int startAt, int maxContainers) {
		return createNamedQuery(GET_LEAF_CONTAINERS, Long.class)
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

		Function<AbstractCriteria<?, ?>, SubQuery<Long>> validSpmnsQuery = (query) -> {
			SubQuery<Long> subQuery = query.createSubQuery(Specimen.class, "specimen");
			subQuery.join("specimen.position", "pos")
				.join("pos.container", "container")
				.join("container.ancestorContainers", "aContainer")
				.add(subQuery.in("aContainer.id", containerIds))
				.distinct().select("specimen.id");
			BiospecimenDaoHelper.getInstance().addSiteCpsCond(subQuery, siteCps, useMrnSites);
			return subQuery;
		};

		return getInvalidSpecimens(containerIds, validSpmnsQuery, firstN);
	}

	@Override
	public Map<String, List<String>> getInvalidSpecimensForSite(List<Long> containerIds, Long siteId, int firstN) {
		Function<AbstractCriteria<?, ?>, SubQuery<Long>> validSpmnsQuery = (query) -> {
			SubQuery<Long> subQuery = query.createSubQuery(Specimen.class, "specimen");
			subQuery.join("specimen.position", "pos")
				.join("pos.container", "container")
				.join("container.ancestorContainers", "aContainer")
				.join("specimen.collectionProtocol", "cp")
				.join("cp.sites", "cpSite")
				.join("cpSite.site", "site")
				.add(subQuery.in("aContainer.id", containerIds))
				.add(subQuery.eq("site.id", siteId))
				.distinct().select("specimen.id");
			return subQuery;
		};
		return getInvalidSpecimens(containerIds, validSpmnsQuery, firstN);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, List<Long>> getDescendantContainerIds(Collection<Long> containerIds) {
		List<Object[]> rows = createNamedQuery(GET_DESCENDANT_CONTAINER_IDS, Object[].class)
			.setParameterList("containerIds", containerIds)
			.list();

		Map<Long, List<Long>> result = new HashMap<>();
		for (Object[] row : rows) {
			Long ancestorId = (Long) row[0];
			List<Long> descendantIds = result.computeIfAbsent(ancestorId, k -> new ArrayList<>());
			descendantIds.add((Long) row[1]);
		}

		return result;
	}

	@Override
	public List<StorageContainer> getShippedContainers(Collection<Long> containerIds) {
		return createNamedQuery(GET_SHIPPED_CONTAINERS, StorageContainer.class)
			.setParameterList("containerIds", containerIds)
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<StorageContainerSummary> getUtilisations(Collection<Long> containerIds) {
		List<Object[]> rows = createNamedQuery(GET_CONTAINER_UTILISATION, Object[].class)
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

	private Map<String, List<String>> getInvalidSpecimens(List<Long> containerIds, Function<AbstractCriteria<?, ?>, SubQuery<Long>> validSpmnsQuery, int firstN) {
		Criteria<Object[]> query = createCriteria(Specimen.class, Object[].class, "specimen");
		List<Object[]> rows = query.join("specimen.position", "pos")
			.join("pos.container", "container")
			.join("container.ancestorContainers", "aContainer")
			.select("specimen.label", "container.name")
			.add(query.notIn("specimen.id", validSpmnsQuery.apply(query)))
			.add(query.in("aContainer.id", containerIds))
			.addOrder(query.asc("container.name"))
			.list(0, firstN > 0 ? firstN : 5);

		Map<String, List<String>> result = new HashMap<>();
		for (Object[] row : rows) {
			List<String> spmnLabels = result.computeIfAbsent((String) row[1], k -> new ArrayList<>());
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

		container.getChildContainers().sort(this::comparePositions);
		container.getChildContainers().forEach(this::sortChildContainers);
		return container;
	}

	private int comparePositions(StorageContainerSummary s1, StorageContainerSummary s2) {
		return ObjectUtils.compare(s1.getStorageLocation().getPosition(), s2.getStorageLocation().getPosition());
	}

	private SubQuery<Long> getContainerSpecimensListQuery(SpecimenListCriteria crit, AbstractCriteria<?, ?> mainQuery) {
		SubQuery<Long> query = mainQuery.createSubQuery(Specimen.class, "specimen");
		query.distinct().select("specimen.id")
			.join("specimen.position", "pos")
			.join("pos.container", "container")
			.join("container.ancestorContainers", "anscestor")
			.add(query.eq("anscestor.id", crit.ancestorContainerId()));

		if (crit.containerId() != null) {
			query.add(query.eq("container.id", crit.containerId()));
		} else if (StringUtils.isNotBlank(crit.container())) {
			query.add(query.eq("container.name", crit.container()));
		}

		if (StringUtils.isNotBlank(crit.type())) {
			query.join("specimen.specimenType", "typePv")
				.add(query.eq("typePv.value", crit.type()));
		}

		if (StringUtils.isNotBlank(crit.anatomicSite())) {
			query.join("specimen.tissueSite", "anatomicPv")
				.add(query.eq("anatomicPv.value", crit.anatomicSite()));
		}

		if (StringUtils.isNotBlank(crit.ppid()) || crit.cpId() != null || StringUtils.isNotBlank(crit.cpShortTitle())) {
			query.join("specimen.visit", "visit")
				.join("visit.registration", "cpr");

			if (StringUtils.isNotBlank(crit.ppid())) {
				query.add(query.ilike("cpr.ppid", crit.ppid()));
			}

			if (crit.cpId() != null || StringUtils.isNotBlank(crit.cpShortTitle())) {
				query.join("cpr.collectionProtocol", "cp");

				if (crit.cpId() != null) {
					query.add(query.eq("cp.id", crit.cpId()));
				} else {
					query.add(query.eq("cp.shortTitle", crit.cpShortTitle()));
				}
			}
		}

		BiospecimenDaoHelper.getInstance().addSiteCpsCond(query, crit.siteCps(), crit.useMrnSites());
		return query;
	}

	private List<Long> getPvIds(Collection<PermissibleValue> pvs) {
		return Utility.nullSafeStream(pvs).map(PermissibleValue::getId).collect(Collectors.toList());
	}

	private class ListQueryBuilder {
		private StorageContainerListCriteria crit;
		
		private StringBuilder select = new StringBuilder();
		
		private StringBuilder from = new StringBuilder();
		
		private StringBuilder where = new StringBuilder();
		
		private final Map<String, Object> params = new HashMap<>();

		private boolean countReq = false;
		
		public ListQueryBuilder(StorageContainerListCriteria crit) {
			prepareQuery(crit);
		}
		
		public ListQueryBuilder(StorageContainerListCriteria crit, boolean countReq) {
			this.countReq = countReq;
			prepareQuery(crit);
		}

		public <T> Query<T> query(Class<T> returnType) {
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
			
			addParentRestriction();
			addCanHoldRestriction();

			// permissions check
			addSiteCpRestriction();
			
			String hql = select + " " + from + " " + where + " order by c.id asc";
			Query<T> query = createQuery(hql, returnType);
			for (Map.Entry<String, Object> param : params.entrySet()) {
				if (param.getValue() instanceof Collection<?>) {
					query.setParameterList(param.getKey(), (Collection<?>)param.getValue());
				} else {
					query.setParameter(param.getKey(), param.getValue());
				}				
			}
			
			return query;
		}
		
		private void prepareQuery(StorageContainerListCriteria crit) {
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
