package mx.com.liverpool.p360.services.core.temp.xml.local.neostream;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX reader shared by the STEP processors.
 *
 * It intentionally keeps at most one root Product tree (root + children) alive.
 * Once the closing tag of a root Product is found, the Product is delivered to
 * the consumer and becomes eligible for GC after the consumer returns.
 */
public final class StepXmlStreamingParser {

    private StepXmlStreamingParser() {
    }

    public static final class Value {
        private final String attributeId;
        private final String id;
        private final String unitId;
        private final StringBuilder text = new StringBuilder();

        Value(String attributeId, String id, String unitId) {
            this.attributeId = attributeId;
            this.id = id;
            this.unitId = unitId;
        }

        public String getAttributeId() { return attributeId; }
        public String getId() { return id; }
        public String getUnitId() { return unitId; }
        public String getText() { return text.length() == 0 ? "" : text.toString(); }
        void append(char[] ch, int start, int length) { text.append(ch, start, length); }

        public String idOrText() {
            return id != null && !id.isEmpty() ? id : getText();
        }
    }

    public static final class MultiValue {
        private final String attributeId;
        private final LinkedList<Value> values = new LinkedList<>();

        MultiValue(String attributeId) {
            this.attributeId = attributeId;
        }

        public String getAttributeId() { return attributeId; }
        public LinkedList<Value> getValues() { return values; }
        void add(Value value) { if (value != null) values.add(value); }
    }

    public static final class Classification {
        private final String id;
        private final String type;

        Classification(String id, String type) {
            this.id = id;
            this.type = type;
        }

        public String getId() { return id; }
        public String getType() { return type; }
    }

    public static final class Product {
        private final String id;
        private final String parentId;
        private final String userTypeId;
        private final StringBuilder name = new StringBuilder();
        private LinkedList<Value> values;
        private LinkedList<MultiValue> multiValues;
        private final LinkedList<Product> products = new LinkedList<>();
        private final LinkedList<Classification> classifications = new LinkedList<>();
        private Value workingValue;
        private MultiValue workingMultiValue;
        private Map<String, Value> valueMap;
        private boolean valuesOpen;

        Product(String id, String parentId, String userTypeId) {
            this.id = id;
            this.parentId = parentId;
            this.userTypeId = userTypeId;
        }

        public String getId() { return id; }
        public String getParentId() { return parentId; }
        public String getUserTypeId() { return userTypeId == null ? "" : userTypeId; }
        public String getName() { return name.length() == 0 ? "" : name.toString(); }
        public LinkedList<Value> getValues() { return values == null ? new LinkedList<>() : values; }
        public LinkedList<MultiValue> getMultiValues() { return multiValues == null ? new LinkedList<>() : multiValues; }
        public LinkedList<Product> getProducts() { return products; }
        public LinkedList<Classification> getClassifications() { return classifications; }

        public Map<String, Value> getValueMap() {
            if (valueMap == null) {
                Map<String, Value> map = new LinkedHashMap<>();
                if (values != null) {
                    for (Value value : values) {
                        if (value != null && value.getAttributeId() != null) {
                            map.put(value.getAttributeId(), value);
                        }
                    }
                }
                valueMap = map;
            }
            return valueMap;
        }

        void createLists() {
            if (values == null) values = new LinkedList<>();
            if (multiValues == null) multiValues = new LinkedList<>();
        }

        void openValues() {
            createLists();
            valuesOpen = true;
        }

        void closeValues() {
            flushMultiValue();
            valuesOpen = false;
        }

        boolean isValuesOpen() {
            return valuesOpen;
        }

        void prepareValue(Value value) {
            flushValue();
            workingValue = value;
        }

        void prepareMultiValue(MultiValue multiValue) {
            flushMultiValue();
            workingMultiValue = multiValue;
        }

        void flushValue() {
            if (workingValue == null) return;
            if (workingMultiValue != null) {
                workingMultiValue.add(workingValue);
            } else {
                createLists();
                values.add(workingValue);
                valueMap = null;
            }
            workingValue = null;
        }

        void flushMultiValue() {
            if (workingMultiValue == null) return;
            flushValue();
            createLists();
            multiValues.add(workingMultiValue);
            workingMultiValue = null;
        }

        void addProduct(Product product) { products.add(product); }
        void addClassification(Classification classification) { classifications.add(classification); }
        void appendName(char[] ch, int start, int length) { name.append(ch, start, length); }
        Value workingValue() { return workingValue; }
    }

