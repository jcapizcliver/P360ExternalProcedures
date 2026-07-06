package mx.com.liverpool.p360.services.core;

public class SimpleDelimitedFileParser {

	
	public static void main(String[] args) {
//		java.util.List<String[]> m1 = new java.util.ArrayList<>();
//		java.nio.file.Path p = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "weird_parser_test_v2.csv");
//		SimpleDelimitedFileParser s = new SimpleDelimitedFileParser('"',',','\\',"\n",java.nio.charset.StandardCharsets.UTF_8,(arr) -> { m1.add(arr); System.out.println( arr.length + " - " + java.util.Arrays.asList(arr)); });
//		s.parse(p);
//		String[] p1 = null;
//		for(int i=0; i<m1.size(); i++) {
//			p1 = m1.get(i);
//			System.out.println((i+1) + ": " + java.util.Arrays.asList(p1));
//		}
//		System.out.println("Done.");
		SimpleDelimitedFileParser s = new SimpleDelimitedFileParser('"',',','\\',"\n",java.nio.charset.StandardCharsets.UTF_8,(arr) -> { System.out.println( arr.length + " - " + java.util.Arrays.asList(arr)); }); // S87363150,"0001;\"98\\\"\";;"
		s.parseString("3135;Reloj de Mesa Howard Miller Davis;;;Servicios DILISA.;;Indirecto;ART. MARKETPLACE;;;Compra única;;;;;0.01;0.01;;;;;\"Caja redonda cepillado de aluminio de este reloj de mesa moderno se encuentra en una, base de cristal cónico negro. El reloj cuenta con una esfera de metal hilado, diamante de corte; un anillo exterior negro mate con marcadores de hora de estilo bar y estilo limpio números romanos en el \\\\\"12\\\\\" y la posición \\\\\"6\\\\\"; y un cristal de vidrio. Pulido hora tono de plata y los minutos con un acento bandas de color negro por la parte media y segundo de plata. fondo cubierto sentido ayuda a proteger la mesa o escritorio. Cuarzo, movimiento operado por baterías incluye una pila de tamaño AA\";");
	}
	
	public interface LineProcessor{
		
		void processLine(String[] values);
	}
	
	private final char delim;
	private final char sep;
	private final char esc;
	private final char[] endLine;
	private final java.nio.charset.Charset charset;
	private final LineProcessor lp;
	
	private int[] elements = new int[1024];
	private int index = 0;
	private int lineCount = 0;
	private int times = 0;
	private short endLineIndex = 0;

	private int escapes = 0;
	
	private long off = 0;        // offset absoluto (chars leídos)
	private int physLine = 1;    // línea física en el archivo
	private int physCol = 0;     // columna física
	
	private long qOff = -1;      // dónde abrió la comilla
	private int qLine = -1;
	private int qCol = -1;
	
	private char[] blanks = new char[1024];
	private int indexBlanks = 0;
	
	private java.util.List<String> pieces = new java.util.ArrayList<>();
	
	private void addLine() {
		if(index > 0 || indexBlanks > 0) {
			addValue();
		}
		lp.processLine(pieces.toArray(new String[] {}));
		pieces.clear();
		lineCount++;
	}
	
	private void addValue() {
		pieces.add( new StringBuilder().append(new String(blanks, 0, indexBlanks)).append( new String( elements, 0, index )).toString());
		index = 0;
		indexBlanks = 0;
		escapes = 0;
	}
	
	private void resize() {
		elements = java.util.Arrays.copyOf(elements, elements.length + 1024);
	}
	
	private void resizeBlanks() {
		blanks = java.util.Arrays.copyOf(blanks, blanks.length + 1024);
	}
	
	private void addBlank(char blank) {
		if(indexBlanks == blanks.length) {
			resizeBlanks();
		}
		blanks[indexBlanks] = blank;
		indexBlanks++;
	}
	
