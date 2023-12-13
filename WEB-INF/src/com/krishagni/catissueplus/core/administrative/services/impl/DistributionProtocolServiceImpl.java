
package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.DpConsentTier;
import com.krishagni.catissueplus.core.administrative.domain.DpDistributionSite;
import com.krishagni.catissueplus.core.administrative.domain.DpRequirement;
import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionOrderErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionProtocolErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionProtocolFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.DpRequirementErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.DpRequirementFactory;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderStat;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderStatListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionProtocolDetail;
import com.krishagni.catissueplus.core.administrative.events.DpConsentTierDetail;
import com.krishagni.catissueplus.core.administrative.events.DpRequirementDetail;
import com.krishagni.catissueplus.core.administrative.events.DprStat;
import com.krishagni.catissueplus.core.administrative.repository.DpListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.DpRequirementDao;
import com.krishagni.catissueplus.core.administrative.repository.UserListCriteria;
import com.krishagni.catissueplus.core.administrative.services.DistributionProtocolService;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolSite;
import com.krishagni.catissueplus.core.biospecimen.domain.ConsentStatement;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ConsentStatementErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenListErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.BiospecimenDaoHelper;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.ConfigSettingDetail;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.StarredItemService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.NotifUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.domain.Form;
import com.krishagni.catissueplus.core.de.events.FormContextDetail;
import com.krishagni.catissueplus.core.de.events.RemoveFormContextOp;
import com.krishagni.catissueplus.core.de.services.FormAccessChecker;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.catissueplus.core.query.Column;
import com.krishagni.catissueplus.core.query.ListConfig;
import com.krishagni.catissueplus.core.query.ListService;
import com.krishagni.catissueplus.core.query.ListUtil;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.napi.FormEventsListener;
import edu.common.dynamicextensions.napi.FormEventsNotifier;
import krishagni.catissueplus.beans.FormContextBean;

public class DistributionProtocolServiceImpl implements DistributionProtocolService, ObjectAccessor, InitializingBean {
	
	private static final Map<String, String> attrDisplayKeys = new HashMap<String, String>() {
		{
			put("specimenType", "dist_specimen_type");
			put("anatomicSite", "dist_anatomic_site");
			put("pathologyStatus", "dist_pathology_status");
		}
	};

	private static final String SYS_CUSTOM_FIELDS_FORM = "order_custom_fields_form";
	
	private DaoFactory daoFactory;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;

	private DistributionProtocolFactory distributionProtocolFactory;
	
	private DpRequirementFactory dprFactory;

	private FormService formSvc;

	private ConfigurationService cfgSvc;

	private ListService listSvc;

	private ExportService exportSvc;

	private StarredItemService starredItemSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	public void setDistributionProtocolFactory(DistributionProtocolFactory distributionProtocolFactory) {
		this.distributionProtocolFactory = distributionProtocolFactory;
	}
	
