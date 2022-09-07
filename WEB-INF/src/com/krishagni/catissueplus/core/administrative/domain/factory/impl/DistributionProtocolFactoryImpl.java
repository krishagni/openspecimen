
package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.DpDistributionSite;
import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionProtocolErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionProtocolFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.InstituteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.events.DistributionProtocolDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.domain.Form;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.events.FormSummary;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;

public class DistributionProtocolFactoryImpl implements DistributionProtocolFactory {
	private DaoFactory daoFactory;
	
	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;

	private LabelGenerator distributionLabelGenerator;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	public void setDistributionLabelGenerator(LabelGenerator distributionLabelGenerator) {
		this.distributionLabelGenerator = distributionLabelGenerator;
	}

	@Override
	public DistributionProtocol createDistributionProtocol(DistributionProtocolDetail detail) {
		return createDistributionProtocol(null, detail);
	}

	@Override
	public DistributionProtocol createDistributionProtocol(DistributionProtocol existing, DistributionProtocolDetail detail) {
		DistributionProtocol dp = new DistributionProtocol();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		dp.setId(detail.getId());
		setTitle(detail, existing, dp, ose);
		setShortTitle(detail, existing, dp, ose);
		setInstitute(detail, existing, dp, ose);
		setDefReceivingSite(detail, existing, dp, ose);
		setPrincipalInvestigator(detail, existing, dp, ose);
		setCoordinators(detail, existing, dp, ose);
		setIrbId(detail, existing, dp, ose);
		setStartDate(detail, existing, dp);
		setEndDate(detail, existing, dp);
		setActivityStatus(detail, existing, dp, ose);
		setReport(detail, existing, dp, ose);
		setOrderExtnForm(detail, existing, dp, ose);
		setDisableEmailNotifs(detail, existing, dp, ose);
		setAttachmentType(detail, existing, dp, ose);
		setOrderItemLabelFormat(detail, existing, dp, ose);
		setDistributingSites(detail, existing, dp, ose);
		setExtension(detail, existing, dp, ose);

		ose.checkAndThrow();
		return dp;
	}

