package com.krishagni.catissueplus.core.config;

import java.io.File;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusRenameFilter;

@Configuration
public class MetricsConfig {

	@Bean
	public MeterFilter prometheusRenameFilter() {
		return new PrometheusRenameFilter();
	}

	@Bean
	public PrometheusMeterRegistry meterRegistry() {
		PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

		new ProcessorMetrics().bindTo(registry);
		new FileDescriptorMetrics().bindTo(registry);
		new UptimeMetrics().bindTo(registry);

		// 2. JVM & Thread Stats (Number of threads doing work)
		new ClassLoaderMetrics().bindTo(registry);
		new JvmThreadMetrics().bindTo(registry);
		new JvmMemoryMetrics().bindTo(registry);
		new JvmGcMetrics().bindTo(registry);

		// 3. Disk I/O (Disk space and basic usage)
		new DiskSpaceMetrics(new File("/")).bindTo(registry);

		new Log4j2Metrics().bindTo(registry);
		return registry;
	}
}
