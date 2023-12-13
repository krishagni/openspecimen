package com.krishagni.catissueplus.core.de.services;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.web.multipart.MultipartFile;

import com.krishagni.catissueplus.core.administrative.repository.FormListCriteria;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.AddRecordEntryOp;
import com.krishagni.catissueplus.core.de.events.EntityFormRecords;
import com.krishagni.catissueplus.core.de.events.FileDetail;
import com.krishagni.catissueplus.core.de.events.FormContextDetail;
import com.krishagni.catissueplus.core.de.events.FormContextRevisionDetail;
import com.krishagni.catissueplus.core.de.events.FormCtxtSummary;
import com.krishagni.catissueplus.core.de.events.FormDataDetail;
import com.krishagni.catissueplus.core.de.events.FormFieldSummary;
import com.krishagni.catissueplus.core.de.events.FormRecordCriteria;
import com.krishagni.catissueplus.core.de.events.FormRecordsList;
import com.krishagni.catissueplus.core.de.events.FormRevisionDetail;
import com.krishagni.catissueplus.core.de.events.FormSummary;
import com.krishagni.catissueplus.core.de.events.GetEntityFormRecordsOp;
import com.krishagni.catissueplus.core.de.events.GetFileDetailOp;
import com.krishagni.catissueplus.core.de.events.GetFormFieldPvsOp;
import com.krishagni.catissueplus.core.de.events.GetFormRecordsListOp;
import com.krishagni.catissueplus.core.de.events.ListEntityFormsOp;
import com.krishagni.catissueplus.core.de.events.ListFormFields;
import com.krishagni.catissueplus.core.de.events.MoveFormRecordsOp;
import com.krishagni.catissueplus.core.de.events.RemoveFormContextOp;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.PermissibleValue;
import edu.common.dynamicextensions.napi.FormData;
import krishagni.catissueplus.beans.FormContextBean;

public interface FormService {
	public ResponseEvent<List<FormSummary>> getForms(RequestEvent<FormListCriteria> req);
	
	public ResponseEvent<Long> getFormsCount(RequestEvent<FormListCriteria> req);
	
	public ResponseEvent<Container> getFormDefinition(RequestEvent<Long> req);

	ResponseEvent<Container> getFormDefinitionByName(RequestEvent<String> req);

	ResponseEvent<FormSummary> importForm(RequestEvent<String> req);

	public ResponseEvent<Long> saveForm(RequestEvent<Map<String, Object>> req);

	public ResponseEvent<Boolean> deleteForms(RequestEvent<BulkDeleteEntityOp> req);
	
	public ResponseEvent<List<FormFieldSummary>> getFormFields(RequestEvent<ListFormFields> req);
	
	public ResponseEvent<List<FormContextDetail>> getFormContexts(RequestEvent<Long> req);
	
	public ResponseEvent<List<FormContextDetail>> addFormContexts(RequestEvent<List<FormContextDetail>> req);
	
	public ResponseEvent<Boolean> removeFormContext(RequestEvent<RemoveFormContextOp> req);
	
	public ResponseEvent<List<FormCtxtSummary>> getEntityForms(RequestEvent<ListEntityFormsOp> req);
	
	public ResponseEvent<EntityFormRecords> getEntityFormRecords(RequestEvent<GetEntityFormRecordsOp> req);
	
	public ResponseEvent<FormDataDetail> getFormData(RequestEvent<FormRecordCriteria> req);

	public ResponseEvent<List<FormDataDetail>> getLatestRecords(RequestEvent<FormRecordCriteria> req);

	public ResponseEvent<FormDataDetail> saveFormData(RequestEvent<FormDataDetail> req);
	
	public ResponseEvent<List<FormData>> saveBulkFormData(RequestEvent<List<FormData>> req);

	public ResponseEvent<FileDetail> getFileDetail(RequestEvent<GetFileDetailOp> req);

	public ResponseEvent<FileDetail> uploadFile(RequestEvent<MultipartFile> req);

	ResponseEvent<FileDetail> uploadImage(RequestEvent<String> req);

	public ResponseEvent<Long> deleteRecord(RequestEvent<FormRecordCriteria> req);

	public ResponseEvent<Long> addRecordEntry(RequestEvent<AddRecordEntryOp> req);

	public ResponseEvent<List<FormRecordsList>> getFormRecords(RequestEvent<GetFormRecordsListOp> req);
	
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<Long> req);

	ResponseEvent<Integer> moveRecords(RequestEvent<MoveFormRecordsOp> req);

	//
	// Form revisions
	//
	ResponseEvent<List<FormRevisionDetail>> getFormRevisions(RequestEvent<Long> req);

	ResponseEvent<Container> getFormAtRevision(RequestEvent<Pair<Long, Long>> req);

	ResponseEvent<List<FormContextRevisionDetail>> getFormContextRevisions(RequestEvent<Long> req);

	/**
	 * Internal usage
	 */
	List<FormData> getSummaryRecords(Long formId, List<Long> recordIds);

	FormData getRecord(Container form, Long recordId);

	List<FormData> getRecords(Container form, List<Long> recordIds);
	
	ResponseEvent<List<PermissibleValue>> getPvs(RequestEvent<GetFormFieldPvsOp> req);

	void addFormContextProc(String entity, FormContextProcessor proc);

	Map<String, Object> getExtensionInfo(Long cpId, String entityType);

	Map<String, Object> getExtensionInfo(boolean cpBased, String entityType, Long entityId);

	List<FormSummary> getEntityForms(Long cpId, String[] entityTypes);

	void anonymizeRecord(Container form, Long recordId);

	void addAccessChecker(String entityType, FormAccessChecker checker);
}
