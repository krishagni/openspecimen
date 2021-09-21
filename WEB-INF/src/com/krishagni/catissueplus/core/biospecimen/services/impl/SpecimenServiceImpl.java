
package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.audit.services.impl.DeleteLogUtil;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.CpSpecimenLabelPrintSetting;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenPreSaveEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpEntityDeleteCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.PrintSpecimenLabelDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ReceivedEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenAliquotsSpec;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenStatusDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenResolver;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.domain.PrintItem;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkEntityDetail;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.LabelPrintJobSummary;
import com.krishagni.catissueplus.core.common.events.LabelTokenDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.service.ConfigChangeListener;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.service.LabelPrinter;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.NumUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class SpecimenServiceImpl implements SpecimenService, ObjectAccessor, ConfigChangeListener, InitializingBean {

	private static Log logger = LogFactory.getLog(SpecimenServiceImpl.class);

	private DaoFactory daoFactory;

	private SpecimenFactory specimenFactory;

	private SpecimenResolver specimenResolver;
	
	private ConfigurationService cfgSvc;

	private LabelGenerator labelGenerator;

	private LabelGenerator specimenBarcodeGenerator;

	private ExportService exportSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setSpecimenFactory(SpecimenFactory specimenFactory) {
		this.specimenFactory = specimenFactory;
	}

	public void setSpecimenResolver(SpecimenResolver specimenResolver) {
		this.specimenResolver = specimenResolver;
	}

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setLabelGenerator(LabelGenerator labelGenerator) {
		this.labelGenerator = labelGenerator;
	}

	public void setSpecimenBarcodeGenerator(LabelGenerator specimenBarcodeGenerator) {
		this.specimenBarcodeGenerator = specimenBarcodeGenerator;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenDetail> getSpecimen(RequestEvent<SpecimenQueryCriteria> req) {
		try {
			SpecimenQueryCriteria crit = req.getPayload();

			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			Specimen specimen = getSpecimen(crit.getId(), crit.getCpShortTitle(), crit.getName(), crit.getBarcode(), ose);
			if (specimen == null) {
				return ResponseEvent.error(ose);
			}

			AccessCtrlMgr.SpecimenAccessRights rights = AccessCtrlMgr.getInstance().ensureReadSpecimenRights(specimen);
			SpecimenDetail detail = SpecimenDetail.from(specimen, false, !rights.phiAccess, rights.onlyPrimarySpmns || !crit.isIncludeChildren());
			return ResponseEvent.response(detail);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<? extends SpecimenInfo>> getSpecimens(RequestEvent<SpecimenListCriteria> req) {
		try {
			SpecimenListCriteria crit = req.getPayload();
			List<Specimen> specimens = getSpecimens(crit);
			if (CollectionUtils.isNotEmpty(crit.labels())) {
				specimens = Specimen.sortByLabels(specimens, crit.labels());
			} else if (CollectionUtils.isNotEmpty(crit.barcodes())) {
				specimens = Specimen.sortByBarcodes(specimens, crit.barcodes());
			} else if (CollectionUtils.isNotEmpty(crit.ids())) {
				specimens = Specimen.sortByIds(specimens, crit.ids());
			}

			List<? extends SpecimenInfo> result = null;
			if (crit.includeExtensions()) {
				createExtensions(crit.cpId(), specimens);
				result = specimens.stream().map(s -> SpecimenDetail.from(s, false, true, true)).collect(Collectors.toList());
			} else if (crit.minimalInfo()) {
				result = specimens.stream().map(s -> toMinimalInfo(s)).collect(Collectors.toList());
			} else {
				result = SpecimenInfo.from(specimens);
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
	public ResponseEvent<List<? extends SpecimenInfo>> getSpecimensById(List<Long> ids, boolean includeExtensions) {
		return getSpecimensById(ids, includeExtensions, false);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<? extends SpecimenInfo>> getSpecimensById(List<Long> ids, boolean includeExtensions, boolean minimalInfo) {
		try {
			List<Specimen> specimens = getSpecimensById(ids);
			specimens = Specimen.sortByIds(specimens, ids);

			List<? extends SpecimenInfo> result = null;
			if (includeExtensions) {
				createExtensions(null, specimens);
				result = specimens.stream().map(s -> SpecimenDetail.from(s, false, true, true)).collect(Collectors.toList());
			} else if (minimalInfo) {
				result = specimens.stream().map(s -> toMinimalInfo(s)).collect(Collectors.toList());
			} else {
				result = SpecimenInfo.from(specimens);
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
	public ResponseEvent<List<SpecimenInfo>> getPrimarySpecimensByCp(RequestEvent<Long> req) {
		try {
			Long cpId = req.getPayload();
			if (cpId == null) {
				return ResponseEvent.response(Collections.emptyList());
			}

			SpecimenListCriteria crit = new SpecimenListCriteria()
				.cpId(cpId)
				.lineages(new String[] {Specimen.NEW})
				.collectionStatuses(new String[] {Specimen.COLLECTED})
				.limitItems(true);
			return ResponseEvent.response(SpecimenInfo.from(getSpecimens(crit)));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenDetail> createSpecimen(RequestEvent<SpecimenDetail> req) {
		try {
			SpecimenDetail detail = req.getPayload();
			Specimen specimen = saveOrUpdate(detail, null, null, null);
			getLabelPrinter().print(getSpecimenPrintItems(Collections.singleton(specimen)));
			return ResponseEvent.response(SpecimenDetail.from(specimen, false, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception ex) {
			return ResponseEvent.serverError(ex);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenDetail> updateSpecimen(RequestEvent<SpecimenDetail> req) {
		try {
			SpecimenDetail detail = req.getPayload();
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			Specimen existing = updateSpecimen(detail, ose);
			ose.checkAndThrow();
			return ResponseEvent.response(SpecimenDetail.from(existing, false, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenInfo>> updateSpecimens(RequestEvent<List<SpecimenDetail>> req) {
		try {
			List<Specimen> savedSpmns = new ArrayList<>();
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			req.getPayload().forEach(spmn -> savedSpmns.add(updateSpecimen(spmn, ose)));
			ose.checkAndThrow();
			return ResponseEvent.response(SpecimenDetail.from(savedSpmns));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenInfo>> bulkUpdateSpecimens(RequestEvent<BulkEntityDetail<SpecimenDetail>> req) {
		try {
			BulkEntityDetail<SpecimenDetail> buDetail = req.getPayload();
			SpecimenDetail spmn = buDetail.getDetail();

			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			List<Specimen> savedSpmns = new ArrayList<>();
			for (Long id : buDetail.getIds()) {
				spmn.setId(id);
				savedSpmns.add(updateSpecimen(spmn, ose));
			}

			ose.checkAndThrow();
			return ResponseEvent.response(SpecimenDetail.from(savedSpmns));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenDetail>> updateSpecimensStatus(RequestEvent<List<SpecimenStatusDetail>> req) {
		try {
			List<SpecimenDetail> result = new ArrayList<>();

			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			for (SpecimenStatusDetail detail : req.getPayload()) {
				Specimen specimen = getSpecimen(detail.getId(), detail.getCpShortTitle(), detail.getName(), detail.getBarcode(), ose);
				User user = getUser(detail.getUser(), ose);
				Date date = detail.getDate() != null ? detail.getDate() : Calendar.getInstance().getTime();
				ose.checkAndThrow();

				AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(specimen, false);
				specimen.updateStatus(detail.getStatus(), user, date, detail.getReason(), detail.isForceUpdate());

				if (specimen.isDeleted()) {
					specimen.setOpComments(detail.getReason());
					DeleteLogUtil.getInstance().log(specimen);
				}

				result.add(SpecimenDetail.from(specimen));
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
	public ResponseEvent<List<SpecimenInfo>> deleteSpecimens(RequestEvent<List<CpEntityDeleteCriteria>> request) {
		List<SpecimenInfo> result = new ArrayList<>();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		for (CpEntityDeleteCriteria criteria : request.getPayload()) {
			Specimen specimen = getSpecimen(criteria.getId(), criteria.getCpShortTitle(), criteria.getName(), ose);
			if (specimen == null) {
				continue;
			}

			AccessCtrlMgr.getInstance().ensureDeleteSpecimenRights(specimen);

			specimen.setOpComments(criteria.getReason());
			specimen.disable(!criteria.isForceDelete());

			DeleteLogUtil.getInstance().log(specimen);
			result.add(SpecimenInfo.from(specimen));
		}

		return ResponseEvent.response(result);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<SpecimenQueryCriteria> req) {
		try {
			SpecimenQueryCriteria crit = req.getPayload();
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			Specimen specimen = getSpecimen(crit.getId(), crit.getCpShortTitle(), crit.getName(), crit.getBarcode(), ose);
			if (specimen == null) {
				return ResponseEvent.error(ose);
			}
			
			AccessCtrlMgr.getInstance().ensureReadSpecimenRights(specimen, false);
			return ResponseEvent.response(specimen.getDependentEntities());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
			
	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenDetail>> collectSpecimens(RequestEvent<List<SpecimenDetail>> req) {
		try {
			Collection<Specimen> specimens = new ArrayList<>();
			for (SpecimenDetail detail : req.getPayload()) {
				Specimen specimen = collectSpecimen(detail, null, new HashMap<>());
				specimens.add(specimen);
			}

			getLabelPrinter().print(getSpecimenPrintItems(specimens));
			return ResponseEvent.response(
				specimens.stream()
					.map(spmn -> SpecimenDetail.from(spmn, false, false))
					.collect(Collectors.toList())
			);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenDetail>> createAliquots(RequestEvent<SpecimenAliquotsSpec> req) {
		try {
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			
			SpecimenAliquotsSpec spec = req.getPayload();
			Specimen parentSpmn = getSpecimen(spec.getParentId(), spec.getCpShortTitle(), spec.getParentLabel(), ose);
			ose.checkAndThrow();
			
			if (!parentSpmn.isCollected()) {
				return ResponseEvent.userError(SpecimenErrorCode.NOT_COLLECTED, parentSpmn.getLabel());
			}

			SpecimenDetail derived = null;
			if ((StringUtils.isNotBlank(spec.getSpecimenClass()) && !spec.getSpecimenClass().equals(parentSpmn.getSpecimenClass().getValue())) ||
				(StringUtils.isNotBlank(spec.getType()) && !spec.getType().equals(parentSpmn.getSpecimenType().getValue())) ||
				(spec.createDerived() && !parentSpmn.isDerivative())) {
				derived = getDerivedSpecimen(parentSpmn, spec);
			}

			Integer count = spec.getNoOfAliquots();
			BigDecimal aliquotQty = spec.getQtyPerAliquot();
			if ((count != null && count <= 0) || NumUtil.lessThanEqualsZero(aliquotQty)) {
				return ResponseEvent.userError(SpecimenErrorCode.INVALID_QTY_OR_CNT);
			}

			List<String> labels = Utility.csvToStringList(spec.getLabels());
			if (!labels.isEmpty()) {
				count = labels.size();
			}

			List<String> barcodes = Utility.csvToStringList(spec.getBarcodes());
			if (count == null && !barcodes.isEmpty()) {
				count = barcodes.size();
			}

			BigDecimal parentQty = derived != null ? derived.getInitialQty() : parentSpmn.getAvailableQuantity();
			boolean aliquotQtyReq = ConfigUtil.getInstance().getBoolSetting(ConfigParams.MODULE, ConfigParams.ALIQUOT_QTY_REQ, true);
			if ((count == null && (parentQty == null || aliquotQty == null)) ||
				(parentQty == null && aliquotQty == null && aliquotQtyReq)) {
				return ResponseEvent.userError(SpecimenErrorCode.ALIQUOT_CNT_N_QTY_REQ);
			}

			if (count == null) {
				count = parentQty.divide(aliquotQty, RoundingMode.FLOOR).intValue();
			} else if (aliquotQty == null && parentQty != null) {
				aliquotQty = parentQty.divide(new BigDecimal(count), RoundingMode.FLOOR);
			}

			List<Long> reqIds = new ArrayList<>();
			if (spec.isLinkToReqs() && parentSpmn.getSpecimenRequirement() != null) {
				reqIds = parentSpmn.getSpecimenRequirement().getOrderedChildRequirements().stream()
					.filter(SpecimenRequirement::isAliquot)
					.map(SpecimenRequirement::getId).collect(Collectors.toList());

				Set<Long> collectedReqs = parentSpmn.getChildCollection().stream()
					.filter(c -> c.getSpecimenRequirement() != null && c.isAliquot())
					.map(c -> c.getSpecimenRequirement().getId())
					.collect(Collectors.toSet());
				reqIds.removeAll(collectedReqs);
			}

			List<StorageLocationSummary> locations = spec.getLocations();
			List<SpecimenDetail> aliquots = new ArrayList<>();
			for (int i = 0; i < count; ++i) {
				SpecimenDetail aliquot = new SpecimenDetail();
				aliquot.setLineage(Specimen.ALIQUOT);
				aliquot.setInitialQty(aliquotQty);
				aliquot.setAvailableQty(aliquotQty);
				aliquot.setConcentration(spec.getConcentration());
				aliquot.setParentLabel(derived == null ? parentSpmn.getLabel() : null);
				aliquot.setParentId(derived == null ? parentSpmn.getId() : null);
				aliquot.setCreatedOn(spec.getCreatedOn());
				aliquot.setCreatedBy(spec.getCreatedBy());
				aliquot.setFreezeThawCycles(spec.getFreezeThawCycles());
				aliquot.setIncrParentFreezeThaw(spec.getIncrParentFreezeThaw());
				aliquot.setCloseParent(spec.closeParent());
				aliquot.setPrintLabel(spec.printLabel());
				aliquot.setComments(spec.getComments());
				aliquot.setExtensionDetail(spec.getExtensionDetail());

				if (i < reqIds.size()) {
					aliquot.setReqId(reqIds.get(i));
				}

				if (i < labels.size()) {
					aliquot.setLabel(labels.get(i));
				}

				if (i < barcodes.size()) {
					aliquot.setBarcode(barcodes.get(i));
				}

				if (StringUtils.isNotBlank(spec.getParentContainerName())) {
					StorageLocationSummary containerLocation = new StorageLocationSummary();
					containerLocation.setName(spec.getParentContainerName());
					aliquot.setContainerLocation(containerLocation);
				}

				if (StringUtils.isNotBlank(spec.getContainerType())) {
					aliquot.setContainerTypeName(spec.getContainerType());
				}
				
				StorageLocationSummary location = null;
				if (StringUtils.isNotBlank(spec.getContainerName())) {
					location = new StorageLocationSummary();
					location.setName(spec.getContainerName());
					if (i == 0) {
						if (spec.getPosition() != null && spec.getPosition() != 0) {
							location.setPosition(spec.getPosition());
						}

						if (spec.getPositionX() != null && spec.getPositionY() != null) {
							location.setPositionX(spec.getPositionX());
							location.setPositionY(spec.getPositionY());
						}
					}
				} else if (locations != null && i < locations.size()) {
					location = locations.get(i);
				}

				aliquot.setStorageLocation(location);
				aliquots.add(aliquot);
			}

			List<SpecimenDetail> inputSpmns = aliquots;
			if (derived != null) {
				derived.setChildren(aliquots);
				inputSpmns = Collections.singletonList(derived);
			}

			ResponseEvent<List<SpecimenDetail>> resp = collectSpecimens(new RequestEvent<>(inputSpmns));
			if (resp.isSuccessful() && spec.closeParent()) {
				parentSpmn.close(AuthUtil.getCurrentUser(), new Date(), "");
			}

			return resp;
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenDetail> createDerivative(RequestEvent<SpecimenDetail> derivedReq) {
		try {
			SpecimenDetail spmnDetail = derivedReq.getPayload();
			spmnDetail.setLineage(Specimen.DERIVED);
			spmnDetail.setStatus(Specimen.COLLECTED);

			ResponseEvent<SpecimenDetail> resp = createSpecimen(new RequestEvent<SpecimenDetail>(spmnDetail));
			if (resp.isSuccessful() && spmnDetail.closeParent()) {
				Specimen parent = getSpecimen(spmnDetail.getParentId(), spmnDetail.getCpShortTitle(), spmnDetail.getParentLabel(), null);
				parent.close(AuthUtil.getCurrentUser(), new Date(), "");
			}

			return resp;
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> doesSpecimenExists(RequestEvent<SpecimenQueryCriteria> req) {
		SpecimenQueryCriteria crit = req.getPayload();
		return ResponseEvent.response(getSpecimen(crit.getCpShortTitle(), crit.getName()) != null);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<LabelPrintJobSummary> printSpecimenLabels(RequestEvent<PrintSpecimenLabelDetail> req) {
		PrintSpecimenLabelDetail printDetail = req.getPayload();
		
		LabelPrinter<Specimen> printer = getLabelPrinter();
		if (printer == null) {
			return ResponseEvent.serverError(SpecimenErrorCode.NO_PRINTER_CONFIGURED);
		}
				
		List<Specimen> specimens = getSpecimensToPrint(printDetail);
		if (CollectionUtils.isEmpty(specimens)) {
			return ResponseEvent.userError(SpecimenErrorCode.NO_SPECIMENS_TO_PRINT);
		}

		LabelPrintJob job = printer.print(PrintItem.make(specimens, printDetail.getNumCopies()));
		if (job == null) {
			return ResponseEvent.userError(SpecimenErrorCode.PRINT_ERROR);
		}

		job.generateLabelsDataFile();
		return ResponseEvent.response(LabelPrintJobSummary.from(job));
	}

	@Override
	public ResponseEvent<List<LabelTokenDetail>> getPrintLabelTokens() {
		return ResponseEvent.response(LabelTokenDetail.from("print_", getLabelPrinter().getTokens()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public LabelPrinter<Specimen> getLabelPrinter() {
		String labelPrinterBean = cfgSvc.getStrSetting(
				ConfigParams.MODULE,
				ConfigParams.SPECIMEN_LABEL_PRINTER,
				"defaultSpecimenLabelPrinter");
		
		return (LabelPrinter<Specimen>)OpenSpecimenAppCtxProvider.getAppCtx().getBean(labelPrinterBean);
	}
		
	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> getCprAndVisitIds(RequestEvent<Long> req) {
		try {
			Map<String, Object> ids = resolveUrl("id", req.getPayload());
			if (ids == null || ids.isEmpty()) {
				return ResponseEvent.userError(SpecimenErrorCode.NOT_FOUND, req.getPayload());
			}
			
			return ResponseEvent.response(ids);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public Long getPrimarySpecimen(SpecimenQueryCriteria crit) {
		Long primarySpecimenId = daoFactory.getSpecimenDao().getPrimarySpecimen(crit.getId());
		if (primarySpecimenId == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, crit.getId());
		}

		return primarySpecimenId;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenDetail> undeleteSpecimen(RequestEvent<SpecimenQueryCriteria> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();

			SpecimenQueryCriteria crit = req.getPayload();
			Long specimenId = crit != null ? crit.getId() : null;
			if (specimenId == null) {
				return ResponseEvent.userError(SpecimenErrorCode.ID_REQUIRED);
			}

			Map<String, Object> info = daoFactory.getSpecimenDao().getDeletedSpecimenInfo(specimenId);
			if (info == null) {
				return ResponseEvent.userError(SpecimenErrorCode.DEL_NOT_FOUND, specimenId);
			}

			//
			// first activate the specimen so that it is visible to Hibernate
			//
			int activated = daoFactory.getSpecimenDao().activateSpecimen(specimenId, crit.isIncludeChildren());
			if (activated <= 0) {
				return ResponseEvent.response(null);
			}

			//
			// Do the rest of manipulation using Hibernate attached object so that
			// changes are recorded in the audit logs
			//
			Specimen spmn = daoFactory.getSpecimenDao().getById(specimenId);
			spmn.undelete(crit.isIncludeChildren());
			return ResponseEvent.response(SpecimenDetail.from(spmn, false, true));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public List<Specimen> getSpecimensByLabel(List<String> labels) {
		if (CollectionUtils.isEmpty(labels)) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_REQUEST);
		}

		return getSpecimens(new SpecimenListCriteria().labels(labels));
	}

	@Override
	@PlusTransactional
	public List<Specimen> getSpecimensById(List<Long> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_REQUEST);
		}

		return getSpecimens(new SpecimenListCriteria().ids(ids));
	}

	@Override
	@PlusTransactional
	public Specimen updateSpecimen(Specimen existing, Specimen newSpmn) {
		return saveOrUpdate(null, newSpmn, existing, null);
	}

	@Override
	public String getObjectName() {
		return Specimen.getEntityName();
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getSpecimenDao().getCprAndVisitIds(key, value);
	}

	@Override
	public String getAuditTable() {
		return "CATISSUE_SPECIMEN_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		AccessCtrlMgr.getInstance().ensureReadSpecimenRights(id);
	}

	@Override
	public void onConfigChange(String name, String value) {
		if (StringUtils.equals(name, ConfigParams.SPMN_BARCODE_FORMAT)) {
			if (StringUtils.isNotBlank(value) && !specimenBarcodeGenerator.isValidLabelTmpl(value)) {
				throw OpenSpecimenException.userError(CpErrorCode.INVALID_SPECIMEN_BARCODE_FMT, value);
			}
		} else if (StringUtils.equals(name, ConfigParams.UNIQUE_SPMN_LABEL_PER_CP)) {
			if (!StringUtils.equalsIgnoreCase(value, "true") && daoFactory.getSpecimenDao().areDuplicateLabelsPresent()) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.UQ_LBL_CP_CHG_NA);
			}
		} else if (StringUtils.equals(name, ConfigParams.UNIQUE_SPMN_BARCODE_PER_CP)) {
			if (!StringUtils.equalsIgnoreCase(value, "true") && daoFactory.getSpecimenDao().areDuplicateBarcodesPresent()) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.UQ_BC_CP_CHG_NA);
			}
		} else if (StringUtils.equals(name, ConfigParams.ALIQUOT_LABEL_FORMAT)) {
			if (StringUtils.isNotBlank(value) && !labelGenerator.isValidLabelTmpl(value)) {
				throw OpenSpecimenException.userError(CpErrorCode.INVALID_ALIQUOT_LABEL_FMT, value);
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		cfgSvc.registerChangeListener(ConfigParams.MODULE, this);
		exportSvc.registerObjectsGenerator("specimen", this::getSpecimensGenerator);
	}

	private List<Specimen> getSpecimens(SpecimenListCriteria crit) {
		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps(crit.cpId());
		if (siteCps != null && siteCps.isEmpty()) {
			return Collections.emptyList();
		}

		crit.siteCps(siteCps);
		crit.useMrnSites(AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn());
		return daoFactory.getSpecimenDao().getSpecimens(crit);
	}

	private Specimen updateSpecimen(SpecimenDetail detail, OpenSpecimenException ose) {
		Specimen existing = getSpecimen(detail.getId(), detail.getCpShortTitle(), detail.getLabel(), detail.getBarcode(), ose);
		if (existing == null) {
			return null;
		}

		AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(existing);
		saveOrUpdate(detail, null, existing, null);
		return existing;
	}

	private void ensureEditAllowed(SpecimenDetail detail, Specimen existing) {
		if (existing == null || existing.isEditAllowed()) {
			return;
		}

		if (!existing.isReserved()) {
			//
			// check whether the new status is active
			//
			String status = detail.getActivityStatus();
			if (StringUtils.isNotBlank(status) && !Status.isValidActivityStatus(status))  {
				throw OpenSpecimenException.userError(ActivityStatusErrorCode.INVALID);
			}

			if (StringUtils.isNotBlank(status) && Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(status)) {
				return;
			}

			if (existing.isStoredInDistributionContainer() &&
				detail.areTheOnlyModifiedAttrs("id", "cpShortTitle", "label", "barcode", "storageLocation", "transferComments")) {
				return;
			}
		}

		throw OpenSpecimenException.userError(SpecimenErrorCode.EDIT_NOT_ALLOWED, existing.getLabel());
	}

	private void ensureEditAllowed(Specimen newSpmn, Specimen existing) {
		if (existing == null || existing.isEditAllowed()) {
			return;
		}

		if (!existing.isReserved()) {
			if (newSpmn.isActive()) {
				return;
			}

			if (existing.isStoredInDistributionContainer()) {
				return;
			}
		}

		throw OpenSpecimenException.userError(SpecimenErrorCode.EDIT_NOT_ALLOWED, existing.getLabel());
	}

	private void ensureValidAndUniqueLabel(Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing != null && 
			StringUtils.isNotBlank(existing.getLabel()) &&
			existing.getLabel().equals(specimen.getLabel())) {
			return;
		}
		
		CollectionProtocol cp = specimen.getCollectionProtocol();
		String labelTmpl = specimen.getLabelTmpl();
		
		String label = specimen.getLabel();		
		if (StringUtils.isBlank(label)) {
			boolean labelReq = cp.isManualSpecLabelEnabled() || StringUtils.isBlank(labelTmpl);
			if (labelReq && specimen.isCollected()) {
				ose.addError(SpecimenErrorCode.LABEL_REQUIRED);
			}
			
			return;
		}

		if (StringUtils.isNotBlank(labelTmpl)) {
			if (!cp.isManualSpecLabelEnabled()) {
				ose.addError(SpecimenErrorCode.MANUAL_LABEL_NOT_ALLOWED);
				return;
			}
			
			if (!labelGenerator.validate(labelTmpl, specimen, label)) {
				ose.addError(SpecimenErrorCode.INVALID_LABEL, label);
				return;
			}
		}

		if (getSpecimen(cp.getShortTitle(), label) != null) {
			if (areLabelsUniquePerCp()) {
				ose.addError(SpecimenErrorCode.DUP_LABEL_IN_CP, label, cp.getShortTitle());
			} else {
				ose.addError(SpecimenErrorCode.DUP_LABEL, label);
			}
		}
	}

	private void ensureUniqueBarcode(Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (StringUtils.isBlank(specimen.getBarcode())) {
			return;
		}
		
		if (existing != null && specimen.getBarcode().equals(existing.getBarcode())) {
			return;
		}

		CollectionProtocol cp = specimen.getCollectionProtocol();
		if (StringUtils.isNotBlank(cp.getSpecimenBarcodeFormatToUse())) {
			ose.addError(SpecimenErrorCode.MANUAL_BARCODE_NOT_ALLOWED);
		}

		if (getSpecimenByBarcode(cp.getShortTitle(), specimen.getBarcode()) != null) {
			if (areBarcodesUniquePerCp()) {
				ose.addError(SpecimenErrorCode.DUP_BARCODE_IN_CP, specimen.getBarcode(), cp.getShortTitle());
			} else {
				ose.addError(SpecimenErrorCode.DUP_BARCODE, specimen.getBarcode());
			}
		}
	}

	private void ensureContainerAccess(Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing != null) {
			//
			// for existing specimens, access rights is checked in transfer event
			//
			return;
		}

		if (specimen.getPosition() != null) {
			AccessCtrlMgr.getInstance().ensureSpecimenStoreRights(specimen.getPosition().getContainer(), ose);
		}
	}

	private Specimen collectSpecimen(SpecimenDetail detail, Specimen parent, Map<Long, Specimen> reqSpmnsMap) {
		Specimen existing = null;

		if (detail.getId() == null && detail.getReqId() != null) {
			existing = initSpecimenIds(detail, reqSpmnsMap);
		} else if (detail.getId() != null) {
			existing = daoFactory.getSpecimenDao().getById(detail.getId());
			if (existing == null) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, detail.getId());
			}
		}

		Specimen specimen = existing;
		if (existing == null || !existing.isCollected()) {
			existing = collectPoolSpecimens(detail, existing, reqSpmnsMap);
			specimen = saveOrUpdate(detail, null, existing, parent);
			if (specimen.getPreCreatedSpmnsMap() != null) {
				reqSpmnsMap.putAll(specimen.getPreCreatedSpmnsMap());
			}
		} else {
			existing.setUid(detail.getUid());
			collectPoolSpecimens(detail, existing, reqSpmnsMap);
		}

		if (CollectionUtils.isNotEmpty(detail.getChildren())) {
			for (SpecimenDetail childDetail : detail.getChildren()) {
				if (childDetail.getCreatedOn() == null) {
					childDetail.setCreatedOn(specimen.getCreatedOn());
				}

				collectSpecimen(childDetail, specimen, reqSpmnsMap);
			}
		}

		if (BooleanUtils.isTrue(detail.getCloseAfterChildrenCreation())) {
			specimen.close(AuthUtil.getCurrentUser(), Calendar.getInstance().getTime(), "");
		}

		return specimen;
	}

	private Specimen collectPoolSpecimens(SpecimenDetail detail, Specimen existing, Map<Long, Specimen> reqSpmnsMap) {
		// If not pooled specimen then return existing specimen
		if (CollectionUtils.isEmpty(detail.getSpecimensPool())) {
			return existing;
		}

		Set<Specimen> specimensPool = new HashSet<>();
		for (SpecimenDetail poolSpmnDetail : detail.getSpecimensPool()) {
			specimensPool.add(collectSpecimen(poolSpmnDetail, null, reqSpmnsMap));
		}

		if (existing == null) {
			existing = specimensPool.iterator().next().getPooledSpecimen();
		}

		existing.getSpecimensPool().addAll(specimensPool);
		return existing;
	}

	private Specimen initSpecimenIds(SpecimenDetail detail, Map<Long, Specimen> reqSpmnsMap) {
		Specimen existing = reqSpmnsMap.get(detail.getReqId());
		if (existing == null) {
			return null;
		}

		detail.setId(existing.getId());
		if (StringUtils.isBlank(detail.getLabel()) && StringUtils.isNotBlank(existing.getLabel())) {
			detail.setLabel(existing.getLabel());
		}

		if (StringUtils.isBlank(detail.getBarcode()) && StringUtils.isNotBlank(existing.getBarcode())) {
			detail.setBarcode(existing.getBarcode());
		}

		return existing;
	}

	private Specimen saveOrUpdate(SpecimenDetail detail, Specimen specimen, Specimen existing, Specimen parent) {
		if (specimen == null) {
			ensureEditAllowed(detail, existing);

			specimen = specimenFactory.createSpecimen(existing, detail, parent);
		} else {
			ensureEditAllowed(specimen, existing);
		}

		AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(specimen);
		EventPublisher.getInstance().publish(new SpecimenPreSaveEvent(existing, specimen));

		String prevStatus = existing != null ? existing.getCollectionStatus() : null;
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureValidAndUniqueLabel(existing, specimen, ose);
		ensureUniqueBarcode(existing, specimen, ose);
		ensureContainerAccess(existing, specimen, ose);
		ose.checkAndThrow();

		//
		// NOTE OPSMN-3468: setLabelIfEmpty() is strategically placed to ensure it is called late but
		// before the specimen is associated to session to ensure specimen is not flushed
		// to database
		//

		if (existing != null) {
			existing.update(specimen);
			specimen = existing;
			specimen.setLabelIfEmpty();
		} else if (specimen.getParentSpecimen() != null) {
			if (!specimen.getParentSpecimen().isEditAllowed()) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.EDIT_NOT_ALLOWED, specimen.getParentSpecimen().getLabel());
			}

			specimen.setLabelIfEmpty();
			specimen.getParentSpecimen().addChildSpecimen(specimen);
		} else {
			specimen.checkPoolStatusConstraints();
			specimen.setLabelIfEmpty();
			specimen.occupyPosition();
		}

		specimen.setBarcodeIfEmpty();

		if (detail != null) {
			incrParentFreezeThawCycles(detail, specimen);
			specimen.setUid(detail.getUid());
			specimen.setParentUid(detail.getParentUid());
		}

		if (existing == null && specimen.isMissedOrNotCollected()) {
			specimen.updateHierarchyStatus();
		}

		specimen.updateAvailableStatus();
		daoFactory.getSpecimenDao().saveOrUpdate(specimen, true);
		specimen.addOrUpdateCollRecvEvents();
		specimen.addOrUpdateExtension();

		if (specimen.isDeleted()) {
			DeleteLogUtil.getInstance().log(specimen);
		}

		EventPublisher.getInstance().publish(new SpecimenSavedEvent(specimen));
		specimen.prePrintChildrenLabels(prevStatus, getLabelPrinter());
		return specimen;
	}

	/**
	 * Returns list of specimens based on input specification
	 * The input specification could be
	 * 1. List of specimen IDs or specimen labels
	 * 2. Flattened list of specimens of a visit identified by either visit name or visit ID
	 */
	private List<Specimen> getSpecimensToPrint(PrintSpecimenLabelDetail detail) {
		List<Specimen> specimens = null;
		if (CollectionUtils.isNotEmpty(detail.getSpecimenIds())) {
			specimens = daoFactory.getSpecimenDao().getSpecimensByIds(detail.getSpecimenIds());
			specimens = Specimen.sortByIds(specimens, detail.getSpecimenIds());
		} else if (CollectionUtils.isNotEmpty(detail.getSpecimenLabels())) {
			specimens = daoFactory.getSpecimenDao().getSpecimens(new SpecimenListCriteria().labels(detail.getSpecimenLabels()));
			specimens = Specimen.sortByLabels(specimens, detail.getSpecimenLabels());
		} else if (detail.getVisitId() != null) {
			Visit visit = daoFactory.getVisitsDao().getById(detail.getVisitId());
			if (visit == null) {
				throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND, detail.getVisitId());
			}

			specimens = getFlattenedSpecimens(visit.getTopLevelSpecimens());
		} else if (StringUtils.isNotBlank(detail.getVisitName())) {
			Visit visit = daoFactory.getVisitsDao().getByName(detail.getVisitName());
			if (visit == null) {
				throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND, detail.getVisitName());
			}

			specimens = getFlattenedSpecimens(visit.getTopLevelSpecimens());
		} else if (detail.getCprId() != null || (StringUtils.isNotBlank(detail.getCpShortTitle()) && StringUtils.isNotBlank(detail.getPpid()))) {
			CollectionProtocolRegistration cpr = null;
			Object key = null;
			if (detail.getCprId() != null) {
				key = detail.getCprId();
				cpr = daoFactory.getCprDao().getById(detail.getCprId());
			} else {
				key = detail.getCpShortTitle() + ":" + detail.getPpid();
				cpr = daoFactory.getCprDao().getCprByCpShortTitleAndPpid(detail.getCpShortTitle(), detail.getPpid());
			}

			if (cpr == null) {
				throw OpenSpecimenException.userError(CprErrorCode.M_NOT_FOUND, key, 1);
			}

			specimens = cpr.getVisits().stream()
				.map(v -> getFlattenedSpecimens(v.getTopLevelSpecimens()))
				.flatMap(List::stream)
				.collect(Collectors.toList());
		}
		
		return specimens;		
	}

	/**
	 * Filters input collection of specimens based on printLabel flag
	 */
	private List<PrintItem<Specimen>> getSpecimenPrintItems(Collection<Specimen> specimens) {
		List<PrintItem<Specimen>> printItems = new ArrayList<>();
		specimens.stream().sorted(Comparator.comparingLong(Specimen::getId)).forEach(
			(specimen) -> {
				if (specimen.isPrintLabel()) {
					printItems.add(PrintItem.make(specimen, getCopiesToPrint(specimen)));
				}

				if (CollectionUtils.isNotEmpty(specimen.getSpecimensPool())) {
					printItems.addAll(getSpecimenPrintItems(specimen.getSpecimensPool()));
				}

				if (CollectionUtils.isNotEmpty(specimen.getChildCollection())) {
					printItems.addAll(getSpecimenPrintItems(specimen.getChildCollection()));
				}
			}
		);

		return printItems;
	}

	private Integer getCopiesToPrint(Specimen spmn) {
		if (spmn.getSpecimenRequirement() != null) {
			return spmn.getSpecimenRequirement().getLabelPrintCopiesToUse();
		}

		CpSpecimenLabelPrintSetting setting = spmn.getCollectionProtocol().getSpmnLabelPrintSetting(spmn.getLineage());
		return setting != null ? setting.getCopies() : null;
	}

	private List<Specimen> getFlattenedSpecimens(Collection<Specimen> specimens) {
		List<Specimen> sortedSpecimens = Specimen.sort(specimens);

		List<Specimen> result = new ArrayList<>();
		for (Specimen specimen : sortedSpecimens) {
			result.add(specimen);
			result.addAll(getFlattenedSpecimens(specimen.getSpecimensPool()));
			result.addAll(getFlattenedSpecimens(specimen.getChildCollection()));
		}

		return result;
	}

	private Specimen getSpecimen(Long specimenId, String cpShortTitle, String label, String barcode, OpenSpecimenException ose) {
		return specimenResolver.getSpecimen(specimenId, cpShortTitle, label, barcode, ose);
	}

	private Specimen getSpecimen(Long specimenId, String cpShortTitle, String label, OpenSpecimenException ose) {
		return specimenResolver.getSpecimen(specimenId, cpShortTitle, label, ose);
	}

	private Specimen getSpecimen(String cpShortTitle, String label) {
		return specimenResolver.getSpecimen(cpShortTitle, label);
	}

	private Specimen getSpecimenByBarcode(String cpShortTitle, String barcode) {
		return specimenResolver.getSpecimenByBarcode(cpShortTitle, barcode);
	}

	private boolean areLabelsUniquePerCp() {
		return cfgSvc.getBoolSetting(ConfigParams.MODULE, ConfigParams.UNIQUE_SPMN_LABEL_PER_CP, false);
	}

	private boolean areBarcodesUniquePerCp() {
		return cfgSvc.getBoolSetting(ConfigParams.MODULE, ConfigParams.UNIQUE_SPMN_BARCODE_PER_CP, false);
	}

	private void incrParentFreezeThawCycles(SpecimenDetail detail, Specimen spec) {
		if (spec.getParentSpecimen() == null) {
			return;
		}

		spec.getParentSpecimen().incrementFreezeThaw(detail.getIncrParentFreezeThaw());
	}

	private User getUser(UserSummary detail, OpenSpecimenException ose) {
		Long userId = null;
		String emailAddress = null;
		if (detail != null) {
			userId = detail.getId();
			emailAddress = detail.getEmailAddress();
		}

		User user;
		if (userId != null) {
			user = daoFactory.getUserDao().getById(userId);
		} else if (StringUtils.isNotBlank(emailAddress)) {
			user = daoFactory.getUserDao().getUserByEmailAddress(emailAddress);
		} else {
			user = AuthUtil.getCurrentUser();
		}

		if (user == null) {
			ose.addError(UserErrorCode.NOT_FOUND);
		}

		return user;
	}

	private SpecimenDetail getDerivedSpecimen(Specimen parentSpecimen, SpecimenAliquotsSpec spec) {
		SpecimenDetail derived = new SpecimenDetail();
		derived.setLineage(Specimen.DERIVED);
		derived.setParentId(parentSpecimen.getId());
		derived.setCreatedOn(spec.getCreatedOn());
		derived.setCreatedBy(spec.getCreatedBy());
		derived.setSpecimenClass(spec.getSpecimenClass());
		derived.setType(spec.getType());
		derived.setStatus(Specimen.COLLECTED);
		derived.setIncrParentFreezeThaw(spec.getIncrParentFreezeThaw());
		derived.setCloseAfterChildrenCreation(true);

		Integer count = spec.getNoOfAliquots();
		BigDecimal qtyPerAliquot = spec.getQtyPerAliquot();
		derived.setInitialQty(count != null && qtyPerAliquot != null ? qtyPerAliquot.multiply(new BigDecimal(count)) : null);
		return derived;
	}

	private void createExtensions(Long cpId, List<Specimen> specimens) {
		if (cpId != null) {
			DeObject.createExtensions(true, Specimen.EXTN, cpId, specimens);
			return;
		}

		Map<Long, List<Specimen>> cpSpmnsMap = new HashMap<>();
		for (Specimen spmn : specimens) {
			List<Specimen> cpSpmns = cpSpmnsMap.computeIfAbsent(spmn.getCpId(), (k) -> new ArrayList<>());
			cpSpmns.add(spmn);
		}

		for (Map.Entry<Long, List<Specimen>> cpSpmns : cpSpmnsMap.entrySet()) {
			DeObject.createExtensions(true, Specimen.EXTN, cpSpmns.getKey(), cpSpmns.getValue());
		}
	}

	private SpecimenInfo toMinimalInfo(Specimen spmn) {
		SpecimenInfo info = new SpecimenInfo();
		info.setId(spmn.getId());
		info.setLabel(spmn.getLabel());
		info.setBarcode(spmn.getBarcode());
		info.setInitialQty(spmn.getInitialQuantity());
		info.setAvailableQty(spmn.getAvailableQuantity());
		info.setActivityStatus(spmn.getActivityStatus());
		return info;
	}

	private Function<ExportJob, List<? extends Object>> getSpecimensGenerator() {
		return new Function<ExportJob, List<? extends Object>>() {
			private boolean endOfSpecimens;

			private boolean paramsInited;

			private Long lastId;

			private SpecimenListCriteria crit;

			@Override
			public List<? extends Object> apply(ExportJob job) {
				initParams(job);

				if (endOfSpecimens) {
					return Collections.emptyList();
				}

				List<Specimen> specimens = daoFactory.getSpecimenDao().getSpecimens(crit.lastId(lastId));
				if (CollectionUtils.isNotEmpty(crit.labels()) || specimens.size() < 100) {
					endOfSpecimens = true;
				}

				List<SpecimenDetail> records = new ArrayList<>();
				for (Specimen specimen : specimens) {
					lastId = specimen.getId();

					try {
						AccessCtrlMgr.SpecimenAccessRights rights = AccessCtrlMgr.getInstance().ensureReadSpecimenRights(specimen, true);

						SpecimenDetail detail = SpecimenDetail.from(specimen, false, !rights.phiAccess, true);
						if (specimen.isPrimary()) {
							detail.setCollectionEvent(CollectionEventDetail.from(specimen.getCollectionEvent()));
							detail.setReceivedEvent(ReceivedEventDetail.from(specimen.getReceivedEvent()));
						}

						records.add(detail);
					} catch (OpenSpecimenException ose) {
						if (!ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
							logger.error("Encountered error exporting specimen record", ose);
						}
					}
				}

				return records;
			}

			private void initParams(ExportJob job) {
				if (paramsInited) {
					return;
				}

				Map<String, String> params = job.getParams();
				if (params == null) {
					params = Collections.emptyMap();
				}

				Long cpId = null;
				String cpIdStr = params.get("cpId");
				if (StringUtils.isNotBlank(cpIdStr)) {
					try {
						cpId = Long.parseLong(cpIdStr);
						if (cpId == -1L) {
							cpId = null;
						}
					} catch (Exception e) {
						logger.error("Invalid CP ID: " + cpIdStr, e);
					}
				}

				List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps(cpId, false);
				if (siteCps != null && siteCps.isEmpty()) {
					endOfSpecimens = true;
				} else if (!AccessCtrlMgr.getInstance().hasSpecimenEximRights(cpId)) {
					endOfSpecimens = true;
				} else {
					crit = new SpecimenListCriteria()
						.labels(Utility.csvToStringList(params.get("specimenLabels")))
						.siteCps(siteCps)
						.useMrnSites(AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn())
						.cpId(cpId);

					if (CollectionUtils.isNotEmpty(crit.labels())) {
						crit.limitItems(false);
					} else {
						crit.limitItems(true).maxResults(100);
					}
				}

				paramsInited = true;
			}
		};
	}
}
