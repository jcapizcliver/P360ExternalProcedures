package mx.com.liverpool.p360.services.core.temp.gcp;

import java.io.FileInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.http.HttpTransportOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CollectIDS {

	static final ZoneOffset FIXED_UTC_MINUS_6 = ZoneOffset.ofHours(-6);
	static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	static final DateTimeFormatter DATETIME_IN = new DateTimeFormatterBuilder()
	        .appendPattern("yyyy-MM-dd HH:mm:ss")
	        .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true).optionalEnd()
	        .toFormatter();

	static final DateTimeFormatter TIME_IN = new DateTimeFormatterBuilder()
	        .appendPattern("HH:mm:ss")
	        .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true).optionalEnd()
	        .toFormatter();
	
    public static void main(String[] args) throws Exception {
    	long init0 = System.currentTimeMillis();
        String keyPath  = "C:\\opt\\LVP\\dev\\crp-pro-cx-analitica-f63214cf6e70-pro-gob-prod.json";
        String projectId = "crp-pro-cx-analitica";

        HttpTransportOptions transport = HttpTransportOptions.newBuilder()
                .setHttpTransportFactory(NetHttpTransport::new)
                .build();

        BigQuery bigquery = BigQueryOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(keyPath)))
                .setTransportOptions(transport)
                .build()
                .getService();

        JsonArray out = new JsonArray();
        Page<Dataset> page = bigquery.listDatasets(projectId);
        if (page != null) {
            for (Dataset ds : page.iterateAll()) {
                JsonObject o = new JsonObject();
                o.addProperty("projectId", ds.getDatasetId().getProject());
                o.addProperty("datasetId", ds.getDatasetId().getDataset());
                o.addProperty("friendlyName", ds.getFriendlyName());
                o.addProperty("location", ds.getLocation());
                out.add(o);
            }
        }
        RESTWrapper rw = new RESTWrapper();
        RESTWorkshop w = rw.getRw();

        java.util.LinkedList<String> malitas = new java.util.LinkedList<>();
        for(String table : tables) {
        	try
        	(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "IDs_").toFile()))))
        	{
		        String query = 
		        		"SELECT PIM_PROD_ID FROM `" + table + "` ";
		
		        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query)
//		        		.setJobTimeoutMs(60000l)
		        		.build();
