package dev.bountysystem.plugin.economy;

import dev.bountysystem.plugin.BountyPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;

/**
 * Ermittelt beim Start (und bei /bountyadmin reload) den zu verwendenden
 * Economy-Provider. Die Prioritaet entspricht der Vorgabe:
 *
 * <ol>
 *     <li>EternalEconomy ueber Vault, falls kompatibel verfuegbar</li>
 *     <li>ein anderer kompatibler Vault-Economy-Provider</li>
 *     <li>direkte EternalEconomy-API, falls erforderlich und verfuegbar</li>
 *     <li>keine Economy verfuegbar -&gt; klarer Fehler</li>
 * </ol>
 *
 * <p><b>Hinweis zu Stufe 3:</b> EternalEconomy verlangt laut eigener
 * Dokumentation (github.com/EternalCodeTeam/EternalEconomy) zwingend
 * Vault, um ueberhaupt als Economy-Provider zu funktionieren, und
 * stellt aktuell keine oeffentlich dokumentierte, eigenstaendige Java-API
 * fuer Fremdplugins bereit. Eine direkte Anbindung ohne Vault wuerde
 * daher zwangslaeufig auf frei erfundenen Klassen/Methoden beruhen -
 * das ist ausdruecklich untersagt. Stufe 3 erkennt diesen Fall deshalb
 * zuverlaessig (EternalEconomy ist installiert, aber ohne funktionierende
 * Vault-Anbindung) und meldet ihn dem Server-Betreiber klar und
 * verstaendlich, statt stillschweigend zu versagen oder erfundenen Code
 * auszufuehren.</p>
 */
public final class EconomyManager {

    private final BountyPlugin plugin;

    private EconomyProvider provider;
    private String unavailableReason;

    public EconomyManager(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Fuehrt die Erkennung durch. Gibt true zurueck, wenn danach ein
     * nutzbarer Economy-Provider zur Verfuegung steht.
     */
    public boolean initialize() {
        this.provider = null;
        this.unavailableReason = null;

        // Stufe 1 & 2: Vault-basierte Erkennung.
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        if (vault != null && vault.isEnabled()) {
            Economy economy = findPreferredVaultEconomy();
            if (economy != null) {
                this.provider = new VaultEconomyProvider(economy);
                plugin.getLogger().info("Economy-Anbindung aktiv: Vault (" + economy.getName() + ")");
                return true;
            }
        }

        // Stufe 3: EternalEconomy ist da, aber ohne funktionierende Vault-Bruecke.
        Plugin eternalEconomy = Bukkit.getPluginManager().getPlugin("EternalEconomy");
        if (eternalEconomy != null && eternalEconomy.isEnabled()) {
            plugin.getLogger().warning(
                    "EternalEconomy wurde gefunden, aber es konnte keine funktionierende Vault-Anbindung "
                            + "hergestellt werden. EternalEconomy benoetigt laut offizieller Dokumentation "
                            + "selbst das Vault-Plugin, um als Economy-Provider zu arbeiten, und stellt "
                            + "aktuell keine oeffentlich dokumentierte eigenstaendige API fuer Fremdplugins "
                            + "bereit. Um keine erfundene API zu verwenden, wird keine direkte Anbindung "
                            + "versucht. Bitte installiere zusaetzlich das Vault-Plugin."
            );
        }

        // Stufe 4: keine Economy verfuegbar.
        this.unavailableReason =
                "Kein kompatibles Economy-Plugin gefunden. Installiere Vault zusammen mit EternalEconomy "
                        + "oder einem anderen Vault-kompatiblen Economy-Plugin.";
        plugin.getLogger().severe(unavailableReason);
        return false;
    }

    /**
     * Sucht unter allen bei Vault registrierten Economy-Providern zuerst
     * gezielt nach EternalEconomy (Stufe 1). Falls dieser nicht registriert
     * ist, wird auf den von Vault als Standard gewaehlten Provider
     * zurueckgegriffen (Stufe 2).
     */
    private Economy findPreferredVaultEconomy() {
        List<RegisteredServiceProvider<Economy>> registrations =
                Bukkit.getServicesManager().getRegistrations(Economy.class);

        for (RegisteredServiceProvider<Economy> registration : registrations) {
            if (registration.getPlugin().getName().equalsIgnoreCase("EternalEconomy")) {
                return registration.getProvider();
            }
        }

        RegisteredServiceProvider<Economy> fallback = Bukkit.getServicesManager().getRegistration(Economy.class);
        return fallback != null ? fallback.getProvider() : null;
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
