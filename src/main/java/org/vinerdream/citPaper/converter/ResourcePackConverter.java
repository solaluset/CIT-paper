package org.vinerdream.citPaper.converter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.vinerdream.citPaper.utils.PropertiesUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ResourcePackConverter {
    private final Consumer<String> logger;
    private final List<ParsedTextureProperties> convertedEntries;

    public ResourcePackConverter(Consumer<String> logger) {
        this.logger = logger;
        convertedEntries = new ArrayList<>();
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
        Map<String, String> propertiesMap = PropertiesUtils.propertiesToMap(properties);

        ParsedTextureProperties data = new ParsedTextureProperties(propertiesMap);
        for (String key : propertiesMap.keySet()) {
            log("Unknown property in " + file + ": " + key);
        }

        if (data.getType() == TextureType.ITEM) {
            convertItemFile(file, data, outputDirectory);
            convertedEntries.add(data);
        }

    }

    private void convertItemFile(Path file, ParsedTextureProperties data, Path outputDirectory) throws IOException {
        String namespace = file.getParent().getFileName().toString();
        if (!file.getParent().getParent().getFileName().toString().equals("cit")) {
            namespace = file.getParent().getParent().getFileName() + "_" + namespace;
        }
        final String path = file.getFileName().toString().replaceFirst("\\.properties$", "");
        data.setKey(new NamespacedKey(namespace.toLowerCase(), path.toLowerCase()));
        final String modelName;
        final String textureName;
        final String forcedTexture;
        Path blockingModelPath = null;
        if (data.getTexture() != null) {
            textureName = getFilenameWithoutAndWithExtension(copyTexture(file.getParent(), namespace, data.getTexture(), outputDirectory).getFileName().toString(), "png").getKey();
            forcedTexture = textureName;
        } else {
            textureName = path;
            forcedTexture = null;
        }
        if (data.getModel() != null) {
            Path newModelPath = copyModel(
                    file.getParent(),
                    namespace,
                    getFilenameWithoutAndWithExtension(data.getModel(), "json").getKey(),
                    forcedTexture,
                    outputDirectory
            );
            if (newModelPath != null) {
                modelName = getFilenameWithoutAndWithExtension(newModelPath.getFileName().toString(), "json").getKey();
                if (data.getModelShieldBlocking() != null) {
                    blockingModelPath = copyModel(
                            file.getParent(),
                            namespace,
                            getFilenameWithoutAndWithExtension(data.getModelShieldBlocking(), "json").getKey(),
                            forcedTexture,
                            outputDirectory
                    );
                }
            } else {
                modelName = null;
            }
        } else {
            final Path tmpModelPath = Paths.get(System.getProperty("java.io.tmpdir"), "cit-paper", path + ".json");
            tmpModelPath.getParent().toFile().mkdirs();
            try (FileWriter writer = new FileWriter(tmpModelPath.toFile())) {
                writer.write("{\"textures\":{\"layer0\":\"" + namespace + ":item/" + textureName + "\"}, \"parent\":\"item/generated\"}");
            }
            modelName = getFilenameWithoutAndWithExtension(Objects.requireNonNull(
                    copyModel(tmpModelPath.getParent(), namespace, path, forcedTexture, outputDirectory),
                    "Failed to copy generated model!"
            ).getFileName().toString(), "json").getKey();
            tmpModelPath.getParent().toFile().delete();
        }

        final Path jsonPath = outputDirectory.resolve(Paths.get("assets", namespace, "items", path + ".json"));
        final String jsonData;
        if (blockingModelPath == null) {
            jsonData = "{\"model\": {\"type\": \"model\", \"model\": \"item/" + namespace + "/" + modelName + "\"}}";
        } else {
            jsonData = "{\"model\": {\"type\": \"condition\", \"property\": \"using_item\", \"on_true\": {\"type\": \"model\", \"model\": \"item/"
                    + namespace + "/" + getFilenameWithoutAndWithExtension(blockingModelPath.getFileName().toString(), "json").getKey()
                    + "\"}, \"on_false\": {\"type\": \"model\", \"model\": \"item/" + namespace + "/" + modelName + "\"}}}";
        }
        jsonPath.getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(jsonPath.toFile())) {
            writer.write(jsonData);
        }
    }

    private void addShieldBlockingModel(
            Path inputDirectory,
            Path originalModelPath,
            String blockingModel,
            String namespace,
            String forcedTexture,
            Path outputDirectory
    ) throws IOException {
        copyModel(
                inputDirectory,
                namespace,
                getFilenameWithoutAndWithExtension(blockingModel, "json").getKey(),
                forcedTexture,
                outputDirectory
        );
        final JsonObject json;
        try (FileReader reader = new FileReader(originalModelPath.toFile())) {
            json = new Gson().fromJson(reader, JsonObject.class);
        }

        final JsonObject newOverride = new JsonObject();
        final JsonObject predicate = new JsonObject();
        predicate.addProperty("blocking", 1);
        newOverride.add("predicate", predicate);
        newOverride.addProperty("model", "item/" + namespace + "/" + blockingModel);

        final JsonElement overrides = json.get("overrides");
        final JsonArray overridesArray;
        if (overrides != null) {
            overridesArray = overrides.getAsJsonArray();
        } else {
            overridesArray = new JsonArray();
        }
        overridesArray.add(newOverride);
        json.add("overrides", overridesArray);

        try (FileWriter writer = new FileWriter(originalModelPath.toFile())) {
            writer.write(new Gson().toJson(json));
        }
    }

    private Path copyTexture(Path inputDirectory, String namespace, String texture, Path outputDirectory) throws IOException {
        return copyResource(
                inputDirectory,
                texture.replaceFirst(":", "/textures/"),
                "png",
                outputDirectory.resolve(Paths.get(
                        "assets",
                        namespace,
                        "textures",
                        "item"
                ))
        );
    }

    private Path copyModel(Path inputDirectory, String namespace, String model, String textureName, Path outputDirectory) throws IOException {
        Path modelDirectory = outputDirectory.resolve(Paths.get(
                "assets",
                "minecraft",
                "models",
                "item",
                namespace
        ));

        Path newPath = copyResource(
                inputDirectory,
                model,
                "json",
                modelDirectory
        );
        if (newPath == null) return null;
        JsonObject json;
        try (FileReader reader = new FileReader(newPath.toFile())) {
            json = new Gson().fromJson(reader, JsonObject.class);
            JsonObject textures = json.get("textures").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                if (textureName == null) {
                    Path outputTexture = copyTexture(inputDirectory, namespace, entry.getValue().getAsString(), outputDirectory);
                    if (outputTexture == null) continue;
                    textures.addProperty(
                            entry.getKey(),
                            outputTexture.getParent().getParent().getParent().getFileName()
                                    + ":item/" + getFilenameWithoutAndWithExtension(
                                    outputTexture.getFileName().toString(),
                                    "png"
                            ).getKey()
                    );
                } else {
                    textures.addProperty(entry.getKey(), namespace + ":item/" + textureName);
                }
            }
        }
        try (FileWriter writer = new FileWriter(newPath.toFile())) {
            writer.write(new Gson().toJson(json));
        }
        return newPath;
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
        int i = 0;
        Path newPath;
        do {
            newPath = outputDirectory.resolve(getFilenameWithoutAndWithExtension(oldPath.getFileName().toString(), extension).getKey() + i++ + "." + extension);
        } while (newPath.toFile().exists());
        newPath.getParent().toFile().mkdirs();
        Files.copy(oldPath, newPath);
        return newPath;
    }

    public void saveConfiguration(Path outputPath) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.set("renames", convertedEntries.stream().map(ParsedTextureProperties::asMap).toList());
        config.save(outputPath.toFile());
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
