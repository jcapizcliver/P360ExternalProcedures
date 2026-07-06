package com.example.ei.forfun.logic;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class LoadFechasDeAprobación {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		java.util.Map<String, java.util.List< String >> skuToProduct = new java.util.HashMap<>();
		java.util.Map<String, java.util.List< String >> skuToArticle = new java.util.HashMap<>();
		java.util.List<String> items = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader( new java.io.FileInputStream( java.nio.file.Paths.get(args[0]).toFile()) ))){
			String line = br.readLine();
			String[] pieces = null;
			while( (line = br.readLine()) != null ) {
				if(!"".equals(line)) {
					pieces = line.split(",");
					if("Product2G".equals(pieces[2])) {
						items = skuToProduct.get(pieces[1]);
						if(items == null) {
							items = new java.util.ArrayList<>();
							skuToProduct.put(pieces[1], items);
						}
						items.add(pieces[0]);
					}else {
						if("Article".equals(pieces[2])) {
							items = skuToArticle.get(pieces[1]);
							if(items == null) {
								items = new java.util.ArrayList<>();
								skuToArticle.put(pieces[1], items);
							}
							items.add(pieces[0]);
						}
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.FirstDateApproved")), 2000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) ); 
		RequestHandler rh2 = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.LastDateApproved")), 2000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) ); 
		RequestHandler rhA = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.FirstDateApproved")), 2000, request -> rw.writeData("list", "Article", null, qp, request, System.out::println) ); 
		RequestHandler rhA2 = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.LastDateApproved")), 2000, request -> rw.writeData("list", "Article", null, qp, request, System.out::println) ); 
		java.time.format.DateTimeFormatter inputFormatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm:ss");
		java.time.format.DateTimeFormatter outputFormatter = java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader( new java.io.FileInputStream( java.nio.file.Paths.get(args[1]).toFile()) ))){
			String line = br.readLine();
			String[] pieces = null;
			while( (line = br.readLine()) != null ) {
				if(!"".equals(line)) {
					pieces = line.split("\\|");
					if(!"".equals(pieces[1]) || !"".equals(pieces[2])) {
						items = skuToProduct.get(pieces[0]);
						if(items != null) {
							if(!"".equals(pieces[1])) {
								for(String item : items) {
									rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + item + "'@1")).put("values", new org.json.JSONArray().put( java.time.LocalDateTime.parse( pieces[1], inputFormatter ).format(outputFormatter) )));
								}
							}
							if(!"".equals(pieces[2])) {
								for(String item : items) {
									rh2.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + item + "'@1")).put("values", new org.json.JSONArray().put( java.time.LocalDateTime.parse( pieces[2], inputFormatter ).format(outputFormatter) )));
								}
							}
						}
						items = skuToArticle.get(pieces[0]);
						if(items != null) {
							if(!"".equals(pieces[1])) {
								for(String item : items) {
									rhA.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + item + "'@1")).put("values", new org.json.JSONArray().put( java.time.LocalDateTime.parse( pieces[1], inputFormatter ).format(outputFormatter) )));
								}
							}
							if(!"".equals(pieces[2])) {
								for(String item : items) {
									rhA2.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + item + "'@1")).put("values", new org.json.JSONArray().put( java.time.LocalDateTime.parse( pieces[2], inputFormatter ).format(outputFormatter) )));
								}
							}
						}
					}
				}
			}
			rh.sendData();
			rh2.sendData();
			rhA.sendData();
			rhA2.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
