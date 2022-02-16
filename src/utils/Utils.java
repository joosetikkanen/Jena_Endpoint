package utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URISyntaxException;

import org.apache.any23.Any23;
import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.http.HTTPClient;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.HTTPDocumentSource;
import org.apache.any23.writer.RDFXMLWriter;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TripleHandlerException;



public class Utils {

    public static Reader getRequest(String url) throws IOException {

        Any23 runner = new Any23();
        runner.setHTTPUserAgent("test-user-agent");
        HTTPClient httpClient = runner.getHTTPClient();
        DocumentSource source = null;
        try {
            source = new HTTPDocumentSource(httpClient, url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        TripleHandler handler = new RDFXMLWriter(output);
        try {
            runner.extract(source, handler);
        } catch (ExtractionException e) {
            e.printStackTrace();
        } finally {
            try {
                handler.close();
            } catch (TripleHandlerException e) {
                e.printStackTrace();
            }
        }
        return new StringReader(output.toString());
        
        
    }

}
