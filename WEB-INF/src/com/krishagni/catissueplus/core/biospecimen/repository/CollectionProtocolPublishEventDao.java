package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;


import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolPublishEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolPublishedVersion;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface CollectionProtocolPublishEventDao extends Dao<CollectionProtocolPublishEvent> {
	List<CollectionProtocolPublishEvent> getEvents(CpPublishEventListCriteria crit);

	void saveOrUpdate(CollectionProtocolPublishedVersion version, boolean flush);
}
