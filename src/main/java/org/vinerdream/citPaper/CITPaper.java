package org.vinerdream.citPaper;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.vinerdream.citPaper.converter.ParsedTextureProperties;
import org.vinerdream.citPaper.listeners.AnvilListener;
import org.vinerdream.citPaper.listeners.BookListener;
import org.vinerdream.citPaper.utils.ItemUpdater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class CITPaper extends JavaPlugin {
    @Getter
    private final List<ParsedTextureProperties> renames = new ArrayList<>();
    @Getter
    private ItemUpdater itemUpdater;
    @Getter
    private final NamespacedKey isManagedKey;

    public CITPaper() {
        isManagedKey = new NamespacedKey(this, "is_managed");
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new AnvilListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BookListener(this), this);

        loadConfigs();

        itemUpdater = new ItemUpdater(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void loadConfigs() {
        Path renamesPath = getDataPath().resolve("renames");
        if (!renamesPath.toFile().isDirectory()) {
            renamesPath.toFile().mkdirs();
        }
        try (Stream<Path> contents = Files.walk(renamesPath)) {
            contents.forEach(path -> {
                Configuration config = YamlConfiguration.loadConfiguration(path.toFile());
                config.getMapList("renames").forEach(map -> renames.add(new ParsedTextureProperties((Map<String, String>) map)));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
