package com.krishagni.catissueplus.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.HtmlUtils;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.errors.ParameterizedError;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;

@ControllerAdvice
public class RestErrorController extends ResponseEntityExceptionHandler {

	private static final LogUtil logger = LogUtil.getLogger(RestErrorController.class);

	private static final String INTERNAL_ERROR = "internal_error";

	public RestErrorController() {
		super();
	}

	@ExceptionHandler(value = { Exception.class })
	public ResponseEntity<Object> handleOtherException(Exception exception, WebRequest request) {

		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		List<ErrorMessage> errorMsgs = new ArrayList<>();

		if (exception instanceof OpenSpecimenException) {
			OpenSpecimenException ose = (OpenSpecimenException) exception;
			status = getHttpStatus(ose.getErrorType());

			if (ose.getException() != null) {
				logger.error("Error handling request", ose.getException());

				if (CollectionUtils.isEmpty(ose.getErrors())) {
					errorMsgs.add(getMessage(INTERNAL_ERROR, getExceptionId(ose)));
				}
			}

			for (ParameterizedError error : ose.getErrors()) {
				errorMsgs.add(getMessage(error.error(), error.params()));
			}
		} else {
			logger.error("Error handling request", exception);

			Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(exception);
			String msg = rootCause.getClass().getSimpleName() + ":" + rootCause.getMessage();
			errorMsgs.add(getMessage(INTERNAL_ERROR, new Object[] { msg }));
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		return handleExceptionInternal(exception, errorMsgs, headers, status, request);
	}

	@Override
	public ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
		String msg = ex.getMessage();
		if (StringUtils.isNotBlank(msg)) {
			int idx = msg.indexOf('(');
			msg = msg.substring(0, idx);
		}

		ErrorMessage err = new ErrorMessage(CommonErrorCode.INVALID_REQUEST.name(), msg);
		return handleExceptionInternal(ex, Collections.singletonList(err), headers, status, request);
	}

	@Override
	public ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
		ErrorMessage err = new ErrorMessage(CommonErrorCode.INVALID_REQUEST.name(), ex.getMessage());
		return handleExceptionInternal(ex, Collections.singletonList(err), headers, status, request);
	}

	private HttpStatus getHttpStatus(ErrorType type) {
		switch (type) {
			case SYSTEM_ERROR:
				return HttpStatus.INTERNAL_SERVER_ERROR;
				
			case USER_ERROR:
				return HttpStatus.BAD_REQUEST;
				
			case UNKNOWN_ERROR:
				return HttpStatus.INTERNAL_SERVER_ERROR;

			case UNAUTHORIZED:
				return HttpStatus.UNAUTHORIZED;
				
			case NONE:
				return HttpStatus.OK;
				
			default:
				throw new RuntimeException("Unknown error type: " + type);
		}
	}

	private ErrorMessage getMessage(ErrorCode error, Object[] params) {
		return getMessage(error.code(), params);
	}

	private ErrorMessage getMessage(String code, Object[] params) {
		String message = MessageUtil.getInstance().getMessage(code.toLowerCase(), params);
		if (!getBoolSetting("common", "de_form_html_markup", false)) {
			message = HtmlUtils.htmlEscape(message);
		}

		return new ErrorMessage(code, message);
	}
	
	private Object[] getExceptionId(OpenSpecimenException ose) {
		Long id = ose.getExceptionId();
		return new Object[] {id != null ? id : ""};
	}

	private boolean getBoolSetting(String module, String name, boolean defValue) {
		try {
			return ConfigUtil.getInstance().getBoolSetting(module, name, defValue);
		} catch (Throwable t) {
			logger.error("Error retrieving the configuration setting", t);
		}

		return defValue;
	}
}
