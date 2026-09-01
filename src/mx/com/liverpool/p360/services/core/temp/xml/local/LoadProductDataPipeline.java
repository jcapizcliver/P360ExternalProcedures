package mx.com.liverpool.p360.services.core.temp.xml.local;

import mx.com.liverpool.dataprofiling.preparison.envioproductos.PruebaEnvioPubSubMediaAssets;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.temp.xml.local.precise.AnotherXMLHandlerSecondOpinionOnSpecificProducts;

public class LoadProductDataPipeline {

    private static final java.util.concurrent.ConcurrentLinkedQueue<Integer> productos =
            new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final java.util.concurrent.ConcurrentLinkedQueue<String> paths =
            new java.util.concurrent.ConcurrentLinkedQueue<>();

    /*
     * El trabajo STEP es muy pesado en heap. Dos ejecuciones simultáneas siguen
     * permitiendo paralelismo sin dejar que el pool HTTP de Tomcat dispare N
     * parsers grandes al mismo tiempo.
     */
    private static final int MAX_CONCURRENT_STEP =
            Math.max(1, Integer.getInteger("p360.step.max.concurrent", 2));
    private static final java.util.concurrent.Semaphore STEP_PROCESSING_SLOTS =
            new java.util.concurrent.Semaphore(MAX_CONCURRENT_STEP, true);

    private static volatile boolean running = true;
    private static int prev = -1;

    static {
        Thread t = new Thread(() -> {
            while (running) {
                try {
                    if (!productos.isEmpty()) {
                        int a = 0;
                        for (Integer a0 : productos) {
                            if (a0 != null) {
                                a += a0;
                            }
                        }

                        if (a == prev) {
                            java.util.Set<String> pathsSet = new java.util.TreeSet<>(paths);
                            productos.clear();
                            paths.clear();
                            prev = -1;

                            for (String path : pathsSet) {
                                try {
                                    LoadProductDataSecondOpinionRelations.main(new String[] { path });
                                } catch (Exception e) {
                                    logE(e);
                                }
                            }

                            java.util.Map<String, String> qp = new java.util.TreeMap<>();
                            RESTWrapper rw = new RESTWrapper();
                            rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces");
                            qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
                            qp.put("token", "H3kGU98FssCp15V7VT9s1nltZfbLxuj94WFRMOVzAs0");
                            logMe("" + rw.getRw().makeRequest(
                                    "POST",
                                    "/AAAAvwSYdXo/messages",
                                    qp,
                                    new org.json.JSONObject()
                                            .put("text", "Cadencia finalizada. Productos procesados: " + a + " 😁.")
                                            .toString()));
                        } else {
                            java.util.Map<String, String> qp = new java.util.TreeMap<>();
                            RESTWrapper rw = new RESTWrapper();
                            rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces");
                            qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
                            qp.put("token", "H3kGU98FssCp15V7VT9s1nltZfbLxuj94WFRMOVzAs0");
                            logMe("" + rw.getRw().makeRequest(
                                    "POST",
                                    "/AAAAvwSYdXo/messages",
                                    qp,
                                    new org.json.JSONObject()
                                            .put("text", "Productos procesados: " + a + " 😁.")
                                            .toString()));
                            prev = a;
                        }
                    }

                    Thread.sleep(600000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                } catch (Exception e) {
                    logE(e);
                }
            }
            System.out.println("Exiting.");
        }, "p360-step-cadence");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Camino normal nuevo: nunca carga el XML completo como String.
     */
    public static void processContent(java.nio.file.Path path) {
        boolean acquired = false;
        long init = System.currentTimeMillis();
        try {
            logMe("Waiting STEP processing slot for: " + path);
            STEP_PROCESSING_SLOTS.acquire();
            acquired = true;
            logMe("Acquired STEP processing slot for: " + path);

            logMe("Sending data media assets: " + path);
            PruebaEnvioPubSubMediaAssets.process(path);

            logMe("Sending second opinion: " + path);
            int a = AnotherXMLHandlerSecondOpinionOnSpecificProducts.processPath(path);

            productos.add(a);
            paths.add(path.toString());
            logMe("STEP processing finished: " + path + " in "
                    + new RESTWrapper().getRw().formatTime(System.currentTimeMillis() - init));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logE(e);
        } catch (Exception e) {
            handleProcessingError(e);
        } finally {
            if (acquired) {
                STEP_PROCESSING_SLOTS.release();
            }
        }
    }

    /**
     * Compatibilidad temporal con invocadores viejos. El flujo de ReceiveSTEPFile
     * ya no usa esta variante.
     */
    @Deprecated
    public static void processContent(String content, String path) {
        boolean acquired = false;
        try {
            STEP_PROCESSING_SLOTS.acquire();
            acquired = true;
            PruebaEnvioPubSubMediaAssets.process(content);
            int a = AnotherXMLHandlerSecondOpinionOnSpecificProducts.processContent(content);
            productos.add(a);
            paths.add(path);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logE(e);
        } catch (Exception e) {
            handleProcessingError(e);
        } finally {
            if (acquired) {
                STEP_PROCESSING_SLOTS.release();
            }
        }
    }

    private static void handleProcessingError(Exception e) {
        logE(e);
        try {
            java.util.Map<String, String> qp = new java.util.TreeMap<>();
            RESTWrapper rw = new RESTWrapper();
            rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces");
            qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
            qp.put("token", "H3kGU98FssCp15V7VT9s1nltZfbLxuj94WFRMOVzAs0");
            logMe("" + rw.getRw().makeRequest(
                    "POST",
                    "/AAAAvwSYdXo/messages",
                    qp,
                    new org.json.JSONObject()
                            .put("text", "🚨 El proceso falló.\nYa tronó y necesita revisión (última cuenta: "
                                    + prev + ").\nChequen logs porfa y el último error. "
                                    + e.getMessage() + " .")
                            .toString()));
        } catch (Exception notifyError) {
            logE(notifyError);
        }
    }

    public static void main(String[] args) {
        running = false;
        try {
            logMe("Sending second opinion");
            LoadProductDataSecondOpinion.main(args);
            logMe("Sending remaining fields");
            LoadProductDataRemainingFields.main(args);
            logMe("Sending ftw");
            LoadProductDataSecondOpinionFTW.main(args);
            logMe("Now sending rels...");
            LoadProductDataSecondOpinionRelations.main(args);
            logMe("Sending data to admin");
            LoadProductDataSecondOpinionSendThemToAdmin.main(args);
        } catch (Exception e) {
            logE(e);
        }
    }

    private static void logMe(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/loadProductDataPipeline.log", true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "]  " + message);
        } catch (java.io.IOException e) {
        }
    }

    private static void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/loadProductDataPipeline.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }
}
