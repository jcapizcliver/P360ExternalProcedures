import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;
import mx.com.liverpool.p360.services.core.*;
import mx.com.liverpool.p360.services.core.completeness.*;

public class CompletenessWriterCheck {
    static class Transport extends RESTWrapper {
        int calls;
        boolean failure;
        public void writeData(String api, String entity, String sub, Map<String,String> qp,
                org.json.JSONObject request, SimpleWriteProcessor processor) {
            calls++;
            if (failure && calls == 1) throw new IllegalStateException("simulated transport failure");
            processor.process("{\"counters\":{\"errors\":0,\"objectsWithErrors\":0}}");
            // Deliberately retain rows to verify writer clears its reusable request.
        }
    }
    static class Snapshot {
        List<String> statuses = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        Snapshot(Connection c) {}
        public void markMandatorySync(Collection<String> values, String status, String message) {
            ids.addAll(values); statuses.add(status);
        }
    }
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        Connection c = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class}, (p,m,a) -> null);
        List<CompletenessResult> results = new ArrayList<>();
        for (int i=0; i<5; i++) results.add(new CompletenessResult("P"+i, (long)i,
            "T", "LVP", "Liverpool", new CompletenessResult.MetricResult(1,1,0,
            new BigDecimal("100.0000"),true,"OK",null),Instant.now(),"test"));
        for (int mode=0; mode<3; mode++) {
            Snapshot snapshot = new Snapshot(c);
            Transport transport = new Transport(); transport.failure = mode != 0;
                        String[] bound = new String[4];
            PreparedStatement ps = (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class}, (p,m,a) -> {
                    if (m.getName().equals("setString") || m.getName().equals("setNString")) bound[(Integer)a[0]]=(String)a[1];
                    if (m.getName().equals("addBatch")) { snapshot.ids.add(bound[3]); snapshot.statuses.add(bound[1]); }
                    if (m.getName().equals("executeBatch")) return new int[0];
                    return null;
                });
            Connection db = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (p,m,a) -> m.getName().equals("prepareStatement") ? ps : null);
            MandatoryCompletenessP360Writer writer = new MandatoryCompletenessP360Writer(db,new CompletenessSnapshotDao(db),2);
            Field field = MandatoryCompletenessP360Writer.class.getDeclaredField("rw");
            field.setAccessible(true); field.set(writer,transport);
            if (mode == 1) {
                try { writer.write(results,true); throw new AssertionError("Expected stop"); }
                catch (IllegalStateException expected) {}
                check(transport.calls==1 && snapshot.ids.size()==2);
                check(snapshot.statuses.equals(List.of("FAILED","FAILED")));
            } else {
                check(writer.write(results,false) == (mode == 0));
                check(transport.calls==3 && snapshot.ids.size()==5);
                check(new HashSet<>(snapshot.ids).size()==5);
            }
        }
        Class<?> config = Class.forName(MandatoryCompletenessBootstrap.class.getName()+"$Config");
        Method parse = config.getDeclaredMethod("parse",String[].class); parse.setAccessible(true);
        for (String arg : List.of("--dry-run=tru","--max-products=-1","--unknown=true","--rest-batch-size=0")) {
            try { parse.invoke(null,(Object)new String[]{arg}); throw new AssertionError(arg); }
            catch (InvocationTargetException expected) { check(expected.getCause() instanceof IllegalArgumentException); }
        }
        System.out.println("PASS: three writer batch scenarios and four invalid startup options");
    }
}
