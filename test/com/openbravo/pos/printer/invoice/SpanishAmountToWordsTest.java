package com.openbravo.pos.printer.invoice;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests del total en letras en español.
 */
public class SpanishAmountToWordsTest {

    @Test
    public void zeroAndSmallAmounts() {
        assertEquals("Cero", SpanishAmountToWords.toWords(0));
        assertEquals("Uno", SpanishAmountToWords.toWords(1));
        assertEquals("Veintiuno", SpanishAmountToWords.toWords(21));
    }

    @Test
    public void referenceInvoiceTotal() {
        assertEquals("Doscientos Ochenta Y Nueve Mil", SpanishAmountToWords.toWords(289000));
    }

    @Test
    public void thousandsAndMillions() {
        assertEquals("Mil", SpanishAmountToWords.toWords(1000));
        assertEquals("Dos Mil", SpanishAmountToWords.toWords(2000));
        assertEquals("Un Millon", SpanishAmountToWords.toWords(1000000));
        assertEquals("Dos Millones", SpanishAmountToWords.toWords(2000000));
    }

    @Test
    public void negativeAmount() {
        assertEquals("Menos Cien", SpanishAmountToWords.toWords(-100));
    }
}
