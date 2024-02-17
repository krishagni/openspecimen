package com.krishagni.catissueplus.core.administrative.services.impl;

import com.krishagni.catissueplus.core.administrative.domain.ContainerLabelPrintRule;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.services.impl.AbstractLabelPrinter;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;

public class DefaultContainerLabelPrinter extends AbstractLabelPrinter<StorageContainer> {
	@Override
	protected boolean isApplicableFor(LabelPrintRule rule, StorageContainer container, User user, String ipAddr) {
		ContainerLabelPrintRule contLabelPrintRule = (ContainerLabelPrintRule) rule;
		return contLabelPrintRule.isApplicableFor(container, user, ipAddr);
	}

	@Override
	protected String getObjectType() {
		return "CONTAINER";
	}

	@Override
	protected String getItemType() {
		return StorageContainer.getEntityName();
	}

	@Override
	protected String getItemLabel(StorageContainer container) {
		return container.getId().toString();
	}

	@Override
	protected Long getItemId(StorageContainer container) {
		return container.getId();
	}
}