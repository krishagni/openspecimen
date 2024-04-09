package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.HashSet;
import java.util.Set;

import com.krishagni.catissueplus.core.common.TransactionalThreadLocals;
import com.krishagni.catissueplus.core.common.util.Status;

public class ClosedSpecimensTracker {
	private static final ClosedSpecimensTracker instance = new ClosedSpecimensTracker();

	private ThreadLocal<Set<Long>> closedSpecimens = new ThreadLocal<>() {
		@Override
		protected Set<Long> initialValue() {
			TransactionalThreadLocals.getInstance().register(this);
			return new HashSet<>();
		}
	};

	public static ClosedSpecimensTracker getInstance() {
		return instance;
	}

	public void add(Specimen specimen) {
		if (specimen.isClosed() && specimen.getId() != null) {
			closedSpecimens.get().add(specimen.getId());
		}
	}

	public boolean isActive(Specimen specimen) {
		// The specimen is closed in this txn or still active
		return closedSpecimens.get().contains(specimen.getId()) ||
			Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(specimen.getActivityStatus());
	}
}
