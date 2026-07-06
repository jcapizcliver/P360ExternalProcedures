package mx.com.liverpool.p360.services.core.temp.gcp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.CsvOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobException;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.JobStatistics;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableDataWriteChannel;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.bigquery.ViewDefinition;
import com.google.cloud.bigquery.WriteChannelConfiguration;
import com.google.cloud.http.HttpTransportOptions;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CollectDataByBlocks {

    private static final RESTWrapper rw = new RESTWrapper();
    private static final RESTWorkshop w = rw.getRw();
	private static final ZoneOffset FIXED_UTC_MINUS_6 = ZoneOffset.ofHours(-6);
	private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	static final DateTimeFormatter DATETIME_IN = new DateTimeFormatterBuilder()
	        .appendPattern("yyyy-MM-dd HH:mm:ss")
	        .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true).optionalEnd()
	        .toFormatter();

	static final DateTimeFormatter TIME_IN = new DateTimeFormatterBuilder()
	        .appendPattern("HH:mm:ss")
	        .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true).optionalEnd()
	        .toFormatter();
	
    public static void main(String[] args) throws Exception {
        String query = 
        		"SELECT PIM_ATRIB_ITEMGROUP, count(*) freq FROM `crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET` WHERE "
//        		+ "    STARTS_WITH(PIM_PLANTILLA_ID, 'EU4') \n"
//        		+ "    AND NULLIF(TRIM(PIM_ATRIB_ITEMGROUP), '') IS NOT NULL \n"
//        		+ "    AND NULLIF(TRIM(PIM_ATRIB_PRODUCTTYPESAP), '') IS NOT NULL \n"
        		+ "    PIM_ATRIB_ITEMGROUP like 'SB%' group by PIM_ATRIB_ITEMGROUP";
    	qq(query);
    	System.exit(0);
        
        String datasetForWrite      = "mus_pro_gob_producto_prd_tbls";
        String datasetForWriteViews = "mus_pro_gob_producto_prd_views";
    	
        String keyPath  = "C:\\opt\\LVP\\dev\\crp-pro-cx-analitica-f63214cf6e70-pro-gob-prod.json";
        String projectId = "crp-pro-cx-analitica";
//        projectId = "crp-pro-dwh-semanticagold";

        HttpTransportOptions transport = HttpTransportOptions.newBuilder()
                .setHttpTransportFactory(NetHttpTransport::new)
                .build();

        BigQuery bigquery = BigQueryOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(keyPath)))
                .setTransportOptions(transport)
                .build()
                .getService();
        
//        String location = "US"; // igual que tus jobs

        /**
         * 
         * 
         * 
         * 
         * 
         * */
     // ====== VW_EU_COB_ATR_PTSAP ======
     String vwPtsap = "VW_EU_COB_ATR_PTSAP";
     String descPtsap = "Cobertura de atributos por Plantilla + ItemGroup + ProductTypeSAP (EU). Por atributo: % de presencia en productos del grupo y métricas de conteo.";

     java.util.Map<String, String> colsPtsap = new java.util.LinkedHashMap<>();
     colsPtsap.put("PIM_PLANTILLA_ID", "Identificador de plantilla PIM (solo EU).");
     colsPtsap.put("PIM_ATRIB_ITEMGROUP", "ItemGroup PIM (nivel padre jerárquico).");
     colsPtsap.put("PIM_ATRIB_PRODUCTTYPESAP", "ProductTypeSAP PIM (nivel hijo dentro del ItemGroup).");
     colsPtsap.put("PIM_ATRIBUTO_ID", "Identificador del atributo PIM evaluado.");
     colsPtsap.put("pct_cobertura", "Porcentaje (0–100) de productos del grupo (Plantilla+ItemGroup+ProductTypeSAP) donde aparece el atributo.");
     colsPtsap.put("total_atributos_distintos", "Número de atributos distintos (PIM_ATRIBUTO_ID) presentes en el grupo.");
     colsPtsap.put("registros_con_atributo", "Cantidad de productos (pk) del grupo que traen el atributo (numerador).");
     colsPtsap.put("total_registros", "Cantidad total de productos (pk) en el grupo (denominador).");

     describeViewAndColumns(bigquery, "crp-pro-cx-semantica", datasetForWriteViews, vwPtsap, descPtsap, colsPtsap);


     // ====== VW_EU_COB_ATR_ITEMGROUP ======
     String vwIg = "VW_EU_COB_ATR_ITEMGROUP";
     String descIg = "Cobertura de atributos por Plantilla + ItemGroup (rollup EU). Agrega todos los ProductTypeSAP hijos; % de presencia del atributo en el total del ItemGroup.";

     java.util.Map<String, String> colsIg = new java.util.LinkedHashMap<>();
     colsIg.put("PIM_PLANTILLA_ID", "Identificador de plantilla PIM (solo EU).");
     colsIg.put("PIM_ATRIB_ITEMGROUP", "ItemGroup PIM (nivel padre).");
     colsIg.put("PIM_ATRIB_PRODUCTTYPESAP", "Valor fijo \"ALL\" indicando agregación (rollup) a nivel ItemGroup.");
     colsIg.put("PIM_ATRIBUTO_ID", "Identificador del atributo PIM evaluado.");
     colsIg.put("pct_cobertura", "Porcentaje (0–100) de productos del grupo (Plantilla+ItemGroup) donde aparece el atributo.");
     colsIg.put("total_atributos_distintos", "Número de atributos distintos (PIM_ATRIBUTO_ID) presentes en el grupo (rollup).");
     colsIg.put("registros_con_atributo", "Cantidad de productos (pk) del grupo que traen el atributo (numerador).");
     colsIg.put("total_registros", "Cantidad total de productos (pk) en el grupo (denominador).");

     describeViewAndColumns(bigquery, "crp-pro-cx-semantica", datasetForWriteViews, vwIg, descIg, colsIg);

     System.out.println("Descripciones aplicadas a vistas y columnas ✅");
     System.exit(0);
     /**
      * 
      * 
      * 
      * 
      * 
      * */
        
