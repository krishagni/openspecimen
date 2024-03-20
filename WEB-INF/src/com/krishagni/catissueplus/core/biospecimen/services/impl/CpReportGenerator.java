package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CpReportSettings;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolDetail;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolService;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolService.DataSource;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.EmailService;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.de.events.ExecuteQueryEventOp;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.catissueplus.core.de.events.QueryExecResult;
import com.krishagni.catissueplus.core.de.services.QueryService;

import edu.common.dynamicextensions.query.WideRowMode;

@Configurable
public class CpReportGenerator {
	private static final LogUtil logger = LogUtil.getLogger(CpReportGenerator.class);

	private static final String RPTS_DIR = "cp-reports";

	@Autowired
	private EmailService emailSvc;

	@Autowired
	private QueryService querySvc;

	public void generateReport(CollectionProtocol cp, CpReportSettings sysSettings, CpReportSettings rptSettings)
	throws Exception {
		if (sysSettings == null) {
			sysSettings = new CpReportSettings();
		}

		if (rptSettings == null) {
			rptSettings = new CpReportSettings();
			rptSettings.setCp(cp);
		}

		String emailTmpl = rptSettings.getEmailTmpl();
		if (StringUtils.isBlank(emailTmpl)) {
			emailTmpl = sysSettings.getEmailTmpl();
		}

		String emailTmplKey = sysSettings.getEmailTmplKey();
		if (StringUtils.isBlank(emailTmpl) && StringUtils.isBlank(emailTmplKey)) {
			return;
		}

		List<String> rcptEmailIds = rptSettings.getRecipients().stream()
			.map(User::getEmailAddress)
			.collect(Collectors.toList());

		if (rcptEmailIds.isEmpty()) {
			rcptEmailIds = cp.getCoordinators().stream()
				.map(User::getEmailAddress)
				.collect(Collectors.toList());
			rcptEmailIds.add(0, cp.getPrincipalInvestigator().getEmailAddress());
		}

		Map<String, Object> emailCtxt = new HashMap<>();
		emailCtxt.putAll(getMetrics(cp, sysSettings, rptSettings));
		emailCtxt.put("dataFile", getDataFile(cp, sysSettings, rptSettings));
		emailCtxt.put("cp", CollectionProtocolDetail.from(cp)); // introduced in v4.0
		emailCtxt.put("cpId", cp.getId()); // retaining this for backward compatibility
		emailCtxt.put("cpShortTitle", cp.getShortTitle()); // retaining this for backward compatibility
		emailCtxt.put("$subject", new String[] { cp.getShortTitle() });

		if (StringUtils.isNotBlank(emailTmpl)) {
			emailSvc.sendEmail("default_cp_report", emailTmpl, rcptEmailIds.toArray(new String[0]), emailCtxt);
		} else {
			emailSvc.sendEmail(emailTmplKey, rcptEmailIds.toArray(new String[0]), emailCtxt);
		}
	}

	public File getDataFile(Long cpId, String fileId) {
		return new File(getReportsDir(), cpId + "-" + fileId);
	}

	private Map<String, Object> getMetrics(CollectionProtocol cp, CpReportSettings sysSettings, CpReportSettings rptSettings)
	throws Exception {
		Map<String, Map<String, Object>> metricsCfg = rptSettings.getMetricsCfg();
		if (metricsCfg == null || metricsCfg.isEmpty()) {
			metricsCfg = sysSettings.getMetricsCfg();
		}

		if (metricsCfg == null || metricsCfg.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<String, Map<String, Object>> metricCfg : metricsCfg.entrySet()) {
			result.put(metricCfg.getKey(), getMetric(cp, metricCfg.getValue()));
		}

		return result;
	}

	private Object getMetric(CollectionProtocol cp, Map<String, Object> metricCfg)
	throws Exception {
		String type = (String) metricCfg.get("type");

		CollectionProtocolService.DataSource source;
		if (StringUtils.isBlank(type) || type.equalsIgnoreCase("AQL")) {
			source = new CpAqlDataSource();
		} else {
			source = (CollectionProtocolService.DataSource) BeanUtils.instantiateClass(Class.forName(type));
		}

		return source.getMetric(cp, metricCfg);
	}

