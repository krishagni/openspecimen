package com.krishagni.catissueplus.core.common.domain;

import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;

public class LabelPrintJobSavedEvent extends OpenSpecimenEvent<LabelPrintJob> {
    public LabelPrintJobSavedEvent(LabelPrintJob job) {
        super(null, job);
    }
}
