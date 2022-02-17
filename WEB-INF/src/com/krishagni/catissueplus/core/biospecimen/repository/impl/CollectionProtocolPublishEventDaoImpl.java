package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.List;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;


import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolPublishEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolPublishedVersion;
import com.krishagni.catissueplus.core.biospecimen.repository.CollectionProtocolPublishEventDao;
import com.krishagni.catissueplus.core.biospecimen.repository.CpPublishEventListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class CollectionProtocolPublishEventDaoImpl extends AbstractDao<CollectionProtocolPublishEvent> implements CollectionProtocolPublishEventDao  {
	public Class<CollectionProtocolPublishEvent> getType() {
		return CollectionProtocolPublishEvent.class;
	}

	@Override
	public List<CollectionProtocolPublishEvent> getEvents(CpPublishEventListCriteria crit) {
		return getCurrentSession().createCriteria(CollectionProtocolPublishEvent.class, "event")
			.createAlias("event.cp", "cp")
			.add(Restrictions.eq("cp.id", crit.cpId()))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.addOrder(Order.desc("event.id"))
			.list();
	}

	@Override
	public void saveOrUpdate(CollectionProtocolPublishedVersion version, boolean flush) {
		getCurrentSession().saveOrUpdate(version);
		if (flush) {
			getCurrentSession().flush();
		}
	}
}
