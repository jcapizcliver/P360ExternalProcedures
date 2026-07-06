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
import java.util.List;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQuery.TableField;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.MaterializedViewDefinition;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.bigquery.ViewDefinition;
import com.google.cloud.http.HttpTransportOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class GrabLaDataDelWarjaus {

	static final ZoneOffset FIXED_UTC_MINUS_6 = ZoneOffset.ofHours(-6);
	static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	private static final DateTimeFormatter BQ_TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static String fmtEpochMillis(Long epochMillis) {
	    if (epochMillis == null) return null;
	    Instant inst = Instant.ofEpochMilli(epochMillis);
	    return inst.atOffset(FIXED_UTC_MINUS_6).format(BQ_TS_FMT);
	}
	
	static final DateTimeFormatter DATETIME_IN = new DateTimeFormatterBuilder()
	        .appendPattern("yyyy-MM-dd HH:mm:ss")
	        .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true).optionalEnd()
	        .toFormatter();

	static final DateTimeFormatter TIME_IN = new DateTimeFormatterBuilder()
	        .appendPattern("HH:mm:ss")
	        .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true).optionalEnd()
	        .toFormatter();
	
    public static void main(String[] args) throws Exception {
        String keyPath  = "C:\\opt\\LVP\\dev\\crp-pro-cx-analitica-f63214cf6e70-pro-gob-prod.json";
        String projectId = "crp-pro-cx-analitica";
//        String keyPath  = "C:\\opt\\LVP\\dev\\crp-pro-cx-analitica-f63214cf6e70-pro-gob-prod.json";
//        String projectId = "crp-pro-cx-semantica";

        HttpTransportOptions transport = HttpTransportOptions.newBuilder()
                .setHttpTransportFactory(NetHttpTransport::new)
                .build();

        BigQuery bigquery = BigQueryOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(keyPath)))
                .setTransportOptions(transport)
                .build()
                .getService()
               ;

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
//        crp-pro-dwh-semanticagold.MUS_PRO_DWH_VIEWS_ODS.VDIM_SKU_VISIBLES_LIV
        printViewMetadata(
      		  bigquery
      		, "crp-pro-dwh-semanticagold"
      		, "MUS_PRO_DWH_VIEWS_ODS"
      		, "VDIM_SKU_VISIBLES_LIV"
      	);
//      System.out.println("\n**** ^^ ****\n");
//      printViewMetadata(
//      		  bigquery
//      		, "crp-pro-dwh-semanticagold"
//      		, "MUS_PRO_DWH_VIEWS_ODS"
//      		, "VDIM_SKU_VISIBLES_LIV"
//      	);
//        printViewMetadata(
//        		  bigquery
//        		, "crp-pro-cx-semantica"
//        		, "mus_pro_gob_producto_prd_views"
//        		, "VW_EU_COB_ATR_ITEMGROUP"
//        	);
//        System.out.println("\n**** ^^ ****\n");
//        printViewMetadata(
//        		  bigquery
//        		, "crp-pro-cx-semantica"
//        		, "mus_pro_gob_producto_prd_views"
//        		, "VW_EU_COB_ATR_PTSAP"
//        	);
//        System.exit(0);
        
        java.util.Map<String, String> skus = new java.util.TreeMap<>();
        java.util.Map<String, String> templates = new java.util.TreeMap<>();
        java.util.LinkedList<String> malitas = new java.util.LinkedList<>();
//        String table =   "crp-pro-dwh-semanticagold.EIL_DP_DWI.DIM_PIM_ASSET_URL";
//        String table =   "crp-qas-dwh-semanticatf.MUS_PRO_DWH_VIEWS_ODS.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET_ASSET";
//        String table =   "crp-pro-dwh-semantica.EIL_DP_VDWH.VDIM_PIM_PROD_ATRIBUTO_DET";
//        String table =   "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET_ASSET";
//        String table = "crp-pro-cx-semantica.mus_pro_gob_producto_prd_views.CIFRAS_ATRIBUTOS_PLANTILLAS";
//        String table = "crp-pro-cx-semantica.mus_pro_gob_producto_prd_views.CIFRAS_ATRIBUTOS_GRUPOS_DE_ARTICULO";
//        String table = "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET";
//        String table = "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET";
//        String table = "select * from `crp-pro-cx-semantica.mus_pro_gob_producto_prd_views.VW_EU_COB_ATR_ITEMGROUP` where pct_cobertura <> 100.0 limit 50";
        String table = "select * from `crp-pro-dwh-semanticagold.MUS_PRO_DWH_VIEWS_ODS.VDIM_SKU_VISIBLES_LIV` limit 50";
    	try
