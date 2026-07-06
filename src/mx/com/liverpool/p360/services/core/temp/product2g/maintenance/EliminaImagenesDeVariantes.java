package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class EliminaImagenesDeVariantes extends RESTWrapper {

	public static void main(String[] args) {
		EliminaImagenesDeVariantes s = new EliminaImagenesDeVariantes();
		java.util.concurrent.ConcurrentLinkedQueue<String> a = new java.util.concurrent.ConcurrentLinkedQueue<>();
		try(java.util.stream.Stream<String> stream = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "migración", "to_delete_media_assets"))){
			stream.parallel().forEach(a::add);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		int counter = 0;
		int i = 0;
		StringBuilder sb = new StringBuilder();
		for(String productNo : a) {
			sb.append(i == 0 ? "" : ",");
			sb.append("'");
			sb.append(productNo);
			sb.append("'@1");
			i++;
			counter++;
			if(i == 200) {
				s.deleteAssets( s.collectVariants(sb.toString()) );
				s.deleteAssetsProduct( sb.toString() );
				sb.setLength(0);
				i = 0;
				System.out.println(counter + "/" + a.size());
			}
		}
		if(i > 0) {
			s.deleteAssets( s.collectVariants(sb.toString()) );
			s.deleteAssetsProduct( sb.toString() );
			sb.setLength(0);
			i = 0;
		}
		System.out.println(counter + "/" + a.size());
	}
	
	public void deleteAssetsProduct(String items) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("items",  items);
		qp.put("fields", "Product2GCharacteristicValue.RecordKey");
		qp.put("qualificationFilter", "rootCharacteristic('LiverpoolManual'),characteristic('LiverpoolManual_URL')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('LiverpoolManual'),characteristic('LiverpoolManual_Name')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('LiverpoolManual'),characteristic('LiverpoolManual_Status')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('LiverpoolManual'),characteristic('LiverpoolManual')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		
		qp.put("qualificationFilter", "rootCharacteristic('ProductVideo'),characteristic('ProductVideo_URL')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductVideo'),characteristic('ProductVideo_Name')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductVideo'),characteristic('ProductVideo_Status')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductVideo'),characteristic('ProductVideo')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		
		qp.put("qualificationFilter", "rootCharacteristic('OwnersManual'),characteristic('OwnersManual_URL')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('OwnersManual'),characteristic('OwnersManual_Name')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('OwnersManual'),characteristic('OwnersManual_Status')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('OwnersManual'),characteristic('OwnersManual')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		
		qp.put("qualificationFilter", "rootCharacteristic('NOM'),characteristic('NOM_URL')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('NOM'),characteristic('NOM_Name')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('NOM'),characteristic('NOM_Status')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('NOM'),characteristic('NOM')");
		deleteData ("list", "Product2G", "Product2GCharacteristicValue", "byItems", qp, System.out::println);
	}
	
	public void deleteAssets(String items) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("items",  items);
		qp.put("fields", "ArticleCharacteristicValue.RecordKey");
		qp.put("qualificationFilter", "rootCharacteristic('ProductImage'),characteristic('ProductImage_URL')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImage'),characteristic('ProductImage_Name')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImage'),characteristic('ProductImage_Status')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImage'),characteristic('ProductImage')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageDetail'),characteristic('ProductImageDetail_URL')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageDetail'),characteristic('ProductImageDetail_Name')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageDetail'),characteristic('ProductImageDetail_Status')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageDetail'),characteristic('ProductImageDetail')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageSmosh'),characteristic('ProductImageSmosh_URL')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageSmosh'),characteristic('ProductImageSmosh_Name')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageSmosh'),characteristic('ProductImageSmosh_Status')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageSmosh'),characteristic('ProductImageSmosh')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		
		qp.put("qualificationFilter", "rootCharacteristic('Illustration'),characteristic('Illustration_URL')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('Illustration'),characteristic('Illustration_Name')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('Illustration'),characteristic('Illustration_Status')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('Illustration'),characteristic('Illustration')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
	}
	
	public void deleteAssets2(String items) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("items",  items);
		qp.put("fields", "ArticleCharacteristicValue.RecordKey");
		qp.put("qualificationFilter", "rootCharacteristic('ProductImage2'),characteristic('ProductImage_URL2')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImage2'),characteristic('ProductImage_Name2')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImage2'),characteristic('ProductImage2')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageDetail2'),characteristic('ProductImageDetail_URL2')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageDetail2'),characteristic('ProductImageDetail_Name2')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageDetail2'),characteristic('ProductImageDetail2')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageSmosh2'),characteristic('ProductImageSmosh_URL2')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageSmosh2'),characteristic('ProductImageSmosh_Name2')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('ProductImageSmosh2'),characteristic('ProductImageSmosh')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		
		qp.put("qualificationFilter", "rootCharacteristic('Illustration2'),characteristic('Illustration_URL2')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('Illustration2'),characteristic('Illustration_Name2')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
		qp.put("qualificationFilter", "rootCharacteristic('Illustration2'),characteristic('Illustration')");
		deleteData ("list", "Article", "ArticleCharacteristicValue", "byItems", qp, System.out::println);
	}
	
	public String collectVariants(String products){
		java.util.LinkedList<String> ids = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID");
		qp.put("products", products);
		qp.put("pageSize", "5000");
		collectData("list", "Article", null, "byProducts", qp, row->ids.addLast(row.getJSONArray("values").getString(0)), System.out::println);
		StringBuilder sb = new StringBuilder();
		int a = 0;
		for(String id : ids) {
			sb.append(a == 0 ? "" : ",");
			sb.append("'");
			sb.append(id);
			sb.append("'@1");
			a++;
		}
		return sb.toString();
	}
	
}
