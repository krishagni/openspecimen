package com.krishagni.catissueplus.rest.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.servlet.HandlerMapping;

import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

@Configuration
public class HttpRequestsMetricsFilter {
	private static final String API_PATH = "rest/ng";

	@Autowired
	private PrometheusMeterRegistry registry;

	@Bean(name = "httpMetricsFilter")
	public GenericFilterBean filterBean() {
		return new GenericFilterBean() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
				// Start a sample to measure duration
				Timer.Sample sample = Timer.start(registry);

				try {
					chain.doFilter(request, response);
				} finally {
					HttpServletRequest httpReq = (HttpServletRequest) request;
					HttpServletResponse httpResp   = (HttpServletResponse) response;
					String apiPath = (String) httpReq.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
					if (StringUtils.isNotBlank(apiPath)) {
						sample.stop(
							registry.timer(
								"http.server.requests",
								"uri", apiPath,
								"method", httpReq.getMethod(),
								"status", String.valueOf(httpResp.getStatus())
							)
						);
					}
				}
			}
		};
	}
}
