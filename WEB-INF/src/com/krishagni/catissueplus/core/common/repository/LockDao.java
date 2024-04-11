package com.krishagni.catissueplus.core.common.repository;

import com.krishagni.catissueplus.core.common.domain.Lock;

public interface LockDao extends Dao<Lock> {
	Lock getLockForUpdate(String type);

	void unlockAll();
}
