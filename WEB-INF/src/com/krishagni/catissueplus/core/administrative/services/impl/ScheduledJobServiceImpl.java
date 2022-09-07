package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobSavedEvent;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.ScheduledJobErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.ScheduledJobFactory;
import com.krishagni.catissueplus.core.administrative.events.JobExportDetail;
import com.krishagni.catissueplus.core.administrative.events.JobRunsListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ScheduledJobDetail;
import com.krishagni.catissueplus.core.administrative.events.ScheduledJobListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ScheduledJobRunDetail;
import com.krishagni.catissueplus.core.administrative.services.ScheduledJobService;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTaskManager;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;

public class ScheduledJobServiceImpl implements ScheduledJobService, ApplicationListener<ContextRefreshedEvent> {
	private DaoFactory daoFactory;
	
	private ScheduledJobFactory jobFactory;
	
	private ScheduledTaskManager taskMgr;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setJobFactory(ScheduledJobFactory jobFactory) {
		this.jobFactory = jobFactory;
	}

	public void setTaskMgr(ScheduledTaskManager taskMgr) {
		this.taskMgr = taskMgr;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ScheduledJobDetail>> getScheduledJobs(RequestEvent<ScheduledJobListCriteria> req) {
		try {
			AccessCtrlMgr.getInstance().ensureReadScheduledJobRights();
			ScheduledJobListCriteria crit = req.getPayload();
			if (!AuthUtil.isAdmin()) {
				crit.userId(AuthUtil.getCurrentUser().getId());
			}

			List<ScheduledJob> jobs = daoFactory.getScheduledJobDao().getScheduledJobs(crit);
			Map<Long, ScheduledJob> jobsMap = jobs.stream().collect(Collectors.toMap(ScheduledJob::getId, job -> job));

			Map<Long, Date> jobsLastRuntime = daoFactory.getScheduledJobDao().getJobsLastRunTime(jobsMap.keySet());
			jobsLastRuntime.forEach((jobId, lastRuntime) -> jobsMap.get(jobId).setLastRunOn(lastRuntime));
			return ResponseEvent.response(ScheduledJobDetail.from(jobs));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getScheduledJobsCount(RequestEvent<ScheduledJobListCriteria> req) {
		try {
			AccessCtrlMgr.getInstance().ensureReadScheduledJobRights();
			ScheduledJobListCriteria crit = req.getPayload();
			if (!AuthUtil.isAdmin()) {
				crit.userId(AuthUtil.getCurrentUser().getId());
			}

			return ResponseEvent.response(daoFactory.getScheduledJobDao().getScheduledJobsCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ScheduledJobDetail> getScheduledJob(RequestEvent<Long> req) {
		try {
			ScheduledJob job = daoFactory.getScheduledJobDao().getById(req.getPayload());
			if (job == null) {
				return ResponseEvent.userError(ScheduledJobErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureReadScheduledJobRights(job);
			return ResponseEvent.response(ScheduledJobDetail.from(job));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}


	@Override
	@PlusTransactional
	public ResponseEvent<ScheduledJobDetail> createScheduledJob(RequestEvent<ScheduledJobDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureCreateScheduledJobRights();
						
			ScheduledJob job = jobFactory.createScheduledJob(req.getPayload());
			job.setCreatedBy(AuthUtil.getCurrentUser());
			ensureUniqueJobName(job);
			
			daoFactory.getScheduledJobDao().saveOrUpdate(job, true);
			taskMgr.schedule(job);
			sendSharedJobNotification(job, job.getSharedWith());
			EventPublisher.getInstance().publish(new ScheduledJobSavedEvent(job));
			return ResponseEvent.response(ScheduledJobDetail.from(job));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ScheduledJobDetail> updateScheduledJob(RequestEvent<ScheduledJobDetail> req) {
		try {
			Long jobId = req.getPayload().getId();
			ScheduledJob existing = daoFactory.getScheduledJobDao().getById(jobId);
			if (existing == null) {
				return ResponseEvent.userError(ScheduledJobErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureUpdateScheduledJobRights(existing);

			ScheduledJobDetail input = req.getPayload();
			if (input.getStartDate() == null) {
				input.setStartDate(existing.getStartDate());
			}

			ScheduledJob job = jobFactory.createScheduledJob(input);
			if (!existing.getName().equals(job.getName())) {
				ensureUniqueJobName(job);
			}

			Set<User> existingSharedUsers = new HashSet<>(existing.getSharedWith());
			existing.update(job);
			daoFactory.getScheduledJobDao().saveOrUpdate(existing);			
			taskMgr.schedule(existing);

			List<User> newSharedUsers = job.getSharedWith().stream()
				.filter(su -> !existingSharedUsers.contains(su))
				.collect(Collectors.toList());
			sendSharedJobNotification(job, newSharedUsers);

			EventPublisher.getInstance().publish(new ScheduledJobSavedEvent(existing));
			return ResponseEvent.response(ScheduledJobDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ScheduledJobDetail> deleteScheduledJob(RequestEvent<Long> req) {
		try {
			ScheduledJob job = daoFactory.getScheduledJobDao().getById(req.getPayload());
			if (job == null) {
				return ResponseEvent.userError(ScheduledJobErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureDeleteScheduledJobRights(job);
			taskMgr.cancel(job);
			job.delete();
			daoFactory.getScheduledJobDao().saveOrUpdate(job);
			EventPublisher.getInstance().publish(new ScheduledJobSavedEvent(job));
			return ResponseEvent.response(ScheduledJobDetail.from(job));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ScheduledJobDetail> executeJob(RequestEvent<ScheduledJobRunDetail> req) {
		try {
			ScheduledJobRunDetail detail = req.getPayload();
			ScheduledJob job = daoFactory.getScheduledJobDao().getById(detail.getJobId());
			if (job == null) {
				return ResponseEvent.userError(ScheduledJobErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureRunJobRights(job);
			taskMgr.run(job, detail.getRtArgs());
			return ResponseEvent.response(ScheduledJobDetail.from(job));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	

	//////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Job Run APIs
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<ScheduledJobRunDetail>> getJobRuns(RequestEvent<JobRunsListCriteria> req) {
		try {
			AccessCtrlMgr.getInstance().ensureReadScheduledJobRights();
			JobRunsListCriteria crit = req.getPayload();
			if (!AuthUtil.isAdmin()) {
				crit.userId(AuthUtil.getCurrentUser().getId());
			}

			List<ScheduledJobRun> result = daoFactory.getScheduledJobDao().getJobRuns(crit);
			return ResponseEvent.response(ScheduledJobRunDetail.from(result));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}		
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ScheduledJobRunDetail> getJobRun(RequestEvent<Long> req) {
		try {
			ScheduledJobRun jobRun = daoFactory.getScheduledJobDao().getJobRun(req.getPayload());
			if (jobRun == null) {
				return ResponseEvent.userError(ScheduledJobErrorCode.JOB_RUN_NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureReadScheduledJobRights(jobRun.getScheduledJob());
			if (!AuthUtil.isAdmin() && !jobRun.getRunBy().equals(AuthUtil.getCurrentUser())) {
				return ResponseEvent.userError(ScheduledJobErrorCode.OP_NOT_ALLOWED);
			}

			return ResponseEvent.response(ScheduledJobRunDetail.from(jobRun));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<JobExportDetail> getJobResultFile(RequestEvent<Long> req) {
		try {
			ScheduledJobRun jobRun = daoFactory.getScheduledJobDao().getJobRun(req.getPayload());
			if (jobRun == null) {
				return ResponseEvent.userError(ScheduledJobErrorCode.JOB_RUN_NOT_FOUND);
			}

			User currentUser = AuthUtil.getCurrentUser();
			ScheduledJob job = jobRun.getScheduledJob();
			if (!AuthUtil.isAdmin() && // not admin
				!jobRun.getRunBy().equals(currentUser) &&  // not the user who has run the job
				!job.getRecipients().contains(currentUser)) { // not the user who is in the notif rcpts list
				return ResponseEvent.userError(ScheduledJobErrorCode.OP_NOT_ALLOWED);
			}

			String path = jobRun.getLogFilePath();
			if (StringUtils.isBlank(path)) {
				return ResponseEvent.userError(ScheduledJobErrorCode.RESULT_DATA_FILE_NOT_AVAILABLE);
			}
			
			File f = new File(path);
			if (f.exists()) {
				ScheduledJobRunDetail detail = ScheduledJobRunDetail.from(jobRun);
				return ResponseEvent.response(new JobExportDetail(detail, f));
			} else {
				return ResponseEvent.userError(ScheduledJobErrorCode.RESULT_DATA_FILE_NOT_FOUND);
			}
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			loadAllJobs();
		} catch (Exception e) {
			throw new RuntimeException("Error loading all scheduled jobs", e);
		}
	}
	
	@PlusTransactional
	private void loadAllJobs() {
		ScheduledJobListCriteria crit = new ScheduledJobListCriteria().maxResults(5000000);			
		List<ScheduledJob> jobs = daoFactory.getScheduledJobDao().getScheduledJobs(crit);
			
		for (ScheduledJob job : jobs) {
			taskMgr.schedule(job);
		}			
	}
	
	private void ensureUniqueJobName(ScheduledJob job) {
		if (daoFactory.getScheduledJobDao().getJobByName(job.getName()) != null) {
			throw OpenSpecimenException.userError(ScheduledJobErrorCode.DUP_JOB_NAME);
		}
	}

	private void sendSharedJobNotification(ScheduledJob job, Collection<User> users) {
		if (CollectionUtils.isEmpty(users)) {
			return;
		}

		Map<String, Object> props = new HashMap<>();
		props.put("job", job);
		props.put("jobId", job.getId());
		props.put("$subject", new String[] { job.getName() });
		props.put("sharedBy", AuthUtil.getCurrentUser());

		for (User user : users) {
			props.put("rcpt", user);

			String[] to = new String[] { user.getEmailAddress() };
			EmailUtil.getInstance().sendEmail("scheduled_job_shared", to, null, props);
		}
	}
}