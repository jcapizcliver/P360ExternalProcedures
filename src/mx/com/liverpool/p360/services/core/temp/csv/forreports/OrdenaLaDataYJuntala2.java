package mx.com.liverpool.p360.services.core.temp.csv.forreports;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class OrdenaLaDataYJuntala2 {

	private static final RESTWrapper RW = new RESTWrapper();

	private static final Path TMP_DIR = Paths.get("C:\\opt\\LVP\\desorden\\PROD\\tmp");
	private static final int CHUNK_SIZE = 100_000;

	private static final String INPUT_1_DEFAULT = "C:\\opt\\LVP\\desorden\\PROD\\MD_MainProductArticleData.csv";
	private static final String INPUT_2_DEFAULT = "C:\\opt\\LVP\\desorden\\PROD\\MD_CharacteristicsData.csv";
	private static final String OUTPUT_DEFAULT = "C:\\opt\\LVP\\desorden\\PROD\\LaMasaMD.csv";

	public static void main(String[] args) {
		Path input1 = Paths.get(args.length > 0 ? args[0] : INPUT_1_DEFAULT);
		Path input2 = Paths.get(args.length > 1 ? args[1] : INPUT_2_DEFAULT);
		Path output = Paths.get(args.length > 2 ? args[2] : OUTPUT_DEFAULT);

		Path sorted1 = null;
		Path sorted2 = null;

		try {
			Files.createDirectories(TMP_DIR);

			SortResult left = sortCsv(input1, "mdpad");
			SortResult right = sortCsv(input2, "mdcd");

			sorted1 = left.sortedFile;
			sorted2 = right.sortedFile;

			joinSortedFiles(left, right, output);

			System.out.println("Listo: " + output);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			deleteIfExists(sorted1);
			deleteIfExists(sorted2);
		}
	}

	private static SortResult sortCsv(Path input, String prefix) throws IOException {
		final List<String[]> buffer = new ArrayList<>(CHUNK_SIZE);
		final List<Path> chunkFiles = new ArrayList<>();
		final boolean[] firstRow = new boolean[] { true };
		final String[][] headerBox = new String[1][];

		try {
			SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
				'"',
				',',
				'\\',
				"\n",
				StandardCharsets.UTF_8,
				row -> {
					if (row == null || row.length == 0) {
						return;
					}

					String[] copy = Arrays.copyOf(row, row.length);

					if (firstRow[0]) {
						firstRow[0] = false;
						headerBox[0] = copy;
						return;
					}

					buffer.add(copy);

					if (buffer.size() >= CHUNK_SIZE) {
						try {
							spillChunk(buffer, chunkFiles, prefix);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}
				}
			);

			parser.parse(input);
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}

		if (!buffer.isEmpty()) {
			spillChunk(buffer, chunkFiles, prefix);
		}

		Path sortedFile = TMP_DIR.resolve("LaMasa_" + prefix + "_sorted.dat");
		mergeChunks(chunkFiles, sortedFile);

		return new SortResult(headerBox[0], sortedFile);
	}

	private static void spillChunk(List<String[]> rows, List<Path> chunkFiles, String prefix) throws IOException {
		rows.sort(Comparator.comparing(o -> safeKey(o)));

		Path chunk = Files.createTempFile(TMP_DIR, prefix + "_", ".dat");

		try (
			BufferedWriter bw = Files.newBufferedWriter(chunk, StandardCharsets.UTF_8);
			PrintWriter pw = new PrintWriter(bw)
		) {
			for (String[] row : rows) {
				pw.println(RW.getRw().serializeChunk(escapeVisibleNewlines(row)));
			}
		}

		chunkFiles.add(chunk);
		rows.clear();
	}

	private static void mergeChunks(List<Path> chunkFiles, Path outputFile) throws IOException {
		deleteIfExists(outputFile);

		if (chunkFiles.isEmpty()) {
			Files.createFile(outputFile);
			return;
		}

		PriorityQueue<ChunkCursor> pq = new PriorityQueue<>(Comparator.comparing(o -> safeKey(o.row)));
		List<ChunkCursor> openCursors = new ArrayList<>();

		try (
			BufferedWriter bw = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8);
			PrintWriter pw = new PrintWriter(bw)
		) {
			for (Path chunk : chunkFiles) {
				BufferedReader br = Files.newBufferedReader(chunk, StandardCharsets.UTF_8);
				String[] first = readTempRow(br);
				if (first != null) {
					ChunkCursor cursor = new ChunkCursor(br, chunk, first);
					openCursors.add(cursor);
					pq.add(cursor);
				} else {
					br.close();
					deleteIfExists(chunk);
				}
			}

			while (!pq.isEmpty()) {
				ChunkCursor current = pq.poll();
				pw.println(RW.getRw().serializeChunk(current.row));

				String[] next = readTempRow(current.reader);
				if (next != null) {
					current.row = next;
					pq.add(current);
				} else {
					current.close();
					deleteIfExists(current.file);
				}
			}
		} finally {
			for (ChunkCursor cursor : openCursors) {
				try {
					cursor.close();
				} catch (IOException ignored) {
				}
			}
			for (Path chunk : chunkFiles) {
				deleteIfExists(chunk);
			}
		}
	}

	private static void joinSortedFiles(SortResult left, SortResult right, Path output) throws IOException {
		try (
			BufferedReader br1 = Files.newBufferedReader(left.sortedFile, StandardCharsets.UTF_8);
			BufferedReader br2 = Files.newBufferedReader(right.sortedFile, StandardCharsets.UTF_8);
			BufferedWriter bw = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
			PrintWriter pw = new PrintWriter(bw)
		) {
			if (left.header != null && right.header != null) {
				pw.println(RW.getRw().serializeChunk(concat(left.header, right.header)));
			}

			String[] row1 = readTempRow(br1);
			String[] row2 = readTempRow(br2);

			while (row1 != null && row2 != null) {
				int cmp = safeKey(row1).compareTo(safeKey(row2));

				if (cmp < 0) {
					row1 = readTempRow(br1);
				} else if (cmp > 0) {
					row2 = readTempRow(br2);
				} else {
					String key = safeKey(row1);

					List<String[]> group1 = new ArrayList<>();
					List<String[]> group2 = new ArrayList<>();

					do {
						group1.add(row1);
						row1 = readTempRow(br1);
					} while (row1 != null && key.equals(safeKey(row1)));

					do {
						group2.add(row2);
						row2 = readTempRow(br2);
					} while (row2 != null && key.equals(safeKey(row2)));

					for (String[] leftRow : group1) {
						for (String[] rightRow : group2) {
							pw.println(RW.getRw().serializeChunk(joinForOutput(leftRow, rightRow)));
						}
					}
				}
			}
		}
	}

	private static String[] readTempRow(BufferedReader br) throws IOException {
		String line = br.readLine();
		return line == null ? null : RW.getRw().parseLine(line);
	}

	private static String[] joinForOutput(String[] left, String[] right) {
		String[] joined = new String[left.length + right.length];
		int idx = 0;

		for (String value : left) {
			joined[idx++] = restoreVisibleNewlines(value);
		}
		for (String value : right) {
			joined[idx++] = restoreVisibleNewlines(value);
		}

		return joined;
	}

	private static String[] concat(String[] a, String[] b) {
		String[] result = new String[a.length + b.length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	private static String[] escapeVisibleNewlines(String[] row) {
		String[] copy = new String[row.length];
		for (int i = 0; i < row.length; i++) {
			copy[i] = row[i] == null ? null : row[i].replace("\n", "\\n");
		}
		return copy;
	}

	private static String restoreVisibleNewlines(String value) {
		return value == null ? null : value.replace("\\n", "\n");
	}

	private static String safeKey(String[] row) {
		return row != null && row.length > 0 && row[0] != null ? row[0] : "";
	}

	private static void deleteIfExists(Path path) {
		if (path == null) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
		}
	}

	private static final class SortResult {
		private final String[] header;
		private final Path sortedFile;

		private SortResult(String[] header, Path sortedFile) {
			this.header = header;
			this.sortedFile = sortedFile;
		}
	}

	private static final class ChunkCursor implements AutoCloseable {
		private final BufferedReader reader;
		private final Path file;
		private String[] row;

		private ChunkCursor(BufferedReader reader, Path file, String[] row) {
			this.reader = reader;
			this.file = file;
			this.row = row;
		}

		@Override
		public void close() throws IOException {
			reader.close();
		}
	}
}