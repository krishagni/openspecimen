package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentDetail;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class ConsentImporter implements ObjectImporter<ConsentDetail, ConsentDetail> {
	
	private CollectionProtocolRegistrationService cprSvc;
	
	public void setCprSvc(CollectionProtocolRegistrationService cprSvc) {
		this.cprSvc = cprSvc;
	}

	@Override
	public ResponseEvent<ConsentDetail> importObject(RequestEvent<ImportObjectDetail<ConsentDetail>> req) {
		try {
			ImportObjectDetail<ConsentDetail> importDetail = req.getPayload();
			ConsentDetail consentDetail = importDetail.getObject();

			uploadConsentFile(importDetail.getUploadedFilesDir(), consentDetail);
			return cprSvc.saveConsents(new RequestEvent<>(consentDetail));
		} catch (OpenSpecimenException e) {
			return ResponseEvent.error(e);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void uploadConsentFile(String uploadedFilesDir, ConsentDetail consentDetail) {
		String filename = consentDetail.getConsentDocumentName();
		if (StringUtils.isBlank(filename)) {
			return;
		}

		FileInputStream in = null;
		try {
			in = new FileInputStream(uploadedFilesDir + File.separator + filename);
			
			Map<String, Object> objectProps = new HashMap<>();
			objectProps.put("cpShortTitle", consentDetail.getCpShortTitle());
			objectProps.put("ppid",         consentDetail.getPpid());
			objectProps.put("empi",         consentDetail.getEmpi());

			
			FileDetail fileDetail = new FileDetail();
			fileDetail.setFileIn(in);
			fileDetail.setFilename(filename);
			fileDetail.setObjectProps(objectProps);

			ResponseEvent<String> resp = cprSvc.uploadConsentForm(new RequestEvent<>(fileDetail));
			resp.throwErrorIfUnsuccessful();
		} catch (FileNotFoundException fnfe) {
			throw OpenSpecimenException.userError(CprErrorCode.CONSENT_FORM_NOT_FOUND, filename);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}
}
