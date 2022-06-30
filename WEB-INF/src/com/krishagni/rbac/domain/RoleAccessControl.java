package com.krishagni.rbac.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.Audited;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

@Audited
public class RoleAccessControl extends BaseEntity {
	private Role role;
	
	private Resource resource;
	
	private Set<ResourceInstanceOp> operations = new HashSet<>();

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public Set<ResourceInstanceOp> getOperations() {
		return operations;
	}

	public void setOperations(Set<ResourceInstanceOp> operations) {
		this.operations = operations;
	}

	public void update(RoleAccessControl other) {
		Map<Operation, ResourceInstanceOp> existingOpsMap = new HashMap<>();
		for (ResourceInstanceOp op : getOperations()) {
			existingOpsMap.put(op.getOperation(), op);
		}

		Set<ResourceInstanceOp> newOps = new HashSet<>();
		for (ResourceInstanceOp otherOp : other.getOperations()) {
			ResourceInstanceOp existingOp = existingOpsMap.remove(otherOp.getOperation());
			if (existingOp == null) {
				ResourceInstanceOp newOp = new ResourceInstanceOp();
				newOp.setAccessControl(this);
				newOp.setOperation(otherOp.getOperation());
				newOps.add(newOp);
			}
		}

		getOperations().removeAll(existingOpsMap.values());
		getOperations().addAll(newOps);
	}
}
