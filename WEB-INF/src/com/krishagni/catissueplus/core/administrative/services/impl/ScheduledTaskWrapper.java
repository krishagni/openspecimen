package com.krishagni.catissueplus.core.administrative.services.impl;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTaskListener;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;

public class ScheduledTaskWrapper implements Runnable {
	private static final LogUtil logger = LogUtil.getLogger(ScheduledTaskWrapper.class);

	private ScheduledJob job;
	
	private String args;
	
	private User runBy;
	
	private ScheduledTaskListener callback;

	public ScheduledTaskWrapper(ScheduledJob job, String args, User runBy, ScheduledTaskListener callback) {
		this.job = job;
		this.args = args;
		this.runBy = runBy;
		this.callback = callback;
	}

	@Override
	public void run() {
		ScheduledJobRun jobRun = null;
		try {			
			jobRun = callback.started(job, args, runBy);
			if (jobRun == null) {
				return;
			}

			AuthUtil.setCurrentUser(runBy);
			job.newTask().doJob(jobRun);
			callback.completed(jobRun); 
		} catch (Exception e) {
			logger.error("Error running scheduled job: " + (job != null ? job.getName() : "Unknown"), e);
			callback.failed(jobRun, e);
		} finally {
			callback.cleanup(jobRun);
		}
	}
}