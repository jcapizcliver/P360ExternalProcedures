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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;

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
	private final long retryMillis;
	private final long scanMillis;
	private final int batchSize;
	private final Map<Path, FileObservation> observations = new HashMap<>();
	private final Map<Path, Long> retryAfter = new HashMap<>();

	public ToAtgSftpDispatcher() {
		this(Paths.get(PropertiesManager.get("p360.contingency.out.ecomm_pepele_directory",
				"/u01/workshop/stage/ToATG")),
				Long.parseLong(PropertiesManager.get(DISPATCHER_PREFIX + "settle_seconds", "2")),
				Long.parseLong(PropertiesManager.get(DISPATCHER_PREFIX + "retry_seconds", "60")),
				Long.parseLong(PropertiesManager.get(DISPATCHER_PREFIX + "scan_millis", "1000")),
				Integer.parseInt(PropertiesManager.get(DISPATCHER_PREFIX + "batch_size", "100")));
	}

	ToAtgSftpDispatcher(Path sourceDirectory, long settleSeconds, long retrySeconds) {
		this(sourceDirectory, settleSeconds, retrySeconds, 1000L, 100);
	}

	ToAtgSftpDispatcher(Path sourceDirectory, long settleSeconds, long retrySeconds,
			long scanMillis, int batchSize) {
		if (settleSeconds < 1 || retrySeconds < 1 || scanMillis < 100 || batchSize < 1) {
			throw new IllegalArgumentException(
					"settle_seconds, retry_seconds and batch_size must be greater than zero; scan_millis must be at least 100");
		}
		this.sourceDirectory = sourceDirectory;
		this.processedDirectory = sourceDirectory.resolve(
				PropertiesManager.get(DISPATCHER_PREFIX + "processed_directory", "processed"));
		this.stateDirectory = sourceDirectory.resolve(".dispatch-state");
		this.settleMillis = TimeUnit.SECONDS.toMillis(settleSeconds);
		this.retryMillis = TimeUnit.SECONDS.toMillis(retrySeconds);
		this.scanMillis = scanMillis;
		this.batchSize = batchSize;
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
			LOGGER.info("Watching ToATG directory: " + sourceDirectory + "; batchSize=" + batchSize
					+ "; scanMillis=" + scanMillis);
			processPendingFiles();

			while (!Thread.currentThread().isInterrupted()) {
				WatchKey key = watcher.poll(scanMillis, TimeUnit.MILLISECONDS);
				if (key != null) {
					for (WatchEvent<?> event : key.pollEvents()) {
						// Events only wake the loop early. The complete scan below also
						// handles network filesystems that do not propagate every event.
						event.kind();
					}
					if (!key.reset()) {
						throw new IOException("ToATG directory is no longer available: " + sourceDirectory);
					}
				}
				processPendingFiles();
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
		Set<Path> currentFiles = new HashSet<>(pending);
		observations.keySet().retainAll(currentFiles);
		retryAfter.keySet().retainAll(currentFiles);

		long now = System.currentTimeMillis();
		List<Path> stableFiles = new ArrayList<>();
		for (Path file : pending) {
			try {
				FileStamp stamp = FileStamp.read(file);
				FileObservation previous = observations.get(file);
				if (previous == null || !previous.stamp.equals(stamp)) {
					observations.put(file, new FileObservation(stamp, now));
					retryAfter.remove(file);
					continue;
				}
				if (now - previous.stableSince >= settleMillis
						&& now >= retryAfter.getOrDefault(file, 0L)) {
					stableFiles.add(file);
				}
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Could not inspect pending file " + file, e);
			}
		}

		for (int from = 0; from < stableFiles.size(); from += batchSize) {
			int to = Math.min(from + batchSize, stableFiles.size());
			processBatch(new ArrayList<>(stableFiles.subList(from, to)));
		}
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

	private void processBatch(List<Path> files) {
		if (files.isEmpty()) {
			return;
		}
		LOGGER.info("Processing batch with " + files.size() + " file(s)");
		sendBatchToDwh(files);
		sendBatchToPricing(files);
		archiveCompletedFiles(files);
	}

	private void sendBatchToDwh(List<Path> files) {
		List<Path> unsent = filesWithoutMarker(files, "dwh");
		if (unsent.isEmpty()) {
			return;
		}

		try (SftpConnection connection = openDwhConnection()) {
			for (int i = 0; i < unsent.size(); i++) {
				Path localFile = unsent.get(i);
				try {
					String remoteFileName = dwhFileName(timestampFromFile(localFile));
					uploadAtomically(connection.sftp, localFile,
							joinRemotePath(connection.remoteDirectory, remoteFileName));
					writeMarker(stateMarker(localFile, "dwh"));
					LOGGER.info("DWH sent: " + localFile.getFileName() + " as " + remoteFileName);
				} catch (Exception e) {
					scheduleRetry(unsent.subList(i, unsent.size()));
					LOGGER.log(Level.SEVERE, "DWH batch interrupted at " + localFile + "; remaining files will be retried", e);
					break;
				}
			}
		} catch (Exception e) {
			scheduleRetry(unsent);
			LOGGER.log(Level.SEVERE, "Could not open or close the DWH SFTP batch connection", e);
		}
	}

	private void sendBatchToPricing(List<Path> files) {
		List<Path> unsent = filesWithoutMarker(files, "pricing");
		if (unsent.isEmpty()) {
			return;
		}

		try (SftpConnection connection = openPricingConnection()) {
			for (int i = 0; i < unsent.size(); i++) {
				Path localFile = unsent.get(i);
				try {
					String remoteFileName = pricingFileName(timestampFromFile(localFile));
					uploadAtomically(connection.sftp, localFile,
							joinRemotePath(connection.remoteDirectory, remoteFileName));
					writeMarker(stateMarker(localFile, "pricing"));
					LOGGER.info("Pricing sent: " + localFile.getFileName() + " as " + remoteFileName);
				} catch (Exception e) {
					scheduleRetry(unsent.subList(i, unsent.size()));
					LOGGER.log(Level.SEVERE, "Pricing batch interrupted at " + localFile + "; remaining files will be retried", e);
					break;
				}
			}
		} catch (Exception e) {
			scheduleRetry(unsent);
			LOGGER.log(Level.SEVERE, "Could not open or close the Pricing SFTP batch connection", e);
		}
	}

	private List<Path> filesWithoutMarker(List<Path> files, String destination) {
		List<Path> result = new ArrayList<>();
		for (Path file : files) {
			if (!Files.exists(stateMarker(file, destination))) {
				result.add(file);
			}
		}
		return result;
	}

	private void scheduleRetry(List<Path> files) {
		long nextAttempt = System.currentTimeMillis() + retryMillis;
		for (Path file : files) {
			retryAfter.put(file, nextAttempt);
		}
	}

	private void archiveCompletedFiles(List<Path> files) {
		for (Path localFile : files) {
			Path dwhMarker = stateMarker(localFile, "dwh");
			Path pricingMarker = stateMarker(localFile, "pricing");
			if (!Files.exists(dwhMarker) || !Files.exists(pricingMarker)) {
				continue;
			}

			Path archived = processedDirectory.resolve(localFile.getFileName());
			try {
				Files.move(localFile, archived, StandardCopyOption.REPLACE_EXISTING);
				observations.remove(localFile);
				retryAfter.remove(localFile);
				LOGGER.info("Dispatch completed; archived in " + archived);
			} catch (IOException e) {
				scheduleRetry(java.util.Collections.singletonList(localFile));
				LOGGER.log(Level.SEVERE, "Both destinations succeeded but the file could not be archived: " + localFile, e);
				continue;
			}

			try {
				Files.deleteIfExists(dwhMarker);
				Files.deleteIfExists(pricingMarker);
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Could not remove completed state markers for " + localFile, e);
			}
		}
	}

	private long timestampFromFile(Path localFile) {
		Matcher matcher = SOURCE_FILE_PATTERN.matcher(localFile.getFileName().toString());
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Unexpected ToATG file name: " + localFile);
		}
		return Long.parseLong(matcher.group(1));
	}

	private SftpConnection openDwhConnection() throws Exception {
		String host = requireProperty("p360.contingency.dwh.host");
		int port = Integer.parseInt(PropertiesManager.get("p360.contingency.dwh.port", "22"));
		String user = requireProperty("p360.contingency.dwh.user");
		Path privateKey = Paths.get(PropertiesManager.get("p360.contingency.dwh.private_key",
				"/home/P360admin/.ssh/id_rsa"));
		String remoteDirectory = requireProperty("p360.contingency.dwh.remote_directory_base");
		long timeout = Long.parseLong(PropertiesManager.get("p360.contingency.dwh.timeout_seconds", "10"));

		SshClient client = SshClient.setUpDefaultClient();
		ClientSession session = null;
		try {
			client.start();
			session = client.connect(user, host, port).verify(timeout, TimeUnit.SECONDS).getSession();
			FileKeyPairProvider keyProvider = new FileKeyPairProvider(privateKey);
			keyProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
			keyProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);
			session.auth().verify(timeout, TimeUnit.SECONDS);
			return new SftpConnection(client, session,
					SftpClientFactory.instance().createSftpClient(session), remoteDirectory);
		} catch (Exception e) {
			closeFailedConnection(client, session, e);
			throw e;
		}
	}

	private SftpConnection openPricingConnection() throws Exception {
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
		ClientSession session = null;
		try {
			client.start();
			session = client.connect(username, host, port)
					.verify(connectTimeout, TimeUnit.SECONDS).getSession();
			session.addPasswordIdentity(password);
			session.auth().verify(authTimeout, TimeUnit.SECONDS);
			return new SftpConnection(client, session,
					SftpClientFactory.instance().createSftpClient(session), remoteDirectory);
		} catch (Exception e) {
			closeFailedConnection(client, session, e);
			throw e;
		}
	}

	private void closeFailedConnection(SshClient client, ClientSession session, Exception original) {
		if (session != null) {
			try {
				session.close();
			} catch (IOException closeFailure) {
				original.addSuppressed(closeFailure);
			}
		}
		client.stop();
	}

	private void uploadAtomically(SftpClient sftp, Path localFile, String remoteFile) throws IOException {
		String temporaryRemoteFile = remoteFile + ".part";
		try (OutputStream remote = sftp.write(temporaryRemoteFile,
				SftpClient.OpenMode.Write, SftpClient.OpenMode.Create, SftpClient.OpenMode.Truncate)) {
			Files.copy(localFile, remote);
		}
		removeRemoteFileIfExists(sftp, remoteFile);
		sftp.rename(temporaryRemoteFile, remoteFile);
	}

	private void removeRemoteFileIfExists(SftpClient sftp, String remoteFile) throws IOException {
		try {
			sftp.remove(remoteFile);
		} catch (SftpException e) {
			if (e.getStatus() != SftpConstants.SSH_FX_NO_SUCH_FILE) {
				throw e;
			}
		}
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

	private static final class FileStamp {
		private final long size;
		private final long modified;

		private FileStamp(long size, long modified) {
			this.size = size;
			this.modified = modified;
		}

		private static FileStamp read(Path file) throws IOException {
			return new FileStamp(Files.size(file), Files.getLastModifiedTime(file).toMillis());
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof FileStamp)) {
				return false;
			}
			FileStamp that = (FileStamp) other;
			return size == that.size && modified == that.modified;
		}

		@Override
		public int hashCode() {
			return Long.hashCode(size) * 31 + Long.hashCode(modified);
		}
	}

	private static final class FileObservation {
		private final FileStamp stamp;
		private final long stableSince;

		private FileObservation(FileStamp stamp, long stableSince) {
			this.stamp = stamp;
			this.stableSince = stableSince;
		}
	}

	private static final class SftpConnection implements AutoCloseable {
		private final SshClient client;
		private final ClientSession session;
		private final SftpClient sftp;
		private final String remoteDirectory;

		private SftpConnection(SshClient client, ClientSession session,
				SftpClient sftp, String remoteDirectory) {
			this.client = client;
			this.session = session;
			this.sftp = sftp;
			this.remoteDirectory = remoteDirectory;
		}

		@Override
		public void close() throws IOException {
			try {
				sftp.close();
			} finally {
				try {
					session.close();
				} finally {
					client.stop();
				}
			}
		}
	}
}
