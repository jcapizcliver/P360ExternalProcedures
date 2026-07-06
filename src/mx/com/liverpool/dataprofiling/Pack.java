package mx.com.liverpool.dataprofiling;

public class Pack {

	private final long foil;
	private final String[] pack;
	
	public Pack(final long foil, final String[] pack) {
		this.foil = foil;
		this.pack = pack;
	}
	
	public long getFoil() {
		return this.foil;
	}
	
	public String[] getPack(){
		return this.pack;
	}
	
}