	private void setTitle(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("title")) {
			setTitle(detail, dp, ose);
		} else {
			dp.setTitle(existing.getTitle());
		}
	}

	private void setTitle(DistributionProtocolDetail detail, DistributionProtocol dp, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getTitle())) {
			ose.addError(DistributionProtocolErrorCode.TITLE_REQUIRED);
			return;
		}
		dp.setTitle(detail.getTitle());
	}

	private void setShortTitle(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("shortTitle")) {
			setShortTitle(detail, dp, ose);
		} else {
			dp.setShortTitle(existing.getShortTitle());
		}
	}

	private void setShortTitle(DistributionProtocolDetail detail, DistributionProtocol dp, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getShortTitle())) {
			ose.addError(DistributionProtocolErrorCode.SHORT_TITLE_REQUIRED);
			return;
		}
		dp.setShortTitle(detail.getShortTitle());
	}
	
	private void setInstitute(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("instituteName")) {
			setInstitute(detail, dp, ose);
		} else {
			dp.setInstitute(existing.getInstitute());
		}
	}

	private void setInstitute(DistributionProtocolDetail detail, DistributionProtocol dp, OpenSpecimenException ose) {
		String instituteName = detail.getInstituteName();
		if (StringUtils.isBlank(instituteName)) {
			ose.addError(DistributionProtocolErrorCode.INSTITUTE_REQUIRED);
			return;
		}
		
		Institute institute = daoFactory.getInstituteDao().getInstituteByName(instituteName);
		if (institute == null) {
			ose.addError(InstituteErrorCode.NOT_FOUND, instituteName, 1);
			return;
		}
		
		dp.setInstitute(institute);
	}

	private void setDefReceivingSite(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("defReceivingSiteName")) {
			setDefReceivingSite(detail, dp, ose);
		} else {
			dp.setDefReceivingSite(existing.getDefReceivingSite());
		}
	}
	
	private void setDefReceivingSite(DistributionProtocolDetail detail, DistributionProtocol dp, OpenSpecimenException ose) {
		String defReceivingSiteName = detail.getDefReceivingSiteName();
		if (StringUtils.isBlank(defReceivingSiteName)) {
			return;
		}
		
		Site defReceivingSite = daoFactory.getSiteDao().getSiteByName(defReceivingSiteName);
		if (defReceivingSite == null) {
			ose.addError(SiteErrorCode.NOT_FOUND);
			return;
		}
		
		if (dp.getInstitute() != null && !defReceivingSite.getInstitute().equals(dp.getInstitute())) {
			ose.addError(SiteErrorCode.INVALID_SITE_INSTITUTE, defReceivingSite.getName(), dp.getInstitute().getName());
			return;
		}
		
		dp.setDefReceivingSite(defReceivingSite);
	}

	private void setPrincipalInvestigator(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("principalInvestigator")) {
			setPrincipalInvestigator(detail, dp, ose);
		} else {
			dp.setPrincipalInvestigator(existing.getPrincipalInvestigator());
		}
	}
	
	private void setPrincipalInvestigator(DistributionProtocolDetail detail, DistributionProtocol dp, OpenSpecimenException ose) {
		UserSummary user = detail.getPrincipalInvestigator();
		if (user == null) {
			ose.addError(DistributionProtocolErrorCode.PI_REQUIRED);
			return;
		}

		User pi = getUser(user, ose);
		if (pi == null) {
			return;
		}

		if (dp.getInstitute() != null && !pi.getInstitute().equals(dp.getInstitute())) {
			ose.addError(DistributionProtocolErrorCode.PI_DOES_NOT_BELONG_TO_INST, pi.formattedName(), dp.getInstitute().getName());
			return;
		}
		
		dp.setPrincipalInvestigator(pi);
	}

	private void setCoordinators(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("coordinators")) {
			setCoordinators(detail, dp, ose);
		} else {
			dp.setCoordinators(existing.getCoordinators());
		}
	}
	
	private void setCoordinators(DistributionProtocolDetail input, DistributionProtocol dp, OpenSpecimenException ose) {
		List<UserSummary> users = input.getCoordinators();
		if (CollectionUtils.isEmpty(users)) {
			return;
		}

		Set<User> coordinators = new HashSet<>();
		for (UserSummary user : users) {
			User coordinator = getUser(user, ose);
			if (coordinator == null) {
				continue;
			}

			coordinators.add(coordinator);
		}

		dp.setCoordinators(coordinators);
	}

	private void setIrbId(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("irbId")) {
			setIrbId(detail, dp);
		} else {
			dp.setIrbId(existing.getIrbId());
		}
	}

	private void setIrbId(DistributionProtocolDetail detail, DistributionProtocol dp) {
		dp.setIrbId(detail.getIrbId());
	}

	private void setStartDate(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp) {
		if (existing == null || detail.isAttrModified("startDate")) {
			setStartDate(detail, dp);
		} else {
			dp.setStartDate(existing.getStartDate());
		}
	}

	private void setStartDate(DistributionProtocolDetail detail, DistributionProtocol dp) {
		dp.setStartDate(detail.getStartDate());
	}

	private void setEndDate(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp) {
		if (existing == null || detail.isAttrModified("endDate")) {
			setEndDate(detail, dp);
		} else {
			dp.setEndDate(existing.getEndDate());
		}
	}

	private void setEndDate(DistributionProtocolDetail detail, DistributionProtocol distributionProtocol) {
		distributionProtocol.setEndDate(detail.getEndDate());
	}

	private void setActivityStatus(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("activityStatus")) {
			setActivityStatus(detail, dp, ose);
		} else {
			dp.setActivityStatus(existing.getActivityStatus());
		}
	}

	private void setActivityStatus(DistributionProtocolDetail detail, DistributionProtocol dp, OpenSpecimenException ose) {
		String activityStatus = detail.getActivityStatus();
		if (StringUtils.isBlank(activityStatus)) {
			activityStatus = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		} else if (!Status.isValidActivityStatus(activityStatus)) {
			ose.addError(ActivityStatusErrorCode.INVALID);
			return;
		}
		
		dp.setActivityStatus(activityStatus);
	}

	private void setReport(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("report")) {
			setReport(detail, dp, ose);
		} else {
			dp.setReport(existing.getReport());
		}
	}

	private void setReport(DistributionProtocolDetail detail, DistributionProtocol dp, OpenSpecimenException ose) {
		if (detail.getReport() == null || detail.getReport().getId() == null) {
			return;
		}

		SavedQuery report = deDaoFactory.getSavedQueryDao().getQuery(detail.getReport().getId());
		if (report == null) {
			ose.addError(SavedQueryErrorCode.NOT_FOUND, detail.getReport().getId());
			return;
		}

		dp.setReport(report);
	}

	private void setOrderExtnForm(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("orderExtnForm")) {
			setOrderExtnForm(detail, dp, ose);
		} else {
			dp.setOrderExtnForm(existing.getOrderExtnForm());
		}
	}

	private void setOrderExtnForm(DistributionProtocolDetail detail, DistributionProtocol dp, OpenSpecimenException ose) {
		if (detail.getOrderExtnForm() == null) {
			return;
		}

		Form form = null;
		Object key = null;
		FormSummary formDetail = detail.getOrderExtnForm();
		if (formDetail.getFormId() != null) {
			form = deDaoFactory.getFormDao().getFormById(formDetail.getFormId());
			key = formDetail.getFormId();
		} else if (StringUtils.isNotBlank(formDetail.getName())) {
			form = deDaoFactory.getFormDao().getFormByName(formDetail.getName());
			key = formDetail.getName();
		}

		if (form != null) {
			dp.setOrderExtnForm(form);
		} else if (key != null) {
			ose.addError(FormErrorCode.NOT_FOUND, key, 1);
		}
	}

	private void setDisableEmailNotifs(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("disableEmailNotifs")) {
			dp.setDisableEmailNotifs(detail.getDisableEmailNotifs());
		} else {
			dp.setDisableEmailNotifs(existing.getDisableEmailNotifs());
		}
	}

	private void setAttachmentType(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("attachmentType")) {
			if (StringUtils.isNotBlank(detail.getAttachmentType())) {
				try {
					dp.setAttachmentType(DistributionProtocol.NotifAttachmentType.valueOf(detail.getAttachmentType()));
				} catch (Exception e) {
					ose.addError(DistributionProtocolErrorCode.INV_ATTACH_TYPE, detail.getAttachmentType());
				}
			}
		} else {
			dp.setAttachmentType(existing.getAttachmentType());
		}
	}

	private void setOrderItemLabelFormat(DistributionProtocolDetail detail, DistributionProtocol dp, OpenSpecimenException ose) {
		String fmt = detail.getOrderItemLabelFormat();
		if (StringUtils.isNotBlank(fmt) && !distributionLabelGenerator.isValidLabelTmpl(fmt)) {
			ose.addError(DistributionProtocolErrorCode.INV_OI_LABEL_FMT, fmt);
		}

		dp.setOrderItemLabelFormat(fmt);
	}

	private void setOrderItemLabelFormat(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("orderItemLabelFormat")) {
			setOrderItemLabelFormat(detail, dp, ose);
		} else {
			dp.setOrderItemLabelFormat(existing.getOrderItemLabelFormat());
		}
	}

	private void setDistributingSites(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("distributingSites")) {
			setDistributingSites(detail, dp, ose);
		} else {
			dp.setDistributingSites(existing.getDistributingSites());
		}
	}

	private void setDistributingSites(DistributionProtocolDetail detail, DistributionProtocol dp, OpenSpecimenException ose) {
		if (Utility.isEmpty(detail.getDistributingSites())) {
			ose.addError(DistributionProtocolErrorCode.DISTRIBUTING_SITES_REQUIRED);
			return;
		}
		
		List<String> siteNames = new ArrayList<String>();
		List<String> instituteNames = new ArrayList<String>();
		for (Map.Entry<String, List<String>> instSites : detail.getDistributingSites().entrySet()) {
			if (CollectionUtils.isNotEmpty(instSites.getValue())) {
				siteNames.addAll(instSites.getValue());
			} else if (instSites.getKey() != null) {
				instituteNames.add(instSites.getKey());
			}
		}
		
		Set<DpDistributionSite> distSites = new HashSet<DpDistributionSite>();
		
		if (CollectionUtils.isNotEmpty(siteNames)) {
			List<Site> distributingSites = daoFactory.getSiteDao().getSitesByNames(siteNames);
			if (distributingSites.size() != siteNames.size()) {
				ose.addError(DistributionProtocolErrorCode.INVALID_DISTRIBUTING_SITES);
				return;
			}
			
			for (Site site : distributingSites) {
				distSites.add(makeDistributingSite(dp, site.getInstitute(), site));
			}
		}
		
		if (CollectionUtils.isNotEmpty(instituteNames)) {
			List<Institute> distInstitutes = daoFactory.getInstituteDao().getInstituteByNames(instituteNames);
			if (distInstitutes.size() != instituteNames.size()) {
				ose.addError(DistributionProtocolErrorCode.INVALID_DISTRIBUTING_INSTITUTE);
				return;
			}
			
			for (Institute inst : distInstitutes) {
				distSites.add(makeDistributingSite(dp, inst, null));
			}
		}
		
		dp.setDistributingSites(distSites);
	}
	
	private DpDistributionSite makeDistributingSite(DistributionProtocol dp, Institute inst, Site site) {
		DpDistributionSite distSite = new DpDistributionSite();
		distSite.setDistributionProtocol(dp);
		distSite.setInstitute(inst);
		distSite.setSite(site);
		
		return distSite;
	}

	private void setExtension(DistributionProtocolDetail detail, DistributionProtocol existing, DistributionProtocol dp, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("extensionDetail")) {
			setExtension(detail, dp, ose);
		} else {
			dp.setExtension(existing.getExtension());
		}
	}

	private void setExtension(DistributionProtocolDetail detail, DistributionProtocol dp, OpenSpecimenException ose) {
		DeObject extension = DeObject.createExtension(detail.getExtensionDetail(), dp);
		dp.setExtension(extension);
	}

	private User getUser(UserSummary detail, OpenSpecimenException ose) {
		if (detail == null) {
			return null;
		}

		User user = null;
		Object key = null;
		if (detail.getId() != null) {
			user = daoFactory.getUserDao().getById(detail.getId());
			key = detail.getId();
		} else if (StringUtils.isNotBlank(detail.getLoginName()) && StringUtils.isNotBlank(detail.getDomain())) {
			user = daoFactory.getUserDao().getUser(detail.getLoginName(), detail.getDomain());
			key = detail.getLoginName() + ":" + detail.getDomain();
		} else if (StringUtils.isNotBlank(detail.getEmailAddress())) {
			user = daoFactory.getUserDao().getUserByEmailAddress(detail.getEmailAddress());
			key = detail.getEmailAddress();
		}

		if (key != null && user == null) {
			ose.addError(UserErrorCode.NOT_FOUND, key);
		}

		return user;
	}
}
