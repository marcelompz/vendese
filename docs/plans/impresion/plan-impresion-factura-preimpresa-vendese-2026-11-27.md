# Plan — Impresión de factura preimpresa en @vendese (Windows 10)

| Campo | Valor |
| :--- | :--- |
| id | PLAN-impresion-factura-preimpresa-vendese-2026-11-27 (id estable: se conserva si se amplía el alcance) |
| titulo | Impresión de factura preimpresa en @vendese sobre formulario físico, con plantilla configurable en milímetros y validación en VM Windows 10 |
| dominio | impresion (transversal: POS / ticket / reportes) |
| estado | borrador (decisión técnica inicial documentada; pendiente de aprobación y de autorización para imprimir en la VM) |
| owner | Marcelo Pesallaccia (@marcelompz) |
| version_objetivo | 3.83 (sin cambios de esquema previstos; si aparecen, upgrade 3.82 → 3.83 en los 5 motores) |
| fecha | 2026-11-27 |
| alcance | Diseñar e implementar un render de factura para **formulario preimpreso** independiente del ticket térmico: datos de venta, agrupación de impuestos, dos copias con desplazamiento en mm y plantilla `invoice-template.properties` ajustable sin recompilar. **Excluye:** facturación electrónica, timbrado/firmas, rediseño del talonario, reportes Jasper nuevos, ESC/POS térmico, cambios de esquema sin autorización y pruebas sobre papel real sin autorización. |
| dependencias | Mantener GPL v3 (`LICENSE.md`) y atribución a uniCenta oPOS / Jack Gerrard y a Marcelo Pesallaccia. Compatibilidad Java 7 (`source/target 1.7`) y build Ant. Medir el formulario preimpreso real antes de fijar coordenadas. Definir el origen de RUC/dirección/teléfono del cliente sin tocar el esquema en la primera entrega. Para la validación: flujo commit/push → `git archive HEAD` → compartido SMB → VM Windows 10 con JDK 8 y Ant en `C:\tools`. Referencia de estructura y decisiones: plan `PLAN-impresion-factura-preimpresa-w7-2026-11-26` del proyecto `supermarket_system`. |
| riesgos | La impresión actual de @vendese está orientada a ticket (Velocity + `TicketParser`) y a reportes Jasper con *bands*, difícil de alinear sobre papel preimpreso. Los importes y tasas de impuestos viven en el ticket y en `TAXES`; agruparlos en memoria evita migraciones, pero puede diferir de la convención del formulario si éste muestra subtotales con IVA incluido. La fuente y el tamaño de letra disponibles en Windows pueden desplazar el texto. Mitigación: coordenadas en mm convertidas a puntos, plantilla externa sobre `%USERPROFILE%`, validación previa en PDF virtual y un único disparador de impresión. |
| dod | Ver sección *Definition of Done*. Incluye build/tests Ant, plantilla ajustable sin recompilar, dos copias alineadas, sin impresión duplicada, evidencia en VM Windows 10 y sincronía de `README.md`, `ARCHITECTURE.md` y `docs/plans/ACTIVE.md` si aplica. |
| evidencia | Revisión de `ARCHITECTURE.md` §7.2/§7.5/§7.6, `IMPROVEMENT_PLAN.md`, `build_working.xml` (target `test` con 11 suites), mapeo directo de `printer/`, `sales/JPanelTicket.java`, `AppConfig`, `JPanelConfigPeripheral` y `*-create.sql`, y del plan de referencia de `supermarket_system`. Sin código implementado ni pruebas en VM para este plan. |

## Objetivo

Que @vendese pueda imprimir una **factura sobre un formulario preimpreso** usando los datos de la venta ya existentes (cliente, líneas, impuestos, pagos y total), con posiciones definidas en milímetros y ajustables sin recompilar. La salida debe ser predecible en Windows 10 y no debe sustituir ni duplicar el ticket térmico habitual.

## Contexto y evidencia

