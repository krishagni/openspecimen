package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

import com.krishagni.catissueplus.core.biospecimen.services.SpecimenEventsService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.repository.FormDao;
import com.krishagni.catissueplus.core.de.services.FormService;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.napi.FormData;

public class SpecimenEventsServiceImpl implements SpecimenEventsService {
	private FormDao formDao;

	private FormService formSvc;

	public void setFormDao(FormDao formDao) {
		this.formDao = formDao;
	}

	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<FormData>> saveSpecimenEvents(RequestEvent<List<FormData>> req) {
		List<FormData> formDataList = req.getPayload();
		if (CollectionUtils.isEmpty(formDataList)) {
			return ResponseEvent.response(Collections.emptyList());
		}

		Container form = formDataList.get(0).getContainer();
		Long formCtxtId = formDao.getFormCtxtId(form.getId(), "SpecimenEvent", -1L);
		for (FormData formData : formDataList) {
			Map<String, Object> appData = formData.getAppData();
			appData.put("formCtxtId", formCtxtId);
			appData.put("objectId", appData.get("id"));
		}

		return formSvc.saveBulkFormData(req);
	}
}