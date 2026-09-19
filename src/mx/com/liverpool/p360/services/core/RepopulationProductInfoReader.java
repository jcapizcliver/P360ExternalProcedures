package mx.com.liverpool.p360.services.core;

import java.sql.*;
import java.util.*;

/** Read-only, bounded projection for Repopulate. Missing/ambiguous rows fail closed. */
public final class RepopulationProductInfoReader {
    private RepopulationProductInfoReader() {}
    private static final int BATCH = 900;
    private static final String ACTIVE = "timestamp '9999-12-31 00:00:00.0'";
    private static final String[] KEYS = {"status", "prevStatus", "firstDateApproved", "sku", "businessCode", "template", "remarksEs", "remarksEn"};

    public static Map<String, Map<String,String>> read(Connection con, Collection<String> identifiers) throws SQLException {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (identifiers != null) for (String id : identifiers) {
            if (id == null || id.trim().isEmpty()) throw new SQLException("Empty product identifier");
            unique.add(id.trim());
        }
        List<String> ids = new ArrayList<>(unique);
        Map<String, Map<String,String>> result = new LinkedHashMap<>();
        for (int start=0; start<ids.size(); start+=BATCH) {
            checkInterrupted();
            List<String> part=ids.subList(start, Math.min(ids.size(), start+BATCH));
            String sql = """
                select /*+ qb_name(repopulate_info) leading(ar ad es en sm bv)
                    use_nl(ad es en sm bv) index(ar XAK2_ArticleRevision)
                    index(ad XAK1_ArticleDetail) index(es XAK1_ArticleLang)
                    index(en XAK1_ArticleLang) index(sm XAK2_ArticleStructureMap) */
                    ar."Identifier", ad."CurrentStatus", ad."Res_Int_03",
                    ad."Res_DateTime_02",
                    ad."Res_Int_02", bv."Code", sm."StructureGroupIdentifier",
                    es."Remarks", en."Remarks"
                from "ArticleRevision" ar
                join "ArticleDetail" ad on ad."ArticleRevisionID"=ar."ID" and ad."DeletionTimestamp"=%s
                left join "ArticleLang" es on es."ArticleRevisionID"=ar."ID" and es."LanguageID"=10
                    and es."ChannelID"=1 and es."EntityID"=1105 and es."DeletionTimestamp"=%s
                left join "ArticleLang" en on en."ArticleRevisionID"=ar."ID" and en."LanguageID"=9
                    and en."ChannelID"=1 and en."EntityID"=1105 and en."DeletionTimestamp"=%s
                left join "ArticleStructureMap" sm on sm."ArticleRevisionID"=ar."ID"
                    and sm."StructureID"=(select sr."StructureID" from PIM_MAIN."StructureRevision" sr
                    where sr."Identifier"=N'PrimaryProductTaxonomy' and sr."RevisionID"=1 and sr."DeletionTimestamp"=%s)
                    and sm."DeletionTimestamp"=%s
                left join PIM_MAIN."LookupValueRevision" bv on bv."LookupValueID"=ad."Res_Int_01"
                    and bv."RevisionID"=1 and bv."DeletionTimestamp"=%s
                where ar."RevisionID"=1 and ar."EntityID"=1100 and ar."DeletionTimestamp"=%s
                    and ar."Identifier" in (%s)
                """.formatted(ACTIVE,ACTIVE,ACTIVE,ACTIVE,ACTIVE,ACTIVE,ACTIVE,
                    String.join(",", Collections.nCopies(part.size(), "?")));
            try (PreparedStatement ps=con.prepareStatement(sql)) {
                ps.setQueryTimeout(30);
                ps.setFetchSize(Math.min(part.size(), 250));
                for (int i=0;i<part.size();i++) ps.setNString(i+1, part.get(i));
                try(ResultSet rs=ps.executeQuery()) {
                    while(rs.next()) {
                        checkInterrupted();
                        String id=rs.getString(1);
                        if (!unique.contains(id)) throw new SQLException("Unexpected product returned");
                        if(result.containsKey(id)) throw new SQLException("Ambiguous active ProductInfo for " + id);
                        Map<String,String> row=new LinkedHashMap<>();
                        for(int i=0;i<KEYS.length;i++) {
                            String value=rs.getString(i+2);
                            if(i==2) { java.sql.Timestamp ts=rs.getTimestamp(i+2); value=ts==null?"":new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSSZ").format(ts); }
                            row.put(KEYS[i],value==null?"":value);
                        }
                        result.put(id,row);
                    }
                }
            }
        }
        return result;
    }
    private static void checkInterrupted() throws SQLException {
        if(Thread.currentThread().isInterrupted()) throw new SQLException("ProductInfo read cancelled");
    }
}
