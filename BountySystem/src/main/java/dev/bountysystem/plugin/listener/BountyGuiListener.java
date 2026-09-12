package dev.bountysystem.plugin.listener;

import dev.bountysystem.plugin.BountyPlugin;
import dev.bountysystem.plugin.gui.BountyConfirmationHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Schuetzt das Bounty-Bestaetigungs-GUI vollstaendig vor Item-Manipulation:
 * Klicks, Shift-Klicks und Drag&amp;Drop werden immer abgebrochen, sobald
 * das obere Inventar zu diesem GUI gehoert - unabhaengig davon, ob in das
 * GUI selbst oder in das eigene Spieler-Inventar geklickt wird (das
 * verhindert insbesondere, dass per Shift-Klick eigene Items in das GUI
 * hineingeschoben werden koennen).
 */
public final class BountyGuiListener implements Listener {

    private final BountyPlugin plugin;

    public BountyGuiListener(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();

        if (!(holder instanceof BountyConfirmationHolder confirmationHolder)) {
            return;
        }

        // Jeder Klick wird abgebrochen, solange dieses GUI offen ist -
        // egal ob im GUI selbst oder im Spieler-Inventar (Shift-Klick-Schutz).
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInventory)) {
            return;
        }

        int slot = event.getSlot();
        if (slot == BountyConfirmationHolder.CONFIRM_SLOT) {
            plugin.getConfirmationService().confirm(player, confirmationHolder);
        } else if (slot == BountyConfirmationHolder.CANCEL_SLOT) {
            plugin.getConfirmationService().cancel(player, confirmationHolder);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof BountyConfirmationHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BountyConfirmationHolder confirmationHolder) {
            plugin.getConfirmationService().handleClose(confirmationHolder);
        }
    }
}
