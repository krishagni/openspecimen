package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CpGroupWorkflowCfgDetail {
	private Long groupId;

	private String groupName;

	private Map<String, WorkflowDetail> workflows = new HashMap<>();

	private Boolean importWfs;

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public Map<String, WorkflowDetail> getWorkflows() {
		return workflows;
	}

	public void setWorkflows(Map<String, WorkflowDetail> workflows) {
		this.workflows = workflows;
	}

	public Boolean getImportWfs() {
		return importWfs;
	}

	public void setImportWfs(Boolean importWfs) {
		this.importWfs = importWfs;
	}

	public static CpGroupWorkflowCfgDetail from(CollectionProtocolGroup group) {
		CpGroupWorkflowCfgDetail result = new CpGroupWorkflowCfgDetail();
		result.setGroupId(group.getId());
		result.setGroupName(group.getName());
		result.setWorkflows(WorkflowDetail.from(group.getWorkflows().values()));
		return result;
	}
}
