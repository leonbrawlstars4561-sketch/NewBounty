package dev.bountysystem.plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Markiert ein Inventar als Bounty-Bestaetigungs-GUI und traegt alle
 * fuer die Bestaetigung noetigen Daten. {@link #getProcessed()} wird
 * per {@link AtomicBoolean#compareAndSet(boolean, boolean)} verwendet,
 * um sicherzustellen, dass ein Bounty-Vorgang (Bestaetigen/Abbrechen)
 * unter keinen Umstaenden doppelt verarbeitet werden kann - auch nicht
 * bei sehr schnellem Doppelklick.
 */
public final class BountyConfirmationHolder implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int CONFIRM_SLOT = 11;
    public static final int CANCEL_SLOT = 15;

    private final UUID senderId;
    private final UUID targetId;
    private final String targetName;
    private final BigDecimal amount;
    private final AtomicBoolean processed = new AtomicBoolean(false);

    private Inventory inventory;

    public BountyConfirmationHolder(UUID senderId, UUID targetId, String targetName, BigDecimal amount) {
        this.senderId = senderId;
        this.targetId = targetId;
        this.targetName = targetName;
        this.amount = amount;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public AtomicBoolean getProcessed() {
        return processed;
    }
}
