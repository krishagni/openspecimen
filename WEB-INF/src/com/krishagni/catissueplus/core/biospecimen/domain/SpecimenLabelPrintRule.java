package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenLabelPrintRule extends LabelPrintRule {
	private List<CollectionProtocol> cps = new ArrayList<>();

	private Site visitSite;

	private List<String> specimenClasses;
	
	private List<String> specimenTypes;

	private String lineage;

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

	public List<String> getSpecimenClasses() {
		return specimenClasses;
	}

	public void setSpecimenClasses(List<String> specimenClasses) {
		this.specimenClasses = specimenClasses;
	}

	public List<String> getSpecimenTypes() {
		return specimenTypes;
	}

	public void setSpecimenTypes(List<String> specimenTypes) {
		this.specimenTypes = specimenTypes;
	}

	public String getLineage() {
		return lineage;
	}

	public void setLineage(String lineage) {
		if (!isValidLineage(lineage)) {
			throw new IllegalArgumentException("Invalid lineage: " + lineage + " Expected: New, Derived or Aliquot");
		}

		this.lineage = lineage;
	}

	public boolean isApplicableFor(Specimen specimen, User user, String ipAddr) {
		if (!super.isApplicableFor(user, ipAddr)) {
			return false;
		}

		if (CollectionUtils.isNotEmpty(cps) && cps.stream().noneMatch(cp -> cp.equals(specimen.getCollectionProtocol()))) {
			return false;
		}

		Visit visit = specimen.getVisit();
		if (visitSite != null && !visitSite.equals(visit.getSite())) {
			return false;
		}

		String spmnClass = specimen.getSpecimenClass().getValue();
		String spmnType  = specimen.getSpecimenType().getValue();
		if (CollectionUtils.isNotEmpty(specimenClasses) || CollectionUtils.isNotEmpty(specimenTypes)) {
			// either one of - specimen classes or specimen types is configured
			if (specimenClasses == null || !specimenClasses.contains(spmnClass)) {
				// input specimen class is not present in configured classes, if any
				if (specimenTypes == null || !specimenTypes.contains(spmnType)) {
					//input specimen type is not present in configured types, if any
					return false;
				}
			}
		}

		if (StringUtils.isNotBlank(lineage) && !lineage.equals(specimen.getLineage())) {
			return false;
		}
		
		return true;
	}

	@Override
	protected Map<String, Object> getDefMap(boolean ufn) {
		Map<String, Object> ruleDef = new HashMap<>();

		ruleDef.put("cps", getCpList(ufn));
		ruleDef.put("visitSite", getSite(ufn, getVisitSite()));
		ruleDef.put("specimenClasses", getClassesList());
		ruleDef.put("specimenTypes", getTypesList());
		ruleDef.put("lineage", getLineage());
		return ruleDef;
	}

	public String toString() {
		return new StringBuilder(super.toString())
			.append(", cp = ").append(getCpList(true))
			.append(", lineage = ").append(getLineage())
			.append(", visit site = ").append(getSite(true, getVisitSite()))
			.append(", specimen classes = ").append(getClassesList())
			.append(", specimen types = ").append(getTypesList())
			.toString();
	}

	private boolean isValidLineage(String lineage) {
		return isWildCard(lineage) || Specimen.isValidLineage(lineage);
	}

	private List<String> getCpList(boolean ufn) {
		Function<CollectionProtocol, String> cpMapper = ufn ? (cp) -> cp.getShortTitle() : (cp) -> cp.getId().toString();
		return Utility.nullSafeStream(getCps()).map(cpMapper).collect(Collectors.toList());
	}

	private List<String> getClassesList() {
		return getSpecimenClasses();
	}

	private List<String> getTypesList() {
		return getSpecimenTypes();
	}

	private String getSite(boolean ufn, Site site) {
		return site != null ? (ufn ? site.getName() : site.getId().toString()) : null;
	}
}
