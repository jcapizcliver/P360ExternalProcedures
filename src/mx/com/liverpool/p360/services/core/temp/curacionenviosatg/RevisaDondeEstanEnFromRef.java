package mx.com.liverpool.p360.services.core.temp.curacionenviosatg;

public class RevisaDondeEstanEnFromRef {

	
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
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "skusProdFromRef").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				if(!"".equals(line))
					skusProd.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "skusArticleFromRef").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				if(!"".equals(line))
					skusArt.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "skusBothFromRef").toFile())))){
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
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUsVariantesPendienteDePublicar.txt").toFile())))){
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
		

		java.util.Set<String> prodIDsQueEnvié = new java.util.TreeSet<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "loenviado", "primer_lote_de_11_mil_no_existen_en_atg_13052026").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					prodIDsQueEnvié.add(line);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "loenviado", "segundo_lote_de_11_mil_no_existen_en_atg_13052026__imagenes_congeladas").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					prodIDsQueEnvié.add(line);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		java.util.Set<String> esecaus = new java.util.TreeSet<>();
		java.util.Map<String, String> pidToSKU = new java.util.HashMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Product2G_SKUs.txt").toFile())))){
			String line = br.readLine();
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					pieces = line.split(",");
					if( pieces.length == 2 && !"".equals(pieces[1]) ) {
						pidToSKU.put(pieces[1], pieces[0]);
						esecaus.add(pieces[1]);
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Tamos con: " + esecaus.size() + " ese ca us.");
		int c = 0;
		int d = 0;
		for(String sku0 : esecaus) {
			if(skusBoth.contains(sku0)) {
				c++;
				losQueEncontré.add(sku0);
			}else if(skusArt.contains(sku0)) {
				c++;
				losQueEncontré.add(sku0);
			}else if(skusProd.contains(sku0)) {
				d++;
				losQueEncontré.add(sku0);
			}
		}
		System.out.println("--->" + c);
		System.out.println("--->" + d);
		
		System.out.println("Map pid to sku: " + pidToSKU.size());
		System.out.println("Los que encontré: " + losQueEncontré.size());
		
		java.util.Map<String, String> skuToEstibo = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Estibos.txt").toFile())))){
			String line = br.readLine();
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					pieces = line.split(":");
					if( pieces.length == 2 && !"".equals(pieces[1]) ) {
						skuToEstibo.put(pieces[1], pieces[0]);
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		String estibo = null;
		java.util.List<String> losQueNoViEnEstibos = new java.util.ArrayList<>();
		java.util.List<String> losQueSí = new java.util.ArrayList<>();
		System.out.println("Los que encontré: " + losQueEncontré.size());
		for(String sku0 : losQueEncontré) {
			estibo = skuToEstibo.get(sku0);
			if(estibo == null)
				losQueNoViEnEstibos.add(sku0);
			else
				losQueSí.add(estibo + ":" + sku0);
		}
		
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Ubicación StiboFeeds.csv").toFile())))){
			losQueSí.forEach(pw::println);
		}catch(java.io.IOException e){
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Sin ubicación StiboFeed.csv").toFile())))){
			losQueNoViEnEstibos.forEach(pw::println);
		}catch(java.io.IOException e){
			e.printStackTrace();
		}
		
		java.util.List<String> encoladosInd = new java.util.ArrayList<>();
		java.util.List<String> encoladosProd = new java.util.ArrayList<>();
		String sku = null;
		for(String envié : prodIDsQueEnvié) {
			sku = pidToSKU.get(envié);
			if(sku != null && !"".equals(sku)) {
				if(skusBoth.contains(sku)) {
					encoladosInd.add(sku);
				}else if(skusProd.contains(sku)) {
					encoladosProd.add(sku);
				}
			}
		}
		System.out.println("Total de skuToEstibo: " + skuToEstibo.size());
		System.out.println("Encolados individuales: " + encoladosInd.size()); 
		System.out.println("Encolados product2g: " + encoladosProd.size()); 
		System.out.println("Encolados productos totales: " + (encoladosInd.size() + encoladosProd.size())); 
		
//		java.util.Set<String> skusQueEnvié = new java.util.TreeSet<>();
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUsProductosRefEnEnvioDeLosNoExistenEnATG.txt").toFile())))){
//			String line = br.readLine();
//			while((line = br.readLine()) != null) {
//				if(!"".equals(line)) {
//					if(skusBoth.contains(line)) {
//						
//					}else if(skusProd.contains(line)) {
//						
//					}
//				}
//			}
//		}catch(java.io.IOException e) {
//			e.printStackTrace();
//		}
		
		
		
		
	}
	
}
