package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Administrador JDBC sencillo para procesos Java standalone.
 *
 * La configuración y el driver se cargan una sola vez por instancia.
 * Cada llamada a openConnection crea una conexión física nueva; quien la
 * solicite es responsable de cerrarla.
 */
public final class QuickJdbcConnectionManager {

    private static final String DEFAULT_PROPERTIES_PATH = "/u01/Informatica/server.properties";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private final JdbcConfig jdbcConfig;

    public QuickJdbcConnectionManager() {
        this(resolveServerPropertiesPath());
    }

    public QuickJdbcConnectionManager(Path propertiesPath) {
        try {
            this.jdbcConfig = loadJdbcConfig(propertiesPath);
            Class.forName(jdbcConfig.jdbcDriver);
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("No fue posible inicializar la configuración JDBC", e);
        }
    }

    public Connection openConnection(boolean autoCommit) throws SQLException {
        Connection connection = DriverManager.getConnection(
                jdbcConfig.jdbcUrl,
                jdbcConfig.user,
                jdbcConfig.password);

        boolean success = false;
        try {
            connection.setAutoCommit(autoCommit);
            success = true;
            return connection;
        } finally {
            if (!success) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                    // Se conserva la excepción original.
                }
            }
        }
    }

    private static JdbcConfig loadJdbcConfig(Path propertiesPath) throws IOException {
        Properties raw = new Properties();
        try (InputStream in = Files.newInputStream(propertiesPath)) {
            raw.load(in);
        }

        return new JdbcConfig(
                resolveRequiredProperty(raw, "db.master.pool.jdbcDriver"),
                resolveRequiredProperty(raw, "db.master.pool.jdbcUrl"),
                resolveRequiredProperty(raw, "db.master.user"),
                resolveRequiredProperty(raw, "db.master.password"));
    }

    private static Path resolveServerPropertiesPath() {
        String path = System.getenv("P360_SERVER_PROPERTIES");
        if (path == null || path.trim().isEmpty()) {
            path = DEFAULT_PROPERTIES_PATH;
        }

        Path resolved = Paths.get(path).toAbsolutePath().normalize();
        if (!Files.exists(resolved)) {
            throw new IllegalArgumentException("No existe server.properties en: " + resolved);
        }
        if (!Files.isRegularFile(resolved)) {
            throw new IllegalArgumentException("La ruta no es archivo: " + resolved);
        }
        return resolved;
    }

    private static String resolveRequiredProperty(Properties raw, String key) {
        String value = resolvePropertyValue(raw, key, new HashSet<>());
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("No se encontró la property requerida: " + key);
        }
        return value.trim();
    }

    private static String resolvePropertyValue(Properties raw, String key, Set<String> visiting) {
        if (!visiting.add(key)) {
            throw new IllegalArgumentException(
                    "Referencia circular detectada en properties para la clave: " + key);
        }

        try {
            String value = raw.getProperty(key);
            if (value == null) {
                return null;
            }

            Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
            StringBuffer resolved = new StringBuffer();

            while (matcher.find()) {
                String referencedKey = matcher.group(1);
                String referencedValue = resolvePropertyValue(raw, referencedKey, visiting);
                if (referencedValue == null) {
                    throw new IllegalArgumentException(
                            "No se pudo resolver la property referenciada: " + referencedKey);
                }
                matcher.appendReplacement(resolved, Matcher.quoteReplacement(referencedValue));
            }

            matcher.appendTail(resolved);
            return resolved.toString();
        } finally {
            visiting.remove(key);
        }
    }

    private static final class JdbcConfig {
        private final String jdbcDriver;
        private final String jdbcUrl;
        private final String user;
        private final String password;

        private JdbcConfig(String jdbcDriver, String jdbcUrl, String user, String password) {
            this.jdbcDriver = jdbcDriver;
            this.jdbcUrl = jdbcUrl;
            this.user = user;
            this.password = password;
        }
    }
}