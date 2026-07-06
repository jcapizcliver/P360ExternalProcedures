package mx.com.liverpool.p360.services.core.temp.curacionenviosatg.remanentes;

public class RevisaDondeEstanEnFromRefParaRemanentes {

	
	public static void main(String[] args) {
		/**
		 * 
		 *	Las listas de abajo, son el resultado de haber identificado todo lo de los cortes en los SKUs de P360 
		 * 
		 * 
		 ****/
		java.util.List<String> skusProd = new java.util.ArrayList<>();
		java.util.List<String> skusArt = new java.util.ArrayList<>();
		java.util.List<String> skusBoth = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "Remanentes", "skusProdFromRef").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				if(!"".equals(line))
					skusProd.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "Remanentes", "skusArticleFromRef").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				if(!"".equals(line))
					skusArt.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "Remanentes", "skusBothFromRef").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				if(!"".equals(line))
					skusBoth.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("RefTot: " + ( skusBoth.size() + skusArt.size() + skusProd.size() ));
		
		java.util.Set<String> losQueEncontré = new java.util.TreeSet<>();
		java.util.Set<String> skusVariantesEnviadas = new java.util.TreeSet<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "Remanentes", "Remanentes.csv").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					skusVariantesEnviadas.add(line);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		int a = 0;
		int b = 0;
		for(String sku0 : skusVariantesEnviadas) {
			if(skusBoth.contains(sku0)) {
				a++;
				losQueEncontré.add(sku0);
			}else if(skusArt.contains(sku0)) {
				a++;
				losQueEncontré.add(sku0);
			}else if(skusProd.contains(sku0)) {
				b++;
				losQueEncontré.add(sku0);
			}
		}
		System.out.println("--->" + a);
		System.out.println("--->" + b);
		

	}
	
}
