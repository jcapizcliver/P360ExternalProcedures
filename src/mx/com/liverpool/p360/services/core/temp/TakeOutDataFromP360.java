package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class TakeOutDataFromP360 {

	private static RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		long init = System.currentTimeMillis();

		System.out.println("Elapsed time: " +  workshop.formatTime(System.currentTimeMillis() - init) );
	}

	private static void deleteData() {
		RESTWorkshop rw = new RESTWorkshop();
		rw.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");

		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		qp.put("query", "not Article.SupplierAID is empty");
		System.out.println(rw.makeRequest("DELETE", "/list/Article/bySearch", qp, null));

		qp.put("query", "not Product2G.ProductNo is empty");
		System.out.println(rw.makeRequest("DELETE", "/list/Product2G/bySearch", qp, null));

		qp.clear();

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;

		qp.put("fields", "StructureGroup.Identifier,StructureGroup.CharacteristicCategories->LookupValue.Code");
		qp.put("pageSize", "900");
		qp.put("query", "not StructureGroup.CharacteristicCategories is empty");

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\data\\structureGroupCategories.dat")))) {
			String line = null;
			rows = new org.json.JSONArray();
			while((line = br.readLine()) != null) {
				response = new org.json.JSONObject(line);
				response.put("values", new org.json.JSONArray().put(new org.json.JSONArray()));
				rows.put(response);
			}
			System.out.println(workshop.makeRequest("POST", "/list/StructureGroup", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroup.CharacteristicCategories"))).put("rows", rows).toString()));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}

	private static void structureGroupCategories() {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "not Structure.Identifier is empty");
		qp.put("fields", "Structure.Identifier");
		qp.put("pageSize", "900");

		java.util.LinkedList<String> ids = new java.util.LinkedList<>();

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Structure/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				ids.addLast(values.getString(0));
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;

		System.out.println("Got: " + ids.size() + " structures to check.");

		qp.clear();

		qp.put("fields", "StructureGroup.Identifier,StructureGroup.CharacteristicCategories->LookupValue.Code");
		qp.put("pageSize", "900");
		qp.put("query", "not StructureGroup.CharacteristicCategories is empty");

		int cnt = 0;

		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\data\\structureGroupCategories.dat"), java.nio.charset.Charset.forName("UTF-8")))){
			for(String id : ids) {
				qp.put("structure", id);
				System.out.println("Structure: " + id);
				do {
					qp.put("startIndex", String.valueOf(currentIndex));
					response = workshop.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
					totalSize = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						currentIndex++;
						pw.println(rows.getJSONObject(i));
						cnt++;
					}
					System.out.println(currentIndex + "/" + totalSize);
				}while(currentIndex < totalSize);
				currentIndex = 0;
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println(cnt + " times.");

	}

	private static void collectLookupValuesOther() {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Lookup.Identifier");
		qp.put("pageSize", "900");
		qp.put("query", "not Lookup.Identifier is empty");

		java.util.LinkedList<String> ids = new java.util.LinkedList<>();

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Lookup/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				ids.addLast(values.getString(0));
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;

		java.util.LinkedList<String> enums = new java.util.LinkedList<>();

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\Infa\\P360\\Dev_PIM\\Repository.repository")))){
			String line = null;
			java.util.regex.Matcher m = null;
			java.util.regex.Pattern p = java.util.regex.Pattern.compile("<param name=\"lookupIdentifier\">([A-Za-z0-9_ -]+)</param>");
			while((line = br.readLine())!= null) {
				m = p.matcher(line);
				if(m.find()) {
					enums.addLast(m.group(1));
				}
			}
		}catch(java.io.IOException e){
			e.printStackTrace();
		}

		System.out.println(enums.size() + " internal Enums");
		qp.clear();
		qp.put("pageSize", "900");
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es),LookupValueLang.Name(en),LookupValueIdentifier.Code(ATG),LookupValue.IsActive");
		qp.put("query", "LookupValue.IsActive = true");

		for(String enm : ids) {
			if(!enums.contains(enm)) {
				try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\data\\general_lookup_" + enm + ".dat")))){
					qp.put("lookup", enm);

					do {
						qp.put("startIndex", String.valueOf(currentIndex));
						response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
						totalSize = response.getInt("totalSize");
						rows = response.getJSONArray("rows");
						for(int i=0; i<rows.length(); i++) {
							currentIndex++;
							values = rows.getJSONObject(i).getJSONArray("values");
							pw.println(values);
						}
						System.out.println(currentIndex + "/" + totalSize);
					}while(currentIndex < totalSize);
					currentIndex = 0;
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private static void collectLookupValues() {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Lookup.Identifier");
		qp.put("pageSize", "900");
		qp.put("query", "not Lookup.Identifier is empty");

		java.util.LinkedList<String> ids = new java.util.LinkedList<>();

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Lookup/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				ids.addLast(values.getString(0));
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;

		java.util.LinkedList<String> enums = new java.util.LinkedList<>();

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\Infa\\P360\\Dev_PIM\\Repository.repository")))){
			String line = null;
			java.util.regex.Matcher m = null;
			java.util.regex.Pattern p = java.util.regex.Pattern.compile("<param name=\"lookupIdentifier\">([A-Za-z0-9_ -]+)</param>");
			while((line = br.readLine())!= null) {
				m = p.matcher(line);
				if(m.find()) {
					enums.addLast(m.group(1));
				}
			}
		}catch(java.io.IOException e){
			e.printStackTrace();
		}

		System.out.println(enums.size() + " internal Enums");
		qp.clear();
		qp.put("pageSize", "900");
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es),LookupValueLang.Name(en),LookupValueIdentifier.Code(ATG),LookupValue.IsActive");
		qp.put("query", "LookupValue.IsActive = true");

		for(String enm : enums) {
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\data\\lookup_" + enm + ".dat")))){
				qp.put("lookup", enm);

				do {
					qp.put("startIndex", String.valueOf(currentIndex));
					response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
					totalSize = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						currentIndex++;
						values = rows.getJSONObject(i).getJSONArray("values");
						pw.println(values);
					}
					System.out.println(currentIndex + "/" + totalSize);
				}while(currentIndex < totalSize);
				currentIndex = 0;
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}

	}

	private static void collectPrimaryEntitiesRelatedToLookupValues() {
		Thread product2Gt = new Thread(TakeOutDataFromP360::product2G);
		Thread articlet = new Thread(TakeOutDataFromP360::article);
		Thread characteristicst = new Thread(TakeOutDataFromP360::characteristics);

		characteristicst.start();
		product2Gt.start();
		articlet.start();

		try {
			product2Gt.join();
			articlet.join();
			characteristicst.join();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void product2G() {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "900");

		java.util.LinkedList<String> productIds = new java.util.LinkedList<>();

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Product2G/byCatalog", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				productIds.addLast(values.getString(0));
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;
		int count = 0;
		qp.clear();
		qp.put("entityFilter", "Product2G,Product2GExtraHeaderData,Product2GExtraHeaderData2,Product2GExtraHeaderData3,Product2GExtraHeaderData4,Product2GExtraHeaderData5,Product2GWorkFlow2,Product2GWorkFlow3,Product2GBusinessSupplierQualified,Product2GBusinessSupplierQualified2,Product2GBusinessSupplierQualified3,Product2GBusinessSupplierQualified4,Product2GBusinessSupplierQualified5,Product2GSupplierLiverpool,Product2GSupplierLiverpool2,Product2GSupplierSuburbia,Product2GSupplierSuburbia2,Product2GSupplierSuburbia3,Product2GSupplierMKP,Product2GSupplierQualified,Product2GBusinessQualified,Product2GLogisticExtension,Product2GLang,Product2GPricePurchase,Product2GPurchase,Product2GPriceSales,Product2GSales,Product2GStructureMap,Product2GStructureGroupMap,Product2GAttribute,Product2GMediaAssetMap,Product2GExtension.Flag,Product2GLog,Product2GOwnLog,Product2GSpecialTreatment,Product2GReference,Product2G2ArticleReference,Product2GCharacteristicValue,SimpleProduct2GCharacteristicValue");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\Product2G_objects.dat")))){
			for(String productId : productIds) {
				count++;
				response = workshop.makeRequest("GET", "/object/Product2G/'" + workshop.encode( productId ) + "'@'MASTER'", qp, null);
				pw.println(response);

				System.out.println("Product2G:\t\t\t" + count + "/" + productIds.size());
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

	private static void article() {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID");
		qp.put("pageSize", "900");

		java.util.LinkedList<String> ids = new java.util.LinkedList<>();

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Article/byCatalog", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				ids.addLast(values.getString(0));
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;
		int count = 0;
		qp.clear();
		qp.put("entityFilter", "Article,ArticleExtraHeaderData,ArticleExtraHeaderData2,ArticleExtraHeaderData3,ArticleExtraHeaderData4,ArticleExtraHeaderData5,ArticleWorkFlow2,ArticleWorkFlow3,ArticleBusinessSupplierQualified,ArticleBusinessSupplierQualified2,ArticleBusinessSupplierQualified3,ArticleBusinessSupplierQualified4,ArticleBusinessSupplierQualified5,ArticleSupplierLiverpool,ArticleSupplierLiverpool2,ArticleSupplierSuburbia,ArticleSupplierSuburbia2,ArticleSupplierSuburbia3,ArticleSupplierMKP,ArticleLogistic,ArticleLogisticExtension,ArticleSurchargeMetal,ArticleSurchargeOther,ArticleLang,ArticlePricePurchase,ArticlePurchase,ArticlePriceSales,ArticleSales,ArticleSupplierRelation,ArticleComponent,ProductReference,ArticleStructureMap,ArticleStructureGroupMap,ArticleCatalogStructureMap,ArticleAttribute,ArticleMediaAssetMap,ArticleExtension.Flag,ArticleExtension.ErpGroupBuyer,ArticleLog,ArticleOwnLog,ArticleSpecialTreatment,Article2Product2GReference,ArticleReference,ArticleMicrobiologics,ArticleDiet,ArticleAllergen,ArticlePreparationServing,ArticleNutrient,ArticleIngredient,ArticleServingQuantity,ArticlePreparation,ArticleNutritionalClaim,ArticleCertifications,ArticlePhysioChemical,ArticleHealthCare,ArticleDairyFishMeatPoultry,ArticleCharacteristicValue,SimpleArticleCharacteristicValue,ArticleProductInformation");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\Article_objects.dat")))){
			for(String currentId : ids) {
				count++;
				response = workshop.makeRequest("GET", "/object/Article/'" + workshop.encode( currentId ) + "'@'MASTER'", qp, null);
				pw.println(response);

				System.out.println("Article:\t\t\t" + count + "/" + ids.size());
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

	private static void characteristics() {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("pageSize", "900");
		qp.put("query", "not Characteristic.Identifier is empty");

		java.util.LinkedList<String> ids = new java.util.LinkedList<>();

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				ids.addLast(values.getString(0));
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;
		int count = 0;
		qp.clear();
		qp.put("entityFilter", "Characteristic,CharacteristicLang,CharacteristicIdentifier");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\Characteristic_objects.dat")))){
			for(String currentId : ids) {
				count++;
				response = workshop.makeRequest("GET", "/object/Characteristic/'" + workshop.encode( currentId ) + "'", qp, null);
				pw.println(response);

				System.out.println("Characteristic:\t\t\t" + count + "/" + ids.size());
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

}
