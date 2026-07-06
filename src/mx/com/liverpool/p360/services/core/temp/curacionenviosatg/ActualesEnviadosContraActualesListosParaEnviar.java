package mx.com.liverpool.p360.services.core.temp.curacionenviosatg;

public class ActualesEnviadosContraActualesListosParaEnviar {

	
	public static void main(String[] args) {

		java.util.List<String> one = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(
				new java.io.InputStreamReader(
						new java.io.FileInputStream(
								java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "15052026", "ActualmenteCorrectosParaEnviarA_ATG_17_31.txt").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					one.add(line);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		

		try(java.io.BufferedReader br = new java.io.BufferedReader(
				new java.io.InputStreamReader(
						new java.io.FileInputStream(
								java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "14052026", "SoFarSentSKUsVariantes.csv").toFile())))){
			String line = br.readLine();
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "15052026", "NuevosParaEnviar.txt").toFile())))){
				while((line = br.readLine()) != null) {
					if(!"".equals(line)) {
						if(!one.contains(line)) {
							pw.println(line);
						}
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
