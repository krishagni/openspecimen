package com.krishagni.catissueplus.core.audit.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.audit.services.impl.EntityRevisionListenerImpl;

@RevisionEntity(EntityRevisionListenerImpl.class)
public class Revision {
	
	@RevisionNumber
	private Long id;
	  
	@RevisionTimestamp
	private Date revtstmp;
	
	private User user;
	
	private String ipAddress;

	@ModifiedEntityNames
	private Set<String> entityNames;

	private Set<RevisionEntityRecord> entityRecords = new HashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public Date getRevtstmp() {
		return revtstmp;
	}

	public void setRevtstmp(Date revtstmp) {
		this.revtstmp = revtstmp;
	}

	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public Set<String> getEntityNames() {
		return entityNames;
	}

	public void setEntityNames(Set<String> entityNames) {
		this.entityNames = entityNames;
	}

	public Set<RevisionEntityRecord> getEntityRecords() {
		return entityRecords;
	}

	public void setEntityRecords(Set<RevisionEntityRecord> entityRecords) {
		this.entityRecords = entityRecords;
	}

	public void addEntityRecord(RevisionEntityRecord record) {
		RevisionEntityRecord existing = entityRecords.stream()
			.filter(er -> er.getEntityName().equals(record.getEntityName()) && er.getEntityId().equals(record.getEntityId()))
			.findFirst().orElse(null);
		if (existing == null) {
			entityRecords.add(record);
		} else if (record.getType() == 0 || record.getType() == 2) {
			existing.setType(record.getType());
		}
	}
}