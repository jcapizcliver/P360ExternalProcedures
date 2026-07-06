package mx.com.liverpool.p360.services.core.temp.product2g.maintenance4;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.temp.product2g.maintenance4.ProductNameResolver.ResolvedName;

public class UrgentProcessingToPublishCheckProductNameFromVariants {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {

		java.util.List<String> ids = new java.util.ArrayList<>();
		
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUsVariants.txt").toFile())))){
			String line = null;
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("pageSize", "1000");
			StringBuilder sb = new StringBuilder();
			int a = 0;
			java.util.List<String> am = new java.util.ArrayList<>();
			java.util.List<String> invalidValues = new java.util.ArrayList<>();
			while((line = br.readLine()) != null) {
				try {
					Long.parseLong(line);
					sb.append(sb.length() == 0 ? "" : ",").append(line);
					a++;
					if(a % 1000 == 0) {
						qp.put("query", "Article.SKU in (" + sb.toString() + ")");
						rw.collectData("list", "Article", null, "bySearch", qp, row -> {
							am.add( row.getJSONObject("object").getString("id") );
						});
						sb.setLength(0);
					}
				}catch(NumberFormatException e) {
					invalidValues.add(line);
				}
			}
			if(sb.length() > 0) {
				qp.put("query", "Article.SKU in (" + sb.toString() + ")");
				rw.collectData("list", "Article", null, "bySearch", qp, row -> {
					am.add( row.getJSONObject("object").getString("id") );
				});
				sb.setLength(0);
			}
			System.out.println("Valores incorrectos: " + invalidValues.size());
			qp.put("fields", "ProductReference.ReferencedSupplierAid");
			a = 0;
			for(String a0 : am) {
				sb.append(sb.length() == 0 ? "" : ",").append(a0);
				a++;
				if(a % 1000 == 0) {
					qp.put("items", sb.toString());
					rw.collectData("list", "Article", "ProductReference", "byItems", qp, row -> {
						ids.add( row.getJSONArray("values").getString(0) );
					});
					sb.setLength(0);
				}
			}
			if( sb.length() > 0 ) {
				qp.put("items", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byItems", qp, row -> {
					ids.add( row.getJSONArray("values").getString(0) );
				});
				sb.setLength(0);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Read.");
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("pageSize", "5000");
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GLang.ProductName(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('Name',-1)"
			    + ",SimpleProduct2GCharacteristicValue.LookupValue('ProductTypeSAP')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('SB_0002')->LookupValueLang.Name(es)"
				+ ",Product2GExtraData.ItemGroup(MX)->LookupValueLang.Name(es)"
				+ ",Product2GExtraData.ItemGroupS4H(MX)->LookupValueLang.Name(es)"
				+ ",Product2GExtraData.Section(MX)->LookupValueLang.Name(es)"
				+ ",Product2GExtraData.Direccion(MX)->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('Negocio')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('Negocio')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('EXTWG_S4H')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('EXTWG_S4H')->LookupValueLang.Name(es)"
				+ ",Product2GExtraData.BrandName(MX)->LookupValueLang.Name(es)"
				+ ",Product2GExtraData.BRAND_ID_S4H(MX)->LookupValueLang.Name(es)"
				+ ",Product2GExtraData.SupplierID(MX)->LookupValueLang.Name(es)"
				+ ",Product2GExtraData.SupplierPartNumber(MX)"
				+ ",Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroup.Identifier"
				+ ",Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroupLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('GenderAtt')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('GenderAtt')->LookupValueLang.Name(es)"
			);
		
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "TO_APPLY_NAME_as_ProductName.csv").toFile())))){
//			pw.print( rw.getRw().serializeChunk( new String[] { 
//					  "ID"
//					, "ProductName"
//					, "Producto"
//					, "Producto (Suburbia)"
//					, "Grupo de Artículos" 
//					, "Grupo de Artículos (Suburbia)" 
//					, "Dirección" 
//					, "Sección" 
//					, "Negocio"
//					, "NegocioName"
//					, "Negocio (Suburbia)" 
//					, "NegocioName (Suburbia)" 
//					, "Marca"
//					, "Marca (Suburbia)"
//					, "Proveedor"
//					, "Modelo"
//					, "Plantilla"
//					, "PlantillaName"
//					, "Genero"
//					, "GeneroName"
//				} ) );

			pw.print( rw.getRw().serializeChunk( new String[] { 
					 "ProductID"
					,"ProductName"
					,"Name"
					,"ProductTypeSAP"
					,"ProductSuburbia"
					,"ItemGroup"
					,"ItemGroupS4H"
					,"Section"
					,"Direccion"
					,"NegocioCode"
					,"NegocioName"
					,"NegocioSuburbiaCode"
					,"NegocioSuburbiaName"
					,"BrandName"
					,"BrandIDS4H"
					,"Supplier"
					,"Modelo"
					,"PlantillaCode"
					,"Planmtilla"
					,"GenderCode"
					,"GenderName"
				} ) );
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i<ids.size(); i++) {
				sb.append(sb.length() == 0 ? "" : ",").append("'").append(ids.get(i)).append("'@1");
				if( (i+1) % 1000 == 0 ) {
					qp.put("items", sb.toString());
					rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						String cpn = values.getString(1);
						String cn = values.getJSONArray(2).getString(0);
						String name = null;
						ResolvedName rn = ProductNameResolver.resolve(cpn, cn);
						name = rn.value();
						values.put(1, name);
						pw.println( rw.getRw().serializeChunk( toArray(values) ) );
//						pw.println( rw.getRw().serializeChunk( new Object[] { 
//								values.getString(0)
//								, name
//								, values.getJSONArray(3).getString(0)
//								, values.getJSONArray(4).getString(0)
//								, values.getString(5)
//								, values.getString(6)
//								, values.getString(7)
//								, values.getString(8)
//								, values.getJSONArray(9).getString(0)
//								, values.getJSONArray(10).getString(0)
//								, values.getString(11)
//								, values.getString(12)
//						} ) );
					});
					sb.setLength(0);
				}
			}
			if(sb.length() > 0) {
				qp.put("items", sb.toString());
				rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					String cpn = values.getString(1);
					String cn = values.getJSONArray(2).getString(0);
					String name = null;
					ResolvedName rn = ProductNameResolver.resolve(cpn, cn);
					name = rn.value();
					values.put(1, name);
					pw.println( rw.getRw().serializeChunk( toArray( values ) ) );
				});
				sb.setLength(0);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static String[] toArray(org.json.JSONArray values) {
		java.util.List<String> vls = new java.util.ArrayList<>();
		for(int i=0; i<values.length(); i++) {
			vls.add( values.get(i) instanceof org.json.JSONArray ? values.getJSONArray(i).getString(0) : values.getString(i));
		}
		return vls.toArray(new String[] {});
	}
	
}
