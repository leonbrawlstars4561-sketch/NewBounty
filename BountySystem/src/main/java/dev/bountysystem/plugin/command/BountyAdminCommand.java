package dev.bountysystem.plugin.command;

import dev.bountysystem.plugin.BountyPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * Verarbeitet {@code /bountyadmin reload} (Alias: {@code /bountyad reload}).
 * Laedt config.yml neu und fuehrt die Economy-Erkennung erneut aus, ohne
 * dass bereits ausgesetzte Bountys verloren gehen.
 */
@SuppressWarnings("deprecation")
public final class BountyAdminCommand implements CommandExecutor, TabCompleter {

    private final BountyPlugin plugin;

    public BountyAdminCommand(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bounty.admin")) {
            sender.sendMessage(plugin.getMessages().noPermission());
            return true;
        }

        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(plugin.getMessages().adminUsage(label));
            return true;
        }

        plugin.reload();

        if (plugin.getEconomyManager().isAvailable()) {
            sender.sendMessage(plugin.getMessages().reloadSuccess());
        } else {
            sender.sendMessage(plugin.getMessages().reloadEconomyWarning());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("reload");
        }
        return List.of();
    }
}
