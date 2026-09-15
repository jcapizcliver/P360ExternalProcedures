package mx.com.liverpool.p360.services.core;
import org.json.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public final class ForoMitigationTest {
    static int count;
    static void check(boolean b,String m){count++;if(!b)throw new AssertionError(m);}
    static JSONObject r(String code,Object value){return new JSONObject().put("_qualification",new JSONObject().put("characteristic",new JSONObject().put("_code",code))).put("_recordLang",new JSONArray().put(new JSONObject().put("_qualification",new JSONObject().put("language",new JSONObject().put("_code","zxx"))).put("values",new JSONArray().put(value))));}
    public static void main(String[] args)throws Exception {
        JSONObject snapshot=new JSONObject(Files.readString(Path.of(args[0])).replace("\ufeff",""));
        JSONArray current=snapshot.getJSONObject("_data").getJSONArray("_characteristicRecords");
        JSONArray desired=new JSONArray().put(r("AssignTakeNoTake","NO TOMAR")).put(r("AdmissionDate","2099-01-01T00:00:00Z")).put(r("AssignTakeNoTakeReason","Tiene imagen principal"));
        check(TakeNoTakeWriter.delta(current,desired).length()==0,"Real Article: date-only replay must not write");
        desired.put(0,r("AssignTakeNoTake","TOMAR"));
        check(TakeNoTakeWriter.delta(current,desired).length()==2,"Decision transition writes decision and date");
        desired.put(0,r("AssignTakeNoTake","NO TOMAR"));desired.put(2,r("AssignTakeNoTakeReason","Nueva razon"));
        JSONArray changed=TakeNoTakeWriter.delta(current,desired);
        check(changed.length()==1,"Reason changes retain admission date");
        check(TakeNoTakeWriter.delta(null,desired).length()==3,"Initial values still written");
        check(TakeNoTakeWriter.delta(new JSONArray().put(r("A",new JSONObject().put("_code","X").put("_label","Etiqueta"))),new JSONArray().put(r("A","Etiqueta"))).length()==0,"Lookup label equivalence");
        check(TakeNoTakeWriter.delta(new JSONArray().put(r("A",false)),new JSONArray().put(r("A","false"))).length()==0,"Boolean representation equivalence");
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch entered=new CountDownLatch(2),release=new CountDownLatch(1);AtomicInteger calls=new AtomicInteger();
        Callable<String> action=()->{calls.incrementAndGet();entered.countDown();if(!release.await(5,TimeUnit.SECONDS))throw new TimeoutException();return "ok";};
        Future<String> one=pool.submit(()->ForoRequestGate.execute("{\"a\":1,\"b\":2}",action));
        Future<String> two=pool.submit(()->ForoRequestGate.execute("{\"a\":2}",action));
        check(entered.await(5,TimeUnit.SECONDS),"Two independent leaders run");
        try{ForoRequestGate.execute("{\"a\":3}",()->"unexpected");throw new AssertionError("Capacity not bounded");}catch(RejectedExecutionException expected){count++;}
        AtomicReference<String> answer=new AtomicReference<>();Thread follower=new Thread(()->{try{answer.set(ForoRequestGate.execute("{ \"b\":2, \"a\":1 }",()->{calls.incrementAndGet();return "bad";}));}catch(Exception e){answer.set(e.toString());}});follower.start();
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);while(follower.getState()!=Thread.State.TIMED_WAITING&&System.nanoTime()<end)Thread.sleep(5);
        check(follower.getState()==Thread.State.TIMED_WAITING,"Identical request waits for shared execution");
        release.countDown();one.get();two.get();follower.join(2000);pool.shutdownNow();
        check("ok".equals(answer.get())&&calls.get()==2,"Canonical identical request shares one result");
        check("retry".equals(ForoRequestGate.execute("{\"a\":3}",()->"retry")),"Capacity recovers");
        try{ForoRequestGate.execute("{\"fail\":1}",()->{throw new IllegalArgumentException();});}catch(IllegalArgumentException expected){}
        check("ok".equals(ForoRequestGate.execute("{\"fail\":1}",()->"ok")),"Failures do not poison retries");
        System.out.println("PASS "+count+" mitigation checks; no network writes");
    }
}
