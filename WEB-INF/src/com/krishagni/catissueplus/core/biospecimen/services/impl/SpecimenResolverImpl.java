package com.krishagni.catissueplus.core.biospecimen.services.impl;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenResolver;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

public class SpecimenResolverImpl implements SpecimenResolver {

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public Specimen getSpecimen(String cpShortTitle, String label) {
		Specimen specimen = null;

		if (areLabelsUniquePerCp()) {
			if (StringUtils.isBlank(cpShortTitle)) {
				throw OpenSpecimenException.userError(CpErrorCode.SHORT_TITLE_REQUIRED);
			}

			specimen = daoFactory.getSpecimenDao().getByLabelAndCp(cpShortTitle, label);
		} else {
			specimen = daoFactory.getSpecimenDao().getByLabel(label);
		}

		return specimen;
	}

	@Override
	public Specimen getSpecimen(Long specimenId, String cpShortTitle, String label) {
		return getSpecimen(specimenId, cpShortTitle, label, (String)null);
	}

	@Override
	public Specimen getSpecimen(Long specimenId, String cpShortTitle, String label, OpenSpecimenException ose) {
		return getSpecimen(specimenId, cpShortTitle, label, null, ose);
	}

	@Override
	public Specimen getSpecimen(Long specimenId, String cpShortTitle, String label, String barcode) {
		Specimen specimen = null;
		Object key = null;
		ErrorCode notFoundError = SpecimenErrorCode.NOT_FOUND;

		if (specimenId != null) {
			key = specimenId;
			specimen = daoFactory.getSpecimenDao().getById(specimenId);
		} else if (StringUtils.isNotBlank(label)) {
			key = label;
			specimen = getSpecimen(cpShortTitle, label);
			if (areLabelsUniquePerCp()) {
				notFoundError = SpecimenErrorCode.NOT_FOUND_IN_CP;
			}
		} else if (StringUtils.isNotBlank(barcode)) {
			key = barcode;
			specimen = getSpecimenByBarcode(cpShortTitle, barcode);
			if (areBarcodesUniquePerCp()) {
				notFoundError = SpecimenErrorCode.NOT_FOUND_IN_CP;
			}
		}

		if (key == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.LABEL_REQUIRED);
		} else if (specimen == null) {
			throw OpenSpecimenException.userError(notFoundError, key, cpShortTitle);
		}

		return specimen;
	}

	@Override
	public Specimen getSpecimen(Long specimenId, String cpShortTitle, String label, String barcode, OpenSpecimenException ose) {
		try {
			return getSpecimen(specimenId, cpShortTitle, label, barcode);
		} catch (OpenSpecimenException e) {
			e.getErrors().forEach(pe -> ose.addError(pe.error(), pe.params()));
		}

		return null;
	}

	@Override
	public Specimen getSpecimenByBarcode(String cpShortTitle, String barcode) {
		Specimen specimen = null;

		if (areBarcodesUniquePerCp()) {
			if (StringUtils.isBlank(cpShortTitle)) {
				throw OpenSpecimenException.userError(CpErrorCode.SHORT_TITLE_REQUIRED);
			}

			specimen = daoFactory.getSpecimenDao().getByBarcodeAndCp(cpShortTitle, barcode);
		} else {
			specimen = daoFactory.getSpecimenDao().getByBarcode(barcode);
		}

		return specimen;
	}

	private boolean areLabelsUniquePerCp() {
		return ConfigUtil.getInstance().getBoolSetting(
			ConfigParams.MODULE,
			ConfigParams.UNIQUE_SPMN_LABEL_PER_CP,
			false
		);
	}

	private boolean areBarcodesUniquePerCp() {
		return ConfigUtil.getInstance().getBoolSetting(
			ConfigParams.MODULE,
			ConfigParams.UNIQUE_SPMN_BARCODE_PER_CP,
			false
		);
	}
}