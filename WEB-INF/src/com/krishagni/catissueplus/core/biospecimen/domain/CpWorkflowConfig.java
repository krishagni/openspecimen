package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.envers.Audited;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

@Audited
public class CpWorkflowConfig extends BaseEntity {
	private CollectionProtocol cp;
	
	private Map<String, Workflow> workflows = new HashMap<>();
		
	public CollectionProtocol getCp() {
		return cp;
	}

	public void setCp(CollectionProtocol cp) {
		this.cp = cp;
	}

	public Map<String, Workflow> getWorkflows() {
		return workflows;
	}

	public void setWorkflows(Map<String, Workflow> workflows) {
		this.workflows = workflows;
	}
	
	public String getWorkflowsJson() {
		try {
			return getWriteMapper().writeValueAsString(workflows.values());
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}		
	}
	
	public void setWorkflowsJson(String json) {
		try {
			Workflow[] workflows = getReadMapper().readValue(json, Workflow[].class);
			this.workflows.clear();
			for (Workflow workflow : workflows) {
				this.workflows.put(workflow.getName(), workflow);
			}
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	protected Set<String> getAuditStringInclusionProps() {
		return Arrays.stream(new String[] { "cp", "workflowsJson" }).collect(Collectors.toSet());
	}

	private ObjectMapper getReadMapper() {
		return new ObjectMapper();
	}
	
	private ObjectMapper getWriteMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibilityChecker(
			mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(Visibility.ANY)
				.withGetterVisibility(Visibility.NONE)
				.withSetterVisibility(Visibility.NONE)
				.withCreatorVisibility(Visibility.NONE));
		return mapper;		
	}
	
	public static class Workflow {
		private String name;
		
		private String view;
		
		private String ctrl;
		
		private Map<String, Object> data = new HashMap<>();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getView() {
			return view;
		}

		public void setView(String view) {
			this.view = view;
		}

		public String getCtrl() {
			return ctrl;
		}

		public void setCtrl(String ctrl) {
			this.ctrl = ctrl;
		}

		public Map<String, Object> getData() {
			return data;
		}

		public void setData(Map<String, Object> data) {
			this.data = data;
		}		
	}
}
