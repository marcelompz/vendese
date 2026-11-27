//    @vendese - Touch Friendly Point Of Sale
//    Fork of uniCenta oPOS - Copyright (c) 2009-2014 uniCenta & previous Openbravo POS works
//
//    This file is part of @vendese and is licensed under the GNU GPL v3.
//    See LICENSE.md for the full license text.

package com.openbravo.pos.printer.invoice;

import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Datos ya calculados que necesita el render de la factura preimpresa.
 * Se construyen desde el ticket cerrado, sin tocar el esquema SQL.
 */
public final class InvoiceData {

    /** Grupos fiscales del formulario de referencia (exento / IVA 5 % / IVA 10 %). */
    public enum TaxBucket {
        EXEMPT,
        TAX5,
        TAX10
    }

    /** Línea de detalle con el importe ya gravado, como exige el formulario. */
    public static final class Line {
        private final String description;
        private final double quantity;
        private final double unitPrice;
        private final double amount;
        private final TaxBucket bucket;

        public Line(String description, double quantity, double unitPrice, double amount, TaxBucket bucket) {
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.amount = amount;
            this.bucket = bucket;
        }

        public String getDescription() {
            return description;
        }

        public double getQuantity() {
            return quantity;
        }

        public double getUnitPrice() {
            return unitPrice;
        }

        public double getAmount() {
            return amount;
        }

        public TaxBucket getBucket() {
            return bucket;
        }
    }

    private final String documentNumber;
    private final String dateText;
    private final String saleCondition;
    private final String customerName;
    private final String customerRuc;
    private final String address;
    private final String phone;
    private final List<Line> lines;
    private final double exemptTotal;
    private final double tax5Total;
    private final double tax10Total;
    private final double tax5Amount;
    private final double tax10Amount;
    private final double grandTotal;
    private final String amountInWords;

    InvoiceData(String documentNumber, String dateText, String saleCondition,
            String customerName, String customerRuc, String address, String phone,
            List<Line> lines, double exemptTotal, double tax5Total, double tax10Total,
            double tax5Amount, double tax10Amount, double grandTotal) {
        this.documentNumber = documentNumber;
        this.dateText = dateText;
        this.saleCondition = saleCondition;
        this.customerName = customerName;
        this.customerRuc = customerRuc;
        this.address = address;
        this.phone = phone;
        this.lines = lines;
        this.exemptTotal = exemptTotal;
        this.tax5Total = tax5Total;
        this.tax10Total = tax10Total;
        this.tax5Amount = tax5Amount;
        this.tax10Amount = tax10Amount;
        this.grandTotal = grandTotal;
        this.amountInWords = SpanishAmountToWords.toWords(grandTotal);
    }

    /** Construye los datos de la factura a partir de un ticket ya cobrado. */
    public static InvoiceData fromTicket(TicketInfo ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("El ticket no puede ser nulo");
        }

        List<Line> lines = new ArrayList<Line>();
        double exemptTotal = 0.0;
        double tax5Total = 0.0;
        double tax10Total = 0.0;
        double tax5Amount = 0.0;
        double tax10Amount = 0.0;
        double grandTotal = 0.0;

        for (TicketLineInfo ticketLine : ticket.getLines()) {
            TaxBucket bucket = bucketForRate(ticketLine.getTaxRate());
            double gross = ticketLine.getValue();
            double net = ticketLine.getSubValue();
            String name = ticketLine.getProductName();
            if (name == null || name.length() == 0) {
                name = ticketLine.getProductID();
            }
            lines.add(new Line(nullToEmpty(name), ticketLine.getMultiply(),
                    ticketLine.getPrice(), gross, bucket));

            if (bucket == TaxBucket.EXEMPT) {
                exemptTotal += gross;
            } else if (bucket == TaxBucket.TAX5) {
                tax5Total += gross;
                tax5Amount += gross - net;
            } else {
                tax10Total += gross;
                tax10Amount += gross - net;
            }
            grandTotal += gross;
        }

        CustomerInfoExt customer = ticket.getCustomer();
        String name = customer == null ? "" : nullToEmpty(customer.getName());
        String taxid = customer == null ? "" : nullToEmpty(customer.getTaxid());
        String address = customer == null ? "" : joinAddress(customer);
        String phone = customer == null ? "" : nullToEmpty(customer.getPhone());

        return new InvoiceData(safeDocumentNumber(ticket), formatDate(ticket.getDate()),
                "CONTADO (X)", name, taxid, address, phone, lines,
                exemptTotal, tax5Total, tax10Total, tax5Amount, tax10Amount, grandTotal);
    }

    /**
     * El formulario solo tiene tres columnas. Cero es exento; hasta 7,5 % se
     * considera IVA 5 % y el resto IVA 10 %. Documentado en el diseño.
     */
    public static TaxBucket bucketForRate(double rate) {
        if (rate <= 0.0001) {
            return TaxBucket.EXEMPT;
        }
        if (rate <= 0.075) {
            return TaxBucket.TAX5;
        }
        return TaxBucket.TAX10;
    }

    private static String joinAddress(CustomerInfoExt customer) {
        StringBuilder sb = new StringBuilder();
        append(sb, customer.getAddress());
        append(sb, customer.getAddress2());
        append(sb, customer.getCity());
        return sb.toString();
    }

    private static void append(StringBuilder sb, String value) {
        if (value == null || value.length() == 0) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(value);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String safeDocumentNumber(TicketInfo ticket) {
        try {
            return nullToEmpty(ticket.printId());
        } catch (RuntimeException e) {
            return ticket.getTicketId() > 0 ? Integer.toString(ticket.getTicketId()) : "";
        }
    }

    private static String formatDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("dd 'de' MMMM 'del' yyyy", new Locale("es", "ES"));
        return format.format(date == null ? new Date() : date);
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getDateText() {
        return dateText;
    }

    public String getSaleCondition() {
        return saleCondition;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerRuc() {
        return customerRuc;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public List<Line> getLines() {
        return lines;
    }

    public double getExemptTotal() {
        return exemptTotal;
    }

    public double getTax5Total() {
        return tax5Total;
    }

    public double getTax10Total() {
        return tax10Total;
    }

    public double getTax5Amount() {
        return tax5Amount;
    }

    public double getTax10Amount() {
        return tax10Amount;
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    public String getAmountInWords() {
        return amountInWords;
    }
}
