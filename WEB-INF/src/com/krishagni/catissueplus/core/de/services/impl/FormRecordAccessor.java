package com.krishagni.catissueplus.core.de.services.impl;

import java.util.HashMap;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import krishagni.catissueplus.beans.FormContextBean;
import krishagni.catissueplus.beans.FormRecordEntryBean;

public class FormRecordAccessor implements ObjectAccessor {

	private DaoFactory daoFactory;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	@Override
	public String getObjectName() {
		return "formRecord";
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (!key.equals("recordId")) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Unknown formRecord key: " + key);
		}

		Long recordId = Long.parseLong(value.toString());
		FormRecordEntryBean re = deDaoFactory.getFormDao().getRecordEntry(recordId);
		if (re == null) {
			throw OpenSpecimenException.userError(FormErrorCode.REC_NOT_FOUND, recordId);
		}

		FormContextBean fc = re.getFormCtxt();

		Map<String, Object> result = new HashMap<>();
		result.put("formId", fc.getContainerId());
		result.put("formCtxtId", fc.getIdentifier());
		result.put("recordId", re.getRecordId());


		switch (fc.getEntityType()) {
			case "Participant":
				CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(re.getObjectId());
				AccessCtrlMgr.getInstance().ensureReadCprRights(cpr);

				result.put("stateName", "ParticipantsListItemDetail.Forms");
				result.put("cprId", cpr.getId());
				result.put("cpId", cpr.getCollectionProtocol().getId());
				break;

			case "CommonParticipant":
				Participant participant = daoFactory.getParticipantDao().getById(re.getObjectId());
				CollectionProtocolRegistration reg = null;
				for (CollectionProtocolRegistration r : participant.getCprs()) {
					try {
						AccessCtrlMgr.getInstance().ensureReadCprRights(r);
						reg = r;
						break;
					} catch (OpenSpecimenException ose) {
						if (ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
							continue;
						}

						throw ose;
					}
				}

				if (reg == null) {
					throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
				}

				result.put("stateName", "ParticipantsListItemDetail.Forms");
				result.put("cprId", reg.getId());
				result.put("cpId", reg.getCollectionProtocol().getId());
				break;

			case "SpecimenCollectionGroup":
				Visit visit = daoFactory.getVisitsDao().getById(re.getObjectId());
				AccessCtrlMgr.getInstance().ensureReadVisitRights(visit);

				result.put("stateName", "ParticipantsListItemVisitDetail.Forms");
				result.put("visitId", visit.getId());
				result.put("cprId", visit.getRegistration().getId());
				result.put("cpId", visit.getRegistration().getCollectionProtocol().getId());
				break;

			case "Specimen":
			case "SpecimenEvent":
				Specimen spmn = daoFactory.getSpecimenDao().getById(re.getObjectId());
				AccessCtrlMgr.getInstance().ensureReadSpecimenRights(spmn);

				if (fc.getEntityType().equals("Specimen")) {
					result.put("stateName", "ParticipantsListItemSpecimenDetail.Forms");
				} else {
					result.put("stateName", "ParticipantsListItemSpecimenDetail.Overview");
				}

				result.put("specimenId", spmn.getId());
				result.put("visitId", spmn.getVisit().getId());
				result.put("cprId", spmn.getRegistration().getId());
				result.put("cpId", spmn.getCollectionProtocol().getId());
				break;
		}

		return result;
	}

	@Override
	public String getAuditTable() {
		return null;
	}

	@Override
	public void ensureReadAllowed(Long objectId) {

	}
}
