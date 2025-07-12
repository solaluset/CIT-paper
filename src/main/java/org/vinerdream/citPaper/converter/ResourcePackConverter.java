package org.vinerdream.citPaper.converter;

import com.google.gson.*;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.vinerdream.citPaper.utils.FileUtils;
import org.vinerdream.citPaper.utils.PropertiesUtils;
import org.vinerdream.citPaper.utils.ZipUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import static org.vinerdream.citPaper.utils.CollectionUtils.*;

public class ResourcePackConverter {
    private static final Set<String> DEFAULT_MODEL_DIRECTORIES = Set.of("builtin", "item");

    private final Path resourcePackPath;
    private final Path resultPath;
    private final boolean preserveCitDirectories;
    private final Path tempPath;
    private final Consumer<String> logger;
    private final String namespace;
    private final List<ParsedTextureProperties> convertedEntries = new ArrayList<>();

    public ResourcePackConverter(Path resourcePackPath, Path resultPath, Path tempPath, boolean preserveCitDirectories, Consumer<String> logger) {
        this.resourcePackPath = resourcePackPath;
        this.resultPath = resultPath;
        this.tempPath = tempPath;
        this.preserveCitDirectories = preserveCitDirectories;
        this.logger = logger;
        final CRC32 crc32 = new CRC32();
        crc32.update(resourcePackPath.getFileName().toString().getBytes(StandardCharsets.UTF_8));
        this.namespace = "cit-" + String.format("%08x", crc32.getValue());
    }

    public void convertResourcePack() throws IOException {
        Path rootPath = resourcePackPath;
        if (rootPath.toFile().isFile()) {
            Path newPath = getTmpDir().resolve(UUID.randomUUID().toString());
            ZipUtils.unzip(rootPath, newPath);
            rootPath = newPath;
        }
        Path outputPath = resultPath;
        final Path zipPath;
        if (outputPath.toString().endsWith(".zip")) {
            zipPath = outputPath;
            outputPath = getTmpDir().resolve(UUID.randomUUID().toString());
        } else {
            zipPath = null;
        }

        final JsonObject meta;
        try (FileReader reader = new FileReader(rootPath.resolve("pack.mcmeta").toFile())) {
            meta = new Gson().fromJson(reader, JsonObject.class);
        }

        FileUtils.removeDirectory(outputPath);
        FileUtils.copyDirectory(rootPath, outputPath);

        convertCitDirectories(rootPath, outputPath);
        if (meta.get("overlays") != null && meta.get("overlays").isJsonObject()) {
            final JsonObject overlays = meta.getAsJsonObject("overlays");
            if (overlays.get("entries") != null && overlays.get("entries").isJsonArray()) {
                final JsonArray entries = overlays.getAsJsonArray("entries");
                for (var entry : entries) {
                    if (entry.isJsonObject()) {
                        final var directory = entry.getAsJsonObject().get("directory");
                        if (directory != null) {
                            final String directoryName = directory.getAsString();
                            convertCitDirectories(rootPath.resolve(directoryName), outputPath.resolve(directoryName));
                        }
                    }
                }
            }
        }

        if (zipPath != null) {
            ZipUtils.zip(outputPath, zipPath);
        }

        FileUtils.removeDirectory(getTmpDir());
    }

    public void convertCitDirectories(Path rootPath, Path outputPath) throws IOException {
        Path directory = rootPath.resolve("assets").resolve("minecraft");
        convertDirectory(directory.resolve("optifine").resolve("cit"), outputPath);
        convertDirectory(directory.resolve("mcpatcher").resolve("cit"), outputPath);
        if (!preserveCitDirectories) {
            directory = outputPath.resolve("assets").resolve("minecraft");
            FileUtils.removeDirectory(directory.resolve("optifine").resolve("cit"));
            FileUtils.removeDirectory(directory.resolve("mcpatcher").resolve("cit"));
        }
    }

