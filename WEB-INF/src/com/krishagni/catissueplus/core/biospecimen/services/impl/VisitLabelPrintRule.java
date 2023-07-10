package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.util.Utility;

public class VisitLabelPrintRule extends LabelPrintRule {
	private List<CollectionProtocol> cps = new ArrayList<>();
	
	private Site visitSite;

	public void setCp(CollectionProtocol cp) {
		cps = new ArrayList<>();
		cps.add(cp);
	}

	public List<CollectionProtocol> getCps() {
		return cps;
	}

	public void setCps(List<CollectionProtocol> cps) {
		this.cps = cps;
	}


	public Site getVisitSite() {
		return visitSite;
	}

	public void setVisitSite(Site visitSite) {
		this.visitSite = visitSite;
	}

	public boolean isApplicableFor(Visit visit, User user, String ipAddr) {
		if (!super.isApplicableFor(user, ipAddr)) {
			return false;
		}

		if (CollectionUtils.isNotEmpty(cps) && cps.stream().noneMatch(cp -> cp.equals(visit.getCollectionProtocol()))) {
			return false;
		}

		if (visitSite != null && !visitSite.equals(visit.getSite())) {
			return false;
		}

		return true;
	}

	@Override
	protected Map<String, Object> getDefMap(boolean ufn) {
		Map<String, Object> ruleDef = new HashMap<>();
		ruleDef.put("cps", getCpList(ufn));
		ruleDef.put("visitSite", getSite(ufn, getVisitSite()));
		return ruleDef;
	}

	public String toString() {
		return super.toString() +
			", cp = " + getCpList(true) +
			", visit site = " + getSite(true, getVisitSite());
	}

	private List<String> getCpList(boolean ufn) {
		Function<CollectionProtocol, String> cpMapper = ufn ? CollectionProtocol::getShortTitle : (cp) -> cp.getId().toString();
		return Utility.nullSafeStream(getCps()).map(cpMapper).collect(Collectors.toList());
	}

	private String getSite(boolean ufn, Site site) {
		return site != null ? (ufn ? site.getName() : site.getId().toString()) : null;
	}
}