//    	exportCoberturaAtributosPlantillaItemGroupToCsv(bigquery, java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "hola"));
/*
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
*/      

//        String datasetForWrite      = "mus_pro_gob_producto_prd_tbls";
//        String datasetForWriteViews = "mus_pro_gob_producto_prd_views";
        String atributosPlantillasTableName = "CIFRAS_ATRIBUTOS_GRUPOS_DE_ARTICULO";
        
        query =
        		  "SELECT * " 
        		+ "FROM `" 
        		+ projectId 
        		+ "." 
        		+ datasetForWrite 
        		+ ".CIFRAS_ATRIBUTOS_GRUPOS_DE_ARTICULO` "
        	;
        
        // 2) View definition (Standard SQL by default)
        ViewDefinition viewDefinition = ViewDefinition.of(query);
        
        // 3) TableInfo with ViewDefinition
        TableId viewId = TableId.of(datasetForWriteViews, atributosPlantillasTableName);
        TableInfo tableInf = TableInfo.of(viewId, viewDefinition);
        
        // 4) Create the view
        Table view = bigquery.create(tableInf);
        
        System.out.println("Created view: " +
        		view.getTableId().getDataset() + "." + view.getTableId().getTable());
        
        System.exit(0);
        
        Schema schema = Schema.of(
                  Field.of("ItemGroup", StandardSQLTypeName.STRING)
                , Field.of("Attribute", StandardSQLTypeName.STRING)
                , Field.of("Attribute_Label", StandardSQLTypeName.STRING)
                , Field.of("Completes", StandardSQLTypeName.FLOAT64)
                , Field.of("Cantidad_de_Valores_Distintos", StandardSQLTypeName.INT64)
                , Field.of("Total_de_Valores", StandardSQLTypeName.INT64)
                , Field.of("Espacios_Multiples", StandardSQLTypeName.FLOAT64)
                , Field.of("Espacios_Inicio_Fin", StandardSQLTypeName.FLOAT64)
                , Field.of("Caracteres_Especiales", StandardSQLTypeName.INT64)
//                Field.of("created_at", StandardSQLTypeName.TIMESTAMP)
        );

        TableId tableId = TableId.of(datasetForWrite, atributosPlantillasTableName);

        StandardTableDefinition tableDefinition =
                StandardTableDefinition.newBuilder()
                        .setSchema(schema)
                        .build();

        TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition)
                .build();

        try {
        	bigquery.delete(tableId);
        	Table createdTable = bigquery.create(tableInfo);
        }catch(com.google.cloud.bigquery.BigQueryException e) {
        	System.out.println(e.getMessage());
        }
        
        String dataSet = "";
        String table = "";

//        TableId tableId = TableId.of(dataSet, table);
        CsvOptions options = CsvOptions.newBuilder().setSkipLeadingRows(1).build();
        System.out.println("--- " + options.getSkipLeadingRows());
        WriteChannelConfiguration config = WriteChannelConfiguration
                .newBuilder(tableId)
                .setFormatOptions(options)
