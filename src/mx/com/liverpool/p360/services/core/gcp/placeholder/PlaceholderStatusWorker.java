package mx.com.liverpool.p360.services.core.gcp.placeholder;

/**
 * Manual entry point for the scheduled placeholder status process.
 */
public class PlaceholderStatusWorker {

    public static void main(String[] args) throws Exception {
        String sourceFilePath = args.length > 0 ? args[0] : null;
        PlaceholderStatusProcessResult result = new PlaceholderStatusJobRunner().run(sourceFilePath);
        System.out.println(result.toJson());
    }
}
