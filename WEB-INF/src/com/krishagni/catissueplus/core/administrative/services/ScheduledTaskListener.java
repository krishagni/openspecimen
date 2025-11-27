package com.krishagni.catissueplus.core.administrative.services;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.User;

public interface ScheduledTaskListener {
	ScheduledJobRun started(ScheduledJob job, String args, User runBy);
	
	void completed(ScheduledJobRun jobRun);
	
	void failed(ScheduledJobRun jobRun, Exception e);

	void cleanup(ScheduledJobRun jobRun);
}
