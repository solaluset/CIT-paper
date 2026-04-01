package org.vinerdream.citPaper.converter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.*;
import com.nexomc.nexo.api.NexoPack;
import com.nexomc.nexo.pack.creative.NexoPackReader;
import com.nexomc.nexo.utils.logs.Logs;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vinerdream.citPaper.config.MainConfig;
import org.vinerdream.citPaper.config.Mode;
import org.vinerdream.citPaper.exceptions.UnsupportedCitTypeException;
import org.vinerdream.citPaper.utils.FileUtils;
import org.vinerdream.citPaper.utils.ZipUtils;
import team.unnamed.creative.ResourcePack;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import static org.vinerdream.citPaper.utils.CollectionUtils.*;
import static org.vinerdream.citPaper.utils.ItemUtils.*;
import static org.vinerdream.citPaper.utils.ReflectionUtils.readResource;

public class ResourcePackConverter {
    private static final Set<String> DEFAULT_MODEL_DIRECTORIES = Set.of("builtin", "item", "items");

    private final Mode mode;
    private final Path resourcePackPath;
    private final Path resultPath;
    private final boolean preserveCitDirectories;
    private final Path tempPath;
    private final Logger logger;
    private final ResourcePack resourcePackToMerge;
    private final String namespace;
    private final List<Map<String, String>> renames = new ArrayList<>();
    private final List<Map.Entry<Level, String>> logMessages = new ArrayList<>();
    private final @Nullable YamlConfiguration oraxenConfig;
    private final String oraxenArmorType;
    private final boolean verboseLogging;
    private int warningCounter = 0;

    public ResourcePackConverter(MainConfig config, Path resourcePackPath, Path resultPath, Logger logger, @Nullable ResourcePack resourcePackToMerge) {
        this.mode = config.getMode();
        this.resourcePackPath = resourcePackPath;
        this.resultPath = resultPath;
        this.tempPath = config.getTempFolder();
        this.preserveCitDirectories = config.isConverterPreserveCitDirectories();
        this.logger = logger;
        this.resourcePackToMerge = resourcePackToMerge;
        final CRC32 crc32 = new CRC32();
        crc32.update(resourcePackPath.getFileName().toString().getBytes(StandardCharsets.UTF_8));
        this.namespace = "cit-" + String.format("%08x", crc32.getValue());
        this.oraxenConfig = this.mode == Mode.ORAXEN ? new YamlConfiguration() : null;
        this.oraxenArmorType = config.getOraxenArmorType();
        this.verboseLogging = config.isVerboseLogging();
    }

