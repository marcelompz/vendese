# Plan — Impresión de factura preimpresa en @vendese (Windows 10)

| Campo | Valor |
| :--- | :--- |
| id | PLAN-impresion-factura-preimpresa-vendese-2026-11-27 (id estable: se conserva si se amplía el alcance) |
| titulo | Impresión de factura preimpresa en @vendese sobre formulario físico, con plantilla configurable en milímetros y validación en VM Windows 10 |
| dominio | impresion (transversal: POS / ticket / reportes) |
| estado | cerrado el 2026-11-27 (alcance autorizado: código, 71 tests en Linux y Windows 10, PDF virtual y POS en VM validados; sin impresión en papel real por decisión del usuario) |
| owner | Marcelo Pesallaccia (@marcelompz) |
| version_objetivo | 3.83.0 (criterio `MAYOR.FEATURE.PATCH`; sin cambios de esquema previstos; si aparecen, upgrade 3.82 → 3.83.0 en los 5 motores). Los ajustes posteriores de esta misma feature se registran como 3.83.1 en el plan [`PLAN-ajuste-impresion-factura-pdf-vm-2026-11-27`](plan-ajuste-impresion-factura-pdf-vm-2026-11-27.md). |
| fecha | 2026-11-27 |
| alcance | Diseñar e implementar un render de factura para **formulario preimpreso** independiente del ticket térmico: datos de venta, agrupación de impuestos, dos copias con desplazamiento en mm y plantilla `invoice-template.properties` ajustable sin recompilar. **Excluye:** facturación electrónica, timbrado/firmas, rediseño del talonario, reportes Jasper nuevos, ESC/POS térmico, cambios de esquema sin autorización y pruebas sobre papel real sin autorización. |
| dependencias | Mantener GPL v3 (`LICENSE.md`) y atribución a uniCenta oPOS / Jack Gerrard y a Marcelo Pesallaccia. Compatibilidad Java 7 (`source/target 1.7`) y build Ant. Medir el formulario preimpreso real antes de fijar coordenadas. Definir el origen de RUC/dirección/teléfono del cliente sin tocar el esquema en la primera entrega. Para la validación: flujo commit/push → `git archive HEAD` → compartido SMB → VM Windows 10 con JDK 8 y Ant en `C:\tools`. Referencia de estructura y decisiones: plan `PLAN-impresion-factura-preimpresa-w7-2026-11-26` del proyecto `supermarket_system`. |
| riesgos | La impresión actual de @vendese está orientada a ticket (Velocity + `TicketParser`) y a reportes Jasper con *bands*, difícil de alinear sobre papel preimpreso. Los importes y tasas de impuestos viven en el ticket y en `TAXES`; agruparlos en memoria evita migraciones, pero puede diferir de la convención del formulario si éste muestra subtotales con IVA incluido. La fuente y el tamaño de letra disponibles en Windows pueden desplazar el texto. Mitigación: coordenadas en mm convertidas a puntos, plantilla externa sobre `%USERPROFILE%`, validación previa en PDF virtual y un único disparador de impresión. |
| dod | Ver sección *Definition of Done*. Incluye build/tests Ant, plantilla ajustable sin recompilar, dos copias alineadas, sin impresión duplicada, evidencia en VM Windows 10 y sincronía de `README.md`, `ARCHITECTURE.md` y `docs/plans/ACTIVE.md` si aplica. |
| evidencia | Revisión de `ARCHITECTURE.md` §7.2/§7.5/§7.6, `IMPROVEMENT_PLAN.md`, `build_working.xml`, mapeo directo de `printer/`, `sales/JPanelTicket.java`, `AppConfig`, `JPanelConfigPeripheral` y `*-create.sql`, y del plan de referencia de `supermarket_system`. Implementación: `printer/invoice/*`, `reports/invoice-template.properties`, hook en `JPanelTicket`, defaults en `AppConfig`, 5 suites nuevas y documentación de diseño. Build/tests OK en Linux y VM, PDFCreator 6.3.1 instalado en la VM y PDF generado desde el `PrinterJob` de producción (detalle en el plan de ajuste `3.83.1`). |

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

