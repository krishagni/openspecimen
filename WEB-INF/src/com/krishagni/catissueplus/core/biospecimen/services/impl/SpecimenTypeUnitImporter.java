package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenTypeUnit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenTypeUnitError;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenTypeUnitDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenTypeUnitsService;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class SpecimenTypeUnitImporter implements ObjectImporter<SpecimenTypeUnitDetail, SpecimenTypeUnitDetail> {
	private SpecimenTypeUnitsService unitsSvc;

	private DaoFactory daoFactory;

	public void setUnitsSvc(SpecimenTypeUnitsService unitsSvc) {
		this.unitsSvc = unitsSvc;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public ResponseEvent<SpecimenTypeUnitDetail> importObject(RequestEvent<ImportObjectDetail<SpecimenTypeUnitDetail>> req) {
		try {
			ImportObjectDetail<SpecimenTypeUnitDetail> detail = req.getPayload();
			Map<String, String> params = detail.getParams();
			if (params == null) {
				params = Collections.emptyMap();
			}

			Long cpId = null;
			try {
				String cpIdStr = params.get("cpId");
				if (StringUtils.isNotBlank(cpIdStr)) {
					cpId = Long.parseLong(cpIdStr);
				}
			} catch (NumberFormatException nfe) {
				return ResponseEvent.userError(CommonErrorCode.INVALID_INPUT, "Invalid CP ID: " + nfe.getMessage() + ". Probably a bug in the UI. Contact development team.");
			}

			SpecimenTypeUnitDetail unitDetail = detail.getObject();
			if (cpId != null && cpId > 0L) {
				unitDetail.setCpId(cpId);
			}

			boolean update = false; // this is insert op
			if (unitDetail.getId() != null) {
				update = true;
			} else {
				SpecimenTypeUnit dbUnit = daoFactory.getSpecimenTypeUnitDao().getUnit(unitDetail.getCpId(), unitDetail.getCpShortTitle(), unitDetail.getSpecimenClass(), unitDetail.getType());
				if (dbUnit != null) {
					unitDetail.setId(dbUnit.getId());
					update = true;
				}
			}

			if (Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(unitDetail.getActivityStatus())) {
				if (update) {
					return unitsSvc.deleteUnit(RequestEvent.wrap(unitDetail));
				} else {
					return ResponseEvent.userError(SpecimenTypeUnitError.NOT_FOUND, unitDetail.getKey());
				}
			} else if (update) {
				return unitsSvc.updateUnit(RequestEvent.wrap(unitDetail));
			} else {
				return unitsSvc.createUnit(RequestEvent.wrap(unitDetail));
			}
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
}
