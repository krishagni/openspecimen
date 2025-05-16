package com.krishagni.catissueplus.core.common.service;

import java.util.List;

import com.krishagni.catissueplus.core.common.domain.UnhandledException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UnhandledExceptionSummary;
import com.krishagni.catissueplus.core.common.repository.UnhandledExceptionListCriteria;

public interface CommonService {
	ResponseEvent<List<UnhandledExceptionSummary>> getUnhandledExceptions(RequestEvent<UnhandledExceptionListCriteria> req);

	ResponseEvent<Long> getUnhandledExceptionsCount(RequestEvent<UnhandledExceptionListCriteria> req);
	
	ResponseEvent<String> getUnhandledExceptionLog(RequestEvent<Long> req);

	String getLatestReleaseNotes();
}