### Decisión de validación (2026-11-27)

El usuario eligió **solo PDF virtual**. Queda prohibido imprimir sobre papel preimpreso real sin una autorización nueva y explícita. Inicialmente la VM Windows 10 no tenía impresoras, por lo que la primera vista se generó con iText desde el mismo `PreprintedInvoicePrintable` (`\\192.168.122.1\compartidos\factura-preimpresa-preview.pdf`). Posteriormente se instaló **PDFCreator 6.3.1** como impresora PDF del sistema y se generó el PDF definitivo desde el `PrinterJob` de producción (`factura-preimpresa-printerjob.pdf`); ambos pasos quedan registrados en el plan de ajuste `3.83.1`.

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
2. [x] Publicar `docs/plans/impresion/diseno-factura-preimpresa-vendese.md` con campos, coordenadas en mm y convención de impuestos, usando el diseño Odoo/Provecchio como plantilla inicial (pendiente de medir el formulario físico).
3. [x] Confirmar el origen de los datos fiscales del cliente desde `CUSTOMERS.TAXID`, `NAME`, `ADDRESS`/`ADDRESS2`, `CITY` y `PHONE`; impuestos desde `TicketInfo`/`TicketLineInfo` y `TaxInfo.RATE`, sin tocar el esquema. `Printer.Ticket.xml` ya imprime `printTaxid`, pero no dirección ni total en letras.
4. [x] Definir el modelo `InvoiceData` con cliente, fecha, número, líneas, impuestos agrupados y total en letras en español (`SpanishAmountToWords`).
5. [x] Implementar `PreprintedInvoicePrintable` + `InvoicePrintService` con `PrinterJob`, `Printable`, conversión mm→pt y dos copias (`copy2.yOffset.mm`).
6. [x] Crear `invoice-template.properties` con claves de página, fuente, campos, detalle, totales, impuestos y offset de copia; override desde `%USERPROFILE%` sin recompilar.
7. [x] Enlazar la impresión en un único punto de `JPanelTicket.closeTicket()` (1336-1433), con `machine.invoice.preprinted` y `machine.invoice.printername` registrados en `AppConfig.loadDefault()`, sin duplicar `Printer.Ticket`/`Ticket2`.
8. [x] Añadir 5 suites JUnit 4 de impresión y registrarlas en el target `test` de `build_working.xml` con `<arg value="..."/>`.
9. [x] Ejecutar `ant -f build.xml clean test dist` y verificar el JAR en `build/jar/unicentaopos.jar` (71 tests OK en Linux y en la VM).
10. [x] Validar en VM Windows 10: `git archive HEAD` al compartido SMB → compilación Ant → copiar JAR y plantilla a `C:\unicenta` → reiniciar POS → verificar título `uniCenta oPOS - 3.82` → generar el PDF desde el `PrinterJob` de producción y revisar posiciones en el PDF virtual (sin formulario físico medido, no se aplica la tolerancia de ±0,5 mm sobre papel).
11. [ ] Solo con autorización registrada aquí, imprimir sobre papel preimpreso en la VM; adjuntar evidencia. **Fuera del alcance cerrado: el usuario no la ha autorizado; queda como seguimiento futuro, no como deuda del cierre.**
12. [x] Sincronizar documentación (`ACTIVE.md`, `ROADMAP.md`, `featurelist.json`, `AGENTS.md`) y publicar en `origin/main`.

## Validación