//                .setAutodetect(true)
                .build();
        
        // If your dataset is in US multi-region; change if needed
        JobId jobId = JobId.newBuilder().setLocation("US").build();

        TableDataWriteChannel writer = bigquery.writer(jobId, config);

        // Stream the local file into BigQuery
        try (OutputStream os = Channels.newOutputStream(writer)) {
        	java.nio.file.Path fp = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "ItemGroup", "CompletitudDeAtributosPorPlantilla.csv");
        	long size = java.nio.file.Files.size(fp);
        	System.out.println("Now sending...");
        	long read = 0;
        	try(java.io.FileInputStream fis = new java.io.FileInputStream(fp.toFile())){
        		byte[] chunk = new byte[8*1024*1024];
        		int len;
        		while((len = fis.read(chunk)) != -1) {
        			os.write(chunk, 0, len);
        			read += len;
        			System.out.println( (float) 100*read/size + " %" );
        		}
        	}catch(java.io.IOException e) {
        		e.printStackTrace();
        	}
        }

        // Wait for the load job to finish
        Job job = writer.getJob();
        try {
	        job = job.waitFor();
	
	        if (job == null) {
	            throw new RuntimeException("Job disappeared.");
	        }
	        if (job.getStatus().getError() != null) {
	            throw new RuntimeException("BigQuery load error: " + job.getStatus().getError());
	        }
	
	        JobStatistics.LoadStatistics stats = job.getStatistics();
	        System.out.println("Loaded rows: " + stats.getOutputRows());
        }catch(com.google.cloud.bigquery.BigQueryException ex) {
        	ex.printStackTrace();
        	java.util.List<com.google.cloud.bigquery.BigQueryError> executionErrors =
        			job.getStatus().getExecutionErrors();

        	if (executionErrors != null && !executionErrors.isEmpty()) {
        	    System.out.println("Execution errors (" + executionErrors.size() + "):");
        	    for (com.google.cloud.bigquery.BigQueryError e : executionErrors) {
        	        System.out.println("  message = " + e.getMessage());
        	        System.out.println("  reason  = " + e.getReason());
        	        System.out.println("  location= " + e.getLocation());
        	        System.out.println("-----");
        	    }
//        	}else {
//        		System.out.println(executionErrors);
        	}
        }
        
        System.exit(0);
    	
    	long init0 = System.currentTimeMillis();

        long init = System.currentTimeMillis();
        java.util.concurrent.ConcurrentLinkedQueue<String> processedIDs = new java.util.concurrent.ConcurrentLinkedQueue<>();
        try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get( "C:", "opt", "LVP", "desorden", "BQ", "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET.csv.bkp" ))){
        	lns.parallel().map( l -> w.parseLine(l) ).forEach( a -> processedIDs.add(a[0]) );
        }catch(java.io.IOException e){
        	e.printStackTrace();
        }
        java.util.Set<String> processedIds = new java.util.TreeSet<>( processedIDs );
        System.out.println("Done reading file. " + processedIds.size() + " lines read (" + processedIDs.size() + "), took --->" + w.formatTime(System.currentTimeMillis() - init));
        processedIDs.clear();
        System.out.println(processedIds.size());
        try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "IDs_"))){
        	lns.parallel().filter(l -> !processedIds.contains(l)).forEach(processedIDs::add);
        }catch(java.io.IOException e) {
        	e.printStackTrace();
        }
        int initSize = processedIDs.size();
        System.out.println("--->" + initSize);
        long agg = 0;
        java.math.BigDecimal tz = new java.math.BigDecimal(initSize);
        while(!processedIDs.isEmpty()) {
        	agg += collectData( collectIDs(processedIDs, 50000) );
        	System.out.println(agg + "/" + initSize + " (" + new java.math.BigDecimal(agg).multiply(java.math.BigDecimal.TEN.pow(2)).divide(tz, 4, java.math.RoundingMode.HALF_UP) + ")");
        }
        System.out.println("Done. " + w.formatTime(System.currentTimeMillis() - init0));
    }
    
    private static String collectIDs(java.util.concurrent.ConcurrentLinkedQueue<String> missingIDs, int howMany) {
    	StringBuilder sb = new StringBuilder();
    	java.util.Iterator<String> iter = missingIDs.iterator();
    	String currentId = null;
    	int counter = 0;
    	while(iter.hasNext() && counter < howMany) {
    		currentId = iter.next();
    		iter.remove();
    		sb.append(sb.length() == 0 ? "" : ",");
    		sb.append("'");
    		sb.append(currentId);
    		sb.append("'");
    		counter++;
    	}
    	return sb.toString();
    }
    
    private static int collectData(String ids) throws FileNotFoundException, IOException, JobException, InterruptedException {
        int counter = 0;
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
/*
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
*/      

        String datasetForWrite = "mus_pro_gob_producto_prd_tbls";
        String atributosPlantillasTableName = "CIFRAS_ATRIBUTOS_PLANTILLAS";
        
        // === 1. Define schema (adjust to your real columns) ===
        Schema schema = Schema.of(
                  Field.of("Template", StandardSQLTypeName.STRING)
                , Field.of("Template_Label", StandardSQLTypeName.STRING)
                , Field.of("Attribute", StandardSQLTypeName.STRING)
                , Field.of("Attribute_Label", StandardSQLTypeName.STRING)
                , Field.of("Completes", StandardSQLTypeName.FLOAT64)
                , Field.of("Cantidad_de_Valores_Distintos", StandardSQLTypeName.INT64)
                , Field.of("Total_de_Valores", StandardSQLTypeName.INT64)
                , Field.of("Espacios_Multiples", StandardSQLTypeName.INT64)
                , Field.of("Espacios_Inicio_Fin", StandardSQLTypeName.INT64)
                , Field.of("Caracteres_Especiales", StandardSQLTypeName.INT64)
//                Field.of("created_at", StandardSQLTypeName.TIMESTAMP)
        );

        TableId tableId = TableId.of(datasetForWrite, atributosPlantillasTableName);

        StandardTableDefinition tableDefinition =
                StandardTableDefinition.newBuilder()
                        .setSchema(schema)
                        .build();

        TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition)
                .build();

        try {
        	Table createdTable = bigquery.create(tableInfo);
        }catch(com.google.cloud.bigquery.BigQueryException e) {
        	System.out.println(e.getMessage());
        }
        
        String dataSet = "";
        String table = "";

