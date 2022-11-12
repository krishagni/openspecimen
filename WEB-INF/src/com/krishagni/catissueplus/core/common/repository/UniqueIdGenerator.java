package com.krishagni.catissueplus.core.common.repository;

public interface UniqueIdGenerator {
	Long getUniqueId(String type, String id);

	Long getUniqueId(String type, String id, Long defStartSeq);

	Long getUniqueId(String type, String id, Long defStartSeq, int incrementBy);
}
