package dev.bountysystem.plugin.data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet alle aktiven Bountys im Speicher und synchronisiert sie mit
 * {@link BountyStorage}. Alle Betraege werden ausschliesslich als
 * {@link BigDecimal} gehalten, damit die Summe mehrerer Bounty-Einzahlungen
 * exakt bleibt.
 *
 * <p>{@link #clearBounty(UUID)} entfernt und liefert den Betrag in einem
 * einzigen atomaren Schritt zurueck. Dadurch kann derselbe Bounty niemals
 * zweimal ausgezahlt werden, selbst wenn ein Auszahlungs-Event durch einen
 * Fehler mehrfach ausgeloest wuerde.</p>
 */
public final class BountyManager {

    private final Map<UUID, BigDecimal> bounties = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerNames = new ConcurrentHashMap<>();
    private final BountyStorage storage;
    private final Object lock = new Object();

    public BountyManager(BountyStorage storage) {
        this.storage = storage;
    }

    public void loadFromStorage() {
        synchronized (lock) {
            bounties.clear();
            playerNames.clear();
            storage.load(bounties, playerNames);
        }
    }

    public BigDecimal getBounty(UUID playerId) {
        return bounties.getOrDefault(playerId, BigDecimal.ZERO);
    }

    /**
     * Addiert den Betrag zum bestehenden Bounty des Spielers (oder legt
     * einen neuen an) und speichert den neuen Zustand sofort.
     */
    public void addBounty(UUID playerId, String playerName, BigDecimal amount) {
        synchronized (lock) {
            BigDecimal current = bounties.getOrDefault(playerId, BigDecimal.ZERO);
            bounties.put(playerId, current.add(amount));
            playerNames.put(playerId, playerName);
            storage.save(bounties, playerNames);
        }
    }

    /**
     * Entfernt den Bounty des Spielers atomar und gibt den zuvor
     * gespeicherten Betrag zurueck (oder {@link BigDecimal#ZERO}, wenn
     * kein Bounty vorhanden war). Speichert den neuen Zustand sofort.
     */
    public BigDecimal clearBounty(UUID playerId) {
        synchronized (lock) {
            BigDecimal removed = bounties.remove(playerId);
            if (removed == null) {
                return BigDecimal.ZERO;
            }
            storage.save(bounties, playerNames);
            return removed;
        }
    }

    public Map<UUID, BigDecimal> getAllBounties() {
        return bounties;
    }

    public Map<UUID, String> getPlayerNamesView() {
        return playerNames;
    }
}