    public void convertDirectory(Path directory, Path outputDirectory) throws IOException {
        log("Converting " + directory);
        File dir = directory.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            log("Directory not found, skipping");
            return;
        }
        try (Stream<Path> contents = Files.walk(directory)) {
            for (Path path : iterateStream(contents)) {
                if (!path.toFile().isFile()) {
                    continue;
                }
                if (path.toString().endsWith(".properties")) {
                    convertFile(directory, path, outputDirectory);
                }
            }
        }
    }

    public void convertFile(Path citRoot, Path file, Path outputDirectory) throws IOException {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader(file.toFile())) {
            properties.load(reader);
        }
        Map<String, String> propertiesMap = PropertiesUtils.propertiesToMap(properties);
        propertiesMap.replaceAll((k, v) -> v.trim());

        ParsedTextureProperties data = new ParsedTextureProperties(propertiesMap, string -> logger.accept(file + ": " + string));
        for (String key : propertiesMap.keySet()) {
            log("Unknown property in " + file + ": " + key);
        }

        try {
            convertPropertiesFile(citRoot, file, data, outputDirectory);
        } catch (Exception e) {
            log("Error when converting " + file);
            throw e;
        }
        convertedEntries.add(data);
    }

    private void convertPropertiesFile(Path citRoot, Path file, ParsedTextureProperties data, Path outputDirectory) throws IOException {
        final String path = file.getFileName().toString().replaceFirst("\\.properties$", "");
        final Path prefix = lowerPath(citRoot.relativize(file).getParent());
        final String prefixString = prefixToString(prefix);

        if (data.getKey() == null) {
            data.setKey(new NamespacedKey(namespace, prefixString + path.toLowerCase(Locale.ROOT)));
        } else return;

        if (!data.hasAnyData()) {
            if (file.getParent().resolve(path + ".json").toFile().isFile()) {
                data.setMainTextureData(new TextureData(path, null));
            } else if (file.getParent().resolve(path + ".png").toFile().isFile()) {
                data.setMainTextureData(new TextureData(null, path));
            }
        }

        if (data.getArmorData() != null) {
            final String armorModel = convertArmorTextureData(file, data.getArmorData(), data.getArmorDataType(), data.getMainTextureData() != null ? data.getMainTextureData().getTexture() : null, outputDirectory, prefix);

            data.getArmorData().setModel(namespace + ":" + armorModel);
        }

        final String modelName;
        if (data.getMainTextureData() != null) {
            modelName = convertTextureData(file, data.getMainTextureData(), guessParent(data), null, outputDirectory, prefix);
        } else return;

        final Path jsonPath = outputDirectory.resolve(Paths.get("assets", namespace, "items")).resolve(prefix).resolve(path.toLowerCase(Locale.ROOT) + ".json");
        final String jsonData;

        if (data.getBowTextureData() != null) {
            BowTextureData bowTextureData = data.getBowTextureData();
            normalizeData(file, bowTextureData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

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
            normalizeData(file, crossbowTextureData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

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
            normalizeData(file, tridentData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

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
            normalizeData(file, fishingRodData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

            jsonData = String.format(
                    readResource("/models/fishing_rod.json"),
                    namespace,
                    fishingRodData.getModel(),
                    namespace,
                    fishingRodData.getCast().getModel()
            );
        } else if (data.getElytraTextureData() != null) {
            ElytraTextureData elytraData = data.getElytraTextureData();
            normalizeData(file, elytraData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

            jsonData = String.format(
                    readResource("/models/elytra.json"),
                    namespace,
                    elytraData.getModel(),
                    namespace,
                    elytraData.getBroken().getModel()
            );
        } else if (data.getShieldBlockingData() != null) {
            final String blockingModel = convertTextureData(file, data.getShieldBlockingData(), "shield", data.getMainTextureData().getTexture(), outputDirectory, prefix);
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

    private void normalizeData(Path file, @NotNull TextureData data, String fallbackTexture, Path outputDirectory, Path prefix) throws IOException {
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
            data1.setModel(convertTextureData(file, data1, parent, fallbackTexture, outputDirectory, prefix));
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

    private String convertTextureData(Path file, TextureData data, String parent, String fallbackTexture, Path outputDirectory, Path prefix) throws IOException {
        final String model = removeExtension(data.getModel());
        final String texture;
        if (data.getTexture() != null) {
            texture = removeExtension(data.getTexture());
        } else {
            texture = removeExtension(fallbackTexture);
        }
        final String overlay = removeExtension(data.getOverlay());

        final String prefixString = prefixToString(prefix);

        if (model == null) {
            if (texture == null) {
                return null;
            }
            final Path texturePath = copyTexture(List.of(file.getParent()), texture, outputDirectory, prefix);
            if (texturePath == null) return null;
            final String textureName = resourceNameFromPath(texturePath);
            String overlayName = null;
            if (overlay != null) {
                final Path overlayPath = copyTexture(List.of(file.getParent()), overlay, outputDirectory, prefix);
                if (overlayPath != null) {
                    overlayName = resourceNameFromPath(overlayPath);
                }
            }

            return textureToModel(textureName, overlayName, parent, outputDirectory, prefix);
        }
        final Path modelPath = copyModel(file.getParent(), model, texture, outputDirectory, prefix);
        if (modelPath == null) return null;

        return prefixString + resourceNameFromPath(modelPath);
    }

    private String convertArmorTextureData(Path file, TextureData data, int type, String fallbackTexture, Path outputDirectory, Path prefix) throws IOException {
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
            return armorTextureToModel(file, texture, type, overlay, outputDirectory, prefix);
        }
        final Path modelPath = copyResource(
                List.of(file.getParent()),
                texture.replaceFirst(":", "/models/"),
                "json",
                outputDirectory.resolve(Paths.get("assets", namespace, "equipment")),
                prefix
        );
        if (modelPath == null) return null;

        return prefixToString(prefix) + resourceNameFromPath(modelPath);
    }

    private String resourceNameFromPath(Path path) {
        return removeExtension(path.getFileName().toString());
    }

    private String removeExtension(String filename) {
        if (filename == null) return null;
        return filename.replaceFirst("\\.[^/]+$", "");
    }

    private String textureToModel(String textureName, String overlayName, String parent, Path outputDirectory, Path prefix) throws IOException {
        final Path tmpModelPath = getTmpDir().resolve("models").resolve(textureName + ".json");
        final String prefixString = prefixToString(prefix);
        tmpModelPath.getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(tmpModelPath.toFile())) {
            if (overlayName == null) {
                writer.write(String.format(readResource("/models/item.json"), parent, namespace, prefixString + textureName));
            } else {
                writer.write(String.format(readResource("/models/item_with_overlay.json"), parent, namespace, prefixString + textureName, namespace, prefixString + overlayName));
            }
        }

        return prefixString + removeExtension(Objects.requireNonNull(
                copyModel(tmpModelPath.getParent(), textureName, false, null, outputDirectory, prefix),
                "Failed to copy generated model!"
        ).getFileName().toString());
    }

    private String armorTextureToModel(Path file, String texture, int type, String overlay, Path outputDirectory, Path prefix) throws IOException {
        final Path armorTexturePath = copyArmorTexture(
                file.getParent(),
                texture,
                type,
                outputDirectory,
                prefix
        );
        if (armorTexturePath == null) return null;
        final String armorTextureName = getFilenameWithoutAndWithExtension(
                armorTexturePath.getFileName().toString(), "png"
        ).getKey();
        final String armorOverlayName;
        if (overlay != null) {
            final Path armorOverlayPath = copyArmorTexture(
                    file.getParent(),
                    overlay,
                    type,
                    outputDirectory,
                    prefix
            );
            if (armorOverlayPath != null) {
                armorOverlayName = getFilenameWithoutAndWithExtension(
                        armorOverlayPath.getFileName().toString(), "png"
                ).getKey();
            } else {
                armorOverlayName = null;
            }
        } else {
            armorOverlayName = null;
        }
        final String prefixString = prefixToString(prefix);
        final Path modelPath = outputDirectory.resolve(
                Paths.get("assets", namespace, "equipment")
        ).resolve(prefix).resolve(armorTextureName + ".json");
        modelPath.getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(modelPath.toFile())) {
            if (type != 3) {
                if (armorOverlayName != null) {
                    writer.write(String.format(
                            readResource("/models/armor_with_overlay.json"),
                            namespace,
                            prefixString + armorTextureName,
                            namespace,
                            prefixString + armorOverlayName,
                            namespace,
                            prefixString + armorTextureName,
                            namespace,
                            prefixString + armorOverlayName,
                            namespace,
                            prefixString + armorTextureName,
                            namespace,
                            prefixString + armorOverlayName
                    ));
                } else {
                    writer.write(String.format(
                            readResource("/models/armor.json"),
                            namespace,
                            prefixString + armorTextureName,
                            namespace,
                            prefixString + armorTextureName,
                            namespace,
                            prefixString + armorTextureName
                    ));
                }
            } else {
                writer.write(String.format(
                        readResource("/models/armor_elytra.json"),
                        namespace,
                        prefixString + armorTextureName
                ));
            }
        }
        return prefixString + resourceNameFromPath(modelPath);
    }

    private Path copyTexture(List<Path> inputDirectories, String texture, Path outputDirectory, Path prefix) throws IOException {
        return copyResource(
                inputDirectories,
                texture.replaceFirst(":", "/textures/"),
                "png",
                outputDirectory.resolve(Paths.get(
                        "assets",
                        namespace,
                        "textures",
                        "item"
                )),
                prefix
        );
    }

    private Path copyArmorTexture(Path inputDirectory, String texture, int textureType, Path outputDirectory, Path prefix) throws IOException {
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
                )),
                prefix
        );
    }

    private Path copyModel(Path inputDirectory, String model, String textureName, Path outputDirectory, Path prefix) throws IOException {
        return copyModel(inputDirectory, model, true, textureName, outputDirectory, prefix);
    }

    private Path copyModel(Path inputDirectory, String model, boolean processTextures, String textureName, Path outputDirectory, Path prefix) throws IOException {
        final Path modelDirectory = outputDirectory.resolve(Paths.get(
                "assets",
                namespace,
                "models",
                "item"
        ));
        final String prefixString = prefixToString(prefix);

        final Path texturePath = textureName != null ? resolveResource(inputDirectory, getFilenameWithoutAndWithExtension(textureName, "png").getValue(), ResourceType.TEXTURE) : null;
        model = model.replaceFirst(":", "/models/");
        Path newPath = copyResource(
                List.of(inputDirectory),
                model,
                "json",
                modelDirectory,
                prefix
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
                final Path parentModel = copyModel(inputDirectory, parent.getAsString(), null, outputDirectory, prefix);
                if (parentModel != null) {
                    json.addProperty("parent", namespace + ":item/" + prefixString + resourceNameFromPath(parentModel));
                }
            }
        }
        if (texturePath != null) {
            fixTextures(inputDirectory, model, json, null, outputDirectory, prefix);
            try (FileWriter writer = new FileWriter(newPath.toFile())) {
                writer.write(new Gson().toJson(json));
            }
            final JsonObject newJson = new JsonObject();
            newJson.addProperty("parent", namespace + ":item/" + prefixString + resourceNameFromPath(newPath));
            if (json.get("textures") != null) {
                newJson.add("textures", json.get("textures"));
            }
            json = newJson;
            newPath = newPath.getParent().resolve(
                    getFilenameWithoutAndWithExtension(newPath.getFileName().toString(), "json").getKey()
                            + "_" + removeExtension(texturePath.getFileName().toString()) + ".json"
            );
        }
        fixTextures(inputDirectory, model, json, textureName, outputDirectory, prefix);
        try (FileWriter writer = new FileWriter(newPath.toFile())) {
            writer.write(new Gson().toJson(json));
        }
        return newPath;
    }

    private void fixTextures(Path inputDirectory, String modelName, JsonObject model, String textureName, Path outputDirectory, Path prefix) throws IOException {
        JsonElement texturesElement = model.get("textures");
        if (texturesElement != null) {
            JsonObject textures = texturesElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                Path outputTexture = copyTexture(List.of(inputDirectory, resolveOldPath(inputDirectory, modelName, "json").getParent()), textureName == null ? entry.getValue().getAsString() : textureName, outputDirectory, prefix);
                if (outputTexture == null) continue;
                textures.addProperty(
                        entry.getKey(),
                        namespace
                                + ":item/" + prefixToString(prefix) + removeExtension(outputTexture.getFileName().toString())
                );
            }
        }
    }

    private Path copyResource(List<Path> inputDirectories, String resource, String extension, Path outputDirectory, Path prefix) throws IOException {
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
            if (!DEFAULT_MODEL_DIRECTORIES.contains(stringToPath(resource).getName(0).toString())) {
                log("Missing resource: " + resource + " (searched in: " + inputDirectories + ")");
            }
            return null;
        }
        String outputName = getFilenameWithoutAndWithExtension(joinPath(foundDirectory.relativize(oldPath)), extension).getKey();
        Path newPath = outputDirectory.resolve(prefix).resolve(getFilenameWithoutAndWithExtension(outputName.toLowerCase(Locale.ROOT), extension).getValue());
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

    private Path lowerPath(Path path) {
        final List<String> parts = new ArrayList<>();
        do {
            parts.addFirst(path.getFileName().toString().toLowerCase(Locale.ROOT));
            path = path.getParent();
        } while (path != null);
        final String first = parts.removeFirst();
        return Path.of(first, parts.toArray(String[]::new));
    }

    private String prefixToString(Path prefix) {
        return prefix.toString().replace(File.separator, "/") + "/";
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
        config.set("renames", convertedEntries.stream().map(ParsedTextureProperties::saveToMap).toList());
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
            try (InputStreamReader inputStreamReader = new InputStreamReader(input)) {
                try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line.strip());
                    }
                    return builder.toString();
                }
            }
        }
    }

    private void log(String text) {
        logger.accept(text);
    }
}
