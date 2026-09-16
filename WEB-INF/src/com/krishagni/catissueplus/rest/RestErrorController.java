package com.krishagni.catissueplus.rest;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.HtmlUtils;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.ErrorMessageUtil;
import com.krishagni.catissueplus.core.common.util.JsonErrorUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class RestErrorController extends ResponseEntityExceptionHandler {

	private static final LogUtil logger = LogUtil.getLogger(RestErrorController.class);

	public RestErrorController() {
		super();
	}

	@ExceptionHandler(value = { Exception.class })
	public ResponseEntity<Object> handleOtherException(Exception exception, WebRequest request) {
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		if (exception instanceof OpenSpecimenException ose) {
			status = getHttpStatus(ose.getErrorType());
		}

		List<ErrorMessage> errorMsgs = ErrorMessageUtil.getMessages(exception);
		errorMsgs.forEach(error -> error.setMessage(escapeMessage(error.getMessage())));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return handleExceptionInternal(exception, errorMsgs, headers, status, request);
	}

	@Override
	public ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		dumpRequestBody(request, ex);
		ErrorMessage err = new ErrorMessage(CommonErrorCode.INVALID_REQUEST.name(), escapeMessage(JsonErrorUtil.getMessage(ex)));
		return handleExceptionInternal(ex, Collections.singletonList(err), headers, status, request);
	}

	@Override
	public ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		dumpRequestBody(request, ex);

		ErrorMessage err = new ErrorMessage(CommonErrorCode.INVALID_REQUEST.name(), getTypeMismatchMessage(ex));
		return handleExceptionInternal(ex, Collections.singletonList(err), headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		if (ex instanceof HttpRequestMethodNotSupportedException ||
			ex instanceof NoHandlerFoundException ||
			ex instanceof NoResourceFoundException) {
			logger.error("Error handling request", ex);
			String detail = "The requested endpoint was not found.";
			if (ex instanceof HttpRequestMethodNotSupportedException methodError) {
				detail = "The " + HtmlUtils.htmlEscape(methodError.getMethod()) + " method is not supported for this endpoint.";
			}

			ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
			problem.setInstance(getExternalPath(request));
			body = problem;
		}

		return super.handleExceptionInternal(ex, body, headers, status, request);
	}

	private String getTypeMismatchMessage(TypeMismatchException ex) {
		String name = ex instanceof MethodArgumentTypeMismatchException parameter ? parameter.getName() : null;
		String subject = name != null ? "The parameter \"" + HtmlUtils.htmlEscape(name) + "\"" : "A request parameter";
		Class<?> type = ex.getRequiredType();
		if (isIntegerType(type)) {
			return subject + " must be an integer.";
		} else if (isNumberType(type)) {
			return subject + " must be a number.";
		} else if (type == boolean.class || type == Boolean.class) {
			return subject + " must be a boolean (true or false).";
		}

		return subject + " has an invalid value.";
	}

	private static boolean isIntegerType(Class<?> type) {
		return type != null &&
			List.of(
				byte.class, Byte.class,
				short.class, Short.class,
				int.class, Integer.class,
				long.class, Long.class,
				BigInteger.class
			).contains(type);
	}

	private static boolean isNumberType(Class<?> type) {
		return type != null &&
			List.of(
				float.class, Float.class,
				double.class, Double.class,
				BigDecimal.class
			).contains(type);
	}

	private URI getExternalPath(WebRequest request) {
		if (!(request instanceof ServletWebRequest servletRequest)) {
			return URI.create("");
		}

		HttpServletRequest httpRequest = servletRequest.getRequest();
		String path = httpRequest.getRequestURI();
		String context = httpRequest.getContextPath();
		if (!context.isEmpty() && (path.equals(context) || path.startsWith(context + "/"))) {
			path = path.substring(context.length());
		}

		try {
			String appUrl = ConfigUtil.getInstance().getAppUrl();
			if (appUrl == null || appUrl.isBlank()) {
				return URI.create("");
			}

			String prefix = URI.create(appUrl).getRawPath();
			prefix = prefix == null ? "" : prefix.replaceAll("/+$", "");
			URI externalPath = URI.create(prefix + (path.isEmpty() ? "/" : path));
			return externalPath.getRawAuthority() == null ? externalPath : URI.create("");
		} catch (IllegalArgumentException e) {
			logger.error("Could not construct the external error path", e);
			// A non-null empty reference prevents Spring from inserting the internal request URI.
			return URI.create("");
		}
	}

	private HttpStatus getHttpStatus(ErrorType type) {
		return switch (type) {
			case SYSTEM_ERROR, UNKNOWN_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
			case USER_ERROR -> HttpStatus.BAD_REQUEST;
			case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
			case NONE -> HttpStatus.OK;
		};
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

	private String escapeMessage(String message) {
		return HtmlUtils.htmlEscape(message).replace("&#34;", "\"").replace("&#39;", "'");
	}
}
