package com.krishagni.catissueplus.core.importer.services.impl;

public class ImporterContextHolder {
    private static ImporterContextHolder instance = new ImporterContextHolder();

    //
    // Thread specific contextual information
    // Stores boolean indicating whether current thread is
    // doing import operation or not.
    //
    private static ThreadLocal<Boolean> ctx = ThreadLocal.withInitial(() -> false);

    private ImporterContextHolder() {
    }

    public static ImporterContextHolder getInstance() {
        return instance;
    }

    public void newContext() {
        ctx.set(true);
    }

    public void clearContext() {
        ctx.set(false);
    }

    public boolean isImportOp() {
        return ctx.get();
    }
}
