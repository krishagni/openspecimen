package com.krishagni.catissueplus.core.biospecimen.services.impl;

import org.apache.commons.lang3.StringUtils;


import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.services.impl.ExtensionsUtil;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class SpecimenImporter implements ObjectImporter<SpecimenDetail, SpecimenDetail> {
	
	private SpecimenService specimenSvc;

	private DaoFactory daoFactory;
	
	public void setSpecimenSvc(SpecimenService specimenSvc) {
		this.specimenSvc = specimenSvc;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public ResponseEvent<SpecimenDetail> importObject(RequestEvent<ImportObjectDetail<SpecimenDetail>> req) {
		try {
			ImportObjectDetail<SpecimenDetail> detail = req.getPayload();
			SpecimenDetail spmn = detail.getObject();
			spmn.setForceDelete(true);
			ExtensionsUtil.initFileFields(detail.getUploadedFilesDir(), spmn.getExtensionDetail());

			boolean update = false; // this is insert op
			switch (detail.getType()) {
				case "UPSERT":
					if (spmn.getId() != null) {
						update = true;
					} else {
						Specimen dbSpmn = getSpecimen(spmn);
						if (dbSpmn != null) {
							spmn.setId(dbSpmn.getId());
							update = true;
						}
					}
					break;

				case "UPDATE":
					update = true;
					break;
			}

			return upsert(spmn, update);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}		
	}

	private Specimen getSpecimen(SpecimenDetail input) {
		String cpShortTitle = input.getCpShortTitle();
		if (StringUtils.isBlank(cpShortTitle)) {
			if (StringUtils.isBlank(input.getVisitName())) {
				return null;
			}

			Visit dbVisit = daoFactory.getVisitsDao().getByName(input.getVisitName());
			if (dbVisit == null) {
				return null;
			}

			cpShortTitle = dbVisit.getCollectionProtocol().getShortTitle();
		}

		Specimen specimen = null;
		if (StringUtils.isNotBlank(input.getLabel())) {
			specimen = daoFactory.getSpecimenDao().getByLabelAndCp(cpShortTitle, input.getLabel());
		}

		if (specimen == null && StringUtils.isNotBlank(input.getBarcode())) {
			specimen = daoFactory.getSpecimenDao().getByBarcodeAndCp(cpShortTitle, input.getBarcode());
		}

		return specimen;
	}

	private ResponseEvent<SpecimenDetail> upsert(SpecimenDetail input, boolean update) {
		if (update) {
			return specimenSvc.updateSpecimen(RequestEvent.wrap(input));
		} else {
			return specimenSvc.createSpecimen(RequestEvent.wrap(input));
		}
	}
}
