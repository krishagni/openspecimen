package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.HashSet;
import java.util.Set;

import com.krishagni.catissueplus.core.common.TransactionCache;
import com.krishagni.catissueplus.core.common.util.Status;

public class ClosedSpecimensTracker {
	private static final ClosedSpecimensTracker instance = new ClosedSpecimensTracker();

	public static ClosedSpecimensTracker getInstance() {
		return instance;
	}

	public void add(Specimen specimen) {
		if (specimen.isClosed() && specimen.getId() != null) {
			getClosedSpecimenIds().add(specimen.getId());
		}
	}

	public boolean isActive(Specimen specimen) {
		// The specimen is closed in this txn or still active
		return getClosedSpecimenIds().contains(specimen.getId()) ||
			Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(specimen.getActivityStatus());
	}

	private Set<Long> getClosedSpecimenIds() {
		return TransactionCache.getInstance().get("closedSpecimenIds", new HashSet<>());
	}
}
