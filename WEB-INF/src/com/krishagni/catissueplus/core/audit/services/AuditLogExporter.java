package com.krishagni.catissueplus.core.audit.services;

import java.io.File;
import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.audit.repository.RevisionsListCriteria;

public interface AuditLogExporter {
	File export(String baseDir, User exportedBy, Date exportedOn, List<User> users, RevisionsListCriteria criteria);
}
