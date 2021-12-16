package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;


import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.common.util.LogUtil;

public class ExternalScheduledTask implements ScheduledTask {
	private static final LogUtil logger = LogUtil.getLogger(ExternalScheduledTask.class);

	private static final String SPACE_DELIMITED_TOKENS = "\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)";

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		executeProcess(jobRun);
	}
	
	private void executeProcess(ScheduledJobRun jobRun) {
		try {			
			String[] cmdParts = jobRun.getScheduledJob().getCommand().split(SPACE_DELIMITED_TOKENS);
			String[] argParts = new String[0];
			if (StringUtils.isNotBlank(jobRun.getRtArgs())) {
				argParts = jobRun.getRtArgs().split(SPACE_DELIMITED_TOKENS);
			}
			
			List<String> cmd = new ArrayList<String>();			
			for (String cmdPart : cmdParts) {
				cmd.add(cmdPart);
			}
			
			for (String argPart : argParts) {
				cmd.add(argPart);
			}
			
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.environment().put("OS_JOB_RUN_ID", jobRun.getId().toString());
						
			Process process = pb.start();
			process.waitFor();
		} catch (Exception e) {
			logger.error("Error running scheduled job: " + jobRun.getScheduledJob().getName(), e);
			throw new RuntimeException(e);
		}
	}
}
