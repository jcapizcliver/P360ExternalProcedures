package mx.com.liverpool.p360.services.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {

    private final File file;
    private final char enclosingChar;
    private final char separatorChar;
    private final String lineTerminationSequence;
    private BufferedReader reader;
    private boolean isEOF = false;

	private int saltos = 0;

    public CSVReader(String filePath, char enclosingChar, char separatorChar, String lineTerminationSequence) throws FileNotFoundException {
        this.file = new File(filePath);
        this.enclosingChar = enclosingChar;
        this.separatorChar = separatorChar;
        this.lineTerminationSequence = lineTerminationSequence;
        this.reader = new BufferedReader(new FileReader(file));
    }

    public String[] readLine() throws IOException, EOFBeforeCompleteLineException {
        if (isEOF) {
            return null; // File is already closed, no more lines to return.
        }

        StringBuilder currentField = new StringBuilder();
        List<String> fields = new ArrayList<>();
        boolean insideEnclosure = false;
        int prevChar = -1;
		int currentChar = -1;

        while (true) {
            currentChar = reader.read();

            // Handle EOF
            if (currentChar == -1) {
                if (insideEnclosure) {
                    throw new EOFBeforeCompleteLineException("EOF reached before completing the line.");
                }
                if (currentField.length() > 0 || !fields.isEmpty()) {
                    fields.add(currentField.toString());
                }
                closeFile();
                return fields.isEmpty() ? null : fields.toArray(new String[0]);
            }

            char c = (char) currentChar;

            // Handle enclosing character logic
            if (c == enclosingChar) {
                if (insideEnclosure) {
					prevChar = reader.read();
					// Handle EOF
					if (prevChar == -1) {
						throw new EOFBeforeCompleteLineException("EOF reached before completing the line.");
					}
					if( prevChar == enclosingChar) {
						currentField.append(c); // Escaped enclosing character
					} else{
						if(prevChar == separatorChar){
							fields.add(currentField.toString());
							currentField.setLength(0);
							insideEnclosure = !insideEnclosure;
						}else{
							throw new IllegalStateException("Invalid format, found a character ahead the encoling character that is invalid, please review that the file supplied is correctly formatted.");
						}
					}
                } else {
                    insideEnclosure = !insideEnclosure;
                }
            }
            // Handle separator character logic
            else if (!insideEnclosure && c == separatorChar) {
                fields.add(currentField.toString());
                currentField.setLength(0); // Clear current field
            }
            // Handle line termination sequence
            else if (!insideEnclosure && c == lineTerminationSequence.charAt(0)) {
                boolean matches = true;
                StringBuilder terminationBuffer = new StringBuilder();
                terminationBuffer.append(c);

                for (int i = 1; i < lineTerminationSequence.length(); i++) {
                    currentChar = reader.read();
                    if (currentChar == -1 || ((char) currentChar != lineTerminationSequence.charAt(i))) {
                        matches = false;
                        break;
					}else{
						terminationBuffer.append((char) currentChar);
					}
                }

                if (matches) {
                    fields.add(currentField.toString());
					saltos ++;
                    break; // Line termination sequence matched
                } else {
                    currentField.append(terminationBuffer); // Append unmatched sequence
                }
            }
            // Append character to current field
            else {
				if( prevChar != -1 && prevChar == lineTerminationSequence.charAt(0) && c == lineTerminationSequence.charAt(1) ) {
					saltos++;
				}
                currentField.append(c);
            }
        }

        return fields.toArray(new String[0]);
    }

	public int getSaltos(){
		return saltos;
	}

    private void closeFile() throws IOException {
        if (reader != null) {
            reader.close();
            reader = null;
            isEOF = true;
        }
    }

    public static class EOFBeforeCompleteLineException extends Exception {
        public EOFBeforeCompleteLineException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {
        try {
            CSVReader fileReader = new CSVReader("D:\\tmp\\Data Dictionary (Entregable) - Data Element List.csv", '"', ',', "\r\n");
            String[] line;
			String[] prev = null;
			int cnt = 0;
			java.util.LinkedList<String> header = new java.util.LinkedList<>();
			int a = 0;
			while ((line = fileReader.readLine()) != null) {
				cnt++;
				if(header.isEmpty()){
					for (String element : line) {
						if("".equals(element)){
							a++;
							header.addLast("Column_" + a);
						}else{
							header.addLast(element);
						}
					}
				}
				if(prev != null){
					System.out.println("Header len: " + header.size() + ", current length: " + line.length);
					if(header.size() != line.length){
						throw new IllegalStateException("String line not complete (at " + fileReader.getSaltos() + "): " + "");
					}
				}
				prev = line;
            }
			System.out.println("Read: " + cnt + " lines.");
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        } catch (EOFBeforeCompleteLineException e) {
            System.err.println("Incomplete line: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
        }
    }
}
