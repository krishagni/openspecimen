package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.LabServiceRateListCp;
import com.krishagni.catissueplus.core.biospecimen.domain.LabServicesRateList;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface LabServicesRateListDao extends Dao<LabServicesRateList> {

	List<LabServicesRateList> getRateLists(LabServicesRateListCriteria crit);

	Long getRateListsCount(LabServicesRateListCriteria crit);

	// {rateListId -> {services count, cps count}
	Map<Long, Pair<Long, Long>> getRateListsStats(Collection<Long> rateListIds);

	List<Long> getAssociatedCpIds(Long rateListId, Collection<Long> cpIds);

	List<LabServiceRateListCp> getRateListCps(Long rateListId, Collection<Long> cpIds);

	List<LabServiceRateListCp> getRateListCps(Long rateListId, CpListCriteria cpListCrit);

	Long getRateListCpsCount(Long rateListId, CpListCriteria cpListCrit);

	int cloneRateListCps(Long srcRateListId, Long tgtRateListId);

	void saveRateListCp(LabServiceRateListCp rateListCp);

	void deleteRateListCp(LabServiceRateListCp rateListCp);

	int cloneRateListServices(Long srcRateListId, Long tgtRateListId);

	List<Object[]> getOverlappingServiceRates(LabServicesRateList rateList);

	List<CollectionProtocol> notAllowedCps(Long rateListId, Collection<SiteCpPair> siteCps, int maxCps);
}
