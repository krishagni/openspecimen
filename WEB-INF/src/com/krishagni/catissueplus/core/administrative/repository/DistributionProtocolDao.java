
package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.SpecimenReservedEvent;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderStat;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderStatListCriteria;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface DistributionProtocolDao extends Dao<DistributionProtocol> {

	DistributionProtocol getByShortTitle(String shortTitle);

	DistributionProtocol getDistributionProtocol(String title);

	List<DistributionProtocol> getDistributionProtocols(DpListCriteria criteria);

	List<DistributionProtocol> getDistributionProtocols(Collection<String> dpShortTitles);

	Long getDistributionProtocolsCount(DpListCriteria criteria);

	List<DistributionProtocol> getExpiringDps(Date fromDate, Date toDate);

	//
	// At present this is only returning count of specimens distributed by protocol
	// in future this would be extended to return other stats related to protocol
	//	
	Map<Long, Integer> getSpecimensCountByDpIds(Collection<Long> dpIds);

	List<DistributionOrderStat> getOrderStats(DistributionOrderStatListCriteria listCrit);

	Map<String, Object> getDpIds(String key, Object value);

	List<String> getNonConsentingSpecimens(Long dpId, List<Long> specimenIds, int stmtsCount);

	void saveReservedEvents(Collection<SpecimenReservedEvent> events);

	void unlinkCustomForm(Long formId);

	Map<String, Integer> getDependents(Long dpId);
}
