package com.krishagni.catissueplus.core.de.services.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.de.repository.FormDao;

@Configurable
public class FormUtil {
	private static FormUtil instance = null;

	@Autowired
	private FormDao formDao;

	public static FormUtil getInstance() {
		if (instance == null || instance.formDao == null) {
			instance = new FormUtil();
		}

		return instance;
	}

	public int deleteCpEntityForms(Long cpId) {
		return deleteCpEntityForms(
			cpId,
			Arrays.asList(
				"Participant", "ParticipantExtension",
				"SpecimenCollectionGroup", "VisitExtension",
				"Specimen", "SpecimenExtension", "SpecimenEvent")
		);
	}

	public int deleteCpEntityForms(Long cpId, List<String> entityTypes) {
		return formDao.deleteFormContexts(cpId, entityTypes);
	}

	public int deleteRecords(Long cpId, List<String> entityTypes, Long objectId) {
		return formDao.deleteRecords(cpId, entityTypes, objectId);
	}

	public int undeleteRecords(Long cpId, List<String> entityTypes, Long objectId) {
		return formDao.undeleteRecords(cpId, entityTypes, objectId);
	}
}