    public static final class StepIndex {
        private final Set<String> productIds = new LinkedHashSet<>();
        private final Set<String> productSkus = new LinkedHashSet<>();
        private final Set<String> articleIds = new LinkedHashSet<>();
        private final Set<String> articleSkus = new LinkedHashSet<>();
        private final Set<String> productAttributeIds = new LinkedHashSet<>();
        private final Set<String> articleAttributeIds = new LinkedHashSet<>();
        private final Map<String, String> productSkuById = new LinkedHashMap<>();
        private final Map<String, String> articleSkuById = new LinkedHashMap<>();
        private final Set<String> primaryStructureGroupIds = new LinkedHashSet<>();
        private final Set<String> webStructureGroupIds = new LinkedHashSet<>();
        private final Set<String> eccStructureGroupIds = new LinkedHashSet<>();
        private final Set<String> s4hStructureGroupIds = new LinkedHashSet<>();

        public Set<String> getProductIds() { return Collections.unmodifiableSet(productIds); }
        public Set<String> getProductSkus() { return Collections.unmodifiableSet(productSkus); }
        public Set<String> getArticleIds() { return Collections.unmodifiableSet(articleIds); }
        public Set<String> getArticleSkus() { return Collections.unmodifiableSet(articleSkus); }
        public Set<String> getProductAttributeIds() { return Collections.unmodifiableSet(productAttributeIds); }
        public Set<String> getArticleAttributeIds() { return Collections.unmodifiableSet(articleAttributeIds); }
        public Map<String, String> getProductSkuById() { return Collections.unmodifiableMap(productSkuById); }
        public Map<String, String> getArticleSkuById() { return Collections.unmodifiableMap(articleSkuById); }
        public Set<String> getPrimaryStructureGroupIds() { return Collections.unmodifiableSet(primaryStructureGroupIds); }
        public Set<String> getWebStructureGroupIds() { return Collections.unmodifiableSet(webStructureGroupIds); }
        public Set<String> getEccStructureGroupIds() { return Collections.unmodifiableSet(eccStructureGroupIds); }
        public Set<String> getS4hStructureGroupIds() { return Collections.unmodifiableSet(s4hStructureGroupIds); }
    }

    public static StepIndex index(Path path)
            throws ParserConfigurationException, SAXException, IOException {
        StepIndex index = new StepIndex();
        SAXParser parser = newSafeParser();
        parser.parse(path.toFile(), new IndexHandler(index));
        return index;
    }

    public static int parse(Path path, Consumer<Product> rootConsumer)
            throws ParserConfigurationException, SAXException, IOException {
        SAXParser parser = newSafeParser();
        ProductHandler handler = new ProductHandler(rootConsumer);
        parser.parse(path.toFile(), handler);
        return handler.productsCounter;
    }

