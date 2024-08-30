
package com.krishagni.catissueplus.core.common.repository;

import java.util.Collection;
import java.util.List;

public interface Dao<T> {
	void saveOrUpdate(T t);
	
	void saveOrUpdate(T t, boolean flush);
	
	<R> void delete(R t);
	
	T getById(Long id);

	List<T> getByIds(Collection<Long> ids);
	
	void flush();
}
