package dev.bountysystem.plugin.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * {@link EconomyProvider}-Implementierung auf Basis der echten Vault-API
 * ({@code net.milkbowl.vault.economy.Economy}). Vault selbst rechnet
 * intern ausschliesslich mit {@code double} - das ist eine feste
 * Eigenschaft der offiziellen Vault-Schnittstelle und kann ohne eine
 * erfundene API nicht umgangen werden.
 *
 * <p>Um Rundungsfehler so gering wie moeglich zu halten, wird jeder
 * {@link BigDecimal}-Betrag unmittelbar vor dem Aufruf an Vault exakt
 * auf zwei Nachkommastellen normalisiert (kaufmaennisch gerundet) und
 * erst dann in {@code double} umgewandelt. Ueberall sonst im Plugin
 * (Speicherung, Addition mehrerer Bountys, Anzeige) wird ausschliesslich
 * mit {@link BigDecimal} gerechnet.</p>
 */
public final class VaultEconomyProvider implements EconomyProvider {

    private final Economy economy;

    public VaultEconomyProvider(Economy economy) {
        this.economy = economy;
    }

    @Override
    public String getName() {
        return economy.getName();
    }

    @Override
    public BigDecimal getBalance(OfflinePlayer player) {
        return BigDecimal.valueOf(economy.getBalance(player));
    }

    @Override
    public boolean has(OfflinePlayer player, BigDecimal amount) {
        return economy.has(player, toVaultAmount(amount));
    }

    @Override
    public boolean withdraw(OfflinePlayer player, BigDecimal amount) {
        ensureAccount(player);
        EconomyResponse response = economy.withdrawPlayer(player, toVaultAmount(amount));
        return response != null && response.transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer player, BigDecimal amount) {
        ensureAccount(player);
        EconomyResponse response = economy.depositPlayer(player, toVaultAmount(amount));
        return response != null && response.transactionSuccess();
    }

    @Override
    public String format(BigDecimal amount) {
        return economy.format(toVaultAmount(amount));
    }

    private void ensureAccount(OfflinePlayer player) {
        if (!economy.hasAccount(player)) {
            economy.createPlayerAccount(player);
        }
    }

    private double toVaultAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
