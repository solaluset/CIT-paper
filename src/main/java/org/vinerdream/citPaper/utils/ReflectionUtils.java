package org.vinerdream.citPaper.utils;

public class ReflectionUtils {
    public static boolean isClassPresent(String name) {
        try {
            Class.forName(name, false, ReflectionUtils.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }
}
