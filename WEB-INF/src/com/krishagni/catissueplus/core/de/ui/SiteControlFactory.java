package com.krishagni.catissueplus.core.de.ui;

import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.factory.AbstractLookupControlFactory;

public class SiteControlFactory extends AbstractLookupControlFactory {
	public static SiteControlFactory getInstance() {
		return new SiteControlFactory();
	}
	
	@Override
	public String getType() {
		return "siteField";
	}

	protected Control createControl() {
		return new SiteControl();
	}
}
