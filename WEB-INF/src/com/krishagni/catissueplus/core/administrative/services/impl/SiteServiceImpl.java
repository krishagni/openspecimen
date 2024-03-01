
package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteFactory;
import com.krishagni.catissueplus.core.administrative.events.SiteDetail;
import com.krishagni.catissueplus.core.administrative.events.SiteQueryCriteria;
import com.krishagni.catissueplus.core.administrative.events.SiteSummary;
import com.krishagni.catissueplus.core.administrative.repository.SiteListCriteria;
import com.krishagni.catissueplus.core.administrative.services.SiteService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkEntityDetail;
import com.krishagni.catissueplus.core.common.events.DeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.rbac.events.SubjectRoleOpNotif;
import com.krishagni.rbac.service.RbacService;


public class SiteServiceImpl implements SiteService, ObjectAccessor, InitializingBean {
	private SiteFactory siteFactory;

	private DaoFactory daoFactory;
	
	private RbacService rbacSvc;

	private ExportService exportSvc;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setSiteFactory(SiteFactory siteFactory) {
		this.siteFactory = siteFactory;
	}
	
	public void setRbacSvc(RbacService rbacSvc) {
		this.rbacSvc = rbacSvc;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	@Override
	@PlusTransactional	
	public ResponseEvent<List<SiteSummary>> getSites(RequestEvent<SiteListCriteria> req) {
		try {
			SiteListCriteria listCrit = req.getPayload();			
			List<Site> sites = null;

			Function<SiteListCriteria, List<Site>> sitesFn = null;
			if (AuthUtil.isAdmin()) {
				sites = daoFactory.getSiteDao().getSites(listCrit);
				sitesFn = daoFactory.getSiteDao()::getSites;
			} else if (listCrit.listAll() && isAllSitesAllowed()) {
				sites = daoFactory.getSiteDao().getSites(listCrit);
				sitesFn = daoFactory.getSiteDao()::getSites;
			} else {
				sites = getAccessibleSites(listCrit);
				sitesFn = this::getAccessibleSites;
			}

			if (CollectionUtils.isEmpty(sites) && StringUtils.isNumeric(listCrit.query())) {
				// The site custom field stores site ID instead of name
				listCrit.ids(Collections.singletonList(Long.parseLong(listCrit.query())));
				sites = sitesFn.apply(listCrit.query(null));
			}

			List<SiteSummary> result = SiteSummary.from(sites);
			if (listCrit.includeStat()) {
				addSiteStats(result);
			}
			
			return ResponseEvent.response(result);
		} catch(OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch(Exception ex) {
			return ResponseEvent.serverError(ex);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getSitesCount(RequestEvent<SiteListCriteria> req) {
		return ResponseEvent.response(daoFactory.getSiteDao().getSitesCount(req.getPayload()));
	}

	@Override
	@PlusTransactional		
	public ResponseEvent<SiteDetail> getSite(RequestEvent<SiteQueryCriteria> req) {
		SiteQueryCriteria crit = req.getPayload();
		Site site = getFromAccessibleSite(crit.getId(), crit.getName());
		return ResponseEvent.response(SiteDetail.from(site));
	}

	@Override
	@PlusTransactional	
	public ResponseEvent<SiteDetail> createSite(RequestEvent<SiteDetail> req) {
		try {
			Site site = siteFactory.createSite(req.getPayload());
			AccessCtrlMgr.getInstance().ensureCreateUpdateDeleteSiteRights(site);
			
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueConstraint(site, null, ose);
			ose.checkAndThrow();
			daoFactory.getSiteDao().saveOrUpdate(site, true);
			site.addOrUpdateExtension();
			addDefaultCoordinatorRoles(site, site.getCoordinators(), "CREATE");
			return ResponseEvent.response(SiteDetail.from(site));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional		
	public ResponseEvent<SiteDetail> updateSite(RequestEvent<SiteDetail> req) {
		return updateSite(req, false);
	}

	@Override
	@PlusTransactional		
	public ResponseEvent<SiteDetail> patchSite(RequestEvent<SiteDetail> req) {
		return updateSite(req, true);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SiteDetail>> bulkUpdateSites(RequestEvent<BulkEntityDetail<SiteDetail>> req) {
		try {
			BulkEntityDetail<SiteDetail> buDetail = req.getPayload();

			List<SiteDetail> updatedSites = new ArrayList<>();
			SiteDetail detail = curateBulkUpdateFields(buDetail.getDetail());
			for (Long siteId : Utility.nullSafe(buDetail.getIds())) {
				detail.setId(siteId);
				updatedSites.add(updateSite(detail, true));
			}

			detail.setId(null);
			for (String name : Utility.nullSafe(buDetail.getNames())) {
				detail.setName(name);
				updatedSites.add(updateSite(detail, true));
			}

			return ResponseEvent.response(updatedSites);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<Long> req) {
		try {
			Site existing = daoFactory.getSiteDao().getById(req.getPayload());
			if (existing == null) {
				return ResponseEvent.userError(SiteErrorCode.NOT_FOUND);
			}
			
			return ResponseEvent.response(existing.getDependentEntities());
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional	
	public ResponseEvent<SiteDetail> deleteSite(RequestEvent<DeleteEntityOp> req) {
		try {
			DeleteEntityOp deleteOp = req.getPayload();
			Site existing = daoFactory.getSiteDao().getById(deleteOp.getId());
			if (existing == null) {
				return ResponseEvent.userError(SiteErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureCreateUpdateDeleteSiteRights(existing);
			
			removeDefaultCoordinatorRoles(existing, existing.getCoordinators(), "DELETE");
			existing.delete(deleteOp.isClose());
			return ResponseEvent.response(SiteDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public String getObjectName() {
		return Site.getEntityName();
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getSiteDao().getSiteIds(key, value);
	}

	@Override
	public String getAuditTable() {
		return "CATISSUE_SITE_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		getFromAccessibleSite(id, null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		exportSvc.registerObjectsGenerator("site", this::getSitesGenerator);
	}

	private boolean isAllSitesAllowed() {
		return AccessCtrlMgr.getInstance().canCreateUpdateParticipant() ||
			AccessCtrlMgr.getInstance().canCreateUpdateDistributionOrder() ||
			AccessCtrlMgr.getInstance().canCreateUpdateShipment();
	}

	private void addSiteStats(List<SiteSummary> sites) {
		if (CollectionUtils.isEmpty(sites)) {
			return;
		}
		
		Map<Long, SiteSummary> sitesMap = new HashMap<Long, SiteSummary>();
		for (SiteSummary site : sites) {
			sitesMap.put(site.getId(), site);
		}
		
		Map<Long, Integer> cpCountMap = daoFactory.getSiteDao().getCpCountBySite(sitesMap.keySet());
		for (Map.Entry<Long, Integer> cpCount : cpCountMap.entrySet()) {
			sitesMap.get(cpCount.getKey()).setCpCount(cpCount.getValue());
		}		
	}

	private ResponseEvent<SiteDetail> updateSite(RequestEvent<SiteDetail> req, boolean partial) {
		try {
			return ResponseEvent.response(updateSite(req.getPayload(), partial));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private SiteDetail updateSite(SiteDetail detail, boolean partial) {
		Site existing = getSite(detail.getId(), detail.getName());
		AccessCtrlMgr.getInstance().ensureCreateUpdateDeleteSiteRights(existing);

		Site site = partial ? siteFactory.createSite(existing, detail) : siteFactory.createSite(detail);
		AccessCtrlMgr.getInstance().ensureCreateUpdateDeleteSiteRights(site);

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureUniqueConstraint(site, existing, ose);
		ose.checkAndThrow();

		Set<User> addedCoordinators   = Utility.subtract(site.getCoordinators(), existing.getCoordinators());
		Set<User> removedCoordinators = Utility.subtract(existing.getCoordinators(), site.getCoordinators());
		if (Status.isClosedOrDisabledStatus(site.getActivityStatus())) {
			removedCoordinators.addAll(existing.getCoordinators());
			addedCoordinators = Collections.emptySet();
		}

		//
		// OPSMN-4614: To prevent the mismatch between the site institute and that of the removed coordinators,
		// the roles of the removed coordinators are updated before doing site update
		//
		if (!removedCoordinators.isEmpty()) {
			removeDefaultCoordinatorRoles(existing, removedCoordinators, "UPDATE");
		}

		existing.update(site);
		daoFactory.getSiteDao().saveOrUpdate(existing);
		existing.addOrUpdateExtension();

		if (!addedCoordinators.isEmpty()) {
			addDefaultCoordinatorRoles(existing, addedCoordinators, "UPDATE");
		}

		return SiteDetail.from(existing);
	}

	private Site getSite(Long siteId, String name) {
		Site site = null;
		Object key = null;

		if (siteId != null) {
			site = daoFactory.getSiteDao().getById(siteId);
			key = siteId;
		} else if (StringUtils.isNotBlank(name)) {
			site = daoFactory.getSiteDao().getSiteByName(name);
			key = name;
		}

		if (key == null) {
			throw  OpenSpecimenException.userError(SiteErrorCode.NAME_REQUIRED);
		} else if (site == null) {
			throw  OpenSpecimenException.userError(SiteErrorCode.NOT_FOUND, key);
		}

		return site;
	}

	private void addDefaultCoordinatorRoles(Site site, Collection<User> users, String siteOp) {
		SubjectRoleOpNotif notifReq = getNotifReq(site, siteOp, "ADD");
		for (User user : users) {
			notifReq.setUser(user);
			notifReq.setAdminNotifParams(new Object[] { user.getFirstName(), user.getLastName(), site.getName() });
			rbacSvc.addSubjectRole(site, null, user, getDefaultCoordinatorRoles(), notifReq);
		}
	}
	
	private void removeDefaultCoordinatorRoles(Site site, Collection<User> users, String siteOp) {
		SubjectRoleOpNotif notifReq = getNotifReq(site, siteOp, "REMOVE");
		for (User user: users) {
			notifReq.setUser(user);
			notifReq.setAdminNotifParams(new Object[]{user.getFirstName(), user.getLastName(), site.getName()});
			rbacSvc.removeSubjectRole(site, null, user, getDefaultCoordinatorRoles(), notifReq);
		}
	}

	private SubjectRoleOpNotif getNotifReq(Site site, String siteOp, String roleOp) {
		SubjectRoleOpNotif notifReq = new SubjectRoleOpNotif();
		notifReq.setEndUserOp(siteOp);
		notifReq.setAdmins(AccessCtrlMgr.getInstance().getSuperAdmins());
		notifReq.setAdminNotifMsg("site_admin_notif_role_" + roleOp.toLowerCase());
		notifReq.setSubjectNotifMsg(siteOp.equals("DELETE") ? "site_delete_user_notif": "site_user_notif_role_" + roleOp.toLowerCase());
		notifReq.setSubjectNotifParams(new Object[] { site.getName() });
		return notifReq;
	}
	
	private String[] getDefaultCoordinatorRoles() {
		return new String[] {"Administrator"};
	}

	private void ensureUniqueConstraint(Site newSite, Site existingSite, OpenSpecimenException ose) {
		if (!isUniqueName(newSite, existingSite)) {
			ose.addError(SiteErrorCode.DUP_NAME);
		}
		
		if(!isUniqueCode(newSite, existingSite)) {
			ose.addError(SiteErrorCode.DUP_CODE);
		}
	}

	private boolean isUniqueName(Site newSite, Site existingSite) {
		if (existingSite != null && newSite.getName().equals(existingSite.getName())) {
			return true;
		}
		
		Site site = daoFactory.getSiteDao().getSiteByName(newSite.getName());
		if (site != null) {
			return false; 
		} 
		
		return true;
	}
	
	private boolean isUniqueCode(Site newSite, Site existingSite) {
		if (StringUtils.isBlank(newSite.getCode()) || 
			existingSite != null && newSite.getCode().equals(existingSite.getCode())) {
			return true;
		}
		
		Site site = daoFactory.getSiteDao().getSiteByCode(newSite.getCode());
		if (site != null) {
			return false; 
		}
		
		return true;
	}
	
	private List<Site> getAccessibleSites(SiteListCriteria criteria) {
		return AccessCtrlMgr.getInstance().getAccessibleSites(criteria);
	}
	
	private Site getFromAccessibleSite(Long siteId, String siteName) {
		Site site = getSite(siteId, siteName);

		if (AuthUtil.isAdmin()) {
			return site;
		}

		if (AccessCtrlMgr.getInstance().isAccessible(site)) {
			return site;
		}


		AccessCtrlMgr.getInstance().ensureCreateShipmentRights();
		return site;
	}
	
	private SiteDetail curateBulkUpdateFields(SiteDetail input) {
		SiteDetail detail = new SiteDetail();

		if (input.isAttrModified("instituteName")) {
			detail.setInstituteName(input.getInstituteName());
		}

		if (input.isAttrModified("coordinators")) {
			detail.setCoordinators(input.getCoordinators());
		}

		if (input.isAttrModified("type")) {
			detail.setType(input.getType());
		}

		if (input.isAttrModified("activityStatus")) {
			detail.setActivityStatus(input.getActivityStatus());
		}

		//
		// TODO: need to handle custom site fields
		//

		return detail;
	}

	private Function<ExportJob, List<? extends Object>> getSitesGenerator() {
		return new Function<ExportJob, List<? extends Object>>() {
			private boolean endOfSites;

			private int startAt;

			@Override
			public List<? extends Object> apply(ExportJob job) {
				if (endOfSites) {
					return Collections.emptyList();
				}

				int maxRecs = CollectionUtils.isNotEmpty(job.getRecordIds()) ? job.getRecordIds().size() : 100;
				SiteListCriteria crit = new SiteListCriteria().ids(job.getRecordIds()).startAt(startAt).maxResults(maxRecs);
				if (!AuthUtil.isAdmin()) {
					crit.institute(AuthUtil.getCurrentUserInstitute().getName());
				}

				Collection<Site> sites = daoFactory.getSiteDao().getSites(crit);
				startAt += sites.size();
				if (sites.isEmpty() || CollectionUtils.isNotEmpty(job.getRecordIds())) {
					endOfSites = true;
				}

				return SiteDetail.from(sites);
			}
		};
	}
}
