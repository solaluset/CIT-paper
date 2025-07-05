package org.vinerdream.citPaper.converter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
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
    private final Path tempPath;

    public ResourcePackConverter(Consumer<String> logger, Path tempPath) {
        this.logger = logger;
        convertedEntries = new ArrayList<>();
        this.tempPath = tempPath;
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
        convertedEntries.add(data);
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
            final String armorModel = convertArmorTextureData(file, data.getArmorData(), namespace, data.getArmorDataType(), data.getMainTextureData() != null ? data.getMainTextureData().getTexture() : null, outputDirectory);

            data.getArmorData().setModel(namespace + ":" + armorModel);
        }

        final String modelName;
        if (data.getMainTextureData() != null) {
            modelName = convertTextureData(file, data.getMainTextureData(), namespace, guessParent(data), null, outputDirectory);
        } else return;

        final Path jsonPath = outputDirectory.resolve(Paths.get("assets", namespace, "items", path.toLowerCase() + ".json"));
        final String jsonData;

        if (data.getBowTextureData() != null) {
            BowTextureData bowTextureData = data.getBowTextureData();
            normalizeData(file, bowTextureData, namespace, data.getMainTextureData().getTexture(), outputDirectory);

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
            normalizeData(file, crossbowTextureData, namespace, data.getMainTextureData().getTexture(), outputDirectory);

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
            normalizeData(file, tridentData, namespace, data.getMainTextureData().getTexture(), outputDirectory);

            jsonData = String.format(
                    readResource("/models/trident.json"),
                    namespace,
                    tridentData.getModel(),
                    namespace,
                    tridentData.getInHand().getModel(),
                    namespace,
                    tridentData.getThrowing().getModel()
            );
        } else if (data.getFishingRodTextureData() != null) {
            FishingRodTextureData fishingRodData = data.getFishingRodTextureData();
            normalizeData(file, fishingRodData, namespace, data.getMainTextureData().getTexture(), outputDirectory);

            jsonData = String.format(
                    readResource("/models/fishing_rod.json"),
                    namespace,
                    fishingRodData.getModel(),
                    namespace,
                    fishingRodData.getCast().getModel()
            );
        } else if (data.getElytraTextureData() != null) {
            ElytraTextureData elytraData = data.getElytraTextureData();
            normalizeData(file, elytraData, namespace, data.getMainTextureData().getTexture(), outputDirectory);

            jsonData = String.format(
                    readResource("/models/elytra.json"),
                    namespace,
                    elytraData.getModel(),
                    namespace,
                    elytraData.getBroken().getModel()
            );
        } else if (data.getShieldBlockingData() != null) {
            final String blockingModel = convertTextureData(file, data.getShieldBlockingData(), namespace, "shield", data.getMainTextureData().getTexture(), outputDirectory);
            jsonData = String.format(readResource("/models/shield.json"), namespace, modelName, namespace, blockingModel);
        } else {
            if (modelName != null) {
                jsonData = String.format(readResource("/models/default.json"), namespace + ":item/" + modelName);
            } else {
                jsonData = String.format(readResource("/models/default.json"), data.getMainTextureData().getModel());
            }
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

    private void normalizeData(Path file, @NotNull TextureData data, String namespace, String fallbackTexture, Path outputDirectory) throws IOException {
        TextureData first = null;
        final String parent = switch (data) {
            case CrossbowTextureData ignored -> "crossbow";
            case BowTextureData ignored -> "bow";
            case FishingRodTextureData ignored -> "handheld_rod";
            default -> "generated";
        };
        for (TextureData data1 : data.getAll()) {
            if (data1 == null) continue;
            if (first == null) {
                first = data1;
            }
            data1.setModel(convertTextureData(file, data1, namespace, parent, fallbackTexture, outputDirectory));
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
        if (data instanceof FishingRodTextureData fishingRod) {
            if (fishingRod.getCast() == null) {
                fishingRod.setCast(first);
            }
        }
        if (data instanceof ElytraTextureData elytra) {
            if (elytra.getBroken() == null) {
                elytra.setBroken(first);
            }
        }
    }

    private String convertTextureData(Path file, TextureData data, String namespace, String parent, String fallbackTexture, Path outputDirectory) throws IOException {
        final String model = removeExtension(data.getModel());
        final String texture;
        if (data.getTexture() != null) {
            texture = removeExtension(data.getTexture());
        } else {
            texture = removeExtension(fallbackTexture);
        }
        final String overlay = removeExtension(data.getOverlay());

        if (model == null) {
            if (texture == null) {
                return null;
            }
            final Path texturePath = copyTexture(List.of(file.getParent()), namespace, texture, outputDirectory);
            if (texturePath == null) return null;
            final String textureName = resourceNameFromPath(texturePath);
            String overlayName = null;
            if (overlay != null) {
                final Path overlayPath = copyTexture(List.of(file.getParent()), namespace, overlay, outputDirectory);
                if (overlayPath != null) {
                    overlayName = resourceNameFromPath(overlayPath);
                }
            }

            return textureToModel(namespace, textureName, overlayName, parent, outputDirectory);
        }
        final Path modelPath = copyModel(file.getParent(), namespace, model, texture, outputDirectory);
        if (modelPath == null) return null;

        return resourceNameFromPath(modelPath);
    }

    private String convertArmorTextureData(Path file, TextureData data, String namespace, int type, String fallbackTexture, Path outputDirectory) throws IOException {
        final String model = removeExtension(data.getModel());
        final String texture;
        if (data.getTexture() != null) {
            texture = removeExtension(data.getTexture());
        } else {
            texture = fallbackTexture;
        }
        final String overlay = removeExtension(data.getOverlay());

        if (model == null) {
            if (texture == null) {
                return null;
            }
            return armorTextureToModel(file, namespace, texture, type, overlay, outputDirectory);
        }
        final Path modelPath = copyResource(
                List.of(file.getParent()),
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

    private String textureToModel(String namespace, String textureName, String overlayName, String parent, Path outputDirectory) throws IOException {
        final Path tmpModelPath = getTmpDir().resolve("models").resolve(textureName + ".json");
        tmpModelPath.getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(tmpModelPath.toFile())) {
            if (overlayName == null) {
                writer.write(String.format(readResource("/models/item.json"), parent, namespace, textureName));
            } else {
                writer.write(String.format(readResource("/models/item_with_overlay.json"), parent, namespace, textureName, namespace, overlayName));
            }
        }

        return getFilenameWithoutAndWithExtension(Objects.requireNonNull(
                copyModel(tmpModelPath.getParent(), namespace, textureName, false, null, outputDirectory),
                "Failed to copy generated model!"
        ).getFileName().toString(), "json").getKey();
    }

    private String armorTextureToModel(Path file, String namespace, String texture, int type, String overlay, Path outputDirectory) throws IOException {
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
        final String armorOverlayName;
        if (overlay != null) {
            final Path armorOverlayPath = copyArmorTexture(
                    file.getParent(),
                    namespace,
                    overlay,
                    type,
                    outputDirectory
            );
            armorOverlayName = getFilenameWithoutAndWithExtension(
                    armorOverlayPath.getFileName().toString(), "png"
            ).getKey();
        } else {
            armorOverlayName = null;
        }
        final Path modelPath = outputDirectory.resolve(
                Paths.get("assets", namespace, "equipment", armorTextureName + ".json")
        );
        modelPath.getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(modelPath.toFile())) {
            if (type != 3) {
                if (armorOverlayName != null) {
                    writer.write(String.format(
                            readResource("/models/armor_with_overlay.json"),
                            namespace,
                            armorTextureName,
                            namespace,
                            armorOverlayName,
                            namespace,
                            armorTextureName,
                            namespace,
                            armorOverlayName,
                            namespace,
                            armorTextureName,
                            namespace,
                            armorOverlayName
                    ));
                } else {
                    writer.write(String.format(
                            readResource("/models/armor.json"),
                            namespace,
                            armorTextureName,
                            namespace,
                            armorTextureName,
                            namespace,
                            armorTextureName
                    ));
                }
            } else {
                writer.write(String.format(
                        readResource("/models/armor_elytra.json"),
                        namespace,
                        armorTextureName
                ));
            }
        }
        return resourceNameFromPath(modelPath);
    }

    private Path copyTexture(List<Path> inputDirectories, String namespace, String texture, Path outputDirectory) throws IOException {
        return copyResource(
                inputDirectories,
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
                List.of(inputDirectory),
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

        final Path texturePath = textureName != null ? resolveResource(inputDirectory, getFilenameWithoutAndWithExtension(textureName, "png").getValue(), ResourceType.TEXTURE) : null;
        model = model.replaceFirst(":", "/models/");
        Path newPath = copyResource(
                List.of(inputDirectory),
                model,
                "json",
                modelDirectory
        );
        if (newPath == null || !processTextures) return newPath;
        JsonObject json;
        try (FileReader reader = new FileReader(newPath.toFile())) {
            try {
                json = new Gson().fromJson(reader, JsonObject.class);
            } catch (JsonSyntaxException ignored) {
                log("Invalid JSON: " + newPath);
                return newPath;
            }
            final JsonElement parent = json.get("parent");
            if (parent != null) {
                final Path parentModel = copyModel(inputDirectory, namespace, parent.getAsString(), null, outputDirectory);
                if (parentModel != null) {
                    json.addProperty("parent", namespace + ":item/" + resourceNameFromPath(parentModel));
                }
            }
        }
        if (texturePath != null) {
            fixTextures(inputDirectory, namespace, model, json, null, outputDirectory);
            try (FileWriter writer = new FileWriter(newPath.toFile())) {
                writer.write(new Gson().toJson(json));
            }
            final JsonObject newJson = new JsonObject();
            newJson.addProperty("parent", namespace + ":item/" + resourceNameFromPath(newPath));
            if (json.get("textures") != null) {
                newJson.add("textures", json.get("textures"));
            }
            json = newJson;
            newPath = newPath.getParent().resolve(
                    getFilenameWithoutAndWithExtension(newPath.getFileName().toString(), "json").getKey()
                            + "_" + removeExtension(texturePath.getFileName().toString()) + ".json"
            );
        }
        fixTextures(inputDirectory, namespace, model, json, textureName, outputDirectory);
        try (FileWriter writer = new FileWriter(newPath.toFile())) {
            writer.write(new Gson().toJson(json));
        }
        return newPath;
    }

    private void fixTextures(Path inputDirectory, String namespace, String modelName, JsonObject model, String textureName, Path outputDirectory) throws IOException {
        JsonElement texturesElement = model.get("textures");
        if (texturesElement != null) {
            JsonObject textures = texturesElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                Path outputTexture = copyTexture(List.of(inputDirectory, resolveOldPath(inputDirectory, modelName, "json").getParent()), namespace, textureName == null ? entry.getValue().getAsString() : textureName, outputDirectory);
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
    }

    private Path copyResource(List<Path> inputDirectories, String resource, String extension, Path outputDirectory) throws IOException {
        Path foundDirectory = null;
        Path oldPath = null;
        for (Path inputDirectory : inputDirectories) {
            oldPath = resolveOldPath(inputDirectory, resource, extension);
            if (oldPath != null) {
                foundDirectory = inputDirectory;
                break;
            }
        }
        if (oldPath == null) {
            log("Missing resource: " + resource + " (searched in: " + inputDirectories + ")");
            return null;
        }
        String outputName = getFilenameWithoutAndWithExtension(joinPath(foundDirectory.relativize(oldPath)), extension).getKey();
        Path newPath = outputDirectory.resolve(getFilenameWithoutAndWithExtension(outputName.toLowerCase(), extension).getValue());
        newPath.getParent().toFile().mkdirs();
        Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        if (addMcmeta(oldPath).toFile().isFile()) {
            Files.copy(addMcmeta(oldPath), addMcmeta(newPath), StandardCopyOption.REPLACE_EXISTING);
        }
        return newPath;
    }

    private String joinPath(Path path) {
        final StringBuilder builder = new StringBuilder();
        while (path != null) {
            final String part = path.getFileName().toString();
            if (!part.equals(".") && !part.equals("..")) {
                builder.insert(0, part);
                builder.insert(0, '_');
            }
            path = path.getParent();
        }
        return builder.substring(1);
    }

    private Path addMcmeta(Path path) {
        return path.getParent().resolve(path.getFileName() + ".mcmeta");
    }

    private Path resolveOldPath(Path inputDirectory, String resource, String extension) throws IOException {
        Map.Entry<String, String> pair = getFilenameWithoutAndWithExtension(resource, extension);
        return resolveResource(inputDirectory, pair.getValue(), extension.equals("json") ? ResourceType.MODEL : ResourceType.TEXTURE);
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
        return tempPath;
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
