package com.openbravo.pos.forms;

import static org.junit.Assert.*;

import java.io.File;
import java.sql.Driver;
import org.junit.Test;

/**
 * Verifies that the JDBC drivers shipped in lib/ can be loaded by the current
 * JVM and still accept the URL schemes used by @vendese. The bytecode stays
 * Java 7 compatible; the same tests run on JDK 8 and JDK 17 LTS.
 */
public class JdbcDriverCompatibilityTest {

    private static void assertDriver(String className, String url) throws Exception {
        Driver driver = (Driver) Class.forName(className).newInstance();
        assertTrue(className + " must accept " + url, driver.acceptsURL(url));
    }

    @Test
    public void modernPostgresDriverSupportsScramSha256Urls() throws Exception {
        assertTrue(new File("lib/postgresql-42.7.7.jar").isFile());
        assertDriver("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/unicentaopos");
    }

    @Test
    public void modernMariaDbDriverSupportsMariaDbUrls() throws Exception {
        assertTrue(new File("lib/mariadb-java-client-3.5.3.jar").isFile());
        assertDriver("org.mariadb.jdbc.Driver", "jdbc:mariadb://localhost:3306/unicentaopos");
    }

    @Test
    public void modernMySqlDriverSupportsCachingSha2Urls() throws Exception {
        assertTrue(new File("lib/mysql-connector-j-8.4.0.jar").isFile());
        assertDriver("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/unicentaopos");
    }

    @Test
    public void legacyMySqlDriverIsKeptForExistingInstallations() throws Exception {
        assertTrue(new File("lib/mysql-connector-java-5.1.26-bin.jar").isFile());
        assertDriver("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/unicentaopos");
    }

    @Test
    public void derbyEmbeddedDriverRemainsTheDefault() throws Exception {
        assertTrue(new File("lib/derby.jar").isFile());
        assertDriver("org.apache.derby.jdbc.EmbeddedDriver",
                "jdbc:derby:target/compat-db;create=true");
    }
}
