package com.krishagni.catissueplus.core.common;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.krishagni.catissueplus.core.common.util.LogUtil;

public class TransactionCache {
	private static final LogUtil logger = LogUtil.getLogger(TransactionCache.class);

	private static final ThreadLocal<Map<String, Object>> cache = ThreadLocal.withInitial(HashMap::new);

	private static final TransactionCache instance = new TransactionCache();

	public static TransactionCache getInstance() {
		return instance;
	}

	public <T> T get(String key) {
		registerIfRequired();
		return (T) cache.get().get(key);
	}

	public <T> T get(String key, T putIfAbsent) {
		registerIfRequired();
		T result = (T) cache.get().get(key);
		if (result == null) {
			put(key, putIfAbsent);
			result = putIfAbsent;
		}

		return result;
	}

	public void put(String key, Object value) {
		this.put(key, value, null);
	}

	public void put(String key, Object value, Consumer<Integer> onFinish) {
		registerIfRequired();
		cache.get().put(key, value);
		if (onFinish != null) {
			Map<String, Consumer<Integer>> handlers = get("$transactionFinishHandlers");
			if (handlers == null) {
				handlers = new LinkedHashMap<>();
				cache.get().put("$transactionFinishHandlers", handlers);
			}

			handlers.put(key, onFinish);
		}
	}

	public void clear(int status) {
		if (logger.isDebugEnabled()) {
			Set<String> cacheKeys = cache.get().keySet();
			String cacheKeysCsv = String.join(", ", cacheKeys);
			int items = cacheKeys.size();
			logger.debug("Thread " + getThreadId() + " destroying transactional cache. Items = " + items + " (" + cacheKeysCsv + ")");
		}

		Map<String, Consumer<Integer>> handlers = get("$transactionFinishHandlers");
		cache.get().clear();
		cache.remove();

		if (handlers != null && !handlers.isEmpty()) {
			String threadId = getThreadId();
			logger.info("Thread " + threadId + " invoking transaction finish handlers: " + handlers.size() + ", status: " + status);
			handlers.forEach(
				(name, handler) -> {
					try {
						logger.info("Thread " + threadId + " invoking transaction finish handler " + name);
						handler.accept(status);
					} catch (Throwable t) {
						logger.error("Error invoking the transaction completion handler: ", t);
					}
				}
			);
		}
	}

	private void registerIfRequired() {
		Boolean registered = (Boolean) cache.get().get("$txnSyncRegistered");
		if (Boolean.TRUE.equals(registered)) {
			return;
		}

		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			throw new RuntimeException("Transaction is not active!");
		}

		TransactionSynchronizationManager.registerSynchronization(
			new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					clear(status);
				}
			}
		);

		cache.get().put("$txnSyncRegistered", true);
		if (logger.isDebugEnabled()) {
			logger.debug("Thread " + getThreadId() + " initialising transactional cache. Items = " + cache.get().size());
		}
	}

	private String getThreadId() {
		Thread thread = Thread.currentThread();
		return "[" + thread.getId() + " " + thread.getName() + "]";
	}
}
