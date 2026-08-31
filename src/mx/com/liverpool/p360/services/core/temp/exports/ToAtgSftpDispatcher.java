package mx.com.liverpool.p360.services.core.temp.exports;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;

import mx.com.liverpool.p360.services.core.PropertiesManager;

/**
 * Dispatches every pepele&lt;batch&gt;_&lt;timestamp&gt;.xml file in ToATG to the
 * DWH and Pricing SFTP servers. It scans files already present at startup and
 * then watches the directory for new files.
 */
public class ToAtgSftpDispatcher {

	private static final Logger LOGGER = Logger.getLogger(ToAtgSftpDispatcher.class.getName());
	private static final String DISPATCHER_PREFIX = "p360.contingency.toatg.dispatcher.";
	private static final String PRICING_PREFIX = "p360.contingency.pricing.sftp.";
	private static final Pattern SOURCE_FILE_PATTERN = Pattern.compile("^pepele\\d+_(\\d{13})\\.xml$");

	private final Path sourceDirectory;
	private final Path processedDirectory;
	private final Path stateDirectory;
	private final long settleMillis;
	private final long retrySeconds;

	public ToAtgSftpDispatcher() {
		this(Paths.get(PropertiesManager.get("p360.contingency.out.ecomm_pepele_directory",
				"/u01/workshop/stage/ToATG")),
				Long.parseLong(PropertiesManager.get(DISPATCHER_PREFIX + "settle_seconds", "2")),
				Long.parseLong(PropertiesManager.get(DISPATCHER_PREFIX + "retry_seconds", "60")));
	}

	ToAtgSftpDispatcher(Path sourceDirectory, long settleSeconds, long retrySeconds) {
		if (settleSeconds < 1 || retrySeconds < 1) {
			throw new IllegalArgumentException("settle_seconds and retry_seconds must be greater than zero");
		}
		this.sourceDirectory = sourceDirectory;
		this.processedDirectory = sourceDirectory.resolve(
				PropertiesManager.get(DISPATCHER_PREFIX + "processed_directory", "processed"));
		this.stateDirectory = sourceDirectory.resolve(".dispatch-state");
		this.settleMillis = TimeUnit.SECONDS.toMillis(settleSeconds);
		this.retrySeconds = retrySeconds;
	}

