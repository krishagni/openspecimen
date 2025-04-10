package com.krishagni.catissueplus.core.de.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.administrative.repository.FormListCriteria;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.catissueplus.core.de.domain.Form;
import com.krishagni.catissueplus.core.de.events.FormContextDetail;
import com.krishagni.catissueplus.core.de.events.FormContextRevisionDetail;
import com.krishagni.catissueplus.core.de.events.FormCtxtSummary;
import com.krishagni.catissueplus.core.de.events.FormRecordSummary;
import com.krishagni.catissueplus.core.de.events.FormRevisionDetail;
import com.krishagni.catissueplus.core.de.events.FormSummary;
import com.krishagni.catissueplus.core.de.events.ObjectCpDetail;

import edu.common.dynamicextensions.domain.nui.Container;
import krishagni.catissueplus.beans.FormContextBean;
import krishagni.catissueplus.beans.FormRecordEntryBean;

public interface FormDao extends Dao<FormContextBean> {
	public Form getFormById(Long formId);

	public Form getFormByName(String name);

	public List<Form> getFormsByIds(Collection<Long> formIds);

	List<Form> getFormsByName(Collection<String> formNames);

	List<Form> getForms(FormListCriteria crit);

	Long getFormsCount(FormListCriteria crit);

	List<FormSummary> getEntityForms(FormListCriteria crit);

	Map<Long, Integer> getAssociationsCount(Collection<Long> formIds);
	
	public boolean isSystemForm(Long formId);

	public Date getUpdateTime(Long formId);

	public List<FormSummary> getQueryForms();
			
	public List<FormContextDetail> getFormContexts(Long formId);

	Map<Long, List<UserGroupSummary>> getNotifUsers(Collection<Long> contextIds);
	
	public List<FormCtxtSummary> getCprForms(Long cprId);

	public List<FormCtxtSummary> getParticipantForms(Long cprId);
		
	public List<FormCtxtSummary> getSpecimenForms(Long specimenId);
	
	public List<FormCtxtSummary> getSpecimenEventForms(Long specimenId);
	
	public List<FormCtxtSummary> getScgForms(Long scgId);
	
	public List<FormCtxtSummary> getFormContexts(Long cpId, String entityType);
	
	public List<FormRecordSummary> getFormRecords(Long formCtxtId, Long objectId);
	
	public FormSummary getFormByContext(Long formCtxtId);

	FormContextBean getFormContext(Long formCtxtId);

	public FormContextBean getFormContext(Long formId, Long cpId, String entity);

	FormContextBean getFormContext(Long formId, Long cpId, List<String> entities);

	public FormContextBean getFormContext(boolean cpBased, String entityType, Long entityId, Long formId);

	public FormContextBean getQueryFormContext(Long formId);

	public List<FormContextBean> getFormContexts(Collection<Long> cpIds, String entityType);

	public Pair<String, Long> getFormNameContext(Long cpId, String entityType, Long entityId);
	
	public void saveOrUpdateRecordEntry(FormRecordEntryBean recordEntry);

	FormRecordEntryBean getRecordEntry(Long recordId);
	
	public List<FormRecordEntryBean> getRecordEntries(Long formCtxtId, Long objectId);

	public Map<Long, Pair<Long, Long>> getLatestRecordIds(Long formId, String entityType, List<Long> objectIds);

	public FormRecordEntryBean getRecordEntry(Long formCtxtId, Long objectId, Long recordId);

	public FormRecordEntryBean getRecordEntry(Long formId, Long recordId);

	FormRecordEntryBean getRecordEntry(String formName, Long recordId);

	public Long getRecordsCount(Long formCtxtId, Long objectId);
	
	public ObjectCpDetail getObjectCpDetail(Map<String, Object> map);

	public Long getFormCtxtId(Long containerId, String entityType, Long cpId);
	
	public List<Long> getFormIds(Long cpId, String entityType);
	
	public List<Long> getFormIds(Long cpId, List<String> entityTypes);
	
	public List<FormContextBean> getFormContextsById(List<Long> formContextIds);

	FormContextBean getAbsFormContext(Long formId, Long cpId, String entity);

	public Map<Long, List<FormRecordSummary>> getFormRecords(Long objectId, String entityType, Long formId);
	
	public List<DependentEntityDetail> getDependentEntities(Long formId);
	
	public String getFormChangeLogDigest(String file);

	public Object[] getLatestFormChangeLog(String file);
	
	public void insertFormChangeLog(String file, String digest, Long formId);
	
	public void deleteFormContexts(Collection<Long> formIds);

	public void deleteRecords(Long formCtxtId, Collection<Long> recordIds);

	int deleteRecords(Long cpId, List<String> entityTypes, Long objectId);

	int undeleteRecords(Long cpId, List<String> entityTypes, Long objectId);

	int deleteFormContexts(Long cpId, List<String> entityTypes);

	// object id -> [record id]
	Map<Long, List<Long>> getRecordIds(Long formCtxtId, Collection<Long> objectIds);

	// form Id -> [record id]
	Map<String, List<Long>> getEntityFormRecordIds(Collection<String> entityTypes, Long objectId, Collection<String> formNames);

	//
	// used by form data exporter
	//
	List<Map<String, Object>> getRegistrationRecords(Long cpId, Collection<SiteCpPair> siteCps, Long formId, List<String> ppids, int startAt, int maxResults);

	List<Map<String, Object>> getParticipantRecords(Long cpId, Collection<SiteCpPair> siteCps, Long formId, List<String> ppids, int startAt, int maxResults);

	List<Map<String, Object>> getVisitRecords(Long cpId, Collection<SiteCpPair> siteCps, Long formId, List<String> visitNames, int startAt, int maxResults);

	List<Map<String, Object>> getSpecimenRecords(Long cpId, Collection<SiteCpPair> siteCps, Long formId, String entityType, List<String> spmnLabels, int startAt, int maxResults);

	int moveRegistrationRecords(Long cpId, Long srcFormCtxtId, Long tgtFormCtxtId);

	int moveVisitRecords(Long cpId, Long srcFormCtxtId, Long tgtFormCtxtId);

	int moveSpecimenRecords(Long cpId, Long srcFormCtxtId, Long tgtFormCtxtId);

	//
	// form revisions
	//
	List<FormRevisionDetail> getFormRevisions(Long formId);

	Container getFormAtRevision(Long formId, Long revId);

	List<FormContextRevisionDetail> getFormContextRevisions(Long formId);
}
