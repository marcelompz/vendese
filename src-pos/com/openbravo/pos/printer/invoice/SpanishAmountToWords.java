//    @vendese - Touch Friendly Point Of Sale
//    Fork of uniCenta oPOS - Copyright (c) 2009-2014 uniCenta & previous Openbravo POS works
//
//    This file is part of @vendese and is licensed under the GNU GPL v3.
//    See LICENSE.md for the full license text.

package com.openbravo.pos.printer.invoice;

/**
 * Convierte un importe entero a letras en español, sin decimales y sin
 * nombre de moneda, siguiendo la convención del formulario de referencia
 * (por ejemplo, 289000 → "Doscientos Ochenta Y Nueve Mil").
 */
public final class SpanishAmountToWords {

    private static final String[] UNITS = {
        "", "Uno", "Dos", "Tres", "Cuatro", "Cinco", "Seis", "Siete", "Ocho", "Nueve",
        "Diez", "Once", "Doce", "Trece", "Catorce", "Quince", "Dieciseis",
        "Diecisiete", "Dieciocho", "Diecinueve"
    };

    private static final String[] TENS = {
        "", "", "Veinte", "Treinta", "Cuarenta", "Cincuenta", "Sesenta", "Setenta", "Ochenta", "Noventa"
    };

    private static final String[] HUNDREDS = {
        "", "Cien", "Doscientos", "Trescientos", "Cuatrocientos", "Quinientos",
        "Seiscientos", "Setecientos", "Ochocientos", "Novecientos"
    };

    private static final String[] SCALES = {
        "", "Mil", "Millon", "Billon"
    };

    private SpanishAmountToWords() {
    }

    /** Redondea a enteros y devuelve el importe en letras. */
    public static String toWords(double amount) {
        long value = Math.round(amount);
        if (value == 0) {
            return "Cero";
        }
        if (value < 0) {
            return "Menos " + toWords(-value);
        }
        return groupToWords(value);
    }

    private static String groupToWords(long value) {
        StringBuilder result = new StringBuilder();
        int scale = 0;
        long remaining = value;

        while (remaining > 0) {
            int group = (int) (remaining % 1000);
            remaining = remaining / 1000;

            if (group > 0) {
                String groupText;
                if (group == 1 && scale == 1) {
                    groupText = "Mil";
                } else if (group == 1 && scale >= 2) {
                    groupText = "Un " + SCALES[scale];
                } else {
                    groupText = threeDigitsToWords(group, scale);
                }
                if (result.length() > 0) {
                    result.insert(0, " ");
                }
                result.insert(0, groupText);
            }
            scale++;
        }
        return result.toString();
    }

    private static String threeDigitsToWords(int group, int scale) {
        StringBuilder text = new StringBuilder();

        int hundreds = group / 100;
        int rest = group % 100;
        if (hundreds > 0) {
            if (hundreds == 1 && rest == 0) {
                text.append(HUNDREDS[1]);
            } else if (hundreds == 1) {
                text.append("Ciento");
            } else {
                text.append(HUNDREDS[hundreds]);
            }
        }

        if (rest > 0) {
            if (text.length() > 0) {
                text.append(" ");
            }
            if (rest < 20) {
                text.append(UNITS[rest]);
            } else {
                int tens = rest / 10;
                int units = rest % 10;
                if (units == 0) {
                    text.append(TENS[tens]);
                } else {
                    text.append(TENS[tens]).append(" Y ").append(UNITS[units]);
                }
            }
        }

        if (scale > 0 && scale < SCALES.length) {
            if (text.length() > 0) {
                text.append(" ");
            }
            if (scale == 1) {
                text.append("Mil");
            } else {
                text.append(SCALES[scale].concat("es"));
            }
        }
        return text.toString();
    }
}
