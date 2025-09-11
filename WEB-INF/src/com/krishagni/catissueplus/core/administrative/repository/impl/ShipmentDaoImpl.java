package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Shipment;
import com.krishagni.catissueplus.core.administrative.domain.Shipment.Status;
import com.krishagni.catissueplus.core.administrative.domain.ShipmentContainer;
import com.krishagni.catissueplus.core.administrative.domain.ShipmentSpecimen;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.events.ShipmentItemsListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ShipmentListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.ShipmentDao;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.common.repository.Restriction;
import com.krishagni.catissueplus.core.common.repository.SubQuery;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class ShipmentDaoImpl extends AbstractDao<Shipment> implements ShipmentDao {

	@Override
	public Class<Shipment> getType() {
		return Shipment.class;
	}

	@Override
	public List<Shipment> getShipments(ShipmentListCriteria crit) {
		Criteria<Shipment> query = getShipmentsQuery(crit);
		return query.orderBy(
			query.desc(
				query.isNotNull("shipment.shippedDate"),
				"shipment.shippedDate",
				"shipment.requestDate"
			)
		).list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Long getShipmentsCount(ShipmentListCriteria crit) {
		return getShipmentsQuery(crit).getCount("shipment.id");
	}

	@Override
	public Shipment getShipmentByName(String name) {
		return createNamedQuery(GET_SHIPMENT_BY_NAME, Shipment.class)
			.setParameter("name", name)
			.uniqueResult();
	}

	@Override
	public List<Specimen> getShippedSpecimensByIds(List<Long> specimenIds) {
		return createNamedQuery(GET_SHIPPED_SPECIMENS_BY_IDS, Specimen.class)
			.setParameterList("ids", specimenIds)
			.list();
	}

	@Override
	public Map<String, Object> getShipmentIds(String key, Object value) {
		return getObjectIds("shipmentId", key, value);
	}

	@Override
	public List<ShipmentContainer> getShipmentContainers(ShipmentItemsListCriteria crit) {
		Criteria<ShipmentContainer> query = createCriteria(ShipmentContainer.class, "sc")
			.join("sc.shipment", "s");

		if (StringUtils.isNotBlank(crit.orderBy())) {
			if ("name".equals(crit.orderBy())) {
				query.join("sc.container", "box")
					.orderBy(crit.asc() ? query.asc("box.name") : query.desc("box.name"));
			}
		}

		return query.add(query.eq("s.id", crit.shipmentId()))
			.addOrder(query.asc("sc.id"))
			.list(crit.startAt(), crit.maxResults());
	}

	@Override
	public List<ShipmentSpecimen> getShipmentSpecimens(ShipmentItemsListCriteria crit) {
		Criteria<ShipmentSpecimen> query = createCriteria(ShipmentSpecimen.class, "ss")
			.join("ss.shipment", "s")
			.join("ss.specimen", "spmn");

		if (crit.containerId() != null) {
			query.join("spmn.position", "pos")
				.join("pos.container", "box")
				.join("box.ancestorContainers", "container")
				.add(query.eq("container.id", crit.containerId()));
		}

		List<Long> orderBySpmnIds = null;
		if (StringUtils.isNotBlank(crit.orderBy())) {
			switch (crit.orderBy()) {
				case "label" -> query.addOrder(crit.asc() ? query.asc("spmn.label") : query.desc("spmn.label"));
				case "ppid" -> query.join("spmn.visit", "visit")
					.join("visit.registration", "cpr")
					.addOrder(crit.asc() ? query.asc("cpr.ppid") : query.desc("cpr.ppid"));
				case "cp" -> query.join("spmn.collectionProtocol", "cp")
					.addOrder(crit.asc() ? query.asc("cp.shortTitle") : query.desc("cp.shortTitle"));
				case "location" -> {
					if (crit.containerId() == null) {
						query.leftJoin("spmn.position", "pos")
							.leftJoin("pos.container", "box");
					}
					query.addOrder(crit.asc() ? query.asc("box.name") : query.desc("box.name"))
						.addOrder(crit.asc() ? query.asc("pos.posTwoOrdinal") : query.desc("pos.posOneOrdinal"))
						.addOrder(crit.asc() ? query.asc("pos.posOneOrdinal") : query.desc("pos.posOneOrdinal"));
				}
				case "externalId" -> {
					orderBySpmnIds = getSpecimenIdsOrderedByExtId(
						crit.shipmentId(),
						crit.asc() ? "asc" : "desc",
						crit.startAt(),
						crit.maxResults());
					if (orderBySpmnIds.isEmpty()) {
						return Collections.emptyList();
					}
					applyIdsFilter(query, "spmn.id", orderBySpmnIds);
				}
			}
		}

		if (orderBySpmnIds != null) {
			List<ShipmentSpecimen> specimens = query.list();
			List<Long> spmnIdsOrder = orderBySpmnIds;
			specimens.sort(Comparator.comparingInt(ss -> spmnIdsOrder.indexOf(ss.getSpecimen().getId())));
			return specimens;
		}

		return query.addOrder(query.asc("ss.id"))
			.add(query.eq("s.id", crit.shipmentId()))
			.list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Map<Long, Integer> getSpecimensCount(Collection<Long> shipmentIds) {
		List<Object[]> rows = createNamedQuery(GET_SPECIMENS_COUNT, Object[].class)
			.setParameterList("shipmentIds", shipmentIds)
			.list();

		return rows.stream().collect(Collectors.toMap(row -> (Long)row[0], row -> (Integer)row[1]));
	}

	@Override
	public Map<Long, Integer> getSpecimensCountByContainer(Long shipmentId, Collection<Long> containerIds) {
		List<Object[]> rows = createNamedQuery(GET_SPECIMENS_COUNT_BY_CONT, Object[].class)
			.setParameter("shipmentId", shipmentId)
			.setParameterList("containerIds", containerIds)
			.list();

		return rows.stream().collect(Collectors.toMap(row -> (Long)row[0], row -> (Integer)row[1]));
	}

	@Override
	public Integer deleteSpecimenShipmentEvents(Long shipmentId) {
		return createNamedQuery(DELETE_SPECIMEN_SHIPMENT_EVENTS)
			.setParameter("shipmentId", shipmentId)
			.executeUpdate();
	}

	@Override
	public int addSpecimensToCart(Long shipmentId, Long cartId) {
		String insertDml = String.format(isMySQL() ? ADD_SHIPMENT_ITEMS_TO_CART_MYSQL : ADD_SHIPMENT_ITEMS_TO_CART_ORASQL, cartId, AuthUtil.getCurrentUser().getId(), cartId);
		return createNativeQuery(insertDml)
			.setParameter("shipmentId", shipmentId)
			.executeUpdate();
	}

	@Override
	public int removeSpecimensFromCart(Long shipmentId, Long cartId) {
		return createNativeQuery(RM_SPMNS_FROM_CART_SQL)
			.setParameter("shipmentId", shipmentId)
			.setParameter("cartId", cartId)
			.executeUpdate();
	}

	@Override
	public void removeShipmentCart(Long cartId) {
		getCurrentSession().getNamedQuery(REMOVE_SHIPMENT_CART)
			.setParameter("cartId", cartId)
			.executeUpdate();
	}

	private Criteria<Shipment> getShipmentsQuery(ShipmentListCriteria crit) {
		Criteria<Shipment> query = createCriteria(Shipment.class, "shipment")
			.join("shipment.sendingSite", "sendSite")
			.join("shipment.receivingSite", "recvSite");

		applyIdsFilter(query, "shipment.id", crit.ids());
		addNameRestrictions(query, crit);
		addSendSiteRestrictions(query, crit);
		addRecvSiteRestrictions(query, crit);
		addStatusRestrictions(query, crit);
		addRequestStatusRestrictions(query, crit);
		addLabelOrBarcodeRestrictions(query, crit);
		addSiteRestrictions(query, crit);
		return query;
	}

	private void addNameRestrictions(Criteria<Shipment> query, ShipmentListCriteria crit) {
		if (StringUtils.isBlank(crit.name())) {
			return;
		}
		
		query.add(query.ilike("shipment.name", crit.name()));
	}

	private void addSendSiteRestrictions(Criteria<Shipment> query, ShipmentListCriteria crit) {
		if (StringUtils.isBlank(crit.sendingSite())) {
			return;
		}

		query.add(query.eq("sendSite.name", crit.sendingSite()));
	}

	private void addRecvSiteRestrictions(Criteria<Shipment> query, ShipmentListCriteria crit) {
		if (StringUtils.isNotBlank(crit.recvInstitute())) {
			query.join("recvSite.institute", "institute")
				.add(query.eq("institute.name", crit.recvInstitute()));
		}

		if (StringUtils.isNotBlank(crit.recvSite())) {
			query.add(query.eq("recvSite.name", crit.recvSite()));
		}
	}

	private void addStatusRestrictions(Criteria<Shipment> query, ShipmentListCriteria crit) {
		if (crit.status() == null) {
			return;
		}

		query.add(query.eq("shipment.status", crit.status()));
	}

	private void addRequestStatusRestrictions(Criteria<Shipment> query, ShipmentListCriteria crit) {
		if (StringUtils.isBlank(crit.requestStatus())) {
			return;
		}

		query.createAlias("shipment.requestStatus", "reqStatus")
			.add(query.eq("reqStatus.value", crit.requestStatus()));
	}

	private void addLabelOrBarcodeRestrictions(Criteria<Shipment> query, ShipmentListCriteria crit) {
		if (StringUtils.isBlank(crit.labelOrBarcode())) {
			return;
		}

		SubQuery<Long> subQuery = query.createSubQuery(Shipment.class, Long.class, "s")
			.join("s.shipmentSpecimens", "ss")
			.join("ss.specimen", "spmn")
			.select("s.id");
		subQuery.add(
			subQuery.or(
				subQuery.eq("spmn.label", crit.labelOrBarcode()),
				subQuery.eq("spmn.barcode", crit.labelOrBarcode())
			)
		);

		query.add(query.in("shipment.id", subQuery));
	}

	//
	// Used to restrict access of shipments based on users' roles on various sites
	//
	private void addSiteRestrictions(Criteria<Shipment> query, ShipmentListCriteria crit) {
		if (CollectionUtils.isEmpty(crit.sites())) {
			return;
		}

		//
		// A pending request is accessible only to the super admins and the requester
		//
		query.leftJoin("shipment.requester", "requester")
			.add(
				query.or(
					query.eq("shipment.request", false),
					query.ne("shipment.status", Status.PENDING),
					query.eq("requester.id", AuthUtil.getCurrentUser().getId())
				)
			);

		Set<Long> instituteIds = new HashSet<>();
		Set<Long> siteIds      = new HashSet<>();
		for (SiteCpPair site : crit.sites()) {
			if (site.getSiteId() != null) {
				siteIds.add(site.getSiteId());
			} else if (site.getInstituteId() != null) {
				instituteIds.add(site.getInstituteId());
			}
		}

		//
		// (recv site is one of accessible sites and (shipment is not pending or it is a request)) or
		// (send site is one of accessible sites)
		// The shipment.request = true ensures the requesters can see/view the pending/draft shipments
		// created by them
		//
		query.add(
			query.or(
				query.and(
					getSiteRestriction(query, "recvSite.id", instituteIds, siteIds),
					query.or(
						query.eq("shipment.request", true),
						query.ne("shipment.status", Status.PENDING)
					)
				), /* end of AND */
				getSiteRestriction(query, "sendSite.id", instituteIds, siteIds)
			) /* end of OR */
		);
	}

	private Restriction getSiteRestriction(Criteria<?> query, String sitePropName, Collection<Long> instituteIds, Collection<Long> siteIds) {
		Disjunction result = query.disjunction();

		if (!instituteIds.isEmpty()) {
			SubQuery<Long> instituteSites = query.createSubQuery(Site.class, "site")
				.join("site.institute", "institute");
			instituteSites.add(instituteSites.in("institute.id", instituteIds))
				.select("site.id");
			result.add(query.in(sitePropName, instituteSites));
		}

		if (!siteIds.isEmpty()) {
			result.add(query.in(sitePropName, siteIds));
		}

		return result.getRestriction();
	}

	private List<Long> getSpecimenIdsOrderedByExtId(Long shipmentId, String sortOrder, int startAt, int maxResults) {
		String sql = String.format(
			isMySQL() ? GET_SPMN_IDS_ORD_BY_EXT_ID_MYSQL : GET_SPMN_IDS_ORD_BY_EXT_ID_ORA,
			sortOrder, sortOrder, sortOrder, sortOrder
		);

		List<Object[]> specimenIds = createNativeQuery(sql, Object[].class)
			.setParameter("shipmentId", shipmentId)
			.setFirstResult(startAt)
			.setMaxResults(maxResults)
			.list();
		return specimenIds.stream().map(r -> ((Number) r[0]).longValue()).collect(Collectors.toList());
	}
	
	private static final String FQN = Shipment.class.getName();
	
	private static final String GET_SHIPMENT_BY_NAME = FQN + ".getShipmentByName";
	
	private static final String GET_SHIPPED_SPECIMENS_BY_IDS = FQN + ".getShippedSpecimensByIds";

	private static final String GET_SPECIMENS_COUNT = FQN + ".getSpecimensCount";

	private static final String GET_SPECIMENS_COUNT_BY_CONT = FQN + ".getSpecimensCountByContainer";

	private static final String DELETE_SPECIMEN_SHIPMENT_EVENTS = FQN + ".deleteSpecimenShipmentEvents";

	private static final String REMOVE_SHIPMENT_CART = FQN + ".removeShipmentCart";

	private static final String GET_SPMN_IDS_ORD_BY_EXT_ID_MYSQL =
		"select " +
		"  s.identifier, group_concat(e.value order by e.value %s) " +
		"from " +
		"  catissue_specimen s " +
		"  left join os_spmn_external_ids e on e.specimen_id = s.identifier " +
		"where " +
		"  s.identifier in ( " +
		"    select " +
		"      specimen_id " +
		"    from " +
		"      os_shipment_specimens " +
		"    where " +
		"      shipment_id = :shipmentId " +
		"  ) " +
		"group by " +
		"  s.identifier " +
		"order by " +
		"  group_concat(e.value order by e.value %s) %s," +
		"  s.identifier %s";

	private static final String GET_SPMN_IDS_ORD_BY_EXT_ID_ORA =
		"select " +
		"  s.identifier, listagg(e.value, ',') within group (order by e.value %s) " +
		"from " +
		"  catissue_specimen s " +
		"  left join os_spmn_external_ids e on e.specimen_id = s.identifier " +
		"where " +
		"  s.identifier in ( " +
		"    select " +
		"      specimen_id " +
		"    from " +
		"      os_shipment_specimens " +
		"    where " +
		"      shipment_id = :shipmentId " +
		"  ) " +
		"group by " +
		"  s.identifier " +
		"order by " +
		"  listagg(e.value, ',') within group (order by e.value %s) %s," +
		"  s.identifier %s";

	private static final String ADD_SHIPMENT_ITEMS_TO_CART_MYSQL =
		"insert into " +
		"  catissue_spec_tag_items (tag_id, obj_id, added_by, added_on) " +
		"select " +
		"  %d, ss.specimen_id, %d, current_timestamp " +
		"from " +
		"  os_shipment_specimens ss " +
		"  left join catissue_spec_tag_items cart_spmns on cart_spmns.obj_id = ss.specimen_id and cart_spmns.tag_id = %d " +
		"where " +
		"  ss.shipment_id = :shipmentId and " +
		"  cart_spmns.identifier is null";

	private static final String ADD_SHIPMENT_ITEMS_TO_CART_ORASQL =
		"insert into " +
		"  catissue_spec_tag_items (identifier, tag_id, obj_id, added_by, added_on) " +
		"select " +
		"  OS_SPMN_CART_ITEMS_SEQ.nextval, %d, ss.specimen_id, %d, current_timestamp " +
		"from " +
		"  os_shipment_specimens ss " +
		"  left join catissue_spec_tag_items cart_spmns on cart_spmns.obj_id = ss.specimen_id and cart_spmns.tag_id = %d " +
		"where " +
		"  ss.shipment_id = :shipmentId and " +
		"  cart_spmns.identifier is null";

	private static final String RM_SPMNS_FROM_CART_SQL =
		"delete from " +
		"  catissue_spec_tag_items " +
		"where " +
		"  tag_id = :cartId and " +
		"  obj_id not in (select specimen_id from os_shipment_specimens where shipment_id = :shipmentId)";
}