//        TableId tableId = TableId.of(dataSet, table);
        WriteChannelConfiguration config = WriteChannelConfiguration
                .newBuilder(tableId)
                .setFormatOptions(FormatOptions.csv())
//                .setAutodetect(true)
                .build();
        
        // If your dataset is in US multi-region; change if needed
        JobId jobId = JobId.newBuilder().setLocation("US").build();

        TableDataWriteChannel writer = bigquery.writer(jobId, config);

        // Stream the local file into BigQuery
        try (OutputStream os = Channels.newOutputStream(writer)) {
        	java.nio.file.Path fp = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "CompletitudDeAtributosPorPlantilla.csv");
        	long size = java.nio.file.Files.size(fp);
        	System.out.println("Now sending...");
        	long read = 0;
        	try(java.io.FileInputStream fis = new java.io.FileInputStream(fp.toFile())){
        		byte[] chunk = new byte[8*1024*1024];
        		int len;
        		while((len = fis.read(chunk)) != -1) {
        			os.write(chunk, 0, len);
        			read += len;
        			System.out.println( 100*read/size + " %" );
        		}
        	}catch(java.io.IOException e) {
        		e.printStackTrace();
        	}
        }

        // Wait for the load job to finish
        Job job = writer.getJob();
        job = job.waitFor();

        if (job == null) {
            throw new RuntimeException("Job disappeared.");
        }
        if (job.getStatus().getError() != null) {
            throw new RuntimeException("BigQuery load error: " + job.getStatus().getError());
        }

        JobStatistics.LoadStatistics stats = job.getStatistics();
        System.out.println("Loaded rows: " + stats.getOutputRows());
        
        System.exit(0);
        
        /******************
         * 
         * 
         * 
         * 	READ A LOT OF DATA
         * 
         * 
         * 
         **************************/
    	try
    	(
    		java.io.PrintWriter pw  = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ","crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET.csv").toFile(), true)));
        	java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "Detalle.csv").toFile(), true)))
    	)
    	{
	        String query = 
	        		"SELECT * FROM `crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET` where PIM_PROD_ID in (" + ids + ") ";
	
	        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query)
	        		.build();
	
	        TableResult result = bigquery.query(queryConfig);
	        
