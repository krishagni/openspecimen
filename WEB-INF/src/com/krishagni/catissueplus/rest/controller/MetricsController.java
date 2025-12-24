package com.krishagni.catissueplus.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

@Controller
@RequestMapping("/metrics")
public class MetricsController {

	@Autowired
	private PrometheusMeterRegistry registry;

	@RequestMapping(method = RequestMethod.GET, produces = "text/plain")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public String getMetrics() {
		return registry.scrape();
	}
}
