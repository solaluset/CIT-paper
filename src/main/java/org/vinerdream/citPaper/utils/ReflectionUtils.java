package org.vinerdream.citPaper.utils;

public class ReflectionUtils {
    private static final String COMPONENT_CLASS = "net.kyori.adventure.text.Component";

    public static boolean isComponentClassPresent() {
        return isClassPresent(COMPONENT_CLASS);
    }

    private static boolean isClassPresent(String name) {
        try {
            Class.forName(name, false, ReflectionUtils.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    public static boolean hasMethod(Class<?> klass, String method) {
        try {
            klass.getMethod(method);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
