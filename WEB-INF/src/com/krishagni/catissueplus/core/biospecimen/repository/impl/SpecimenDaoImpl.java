
package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.LabSpecimenService;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenPooledEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenDao;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.common.repository.Junction;
import com.krishagni.catissueplus.core.common.repository.SubQuery;

public class SpecimenDaoImpl extends AbstractDao<Specimen> implements SpecimenDao {
	public Class<?> getType() {
		return Specimen.class;
	}

	@Override
	public List<Specimen> getSpecimens(SpecimenListCriteria crit) {
		Criteria<Specimen> query = createCriteria(Specimen.class, "specimen");
		query.add(query.in("specimen.id", getSpecimenIdsQuery(crit, query)));

		if (crit.lastId() == null && crit.specimenListId() != null) {
			query.join("specimen.specimenListItems", "listItem")
				.join("listItem.list", "list")
				.add(query.eq("list.id", crit.specimenListId()))
				.addOrder(query.asc("listItem.id"));
		} else {
			query.addOrder(query.asc("specimen.id"));
		}

		return crit.limitItems() ? query.list(crit.startAt(), crit.maxResults()) : query.list();
	}

	public Integer getSpecimensCount(SpecimenListCriteria crit) {
		Criteria<Specimen> query = createCriteria(Specimen.class, "s");
		query.add(query.in("s.id", getSpecimenIdsQuery(crit, query)));
		return query.getCount("s.id").intValue();
	}

	@Override
	public List<Long> getSpecimenIds(SpecimenListCriteria crit) {
		Criteria<Long> query = createCriteria(Specimen.class, Long.class, "s");
		return query.add(query.in("s.id", getSpecimenIdsQuery(crit, query))).list();
	}

	@Override
	public Long getMaxSpecimenId(SpecimenListCriteria crit) {
		Criteria<Long> query = createCriteria(Specimen.class, Long.class, "specimen");
		query.select(query.max("specimen.id"))
			.in("specimen.id", getSpecimenIdsQuery(crit, query));
		return query.uniqueResult();
	}

	@Override
	public Specimen getByLabel(String label) {
		List<Specimen> specimens = createNamedQuery(GET_BY_LABEL, Specimen.class)
			.setParameter("label", label)
			.list();
		return specimens.isEmpty() ? null : specimens.iterator().next();
	}

	@Override
	public Specimen getByLabelAndCp(String cpShortTitle, String label) {
		List<Specimen> specimens = createNamedQuery(GET_BY_LABEL_AND_CP, Specimen.class)
			.setParameter("label", label)
			.setParameter("cpShortTitle", cpShortTitle)
			.list();
		return specimens.isEmpty() ? null : specimens.iterator().next();
	}

	@Override
	public Specimen getByAdditionalLabel(String label) {
		List<Specimen> specimens = createNamedQuery(GET_BY_ADDL_LABEL, Specimen.class)
			.setParameter("label", label)
			.list();
		return specimens.isEmpty() ? null : specimens.iterator().next();
	}

	@Override
	public Specimen getByAdditionalLabelAndCp(String cpShortTitle, String label) {
		List<Specimen> specimens = createNamedQuery(GET_BY_ADDL_LABEL_AND_CP, Specimen.class)
			.setParameter("label", label)
			.setParameter("cpShortTitle", cpShortTitle)
			.list();

		return specimens.isEmpty() ? null : specimens.iterator().next();
	}

	@Override
	public Specimen getSpecimenByVisitAndSr(Long visitId, Long srId) {
		return getByVisitAndSrId(GET_BY_VISIT_AND_SR, visitId, srId);
	}

	@Override
	public List<Specimen> getByVisitAndSrCode(Long visitId, Collection<String> reqCodes) {
		return createNamedQuery(GET_BY_VISIT_N_SR_CODE, Specimen.class)
			.setParameter("visitId", visitId)
			.setParameterList("srCodes", reqCodes)
			.list();
	}

	@Override
	public Specimen getParentSpecimenByVisitAndSr(Long visitId, Long srId) {
		return getByVisitAndSrId(GET_PARENT_BY_VISIT_AND_SR, visitId, srId);
	}

