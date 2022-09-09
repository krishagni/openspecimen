package com.krishagni.catissueplus.core.biospecimen;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig.Workflow;
import com.krishagni.catissueplus.core.common.service.ConfigChangeListener;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

@Configurable
public class WorkflowUtil {
	private static final String DEF_SYS_WORKFLOWS = "/com/krishagni/catissueplus/core/biospecimen/system-workflows.json";

	private static WorkflowUtil instance = null;

	@Autowired
	private ConfigurationService cfgSvc;

	private CpWorkflowConfig sysWorkflows;

	private CpWorkflowConfig defSysWorkflows;

	public static WorkflowUtil getInstance() {
		if (instance == null || instance.cfgSvc == null) {
			//
			// Defensive check instance.cfgSvc == null is added to handle
			// scenarios when app is incorrectly wired
			//
			instance = new WorkflowUtil();
			instance.listenForConfigChanges();
		}

		return instance;
	}

	public CpWorkflowConfig getSysWorkflows() {
		return getSysWorkflows0();
	}

	public Workflow getSysWorkflow(String name) {
		CpWorkflowConfig sysWorkflows = getSysWorkflows0();
		if (sysWorkflows == null) {
			return null;
		}

		return sysWorkflows.getWorkflows().get(name);
	}

	public <T> T getSysWorkflow(String name, Class<T> klass) {
		Workflow workflow = getSysWorkflow(name);
		if (workflow != null) {
			return Utility.mapToObject(workflow.getData(), klass);
		}

		return null;
	}

	private CpWorkflowConfig getSysWorkflows0() {
		if (sysWorkflows != null) {
			return sysWorkflows;
		}

		synchronized (this) {
			if (sysWorkflows == null) {
				String config = ConfigUtil.getInstance().getFileContent(
					ConfigParams.MODULE, ConfigParams.SYS_WORKFLOWS, null);

				sysWorkflows = new CpWorkflowConfig();
				if (StringUtils.isNotBlank(config)) {
					sysWorkflows.setWorkflowsJson(config);
				}

				mergeWithDefSysWorkflows(sysWorkflows.getWorkflows());
			}
		}

		return sysWorkflows;
	}

	private void mergeWithDefSysWorkflows(Map<String, Workflow> configuredWfs) {
		if (defSysWorkflows == null) {
			InputStream in = null;
			try {
				in = getClass().getResourceAsStream(DEF_SYS_WORKFLOWS);
				if (in == null) {
					throw new RuntimeException("Default system workflow missing from the package");
				}

				String workflowJson = IOUtils.toString(in, Charset.defaultCharset());
				defSysWorkflows = new CpWorkflowConfig();
				defSysWorkflows.setWorkflowsJson(workflowJson);
			} catch (Exception e) {
				throw new RuntimeException("Error loading default system workflows", e);
			} finally {
				IOUtils.closeQuietly(in);
			}
		}

		defSysWorkflows.getWorkflows().forEach(
			(name, wf) -> {
				if (!configuredWfs.containsKey(name)) {
					configuredWfs.put(name, wf);
				}
			}
		);
	}

	private void listenForConfigChanges() {
		if (cfgSvc == null) {
			return;
		}

		instance.cfgSvc.registerChangeListener(ConfigParams.MODULE, new ConfigChangeListener() {
			@Override
			public void onConfigChange(String name, String value) {
				if (StringUtils.isBlank(name) || name.equals(ConfigParams.SYS_WORKFLOWS)) {
					instance.sysWorkflows = null;
				}
			}
		});
	}
}
