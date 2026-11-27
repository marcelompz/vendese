# PLAN-INFRA-UPGRADE-RUNTIME-BD — Actualización de Java y motores MariaDB / PostgreSQL

- **ID:** `PLAN-INFRA-UPGRADE-RUNTIME-BD`
- **Dominio:** `infra`
- **Estado:** `borrador`
- **Versión objetivo:** `3.84.0` (reservada según el criterio `MAYOR.FEATURE.PATCH` de [`ROADMAP.md`](../../../ROADMAP.md))
- **Fecha:** `2026-11-27`

## 1. Objetivo y alcance
Modernizar el stack base de ejecución y persistencia de `@vendese` (heredado de rama 3.82) para soportar **Java 17/21 LTS**, **MariaDB 10.11+ / 11.x** y **PostgreSQL 15+** sin reescribir la lógica de negocio ni migrar hacia ramas externas.

**Incluye:**
- Compilación y ejecución estable bajo JDK 17/21 LTS (resolviendo flags de encapsulamiento/módulos y escalado Swing/HiDPI).
- Actualización de drivers JDBC (`mariadb-java-client` / `mysql-connector-j` y `postgresql` con soporte nativo `scram-sha-256`).
- Ajustes de compatibilidad en `SessionDBPostgreSQL` / `SessionDBMySQL`, dialectos SQL (`sql_mode` estricto) y reportes JasperReports `.jasper`.
- Scripts de arranque (`start.sh` / `start.bat`) y validación de configuración `db.driverlib`.

**Fuera de alcance:**
- Refactorización de *God classes* o migración a `SwingWorker` (cubierto en `PLAN-MEJORA-FASES-4-CALIDAD`).
- Parametrización masiva de `QBFBuilder` / `NormalBuilder` (cubierto en `PLAN-MEJORA-FASE-5-DATOS`).
- Arquitectura multi-tenant SaaS (cubierto en `PLAN-SAAS-POSTGRESQL`).

## 2. Archivos / módulos afectados
- `build.xml` y `build_working.xml` (target/source Java, classpath de librerías JDBC y compilación).
- `lib/` (reemplazo/actualización de `postgresql-*.jar` y `mariadb-java-client-*.jar` o conector MySQL compatible).
- `src-data/com/openbravo/data/loader/Session.java`, `SessionDBPostgreSQL.java`, `SessionDBMySQL.java` (manejo de conexiones, tipos y metadatos).
- `src-pos/com/openbravo/pos/forms/AppConfig.java` y `StartPOS.java` (carga dinámica de `db.driverlib`, validación de versión JVM y flags de renderizado).
- `start.sh` y `start.bat` (opciones `--add-opens` / `--add-exports` si aplican para Swing/JasperReports y parámetros HiDPI).
- `reports/com/openbravo/reports/*.jrxml` / `*.jasper` (recompilación/validación contra la nueva JVM).
- `test/` (nuevos tests de integración de conexión y compatibilidad de esquema).

## 3. Pasos de implementación
1. **Auditoría de build y runtime Java 17/21:**
   - Actualizar propiedades `javac` (`source`/`target` o `--release`) en `build.xml` y `build_working.xml`.
   - Ejecutar compilación limpia y registrar advertencias o errores por APIs eliminadas en Java 9+ (JAXB, CORBA, JavaFX o accesos reflexivos de Swing/Jasper).
   - Definir los flags JVM mínimos necesarios en `start.sh` y `start.bat`.
2. **Actualización de drivers JDBC y conexión:**
   - Incorporar drivers JDBC modernos para PostgreSQL (compatible con `scram-sha-256` y TLS moderno) y MariaDB 10.11+/11.x.
   - Validar el mecanismo de carga de `db.driverlib` respetando las reglas de seguridad de `PLAN-MEJORA-FASES-0-3`.
3. **Verificación de dialectos SQL en MariaDB y PostgreSQL modernos:**
   - Levantar instancias limpias de MariaDB 11.x (con `sql_mode` estricto por defecto) y PostgreSQL 16/17.
   - Ejecutar scripts de creación (`MySQL-create.sql`, `PostgreSQL-create.sql`) y scripts de `upgrade`, corrigiendo valores por defecto inválidos (`0000-00-00`), longitudes de índices `utf8mb4` o casts estrictos.
4. **Validación funcional de UI, periféricos y reportes:**
   - Probar flujo completo en VM/entorno de pruebas: apertura de caja, ventas, impresión de factura preimpresa (`PLAN-impresion-factura-preimpresa-vendese-2026-11-27`), cierre Z y generación de reportes Jasper.
5. **Pruebas automatizadas:**
   - Añadir tests JUnit que verifiquen conexión JDBC, lectura/escritura básica de `RESOURCES`, `PRODUCTS` y `TICKETS` sobre los motores actualizados, registrándolos en `build_working.xml`.

## 4. Criterios de aceptación (DoD)
- [ ] Compilación y tests pasando bajo JDK 17/21 (`ant -f build.xml clean test dist`).
- [ ] Conexión exitosa contra PostgreSQL 15+ (con `scram-sha-256`) y MariaDB 10.11+ sin degradar políticas de autenticación del servidor.
- [ ] Nuevas clases `*Test` registradas en `build_working.xml`.
- [ ] Scripts SQL actualizados en los 5 motores + upgrade (si se requieren ajustes de compatibilidad de esquema/tipos).
- [ ] Documentación actualizada (`README.md` con requisitos mínimos de Java/BD, `ARCHITECTURE.md` y `docs/plans/ACTIVE.md`).

## 5. Evidencia
- Commits: *(pendiente)*
- Tests / validaciones ejecutadas: *(pendiente)*