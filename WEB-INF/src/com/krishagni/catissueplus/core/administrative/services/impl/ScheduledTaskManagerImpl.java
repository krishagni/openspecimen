package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTaskListener;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTaskManager;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.service.EmailService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.NotifUtil;
import com.krishagni.catissueplus.core.init.AppProperties;

public class ScheduledTaskManagerImpl implements ScheduledTaskManager, ScheduledTaskListener {
	private static final LogUtil logger = LogUtil.getLogger(ScheduledTaskManagerImpl.class);

	private static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);
	
	private static Map<Long, ScheduledFuture<?>> scheduledJobs = new HashMap<>();
	
	private static final String JOB_FINISHED_TEMPLATE = "scheduled_job_finished";
	
	private static final String JOB_FAILED_TEMPLATE = "scheduled_job_failed";
	
	private DaoFactory daoFactory;

	private EmailService emailSvc;

	private TransactionTemplate newTxTmpl;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setEmailSvc(EmailService emailSvc) {
		this.emailSvc = emailSvc;
	}

	public void setTransactionManager(PlatformTransactionManager txnMgr) {
		this.newTxTmpl = new TransactionTemplate(txnMgr);
		this.newTxTmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	//////////////////////////////////////////////////////////////////////////
	//
	// Scheduled Task Manager API Start
	//
	//////////////////////////////////////////////////////////////////////////
	
	@Override
	@PlusTransactional
	public void schedule(Long jobId) {
		ScheduledJob job = getScheduledJob(jobId);
		if (job == null) {
			logger.error("No job found with ID = " + jobId);
			return;
		}
		
		schedule(job);
	}
	
	@Override
	@PlusTransactional
	public void schedule(ScheduledJob job) {
		User user = daoFactory.getUserDao().getSystemUser();
		runJob(user, job, null, getNextScheduleInMin(job));
	}

	@Override
	@PlusTransactional
	public void run(ScheduledJob job, String args) {
		runJob(AuthUtil.getCurrentUser(), job, args, 0L);
	}
	
	@Override
	public void cancel(ScheduledJob job) {
		cancel(job.getId());
	}

	@Override
	public void cancel(Long jobId) {
		ScheduledFuture<?> future = scheduledJobs.remove(jobId);
		try {
			if (future != null) {
				logger.info("Unscheduling the job " + jobId);
				future.cancel(false);
			}
		} catch (Exception e) {
			logger.error("Error canceling scheduled job: " + jobId, e);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, int intervalInMinutes) {
		return executorService.scheduleWithFixedDelay(task, intervalInMinutes, intervalInMinutes, TimeUnit.MINUTES);
	}

	//////////////////////////////////////////////////////////////////////////
	//
	// Scheduled Job Listener API implementation
	//
	//////////////////////////////////////////////////////////////////////////
	
	@Override
	@PlusTransactional
	public ScheduledJobRun started(ScheduledJob job, String args, User user) {
		try {
			if (!acquireLock(job)) {
				logger.info("Relinquishing the job " + job.getName());
				return null;
			}

			logger.info("Acquired the lock on the job " + job.getName());
			job = getScheduledJob(job.getId());
			if (job == null) {
				return null;
			}

			ScheduledJobRun jobRun = new ScheduledJobRun();
			jobRun.inProgress(job);
			jobRun.setRunBy(user);
			jobRun.setRtArgs(args);
			daoFactory.getScheduledJobDao().saveOrUpdateJobRun(jobRun);
			initializeLazilyLoadedEntites(job);
			return jobRun;
		} catch (Exception e) {
			logger.error("Error creating job run. Job name: " + job.getName(), e);
			throw new RuntimeException(e);
		}
		
	}

	@Override
	@PlusTransactional
	public void completed(ScheduledJobRun jobRun) {
		ScheduledJobRun dbRun = daoFactory.getScheduledJobDao().getJobRun(jobRun.getId());
		dbRun.completed(jobRun.getLogFilePath());
		notifyJobCompleted(dbRun);
		scheduledJobs.remove(dbRun.getScheduledJob().getId());
		releaseLock(dbRun.getScheduledJob());
		logger.info("Released the lock on the job " + dbRun.getScheduledJob().getName());
		schedule(dbRun.getScheduledJob().getId());
	}

	@Override
	@PlusTransactional
	public void failed(ScheduledJobRun jobRun, Exception e) {
		ScheduledJobRun dbRun = daoFactory.getScheduledJobDao().getJobRun(jobRun.getId());
		dbRun.failed(e);
		notifyJobFailed(dbRun);
		scheduledJobs.remove(dbRun.getScheduledJob().getId());
		releaseLock(dbRun.getScheduledJob());
		logger.info("Released the lock on the job " + dbRun.getScheduledJob().getName());
		schedule(dbRun.getScheduledJob().getId());
	}

	private boolean acquireLock(ScheduledJob job) {
		return setLock(job, false);
	}

	private boolean releaseLock(ScheduledJob job) {
		return setLock(job, true);
	}

	private boolean setLock(ScheduledJob job, boolean clear) {
		return Boolean.TRUE.equals(newTxTmpl.execute(
			status -> {
				Properties appProps = AppProperties.getInstance().getProperties();
				String thisNode = appProps.getProperty("node.name", "thisNode");

				String node = daoFactory.getScheduledJobDao().getRunByNodeForUpdate(job.getId());
				logger.info("Lock on the job " + job.getName() + " is held by " + (node != null ? node : "none"));

				if (StringUtils.isNotBlank(node) &&
					((clear && !node.equals(thisNode)) || (!clear && !node.equals("none")))) {
					return false;
				}

				daoFactory.getScheduledJobDao().updateRunByNode(job.getId(), clear ? "none" : thisNode);
				return true;
			}
		));
	}
		
	private void runJob(User user, ScheduledJob job, String args, Long minutesLater) {
		if (isJobQueued(job)) {
			cancel(job);
		}

		if (!job.isActiveJob(true)) {
			logger.info("The job " + job.getName() + " cannot be scheduled.");
			return;
		}

		if (job.isOnDemand()) {
			logger.info("The job " + job.getName() + " cannot be scheduled, as its repeat frequency is on demand");
			return;
		}

		if (user != null && user.isSysUser() && job.getRunAs() != null) {
			job.getRunAs().getAuthorities();
			user = job.getRunAs();

			if (!user.isActive()) {
				logger.error("Not scheduling the job \"" + job.getName() + "\" because run as user \"" + user.formattedName() + "\" is not active");
				return;
			}
		}

		logger.info("Scheduling the job " + job.getName() + " to run " + minutesLater + " minutes later");
		ScheduledTaskWrapper taskWrapper = new ScheduledTaskWrapper(job, args, user, this);
		ScheduledFuture<?> future = executorService.schedule(taskWrapper, minutesLater, TimeUnit.MINUTES);
		scheduledJobs.put(job.getId(), future);		
	}
	
	private void initializeLazilyLoadedEntites(ScheduledJob job) {
		Hibernate.initialize(job);
		Hibernate.initialize(job.getCreatedBy());
		Hibernate.initialize(job.getRecipients());
		Hibernate.initialize(job.getSavedQuery());
	}

	private boolean isJobQueued(ScheduledJob job) {
		return scheduledJobs.containsKey(job.getId());
	}
	
	private Long getNextScheduleInMin(ScheduledJob job) {
		Date nextRunTime = job.getNextRunOn();
		if (nextRunTime == null) {
			return null;
		}

		long delay = (nextRunTime.getTime() - System.currentTimeMillis()) / (1000 * 60);
		return delay < 0 ? 0 : delay;
	}
	
	private ScheduledJob getScheduledJob(Long jobId) {
		return daoFactory.getScheduledJobDao().getById(jobId);
	}

	private void notifyJobCompleted(ScheduledJobRun jobRun) {
		sendEmail(jobRun, JOB_FINISHED_TEMPLATE, true);
	}
	
	private void notifyJobFailed(ScheduledJobRun jobRun) {
		sendEmail(jobRun, JOB_FAILED_TEMPLATE, false);
	}
	
	private void sendEmail(ScheduledJobRun jobRun, String emailTmpl, boolean success) {
		ScheduledJob job = jobRun.getScheduledJob();
		Map<String, Object> props = new HashMap<>();
		String[] subjParams = {job.getName()};
		props.put("job", job);
		props.put("jobRun", jobRun);
		props.put("$subject", subjParams);

		List<User> rcpts = new ArrayList<>(job.getRecipients());
		if (job.isOnDemand()) {
			rcpts.add(jobRun.getRunBy());
		} else {
			rcpts.add(job.getCreatedBy());
		}

		rcpts = rcpts.stream()
			.filter(rcpt -> !rcpt.isSysUser() && StringUtils.isNotBlank(rcpt.getEmailAddress()) && rcpt.isActive())
			.collect(Collectors.toList());

		if (!success && rcpts.isEmpty()) {
			//
			// failed job. need to inform at least sys admins
			//
			rcpts = daoFactory.getUserDao().getSuperAndInstituteAdmins(null);
		}

		for (User rcpt : rcpts) {
			props.put("rcpt", rcpt);
			emailSvc.sendEmail(emailTmpl, new String[] {rcpt.getEmailAddress()}, null, props);
		}

		if (!success) {
			//
			// notification alerts are sent only for failed jobs
			//
			Notification notif = new Notification();
			notif.setEntityType(ScheduledJobRun.getEntityName());
			notif.setEntityId(job.getId());
			notif.setOperation("ALERT");
			notif.setMessage(MessageUtil.getInstance().getMessage(emailTmpl.toLowerCase() + "_subj", subjParams));
			notif.setCreatedBy(AuthUtil.getCurrentUser());
			notif.setCreationTime(Calendar.getInstance().getTime());
			NotifUtil.getInstance().notify(notif, Collections.singletonMap("job-run-log", rcpts));
		}
	}
}
