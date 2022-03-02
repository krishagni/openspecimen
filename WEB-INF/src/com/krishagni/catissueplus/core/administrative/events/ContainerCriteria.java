package com.krishagni.catissueplus.core.administrative.events;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;

import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionRule;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;

public class ContainerCriteria {
	private Long cpId;

	private SpecimenDetail specimen;

	private int minFreePositions;

	private Date reservedLaterThan;

	private int numContainers;

	private Set<SiteCpPair> siteCps;

	private String ruleName;

	private Map<String, Object> ruleParams = new HashMap<>();

	private ContainerSelectionRule rule;

	public ContainerCriteria() {

	}

	public Long cpId() {
		return cpId;
	}

	public Long getCpId() {
		return cpId;
	}

	public ContainerCriteria cpId(Long cpId) {
		this.cpId = cpId;
		return this;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public SpecimenDetail specimen() {
		return specimen;
	}

	public void setSpecimen(SpecimenDetail specimen) {
		this.specimen = specimen;
	}

	public ContainerCriteria specimen(SpecimenDetail specimen) {
		this.specimen = specimen;
		return this;
	}

	public int minFreePositions() {
		return minFreePositions;
	}

	public void setMinFreePositions(int minFreePositions) {
		this.minFreePositions = minFreePositions;
	}

	public ContainerCriteria minFreePositions(int minFreePositions) {
		this.minFreePositions = minFreePositions;
		return this;
	}

	public int getRequiredPositions(Boolean aliquotsInSameContainer) {
		return BooleanUtils.isTrue(aliquotsInSameContainer) && minFreePositions() > 1 ? minFreePositions() : 1;
	}

	public Date reservedLaterThan() {
		return reservedLaterThan;
	}

	public ContainerCriteria reservedLaterThan(Date reservedLaterThan) {
		this.reservedLaterThan = reservedLaterThan;
		return this;
	}

	public int numContainers() {
		return numContainers <= 0 ? 1 : numContainers;
	}

	public ContainerCriteria numContainers(int numContainers) {
		this.numContainers = numContainers;
		return this;
	}

	public Set<SiteCpPair> siteCps() {
		return siteCps;
	}

	public ContainerCriteria siteCps(Set<SiteCpPair> siteCps) {
		this.siteCps = siteCps;
		return this;
	}

	public String ruleName() {
		return ruleName;
	}

	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}

	public ContainerCriteria ruleName(String restrictionType) {
		this.ruleName = restrictionType;
		return this;
	}

	public Map<String, Object> ruleParams() {
		return ruleParams;
	}

	public void setRuleParams(Map<String, Object> ruleParams) {
		this.ruleParams = ruleParams;
	}

	public ContainerCriteria ruleParams(Map<String, Object> restrictionInput) {
		this.ruleParams = restrictionInput;
		return this;
	}

	public ContainerSelectionRule rule() {
		return rule;
	}

	public ContainerCriteria rule(ContainerSelectionRule restriction) {
		this.rule = restriction;
		return this;
	}

	public String key() {
		StringBuilder key = new StringBuilder()
			.append(specimen.getCpId()).append("#")
			.append(specimen.getSpecimenClass()).append("#")
			.append(specimen.getType()).append("#")
			.append(ruleName);

		for (Map.Entry<String, Object> kv : ruleParams.entrySet()) {
			key.append("#").append(kv.getKey()).append(kv.getValue().toString());
		}

		return key.toString();
	}
}