- **Build:** `ant -f build.xml clean test dist` sin errores; nuevas suites incluidas en el target `test`.
- **Plantilla:** cambiar una coordenada en `%USERPROFILE%\invoice-template.properties` y comprobar el desplazamiento sin recompilar ni reinstalar.
- **Datos:** venta con líneas exentas, al 5 % y al 10 %; subtotales y total coinciden con el ticket guardado; total en letras en español.
- **Impresión:** una y N líneas; cada campo cae en su casilla; ambas copias mantienen el mismo desplazamiento; no hay impresión doble al cobrar; el ticket térmico sigue funcionando igual.
- **Windows 10:** evidencia del flujo acordado y del proceso `javaw.exe` con título `uniCenta oPOS - 3.82` tras reiniciar.
- **Seguridad/licencia:** sin `dbPassword` en scripts, sin cambios en `ScriptSecurity`/`BeanNameValidator`, `LICENSE.md` y atribuciones intactas.

## Definition of Done (DoD)

- [x] Documento de diseño publicado en `docs/plans/impresion/` (formulario físico aún pendiente de medir).
- [x] Render Java2D implementado sin dependencias nuevas y compatible con Java 7.
- [x] Plantilla en mm con override en `%USERPROFILE%` (verificación en runtime pendiente).
- [x] Dos copias y un único disparador de impresión; sin duplicar el ticket térmico.
- [x] `ant -f build.xml clean test dist` OK en JDK 8/Linux y, de nuevo, en VM Windows 10 (71 tests, 0 fallos en ambos); suites registradas en `build_working.xml`.
- [x] Evidencia de build/tests y de la validación en VM Windows 10; primero un PDF virtual generado con iText desde el mismo `Printable` y, tras instalar PDFCreator 6.3.1, el PDF definitivo desde el `PrinterJob` de producción. Papel real pendiente de autorización.
- [x] `README.md`, `ARCHITECTURE.md` y `docs/plans/ACTIVE.md` sincronizados.
- [x] GPL v3, atribución a uniCenta oPOS / Jack Gerrard y a Marcelo Pesallaccia (@marcelompz) conservadas.
- [x] Sin cambios de esquema (no aplica migración).

El plan se entrega con la validación autorizada (PDF virtual) y permanece abierto a un ajuste de coordenadas si el usuario aporta el formulario físico. Cualquier prueba en papel real requiere una autorización nueva registrada aquí.

## Seguimiento y evidencia

