package com.krishagni.catissueplus.core.de.ui;

import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.factory.AbstractLookupControlFactory;

public class StorageContainerControlFactory extends AbstractLookupControlFactory {
	public static StorageContainerControlFactory getInstance() {
		return new StorageContainerControlFactory();
	}
	
	@Override
	public String getType() {
		return "storageContainer";
	}

	protected Control createControl() {
		return new StorageContainerControl();
	}
}
