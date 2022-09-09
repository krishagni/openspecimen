package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;

public class CpWorkflowCfgDetail {
	private Long cpId;
	
	private String shortTitle;
	
	private Map<String, WorkflowDetail> workflows = new HashMap<>();

	@JsonIgnore
	private boolean patch;

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public String getShortTitle() {
		return shortTitle;
	}

	public void setShortTitle(String shortTitle) {
		this.shortTitle = shortTitle;
	}

	public Map<String, WorkflowDetail> getWorkflows() {
		return workflows;
	}

	public void setWorkflows(Map<String, WorkflowDetail> workflows) {
		this.workflows = workflows;
	}

	@JsonIgnore
	public boolean isPatch() {
		return patch;
	}

	public void setPatch(boolean patch) {
		this.patch = patch;
	}

	public static CpWorkflowCfgDetail from(CpWorkflowConfig cfg) {
		CpWorkflowCfgDetail result = new CpWorkflowCfgDetail();
		result.setWorkflows(WorkflowDetail.from(cfg.getWorkflows().values()));
		if (cfg.getCp() != null) {
			result.setCpId(cfg.getCp().getId());
			result.setShortTitle(cfg.getCp().getShortTitle());
		}

		return result;
	}
}
