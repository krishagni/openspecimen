package com.krishagni.catissueplus.core.de.events;

public class QueryAuditLogDigest {
	private long total;

	private long successful;

	private long failed;

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public long getSuccessful() {
		return successful;
	}

	public void setSuccessful(long successful) {
		this.successful = successful;
	}

	public long getFailed() {
		return failed;
	}

	public void setFailed(long failed) {
		this.failed = failed;
	}
}
