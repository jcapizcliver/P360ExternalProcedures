package mx.com.liverpool.p360.services.core.sftp.manual;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.sftp.ParseECCErrResponse;

public class ParseECCErrResponseManual {
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		try(ParseECCErrResponse parser = new ParseECCErrResponse()){
			java.io.File[] files = new java.io.File(args[0]).listFiles();
			for(java.io.File f : files)
			try {
				long init = System.currentTimeMillis();
				System.out.println("Processing: " + f.getName());
				parser.processFile(f.toPath(), null);
				System.out.println("Done processing " + f.getName() + ". " + rw.getRw().formatTime(System.currentTimeMillis() - init));
			} catch (ParserConfigurationException | SAXException | IOException e) {
				e.printStackTrace();
			}
		}
	}

}
