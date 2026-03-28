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
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		try {
			Method method = findAddUrlMethod(loader.getClass());
			if (method == null) {
				throw new IOException("No addURL method found in " + loader.getClass().getName());
			}

			method.setAccessible(true);
			method.invoke(loader, u);
		} catch (Exception e) {
			throw new IOException("Failed to inject URL: " + u, e);
		}
	}

	private static Method findAddUrlMethod(Class<?> type) {
		Class<?> cls = type;
		while (cls != null && cls != URLClassLoader.class) {
			try {
				return cls.getDeclaredMethod("addURL", URL.class);
			} catch (NoSuchMethodException e) {
				cls = cls.getSuperclass();
			}
		}
		return null;
	}
}
