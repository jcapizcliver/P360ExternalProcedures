package mx.com.liverpool.p360.services.core;

public final class LogResources {

    private LogResources() {
        // Evita instanciación
    }

    public static final String LOG_DIR_PROPERTY = "miapp.log.dir";
    public static final String DEFAULT_LOG_DIR = "logs";
    public static final String LOG_FILE_NAME = "mi-app.log";
    public static final String LOGGER_NAME = "MiAplicacionLogger";
    public static final String LOGGER_INIT_ERROR = "No se pudo inicializar el logger";
    public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String LOG_LINE_PREFIX = "[";
    public static final String LOG_LINE_MIDDLE = "]  [";
    public static final String LOG_LINE_SUFFIX = "]  ";
}