	public void setDprFactory(DpRequirementFactory dprFactory) {
		this.dprFactory = dprFactory;
	}

	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
		cfgSvc.registerChangeListener("administrative", (name, value) -> {
			if (!SYS_CUSTOM_FIELDS_FORM.equals(name)) {
				return;
			}

			Long formId = StringUtils.isBlank(value) ? null : Long.parseLong(value);
			updateSysOrderExtnFormContext(formId);
		});
	}

	public void setListSvc(ListService listSvc) {
		this.listSvc = listSvc;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	public void setStarredItemSvc(StarredItemService starredItemSvc) {
		this.starredItemSvc = starredItemSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DistributionProtocolDetail>> getDistributionProtocols(RequestEvent<DpListCriteria> req) {
		try {
			DpListCriteria crit = addDpListCriteria(req.getPayload());
			if (crit == null) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<DistributionProtocolDetail> result = new ArrayList<>();
			if (crit.orderByStarred()) {
				List<Long> dpIds = daoFactory.getStarredItemDao().getItemIds(getObjectName(), AuthUtil.getCurrentUser().getId());
				if (!dpIds.isEmpty()) {
					crit.ids(dpIds);
					List<DistributionProtocol> dps = daoFactory.getDistributionProtocolDao().getDistributionProtocols(crit);
					result.addAll(DistributionProtocolDetail.from(dps));
					result.forEach(dp -> dp.setStarred(true));
					crit.ids(Collections.emptyList()).notInIds(dpIds);
				}
			}

			if (result.size() < crit.maxResults()) {
				crit.maxResults(crit.maxResults() - result.size());
				List<DistributionProtocol> dps = daoFactory.getDistributionProtocolDao().getDistributionProtocols(crit);
				result.addAll(DistributionProtocolDetail.from(dps));
			}

			if (crit.includeStat()) {
				addDpStats(result);
			}

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Long> getDistributionProtocolsCount(RequestEvent<DpListCriteria> req) {
		try {
			DpListCriteria crit = addDpListCriteria(req.getPayload());
			if (crit == null) {
				return ResponseEvent.response(0L);
			}

			return ResponseEvent.response(daoFactory.getDistributionProtocolDao().getDistributionProtocolsCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<DistributionProtocolDetail> getDistributionProtocol(RequestEvent<Long> req) {
		try {
			Long protocolId = req.getPayload();
			DistributionProtocol existing = getDistributionProtocol(protocolId);
			AccessCtrlMgr.getInstance().ensureReadDpRights(existing);
			return ResponseEvent.response(DistributionProtocolDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DistributionProtocolDetail> createDistributionProtocol(RequestEvent<DistributionProtocolDetail> req) {
		try {
			DistributionProtocol dp = distributionProtocolFactory.createDistributionProtocol(req.getPayload());
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(dp);
			ensureUniqueConstraints(dp, null);
			ensurePiCoordNotSame(dp);
			
			daoFactory.getDistributionProtocolDao().saveOrUpdate(dp);
			dp.addOrUpdateExtension();
			updateOrderExtnFormContext(dp, null, dp.getOrderExtnForm());
			notifyOnDpCreate(dp);
			return ResponseEvent.response(DistributionProtocolDetail.from(dp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DistributionProtocolDetail> updateDistributionProtocol(RequestEvent<DistributionProtocolDetail> req) {
		return updateProtocol(req, false);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DistributionProtocolDetail> patchDistributionProtocol(RequestEvent<DistributionProtocolDetail> req) {
		return updateProtocol(req, true);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<Long> req) {
		try {
			DistributionProtocol existing = getDistributionProtocol(req.getPayload());
			return ResponseEvent.response(existing.getDependentEntities());
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DistributionProtocolDetail>> deleteDistributionProtocols(RequestEvent<BulkDeleteEntityOp> req) {
		//
		// TODO: Force delete is not implemented
		//
		try {
			Set<Long> dpIds = req.getPayload().getIds();

			List<DistributionProtocol> dps = daoFactory.getDistributionProtocolDao().getByIds(dpIds);
			if (dpIds.size() != dps.size()) {
				dps.forEach(dp -> dpIds.remove(dp.getId()));
				throw OpenSpecimenException.userError(DistributionProtocolErrorCode.NOT_FOUND, dpIds, dpIds.size());
			}

			dps.forEach(AccessCtrlMgr.getInstance()::ensureDeleteDpRights);
			dps.forEach(dp -> deleteDistributionProtocol(dp));

			return ResponseEvent.response(DistributionProtocolDetail.from(dps));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<DistributionProtocolDetail> updateActivityStatus(RequestEvent<DistributionProtocolDetail> req) {
		try {
			Long dpId = req.getPayload().getId();
			String status = req.getPayload().getActivityStatus();
			if (StringUtils.isBlank(status) || !Status.isValidActivityStatus(status)) {
				return ResponseEvent.userError(ActivityStatusErrorCode.INVALID);
			}
			
			DistributionProtocol existing = getDistributionProtocol(dpId);

			if (existing.getActivityStatus().equals(status)) {
				return ResponseEvent.response(DistributionProtocolDetail.from(existing));
			}
			
			if (status.equals(Status.ACTIVITY_STATUS_DISABLED.getStatus())) {
				AccessCtrlMgr.getInstance().ensureDeleteDpRights(existing);
				deleteDistributionProtocol(existing);
			} else {
				AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(existing);
				existing.setActivityStatus(status);
			}

			daoFactory.getDistributionProtocolDao().saveOrUpdate(existing);
			return ResponseEvent.response(DistributionProtocolDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<DistributionOrderStat>> getOrderStats(RequestEvent<DistributionOrderStatListCriteria> req) {
		try {
			return ResponseEvent.response(getOrderStats(req.getPayload()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<File> exportOrderStats(RequestEvent<DistributionOrderStatListCriteria> req) {
		File tempFile = null;
		CsvWriter csvWriter = null;
		try {
			DistributionOrderStatListCriteria crit = req.getPayload();
			List<DistributionOrderStat> orderStats = getOrderStats(crit);
			
			tempFile = File.createTempFile("dp-order-stats", null);
			csvWriter = CsvFileWriter.createCsvFileWriter(tempFile);
			
			if (crit.dpId() != null && !orderStats.isEmpty()) {
				DistributionOrderStat orderStat = orderStats.get(0);
				csvWriter.writeNext(new String[] {
					MessageUtil.getInstance().getMessage("dist_dp_title"),
					orderStat.getDpShortTitle()
				});
			}
			
			csvWriter.writeNext(new String[] {
				MessageUtil.getInstance().getMessage("dist_exported_by"),
				AuthUtil.getCurrentUser().formattedName()
			});
			csvWriter.writeNext(new String[] {
				MessageUtil.getInstance().getMessage("dist_exported_on"),
				Utility.getDateString(Calendar.getInstance().getTime())
			});
			csvWriter.writeNext(new String[1]);
			
			String[] headers = getOrderStatsReportHeaders(crit);
			csvWriter.writeNext(headers);
			for (DistributionOrderStat stat: orderStats) {
				csvWriter.writeNext(getOrderStatsReportData(stat, crit));
			}
			
			return ResponseEvent.response(tempFile);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			IOUtils.closeQuietly(csvWriter);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> getExtensionForm() {
		return ResponseEvent.response(formSvc.getExtensionInfo(-1L, DistributionProtocol.EXTN));
	}

	@Override
	public ResponseEvent<Map<String, Object>> getOrderExtensionForm(Long dpId) {
		return ResponseEvent.response(formSvc.getExtensionInfo(false, DistributionOrder.getExtnEntityType(), dpId));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DpRequirementDetail>> getRequirements(RequestEvent<Long> req) {
		try {
			Long dpId = req.getPayload();
			DistributionProtocol dp = getDistributionProtocol(dpId);
			AccessCtrlMgr.getInstance().ensureReadDpRights(dp);
			
			List<DpRequirementDetail> reqDetails = DpRequirementDetail.from(dp.getRequirements());
			Map<Long, DprStat> distributionStat = getDprDao().getDistributionStatByDp(dpId);
			for (DpRequirementDetail reqDetail : reqDetails) {
				DprStat stat = distributionStat.get(reqDetail.getId());
				if (stat != null) {
					reqDetail.setDistributedCnt(stat.getDistributedCnt());
					reqDetail.setDistributedQty(stat.getDistributedQty());
				} else {
					reqDetail.setDistributedCnt(0L);
					reqDetail.setDistributedQty(BigDecimal.ZERO);
				}
			}
			
			return ResponseEvent.response(reqDetails);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<DpRequirementDetail> getRequirement(RequestEvent<Long> req) {
		try {
			DpRequirement existing = getDprDao().getById(req.getPayload());
			if (existing == null) {
				return ResponseEvent.userError(DpRequirementErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureReadDpRights(existing.getDistributionProtocol());
			return ResponseEvent.response(DpRequirementDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DpRequirementDetail> createRequirement(RequestEvent<DpRequirementDetail> req) {
		try {
			DpRequirement dpr = dprFactory.createRequirement(req.getPayload());
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(dpr.getDistributionProtocol());

			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureSpecimenPropertyPresent(dpr, ose);
			ensureUniqueReqConstraints(null, dpr, ose);
			ose.checkAndThrow();

			getDprDao().saveOrUpdate(dpr);
			dpr.addOrUpdateExtension();
			return ResponseEvent.response(DpRequirementDetail.from(dpr));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DpRequirementDetail> updateRequirement(RequestEvent<DpRequirementDetail> req) {
		return updateRequirement(req, false);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DpRequirementDetail> patchRequirement(RequestEvent<DpRequirementDetail> req) {
		return updateRequirement(req, true);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DpRequirementDetail> deleteRequirement(RequestEvent<Long> req) {
		try {
			DpRequirement existing = getDprDao().getById(req.getPayload());
			if (existing == null) {
				return ResponseEvent.userError(DpRequirementErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(existing.getDistributionProtocol());
			existing.delete();
			getDprDao().saveOrUpdate(existing);
			return ResponseEvent.response(DpRequirementDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DpConsentTierDetail>> getConsentTiers(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			DistributionProtocol dp = getDistributionProtocol(crit.getId(), crit.getName(), null);
			
			AccessCtrlMgr.getInstance().ensureReadDpRights(dp);
			return ResponseEvent.response(DpConsentTierDetail.from(dp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DpConsentTierDetail> createConsentTier(RequestEvent<DpConsentTierDetail> req) {
		try {
			DpConsentTierDetail detail = req.getPayload();

			DistributionProtocol dp = getDistributionProtocol(detail.getDpId(), detail.getDpShortTitle(), detail.getDpTitle());
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(dp);

			ensureUniqueConsentStatement(detail, dp);
			ConsentStatement stmt = getStatement(detail.getStatementId(), detail.getStatementCode(), detail.getStatement());

			DpConsentTier tier = dp.addConsentTier(getConsentTierObj(detail.getId(), stmt));
			daoFactory.getDistributionProtocolDao().saveOrUpdate(dp, true);
			return ResponseEvent.response(DpConsentTierDetail.from(tier));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DpConsentTierDetail> updateConsentTier(RequestEvent<DpConsentTierDetail> req) {
		try {
			DpConsentTierDetail detail = req.getPayload();

			DistributionProtocol dp = getDistributionProtocol(detail.getDpId(), detail.getDpShortTitle(), detail.getDpTitle());
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(dp);

			ensureUniqueConsentStatement(detail, dp);
			ConsentStatement stmt = getStatement(detail.getStatementId(), detail.getStatementCode(), detail.getStatement());

			DpConsentTier tier = dp.updateConsentTier(getConsentTierObj(detail.getId(), stmt));
			return ResponseEvent.response(DpConsentTierDetail.from(tier));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<DpConsentTierDetail> deleteConsentTier(RequestEvent<DpConsentTierDetail> req) {
		try {
			DpConsentTierDetail detail = req.getPayload();
			DistributionProtocol dp = getDistributionProtocol(detail.getDpId(), detail.getDpShortTitle(), detail.getDpTitle());
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(dp);

			DpConsentTier tier = dp.removeConsentTier(detail.getId());
			return ResponseEvent.response(DpConsentTierDetail.from(tier));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public boolean toggleStarredDp(Long dpId, boolean starred) {
		try {
			DistributionProtocol dp = getDistributionProtocol(dpId);
			AccessCtrlMgr.getInstance().ensureReadDpRights(dp);
			if (starred) {
				starredItemSvc.save(getObjectName(), dp.getId());
			} else {
				starredItemSvc.delete(getObjectName(), dp.getId());
			}

			return true;
		} catch (OpenSpecimenException e) {
			throw e;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	@Override
	public String getObjectName() {
		return DistributionProtocol.getEntityName();
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getDistributionProtocolDao().getDpIds(key, value);
	}

	@Override
	public String getAuditTable() {
		return "CAT_DISTRIBUTION_PROTOCOL_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		DistributionProtocol dp = getDistributionProtocol(id);
		AccessCtrlMgr.getInstance().ensureReadDpRights(dp);
	}

	@Override
	public void afterPropertiesSet()
	throws Exception {
		listSvc.registerListConfigurator(RESV_SPMNS_LIST_NAME, this::getReservedSpecimensConfig);
		exportSvc.registerObjectsGenerator("distributionProtocol", this::getDpsGenerator);
		formSvc.addAccessChecker(DistributionOrder.getExtnEntityType(),
			new FormAccessChecker() {
				@Override
				public boolean isUpdateAllowed(FormContextBean formCtxt) {
					Long dpId = formCtxt.getEntityId();
					if (dpId == null || dpId == -1L) {
						return AuthUtil.isAdmin();
					} else {
						return AccessCtrlMgr.getInstance().hasCreateUpdateDpRights(dpId);
					}
				}

				@Override
				public boolean isDataReadAllowed(Object obj) {
					try {
						if (obj instanceof DistributionOrder) {
							AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights((DistributionOrder) obj);
						} else {
							return false;
						}

						return true;
					} catch (Exception e) {
						return false;
					}
				}

				@Override
				public boolean isDataReadAllowed(String entityType, Long objectId) {
					return isDataReadAllowed(getOrder(objectId));
				}


				@Override
				public boolean isDataUpdateAllowed(Object obj) {
					try {
						if (obj instanceof DistributionOrder) {
							AccessCtrlMgr.getInstance().ensureUpdateDistributionOrderRights((DistributionOrder) obj);
						} else {
							return false;
						}

						return true;
					} catch (Exception e) {
						return false;
					}
				}

				@Override
				public boolean isDataUpdateAllowed(String entityType, Long objectId) {
					return isDataUpdateAllowed(getOrder(objectId));
				}

				private DistributionOrder getOrder(Long orderId) {
					DistributionOrder order = daoFactory.getDistributionOrderDao().getById(orderId);
					if (order == null) {
						throw OpenSpecimenException.userError(DistributionOrderErrorCode.NOT_FOUND, orderId);
					}

					return order;
				}

			});

		FormEventsNotifier.getInstance().addListener(
			new FormEventsListener() {
				@Override
				public void onCreate(Container container) { }

				@Override
				public void preUpdate(Container container) { }

				@Override
				public void onUpdate(Container container) { }

				@Override
				public void onDelete(Container container) {
					daoFactory.getDistributionProtocolDao().unlinkCustomForm(container.getId());

					Integer currentForm = cfgSvc.getIntSetting("administrative", SYS_CUSTOM_FIELDS_FORM, -1);
					if (!currentForm.equals(container.getId().intValue())) {
						return;
					}

					ConfigSettingDetail cfg = new ConfigSettingDetail();
					cfg.setModule("administrative");
					cfg.setName(SYS_CUSTOM_FIELDS_FORM);
					cfg.setValue(null);
					cfgSvc.saveSetting(new RequestEvent<>(cfg)).throwErrorIfUnsuccessful();
				}
			}
		);
	}

	private DpListCriteria addDpListCriteria(DpListCriteria crit) {
		Set<SiteCpPair> sites = AccessCtrlMgr.getInstance().getReadAccessDistributionProtocolSites();
		if (sites != null && sites.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		Set<SiteCpPair> result = sites;
		if (StringUtils.isNotBlank(crit.cpShortTitle())) {
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(crit.cpShortTitle());
			if (cp == null) {
				return null;
			}

			Set<SiteCpPair> cpSites = cp.getSites().stream()
				.map(CollectionProtocolSite::getSite)
				.map(site -> SiteCpPair.make(site.getInstitute().getId(), site.getId(), null))
				.collect(Collectors.toSet());

			if (sites != null) {
				result = cpSites.stream()
					.filter(siteCp -> sites.stream().anyMatch(s -> s.isAllowed(siteCp)))
					.collect(Collectors.toSet());
				if (result.isEmpty()) {
					return null;
				}
			} else {
				result = cpSites;
			}
		}

		if (result != null) {
			crit.sites(result);
		}
		
		return crit;
	}

	private void deleteDistributionProtocol(DistributionProtocol dp) {
		DistributionProtocol deletedDp = new DistributionProtocol();
		BeanUtils.copyProperties(dp, deletedDp);
		dp.delete();
		notifyOnDpDelete(deletedDp);
	}

	private void ensureSpecimenPropertyPresent(DpRequirement dpr, OpenSpecimenException ose) {
		if (dpr.getSpecimenType() == null &&
			dpr.getAnatomicSite() == null &&
			CollectionUtils.isEmpty(dpr.getPathologyStatuses()) &&
			dpr.getClinicalDiagnosis() == null) {
			ose.addError(DpRequirementErrorCode.SPEC_PROPERTY_REQUIRED);
		}
	}

	private void ensureUniqueReqConstraints(DpRequirement oldDpr, DpRequirement newDpr, OpenSpecimenException ose) {
		if (oldDpr != null && oldDpr.equalsSpecimenGroup(newDpr)) {
			return;
		}
		
		DistributionProtocol dp = newDpr.getDistributionProtocol();
		if (dp.hasRequirement(newDpr)) {
			ose.addError(DpRequirementErrorCode.ALREADY_EXISTS);
		}
	}
	
	private void ensurePiCoordNotSame(DistributionProtocol dp) {
		if (!dp.getCoordinators().contains(dp.getPrincipalInvestigator())) {
			return;
		}

		throw OpenSpecimenException.userError(
			DistributionProtocolErrorCode.PI_COORD_CANNOT_BE_SAME,
			dp.getPrincipalInvestigator().formattedName());
	}
	
	private void addDpStats(List<DistributionProtocolDetail> dps) {
		if (CollectionUtils.isEmpty(dps)) {
			return;
		}
		
		Map<Long, DistributionProtocolDetail> dpMap = dps.stream()
			.collect(Collectors.toMap(DistributionProtocolDetail::getId, dp -> dp));
		Map<Long, Integer> countMap = daoFactory.getDistributionProtocolDao().getSpecimensCountByDpIds(dpMap.keySet());
		countMap.forEach((dpId, count) -> dpMap.get(dpId).setDistributedSpecimensCount(count));
	}

	private ResponseEvent<DistributionProtocolDetail> updateProtocol(RequestEvent<DistributionProtocolDetail> req, boolean partial) {
		try {
			DistributionProtocolDetail reqDetail = req.getPayload();
			DistributionProtocol existing = getDistributionProtocol(reqDetail.getId(), reqDetail.getShortTitle(), reqDetail.getTitle());
			if (Status.isDisabledStatus(reqDetail.getActivityStatus())) {
				AccessCtrlMgr.getInstance().ensureDeleteDpRights(existing);
				deleteDistributionProtocol(existing);
				return ResponseEvent.response(DistributionProtocolDetail.from(existing));
			}

			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(existing);

			reqDetail.setId(existing.getId());
			DistributionProtocol dp = distributionProtocolFactory.createDistributionProtocol(partial ? existing : null, reqDetail);
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(dp);
			ensureUniqueConstraints(dp, existing);
			ensurePiCoordNotSame(dp);

			Form oldOrderExtnForm = existing.getOrderExtnForm();
			notifyOnDpUpdate(existing, dp);
			existing.update(dp);
			daoFactory.getDistributionProtocolDao().saveOrUpdate(existing);
			updateOrderExtnFormContext(dp, oldOrderExtnForm, dp.getOrderExtnForm());
			existing.addOrUpdateExtension();
			return ResponseEvent.response(DistributionProtocolDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception ex) {
			return ResponseEvent.serverError(ex);
		}
	}

	private ResponseEvent<DpRequirementDetail> updateRequirement(RequestEvent<DpRequirementDetail> req, boolean partial) {
		try {
			Long dpReqId = req.getPayload().getId();
			DpRequirement existing = getDprDao().getById(dpReqId);
			if (existing == null) {
				return ResponseEvent.userError(DpRequirementErrorCode.NOT_FOUND);
			}

			DpRequirement newDpr = dprFactory.createRequirement(partial ? existing : null, req.getPayload());
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(newDpr.getDistributionProtocol());

			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureSpecimenPropertyPresent(newDpr, ose);
			ensureUniqueReqConstraints(existing, newDpr, ose);
			ose.checkAndThrow();

			existing.update(newDpr);
			getDprDao().saveOrUpdate(existing);
			existing.addOrUpdateExtension();
			return ResponseEvent.response(DpRequirementDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	private void ensureUniqueConstraints(DistributionProtocol newDp, DistributionProtocol existingDp) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		
		if (!isUniqueTitle(newDp, existingDp)) {
			ose.addError(DistributionProtocolErrorCode.DUP_TITLE, newDp.getTitle());
		}
		
		if (!isUniqueShortTitle(newDp, existingDp)) {
			ose.addError(DistributionProtocolErrorCode.DUP_SHORT_TITLE, newDp.getShortTitle());
		}
		
		ose.checkAndThrow();
	}
	
	private boolean isUniqueTitle(DistributionProtocol newDp, DistributionProtocol existingDp) {
		if (existingDp != null && newDp.getTitle().equals(existingDp.getTitle())) {
			return true;
		}
		
		DistributionProtocol existing = daoFactory.getDistributionProtocolDao().getDistributionProtocol(newDp.getTitle());
		if (existing != null) {
			return false;
		}
		
		return true;
	}

	private boolean isUniqueShortTitle(DistributionProtocol newDp, DistributionProtocol existingDp) {
		if (existingDp != null && newDp.getShortTitle().equals(existingDp.getShortTitle())) {
			return true;
		}
		
		DistributionProtocol existing = daoFactory.getDistributionProtocolDao().getByShortTitle(newDp.getShortTitle());
		if (existing != null) {
			return false;
		}
		
		return true;
	}

	private List<DistributionOrderStat> getOrderStats(DistributionOrderStatListCriteria crit) {
		if (crit.dpId() != null) {
			DistributionProtocol dp = getDistributionProtocol(crit.dpId());
			AccessCtrlMgr.getInstance().ensureReadDpRights(dp);
		} else {
			Set<SiteCpPair> sites = AccessCtrlMgr.getInstance().getCreateUpdateAccessDistributionOrderSites();
			if (sites != null && sites.isEmpty()) {
				throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}
			
			if (sites != null) {
				crit.sites(sites);
			}
		}
		
		return daoFactory.getDistributionProtocolDao().getOrderStats(crit);
	}
	
	private String[] getOrderStatsReportHeaders(DistributionOrderStatListCriteria crit) {
		List<String> headers = new ArrayList<String>();
		if (crit.dpId() == null) {
			headers.add(MessageUtil.getInstance().getMessage("dist_dp_title"));
		}
		
		headers.add(MessageUtil.getInstance().getMessage("dist_order_name"));
		headers.add(MessageUtil.getInstance().getMessage("dist_distribution_date"));
		for (String attr: crit.groupByAttrs()) {
			headers.add(MessageUtil.getInstance().getMessage(attrDisplayKeys.get(attr)));
		}
		
		headers.add(MessageUtil.getInstance().getMessage("dist_specimen_distributed"));
		return headers.toArray(new String[0]);
	}
	
	private String [] getOrderStatsReportData(DistributionOrderStat stat, DistributionOrderStatListCriteria crit) {
		List<String> data = new ArrayList<>();
		if (crit.dpId() == null) {
			data.add(stat.getDpShortTitle());
		}
		
		data.add(stat.getName());
		data.add(Utility.getDateString(stat.getExecutionDate()));
		for (String attr: crit.groupByAttrs()) {
			data.add(stat.getGroupByAttrVals().get(attr).toString());
		}
		
		data.add(stat.getDistributedSpecimenCount().toString());
		
		return data.toArray(new String[0]);
	}

	private DistributionProtocol getDistributionProtocol(Long id) {
		return getDistributionProtocol(id, null, null);
	}
	
	private DistributionProtocol getDistributionProtocol(Long id, String dpShortTitle, String dpTitle) {
		DistributionProtocol dp = null;
		Object key = null;

		if (id != null) {
			dp = daoFactory.getDistributionProtocolDao().getById(id);
			key = id;
		} else if (StringUtils.isNotBlank(dpTitle)) {
			dp = daoFactory.getDistributionProtocolDao().getDistributionProtocol(dpTitle);
			key = dpTitle;
		} else if (StringUtils.isNotBlank(dpShortTitle)) {
			dp = daoFactory.getDistributionProtocolDao().getByShortTitle(dpShortTitle);
			key = dpShortTitle;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(DistributionProtocolErrorCode.DP_REQUIRED);
		} else if (dp == null) {
			throw OpenSpecimenException.userError(DistributionProtocolErrorCode.NOT_FOUND, key, 1);
		}
		
		return dp;
	}

	private ConsentStatement getStatement(Long id, String code, String statement) {
		ConsentStatement stmt = null;
		Object key = null;

		if (id != null) {
			key = id;
			stmt = daoFactory.getConsentStatementDao().getById(id);
		} else if (StringUtils.isNotBlank(code)) {
			key = code;
			stmt = daoFactory.getConsentStatementDao().getByCode(code);
		} else if (StringUtils.isNotBlank(statement)) {
			key = statement;
			stmt = daoFactory.getConsentStatementDao().getByStatement(statement);
		}

		if (key == null) {
			throw OpenSpecimenException.userError(ConsentStatementErrorCode.CODE_REQUIRED);
		} else if (stmt == null) {
			throw OpenSpecimenException.userError(ConsentStatementErrorCode.NOT_FOUND, key);
		}

		return stmt;
	}

	private DpConsentTier getConsentTierObj(Long id, ConsentStatement stmt) {
		DpConsentTier tier = new DpConsentTier();
		tier.setId(id);
		tier.setStatement(stmt);
		return tier;
	}

	private void ensureUniqueConsentStatement(DpConsentTierDetail consentTierDetail, DistributionProtocol dp) {
		Predicate<DpConsentTier> findFn;
		if (consentTierDetail.getStatementId() != null) {
			findFn = (t) -> t.getStatement().getId().equals(consentTierDetail.getStatementId());
		} else if (StringUtils.isNotBlank(consentTierDetail.getStatementCode())) {
			findFn = (t) -> t.getStatement().getCode().equals(consentTierDetail.getStatementCode());
		} else if (StringUtils.isNotBlank(consentTierDetail.getStatement())) {
			findFn = (t) -> t.getStatement().getStatement().equals(consentTierDetail.getStatement());
		} else {
			throw OpenSpecimenException.userError(ConsentStatementErrorCode.CODE_REQUIRED);
		}

		DpConsentTier tier = dp.getConsentTiers().stream().filter(findFn).findFirst().orElse(null);
		if (tier != null && !tier.getId().equals(consentTierDetail.getId())) {
			throw OpenSpecimenException.userError(DistributionProtocolErrorCode.DUP_CONSENT, tier.getStatement().getCode(), dp.getShortTitle());
		}
	}

	private void removeContainerRestrictions(DistributionProtocol dp) {
		Set<StorageContainer> containers = dp.getDistributionContainers();
		for (StorageContainer container : containers) {
			container.removeDpRestriction(dp);
		}

		dp.setDistributionContainers(Collections.emptySet());
	}

	private void notifyOnDpCreate(DistributionProtocol dp) {
		notifyDpRoleUpdated(dp, "ADD", "CREATE");
	}

	private void notifyOnDpUpdate(DistributionProtocol existingDp, DistributionProtocol newDp) {
		notifyDpRoleUpdated(existingDp, newDp, "UPDATE");
	}

	private void notifyOnDpDelete(DistributionProtocol dp) {
		notifyDpRoleUpdated(dp, "REMOVE", "DELETE");
	}

	private void notifyDpRoleUpdated(DistributionProtocol dp, String roleOp, String dpOp) {
		Map<String, Object> emailProps = getEmailProps(dp);

		notifyDpRoleUpdated(Collections.singletonList(dp.getPrincipalInvestigator()), dp, emailProps, null, roleOp, dpOp, N_USER, N_DP_PI);
		notifyDpRoleUpdated(dp.getCoordinators(), dp, emailProps, null, roleOp, dpOp, N_USER, N_DP_COORD);
		notifyDpRoleUpdated(getInstituteAdmins(dp.getInstitute()), dp, emailProps, dp.getInstitute().getName(),
			roleOp, dpOp, N_INST_ADMIN, N_DP_RECV_SITE);

		notifyRecvSiteAdmins(dp.getDefReceivingSite(), dp, emailProps, roleOp, dpOp);
		notifyDistSiteAdmins(dp.getDistributingSites(), dp, emailProps, roleOp, dpOp);
	}

	private void notifyDpRoleUpdated(DistributionProtocol existingDp, DistributionProtocol newDp, String op) {
		Map<String, Object> emailProps = getEmailProps(newDp);

		if (!existingDp.getPrincipalInvestigator().equals(newDp.getPrincipalInvestigator())) {
			notifyDpRoleUpdated(Collections.singletonList(existingDp.getPrincipalInvestigator()), newDp, emailProps, null, "REMOVE", op, N_USER, N_DP_PI);
			notifyDpRoleUpdated(Collections.singletonList(newDp.getPrincipalInvestigator()), newDp, emailProps, null, "ADD", op,  N_USER, N_DP_PI);
		}

		Collection<User> removedCoordinators = CollectionUtils.subtract(existingDp.getCoordinators(), newDp.getCoordinators());
		Collection<User> addedCoordinators = CollectionUtils.subtract(newDp.getCoordinators(), existingDp.getCoordinators());
		notifyDpRoleUpdated(removedCoordinators, newDp, emailProps, null, "REMOVE", op, N_USER, N_DP_COORD);
		notifyDpRoleUpdated(addedCoordinators, newDp, emailProps, null, "ADD", op, N_USER, N_DP_COORD);

		if (existingDp.getInstitute() != newDp.getInstitute()) {
			notifyDpRoleUpdated(getInstituteAdmins(existingDp.getInstitute()), newDp, emailProps,
				existingDp.getInstitute().getName(), "REMOVE", op,  N_INST_ADMIN, N_DP_RECV_SITE);
			notifyDpRoleUpdated(getInstituteAdmins(newDp.getInstitute()), newDp, emailProps,
				newDp.getInstitute().getName(), "ADD", op, N_INST_ADMIN, N_DP_RECV_SITE);
		}

		if (existingDp.getDefReceivingSite() != newDp.getDefReceivingSite()) {
			notifyRecvSiteAdmins(existingDp.getDefReceivingSite(), newDp, emailProps, "REMOVE", op);
			notifyRecvSiteAdmins(newDp.getDefReceivingSite(), newDp, emailProps, "ADD", op);
		}

		Collection<DpDistributionSite> removedDistSites = CollectionUtils.subtract(existingDp.getDistributingSites(), newDp.getDistributingSites());
		Collection<DpDistributionSite> addedDistSites = CollectionUtils.subtract(newDp.getDistributingSites(), existingDp.getDistributingSites());
		notifyDistSiteAdmins(removedDistSites, newDp, emailProps, "REMOVE", op);
		notifyDistSiteAdmins(addedDistSites, newDp, emailProps, "ADD", op);
	}

	private Map<String, Object> getEmailProps(DistributionProtocol dp) {
		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("dp", dp);
		emailProps.put("ccAdmin", false);
		emailProps.put("instituteSitesMap", DpDistributionSite.getInstituteSitesMap(dp.getDistributingSites()));
		return emailProps;
	}

	private void notifyDpRoleUpdated(
		Collection<User> notifyUsers,
		DistributionProtocol dp,
		Map<String, Object> props,
		String instSiteName,
		String roleOp,
		String dpOp,
		int notifRcptType,     // 0: user, 1: site, 2: institute
		int roleChoice) {     // for users -  1: PI / 2: Coordinator, for others - 1: distributing / 2: receiving

		if (CollectionUtils.isEmpty(notifyUsers)) {
			return;
		}

		String notifMessage = getNotifMsg(dp.getShortTitle(), instSiteName, roleOp, dpOp, notifRcptType, roleChoice);
		props.put("emailText", notifMessage);
		for (User rcpt : notifyUsers) {
			props.put("rcpt", rcpt);
			EmailUtil.getInstance().sendEmail(ROLE_UPDATED_EMAIL_TMPL, new String[] {rcpt.getEmailAddress()}, null, props);
		}

		Notification notif = new Notification();
		notif.setEntityType(DistributionProtocol.getEntityName());
		notif.setEntityId(dp.getId());
		notif.setOperation("UPDATE");
		notif.setCreatedBy(AuthUtil.getCurrentUser());
		notif.setCreationTime(Calendar.getInstance().getTime());
		notif.setMessage(notifMessage);
		NotifUtil.getInstance().notify(notif, Collections.singletonMap("dp-overview", notifyUsers));
	}

	private String getNotifMsg(String shortTitle, String instSiteName, String roleOp, String dpOp, int notifRcptType, int roleChoice) {
		String msgKey;
		Object[] params;

		if (dpOp == "DELETE") {
			if (notifRcptType == N_USER) {
				msgKey = "dp_delete_user_notif";
				params = new Object[] { shortTitle, roleChoice};
			} else {
				msgKey = "dp_delete_site_inst_notif";
				params = new Object[] {shortTitle, notifRcptType, instSiteName, roleChoice};
			}
		} else {
			if (notifRcptType == N_USER) {
				msgKey = "dp_user_notif_role_" + roleOp.toLowerCase();
				params = new Object[] { roleChoice, shortTitle};
			} else {
				msgKey = "dp_site_inst_notif_" + roleOp.toLowerCase();
				params = new Object[] {notifRcptType, instSiteName, roleChoice, shortTitle};
			}
		}

		return MessageUtil.getInstance().getMessage(msgKey, params);
	}

	private List<User> getInstituteAdmins(Institute institute) {
		return daoFactory.getUserDao().getUsers(
			new UserListCriteria()
				.instituteName(institute.getName())
				.type("INSTITUTE")
				.activityStatus("Active")
		);
	}

	private void notifyRecvSiteAdmins(Site recvSite, DistributionProtocol dp, Map<String, Object> emailProps, String roleOp, String dpOp) {
		if (recvSite != null) {
			notifyDpRoleUpdated(recvSite.getCoordinators(), dp, emailProps, recvSite.getName(), roleOp, dpOp, N_SITE_ADMIN, N_DP_RECV_SITE);
		}
	}

	private void notifyDistSiteAdmins(Collection<DpDistributionSite> distSites, DistributionProtocol dp,
		Map<String, Object> emailProps, String roleOp, String dpOp) {

		for (DpDistributionSite distSite : distSites) {
			if (distSite.getSite() != null) {
				notifyDpRoleUpdated(distSite.getSite().getCoordinators(), dp, emailProps, distSite.getSite().getName(),
					roleOp, dpOp, N_SITE_ADMIN, N_DP_DIST_SITE);
			}
		}
	}

	private void updateOrderExtnFormContext(DistributionProtocol dp, Form oldForm, Form newForm) {
		if (Objects.equals(oldForm, newForm)) {
			return;
		}

		if (oldForm != null) {
			removeOrderExtnFormContext(dp, oldForm);
		}

		if (newForm != null) {
			addOrderExtnFormContext(dp, newForm);
		}
	}

	private void addOrderExtnFormContext(DistributionProtocol dp, Form form) {
		addOrderExtnFormContext(dp.getId(), form.getId());
	}

	private void addOrderExtnFormContext(Long dpId, Long formId) {
		CollectionProtocolSummary cp = new CollectionProtocolSummary();
		cp.setId(-1L);

		FormContextDetail detail = new FormContextDetail();
		detail.setLevel(DistributionOrder.getExtnEntityType());
		detail.setCollectionProtocol(cp);
		detail.setEntityId(dpId);
		detail.setFormId(formId);

		RequestEvent<List<FormContextDetail>> req = new RequestEvent<>(Collections.singletonList(detail));
		ResponseEvent<List<FormContextDetail>> resp = formSvc.addFormContexts(req);
		resp.throwErrorIfUnsuccessful();
	}

	private void removeOrderExtnFormContext(DistributionProtocol dp, Form form) {
		removeOrderExtnFormContext(dp.getId(), form.getId());
	}

	private void removeOrderExtnFormContext(Long dpId, Long formId) {
		RemoveFormContextOp op = new RemoveFormContextOp();
		op.setFormId(formId);
		op.setCpId(-1L);
		op.setEntityId(dpId);
		op.setEntityType(DistributionOrder.getExtnEntityType());
		op.setRemoveType(RemoveFormContextOp.RemoveType.SOFT_REMOVE);
		ResponseEvent<Boolean> resp = formSvc.removeFormContext(new RequestEvent<>(op));
		resp.throwErrorIfUnsuccessful();
	}

	private void updateSysOrderExtnFormContext(Long formId) {
		FormContextBean formCtxt = deDaoFactory.getFormDao().getFormContext(
			false,
			DistributionOrder.getExtnEntityType(),
			-1L,
			null);

		if (formCtxt != null) {
			if (formCtxt.getContainerId().equals(formId)) {
				return;
			}

			removeOrderExtnFormContext(-1L, formCtxt.getContainerId());
		}

		if (formId != null && formId != -1L) {
			addOrderExtnFormContext(-1L, formId);
		}
	}

	private ListConfig getReservedSpecimensConfig(Map<String, Object> listReq) {
		Number dpId = (Number) listReq.get("dpId");
		if (dpId == null) {
			dpId = (Number) listReq.get("objectId");
		}

		DistributionProtocol dp = getDistributionProtocol(dpId != null ? dpId.longValue() : null);

		ListConfig cfg = ListUtil.getSpecimensListConfig(RESV_SPMNS_LIST_NAME, true);
		ListUtil.addHiddenFieldsOfSpecimen(cfg);
		if (cfg == null) {
			return null;
		}

		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(SpecimenListErrorCode.ACCESS_NOT_ALLOWED);
		}

		String restriction = "Specimen.specimenResv.dpShortTitle = \"" + dp.getShortTitle() + "\"";

		boolean useMrnSites = AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn();
		String cpSitesCond = BiospecimenDaoHelper.getInstance().getSiteCpsCondAql(siteCps, useMrnSites);
		if (StringUtils.isNotBlank(cpSitesCond)) {
			restriction += " and " + cpSitesCond;
		}

		Column orderBy = new Column();
		orderBy.setExpr("Specimen.specimenResv.id");
		orderBy.setDirection("asc");

		cfg.setDrivingForm("Specimen");
		cfg.setRestriction(restriction);
		cfg.setDistinct(true);
		cfg.setOrderBy(Collections.singletonList(orderBy));
		return ListUtil.setListLimit(cfg, listReq);
	}

	private DpRequirementDao getDprDao() {
		return daoFactory.getDistributionProtocolRequirementDao();
	}

	private Function<ExportJob, List<? extends Object>> getDpsGenerator() {
		return new Function<ExportJob, List<? extends Object>>() {
			private boolean endOfDps;

			private boolean paramsInited;

			private int startAt;

			private DpListCriteria crit;

			@Override
			public List<? extends Object> apply(ExportJob exportJob) {
				initParams();

				if (endOfDps) {
					return Collections.emptyList();
				}

				if (CollectionUtils.isNotEmpty(exportJob.getRecordIds())) {
					crit.ids(exportJob.getRecordIds());
					crit.maxResults(exportJob.getRecordIds().size() + 1);
					endOfDps = true;
				}

				List<DistributionProtocol> dps = daoFactory.getDistributionProtocolDao().getDistributionProtocols(crit.startAt(startAt));
				DeObject.createExtensions(true, DistributionProtocol.EXTN, -1L, dps);
				startAt += dps.size();
				endOfDps = dps.size() < crit.maxResults();
				return DistributionProtocolDetail.from(dps);
			}

			private void initParams() {
				if (paramsInited) {
					return;
				}

				Set<SiteCpPair> sites = AccessCtrlMgr.getInstance().getReadAccessDistributionProtocolSites();
				if ((sites != null && sites.isEmpty()) || !AccessCtrlMgr.getInstance().hasDpEximRights()) {
					endOfDps = true;
					return;
				}

				crit = new DpListCriteria().sites(sites);
				paramsInited = true;
			}
		};
	}

	private static final String ROLE_UPDATED_EMAIL_TMPL = "users_dp_role_updated";

	private static final int N_USER = 0;

	private static final int N_SITE_ADMIN = 1;

	private static final int N_INST_ADMIN = 2;

	private static final int N_DP_PI = 1;

	private static final int N_DP_COORD = 2;

	private static final int N_DP_DIST_SITE = 1;

	private static final int N_DP_RECV_SITE = 2;

	private static final String RESV_SPMNS_LIST_NAME =  "reserved-specimens-list-view";
}
