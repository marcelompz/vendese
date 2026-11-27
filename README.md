# @vendese — Manual de Instalación (v3.82)

Fork de **uniCenta oPOS** mantenido por **Marcelo Pesallaccia** (`@marcelompz`) — https://github.com/marcelompz/vendese

- **Proyecto original:** uniCenta oPOS por Jack Gerrard (uniCenta) — http://www.unicenta.com  
- **Comunidad original:** http://www.sourceforge.net/projects/unicentaopos  
- **Licencia:** Este fork conserva la misma licencia que el proyecto original, **GNU General Public License v3**. Véase [LICENSE.md](LICENSE.md).

---

## 1. Requisitos del sistema

### 1.1 Software obligatorio (ejecución)
- **Sistema operativo:** Windows, Linux o macOS.
- **Java:** **JRE o JDK 8** (compilado con compatibilidad Java 7 / target `1.7`; probado con Eclipse Adoptium / Temurin 8).
  - En **Windows**, los scripts `start.bat` y `configure.bat` buscan `javaw.exe` en este orden:
    1. Variable de entorno `JAVA_HOME` (`%JAVA_HOME%\bin\javaw.exe`).
    2. Variable `PATH`.
    3. `C:\tools\jdk8*`.
    4. `%ProgramFiles%\Eclipse Adoptium\jdk-8*` o `%ProgramFiles(x86)%\Eclipse Adoptium\jdk-8*`.
  - **Nota sobre periféricos serie (RXTX) en Windows:** las librerías nativas incluidas en `lib/Windows/i368-mingw32` (`rxtxSerial.dll`, `rxtxParallel.dll`) son de 32 bits. Si va a utilizar dispositivos por puerto serie/paralelo mediante RXTX en Windows, utilice una JVM de 32 bits (x86).

### 1.2 Software adicional (solo para compilar desde el código fuente)
- **JDK 8** completo (con `javac`).
- **Apache Ant** (`ant`).
- **Inno Setup 6** (solo si desea generar el instalador `.exe` de Windows).

### 1.3 Motores de base de datos
Para uso habitual en un equipo local o red local, los motores recomendados son:

| Motor | Archivo en `lib/` | Uso recomendado |
|---|---|---|
| **Apache Derby Embedded** *(por defecto)* | `derby.jar` | Instalación rápida en **un solo terminal** (sin instalar servidor de base de datos). |
| **MySQL** *(o MariaDB)* | `mysql-connector-java-5.1.26-bin.jar` | **Servidor local o múltiples terminales** en red; ligero, fácil de respaldar en vivo y administrar. |

> *Otros motores soportados:* El proyecto también incluye controladores para **PostgreSQL** (`postgresql-9.2-1003.jdbc4.jar`), **Apache Derby Client/Server** (`derbyclient.jar`), **Oracle 11g Express** (`ojdbc6.jar`) y **HSQLDB** (`hsqldb.jar`, *no recomendado* por un error conocido con campos BLOB indicado en `README_First.txt`).

---

## 2. Métodos de instalación

Elija el método que corresponda según su entorno y artefacto disponible.

### Opción A: Instalador de Windows (`unicentaopos_3.82_windows_setup.exe`)
1. Ejecute el archivo `unicentaopos_3.82_windows_setup.exe`.
2. Siga el asistente:
   - No requiere permisos de administrador (`PrivilegesRequired=lowest`).
   - Ruta de instalación por defecto: `%LOCALAPPDATA%\uniCenta oPOS`.
   - Opcionalmente marque la creación del acceso directo en el Escritorio.
3. Al finalizar, puede marcar **"Run configuration before first use"** para abrir el asistente de configuración inicial (`configure.bat`).

> **Instalación silenciosa (desatendida):**
> ```cmd
> unicentaopos_3.82_windows_setup.exe /VERYSILENT /SUPPRESSMSGBOXES /NORESTART
> ```