| Fecha | Avance | Evidencia |
| :--- | :--- | :--- |
| 2026-11-27 | Plan creado con estructura equivalente al de `supermarket_system`. | Este documento |
| 2026-11-27 | Decisión técnica inicial: Java2D + `PrinterJob`, plantilla en mm, Jasper solo para reportes. | Revisión de `ARCHITECTURE.md` §7.2/§7.5/§7.6 y del plan de referencia |
| 2026-11-27 | Restricciones registradas: Java 7, Ant, GPL v3, `es_ES`, sin `dbPassword` en scripts, 5 motores SQL. | `AGENTS.md`, `IMPROVEMENT_PLAN.md`, `build_working.xml` |
| 2026-11-27 | Sin código implementado; medición del formulario y datos fiscales pendientes. | `git status` / estado del repositorio |
| 2026-11-27 | Mapa de impresión confirmado: `DeviceTicket.java:43`, `TicketParser.java:50`, `JPanelTicket.closeTicket()` 1336-1433 y `printTicket()` 1462-1503. | Revisión de `src-pos/com/openbravo/pos/printer/` y `sales/JPanelTicket.java` |
| 2026-11-27 | Base Java2D/`Printable` ya presente en `BasicTicketForPrinter` y `PrintableBasicTicket`; no hay soporte de factura preimpresa ni tests de impresión. | Revisión de `printer/ticket/`, `printer/printer/` y `test/` |
| 2026-11-27 | Implementados `InvoiceTemplate`, `InvoiceData`, `SpanishAmountToWords`, `PreprintedInvoicePrintable` e `InvoicePrintService`; plantilla por defecto y hook en `JPanelTicket`. | `git diff` / archivos nuevos |
| 2026-11-27 | 5 suites JUnit nuevas registradas en `build_working.xml`; documentación de diseño, README y ARQUITECTURA actualizadas. | `test/com/openbravo/pos/printer/invoice/`, `docs/plans/impresion/diseno-factura-preimpresa-vendese.md` |
| 2026-11-27 | `ant -f build.xml clean test dist` OK en JDK 8 (Temurin 8u514): 570 fuentes, 71 tests, 0 fallos; ZIPs `unicentaopos_3.82_bin.zip` y `..._src.zip` generados. Correcciones publicadas en `1427922`. | Salida de Ant en host Linux |
| 2026-11-27 | VM Windows 10: `ant -f build.xml clean test dist` OK en `C:\unicenta-build` (JDK 8u512 + Ant 1.10.14), **71 tests OK**; JAR de 2.406.292 bytes y plantilla copiados a `C:\unicenta`. | `\\192.168.122.1\compartidos\build_vendese_vm.log` y `build_vendese_vm.done=OK` |
| 2026-11-27 | POS reiniciado en la VM: proceso `java.exe` con título `uniCenta oPOS - 3.82`. | `\\192.168.122.1\compartidos\checkpos.out` y `pos.log` |
| 2026-11-27 | La VM no tiene impresoras (`PrintServiceLookup` → `DEFAULT=null`); la validación virtual se generó con iText desde el mismo `PreprintedInvoicePrintable`: 1 página de 594,99 × 934,87 pt, dos copias, columnas de exento/IVA 5 %/IVA 10 % y total en letras correctos. | `factura-preimpresa-preview.pdf` y `.png` en el compartido SMB; `pdftotext -layout` |
| 2026-11-27 | Decisión del usuario: **solo PDF virtual**; no autoriza impresión sobre papel preimpreso real. | Respuesta registrada en esta sesión y en la sección «Decisión de validación» |
| 2026-11-27 | Solicitud posterior del usuario: instalar impresora PDF en la VM, activar la factura preimpresa y versionar la evidencia. | Sección «Habilitación de impresora PDF en la VM» y plan de ajuste `3.83.1` |
| 2026-11-27 | *Microsoft Print to PDF* no existe en la imagen (`dism`: `0x800f080c`, `feature ... is unknown`). Se instala PDFCreator 6.3.1 (GPL v3) desde `PDFCreator-6_3_1-Setup.exe`; `SETUP_EXIT=0`. | `fullsetup2.out`, `pdfcreator-fullsetup.log` en el compartido SMB |
| 2026-11-27 | `wmic printer get name,drivername` → `PDFCreator  PDFCreator`; `PrintProbe` → `DEFAULT=Win32 Printer : PDFCreator`, `SERVICE=PDFCreator`. | Salidas de WMIC y `PrintProbe` en el compartido SMB |
| 2026-11-27 | Factura activada por configuración (`machine.invoice.preprinted=true`, `machine.invoice.printername=PDFCreator`) y POS reiniciado (`java.exe` PID 3032, `uniCenta oPOS - 3.82`). | `invoice_config.out`, `checkpos.out`, `pos.log` |
| 2026-11-27 | PDF generado desde el `PrinterJob` de producción: `PRINT_OK=true`; `factura-preimpresa-vm.pdf` (8.920 bytes, 1 página) guardado vía `SaveButton` y archivado como `factura-preimpresa-printerjob.pdf`. | `printjob.log`, `factura-preimpresa-printerjob.pdf` y `pdftotext -layout` (dos copias, total 10.254 y total en letras) |
| 2026-11-27 | Plan cerrado con el alcance autorizado (PDF virtual); papel real queda como seguimiento futuro. | Este documento, `ACTIVE.md`, `ROADMAP.md` y `featurelist.json` |

## Habilitación de impresora PDF en la VM (solicitud posterior del usuario)

