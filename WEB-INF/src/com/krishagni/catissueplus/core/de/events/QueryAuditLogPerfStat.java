package com.krishagni.catissueplus.core.de.events;

public class QueryAuditLogPerfStat {
	private String name;

	private long runtime;

	private String runtimeInSecs;

	public QueryAuditLogPerfStat(String name, long runtime, String runtimeInSecs) {
		this.name = name;
		this.runtime = runtime;
		this.runtimeInSecs = runtimeInSecs;
	}

	public String getName() {
		return name;
	}

	public long getRuntime() {
		return runtime;
	}

	public String getRuntimeInSecs() {
		return runtimeInSecs;
	}

	public static final String P50 = "p50";

	public static final String P90 = "p90";

	public static final String P95 = "p95";

	public static final String P99 = "p99";
}
