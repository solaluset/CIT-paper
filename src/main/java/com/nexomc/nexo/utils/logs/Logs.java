package com.nexomc.nexo.utils.logs;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class Logs {
    public static @Nullable Consumer<String> logger = null;
    public static void logWarn$default(String var0, boolean var1, int var2, Object var3) {
        if (logger != null) {
            logger.accept(var0);
        }
    }
}
