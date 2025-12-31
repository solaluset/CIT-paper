package com.nexomc.nexo.configs;

import java.util.List;

public enum Settings {
    PACK_READER_LENIENT(true),
    DEBUG,
    PACK_OUTPUT_PATH;

    private final boolean booleanValue;

    Settings() {
        this.booleanValue = false;
    }

    Settings(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public boolean toBool() {
        return booleanValue;
    }

    public List<String> toStringListOrSingle(kotlin.jvm.functions.Function1<?, ?> f) {
        return List.of("dummy");
    }
}
