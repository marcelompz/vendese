# PLAN-INTEGRACION-OMNIFLOW-ECOSYSTEM — Integración de `@vendese` con OmniGastro y capa EE OmniFlow

- **ID:** `PLAN-INTEGRACION-OMNIFLOW-ECOSYSTEM`
- **Dominio:** `integracion`
- **Estado:** `borrador`
- **Versión objetivo:** `3.84+`
- **Fecha:** `2026-11-27`

## 1. Objetivo y alcance
Establecer una arquitectura de integración desacoplada entre `@vendese` (terminal POS de escritorio bajo licencia GPLv3) y la suite **OmniFlow** (**OmniGastro**, **OmniLedger**, **OmniBI**), permitiendo operar ventas locales con resiliencia offline mientras se delega la operación gastronómica avanzada, la contabilidad/facturación electrónica y la inteligencia de negocios a servicios externos.

**Principios rectores:**
- **Separación de procesos y licencias:** Toda comunicación se realiza por contratos externos (API REST/JSON, webhooks o cola de eventos en BD/HTTP) sin enlazar código propietario dentro de la JVM GPLv3 de `@vendese`.
- **Resiliencia local (*Offline-first*):** `@vendese` nunca bloquea un cobro ni una impresión por caída de red; persiste eventos localmente y sincroniza de forma asíncrona e idempotente.
- **Fuente única de verdad por dominio:** Catálogo/precios maestros desde OmniFlow; comandas de salón/KDS en OmniGastro; cobro, cierre de caja y periféricos físicos en `@vendese`; asientos contables y documentos fiscales en `OmniLedger`.

**Fuera de alcance:**
- Embeber interfaces web/móviles de OmniGastro dentro de Java Swing.
- Reemplazar el motor de tickets/impresión local de `@vendese` (ya cubierto por `PLAN-impresion-factura-preimpresa-vendese-2026-11-27`).
- Refactorización interna de clases heredadas (cubierta por `PLAN-MEJORA-FASE-4-CALIDAD` y `PLAN-MEJORA-FASE-5-DATOS`).

## 2. Archivos / módulos afectados
- `src-pos/com/openbravo/pos/integration/` *(nuevo paquete desacoplado)*:
  - Cliente HTTP/REST ligero, serialización JSON de eventos y trabajador asíncrono de despacho (*Outbox Worker*).
  - Adaptadores de recepción/importación de pre-cuentas desde OmniGastro y sincronización incremental de catálogo.
- `src-pos/com/openbravo/pos/sales/JPanelTicket.java` y `DataLogicSales.java`:
  - Hooks mínimos al confirmar venta, anular ticket, cerrar caja o importar orden de mesa (sin añadir lógica de negocio pesada en UI).
- `src-pos/com/openbravo/pos/config/` y `unicentaopos.properties`:
  - Parámetros de conexión (`omniflow.api.url`, `omniflow.tenant.id`, credenciales/tokens cifrados con el esquema AES-GCM v2 de `PLAN-MEJORA-FASES-0-3`, intervalos de sincronización).
- `scripts/` (los 5 motores soportados + `upgrade`):
  - Nueva tabla de cola transaccional (`INTEGRATION_OUTBOX`) y mapeo de identificadores externos (`EXTERNAL_ENTITY_MAP` / columnas `external_id`, `sync_status`).
- `test/com/openbravo/pos/integration/`:
  - Suite JUnit con mocks HTTP para idempotencia, reintentos ante fallo de red, traducción de modificadores/impuestos y conciliación de cierre.

## 3. Pasos de implementación (Fases macro)

### Fase 1: Contrato de datos y patrón Outbox en `@vendese`
1. Definir el esquema JSON canónico de eventos (`sale.completed`, `sale.voided`, `shift.closed`, `stock.adjusted`) con `event_id` (UUID), `idempotency_key`, desglose de líneas, modificadores, impuestos, medios de pago y metadatos de terminal/cajero.
2. Crear la tabla `INTEGRATION_OUTBOX` en los scripts SQL (y script de migración) para registrar eventos dentro de la misma transacción de cierre de venta.
3. Implementar el `OutboxDispatcher` en segundo plano con reintentos exponenciales, manejo de errores transitorios/permanentes y cifrado de credenciales en configuración.

### Fase 2: Puente operativo con `OmniGastro` (Salón, KDS y Caja)
1. Definir el flujo de cuenta consolidada: OmniGastro gestiona mesas, comandas desde tablets/QR y estados de cocina/barra (KDS), exponiendo la orden lista para cobro mediante API o evento hacia `@vendese`.
2. Implementar en `@vendese` el importador de pre-cuentas de OmniGastro hacia un ticket activo (mapeando platos, guarniciones/modificadores, descuentos y propinas sin pérdida de precisión decimal).
3. Emitir el webhook/evento de confirmación de pago desde `@vendese` hacia OmniGastro para liberar la mesa, actualizar el estado en KDS/sala y disparar la impresión local correspondiente.

### Fase 3: Sincronización de maestros (OmniFlow → `@vendese`)
1. Implementar un sincronizador incremental (`GET /catalog/changes?since=...`) para actualizar categorías, productos, códigos de barras, listas de precios e impuestos en la base local de `@vendese`.
2. Garantizar resolución determinista de conflictos (los cambios maestros provienen de OmniFlow; el stock operativo local reporta movimientos de salida/ajuste sin sobrescribir precios).

### Fase 4: Integración fiscal y contable con `OmniLedger`
1. Conectar los eventos `sale.completed`, `sale.voided` y `shift.closed` al pipeline de `OmniLedger` para generación automática de asientos contables y emisión/validación de facturación electrónica.
2. Permitir que `@vendese` consulte o reciba de forma asíncrona el estado fiscal/CDC/timbrado o comprobante electrónico asociado para reimpresión o auditoría en caja.

### Fase 5: Alimentación analítica hacia `OmniBI`
1. Exponer desde el bus de eventos de OmniFlow (alimentado por el *outbox* de `@vendese` y `OmniGastro`) los hechos normalizados de ventas, márgenes, tiempos de despacho KDS, desempeño por turno/cajero y comparativa multi-sucursal.
2. Validar cuadratura diaria entre el reporte de cierre Z de `@vendese`, los asientos de `OmniLedger` y los tableros de `OmniBI`.

## 4. Criterios de aceptación (DoD)
- [ ] Compilación y tests pasando (`ant -f build.xml clean test dist`) sobre el runtime actualizado (`PLAN-INFRA-UPGRADE-RUNTIME-BD`).
- [ ] Nuevas clases `*Test` de integración registradas en `build_working.xml`.
- [ ] Scripts SQL (`INTEGRATION_OUTBOX` y campos de trazabilidad) actualizados en los 5 motores + script `upgrade`.
- [ ] Prueba de resiliencia offline: 50 ventas registradas sin conexión se sincronizan sin duplicados ni pérdida de datos al restablecer red.
- [ ] Cuadratura verificada de centavos/impuestos entre ticket de `@vendese`, pre-cuenta de `OmniGastro`, asiento en `OmniLedger` y métrica en `OmniBI`.
- [ ] Documentación actualizada (`README.md`, `ARCHITECTURE.md` y `docs/plans/ACTIVE.md`).

## 5. Evidencia
- Commits: *(pendiente)*
- Tests / validaciones ejecutadas: *(pendiente)*