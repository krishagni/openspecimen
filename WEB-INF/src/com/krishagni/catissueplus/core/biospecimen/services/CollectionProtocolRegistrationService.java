
package com.krishagni.catissueplus.core.biospecimen.services;

import java.io.File;
import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.events.BulkRegistrationsDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpEntityDeleteCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.CprSummary;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedRegistrationsList;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantRegistrationsList;
import com.krishagni.catissueplus.core.biospecimen.events.RegistrationQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSpecimensQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsListCriteria;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityResp;
import com.krishagni.catissueplus.core.common.events.BulkEntityDetail;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface CollectionProtocolRegistrationService {
	public ResponseEvent<CollectionProtocolRegistrationDetail> getRegistration(RequestEvent<RegistrationQueryCriteria> req);
	
	public ResponseEvent<CollectionProtocolRegistrationDetail> createRegistration(RequestEvent<CollectionProtocolRegistrationDetail> req);

	public ResponseEvent<CollectionProtocolRegistrationDetail> updateRegistration(RequestEvent<CollectionProtocolRegistrationDetail> req);

	ResponseEvent<List<CollectionProtocolRegistrationDetail>> bulkUpdateRegistrations(RequestEvent<BulkEntityDetail<CollectionProtocolRegistrationDetail>> req);

	public ResponseEvent<List<CollectionProtocolRegistrationDetail>> bulkRegistration(RequestEvent<BulkRegistrationsDetail> req);

	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<RegistrationQueryCriteria> req);
	
	public ResponseEvent<CollectionProtocolRegistrationDetail> deleteRegistration(RequestEvent<CpEntityDeleteCriteria> req);

	ResponseEvent<BulkDeleteEntityResp<CprSummary>> deleteRegistrations(RequestEvent<BulkDeleteEntityOp> crit);
	
	public ResponseEvent<List<VisitDetail>> getVisits(RequestEvent<VisitsListCriteria> req);

	public ResponseEvent<VisitDetail> getLatestVisit(RequestEvent<RegistrationQueryCriteria> req);
	
	public ResponseEvent<List<SpecimenDetail>> getSpecimens(RequestEvent<VisitSpecimensQueryCriteria> req);

	ResponseEvent<List<MatchedRegistrationsList>> getMatches(RequestEvent<List<CollectionProtocolRegistrationDetail>> req);

	ResponseEvent<List<CollectionProtocolRegistrationDetail>> getRegistrations(RequestEvent<List<Long>> req);

	public ResponseEvent<ParticipantRegistrationsList> createRegistrations(RequestEvent<ParticipantRegistrationsList> req);
	
	public ResponseEvent<ParticipantRegistrationsList> updateRegistrations(RequestEvent<ParticipantRegistrationsList> req);

	public ResponseEvent<CollectionProtocolRegistrationDetail> anonymize(RequestEvent<RegistrationQueryCriteria> req);
	
	public ResponseEvent<File> getConsentForm(RequestEvent<RegistrationQueryCriteria> req);
	
	public ResponseEvent<String> uploadConsentForm(RequestEvent<FileDetail> req);

	public ResponseEvent<Boolean> deleteConsentForm(RequestEvent<RegistrationQueryCriteria> req);

	public ResponseEvent<ConsentDetail> saveConsents(RequestEvent<ConsentDetail> req);

	public ResponseEvent<ConsentDetail> getConsents(RequestEvent<RegistrationQueryCriteria> req);
}

