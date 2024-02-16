package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.services.Anonymizer;
import com.krishagni.catissueplus.core.de.events.FormRecordSummary;
import com.krishagni.catissueplus.core.de.repository.FormDao;
import com.krishagni.catissueplus.core.de.services.FormService;

import edu.common.dynamicextensions.domain.nui.Container;

public class ParticipantAnonymizer implements Anonymizer<CollectionProtocolRegistration> {
	private FormDao formDao;

	private FormService formSvc;

	public void setFormDao(FormDao formDao) {
		this.formDao = formDao;
	}

	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	@Override
	public void anonymize(CollectionProtocolRegistration cpr) {
		anonymize(cpr, cpr.getParticipant());
		cpr.getVisits().forEach(this::anonymize);

		if (StringUtils.isNotBlank(cpr.getSignedConsentDocumentName())) {
			File file = new File(ConfigParams.getConsentsDirPath() + File.separator + cpr.getSignedConsentDocumentName());
			file.delete();
			cpr.setSignedConsentDocumentName(null);
		}

		anonymizeFormRecords(cpr.getId(), "Participant");
	}

	private void anonymize(CollectionProtocolRegistration cpr, Participant p) {
		p.setFirstName(null);
		p.setLastName(null);
		p.setMiddleName(null);
		p.setBirthDate(null);
		p.setDeathDate(null);
		p.setEmpi(null);
		p.setUid(null);
		p.setEmailAddress(null);
		p.setEmailOptIn(null);
		p.setPhoneNumber(null);
		p.setTextOptIn(null);

		p.getPmis().forEach(pmi -> pmi.setMedicalRecordNumber(null));
		if (cpr.getExtension() != null) {
			cpr.getExtension().anonymize();
		}
	}

	private void anonymize(Visit v) {
		v.setSurgicalPathologyNumber(null);
		if (v.getExtension() != null) {
			v.getExtension().anonymize();
		}
		v.getSpecimens().forEach(this::anonymize);
		anonymizeFormRecords(v.getId(), "SpecimenCollectionGroup");
	}

	private void anonymize(Specimen s) {
		if (s.getExtension() != null) {
			s.getExtension().anonymize();
		}
		anonymizeFormRecords(s.getId(), "Specimen");
	}


	private void anonymizeFormRecords(Long objectId, String entityType) {
		Map<Long, List<FormRecordSummary>> recsMap = formDao.getFormRecords(objectId, entityType, null);
		recsMap.forEach((formId, formRecs) -> {
			Container form = Container.getContainer(formId);
			if (form.hasPhiFields()) {
				formRecs.forEach(formRec -> anonymizeFormRecord(form, formRec.getRecordId()));
			}
		});
	}

	private void anonymizeFormRecord(Container form, Long recordId) {
		formSvc.anonymizeRecord(form, recordId);
	}
}
