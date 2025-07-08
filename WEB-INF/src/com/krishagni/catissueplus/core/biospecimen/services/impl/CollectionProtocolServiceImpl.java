
package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.events.SiteSummary;
import com.krishagni.catissueplus.core.audit.services.impl.DeleteLogUtil;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.SpecimenUtil;
import com.krishagni.catissueplus.core.biospecimen.WorkflowUtil;
import com.krishagni.catissueplus.core.biospecimen.domain.AliquotSpecimensRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolPublishEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolPublishedVersion;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolSite;
import com.krishagni.catissueplus.core.biospecimen.domain.ConsentStatement;
import com.krishagni.catissueplus.core.biospecimen.domain.CpConsentTier;
import com.krishagni.catissueplus.core.biospecimen.domain.CpReportSettings;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig.Workflow;
import com.krishagni.catissueplus.core.biospecimen.domain.DerivedSpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Service;
import com.krishagni.catissueplus.core.biospecimen.domain.ServiceRate;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenTypeUnit;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CollectionProtocolFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ConsentStatementErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpReportSettingsFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ServiceErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ServiceFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ServiceRateErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ServiceRateFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenRequirementFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolPublishDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierOp;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierOp.OP;
import com.krishagni.catissueplus.core.biospecimen.events.CopyCpOpDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CopyCpeOpDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.CpReportSettingsDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpWorkflowCfgDetail;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.events.MergeCpDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ServiceDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ServiceRateDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenRequirementDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenTypeUnitDetail;
import com.krishagni.catissueplus.core.biospecimen.events.WorkflowDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.CollectionProtocolDao;
import com.krishagni.catissueplus.core.biospecimen.repository.CpGroupListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.CpListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.CpPublishEventListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.CprListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.ServiceListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.ServiceRateListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.BiospecimenDaoHelper;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.Tuple;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr.ParticipantReadAccess;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityResp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.Resource;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.StarredItemService;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.NotifUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.catissueplus.core.init.AppProperties;
import com.krishagni.catissueplus.core.query.Column;
import com.krishagni.catissueplus.core.query.ListConfig;
import com.krishagni.catissueplus.core.query.ListDetail;
import com.krishagni.catissueplus.core.query.ListService;
import com.krishagni.catissueplus.core.query.ListUtil;
import com.krishagni.catissueplus.core.query.Row;
import com.krishagni.rbac.common.errors.RbacErrorCode;
import com.krishagni.rbac.events.SubjectRoleOpNotif;
import com.krishagni.rbac.service.RbacService;

public class CollectionProtocolServiceImpl implements CollectionProtocolService, ObjectAccessor, InitializingBean {

	private ThreadPoolTaskExecutor taskExecutor;

	private CollectionProtocolFactory cpFactory;
	
	private CpeFactory cpeFactory;
	
	private SpecimenRequirementFactory srFactory;

	private ServiceFactory serviceFactory;

	private ServiceRateFactory serviceRateFactory;

	private DaoFactory daoFactory;
	
	private RbacService rbacSvc;

	private ListService listSvc;

	private CpReportSettingsFactory rptSettingsFactory;

	private SessionFactory sessionFactory;

	private StarredItemService starredItemSvc;

	private ExportService exportSvc;

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setCpFactory(CollectionProtocolFactory cpFactory) {
		this.cpFactory = cpFactory;
	}

	public void setCpeFactory(CpeFactory cpeFactory) {
		this.cpeFactory = cpeFactory;
	}
	
	public void setSrFactory(SpecimenRequirementFactory srFactory) {
		this.srFactory = srFactory;
	}

