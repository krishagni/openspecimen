package com.krishagni.rbac.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.envers.Audited;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

@Audited
public class Role extends BaseEntity {
	private String name;

	private String description;
	
	private Set<RoleAccessControl> acl = new HashSet<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<RoleAccessControl> getAcl() {
		return acl;
	}

	public void setAcl(Set<RoleAccessControl> acl) {
		this.acl = acl;
	}

	public void addAcl(RoleAccessControl rac) {
		RoleAccessControl newAcl = new RoleAccessControl();
		newAcl.setRole(this);
		newAcl.setResource(rac.getResource());
		newAcl.setOperations(rac.getOperations().stream().map(op -> {
			ResourceInstanceOp newOp = new ResourceInstanceOp();
			newOp.setAccessControl(newAcl);
			newOp.setOperation(op.getOperation());
			return newOp;
		}).collect(Collectors.toSet()));

		acl.add(newAcl);
	}

	public void updateRole(Role other) {
		setName(other.getName());
		setDescription(other.getDescription());
		updateAcl(other);
	}

	private void updateAcl(Role other) {
		Map<Resource, RoleAccessControl> existingAclMap = new HashMap<>();
		for (RoleAccessControl rac : getAcl()) {
			existingAclMap.put(rac.getResource(), rac);
		}

		for (RoleAccessControl otherRac : other.getAcl()) {
			RoleAccessControl existing = existingAclMap.remove(otherRac.getResource());
			if (existing == null) {
				addAcl(otherRac);
			} else {
				existing.update(otherRac);
			}
		}

		getAcl().removeAll(existingAclMap.values());
	}
}
