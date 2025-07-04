package com.krishagni.catissueplus.rest.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.biospecimen.events.ServiceDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ServiceRateDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.ServiceListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.ServiceRateListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolService;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/cp-services")
public class ServicesController {
	@Autowired
	private CollectionProtocolService cpSvc;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ServiceDetail> getServices(
		@RequestParam(value = "cpId")
		Long cpId,

		@RequestParam(value = "code", required = false)
		String code,

		@RequestParam(value = "rateEffectiveOn", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		LocalDate rateEffectiveOn,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		ServiceListCriteria crit = new ServiceListCriteria()
			.cpId(cpId)
			.query(code)
			.rateEffectiveOn(rateEffectiveOn)
			.startAt(startAt)
			.maxResults(maxResults);
		return response(cpSvc.getServices(request(crit)));
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ServiceDetail createService(@RequestBody ServiceDetail input) {
		return response(cpSvc.createService(request(input)));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ServiceDetail updateService(@PathVariable("id") Long serviceId, @RequestBody ServiceDetail input) {
		input.setId(serviceId);
		return response(cpSvc.updateService(request(input)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ServiceDetail deleteService(@PathVariable("id") Long serviceId) {
		EntityQueryCriteria crit = new EntityQueryCriteria(serviceId);
		return response(cpSvc.deleteService(request(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/rates")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ServiceRateDetail> getServiceRates(
		@PathVariable("id")
		Long serviceId,

		@RequestParam(value = "startDate", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		LocalDate startDate,

		@RequestParam(value = "endDate", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		LocalDate endDate,

		@RequestParam(value = "effectiveDate", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		LocalDate effectiveDate) {

		ServiceRateListCriteria crit = new ServiceRateListCriteria()
			.serviceId(serviceId)
			.startDate(startDate)
			.endDate(endDate)
			.effectiveDate(effectiveDate);

		return response(cpSvc.getServiceRates(request(crit)));
	}


	@RequestMapping(method = RequestMethod.POST, value = "/{id}/rates")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ServiceRateDetail addServiceRate(@PathVariable("id") Long serviceId, @RequestBody ServiceRateDetail input) {
		input.setServiceId(serviceId);
		List<ServiceRateDetail> result = response(cpSvc.addServiceRates(request(Collections.singletonList(input))));
		return result.isEmpty() ? null : result.iterator().next();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/rates/{rateId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ServiceRateDetail updateServiceRate(
		@PathVariable("id")
		Long serviceId,

		@PathVariable("rateId")
		Long rateId,

		@RequestBody
		ServiceRateDetail input) {

		input.setId(rateId);
		input.setServiceId(serviceId);
		return response(cpSvc.updateServiceRate(request(input)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}/rates/{rateId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ServiceRateDetail deleteServiceRate(
		@PathVariable("id")
		Long serviceId,

		@PathVariable("rateId")
		Long rateId) {

		EntityQueryCriteria crit = new EntityQueryCriteria(rateId);
		crit.setParams(Collections.singletonMap("serviceId", serviceId));
		return response(cpSvc.deleteServiceRate(request(new EntityQueryCriteria(rateId))));
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
