
package com.krishagni.catissueplus.core.biospecimen.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.krishagni.catissueplus.core.biospecimen.domain.LabSpecimenService;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenPooledEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface SpecimenDao extends Dao<Specimen> {
	List<Specimen> getSpecimens(SpecimenListCriteria crit);

	Integer getSpecimensCount(SpecimenListCriteria crit);

	List<Long> getSpecimenIds(SpecimenListCriteria crit);

	Long getMaxSpecimenId(SpecimenListCriteria crit);
	
	Specimen getByLabel(String label);

	Specimen getByLabelAndCp(String cpShortTitle, String label);

	Specimen getByAdditionalLabel(String label);

	Specimen getByAdditionalLabelAndCp(String cpShortTitle, String label);

	Specimen getByBarcode(String barcode);

	Specimen getByBarcodeAndCp(String cpShortTitle, String barcode);

	Long getPrimarySpecimen(Long specimenId);
	
	List<Specimen> getSpecimensByIds(List<Long> specimenIds);

	List<Specimen> getByLabels(Collection<Pair<String, String>> cpLabels); // [{cpShortTitle, label}]

	List<Specimen> getByBarcodes(Collection<Pair<String, String>> cpBarcodes); // [{cpShortTitle, barcode}]
	
	List<Specimen> getSpecimensByVisitId(Long visitId);
	
	List<Specimen> getSpecimensByVisitName(String visitName);
	
	Specimen getSpecimenByVisitAndSr(Long visitId, Long srId);

	List<Specimen> getByVisitAndSrCode(Long visitId, Collection<String> reqCodes);

	Specimen getParentSpecimenByVisitAndSr(Long visitId, Long srId);

	Map<String, Object> getCprAndVisitIds(String key, Object value);
	
	Map<Long, Set<SiteCpPair>> getSpecimenSites(Set<Long> specimenIds);

	Map<Long, String> getDistributionStatus(List<Long> specimenIds);

	String getDistributionStatus(Long specimenId);

	List<Visit> getSpecimenVisits(SpecimenListCriteria crit);

	boolean areDuplicateLabelsPresent();

	boolean areDuplicateBarcodesPresent();

	Map<Long, Long> getSpecimenStorageSite(Set<Long> specimenIds);

	List<String> getNonCompliantSpecimens(SpecimenListCriteria crit);

	Map<String, Object> getDeletedSpecimenInfo(Long specimenId);

	int activateSpecimen(Long specimenId, boolean includeChildren);

	void savedPooledEvent(SpecimenPooledEvent event);

	List<Map<String, String>> getKitLabels(Visit visit, SpecimenRequirement sr);

	Map<Long, Map<String, String>> getKitLabels(Visit visit);

	List<Specimen> getDescendantSpecimens(List<Long> ancestorIds, String labelField, List<String> labels, String lineage, String status);

	LabSpecimenService getLabSpecimenService(Long id);

	List<LabSpecimenService> getLabSpecimenServices(Long specimenId);

	Map<Long, BigDecimal> getLabSpecimenServicesRate(Long specimenId);

	void saveOrUpdate(LabSpecimenService svc);

	void delete(LabSpecimenService svc);

	void deleteServices(Long specimenId);
}
