package org.vinerdream.citPaper.converter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.vinerdream.citPaper.utils.FileUtils;
import org.vinerdream.citPaper.utils.PropertiesUtils;
import org.vinerdream.citPaper.utils.ZipUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.vinerdream.citPaper.utils.CollectionUtils.allHaveAnySuffix;

public class ResourcePackConverter {
    private final Consumer<String> logger;
    private final List<ParsedTextureProperties> convertedEntries;

    public ResourcePackConverter(Consumer<String> logger) {
        this.logger = logger;
        convertedEntries = new ArrayList<>();
    }

    public void convertResourcePack(Path rootPath, Path outputPath) throws IOException {
        convertResourcePack(rootPath, outputPath, true);
    }

    public void convertResourcePack(Path rootPath, Path outputPath, boolean preserveCitDirectories) throws IOException {
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
        FileUtils.copyDirectory(rootPath, outputPath);

        Path directory = rootPath.resolve("assets").resolve("minecraft");
        convertDirectory(directory.resolve("optifine").resolve("cit"), outputPath);
        convertDirectory(directory.resolve("mcpatcher").resolve("cit"), outputPath);

        if (!preserveCitDirectories) {
            directory = outputPath.resolve("assets").resolve("minecraft");
            FileUtils.removeDirectory(directory.resolve("optifine").resolve("cit"));
            FileUtils.removeDirectory(directory.resolve("mcpatcher").resolve("cit"));
        }

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
        final String namespace = file.getParent().getParent().getFileName() + "_" + file.getParent().getFileName().toString();
        final String path = file.getFileName().toString().replaceFirst("\\.properties$", "");

        if (data.getKey() == null) {
            data.setKey(new NamespacedKey(namespace.toLowerCase(), path.toLowerCase()));
        } else return;

        if (!data.hasAnyData()) {
            if (file.getParent().resolve(path + ".json").toFile().isFile()) {
                data.setMainTextureData(new TextureData(path, null));
            } else if (file.getParent().resolve(path + ".png").toFile().isFile()) {
                data.setMainTextureData(new TextureData(null, path));
            }
        }

        if (data.getArmorData() != null) {
            final String armorModel = convertArmorTextureData(file, data.getArmorData(), namespace, data.getArmorDataType(), outputDirectory);

            data.getArmorData().setModel(namespace + ":" + armorModel);
        }

        final String modelName;
        if (data.getMainTextureData() != null) {
            modelName = convertTextureData(file, data.getMainTextureData(), namespace, guessParent(data), outputDirectory);
        } else if (data.getElytraTextureData() != null) {
            modelName = convertTextureData(file, data.getElytraTextureData(), namespace, "generated", outputDirectory);
        } else return;

        final Path jsonPath = outputDirectory.resolve(Paths.get("assets", namespace, "items", path.toLowerCase() + ".json"));
        final String jsonData;

        if (data.getBowTextureData() != null) {
            BowTextureData bowTextureData = data.getBowTextureData();
            normalizeData(file, bowTextureData, namespace, outputDirectory);

            jsonData = String.format(
                    readResource("/models/bow.json"),
                    namespace,
                    bowTextureData.getModel(),
                    namespace,
                    bowTextureData.getPulling_1().getModel(),
                    namespace,
                    bowTextureData.getPulling_2().getModel(),
                    namespace,
                    bowTextureData.getPulling_0().getModel()
            );
        } else if (data.getCrossbowTextureData() != null) {
            CrossbowTextureData crossbowTextureData = data.getCrossbowTextureData();
            normalizeData(file, crossbowTextureData, namespace, outputDirectory);

            jsonData = String.format(
                    readResource("/models/crossbow.json"),
                    namespace,
                    crossbowTextureData.getWithArrow().getModel(),
                    namespace,
                    crossbowTextureData.getWithFirework().getModel(),
                    namespace,
                    crossbowTextureData.getModel(),
                    namespace,
                    crossbowTextureData.getPulling_1().getModel(),
                    namespace,
                    crossbowTextureData.getPulling_2().getModel(),
                    namespace,
                    crossbowTextureData.getPulling_0().getModel()
            );
        } else if (data.getTridentTextureData() != null) {
            TridentTextureData tridentData = data.getTridentTextureData();
            normalizeData(file, tridentData, namespace, outputDirectory);

            jsonData = String.format(
                    readResource("/models/trident.json"),
                    namespace,
                    tridentData.getModel(),
                    namespace,
                    tridentData.getInHand().getModel(),
                    namespace,
                    tridentData.getThrowing().getModel()
            );
        } else if (data.getShieldBlockingData() != null) {
            final String blockingModel = convertTextureData(file, data.getShieldBlockingData(), namespace, "shield", outputDirectory);
            jsonData = String.format(readResource("/models/shield.json"), namespace, modelName, namespace, blockingModel);
        } else {
            jsonData = String.format(readResource("/models/default.json"), namespace, modelName);
        }
        jsonPath.getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(jsonPath.toFile())) {
            writer.write(jsonData);
        }
    }

    private String guessParent(ParsedTextureProperties data) {
        if (allHaveAnySuffix(data.getItems(), List.of("mace"))) {
            return "handheld_mace";
        }
        if (allHaveAnySuffix(data.getItems(), List.of("carrot_on_a_stick", "fishing_rod", "warped_fungus_on_a_stick"))) {
            return "handheld_rod";
        }
        if (allHaveAnySuffix(data.getItems(), List.of(
                "_axe", "_pickaxe", "_sword", "_rod", "_hoe", "_shovel", "stick", "bone", "bamboo"
        ))) {
            return "handheld";
        }
        return "generated";
    }

    private void normalizeData(Path file, TextureData data, String namespace, Path outputDirectory) throws IOException {
        TextureData first = null;
        final String parent;
        if (data instanceof CrossbowTextureData) {
            parent = "crossbow";
        } else if (data instanceof BowTextureData) {
            parent = "bow";
        } else {
            parent = "generated";
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
        if (data instanceof BowTextureData bow) {
            if (bow.getPulling_0() == null) {
                bow.setPulling_0(first);
            }
            if (bow.getPulling_1() == null) {
                bow.setPulling_1(first);
            }
            if (bow.getPulling_2() == null) {
                bow.setPulling_2(first);
            }
        }
        if (data instanceof CrossbowTextureData crossbow) {
            if (crossbow.getWithArrow() == null) {
                crossbow.setWithArrow(first);
            }
            if (crossbow.getWithFirework() == null) {
                crossbow.setWithFirework(first);
            }
        }
        if (data instanceof TridentTextureData trident) {
            if (trident.getInHand() == null) {
                trident.setInHand(first);
            }
            if (trident.getThrowing() == null) {
                trident.setThrowing(first);
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
            return armorTextureToModel(file, namespace, texture, type, outputDirectory);
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
            writer.write(String.format(readResource("/models/item.json"), parent, namespace, textureName));
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
                writer.write(String.format(
                        readResource("/models/armor.json"),
                        namespace,
                        armorTextureName,
                        namespace,
                        armorTextureName,
                        namespace,
                        armorTextureName
                ));
            } else {
                writer.write(String.format(
                        readResource("/models/elytra.json"),
                        namespace,
                        armorTextureName
                ));
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
                texture.replaceFirst(":", "/textures/"),
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

        Path newPath = copyResource(
                inputDirectory,
                model.replaceFirst(":", "/models/"),
                "json",
                modelDirectory,
                textureName
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

    private Path copyResource(Path inputDirectory, String resource, String extension, Path outputDirectory, String outputSuffix) throws IOException {
        Map.Entry<String, String> pair = getFilenameWithoutAndWithExtension(resource, extension);
        resource = pair.getValue();
        Path oldPath = resolveResource(inputDirectory, resource, extension.equals("json") ? ResourceType.MODEL : ResourceType.TEXTURE);
        if (oldPath == null) {
            log("Missing resource: " + resource + " (path: " + inputDirectory + ")");
            return null;
        }
        String outputName = String.join("_", pair.getKey().split("/"));
        if (outputSuffix != null) {
            outputName += "_" + outputSuffix;
        }
        Path newPath = outputDirectory.resolve(getFilenameWithoutAndWithExtension(outputName.toLowerCase(), extension).getValue());
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

    private String readResource(String path) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assert input != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line.strip());
                }
                return builder.toString();
            }
        }
    }

    private void log(String text) {
        logger.accept(text);
    }
}
