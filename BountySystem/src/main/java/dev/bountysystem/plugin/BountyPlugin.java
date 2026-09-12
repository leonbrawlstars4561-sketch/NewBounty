package dev.bountysystem.plugin;

import dev.bountysystem.plugin.command.BountyAdminCommand;
import dev.bountysystem.plugin.command.BountyCommand;
import dev.bountysystem.plugin.config.ConfigManager;
import dev.bountysystem.plugin.config.Messages;
import dev.bountysystem.plugin.data.BountyManager;
import dev.bountysystem.plugin.data.BountyStorage;
import dev.bountysystem.plugin.economy.EconomyManager;
import dev.bountysystem.plugin.gui.BountyConfirmationService;
import dev.bountysystem.plugin.listener.BountyGuiListener;
import dev.bountysystem.plugin.listener.PlayerDeathListener;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Haupteinstiegspunkt von BountySystem. Verdrahtet Economy-Erkennung,
 * Persistenz, GUI-Service, Befehle und Listener miteinander.
 */
public final class BountyPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private Messages messages;
    private EconomyManager economyManager;
    private BountyStorage bountyStorage;
    private BountyManager bountyManager;
    private BountyConfirmationService confirmationService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messages = new Messages(getConfig());
        this.configManager = new ConfigManager(this);

        this.economyManager = new EconomyManager(this);
        this.economyManager.initialize();

        this.bountyStorage = new BountyStorage(this);
        this.bountyManager = new BountyManager(bountyStorage);
        this.bountyManager.loadFromStorage();

        this.confirmationService = new BountyConfirmationService(this);

        getServer().getPluginManager().registerEvents(new BountyGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);

        registerCommand("bounty", new BountyCommand(this));
        registerCommand("bountyadmin", new BountyAdminCommand(this));

        getLogger().info("BountySystem wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        if (bountyManager != null && bountyStorage != null) {
            bountyStorage.save(bountyManager.getAllBounties(), bountyManager.getPlayerNamesView());
        }
        getLogger().info("BountySystem wurde deaktiviert.");
    }

    /**
     * Laedt config.yml neu und wiederholt die Economy-Erkennung, ohne
     * bereits gespeicherte Bountys zu beeinflussen. Wird von
     * {@code /bountyadmin reload} aufgerufen.
     */
    public void reload() {
        reloadConfig();
        this.messages = new Messages(getConfig());
        this.configManager.reload();
        this.economyManager.initialize();
    }

    private <T extends CommandExecutor & TabCompleter> void registerCommand(String name, T executor) {
        PluginCommand pluginCommand = getCommand(name);
        if (pluginCommand == null) {
            getLogger().severe("Befehl '" + name + "' konnte nicht registriert werden. Pruefe die plugin.yml.");
            return;
        }
        pluginCommand.setExecutor(executor);
        pluginCommand.setTabCompleter(executor);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Messages getMessages() {
        return messages;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public BountyManager getBountyManager() {
        return bountyManager;
    }

    public BountyConfirmationService getConfirmationService() {
        return confirmationService;
    }
}
