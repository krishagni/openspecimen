package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.LabServiceRateListCp;
import com.krishagni.catissueplus.core.biospecimen.domain.LabServicesRateList;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServicesRateListDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class LabServicesRateListDaoImpl extends AbstractDao<LabServicesRateList> implements LabServicesRateListDao {
	@Override
	public Class<LabServicesRateList> getType() {
		return LabServicesRateList.class;
	}

	@Override
	public List<Long> getAssociatedCpIds(Long rateListId, Collection<Long> cpIds) {
		Criteria<Long> query = createCriteria(LabServicesRateList.class, Long.class, "rl")
			.join("rl.cps", "rlCp")
			.join("rlCp.cp", "cp");

		return query.add(query.eq("rl.id", rateListId))
			.add(query.in("cp.id", cpIds))
			.select(query.column("cp.id"))
			.list();
	}

	@Override
	public List<LabServiceRateListCp> getRateListCps(Long rateListId, Collection<Long> cpIds) {
		Criteria<LabServiceRateListCp> query = createCriteria(LabServiceRateListCp.class, "rateListCp")
			.join("rateListCp.rateList", "rateList")
			.join("rateListCp.cp", "cp");

		return query.add(query.eq("rateList.id", rateListId))
			.add(query.in("cp.id", cpIds))
			.list();
	}

	@Override
	public void saveRateListCp(LabServiceRateListCp rateListCp) {
		getCurrentSession().saveOrUpdate(rateListCp);
	}

	@Override
	public void deleteRateListCp(LabServiceRateListCp rateListCp) {
		getCurrentSession().delete(rateListCp);
	}

	@Override
	public List<Object[]> getOverlappingServiceRates(LabServicesRateList rateList) {
		return getCurrentSession().getNamedQuery(GET_OVERLAPPING_SERVICE_RATES)
			.setParameter("rateListId", rateList.getId())
			.setParameter("startDate", rateList.getStartDate())
			.setParameter("endDate", rateList.getEndDate() != null ? rateList.getEndDate() : DISTANT_FUTURE)
			.setMaxResults(10)
			.list();
	}

	private static Date getFarInFutureDate() {
		Calendar cal = Calendar.getInstance();
		cal.set(9999, 11, 31, 23, 59, 59); // 9999-12-31 23:59:59
		return cal.getTime();
	}

	private static final Date DISTANT_FUTURE = getFarInFutureDate();

	private static final String FQN = LabServicesRateList.class.getName();

	private static final String GET_OVERLAPPING_SERVICE_RATES = FQN + ".getOverlappingServiceRates";

}
