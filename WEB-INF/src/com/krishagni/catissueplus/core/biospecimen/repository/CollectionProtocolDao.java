
package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CpConsentTier;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface CollectionProtocolDao extends Dao<CollectionProtocol> {

	public List<CollectionProtocolSummary> getCollectionProtocols(CpListCriteria criteria);

	List<CollectionProtocol> getCollectionProtocolsList(CpListCriteria crit);

	public List<Long> getAllCpIds();

	public Long getCpCount(CpListCriteria criteria);

	public CollectionProtocol getCollectionProtocol(String title);

	List<CollectionProtocol> getCpsByTitle(Collection<String> titles);
	
	public CollectionProtocol getCpByShortTitle(String shortTitle);
	
	public List<CollectionProtocol> getCpsByShortTitle(Collection<String> shortTitles);
	
	public List<CollectionProtocol> getCpsByShortTitle(Collection<String> shortTitles, String siteName);
	
	public List<CollectionProtocol> getExpiringCps(Date fromDate, Date toDate);
	
	public CollectionProtocol getCpByCode(String code);

	public List<Long> getCpIdsBySiteIds(Collection<Long> instituteIds, Collection<Long> siteIds, Collection<String> siteNames);

	public Map<String, Object> getCpIds(String key, Object value);
	
	public Set<SiteCpPair> getSiteCps(Collection<Long> cpIds);

	public boolean isCpAffiliatedToUserInstitute(Long cpId, Long userId);

	public CollectionProtocolEvent getCpe(Long cpeId);
	
	public List<CollectionProtocolEvent> getCpes(Collection<Long> cpeIds);

	public CollectionProtocolEvent getCpeByEventLabel(Long cpId, String eventLabel);
	
	public CollectionProtocolEvent getCpeByEventLabel(String title, String label);
	
	public CollectionProtocolEvent getCpeByShortTitleAndEventLabel(String shortTitle, String label);

	public List<CollectionProtocolEvent> getCpesByShortTitleAndEventLabels(String shortTitle, Collection<String> labels);

	public CollectionProtocolEvent getCpeByCode(String shortTitle, String code);

	public void saveCpe(CollectionProtocolEvent cpe);
	
	public void saveCpe(CollectionProtocolEvent cpe, boolean flush);

	public int getAllVisitsCount(Long cpeId);

	public SpecimenRequirement getSpecimenRequirement(Long requirementId);
	
	public SpecimenRequirement getSrByCode(String code);
	
	public List<CpWorkflowConfig> getCpWorkflows(Collection<Long> cpIds);

	public void saveCpWorkflows(CpWorkflowConfig cfg);
	
	public CpWorkflowConfig getCpWorkflows(Long cpId);
	
	public CpConsentTier getConsentTier(Long consentId);
	
	public CpConsentTier getConsentTierByStatement(Long cpId, String statement);
	
	public int getConsentRespsCount(Long consentId);

	public boolean anyBarcodingEnabledCpExists();

	public List<String> getDependentContainers(Long cpId, Collection<Long> siteIds);

	Long getCustomFieldRecordId(Long cpId, Long formId, Long formCtxtId);

	Map<Long, Long> getCustomFieldRecordIds(Collection<Long> cpIds, Long formId, Long formCtxtId);

	int insertCustomFieldRecordId(Long cpId, Long formId, Long formCtxtId, Long recordId);
}
