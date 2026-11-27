//    @vendese - Touch Friendly Point Of Sale
//    Fork of uniCenta oPOS - Copyright (c) 2009-2014 uniCenta & previous Openbravo POS works
//
//    This file is part of @vendese and is licensed under the GNU GPL v3.
//    See LICENSE.md for the full license text.

package com.openbravo.pos.printer.invoice;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Render de la factura sobre el formulario preimpreso.
 *
 * Las coordenadas de la plantilla son milímetros desde la esquina superior
 * izquierda; se convierten a puntos con {@code mm * 72 / 25.4}. Se imprimen
 * dos copias, la segunda desplazada en Y según la plantilla.
 */
public final class PreprintedInvoicePrintable implements Printable {

    private static final DecimalFormat AMOUNT_FORMAT = amountFormat();
    private static final DecimalFormat QUANTITY_FORMAT = quantityFormat();

    private final InvoiceData data;
    private final InvoiceTemplate template;

    public PreprintedInvoicePrintable(InvoiceData data, InvoiceTemplate template) {
        if (data == null || template == null) {
            throw new IllegalArgumentException("Datos y plantilla son obligatorios");
        }
        this.data = data;
        this.template = template;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex != 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            drawCopy(g2d, 0.0);
            drawCopy(g2d, template.getCopy2YOffsetMm());
            return PAGE_EXISTS;
        } finally {
            g2d.dispose();
        }
    }

    private void drawCopy(Graphics2D g2d, double yOffsetMm) {
        Font baseFont = new Font(template.getFontName(), Font.PLAIN, template.getFontSizePt());
        Font totalFont = new Font(template.getFontName(), Font.PLAIN, template.getTotalFontSizePt());
        g2d.setFont(baseFont);

        if (template.getDocumentNumberY() >= 0) {
            draw(g2d, data.getDocumentNumber(), template.getDocumentNumberX(),
                    template.getDocumentNumberY() + yOffsetMm);
        }
        draw(g2d, data.getDateText(), template.getDateX(), template.getDateY() + yOffsetMm);
        draw(g2d, data.getSaleCondition(), template.getSaleConditionX(),
                template.getSaleConditionY() + yOffsetMm);
        draw(g2d, data.getCustomerName(), template.getCustomerNameX(),
                template.getCustomerNameY() + yOffsetMm);
        draw(g2d, data.getCustomerRuc(), template.getCustomerRucX(),
                template.getCustomerRucY() + yOffsetMm);
        draw(g2d, data.getAddress(), template.getAddressX(), template.getAddressY() + yOffsetMm);
        draw(g2d, data.getPhone(), template.getPhoneX(), template.getPhoneY() + yOffsetMm);

        double y = template.getDetailFirstY();
        double step = template.getDetailLineStepMm();
        List<InvoiceData.Line> lines = data.getLines();
        int count = Math.min(lines.size(), template.getDetailMaxLines());
        for (int i = 0; i < count; i++) {
            InvoiceData.Line line = lines.get(i);
            String description = truncate(cleanDescription(line.getDescription()),
                    template.getDescriptionMaxChars());

            draw(g2d, QUANTITY_FORMAT.format(line.getQuantity()), template.getQtyX(), y + yOffsetMm);
            draw(g2d, description, template.getDescriptionX(), y + yOffsetMm);
            draw(g2d, AMOUNT_FORMAT.format(line.getUnitPrice()), template.getUnitPriceX(), y + yOffsetMm);

            if (line.getBucket() == InvoiceData.TaxBucket.EXEMPT) {
                draw(g2d, AMOUNT_FORMAT.format(line.getAmount()), template.getAmountExemptX(), y + yOffsetMm);
            } else if (line.getBucket() == InvoiceData.TaxBucket.TAX5) {
                draw(g2d, AMOUNT_FORMAT.format(line.getAmount()), template.getAmountTax5X(), y + yOffsetMm);
            } else {
                draw(g2d, AMOUNT_FORMAT.format(line.getAmount()), template.getAmountTax10X(), y + yOffsetMm);
            }

            if (description.length() > template.getDescriptionWrapChars()) {
                y += step;
            }
            y += step;
        }

        draw(g2d, AMOUNT_FORMAT.format(data.getExemptTotal()), template.getTotalExemptX(),
                template.getTotalsY() + yOffsetMm);
        draw(g2d, AMOUNT_FORMAT.format(data.getTax5Total()), template.getTotalTax5X(),
                template.getTotalsY() + yOffsetMm);
        draw(g2d, AMOUNT_FORMAT.format(data.getTax10Total()), template.getTotalTax10X(),
                template.getTotalsY() + yOffsetMm);

        g2d.setFont(totalFont);
        draw(g2d, AMOUNT_FORMAT.format(data.getGrandTotal()), template.getGrandTotalX(),
                template.getGrandTotalY() + yOffsetMm);
        g2d.setFont(baseFont);

        draw(g2d, data.getAmountInWords(), template.getAmountInWordsX(),
                template.getAmountInWordsY() + yOffsetMm);
        draw(g2d, AMOUNT_FORMAT.format(data.getTax5Amount()), template.getTaxIva5X(),
                template.getTaxY() + yOffsetMm);
        draw(g2d, AMOUNT_FORMAT.format(data.getTax10Amount()), template.getTaxIva10X(),
                template.getTaxY() + yOffsetMm);
        draw(g2d, AMOUNT_FORMAT.format(data.getTax5Amount() + data.getTax10Amount()),
                template.getTaxTotalX(), template.getTaxY() + yOffsetMm);
    }

    private void draw(Graphics2D g2d, String text, double xMm, double yMm) {
        if (text == null || text.length() == 0) {
            return;
        }
        g2d.drawString(text, (float) template.mmToPoints(xMm), (float) template.mmToPoints(yMm));
    }

    private static String cleanDescription(String description) {
        if (description == null) {
            return "";
        }
        int paren = description.indexOf(" (");
        if (paren > 0) {
            return description.substring(0, paren);
        }
        return description;
    }

    private static String truncate(String value, int maxChars) {
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars);
    }

    private static DecimalFormat amountFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "ES"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("###,##0", symbols);
        format.setGroupingUsed(true);
        return format;
    }

    private static DecimalFormat quantityFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "ES"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("0.0", symbols);
    }
}
