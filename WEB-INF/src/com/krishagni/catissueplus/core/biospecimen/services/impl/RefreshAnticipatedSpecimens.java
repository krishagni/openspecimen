package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.Utility;

@Configurable
public class RefreshAnticipatedSpecimens implements ScheduledTask {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun)
	throws Exception {
		String args = jobRun.getScheduledJob().getFixedArgs();
		List<String> cpIds = Utility.csvToStringList(args);
		if (cpIds.isEmpty()) {
			return;
		}

		args = cpIds.stream().map(String::trim).filter(StringUtils::isNotBlank).collect(Collectors.joining(","));
		sessionFactory.getCurrentSession()
			.createNativeQuery("call refresh_anticipated_specimens('" + args + "')")
			.executeUpdate();
	}
}