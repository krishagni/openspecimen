
package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface ParticipantDao extends Dao<Participant> {
	
	Participant getByUid(String uid);
	
	Participant getByEmpi(String empi);

	List<Participant> getByLastNameAndBirthDate(String lname, Date dob);
	
	List<Participant> getByPmis(List<PmiDetail> pmis);
	
	List<Long> getParticipantIdsByPmis(List<PmiDetail> pmis);

	boolean isUidUnique(String uid);

	boolean isPmiUnique(String siteName, String mrn);

	List<Participant> getByPhoneNumber(String phoneNumber);
}
