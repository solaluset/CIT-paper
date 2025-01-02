package org.vinerdream.citPaper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.vinerdream.citPaper.listeners.AnvilListener;

public final class CITPaper extends JavaPlugin {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new AnvilListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
