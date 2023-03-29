package com.krishagni.catissueplus.core.common.util;

import java.util.Calendar;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import com.krishagni.catissueplus.core.common.domain.UnhandledException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.service.CommonService;

@Configurable
public class UnhandledExceptionUtil {

	private static final LogUtil logger = LogUtil.getLogger(UnhandledExceptionUtil.class);

	private static UnhandledExceptionUtil instance = null;
	
	@Autowired
	private CommonService commonSvc;

	public static UnhandledExceptionUtil getInstance() {
		if (instance == null || instance.commonSvc == null) {
			instance = new UnhandledExceptionUtil();
		}

		return instance;
	}
	
	public Long saveUnhandledException(Throwable t, StackTraceElement ste, Object[] inputArgs) {
		if (commonSvc == null) {
			logger.warn("Unhandled exception sub-system not initialised yet.");
			return null;
		}

		UnhandledException exception = new UnhandledException();
		exception.setUser(AuthUtil.getCurrentUser());
		exception.setClassName(ste.getFileName());
		exception.setMethodName(ste.getMethodName());
		exception.setTimestamp(Calendar.getInstance().getTime());
		exception.setException(t.getCause() != null ? t.getCause().toString() : t.toString());
		exception.setStackTrace(ExceptionUtils.getStackTrace(t));

		if (inputArgs.length != 0) {
			Object args = inputArgs;
			if (inputArgs[0] instanceof RequestEvent) {
				args = ((RequestEvent)inputArgs[0]).getPayload();
			}

			ObjectMapper mapper = new ObjectMapper();
			FilterProvider filters = new SimpleFilterProvider()
				.addFilter("withoutId", SimpleBeanPropertyFilter.serializeAllExcept("id", "statementId"));
			try {
				exception.setInputArgs(mapper.writer(filters).writeValueAsString(args));
			} catch (Throwable jsonError) {
				// Best efforts to serialize the input arguments
				exception.setInputArgs(jsonError.getMessage());
			}
		}

		return commonSvc.saveUnhandledException(exception);
	}

}
