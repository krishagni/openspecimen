package com.krishagni.catissueplus.core.common.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

public class ConcurrentLruCache<K, V> {
	private ConcurrentHashMap<K, V> elements;

	//
	// least recently accessed will be at the front of the queue
	// most recently accessed will be at the end of the queue
	//
	private ConcurrentLinkedDeque<K> accessOrder;

	//
	// to ensure the elements and accessOrder are thread safe
	//
	private ReentrantLock lock;

	private int capacity;

	public ConcurrentLruCache(int capacity) {
		elements = new ConcurrentHashMap<>(capacity + 1, 0.75f, 16);
		accessOrder = new ConcurrentLinkedDeque<>();
		lock = new ReentrantLock(true);
		this.capacity = capacity;
	}

	public V put(K key, V value) {
		lock.lock();
		try {
			value = elements.put(key, value);
			accessOrder.remove(key);
			accessOrder.addLast(key);
			if (elements.size() > capacity) {
				K lruKey = accessOrder.removeFirst();
				elements.remove(lruKey);
			}

			return value;
		} finally {
			lock.unlock();
		}
	}

	public V touch(K key, V value) {
		lock.lock();
		try {
			V existing = elements.get(key);
			if (existing != null) {
				elements.put(key, value);
				existing = value;

				accessOrder.remove(key);
				accessOrder.addLast(key);
			}

			return existing;
		} finally {
			lock.unlock();
		}
	}

	public V get(K key) {
		lock.lock();
		try {
			V value = elements.get(key);
			if (value != null) {
				accessOrder.remove(key);
				accessOrder.addLast(key);
			}

			return value;
		} finally {
			lock.unlock();
		}
	}

	public V remove(K key) {
		lock.lock();
		try {
			V value = elements.remove(key);
			accessOrder.remove(key);
			return value;
		} finally {
			lock.unlock();
		}
	}

	public int removeIf(Predicate<V> predicate) {
		int result = 0;

		lock.lock();
		try {
			for (Map.Entry<K, V> entry : elements.entrySet()) {
				if (predicate.test(entry.getValue())) {
					elements.remove(entry.getKey());
					accessOrder.remove(entry.getKey());
					++result;
				}
			}

			return result;
		} finally {
			lock.unlock();
		}
	}
}
