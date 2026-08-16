package mx.com.liverpool.p360.services.core.temp.xml.local;

import java.nio.file.Path;
import java.util.concurrent.Semaphore;

import mx.com.liverpool.dataprofiling.preparison.envioproductos.PruebaEnvioPubSubMediaAssets;
import mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion.AnotherXMLHandlerNeoProcessPipeline;

/** Single entry point intended for ReceiveSTEPFile. */
public final class LoadProductDataPipelineNeo {

    private static final int MAX_CONCURRENT = Math.max(
            1,
            Integer.getInteger("p360.step.max.concurrent", 2));
    private static final Semaphore STEP_PERMITS = new Semaphore(MAX_CONCURRENT, true);

    private LoadProductDataPipelineNeo() {
    }

    public static int processContent(Path path) throws Exception {
        STEP_PERMITS.acquire();
        try {
            // Uses the Path overload from the previous RAM fix: no giant XML String.
            PruebaEnvioPubSubMediaAssets.process(path);
            return AnotherXMLHandlerNeoProcessPipeline.processPath(path);
        } finally {
            STEP_PERMITS.release();
        }
    }
}
