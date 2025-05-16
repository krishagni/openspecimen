
package com.krishagni.catissueplus.core.common;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.UnhandledExceptionUtil;

import edu.common.dynamicextensions.napi.FormException;

@Aspect
public class TransactionalInterceptor {
	private static final LogUtil logger = LogUtil.getLogger(TransactionalInterceptor.class);

	private PlatformTransactionManager transactionManager;
	
	private TransactionTemplate txTmpl;
	
	private TransactionTemplate newTxTmpl;

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
		this.txTmpl = new TransactionTemplate(transactionManager);
		this.txTmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		
		this.newTxTmpl = new TransactionTemplate(transactionManager);
		this.newTxTmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@Pointcut("execution(@com.krishagni.catissueplus.core.common.PlusTransactional * *(..))")
	public void transactionalMethod() {
	}

	@Around("transactionalMethod()")
	public Object startTransaction(final ProceedingJoinPoint pjp) {
		return doWork(pjp, false);
	}

	@Pointcut("execution(@com.krishagni.catissueplus.core.common.RollbackTransaction * *(..))")
	public void rollbackMethod() {
	}

	@Around("rollbackMethod()")
	public Object rollbackTransaction(final ProceedingJoinPoint pjp) {
		return doWork(pjp, true);
	}

	private Object doWork(ProceedingJoinPoint pjp, boolean rollback) {
		Map<String, Object> originalError = new HashMap<>();

		try {
			return doWork0(pjp, rollback, originalError);
		} catch (DataAccessException dae) {
			throw OpenSpecimenException.serverError(dae.getCause() != null ? dae.getCause() : dae);
		} catch (CannotCreateTransactionException te) {
			String rootCause = NestedExceptionUtils.getMostSpecificCause(te).getMessage();
			throw OpenSpecimenException.serverError(CommonErrorCode.DB_CONN_ERROR, rootCause);
		} catch (Throwable t) {
			OpenSpecimenException ose = t instanceof OpenSpecimenException ? (OpenSpecimenException) t : null;
			if (ose != null) {
				if (ose.getErrorType() == ErrorType.USER_ERROR || ose.getExceptionId() != null) {
					throw ose;
				}
			} else if (t instanceof FormException fe) {
				throw OpenSpecimenException.userError(CommonErrorCode.FORM_ERROR, fe.getError());
			} else if (!originalError.isEmpty()) {
				return originalError.get("error");
			}

			logger.error("Error doing work inside " + pjp.getSignature(), t);
			Long exceptionId = handleServerError(t.getCause() != null ? t.getCause() : t, pjp.getArgs());
			throw OpenSpecimenException.serverError(exceptionId, t);
		}
	}

