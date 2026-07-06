package mx.com.liverpool.dataprofiling;
public class BRWrapper {

	private final java.nio.file.Path p;
	private final java.io.BufferedReader br;
	
	public BRWrapper(java.nio.file.Path p, java.io.BufferedReader br) {
		this.p = p;
		this.br = br;
	}
	
	public java.nio.file.Path getPath(){
		return p;
	}
	
	public java.io.BufferedReader getBr(){
		return br;
	}
	
}
