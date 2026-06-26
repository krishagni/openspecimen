package com.krishagni.catissueplus.core.biospecimen;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig.Workflow;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class WorkflowConfigUtil {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static boolean areSame(Map<String, Workflow> first, Map<String, Workflow> second) {
		if (first == second) {
			return true;
		} else if (first == null || second == null || !first.keySet().equals(second.keySet())) {
			return false;
		}

		for (String name : first.keySet()) {
			JsonNode firstWorkflow = MAPPER.valueToTree(first.get(name));
			JsonNode secondWorkflow = MAPPER.valueToTree(second.get(name));
			if (!areSame(firstWorkflow, secondWorkflow)) {
				return false;
			}
		}

		return true;
	}

	public static boolean areSame(String first, String second) {
		try {
			CpWorkflowConfig firstCfg = new CpWorkflowConfig();
			firstCfg.setWorkflowsJson(first != null ? first : "[]");

			CpWorkflowConfig secondCfg = new CpWorkflowConfig();
			secondCfg.setWorkflowsJson(second != null ? second : "[]");
			return areSame(firstCfg.getWorkflows(), secondCfg.getWorkflows());
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	private static boolean areSame(JsonNode first, JsonNode second) {
		if (first == second) {
			return true;
		} else if (first == null || second == null) {
			return false;
		} else if (first.isNumber() && second.isNumber()) {
			BigDecimal firstNumber = first.decimalValue();
			BigDecimal secondNumber = second.decimalValue();
			return firstNumber.compareTo(secondNumber) == 0;
		} else if (first.isObject() && second.isObject()) {
			if (first.size() != second.size()) {
				return false;
			}

			Iterator<Map.Entry<String, JsonNode>> fields = first.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();
				if (!second.has(field.getKey()) || !areSame(field.getValue(), second.get(field.getKey()))) {
					return false;
				}
			}

			return true;
		} else if (first.isArray() && second.isArray()) {
			if (first.size() != second.size()) {
				return false;
			}

			for (int i = 0; i < first.size(); ++i) {
				if (!areSame(first.get(i), second.get(i))) {
					return false;
				}
			}

			return true;
		}

		return first.equals(second);
	}

	private WorkflowConfigUtil() {
	}
}
