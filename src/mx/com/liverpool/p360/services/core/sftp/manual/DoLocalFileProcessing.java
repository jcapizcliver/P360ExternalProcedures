package mx.com.liverpool.p360.services.core.sftp.manual;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.sftp.ParseECC122Response;

public class DoLocalFileProcessing {

	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
//		ParseECCAttributesFile parse = new ParseECCAttributesFile();
//		parse.processFile(java.nio.file.Paths.get(args[0]), null);
			try(ParseECC122Response p = new ParseECC122Response()){
				p.processFile(  
						java.nio.file.Paths.get(
								args[0]
	//							  "C:"
	//							, "opt"
	//							, "LVP"
	//							, "desorden"
	//							, "PROD"
	//							, "samples"
	//							, "GenericXMLproducts20260205185144.XML"
	//							, "GenericXMLproducts20251020131958.XML"
							), null, null);
			} catch (ServiceUnavailableException | ParserConfigurationException | SAXException | IOException e) {
				e.printStackTrace();
			}
	}
	
	
	
	
}
