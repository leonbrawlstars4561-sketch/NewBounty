package dev.bountysystem.plugin.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Persistiert alle aktiven Bountys in {@code bounties.yml}. Betraege
 * werden ueber {@link BigDecimal#toPlainString()} als Text gespeichert,
 * damit beim Neuladen exakt derselbe Wert (ohne Rundung oder
 * wissenschaftliche Notation) wiederhergestellt wird.
 */
public final class BountyStorage {

    private final Plugin plugin;
    private final File file;

    public BountyStorage(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bounties.yml");
    }

    /**
     * Laedt alle gespeicherten Bountys in die uebergebenen Maps.
     * Ungueltige Eintraege werden uebersprungen und protokolliert,
     * statt den kompletten Ladevorgang abzubrechen.
     */
    public void load(Map<UUID, BigDecimal> bounties, Map<UUID, String> playerNames) {
        if (!file.exists()) {
            return;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = configuration.getConfigurationSection("bounties");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                String amountText = section.getString(key + ".amount");
                String name = section.getString(key + ".name", "?");

                if (amountText == null) {
                    continue;
                }

                BigDecimal amount = new BigDecimal(amountText);
                if (amount.signum() > 0) {
                    bounties.put(playerId, amount);
                    playerNames.put(playerId, name);
                }
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoriere ungueltigen Bounty-Eintrag in bounties.yml: " + key);
            }
        }
    }

    /**
     * Schreibt den aktuellen Zustand vollstaendig und synchron auf die
     * Festplatte. Wird sofort nach jeder Aenderung aufgerufen, damit ein
     * Server-Neustart oder -Absturz keine Bountys verliert.
     */
    public synchronized void save(Map<UUID, BigDecimal> bounties, Map<UUID, String> playerNames) {
        YamlConfiguration configuration = new YamlConfiguration();

        for (Map.Entry<UUID, BigDecimal> entry : bounties.entrySet()) {
            String path = "bounties." + entry.getKey();
            configuration.set(path + ".amount", entry.getValue().toPlainString());
            configuration.set(path + ".name", playerNames.getOrDefault(entry.getKey(), "?"));
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Bountys konnten nicht gespeichert werden: " + exception.getMessage());
        }
    }
}
