package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolPublishEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolPublishedVersion;
import com.krishagni.catissueplus.core.biospecimen.repository.CollectionProtocolPublishEventDao;
import com.krishagni.catissueplus.core.biospecimen.repository.CpPublishEventListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class CollectionProtocolPublishEventDaoImpl extends AbstractDao<CollectionProtocolPublishEvent> implements CollectionProtocolPublishEventDao  {
	public Class<CollectionProtocolPublishEvent> getType() {
		return CollectionProtocolPublishEvent.class;
	}

	@Override
	public List<CollectionProtocolPublishEvent> getEvents(CpPublishEventListCriteria crit) {
		Criteria<CollectionProtocolPublishEvent> query = createCriteria(CollectionProtocolPublishEvent.class, "event");
		return query.join("event.cp", "cp")
			.add(query.eq("cp.id", crit.cpId()))
			.addOrder(query.desc("event.id"))
			.list(crit.startAt(), crit.maxResults());
	}

	@Override
	public void saveOrUpdate(CollectionProtocolPublishedVersion version, boolean flush) {
		getCurrentSession().saveOrUpdate(version);
		if (flush) {
			getCurrentSession().flush();
		}
	}
}
