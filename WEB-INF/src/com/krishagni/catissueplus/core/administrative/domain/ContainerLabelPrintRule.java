package com.krishagni.catissueplus.core.administrative.domain;

import java.util.HashMap;
import java.util.Map;

import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;

public class ContainerLabelPrintRule extends LabelPrintRule {
	private Site site;

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	public boolean isApplicableFor(StorageContainer container, User user, String ipAddr) {
		if (!super.isApplicableFor(user, ipAddr)) {
			return false;
		}

		return getSite() == null || getSite().equals(container.getSite());
	}

	@Override
	protected Map<String, Object> getDefMap(boolean ufn) {
		Map<String, Object> ruleDef = new HashMap<>();
		ruleDef.put("site", getSite() != null ? (ufn ? getSite().getName() : getSite().getId().toString()) : null);
		return ruleDef;
	}

	public String toString() {
		return super.toString() + ", site = " + getSite().getName();
	}
}
