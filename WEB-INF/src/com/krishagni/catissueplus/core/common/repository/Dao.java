
package com.krishagni.catissueplus.core.common.repository;

import java.util.Collection;
import java.util.List;

public interface Dao<T> {
	void saveOrUpdate(T t);
	
	void saveOrUpdate(T t, boolean flush);
	
	<R> void delete(R t);
	
	T getById(Long id);
	
	T getById(Long id, String activeCondition);

	List<T> getByIds(Collection<Long> ids);

	List<T> getByIds(Collection<Long> ids, String activeCondition);
	
	void flush();
}
