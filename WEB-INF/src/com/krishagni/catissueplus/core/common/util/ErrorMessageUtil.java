package com.krishagni.catissueplus.core.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.krishagni.catissueplus.core.administrative.domain.factory.PvErrorCode;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.errors.ParameterizedError;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.catissueplus.rest.ErrorMessage;

//
// Formats safe plain-text errors.
// Callers rendering HTML must escape the resulting messages.
//
public class ErrorMessageUtil {
	private static final LogUtil logger = LogUtil.getLogger(ErrorMessageUtil.class);

	private static final String INTERNAL_ERROR = "internal_error";

	public static List<ErrorMessage> getMessages(Throwable exception) {
		List<ErrorMessage> errorMsgs = new ArrayList<>();

		if (exception instanceof OpenSpecimenException ose) {
			boolean diagnostic = ose.getException() != null || ose.getCause() != null ||
				ose.getErrors().stream().anyMatch(error -> hasDiagnosticDetails(error.error()));
			String reference = diagnostic ? logDiagnostic(ose, ose.getExceptionId()) : null;
			if (ose.getException() != null && CollectionUtils.isEmpty(ose.getErrors())) {
				errorMsgs.add(getMessage(INTERNAL_ERROR, new Object[] {reference}));
			}

			for (ParameterizedError error : ose.getErrors()) {
				ErrorMessage message = getApplicationMessage(error);
				if (reference != null) {
					message.setMessage(message.getMessage() + " " + getMessage("internal_error_reference", new Object[] {reference}).getMessage());
				}
				errorMsgs.add(message);
			}
		} else {
			String reference = logDiagnostic(exception, null);
			errorMsgs.add(getMessage(INTERNAL_ERROR, new Object[] {reference}));
		}

		return errorMsgs;
	}

	public static String getMessage(Throwable exception) {
		String message = getMessages(exception).stream().map(ErrorMessage::getMessage).collect(Collectors.joining("; "));
		if (!message.isBlank()) {
			return message;
		}

		String fallback = exception instanceof OpenSpecimenException ose && ose.getErrorType() == ErrorType.UNAUTHORIZED
			? "You do not have permission to perform this operation."
			: "The operation could not be completed.";
		return getDiagnosticMessage(exception, fallback);
	}

	public static String getDiagnosticMessage(Throwable exception, String safeMessage) {
		Long id = exception instanceof OpenSpecimenException ose ? ose.getExceptionId() : null;
		String reference = logDiagnostic(exception, id);
		return safeMessage + " " + getMessage("internal_error_reference", new Object[] {reference}).getMessage();
	}

	private static String logDiagnostic(Throwable exception, Long exceptionId) {
		String reference = exceptionId != null ? exceptionId.toString() : UUID.randomUUID().toString();
		logger.error("Error handling request. Error reference: " + reference, exception);
		if (exception instanceof OpenSpecimenException ose && ose.getException() != null && ose.getException() != ose.getCause()) {
			logger.error("Underlying exception. Error reference: " + reference, ose.getException());
		}
		return reference;
	}

	private static boolean hasDiagnosticDetails(ErrorCode code) {
		return code == CommonErrorCode.SERVER_ERROR || code == CommonErrorCode.SQL_EXCEPTION ||
			code == CommonErrorCode.DB_CONN_ERROR || code == CommonErrorCode.FILE_SEND_ERROR ||
			code == CommonErrorCode.DATE_PARSE_ERROR || code == PvErrorCode.IN_USE ||
			code == FormErrorCode.PV_CONV_SCHEMA_RESET_FAILED || code == FormErrorCode.PV_CONV_ROLLBACK_FAILED;
	}

	private static ErrorMessage getApplicationMessage(ParameterizedError error) {
		ErrorCode code = error.error();
		if (!hasDiagnosticDetails(code)) {
			return getMessage(code, error.params());
		}

		// Only documented public parameters cross the API boundary. Keep the original error for diagnostics.
		Object[] params = new Object[0];
		if (code == PvErrorCode.IN_USE) {
			params = new Object[] {getPublicParam(error, 0, "specified value")};
		} else if (code == CommonErrorCode.DATE_PARSE_ERROR) {
			params = new Object[] {getPublicParam(error, 0, "specified value"), getPublicParam(error, 1, "specified format")};
		} else if (code == CommonErrorCode.FILE_SEND_ERROR) {
			params = new Object[] {getPublicParam(error, 1, "requested file")};
		}

		ErrorMessage message = getMessage(code.code().toLowerCase() + "_api", params);
		message.setCode(code.code());
		return message;
	}

	private static Object getPublicParam(ParameterizedError error, int index, String fallback) {
		Object[] params = error.params();
		return params != null && params.length > index && params[index] != null ? params[index] : fallback;
	}

	private static ErrorMessage getMessage(ErrorCode error, Object[] params) {
		return getMessage(error.code(), params);
	}

	private static ErrorMessage getMessage(String code, Object[] params) {
		String message = MessageUtil.getInstance().getMessage(code.toLowerCase(), params);
		return new ErrorMessage(code, message);
	}
}
