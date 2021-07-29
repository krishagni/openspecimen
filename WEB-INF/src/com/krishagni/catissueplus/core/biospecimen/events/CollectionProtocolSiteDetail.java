package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolSite;
import com.krishagni.catissueplus.core.common.util.Utility;

@JsonFilter("withoutId")
public class CollectionProtocolSiteDetail {
	private Long id;

	private Long siteId;

	private String siteName;
	
	private String code;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getSiteId() {
		return siteId;
	}

	public void setSiteId(Long siteId) {
		this.siteId = siteId;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
	public static CollectionProtocolSiteDetail from(CollectionProtocolSite cpSite) {
		CollectionProtocolSiteDetail detail = new CollectionProtocolSiteDetail();
		detail.setId(cpSite.getId());
		detail.setSiteId(cpSite.getSite().getId());
		detail.setSiteName(cpSite.getSite().getName());
		detail.setCode(cpSite.getCode());
		return detail;
	}
	
	public static List<CollectionProtocolSiteDetail> from(Collection<CollectionProtocolSite> cpSites) {
		return Utility.nullSafeStream(cpSites).map(CollectionProtocolSiteDetail::from).collect(Collectors.toList());
	}
}
