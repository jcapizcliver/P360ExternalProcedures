import org.json.*;
import java.util.*;
public class JanaRelationshipTest {
 Map<String,String> articleHigherLevelProduct=new TreeMap<>(), articleHigherLevelProductNotReadyYet=new TreeMap<>(), articleSupplierAIDToSKU=new TreeMap<>();
 Map<String,String> qp=new HashMap<>(),qp0=new HashMap<>();
 JSONObject reqSKU=request(),reqSKUA=request();
 List<String> events=new ArrayList<>(); List<JSONObject> written=new ArrayList<>();
 Wrapper rw=new Wrapper(); Admin dr=new Admin();
 static JSONObject request(){return new JSONObject().put("rows",new JSONArray());}
 void log(String x){} boolean isBlank(String x){return x==null||x.trim().isEmpty();}
 String resolveProductIdBySku(String sku, Admin ignored){
   if(!events.contains("Product2G")||!events.contains("Article"))throw new AssertionError("Lookup before creation");
   return sku.equals("missing")?"":"SBB"+sku;
 }
 class Admin {void putSkuSupplierAID(JSONArray a){}}
 class Wrapper {void writeData(String api,String entity,String sub,Map<String,String> q,JSONObject r,java.util.function.Consumer<String> log){
   events.add(sub==null?entity:sub);
   JSONArray rows=r.getJSONArray("rows");
   if(sub!=null){if(rows.length()>1000)throw new AssertionError("Batch overflow"); for(int i=0;i<rows.length();i++)written.add(new JSONObject(rows.getJSONObject(i).toString()));}
   while(rows.length()>0)rows.remove(0);
 }}
 void run(){
			// Persist both endpoints before resolving or writing ProductReference.
			if (reqSKU.getJSONArray("rows").length() > 0) {
				rw.writeData("list", "Product2G", null, qp0, reqSKU, this::log);
			}
			if (reqSKUA.getJSONArray("rows").length() > 0) {
				rw.writeData("list", "Article", null, qp0, reqSKUA, this::log);
			}
			log("\t\tNow placing relationships...");
			org.json.JSONArray items = new org.json.JSONArray();
			org.json.JSONObject item = null;
			org.json.JSONArray columns00 = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid"));
			org.json.JSONArray rows00 = new org.json.JSONArray();
			org.json.JSONObject req = new org.json.JSONObject();
			req.put("columns", columns00);
			req.put("rows", rows00);
			for (java.util.Map.Entry<String, String> entry : articleHigherLevelProduct.entrySet()) {
				item = new org.json.JSONObject();
				item.put("supplierAID", entry.getKey());
				item.put("sku", articleSupplierAIDToSKU.get(entry.getKey()));
				item.put("productNo", entry.getValue());
				items.put(item);
				rows00.put(new org.json.JSONObject()
						.put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@1"))
						.put("qualification", new org.json.JSONObject().put("referencedSupplierAid", entry.getValue()))
						.put("values", new org.json.JSONArray().put(entry.getValue())));
				if (rows00.length() == 1000) {
					rw.writeData("list", "Article", "ProductReference", qp, req, this::log);
				}
			}
			if (rows00.length() > 0) {
				rw.writeData("list", "Article", "ProductReference", qp, req, this::log);
			}
			log("HLPs: " + articleHigherLevelProduct);
			String parentId = null;
			for (java.util.Map.Entry<String, String> entry : articleHigherLevelProductNotReadyYet.entrySet()) {
				// The pending value is SAP's parent SKU, never a P360 Identifier.
				parentId = resolveProductIdBySku(entry.getValue(), dr);
				if (isBlank(parentId) || "null".equals(parentId)) {
					log("Relacion pendiente: Article=" + entry.getKey() + ", SKU padre=" + entry.getValue());
					continue;
				}
				{
					item = new org.json.JSONObject();
					item.put("supplierAID", entry.getKey());
					item.put("sku", articleSupplierAIDToSKU.get(entry.getKey()));
					item.put("productNo", parentId);
					items.put(item);
					rows00.put(new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@1"))
							.put("qualification", new org.json.JSONObject().put("referencedSupplierAid", parentId))
							.put("values", new org.json.JSONArray().put(parentId)));
					if (rows00.length() == 1000) {
						rw.writeData("list", "Article", "ProductReference", qp, req, this::log);
					}
				}
			}
			if (rows00.length() > 0) {
				rw.writeData("list", "Article", "ProductReference", qp, req, this::log);
			}
			dr.putSkuSupplierAID(items);

 }
 public static void main(String[] args){
   JanaRelationshipTest t=new JanaRelationshipTest();
   t.reqSKU.getJSONArray("rows").put(new JSONObject()); t.reqSKUA.getJSONArray("rows").put(new JSONObject());
   t.articleHigherLevelProductNotReadyYet.put("SBB5016514618","5016514609");
   t.articleHigherLevelProductNotReadyYet.put("unknown","missing");
   t.articleHigherLevelProduct.put("individualArticle","individualProduct");
   for(int i=0;i<1001;i++)t.articleHigherLevelProductNotReadyYet.put("A"+i,"P"+i);
   t.run(); if(t.written.size()!=1003)throw new AssertionError("Missing or duplicate references: "+t.written.size());
   boolean found=false;
   for(JSONObject row:t.written){String id=row.getJSONObject("object").getString("id");String value=row.getJSONArray("values").getString(0);
     if(!value.equals(row.getJSONObject("qualification").getString("referencedSupplierAid")))throw new AssertionError("Qualification mismatch");
     if(id.equals("'SBB5016514618'@1")){found=true;if(!value.equals("SBB5016514609"))throw new AssertionError("Raw SKU used");}
     if(id.equals("'unknown'@1"))throw new AssertionError("Unresolved parent written");
   }
   if(!found)throw new AssertionError("Case missing");
   System.out.println("PASS: resolved Identifier, endpoint order, individual mapping, unresolved parent, batching 1001, no duplicates");
 }
}
