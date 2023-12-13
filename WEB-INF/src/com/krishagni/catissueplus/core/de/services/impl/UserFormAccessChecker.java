package com.krishagni.catissueplus.core.de.services.impl;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.de.services.FormAccessChecker;

import krishagni.catissueplus.beans.FormContextBean;

public class UserFormAccessChecker implements FormAccessChecker {
	@Override
	public boolean isUpdateAllowed(FormContextBean formCtxt) {
		boolean allowed = false;
		User currentUser = AuthUtil.getCurrentUser();
		if (currentUser != null && currentUser.isAdmin()) {
			allowed = true;
		} else if (currentUser != null) {
			allowed = formCtxt.getEntityId() != -1L &&
				currentUser.getInstitute().getId().equals(formCtxt.getEntityId()) &&
				currentUser.isInstituteAdmin();
		}

		return allowed;
	}

	@Override
	public boolean isDataReadAllowed(Object obj) {
		return obj instanceof User;
	}

	@Override
	public boolean isDataReadAllowed(String entityType, Long objectId) {
		return true;
	}

	@Override
	public boolean isDataUpdateAllowed(Object obj) {
		boolean allowed = true;
		try {
			if (obj instanceof User) {
				AccessCtrlMgr.getInstance().ensureUpdateUserRights((User) obj);
			} else {
				allowed = false;
			}
		} catch (Exception e) {
			allowed = false;
		}

		return allowed;
	}

	@Override
	public boolean isDataUpdateAllowed(String entityType, Long objectId) {
		boolean allowed = true;
		try {
			AccessCtrlMgr.getInstance().ensureUpdateUserRights(objectId);
		} catch (Exception e) {
			allowed = false;
		}

		return allowed;
	}
}