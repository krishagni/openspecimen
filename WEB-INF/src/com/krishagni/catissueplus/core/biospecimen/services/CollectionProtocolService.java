
package com.krishagni.catissueplus.core.biospecimen.services;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.events.SiteSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.AliquotSpecimensRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.biospecimen.domain.DerivedSpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolPublishDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierOp;
import com.krishagni.catissueplus.core.biospecimen.events.CopyCpOpDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CopyCpeOpDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.CpReportSettingsDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpWorkflowCfgDetail;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.events.MergeCpDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ServiceDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ServiceRateDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ServiceReportCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenRequirementDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.CpListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.CpPublishEventListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.CprListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.ServiceListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.ServiceRateListCriteria;
import com.krishagni.catissueplus.core.common.Tuple;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityResp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.query.ListConfig;
import com.krishagni.catissueplus.core.query.ListDetail;

public interface CollectionProtocolService {

	public ResponseEvent<List<CollectionProtocolSummary>> getProtocols(RequestEvent<CpListCriteria> req);

	public ResponseEvent<Long> getProtocolsCount(RequestEvent<CpListCriteria> req);

	ResponseEvent<List<Map<String, Object>>> getCpsAndGroups(RequestEvent<String> req);

	public ResponseEvent<CollectionProtocolDetail> getCollectionProtocol(RequestEvent<CpQueryCriteria> req);

	public ResponseEvent<String> getCollectionProtocolDefinition(RequestEvent<CpQueryCriteria> req);

	ResponseEvent<List<SiteSummary>> getSites(RequestEvent<CpQueryCriteria> req);

	ResponseEvent<List<CollectionProtocolRegistrationDetail>> getRegisteredParticipants(RequestEvent<CprListCriteria> req);

	public ResponseEvent<Long> getRegisteredParticipantsCount(RequestEvent<CprListCriteria> req);

	public ResponseEvent<CollectionProtocolDetail> createCollectionProtocol(RequestEvent<CollectionProtocolDetail> req);

	public ResponseEvent<CollectionProtocolDetail> updateCollectionProtocol(RequestEvent<CollectionProtocolDetail> req);

	public ResponseEvent<CollectionProtocolDetail> copyCollectionProtocol(RequestEvent<CopyCpOpDetail> req);

	public ResponseEvent<MergeCpDetail> mergeCollectionProtocols(RequestEvent<MergeCpDetail> req);

	public ResponseEvent<CollectionProtocolDetail> updateConsentsWaived(RequestEvent<CollectionProtocolDetail> req);

	ResponseEvent<CollectionProtocolDetail> updateConsentsSource(RequestEvent<CollectionProtocolDetail> req);

	public ResponseEvent<CollectionProtocolDetail> importCollectionProtocol(RequestEvent<CollectionProtocolDetail> req);

	public ResponseEvent<List<DependentEntityDetail>> getCpDependentEntities(RequestEvent<Long> req);

	public ResponseEvent<BulkDeleteEntityResp<CollectionProtocolDetail>> deleteCollectionProtocols(RequestEvent<BulkDeleteEntityOp> crit);

	public ResponseEvent<File> getSopDocument(RequestEvent<Long> req);

	public ResponseEvent<String> uploadSopDocument(RequestEvent<FileDetail> req);

	public ResponseEvent<Boolean> isSpecimenBarcodingEnabled();

	//
	// Consent Tier APIs
	//
	public ResponseEvent<List<ConsentTierDetail>> getConsentTiers(RequestEvent<Long> req);

	public ResponseEvent<ConsentTierDetail> updateConsentTier(RequestEvent<ConsentTierOp> req);

	public ResponseEvent<List<DependentEntityDetail>> getConsentDependentEntities(RequestEvent<ConsentTierDetail> request);

	//
	// Events API
	//
	public ResponseEvent<List<CollectionProtocolEventDetail>> getProtocolEvents(RequestEvent<EntityQueryCriteria> req);

	public ResponseEvent<CollectionProtocolEventDetail> getProtocolEvent(RequestEvent<Long> req);

	public ResponseEvent<CollectionProtocolEventDetail> addEvent(RequestEvent<CollectionProtocolEventDetail> req);

	public ResponseEvent<CollectionProtocolEventDetail> updateEvent(RequestEvent<CollectionProtocolEventDetail> req);

	public ResponseEvent<CollectionProtocolEventDetail> copyEvent(RequestEvent<CopyCpeOpDetail> req);

	public ResponseEvent<CollectionProtocolEventDetail> deleteEvent(RequestEvent<Long> req);

	//
	// Specimen Requirement API
	//
	public ResponseEvent<List<SpecimenRequirementDetail>> getSpecimenRequirments(RequestEvent<Tuple> req);

