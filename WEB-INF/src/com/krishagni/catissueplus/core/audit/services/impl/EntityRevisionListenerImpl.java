package com.krishagni.catissueplus.core.audit.services.impl;

import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionType;

import com.krishagni.catissueplus.core.audit.domain.Revision;
import com.krishagni.catissueplus.core.audit.domain.RevisionEntityRecord;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class EntityRevisionListenerImpl implements EntityTrackingRevisionListener {

	@Override
	public void newRevision(Object revisionEntity) {
		Revision revision = (Revision) revisionEntity;
		
		if (AuthUtil.getCurrentUser() != null) {
			revision.setUser(AuthUtil.getCurrentUser());
		}

		revision.setIpAddress(AuthUtil.getRemoteAddr());
	}

	@Override
	public void entityChanged(Class entityClass, String entityName, Object entityId, RevisionType revisionType, Object revisionEntity) {
		if (!(entityId instanceof Number)) {
			return;
		}

		Revision revision = (Revision) revisionEntity;

		RevisionEntityRecord record = new RevisionEntityRecord();
		record.setRevision(revision);
		record.setType(revisionType.getRepresentation());
		record.setEntityName(entityClass.getName());
		record.setEntityId(((Number) entityId).longValue());
		revision.addEntityRecord(record);
	}
}