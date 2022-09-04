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
		getCurrentSession().saveOrUpdate(seq);
		return uniqueId;
	}

	private static final String FQN = KeySequence.class.getName();
	
	private static final String GET_BY_TYPE_AND_TYPE_ID = FQN + ".getByTypeAndTypeId";
}