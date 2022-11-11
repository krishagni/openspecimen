package com.krishagni.catissueplus.core.biospecimen.services;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public interface SpecimenResolver {
	public Specimen getSpecimen(String cpShortTitle, String label);

	public Specimen getSpecimen(Long specimenId, String cpShortTitle, String label);

	public Specimen getSpecimen(Long specimenId, String cpShortTitle, String label, OpenSpecimenException ose);

	public Specimen getSpecimen(Long specimenId, String cpShortTitle, String label, String barcode);

	public Specimen getSpecimen(Long specimenId, String cpShortTitle, String label, String barcode, OpenSpecimenException ose);

	public Specimen getSpecimenByAddlLabel(String cpShortTitle, String label);

	public Specimen getSpecimenByBarcode(String cpShortTitle, String barcode);
}
