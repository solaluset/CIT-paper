package org.vinerdream.citPaper.converter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
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
        convertDirectory(Paths.get(directory, "optifine", "cit"), Paths.get(outputDir));
        convertDirectory(Paths.get(directory, "mcpatcher", "cit"), Paths.get(outputDir));
    }

    public void convertDirectory(Path directory, Path outputDirectory) {
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
                        convertFile(path, outputDirectory);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void convertFile(Path file, Path outputDirectory) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader(file.toFile()));

        ParsedTextureProperties data = new ParsedTextureProperties(properties);
        for (Object key : properties.keySet()) {
            log("Unknown property in " + file + ": " + key);
        }

        if (data.getType() == TextureType.ITEM) {
            convertItemFile(file, data, outputDirectory);
        }
    }

    private void convertItemFile(Path file, ParsedTextureProperties data, Path outputDirectory) throws IOException {
        String namespace = file.getParent().getFileName().toString();
        String path = file.getFileName().toString().replaceFirst("\\.properties$", "");
        final Path newPath;
        final Path oldPath;
        final Path jsonPath = outputDirectory.resolve(Paths.get("assets", namespace, "items", path + ".json"));
        final String jsonData;
        if (data.getTexture() != null) {
            Map.Entry<String, String> pair = getFilenameWithoutAndWithExtension(data.getTexture(), "png");
            final String bareTexture = pair.getKey();
            final String texture = pair.getValue();
            oldPath = file.getParent().resolve(texture);
            newPath = outputDirectory.resolve(Paths.get("assets", namespace, "textures", texture));
            jsonData = "{\"model\": {\"type\": \"texture\", \"texture\": \"item" + namespace + "/" + bareTexture + "\"}}";
        } else if (data.getModel() != null) {
            Map.Entry<String, String> pair = getFilenameWithoutAndWithExtension(data.getModel(), "json");
            final String bareModel = pair.getKey();
            final String model = pair.getValue();

            oldPath = file.getParent().resolve(model);
            newPath = outputDirectory.resolve(Paths.get("assets", "minecraft", "models", "item", namespace, model));
            jsonData = "{\"model\": {\"type\": \"model\", \"model\": \"item/" + namespace + "/" + bareModel + "\"}}";
        } else {
            log("No texture or model in " + file);
            return;
        }
        if (!oldPath.toFile().exists()) {
            log("Missing texture/model file: " + oldPath);
            return;
        }
        jsonPath.getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(jsonPath.toFile())) {
            writer.write(jsonData);
        }

        newPath.getParent().toFile().mkdirs();
        Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private Map.Entry<String, String> getFilenameWithoutAndWithExtension(String name, String extension) {
        if (name.endsWith(extension)) {
            return Map.entry(name.replaceFirst("\\." + extension + "$", ""), name);
        }
        return Map.entry(name, name + "." + extension);
    }

    private void log(String text) {
        logger.accept(text);
    }
}
