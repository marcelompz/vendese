package com.openbravo.pos.printer.invoice;

import static org.junit.Assert.*;
import com.openbravo.pos.printer.invoice.InvoiceData.TaxBucket;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import com.openbravo.pos.ticket.TaxInfo;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Test;

/**
 * Smoke test del render Java2D de la factura preimpresa.
 */
public class PreprintedInvoicePrintableTest {

    private static InvoiceData data() {
        TicketInfo ticket = new TicketInfo();
        ticket.setDate(new Date(0L));
        TaxInfo tax = new TaxInfo("10", "IVA10", "", "", null, 0.10, false, null);
        ticket.addLine(new TicketLineInfo("p1", "Producto", "", 2.0, 100.0, tax));
        return InvoiceData.fromTicket(ticket);
    }

    private static PageFormat pageFormat() {
        PageFormat page = new PageFormat();
        Paper paper = new Paper();
        paper.setSize(595.0, 935.0);
        paper.setImageableArea(0.0, 0.0, 595.0, 935.0);
        page.setPaper(paper);
        return page;
    }

    @Test
    public void firstPageExistsAndSecondDoesNot() throws Exception {
        PreprintedInvoicePrintable printable = new PreprintedInvoicePrintable(
                data(), InvoiceTemplate.fromProperties(new java.util.Properties()));
        BufferedImage image = new BufferedImage(800, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        try {
            assertEquals(Printable.PAGE_EXISTS, printable.print(g2d, pageFormat(), 0));
            assertEquals(Printable.NO_SUCH_PAGE, printable.print(g2d, pageFormat(), 1));
        } finally {
            g2d.dispose();
        }
    }

    @Test
    public void emptyTicketStillRendersTotals() throws Exception {
        List<InvoiceData.Line> noLines = new ArrayList<InvoiceData.Line>();
        InvoiceData empty = new InvoiceData("1", "01 de enero del 1970", "CONTADO (X)",
                "", "", "", "", noLines, 0, 0, 0, 0, 0, 0);
        PreprintedInvoicePrintable printable = new PreprintedInvoicePrintable(
                empty, InvoiceTemplate.fromProperties(new java.util.Properties()));
        BufferedImage image = new BufferedImage(800, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        try {
            assertEquals(Printable.PAGE_EXISTS, printable.print(g2d, pageFormat(), 0));
        } finally {
            g2d.dispose();
        }
        assertEquals(TaxBucket.EXEMPT, InvoiceData.bucketForRate(0));
    }
}
