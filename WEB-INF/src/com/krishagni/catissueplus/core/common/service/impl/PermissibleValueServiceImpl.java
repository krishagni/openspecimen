
package com.krishagni.catissueplus.core.common.service.impl;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.factory.PvErrorCode;
import com.krishagni.catissueplus.core.administrative.events.ListPvCriteria;
import com.krishagni.catissueplus.core.administrative.events.PvDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.PermissibleValueService;

public class PermissibleValueServiceImpl implements PermissibleValueService {
	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<PvDetail>> getPermissibleValues(RequestEvent<ListPvCriteria> req) {
		ListPvCriteria crit = req.getPayload();		
		List<PermissibleValue> pvs = daoFactory.getPermissibleValueDao().getPvs(crit);
		return ResponseEvent.response(PvDetail.from(pvs, crit.includeParentValue(), crit.includeProps()));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getPermissibleValuesCount(RequestEvent<ListPvCriteria> req) {
		return ResponseEvent.response(daoFactory.getPermissibleValueDao().getPvsCount(req.getPayload()));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<PvDetail> getPermissibleValue(RequestEvent<EntityQueryCriteria> req) {
		EntityQueryCriteria crit = req.getPayload();
		PermissibleValue value = daoFactory.getPermissibleValueDao().getById(crit.getId());
		if (value == null) {
			throw OpenSpecimenException.userError(PvErrorCode.NOT_FOUND, crit.getId());
		}

		boolean includeProps = Boolean.TRUE.equals(crit.paramBoolean("includeProps"));
		return ResponseEvent.response(PvDetail.from(value, includeProps, includeProps));
	}
}
