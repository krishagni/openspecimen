package com.krishagni.catissueplus.core.audit.services.impl;

import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.audit.domain.DeleteLog;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

@Configurable
public class DeleteLogUtil {
	private static DeleteLogUtil instance = new DeleteLogUtil();

	@Autowired
	private DaoFactory daoFactory;

	public static DeleteLogUtil getInstance() {
		if (instance == null || instance.daoFactory == null) {
			instance = new DeleteLogUtil();
		}

		return instance;
	}

	public void log(BaseEntity obj) {
		DeleteLog log = new DeleteLog();
		log.setEntityId(obj.getId());
		log.setEntityType(obj.getClass().getName());
		log.setDate(Calendar.getInstance().getTime());
		log.setUser(AuthUtil.getCurrentUser());
		log.setReason(obj.getOpComments());
		daoFactory.getAuditDao().saveDeleteLog(log);
	}
}
