package com.jcapiz.memelos.misc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class RestClient {

	public RestClient() {

	}

	public RestClient(String... headers) {
		String[] headerPair = null;
		for(String header : headers) {
			headerPair = header.split(":");
			this.header.put(headerPair[0], headerPair[1].trim());
		}
	}

	private java.util.Map<String, String> header = new java.util.HashMap<>();

	public String getRequest( String method, String url, String payload ) throws Exception{
		return getRequest(method, url, payload, header);
	}

	public java.util.Map<String, String> getHeader(){
		return header;
	}

	  public String getRequest( String method, String url, String payload,
	                            java.util.Map< String, String > header ) throws Exception
	  {
	    // Disable SSL certificate validation
	    disableSSLValidation();

	    URL obj = new URI(url).toURL();
//	    System.out.println(url);
	    HttpURLConnection con = ( HttpURLConnection ) obj.openConnection();
	    con.setRequestMethod( method );
	    for ( java.util.Map.Entry< String, String > entry : header.entrySet() )
	    {
	      con.setRequestProperty( entry.getKey(), entry.getValue() );
	    }

	    if(payload != null && !"".equals( payload )) {
	      con.setDoOutput( true );
	      try(java.io.PrintWriter pw = new java.io.PrintWriter( new java.io.OutputStreamWriter( con.getOutputStream(), java.nio.charset.Charset.forName( "UTF-8" ) ) )){
	        pw.println(payload);
	      }
	    }
	    int responseCode = con.getResponseCode();
//	    System.out.println("____" + responseCode + "_____");
	    if ( responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED )
	    { // success
	      try (BufferedReader in = new BufferedReader( new InputStreamReader( con.getInputStream(),
	                                                                          java.nio.charset.Charset.forName( "UTF-8" ) ) ))
	      {
	        String inputLine;
	        StringBuffer response = new StringBuffer();

	        while ( ( inputLine = in.readLine() ) != null )
	        {
	          response.append( inputLine );
	        }
	        in.close();

	        // print result
	        return response.toString();
	      }
	    }
	    String inputLine;
	    StringBuffer response = new StringBuffer();
	    try (BufferedReader in = new BufferedReader(
	                                                 new InputStreamReader(
	                                                                        con.getErrorStream(),
	                                                                        java.nio.charset.Charset
	                                                                        .forName( "UTF-8" ) ) ))
	    {

	      while ( ( inputLine = in.readLine() ) != null )
	      {
	        response.append( inputLine );
	      }
	      in.close();
	    }catch(NullPointerException e) {
	      try (BufferedReader in = new BufferedReader( new InputStreamReader( con.getInputStream(),
	                                                                          java.nio.charset.Charset.forName( "UTF-8" ) ) ))
	      {
	        response = new StringBuffer();

	        while ( ( inputLine = in.readLine() ) != null )
	        {
	          response.append( inputLine );
	        }
	        in.close();

	        // print result
	        return response.toString();
	      }catch(java.io.IOException ex) {
	        return "Resource not found, -->" + ex.getMessage();
	      }
	    }

	    return response.toString();
	  }

	  private void disableSSLValidation() throws Exception
	  {
	    TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager()
	    {
	      @Override
	      public java.security.cert.X509Certificate[] getAcceptedIssuers()
	      {
	        return null;
	      }

	      @Override
	      public void checkClientTrusted( X509Certificate[] certs, String authType )
	      {
	      }

	      @Override
	      public void checkServerTrusted( X509Certificate[] certs, String authType )
	      {
	      }
	    } };

	    SSLContext sc = SSLContext.getInstance( "TLS" );
	    sc.init( null, trustAllCerts, new SecureRandom() );
	    HttpsURLConnection.setDefaultSSLSocketFactory( sc.getSocketFactory() );

	    // Create all-trusting host name verifier
	    HostnameVerifier allHostsValid = new HostnameVerifier()
	    {
	      @Override
	      public boolean verify( String hostname, SSLSession session )
	      {
	        return true;
	      }
	    };

	    // Install the all-trusting host verifier
	    HttpsURLConnection.setDefaultHostnameVerifier( allHostsValid );
	  }
}
