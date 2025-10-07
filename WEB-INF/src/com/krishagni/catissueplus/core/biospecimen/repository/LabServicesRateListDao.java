package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Collection;
import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.LabServiceRateListCp;
import com.krishagni.catissueplus.core.biospecimen.domain.LabServicesRateList;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface LabServicesRateListDao extends Dao<LabServicesRateList> {

	List<LabServicesRateList> getRateLists(LabServicesRateListCriteria crit);

	Long getRateListsCount(LabServicesRateListCriteria crit);

	List<Long> getAssociatedCpIds(Long rateListId, Collection<Long> cpIds);

	List<LabServiceRateListCp> getRateListCps(Long rateListId, Collection<Long> cpIds);

	void saveRateListCp(LabServiceRateListCp rateListCp);

	void deleteRateListCp(LabServiceRateListCp rateListCp);

	List<Object[]> getOverlappingServiceRates(LabServicesRateList rateList);

	List<CollectionProtocol> notAllowedCps(Long rateListId, Collection<SiteCpPair> siteCps, int maxCps);
}