	private int resolveCP(char c, java.io.BufferedReader br) throws java.io.IOException {
		if(Character.isHighSurrogate(c)) {
			int d = read1( br );
			times++;
			if(d != -1 && Character.isLowSurrogate((char)d)) {
				add( Character.toCodePoint(c, (char)d) );
				return d;
			}else {
				add(c);
				if(d != -1) {
					add(d);
					return d;
				}else {
					return -1;
				}
			}
		}else {
			add(c);
		}
		return c;
	}
	
	private void add(int c) {
		if(index == elements.length) {
			resize();
		}
		elements[index] = c;
		index++;
	}
	
	public SimpleDelimitedFileParser(LineProcessor lp) {
		delim = '\"';
		sep = ',';
		esc = '\\';
		endLine = "\r\n".toCharArray();
		charset = java.nio.charset.StandardCharsets.UTF_8;
		this.lp = lp;
	}
	
	public SimpleDelimitedFileParser(char delim, char sep, char esc, String endLine, java.nio.charset.Charset charset, LineProcessor lp) {
		this.delim = delim;
		this.sep = sep;
		this.esc = esc;
		this.endLine = (endLine == null ? "\r\n" : endLine).toCharArray();
		this.charset = charset;
		this.lp = lp;
	}
	
	private int read1(java.io.BufferedReader br) throws java.io.IOException {
	    int pc = br.read();
	    if (pc == -1) return -1;

	    off++;
	    times++;

	    char ch = (char) pc;
	    if (ch == '\n') { physLine++; physCol = 0; }
	    else { physCol++; }

	    return pc;
	}
	
	private IllegalStateException fail(String msg) {
	    long rec = lineCount + 1;
	    int fld = pieces.size() + 1;
	    return new IllegalStateException(msg +
	        " @off=" + off + " line=" + physLine + " col=" + physCol +
	        " rec=" + rec + " field=" + fld +
	        " (times=" + times + ")"
	    );
	}
	
	private IllegalStateException failUnclosedQuote() {
	    long rec = lineCount + 1;
	    int fld = pieces.size() + 1;
	    return new IllegalStateException(
	        "Unclosed quote (opened @off=" + qOff + " line=" + qLine + " col=" + qCol + ")" +
	        " @off=" + off + " line=" + physLine + " col=" + physCol +
	        " rec=" + rec + " field=" + fld
	    );
	}
	
	public void parseString( String str ) {
		boolean a = false;
		boolean isDelim = false;
		char c = 0;
		int cola = 0;
		for(int i = 0; i<str.length(); i++) {
			times++;
			c = (char) str.charAt(i);
			if(c == delim) {
				if(!a) {
					if( index == 0 || elements[index-1] == sep ) {
						a = true;
						qOff = off;
						qLine = physLine;
						qCol = physCol;
						if(indexBlanks > 0) {
							indexBlanks = 0;
						}
					} else {
						// PANIC
						throw fail("Malformed: delimiter not at start of value");
					}
				}else {
					if( index > 0 && (elements[index-1] == esc) ) {
//						index--;
//						add(c);
						if(escapes % 2 == 0) {
							isDelim = true;
						}else {
							index--;
						}
						escapes = 0;
						add(c);
					}else {
						if(!isDelim) {
							isDelim = true;
							add(c);
						}else if( index > 0 && elements[index - 1] == delim ) {
							index--;
							add(c);
							isDelim = false;
						}
					}
				}
			} else if(c == sep) {
				if(a) {
					if(isDelim) {
						isDelim = false;
						a = false;
						index--;
						addValue();
					}else {
						add(c);
					}
				}else {
					addValue();
				}
			} else if( c == endLine[endLineIndex] ) {
				if(a) {
					if(isDelim) {
						if(endLineIndex == endLine.length - 1) {
							isDelim = false;
							a = false;
							index--;
							addLine();
						}
					}else {
						add(c);
					}
				}else {
					if(endLineIndex == endLine.length - 1) {
						addLine();
					}
				}
				endLineIndex++;
				if(endLineIndex == endLine.length) {
					endLineIndex = 0;
				}
			} else if(c == esc) {
//				if(index > 0 && (elements[index-1] == esc)) {
//					index--;
//					add(c);
//				}
				if(index > 0 && (elements[index-1] == esc)) {
					if(cola == 0) {
						index--;
						cola++;
					}else {
						cola--;
					}
				}
				escapes++;
				add(c);
			} else {
				if(isDelim) {
					throw fail("Junk after closing quote");
				}
				if(
						c==7    || c==27 ||
					    c==28   || c==29   || c==30 || c==31 || c==127 ||
					    c==8232 || c==8233 || c == ' ' ||
					    c==8234 || c==8235 || c==8236 || c==8237 || c==8238 ||
					    c==8294 || c==8295 || c==8296 || c==8297 ||
					    c==8203 || c==8204 || c==8205 || c==8288 || c==65279) {
					if(index == 0) {
						addBlank(c);
					} else {
						if(c != 0)
							add(c);
					}
				}
				if(c != esc && cola == 1) {
					cola--;
				}
			}
		}
		if(a) {
			throw failUnclosedQuote();
		}
		if(index > 0 || indexBlanks > 0) {
			addLine();
		}
	}
	
