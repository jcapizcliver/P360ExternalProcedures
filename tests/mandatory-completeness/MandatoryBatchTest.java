package mx.com.liverpool.p360.services.core.amqp.run;
import java.util.*;
import java.sql.*;
import mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager;
import mx.com.liverpool.p360.services.core.completeness.*;
import mx.com.liverpool.p360.services.core.completeness.MandatoryCompletenessPendingDao.Pending;
public class MandatoryBatchTest {
 static Pending row(String id,boolean force,String... codes){return new Pending(new MandatoryCompletenessChange("Article",id,force,Set.of(codes)),1,0,0);}
 static void check(boolean x,String msg){if(!x)throw new AssertionError(msg);}
 static String metric(CompletenessResult r){var m=r.getMandatory();return m.getTotal()+"/"+m.getPresent()+"/"+m.getMissing()+"/"+m.getPercentage()+"/"+m.getStatus();}
 public static void main(String[] args)throws Exception{
  Map<Pending,Set<String>> pages=new LinkedHashMap<>();
  pages.put(row("A1",false,"SIZE"),Set.of("P1","P2"));
  pages.put(row("A2",true),Set.of("P1"));
  pages.put(row("A3",false,"COLOR"),Set.of("P2"));
  pages.put(row("A4",false,"LOG"),Set.of("P3"));
  pages.put(row("A5",true),Set.of());
  check(MandatoryCompletenessChangeProcessor.selectProducts(pages,Map.of("P2",Set.of("COLOR"),"P3",Set.of("SIZE"))).equals(Set.of("P1","P2")),"force/union/dedup/irrelevant");
  check(MandatoryCompletenessChangeProcessor.selectProducts(Map.of(row("A6",false,"LOG"),Set.of("P3")),Map.of()).isEmpty(),"empty batch");
  System.out.println("PASS selection: forced product, merged events, unique IDs, irrelevant changes, empty targets");
  if(args.length==0)return;
  try(Connection c=new QuickJdbcConnectionManager().openConnection(false)){
   try{
    MandatoryCompletenessService service=new MandatoryCompletenessService(c);CompletenessWorkDao work=new CompletenessWorkDao(c);
    String run=UUID.randomUUID().toString();List<String> ids=List.of("1754611680435885","1754611684036611");
    work.replaceRun(run,ids);Map<String,String> batch=new HashMap<>();for(var v:service.calculateWorkBatch(run))batch.put(v.getProductIdentifier(),metric(v));work.deleteRun(run);
    for(String id:ids){work.replaceRun(run,List.of(id));var single=service.calculateWorkBatch(run);check(single.size()==1,"single count");check(Objects.equals(batch.get(id),metric(single.get(0))),"batch differs for "+id);work.deleteRun(run);}
    check(batch.size()==2,"batch count");c.rollback();System.out.println("PASS Oracle: batch equals individual metrics for both products; work rolled back; no P360 writes");
   }finally{c.rollback();}
  }
 }
}