//    	(
//    			java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "___" + table + "___.csv").toFile())))
//    	)
    	{
	        String query = table;
//	        		"SELECT * FROM `" + table + "` LIMIT 2";

	        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query)
	        		.build();
	
	        TableResult result = bigquery.query(queryConfig);
	        Schema schema = result.getSchema();
	        List<Field> fields = schema.getFields();
	        RESTWrapper rw = new RESTWrapper();
	        int a = 0;
	        int b = fields.size();
	        String[] arr = new String[b];
	        for (int i = 0; i < b; i++) {
	            arr[i] = fields.get(i).getName();
	            printFieldMeta(fields.get(i), 0);
	        }
//	        pw.println( rw.getRw().serializeChunk(arr) );
	        System.out.println( rw.getRw().serializeChunk(arr) );
	        String sku = null;
	        String template = null;
	        String parentTemplate = null;
	        for (FieldValueList row : result.iterateAll()) {
	        	for(FieldValue fv : row) {
	        		if(a == 1)
	        			sku = fv.isNull() ? null : fv.getStringValue();
	        		else if(a == 2) {
	        			if(!fv.isNull()) {
	        				parentTemplate =  fv.getStringValue();
	        			}
	        		}
        			arr[a] = toString( fields.get(a), fv );
	        		a++;
	        	}
	        	a = 0;
	            System.out.println( rw.getRw().serializeChunk(arr) );
	        }
	        
	        
    	}catch(com.google.cloud.bigquery.BigQueryException e) {
    		e.printStackTrace();
    		malitas.addLast(table);
//    	}catch(java.io.IOException e) {
//    		e.printStackTrace();
    	}
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
            case BIGNUMERIC: return v.getNumericValue().toString();
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
    
    
    
    private static long exportCoberturaAtributosPlantillaItemGroupToCsv(
            com.google.cloud.bigquery.BigQuery bigquery,
            java.nio.file.Path outputCsv
    ) throws com.google.cloud.bigquery.JobException, InterruptedException, java.io.IOException {

        final String tableFqn = "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET";

        // Nota: para contar "registros" de forma robusta, uso una "llave" que no sea NULL casi nunca.
        // Si PIM_PROD_ID siempre viene, chido. Si a veces viene NULL, caemos a SKU o PADRE_SKU.
        final String sql =
                "WITH base AS ( \n" +
                "  SELECT \n" +
                "    COALESCE(PIM_PROD_ID, CAST(PIM_SKU_CVE AS STRING), CAST(PIM_PADRE_SKU_CVE AS STRING)) AS pk, \n" +
                "    PIM_PLANTILLA_ID AS plantilla_id, \n" +
                "    NULLIF(TRIM(PIM_ATRIB_ITEMGROUP), '') AS itemgroup, \n" +
                "    NULLIF(TRIM(PIM_ATRIB_PRODUCTTYPESAP), '') AS producttypesap, \n" +
                "    PIM_ATRIB_ATRIBUTOS AS atributos \n" +
                "  FROM `crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET` \n" +
                "  WHERE STARTS_WITH(PIM_PLANTILLA_ID, 'EU') \n" +
                "    AND NULLIF(TRIM(PIM_ATRIB_ITEMGROUP), '') IS NOT NULL \n" +
                "    AND NULLIF(TRIM(PIM_ATRIB_PRODUCTTYPESAP), '') IS NOT NULL \n" +
                "), \n" +

                "tot_ig AS ( \n" +
                "  SELECT plantilla_id, itemgroup, COUNT(DISTINCT pk) AS total_registros \n" +
                "  FROM base \n" +
                "  GROUP BY plantilla_id, itemgroup \n" +
                "), \n" +

                "tot_pt AS ( \n" +
                "  SELECT plantilla_id, itemgroup, producttypesap, COUNT(DISTINCT pk) AS total_registros \n" +
                "  FROM base \n" +
                "  GROUP BY plantilla_id, itemgroup, producttypesap \n" +
                "), \n" +

                "attrs_ig AS ( \n" +
                "  SELECT b.plantilla_id, b.itemgroup, COUNT(DISTINCT a.PIM_ATRIBUTO_ID) AS total_atributos_distintos \n" +
                "  FROM base b, UNNEST(IFNULL(b.atributos, [])) a \n" +
                "  GROUP BY b.plantilla_id, b.itemgroup \n" +
                "), \n" +

                "attrs_pt AS ( \n" +
                "  SELECT b.plantilla_id, b.itemgroup, b.producttypesap, COUNT(DISTINCT a.PIM_ATRIBUTO_ID) AS total_atributos_distintos \n" +
                "  FROM base b, UNNEST(IFNULL(b.atributos, [])) a \n" +
                "  GROUP BY b.plantilla_id, b.itemgroup, b.producttypesap \n" +
                "), \n" +

                "pres_ig AS ( \n" +
                "  SELECT \n" +
                "    b.plantilla_id, \n" +
                "    b.itemgroup, \n" +
                "    a.PIM_ATRIBUTO_ID AS atributo_id, \n" +
                "    ANY_VALUE(a.PIM_ATRIBUTO_DESC) AS atributo_desc, \n" +
                "    COUNT(DISTINCT b.pk) AS registros_con_atributo \n" +
                "  FROM base b, UNNEST(IFNULL(b.atributos, [])) a \n" +
                "  GROUP BY b.plantilla_id, b.itemgroup, a.PIM_ATRIBUTO_ID \n" +
                "), \n" +

                "pres_pt AS ( \n" +
                "  SELECT \n" +
                "    b.plantilla_id, \n" +
                "    b.itemgroup, \n" +
                "    b.producttypesap, \n" +
                "    a.PIM_ATRIBUTO_ID AS atributo_id, \n" +
                "    ANY_VALUE(a.PIM_ATRIBUTO_DESC) AS atributo_desc, \n" +
                "    COUNT(DISTINCT b.pk) AS registros_con_atributo \n" +
                "  FROM base b, UNNEST(IFNULL(b.atributos, [])) a \n" +
                "  GROUP BY b.plantilla_id, b.itemgroup, b.producttypesap, a.PIM_ATRIBUTO_ID \n" +
                ") \n" +

                "SELECT \n" +
                "  'ITEMGROUP' AS nivel, \n" +
                "  p.plantilla_id, \n" +
                "  p.itemgroup, \n" +
                "  NULL AS producttypesap, \n" +
                "  p.atributo_id, \n" +
                "  p.atributo_desc, \n" +
                "  t.total_registros, \n" +
                "  a.total_atributos_distintos, \n" +
                "  p.registros_con_atributo, \n" +
                "  SAFE_DIVIDE(p.registros_con_atributo, t.total_registros) * 100 AS pct_registros_con_atributo \n" +
                "FROM pres_ig p \n" +
                "JOIN tot_ig t USING (plantilla_id, itemgroup) \n" +
                "JOIN attrs_ig a USING (plantilla_id, itemgroup) \n" +

                "UNION ALL \n" +

                "SELECT \n" +
                "  'PRODUCTTYPESAP' AS nivel, \n" +
                "  p.plantilla_id, \n" +
                "  p.itemgroup, \n" +
                "  p.producttypesap, \n" +
                "  p.atributo_id, \n" +
                "  p.atributo_desc, \n" +
                "  t.total_registros, \n" +
                "  a.total_atributos_distintos, \n" +
                "  p.registros_con_atributo, \n" +
                "  SAFE_DIVIDE(p.registros_con_atributo, t.total_registros) * 100 AS pct_registros_con_atributo \n" +
                "FROM pres_pt p \n" +
                "JOIN tot_pt t USING (plantilla_id, itemgroup, producttypesap) \n" +
                "JOIN attrs_pt a USING (plantilla_id, itemgroup, producttypesap) \n" +
                "ORDER BY nivel, plantilla_id, itemgroup, producttypesap, pct_registros_con_atributo DESC, atributo_id";


        com.google.cloud.bigquery.QueryJobConfiguration queryConfig =
                com.google.cloud.bigquery.QueryJobConfiguration.newBuilder(sql)
                        .setUseLegacySql(false)
                        .build();

        com.google.cloud.bigquery.TableResult result = bigquery.query(queryConfig);

        java.nio.file.Path parent = outputCsv.getParent();
        if (parent != null) java.nio.file.Files.createDirectories(parent);

        long rows = 0;

        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.BufferedWriter(
                        new java.io.OutputStreamWriter(
                                java.nio.file.Files.newOutputStream(outputCsv),
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                ), true)) {

            com.google.cloud.bigquery.Schema schema = result.getSchema();
            java.util.List<com.google.cloud.bigquery.Field> fields = schema.getFields();

            // Header
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) pw.print(",");
                pw.print(csvEscape(fields.get(i).getName()));
            }
            pw.println();

            // Rows
            for (com.google.cloud.bigquery.FieldValueList row : result.iterateAll()) {
                for (int i = 0; i < fields.size(); i++) {
                    if (i > 0) pw.print(",");
                    String s = toString(fields.get(i), row.get(i)); // usa tu helper existente
                    pw.print(csvEscape(s));
                }
                pw.println();
                rows++;
            }
        }

        System.out.println("OK -> " + rows + " filas en: " + outputCsv);
        return rows;
    }

    public static void printViewMetadata(BigQuery bigquery, String projectId, String datasetId, String viewName) {
    	String fqn = projectId + "." + datasetId + "." + viewName;
    	TableId tableId = TableId.of(projectId, datasetId, viewName);

        Table table = bigquery.getTable(
                tableId,
                BigQuery.TableOption.fields(
                        TableField.SCHEMA,
                        TableField.TYPE,
                        TableField.FRIENDLY_NAME,
                        TableField.DESCRIPTION,
                        TableField.CREATION_TIME,
                        TableField.LAST_MODIFIED_TIME,
                        TableField.EXPIRATION_TIME,
                        TableField.LABELS
                )
        );

        if (table == null) {
            System.out.println("No existe: " + projectId + "." + datasetId + "." + viewName);
            return;
        }

        TableDefinition def = table.getDefinition();
        System.out.println("==============================================");
        System.out.println("FQN: " + projectId + "." + datasetId + "." + viewName);
        System.out.println("Type: " + def.getType()); // TABLE / VIEW / MATERIALIZED_VIEW / EXTERNAL / SNAPSHOT
        System.out.println("FriendlyName: " + table.getFriendlyName());
        System.out.println("Description: " + table.getDescription());
        System.out.println("Created: " + fmtEpochMillis( table.getCreationTime() ));
        System.out.println("LastModified: " + fmtEpochMillis( table.getLastModifiedTime() ));
        System.out.println("Expiration: " + fmtEpochMillis( table.getExpirationTime() ));
        System.out.println("Labels: " + table.getLabels());

        if (def instanceof ViewDefinition vd) {
            System.out.println("----------------------------------------------");
            System.out.println("View SQL (useLegacySql=" + vd.useLegacySql() + "):");
            System.out.println(vd.getQuery());
        } else if (def instanceof MaterializedViewDefinition mvd) {
            System.out.println("----------------------------------------------");
            System.out.println("Materialized View SQL:");
            System.out.println(mvd.getQuery());
        } else {
            System.out.println("Ojo: no es VIEW, es: " + def.getType());
        }

//        Schema schema = null;
//        try {
//            String sql = "SELECT * FROM `" + fqn + "` LIMIT 0";
//            QueryJobConfiguration cfg = QueryJobConfiguration.newBuilder(sql)
//                    .setUseLegacySql(false)
//                    .build();
//            TableResult r = bigquery.query(cfg);
//            schema = r.getSchema();
//        } catch (com.google.cloud.bigquery.BigQueryException | InterruptedException e) {
//            System.out.println("No fue posible obtener schema vía consulta (permisos/política).");
//            System.out.println("Error: " + e.getMessage());
//        }
        Schema schema = def.getSchema();
        System.out.println("----------------------------------------------");
        if (schema == null || schema.getFields() == null) {
            System.out.println("Schema = null (puede pasar en algunas vistas/autorizadas).");
            return;
        }

        List<Field> fields = schema.getFields();
        System.out.println("Columns (" + fields.size() + "):");
        for (Field f : fields) {
            GrabLaDataDelWarjaus.printFieldMeta(f, 0); // usa TU método tal cual
        }
        System.out.println("==============================================");
    }
    
    public static void printViewMetadata2(BigQuery bigquery, String projectId, String datasetId, String viewName) {
    	String fqn = projectId + "." + datasetId + "." + viewName;
    	TableId tableId = TableId.of(projectId, datasetId, viewName);

        Table table = bigquery.getTable(
                tableId,
                BigQuery.TableOption.fields(
                        TableField.SCHEMA,
                        TableField.TYPE,
                        TableField.FRIENDLY_NAME,
                        TableField.DESCRIPTION,
                        TableField.CREATION_TIME,
                        TableField.LAST_MODIFIED_TIME,
                        TableField.EXPIRATION_TIME,
                        TableField.LABELS
                )
        );

        if (table == null) {
            System.out.println("No existe: " + projectId + "." + datasetId + "." + viewName);
            return;
        }

        TableDefinition def = table.getDefinition();
        System.out.println("==============================================");
        System.out.println("FQN: " + projectId + "." + datasetId + "." + viewName);
        System.out.println("Type: " + def.getType()); // TABLE / VIEW / MATERIALIZED_VIEW / EXTERNAL / SNAPSHOT
        System.out.println("FriendlyName: " + table.getFriendlyName());
        System.out.println("Description: " + table.getDescription());
        System.out.println("Created: " + fmtEpochMillis( table.getCreationTime() ));
        System.out.println("LastModified: " + fmtEpochMillis( table.getLastModifiedTime() ));
        System.out.println("Expiration: " + fmtEpochMillis( table.getExpirationTime() ));
        System.out.println("Labels: " + table.getLabels());

        if (def instanceof ViewDefinition vd) {
            System.out.println("----------------------------------------------");
            System.out.println("View SQL (useLegacySql=" + vd.useLegacySql() + "):");
            System.out.println(vd.getQuery());
        } else if (def instanceof MaterializedViewDefinition mvd) {
            System.out.println("----------------------------------------------");
            System.out.println("Materialized View SQL:");
            System.out.println(mvd.getQuery());
        } else {
            System.out.println("Ojo: no es VIEW, es: " + def.getType());
        }

        Schema schema = null;
        try {
            String sql = "SELECT * FROM `" + fqn + "` LIMIT 0";
            QueryJobConfiguration cfg = QueryJobConfiguration.newBuilder(sql)
                    .setUseLegacySql(false)
                    .build();
            TableResult r = bigquery.query(cfg);
            schema = r.getSchema();
        } catch (com.google.cloud.bigquery.BigQueryException | InterruptedException e) {
            System.out.println("No fue posible obtener schema vía consulta (permisos/política).");
            System.out.println("Error: " + e.getMessage());
        }
//        Schema schema = def.getSchema();
        System.out.println("----------------------------------------------");
        if (schema == null || schema.getFields() == null) {
            System.out.println("Schema = null (puede pasar en algunas vistas/autorizadas).");
            return;
        }

        List<Field> fields = schema.getFields();
        System.out.println("Columns (" + fields.size() + "):");
        for (Field f : fields) {
            GrabLaDataDelWarjaus.printFieldMeta(f, 0); // usa TU método tal cual
        }
        System.out.println("==============================================");
    }
    
    private static String csvEscape(String s) {
        if (s == null) return "";
        boolean needsQuotes = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (s.indexOf('"') >= 0) s = s.replace("\"", "\"\"");
        return needsQuotes ? "\"" + s + "\"" : s;
    }

    
}