	public void setServiceFactory(ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	public void setServiceRateFactory(ServiceRateFactory serviceRateFactory) {
		this.serviceRateFactory = serviceRateFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setRbacSvc(RbacService rbacSvc) {
		this.rbacSvc = rbacSvc;
	}

	public void setListSvc(ListService listSvc) {
		this.listSvc = listSvc;
	}

	public void setRptSettingsFactory(CpReportSettingsFactory rptSettingsFactory) {
		this.rptSettingsFactory = rptSettingsFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setStarredItemSvc(StarredItemService starredItemSvc) {
		this.starredItemSvc = starredItemSvc;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<CollectionProtocolSummary>> getProtocols(RequestEvent<CpListCriteria> req) {
		try {
			CpListCriteria crit = addCpListCriteria(req.getPayload());
			if (crit == null) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<CollectionProtocolSummary> cps = new ArrayList<>();
			if (crit.orderByStarred()) {
				List<Long> cpIds = daoFactory.getStarredItemDao()
					.getItemIds(getObjectName(), AuthUtil.getCurrentUser().getId());
				if (!cpIds.isEmpty()) {
					crit.ids(cpIds);
					cps.addAll(daoFactory.getCollectionProtocolDao().getCollectionProtocols(crit));
					cps.forEach(cp -> cp.setStarred(true));
					crit.ids(Collections.emptyList()).notInIds(cpIds);
				}
			}

			if (cps.size() < crit.maxResults()) {
				crit.maxResults(crit.maxResults() - cps.size());
				cps.addAll(daoFactory.getCollectionProtocolDao().getCollectionProtocols(crit));
			}

			return ResponseEvent.response(cps);
		} catch (OpenSpecimenException oce) {
			return ResponseEvent.error(oce);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Long> getProtocolsCount(RequestEvent<CpListCriteria> req) {
		try {
			CpListCriteria crit = addCpListCriteria(req.getPayload());
			if (crit == null) {
				return ResponseEvent.response(0L);
			}
			
			return ResponseEvent.response(daoFactory.getCollectionProtocolDao().getCpCount(crit));
		} catch (OpenSpecimenException oce) {
			return ResponseEvent.error(oce);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<Map<String, Object>>> getCpsAndGroups(RequestEvent<String> req) {
		try {
			Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadableSiteCps();
			if (siteCps != null && siteCps.isEmpty()) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<Map<String, Object>> result = new ArrayList<>();

			String query = req.getPayload();
			CpListCriteria cpCrit = new CpListCriteria().query(query).siteCps(siteCps);
			CpGroupListCriteria cpGroupCrit = new CpGroupListCriteria().query(query).siteCps(siteCps);
			if (query != null && query.trim().length() > 0) {
				if (query.startsWith("cp_")) {
					Long cpId = Long.parseLong(query.substring(3));
					cpCrit.query(null).ids(Collections.singletonList(cpId));
					cpGroupCrit = null;
				} else if (query.startsWith("cpg_")) {
					Long groupId = Long.parseLong(query.substring(4));
					cpGroupCrit.query(null).ids(Collections.singletonList(groupId));
					cpCrit = null;
				}
			}

			if (cpCrit != null) {
				List<CollectionProtocolSummary> cps = daoFactory.getCollectionProtocolDao().getCollectionProtocols(cpCrit);
				for (CollectionProtocolSummary cp : cps) {
					Map<String, Object> cpMap = new HashMap<>();
					cpMap.put("id", "cp_" + cp.getId());
					cpMap.put("cpId", cp.getId());
					cpMap.put("label", cp.getShortTitle());
					cpMap.put("group", "Collection Protocols");
					result.add(cpMap);
				}
			}

			if (result.size() < 100 && cpGroupCrit != null) {
				cpGroupCrit.maxResults(100 - result.size());

				List<CollectionProtocolGroup> groups = daoFactory.getCpGroupDao().getGroups(cpGroupCrit);
				for (CollectionProtocolGroup group : groups) {
					Map<String, Object> groupMap = new HashMap<>();
					groupMap.put("id", "cpg_" + group.getId());
					groupMap.put("cpGroupId", group.getId());
					groupMap.put("label", group.getName());
					groupMap.put("group", "Collection Protocol Groups");
					result.add(groupMap);
				}
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
	public ResponseEvent<CollectionProtocolDetail> getCollectionProtocol(RequestEvent<CpQueryCriteria> req) {
		try {
			CpQueryCriteria crit = req.getPayload();
			CollectionProtocol cp = getCollectionProtocol(crit.getId(), crit.getTitle(), crit.getShortTitle());
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			return ResponseEvent.response(CollectionProtocolDetail.from(cp, crit.isFullObject()));
		} catch (OpenSpecimenException oce) {
			return ResponseEvent.error(oce);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<String> getCollectionProtocolDefinition(RequestEvent<CpQueryCriteria> req) {
		try {
			CollectionProtocolDetail cp = ResponseEvent.unwrap(getCollectionProtocol(req));
			Boolean includeIds = req.getPayload().param("includeIds");
			return ResponseEvent.response(toJson(cp, Boolean.TRUE.equals(includeIds)));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SiteSummary>> getSites(RequestEvent<CpQueryCriteria> req) {
		try {
			CpQueryCriteria crit = req.getPayload();
			CollectionProtocol cp = getCollectionProtocol(crit.getId(), crit.getTitle(), crit.getShortTitle());
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			List<Site> sites = cp.getSites().stream().map(CollectionProtocolSite::getSite).collect(Collectors.toList());
			return ResponseEvent.response(SiteSummary.from(sites));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<CollectionProtocolRegistrationDetail>> getRegisteredParticipants(RequestEvent<CprListCriteria> req) {
		try {
			CprListCriteria listCrit = addCprListCriteria(req.getPayload());
			if (listCrit == null) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<CollectionProtocolRegistration> cprs = daoFactory.getCprDao().getCprs(listCrit);
			createExtensions(cprs);

			Map<CollectionProtocol, Boolean> phiAccess = new HashMap<>();
			List<CollectionProtocolRegistrationDetail> result = new ArrayList<>();
			for (CollectionProtocolRegistration cpr : cprs) {
				boolean includePhi = listCrit.includePhi();
				if (includePhi && CollectionUtils.isNotEmpty(listCrit.phiSiteCps())) {
					CollectionProtocol cp = cpr.getCollectionProtocol();
					if (!phiAccess.containsKey(cp)) {
						phiAccess.put(cp, AccessCtrlMgr.getInstance().isAccessAllowed(listCrit.phiSiteCps(), cp));
					}

					includePhi = phiAccess.get(cp);
				}

				result.add(CollectionProtocolRegistrationDetail.from(cpr, !includePhi));
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
	public ResponseEvent<Long> getRegisteredParticipantsCount(RequestEvent<CprListCriteria> req) {
		try {
			CprListCriteria listCrit = addCprListCriteria(req.getPayload());
			if (listCrit == null) {
				return ResponseEvent.response(0L);
			}

			return ResponseEvent.response(daoFactory.getCprDao().getCprCount(listCrit));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolDetail> createCollectionProtocol(RequestEvent<CollectionProtocolDetail> req) {
		try {
			CollectionProtocol cp = createCollectionProtocol(req.getPayload(), null, false);
			notifyUsersOnCpCreate(cp);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(cp));
			return ResponseEvent.response(CollectionProtocolDetail.from(cp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolDetail> updateCollectionProtocol(RequestEvent<CollectionProtocolDetail> req) {
		try {
			CollectionProtocolDetail detail = req.getPayload();

			CollectionProtocol existingCp = getCollectionProtocol(detail.getId(), null, detail.getShortTitle());
			detail.setId(existingCp.getId());

			AccessCtrlMgr.getInstance().ensureUpdateCpRights(existingCp);
			CollectionProtocol cp = cpFactory.createCollectionProtocol(detail);
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			ensureUsersBelongtoCpSites(cp);
			
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueTitle(existingCp, cp, ose);
			ensureUniqueShortTitle(existingCp, cp, ose);
			ensureUniqueCode(existingCp, cp, ose);
			ensureUniqueCpSiteCode(cp, ose);
			if (existingCp.isConsentsWaived() != cp.isConsentsWaived() || cp.getConsentsSource() != null) {
				ensureConsentTierIsEmpty(existingCp, ose);
			}

			ose.checkAndThrow();

			Set<Site> addedSites = Utility.subtract(cp.getRepositories(), existingCp.getRepositories());
			Set<Site> removedSites = Utility.subtract(existingCp.getRepositories(), cp.getRepositories());
			ensureSitesAreNotInUse(existingCp, removedSites);

			existingCp.update(cp);
			existingCp.addOrUpdateExtension();
			
			fixSopDocumentName(existingCp);
			notifyUsersOnCpUpdate(existingCp, addedSites, removedSites);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(existingCp));
			return ResponseEvent.response(CollectionProtocolDetail.from(existingCp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolDetail> copyCollectionProtocol(RequestEvent<CopyCpOpDetail> req) {
		try {
			CopyCpOpDetail opDetail = req.getPayload();
			Long cpId = opDetail.getCpId();
			CollectionProtocol existing = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (existing == null) {
				throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureReadCpRights(existing);
			
			CollectionProtocol cp = createCollectionProtocol(opDetail.getCp(), existing, true);
			notifyUsersOnCpCreate(cp);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(cp));
			return ResponseEvent.response(CollectionProtocolDetail.from(cp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<MergeCpDetail> mergeCollectionProtocols(RequestEvent<MergeCpDetail> req) {
		AccessCtrlMgr.getInstance().ensureUserIsAdmin();

		CollectionProtocol srcCp = getCollectionProtocol(req.getPayload().getSrcCpShortTitle());
		CollectionProtocol tgtCp = getCollectionProtocol(req.getPayload().getTgtCpShortTitle());

		ensureMergeableCps(srcCp, tgtCp);

		int maxRecords = 30;
		boolean moreRecords = true;
		while (moreRecords) {
			List<CollectionProtocolRegistration> cprs = daoFactory.getCprDao().getCprsByCpId(srcCp.getId(), 0, maxRecords);
			for (CollectionProtocolRegistration srcCpr: cprs) {
				mergeCprIntoCp(srcCpr, tgtCp);
			}

			if (cprs.size() < maxRecords) {
				moreRecords = false;
			}
		}

		return ResponseEvent.response(req.getPayload());
	}

	@PlusTransactional
	public ResponseEvent<CollectionProtocolDetail> updateConsentsWaived(RequestEvent<CollectionProtocolDetail> req) {
		try {
			CollectionProtocolDetail detail = req.getPayload();
			CollectionProtocol existingCp = daoFactory.getCollectionProtocolDao().getById(detail.getId());
			if (existingCp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(existingCp);
			
			if (CollectionUtils.isNotEmpty(existingCp.getConsentTier())) {
				return ResponseEvent.userError(CpErrorCode.CONSENT_TIER_FOUND, existingCp.getShortTitle());
			}

			existingCp.setConsentsWaived(detail.getConsentsWaived());
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(existingCp));
			return ResponseEvent.response(CollectionProtocolDetail.from(existingCp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolDetail> updateConsentsSource(RequestEvent<CollectionProtocolDetail> req) {
		try {
			CollectionProtocolDetail detail = req.getPayload();
			CollectionProtocol existingCp = getCollectionProtocol(detail.getId(), detail.getTitle(), detail.getShortTitle());
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(existingCp);
			if (detail.getConsentsSource() != null && CollectionUtils.isNotEmpty(existingCp.getConsentTier())) {
				return ResponseEvent.userError(CpErrorCode.CONSENT_TIER_FOUND, existingCp.getShortTitle());
			}

			CollectionProtocolSummary inputCs = detail.getConsentsSource();
			if (inputCs == null || (inputCs.getId() == null && StringUtils.isBlank(inputCs.getShortTitle()))) {
				existingCp.setConsentsSource(null);
			} else {
				if (Boolean.TRUE.equals(existingCp.getVisitLevelConsents())) {
					return ResponseEvent.userError(CpErrorCode.VISIT_CONSENTS_ENABLED, existingCp.getShortTitle());
				}

				if (existingCp.isConsentsWaived()) {
					return ResponseEvent.userError(CpErrorCode.CONSENTS_WAIVED, existingCp.getShortTitle());
				}

				CollectionProtocol consentsSource = getCollectionProtocol(inputCs.getId(), null, inputCs.getShortTitle());
				if (Boolean.TRUE.equals(consentsSource.getVisitLevelConsents())) {
					return ResponseEvent.userError(CpErrorCode.VISIT_CONSENTS_ENABLED, consentsSource.getShortTitle());
				}

				if (consentsSource.isConsentsWaived()) {
					return ResponseEvent.userError(CpErrorCode.CONSENTS_WAIVED, existingCp.getShortTitle());
				}

				existingCp.setConsentsSource(consentsSource);
			}

			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(existingCp));
			return ResponseEvent.response(CollectionProtocolDetail.from(existingCp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getCpDependentEntities(RequestEvent<Long> req) {
		try {
			CollectionProtocol existingCp = daoFactory.getCollectionProtocolDao().getById(req.getPayload());
			if (existingCp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}
			
			return ResponseEvent.response(existingCp.getDependentEntities());
 		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<BulkDeleteEntityResp<CollectionProtocolDetail>> deleteCollectionProtocols(RequestEvent<BulkDeleteEntityOp> req) {
		try {
			BulkDeleteEntityOp crit = req.getPayload();

			Set<Long> cpIds = crit.getIds();
			List<CollectionProtocol> cps = daoFactory.getCollectionProtocolDao().getByIds(cpIds);
			if (crit.getIds().size() != cps.size()) {
				cps.forEach(cp -> cpIds.remove(cp.getId()));
				throw OpenSpecimenException.userError(CpErrorCode.DOES_NOT_EXIST, cpIds);
			}

			cps.forEach(cp -> AccessCtrlMgr.getInstance().ensureDeleteCpRights(cp));
			boolean completed = crit.isForceDelete() ? forceDeleteCps(cps, crit.getReason()) : deleteCps(cps, crit.getReason());
			BulkDeleteEntityResp<CollectionProtocolDetail> resp = new BulkDeleteEntityResp<>();
			resp.setCompleted(completed);
			resp.setEntities(CollectionProtocolDetail.from(cps));
			return ResponseEvent.response(resp);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<File> getSopDocument(RequestEvent<Long> req) {
		try {
			Long cpId = req.getPayload();
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (cp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			String filename = cp.getSopDocumentName();
			File file = null;
			if (StringUtils.isNotBlank(filename)) {
				file = new File(getSopDocDir() + filename);
				if (!file.exists()) {
					filename = filename.split("_", 2)[1];
					return ResponseEvent.userError(CpErrorCode.SOP_DOC_MOVED_OR_DELETED, cp.getShortTitle(), filename);
				}
			}

			if (file == null) {
				return ResponseEvent.userError(CpErrorCode.SOP_DOC_NOT_FOUND, cp.getShortTitle());
			}

			return ResponseEvent.response(file);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<String> uploadSopDocument(RequestEvent<FileDetail> req) {
		OutputStream out = null;

		try {
			FileDetail detail = req.getPayload();
			String filename = UUID.randomUUID() + "_" + detail.getFilename();

			File file = new File(getSopDocDir() + filename);
			out = new FileOutputStream(file);
			IOUtils.copy(detail.getFileIn(), out);

			return ResponseEvent.response(filename);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolDetail> importCollectionProtocol(RequestEvent<CollectionProtocolDetail> req) {
		try {
			CollectionProtocolDetail cpDetail = req.getPayload();
			cpDetail.setDistributionProtocols(null);
			
			ResponseEvent<CollectionProtocolDetail> resp = createCollectionProtocol(req);
			resp.throwErrorIfUnsuccessful();

			Long cpId = resp.getPayload().getId();
			importConsents(cpId, cpDetail.getConsents());
			importEvents(cpDetail.getTitle(), cpDetail.getEvents());
			importWorkflows(cpId, cpDetail.getWorkflows());
			importSpecimenUnits(resp.getPayload().getShortTitle(), cpDetail.getUnits());
			
			return resp;
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> isSpecimenBarcodingEnabled() {
		try {
			boolean isEnabled = ConfigUtil.getInstance().getBoolSetting(
					ConfigParams.MODULE, ConfigParams.ENABLE_SPMN_BARCODING, false);

			if (!isEnabled) {
				isEnabled = daoFactory.getCollectionProtocolDao().anyBarcodingEnabledCpExists();
			}

			return ResponseEvent.response(isEnabled);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<ConsentTierDetail>> getConsentTiers(RequestEvent<Long> req) {
		Long cpId = req.getPayload();

		try {
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (cp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);
			Set<CpConsentTier> consents = cp.getConsentsSource() != null ? cp.getConsentsSource().getConsentTier() : cp.getConsentTier();
			return ResponseEvent.response(ConsentTierDetail.from(consents));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ConsentTierDetail> updateConsentTier(RequestEvent<ConsentTierOp> req) {
		try {
			ConsentTierOp opDetail = req.getPayload();
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(opDetail.getCpId());
			if (cp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			if (cp.isConsentsWaived()) {
				return ResponseEvent.userError(CpErrorCode.CONSENTS_WAIVED, cp.getShortTitle());
			}

			if (cp.getConsentsSource() != null) {
				return ResponseEvent.userError(CpErrorCode.CONSENTS_SOURCED, cp.getConsentsSource().getShortTitle());
			}
			
			ConsentTierDetail input = opDetail.getConsentTier();
			CpConsentTier resp = null;			
			ConsentStatement stmt = null;
			switch (opDetail.getOp()) {
				case ADD -> {
					ensureUniqueConsentStatement(input, cp);
					stmt = getStatement(input.getStatementId(), input.getStatementCode(), input.getStatement());
					resp = cp.addConsentTier(getConsentTierObj(input.getId(), stmt));
				}
				case UPDATE -> {
					ensureUniqueConsentStatement(input, cp);
					stmt = getStatement(input.getStatementId(), input.getStatementCode(), input.getStatement());
					resp = cp.updateConsentTier(getConsentTierObj(input.getId(), stmt));
				}
				case REMOVE -> resp = cp.removeConsentTier(input.getId());
			}
			
			if (resp != null) {
				daoFactory.getCollectionProtocolDao().saveOrUpdate(cp, true);
				EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(cp));
			}
						
			return ResponseEvent.response(ConsentTierDetail.from(resp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}		
	}	
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getConsentDependentEntities(RequestEvent<ConsentTierDetail> req) {
		try {
			ConsentTierDetail consentTierDetail = req.getPayload();
			CpConsentTier consentTier = getConsentTier(consentTierDetail);
			return ResponseEvent.response(consentTier.getDependentEntities());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch(Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<CollectionProtocolEventDetail>> getProtocolEvents(RequestEvent<EntityQueryCriteria> req) {
		EntityQueryCriteria crit = req.getPayload();
		
		try {
			CollectionProtocol cp = getCollectionProtocol(crit.getId(), null, crit.getName());
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			List<CollectionProtocolEvent> events = cp.getOrderedCpeList();
			if (!Boolean.TRUE.equals(crit.paramBoolean("includeClosedEvents"))) {
				events = events.stream().filter(event -> !event.isClosed()).collect(Collectors.toList());
			}
			return ResponseEvent.response(CollectionProtocolEventDetail.from(events));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}		
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolEventDetail> getProtocolEvent(RequestEvent<Long> req) {
		Long cpeId = req.getPayload();
		
		try {
			CollectionProtocolEvent cpe = daoFactory.getCollectionProtocolDao().getCpe(cpeId);
			if (cpe == null) {
				return ResponseEvent.userError(CpeErrorCode.NOT_FOUND, cpeId, 1);
			}

			CollectionProtocol cp = cpe.getCollectionProtocol();
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			if (cpe.getEventPoint() != null) {
				CollectionProtocolEvent firstEvent = cp.firstEvent();
				if (firstEvent.getEventPoint() != null) {
					cpe.setOffset(firstEvent.getEventPoint());
					cpe.setOffsetUnit(firstEvent.getEventPointUnit());
				}
			}

			return ResponseEvent.response(CollectionProtocolEventDetail.from(cpe));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		}  catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
		
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolEventDetail> addEvent(RequestEvent<CollectionProtocolEventDetail> req) {
		try {
			CollectionProtocolEvent cpe = cpeFactory.createCpe(req.getPayload());
			CollectionProtocol cp = cpe.getCollectionProtocol();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			
			cp.addCpe(cpe);
			daoFactory.getCollectionProtocolDao().saveOrUpdate(cp, true);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(cp));
			return ResponseEvent.response(CollectionProtocolEventDetail.from(cpe));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolEventDetail> updateEvent(RequestEvent<CollectionProtocolEventDetail> req) {
		try {
			CollectionProtocolEvent cpe = cpeFactory.createCpe(req.getPayload());			
			CollectionProtocol cp = cpe.getCollectionProtocol();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			
			cp.updateCpe(cpe);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(cp));
			return ResponseEvent.response(CollectionProtocolEventDetail.from(cpe));			
		} catch (OpenSpecimenException ose) {		
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolEventDetail> copyEvent(RequestEvent<CopyCpeOpDetail> req) {
		try {
			CollectionProtocolDao cpDao = daoFactory.getCollectionProtocolDao();
			
			CopyCpeOpDetail opDetail = req.getPayload();
			String cpTitle = opDetail.getCollectionProtocol();
			String eventLabel = opDetail.getEventLabel();
			
			CollectionProtocolEvent existing = null;
			Object key = null;
			if (opDetail.getEventId() != null) {
				existing = cpDao.getCpe(opDetail.getEventId());
				key = opDetail.getEventId();
			} else if (!StringUtils.isBlank(eventLabel) && !StringUtils.isBlank(cpTitle)) {
				existing = cpDao.getCpeByEventLabel(cpTitle, eventLabel);
				key = eventLabel;
			}
			
			if (existing == null) {
				throw OpenSpecimenException.userError(CpeErrorCode.NOT_FOUND, key, 1);
			}
			
			CollectionProtocol cp = existing.getCollectionProtocol();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			
			CollectionProtocolEvent cpe = cpeFactory.createCpeCopy(opDetail.getCpe(), existing);
			existing.copySpecimenRequirementsTo(cpe);			
			
			cp.addCpe(cpe);			
			cpDao.saveOrUpdate(cp, true);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(cp));
			return ResponseEvent.response(CollectionProtocolEventDetail.from(cpe));			
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolEventDetail> deleteEvent(RequestEvent<Long> req) {
		try {
			Long cpeId = req.getPayload();
			CollectionProtocolEvent cpe = daoFactory.getCollectionProtocolDao().getCpe(cpeId);
			if (cpe == null) {
				return ResponseEvent.userError(CpeErrorCode.NOT_FOUND, cpeId, 1);
			}
			
			CollectionProtocol cp = cpe.getCollectionProtocol();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);

			int count = daoFactory.getCollectionProtocolDao().getAllVisitsCount(cpe.getId());
			if (count > 0) {
				return ResponseEvent.userError(CpeErrorCode.HAS_VISITS, cpe.getEventLabel(), count);
			}

			cpe.delete();
			daoFactory.getCollectionProtocolDao().saveCpe(cpe);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(cp));
			return ResponseEvent.response(CollectionProtocolEventDetail.from(cpe));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
		
	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenRequirementDetail>> getSpecimenRequirments(RequestEvent<Tuple> req) {
		try {
			Tuple tuple = req.getPayload();
			Long cpId = tuple.element(0);
			Long cpeId = tuple.element(1);
			String cpeLabel = tuple.element(2);
			boolean includeChildren = tuple.element(3) == null || (Boolean) tuple.element(3);

			List<CollectionProtocolEvent> cpes = null;
			CollectionProtocolEvent cpe = null;
			Object key = null;
			if (cpeId != null) {
				cpe = daoFactory.getCollectionProtocolDao().getCpe(cpeId);
				key = cpeId;
			} else if (StringUtils.isNotBlank(cpeLabel)) {
				if (cpId == null) {
					return ResponseEvent.userError(CpErrorCode.REQUIRED);
				}

				cpe = daoFactory.getCollectionProtocolDao().getCpeByEventLabel(cpId, cpeLabel);
				key = cpeLabel;
			} else if (cpId != null) {
				CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
				if (cp == null) {
					return ResponseEvent.userError(CpErrorCode.NOT_FOUND, cpId);
				}

				AccessCtrlMgr.getInstance().ensureReadCpRights(cp);
				cpes = cp.getOrderedCpeList();
			}


			if (cpes == null) {
				if (key == null) {
					return ResponseEvent.response(Collections.emptyList());
				} else if (cpe == null) {
					return ResponseEvent.userError(CpeErrorCode.NOT_FOUND, key, 1);
				}

				AccessCtrlMgr.getInstance().ensureReadCpRights(cpe.getCollectionProtocol());
				cpes = Collections.singletonList(cpe);
			}


			List<SpecimenRequirementDetail> result = result = cpes.stream()
				.flatMap(e -> SpecimenRequirementDetail.from(e.getTopLevelAnticipatedSpecimens(), includeChildren).stream())
				.collect(Collectors.toList());
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		}  catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenRequirementDetail> getSpecimenRequirement(RequestEvent<Long> req) {
		Long reqId = req.getPayload();
		try {
			SpecimenRequirement sr = daoFactory.getSpecimenRequirementDao().getById(reqId);
			if (sr == null) {
				return ResponseEvent.userError(SrErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureReadCpRights(sr.getCollectionProtocol());

			SpecimenRequirementDetail result = SpecimenRequirementDetail.from(sr);
			if (MapUtils.isNotEmpty(sr.getDefaultCustomFieldValues())) {
				DeObject extn = DeObject.fromValueMap(result.getCpId(), Specimen.EXTN, sr.getDefaultCustomFieldValues());
				result.setExtensionDetail(ExtensionDetail.from(extn, false, true));
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
	public ResponseEvent<SpecimenRequirementDetail> addSpecimenRequirement(RequestEvent<SpecimenRequirementDetail> req) {
		try {
			SpecimenRequirement requirement = srFactory.createSpecimenRequirement(req.getPayload());			
			CollectionProtocolEvent cpe = requirement.getCollectionProtocolEvent();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cpe.getCollectionProtocol());
			
			cpe.addSpecimenRequirement(requirement);
			daoFactory.getCollectionProtocolDao().saveCpe(cpe, true);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(cpe.getCollectionProtocol()));
			return ResponseEvent.response(SpecimenRequirementDetail.from(requirement));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenRequirementDetail>> createAliquots(RequestEvent<AliquotSpecimensRequirement> req) {
		try {
			return ResponseEvent.response(SpecimenRequirementDetail.from(createAliquots(req.getPayload())));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenRequirementDetail> createDerived(RequestEvent<DerivedSpecimenRequirement> req) {
		try {
			DerivedSpecimenRequirement requirement = req.getPayload();
			SpecimenRequirement derived = srFactory.createDerived(requirement);						
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(derived.getCollectionProtocol());
			ensureSrIsNotClosed(derived.getParentSpecimenRequirement());

			if (StringUtils.isNotBlank(derived.getCode())) {
				if (derived.getCollectionProtocolEvent().getSrByCode(derived.getCode()) != null) {
					return ResponseEvent.userError(SrErrorCode.DUP_CODE, derived.getCode());
				}
			}
			
			daoFactory.getSpecimenRequirementDao().saveOrUpdate(derived, true);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(derived.getCollectionProtocol()));
			return ResponseEvent.response(SpecimenRequirementDetail.from(derived));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenRequirementDetail> updateSpecimenRequirement(RequestEvent<SpecimenRequirementDetail> req) {
		try {
			SpecimenRequirementDetail detail = req.getPayload();
			Long srId = detail.getId();
			String cpShortTitle = detail.getCpShortTitle();
			String eventLabel = detail.getEventLabel();
			String code = detail.getCode();

			Object key = null;
			SpecimenRequirement sr = null;
			if (srId != null) {
				key = srId;
				sr = daoFactory.getSpecimenRequirementDao().getById(srId);
			} else if (StringUtils.isNotBlank(cpShortTitle) && StringUtils.isNotBlank(eventLabel) && StringUtils.isNotBlank(code)) {
				key = cpShortTitle + ":" + eventLabel + ":" + code;
				sr = daoFactory.getSpecimenRequirementDao().getByCpEventLabelAndSrCode(cpShortTitle, eventLabel, code);
			}

			if (key == null) {
				return ResponseEvent.userError(SrErrorCode.ID_CODE_REQ);
			} else if (sr == null) {
				return ResponseEvent.userError(SrErrorCode.NOT_FOUND, key);
			}

			AccessCtrlMgr.getInstance().ensureUpdateCpRights(sr.getCollectionProtocol());
			SpecimenRequirement partial = srFactory.createForUpdate(sr, detail);
			if (isSpecimenClassOrTypeChanged(sr, partial)) {
				ensureSpecimensNotCollected(sr);
			}
			
			sr.update(partial);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(sr.getCollectionProtocol()));
			return ResponseEvent.response(SpecimenRequirementDetail.from(sr));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenRequirementDetail> copySpecimenRequirement(RequestEvent<Long> req) {
		try {
			Long srId = req.getPayload();
			
			SpecimenRequirement sr = daoFactory.getSpecimenRequirementDao().getById(srId);
			if (sr == null) {
				throw OpenSpecimenException.userError(SrErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(sr.getCollectionProtocol());
			SpecimenRequirement copy = sr.deepCopy(null);
			daoFactory.getSpecimenRequirementDao().saveOrUpdate(copy, true);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(sr.getCollectionProtocol()));
			return ResponseEvent.response(SpecimenRequirementDetail.from(copy));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenRequirementDetail> deleteSpecimenRequirement(RequestEvent<Long> req) {
		try {
			Long srId = req.getPayload();
			SpecimenRequirement sr = daoFactory.getSpecimenRequirementDao().getById(srId);
			if (sr == null) {
				return ResponseEvent.userError(SrErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureUpdateCpRights(sr.getCollectionProtocol());

			int count = daoFactory.getSpecimenRequirementDao().getAllSpecimensCount(sr.getId());
			if (count > 0) {
				String code = sr.getCode();
				if (StringUtils.isBlank(code)) {
					code = sr.getSpecimenType().getValue() + " (" + sr.getId() + ")";
				}

				return ResponseEvent.userError(SrErrorCode.HAS_SPECIMENS, code, count);
			}

			sr.delete();
			daoFactory.getSpecimenRequirementDao().saveOrUpdate(sr);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(sr.getCollectionProtocol()));
			return ResponseEvent.response(SpecimenRequirementDetail.from(sr));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Integer> getSrSpecimensCount(RequestEvent<Long> req) {
		try {
			Long srId = req.getPayload();
			SpecimenRequirement sr = daoFactory.getSpecimenRequirementDao().getById(srId);
			if (sr == null) {
				throw OpenSpecimenException.userError(SrErrorCode.NOT_FOUND);
			}
			
			return ResponseEvent.response(daoFactory.getSpecimenRequirementDao().getSpecimensCount(srId));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpReportSettingsDetail> getReportSettings(RequestEvent<CpQueryCriteria> req) {
		try {
			CpReportSettings settings = getReportSetting(req.getPayload());
			if (settings == null) {
				return ResponseEvent.response(null);
			}

			AccessCtrlMgr.getInstance().ensureReadCpRights(settings.getCp());
			return ResponseEvent.response(CpReportSettingsDetail.from(settings));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpReportSettingsDetail> saveReportSettings(RequestEvent<CpReportSettingsDetail> req) {
		try {
			CpReportSettings settings = rptSettingsFactory.createSettings(req.getPayload());
			CollectionProtocol cp = settings.getCp();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);

			CpReportSettings existing = daoFactory.getCpReportSettingsDao().getByCp(cp.getId());
			if (existing == null) {
				existing = settings;
			} else {
				existing.update(settings);
			}

			daoFactory.getCpReportSettingsDao().saveOrUpdate(existing);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(cp));
			return ResponseEvent.response(CpReportSettingsDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpReportSettingsDetail> deleteReportSettings(RequestEvent<CpQueryCriteria> req) {
		try {
			CpReportSettings settings = getReportSetting(req.getPayload());
			if (settings == null) {
				return ResponseEvent.response(null);
			}

			AccessCtrlMgr.getInstance().ensureUpdateCpRights(settings.getCp());
			settings.delete();
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(settings.getCp()));
			return ResponseEvent.response(CpReportSettingsDetail.from(settings));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> generateReport(RequestEvent<CpQueryCriteria> req) {
		try {
			CpQueryCriteria crit = req.getPayload();
			CollectionProtocol cp = getCollectionProtocol(crit.getId(), crit.getTitle(), crit.getShortTitle());
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			CpReportSettings cpSettings = daoFactory.getCpReportSettingsDao().getByCp(cp.getId());
			if (cpSettings != null && !cpSettings.isEnabled()) {
				return ResponseEvent.userError(CpErrorCode.RPT_DISABLED, cp.getShortTitle());
			}

			taskExecutor.execute(new CpReportTask(cp.getId()));
			return ResponseEvent.response(true);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<File> getReportFile(Long cpId, String fileId) {
		try {
			CollectionProtocol cp = getCollectionProtocol(cpId, null, null);
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			File file = new CpReportGenerator().getDataFile(cpId, fileId);
			if (file == null) {
				return ResponseEvent.userError(CpErrorCode.RPT_FILE_NOT_FOUND);
			}

			return ResponseEvent.response(file);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpWorkflowCfgDetail> getWorkflows(RequestEvent<Long> req) {
		Long cpId = req.getPayload();
		CollectionProtocol cp = null;

		CpWorkflowConfig cfg;
		if (cpId == null || cpId == -1L) {
			cfg = WorkflowUtil.getInstance().getSysWorkflows();
		} else {
			cp = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (cp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}

			cfg = daoFactory.getCollectionProtocolDao().getCpWorkflows(cpId);
		}

		if (cfg == null) {
			cfg = new CpWorkflowConfig();
			cfg.setCp(cp);
		}
		
		return ResponseEvent.response(CpWorkflowCfgDetail.from(cfg));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpWorkflowCfgDetail> saveWorkflows(RequestEvent<CpWorkflowCfgDetail> req) {
		try {
			CpWorkflowCfgDetail input = req.getPayload();
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(input.getCpId());
			if (cp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			CpWorkflowConfig cfg = saveWorkflows(cp, input);
			return ResponseEvent.response(CpWorkflowCfgDetail.from(cfg));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<CollectionProtocolSummary>> getRegisterEnabledCps(
			List<String> siteNames, String searchTitle, int maxResults) {
		try {
			Set<Long> cpIds = AccessCtrlMgr.getInstance().getRegisterEnabledCpIds(siteNames);
			
			CpListCriteria crit = new CpListCriteria().title(searchTitle).maxResults(maxResults);
			if (cpIds != null && cpIds.isEmpty()) {
				return ResponseEvent.response(Collections.<CollectionProtocolSummary>emptyList());
			} else if (cpIds != null) {
				crit.ids(new ArrayList<>(cpIds));
			}

			return ResponseEvent.response(daoFactory.getCollectionProtocolDao().getCollectionProtocols(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ListConfig> getCpListCfg(RequestEvent<Map<String, Object>> req) {
		return listSvc.getListCfg(req);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ListDetail> getList(RequestEvent<Map<String, Object>> req) {
		return listSvc.getList(req);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> getListSize(RequestEvent<Map<String, Object>> req) {
		return listSvc.getListSize(req);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Collection<Object>> getListExprValues(RequestEvent<Map<String, Object>> req) {
		return listSvc.getListExprValues(req);
	}

	@Override
	@PlusTransactional
	public boolean toggleStarredCp(Long cpId, boolean starred) {
		try {
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (cp == null) {
				throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND, cpId);
			}

			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);
			if (starred) {
				starredItemSvc.save(getObjectName(), cp.getId());
			} else {
				starredItemSvc.delete(getObjectName(), cp.getId());
			}

			return true;
		} catch (Exception e) {
			if (e instanceof OpenSpecimenException) {
				throw e;
			}

			throw OpenSpecimenException.serverError(e);
		}
	}

	@Override
	public String getObjectName() {
		return CollectionProtocol.getEntityName();
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getCollectionProtocolDao().getCpIds(key, value);
	}

	@Override
	public String getAuditTable() {
		return "CAT_COLLECTION_PROTOCOL_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		CollectionProtocol cp = getCollectionProtocol(id, null, null);
		AccessCtrlMgr.getInstance().ensureReadCpRights(cp);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		listSvc.registerListConfigurator("cp-list-view", this::getCpListConfig);
		listSvc.registerListConfigurator("participant-list-view", this::getParticipantsListConfig);
		listSvc.registerListConfigurator("specimen-list-view", this::getSpecimenListConfig);

		exportSvc.registerObjectsGenerator("cp", this::getCpsGenerator);
		exportSvc.registerObjectsGenerator("cpe", this::getEventsGenerator);
		exportSvc.registerObjectsGenerator("sr", this::getSpecimenRequirementsGenerator);
	}


	@Override
	public CpWorkflowConfig saveWorkflows(CollectionProtocol cp, CpWorkflowCfgDetail input) {
		CpWorkflowConfig cfg = daoFactory.getCollectionProtocolDao().getCpWorkflows(cp.getId());
		if (cfg == null) {
			cfg = new CpWorkflowConfig();
			cfg.setCp(cp);
		}

		if (!input.isPatch()) {
			cfg.getWorkflows().clear();
		}

		if (input.getWorkflows() != null) {
			for (WorkflowDetail detail : input.getWorkflows().values()) {
				Workflow wf = new Workflow();
				BeanUtils.copyProperties(detail, wf);
				cfg.getWorkflows().put(wf.getName(), wf);
			}
		}

		daoFactory.getCollectionProtocolDao().saveCpWorkflows(cfg);
		EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(cp));
		return cfg;
	}

	//
	// Publication APIs
	//

	@Override
	@PlusTransactional
	public ResponseEvent<List<CollectionProtocolPublishDetail>> getPublishEvents(RequestEvent<CpPublishEventListCriteria> req) {
		try {
			CpPublishEventListCriteria crit = req.getPayload();
			if (crit.cpId() == null) {
				return ResponseEvent.userError(CpErrorCode.REQUIRED);
			}

			CollectionProtocol cp = getCollectionProtocol(crit.cpId(), null, null);
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			List<CollectionProtocolPublishEvent> events = daoFactory.getCollectionProtocolPublishEventDao().getEvents(crit);
			return ResponseEvent.response(CollectionProtocolPublishDetail.from(events));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolPublishDetail> publish(RequestEvent<CollectionProtocolPublishDetail> req) {
		try {
			CollectionProtocolPublishDetail input = req.getPayload();
			CollectionProtocol cp = getCollectionProtocol(input.getCpId(), input.getCpShortTitle(), input.getCpShortTitle());
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);

			if (!cp.isDraftMode()) {
				return ResponseEvent.userError(CpErrorCode.NOT_IN_DRAFT, cp.getShortTitle());
			}

			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			CollectionProtocolPublishEvent publishEvent = createPublishEvent(cp, input, ose);
			ose.checkAndThrow();

			cp.setDraftMode(false);
			CollectionProtocolPublishedVersion version = new CollectionProtocolPublishedVersion();
			version.setDefinition(toJson(cp, true));
			daoFactory.getCollectionProtocolPublishEventDao().saveOrUpdate(version, true);

			publishEvent.setPublishedVersion(version);
			daoFactory.getCollectionProtocolPublishEventDao().saveOrUpdate(publishEvent, true);

			CollectionProtocolPublishDetail output = CollectionProtocolPublishDetail.from(publishEvent);

			Map<String, Object> emailProps = new HashMap<>();
			emailProps.put("cp", cp);
			emailProps.put("version", output);
			emailProps.put("$subject", new Object[] { cp.getShortTitle() });

			Set<User> notifUsers = new LinkedHashSet<>();
			notifUsers.add(cp.getPrincipalInvestigator());
			notifUsers.addAll(cp.getCoordinators());
			notifUsers.add(publishEvent.getPublishedBy());
			notifUsers.addAll(publishEvent.getReviewers());
			for (User user : notifUsers) {
				emailProps.put("rcpt", user);
				EmailUtil.getInstance().sendEmail(CP_PUBLISHED_EMAIL_TMPL, new String[] { user.getEmailAddress() }, null, emailProps);
			}

			return ResponseEvent.response(output);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<String> getPublishedVersion(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			CollectionProtocolPublishEvent event = daoFactory.getCollectionProtocolPublishEventDao().getById(crit.getId());
			if (event == null) {
				return ResponseEvent.userError(CpErrorCode.INVALID_PUB_ID, crit.getId());
			}

			CollectionProtocol cp = event.getCp();
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);
			Number cpId = crit.paramNumber("cpId");
			if (cpId != null && !cpId.equals(cp.getId())) {
				return ResponseEvent.userError(CommonErrorCode.INVALID_INPUT, "Published event does not belong to the CP: " + cpId);
			}

			return ResponseEvent.response(event.getPublishedVersion().getDefinition());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ServiceDetail>> getServices(RequestEvent<ServiceListCriteria> req) {
		try {
			ServiceListCriteria crit = req.getPayload();
			if (crit.cpId() == null || crit.cpId() <= 0L) {
				return ResponseEvent.userError(CpErrorCode.ID_REQ);
			}

			CollectionProtocol cp = getCollectionProtocol(crit.cpId(), null, null);
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			List<Service> services = daoFactory.getServiceDao().getServices(crit);
			if (crit.rateEffectiveOn() != null) {
				Map<Long, ServiceDetail> result = new LinkedHashMap<>();
				for (Service svc : services) {
					result.put(svc.getId(), ServiceDetail.from(svc));
				}

				ServiceRateListCriteria rlCrit = new ServiceRateListCriteria()
					.serviceIds(result.keySet())
					.effectiveDate(crit.rateEffectiveOn());
				List<ServiceRate> rates = daoFactory.getServiceRateDao().getRates(rlCrit);
				for (ServiceRate rate : rates) {
					result.get(rate.getService().getId()).setRate(rate.getRate());
				}

				return ResponseEvent.response(new ArrayList<>(result.values()));
			}

			return ResponseEvent.response(ServiceDetail.from(services));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ServiceDetail> createService(RequestEvent<ServiceDetail> req) {
		try {
			ServiceDetail input = req.getPayload();
			Service service = serviceFactory.createService(input);
			service.setId(null);
			if (!Status.isActiveStatus(service.getActivityStatus())) {
				return ResponseEvent.userError(ActivityStatusErrorCode.INVALID, service.getActivityStatus());
			}

			AccessCtrlMgr.getInstance().ensureUpdateCpRights(service.getCp());

			ensureUniqueServiceCode(null, service);
			daoFactory.getServiceDao().saveOrUpdate(service, true);
			if (input.getRateStartDate() != null && input.getRate() != null) {
				ServiceRateDetail rate = new ServiceRateDetail();
				rate.setServiceId(service.getId());
				rate.setStartDate(input.getRateStartDate());
				rate.setEndDate(input.getRateEndDate());
				rate.setRate(input.getRate());
				ResponseEvent.unwrap(addServiceRates(RequestEvent.wrap(Collections.singletonList(rate))));
			}

			return ResponseEvent.response(ServiceDetail.from(service));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ServiceDetail> updateService(RequestEvent<ServiceDetail> req) {
		try {
			Service service = serviceFactory.createService(req.getPayload());
			Service existing = getService(service.getId(), service.getCp().getId(), service.getCp().getShortTitle(), service.getCode());
			if (!service.getCp().equals(existing.getCp())) {
				return ResponseEvent.userError(ServiceErrorCode.CP_CHG_NA);
			}

			AccessCtrlMgr.getInstance().ensureUpdateCpRights(existing.getCp());

			ensureUniqueServiceCode(existing, service);
			if (Status.isDisabledStatus(service.getActivityStatus())) {
				ensureServiceIsNotInUse(service);
			}

			existing.update(service);
			return ResponseEvent.response(ServiceDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ServiceDetail> deleteService(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			Service service = getService(crit.getId(), crit.paramLong("cpId"), crit.paramString("cpShortTitle"), crit.getName());
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(service.getCp());
			ensureServiceIsNotInUse(service);
			service.delete();
			return ResponseEvent.response(ServiceDetail.from(service));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ServiceRateDetail>> getServiceRates(RequestEvent<ServiceRateListCriteria> req) {
		try {
			ServiceRateListCriteria crit = req.getPayload();

			CollectionProtocol cp = null;
			if (crit.serviceId() != null) {
				Service service = getService(crit.serviceId(), null, null, null);
				cp = service.getCp();
			} else if (crit.cpId() != null) {
				cp = getCollectionProtocol(crit.cpId(), null, null);
			} else {
				return ResponseEvent.userError(ServiceRateErrorCode.CP_OR_SVC_ID_REQ);
			}

			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);
			List<ServiceRate> rates = daoFactory.getServiceRateDao().getRates(crit);
			return ResponseEvent.response(ServiceRateDetail.from(rates));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ServiceRateDetail>> addServiceRates(RequestEvent<List<ServiceRateDetail>> req) {
		try {
			List<ServiceRateDetail> result = new ArrayList<>();

			Set<CollectionProtocol> allowedAccess = new HashSet<>();
			for (ServiceRateDetail input : req.getPayload()) {
				ServiceRate rate = serviceRateFactory.createServiceRate(input);
				if (!allowedAccess.contains(rate.getService().getCp())) {
					AccessCtrlMgr.getInstance().ensureUpdateCpRights(rate.getService().getCp());
					allowedAccess.add(rate.getService().getCp());
				}

				ensureNoRateIntervalOverlap(rate);
				daoFactory.getServiceRateDao().saveOrUpdate(rate);
				result.add(ServiceRateDetail.from(rate));
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
	public ResponseEvent<ServiceRateDetail> updateServiceRate(RequestEvent<ServiceRateDetail> req) {
		try {
			ServiceRateDetail input = req.getPayload();
			if (input.getId() == null) {
				return ResponseEvent.userError(ServiceRateErrorCode.ID_REQ);
			}

			ServiceRate existing = daoFactory.getServiceRateDao().getById(input.getId());
			if (existing == null) {
				return ResponseEvent.userError(ServiceRateErrorCode.NOT_FOUND, input.getId());
			}

			AccessCtrlMgr.getInstance().ensureUpdateCpRights(existing.getService().getCp());

			ServiceRate rate = serviceRateFactory.createServiceRate(input);
			if (!rate.getService().equals(existing.getService())) {
				return ResponseEvent.userError(ServiceRateErrorCode.SERVICE_CHG_NA);
			}

			ensureNoRateIntervalOverlap(rate);
			existing.update(rate);
			return ResponseEvent.response(ServiceRateDetail.from(rate));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ServiceRateDetail> deleteServiceRate(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			if (crit.getId() == null) {
				return ResponseEvent.userError(ServiceRateErrorCode.ID_REQ);
			}

			ServiceRate rate = daoFactory.getServiceRateDao().getById(crit.getId());
			if (rate == null) {
				return ResponseEvent.userError(ServiceRateErrorCode.NOT_FOUND, crit.getId());
			}

			AccessCtrlMgr.getInstance().ensureUpdateCpRights(rate.getService().getCp());
			rate.delete();
			return ResponseEvent.response(ServiceRateDetail.from(rate));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private CpListCriteria addCpListCriteria(CpListCriteria crit) {
		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadableSiteCps();
		return siteCps != null && siteCps.isEmpty() ? null : crit.siteCps(siteCps);
	}

	private CprListCriteria addCprListCriteria(CprListCriteria listCrit) {
		ParticipantReadAccess access = AccessCtrlMgr.getInstance().getParticipantReadAccess(listCrit.cpId());
		if (!access.admin) {
			if (access.noAccessibleSites() || (!access.phiAccess && listCrit.hasPhiFields())) {
				return null;
			}
		}

		return listCrit.includePhi(access.phiAccess)
			.phiSiteCps(access.phiSiteCps)
			.siteCps(access.siteCps)
			.useMrnSites(AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn());
	}

	private CollectionProtocol createCollectionProtocol(CollectionProtocolDetail detail, CollectionProtocol existing, boolean createCopy) {
		CollectionProtocol cp = null;
		if (!createCopy) {
			cp = cpFactory.createCollectionProtocol(detail);
		} else {
			cp = cpFactory.createCpCopy(detail, existing);
		}
		
		AccessCtrlMgr.getInstance().ensureCreateCpRights(cp);
		ensureUsersBelongtoCpSites(cp);
		
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureUniqueTitle(null, cp, ose);
		ensureUniqueShortTitle(null, cp, ose);
		ensureUniqueCode(null, cp, ose);
		ensureUniqueCpSiteCode(cp, ose);
		ose.checkAndThrow();

		daoFactory.getCollectionProtocolDao().saveOrUpdate(cp, true);
		cp.addOrUpdateExtension();
		fixSopDocumentName(cp);
		copyWorkflows(existing, cp);
		copySpecimenUnits(existing, cp);
		return cp;
	}

	private void ensureUsersBelongtoCpSites(CollectionProtocol cp) {
		ensureCreatorBelongToCpSites(cp);
	}
	
	private void ensureCreatorBelongToCpSites(CollectionProtocol cp) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		Set<Site> cpSites = cp.getRepositories();
		String inaccessibleSites = cpSites.stream()
			.filter(cpSite -> !cpSite.getInstitute().equals(AuthUtil.getCurrentUserInstitute()))
			.map(Site::getName)
			.collect(Collectors.joining(","));
		if (!inaccessibleSites.isEmpty()) {
			throw OpenSpecimenException.userError(CpErrorCode.CREATOR_DOES_NOT_BELONG_CP_REPOS, inaccessibleSites);
		}
	}
	
	private void ensureUniqueTitle(CollectionProtocol existingCp, CollectionProtocol cp, OpenSpecimenException ose) {
		String title = cp.getTitle();
		if (existingCp != null && existingCp.getTitle().equals(title)) {
			return;
		}
		
		CollectionProtocol dbCp = daoFactory.getCollectionProtocolDao().getCollectionProtocol(title);
		if (dbCp != null) {
			ose.addError(CpErrorCode.DUP_TITLE, title);
		}		
	}
	
	private void ensureUniqueShortTitle(CollectionProtocol existingCp, CollectionProtocol cp, OpenSpecimenException ose) {
		String shortTitle = cp.getShortTitle();
		if (existingCp != null && existingCp.getShortTitle().equals(shortTitle)) {
			return;
		}
		
		CollectionProtocol dbCp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(shortTitle);
		if (dbCp != null) {
			ose.addError(CpErrorCode.DUP_SHORT_TITLE, shortTitle);
		}
	}
	
	private void ensureUniqueCode(CollectionProtocol existingCp, CollectionProtocol cp, OpenSpecimenException ose) {
		String code = cp.getCode();
		if (StringUtils.isBlank(code)) {
			return;
		}
		
		if (existingCp != null && code.equals(existingCp.getCode())) {
			return;
		}
		
		CollectionProtocol dbCp = daoFactory.getCollectionProtocolDao().getCpByCode(code);
		if (dbCp != null) {
			ose.addError(CpErrorCode.DUP_CODE, code);
		}
	}
	
	private void ensureUniqueCpSiteCode(CollectionProtocol cp, OpenSpecimenException ose) {
		List<String> codes = Utility.<List<String>>collect(cp.getSites(), "code");
		codes.removeAll(Arrays.asList(new String[] {null, ""}));
		
		Set<String> uniqueCodes = new HashSet<String>(codes);
		if (codes.size() != uniqueCodes.size()) {
			ose.addError(CpErrorCode.DUP_CP_SITE_CODES, codes);
		}
	}

	private void ensureSitesAreNotInUse(CollectionProtocol cp, Collection<Site> sites) {
		if (sites.isEmpty()) {
			return;
		}

		List<Long> siteIds = sites.stream().map(Site::getId).collect(Collectors.toList());
		Map<String, Integer> counts = daoFactory.getCprDao().getParticipantsBySite(cp.getId(), siteIds);
		if (!counts.isEmpty()) {
			String siteLabels = String.join(", ", counts.keySet());
			throw OpenSpecimenException.userError(CpErrorCode.USED_SITES, siteLabels, counts.size());
		}

		List<String> containers = daoFactory.getCollectionProtocolDao().getDependentContainers(cp.getId(), siteIds);
		if (!containers.isEmpty()) {
			throw OpenSpecimenException.userError(CpErrorCode.USED_CONT_RESTRICTION, StringUtils.join(containers, ","), containers.size());
		}
	}

	private void fixSopDocumentName(CollectionProtocol cp) {
		if (StringUtils.isBlank(cp.getSopDocumentName())) {
			return;
		}

		String[] nameParts = cp.getSopDocumentName().split("_", 2);
		if (nameParts[0].equals(cp.getId().toString())) {
			return;
		}

		try {
			UUID uuid = UUID.fromString(nameParts[0]);
		} catch (Exception e) {
			throw OpenSpecimenException.userError(CpErrorCode.INVALID_SOP_DOC, cp.getSopDocumentName());
		}

		if (StringUtils.isBlank(nameParts[1])) {
			throw OpenSpecimenException.userError(CpErrorCode.INVALID_SOP_DOC, cp.getSopDocumentName());
		}

		File src = new File(getSopDocDir() + File.separator + cp.getSopDocumentName());
		if (!src.exists()) {
			throw OpenSpecimenException.userError(CpErrorCode.SOP_DOC_MOVED_OR_DELETED, cp.getSopDocumentName(), cp.getShortTitle());
		}

		cp.setSopDocumentName(cp.getId() + "_" + nameParts[1]);

		File dest = new File(getSopDocDir() + File.separator + cp.getSopDocumentName());
		src.renameTo(dest);
	}

	private void ensureConsentTierIsEmpty(CollectionProtocol existingCp, OpenSpecimenException ose) {
		if (CollectionUtils.isNotEmpty(existingCp.getConsentTier())) {
			ose.addError(CpErrorCode.CONSENT_TIER_FOUND, existingCp.getShortTitle());
		}
	}
	
	private void importConsents(Long cpId, List<ConsentTierDetail> consents) {
		if (CollectionUtils.isEmpty(consents)) {
			return;			
		}
		
		for (ConsentTierDetail consent : consents) {
			ConsentTierOp addOp = new ConsentTierOp();
			addOp.setConsentTier(consent);
			addOp.setCpId(cpId);
			addOp.setOp(OP.ADD);

			ResponseEvent<ConsentTierDetail> resp = updateConsentTier(new RequestEvent<>(addOp));
			resp.throwErrorIfUnsuccessful();
		}
	}
	
	private void importEvents(String cpTitle, List<CollectionProtocolEventDetail> events) {
		if (CollectionUtils.isEmpty(events)) {
			return;
		}
		
		for (CollectionProtocolEventDetail event : events) {
			if (Status.isClosedOrDisabledStatus(event.getActivityStatus())) {
				continue;
			}

			event.setCollectionProtocol(cpTitle);
			ResponseEvent<CollectionProtocolEventDetail> resp = addEvent(new RequestEvent<>(event));
			resp.throwErrorIfUnsuccessful();
			
			Long eventId = resp.getPayload().getId();
			importSpecimenReqs(eventId, null, event.getSpecimenRequirements());
		}
	}
	
	private void importSpecimenReqs(Long eventId, Long parentSrId, List<SpecimenRequirementDetail> srs) {
		if (CollectionUtils.isEmpty(srs)) {
			return;
		}
		
		for (SpecimenRequirementDetail sr : srs) {
			if (Status.isClosedOrDisabledStatus(sr.getActivityStatus())) {
				continue;
			}

			sr.setEventId(eventId);
			
			if (sr.getLineage().equals(Specimen.NEW)) {
				ResponseEvent<SpecimenRequirementDetail> resp = addSpecimenRequirement(new RequestEvent<>(sr));
				resp.throwErrorIfUnsuccessful();
				
				importSpecimenReqs(eventId, resp.getPayload().getId(), sr.getChildren());
			} else if (parentSrId != null && sr.getLineage().equals(Specimen.ALIQUOT)) {				
				AliquotSpecimensRequirement aliquotReq = sr.toAliquotRequirement(parentSrId, 1);
				List<SpecimenRequirement> aliquots = createAliquots(aliquotReq);

				if (StringUtils.isNotBlank(sr.getCode())) {
					aliquots.get(0).setCode(sr.getCode());
				}
				
				importSpecimenReqs(eventId, aliquots.get(0).getId(), sr.getChildren());
			} else if (parentSrId != null && sr.getLineage().equals(Specimen.DERIVED)) {
				DerivedSpecimenRequirement derivedReq = sr.toDerivedRequirement(parentSrId);
				ResponseEvent<SpecimenRequirementDetail> resp = createDerived(new RequestEvent<DerivedSpecimenRequirement>(derivedReq));
				resp.throwErrorIfUnsuccessful();
				
				importSpecimenReqs(eventId, resp.getPayload().getId(), sr.getChildren());
			}			
		}
	}

	private void ensureSrIsNotClosed(SpecimenRequirement sr) {
		if (!sr.isClosed()) {
			return;
		}

		String key = sr.getCode();
		if (StringUtils.isBlank(key)) {
			key = sr.getName();
		}

		if (StringUtils.isBlank(key)) {
			key = sr.getId().toString();
		}

		throw OpenSpecimenException.userError(SrErrorCode.CLOSED, key);
	}

	private List<SpecimenRequirement> createAliquots(AliquotSpecimensRequirement requirement) {
		List<SpecimenRequirement> aliquots = srFactory.createAliquots(requirement);
		SpecimenRequirement aliquot = aliquots.iterator().next();
		AccessCtrlMgr.getInstance().ensureUpdateCpRights(aliquot.getCollectionProtocol());

		SpecimenRequirement parent = aliquot.getParentSpecimenRequirement();
		if (StringUtils.isNotBlank(requirement.getCode())) {
			setAliquotCode(parent, aliquots, requirement.getCode());
		}

		parent.addChildRequirements(aliquots);
		daoFactory.getSpecimenRequirementDao().saveOrUpdate(parent, true);
		EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(aliquot.getCollectionProtocol()));
		return aliquots;
	}

	private void importWorkflows(Long cpId, Map<String, WorkflowDetail> workflows) {
		CpWorkflowCfgDetail input = new CpWorkflowCfgDetail();
		input.setCpId(cpId);
		input.setWorkflows(workflows);

		ResponseEvent<CpWorkflowCfgDetail> resp = saveWorkflows(new RequestEvent<>(input));
		resp.throwErrorIfUnsuccessful();
	}

	private void copyWorkflows(CollectionProtocol srcCp, CollectionProtocol dstCp) {
		if (srcCp == null) {
			return;
		}

		CpWorkflowConfig srcWfCfg = daoFactory.getCollectionProtocolDao().getCpWorkflows(srcCp.getId());
		if (srcWfCfg != null) {
			CpWorkflowConfig newConfig = new CpWorkflowConfig();
			newConfig.setCp(dstCp);
			newConfig.setWorkflows(srcWfCfg.getWorkflows());
			daoFactory.getCollectionProtocolDao().saveCpWorkflows(newConfig);
		}
	}

	private void importSpecimenUnits(String shortTitle, List<SpecimenTypeUnitDetail> units) {
		for (SpecimenTypeUnitDetail unit : units) {
			unit.setCpShortTitle(shortTitle);
			SpecimenUtil.getInstance().saveUnit(unit);
		}
	}

	private void copySpecimenUnits(CollectionProtocol srcCp, CollectionProtocol dstCp) {
		if (srcCp == null) {
			return;
		}

		List<SpecimenTypeUnit> units = SpecimenUtil.getInstance().getUnits(srcCp.getShortTitle());
		for (SpecimenTypeUnit unit : units) {
			SpecimenTypeUnit toSave = new SpecimenTypeUnit();
			toSave.setCp(dstCp);
			toSave.setSpecimenClass(unit.getSpecimenClass());
			toSave.setType(unit.getType());
			toSave.setQuantityUnit(unit.getQuantityUnit());
			toSave.setConcentrationUnit(unit.getConcentrationUnit());
			daoFactory.getSpecimenTypeUnitDao().saveOrUpdate(toSave);
		}
	}

	private CpConsentTier getConsentTier(ConsentTierDetail consentTierDetail) {
		CollectionProtocolDao cpDao = daoFactory.getCollectionProtocolDao();
		
		CpConsentTier consentTier = null;
		if (consentTierDetail.getId() != null) {
			consentTier = cpDao.getConsentTier(consentTierDetail.getId());
		} else if (StringUtils.isNotBlank(consentTierDetail.getStatement()) && consentTierDetail.getCpId() != null ) {
			consentTier = cpDao.getConsentTierByStatement(consentTierDetail.getCpId(), consentTierDetail.getStatement());
		}
		
		if (consentTier == null) {
			throw OpenSpecimenException.userError(CpErrorCode.CONSENT_TIER_NOT_FOUND);
		}
		
		return consentTier;
	}

	private void ensureUniqueConsentStatement(ConsentTierDetail consentTierDetail, CollectionProtocol cp) {
		Predicate<CpConsentTier> findFn;
		if (consentTierDetail.getStatementId() != null) {
			findFn = (t) -> t.getStatement().getId().equals(consentTierDetail.getStatementId());
		} else if (StringUtils.isNotBlank(consentTierDetail.getStatementCode())) {
			findFn = (t) -> t.getStatement().getCode().equals(consentTierDetail.getStatementCode());
		} else if (StringUtils.isNotBlank(consentTierDetail.getStatement())) {
			findFn = (t) -> t.getStatement().getStatement().equals(consentTierDetail.getStatement());
		} else {
			throw OpenSpecimenException.userError(ConsentStatementErrorCode.CODE_REQUIRED);
		}

		CpConsentTier tier = cp.getConsentTier().stream().filter(findFn).findFirst().orElse(null);
		if (tier != null && !tier.getId().equals(consentTierDetail.getId())) {
			throw OpenSpecimenException.userError(CpErrorCode.DUP_CONSENT, tier.getStatement().getCode(), cp.getShortTitle());
		}
	}

	private CpConsentTier getConsentTierObj(Long id, ConsentStatement stmt) {
		CpConsentTier tier = new CpConsentTier();
		tier.setId(id);
		tier.setStatement(stmt);
		return tier;
	}

	private void ensureSpecimensNotCollected(SpecimenRequirement sr) {
		int count = daoFactory.getSpecimenRequirementDao().getSpecimensCount(sr.getId());
		if (count > 0) {
			throw OpenSpecimenException.userError(SrErrorCode.CANNOT_CHANGE_CLASS_OR_TYPE);
		}
	}
	
	private boolean isSpecimenClassOrTypeChanged(SpecimenRequirement existingSr, SpecimenRequirement sr) {
		return !existingSr.getSpecimenClass().equals(sr.getSpecimenClass()) || 
				!existingSr.getSpecimenType().equals(sr.getSpecimenType());
	}
	
	private void setAliquotCode(SpecimenRequirement parent, List<SpecimenRequirement> aliquots, String code) {
		Set<String> codes = new HashSet<String>();
		CollectionProtocolEvent cpe = parent.getCollectionProtocolEvent();
		for (SpecimenRequirement sr : cpe.getSpecimenRequirements()) {
			if (StringUtils.isNotBlank(sr.getCode())) {
				codes.add(sr.getCode());
			}
		}

		int count = 1;
		for (SpecimenRequirement sr : aliquots) {
			while (!codes.add(code + count)) {
				count++;
			}

			sr.setCode(code + count++);
		}
	}

	private CollectionProtocol getCollectionProtocol(String shortTitle) {
		return getCollectionProtocol(null, null, shortTitle);
	}

	private CollectionProtocol getCollectionProtocol(Long id, String title, String shortTitle) {
		Object key = null;
		CollectionProtocol cp = null;
		if (id != null) {
			key = id;
			cp = daoFactory.getCollectionProtocolDao().getById(id);
		} else if (StringUtils.isNotBlank(title)) {
			key = title;
			cp = daoFactory.getCollectionProtocolDao().getCollectionProtocol(title);
		} else if (StringUtils.isNoneBlank(shortTitle)) {
			key = shortTitle;
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(shortTitle);
		}

		if (key == null) {
			throw OpenSpecimenException.userError(CpErrorCode.SHORT_TITLE_REQUIRED);
		} else if (cp == null) {
			throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND, key);
		}

		return cp;
	}

	private String toJson(CollectionProtocol cp, boolean includeIds) {
		return toJson(CollectionProtocolDetail.from(cp, true), includeIds);
	}

	private String toJson(CollectionProtocolDetail cp, boolean includeIds) {
		cp.setSopDocumentName(null);
		cp.setSopDocumentUrl(null);

		SimpleFilterProvider filters = new SimpleFilterProvider();
		if (includeIds) {
			filters.addFilter("withoutId", SimpleBeanPropertyFilter.serializeAllExcept());
		} else {
			filters.addFilter("withoutId", SimpleBeanPropertyFilter.serializeAllExcept("id", "statementId"));
		}

		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writer(filters).withDefaultPrettyPrinter().writeValueAsString(cp);
		} catch (JsonProcessingException e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	private void mergeCprIntoCp(CollectionProtocolRegistration srcCpr, CollectionProtocol tgtCp) {
		//
		// Step 1: Get a matching CPR either by PPID or participant ID
		//
		CollectionProtocolRegistration tgtCpr = daoFactory.getCprDao().getCprByPpid(tgtCp.getId(), srcCpr.getPpid());
		if (tgtCpr == null) {
			tgtCpr = srcCpr.getParticipant().getCpr(tgtCp);
		}

		//
		// Step 2: Map all visits of source CP registrations to first event of target CP
		// Further mark all created specimens as unplanned
		//
		CollectionProtocolEvent firstCpe = tgtCp.firstEvent();
		for (Visit visit : srcCpr.getVisits()) {
			visit.setCpEvent(firstCpe);
			visit.getSpecimens().forEach(s -> s.setSpecimenRequirement(null));
		}

		//
		// Step 3: Attach registrations to target CP
		//
		if (tgtCpr == null) {
			//
			// case 1: No matching registration was found in target CP; therefore make source
			// registration as part of target CP
			//
			srcCpr.setCollectionProtocol(tgtCp);
		} else {
			//
			// case 2: Matching registration was found in target CP; therefore do following
			// 2.1 Move all visits of source CP registration to target CP registration
			// 2.2 Finally delete source CP registration
			//
			tgtCpr.addVisits(srcCpr.getVisits());
			srcCpr.getVisits().clear();
			srcCpr.delete();
		}
	}

	private void ensureMergeableCps(CollectionProtocol srcCp, CollectionProtocol tgtCp) {
		ArrayList<String> notSameLabels = new ArrayList<>();

		ensureBlankOrSame(srcCp.getPpidFormat(), tgtCp.getPpidFormat(), PPID_MSG, notSameLabels);
		ensureBlankOrSame(srcCp.getVisitNameFormat(), tgtCp.getVisitNameFormat(), VISIT_NAME_MSG, notSameLabels);
		ensureBlankOrSame(srcCp.getSpecimenLabelFormat(), tgtCp.getSpecimenLabelFormat(), SPECIMEN_LABEL_MSG, notSameLabels);
		ensureBlankOrSame(srcCp.getDerivativeLabelFormat(), tgtCp.getDerivativeLabelFormat(), DERIVATIVE_LABEL_MSG, notSameLabels);
		ensureBlankOrSame(srcCp.getAliquotLabelFormat(), tgtCp.getAliquotLabelFormat(), ALIQUOT_LABEL_MSG, notSameLabels);

		if (!notSameLabels.isEmpty()) {
			throw OpenSpecimenException.userError(
				CpErrorCode.CANNOT_MERGE_FMT_DIFFERS,
				srcCp.getShortTitle(),
				tgtCp.getShortTitle(),
				notSameLabels);
		}
	}

	private void ensureBlankOrSame(String srcLabelFmt, String tgtLabelFmt, String labelKey, List<String> notSameLabels) {
		if (!StringUtils.isBlank(tgtLabelFmt) && !tgtLabelFmt.equals(srcLabelFmt)) {
			notSameLabels.add(getMsg(labelKey));
		}
	}

	private boolean forceDeleteCps(final List<CollectionProtocol> cps, final String reason)
	throws Exception {
		final Authentication auth = AuthUtil.getAuth();

		Future<Boolean> result = taskExecutor.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				SecurityContextHolder.getContext().setAuthentication(auth);
				cps.forEach(cp -> forceDeleteCp(cp, reason));
				return true;
			}
		});

		boolean completed = false;
		try {
			completed = result.get(30, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			completed = false;
		}

		return completed;
	}

	private void forceDeleteCp(CollectionProtocol cp, String reason) {
		while (deleteRegistrations(cp));
		deleteCp(cp, reason);
	}

	private boolean deleteCps(List<CollectionProtocol> cps, String reason) {
		cps.forEach(cp -> deleteCp(cp, reason));
		return true;
	}

	@PlusTransactional
	private boolean deleteCp(CollectionProtocol cp, String reason) {
		boolean success = false;
		String stackTrace = null;
		CollectionProtocol deletedCp = new CollectionProtocol();
		try {
			//
			// refresh cp, as it could have been fetched in another transaction
			// if in same transaction, then it will be obtained from session
			//
			cp = daoFactory.getCollectionProtocolDao().getById(cp.getId());
			BeanUtils.copyProperties(cp, deletedCp);

			removeContainerRestrictions(cp);
			removeCpRoles(cp);

			cp.setOpComments(reason);
			cp.delete();

			DeleteLogUtil.getInstance().log(cp);
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(cp));
			success = true;
		} catch (Exception ex) {
			stackTrace = ExceptionUtils.getStackTrace(ex);

			if (ex instanceof OpenSpecimenException) {
				throw ex;
			} else {
				throw OpenSpecimenException.serverError(ex);
			}
		} finally {
			notifyUsersOnCpDelete(deletedCp, success, stackTrace);
		}

		return true;
	}
	
	@PlusTransactional
	private boolean deleteRegistrations(CollectionProtocol cp) {
		List<CollectionProtocolRegistration> cprs = daoFactory.getCprDao().getCprsByCpId(cp.getId(), 0, 10);
		cprs.forEach(cpr -> cpr.delete(false));
		return cprs.size() == 10;
	}

	private void removeContainerRestrictions(CollectionProtocol cp) {
		Set<StorageContainer> containers = cp.getStorageContainers();
		for (StorageContainer container : containers) {
			container.removeCpRestriction(cp);
		}
		
		cp.setStorageContainers(Collections.EMPTY_SET);
	}

	private void removeCpRoles(CollectionProtocol cp) {
		rbacSvc.removeCpRoles(cp.getId());
	}

	private void notifyUsersOnCpCreate(CollectionProtocol cp) {
		notifyUsersOnCpOp(cp, cp.getRepositories(), OP_CP_CREATED);
	}

	private void notifyUsersOnCpUpdate(CollectionProtocol cp, Collection<Site> addedSites, Collection<Site> removedSites) {
		notifyUsersOnCpOp(cp, removedSites, OP_CP_SITE_REMOVED);
		notifyUsersOnCpOp(cp, addedSites, OP_CP_SITE_ADDED);
	}

	private void notifyUsersOnCpDelete(CollectionProtocol cp, boolean success, String stackTrace) {
		if (success) {
			notifyUsersOnCpOp(cp, cp.getRepositories(), OP_CP_DELETED);
		} else {
			User currentUser = AuthUtil.getCurrentUser();
			String[] rcpts = {currentUser.getEmailAddress(), cp.getPrincipalInvestigator().getEmailAddress()};
			String[] subjParams = new String[] {cp.getShortTitle()};

			Map<String, Object> props = new HashMap<>();
			props.put("cp", cp);
			props.put("$subject", subjParams);
			props.put("user", currentUser);
			props.put("error", stackTrace);
			EmailUtil.getInstance().sendEmail(CP_DELETE_FAILED_EMAIL_TMPL, rcpts, null, props);
		}
	}

	private void notifyUsersOnCpOp(CollectionProtocol cp, Collection<Site> sites, int op) {
		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("$subject", new Object[] {cp.getShortTitle(), op});
		emailProps.put("cp", cp);
		emailProps.put("op", op);
		emailProps.put("currentUser", AuthUtil.getCurrentUser());
		emailProps.put("ccAdmin", false);

		if (op == OP_CP_CREATED || op == OP_CP_DELETED) {
			List<User> superAdmins = AccessCtrlMgr.getInstance().getSuperAdmins();
			notifyUsers(superAdmins, CP_OP_EMAIL_TMPL, emailProps, (op == OP_CP_CREATED) ? "CREATE" : "DELETE");
		}

		for (Site site : sites) {
			String siteName = site.getName();
			emailProps.put("siteName", siteName);
			emailProps.put("$subject", new Object[] {siteName, op, cp.getShortTitle()});
			notifyUsers(site.getCoordinators(), CP_SITE_UPDATED_EMAIL_TMPL, emailProps, "UPDATE");
		}
	}

	private void notifyUsers(Collection<User> users, String template, Map<String, Object> emailProps, String notifOp) {
		for (User rcpt : users) {
			emailProps.put("rcpt", rcpt);
			EmailUtil.getInstance().sendEmail(template, new String[] {rcpt.getEmailAddress()}, null, emailProps);
		}

		CollectionProtocol cp = (CollectionProtocol)emailProps.get("cp");
		Object[] subjParams = (Object[])emailProps.get("$subject");

		Notification notif = new Notification();
		notif.setEntityType(CollectionProtocol.getEntityName());
		notif.setEntityId(cp.getId());
		notif.setOperation(notifOp);
		notif.setCreatedBy(AuthUtil.getCurrentUser());
		notif.setCreationTime(Calendar.getInstance().getTime());
		notif.setMessage(MessageUtil.getInstance().getMessage(template + "_subj", subjParams));
		NotifUtil.getInstance().notify(notif, Collections.singletonMap("cp-overview", users));
	}

	private SubjectRoleOpNotif getNotifReq(CollectionProtocol cp, String role, List<User> notifUsers, User user, String cpOp, String roleOp) {
		SubjectRoleOpNotif notifReq = new SubjectRoleOpNotif();
		notifReq.setAdmins(notifUsers);
		notifReq.setAdminNotifMsg("cp_admin_notif_role_" + roleOp.toLowerCase());
		notifReq.setAdminNotifParams(new Object[] { user.getFirstName(), user.getLastName(), role, cp.getShortTitle(), user.getInstitute().getName() });
		notifReq.setUser(user);

		if (cpOp.equals("DELETE")) {
			notifReq.setSubjectNotifMsg("cp_delete_user_notif_role");
			notifReq.setSubjectNotifParams(new Object[] { cp.getShortTitle(), role });
		} else {
			notifReq.setSubjectNotifMsg("cp_user_notif_role_" + roleOp.toLowerCase());
			notifReq.setSubjectNotifParams(new Object[] { role, cp.getShortTitle(), user.getInstitute().getName() });
		}

		notifReq.setEndUserOp(cpOp);
		return notifReq;
	}

	private String getMsg(String code) {
		return MessageUtil.getInstance().getMessage(code);
	}

	private String getSopDocDir() {
		String defDir = ConfigUtil.getInstance().getDataDir() + File.separator + "cp-sop-documents";
		String dir = ConfigUtil.getInstance().getStrSetting(ConfigParams.MODULE, ConfigParams.CP_SOP_DOCS_DIR, defDir);
		new File(dir).mkdirs();
		return dir + File.separator;
	}

	private CpReportSettings getReportSetting(CpQueryCriteria crit) {
		CpReportSettings settings = null;
		if (crit.getId() != null) {
			settings = daoFactory.getCpReportSettingsDao().getByCp(crit.getId());
		} else if (StringUtils.isNotBlank(crit.getShortTitle())) {
			settings = daoFactory.getCpReportSettingsDao().getByCp(crit.getShortTitle());
		}

		return settings;
	}

	private ListConfig getSpecimenListConfig(Map<String, Object> listReq) {
		ListConfig cfg = getListConfig(listReq, "specimen-list-view", "Specimen");
		if (cfg == null) {
			return null;
		}

		ListUtil.addHiddenFieldsOfSpecimen(cfg);
		Long cpId = (Long)listReq.get("cpId");
		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps(cpId);
		if (siteCps == null) {
			//
			// Admin; hence no additional restrictions
			//
			return cfg;
		}

		if (siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		boolean useMrnSites = AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn();
		String restrictions = BiospecimenDaoHelper.getInstance().getSiteCpsCondAql(siteCps, useMrnSites);
		cfg.setRestriction(restrictions);
		cfg.setDistinct(true);
		return cfg;
	}

	private ListConfig getCpListConfig(Map<String, Object> listReq) {
		ListConfig cfg = getListConfig(listReq, "cp-list-view", "CollectionProtocol");
		if (cfg == null) {
			return null;
		}

		List<Column> hiddenColumns = new ArrayList<>();
		Column id = new Column();
		id.setExpr("CollectionProtocol.id");
		id.setCaption("cpId");
		cfg.setPrimaryColumn(id);
		hiddenColumns.add(id);

		Column catalogId = new Column();
		catalogId.setExpr("CollectionProtocol.catalogId");
		catalogId.setCaption("catalogId");
		hiddenColumns.add(catalogId);

		Column starred = new Column();
		starred.setExpr("CollectionProtocol.__userLabels.userId");
		starred.setCaption("starred");
		starred.setDirection(isOracle() ? "asc" : "desc");
		hiddenColumns.add(starred);
		cfg.setHiddenColumns(hiddenColumns);

		List<Column> fixedColumns = new ArrayList<>();
		Column participantsCount = new Column();
		participantsCount.setCaption("Participants");
		participantsCount.setExpr("count(Participant.id)");
		fixedColumns.add(participantsCount);

		Column specimensCount = new Column();
		specimensCount.setCaption("Specimens");
		specimensCount.setExpr("count(Specimen.id)");
		fixedColumns.add(specimensCount);
		cfg.setFixedColumns(fixedColumns);

		cfg.setFixedColumnsGenerator(
			(req, rows) -> {
				boolean includeStats = false;
				if (req != null && req.get("allParams") != null) {
					Map<String, String> allParams = (Map<String, String>) req.get("allParams");
					includeStats = "true".equals(allParams.get("includeStats"));
				}

				if (!includeStats) {
					return Collections.singletonMap("noFixedColumns", true);
				}

				Map<String, Object> resp = new HashMap<>();
				resp.put("noFixedColumns", false);
				resp.put("rows", rows);
				if (rows == null || rows.isEmpty()) {
					return resp;
				}

				Map<Long, Row> cpRows = new HashMap<>();
				for (Row row : rows) {
					Long cpId = Long.parseLong((String) row.getHidden().get("cpId"));
					cpRows.put(cpId, row);
				}

				List<Object[]> dbRows = sessionFactory.getCurrentSession()
					.getNamedQuery(CollectionProtocol.class.getName() + ".getParticipantAndSpecimenCount")
					.setParameterList("cpIds", cpRows.keySet())
					.list();

				for (Object[] dbRow : dbRows) {
					int idx = -1;
					Long cpId = ((Number) dbRow[++idx]).longValue();
					cpRows.get(cpId).setFixedData(new Object[] {dbRow[++idx], dbRow[++idx]});
				}

				return resp;
			}
		);

		List<Column> orderBy = cfg.getOrderBy();
		if (orderBy == null) {
			orderBy = new ArrayList<>();
			cfg.setOrderBy(orderBy);
		}
		orderBy.add(0, starred);

		cfg.setRestriction(String.format(USR_LABELS_RESTRICTION, AuthUtil.getCurrentUser().getId()));

		Set<SiteCpPair> cpSites = AccessCtrlMgr.getInstance().getReadableSiteCps();
		if (cpSites == null) {
			return cfg;
		}

		if (cpSites.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		String cpSitesRestriction = BiospecimenDaoHelper.getInstance().getSiteCpsCondAqlForCps(cpSites);
		if (StringUtils.isNotBlank(cpSitesRestriction)) {
			cfg.setRestriction(String.format(AND_COND, cfg.getRestriction(), cpSitesRestriction));
			cfg.setDistinct(true);
		}

		return cfg;
	}

	private ListConfig getParticipantsListConfig(Map<String, Object> listReq) {
		ListConfig cfg = getListConfig(listReq, "participant-list-view", "Participant");
		if (cfg == null) {
			return null;
		}

		Column id = new Column();
		id.setExpr("Participant.id");
		id.setCaption("cprId");
		cfg.setPrimaryColumn(id);
		cfg.setHiddenColumns(Collections.singletonList(id));

		Long cpId = (Long)listReq.get("cpId");
		ParticipantReadAccess access = AccessCtrlMgr.getInstance().getParticipantReadAccess(cpId);
		if (access.admin) {
			return cfg;
		}

		if (access.siteCps == null || access.siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		boolean useMrnSites = AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn();
		cfg.setRestriction(BiospecimenDaoHelper.getInstance().getSiteCpsCondAql(access.siteCps, useMrnSites));
		cfg.setDistinct(true);
		return cfg;
	}

	private ListConfig getListConfig(Map<String, Object> listReq, String listName, String drivingForm) {
		Long cpId = (Long) listReq.get("cpId");
		if (cpId == null) {
			cpId = (Long) listReq.get("objectId");
		}

		Workflow workflow = getWorkFlow(cpId, listName);
		if (workflow == null) {
			return null;
		}

		ListConfig listCfg = new ObjectMapper().convertValue(workflow.getData(), ListConfig.class);
		listCfg.setCpId(cpId);
		listCfg.setDrivingForm(drivingForm);
		setListLimit(listReq, listCfg);

		Boolean includeCount = (Boolean)listReq.get("includeCount");
		listCfg.setIncludeCount(includeCount != null && includeCount);
		return listCfg;
	}

	private Workflow getWorkFlow(Long cpId, String name) {
		Workflow workflow = null;

		CpWorkflowConfig cfg = null;
		if (cpId != null) {
			cfg = daoFactory.getCollectionProtocolDao().getCpWorkflows(cpId);
		}

		if (cfg != null) {
			workflow = cfg.getWorkflows().get(name);
		}

		if (workflow == null) {
			workflow = getSysWorkflow(name);
		}

		return workflow;
	}

	private Workflow getSysWorkflow(String name) {
		return WorkflowUtil.getInstance().getSysWorkflow(name);
	}

	private void setListLimit(Map<String, Object> listReq, ListConfig cfg) {
		Integer startAt = (Integer)listReq.get("startAt");
		if (startAt == null) {
			startAt = 0;
		}

		Integer maxResults = (Integer)listReq.get("maxResults");
		if (maxResults == null) {
			maxResults = 100;
		}

		cfg.setStartAt(startAt);
		cfg.setMaxResults(maxResults);
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

	private boolean isOracle() {
		String dbType = AppProperties.getInstance().getProperties()
			.getProperty("database.type", "mysql")
			.toLowerCase();
		return dbType.equalsIgnoreCase("oracle");
	}

	private void createExtensions(List<CollectionProtocolRegistration> cprs) {
		Map<CollectionProtocol, List<CollectionProtocolRegistration>> cpRegs = new HashMap<>();
		cprs.forEach(cpr -> cpRegs.computeIfAbsent(cpr.getCollectionProtocol(), (k) -> new ArrayList<>()).add(cpr));
		cpRegs.forEach((cp, regs) -> DeObject.createExtensions(true, Participant.EXTN, cp.getId(), regs));
	}

	private CollectionProtocolPublishEvent createPublishEvent(CollectionProtocol cp, CollectionProtocolPublishDetail input, OpenSpecimenException ose) {
		CollectionProtocolPublishEvent publishEvent = new CollectionProtocolPublishEvent();
		publishEvent.setCp(cp);
		publishEvent.setPublishedBy(AuthUtil.getCurrentUser());
		publishEvent.setPublicationDate(Calendar.getInstance().getTime());

		publishEvent.setChanges(input.getChanges());
		if (StringUtils.isBlank(publishEvent.getChanges())) {
			ose.addError(CpErrorCode.PUB_CHANGES_REQ, cp.getShortTitle());
		}

		publishEvent.setReason(input.getReason());
		if (StringUtils.isBlank(input.getReason())) {
			ose.addError(CpErrorCode.PUB_REASON_REQ, cp.getShortTitle());
		}

		publishEvent.setReviewers(new HashSet<>(getPubReviewers(cp, input.getReviewers(), ose)));
		return publishEvent;
	}

	private List<User> getPubReviewers(CollectionProtocol cp, List<UserSummary> input, OpenSpecimenException ose) {
		if (CollectionUtils.isEmpty(input)) {
			ose.addError(CpErrorCode.PUB_REVIEWERS_REQ, cp.getShortTitle());
			return Collections.emptyList();
		}

		List<Long> userIds        = new ArrayList<>();
		List<String> userEmailIds = new ArrayList<>();
		for (UserSummary ir : input) {
			if (ir.getId() != null) {
				userIds.add(ir.getId());
			} else if (StringUtils.isNotBlank(ir.getEmailAddress())) {
				userEmailIds.add(ir.getEmailAddress());
			}
		}

		if (userIds.isEmpty() && userEmailIds.isEmpty()) {
			ose.addError(CpErrorCode.PUB_REVIEWERS_REQ, cp.getShortTitle());
		}

		List<User> reviewers = new ArrayList<>();
		if (!userIds.isEmpty()) {
			reviewers.addAll(daoFactory.getUserDao().getByIds(userIds));
			if (reviewers.size() != userIds.size()) {
				List<Long> notFoundIds = userIds.stream()
					.filter(userId -> reviewers.stream().noneMatch(r -> r.getId().equals(userId)))
					.collect(Collectors.toList());
				if (!notFoundIds.isEmpty()) {
					ose.addError(CpErrorCode.PUB_INVALID_REVIEWERS, notFoundIds);
				}
			}
		}

		if (!userEmailIds.isEmpty()) {
			int existing = reviewers.size();
			reviewers.addAll(daoFactory.getUserDao().getUsersByEmailAddress(userEmailIds));
			if ((reviewers.size() - existing) != userEmailIds.size()) {
				List<String> notFoundIds = userEmailIds.stream()
					.filter(emailId -> reviewers.stream().noneMatch(r -> emailId.equalsIgnoreCase(r.getEmailAddress())))
					.collect(Collectors.toList());
				if (!notFoundIds.isEmpty()) {
					ose.addError(CpErrorCode.PUB_INVALID_REVIEWERS, notFoundIds);
				}
			}
		}

		return reviewers;
	}

	private Service getService(Long serviceId, Long cpId, String cpShortTitle, String serviceCode) {
		Service service = null;
		Object key = null;

		if (serviceId != null) {
			key = serviceId;
			service = daoFactory.getServiceDao().getById(serviceId);
		} else if (StringUtils.isNotBlank(serviceCode)) {
			if (cpId != null) {
				key = cpId + " / " + serviceCode;
				service = daoFactory.getServiceDao().getService(cpId, serviceCode);
			} else if (StringUtils.isNotBlank(cpShortTitle)) {
				key = cpShortTitle + " / " + serviceCode;
				service = daoFactory.getServiceDao().getService(cpShortTitle, serviceCode);
			}

			if (key == null) {
				throw OpenSpecimenException.userError(CpErrorCode.SHORT_TITLE_REQUIRED);
			}
		}

		if (key == null) {
			throw OpenSpecimenException.userError(ServiceErrorCode.CODE_REQ);
		} else if (service == null) {
			throw OpenSpecimenException.userError(ServiceErrorCode.NOT_FOUND, key);
		}

		return service;
	}

	private void ensureUniqueServiceCode(Service existing, Service service) {
		if (existing != null && existing.getCode().equalsIgnoreCase(service.getCode())) {
			return;
		}

		Service dbService = daoFactory.getServiceDao().getService(service.getCp().getShortTitle(), service.getCode());
		if (dbService != null) {
			throw OpenSpecimenException.userError(ServiceErrorCode.DUP_CODE, service.getCp().getShortTitle(), service.getCode());
		}
	}

	private void ensureServiceIsNotInUse(Service service) {
		long count = daoFactory.getServiceDao().getServiceUsageCount(service.getId());
		if (count > 0) {
			throw OpenSpecimenException.userError(ServiceErrorCode.IN_USE, service.getCode(), count);
		}
	}

	private void ensureNoRateIntervalOverlap(ServiceRate rate) {
		List<ServiceRate> rates = daoFactory.getServiceRateDao().getOverlappingRates(rate);
		if (!rates.isEmpty()) {
			String existingInterval = rates.iterator().next().intervalString();
			throw OpenSpecimenException.userError(ServiceRateErrorCode.INT_OVERLAP, rate.intervalString(), existingInterval);
		}
	}


	private Function<ExportJob, List<? extends Object>> getCpsGenerator() {
		return new Function<>() {
			private boolean paramsInited;

			private boolean endOfCps;

			private Long lastId;

			private CpListCriteria crit;

			@Override
			public List<? extends Object> apply(ExportJob job) {
				initParams(job);
				if (endOfCps) {
					return Collections.emptyList();
				}

				crit.lastId(lastId);
				List<CollectionProtocol> cps = daoFactory.getCollectionProtocolDao().getCollectionProtocolsList(crit);
				if (!cps.isEmpty()) {
					lastId = cps.get(cps.size() - 1).getId();
				}

				endOfCps = cps.size() < crit.maxResults();
				return CollectionProtocolDetail.from(cps);
			}

			private void initParams(ExportJob job) {
				if (paramsInited) {
					return;
				}

				Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadableSiteCps();
				if ((siteCps != null && siteCps.isEmpty()) || !AccessCtrlMgr.getInstance().hasEximRights(null, Resource.CP.getName())) {
					endOfCps = true;
					return;
				}

				crit = new CpListCriteria()
					.siteCps(siteCps)
					.ids(job.getRecordIds())
					.orderBy("cp.id");
				paramsInited = true;
			}
		};
	}

	private Function<ExportJob, List<? extends Object>> getEventsGenerator() {
		return new Function<>() {
			private boolean paramsInited;

			private boolean endOfEvents;

			private Long lastCpId;

			private CpListCriteria crit;

			@Override
			public List<CollectionProtocolEventDetail> apply(ExportJob job) {
				initParams(job);
				if (endOfEvents) {
					return Collections.emptyList();
				}

				List<CollectionProtocolEventDetail> cpes = null;
				while (true) {
					CollectionProtocol cp = nextCp();
					if (cp == null) {
						endOfEvents = true;
						cpes = Collections.emptyList();
						break;
					}

					cpes = CollectionProtocolEventDetail.from(cp.getOrderedCpeList(), false);
					if (!cpes.isEmpty()) {
						break;
					}
				}

				return cpes;
			}

			private void initParams(ExportJob job) {
				if (paramsInited) {
					return;
				}

				Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadableSiteCps();
				if ((siteCps != null && siteCps.isEmpty()) || !AccessCtrlMgr.getInstance().hasEximRights(null, Resource.CP.getName())) {
					endOfEvents = true;
					return;
				}

				List<Long> cpIds = job.getRecordIds();
				if (cpIds == null || cpIds.isEmpty()) {
					String cpIdStr = job.param("cpId");
					if (StringUtils.isNotBlank(cpIdStr) && !cpIdStr.trim().equals("-1")) {
						cpIds = Collections.singletonList(Long.parseLong(cpIdStr));
					}
				}

				crit = new CpListCriteria()
					.siteCps(siteCps)
					.ids(cpIds)
					.maxResults(1)
					.orderBy("cp.id");
				paramsInited = true;
			}

			private CollectionProtocol nextCp() {
				List<CollectionProtocol> cps = daoFactory.getCollectionProtocolDao().getCollectionProtocolsList(crit.lastId(lastCpId));
				if (cps == null || cps.isEmpty()) {
					return null;
				} else {
					CollectionProtocol cp = cps.iterator().next();
					lastCpId = cp.getId();
					return cp;
				}
			}
		};
	}

	private Function<ExportJob, List<? extends Object>> getSpecimenRequirementsGenerator() {
		return new Function<>() {
			private boolean paramsInited;

			private boolean endOfSrs;

			private Long lastCpId;

			private CpListCriteria crit;

			private Map<String, SpecimenTypeUnit> units = new HashMap<>();

			@Override
			public List<SpecimenRequirementDetail> apply(ExportJob job) {
				initParams(job);
				if (endOfSrs) {
					return Collections.emptyList();
				}

				List<SpecimenRequirementDetail> srs = null;
				while (true) {
					CollectionProtocol cp = nextCp();
					if (cp == null) {
						endOfSrs = true;
						srs = Collections.emptyList();
						break;
					}

					srs = getSrs(cp);
					if (!srs.isEmpty()) {
						break;
					}
				}

				return srs;
			}

			private void initParams(ExportJob job) {
				if (paramsInited) {
					return;
				}

				Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadableSiteCps();
				if ((siteCps != null && siteCps.isEmpty()) || !AccessCtrlMgr.getInstance().hasEximRights(null, Resource.CP.getName())) {
					endOfSrs = true;
					return;
				}

				List<Long> cpIds = job.getRecordIds();
				if (cpIds == null || cpIds.isEmpty()) {
					String cpIdStr = job.param("cpId");
					if (StringUtils.isNotBlank(cpIdStr) && !cpIdStr.trim().equals("-1")) {
						cpIds = Collections.singletonList(Long.parseLong(cpIdStr));
					}
				}

				crit = new CpListCriteria()
					.siteCps(siteCps)
					.ids(cpIds)
					.maxResults(1)
					.orderBy("cp.id");
				paramsInited = true;
			}

			private CollectionProtocol nextCp() {
				List<CollectionProtocol> cps = daoFactory.getCollectionProtocolDao().getCollectionProtocolsList(crit.lastId(lastCpId));
				if (cps == null || cps.isEmpty()) {
					return null;
				} else {
					CollectionProtocol cp = cps.iterator().next();
					lastCpId = cp.getId();
					return cp;
				}
			}

			private List<SpecimenRequirementDetail> getSrs(CollectionProtocol cp) {
				List<SpecimenRequirementDetail> result = new ArrayList<>();
				for (CollectionProtocolEvent cpe : cp.getOrderedCpeList()) {
					List<SpecimenRequirement> parents = new ArrayList<>(cpe.getOrderedTopLevelAnticipatedSpecimens());
					while (!parents.isEmpty()) {
						SpecimenRequirement sr = parents.remove(0);
						parents.addAll(0, sr.getOrderedChildRequirements());

						SpecimenRequirementDetail detail = SpecimenRequirementDetail.from(sr, false);
						detail.setCpShortTitle(cp.getShortTitle());
						detail.setEventLabel(cpe.getEventLabel());
						if (sr.getParentSpecimenRequirement() != null) {
							detail.setParentSrCode(sr.getParentSpecimenRequirement().getCode());
						}


						SpecimenTypeUnit unit = getUnit(sr);
						if (unit != null) {
							detail.setQuantityUnit(PermissibleValue.getValue(unit.getQuantityUnit()));
							detail.setConcentrationUnit(PermissibleValue.getValue(unit.getConcentrationUnit()));
						}

						result.add(detail);
					}
				}

				return result;
			}

			private SpecimenTypeUnit getUnit(SpecimenRequirement sr) {
				CollectionProtocol cp = sr.getCollectionProtocol();
				PermissibleValue specimenClass = sr.getSpecimenClass();
				PermissibleValue type = sr.getSpecimenType();

				String key = cp.getId() + ":" + specimenClass.getId() + ":" + type.getId();
				return units.computeIfAbsent(key, (k) -> SpecimenUtil.getInstance().getUnit(cp, specimenClass, type));
			}
		};
	}

	private static final String PPID_MSG                     = "cp_ppid";

	private static final String VISIT_NAME_MSG               = "cp_visit_name";

	private static final String SPECIMEN_LABEL_MSG           = "cp_specimen_label";

	private static final String DERIVATIVE_LABEL_MSG         = "cp_derivative_label";

	private static final String ALIQUOT_LABEL_MSG            = "cp_aliquot_label";
	
	private static final String CP_OP_EMAIL_TMPL             = "cp_op";
	
	private static final String CP_DELETE_FAILED_EMAIL_TMPL  = "cp_delete_failed";

	private static final String CP_SITE_UPDATED_EMAIL_TMPL   = "cp_site_updated";

	private static final int OP_CP_SITE_ADDED = -1;

	private static final int OP_CP_SITE_REMOVED = 1;

	private static final int OP_CP_CREATED = 0;

	private static final int OP_CP_DELETED = 2;

	private static final String USR_LABELS_RESTRICTION =
		"(CollectionProtocol.__userLabels.userId not exists or CollectionProtocol.__userLabels.userId = %d)";

	private static final String AND_COND = "(%s) and (%s)";

	private static final String CP_PUBLISHED_EMAIL_TMPL = "cp_published";
}
