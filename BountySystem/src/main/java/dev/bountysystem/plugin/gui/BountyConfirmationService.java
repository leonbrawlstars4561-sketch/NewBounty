package dev.bountysystem.plugin.gui;

import dev.bountysystem.plugin.BountyPlugin;
import dev.bountysystem.plugin.data.BountyManager;
import dev.bountysystem.plugin.economy.EconomyManager;
import dev.bountysystem.plugin.economy.EconomyProvider;
import dev.bountysystem.plugin.util.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Baut das Bestaetigungs-GUI fuer /bounty add auf und fuehrt die
 * eigentliche Transaktion erst dann aus, wenn der Spieler aktiv auf
 * "Bestaetigen" klickt. Vor dem Klick auf "Bestaetigen" passiert
 * bewusst NICHTS im Chat - keine Vorschau-Nachricht, keine Ankuendigung.
 */
@SuppressWarnings("deprecation")
public final class BountyConfirmationService {

    private final BountyPlugin plugin;
    private final Map<UUID, BountyConfirmationHolder> openConfirmations = new ConcurrentHashMap<>();

    public BountyConfirmationService(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Oeffnet das Bestaetigungs-GUI. Sendet absichtlich KEINE Chat-Nachricht.
     */
    public void openConfirmation(Player sender, OfflinePlayer target, BigDecimal amount) {
        BountyConfirmationHolder existing = openConfirmations.remove(sender.getUniqueId());
        if (existing != null) {
            existing.getProcessed().set(true);
        }

        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        BountyConfirmationHolder holder =
                new BountyConfirmationHolder(sender.getUniqueId(), target.getUniqueId(), targetName, amount);

        Inventory inventory = Bukkit.createInventory(holder, BountyConfirmationHolder.SIZE,
                plugin.getMessages().guiTitle());
        holder.setInventory(inventory);

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < BountyConfirmationHolder.SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(13, infoItem(targetName, amount));
        inventory.setItem(BountyConfirmationHolder.CONFIRM_SLOT,
                namedItem(Material.LIME_STAINED_GLASS_PANE, plugin.getMessages().guiConfirmItemName()));
        inventory.setItem(BountyConfirmationHolder.CANCEL_SLOT,
                namedItem(Material.RED_STAINED_GLASS_PANE, plugin.getMessages().guiCancelItemName()));

        openConfirmations.put(sender.getUniqueId(), holder);
        sender.openInventory(inventory);
    }

    /**
     * Wird beim Klick auf "Bestaetigen" ausgefuehrt. Durch
     * {@code compareAndSet} kann dieser Pfad fuer denselben Vorgang
     * garantiert nur ein einziges Mal durchlaufen werden.
     */
    public void confirm(Player sender, BountyConfirmationHolder holder) {
        if (!holder.getProcessed().compareAndSet(false, true)) {
            return;
        }
        openConfirmations.remove(holder.getSenderId());

        EconomyManager economyManager = plugin.getEconomyManager();
        if (!economyManager.isAvailable()) {
            sender.sendMessage(plugin.getMessages().economyUnavailable());
            closeNextTick(sender);
            return;
        }

        EconomyProvider provider = economyManager.getProvider();
        BigDecimal amount = holder.getAmount();

        if (!provider.has(sender, amount)) {
            sender.sendMessage(plugin.getMessages().insufficientFunds());
            closeNextTick(sender);
            return;
        }

        if (!provider.withdraw(sender, amount)) {
            sender.sendMessage(plugin.getMessages().transactionFailed());
            closeNextTick(sender);
            return;
        }

        BountyManager bountyManager = plugin.getBountyManager();
        bountyManager.addBounty(holder.getTargetId(), holder.getTargetName(), amount);

        String formatted = MoneyFormat.format(amount);
        sender.sendMessage(plugin.getMessages().bountyAddedSender(holder.getTargetName(), formatted));

        Player targetPlayer = Bukkit.getPlayer(holder.getTargetId());
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(plugin.getMessages().bountyAddedTarget(sender.getName(), formatted));
        }

        closeNextTick(sender);
    }

    /**
     * Wird beim Klick auf "Abbrechen" ausgefuehrt.
     */
    public void cancel(Player sender, BountyConfirmationHolder holder) {
        if (!holder.getProcessed().compareAndSet(false, true)) {
            return;
        }
        openConfirmations.remove(holder.getSenderId());
        sender.sendMessage(plugin.getMessages().bountyCancelled());
        closeNextTick(sender);
    }

    /**
     * Wird aufgerufen, wenn das Inventar auf andere Weise geschlossen wird
     * (z.B. durch Escape), ohne dass zuvor Bestaetigen/Abbrechen geklickt
     * wurde. Raeumt den Vorgang lediglich auf, ohne eine weitere Nachricht
     * zu senden oder Geld zu bewegen.
     */
    public void handleClose(BountyConfirmationHolder holder) {
        if (holder.getProcessed().compareAndSet(false, true)) {
            openConfirmations.remove(holder.getSenderId());
        }
    }

    private void closeNextTick(Player player) {
        Bukkit.getScheduler().runTask(plugin, player::closeInventory);
    }

    private ItemStack infoItem(String targetName, BigDecimal amount) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getMessages().guiInfoItemName(targetName));
            meta.setLore(plugin.getMessages().guiInfoItemLore(MoneyFormat.format(amount)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack namedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
