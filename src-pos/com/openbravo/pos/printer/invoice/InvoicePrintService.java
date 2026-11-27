//    @vendese - Touch Friendly Point Of Sale
//    Fork of uniCenta oPOS - Copyright (c) 2009-2014 uniCenta & previous Openbravo POS works
//
//    This file is part of @vendese and is licensed under the GNU GPL v3.
//    See LICENSE.md for the full license text.

package com.openbravo.pos.printer.invoice;

import com.openbravo.pos.ticket.TicketInfo;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.PrintService;

/**
 * Servicio de impresión de la factura preimpresa.
 *
 * Se activa con {@code machine.invoice.preprinted=true}. Es independiente de
 * {@code DeviceTicket}, por lo que el ticket térmico habitual no se modifica.
 */
public final class InvoicePrintService {

    private static final Logger LOGGER = Logger.getLogger(InvoicePrintService.class.getName());

    private final Properties properties;
    private final InvoiceTemplate template;

    public InvoicePrintService(Properties properties) {
        this(properties, InvoiceTemplate.load());
    }

    /** Constructor con plantilla inyectada, útil para pruebas. */
    public InvoicePrintService(Properties properties, InvoiceTemplate template) {
        this.properties = properties == null ? new Properties() : properties;
        this.template = template == null ? InvoiceTemplate.load() : template;
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(properties.getProperty("machine.invoice.preprinted", "false"));
    }

    /** Imprime solo si la opción está activada; nunca lanza hacia la UI. */
    public boolean printIfEnabled(TicketInfo ticket) {
        if (!isEnabled()) {
            return false;
        }
        return print(ticket);
    }

    /** Imprime la factura sobre la impresora configurada. */
    public boolean print(TicketInfo ticket) {
        if (ticket == null) {
            return false;
        }

        try {
            InvoiceData data = InvoiceData.fromTicket(ticket);
            PrinterJob job = PrinterJob.getPrinterJob();
            configurePrinter(job);

            double width = template.mmToPoints(template.getPageWidthMm());
            double height = template.mmToPoints(template.getPageHeightMm());
            Paper paper = new Paper();
            paper.setSize(width, height);
            paper.setImageableArea(0.0, 0.0, width, height);

            PageFormat pageFormat = job.defaultPage();
            pageFormat.setPaper(paper);
            pageFormat.setOrientation(PageFormat.PORTRAIT);

            job.setPrintable(new PreprintedInvoicePrintable(data, template), pageFormat);
            job.print();
            return true;
        } catch (PrinterException e) {
            LOGGER.log(Level.WARNING, "No se pudo imprimir la factura preimpresa", e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "No se pudo preparar la factura preimpresa", e);
        }
        return false;
    }

    private void configurePrinter(PrinterJob job) {
        String name = properties.getProperty("machine.invoice.printername", "(Default)");
        if (name == null || name.length() == 0 || "(Default)".equals(name)) {
            return;
        }
        PrintService[] services = PrinterJob.lookupPrintServices();
        for (PrintService service : services) {
            if (service.getName().equals(name)) {
                try {
                    job.setPrintService(service);
                } catch (PrinterException e) {
                    LOGGER.log(Level.WARNING, "No se pudo seleccionar la impresora " + name, e);
                }
                return;
            }
        }
        LOGGER.warning("No se encontró la impresora de facturas " + name + "; se usa la predeterminada");
    }
}
