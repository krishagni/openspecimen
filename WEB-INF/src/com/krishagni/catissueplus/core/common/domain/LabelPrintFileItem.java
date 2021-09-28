package com.krishagni.catissueplus.core.common.domain;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

public class LabelPrintFileItem extends BaseEntity {
    private LabelPrintJob job;

    private String content;

    public LabelPrintJob getJob() {
        return job;
    }

    public void setJob(LabelPrintJob job) {
        this.job = job;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
