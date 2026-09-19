package mx.com.liverpool.p360.services.core.sftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.client.SftpClientFactory;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class ParseEANGrupoArticulo implements AutoCloseable {

    private final String source;
    private final String host;
    private final int port;
    private final String user;
    private final Path privateKeyPath;
    private final boolean deleteRemoteAfterSuccess;

    private static final String DEFAULT_REMOTE_DIRECTORY =
            "/interfase/mer/temp";

    private static final String DEFAULT_FILENAME_REGEX =
            "EAN_GPOART_([0-9]+)\\.csv";

    private static final String DEFAULT_DELTA_FILENAME_REGEX =
            "EAN_delta\\.csv";

    private static final String DEFAULT_LOCAL_PROCESSED_DIRECTORY =
            "/u01/stage/ean_gpoart/processed";

    private static final long DEFAULT_POLL_INTERVAL_MS = 10_000L;
    private static final int REST_BATCH_SIZE = 500;

    private static final org.json.JSONArray PRODUCT_EAN_COLUMNS =
            new org.json.JSONArray()
                    .put(new org.json.JSONObject().put(
                            "identifier",
                            "Product2G.EAN"))
                    .put(new org.json.JSONObject().put(
                            "identifier",
                            "Product2GCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"))
                    .put(new org.json.JSONObject().put(
                            "identifier",
                            "Product2GCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"));

    private static final org.json.JSONArray ARTICLE_EAN_COLUMNS =
            new org.json.JSONArray()
                    .put(new org.json.JSONObject().put(
                            "identifier",
                            "Article.EAN"))
                    .put(new org.json.JSONObject().put(
                            "identifier",
                            "ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"))
                    .put(new org.json.JSONObject().put(
                            "identifier",
                            "ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"));

    private final String remoteDirectory;
    private final Pattern filenamePattern;
    private final Pattern deltaFilenamePattern;
    private final Path localProcessedDirectory;
    private final Path stateFile;
    private final long pollIntervalMs;

    private volatile boolean running = true;

    private final RESTWrapper rw = new RESTWrapper();

    private final ELog el = new ELog() {
        @Override
        public void logE(Exception e) {
            ParseEANGrupoArticulo.this.logE(e);
        }

        @Override
        public void log(String message) {
            ParseEANGrupoArticulo.this.log(message);
        }
    };

    private final DBAccessDataStub dastub = new DBAccessDataStub(el);

    public ParseEANGrupoArticulo() {
        this("ecc");
    }

    public ParseEANGrupoArticulo(String source) {
        this(source,
                "s4h".equals(validateSource(source))
                        ? requiredProperty(profileKey(source, "remote_directory"))
                        : propertyOrDefault(profileKey(source, "remote_directory"), DEFAULT_REMOTE_DIRECTORY),
                propertyOrDefault(profileKey(source, "filename_regex"), DEFAULT_FILENAME_REGEX),
                Paths.get(propertyOrDefault(profileKey(source, "local_processed_dir"),
                        "s4h".equals(source) ? "/u01/stage/ean_gpoart_s4h/processed" : DEFAULT_LOCAL_PROCESSED_DIRECTORY)),
                positiveLongProperty(profileKey(source, "poll_interval_ms"), DEFAULT_POLL_INTERVAL_MS));
    }

    public ParseEANGrupoArticulo(String remoteDirectory, String filenameRegex,
            Path localProcessedDirectory, long pollIntervalMs) {
        this("ecc", remoteDirectory, filenameRegex, localProcessedDirectory, pollIntervalMs);
    }

    private ParseEANGrupoArticulo(String source, String remoteDirectory, String filenameRegex,
            Path localProcessedDirectory, long pollIntervalMs) {
        this.source = validateSource(source);
        String connection = "p360.contingency." + source + ".";
        this.host = requiredProperty(connection + "host");
        this.port = Integer.parseInt(requiredProperty(connection + "port"));
        this.user = requiredProperty(connection + "userp360");
        this.privateKeyPath = Paths.get(requiredProperty(connection + "private_key_path"));
        String delete = propertyOrDefault(profileKey(source, "delete_remote_after_success"), "true");
        if (!"true".equalsIgnoreCase(delete) && !"false".equalsIgnoreCase(delete))
            throw new IllegalArgumentException("delete_remote_after_success must be true or false");
        this.deleteRemoteAfterSuccess = Boolean.parseBoolean(delete);
        this.remoteDirectory = remoteDirectory;
        this.filenamePattern = Pattern.compile(filenameRegex);
        if (this.filenamePattern.matcher("").groupCount() < 1)
            throw new IllegalArgumentException("filename_regex must capture the item group");
        this.deltaFilenamePattern = Pattern.compile(propertyOrDefault(
                profileKey(source, "delta_filename_regex"), DEFAULT_DELTA_FILENAME_REGEX), Pattern.CASE_INSENSITIVE);
        this.localProcessedDirectory = localProcessedDirectory;
        this.pollIntervalMs = pollIntervalMs > 0L ? pollIntervalMs : DEFAULT_POLL_INTERVAL_MS;
        this.stateFile = Paths.get(propertyOrDefault(profileKey(source, "state_file"),
                localProcessedDirectory.resolve("processed_ean_gpoart.properties").toString()));
    }

    private static String validateSource(String source) {
        if (!"ecc".equals(source) && !"s4h".equals(source))
            throw new IllegalArgumentException("Source must be ecc or s4h");
        return source;
    }

    private static String profileKey(String source, String suffix) {
        validateSource(source);
        return ("ecc".equals(source) ? "p360.contingency.ean_gpoart."
                : "p360.contingency.s4h.ean_gpoart.") + suffix;
    }

    private static String requiredProperty(String key) {
        String value = propertyOrDefault(key, null);
        if (value == null) throw new IllegalArgumentException("Missing property: " + key);
        return value;
    }

    public void runDaemon() {

        try {
            java.nio.file.Files.createDirectories(localProcessedDirectory);

            Path parent = stateFile.getParent();
            if (parent != null) {
                java.nio.file.Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No fue posible preparar los directorios locales de EAN_GPOART.",
                    e);
        }

        Properties processedState = loadProcessedState();

        log("Starting EAN Grupo Articulo daemon. source=" + source + ", host=" + host + ", deleteRemoteAfterSuccess=" + deleteRemoteAfterSuccess);
        log("Remote directory: " + remoteDirectory);
        log("Filename regex: " + filenamePattern.pattern());
        log("Delta filename regex: " + deltaFilenamePattern.pattern());
        log("Local directory: " + localProcessedDirectory);
        log("State file: " + stateFile);
        log("Poll interval ms: " + pollIntervalMs);

        try (SshClient client = SshClient.setUpDefaultClient()) {

            client.setKeyIdentityProvider(
                    new FileKeyPairProvider(privateKeyPath));

            client.start();

            while (running) {

                try (ClientSession session =
                             client.connect(user, host, port)
                                     .verify(15, TimeUnit.SECONDS)
                                     .getSession()) {

                    session.auth().verify(15, TimeUnit.SECONDS);
                    session.setSessionHeartbeat(org.apache.sshd.common.session.SessionHeartbeatController.HeartbeatType.IGNORE, TimeUnit.SECONDS, 30L);

                    try (SftpClient sftp =
                                 SftpClientFactory.instance()
                                         .createSftpClient(session)) {


                        try {
                            processFiles(sftp, processedState);
                        } catch (Exception sftpBroken) {
                            log("SFTP/session se rompiÃ³; reconecto en el siguiente ciclo.");
                            logE(asException(sftpBroken));
                        }
                    }

                } catch (Exception connectOrAuthError) {
                    log("No se pudo conectar/auth; reintento en el siguiente ciclo.");
                    logE(asException(connectOrAuthError));
                }

                sleepPollInterval();
            }

        } catch (IOException e) {
            logE(e);
        }

        log("EAN Grupo Articulo daemon finished.");
    }

    private void processFiles(
            SftpClient sftp,
            Properties processedState)
            throws IOException {

        Iterable<DirEntry> entriesIt = sftp.readDir(remoteDirectory);
        List<DirEntry> entries = new ArrayList<>();

        try {
            for (DirEntry entry : entriesIt) {

                String name = entry.getFilename();

                if (".".equals(name) || "..".equals(name)) {
                    continue;
                }

                Matcher matcher = filenamePattern.matcher(name);
                boolean standardFile = matcher.matches();
                boolean deltaFile = deltaFilenamePattern.matcher(name).matches();

                if (!standardFile && !deltaFile) {
                    continue;
                }

                entries.add(entry);
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }

        entries.sort(Comparator.comparing(DirEntry::getFilename));

        int found = entries.size();
        int processed = 0;
        int skipped = 0;
        int failed = 0;

        for (DirEntry entry : entries) {

            if (!running) {
                break;
            }

            String name = entry.getFilename();
            Matcher matcher = filenamePattern.matcher(name);

            boolean standardFile = matcher.matches();
            boolean deltaFile = deltaFilenamePattern.matcher(name).matches();

            if (!standardFile && !deltaFile) {
                continue;
            }

            String itemGroup = standardFile ? matcher.group(1) : null;
            long remoteModified =
                    entry.getAttributes().getModifyTime().toMillis();

            String previousTimestamp = processedState.getProperty(name);

            if (previousTimestamp != null
                    && previousTimestamp.equals(String.valueOf(remoteModified))) {

                skipped++;
                continue;
            }

            String remoteFile =
                    remoteDirectory.endsWith("/")
                            ? remoteDirectory + name
                            : remoteDirectory + "/" + name;

            log("Processing file: "
                    + remoteFile
                    + ", type="
                    + (deltaFile ? "EAN_DELTA" : "EAN_GPOART")
                    + ", ItemGroup="
                    + itemGroup
                    + ", modifyTime="
                    + remoteModified);

            try {

                Path localCopy = copyLocal(sftp, remoteFile, name);

                processLocalFile(localCopy, itemGroup, deltaFile);
                if (deleteRemoteAfterSuccess) {
                    sftp.remove(remoteFile);
                    log("Remote file deleted successfully: " + remoteFile);
                } else {
                    log("Remote file retained after success: " + remoteFile);
                }
                processedState.setProperty(
                        name,
                        String.valueOf(remoteModified));

                saveProcessedState(processedState);

                processed++;

                log("File completed and state saved: " + name);

            } catch (Exception perFileError) {

                failed++;

                log("File failed; it will be retried: " + name);
                logE(asException(perFileError));

                if (isSftpBroken(perFileError)) {
                    throw perFileError instanceof IOException
                            ? (IOException) perFileError
                            : new IOException(
                                    "SFTP connection was closed while processing " + name,
                                    perFileError);
                }
            }
        }

        log("Finished file scan."
                + " found=" + found
                + ", processed=" + processed
                + ", skipped=" + skipped
                + ", failed=" + failed);
    }

    private Path copyLocal(
            SftpClient sftp,
            String remoteFile,
            String name)
            throws IOException {

        Path localCopy = localProcessedDirectory.resolve(name);

        try (InputStream input = sftp.read(remoteFile)) {
            java.nio.file.Files.copy(
                    input,
                    localCopy,
                    StandardCopyOption.REPLACE_EXISTING);
        }

        log("Local copy created: " + localCopy);

        return localCopy;
    }

    private void processLocalFile(
            Path localFile,
            String itemGroup,
            boolean deltaFile)
            throws IOException {

        long lineNumber = 0L;
        long rejectedRows = 0L;

        /*
         * Conservamos Ãºnicamente la Ãºltima acciÃ³n para la misma relaciÃ³n.
         *
         * EAN_GPOART:
         *   SKU|EAN|NEGOCIO|ITEMGROUP
         *
         * EAN_delta:
         *   SKU|EAN|NEGOCIO|NULL
         */
        Map<String, String[]> finalChanges = new LinkedHashMap<>();

        try (BufferedReader reader =
                     java.nio.file.Files.newBufferedReader(
                             localFile,
                             StandardCharsets.UTF_8)) {

            String line;

            while ((line = reader.readLine()) != null) {

                lineNumber++;

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split("\\|", -1);

                if (data.length != 4) {
                    rejectedRows++;
                    log("Invalid row."
                            + " file=" + localFile.getFileName()
                            + ", line=" + lineNumber
                            + ", expectedColumns=4"
                            + ", actualColumns=" + data.length
                            + ", value=" + line);
                    continue;
                }

                String[] row = parseRow(
                        data,
                        itemGroup,
                        deltaFile,
                        lineNumber);

                if (row == null) {
                    rejectedRows++;
                    continue;
                }

                finalChanges.put(relationKey(row), row);
            }
        }

        List<String[]> creates = new ArrayList<>();
        List<String[]> deletes = new ArrayList<>();

        for (String[] row : finalChanges.values()) {
            if ("1".equals(row[4])) {
                creates.add(row);
            } else if ("2".equals(row[4])) {
                deletes.add(row);
            }
        }

        log("File parsed."
                + " file=" + localFile.getFileName()
                + ", type=" + (deltaFile ? "EAN_DELTA" : "EAN_GPOART")
                + ", ItemGroup=" + itemGroup
                + ", finalRows=" + finalChanges.size()
                + ", creates=" + creates.size()
                + ", deletes=" + deletes.size()
                + ", rejectedRows=" + rejectedRows);

        /*
         * Altas: idempotentes vÃ­a MERGE.
         */
        if (!creates.isEmpty()) {

            log("Applying "
                    + creates.size()
                    + " EAN creates to TC_EAN_NEGOCIO"
                    + ", ItemGroup=" + itemGroup);

            if (!dastub.applyEanNegocioChanges(creates)) {
                throw new IOException(
                        "No fue posible aplicar altas en TC_EAN_NEGOCIO"
                                + ", file=" + localFile
                                + ", ItemGroup=" + itemGroup);
            }
        }

        /*
         * Bajas:
         * 1) resolver IDs y EAN actual por SKU;
         * 2) vaciar P360 por List API cuando el EAN actual todavÃ­a corresponde
         *    a alguno de los EAN que este feed estÃ¡ dando de baja;
         * 3) sÃ³lo despuÃ©s borrar la relaciÃ³n de la tabla maestra.
         *
         * Si falla REST, lanzamos IOException y NO ejecutamos el DELETE maestro.
         * El archivo no se marca como procesado y se reintentarÃ¡.
         */
        if (!deletes.isEmpty()) {

            clearDeletedEansInP360(deletes);

            log("Deleting "
                    + deletes.size()
                    + " EAN relations from TC_EAN_NEGOCIO"
                    + ", ItemGroup=" + itemGroup);

            if (!dastub.applyEanNegocioChanges(deletes)) {
                throw new IOException(
                        "No fue posible aplicar bajas en TC_EAN_NEGOCIO"
                                + ", file=" + localFile
                                + ", ItemGroup=" + itemGroup);
            }
        }
    }

    private String[] parseRow(
            String[] data,
            String itemGroup,
            boolean deltaFile,
            long lineNumber) {

        String sku = normalizeSku(data[0]);
        String ean = trimToEmpty(data[1]);
        String negocio = trimToEmpty(data[2]);
        String accion = trimToEmpty(data[3]);

        if (sku.isEmpty()) {
            log("Rejected row " + lineNumber + ": empty SKU");
            return null;
        }

        if (!sku.matches("[0-9]+")) {
            log("Rejected row "
                    + lineNumber
                    + ": non numeric SKU="
                    + sku);
            return null;
        }

        if (ean.isEmpty()) {
            log("Rejected row "
                    + lineNumber
                    + ": empty EAN, SKU="
                    + sku);
            return null;
        }

        if (!"1".equals(accion) && !"2".equals(accion)) {
            log("Rejected row "
                    + lineNumber
                    + ": invalid action="
                    + accion
                    + ", SKU="
                    + sku
                    + ", EAN="
                    + ean
                    + ", Negocio="
                    + negocio
                    + ", ItemGroup="
                    + itemGroup);
            return null;
        }

        /*
         * EAN_delta.csv no contiene grupo de artÃ­culo.
         *
         * Action=1:
         *   - Negocio sÃ­ es obligatorio.
         *   - ItemGroup se guarda como SQL NULL.
         *
         * Action=2:
         *   - ItemGroup queda NULL y significa "no acotar la baja por grupo".
         *   - Negocio puede venir vacÃ­o; en ese caso la baja tampoco se acota
         *     por negocio.
         */
        if (deltaFile) {

            if ("1".equals(accion) && negocio.isEmpty()) {
                log("Rejected EAN_delta Action=1 row "
                        + lineNumber
                        + ": empty Negocio, SKU="
                        + sku
                        + ", EAN="
                        + ean);
                return null;
            }

            return new String[] {
                    sku,
                    ean,
                    negocio.isEmpty() ? null : negocio,
                    null,
                    accion
            };
        }

        /*
         * El archivo EAN_GPOART_<ItemGroup>.csv conserva la semÃ¡ntica anterior.
         */
        if (negocio.isEmpty()) {
            log("Rejected row "
                    + lineNumber
                    + ": empty Negocio, SKU="
                    + sku
                    + ", EAN="
                    + ean);
            return null;
        }

        if (itemGroup == null || itemGroup.isEmpty()) {
            log("Rejected row "
                    + lineNumber
                    + ": empty ItemGroup, SKU="
                    + sku
                    + ", EAN="
                    + ean);
            return null;
        }

        return new String[] {
                sku,
                ean,
                negocio,
                itemGroup,
                accion
        };
    }

    private void clearDeletedEansInP360(
            List<String[]> deletes)
            throws IOException {

        Map<String, Set<String>> eansBySku = new LinkedHashMap<>();

        for (String[] row : deletes) {
            eansBySku
                    .computeIfAbsent(
                            row[0],
                            k -> new LinkedHashSet<>())
                    .add(row[1]);
        }

        Map<String, String[]> targets;

        try {
            targets = dastub.getEanCleanupTargetsBySKUs(eansBySku.keySet());
        } catch (RuntimeException e) {
            throw new IOException(
                    "No fue posible resolver Product/Article para las bajas EAN.",
                    e);
        }

        Map<String, String> qp = new HashMap<>();
        qp.put("includeObjectsInProtocol", "false");

        AtomicBoolean productWriteOk = new AtomicBoolean(true);
        AtomicBoolean articleWriteOk = new AtomicBoolean(true);

        RequestHandler productRequest =
                new RequestHandler(
                        cloneColumns(PRODUCT_EAN_COLUMNS),
                        REST_BATCH_SIZE,
                        request -> {
                            if (!writeAndValidate(
                                    "Product2G",
                                    qp,
                                    request)) {
                                productWriteOk.set(false);
                            }
                        });

        RequestHandler articleRequest =
                new RequestHandler(
                        cloneColumns(ARTICLE_EAN_COLUMNS),
                        REST_BATCH_SIZE,
                        request -> {
                            if (!writeAndValidate(
                                    "Article",
                                    qp,
                                    request)) {
                                articleWriteOk.set(false);
                            }
                        });

        int productRows = 0;
        int articleRows = 0;
        int notFound = 0;
        int protectedByCurrentEan = 0;

        for (Map.Entry<String, Set<String>> entry : eansBySku.entrySet()) {

            String sku = entry.getKey();
            Set<String> eansToDelete = entry.getValue();
            String[] target = targets.get(sku);

            if (target == null) {
                notFound++;
                log("Action=2: no active Article/Product relation found for SKU="
                        + sku
                        + ". Only TC_EAN_NEGOCIO relation will be removed.");
                continue;
            }

            String articleId = target[0];
            String productId = target[1];
            String articleEan = trimToEmpty(target[2]);
            String productEan = trimToEmpty(target[3]);

            boolean articleMatches = eansToDelete.contains(articleEan);
            boolean productMatches = eansToDelete.contains(productEan);
            boolean articleBlank = articleEan.isEmpty();
            boolean productBlank = productEan.isEmpty();

            /*
             * Si un lado tiene el EAN viejo y el otro ya estÃ¡ vacÃ­o, limpiamos
             * tambiÃ©n el lado vacÃ­o para borrar posibles valores residuales de
             * MainBarCode/MainBarCodeS4H.
             *
             * Si ambos estÃ¡n vacÃ­os hacemos lo mismo: no existe riesgo de borrar
             * un EAN base nuevo y dejamos limpios los characteristic values.
             */
            boolean clearArticle =
                    !isBlank(articleId)
                    && (articleMatches
                        || (articleBlank && (productMatches || productBlank)));

            boolean clearProduct =
                    !isBlank(productId)
                    && (productMatches
                        || (productBlank && (articleMatches || articleBlank)));

            if (clearProduct) {
                productRequest.addRow(emptyEanRow(productId));
                productRows++;
            } else if (!isBlank(productId)) {
                protectedByCurrentEan++;
                log("Action=2: Product2G EAN was NOT cleared because current EAN changed."
                        + " SKU=" + sku
                        + ", ProductID=" + productId
                        + ", currentEAN=" + productEan
                        + ", feedEANs=" + eansToDelete);
            }

            if (clearArticle) {
                articleRequest.addRow(emptyEanRow(articleId));
                articleRows++;
            } else if (!isBlank(articleId)) {
                protectedByCurrentEan++;
                log("Action=2: Article EAN was NOT cleared because current EAN changed."
                        + " SKU=" + sku
                        + ", ArticleID=" + articleId
                        + ", currentEAN=" + articleEan
                        + ", feedEANs=" + eansToDelete);
            }
        }

        productRequest.sendData();
        articleRequest.sendData();

        log("Action=2 P360 cleanup summary:"
                + " skus=" + eansBySku.size()
                + ", productRows=" + productRows
                + ", articleRows=" + articleRows
                + ", noP360Relation=" + notFound
                + ", protectedByCurrentEan=" + protectedByCurrentEan);

        if (!productWriteOk.get() || !articleWriteOk.get()) {
            throw new IOException(
                    "List API reported errors while clearing EAN fields."
                            + " Product2G_OK=" + productWriteOk.get()
                            + ", Article_OK=" + articleWriteOk.get());
        }
    }

    private boolean writeAndValidate(
            String entity,
            Map<String, String> qp,
            org.json.JSONObject request) {

        AtomicBoolean ok = new AtomicBoolean(false);

        try {
            rw.writeData(
                    "list",
                    entity,
                    null,
                    qp,
                    request,
                    rawResponse -> {
                        log("List API " + entity + " response: " + rawResponse);
                        ok.set(isSuccessfulWriteResponse(rawResponse));
                    });
        } catch (RuntimeException e) {
            logE(e);
            return false;
        }

        if (!ok.get()) {
            log("List API write failed validation for entity=" + entity);
        }

        return ok.get();
    }

    private boolean isSuccessfulWriteResponse(String rawResponse) {

        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return false;
        }

        try {
            org.json.JSONObject response =
                    new org.json.JSONObject(rawResponse);

            org.json.JSONObject counters =
                    response.optJSONObject("counters");

            if (counters == null) {
                return false;
            }

            return counters.optInt("errors", -1) == 0
                    && counters.optInt("objectsWithErrors", -1) == 0;

        } catch (org.json.JSONException e) {
            logE(e);
            return false;
        }
    }

    private static org.json.JSONObject emptyEanRow(String identifier) {
        return new org.json.JSONObject()
                .put(
                        "object",
                        new org.json.JSONObject()
                                .put("id", "'" + identifier + "'@1"))
                .put(
                        "values",
                        new org.json.JSONArray()
                                .put("")
                                .put("")
                                .put(""));
    }

    private static org.json.JSONArray cloneColumns(
            org.json.JSONArray source) {
        return new org.json.JSONArray(source.toString());
    }

    private Properties loadProcessedState() {

        Properties state = new Properties();

        if (!java.nio.file.Files.exists(stateFile)) {
            return state;
        }

        try (InputStream in =
                     java.nio.file.Files.newInputStream(stateFile)) {

            state.load(in);

        } catch (IOException e) {
            logE(e);
        }

        return state;
    }

    private void saveProcessedState(Properties state)
            throws IOException {

        Path tmp = stateFile.resolveSibling(
                stateFile.getFileName().toString() + ".tmp");

        try (OutputStream out =
                     java.nio.file.Files.newOutputStream(tmp)) {

            state.store(out, null);
        }

        try {
            java.nio.file.Files.move(
                    tmp,
                    stateFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            java.nio.file.Files.move(
                    tmp,
                    stateFile,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void sleepPollInterval() {

        if (!running) {
            return;
        }

        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    private static boolean isSftpBroken(Throwable e) {

        Throwable current = e;

        while (current != null) {

            String msg = String.valueOf(current.getMessage());

            if (msg.contains("client is closed")
                    || msg.contains("Channel is closed")
                    || msg.contains("SSH_FX_CONNECTION_LOST")
                    || msg.contains("Connection reset")
                    || msg.contains("Broken pipe")) {
                return true;
            }

            current = current.getCause();
        }

        return e instanceof UncheckedIOException;
    }

    private static String relationKey(String[] row) {
        return java.util.Objects.toString(row[0], "")
                + "\u0001" + java.util.Objects.toString(row[1], "")
                + "\u0001" + java.util.Objects.toString(row[2], "")
                + "\u0001" + java.util.Objects.toString(row[3], "");
    }

    private static String normalizeSku(String value) {

        String trimmed = trimToEmpty(value);

        if (trimmed.isEmpty()) {
            return "";
        }

        return trimmed.replaceFirst("^0+(?!$)", "");
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String propertyOrDefault(
            String property,
            String defaultValue) {

        try {
            String value = PropertiesManager.get(property);

            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        } catch (Exception ignored) {
        }

        return defaultValue;
    }

    private static long positiveLongProperty(
            String property,
            long defaultValue) {

        String value = propertyOrDefault(
                property,
                String.valueOf(defaultValue));

        try {
            long parsed = Long.parseLong(value);
            return parsed > 0L ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Exception asException(Throwable e) {
        return e instanceof Exception
                ? (Exception) e
                : new RuntimeException(e);
    }

    public void stop() {
        running = false;
    }

    private void log(String message) {
        System.out.println(
                "["
                        + new java.text.SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss")
                                .format(new java.util.Date())
                        + "] "
                        + message);
    }

    private void logE(Exception e) {
        if (e != null) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {

        running = false;

        try {
            dastub.close();
        } catch (Exception e) {
            logE(asException(e));
        }
    }

    public static void main(String[] args) {
        if (args.length > 1) throw new IllegalArgumentException("Usage: ParseEANGrupoArticulo [ecc|s4h]");

        try (ParseEANGrupoArticulo parser =
                     new ParseEANGrupoArticulo(args.length == 0 ? "ecc" : args[0])) {

            Runtime.getRuntime().addShutdownHook(
                    new Thread(
                            parser::stop,
                            "EAN-GPOART-shutdown"));

            parser.runDaemon();
        }
    }
}