	public ResponseEvent<SpecimenRequirementDetail> getSpecimenRequirement(RequestEvent<Long> req);

	public ResponseEvent<SpecimenRequirementDetail> addSpecimenRequirement(RequestEvent<SpecimenRequirementDetail> req);

	public ResponseEvent<List<SpecimenRequirementDetail>> createAliquots(RequestEvent<AliquotSpecimensRequirement> req);

	public ResponseEvent<SpecimenRequirementDetail> createDerived(RequestEvent<DerivedSpecimenRequirement> req);

	public ResponseEvent<SpecimenRequirementDetail> updateSpecimenRequirement(RequestEvent<SpecimenRequirementDetail> req);

	public ResponseEvent<SpecimenRequirementDetail> copySpecimenRequirement(RequestEvent<Long> req);

	public ResponseEvent<SpecimenRequirementDetail> deleteSpecimenRequirement(RequestEvent<Long> req);

	public ResponseEvent<Integer> getSrSpecimensCount(RequestEvent<Long> req);

	//
	// CP reports API
	//
	public ResponseEvent<CpReportSettingsDetail> getReportSettings(RequestEvent<CpQueryCriteria> req);

	public ResponseEvent<CpReportSettingsDetail> saveReportSettings(RequestEvent<CpReportSettingsDetail> req);

	public ResponseEvent<CpReportSettingsDetail> deleteReportSettings(RequestEvent<CpQueryCriteria> req);

	public ResponseEvent<Boolean> generateReport(RequestEvent<CpQueryCriteria> req);

	public ResponseEvent<File> getReportFile(Long cpId, String fileId);

	//
	// Workflow API
	//
	public ResponseEvent<CpWorkflowCfgDetail> getWorkflows(RequestEvent<Long> req);

	public ResponseEvent<CpWorkflowCfgDetail> saveWorkflows(RequestEvent<CpWorkflowCfgDetail> req);

	public CpWorkflowConfig saveWorkflows(CollectionProtocol cp, CpWorkflowCfgDetail input);

	//
	// Publish APIs
	//
	public ResponseEvent<List<CollectionProtocolPublishDetail>> getPublishEvents(RequestEvent<CpPublishEventListCriteria> req);

	public ResponseEvent<CollectionProtocolPublishDetail> publish(RequestEvent<CollectionProtocolPublishDetail> req);

	public ResponseEvent<String> getPublishedVersion(RequestEvent<EntityQueryCriteria> req);

	//
	// CP services
	//
	ResponseEvent<List<ServiceDetail>> getServices(RequestEvent<ServiceListCriteria> req);

	ResponseEvent<ServiceDetail> createService(RequestEvent<ServiceDetail> req);

	ResponseEvent<ServiceDetail> updateService(RequestEvent<ServiceDetail> req);

	ResponseEvent<ServiceDetail> deleteService(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<List<ServiceRateDetail>> getServiceRates(RequestEvent<ServiceRateListCriteria> req);

	ResponseEvent<List<ServiceRateDetail>> addServiceRates(RequestEvent<List<ServiceRateDetail>> req);

	ResponseEvent<ServiceRateDetail> updateServiceRate(RequestEvent<ServiceRateDetail> req);

	ResponseEvent<ServiceRateDetail> deleteServiceRate(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<FileDetail> generateServiceReport(RequestEvent<ServiceReportCriteria> req);

	//
	// For UI work
	//

	// Input is site names, search title and max results count
	public ResponseEvent<List<CollectionProtocolSummary>> getRegisterEnabledCps(
			List<String> siteNames, String searchTitle, int maxResults);

	//
	// Input is following:
	// {
	//   "cpId": ...
	//   "listName": ...
	// }
	//
	//
	public ResponseEvent<ListConfig> getCpListCfg(RequestEvent<Map<String, Object>> req);

	//
	// CP based lists. Input is following
	//	{
	//		"cpId": ...,
	//		"listName": ...,
	//		"startAt": ...,
	//		"maxResults": ...,
	//		"includeCount": ...,
	//		"filters": [
	//			{
	//				"expr": ...
	//				"values": ...
	//			}
	//		]
	//	}
	//
	public ResponseEvent<ListDetail> getList(RequestEvent<Map<String, Object>> req);

	public ResponseEvent<Integer> getListSize(RequestEvent<Map<String, Object>> req);

	public ResponseEvent<Collection<Object>> getListExprValues(RequestEvent<Map<String, Object>> req);

	//
	// Labelling
	//
	boolean toggleStarredCp(Long cpId, boolean starred);

	interface DataSource {
		public Object getMetric(CollectionProtocol cp, Map<String, Object> input);

		public File getDataFile(CollectionProtocol cp, Map<String, Object> input);
	}
}
