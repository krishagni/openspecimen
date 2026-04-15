package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.SpecimenRequest;
import com.krishagni.catissueplus.core.administrative.events.SpecimenRequestSummary;
import com.krishagni.catissueplus.core.administrative.repository.SpecimenRequestDao;
import com.krishagni.catissueplus.core.administrative.repository.SpecimenRequestListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.common.repository.SubQuery;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenRequestDaoImpl extends AbstractDao<SpecimenRequest> implements SpecimenRequestDao {
	public Class<SpecimenRequest> getType() {
		return SpecimenRequest.class;
	}

	@Override
	public List<SpecimenRequestSummary> getSpecimenRequests(SpecimenRequestListCriteria crit) {
		Criteria<SpecimenRequest> query = getListQuery(crit);
		List<SpecimenRequest> requests = query.orderBy(query.desc("selectedReq.id"))
			.list(crit.startAt(), crit.maxResults());
		return SpecimenRequestSummary.from(requests);
	}

	@Override
	public Map<String, Object> getRequestIds(String key, Object value) {
		Criteria<Object[]> query = createCriteria(SpecimenRequest.class, Object[].class, "req");

		List<Object[]> rows = query.add(query.eq("req." + key, value))
			.select("req.id", "req.catalogId")
			.list();
		if (CollectionUtils.isEmpty(rows)) {
			return Collections.emptyMap();
		}

		Object[] row = rows.iterator().next();
		Map<String, Object> ids = new HashMap<>();
		ids.put("requestId", row[0]);
		ids.put("catalogId", row[1]);
		return ids;
	}

	@Override
	public void removeRequestCart(Long cartId) {
		getCurrentSession().createNamedMutationQuery(REMOVE_REQ_CART)
			.setParameter("cartId", cartId)
			.executeUpdate();
	}

	private Criteria<SpecimenRequest> getListQuery(SpecimenRequestListCriteria crit) {
		Criteria<SpecimenRequest> query = createCriteria(SpecimenRequest.class, "selectedReq")
			.leftJoin("selectedReq.cp", "ocp")
			.leftJoin("selectedReq.dp", "odp")
			.leftJoin("selectedReq.requestor", "orequestor");

		SubQuery<Long> reqIds = query.createSubQuery(SpecimenRequest.class, "req")
			.leftJoin("req.cp", "cp")
			.leftJoin("req.dp", "dp")
			.leftJoin("req.requestor", "requestor")
			.select("req.id");

		addCatalogCond(reqIds, crit);
		addCpCond(reqIds, crit);
		addDateConds(reqIds, crit);
		addStatusConds(reqIds, crit);
		addUserRestrictions(reqIds, crit);

		query.add(query.in("selectedReq.id", reqIds));
		return query;
	}

	private void addCatalogCond(AbstractCriteria<?, ?> query, SpecimenRequestListCriteria crit) {
		if (crit.catalogId() == null) {
			return;
		}

		query.add(query.eq("req.catalogId", crit.catalogId()));
	}

	private void addCpCond(AbstractCriteria<?, ?> query, SpecimenRequestListCriteria crit) {
		if (crit.cpId() == null) {
			return;
		}

		query.add(query.eq("cp.id", crit.cpId()));
	}


	private void addDateConds(AbstractCriteria<?, ?> query, SpecimenRequestListCriteria crit) {
		if (crit.fromReqDate() != null) {
			query.add(query.ge("req.dateOfRequest", Utility.chopTime(crit.fromReqDate())));
		}

		if (crit.toReqDate() != null) {
			query.add(query.le("req.dateOfRequest", Utility.getEndOfDay(crit.toReqDate())));
		}
	}

	private void addStatusConds(AbstractCriteria<?, ?> query, SpecimenRequestListCriteria crit) {
		if (StringUtils.isBlank(crit.status())) {
			return;
		}

		if (crit.status().equals("PROCESSED")) {
			//
			// processed requests are those that are approved and closed
			//
			query.add(query.eq("req.screeningStatus", SpecimenRequest.ScreeningStatus.APPROVED))
				.add(query.eq("req.activityStatus", "Closed"));
		} else {
			try {
				SpecimenRequest.ScreeningStatus status = SpecimenRequest.ScreeningStatus.valueOf(crit.status());
				if (status == SpecimenRequest.ScreeningStatus.APPROVED) {
					//
					// we need to add this condition to filter out the processed requests
					// as they are also approved requests
					//
					query.add(query.eq("req.activityStatus", "Active"));
				}

				query.add(query.eq("req.screeningStatus", status));
			} catch (Exception e) {
				throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, crit.status());
			}
		}
	}

	private void addUserRestrictions(AbstractCriteria<?, ?> query, SpecimenRequestListCriteria crit) {
		Disjunction orCond = query.disjunction();

		if (CollectionUtils.isNotEmpty(crit.sites())) {
			Set<Long> instituteIds = new HashSet<>();
			Set<Long> siteIds      = new HashSet<>();
			for (SiteCpPair site : crit.sites()) {
				if (site.getSiteId() != null) {
					siteIds.add(site.getSiteId());
				} else if (site.getInstituteId() != null) {
					instituteIds.add(site.getInstituteId());
				}
			}

			query.leftJoin("dp.distributingSites", "distSites")
				.leftJoin("distSites.site", "distSite")
				.leftJoin("distSites.institute", "distInst")
				.leftJoin("distInst.sites", "instSite");

			Disjunction instituteConds = query.disjunction();
			if (!siteIds.isEmpty()) {
				instituteConds.add(query.in("instSite.id", siteIds));
			}

			if (!instituteIds.isEmpty()) {
				instituteConds.add(query.in("distInst.id", instituteIds));
			}

			Disjunction siteConds = query.disjunction();
			if (!siteIds.isEmpty()) {
				siteConds.add(query.in("distSite.id", siteIds));
			}

			if (!instituteIds.isEmpty()) {
				SubQuery<Long> instituteSites = query.createSubQuery(Site.class, "site")
					.join("site.institute", "institute");
				instituteSites.add(instituteSites.in("institute.id", instituteIds))
					.select("site.id");
				siteConds.add(query.in("distSite.id", instituteSites));
			}

			orCond.add(
				query.and(
					query.or(
						query.and(query.isNull("distSites.site"), instituteConds.getRestriction()),
						query.and(query.isNotNull("distSites.site"), siteConds.getRestriction())
					),
					query.eq("req.screeningStatus", SpecimenRequest.ScreeningStatus.APPROVED)
				)
			);
		}

		if (crit.requestorId() != null) {
			orCond.add(query.eq("requestor.id", crit.requestorId()));
		}

		if (CollectionUtils.isNotEmpty(crit.reqMgrCatalogIds())) {
			orCond.add(query.in("req.catalogId", crit.reqMgrCatalogIds()));
		}

		if (crit.userId() != null) {
			SubQuery<Long> rmCpsQuery = query.createSubQuery(CollectionProtocol.class, "cp")
				.join("cp.reqManagers", "mug")
				.join("mug.users", "user");
			rmCpsQuery.add(rmCpsQuery.eq("user.id", crit.userId()))
					.select("cp.id");
			orCond.add(query.in("cp.id", rmCpsQuery));
		}

		query.add(orCond);
	}

	private static final String REMOVE_REQ_CART = SpecimenRequest.class.getName() + ".removeRequestCart";
}