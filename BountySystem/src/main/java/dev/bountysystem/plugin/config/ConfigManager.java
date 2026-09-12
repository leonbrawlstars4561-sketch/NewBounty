package dev.bountysystem.plugin.config;

import dev.bountysystem.plugin.BountyPlugin;
import dev.bountysystem.plugin.util.AmountParser;
import dev.bountysystem.plugin.util.InvalidAmountException;

import java.math.BigDecimal;

/**
 * Haelt typisierte, aus config.yml geladene Einstellungen (im Gegensatz
 * zu {@link Messages}, die reine Textbausteine verwaltet).
 */
public final class ConfigManager {

    private final BountyPlugin plugin;
    private BigDecimal minimumAmount;
    private boolean broadcastPayout;

    public ConfigManager(BountyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        loadMinimumAmount();
        this.broadcastPayout = plugin.getConfig().getBoolean("broadcast-bounty-payout", true);
    }

    private void loadMinimumAmount() {
        String raw = plugin.getConfig().getString("minimum-amount", "1");
        try {
            this.minimumAmount = AmountParser.parse(raw);
        } catch (InvalidAmountException exception) {
            plugin.getLogger().warning("Ungueltiger minimum-amount in config.yml, verwende 1.");
            this.minimumAmount = BigDecimal.ONE.setScale(2);
        }
    }

    public BigDecimal getMinimumAmount() {
        return minimumAmount;
    }

    public boolean isBroadcastPayout() {
        return broadcastPayout;
    }
}