	public void parse( java.nio.file.Path filePath ) {
		boolean a = false;
		boolean isDelim = false;
		try( java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(filePath.toFile()), charset)) ){
			char c = 0;
			int pc = 0;
			int cola = 0;
			while((pc = read1( br )) != -1) {
				times++;
				c = (char) pc;
				if(c == delim) {
					if(!a) {
						if( index == 0 || elements[index-1] == sep ) {
							a = true;
							qOff = off;
							qLine = physLine;
							qCol = physCol;
							if(indexBlanks > 0) {
								indexBlanks = 0;
							}
						} else {
							// PANIC
							throw fail("Malformed: delimiter not at start of value");
						}
					}else {
						if( index > 0 && (elements[index-1] == esc) ) {
							if(escapes % 2 == 0) {
								isDelim = true;
							}else {
								index--;
							}
							escapes = 0;
							add(c);
						}else {
							if(!isDelim) {
								isDelim = true;
								add(c);
							}else if( index > 0 && elements[index - 1] == delim ) {
								index--;
								add(c);
								isDelim = false;
							}
						}
					}
				} else if(c == sep) {
					if(a) {
						if(isDelim) {
							isDelim = false;
							a = false;
							index--;
							addValue();
						}else {
							add(c);
						}
					}else {
						addValue();
					}
				} else if( c == endLine[endLineIndex] ) {
					if(a) {
						if(isDelim) {
							if(endLineIndex == endLine.length - 1) {
								isDelim = false;
								a = false;
								index--;
								addLine();
							}
						}else {
							add(c);
						}
					}else {
						if(endLineIndex == endLine.length - 1) {
							addLine();
						}
					}
					endLineIndex++;
					if(endLineIndex == endLine.length) {
						endLineIndex = 0;
					}
				} else if(c == esc) {
					if(index > 0 && (elements[index-1] == esc)) {
						if(cola == 0) {
							index--;
							cola++;
						}else {
							cola--;
						}
					}
					add(c);
					escapes++;
				} else {
					if(isDelim) {
						throw fail("Junk after closing quote");
					}
					if(
							c==7    || c==27 ||
						    c==28   || c==29   || c==30 || c==31 || c==127 ||
						    c==8232 || c==8233 || c == ' ' ||
						    c==8234 || c==8235 || c==8236 || c==8237 || c==8238 ||
						    c==8294 || c==8295 || c==8296 || c==8297 ||
						    c==8203 || c==8204 || c==8205 || c==8288 || c==65279) {
						if(index == 0) {
							addBlank(c);
						} else {
							if(c != 0)
								add(c);
						}
					}else
						if( resolveCP(c, br) == -1 )
							break;
				}
				if(c != esc && cola == 1) {
					cola--;
				}
			}
			if(a) {
				throw failUnclosedQuote();
			}
			if(index > 0 || indexBlanks > 0) {
				addLine();
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