### Opción B: Paquete binario portátil (`unicentaopos_3.82_bin.zip`) — Windows, Linux y macOS
1. Descargue o genere el archivo `unicentaopos_3.82_bin.zip`.
2. Descomprímalo en el directorio de destino deseado:
   - Ejemplo Windows: `C:\vendese\`
   - Ejemplo Linux/macOS: `/opt/vendese/`
3. Verifique que en la raíz del directorio existan:
   - `unicentaopos.jar`
   - Carpetas `lib/`, `locales/`, `reports/`, `licensing/`
   - Scripts de arranque y configuración (`start.bat`, `configure.bat`, `start.sh`, `configure.sh`)
4. En **Linux y macOS**, asigne permisos de ejecución a los scripts:
   ```bash
   chmod +x start.sh configure.sh
   ```

### Opción C: Compilación desde el código fuente
1. Clone o descargue el repositorio:
   ```bash
   git clone https://github.com/marcelompz/vendese.git
   cd vendese
   ```
2. Compile, ejecute las pruebas unitarias y genere los paquetes de distribución:
   ```bash
   ant -f build.xml clean test dist
   ```
   Esto generará en `build/`:
   - `build/jar/unicentaopos.jar`
   - `build/dist/unicentaopos_3.82_bin.zip` (paquete listo para usar)
   - `build/dist/unicentaopos_3.82_src.zip` (código fuente empaquetado)
3. *(Opcional, solo Windows)* Para generar el instalador `.exe` con Inno Setup 6:
   ```cmd
   ant -f build.xml installer.windows -Dinnosetup="C:\Program Files (x86)\Inno Setup 6"
   ```
   El ejecutable quedará en `build/dist/unicentaopos_3.82_windows_setup.exe`.
4. Para ejecutar directamente desde el directorio del proyecto tras compilar, copie `build/jar/unicentaopos.jar` a la raíz del proyecto o utilice el contenido descomprimido de `build/dist/unicentaopos_3.82_bin.zip`.

---

## 3. Preparación de la base de datos

### 3.1 Si usa Apache Derby Embedded (opción por defecto)
No requiere instalación ni pasos previos. En el primer arranque la aplicación creará automáticamente la base de datos embebida en:
- **Windows:** `%USERPROFILE%\unicentaopos-database`
- **Linux/macOS:** `$HOME/unicentaopos-database`

### 3.2 Si usa MySQL (recomendado para servidor local o múltiples cajas)
Antes de iniciar `@vendese`, instale MySQL (o MariaDB) y cree una **base de datos vacía** y un usuario con privilegios sobre ella. No cree las tablas manualmente: la aplicación lo hará en el primer arranque.

```sql
CREATE DATABASE unicentaopos CHARACTER SET utf8 COLLATE utf8_general_ci;
CREATE USER 'vendese'@'localhost' IDENTIFIED BY 'tu_password_segura';
GRANT ALL PRIVILEGES ON unicentaopos.* TO 'vendese'@'localhost';
FLUSH PRIVILEGES;
```
*(Si conectará otras cajas por red, reemplace `'localhost'` por `'%'` o la subred correspondiente y habilite el puerto `3306` en el firewall).*

> *Nota para MySQL 8+:* el conector incluido (`mysql-connector-java-5.1.26-bin.jar`) es de la serie 5.1; si usa MySQL 8+, verifique el método de autenticación del usuario (`mysql_native_password`) o coloque un conector JDBC más reciente compatible con Java 8 dentro de `lib/`.

---

## 4. Configuración inicial

Antes de abrir el punto de venta por primera vez, ejecute la herramienta de configuración:
- **Windows:** doble clic en `configure.bat` (o desde el Menú Inicio: *uniCenta oPOS Configuration*).
- **Linux / macOS:**
  ```bash
  ./configure.sh
  ```

La configuración se guarda en el archivo `unicentaopos.properties` dentro del directorio personal del usuario:
- **Windows:** `%USERPROFILE%\unicentaopos.properties`
- **Linux / macOS:** `$HOME/unicentaopos.properties`

*(También puede pasar una ruta alternativa de archivo `.properties` como argumento a `start.bat` o `start.sh`).*

### Pestañas principales a revisar
1. **Base de datos (`Database`):**
   - Seleccione el motor en el desplegable (`Apache Derby Embedded` o `MySQL`; también figuran otros motores compatibles como `PostgreSQL`).
   - Al elegir un motor se autocompletan los campos recomendados:
     - **Librería del driver (`db.driverlib`):** ruta al `.jar` dentro de `lib/`.
     - **Clase del driver (`db.driver`):** p. ej. `org.apache.derby.jdbc.EmbeddedDriver` o `com.mysql.jdbc.Driver`.
     - **URL (`db.URL`):** p. ej. `jdbc:mysql://localhost:3306/unicentaopos` en MySQL.
   - Ingrese **Usuario** y **Contraseña** de la base de datos.
   - Pulse el botón **Test** para confirmar que la conexión es exitosa.
   - *Nota de seguridad:* Por defecto solo se permite cargar drivers JDBC ubicados dentro del directorio de la aplicación (`lib/`). Si necesita usar un driver externo del sistema, agregue `db.driverlib.allowexternal=true` en `unicentaopos.properties`.
2. **General:**
   - Verifique el **Nombre de máquina** (`machine.hostname`, identifica a esta caja en cierres y propiedades).
   - Modo de pantalla (`window` o `fullscreen`), bolsa de tickets (`standard`, `simple` o `restaurant`) y apariencia visual (Look & Feel).
3. **Idioma / Locale:**
   - El idioma predeterminado de esta distribución es **español (`es_ES`)**. Puede cambiarlo aquí junto con los formatos numéricos, de fecha y moneda.
4. **Periféricos y Configuración de Ticket:**
   - Configure impresora de tickets (`screen` por defecto para pruebas en pantalla, o impresora del sistema / ESC/POS / JavaPOS), visor de cliente, cajón portamonedas, báscula y lector de códigos.
5. **Pagos:**
   - Método/pasarela (`external` por defecto) y lector de banda magnética si aplica.
6. **Sistema:**
   - Opción de instancia única (`machine.uniqueinstance`) para evitar abrir dos veces la aplicación en el mismo equipo.
7. **Actualizaciones:**
   - Desactivadas por defecto (`update.enabled=false`). Si las habilita, consultan lanzamientos mediante HTTPS.

