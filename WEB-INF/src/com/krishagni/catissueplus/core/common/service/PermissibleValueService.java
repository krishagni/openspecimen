
package com.krishagni.catissueplus.core.common.service;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.events.ListPvCriteria;
import com.krishagni.catissueplus.core.administrative.events.ListPvAttributesCriteria;
import com.krishagni.catissueplus.core.administrative.events.PvDetail;
import com.krishagni.catissueplus.core.administrative.events.PermissibleValueDetails;
import com.krishagni.catissueplus.core.administrative.events.PvAttributeDetail;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface PermissibleValueService {
	ResponseEvent<List<PvAttributeDetail>> getAttributes(RequestEvent<ListPvAttributesCriteria> req);

	ResponseEvent<Long> getAttributesCount(RequestEvent<ListPvAttributesCriteria> req);

	ResponseEvent<PvAttributeDetail> createAttribute(RequestEvent<PvAttributeDetail> req);

	ResponseEvent<PvAttributeDetail> promoteAttribute(RequestEvent<PvAttributeDetail> req);

	ResponseEvent<List<PvAttributeDetail>> promoteAttributes(RequestEvent<List<PvAttributeDetail>> req);

	ResponseEvent<List<PvDetail>> getPermissibleValues(RequestEvent<ListPvCriteria> req);

	ResponseEvent<Long> getPermissibleValuesCount(RequestEvent<ListPvCriteria> req);

	ResponseEvent<PvDetail> getPermissibleValue(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<PermissibleValueDetails> createPermissibleValue(RequestEvent<PermissibleValueDetails> req);

	ResponseEvent<PermissibleValueDetails> updatePermissibleValue(RequestEvent<PermissibleValueDetails> req);

	ResponseEvent<PermissibleValueDetails> deletePermissibleValue(RequestEvent<Long> req);
}
