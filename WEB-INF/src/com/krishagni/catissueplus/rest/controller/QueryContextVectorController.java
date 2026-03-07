package com.krishagni.catissueplus.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.VectorContextResult;
import com.krishagni.catissueplus.core.de.events.VectorContextSearchOp;
import com.krishagni.catissueplus.core.de.events.VectorMetadataSyncOp;
import com.krishagni.catissueplus.core.de.services.QueryContextVectorService;

@Controller
@RequestMapping("/query-context")
public class QueryContextVectorController {
	@Autowired
	private QueryContextVectorService queryContextVectorSvc;

	@RequestMapping(method = RequestMethod.POST, value = "/sync-metadata")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Boolean syncMetadata(@RequestBody(required = false) VectorMetadataSyncOp op) {
		if (op == null) {
			op = new VectorMetadataSyncOp();
		}

		return response(queryContextVectorSvc.syncMetadata(RequestEvent.wrap(op)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/search")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public VectorContextResult search(
		@RequestParam("text") String text,
		@RequestParam(value = "cpId", required = false) Long cpId,
		@RequestParam(value = "cpGroupId", required = false) Long cpGroupId,
		@RequestParam(value = "maxResults", required = false, defaultValue = "25") Integer maxResults) {

		VectorContextSearchOp op = new VectorContextSearchOp();
		op.setText(text);
		op.setCpId(cpId);
		op.setCpGroupId(cpGroupId);
		op.setMaxResults(maxResults);
		return response(queryContextVectorSvc.search(RequestEvent.wrap(op)));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/metadata-changed")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Boolean metadataChanged() {
		queryContextVectorSvc.onMetadataChanged();
		return true;
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
