
package com.krishagni.catissueplus.core.biospecimen.services;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.events.CpEntityDeleteCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedVisitDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PrintVisitNameDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SprDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SprLockDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSearchDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsListCriteria;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.LabelPrintJobSummary;
import com.krishagni.catissueplus.core.common.events.LabelTokenDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.LabelPrinter;

public interface VisitService {
	ResponseEvent<VisitDetail> getVisit(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<List<VisitDetail>> getVisits(RequestEvent<VisitsListCriteria> criteria);

	ResponseEvent<VisitDetail> addVisit(RequestEvent<VisitDetail> req);
	
	ResponseEvent<VisitDetail> updateVisit(RequestEvent<VisitDetail> req);
	
	ResponseEvent<VisitDetail> patchVisit(RequestEvent<VisitDetail> req);
	
	ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<EntityQueryCriteria> req);
	
	ResponseEvent<VisitDetail> deleteVisit(RequestEvent<CpEntityDeleteCriteria> req);
			
	ResponseEvent<VisitSpecimenDetail> collectVisitAndSpecimens(RequestEvent<VisitSpecimenDetail> req);

	ResponseEvent<LabelPrintJobSummary> printVisitNames(RequestEvent<PrintVisitNameDetail> req);

	//
	// SPR APIs
	//
	ResponseEvent<FileDetail> getSpr(RequestEvent<FileDetail> req);
	
	ResponseEvent<String> uploadSprFile(RequestEvent<SprDetail> req);

	ResponseEvent<String> updateSprText(RequestEvent<SprDetail> req);

	ResponseEvent<Boolean> deleteSprFile(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<SprLockDetail> updateSprLockStatus(RequestEvent<SprLockDetail> req);

	//
	// Visits lookup API
	//
	ResponseEvent<List<MatchedVisitDetail>> getMatchingVisits(RequestEvent<VisitSearchDetail> req);

	//
	// Internal APIs
	//
	List<LabelTokenDetail> getPrintLabelTokens();

	LabelPrinter<Visit> getLabelPrinter();

	List<Visit> getVisitsByName(List<String> visitNames);

	List<Visit> getSpecimenVisits(List<String> specimenLabels);

	Visit addVisit(VisitDetail input, boolean checkPermission);
}
