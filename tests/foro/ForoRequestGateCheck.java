import mx.com.liverpool.p360.services.core.ForoRequestGate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ForoRequestGateCheck {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+label);}
    public static void main(String[] args)throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(10);
        try {
            CountDownLatch entered=new CountDownLatch(8),release=new CountDownLatch(1);
            List<Future<String>> independent=new ArrayList<>();
            for(int i=0;i<8;i++){final int id=i;independent.add(pool.submit(()->ForoRequestGate.execute("{\"id\":"+id+"}",()->{entered.countDown();release.await();return "ok";})));}
            boolean all=entered.await(5,TimeUnit.SECONDS);release.countDown();
            check(all,"eight independent requests enter concurrently");
            for(Future<String> f:independent)check("ok".equals(f.get(5,TimeUnit.SECONDS)),"independent request completes without rejection");
            AtomicInteger calls=new AtomicInteger();CountDownLatch started=new CountDownLatch(1),finish=new CountDownLatch(1);
            Future<String> leader=pool.submit(()->ForoRequestGate.execute("{\"b\":2,\"a\":1}",()->{calls.incrementAndGet();started.countDown();finish.await();return "shared";}));
            check(started.await(5,TimeUnit.SECONDS),"leader started");
            Future<String> follower=pool.submit(()->ForoRequestGate.execute("{\"a\":1,\"b\":2}",()->{calls.incrementAndGet();return "wrong";}));
            Thread.sleep(31500);
            boolean waiting=!follower.isDone();finish.countDown();
            check(waiting,"duplicate remains attached beyond 30 seconds without timeout/rejection");
            check("shared".equals(leader.get(5,TimeUnit.SECONDS))&&"shared".equals(follower.get(5,TimeUnit.SECONDS))&&calls.get()==1,"canonical identical requests share one execution");
            try{ForoRequestGate.execute("{\"failure\":1}",()->{throw new IllegalArgumentException("test");});throw new AssertionError("failure lost");}
            catch(IllegalArgumentException expected){check(true,"real action error preserved");}
            check("recovered".equals(ForoRequestGate.execute("{\"failure\":1}",()->"recovered")),"failed request removed from in-flight map");
            try{ForoRequestGate.execute("not-json",()->"wrong");throw new AssertionError("invalid JSON accepted");}
            catch(org.json.JSONException expected){check(true,"JSON validation preserved");}
            System.out.println("ALL_CHECKS_PASSED");
        } finally {pool.shutdownNow();}
    }
}
