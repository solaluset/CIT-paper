package org.vinerdream.citPaper.converter;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
        if (data.getModel() != null) {
            copyModel(file.getParent(), namespace, data.getModel(), outputDirectory);
        } else {
            final Path tmpModelPath = Paths.get("tmp", path + ".json");
            tmpModelPath.getParent().toFile().mkdirs();
            try (FileWriter writer = new FileWriter(tmpModelPath.toFile())) {
                writer.write("{\"textures\":{\"layer0\":\"" + namespace + ":item/" + path + "\"}, \"parent\":\"item/generated\"}");
            }
            copyModel(tmpModelPath.getParent(), namespace, path, outputDirectory);
        }
        if (data.getTexture() != null) {
            copyTexture(file.getParent(), data.getTexture(), outputDirectory);
        }

        final Path jsonPath = outputDirectory.resolve(Paths.get("assets", namespace, "items", path + ".json"));
        final String jsonData = "{\"model\": {\"type\": \"model\", \"model\": \"item/" + namespace + "/" + path + "\"}}";
        jsonPath.getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(jsonPath.toFile())) {
            writer.write(jsonData);
        }
    }

    private Path copyTexture(Path inputDirectory, String texture, Path outputDirectory) throws IOException {
        return copyResource(
                inputDirectory,
                texture,
                "png",
                outputDirectory.resolve(Paths.get(
                        "assets",
                        inputDirectory.getFileName().toString(),
                        "textures",
                        "item"
                ))
        );
    }

    private void copyModel(Path inputDirectory, String namespace, String model, Path outputDirectory) throws IOException {
        Path newPath = copyResource(
                inputDirectory,
                model,
                "json",
                outputDirectory.resolve(Paths.get(
                        "assets",
                        "minecraft",
                        "models",
                        "item",
                        namespace
                ))
        );
        if (newPath == null) return;
        JSONObject json;
        try (FileReader reader = new FileReader(newPath.toFile())) {
            json = (JSONObject) new JSONParser().parse(reader);
            JSONObject textures = (JSONObject) json.get("textures");
            for (Object value : textures.entrySet()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) value;
                Path outputTexture = copyTexture(inputDirectory, entry.getValue(), outputDirectory);
                if (outputTexture == null) continue;
                textures.put(
                        entry.getKey(),
                        outputTexture.getParent().getParent().getParent().getFileName()
                                + ":item/" + getFilenameWithoutAndWithExtension(
                                        outputTexture.getFileName().toString(),
                                "png"
                                ).getKey()
                );
            }
        } catch (ParseException e) {
            log("Invalid model: " + namespace + ":" + model);
            throw new RuntimeException(e);
        }
        try (FileWriter writer = new FileWriter(newPath.toFile())) {
            writer.write(json.toJSONString());
        }
    }

    private Path copyResource(Path inputDirectory, String resource, String extension, Path outputDirectory) throws IOException {
        Map.Entry<String, String> pair = getFilenameWithoutAndWithExtension(resource, extension);
        resource = pair.getValue();
        Path oldPath;
        do {
            oldPath = inputDirectory.resolve(resource);
            inputDirectory = inputDirectory.getParent();
        } while (!oldPath.toFile().isFile() && inputDirectory != null);
        if (!oldPath.toFile().isFile()) {
            log("Missing resource: " + resource);
            return null;
        }
        final Path newPath = outputDirectory.resolve(oldPath.getFileName());
        newPath.getParent().toFile().mkdirs();
        Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        return newPath;
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
