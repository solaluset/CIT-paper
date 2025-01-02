package org.vinerdream.citPaper.converter;

import java.io.File;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class ResourcePackConverter {
    private final Consumer<String> logger;

    public ResourcePackConverter(Consumer<String> logger) {
        this.logger = logger;
    }

    public void convertResourcePack(String root, String outputDir) {
        String directory = Paths.get(root, "assets", "minecraft").toString();
        convertDirectory(Paths.get(directory, "optifine", "cit").toFile());
        convertDirectory(Paths.get(directory, "mcpatcher", "cit").toFile());
    }

    public void convertDirectory(File directory) {
        log("Converting " + directory);
    }

    private void log(String text) {
        logger.accept(text);
    }
}
