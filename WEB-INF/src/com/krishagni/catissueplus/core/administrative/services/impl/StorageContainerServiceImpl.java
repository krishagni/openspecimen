package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreList;
import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreList.Op;
import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreListItem;
import com.krishagni.catissueplus.core.administrative.domain.ContainerType;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerFactory;
import com.krishagni.catissueplus.core.administrative.events.AutoFreezerReportDetail;
import com.krishagni.catissueplus.core.administrative.events.BoxDetail;
import com.krishagni.catissueplus.core.administrative.events.ContainerCriteria;
import com.krishagni.catissueplus.core.administrative.events.ContainerDefragDetail;
import com.krishagni.catissueplus.core.administrative.events.ContainerHierarchyDetail;
import com.krishagni.catissueplus.core.administrative.events.ContainerQueryCriteria;
import com.krishagni.catissueplus.core.administrative.events.ContainerReplicationDetail;
import com.krishagni.catissueplus.core.administrative.events.ContainerReplicationDetail.DestinationDetail;
import com.krishagni.catissueplus.core.administrative.events.ContainerTransferEventDetail;
import com.krishagni.catissueplus.core.administrative.events.FindPlacesCriteria;
import com.krishagni.catissueplus.core.administrative.events.PositionsDetail;
import com.krishagni.catissueplus.core.administrative.events.PrintContainerLabelDetail;
import com.krishagni.catissueplus.core.administrative.events.ReservePositionsOp;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerPositionDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerSummary;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.administrative.events.TenantDetail;
import com.krishagni.catissueplus.core.administrative.events.VacantPositionsOp;
import com.krishagni.catissueplus.core.administrative.repository.ContainerReportCriteria;
import com.krishagni.catissueplus.core.administrative.repository.ContainerStoreListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerListCriteria;
import com.krishagni.catissueplus.core.administrative.services.ContainerReport;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionRule;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionStrategy;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionStrategyFactory;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTaskManager;
import com.krishagni.catissueplus.core.administrative.services.StorageContainerService;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.BiospecimenDaoHelper;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenResolver;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.RollbackTransaction;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.domain.PrintItem;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.events.LabelPrintJobSummary;
import com.krishagni.catissueplus.core.common.events.LabelTokenDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.service.LabelPrinter;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.StarredItemService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.domain.Filter;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.events.ExecuteQueryEventOp;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.catissueplus.core.de.services.QueryService;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.catissueplus.core.query.Column;
import com.krishagni.catissueplus.core.query.ListConfig;
import com.krishagni.catissueplus.core.query.ListService;
import com.krishagni.catissueplus.core.query.ListUtil;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import edu.common.dynamicextensions.query.WideRowMode;