//		        JobInfo jobInfo = JobInfo.newBuilder(queryConfig)
//		            .build();
//		        Job job = bigquery.create(jobInfo);
//		        job = job.waitFor();  // espera a que termine
		        TableResult result = bigquery.query(queryConfig);
		        Schema schema = result.getSchema();
		        java.util.List<Field> fields = schema.getFields();
		        String productId = null;
		        int a = 0;
		        int b = fields.size();
		        String[] arr = new String[b];
		        for (int i = 0; i < b; i++) {
		            if(i <= 9) {
	        			arr[i] = fields.get(i).getName();
	        		} else if(i == 10) {
	        			
	        		} else if(i == 11) {
	        			arr[i-1] = fields.get(i).getName();
	        		}
		            printFieldMeta(fields.get(i), 0);
		        }
		        long counter = 0;
		        System.out.println("No iterating...");
		        for (FieldValueList row : result.iterateAll()) {
		        	for(FieldValue fv : row) {
		        		if(a == 0){
		        			productId = toString( fields.get(a), fv );
		        		}
		        		arr[a] = toString( fields.get(a), fv );
		        		a++;
		        	}
		        	a = 0;
		        	pw.println(productId);
		        	counter++;
		        	if(counter % 100000 == 0) {
		        		System.out.print(".");
		        		if(counter % 1000000 == 0) {
		        			System.out.println(counter);
		        		}
		        	}
		        	a = 0;
		        }
		        System.out.println();
		        System.out.println(counter);
        	}catch(com.google.cloud.bigquery.BigQueryException e) {
        		e.printStackTrace();
        		malitas.addLast(table);
        		break;
        	}
	        System.out.println("~~~");
        }
        System.out.println("Done. " + w.formatTime(System.currentTimeMillis() - init0));
    }
    
    static String toString(Field field, FieldValue v) {
        StandardSQLTypeName t = field.getType().getStandardType();
        if(v == null || v.isNull()) {
        	return null;
        }
        switch (t) {
            case TIMESTAMP: {
                long micros = v.getTimestampValue();
                long secs   = Math.floorDiv(micros, 1_000_000);
                int  nanos  = (int) ((micros % 1_000_000) * 1_000);
                Instant inst = Instant.ofEpochSecond(secs, nanos);
                return inst.atOffset(FIXED_UTC_MINUS_6).format(OUT_FMT);
            }
            case DATETIME: {
                String s = v.getStringValue();
                LocalDateTime ldt = LocalDateTime.parse(s, DATETIME_IN);
                return ldt.atOffset(FIXED_UTC_MINUS_6).format(OUT_FMT);
            }
            case DATE: {
                LocalDate d = LocalDate.parse(v.getStringValue());
                return d.atTime(LocalTime.MIDNIGHT).atOffset(FIXED_UTC_MINUS_6).format(OUT_FMT);
            }
            case TIME: {
                LocalTime tIn = LocalTime.parse(v.getStringValue(), TIME_IN);
                LocalDate base = LocalDate.of(1970, 1, 1);
                return LocalDateTime.of(base, tIn).atOffset(FIXED_UTC_MINUS_6).format(OUT_FMT);
            }
            case BOOL:    return String.valueOf( v.getBooleanValue() );
            case INT64:   return String.valueOf( v.getLongValue() );
            case FLOAT64: return String.valueOf( v.getDoubleValue() );
            case NUMERIC:
            case BIGNUMERIC: return v.getStringValue();
            case BYTES:   return java.util.Base64.getEncoder().encodeToString( v.getBytesValue() );
            case STRUCT:  return String.valueOf( v.getRecordValue() );
            case ARRAY:   return String.valueOf( v.getRepeatedValue() );
            default:
                return v.getStringValue();
        }
    }
    
    private static void printFieldMeta(Field field, int level) {
        String indent = "  ".repeat(level);
        StandardSQLTypeName t = field.getType().getStandardType();
        String mode = field.getMode() == null ? "NULLABLE" : field.getMode().name();

        String typeStr;
        if (t == StandardSQLTypeName.NUMERIC) {
            long p = field.getPrecision() == null ? 38 : field.getPrecision();
            long s = field.getScale() == null ? 9  : field.getScale();
            typeStr = "NUMERIC(" + p + "," + s + ")";
        } else if (t == StandardSQLTypeName.BIGNUMERIC) {
            long p = field.getPrecision() == null ? 76 : field.getPrecision();
            long s = field.getScale() == null ? 38 : field.getScale();
            typeStr = "BIGNUMERIC(" + p + "," + s + ")";
        } else {
            typeStr = t.name();
        }

        System.out.printf("%s- %s : %s [%s]%s%s%s%s%n",
            indent, field.getName(), typeStr, mode,
            field.getDescription() != null ? "  desc=\"" + field.getDescription() + "\"" : "",
            (field.getPolicyTags() != null && field.getPolicyTags().getNames() != null)
                ? "  policyTags=" + String.join(",", field.getPolicyTags().getNames()) : "",
            field.getMaxLength() != null ? "  maxLength=" + field.getMaxLength() : "",
            field.getCollation() != null ? "  collation=" + field.getCollation() : ""
        );

        if (t == StandardSQLTypeName.STRUCT /* || t == StandardSQLTypeName.RECORD */) {
            for (Field sf : field.getSubFields()) {
                printFieldMeta(sf, level + 1);
            }
        }
    }
    
    private static final String[] tables = (
//    		"crp-pro-dwh-semanticagold.EIL_DP_SNBW.VDIM_SAPBW_PRODUCTO"
    		"crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_ASSET_URL"
//    		"crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET"
//	    		  "crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_JERARQUIA_TIPO\r\n"
//    		"crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_ATRIB_METAATRIBUTO\r\n"
//	    		+ "crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_NIVEL_ATRIBUTO\r\n"
//	    		+ "crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_PROD_TIPO\r\n"
//    	        + "crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_ATRIBUTO\r\n"
//    	        + "crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_JERARQUIA\r\n"

//    	        + "crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_JER_NIVEL_PROD\r\n"
//    	        + "crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_NIVEL\r\n"
    	        
//    	        + "crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_PRODUCTO"
//crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET

//    	      "crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_ATRIBUTO"
//    		+ "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET\r\n"
//    		+ "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET_ASSET\r\n"
//    		+ "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_AI_PLNTILLA_PROD\r\n"
//    		+ "crp-pro-dwh-semanticagold.MUS_PRO_DWH_VIEWS_ODS.VDIM_PIM_ATRIB_ATTRIBUTEGROUPLIST\r\n"
//    		+ "crp-pro-dwh-semanticagold.MUS_PRO_DWH_VIEWS_ODS.VDIM_PIM_LISTSOFVALUES_VALIDATION\r\n"
//    		+ "crp-pro-dwh-semanticagold.MUS_PRO_DWH_VIEWS_ODS.VDIM_PIM_ATRIB_ATTRIBUTELIST_ATRIB\r\n"
//    		+ "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_AI_PLNTILLA_PROD\r\n"
//    		+ "crp-pro-cx-semantica.mus_pro_shared_views.VDIM_PRODUCTOS_PIM_JER"
    	).split("\r\n");

    private static final java.util.Set<String> alreadyExistingAttributesForLastID = new java.util.TreeSet<>( java.util.Arrays.asList(("esdeEntregadeMercancIa\r\n"
    		+ "ZNTGCJ\r\n"
    		+ "StateSKU\r\n"
    		+ "Direction\r\n"
    		+ "TAXESSAP\r\n"
    		+ "ImpuestoALaVenta\r\n"
    		+ "WF_Date_created_rejected\r\n"
    		+ "SAP_BEHVO\r\n"
    		+ "BaseUnitOfMeasure\r\n"
    		+ "WESCH\r\n"
    		+ "ProductWidth\r\n"
    		+ "ProductTypeSAP2\r\n"
    		+ "AssetRejectionMessage\r\n"
    		+ "ZBRGCJ\r\n"
    		+ "MainBarCode\r\n"
    		+ "AE485\r\n"
    		+ "Coleccion\r\n"
    		+ "NeckAtt\r\n"
    		+ "VOLUMAtt\r\n"
    		+ "ZLAECJ\r\n"
    		+ "ColoursLiverpoolAtt\r\n"
    		+ "SAPObjectType\r\n"
    		+ "FitVaD\r\n"
    		+ "PrecioSugeridocIVA\r\n"
    		+ "TypeMainBarCode\r\n"
    		+ "IndicadordeImpuesto\r\n"
    		+ "SupplierName\r\n"
    		+ "CuffAtt\r\n"
    		+ "TecnologiaDeLaPrendaVaD\r\n"
    		+ "AE488\r\n"
    		+ "Section\r\n"
    		+ "ProductName\r\n"
    		+ "ZBRECJ\r\n"
    		+ "ZHOECJ\r\n"
    		+ "BrandNameATG\r\n"
    		+ "ItemGroup2\r\n"
    		+ "ZMEACJ\r\n"
    		+ "ProductTypeSAP\r\n"
    		+ "PublicarEnATG\r\n"
    		+ "OccasionAtt\r\n"
    		+ "FamilyDescription\r\n"
    		+ "ProductDepth\r\n"
    		+ "TextoAdicional\r\n"
    		+ "clothingSize\r\n"
    		+ "PesoBruto\r\n"
    		+ "MesdeEntregadeMuestra\r\n"
    		+ "SleeveAtt\r\n"
    		+ "ProductType\r\n"
    		+ "CountryOfOrigin\r\n"
    		+ "WHERL\r\n"
    		+ "ZNUMV\r\n"
    		+ "ProductHeight\r\n"
    		+ "Temporada\r\n"
    		+ "FotoTomadaLiverpool\r\n"
    		+ "TImportacion\r\n"
    		+ "ItemGroup\r\n"
    		+ "InstruccionesDeCuidadoVaD\r\n"
    		+ "ProductTypeSAPTEMP\r\n"
    		+ "isMarketPlace\r\n"
    		+ "SupplierPartNumber\r\n"
    		+ "ProductWeight\r\n"
    		+ "Negocio\r\n"
    		+ "CostobrutoSinIVA\r\n"
    		+ "SKUCreationDate\r\n"
    		+ "SAP_ZZCOMA\r\n"
    		+ "SkuType\r\n"
    		+ "ZVOLCJ\r\n"
    		+ "EstatusPropuesta\r\n"
    		+ "BuyerRejectionMessage\r\n"
    		+ "PatternAtt\r\n"
    		+ "DisplayOrder\r\n"
    		+ "BrandOwner\r\n"
    		+ "GradoDemoda\r\n"
    		+ "AnoEstacion\r\n"
    		+ "SistemaOrigen\r\n"
    		+ "BrandName\r\n"
    		+ "SupplierID\r\n"
    		+ "DisplayGroupOrder\r\n"
    		+ "MenSizeAt").split("\\r\\n")) );
    
}