- @vendese es un fork de uniCenta oPOS con licencia GPL v3, Java 7 (`source/target 1.7`), build Apache Ant, idioma por defecto `es_ES` y paquetes internos `com.openbravo.*` / `unicentaopos` (ver `AGENTS.md` y `README.md`).
- El flujo de cobro termina en `JPanelTicket.closeTicket()`: `ticket.save` → `DataLogicSales.saveTicket()` → `ticket.close` → impresión de `Printer.Ticket`/`Ticket2` mediante Velocity + `TicketParser` (`ARCHITECTURE.md`, §7.2).
- La fachada de impresión es `DeviceTicket` (`src-pos/com/openbravo/pos/printer/DeviceTicket.java:43`; `machine.printer[.2..6]` en 147-214); `TicketParser` (`TicketParser.java:50`) interpreta el DTD `ticket.dtd` (`<ticket>`, `<line>`, `<text>`, `<image>`, `<barcode>`, `<display>`, `<opendrawer/>`, comandos fiscales). Destinos: `DevicePrinterPrinter` (impresora del SO vía `javax.print`), ESC/POS, JavaPOS, pantalla, fiscal y archivo/serie (`ARCHITECTURE.md`, §7.5).
- Ya existe una base Java2D/`Printable` reutilizable: `printer/ticket/BasicTicketForPrinter.java:31` (fuente `Monospaced` 7 pt, factor Y 1,40) y `printer/printer/PrintableBasicTicket.java:60-104` (paginación y `g2d.translate(..., imageable_x, imageable_y)`). Es el punto natural para el render en mm, sin añadir dependencias.
- La impresión final ocurre en `JPanelTicket.closeTicket()` (`src-pos/com/openbravo/pos/sales/JPanelTicket.java:1336-1433`): `ticket.save` → `dlSales.saveTicket()` (1382) → evento `ticket.close` con flag `print` (1388) → `Printer.Ticket`/`Ticket2` (1392-1395). `printTicket()` (1462-1503) evalúa Velocity e inyecta `taxes`, `taxeslogic`, `ticket`, `place`, `warranty` y `pickupid` antes de `m_TTP.printTicket(...)`.
- `Printer.Ticket.xml` incluye cliente/NIF (`printTaxid`, líneas 51-55) pero no dirección ni total en letras. Existen `Printer.Ticket_A4.txt` y `Printer.TicketPreview_A4.txt` con cabecera "Tax Invoice" y columnas de 105 caracteres, pero **no están sembrados** en `RESOURCES` de los `*-create.sql`; no son usables sin importarlos.
- JasperReports se usa para reportes (`JPanelReport`, `PanelReportBean`, `.bs`, `.jrxml`, `JasperReportLoader`), no para imprimir sobre un formulario ya impreso: los *bands* dificultan el control milimétrico (`ARCHITECTURE.md`, §7.6).
- Las plantillas Velocity de ticket (`Printer.Ticket.xml`, `TicketNew`, `TicketKitchen`, `CloseCash`, `PartialCash`, etc.) viven como recursos versionados; los scripts BeanShell de la BD están sujetos a `ScriptSecurity` y `BeanNameValidator` (Fase 3). Este plan **no** introduce BeanShell nuevo ni entrega `dbPassword` a scripts.
- No existe hoy un render dedicado a "factura preimpresa" con coordenadas absolutas en mm y configuración por modelo de papel.
- El plan de referencia de `supermarket_system` resolvió un problema equivalente con `java.awt.print.PrinterJob` + `Graphics2D` (`Printable`), conversión `mm * 72 / 25.4` y una plantilla de propiedades. Esa decisión se adopta aquí, adaptada a la arquitectura de @vendese.
- La VM Windows 10 ya tiene JDK 8 y Ant en `C:\tools`, el POS se ejecuta con `javaw.exe` y título `uniCenta oPOS - 3.82`, y el flujo de prueba por SMB está acordado.

## Decisión técnica de impresión

| Opción | Veredicto | Motivo |
| :--- | :--- | :--- |
| Java2D + `PrinterJob` (`Printable`, `Graphics2D`) | ✅ Adoptado | Control milimétrico exacto, sin dependencias nuevas (JDK), compatible con Java 7, imprime directamente sobre el formulario preimpreso. |
| JasperReports | ⚠️ No para este fin | Ya está en el proyecto y se conserva para reportes; sus *bands* no son adecuados para alinear sobre papel preimpreso. |
| Plantillas Velocity / BeanShell del ticket | ❌ Descartado | Orientadas a ticket térmico y a eventos; ampliarlas añadiría superficie de scripting y no resuelve la alineación en mm. |
| ESC/POS | ❌ Descartado | Pensado para ticket térmico, no para formularios de hoja completa. |

Se adopta `java.awt.print.PrinterJob` + `Printable`, dibujando con `Graphics2D` y convirtiendo cada coordenada con `mm * 72 / 25.4`. El render será un servicio nuevo, separado de `DeviceTicket` y de la cadena Velocity → `TicketParser`, pero reutilizando la base `javax.print`/`Printable` ya presente en `DevicePrinterPrinter` y `PrintableBasicTicket`, para no alterar la impresión de ticket existente.

## Alcance / Fuera de alcance

Incluye: modelo de datos de factura desde el ticket, agrupación de impuestos, render Java2D, dos copias con desplazamiento configurable, `invoice-template.properties` con override en `%USERPROFILE%`, textos en español, tests unitarios y validación en VM Windows 10 (primero PDF virtual).