	public static void main(String[] args) {
		try {
			new ToAtgSftpDispatcher().run();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "ToATG SFTP dispatcher stopped", e);
			System.exit(1);
		}
	}

	/** Runs until its thread is interrupted or the watched directory is lost. */
	public void run() throws IOException, InterruptedException {
		Files.createDirectories(sourceDirectory);
		Files.createDirectories(processedDirectory);
		Files.createDirectories(stateDirectory);

		Path lockPath = stateDirectory.resolve("dispatcher.lock");
		try (FileChannel lockChannel = FileChannel.open(lockPath,
				StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
			FileLock lock = acquireLock(lockChannel);
			try {
				watchAndDispatch();
			} finally {
				lock.release();
			}
		}
	}

	private FileLock acquireLock(FileChannel lockChannel) throws IOException {
		try {
			FileLock lock = lockChannel.tryLock();
			if (lock == null) {
				throw new IllegalStateException("Another dispatcher is already running for " + sourceDirectory);
			}
			return lock;
		} catch (OverlappingFileLockException e) {
			throw new IllegalStateException("Another dispatcher is already running for " + sourceDirectory, e);
		}
	}

	private void watchAndDispatch() throws IOException, InterruptedException {
		try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
			sourceDirectory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_MODIFY);
			LOGGER.info("Watching ToATG directory: " + sourceDirectory);
			processPendingFiles();

			while (!Thread.currentThread().isInterrupted()) {
				WatchKey key = watcher.poll(retrySeconds, TimeUnit.SECONDS);
				if (key == null) {
					processPendingFiles();
					continue;
				}

				boolean mustScan = false;
				for (WatchEvent<?> event : key.pollEvents()) {
					if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
						mustScan = true;
						continue;
					}
					Path changed = (Path) event.context();
					if (changed != null && isSourceFile(changed.getFileName().toString())) {
						mustScan = true;
					}
				}
				if (!key.reset()) {
					throw new IOException("ToATG directory is no longer available: " + sourceDirectory);
				}
				if (mustScan) {
					processPendingFiles();
				}
			}
		}
	}

	void processPendingFiles() {
		List<Path> pending = new ArrayList<>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(sourceDirectory)) {
			for (Path file : files) {
				if (Files.isRegularFile(file) && isSourceFile(file.getFileName().toString())) {
					pending.add(file);
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Could not scan ToATG directory " + sourceDirectory, e);
			return;
		}

		pending.sort(Comparator.comparing(path -> path.getFileName().toString()));
		for (Path file : pending) {
			try {
				processFile(file);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Could not dispatch " + file + "; it will be retried", e);
			}
		}
	}

	void processFile(Path localFile) throws Exception {
		Matcher matcher = SOURCE_FILE_PATTERN.matcher(localFile.getFileName().toString());
		if (!matcher.matches() || !Files.isRegularFile(localFile)) {
			return;
		}
		waitUntilStable(localFile);

		long timestamp = Long.parseLong(matcher.group(1));
		String dwhName = dwhFileName(timestamp);
		String pricingName = pricingFileName(timestamp);
		Path dwhMarker = stateMarker(localFile, "dwh");
		Path pricingMarker = stateMarker(localFile, "pricing");

		if (!Files.exists(dwhMarker)) {
			sendFileToDwh(localFile, dwhName);
			writeMarker(dwhMarker);
			LOGGER.info("DWH sent: " + localFile.getFileName() + " as " + dwhName);
		}
		if (!Files.exists(pricingMarker)) {
			sendFileToPricingSftp(localFile, pricingName);
			writeMarker(pricingMarker);
			LOGGER.info("Pricing sent: " + localFile.getFileName() + " as " + pricingName);
		}

		Path archived = processedDirectory.resolve(localFile.getFileName());
		Files.move(localFile, archived, StandardCopyOption.REPLACE_EXISTING);
		Files.deleteIfExists(dwhMarker);
		Files.deleteIfExists(pricingMarker);
		LOGGER.info("Dispatch completed; archived in " + archived);
	}

	static String dwhFileName(long timestamp) {
		return "eilstep_" + new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date(timestamp)) + ".xml";
	}

	static String pricingFileName(long timestamp) {
		return "output-" + timestamp + ".xml";
	}

	private static boolean isSourceFile(String name) {
		return SOURCE_FILE_PATTERN.matcher(name).matches();
	}

	private Path stateMarker(Path localFile, String destination) {
		return stateDirectory.resolve(localFile.getFileName().toString() + "." + destination + ".sent");
	}

	private void writeMarker(Path marker) throws IOException {
		Files.write(marker, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private void waitUntilStable(Path file) throws IOException, InterruptedException {
		long previousSize = -1;
		long previousModified = -1;
		int stableChecks = 0;
		while (stableChecks < 2) {
			if (!Files.isRegularFile(file)) {
				throw new IOException("File disappeared while waiting for it to finish: " + file);
			}
			long size = Files.size(file);
			long modified = Files.getLastModifiedTime(file).toMillis();
			if (size == previousSize && modified == previousModified) {
				stableChecks++;
			} else {
				stableChecks = 0;
				previousSize = size;
				previousModified = modified;
			}
			Thread.sleep(settleMillis);
		}
	}

	private void sendFileToDwh(Path localFile, String remoteFileName) throws Exception {
		String host = requireProperty("p360.contingency.dwh.host");
		int port = Integer.parseInt(PropertiesManager.get("p360.contingency.dwh.port", "22"));
		String user = requireProperty("p360.contingency.dwh.user");
		Path privateKey = Paths.get(PropertiesManager.get("p360.contingency.dwh.private_key",
				"/home/P360admin/.ssh/id_rsa"));
		String remoteDirectory = requireProperty("p360.contingency.dwh.remote_directory_base");
		long timeout = Long.parseLong(PropertiesManager.get("p360.contingency.dwh.timeout_seconds", "10"));

		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		try (ClientSession session = client.connect(user, host, port).verify(timeout, TimeUnit.SECONDS).getSession()) {
			FileKeyPairProvider keyProvider = new FileKeyPairProvider(privateKey);
			keyProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
			keyProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);
			session.auth().verify(timeout, TimeUnit.SECONDS);
			try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
				uploadAtomically(sftp, localFile, joinRemotePath(remoteDirectory, remoteFileName));
			}
		} finally {
			client.stop();
		}
	}

	private void sendFileToPricingSftp(Path localFile, String remoteFileName) throws Exception {
		String host = requireProperty(PRICING_PREFIX + "host");
		int port = Integer.parseInt(PropertiesManager.get(PRICING_PREFIX + "port", "22"));
		String username = requireProperty(PRICING_PREFIX + "username");
		String password = requireProperty(PRICING_PREFIX + "password");
		String remoteDirectory = PropertiesManager.get(PRICING_PREFIX + "remote_directory", "");
		long connectTimeout = Long.parseLong(PropertiesManager.get(
				PRICING_PREFIX + "connect_timeout_seconds", "10"));
		long authTimeout = Long.parseLong(PropertiesManager.get(
				PRICING_PREFIX + "auth_timeout_seconds", "10"));

		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		try (ClientSession session = client.connect(username, host, port)
				.verify(connectTimeout, TimeUnit.SECONDS).getSession()) {
			session.addPasswordIdentity(password);
			session.auth().verify(authTimeout, TimeUnit.SECONDS);
			try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
				uploadAtomically(sftp, localFile, joinRemotePath(remoteDirectory, remoteFileName));
			}
		} finally {
			client.stop();
		}
	}

	private void uploadAtomically(SftpClient sftp, Path localFile, String remoteFile) throws IOException {
		String temporaryRemoteFile = remoteFile + ".part";
		try (OutputStream remote = sftp.write(temporaryRemoteFile,
				SftpClient.OpenMode.Write, SftpClient.OpenMode.Create, SftpClient.OpenMode.Truncate)) {
			Files.copy(localFile, remote);
		}
		sftp.rename(temporaryRemoteFile, remoteFile, SftpClient.CopyMode.Overwrite);
	}

	private static String joinRemotePath(String directory, String fileName) {
		if (directory == null || directory.trim().isEmpty()) {
			return fileName;
		}
		return directory.endsWith("/") ? directory + fileName : directory + "/" + fileName;
	}

	private static String requireProperty(String key) {
		String value = PropertiesManager.get(key);
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalStateException("Missing required property: " + key);
		}
		return value.trim();
	}
}
