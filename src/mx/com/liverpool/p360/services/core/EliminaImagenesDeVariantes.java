package mx.com.liverpool.p360.services.core;

public class EliminaImagenesDeVariantes extends RESTWrapper {

    private static final String[] PRODUCT_FILTERS = {
        "rootCharacteristic('LiverpoolManual'),characteristic('LiverpoolManual_URL')",
        "rootCharacteristic('LiverpoolManual'),characteristic('LiverpoolManual_Name')",
        "rootCharacteristic('LiverpoolManual'),characteristic('LiverpoolManual_Status')",
        "rootCharacteristic('LiverpoolManual'),characteristic('LiverpoolManual')",
        "rootCharacteristic('ProductVideo'),characteristic('ProductVideo_URL')",
        "rootCharacteristic('ProductVideo'),characteristic('ProductVideo_Name')",
        "rootCharacteristic('ProductVideo'),characteristic('ProductVideo_Status')",
        "rootCharacteristic('ProductVideo'),characteristic('ProductVideo')",
        "rootCharacteristic('OwnersManual'),characteristic('OwnersManual_URL')",
        "rootCharacteristic('OwnersManual'),characteristic('OwnersManual_Name')",
        "rootCharacteristic('OwnersManual'),characteristic('OwnersManual_Status')",
        "rootCharacteristic('OwnersManual'),characteristic('OwnersManual')",
        "rootCharacteristic('NOM'),characteristic('NOM_URL')",
        "rootCharacteristic('NOM'),characteristic('NOM_Name')",
        "rootCharacteristic('NOM'),characteristic('NOM_Status')",
        "rootCharacteristic('NOM'),characteristic('NOM')"
    };

    private static final String[] ARTICLE_FILTERS = {
        "rootCharacteristic('ProductImage'),characteristic('ProductImage_URL')",
        "rootCharacteristic('ProductImage'),characteristic('ProductImage_Name')",
        "rootCharacteristic('ProductImage'),characteristic('ProductImage_Status')",
        "rootCharacteristic('ProductImage'),characteristic('ProductImage')",
        "rootCharacteristic('ProductImageDetail'),characteristic('ProductImageDetail_URL')",
        "rootCharacteristic('ProductImageDetail'),characteristic('ProductImageDetail_Name')",
        "rootCharacteristic('ProductImageDetail'),characteristic('ProductImageDetail_Status')",
        "rootCharacteristic('ProductImageDetail'),characteristic('ProductImageDetail')",
        "rootCharacteristic('ProductImageSmosh'),characteristic('ProductImageSmosh_URL')",
        "rootCharacteristic('ProductImageSmosh'),characteristic('ProductImageSmosh_Name')",
        "rootCharacteristic('ProductImageSmosh'),characteristic('ProductImageSmosh_Status')",
        "rootCharacteristic('ProductImageSmosh'),characteristic('ProductImageSmosh')",
        "rootCharacteristic('Illustration'),characteristic('Illustration_URL')",
        "rootCharacteristic('Illustration'),characteristic('Illustration_Name')",
        "rootCharacteristic('Illustration'),characteristic('Illustration_Status')",
        "rootCharacteristic('Illustration'),characteristic('Illustration')"
    };

    private static final String[] ARTICLE_FINAL_FILTERS = {
        "rootCharacteristic('ProductImage2'),characteristic('ProductImage_URL2')",
        "rootCharacteristic('ProductImage2'),characteristic('ProductImage_Name2')",
        "rootCharacteristic('ProductImage2'),characteristic('ProductImage2')",
        "rootCharacteristic('ProductImageDetail2'),characteristic('ProductImageDetail_URL2')",
        "rootCharacteristic('ProductImageDetail2'),characteristic('ProductImageDetail_Name2')",
        "rootCharacteristic('ProductImageDetail2'),characteristic('ProductImageDetail2')",
        "rootCharacteristic('ProductImageSmosh2'),characteristic('ProductImageSmosh_URL2')",
        "rootCharacteristic('ProductImageSmosh2'),characteristic('ProductImageSmosh_Name2')",
        "rootCharacteristic('ProductImageSmosh2'),characteristic('ProductImageSmosh2')",
        "rootCharacteristic('Illustration2'),characteristic('Illustration_URL2')",
        "rootCharacteristic('Illustration2'),characteristic('Illustration_Name2')",
        "rootCharacteristic('Illustration2'),characteristic('Illustration2')"
    };

