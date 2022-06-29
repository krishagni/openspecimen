package com.krishagni.catissueplus.core.common.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;

@Audited
public class ReportSettings extends BaseEntity {
	private boolean enabled = true;

	private SavedQuery dataQuery;

	private String dataQueryAql;

	private Map<String, Object> dataCfg;

	private Map<String, Map<String, Object>> metricsCfg;

	private String emailTmpl;

	private String emailTmplKey;

	private Set<User> recipients = new HashSet<>();

	private String activityStatus;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public SavedQuery getDataQuery() {
		return dataQuery;
	}

	public void setDataQuery(SavedQuery dataQuery) {
		this.dataQuery = dataQuery;
	}

	public String getDataQueryAql() {
		return dataQueryAql;
	}

	public void setDataQueryAql(String dataQueryAql) {
		this.dataQueryAql = dataQueryAql;
	}

	public Map<String, Object> getDataCfg() {
		return dataCfg;
	}

	public void setDataCfg(Map<String, Object> dataCfg) {
		this.dataCfg = dataCfg;
	}

	public Map<String, Map<String, Object>> getMetricsCfg() {
		return metricsCfg;
	}

	public void setMetricsCfg(Map<String, Map<String, Object>> metricsCfg) {
		this.metricsCfg = metricsCfg;
	}

	public String getEmailTmpl() {
		return emailTmpl;
	}

	public void setEmailTmpl(String emailTmpl) {
		this.emailTmpl = emailTmpl;
	}

	public String getEmailTmplKey() {
		return emailTmplKey;
	}

	public void setEmailTmplKey(String emailTmplKey) {
		this.emailTmplKey = emailTmplKey;
	}

	public Set<User> getRecipients() {
		return recipients;
	}

	public void setRecipients(Set<User> recipients) {
		this.recipients = recipients;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public String getConfigJson() {
		Map<String, Object> cfg = new HashMap<>();
		cfg.put("metrics", getMetricsCfg());
		cfg.put("data", getDataCfg());

		try {
			return new ObjectMapper().writeValueAsString(cfg);
		} catch (Exception e) {
			throw new RuntimeException("Error serialising report config to JSON", e);
		}
	}

	public void setConfigJson(String configJson) {
		try {
			TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
			Map<String, Object> config = new ObjectMapper().readValue(configJson, typeRef);

			this.dataCfg = (Map<String, Object>)config.get("data");
			this.metricsCfg = (Map<String, Map<String, Object>>)config.get("metrics");
		} catch (Exception e) {
			throw new RuntimeException("Error deserialising report config JSON to Java object", e);
		}
	}

	public void update(ReportSettings other) {
		setEnabled(other.isEnabled());
		setDataQuery(other.getDataQuery());
		setDataCfg(other.getDataCfg());
		setMetricsCfg(other.getMetricsCfg());
		setEmailTmpl(other.getEmailTmpl());
		setRecipients(other.getRecipients());
		setActivityStatus(other.getActivityStatus());
	}

	public void delete() {
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}
}
