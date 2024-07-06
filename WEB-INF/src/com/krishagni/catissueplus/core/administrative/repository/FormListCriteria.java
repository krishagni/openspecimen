package com.krishagni.catissueplus.core.administrative.repository;

import java.util.List;
import java.util.Set;

import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class FormListCriteria extends AbstractListCriteria<FormListCriteria> {
	private List<Long> formIds;

	private List<String> names;

	private List<String> entityTypes;

	private Long userId;

	private List<Long> cpIds;

	private List<String> cps;

	private Boolean excludeSysForm;

	private Set<SiteCpPair> siteCps;

	private List<Long> entityIds;

	@Override
	public FormListCriteria self() {
		return this;
	}

	public List<Long> formIds() {
		return formIds;
	}

	public FormListCriteria formIds(List<Long> formIds) {
		this.formIds = formIds;
		return self();
	}

	public List<String> names() {
		return names;
	}

	public FormListCriteria names(List<String> names) {
		this.names = names;
		return self();
	}

	public List<String> entityTypes() {
		return entityTypes;
	}

	public FormListCriteria entityTypes(List<String> entityTypes) {
		this.entityTypes = entityTypes;
		return self();
	}

	public Long userId() {
		return userId;
	}

	public FormListCriteria userId(Long userId) {
		this.userId = userId;
		return self();
	}

	public List<Long> cpIds() {
		return cpIds;
	}

	public FormListCriteria cpIds(List<Long> cpIds) {
		this.cpIds = cpIds;
		return self();
	}

	public List<String> cps() {
		return cps;
	}

	public FormListCriteria cps(List<String> cps) {
		this.cps = cps;
		return self();
	}

	public Boolean excludeSysForm() {
		return excludeSysForm;
	}

	public FormListCriteria excludeSysForm(Boolean excludeSysForm) {
		this.excludeSysForm = excludeSysForm;
		return self();
	}

	public Set<SiteCpPair> siteCps() {
		return siteCps;
	}

	public FormListCriteria siteCps(Set<SiteCpPair> siteCps) {
		this.siteCps = siteCps;
		return self();
	}

	public List<Long> entityIds() {
		return entityIds;
	}

	public FormListCriteria entityIds(List<Long> entityIds) {
		this.entityIds = entityIds;
		return self();
	}
}