    public static void main(String[] args) {
        EliminaImagenesDeVariantes s = new EliminaImagenesDeVariantes();
        java.util.concurrent.ConcurrentLinkedQueue<String> a = new java.util.concurrent.ConcurrentLinkedQueue<>();
        try (java.util.stream.Stream<String> stream = java.nio.file.Files.lines(
                java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "migración", "to_delete_media_assets"))) {
            stream.forEach(a::add);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        java.util.ArrayList<String> products = new java.util.ArrayList<>(200);
        int counter = 0;
        for (String productNo : a) {
            products.add(productNo);
            counter++;
            if (products.size() == 200) {
                String productItems = toItems(products);
                s.deleteAssets(s.collectVariants(productItems));
                s.deleteAssetsProduct(productItems);
                products.clear();
                System.out.println(counter + "/" + a.size());
            }
        }
        if (!products.isEmpty()) {
            String productItems = toItems(products);
            s.deleteAssets(s.collectVariants(productItems));
            s.deleteAssetsProduct(productItems);
        }
        System.out.println(counter + "/" + a.size());
    }

    public void deleteAssetsProduct(String items) {
        deleteByFilters("Product2G", "Product2GCharacteristicValue", "Product2GCharacteristicValue.RecordKey", items, PRODUCT_FILTERS);
    }

    public void deleteAssets(String items) {
        deleteByFilters("Article", "ArticleCharacteristicValue", "ArticleCharacteristicValue.RecordKey", items, ARTICLE_FILTERS);
    }

    public void deleteAssets2(String items) {
        deleteByFilters("Article", "ArticleCharacteristicValue", "ArticleCharacteristicValue.RecordKey", items, ARTICLE_FINAL_FILTERS);
    }

    public void deleteAssetsBatched(java.util.Collection<String> externalIds) {
        deleteAssetsBatched(externalIds, ImageTrafficLimiter.getDeleteBatchSize());
    }

    public void deleteAssetsBatched(java.util.Collection<String> externalIds, int batchSize) {
        forEachItemsBatch(externalIds, batchSize, this::deleteAssets);
    }

    public void deleteAssets2Batched(java.util.Collection<String> externalIds) {
        deleteAssets2Batched(externalIds, ImageTrafficLimiter.getDeleteBatchSize());
    }

    public void deleteAssets2Batched(java.util.Collection<String> externalIds, int batchSize) {
        forEachItemsBatch(externalIds, batchSize, this::deleteAssets2);
    }

    public void deleteAssetsProductBatched(java.util.Collection<String> externalIds, int batchSize) {
        forEachItemsBatch(externalIds, batchSize, this::deleteAssetsProduct);
    }

    private void deleteByFilters(String rootEntity, String childEntity, String fields, String items, String[] filters) {
        if (items == null || items.trim().isEmpty()) {
            return;
        }
        java.util.Map<String, String> qp = new java.util.TreeMap<>();
        qp.put("items", items);
        qp.put("fields", fields);
        for (String filter : filters) {
            qp.put("qualificationFilter", filter);
            deleteData("list", rootEntity, childEntity, "byItems", qp, System.out::println);
        }
    }

    private void forEachItemsBatch(java.util.Collection<String> externalIds, int batchSize,
            java.util.function.Consumer<String> consumer) {
        if (externalIds == null || externalIds.isEmpty()) {
            return;
        }
        int safeBatchSize = Math.max(1, batchSize);
        java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
        for (String externalId : externalIds) {
            if (externalId != null && !externalId.trim().isEmpty()) {
                unique.add(externalId.trim());
            }
        }
        java.util.ArrayList<String> batch = new java.util.ArrayList<>(safeBatchSize);
        for (String externalId : unique) {
            batch.add(externalId);
            if (batch.size() == safeBatchSize) {
                consumer.accept(toItems(batch));
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            consumer.accept(toItems(batch));
        }
    }

    public static String toItems(java.util.Collection<String> externalIds) {
        StringBuilder sb = new StringBuilder();
        if (externalIds == null) {
            return "";
        }
        for (String externalId : externalIds) {
            if (externalId == null || externalId.trim().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append('\'').append(externalId.trim().replace("'", "''")).append("'@1");
        }
        return sb.toString();
    }

    public String collectVariants(String products) {
        java.util.LinkedList<String> ids = new java.util.LinkedList<>();
        java.util.Map<String, String> qp = new java.util.TreeMap<>();
        qp.put("fields", "Article.SupplierAID");
        qp.put("products", products);
        qp.put("pageSize", "5000");
        collectData("list", "Article", null, "byProducts", qp,
                row -> ids.addLast(row.getJSONArray("values").getString(0)), System.out::println);
        return toItems(ids);
    }
}
