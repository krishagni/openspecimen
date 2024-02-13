
package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.ParticipantMedicalIdentifier;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.ParticipantDao;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.common.repository.SubQuery;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

public class ParticipantDaoImpl extends AbstractDao<Participant> implements ParticipantDao {
	
	@Override
	public Participant getByUid(String uid) {
		Criteria<Participant> query = createCriteria(Participant.class, "p");
		if (isMySQL()) {
			query.add(query.eq("p.uid", uid));
		} else {
			query.add(query.eq(query.lower("p.uid"), uid.toLowerCase()));
		}

		return query.uniqueResult();
	}
	
	@Override
	public Participant getByEmpi(String empi) {
		Criteria<Participant> query = createCriteria(Participant.class, "p");
		if (isMySQL()) {
			query.add(query.eq("p.empi", empi));
		} else {
			query.add(query.eq(query.lower("p.empi"), empi.toLowerCase()));
		}

		return query.uniqueResult();
	}

	@Override
	public List<Participant> getByLastNameAndBirthDate(String lname, Date dob) {
		ZonedDateTime zdt = ZonedDateTime.ofInstant(new Date(dob.getTime()).toInstant(), ZoneId.systemDefault());
		Date dobStart     = Date.from(zdt.with(LocalTime.MIN).toInstant());
		Date dobEnd       = Date.from(zdt.with(LocalTime.MAX).toInstant());

		Criteria<Participant> query = createCriteria(Participant.class, "p");
		if (isMySQL()) {
			query.add(query.eq("p.lastName", lname));
		} else {
			query.add(query.eq(query.lower("p.lastName"), lname.toLowerCase()));
		}

		return query.add(query.between("p.birthDate", dobStart, dobEnd)).list();
	}

	@Override
	public List<Participant> getByPmis(List<PmiDetail> pmis) {
		Criteria<Participant> query = createCriteria(Participant.class, "p");
		SubQuery<Long> subQuery = getByPmisQuery(pmis, query);
		if (subQuery == null) {
			return Collections.emptyList();
		}

		return query.add(query.in("p.id", subQuery)).list();
	}
	
	@Override
	public List<Long> getParticipantIdsByPmis(List<PmiDetail> pmis) {
		Criteria<Long> query = createCriteria(Participant.class, Long.class, "p");
		SubQuery<Long> subQuery = getByPmisQuery(pmis, query);
		if (subQuery == null) {
			return Collections.emptyList();
		}

		return query.select("p.id").add(query.in("p.id", subQuery)).list();
	}
	
	@Override
	public boolean isUidUnique(String uid) {
		Criteria<Participant> query = createCriteria(Participant.class, "p");
		if (isMySQL()) {
			query.add(query.eq("p.uid", uid));
		} else {
			query.add(query.eq(query.lower("p.uid"), uid.toLowerCase()));
		}

		return query.getCount("p.id") == 0L;
	}

	@Override
	public boolean isPmiUnique(String siteName, String mrn) {
		Criteria<ParticipantMedicalIdentifier> query = createCriteria(ParticipantMedicalIdentifier.class, "pmi")
			.join("pmi.site", "site");

		if (isMySQL()) {
			query.add(query.eq("site.name", siteName))
				.add(query.eq("pmi.medicalRecordNumber", mrn));
		} else {
			query.add(query.eq(query.lower("site.name"), siteName.toLowerCase()))
				.add(query.eq(query.lower("pmi.medicalRecordNumber"), mrn.toLowerCase()));
		}

		return query.getCount("pmi.id") == 0L;
	}

	@Override
	public List<Participant> getByPhoneNumber(String phoneNumber) {
		Criteria<Participant> query = createCriteria(Participant.class, "participant");

		String defIsdCode = ConfigUtil.getInstance().getStrSetting("common", "default_isd_code", "+1");
		List<String> phoneNumbers = new ArrayList<>();
		phoneNumbers.add(phoneNumber);
		if (phoneNumber.startsWith("+")) {
			if (phoneNumber.startsWith(defIsdCode)) {
				phoneNumber = phoneNumber.substring(defIsdCode.length());
				phoneNumbers.add(phoneNumber);
			}
		} else {
			phoneNumbers.add(defIsdCode + phoneNumber);
		}

		return query.add(query.in("participant.phoneNumber", phoneNumbers)).list();
	}

	@Override
	public Class<Participant> getType() {
		return Participant.class;
	}
	
	private SubQuery<Long> getByPmisQuery(List<PmiDetail> pmis, AbstractCriteria<?, ?> mainQuery) {
		SubQuery<Long> query = mainQuery.createSubQuery(Participant.class, "p")
			.distinct().select("p.id")
			.join("p.pmis", "pmi")
			.join("pmi.site", "site");

		Disjunction junction = query.disjunction();
		boolean added = false;
		for (PmiDetail pmi : pmis) {
			if (StringUtils.isBlank(pmi.getSiteName()) || StringUtils.isBlank(pmi.getMrn())) {
				continue;
			}

			if (isMySQL()) {
				junction.add(query.and(
					query.eq("pmi.medicalRecordNumber", pmi.getMrn()),
					query.eq("site.name", pmi.getSiteName())
				));
			} else {
				junction.add(query.and(
					query.eq(query.lower("pmi.medicalRecordNumber"), pmi.getMrn().toLowerCase()),
					query.eq(query.lower("site.name"), pmi.getSiteName().toLowerCase())
				));
			}

			added = true;
		}

		return added ? query.add(junction) : null;
	}
}
