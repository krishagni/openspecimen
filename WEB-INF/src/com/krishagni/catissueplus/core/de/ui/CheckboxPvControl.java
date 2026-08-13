package com.krishagni.catissueplus.core.de.ui;

public class CheckboxPvControl extends AbstractPvOptionsControl {
	private static final long serialVersionUID = 1L;

	public CheckboxPvControl() {
		setMultiValued(true);
	}

	@Override
	public String getCtrlType() {
		return "pvCheckbox";
	}
}
