package dev.bountysystem.plugin.listener;

import dev.bountysystem.plugin.BountyPlugin;
import dev.bountysystem.plugin.economy.EconomyManager;
import dev.bountysystem.plugin.economy.EconomyProvider;
import dev.bountysystem.plugin.util.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.math.BigDecimal;

/**
 * Zahlt ein Bounty aus, sobald das Ziel stirbt:
 *
 * <ul>
 *     <li>Wird das Ziel von einem anderen Spieler getoetet, erhaelt dieser
 *     Spieler das Bounty.</li>
 *     <li>Stirbt das Ziel ohne einen anderen Spieler als Killer (z.B. durch
 *     Sturz, Lava, Ertrinken, den Befehl /kill oder sonstige
 *     Selbsttoetung), und hatte das Ziel ein Bounty auf sich selbst
 *     ausgesetzt, kassiert es dieses Bounty selbst.</li>
 * </ul>
 *
 * <p>{@link dev.bountysystem.plugin.data.BountyManager#clearBounty} entfernt
 * und liest den Betrag atomar in einem Schritt, sodass ein Bounty niemals
 * doppelt ausgezahlt werden kann.</p>
 */
@SuppressWarnings("deprecation")
public final class PlayerDeathListener implements Listener {

    private final BountyPlugin plugin;

    public PlayerDeathListener(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        boolean killedByOtherPlayer = killer != null && !killer.getUniqueId().equals(victim.getUniqueId());
        Player beneficiary = killedByOtherPlayer ? killer : victim;

        BigDecimal amount = plugin.getBountyManager().clearBounty(victim.getUniqueId());
        if (amount.signum() <= 0) {
            return;
        }

        EconomyManager economyManager = plugin.getEconomyManager();
        if (!economyManager.isAvailable()) {
            plugin.getLogger().warning("Bounty auf " + victim.getName()
                    + " konnte nicht ausgezahlt werden: kein Economy-Plugin verfuegbar.");
            return;
        }

        EconomyProvider provider = economyManager.getProvider();
        if (!provider.deposit(beneficiary, amount)) {
            plugin.getLogger().warning("Auszahlung des Bountys auf " + victim.getName()
                    + " an " + beneficiary.getName() + " ist fehlgeschlagen.");
            return;
        }

        String formatted = MoneyFormat.format(amount);
        String message = killedByOtherPlayer
                ? plugin.getMessages().bountyPaid(killer.getName(), victim.getName(), formatted)
                : plugin.getMessages().bountySelfPaid(victim.getName(), formatted);

        if (plugin.getConfigManager().isBroadcastPayout()) {
            Bukkit.broadcastMessage(message);
        } else {
            beneficiary.sendMessage(message);
        }
    }
}
