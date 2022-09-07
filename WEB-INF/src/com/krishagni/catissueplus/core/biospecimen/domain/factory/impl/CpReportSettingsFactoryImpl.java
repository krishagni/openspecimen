package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CpReportSettings;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpReportSettingsFactory;
import com.krishagni.catissueplus.core.biospecimen.events.CpReportSettingsDetail;
import com.krishagni.catissueplus.core.common.domain.ReportSettings;
import com.krishagni.catissueplus.core.common.domain.factory.ReportSettingErrorCode;
import com.krishagni.catissueplus.core.common.events.ReportSettingsDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;

public class CpReportSettingsFactoryImpl implements CpReportSettingsFactory {
	private DaoFactory daoFactory;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	@Override
	public CpReportSettings createSettings(CpReportSettingsDetail detail) {
		CpReportSettings settings = new CpReportSettings();

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		setCp(detail, settings, ose);
		setEnabled(detail, settings, ose);
		setDataQuery(detail, settings, ose);
		setDataCfg(detail, settings, ose);
		setMetricCfg(detail, settings, ose);
		setEmailTmpl(detail, settings, ose);
		setRecipients(detail, settings, ose);
		setActivityStatus(detail, settings, ose);
		ose.checkAndThrow();

		return settings;
	}

	private void setCp(CpReportSettingsDetail detail, CpReportSettings settings, OpenSpecimenException ose) {
		if (detail.getCp() == null) {
			ose.addError(CpErrorCode.REQUIRED);
			return;
		}

		CollectionProtocol cp = null;

		Long cpId = detail.getCp().getId();
		String shortTitle = detail.getCp().getShortTitle();
		Object key = null;
		if (cpId != null) {
			cp = daoFactory.getCollectionProtocolDao().getById(cpId);
			key = cpId;
		} else if (StringUtils.isNotBlank(shortTitle)) {
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(shortTitle);
			key = shortTitle;
		} else {
			ose.addError(CpErrorCode.REQUIRED);
			return;
		}

		if (cp == null) {
			ose.addError(CpErrorCode.NOT_FOUND, key);
			return;
		}

		settings.setCp(cp);
	}

	private void setEnabled(ReportSettingsDetail detail, ReportSettings settings, OpenSpecimenException ose) {
		settings.setEnabled(detail.isEnabled());
	}

	private void setDataQuery(ReportSettingsDetail detail, ReportSettings settings, OpenSpecimenException ose) {
		if (detail.getDataQuery() == null || detail.getDataQuery().getId() == null) {
			return;
		}

		SavedQuery query = deDaoFactory.getSavedQueryDao().getQuery(detail.getDataQuery().getId());
		if (query == null) {
			ose.addError(SavedQueryErrorCode.NOT_FOUND, detail.getDataQuery().getId());
			return;
		}

		settings.setDataQuery(query);
	}

	private void setDataCfg(ReportSettingsDetail detail, ReportSettings settings, OpenSpecimenException ose) {
		if (settings.getDataQuery() != null) {
			return;
		}

		Map<String, Object> cfg = detail.getDataCfg();
		if (cfg == null) {
			return;
		}

		if (!cfg.containsKey("type")) {
			ose.addError(ReportSettingErrorCode.DATA_SOURCE_REQ);
			return;
		}

		try {
			String type = (String)cfg.get("type");
			if (!type.equals("AQL")) {
				Class.forName((String)cfg.get("type"));
			}

			settings.setDataCfg(detail.getDataCfg());
		} catch (Throwable t) {
			ose.addError(ReportSettingErrorCode.INVALID_SOURCE, cfg.get("type"));
		}
	}

	private void setMetricCfg(ReportSettingsDetail detail, ReportSettings settings, OpenSpecimenException ose) {
		Map<String, Map<String, Object>> metricsCfg = detail.getMetricsCfg();
		if (metricsCfg == null) {
			return;
		}

		for (Map.Entry<String, Map<String, Object>> metricCfg : metricsCfg.entrySet()) {
			Map<String, Object> cfg = metricCfg.getValue();
			if (cfg == null) {
				continue;
			}

			try {
				String type = (String)cfg.get("type");
				if (StringUtils.isBlank(type)) {
					cfg.put("type", (type = "AQL"));
				}

				if (!type.equals("AQL")) {
					Class.forName(type);
				}
			} catch (Exception e) {
				ose.addError(ReportSettingErrorCode.INVALID_SOURCE, cfg.get("type"));
			}
		}

		settings.setMetricsCfg(detail.getMetricsCfg());
	}

	private void setEmailTmpl(ReportSettingsDetail detail, ReportSettings settings, OpenSpecimenException ose) {
		settings.setEmailTmpl(detail.getEmailTmpl());
	}

	private void setRecipients(ReportSettingsDetail detail, ReportSettings settings, OpenSpecimenException ose) {
		if (CollectionUtils.isEmpty(detail.getRecipients())) {
			return;
		}

		Set<User> recipients = new HashSet<>();
		for (UserSummary rcpt : detail.getRecipients()) {
			if (rcpt == null) {
				continue;
			}

			User user = null;
			Object key = null;
			if (rcpt.getId() != null) {
				user = daoFactory.getUserDao().getById(rcpt.getId());
				key = rcpt.getId();
			} else if (StringUtils.isNotBlank(rcpt.getEmailAddress())) {
				user = daoFactory.getUserDao().getUserByEmailAddress(rcpt.getEmailAddress());
				key = rcpt.getEmailAddress();
			}

			if (user == null) {
				if (key != null) {
					ose.addError(UserErrorCode.NOT_FOUND, key);
				}

				continue;
			}

			recipients.add(user);
		}

		settings.setRecipients(recipients);
	}

	private void setActivityStatus(ReportSettingsDetail detail, ReportSettings settings, OpenSpecimenException ose) {
		String activityStatus = detail.getActivityStatus();
		if (StringUtils.isBlank(activityStatus)) {
			activityStatus = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		}

		if (!Status.isValidActivityStatus(activityStatus)) {
			ose.addError(ActivityStatusErrorCode.INVALID, activityStatus);
			return;
		}

		settings.setActivityStatus(activityStatus);
	}
}
