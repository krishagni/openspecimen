package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.LabServiceRateListCp;
import com.krishagni.catissueplus.core.biospecimen.domain.LabServicesRateList;
import com.krishagni.catissueplus.core.biospecimen.repository.CpListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServicesRateListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServicesRateListDao;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.SubQuery;

public class LabServicesRateListDaoImpl extends AbstractDao<LabServicesRateList> implements LabServicesRateListDao {
	@Override
	public Class<LabServicesRateList> getType() {
		return LabServicesRateList.class;
	}

	@Override
	public List<LabServicesRateList> getRateLists(LabServicesRateListCriteria crit) {
		Criteria<LabServicesRateList> query = createCriteria(LabServicesRateList.class, "rateList");
		return query.add(query.in("rateList.id", getRateListsQuery(crit, query)))
			.addOrder(query.desc("rateList.startDate"))
			.list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Long getRateListsCount(LabServicesRateListCriteria crit) {
		Criteria<LabServicesRateList> query = createCriteria(LabServicesRateList.class, "rateList");
		return query.add(query.in("rateList.id", getRateListsQuery(crit, query)))
			.getCount("rateList.id");
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
	public List<LabServiceRateListCp> getRateListCps(Long rateListId, CpListCriteria cpListCrit) {
		Criteria<LabServiceRateListCp> query = getRateListCpsQuery(rateListId, cpListCrit);
		return query.addOrder(query.asc("cp.shortTitle"))
			.list(cpListCrit.startAt(), cpListCrit.maxResults());
	}

	@Override
	public Long getRateListCpsCount(Long rateListId, CpListCriteria cpListCrit) {
		Criteria<LabServiceRateListCp> query = getRateListCpsQuery(rateListId, cpListCrit);
		return query.getCount("cp.id");
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

	@Override
	public List<CollectionProtocol> notAllowedCps(Long rateListId, Collection<SiteCpPair> siteCps, int maxCps) {
		Criteria<LabServiceRateListCp> query = createCriteria(LabServiceRateListCp.class, "rateListCp")
			.join("rateListCp.rateList", "rateList")
			.join("rateListCp.cp", "cp");

		SubQuery<Long> allowedCps = BiospecimenDaoHelper.getInstance().getCpIdsFilter(query, siteCps);
		List<LabServiceRateListCp> rateListCps = query.add(query.eq("rateList.id", rateListId))
			.add(query.notIn("cp.id", allowedCps))
			.list(0, maxCps);
		return rateListCps.stream().map(LabServiceRateListCp::getCp).collect(Collectors.toList());
	}

	private static Date getFarInFutureDate() {
		Calendar cal = Calendar.getInstance();
		cal.set(9999, 11, 31, 23, 59, 59); // 9999-12-31 23:59:59
		return cal.getTime();
	}

	private SubQuery<Long> getRateListsQuery(LabServicesRateListCriteria crit, AbstractCriteria<?, ?> mainQuery) {
		SubQuery<Long> query = mainQuery.createSubQuery(LabServicesRateList.class, "rateList")
			.join("rateList.creator", "creator")
			.leftJoin("rateList.cps", "rateListCp")
			.leftJoin("rateListCp.cp", "cp");

		if (StringUtils.isNotBlank(crit.query())) {
			if (isMySQL()) {
				query.add(query.like("rateList.name", crit.query()));
			} else {
				query.add(query.ilike("rateList.name", crit.query()));
			}
		}

		if (crit.startDate() != null) {
			query.add(query.ge("rateList.startDate", crit.startDate()));
		}

		if (crit.endDate() != null) {
			query.add(query.le("rateList.endDate", crit.endDate()));
		}

		if (crit.effectiveDate() != null) {
			query.add(query.le("rateList.startDate", crit.effectiveDate()))
				.add(
					query.or(
						query.isNull("rateList.endDate"),
						query.ge("rateList.endDate", crit.effectiveDate())
					)
				);
		}

		if (crit.cpId() != null) {
			query.add(query.eq("cp.id", crit.cpId()));
		} else if (StringUtils.isNotBlank(crit.cpShortTitle())) {
			query.add(query.eq("cp.shortTitle", crit.cpShortTitle()));
		}

		if (StringUtils.isNotBlank(crit.serviceCode())) {
			query.join("rateList.serviceRates", "serviceRate")
				.join("serviceRate.service", "service")
				.add(query.eq("service.code", crit.serviceCode()));
		}

		if (CollectionUtils.isNotEmpty(crit.siteCps())) {
			query.add(
				query.or(
					query.and(query.isNull("cp.id"), query.eq("creator.id", crit.creatorId())),
					query.in("cp.id", BiospecimenDaoHelper.getInstance().getCpIdsFilter(query, crit.siteCps()))
				)
			);
		}

		applyIdsFilter(query, "rateList.id", crit.ids());
		return query;
	}

	private Criteria<LabServiceRateListCp> getRateListCpsQuery(Long rateListId, CpListCriteria cpListCrit) {
		Criteria<LabServiceRateListCp> query = createCriteria(LabServiceRateListCp.class, "rateListCp")
			.join("rateListCp.rateList", "rateList")
			.join("rateListCp.cp", "cp")
			.join("cp.principalInvestigator", "pi");

		query.add(query.eq("rateList.id", rateListId));
		if (StringUtils.isNotBlank(cpListCrit.query())) {
			query.add(
				query.or(
					query.like("cp.shortTitle", cpListCrit.query()),
					query.like("cp.title", cpListCrit.query()),
					query.like("cp.code", cpListCrit.query())
				)
			);
		}

		if (cpListCrit.piId() != null) {
			query.add(query.eq("pi.id", cpListCrit.piId()));
		}

		if (StringUtils.isNotBlank(cpListCrit.repositoryName())) {
			SubQuery<Long> cpSitesQuery = query.createSubQuery(CollectionProtocol.class, "icp")
				.join("icp.sites", "cpSite")
				.join("cpSite.site", "site");
			cpSitesQuery.add(cpSitesQuery.eq("site.name", cpListCrit.repositoryName()));
			query.add(query.in("cp.id", cpSitesQuery));
		}

		return query;
	}

	private static final Date DISTANT_FUTURE = getFarInFutureDate();

	private static final String FQN = LabServicesRateList.class.getName();

	private static final String GET_OVERLAPPING_SERVICE_RATES = FQN + ".getOverlappingServiceRates";

}
