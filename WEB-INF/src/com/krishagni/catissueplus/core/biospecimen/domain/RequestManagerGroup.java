package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.krishagni.catissueplus.core.administrative.domain.UserGroup;

@Audited
@AuditTable(value = "OS_REQ_MGR_GROUPS_AUD")
public class RequestManagerGroup extends BaseEntity {
	private String name;

	private UserGroup userGroup;

	private Set<CollectionProtocol> cps = new HashSet<>();

	private Set<CollectionProtocolGroup> cpgs = new HashSet<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UserGroup getUserGroup() {
		return userGroup;
	}

	public void setUserGroup(UserGroup userGroup) {
		this.userGroup = userGroup;
	}

	@NotAudited
	public Set<CollectionProtocol> getCps() {
		return cps;
	}

	public void setCps(Set<CollectionProtocol> cps) {
		this.cps = cps;
	}

	@NotAudited
	public Set<CollectionProtocolGroup> getCpgs() {
		return cpgs;
	}

	public void setCpgs(Set<CollectionProtocolGroup> cpgs) {
		this.cpgs = cpgs;
	}
}
