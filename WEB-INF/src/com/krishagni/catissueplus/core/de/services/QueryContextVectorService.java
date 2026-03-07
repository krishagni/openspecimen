package com.krishagni.catissueplus.core.de.services;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.VectorContextResult;
import com.krishagni.catissueplus.core.de.events.VectorContextSearchOp;
import com.krishagni.catissueplus.core.de.events.VectorMetadataSyncOp;

public interface QueryContextVectorService {
	ResponseEvent<Boolean> syncMetadata(RequestEvent<VectorMetadataSyncOp> req);

	ResponseEvent<VectorContextResult> search(RequestEvent<VectorContextSearchOp> req);

	void onMetadataChanged();
}
