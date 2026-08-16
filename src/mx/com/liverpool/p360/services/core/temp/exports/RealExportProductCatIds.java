package mx.com.liverpool.p360.services.core.temp.exports;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RestClient;

/**
 * Exportador mínimo de CatIDs a ATG.
 *
 * NO envía a Marketplace.
 * NO envía a DWH.
 * NO envía a OMS.
 * NO genera Values/AttributeList de negocio.
 * NO modifica Product2G.
 *
 * Sólo toma los CatIDs actuales de "Sitios Web" y genera ClassificationReference
 * como lo hace RealExportProducts.
 */
public final class RealExportProductCatIds
{
  private static final int MAX_PRODUCTS_PER_BATCH = 1000;

  private static final String URL_ATG =
      PropertiesManager.get( "p360.contingency.out.url_atg" );

  private static final String BASE_URL =
      PropertiesManager.get( "p360.contingency.base_url" );

  private static final Path STAGE_ATG =
      java.nio.file.Paths.get( "/", "u01", "workshop", "stage", "ToATG" );

  private static final RESTWrapper WRAPPER = new RESTWrapper();
  private static final RESTWorkshop RW = WRAPPER.getRw();
  private static final RestClient RC = RW.getRc();

  private RealExportProductCatIds()
  {
  }

  public static ExportRunResult runForProductIdsWithResult( String[] productIds,
                                                             boolean send )
      throws IOException, ParserConfigurationException, TransformerException
  {
    String[] clean = cleanIds( productIds );
    List<String> payloadFiles = new ArrayList<String>();
    List<String> brokerResponses = new ArrayList<String>();
    List<String> withoutCatIds = new ArrayList<String>();
    List<String> readErrors = new ArrayList<String>();

    boolean brokerFailure = false;
    int includedProducts = 0;
    int batchNumber = 1;

    Files.createDirectories( STAGE_ATG );

    for ( int start = 0; start < clean.length; start += MAX_PRODUCTS_PER_BATCH )
    {
      int end = Math.min( clean.length, start + MAX_PRODUCTS_PER_BATCH );
      Document doc = createDocument();
      Element products = directChild( doc.getDocumentElement(), "Products" );
      int inBatch = 0;

      for ( int i = start; i < end; i++ )
      {
        String productId = clean[i];
        Set<String> catIds;

        try
        {
          catIds = loadWebCategoryIds( productId );
        }
        catch ( Exception e )
        {
          readErrors.add( productId + ": " + e.getClass().getSimpleName() + ": " + e.getMessage() );
          continue;
        }

        if ( catIds.isEmpty() )
        {
          withoutCatIds.add( productId );
          continue;
        }

        Element product = doc.createElement( "Product" );
        product.setAttribute( "ID", productId );
        product.setAttribute( "Changed", "true" );

        for ( String catId : catIds )
        {
          Element ref = doc.createElement( "ClassificationReference" );
          ref.setAttribute( "ClassificationID", catId );
          ref.setAttribute( "Type", "WebsiteLink" );
          ref.setAttribute( "Changed", "true" );
          product.appendChild( ref );
        }

        products.appendChild( product );
        includedProducts++;
        inBatch++;
      }

      if ( inBatch == 0 )
      {
        continue;
      }

      String xml = serialize( doc, true );
      String filename = "catids_atg_batch"
          + String.format( "%03d", batchNumber )
          + "_"
          + System.currentTimeMillis()
          + ".xml";

      Path file = STAGE_ATG.resolve( filename );
      Files.writeString( file, xml, StandardCharsets.UTF_8 );
      payloadFiles.add( file.toString() );

      if ( send )
      {
        try
        {
          RestClient client =
              new RestClient( "Content-Type: application/xml", "Accept: application/xml" );
          String response = client.getRequest( "POST", URL_ATG, xml );
          brokerResponses.add( response == null ? "" : response );

          if ( response == null || !response.contains( "Se proceso correctamente" ) )
          {
            brokerFailure = true;
          }
        }
        catch ( IOException e )
        {
          brokerFailure = true;
          brokerResponses.add( "IOException: " + e.getMessage() );
        }
      }

      batchNumber++;
    }

    boolean successful =
        send
        && !payloadFiles.isEmpty()
        && !brokerFailure
        && brokerResponses.size() == payloadFiles.size();

    return new ExportRunResult(
        clean.length,
        includedProducts,
        withoutCatIds,
        readErrors,
        payloadFiles,
        brokerResponses,
        successful );
  }

