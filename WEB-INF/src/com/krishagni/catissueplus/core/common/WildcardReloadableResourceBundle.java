package com.krishagni.catissueplus.core.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.VfsResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ResourceUtils;


import com.krishagni.catissueplus.core.common.util.LogUtil;

public class WildcardReloadableResourceBundle extends ReloadableResourceBundleMessageSource {
	private static final LogUtil logger = LogUtil.getLogger(WildcardReloadableResourceBundle.class);

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private String pluginDir;

	public WildcardReloadableResourceBundle() {

	}

	public WildcardReloadableResourceBundle(String pluginDir) {
		if (StringUtils.isNotBlank(pluginDir)) {
			try {
				this.pluginDir = new File(pluginDir).toURI().toURL().getFile();
			} catch (IOException e) {
				logger.debug("Invalid plugin directory : " + pluginDir);
			}
		}
	}

	@Override
	public void setBasenames(String... inputList) {
		if (inputList == null) {
			return;
		}

		List<String> pluginBasenames = new ArrayList<String>();
		List<String> appBasenames = new ArrayList<String>();

		for (String name : inputList) {
			if (StringUtils.isBlank(name)) {
				continue;
			}

			try {
				name = name.trim();
				Resource[] resources = resourcePatternResolver.getResources(name);
				
				for (Resource resource : resources) {
					String uri = resource.getURI().toString();
					String basename = null;
					if (resource instanceof FileSystemResource || resource instanceof VfsResource) {
						basename = "classpath:"	+ StringUtils.substringBetween(uri, "/classes/", ".properties");
					} else if (resource instanceof ClassPathResource || resource instanceof UrlResource) {
						basename = StringUtils.substringBefore(uri, ".properties");
					} 

					if (basename != null) {
						String fullName = processBasename(basename);
						(isPluginResource(resource) ? pluginBasenames : appBasenames).add(fullName);
					}
				}
			} catch (IOException e) {
				logger.debug("No message source files found for basename " + name + ".");
			}
		}

		pluginBasenames.addAll(appBasenames);
		super.setBasenames(pluginBasenames.toArray(new String[0]));
	}

	private String processBasename(String basename) {
		String prefix = StringUtils.substringBeforeLast(basename, "/");
		String name = StringUtils.substringAfterLast(basename, "/");
		do {
			name = StringUtils.substringBeforeLast(name, "_");
		} while (name.contains("_"));

		return prefix + "/" + name;
	}

	private boolean isPluginResource(Resource resource)
	throws IOException {
		boolean isPluginResource = false;

		if (StringUtils.isNotBlank(pluginDir) && ResourceUtils.isJarURL(resource.getURL())) {
			isPluginResource = ResourceUtils.extractJarFileURL(resource.getURL()).getFile().startsWith(pluginDir);
		}

		return isPluginResource;
	}
}