	private String getDataFile(CollectionProtocol cp, CpReportSettings sysSettings, CpReportSettings cpSettings)
	throws Exception {
		File dataFile = null;

		String aql = getDataAql(cpSettings);
		if (StringUtils.isNotBlank(aql)) {
			dataFile = new CpAqlDataSource().getDataFile(cp, Collections.singletonMap("aql", aql));
		} else if (cpSettings.getDataCfg() != null && !cpSettings.getDataCfg().isEmpty()) {
			String type = (String)cpSettings.getDataCfg().get("type");
			if (type.equals("AQL")) {
				dataFile = new CpAqlDataSource().getDataFile(cp, cpSettings.getDataCfg());
			} else {
				DataSource ds = (DataSource) BeanUtils.instantiateClass(Class.forName(type));
				dataFile = ds.getDataFile(cp, cpSettings.getDataCfg());
			}
		} else {
			aql = getDataAql(sysSettings);
			if (StringUtils.isNotBlank(aql)) {
				dataFile = new CpAqlDataSource().getDataFile(cp, Collections.singletonMap("aql", aql));
			}
		}

		if (dataFile != null) {
			return moveFileToReportsDir(cp, dataFile);
		}

		return null;
	}

	private String getDataAql(CpReportSettings settings) {
		String aql = null;
		if (settings.getDataQuery() != null) {
			aql = settings.getDataQuery().getAql();
		} else if (StringUtils.isNotBlank(settings.getDataQueryAql())) {
			aql = settings.getDataQueryAql();
		}

		return aql;
	}

	private String moveFileToReportsDir(CollectionProtocol cp, File file) {
		String extn = ".csv.zip";
		int extnStartIdx = file.getName().lastIndexOf('.');
		if (extnStartIdx != -1) {
			extn = file.getName().substring(extnStartIdx);
		}

		String fileId = UUID.randomUUID() + extn;
		file.renameTo(new File(getReportsDir(), cp.getId() + "-" + fileId));
		return fileId;
	}

	private File getReportsDir() {
		File dir = new File(ConfigUtil.getInstance().getDataDir() + File.separator + RPTS_DIR);
		if (!dir.exists()) {
			synchronized (this) {
				dir.mkdirs();
			}
		}

		return dir;
	}

	private class CpAqlDataSource implements CollectionProtocolService.DataSource {
		@Override
		public Object getMetric(CollectionProtocol cp, Map<String, Object> input) {
			String aql = (String)input.get("aql");
			if (StringUtils.isBlank(aql)) {
				return null;
			}

			ExecuteQueryEventOp op = new ExecuteQueryEventOp();
			op.setRunType("Data");
			op.setCpId(cp.getId());
			op.setWideRowMode(WideRowMode.OFF.name());
			op.setAql(aql);
			op.setDisableAccessChecks(true);

			String drivingForm = (String)input.get("drivingForm");
			op.setDrivingForm(StringUtils.isBlank(drivingForm) ? "Participant" : drivingForm);

			ResponseEvent<QueryExecResult> resp = querySvc.executeQuery(new RequestEvent<>(op));
			return resp.isSuccessful() ? resp.getPayload() : null;
		}

		@Override
		public File getDataFile(CollectionProtocol cp, Map<String, Object> input) {
			ExecuteQueryEventOp op = new ExecuteQueryEventOp();
			op.setCpId(cp.getId());
			op.setDrivingForm("Participant");
			op.setAql((String)input.get("aql"));
			op.setRunType("Export");
			op.setWideRowMode(WideRowMode.DEEP.name());

			ResponseEvent<QueryDataExportResult> resp = querySvc.exportQueryData(new RequestEvent<>(op));
			resp.throwErrorIfUnsuccessful();

			QueryDataExportResult result = resp.getPayload();
			String dataFile = result.getDataFile();
			if (!result.isCompleted()) {
				try {
					result.getPromise().get();
				} catch (Exception e) {
					logger.error("Error retrieving CP report data file", e);
					dataFile = null;
				}
			}

			if (StringUtils.isBlank(dataFile)) {
				return null;
			}

			ResponseEvent<File> fileResp = querySvc.getExportDataFile(new RequestEvent<>(dataFile));
			fileResp.throwErrorIfUnsuccessful();
			return fileResp.getPayload();
		}
	}
}
