package com.krishagni.catissueplus.core.de.services.impl;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.de.services.FormAccessChecker;

import krishagni.catissueplus.beans.FormContextBean;

public class CpFormAccessChecker implements FormAccessChecker {
	@Override
	public boolean isUpdateAllowed(FormContextBean formCtxt) {
		Long cpId = formCtxt.getCpId();
		return ((cpId == null || cpId == -1L) && AuthUtil.isAdmin()) ||
			(cpId != null && cpId != -1L && AccessCtrlMgr.getInstance().hasUpdateCpRights(cpId));
	}

	@Override
	public boolean isDataReadAllowed(Object obj) {
		boolean allowed = true;
		try {
			if (obj instanceof CollectionProtocolRegistration) {
				AccessCtrlMgr.getInstance().ensureReadCprRights((CollectionProtocolRegistration) obj);
			} else if (obj instanceof Visit) {
				AccessCtrlMgr.getInstance().ensureReadVisitRights((Visit) obj);
			} else if (obj instanceof Specimen) {
				AccessCtrlMgr.getInstance().ensureReadSpecimenRights((Specimen) obj);
			} else if (obj instanceof Participant) {
				AccessCtrlMgr.getInstance().ensureReadParticipantRights((Participant) obj);
			} else {
				allowed = false;
			}
		} catch (Exception e) {
			allowed = false;
		}

		return allowed;
	}

	@Override
	public boolean isDataReadAllowed(String entityType, Long objectId) {
		boolean allowed = true;

		try {
			switch (entityType) {
				case "Participant" ->
					AccessCtrlMgr.getInstance().ensureReadCprRights(objectId);

				case "ParticipantExtension" ->
					AccessCtrlMgr.getInstance().ensureReadParticipantRights(objectId);

				case "VisitExtension", "SpecimenCollectionGroup" ->
					AccessCtrlMgr.getInstance().ensureReadVisitRights(objectId);

				case "SpecimenExtension", "Specimen" ->
					AccessCtrlMgr.getInstance().ensureReadSpecimenRights(objectId);

				default ->
					allowed = false;
			}
		} catch (Exception e) {
			allowed = false;
		}

		return allowed;
	}

	@Override
	public boolean isDataUpdateAllowed(Object obj) {
		boolean allowed = true;
		try {
			if (obj instanceof CollectionProtocolRegistration) {
				AccessCtrlMgr.getInstance().ensureUpdateCprRights((CollectionProtocolRegistration) obj);
			} else if (obj instanceof Visit) {
				AccessCtrlMgr.getInstance().ensureCreateOrUpdateVisitRights((Visit) obj);
			} else if (obj instanceof Specimen) {
				AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights((Specimen) obj);
			} else {
				allowed = false;
			}
		} catch (Exception e) {
			allowed = false;
		}

		return allowed;
	}

	@Override
	public boolean isDataUpdateAllowed(String entityType, Long objectId) {
		boolean allowed = true;
		try {
			switch (entityType) {
				case "Participant" ->
					AccessCtrlMgr.getInstance().ensureUpdateCprRights(objectId);

				case "ParticipantExtension" ->
					AccessCtrlMgr.getInstance().ensureUpdateParticipantRights(objectId);

				case "VisitExtension", "SpecimenCollectionGroup" ->
					AccessCtrlMgr.getInstance().ensureCreateOrUpdateVisitRights(objectId, false);

				case "SpecimenExtension", "Specimen" ->
					AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(objectId, false);

				default ->
					allowed = false;
			}
		} catch (Exception e) {
			allowed = false;
		}

		return allowed;
	}
}
