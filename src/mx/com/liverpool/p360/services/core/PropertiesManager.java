package mx.com.liverpool.p360.services.core;

public class PropertiesManager {
	
	 	private static final String EXTERNAL_CONFIG_PATH = "/u01/workshop/p360_contingencyservices.properties";
	    private static final String INTERNAL_CONFIG_FILE = "/p360_contingencyservices.properties";
	    
	    private static String resolvedExternalPath = null;

	    private static volatile java.util.Properties properties;

	    static {
	    	String envPath = System.getenv("EXTERNAL_CONFIG_PATH");
	        if (envPath == null || envPath.trim().isEmpty()) {
	        	envPath = EXTERNAL_CONFIG_PATH;
	        }
	        resolvedExternalPath = envPath;
	        properties = new java.util.Properties();
	        
	        loadProperties();
	        startWatcher();

	    }
	    
	    private static synchronized void loadProperties() {
	        java.util.Properties newProperties = new java.util.Properties();
	        boolean loaded = false;

	        try (java.io.InputStream ext = new java.io.FileInputStream(resolvedExternalPath)) {
	            newProperties.load(ext);
	            loaded = true;
	            System.out.println("PropertiesManager: Loaded external config from " + resolvedExternalPath);
	        } catch (Exception e) {
	            System.out.println("PropertiesManager: No external config found. Trying internal fallback.");
	        }

	        if (!loaded) {
	            try (java.io.InputStream in = PropertiesManager.class.getResourceAsStream(INTERNAL_CONFIG_FILE)) {
	                if (in != null) {
	                    newProperties.load(in);
	                    System.out.println("PropertiesManager: Loaded internal default config.");
	                } else {
	                    System.err.println("PropertiesManager: No config file found.");
	                }
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }

	        properties = newProperties;
	    }
	    
	    private static void startWatcher() {
	        java.nio.file.Path configFilePath = java.nio.file.Paths.get(resolvedExternalPath);

	        if (!java.nio.file.Files.exists(configFilePath)) {
	            System.out.println("PropertiesManager: External config file does not exist, watcher not started for " + resolvedExternalPath);
	            return;
	        }

	        java.nio.file.Path parentDir = configFilePath.getParent();
	        if (parentDir == null) {
	            System.out.println("PropertiesManager: Could not resolve parent directory for " + resolvedExternalPath);
	            return;
	        }

	        Thread watcherThread = new Thread(() -> {
	            try (java.nio.file.WatchService watchService = java.nio.file.FileSystems.getDefault().newWatchService()) {
	                parentDir.register(
	                    watchService,
	                    java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY,
	                    java.nio.file.StandardWatchEventKinds.ENTRY_CREATE,
	                    java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
	                );

	                String targetFileName = configFilePath.getFileName().toString();
	                System.out.println("PropertiesManager: Watching file " + resolvedExternalPath);

	                while (true) {
	                    java.nio.file.WatchKey key = watchService.take();

	                    for (java.nio.file.WatchEvent<?> event : key.pollEvents()) {
	                        java.nio.file.WatchEvent.Kind<?> kind = event.kind();

	                        if (kind == java.nio.file.StandardWatchEventKinds.OVERFLOW) {
	                            continue;
	                        }

	                        java.nio.file.Path changedFile = (java.nio.file.Path) event.context();
	                        if (changedFile != null && targetFileName.equals(changedFile.toString())) {
	                            System.out.println("PropertiesManager: Detected change in " + resolvedExternalPath + " (" + kind.name() + "), reloading...");
	                            try {
	                                Thread.sleep(300);
	                            } catch (InterruptedException ie) {
	                                Thread.currentThread().interrupt();
	                                return;
	                            }
	                            loadProperties();
	                        }
	                    }

	                    boolean valid = key.reset();
	                    if (!valid) {
	                        System.err.println("PropertiesManager: Watch key is no longer valid. Stopping watcher.");
	                        break;
	                    }
	                }
	            } catch (Exception e) {
	                System.err.println("PropertiesManager: Watcher stopped due to error.");
	                e.printStackTrace();
	            }
	        });

	        watcherThread.setName("properties-manager-watcher");
	        watcherThread.setDaemon(true);
	        watcherThread.start();
	    }

	    public static String get(String key, String defaultValue) {
	        return properties.getProperty(key, defaultValue);
	    }

	    public static String get(String key) {
	        return properties.getProperty(key);
	    }
	    
}
