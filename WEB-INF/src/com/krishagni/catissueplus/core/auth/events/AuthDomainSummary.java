package com.krishagni.catissueplus.core.auth.events;

import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.auth.domain.AuthDomain;

public class AuthDomainSummary {
	private Long id;
	
	private String name;

	private String type;

	private boolean allowLogins = true;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isAllowLogins() {
		return allowLogins;
	}

	public void setAllowLogins(boolean allowLogins) {
		this.allowLogins = allowLogins;
	}

	public static AuthDomainSummary from(AuthDomain domain) {
		AuthDomainSummary summary = new AuthDomainSummary();
		summary.setId(domain.getId());
		summary.setName(domain.getName());
		summary.setType(domain.getAuthProvider().getAuthType());
		summary.setAllowLogins(domain.isAllowLogins());
		return summary;
	}
	
	public static List<AuthDomainSummary> from(List<AuthDomain> domains) {
		return domains.stream().map(AuthDomainSummary::from).collect(Collectors.toList());
	}
}