    private static SAXParser newSafeParser()
            throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {
        }
        return factory.newSAXParser();
    }

    private static final class ProductHandler extends DefaultHandler {
        private final LinkedList<Product> stack = new LinkedList<>();
        private final Consumer<Product> rootConsumer;
        private boolean gettingName;
        private int productsCounter;

        ProductHandler(Consumer<Product> rootConsumer) {
            this.rootConsumer = rootConsumer;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String tag = tag(localName, qName);
            if ("Product".equals(tag)) {
                String parentId = attributes.getValue("ParentID");
                if (parentId == null && !stack.isEmpty()) parentId = stack.getLast().getId();
                stack.addLast(new Product(
                        attributes.getValue("ID"),
                        parentId,
                        attributes.getValue("UserTypeID")));
                return;
            }
            if (stack.isEmpty()) return;

            Product product = stack.getLast();
            if ("Values".equals(tag)) {
                product.openValues();
            } else if ("Value".equals(tag) && product.isValuesOpen()) {
                product.prepareValue(new Value(
                        attributes.getValue("AttributeID"),
                        attributes.getValue("ID"),
                        attributes.getValue("UnitID")));
            } else if ("MultiValue".equals(tag) && product.isValuesOpen()) {
                product.prepareMultiValue(new MultiValue(attributes.getValue("AttributeID")));
            } else if ("ClassificationReference".equals(tag)) {
                product.addClassification(new Classification(
                        attributes.getValue("ClassificationID"),
                        attributes.getValue("Type")));
            } else if ("Name".equals(tag)) {
                gettingName = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (stack.isEmpty()) return;
            Product product = stack.getLast();
            Value value = product.workingValue();
            if (value != null) value.append(ch, start, length);
            if (gettingName) product.appendName(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String tag = tag(localName, qName);
            if (stack.isEmpty()) return;
            Product product = stack.getLast();

            if ("Value".equals(tag) && product.workingValue() != null) {
                product.flushValue();
            } else if ("MultiValue".equals(tag) && product.isValuesOpen()) {
                product.flushMultiValue();
            } else if ("Values".equals(tag)) {
                product.closeValues();
            } else if ("Name".equals(tag)) {
                gettingName = false;
            } else if ("Product".equals(tag)) {
                product.flushMultiValue();
                stack.removeLast();
                productsCounter++;
                if (stack.isEmpty()) {
                    if (rootConsumer != null) rootConsumer.accept(product);
                } else {
                    stack.getLast().addProduct(product);
                }
            }
        }
    }

    private static final class IndexHandler extends DefaultHandler {
        private static final class IndexProduct {
            final String id;
            final String parentId;
            final String userTypeId;
            final boolean xmlRoot;
            final boolean productEntity;
            final Set<String> attributeIds = new LinkedHashSet<>();
            String currentAttribute;
            StringBuilder currentText;
            String sku;
            int childCount;
            boolean valuesOpen;

            IndexProduct(
                    String id,
                    String parentId,
                    String userTypeId,
                    boolean xmlRoot) {
                this.id = id;
                this.parentId = parentId;
                this.userTypeId = userTypeId == null ? "" : userTypeId;
                this.xmlRoot = xmlRoot;
                this.productEntity = xmlRoot && !isNumericProductParent(parentId);
            }

            boolean alsoArticleEntity() {
                return !productEntity
                        || (xmlRoot
                            && childCount == 0
                            && !userTypeId.startsWith("SalesItemFamily"));
            }
        }

        private final StepIndex index;
        private final LinkedList<IndexProduct> stack = new LinkedList<>();

        IndexHandler(StepIndex index) { this.index = index; }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String tag = tag(localName, qName);
            if ("Product".equals(tag)) {
                String parentId = attributes.getValue("ParentID");
                if (parentId == null && !stack.isEmpty()) parentId = stack.getLast().id;
                stack.addLast(new IndexProduct(
                        attributes.getValue("ID"),
                        parentId,
                        attributes.getValue("UserTypeID"),
                        stack.isEmpty()));
                return;
            }
            if (stack.isEmpty()) return;
            IndexProduct current = stack.getLast();
            if ("Values".equals(tag)) {
                current.valuesOpen = true;
            } else if ("Value".equals(tag) && current.valuesOpen) {
                current.currentAttribute = attributes.getValue("AttributeID");
                current.currentText = new StringBuilder();
                if (current.currentAttribute != null) {
                    current.attributeIds.add(current.currentAttribute);
                }
            } else if ("MultiValue".equals(tag) && current.valuesOpen) {
                String attributeId = attributes.getValue("AttributeID");
                if (attributeId != null) current.attributeIds.add(attributeId);
            } else if ("ClassificationReference".equals(tag) && current.productEntity) {
                String id = attributes.getValue("ClassificationID");
                String type = attributes.getValue("Type");
                if (id != null && type != null) {
                    if ("WebsiteLink".equals(type)) index.webStructureGroupIds.add(id);
                    else if ("GALink".equals(type)) index.eccStructureGroupIds.add(id);
                    else if ("GALink_S4H".equals(type)) index.s4hStructureGroupIds.add(id);
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (stack.isEmpty()) return;
            IndexProduct product = stack.getLast();
            if (product.currentText != null) product.currentText.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String tag = tag(localName, qName);
            if (stack.isEmpty()) return;
            IndexProduct product = stack.getLast();
            if ("Value".equals(tag) && product.valuesOpen) {
                if ("SKU".equals(product.currentAttribute) && product.currentText != null) {
                    product.sku = product.currentText.toString().trim();
                }
                product.currentAttribute = null;
                product.currentText = null;
            } else if ("Values".equals(tag)) {
                product.valuesOpen = false;
                product.currentAttribute = null;
                product.currentText = null;
            } else if ("Product".equals(tag)) {
                stack.removeLast();

                if (product.productEntity) {
                    if (product.id != null && !product.id.isBlank()) {
                        index.productIds.add(product.id);
                        if (product.sku != null && !product.sku.isBlank()) {
                            index.productSkuById.put(product.id, product.sku);
                        }
                    }
                    index.productAttributeIds.addAll(product.attributeIds);
                    if (product.parentId != null && !product.parentId.isBlank()) {
                        index.primaryStructureGroupIds.add(product.parentId);
                    }
                    if (product.sku != null && !product.sku.isBlank()) {
                        index.productSkus.add(product.sku);
                    }
                }

                if (product.alsoArticleEntity()) {
                    if (product.id != null && !product.id.isBlank()) {
                        index.articleIds.add(product.id);
                        if (product.sku != null && !product.sku.isBlank()) {
                            index.articleSkuById.put(product.id, product.sku);
                        }
                    }
                    index.articleAttributeIds.addAll(product.attributeIds);
                    if (product.sku != null && !product.sku.isBlank()) {
                        index.articleSkus.add(product.sku);
                    }
                }

                if (!stack.isEmpty()) {
                    stack.getLast().childCount++;
                }
            }
        }
    }

    private static boolean isNumericProductParent(String parentId) {
        return parentId != null && parentId.matches("^(S?[0-9]+)$");
    }

    private static String tag(String localName, String qName) {
        return localName != null && !localName.isEmpty() ? localName : qName;
    }
}
