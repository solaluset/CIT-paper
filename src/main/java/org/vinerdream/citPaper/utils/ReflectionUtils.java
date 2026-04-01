package org.vinerdream.citPaper.utils;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

    public static @NotNull String readResource(@NotNull String path) throws IOException {
        try (InputStream input = ReflectionUtils.class.getResourceAsStream(path)) {
            assert input != null;
            try (InputStreamReader inputStreamReader = new InputStreamReader(input)) {
                try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                        builder.append('\n');
                    }
                    return builder.toString();
                }
            }
        }
    }
}