    public void convertResourcePack() throws IOException {
        logger.info("Starting conversion of " + resourcePackPath);

        Path rootPath = resourcePackPath;
        Path outputPath = resultPath;
        final Path zipPath;
        if (rootPath.toFile().isFile()) {
            Path newPath = getTmpPackDir();
            ZipUtils.unzip(rootPath, newPath);
            rootPath = newPath;
            zipPath = outputPath;
            outputPath = getTmpPackDir();
        } else {
            zipPath = null;
            if (resourcePackToMerge != null) {
                outputPath = getTmpPackDir();
            }
        }

        final @Nullable JsonObject meta;
        final File mcmetaFile = rootPath.resolve("pack.mcmeta").toFile();
        if (mcmetaFile.isFile()) {
            try (FileReader reader = new FileReader(mcmetaFile)) {
                meta = new Gson().fromJson(reader, JsonObject.class);
                if (meta == null) {
                    throw new IllegalStateException("pack.mcmeta is malformed");
                }
            }
        } else {
            logger.warning("pack.mcmeta is missing");
            warningCounter++;
            meta = null;
        }

        FileUtils.removeDirectory(outputPath);
        FileUtils.copyDirectory(rootPath, outputPath);

        convertCitDirectories(rootPath, outputPath);

        if (meta != null && meta.get("overlays") != null && meta.get("overlays").isJsonObject()) {
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

        if (resourcePackToMerge != null) {
            Logs.logger = message -> {
                warningCounter++;
                if (verboseLogging) {
                    logger.log(Level.WARNING, message);
                }
            };
            NexoPack.mergePack(resourcePackToMerge, NexoPackReader.INSTANCE.readFromDirectory(outputPath.toFile()));
            Logs.logger = null;

        } else if (zipPath != null) {
            ZipUtils.zip(outputPath, zipPath);
        }

        FileUtils.removeDirectory(getTmpDir());

        logger.info("Conversion completed with " + warningCounter + " warnings.");
        warningCounter = 0;
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
        File dir = directory.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        try (Stream<Path> contents = Files.walk(directory)) {
            for (Path path : iterateStream(contents)) {
                if (!path.toFile().isFile()) {
                    continue;
                }
                if (path.toString().endsWith(".properties")) {
                    logMessages.clear();
                    try {
                        convertFile(directory, path, outputDirectory);
                    } finally {
                        warningCounter += logMessages.size();
                        if (verboseLogging) {
                            logMessages.forEach(entry -> logger.log(entry.getKey(), path + ": " + entry.getValue()));
                        }
                        logMessages.clear();
                    }
                }
            }
        }
    }

    public void convertFile(Path citRoot, Path file, Path outputDirectory) throws IOException {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader(file.toFile())) {
            properties.load(reader);
        }
        Map<String, String> propertiesMap = mapToStringMap(properties);
        propertiesMap.replaceAll((k, v) -> v.trim());

        final ParsedTextureProperties data;
        try {
            data = new ParsedTextureProperties(propertiesMap, string -> log(Level.WARNING, string));
        } catch (UnsupportedCitTypeException e) {
            log(Level.WARNING, e.getMessage());
            return;
        }

        try {
            renames.addAll(convertPropertiesFile(citRoot, file, data, outputDirectory));
        } catch (Exception e) {
            log(Level.SEVERE, "Error when converting");
            throw e;
        }
    }

