package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Date;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.User;

@Audited
public class SpecimenCollectionReceiveDetail implements Identifiable<Long> {
	private Specimen primarySpecimen;

	private PermissibleValue collContainer;

	private PermissibleValue collProcedure;

	private User collector;

	private Date collTime;

	private String collComments;

	private PermissibleValue recvQuality;

	private User receiver;

	private Date recvTime;

	private String recvComments;

	public Long getId() {
		return primarySpecimen == null ? null : primarySpecimen.getId();
	}

	public void setId(Long id) {
	}

	public Specimen getPrimarySpecimen() {
		return primarySpecimen;
	}

	public void setPrimarySpecimen(Specimen primarySpecimen) {
		this.primarySpecimen = primarySpecimen;
	}

	public Long getCollEventId() {
		return getId();
	}

	public void setCollEventId(Long collEventId) {
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getCollContainer() {
		return collContainer;
	}

	public void setCollContainer(PermissibleValue collContainer) {
		this.collContainer = collContainer;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getCollProcedure() {
		return collProcedure;
	}

	public void setCollProcedure(PermissibleValue collProcedure) {
		this.collProcedure = collProcedure;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public User getCollector() {
		return collector;
	}

	public void setCollector(User collector) {
		this.collector = collector;
	}

	public Date getCollTime() {
		return collTime;
	}

	public void setCollTime(Date collTime) {
		this.collTime = collTime;
	}

	public String getCollComments() {
		return collComments;
	}

	public void setCollComments(String collComments) {
		this.collComments = collComments;
	}

	public Long getRecvEventId() {
		return getId();
	}

	public void setRecvEventId(Long recvEventId) {
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getRecvQuality() {
		return recvQuality;
	}

	public void setRecvQuality(PermissibleValue recvQuality) {
		this.recvQuality = recvQuality;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public User getReceiver() {
		return receiver;
	}

	public void setReceiver(User receiver) {
		this.receiver = receiver;
	}

	public Date getRecvTime() {
		return recvTime;
	}

	public void setRecvTime(Date recvTime) {
		this.recvTime = recvTime;
	}

	public String getRecvComments() {
		return recvComments;
	}

	public void setRecvComments(String recvComments) {
		this.recvComments = recvComments;
	}
}
