package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.AliquotSpecimensRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.DerivedSpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenRequirementDetail;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;
import com.krishagni.catissueplus.core.importer.services.ObjectImporterLifecycle;

public class SpecimenRequirementImporter implements ObjectImporter<SpecimenRequirementDetail, SpecimenRequirementDetail>, ObjectImporterLifecycle {
	private CollectionProtocolService cpSvc;

	private Map<String, Map<String, Long>> contextMap = new HashMap<>();

	public void setCpSvc(CollectionProtocolService cpSvc) {
		this.cpSvc = cpSvc;
	}

	@Override
	public void start(String id) {
		contextMap.put(id, new HashMap<>());
	}

	@Override
	public void stop(String id, Map<String, Object> runCtxt) {
		contextMap.remove(id);
	}

	@Override
	public ResponseEvent<SpecimenRequirementDetail> importObject(RequestEvent<ImportObjectDetail<SpecimenRequirementDetail>> req) {
		try {
			ImportObjectDetail<SpecimenRequirementDetail> importDetail = req.getPayload();

			SpecimenRequirementDetail detail = importDetail.getObject();
			if (!importDetail.isCreate()) {
				return cpSvc.updateSpecimenRequirement(new RequestEvent<>(detail));
			}

			Map<String, Long> srIds = contextMap.get(importDetail.getId());
			ResponseEvent<SpecimenRequirementDetail> resp = null;
			String lineage = detail.getLineage();
			if (StringUtils.isBlank(lineage) || StringUtils.equalsIgnoreCase(lineage, Specimen.NEW)) {
				resp = importPrimaryRequirements(detail);
			} else {
				String cpShortTitle = detail.getCpShortTitle();
				String eventLabel   = detail.getEventLabel();
				String parentSrCode = detail.getParentSrCode();
				Long parentSrId     = null;

				if (StringUtils.isBlank(cpShortTitle) || StringUtils.isBlank(eventLabel) || StringUtils.isBlank(parentSrCode)) {
					if (StringUtils.isNotBlank(detail.getParentUid())) {
						parentSrId = srIds.get(detail.getParentUid());
					}
				}

				if (StringUtils.equalsIgnoreCase(lineage, Specimen.ALIQUOT)) {
					resp = importAliquots(parentSrId, detail);
				} else if (StringUtils.equalsIgnoreCase(lineage, Specimen.DERIVED)) {
					resp = importDerivatives(parentSrId, detail);
				} else {
					resp = ResponseEvent.userError(SrErrorCode.INVALID_LINEAGE);
				}
			}

			if (resp.isSuccessful() && StringUtils.isNotBlank(detail.getUid())) {
				srIds.put(detail.getUid(), resp.getPayload().getId());
			}

			return resp;
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	private ResponseEvent<SpecimenRequirementDetail> importPrimaryRequirements(SpecimenRequirementDetail detail) {
		return cpSvc.addSpecimenRequirement(new RequestEvent<>(detail));
	}
	
	private ResponseEvent<SpecimenRequirementDetail> importAliquots(Long parentSrId, SpecimenRequirementDetail detail) {
		AliquotSpecimensRequirement aliquotDetail = new AliquotSpecimensRequirement();
		BeanUtils.copyProperties(detail, aliquotDetail);
		aliquotDetail.setNoOfAliquots(1);
		aliquotDetail.setQtyPerAliquot(detail.getInitialQty());
		if (parentSrId != null) {
			aliquotDetail.setParentSrId(parentSrId);
		}
			
		ResponseEvent<List<SpecimenRequirementDetail>> resp = cpSvc.createAliquots(new RequestEvent<>(aliquotDetail));
		resp.throwErrorIfUnsuccessful();
		return new ResponseEvent<>(resp.getPayload().iterator().next());
	}
	
	private ResponseEvent<SpecimenRequirementDetail> importDerivatives(Long parentSrId, SpecimenRequirementDetail detail) {
		DerivedSpecimenRequirement derivativeDetail = new DerivedSpecimenRequirement();
		BeanUtils.copyProperties(detail, derivativeDetail);
		derivativeDetail.setQuantity(detail.getInitialQty());
		if (parentSrId != null) {
			derivativeDetail.setParentSrId(parentSrId);
		}
			
		return cpSvc.createDerived(new RequestEvent<>(derivativeDetail));
	}
}