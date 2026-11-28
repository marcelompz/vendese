# Registro de planes activos — @vendese

Índice de mejoras e iniciativas para mantener el control de cambios y evitar solapamientos. Las iniciativas **no cerradas** se listan primero; las cerradas se conservan debajo como referencia. Antes de iniciar trabajo no trivial, revisar este registro, [`AGENTS.md`](../../AGENTS.md), [`IMPROVEMENT_PLAN.md`](IMPROVEMENT_PLAN.md) y el [`ROADMAP.md`](../../ROADMAP.md) / [`featurelist.json`](../../featurelist.json).

- **Regla normativa:** secciones *"Criterio de versionamiento y hoja de ruta"* y *"Planes de trabajo y ciclo de vida documental"* en [`AGENTS.md`](../../AGENTS.md).
- **Última actualización:** 2026-11-28 (criterio `MAYOR.FEATURE.PATCH`: base `3.82.0`; `3.83.0`, `3.83.1` y `3.84.0` **cerrados**). La factura preimpresa solo se validó en PDF virtual, sin papel real por decisión del usuario. `3.84.0` cubre JDK 8/17 y drivers JDBC modernos en VM Windows 10; JDK 21 y conexiones reales a BD quedan diferidos. Quedan activos los planes de calidad, datos y SaaS.

---

## Iniciativas activas y de referencia

## Iniciativas pendientes o en curso

| id | dominio | título | estado | versión objetivo | alcance breve | enlace |
|---|---|---|---|---|---|---|
| `PLAN-MEJORA-FASE-4-CALIDAD` | arquitectura / calidad | Refactorización incremental de God classes, ejecución asíncrona y cobertura | **en_progreso** | 3.85.0 (reservada) | Pendiente según `IMPROVEMENT_PLAN.md`: dividir `JPanelTicket`, `JPanelConfigPeripheral` y `DataLogicSales`; completar `SwingWorker` en Jasper/CSV; eliminar duplicación y tipos raw; elevar cobertura de tests. | [`IMPROVEMENT_PLAN.md`](IMPROVEMENT_PLAN.md) |
| `PLAN-MEJORA-FASE-5-DATOS` | datos | Parametrización completa de SQL (`QBFBuilder`/`NormalBuilder`) y endurecimiento de esquema | **en_progreso** | 3.86.0 (reservada) | Implementadas 28 sentencias `PreparedSentence`. Pendiente: parametrizar `QBFBuilder`/`NormalBuilder`, eliminar SQL concatenado residual, añadir constraints/pool y evaluar migraciones versionadas. | [`IMPROVEMENT_PLAN.md`](IMPROVEMENT_PLAN.md) |
| `PLAN-SAAS-POSTGRESQL` | saas / datos | Evolución de despliegue como SaaS con PostgreSQL | **borrador** | 3.87.0 (por definir) | Preparar `@vendese` / backend asociado para operación SaaS sobre PostgreSQL (actualizar driver JDBC compatible con `scram-sha-256`, revisar `SessionDBPostgreSQL`, aislamiento multi-tenant y empaquetado de servidor). Se definirá plan detallado al retomar esta etapa. | *(pendiente de crear en `docs/plans/saas/`)* |

## Cerradas (referencia)

