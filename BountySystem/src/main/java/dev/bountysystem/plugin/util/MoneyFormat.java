package dev.bountysystem.plugin.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formatiert {@link BigDecimal}-Geldbetraege fuer die Chat-Anzeige
 * (z.B. {@code 1,500,000.00}). {@link DecimalFormat} formatiert
 * {@link BigDecimal}-Objekte intern verlustfrei (ohne Umweg ueber
 * {@code double}), wodurch auch sehr grosse Betraege exakt dargestellt
 * werden.
 */
public final class MoneyFormat {

    private MoneyFormat() {
    }

    public static String format(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);

        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        return format.format(scaled);
    }
}
