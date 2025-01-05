package org.vinerdream.citPaper.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.vinerdream.citPaper.CITPaper;

public class CITPaperReloadCommand implements CommandExecutor {
    private final CITPaper plugin;

    public CITPaperReloadCommand(CITPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        plugin.reloadConfig();
        plugin.loadRenames();

        sender.sendMessage("Reloaded successfully!");

        return true;
    }
}
