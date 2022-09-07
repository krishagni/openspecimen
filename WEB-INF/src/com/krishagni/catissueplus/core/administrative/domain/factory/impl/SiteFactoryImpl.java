
package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import static com.krishagni.catissueplus.core.common.PvAttributes.SITE_TYPE;
import static com.krishagni.catissueplus.core.common.service.PvValidator.isValid;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.InstituteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.events.SiteDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.de.domain.DeObject;

public class SiteFactoryImpl implements SiteFactory {

	private DaoFactory daoFactory;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public Site createSite(SiteDetail detail) {
		Site site = new Site();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		site.setId(detail.getId());
		setName(detail, site, ose);
		setInstitute(detail, site, ose);
		setCode(detail, site);
		setCoordinators(detail, site, ose);
		setType(detail, site, ose);
		setAddress(detail, site);
		setActivityStatus(detail, site, ose);
		setSiteExtension(detail, site, ose);
		
		ose.checkAndThrow();
		return site;
	}
	
	@Override
	public Site createSite(Site existing, SiteDetail detail) {
		Site site = new Site();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		
		site.setId(existing.getId());
		setName(detail, existing, site, ose);
		setInstitute(detail, existing, site, ose);
		setCode(detail, existing, site, ose);
		setCoordinators(detail, existing, site, ose);
		setType(detail, existing, site, ose);
		setAddress(detail, existing, site, ose);
		setActivityStatus(detail, existing, site, ose);
		setSiteExtension(detail, existing, site, ose);
		
		ose.checkAndThrow();
		return site;		
	}
	

	private void setName(SiteDetail detail, Site site, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getName())) {
			ose.addError(SiteErrorCode.NAME_REQUIRED);
			return;
		}
		
		site.setName(detail.getName());
	}
	
	private void setName(SiteDetail detail, Site existing, Site site, OpenSpecimenException ose) {
		if (detail.isAttrModified("name")) {
			setName(detail, site, ose);
		} else {
			site.setName(existing.getName());
		}		
	}
	
	private void setInstitute(SiteDetail detail, Site site, OpenSpecimenException ose) {
		String instituteName = detail.getInstituteName();
		if (StringUtils.isBlank(instituteName)) {
			ose.addError(SiteErrorCode.INSTITUTE_REQUIRED);
			return;
		}
		
		Institute institute = daoFactory.getInstituteDao().getInstituteByName(instituteName);
		if (institute == null) {
			ose.addError(InstituteErrorCode.NOT_FOUND, instituteName, 1);
			return;
		}
		
		site.setInstitute(institute);
	}
	
	private void setInstitute(SiteDetail detail, Site existing, Site site, OpenSpecimenException ose) {
		if (detail.isAttrModified("instituteName")) {
			setInstitute(detail, site, ose);
		} else {
			site.setInstitute(existing.getInstitute());
		}		
	}
	
	private void setCode(SiteDetail detail, Site site) {
		if (StringUtils.isNotBlank(detail.getCode())) {
			site.setCode(detail.getCode().trim());
		}
	}
	
	private void setCode(SiteDetail detail, Site existing, Site site, OpenSpecimenException ose) {
		if (detail.isAttrModified("code")) {
			setCode(detail, site);
		} else {
			site.setCode(existing.getCode());
		}
	}

	private void setCoordinators(SiteDetail detail, Site site, OpenSpecimenException ose) {		
		Set<User> result = new HashSet<>();
		
		for (UserSummary userSummary : detail.getCoordinators()) {
			User user = null;
			Object key = null;

			if (userSummary.getId() != null) {
				user = daoFactory.getUserDao().getById(userSummary.getId());
				key = userSummary.getId();
			} else if (StringUtils.isNotBlank(userSummary.getEmailAddress())) {
				user = daoFactory.getUserDao().getUserByEmailAddress(userSummary.getEmailAddress());
				key = userSummary.getEmailAddress();
			}
						
			if (user == null) {
				if (key != null) {
					ose.addError(UserErrorCode.NOT_FOUND, key);
				}
			} else if (site.getInstitute() != null && !user.getInstitute().equals(site.getInstitute())) {
				ose.addError(SiteErrorCode.INVALID_COORDINATOR, user.formattedName(), site.getInstitute().getName());
			} else {
				result.add(user);
			}
		}

		site.setCoordinators(result);
	}
	
	private void setCoordinators(SiteDetail detail, Site existing, Site site, OpenSpecimenException ose) {
		if (detail.isAttrModified("coordinators")) {
			setCoordinators(detail, site, ose);
		} else {
			Institute institute = site.getInstitute();
			if (institute != null) {
				existing.getCoordinators().stream()
					.filter(user -> !user.getInstitute().equals(institute))
					.forEach(user -> ose.addError(SiteErrorCode.INVALID_COORDINATOR, user.formattedName(), institute.getName()));
			}

			site.setCoordinators(existing.getCoordinators());
		}
	}
	
	private void setType(SiteDetail detail, Site site, OpenSpecimenException ose) {
		String siteType = detail.getType();
		if (StringUtils.isBlank(siteType)) {
			ose.addError(SiteErrorCode.TYPE_REQUIRED);
			return;
		}

		PermissibleValue typePv = daoFactory.getPermissibleValueDao().getPv(SITE_TYPE, siteType);
		if (typePv == null) {
			ose.addError(SiteErrorCode.INVALID_TYPE);
			return;
		}
		
		site.setType(typePv);
	}
	
	private void setType(SiteDetail detail, Site existing, Site site, OpenSpecimenException ose) {
		if (detail.isAttrModified("type")) {
			setType(detail, site, ose);
		} else {
			site.setType(existing.getType());
		}
	}

	private void setAddress(SiteDetail detail, Site site) {
		site.setAddress(detail.getAddress());
	}
	
	private void setAddress(SiteDetail detail, Site existing, Site site, OpenSpecimenException ose) {
		if (detail.isAttrModified("address")) {
			setAddress(detail, site);
		} else {
			site.setAddress(existing.getAddress());
		}
	}
	
	private void setActivityStatus(SiteDetail detail, Site site, OpenSpecimenException ose) {
		String activityStatus = detail.getActivityStatus();
		if (StringUtils.isBlank(activityStatus)) {
			site.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
			return;
		}
		
		if (!Status.isValidActivityStatus(activityStatus)) {
			ose.addError(ActivityStatusErrorCode.INVALID);
			return;
		}
		
		site.setActivityStatus(activityStatus);
	}
	
	private void setActivityStatus(SiteDetail detail, Site existing, Site site, OpenSpecimenException ose) {
		if (detail.isAttrModified("activityStatus")) {
			setActivityStatus(detail, site, ose);
		} else {
			site.setActivityStatus(existing.getActivityStatus());
		}
	}
	
	private void setSiteExtension(SiteDetail detail, Site site, OpenSpecimenException ose) {
		DeObject extension = DeObject.createExtension(detail.getExtensionDetail(), site);
		site.setExtension(extension);
	}
	
	private void setSiteExtension(SiteDetail detail, Site existing, Site site, OpenSpecimenException ose) {
		if (detail.isAttrModified("extensionDetail")) {
			setSiteExtension(detail, site, ose);
		} else {
			site.setExtension(existing.getExtension());
		}
	}
}
