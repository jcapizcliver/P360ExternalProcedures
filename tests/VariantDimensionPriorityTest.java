package mx.com.liverpool.p360.services.core;
import org.json.*;
public final class VariantDimensionPriorityTest {
 static int n;
 static void check(boolean b){n++;if(!b)throw new AssertionError("check "+n);}
 public static void main(String[] args){
  JSONObject r=new JSONObject("{TamanoUnico:'Legacy size',ColoursLiverpoolAtt:'Legacy color',SupplierPartNumber:'unchanged'}");
  JSONObject d=new JSONObject("{articleExtraData:[{_qualification:{targetMarket:{_code:'US'}},tamanoUnico:{_label:'Wrong market'}},{_qualification:{targetMarket:{_code:'MX'}},tamanoUnico:{_label:'Medium'},coloursLiverpoolAtt:{_label:'Blue'}}]}");
  GetProposals.applyVariantDimensionPriority(r,d);
  check(r.getString("TamanoUnico").equals("Medium"));check(r.getString("ColoursLiverpoolAtt").equals("Blue"));check(r.getString("SupplierPartNumber").equals("unchanged"));
  JSONObject fallback=new JSONObject("{TamanoUnico:'Characteristic size',ColoursLiverpoolAtt:'Characteristic color'}");
  d=new JSONObject("{articleExtraData:[{_qualification:{targetMarket:{_code:'MX'}},tamanoUnico:{_label:'  ',_code:' '},coloursLiverpoolAtt:null}]}");
  GetProposals.applyVariantDimensionPriority(fallback,d);check(fallback.getString("TamanoUnico").equals("Characteristic size"));check(fallback.getString("ColoursLiverpoolAtt").equals("Characteristic color"));
  GetProposals.applyVariantDimensionPriority(fallback,new JSONObject());check(fallback.getString("TamanoUnico").equals("Characteristic size"));
  JSONObject blank=new JSONObject();GetProposals.applyVariantDimensionPriority(blank,d);check(blank.getString("TamanoUnico").isEmpty());check(blank.getString("ColoursLiverpoolAtt").isEmpty());
  d=new JSONObject("{articleExtraData:[{_qualification:{targetMarket:{_key:'MX'}},tamanoUnico:{_label:null,_code:'M'},coloursLiverpoolAtt:' Navy '}]}");
  GetProposals.applyVariantDimensionPriority(fallback,d);check(fallback.getString("TamanoUnico").equals("M"));check(fallback.getString("ColoursLiverpoolAtt").equals("Navy"));
  check(GetProposals.variantExtraDataValue(new JSONObject("{articleExtraData:[null,{}]}"),"tamanoUnico").isEmpty());
  System.out.println("PASS "+n+" variant dimension checks; no network calls");
 }
}
