package dev.bountysystem.plugin.command;

import dev.bountysystem.plugin.BountyPlugin;
import dev.bountysystem.plugin.economy.EconomyManager;
import dev.bountysystem.plugin.economy.EconomyProvider;
import dev.bountysystem.plugin.util.AmountParser;
import dev.bountysystem.plugin.util.InvalidAmountException;
import dev.bountysystem.plugin.util.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Verarbeitet:
 * <ul>
 *     <li>{@code /bounty add <Spieler> <Betrag>} - setzt ein Bounty aus
 *     (nach Bestaetigung im GUI). Ein Spieler darf ausdruecklich auch ein
 *     Bounty auf sich selbst aussetzen.</li>
 *     <li>{@code /bounty <Spieler>} - zeigt das aktuelle Bounty eines
 *     Spielers an.</li>
 * </ul>
 *
 * <p>Beim Aufruf von {@code /bounty add} passiert bei gueltiger Eingabe
 * ausschliesslich das Oeffnen des Bestaetigungs-GUI - es wird bewusst
 * keine Chat-Nachricht gesendet, bevor der Spieler bestaetigt oder
 * abbricht.</p>
 */
@SuppressWarnings("deprecation")
public final class BountyCommand implements CommandExecutor, TabCompleter {

    private final BountyPlugin plugin;

    public BountyCommand(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessages().usageAdd());
            sender.sendMessage(plugin.getMessages().usageShow());
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            handleAdd(sender, args);
        } else {
            handleShow(sender, args);
        }

        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bounty.add")) {
            sender.sendMessage(plugin.getMessages().noPermission());
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().playersOnly());
            return;
        }

        if (args.length != 3) {
            sender.sendMessage(plugin.getMessages().usageAdd());
            return;
        }

        EconomyManager economyManager = plugin.getEconomyManager();
        if (!economyManager.isAvailable()) {
            sender.sendMessage(plugin.getMessages().economyUnavailable());
            return;
        }

        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessages().playerNotFound(args[1]));
            return;
        }

        // Ein Bounty auf sich selbst ist ausdruecklich erlaubt: Stirbt der
        // Spieler spaeter ohne fremden Killer, kassiert er es selbst
        // (siehe PlayerDeathListener).

        BigDecimal amount;
        try {
            amount = AmountParser.parse(args[2]);
        } catch (InvalidAmountException exception) {
            sender.sendMessage(plugin.getMessages().invalidAmount(args[2]));
            return;
        }

        BigDecimal minimum = plugin.getConfigManager().getMinimumAmount();
        if (amount.compareTo(minimum) < 0) {
            sender.sendMessage(plugin.getMessages().amountTooLow(MoneyFormat.format(minimum)));
            return;
        }

        EconomyProvider provider = economyManager.getProvider();
        if (!provider.has(player, amount)) {
            sender.sendMessage(plugin.getMessages().insufficientFunds());
            return;
        }

        plugin.getConfirmationService().openConfirmation(player, target, amount);
    }

    private void handleShow(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bounty.show")) {
            sender.sendMessage(plugin.getMessages().noPermission());
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(plugin.getMessages().usageShow());
            return;
        }

        OfflinePlayer target = resolvePlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getMessages().playerNotFound(args[0]));
            return;
        }

        BigDecimal bounty = plugin.getBountyManager().getBounty(target.getUniqueId());
        String displayName = target.getName() != null ? target.getName() : args[0];

        if (bounty.signum() <= 0) {
            sender.sendMessage(plugin.getMessages().bountyShowNone(displayName));
        } else {
            sender.sendMessage(plugin.getMessages().bountyShowHas(displayName, MoneyFormat.format(bounty)));
        }
    }

    private OfflinePlayer resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            return offline;
        }

        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("add");
            suggestions.addAll(onlinePlayerNames());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            suggestions.addAll(onlinePlayerNames());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            suggestions.addAll(List.of("100", "1k", "1.5m", "1b", "10t"));
        }

        String currentArg = args.length > 0 ? args[args.length - 1] : "";
        return suggestions.stream()
                .filter(suggestion -> suggestion.toLowerCase().startsWith(currentArg.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }
}
