//    @vendese - Touch Friendly Point Of Sale
//    Fork of uniCenta oPOS - Copyright (c) 2009-2014 uniCenta & previous Openbravo POS works
//    http://www.unicenta.com
//
//    This file is part of @vendese.
//
//    @vendese is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    @vendese is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with @vendese.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.printer.invoice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plantilla de posiciones para la factura preimpresa.
 *
 * Las coordenadas se expresan en milímetros desde la esquina superior
 * izquierda y se convierten a puntos de impresión con {@code mm * 72 / 25.4}.
 * La plantilla versionada vive en {@code reports/invoice-template.properties};
 * el usuario puede copiarla a {@code %USERPROFILE%\invoice-template.properties}
 * para ajustar la alineación sin recompilar.
 */
public final class InvoiceTemplate {

    private static final Logger LOGGER = Logger.getLogger(InvoiceTemplate.class.getName());

    private static final String RESOURCE = "reports/invoice-template.properties";
    private static final String USER_FILE = "invoice-template.properties";

    private final Properties properties;

    private InvoiceTemplate(Properties properties) {
        this.properties = properties;
    }

    /**
     * Carga los valores por defecto, la plantilla del classpath y el override
     * del directorio personal del usuario, en ese orden de precedencia.
     */
    public static InvoiceTemplate load() {
        Properties props = defaultProperties();
        InputStream in = null;
        try {
            in = InvoiceTemplate.class.getClassLoader().getResourceAsStream(RESOURCE);
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo leer " + RESOURCE + "; se usan valores por defecto", e);
        } finally {
            close(in);
        }

        File override = new File(System.getProperty("user.home"), USER_FILE);
        if (override.isFile()) {
            InputStream userIn = null;
            try {
                userIn = new FileInputStream(override);
                props.load(userIn);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "No se pudo leer " + override + "; se ignora el override", e);
            } finally {
                close(userIn);
            }
        }
        return new InvoiceTemplate(props);
    }

    /** Construye una plantilla a partir de propiedades ya cargadas (para tests). */
    public static InvoiceTemplate fromProperties(Properties overrides) {
        Properties props = defaultProperties();
        if (overrides != null) {
            props.putAll(overrides);
        }
        return new InvoiceTemplate(props);
    }

    private static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "No se pudo cerrar el flujo de la plantilla de factura", e);
            }
        }
    }

    /** Propiedades por defecto, medidas sobre el formulario de referencia Odoo/Provecchio. */
    public static Properties defaultProperties() {
        Properties p = new Properties();
        p.setProperty("invoice.page.width.mm", "209.9");
        p.setProperty("invoice.page.height.mm", "329.8");
        p.setProperty("invoice.font.name", "DejaVuSans");
        p.setProperty("invoice.font.size.pt", "6.8");
        p.setProperty("invoice.font.total.size.pt", "12");
        p.setProperty("invoice.copy2.yOffset.mm", "161.6");

        p.setProperty("invoice.field.documentNumber.x.mm", "18.3");
        p.setProperty("invoice.field.documentNumber.y.mm", "-1");
        p.setProperty("invoice.field.date.x.mm", "42.9");
        p.setProperty("invoice.field.date.y.mm", "51.8");
        p.setProperty("invoice.field.saleCondition.x.mm", "184.2");
        p.setProperty("invoice.field.saleCondition.y.mm", "51.8");
        p.setProperty("invoice.field.customerName.x.mm", "51.8");
        p.setProperty("invoice.field.customerName.y.mm", "55.9");
        p.setProperty("invoice.field.customerRuc.x.mm", "164.7");
        p.setProperty("invoice.field.customerRuc.y.mm", "55.6");
        p.setProperty("invoice.field.address.x.mm", "18.3");
        p.setProperty("invoice.field.address.y.mm", "60.1");
        p.setProperty("invoice.field.phone.x.mm", "155.8");
        p.setProperty("invoice.field.phone.y.mm", "59.6");

        p.setProperty("invoice.detail.first.y.mm", "73.4");
        p.setProperty("invoice.detail.lineStep.mm", "3.25");
        p.setProperty("invoice.detail.maxLines", "20");
        p.setProperty("invoice.detail.qty.x.mm", "21.9");
        p.setProperty("invoice.detail.description.x.mm", "33.2");
        p.setProperty("invoice.detail.description.maxChars", "80");
        p.setProperty("invoice.detail.description.wrapChars", "72");
        p.setProperty("invoice.detail.unitPrice.x.mm", "125.0");
        p.setProperty("invoice.detail.amountExempt.x.mm", "148.3");
        p.setProperty("invoice.detail.amountTax5.x.mm", "180.3");
        p.setProperty("invoice.detail.amountTax10.x.mm", "195.4");

        p.setProperty("invoice.total.exempt.x.mm", "146.8");
        p.setProperty("invoice.total.tax5.x.mm", "180.3");
        p.setProperty("invoice.total.tax10.x.mm", "195.4");
        p.setProperty("invoice.totals.y.mm", "131.6");
        p.setProperty("invoice.grandTotal.x.mm", "187.1");
        p.setProperty("invoice.grandTotal.y.mm", "136.8");
        p.setProperty("invoice.amountInWords.x.mm", "18.3");
        p.setProperty("invoice.amountInWords.y.mm", "140.7");
        p.setProperty("invoice.tax.iva5.x.mm", "96.7");
        p.setProperty("invoice.tax.iva10.x.mm", "137.3");
        p.setProperty("invoice.tax.total.x.mm", "187.0");
        p.setProperty("invoice.tax.y.mm", "147.1");
        return p;
    }

    /** Convierte milímetros a puntos de impresión. */
    public double mmToPoints(double mm) {
        return mm * 72.0 / 25.4;
    }

    private double getDouble(String key) {
        return Double.parseDouble(properties.getProperty(key));
    }

    private int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public double getPageWidthMm() {
        return getDouble("invoice.page.width.mm");
    }

    public double getPageHeightMm() {
        return getDouble("invoice.page.height.mm");
    }

    public String getFontName() {
        return properties.getProperty("invoice.font.name");
    }

    public int getFontSizePt() {
        return (int) Math.round(getDouble("invoice.font.size.pt"));
    }

    public int getTotalFontSizePt() {
        return (int) Math.round(getDouble("invoice.font.total.size.pt"));
    }

    public double getCopy2YOffsetMm() {
        return getDouble("invoice.copy2.yOffset.mm");
    }

    public double getDocumentNumberX() {
        return getDouble("invoice.field.documentNumber.x.mm");
    }

    public double getDocumentNumberY() {
        return getDouble("invoice.field.documentNumber.y.mm");
    }

    public double getDateX() {
        return getDouble("invoice.field.date.x.mm");
    }

    public double getDateY() {
        return getDouble("invoice.field.date.y.mm");
    }

    public double getSaleConditionX() {
        return getDouble("invoice.field.saleCondition.x.mm");
    }

    public double getSaleConditionY() {
        return getDouble("invoice.field.saleCondition.y.mm");
    }

    public double getCustomerNameX() {
        return getDouble("invoice.field.customerName.x.mm");
    }

    public double getCustomerNameY() {
        return getDouble("invoice.field.customerName.y.mm");
    }

    public double getCustomerRucX() {
        return getDouble("invoice.field.customerRuc.x.mm");
    }

    public double getCustomerRucY() {
        return getDouble("invoice.field.customerRuc.y.mm");
    }

    public double getAddressX() {
        return getDouble("invoice.field.address.x.mm");
    }

    public double getAddressY() {
        return getDouble("invoice.field.address.y.mm");
    }

    public double getPhoneX() {
        return getDouble("invoice.field.phone.x.mm");
    }

    public double getPhoneY() {
        return getDouble("invoice.field.phone.y.mm");
    }

    public double getDetailFirstY() {
        return getDouble("invoice.detail.first.y.mm");
    }

    public double getDetailLineStepMm() {
        return getDouble("invoice.detail.lineStep.mm");
    }

    public int getDetailMaxLines() {
        return getInt("invoice.detail.maxLines");
    }

    public double getQtyX() {
        return getDouble("invoice.detail.qty.x.mm");
    }

    public double getDescriptionX() {
        return getDouble("invoice.detail.description.x.mm");
    }

    public int getDescriptionMaxChars() {
        return getInt("invoice.detail.description.maxChars");
    }

    public int getDescriptionWrapChars() {
        return getInt("invoice.detail.description.wrapChars");
    }

    public double getUnitPriceX() {
        return getDouble("invoice.detail.unitPrice.x.mm");
    }

    public double getAmountExemptX() {
        return getDouble("invoice.detail.amountExempt.x.mm");
    }

    public double getAmountTax5X() {
        return getDouble("invoice.detail.amountTax5.x.mm");
    }

    public double getAmountTax10X() {
        return getDouble("invoice.detail.amountTax10.x.mm");
    }

    public double getTotalExemptX() {
        return getDouble("invoice.total.exempt.x.mm");
    }

    public double getTotalTax5X() {
        return getDouble("invoice.total.tax5.x.mm");
    }

    public double getTotalTax10X() {
        return getDouble("invoice.total.tax10.x.mm");
    }

    public double getTotalsY() {
        return getDouble("invoice.totals.y.mm");
    }

    public double getGrandTotalX() {
        return getDouble("invoice.grandTotal.x.mm");
    }

    public double getGrandTotalY() {
        return getDouble("invoice.grandTotal.y.mm");
    }

    public double getAmountInWordsX() {
        return getDouble("invoice.amountInWords.x.mm");
    }

    public double getAmountInWordsY() {
        return getDouble("invoice.amountInWords.y.mm");
    }

    public double getTaxIva5X() {
        return getDouble("invoice.tax.iva5.x.mm");
    }

    public double getTaxIva10X() {
        return getDouble("invoice.tax.iva10.x.mm");
    }

    public double getTaxTotalX() {
        return getDouble("invoice.tax.total.x.mm");
    }

    public double getTaxY() {
        return getDouble("invoice.tax.y.mm");
    }
}
