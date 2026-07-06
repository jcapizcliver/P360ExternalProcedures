package mx.com.liverpool.p360.services.core.temp.gcp;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQuery.DatasetListOption;
import com.google.cloud.bigquery.BigQuery.TableListOption;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.http.HttpTransportOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CheckDataSetsBigQuery {

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

        long init = System.currentTimeMillis();
        java.util.LinkedList<String> malitas = new java.util.LinkedList<>();
        
        
        //////////////////////////////////////////
        

        projectId = "crp-pro-dwh-semanticagold";
        try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "SAanaliticaDatasetsContent2.csv").toFile())))) {
            // List datasets in the project
            pw.println("Project: " + projectId);
            pw.println("========================================");

            Iterable<Dataset> datasets = bigquery.listDatasets(
                    projectId,
                    DatasetListOption.pageSize(1000) // you can omit this if you want defaults
            ).iterateAll();

            boolean anyDataset = false;

            for (Dataset dataset : datasets) {
                anyDataset = true;
                DatasetId datasetId = dataset.getDatasetId();
                String datasetName = datasetId.getDataset();

                pw.println("Dataset: " + datasetName);

                // List tables in this dataset
                Iterable<Table> tables = bigquery.listTables(
                        datasetId,
                        TableListOption.pageSize(1000)
                ).iterateAll();

                boolean anyTable = false;
                for (Table table : tables) {
                    anyTable = true;
                    TableId tableId = table.getTableId();
                    pw.println("  - " + tableId.getTable());
                    Table fullTable = bigquery.getTable(tableId);
                    if (fullTable == null) {
                        System.out.println("    (could not load table metadata)");
                        continue;
                    }

                    TableDefinition def = fullTable.getDefinition();
                    Schema schema = def.getSchema();

                    if (schema == null || schema.getFields() == null || schema.getFields().isEmpty()) {
                        System.out.println("    (no schema or not a standard table)");
                    } else {
                        printFields(schema.getFields(), "    ", pw);
                    }
                }

                if (!anyTable) {
                    pw.println("  (no tables)");
                }

                pw.println(); // blank line between datasets
            }

            if (!anyDataset) {
                pw.println("No datasets found in project " + projectId);
            }

        } catch (java.io.IOException | com.google.cloud.bigquery.BigQueryException e) {
            System.err.println("Error while listing datasets/tables: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(2);
        }
        System.exit(0);
        
        /////////////////////////////////////////
        
        /*
        projectId = "crp-pro-dwh-semanticagold";
        try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "SAanaliticaDatasetsContent.csv").toFile())))) {
            // List datasets in the project
            pw.println("Project: " + projectId);
            pw.println("========================================");

            Iterable<Dataset> datasets = bigquery.listDatasets(
                    projectId,
                    DatasetListOption.pageSize(1000) // you can omit this if you want defaults
            ).iterateAll();

            boolean anyDataset = false;

            for (Dataset dataset : datasets) {
                anyDataset = true;
                DatasetId datasetId = dataset.getDatasetId();
                String datasetName = datasetId.getDataset();

                pw.println("Dataset: " + datasetName);

                // List tables in this dataset
                Iterable<Table> tables = bigquery.listTables(
                        datasetId,
                        TableListOption.pageSize(1000)
                ).iterateAll();

                boolean anyTable = false;
                for (Table table : tables) {
                    anyTable = true;
                    TableId tableId = table.getTableId();
                    pw.println("  - " + tableId.getTable());
                    Table fullTable = bigquery.getTable(tableId);
                    if (fullTable == null) {
                        System.out.println("    (could not load table metadata)");
                        continue;
                    }

                    TableDefinition def = fullTable.getDefinition();
                    Schema schema = def.getSchema();

                    if (schema == null || schema.getFields() == null || schema.getFields().isEmpty()) {
                        System.out.println("    (no schema or not a standard table)");
                    } else {
                        printFields(schema.getFields(), "    ", pw);
                    }
                }

                if (!anyTable) {
                    pw.println("  (no tables)");
                }

                pw.println(); // blank line between datasets
            }

            if (!anyDataset) {
                pw.println("No datasets found in project " + projectId);
            }

        } catch (java.io.IOException | com.google.cloud.bigquery.BigQueryException e) {
            System.err.println("Error while listing datasets/tables: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(2);
        }
        System.exit(0);
        */
        
        /*
		BigQuery bq = BigQueryOptions.getDefaultInstance().getService();

		Page<Table> tables = bigquery.listTables(
		        "my_dataset",
		        BigQuery.TableListOption.pageSize(1000)
		);

		for (Table t : tables.iterateAll()) {
		    pw.println(t.getTableId().getTable());
		}
		System.exit(0);
		*/
        	try
        	{
		        String query = 
//		        		"select * from `crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET`"
//		        		"create view `crp-pro-cx-semantica.mus_pro_gob_producto_prd_views.CIFRAS_ATRIBUTOS_GRUPOS_DE_ARTICULO` as select * from `crp-pro-cx-analitica.mus_pro_gob_producto_prd_tbls.CIFRAS_ATRIBUTOS_GRUPOS_DE_ARTICULO`"
		        		"select * from `crp-pro-cx-semantica.mus_pro_gob_producto_prd_views.VW_EU_COB_ATR_ITEMGROUP` where pct_cobertura <> 100.0 limit 50"
//		        		"select * from `crp-pro-cx-semantica.mus_pro_gob_producto_prd_views.VW_EU_COB_ATR_PTSAP` limit 50"
	        		;
		
		        
		        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
		
		        TableResult result = bigquery.query(queryConfig);
		        
		        Schema schema = result.getSchema();
		        java.util.List<Field> fields = schema.getFields();
		        java.util.List<FieldValue> subRecords = null;
		        FieldValueList fvl = null;
		        String productId = null;

		        int a = 0;
		        int b = fields.size();
		        String[] arr = new String[b];
		        for (int i = 0; i < arr.length; i++) {
//		            if(i <= 9) {
	        			arr[i] = fields.get(i).getName();
//	        		} else if(i == 10) {
//	        			
//	        		} else if(i == 11) {
//	        			arr[i-1] = fields.get(i).getName();
//	        		}
//		            printFieldMeta(fields.get(i), 0);
		        }
		        long counter = 0;
		        System.out.println("No iterating...");
		        for (FieldValueList row : result.iterateAll()) {
		        	for(FieldValue fv : row) {
//		        		if(a <= 9) {
//		        			if(a == 0) {
//		        				productId = toString( fields.get(a), fv );
//		        			}
		        			arr[a] = toString( fields.get(a), fv );
//		        		} else if(a == 10) {
//		        			 subRecords = fv.getRepeatedValue();
//		        			 for(FieldValue fv0 : subRecords) {
//			        			 fvl = fv0.getRecordValue();
//		        			 }
//		        		} else if(a == 11) {
//		        			arr[a-1] = toString( fields.get(a), fv );
//		        		}
		        		a++;
		        	}
		        	counter++;
		        	if(counter % 100000 == 0) {
		        		System.out.print(".");
		        		if(counter % 1000000 == 0) {
		        			System.out.println(counter);
		        		}
		        	}
	    			System.out.println( rw.getRw().serializeChunk(arr) );
		        	a = 0;
		        }
		        System.out.println();
		        System.out.println(counter);
        	}catch(com.google.cloud.bigquery.BigQueryException e) {
        		e.printStackTrace();
        	}
	        System.out.println("~~~");
        System.out.println("Done. " + w.formatTime(System.currentTimeMillis() - init0));
    }
    
    private static void printFields(Iterable<Field> fields, String indent, java.io.PrintWriter pw) {
        for (Field field : fields) {
            String mode = (field.getMode() == null) ? "NULLABLE" : field.getMode().name();

            pw.println(indent + "- " + field.getName()
                    + " : " + field.getType().getStandardType()
                    + " (" + mode + ")");

            if (field.getDescription() != null && !field.getDescription().isEmpty()) {
                pw.println(indent + "  description: " + field.getDescription());
            }

            // Campos anidados (STRUCT / RECORD)
            if (field.getType().getStandardType() == StandardSQLTypeName.STRUCT
                    && field.getSubFields() != null
                    && !field.getSubFields().isEmpty()) {

                pw.println(indent + "  nested fields:");
                printFields(field.getSubFields(), indent + "    ", pw);
            }
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
            case BIGNUMERIC: return v.getStringValue();
            case BYTES:   return java.util.Base64.getEncoder().encodeToString( v.getBytesValue() );
            case STRUCT:  return String.valueOf( v.getRecordValue() );
            case ARRAY:   return String.valueOf( v.getRepeatedValue() );
            default:
                return v.getStringValue();
        }
    }
    
    
    class EscritorDatos {
    	
  	  private final BigQuery bq;
  	  private final String fqTable; // `dataset.table`
  	  private final int maxRowsPerBatch;
  	  private final List<Map<String, QueryParameterValue>> batch = new ArrayList<>();

  	  public EscritorDatos(String dataset, String table, int maxRowsPerBatch) {
  	    this.bq = BigQueryOptions.getDefaultInstance().getService();
  	    this.fqTable = dataset + "." + table;
  	    this.maxRowsPerBatch = maxRowsPerBatch;
  	  }

  	  public void addRow(long id, String descripcion, BigDecimal monto) throws Exception {
  	    Map<String, QueryParameterValue> fields = new LinkedHashMap<>();
  	    fields.put("id", QueryParameterValue.int64(id));
  	    fields.put("descripcion", QueryParameterValue.string(descripcion));
  	    fields.put("monto", QueryParameterValue.numeric(monto.setScale(4))); // 4 decimales

  	    batch.add(fields);
  	    if (batch.size() >= maxRowsPerBatch) flush();
  	  }

  	  public void flush() throws Exception {
  	    if (batch.isEmpty()) return;

  	    // Construye el parámetro ARRAY<STRUCT<id INT64, descripcion STRING, monto NUMERIC>>
  	    QueryParameterValue[] structs = batch.stream()
  	        .map(QueryParameterValue::struct)
  	        .toArray(QueryParameterValue[]::new);

//  	    QueryParameterValue rowsParam = QueryParameterValue.array(
//  	        structs
//  	        ,
//  	        QueryParameterType.struct(
//  	            new QueryParameterType.StructField("id", QueryParameterType.Int64),
//  	            new QueryParameterType.StructField("descripcion", QueryParameterType.String),
//  	            new QueryParameterType.StructField("monto", QueryParameterType.Numeric)
//  	        )
//  	    );

  	    String sql = "INSERT INTO `" + fqTable + "` (id, descripcion, monto) SELECT id, descripcion, monto FROM UNNEST(@rows)";

  	    QueryJobConfiguration qjc = QueryJobConfiguration.newBuilder(sql)
//  	        .addNamedParameter("rows", rowsParam)
  	        .build();

  	    Job job = bq.create(JobInfo.of(qjc)).waitFor();
  	    if (job == null || job.getStatus().getError() != null) {
  	      throw new RuntimeException("Insert failed: " + (job == null ? "job disappeared" : job.getStatus().getError()));
  	    }
  	    batch.clear();
  	  }
  	
  }
    
}
