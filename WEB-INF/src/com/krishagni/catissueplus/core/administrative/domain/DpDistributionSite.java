package com.krishagni.catissueplus.core.administrative.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.proxy.HibernateProxyHelper;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

@Audited
@AuditTable(value = "OS_DIST_PROTOCOL_SITES_AUD")
public class DpDistributionSite extends BaseEntity {
	private DistributionProtocol distributionProtocol;
	
	private Institute institute;
	
	private Site site;
	
	public DistributionProtocol getDistributionProtocol() {
		return distributionProtocol;
	}
	
	public void setDistributionProtocol(DistributionProtocol distributionProtocol) {
		this.distributionProtocol = distributionProtocol;
	}
	
	public Institute getInstitute() {
		return institute;
	}
	
	public void setInstitute(Institute institute) {
		this.institute = institute;
	}
	
	public Site getSite() {
		return site;
	}
	
	public void setSite(Site site) {
		this.site = site;
	}
	
	@Override
	public int hashCode() {
		int prime = 31;
		if (getDistributionProtocol() == null && getInstitute() == null && getSite() == null) {
			return super.hashCode();
		}

		return prime +
			(getDistributionProtocol() == null ? 0 : getDistributionProtocol().hashCode()) +
			(getInstitute() == null ? 0 : getInstitute().hashCode()) +
			(getSite() == null ? 0 : getSite().hashCode());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null	|| 
			getClass() != HibernateProxyHelper.getClassWithoutInitializingProxy(obj)) {
			return false;
		}
		
		DpDistributionSite other = (DpDistributionSite) obj;

		if (ObjectUtils.notEqual(getDistributionProtocol(), other.getDistributionProtocol())) {
			return false;
		}
		
		if (ObjectUtils.notEqual(getInstitute(), other.getInstitute())) {
			return false;
		}

		if (ObjectUtils.notEqual(getSite(), other.getSite())) {
			return false;
		}
		
		return true;
	}
	
	public static Map<String, List<String>> getInstituteSitesMap(Collection<DpDistributionSite> distSites) {
		Map<String, List<String>> instSitesMap = new HashMap<String, List<String>>();
		
		for (DpDistributionSite distSite : distSites) {
			String instituteName = distSite.getInstitute().getName();
			List<String> siteNames = instSitesMap.get(instituteName);
			if (siteNames == null) {
				siteNames = new ArrayList<String>();
				instSitesMap.put(instituteName, siteNames);
			}
			
			if (distSite.getSite() != null) {
				siteNames.add(distSite.getSite().getName());
			}
		}
		
		return instSitesMap;
	}
}