Excluye: facturación electrónica, envío a organismos fiscales, timbrado/firmas, generación de un nuevo talonario, reportes Jasper nuevos, movimiento de importes a `BigDecimal`, cambios de esquema sin autorización e impresión en papel real sin autorización explícita.

## Archivos / módulos afectados (estimados)

- `src-pos/com/openbravo/pos/printer/`: nuevo servicio de impresión y `Printable` de factura preimpresa, reutilizando `DevicePrinterPrinter`/`PrintableBasicTicket` y sin tocar `TicketParser`.
- `src-pos/com/openbravo/pos/sales/JPanelTicket.java:1336-1433`: enganche único tras `ticket.close` y antes/a la par de `Printer.Ticket`/`Ticket2`, controlado por configuración.
- `src-pos/com/openbravo/pos/ticket/` (`TicketInfo`, `TicketLineInfo`, `TicketTaxInfo`, `TaxInfo`) y `src-pos/com/openbravo/pos/sales/TaxesLogic.java:117-121`: adaptadores al modelo de factura, junto con `CustomerInfo` (`src-pos/com/openbravo/pos/customers/CustomerInfo.java:188-198`) y `PaymentInfo`.
- `src-pos/com/openbravo/pos/forms/AppConfig.java:257-300` (`loadDefault`) y `src-pos/com/openbravo/pos/config/JPanelConfigPeripheral.java:449-605,667-822`: nueva propiedad `machine.invoice.preprinted` si se expone en la configuración; hoy solo existen `machine.printer*`, `paper.receipt.*` y `paper.standard.*`, sin claves `invoice.*`.
- `reports/invoice-template.properties`: plantilla por modelo de papel; override opcional en `%USERPROFILE%\invoice-template.properties`.
- `locales/*_es.properties` y `reports/com/openbravo/reports/*_es.properties`: etiquetas nuevas, si las hubiera (idioma por defecto `es_ES`).
- `test/`: nuevas suites JUnit 4 sobre conversión mm→pt, agrupación de impuestos, plantilla y offset de copias. Hoy **no hay tests** de `TicketParser`, `DeviceTicket` ni impresión.
- `build_working.xml`: registrar las nuevas suites como `<arg value="..."/>` en el target `test` (líneas 486-496; gotcha conocido: crear `*Test.java` no basta).
- `docs/plans/impresion/plan-impresion-factura-preimpresa-vendese-2026-11-27.md` y `docs/plans/ACTIVE.md`.
- `README.md` / `ARCHITECTURE.md`: solo si se añade una propiedad de configuración o cambia la arquitectura de impresión.

**No se prevén cambios SQL.** Si finalmente se persiste moneda, tasa, RUC o columnas de impuestos por línea, aplicar el cambio en los 5 `*-create.sql` y en `*-upgrade-3.82.sql`.

## Pasos de implementación

1. [x] Crear este plan y registrar la decisión técnica inicial.
2. [ ] Medir el formulario preimpreso real (PDF de referencia y papel) y publicar `docs/plans/impresion/diseno-factura-preimpresa-vendese.md` con campos, coordenadas en mm y convención de impuestos.
3. [ ] Confirmar el origen de los datos fiscales del cliente: `CUSTOMERS.TAXID` (NIF/RUC), `NAME`, `ADDRESS`/`ADDRESS2`, `CITY`, `PHONE` (`Derby-create.sql:147-178`), con impuestos desde `TAXES.RATE` (`203-219`) y `TAXLINES.BASE/AMOUNT` (`468-477`), líneas desde `TICKETLINES.TAXID` (`428-442`) y pagos desde `PAYMENTS` (`453-466`), sin tocar el esquema en la primera entrega. `Printer.Ticket.xml` ya imprime `printTaxid`, pero no dirección ni total en letras.
4. [ ] Definir el modelo `PreprintedInvoiceData` a partir del ticket ya cerrado: cliente, cajero, fecha, número de ticket/factura, líneas, impuestos agrupados, pagos y total en letras en español.
5. [ ] Implementar `PreprintedInvoicePrintable` + `InvoicePrintService` con `PrinterJob`, `Printable`, conversión mm→pt y dos copias (`copy2.yOffset.mm`).
6. [ ] Crear `invoice-template.properties` con claves de página, fuente, campos, detalle, totales, impuestos y offset de copia; permitir override desde `%USERPROFILE%` sin recompilar.
7. [ ] Enlazar la impresión en un único punto de `JPanelTicket.closeTicket()` (1336-1433), con una propiedad explícita (p. ej. `machine.invoice.preprinted=true`, registrada en `AppConfig.loadDefault()` y, si se expone, en `JPanelConfigPeripheral`) para no duplicar `Printer.Ticket`/`Ticket2`.
8. [ ] Añadir las primeras suites JUnit 4 de impresión (no existen hoy) y registrarlas en el target `test` de `build_working.xml` con `<arg value="..."/>`.
9. [ ] Ejecutar `ant -f build.xml clean test dist` y verificar el JAR en `build/jar/unicentaopos.jar`.
10. [ ] Validar en VM Windows 10: commit/push → `git archive HEAD` al compartido SMB → compilación Ant → copiar JAR → reiniciar POS → verificar título `uniCenta oPOS - 3.82` → imprimir primero en PDF virtual y comparar posiciones (±0,5 mm).
11. [ ] Solo con autorización registrada aquí, imprimir sobre papel preimpreso en la VM; adjuntar evidencia.
12. [ ] Sincronizar documentación y cerrar el plan cuando se cumpla el DoD.

