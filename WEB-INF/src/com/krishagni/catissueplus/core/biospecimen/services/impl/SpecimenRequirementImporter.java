package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.List;

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

public class SpecimenRequirementImporter implements ObjectImporter<SpecimenRequirementDetail, SpecimenRequirementDetail> {
	private CollectionProtocolService cpSvc;

	public void setCpSvc(CollectionProtocolService cpSvc) {
		this.cpSvc = cpSvc;
	}

	@Override
	public ResponseEvent<SpecimenRequirementDetail> importObject(RequestEvent<ImportObjectDetail<SpecimenRequirementDetail>> req) {
		try {
			ImportObjectDetail<SpecimenRequirementDetail> importDetail = req.getPayload();
			SpecimenRequirementDetail detail = importDetail.getObject();

			if (!importDetail.isCreate()) {
				return cpSvc.updateSpecimenRequirement(new RequestEvent<>(detail));
			}

			String lineage = detail.getLineage();
			if (StringUtils.isBlank(lineage) || StringUtils.equalsIgnoreCase(lineage, Specimen.NEW)) {
				return importPrimaryRequirements(detail);
			} else if (StringUtils.equalsIgnoreCase(lineage, Specimen.ALIQUOT)) {
				return importAliquots(detail);
			} else if (StringUtils.equalsIgnoreCase(lineage, Specimen.DERIVED)) {
				return importDerivatives(detail);
			} else {
				return ResponseEvent.userError(SrErrorCode.INVALID_LINEAGE);
			}
			
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	private ResponseEvent<SpecimenRequirementDetail> importPrimaryRequirements(SpecimenRequirementDetail detail) {
		return cpSvc.addSpecimenRequirement(new RequestEvent<SpecimenRequirementDetail>(detail));
	}
	
	private ResponseEvent<SpecimenRequirementDetail> importAliquots(SpecimenRequirementDetail detail) {
		AliquotSpecimensRequirement aliquotDetail = new AliquotSpecimensRequirement();
		BeanUtils.copyProperties(detail, aliquotDetail);
		aliquotDetail.setNoOfAliquots(1);
		aliquotDetail.setQtyPerAliquot(detail.getInitialQty());
			
		ResponseEvent<List<SpecimenRequirementDetail>> resp = cpSvc.createAliquots(new RequestEvent<AliquotSpecimensRequirement>(aliquotDetail));
		resp.throwErrorIfUnsuccessful();
		return new ResponseEvent<SpecimenRequirementDetail>(resp.getPayload().iterator().next());
	}
	
	private ResponseEvent<SpecimenRequirementDetail> importDerivatives(SpecimenRequirementDetail detail) {
		DerivedSpecimenRequirement derivativeDetail = new DerivedSpecimenRequirement();
		BeanUtils.copyProperties(detail, derivativeDetail);
		derivativeDetail.setQuantity(detail.getInitialQty());
			
		return cpSvc.createDerived(new RequestEvent<DerivedSpecimenRequirement>(derivativeDetail));
	}
}