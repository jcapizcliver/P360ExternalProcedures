package mx.com.liverpool.p360.services.core.sftp;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleLog;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class ParsersTools {
	
	private final SimpleLog sl;
	private final RESTWrapper rw = new RESTWrapper(); 
	private final RESTWorkshop workshop = rw.getRw();
	
	public ParsersTools(SimpleLog sl) {
		this.sl = sl == null ? new SimpleLog() {

			@Override
			public void log(String message) {
				System.out.println();
			}

			@Override
			public void logE(Exception ex) {
				ex.printStackTrace();
			}
		}
		: sl;
	}

	public String[] checkProductBySKU(String sku) {
		DataRequestor dr = new DataRequestor();
		String resp = dr.productBySKU(new org.json.JSONArray().put(sku));
		if(resp != null) {
			try {
				org.json.JSONObject jr = new org.json.JSONObject(resp);
				org.json.JSONArray items = jr.getJSONArray("items");
				String pid = items.getString(0);
				if(!"".equals(pid)) {
					String[] res = checkProduct(pid);
					return res == null ? null : new String[] { pid, res[0], res[1], res[2], res[3] };
				}
			}catch(org.json.JSONException e) {
				sl.logE(e);
			}
		}
		return null;
	}

	public String[] checkProductBySKUOnP360(String sku) {
		sl.log("Gonna query on P360... " + sku);
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GExtraData.SAPObjectType(MX)->LookupValue.Code"
				+ ",Product2G.Business->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('FotoTomadaLiverpool')->LookupValue.Code"
				+ ",Product2G.CurrentStatus"
			);
		qp.put("query", "Product2G.SKU = " + sku);
		String[] ladata = new String[5];
		ladata[0] = null;
		ladata[1] = null;
		ladata[2] = null;
		ladata[3] = null;
		ladata[4] = null;
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			ladata[0] = values.getString(0);
			ladata[1] = values.getString(1);
			ladata[2] = values.getString(2);
			ladata[3] = values.getJSONArray(3).getString(0);
			ladata[4] = values.getString(4);
			sl.log("From within P360 search: " + values);
		});
		/*
		 *  j.getString("SAPObjectType")
								,j.getString("Business")
								,j.getString("FotoTomadaLiverpool")
								,j.getString("CurrentStatus")
		 *  
		 **/
		return ladata[0] == null ? null : ladata;
	}
	
	public String checkArticleBySKU(String sku) {
		DataRequestor dr = new DataRequestor();
		String resp = dr.articleBySKU(new org.json.JSONArray().put(sku));
		if(resp != null) {
			try {
				org.json.JSONObject r = new org.json.JSONObject(resp);
				org.json.JSONArray items = r.getJSONArray("items");
				org.json.JSONObject j = items.getJSONObject(0);
				return !"".equals( j.getString("article") ) ? j.getString("article") : null;
			}catch(org.json.JSONException e) {
				sl.logE(e);
			}
		}
		return null;
	}
	
	public String[] checkProduct(String id) {
		DataRequestor dr = new DataRequestor();
		try {
				String resp = dr.getProductData(new org.json.JSONArray().put(id));
				if(resp != null) {
					org.json.JSONObject rj = new org.json.JSONObject(resp);
					org.json.JSONArray items = rj.getJSONArray("items");
					org.json.JSONObject j = items.getJSONObject(0);
//					sl.log("Me lo pidieron (producto): " + j);
					if(
						j.has("SAPObjectType") 
						&& !"".equals(j.getString("SAPObjectType")) 
						&& !"02".equals(j.getString("SAPObjectType")))
						return new String[] {
								 j.getString("SAPObjectType")
								,j.getString("Business")
								,j.getString("FotoTomadaLiverpool")
								,j.getString("CurrentStatus")
						};
					else return null;
				}
		}catch(org.json.JSONException e) {
			sl.logE(e);
		}
		return null;
	}
	
	public String[] checkArticle(String id) {
		DataRequestor dr = new DataRequestor();
		String resp = dr.getProductByVariant(new org.json.JSONArray().put(id));
		sl.log("Req " + id + ": " + resp);
		if(resp != null) {
			try {
				org.json.JSONObject rj = new org.json.JSONObject(resp);
				org.json.JSONArray items = rj.getJSONArray("items");
				String pid = items.getString(0);
				if(!"".equals(pid)) {
//					sl.log("pid -->" + pid);
					resp = dr.getProductData(new org.json.JSONArray().put(pid));
					rj = new org.json.JSONObject(resp);
					items = rj.getJSONArray("items");
					org.json.JSONObject j = items.getJSONObject(0);
					sl.log("Me lo pidieron -->" + pid + "<-- (item): " + j);
					return new String[] {
							 pid
							,j.getString("SAPObjectType")
							,j.getString("Business")
							,j.getString("FotoTomadaLiverpool")
							,j.getString("CurrentStatus")
					};
				}
			}catch(org.json.JSONException e) {
				sl.logE(e);
			}
		}
		return null;
	}
	
	public void collectCharacteristicsByEntity(java.util.LinkedList<String> product2G, java.util.LinkedList<String> article) {
		try( java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_entities")) ){
			lns.forEach(s -> {
				String[] pieces = workshop.parseLine(s, "\"", ";", "\\");
				String[] entities = workshop.parseLine(pieces[1], "\"", ",", "\\");
				for(int i=0; i<entities.length; i++) {
					if("Product2G".equals(entities[i])) {
						product2G.addLast(pieces[0]);
					}else if("Article".equals(entities[i])) {
						article.addLast(pieces[0]);
					}
				}
			});
		}catch(java.io.IOException e) {
			sl.logE(e);
		}
	}
	
	public void collectLookupValues(String lkpId, java.util.Map<String, java.util.Map<String, String>> map, java.util.Map<String, java.util.Map<String, String>> mapB, String dataType){
		if("LOOKUP".equals(dataType) && lkpId != null) {
			java.util.Map<String, String> data = null; // map.get(lkpId);
			data = getData(lkpId);
			if(data != null) {
				map.put(lkpId, data);
			}
			data = getDataB(lkpId);
			if(data != null) {
				mapB.put(lkpId, data);
			}
		}
	}
	
	public java.util.Map<String, String> getData(String lkpId){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		if(lkpId != null) {
			int cnt = 0;
			String line = null;
			try(
				java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", lkpId.replaceAll("/", "<::>")).toFile()), java.nio.charset.StandardCharsets.UTF_8))
			){
				String[] pieces = null;
				while((line = br.readLine()) != null) {
					cnt++;
					if(!"".equals(line)) {
						pieces = workshop.parseLine(line, "\"", ";", "\\");
						data.put(pieces[0], pieces[1]);
					}
				}
			}catch(java.io.IOException e) {
//				sl.log(e.getMessage());
			}catch(ArrayIndexOutOfBoundsException e) {
				sl.log("Unable to read (" + cnt + "): -->" + line + "<--");
				sl.log("Retrying with different format...");
				try(
						java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", lkpId.replaceAll("/", "<::>")).toFile()), java.nio.charset.StandardCharsets.UTF_8))
					){
					cnt = 0;
					String[] pieces = null;
					while((line = br.readLine()) != null) {
						cnt++;
						if(!"".equals(line)) {
							pieces = workshop.parseLine(line);
							data.put(pieces[0], pieces[1]);
						}
					}
					sl.log("Restored. " + java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", lkpId.replaceAll("/", "<::>")).toString());
				}catch(java.io.IOException ex) {
					sl.log(e.getMessage());
				}catch(ArrayIndexOutOfBoundsException ex) {
					sl.log("Unable to read (" + cnt + "): -->" + line + "<--");
					return null;
				}
			}
		}
		return data;
	}
	
	public java.util.Map<String, String> getDataB(String lkpId){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		if(lkpId != null) {
			int cnt = 0;
			String line = null;
			try(
				java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", lkpId.replaceAll("/", "<::>")).toFile()), java.nio.charset.StandardCharsets.UTF_8))
			){
				String[] pieces = null;
				while((line = br.readLine()) != null) {
					cnt++;
					if(!"".equals(line)) {
						pieces = workshop.parseLine(line, "\"", ";", "\\");
						data.put(pieces[1], pieces[0]);
					}
				}
			}catch(java.io.IOException e) {
				sl.log(e.getMessage());
			}catch(ArrayIndexOutOfBoundsException e) {
				sl.log("B Unable to read (" + cnt + "): -->" + line + "<--");
				sl.log("B Retrying with different format...");
				try(
						java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", lkpId.replaceAll("/", "<::>")).toFile()), java.nio.charset.StandardCharsets.UTF_8))
					){
					cnt = 0;
					String[] pieces = null;
					while((line = br.readLine()) != null) {
						cnt++;
						if(!"".equals(line)) {
							pieces = workshop.parseLine(line);
							data.put(pieces[1], pieces[0]);
						}
					}
					sl.log("B Restored. " + java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", lkpId.replaceAll("/", "<::>")).toString());
				}catch(java.io.IOException ex) {
					sl.log("B " + e.getMessage());
				}catch(ArrayIndexOutOfBoundsException ex) {
					sl.log("B Unable to read (" + cnt + "): -->" + line + "<--");
					return null;
				}
			}
		}
		return data;
	}
	
	public Object resolveDataType(String charId, String dataType, String lkpId, String value, java.util.Map<String, java.util.Map<String, String>> map, java.util.Map<String, java.util.Map<String, String>> mapB) {
		if(dataType != null && value != null) {
			if("LOOKUP".equals(dataType)) {
				String code = null;
				String label = null;
				if("UnidadDeMedidaPeso".equals(charId)) {
					value = unidadesPeso.get(value);
				}else if("UnidadDeMedidaLongitud".equals(charId)) {
					value = unidadesLongitud.get(value);
				}else if("UnidadDeMedidaVolumen".equals(charId)) {
					value = unidadesVolumen.get(value);
				}else {
					java.util.Map<String, String> lkp = map.get(lkpId);
					if(lkp != null) {
						label = lkp.get(value);
					}
					java.util.Map<String, String> lkpB = mapB.get(lkpId);
					if(lkpB != null && label == null) {
						code = lkpB.get(value);
					}
					if(code == null && label == null) {
						sl.log("Unknown value found: " + value + " for Characteristic: " + charId + ", data for core: " + ( lkp == null ? null : lkp.size() > 20 ? "Too long to show" : lkp));
					}
				}
				if(label == null && code == null)
					return null;
				return new org.json.JSONObject().put( "_code", label != null ? value : code);
			}else if("INTEGER".equals(dataType)) {
				try{
					return new java.math.BigDecimal(value).intValue();
				}catch(NumberFormatException e) {
					sl.logE(e);
				}
			}else if("DECIMAL".equals(dataType)) {
				try {
					return new java.math.BigDecimal(value).floatValue();
				}catch(NumberFormatException e) {
					sl.logE(e);
				}
			}else if("BOOLEAN".equals(dataType)) {
				return Boolean.parseBoolean(value);
			}else if("DATE".equals(dataType)) {
				try{
					return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.text.SimpleDateFormat().parse(value));
				}catch(java.text.ParseException e) {
					sl.logE(e);
				}
			}
		}
		return value;
	}


	private static final java.util.Map<String, String> unidadesPeso = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> unidadesLongitud = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> unidadesVolumen = new java.util.TreeMap<>();

	static {
		unidadesPeso.put("unece.unit.KGM", "KG");
		unidadesPeso.put("unece.unit.GRM", "G");
		unidadesLongitud.put("unece.unit.CMT", "CM");
		unidadesLongitud.put("unece.unit.MTR", "M");
		unidadesLongitud.put("unece.unit.MMT", "MM");
		unidadesVolumen.put("unece.unit.CMQ", "CM3");
		unidadesVolumen.put("unece.unit.LTR", "L");
	}

}
