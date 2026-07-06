package mx.com.liverpool.p360.services.core.temp.gcp;

public class EscritorDatos {
	
//	  private final BigQuery bq;
//	  private final String fqTable; // `dataset.table`
//	  private final int maxRowsPerBatch;
//	  private final List<Map<String, QueryParameterValue>> batch = new ArrayList<>();
//
//	  public EscritorDatos(String dataset, String table, int maxRowsPerBatch) {
//	    this.bq = BigQueryOptions.getDefaultInstance().getService();
//	    this.fqTable = dataset + "." + table;
//	    this.maxRowsPerBatch = maxRowsPerBatch;
//	  }
//
//	  public void addRow(long id, String descripcion, BigDecimal monto) throws Exception {
//	    Map<String, QueryParameterValue> fields = new LinkedHashMap<>();
//	    fields.put("id", QueryParameterValue.int64(id));
//	    fields.put("descripcion", QueryParameterValue.string(descripcion));
//	    fields.put("monto", QueryParameterValue.numeric(monto.setScale(4))); // 4 decimales
//
//	    batch.add(fields);
//	    if (batch.size() >= maxRowsPerBatch) flush();
//	  }
//
//	  public void flush() throws Exception {
//	    if (batch.isEmpty()) return;
//
//	    // Construye el parámetro ARRAY<STRUCT<id INT64, descripcion STRING, monto NUMERIC>>
//	    QueryParameterValue[] structs = batch.stream()
//	        .map(QueryParameterValue::struct)
//	        .toArray(QueryParameterValue[]::new);
//
//	    QueryParameterValue rowsParam = QueryParameterValue.array(
//	        structs,
//	        QueryParameterType.struct(
////	            new QueryParameterType.StructField("id", QueryParameterType.Int64),
////	            new QueryParameterType.StructField("descripcion", QueryParameterType.String),
////	            new QueryParameterType.StructField("monto", QueryParameterType.Numeric)
//	        )
//	    );
//
//	    String sql = "INSERT INTO `" + fqTable + "` (id, descripcion, monto) SELECT id, descripcion, monto FROM UNNEST(@rows)";
//
//	    QueryJobConfiguration qjc = QueryJobConfiguration.newBuilder(sql)
//	        .addNamedParameter("rows", rowsParam)
//	        .build();
//
//	    Job job = bq.create(JobInfo.of(qjc)).waitFor();
//	    if (job == null || job.getStatus().getError() != null) {
//	      throw new RuntimeException("Insert failed: " + (job == null ? "job disappeared" : job.getStatus().getError()));
//	    }
//	    batch.clear();
//	  }
	
}
