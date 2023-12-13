package com.krishagni.catissueplus.core.de.services;

import krishagni.catissueplus.beans.FormContextBean;

public interface FormAccessChecker {
	boolean isUpdateAllowed(FormContextBean formCtxt);

	boolean isDataReadAllowed(Object obj);

	boolean isDataReadAllowed(String entityType, Long objectId);

	boolean isDataUpdateAllowed(Object obj);

	boolean isDataUpdateAllowed(String entityType, Long objectId);
}
