package org.vinerdream.citPaper.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class OraxenDatapackHelper {
    public static Set<String> getCurrentTrimsFiles() {
        final var files = Bukkit.getWorlds().getFirst().getWorldFolder().toPath()
                .resolve("datapacks").resolve("oraxen_trim_armor").resolve("data")
                .resolve("oraxen").resolve("trim_pattern").toFile().list();
        return files != null ? Set.of(files) : Set.of();
    }

    public static Set<String> getCachedTrimsFiles(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            return Set.of(new Gson().fromJson(reader, JsonArray.class).asList().stream().map(JsonElement::getAsString).toArray(String[]::new));
        }
    }

    public static void cacheTrimsFiles(File file, Set<String> trims) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(new Gson().toJson(trims));
        }
    }
}
