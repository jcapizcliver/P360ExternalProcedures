package mx.com.liverpool.p360.services.core.amqp.run;

import java.sql.*;
import java.util.*;
import org.json.*;
import mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager;

/** Read-only, bounded, model-first snapshots. Never runs on the JMS receiver. */
final class PacFieldSnapshot {
    static final String LIVE="timestamp '9999-12-31 00:00:00.0'";
    static final Set<String> FIELDS=Set.of("SKU","MainBarCode","MainBarCodeS4H","Direction","Section","Business","ItemGroup","ItemGroupS4H","ProductTypeSAP","ProductTypeSAPTEMP","ProductTypeSAPTEMPSBB","BrandName","BRAND_ID_S4H","ColoursLiverpoolAtt","TamanoUnico","SupplierPartNumber","Template");
    static final class Row {
        long revision; int entity; String id; boolean nativeEan; Set<String> parents=new LinkedHashSet<>();
        Map<String,String> values=new LinkedHashMap<>(); Map<String,Long> lookup=new LinkedHashMap<>();
    }
    static String marks(int count){return String.join(",",Collections.nCopies(count,"?"));}
    static PreparedStatement statement(Connection c,String sql,Collection<?> args)throws SQLException{
        if(Boolean.getBoolean("p360.pac.sql.trace"))System.out.println("SQL_BEGIN "+System.currentTimeMillis()+" "+sql.substring(0,Math.min(sql.length(),240))+" binds="+args.size());
        PreparedStatement p=c.prepareStatement(sql);p.setQueryTimeout(45);p.setFetchSize(900);int i=0;
        for(Object a:args){if(a instanceof String)p.setNString(++i,(String)a);else p.setObject(++i,a);}return p;
    }
    static void put(Row row,String key,String value){if(value!=null&&!value.isBlank())row.values.put(key,value);}
    Map<String,Row> read(Collection<String> ids)throws Exception{
        if(ids.isEmpty())return Map.of();if(ids.size()>900)throw new IllegalArgumentException("Snapshot exceeds 900");
        Set<String> names=new LinkedHashSet<>();for(String id:ids)names.add(id.replaceFirst("^(1000|1100):",""));
        boolean qualified=ids.stream().allMatch(id->id.matches("^(1000|1100):.+"));
        if(Boolean.getBoolean("p360.pac.sql.trace"))System.out.println("READ_START "+System.currentTimeMillis());
        try(Connection c=new QuickJdbcConnectionManager().openConnection(true)){
            if(Boolean.getBoolean("p360.pac.sql.trace"))System.out.println("READ_CONNECTED "+System.currentTimeMillis());
            Map<String,Row> out=new LinkedHashMap<>();Map<Long,Row> byRevision=new LinkedHashMap<>();
            String sql="select /*+ index(a \"XAK2_ArticleRevision\") */ a.\"ID\",a.\"Identifier\",a.\"EntityID\" from \"ArticleRevision\" a where a.\"Identifier\" in ("+marks(names.size())+") and a.\"EntityID\" in (1000,1100) and a.\"RevisionID\"=1 and a.\"DeletionTimestamp\"="+LIVE;

            try(PreparedStatement p=statement(c,sql,names);ResultSet r=p.executeQuery()){
                while(r.next()){
                    Row row=new Row();row.revision=r.getLong(1);row.id=r.getString(2);row.entity=r.getInt(3);
                    String key=row.entity+":"+row.id;if(qualified&&!ids.contains(key))continue;
                    if(out.putIfAbsent(key,row)!=null)throw new SQLException("Ambiguous live snapshot within entity: "+key);
                    byRevision.put(row.revision,row);
                }
            }
            if(out.isEmpty())return out;
            if(out.size()>900)throw new SQLException("Use entity-qualified identifiers for a snapshot exceeding 900 revisions");
            List<Long> revs=new ArrayList<>(byRevision.keySet());Set<Long> seen=new HashSet<>();
            sql="select /*+ index(d \"XAK1_ArticleDetail\") */ d.\"ArticleRevisionID\",d.\"Res_Int_02\",d.\"EAN\",d.\"Res_Int_01\" from \"ArticleDetail\" d where d.\"ArticleRevisionID\" in ("+marks(revs.size())+") and d.\"DeletionTimestamp\"="+LIVE;
            try(PreparedStatement p=statement(c,sql,revs);ResultSet r=p.executeQuery()){
                while(r.next()){Row row=byRevision.get(r.getLong(1));if(!seen.add(row.revision))throw new SQLException("Multiple live details "+row.id);
                    put(row,"SKU",r.getString(2));put(row,"MainBarCode",r.getString(3));row.nativeEan=row.values.containsKey("MainBarCode");
                    long business=r.getLong(4);if(!r.wasNull()&&row.entity==1100)row.lookup.put("Business",business);
                }
            }
            // Separate point reads avoid optimizer join-order changes on larger IN lists.
            sql="select /*+ index(x \"XAK1_ArticleDomain\") */ x.\"ArticleRevisionID\",x.\"EntityID\",x.\"Res_Text250_01\",x.\"Res_Int_01\",x.\"Res_Int_02\",x.\"Res_Int_03\",x.\"Res_Int_04\",x.\"Res_Int_05\",x.\"Res_Int_06\" from \"ArticleDomain\" x where x.\"ArticleRevisionID\" in ("+marks(revs.size())+") and x.\"EntityID\" in (21006,21106) and x.\"DeletionTimestamp\"="+LIVE;
            seen.clear();
            try(PreparedStatement p=statement(c,sql,revs);ResultSet r=p.executeQuery()){
                while(r.next()){Row row=byRevision.get(r.getLong(1));if(r.getInt(2)!=(row.entity==1100?21006:21106))continue;
                    if(!seen.add(row.revision))throw new SQLException("Multiple native domains "+row.id);
                    put(row,"SupplierPartNumber",r.getString(3));
                    String[] keys=row.entity==1100?new String[]{"Direction","Section","ItemGroup","ItemGroupS4H","BrandName","BRAND_ID_S4H"}:new String[]{"TamanoUnico","ColoursLiverpoolAtt"};
                    for(int i=0;i<keys.length;i++){long id=r.getLong(4+i);if(!r.wasNull())row.lookup.put(keys[i],id);}
                }
            }
            Set<Long> lookupIds=new LinkedHashSet<>();for(Row row:out.values())lookupIds.addAll(row.lookup.values());
            Map<Long,String[]> labels=lookups(c,lookupIds);
            for(Row row:out.values())for(var e:row.lookup.entrySet()){
                String[] v=labels.get(e.getValue());if(v==null)throw new SQLException("Missing live lookup "+e.getValue());
                put(row,e.getKey(),v[1].isBlank()?v[0]:v[1]);
                if(e.getKey().equals("Business"))put(row,"Business",switch(v[0]){case "LVP","LIV"->"Liverpool";case "SBB"->"Suburbia";case "MKP"->"Marketplace";default->v[1];});
            }
            sql="select /*+ leading(s) index(s \"XAK1_ArticleStructureMap\") */ s.\"ArticleRevisionID\",s.\"StructureGroupIdentifier\" from \"ArticleStructureMap\" s where s.\"ArticleRevisionID\" in ("+marks(revs.size())+") and s.\"StructureID\"=(select t.\"StructureID\" from PIM_MAIN.\"StructureRevision\" t where t.\"Identifier\"=N'PrimaryProductTaxonomy' and t.\"RevisionID\"=1 and t.\"DeletionTimestamp\"="+LIVE+") and s.\"DeletionTimestamp\"="+LIVE;
            try(PreparedStatement p=statement(c,sql,revs);ResultSet r=p.executeQuery()){while(r.next()){Row row=byRevision.get(r.getLong(1));if(row.entity==1100){String value=r.getString(2);if(row.values.containsKey("Template")&&!row.values.get("Template").equals(value))throw new SQLException("Multiple templates "+row.id);put(row,"Template",value);}}}
            sql="select /*+ index(x \"XIE3_ArticleReference\") */ x.\"ArticleRevisionID\",x.\"RefExtArtIdentifier\" from \"ArticleReference\" x where x.\"ArticleRevisionID\" in ("+marks(revs.size())+") and x.\"RefEntityID\"=1100 and x.\"DeletionTimestamp\"="+LIVE;
            try(PreparedStatement p=statement(c,sql,revs);ResultSet r=p.executeQuery()){while(r.next()){String parent=r.getString(2);if(parent!=null&&!parent.isBlank())byRevision.get(r.getLong(1)).parents.add(parent);}}
            // Only requested characteristics; model fields above always win.
            List<String> chars=new ArrayList<>(FIELDS);chars.remove("Template");
            sql="select /*+ leading(v a) use_nl(a) index(v IX_ACV_TUNE_02) index(a \"XAK1_CharacteristicRevision\") */ v.\"ArticleRevisionID\",a.\"Identifier\",v.\"Value\",v.\"LookupValueID\" from \"ArticleCharactValue\" v join PIM_MAIN.\"CharacteristicRevision\" a on a.\"CharacteristicID\"=v.\"CharacteristicID\" and a.\"RevisionID\"=1 and a.\"DeletionTimestamp\"="+LIVE+" where v.\"ArticleRevisionID\" in ("+marks(revs.size())+") and a.\"Identifier\" in ("+marks(chars.size())+") and v.\"DeletionTimestamp\"="+LIVE;
            List<Object> args=new ArrayList<>(revs);args.addAll(chars);List<Object[]> fallback=new ArrayList<>();lookupIds.clear();
            try(PreparedStatement p=statement(c,sql,args);ResultSet r=p.executeQuery()){
                while(r.next()){Row row=byRevision.get(r.getLong(1));String key=r.getString(2);if(row.values.containsKey(key))continue;
                    String value=r.getString(3);long lookup=r.getLong(4);if(lookup!=0)lookupIds.add(lookup);fallback.add(new Object[]{row,key,value,lookup});}
            }
            labels=lookups(c,lookupIds);
            for(Object[] f:fallback){Row row=(Row)f[0];String key=(String)f[1],value=(String)f[2];long id=(Long)f[3];
                if((value==null||value.isBlank())&&id!=0){String[] v=labels.get(id);if(v!=null)value=v[1].isBlank()?v[0]:v[1];}
                if(value!=null&&!value.isBlank()){String previous=row.values.putIfAbsent(key,value);if(previous!=null&&!previous.equals(value))throw new SQLException("Ambiguous characteristic "+row.id+" "+key);}
            }
            for(Row row:out.values())if(!row.nativeEan&&row.values.containsKey("MainBarCodeS4H")&&(row.values.getOrDefault("Business","").equals("Suburbia")||!row.values.containsKey("MainBarCode")))row.values.put("MainBarCode",row.values.get("MainBarCodeS4H"));
            return out;
        }
    }
    static Map<Long,String[]> lookups(Connection c,Set<Long> ids)throws SQLException{
        Map<Long,String[]> out=new HashMap<>();List<Long> all=new ArrayList<>(ids);
        for(int start=0;start<all.size();start+=900){List<Long> part=all.subList(start,Math.min(all.size(),start+900));
            String sql="select /*+ leading(v) use_nl(l) index(v \"XAK1_LookupValueRevision\") index(l \"XAK1_LookupValueLang\") */ v.\"LookupValueID\",v.\"Code\",l.\"Name\" from PIM_MAIN.\"LookupValueRevision\" v left join PIM_MAIN.\"LookupValueLang\" l on l.\"LookupValueRevisionID\"=v.\"ID\" and l.\"LanguageID\"=10 and l.\"DeletionTimestamp\"="+LIVE+" where v.\"LookupValueID\" in ("+marks(part.size())+") and v.\"RevisionID\"=1 and v.\"DeletionTimestamp\"="+LIVE;
            try(PreparedStatement p=statement(c,sql,part);ResultSet r=p.executeQuery()){while(r.next())out.put(r.getLong(1),new String[]{Objects.toString(r.getString(2),""),Objects.toString(r.getString(3),"")});}
        }return out;
    }
}
