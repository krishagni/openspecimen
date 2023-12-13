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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.web.multipart.MultipartFile;

import com.krishagni.catissueplus.core.administrative.domain.FormDataDeleteEvent;
import com.krishagni.catissueplus.core.administrative.domain.FormDataSavedEvent;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserGroupErrorCode;
import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.administrative.repository.FormListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenReceivedEvent;
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
import com.krishagni.catissueplus.core.common.domain.PrintItem;
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
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
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
import com.krishagni.catissueplus.core.de.services.FormAccessChecker;
import com.krishagni.catissueplus.core.de.services.FormContextProcessor;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.catissueplus.core.exporter.services.impl.ExporterContextHolder;
import com.krishagni.catissueplus.core.importer.services.impl.ImporterContextHolder;
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
	private static final LogUtil logger = LogUtil.getLogger(FormServiceImpl.class);

	private static final String CP_FORM = "CollectionProtocol";

	private static final String PARTICIPANT_FORM = "Participant";

	private static final String COMMON_PARTICIPANT = "CommonParticipant";
	
	private static final String SCG_FORM = "SpecimenCollectionGroup";
	
	private static final String SPECIMEN_FORM = "Specimen";
	
	private static final String SPECIMEN_EVENT_FORM = "SpecimenEvent";
	
	private static Set<String> staticExtendedForms = new HashSet<>();
	
	private static Map<String, String> customFieldEntities = new HashMap<>();

	private static Map<String, List<String>> editableEvents;

	private Map<String, FormAccessChecker> formAccessCheckers = new HashMap<>();

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

		formAccessCheckers.put("CollectionProtocolExtension", SYS_FORMS_CHECKER);
		formAccessCheckers.put("StorageContainerExtension", SYS_FORMS_CHECKER);
		formAccessCheckers.put("DpRequirementExtension", SYS_FORMS_CHECKER);
		formAccessCheckers.put("DistributionProtocolExtension", SYS_FORMS_CHECKER);
		formAccessCheckers.put("SiteExtension", SYS_FORMS_CHECKER);
		formAccessCheckers.put("CommonParticipant", SYS_FORMS_CHECKER);
		formAccessCheckers.put("SpecimenEvent", SYS_FORMS_CHECKER);

		formAccessCheckers.put("ParticipantExtension", CP_FORMS_CHECKER);
		formAccessCheckers.put("Participant", CP_FORMS_CHECKER);
		formAccessCheckers.put("VisitExtension", CP_FORMS_CHECKER);
		formAccessCheckers.put("SpecimenCollectionGroup", CP_FORMS_CHECKER);
		formAccessCheckers.put("SpecimenExtension", CP_FORMS_CHECKER);
		formAccessCheckers.put("Specimen", CP_FORMS_CHECKER);

		formAccessCheckers.put("User", USER_FORM_CHECKER);
		formAccessCheckers.put("UserProfile", USER_FORM_CHECKER);
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
	public ResponseEvent<Container> getFormDefinitionByName(RequestEvent<String> req) {
		Container form = getContainer(null, req.getPayload());
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

		Form form = null;
		if (input.getId() != null) {
			form = formDao.getFormById(input.getId());
			if (form == null) {
				return ResponseEvent.userError(FormErrorCode.NOT_FOUND, input.getId());
			}
		}

		if (StringUtils.isBlank(input.getName())) {
			return ResponseEvent.userError(FormErrorCode.NAME_REQUIRED);
		}

		if (form == null || !form.getName().equals(input.getName())) {
			//
			// Either a new form is being created or the form name has been modified, in which case,
			// we need to ensure the form name doesn't conflict with the existing forms
			// i.e. there should be no form in the DB with the same name
			//
			Form dbForm = formDao.getFormByName(input.getName());
			if (dbForm != null) {
				return ResponseEvent.userError(FormErrorCode.DUP_NAME, input.getName());
			}
		}

		if (form != null) {
			for (FormContextBean formCtxt : form.getAssociations()) {
				ensureUpdateAllowed(formCtxt);
			}
		}

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

			for (Form form : forms) {
				form.getAssociations().forEach(this::ensureUpdateAllowed);
			}

			formIds.forEach(formId -> Container.softDeleteContainer(getUserContext(false), formId));
			formDao.deleteFormContexts(formIds);

			//
			// TODO: Ideally, this API should have emitted events saying so and so form has been deleted.
			// TODO: The interested listeners, like CP group listener, would then take appropriate actions
			//
			daoFactory.getCpGroupDao().deleteForms(formIds);
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
		List<FormContextDetail> contexts = formDao.getFormContexts(req.getPayload());
		Map<Long, FormContextDetail> contextsMap = Utility.toLinkedMap(contexts, FormContextDetail::getFormCtxtId, fc -> fc);
		if (!contextsMap.isEmpty()) {
			Map<Long, List<UserGroupSummary>> notifUsers = formDao.getNotifUsers(contextsMap.keySet());
			notifUsers.forEach((fcId, groups) -> contextsMap.get(fcId).setNotifUserGroups(groups));
		}

		return ResponseEvent.response(contexts);
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<FormContextDetail>> addFormContexts(RequestEvent<List<FormContextDetail>> req) { // TODO: check form is deleted
		try {
			Map<Long, Form> forms = new HashMap<>();

			List<FormContextDetail> result = new ArrayList<>();
			for (FormContextDetail formCtxtDetail : req.getPayload()) {
				Long formId = formCtxtDetail.getFormId();
				Form form = forms.get(formCtxtDetail.getFormId());
				if (form == null) {
					form = formDao.getFormById(formId);
					if (form == null) {
						return ResponseEvent.userError(FormErrorCode.NOT_FOUND, formId);
					}

					forms.put(formId, form);
				}

				form.getAssociations().forEach(this::ensureUpdateAllowed);
				String entity = formCtxtDetail.getLevel();
				if (StringUtils.isBlank(entity)) {
					return ResponseEvent.userError(FormErrorCode.ENTITY_TYPE_REQUIRED);
				}

				ensureUpdateAllowed(formCtxtDetail);
				addFormContext(formCtxtDetail);
				result.add(formCtxtDetail);
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

			ensureUpdateAllowed(formCtx);
			notifyContextRemoved(formCtx);
			switch (opDetail.getRemoveType()) {
				case SOFT_REMOVE -> formCtx.setDeletedOn(Calendar.getInstance().getTime());
				case HARD_REMOVE -> formDao.delete(formCtx);
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
	public ResponseEvent<List<FormCtxtSummary>> getEntityForms(RequestEvent<ListEntityFormsOp> req) {
		try {
			ListEntityFormsOp opDetail = req.getPayload();
			
			List<FormCtxtSummary> forms = null;
			
			Long entityId = opDetail.getEntityId();
			switch (opDetail.getEntityType()) {
				case COLLECTION_PROTOCOL_REGISTRATION -> {
					AccessCtrlMgr.getInstance().ensureReadCprRights(entityId);
					forms = formDao.getParticipantForms(entityId);
					forms.addAll(formDao.getCprForms(entityId));
				}
				case SPECIMEN -> {
					AccessCtrlMgr.getInstance().ensureReadSpecimenRights(entityId);
					forms = formDao.getSpecimenForms(opDetail.getEntityId());
				}
				case SPECIMEN_COLLECTION_GROUP -> {
					AccessCtrlMgr.getInstance().ensureReadVisitRights(entityId);
					forms = formDao.getScgForms(opDetail.getEntityId());
				}
				case SPECIMEN_EVENT -> {
					AccessCtrlMgr.getInstance().ensureReadSpecimenRights(entityId);
					forms = formDao.getSpecimenEventForms(opDetail.getEntityId());
				}
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
		if (form == null) {
			return ResponseEvent.userError(FormErrorCode.INVALID_FORM_CTXT, opDetail.getFormCtxtId());
		}

		if (opDetail.getAccessAllowed() != null && !opDetail.getAccessAllowed().apply(form.getEntityType())) {
			return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
		}

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
			formData.getAppData().remove("object");
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
			List<FormData> savedFormDataList = new ArrayList<>();
			for (FormData formData : req.getPayload()) {
				FormData savedFormData = saveOrUpdateFormData(formData.getRecordId(), formData, true);
				savedFormData.getAppData().remove("object");
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
			switch (entityType) {
				case "Participant" -> {
					CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(objectId);
					if (cpr == null) {
						throw OpenSpecimenException.userError(CprErrorCode.M_NOT_FOUND, objectId, 1);
					}
					object = cpr;
				}
				case "CommonParticipant" -> {
					Participant participant = daoFactory.getParticipantDao().getById(objectId);
					if (participant == null) {
						throw OpenSpecimenException.userError(ParticipantErrorCode.NOT_FOUND, objectId);
					}
					object = participant;
				}
				case "SpecimenCollectionGroup" -> {
					Visit visit = daoFactory.getVisitsDao().getById(objectId);
					if (visit == null) {
						throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND, objectId);
					}
					object = visit;
				}
				case "Specimen", "SpecimenEvent" -> object = getSpecimen(objectId);
				case "User", "UserProfile" -> {
					User user = daoFactory.getUserDao().getById(objectId);
					if (user == null) {
						throw OpenSpecimenException.userError(UserErrorCode.NOT_FOUND, objectId);
					}
					object = user;
				}
			}

			String accessType = getCuratedEntityType(entityType);
			FormAccessChecker accessChecker = formAccessCheckers.get(accessType);
			if (accessChecker == null || (object != null && !accessChecker.isDataUpdateAllowed(object)) || (object == null && !accessChecker.isDataUpdateAllowed(accessType, objectId))) {
				throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}
			
			recEntry.delete();
			formDao.saveOrUpdateRecordEntry(recEntry);
			if (object != null) {
				EventPublisher.getInstance().publish(new FormDataDeleteEvent(entityType, object, recEntry));
			}

			notifFormDeleted(object, recEntry);
			return  ResponseEvent.response(crit.getRecordId());
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

			String accessType = getCuratedEntityType(entityType);
			FormAccessChecker checker = formAccessCheckers.get(accessType);
			if (checker == null || !checker.isDataReadAllowed(accessType, objectId)) {
				throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
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

			SelectControl selectCtrl = (SelectControl) control;
			int maxPvs = input.getMaxResults() <= 0 ? 100 : input.getMaxResults();
			List<PermissibleValue> pvs = selectCtrl.getPvDataSource().getPermissibleValues(input.getQueries(), maxPvs);
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

	@Override
	public void addAccessChecker(String entityType, FormAccessChecker checker) {
		formAccessCheckers.put(entityType, checker);
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

			Control recStatusCtrl = form.getRecordStatusControl();
			FormFieldSummary recDetails = new FormFieldSummary();
			recDetails.setName(recStatusCtrl.getName());
			recDetails.setCaption(recStatusCtrl.getCaption());
			recDetails.setType("SUBFORM");
			extensionField.getSubFields().add(recDetails);

			FormFieldSummary status = new FormFieldSummary();
			status.setName("status");
			status.setType("STRING");
			status.setCaption(form.getCaption() + " Status");
			status.setPvs(Arrays.asList("COMPLETE", "DRAFT"));
			recDetails.setSubFields(new ArrayList<>(List.of(status)));
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
			addNotifSettings(input, fc);
			input.setFormCtxtId(fc.getIdentifier());
			notifyContextSaved(fc);
			return fc;
		}

		if (cpId == -1L) {
			if (fc == null) {
				fc = addFormContext0(input);
			} else if (fc.getDeletedOn() != null) {
				fc.setDeletedOn(null);
				addNotifSettings(input, fc);
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
				addNotifSettings(input, fc);
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
		addNotifSettings(input, formCtxt);

		notifyContextSaved(formCtxt);
		formDao.saveOrUpdate(formCtxt, true);
		input.setFormCtxtId(formCtxt.getIdentifier());
		return formCtxt;
	}

	private void addNotifSettings(FormContextDetail input, FormContextBean fc) {
		fc.setNotifEnabled(input.isNotifEnabled());
		if (!fc.isNotifEnabled()) {
			fc.setDataInNotif(false);
			fc.getNotifUserGroups().clear();
			return;
		}

		fc.setDataInNotif(input.isDataInNotif());
		if (CollectionUtils.isEmpty(input.getNotifUserGroups())) {
			fc.getNotifUserGroups().clear();
			return;
		}

		Set<UserGroup> userGroups = new HashSet<>();
		for (UserGroupSummary inputUg : input.getNotifUserGroups()) {
			Object key = null;
			UserGroup userGroup = null;
			if (inputUg.getId() != null) {
				userGroup = daoFactory.getUserGroupDao().getById(inputUg.getId());
				key = inputUg.getId();
			} else if (StringUtils.isNotBlank(inputUg.getName())) {
				userGroup = daoFactory.getUserGroupDao().getByName(inputUg.getName());
				key = inputUg.getName();
			}

			if (key != null && userGroup == null) {
				throw OpenSpecimenException.userError(UserGroupErrorCode.NOT_FOUND, key);
			} else if (userGroup != null) {
				userGroups.add(userGroup);
			}
		}

		fc.getNotifUserGroups().retainAll(userGroups);
		fc.getNotifUserGroups().addAll(userGroups);
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

		String formStatus = (String) appData.get("formStatus");
		if (StringUtils.isBlank(formStatus)) {
			formStatus = "COMPLETE";
		}

		if (!formStatus.equals("COMPLETE") && !formStatus.equals("DRAFT")) {
			throw OpenSpecimenException.userError(FormErrorCode.INV_DATA_STATUS, formStatus);
		}

		if (formStatus.equals("COMPLETE")) {
			formData.validate();
		}

		String prevStatus = null;
		String prevRecv = null;
		OpenSpecimenEvent<?> collOrRecvEvent = null;
		Object object = null;

		CollectionProtocol cp = null;
		String entityType = formContext.getEntityType();
		switch (entityType) {
			case "Participant" -> {
				CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(objectId);
				if (cpr == null) {
					throw OpenSpecimenException.userError(CprErrorCode.M_NOT_FOUND, objectId, 1);
				}

				object = cpr;
				cp = cpr.getCollectionProtocol();
			}
			case "CommonParticipant" -> {
				CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(objectId);
				if (cpr == null) {
					throw OpenSpecimenException.userError(CprErrorCode.M_NOT_FOUND, objectId, 1);
				}

				objectId = cpr.getParticipant().getId();
				object = cpr;
				cp = cpr.getCollectionProtocol();
			}
			case "SpecimenCollectionGroup" -> {
				Visit visit = daoFactory.getVisitsDao().getById(objectId);
				if (visit == null) {
					throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND, objectId);
				}

				object = visit;
				cp = visit.getCollectionProtocol();
			}
			case "Specimen", "SpecimenEvent" -> {
				Specimen specimen = getSpecimen(objectId);
				object = specimen;
				cp = specimen.getCollectionProtocol();
				if (isCollectionOrReceivedEvent(form)) {
					if (isReceivedEvent(form.getName())) {
						String newLabel = (String) formData.getAppData().get("newSpecimenlabel");
						if (StringUtils.isNotBlank(newLabel)) {
							specimen.setLabel(newLabel.trim());
						}

						prevStatus = specimen.getCollectionStatus();
						if (specimen.getReceivedEvent() != null && specimen.getReceivedEvent().getQuality() != null) {
							prevRecv = specimen.getReceivedEvent().getQuality().getValue();
						}

						setReceivedEventUserTime(formData);
					}

					specimen.setUpdated(true);
					collOrRecvEvent = new SpecimenSavedEvent(specimen);
				}
			}
			case "User", "UserProfile" -> {
				User user = daoFactory.getUserDao().getById(objectId);
				if (user == null) {
					throw OpenSpecimenException.userError(UserErrorCode.NOT_FOUND, objectId);
				}

				object = user;
			}
		}

		String accessType = getCuratedEntityType(entityType);
		FormAccessChecker accessChecker = formAccessCheckers.get(accessType);
		if (accessChecker == null || (object != null && !accessChecker.isDataUpdateAllowed(object)) || (object == null && !accessChecker.isDataUpdateAllowed(accessType, objectId))) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		BaseEntity.DataEntryStatus dataEntryStatus = BaseEntity.DataEntryStatus.valueOf(formStatus);
		if (dataEntryStatus == BaseEntity.DataEntryStatus.DRAFT && cp != null && !cp.draftDataEntryEnabled()) {
			throw OpenSpecimenException.userError(CprErrorCode.DRAFT_NOT_ALLOWED, cp.getShortTitle());
		}

		formData.setRecordId(recordId);
		formData.getAppData().put("object", object);
		
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
		recordEntry.setFormStatus(dataEntryStatus);
		formDao.saveOrUpdateRecordEntry(recordEntry);
		formData.setRecordId(recordId);

		if (collOrRecvEvent != null) {
			Specimen specimen = (Specimen) collOrRecvEvent.getEventData();
			if (isReceivedEvent(form.getName())) {
				// This is to flush out the old received event values
				specimen.getReceivedEvent().setAttrValues(formData.getFieldNameValueMap(true));
			}

			EventPublisher.getInstance().publish(collOrRecvEvent);
			if (isReceivedEvent(form.getName())) {
				Object printLabel = formData.getAppData().get("printLabel");
				if (printLabel != null && printLabel.toString().toLowerCase().equals("true")) {
					PrintItem<Specimen> printItem = PrintItem.make(specimen, specimen.getCopiesToPrint());
					Specimen.getLabelPrinter().print(Collections.singletonList(printItem));
				}

				specimen.prePrintChildrenLabels(prevStatus, prevRecv);
			}
		} else if (object != null) {
			EventPublisher.getInstance().publish(new FormDataSavedEvent(entityType, object, formData));
		}

		if (pde) {
			savePdeAuditLog(recordEntry.getIdentifier(), isInsert);
		}

		notifyFormSave(object, formContext, recordEntry, formData, isInsert);
		return formData;
	}

	private void setReceivedEventUserTime(FormData formData) {
		ControlValue qualityCv = formData.getFieldValue("quality");
		String quality = null;
		if (qualityCv != null && qualityCv.getValue() != null && StringUtils.isNotBlank(qualityCv.getValue().toString())) {
			quality = qualityCv.getControl().toDisplayValue(qualityCv.getValue());
		}

		ControlValue userCv = formData.getFieldValue("user");
		ControlValue timeCv = formData.getFieldValue("time");
		if (StringUtils.isBlank(quality) || quality.equals(SpecimenReceivedEvent.TO_BE_RECEIVED)) {
			if (userCv != null) {
				userCv.setValue(null);
			}

			if (timeCv != null) {
				timeCv.setValue(null);
			}
		} else {
			if (userCv != null) {
				Object user = userCv.getValue();
				if (user == null || StringUtils.isBlank(user.toString())) {
					userCv.setValue(AuthUtil.getCurrentUser().getId().toString());
				}
			}

			if (timeCv != null) {
				Object time = timeCv.getValue();
				if (time == null || StringUtils.isBlank(time.toString())) {
					timeCv.setValue(String.valueOf(Calendar.getInstance().getTimeInMillis()));
				}
			}
		}
	}

	private void notifyFormSave(Object object, FormContextBean ctxt, FormRecordEntryBean fre, FormData formData, boolean added) {
		if (!ctxt.isNotifEnabled() || ImporterContextHolder.getInstance().isImportOp() || ExporterContextHolder.getInstance().isExportOp()) {
			return;
		}

		Map<String, Object> props = new HashMap<>();
		props.put("user", AuthUtil.getCurrentUser());
		props.put("form", ctxt.getForm().getCaption());
		props.put("recordId", fre.getRecordId());
		props.put("added", added ? 1 : 0);
		if (object instanceof CollectionProtocolRegistration) {
			addEntityProps((CollectionProtocolRegistration) object, ctxt, fre, props);
		} else if (object instanceof Visit) {
			addEntityProps((Visit) object, ctxt, fre, props);
		} else if (object instanceof Specimen) {
			addEntityProps((Specimen) object, ctxt, fre, props);
		} else if (object instanceof User) {
			addEntityProps((User) object, ctxt, fre, props);
		} else {
			return;
		}

		props.put("$subject", new Object[] { props.get("entityType"), props.get("entityName"), props.get("form"), added ? 1 : 0} );
		List<User> dataRcpts = Collections.emptyList();
		if (ctxt.isDataInNotif()) {
			dataRcpts = filterByAccess(ctxt, object).stream()
				.filter(user -> !user.isDndEnabled() && user.isActive())
				.collect(Collectors.toList());
			if (CollectionUtils.isNotEmpty(dataRcpts)) {
				//
				// form data is refetched to ensure the field UI values are init'ed correctly.
				// PHI fields are always masked irrespective of the user access
				//
				formData = formDataMgr.getFormData(formData.getContainer(), formData.getRecordId());
				formData.maskPhiFieldValues();
				props.put("formData", formData.getFieldValueMap());
			}
		}

		List<User> linkRcpts = new ArrayList<>();
		for (UserGroup group : ctxt.getNotifUserGroups()) {
			for (User user : group.getUsers()) {
				if (!dataRcpts.contains(user) && !linkRcpts.contains(user) && !user.isDndEnabled() && user.isActive()) {
					linkRcpts.add(user);
				}
			}
		}

		for (User rcpt : dataRcpts) {
			props.put("rcpt", rcpt);
			EmailUtil.getInstance().sendEmail(FORM_SAVE_NOTIF_TMPL, new String[] { rcpt.getEmailAddress() }, null, props);
		}

		props.remove("formData");
		for (User rcpt : linkRcpts) {
			props.put("rcpt", rcpt);
			EmailUtil.getInstance().sendEmail(FORM_SAVE_NOTIF_TMPL, new String[] { rcpt.getEmailAddress() }, null, props);
		}
	}

	private void notifFormDeleted(Object object, FormRecordEntryBean fre) {
		FormContextBean ctxt = fre.getFormCtxt();
		if (!ctxt.isNotifEnabled() || ImporterContextHolder.getInstance().isImportOp()) {
			return;
		}

		Map<String, Object> props = new HashMap<>();
		props.put("user", AuthUtil.getCurrentUser());
		props.put("form", ctxt.getForm().getCaption());
		props.put("recordId", fre.getRecordId());
		if (object instanceof CollectionProtocolRegistration) {
			addEntityProps((CollectionProtocolRegistration) object, ctxt, fre, props);
		} else if (object instanceof Visit) {
			addEntityProps((Visit) object, ctxt, fre, props);
		} else if (object instanceof Specimen) {
			addEntityProps((Specimen) object, ctxt, fre, props);
		} else if (object instanceof User) {
			addEntityProps((User) object, ctxt, fre, props);
		} else {
			return;
		}

		props.put("$subject", new Object[] { props.get("entityType"), props.get("entityName"), props.get("form") });
		Set<User> sent = new HashSet<>();
		for (UserGroup group : ctxt.getNotifUserGroups()) {
			for (User user : group.getUsers()) {
				if (!sent.contains(user) && !user.isDndEnabled() && user.isActive()) {
					props.put("rcpt", user);
					EmailUtil.getInstance().sendEmail(FORM_DELETE_NOTIF_TMPL, new String[] { user.getEmailAddress() }, null, props);
					sent.add(user);
				}
			}
		}
	}

	private void addEntityProps(CollectionProtocolRegistration cpr, FormContextBean ctxt, FormRecordEntryBean fre, Map<String, Object> props) {
		props.put("entityType", MessageUtil.getInstance().getMessage("form_entity_Participant"));
		props.put("entityName", cpr.getPpid());
		props.put("cp", cpr.getCpShortTitle());
		props.put("url", getCprRecordUrl(cpr, ctxt, fre));
	}

	private void addEntityProps(Visit visit, FormContextBean ctxt, FormRecordEntryBean fre, Map<String, Object> props) {
		String title = visit.getName();
		if (visit.getCpEvent() != null) {
			title += " (" + visit.getCpEvent().getEventLabel() + ")";
		}

		props.put("entityType", MessageUtil.getInstance().getMessage("form_entity_SpecimenCollectionGroup"));
		props.put("entityName", title);
		props.put("cp", visit.getCollectionProtocol().getShortTitle());
		props.put("url", getVisitRecordUrl(visit, ctxt, fre));
	}

	private void addEntityProps(Specimen specimen, FormContextBean ctxt, FormRecordEntryBean fre, Map<String, Object> props) {
		String title = specimen.getLabel();
		if (StringUtils.isNotBlank(specimen.getBarcode())) {
			title += " (" + specimen.getBarcode() + ")";
		}

		props.put("entityType", MessageUtil.getInstance().getMessage("form_entity_Specimen"));
		props.put("entityName", title);
		props.put("cp", specimen.getCpShortTitle());
		props.put("url", getSpecimenRecordUrl(specimen, ctxt, fre));
	}

	private void addEntityProps(User user, FormContextBean ctxt, FormRecordEntryBean fre, Map<String, Object> props) {
		props.put("entityType", MessageUtil.getInstance().getMessage("form_entity_User"));
		props.put("entityName", user.formattedName());
		props.put("url", getUserRecordUrl(user, ctxt, fre));
	}

	private List<User> filterByAccess(FormContextBean ctxt, Object object) {
		List<User> result = new ArrayList<>();
		Set<User> seen = new HashSet<>();

		String accessType = getCuratedEntityType(ctxt.getEntityType());
		FormAccessChecker checker = formAccessCheckers.get(accessType);
		if (checker == null) {
			return Collections.emptyList();
		}

		User currentUser = AuthUtil.getCurrentUser();
		try {
			for (UserGroup ug : ctxt.getNotifUserGroups()) {
				for (User user : ug.getUsers()) {
					if (seen.contains(user)) {
						continue;
					}

					try {
						seen.add(user);
						AuthUtil.setCurrentUser(currentUser);
						if (checker.isDataReadAllowed(object)) {
							result.add(user);
						}
					} catch (Exception e) {
						// the set user has no access
					}
				}
			}
		} finally {
			AuthUtil.setCurrentUser(currentUser);
		}

		return result;
	}

	private String getCprRecordUrl(CollectionProtocolRegistration cpr, FormContextBean fc, FormRecordEntryBean fre) {
		String url = "#/cp-view/%d/participants/%d/detail/extensions/list?formId=%d&formCtxtId=%d&recordId=%d";
		return getUrl(String.format(url, cpr.getCollectionProtocol().getId(), cpr.getId(), fc.getContainerId(), fc.getIdentifier(), fre.getRecordId()));
	}

	private String getVisitRecordUrl(Visit visit, FormContextBean fc, FormRecordEntryBean fre) {
		String url = "#/cp-view/%d/participants/%d/visits/detail/extensions/list?visitId=%d&formId=%d&formCtxtId=%d&recordId=%d";
		return getUrl(String.format(url, visit.getCollectionProtocol().getId(), visit.getRegistration().getId(), visit.getId(), fc.getContainerId(), fc.getIdentifier(), fre.getRecordId()));
	}

	private String getSpecimenRecordUrl(Specimen specimen, FormContextBean fc, FormRecordEntryBean fre) {
		if ("SpecimenEvent".equals(fc.getEntityType())) {
			return getEventRecordUrl(specimen, fc, fre);
		}

		String url = "#/cp-view/%d/participants/%d/visits/specimens/detail/extensions/list?visitId=%d&specimenId=%d&formId=%d&formCtxtId=%d&recordId=%d";
		return getUrl(String.format(url, specimen.getCollectionProtocol().getId(), specimen.getRegistration().getId(), specimen.getVisit().getId(), specimen.getId(), fc.getContainerId(), fc.getIdentifier(), fre.getRecordId()));
	}

	private String getEventRecordUrl(Specimen specimen, FormContextBean fc, FormRecordEntryBean fre) {
		String url = "#/cp-view/%d/participants/%d/visits/specimens/detail/event-overview?visitId=%d&specimenId=%d&formId=%d&recordId=%d";
		return getUrl(String.format(url, specimen.getCollectionProtocol().getId(), specimen.getRegistration().getId(), specimen.getVisit().getId(), specimen.getId(), fc.getContainerId(), fre.getRecordId()));
	}

	private String getUserRecordUrl(User user, FormContextBean fc, FormRecordEntryBean fre) {
		String url = "ui-app/#/users/%d/forms/list?formId=%d&formCtxtId=%d&recordId=%d";
		return getUrl(String.format(url, user.getId(), fc.getContainerId(), fc.getIdentifier(), fre.getRecordId()));
	}

	private String getUrl(String path) {
		String appUrl = ConfigUtil.getInstance().getAppUrl();
		if (!appUrl.endsWith("/")) {
			appUrl += "/";
		}

		return appUrl + path;
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

		String accessType = getCuratedEntityType(entityType);
		FormAccessChecker accessChecker = formAccessCheckers.get(accessType);
		if (accessChecker == null || !accessChecker.isDataReadAllowed(accessType, objectId)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
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
			appData.put("formStatus",  record.getFormStatus());
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

	private Specimen getSpecimen(Long objectId) {
		Specimen specimen = daoFactory.getSpecimenDao().getById(objectId);
		if (specimen == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, objectId);
		} else if (specimen.isReserved()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.RESV_EDIT_NOT_ALLOWED, specimen.getLabel());
		}

		return specimen;
	}

	private String getCuratedEntityType(String entityType) {
		String accessType = entityType;
		int hyphenIdx = entityType.indexOf("-");
		if (hyphenIdx >= 0) {
			accessType = entityType.substring(0, hyphenIdx);
		}
		return accessType;
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

		int count = switch (entity) {
			case "Participant", "ParticipantExtension" ->
				formDao.moveRegistrationRecords(cpId, srcFc.getIdentifier(), tgtFc.getIdentifier());
			case "SpecimenCollectionGroup", "VisitExtension" ->
				formDao.moveVisitRecords(cpId, srcFc.getIdentifier(), tgtFc.getIdentifier());
			case "Specimen", "SpecimenExtension" ->
				formDao.moveSpecimenRecords(cpId, srcFc.getIdentifier(), tgtFc.getIdentifier());
			default -> 0;
		};

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

	private void ensureUpdateAllowed(FormContextBean formCtxt) {
		if (formCtxt.getDeletedOn() != null) {
			// ignore deleted associations
			return;
		}

		String accessType = getCuratedEntityType(formCtxt.getEntityType());
		FormAccessChecker checker = formAccessCheckers.get(accessType);
		if (checker == null || !checker.isUpdateAllowed(formCtxt)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private void ensureUpdateAllowed(FormContextDetail detail) {
		FormContextBean ctxt = new FormContextBean();
		ctxt.setContainerId(detail.getFormId());
		ctxt.setCpId(detail.getCollectionProtocol() != null ? detail.getCollectionProtocol().getId() : -1L);
		ctxt.setEntityType(detail.getLevel());
		ctxt.setEntityId(detail.getEntityId());
		ctxt.setSysForm(detail.isSysForm());
		ctxt.setMultiRecord(detail.isMultiRecord());
		ctxt.setSysForm(detail.isSysForm());
		ensureUpdateAllowed(ctxt);
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
						Map<String, Object> appData = (Map<String, Object>) valueMap.get("appData");

						Map<String, Object> formData = new HashMap<>();
						formData.put("recordId", valueMap.get("id"));
						formData.put("cpShortTitle", cpr.getCollectionProtocol().getShortTitle());
						formData.put("ppid", cpr.getPpid());
						formData.put("fdeStatus", appData != null ? appData.get("formStatus") : "N/A");
						formData.put("activityStatus", "ACTIVE");
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

					Long recordId = (Long) record.get("recordId");
					String status = (String) record.get("formStatus");
					result.add(toFormRec.apply(Pair.make(lastObj, getFormData(recordId, status, !lastObjPhiAllowed))));
				}

				return result;
			}

			private Map<String, Object> getFormData(Long recordId, String formStatus, boolean maskPhi) {
				FormData formData = formDataMgr.getFormData(form, recordId);

				if (formHasPhi && maskPhi) {
					formData.maskPhiFieldValues();
				}

				Map<String, Object> appData = formData.getAppData();
				if (appData == null) {
					appData = new HashMap<>();
					formData.setAppData(appData);
				}

				appData.put("formStatus", formStatus);
				return formData.getFieldNameValueMap(true, true);
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

	//
	// form checkers
	//
	private static final FormAccessChecker SYS_FORMS_CHECKER = new SysFormAccessChecker();

	private static final FormAccessChecker CP_FORMS_CHECKER = new CpFormAccessChecker();

	private static final FormAccessChecker USER_FORM_CHECKER =  new UserFormAccessChecker();

	private static final String FORM_SAVE_NOTIF_TMPL = "form_data_save_notif";

	private static final String FORM_DELETE_NOTIF_TMPL = "form_data_delete_notif";
}