	private Object doWork0(ProceedingJoinPoint pjp, boolean rollback, Map<String, Object> originalError) {
		return txTmpl.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					if (rollback) {
						status.setRollbackOnly();
					}

					Object result = pjp.proceed();
					if (result instanceof ResponseEvent<?> resp) {
						if (resp.getError() != null) {
							originalError.put("error", resp);
						}

						if (!resp.isSuccessful() && !resp.isForceTxCommitEnabled()) {
							status.setRollbackOnly();
							if (resp.isSystemError() || resp.isUnknownError()) {
								logger.error("Error doing work inside " + pjp.getSignature(), resp.getError().getException());
								OpenSpecimenException ose = resp.getError();
								Long exceptionId = handleServerError(ose.getException(), pjp.getArgs());
								ose.setExceptionId(exceptionId);
							}
						} else if (resp.isRollback()) {
							status.setRollbackOnly();
						}
					}
				
					return result;
				} catch (OpenSpecimenException ose) {
					status.setRollbackOnly();
					throw ose;
				} catch (FormException fe) {
					if (fe.getCause() != null && fe.getCause() != fe) {
						logger.error("Error encountered when doing form ops: " + fe.getMessage(), fe);
					}

					status.setRollbackOnly();
					throw OpenSpecimenException.userError(CommonErrorCode.FORM_ERROR, fe.getError());
				} catch (Throwable t) {
					logger.error("Error doing work inside " + pjp.getSignature(), t);
					status.setRollbackOnly();
					throw OpenSpecimenException.serverError(t);
				}
			}
		});
	}

	private Long handleServerError(Throwable t, Object[] args) {
		Long exceptionId = null;
		try {
			StackTraceElement ste = getSte(t);
			if (seenMaxTimesInLastInterval(ste)) {
				return exceptionId;
			}

			exceptionId = saveUnhandledExceptionToDb(t, ste, args);
			
			String itAdminEmailId = ConfigUtil.getInstance().getItAdminEmailId();
			if (StringUtils.isBlank(itAdminEmailId)) {
				logger.error("No admin or IT admin email ID has been configured to send uncaught system errors");
				return exceptionId;
			}

			EmailUtil.getInstance().sendEmail(
				SERVER_ERROR_TMPL,
				new String[] {itAdminEmailId},
				new File[] {getErrorLog(t)},
				getErrorDetail(exceptionId, t, ste));
		} catch (Exception e) {
			logger.error("Error notifying uncaught server errors", e);
		}
		
		return exceptionId;
    }

	private File getErrorLog(Throwable t)
	throws IOException {
		PrintStream ps = null;
		File tmpFile = null;
		try {
			int count = errorCount.incrementAndGet();
			tmpFile = new File(
				ConfigUtil.getInstance().getTempDir(),
				"exception_" + sdf.format(Calendar.getInstance().getTime()) + "_" + count + ".log");
			ps = new PrintStream(tmpFile);
			t.printStackTrace(ps);
			return tmpFile;
		} finally {
			IOUtils.closeQuietly(ps);

			if (tmpFile != null) {
				tmpFile.deleteOnExit();
			}
		}
	}

	private StackTraceElement getSte(Throwable t) {
		StackTraceElement[] stackTrace = t.getStackTrace();
		for (int i = 0; i < stackTrace.length; i++) {
			if (stackTrace[i].getMethodName().contains("startTransaction")) {
				return (i + 1 < stackTrace.length) ? stackTrace[++i] : stackTrace[i];
			}

		}
		return null;
	}

	private Map<String, Object> getErrorDetail(Long exceptionId, Throwable t, StackTraceElement ste) {
		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("userName", AuthUtil.getCurrentUser().getUsername());
		emailProps.put("ste", ste);
		emailProps.put("time", new Date());

		/*
		 * For hibernate exceptions
		 * Throwable "t" gives -> org.hibernate.exception.DataException: could not execute statement
		 * "t.getCause()" gives -> actual error like
		 *   com.mysql.jdbc.MysqlDataTruncation: Data truncation: Data too long for column 'NAME' at row 1
		 *
		 * For java.lang.*
		 * Throwable "t" gives -> java.lang.NullPointerException
		 * "t.getCause()" gives -> null
		 */
		emailProps.put("exception", t.getCause() != null ? t.getCause() : t);
		emailProps.put("exceptionId", exceptionId);
		return emailProps;
	}

	private synchronized boolean seenMaxTimesInLastInterval(StackTraceElement ste) {
		long currentTime = System.currentTimeMillis();
		List<Long> recentTimes = errorNotifTimes.get(ste);
		if (recentTimes == null) {
			recentTimes = new ArrayList<>();
			errorNotifTimes.put(ste, recentTimes);
		}

		if (recentTimes.size() >= MAX_INTERVAL_ERRORS) {
			//
			// Our remembering capacity has exceeded.
			// Try to remove times that do not fall in interval
			//
			Iterator<Long> iter = recentTimes.iterator();
			while (iter.hasNext()) {
				long t1 = iter.next();
				if (areTimesInSameInterval(t1, currentTime, INTERVAL_IN_MINS)) {
					break;
				}

				iter.remove();
			}
		}

		boolean seenMaxTimes = (recentTimes.size() >= MAX_INTERVAL_ERRORS);
		if (!seenMaxTimes) {
			recentTimes.add(currentTime);
		}

		return seenMaxTimes;
	}

	private boolean areTimesInSameInterval(long t1, long t2, int interval) {
		return ((t2 - t1) / (60 * 1000)) < interval;
	}
	
	private Long saveUnhandledExceptionToDb(Throwable t, StackTraceElement ste, Object[] args) {
		return newTxTmpl.execute(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				try {
					return UnhandledExceptionUtil.getInstance().saveUnhandledException(t, ste, args);
				} catch (Throwable t) {
					status.setRollbackOnly();
					logger.error("Error saving unhandled exception to DB", t);
				}

				return null;
			}
		});
	}

	//
	// Error notification variables
	//
	private static AtomicInteger errorCount = new AtomicInteger(0);

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

	private static Map<StackTraceElement, List<Long>> errorNotifTimes = new HashMap<>();

	private static final int MAX_INTERVAL_ERRORS = 5;

	private static final int INTERVAL_IN_MINS = 60;

	private static final String SERVER_ERROR_TMPL = "server_error";
}
