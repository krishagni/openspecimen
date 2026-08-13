package com.krishagni.catissueplus.core.de.ui;

import edu.common.dynamicextensions.domain.nui.Control;

public class CheckboxPvControlFactory extends AbstractPvOptionsControlFactory {
	public static CheckboxPvControlFactory getInstance() {
		return new CheckboxPvControlFactory();
	}

	@Override
	public String getType() {
		return "pvCheckbox";
	}

	@Override
	protected boolean isMultiValued() {
		return true;
	}

	@Override
	protected Control createControl() {
		return new CheckboxPvControl();
	}
}
