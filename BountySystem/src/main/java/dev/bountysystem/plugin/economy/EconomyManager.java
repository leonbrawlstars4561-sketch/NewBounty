package dev.bountysystem.plugin.economy;

import dev.bountysystem.plugin.BountyPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Collection;

/**
 * Ermittelt beim Start (und bei /bountyadmin reload) den zu verwendenden
 * Economy-Provider.
 */
public final class EconomyManager {

    private final BountyPlugin plugin;

    private EconomyProvider provider;
    private String unavailableReason;

    public EconomyManager(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Fuehrt die Erkennung durch.
     *
     * @return true, wenn ein nutzbarer Economy-Provider gefunden wurde
     */
    public boolean initialize() {
        this.provider = null;
        this.unavailableReason = null;

        // Vault-basierte Economy-Erkennung.
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");

        if (vault != null && vault.isEnabled()) {
            Economy economy = findPreferredVaultEconomy();

            if (economy != null) {
                this.provider = new VaultEconomyProvider(economy);

                plugin.getLogger().info(
                        "Economy-Anbindung aktiv: Vault (" + economy.getName() + ")"
                );

                return true;
            }
        }

        // EternalEconomy erkannt, aber keine funktionierende Vault-Anbindung.
        Plugin eternalEconomy =
                Bukkit.getPluginManager().getPlugin("EternalEconomy");

        if (eternalEconomy != null && eternalEconomy.isEnabled()) {
            plugin.getLogger().warning(
                    "EternalEconomy wurde gefunden, aber es konnte keine "
                            + "funktionierende Vault-Anbindung hergestellt werden. "
                            + "Bitte stelle sicher, dass Vault installiert und "
                            + "EternalEconomy als Economy-Provider registriert ist."
            );
        }

        // Keine Economy gefunden.
        this.unavailableReason =
                "Kein kompatibles Economy-Plugin gefunden. "
                        + "Installiere Vault zusammen mit EternalEconomy "
                        + "oder einem anderen Vault-kompatiblen Economy-Plugin.";

        plugin.getLogger().severe(unavailableReason);

        return false;
    }

    /**
     * Sucht zuerst nach EternalEconomy.
     * Falls EternalEconomy nicht vorhanden ist, wird der von Vault
     * registrierte Standard-Economy-Provider verwendet.
     */
    private Economy findPreferredVaultEconomy() {

        Collection<RegisteredServiceProvider<Economy>> registrations =
                Bukkit.getServicesManager().getRegistrations(Economy.class);

        // Zuerst EternalEconomy bevorzugen.
        for (RegisteredServiceProvider<Economy> registration : registrations) {

            if (registration == null) {
                continue;
            }

            Plugin registrationPlugin = registration.getPlugin();

            if (registrationPlugin != null
                    && registrationPlugin.getName().equalsIgnoreCase("EternalEconomy")) {

                Economy economy = registration.getProvider();

                if (economy != null) {
                    return economy;
                }
            }
        }

        // Fallback auf den normalen Vault-Provider.
        RegisteredServiceProvider<Economy> fallback =
                Bukkit.getServicesManager().getRegistration(Economy.class);

        if (fallback != null) {
            return fallback.getProvider();
        }

        return null;
    }

    public boolean isAvailable() {
        return provider != null;
    }

    public EconomyProvider getProvider() {
        return provider;
    }

    public String getUnavailableReason() {
        return unavailableReason;
    }
}
