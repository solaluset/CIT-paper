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
}
