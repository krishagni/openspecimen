
package com.krishagni.catissueplus.core.biospecimen.repository;

import com.krishagni.catissueplus.core.administrative.repository.AutoFreezerProviderDao;
import com.krishagni.catissueplus.core.administrative.repository.ContainerActivityLogDao;
import com.krishagni.catissueplus.core.administrative.repository.ContainerStoreListDao;
import com.krishagni.catissueplus.core.administrative.repository.ContainerTaskDao;
import com.krishagni.catissueplus.core.administrative.repository.ContainerTypeDao;
import com.krishagni.catissueplus.core.administrative.repository.DistributionOrderDao;
import com.krishagni.catissueplus.core.administrative.repository.DistributionProtocolDao;
import com.krishagni.catissueplus.core.administrative.repository.DpRequirementDao;
import com.krishagni.catissueplus.core.administrative.repository.InstituteDao;
import com.krishagni.catissueplus.core.administrative.repository.PermissibleValueDao;
import com.krishagni.catissueplus.core.administrative.repository.ScheduledContainerActivityDao;
import com.krishagni.catissueplus.core.administrative.repository.ScheduledJobDao;
import com.krishagni.catissueplus.core.administrative.repository.ShipmentDao;
import com.krishagni.catissueplus.core.administrative.repository.SiteDao;
import com.krishagni.catissueplus.core.administrative.repository.SpecimenRequestDao;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerDao;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerPositionDao;
import com.krishagni.catissueplus.core.administrative.repository.UserDao;
import com.krishagni.catissueplus.core.administrative.repository.UserGroupDao;
import com.krishagni.catissueplus.core.audit.repository.AuditDao;
import com.krishagni.catissueplus.core.auth.repository.AuthDao;
import com.krishagni.catissueplus.core.common.repository.ConfigSettingDao;
import com.krishagni.catissueplus.core.common.repository.ExternalAppIdDao;
import com.krishagni.catissueplus.core.common.repository.MessageLogDao;
import com.krishagni.catissueplus.core.common.repository.PdeAuditLogDao;
import com.krishagni.catissueplus.core.common.repository.PrintRuleConfigDao;
import com.krishagni.catissueplus.core.common.repository.SearchEntityKeywordDao;
import com.krishagni.catissueplus.core.common.repository.StarredItemDao;
import com.krishagni.catissueplus.core.common.repository.UnhandledExceptionDao;
import com.krishagni.catissueplus.core.common.repository.UniqueIdGenerator;
import com.krishagni.catissueplus.core.common.repository.UpgradeLogDao;
import com.krishagni.catissueplus.core.common.repository.UserNotificationDao;

public interface DaoFactory {
	CollectionProtocolDao getCollectionProtocolDao();

	CollectionProtocolPublishEventDao getCollectionProtocolPublishEventDao();

	ParticipantDao getParticipantDao();

	StagedParticipantDao getStagedParticipantDao();

	CollectionProtocolRegistrationDao getCprDao();

	AnonymizeEventDao getAnonymizeEventDao();

	SiteDao getSiteDao();

	SpecimenDao getSpecimenDao();

	SpecimenTypeUnitDao getSpecimenTypeUnitDao();

	SpecimenRequirementDao getSpecimenRequirementDao();

	VisitsDao getVisitsDao();

	StagedVisitDao getStagedVisitDao();

	UserDao getUserDao();

	UserGroupDao getUserGroupDao();
	
	AuthDao getAuthDao();

	UniqueIdGenerator getUniqueIdGenerator();

	InstituteDao getInstituteDao();

	StorageContainerDao getStorageContainerDao();

	StorageContainerPositionDao getStorageContainerPositionDao();
	
	ContainerTypeDao getContainerTypeDao();

	ContainerTaskDao getContainerTaskDao();

	ScheduledContainerActivityDao getScheduledContainerActivityDao();

	ContainerActivityLogDao getContainerActivityLogDao();

	DistributionProtocolDao getDistributionProtocolDao();

	SpecimenListDao getSpecimenListDao();

	SpecimenListsFolderDao getSpecimenListsFolderDao();

	SpecimenKitDao getSpecimenKitDao();

	PermissibleValueDao getPermissibleValueDao();
	
	ScheduledJobDao getScheduledJobDao();
	
	DistributionOrderDao getDistributionOrderDao();
	
	ConfigSettingDao getConfigSettingDao();
	
	LabelPrintJobDao getLabelPrintJobDao();
	
	AuditDao getAuditDao();

	DpRequirementDao getDistributionProtocolRequirementDao();
	
	ShipmentDao getShipmentDao();

	SpecimenRequestDao getSpecimenRequestDao();
	
	UpgradeLogDao getUpgradeLogDao();

	CpReportSettingsDao getCpReportSettingsDao();
	
	UnhandledExceptionDao getUnhandledExceptionDao();

	ConsentStatementDao getConsentStatementDao();

	ContainerStoreListDao getContainerStoreListDao();

	AutoFreezerProviderDao getAutoFreezerProviderDao();

	UserNotificationDao getUserNotificationDao();

	PrintRuleConfigDao getPrintRuleConfigDao();

	ExternalAppIdDao getExternalAppIdDao();

	MessageLogDao getMessageLogDao();

	SearchEntityKeywordDao getSearchEntityKeywordDao();

	CollectionProtocolGroupDao getCpGroupDao();

	StarredItemDao getStarredItemDao();

	PdeAuditLogDao getPdeAuditLogDao();
} 
