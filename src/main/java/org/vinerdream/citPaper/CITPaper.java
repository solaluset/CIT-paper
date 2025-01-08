package org.vinerdream.citPaper;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.vinerdream.citPaper.commands.CITPaperCommand;
import org.vinerdream.citPaper.converter.ParsedTextureProperties;
import org.vinerdream.citPaper.converter.ResourcePackConverter;
import org.vinerdream.citPaper.listeners.AnvilListener;
import org.vinerdream.citPaper.listeners.BookListener;
import org.vinerdream.citPaper.listeners.InventoryListener;
import org.vinerdream.citPaper.listeners.ItemDamageListener;
import org.vinerdream.citPaper.utils.FileUtils;
import org.vinerdream.citPaper.utils.ItemUpdater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.vinerdream.citPaper.utils.MapUtils.mapToStringMap;

public final class CITPaper extends JavaPlugin {
    @Getter
    private final List<ParsedTextureProperties> renames = new ArrayList<>();
    @Getter
    private ItemUpdater itemUpdater;
    @Getter
    private final NamespacedKey isManagedKey;

    public CITPaper() throws IOException {
        isManagedKey = new NamespacedKey(this, "is_managed");

        saveDefaultConfig();

        generateResourcePacks();
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new AnvilListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BookListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ItemDamageListener(this), this);

        PluginCommand command = Objects.requireNonNull(getCommand("cit-paper"));
        CITPaperCommand executor = new CITPaperCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        loadRenames();

        itemUpdater = new ItemUpdater(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void loadRenames() {
        renames.clear();
        Path renamesPath = getDataPath().resolve("renames");
        if (!renamesPath.toFile().isDirectory()) {
            renamesPath.toFile().mkdirs();
        }
        try (Stream<Path> contents = Files.walk(renamesPath)) {
            contents.forEach(path -> {
                Configuration config = YamlConfiguration.loadConfiguration(path.toFile());
                config.getMapList("renames").forEach(map -> renames.add(new ParsedTextureProperties(mapToStringMap(map), getLogger()::warning)));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        renames.sort(Comparator.comparingInt(ParsedTextureProperties::getWeight).reversed());
    }

    public boolean generateResourcePacks() throws IOException {
        if (!getConfig().getBoolean("converter.enabled")) {
            return false;
        }
        final String inputDirectory = getConfig().getString("converter.inputDirectory");
        final String outputDirectory = getConfig().getString("converter.outputDirectory");
        if (inputDirectory == null || outputDirectory == null) return false;

        final Path inputPath = Paths.get(inputDirectory);
        final Path outputPath = Paths.get(outputDirectory);
        final Path renamesPath = getDataPath().resolve("renames");

        if (getConfig().getBoolean("converter.clearConfigs")) {
            FileUtils.removeDirectory(renamesPath);
        }
        if (getConfig().getBoolean("converter.clearOutputDirectory")) {
            FileUtils.removeDirectory(outputPath);
        }

        try (Stream<Path> inputs = Files.walk(inputPath, 1)) {
            inputs.forEach(input -> {
                if (input.equals(inputPath)) return;
                ResourcePackConverter converter = new ResourcePackConverter(getLogger()::warning);
                try {
                    converter.convertResourcePack(
                            input,
                            outputPath.resolve(input.getFileName())
                    );
                    converter.saveConfiguration(renamesPath.resolve(input.getFileName() + ".yml"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return true;
    }
}
