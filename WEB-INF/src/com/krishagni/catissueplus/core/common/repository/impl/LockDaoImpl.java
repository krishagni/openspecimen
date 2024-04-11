package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.Collection;
import java.util.List;

import com.krishagni.catissueplus.core.common.domain.Lock;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.LockDao;
import com.krishagni.catissueplus.core.common.util.Utility;

public class LockDaoImpl extends AbstractDao<Lock> implements LockDao {
	@Override
	public Class<Lock> getType() {
		return Lock.class;
	}

	@Override
	public Lock getById(Long id) {
		throw new UnsupportedOperationException("Locks cannot be obtained by IDs");
	}

	@Override
	public Lock getById(Long id, String activeCondition) {
		throw new UnsupportedOperationException("Locks cannot be obtained by IDs");
	}

	@Override
	public List<Lock> getByIds(Collection<Long> ids) {
		throw new UnsupportedOperationException("Locks cannot be obtained by IDs");
	}

	@Override
	public List<Lock> getByIds(Collection<Long> ids, String activeCondition) {
		throw new UnsupportedOperationException("Locks cannot be obtained by IDs");
	}

	@Override
	public <R> void delete(R obj) {
		throw new UnsupportedOperationException("Locks cannot be deleted");
	}

	@Override
	public Lock getLockForUpdate(String type) {
		return createNamedQuery(GET_LOCK, Lock.class)
			.acquirePessimisticWriteLock("l")
			.setParameter("type", type)
			.uniqueResult();
	}

	@Override
	public void unlockAll() {
		createNamedQuery(UNLOCK_ALL)
			.setParameter("node", Utility.getNodeName())
			.executeUpdate();
	}

	private static final String FQN = Lock.class.getName();

	private static final String GET_LOCK = FQN + ".getLock";

	private static final String UNLOCK_ALL = FQN + ".unlockAll";
}
