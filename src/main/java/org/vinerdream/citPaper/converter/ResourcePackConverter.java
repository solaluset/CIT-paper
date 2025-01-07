package org.vinerdream.citPaper.converter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.FileUtil;
import org.vinerdream.citPaper.utils.FileUtils;
import org.vinerdream.citPaper.utils.PropertiesUtils;
import org.vinerdream.citPaper.utils.ZipUtils;

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

    public void convertResourcePack(Path rootPath, Path outputPath) throws IOException {
        if (rootPath.toFile().isFile()) {
            Path newPath = getTmpDir().resolve(UUID.randomUUID().toString());
            ZipUtils.unzip(rootPath, newPath);
            rootPath = newPath;
        }
        final Path zipPath;
        if (outputPath.toString().endsWith(".zip")) {
            zipPath = outputPath;
            outputPath = getTmpDir().resolve(UUID.randomUUID().toString());
        } else {
            zipPath = null;
        }

        FileUtils.removeDirectory(outputPath);
        if (zipPath == null) {
            FileUtils.copyDirectory(rootPath, outputPath);
        } else {
            outputPath = rootPath;
        }

        final Path directory = rootPath.resolve("assets").resolve("minecraft");
        convertDirectory(directory.resolve("optifine").resolve("cit"), outputPath);
        convertDirectory(directory.resolve("mcpatcher").resolve("cit"), outputPath);

        if (zipPath != null) {
            ZipUtils.zip(outputPath, zipPath);
        }

        FileUtils.removeDirectory(getTmpDir());
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
            if (savedData.itemEquals(data, logger)) {
                if (savedData.getArmorData() == null) {
                    savedData.setArmorData(data.getArmorData());
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
        if (data.getArmorData() != null) {
            final String armorModel = convertArmorTextureData(file, data.getArmorData(), namespace, data.getArmorDataType(), outputDirectory);

            data.getArmorData().setModel(namespace + ":" + armorModel);
        }

        final String modelName;
        if (data.getMainTextureData() != null) {
            modelName = convertTextureData(file, data.getMainTextureData(), namespace, "generated", outputDirectory);
        } else if (data.getElytraTextureData() != null) {
            modelName = convertTextureData(file, data.getElytraTextureData(), namespace, "generated", outputDirectory);
        } else return;

        final Path jsonPath = outputDirectory.resolve(Paths.get("assets", namespace, "items", path.toLowerCase() + ".json"));
        final String jsonData;

        if (data.getBowTextureData() != null) {
            BowTextureData bowTextureData = data.getBowTextureData();
            normalizeBowData(file, bowTextureData, namespace, outputDirectory);

            jsonData = "{\"model\": {\"type\": \"condition\", \"property\": \"using_item\", \"on_false\": {\"type\": \"model\", \"model\": \""
                    + namespace + ":item/" + bowTextureData.getModel() + "\"}, \"on_true\": {\"type\": \"range_dispatch\", \"property\": \"use_duration\", \"scale\": 0.05, \"entries\": [{\"model\": {\"type\": \"model\", \"model\": \""
                    + namespace + ":item/" + bowTextureData.getPulling_1().getModel() + "\"}, \"threshold\": 0.65}, {\"model\": {\"type\": \"model\", \"model\": \""
                    + namespace + ":item/" + bowTextureData.getPulling_2().getModel() + "\"}, \"threshold\": 0.9}], \"fallback\": {\"type\": \"model\", \"model\": \""
                    + namespace + ":item/" + bowTextureData.getPulling_0().getModel() + "\"}}}}";
        } else if (data.getCrossbowTextureData() != null) {
            CrossbowTextureData crossbowTextureData = data.getCrossbowTextureData();
            normalizeBowData(file, crossbowTextureData, namespace, outputDirectory);

            jsonData = "{\"model\": {\"type\": \"minecraft:condition\", \"on_false\": {\"type\": \"minecraft:select\", \"cases\": [{\"model\": {\"type\": \"minecraft:model\", \"model\": \""
                    + namespace + ":item/" + crossbowTextureData.getWithArrow().getModel() + "\"}, \"when\": \"arrow\"}, {\"model\": {\"type\": \"minecraft:model\", \"model\": \""
                    + namespace + ":item/" + crossbowTextureData.getWithFirework().getModel() + "\"}, \"when\": \"rocket\"}], \"fallback\": {\"type\": \"minecraft:model\", \"model\": \""
                    + namespace + ":item/" + crossbowTextureData.getModel() + "\"}, \"property\": \"minecraft:charge_type\"}, \"on_true\": {\"type\": \"minecraft:range_dispatch\", \"entries\": [{\"model\": {\"type\": \"minecraft:model\", \"model\": \""
                    + namespace + ":item/" + crossbowTextureData.getPulling_1().getModel() + "\"}, \"threshold\": 0.58}, {\"model\": {\"type\": \"minecraft:model\", \"model\": \""
                    + namespace + ":item/" + crossbowTextureData.getPulling_2().getModel() + "\"}, \"threshold\": 1.0}], \"fallback\": {\"type\": \"minecraft:model\", \"model\": \""
                    + namespace + ":item/" + crossbowTextureData.getPulling_0().getModel() + "\"}, \"property\": \"minecraft:crossbow/pull\"}, \"property\": \"minecraft:using_item\"}}";
        } else if (data.getShieldBlockingData() != null) {
            final String blockingModel = convertTextureData(file, data.getShieldBlockingData(), namespace, "shield", outputDirectory);
            jsonData = "{\"model\": {\"type\": \"condition\", \"property\": \"using_item\", \"on_true\": {\"type\": \"model\", \"model\": \""
                    + namespace + ":item/" + blockingModel
                    + "\"}, \"on_false\": {\"type\": \"model\", \"model\": \"" + namespace + ":item/" + modelName + "\"}}}";
        } else {
            jsonData = "{\"model\": {\"type\": \"model\", \"model\": \"" + namespace + ":item/" + modelName + "\"}}";
        }
        jsonPath.getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(jsonPath.toFile())) {
            writer.write(jsonData);
        }
    }

    private void normalizeBowData(Path file, BowTextureData data, String namespace, Path outputDirectory) throws IOException {
        TextureData first = null;
        final String parent;
        if (data instanceof CrossbowTextureData) {
            parent = "crossbow";
        } else {
            parent = "bow";
        }
        for (TextureData data1 : data.getAll()) {
            if (data1 == null) continue;
            if (first == null) {
                first = data1;
            }
            data1.setModel(convertTextureData(file, data1, namespace, parent, outputDirectory));
        }
        assert first != null;
        if (data.getModel() == null) {
            data.setModel(first.getModel());
        }
        if (data.getPulling_0() == null) {
            data.setPulling_0(first);
        }
        if (data.getPulling_1() == null) {
            data.setPulling_1(first);
        }
        if (data.getPulling_2() == null) {
            data.setPulling_2(first);
        }
        if (data instanceof CrossbowTextureData crossbow) {
            if (crossbow.getWithArrow() == null) {
                crossbow.setWithArrow(first);
            }
            if (crossbow.getWithFirework() == null) {
                crossbow.setWithFirework(first);
            }
        }
    }

    private String convertTextureData(Path file, TextureData data, String namespace, String parent, Path outputDirectory) throws IOException {
        final String model = removeExtension(data.getModel());
        final String texture = removeExtension(data.getTexture());

        if (model == null) {
            if (texture == null) {
                return null;
            }
            final Path texturePath = copyTexture(file.getParent(), namespace, texture, outputDirectory);
            if (texturePath == null) return null;
            final String textureName = resourceNameFromPath(texturePath);

            return textureToModel(namespace, textureName, parent, outputDirectory);
        }
        final Path modelPath = copyModel(file.getParent(), namespace, model, texture, outputDirectory);
        if (modelPath == null) return null;

        return resourceNameFromPath(modelPath);
    }

    private String convertArmorTextureData(Path file, TextureData data, String namespace, int type, Path outputDirectory) throws IOException {
        final String model = removeExtension(data.getModel());
        final String texture = removeExtension(data.getTexture());

        if (model == null) {
            if (texture == null) {
                return null;
            }
            final Path texturePath = copyArmorTexture(file.getParent(), namespace, texture, type, outputDirectory);
            if (texturePath == null) return null;
            final String textureName = resourceNameFromPath(texturePath);

            return armorTextureToModel(file, namespace, textureName, type, outputDirectory);
        }
        final Path modelPath = copyResource(
                file.getParent(),
                texture.replaceFirst(":", "/models/"),
                "json",
                outputDirectory.resolve(Paths.get("assets", namespace, "equipment", model + ".json"))
        );
        if (modelPath == null) return null;

        return resourceNameFromPath(modelPath);
    }

    private String resourceNameFromPath(Path path) {
        return removeExtension(path.getFileName().toString());
    }

    private String removeExtension(String filename) {
        if (filename == null) return null;
        return filename.replaceFirst("\\.[^/]+$", "");
    }

    private String textureToModel(String namespace, String textureName, String parent, Path outputDirectory) throws IOException {
        final Path tmpModelPath = getTmpDir().resolve("models").resolve(textureName + ".json");
        tmpModelPath.getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(tmpModelPath.toFile())) {
            writer.write("{\"textures\":{\"layer0\":\"" + namespace + ":item/" + textureName + "\"}, \"parent\":\"item/" + parent + "\"}");
        }

        return getFilenameWithoutAndWithExtension(Objects.requireNonNull(
                copyModel(tmpModelPath.getParent(), namespace, textureName, false, null, outputDirectory),
                "Failed to copy generated model!"
        ).getFileName().toString(), "json").getKey();
    }

    private String armorTextureToModel(Path file, String namespace, String texture, int type, Path outputDirectory) throws IOException {
        final Path armorTexturePath = copyArmorTexture(
                file.getParent(),
                namespace,
                texture,
                type,
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
            if (type != 3) {
                writer.write("{\"layers\": {\"humanoid\": [{\"texture\": \"" + namespace + ":" + armorTextureName
                        + "\"}], \"humanoid_leggings\": [{\"texture\": \"" + namespace + ":" + armorTextureName
                        + "\"}]}}");
            } else {
                writer.write("{\"layers\": {\"wings\": [{\"texture\": \"" + namespace + ":" + armorTextureName + "\"}]}}");
            }
        }
        return resourceNameFromPath(modelPath);
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
        return copyModel(inputDirectory, namespace, model, true, textureName, outputDirectory);
    }

    private Path copyModel(Path inputDirectory, String namespace, String model, boolean processTextures, String textureName, Path outputDirectory) throws IOException {
        final Path modelDirectory = outputDirectory.resolve(Paths.get(
                "assets",
                namespace,
                "models",
                "item"
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
        if (newPath == null || !processTextures) return newPath;
        JsonObject json;
        try (FileReader reader = new FileReader(newPath.toFile())) {
            json = new Gson().fromJson(reader, JsonObject.class);
            JsonObject textures = json.get("textures").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                Path outputTexture = copyTexture(inputDirectory, namespace, textureName == null ? entry.getValue().getAsString() : textureName, outputDirectory);
                if (outputTexture == null) continue;
                textures.addProperty(
                        entry.getKey(),
                        outputTexture.getParent().getParent().getParent().getFileName()
                                + ":item/" + getFilenameWithoutAndWithExtension(
                                outputTexture.getFileName().toString(),
                                "png"
                        ).getKey()
                );
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
            log("Missing resource: " + resource + " (path: " + inputDirectory + ")");
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

    private Path getTmpDir() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "cit-paper");
    }

    private void log(String text) {
        logger.accept(text);
    }
}
