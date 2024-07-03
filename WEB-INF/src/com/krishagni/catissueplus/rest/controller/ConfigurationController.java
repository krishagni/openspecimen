package com.krishagni.catissueplus.rest.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PluginManager;
import com.krishagni.catissueplus.core.common.events.ConfigSettingDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

@Controller
@RequestMapping("/config-settings")
public class ConfigurationController {

	private static final LogUtil logger = LogUtil.getLogger(ConfigurationController.class);
	
	@Autowired
	private ConfigurationService cfgSvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public List<ConfigSettingDetail> getConfigSettings(
		@RequestParam(value = "module", required = false)
		String moduleName,

		@RequestParam(value = "property", required = false)
		String propertyName) {

		if (StringUtils.isNotBlank(moduleName) && StringUtils.isNotBlank(propertyName)) {
			ConfigSettingDetail setting = response(cfgSvc.getSetting(request(Pair.make(moduleName, propertyName))));
			return Collections.singletonList(setting);
		} else {
			return response(cfgSvc.getSettings(request(moduleName)));
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = "history")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ConfigSettingDetail> getConfigSettingHistory(
		@RequestParam(value = "module")
		String module,

		@RequestParam(value = "property")
		String property) {
		return response(cfgSvc.getSettingHistory(request(Pair.make(module, property))));
	}

	@RequestMapping(method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public ConfigSettingDetail saveConfigSetting(@RequestBody ConfigSettingDetail detail) {
		return response(cfgSvc.saveSetting(request(detail)));
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/locale")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> getLocaleSettings() {
		return cfgSvc.getLocaleSettings();
	}	
	
	@RequestMapping(method = RequestMethod.GET, value="/app-props")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> getAppProps() {
		return cfgSvc.getAppProps();
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/welcome-video")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, String> getWelcomeVideoSettings() {
		return cfgSvc.getWelcomeVideoSettings();
	}

	@RequestMapping(method = RequestMethod.GET, value="/files")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void downloadSettingFile(
		@RequestParam(value = "module")
		String moduleName,

		@RequestParam(value = "property")
		String propertyName,

		HttpServletResponse httpResp) throws IOException {

		cfgSvc.downloadSettingFile(moduleName, propertyName, httpResp);
	}

	@RequestMapping(method = RequestMethod.POST, value="/files")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public String uploadSettingFile(
		@RequestParam(value = "module")
		String module,

		@RequestParam(value = "property")
		String property,

		@PathVariable("file") MultipartFile file)
	throws IOException {
		InputStream in = null;
		try {
			in = file.getInputStream();

			Map<String, Object> props = new HashMap<>();
			props.put("module", module);
			props.put("property", property);

			FileDetail detail = new FileDetail();
			detail.setFilename(file.getOriginalFilename());
			detail.setFileIn(in);
			detail.setObjectProps(props);

			ResponseEvent<String> resp = cfgSvc.uploadSettingFile(request(detail));
			resp.throwErrorIfUnsuccessful();
			return resp.getPayload();
		} finally {
			IOUtils.closeQuietly(in);
		}
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/password")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, String> getPasswordSettings() {
		return cfgSvc.getPasswordSettings();
	}

	@RequestMapping(method = RequestMethod.GET, value="/deployment-site-assets")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, String> getDeploymentSiteAssets() {
		return cfgSvc.getDeploymentSiteAssets();
	}

	@RequestMapping(method = RequestMethod.GET, value="/deployment-site-logo")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void getDeploymentSiteLogo(HttpServletResponse httpResp) {
		FileDetail detail = cfgSvc.getFileDetail("common", "deployment_site_logo");
		if (detail == null || detail.getFileIn() == null) {
			return;
		}

		Utility.sendToClient(httpResp, detail.getFilename(), detail.getContentType(), detail.getFileIn());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/i18n-messages")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> getMessages()
	throws Exception {
		Resource[] resources = OpenSpecimenAppCtxProvider.getAppCtx().getResources("ui-app/i18n/*.json");
		Map<String, Object> result = new HashMap<>();
		for (Resource resource : resources) {
			Map<String, Object> langI18n = getI18nMap(resource);
			merge(langI18n, result);
		}

		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		for (String plugin : PluginManager.getInstance().getPluginNames()) {
			try {
				Resource[] pluginResources = resolver.getResources("META-INF/resources/ui/" + plugin + "/i18n/**.json");
				for (Resource resource : pluginResources) {
					Map<String, Object> langI18n = getI18nMap(resource);
					merge(langI18n, result);
				}
			} catch (FileNotFoundException fnfe) {
				logger.info("No i18n resources in the plugin: " + plugin + ". " + fnfe.getMessage());
			}
		}

		return result;
	}

	private Map<String, Object> getI18nMap(Resource resource)
	throws Exception {
		Map<String, Object> langI18n = Utility.toMap(resource.getInputStream());
		String filename = resource.getFilename();
		String lang = filename.substring(0, filename.lastIndexOf("."));
		return Collections.singletonMap(lang, langI18n);
	}

	private Map<String, Object> merge(Map<String, Object> source, Map<String, Object> dest) {
		for (Map.Entry<String, Object> kv : source.entrySet()) {
			Object destObj = dest.get(kv.getKey());
			if (!(kv.getValue() instanceof Map) || !(destObj instanceof Map)) {
				Object value =  kv.getValue();
				if (value instanceof Map) {
					value = new HashMap<>((Map<String, Object>) value);
				}

				dest.put(kv.getKey(), value);
			} else {
				merge((Map<String, Object>) kv.getValue(), (Map<String, Object>) destObj);
			}
		}

		return dest;
	}

	private static <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<T>(payload);
	}

	private static <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
