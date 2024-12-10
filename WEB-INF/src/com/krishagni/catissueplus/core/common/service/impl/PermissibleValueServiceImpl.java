
package com.krishagni.catissueplus.core.common.service.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
		PermissibleValue value = null;
		Object key = null;
		if (crit.getId() != null) {
			value = daoFactory.getPermissibleValueDao().getById(crit.getId());
			key = crit.getId();
		} else if (StringUtils.isNotBlank(crit.getName()) && StringUtils.isNotBlank(crit.paramString("attribute"))) {
			value = daoFactory.getPermissibleValueDao().getByValue(crit.paramString("attribute"), crit.getName());
			key = crit.paramString("attribute") + ": " + crit.getName();
		}

		if (key == null) {
			throw OpenSpecimenException.userError(PvErrorCode.VALUE_REQUIRED);
		} else if (value == null) {
			throw OpenSpecimenException.userError(PvErrorCode.NOT_FOUND, key);
		}

		boolean includeProps = Boolean.TRUE.equals(crit.paramBoolean("includeProps"));
		return ResponseEvent.response(PvDetail.from(value, includeProps, includeProps));
	}
}
