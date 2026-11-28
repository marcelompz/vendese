# ROADMAP — @vendese

> Criterio: [`MAYOR.FEATURE.PATCH`](#criterio-de-versionamiento), con línea base `3.82.0` (la versión heredada `3.82` se escribe con tres componentes en esta hoja de ruta).
> **Fuentes de verdad:** [`ROADMAP.md`](ROADMAP.md) y [`featurelist.json`](featurelist.json). Toda versión debe quedar reflejada también en [`AGENTS.md`](AGENTS.md) y en [`docs/plans/ACTIVE.md`](docs/plans/ACTIVE.md).
> Última actualización: 2026-11-27. `3.83.0` y `3.83.1` están **cerradas** (solo PDF virtual; sin papel real ni bump de release). El código en ejecución sigue en `3.82` (`AppLocal.APP_VERSION`): las versiones de abajo son el registro de entrega/planificación, no un bump de release autorizado.

## Criterio de versionamiento

| Componente | Nombre | Se incrementa cuando… | Ejemplo |
|---|---|---|---|
| 1.º | `MAYOR` (compatibilidad) | Hay un cambio incompatible de arquitectura o de API de datos. Reservado; hoy `3`. | `3.x.y → 4.0.0` |
| 2.º | `FEATURE` (línea funcional) | Llega una funcionalidad relevante o un «cambio mayor funcional». El tercer componente vuelve a `0`. | `3.82.0 → 3.83.0` |
| 3.º | `PATCH` (ajuste) | Se mejora o corrige algo **dentro de la misma** línea de feature, sin abrir una nueva. | `3.83.0 → 3.83.1` |

- En este proyecto «cambio mayor» significa subir el componente `FEATURE` (`3.83.0`), no un `MAJOR` semver (`4.0.0`).
- El primer feature del fork documentado con este criterio es la impresión de factura preimpresa: `3.83.0`; sus mejoras posteriores son `3.83.1`, `3.83.2`, etc.
- Las versiones `3.84.0+` están **reservadas** para planes ya registrados: fijan la numeración, no una fecha de entrega. Si se reordenan, se actualizan este archivo, `featurelist.json`, `ACTIVE.md` y el plan afectado en el mismo cambio.
- Un `PATCH` sin cambios de esquema no añade scripts `*-upgrade-*.sql`. Si los hay, se aplica la regla de los 5 motores de [`AGENTS.md`](AGENTS.md#invariantes-de-arquitectura-y-seguridad-no-romper).

## Versiones

| Versión | Estado | Fecha | Feature / cambio descrito | Plan de origen | Evidencia |
|---|---|---|---|---|---|
| `3.82.0` | entregado (base heredada) | histórico | Línea base de uniCenta oPOS en @vendese: build Ant/CI unificado, seguridad fases 0–3 (PBKDF2, AES-GCM, RMI loopback, ScriptSecurity) y precios por cantidad. | `PLAN-MEJORA-FASES-0-3` — [`IMPROVEMENT_PLAN.md`](docs/plans/IMPROVEMENT_PLAN.md) | Fases 0–3 verificadas; ZIPs `unicentaopos_3.82_bin.zip` y `unicentaopos_3.82_src.zip`. |
| `3.83.0` | **cerrado** | 2026-11-27 | **Impresión de factura preimpresa**: render Java2D + `PrinterJob` con coordenadas en mm, dos copias, plantilla `invoice-template.properties` con override en `%USERPROFILE%` y un único disparador en `JPanelTicket.closeTicket()`. Sin cambios de esquema. | [`PLAN-impresion-factura-preimpresa-vendese-2026-11-27`](docs/plans/impresion/plan-impresion-factura-preimpresa-vendese-2026-11-27.md) | 71 tests OK en Linux y VM Windows 10; build/dist OK; PDF virtual validado; sin papel real por decisión del usuario. |
| `3.83.1` | **cerrado** | 2026-11-27 | **Ajustes a la misma feature**: *Microsoft Print to PDF* no existe en la imagen, así que se instaló PDFCreator 6.3.1 (GPL v3) en la VM; activar `machine.invoice.preprinted` / `machine.invoice.printername=PDFCreator`, reiniciar el POS y generar el PDF desde `PrinterJob` (no desde el render auxiliar). Coordenadas sin cambios: no hay formulario físico medido. | [`PLAN-ajuste-impresion-factura-pdf-vm-2026-11-27`](docs/plans/impresion/plan-ajuste-impresion-factura-pdf-vm-2026-11-27.md) | `wmic printer` → `PDFCreator`; `PrintProbe` → `DEFAULT=Win32 Printer : PDFCreator`; `PRINT_OK=true` en `printjob.log`; `factura-preimpresa-printerjob.pdf` (1 página, dos copias, total 10.254) revisado con `pdftotext -layout`; POS `uniCenta oPOS - 3.82`. Sin papel real. |
| `3.84.0` | borrador (reservada) | 2026-11-27 | **Runtime y BD**: Java 17/21 LTS y drivers JDBC modernos para MariaDB 10.11+/PostgreSQL 15+ (`scram-sha-256`). Requiere revisar la invariante `source/target 1.7`. | [`PLAN-INFRA-UPGRADE-RUNTIME-BD`](docs/plans/PLAN-INFRA-UPGRADE-RUNTIME-BD/PLAN-INFRA-UPGRADE-RUNTIME-BD.md) | Sin evidencia todavía. |
| `3.85.0` | en_progreso (reservada) | 2026-11-27 | **Calidad (Fase 4)**: refactorización incremental de *God classes*, ejecución asíncrona y cobertura de tests. | `PLAN-MEJORA-FASE-4-CALIDAD` — [`IMPROVEMENT_PLAN.md`](docs/plans/IMPROVEMENT_PLAN.md) | Trabajo incremental en curso; ver plan. |
| `3.86.0` | en_progreso (reservada) | 2026-11-27 | **Datos (Fase 5)**: parametrización de `QBFBuilder`/`NormalBuilder`, reducción de SQL concatenado y endurecimiento de esquema. | `PLAN-MEJORA-FASE-5-DATOS` — [`IMPROVEMENT_PLAN.md`](docs/plans/IMPROVEMENT_PLAN.md) | 28 sentencias `PreparedSentence` implementadas; resto pendiente. |
| `3.87.0` | borrador (por definir) | 2026-11-27 | **SaaS sobre PostgreSQL**: driver compatible, aislamiento multi-tenant y empaquetado de servidor. | `PLAN-SAAS-POSTGRESQL` — pendiente de crear en `docs/plans/saas/` | Sin evidencia todavía. |

## Proceso de documentación (sin release de código)

- [`PLAN-versionamiento-roadmap-featurelist-2026-11-27`](docs/plans/infra/plan-versionamiento-roadmap-featurelist-2026-11-27.md) — **cerrado** en `351ab15`: define el criterio `MAYOR.FEATURE.PATCH`, crea `ROADMAP.md` y `featurelist.json` y los sincroniza con `AGENTS.md` / `ACTIVE.md`. No modifica `AppLocal.APP_VERSION`.

## Cómo registrar una versión nueva

1. Elegir `3.<FEATURE>.<PATCH>` según la tabla de criterio; si es una mejora de `3.83.0`, usar `3.83.1`, no `3.84.0`.
2. Crear o actualizar el plan en `docs/plans/<dominio>/` con `versión objetivo` de tres componentes y su evidencia.
3. Añadir la entrada en **`ROADMAP.md`** y en **`featurelist.json`** con los mismos `versión`, `estado`, `fecha`, `descripción` y ruta de plan.
4. Reflejarla en **`AGENTS.md`** (sección de versionamiento) y en **`docs/plans/ACTIVE.md`**.
5. Solo al publicar un release: sincronizar `AppLocal.APP_VERSION`, `build_working.xml`, `installer/unicentaopos.iss`, scripts SQL si aplica y referencias de versión de `README.md` / `ARCHITECTURE.md` (ver [`AGENTS.md`](AGENTS.md#sincronización-de-versión-obligatoria-en-releases)).
