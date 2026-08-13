package com.krishagni.catissueplus.core.de.ui;

import edu.common.dynamicextensions.domain.nui.Control;

public class PvControlFactory extends AbstractPvControlFactory {
	public static PvControlFactory getInstance() {
		return new PvControlFactory();
	}

	@Override
	public String getType() {
		return "pvField";
	}

	@Override
	protected Control createControl() {
		return new PvControl();
	}
}
