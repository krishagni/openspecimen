
package com.krishagni.catissueplus.core.common.errors;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.JDBCException;
import org.springframework.core.NestedExceptionUtils;

import com.krishagni.catissueplus.core.common.util.MessageUtil;

public class OpenSpecimenException extends RuntimeException {
	private static final long serialVersionUID = -1473557909717365251L;
	
	private ErrorType errorType = ErrorType.NONE;

	private List<ParameterizedError> errors = new ArrayList<ParameterizedError>();
	
	private Throwable exception;

	private Long exceptionId;
	
	public OpenSpecimenException(ErrorType type, ErrorCode error, Object ... params) {
		this.errorType = type;
		errors.add(new ParameterizedError(error, params));
	}
	
	public OpenSpecimenException(ErrorType type) {
		this.errorType = type;
	}
	
	public OpenSpecimenException(Throwable t) {
		this(null, t);
	}
	
	public OpenSpecimenException(Long exceptionId, Throwable t) {
		super(t);
		this.exceptionId = exceptionId;
		this.errorType = ErrorType.SYSTEM_ERROR;
		this.exception = t;

		if (t instanceof JDBCException) {
			JDBCException je = (JDBCException) t;
			String dbMsg = je.getCause().getMessage();
			if (StringUtils.isBlank(dbMsg)) {
				dbMsg = je.getSQLException().getMessage();
			}

			errors.add(new ParameterizedError(CommonErrorCode.SQL_EXCEPTION, dbMsg));
		} else if (t != null) {
			Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(t);
			String msg = rootCause.getClass().getSimpleName() + ":" + rootCause.getMessage();
			errors.add(new ParameterizedError(CommonErrorCode.SERVER_ERROR, msg));
		}
	}
	
	public ErrorType getErrorType() {
		return errorType;
	}
	
	public List<ParameterizedError> getErrors() {
		return errors;
	}

	public Throwable getException() {
		return exception;
	}
	
	public void addError(ErrorCode error, Object ... params) {
		this.errors.add(new ParameterizedError(error, params));
	}

	public void addErrors(List<ParameterizedError> errors) {
		this.errors.addAll(errors);
	}
	
	public boolean hasAnyErrors() {
		return !this.errors.isEmpty() || exception != null;
	}
	
	public void checkAndThrow() {
		if (hasAnyErrors()) {
			throw this;
		}
	}

	public boolean containsError(ErrorCode error) {
		if (CollectionUtils.isEmpty(errors)) {
			return false;
		}

		return errors.stream().anyMatch(pe -> pe.error().equals(error));
	}

	public void rethrow(ErrorCode oldError, ErrorCode newError, Object ... params) {
		if (containsError(oldError)) {
			throw OpenSpecimenException.userError(newError, params);
		}
		throw this;
	}	
	
	public String getMessage() {
		StringBuilder errorMsg = new StringBuilder();

		if (CollectionUtils.isNotEmpty(errors)) {
			for (ParameterizedError pe : errors) {
				errorMsg.append(getMessage(pe)).append(", ");
			}
			errorMsg.delete(errorMsg.length() - 2, errorMsg.length());
		} else if (exception != null) {
			if (StringUtils.isNotBlank(exception.getMessage())) {
				errorMsg.append(exception.getMessage());
			} else {
				errorMsg.append(ExceptionUtils.getStackTrace(exception));
			}
		} else {
			errorMsg.append(MessageUtil.getInstance().getMessage("internal_error"));
		}

		return errorMsg.toString();
	}

	public Long getExceptionId() {
		return exceptionId;
	}

	public void setExceptionId(Long exceptionId) {
		this.exceptionId = exceptionId;
	}

	public static OpenSpecimenException unauthorized() {
		return new OpenSpecimenException(ErrorType.UNAUTHORIZED);
	}

	public static OpenSpecimenException userError(ErrorCode error, Object ... params) {		
		return new OpenSpecimenException(ErrorType.USER_ERROR, error, params);
	}
	
	public static OpenSpecimenException serverError(ErrorCode error, Object ... params) {
		return new OpenSpecimenException(ErrorType.SYSTEM_ERROR, error, params);
	}
	
	public static OpenSpecimenException serverError(Throwable e) {
		return new OpenSpecimenException(e);
	}
	
	public static OpenSpecimenException serverError(Long exceptionId, Throwable e) {
		return new OpenSpecimenException(exceptionId, e);
	}

	private String getMessage(ParameterizedError error) {
		return MessageUtil.getInstance().getMessage(error.error().code().toLowerCase(), error.params());
	}
}
