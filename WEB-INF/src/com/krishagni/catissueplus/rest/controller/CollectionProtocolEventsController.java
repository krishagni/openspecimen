package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

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

import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CopyCpeOpDetail;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolService;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/collection-protocol-events")
public class CollectionProtocolEventsController {

	@Autowired
	private CollectionProtocolService cpSvc;
	
	@Autowired
	private HttpServletRequest httpServletRequest;	

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<CollectionProtocolEventDetail> getEvents(
		@RequestParam(value = "cpId", required = false)
		Long cpId,

		@RequestParam(value = "cpShortTitle", required = false)
		String cpShortTitle,

		@RequestParam(value = "includeClosedEvents", required = false, defaultValue = "true")
		boolean includeClosedEvents) {

		EntityQueryCriteria crit = new EntityQueryCriteria(cpId);
		crit.setName(cpShortTitle);
		crit.setParams(Collections.singletonMap("includeClosedEvents", includeClosedEvents));

		ResponseEvent<List<CollectionProtocolEventDetail>> resp = cpSvc.getProtocolEvents(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public CollectionProtocolEventDetail getEvent(@PathVariable("id") Long cpeId) {
		ResponseEvent<CollectionProtocolEventDetail> resp = cpSvc.getProtocolEvent(getRequest(cpeId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolEventDetail createEvent(@RequestBody CollectionProtocolEventDetail event) {
		ResponseEvent<CollectionProtocolEventDetail> resp = cpSvc.addEvent(getRequest(event));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="/{eventId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolEventDetail updateEvent(
			@PathVariable("eventId") Long eventId,
			@RequestBody CollectionProtocolEventDetail event) {
		
		event.setId(eventId);
		
		ResponseEvent<CollectionProtocolEventDetail> resp = cpSvc.updateEvent(getRequest(event));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}	

	@RequestMapping(method = RequestMethod.POST, value="/{eventId}/copy")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public CollectionProtocolEventDetail copyEvent(
			@PathVariable("eventId") Long eventId,
			@RequestBody CollectionProtocolEventDetail event) {
		
		CopyCpeOpDetail opDetail = new CopyCpeOpDetail();
		opDetail.setCpe(event);
		opDetail.setEventId(eventId);
		
		ResponseEvent<CollectionProtocolEventDetail> resp = cpSvc.copyEvent(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();		
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/{eventId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public CollectionProtocolEventDetail deleteEvent(@PathVariable("eventId") Long eventId) {
		ResponseEvent<CollectionProtocolEventDetail> resp = cpSvc.deleteEvent(new RequestEvent<Long>(eventId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);
	}	
}
