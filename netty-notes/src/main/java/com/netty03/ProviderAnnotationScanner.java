package com.netty03;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.netty03.annotation.ServiceProvider;

//扫描所有的@ServerProvider类
public class ProviderAnnotationScanner {

	public Set<Class<?>> scannerClass(String basePackage) {
		URL url = this.getClass().getClassLoader().getResource(basePackage.replaceAll("\\.", "/"));
		System.out.println("path=" + url.getPath() + " classLoadder=" + this.getClass().getClassLoader());

		String basePath = url.getPath();
		System.out.println("basePath=" + basePath);

		Set<Class<?>> classSet = new HashSet<>();
		findAndFillPropertiesFile(basePackage, classSet);

		return classSet;
	}

	private void findAndFillPropertiesFile(String basePackage, Set<Class<?>> classSet) {
		URL url = this.getClass().getClassLoader().getResource(basePackage.replaceAll("\\.", "/"));
		File parent = new File(url.getFile());
		File[] files = parent.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				findAndFillPropertiesFile(basePackage + "." + file.getName(), classSet);
			} else if (file.isFile() && file.getName().endsWith(".class")) {
				String className = basePackage + "." + file.getName().replace(".class", "");
				System.out.println(" className=" + className);
				try {
					Class<?> clazz = Class.forName(className);
					if (!clazz.isAnnotationPresent(ServiceProvider.class)) {
						continue;
					}
					classSet.add(clazz);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
