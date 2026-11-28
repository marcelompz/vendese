# AGENTS.md — @vendese

> Protocolo operativo compacto para agentes y colaboradores. Antes de cambios no triviales, lee [`ARCHITECTURE.md`](ARCHITECTURE.md) (mapa completo del código) e [`IMPROVEMENT_PLAN.md`](docs/plans/IMPROVEMENT_PLAN.md) (fases previas 0–5).
> **@vendese** = marca/fork público ([`marcelompz/vendese`](https://github.com/marcelompz/vendese), licencia GPL v3 en [`LICENSE.md`](LICENSE.md)). Internamente el código conserva los paquetes `com.openbravo.*` e identificador `unicentaopos` (`AppLocal.APP_ID`).

## Stack y estructura (Java + Apache Ant)

- **Lenguaje y build:** Java (`source/target 1.7`, compilado y validado en CI con **JDK 8 y JDK 17 Temurin**) + **Apache Ant** (`build.xml` importa `build_working.xml`; no usar `build_original.xml`). JDK 21 queda fuera hasta abandonar `source 7`.
- **Módulos fuente** (se compilan **juntos** en un único `javac` porque `src-beans` referencia clases de `src-pos`):
  - `src-beans/` — widgets táctiles, `Formats`, `LocaleResources`.
  - `src-data/` — micro-ORM propio (`Session`, `SessionDB*`, `BaseSentence`, `BrowsableEditableData`).
  - `src-pos/` — aplicación POS (`forms`, `sales`, `ticket`, `payment`, `printer`, `inventory`, `customers`, `admin`, `epm`, `reports`, `updates`, `scripting`, `scripts/`, `templates/`).
  - `test/` — 17 suites JUnit 4 ejecutadas mediante `org.junit.runner.JUnitCore` (76 tests; sin depender de `ant-junit.jar`).
- **Recursos y empaquetado:**
  - `lib/` — dependencias JAR y librerías nativas RXTX (`lib/Linux/`, `lib/Windows/i368-mingw32/`, `lib/Mac_OS_X/`).
  - `locales/` — 16 idiomas; idioma enviado por defecto: **español (`es_ES`)**.
  - `reports/` — plantillas Jasper (`.jrxml`), definiciones BeanShell (`.bs`) y bundles `.properties`.
  - `installer/unicentaopos.iss` — instalador Windows sin elevación con Inno Setup 6.
- **Estado en runtime (directorio personal `$HOME` / `%USERPROFILE%`):**
  - `unicentaopos.properties` (configuración), `.unicentaopos.key` (clave local AES-128-GCM v2), `.unicentaopos.instance` (token RMI), `unicentaopos-database/` (Derby Embedded), `.unicentaopos-updates/`.

## Comandos verificados

```bash
# Compilar, ejecutar tests JUnit y generar ZIPs de distribución (build/dist/*.zip)
ant -f build.xml clean test dist

# Solo compilar y ejecutar las pruebas unitarias
ant -f build.xml test

# Solo generar JAR (build/jar/unicentaopos.jar)
ant -f build.xml jar

# Release completa en Windows (requiere Inno Setup 6)
ant -f build.xml installer.windows -Dinnosetup="C:\Program Files (x86)\Inno Setup 6"
ant -f build.xml release

# Ejecución local (requiere unicentaopos.jar en la raíz o usar build/dist/*_bin.zip)
./configure.sh          # Linux/macOS (Windows: configure.bat)
./start.sh              # Linux/macOS (Windows: start.bat)
```

- **CI (`.github/workflows/ci.yml`):**
  - Job `build` (`ubuntu-latest`, matriz JDK 8 y JDK 17 + Ant): `ant -f build.xml clean test dist` → sube `build/dist/*.zip` por versión de JDK.
  - Job `installer-windows` (`windows-latest`, JDK 8 + Ant + Inno Setup): `ant -f build.xml installer.windows -Dinnosetup="C:\Program Files (x86)\Inno Setup 6"` → sube `build/dist/*setup.exe`.

## Invariantes de arquitectura y seguridad (no romper)

1. **Compilación unificada y compatibilidad Java 7:** mantener `source/target 1.7`, `encoding="UTF-8"` y no introducir dependencias de NetBeans (`nbproject/` no existe).
2. **Fronteras de seguridad (Fases 1–3):**
   - **Contraseñas de usuario:** PBKDF2-HMAC-SHA256 (`PasswordHasher` / `Hashcypher`); conservar lectura de formatos legacy (`sha1:`, `plain:`, `empty:`) para migración transparente al login.
   - **Secretos de configuración (`crypt:`):** AES-128-GCM v2 (`SecureEncrypter` / `AltEncrypter` + `~/.unicentaopos.key`); 3DES queda solo para lectura legacy.
   - **Instancia única RMI (`InstanceManager`):** enlazar únicamente a `127.0.0.1:1099` y validar `InstanceToken`. Nunca exponer en `0.0.0.0`.
   - **Scripts dinámicos (`ScriptSecurity` + `BeanNameValidator`):** todo script BeanShell en `RESOURCES` se contrasta con `/com/openbravo/pos/templates/`; nunca pasar `dbPassword` real a scripts; mantener la lista blanca de clases en `BeanNameValidator`.
   - **Drivers JDBC (`AppViewConnection`):** `db.driverlib` debe apuntar dentro del directorio de la app salvo `db.driverlib.allowexternal=true` explícito.
3. **Esquemas SQL y multi-BD:**
   - Cualquier cambio de esquema debe aplicarse de forma consistente en los **5** `*-create.sql` (`Derby`, `MySQL`, `PostgreSQL`, `Oracle`, `HSQLDB`) **y** en los scripts incrementales `*-upgrade-<version_anterior>.sql` en `src-pos/com/openbravo/pos/scripts/`.
   - Preferir `PreparedSentence` (parámetros enlazados) para nuevas consultas; no agregar SQL concatenado con literales en `StaticSentence`.
   - No eliminar los datos semilla obligatorios (`CATEGORY STANDARD`, producto `xxx999`, impuestos `TAX EXEMPT` y `TAX STANDARD`).
4. **Idioma por defecto (`es_ES`):** `AppConfig.loadDefault()` y `StartPOS.ensureDefaultSpanish()` fijan español; si agregas textos de UI o reportes, actualiza también los bundles `*_es.properties`.
5. **Licencia y atribuciones:** conservar cabeceras GPL v3, [`LICENSE.md`](LICENSE.md) y la atribución del proyecto original (uniCenta oPOS / Jack Gerrard) y del fork (`@marcelompz`).

## Criterio de versionamiento y hoja de ruta

- **Formato:** `MAYOR.FEATURE.PATCH` (`3.<línea de feature>.<ajuste>`). La línea base heredada `3.82` se documenta como **`3.82.0`**.
- **Reglas:**
  - `MAYOR` (primer dígito): solo cambios incompatibles de arquitectura o API de datos; hoy `3` (pasaría a `4.0.0`).
  - `FEATURE` (segundo dígito): nueva funcionalidad relevante o «cambio mayor funcional»; al subir, el `PATCH` vuelve a `0` (`3.82.0 → 3.83.0`, impresión de factura preimpresa).
  - `PATCH` (tercer dígito): mejoras o correcciones dentro de la misma línea de feature (`3.83.0 → 3.83.1`, ajustes de esa impresión en la VM).
- **Resumen (detalle en `ROADMAP.md`):** `3.82.0` base heredada; `3.83.0` factura preimpresa (cerrada); `3.83.1` ajustes en VM (cerrados: `PDFCreator` en la VM y PDF desde `PrinterJob`; sin papel real); `3.84.0` runtime/BD (cerrado el 2026-11-28: JDK 8/17 y drivers JDBC modernos validados en VM; JDK 21 y conexiones reales diferidos); `3.85.0` Fase 4 calidad; `3.86.0` Fase 5 datos; `3.87.0` SaaS PostgreSQL (por definir).
- **Fuentes de verdad:** [`ROADMAP.md`](ROADMAP.md) y [`featurelist.json`](featurelist.json). Toda versión nueva se añade en ambos con la misma descripción, estado, fecha y plan de origen, y se refleja en [`docs/plans/ACTIVE.md`](docs/plans/ACTIVE.md) y en el `versión objetivo` del plan correspondiente.
- **No confundir:** en este proyecto «cambio mayor» es el salto de `FEATURE`, no un `MAJOR` semver. Las versiones reservadas del roadmap no cambian el código: `AppLocal.APP_VERSION` sigue en `3.82` hasta un release autorizado.
- **Proceso:** [`PLAN-versionamiento-roadmap-featurelist-2026-11-27`](docs/plans/infra/plan-versionamiento-roadmap-featurelist-2026-11-27.md).

## Sincronización de versión (obligatoria en releases)

Al preparar una nueva versión (`3.82.0` → `3.83.0`, o un ajuste `3.83.0` → `3.83.1`), actualizar **todos** estos puntos en el mismo cambio:
1. `AppLocal.APP_VERSION` en `src-pos/com/openbravo/pos/forms/AppLocal.java`.
2. `<property name="version" value="..."/>` en `build_working.xml`.
3. `#define MyAppVersion "..."` en `installer/unicentaopos.iss`.
4. Scripts `*-upgrade-<version_anterior>.sql` si hubo cambios de base de datos (`JRootApp` compara `APPLICATIONS.VERSION` con `APP_VERSION`). Para `3.83.0` el script heredado es `*-upgrade-3.82.sql`; un `PATCH` sin cambios de esquema no añade script.
5. Referencias de versión en [`README.md`](README.md) (y [`ARCHITECTURE.md`](ARCHITECTURE.md) si cambia arquitectura o conteo de tests).
6. [`ROADMAP.md`](ROADMAP.md), [`featurelist.json`](featurelist.json) y [`docs/plans/ACTIVE.md`](docs/plans/ACTIVE.md): estado, fecha y evidencia de la versión, además del plan de origen.

## Planes de trabajo y ciclo de vida documental

- **Antes de iniciar trabajo no trivial:** revisar [`docs/plans/ACTIVE.md`](docs/plans/ACTIVE.md), [`IMPROVEMENT_PLAN.md`](docs/plans/IMPROVEMENT_PLAN.md) y [`ROADMAP.md`](ROADMAP.md) para evitar duplicaciones o solapamientos.
- **Planificar cada mejora:** crear un plan en `docs/plans/<dominio>/` (por ejemplo `pos/`, `datos/`, `saas/`, `infra/`, `ui/`) con metadatos mínimos (`id`, `dominio`, `estado`, `versión objetivo`, `fecha`), objetivo, alcance, pasos, criterios de aceptación (DoD) y sección de evidencia. La `versión objetivo` usa tres componentes (`3.83.0`, `3.83.1`) y debe figurar también en `ROADMAP.md` y `featurelist.json`.
- **Estados permitidos:** `borrador → propuesto → aprobado → en_progreso → entregado → cerrado` (o `supersedido` / `archivado`). Mientras no esté cerrado, debe figurar en [`docs/plans/ACTIVE.md`](docs/plans/ACTIVE.md).
- **Cierre obligatorio sincronizado:** solo marcar como `cerrado` cuando se cumpla el DoD, pasen los tests (`ant -f build.xml clean test dist`), exista evidencia vinculada (commits/tests/validaciones) y estén sincronizados `docs/plans/ACTIVE.md`, `ROADMAP.md`/`featurelist.json` y `README.md`/`ARCHITECTURE.md` si aplica.

## Flujo de ramas y cambios

- Trabajar mejoras nuevas en ramas desde `main`: `feat/<area>-<desc>`, `fix/<area>-<desc>`, `docs/<desc>`, `chore/<desc>`.
- Commits descriptivos y atómicos; hacer `push` a `origin/main` o crear releases solo cuando el usuario lo solicite/autorice explícitamente.
- Antes de migrar o probar actualizaciones contra una base de datos real, respaldar la BD y los archivos `~/.unicentaopos.properties` y `~/.unicentaopos.key`.

## Gotchas conocidos

- **Registro manual de tests en `build_working.xml`:** crear un `*Test.java` en `test/` **no** lo ejecuta automáticamente; hay que añadir la clase como `<arg value="..."/>` en el target `test` de `build_working.xml`.
- **Classpath manual en `start.sh` / `configure.sh`:** a diferencia de `start.bat` (`lib\*`), `start.sh` enumera cada `.jar` a mano. Si agregas una librería a `lib/`, añádela también a `start.sh` (y `configure.sh` si se usa en configuración).
- **Compatibilidad Ant 1.7.1:** `build_working.xml` omite intencionalmente `includeantruntime="false"` en `<javac>`.
- **URLs heredadas en actualizador e instalador:** `UpdateSettings.DEFAULT_URL` (`src-pos/com/openbravo/pos/updates/UpdateSettings.java`) e `installer/unicentaopos.iss` (`MyAppURL`) aún apuntan a `scanby/unicenta`; al activar releases propias de `@vendese`, actualizarlos a `marcelompz/vendese`.
- **Drivers JDBC en `lib/`:** los JAR legacy (`mysql-connector-java-5.1.26-bin.jar`, `postgresql-9.2-1003.jdbc4.jar`) se conservan solo para compatibilidad; para MySQL 8 usar `mysql-connector-j-8.4.0.jar` con `com.mysql.cj.jdbc.Driver`, para MariaDB usar `mariadb-java-client-3.5.3.jar` con `org.mariadb.jdbc.Driver` / `jdbc:mariadb://...`, y para PostgreSQL moderno usar `postgresql-42.7.7.jar` con `org.postgresql.Driver`. Deben coincidir `db.driverlib`, `db.driver` y `db.URL`.
- **PostgreSQL `resetSequenceSentence`:** `SessionDBPostgreSQL.java` usa `ALTER SEQUENCE ... RESTART WITH '0'` (con comillas simples); revisar si se utiliza `ResetSeq`.
- **Windows RXTX 32-bit:** las DLL nativas en `lib/Windows/i368-mingw32` son de 32 bits; si un terminal usa puerto serie con RXTX en Windows requiere JVM x86.
- **Scripts legacy/obsoletos:** `migrate.sh` está corrupto en una sola línea (`libjasperreports` sin `/`); `build_original.xml` y `unicentaopos_*_installer.xml` (BitRock) no deben usarse.
