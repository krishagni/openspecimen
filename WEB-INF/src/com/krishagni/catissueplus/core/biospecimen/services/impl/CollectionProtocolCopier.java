package com.krishagni.catissueplus.core.biospecimen.services.impl;


import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.common.service.impl.AbstractObjectCopier;

public class CollectionProtocolCopier extends AbstractObjectCopier<CollectionProtocol>{

	public CollectionProtocolCopier() {
		addAttrCopier(CollectionProtocol::copyTo);
	}
	
	public CollectionProtocol copy(CollectionProtocol src) {
		CollectionProtocol result  = new CollectionProtocol();
		super.copy(src, result);
		return result;
	}
	
}
