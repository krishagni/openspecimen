package com.krishagni.catissueplus.core.de.services.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.web.multipart.MultipartFile;

import com.krishagni.catissueplus.core.administrative.domain.FormDataDeleteEvent;
import com.krishagni.catissueplus.core.administrative.domain.FormDataSavedEvent;
import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.repository.FormListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpGroupErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.impl.SystemFormUpdatePreventer;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.domain.PdeAuditLog;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;
import com.krishagni.catissueplus.core.common.events.Operation;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.Resource;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.domain.Form;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.catissueplus.core.de.events.AddRecordEntryOp;
import com.krishagni.catissueplus.core.de.events.EntityFormRecords;
import com.krishagni.catissueplus.core.de.events.FileDetail;
import com.krishagni.catissueplus.core.de.events.FormContextDetail;
import com.krishagni.catissueplus.core.de.events.FormContextRevisionDetail;
import com.krishagni.catissueplus.core.de.events.FormCtxtSummary;
import com.krishagni.catissueplus.core.de.events.FormDataDetail;
import com.krishagni.catissueplus.core.de.events.FormFieldSummary;
import com.krishagni.catissueplus.core.de.events.FormRecordCriteria;
import com.krishagni.catissueplus.core.de.events.FormRecordSummary;
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
import com.krishagni.catissueplus.core.de.events.ObjectCpDetail;
import com.krishagni.catissueplus.core.de.events.RemoveFormContextOp;
import com.krishagni.catissueplus.core.de.repository.FormDao;
import com.krishagni.catissueplus.core.de.services.FormContextProcessor;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.DataType;
import edu.common.dynamicextensions.domain.nui.FileUploadControl;
import edu.common.dynamicextensions.domain.nui.Label;
import edu.common.dynamicextensions.domain.nui.LookupControl;
import edu.common.dynamicextensions.domain.nui.PageBreak;
import edu.common.dynamicextensions.domain.nui.PermissibleValue;
import edu.common.dynamicextensions.domain.nui.SelectControl;
import edu.common.dynamicextensions.domain.nui.SubFormControl;
import edu.common.dynamicextensions.domain.nui.UserContext;
import edu.common.dynamicextensions.napi.ControlValue;
import edu.common.dynamicextensions.napi.FileControlValue;
import edu.common.dynamicextensions.napi.FormData;
import edu.common.dynamicextensions.napi.FormDataManager;
import edu.common.dynamicextensions.napi.FormEventsNotifier;
import edu.common.dynamicextensions.nutility.ContainerParser;
import edu.common.dynamicextensions.nutility.ContainerPropsParser;
import edu.common.dynamicextensions.nutility.FileUploadMgr;
import krishagni.catissueplus.beans.FormContextBean;
import krishagni.catissueplus.beans.FormRecordEntryBean;
import krishagni.catissueplus.beans.FormRecordEntryBean.Status;

public class FormServiceImpl implements FormService, InitializingBean {
	private static Log logger = LogFactory.getLog(FormServiceImpl.class);

	private static final String CP_FORM = "CollectionProtocol";

	private static final String PARTICIPANT_FORM = "Participant";

	private static final String COMMON_PARTICIPANT = "CommonParticipant";
	
	private static final String SCG_FORM = "SpecimenCollectionGroup";
	
	private static final String SPECIMEN_FORM = "Specimen";
	
	private static final String SPECIMEN_EVENT_FORM = "SpecimenEvent";
	
	private static Set<String> staticExtendedForms = new HashSet<>();
	
	private static Map<String, String> customFieldEntities = new HashMap<>();

	private Map<String, Function<Long, Boolean>> entityAccessCheckers = new HashMap<>();

	private static Map<String, List<String>> editableEvents;

	static {
		staticExtendedForms.add(PARTICIPANT_FORM);
		staticExtendedForms.add(SCG_FORM);
		staticExtendedForms.add(SPECIMEN_FORM);

		customFieldEntities.put(CP_FORM, CollectionProtocol.EXTN);
		customFieldEntities.put(PARTICIPANT_FORM, Participant.EXTN);
		customFieldEntities.put(SCG_FORM, Visit.EXTN);
		customFieldEntities.put(SPECIMEN_FORM, Specimen.EXTN);

		editableEvents = new HashMap<String, List<String>>() {
			{
				put("SpecimenDisposalEvent", Arrays.asList("reason", "user", "time", "comments"));
				put("SpecimenTransferEvent", Arrays.asList("user", "time", "comments"));

			}
		};
	}

	private FormDao formDao;

	private FormDataManager formDataMgr;

	private DaoFactory daoFactory;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;

	private ExportService exportSvc;
	
	private Map<String, List<FormContextProcessor>> ctxtProcs = new HashMap<>();

	public void setFormDao(FormDao formDao) {
		this.formDao = formDao;
	}

