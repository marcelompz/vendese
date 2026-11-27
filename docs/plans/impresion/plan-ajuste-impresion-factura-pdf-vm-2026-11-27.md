# PLAN-ajuste-impresion-factura-pdf-vm-2026-11-27 — Ajustes a la factura preimpresa en VM (3.83.1)

- **ID:** `PLAN-ajuste-impresion-factura-pdf-vm-2026-11-27`
- **Dominio:** `impresion` (VM / configuración)
- **Estado:** `entregado`
- **Versión objetivo:** `3.83.1` (PATCH de `3.83.0`, sin nueva línea de feature)
- **Fecha:** `2026-11-27`
- **Plan padre:** [`PLAN-impresion-factura-preimpresa-vendese-2026-11-27`](plan-impresion-factura-preimpresa-vendese-2026-11-27.md)
- **Registro:** [`ROADMAP.md`](../../../ROADMAP.md) · [`featurelist.json`](../../../featurelist.json) · [`ACTIVE.md`](../ACTIVE.md)

## 1. Objetivo y alcance

Completar la entrega de `3.83.0` con los ajustes de operación en la VM Windows 10: disponer de una impresora PDF real del sistema, activar la factura preimpresa mediante configuración y capturar evidencia generada por el propio `PrinterJob` de producción. Solo se modificarían coordenadas de la plantilla si se aporta y mide un formulario físico; no fue el caso.

**Incluye:** instalación de *PDFCreator 6.3.1* (impresora PDF del sistema, GPL v3) porque *Microsoft Print to PDF* no existe en esta imagen de Windows 10, verificación con `PrintServiceLookup`, activación de `machine.invoice.preprinted` y `machine.invoice.printername`, reinicio del POS, guardado del PDF resultante y sincronización de `ROADMAP.md` / `featurelist.json` / `ACTIVE.md`.

**Fuera de alcance:** impresión sobre papel preimpreso real sin autorización nueva y explícita, facturación electrónica, cambios de esquema, nueva línea de feature (sería `3.84.0`) y cambios en el código de render ya entregado salvo corrección de coordenadas medidas.

## 2. Archivos / módulos afectados

- VM Windows 10: impresora `PDFCreator` (PDFCreator 6.3.1, `C:\Program Files\PDFCreator`), instalada desde `PDFCreator-6_3_1-Setup.exe`.
- `%USERPROFILE%\unicentaopos.properties`: `machine.invoice.preprinted=true`, `machine.invoice.printername=PDFCreator`.
- `reports/invoice-template.properties`: **sin cambios** (no hay formulario físico medido).
- Repositorio: solo documentación de plan/versionado; **sin cambios de código, SQL ni de `AppLocal.APP_VERSION`** (sigue en `3.82`).
- Documentación: este plan, el plan padre, `ROADMAP.md`, `featurelist.json`, `docs/plans/ACTIVE.md` y `AGENTS.md`.

## 3. Pasos ejecutados

1. [x] Verificar privilegios: `whoami /groups` → `BUILTIN\Administrators` y `Mandatory Label\High Mandatory Level`.
2. [x] Intentar *Microsoft Print to PDF*: `dism /online /get-featureinfo /featurename:Printing-PrintToPDFServices-Features` devolvió `Error: 0x800f080c` y `Feature name Printing-PrintToPDFServices-Features is unknown`. La imagen no dispone de esa característica (tampoco de PowerShell).
3. [x] Instalar la alternativa GPL v3: `PDFCreator-6_3_1-Setup.exe /VerySilent /NoIcons /DisableContextMenu /LogFile=C:\unicenta-build\pdfcreator-fullsetup.log`. El primer intento desde el recurso UNC terminó con `Access is denied`; se copió el instalador a `C:\unicenta-build\PDFCreator-6_3_1-Setup.exe` y la segunda ejecución devolvió `SETUP_EXIT=0`.
4. [x] Verificar el servicio de impresión: `wmic printer get name,drivername` → `PDFCreator  PDFCreator`; `PrintProbe.java` (`javax.print.PrintServiceLookup`) → `DEFAULT=Win32 Printer : PDFCreator` y `SERVICE=PDFCreator`.
5. [x] Activar la factura en `%USERPROFILE%\unicentaopos.properties` con `machine.invoice.preprinted=true` y `machine.invoice.printername=PDFCreator` (coincidencia exacta con el nombre del `PrintService`).
6. [x] Reiniciar el POS con `launch.cmd`: proceso `java.exe` (PID 3032) y título `uniCenta oPOS - 3.82`; `pos.log` registra la lectura de `C:\Users\Marcelo\unicentaopos.properties`.
7. [x] Generar el PDF desde el `Printable` de producción: `InvoicePrintJob.java` compila contra `C:\unicenta\unicentaopos.jar` y llama a `InvoicePrintService.print(ticket)`; `C:\unicenta-build\printjob.log` registra `PRINT_OK=true`.
8. [x] Guardar el trabajo con el botón `SaveButton` de la GUI de PDFCreator (UI Automation, `TargetFolderTextBox=C:\unicenta-build`, `TargetFilenameTextBox=factura-preimpresa-vm`) → `C:\unicenta-build\factura-preimpresa-vm.pdf` (8.920 bytes, 1 página). Copia archivada en `\\192.168.122.1\compartidos\factura-preimpresa-printerjob.pdf`.
9. [x] Revisar el contenido con `pdftotext -layout`: dos copias, fecha `27 de noviembre del 2026`, `CONTADO (X)`, líneas exenta/5 %/10 %, total `10.254` y `Diez Mil Doscientos Cincuenta Y Cuatro` en letras.
10. [x] Sincronizar documentación y estado `3.83.1` en `ROADMAP.md`, `featurelist.json`, `ACTIVE.md` y `AGENTS.md`, y publicar en `origin/main`.