  private static Set<String> loadWebCategoryIds( String productId ) throws IOException
  {
    String raw = RC.getRequest(
        "GET",
        BASE_URL
            + "/object/Product2G/'"
            + productId
            + "'@'MASTER'"
            + "?entityFilter=Product2GStructureGroupMap,Product2G"
            + "&includeIds=true",
        null );

    JSONObject root = new JSONObject( raw );
    JSONObject data = root.optJSONObject( "_data" );
    LinkedHashSet<String> result = new LinkedHashSet<String>();

    if ( data == null )
    {
      return result;
    }

    JSONArray maps = data.optJSONArray( "structureGroupMap" );
    if ( maps == null )
    {
      return result;
    }

    for ( int i = 0; i < maps.length(); i++ )
    {
      JSONObject map = maps.optJSONObject( i );
      JSONObject qualification =
          map == null ? null : map.optJSONObject( "_qualification" );
      JSONObject structureGroup =
          qualification == null ? null : qualification.optJSONObject( "structureGroup" );
      String externalId =
          structureGroup == null ? null : structureGroup.optString( "_externalId", null );

      if ( externalId == null || !externalId.endsWith( "'Sitios Web'" ) )
      {
        continue;
      }

      String catId =
          externalId.replaceAll( "(^')|(('@'Sitios Web')$)", "" ).trim();

      if ( !catId.isEmpty() )
      {
        result.add( catId );
      }
    }

    return result;
  }

  private static Document createDocument()
      throws ParserConfigurationException
  {
    Document doc = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .newDocument();

    Element root = doc.createElement( "STEP-ProductInformation" );
    root.setAttribute(
        "ExportTime",
        new java.text.SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" )
            .format( new java.util.Date() ) );
    root.setAttribute( "ExportContext", "Context2" );
    root.setAttribute( "ContextID", "Context2" );
    root.setAttribute( "WorkspaceID", "Approved" );
    root.setAttribute( "UseContextLocale", "false" );
    doc.appendChild( root );

    root.appendChild( doc.createElement( "AttributeList" ) );
    root.appendChild( doc.createElement( "Assets" ) );
    root.appendChild( doc.createElement( "Products" ) );
    root.appendChild( doc.createElement( "Classifications" ) );

    return doc;
  }

  private static Element directChild( Element parent, String tagName )
  {
    for ( org.w3c.dom.Node child = parent.getFirstChild();
          child != null;
          child = child.getNextSibling() )
    {
      if ( child instanceof Element
          && tagName.equals( ( (Element) child ).getTagName() ) )
      {
        return (Element) child;
      }
    }
    throw new IllegalStateException( "No se encontró " + tagName );
  }

  private static String serialize( Document doc, boolean indent )
      throws TransformerException
  {
    Transformer transformer =
        TransformerFactory.newInstance().newTransformer();

    transformer.setOutputProperty( OutputKeys.ENCODING, StandardCharsets.UTF_8.name() );
    transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "no" );
    transformer.setOutputProperty( OutputKeys.INDENT, indent ? "yes" : "no" );

    if ( indent )
    {
      try
      {
        transformer.setOutputProperty(
            "{http://xml.apache.org/xslt}indent-amount",
            "2" );
      }
      catch ( IllegalArgumentException ignored )
      {
      }
    }

    java.io.StringWriter out = new java.io.StringWriter();
    transformer.transform( new DOMSource( doc ), new StreamResult( out ) );
    return out.toString();
  }

  private static String[] cleanIds( String[] ids )
  {
    LinkedHashSet<String> clean = new LinkedHashSet<String>();

    if ( ids != null )
    {
      for ( String id : ids )
      {
        if ( id != null && !id.trim().isEmpty() )
        {
          clean.add( id.trim() );
        }
      }
    }

    return clean.toArray( new String[clean.size()] );
  }

  public static final class ExportRunResult
  {
    private final int requestedProducts;
    private final int includedProducts;
    private final List<String> productsWithoutCatIds;
    private final List<String> readErrors;
    private final List<String> payloadFiles;
    private final List<String> brokerResponses;
    private final boolean successful;

    private ExportRunResult( int requestedProducts,
                             int includedProducts,
                             List<String> productsWithoutCatIds,
                             List<String> readErrors,
                             List<String> payloadFiles,
                             List<String> brokerResponses,
                             boolean successful )
    {
      this.requestedProducts = requestedProducts;
      this.includedProducts = includedProducts;
      this.productsWithoutCatIds =
          Collections.unmodifiableList( new ArrayList<String>( productsWithoutCatIds ) );
      this.readErrors =
          Collections.unmodifiableList( new ArrayList<String>( readErrors ) );
      this.payloadFiles =
          Collections.unmodifiableList( new ArrayList<String>( payloadFiles ) );
      this.brokerResponses =
          Collections.unmodifiableList( new ArrayList<String>( brokerResponses ) );
      this.successful = successful;
    }

    public int getRequestedProducts() { return this.requestedProducts; }
    public int getIncludedProducts() { return this.includedProducts; }
    public List<String> getProductsWithoutCatIds() { return this.productsWithoutCatIds; }
    public List<String> getReadErrors() { return this.readErrors; }
    public List<String> getPayloadFiles() { return this.payloadFiles; }
    public List<String> getBrokerResponses() { return this.brokerResponses; }
    public boolean isSuccessful() { return this.successful; }
  }
}
