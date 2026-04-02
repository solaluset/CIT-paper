package org.vinerdream.citPaper.converter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vinerdream.citPaper.config.MainConfig;
import org.vinerdream.citPaper.config.Mode;
import org.vinerdream.citPaper.utils.FileUtils;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.vinerdream.citPaper.utils.ReflectionUtils.readResource;

public class ConversionHelper {
    private static final Path MAIN_FAILURE = Path.of("MAIN");
    private static final String MODIFICATION_TIMES_FILENAME = "cit-paper-time-cache.json";
    private static final String VERSION = getVersion();

    public static Map.Entry<List<Path>, Map<Path, Exception>> runConversion(final MainConfig mainConfig, final Logger logger, final Path renamesPath, final @Nullable Path oraxenItemsPath) {
        final Path inputPath = mainConfig.getConverterInputDirectory();
        final Path outputPath = mainConfig.getConverterOutputDirectory();

        try {
            if (mainConfig.isConverterClearConfigs()) {
                FileUtils.removeDirectory(renamesPath);
                if (oraxenItemsPath != null) {
                    FileUtils.removeDirectory(oraxenItemsPath);
                }
            }

            if (!inputPath.toFile().isDirectory()) {
                Files.createDirectories(inputPath);
            }

            if (mainConfig.getMode() == Mode.ORAXEN) {
                assert oraxenItemsPath != null;
                Files.createDirectories(oraxenItemsPath);
            }

            Files.createDirectories(outputPath);
        } catch (IOException e) {
            return Map.entry(List.of(), Map.of(MAIN_FAILURE, e));
        }

        final List<Path> convertedResourcePacks = new ArrayList<>();
        final Map<Path, Exception> failedResourcePacks = new HashMap<>();
        final ResourcePack mergedPack = mainConfig.isConverterMergePacks() ? ResourcePack.resourcePack() : null;

        final @NotNull Map<Path, Long> actualTimes;
        try {
            actualTimes = getPackModificationTimes(inputPath);
        } catch (IOException e) {
            return Map.entry(List.of(), Map.of(MAIN_FAILURE, e));
        }
        final @NotNull Map<Path, Long> savedTimes;
        {
            @NotNull Map<Path, Long> tmp;
            try {
                tmp = loadPackModificationTimes(outputPath);
            } catch (Exception e) {
                logger.warning("Unable to load timestamp cache, every resource pack will be regenerated: " + e.getMessage());
                tmp = Map.of();
            }
            savedTimes = tmp;
        }

        if (actualTimes.equals(savedTimes)) {
            logger.info("All packs are up to date - skipping conversion");
            return Map.entry(List.of(), Map.of());
        }

        final @NotNull Set<Path> skip = new HashSet<>();
        if (mergedPack == null) {
            savedTimes.forEach((path, time) -> {
                final @Nullable Long actualTime = actualTimes.get(path);
                if (actualTime == null) {
                    try {
                        FileUtils.removeDirectory(outputPath.resolve(path.getFileName()));
                    } catch (IOException e) {
                        logger.severe("Unable to delete outdated pack: " + e.getMessage());
                    }
                } else if (Objects.equals(time, actualTime)) {
                    skip.add(path);
                }
            });
        }

        actualTimes.keySet().stream().sorted().forEachOrdered(input -> {
            if (skip.contains(input)) {
                logger.info("Pack " + input + " is up to date - skipping conversion");
                return;
            }

            ResourcePackConverter converter = new ResourcePackConverter(
                    mainConfig,
                    input,
                    outputPath.resolve(input.getFileName()),
                    logger,
                    mergedPack
            );
            try {
                converter.convertResourcePack();
                converter.saveConfiguration(renamesPath.resolve(input.getFileName() + ".yml"));
                if (mainConfig.getMode() == Mode.ORAXEN) {
                    assert oraxenItemsPath != null;
                    converter.saveOraxenConfig(oraxenItemsPath.resolve(input.getFileName() + ".yml"));
                }
                convertedResourcePacks.add(input);
            } catch (Exception e) {
                failedResourcePacks.put(input, e);
            }
        });

        if (mergedPack != null) {
            final String fileName = mainConfig.getConverterMergedOutputFile();
            if (fileName != null && !fileName.isEmpty()) {
                MinecraftResourcePackWriter.minecraft().writeToZipFile(outputPath.resolve(fileName), mergedPack);
            } else {
                MinecraftResourcePackWriter.minecraft().writeToDirectory(outputPath.toFile(), mergedPack);
            }
        }

        try {
            savePackModificationTimes(outputPath, actualTimes);
        } catch (IOException e) {
            logger.warning("Unable to save timestamp cache: " + e.getMessage());
        }

        failedResourcePacks.forEach((path, exception) -> {
            logger.severe("Failed to convert " + path);
            exception.printStackTrace();
        });

        return Map.entry(convertedResourcePacks, failedResourcePacks);
    }

    private static @NotNull Map<Path, Long> getPackModificationTimes(final @NotNull Path inputPath) throws IOException {
        try (Stream<Path> inputs = Files.walk(inputPath, 1)) {
            final Map<Path, Long> result = new HashMap<>();
            inputs.forEach(input -> {
                if (input.equals(inputPath)) return;

                try {
                    result.put(input, Files.getLastModifiedTime(input).toMillis());
                } catch (IOException e) {
                    result.put(input, 0L);
                }
            });
            return result;
        }
    }

    private static void savePackModificationTimes(final @NotNull Path outputPath, final Map<Path, Long> modificationTimes) throws IOException {
        final JsonObject modificationTimesJson = new JsonObject();
        modificationTimesJson.addProperty("!version", VERSION);
        modificationTimes.forEach((path, modificationTime) -> {
            if (path.toFile().isDirectory()) {
                // mtime of directories is unreliable
                modificationTime = 0L;
            }
            modificationTimesJson.addProperty(path.toString(), modificationTime);
        });
        try (FileWriter writer = new FileWriter(outputPath.resolve(MODIFICATION_TIMES_FILENAME).toFile())) {
            writer.write(new Gson().toJson(modificationTimesJson));
        }
    }

    private static @NotNull Map<Path, Long> loadPackModificationTimes(final @NotNull Path outputPath) throws IOException {
        final JsonObject data;
        try (FileReader reader = new FileReader(outputPath.resolve(MODIFICATION_TIMES_FILENAME).toFile())) {
            data = new Gson().fromJson(reader, JsonObject.class);
        }
        if (data == null) {
            return Map.of();
        }
        final var version = data.remove("!version");
        if (version == null || !VERSION.equals(version.getAsString())) {
            return Map.of();
        }
        final Map<Path, Long> result = new HashMap<>();
        data.entrySet().forEach(entry -> result.put(Path.of(entry.getKey()), entry.getValue().getAsLong()));
        return result;
    }

    private static @NotNull String getVersion() {
        try {
            for (String line : readResource("/plugin.yml").split("\n")) {
                if (line.startsWith("version:")) {
                    line = line.split(":", 2)[1].strip();
                    return line.substring(1, line.length() - 1);
                }
            }
        } catch (IOException ignored) {}
        throw new RuntimeException("Unable to get version from resource plugin.yml");
    }
}
