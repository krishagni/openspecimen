
package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.ParticipantMedicalIdentifier;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantLookupFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantUtil;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;

import static com.krishagni.catissueplus.core.common.PvAttributes.ETHNICITY;
import static com.krishagni.catissueplus.core.common.PvAttributes.GENDER;
import static com.krishagni.catissueplus.core.common.PvAttributes.RACE;
import static com.krishagni.catissueplus.core.common.PvAttributes.VITAL_STATUS;

public class ParticipantFactoryImpl implements ParticipantFactory, InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(ParticipantFactoryImpl.class);

	private static final String DEAD_STATUS = "Dead";

	private Set<Site> externalSourceSites;

	private TransactionTemplate newTxTmpl;

	private DaoFactory daoFactory;

	private ParticipantLookupFactory lookupFactory;

	private ConfigurationService cfgSvc;

	public void setTransactionManager(PlatformTransactionManager txnMgr) {
		this.newTxTmpl = new TransactionTemplate(txnMgr);
		this.newTxTmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setLookupFactory(ParticipantLookupFactory lookupFactory) {
		this.lookupFactory = lookupFactory;
	}

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	@Override
	public Participant createParticipant(ParticipantDetail input) {
		Participant participant = new Participant();
		Long matchedId = copyMatchedParticipantFields(null, input);

		boolean partial = false;
		if (matchedId != null && matchedId > 0) {
			Participant match = daoFactory.getParticipantDao().getById(matchedId);
			BeanUtils.copyProperties(match, participant, "cprs", "source");
			participant.setId(matchedId);
			partial = true;
		}
		
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		setParticipantAttrs(input, participant, partial, ose);
		ose.checkAndThrow();
		return participant;
	}
	
	@Override
	public Participant createParticipant(Participant existing, ParticipantDetail input) {
		Long matchedId = copyMatchedParticipantFields(existing, input);

		Participant participant = new Participant();
		BeanUtils.copyProperties(existing, participant, "cprs", "source");
		if (matchedId != null) {
			participant.setId(matchedId);
		}

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		setParticipantAttrs(input, participant, true, ose);

		ose.checkAndThrow();
		return participant;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		cfgSvc.registerChangeListener(
			ConfigParams.MODULE,
			(name, value) -> {
				if (StringUtils.isBlank(name) || ConfigParams.EXT_PARTICIPANT_SITES.equals(name)) {
					externalSourceSites = null;
				}
			}
		);
	}

	private Long copyMatchedParticipantFields(Participant existing, ParticipantDetail input) {
		ParticipantDetail match = lookup(input);
		if (match == null) {
			return null;
		}

		input.setId(match.getId());
		match.setCpId(input.getCpId());
		if (!StringUtils.equalsIgnoreCase(input.getSource(), match.getSource())) {
			Participant p = existing != null ? existing : new Participant();
			copyLockedFields(match, input);
		}

		return match.getId();
	}

	private ParticipantDetail lookup(ParticipantDetail input) {
		return newTxTmpl.execute(
			new TransactionCallback<ParticipantDetail>() {
				@Override
				public ParticipantDetail doInTransaction(TransactionStatus transactionStatus) {
					List<MatchedParticipant> matches = lookupFactory.getLookupLogic().getMatchingParticipants(input);
					matches = matches.stream().filter(
						match -> !(
							match.getMatchedAttrs().size() == 1 &&
							match.getMatchedAttrs().get(0).equals("lnameAndDob")
						)
					).collect(Collectors.toList());

					if (matches.size() > 1) {
						throw OpenSpecimenException.userError(ParticipantErrorCode.MULTI_MATCHES);
					}

					if (CollectionUtils.isNotEmpty(input.getPmis())) {
						ensureExtSourceMrnMatch(input, !matches.isEmpty() ? matches.get(0) : null);
					}

					if (matches.isEmpty()) {
						return null;
					}

					MatchedParticipant match = matches.get(0);
					if (Collections.disjoint(match.getMatchedAttrs(), Arrays.asList("empi", "uid", "pmi"))) {
						return null;
					}

					return match.getParticipant();
				}
			}
		);
	}

	private void copyLockedFields(ParticipantDetail match, ParticipantDetail input) {
		List<String> lockedFields = ParticipantUtil.getLockedFields(match.getSource());
		if (lockedFields.isEmpty()) {
			return;
		}

		if (match.getExtensionDetail() == null) {
			match.setExtensionDetail(new ExtensionDetail());
		}

		if (input.getExtensionDetail() == null) {
			input.setExtensionDetail(match.getExtensionDetail());
		}

		Map<String, Object> matchExtnAttrsMap = match.getExtensionDetail().getAttrsMap();
		Map<String, Object> inputExtnAttrsMap = input.getExtensionDetail().getAttrsMap();
		for (String field : lockedFields) {
			try {
				if (!field.startsWith("extensionDetail.attrsMap")) {
					PropertyUtils.setProperty(input, field, PropertyUtils.getProperty(match, field));
				} else {
					String extnAttr = field.substring("extensionDetail.attrsMap.".length());
					inputExtnAttrsMap.put(extnAttr, matchExtnAttrsMap.get(extnAttr));
				}
			} catch (Exception e) {
				logger.error("Error copying the field: " + field + ". Error: " + e.getMessage(), e);
				throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Invalid locked field name: " + field + ". Error: " + e.getMessage());
			}
		}

		if (!inputExtnAttrsMap.isEmpty()) {
			input.getExtensionDetail().setAttrsMap(inputExtnAttrsMap);
		}
	}

	private void ensureExtSourceMrnMatch(ParticipantDetail input, MatchedParticipant match) {
		Set<String> names = getExternalSourceSites().stream()
			.map(s -> s.getName().toLowerCase())
			.collect(Collectors.toSet());
		if (names.isEmpty()) {
			// no external sources configured
			return;
		}

		boolean hasExtSite = input.getPmis().stream()
			.anyMatch(pmi ->
				StringUtils.isNotBlank(pmi.getSiteName()) &&
				StringUtils.isNotBlank(pmi.getMrn()) &&
				names.contains(pmi.getSiteName().toLowerCase())
			);
		if (!hasExtSite) {
			// none of the input MRNs are of external sources
			return;
		}

		if (match == null || !match.getMatchedAttrs().contains("pmi")) {
			throw OpenSpecimenException.userError(ParticipantErrorCode.NO_MRN_MATCH, PmiDetail.toString(input.getPmis()));
		}

		Map<String, String> mrns = PmiDetail.toMap(match.getParticipant().getPmis());
		boolean hasMatch = input.getPmis().stream()
			.anyMatch(pmi ->
				StringUtils.isNotBlank(pmi.getSiteName()) &&
				names.contains(pmi.getSiteName().toLowerCase()) &&
				StringUtils.isNotBlank(pmi.getMrn()) &&
				pmi.getMrn().toLowerCase().equals(mrns.get(pmi.getSiteName().toLowerCase()))
			);

		if (!hasMatch) {
			throw OpenSpecimenException.userError(ParticipantErrorCode.NO_MRN_MATCH, PmiDetail.toString(input.getPmis()));
		}
	}

	private Set<Site> getExternalSourceSites() {
		if (externalSourceSites != null) {
			return externalSourceSites;
		}

		String sitesStr = ConfigUtil.getInstance().getStrSetting(ConfigParams.MODULE, ConfigParams.EXT_PARTICIPANT_SITES, "");
		List<String> sitesList = Utility.csvToStringList(sitesStr);

		externalSourceSites = new HashSet<>();
		for (String inputSite : sitesList) {
			Site site = null;
			if (StringUtils.isNumeric(inputSite)) {
				site = daoFactory.getSiteDao().getById(Long.parseLong(inputSite));
			} else {
				site = daoFactory.getSiteDao().getSiteByName(inputSite);
			}

			if (site == null) {
				logger.error("External source site not found. Key = " + inputSite);
				throw OpenSpecimenException.userError(SiteErrorCode.NOT_FOUND, inputSite);
			}

			externalSourceSites.add(site);
		}

		return externalSourceSites;
	}
	
	private void setParticipantAttrs(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException ose) {
		if (participant.getId() == null && detail.getId() != null && detail.getId() != -1L) {
			participant.setId(detail.getId());
		}

		setSource(detail, participant, partial, ose);
		setUid(detail, participant, partial, ose);
		setEmpi(detail, participant, partial, ose);
		setName(detail, participant, partial, ose);
		setEmailAddress(detail, participant, partial, ose);
		setPhoneNumber(detail, participant, partial, ose);
		setVitalStatus(detail, participant, partial, ose);
		setBirthAndDeathDate(detail, participant, partial, ose);
		setActivityStatus(detail, participant, partial, ose);
		setGender(detail, participant, partial, ose);
		setRace(detail, participant, partial, ose);
		setEthnicity(detail, participant, partial, ose);
		setPmi(detail, participant, partial, ose);
	}

	private void setSource(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException ose) {
		if (partial && !detail.isAttrModified("source")) {
			return;
		}

		participant.setSource(detail.getSource());
	}

	private void setUid(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (partial && !detail.isAttrModified("uid")) {
			return;
		}
		
		String uid = detail.getUid();
		if (StringUtils.isBlank(uid)) {
			if (ConfigUtil.getInstance().getBoolSetting("biospecimen", "uid_mandatory", false)) {
				oce.addError(ParticipantErrorCode.UID_REQUIRED);
			} else {
				participant.setUid(null);
			}
			return;
		}
		
		if (!ParticipantUtil.isValidUid(uid, oce)) {
			return;
		}
		
		if (partial && !uid.equals(participant.getUid())) {
			ParticipantUtil.ensureUniqueUid(daoFactory, uid, oce);
		}
		
		participant.setUid(uid);
	}
	
	private void setEmpi(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException ose) {
		if (partial && !detail.isAttrModified("empi")) {
			return;
		}
		
		String empi = detail.getEmpi();
		if (StringUtils.isBlank(empi)) {
			participant.setEmpi(null);
			return;
		}

		if (!ParticipantUtil.isValidMpi(empi, ose)) {
			return;
		}
		
		if (partial && !empi.equals(participant.getEmpi())) {
			//ParticipantUtil.en
		}
		
		participant.setEmpi(empi);
	}
	
	private void setName(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException ose) {
		if (!partial || detail.isAttrModified("firstName")) {
			participant.setFirstName(detail.getFirstName());
		}
		
		if (!partial || detail.isAttrModified("middleName")) {
			participant.setMiddleName(detail.getMiddleName());
		}
		
		if (!partial || detail.isAttrModified("lastName")) {
			participant.setLastName(detail.getLastName());
		}		
	}

	private void setEmailAddress(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException ose) {
		if (!partial || detail.isAttrModified("emailAddress")) {
			if (StringUtils.isNotBlank(detail.getEmailAddress()) && !Utility.isValidEmail(detail.getEmailAddress())) {
				ose.addError(ParticipantErrorCode.INVALID_EMAIL_ID, detail.getEmailAddress());
			}

			participant.setEmailAddress(detail.getEmailAddress());
		}

		if (StringUtils.isBlank(participant.getEmailAddress())) {
			participant.setEmailOptIn(false);
		} else if (!partial || detail.isAttrModified("emailOptIn")) {
			participant.setEmailOptIn(detail.getEmailOptIn());
		}
	}

	private void setPhoneNumber(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException ose) {
		if (!partial || detail.isAttrModified("phoneNumber")) {
			participant.setPhoneNumber(detail.getPhoneNumber());
		}

		if (StringUtils.isBlank(participant.getPhoneNumber())) {
			participant.setTextOptIn(false);
		} else if (!partial || detail.isAttrModified("textOptIn")) {
			participant.setTextOptIn(detail.getTextOptIn());
		}
	}

	private void setVitalStatus(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (partial && !detail.isAttrModified("vitalStatus")) {
			return;
		}

		String vitalStatus = detail.getVitalStatus();
		participant.setVitalStatus(getPv(VITAL_STATUS, vitalStatus, ParticipantErrorCode.INVALID_VITAL_STATUS, oce));
	}

	private void setBirthAndDeathDate(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (!partial || detail.isAttrModified("birthDate")) {
			Date birthDate = detail.getBirthDate();
			if (birthDate != null && birthDate.after(Calendar.getInstance().getTime())) {
				oce.addError(ParticipantErrorCode.INVALID_BIRTH_DATE);
				return;
			}

			participant.setBirthDate(birthDate);
		}

		PermissibleValue vitalStatus = participant.getVitalStatus();
		Map<String, String> pvProps = vitalStatus != null ? vitalStatus.getProps() : Collections.emptyMap();
		if (vitalStatus == null || !"true".equals(pvProps.get("dead"))) {
			participant.setDeathDate(null);
		} else if (!partial || detail.isAttrModified("deathDate")) {
			Date deathDate = detail.getDeathDate();
			if (deathDate != null && deathDate.after(Calendar.getInstance().getTime())) {
				oce.addError(ParticipantErrorCode.INVALID_DEATH_DATE);
			}

			participant.setDeathDate(deathDate);
		}

		if (participant.getBirthDate() != null && participant.getDeathDate() != null &&
			participant.getBirthDate().after(participant.getDeathDate())) {
			oce.addError(ParticipantErrorCode.INVALID_DEATH_DATE);
		}
	}

	private void setActivityStatus(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (partial && !detail.isAttrModified("activityStatus")) {
			return;
		}
				
		String status = detail.getActivityStatus();		
		if (StringUtils.isBlank(status)) {
			participant.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
			return;
		}
		
		if (!Status.isValidActivityStatus(status)) {
			oce.addError(ActivityStatusErrorCode.INVALID);
			return;
		}
		
		participant.setActivityStatus(status);		
	}

	private void setGender(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (partial && !detail.isAttrModified("gender")) {
			return;
		}
		
		String gender = detail.getGender();
		participant.setGender(getPv(GENDER, gender, ParticipantErrorCode.INVALID_GENDER, oce));
	}

	private void setRace(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (partial && !detail.isAttrModified("races")) {
			return;
		}

		participant.setRaces(getPvs(RACE, detail.getRaces(), ParticipantErrorCode.INVALID_RACE, oce));
	}

	private void setEthnicity(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (partial && !detail.isAttrModified("ethnicities")) {
			return;
		}
		
		participant.setEthnicities(getPvs(ETHNICITY, detail.getEthnicities(), ParticipantErrorCode.INVALID_ETHNICITY, oce));
	}
	
	private void setPmi(
			ParticipantDetail detail,
			Participant participant, 	
			boolean partial,
			OpenSpecimenException oce) {
		
		if (partial && !detail.isAttrModified("pmis")) {
			return;
		}

		if (partial) {
			boolean unique = ParticipantUtil.ensureUniquePmis(
					daoFactory, 
					detail.getPmis(), 
					participant, 
					oce);
			if (!unique) {
				return;
			}
		}
		
		Set<ParticipantMedicalIdentifier> newPmis = new HashSet<>();
		if (CollectionUtils.isEmpty(detail.getPmis())) {
			participant.setPmis(newPmis);
		} else {
			Set<String> siteNames = new HashSet<>();
			boolean dupSite = false;
			
			for (PmiDetail pmiDetail : detail.getPmis()) {
				ParticipantMedicalIdentifier pmi = getPmi(pmiDetail, oce);
				if (pmi == null) {
					continue;
				}
				
				if (!dupSite && !siteNames.add(pmiDetail.getSiteName())) {
					dupSite = true;
					oce.addError(ParticipantErrorCode.DUP_MRN_SITE, pmiDetail.getSiteName());
				}
				
				pmi.setParticipant(participant);
				newPmis.add(pmi);
			}			
		}
				
		participant.setPmis(newPmis);
	}

	private ParticipantMedicalIdentifier getPmi(PmiDetail pmiDetail, OpenSpecimenException oce) {
		if (StringUtils.isBlank(pmiDetail.getSiteName()) && StringUtils.isBlank(pmiDetail.getMrn())) {
			return null;
		}

		Site site = daoFactory.getSiteDao().getSiteByName(pmiDetail.getSiteName());		
		if (site == null) {
			oce.addError(SiteErrorCode.NOT_FOUND);
			return null;
		}
		
		ParticipantMedicalIdentifier pmi = new ParticipantMedicalIdentifier();
		pmi.setSite(site);
		pmi.setMedicalRecordNumber(pmiDetail.getMrn());
		return pmi;
	}

	private PermissibleValue getPv(String attr, String value, ErrorCode invErrorCode, OpenSpecimenException ose) {
		if (StringUtils.isBlank(value)) {
			return null;
		}

		PermissibleValue pv = daoFactory.getPermissibleValueDao().getPv(attr, value);
		if (pv == null) {
			ose.addError(invErrorCode);
		}

		return pv;
	}

	private Set<PermissibleValue> getPvs(String attr, Collection<String> values, ErrorCode invErrorCode, OpenSpecimenException ose) {
		if (CollectionUtils.isEmpty(values)) {
			return new HashSet<>();
		}

		List<PermissibleValue> pvs = daoFactory.getPermissibleValueDao().getPvs(attr, values);
		if (pvs.size() != values.size()) {
			ose.addError(invErrorCode);
		}

		return new HashSet<>(pvs);
	}
}
