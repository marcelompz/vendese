# PLAN-INFRA-UPGRADE-RUNTIME-BD — Actualización de Java y motores MariaDB / PostgreSQL

- **ID:** `PLAN-INFRA-UPGRADE-RUNTIME-BD`
- **Dominio:** `infra`
- **Estado:** `cerrado` el 2026-11-28
- **Versión objetivo:** `3.84.0` (registro de entrega según el criterio `MAYOR.FEATURE.PATCH` de [`ROADMAP.md`](../../../ROADMAP.md); no implica bump de `AppLocal.APP_VERSION`)
- **Fecha:** 2026-11-28

## 1. Objetivo y alcance
Modernizar el stack base de ejecución y persistencia de `@vendese` (heredado de la rama 3.82) para soportar **JDK 8 y JDK 17 LTS** con bytecode Java 7 y **drivers JDBC modernos** para MariaDB 10.11+/11.x y PostgreSQL 15+, sin reescribir la lógica de negocio ni migrar hacia ramas externas.

**Incluido en el cierre:**
- Compilación y ejecución estable bajo JDK 8 y JDK 17 LTS, manteniendo `source/target 1.7`.
- Actualización de drivers JDBC: `postgresql-42.7.7.jar`, `mariadb-java-client-3.5.3.jar` y `mysql-connector-j-8.4.0.jar`; se conserva el conector MySQL 5.1 legacy.
- Compatibilidad de JasperReports 4.5.1 con JDK 9+ mediante un archivo de propiedades propio y compilación de expresiones con `javac` externo.
- Arranque con `--add-opens` condicionales en `start.sh` / `start.bat` y validación de la carga de `db.driverlib`.
- Validación y despliegue en la VM Windows 10 con el `HEAD` exacto publicado.

**Diferido / fuera de alcance en este cierre:**
- **JDK 21:** no se incorpora al build porque `javac` 21 rechaza `source/target 1.7`; se mantiene la invariante de bytecode Java 7.
- **Conexiones reales a servidores MariaDB/PostgreSQL y pruebas de dialecto contra instancias limpias:** no se ejecutaron en esta entrega; queda para el plan SaaS/datos.
- Refactorización de *God classes* o migración a `SwingWorker` (`PLAN-MEJORA-FASES-4-CALIDAD`).
- Parametrización masiva de `QBFBuilder` / `NormalBuilder` (`PLAN-MEJORA-FASE-5-DATOS`).
- Arquitectura multi-tenant SaaS (`PLAN-SAAS-POSTGRESQL`).
- Cambios de esquema SQL: no hubo, por lo que no se añaden scripts `*-upgrade-*.sql`.

## 2. Archivos / módulos afectados
- `.github/workflows/ci.yml`: matriz de build JDK 8 y JDK 17.
- `build_working.xml`: registro de `JdbcDriverCompatibilityTest`.
- `lib/`: drivers PostgreSQL 42.7.7, MariaDB 3.5.3 y MySQL Connector/J 8.4.0.
- `src-pos/com/openbravo/pos/forms/AppConfig.java`: ejemplos de `db.driverlib`, `db.driver` y `db.URL`.
- `src-pos/com/openbravo/pos/forms/StartPOS.java` y `src-pos/com/openbravo/pos/reports/JasperReportLoader.java`: selección de la configuración Jasper de `@vendese`.
- `src-pos/vendese-jasperreports.properties`: compilador `javac` externo para JasperReports 4.5.1.
- `src-pos/com/openbravo/pos/payment/PaymentGatewayPayPoint.java`: retirada del proveedor SSL interno eliminado en JDK 9+.
- `start.sh` y `start.bat`: detección JDK 8/17, `--add-opens` condicionales y `javac` en `PATH`.
- `test/com/openbravo/pos/forms/JdbcDriverCompatibilityTest.java`: 5 tests de drivers y contratos de URL.
- `README.md` y `ARCHITECTURE.md`: requisitos, compatibilidad y conteo de tests.

## 3. Pasos ejecutados
1. **Build y runtime:** se mantuvo `source/target 1.7`; CI pasó a matriz JDK 8/17; se eliminó la referencia al proveedor SSL interno y se añadieron `--add-opens` solo cuando la JVM los admite.
2. **JDBC:** se añadieron los tres drivers modernos, se conservó el legacy y se corrigió el contrato de URL (`jdbc:mariadb://` para MariaDB, `jdbc:mysql://` para MySQL).
3. **Reportes:** se aisló `vendese-jasperreports.properties` para evitar que otros JARs shadowen la configuración genérica; Jasper usa `javac` del JDK seleccionado en los scripts de arranque.
4. **Pruebas:** se añadió `JdbcDriverCompatibilityTest` y se registró en `build_working.xml`; la suite quedó en 17 clases / 76 tests.
5. **VM y despliegue:** se reconstruyó el código desde el `HEAD` final en Windows 10 con JDK 8 y JDK 17, se copió el JAR y los recursos a `C:\unicenta` y se reinició el POS en sesión interactiva con JDK 17.

## 4. Criterios de aceptación (DoD)
- [x] Compilación y tests pasando bajo JDK 8 y JDK 17 (`ant -f build.xml clean test dist`).
- [x] Drivers modernos incluidos y contratos de URL validados por tests JUnit.
- [x] Nueva clase `*Test` registrada en `build_working.xml`.
- [x] Sin cambios de esquema: no corresponde script `*-upgrade-*.sql`.
- [x] Documentación actualizada (`README.md`, `ARCHITECTURE.md`, `AGENTS.md` y `docs/plans/ACTIVE.md`).
- [x] Despliegue y arranque verificados en la VM Windows 10.
- [ ] JDK 21 y conexiones a servidores reales: **diferidos documentalmente**, no forman parte del alcance cerrado.

## 5. Evidencia
- **Commits de implementación:** `52af3e2`, `5cf969e`, `e52acec`, `a5a7a94`, `e772d2e`.
- **Validación JDK 8 en VM:** `C:\temp\infra_build_jdk8.log` → `openjdk version "1.8.0_512"`, `[java] OK (76 tests)`, `BUILD SUCCESSFUL`, `ANT_RC=0`.
- **Validación JDK 17 en VM:** `C:\temp\infra_build_jdk17.log` → `openjdk version "17.0.21.1"`, `[java] OK (76 tests)`, `BUILD SUCCESSFUL`, `ANT_RC=0`.
- **Fuente validada:** `vendese-infra-final.tar.gz`, SHA256 `684f29c60728d33262f3873743ea1282c89c8eedd1ccecbccbda1c7d84054b5b`, extraída desde el `HEAD` final antes de ejecutar ambos builds.
- **Despliegue:** `C:\temp\infra_deploy.out` → `DEPLOY_DONE`; JAR copiado a `C:\unicenta\unicentaopos.jar`, respaldo `unicentaopos.jar.pre-infra`, tres drivers nuevos en `C:\unicenta\lib\`, scripts y README actualizados; `jar tf` confirma `vendese-jasperreports.properties` dentro del JAR.
- **Runtime en VM:** PID 3024 en `console` (sesión 1), `C:\tools\jdk17\bin\javaw.exe` con `--add-opens java.base/java.lang`, `java.base/java.util` y `java.desktop/java.awt`; `AppActivate("uniCenta oPOS - 3.82")` → `True`.
- **Restricciones respetadas:** sin bump de `AppLocal.APP_VERSION` (sigue en `3.82`), sin cambios de esquema, sin impresión sobre papel preimpreso real.