    private List<Map<String, String>> convertPropertiesFile(Path citRoot, Path file, ParsedTextureProperties data, Path outputDirectory) throws IOException {
        final String path = file.getFileName().toString().replaceFirst("\\.properties$", "");
        final Path originalPrefix = citRoot.relativize(file).getParent();
        final Path prefix = lowerPath(originalPrefix != null ? originalPrefix : Path.of(""));
        final String prefixString = prefixToString(prefix);

        if (data.getKey() == null) {
            try {
                data.setKey(new NamespacedKey(namespace, prefixString + path.toLowerCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                log(Level.WARNING, e.getMessage());
                return List.of();
            }
        } else return List.of(data.saveToMap());

        if (!data.hasAnyData()) {
            if (file.getParent().resolve(path + ".json").toFile().isFile()) {
                data.setMainTextureData(new TextureData(path, null));
            } else if (file.getParent().resolve(path + ".png").toFile().isFile()) {
                data.setMainTextureData(new TextureData(null, path));
            }
        }

        if (data.getArmorData() != null) {
            final String armorModel = convertArmorTextureData(file, data.getArmorData(), data.getArmorDataType(), data.getMainTextureData() != null ? data.getMainTextureData().getTexture() : null, outputDirectory, prefix);

            final var ns = mode != Mode.ORAXEN ? namespace : "oraxen";
            data.getArmorData().setModel(ns + ":" + armorModel);
        }

        if (data.getMainTextureData() != null) {
            convertTextureData(file, data.getMainTextureData(), guessParent(data), null, outputDirectory, prefix);
        } else return List.of(data.saveToMap());

        if (mode == Mode.ORAXEN) {
            assert oraxenConfig != null;
            final ConfigurationSection itemConfig = oraxenConfig.createSection("temporary");
            final ConfigurationSection config = itemConfig.createSection("Pack");
            config.set("generate_model", false);

            if (data.getBowTextureData() != null) {
                BowTextureData bowTextureData = data.getBowTextureData();
                normalizeData(file, bowTextureData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

                config.set("model", bowTextureData.getModel());
                config.set("pulling_models", List.of(
                        bowTextureData.getPulling_0().getModel(),
                        bowTextureData.getPulling_1().getModel(),
                        bowTextureData.getPulling_2().getModel()
                ));

            } else if (data.getCrossbowTextureData() != null) {
                CrossbowTextureData crossbowTextureData = data.getCrossbowTextureData();
                normalizeData(file, crossbowTextureData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

                config.set("model", crossbowTextureData.getModel());
                config.set("pulling_models", List.of(
                        crossbowTextureData.getPulling_0().getModel(),
                        crossbowTextureData.getPulling_1().getModel(),
                        crossbowTextureData.getPulling_2().getModel()
                ));
                config.set("charged_model", crossbowTextureData.getWithArrow().getModel());
                config.set("firework_model", crossbowTextureData.getWithFirework().getModel());

            } else if (data.getTridentTextureData() != null) {
                TridentTextureData tridentData = data.getTridentTextureData();
                normalizeData(file, tridentData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

                // not supported by Oraxen
                config.set("model", tridentData.getModel());

            } else if (data.getFishingRodTextureData() != null) {
                FishingRodTextureData fishingRodData = data.getFishingRodTextureData();
                normalizeData(file, fishingRodData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

                config.set("model", fishingRodData.getModel());
                config.set("cast_model", fishingRodData.getCast().getModel());

            } else if (data.getElytraTextureData() != null) {
                ElytraTextureData elytraData = data.getElytraTextureData();
                normalizeData(file, elytraData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

                config.set("model", elytraData.getModel());

            } else if (data.getShieldTextureData() != null) {
                final ShieldTextureData shieldData = data.getShieldTextureData();
                normalizeData(file, shieldData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

                config.set("model", shieldData.getModel());
                config.set("blocking_model", shieldData.getBlocking().getModel());

            } else {
                config.set("model", data.getMainTextureData().getModel());
            }

            final List<Map<String, String>> renames = new ArrayList<>();
            final Map<String, String> baseMap = data.saveToMap();

            final Map<String, List<String>> materials = new HashMap<>();
            for (String item : data.getItems()) {
                final String material = getOraxenMaterial(oraxenArmorType, item).toUpperCase(Locale.ROOT);
                if (materials.containsKey(material)) {
                    materials.get(material).add(item);
                } else {
                    materials.put(material, new ArrayList<>(List.of(item)));
                }
            }

            for (var entry : materials.entrySet()) {
                final ConfigurationSection itemSection = oraxenConfig.createSection((namespace + "_" + entry.getKey() + "_" + prefixString.replace("/", "_") + path).toLowerCase(Locale.ROOT));
                itemSection.set("material", entry.getKey());
                itemSection.set("Pack", config);

                final Map<String, String> renameMap = new HashMap<>(baseMap);
                renameMap.put("items", String.join(" ", entry.getValue()));
                renameMap.put("oraxen_id", itemSection.getName());
                renames.add(renameMap);
            }
            oraxenConfig.set("temporary", null);

            return renames;
        }

        final Path jsonPath = outputDirectory.resolve(Path.of("assets", namespace, "items")).resolve(prefix).resolve(path.toLowerCase(Locale.ROOT) + ".json");
        final String jsonData;

        if (data.getBowTextureData() != null) {
            BowTextureData bowTextureData = data.getBowTextureData();
            normalizeData(file, bowTextureData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

            jsonData = String.format(
                    readResource("/models/bow.json"),
                    bowTextureData.getModel(),
                    bowTextureData.getPulling_1().getModel(),
                    bowTextureData.getPulling_2().getModel(),
                    bowTextureData.getPulling_0().getModel()
            );
        } else if (data.getCrossbowTextureData() != null) {
            CrossbowTextureData crossbowTextureData = data.getCrossbowTextureData();
            normalizeData(file, crossbowTextureData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

            jsonData = String.format(
                    readResource("/models/crossbow.json"),
                    crossbowTextureData.getWithArrow().getModel(),
                    crossbowTextureData.getWithFirework().getModel(),
                    crossbowTextureData.getModel(),
                    crossbowTextureData.getPulling_1().getModel(),
                    crossbowTextureData.getPulling_2().getModel(),
                    crossbowTextureData.getPulling_0().getModel()
            );
        } else if (data.getTridentTextureData() != null) {
            TridentTextureData tridentData = data.getTridentTextureData();
            normalizeData(file, tridentData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

            jsonData = String.format(
                    readResource("/models/trident.json"),
                    tridentData.getModel(),
                    tridentData.getInHand().getModel(),
                    tridentData.getThrowing().getModel()
            );
        } else if (data.getFishingRodTextureData() != null) {
            FishingRodTextureData fishingRodData = data.getFishingRodTextureData();
            normalizeData(file, fishingRodData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

            jsonData = String.format(
                    readResource("/models/fishing_rod.json"),
                    fishingRodData.getModel(),
                    fishingRodData.getCast().getModel()
            );
        } else if (data.getElytraTextureData() != null) {
            ElytraTextureData elytraData = data.getElytraTextureData();
            normalizeData(file, elytraData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

            jsonData = String.format(
                    readResource("/models/elytra.json"),
                    elytraData.getModel(),
                    elytraData.getBroken().getModel()
            );
        } else if (data.getShieldTextureData() != null) {
            final ShieldTextureData shieldData = data.getShieldTextureData();
            normalizeData(file, shieldData, data.getMainTextureData().getTexture(), outputDirectory, prefix);

            jsonData = String.format(
                    readResource("/models/shield.json"),
                    shieldData.getModel(),
                    shieldData.getBlocking().getModel()
            );

        } else {
            jsonData = String.format(readResource("/models/default.json"), data.getMainTextureData().getModel());
        }
        jsonPath.getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(jsonPath.toFile())) {
            writer.write(jsonData);
        }

        return List.of(data.saveToMap());
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
            case ShieldTextureData ignored -> "shield";
            default -> "generated";
        };
        for (TextureData data1 : data.getAll()) {
            if (data1 == null) continue;
            if (first == null) {
                first = data1;
            }
            convertTextureData(file, data1, parent, fallbackTexture, outputDirectory, prefix);
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
        if (data instanceof ShieldTextureData shield) {
            if (shield.getBlocking() == null) {
                shield.setBlocking(first);
            }
        }
    }

    private void convertTextureData(Path file, TextureData data, String parent, String fallbackTexture, Path outputDirectory, Path prefix) throws IOException {
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
                return;
            }
            final String textureKey = copyTexture(List.of(file.getParent()), texture, outputDirectory, prefix);
            if (textureKey == null) return;
            final String overlayKey = overlay != null ? copyTexture(List.of(file.getParent()), overlay, outputDirectory, prefix) : null;

            data.setModel(textureToModel(textureKey, overlayKey, parent, outputDirectory, prefix, null));
            return;
        }

        data.setModel(copyModel(file.getParent(), model, texture, outputDirectory, prefix));
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
        final var location = findResource(List.of(file.getParent()), texture.replaceFirst(":", "/models/"), "json");
        if (location == null) return null;
        final Path modelPath = copyResource(
                location,
                "json",
                outputDirectory.resolve(Path.of("assets", namespace, "equipment")),
                prefix
        );

        return prefixToString(prefix) + resourceNameFromPath(modelPath);
    }

    private String resourceNameFromPath(Path path) {
        return removeExtension(path.getFileName().toString());
    }

    private String removeExtension(String filename) {
        if (filename == null) return null;
        return filename.replaceFirst("\\.[^/]+$", "");
    }

    private String ensureExtension(String filename, String extension) {
        if (filename == null) return null;
        if (filename.endsWith("." + extension)) {
            return filename;
        }
        return filename + "." + extension;
    }

    private @NotNull String textureToModel(String textureKey, String overlayKey, String parent, Path outputDirectory, Path prefix, Object n) throws IOException {
        final String filename = lastKeyPart(textureKey);
        final Path tmpModelPath = getTmpDir().resolve("models").resolve(filename + ".json");
        tmpModelPath.getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(tmpModelPath.toFile())) {
            if (overlayKey == null) {
                writer.write(String.format(readResource("/models/item.json"), parent, textureKey));
            } else {
                writer.write(String.format(readResource("/models/item_with_overlay.json"), parent, textureKey, overlayKey));
            }
        }

        return Objects.requireNonNull(
                copyModel(tmpModelPath.getParent(), filename, false, null, outputDirectory, prefix),
                "Failed to copy generated model!"
        );
    }

    private String armorTextureToModel(Path file, String texture, int type, String overlay, Path outputDirectory, Path prefix) throws IOException {
        if (mode == Mode.ORAXEN) {
            final var location = findResource(
                    List.of(file.getParent()),
                    texture.replaceFirst(":", "/textures/"),
                    "png"
            );
            if (location == null) return null;
            final Path armorTexturePath = copyResource(
                    location,
                    "png",
                    outputDirectory.resolve(Path.of(
                            "assets",
                            namespace,
                            "textures",
                            "armor"
                    )),
                    Path.of("")
            );
            final String armorName = removeExtension(namespace + "_" + prefixToString(prefix).replace("/", "_") + armorTexturePath.getFileName());
            Files.move(
                    armorTexturePath,
                    armorTexturePath.resolveSibling(
                            String.format("%s_armor_layer_%s.png", armorName, type)
                    ),
                    StandardCopyOption.REPLACE_EXISTING
            );
            return armorName;
        }

        final String armorTextureKey = copyArmorTexture(
                file.getParent(),
                texture,
                type,
                outputDirectory,
                prefix
        );
        if (armorTextureKey == null) return null;
        final String armorOverlayKey = overlay != null ? copyArmorTexture(
                file.getParent(),
                overlay,
                type,
                outputDirectory,
                prefix
        ) : null;

        final String prefixString = prefixToString(prefix);
        final Path modelPath = outputDirectory.resolve(
                Path.of("assets", namespace, "equipment")
        ).resolve(prefix).resolve(lastKeyPart(armorTextureKey) + ".json");
        modelPath.getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(modelPath.toFile())) {
            if (type != 3) {
                if (armorOverlayKey != null) {
                    writer.write(String.format(
                            readResource("/models/armor_with_overlay.json"),
                            armorTextureKey,
                            armorOverlayKey,
                            armorTextureKey,
                            armorOverlayKey,
                            armorTextureKey,
                            armorOverlayKey
                    ));
                } else {
                    writer.write(String.format(
                            readResource("/models/armor.json"),
                            armorTextureKey,
                            armorTextureKey,
                            armorTextureKey
                    ));
                }
            } else {
                writer.write(String.format(
                        readResource("/models/armor_elytra.json"),
                        armorTextureKey
                ));
            }
        }
        return prefixString + resourceNameFromPath(modelPath);
    }

    private final BiMap<Path, String> copiedTextures = HashBiMap.create();

    private @Nullable String copyTexture(List<Path> inputDirectories, String texture, Path outputDirectory, Path prefix) throws IOException {
        if (copiedTextures.containsValue(texture)) {
            return texture;
        }
        final var location = findResource(
                inputDirectories,
                texture.replaceFirst(":", "/textures/"),
                "png"
        );
        if (location == null) return null;
        if (copiedTextures.containsKey(location.getValue())) {
            return copiedTextures.get(location.getValue());
        }
        final String key = addNamespace(prefixToString(prefix) + resourceNameFromPath(copyResource(
                location,
                "png",
                outputDirectory.resolve(Path.of(
                        "assets",
                        namespace,
                        "textures",
                        "item"
                )),
                prefix
        )));
        copiedTextures.put(location.getValue(), key);
        return key;
    }

    private @Nullable String copyArmorTexture(Path inputDirectory, String texture, int textureType, Path outputDirectory, Path prefix) throws IOException {
        final String subfolder = switch (textureType) {
            case 1 -> "humanoid";
            case 2 -> "humanoid_leggings";
            case 3 -> "wings";
            default -> throw new IllegalStateException("Unexpected value: " + textureType);
        };
        final var location = findResource(
                List.of(inputDirectory),
                texture.replaceFirst(":", "/textures/"),
                "png"
        );
        if (location == null) return null;
        return namespace + ":" + prefixToString(prefix) + resourceNameFromPath(copyResource(
                location,
                "png",
                outputDirectory.resolve(Path.of(
                    "assets",
                        namespace,
                        "textures",
                        "entity",
                        "equipment",
                        subfolder
                )),
                prefix
        ));
    }

    private final Map<Path, String> copiedModels = new HashMap<>();
    private final Set<String> copiedModelKeys = new HashSet<>();

    private @Nullable String copyModel(Path inputDirectory, String model, String textureName, Path outputDirectory, Path prefix) throws IOException {
        return copyModel(inputDirectory, model, true, textureName, outputDirectory, prefix);
    }

    private @Nullable String copyModel(Path inputDirectory, String model, boolean processTextures, String textureName, Path outputDirectory, Path prefix) throws IOException {
        return copyModel(inputDirectory, model, processTextures, textureName, outputDirectory, prefix, new ArrayList<>());
    }

    /*
        returns Entry<sourcePath, copiedPath>
        if copiedPath is empty, sourcePath must be not empty; retrieve cache by sourcePath
        if sourcePath is empty, do not record cache
    */
    private @Nullable String copyModel(Path inputDirectory, String model, boolean processTextures, String textureName, Path outputDirectory, Path prefix, List<Path> seenParents) throws IOException {
        if (copiedModelKeys.contains(model)) {
            return model;
        }
        final Path modelDirectory = outputDirectory.resolve(Path.of(
                "assets",
                namespace,
                "models",
                "item"
        ));
        final String prefixString = prefixToString(prefix);

        final Path texturePath = textureName != null ? resolveResource(inputDirectory, ensureExtension(textureName, "png"), ResourceType.TEXTURE) : null;
        model = model.replaceFirst(":", "/models/");
        final var location = findResource(
                List.of(inputDirectory),
                model,
                "json"
        );
        if (location == null) return null;

        final @NotNull String parentModelKey;
        if (copiedModels.containsKey(location.getValue())) {
            if (texturePath == null) {
                return copiedModels.get(location.getValue());
            } else {
                parentModelKey = copiedModels.get(location.getValue());
            }
        } else {
            final Path newPath = copyResource(
                    location,
                    "json",
                    modelDirectory,
                    prefix
            );
            if (!processTextures) {
                final var key = addNamespace(prefixString + resourceNameFromPath(newPath));
                // skip path caching
                copiedModelKeys.add(key);
                return key;
            }

            seenParents.add(prefix.resolve(resourceNameFromPath(newPath)));

            final JsonObject json;
            try (FileReader reader = new FileReader(newPath.toFile())) {
                try {
                    json = new Gson().fromJson(reader, JsonObject.class);
                } catch (JsonSyntaxException ignored) {
                    log(Level.WARNING, "Invalid JSON: " + location.getValue());
                    final var key = addNamespace(prefixString + resourceNameFromPath(newPath));
                    copiedModels.put(location.getValue(), key);
                    copiedModelKeys.add(key);
                    return key;
                }
            }
            final JsonElement parent = json.get("parent");
            if (parent != null) {
                final Path parentPath = stringToPath(parent.getAsString());

                final Path fullParentPath = prefix.resolve(parentPath);
                if (!seenParents.contains(fullParentPath)) {

                    Path parentInputDirectory = inputDirectory;
                    Path parentPrefix = prefix;
                    if (parentPath.getParent() != null) {
                        parentInputDirectory = parentInputDirectory.resolve(parentPath.getParent());
                        parentPrefix = parentPrefix.resolve(parentPath.getParent());
                    }
                    final String parentModel = copyModel(parentInputDirectory, parentPath.getFileName().toString(), true, null, outputDirectory, parentPrefix, seenParents);
                    if (parentModel != null) {
                        json.addProperty("parent", parentModel);
                    }
                } else {
                    log(Level.WARNING, "Recursive model detected (the model is parent of itself)");
                    json.remove("parent");
                }
            }
            fixTextures(inputDirectory, model, json, null, outputDirectory, prefix);
            try (FileWriter writer = new FileWriter(newPath.toFile())) {
                writer.write(new Gson().toJson(json));
            }

            parentModelKey = addNamespace(prefixString + resourceNameFromPath(newPath));

            copiedModels.put(location.getValue(), parentModelKey);
            copiedModelKeys.add(parentModelKey);
        }

        if (texturePath == null) {
            return Objects.requireNonNull(
                    copiedModels.get(location.getValue()),
                    location.getValue() + " was not yet processed"
            );
        }

        final Path parentPath = Objects.requireNonNull(
                findResource(List.of(outputDirectory.resolve("assets")), parentModelKey.replaceFirst(":", "/models/"), "json"),
                "Model " + parentModelKey + " must've been cached but not found"
        ).getValue();
        final JsonObject parentJson;
        try (FileReader reader = new FileReader(parentPath.toFile())) {
            try {
                parentJson = new Gson().fromJson(reader, JsonObject.class);
            } catch (JsonSyntaxException ignored) {
                log(Level.WARNING, "Invalid JSON: " + parentPath);
                return copiedModels.get(location.getValue());
            }
        }

        final JsonObject newJson = new JsonObject();
        newJson.addProperty("parent", parentModelKey);
        if (parentJson.get("textures") != null) {
            newJson.add("textures", parentJson.get("textures"));
        } else {
            final JsonObject textures = new JsonObject();
            textures.addProperty("layer0", addNamespace(prefixString + textureName));
            newJson.add("textures", textures);
        }
        final Path newPath = parentPath.resolveSibling(
                resourceNameFromPath(parentPath)
                        + "_" + resourceNameFromPath(texturePath) + ".json"
        );
        fixTextures(inputDirectory, model, newJson, textureName, outputDirectory, prefix);
        try (FileWriter writer = new FileWriter(newPath.toFile())) {
            writer.write(new Gson().toJson(newJson));
        }

        final var key = namespace + ":" + parentModelKey.split(":", 2)[1].replaceFirst("[^/$]*$", "") + resourceNameFromPath(newPath);
        copiedModelKeys.add(key);
        return key;
    }

    private void fixTextures(Path inputDirectory, String modelName, JsonObject model, String textureOverride, Path outputDirectory, Path prefix) throws IOException {
        JsonElement texturesElement = model.get("textures");
        if (texturesElement != null) {
            JsonObject textures = texturesElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                final String textureName = textureOverride == null ? entry.getValue().getAsString() : textureOverride;
                final String outputTexture = copyTexture(List.of(inputDirectory, resolveOldPath(inputDirectory, modelName, "json").getParent()), textureName, outputDirectory, prefix);

                textures.addProperty(entry.getKey(), outputTexture != null ? outputTexture : textureName);
            }
        }
    }

    private Map.Entry<Path, Path> findResource(List<Path> inputDirectories, String resource, String extension) throws IOException {
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
            final Path resourcePath = stringToPath(resource);
            if (!DEFAULT_MODEL_DIRECTORIES.contains(resourcePath.getName(0).toString())
                    && (resourcePath.getNameCount() != 1 || inputDirectories.size() != 1
                    || !DEFAULT_MODEL_DIRECTORIES.contains(inputDirectories.getFirst().getFileName()
                    .toString().replaceFirst("^minecraft:", "")))
            ) {
                log(Level.WARNING, "Missing resource: " + resource + " (searched in: " + inputDirectories + ")");
            }
            return null;
        }
        return Map.entry(foundDirectory, oldPath);
    }

    private Path copyResource(@NotNull Map.Entry<Path, Path> location, String extension, Path outputDirectory, Path prefix) throws IOException {
        final var oldPath = location.getValue();
        String outputName = removeExtension(joinPath(location.getKey().relativize(oldPath)));
        Path newPath = outputDirectory.resolve(prefix).resolve(ensureExtension(outputName.toLowerCase(Locale.ROOT), extension));
        newPath.getParent().toFile().mkdirs();
        Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        if (addMcmeta(oldPath).toFile().isFile()) {
            Files.copy(addMcmeta(oldPath), addMcmeta(newPath), StandardCopyOption.REPLACE_EXISTING);
        }
        return newPath;
    }

    private @NotNull String joinPath(@NotNull Path path) {
        return Arrays.stream(pathToString(path).split("/"))
                .dropWhile(".."::equals).reduce("", (s1, s2) -> s1 + "_" + s2).substring(1);
    }

    private Path addMcmeta(Path path) {
        return path.getParent().resolve(path.getFileName() + ".mcmeta");
    }

    private @NotNull Path lowerPath(@NotNull Path path) {
        return stringToPath(pathToString(path).toLowerCase(Locale.ROOT));
    }

    private @NotNull String prefixToString(@NotNull Path prefix) {
        final String string = pathToString(prefix);
        if (string.isEmpty()) {
            return string;
        }
        return string + "/";
    }

    private @NotNull String lastKeyPart(@NotNull String key) {
        final String[] parts = key.split(":", 2)[1].split("/");
        return parts[parts.length - 1];
    }

    private @NotNull String addNamespace(@NotNull String path) {
        return namespace + ":item/" + path;
    }

    private Path resolveOldPath(Path inputDirectory, String resource, String extension) throws IOException {
        return resolveResource(
                inputDirectory,
                ensureExtension(resource, extension),
                extension.equals("json") ? ResourceType.MODEL : ResourceType.TEXTURE
        );
    }

    private Path resolveResource(Path currentDirectory, String resource, ResourceType type) throws IOException {
        final Path resourcePath = stringToPath(resource);
        Path result;
        do {
            result = currentDirectory.resolve(resourcePath);
            currentDirectory = currentDirectory.getParent();
        } while (!result.toFile().isFile() && currentDirectory != null && !currentDirectory.endsWith("minecraft"));
        if (result.toFile().isFile()) return result;

        if (currentDirectory == null) return null;

        result = currentDirectory.resolve(resourcePath);
        if (result.toFile().isFile()) return result;

        currentDirectory = currentDirectory.getParent();

        result = currentDirectory.resolve(resourcePath);
        if (result.toFile().isFile()) return result;

        try (Stream<Path> directories = Files.walk(currentDirectory, 1)) {
            result = directories.filter(dir -> dir.resolve(type == ResourceType.MODEL ? "models" : "textures").resolve(resourcePath).toFile().isFile()).findFirst().orElse(null);
            if (result == null) return null;
            return result.resolve(type == ResourceType.MODEL ? "models" : "textures").resolve(resourcePath);
        }
    }

    public void saveConfiguration(Path outputPath) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.set("renames", renames);
        config.save(outputPath.toFile());
    }

    public void saveOraxenConfig(Path outputPath) throws IOException {
        assert oraxenConfig != null;
        oraxenConfig.save(outputPath.toFile());
    }

    private Path getTmpDir() {
        return tempPath;
    }

    private Path getTmpPackDir() {
        return getTmpDir().resolve(UUID.randomUUID().toString());
    }

    private void log(@NotNull Level level, @NotNull String message) {
        logMessages.add(Map.entry(level, message));
    }
}
