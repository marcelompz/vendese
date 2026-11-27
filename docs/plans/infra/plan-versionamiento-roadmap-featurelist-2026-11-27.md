# PLAN-versionamiento-roadmap-featurelist-2026-11-27 — Criterio de versionamiento y hoja de ruta

- **ID:** `PLAN-versionamiento-roadmap-featurelist-2026-11-27`
- **Dominio:** `infra` (documentación y proceso)
- **Estado:** `entregado`
- **Versión objetivo:** `3.83.0` (solo documentación; sin bump de versión en el código)
- **Fecha:** `2026-11-27`
- **Registro:** [`ROADMAP.md`](../../../ROADMAP.md) · [`featurelist.json`](../../../featurelist.json) · [`ACTIVE.md`](../ACTIVE.md)

## 1. Objetivo y alcance

Establecer un criterio de versionamiento para @vendese a partir de la línea base `3.82` y reflejar cada feature en `ROADMAP.md` y `featurelist.json`, con descripción del cambio por versión. La impresión de factura preimpresa pasa a ser `3.83.0` y sus mejoras `3.83.1`, `3.83.2`, etc.

**Incluye:** definición `MAYOR.FEATURE.PATCH`, creación de ambos artefactos, correlación de los planes activos con versiones reservadas y sincronización de `AGENTS.md` y `docs/plans/ACTIVE.md`.

**Fuera de alcance:** cambiar `AppLocal.APP_VERSION`, `build_working.xml`, `installer/unicentaopos.iss` o scripts SQL. El runtime permanece en `3.82` hasta un release autorizado; las versiones del roadmap son registro de entrega/planificación.

## 2. Archivos / módulos afectados

- `ROADMAP.md` y `featurelist.json` (nuevos, fuentes de verdad).
- `AGENTS.md` (sección de versionamiento y sincronización de releases).
- `docs/plans/ACTIVE.md` (versión objetivo de cada plan y registro del propio plan).
- Planes correlacionados: `docs/plans/impresion/plan-impresion-factura-preimpresa-vendese-2026-11-27.md`, `docs/plans/impresion/plan-ajuste-impresion-factura-pdf-vm-2026-11-27.md`, `docs/plans/PLAN-INFRA-UPGRADE-RUNTIME-BD/PLAN-INFRA-UPGRADE-RUNTIME-BD.md` y `docs/plans/IMPROVEMENT_PLAN.md`.

## 3. Pasos de implementación

1. [x] Definir el criterio: `MAYOR.FEATURE.PATCH`; `3.83.0` = cambio mayor funcional (nueva línea), `3.83.1` = ajuste dentro de esa línea.
2. [x] Crear `ROADMAP.md` con base `3.82.0`, versión entregada `3.83.0`, ajuste entregado `3.83.1` y líneas reservadas `3.84.0`–`3.87.0`.
3. [x] Crear `featurelist.json` con la misma información en formato estructurado y validar el JSON.
4. [x] Crear el plan del ajuste `3.83.1` y actualizar la versión objetivo de los planes correlacionados.
5. [x] Sincronizar `AGENTS.md` y `docs/plans/ACTIVE.md`, incluida la plantilla de nuevos planes.
6. [x] Publicar en `origin/main` con el cierre de `3.83.1`; verificado que no se alteró la versión de runtime (`AppLocal.APP_VERSION` sigue en `3.82`).

## 4. Criterios de aceptación (DoD)

- [x] `featurelist.json` válido y consistente con `ROADMAP.md` (mismas versiones, estados, fechas y planes).
- [x] `AGENTS.md` y `docs/plans/ACTIVE.md` referencian el criterio, los artefactos y todos los planes versionados.
- [x] Los planes de impresión e infra usan versiones de tres componentes.
- [x] Sin cambios de código, SQL ni versión de runtime (`3.82`).
- [x] Evidencia publicada en `origin/main` en el commit de cierre del plan `3.83.1`.

## 5. Evidencia

- Commits: la implementación de `3.83.0` quedó publicada hasta `5dd42b9`; este cierre documental (incluido el registro de `3.83.1` con PDFCreator en la VM) se publicó en `origin/main` como `0e16169`.
- Tests / validaciones ejecutadas: validación sintáctica de `featurelist.json` y revisión de enlaces relativos en los documentos; sin cambios en `AppLocal.APP_VERSION` ni en `build_working.xml` / `installer/unicentaopos.iss`.
