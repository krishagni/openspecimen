package com.krishagni.catissueplus.rest;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.HtmlUtils;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.errors.ParameterizedError;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

import jakarta.servlet.http.HttpServletRequest;

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

		if (exception instanceof OpenSpecimenException ose) {
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
			String msg = Utility.getErrorMessage(rootCause);
			errorMsgs.add(getMessage(INTERNAL_ERROR, new Object[] { msg }));
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		return handleExceptionInternal(exception, errorMsgs, headers, status, request);
	}

	@Override
	public ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		dumpRequestBody(request, ex);
		ErrorMessage err = new ErrorMessage(CommonErrorCode.INVALID_REQUEST.name(), Utility.getErrorMessage(ex));
		return handleExceptionInternal(ex, Collections.singletonList(err), headers, status, request);
	}

	@Override
	public ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		dumpRequestBody(request, ex);

		ErrorMessage err = new ErrorMessage(CommonErrorCode.INVALID_REQUEST.name(), Utility.getErrorMessage(ex));
		return handleExceptionInternal(ex, Collections.singletonList(err), headers, status, request);
	}

	private HttpStatus getHttpStatus(ErrorType type) {
		return switch (type) {
			case SYSTEM_ERROR, UNKNOWN_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
			case USER_ERROR -> HttpStatus.BAD_REQUEST;
			case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
			case NONE -> HttpStatus.OK;
		};
	}

	private ErrorMessage getMessage(ErrorCode error, Object[] params) {
		return getMessage(error.code(), params);
	}

	private ErrorMessage getMessage(String code, Object[] params) {
		String message = HtmlUtils.htmlEscape(MessageUtil.getInstance().getMessage(code.toLowerCase(), params))
			.replace("&#34;", "\"")
			.replace("&#39;", "'");
		return new ErrorMessage(code, message);
	}
	
	private Object[] getExceptionId(OpenSpecimenException ose) {
		Long id = ose.getExceptionId();
		return new Object[] {id != null ? id : ""};
	}

	private void dumpRequestBody(WebRequest request, Exception e) {
		if (request instanceof ServletWebRequest wr && wr.getNativeRequest() instanceof HttpServletRequest httpReq) {
			dumpRequestBody(httpReq, e);
		}
	}

	private void dumpRequestBody(HttpServletRequest httpReq, Exception e) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n--- HTTP Request Dump ---\n");
		sb.append("Method: ").append(httpReq.getMethod()).append("\n");
		sb.append("URI: ").append(httpReq.getRequestURI()).append("\n");
		sb.append("Query: ").append(httpReq.getQueryString()).append("\n");

		// Dump Parameters
		sb.append("Parameters:\n");
		for (Map.Entry<String, String[]> entry : httpReq.getParameterMap().entrySet()) {
			sb.append("  ").append(entry.getKey()).append(": ")
				.append(String.join(", ", entry.getValue())).append("\n");
		}

		// Safe Body/JSON Dump
		if (httpReq instanceof ContentCachingRequestWrapper wrapper) {
			byte[] buf = wrapper.getContentAsByteArray();
			if (buf.length > 0) {
				try {
					String body = new String(buf, 0, buf.length, wrapper.getCharacterEncoding());
					sb.append("Body Payload:\n").append(body).append("\n");
				} catch (UnsupportedEncodingException uee) {
					sb.append("[Could not read body encoding]\n");
				}
			} else {
				sb.append("Body Payload: [Empty or not yet read by controller]\n");
			}
		} else {
			sb.append("Body Payload: [Unwrapped request - body reading skipped to prevent data loss]\n");
		}

		sb.append("Error: ").append(Utility.getErrorMessage(e)).append("\n");
		sb.append("-------------------------\n");
		logger.info(sb.toString());
	}
}
