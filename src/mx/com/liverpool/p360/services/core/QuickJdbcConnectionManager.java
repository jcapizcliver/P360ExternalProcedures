package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuickJdbcConnectionManager {

	

	private JdbcConfig initJdbcConfig() throws IOException {
    	JdbcConfig config = new JdbcConfig();
    	Path propertiesPath = resolveServerPropertiesPath();
        Properties raw = new Properties();
        try (InputStream in = Files.newInputStream(propertiesPath))
        {
          raw.load(in);
		}
        config.jdbcDriver = resolveRequiredProperty(raw, "db.master.pool.jdbcDriver");
        config.jdbcUrl = resolveRequiredProperty(raw, "db.master.pool.jdbcUrl");
        config.user = resolveRequiredProperty(raw, "db.master.user");
        config.password = resolveRequiredProperty(raw, "db.master.password");
        return config;
    }
	
	private static Path resolveServerPropertiesPath(){
		String path = System.getenv("P360_SERVER_PROPERTIES");
		if (path == null || path.trim().isEmpty()){
			path = "/u01/Informatica/server.properties";
		}
		Path resolved = Paths.get(path).toAbsolutePath().normalize();
		if (!Files.exists(resolved)){
			throw new IllegalArgumentException("No existe server.properties en: " + resolved);
		}
		if (!Files.isRegularFile(resolved)){
			throw new IllegalArgumentException("La ruta no es archivo: " + resolved);
		}
		return resolved;
	}
		
	private static String resolveRequiredProperty(Properties raw, String key){
		String value = resolvePropertyValue(raw, key, new java.util.HashSet<String>());
		if (value == null || value.trim().isEmpty()){
			throw new IllegalArgumentException("No se encontró la property requerida: " + key);
		}
		return value.trim();
	}
	
	private static String resolvePropertyValue(Properties raw, String key, java.util.Set<String> visiting){
		if (visiting.contains(key)){
			throw new IllegalArgumentException("Referencia circular detectada en properties para la clave: " + key);
		}
		
		String value = raw.getProperty(key);
			if (value == null){
			return null;
		}
		
		visiting.add(key);
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
		StringBuffer sb = new StringBuffer();
		
		while (matcher.find()){
			String referencedKey = matcher.group(1);
			String referencedValue = resolvePropertyValue(raw, referencedKey, visiting);
			if (referencedValue == null){
				throw new IllegalArgumentException("No se pudo resolver la property referenciada: " + referencedKey);
			}
			
			matcher.appendReplacement(sb, Matcher.quoteReplacement(referencedValue));
		}
		
		matcher.appendTail(sb);
		visiting.remove(key);
		
		return sb.toString();
	}
	
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
	
	public java.sql.Connection openConnection(boolean autoCommit) throws java.sql.SQLException, ClassNotFoundException, java.io.IOException{
		JdbcConfig jdbcConfig = initJdbcConfig();
		Class.forName(jdbcConfig.jdbcDriver);
		java.sql.Connection connection = java.sql.DriverManager.getConnection(
		jdbcConfig.jdbcUrl,
		jdbcConfig.user,
		jdbcConfig.password
		);
		connection.setAutoCommit(autoCommit);
		return connection;
	}
	
	private final class JdbcConfig{
		private String jdbcDriver;
		private String jdbcUrl;
		private String user;
		private String password;
	}
	
}
