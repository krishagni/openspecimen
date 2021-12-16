package com.krishagni.catissueplus.core.common;

import java.util.LinkedHashSet;
import java.util.Set;


import com.krishagni.catissueplus.core.common.util.LogUtil;

public class TransactionalThreadLocals {
	private static final LogUtil logger = LogUtil.getLogger(TransactionalThreadLocals.class);

	private static ThreadLocal<TransactionalThreadLocals> instance = new ThreadLocal<TransactionalThreadLocals>() {
		@Override
		protected TransactionalThreadLocals initialValue() {
			logger.debug("Creating a new transaction based thread locals list");
			return new TransactionalThreadLocals();
		}
	};

	private Set<ThreadLocal<?>> threadLocals = new LinkedHashSet<>();

	public static TransactionalThreadLocals getInstance() {
		return instance.get();
	}

	public void register(ThreadLocal<?> threadLocal) {
		threadLocals.add(threadLocal);
	}

	public void cleanup() {
		threadLocals.forEach(ThreadLocal::remove);
		instance.remove();
		logger.debug("Cleaned up transaction based thread locals list");
	}
}