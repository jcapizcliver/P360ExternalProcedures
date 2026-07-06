package mx.com.liverpool.p360.services.core.temp.exports;

public final class RepublishSelectedProductsExporter
{
  private RepublishSelectedProductsExporter()
  {
  }

  public static void runForProductIds( String[] productIds, boolean send )
      throws Exception
  {
    RealExportProducts.runForProductIds( productIds, send );
    RealExportProducts2Mirakl.runForProductIds( productIds, send );
  }
}