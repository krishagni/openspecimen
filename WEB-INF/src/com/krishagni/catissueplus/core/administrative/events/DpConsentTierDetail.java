package com.krishagni.catissueplus.core.administrative.events;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.DpConsentTier;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DpConsentTierDetail {
	private Long id;

	private Long dpId;
	
	private String dpTitle;
	
	private String dpShortTitle;

	private Long statementId;

	private String statementCode;

	private String statement;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getDpId() {
		return dpId;
	}

	public void setDpId(Long dpId) {
		this.dpId = dpId;
	}

	public String getDpTitle() {
		return dpTitle;
	}

	public void setDpTitle(String dpTitle) {
		this.dpTitle = dpTitle;
	}

	public String getDpShortTitle() {
		return dpShortTitle;
	}

	public void setDpShortTitle(String dpShortTitle) {
		this.dpShortTitle = dpShortTitle;
	}

	public Long getStatementId() {
		return statementId;
	}

	public void setStatementId(Long statementId) {
		this.statementId = statementId;
	}

	public String getStatementCode() {
		return statementCode;
	}

	public void setStatementCode(String statementCode) {
		this.statementCode = statementCode;
	}
	
	public String getStatement() {
		return statement;
	}

	public void setStatement(String statement) {
		this.statement = statement;
	}

	public static DpConsentTierDetail from(DpConsentTier ct) {
		DpConsentTierDetail detail = new DpConsentTierDetail();
		detail.setId(ct.getId());
		detail.setStatementId(ct.getStatement().getId());
		detail.setStatementCode(ct.getStatement().getCode());
		detail.setStatement(ct.getStatement().getStatement());
		detail.setDpId(ct.getDistributionProtocol().getId());
		detail.setDpTitle(ct.getDistributionProtocol().getTitle());
		detail.setDpShortTitle(ct.getDistributionProtocol().getShortTitle());
		return detail;
	}

	public static List<DpConsentTierDetail> from(DistributionProtocol dp) {
		return dp.getConsentTiers().stream().map(DpConsentTierDetail::from).collect(Collectors.toList());
	}
}
