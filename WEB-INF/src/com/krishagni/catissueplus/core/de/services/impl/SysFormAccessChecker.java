package com.krishagni.catissueplus.core.de.services.impl;

import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.DpRequirement;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.de.services.FormAccessChecker;

import krishagni.catissueplus.beans.FormContextBean;

public class SysFormAccessChecker implements FormAccessChecker {
	@Override
	public boolean isUpdateAllowed(FormContextBean formCtxt) {
		return AuthUtil.isAdmin();
	}

	@Override
	public boolean isDataReadAllowed(Object obj) {
		boolean allowed = true;

		try {
			if (obj instanceof CollectionProtocol) {
				AccessCtrlMgr.getInstance().ensureReadCpRights((CollectionProtocol) obj);
			} else if (obj instanceof StorageContainer) {
				AccessCtrlMgr.getInstance().ensureReadContainerRights((StorageContainer) obj);
			} else if (obj instanceof DistributionProtocol) {
				AccessCtrlMgr.getInstance().ensureReadDpRights((DistributionProtocol) obj);
			} else if (obj instanceof DpRequirement) {
				AccessCtrlMgr.getInstance().ensureReadDpRights(((DpRequirement) obj).getDistributionProtocol());
			} else if (obj instanceof Site) {
				if (!AccessCtrlMgr.getInstance().isAccessible((Site) obj)) {
					AccessCtrlMgr.getInstance().ensureCreateShipmentRights();
				}
			} else if (obj instanceof Specimen) {
				AccessCtrlMgr.getInstance().ensureReadSpecimenRights((Specimen) obj);
			} else if (obj instanceof Participant) {
				AccessCtrlMgr.getInstance().ensureReadParticipantRights((Participant) obj);
			} else {
				allowed = false;
			}
		} catch (Exception e) {
			allowed = false;
		}

		return allowed;
	}

	@Override
	public boolean isDataReadAllowed(String entityType, Long objectId) {
		boolean allowed = true;
		try {
			switch (entityType) {
				case "CollectionProtocolExtension" ->
					AccessCtrlMgr.getInstance().ensureReadCpRights(objectId);

				case "StorageContainerExtension" ->
					AccessCtrlMgr.getInstance().ensureReadContainerRights(objectId);

				case "DpRequirementExtension" ->
					AccessCtrlMgr.getInstance().ensureReadDpReqRights(objectId);

				case "DistributionProtocolExtension" ->
					AccessCtrlMgr.getInstance().ensureReadDpRights(objectId);

				case "SiteExtension" -> {
					if (!AccessCtrlMgr.getInstance().isAccessibleSite(objectId)) {
						AccessCtrlMgr.getInstance().ensureCreateShipmentRights();
					}
				}

				case "CommonParticipant" ->
					AccessCtrlMgr.getInstance().ensureReadParticipantRights(objectId);

				case "SpecimenEvent" ->
					AccessCtrlMgr.getInstance().ensureReadSpecimenRights(objectId);

				default ->
					allowed = false;
			}
		} catch (Exception e) {
			allowed = false;
		}

		return allowed;
	}

	@Override
	public boolean isDataUpdateAllowed(Object obj) {
		boolean allowed = true;

		try {
			if (obj instanceof CollectionProtocol) {
				AccessCtrlMgr.getInstance().ensureUpdateCpRights((CollectionProtocol) obj);
			} else if (obj instanceof StorageContainer) {
				AccessCtrlMgr.getInstance().ensureUpdateContainerRights((StorageContainer) obj);
			} else if (obj instanceof DistributionProtocol) {
				AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights((DistributionProtocol) obj);
			} else if (obj instanceof DpRequirement) {
				AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(((DpRequirement) obj).getDistributionProtocol());
			} else if (obj instanceof Site) {
				AccessCtrlMgr.getInstance().ensureCreateUpdateDeleteSiteRights((Site) obj);
			} else if (obj instanceof Specimen) {
				AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights((Specimen) obj);
			} else if (obj instanceof Participant) {
				AccessCtrlMgr.getInstance().ensureUpdateParticipantRights((Participant) obj);
			} else if (obj instanceof CollectionProtocolRegistration) {
				AccessCtrlMgr.getInstance().ensureUpdateCprRights((CollectionProtocolRegistration) obj);
			} else {
				allowed = false;
			}
		} catch (Exception e) {
			allowed = false;
		}

		return allowed;
	}

	@Override
	public boolean isDataUpdateAllowed(String entityType, Long objectId) {
		boolean allowed = true;

		try {
			switch (entityType) {
				case "CollectionProtocolExtension" ->
					AccessCtrlMgr.getInstance().ensureUpdateCpRights(objectId);

				case "StorageContainerExtension" ->
					AccessCtrlMgr.getInstance().ensureUpdateContainerRights(objectId);

				case "DpRequirementExtension" ->
					AccessCtrlMgr.getInstance().ensureUpdateDpReqRights(objectId);

				case "DistributionProtocolExtension" ->
					AccessCtrlMgr.getInstance().ensureUpdateDpRights(objectId);

				case "SiteExtension" ->
					AccessCtrlMgr.getInstance().ensureCreateUpdateDeleteSiteRights(objectId);

				case "CommonParticipant" ->
					AccessCtrlMgr.getInstance().ensureUpdateParticipantRights(objectId);

				case "SpecimenEvent" ->
					AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(objectId, false);

				default ->
					allowed = false;
			}
		} catch (Exception e) {
			allowed = false;
		}

		return allowed;
	}
}