	@Override
	public Specimen getByBarcode(String barcode) {
		List<Specimen> specimens = createNamedQuery(GET_BY_BARCODE, Specimen.class)
			.setParameter("barcode", barcode)
			.list();
		return specimens.isEmpty() ? null : specimens.iterator().next();
	}

	@Override
	public Specimen getByBarcodeAndCp(String cpShortTitle, String barcode) {
		List<Specimen> specimens = createNamedQuery(GET_BY_BARCODE_AND_CP, Specimen.class)
			.setParameter("barcode", barcode)
			.setParameter("cpShortTitle", cpShortTitle)
			.list();
		return specimens.isEmpty() ? null : specimens.iterator().next();
	}

	@Override
	public Long getPrimarySpecimen(Long specimenId) {
		return createNamedQuery(GET_ROOT_ID, Long.class)
			.setParameter("specimenId", specimenId)
			.uniqueResult();
	}

	@Override
	public List<Specimen> getSpecimensByIds(List<Long> specimenIds) {
		return createNamedQuery(GET_BY_IDS, Specimen.class)
			.setParameterList("specimenIds", specimenIds)
			.list();
	}

	@Override
	public List<Specimen> getByLabels(Collection<Pair<String, String>> cpLabels) {
		Criteria<Specimen> query = createCriteria(Specimen.class, "specimen")
			.join("specimen.collectionProtocol", "cp");

		Disjunction disjunction = query.disjunction();
		for (Pair<String, String> cpLabel : cpLabels) {
			disjunction.add(
				query.and(
					query.eq("cp.shortTitle", cpLabel.first()),
					query.eq("specimen.label", cpLabel.second())
				)
			);
		}

		return query.add(disjunction).list();
	}

	@Override
	public List<Specimen> getByBarcodes(Collection<Pair<String, String>> cpBarcodes) {
		Criteria<Specimen> query = createCriteria(Specimen.class, "specimen")
			.join("specimen.collectionProtocol", "cp");

		Disjunction disjunction = query.disjunction();
		for (Pair<String, String> cpBarcode : cpBarcodes) {
			disjunction.add(
				query.and(
					query.eq("cp.shortTitle", cpBarcode.first()),
					query.eq("specimen.barcode", cpBarcode.second())
				)
			);
		}

		return query.add(disjunction).list();
	}

	@Override
	public List<Specimen> getSpecimensByVisitId(Long visitId) {
		return createNamedQuery(GET_BY_VISIT_ID, Specimen.class)
			.setParameter("visitId", visitId)
			.list();
	}

	@Override
	public List<Specimen> getSpecimensByVisitName(String visitName) {
		return createNamedQuery(GET_BY_VISIT_NAME, Specimen.class)
			.setParameter("visitName", visitName)
			.list();
	}

