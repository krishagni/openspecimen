package com.krishagni.catissueplus.core.biospecimen.repository.impl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.Resource;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.common.repository.Junction;
import com.krishagni.catissueplus.core.common.repository.Restriction;
import com.krishagni.catissueplus.core.common.repository.SubQuery;

public class BiospecimenDaoHelper {

	private static final BiospecimenDaoHelper instance = new BiospecimenDaoHelper();

	private BiospecimenDaoHelper() {
	}

	public static BiospecimenDaoHelper getInstance() {
		return instance;
	}

	public void addSiteCpsCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit) {
		addSiteCpsCond(query, crit, true);
	}

	public void addSiteCpsCond(AbstractCriteria<?, ?> query, SpecimenListCriteria crit, boolean spmnList) {
		addSiteCpsCond(query, crit.siteCps(), crit.useMrnSites(), spmnList);
	}

	public void addSiteCpsCond(AbstractCriteria<?, ?> query, Collection<SiteCpPair> siteCps, boolean useMrnSites) {
		addSiteCpsCond(query, siteCps, useMrnSites, true);
	}

	public void addSiteCpsCond(AbstractCriteria<?, ?> query, Collection<SiteCpPair> siteCps, boolean useMrnSites, boolean spmnList) {
		if (CollectionUtils.isEmpty(siteCps)) {
			return;
		}

		if (!query.hasAlias("cp")) {
			if (!query.hasAlias("cpr")) {
				if (!query.hasAlias("visit")) {
					query.join("specimen.visit", "visit");
				}

				query.join("visit.registration", "cpr");
			}

			query.join("cpr.collectionProtocol", "cp");
		}

		query.join("cp.sites", "cpSite")
			.join("cpSite.site", "site")
			.join("cpr.participant", "participant")
			.leftJoin("participant.pmis", "pmi")
			.leftJoin("pmi.site", "mrnSite");

		Disjunction mainCond = query.disjunction();
		Map<String, Set<SiteCpPair>> siteCpsMap = SiteCpPair.segregateByResources(siteCps);
		for (Map.Entry<String, Set<SiteCpPair>> siteCpEntry : siteCpsMap.entrySet()) {
			Junction siteCpsCond = getSiteCpsCond(query, siteCpEntry.getValue(), useMrnSites);
			if (spmnList && Resource.PRIMARY_SPECIMEN.getName().equals(siteCpEntry.getKey())) {
				mainCond.add(
					query.and(
						query.eq("specimen.lineage", "New"),
						siteCpsCond.getRestriction()
					)
				);
			} else {
				mainCond.add(siteCpsCond);
			}
		}

		query.add(mainCond);
	}

	public String getSiteCpsCondAqlForCps(Collection<SiteCpPair> siteCps) {
		if (siteCps == null || siteCps.isEmpty()) {
			return StringUtils.EMPTY;
		}

		StringBuilder aql = new StringBuilder();
		for (SiteCpPair siteCp : siteCps) {
			String restriction = getAqlSiteIdRestriction("CollectionProtocol.cpSites.siteId", siteCp);
			if (aql.length() > 0) {
				aql.append(" or ");
			}

			aql.append(restriction);
		}

		if (aql.length() > 0) {
			aql.insert(0, "(").append(")");
		}

		return aql.toString();
	}

	public String getSiteCpsCondAql(Collection<SiteCpPair> siteCps, boolean useMrnSites) {
		if (CollectionUtils.isEmpty(siteCps)) {
			return StringUtils.EMPTY;
		}

		Map<String, Set<SiteCpPair>> siteCpsByResources = SiteCpPair.segregateByResources(siteCps);
		StringBuilder aql = new StringBuilder();
		for (Map.Entry<String, Set<SiteCpPair>> siteCpsEntry : siteCpsByResources.entrySet()) {
			String restriction = getSiteCpsCondAql0(siteCpsEntry.getValue(), useMrnSites);
			if (Resource.PRIMARY_SPECIMEN.getName().equals(siteCpsEntry.getKey())) {
				restriction = "(Specimen.lineage = \"New\" and " + restriction + ")";
			}

			if (aql.length() > 0) {
				aql.append(" or ");
			}

			aql.append(restriction);
		}

		if (aql.length() > 0) {
			aql.insert(0, "(").append(")");
		}

		return aql.toString();
	}

	public SubQuery<Long> getCpIdsFilter(AbstractCriteria<?, ?> query, Collection<SiteCpPair> siteCps) {
		SubQuery<Long> subQuery = query.createSubQuery(CollectionProtocol.class, "cp")
			.distinct().select("cp.id");

		boolean siteAdded = false, instAdded = false;
		Disjunction orCond = subQuery.disjunction();
		for (SiteCpPair siteCp : siteCps) {
			if (siteCp.getCpId() != null) {
				orCond.add(subQuery.eq("cp.id", siteCp.getCpId()));
			} else {
				if (!siteAdded) {
					subQuery.join("cp.sites", "cpSite")
						.join("cpSite.site", "site");
					siteAdded = true;
				}

				if (siteCp.getSiteId() != null) {
					orCond.add(subQuery.eq("site.id", siteCp.getSiteId()));
				} else {
					if (!instAdded) {
						subQuery.join("site.institute", "institute");
						instAdded = true;
					}

					orCond.add(subQuery.eq("institute.id", siteCp.getInstituteId()));
				}
			}
		}

		return subQuery.add(orCond);
	}

	private Junction getSiteCpsCond(AbstractCriteria<?, ?> query, Collection<SiteCpPair> siteCps, boolean useMrnSites) {
		Disjunction cpSitesCond = query.disjunction();
		for (SiteCpPair siteCp : siteCps) {
			Junction siteCond = query.disjunction();
			if (useMrnSites) {
				//
				// When MRNs exist, site ID should be one of the MRN site
				//
				Junction mrnSite = query.conjunction()
					.add(query.isNotEmpty("participant.pmis"))
					.add(getSiteIdRestriction(query, "mrnSite.id", siteCp));

				//
				// When no MRNs exist, site ID should be one of CP site
				//
				Junction cpSite = query.conjunction()
					.add(query.isEmpty("participant.pmis"))
					.add(getSiteIdRestriction(query, "site.id", siteCp));

				siteCond.add(mrnSite).add(cpSite);
			} else {
				//
				// Site ID should be either MRN site or CP site
				//
				siteCond
					.add(getSiteIdRestriction(query, "mrnSite.id", siteCp))
					.add(getSiteIdRestriction(query, "site.id", siteCp));
			}

			Junction cond = query.conjunction().add(siteCond);
			if (siteCp.getCpId() != null) {
				cond.add(query.eq("cp.id", siteCp.getCpId()));
			}

			cpSitesCond.add(cond);
		}

		return cpSitesCond;
	}

	private String getSiteCpsCondAql0(Collection<SiteCpPair> siteCps, boolean useMrnSites) {
		if (CollectionUtils.isEmpty(siteCps)) {
			return StringUtils.EMPTY;
		}

		Set<String> cpSitesCond = new LinkedHashSet<>(); // joined by or
		for (SiteCpPair siteCp : siteCps) {
			List<String> siteCond = new ArrayList<>(); // joined by or
			if (useMrnSites) {
				//
				// When MRNs exist, site ID should be one of the MRN site
				//
				String mrnSite =
					"Participant.medicalRecord.mrnSiteId exists and " +
					getAqlSiteIdRestriction("Participant.medicalRecord.mrnSiteId", siteCp);
				siteCond.add("(" + mrnSite + ")");

				//
				// When no MRNs exist, site ID should be one of CP site
				//
				String cpSite =
					"Participant.medicalRecord.mrnSiteId not exists and " +
					getAqlSiteIdRestriction("CollectionProtocol.cpSites.siteId", siteCp);
				siteCond.add("(" + cpSite + ")");
			} else {
				//
				// Site ID should be either MRN site or CP site
				//
				siteCond.add("(" + getAqlSiteIdRestriction("Participant.medicalRecord.mrnSiteId", siteCp) + ")");
				siteCond.add("(" + getAqlSiteIdRestriction("CollectionProtocol.cpSites.siteId", siteCp) + ")");
			}

			String cond = "(" + StringUtils.join(siteCond, " or ") + ")";
			if (siteCp.getCpId() != null) {
				cond += " and CollectionProtocol.id = " + siteCp.getCpId();
			}

			cpSitesCond.add("(" + cond + ")");
		}

		return "(" + StringUtils.join(cpSitesCond, " or ") + ")";
	}

	private Restriction getSiteIdRestriction(AbstractCriteria<?, ?> query, String property, SiteCpPair siteCp) {
		if (siteCp.getSiteId() != null) {
			return query.eq(property, siteCp.getSiteId());
		}

		SubQuery<Long> subQuery = query.createSubQuery(Site.class, "site")
			.join("site.institute", "institute");
		subQuery.add(subQuery.eq("institute.id", siteCp.getInstituteId()))
			.select("site.id");
		return query.in(property, subQuery);
	}

	private String getAqlSiteIdRestriction(String property, SiteCpPair siteCp) {
		if (siteCp.getSiteId() != null) {
			return property + " = " + siteCp.getSiteId();
		} else {
			return property + " in " + sql(String.format(INSTITUTE_SITE_IDS_SQL, siteCp.getInstituteId()));
		}
	}

	private String sql(String sql) {
		return "sql(\"" + sql + "\")";
	}

	private static final String INSTITUTE_SITE_IDS_SQL =
		"select identifier from catissue_site where institute_id = %d and activity_status != 'Disabled'";
}
