package mx.com.liverpool.p360.services.core.temp.curacionenviosatg.remanentes;

public class RevisaSKUsEnListas {

	
	public static void main(String[] args) {
		java.util.List<String> skusProd = new java.util.ArrayList<>();
		java.util.List<String> skusArt = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "salidas", "SKUsProduct2G.csv").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				if(!"".equals(line))
					skusProd.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "salidas", "SKUsArticle.csv").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				if(!"".equals(line))
					skusArt.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		String[] prodRef = skusProd.toArray(new String[] {});
		String[] artRef = skusArt.toArray(new String[] {});
		java.util.Arrays.sort(prodRef);
		java.util.Arrays.sort(artRef);
		skusProd.clear();
		skusProd = new java.util.ArrayList<>();
		skusArt.clear();
		skusArt = new java.util.ArrayList<>();
		java.util.List<String> none = new java.util.ArrayList<>();
		java.util.List<String> both = new java.util.ArrayList<>();
		boolean isProd = false;
		boolean isArt = false;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "Remanentes", "Remanentes.csv").toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					if(line.contains("SB")) {
						System.out.println(line);
					}
					if(java.util.Arrays.binarySearch(prodRef, line) > -1) {
						isProd = true;
					}
					if( java.util.Arrays.binarySearch(artRef, line) > -1 ) {
						isArt = true;
					}
					if(isProd && isArt) {
						both.add(line);
					}else if(isProd) {
						skusProd.add(line);
					}else if(isArt) {
						skusArt.add(line);
					}else {
						none.add(line);
					}
					isProd = false;
					isArt = false;
				}
			}
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "Remanentes", "skusProdFromRef").toFile())))){
				skusProd.forEach(pw::println);
			}
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "Remanentes", "skusArticleFromRef").toFile())))){
				skusArt.forEach(pw::println);
			}
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "Remanentes", "skusNoneFromRef").toFile())))){
				none.forEach(pw::println);
			}
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "Remanentes", "skusBothFromRef").toFile())))){
				both.forEach(pw::println);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
