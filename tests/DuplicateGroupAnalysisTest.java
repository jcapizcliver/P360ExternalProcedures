package mx.com.liverpool.p360.services.core.sftp;
import org.json.*;
import java.nio.file.*;
import java.util.*;
public class DuplicateGroupAnalysisTest {
    static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static DuplicateGroupAnalysis.Node node(String id,String name,String value){
        DuplicateGroupAnalysis.Node n=new DuplicateGroupAnalysis.Node(new JSONObject().put("ID",1).put("Identifier",id).put("EntityID",1100));
        n.rows("ArticleDetail").put(new JSONObject().put("ID",4).put("ArticleRevisionID",1).put("CurrentStatus",1).put("Res_Int_02",123).put("EAN","conflict-ean").put("StatusModification","do not merge").put("Res_Text250_01",name).put("ModificationTimestamp","2026-09-07 20:00:00"));
        n.rows("ArticleCharactValue").put(new JSONObject().put("ID",9).put("CharacteristicID",10).put("RecordKey",0).put("ParentRecordKey",0).put("Value",value));
        n.rows("ArticleCharactValueLang").put(new JSONObject().put("ID",12).put("ArticleCharactValueID",9).put("EntityID",1100).put("LanguageID",34).put("Value",value));
        n.rows("ArticleCharactValue").put(new JSONObject().put("ID",11).put("CharacteristicID",20).put("RecordKey",0).put("ParentRecordKey",0).put("Value","log noise"));return n;
    }
    public static void main(String[] args)throws Exception {
        Map<String,String> meta=Map.of("10","SIZE","20","StatusModification");
        JSONObject a=DuplicateGroupAnalysis.normalized(node("A","Base",""),meta),b=DuplicateGroupAnalysis.normalized(node("B","Other","M"),meta);
        JSONObject d=a.getJSONObject("dbFields").getJSONObject("ArticleDetail");
        require(!d.has("Res_Int_02")&&!d.has("EAN")&&!d.has("StatusModification")&&!d.has("ID")&&!d.has("ModificationTimestamp"),"Protected fields leaked");
        require(a.getJSONObject("dbFields").getJSONObject("characteristics").length()==1,"Log characteristic retained");
        JSONObject result=ReconciliationPlan.plan(new JSONObject().put("base",new JSONObject().put("product",a).put("articles",new JSONArray())).put("sources",new JSONArray().put(new JSONObject().put("product",b).put("articles",new JSONArray()))));
        require(result.getJSONObject("product").getString("identifier").equals("A"),"Identifier overwritten");
        require(result.getJSONObject("product").getJSONObject("dbFields").getJSONObject("ArticleDetail").getString("Res_Text250_01").equals("Base"),"Conflict overwritten");
        require(result.getJSONObject("product").getJSONObject("dbFields").getJSONObject("characteristics").getJSONObject("[10,0,0]").getString("Value").equals("M"),"Missing value not complemented");
        require(result.getJSONArray("conflicts").length()>0,"Conflict not reported");
        require(a.getJSONObject("dbFields").getJSONObject("characteristics").getJSONObject("[10,0,0]").getString("Value").isEmpty(),"Input mutated");
        require(DuplicateGroupAnalysis.fields(new JSONObject().put("Res_Int_02",55),false).has("Res_Int_02"),"Domain field accidentally stripped");
        DuplicateGroupAnalysis.Node amb=node("A","","");amb.rows("ArticleDetail").put(new JSONObject());boolean blocked=false;try{DuplicateGroupAnalysis.normalized(amb,meta);}catch(IllegalStateException e){blocked=true;}require(blocked,"Ambiguous detail accepted");
        Path csv=Files.createTempFile("duplicate-analysis-test-",".csv");try{Files.writeString(csv,"IDENTIFIER,SKU,IDENTIFIERS_PER_SKU\nA,123,2\nB,123,2\n\n");require(DuplicateGroupAnalysis.csv(csv).get("123").size()==2,"CSV blank line handling");Files.writeString(csv,"IDENTIFIER,SKU,IDENTIFIERS_PER_SKU\nA,123,2\n");blocked=false;try{DuplicateGroupAnalysis.csv(csv);}catch(java.io.IOException e){blocked=true;}require(blocked,"Incomplete group accepted");}finally{Files.delete(csv);}
        System.out.println("PASS: protected fields, log exclusion, fill-missing, conflicts, input immutability, qualifiers, ambiguity, CSV completeness");
    }
}
