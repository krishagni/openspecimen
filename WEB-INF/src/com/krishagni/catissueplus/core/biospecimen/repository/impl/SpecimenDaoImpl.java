
package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.math.BigDecimal;
import java.util.Arrays;
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
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenDao;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class SpecimenDaoImpl extends AbstractDao<Specimen> implements SpecimenDao {
	public Class<?> getType() {
		return Specimen.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Specimen> getSpecimens(SpecimenListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(Specimen.class, "specimen")
			.add(Subqueries.propertyIn("specimen.id", getSpecimenIdsQuery(crit)));

		if (crit.lastId() == null && crit.specimenListId() != null) {
			query.createAlias("specimen.specimenListItems", "listItem")
				.createAlias("listItem.list", "list")
				.add(Restrictions.eq("list.id", crit.specimenListId()))
				.addOrder(Order.asc("listItem.id"));
		} else {
			query.addOrder(Order.asc("specimen.id"));
		}

		if (crit.limitItems()) {
			query.setFirstResult(crit.startAt()).setMaxResults(crit.maxResults());
		}

		return query.list();
	}

	public Integer getSpecimensCount(SpecimenListCriteria crit) {
		Number count = (Number) getCurrentSession().createCriteria(Specimen.class, "s")
			.add(Subqueries.propertyIn("s.id", getSpecimenIdsQuery(crit)))
			.setProjection(Projections.rowCount())
			.uniqueResult();
		return count.intValue();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Long> getSpecimenIds(SpecimenListCriteria crit) {
		return getSpecimenIdsQuery(crit).getExecutableCriteria(getCurrentSession()).list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Specimen getByLabel(String label) {
		List<Specimen> specimens = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_BY_LABEL)
			.setString("label", label)
			.list();
		return specimens.isEmpty() ? null : specimens.iterator().next();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Specimen getByLabelAndCp(String cpShortTitle, String label) {
		List<Specimen> specimens = getCurrentSession().getNamedQuery(GET_BY_LABEL_AND_CP)
			.setString("label", label)
			.setString("cpShortTitle", cpShortTitle)
			.list();

		return specimens.isEmpty() ? null : specimens.iterator().next();
	}
	
	@Override
	public Specimen getSpecimenByVisitAndSr(Long visitId, Long srId) {
		return getByVisitAndSrId(GET_BY_VISIT_AND_SR, visitId, srId);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Specimen> getByVisitAndSrCode(Long visitId, Collection<String> reqCodes) {
		return getCurrentSession().getNamedQuery(GET_BY_VISIT_N_SR_CODE)
			.setParameter("visitId", visitId)
			.setParameterList("srCodes", reqCodes)
			.list();
	}

	@Override
	public Specimen getParentSpecimenByVisitAndSr(Long visitId, Long srId) {
		return getByVisitAndSrId(GET_PARENT_BY_VISIT_AND_SR, visitId, srId);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Specimen getByBarcode(String barcode) {
		List<Specimen> specimens = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_BY_BARCODE)
			.setParameter("barcode", barcode)
			.list();

		return specimens.isEmpty() ? null : specimens.iterator().next();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Specimen getByBarcodeAndCp(String cpShortTitle, String barcode) {
		List<Specimen> specimens = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_BY_BARCODE_AND_CP)
			.setParameter("barcode", barcode)
			.setParameter("cpShortTitle", cpShortTitle)
			.list();

		return specimens.isEmpty() ? null : specimens.iterator().next();
	}

	@Override
	public Long getPrimarySpecimen(Long specimenId) {
		return (Long) getCurrentSession().getNamedQuery(GET_ROOT_ID)
			.setParameter("specimenId", specimenId)
			.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Specimen> getSpecimensByIds(List<Long> specimenIds) {
		return getCurrentSession().getNamedQuery(GET_BY_IDS)
			.setParameterList("specimenIds", specimenIds)
			.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Specimen> getByLabels(Collection<Pair<String, String>> cpLabels) {
		Criteria query = getCurrentSession().createCriteria(Specimen.class)
			.createAlias("collectionProtocol", "cp");

		Disjunction disjunction = Restrictions.disjunction();
		for (Pair<String, String> cpLabel : cpLabels) {
			disjunction.add(
				Restrictions.and(
					Restrictions.eq("cp.shortTitle", cpLabel.first()),
					Restrictions.eq("label", cpLabel.second())
				)
			);
		}

		return (List<Specimen>) query.add(disjunction).list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Specimen> getByBarcodes(Collection<Pair<String, String>> cpBarcodes) {
		Criteria query = getCurrentSession().createCriteria(Specimen.class)
			.createAlias("collectionProtocol", "cp");

		Disjunction disjunction = Restrictions.disjunction();
		for (Pair<String, String> cpBarcode : cpBarcodes) {
			disjunction.add(
				Restrictions.and(
					Restrictions.eq("cp.shortTitle", cpBarcode.first()),
					Restrictions.eq("barcode", cpBarcode.second())
				)
			);
		}

		return (List<Specimen>) query.add(disjunction).list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Specimen> getSpecimensByVisitId(Long visitId) {
		return sessionFactory.getCurrentSession()
				.getNamedQuery(GET_BY_VISIT_ID)
				.setLong("visitId", visitId)
				.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Specimen> getSpecimensByVisitName(String visitName) {
		return sessionFactory.getCurrentSession()
				.getNamedQuery(GET_BY_VISIT_NAME)
				.setString("visitName", visitName)
				.list();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> getCprAndVisitIds(String key, Object value) {
		List<Object[]> rows = getCurrentSession().createCriteria(Specimen.class)
			.createAlias("visit", "visit")
			.createAlias("visit.registration", "cpr")
			.createAlias("cpr.collectionProtocol", "cp")
			.setProjection(
				Projections.projectionList()
					.add(Projections.property("cp.id"))
					.add(Projections.property("cpr.id"))
					.add(Projections.property("visit.id"))
					.add(Projections.property("id")))
			.add(Restrictions.eq(key, value))
			.list();

		if (CollectionUtils.isEmpty(rows)) {
			return null;
		}
		
		Map<String, Object> result = new HashMap<>();
		Object[] row = rows.iterator().next();
		result.put("cpId",       row[0]);
		result.put("cprId",      row[1]);
		result.put("visitId",    row[2]);
		result.put("specimenId", row[3]);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<Long, Set<SiteCpPair>> getSpecimenSites(Set<Long> specimenIds) {
		Criteria query = getSessionFactory().getCurrentSession().createCriteria(Specimen.class)
				.createAlias("visit", "visit")
				.createAlias("visit.registration", "cpr")
				.createAlias("cpr.collectionProtocol", "cp")
				.createAlias("cp.sites", "cpSite")
				.createAlias("cpSite.site", "site");
		
		ProjectionList projs = Projections.projectionList();
		query.setProjection(projs);
		projs.add(Projections.property("id"));
		projs.add(Projections.property("site.institute.id"));
		projs.add(Projections.property("site.id"));
		query.add(Restrictions.in("id", specimenIds));
		
		List<Object []> rows = query.list();
		Map<Long, Set<SiteCpPair>> results = new HashMap<>();
		for (Object[] row: rows) {
			int idx = 0;
			Long id = (Long)row[idx++];
			Long instituteId = (Long)row[idx++];
			Long siteId = (Long)row[idx++];

			Set<SiteCpPair> sites = results.computeIfAbsent(id, (k) -> new HashSet<>());
			sites.add(SiteCpPair.make(instituteId, siteId, null));
		}
		
		return results;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<Long, String> getDistributionStatus(List<Long> specimenIds) {
		List<Object[]> rows = getSessionFactory().getCurrentSession()
			.getNamedQuery(GET_LATEST_DISTRIBUTION_AND_RETURN_DATES)
			.setParameterList("specimenIds", specimenIds)
			.list();

		return rows.stream().collect(
			Collectors.toMap(
				row -> (Long)row[0],
				row -> getDistributionStatus((Date)row[1], (Date)row[2])
			));
	}

	@Override
	public String getDistributionStatus(Long specimenId) {
		Map<Long, String> statuses = getDistributionStatus(Collections.singletonList(specimenId));
		return statuses.get(specimenId);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Visit> getSpecimenVisits(SpecimenListCriteria crit) {
		boolean noLabels = CollectionUtils.isEmpty(crit.labels());
		boolean noIds = CollectionUtils.isEmpty(crit.ids());

		if (noLabels && noIds && crit.specimenListId() == null) {
			throw new IllegalArgumentException("No limiting condition on specimens");
		}

		Criteria query = getSessionFactory().getCurrentSession()
			.createCriteria(Visit.class, "visit")
			.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
			.createAlias("visit.specimens", "specimen");

		if (!noIds) {
			addIdsCond(query, crit.ids());
		} else if (!noLabels) {
			addLabelsCond(query, crit.labels());
		}

		addSiteCpsCond(query, crit, false);
		addSpecimenListCond(query, crit);
		return query.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean areDuplicateLabelsPresent() {
		List<Object[]> rows = getSessionFactory().getCurrentSession()
			.getNamedQuery(GET_DUPLICATE_LABEL_COUNT)
			.list();

		return !rows.isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean areDuplicateBarcodesPresent() {
		List<Object[]> rows = getSessionFactory().getCurrentSession()
			.getNamedQuery(GET_DUPLICATE_BARCODE_COUNT)
			.list();

		return !rows.isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<Long, Long> getSpecimenStorageSite(Set<Long> specimenIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_STORAGE_SITE)
			.setParameterList("specimenIds", specimenIds)
			.list();

		// null value for site means virtual specimen
		HashMap<Long, Long> result = new HashMap<>();
		rows.forEach((row) -> result.put((Long)row[0], (Long)row[1]));
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getNonCompliantSpecimens(SpecimenListCriteria crit) {
		if (CollectionUtils.isEmpty(crit.ids()) && crit.specimenListId() == null) {
			return Collections.emptyList();
		}

		Criteria query = getCurrentSession().createCriteria(Specimen.class, "specimen")
			.setProjection(Projections.property("specimen.label"))
			.add(Subqueries.propertyNotIn("specimen.id", getSpecimenIdsQuery(crit)));

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			addIdsCond(query, crit.ids());
		}

		addSpecimenListCond(query, crit);
		return query.list();
	}

	@Override
	public Map<String, Object> getDeletedSpecimenInfo(Long specimenId) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_DELETED_SPMN)
			.setParameter("specimenId", specimenId)
			.list();
		if (rows.isEmpty()) {
			return null;
		}

		Object[] info = rows.iterator().next();
		int idx = -1;
		Map<String, Object> result = new HashMap<>();
		result.put("id", info[++idx]);
		result.put("label", info[++idx]);
		result.put("barcode", info[++idx]);
		return result;
	}

	@Override
	public int activateSpecimen(Long specimenId, boolean includeChildren) {
		String query = includeChildren ? ACTIVATE_HIERARCHY : ACTIVATE_SPMN;
		return getCurrentSession().getNamedQuery(query)
			.setParameter("specimenId", specimenId)
			.executeUpdate();
	}

	private void addIdsCond(Criteria query, List<Long> ids) {
		addInCond(query, "specimen.id", ids);
	}

	private void addLabelCond(Junction condition, String label, MatchMode matchMode) {
		if (matchMode == MatchMode.EXACT) {
			condition.add(Restrictions.eq("specimen.label", label));
		} else {
			condition.add(Restrictions.ilike("specimen.label", label, matchMode));
		}
	}

	private void addLabelsCond(Disjunction condition, List<String> labels) {
		addInCond(condition, "specimen.label", labels);
	}

	private void addLabelsCond(Criteria query, List<String> labels) {
		addInCond(query, "specimen.label", labels);
	}

	private void addBarcodeCond(Junction condition, String barcode, MatchMode matchMode) {
		if (matchMode == MatchMode.EXACT) {
			condition.add(Restrictions.eq("specimen.barcode", barcode));
		} else {
			condition.add(Restrictions.ilike("specimen.barcode", barcode, matchMode));
		}
	}

	private void addBarcodesCond(Disjunction condition, List<String> barcodes) {
		addInCond(condition, "specimen.barcode", barcodes);
	}

	private <T> void addInCond(Disjunction condition, String property, List<T> values) {
		int numValues = values.size();
		for (int i = 0; i < numValues; i += 500) {
			List<T> params = values.subList(i, i + 500 > numValues ? numValues : i + 500);
			condition.add(Restrictions.in(property, params));
		}
	}

	private <T> void addInCond(Criteria query, String property, List<T> values) {
		Disjunction labelIn = Restrictions.disjunction();
		addInCond(labelIn, property, values);
		query.add(labelIn);
	}

	private DetachedCriteria getSpecimenIdsQuery(SpecimenListCriteria crit) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Specimen.class, "specimen")
			.setProjection(Projections.distinct(Projections.property("specimen.id")));
		Criteria query = detachedCriteria.getExecutableCriteria(getCurrentSession());

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			addIdsCond(query, crit.ids());
		} else {
			Disjunction labelOrBarcode = Restrictions.disjunction();
			if (CollectionUtils.isNotEmpty(crit.labels())) {
				if (crit.labels().size() == 1) {
					addLabelCond(labelOrBarcode, crit.labels().iterator().next(), crit.matchMode());
				} else {
					addLabelsCond(labelOrBarcode, crit.labels());
				}
			}

			if (CollectionUtils.isNotEmpty(crit.barcodes())) {
				if (crit.barcodes().size() == 1) {
					addBarcodeCond(labelOrBarcode, crit.barcodes().iterator().next(), crit.matchMode());
				} else {
					addBarcodesCond(labelOrBarcode, crit.barcodes());
				}
			}

			query.add(labelOrBarcode);
		}

		addLastIdCond(query, crit);
		addLineageCond(query, crit);
		addCollectionStatusCond(query, crit);
		addSiteCpsCond(query, crit);
		addCpCond(query, crit);
		addPpidCond(query, crit);
		addCprIdCond(query, crit);
		addVisitIdCond(query, crit);
		addAncestorId(query, crit);
		addSpecimenListCond(query, crit);
		addReservedForDpCond(query, crit);
		addStorageLocationCond(query, crit);
		addSpecimenTypeCond(query, crit);
		addAnatomicSiteCond(query, crit);
		addAvailableSpecimenCond(query, crit);
		addNoQtySpecimenCond(query, crit);
		addTbrSpecimensCond(query, crit);
		return detachedCriteria;
	}

	private void addLastIdCond(Criteria query, SpecimenListCriteria crit) {
		if (crit.lastId() == null || crit.lastId() < 0L) {
			return;
		}

		query.add(Restrictions.gt("id", crit.lastId()));
	}

	private void addLineageCond(Criteria query, SpecimenListCriteria crit) {
		if (crit.lineages() == null || crit.lineages().length == 0) {
			return;
		}

		query.add(Restrictions.in("lineage", Arrays.asList(crit.lineages())));
	}

	private void addCollectionStatusCond(Criteria query, SpecimenListCriteria crit) {
		if (crit.collectionStatuses() == null || crit.collectionStatuses().length == 0) {
			return;
		}

		query.add(Restrictions.in("collectionStatus", Arrays.asList(crit.collectionStatuses())));
	}

	private void addSiteCpsCond(Criteria query, SpecimenListCriteria crit) {
		BiospecimenDaoHelper.getInstance().addSiteCpsCond(query, crit);
	}

	private void addSiteCpsCond(Criteria query, SpecimenListCriteria crit, boolean spmnList) {
		BiospecimenDaoHelper.getInstance().addSiteCpsCond(query, crit, spmnList);
	}

	private void addCpCond(Criteria query, SpecimenListCriteria crit) {
		if (crit.cpId() == null && StringUtils.isBlank(crit.cpShortTitle())) {
			return;
		}

		if (CollectionUtils.isEmpty(crit.siteCps())) {
			if (!query.getAlias().equals("visit")) {
				query.createAlias("specimen.visit", "visit");
			}

			query.createAlias("visit.registration", "cpr")
				.createAlias("cpr.collectionProtocol", "cp");
		}

		if (crit.cpId() != null) {
			query.add(Restrictions.eq("cp.id", crit.cpId()));
		} else {
			query.add(Restrictions.eq("cp.shortTitle", crit.cpShortTitle()));
		}
	}

	private void addPpidCond(Criteria query, SpecimenListCriteria crit) {
		if (StringUtils.isBlank(crit.ppid())) {
			return;
		}

		if (CollectionUtils.isEmpty(crit.siteCps()) && crit.cpId() == null) {
			if (!query.getAlias().equals("visit")) {
				query.createAlias("specimen.visit", "visit");
			}

			query.createAlias("visit.registration", "cpr");
		}

		query.add(Restrictions.ilike("cpr.ppid", crit.ppid(), crit.matchMode()));
	}

	private void addCprIdCond(Criteria query, SpecimenListCriteria crit) {
		if (crit.cprId() == null) {
			return;
		}

		if (CollectionUtils.isEmpty(crit.siteCps()) && crit.cpId() == null && StringUtils.isBlank(crit.ppid())) {
			if (!query.getAlias().equals("visit")) {
				query.createAlias("specimen.visit", "visit");
			}

			query.createAlias("visit.registration", "cpr");
		}

		query.add(Restrictions.eq("cpr.id", crit.cprId()));
	}

	private void addVisitIdCond(Criteria query, SpecimenListCriteria crit) {
		if (crit.visitId() == null && CollectionUtils.isEmpty(crit.visitNames())) {
			return;
		}

		if (CollectionUtils.isEmpty(crit.siteCps()) && crit.cpId() == null && StringUtils.isBlank(crit.ppid()) && crit.cprId() == null) {
			if (!query.getAlias().equals("visit")) {
				query.createAlias("specimen.visit", "visit");
			}
		}

		if (crit.visitId() != null) {
			query.add(Restrictions.eq("visit.id", crit.visitId()));
		}

		if (CollectionUtils.isNotEmpty(crit.visitNames())) {
			query.add(Restrictions.in("visit.name", crit.visitNames()));
		}
	}

	private void addAncestorId(Criteria query, SpecimenListCriteria crit) {
		if (crit.ancestorId() == null) {
			return;
		}

		String sql = String.format(GET_DESCENDENTS_SQL, crit.ancestorId());
		query.add(Restrictions.sqlRestriction("{alias}.identifier in (" + sql + ")"));
	}

	private void addSpecimenListCond(Criteria query, SpecimenListCriteria crit) {
		if (crit.specimenListId() == null) {
			return;
		}

		query.createAlias("specimen.specimenListItems", "listItem")
			.createAlias("listItem.list", "list")
			.add(Restrictions.eq("list.id", crit.specimenListId()));
	}

	private void addReservedForDpCond(Criteria query, SpecimenListCriteria crit) {
		if (crit.reservedForDp() == null) {
			return;
		}

		query.createAlias("specimen.reservedEvent", "reservedDpEvent")
			.createAlias("reservedDpEvent.dp", "dp")
			.add(Restrictions.eq("dp.id", crit.reservedForDp()));
	}

	private void addStorageLocationCond(Criteria query, SpecimenListCriteria crit) {
		if (StringUtils.isBlank(crit.storageLocationSite()) &&
			StringUtils.isBlank(crit.container()) &&
			crit.containerId() == null &&
			crit.ancestorContainerId() == null) {
			return;
		}

		query.createAlias("specimen.position", "pos", JoinType.LEFT_OUTER_JOIN)
			.createAlias("pos.container", "cont", JoinType.LEFT_OUTER_JOIN);

		if (crit.ancestorContainerId() != null) {
			query.createAlias("cont.ancestorContainers", "ancestor")
					.add(Restrictions.eq("ancestor.id", crit.ancestorContainerId()));
		}

		if (StringUtils.isNotBlank(crit.storageLocationSite())) {
			query.createAlias("cont.site", "contSite", JoinType.LEFT_OUTER_JOIN)
				.add(Restrictions.or(
					Restrictions.isNull("pos.id"),
					Restrictions.eq("contSite.name", crit.storageLocationSite())
				));
		}

		if (StringUtils.isNotBlank(crit.container())) {
			query.add(Restrictions.eq("cont.name", crit.container()));
		} else if (crit.containerId() != null) {
			query.add(Restrictions.eq("cont.id", crit.containerId()));
		}
	}

	@SuppressWarnings("unchecked")
	private Specimen getByVisitAndSrId(String hql, Long visitId, Long srId) {
		List<Specimen> specimens = sessionFactory.getCurrentSession()
				.getNamedQuery(hql)
				.setLong("visitId", visitId)
				.setLong("srId", srId)
				.list();
		return specimens.isEmpty() ? null : specimens.iterator().next();
	}

	private String getDistributionStatus(Date execDate, Date returnDate) {
		return (returnDate == null || execDate.after(returnDate)) ? "Distributed" : "Returned";
	}

	private void addSpecimenTypeCond(Criteria query, SpecimenListCriteria crit) {
		if (StringUtils.isBlank(crit.type())) {
			return;
		}

		query.createAlias("specimenType", "typePv")
			.add(Restrictions.eq("typePv.value", crit.type()));
	}

	private void addAnatomicSiteCond(Criteria query, SpecimenListCriteria crit) {
		if (StringUtils.isBlank(crit.anatomicSite())) {
			return;
		}

		query.createAlias("tissueSite", "anatomicSitePv")
			.add(Restrictions.eq("anatomicSitePv.value", crit.anatomicSite()));
	}

	private void addAvailableSpecimenCond(Criteria query, SpecimenListCriteria crit) {
		if (!crit.available()) {
			return;
		}

		query.add(
			Restrictions.disjunction()
				.add(Restrictions.isNull("availableQuantity"))
				.add(Restrictions.gt("availableQuantity", new BigDecimal(0)))
		);
	}

	private void addNoQtySpecimenCond(Criteria query, SpecimenListCriteria crit) {
		if (!crit.noQty()) {
			return;
		}

		query.add(
			Restrictions.disjunction()
				.add(Restrictions.isNull("availableQuantity"))
				.add(Restrictions.eq("availableQuantity", BigDecimal.ZERO))
		);
	}

	private void addTbrSpecimensCond(Criteria query, SpecimenListCriteria crit) {
		if (!crit.includeOnlyTbr()) {
			return;
		}

		query.add(Restrictions.eq("specimen.lineage", Specimen.NEW));
		query.add(Restrictions.sqlRestriction("exists (" + TBR_SPMNS_SQL + ")"));
	}

	private static final String FQN = Specimen.class.getName();
	
	private static final String GET_BY_LABEL = FQN + ".getByLabel";

	private static final String GET_BY_LABEL_AND_CP = FQN + ".getByLabelAndCp";

	private static final String GET_BY_BARCODE = FQN + ".getByBarcode";

	private static final String GET_BY_BARCODE_AND_CP = FQN + ".getByBarcodeAndCp";
	
	private static final String GET_BY_VISIT_AND_SR = FQN + ".getByVisitAndReq";

	private static final String GET_BY_VISIT_N_SR_CODE = FQN + ".getByVisitAndReqCode";

	private static final String GET_PARENT_BY_VISIT_AND_SR = FQN + ".getParentByVisitAndReq";
	
	private static final String GET_BY_IDS = FQN + ".getByIds";
	
	private static final String GET_BY_VISIT_ID = FQN + ".getByVisitId";
	
	private static final String GET_BY_VISIT_NAME = FQN + ".getByVisitName";
	
	private static final String GET_LATEST_DISTRIBUTION_AND_RETURN_DATES = FQN + ".getLatestDistributionAndReturnDates";

	private static final String GET_DUPLICATE_LABEL_COUNT = FQN + ".getDuplicateLabelCount";

	private static final String GET_DUPLICATE_BARCODE_COUNT = FQN + ".getDuplicateBarcodeCount";

	private static final String GET_STORAGE_SITE = FQN + ".getStorageSite";

	private static final String GET_ROOT_ID = FQN + ".getRootId";

	private static final String GET_DELETED_SPMN = FQN + ".getDeletedSpecimen";

	private static final String ACTIVATE_SPMN = FQN + ".activateSpecimen";

	private static final String ACTIVATE_HIERARCHY = FQN + ".activateHierarchy";

	private static final String GET_DESCENDENTS_SQL =
		"select descendent_id from catissue_specimen_hierarchy where ancestor_id = %d and ancestor_id != descendent_id";

	private static final String TBR_SPMNS_SQL =
		"select " +
		"  specimen_id " +
		"from " +
		"  catissue_received_event_param rcv " +
		"  inner join catissue_permissible_value rqual on rqual.identifier = rcv.received_quality_id " +
		"where " +
		"  rqual.value = 'To be Received' and " +
		"  rcv.specimen_id = {alias}.identifier";
}
