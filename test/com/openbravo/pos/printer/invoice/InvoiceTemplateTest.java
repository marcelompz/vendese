package com.openbravo.pos.printer.invoice;

import static org.junit.Assert.*;
import java.util.Properties;
import org.junit.Test;

/**
 * Tests de la plantilla de factura preimpresa.
 */
public class InvoiceTemplateTest {

    @Test
    public void defaultTemplateMatchesReferenceGeometry() {
        InvoiceTemplate template = InvoiceTemplate.fromProperties(new Properties());
        assertEquals(209.9, template.getPageWidthMm(), 0.0001);
        assertEquals(329.8, template.getPageHeightMm(), 0.0001);
        assertEquals(161.6, template.getCopy2YOffsetMm(), 0.0001);
        assertEquals(73.4, template.getDetailFirstY(), 0.0001);
    }

    @Test
    public void millimetersConvertToPrinterPoints() {
        InvoiceTemplate template = InvoiceTemplate.fromProperties(new Properties());
        assertEquals(72.0, template.mmToPoints(25.4), 0.0001);
        assertEquals(36.0, template.mmToPoints(12.7), 0.0001);
    }

    @Test
    public void overrideChangesOnlyProvidedKey() {
        Properties override = new Properties();
        override.setProperty("invoice.page.width.mm", "210");
        InvoiceTemplate template = InvoiceTemplate.fromProperties(override);
        assertEquals(210.0, template.getPageWidthMm(), 0.0001);
        assertEquals(329.8, template.getPageHeightMm(), 0.0001);
    }
}
