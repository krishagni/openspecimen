
package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.InstituteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserFactory;
import com.krishagni.catissueplus.core.administrative.events.UserDetail;
import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

public class UserFactoryImpl implements UserFactory {

	private static final LogUtil logger = LogUtil.getLogger(UserFactoryImpl.class);

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public User createUser(UserDetail detail) {
		User user = new User();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
				
		setType(detail, user, ose);
		setFirstName(detail, user, ose);
		setLastName(detail, user, ose);
		setLoginName(detail, user, ose);
		setActivityStatus(detail, user, ose);
		setEmailAddress(detail, user, ose);
		setPhoneNumber(detail, user, ose);
		setInstitute(detail, user, ose);
		setPrimarySite(detail, user, ose);
		setAddress(detail, user, ose);
		setAuthDomain(detail, user, ose);
		setManageForms(detail, user, ose);
		setManageWfs(detail, user, ose);
		setDnd(detail, user, ose);
		setApiUser(detail, user, ose);
		setIpRange(detail, user, ose);
		setTimeZone(detail, user, ose);
		setDefaultPrinter(detail, user, ose);
		setDownloadLabelsPrintFile(detail, user, ose);
		user.setCreationDate(Calendar.getInstance().getTime());
		ose.checkAndThrow();
		return user;
	}

	@Override
	public User createUser(User existing, UserDetail detail) {
		User user = new User();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		
		user.setId(existing.getId());
		setType(detail, existing, user, ose);
		setFirstName(detail, existing, user, ose);
		setLastName(detail, existing, user, ose);
		setLoginName(detail, existing, user, ose);
		setActivityStatus(detail, existing, user, ose);
		setEmailAddress(detail, existing, user, ose);
		setPhoneNumber(detail, existing, user, ose);
		setInstitute(detail, existing, user, ose);
		setPrimarySite(detail, existing, user, ose);
		setAddress(detail, existing, user, ose);
		setAuthDomain(detail, existing, user, ose);
		setManageForms(detail, existing, user, ose);
		setManageWfs(detail, existing, user, ose);
		setDnd(detail, existing, user, ose);
		setApiUser(detail, existing, user, ose);
		setIpRange(detail, existing, user, ose);
		setTimeZone(detail, existing, user, ose);
		setDefaultPrinter(detail, existing, user, ose);
		setDownloadLabelsPrintFile(detail, existing, user, ose);
		ose.checkAndThrow();
		return user;		
	}

	private void setType(UserDetail detail, User user, OpenSpecimenException ose) {
		user.setType(User.Type.NONE);

		try {
			if (StringUtils.isNotBlank(detail.getType())) {
				user.setType(User.Type.valueOf(detail.getType()));
			}
		} catch (IllegalArgumentException iae) {
			ose.addError(UserErrorCode.INVALID_TYPE, detail.getType());
		}
	}

