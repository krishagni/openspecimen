package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListsFolderDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListsFolderSummary;
import com.krishagni.catissueplus.core.biospecimen.events.UpdateFolderCartsOp;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsFoldersCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenListService;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/specimen-list-folders")
public class SpecimenListFoldersController {
	@Autowired
	private SpecimenListService specimenListSvc;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenListsFolderSummary> getFolders(
		@RequestParam(value = "name", required = false)
		String name,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false")
		boolean includeStats
	) {
		SpecimenListsFoldersCriteria crit = new SpecimenListsFoldersCriteria()
			.query(name)
			.startAt(startAt)
			.maxResults(maxResults)
			.includeStat(includeStats);
		return ResponseEvent.unwrap(specimenListSvc.getFolders(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getFoldersCount(@RequestParam(value = "name", required = false) String name) {
		SpecimenListsFoldersCriteria crit = new SpecimenListsFoldersCriteria().query(name);
		Long count = ResponseEvent.unwrap(specimenListSvc.getFoldersCount(RequestEvent.wrap(crit)));
		return Collections.singletonMap("count", count);
	}

	@RequestMapping(method = RequestMethod.GET, value="/{folderId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListsFolderDetail getFolder(@PathVariable("folderId") Long folderId) {
		return ResponseEvent.unwrap(specimenListSvc.getFolder(RequestEvent.wrap(new EntityQueryCriteria(folderId))));
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListsFolderDetail createFolder(@RequestBody SpecimenListsFolderDetail input) {
		return ResponseEvent.unwrap(specimenListSvc.createFolder(RequestEvent.wrap(input)));
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{folderId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListsFolderDetail updateFolder(@PathVariable Long folderId, @RequestBody SpecimenListsFolderDetail input) {
		input.setId(folderId);
		return ResponseEvent.unwrap(specimenListSvc.updateFolder(RequestEvent.wrap(input)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/{folderId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListsFolderDetail deleteFolder(@PathVariable Long folderId) {
		return ResponseEvent.unwrap(specimenListSvc.deleteFolder(RequestEvent.wrap(new EntityQueryCriteria(folderId))));
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{folderId}/carts")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> updateFolderCarts(
		@PathVariable("folderId")
		Long folderId,

		@RequestParam(value = "operation", required = false, defaultValue = "ADD")
		String operation,

		@RequestBody
		List<Long> cartIds) {

		UpdateFolderCartsOp op = new UpdateFolderCartsOp();
		op.setFolderId(folderId);
		op.setCartIds(cartIds);
		op.setOp(UpdateFolderCartsOp.Operation.valueOf(operation));
		return Collections.singletonMap("count", ResponseEvent.unwrap(specimenListSvc.updateFolderCarts(RequestEvent.wrap(op))));
	}
}