	@Override
	public Map<String, Object> getCprAndVisitIds(String key, Object value) {
		Criteria<Object[]> query = createCriteria(Specimen.class, Object[].class, "s");
		List<Object[]> rows = query.join("s.visit", "visit")
			.join("visit.registration", "cpr")
			.join("cpr.collectionProtocol", "cp")
			.select("cp.id", "cpr.id", "visit.id", "s.id")
			.add(query.eq(key, value))
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

	@Override
	public Map<Long, Set<SiteCpPair>> getSpecimenSites(Set<Long> specimenIds) {
		Criteria<Object[]> query = createCriteria(Specimen.class, Object[].class, "s")
			.join("s.visit", "visit")
			.join("visit.registration", "cpr")
			.join("cpr.collectionProtocol", "cp")
			.join("cp.sites", "cpSite")
			.join("cpSite.site", "site")
			.join("site.institute", "institute");

		query.select("s.id", "institute.id", "site.id")
			.add(query.in("s.id", specimenIds));

		List<Object []> rows = query.list();
		Map<Long, Set<SiteCpPair>> results = new HashMap<>();
		for (Object[] row: rows) {
			int idx = -1;
			Long id = (Long)row[++idx];
			Long instituteId = (Long)row[++idx];
			Long siteId = (Long)row[++idx];

			Set<SiteCpPair> sites = results.computeIfAbsent(id, (k) -> new HashSet<>());
			sites.add(SiteCpPair.make(instituteId, siteId, null));
		}
		
		return results;
	}

	@Override
	public Map<Long, String> getDistributionStatus(List<Long> specimenIds) {
		return createNamedQuery(GET_LATEST_DISTRIBUTION_AND_RETURN_DATES, Object[].class)
			.setParameterList("specimenIds", specimenIds)
			.list().stream().collect(
				Collectors.toMap(
					row -> (Long)row[0],
					row -> getDistributionStatus((Date)row[1], (Date)row[2])
				)
			);
	}

	@Override
	public String getDistributionStatus(Long specimenId) {
		Map<Long, String> statuses = getDistributionStatus(Collections.singletonList(specimenId));
		return statuses.get(specimenId);
	}

	@Override
	public List<Visit> getSpecimenVisits(SpecimenListCriteria crit) {
		boolean noLabels = CollectionUtils.isEmpty(crit.labels());
		boolean noIds = CollectionUtils.isEmpty(crit.ids());

		if (noLabels && noIds && crit.specimenListId() == null) {
			throw new IllegalArgumentException("No limiting condition on specimens");
		}

		Criteria<Visit> query = createCriteria(Visit.class, "visit")
			.join("visit.specimens", "specimen")
			.distinct();

		if (!noIds) {
			addIdsCond(query, crit.ids());
		} else if (!noLabels) {
			addLabelsCond(query, crit.labels());
		}

		addSiteCpsCond(query, crit, false);
		addSpecimenListCond(query, crit);
		return query.list();
	}

	@Override
	public boolean areDuplicateLabelsPresent() {
		return !createNamedQuery(GET_DUPLICATE_LABEL_COUNT, Object[].class).list().isEmpty();
	}

	@Override
	public boolean areDuplicateBarcodesPresent() {
		return !createNamedQuery(GET_DUPLICATE_BARCODE_COUNT, Object[].class).list().isEmpty();
	}

	@Override
	public Map<Long, Long> getSpecimenStorageSite(Set<Long> specimenIds) {
		List<Object[]> rows = createNamedQuery(GET_STORAGE_SITE, Object[].class)
			.setParameterList("specimenIds", specimenIds)
			.list();

		// null value for site means virtual specimen
		HashMap<Long, Long> result = new HashMap<>();
		rows.forEach((row) -> result.put((Long)row[0], (Long)row[1]));
		return result;
	}

	@Override
	public List<String> getNonCompliantSpecimens(SpecimenListCriteria crit) {
		if (CollectionUtils.isEmpty(crit.ids()) && crit.specimenListId() == null) {
			return Collections.emptyList();
		}

		Criteria<String> query = createCriteria(Specimen.class, String.class, "specimen")
			.select("specimen.label");
		query.add(query.notIn("specimen.id", getSpecimenIdsQuery(crit, query)));
		if (CollectionUtils.isNotEmpty(crit.ids())) {
			addIdsCond(query, crit.ids());
		}

		addSpecimenListCond(query, crit);
		return query.list();
	}

	@Override
	public Map<String, Object> getDeletedSpecimenInfo(Long specimenId) {
		List<Object[]> rows = createNamedQuery(GET_DELETED_SPMN, Object[].class)
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
		return createNamedQuery(query, Integer.class)
			.setParameter("specimenId", specimenId)
			.executeUpdate();
	}

	@Override
	public void savedPooledEvent(SpecimenPooledEvent event) {
		getCurrentSession().saveOrUpdate(event);
		getCurrentSession().flush();
	}

	@Override
	public List<Map<String, String>> getKitLabels(Visit visit, SpecimenRequirement sr) {
		List<Map<String, String>> kitLabels = new ArrayList<>();

		List<Object[]> rows = createQuery(GET_KIT_LABELS_HQL, Object[].class)
			.setParameter("visitId", visit.getId())
			.setParameter("cpeId", visit.getCpEvent().getId())
			.setParameter("srId", sr.getId())
			.list();
		for (Object[] row : rows) {
			Map<String, String> kit = new HashMap<>();
			kit.put("label", (String) row[0]);
			kit.put("barcode", (String) row[1]);
			kitLabels.add(kit);
		}

		return kitLabels;
	}

	// sr_id => label
	@Override
	public Map<Long, Map<String, String>> getKitLabels(Visit visit) {
		List<Object[]> rows = createQuery(GET_VISIT_KIT_LABELS_HQL, Object[].class)
			.setParameter("visitId", visit.getId())
			.setParameter("eventId", visit.getCpEvent().getId())
			.list();

		Map<Long, Map<String, String>> labels = new HashMap<>();
		for (Object[] row : rows) {
			Map<String, String> kit = new HashMap<>();
			kit.put("label", (String) row[1]);
			kit.put("barcode", (String) row[2]);
			labels.put((Long) row[0], kit);
		}

		return labels;
	}

	@Override
	public List<Specimen> getDescendantSpecimens(List<Long> ancestorIds, String labelField, List<String> labels, String lineage, String status) {
		List<Object[]> result = createNamedQuery(GET_DESCENDANT_SPMN_IDS, Object[].class)
			.setParameterList("parentIds", ancestorIds)
			.list();
		if (result.isEmpty()) {
			return Collections.emptyList();
		}

		Map<Long, Long> descendantAncestorIdsMap = new HashMap<>();
		for (Object[] pair : result) {
			descendantAncestorIdsMap.put((Long) pair[1], (Long) pair[0]);
		}

		Criteria<Specimen> query = createCriteria(Specimen.class, "s");
		query.add(query.in("s.id", descendantAncestorIdsMap.keySet()));
		if (CollectionUtils.isNotEmpty(labels)) {
			if (StringUtils.isBlank(labelField) || labelField.equals("specimen.label")) {
				addInCond(query, "s.label", labels);
			} else if (labelField.equals("specimen.barcode")) {
				addInCond(query, "s.barcode", labels);
			} else if (labelField.equals("specimen.additionalLabel")) {
				addInCond(query, "s.additionalLabel", labels);
			}
		}

		if (Specimen.ALIQUOT.equals(lineage)) {
			query.add(query.eq("s.lineage", Specimen.ALIQUOT));
		} else if (Specimen.DERIVED.equals(lineage)) {
			query.add(query.eq("s.lineage", Specimen.DERIVED));
		}

		if (StringUtils.isNotBlank(status)) {
			query.add(query.eq("s.collectionStatus", status));
		}

		boolean sorted = true;
		List<Specimen> specimens = query.list();
		if (CollectionUtils.isNotEmpty(labels)) {
			if (StringUtils.isBlank(labelField) || labelField.equals("specimen.label")) {
				specimens = Specimen.sortByLabels(specimens, labels);
			} else if (labelField.equals("specimen.barcode")) {
				specimens = Specimen.sortByBarcodes(specimens, labels);
			} else if (labelField.equals("specimen.additionalLabel")) {
				specimens = Specimen.sortByAdditionalLabels(specimens, labels);
			} else {
				sorted = false;
			}
		} else {
			sorted = false;
		}

		if (!sorted) {
			specimens = specimens.stream().sorted(Comparator.comparing(BaseEntity::getId)).collect(Collectors.toList());
		}

		for (Specimen specimen : specimens) {
			specimen.setAncestorId(descendantAncestorIdsMap.get(specimen.getId()));
		}

		return specimens;
	}

	@Override
	public LabSpecimenService getLabSpecimenService(Long id) {
		Criteria<LabSpecimenService> query = createCriteria(LabSpecimenService.class, "ls");
		return query.add(query.eq("ls.id", id)).uniqueResult();
	}

	@Override
	public List<LabSpecimenService> getLabSpecimenServices(Long specimenId) {
		Criteria<LabSpecimenService> query = createCriteria(LabSpecimenService.class, "ls")
			.join("ls.specimen", "spmn");
		return query.add(query.eq("spmn.id", specimenId))
			.addOrder(query.desc("ls.serviceDate"))
			.list();
	}

	@Override
	public void saveOrUpdate(LabSpecimenService labSpecimenService) {
		sessionFactory.getCurrentSession().saveOrUpdate(labSpecimenService);
	}

	@Override
	public void delete(LabSpecimenService svc) {
		sessionFactory.getCurrentSession().delete(svc);
	}

	@Override
	public void deleteServices(Long specimenId) {
		createNamedQuery(DELETE_SERVICES)
			.setParameter("specimenId", specimenId)
			.executeUpdate();
	}

	private void addIdsCond(AbstractCriteria<?, ?> query, List<Long> ids) {
		addInCond(query, "specimen.id", ids);
	}

	private void addLabelCond(AbstractCriteria<?, ?> query, Junction condition, String label, boolean exactMatch) {
		if (exactMatch) {
			condition.add(query.eq("specimen.label", label));
		} else {
			condition.add(query.ilike("specimen.label", label));
		}
	}

	private void addLabelsCond(AbstractCriteria<?, ?> query, Disjunction condition, List<String> labels) {
		addInCond(query, condition, "specimen.label", labels);
	}

	private void addLabelsCond(AbstractCriteria<?, ?> query, List<String> labels) {
		addInCond(query, "specimen.label", labels);
	}

	private void addBarcodeCond(AbstractCriteria<?, ?> query, Junction condition, String barcode, boolean exactMatch) {
		if (exactMatch) {
			condition.add(query.eq("specimen.barcode", barcode));
		} else {
			condition.add(query.ilike("specimen.barcode", barcode));
		}
	}

	private void addBarcodesCond(AbstractCriteria<?, ?> query, Disjunction condition, List<String> barcodes) {
		addInCond(query, condition, "specimen.barcode", barcodes);
	}

	private <T> void addInCond(AbstractCriteria<?, ?> query, Disjunction condition, String property, List<T> values) {
		int numValues = values.size();
		for (int i = 0; i < numValues; i += 500) {
			List<T> params = values.subList(i, Math.min(i + 500, numValues));
			condition.add(query.in(property, params));
		}
	}

	private <T> void addInCond(AbstractCriteria<?, ?> query, String property, List<T> values) {
		Disjunction labelIn = query.disjunction();
		addInCond(query, labelIn, property, values);
		query.add(labelIn);
	}

	private SubQuery<Long> getSpecimenIdsQuery(SpecimenListCriteria crit, AbstractCriteria<?, ?> mainQuery) {
		SubQuery<Long> query = mainQuery.createSubQuery(Specimen.class, "specimen")
			.distinct().select("specimen.id");

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			addIdsCond(query, crit.ids());
		} else {
			Disjunction labelOrBarcode = query.disjunction();
			if (CollectionUtils.isNotEmpty(crit.labels())) {
				if (crit.labels().size() == 1) {
					addLabelCond(query, labelOrBarcode, crit.labels().iterator().next(), crit.exactMatch());
				} else {
					addLabelsCond(query, labelOrBarcode, crit.labels());
				}
			}

			if (CollectionUtils.isNotEmpty(crit.barcodes())) {
				if (crit.barcodes().size() == 1) {
					addBarcodeCond(query, labelOrBarcode, crit.barcodes().iterator().next(), crit.exactMatch());
				} else {
					addBarcodesCond(query, labelOrBarcode, crit.barcodes());
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
		return query;
	}

	private void addLastIdCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (crit.lastId() == null || crit.lastId() < 0L) {
			return;
		}

		query.add(query.gt("specimen.id", crit.lastId()));
		if (crit.enableIdRange()) {
			query.add(query.le("specimen.id", crit.lastId() + crit.rangeFactor() * crit.maxResults()));
		}
	}

	private void addLineageCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (crit.lineages() == null || crit.lineages().length == 0) {
			return;
		}

		query.add(query.in("specimen.lineage", Arrays.asList(crit.lineages())));
	}

	private void addCollectionStatusCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (crit.collectionStatuses() == null || crit.collectionStatuses().length == 0) {
			return;
		}

		query.add(query.in("specimen.collectionStatus", Arrays.asList(crit.collectionStatuses())));
	}

