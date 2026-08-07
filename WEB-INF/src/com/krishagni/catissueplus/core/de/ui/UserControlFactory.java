package com.krishagni.catissueplus.core.de.ui;

import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.factory.AbstractLookupControlFactory;

public class UserControlFactory extends AbstractLookupControlFactory {
	public static UserControlFactory getInstance() {
		return new UserControlFactory();
	}
	
	@Override
	public String getType() {
		return "userField";
	}

	protected Control createControl() {
		return new UserControl();
	}
}
