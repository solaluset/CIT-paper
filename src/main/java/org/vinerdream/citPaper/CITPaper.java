package org.vinerdream.citPaper;

import io.th0rgal.oraxen.api.OraxenItems;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.vinerdream.citPaper.api.events.ResourcePacksPostGenerateEvent;
import org.vinerdream.citPaper.commands.CITPaperCommand;
import org.vinerdream.citPaper.config.MainConfig;
import org.vinerdream.citPaper.config.Mode;
import org.vinerdream.citPaper.converter.ConversionHelper;
import org.vinerdream.citPaper.converter.OraxenData;
import org.vinerdream.citPaper.converter.ParsedTextureProperties;
import org.vinerdream.citPaper.exceptions.UnsupportedCitTypeException;
import org.vinerdream.citPaper.listeners.*;
import org.vinerdream.citPaper.utils.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.vinerdream.citPaper.utils.ItemUtils.*;

public final class CITPaper extends JavaPlugin {
    private static final Path oraxenItemsPath;
    static {
        final Plugin oraxen = Bukkit.getPluginManager().getPlugin("Oraxen");
        oraxenItemsPath = oraxen != null
                ? oraxen.getDataFolder().toPath()
                .resolve("items").resolve("cit-paper")
                : null;
    }

    @Getter
    private final List<ParsedTextureProperties> renames = new ArrayList<>();
    @Getter
    private final ItemUpdater itemUpdater;
    @Getter
    private final MainConfig mainConfig;
    @Getter
    private final Mode mode;
    @Getter
    private final String oraxenArmorType;
    @Getter
    private final @NotNull Path cachePath;

    public CITPaper() {
        saveDefaultConfig();

        mainConfig = new MainConfig(super.getConfig());

        cachePath = mainConfig.getTempFolder();

        mode = mainConfig.getMode();
        if (mode == Mode.ORAXEN && oraxenItemsPath == null) {
            throw new IllegalStateException("mode is ORAXEN but Oraxen was not found!");
        }
        oraxenArmorType = mainConfig.getOraxenArmorType();

        if (mainConfig.isConverterEnabled()) {
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

        if (mode != Mode.ORAXEN) {
            loadRenames();
        } else {
            registerEvents(new OraxenListener(this));
        }
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

    @Override
    @Deprecated
    public @NotNull FileConfiguration getConfig() {
        return super.getConfig();
    }

    private Path getRenamesPath() {
        return getDataFolder().toPath().resolve("renames");
    }

    public void loadRenames() {
        renames.clear();
        Path renamesPath = getRenamesPath();
        if (!renamesPath.toFile().isDirectory()) {
            renamesPath.toFile().mkdirs();
        }
        try (Stream<Path> contents = Files.walk(renamesPath)) {
            contents.forEach(path -> {
                if (path.toFile().isDirectory()) {
                    return;
                }
                final YamlConfiguration config = YamlConfiguration.loadConfiguration(path.toFile());
                final List<Map<String, String>> loaded = config.getMapList("renames").stream().map(CollectionUtils::mapToStringMap).toList();
                if (mode == Mode.ORAXEN) {
                    loaded.forEach(this::updateOraxenRename);
                    config.set("renames", loaded);
                    try {
                        config.save(path.toFile());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                loaded.forEach(map -> {
                    final ParsedTextureProperties data;
                    try {
                        data = new ParsedTextureProperties(map, getLogger()::warning);
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

    private void updateOraxenRename(Map<String, String> data) {
        final OraxenData oraxenData = OraxenData.fromMap(data);
        if (oraxenData == null) {
            return;
        }
        final ItemStack item = OraxenItems.getItemById(oraxenData.getId()).build();
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        if (getItemModel(meta) != null) {
            oraxenData.setItemModel(meta.getItemModel());
        }
        if (meta.hasCustomModelData()) {
            oraxenData.setCustomModelData(meta.getCustomModelData());
        }
        oraxenData.toMap(data);
    }

    public boolean generateResourcePacks() {
        final List<Path> convertedResourcePacks;
        final Map<Path, Exception> failedResourcePacks;
        {
            final var result = ConversionHelper.runConversion(mainConfig, getLogger(), getRenamesPath(), oraxenItemsPath);
            convertedResourcePacks = result.getKey();
            failedResourcePacks = result.getValue();
        }

        failedResourcePacks.forEach((path, exception) -> {
            getLogger().severe("Failed to convert " + path);
            exception.printStackTrace();
        });

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
