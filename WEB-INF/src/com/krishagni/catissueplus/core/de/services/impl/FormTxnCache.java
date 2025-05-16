package com.krishagni.catissueplus.core.de.services.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.common.TransactionCache;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;

import edu.common.dynamicextensions.domain.nui.Container;

@Configurable
public class FormTxnCache {
	private final static FormTxnCache INSTANCE = new FormTxnCache();

	public static FormTxnCache getInstance() {
		return INSTANCE;
	}

	public Container getForm(Long formId) {
		List<Container> forms = getFormsCache();
		Container form = forms.stream().filter(f -> f.getId().equals(formId)).findFirst().orElse(null);
		if (form == null) {
			form = Container.getContainer(formId);
			if (form == null) {
				throw OpenSpecimenException.userError(FormErrorCode.NOT_FOUND, formId, 1);
			}

			forms.add(form);
		}

		return form;
	}

	public Container getForm(String formName) {
		List<Container> forms = getFormsCache();
		Container form = forms.stream().filter(f -> f.getName().equals(formName)).findFirst().orElse(null);
		if (form == null) {
			form = Container.getContainer(formName);
			if (form == null) {
				throw OpenSpecimenException.userError(FormErrorCode.NOT_FOUND, formName, 1);
			}

			forms.add(form);
		}

		return form;
	}

	private List<Container> getFormsCache() {
		return TransactionCache.getInstance().get("formsCache", new ArrayList<>());
	}
}