| id | dominio | título | estado | versión objetivo | alcance breve | enlace |
|---|---|---|---|---|---|---|
| `PLAN-INFRA-UPGRADE-RUNTIME-BD` | infra | Runtime JDK 8/17 LTS y drivers JDBC modernos | **cerrado el 2026-11-28** | 3.84.0 | Compatibilidad verificada en VM Windows 10 con JDK 8 y 17 manteniendo bytecode Java 7; drivers PostgreSQL 42.7.7, MariaDB 3.5.3 y MySQL Connector/J 8.4.0; JasperReports con `javac` externo. JDK 21 y conexiones reales a servidores quedan diferidos. | [`PLAN-INFRA-UPGRADE-RUNTIME-BD.md`](PLAN-INFRA-UPGRADE-RUNTIME-BD/PLAN-INFRA-UPGRADE-RUNTIME-BD.md) |
| `PLAN-versionamiento-roadmap-featurelist-2026-11-27` | infra (documentación / proceso) | Criterio de versionamiento `MAYOR.FEATURE.PATCH`, `ROADMAP.md` y `featurelist.json` | **cerrado el 2026-11-27** | 3.83.0 (documentación; sin bump de código) | Define base `3.82.0`, `3.83.0` para la factura preimpresa y `3.83.1` para sus ajustes; reserva `3.84.0`–`3.87.0` para los planes activos. Sin cambios en `AppLocal.APP_VERSION` (sigue en `3.82`). | [`plan-versionamiento-roadmap-featurelist-2026-11-27.md`](infra/plan-versionamiento-roadmap-featurelist-2026-11-27.md) · [`ROADMAP.md`](../../ROADMAP.md) · [`featurelist.json`](../../featurelist.json) |
| `PLAN-impresion-factura-preimpresa-vendese-2026-11-27` | impresion (transversal: POS / ticket / reportes) | Impresión de factura preimpresa en @vendese con plantilla en mm y validación en VM Windows 10 | **cerrado el 2026-11-27** | 3.83.0 | Render Java2D + `PrinterJob`, dos copias, `invoice-template.properties` ajustable sin recompilar y un único disparador en el cierre de venta. 71 tests OK en Linux/Windows, PDF virtual validado; sin papel real por decisión del usuario. Sin cambios de esquema. | [`plan-impresion-factura-preimpresa-vendese-2026-11-27.md`](impresion/plan-impresion-factura-preimpresa-vendese-2026-11-27.md) |
| `PLAN-ajuste-impresion-factura-pdf-vm-2026-11-27` | impresion (VM / configuración) | Ajustes a la factura preimpresa: impresora PDF en VM y activación por propiedades | **cerrado el 2026-11-27** | 3.83.1 (PATCH de 3.83.0) | *Microsoft Print to PDF* no existe en la imagen; se instaló PDFCreator 6.3.1 (GPL v3). Verificado `PrintServiceLookup`, activados `machine.invoice.preprinted` / `machine.invoice.printername=PDFCreator`, reiniciado el POS y generado el PDF desde `PrinterJob` (`PRINT_OK=true`). Sin papel real sin autorización nueva. | [`plan-ajuste-impresion-factura-pdf-vm-2026-11-27.md`](impresion/plan-ajuste-impresion-factura-pdf-vm-2026-11-27.md) |
| `PLAN-MEJORA-FASES-0-3` | seguridad / build | Endurecimiento base (PBKDF2, AES-GCM, RMI loopback, ScriptSecurity) y build Ant/CI | **cerrado** | 3.81.0 / 3.82.0 (legado) | Fases 0–3 implementadas y verificadas (build unificado, hash PBKDF2, AES-GCM v2, RMI `127.0.0.1` + token, validación de scripts y `db.driverlib`, suite JUnit). | [`IMPROVEMENT_PLAN.md`](IMPROVEMENT_PLAN.md) |

---

## Plantilla rápida para nuevos planes (`docs/plans/<dominio>/<plan>.md`)

Al agregar una mejora nueva, crea un archivo Markdown en `docs/plans/<dominio>/` con esta estructura y regístralo en la tabla superior:

```markdown
# PLAN-<AREA>-<NOMBRE> — <Título>

- **ID:** `PLAN-<AREA>-<NOMBRE>`
- **Dominio:** `<pos | datos | ui | saas | infra>`
- **Estado:** `borrador | propuesto | aprobado | en_progreso | entregado | cerrado`
- **Versión objetivo:** `3.83.0` (o `3.83.1` si es un ajuste de esa misma feature; tres componentes siempre)
- **Fecha:** `YYYY-MM-DD`

## 1. Objetivo y alcance
Qué problema resuelve y qué queda fuera de alcance.

## 2. Archivos / módulos afectados
Lista de rutas estimadas (`src-pos/...`, `scripts/...`, `test/...`).

## 3. Pasos de implementación
1. ...
2. ...

## 4. Criterios de aceptación (DoD)
- [ ] Compilación y tests pasando (`ant -f build.xml clean test dist`).
- [ ] Nuevas clases `*Test` registradas en `build_working.xml`.
- [ ] Scripts SQL actualizados en los 5 motores + upgrade (si cambia esquema).
- [ ] Documentación actualizada (`README.md` / `ARCHITECTURE.md` / `ACTIVE.md` / `ROADMAP.md` / `featurelist.json`).

## 5. Evidencia
- Commits:
- Tests / validaciones ejecutadas:
```

## Notas de mantenimiento

- Cuando una iniciativa pase a `cerrado` (o `supersedido` / `archivado`), actualiza su estado final con evidencia en su documento fuente y retira o marca la fila en este registro.
- Todo cambio de versión o estado debe replicarse en [`ROADMAP.md`](../../ROADMAP.md) y [`featurelist.json`](../../featurelist.json) con la misma descripción, fecha y plan de origen.
- Si una mejora afecta el arranque, la configuración o los requisitos de instalación, sincroniza también [`README.md`](../../README.md).
