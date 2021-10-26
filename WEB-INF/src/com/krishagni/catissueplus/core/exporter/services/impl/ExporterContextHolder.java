package com.krishagni.catissueplus.core.exporter.services.impl;

public class ExporterContextHolder {
	private static ExporterContextHolder instance = new ExporterContextHolder();

	//
	// Thread specific contextual information
	// Stores boolean indicating whether current thread is
	// doing export operation or not.
	//
	private static ThreadLocal<Boolean> ctx = ThreadLocal.withInitial(() -> false);

	private ExporterContextHolder() {
	}


	public static ExporterContextHolder getInstance() {
		return instance;
	}

	public void newContext() {
		ctx.set(true);
	}

	public void clearContext() {
		ctx.set(false);
	}

	public boolean isExportOp() {
		return ctx.get();
	}
}