Guarde los cambios y cierre la ventana de configuración.

---

## 5. Primer arranque y creación del esquema

1. Inicie la aplicación:
   - **Windows:** `start.bat` (o acceso directo *uniCenta oPOS*).
   - **Linux / macOS:**
     ```bash
     ./start.sh
     ```
2. Al conectarse a una base de datos vacía, el sistema mostrará un aviso indicando que no existe una base de datos inicializada y preguntará si desea crearla. Seleccione **Sí**.
3. Se ejecutará el script de creación correspondiente al motor (`*-create.sql`), generando las tablas, roles, usuarios y plantillas iniciales.

### Usuarios iniciales
El sistema crea cuatro usuarios sin contraseña por defecto:
- **Administrator** *(Rol Administrador — acceso completo)*
- **Manager** *(Rol Gerente)*
- **Employee** *(Rol Empleado)*
- **Guest** *(Rol Invitado)*

### Primeros pasos obligatorios tras el arranque
1. **Asignar contraseña al usuario `Administrator`:** ingrese con `Administrator` y configure una contraseña en *Mantenimiento → Personas* (las contraseñas nuevas se almacenan protegidas con PBKDF2-HMAC-SHA256).
2. **No eliminar los registros predeterminados** (advertencia crítica heredada de uniCenta oPOS):
   - Categoría: `CATEGORY STANDARD`
   - Producto con referencia: `xxx999`
   - Impuestos: `TAX EXEMPT` y `TAX STANDARD`
   > Puede **renombrarlos** según su negocio, pero **no los elimine**.
3. Configure sus impuestos, categorías, productos, logotipo y plantillas de ticket en *Mantenimiento → Recursos*.
4. Realice su primera copia de seguridad.

---

## 6. Actualización y migración

### 6.1 Actualización desde versiones anteriores
- Las migraciones de esquema en `@vendese` / uniCenta oPOS son **estrictamente secuenciales** (`3.70` → `3.80` → `3.81` → `3.82`).
- Haga siempre una **copia de seguridad completa** de la base de datos antes de actualizar.
- Al iniciar la nueva versión contra una base de datos de una versión compatible anterior, la aplicación detectará la versión en la tabla `APPLICATIONS` y solicitará confirmación para ejecutar el script de actualización (`*-upgrade-<versión>.sql`).

### 6.2 Migración entre motores de base de datos
Para migrar datos (por ejemplo, de Derby Embedded a MySQL):
- **Windows:** ejecute `Migrate.bat`.
- **Linux/macOS:** la herramienta gráfica corresponde a la clase `com.openbravo.pos.migrate.JFrmMigrate` (nota: `migrate.sh` contiene errores de formato en esta versión; ejecute `java` usando el mismo `CLASSPATH` de `start.sh` apuntando a `com.openbravo.pos.migrate.JFrmMigrate`).

---

## 7. Copias de seguridad (archivos críticos)

Además de respaldar la base de datos (`~/unicentaopos-database` en Derby Embedded, o mediante `mysqldump` en MySQL), respalde los siguientes archivos del directorio personal del usuario (`%USERPROFILE%` en Windows / `$HOME` en Linux y macOS):

- `unicentaopos.properties` — configuración de la terminal y credenciales cifradas.
- `.unicentaopos.key` — clave local AES-128-GCM utilizada para descifrar las contraseñas guardadas (`crypt:`). **Sin este archivo los secretos cifrados no se pueden recuperar.**
- `.unicentaopos.instance` — token de instancia local.

---

## 8. Solución de problemas frecuentes

- **Error `No se encontro Java 8 (javaw.exe)` en Windows:**
  Instale JRE/JDK 8 (por ejemplo Eclipse Adoptium Temurin 8) o configure la variable de entorno `JAVA_HOME` apuntando a su instalación de Java 8.
- **Error `Database driver library is outside the application directory`:**
  Coloque el `.jar` del controlador JDBC dentro de la carpeta `lib/` de la aplicación y vuelva a seleccionarlo en Configuración, o agregue `db.driverlib.allowexternal=true` en `unicentaopos.properties`.
- **La aplicación no abre o se cierra de inmediato:**
  Verifique si ya hay otra instancia ejecutándose (`machine.uniqueinstance=true` utiliza RMI en `127.0.0.1:1099`) o ejecute `start.bat` / `./start.sh` desde una terminal para ver el detalle de la excepción en consola.
- **Error de acceso a puertos serie (impresora/báscula) en Linux:**
  Asegúrese de que su usuario pertenezca al grupo con permisos sobre `/dev/ttyS*` o `/dev/ttyUSB*` (habitualmente `dialout` o `uucp`) y que `start.sh` resuelva su arquitectura en `lib/Linux/`.
- **Error con HSQLDB al abrir Ventas:**
  Tal como indica `README_First.txt`, el driver HSQLDB 2.x falla al leer campos BLOB. Utilice Derby Embedded o MySQL.

---

**Autor del fork:** Marcelo Pesallaccia (`@marcelompz`) — https://github.com/marcelompz/vendese  
**Basado en:** uniCenta oPOS por Jack Gerrard
