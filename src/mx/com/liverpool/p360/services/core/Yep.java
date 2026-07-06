package mx.com.liverpool.p360.services.core;

public class Yep {

//	public static void main(String[] args) {
//		Yep y = new Yep();
//		java.util.regex.Pattern p = java.util.regex.Pattern.compile("(^|\")(.+)((?<!\\\\)\")?;\"?(.+)(?=((?<!\\\\)\"|$))");
//		String[] sample = (
//				"DATA;DATA2\r\n"
//				+ "\"UnDat;o\";\"Razonable;\"\r\n"
//				+ "\"Otro\\\"Datoprr;n;\";Cochinita\r\n"
//				+
//				"1359;\"21\\\"\"").split("\\r\\n");
//		for(String piece : sample) {
//			System.out.println(java.util.Arrays.asList( y.parseLine(piece, "\"", ";", "\\")));
//		}
//	}
	
	public String[] parseLine(String line, String delim, String sep, String esc) {
		String eh = null;
		java.util.LinkedList<String> elchunk = new java.util.LinkedList<>();
		java.util.LinkedList<String> lasPises = new java.util.LinkedList<>();
		boolean a = false;
		StringBuilder left = new StringBuilder();
		StringBuilder right = new StringBuilder();
		int skips = 0;
		for(int i=0; i<line.length(); i++) {
			eh = line.substring(i, i+1);
			if(skips > 0) {
				skips--;
				continue;
			}
			if(eh.equals(delim)) {
				if(a) {
					if( i < line.length() - 1 && delim.equals(line.substring(i+1, i+2)) ) {
						if(!esc.equals(line.subSequence(i-1, i))) {
							skips++;
							elchunk.addFirst(eh);
						}
					}else if(i == line.length() - 1) {
						a = false;
					}else if(line.substring(i+1, i + 2).equals(sep)) {
						a = false;
					}else if(esc.equals(line.subSequence(i-1, i))){
						elchunk.addFirst(eh);
					}else {
						for(int j=0; j<i; j++) {
							left.append(line.substring(j, j+1));
						}
						for(int j=i+1; j<line.length(); j++) {
							right.append(line.substring(j, j+1));
						}
						throw new IllegalStateException("Malformed line found ---->" + left.toString() + "-->" + eh + "<--" + right.toString() + "<----");
					}
				}else {
						a = true;
				}
			}else if(eh.equals(sep)) {
				if(a) {
					elchunk.addFirst(eh);
				}else {
					lasPises.addLast(empty(elchunk));
					elchunk.clear();
				}
			} else if(eh.equals(esc)) {
				if(i == line.length() - 1) {
					for(int j=0; j<i; j++) {
						left.append(line.substring(j, j+1));
					}
					for(int j=i+1; j<line.length(); j++) {
						right.append(line.substring(j, j+1));
					}
					throw new IllegalStateException("Malformed line found ---->" + left.toString() + "-->" + eh + "<--" + right.toString() + "<----");
				}else if(line.substring(i+1, i + 2).equals(esc)) {
					elchunk.addFirst(eh);
					skips++;
//					if(i < line.length() - 2 && delim.equals(line.substring(i+2, i+3))) {
//						elchunk.addFirst(line.substring(i+2, i+3));
//						skips++;
//					}
				}else if(line.substring(i+1, i + 2).equals(delim)) {
					elchunk.addFirst(delim);
					skips++;
				}else {
					// Illegal character escaped.
					for(int j=0; j<i; j++) {
						left.append(line.substring(j, j+1));
					}
					for(int j=i+1; j<line.length(); j++) {
						right.append(line.substring(j, j+1));
					}
					throw new IllegalStateException("Malformed line found ---->" + left.toString() + "-->" + eh + "<--" + right.toString() + "<----");
				}
			} else {
				elchunk.addFirst(eh);
			}
		}
		if(a) {
			throw new IllegalStateException("Malformed line found, missing enclosing character: " + line + "----><----");
		}
		if(!elchunk.isEmpty()) {
			lasPises.addLast(empty(elchunk));
		}else {
			lasPises.addLast("");
		}
		return lasPises.toArray(new String[] {});
	}

	private String empty(java.util.LinkedList<String> elchunk) {
		StringBuilder sb = new StringBuilder();
		String pis = null;
		while(!elchunk.isEmpty()) {
			pis = elchunk.removeLast();
			sb.append(pis);
		}
		return sb.toString();
	}

//	private String emptyness(java.util.LinkedList<String> elchunk, String delim, String escape) {
//		StringBuilder sb = new StringBuilder();
//		String pis = null;
//		String otroPis = null;
//		while(!elchunk.isEmpty()) {
//			pis = elchunk.removeLast();
//			if(pis.equals(delim)) {
//				if(elchunk.isEmpty()) {
//					//PANIC
//				}else {
//					otroPis = elchunk.removeLast();
//					if(!otroPis.equals(escape)) {
//						//PANIC
//					}else {
//						sb.append(pis);
//					}
//				}
//			}else if(pis.equals(escape)) {
//				if(elchunk.isEmpty()) {
//					// PANIC
//				}else {
//					otroPis = elchunk.removeLast();
//					if(otroPis.equals(escape)) {
//						sb.append(pis);
//					}else if(otroPis.equals(delim)) {
//						sb.append(otroPis);
//					}else {
//						// PANIC
//					}
//				}
//			}else {
//				sb.append(pis);
//			}
//		}
//		return sb.toString();
//	}
}
