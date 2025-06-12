package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.Collections;
import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenStatusDetail;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class SpecimenDisposalImporter implements ObjectImporter<SpecimenStatusDetail, SpecimenDetail> {

	private SpecimenService specimenSvc;

	public void setSpecimenSvc(SpecimenService specimenSvc) {
		this.specimenSvc = specimenSvc;
	}

	@Override
	public ResponseEvent<SpecimenDetail> importObject(RequestEvent<ImportObjectDetail<SpecimenStatusDetail>> req) {
		try {
			ImportObjectDetail<SpecimenStatusDetail> detail = req.getPayload();
			ensureValidStatus(detail.getObject());
			detail.getObject().setForceUpdate(true);

			List<SpecimenDetail> specimens = ResponseEvent.unwrap(specimenSvc.updateSpecimensStatus(RequestEvent.wrap(Collections.singletonList(detail.getObject()))));
			return ResponseEvent.response(specimens != null && !specimens.isEmpty() ? specimens.get(0) : null);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch	(Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void ensureValidStatus(SpecimenStatusDetail detail) {
		if (!Status.isClosedStatus(detail.getStatus()) && !Status.isDisabledStatus(detail.getStatus())) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.INVALID_DISPOSE_STATUS, detail.getStatus());
		}
	}
}