	private void setType(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("type")) {
			setType(detail, user, ose);
		} else {
			user.setType(existing.getType());
		}
	}
	
	private void setFirstName(UserDetail detail, User user, OpenSpecimenException ose) {
		String firstName = detail.getFirstName();
		if (StringUtils.isBlank(firstName)) {
			ose.addError(UserErrorCode.FIRST_NAME_REQUIRED);
			return;
		}
		
		user.setFirstName(firstName);
	}
	
	private void setFirstName(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("firstName")) {
			setFirstName(detail, user, ose);
		} else {
			user.setFirstName(existing.getFirstName());
		}
	}
	
	private void setLastName(UserDetail detail, User user, OpenSpecimenException ose) {
		String lastName = detail.getLastName();
		if (StringUtils.isBlank(lastName)) {
			ose.addError(UserErrorCode.LAST_NAME_REQUIRED);
			return;
		}
		
		user.setLastName(lastName);
	}

	private void setLastName(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("lastName")) {
			setLastName(detail, user, ose);
		} else {
			user.setLastName(existing.getLastName());
		}
	}
	
	private void setLoginName(UserDetail detail, User user, OpenSpecimenException ose) {
		String loginName = detail.getLoginName();
		if (StringUtils.isBlank(loginName)) {
			if (!user.isContact()) {
				ose.addError(UserErrorCode.LOGIN_NAME_REQUIRED);
				return;
			}

			loginName = UUID.randomUUID().toString();
		}

		user.setLoginName(loginName);
	}
	
	private void setLoginName(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("loginName")) {
			setLoginName(detail, user, ose);
		} else {
			user.setLoginName(existing.getLoginName());
		}
	}
	
	private void setInstitute(UserDetail detail, User user, OpenSpecimenException ose) {
		String instituteName = detail.getInstituteName();
		if (StringUtils.isBlank(instituteName)) {
			ose.addError(UserErrorCode.INST_REQUIRED);
			return;
		}
		
		Institute institute = daoFactory.getInstituteDao().getInstituteByName(instituteName);
		if (institute == null) {
			ose.addError(InstituteErrorCode.NOT_FOUND, instituteName, 1);
			return;
		}
		
		user.setInstitute(institute);
	}
	
	private void setInstitute(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("instituteName")) {
			setInstitute(detail, user, ose);
		} else {
			user.setInstitute(existing.getInstitute());
		}
	}

	private void setPrimarySite(UserDetail detail, User user, OpenSpecimenException ose) {
		String siteName = detail.getPrimarySite();
		if (StringUtils.isBlank(siteName)) {
			return;
		}

		Site primarySite = daoFactory.getSiteDao().getSiteByName(siteName);
		if (primarySite == null) {
			ose.addError(SiteErrorCode.NOT_FOUND, siteName);
			return;
		}

		ensurePrimarySiteBelongsToInstitute(primarySite, user.getInstitute(), ose);
		user.setPrimarySite(primarySite);
	}

	private void setPrimarySite(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("primarySite")) {
			setPrimarySite(detail, user, ose);
		} else if (existing.getPrimarySite() != null) {
			ensurePrimarySiteBelongsToInstitute(existing.getPrimarySite(), user.getInstitute(), ose);
			user.setPrimarySite(existing.getPrimarySite());
		}
	}

	private void ensurePrimarySiteBelongsToInstitute(Site primarySite, Institute institute, OpenSpecimenException ose) {
		if (institute != null && !primarySite.getInstitute().equals(institute)) {
			ose.addError(SiteErrorCode.INVALID_SITE_INSTITUTE, primarySite.getName(), institute.getName());
		}
	}

	private void setActivityStatus(UserDetail detail, User user, OpenSpecimenException ose) {
		String status = detail.getActivityStatus();
		if (StringUtils.isBlank(status)) {
			status = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		}

		if (!isValidStatus(status)) {
			ose.addError(ActivityStatusErrorCode.INVALID, status);
		}

		user.setActivityStatus(status);
	}

	private boolean isValidStatus(String status) {
		return status.equals(Status.ACTIVITY_STATUS_ACTIVE.getStatus()) ||
			status.equals(Status.ACTIVITY_STATUS_DISABLED.getStatus()) ||
			status.equals(Status.ACTIVITY_STATUS_CLOSED.getStatus()) ||
			status.equals(Status.ACTIVITY_STATUS_LOCKED.getStatus()) ||
			status.equals(Status.ACTIVITY_STATUS_EXPIRED.getStatus()) ||
			status.equals(Status.ACTIVITY_STATUS_PENDING.getStatus());
	}

	private void setActivityStatus(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("activityStatus")) {
			setActivityStatus(detail, user, ose);
		} else {
			user.setActivityStatus(existing.getActivityStatus());
		}
	}
	
	private void setEmailAddress(UserDetail detail, User user, OpenSpecimenException ose) {
		String email = detail.getEmailAddress();
		if (!Utility.isValidEmail(email)) {
			ose.addError(UserErrorCode.INVALID_EMAIL);
			return;
		}
		
		user.setEmailAddress(email);
	}

	private void setEmailAddress(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("emailAddress")) {
			setEmailAddress(detail, user, ose);
		} else {
			user.setEmailAddress(existing.getEmailAddress());
		}
	}

	private void setPhoneNumber(UserDetail detail, User user, OpenSpecimenException ose) {
		user.setPhoneNumber(detail.getPhoneNumber());
	}

	private void setPhoneNumber(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("phoneNumber")) {
			setPhoneNumber(detail, user, ose);
		} else {
			user.setPhoneNumber(existing.getPhoneNumber());
		}
	}

	private void setAddress(UserDetail detail, User user, OpenSpecimenException ose) {
		user.setAddress(detail.getAddress());
	}

	private void setAddress(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("address")) {
			setAddress(detail, user, ose);
		} else {
			user.setAddress(existing.getAddress());
		}
	}


	private void setAuthDomain(UserDetail detail, User user, OpenSpecimenException ose) {
		String domainName = detail.getDomainName();
		if (StringUtils.isBlank(domainName)) {
			if (!user.isContact()) {
				ose.addError(UserErrorCode.DOMAIN_NAME_REQUIRED);
				return;
			}

			domainName = User.DEFAULT_AUTH_DOMAIN;
		}

		AuthDomain authDomain = daoFactory.getAuthDao().getAuthDomainByName(domainName);
		if (authDomain == null) {
			ose.addError(UserErrorCode.DOMAIN_NOT_FOUND);
			return;
		}
		
		user.setAuthDomain(authDomain);
	}

	private void setAuthDomain(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("domainName")) {
			setAuthDomain(detail, user, ose);
		} else {
			user.setAuthDomain(existing.getAuthDomain());
		}
	}

	private void setManageForms(UserDetail detail, User user, OpenSpecimenException ose) {
		switch (user.getType()) {
			case SUPER:
			case INSTITUTE:
				user.setManageForms(true);
				break;

			case CONTACT:
				user.setManageForms(false);
				break;

			default:
				user.setManageForms(detail.getManageForms());
				break;
		}
	}

	private void setManageForms(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("manageForms")) {
			setManageForms(detail, user, ose);
		} else {
			user.setManageForms(existing.getManageForms());
		}
	}

	private void setManageWfs(UserDetail detail, User user, OpenSpecimenException ose) {
		switch (user.getType()) {
			case SUPER:
			case INSTITUTE:
				user.setManageWfs(true);
				break;

			case CONTACT:
				user.setManageWfs(false);
				break;

			default:
				user.setManageWfs(detail.isManageWfs());
				break;
		}
	}

	private void setManageWfs(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("manageWfs")) {
			setManageWfs(detail, user, ose);
		} else {
			user.setManageWfs(existing.getManageWfs());
		}
	}

	private void setDnd(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("dnd")) {
			setDnd(detail, user, ose);
		} else {
			user.setDnd(existing.getDnd());
		}
	}

	private void setDnd(UserDetail detail, User user, OpenSpecimenException ose) {
		user.setDnd(detail.getDnd());
	}

	private void setApiUser(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("apiUser")) {
			setApiUser(detail, user, ose);
		} else {
			user.setApiUser(existing.isApiUser());
		}
	}

	private void setApiUser(UserDetail detail, User user, OpenSpecimenException ose) {
		user.setApiUser(detail.isApiUser());
	}

	private void setIpRange(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("ipRange")) {
			setIpRange(detail, user, ose);
		} else {
			user.setIpRange(existing.getIpRange());
		}

		if (user.isApiUser() && StringUtils.isBlank(user.getIpRange())) {
			ose.addError(UserErrorCode.IP_REQ);
		}

	}

	private void setIpRange(UserDetail detail, User user, OpenSpecimenException ose) {
		user.setIpRange(detail.getIpRange());

		try {
			if (StringUtils.isNotBlank(user.getIpRange()) && !user.getIpRange().equals("*")) {
				new IpAddressMatcher(user.getIpRange());
			}
		} catch (Exception e) {
			ose.addError(UserErrorCode.INVALID_IP, user.getIpRange());
		}

		if (user.isApiUser() && StringUtils.isBlank(user.getIpRange())) {
			ose.addError(UserErrorCode.IP_REQ);
		}
	}

	private void setTimeZone(UserDetail detail, User user, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getTimeZone())) {
			return;
		}

		try {
			TimeZone tz = TimeZone.getTimeZone(detail.getTimeZone());
			if (tz == null) {
				ose.addError(CommonErrorCode.INVALID_TZ, detail.getTimeZone());
			}

			user.setTimeZone(detail.getTimeZone());
		} catch (Exception e) {
			ose.addError(CommonErrorCode.INVALID_TZ, detail.getTimeZone());
			logger.error("Error obtaining timezone for " + detail.getTimeZone(), e);
		}
	}

	private void setTimeZone(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("timeZone")) {
			setTimeZone(detail, user, ose);
		} else {
			user.setTimeZone(existing.getTimeZone());
		}
	}

	private void setDefaultPrinter(UserDetail detail, User user, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getDefaultPrinter())) {
			user.setDefaultPrinter(null);
			return;
		}

		PermissibleValue pv = daoFactory.getPermissibleValueDao().getByValue("system_printer_name", detail.getDefaultPrinter());
		if (pv == null) {
			ose.addError(UserErrorCode.INVALID_PRINTER_NAME, detail.getDefaultPrinter());
		}

		user.setDefaultPrinter(pv);
	}

	private void setDefaultPrinter(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("defaultPrinter")) {
			setDefaultPrinter(detail, user, ose);
		} else {
			user.setDefaultPrinter(existing.getDefaultPrinter());
		}
	}

	private void setDownloadLabelsPrintFile(UserDetail detail, User user, OpenSpecimenException ose) {
		user.setDownloadLabelsPrintFile(detail.isDownloadLabelsPrintFile());
	}

	private void setDownloadLabelsPrintFile(UserDetail detail, User existing, User user, OpenSpecimenException ose) {
		if (detail.isAttrModified("downloadLabelsPrintFile")) {
			setDownloadLabelsPrintFile(detail, user, ose);
		} else {
			user.setDownloadLabelsPrintFile(existing.isDownloadLabelsPrintFile());
		}
	}
}
