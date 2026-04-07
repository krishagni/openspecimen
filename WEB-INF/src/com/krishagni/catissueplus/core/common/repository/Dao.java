
package com.krishagni.catissueplus.core.common.repository;

import java.util.Collection;
import java.util.List;

public interface Dao<T> {
	void saveOrUpdate(T t);
	
	void saveOrUpdate(T t, boolean flush);

	T merge(T t);

	T merge(T t, boolean flush);

	void save(Object obj);

	void save(Object obj, boolean flush);

	void update(Object obj);

	void update(Object obj, boolean flush);
	
	<R> void delete(R t);
	
	T getById(Long id);

	List<T> getByIds(Collection<Long> ids);
	
	void flush();
}
