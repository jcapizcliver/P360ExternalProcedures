package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;

import mx.com.liverpool.p360.services.core.RESTWrapper;

/**
 * Ejecuta carga List API para valores faltantes detectados en tabla origen-vs-P360.
 *
 * Args:
 *   0 jdbcUrl
 *   1 dbUser
 *   2 dbPassword
 *   3 tablaFaltantes
 *   4 batchSize opcional, default 200
 *
 * La tabla/query debe exponer estas columnas:
 *   ENTITY_TYPE                  Article / Product2G
 *   IDENTIFIER                   Identifier del objeto destino
 *   CHARACTERISTIC_IDENTIFIER    Identifier de la caracteristica
 *   VALUE_ORIGEN                 Valor a mandar a List API
 *
 * Puedes cambiar buildMissingSql() si tus nombres reales son otros.
 */
public class MissingDataCasterDeployment_v001 {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Uso: java ...MissingDataCasterDeployment_v001 <jdbcUrl> <dbUser> <dbPassword> <tablaFaltantes> [batchSize]");
            System.exit(2);
        }

        String jdbcUrl = args[0];
        String dbUser = args[1];
        String dbPassword = args[2];
        String table = args[3];
        int batchSize = args.length >= 5 ? Integer.parseInt(args[4]) : 200;

        RESTWrapper rw = new RESTWrapper();
        Map<String, CharacteristicInfo> characteristics = loadCharacteristics(rw);
        Map<String, MissingDataCaster_v001> casters = new LinkedHashMap<>();

        long read = 0;
        long skipped = 0;

        try (Connection cn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
             PreparedStatement ps = cn.prepareStatement(buildMissingSql(table));) {

            ps.setFetchSize(1000);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    read++;
                    String entity = normalizeEntity(rs.getString("ENTITY_TYPE"));
                    String identifier = rs.getString("IDENTIFIER");
                    String att = rs.getString("CHARACTERISTIC_IDENTIFIER");
                    String value = rs.getString("VALUE_ORIGEN");

                    if (entity == null || att == null || att.trim().isEmpty()) {
                        skipped++;
                        continue;
                    }

                    CharacteristicInfo ci = characteristics.get(att);
                    if (ci == null || !ci.entities.contains(entity)) {
                        System.out.println("SKIP unknown/not allowed characteristic: entity=" + entity + " att=" + att + " identifier=" + identifier);
                        skipped++;
                        continue;
                    }

                    String key = entity + "|" + att;
                    MissingDataCaster_v001 caster = casters.get(key);
                    if (caster == null) {
                        caster = new MissingDataCaster_v001(entity, rw, att, ci.dataType, batchSize, "remainingData_missing_v001");
                        casters.put(key, caster);
                    }
                    caster.addValue(identifier, value);
                }
            }
        } finally {
            for (MissingDataCaster_v001 caster : casters.values()) {
                try { caster.close(); } catch (Exception e) { e.printStackTrace(); }
            }
        }

        long sent = 0;
        for (MissingDataCaster_v001 caster : casters.values()) sent += caster.getSent();
        System.out.println("DONE read=" + read + " skipped=" + skipped + " casters=" + casters.size() + " sent=" + sent);
    }

    private static String buildMissingSql(String table) {
        // Ajusta aquí si tu tabla real tiene otros nombres.
        // DISTINCT evita repetir el mismo valor exacto en el mismo objeto/caracteristica.
        return "select distinct " +
               "ENTITY_TYPE, IDENTIFIER, CHARACTERISTIC_IDENTIFIER, VALUE_ORIGEN " +
               "from " + table + " " +
               "where VALUE_ORIGEN is not null " +
               "and IDENTIFIER is not null " +
               "and CHARACTERISTIC_IDENTIFIER is not null " +
               "order by ENTITY_TYPE, CHARACTERISTIC_IDENTIFIER, IDENTIFIER";
    }

    private static String normalizeEntity(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.equalsIgnoreCase("ARTICLE") || v.equals("1000")) return "Article";
        if (v.equalsIgnoreCase("PRODUCT2G") || v.equalsIgnoreCase("PRODUCT") || v.equals("1100")) return "Product2G";
        return v;
    }

    private static Map<String, CharacteristicInfo> loadCharacteristics(RESTWrapper rw) {
        Map<String, CharacteristicInfo> characteristics = new HashMap<>();
        Map<String, String> qp = new HashMap<>();
        qp.put("fields", "Characteristic.Identifier,Characteristic.DataType,Characteristic.Entities");
        qp.put("query", "Characteristic.ParentCharacteristic is empty and not Characteristic.DataType = \"NONE\" and not Characteristic.Entities is empty and not Characteristic.Identifier wildcard \"%_Rechazo\"");
        qp.put("pageSize", "5000");

        rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> {
            JSONArray values = row.getJSONArray("values");
            String identifier = values.getString(0);
            String dataType = values.getString(1);
            JSONArray entities = values.getJSONArray(2);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < entities.length(); i++) {
                if (i > 0) sb.append(',');
                sb.append(String.valueOf(entities.get(i)));
            }
            characteristics.put(identifier, new CharacteristicInfo(dataType, sb.toString()));
        });
        return characteristics;
    }

    private static class CharacteristicInfo {
        final String dataType;
        final String entities;
        CharacteristicInfo(String dataType, String entities) {
            this.dataType = dataType;
            this.entities = entities == null ? "" : entities;
        }
    }
}
