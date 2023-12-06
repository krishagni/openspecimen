package com.krishagni.catissueplus.core.common.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.Shipment;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionProtocolErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.ScheduledJobErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.repository.SiteListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.UserListCriteria;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolSite;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpGroupErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.Operation;
import com.krishagni.catissueplus.core.common.events.Resource;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.exporter.services.impl.ExporterContextHolder;
import com.krishagni.catissueplus.core.importer.services.impl.ImporterContextHolder;
import com.krishagni.rbac.common.errors.RbacErrorCode;
import com.krishagni.rbac.domain.Subject;
import com.krishagni.rbac.domain.SubjectAccess;
import com.krishagni.rbac.domain.SubjectRole;
import com.krishagni.rbac.repository.DaoFactory;

@Configurable
public class AccessCtrlMgr {

	@Autowired
	private DaoFactory daoFactory;

	@Autowired
	private com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory bsDaoFactory;

	private static AccessCtrlMgr instance;

	private AccessCtrlMgr() {
	}

	public static AccessCtrlMgr getInstance() {
		if (instance == null || instance.daoFactory == null || instance.bsDaoFactory == null) {
			instance = new AccessCtrlMgr();
		}

		return instance;
	}

	public void ensureUserIsAdmin() {
		User user = AuthUtil.getCurrentUser();

		if (!user.isAdmin()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
		}
	}

