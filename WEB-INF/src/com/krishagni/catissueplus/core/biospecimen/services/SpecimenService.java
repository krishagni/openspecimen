
package com.krishagni.catissueplus.core.biospecimen.services;

import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.events.CpEntityDeleteCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.PrintSpecimenLabelDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenAliquotsSpec;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenServiceDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenStatusDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.events.BulkEntityDetail;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.LabelPrintJobSummary;
import com.krishagni.catissueplus.core.common.events.LabelTokenDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.LabelPrinter;

public interface SpecimenService {
	public ResponseEvent<SpecimenDetail> getSpecimen(RequestEvent<SpecimenQueryCriteria> req);

	public ResponseEvent<List<? extends SpecimenInfo>> getSpecimens(RequestEvent<SpecimenListCriteria> req);

	public ResponseEvent<List<? extends SpecimenInfo>> getSpecimensById(List<Long> ids, boolean includeExtensions);

	ResponseEvent<List<? extends SpecimenInfo>> getSpecimensById(List<Long> ids, boolean includeExtensions, boolean minimalInfo);

	public ResponseEvent<List<SpecimenInfo>> getPrimarySpecimensByCp(RequestEvent<Long> req);
	
	public ResponseEvent<SpecimenDetail> createSpecimen(RequestEvent<SpecimenDetail> req);
	
	public ResponseEvent<SpecimenDetail> updateSpecimen(RequestEvent<SpecimenDetail> req);

	public ResponseEvent<List<SpecimenInfo>> updateSpecimens(RequestEvent<List<SpecimenDetail>> req);

	public ResponseEvent<List<SpecimenInfo>> bulkUpdateSpecimens(RequestEvent<BulkEntityDetail<SpecimenDetail>> req);
	
	public ResponseEvent<List<SpecimenDetail>> updateSpecimensStatus(RequestEvent<List<SpecimenStatusDetail>> req);

	public ResponseEvent<List<SpecimenInfo>> deleteSpecimens(RequestEvent<List<CpEntityDeleteCriteria>> req);

	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<SpecimenQueryCriteria> req);
	
	public ResponseEvent<List<SpecimenDetail>> collectSpecimens(RequestEvent<List<SpecimenDetail>> req);
	
	public ResponseEvent<List<SpecimenDetail>> createAliquots(RequestEvent<SpecimenAliquotsSpec> req);

	public ResponseEvent<SpecimenDetail> createDerivative(RequestEvent<SpecimenDetail> derivedReq);

	public ResponseEvent<SpecimenDetail> createPooledSpecimen(RequestEvent<SpecimenDetail> req);

	public ResponseEvent<Boolean> doesSpecimenExists(RequestEvent<SpecimenQueryCriteria> req);
	
	public ResponseEvent<LabelPrintJobSummary> printSpecimenLabels(RequestEvent<PrintSpecimenLabelDetail> req);

	public ResponseEvent<List<LabelTokenDetail>> getPrintLabelTokens();

	ResponseEvent<List<SpecimenServiceDetail>> getServices(RequestEvent<SpecimenQueryCriteria> req);

	ResponseEvent<List<SpecimenServiceDetail>> getMultiSpecimenServices(RequestEvent<SpecimenListCriteria> req);

	ResponseEvent<SpecimenServiceDetail> getService(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<SpecimenServiceDetail> addOrUpdateService(RequestEvent<SpecimenServiceDetail> req);

	ResponseEvent<SpecimenServiceDetail> deleteService(RequestEvent<EntityQueryCriteria> req);

	
	/** Used mostly by plugins **/
	public LabelPrinter<Specimen> getLabelPrinter();
	
	/** Mostly present for UI **/
	public ResponseEvent<Map<String, Object>> getCprAndVisitIds(RequestEvent<Long> req);

	public Long getPrimarySpecimen(SpecimenQueryCriteria crit);

	//
	// Mostly used for administrative purpose
	//
	public ResponseEvent<SpecimenDetail> undeleteSpecimen(RequestEvent<SpecimenQueryCriteria> req);

	//
	// For internal and plugin usage purpose
	//
	public List<Specimen> getSpecimensByLabel(List<String> labels);

	public List<Specimen> getSpecimensById(List<Long> ids);

	Specimen updateSpecimen(Specimen existing, Specimen newSpmn);

	List<SpecimenInfo> bulkUpdateSpecimens(BulkEntityDetail<SpecimenDetail> req);
}
