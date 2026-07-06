package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ProcesamientoDeEstatusModification {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();

	private static String[] header = null;
	private static int a = 0;
	

	private static final String[][] tuplas = collectStatusInformation();
	private static final java.util.Map<String, String> espMap = fromTuples(tuplas, 1);
	private static final java.util.Map<String, String> engMap = fromTuples(tuplas, 2);

	private static final java.util.Map<String, java.util.List<Transicion>> transicionesA = new java.util.HashMap<>();
    private static final java.util.Map<String, java.util.List<Transicion>> transicionesDesde = new java.util.HashMap<>();
	
    private static final java.util.Map<String, String> statusNameById = statusNameById(tuplas);

    
    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.println("Uso: java ProcesamientoDeEstatusModification input.csv timeline_estatus.csv");
            return;
        }

        java.nio.file.Path input = java.nio.file.Paths.get(args[0]);
        java.nio.file.Path output = java.nio.file.Paths.get(args[1]);

        try (java.io.PrintWriter out = new java.io.PrintWriter(
                new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(output.toFile()),
                    java.nio.charset.StandardCharsets.UTF_8
                )
            )) {

            out.println("Identifier,Seq,Fecha,StatusID,StatusName,RawRecord");

            SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
                '"',
                ',',
                '\\',
                "\n",
                java.nio.charset.StandardCharsets.UTF_8,
                row -> {
                    a++;

                    if (row.length == 0) {
                        return;
                    }

                    if (header == null) {
                        header = row;
                        System.out.println(java.util.Arrays.asList(header));
                        return;
                    }

                    if (row.length <= 9) {
                        return;
                    }

                    String identifier = row[0];
                    String statusModification = row[9];

                    java.util.List<EventoEstatus> eventos = processStatusModificationTimeline(identifier, statusModification);

                    for (EventoEstatus ev : eventos) {
                        out.println(
                            csv(ev.identifier) + "," +
                            ev.seq + "," +
                            csv(formatExcelDate(ev.fecha)) + "," +
                            csv(ev.statusId) + "," +
                            csv(ev.statusName) + "," +
                            csv(ev.rawRecord)
                        );
                    }
                }
            );

            parser.parse(input);
        }

        System.out.println("Registros leídos: " + a);
        System.out.println("Timeline generado: " + output.toAbsolutePath());
    }
    
    private static java.util.List<EventoEstatus> processStatusModificationTimeline(
            String identifier,
            String statusModification
    ) {
        java.util.List<EventoEstatus> eventos = new java.util.ArrayList<>();

        if (statusModification == null || statusModification.trim().isEmpty()) {
            return eventos;
        }

        String[] records = statusModification
            .replace("\\r\\n", "\n")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split("\n");

        java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile(
            "(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{2}(?:\\s*[AaPp][Mm])?)"
        );

        java.util.regex.Pattern statusPattern = java.util.regex.Pattern.compile("\"([^\"]+)\"");

        java.text.SimpleDateFormat sdfEng = new java.text.SimpleDateFormat(
            "M/d/yyyy h:mm a",
            java.util.Locale.US
        );
        sdfEng.setLenient(false);

        java.text.SimpleDateFormat sdfEsp = new java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            java.util.Locale.US
        );
        sdfEsp.setLenient(false);

        for (int i = 0; i < records.length; i++) {
            String record = records[i];

            if (record == null) {
                continue;
            }

            record = record.trim();

            if (record.isEmpty()) {
                continue;
            }

            java.util.regex.Matcher mDate = datePattern.matcher(record);
            if (!mDate.find()) {
                continue;
            }

            java.util.regex.Matcher mStatus = statusPattern.matcher(record);
            if (!mStatus.find()) {
                continue;
            }

            try {
                String datePart = mDate.group(1).trim();
                String rawStatus = mStatus.group(1).trim();

                java.util.Date fecha = null;
                String statusId = null;

                if (record.startsWith("El usuario")) {
                    fecha = sdfEsp.parse(datePart);
                    statusId = espMap.get(rawStatus);
                } else if (record.startsWith("The user")) {
                    fecha = sdfEng.parse(datePart.toUpperCase(java.util.Locale.US));
                    statusId = engMap.get(rawStatus);
                }

                if (fecha == null || statusId == null) {
                    continue;
                }

                EventoEstatus ev = new EventoEstatus();
                ev.identifier = identifier;
                ev.fecha = fecha;
                ev.statusId = statusId;
                ev.statusName = statusNameById.get(statusId);
                ev.rawRecord = record;

                eventos.add(ev);

            } catch (java.text.ParseException e) {
                logE(e);
            }
        }

        /*
         * Para Excel conviene orden cronológico:
         * más viejo -> más reciente.
         */
        java.util.Collections.sort(eventos, new java.util.Comparator<EventoEstatus>() {
            @Override
            public int compare(EventoEstatus a, EventoEstatus b) {
                return a.fecha.compareTo(b.fecha);
            }
        });

        for (int i = 0; i < eventos.size(); i++) {
            eventos.get(i).seq = i + 1;
        }

        return eventos;
    }
    
    private static String formatExcelDate(java.util.Date date) {
        if (date == null) {
            return "";
        }

        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static java.util.Map<String, String> statusNameById(String[][] tuplas) {
        java.util.Map<String, String> map = new java.util.TreeMap<>();
        if (tuplas != null) {
            for (int i = 0; i < tuplas.length; i++) {
                map.put(tuplas[i][0], tuplas[i][1]); // ID -> etiqueta español
            }
        }
        return map;
    }
	
	private static void imprimeEstadisticas(EstadisticasTiempo e) {
	    if (e == null || e.count == 0) {
	        System.out.println("\tSin datos");
	        return;
	    }

	    System.out.println("\tCount: " + e.count);
	    System.out.println("\tMin: " + rw.getRw().formatTime(e.min));
	    System.out.println("\tMax: " + rw.getRw().formatTime(e.max));
	    System.out.println("\tPromedio: " + rw.getRw().formatTime(e.promedio));
	}
	
	private static EstadisticasTiempo calculaEstadisticas(java.util.List<Transicion> transiciones) {
	    EstadisticasTiempo e = new EstadisticasTiempo();

	    if (transiciones == null || transiciones.isEmpty()) {
	        return e;
	    }

	    long min = Long.MAX_VALUE;
	    long max = Long.MIN_VALUE;
	    long suma = 0L;
	    int count = 0;

	    for (Transicion t : transiciones) {
	        if (t == null || t.tiempo == null) {
	            continue;
	        }

	        long tiempo = t.tiempo.longValue();

	        if (tiempo < min) {
	            min = tiempo;
	        }

	        if (tiempo > max) {
	            max = tiempo;
	        }

	        suma += tiempo;
	        count++;
	    }

	    if (count == 0) {
	        return e;
	    }

	    e.count = count;
	    e.min = min;
	    e.max = max;
	    e.promedio = suma / count;

	    return e;
	}

	private static final class EstadisticasTiempo {
	    private int count;
	    private Long min;
	    private Long max;
	    private Long promedio;
	}
	
	private static void processStatusModification(String statusModification) {
	    if (statusModification == null || statusModification.trim().isEmpty()) {
	        return;
	    }

	    String[] records = statusModification
	        .replace("\\r\\n", "\n")
	        .replace("\r\n", "\n")
	        .replace("\r", "\n")
	        .split("\n");

	    java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile(
	        "(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{2}(?:\\s*[AaPp][Mm])?)"
	    );

	    java.util.regex.Pattern statusPattern = java.util.regex.Pattern.compile("\"([^\"]+)\"");

	    java.text.SimpleDateFormat sdfEng = new java.text.SimpleDateFormat(
	        "M/d/yyyy h:mm a",
	        java.util.Locale.US
	    );
	    sdfEng.setLenient(false);

	    java.text.SimpleDateFormat sdfEsp = new java.text.SimpleDateFormat(
	        "dd/MM/yyyy HH:mm",
	        java.util.Locale.US
	    );
	    sdfEsp.setLenient(false);

	    java.util.Date prevLogDate = null;
	    java.util.Date currentLogDate = null;

	    String prevStatus = null;
	    String currentStatus = null;

	    java.util.Set<String> status = new java.util.TreeSet<>();

	    for (int i = 0; i < records.length; i++) {
	        String record = records[i];

	        if (record == null) {
	            continue;
	        }

	        record = record.trim();

	        if (record.isEmpty()) {
	            continue;
	        }

	        java.util.regex.Matcher m = datePattern.matcher(record);

	        if (!m.find()) {
	            continue;
	        }

	        try {
	            String datePart = m.group(1).trim();

	            if (record.startsWith("El usuario")) {
	                currentLogDate = sdfEsp.parse(datePart);
	            } else if (record.startsWith("The user")) {
	                currentLogDate = sdfEng.parse(datePart.toUpperCase(java.util.Locale.US));
	            } else {
	                currentLogDate = null;
	            }

	            if (currentLogDate == null) {
	                continue;
	            }

	            java.util.regex.Matcher mSP = statusPattern.matcher(record);

	            if (!mSP.find()) {
	                continue;
	            }

	            String rawStatus = mSP.group(1);

	            if (record.startsWith("El usuario")) {
	                currentStatus = espMap.get(rawStatus);
	            } else if (record.startsWith("The user")) {
	                currentStatus = engMap.get(rawStatus);
	            } else {
	                currentStatus = null;
	            }

	            if (currentStatus == null) {
	                continue;
	            }

	            status.add(currentStatus);

	            if (prevStatus != null && prevLogDate != null) {
	                Transicion t = new Transicion();
	                t.tiempo = prevLogDate.getTime() - currentLogDate.getTime();
	                t.estadoOrigen = currentStatus;
	                t.estadoDestino = prevStatus;

	                java.util.List<Transicion> tiempos = transicionesA.get(prevStatus);
	                if (tiempos == null) {
	                    tiempos = new java.util.ArrayList<>();
	                    transicionesA.put(prevStatus, tiempos);
	                }
	                tiempos.add(t);

	                tiempos = transicionesDesde.get(currentStatus);
	                if (tiempos == null) {
	                    tiempos = new java.util.ArrayList<>();
	                    transicionesDesde.put(currentStatus, tiempos);
	                }
	                tiempos.add(t);
	            }

	            prevLogDate = currentLogDate;
	            prevStatus = currentStatus;

	        } catch (java.text.ParseException e) {
	            logE(e);
	        }
	    }

//	    System.out.println("Transiciones A:");
//	    for (java.util.Map.Entry<String, java.util.List<Transicion>> entry : transicionesA.entrySet()) {
//	        System.out.println(entry.getKey());
//	        for (Transicion t : entry.getValue()) {
//	            System.out.println(
//	                "\t" + t.estadoOrigen +
//	                " - " + t.estadoDestino +
//	                " (" + rw.getRw().formatTime(t.tiempo) + ")"
//	            );
//	        }
//	    }
//
//	    System.exit(0);
	}
	
	private static final class Transicion{
		
		private String estadoOrigen;
		private String estadoDestino;
		private Long tiempo;
		
	}

	private static java.util.Map<String, String> fromTuples(String[][] tuplas, int index){
		java.util.Map<String, String> map = new java.util.TreeMap<>();
		if(tuplas != null && tuplas.length > 0) {
			if(index > 0 && index < tuplas[0].length) {
				for(int i=0; i<tuplas.length; i++) {
					map.put(tuplas[i][index], tuplas[i][0]);
				}
			}
		}
		return map;
	}
	
	private static String[][] collectStatusInformation(){
		RESTWorkshop rw = workshop;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = rw.makeRequest("GET", "/enum/Enum.ProductStatus", qp, null);
		java.util.Map<String, String> esp = new java.util.TreeMap<>();
		if(response != null) {
			org.json.JSONArray entries = response.getJSONArray("entries");
			for(int i=0; i<entries.length(); i++) {
				esp.put(entries.getJSONObject(i).getString("key"), entries.getJSONObject(i).getString("label"));
			}
		}
		rw.getRc().getHeader().put("Accept-Language", "en");
		response = rw.makeRequest("GET", "/enum/Enum.ProductStatus", qp, null);
		java.util.Map<String, String> eng = new java.util.TreeMap<>();
		if(response != null) {
			org.json.JSONArray entries = response.getJSONArray("entries");
			for(int i=0; i<entries.length(); i++) {
				eng.put(entries.getJSONObject(i).getString("key"), entries.getJSONObject(i).getString("label"));
			}
		}
		java.util.LinkedList<String[]> tuplas = new java.util.LinkedList<>();
		for(java.util.Map.Entry<String, String> entry : esp.entrySet()) {
			tuplas.addLast(new String[] {entry.getKey(), entry.getValue(), eng.get(entry.getKey())});
		}
		return tuplas.toArray(new String[][] {});
	}
	
	private static final class EventoEstatus {
	    private String identifier;
	    private int seq;
	    private java.util.Date fecha;
	    private String statusId;
	    private String statusName;
	    private String rawRecord;
	}

	private static void logE(Exception e) {
		e.printStackTrace();
	}
	
	private static void log(String message) {
		System.out.println(message);
	}
}
