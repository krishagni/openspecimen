package com.krishagni.catissueplus.core.common.events;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.common.domain.UpgradeLog;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpgradeLogDetail {
	private Long id;

	private String version;

	private Date upgradeDate;

	private String upgradedBy;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Date getUpgradeDate() {
		return upgradeDate;
	}

	public void setUpgradeDate(Date upgradeDate) {
		this.upgradeDate = upgradeDate;
	}

	public String getUpgradedBy() {
		return upgradedBy;
	}

	public void setUpgradedBy(String upgradedBy) {
		this.upgradedBy = upgradedBy;
	}

	public static UpgradeLogDetail from(UpgradeLog upgradeLog) {
		UpgradeLogDetail detail = new UpgradeLogDetail();
		detail.setId(upgradeLog.getId());
		detail.setVersion(upgradeLog.getVersion());
		detail.setUpgradeDate(upgradeLog.getUpgradeDate());
		detail.setUpgradedBy(upgradeLog.getUpgradedBy());
		return detail;
	}

	public static List<UpgradeLogDetail> from(List<UpgradeLog> logs) {
		return logs.stream().map(UpgradeLogDetail::from).collect(Collectors.toList());
	}
}