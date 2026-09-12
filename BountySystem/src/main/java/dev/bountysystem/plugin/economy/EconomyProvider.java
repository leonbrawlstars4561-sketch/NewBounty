package dev.bountysystem.plugin.economy;

import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;

/**
 * Abstraktion ueber das tatsaechlich verwendete Economy-Backend.
 * Der Rest des Plugins rechnet ausschliesslich mit {@link BigDecimal},
 * damit interne Betraege exakt bleiben. Eine konkrete Implementierung
 * (z.B. {@link VaultEconomyProvider}) ist dafuer verantwortlich, die
 * Konvertierung zur zugrunde liegenden API korrekt durchzufuehren.
 */
public interface EconomyProvider {

    /**
     * Der Anzeigename des aktiven Economy-Providers (z.B. "EternalEconomy").
     */
    String getName();

    /**
     * Aktueller Kontostand des Spielers.
     */
    BigDecimal getBalance(OfflinePlayer player);

    /**
     * Prueft, ob der Spieler mindestens den angegebenen Betrag besitzt.
     */
    boolean has(OfflinePlayer player, BigDecimal amount);

    /**
     * Zieht dem Spieler den angegebenen Betrag ab.
     *
     * @return true, wenn die Transaktion erfolgreich war.
     */
    boolean withdraw(OfflinePlayer player, BigDecimal amount);

    /**
     * Zahlt dem Spieler den angegebenen Betrag aus.
     *
     * @return true, wenn die Transaktion erfolgreich war.
     */
    boolean deposit(OfflinePlayer player, BigDecimal amount);

    /**
     * Formatiert einen Betrag im Stil des Economy-Providers
     * (z.B. inklusive dessen Waehrungssymbol).
     */
    String format(BigDecimal amount);
}
