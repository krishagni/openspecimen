package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import com.fasterxml.jackson.databind.ObjectMapper;


import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CpReportSettings;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;

@Configurable
public class CpReportTask implements ScheduledTask, Runnable {
	private static final LogUtil logger = LogUtil.getLogger(CpReportTask.class);

	@Autowired
	private DaoFactory daoFactory;

	private Long cpId;

	public CpReportTask() {

	}

	public CpReportTask(Long cpId) {
		this.cpId = cpId;
	}

	@Override
	public void doJob(ScheduledJobRun jobRun)
	throws Exception {
		CpReportSettings sysSettings = getSysRptSetting();
		CpReportSettings spmnCpSysSettings = getSpecimenCentricSysRptSetting();
		for (Long cpId : getAllCpIds()) {
			generateCpReport(cpId, sysSettings, spmnCpSysSettings);
		}
	}

	@Override
	public void run() {
		generateCpReport(cpId, null, null);
	}

	@PlusTransactional
	private List<Long> getAllCpIds() {
		return daoFactory.getCollectionProtocolDao().getAllCpIds();
	}

	@PlusTransactional
	private void generateCpReport(Long cpId, CpReportSettings sysSettings, CpReportSettings spmnCpSysSettings) {
		try {
			CpReportSettings cpSettings = daoFactory.getCpReportSettingsDao().getByCp(cpId);
			if (cpSettings != null && !cpSettings.isEnabled()) {
				return;
			}

			CpReportSettings sysSettingsToUse = null;
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (cp.isSpecimenCentric()) {
				sysSettingsToUse = spmnCpSysSettings != null ? spmnCpSysSettings : getSpecimenCentricSysRptSetting();
			}

			if (sysSettingsToUse == null) {
				sysSettingsToUse = sysSettings != null ? sysSettings : getSysRptSetting();
			}

			AuthUtil.setCurrentUser(daoFactory.getUserDao().getSystemUser());
			new CpReportGenerator().generateReport(cp, sysSettingsToUse, cpSettings);
		} catch (Exception e) {
			logger.error("Error generating report for collection protocol: " + cpId, e);
		}
	}

	private CpReportSettings getSysRptSetting() {
		CpReportSettings settings = getRptSetting(ConfigParams.SYS_RPT_SETTINGS);
		if (settings == null) {
			settings = new CpReportSettings();
		}

		return settings;
	}

	private CpReportSettings getSpecimenCentricSysRptSetting() {
		return getRptSetting(ConfigParams.SYS_SPMN_CP_RPT_SETTINGS);
	}

	private CpReportSettings getRptSetting(String configPropName) {
		try {
			String cfg = ConfigUtil.getInstance().getFileContent(ConfigParams.MODULE, configPropName, null);
			if (StringUtils.isBlank(cfg)) {
				return null;
			}

			return new ObjectMapper().readValue(cfg, CpReportSettings.class);
		} catch (Exception e) {
			logger.error("Error reading CP report settings: " + configPropName, e);
			throw new RuntimeException("Error reading CP report settings: " + configPropName, e);
		}
	}
}
