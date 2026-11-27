package com.openbravo.pos.printer.invoice;

import static org.junit.Assert.*;
import java.util.Properties;
import org.junit.Test;

/**
 * Tests de la configuración que activa la factura preimpresa.
 */
public class InvoicePrintServiceTest {

    @Test
    public void disabledByDefault() {
        InvoicePrintService service = new InvoicePrintService(new Properties(),
                InvoiceTemplate.fromProperties(new Properties()));
        assertFalse(service.isEnabled());
    }

    @Test
    public void enabledByProperty() {
        Properties props = new Properties();
        props.setProperty("machine.invoice.preprinted", "true");
        InvoicePrintService service = new InvoicePrintService(props,
                InvoiceTemplate.fromProperties(new Properties()));
        assertTrue(service.isEnabled());
    }

    @Test
    public void printIfEnabledDoesNothingWhenDisabled() {
        InvoicePrintService service = new InvoicePrintService(new Properties(),
                InvoiceTemplate.fromProperties(new Properties()));
        assertFalse(service.printIfEnabled(null));
    }
}
