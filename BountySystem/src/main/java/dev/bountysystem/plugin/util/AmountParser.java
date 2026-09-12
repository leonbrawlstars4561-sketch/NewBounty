package dev.bountysystem.plugin.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wandelt vom Spieler eingegebene Betragsangaben in einen exakten
 * {@link BigDecimal}-Wert um. Es wird bewusst NIE mit {@code double}
 * gerechnet, um Rundungsfehler bei grossen Geldbetraegen zu vermeiden.
 *
 * <p>Unterstuetzte Schreibweisen (Gross-/Kleinschreibung ist egal):</p>
 * <ul>
 *     <li>Reine Zahlen: {@code 1000000}, {@code 1000000.50}</li>
 *     <li>{@code k} = Tausend (10^3), z.B. {@code 1k}</li>
 *     <li>{@code m} = Million (10^6), z.B. {@code 1.5m}</li>
 *     <li>{@code b} = Milliarde (10^9), z.B. {@code 1b}, {@code 2.75b}</li>
 *     <li>{@code t} = Billion (10^12), z.B. {@code 10t}</li>
 *     <li>{@code q} = Billiarde (10^15), z.B. {@code 1q}</li>
 * </ul>
 */
public final class AmountParser {

    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("^(\\d+(?:\\.\\d+)?)([kmbtq]?)$", Pattern.CASE_INSENSITIVE);

    private AmountParser() {
    }

    /**
     * Parst die gegebene Eingabe in einen exakten, auf zwei Nachkommastellen
     * gerundeten Geldbetrag. Wirft eine {@link InvalidAmountException}, wenn
     * die Eingabe leer, negativ, null, nicht numerisch oder nicht dem
     * erwarteten Format entspricht.
     */
    public static BigDecimal parse(String input) throws InvalidAmountException {
        if (input == null) {
            throw new InvalidAmountException("null");
        }

        String trimmed = input.trim();
        Matcher matcher = AMOUNT_PATTERN.matcher(trimmed);

        if (!matcher.matches()) {
            throw new InvalidAmountException(input);
        }

        BigDecimal base;
        try {
            base = new BigDecimal(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw new InvalidAmountException(input);
        }

        String suffix = matcher.group(2).toLowerCase();
        BigDecimal multiplier = multiplierFor(suffix);
        BigDecimal result = base.multiply(multiplier);

        if (result.signum() <= 0) {
            throw new InvalidAmountException(input);
        }

        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal multiplierFor(String suffix) {
        return switch (suffix) {
            case "k" -> BigDecimal.valueOf(1_000L);
            case "m" -> BigDecimal.valueOf(1_000_000L);
            case "b" -> BigDecimal.valueOf(1_000_000_000L);
            case "t" -> BigDecimal.valueOf(1_000_000_000_000L);
            case "q" -> BigDecimal.valueOf(1_000_000_000_000_000L);
            default -> BigDecimal.ONE;
        };
    }
}
