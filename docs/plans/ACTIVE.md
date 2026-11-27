# Registro de planes activos — @vendese

Índice de mejoras e iniciativas **no cerradas** para mantener el control de cambios y evitar solapamientos. Antes de iniciar trabajo no trivial, revisar este registro, [`AGENTS.md`](../../AGENTS.md) y [`IMPROVEMENT_PLAN.md`](../../IMPROVEMENT_PLAN.md).

- **Regla normativa:** sección *"Planes de trabajo y ciclo de vida documental"* en [`AGENTS.md`](../../AGENTS.md).
- **Última actualización:** 2026-11-27 (creación de `AGENTS.md` y registro `docs/plans/ACTIVE.md`; manual de instalación consolidado en `README.md`; plan `PLAN-impresion-factura-preimpresa-vendese-2026-11-27` en `en_progreso` con implementación y tests pendientes de ejecutar en VM).

---

## Iniciativas activas y de referencia

| id | dominio | título | estado | versión objetivo | alcance breve | enlace |
|---|---|---|---|---|---|---|
| `PLAN-impresion-factura-preimpresa-vendese-2026-11-27` | impresion (transversal: POS / ticket / reportes) | Impresión de factura preimpresa en @vendese con plantilla en mm y validación en VM Windows 10 | **borrador** | 3.83 | Render Java2D + `PrinterJob`, dos copias, `invoice-template.properties` ajustable sin recompilar y un único disparador en el cierre de venta. Sin cambios de esquema en la primera entrega. | [`plan-impresion-factura-preimpresa-vendese-2026-11-27.md`](impresion/plan-impresion-factura-preimpresa-vendese-2026-11-27.md) |
| `PLAN-MEJORA-FASES-0-3` | seguridad / build | Endurecimiento base (PBKDF2, AES-GCM, RMI loopback, ScriptSecurity) y build Ant/CI | **cerrado** | 3.81 / 3.82 | Fases 0–3 implementadas y verificadas (build unificado, hash PBKDF2, AES-GCM v2, RMI `127.0.0.1` + token, validación de scripts y `db.driverlib`, suite JUnit). | [`IMPROVEMENT_PLAN.md`](../../IMPROVEMENT_PLAN.md) |
| `PLAN-MEJORA-FASE-4-CALIDAD` | arquitectura / calidad | Refactorización incremental de God classes, ejecución asíncrona y cobertura | **en_progreso** | 3.83+ | Pendiente según `IMPROVEMENT_PLAN.md`: dividir `JPanelTicket`, `JPanelConfigPeripheral` y `DataLogicSales`; completar `SwingWorker` en Jasper/CSV; eliminar duplicación y tipos raw; elevar cobertura de tests. | [`IMPROVEMENT_PLAN.md`](../../IMPROVEMENT_PLAN.md) |
| `PLAN-MEJORA-FASE-5-DATOS` | datos | Parametrización completa de SQL (`QBFBuilder`/`NormalBuilder`) y endurecimiento de esquema | **en_progreso** | 3.83+ | Implementadas 28 sentencias `PreparedSentence`. Pendiente: parametrizar `QBFBuilder`/`NormalBuilder`, eliminar SQL concatenado residual, añadir constraints/pool y evaluar migraciones versionadas. | [`IMPROVEMENT_PLAN.md`](../../IMPROVEMENT_PLAN.md) |
| `PLAN-SAAS-POSTGRESQL` | saas / datos | Evolución de despliegue como SaaS con PostgreSQL | **borrador** | Por definir | Preparar `@vendese` / backend asociado para operación SaaS sobre PostgreSQL (actualizar driver JDBC compatible con `scram-sha-256`, revisar `SessionDBPostgreSQL`, aislamiento multi-tenant y empaquetado de servidor). Se definirá plan detallado al retomar esta etapa. | *(pendiente de crear en `docs/plans/saas/`)* |

---

## Plantilla rápida para nuevos planes (`docs/plans/<dominio>/<plan>.md`)

Al agregar una mejora nueva, crea un archivo Markdown en `docs/plans/<dominio>/` con esta estructura y regístralo en la tabla superior:

```markdown
# PLAN-<AREA>-<NOMBRE> — <Título>

- **ID:** `PLAN-<AREA>-<NOMBRE>`
- **Dominio:** `<pos | datos | ui | saas | infra>`
- **Estado:** `borrador | propuesto | aprobado | en_progreso | entregado | cerrado`
- **Versión objetivo:** `3.83`
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
- [ ] Documentación actualizada (`README.md` / `ARCHITECTURE.md` / `ACTIVE.md`).

## 5. Evidencia
- Commits:
- Tests / validaciones ejecutadas:
```

## Notas de mantenimiento

- Cuando una iniciativa pase a `cerrado` (o `supersedido` / `archivado`), actualiza su estado final con evidencia en su documento fuente y retira o marca la fila en este registro.
- Si una mejora afecta el arranque, la configuración o los requisitos de instalación, sincroniza también [`README.md`](../../README.md).
