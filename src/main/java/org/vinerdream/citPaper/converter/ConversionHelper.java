package org.vinerdream.citPaper.converter;

import org.vinerdream.citPaper.config.MainConfig;
import org.vinerdream.citPaper.config.Mode;
import org.vinerdream.citPaper.utils.FileUtils;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ConversionHelper {
    private static final Path MAIN_FAILURE = Path.of("MAIN");

    public static Map.Entry<List<Path>, Map<Path, Exception>> runConversion(final MainConfig mainConfig, final Logger logger, final Path renamesPath, final Path oraxenItemsPath) {
        final Path inputPath = mainConfig.getConverterInputDirectory();
        final Path outputPath = mainConfig.getConverterOutputDirectory();

        if (!inputPath.toFile().isDirectory()) {
            inputPath.toFile().mkdirs();
        }
        try {
            if (mainConfig.isConverterClearConfigs()) {
                FileUtils.removeDirectory(renamesPath);
                FileUtils.removeDirectory(oraxenItemsPath);
            }
            if (mainConfig.isConverterClearOutputDirectory()) {
                FileUtils.removeDirectory(outputPath);
            }
        } catch (IOException e) {
            return Map.entry(List.of(), Map.of(MAIN_FAILURE, e));
        }
        if (mainConfig.getMode() == Mode.ORAXEN) {
            oraxenItemsPath.toFile().mkdirs();
        }

        final List<Path> convertedResourcePacks = new ArrayList<>();
        final Map<Path, Exception> failedResourcePacks = new HashMap<>();
        final ResourcePack mergedPack = mainConfig.isConverterMergePacks() ? ResourcePack.resourcePack() : null;

        try (Stream<Path> inputs = Files.walk(inputPath, 1)) {
            inputs.sorted().forEachOrdered(input -> {
                if (input.equals(inputPath)) return;
                ResourcePackConverter converter = new ResourcePackConverter(
                        mainConfig.getMode(),
                        input,
                        outputPath.resolve(input.getFileName()),
                        mainConfig.getTempFolder(),
                        mainConfig.isConverterPreserveCitDirectories(),
                        logger,
                        mergedPack,
                        mainConfig.getOraxenArmorType()
                );
                try {
                    converter.convertResourcePack();
                    converter.saveConfiguration(renamesPath.resolve(input.getFileName() + ".yml"));
                    if (mainConfig.getMode() == Mode.ORAXEN) {
                        converter.saveOraxenConfig(oraxenItemsPath.resolve(input.getFileName() + ".yml"));
                    }
                    convertedResourcePacks.add(input);
                } catch (Exception e) {
                    failedResourcePacks.put(input, e);
                }
            });
        } catch (IOException e) {
            return Map.entry(List.of(), Map.of(MAIN_FAILURE, e));
        }

        if (mergedPack != null) {
            outputPath.toFile().mkdirs();
            final String fileName = mainConfig.getConverterMergedOutputFile();
            if (fileName != null && !fileName.isEmpty()) {
                MinecraftResourcePackWriter.minecraft().writeToZipFile(outputPath.resolve(fileName), mergedPack);
            } else {
                MinecraftResourcePackWriter.minecraft().writeToDirectory(outputPath.toFile(), mergedPack);
            }
        }

        return Map.entry(convertedResourcePacks, failedResourcePacks);
    }
}
