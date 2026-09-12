package dev.bountysystem.plugin.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Liest alle Chat-Texte aus config.yml und ersetzt Platzhalter sowie
 * Farbcodes (&amp;). Jede Nachricht ist ueber die Konfiguration frei
 * anpassbar; die hier hinterlegten Werte dienen nur als Fallback,
 * falls ein Schluessel in config.yml fehlt.
 */
@SuppressWarnings("deprecation")
public final class Messages {

    private final FileConfiguration config;

    public Messages(FileConfiguration config) {
        this.config = config;
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private String raw(String path, String fallback) {
        return colorize(config.getString(path, fallback));
    }

    private String replace(String message, String... replacements) {
        String result = message;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }
        return result;
    }

    public String usageAdd() {
        return raw("messages.usage-add", "&cVerwendung: /bounty add <Spieler> <Betrag>");
    }

    public String usageShow() {
        return raw("messages.usage-show", "&cVerwendung: /bounty <Spieler>");
    }

    public String adminUsage(String label) {
        return replace(raw("messages.admin-usage", "&cVerwendung: /%command% reload"), "%command%", label);
    }

    public String playersOnly() {
        return raw("messages.players-only", "&cDieser Befehl kann nur von Spielern verwendet werden.");
    }

    public String noPermission() {
        return raw("messages.no-permission", "&cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
    }

    public String playerNotFound(String name) {
        return replace(raw("messages.player-not-found", "&cSpieler &e%player% &cwurde nicht gefunden."),
                "%player%", name);
    }

    public String invalidAmount(String input) {
        return replace(raw("messages.invalid-amount",
                "&cUngueltiger Betrag: &e%amount%&c. Beispiele: 1000, 1.5k, 2.75b, 10t, 1q"),
                "%amount%", input);
    }

    public String amountTooLow(String minimum) {
        return replace(raw("messages.amount-too-low", "&cDer Betrag muss mindestens %minimum% betragen."),
                "%minimum%", minimum);
    }

    public String insufficientFunds() {
        return raw("messages.insufficient-funds", "&cDu hast nicht genuegend Geld fuer diesen Betrag.");
    }

    public String transactionFailed() {
        return raw("messages.transaction-failed", "&cDie Transaktion konnte nicht abgeschlossen werden.");
    }

    public String economyUnavailable() {
        return raw("messages.economy-unavailable",
                "&cEs ist derzeit kein Economy-Plugin verfuegbar. Bitte installiere Vault und ein "
                        + "kompatibles Economy-Plugin wie EternalEconomy.");
    }

    public String bountyAddedSender(String target, String amount) {
        return replace(raw("messages.bounty-added-sender", "&aYou added &e$%amount% &ato %target%'s bounty"),
                "%amount%", amount, "%target%", target);
    }

    public String bountyAddedTarget(String sender, String amount) {
        return replace(raw("messages.bounty-added-target", "&a%sender% added &e$%amount% &ato your Bounty"),
                "%sender%", sender, "%amount%", amount);
    }

    public String bountyCancelled() {
        return raw("messages.bounty-cancelled", "&7Bounty-Vorgang abgebrochen.");
    }

    public String bountyShowNone(String target) {
        return replace(raw("messages.bounty-show-none", "&7Auf &e%player% &7ist derzeit kein Bounty ausgesetzt."),
                "%player%", target);
    }

    public String bountyShowHas(String target, String amount) {
        return replace(raw("messages.bounty-show-has", "&7Bounty auf &e%player%&7: &a$%amount%"),
                "%player%", target, "%amount%", amount);
    }

    public String bountyPaid(String killer, String victim, String amount) {
        return replace(raw("messages.bounty-paid",
                "&6%killer% &7hat das Bounty auf &6%victim% &7in Hoehe von &a$%amount% &7eingeloest!"),
                "%killer%", killer, "%victim%", victim, "%amount%", amount);
    }

    public String bountySelfPaid(String player, String amount) {
        return replace(raw("messages.bounty-self-paid",
                "&6%player% &7hat sich selbst getoetet und das eigene Bounty in Hoehe von &a$%amount% &7kassiert!"),
                "%player%", player, "%amount%", amount);
    }

    public String reloadSuccess() {
        return raw("messages.reload-success", "&aKonfiguration wurde neu geladen.");
    }

    public String reloadEconomyWarning() {
        return raw("messages.reload-economy-warning",
                "&eKonfiguration wurde neu geladen, aber es wurde weiterhin kein Economy-Plugin gefunden.");
    }

    public String guiTitle() {
        return raw("messages.gui-title", "&8Bounty bestaetigen");
    }

    public String guiConfirmItemName() {
        return raw("messages.gui-confirm-item", "&aBestaetigen");
    }

    public String guiCancelItemName() {
        return raw("messages.gui-cancel-item", "&cAbbrechen");
    }

    public String guiInfoItemName(String target) {
        return replace(raw("messages.gui-info-item-name", "&eBounty auf %player%"), "%player%", target);
    }

    public List<String> guiInfoItemLore(String amount) {
        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Betrag: &a$" + amount));
        lore.add(colorize("&7Klicke auf Bestaetigen oder Abbrechen."));
        return lore;
    }
}
