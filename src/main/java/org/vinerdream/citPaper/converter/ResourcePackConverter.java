package org.vinerdream.citPaper.converter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.NamespacedKey;
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

        ParsedTextureProperties data = new ParsedTextureProperties(propertiesMap, logger);
        for (String key : propertiesMap.keySet()) {
            log("Unknown property in " + file + ": " + key);
        }

        convertPropertiesFile(file, data, outputDirectory);

        boolean found = false;
        for (ParsedTextureProperties savedData : convertedEntries) {
            if (savedData.itemEquals(data)) {
                if (savedData.getArmorTexture() == null) {
                    savedData.setArmorTexture(data.getArmorTexture());
                } else {
                    savedData.setKey(data.getKey());
                }
                convertedEntries.remove(savedData);
                convertedEntries.add(new ParsedTextureProperties(savedData.asMap(), logger));
                found = true;
                break;
            }
        }
        if (!found) {
            convertedEntries.add(data);
        }

    }

    private void convertPropertiesFile(Path file, ParsedTextureProperties data, Path outputDirectory) throws IOException {
        String namespace = file.getParent().getFileName().toString();
        namespace = file.getParent().getParent().getFileName() + "_" + namespace;
        final String path = file.getFileName().toString().replaceFirst("\\.properties$", "");
        data.setKey(new NamespacedKey(namespace.toLowerCase(), path.toLowerCase()));
        final String modelName;
        final String textureName;
        final String forcedTexture;
        Path blockingModelPath = null;
        if (data.getTexture() != null) {
            Path newTexturePath = copyTexture(file.getParent(), namespace, data.getTexture(), outputDirectory);
            textureName = newTexturePath != null ?
                    getFilenameWithoutAndWithExtension(newTexturePath.getFileName().toString(), "png").getKey()
                    : null;
            forcedTexture = textureName;
        } else {
            textureName = path;
            forcedTexture = null;
        }
        if (data.getArmorTexture() != null) {
            final Path armorTexturePath = copyArmorTexture(
                    file.getParent(),
                    namespace,
                    data.getArmorTexture(),
                    data.getArmorTextureType(),
                    outputDirectory
            );
            final String armorTextureName = getFilenameWithoutAndWithExtension(
                    armorTexturePath.getFileName().toString(), "png"
            ).getKey();
            final Path modelPath = outputDirectory.resolve(
                    Paths.get("assets", namespace, "equipment", armorTextureName + ".json")
            );
            modelPath.getParent().toFile().mkdirs();
            try (FileWriter writer = new FileWriter(modelPath.toFile())) {
                if (data.getType() != TextureType.ELYTRA) {
                    writer.write("{\"layers\": {\"humanoid\": [{\"texture\": \"" + namespace + ":" + armorTextureName
                            + "\"}], \"humanoid_leggings\": [{\"texture\": \"" + namespace + ":" + armorTextureName
                            + "\"}]}}");
                } else {
                    writer.write("{\"layers\": {\"wings\": [{\"texture\": \"" + namespace + ":" + armorTextureName + "\"}]}}");
                }
            }
            data.setArmorTexture(namespace + ":" + armorTextureName);
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
                if (data.getShieldBlockingModel() != null) {
                    blockingModelPath = copyModel(
                            file.getParent(),
                            namespace,
                            getFilenameWithoutAndWithExtension(data.getShieldBlockingModel(), "json").getKey(),
                            forcedTexture,
                            outputDirectory
                    );
                }
            } else {
                modelName = null;
            }
        } else if (data.getType() == TextureType.ITEM) {
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
        } else {
            return;
        }

        final Path jsonPath = outputDirectory.resolve(Paths.get("assets", namespace, "items", path.toLowerCase() + ".json"));
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

    private Path copyArmorTexture(Path inputDirectory, String namespace, String texture, int textureType, Path outputDirectory) throws IOException {
        final String subfolder = switch (textureType) {
            case 1:
                yield "humanoid";
            case 2:
                yield "humanoid_leggings";
            case 3:
                yield "wings";
            default:
                throw new IllegalStateException("Unexpected value: " + textureType);
        };
        return copyResource(
                inputDirectory,
                texture,
                "png",
                outputDirectory.resolve(Paths.get(
                    "assets",
                        namespace,
                        "textures",
                        "entity",
                        "equipment",
                        subfolder
                ))
        );
    }

    private Path copyModel(Path inputDirectory, String namespace, String model, String textureName, Path outputDirectory) throws IOException {
        final Path modelDirectory = outputDirectory.resolve(Paths.get(
                "assets",
                "minecraft",
                "models",
                "item",
                namespace
        ));
        final String outputName;
        if (textureName != null) {
            String[] parts = model.split("/");
            outputName = parts[parts.length - 1] + "_" + textureName;
        } else {
            outputName = null;
        }

        Path newPath = copyResource(
                inputDirectory,
                model,
                "json",
                modelDirectory,
                outputName
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
        return copyResource(inputDirectory, resource, extension, outputDirectory, null);
    }

    private Path copyResource(Path inputDirectory, String resource, String extension, Path outputDirectory, String outputName) throws IOException {
        Map.Entry<String, String> pair = getFilenameWithoutAndWithExtension(resource, extension);
        resource = pair.getValue();
        Path oldPath = resolveResource(inputDirectory, resource, extension.equals("json") ? ResourceType.MODEL : ResourceType.TEXTURE);
        if (oldPath == null) {
            log("Missing resource: " + resource);
            return null;
        }
        Path newPath = outputDirectory.resolve(getFilenameWithoutAndWithExtension((outputName != null ? outputName : oldPath.getFileName()).toString().toLowerCase(), extension).getValue());
        newPath.getParent().toFile().mkdirs();
        Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        if (addMcmeta(oldPath).toFile().isFile()) {
            Files.copy(addMcmeta(oldPath), addMcmeta(newPath), StandardCopyOption.REPLACE_EXISTING);
        }
        return newPath;
    }

    private Path addMcmeta(Path path) {
        return path.getParent().resolve(path.getFileName() + ".mcmeta");
    }

    private Path resolveResource(Path currentDirectory, String resource, ResourceType type) throws IOException {
        Path result;
        do {
            result = currentDirectory.resolve(resource);
            currentDirectory = currentDirectory.getParent();
        } while (!result.toFile().isFile() && currentDirectory != null && !currentDirectory.endsWith("minecraft"));
        if (result.toFile().isFile()) return result;

        if (currentDirectory == null) return null;

        result = currentDirectory.resolve(resource);
        if (result.toFile().isFile()) return result;

        currentDirectory = currentDirectory.getParent();

        result = currentDirectory.resolve(resource);
        if (result.toFile().isFile()) return result;

        try (Stream<Path> directories = Files.walk(currentDirectory, 1)) {
            result = directories.filter(dir -> dir.resolve(type == ResourceType.MODEL ? "models" : "textures").resolve(resource).toFile().isFile()).findFirst().orElse(null);
            if (result == null) return null;
            return result.resolve(type == ResourceType.MODEL ? "models" : "textures").resolve(resource);
        }
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