	public void setFormDataMgr(FormDataManager formDataMgr) {
		this.formDataMgr = formDataMgr;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	public void setCtxtProcs(Map<String, List<FormContextProcessor>> ctxtProcs) {
		this.ctxtProcs = ctxtProcs;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		FormEventsNotifier.getInstance().addListener(new SystemFormUpdatePreventer(formDao));
		exportSvc.registerObjectsGenerator("extensions", this::getFormRecordsGenerator);
		exportSvc.registerObjectsGenerator("userExtensions", this::getFormRecordsGenerator);

		entityAccessCheckers.put(
			"User",
			(entityId) -> {
				if (AuthUtil.getCurrentUser().isAdmin()) {
					return true;
				}

				Institute institute = AuthUtil.getCurrentUserInstitute();
				return entityId != -1L &&
					institute.getId().equals(entityId) &&
					AuthUtil.getCurrentUser().isInstituteAdmin();
			}
		);
	}

	@Override
    @PlusTransactional
	public ResponseEvent<List<FormSummary>> getForms(RequestEvent<FormListCriteria> req) {
		FormListCriteria crit = req.getPayload();
		if (crit.entityTypes() != null && crit.entityTypes().size() == 1 && crit.entityTypes().contains("Query")) {
			return ResponseEvent.response(formDao.getQueryForms());
		} else {
			crit = addFormsListCriteria(crit);
			if (crit == null) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<Form> forms = formDao.getForms(crit);
			List<FormSummary> result = forms.stream().map(FormSummary::from).collect(Collectors.toList());
			if (!result.isEmpty() && crit.includeStat()) {
				Map<Long, FormSummary> formsMap = result.stream().collect(Collectors.toMap(FormSummary::getFormId, f -> f));
				Map<Long, Integer> associationCounts = formDao.getAssociationsCount(formsMap.keySet());
				for (FormSummary form : result) {
					form.setAssociations(associationCounts.getOrDefault(form.getFormId(), 0));
				}
			}

			return ResponseEvent.response(result);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getFormsCount(RequestEvent<FormListCriteria> req) {
		FormListCriteria crit = addFormsListCriteria(req.getPayload());
		if (crit == null) {
			return ResponseEvent.response(0L);
		}

		return ResponseEvent.response(formDao.getFormsCount(crit));
	}

    @Override
    @PlusTransactional
	public ResponseEvent<Container> getFormDefinition(RequestEvent<Long> req) {
		Container form = getContainer(req.getPayload(), null);
		return ResponseEvent.response(form);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<FormSummary> importForm(RequestEvent<String> req) {
		try {
			AccessCtrlMgr.getInstance().ensureFormUpdateRights();

			//
			// The input directory layout
			//   form-dir (created from zip)
			//       |____ form1.xml
			//       |____ pvs
			//
			File inputDir = new File(req.getPayload());
			String formXml = Arrays.stream(inputDir.list((dir, name) -> name.endsWith(".xml")))
				.sorted(Comparator.naturalOrder())
				.findFirst().orElse(null);
			if (StringUtils.isBlank(formXml)) {
				return ResponseEvent.response(null);
			}

			String formXmlPath = new File(inputDir, formXml).getAbsolutePath();
			String pvsDirPath = new File(inputDir, "pvs").getAbsolutePath();

			Container parsedForm = new ContainerParser(formXmlPath, pvsDirPath).parse();
			Long formId = Container.createContainer(getUserContext(false), parsedForm, true);

			FormSummary result = new FormSummary();
			result.setFormId(formId);
			result.setName(parsedForm.getName());
			result.setCaption(parsedForm.getCaption());
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> saveForm(RequestEvent<Map<String, Object>> req) {
		AccessCtrlMgr.getInstance().ensureFormUpdateRights();
		Container input = new ContainerPropsParser(req.getPayload()).parse();
		return ResponseEvent.response(Container.createContainer(getUserContext(false), input, true));
	}
    
	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> deleteForms(RequestEvent<BulkDeleteEntityOp> req) {
		try {
			AccessCtrlMgr.getInstance().ensureFormUpdateRights();

			Set<Long> formIds = req.getPayload().getIds();
			List<Form> forms = formDao.getFormsByIds(formIds);
			if (formIds.size() != forms.size()) {
				forms.forEach(form -> formIds.remove(form.getId()));
				throw OpenSpecimenException.userError(FormErrorCode.NOT_FOUND, formIds, formIds.size());
			}

			formIds.forEach(formId -> Container.softDeleteContainer(getUserContext(false), formId));
			formDao.deleteFormContexts(formIds);

			return ResponseEvent.response(true);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
    
    @Override
    @PlusTransactional
	public ResponseEvent<List<FormFieldSummary>> getFormFields(RequestEvent<ListFormFields> req) {
    	ListFormFields op = req.getPayload();
		Container form = getContainer(op.getFormId(), null);
		
		List<FormFieldSummary> fields = getFormFields(form);
		if (!op.isExtendedFields()) {
			return ResponseEvent.response(fields);
		}

		Long cpId = op.getCpId();
		Long groupId = op.getCpGroupId();
		if (groupId == null || groupId <= 0) {
			fields.addAll(getCpFields(cpId, form));
		} else {
			fields.addAll(getCpGroupFields(groupId, form));
		}

		return ResponseEvent.response(fields);
	}
    
	@Override
	@PlusTransactional
	public ResponseEvent<List<FormContextDetail>> getFormContexts(RequestEvent<Long> req) {
		return ResponseEvent.response(formDao.getFormContexts(req.getPayload()));
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<FormContextDetail>> addFormContexts(RequestEvent<List<FormContextDetail>> req) { // TODO: check form is deleted
		try {
			List<FormContextDetail> formCtxts = req.getPayload();
			for (FormContextDetail formCtxtDetail : req.getPayload()) {
				Long cpId = formCtxtDetail.getCollectionProtocol().getId();
				String entity = formCtxtDetail.getLevel();
				Long entityId = formCtxtDetail.getEntityId();

				if (entityId != null) {
					Function<Long, Boolean> accessChecker = entityAccessCheckers.get(entity);
					if (accessChecker != null && !accessChecker.apply(entityId)) {
						throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
					}
				} else if (cpId == -1L && !AuthUtil.isAdmin()) {
					throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
				} else if (cpId != -1L) {
					AccessCtrlMgr.getInstance().ensureUpdateCpRights(cpId);
				}

				addFormContext(formCtxtDetail);
			}
			
			return ResponseEvent.response(formCtxts);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<FormCtxtSummary>> getEntityForms(RequestEvent<ListEntityFormsOp> req) {
		try {
			ListEntityFormsOp opDetail = req.getPayload();
			
			List<FormCtxtSummary> forms = null;
			
			Long entityId = opDetail.getEntityId();
			switch (opDetail.getEntityType()) {
			    case COLLECTION_PROTOCOL_REGISTRATION:
			    	AccessCtrlMgr.getInstance().ensureReadCprRights(entityId);
					forms = formDao.getParticipantForms(entityId);
					forms.addAll(formDao.getCprForms(entityId));
			    	break;
			    	
			    case SPECIMEN:
			    	AccessCtrlMgr.getInstance().ensureReadSpecimenRights(entityId);
			    	forms = formDao.getSpecimenForms(opDetail.getEntityId());
			    	break;
			    	
			    case SPECIMEN_COLLECTION_GROUP:
			    	AccessCtrlMgr.getInstance().ensureReadVisitRights(entityId);
			    	forms = formDao.getScgForms(opDetail.getEntityId());
			    	break;

			    case SPECIMEN_EVENT :
			    	AccessCtrlMgr.getInstance().ensureReadSpecimenRights(entityId);
			    	forms = formDao.getSpecimenEventForms(opDetail.getEntityId());
			    	break;
			}
			
			return ResponseEvent.response(forms);			
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<EntityFormRecords> getEntityFormRecords(RequestEvent<GetEntityFormRecordsOp> req) {
		GetEntityFormRecordsOp opDetail = req.getPayload();
		
		FormSummary form = formDao.getFormByContext(opDetail.getFormCtxtId());
		List<FormRecordSummary> formRecs = formDao.getFormRecords(opDetail.getFormCtxtId(), opDetail.getEntityId());
		
		EntityFormRecords result = new EntityFormRecords();
		result.setFormId(form.getFormId());
		result.setFormCaption(form.getCaption());
		result.setFormCtxtId(opDetail.getFormCtxtId());
		result.setRecords(formRecs);
		return ResponseEvent.response(result);
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<FormDataDetail> getFormData(RequestEvent<FormRecordCriteria> req) {
		try {
			FormRecordCriteria crit = req.getPayload();
			Container form = getContainer(crit.getFormId(), null);
			FormData record = getRecord(form, crit.getRecordId());
			return ResponseEvent.response(FormDataDetail.ok(crit.getFormId(), crit.getRecordId(), record));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<FormDataDetail>> getLatestRecords(RequestEvent<FormRecordCriteria> req) {
		try {
			FormRecordCriteria crit = req.getPayload();
			Container form = getContainer(crit.getFormId(), null);
			Map<Long, Pair<Long, Long>> recordIds = formDao.getLatestRecordIds(
				crit.getFormId(), crit.getEntityType(), crit.getObjectIds());

			List<FormDataDetail> records = new ArrayList<>();
			recordIds.forEach((objectId, recordId) -> {
				FormData record = getRecord(form, objectId, recordId.first(), crit.getEntityType(), recordId.second());
				records.add(FormDataDetail.ok(form.getId(), record.getRecordId(), record));
			});

			return ResponseEvent.response(records);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<FormDataDetail> saveFormData(RequestEvent<FormDataDetail> req) {
		FormDataDetail detail = req.getPayload();
		
		try {
			FormData formData = saveOrUpdateFormData(detail.getRecordId(), detail.getFormData(), detail.isPartial());
			return ResponseEvent.response(FormDataDetail.ok(formData.getContainer().getId(), formData.getRecordId(), formData));
		} catch(IllegalArgumentException ex) {
			return ResponseEvent.userError(FormErrorCode.INVALID_DATA, ex.getMessage());
		} catch (DataAccessException dae) {
			return ResponseEvent.userError(CommonErrorCode.SQL_EXCEPTION, dae.getMessage());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<FormData>> saveBulkFormData(RequestEvent<List<FormData>> req) {
		try{
			List<FormData> formDataList = req.getPayload();
			List<FormData> savedFormDataList = new ArrayList<>();
			for (FormData formData : formDataList) {
				FormData savedFormData = saveOrUpdateFormData(formData.getRecordId(), formData, true);
				savedFormDataList.add(savedFormData);
			}
			
			return ResponseEvent.response(savedFormDataList);
		} catch(IllegalArgumentException ex) {
			return ResponseEvent.userError(FormErrorCode.INVALID_DATA, ex.getMessage());
		} catch (DataAccessException dae) {
			return ResponseEvent.userError(CommonErrorCode.SQL_EXCEPTION, dae.getMessage());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}	
	}

	@Override
	@PlusTransactional
	public ResponseEvent<FileDetail> getFileDetail(RequestEvent<GetFileDetailOp> req) {
		GetFileDetailOp op = req.getPayload();

		FileControlValue fcv = null;
		if (op.getRecordId() != null) {
			fcv = formDataMgr.getFileControlValue(op.getFormId(), op.getRecordId(), op.getCtrlName());
		} else if (StringUtils.isNotBlank(op.getFileId())) {
			fcv = formDataMgr.getFileControlValue(op.getFormId(), op.getCtrlName(), op.getFileId());
		}

		if (fcv == null) {
			return ResponseEvent.userError(FormErrorCode.FILE_NOT_FOUND);
		}
		
		return ResponseEvent.response(FileDetail.from(fcv));
	}
	
	@Override
	public ResponseEvent<FileDetail> uploadFile(RequestEvent<MultipartFile> req) {
		MultipartFile input = req.getPayload();
		
		FileDetail fileDetail = new FileDetail();
		fileDetail.setFilename(input.getOriginalFilename());
		fileDetail.setSize(input.getSize());
		fileDetail.setContentType(input.getContentType());
		
		try {
			InputStream in = input.getInputStream();
			String fileId = FileUploadMgr.getInstance().saveFile(in);
			fileDetail.setFileId(fileId);
			return ResponseEvent.response(fileDetail);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}		
	}

	@Override
	public ResponseEvent<FileDetail> uploadImage(RequestEvent<String> req) {
		String dataUrl = req.getPayload();
		if (StringUtils.isBlank(dataUrl)) {
			return ResponseEvent.response(null);
		}

		String[] parts = dataUrl.split(",", 2);
		if (parts.length == 1) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Invalid image data URL");
		}

		String header = parts[0];
		String base64Img = parts[1];

		String[] headerParts = header.split("[:/;]");
		if (headerParts.length < 4) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Invalid image data URL");
		}

		String type = headerParts[2];
		byte[] imgBinary = Base64.getDecoder().decode(base64Img);
		ByteArrayInputStream bin = new ByteArrayInputStream(imgBinary);

		try {
			String fileId = FileUploadMgr.getInstance().saveFile(bin, type);

			FileDetail result = new FileDetail();
			result.setFileId(fileId);
			result.setContentType("image/" + type);
			result.setSize(imgBinary.length);
			return ResponseEvent.response(result);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Long> deleteRecord(RequestEvent<FormRecordCriteria> req) {
		try {
			FormRecordCriteria crit = req.getPayload();
			FormRecordEntryBean recEntry = formDao.getRecordEntry(crit.getFormId(), crit.getRecordId());
			if (recEntry == null) {
				return ResponseEvent.userError(FormErrorCode.REC_NOT_FOUND);
			}

			if (recEntry.getFormCtxt().isSysForm()) {
				return ResponseEvent.userError(FormErrorCode.SYS_REC_DEL_NOT_ALLOWED);
			}
			
			String entityType = recEntry.getEntityType();
			Long objectId = recEntry.getObjectId();
			Object object = null;
			if (entityType.equals("Participant")) {
				CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(objectId);
				if (cpr == null) {
					throw OpenSpecimenException.userError(CprErrorCode.M_NOT_FOUND, objectId, 1);
				}

				AccessCtrlMgr.getInstance().ensureUpdateCprRights(cpr);
				object = cpr;
			} else if (entityType.equals("CommonParticipant")) {
				Participant participant = daoFactory.getParticipantDao().getById(objectId);
				if (participant == null) {
					throw OpenSpecimenException.userError(ParticipantErrorCode.NOT_FOUND, objectId);
				}

				AccessCtrlMgr.getInstance().ensureUpdateParticipantRights(participant);
				object = participant;
			} else if (entityType.equals("SpecimenCollectionGroup")) {
				Visit visit = daoFactory.getVisitsDao().getById(objectId);
				if (visit == null) {
					throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND, objectId);
				}

				AccessCtrlMgr.getInstance().ensureCreateOrUpdateVisitRights(visit, false);
				object = visit;
			} else if (entityType.equals("Specimen") || entityType.equals("SpecimenEvent")) {
				Specimen specimen = ensureSpecimenUpdateRights(objectId, false);
				object = specimen;
			}
			
			recEntry.delete();
			formDao.saveOrUpdateRecordEntry(recEntry);
			if (object != null) {
				EventPublisher.getInstance().publish(new FormDataDeleteEvent(entityType, object, recEntry));
			}

			return  ResponseEvent.response(crit.getRecordId());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> removeFormContext(RequestEvent<RemoveFormContextOp> req) {
		try {
			AccessCtrlMgr.getInstance().ensureFormUpdateRights();

			RemoveFormContextOp opDetail = req.getPayload();
			Long cpId = opDetail.getCpId();
			Long entityId = opDetail.getEntityId();
			FormContextBean formCtx = formDao.getFormContext(
				entityId == null,
				opDetail.getEntityType(),
				entityId == null ? opDetail.getCpId() : entityId,
				opDetail.getFormId());

			if (formCtx == null) {
				return ResponseEvent.userError(FormErrorCode.NO_ASSOCIATION, cpId, opDetail.getFormId()	);
			}
			
			if (formCtx.isSysForm()) {
				return ResponseEvent.userError(FormErrorCode.SYS_FORM_DEL_NOT_ALLOWED);
			}

			if (entityId != null) {
				Function<Long, Boolean> accessChecker = entityAccessCheckers.get(opDetail.getEntityType());
				if (accessChecker != null && !accessChecker.apply(entityId)) {
					return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
				}
			} else if (cpId == -1L && !AuthUtil.isAdmin()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			} else if (cpId != -1L) {
				AccessCtrlMgr.getInstance().ensureUpdateCpRights(cpId);
			}

			notifyContextRemoved(formCtx);
			switch (opDetail.getRemoveType()) {
				case SOFT_REMOVE:
					formCtx.setDeletedOn(Calendar.getInstance().getTime());
					break;
					
				case HARD_REMOVE:
					formDao.delete(formCtx);
					break;
			}
			
			return ResponseEvent.response(true);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
		
	@Override
	@PlusTransactional
	public ResponseEvent<Long> addRecordEntry(RequestEvent<AddRecordEntryOp> req) {
		AddRecordEntryOp opDetail = req.getPayload();
		String entityType = (String) opDetail.getRecIntegrationInfo().get("entityType");

		ObjectCpDetail objCp = formDao.getObjectCpDetail(opDetail.getRecIntegrationInfo());
		Long formCtxtId = formDao.getFormCtxtId(opDetail.getContainerId(), entityType, objCp.getCpId());

		FormRecordEntryBean recordEntry = new FormRecordEntryBean();
		recordEntry.setFormCtxtId(formCtxtId);
		recordEntry.setObjectId(objCp.getObjectId());
		recordEntry.setRecordId(opDetail.getRecordId());
		recordEntry.setUpdatedBy(AuthUtil.getCurrentUser().getId());
		recordEntry.setUpdatedTime(Calendar.getInstance().getTime());
		recordEntry.setActivityStatus(Status.ACTIVE);

		formDao.saveOrUpdateRecordEntry(recordEntry);		
		return ResponseEvent.response(recordEntry.getIdentifier());
	}
		
	@Override
	@PlusTransactional
	public ResponseEvent<List<FormRecordsList>> getFormRecords(RequestEvent<GetFormRecordsListOp> req) {
		try {
			GetFormRecordsListOp input = req.getPayload();
			String entityType = input.getEntityType();
			Long objectId = input.getObjectId();

			if (entityType.equals("Participant")) {
				AccessCtrlMgr.getInstance().ensureReadCprRights(objectId);
			} else if (entityType.equals("SpecimenCollectionGroup")) {
				AccessCtrlMgr.getInstance().ensureReadVisitRights(objectId);
			} else if (entityType.equals("Specimen") || entityType.equals("SpecimenEvent")) {
				AccessCtrlMgr.getInstance().ensureReadSpecimenRights(objectId);
			}

			List<FormRecordsList> result = getFormRecords(entityType, input.getFormId(), objectId);
			if (entityType.equals("Participant")) {
				objectId = daoFactory.getCprDao().getById(objectId).getParticipant().getId();
				result.addAll(0, getFormRecords("CommonParticipant", input.getFormId(), objectId));
			}

			return ResponseEvent.response(result);			
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<Long> req) {
		try {
			return ResponseEvent.response(formDao.getDependentEntities(req.getPayload()));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> moveRecords(RequestEvent<MoveFormRecordsOp> req) {
		try {
			if (!AuthUtil.isAdmin()) {
				return ResponseEvent.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
			}

			MoveFormRecordsOp op = req.getPayload();
			if (StringUtils.isBlank(op.getEntity())) {
				return ResponseEvent.userError(FormErrorCode.ENTITY_TYPE_REQUIRED);
			} else if (isNotCpEntity(op.getEntity())) {
				return ResponseEvent.userError(FormErrorCode.OP_NOT_ALLOWED);
			}

			Form form = formDao.getFormByName(op.getFormName());
			if (form == null) {
				return ResponseEvent.userError(FormErrorCode.NOT_FOUND, op.getFormName(), 1);
			}

			List<Long> srcCpIds = getCpIds(op.getSourceCp(), op.getSourceCpGroup());
			List<Long> tgtCpIds = getCpIds(op.getTargetCp(), op.getTargetCpGroup());
			if (srcCpIds.size() > 1 && tgtCpIds.size() > 1) {
				return ResponseEvent.userError(FormErrorCode.OP_NOT_ALLOWED);
			}

			int result = 0;
			for (Long srcCpId : srcCpIds) {
				for (Long tgtCpId : tgtCpIds) {
					result += moveRecords(form.getId(), op.getEntity(), srcCpId, tgtCpId);
				}
			}

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<FormRevisionDetail>> getFormRevisions(RequestEvent<Long> req) {
		return ResponseEvent.response(formDao.getFormRevisions(req.getPayload()));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Container> getFormAtRevision(RequestEvent<Pair<Long, Long>> req) {
		Pair<Long, Long> formRevIds = req.getPayload();
		return ResponseEvent.response(formDao.getFormAtRevision(formRevIds.first(), formRevIds.second()));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<FormContextRevisionDetail>> getFormContextRevisions(RequestEvent<Long> req) {
		return ResponseEvent.response(formDao.getFormContextRevisions(req.getPayload()));
	}

	//
	// Internal APIs
	//
	@Override
	public List<FormData> getSummaryRecords(Long formId, List<Long> recordIds) {
		return formDataMgr.getSummaryData(formId, recordIds);
	}

	@Override
	public FormData getRecord(Container form, Long recordId) {
		return getRecord(form, null, null, null, recordId);
	}

	@Override
	public List<FormData> getRecords(Container form, List<Long> recordIds) {
		return formDataMgr.getFormData(form, recordIds);
	}

	@Override
	public ResponseEvent<List<PermissibleValue>> getPvs(RequestEvent<GetFormFieldPvsOp> req) {
		try {
			GetFormFieldPvsOp input = req.getPayload();
			Container form = getContainer(input.getFormId(), input.getFormName());

			String controlName = input.getControlName();
			Control control;
			if (input.isUseUdn()) {
				control = form.getControlByUdn(controlName, "\\.");
			} else {
				control = form.getControl(controlName, "\\.");
			}

			if (!(control instanceof SelectControl)) {
				return ResponseEvent.userError(FormErrorCode.NOT_SELECT_CONTROL, controlName);
			}

			String searchStr = input.getSearchString();
			int maxResults = input.getMaxResults() <= 0 ? 100 : input.getMaxResults();

			SelectControl selectControl = (SelectControl) control;
			List<PermissibleValue> pvs = selectControl.getPvDataSource().getPermissibleValues(searchStr, maxResults);
			return ResponseEvent.response(pvs);
		} catch (IllegalArgumentException iae) {
			return ResponseEvent.error(OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, iae.getMessage()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public void addFormContextProc(String entity, FormContextProcessor proc) {
		List<FormContextProcessor> procs = ctxtProcs.computeIfAbsent(entity, k -> new ArrayList<>());
		if (procs.stream().noneMatch(existing -> existing == proc)) {
			procs.add(proc);
		}
	}

	@Override
	@PlusTransactional
	public Map<String, Object> getExtensionInfo(Long cpId, String entityType) {
		return getExtensionInfo(true, entityType, cpId);
	}

	@Override
	@PlusTransactional
	public Map<String, Object> getExtensionInfo(boolean cpBased, String entityType, Long entityId) {
		return DeObject.getFormInfo(cpBased, entityType, entityId);
	}

	@Override
	@PlusTransactional
	public List<FormSummary> getEntityForms(Long cpId, String[] entityTypes) {
		FormListCriteria crit = new FormListCriteria()
			.cpIds(Arrays.asList(-1L, cpId))
			.entityTypes(Arrays.asList(entityTypes));
		return formDao.getEntityForms(crit);
	}

	//
	// anonymize. Used by internal code
	//
	@Override
	@PlusTransactional
	public void anonymizeRecord(Container form, Long recordId) {
		FormRecordEntryBean recEntry = formDao.getRecordEntry(form.getId(), recordId);
		if (recEntry == null) {
			throw OpenSpecimenException.userError(FormErrorCode.REC_NOT_FOUND);
		}

		formDataMgr.anonymize(
			new UserContext() {
				@Override
				public Long getUserId() {
					return AuthUtil.getCurrentUser().getId();
				}

				@Override
				public String getUserName() {
					return AuthUtil.getCurrentUser().getLoginName();
				}

				@Override
				public String getIpAddress() {
					return AuthUtil.getRemoteAddr();
				}
			},
			form,
			recordId
		);

		recEntry.setUpdatedBy(AuthUtil.getCurrentUser().getId());
		recEntry.setUpdatedTime(Calendar.getInstance().getTime());
		formDao.saveOrUpdateRecordEntry(recEntry);
	}

	private FormListCriteria addFormsListCriteria(FormListCriteria crit) {
		User currUser = AuthUtil.getCurrentUser();
		if (!currUser.isAdmin() && !currUser.getManageForms()) {
			return null;
		} else if (!currUser.isAdmin() && currUser.getManageForms()) {
			crit.userId(currUser.getId());
			crit.siteCps(AccessCtrlMgr.getInstance().getReadableSiteCps());
		}
		
		return crit;
	}

	private List<FormFieldSummary> getCpFields(Long cpId, Container form) {
		List<FormFieldSummary> fields = new ArrayList<>();
		if (cpId == null || cpId < 0) {
			cpId = -1L;
		}

		String formName = form.getName();
		String entityType = customFieldEntities.get(formName);
		if (StringUtils.isNotBlank(entityType)) {
			Map<String, Object> extnInfo = getExtensionInfo(cpId, entityType);
			if (extnInfo == null && cpId != -1L) {
				extnInfo = getExtensionInfo(-1L, entityType);
			}

			if (extnInfo != null) {
				Long extnFormId = (Long)extnInfo.get("formId");
				fields.add(getExtensionField("customFields", "Custom Fields", Collections.singletonList(extnFormId)));
			}
		}

		if (!staticExtendedForms.contains(formName)) {
			return fields;
		}

		List<Long> extendedFormIds = formDao.getFormIds(cpId, formName);
		if (formName.equals(PARTICIPANT_FORM)) {
			extendedFormIds.addAll(0, formDao.getFormIds(-1L, COMMON_PARTICIPANT));
		} else if (formName.equals(SPECIMEN_FORM)) {
			extendedFormIds.addAll(formDao.getFormIds(-1L, SPECIMEN_EVENT_FORM));
		}

		fields.add(getExtensionField("extensions", "Extensions", extendedFormIds));
		return fields;
	}

	private List<FormFieldSummary> getCpGroupFields(Long groupId, Container form) {
		if (groupId == null || groupId <= 0) {
			return Collections.emptyList();
		}

		CollectionProtocolGroup group = null;
		if (customFieldEntities.containsKey(form.getName()) || staticExtendedForms.contains(form.getName())) {
			group = daoFactory.getCpGroupDao().getById(groupId);
			if (group == null) {
				throw OpenSpecimenException.userError(CpGroupErrorCode.NOT_FOUND, groupId);
			}
		}

		if (group == null) {
			return Collections.emptyList();
		}

		List<FormFieldSummary> fields = new ArrayList<>();

		String entityType = customFieldEntities.get(form.getName());
		if (StringUtils.isNotBlank(entityType)) {
			List<Long> formIds = group.getForms(entityType).stream()
				.map(f -> f.getForm().getId())
				.collect(Collectors.toList());
			fields.add(getExtensionField("customFields", "Custom Fields", formIds));
		}

		if (!staticExtendedForms.contains(form.getName())) {
			return fields;
		}

		List<Long> extendedFormIds = group.getForms(form.getName()).stream()
			.map(f -> f.getForm().getId())
			.collect(Collectors.toList());

		if (form.getName().equals(PARTICIPANT_FORM)) {
			extendedFormIds.addAll(0, formDao.getFormIds(-1L, COMMON_PARTICIPANT));
		} else if (form.getName().equals(SPECIMEN_FORM)) {
			extendedFormIds.addAll(formDao.getFormIds(-1L, SPECIMEN_EVENT_FORM));
		}

		fields.add(getExtensionField("extensions", "Extensions", extendedFormIds));
		return fields;
	}

	private FormFieldSummary getExtensionField(String name, String caption, List<Long> extendedFormIds ) {
		FormFieldSummary field = new FormFieldSummary();
		field.setName(name);
		field.setCaption(caption);
		field.setType("SUBFORM");

		List<FormFieldSummary> extensionFields = new ArrayList<FormFieldSummary>();
		for (Long extendedFormId : extendedFormIds) {
			Container form = Container.getContainer(extendedFormId);

			FormFieldSummary extensionField = new FormFieldSummary();
			extensionField.setName(form.getName());
			extensionField.setCaption(form.getCaption());
			extensionField.setType("SUBFORM");
			extensionField.setSubFields(getFormFields(form));
			extensionField.getSubFields().add(0, getRecordIdField(form));
			extensionFields.add(extensionField);
		}

		field.setSubFields(extensionFields);
		return field;
	}

	private FormContextBean addFormContext(FormContextDetail input) {
		if (isNotCpEntity(input.getLevel())) {
			return addFormContext0(input);
		}

		Long cpId = input.getCollectionProtocol() != null ? input.getCollectionProtocol().getId() : -1L;
		FormContextBean fc = formDao.getAbsFormContext(input.getFormId(), cpId, input.getLevel());
		if (fc != null && fc.getDeletedOn() == null) {
			fc.setMultiRecord(input.isMultiRecord());
			fc.setSortOrder(input.getSortOrder());
			input.setFormCtxtId(fc.getIdentifier());
			notifyContextSaved(fc);
			return fc;
		}

		if (cpId == -1L) {
			if (fc == null) {
				fc = addFormContext0(input);
			} else if (fc.getDeletedOn() != null) {
				fc.setDeletedOn(null);
				notifyContextSaved(fc);
			} else {
				throw OpenSpecimenException.userError(FormErrorCode.MULTIPLE_CTXS_NOT_ALLOWED);
			}
		} else {
			FormContextBean allFc = formDao.getAbsFormContext(input.getFormId(), -1L, input.getLevel());
			if (allFc != null && allFc.getDeletedOn() == null) {
				throw OpenSpecimenException.userError(FormErrorCode.MULTIPLE_CTXS_NOT_ALLOWED);
			}

			if (fc == null) {
				fc = addFormContext0(input);
				if (allFc != null) {
					moveRecords(cpId, input.getLevel(), allFc, fc);
				}
			} else if (fc.getDeletedOn() != null) {
				fc.setDeletedOn(null);
				if (allFc != null) {
					moveRecords(cpId, input.getLevel(), allFc, fc);
				}

				notifyContextSaved(fc);
			} else {
				throw OpenSpecimenException.userError(FormErrorCode.MULTIPLE_CTXS_NOT_ALLOWED);
			}
		}

		return fc;
	}

	private boolean isNotCpEntity(String entity) {
		return !Arrays.asList(
			"Participant", "ParticipantExtension",
			"SpecimenCollectionGroup", "VisitExtension",
			"Specimen", "SpecimenExtension"
		).contains(entity);
	}

	private FormContextBean addFormContext0(FormContextDetail input) {
		Long entityId = input.getEntityId();
		Long cpId     = input.getCollectionProtocol().getId();
		String entity = input.getLevel();
		Long formId   = input.getFormId();

		FormContextBean formCtxt = formDao.getFormContext(entityId == null, entity, entityId == null ? cpId : entityId, formId);
		if (formCtxt == null) {
			formCtxt = new FormContextBean();
			formCtxt.setContainerId(formId);
			formCtxt.setCpId(entity.equals(SPECIMEN_EVENT_FORM) ? -1 : cpId);
			formCtxt.setEntityType(entity);
			formCtxt.setEntityId(entityId);
		}

		formCtxt.setMultiRecord(input.isMultiRecord());
		formCtxt.setSortOrder(input.getSortOrder());

		notifyContextSaved(formCtxt);
		formDao.saveOrUpdate(formCtxt, true);
		input.setFormCtxtId(formCtxt.getIdentifier());
		return formCtxt;
	}


	private FormData saveOrUpdateFormData(Long recordId, FormData formData, boolean isPartial) {
		Map<String, Object> appData = formData.getAppData();
		if (appData.get("formCtxtId") == null || appData.get("objectId") == null) {
			throw new IllegalArgumentException("Invalid form context id or object id ");
		}

		Long objectId = ((Number) appData.get("objectId")).longValue();
		List<Long> formCtxtId = new ArrayList<>();
		formCtxtId.add(((Number) appData.get("formCtxtId")).longValue());
		
		List<FormContextBean> formContexts = formDao.getFormContextsById(formCtxtId);
		if (CollectionUtils.isEmpty(formContexts)) {
			throw new IllegalArgumentException("Invalid form context id");
		}

		FormContextBean formContext = formContexts.get(0);
		Container form = formData.getContainer();
		boolean isInsert = (recordId == null);
		if (!isInsert && formContext.isSysForm()) {
			if (isEditableEvent(form)) {
				List<String> fields = getEditableFields(form);
				isPartial = true;

				FormData allowed = new FormData(form);
				allowed.setRecordId(formData.getRecordId());
				allowed.setParentRecordId(formData.getParentRecordId());
				allowed.setAppData(formData.getAppData());
				for (String field : fields) {
					ControlValue cv = formData.getFieldValue(field);
					if (cv != null) {
						allowed.addFieldValue(cv);
					}
				}

				formData = allowed;
			} else if (!isCollectionOrReceivedEvent(form)) {
				throw OpenSpecimenException.userError(FormErrorCode.SYS_REC_EDIT_NOT_ALLOWED);
			}
		}

		if (!isInsert && isPartial) {
			FormData existing = formDataMgr.getFormData(formData.getContainer(), formData.getRecordId());
			formData = updateFormData(existing, formData);
		}
		
		formData.validate();

		OpenSpecimenEvent<?> collOrRecvEvent = null;
		Object object = null;

		String entityType = formContext.getEntityType();
		if (entityType.equals("Participant")) {
			CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(objectId);
			if (cpr == null) {
				throw OpenSpecimenException.userError(CprErrorCode.M_NOT_FOUND, objectId, 1);
			}

			AccessCtrlMgr.getInstance().ensureUpdateCprRights(cpr);
			object = cpr;
		} else if (entityType.equals("CommonParticipant")) {
			CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(objectId);
			if (cpr == null) {
				throw OpenSpecimenException.userError(CprErrorCode.M_NOT_FOUND, objectId, 1);
			}

			AccessCtrlMgr.getInstance().ensureUpdateCprRights(cpr);
			objectId = cpr.getParticipant().getId();
			object = cpr;
		} else if (entityType.equals("SpecimenCollectionGroup")) {
			Visit visit = daoFactory.getVisitsDao().getById(objectId);
			if (visit == null) {
				throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND, objectId);
			}

			AccessCtrlMgr.getInstance().ensureCreateOrUpdateVisitRights(visit, form.hasPhiFields());
			object = visit;
		} else if (entityType.equals("Specimen") || entityType.equals("SpecimenEvent")) {
			Specimen specimen = ensureSpecimenUpdateRights(objectId, form.hasPhiFields());
			object = specimen;
			if (isCollectionOrReceivedEvent(form)) {
				if (isReceivedEvent(form.getName())) {
					String newLabel = (String) formData.getAppData().get("newSpecimenlabel");
					if (StringUtils.isNotBlank(newLabel)) {
						specimen.setLabel(newLabel.trim());
					}
				}

				specimen.setUpdated(true);
				collOrRecvEvent = new SpecimenSavedEvent(specimen);
			}
		} else if (entityType.equals("User")) {
			User user = daoFactory.getUserDao().getById(objectId);
			if (user == null) {
				throw OpenSpecimenException.userError(UserErrorCode.NOT_FOUND, objectId);
			}

			AccessCtrlMgr.getInstance().ensureUpdateUserRights(user);
			object = user;
		}

		formData.setRecordId(recordId);
		
		FormRecordEntryBean recordEntry = null;
		
		if (isInsert) {
			if (!formContext.isMultiRecord()) {
				Long noOfRecords = formDao.getRecordsCount(formContext.getIdentifier(), objectId);
				if (noOfRecords >= 1L) {
					throw OpenSpecimenException.userError(FormErrorCode.MULTIPLE_RECS_NOT_ALLOWED);
				}
			}
			
			recordEntry = new FormRecordEntryBean();
			recordEntry.setActivityStatus(Status.ACTIVE);			
		} else {
			recordEntry = formDao.getRecordEntry(formContext.getIdentifier(), objectId, recordId);
			if (recordEntry == null || recordEntry.getActivityStatus() != Status.ACTIVE) {
				throw OpenSpecimenException.userError(FormErrorCode.INVALID_REC_ID, recordId);				
			}
		}

		boolean pde = getBoolean(appData.getOrDefault("pde", false));
		UserContext userCtxt = getUserContext(pde);
		recordId = formDataMgr.saveOrUpdateFormData(userCtxt, formData);

		recordEntry.setFormCtxtId(formContext.getIdentifier());
		recordEntry.setObjectId(objectId);
		recordEntry.setRecordId(recordId);
		recordEntry.setUpdatedBy(userCtxt.getUserId());
		recordEntry.setUpdatedTime(Calendar.getInstance().getTime());
		formDao.saveOrUpdateRecordEntry(recordEntry);
		formData.setRecordId(recordId);

		if (collOrRecvEvent != null) {
			EventPublisher.getInstance().publish(collOrRecvEvent);
		} else if (object != null) {
			EventPublisher.getInstance().publish(new FormDataSavedEvent(entityType, object, formData));
		}

		if (pde) {
			savePdeAuditLog(recordEntry.getIdentifier(), isInsert);
		}

		return formData;
	}

	private boolean getBoolean(Object input) {
		if (input instanceof String) {
			return Boolean.parseBoolean(input.toString());
		} else if (input instanceof Number) {
			return ((Number) input).intValue() == 1;
		} else if (input instanceof Boolean) {
			return (Boolean) input;
		} else {
			return false;
		}
	}

	private boolean isCollectionOrReceivedEvent(Container form) {
		return isCollectionOrReceivedEvent(form.getName());
	}

	private boolean isCollectionOrReceivedEvent(String name) {
		return isCollectionEvent(name) || isReceivedEvent(name);
	}

	private boolean isCollectionEvent(String name) {
		return name.equals("SpecimenCollectionEvent");
	}

	private boolean isReceivedEvent(String name) {
		return name.equals("SpecimenReceivedEvent");
	}

	private boolean isEditableEvent(Container form) {
		return isEditableEvent(form.getName());
	}

	private boolean isEditableEvent(String name) {
		List<String> fields = editableEvents.get(name);
		return fields != null && !fields.isEmpty();
	}

	private List<String> getEditableFields(Container form) {
		return getEditableFields(form.getName());
	}

	private List<String> getEditableFields(String name) {
		List<String> fields = editableEvents.get(name);
		return fields != null ? fields : Collections.emptyList();
	}

	private void savePdeAuditLog(Long frId, boolean insert) {
		PdeAuditLog log = new PdeAuditLog();
		log.setEntityType(FormRecordEntryBean.class.getName());
		log.setEntityId(frId);
		log.setOp(insert ? PdeAuditLog.Op.INSERT : PdeAuditLog.Op.UPDATE);
		log.setTime(Calendar.getInstance().getTime());
		log.setUser(AuthUtil.getCurrentUser());
		daoFactory.getPdeAuditLogDao().saveOrUpdate(log);
	}

	private UserContext getUserContext(boolean pde) {
		User user = AuthUtil.getCurrentUser();
		if (pde) {
			user = daoFactory.getUserDao().getSystemUser();
		}

		final User ctxtUser = user;
		return new UserContext() {
			@Override
			public Long getUserId() {
				return ctxtUser != null ? ctxtUser.getId() : null;
			}

			@Override
			public String getUserName() {
				return ctxtUser != null ? ctxtUser.getLoginName() : null;
			}

			@Override
			public String getIpAddress() {
				return AuthUtil.getRemoteAddr();
			}
		};
	}
	
	private List<FormFieldSummary> getFormFields(Container container) {
        List<FormFieldSummary> fields = new ArrayList<>();

        for (Control control : container.getControls()) {
        	if (control.isHidden()) {
        		continue;
        	}

            FormFieldSummary field = new FormFieldSummary();
            field.setName(control.getUserDefinedName());
            field.setCaption(control.getCaption());

            if (control instanceof SubFormControl) {
            	SubFormControl sfCtrl = (SubFormControl)control;
            	field.setFlatten(sfCtrl.isFlatten());

            	if (!sfCtrl.isPathLink()) {
                	field.setType("SUBFORM");
                	field.setSubFields(getFormFields(sfCtrl.getSubContainer()));
                	fields.add(field);
            	} else if (sfCtrl.getName().equals("customFields") && StringUtils.isNotBlank(sfCtrl.getCustomFieldsInfo())) {
            		String[] info = sfCtrl.getCustomFieldsInfo().split(":");  // cpBased:entityType:entityId => false:OrderExtension:-1
					Map<String, Object> extnInfo = getExtensionInfo(Boolean.parseBoolean(info[0]), info[1], Long.parseLong(info[2]));
					if (extnInfo != null) {
						Long extnFormId = (Long)extnInfo.get("formId");
						fields.add(getExtensionField("customFields", "Custom Fields", Arrays.asList(extnFormId)));
					}
				}
            } else if (!(control instanceof Label || control instanceof PageBreak)) {
            	DataType dataType = getType(control);
            	field.setType(dataType.name());
            	                
            	if (control instanceof SelectControl) {
            		SelectControl selectCtrl = (SelectControl)control;
					List<String> pvs = selectCtrl.getPvDataSource()
						.getPermissibleValues(Calendar.getInstance().getTime(), 100)
						.stream().map(PermissibleValue::getValue)
						.collect(Collectors.toList());
            		field.setPvs(pvs);
            	} else if (control instanceof LookupControl) {
            		LookupControl luCtrl = (LookupControl)control;
            		field.setLookupProps(luCtrl.getPvSourceProps());
            	}
            	
            	fields.add(field);
            }
        }

        return fields;		
	}

	private FormData getRecord(Container form, Long objectId, Long formCtxtId, String entityType, Long recordId) {
		FormData formData = formDataMgr.getFormData(form, recordId);
		if (formData == null) {
			throw OpenSpecimenException.userError(FormErrorCode.REC_NOT_FOUND);
		}

		FormRecordEntryBean record = null;
		if (objectId == null || entityType == null) {
			record     = formDao.getRecordEntry(formData.getContainer().getId(), formData.getRecordId());
			objectId   = record.getObjectId();
			formCtxtId = record.getFormCtxtId();
			entityType = record.getEntityType();
		}

		if (formData.getContainer().hasPhiFields() && !isPhiAccessAllowed(entityType, objectId)) {
			formData.maskPhiFieldValues();
		}

		Map<String, Object> appData = formData.getAppData();
		appData.put("formCtxtId", formCtxtId);
		appData.put("objectId",   objectId);
		if (record != null) {
			appData.put("sysForm",     record.isSysRecord());
			appData.put("multiRecord", record.isMultiRecord());
		}

		return formData;
	}

	private boolean isPhiAccessAllowed(String entityType, Long objectId) {
		boolean allowPhiAccess = false;
		if (entityType.equals(PARTICIPANT_FORM) || Participant.EXTN.equals(entityType)) {
			allowPhiAccess = AccessCtrlMgr.getInstance().ensureReadCprRights(objectId);
		} else if (entityType.equals(COMMON_PARTICIPANT)) {
			allowPhiAccess = AccessCtrlMgr.getInstance().ensureReadParticipantRights(objectId);
		} else if (entityType.equals(SCG_FORM) || Visit.EXTN.equals(entityType)) {
			allowPhiAccess = AccessCtrlMgr.getInstance().ensureReadVisitRights(objectId, true);
		} else if (entityType.equals(SPECIMEN_FORM) || entityType.equals(SPECIMEN_EVENT_FORM) || Specimen.EXTN.equals(entityType)) {
			allowPhiAccess = AccessCtrlMgr.getInstance().ensureReadSpecimenRights(objectId, true);
		} else {
			allowPhiAccess = true;
		}

		return allowPhiAccess;
	}

	private FormFieldSummary getRecordIdField(Container form) {
		Control pkCtrl = form.getPrimaryKeyControl();

		FormFieldSummary field = new FormFieldSummary();
		field.setName(pkCtrl.getUserDefinedName());
		field.setCaption(pkCtrl.getCaption());
		field.setType(getType(pkCtrl).name());
		return field;
	}
	
	private DataType getType(Control ctrl) {
		if (ctrl instanceof FileUploadControl) {
			return DataType.STRING;
		} else if (ctrl instanceof LookupControl) {
			return ((LookupControl)ctrl).getValueType();
		} else {
			return ctrl.getDataType();
		}
	}
	
	private FormData updateFormData(FormData existing, FormData formData) {
		existing.setAppData(formData.getAppData());
		for (ControlValue ctrlValue : formData.getFieldValues()) {
			existing.addFieldValue(ctrlValue);
		}
		
		return existing;
	}

	private void notifyContextSaved(FormContextBean formCtxt) {
		notifyContextSaved(formCtxt.getEntityType(), formCtxt);
		notifyContextSaved("*", formCtxt);
	}

	private void notifyContextSaved(String entityType, FormContextBean formCtxt) {
		List<FormContextProcessor> procs = ctxtProcs.get(entityType);
		if (procs != null) {
			procs.forEach(proc -> proc.onSaveOrUpdate(formCtxt));
		}
	}

	private void notifyContextRemoved(FormContextBean formCtxt) {
		notifyContextRemoved(formCtxt.getEntityType(), formCtxt);
		notifyContextRemoved("*", formCtxt);
	}

	private void notifyContextRemoved(String entityType, FormContextBean formCtxt) {
		List<FormContextProcessor> procs = ctxtProcs.get(entityType);
		if (procs != null) {
			procs.forEach(proc -> proc.onRemove(formCtxt));
		}
	}

	private List<FormRecordsList> getFormRecords(String entityType, Long inputFormId, Long objectId) {
		List<FormRecordsList> result = new ArrayList<>();

		Map<Long, List<FormRecordSummary>> records = formDao.getFormRecords(objectId, entityType, inputFormId);
		for (Map.Entry<Long, List<FormRecordSummary>> formRecs : records.entrySet()) {
			Long formId = formRecs.getKey();
			Container container = Container.getContainer(formId);

			List<Long> recIds = new ArrayList<>();
			Map<Long, FormRecordSummary> recMap = new HashMap<>();
			for (FormRecordSummary rec : formRecs.getValue()) {
				recMap.put(rec.getRecordId(), rec);
				recIds.add(rec.getRecordId());
			}

			List<FormData> summaryRecs = formDataMgr.getSummaryData(container, recIds);
			for (FormData rec : summaryRecs) {
				recMap.get(rec.getRecordId()).addFieldValues(rec.getFieldValues());
			}

			result.add(FormRecordsList.from(container, recMap.values()));
		}

		return result;
	}

	private Specimen ensureSpecimenUpdateRights(Long objectId, boolean checkPhiAccess) {
		Specimen specimen = daoFactory.getSpecimenDao().getById(objectId);
		if (specimen == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, objectId);
		} else if (!specimen.isEditAllowed()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.EDIT_NOT_ALLOWED, specimen.getLabel());
		}

		AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(specimen, checkPhiAccess);
		return specimen;
	}

	private int moveRecords(Long formId, String entity, long srcCpId, long tgtCpId) {
		if (srcCpId == tgtCpId) {
			// Don't need to do any work when source and target CPs are the same.
			return 0;
		}

		Long cpId = srcCpId != -1L ? srcCpId : tgtCpId;
		FormContextBean srcFc = formDao.getAbsFormContext(formId, srcCpId, entity);
		FormContextBean tgtFc = formDao.getAbsFormContext(formId, tgtCpId, entity);
		return moveRecords(cpId, entity, srcFc, tgtFc);
	}

	private int moveRecords(Long cpId, String entity, FormContextBean srcFc, FormContextBean tgtFc) {
		Date prev = tgtFc.getDeletedOn();
		tgtFc.setDeletedOn(prev == null ? Calendar.getInstance().getTime() : null);
		formDao.saveOrUpdate(tgtFc, true);

		int count = 0;
		switch (entity) {
			case "Participant":
			case "ParticipantExtension":
				count = formDao.moveRegistrationRecords(cpId, srcFc.getIdentifier(), tgtFc.getIdentifier());
				break;

			case "SpecimenCollectionGroup":
			case "VisitExtension":
				count = formDao.moveVisitRecords(cpId, srcFc.getIdentifier(), tgtFc.getIdentifier());
				break;

			case "Specimen":
			case "SpecimenExtension":
				count = formDao.moveSpecimenRecords(cpId, srcFc.getIdentifier(), tgtFc.getIdentifier());
				break;
		}

		tgtFc.setDeletedOn(prev);
		formDao.saveOrUpdate(tgtFc, true);
		return count;
	}

	private List<Long> getCpIds(String cpShortTitle, String cpGroupName) {
		if (StringUtils.isNotBlank(cpShortTitle)) {
			return Collections.singletonList(getCp(cpShortTitle).getId());
		} else if (StringUtils.isNotBlank(cpGroupName)) {
			CollectionProtocolGroup grp = daoFactory.getCpGroupDao().getByName(cpGroupName);
			if (grp == null) {
				throw OpenSpecimenException.userError(CpGroupErrorCode.NOT_FOUND, cpGroupName);
			}

			return new ArrayList<>(grp.getCpIds());
		} else {
			return Collections.singletonList(-1L);
		}
	}

	private CollectionProtocol getCp(String cpShortTitle) {
		CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(cpShortTitle.trim());
		if (cp == null) {
			throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND, cpShortTitle);
		}

		return cp;
	}

	private FormContextBean getFormCtxt(Long formId, Long cpId, String entity) {
		FormContextBean fc = formDao.getFormContext(formId, cpId, entity);
		if (fc == null) {
			throw OpenSpecimenException.userError(FormErrorCode.NO_ASSOCIATION, cpId, formId);
		}

		return fc;
	}

	private Function<ExportJob, List<? extends Object>> getFormRecordsGenerator() {
		return new Function<ExportJob, List<? extends Object>>() {
			private boolean endOfRecords = false;

			private boolean paramsInited = false;

			private Container form;

			private boolean formHasPhi;

			private String entityType;

			private CollectionProtocol cp;

			private Long cpId;

			private Long entityId;

			private Set<SiteCpPair> siteCps;

			private int startAt;

			private Object lastObj;

			private Long lastObjId;

			private boolean lastObjReadAllowed;

			private boolean lastObjPhiAllowed;

			@Override
			public List<? extends Object> apply(ExportJob job) {
				if (endOfRecords) {
					return Collections.emptyList();
				}

				if (!paramsInited) {
					initParams(job);
				}


				Function<ExportJob, List<Map<String, Object>>> recordsFn = null;
				if (entityType.equals("Participant") || entityType.equals("CommonParticipant")) {
					recordsFn = this::getRegistrationFormRecords;
				} else if (entityType.equals("SpecimenCollectionGroup")) {
					recordsFn = this::getVisitFormRecords;
				} else if (entityType.equals("Specimen") || entityType.equals("SpecimenEvent")) {
					recordsFn = this::getSpmnFormRecords;
				} else if (entityType.equals("User")) {
					recordsFn = this::getUserFormRecords;
				}

				List<Map<String, Object>> records = null;
				if (recordsFn != null) {
					while (!endOfRecords && CollectionUtils.isEmpty(records)) {
						records = recordsFn.apply(job);
					}
				}

				return records;
			}

			private void initParams(ExportJob job) {
				Map<String, String> params = job.getParams();
				if (params == null) {
					params = Collections.emptyMap();
				}

				form = getContainer(null, params.get("formName"));
				formHasPhi = form.hasPhiFields();

				entityType = params.get("entityType");
				if (StringUtils.isBlank(entityType)) {
					throw OpenSpecimenException.userError(FormErrorCode.ENTITY_TYPE_REQUIRED);
				}

				String cpIdStr = params.get("cpId");
				if (StringUtils.isNotBlank(cpIdStr)) {
					try {
						cpId = Long.parseLong(cpIdStr);
					} catch (Exception e) {
						logger.error("Invalid CP ID: " + cpIdStr, e);
					}
				}

				String entityIdStr = params.get("entityId");
				if (StringUtils.isNotBlank(entityIdStr)) {
					try {
						entityId = Long.parseLong(entityIdStr);
					} catch (Exception e) {
						logger.error("Invalid entity ID: " + entityIdStr, e);
					}
				}

				boolean allowed = false;
				if (entityType.equals("Participant") || entityType.equals("CommonParticipant")) {
					if (cpId != null && cpId != -1L) {
						allowed = AccessCtrlMgr.getInstance().hasCprEximRights(cpId);
					} else {
						siteCps = AccessCtrlMgr.getInstance().getSiteCps(Resource.PARTICIPANT, Operation.EXIM);
						if (siteCps != null && siteCps.isEmpty()) {
							siteCps = AccessCtrlMgr.getInstance().getSiteCps(Resource.PARTICIPANT_DEID, Operation.EXIM);
						}

						allowed = (siteCps == null || !siteCps.isEmpty());
					}
				} else if (entityType.equals("SpecimenCollectionGroup")) {
					if (cpId != null && cpId != -1L) {
						allowed = AccessCtrlMgr.getInstance().hasVisitEximRights(cpId);
					} else {
						siteCps = AccessCtrlMgr.getInstance().getSiteCps(Resource.VISIT, Operation.EXIM);
						allowed = (siteCps == null || !siteCps.isEmpty());
					}
				} else if (entityType.equals("Specimen") || entityType.equals("SpecimenEvent")) {
					if (cpId != null && cpId != -1L) {
						allowed = AccessCtrlMgr.getInstance().hasSpecimenEximRights(cpId);
					} else {
						siteCps = AccessCtrlMgr.getInstance().getSiteCps(Resource.SPECIMEN, Operation.EXIM);
						allowed = (siteCps == null || !siteCps.isEmpty());
					}
				} else if (entityType.equals("User")) {
					if (entityId != null && entityId != -1L) {
						if (AuthUtil.isAdmin()) {
							allowed = true;
						} else if (AuthUtil.isInstituteAdmin() && AuthUtil.getCurrentUserInstitute().getId().equals(entityId)) {
							allowed = true;
						} else {
							allowed = false;
						}
					} else {
						allowed = AuthUtil.isAdmin();
					}
				}

				if (!allowed) {
					endOfRecords = true;
				}

				paramsInited = true;
			}

			private List<Map<String, Object>> getRegistrationFormRecords(ExportJob job) {
				String entityType = job.getParams().get("entityType");

				return getRecords(
					job,
					"ppids",
					(ppids) -> {
						if (entityType.equals("Participant")) {
							return formDao.getRegistrationRecords(cpId, siteCps, form.getId(), ppids, startAt, 100);
						} else {
							return formDao.getParticipantRecords(cpId, siteCps, form.getId(), ppids, startAt, 100);
						}
					},
					"cprId",
					(cprId) -> daoFactory.getCprDao().getById(cprId),
					(cpr) -> {
						if (entityType.equals("Participant")) {
							return AccessCtrlMgr.getInstance().ensureReadCprRights((CollectionProtocolRegistration) cpr);
						} else {
							return AccessCtrlMgr.getInstance().ensureReadParticipantRights(((CollectionProtocolRegistration) cpr).getParticipant().getId());
						}
					},
					(cprFormValueMap) -> {
						CollectionProtocolRegistration cpr = (CollectionProtocolRegistration) cprFormValueMap.first();
						Map<String, Object> valueMap = cprFormValueMap.second();

						Map<String, Object> formData = new HashMap<>();
						formData.put("recordId", valueMap.get("id"));
						formData.put("cpShortTitle", cpr.getCollectionProtocol().getShortTitle());
						formData.put("ppid", cpr.getPpid());
						formData.put("formValueMap", valueMap);
						return formData;
					}
				);
			}

			private List<Map<String, Object>> getVisitFormRecords(ExportJob job) {
				return getRecords(
					job,
					"visitNames",
					(visitNames) -> formDao.getVisitRecords(cpId, siteCps, form.getId(), visitNames, startAt, 100),
					"visitId",
					(visitId) -> daoFactory.getVisitsDao().getById(visitId),
					(visit) -> AccessCtrlMgr.getInstance().ensureReadVisitRights((Visit) visit, true),
					(visitFormValueMap) -> {
						Visit visit = (Visit) visitFormValueMap.first();
						Map<String, Object> valueMap = visitFormValueMap.second();

						Map<String, Object> formData = new HashMap<>();
						formData.put("recordId", valueMap.get("id"));
						formData.put("cpShortTitle", visit.getCollectionProtocol().getShortTitle());
						formData.put("visitName", visit.getName());
						formData.put("formValueMap", valueMap);
						return formData;
					}
				);
			}

			private List<Map<String, Object>> getSpmnFormRecords(ExportJob job) {
				return getRecords(
					job,
					"specimenLabels",
					(spmnLabels) -> formDao.getSpecimenRecords(cpId, siteCps, form.getId(), entityType, spmnLabels, startAt, 100),
					"spmnId",
					(specimenId) -> daoFactory.getSpecimenDao().getById(specimenId),
					(specimen) -> {
						AccessCtrlMgr.SpecimenAccessRights rights = AccessCtrlMgr.getInstance()
							.ensureReadSpecimenRights((Specimen) specimen, true);
						return rights.phiAccess;
					},
					(spmnFormValueMap) -> {
						Specimen specimen = (Specimen) spmnFormValueMap.first();
						Map<String, Object> valueMap = spmnFormValueMap.second();

						Map<String, Object> formData = new HashMap<>();
						formData.put("recordId", valueMap.get("id"));
						formData.put("cpShortTitle", specimen.getCollectionProtocol().getShortTitle());
						formData.put("specimenLabel", specimen.getLabel());
						formData.put("barcode", specimen.getBarcode());
						formData.put("formValueMap", valueMap);
						return formData;
					}
				);
			}

			private List<Map<String, Object>> getUserFormRecords(ExportJob job) {
				return getRecords(
					job,
					"emailIds",
					(emailIds) -> daoFactory.getUserDao().getFormRecords(entityId, form.getId(), emailIds, startAt, 100),
					"userId",
					(userId) -> daoFactory.getUserDao().getById(userId),
					(user) -> {
						try {
							AccessCtrlMgr.getInstance().ensureUpdateUserRights((User) user);
							return true;
						} catch (OpenSpecimenException ose) {
							if (ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
								return false;
							}

							throw ose;
						}
					},
					(userFormValueMap) -> {
						User user = (User) userFormValueMap.first();
						Map<String, Object> valueMap = userFormValueMap.second();

						Map<String, Object> formData = new HashMap<>();
						formData.put("recordId", valueMap.get("id"));
						formData.put("emailAddress", user.getEmailAddress());
						formData.put("formValueMap", valueMap);
						return formData;
					}
				);
			}

			private List<Map<String, Object>> getRecords(
				ExportJob job,
				String namesCsvVar,
				Function<List<String>, List<Map<String, Object>>> getFormRecs,
				String objIdVar,
				Function<Long, Object> getObj,
				Function<Object, Boolean> objAccessChecker,
				Function<Pair<Object, Map<String, Object>>, Map<String, Object>> toFormRec) {

				String namesCsv = job.getParams().get(namesCsvVar);
				List<String> names = null;
				if (StringUtils.isNotBlank(namesCsv)) {
					names = Utility.csvToStringList(namesCsv);
					endOfRecords = true;
				}

				List<Map<String, Object>> records = getFormRecs.apply(names);
				startAt += records.size();
				if (records.size() < 100) {
					endOfRecords = true;
				}

				List<Map<String, Object>> result = new ArrayList<>();
				for (Map<String, Object> record : records) {
					Long objId = (Long)record.get(objIdVar);
					if (objId.equals(lastObjId) && !lastObjReadAllowed) {
						continue;
					}

					if (!objId.equals(lastObjId)) {
						try {
							lastObj = getObj.apply(objId);
							lastObjId = objId;
							lastObjPhiAllowed = objAccessChecker.apply(lastObj);
							lastObjReadAllowed = true;
						} catch (OpenSpecimenException ose) {
							if (isAccessDeniedError(ose)) {
								lastObjReadAllowed = lastObjPhiAllowed = false;
								continue;
							}

							throw ose;
						}
					}

					Long recordId = (Long)record.get("recordId");
					result.add(toFormRec.apply(Pair.make(lastObj, getFormData(recordId, !lastObjPhiAllowed))));
				}

				return result;
			}

			private Map<String, Object> getFormData(Long recordId, boolean maskPhi) {
				FormData formData = formDataMgr.getFormData(form, recordId);

				if (formHasPhi && maskPhi) {
					formData.maskPhiFieldValues();
				}

				return formData.getFieldNameValueMap(true);
			}

			private boolean isAccessDeniedError(OpenSpecimenException ose) {
				return ose.containsError(RbacErrorCode.ACCESS_DENIED);
			}
		};
	}

	private Container getContainer(Long formId, String formName) {
		Object key = null;
		Container form = null;
		if (formId != null) {
			form = Container.getContainer(formId);
			key = formId;
		} else if (StringUtils.isNotBlank(formName)) {
			form = Container.getContainer(formName);
			key = formName;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(FormErrorCode.NAME_REQUIRED);
		} else if (form == null) {
			throw OpenSpecimenException.userError(FormErrorCode.NOT_FOUND, key, 1);
		}

		return form;
	}
}
