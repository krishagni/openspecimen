package com.krishagni.catissueplus.core.biospecimen.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenListService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;

@Configurable
public class CleanupInactivePickListsTask implements ScheduledTask {

	private static final LogUtil logger = LogUtil.getLogger(CleanupInactivePickListsTask.class);

	@Autowired
	private SpecimenListService specimenListSvc;

	@Autowired
	private DaoFactory daoFactory;

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		try {
			User sysUser = daoFactory.getUserDao().getSystemUser();
			AuthUtil.setCurrentUser(sysUser);
			specimenListSvc.deleteInactivePickLists();
		} catch (Throwable t) {
			logger.error("Encountered error when cleaning up the pick lists", t);
		} finally {
			AuthUtil.clearCurrentUser();
		}
	}
}
