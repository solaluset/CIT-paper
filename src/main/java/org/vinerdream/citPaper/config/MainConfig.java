package org.vinerdream.citPaper.config;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

@Getter
public class MainConfig {
    private final @NotNull Mode mode;
    private final boolean verboseLogging;
    private final @NotNull Path tempFolder;

    private final boolean converterEnabled;
    private final @NotNull Path converterInputDirectory;
    private final @NotNull Path converterOutputDirectory;
    private final boolean converterPreserveCitDirectories;
    private final boolean converterClearConfigs;
    private final boolean converterClearOutputDirectory;
    private final boolean converterMergePacks;
    private final @Nullable String converterMergedOutputFile;

    private final @NotNull String oraxenArmorType;
    private final @NotNull NamespacedKey oraxenDefaultTrimMaterialKey;
    private final @NotNull String oraxenRestartCommand;

    public MainConfig(final Configuration config) {
        this.mode = Mode.valueOf(Objects.requireNonNull(config.getString("mode")).toUpperCase(Locale.ROOT));
        this.verboseLogging = config.getBoolean("verboseLogging");

        final String tempFolder = config.getString("tempFolder");
        this.tempFolder = Path.of(
                tempFolder != null && !tempFolder.isEmpty()
                        ? tempFolder
                        : System.getProperty("java.io.tmpdir"),
                "cit-paper"
        );

        this.converterEnabled = config.getBoolean("converter.enabled");
        this.converterInputDirectory = Path.of(Objects.requireNonNull(config.getString("converter.inputDirectory")));
        this.converterOutputDirectory = Path.of(Objects.requireNonNull(config.getString("converter.outputDirectory")));
        this.converterClearConfigs = config.getBoolean("converter.clearConfigs");
        this.converterClearOutputDirectory = config.getBoolean("converter.clearOutputDirectory");
        this.converterPreserveCitDirectories = config.getBoolean("converter.preserveCitDirectories");
        this.converterMergePacks = config.getBoolean("converter.mergePacks");
        this.converterMergedOutputFile = config.getString("converter.mergedOutputFile");

        this.oraxenArmorType = config.getString("oraxen.armorType", "CHAINMAIL").toLowerCase(Locale.ROOT);
        this.oraxenDefaultTrimMaterialKey = Objects.requireNonNull(
                NamespacedKey.fromString(config.getString("oraxen.defaultTrimMaterial", "minecraft:amethyst"))
        );
        this.oraxenRestartCommand = config.getString("oraxen.restartCommand", "restart");
    }

    public static MainConfig fromFile(final @NotNull File file) {
        return new MainConfig(YamlConfiguration.loadConfiguration(file));
    }
}