public class StorageContainerServiceImpl implements StorageContainerService, ObjectAccessor, InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(StorageContainerServiceImpl.class);

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

	private DaoFactory daoFactory;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;
	
	private StorageContainerFactory containerFactory;
	
	private ContainerReport mapExporter;

	private ContainerReport emptyPositionsReport;

	private ContainerReport utilisationReport;

	private ContainerReport defragReport;

	private LabelGenerator nameGenerator;

	private SpecimenResolver specimenResolver;

	private ContainerSelectionStrategyFactory selectionStrategyFactory;

	private ScheduledTaskManager taskManager;

	private QueryService querySvc;

	private ExportService exportSvc;

	private LabelPrinter<StorageContainer> labelPrinter;

	private ThreadPoolTaskExecutor taskExecutor;

	private StarredItemService starredItemSvc;

	private ListService listSvc;

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	public StorageContainerFactory getContainerFactory() {
		return containerFactory;
	}

	public void setContainerFactory(StorageContainerFactory containerFactory) {
		this.containerFactory = containerFactory;
	}
	
	public void setMapExporter(ContainerReport mapExporter) {
		this.mapExporter = mapExporter;
	}

	public void setEmptyPositionsReport(ContainerReport emptyPositionsReport) {
		this.emptyPositionsReport = emptyPositionsReport;
	}

	public void setUtilisationReport(ContainerReport utilisationReport) {
		this.utilisationReport = utilisationReport;
	}

	public void setDefragReport(ContainerReport defragReport) {
		this.defragReport = defragReport;
	}

	public void setNameGenerator(LabelGenerator nameGenerator) {
		this.nameGenerator = nameGenerator;
	}

	public void setSpecimenResolver(SpecimenResolver specimenResolver) {
		this.specimenResolver = specimenResolver;
	}

	public void setSelectionStrategyFactory(ContainerSelectionStrategyFactory selectionStrategyFactory) {
		this.selectionStrategyFactory = selectionStrategyFactory;
	}

	public void setTaskManager(ScheduledTaskManager taskManager) {
		this.taskManager = taskManager;
	}

	public void setQuerySvc(QueryService querySvc) {
		this.querySvc = querySvc;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	public void setLabelPrinter(LabelPrinter<StorageContainer> labelPrinter) {
		this.labelPrinter = labelPrinter;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setStarredItemSvc(StarredItemService starredItemSvc) {
		this.starredItemSvc = starredItemSvc;
	}

	public void setListSvc(ListService listSvc) {
		this.listSvc = listSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerSummary>> getStorageContainers(RequestEvent<StorageContainerListCriteria> req) {
		try {			
			StorageContainerListCriteria crit = addContainerListCriteria(req.getPayload());

			List<StorageContainerSummary> result = new ArrayList<>();
			if (crit.orderByStarred()) {
				List<Long> containerIds = daoFactory.getStarredItemDao()
					.getItemIds(getObjectName(), AuthUtil.getCurrentUser().getId());
				if (!containerIds.isEmpty()) {
					crit.ids(containerIds);
					List<StorageContainer> containers = daoFactory.getStorageContainerDao().getStorageContainers(crit);
					result.addAll(StorageContainerSummary.from(containers, crit.includeChildren()));
					result.forEach(c -> c.setStarred(true));
					crit.ids(Collections.emptyList()).notInIds(containerIds);
				}
			}

			if (result.size() < crit.maxResults()) {
				crit.maxResults(crit.maxResults() - result.size());
				List<StorageContainer> containers = daoFactory.getStorageContainerDao().getStorageContainers(crit);
				result.addAll(StorageContainerSummary.from(containers, crit.includeChildren()));
			}

			setStoredSpecimensCount(crit, result);
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getStorageContainersCount(RequestEvent<StorageContainerListCriteria> req) {
		try {
			StorageContainerListCriteria crit = addContainerListCriteria(req.getPayload());
			return ResponseEvent.response(daoFactory.getStorageContainerDao().getStorageContainersCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<StorageContainerDetail> getStorageContainer(RequestEvent<ContainerQueryCriteria> req) {
		try {		
			StorageContainer container = getContainer(req.getPayload());
			if (container == null) {
				return ResponseEvent.response(null);
			}

			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
			StorageContainerDetail detail = StorageContainerDetail.from(container);
			if (req.getPayload().includeStats()) {
				Long rootId = container.getId();
				if (container.getPosition() != null) {
					rootId = daoFactory.getStorageContainerDao().getRootContainerId(container.getId());
				}

				detail.setSpecimensByType(daoFactory.getStorageContainerDao().getSpecimensCountByType(detail.getId()));
				detail.setRootContainerId(rootId);
			}

			return ResponseEvent.response(detail);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerPositionDetail>> getOccupiedPositions(RequestEvent<Long> req) {
		try {
			StorageContainer container = getContainer(req.getPayload(), null);
			if (container.isArchived()) {
				return ResponseEvent.userError(StorageContainerErrorCode.ARCHIVED, container.getName());
			}

			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
			return ResponseEvent.response(StorageContainerPositionDetail.from(container.getOccupiedPositions()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenInfo>> getSpecimens(RequestEvent<SpecimenListCriteria> req) {
		StorageContainer container = getContainer(req.getPayload().ancestorContainerId(), null);
		if (container.isArchived()) {
			return ResponseEvent.userError(StorageContainerErrorCode.ARCHIVED, container.getName());
		}

		AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
		SpecimenListCriteria crit = addSiteCpRestrictions(req.getPayload(), container);

		List<Specimen> specimens = daoFactory.getStorageContainerDao().getSpecimens(crit, !container.isDimensionless());
		return ResponseEvent.response(SpecimenInfo.from(specimens));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getSpecimensCount(RequestEvent<SpecimenListCriteria> req) {
		StorageContainer container = getContainer(req.getPayload().ancestorContainerId(), null);
		if (container.isArchived()) {
			return ResponseEvent.userError(StorageContainerErrorCode.ARCHIVED, container.getName());
		}

		AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
		SpecimenListCriteria crit = addSiteCpRestrictions(req.getPayload(), container);

		Long count = daoFactory.getStorageContainerDao().getSpecimensCount(crit);
		return ResponseEvent.response(count);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<QueryDataExportResult> getSpecimensReport(RequestEvent<ContainerQueryCriteria> req) {
		ContainerQueryCriteria crit = req.getPayload();
		StorageContainer container = getContainer(crit.getId(), crit.getName());
		if (container.isArchived()) {
			return ResponseEvent.userError(StorageContainerErrorCode.ARCHIVED, container.getName());
		}

		AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
		Integer queryId = ConfigUtil.getInstance().getIntSetting("common", "cont_spmns_report_query", -1);
		if (queryId == -1) {
			return ResponseEvent.userError(StorageContainerErrorCode.SPMNS_RPT_NOT_CONFIGURED);
		}

		SavedQuery query = deDaoFactory.getSavedQueryDao().getQuery(queryId.longValue());
		if (query == null) {
			return ResponseEvent.userError(SavedQueryErrorCode.NOT_FOUND, queryId);
		}

		return new ResponseEvent<>(exportResult(container, query));
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<StorageContainerDetail> createStorageContainer(RequestEvent<StorageContainerDetail> req) {
		try {
			StorageContainer container = createStorageContainer(null, req.getPayload());
			if (req.getPayload().isPrintLabels()) {
				printLabels(container);
			}

			return ResponseEvent.response(StorageContainerDetail.from(container));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<StorageContainerDetail> updateStorageContainer(RequestEvent<StorageContainerDetail> req) {
		return updateStorageContainer(req, false);
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<StorageContainerDetail> patchStorageContainer(RequestEvent<StorageContainerDetail> req) {
		return updateStorageContainer(req, true);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerSummary>> patchStorageContainers(RequestEvent<List<StorageContainerDetail>> req) {
		User user        = AuthUtil.getCurrentUser();
		String ipAddress = AuthUtil.getRemoteAddr();
		Future<ResponseEvent<List<StorageContainerSummary>>> promise = taskExecutor.submit(
			new Callable<ResponseEvent<List<StorageContainerSummary>>>() {
				@Override
				public ResponseEvent<List<StorageContainerSummary>> call() throws Exception {
					AuthUtil.setCurrentUser(user, ipAddress);
					try {
						return patch();
					} finally {
						// clear only after the txn is committed.
						// other the revision will not have the user.
						AuthUtil.clearCurrentUser();
					}
				}

				@PlusTransactional
				private ResponseEvent<List<StorageContainerSummary>> patch() {
					int count = 0;
					Throwable t = null;
					try {
						List<StorageContainerSummary> result = new ArrayList<>();
						for (StorageContainerDetail input : req.getPayload()) {
							result.add(StorageContainerSummary.from(updateStorageContainer0(input, true)));
						}

						count = result.size();
						return ResponseEvent.response(result);
					} catch (Throwable e) {
						t = e;
						if (e instanceof OpenSpecimenException) {
							OpenSpecimenException ose = (OpenSpecimenException) e;
							if (ose.getErrorType() == ErrorType.USER_ERROR) {
								return ResponseEvent.error(ose);
							}
						}

						return ResponseEvent.serverError(new Exception(t.getCause()));
					} finally {
						sendBulkUpdateNotif(user, count, t);
					}
				}
			}
		);

		try {
			return promise.get(30 * 1000L, TimeUnit.MILLISECONDS);
		} catch (TimeoutException te) {
			return ResponseEvent.response(null);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> isAllowed(RequestEvent<TenantDetail> req) {
		try {
			TenantDetail detail = req.getPayload();

			StorageContainer container = getContainer(detail.getContainerId(), detail.getContainerName());
			if (container.isArchived()) {
				return ResponseEvent.userError(StorageContainerErrorCode.ARCHIVED, container.getName());
			}

			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
			
			CollectionProtocol cp = new CollectionProtocol();
			cp.setId(detail.getCpId());
			String specimenClass = detail.getSpecimenClass();
			String type = detail.getSpecimenType();
			boolean isAllowed = container.canContainSpecimen(cp, specimenClass, type);

			if (!isAllowed) {
				return ResponseEvent.userError(
						StorageContainerErrorCode.CANNOT_HOLD_SPECIMEN, 
						container.getName(), 
						Specimen.getDesc(specimenClass, type));
			} else {
				return ResponseEvent.response(isAllowed);
			}
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	public ResponseEvent<ExportedFileDetail> exportMap(RequestEvent<ContainerQueryCriteria> req) {
		return exportReport(req, mapExporter);
	}

	@Override
	public ResponseEvent<ExportedFileDetail> exportEmptyPositions(RequestEvent<ContainerQueryCriteria> req) {
		return exportReport(req, emptyPositionsReport);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ExportedFileDetail> exportUtilisation(RequestEvent<ContainerReportCriteria> req) {
		try {
			Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessContainerSiteCps();
			if (siteCps != null && siteCps.isEmpty()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			ContainerReportCriteria rptCrit = req.getPayload().siteCps(siteCps);
			List<StorageContainer> containers = daoFactory.getStorageContainerDao().getStorageContainers(rptCrit.toListCriteria());
			return exportReport(utilisationReport, containers, rptCrit);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerPositionDetail>> assignPositions(RequestEvent<PositionsDetail> req) {
		try {
			PositionsDetail op = req.getPayload();
			StorageContainer container = getContainer(op.getContainerId(), op.getContainerName());
			if (container.isArchived()) {
				return ResponseEvent.userError(StorageContainerErrorCode.ARCHIVED, container.getName());
			}

			
			List<StorageContainerPosition> positions = op.getPositions().stream()
				.map(posDetail -> createPosition(container, posDetail, op.getVacateOccupant()))
				.collect(Collectors.toList());

			container.assignPositions(positions, op.getVacateOccupant());
			daoFactory.getStorageContainerDao().saveOrUpdate(container, true);
			if (container.isDimensionless()) {
				return ResponseEvent.response(Collections.emptyList());
			}

			return ResponseEvent.response(StorageContainerPositionDetail.from(container.getOccupiedPositions()));
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
			StorageContainer existing = getContainer(req.getPayload(), null);
			return ResponseEvent.response(existing.getDependentEntities());
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	public ResponseEvent<Map<String, Integer>> deleteStorageContainers(RequestEvent<BulkDeleteEntityOp> req) {
		try {
			BulkDeleteEntityOp op = req.getPayload();
			Set<Long> containerIds = op.getIds();
			List<StorageContainer> ancestors = getAccessibleAncestors(containerIds);
			int clearedSpmnCount = ancestors.stream()
				.mapToInt(c -> deleteContainerHierarchy(c, op.isForceDelete()))
				.sum();

			//
			// returning summary of all containers given by user instead of only ancestor containers
			//
			Map<String, Integer> result = new HashMap<>();
			result.put(StorageContainer.getEntityName(), op.getIds().size());
			result.put(Specimen.getEntityName(), clearedSpmnCount);
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> replicateStorageContainer(RequestEvent<ContainerReplicationDetail> req) {
		try {
			ContainerReplicationDetail replDetail = req.getPayload();
			StorageContainer srcContainer = getContainer(
					replDetail.getSourceContainerId(), 
					replDetail.getSourceContainerName(),
					null,
					StorageContainerErrorCode.SRC_ID_OR_NAME_REQ,
					true);
			if (srcContainer.isArchived()) {
				return ResponseEvent.userError(StorageContainerErrorCode.ARCHIVED, srcContainer.getName());
			}

			List<StorageContainer> toPrint = new ArrayList<>();
			for (DestinationDetail dest : replDetail.getDestinations()) {
				StorageContainer container = replicateContainer(srcContainer, dest);
				if (replDetail.isPrintLabels()) {
					toPrint.add(container);
				}
			}

			printLabels(toPrint);
			return ResponseEvent.response(true);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerSummary>> createContainerHierarchy(RequestEvent<ContainerHierarchyDetail> req) {
		ContainerHierarchyDetail input = req.getPayload();
		List<StorageContainer> containers = new ArrayList<>();

		try {
			StorageContainer container = containerFactory.createStorageContainer(StorageContainer.DUMMY_NAME, input);
			AccessCtrlMgr.getInstance().ensureCreateContainerRights(container);
			container.validateRestrictions();

			StorageContainer parentContainer = container.getParentContainer();
			if (parentContainer != null && parentContainer.isArchived()) {
				return ResponseEvent.userError(StorageContainerErrorCode.ARCHIVED, parentContainer.getName());
			}

			if (parentContainer != null && !parentContainer.hasFreePositionsForReservation(input.getNumOfContainers())) {
				return ResponseEvent.userError(StorageContainerErrorCode.NO_FREE_SPACE, parentContainer.getName());
			}

			if (container.getType() == null || StringUtils.isBlank(container.getType().getNameFormat())) {
				if (CollectionUtils.isEmpty(input.getNames())) {
					return ResponseEvent.userError(StorageContainerErrorCode.NAME_REQUIRED);
				}

				input.setNumOfContainers(input.getNames().size());
			} else {
				input.setNames(null);
			}

			boolean setCapacity = true;
			for (int i = 1; i <= input.getNumOfContainers(); i++) {
				StorageContainer cloned;
				if (i == 1) {
					cloned = container;
				} else {
					cloned = container.copy();
					setPosition(cloned);
				}

				if (CollectionUtils.isEmpty(input.getNames())) {
					generateName(cloned);
				} else {
					cloned.setName(input.getNames().get(i - 1));
				}

				ensureUniqueConstraints(null, cloned);

				if (cloned.isStoreSpecimenEnabled() && setCapacity) {
					cloned.setFreezerCapacity();
					setCapacity = false;
				}

				daoFactory.getStorageContainerDao().saveOrUpdate(cloned);
				cloned.addOrUpdateExtension();
				containers.add(cloned);

				List<StorageContainer> result = new ArrayList<>();
				if (!cloned.getType().isDimensionless() && StringUtils.isNotBlank(cloned.getType().getNameFormat())) {
					result = createContainerHierarchy(cloned.getType().getCanHold(), cloned);
				}

				result.add(0, cloned);
				if (input.isPrintLabels()) {
					printLabels(result);
				}

				result.clear();
			}
			
			return ResponseEvent.response(StorageContainerDetail.from(containers));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerSummary>> createMultipleContainers(RequestEvent<List<StorageContainerDetail>> req) {
		try {
			List<StorageContainerSummary> result = new ArrayList<>();
			List<StorageContainer> toPrint = new ArrayList<>();

			for (StorageContainerDetail detail : req.getPayload()) {
				if (StringUtils.isBlank(detail.getName()) && (StringUtils.isNotBlank(detail.getTypeName()) || detail.getTypeId() != null)) {
					detail.setName(StorageContainer.DUMMY_NAME);
				}

				StorageContainer container = containerFactory.createStorageContainer(detail);
				if (container.getParentContainer() != null && container.getParentContainer().isArchived()) {
					return ResponseEvent.userError(StorageContainerErrorCode.PARENT_ARCHIVED, container.getParentContainer().getName());
				}

				AccessCtrlMgr.getInstance().ensureCreateContainerRights(container);
				if (container.getType() != null && StringUtils.isNotBlank(container.getType().getNameFormat())) {
					generateName(container);
				}

				if (StringUtils.isBlank(container.getName()) || container.getName().equals(StorageContainer.DUMMY_NAME)) {
					return ResponseEvent.userError(StorageContainerErrorCode.NAME_REQUIRED);
				}

				ensureUniqueConstraints(null, container);
				container.validateRestrictions();
				if (container.getPosition() != null) {
					container.getPosition().occupy();
				}

				daoFactory.getStorageContainerDao().saveOrUpdate(container);
				container.addOrUpdateExtension();
				result.add(StorageContainerSummary.from(container));

				if (detail.isPrintLabels()) {
					toPrint.add(container);
				}
			}

			printLabels(toPrint);
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ContainerTransferEventDetail>> getTransferEvents(RequestEvent<ContainerQueryCriteria> req) {
		try {
			StorageContainer container = getContainer(req.getPayload());
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
			return ResponseEvent.response(ContainerTransferEventDetail.from(container.getTransferEvents()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ExportedFileDetail> exportTransferEvents(RequestEvent<ContainerReportCriteria> req) {
		try {
			Set<SiteCpPair> allowedSiteCps = AccessCtrlMgr.getInstance().getReadAccessContainerSiteCps();
			if (allowedSiteCps != null && allowedSiteCps.isEmpty()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			ContainerReportCriteria crit = req.getPayload();
			crit.siteCps(allowedSiteCps);

			User user = AuthUtil.getCurrentUser();
			String ipAddress = AuthUtil.getRemoteAddr();
			Future<ResponseEvent<String>> result = taskExecutor.submit(new Callable<ResponseEvent<String>>() {
				@Override
				public ResponseEvent<String> call() throws Exception {
					try {
						AuthUtil.setCurrentUser(user, ipAddress);
						return exportTransferEvents0(crit);
					} catch (Exception e) {
						return ResponseEvent.serverError(e);
					} finally {
						AuthUtil.clearCurrentUser();
					}
				}
			});

			try {
				ResponseEvent<String> resp = result.get(30000, TimeUnit.MILLISECONDS);
				resp.throwErrorIfUnsuccessful();

				ExportedFileDetail file = new ExportedFileDetail(true);
				file.setName(resp.getPayload());
				return ResponseEvent.response(file);
			} catch (TimeoutException te) {
				return ResponseEvent.response(new ExportedFileDetail(false));
			} catch (OpenSpecimenException ose) {
				return ResponseEvent.error(ose);
			} catch (Exception e) {
				return ResponseEvent.serverError(e);
			}
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerSummary>> findEmptyPlaces(RequestEvent<FindPlacesCriteria> req) {
		try {
			FindPlacesCriteria criteria = req.getPayload();
			Set<SiteCpPair> allowedSiteCps = AccessCtrlMgr.getInstance().getReadAccessContainerSiteCps(criteria.getCpId());
			if (allowedSiteCps != null && allowedSiteCps.isEmpty()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			if (criteria.getCpId() != null) {
				allowedSiteCps = getRequiredSiteCps(allowedSiteCps, Collections.singleton(criteria.getCpId()));
				if (allowedSiteCps.isEmpty()) {
					return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
				}
			}

			criteria.setSiteCps(allowedSiteCps);
			if (criteria.getRequiredPlaces() <= 0) {
				criteria.setRequiredPlaces(1);
			}

			return ResponseEvent.response(daoFactory.getStorageContainerDao().findEmptyPlaces(criteria));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<List<LabelTokenDetail>> getPrintLabelTokens() {
		return ResponseEvent.response(LabelTokenDetail.from("print_", labelPrinter.getTokens()));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<LabelPrintJobSummary> printContainerLabels(RequestEvent<PrintContainerLabelDetail> req) {
		try {
			PrintContainerLabelDetail input = req.getPayload();

			StorageContainerListCriteria crit = new StorageContainerListCriteria()
				.ids(input.getContainerIds()).names(input.getContainerNames());
			addContainerListCriteria(crit);

			List<StorageContainer> containers = daoFactory.getStorageContainerDao().getStorageContainers(crit);
			LabelPrintJob job = labelPrinter.print(PrintItem.make(containers, input.getCopies()));
			if (job == null) {
				return ResponseEvent.userError(StorageContainerErrorCode.NONE_PRINTED);
			}

			job.generateLabelsDataFile();
			return ResponseEvent.response(LabelPrintJobSummary.from(job));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerPositionDetail>> blockPositions(RequestEvent<PositionsDetail> req) {
		try {
			PositionsDetail opDetail = req.getPayload();
			StorageContainer container = getContainer(opDetail.getContainerId(), opDetail.getContainerName());
			if (container.isArchived()) {
				return ResponseEvent.userError(StorageContainerErrorCode.ARCHIVED, container.getName());
			}

			AccessCtrlMgr.getInstance().ensureUpdateContainerRights(container);
			if (container.isDimensionless()) {
				return ResponseEvent.userError(StorageContainerErrorCode.DL_POS_BLK_NP, container.getName());
			}

			List<StorageContainerPosition> positions = Utility.nullSafeStream(opDetail.getPositions())
				.map(detail -> container.createPosition(detail.getPosOne(), detail.getPosTwo()))
				.collect(Collectors.toList());

			if (positions.isEmpty()) {
				container.blockAllPositions();
			} else {
				container.blockPositions(positions);
			}


			daoFactory.getStorageContainerDao().saveOrUpdate(container, true);
			return ResponseEvent.response(StorageContainerPositionDetail.from(container.getOccupiedPositions()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerPositionDetail>> unblockPositions(RequestEvent<PositionsDetail> req) {
		try {
			PositionsDetail opDetail = req.getPayload();
			StorageContainer container = getContainer(opDetail.getContainerId(), opDetail.getContainerName());
			if (container.isArchived()) {
				return ResponseEvent.userError(StorageContainerErrorCode.ARCHIVED, container.getName());
			}

			AccessCtrlMgr.getInstance().ensureUpdateContainerRights(container);
			if (container.isDimensionless()) {
				return ResponseEvent.userError(StorageContainerErrorCode.DL_POS_BLK_NP, container.getName());
			}

			List<StorageContainerPosition> positions = Utility.nullSafeStream(opDetail.getPositions())
				.map(detail -> container.createPosition(detail.getPosOne(), detail.getPosTwo()))
				.collect(Collectors.toList());

			if (positions.isEmpty()) {
				container.unblockAllPositions();
			} else {
				container.unblockPositions(positions);
			}

			daoFactory.getStorageContainerDao().saveOrUpdate(container, true);
			return ResponseEvent.response(StorageContainerPositionDetail.from(container.getOccupiedPositions()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageLocationSummary>> reservePositions(RequestEvent<ReservePositionsOp> req) {
		long t1 = System.currentTimeMillis();
		try {
			ReservePositionsOp op = req.getPayload();
			if (StringUtils.isNotBlank(op.getReservationToCancel())) {
				deleteReservations(op.getReservationToCancel());
			}

			if (op.getCpId() != null && op.getCpId() > 0L) {
				for (ContainerCriteria criteria : op.getCriteria()) {
					criteria.setCpId(op.getCpId());
				}
			}

			Map<Long, CollectionProtocol> cpCache                   = new HashMap<>();
			Map<Long, ContainerSelectionStrategy> selectionStrategy = new HashMap<>();
			Map<Long, Set<SiteCpPair>> cpSites                      = new HashMap<>();

			String reservationId = StorageContainer.getReservationId();
			Date reservationTime = Calendar.getInstance().getTime();
			List<StorageContainerPosition> reservedPositions = new ArrayList<>();

			for (ContainerCriteria criteria : op.getCriteria()) {
				CollectionProtocol cp = cpCache.get(criteria.cpId());
				if (cp == null) {
					cp = daoFactory.getCollectionProtocolDao().getById(criteria.cpId());
					if (cp == null) {
						throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND, criteria.cpId());
					}

					if (StringUtils.isBlank(cp.getContainerSelectionStrategy())) {
						reservedPositions.addAll(nullPositions(criteria));
						continue;
					}

					ContainerSelectionStrategy strategy = selectionStrategyFactory.getStrategy(cp.getContainerSelectionStrategy());
					if (strategy == null) {
						throw OpenSpecimenException.userError(StorageContainerErrorCode.INV_CONT_SEL_STRATEGY, cp.getContainerSelectionStrategy());
					}

					Set<SiteCpPair> allowedSiteCps = AccessCtrlMgr.getInstance().getReadAccessContainerSiteCps(criteria.cpId());
					if (allowedSiteCps != null && allowedSiteCps.isEmpty()) {
						reservedPositions.addAll(nullPositions(criteria));
						continue;
					}

					Set<SiteCpPair> reqSiteCps = getRequiredSiteCps(allowedSiteCps, Collections.singleton(criteria.cpId()));
					if (CollectionUtils.isEmpty(reqSiteCps)) {
						reservedPositions.addAll(nullPositions(criteria));
						continue;
					}

					cpCache.put(cp.getId(), cp);
					selectionStrategy.put(cp.getId(), strategy);
					cpSites.put(cp.getId(), reqSiteCps);
				}


				criteria.siteCps(cpSites.get(criteria.cpId()));
				if (StringUtils.isNotBlank(criteria.ruleName())) {
					ContainerSelectionRule rule = selectionStrategyFactory.getRule(criteria.ruleName());
					if (rule == null) {
						throw OpenSpecimenException.userError(StorageContainerErrorCode.INV_CONT_SEL_RULE, criteria.ruleName());
					}

					criteria.rule(rule);
				}

				boolean allAllocated = false;
				while (!allAllocated) {
					long t2 = System.currentTimeMillis();
					StorageContainer container = selectionStrategy.get(criteria.cpId()).getContainer(criteria, cp.getAliquotsInSameContainer());

					int numPositions = criteria.minFreePositions();
					if (numPositions <= 0) {
						numPositions = 1;
					}

					List<StorageContainerPosition> positions;
					if (container == null) {
						positions = nullPositions(criteria);
					} else {
						positions = container.reservePositions(reservationId, reservationTime, numPositions);
					}

					reservedPositions.addAll(positions);
					numPositions -= positions.size();
					if (numPositions == 0) {
						allAllocated = true;
					} else {
						criteria.minFreePositions(numPositions);
					}

					logger.info("Allocation round time: " + (System.currentTimeMillis() - t2) + " ms");
				}
			}

			if (reservedPositions.stream().allMatch(Objects::isNull)) {
				reservedPositions = Collections.emptyList(); // all nulls. therefore return empty lists
			}

			return ResponseEvent.response(StorageLocationSummary.from(reservedPositions));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			logger.info("Total time for auto-allocation: " + (System.currentTimeMillis() - t1) + " ms");
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> cancelReservation(RequestEvent<String> req) {
		try {
			return ResponseEvent.response(deleteReservations(req.getPayload()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ExportedFileDetail> defragment(RequestEvent<ContainerDefragDetail> req) {
		ContainerDefragDetail input = req.getPayload();
		ContainerQueryCriteria crit = null;
		if (input.getId() != null) {
			crit = new ContainerQueryCriteria(input.getId());
		} else {
			crit = new ContainerQueryCriteria(input.getName());
		}

		return exportReport(RequestEvent.wrap(crit), defragReport, input);
	}

	@Override
	public ResponseEvent<FileDetail> getReport(RequestEvent<String> req) {
		String fileId = req.getPayload();
		String[] parts = fileId.split("_", 4); // <extn_uuid_userid_name>
		if (parts.length < 4) {
			return ResponseEvent.userError(CommonErrorCode.INVALID_INPUT, fileId);
		}

		if (!parts[2].equals(AuthUtil.getCurrentUser().getId().toString())) {
			return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
		}

		String filename = String.join("_", parts[0], parts[1], parts[2], parts[3]) + "." + parts[0];
		File rptFile = Utility.getFile(ConfigUtil.getInstance().getReportsDir(), filename);
		if (!rptFile.exists()) {
			return ResponseEvent.userError(CommonErrorCode.FILE_NOT_FOUND, fileId);
		}

		FileDetail result = new FileDetail();
		result.setContentType(Utility.getContentType(rptFile));
		result.setFilename(parts[3] + "." + parts[0]);
		result.setFileOut(rptFile);
		return ResponseEvent.response(result);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<StorageContainerSummary> getAncestorsHierarchy(RequestEvent<ContainerQueryCriteria> req) {
		try {
			StorageContainer container = getContainer(req.getPayload());
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);

			StorageContainerSummary summary = null;
			if (container.getParentContainer() == null) {
				summary = new StorageContainerSummary();
				summary.setId(container.getId());
				summary.setName(container.getName());
				summary.setDisplayName(container.getDisplayName());
				summary.setNoOfRows(container.getNoOfRows());
				summary.setNoOfColumns(container.getNoOfColumns());
			} else {
				summary = daoFactory.getStorageContainerDao().getAncestorsHierarchy(container.getId());
			}

			return ResponseEvent.response(summary);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerSummary>> getChildContainers(RequestEvent<ContainerQueryCriteria> req) {
		try {
			StorageContainer container = getContainer(req.getPayload());
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
			return ResponseEvent.response(daoFactory.getStorageContainerDao().getChildContainers(container));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerSummary>> getDescendantContainers(RequestEvent<StorageContainerListCriteria> req) {
		StorageContainer container = getContainer(req.getPayload().parentContainerId(), null);
		AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
		List<StorageContainer> containers = daoFactory.getStorageContainerDao().getDescendantContainers(req.getPayload());
		return ResponseEvent.response(StorageContainerSummary.from(containers));
	}

	@Override
	@RollbackTransaction
	public ResponseEvent<List<StorageLocationSummary>> getVacantPositions(RequestEvent<VacantPositionsOp> req) {
		try {
			VacantPositionsOp detail = req.getPayload();
			StorageContainer container = getContainer(detail.getContainerId(), detail.getContainerName());
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);

			int numPositions = detail.getRequestedPositions();
			if (numPositions <= 0) {
				numPositions = 1;
			}

			List<StorageContainerPosition> vacantPositions = new ArrayList<>();
			for (int i = 0; i < numPositions; ++i) {
				StorageContainerPosition position = null;
				if (i == 0) {
					if (StringUtils.isNotBlank(detail.getStartRow()) && StringUtils.isNotBlank(detail.getStartColumn())) {
						position = container.nextAvailablePosition(detail.getStartRow(), detail.getStartColumn());
					} else if (detail.getStartPosition() > 0) {
						position = container.nextAvailablePosition(detail.getStartPosition());
					} else {
						position = container.nextAvailablePosition();
					}
				} else {
					position = container.nextAvailablePosition(true);
				}

				if (position == null) {
					throw OpenSpecimenException.userError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
				}

				container.addPosition(position);
				vacantPositions.add(position);
			}

			return ResponseEvent.response(
				vacantPositions.stream().map(StorageLocationSummary::from).collect(Collectors.toList()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> unblockCheckoutPositions(RequestEvent<List<SpecimenInfo>> req) {
		try {
			int count = 0;
			Set<StorageContainer> allowedContainers = new HashSet<>();
			Set<Specimen> seenSpecimens = new HashSet<>();
			for (SpecimenInfo inputSpmn : req.getPayload()) {
				Specimen spmn = specimenResolver.getSpecimen(inputSpmn.getId(), inputSpmn.getCpShortTitle(), inputSpmn.getLabel(), inputSpmn.getBarcode());
				if (!seenSpecimens.add(spmn)) {
					continue;
				}

				if (spmn.getCheckoutPosition() == null) {
					continue;
				}

				AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(spmn);
				if (!allowedContainers.contains(spmn.getCheckoutPosition().getContainer())) {
					AccessCtrlMgr.getInstance().ensureUpdateContainerRights(spmn.getCheckoutPosition().getContainer());
					allowedContainers.add(spmn.getCheckoutPosition().getContainer());
				}

				daoFactory.getStorageContainerPositionDao().delete(spmn.getCheckoutPosition());
				spmn.setCheckoutPosition(null);
				++count;
			}

			return ResponseEvent.response(Collections.singletonMap("count", count));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	//
	// Box scanning APIs
	//
	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> addBoxSpecimens(RequestEvent<BoxDetail> req) {
		try {
			BoxDetail input = req.getPayload();
			if (StringUtils.isBlank(input.getType())) {
				return ResponseEvent.userError(StorageContainerErrorCode.TYPE_REQUIRED);
			}

			StorageContainerDetail boxDetail = new StorageContainerDetail();
			if (StringUtils.isNotBlank(input.getName())) {
				boxDetail.setName(input.getName());
			} else {
				boxDetail.setName("dummy_" + Calendar.getInstance().getTime().getTime());
			}

			boxDetail.setBarcode(input.getBarcode());
			boxDetail.setTypeName(input.getType());
			boxDetail.setStorageLocation(input.getStorageLocation());
			boxDetail.setAllowedSpecimenClasses(input.getAllowedSpecimenClasses());
			boxDetail.setAllowedSpecimenTypes(input.getAllowedSpecimenTypes());
			boxDetail.setAllowedCollectionProtocols(input.getAllowedCollectionProtocols());

			StorageContainer box = createStorageContainer(null, boxDetail);
			if (StringUtils.isBlank(input.getName())) {
				if (StringUtils.isBlank(box.getType().getNameFormat())) {
					return ResponseEvent.userError(StorageContainerErrorCode.NAME_REQUIRED);
				}

				box.setName(nameGenerator.generateLabel(box.getType().getNameFormat(), box));
			}

			List<StorageContainerPosition> positions = Utility.nullSafeStream(input.getPositions())
				.map(posDetail -> createPosition(box, posDetail, false))
				.collect(Collectors.toList());

			box.assignPositions(positions, false);
			daoFactory.getStorageContainerDao().saveOrUpdate(box, true);

			Map<String, Object> result = new HashMap<>();
			result.put("id", box.getId());
			result.put("name", box.getName());
			result.put("barcode", box.getBarcode());
			result.put("specimens", positions.size());
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> updateBoxSpecimens(RequestEvent<BoxDetail> req) {
		try {
			BoxDetail input = req.getPayload();
			StorageContainer box = getContainer(input.getId(), input.getName(), input.getBarcode());

			StorageContainerDetail boxDetail = new StorageContainerDetail();
			boxDetail.setId(box.getId());
			boxDetail.setStorageLocation(input.getStorageLocation());
			if (input.isAttrModified("allowedSpecimenClasses")) {
				boxDetail.setAllowedSpecimenClasses(input.getAllowedSpecimenClasses());
			}

			if (input.isAttrModified("allowedSpecimenTypes")) {
				boxDetail.setAllowedSpecimenTypes(input.getAllowedSpecimenTypes());
			}

			if (input.isAttrModified("allowedCollectionProtocols")) {
				boxDetail.setAllowedCollectionProtocols(input.getAllowedCollectionProtocols());
			}

			updateStorageContainer0(boxDetail, true);

			Map<Long, Specimen> existing = new HashMap<>();
			for (StorageContainerPosition existingPos : box.getOccupiedPositions()) {
				if (existingPos.getOccupyingSpecimen() != null) {
					existing.put(existingPos.getOccupyingSpecimen().getId(), existingPos.getOccupyingSpecimen());
				}
			}

			List<StorageContainerPosition> positions = new ArrayList<>();
			for (StorageContainerPositionDetail pos : input.getPositions()) {
				existing.remove(pos.getOccupyingEntityId());
				positions.add(createPosition(box, pos, true));
			}

			box.assignPositions(positions, true);
			if (StringUtils.isBlank(input.getRemoveSpecimensReason())) {
				input.setRemoveSpecimensReason("NOT_STORED");
			}

			if ("DISPOSED".equals(input.getRemoveSpecimensReason())) {
				existing.values().forEach(spmn -> spmn.close(AuthUtil.getCurrentUser(), Calendar.getInstance().getTime(), Specimen.NOT_SPECIFIED, null));
			} else {
				existing.values().forEach(spmn -> spmn.virtualise(input.getRemoveSpecimensComments()));
			}

			daoFactory.getStorageContainerDao().saveOrUpdate(box, true);

			Map<String, Object> result = new HashMap<>();
			result.put("id", box.getId());
			result.put("name", box.getName());
			result.put("barcode", box.getBarcode());
			result.put("specimens", positions.size());
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenInfo>> getMissingSpecimens(RequestEvent<BoxDetail> req) {
		try {
			Pair<StorageContainer, Collection<Specimen>> result = getMissingSpecimens(req.getPayload());
			if (result.second() == null) {
				return ResponseEvent.response(null);
			}

			return ResponseEvent.response(result.second().stream().map(SpecimenInfo::from).toList());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ExportedFileDetail> getMissingSpecimensReport(RequestEvent<BoxDetail> req) {
		try {
			Date startTime = Calendar.getInstance().getTime();
			Pair<StorageContainer, Collection<Specimen>> result = getMissingSpecimens(req.getPayload());
			if (result.second() == null) {
				return ResponseEvent.response(null);
			}

			String fileId = String.join(
				"_",
				"csv",
				UUID.randomUUID().toString(),
				AuthUtil.getCurrentUser().getId().toString(),
				result.first().getBarcode());


			File output = new File(ConfigUtil.getInstance().getReportsDir(), fileId + ".csv");
			output.deleteOnExit();

			try (CsvWriter writer = CsvFileWriter.createCsvFileWriter(output)) {
				StorageContainer box = result.first();
				writer.writeNext(new String[] { msg("storage_container_name"), box.getName() });
				writer.writeNext(new String[] { msg("storage_container_barcode"), box.getBarcode() });
				writer.writeNext(new String[] { msg("storage_container_display_name"), box.getDisplayName() });
				writer.writeNext(new String[] { msg("storage_container_hierarchy"), box.getStringifiedAncestors() });
				writer.writeNext(new String[] { msg("common_exported_by"), Objects.requireNonNull(AuthUtil.getCurrentUser()).formattedName() });
				writer.writeNext(new String[] { msg("common_exported_on"), Utility.getDateTimeString(Calendar.getInstance().getTime()) });
				writer.writeNext(new String[] { } );

				writer.writeNext(new String[] {
					msg("specimen_barcode"), msg("specimen_label"), msg("cp"),
					msg("specimen_class"), msg("specimen_type"), msg("specimen_available_quantity"),
					msg("specimen_spec_position_container_row"), msg("specimen_spec_position_container_column")
				});

				DecimalFormat df = new DecimalFormat();
				df.setMaximumFractionDigits(2);
				for (Specimen spmn : result.second()) {
					writer.writeNext(new String[] {
						spmn.getBarcode(), spmn.getLabel(), spmn.getCpShortTitle(),
						spmn.getSpecimenClass().getValue(), spmn.getSpecimenType().getValue(),
						spmn.getAvailableQuantity() != null ? df.format(spmn.getAvailableQuantity()) : null,
						spmn.getPosition().getPosTwo(), spmn.getPosition().getPosOne()
					});
				}
			}

			ExportedFileDetail fileDetail = new ExportedFileDetail(true);
			fileDetail.setName(fileId);
			exportSvc.saveJob("box_missing_specimens_report", startTime, Collections.singletonMap("containerId", result.first().getId().toString()));
			return ResponseEvent.response(fileDetail);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}


	//
	// Automated freezer related APIs
	//
	@Override
	public void processStoreLists(Supplier<List<ContainerStoreList>> supplier) {
		List<Long> failedStoreListIds = new ArrayList<>();

		List<ContainerStoreList> storeLists;
		while ((storeLists = supplier.get()) != null && !storeLists.isEmpty()) {
			for (ContainerStoreList storeList : storeLists) {
				boolean firstAttempt = (storeList.getNoOfRetries() == 0);
				ContainerStoreList.Status status = storeList.process();
				if (status == ContainerStoreList.Status.FAILED && firstAttempt) {
					failedStoreListIds.add(storeList.getId());
				}
			}
		}

		if (!failedStoreListIds.isEmpty()) {
			generateAutoFreezerReport(new AutoFreezerReportDetail(failedStoreListIds));
		}
	}

	@Override
	@PlusTransactional
	public File generateAutoFreezerReport(AutoFreezerReportDetail input) {
		try {
			List<StorageContainer> freezers = null;
			if (input.reportForFailedOps()) {
				freezers = daoFactory.getContainerStoreListDao().getAutomatedFreezers(input.getFailedStoreListIds());
			} else {
				freezers = daoFactory.getStorageContainerDao().getAutomatedFreezers();
			}

			File lastReport = null;
			for (StorageContainer freezer : freezers) {
				if (CollectionUtils.isEmpty(freezer.getSite().getCoordinators())) {
					logger.info("Not generating store list report for the freezer, as it has no site coordinators. Freezer: " + freezer.getName());
					continue;
				}

				AutoFreezerReportDetail detail = generateStoreListsFailureReport(freezer, input);
				if (!detail.reportForFailedOps()) {
					Map<ContainerStoreList.Op, Integer> itemsCnt = daoFactory.getContainerStoreListDao()
						.getStoreListItemsCount(detail.getFromDate(), detail.getToDate());
					detail.setStored(itemsCnt.get(Op.PUT) == null ? 0 : itemsCnt.get(Op.PUT));
					detail.setRetrieved(itemsCnt.get(Op.PICK) == null ? 0 : itemsCnt.get(Op.PICK));
				}

				sendAutoFreezerReport(freezer, detail);
				if (detail.getReport() != null) {
					lastReport = detail.getReport();
				}
			}


			return lastReport;
		} catch (Exception e) {
			logger.error("Error generating automated freezer report", e);
			throw OpenSpecimenException.serverError(e);
		}
	}

	@Override
	public StorageContainer createStorageContainer(StorageContainer base, StorageContainerDetail input) {
		StorageContainer container = containerFactory.createStorageContainer(base, input);
		if (container.getParentContainer() != null && container.getParentContainer().isArchived()) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.PARENT_ARCHIVED, container.getParentContainer().getName());
		}

		if (StringUtils.isBlank(container.getName())) {
			if (container.getType() == null || StringUtils.isBlank(container.getType().getNameFormat())) {
				throw OpenSpecimenException.userError(StorageContainerErrorCode.NAME_REQUIRED);
			}

			generateName(container);
		}

		AccessCtrlMgr.getInstance().ensureCreateContainerRights(container);
		ensureUniqueConstraints(null, container);
		container.validateRestrictions();

		if (container.isStoreSpecimenEnabled()) {
			container.setFreezerCapacity();
		}

		if (container.getPosition() != null) {
			container.getPosition().occupy();
		}

		daoFactory.getStorageContainerDao().saveOrUpdate(container, true);
		container.addOrUpdateExtension();
		return container;
	}

	@Override
	@PlusTransactional
	public StorageContainer createSiteContainer(Long siteId, String siteName) {
		Site site = getSite(siteId, siteName);
		if (site.getContainer() != null) {
			return site.getContainer();
		}

		StorageContainerDetail detail = new StorageContainerDetail();
		detail.setName(StorageContainer.getDefaultSiteContainerName(site));
		detail.setSiteName(site.getName());

		StorageContainer container = containerFactory.createStorageContainer(detail);
		daoFactory.getStorageContainerDao().saveOrUpdate(container, true);
		return container;
	}

	@Override
	@PlusTransactional
	public boolean toggleStarredContainer(Long containerId, boolean starred) {
		try {
			StorageContainer container = getContainer(containerId, null);
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
			if (starred) {
				starredItemSvc.save(getObjectName(), container.getId());
			} else {
				starredItemSvc.delete(getObjectName(), container.getId());
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
		return StorageContainer.getEntityName();
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getStorageContainerDao().getContainerIds(key, value);
	}

	@Override
	public String getAuditTable() {
		return "OS_STORAGE_CONTAINERS_AUD";
	}

	@Override
	public void ensureReadAllowed(Long objectId) {
		StorageContainer container = getContainer(objectId, null);
		AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		taskManager.scheduleWithFixedDelay(
			new Runnable() {
				@Override
				public void run() {
					try {
						logger.debug("Woken up to clean the stale reserved container slots...");
						run0();
					} catch (Throwable e) {
						logger.error("Error deleting stale reserved container slots", e);
					}
				}

				@PlusTransactional
				private void run0() {
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.MINUTE, -5);

					int count = daoFactory.getStorageContainerDao().deleteReservedPositionsOlderThan(cal.getTime());
					if (count > 0) {
						logger.info(String.format("Cleaned up %d stale container slot reservations", count));
					}
				}
			}, 5
		);

		exportSvc.registerObjectsGenerator("storageContainer", this::getContainersGenerator);
		listSvc.registerListConfigurator("container-specimens-list-view", this::getSpecimensListConfig);
	}

	private StorageContainerListCriteria addContainerListCriteria(StorageContainerListCriteria crit) {
		Set<SiteCpPair> allowedSiteCps = AccessCtrlMgr.getInstance().getReadAccessContainerSiteCps();
		if (allowedSiteCps != null && allowedSiteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		if (CollectionUtils.isNotEmpty(crit.cpIds())) {
			allowedSiteCps = getRequiredSiteCps(allowedSiteCps, crit.cpIds());
			if (allowedSiteCps.isEmpty()) {
				throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}
		}

		return crit.siteCps(allowedSiteCps);
	}

	private Set<SiteCpPair> getRequiredSiteCps(Set<SiteCpPair> allowedSiteCps, Set<Long> cpIds) {
		Set<SiteCpPair> reqSiteCps = daoFactory.getCollectionProtocolDao().getSiteCps(cpIds);
		if (allowedSiteCps == null) {
			allowedSiteCps = reqSiteCps;
		} else {
			allowedSiteCps = getSiteCps(allowedSiteCps, reqSiteCps);
		}

		return allowedSiteCps;
	}

	private Set<SiteCpPair> getSiteCps(Set<SiteCpPair> allowed, Set<SiteCpPair> required) {
		Set<SiteCpPair> result = new HashSet<>();
		for (SiteCpPair reqSiteCp : required) {
			for (SiteCpPair allowedSiteCp : allowed) {
				if (allowedSiteCp.getSiteId() != null && !allowedSiteCp.getSiteId().equals(reqSiteCp.getSiteId())) {
					continue;
				}

				if (allowedSiteCp.getSiteId() == null && !allowedSiteCp.getInstituteId().equals(reqSiteCp.getInstituteId())) {
					continue;
				}

				if (allowedSiteCp.getCpId() != null && !allowedSiteCp.getCpId().equals(reqSiteCp.getCpId())) {
					continue;
				}

				result.add(reqSiteCp);
			}
		}

		return result;
	}

	private void setStoredSpecimensCount(StorageContainerListCriteria crit, List<StorageContainerSummary> containers) {
		if (!crit.includeStat() || !crit.topLevelContainers()) {
			return;
		}

		List<Long> containerIds = containers.stream().map(StorageContainerSummary::getId).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(containerIds)) {
			return;
		}

		Map<Long, Integer> countMap = daoFactory.getStorageContainerDao().getRootContainerSpecimensCount(containerIds);
		containers.forEach(c -> c.setStoredSpecimens(countMap.get(c.getId())));
	}

	private StorageContainer getContainer(ContainerQueryCriteria crit) {
		return getContainer(crit.getId(), crit.getName(), crit.getBarcode(), StorageContainerErrorCode.ID_NAME_OR_BARCODE_REQ, crit.errorIfNotFound());
	}

	private StorageContainer getContainer(Long id, String name) {
		return getContainer(id, name, null);
	}
	
	private StorageContainer getContainer(Long id, String name, String barcode) {
		return getContainer(id, name, barcode, StorageContainerErrorCode.ID_NAME_OR_BARCODE_REQ, true);
	}
	
	private StorageContainer getContainer(Long id, String name, String barcode, ErrorCode requiredErrCode, boolean errorIfNotFound) {
		StorageContainer container = null;
		Object key = null;

		if (id != null) {
			container = daoFactory.getStorageContainerDao().getById(id);
			key = id;
		} else {
			if (StringUtils.isNotBlank(name)) {
				container = daoFactory.getStorageContainerDao().getByName(name);
				key = name;
			}

			if (container == null && StringUtils.isNotBlank(barcode)) {
				container = daoFactory.getStorageContainerDao().getByBarcode(barcode);
				key = barcode;
			}
		}

		if (key == null) {
			throw OpenSpecimenException.userError(requiredErrCode);
		} else if (container == null && errorIfNotFound) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NOT_FOUND, key, 1);
		}

		return container;
	}

	private ResponseEvent<StorageContainerDetail> updateStorageContainer(RequestEvent<StorageContainerDetail> req, boolean partial) {
		try {
			StorageContainer result = updateStorageContainer0(req.getPayload(), partial);
			return ResponseEvent.response(StorageContainerDetail.from(result));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private StorageContainer updateStorageContainer0(StorageContainerDetail input, boolean partial) {
		StorageContainer existing = getContainer(
			input.getId(),
			input.getName(),
			input.getBarcode(),
			StorageContainerErrorCode.ID_NAME_OR_BARCODE_REQ,
			!Boolean.TRUE.equals(input.getCreateIfAbsent())
		);

		if (existing == null) {
			return createStorageContainer(null, input);
		}

		AccessCtrlMgr.getInstance().ensureUpdateContainerRights(existing);
		input.setId(existing.getId());
		StorageContainer container;
		if (partial) {
			container = containerFactory.createStorageContainer(existing, input);
		} else {
			container = containerFactory.createStorageContainer(input);
		}

		ensureUniqueConstraints(existing, container);
		existing.update(container);
		if (StringUtils.isBlank(existing.getName())) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NAME_REQUIRED);
		}

		daoFactory.getStorageContainerDao().saveOrUpdate(existing, true);
		existing.updatePositionsIfChanged();
		existing.validateRestrictions();
		existing.addOrUpdateExtension();
		return existing;
	}
	
	private void ensureUniqueConstraints(StorageContainer existing, StorageContainer newContainer) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		
		if (!isUniqueName(existing, newContainer)) {
			ose.addError(StorageContainerErrorCode.DUP_NAME, newContainer.getName());
		}
		
		if (!isUniqueBarcode(existing, newContainer)) {
			ose.addError(StorageContainerErrorCode.DUP_BARCODE);
		}
		
		ose.checkAndThrow();
	}
	
	private boolean isUniqueName(StorageContainer existingContainer, StorageContainer newContainer) {
		if (existingContainer != null && existingContainer.getName().equals(newContainer.getName())) {
			return true;
		}
		
		return isUniqueName(newContainer.getName());
	}
		
	private boolean isUniqueName(String name) {
		StorageContainer container = daoFactory.getStorageContainerDao().getByName(name);
		return container == null;
	}
	
	private boolean isUniqueBarcode(StorageContainer existingContainer, StorageContainer newContainer) {
		if (StringUtils.isBlank(newContainer.getBarcode())) {
			return true;
		}
		
		if (existingContainer != null && newContainer.getBarcode().equals(existingContainer.getBarcode())) {
			return true;
		}
		
		StorageContainer container = daoFactory.getStorageContainerDao().getByBarcode(newContainer.getBarcode());
		return container == null;
	}
	
	private StorageContainerPosition createPosition(StorageContainer container, StorageContainerPositionDetail pos, boolean vacateOccupant) {
		if (StringUtils.isBlank(pos.getPosOne()) ^ StringUtils.isBlank(pos.getPosTwo())) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.INV_POS, container.getName(), pos.getPosOne(), pos.getPosTwo());
		}
		
		String entityType = pos.getOccuypingEntity();
		if (StringUtils.isBlank(entityType)) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.INVALID_ENTITY_TYPE, "none");
		}
		
		if (StringUtils.isBlank(pos.getOccupyingEntityName()) && pos.getOccupyingEntityId() == null) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.OCCUPYING_ENTITY_ID_OR_NAME_REQUIRED);
		}
		
		if (entityType.equalsIgnoreCase("specimen")) {
			return createSpecimenPosition(container, pos, vacateOccupant);
		} else if (entityType.equalsIgnoreCase("container")) {
			return createChildContainerPosition(container, pos);
		}
		
		throw OpenSpecimenException.userError(StorageContainerErrorCode.INVALID_ENTITY_TYPE, entityType);
	}
	
	private StorageContainerPosition createSpecimenPosition(
			StorageContainer container,
			StorageContainerPositionDetail pos,
			boolean vacateOccupant) {


		Specimen specimen = getSpecimen(pos);
		if (!specimen.isActive() || specimen.isReserved()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.STORE_NOT_ALLOWED, specimen.getLabel());
		}

		AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(specimen, false);
		
		StorageContainerPosition position = null;
		if (!container.isDimensionless() && (StringUtils.isBlank(pos.getPosOne()) || StringUtils.isBlank(pos.getPosTwo()))) {
			position = new StorageContainerPosition();
			position.setOccupyingSpecimen(specimen);
			return position;
		}

		if (!container.canContain(specimen)) {
			throw OpenSpecimenException.userError(
				StorageContainerErrorCode.CANNOT_HOLD_SPECIMEN, container.getName(), specimen.getLabelOrDesc());
		}
		
		if (!container.canSpecimenOccupyPosition(specimen.getId(), pos.getPosOne(), pos.getPosTwo(), vacateOccupant)) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
		}
		
		position = container.createPosition(pos.getPosOne(), pos.getPosTwo());
		position.setOccupyingSpecimen(specimen);
		return position;		
	}

	private Specimen getSpecimen(StorageContainerPositionDetail pos) {
		return specimenResolver.getSpecimen(
			pos.getOccupyingEntityId(),
			pos.getCpShortTitle(),
			pos.getOccupyingEntityName()
		);
	}

	private SpecimenListCriteria addSiteCpRestrictions(SpecimenListCriteria crit, StorageContainer container) {
		return crit.siteCps(getReadAccessContainerSiteCps(container));
	}

	private List<SiteCpPair> getReadAccessContainerSiteCps(StorageContainer container) {
		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessContainerSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		List<SiteCpPair> result = null;
		if (siteCps != null) {
			result = siteCps.stream()
				.filter(siteCp -> {
					if (siteCp.getSiteId() == null) {
						return siteCp.getInstituteId().equals(container.getInstitute().getId());
					} else {
						return siteCp.getSiteId().equals(container.getSite().getId());
					}
				})
				.collect(Collectors.toList());
		}

		return result;
	}
	
	private StorageContainerPosition createChildContainerPosition(
			StorageContainer container, 
			StorageContainerPositionDetail pos) {
		
		StorageContainer childContainer = getContainer(pos.getOccupyingEntityId(), pos.getOccupyingEntityName());
		AccessCtrlMgr.getInstance().ensureUpdateContainerRights(childContainer);
		if (!container.canContain(childContainer)) {
			throw OpenSpecimenException.userError(
					StorageContainerErrorCode.CANNOT_HOLD_CONTAINER, 
					container.getName(), 
					childContainer.getName());
		}
		
		if (!container.canContainerOccupyPosition(childContainer.getId(), pos.getPosOne(), pos.getPosTwo())) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
		}
		
		StorageContainerPosition position = container.createPosition(pos.getPosOne(), pos.getPosTwo());
		position.setOccupyingContainer(childContainer);
		return position;
	}
	
	private StorageContainer replicateContainer(StorageContainer srcContainer, DestinationDetail dest) {
		StorageContainerDetail detail = new StorageContainerDetail();
		detail.setName(dest.getName());
		detail.setSiteName(dest.getSiteName());
		
		StorageLocationSummary storageLocation = new StorageLocationSummary();
		storageLocation.setId(dest.getParentContainerId());
		storageLocation.setName(dest.getParentContainerName());
		storageLocation.setPositionX(dest.getPosOne());
		storageLocation.setPositionY(dest.getPosTwo());
		storageLocation.setPosition(dest.getPosition());
		detail.setStorageLocation(storageLocation);

		return createStorageContainer(getContainerCopy(srcContainer), detail);
	}

	private List<StorageContainer> createContainerHierarchy(ContainerType containerType, StorageContainer parentContainer) {
		List<StorageContainer> result = new ArrayList<>();
		if (containerType == null || StringUtils.isBlank(containerType.getNameFormat())) {
			return result;
		}
		
		StorageContainer container = containerFactory.createStorageContainer("dummyName", containerType, parentContainer);
		int noOfContainers = parentContainer.getNoOfRows() * parentContainer.getNoOfColumns();
		boolean setCapacity = true;
		for (int i = 1; i <= noOfContainers; i++) {
			StorageContainer cloned = null;
			if (i == 1) {
				cloned = container;
			} else {
				cloned = container.copy();
				setPosition(cloned);
			}

			generateName(cloned);
			parentContainer.addChildContainer(cloned);

			if (cloned.isStoreSpecimenEnabled() && setCapacity) {
				cloned.setFreezerCapacity();
				setCapacity = false;
			}

			daoFactory.getStorageContainerDao().saveOrUpdate(cloned);
			result.add(cloned);

			List<StorageContainer> descendants = createContainerHierarchy(containerType.getCanHold(), cloned);
			result.addAll(descendants);
			descendants.clear();
		}

		return result;
	}

	@PlusTransactional
	private List<StorageContainer> getAccessibleAncestors(Set<Long> containerIds) {
		List<StorageContainer> containers = daoFactory.getStorageContainerDao().getByIds(containerIds);
		if (containerIds.size() != containers.size()) {
			containers.forEach(container -> containerIds.remove(container.getId()));
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NOT_FOUND, containerIds, containerIds.size());
		}

		List<StorageContainer> ancestors = getAncestors(containers);
		ancestors.forEach(AccessCtrlMgr.getInstance()::ensureDeleteContainerRights);
		return ancestors;
	}

	private List<StorageContainer> getAncestors(List<StorageContainer> containers) {
		Set<Long> descContIds = containers.stream()
			.flatMap(c -> c.getDescendentContainers().stream().filter(d -> !d.equals(c)))
			.map(StorageContainer::getId)
			.collect(Collectors.toSet());

		return containers.stream()
			.filter(c -> !descContIds.contains(c.getId()))
			.collect(Collectors.toList());
	}


	private int deleteContainerHierarchy(StorageContainer container, boolean vacateSpmns) {
		if (!vacateSpmns) {
			raiseErrorIfNotEmpty(container);
		}

		int clearedSpmns = 0;
		boolean endOfContainers = false;
		while (!endOfContainers) {
			List<Long> leafContainers = getLeafContainerIds(container);
			clearedSpmns += leafContainers.stream().mapToInt(cid -> deleteContainer(cid, vacateSpmns)).sum();
			endOfContainers = (leafContainers.isEmpty());
		}

		return clearedSpmns;
	}

	@PlusTransactional
	private void raiseErrorIfNotEmpty(StorageContainer container) {
		int storedSpmns = daoFactory.getStorageContainerDao().getSpecimensCount(container.getId());
		if (storedSpmns > 0) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.REF_ENTITY_FOUND, container.getName());
		}
	}

	@PlusTransactional
	private List<Long> getLeafContainerIds(StorageContainer container) {
		return daoFactory.getStorageContainerDao().getLeafContainerIds(container.getId(), 0, 100);
	}

	private int deleteContainer(Long containerId, boolean vacateSpmns) {
		int spmns = 0;
		if (vacateSpmns) {
			spmns = vacateSpecimens(containerId);
		}

		deleteContainer(containerId);
		return spmns;
	}

	private int vacateSpecimens(Long containerId) {
		boolean endOfSpmns = false;
		int startAt = 0, maxSpmns = 100, totalSpmns = 0;
		SpecimenListCriteria crit = new SpecimenListCriteria()
				.ancestorContainerId(containerId)
				.startAt(startAt)
				.maxResults(maxSpmns);

		while(!endOfSpmns) {
			int count = vacateSpecimensBatch(crit);
			totalSpmns += count;
			endOfSpmns = (count < maxSpmns);
		}

		return totalSpmns;
	}

	@PlusTransactional
	private int vacateSpecimensBatch(SpecimenListCriteria crit) {
		List<Specimen> specimens = daoFactory.getStorageContainerDao().getSpecimens(crit, false);
		specimens.forEach(s -> s.updatePosition(null));
		return specimens.size();
	}

	@PlusTransactional
	private void deleteContainer(Long containerId) {
		StorageContainer container = daoFactory.getStorageContainerDao().getById(containerId); // refresh from DB
		container.delete(false);
	}

	private void generateName(StorageContainer container) {
		ContainerType type = container.getType();
		String name = nameGenerator.generateLabel(type.getNameFormat(), container);
		if (StringUtils.isBlank(name)) {
			throw OpenSpecimenException.userError(
				StorageContainerErrorCode.INCORRECT_NAME_FMT,
				type.getNameFormat(),
				type.getName());
		}

		container.setName(name);
	}

	private void printLabels(StorageContainer container) {
		printLabels(Collections.singletonList(container));
	}

	private void printLabels(List<StorageContainer> containers) {
		if (CollectionUtils.isEmpty(containers)) {
			return;
		}

		labelPrinter.print(PrintItem.make(containers, 1));
	}

	private void setPosition(StorageContainer container) {
		StorageContainer parentContainer = container.getParentContainer();
		if (parentContainer == null) {
			return;
		}
		
		StorageContainerPosition position = parentContainer.nextAvailablePosition(true);
		if (position == null) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NO_FREE_SPACE, parentContainer.getName());
		} 
		
		position.setOccupyingContainer(container);
		container.setPosition(position);
	}

	private List<StorageContainerPosition> nullPositions(ContainerCriteria criteria) {
		int numPositions = criteria.minFreePositions();
		if (numPositions <= 0) {
			numPositions = 1;
		}

		return IntStream.range(0, numPositions)
			.mapToObj(i -> (StorageContainerPosition) null)
			.collect(Collectors.toList());
	}

	private int deleteReservations(String reservationId) {
		return daoFactory.getStorageContainerDao().deleteReservedPositions(Collections.singletonList(reservationId));
	}

	private StorageContainer getContainerCopy(StorageContainer source) {
		StorageContainer copy = new StorageContainer();
		copy.setUsedFor(source.getUsedFor());
		copy.setTemperature(source.getTemperature());
		copy.setNoOfColumns(source.getNoOfColumns());
		copy.setNoOfRows(source.getNoOfRows());
		copy.setColumnLabelingScheme(source.getColumnLabelingScheme());
		copy.setRowLabelingScheme(source.getRowLabelingScheme());
		copy.setPositionLabelingMode(source.getPositionLabelingMode());
		copy.setPositionAssignment(source.getPositionAssignment());
		copy.setComments(source.getComments());
		copy.setAllowedSpecimenClasses(new HashSet<>(source.getAllowedSpecimenClasses()));
		copy.setAllowedSpecimenTypes(new HashSet<>(source.getAllowedSpecimenTypes()));
		copy.setAllowedCps(new HashSet<>(source.getAllowedCps()));
		copy.setAllowedDps(new HashSet<>(source.getAllowedDps()));
		copy.setCompAllowedSpecimenClasses(copy.computeAllowedSpecimenClasses());
		copy.setCompAllowedSpecimenTypes(copy.computeAllowedSpecimenTypes());
		copy.setCompAllowedCps(copy.computeAllowedCps());
		copy.setCompAllowedDps(copy.computeAllowedDps());
		copy.setStoreSpecimenEnabled(source.isStoreSpecimenEnabled());
		copy.setCreatedBy(AuthUtil.getCurrentUser());
		return copy;
	}

	private Site getSite(Long siteId, String siteName) {
		Site site = null;
		Object key = null;
		if (siteId != null) {
			site = daoFactory.getSiteDao().getById(siteId);
			key = siteId;
		} else if (StringUtils.isNotBlank(siteName)) {
			site = daoFactory.getSiteDao().getSiteByName(siteName);
			key = siteName;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(SiteErrorCode.NAME_REQUIRED);
		} else if (site == null) {
			throw OpenSpecimenException.userError(SiteErrorCode.NOT_FOUND, key);
		}

		return site;
	}

	@PlusTransactional
	private ResponseEvent<ExportedFileDetail> exportReport(RequestEvent<ContainerQueryCriteria> req, ContainerReport report, Object... params) {
		try {
			StorageContainer container = getContainer(req.getPayload());
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
			return exportReport(report, Collections.singletonList(container), params);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private ResponseEvent<ExportedFileDetail> exportReport(ContainerReport report, List<StorageContainer> containers, Object... params) {
		User user = AuthUtil.getCurrentUser();
		String ipAddress = AuthUtil.getRemoteAddr();
		Future<ExportedFileDetail> result = taskExecutor.submit(
			() -> {
				Date startTime = Calendar.getInstance().getTime();
				Throwable t = null;
				String fileId = null;
				try {
					AuthUtil.setCurrentUser(user, ipAddress);
					ExportedFileDetail file = report.generate(containers, params);
					fileId = file != null ? file.getName() : null;

					Map<String, String> jobParams = null;
					if (params != null && params.length > 0 && params[0] instanceof ContainerReportCriteria) {
						jobParams = ((ContainerReportCriteria) params[0]).toStrMap();
					} else {
						jobParams = Collections.singletonMap("containerIds", containers.stream().map(c -> c.getId().toString()).collect(Collectors.joining(", ")));
					}

					exportSvc.saveJob(report.getName(), startTime, jobParams);
					return file;
				} catch (Throwable e) {
					t = e;
					throw e;
				} finally {
					StorageContainer container = null;
					if (containers.size() == 1) {
						container = containers.get(0);
					}

					sendReportEmail(report.getName(), user, container, fileId, t);
					AuthUtil.clearCurrentUser();
				}
			}
		);

		try {
			return ResponseEvent.response(result.get(30, TimeUnit.SECONDS));
		} catch (TimeoutException te) {
			return ResponseEvent.response(null);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private QueryDataExportResult exportResult(final StorageContainer container, SavedQuery query) {
		Filter filter = new Filter();
		filter.setField("Specimen.specimenPosition.allAncestors.ancestorId");
		filter.setOp(Filter.Op.EQ);
		filter.setValues(new String[] { container.getId().toString() });

		ExecuteQueryEventOp execReportOp = new ExecuteQueryEventOp();
		execReportOp.setDrivingForm("Participant");
		execReportOp.setAql(query.getAql(new Filter[] { filter }));
		execReportOp.setWideRowMode(WideRowMode.DEEP.name());
		execReportOp.setRunType("Export");
		execReportOp.setSavedQueryId(query.getId());

		String name = container.getDisplayName();
		if (StringUtils.isNotBlank(name)) {
			name += " / " + container.getName();
		} else {
			name = container.getName();
		}
		execReportOp.setReportName(MessageUtil.getInstance().getMessage("container_specimens_report", new String[] { name }));
		return querySvc.exportQueryData(execReportOp, new QueryService.ExportProcessor() {
			@Override
			public String filename() {
				return "container_" + container.getId() + "_" + UUID.randomUUID().toString();
			}

			@Override
			public void headers(OutputStream out) {
				Map<String, String> headers = new LinkedHashMap<>() {{
					String notSpecified = msg("common_not_specified");

					if (StringUtils.isNotBlank(container.getDisplayName())) {
						put(msg("container_display_name"), container.getDisplayName());
					}

					put(msg("container_name"), container.getName());
					put(msg("container_site"), container.getSite().getName());
					if (container.getParentContainer() != null) {
						put(msg("container_parent_container"), container.getParentContainer().getName());
					}

					if (container.getType() != null) {
						put(msg("container_type"), container.getType().getName());
					}

					put("", ""); // blank line
				}};

				Utility.writeKeyValuesToCsv(out, headers);
			}
		});
	}

	private ListConfig getSpecimensListConfig(Map<String, Object> req) {
		Number containerId = (Number) req.get("containerId");
		if (containerId == null) {
			containerId = (Number) req.get("objectId");
		}

		if (containerId == null) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.ID_REQ);
		}

		StorageContainer container = getContainer(containerId.longValue(), null);
		AccessCtrlMgr.getInstance().ensureReadContainerRights(container);

		ListConfig cfg = ListUtil.getSpecimensListConfig("container-specimens-list-view", true);
		ListUtil.addHiddenFieldsOfSpecimen(cfg);
		if (cfg == null) {
			return null;
		}

		String restriction = "Specimen.specimenPosition.allAncestors.ancestorId = " + container.getId();

		List<SiteCpPair> siteCps = getReadAccessContainerSiteCps(container);
		boolean useMrnSites = AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn();
		String cpSitesCond = BiospecimenDaoHelper.getInstance().getSiteCpsCondAql(siteCps, useMrnSites);
		if (StringUtils.isNotBlank(cpSitesCond)) {
			restriction += " and " + cpSitesCond;
		}

		List<Column> orderBy;
		if (container.isDimensionless()) {
			Column positionId = new Column();
			positionId.setExpr("Specimen.specimenPosition.positionId");
			positionId.setDirection("asc");

			orderBy = Collections.singletonList(positionId);
		} else {
			Column name = new Column();
			name.setExpr("Specimen.specimenPosition.containerName");
			name.setDirection("asc");

			Column row = new Column();
			row.setExpr("Specimen.specimenPosition.rowOrdinal");
			row.setDirection("asc");

			Column col = new Column();
			col.setExpr("Specimen.specimenPosition.columnOrdinal");
			col.setDirection("asc");

			orderBy = Arrays.asList(name, row, col);
		}


		cfg.setDrivingForm("Specimen");
		cfg.setRestriction(restriction);
		cfg.setDistinct(true);
		cfg.setOrderBy(orderBy);
		return ListUtil.setListLimit(cfg, req);
	}

	private Function<ExportJob, List<? extends Object>> getContainersGenerator() {
		return new Function<ExportJob, List<? extends Object>>() {
			private boolean paramsInited;

			private boolean loadTopLevelContainers = true;

			private boolean endOfContainers;

			private int startAt;

			private StorageContainerListCriteria topLevelCrit;

			private StorageContainerListCriteria descendantsCrit;

			private List<StorageContainerDetail> topLevelContainers = new ArrayList<>();

			private Set<String> types = null;

			@Override
			public List<StorageContainerDetail> apply(ExportJob job) {
				while (true) {
					initParams();

					if (endOfContainers) {
						return Collections.emptyList();
					}

					if (topLevelContainers.isEmpty()) {
						if (topLevelCrit == null) {
							topLevelCrit = new StorageContainerListCriteria().topLevelContainers(true).ids(job.getRecordIds());
							Map<String, String> params = job.getParams();
							if (params != null && !params.isEmpty()) {
								if (StringUtils.isNotBlank(params.get("names"))) {
									topLevelCrit.names(Arrays.asList(params.get("names").split("\\^")));
								}

								if (StringUtils.isNotBlank(params.get("cps"))) {
									topLevelCrit.cpShortTitles(params.get("cps").split("\\^"));
								}

								if (StringUtils.isNotBlank(params.get("types"))) {
									types = Arrays.stream(params.get("types").split("\\^")).collect(Collectors.toSet());
								}
							}

							addContainerListCriteria(topLevelCrit);
						}

						if (loadTopLevelContainers) {
							topLevelContainers = getContainers(topLevelCrit.startAt(startAt).maxResults(1));
							startAt += topLevelContainers.size();
							loadTopLevelContainers = CollectionUtils.isEmpty(job.getRecordIds());
						}
					}

					if (topLevelContainers.isEmpty()) {
						endOfContainers = true;
						return Collections.emptyList();
					}

					if (descendantsCrit == null) {
						descendantsCrit = new StorageContainerListCriteria()
							.cpShortTitles(topLevelCrit.cpShortTitles())
							.siteCps(topLevelCrit.siteCps()).maxResults(100000);
					}

					StorageContainerDetail topLevelContainer = topLevelContainers.remove(0);
					descendantsCrit.parentContainerId(topLevelContainer.getId());
					List<StorageContainer> descendants = daoFactory.getStorageContainerDao().getDescendantContainers(descendantsCrit);

					Map<Long, List<StorageContainer>> childContainersMap = new HashMap<>();
					for (StorageContainer container : descendants) {
						Long parentId = container.getParentContainer() == null ? null : container.getParentContainer().getId();
						List<StorageContainer> childContainers = childContainersMap.computeIfAbsent(parentId, (k) -> new ArrayList<>());
						childContainers.add(container);
					}

					List<StorageContainerDetail> workList = new ArrayList<>();
					workList.addAll(toDetailList(childContainersMap.get(null)));

					List<StorageContainerDetail> result = new ArrayList<>();
					while (!workList.isEmpty()) {
						StorageContainerDetail containerDetail = workList.remove(0);
						if (types == null || types.isEmpty() || types.contains(containerDetail.getTypeName())) {
							result.add(containerDetail);
						}

						List<StorageContainer> childContainers = childContainersMap.get(containerDetail.getId());
						if (childContainers != null) {
							workList.addAll(0, toDetailList(childContainers));
						}
					}

					if (!result.isEmpty()) {
						return result;
					}

					// continue until all the containers are exhausted
				}
			}

			private void initParams() {
				if (paramsInited) {
					return;
				}

				endOfContainers = !AccessCtrlMgr.getInstance().hasStorageContainerEximRights();
				paramsInited = true;
			}

			private List<StorageContainerDetail> getContainers(StorageContainerListCriteria crit) {
				return toDetailList(daoFactory.getStorageContainerDao().getStorageContainers(crit));
			}

			private List<StorageContainerDetail> toDetailList(List<StorageContainer> containers) {
				DeObject.createExtensions(true, StorageContainer.EXTN, -1L, containers);
				return containers.stream()
					.sorted((c1, c2) -> {
						if (hasPosition(c1) && hasPosition(c2)) {
							return c1.getPosition().getPosition().compareTo(c2.getPosition().getPosition());
						} else if (hasPosition(c1)) {
							return -1;
						} else if (hasPosition(c2)) {
							return 1;
						} else {
							return c1.getId().compareTo(c2.getId());
						}
					})
					.map(c -> StorageContainerDetail.from(c, false)).collect(Collectors.toList());
			}

			private boolean hasPosition(StorageContainer c) {
				return c.getPosition() != null && c.getPosition().getPosition() != null;
			}
		};
	}

	private Pair<StorageContainer, Collection<Specimen>> getMissingSpecimens(BoxDetail input) {
		StorageContainer box = getContainer(input.getId(), input.getName(), input.getBarcode());
		AccessCtrlMgr.getInstance().ensureReadContainerRights(box);
		if (box.isDimensionless()) {
			return Pair.make(box, null);
		}

		List<Long> specimenIds = input.getPositions().stream().map(StorageContainerPositionDetail::getOccupyingEntityId).toList();
		List<Specimen> missingSpmns = daoFactory.getStorageContainerDao()
			.getMissingSpecimens(box.getId(), specimenIds)
			.stream().sorted()
			.map(StorageContainerPosition::getOccupyingSpecimen)
			.toList();
		return Pair.make(box, missingSpmns);
	}

	private AutoFreezerReportDetail generateStoreListsFailureReport(StorageContainer freezer, AutoFreezerReportDetail reportDetail) {
		CsvWriter csvWriter = null;
		int failedLists = 0, maxLists = 100;
		int failedToStore = 0, failedToRetrieve = 0;
		try {
			File file = new File(ConfigUtil.getInstance().getTempDir(), "auto-freezer-report-" + UUID.randomUUID() + ".csv");
			file.deleteOnExit();

			csvWriter = CsvFileWriter.createCsvFileWriter(file);
			csvWriter.writeNext(getAutoFreezerReportHeader());

			List<Long> storeListIds = reportDetail.getFailedStoreListIds();
			boolean retrieveByIds = reportDetail.reportForFailedOps();
			ContainerStoreListCriteria crit = new ContainerStoreListCriteria()
				.containerId(freezer.getId())
				.statuses(Collections.singletonList(ContainerStoreList.Status.FAILED))
				.fromDate(!retrieveByIds ? reportDetail.getFromDate() : null)
				.toDate(!retrieveByIds   ? reportDetail.getToDate() : null)
				.maxResults(maxLists);

			boolean endOfLists = false;
			while (!endOfLists) {
				if (retrieveByIds) {
					int toIdx = storeListIds.size() < maxLists ? storeListIds.size() : maxLists;
					crit.ids(storeListIds.subList(0, toIdx));
					storeListIds = storeListIds.subList(toIdx, storeListIds.size());
				} else {
					crit.startAt(failedLists);
				}

				List<ContainerStoreList> storeLists = daoFactory.getContainerStoreListDao().getStoreLists(crit);
				failedLists += storeLists.size();

				for (ContainerStoreList storeList : storeLists) {
					List<String[]> rows = getAutoFreezerReportRows(storeList);
					csvWriter.writeAll(rows);

					if (storeList.getOp() == ContainerStoreList.Op.PICK) {
						failedToRetrieve += rows.size();
					} else if (storeList.getOp() == ContainerStoreList.Op.PUT) {
						failedToStore += rows.size();
					}
				}

				if (retrieveByIds) {
					endOfLists = storeListIds.isEmpty();
				} else {
					endOfLists = storeLists.size() < maxLists;
				}
			}

			reportDetail.setFailedRetrieves(failedToRetrieve);
			reportDetail.setFailedStores(failedToStore);
			reportDetail.setFailedLists(failedLists);
			reportDetail.setReport(failedToRetrieve > 0 || failedToStore > 0 ? file : null);
			return reportDetail;
		} catch (Exception e) {
			logger.error("Error generating automated freezer failed ops report", e);
			throw new RuntimeException("Error generating automated freezer failed ops report", e);
		}  finally {
			IOUtils.closeQuietly(csvWriter);
		}
	}

	private String[] getAutoFreezerReportHeader() {
		return new String[] {
			msg("auto_freezer_store_list_id"),
			msg("auto_freezer_name"),
			msg("auto_freezer_cp"),
			msg("auto_freezer_specimen_label"),
			msg("auto_freezer_specimen_barcode"),
			msg("auto_freezer_operation"),
			msg("auto_freezer_execution_time"),
			msg("auto_freezer_last_retried_time"),
			msg("auto_freezer_no_of_retries"),
			msg("auto_freezer_error_msg")
		};
	}

	private List<String[]> getAutoFreezerReportRows(ContainerStoreList storeList) {
		List<String[]> rows =  new ArrayList<>();

		String storeListId = storeList.getId().toString();
		String container = storeList.getContainer().getName();
		String operation = storeList.getOp().toString();
		String executionTime = Utility.getDateTimeString(storeList.getCreationTime());
		String lastRetriedTime = Utility.getDateTimeString(storeList.getExecutionTime());
		String noOfRetries = String.valueOf(storeList.getNoOfRetries());
		String storeListError = StringUtils.isBlank(storeList.getError()) ? "" : storeList.getError();

		for (ContainerStoreListItem item : storeList.getItems()) {
			Specimen spmn = item.getSpecimen();
			StringBuilder error = new StringBuilder(storeListError);

			if (StringUtils.isNotBlank(item.getError())) {
				if (StringUtils.isNotBlank(storeListError)) {
					error.append(System.lineSeparator());
				}

				error.append(item.getError());
			}

			rows.add(new String[] {
				storeListId,
				container,
				spmn.getCpShortTitle(),
				spmn.getLabel(),
				spmn.getBarcode(),
				operation,
				executionTime,
				lastRetriedTime,
				noOfRetries,
				error.toString()
			});
		}

		return rows;
	}

	@PlusTransactional
	private ResponseEvent<String> exportTransferEvents0(ContainerReportCriteria crit) {
		Date startTime = Calendar.getInstance().getTime();
		String dateTime = sdf.format(startTime);
		String fileId = "csv_" + UUID.randomUUID().toString() + "_" + AuthUtil.getCurrentUser().getId() + "_transfer_report_" + dateTime;
		File file     = new File(ConfigUtil.getInstance().getReportsDir(), fileId + ".csv");

		Throwable t = null;
		CsvWriter writer = null;
		try {
			writer = CsvFileWriter.createCsvFileWriter(file);
			writer.writeNext(new String[] { msg("common_exported_by"), AuthUtil.getCurrentUser().formattedName()});
			writer.writeNext(new String[] { msg("common_exported_on"), Utility.getDateTimeString(Calendar.getInstance().getTime())});

			writer.writeNext(new String[0]);
			writer.writeNext(new String[] {
				msg("container_name"), msg("container_barcode"), msg("container_display_name"),
				msg("freezer_name"), msg("freezer_barcode"), msg("freezer_display_name"),
				msg("container_type"), msg("storage_container_protocols"),
				msg("transfer_event_id"), msg("transfer_event_user"), msg("transfer_event_time"), msg("common_reason"),
				msg("transfer_event_from_site"), msg("transfer_event_from_container"), msg("transfer_event_from_row"), msg("transfer_event_from_col"), msg("transfer_event_from_position"),
				msg("transfer_event_to_site"), msg("transfer_event_to_container"), msg("transfer_event_to_row"), msg("transfer_event_to_col"), msg("transfer_event_to_position")
			});


			Long lastId = null;
			int numRows = 0;
			boolean endOfEvents = false;
			while (!endOfEvents) {
				crit.lastId(lastId).startAt(0).maxResults(1000);

				List<ContainerTransferEventDetail> events = daoFactory.getStorageContainerDao().getTransferEvents(crit);
				endOfEvents = events.size() < crit.maxResults();

				for (ContainerTransferEventDetail event : events) {
					String name = event.getContainerName();
					String barcode = event.getContainerBarcode();
					String displayName = event.getContainerDisplayName();

					String freezer = event.getFreezerName();
					String freezerBarcode = event.getFreezerBarcode();
					String freezerDisplayName = event.getFreezerDisplayName();

					StorageLocationSummary fromLoc = event.getFromLocation();
					String fromContainer = null, fromRow = null, fromCol = null, fromPos = null;
					if (fromLoc != null) {
						fromContainer = fromLoc.getName();
						if (StringUtils.isNotBlank(fromLoc.getDisplayName())) {
							fromContainer = fromLoc.getDisplayName() + " (" + fromContainer + ")";
						}
						fromRow = fromLoc.getPositionY();
						fromCol = fromLoc.getPositionX();
						fromPos = fromLoc.getPosition() != null ? fromLoc.getPosition().toString() : null;
					}

					StorageLocationSummary toLoc = event.getToLocation();
					String toContainer = null, toRow = null, toCol = null, toPos = null;
					if (toLoc != null) {
						toContainer = toLoc.getName();
						if (StringUtils.isNotBlank(toLoc.getDisplayName())) {
							fromContainer = toLoc.getDisplayName() + " (" + toContainer + ")";
						}
						toRow = toLoc.getPositionY();
						toCol = toLoc.getPositionX();
						toPos = toLoc.getPosition() != null ? toLoc.getPosition().toString() : null;
					}


					++numRows;
					writer.writeNext(new String[]{
						name, barcode, displayName, freezer, freezerBarcode, freezerDisplayName, event.getTypeName(),
						Utility.join(event.getCps(), cp -> cp, ", "),
						event.getId().toString(), event.getUser().formattedName(), Utility.getDateTimeString(event.getTime()), event.getReason(),
						event.getFromSite(), fromContainer, fromRow, fromCol, fromPos,
						event.getToSite(), toContainer, toRow, toCol, toPos
					});

					lastId = event.getId();

					if (numRows % 25 == 0) {
						writer.flush();
					}
				}
			}

			writer.flush();
			exportSvc.saveJob("transfer_report", startTime, crit.toStrMap());
			return ResponseEvent.response(fileId);
		} catch (Throwable e) {
			t = e;
			return ResponseEvent.serverError(new Exception(t));
		} finally {
			IOUtils.closeQuietly(writer);
			sendTransferReportEmail(fileId, t);
		}
	}

	private void sendAutoFreezerReport(StorageContainer freezer, AutoFreezerReportDetail reportDetail) {
		String date = Utility.getDateString(Calendar.getInstance().getTime());

		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("$subject", new String[] {date});
		emailProps.put("date", date);
		emailProps.put("ccAdmin", false);
		emailProps.putAll(reportDetail.toMap());

		if (!reportDetail.hasAnyActivity()) {
			logger.info("Not sending automated freezers report email, as there was no activity seen on: " + date);
			return;
		}

		List<User> rcpts = new ArrayList<>(freezer.getSite().getCoordinators());
		String itAdminEmailId = ConfigUtil.getInstance().getItAdminEmailId();
		if (StringUtils.isNotBlank(itAdminEmailId)) {
			User itAdmin = new User();
			itAdmin.setFirstName("IT");
			itAdmin.setLastName("Admin");
			itAdmin.setEmailAddress(itAdminEmailId);
			rcpts.add(itAdmin);
		}

		String emailTmpl = reportDetail.reportForFailedOps() ? AUTO_FREEZER_FAILED_OPS_RPT : AUTO_FREEZER_DAILY_RPT;
		File[] attachments = reportDetail.getReport() != null ? new File[] { reportDetail.getReport() } : null;
		for (User user : rcpts) {
			emailProps.put("rcpt", user);
			EmailUtil.getInstance().sendEmail(emailTmpl, new String[] {user.getEmailAddress()}, attachments, emailProps);
		}
	}

	private void sendReportEmail(String report, User user, StorageContainer container, String fileId, Throwable exception) {
		String error = null;
		if (exception != null) {
			Throwable rootCause = ExceptionUtils.getRootCause(exception);
			if (rootCause == null) {
				rootCause = exception;
			}
			error = ExceptionUtils.getStackTrace(rootCause);
		}

		report = MessageUtil.getInstance().getMessage(report);

		String displayTitle = null;
		if (container != null) {
			displayTitle = container.getName();
			if (StringUtils.isNotBlank(container.getDisplayName())) {
				displayTitle = container.getDisplayName() + " (" + displayTitle + ")";
			}
		}

		Map<String, Object> props = new HashMap<>();
		props.put("$subject", new String[] { report.toLowerCase(), displayTitle != null ? ": " + displayTitle : "" });
		props.put("report", report.toLowerCase());
		props.put("fileId", fileId);
		props.put("error", error);
		props.put("rcpt", user);
		props.put("container", container);
		props.put("displayTitle", displayTitle);
		EmailUtil.getInstance().sendEmail(RPT_EMAIL_TMPL, new String[] { user.getEmailAddress() }, null, props);
	}

	private void sendTransferReportEmail(String fileId, Throwable t) {
		String error = null;
		if (t != null) {
			Throwable rootCause = ExceptionUtils.getRootCause(t);
			if (rootCause == null) {
				rootCause = t;
			}
			error = ExceptionUtils.getStackTrace(rootCause);
		}

		User rcpt = AuthUtil.getCurrentUser();
		Map<String, Object> props = new HashMap<>();
		props.put("fileId", fileId);
		props.put("error", error);
		props.put("rcpt", rcpt);
		EmailUtil.getInstance().sendEmail(TRANSFER_RPT_TMPL, new String[] { rcpt.getEmailAddress() }, null, props);
	}

	private void sendBulkUpdateNotif(User user, int count, Throwable exception) {
		String error = null;
		if (exception != null) {
			Throwable rootCause = ExceptionUtils.getRootCause(exception);
			if (rootCause == null) {
				rootCause = exception;
			}
			error = ExceptionUtils.getStackTrace(rootCause);
		}

		Map<String, Object> props = new HashMap<>();
		props.put("$subject", new String[0]);
		props.put("count", count);
		props.put("error", error);
		props.put("rcpt", user);
		EmailUtil.getInstance().sendEmail(BULK_UPDATED_TMPL, new String[] { user.getEmailAddress() }, null, props);
	}

	private String msg(String code) {
		return MessageUtil.getInstance().getMessage(code);
	}

	private final static String AUTO_FREEZER_DAILY_RPT      = "auto_freezer_daily";

	private final static String AUTO_FREEZER_FAILED_OPS_RPT = "auto_freezer_failed_ops";

	private final static String RPT_EMAIL_TMPL              = "container_report";

	private final static String BULK_UPDATED_TMPL           = "containers_bulk_updated";

	private final static String TRANSFER_RPT_TMPL           = "container_transfer_report";
}