	private void addSiteCpsCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		BiospecimenDaoHelper.getInstance().addSiteCpsCond(query, crit);
	}

	private void addSiteCpsCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit, boolean spmnList) {
		BiospecimenDaoHelper.getInstance().addSiteCpsCond(query, crit, spmnList);
	}

	private void addCpCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (crit.cpId() == null && StringUtils.isBlank(crit.cpShortTitle())) {
			return;
		}

		if (CollectionUtils.isEmpty(crit.siteCps())) {
			if (!query.hasJoin("visit")) {
				query.join("specimen.visit", "visit");
			}

			query.join("visit.registration", "cpr")
				.join("cpr.collectionProtocol", "cp");
		}

		if (crit.cpId() != null) {
			query.add(query.eq("cp.id", crit.cpId()));
		} else {
			query.add(query.eq("cp.shortTitle", crit.cpShortTitle()));
		}
	}

	private void addPpidCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (StringUtils.isBlank(crit.ppid())) {
			return;
		}

		if (CollectionUtils.isEmpty(crit.siteCps()) && crit.cpId() == null) {
			if (!query.hasJoin("visit")) {
				query.join("specimen.visit", "visit");
			}

			query.join("visit.registration", "cpr");
		}

		query.add(query.ilike("cpr.ppid", crit.ppid()));
	}

	private void addCprIdCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (crit.cprId() == null) {
			return;
		}

		if (CollectionUtils.isEmpty(crit.siteCps()) && crit.cpId() == null && StringUtils.isBlank(crit.ppid())) {
			if (!query.hasJoin("visit")) {
				query.join("specimen.visit", "visit");
			}

			query.join("visit.registration", "cpr");
		}

		query.add(query.eq("cpr.id", crit.cprId()));
	}

	private void addVisitIdCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (crit.visitId() == null && CollectionUtils.isEmpty(crit.visitNames())) {
			return;
		}

		if (CollectionUtils.isEmpty(crit.siteCps()) && crit.cpId() == null && StringUtils.isBlank(crit.ppid()) && crit.cprId() == null) {
			if (!query.hasJoin("visit")) {
				query.join("specimen.visit", "visit");
			}
		}

		if (crit.visitId() != null) {
			query.add(query.eq("visit.id", crit.visitId()));
		}

		if (CollectionUtils.isNotEmpty(crit.visitNames())) {
			query.add(query.in("visit.name", crit.visitNames()));
		}
	}

	private void addAncestorId(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (crit.ancestorId() == null) {
			return;
		}

		String sql = String.format(GET_DESCENDENTS_SQL, crit.ancestorId());
		List<Long> descendentIds = createNativeQuery(sql, Long.class).list();
		addInCond(query, "specimen.id", descendentIds);
	}

	private void addSpecimenListCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (crit.specimenListId() == null) {
			return;
		}

		query.join("specimen.specimenListItems", "listItem")
			.join("listItem.list", "list")
			.add(query.eq("list.id", crit.specimenListId()));
	}

	private void addReservedForDpCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (crit.reservedForDp() == null) {
			return;
		}

		query.join("specimen.reservedEvent", "reservedDpEvent")
			.join("reservedDpEvent.dp", "dp")
			.add(query.eq("dp.id", crit.reservedForDp()));
	}

	private void addStorageLocationCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (StringUtils.isBlank(crit.storageLocationSite()) &&
			StringUtils.isBlank(crit.container()) &&
			crit.containerId() == null &&
			crit.ancestorContainerId() == null) {
			return;
		}

		query.leftJoin("specimen.position", "pos")
			.leftJoin("pos.container", "cont");

		if (crit.ancestorContainerId() != null) {
			query.join("cont.ancestorContainers", "ancestor")
				.add(query.eq("ancestor.id", crit.ancestorContainerId()));
		}

		if (StringUtils.isNotBlank(crit.storageLocationSite())) {
			query.leftJoin("cont.site", "contSite")
				.add(query.or(
					query.isNull("pos.id"),
					query.eq("contSite.name", crit.storageLocationSite())
				));
		}

		if (StringUtils.isNotBlank(crit.container())) {
			query.add(query.eq("cont.name", crit.container()));
		} else if (crit.containerId() != null) {
			query.add(query.eq("cont.id", crit.containerId()));
		}
	}

	private Specimen getByVisitAndSrId(String hql, Long visitId, Long srId) {
		List<Specimen> specimens = getCurrentSession().getNamedQuery(hql)
			.setParameter("visitId", visitId)
			.setParameter("srId", srId)
			.list();
		return specimens.isEmpty() ? null : specimens.iterator().next();
	}

	private String getDistributionStatus(Date execDate, Date returnDate) {
		return (returnDate == null || execDate.after(returnDate)) ? "Distributed" : "Returned";
	}

	private void addSpecimenTypeCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (StringUtils.isBlank(crit.type())) {
			return;
		}

		query.join("specimen.specimenType", "typePv")
			.add(query.eq("typePv.value", crit.type()));
	}

	private void addAnatomicSiteCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (StringUtils.isBlank(crit.anatomicSite())) {
			return;
		}

		query.join("specimen.tissueSite", "anatomicSitePv")
			.add(query.eq("anatomicSitePv.value", crit.anatomicSite()));
	}

	private void addAvailableSpecimenCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (!crit.available()) {
			return;
		}

		query.add(
			query.disjunction()
				.add(query.isNull("specimen.availableQuantity"))
				.add(query.gt("specimen.availableQuantity", new BigDecimal(0)))
		);
	}

	private void addNoQtySpecimenCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (!crit.noQty()) {
			return;
		}

		query.add(
			query.disjunction()
				.add(query.isNull("specimen.availableQuantity"))
				.add(query.eq("specimen.availableQuantity", BigDecimal.ZERO))
		);
	}

	private void addTbrSpecimensCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		if (!crit.includeOnlyTbr()) {
			return;
		}

		query.join("specimen.collRecvDetailsList", "collRecvEvent")
			.add(query.eq("specimen.lineage", Specimen.NEW))
			.add(query.eq("collRecvEvent.recvQuality", Specimen.TO_BE_RECEIVED));
	}

	private static final String FQN = Specimen.class.getName();
	
	private static final String GET_BY_LABEL = FQN + ".getByLabel";

	private static final String GET_BY_LABEL_AND_CP = FQN + ".getByLabelAndCp";

	private static final String GET_BY_ADDL_LABEL = FQN + ".getByAddlLabel";

	private static final String GET_BY_ADDL_LABEL_AND_CP = FQN + ".getByAddlLabelAndCp";


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

	private static final String DELETE_SERVICES = LabSpecimenService.class.getName() + ".deleteServices";

	private static final String GET_DESCENDENTS_SQL =
		"select descendent_id from catissue_specimen_hierarchy where ancestor_id = %d and ancestor_id != descendent_id";

	private static final String GET_KIT_LABELS_HQL =
		"select " +
		"  si.barcode, si.altBarcode " +
		"from " +
		"  com.krishagni.openspecimen.supplies.domain.SupplyItem si " +
		"  join si.rootItem ri " +
		"  join si.cpe cpe " +
		"  join si.sr sr " +
		"where " +
		"  ri.entityType = 'visit' and " +
		"  ri.entityId = :visitId and " +
		"  si.entityType = 'specimen' and " +
		"  si.entityId is null and " +
		"  cpe.id = :cpeId and " +
		"  sr.id = :srId";

	private static final String GET_VISIT_KIT_LABELS_HQL =
		"select " +
		"  sr.id as srId, si.barcode as barcode, si.altBarcode " +
		"from " +
		"  com.krishagni.openspecimen.supplies.domain.SupplyItem si " +
		"  join si.rootItem ri " +
		"  join si.cpe cpe " +
		"  join si.sr sr " +
		"where " +
		"  ri.entityType = 'visit' and " +
		"  ri.entityId = :visitId and " +
		"  si.entityType = 'specimen' and " +
		"  cpe.id = :eventId";

	private static final String GET_DESCENDANT_SPMN_IDS = FQN + ".getDescendantSpecimenIds";
}
