package com.krishagni.catissueplus.core.de.ui;

import edu.common.dynamicextensions.domain.nui.Control;

public class RadioButtonPvControlFactory extends AbstractPvOptionsControlFactory {
	public static RadioButtonPvControlFactory getInstance() {
		return new RadioButtonPvControlFactory();
	}

	@Override
	public String getType() {
		return "pvRadioButton";
	}

	@Override
	protected boolean isMultiValued() {
		return false;
	}

	@Override
	protected Control createControl() {
		return new RadioButtonPvControl();
	}
}
