package mx.com.liverpool.p360.services.core.completeness;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.json.*;
import mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager;

public final class MandatoryCompletenessIncrementalCheck {
    private static int checks;
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++; System.out.println("PASS " + label);
    }
    private static String event(String entity, String id, JSONArray fields, String xml) {
        return new JSONObject().put("entityItemChange",new JSONObject().put("_entity",entity)
                .put("_identifier",id).put("_changedField",fields).put("_changeSummary",xml)).toString();
    }
    public static void main(String[] args) throws Exception {
        String xml="<changes><product><_characteristicRecords><_qualification><characteristic>"
                + "<_code>Material</_code></characteristic></_qualification><_recordLang><values>"
                + "<_old>cotton</_old></values></_recordLang></_characteristicRecords></product></changes>";
        var parsed=MandatoryCompletenessChange.parse(event("Product2G","MC_TEST",new JSONArray()
                .put("Product2GCharacteristicValueLang.Value"),xml));
        check(!parsed.force() && parsed.characteristics().equals(Set.of("Material")),"deleted characteristic value retains qualification");
        check(MandatoryCompletenessChange.parse(event("Product2G","MC_TEST",new JSONArray()
                .put("Product2G.MandatoryCompleteness"),""))==null,"self-write does not requeue");
        check(MandatoryCompletenessChange.parse(event("Product2G","MC_TEST",new JSONArray()
                .put("Product2G.MandatoryCompleteness").put("Product2G.Business"),xml)).force(),"mixed self/business change retained");
        check(MandatoryCompletenessChange.parse(event("Article","MC_TEST",new JSONArray()
                .put("ArticleCharacteristicValueLang.Value"),xml)).characteristics().contains("Material"),"Article characteristic parsed");
        check(MandatoryCompletenessChange.parse(event("Product2G","MC_TEST",new JSONArray()
                .put("Product2GCharacteristicValueLang.Value"),"<changes/>" )).force(),"missing qualification conservatively recalculates");
        boolean secure=false;
        try { MandatoryCompletenessChange.parse(event("Product2G","MC_TEST",new JSONArray().put("Product2GCharacteristicValueLang.Value"),
                "<!DOCTYPE a [<!ENTITY x SYSTEM 'file:///not-read'>]><a>&x;</a>")); }
        catch (Exception expected) { secure=true; }
        check(secure,"external XML entities prohibited");
        check(parsed.merge(new MandatoryCompletenessChange("Product2G","MC_TEST",false,Set.of("Size")))
                .characteristics().equals(Set.of("Material","Size")),"coalescing retains both changed characteristics");
        check(parsed.merge(new MandatoryCompletenessChange("Product2G","MC_TEST",true,Set.of())).force(),"structural invalidation dominates");
        check(MandatoryCompletenessChange.compact("Product2G","MC_TEST",false,Set.of("X".repeat(4001))).force(),"bounded payload falls back to full product calculation");
        if (args.length>0 && args[0].equals("database")) database();
        if (args.length>0 && args[0].equals("intake")) intake();
        System.out.println("PASSED="+checks);
    }
    private static void intake() throws Exception {
        check(MandatoryCompletenessIntake.enabled(),"durable intake enabled for integration check");
        String id="MC_INTAKE_"+UUID.randomUUID();
        var ack=new java.util.concurrent.atomic.AtomicBoolean();
        var running=new java.util.concurrent.atomic.AtomicBoolean(true);
        var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        String body=event("Product2G",id,new JSONArray().put("Product2G.Business"),"<changes/>");
        javax.jms.TextMessage message=(javax.jms.TextMessage)java.lang.reflect.Proxy.newProxyInstance(
                javax.jms.TextMessage.class.getClassLoader(),new Class<?>[]{javax.jms.TextMessage.class},(p,m,a)->{
                    if(m.getName().equals("getText"))return body;
                    if(m.getName().equals("getJMSMessageID"))return id;
                    if(m.getName().equals("acknowledge")) {
                        try(Connection read=new QuickJdbcConnectionManager().openConnection(true);
                            var s=read.prepareStatement("select count(*) from "+MandatoryCompletenessPendingDao.TABLE+" where ENTITY_IDENTIFIER=?")) {
                            s.setNString(1,id);try(var r=s.executeQuery()) {r.next();if(r.getInt(1)!=1)throw new AssertionError("ACK before committed pending");}
                        }
                        ack.set(true);
                    }
                    return null;
                });
        javax.jms.MessageConsumer delegate=(javax.jms.MessageConsumer)java.lang.reflect.Proxy.newProxyInstance(
                javax.jms.MessageConsumer.class.getClassLoader(),new Class<?>[]{javax.jms.MessageConsumer.class},
                (p,m,a)->m.getName().startsWith("receive")?message:null);
        javax.jms.MessageConsumer wrapped=MandatoryCompletenessIntake.wrap(delegate,true,running::get);
        try(Connection lock=new QuickJdbcConnectionManager().openConnection(false)) {
            try(var s=lock.createStatement();var r=s.executeQuery("select ID from "+MandatoryCompletenessPendingDao.CONTROL+" where ID=1 for update")){r.next();}
            Thread receiver=new Thread(()->{try{wrapped.receive(1);}catch(Throwable e){failure.set(e);}});
            receiver.start();
            Thread.sleep(700);
            check(!ack.get(),"JMS not acknowledged while journal cannot commit");
            lock.rollback();
            receiver.join(15000);
            running.set(false);
            if(receiver.isAlive()){receiver.interrupt();receiver.join(7000);}
            check(!receiver.isAlive() && failure.get()==null && ack.get(),"JMS acknowledged only after durable commit");
        } finally {
            running.set(false);wrapped.close();
            try(Connection c=new QuickJdbcConnectionManager().openConnection(false)) {
                var dao=new MandatoryCompletenessPendingDao(c);
                for(var p:dao.due(1000))if(p.change().identifier().equals(id))dao.complete(p);
                c.commit();
            }
        }
    }
    private static void database() throws Exception {
        String id="MC_CHECK_"+UUID.randomUUID();
        try (Connection c=new QuickJdbcConnectionManager().openConnection(false)) {
            MandatoryCompletenessPendingDao dao=new MandatoryCompletenessPendingDao(c);
            dao.preflight();
            var a=new MandatoryCompletenessChange("Product2G",id,false,Set.of("A"));
            dao.enqueue(a);
            var first=dao.due(1000).stream().filter(p->p.change().identifier().equals(id)).findFirst().orElseThrow();
            dao.enqueue(new MandatoryCompletenessChange("Product2G",id,false,Set.of("B")));
            dao.complete(first);
            var updated=dao.due(1000).stream().filter(p->p.change().identifier().equals(id)).findFirst().orElseThrow();
            check(updated.version()==first.version()+1 && updated.change().characteristics().equals(Set.of("A","B")),
                    "new change survives completion of older generation");
            dao.complete(updated);
            check(dao.due(1000).stream().noneMatch(p->p.change().identifier().equals(id)),"successful pending removed immediately");
            c.rollback();
            try (var s=c.createStatement()) { s.executeUpdate("update "+MandatoryCompletenessPendingDao.CONTROL+" set PENDING_COUNT=MAX_PENDING where ID=1"); }
            boolean full=false;
            try { dao.enqueue(a); } catch (SQLException expected) { full="MCFULL".equals(expected.getSQLState()); }
            check(full,"hard pending cap rejects new key without dropping an existing key");
            c.rollback();
            dao.enqueue(a);
            var retry=dao.due(1000).stream().filter(p->p.change().identifier().equals(id)).findFirst().orElseThrow();
            dao.fail(retry,new SQLException("test failure"));
            check(dao.due(1000).stream().noneMatch(p->p.change().identifier().equals(id)),"failure retained with delayed retry");
            try (var s=c.prepareStatement("select ATTEMPTS,LAST_ERROR from "+MandatoryCompletenessPendingDao.TABLE+" where ENTITY_IDENTIFIER=?")) {
                s.setNString(1,id); try(var r=s.executeQuery()) { check(r.next()&&r.getInt(1)==1&&r.getString(2).contains("test failure"),"bounded retry diagnostic persisted"); }
            }
            c.rollback();
            try (var s=c.createStatement();var r=s.executeQuery("select PENDING_COUNT,(select count(*) from "+MandatoryCompletenessPendingDao.TABLE+") from "+MandatoryCompletenessPendingDao.CONTROL+" where ID=1")) {
                check(r.next()&&r.getLong(1)==r.getLong(2),"quota count agrees after rollback; no test data retained");
            }
            // Runtime equality: the new applicability filter must be exactly the calculation CTE prefix.
            var configField=MandatoryCompletenessService.class.getDeclaredField("CONFIG_SQL"); configField.setAccessible(true);
            var batchField=MandatoryCompletenessService.class.getDeclaredField("BATCH_SQL"); batchField.setAccessible(true);
            check(((String)batchField.get(null)).startsWith((String)configField.get(null)),"metadata filter shares calculation SQL");
            CompletenessWorkDao work=new CompletenessWorkDao(c);
            String run=UUID.randomUUID().toString();
            work.replaceRun(run,List.of("1754611647184002"));
            MandatoryCompletenessService service=new MandatoryCompletenessService(c);
            var applicable=service.applicableCharacteristics(run);
            var results=service.calculateWorkBatch(run);
            check(results.size()==1 && !applicable.getOrDefault("1754611647184002",Set.of()).isEmpty(),"real metadata filter and calculator run against Oracle");
            System.out.println("READ_ONLY_SAMPLE_PERCENT="+results.get(0).getMandatory().getPercentage());
            c.rollback();
        }
    }
}
