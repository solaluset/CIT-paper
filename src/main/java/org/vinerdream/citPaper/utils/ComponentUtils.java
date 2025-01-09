package org.vinerdream.citPaper.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ComponentUtils {
    public static String componentToString(Object object) {
        if (object == null) return null;
        if (object instanceof Component component) {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }
        return null;
    }
}
