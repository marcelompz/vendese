package com.openbravo.pos.printer.invoice;

import static org.junit.Assert.*;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.printer.invoice.InvoiceData.TaxBucket;
import com.openbravo.pos.ticket.TaxInfo;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import java.util.Date;
import org.junit.Test;

/**
 * Tests del modelo de datos de la factura preimpresa.
 */
public class InvoiceDataTest {

    private static TaxInfo tax(String id, double rate) {
        return new TaxInfo(id, id, "", "", null, rate, false, null);
    }

    private static TicketInfo ticketWithThreeTaxGroups() {
        TicketInfo ticket = new TicketInfo();
        ticket.setDate(new Date(0L));
        ticket.addLine(new TicketLineInfo("p0", "Exento", "", 2.0, 100.0, tax("0", 0.0)));
        ticket.addLine(new TicketLineInfo("p5", "IVA5", "", 1.0, 100.0, tax("5", 0.05)));
        ticket.addLine(new TicketLineInfo("p10", "IVA10", "", 1.0, 100.0, tax("10", 0.10)));

        CustomerInfoExt customer = new CustomerInfoExt("c1");
        customer.setName("Diego Rodriguez");
        customer.setTaxid("2193067-8");
        customer.setAddress("Calle Central");
        customer.setPhone("123456");
        ticket.setCustomer(customer);
        return ticket;
    }

    @Test
    public void bucketsFollowPreprintedFormColumns() {
        assertEquals(TaxBucket.EXEMPT, InvoiceData.bucketForRate(0.0));
        assertEquals(TaxBucket.TAX5, InvoiceData.bucketForRate(0.05));
        assertEquals(TaxBucket.TAX10, InvoiceData.bucketForRate(0.10));
    }

    @Test
    public void totalsUseGrossAmountsLikeReferenceForm() {
        InvoiceData data = InvoiceData.fromTicket(ticketWithThreeTaxGroups());
        assertEquals(200.0, data.getExemptTotal(), 0.0001);
        assertEquals(105.0, data.getTax5Total(), 0.0001);
        assertEquals(110.0, data.getTax10Total(), 0.0001);
        assertEquals(5.0, data.getTax5Amount(), 0.0001);
        assertEquals(10.0, data.getTax10Amount(), 0.0001);
        assertEquals(415.0, data.getGrandTotal(), 0.0001);
        assertEquals("Cuatrocientos Quince", data.getAmountInWords());
    }

    @Test
    public void customerFieldsComeFromTicket() {
        InvoiceData data = InvoiceData.fromTicket(ticketWithThreeTaxGroups());
        assertEquals("Diego Rodriguez", data.getCustomerName());
        assertEquals("2193067-8", data.getCustomerRuc());
        assertEquals("Calle Central", data.getAddress());
        assertEquals("123456", data.getPhone());
        assertEquals("CONTADO (X)", data.getSaleCondition());
    }

    @Test
    public void nullTicketIsRejected() {
        try {
            InvoiceData.fromTicket(null);
            fail("Se esperaba IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected);
        }
    }
}