## 4. Criterios de aceptación (DoD)

- [x] Una impresora PDF del sistema aparece en `PrintServiceLookup` y el PDF se genera desde el `Printable`/`PrinterJob` de producción (no desde el render auxiliar con iText de `3.83.0`).
- [x] La factura preimpresa se activa solo por configuración, sin recompilar (el JAR desplegado es el de `3.83.0`).
- [x] El ticket térmico sigue funcionando igual y no hay impresión duplicada (único enganche en `JPanelTicket.closeTicket()`).
- [x] No se tocó código: los 71 tests ya validados en `3.83.0` siguen en verde en Linux y VM Windows 10 (`ant -f build.xml clean test dist`, suites registradas en `build_working.xml`).
- [x] Sin papel real, sin cambios de esquema y sin cambios de `AppLocal.APP_VERSION`.
- [x] `ROADMAP.md`, `featurelist.json`, `AGENTS.md` y `ACTIVE.md` sincronizados con `3.83.1` entregado.

## 5. Evidencia

| Validación | Comando / acción | Salida o archivo |
| :--- | :--- | :--- |
| Privilegios | `whoami /groups` | `BUILTIN\Administrators`, `Mandatory Label\High Mandatory Level` |
| *Microsoft Print to PDF* no disponible | `dism /online /get-featureinfo /featurename:Printing-PrintToPDFServices-Features` | `Error: 0x800f080c`, `Feature name ... is unknown` |
| Instalación de PDFCreator | `PDFCreator-6_3_1-Setup.exe /VerySilent /NoIcons /DisableContextMenu /LogFile=...` | `SETUP_EXIT=0`; `fullsetup2.out`, `pdfcreator-fullsetup.log` |
| Impresora del sistema | `wmic printer get name,drivername` | `PDFCreator  PDFCreator` |
| Servicio Java | `javac PrintProbe.java` + `java PrintProbe` | `DEFAULT=Win32 Printer : PDFCreator`, `SERVICE=PDFCreator` |
| Configuración activa | `findstr /b "machine.invoice" "%USERPROFILE%\unicentaopos.properties"` | `machine.invoice.preprinted=true`, `machine.invoice.printername=PDFCreator` (`invoice_config.out`) |
| POS reiniciado | `tasklist /v` | `java.exe` PID 3032, `uniCenta oPOS - 3.82` (`checkpos.out`, `pos.log`) |
| Job real de impresión | `java -cp ... InvoicePrintJob` | `PRINT_OK=true` (`C:\unicenta-build\printjob.log`, `\\192.168.122.1\compartidos\printjob.log`) |
| PDF generado | Botón `SaveButton` de PDFCreator vía UI Automation | `factura-preimpresa-vm.pdf`, 8.920 bytes, 1 página; copia en `factura-preimpresa-printerjob.pdf` |
| Contenido del PDF | `pdftotext -layout factura-preimpresa-printerjob.pdf -` | Dos copias, importes, total `10.254` y total en letras correctos |
| Build/tests previos | `ant -f build.xml clean test dist` | 71 tests OK en Linux y Windows 10 (`build_vendese_vm.log`) |

- Commits: la implementación de `3.83.0` está publicada en `5dd42b9` y anteriores; este cierre documental se publica en `origin/main` en el commit de cierre del plan.
- Restricción cumplida: **no se imprimió papel preimpreso real**. La ventana informativa residual de `PDFCreator-cli.exe` (`0xe0434352`) apareció durante las pruebas de la interfaz de línea de comandos; se terminaron esos procesos y no afecta al servicio `PDFCreator` ni al `PrintService`.
- Sin cambios de esquema: no corresponde script `*-upgrade-*.sql` para este PATCH.