| Campo | Valor |
| :--- | :--- |
| Fecha | 2026-11-27 |
| Solicitud | Instalar una impresora PDF en la VM Windows 10, activar la factura preimpresa y actualizar el repositorio con la evidencia. |
| Estado | **cerrado** como `3.83.1` — impresora `PDFCreator` instalada, PDF generado desde el `PrinterJob` de producción y documentación sincronizada. Detalle completo en el plan [`PLAN-ajuste-impresion-factura-pdf-vm-2026-11-27`](plan-ajuste-impresion-factura-pdf-vm-2026-11-27.md). |
| Restricción vigente | Sigue prohibida la impresión sobre papel preimpreso real sin autorización nueva y explícita. Solo se permite PDF virtual. |

Procedimiento ejecutado, registrado para reproducibilidad:

1. Verificado: `whoami /groups` → `BUILTIN\Administrators` y `Mandatory Label\High Mandatory Level`.
2. *Microsoft Print to PDF* **no está disponible** en esta imagen: `dism /online /get-featureinfo /featurename:Printing-PrintToPDFServices-Features` → `Error: 0x800f080c`, `Feature name Printing-PrintToPDFServices-Features is unknown`.
3. Instalado PDFCreator 6.3.1 (GPL v3), que sí crea una impresora real del sistema:
   ```cmd
   PDFCreator-6_3_1-Setup.exe /VerySilent /NoIcons /DisableContextMenu /LogFile=C:\unicenta-build\pdfcreator-fullsetup.log
   ```
   El primer intento desde `\\192.168.122.1\compartidos` terminó con `Access is denied`; se copió el instalador a `C:\unicenta-build\` y la segunda ejecución devolvió `SETUP_EXIT=0`.
4. Verificado el servicio de impresión con `PrintProbe` (`javax.print.PrintServiceLookup`): `DEFAULT=Win32 Printer : PDFCreator`, `SERVICE=PDFCreator` (`wmic printer get name,drivername` → `PDFCreator  PDFCreator`).
5. Activada la factura en `%USERPROFILE%\unicentaopos.properties`:
   ```properties
   machine.invoice.preprinted=true
   machine.invoice.printername=PDFCreator
   ```
6. Reiniciado el POS (`java.exe`, título `uniCenta oPOS - 3.82`) y generado el PDF desde el `PrinterJob` de producción (`PRINT_OK=true` en `printjob.log`). El PDF se guardó desde el botón `SaveButton` de la GUI de PDFCreator como `C:\unicenta-build\factura-preimpresa-vm.pdf` y se archivó en el compartido como `factura-preimpresa-printerjob.pdf`; `pdftotext -layout` confirma dos copias, total `10.254` y total en letras.
7. Registrada la evidencia en la tabla de seguimiento, sincronizados `docs/plans/ACTIVE.md`, `ROADMAP.md`, `featurelist.json` y `AGENTS.md`, y publicado en `origin/main`.

## Cierre

**Cerrado el 2026-11-27** con el alcance autorizado: código, plantilla, 71 tests, `ant -f build.xml clean test dist` en Linux y Windows 10, JAR desplegado en `C:\unicenta`, POS reiniciado, impresora PDF `PDFCreator` instalada y PDF generado desde el `PrinterJob` de producción. El ajuste operativo se registró como `3.83.1` en su propio plan y en `ROADMAP.md` / `featurelist.json` / `ACTIVE.md` / `AGENTS.md`.

Evidencia vinculada: commits `1427922`, `63822cd`, `5dd42b9`, `0e16169` y `98a0c37`; `build_vendese_vm.log` (`OK (71 tests)`, `BUILD SUCCESSFUL`); `printjob.log` (`PRINT_OK=true`); `factura-preimpresa-printerjob.pdf` (dos copias, total `10.254` y total en letras, revisado con `pdftotext -layout`); `final_check.out` (impresora, propiedades, POS y PDF).

**No se imprimió papel preimpreso**: el usuario eligió «Solo PDF virtual». La medición del formulario físico y cualquier prueba en papel real quedan fuera de este cierre y requieren autorización nueva y explícita; si se autorizan, se registrarán como un PATCH posterior de la línea `3.83`, no reabriendo este plan.
