package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.HashMap;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;

public class VisitLabelPrintRule extends LabelPrintRule {
	private String cpShortTitle;
	
	private String visitSite;

	public String getCpShortTitle() {
		return cpShortTitle;
	}
	
	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public String getVisitSite() {
		return visitSite;
	}

	public void setVisitSite(String visitSite) {
		this.visitSite = visitSite;
	}

	public boolean isApplicableFor(Visit visit, User user, String ipAddr) {
		if (!super.isApplicableFor(user, ipAddr)) {
			return false;
		}
		
		if (!isWildCard(cpShortTitle) && !visit.getCollectionProtocol().getShortTitle().equals(cpShortTitle)) {
			return false;
		}
		
		if (!isWildCard(visitSite) && (visit.getSite() == null || !visit.getSite().getName().equals(visitSite))) {
			return false;
		}

		return true;
	}

	@Override
	protected Map<String, Object> getDefMap(boolean ufn) {
		Map<String, Object> ruleDef = new HashMap<>();
		ruleDef.put("cpShortTitle", getCpShortTitle());
		ruleDef.put("visitSite", getVisitSite());
		return ruleDef;
	}

	public String toString() {
		return new StringBuilder(super.toString())
			.append(", cp = ").append(getCpShortTitle())
			.append(", visit site = ").append(getVisitSite())
			.toString();
	}
}
