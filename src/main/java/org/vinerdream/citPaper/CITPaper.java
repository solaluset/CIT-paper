package org.vinerdream.citPaper;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.vinerdream.citPaper.api.events.ResourcePacksPostGenerateEvent;
import org.vinerdream.citPaper.commands.CITPaperCommand;
import org.vinerdream.citPaper.converter.ParsedTextureProperties;
import org.vinerdream.citPaper.converter.ResourcePackConverter;
import org.vinerdream.citPaper.exceptions.UnsupportedCitTypeException;
import org.vinerdream.citPaper.listeners.*;
import org.vinerdream.citPaper.utils.FileUtils;
import org.vinerdream.citPaper.utils.ItemUpdater;
import org.vinerdream.citPaper.utils.SchedulerUtils;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.vinerdream.citPaper.utils.CollectionUtils.mapToStringMap;

public final class CITPaper extends JavaPlugin {
    @Getter
    private final List<ParsedTextureProperties> renames = new ArrayList<>();
    @Getter
    private final ItemUpdater itemUpdater;

    public CITPaper() {
        saveDefaultConfig();

        if (getConfig().getBoolean("converter.enabled")) {
            generateResourcePacks();
        }

        itemUpdater = new ItemUpdater(this);
    }

    @Override
    public void onEnable() {
        registerEvents(
                new AnvilListener(this),
                new BookListener(this),
                new InventoryListener(this),
                new ItemDamageListener(this),
                new EnchantListener(this),
                new GrindstoneListener(this),
                new MendingListener(this),
                new SmithingListener(this),
                new CraftListener(this),
                new PlayerListener(this)
        );

        PluginCommand command = Objects.requireNonNull(getCommand("cit-paper"));
        CITPaperCommand executor = new CITPaperCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        loadRenames();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void registerEvents(Listener ...listeners) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, this);
        }
    }

    private Path getRenamesPath() {
        return getDataFolder().toPath().resolve("renames");
    }

    private Path getCachePath() {
        return getDataFolder().toPath().resolve("cache");
    }

    public void loadRenames() {
        renames.clear();
        Path renamesPath = getRenamesPath();
        if (!renamesPath.toFile().isDirectory()) {
            renamesPath.toFile().mkdirs();
        }
        try (Stream<Path> contents = Files.walk(renamesPath)) {
            contents.forEach(path -> {
                Configuration config = YamlConfiguration.loadConfiguration(path.toFile());
                config.getMapList("renames").forEach(map -> {
                    final ParsedTextureProperties data;
                    try {
                        data = new ParsedTextureProperties(mapToStringMap(map), getLogger()::warning);
                    } catch (UnsupportedCitTypeException e) {
                        getLogger().warning(path + ": " + e.getMessage());
                        return;
                    }
                    renames.add(data);
                });
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        renames.sort(
                Comparator.comparingInt(ParsedTextureProperties::getWeight)
                        .thenComparing(properties -> properties.getNamePattern() != null ? properties.getNamePattern().pattern().length() : 0)
                        .reversed()
        );
    }

    public boolean generateResourcePacks() {
        final String inputDirectory = getConfig().getString("converter.inputDirectory");
        final String outputDirectory = getConfig().getString("converter.outputDirectory");
        if (inputDirectory == null || outputDirectory == null) return false;

        final Path inputPath = Path.of(inputDirectory);
        final Path outputPath = Path.of(outputDirectory);
        final Path renamesPath = getRenamesPath();

        if (!inputPath.toFile().isDirectory()) {
            inputPath.toFile().mkdirs();
        }
        try {
            if (getConfig().getBoolean("converter.clearConfigs")) {
                FileUtils.removeDirectory(renamesPath);
            }
            if (getConfig().getBoolean("converter.clearOutputDirectory")) {
                FileUtils.removeDirectory(outputPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        final List<Path> convertedResourcePacks = new ArrayList<>();
        final Map<Path, Exception> failedResourcePacks = new HashMap<>();
        final ResourcePack mergedPack = getConfig().getBoolean("converter.mergePacks") ? ResourcePack.resourcePack() : null;

        try (Stream<Path> inputs = Files.walk(inputPath, 1)) {
            inputs.sorted().forEachOrdered(input -> {
                if (input.equals(inputPath)) return;
                ResourcePackConverter converter = new ResourcePackConverter(
                        input,
                        outputPath.resolve(input.getFileName()),
                        getCachePath(),
                        getConfig().getBoolean("converter.preserveCitDirectories"),
                        getLogger(),
                        mergedPack
                );
                try {
                    converter.convertResourcePack();
                    converter.saveConfiguration(renamesPath.resolve(input.getFileName() + ".yml"));
                    convertedResourcePacks.add(input);
                } catch (Exception e) {
                    failedResourcePacks.put(input, e);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        failedResourcePacks.forEach((path, exception) -> {
            getLogger().severe("Failed to convert " + path);
            exception.printStackTrace();
        });

        if (mergedPack != null) {
            outputPath.toFile().mkdirs();
            final String fileName = getConfig().getString("converter.mergedOutputFile");
            if (fileName != null && !fileName.isEmpty()) {
                MinecraftResourcePackWriter.minecraft().writeToZipFile(outputPath.resolve(fileName), mergedPack);
            } else {
                MinecraftResourcePackWriter.minecraft().writeToDirectory(outputPath.toFile(), mergedPack);
            }
        }

        if (isEnabled()) {
            SchedulerUtils.runTask(
                    this,
                    () -> Bukkit.getPluginManager().callEvent(new ResourcePacksPostGenerateEvent(
                            convertedResourcePacks,
                            failedResourcePacks
                    ))
            );
        }

        return failedResourcePacks.isEmpty();
    }
}
