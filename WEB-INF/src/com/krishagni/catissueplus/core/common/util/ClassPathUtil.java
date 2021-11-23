package com.krishagni.catissueplus.core.common.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassPathUtil {
	
	public static void addFile(File f) 
	throws IOException {
		addURL(f.toURI().toURL());
	}

	public static void addURL(URL u) 
	throws IOException {
		ClassLoader loader = ClassPathUtil.class.getClassLoader();
		Class<URLClassLoader> urlLoaderCls = URLClassLoader.class;

		try {
			Method method = urlLoaderCls.getDeclaredMethod(
				"addURL",
				new Class<?>[] { URL.class });
			method.setAccessible(true);
			method.invoke(loader, new Object[] { u });
		} catch (Throwable t) {
			throw new IOException("Error, could not add URL to classloader: " + u, t);
		}
	}
}