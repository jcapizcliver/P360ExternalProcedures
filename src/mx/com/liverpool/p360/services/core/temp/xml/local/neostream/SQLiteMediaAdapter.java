package mx.com.liverpool.p360.services.core.temp.xml.local.neostream;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import org.json.*;
import mx.com.liverpool.p360.services.core.xml.*;
import mx.com.liverpool.dataprofiling.preparison.envioproductos.PruebaEnvioPubSubMediaAssets;

/** Only adapts source models; the existing media class still owns the JSON mapping. */
public final class SQLiteMediaAdapter {
    static ProductFileValueElement value(String attr, JSONObject v) {
        ProductFileValueElement x=new ProductFileValueElement(attr,v.optString("id",null),v.optString("unit",null));
        x.setText(v.optString("text",""));return x;
    }
    static ProductFileProductElement product(JSONObject j) {
        JSONObject s=j.getJSONObject("source");
        ProductFileProductElement p=new ProductFileProductElement(s.getString("id"),s.optString("parentId",""),s.getString("objectType"));
        p.createList();p.createMultiValueList();
        for(Object item:NeoSQLite.items(j.getJSONArray("attributes"))) {
            JSONObject a=(JSONObject)item;String id=a.getString("attributeId");
            if(a.optBoolean("multiValue"))p.prepareMultiValue(new ProductFileMultiValueElement(id));
            for(Object v:NeoSQLite.items(a.getJSONArray("values"))){p.prepareValue(value(id,(JSONObject)v));p.addValue();}
            if(a.optBoolean("multiValue"))p.addMultiValue();
        }
        for(Object item:NeoSQLite.items(j.optJSONArray("references"))){
            JSONObject r=(JSONObject)item;
            if("ASSET".equals(r.optString("kind"))&&r.has("targetId")&&r.has("type"))p.putAssetCrossReference(r.getString("targetId"),r.getString("type"));
            if("CLASSIFICATION".equals(r.optString("kind"))&&r.has("targetId")&&r.has("type")){
                p.prepareClassification(new ProductFileClassificationElement(r.getString("targetId"),r.getString("type")));p.addClassification();
            }
        }
        return p;
    }
    static JSONObject build(Connection src,String root,Map<String,JSONObject> source,Consumer<JSONObject> debt)throws Exception {
        Map<String,ProductFileProductElement> models=new LinkedHashMap<>();
        Map<String,ProductFileAssetElement> assets=new HashMap<>();
        for(JSONObject j:source.values()){ProductFileProductElement p=product(j);models.put(p.getId(),p);}
        for(ProductFileProductElement p:models.values()){
            if(!root.equals(p.getId())&&models.containsKey(p.getParentId()))models.get(p.getParentId()).addProduct(p);
            for(String id:p.getAssetCrossReferences().keySet()){
                if(assets.containsKey(id))continue;
                try(PreparedStatement q=src.prepareStatement("SELECT e.payload_json FROM migration_root r JOIN stage_entity e ON e.run_id=r.run_id AND e.kind='asset' AND e.source_id=? WHERE r.root_id=?")){
                    q.setString(1,id);q.setString(2,root);
                    try(ResultSet rs=q.executeQuery()){
                        if(!rs.next()){debt.accept(new JSONObject().put("kind","MISSING_ASSET_SNAPSHOT").put("objectId",p.getId()).put("assetId",id));continue;}
                        JSONObject j=new JSONObject(rs.getString(1));JSONObject s=j.getJSONObject("source");
                        ProductFileAssetElement a=new ProductFileAssetElement(id,s.optString("objectType",""));
                        for(Object x:NeoSQLite.items(j.optJSONArray("attributes"))){JSONObject attr=(JSONObject)x;
                            for(Object v:NeoSQLite.items(attr.getJSONArray("values"))){a.setCurrentValue(value(attr.getString("attributeId"),(JSONObject)v));a.addValue();}}
                        assets.put(id,a);
                    }
                }
            }
        }
        return PruebaEnvioPubSubMediaAssets.buildPayloadFromModels(List.of(models.get(root)),assets);
    }
}
