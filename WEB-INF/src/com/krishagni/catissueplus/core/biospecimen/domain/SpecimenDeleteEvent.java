package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Calendar;
import java.util.Date;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class SpecimenDeleteEvent extends BaseEntity {
    private Specimen specimen;

    private User user;

    private Date time;

    private String comments;

    private Boolean undo;

    public Specimen getSpecimen() {
        return specimen;
    }

    public void setSpecimen(Specimen specimen) {
        this.specimen = specimen;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Boolean getUndo() {
        return undo;
    }

    public void setUndo(Boolean undo) {
        this.undo = undo;
    }

    public static SpecimenDeleteEvent deleteEvent(Specimen specimen, String comments) {
        return deleteEvent(specimen, comments, false);
    }

    public static SpecimenDeleteEvent undeleteEvent(Specimen specimen, String comments) {
        return deleteEvent(specimen, comments, true);
    }

    private static SpecimenDeleteEvent deleteEvent(Specimen specimen, String comments, boolean undo) {
        SpecimenDeleteEvent event = new SpecimenDeleteEvent();
        event.setSpecimen(specimen);
        event.setUser(AuthUtil.getCurrentUser());
        event.setTime(Calendar.getInstance().getTime());
        event.setComments(comments);
        event.setUndo(undo);
        return event;
    }
}
