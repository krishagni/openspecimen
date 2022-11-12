package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.common.domain.KeySequence;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.UniqueIdGenerator;

public class UniqueIdGeneratorImpl extends AbstractDao<KeySequence> implements UniqueIdGenerator {
	@Override
	public Long getUniqueId(String type, String id) {
		return getUniqueId(type, id, 0L);
	}

	@Override
	public Long getUniqueId(String type, String id, Long defStartSeq) {
		return getUniqueId(type, id, defStartSeq, 1);
	}

	@Override
	public Long getUniqueId(String type, String id, Long defStartSeq, int incrementBy) {
		List<KeySequence> seqs = createNamedQuery(GET_BY_TYPE_AND_TYPE_ID, KeySequence.class)
			.acquirePessimisticWriteLock("ks")
			.setParameter("type", type)
			.setParameter("typeId", id)
			.list();

		KeySequence seq;
		if (seqs.isEmpty()) {
			seq = new KeySequence();
			seq.setType(type);
			seq.setTypeId(id);
			seq.setSequence(defStartSeq);
		} else {
			seq = seqs.iterator().next();
		}

		Long uniqueId = seq.increment();
		seq.incrementBy(incrementBy >= 1 ? incrementBy - 1 : 0);
		getCurrentSession().saveOrUpdate(seq);
		return uniqueId;
	}

	private static final String FQN = KeySequence.class.getName();
	
	private static final String GET_BY_TYPE_AND_TYPE_ID = FQN + ".getByTypeAndTypeId";
}