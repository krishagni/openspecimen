package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenLabelPrintRule extends LabelPrintRule {
	private List<CollectionProtocol> cps = new ArrayList<>();

	private Site visitSite;

	private List<PermissibleValue> specimenClasses;
	
	private List<PermissibleValue> specimenTypes;

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

	public List<PermissibleValue> getSpecimenClasses() {
		return specimenClasses;
	}

	public void setSpecimenClasses(List<PermissibleValue> specimenClasses) {
		this.specimenClasses = specimenClasses;
	}

	public List<PermissibleValue> getSpecimenTypes() {
		return specimenTypes;
	}

	public void setSpecimenTypes(List<PermissibleValue> specimenTypes) {
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

		if (CollectionUtils.isNotEmpty(specimenClasses) || CollectionUtils.isNotEmpty(specimenTypes)) {
			// either one of - specimen classes or specimen types is configured
			if (specimenClasses == null || !specimenClasses.contains(specimen.getSpecimenClass())) {
				// input specimen class is not present in configured classes, if any
				if (specimenTypes == null || !specimenTypes.contains(specimen.getSpecimenType())) {
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
		ruleDef.put("specimenClasses", getClassesList(ufn));
		ruleDef.put("specimenTypes", getTypesList(ufn));
		ruleDef.put("lineage", getLineage());
		return ruleDef;
	}

	public String toString() {
		return new StringBuilder(super.toString())
			.append(", cp = ").append(getCpList(true))
			.append(", lineage = ").append(getLineage())
			.append(", visit site = ").append(getSite(true, getVisitSite()))
			.append(", specimen classes = ").append(getClassesList(true))
			.append(", specimen types = ").append(getTypesList(true))
			.toString();
	}

	private boolean isValidLineage(String lineage) {
		return isWildCard(lineage) || Specimen.isValidLineage(lineage);
	}

	private List<String> getCpList(boolean ufn) {
		Function<CollectionProtocol, String> cpMapper = ufn ? CollectionProtocol::getShortTitle : (cp) -> cp.getId().toString();
		return Utility.nullSafeStream(getCps()).map(cpMapper).collect(Collectors.toList());
	}

	private List<String> getClassesList(boolean ufn) {
		return Utility.nullSafeStream(getSpecimenClasses()).map(getPvMapper(ufn)).collect(Collectors.toList());
	}

	private List<String> getTypesList(boolean ufn) {
		return Utility.nullSafeStream(getSpecimenTypes()).map(getPvMapper(ufn)).collect(Collectors.toList());
	}

	private String getSite(boolean ufn, Site site) {
		return site != null ? (ufn ? site.getName() : site.getId().toString()) : null;
	}

	private Function<PermissibleValue, String> getPvMapper(boolean ufn) {
		return ufn ? (pv) -> pv.getValue() : (pv) -> pv.getId().toString();
	}
}
