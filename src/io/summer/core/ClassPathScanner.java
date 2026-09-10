package io.summer.core;

import io.summer.annotation.Component;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClassPathScanner {

    public List<Class<?>> scan(String basePackage) {
        String path = basePackage.replace('.', '/');
        List<Class<?>> result = new ArrayList<>();
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = cl.getResources(path);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if ("file".equals(url.getProtocol())) {
                    scanDirectory(new File(url.toURI()), basePackage, result);
                }
                // jar 지원은 Phase 2에서는 생략해도 됨 (데모는 file classpath면 충분)
            }
        } catch (Exception e) {
            throw new SummerException("Failed to scan package: " + basePackage, e);
        }
        return result;
    }

    private void scanDirectory(File dir, String packageName, List<Class<?>> out)
            throws ClassNotFoundException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), out);
            } else if (file.getName().endsWith(".class")
                    && !file.getName().contains("$")) { // inner class 제외
                String className = packageName + "." +
                        file.getName().substring(0, file.getName().length() - 6);
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(Component.class)
                        && !clazz.isInterface()
                        && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                    out.add(clazz);
                }
            }
        }
    }
}
