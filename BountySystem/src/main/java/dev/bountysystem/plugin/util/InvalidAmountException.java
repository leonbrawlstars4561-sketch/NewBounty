package dev.bountysystem.plugin.util;

/**
 * Wird geworfen, wenn eine vom Spieler eingegebene Betragsangabe
 * (z.B. bei /bounty add) nicht als gueltiger Geldbetrag interpretiert
 * werden kann.
 */
public final class InvalidAmountException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String rawInput;

    public InvalidAmountException(String rawInput) {
        super("Ungueltiger Betrag: " + rawInput);
        this.rawInput = rawInput;
    }

    public String getRawInput() {
        return rawInput;
    }
}
