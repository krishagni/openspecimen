package com.krishagni.catissueplus.core.common.repository;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.domain.UnhandledException;

public interface UnhandledExceptionDao extends Dao<UnhandledException> {
	List<UnhandledException> getUnhandledExceptions(UnhandledExceptionListCriteria crit);

	Long getUnhandledExceptionsCount(UnhandledExceptionListCriteria crit);
}
