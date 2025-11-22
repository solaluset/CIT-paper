package org.vinerdream.citPaper.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.utils.SchedulerUtils;

import java.util.List;

public class CITPaperCommand implements CommandExecutor, TabCompleter {
    private final CITPaper plugin;

    public CITPaperCommand(CITPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length != 1) {
            sender.sendMessage("Invalid usage.");
            return false;
        }

        SchedulerUtils.runTaskAsynchronously(plugin, () -> {
            plugin.reloadConfig();

            final String message;

            if (args[0].equals("regenerate")) {
                if (plugin.generateResourcePacks()) {
                    message = "Regenerated resource packs and reloaded successfully!";
                } else {
                    message = "Failed to regenerate resource packs!";
                }
            } else {
                message = "Reloaded successfully!";
            }

            plugin.loadRenames();

            sender.sendMessage(message);
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        return List.of("reload", "regenerate");
    }
}
