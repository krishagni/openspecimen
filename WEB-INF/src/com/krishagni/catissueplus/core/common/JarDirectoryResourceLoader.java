package com.krishagni.catissueplus.core.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.runtime.resource.loader.JarResourceLoader;
import org.apache.velocity.util.ExtProperties;


public class JarDirectoryResourceLoader extends JarResourceLoader {

	public void init(ExtProperties configuration) {
		Vector paths = configuration.getVector("path");
		if (paths == null || paths.size() == 0) {
			return;
		}

		List<String> jarFileNames = new ArrayList<String>();
		for (Object path : paths) {
			String pathStr = (String)path;
			if (StringUtils.isBlank(pathStr)) {
				continue;
			}

			jarFileNames.addAll(getJarFileNames(pathStr));
		}

		if (jarFileNames.isEmpty()) {
			return;
		}

		configuration.setProperty("path", String.join(",", jarFileNames));
		super.init(configuration);
	}

	private List<String> getJarFileNames(String dirPath) {
		List<String> jarFileNames = new ArrayList<String>();
		File jarDirFile = new File(dirPath);
		if (!jarDirFile.isDirectory()) {
			throw new RuntimeException("Invalid directory path: " + dirPath);
		}

		for (File file : jarDirFile.listFiles()) {
			if (file.isDirectory() || !file.getName().endsWith(".jar")) {
				continue;
			}

			JarFile jarFile = null;
			try {
				jarFile = new JarFile(file);
				Attributes attrs = jarFile.getManifest().getMainAttributes();
				String pluginName = attrs.getValue("os-plugin-name");
				if (StringUtils.isNotBlank(pluginName)) {
					jarFileNames.add("jar:file:" +  jarFile.getName());
				}
			} catch (Exception e) {
				rsvc.getLog().error("Error loading template resources from: " + dirPath, e);
			} finally {
				IOUtils.closeQuietly(jarFile);
			}
		}

		return jarFileNames;
	}
}
