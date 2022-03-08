package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import com.krishagni.catissueplus.core.administrative.domain.User;

public class SpecimenPooledEvent extends BaseEntity {
	private Specimen pooledSpecimen;

	private User user;

	private Date time;

	private String comments;

	private Set<Specimen> poolItems = new LinkedHashSet<>();

	public Specimen getPooledSpecimen() {
		return pooledSpecimen;
	}

	public void setPooledSpecimen(Specimen pooledSpecimen) {
		this.pooledSpecimen = pooledSpecimen;
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

	public Set<Specimen> getPoolItems() {
		return poolItems;
	}

	public void setPoolItems(Set<Specimen> poolItems) {
		this.poolItems = poolItems;
	}

	public void addPoolItem(Specimen spmn) {
		getPoolItems().add(spmn);
	}
}