## Validación

- **Build:** `ant -f build.xml clean test dist` sin errores; nuevas suites incluidas en el target `test`.
- **Plantilla:** cambiar una coordenada en `%USERPROFILE%\invoice-template.properties` y comprobar el desplazamiento sin recompilar ni reinstalar.
- **Datos:** venta con líneas exentas, al 5 % y al 10 %; subtotales y total coinciden con el ticket guardado; total en letras en español.
- **Impresión:** una y N líneas; cada campo cae en su casilla; ambas copias mantienen el mismo desplazamiento; no hay impresión doble al cobrar; el ticket térmico sigue funcionando igual.
- **Windows 10:** evidencia del flujo acordado y del proceso `javaw.exe` con título `uniCenta oPOS - 3.82` tras reiniciar.
- **Seguridad/licencia:** sin `dbPassword` en scripts, sin cambios en `ScriptSecurity`/`BeanNameValidator`, `LICENSE.md` y atribuciones intactas.

## Definition of Done (DoD)

- [ ] Formulario real medido y documento de diseño publicado en `docs/plans/impresion/`.
- [ ] Render Java2D implementado sin dependencias nuevas y compatible con Java 7.
- [ ] Plantilla en mm con override en `%USERPROFILE%` verificada sin recompilar.
- [ ] Dos copias alineadas y un único disparador de impresión; sin duplicados.
- [ ] `ant -f build.xml clean test dist` OK y nuevas suites registradas en `build_working.xml`.
- [ ] Evidencia de build/tests y de la validación en VM Windows 10 (PDF virtual; papel real solo con autorización).
- [ ] `README.md`, `ARCHITECTURE.md` y `docs/plans/ACTIVE.md` sincronizados si aplica.
- [ ] GPL v3, atribución a uniCenta oPOS / Jack Gerrard y a Marcelo Pesallaccia (@marcelompz) conservadas.
- [ ] Si hubo cambios de esquema: aplicados en los 5 motores y en `*-upgrade-3.82.sql`, con DDL validado.

El plan permanece en `borrador` hasta que se apruebe y se autorice la prueba en papel; el paso a `propuesto`/`aprobado` requiere decisión explícita registrada abajo.

## Seguimiento y evidencia

| Fecha | Avance | Evidencia |
| :--- | :--- | :--- |
| 2026-11-27 | Plan creado con estructura equivalente al de `supermarket_system`. | Este documento |
| 2026-11-27 | Decisión técnica inicial: Java2D + `PrinterJob`, plantilla en mm, Jasper solo para reportes. | Revisión de `ARCHITECTURE.md` §7.2/§7.5/§7.6 y del plan de referencia |
| 2026-11-27 | Restricciones registradas: Java 7, Ant, GPL v3, `es_ES`, sin `dbPassword` en scripts, 5 motores SQL. | `AGENTS.md`, `IMPROVEMENT_PLAN.md`, `build_working.xml` |
| 2026-11-27 | Sin código implementado; medición del formulario y datos fiscales pendientes. | `git status` / estado del repositorio |
| 2026-11-27 | Mapa de impresión confirmado: `DeviceTicket.java:43`, `TicketParser.java:50`, `JPanelTicket.closeTicket()` 1336-1433 y `printTicket()` 1462-1503. | Revisión de `src-pos/com/openbravo/pos/printer/` y `sales/JPanelTicket.java` |
| 2026-11-27 | Base Java2D/`Printable` ya presente en `BasicTicketForPrinter` y `PrintableBasicTicket`; no hay soporte de factura preimpresa ni tests de impresión. | Revisión de `printer/ticket/`, `printer/printer/` y `test/` |

## Cierre

Pendiente. Requiere aprobación del plan, documento de diseño con el formulario real, build/tests OK, evidencia en VM Windows 10 y sincronía documental. Todo avance debe registrarse aquí y en `docs/plans/ACTIVE.md`; los informes de validación se adjuntarán a este mismo plan.
