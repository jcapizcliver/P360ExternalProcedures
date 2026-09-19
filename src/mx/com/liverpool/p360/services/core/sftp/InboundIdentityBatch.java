package mx.com.liverpool.p360.services.core.sftp;

import mx.com.liverpool.p360.services.core.InboundSkuResolver;
import mx.com.liverpool.p360.services.core.SkuCoordinationLock;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.json.*;

/** Keep ambiguous records in the original durable job while allowing unrelated records to finish. */
public final class InboundIdentityBatch {
    public interface Check {void check(Map<String,String> values,String proposal);}
    public final byte[] xml;
    public final List<String> readyKeys;
    public final JSONArray pending;
    private Path manual;
    private final Set<String> prior=new HashSet<>();
    private InboundIdentityBatch(byte[] xml,List<String> keys,JSONArray pending){this.xml=xml;this.readyKeys=keys;this.pending=pending;}
    private static boolean product(Node n){return n instanceof Element&&"Product".equals(n.getLocalName()==null?n.getNodeName():n.getLocalName());}
    private static void collect(Node n,List<Element> products){for(Node c=n.getFirstChild();c!=null;c=c.getNextSibling())collect(c,products);if(product(n))products.add((Element)n);}
    private static boolean owns(Element owner,Node n){for(Node p=n.getParentNode();p!=null;p=p.getParentNode())if(product(p))return p==owner;return false;}
    private static void stripChildren(Node node){for(Node c=node.getFirstChild();c!=null;){Node next=c.getNextSibling();if(product(c))node.removeChild(c);else stripChildren(c);c=next;}}
    private static byte[] serialize(Node node)throws Exception {
        TransformerFactory f=TransformerFactory.newInstance();f.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD,"");f.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");
        Transformer t=f.newTransformer();t.setOutputProperty(OutputKeys.ENCODING,"UTF-8");ByteArrayOutputStream out=new ByteArrayOutputStream();t.transform(new DOMSource(node),new StreamResult(out));return out.toByteArray();
    }
    public static InboundIdentityBatch partition(byte[] original,Check check,Predicate<String> confirmed)throws Exception {
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);f.setFeature("http://xml.org/sax/features/external-general-entities",false);f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        Document source=f.newDocumentBuilder().parse(new ByteArrayInputStream(original));Document target=f.newDocumentBuilder().newDocument();Element root=target.createElement("Products");target.appendChild(root);
        List<Element> records=new ArrayList<>();collect(source,records);if(records.isEmpty())throw new IOException("IDENTITY_INPUT_WITHOUT_PRODUCTS");List<String> keys=new ArrayList<>();JSONArray pending=new JSONArray();
        for(int index=0;index<records.size();index++){
            Element record=records.get(index);String key="identity-record-v1:"+index+":"+DurableSftpQueue.sha(serialize(record));if(confirmed.test(key))continue;
            Map<String,String> values=new HashMap<>();NodeList nodes=record.getElementsByTagNameNS("*","Value");for(int n=0;n<nodes.getLength();n++){Element value=(Element)nodes.item(n);if(owns(record,value))values.put(value.getAttribute("AttributeID"),value.getTextContent().trim());}
            try {check.check(values,record.getAttribute("ZNPRST"));}
            catch(IllegalStateException e){String reason=e.getMessage();if(reason==null||!(reason.startsWith("SKU_IDENTITY_AMBIGUOUS")||reason.startsWith("SKU_ALIAS_")||reason.startsWith("SKU_TARGET_HAS_OTHER_SKU")||reason.startsWith("IDENTITY_RECORD_")))throw e;pending.put(new JSONObject().put("record",index).put("sku",values.getOrDefault("MATNR","")).put("type",values.getOrDefault("ATTYP","")).put("reason",reason));continue;}
            Node copy=target.importNode(record,true);stripChildren(copy);root.appendChild(copy);keys.add(key);
        }
        return new InboundIdentityBatch(serialize(target),keys,pending);
    }
    public static InboundIdentityBatch prepare(byte[] original,InboundSkuResolver guard,String fallback,Consumer<String> log)throws Exception {
        DurableSftpQueue.Job job=DurableSftpQueue.ACTIVE.get();Path manual=job==null?DurableSftpQueue.ROOT.resolve("manual-identity").resolve(DurableSftpQueue.sha(original)):null;Set<String> done=new HashSet<>();
        if(manual!=null){Files.createDirectories(manual);Path receipts=manual.resolve("receipts.json");if(Files.exists(receipts)){JSONArray a=new JSONArray(Files.readString(receipts));for(int i=0;i<a.length();i++)done.add(a.getString(i));}Path input=manual.resolve("input.xml");if(!Files.exists(input))Files.write(input,original);}
        InboundIdentityBatch batch=partition(original,(v,proposal)->{
            String sku=SkuCoordinationLock.normalize(v.get("MATNR"));if(sku.isEmpty())throw new IllegalStateException("IDENTITY_RECORD_MISSING_SKU");String type=v.get("ATTYP"),id="SBB".equals(fallback)?v.get("PRODUCT_ID"):proposal==null||proposal.isBlank()?v.get("ZNPRST"):proposal;
            if(id==null||id.isBlank())id=fallback+sku;if("SBB".equals(fallback)&&id.matches("[0-9]{15}"))id="1"+id;
            if("01".equals(type))guard.resolve(1100,sku,id);else if("02".equals(type))guard.resolve(1000,sku,id);else if("00".equals(type)||"10".equals(type)){guard.validateIndividual(sku,id);guard.redirect(1100,id);guard.redirect(1000,id);}else throw new IllegalStateException("IDENTITY_RECORD_UNKNOWN_ATTYP "+type);
            String parent=SkuCoordinationLock.normalize(v.get("SATNR"));if(!parent.isEmpty()&&!parent.equals("0"))guard.existing(1100,parent);
        },key->job==null?done.contains(key):DurableSftpQueue.confirmed(key));
        batch.manual=manual;batch.prior.addAll(done);Path debt=(job==null?manual:job.dir).resolve("identity-debt.json");DurableSftpQueue.atomic(debt,new JSONObject().put("ready",batch.readyKeys.size()).put("pending",batch.pending).put("at",java.time.Instant.now().toString()).toString());
        log.accept("SKU_IDENTITY_PARTITION ready="+batch.readyKeys.size()+" pending="+batch.pending.length()+" debt="+debt);return batch;
    }
    public void completedWrites()throws IOException {
        if(manual==null)for(String key:readyKeys)DurableSftpQueue.confirm(key);else{prior.addAll(readyKeys);DurableSftpQueue.atomic(manual.resolve("receipts.json"),new JSONArray(prior).toString());}
        if(pending.length()>0)throw new IOException("IDENTITY_RECORDS_PENDING="+pending.length()+"; completed records will not be repeated");
    }
}