//	        Schema schema = result.getSchema();
	        schema = result.getSchema();
	        java.util.List<Field> fields = schema.getFields();
	        java.util.List<FieldValue> subRecords = null;
	        FieldValueList fvl = null;
	        String productId = null;
	        int a = 0;
	        int b = fields.size();
	        String[] arr = new String[b - 1];
	        for (int i = 0; i < b; i++) {
	            if(i <= 9) {
        			arr[i] = fields.get(i).getName();
        		} else if(i == 10) {
        			
        		} else if(i == 11) {
        			arr[i-1] = fields.get(i).getName();
        		}
//	            printFieldMeta(fields.get(i), 0);
	        }
	        System.out.println("Now iterating...");
	        for (FieldValueList row : result.iterateAll()) {
	        	for(FieldValue fv : row) {
	        		if(a <= 9) {
	        			if(a == 0) {
	        				productId = toString( fields.get(a), fv );
	        			}
	        			arr[a] = toString( fields.get(a), fv );
	        		} else if(a == 10) {
	        			 subRecords = fv.getRepeatedValue();
	        			 for(FieldValue fv0 : subRecords) {
		        			 fvl = fv0.getRecordValue();
	        				 pw2.println( rw.getRw().serializeChunk( new Object[] { productId, fvl.get(0).getStringValue(), fvl.get(2).getStringValue(), fvl.get(3).getStringValue() } ) );
	        			 }
	        		} else if(a == 11) {
	        			arr[a-1] = toString( fields.get(a), fv );
	        		}
	        		a++;
	        	}
	        	counter++;
	        	if(counter % 100000 == 0) {
	        		System.out.print(".");
	        		if(counter % 1000000 == 0) {
	        			System.out.println(counter);
	        		}
	        	}
    			pw.println( rw.getRw().serializeChunk(arr) );
	        	a = 0;
	        }
	        System.out.println();
	        System.out.println(counter);
    	}catch(com.google.cloud.bigquery.BigQueryException e) {
    		e.printStackTrace();
    	}catch(java.io.IOException e) {
    		e.printStackTrace();
    	}
        System.out.println("~~~+");
        return counter;
    }
    
    
    private static void qq(String query) throws FileNotFoundException, IOException, JobException, InterruptedException {
    	int counter = 0;
        String keyPath  = "C:\\opt\\LVP\\dev\\crp-pro-cx-analitica-f63214cf6e70-pro-gob-prod.json";
//        String projectId = "crp-pro-cx-analitica";
        String projectId = "crp-pro-dwh-semanticagold";

        HttpTransportOptions transport = HttpTransportOptions.newBuilder()
                .setHttpTransportFactory(NetHttpTransport::new)
                .build();
    	BigQuery bigquery = BigQueryOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(keyPath)))
                .setTransportOptions(transport)
                .build()
                .getService();
    	try{
	        System.out.println("Gonna try: " + query);
	        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query)
	        		.build();
	
	        TableResult result = bigquery.query(queryConfig);
	        
	        Schema schema = result.getSchema();
	        java.util.List<Field> fields = schema.getFields();
	        int a = 0;
	        int b = fields.size();
	        String[] arr = new String[b];
	        for (int i = 0; i < b; i++) {
    			arr[i] = fields.get(i).getName();
	            printFieldMeta(fields.get(i), 0);
	        }
	        System.out.println("Now iterating...");
	        for (FieldValueList row : result.iterateAll()) {
	        	for(FieldValue fv : row) {
        			arr[a] = toString( fields.get(a), fv );
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
        System.out.println("~~~+");
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
//    		"crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PLANTILLA_ITEMGROUP_ASSET"
    		"crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET"
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

    private static long exportCoberturaAtributosPlantillaItemGroupToCsv(
            com.google.cloud.bigquery.BigQuery bigquery,
            java.nio.file.Path outputCsv
    ) throws com.google.cloud.bigquery.JobException, InterruptedException, java.io.IOException {

        final String projectId = "crp-pro-cx-analitica";
        final String tableFqn = "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET";
        
        String datasetForWrite      = "mus_pro_gob_producto_prd_tbls";
        String datasetForWriteViews = "mus_pro_gob_producto_prd_views";

//        String datasetForWriteViews = "mus_pro_gob_producto_prd_views";
//        String datasetForWriteViews = "mus_pro_gob_producto_prd_tbls";

     // Nombres de vistas (ajústalos si quieres)
     String viewTabla1 = "VW_EU_COB_ATR_PTSAP";
     String viewTabla2 = "VW_EU_COB_ATR_ITEMGROUP";

     // =====================
     // TABLA 1 (nivel bajo)
     // PIM_PLANTILLA_ID|PIM_ATRIB_PRODUCTTYPESAP|PIM_ATRIB_ITEMGROUP|PIM_ATRIBUTO_ID|Elporcentaje|LaCantidadDeValoresDistintosDe_PIM_ATRIBUTO_ID
     // =====================
     String queryTabla1 =
           "WITH base AS ( \n"
         + "  SELECT \n"
         + "    COALESCE(PIM_PROD_ID, CAST(PIM_SKU_CVE AS STRING), CAST(PIM_PADRE_SKU_CVE AS STRING)) AS pk, \n"
         + "    PIM_PLANTILLA_ID AS PIM_PLANTILLA_ID, \n"
         + "    NULLIF(TRIM(PIM_ATRIB_ITEMGROUP), '') AS PIM_ATRIB_ITEMGROUP, \n"
         + "    NULLIF(TRIM(PIM_ATRIB_PRODUCTTYPESAP), '') AS PIM_ATRIB_PRODUCTTYPESAP, \n"
         + "    PIM_ATRIB_ATRIBUTOS AS PIM_ATRIB_ATRIBUTOS \n"
         + "  FROM `crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET` \n"
         + "  WHERE STARTS_WITH(PIM_PLANTILLA_ID, 'EU4') \n"
         + "    AND NULLIF(TRIM(PIM_ATRIB_ITEMGROUP), '') IS NOT NULL \n"
         + "    AND NULLIF(TRIM(PIM_ATRIB_PRODUCTTYPESAP), '') IS NOT NULL \n"
         + "), \n"
         + "tot AS ( \n"
         + "  SELECT \n"
         + "    PIM_PLANTILLA_ID, PIM_ATRIB_PRODUCTTYPESAP, PIM_ATRIB_ITEMGROUP, \n"
         + "    COUNT(DISTINCT pk) AS total_registros \n"
         + "  FROM base \n"
         + "  GROUP BY PIM_PLANTILLA_ID, PIM_ATRIB_PRODUCTTYPESAP, PIM_ATRIB_ITEMGROUP \n"
         + "), \n"
         + "attrs AS ( \n"
         + "  SELECT \n"
         + "    b.PIM_PLANTILLA_ID, b.PIM_ATRIB_PRODUCTTYPESAP, b.PIM_ATRIB_ITEMGROUP, \n"
         + "    COUNT(DISTINCT a.PIM_ATRIBUTO_ID) AS total_atributos_distintos \n"
         + "  FROM base b, UNNEST(IFNULL(b.PIM_ATRIB_ATRIBUTOS, [])) a \n"
         + "  GROUP BY b.PIM_PLANTILLA_ID, b.PIM_ATRIB_PRODUCTTYPESAP, b.PIM_ATRIB_ITEMGROUP \n"
         + "), \n"
         + "pres AS ( \n"
         + "  SELECT \n"
         + "    b.PIM_PLANTILLA_ID, b.PIM_ATRIB_PRODUCTTYPESAP, b.PIM_ATRIB_ITEMGROUP, \n"
         + "    a.PIM_ATRIBUTO_ID AS PIM_ATRIBUTO_ID, \n"
         + "    COUNT(DISTINCT b.pk) AS registros_con_atributo \n"
         + "  FROM base b, UNNEST(IFNULL(b.PIM_ATRIB_ATRIBUTOS, [])) a \n"
         + "  GROUP BY b.PIM_PLANTILLA_ID, b.PIM_ATRIB_PRODUCTTYPESAP, b.PIM_ATRIB_ITEMGROUP, a.PIM_ATRIBUTO_ID \n"
         + ") \n"
         + "SELECT \n"
         + "  p.PIM_PLANTILLA_ID, \n"
         + "  p.PIM_ATRIB_PRODUCTTYPESAP, \n"
         + "  p.PIM_ATRIB_ITEMGROUP, \n"
         + "  p.PIM_ATRIBUTO_ID, \n"
         + "  SAFE_DIVIDE(p.registros_con_atributo, t.total_registros) * 100 AS pct_cobertura, \n"
         + "  a.total_atributos_distintos, p.registros_con_atributo,\r\n"
         + "  t.total_registros \n"
         + "FROM pres p \n"
         + "JOIN tot t USING (PIM_PLANTILLA_ID, PIM_ATRIB_PRODUCTTYPESAP, PIM_ATRIB_ITEMGROUP) \n"
         + "JOIN attrs a USING (PIM_PLANTILLA_ID, PIM_ATRIB_PRODUCTTYPESAP, PIM_ATRIB_ITEMGROUP) \n"
         + "ORDER BY PIM_PLANTILLA_ID, PIM_ATRIB_ITEMGROUP, PIM_ATRIB_PRODUCTTYPESAP, pct_cobertura DESC, PIM_ATRIBUTO_ID \n";

     // =====================
     // TABLA 2 (rollup por PLANTILLA + ITEMGROUP)
     // PIM_PLANTILLA_ID|PIM_ATRIB_PRODUCTTYPESAP|PIM_ATRIB_ITEMGROUP|PIM_ATRIBUTO_ID|ElporcentajeDeSumar...|LaCantidadDeValoresDistintosDe_PIM_ATRIBUTO_ID
     // OJO: aquí PIM_ATRIB_PRODUCTTYPESAP queda como 'ALL' porque ya estás agrupando al nivel padre.
     // =====================
     String queryTabla2 =
           "WITH base AS ( \n"
         + "  SELECT \n"
         + "    COALESCE(PIM_PROD_ID, CAST(PIM_SKU_CVE AS STRING), CAST(PIM_PADRE_SKU_CVE AS STRING)) AS pk, \n"
         + "    PIM_PLANTILLA_ID AS PIM_PLANTILLA_ID, \n"
         + "    NULLIF(TRIM(PIM_ATRIB_ITEMGROUP), '') AS PIM_ATRIB_ITEMGROUP, \n"
         + "    NULLIF(TRIM(PIM_ATRIB_PRODUCTTYPESAP), '') AS PIM_ATRIB_PRODUCTTYPESAP, \n"
         + "    PIM_ATRIB_ATRIBUTOS AS PIM_ATRIB_ATRIBUTOS \n"
         + "  FROM `crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET` \n"
         + "  WHERE STARTS_WITH(PIM_PLANTILLA_ID, 'EU4') \n"
         + "    AND NULLIF(TRIM(PIM_ATRIB_ITEMGROUP), '') IS NOT NULL \n"
         + "    AND NULLIF(TRIM(PIM_ATRIB_PRODUCTTYPESAP), '') IS NOT NULL \n"
         + "), \n"
         + "tot AS ( \n"
         + "  SELECT \n"
         + "    PIM_PLANTILLA_ID, PIM_ATRIB_ITEMGROUP, \n"
         + "    COUNT(DISTINCT pk) AS total_registros_rollup \n"
         + "  FROM base \n"
         + "  GROUP BY PIM_PLANTILLA_ID, PIM_ATRIB_ITEMGROUP \n"
         + "), \n"
         + "attrs AS ( \n"
         + "  SELECT \n"
         + "    b.PIM_PLANTILLA_ID, b.PIM_ATRIB_ITEMGROUP, \n"
         + "    COUNT(DISTINCT a.PIM_ATRIBUTO_ID) AS total_atributos_distintos \n"
         + "  FROM base b, UNNEST(IFNULL(b.PIM_ATRIB_ATRIBUTOS, [])) a \n"
         + "  GROUP BY b.PIM_PLANTILLA_ID, b.PIM_ATRIB_ITEMGROUP \n"
         + "), \n"
         + "pres AS ( \n"
         + "  SELECT \n"
         + "    b.PIM_PLANTILLA_ID, b.PIM_ATRIB_ITEMGROUP, \n"
         + "    a.PIM_ATRIBUTO_ID AS PIM_ATRIBUTO_ID, \n"
         + "    COUNT(DISTINCT b.pk) AS registros_con_atributo_rollup \n"
         + "  FROM base b, UNNEST(IFNULL(b.PIM_ATRIB_ATRIBUTOS, [])) a \n"
         + "  GROUP BY b.PIM_PLANTILLA_ID, b.PIM_ATRIB_ITEMGROUP, a.PIM_ATRIBUTO_ID \n"
         + ") \n"
         + "SELECT \n"
         + "  p.PIM_PLANTILLA_ID, \n"
         + "  'ALL' AS PIM_ATRIB_PRODUCTTYPESAP, \n"
         + "  p.PIM_ATRIB_ITEMGROUP, \n"
         + "  p.PIM_ATRIBUTO_ID, \n"
         + "  SAFE_DIVIDE(p.registros_con_atributo_rollup, t.total_registros_rollup) * 100 \n"
         + "    AS pct_cobertura, \n"
         + "  a.total_atributos_distintos, p.registros_con_atributo_rollup AS registros_con_atributo,\r\n"
         + "  t.total_registros_rollup AS total_registros \n"
         + "FROM pres p \n"
         + "JOIN tot t USING (PIM_PLANTILLA_ID, PIM_ATRIB_ITEMGROUP) \n"
         + "JOIN attrs a USING (PIM_PLANTILLA_ID, PIM_ATRIB_ITEMGROUP) \n"
         + "ORDER BY PIM_PLANTILLA_ID, PIM_ATRIB_ITEMGROUP, \n"
         + "  pct_cobertura DESC, \n"
         + "  PIM_ATRIBUTO_ID \n";

     String table1 = "TB_EU_COB_ATR_PTSAP";
     String table2 = "TB_EU_COB_ATR_ITEMGROUP";

     String view1  = "VW_EU_COB_ATR_PTSAP";
     String view2  = "VW_EU_COB_ATR_ITEMGROUP";
     
     String ddlTable1 =
    		    "CREATE OR REPLACE TABLE `" + projectId + "." + datasetForWrite + "." + table1 + "` AS \n" + queryTabla1;

    		String ddlTable2 =
    		    "CREATE OR REPLACE TABLE `" + projectId + "." + datasetForWrite + "." + table2 + "` AS \n" + queryTabla2;

    		runStatement(bigquery, projectId, datasetForWrite, ddlTable1 );
    		runStatement(bigquery, projectId, datasetForWrite, ddlTable2 );

    		// 2) Crear/reemplazar VISTAS en *_views (vil SELECT * FROM tabla)
    		String ddlView1 =
    		    "CREATE OR REPLACE VIEW `" + projectId + "." + datasetForWriteViews + "." + view1 + "` AS \n"
    		  + "SELECT * FROM `" + projectId + "." + datasetForWrite + "." + table1 + "`";

    		String ddlView2 =
    		    "CREATE OR REPLACE VIEW `" + projectId + "." + datasetForWriteViews + "." + view2 + "` AS \n"
    		  + "SELECT * FROM `" + projectId + "." + datasetForWrite + "." + table2 + "`";

    		runStatement(bigquery, projectId, datasetForWriteViews, ddlView1 );
    		runStatement(bigquery, projectId, datasetForWriteViews, ddlView2 );

    		System.out.println("Listo:");
    		System.out.println("Tables: " + datasetForWrite + "." + table1 + " | " + datasetForWrite + "." + table2);
    		System.out.println("Views : " + datasetForWriteViews + "." + view1 + " | " + datasetForWriteViews + "." + view2);
		System.exit(0);

     // Crear/recrear vistas
//     createOrReplaceView(bigquery, datasetForWriteViews, viewTabla1, queryTabla1);
//     createOrReplaceView(bigquery, datasetForWriteViews, viewTabla2, queryTabla2);

//     System.out.println("Created/Updated views:");
//     System.out.println(" - " + datasetForWriteViews + "." + viewTabla1);
//     System.out.println(" - " + datasetForWriteViews + "." + viewTabla2);

//     System.exit(0);
        
        // Nota: para contar "registros" de forma robusta, uso una "llave" que no sea NULL casi nunca.
        // Si PIM_PROD_ID siempre viene, chido. Si a veces viene NULL, caemos a SKU o PADRE_SKU.
        final String sql =
                "WITH base AS ( \n" +
                "  SELECT \n" +
                "    COALESCE(PIM_PROD_ID, CAST(PIM_SKU_CVE AS STRING), CAST(PIM_PADRE_SKU_CVE AS STRING)) AS pk, \n" +
                "    PIM_PLANTILLA_ID AS plantilla_id, \n" +
                "    PIM_ATRIB_ITEMGROUP AS itemgroup, \n" +
                "    PIM_ATRIB_PRODUCTTYPESAP AS producttypesap, \n" +
                "    PIM_ATRIB_ATRIBUTOS AS atributos \n" +
                "  FROM `" + tableFqn + "` \n" +
                "), \n" +

                // Totales por (PLANTILLA, ITEMGROUP)
                "tot_ig AS ( \n" +
                "  SELECT plantilla_id, itemgroup, COUNT(DISTINCT pk) AS total_registros \n" +
                "  FROM base \n" +
                "  GROUP BY plantilla_id, itemgroup \n" +
                "), \n" +

                // Totales por (PLANTILLA, ITEMGROUP, PRODUCTTYPESAP)
                "tot_pt AS ( \n" +
                "  SELECT plantilla_id, itemgroup, producttypesap, COUNT(DISTINCT pk) AS total_registros \n" +
                "  FROM base \n" +
                "  GROUP BY plantilla_id, itemgroup, producttypesap \n" +
                "), \n" +

                // # atributos distintos por (PLANTILLA, ITEMGROUP)
                "attrs_ig AS ( \n" +
                "  SELECT b.plantilla_id, b.itemgroup, COUNT(DISTINCT a.PIM_ATRIBUTO_ID) AS total_atributos_distintos \n" +
                "  FROM base b, UNNEST(IFNULL(b.atributos, [])) a \n" +
                "  GROUP BY b.plantilla_id, b.itemgroup \n" +
                "), \n" +

                // # atributos distintos por (PLANTILLA, ITEMGROUP, PRODUCTTYPESAP)
                "attrs_pt AS ( \n" +
                "  SELECT b.plantilla_id, b.itemgroup, b.producttypesap, COUNT(DISTINCT a.PIM_ATRIBUTO_ID) AS total_atributos_distintos \n" +
                "  FROM base b, UNNEST(IFNULL(b.atributos, [])) a \n" +
                "  GROUP BY b.plantilla_id, b.itemgroup, b.producttypesap \n" +
                "), \n" +

                // Presencia por atributo en nivel ITEMGROUP (cuántos registros traen ese atributo)
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

                // Presencia por atributo en nivel PRODUCTTYPESAP
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

                // Unión de ambos niveles para que te salga en un solo CSV
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

    private static String csvEscape(String s) {
        if (s == null) return "";
        boolean needsQuotes = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (s.indexOf('"') >= 0) s = s.replace("\"", "\"\"");
        return needsQuotes ? "\"" + s + "\"" : s;
    }
    
    private static void createOrReplaceView(BigQuery bigquery, String datasetId, String viewName, String sql) {
        TableId viewId = TableId.of(datasetId, viewName);

        try { bigquery.delete(viewId); } catch (Exception ignore) {}

        ViewDefinition viewDefinition = ViewDefinition.newBuilder(sql)
                .setUseLegacySql(false)
                .build();

        TableInfo tableInfo = TableInfo.newBuilder(viewId, viewDefinition).build();
        Table view = bigquery.create(tableInfo);

        System.out.println("Created view: " + view.getTableId().getDataset() + "." + view.getTableId().getTable());
    }
    
    private static void runStatement(BigQuery bigquery, String projectId, String datasetIdForLocation, String sql)
            throws InterruptedException {
    	 String loc = getDatasetLocation(bigquery, projectId, datasetIdForLocation);

    	    QueryJobConfiguration cfg = QueryJobConfiguration.newBuilder(sql)
    	            .setUseLegacySql(false)
    	            .build();

    	    JobId jobId = JobId.newBuilder()
    	            .setProject("crp-pro-cx-analitica") // <-- IMPORTANTE: aquí va el projectId real, no el SQL
    	            .setLocation(loc)
    	            .setJob(java.util.UUID.randomUUID().toString())
    	            .build();

    	    Job job = bigquery.create(JobInfo.newBuilder(cfg).setJobId(jobId).build()).waitFor();

    	    if (job == null) throw new RuntimeException("BigQuery job desapareció.");
    	    if (job.getStatus().getError() != null) {
    	        throw new RuntimeException("BigQuery error: " + job.getStatus().getError().toString());
    	    }
    }

    
    private static String getDatasetLocation(BigQuery bigquery, String projectId, String datasetId) {
        Dataset ds = bigquery.getDataset(DatasetId.of(projectId, datasetId));
        if (ds == null) throw new RuntimeException("Dataset no existe: " + projectId + ":" + datasetId);
        return ds.getLocation(); // ejemplo: "US", "EU", "northamerica-northeast1", etc.
    }
//    private static void runStatement(BigQuery bigquery, String sql, String location) throws InterruptedException {
//        QueryJobConfiguration cfg = QueryJobConfiguration.newBuilder(sql)
//                .setUseLegacySql(false)
//                .build();
//
//        JobId jobId = JobId.newBuilder()
//                .setLocation(location) // típicamente "US"
//                .setJob(java.util.UUID.randomUUID().toString())
//                .build();
//
//        Job job = bigquery.create(com.google.cloud.bigquery.JobInfo.newBuilder(cfg).setJobId(jobId).build());
//        job = job.waitFor();
//
//        if (job == null) throw new RuntimeException("BigQuery job desapareció.");
//        if (job.getStatus().getError() != null) {
//            throw new RuntimeException("BigQuery error: " + job.getStatus().getError().toString());
//        }
//    }

    private static String sqlStringLiteral(String s) {
        if (s == null) return "''";
        // StandardSQL: escapar ' duplicándolo
        return "'" + s.replace("'", "''") + "'";
    }

    private static void describeViewAndColumns(
            BigQuery bigquery,
            String projectId,
            String datasetViews,
            String viewName,
            String viewDescription,
            java.util.Map<String, String> columnDescriptions
    ) throws InterruptedException {

        String fqn = "`" + projectId + "." + datasetViews + "." + viewName + "`";

        // Descripción de la vista
        runStatement(bigquery, projectId, datasetViews, 
                "ALTER VIEW " + fqn + " SET OPTIONS (description = " + sqlStringLiteral(viewDescription) + ")"
        );

        // Descripción de columnas
        for (java.util.Map.Entry<String, String> e : columnDescriptions.entrySet()) {
            String col = e.getKey();
            String desc = e.getValue();
            runStatement(bigquery, projectId, datasetViews, 
                    "ALTER VIEW " + fqn + " ALTER COLUMN " + col + " SET OPTIONS (description = " + sqlStringLiteral(desc) + ")"
            );
        }
    }

    
}
