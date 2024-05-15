package com.krishagni.catissueplus.core.common.service.impl;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.LabelPrintJobItemListCriteria;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.LabelPrintJobItemDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.LabelPrintJobService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class LabelPrintJobServiceImpl implements LabelPrintJobService {

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<LabelPrintJobItemDetail>> getPrintJobItems(RequestEvent<LabelPrintJobItemListCriteria> req) {
		try {
			User currentUser = AuthUtil.getCurrentUser();
			if (currentUser == null || !currentUser.canManagePrintJobs()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			List<LabelPrintJobItem> items = daoFactory.getLabelPrintJobDao().getPrintJobItems(req.getPayload());
			return ResponseEvent.response(LabelPrintJobItemDetail.from(items, Boolean.TRUE.equals(req.getPayload().includeData())));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
}
