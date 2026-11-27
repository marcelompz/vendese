# Diseño de factura preimpresa — @vendese

| Campo | Valor |
| :--- | :--- |
| id | DOC-impresion-diseno-factura-preimpresa-vendese-2026-11-27 |
| estado | implementado en plantilla por defecto; **pendiente de validar contra el formulario físico** |
| fecha | 2026-11-27 |
| insumo | Plan `PLAN-impresion-factura-preimpresa-vendese-2026-11-27` y diseño de referencia Odoo/Provecchio de `supermarket_system` |
| precedencia | El formulario preimpreso real manda para posiciones; la plantilla `reports/invoice-template.properties` es el único lugar a ajustar. |

## 1. Origen de coordenadas

Se adopta la geometría medida del PDF de Odoo/Provecchio como plantilla inicial:

- Página: **209,9 × 329,8 mm** (595 × 935 pt).
- Dos bloques: original e identico desplazado **+161,6 mm** en Y.
- Origen: esquina superior izquierda; `x` a la derecha, `y` hacia abajo.
- Conversión: `pt = mm * 72 / 25.4`.
- Fuente: `DejaVuSans`; cuerpo 6,8 pt y total general 12 pt.

Estas medidas son un punto de partida. El número de factura y los encabezados de columnas se suponen impresos en el papel (`invoice.field.documentNumber.y.mm=-1` los desactiva por defecto).

## 2. Campos y origen en @vendese

| Campo | Origen en el ticket / BD | Coordenada por defecto (mm) |
| :--- | :--- | :--- |
| Fecha | `TicketInfo.getDate()` formateado `dd 'de' MMMM 'del' yyyy` (es_ES) | 42,9 / 51,8 |
| Condición de venta | `CONTADO (X)` (constante del POS) | 184,2 / 51,8 |
| Cliente | `CUSTOMERS.NAME` vía `CustomerInfoExt.getName()` | 51,8 / 55,9 |
| R.U.C. | `CUSTOMERS.TAXID` vía `getTaxid()` | 164,7 / 55,6 |
| Dirección | `ADDRESS`, `ADDRESS2` y `CITY` unidos con `, ` | 18,3 / 60,1 |
| Teléfono | `CUSTOMERS.PHONE` | 155,8 / 59,6 |
| Número de factura | `TicketInfo.printId()` (desactivado por defecto) | -1 |

## 3. Detalle e impuestos

Cada línea usa `TicketLineInfo.getValue()` (importe **con IVA**) para que coincida con la convención del formulario de referencia. `TicketLineInfo.getPrice()` se usa como precio unitario y `getMultiply()` como cantidad.

| Columna | Regla | Coordenada x (mm) |
| :--- | :--- | :---: |
| Cantidad | `0.0` | 21,9 |
| Descripción | recortada antes de ` (` y a 80 caracteres; +1 paso si supera 72 | 33,2 |
| Precio unitario | `###.##0` | 125,0 |
| Exento | `rate == 0` | 148,3 |
| IVA 5 % | `0 < rate <= 0,075` | 180,3 |
| IVA 10 % | `rate > 0,075` | 195,4 |

Primera línea en **y = 73,4 mm**, paso **3,25 mm**, máximo 20 líneas. Los subtotales se imprimen en **y = 131,6 mm**; el total general en **187,1 / 136,8 mm** con fuente de 12 pt.

Los importes de IVA son `getValue() - getSubValue()` agrupados por columna y se imprimen en **y = 147,1 mm**. El total en letras usa `SpanishAmountToWords` sobre la suma de las columnas, sin decimales y en español.

> Nota: el formulario de referencia admite solo tres alícuotas. Cualquier impuesto distinto de 0 % se agrupa en 5 % o 10 % por cercanía; si el talonario real usa otros grupos, hay que ampliar la plantilla antes de usarlo.

## 4. Plantilla configurable

Archivo versionado: `reports/invoice-template.properties`. El usuario puede copiarlo a:

```text
%USERPROFILE%\invoice-template.properties
```

y cambiar cualquier clave sin recompilar. El override se carga después del archivo del classpath, por lo que solo sobrescribe las claves presentes.

Claves principales: `invoice.page.*`, `invoice.font.*`, `invoice.copy2.yOffset.mm`, `invoice.field.*`, `invoice.detail.*`, `invoice.total.*`, `invoice.grandTotal.*`, `invoice.amountInWords.*`, `invoice.tax.*`.

## 5. Activación

Añadir a `unicentaopos.properties`:

```properties
machine.invoice.preprinted=true
machine.invoice.printername=(Default)
```

Con `false` (valor por defecto) el POS sigue imprimiendo solo `Printer.Ticket`/`Ticket2`. Para validar en PDF virtual, seleccionar `PDFCreator` en `machine.invoice.printername` (verificado en la VM Windows 10; *Microsoft Print to PDF* no está disponible en esa imagen).

## 6. Validación pendiente

1. Comparar la salida en PDF virtual contra el formulario real con tolerancia de ±0,5 mm.
2. Confirmar fuente disponible en Windows 10; si `DejaVuSans` no existe, ajustar `invoice.font.name`.
3. Verificar si el talonario trae número de factura impreso; si no, configurar `invoice.field.documentNumber.y.mm`.
4. Comprobar venta con 1 línea, 20 líneas y líneas largas (salto extra).
5. Imprimir sobre papel real solo con autorización registrada en el plan.
