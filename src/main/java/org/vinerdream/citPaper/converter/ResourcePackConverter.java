package org.vinerdream.citPaper.converter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ResourcePackConverter {
    private final Consumer<String> logger;

    public ResourcePackConverter(Consumer<String> logger) {
        this.logger = logger;
    }

    public void convertResourcePack(String root, String outputDir) {
        String directory = Paths.get(root, "assets", "minecraft").toString();
        convertDirectory(Paths.get(directory, "optifine", "cit"));
        convertDirectory(Paths.get(directory, "mcpatcher", "cit"));
    }

    public void convertDirectory(Path directory) {
        log("Converting " + directory);
        File dir = directory.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            log("Directory not found, skipping");
            return;
        }
        try (Stream<Path> contents = Files.walk(directory)) {
            contents.forEach(path -> {
                if (!path.toFile().isFile()) {
                    return;
                }
                if (path.toString().endsWith(".properties")) {
                    try {
                        convertFile(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void convertFile(Path file) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader(file.toFile()));

        ParsedTextureProperties data = new ParsedTextureProperties(properties);
        for (Object key : properties.keySet()) {
            log("Unknown property in " + file + ": " + key);
        }

        if (data.getType() == TextureType.ITEM) {
            convertItemFile(file, data);
        }
    }

    private void convertItemFile(Path file, ParsedTextureProperties data) {
        log(data.getType().toString());
    }

    private void log(String text) {
        logger.accept(text);
    }
}
