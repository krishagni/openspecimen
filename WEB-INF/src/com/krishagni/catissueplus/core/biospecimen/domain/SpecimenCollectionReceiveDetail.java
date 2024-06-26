package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Date;

import com.krishagni.catissueplus.core.administrative.domain.User;

public class SpecimenCollectionReceiveDetail {
	private Specimen specimen;

	private Long collEventId;

	private String collContainer;

	private String collProcedure;

	private User collector;

	private Date collTime;

	private String collComments;

	private Long recvEventId;

	private String recvQuality;

	private User receiver;

	private Date recvTime;

	private String recvComments;

	public Long getId() {
		return specimen == null ? null : specimen.getId();
	}

	public void setId(Long id) {
	}

	public Specimen getSpecimen() {
		return specimen;
	}

	public void setSpecimen(Specimen specimen) {
		this.specimen = specimen;
	}

	public Long getCollEventId() {
		return collEventId;
	}

	public void setCollEventId(Long collEventId) {
		this.collEventId = collEventId;
	}

	public String getCollContainer() {
		return collContainer;
	}

	public void setCollContainer(String collContainer) {
		this.collContainer = collContainer;
	}

	public String getCollProcedure() {
		return collProcedure;
	}

	public void setCollProcedure(String collProcedure) {
		this.collProcedure = collProcedure;
	}

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
		return recvEventId;
	}

	public void setRecvEventId(Long recvEventId) {
		this.recvEventId = recvEventId;
	}

	public String getRecvQuality() {
		return recvQuality;
	}

	public void setRecvQuality(String recvQuality) {
		this.recvQuality = recvQuality;
	}

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