	public void ensureUserIsAdminOrInstituteAdmin() {
		User user = AuthUtil.getCurrentUser();

		if (user == null || (!user.isAdmin() && !user.isInstituteAdmin())) {
			throw OpenSpecimenException.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          User object access control helper methods                               //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public void ensureCreateUserRights(User user) {
		ensureUserObjectRights(user, Operation.CREATE, true);
		ensureUserEximRights(user, true);
	}

	public void ensureUpdateUserRights(User user) {
		ensureUpdateUserRights(user, true);
	}

	public void ensureUpdateUserRights(User user, boolean selfUpdateAllowed) {
		ensureUserObjectRights(user, Operation.UPDATE, selfUpdateAllowed);
		ensureUserEximRights(user, selfUpdateAllowed);
	}

	public void ensureDeleteUserRights(User user) {
		ensureUserObjectRights(user, Operation.DELETE, true);
		ensureUserEximRights(user, true);
	}

	public void ensureCreateUpdateUserRolesRights(User user, Site roleSite, CollectionProtocol roleCp) {
		//
		// Constraint 1: The site on which the role is assigned should belong to the user institute
		//
		if (roleSite != null && !roleSite.getInstitute().equals(user.getInstitute())) {
			throw OpenSpecimenException.userError(
				SiteErrorCode.INVALID_SITE_INSTITUTE, roleSite.getName(), user.getInstitute().getName());
		}

		//
		// Constraint 2: The CP for which the role is assigned should be active at one of the user institute site
		//
		if (roleCp != null && roleCp.getRepositories().stream().noneMatch(site -> site.getInstitute().equals(user.getInstitute()))) {
			throw OpenSpecimenException.userError(
				CpErrorCode.NOT_OF_INST, roleCp.getShortTitle(), user.getInstitute().getName());
		}

		if (AuthUtil.isAdmin()) {
			//
			// super admin can assign any role to any site and CP combination
			//
			return;
		}

		//
		// Non-super admin users can assign roles to only their institute users
		//
		if (!user.getInstitute().equals(AuthUtil.getCurrentUserInstitute())) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
		
		if (AuthUtil.isInstituteAdmin()) {
			//
			// institute admin can assign any role to any site (constraint 1) and any CP (constraint 2) to their institute users
			//
			return;
		}


		boolean allowed = false;
		Set<SiteCpPair> currentSites = getSiteCps(Resource.USER, Operation.UPDATE, false);
		for (SiteCpPair currentSite : currentSites) {
			if (roleSite == null) {
				allowed = currentSite.getSiteId() == null && user.getInstitute().getId().equals(currentSite.getInstituteId());
			} else {
				if (currentSite.getSiteId() != null) {
					allowed = currentSite.getSiteId().equals(roleSite.getId());
				} else {
					allowed = currentSite.getInstituteId().equals(roleSite.getInstitute().getId());
				}
			}

			if (allowed) {
				if (roleCp == null) {
					allowed = currentSite.getCpId() == null;
				} else {
					if (currentSite.getCpId() != null) {
						allowed = currentSite.getCpId().equals(roleCp.getId());
					} else {
						allowed = (roleSite == null || roleCp.getRepositories().contains(roleSite));
					}
				}

				if (allowed) {
					break;
				}
			}
		}

		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		ensureUserEximRights(user, true);
	}

	public List<User> getSuperAndSiteAdmins(Site site, CollectionProtocol cp) {
		List<User> result = getSuperAdmins();
		result.addAll(getSiteAdmins(site, cp));
		return result;
	}

	public List<User> getSuperAdmins() {
		UserListCriteria crit = new UserListCriteria().activityStatus("Active").type("SUPER");
		return bsDaoFactory.getUserDao().getUsers(crit);
	}

	public List<User> getSiteAdmins(Site site, CollectionProtocol cp) {
		List<User> result = new ArrayList<>();
		if (site != null) {
			result.addAll(site.getCoordinators());
		} else if (cp != null) {
			result.addAll(cp.getSites().stream()
				.map(CollectionProtocolSite::getSite)
				.flatMap(s -> s.getCoordinators().stream())
				.collect(Collectors.toList()));
		}

		return result;
	}

	private void ensureUserObjectRights(User user, Operation op, boolean selfUpdateAllowed) {
		if (AuthUtil.isAdmin()) {
			// admin allowed to update all kinds of users
			return;
		}

		if (selfUpdateAllowed && user.equals(AuthUtil.getCurrentUser()) && op != Operation.DELETE) {
			// user is editing his/her own profile
			return;
		}

		if (user.isAdmin() && op != Operation.READ) {
			throw OpenSpecimenException.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
		}

		if (!user.isContact() && !user.getInstitute().equals(AuthUtil.getCurrentUserInstitute())) {
			// user is neither a contact user nor of the same institute
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		if (!canUserPerformOp(AuthUtil.getCurrentUser().getId(), Resource.USER, new Operation[] { op })) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private void ensureUserEximRights(User user, boolean selfUpdateAllowed) {
		if (isImportOp() || isExportOp()) {
			ensureUserObjectRights(user, Operation.EXIM, selfUpdateAllowed);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Site object access control helper methods                               //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public void ensureCreateUpdateDeleteSiteRights(Site site) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		boolean allowed = false;
		if (AuthUtil.isInstituteAdmin()) {
			allowed = AuthUtil.getCurrentUser().getInstitute().equals(site.getInstitute());
		}

		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.INST_ADMIN_RIGHTS_REQ, site.getInstitute().getName());
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Distribution Protocol object access control helper methods              //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public Set<SiteCpPair> getReadAccessDistributionProtocolSites() {
		if (AuthUtil.isAdmin()) {
			return null;
		}

		return getSiteCps(Resource.DP, Operation.READ, true);
	}

	public void ensureReadDpRights(DistributionProtocol dp) {
		ensureDpObjectRights(dp, new Operation[] {Operation.READ}, false);
	}

	public boolean hasCreateUpdateDpRights(Long dpId) {
		DistributionProtocol dp = bsDaoFactory.getDistributionProtocolDao().getById(dpId);
		if (dp == null) {
			throw OpenSpecimenException.userError(DistributionProtocolErrorCode.NOT_FOUND, dpId);
		}

		return hasDpObjectRights(dp, new Operation[] { Operation.CREATE, Operation.UPDATE }, true);
	}

	public void ensureCreateUpdateDpRights(DistributionProtocol dp) {
		ensureDpObjectRights(dp, new Operation[] {Operation.CREATE, Operation.UPDATE});
		ensureDpEximRights(dp);
	}

	public void ensureDeleteDpRights(DistributionProtocol dp) {
		ensureDpObjectRights(dp, new Operation[] {Operation.DELETE});
	}

	public boolean hasDpEximRights() {
		return hasEximRights(null, Resource.DP.getName());
	}

	private void ensureDpObjectRights(DistributionProtocol dp, Operation[] ops) {
		ensureDpObjectRights(dp, ops, true);
	}

	private void ensureDpObjectRights(DistributionProtocol dp, Operation[] ops, boolean allSites) {
		if (!hasDpObjectRights(dp, ops, allSites)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private boolean hasDpObjectRights(DistributionProtocol dp, Operation[] ops, boolean allSites) {
		if (AuthUtil.isAdmin()) {
			return true;
		}

		Set<SiteCpPair> allowedSites = getSiteCps(Resource.DP, ops);
		return isAccessAllowedOnSites(allowedSites, dp.getAllowedDistributingSites(), allSites);
	}

	private void ensureDpEximRights(DistributionProtocol dp) {
		if (isImportOp() || isExportOp()) {
			ensureDpObjectRights(dp, new Operation[] {Operation.EXIM});
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Collection Protocol object access control helper methods                //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public Set<SiteCpPair> getReadableSiteCps() {
		return getSiteCps(Resource.CP, Operation.READ, false);
	}

	public String getAllowedCpIdsSql(Collection<SiteCpPair> siteCps) {
		if (siteCps == null) {
			return "";
		}

		if (siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		List<String> conds = new ArrayList<>();
		for (SiteCpPair siteCp : siteCps) {
			if (siteCp.getCpId() != null) {
				conds.add("icp.identifier = " + siteCp.getCpId());
			} else if (siteCp.getSiteId() != null) {
				conds.add("site.identifier = " + siteCp.getSiteId());
			} else {
				conds.add("institute.identifier = " + siteCp.getInstituteId());
			}
		}

		return
			"select " +
			"  distinct icp.identifier " +
			"from " +
			"  catissue_collection_protocol icp " +
			"  inner join catissue_site_cp cp_site on cp_site.collection_protocol_id = icp.identifier " +
			"  inner join catissue_site site on site.identifier = cp_site.site_id " +
			"  inner join catissue_institution institute on institute.identifier = site.institute_id " +
			"where " +
			"  icp.activity_status != 'Disabled' and " +
			"  (" + StringUtils.join(conds, " or ") + ")";
	}

	public Set<Long> getRegisterEnabledCpIds(List<String> siteNames) {
		return getEligibleCpIds(Resource.PARTICIPANT.getName(), new String[] {Operation.CREATE.getName()}, siteNames);
	}

	public Set<Long> getReadAccessGroupCpIds(Long groupId) {
		CollectionProtocolGroup group = bsDaoFactory.getCpGroupDao().getById(groupId);
		if (group == null) {
			throw OpenSpecimenException.userError(CpGroupErrorCode.NOT_FOUND, groupId);
		}

		Set<Long> allowedCpIds = getEligibleCpIds(Resource.CP.getName(), new String[] { Operation.READ.getName() }, null);
		if (allowedCpIds == null) {
			return new HashSet<>(group.getCpIds());
		} else if (allowedCpIds.isEmpty()) {
			return Collections.emptySet();
		} else {
			return group.getCpIds().stream().filter(allowedCpIds::contains).collect(Collectors.toSet());
		}
	}

	//
	// Returns list of IDs of users who can perform "ops" on "resource" belonging
	// to collection protocol identified by "cpId"
	//
	public List<Long> getUserIds(Long cpId, Resource resource, Operation[] ops) {
		String[] opsStr = new String[ops.length];
		for (int i = 0; i < ops.length; ++i) {
			opsStr[i] = ops[i].getName();
		}

		return daoFactory.getSubjectDao().getSubjectIds(cpId, resource.getName(), opsStr);
	}

	public List<Long> getUserIds(Long instituteId, Long siteId, Resource resource, Operation[] ops) {
		String[] opsStr = Arrays.stream(ops).map(Operation::getName).toArray(String[]::new);
		return daoFactory.getSubjectDao().getSubjectIds(instituteId, siteId, resource.getName(), opsStr);
	}

	public void ensureCreateCpRights(CollectionProtocol cp) {
		ensureCpObjectRights(cp, Operation.CREATE);
	}

	public void ensureReadCpRights(Long cpId) {
		CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
		if (cp == null) {
			throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND, cpId);
		}

		ensureReadCpRights(cp);
	}

	public void ensureReadCpRights(CollectionProtocol cp) {
		ensureCpObjectRights(cp, Operation.READ);
	}

	public void ensureUpdateCpRights(Long cpId) {
		if (!hasUpdateCpRights(cpId)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	public boolean hasUpdateCpRights(Long cpId) {
		if (AuthUtil.isAdmin()) {
			return true;
		}

		CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
		if (cp == null) {
			throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND, cpId);
		}

		return hasUpdateCpRights(cp);
	}

	public void ensureUpdateCpRights(CollectionProtocol cp) {
		ensureCpObjectRights(cp, Operation.UPDATE);
	}

	public boolean hasUpdateCpRights(CollectionProtocol cp) {
		return hasCpObjectRights(cp, Operation.UPDATE);
	}

	public void ensureDeleteCpRights(CollectionProtocol cp) {
		ensureCpObjectRights(cp, Operation.DELETE);
	}

	private void ensureCpObjectRights(CollectionProtocol cp, Operation op) {
		if (!hasCpObjectRights(cp, op)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private boolean hasCpObjectRights(CollectionProtocol cp, Operation op) {
		if (AuthUtil.isAdmin()) {
			return true;
		}

		Long     userId   = AuthUtil.getCurrentUser().getId();
		String   resource = Resource.CP.getName();
		String[] ops      = { op.getName() };

		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, cp.getId(), resource, ops);
		return isAccessAllowedOnAnySite(accessList, cp.getRepositories());
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Participant object access control helper methods                        //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public static class ParticipantReadAccess {
		public boolean admin;

		public Set<SiteCpPair> phiSiteCps = new HashSet<>();

		public Set<SiteCpPair> siteCps = new HashSet<>();

		public boolean phiAccess;

		public boolean noAccessibleSites() {
			return CollectionUtils.isEmpty(siteCps);
		}
	}

	public ParticipantReadAccess getParticipantReadAccess() {
		ParticipantReadAccess result = new ParticipantReadAccess();
		result.phiAccess = true;

		if (AuthUtil.isAdmin()) {
			result.admin = true;
			return result;
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		String[] ops = {Operation.READ.getName()};
		String resource = Resource.PARTICIPANT.getName();
		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, resource, ops);

		resource = Resource.PARTICIPANT_DEID.getName();
		accessList.addAll(daoFactory.getSubjectDao().getAccessList(userId, resource, ops));

		Long instituteId = AuthUtil.getCurrentUserInstitute().getId();
		for (SubjectAccess access : accessList) {
			Long cpId = access.getCollectionProtocol() != null ? access.getCollectionProtocol().getId() : null;
			Long siteId = access.getSite() != null ? access.getSite().getId() : null;

			SiteCpPair siteCp = SiteCpPair.make(instituteId, siteId, cpId);
			result.siteCps.add(siteCp);
			if (Resource.PARTICIPANT.getName().equals(access.getResource())) {
				result.phiSiteCps.add(siteCp);
			}
		}

		result.phiAccess = !result.phiSiteCps.isEmpty();
		return result;
	}

	public ParticipantReadAccess getParticipantReadAccess(Long cpId) {
		if (cpId == null || cpId == -1L) {
			return getParticipantReadAccess();
		}

		ParticipantReadAccess result = new ParticipantReadAccess();
		result.phiAccess = true;

		if (AuthUtil.isAdmin()) {
			result.admin = true;
			return result;
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		Long instituteId = AuthUtil.getCurrentUserInstitute().getId();
		String[] ops = {Operation.READ.getName()};

		String resource = Resource.PARTICIPANT.getName();
		List<SubjectAccess> phiAccessList = daoFactory.getSubjectDao().getAccessList(userId, cpId, resource, ops);
		result.phiAccess = !phiAccessList.isEmpty();
		for (SubjectAccess access : phiAccessList) {
			Long siteId = access.getSite() != null ? access.getSite().getId() : null;
			result.siteCps.add(SiteCpPair.make(instituteId, siteId, cpId));
			result.phiSiteCps.add(SiteCpPair.make(instituteId, siteId, cpId));
		}

		resource = Resource.PARTICIPANT_DEID.getName();
		List<SubjectAccess> deidAccessList = daoFactory.getSubjectDao().getAccessList(userId, cpId, resource, ops);
		for (SubjectAccess access : deidAccessList) {
			Long siteId = access.getSite() != null ? access.getSite().getId() : null;
			result.siteCps.add(SiteCpPair.make(instituteId, siteId, cpId));
		}

		return result;
	}
	
	public boolean ensurePhiRights(CollectionProtocolRegistration cpr, Operation op) {
		if (op == Operation.CREATE || op == Operation.UPDATE) {
			return ensureCprObjectRights(cpr, Operation.UPDATE);
		} else {
			return ensureCprObjectRights(cpr, Operation.READ);
		}
	}
	
	public boolean ensureReadParticipantRights(Long participantId) {
		return ensureParticipantObjectRights(participantId, Operation.READ);
	}

	public boolean ensureReadParticipantRights(Participant participant) {
		return ensureParticipantObjectRights(participant, Operation.READ);
	}

	public boolean ensureUpdateParticipantRights(Participant participant) {
		return ensureParticipantObjectRights(participant, Operation.UPDATE);
	}

	public List<CollectionProtocolRegistration> getAccessibleCprs(Collection<CollectionProtocolRegistration> cprs) {
		return getAccessibleCprs(cprs, Operation.READ);
	}

	public List<CollectionProtocolRegistration> getAccessibleCprs(Collection<CollectionProtocolRegistration> cprs, Operation op) {
		return cprs.stream().filter(cpr -> {
				try {
					ensureCprObjectRights(cpr, op);
					return true;
				} catch (OpenSpecimenException e) {
					return false;
				}
			}
		).collect(Collectors.toList());
	}

	public boolean ensureCreateCprRights(Long cprId) {
		return ensureCprObjectRights(cprId, Operation.CREATE);
	}

	public boolean ensureCreateCprRights(CollectionProtocolRegistration cpr) {
		boolean phiAccess = ensureCprObjectRights(cpr, Operation.CREATE);
		ensureCprEximRights(cpr);
		return phiAccess;
	}

	public boolean ensureReadCprRights(Long cprId) {
		return ensureCprObjectRights(cprId, Operation.READ);
	}

	public boolean ensureReadCprRights(CollectionProtocolRegistration cpr) {
		boolean phiAccess = ensureCprObjectRights(cpr, Operation.READ);
		ensureCprEximRights(cpr);
		return phiAccess;
	}

	public void ensureUpdateCprRights(Long cprId) {
		ensureCprObjectRights(cprId, Operation.UPDATE);
	}

	public boolean ensureUpdateCprRights(CollectionProtocolRegistration cpr) {
		boolean phiAccess = ensureCprObjectRights(cpr, Operation.UPDATE);
		ensureCprEximRights(cpr);
		return phiAccess;
	}

	public void ensureDeleteCprRights(Long cprId) {
		ensureCprObjectRights(cprId, Operation.DELETE);
	}

	public boolean ensureDeleteCprRights(CollectionProtocolRegistration cpr) {
		boolean phiAccess = ensureCprObjectRights(cpr, Operation.DELETE);
		ensureCprEximRights(cpr);
		return phiAccess;
	}

	private boolean ensureCprObjectRights(Long cprId, Operation op) {
		CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(cprId);
		if (cpr == null) {
			throw OpenSpecimenException.userError(CprErrorCode.M_NOT_FOUND, cprId, 1);
		}

		boolean phiAccess = ensureCprObjectRights(cpr, op);
		ensureCprEximRights(cpr);
		return phiAccess;
	}

	private boolean ensureParticipantObjectRights(Long participantId, Operation op) {
		Participant participant = daoFactory.getParticipantDao().getById(participantId);
		return ensureParticipantObjectRights(participant, op);
	}

	private boolean ensureParticipantObjectRights(Participant p, Operation op) {
		for (CollectionProtocolRegistration cpr : p.getCprs()) {
			try {
				return ensureCprObjectRights(cpr, op);
			} catch (OpenSpecimenException ose) {

			}
		}

		throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
	}

	private boolean ensureCprObjectRights(CollectionProtocolRegistration cpr, Operation op) {
		if (AuthUtil.isAdmin()) {
			return true;
		}

		boolean phiAccess = true;
		Long cpId = cpr.getCollectionProtocol().getId();
		String resource = Resource.PARTICIPANT.getName();
		Long userId = AuthUtil.getCurrentUser().getId();
		String[] ops = {op.getName()};

		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, cpId, resource, ops);
		Set<Site> cpSites = cpr.getCollectionProtocol().getRepositories();
		boolean allowed = isAccessAllowedOnAnySite(accessList, cpSites);
		if (!allowed && op == Operation.READ) {
			phiAccess = false;
			resource = Resource.PARTICIPANT_DEID.getName();
			accessList = daoFactory.getSubjectDao().getAccessList(userId, cpId, resource, ops);
			allowed = isAccessAllowedOnAnySite(accessList, cpSites);
		}

		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		if (!isAccessRestrictedBasedOnMrn()) {
			return phiAccess;
		}

		Set<Site> mrnSites = cpr.getParticipant().getMrnSites();
		if (mrnSites.isEmpty()) {
			return phiAccess;
		}

		allowed = isAccessAllowedOnAnySite(accessList, mrnSites);
		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		return phiAccess;
	}

	private void ensureCprEximRights(CollectionProtocolRegistration cpr) {
		if (isImportOp() || isExportOp()) {
			ensureCprObjectRights(cpr, Operation.EXIM);
		}
	}

	public boolean canCreateUpdateParticipant() {
		return canUserPerformOp(Resource.PARTICIPANT, new Operation[] {Operation.CREATE, Operation.UPDATE});
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Participant consent access control helper methods                       //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////

	public boolean ensureReadConsentRights(CollectionProtocolRegistration cpr) {
		ensureConsentRights(cpr, Collections.singletonList(Operation.READ));
		ensureConsentEximRights(cpr);
		return true;
	}

	public boolean ensureUpdateConsentRights(CollectionProtocolRegistration cpr) {
		ensureConsentRights(cpr, Arrays.asList(Operation.CREATE, Operation.UPDATE));
		ensureConsentEximRights(cpr);
		return true;
	}

	public boolean ensureDeleteConsentRights(CollectionProtocolRegistration cpr) {
		ensureConsentRights(cpr, Arrays.asList(Operation.DELETE));
		ensureConsentEximRights(cpr);
		return true;
	}

	public boolean ensureLockConsentRights(CollectionProtocolRegistration cpr) {
		ensureConsentRights(cpr, Arrays.asList(Operation.LOCK));
		ensureConsentEximRights(cpr);
		return true;
	}

	public boolean ensureUnlockLockConsentRights(CollectionProtocolRegistration cpr) {
		ensureConsentRights(cpr, Arrays.asList(Operation.UNLOCK));
		ensureConsentEximRights(cpr);
		return true;
	}

	private boolean ensureConsentRights(CollectionProtocolRegistration cpr, List<Operation> ops) {
		if (AuthUtil.isAdmin()) {
			return true;
		}

		Long cpId = cpr.getCollectionProtocol().getId();
		String resource = Resource.CONSENT.getName();
		Long userId = AuthUtil.getCurrentUser().getId();
		String[] opNames = ops.stream().map(Operation::getName).toArray(String[]::new);

		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, cpId, resource, opNames);
		Set<Site> cpSites = cpr.getCollectionProtocol().getRepositories();
		boolean allowed = isAccessAllowedOnAnySite(accessList, cpSites);
		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		if (!isAccessRestrictedBasedOnMrn()) {
			return true;
		}

		Set<Site> mrnSites = cpr.getParticipant().getMrnSites();
		if (mrnSites.isEmpty()) {
			return true;
		}

		allowed = isAccessAllowedOnAnySite(accessList, mrnSites);
		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		return true;
	}

	private void ensureConsentEximRights(CollectionProtocolRegistration cpr) {
		if (isImportOp() || isExportOp()) {
			ensureConsentRights(cpr, Collections.singletonList(Operation.EXIM));
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Visit object access control helper methods                              //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public void ensureCreateOrUpdateVisitRights(Long visitId, boolean checkPhiAccess) {
		ensureVisitObjectRights(visitId, Operation.UPDATE, checkPhiAccess);
	}

	public void ensureCreateOrUpdateVisitRights(Visit visit) {
		ensureCreateOrUpdateVisitRights(visit, visit.hasPhiFields());
	}

	public void ensureCreateOrUpdateVisitRights(Visit visit, boolean checkPhiAccess) {
		ensureVisitObjectRights(visit, Operation.UPDATE, checkPhiAccess);
	}

	public boolean ensureReadVisitRights(Long visitId) {
		return ensureReadVisitRights(visitId, false);
	}

	public boolean ensureReadVisitRights(Long visitId, boolean checkPhiAccess) {
		return ensureVisitObjectRights(visitId, Operation.READ, checkPhiAccess);
	}

	public boolean ensureReadVisitRights(Visit visit) {
		return ensureReadVisitRights(visit, visit.hasPhiFields());
	}

	public boolean ensureReadVisitRights(Visit visit, boolean checkPhiAccess) {
		return ensureVisitObjectRights(visit, Operation.READ, checkPhiAccess);
	}

	public boolean ensureReadVisitRights(CollectionProtocolRegistration cpr, boolean checkPhiAccess) {
		return ensureVisitObjectRights(cpr, Operation.READ, checkPhiAccess);
	}

	public boolean hasReadVisitRights(CollectionProtocolRegistration cpr) {
		return hasVisitObjectRights(cpr, Operation.READ);
	}

	public void ensureDeleteVisitRights(Visit visit) {
		ensureVisitObjectRights(visit, Operation.DELETE, false);
	}

	public boolean hasDeleteVisitRights(CollectionProtocolRegistration cpr) {
		return hasVisitObjectRights(cpr, Operation.DELETE);
	}

	public Set<SiteCpPair> getReadVisitSiteCps(Long cpId) {
		return getSiteCps(
			new String[] { Resource.VISIT.getName() },
			cpId,
			new String[] { Operation.READ.getName() },
			false
		);
	}

	private boolean hasVisitObjectRights(CollectionProtocolRegistration cpr, Operation op) {
		try {
			ensureVisitObjectRights(cpr, op, false);
			return true;
		} catch (OpenSpecimenException ose) {
			if (!ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
				throw ose;
			}

			return false;
		}
	}

	private boolean ensureVisitObjectRights(Long visitId, Operation op, boolean checkPhiAccess) {
		Visit visit = daoFactory.getVisitDao().getById(visitId);
		if (visit == null) {
			throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND, visitId);
		}

		return ensureVisitObjectRights(visit, op, checkPhiAccess);
	}

	private boolean ensureVisitObjectRights(Visit visit, Operation op, boolean checkPhiAccess) {
		return ensureVisitObjectRights(visit.getRegistration(), op, checkPhiAccess);
	}

	private boolean ensureVisitObjectRights(CollectionProtocolRegistration cpr, Operation op, boolean checkPhiAccess) {
		Resource[] resources = {Resource.VISIT};
		boolean phiAccess = ensureVisitAndSpecimenObjectRights(cpr, null, resources, op, checkPhiAccess);
		if (isImportOp() || isExportOp()) {
			ensureVisitAndSpecimenObjectRights(cpr, null, resources, Operation.EXIM, false);
		}

		return phiAccess;
	}

	//
	// Specimen access rights
	//
	public static class SpecimenAccessRights {
		public boolean admin;

		public boolean allSpmns;

		public boolean onlyPrimarySpmns;

		public boolean phiAccess;

		public boolean allowed(Specimen specimen) {
			return admin || allSpmns || (onlyPrimarySpmns && specimen.isPrimary());
		}
	}
	
	public void ensureCreateOrUpdateSpecimenRights(Long specimenId, boolean checkPhiAccess) {
		ensureSpecimenObjectRights(specimenId, Operation.UPDATE, checkPhiAccess);
	}

	public void ensureCreateOrUpdateSpecimenRights(Specimen specimen) {
		ensureCreateOrUpdateSpecimenRights(specimen, specimen.hasPhiFields());
	}

	public void ensureCreateOrUpdateSpecimenRights(Specimen specimen, boolean checkPhiAccess) {
		Resource[] resources;
		if (specimen.isPrimary()) {
			resources = new Resource[] { Resource.PRIMARY_SPECIMEN, Resource.SPECIMEN};
		} else {
			resources = new Resource[] { Resource.SPECIMEN};
		}

		Site storageSite = specimen.isStorageSiteBasedAccessRightsEnabled() ? specimen.getStorageSite() : null;
		ensureVisitAndSpecimenObjectRights(specimen.getRegistration(), storageSite, resources, Operation.UPDATE, checkPhiAccess);
		if (isImportOp() || isExportOp()) {
			ensureVisitAndSpecimenObjectRights(specimen.getRegistration(), isImportOp() ? storageSite : null, resources, Operation.EXIM, false);
		}
	}

	public boolean ensureReadSpecimenRights(Long specimenId) {
		return ensureReadSpecimenRights(specimenId, false);
	}

	public boolean ensureReadSpecimenRights(Long specimenId, boolean checkPhiAccess) {
		return ensureSpecimenObjectRights(specimenId, Operation.READ, checkPhiAccess);
	}

	public SpecimenAccessRights ensureReadSpecimenRights(Specimen specimen) {
		return ensureReadSpecimenRights(specimen, specimen.hasPhiFields());
	}

	public SpecimenAccessRights ensureReadSpecimenRights(Specimen specimen, boolean checkPhiAccess) {
		SpecimenAccessRights rights = new SpecimenAccessRights();
		if (AuthUtil.isAdmin()) {
			rights.allSpmns = true;
			rights.admin = true;
			rights.phiAccess = true;
			return rights;
		}

		CollectionProtocolRegistration cpr = specimen.getRegistration();
		Resource[] resources;
		try {
			resources = new Resource[] { Resource.SPECIMEN};
			boolean phiRights = ensureVisitAndSpecimenObjectRights(cpr, null, resources, Operation.READ, checkPhiAccess);
			if (isImportOp() || isExportOp()) {
				ensureVisitAndSpecimenObjectRights(cpr, null, resources, Operation.EXIM, false);
			}

			rights.allSpmns = true;
			rights.phiAccess = phiRights;
		} catch (OpenSpecimenException ose) {
			if (!ose.containsError(RbacErrorCode.ACCESS_DENIED) || !specimen.isPrimary()) {
				throw ose;
			}

			resources = new Resource[] { Resource.PRIMARY_SPECIMEN};
			boolean phiRights = ensureVisitAndSpecimenObjectRights(cpr, null, resources, Operation.READ, checkPhiAccess);
			if (isImportOp() || isExportOp()) {
				ensureVisitAndSpecimenObjectRights(cpr, null, resources, Operation.EXIM, false);
			}

			rights.onlyPrimarySpmns = true;
			rights.phiAccess = phiRights;
		}

		return rights;
	}

	public boolean ensureReadSpecimenRights(CollectionProtocolRegistration cpr, boolean checkPhiAccess) {
		Resource[] resources = { Resource.SPECIMEN };
		boolean phiAccess = ensureVisitAndSpecimenObjectRights(cpr, null, resources, Operation.READ, checkPhiAccess);
		if (isImportOp() || isExportOp()) {
			ensureVisitAndSpecimenObjectRights(cpr, null, resources, Operation.EXIM, false);
		}

		return phiAccess;
	}

	public boolean ensureReadPrimarySpecimenRights(CollectionProtocolRegistration cpr, boolean checkPhiAccess) {
		Resource[] resources = { Resource.PRIMARY_SPECIMEN};
		boolean phiAccess = ensureVisitAndSpecimenObjectRights(cpr, null, resources, Operation.READ, checkPhiAccess);
		if (isImportOp() || isExportOp()) {
			ensureVisitAndSpecimenObjectRights(cpr, null, resources, Operation.EXIM, false);
		}

		return phiAccess;
	}

	public void ensureDeleteSpecimenRights(Specimen specimen) {
		Site storageSite = specimen.isStorageSiteBasedAccessRightsEnabled() ? specimen.getStorageSite() : null;
		ensureSpecimenObjectRights(specimen, storageSite, Operation.DELETE, false);
	}

	public List<SiteCpPair> getReadAccessSpecimenSiteCps() {
		return getReadAccessSpecimenSiteCps(null);
	}

	public List<SiteCpPair> getReadAccessSpecimenSiteCps(Long cpId) {
		return getReadAccessSpecimenSiteCps(cpId, true);
	}

	public List<SiteCpPair> getReadAccessSpecimenSiteCps(Long cpId, boolean addOrderSites) {
		if (AuthUtil.isAdmin()) {
			return null;
		}

		String[] ops = {Operation.READ.getName()};
		List<SiteCpPair> siteCpPairs = new ArrayList<>(getVisitAndSpecimenSiteCps(cpId, ops));
		if (addOrderSites) {
			Set<SiteCpPair> orderSiteCps = getDistributionOrderSiteCps(ops);
			for (SiteCpPair orderSiteCp : orderSiteCps) {
				orderSiteCp = orderSiteCp.copy();
				orderSiteCp.setResource(Resource.SPECIMEN.getName());
				siteCpPairs.add(orderSiteCp);

				orderSiteCp = orderSiteCp.copy();
				orderSiteCp.setResource(Resource.PRIMARY_SPECIMEN.getName());
				siteCpPairs.add(orderSiteCp);
			}

			siteCpPairs = deDupSiteCpPairs(siteCpPairs);
		}

		return siteCpPairs;
	}

	private boolean ensureSpecimenObjectRights(Long specimenId, Operation op, boolean checkPhiAccess) {
		Specimen specimen = daoFactory.getSpecimenDao().getById(specimenId);
		if (specimen == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, specimenId);
		}

		Site storageSite = null;
		if (op == Operation.CREATE || op == Operation.UPDATE || op == Operation.DELETE) {
			storageSite = specimen.isStorageSiteBasedAccessRightsEnabled() ? specimen.getStorageSite() : null;
		}

		return ensureSpecimenObjectRights(specimen, storageSite, op, checkPhiAccess);
	}

	private boolean ensureSpecimenObjectRights(Specimen spmn, Site storageSite, Operation op, boolean checkPhiAccess) {
		Resource[] resources = null;
		if (spmn.isPrimary()) {
			resources = new Resource[]{Resource.SPECIMEN, Resource.PRIMARY_SPECIMEN};
		} else {
			resources = new Resource[]{Resource.SPECIMEN};
		}

		boolean phiAccess =  ensureVisitAndSpecimenObjectRights(spmn.getRegistration(), storageSite, resources, op, checkPhiAccess);
		if (isImportOp() || isExportOp()) {
			ensureVisitAndSpecimenObjectRights(spmn.getRegistration(), isImportOp() ? storageSite : null, resources, Operation.EXIM, false);
		}

		return phiAccess;
	}

	private boolean ensureVisitAndSpecimenObjectRights(CollectionProtocolRegistration cpr, Site storageSite, Resource[] resources, Operation op, boolean checkPhiAccess) {
		if (AuthUtil.isAdmin()) {
			return true;
		}

		String[] ops = null;
		if (op == Operation.CREATE || op == Operation.UPDATE) {
			ops = new String[] {Operation.CREATE.getName(), Operation.UPDATE.getName()};
		} else {
			ops = new String[] {op.getName()};
		}

		ensureVisitAndSpecimenObjectRights(cpr, storageSite, resources, ops);
		return checkPhiAccess && ensurePhiRights(cpr, op);
	}

	private Set<SiteCpPair> getVisitAndSpecimenSiteCps(Long cpId, String[] ops) {
		return getSiteCps(
			new String[] { Resource.SPECIMEN.getName(), Resource.PRIMARY_SPECIMEN.getName() },
			cpId,
			ops,
			false
		);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//         Container type object access control helper methods                      //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public void ensureReadContainerTypeRights() {
		if (!canUserPerformOp(Resource.STORAGE_CONTAINER, new Operation[] {Operation.READ})) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		} 
	}

	public void ensureCreateOrUpdateContainerTypeRights() {
		if (AuthUtil.isAdmin() || AuthUtil.isInstituteAdmin()) {
			return;
		}

		throw OpenSpecimenException.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Storage container object access control helper methods                  //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public Set<SiteCpPair> getReadAccessContainerSiteCps() {
		return getReadAccessContainerSiteCps(null);
	}

	public Set<SiteCpPair> getReadAccessContainerSiteCps(Long cpId) {
		if (AuthUtil.isAdmin()) {
			return null;
		}

		String[] ops = {Operation.READ.getName()};
		return getSiteCps(Resource.STORAGE_CONTAINER.getName(), cpId, ops);
	}

	public void ensureCreateContainerRights(StorageContainer container) {
		ensureStorageContainerObjectRights(container, Operation.CREATE);
		ensureStorageContainerEximRights(container);
	}

	public void ensureReadContainerRights(StorageContainer container) {
		ensureStorageContainerObjectRights(container, Operation.READ);
		ensureStorageContainerEximRights(container);
	}

	public void ensureSpecimenStoreRights(StorageContainer container) {
		ensureSpecimenStoreRights(container, null);
	}

	public void ensureSpecimenStoreRights(StorageContainer container, OpenSpecimenException result) {
		try {
			ensureStorageContainerObjectRights(container, Operation.READ);
		} catch (OpenSpecimenException ose) {
			boolean throwError = false;
			if (result == null) {
				result = new OpenSpecimenException(ErrorType.USER_ERROR);
				throwError = true;
			}

			if (ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
				result.addError(SpecimenErrorCode.CONTAINER_ACCESS_DENIED, container.getName());
			} else {
				result.addErrors(ose.getErrors());
			}

			if (throwError) {
				throw result;
			}
		}
	}

	public void ensureUpdateContainerRights(StorageContainer container) {
		ensureStorageContainerObjectRights(container, Operation.UPDATE);
		ensureStorageContainerEximRights(container);
	}

	public void ensureDeleteContainerRights(StorageContainer container) {
		ensureStorageContainerObjectRights(container, Operation.DELETE);
		ensureStorageContainerEximRights(container);
	}

	private void ensureStorageContainerObjectRights(StorageContainer container, Operation op) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		String resource = Resource.STORAGE_CONTAINER.getName();
		String[] ops = {op.getName()};

		boolean allowed = false;
		Set<CollectionProtocol> allowedCps = container.getCompAllowedCps();
		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, resource, ops);
		for (SubjectAccess access : accessList) {
			if (isAccessAllowedOnSite(access.getSite(), container.getSite())) {
				CollectionProtocol accessCp = access.getCollectionProtocol();
				allowed = (accessCp == null || CollectionUtils.isEmpty(allowedCps) || allowedCps.contains(accessCp));
			}

			if (allowed) {
				break;
			}
		}

		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private void ensureStorageContainerEximRights(StorageContainer container) {
		if (isImportOp() || isExportOp()) {
			ensureStorageContainerObjectRights(container, Operation.EXIM);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Distribution order access control helper methods                        //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	
	public Set<SiteCpPair> getReadAccessDistributionOrderSites() {
		return getSiteCps(Resource.ORDER, Operation.READ);
	}
	
	public boolean canCreateUpdateDistributionOrder() {
		return canUserPerformOp(Resource.ORDER, new Operation[] {Operation.CREATE, Operation.UPDATE});
	}
	
	public Set<SiteCpPair> getCreateUpdateAccessDistributionOrderSites() {
		if (AuthUtil.isAdmin()) {
			return null;
		}
		
		return getSiteCps(Resource.ORDER, new Operation[] {Operation.CREATE, Operation.UPDATE});
	}
	
	@SuppressWarnings("unchecked")
	public Set<SiteCpPair> getDistributionOrderAllowedSites(DistributionProtocol dp) {
		Set<SiteCpPair> dpSites = dp.getAllowedDistributingSites(Resource.ORDER.getName());

		if (AuthUtil.isAdmin()) {
			return dpSites;
		} else {
			Set<SiteCpPair> userAllowedSites = getSiteCps(Resource.ORDER, new Operation[] { Operation.CREATE, Operation.UPDATE });
			return userAllowedSites.stream().filter(allowedSite -> SiteCpPair.contains(dpSites, allowedSite)).collect(Collectors.toSet());
		}
	}

	public void ensureCreateDistributionOrderRights(DistributionOrder order) {
		ensureDistributionOrderObjectRights(order, Operation.CREATE);
		ensureDistributionOrderEximRights(order);
	}

	public void ensureCreateDistributionOrderRights(DistributionProtocol dp) {
		ensureDistributionOrderObjectRights(dp, Operation.CREATE);
	}

	public void ensureReadDistributionOrderRights(DistributionOrder order) {
		ensureDistributionOrderObjectRights(order, Operation.READ);
		ensureDistributionOrderEximRights(order);
	}

	public void ensureUpdateDistributionOrderRights(DistributionOrder order) {
		ensureDistributionOrderObjectRights(order, Operation.UPDATE);
		ensureDistributionOrderEximRights(order);
	}

	public void ensureDeleteDistributionOrderRights(DistributionOrder order) {
		ensureDistributionOrderObjectRights(order, Operation.DELETE);
		ensureDistributionOrderEximRights(order);
	}

	private void ensureDistributionOrderEximRights(DistributionOrder order) {
		if (isImportOp() || isExportOp()) {
			ensureDistributionOrderObjectRights(order, Operation.EXIM);
		}
	}
	
	private void ensureDistributionOrderObjectRights(DistributionOrder order, Operation operation) {
		ensureDistributionOrderObjectRights(order.getDistributionProtocol(), operation);
	}

	private void ensureDistributionOrderObjectRights(DistributionProtocol dp, Operation op) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		Set<SiteCpPair> allowedSites = getSiteCps(Resource.ORDER, op);
		if (!SiteCpPair.contains(allowedSites, dp.getAllowedDistributingSites(Resource.ORDER.getName()))) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private Set<SiteCpPair> getDistributionOrderSiteCps(String[] ops) {
		Long userId = AuthUtil.getCurrentUser().getId();
		Long instituteId = AuthUtil.getCurrentUserInstitute().getId();

		String resource = Resource.ORDER.getName();
		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, resource, ops);

		Set<SiteCpPair> siteCps = new HashSet<>();
		for (SubjectAccess access : accessList) {
			Long siteId = access.getSite() != null ? access.getSite().getId() : null;
			siteCps.add(SiteCpPair.make(instituteId, siteId, null));
		}

		return siteCps;
	}

	public List<Site> getAccessibleSites(SiteListCriteria crit) {
		User user = AuthUtil.getCurrentUser();
		if (user.isAdmin()) {
			return daoFactory.getSiteDao().getSites(crit);
		} else if (user.isInstituteAdmin()) {
			return daoFactory.getSiteDao().getSites(crit.institute(user.getInstitute().getName()));
		}

		boolean allSites = false;
		Set<Site> results = new HashSet<>();
		if (StringUtils.isNotBlank(crit.resource()) && StringUtils.isNotBlank(crit.operation())) {
			Resource resource = Resource.fromName(crit.resource());
			if (resource == null) {
				throw OpenSpecimenException.userError(RbacErrorCode.RESOURCE_NOT_FOUND);
			}

			Operation operation = Operation.fromName(crit.operation());
			if (operation == null) {
				throw OpenSpecimenException.userError(RbacErrorCode.OPERATION_NOT_FOUND);
			}

			List<SubjectAccess> accessList = daoFactory.getSubjectDao()
				.getAccessList(user.getId(), crit.resource(), new String[] { crit.operation() });
			for (SubjectAccess access : accessList) {
				if (access.getSite() == null) {
					allSites = true;
					break;
				}

				results.add(access.getSite());
			}
		} else {
			Subject subject = daoFactory.getSubjectDao().getById(user.getId());
			for (SubjectRole role : subject.getRoles()) {
				if (role.getSite() == null) {
					allSites = true;
					break;
				}

				results.add(role.getSite());
			}
		}

		if (allSites) {
			results.clear();
			return daoFactory.getSiteDao().getSites(crit.institute(user.getInstitute().getName()));
		}

		boolean noIds = CollectionUtils.isEmpty(crit.ids());
		boolean noIncTypes = CollectionUtils.isEmpty(crit.includeTypes());
		boolean noExlTypes = CollectionUtils.isEmpty(crit.excludeTypes());
		boolean noSearchTerm = StringUtils.isBlank(crit.query());
		boolean noInstitute = StringUtils.isBlank(crit.institute());
		return results.stream()
			.filter(site -> noIds || crit.ids().contains(site.getId()))
			.filter(site -> noIncTypes || crit.includeTypes().contains(site.getType().getValue()))
			.filter(site -> noExlTypes || !crit.excludeTypes().contains(site.getType().getValue()))
			.filter(site -> noSearchTerm || StringUtils.containsIgnoreCase(site.getName(), crit.query()))
			.filter(site -> noInstitute || StringUtils.equalsIgnoreCase(site.getInstitute().getName(), crit.institute()))
			.sorted(Comparator.comparing(Site::getName))
			.collect(Collectors.toList());
	}

	public boolean isAccessible(Site site) {
		Subject subject = daoFactory.getSubjectDao().getById(AuthUtil.getCurrentUser().getId());
		for (SubjectRole role : subject.getRoles()) {
			if (site.equals(role.getSite())) {
				return true;
			}

			if (role.getSite() == null) {
				return site.getInstitute().equals(AuthUtil.getCurrentUserInstitute());
			}
		}

		return false;
	}

	public Set<Long> getEligibleCpIds(String resource, String op, List<String> siteNames) {
		return getEligibleCpIds(resource, new String[] {op}, siteNames);
	}

	public Set<Long> getEligibleCpIds(String resource, String[] ops, List<String> siteNames) {
		if (AuthUtil.isAdmin()) {
			return null;
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		List<SubjectAccess> accessList;
		if (CollectionUtils.isEmpty(siteNames)) {
			accessList = daoFactory.getSubjectDao().getAccessList(userId, resource, ops);
		} else {
			accessList = daoFactory.getSubjectDao().getAccessList(userId, resource, ops, siteNames.toArray(new String[0]));
		}

		Set<Long> cpIds = new HashSet<>();
		Set<Long> cpOfInstitutes = new HashSet<>();
		Set<Long> cpOfSites = new HashSet<>();
		for (SubjectAccess access : accessList) {
			if (access.getSite() != null && access.getCollectionProtocol() != null) {
				cpIds.add(access.getCollectionProtocol().getId());
			} else if (access.getSite() != null) {
				cpOfSites.add(access.getSite().getId());
			} else if (access.getCollectionProtocol() != null) {
				cpIds.add(access.getCollectionProtocol().getId());
			} else  {
				cpOfInstitutes.add(AuthUtil.getCurrentUserInstitute().getId());
			}
		}

		if (!cpOfInstitutes.isEmpty() || !cpOfSites.isEmpty()) {
			cpIds.addAll(daoFactory.getCollectionProtocolDao().getCpIdsBySiteIds(cpOfInstitutes, cpOfSites, siteNames));
		}

		return cpIds;
	}

	public List<Long> getSiteCpIds(Long siteId) {
		return daoFactory.getCollectionProtocolDao().getCpIdsBySiteIds(null, Collections.singleton(siteId), null);
	}

	public List<Long> getInstituteCpIds(Long instituteId) {
		return daoFactory.getCollectionProtocolDao().getCpIdsBySiteIds(Collections.singleton(instituteId), null, null);
	}

	private boolean canUserPerformOp(Resource resource, Operation[] ops) {
		return canUserPerformOp(AuthUtil.getCurrentUser().getId(), resource, ops);
	}

	private boolean canUserPerformOp(Long userId, Resource resource, Operation[] operations) {
		if (AuthUtil.isAdmin()) {
			return true;
		}

		String[] ops = Arrays.stream(operations).map(Operation::getName).toArray(String[]::new);
		return daoFactory.getSubjectDao().canUserPerformOps(userId, resource.getName(), ops);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Surgical pathology report access control helper methods                 //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public void ensureCreateOrUpdateSprRights(Visit visit) {
		ensureSprObjectRights(visit, Operation.UPDATE);
	}

	public void ensureDeleteSprRights(Visit visit) {
		ensureSprObjectRights(visit, Operation.DELETE);
	}

	public void ensureReadSprRights(Visit visit) {
		ensureSprObjectRights(visit, Operation.READ);
	}

	public void ensureLockSprRights(Visit visit) {
		ensureSprObjectRights(visit, Operation.LOCK);
	}

	public void ensureUnlockSprRights(Visit visit) {
		ensureSprObjectRights(visit, Operation.UNLOCK);
	}

	private void ensureSprObjectRights(Visit visit, Operation op) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		if (op == Operation.LOCK || op == Operation.UNLOCK) {
			ensureVisitObjectRights(visit, Operation.UPDATE, false);
		} else {
			ensureVisitObjectRights(visit, op, false);
		}

		CollectionProtocolRegistration cpr = visit.getRegistration();
		String[] ops = {op.getName()};
		ensureVisitAndSpecimenObjectRights(cpr, null, new Resource[] { Resource.SURGICAL_PATHOLOGY_REPORT }, ops);
		ensureSprEximRights(visit);
	}

	private void ensureVisitAndSpecimenObjectRights(CollectionProtocolRegistration cpr, Site storageSite, Resource[] resources, String[] ops) {
		if (AuthUtil.getCurrentUser() == null) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		Long cpId = cpr.getCollectionProtocol().getId();
		String[] resourceNames = Stream.of(resources).map(Resource::getName).toArray(String[]::new);
		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, cpId, resourceNames, ops);
		if (storageSite != null && !isAccessAllowedOnAnySite(accessList, Collections.singleton(storageSite))) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		Set<Site> cpSites = cpr.getCollectionProtocol().getRepositories();
		if (!isAccessAllowedOnAnySite(accessList, cpSites)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		if (!isAccessRestrictedBasedOnMrn()) {
			return;
		}

		Set<Site> mrnSites = cpr.getParticipant().getMrnSites();
		if (mrnSites.isEmpty()) {
			return;
		}

		if (!isAccessAllowedOnAnySite(accessList, mrnSites)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private void ensureSprEximRights(Visit visit) {
		if (isImportOp() || isExportOp()) {
			String[] ops = {Operation.EXIM.getName()};
			ensureVisitAndSpecimenObjectRights(visit.getRegistration(), null, new Resource[] { Resource.SURGICAL_PATHOLOGY_REPORT }, ops);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Scheduled Job object access control helper methods                      //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public void ensureReadScheduledJobRights() {
		Operation[] ops = {Operation.READ};
		ensureScheduledJobRights(ops);
	}

	public void ensureReadScheduledJobRights(ScheduledJob job) {
		ensureReadScheduledJobRights();
		ensureUserAccessibleJob(job);
	}

	public void ensureRunJobRights(ScheduledJob job) {
		Operation[] ops = {Operation.READ};
		ensureScheduledJobRights(ops);
		ensureUserAccessibleJob(job);
	}

	public void ensureCreateScheduledJobRights() {
		Operation[] ops = {Operation.CREATE};
		ensureScheduledJobRights(ops);
	}

	public void ensureUpdateScheduledJobRights(ScheduledJob job) {
		Operation[] ops = {Operation.UPDATE};
		ensureScheduledJobRights(ops);
		ensureUserAccessibleJob(job, false);
	}

	public void ensureDeleteScheduledJobRights(ScheduledJob job) {
		Operation[] ops = {Operation.DELETE};
		ensureScheduledJobRights(ops);
		ensureUserAccessibleJob(job, false);
	}

	public void ensureScheduledJobRights(Operation[] ops) {
		if (!canUserPerformOp(Resource.SCHEDULED_JOB, ops)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}
	
	private void ensureUserAccessibleJob(ScheduledJob job) {
		ensureUserAccessibleJob(job, true);
	}

	private void ensureUserAccessibleJob(ScheduledJob job, boolean allowedShared) {
		User currUser = AuthUtil.getCurrentUser();
		if (currUser.isAdmin() || job.getCreatedBy().equals(currUser)) {
			return;
		}

		if (!allowedShared || !job.getSharedWith().contains(currUser)) {
			throw OpenSpecimenException.userError(ScheduledJobErrorCode.OP_NOT_ALLOWED);
		}
	}

	///////////////////////////////////////////////////////////////////////
	// Site based access checks
	///////////////////////////////////////////////////////////////////////

	public Boolean isAccessRestrictedBasedOnMrn() {
		return ConfigUtil.getInstance().getBoolSetting(
			ConfigParams.MODULE,
			ConfigParams.MRN_RESTRICTION_ENABLED,
			false);
	}

	///////////////////////////////////////////////////////////////////////
	//                                                                   //
	//        Shipping and Tracking access control helper methods        //
	//                                                                   //
	///////////////////////////////////////////////////////////////////////
	public Set<SiteCpPair> getReadAccessShipmentSites() {
		return getSiteCps(Resource.SHIPPING_N_TRACKING, Operation.READ);
	}

	public boolean canCreateUpdateShipment() {
		return canUserPerformOp(Resource.SHIPPING_N_TRACKING, new Operation[] {Operation.CREATE, Operation.UPDATE});
	}
	
	public void ensureReadShipmentRights(Shipment shipment) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		if (shipment.isRequest() && shipment.isPending()) {
			//
			// A draft shipment request can be accessed only by its requester
			//
			if (Objects.equals(AuthUtil.getCurrentUser(), shipment.getRequester())) {
				return;
			}

			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		Set<SiteCpPair> allowedSites = getReadAccessShipmentSites();
		if (!isAccessAllowedOnSite(allowedSites, shipment.getSendingSite())) {
			if (shipment.isPending() || !isAccessAllowedOnSite(allowedSites, shipment.getReceivingSite())) {
				throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}
		}

		ensureShipmentEximRights();
	}

	public void ensureCreateShipmentRights() {
		if (AuthUtil.isAdmin()) {
			return;
		}

		Set<SiteCpPair> allowedSites = getSiteCps(Resource.SHIPPING_N_TRACKING, Operation.CREATE);
		if (allowedSites.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		ensureShipmentEximRights();
	}

	public void ensureCreateShipmentRights(Shipment shipment) {
		ensureShipmentRights(shipment, Operation.CREATE);
	}


	public void ensureUpdateShipmentRights(Shipment shipment) {
		ensureShipmentRights(shipment, Operation.UPDATE);
	}

	public void ensureDeleteShipmentRights(Shipment shipment) {
		ensureShipmentRights(shipment, Operation.DELETE);
	}

	private void ensureShipmentRights(Shipment shipment, Operation op) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		if (shipment.isRequest() && shipment.isPending()) {
			//
			// A draft shipment request can be accessed only by its requester
			//
			if (Objects.equals(AuthUtil.getCurrentUser(), shipment.getRequester())) {
				return;
			}

			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		boolean allowed = false;
		Set<SiteCpPair> allowedSites = getSiteCps(Resource.SHIPPING_N_TRACKING, op);
		if (!shipment.isReceived() && isAccessAllowedOnSite(allowedSites, shipment.getSendingSite())) {
			allowed = true; // sender can update/delete
		}

		if (!allowed && shipment.isReceived() && isAccessAllowedOnSite(allowedSites, shipment.getReceivingSite())) {
			allowed = true; // receiver can update / delete
		}

		if (!allowed && shipment.isRequest() && shipment.isRequested() && isAccessAllowedOnSite(allowedSites, shipment.getReceivingSite())) {
			allowed = true; // requester/receiver can modify submitted requests
		}

		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		ensureShipmentEximRights();
	}

	private void ensureShipmentEximRights() {
		if ((isImportOp() || isExportOp()) && getSiteCps(Resource.SHIPPING_N_TRACKING, Operation.EXIM).isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	///////////////////////////////////////////////////////////////////////
	//                                                                   //
	//	Custom form access control helper methods                        //
	//                                                                   //
	///////////////////////////////////////////////////////////////////////
	public void ensureFormUpdateRights() {
		User user = AuthUtil.getCurrentUser();
		if (!user.isAdmin() && !user.canManageForms()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	///////////////////////////////////////////////////////////////////////
	//                                                                   //
	// Query access control methods                                      //
	//                                                                   //
	///////////////////////////////////////////////////////////////////////
	public void ensureReadQueryRights() {
		ensureQueryRights(new Operation[] { Operation.READ });
		ensureQueryEximRights();
	}

	public void ensureCreateQueryRights() {
		ensureQueryRights(new Operation[] { Operation.CREATE });
	}

	public void ensureUpdateQueryRights() {
		ensureQueryRights(new Operation[] { Operation.CREATE, Operation.UPDATE });
	}

	public void ensureDeleteQueryRights() {
		ensureQueryRights(new Operation[] { Operation.DELETE });
	}

	private void ensureQueryEximRights() {
		if (isExportOp()) {
			ensureQueryRights(new Operation[] { Operation.EXIM });
		}
	}

	private void ensureQueryRights(Operation[] op) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		Set<SiteCpPair> siteCps = getSiteCps(Resource.QUERY, op);
		if (CollectionUtils.isEmpty(siteCps)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	///////////////////////////////////////////////////////////////////////
	//                                                                   //
	//	EXIM access control helper methods                        //
	//                                                                   //
	///////////////////////////////////////////////////////////////////////
	public boolean hasCprEximRights(Long cpId) {
		boolean allowed = hasEximRights(cpId, Resource.PARTICIPANT.getName());
		if (!allowed) {
			allowed = hasEximRights(cpId, Resource.PARTICIPANT_DEID.getName());
		}

		return allowed;
	}

	public boolean hasVisitEximRights(Long cpId) {
		return hasEximRights(cpId, Resource.VISIT.getName());
	}

	public boolean hasSpecimenEximRights(Long cpId) {
		return hasEximRights(cpId, Resource.SPECIMEN.getName()) ||
			hasEximRights(cpId, Resource.PRIMARY_SPECIMEN.getName());
	}

	public boolean hasStorageContainerEximRights() {
		return hasEximRights(null, Resource.STORAGE_CONTAINER.getName());
	}

	public boolean hasUserEximRights() {
		return hasEximRights(null, Resource.USER.getName());
	}

	public boolean hasEximRights(Long cpId, String resource) {
		if (AuthUtil.isAdmin()) {
			return true;
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		String[] ops = {Operation.EXIM.getName()};

		List<SubjectAccess> acl;
		if (cpId != null && cpId != -1L) {
			acl = daoFactory.getSubjectDao().getAccessList(userId, cpId, resource, ops);
		} else {
			acl = daoFactory.getSubjectDao().getAccessList(userId, resource, ops);
		}

		return CollectionUtils.isNotEmpty(acl);
	}

	///////////////////////////////////////////////////////////////////////
	//                                                                   //
	// Utility methods                                                   //
	//                                                                   //
	///////////////////////////////////////////////////////////////////////
	public boolean isAccessAllowed(Collection<SiteCpPair> siteCps, CollectionProtocol cp) {
		return siteCps.stream().anyMatch(siteCp -> isAccessAllowed(siteCp, cp));
	}

	public boolean isAccessAllowed(SiteCpPair siteCp, CollectionProtocol cp) {
		if (siteCp.getCpId() != null) {
			return cp.getId().equals(siteCp.getCpId());
		} else if (siteCp.getSiteId() != null) {
			return cp.getRepositories().stream().anyMatch(site -> site.getId().equals(siteCp.getSiteId()));
		} else {
			return cp.getRepositories().stream().anyMatch(site -> site.getInstitute().getId().equals(siteCp.getInstituteId()));
		}
	}

	public Set<SiteCpPair> getSiteCps(Resource resource, Operation op) {
		return getSiteCps(resource.getName(), null, new String[] { op.getName() }, true);
	}

	public Set<SiteCpPair> getSiteCps(Resource resource, Operation[] ops) {
		String[] opNames = Arrays.stream(ops).map(Operation::getName).toArray(String[]::new);
		return getSiteCps(resource.getName(), null, opNames, true);
	}

	public Set<SiteCpPair> getSiteCps(Resource resource, Operation op, boolean excludeCps) {
		return getSiteCps(resource.getName(), null, new String[] { op.getName() }, excludeCps);
	}

	public Set<SiteCpPair> getSiteCps(String resource, Long cpId, String[] ops) {
		return getSiteCps(resource, cpId, ops, false);
	}

	public Set<SiteCpPair> getSiteCps(String resource, Long cpId, String[] ops, boolean excludeCps) {
		return getSiteCps(new String[] { resource }, cpId, ops, excludeCps);
	}

	public Set<SiteCpPair> getSiteCps(String[] resources, Long cpId, String[] ops, boolean excludeCps) {
		if (AuthUtil.isAdmin()) {
			return null;
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		List<SubjectAccess> accessList;
		if (cpId != null) {
			accessList = daoFactory.getSubjectDao().getAccessList(userId, cpId, resources, ops);
		} else {
			accessList = daoFactory.getSubjectDao().getAccessList(userId, resources, ops);
		}

		Long instituteId = AuthUtil.getCurrentUserInstitute().getId();
		Set<SiteCpPair> siteCps = new HashSet<>();
		for (SubjectAccess access : accessList) {
			Long siteId = access.getSite() != null ? access.getSite().getId() : null;
			cpId = access.getCollectionProtocol() != null ? access.getCollectionProtocol().getId() : null;
			siteCps.add(SiteCpPair.make(access.getResource(), instituteId, siteId, !excludeCps ? cpId : null));
		}

		return siteCps;
	}

	private List<SiteCpPair> deDupSiteCpPairs(Collection<SiteCpPair> siteCps) {
		siteCps = new HashSet<>(siteCps);

		Set<Pair<Long, Long>> sitesOfAllCps = new HashSet<>();
		List<SiteCpPair> result = new ArrayList<>();
		for (SiteCpPair siteCp : siteCps) {
			if (siteCp.getCpId() == null) {
				sitesOfAllCps.add(Pair.make(siteCp.getInstituteId(), siteCp.getSiteId()));
				result.add(siteCp);
			}
		}

		for (SiteCpPair siteCp : siteCps) {
			if (sitesOfAllCps.contains(Pair.make(siteCp.getInstituteId(), siteCp.getSiteId()))) {
				continue;
			}

			result.add(siteCp);
		}

		return result;
	}

	private boolean isAccessAllowedOnAnySite(List<SubjectAccess> accessList, Set<Site> sites) {
		boolean allowed = false, userInstituteCheckDone = false;
		for (SubjectAccess access : accessList) {
			Site accessSite = access.getSite();
			if (accessSite != null && sites.contains(accessSite)) { // Specific site
				allowed = true;
			} else if (accessSite == null && !userInstituteCheckDone) { // All user institute sites
				userInstituteCheckDone = true;
				allowed = sites.stream().anyMatch(s -> s.getInstitute().equals(AuthUtil.getCurrentUserInstitute()));
			}

			if (allowed) {
				break;
			}
		}

		return allowed;
	}

	private boolean isAccessAllowedOnSite(Site allowedSite, Site site) {
		return (allowedSite != null && allowedSite.equals(site)) ||
			(allowedSite == null && AuthUtil.getCurrentUserInstitute().equals(site.getInstitute()));
	}

	private boolean isAccessAllowedOnSite(SiteCpPair allowedSite, Site site) {
		if (site == null) {
			return false;
		}

		return (allowedSite.getSiteId() != null && allowedSite.getSiteId().equals(site.getId())) ||
			(allowedSite.getSiteId() == null && allowedSite.getInstituteId().equals(site.getInstitute().getId()));
	}

	private boolean isAccessAllowedOnSite(Collection<SiteCpPair> allowedSites, Site testSite) {
		return allowedSites.stream().anyMatch(allowedSite -> isAccessAllowedOnSite(allowedSite, testSite));
	}

	private boolean isAccessAllowedOnSites(Collection<SiteCpPair> allowedSites, Collection<SiteCpPair> testSites, boolean allSites) {
		return allSites ? SiteCpPair.containsAll(allowedSites, testSites) : SiteCpPair.contains(allowedSites, testSites);
	}

	private boolean isImportOp() {
		return ImporterContextHolder.getInstance().isImportOp();
	}

	private boolean isExportOp() {
		return ExporterContextHolder.getInstance().isExportOp();
	}
}
