package com.example.ei.forfun.logic;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class AutForoWriteBulkPoster
{
  private static final String DEFAULT_ENDPOINT =
      "http://172.18.251.7:8080/process-engine/public/rt/AutForoWrite";

  private final HttpClient httpClient;
  private final URI endpointUri;

  public AutForoWriteBulkPoster()
  {
    this( DEFAULT_ENDPOINT );
  }

  public AutForoWriteBulkPoster( String endpoint )
  {
    this.endpointUri = URI.create( Objects.requireNonNull( endpoint, "endpoint must not be null" ) );
    this.httpClient = HttpClient.newBuilder()
                                .connectTimeout( Duration.ofSeconds( 20 ) )
                                .build();
  }

  public static void main( String[] args ) throws Exception
  {
    if ( args.length < 1 || args.length > 3 )
    {
      System.err.println(
          "Uso:\n"
          + "  java com.example.ei.forfun.logic.AutForoWriteBulkPoster <archivo.csv> [continueOnError:true|false] [logFile]\n\n"
          + "Ejemplo:\n"
          + "  java -cp bin:\"lib/*\" com.example.ei.forfun.logic.AutForoWriteBulkPoster /ruta/filtered_result3.csv true /tmp/autforo_write.log"
      );
      System.exit( 1 );
    }

    Path csvFile = Path.of( args[0] );
    boolean continueOnError = args.length >= 2 ? Boolean.parseBoolean( args[1] ) : true;
    Path logFile = args.length >= 3 ? Path.of( args[2] ) : null;

    AutForoWriteBulkPoster runner = new AutForoWriteBulkPoster();
    runner.processFile( csvFile, continueOnError, logFile );
  }

  public void processFile( Path csvFile, boolean continueOnError, Path logFile ) throws Exception
  {
    Objects.requireNonNull( csvFile, "csvFile must not be null" );

    if ( !Files.exists( csvFile ) )
    {
      throw new IOException( "No existe el archivo: " + csvFile );
    }

    try (BufferedWriter logWriter = logFile != null
        ? Files.newBufferedWriter( logFile, StandardCharsets.UTF_8 )
        : null)
    {
      AtomicInteger rawRowCount = new AtomicInteger( 0 );
      AtomicInteger sentOk = new AtomicInteger( 0 );
      AtomicInteger sentError = new AtomicInteger( 0 );
      AtomicInteger skipped = new AtomicInteger( 0 );
      AtomicInteger stopFlag = new AtomicInteger( 0 );

      SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
          '"',
          ',',
          '\\',
          "\n",
          StandardCharsets.UTF_8,
          values -> {
            int rowNumber = rawRowCount.incrementAndGet();

            if ( stopFlag.get() == 1 )
            {
              return;
            }

            try
            {
              if ( values == null || values.length == 0 )
              {
                skipped.incrementAndGet();
                log( logWriter, "Fila " + rowNumber + ": vacía, se omite." );
                return;
              }

              String payload = safeTrim( values[0] );
              if ( payload == null || payload.isEmpty() )
              {
                skipped.incrementAndGet();
                log( logWriter, "Fila " + rowNumber + ": primera columna vacía, se omite." );
                return;
              }

              HttpResponse<String> response = sendJsonPost( payload );
              int status = response.statusCode();
              String responseBody = response.body();

              if ( status >= 200 && status < 300 )
              {
                sentOk.incrementAndGet();
                log( logWriter,
                     "Fila " + rowNumber + ": OK HTTP " + status
                     + " | response=" + abbreviate( responseBody, 1000 ) );
              }
              else
              {
                sentError.incrementAndGet();
                log( logWriter,
                     "Fila " + rowNumber + ": ERROR HTTP " + status
                     + " | response=" + abbreviate( responseBody, 1000 ) );

                if ( !continueOnError )
                {
                  stopFlag.set( 1 );
                  throw new RuntimeException(
                      "Se detuvo en fila " + rowNumber + " por HTTP " + status
                          + ". Response: " + abbreviate( responseBody, 1000 ) );
                }
              }
            }
            catch ( Exception e )
            {
              sentError.incrementAndGet();
              logQuietly( logWriter,
                          "Fila " + rowNumber + ": EXCEPCIÓN " + e.getClass().getName()
                              + " - " + e.getMessage() );

              if ( !continueOnError )
              {
                stopFlag.set( 1 );
                throw new RuntimeException( e );
              }
            }
          }
      );

      try
      {
        parser.parse( csvFile );
      }
      catch ( RuntimeException e )
      {
        if ( !continueOnError )
        {
          throw unwrapIfNeeded( e );
        }
        throw e;
      }

      log( logWriter,
           "Resumen -> filas leídas: " + rawRowCount.get()
               + ", enviadas OK: " + sentOk.get()
               + ", con error: " + sentError.get()
               + ", omitidas: " + skipped.get() );
    }
  }

  private HttpResponse<String> sendJsonPost( String jsonBody ) throws IOException, InterruptedException
  {
    HttpRequest request = HttpRequest.newBuilder()
                                     .uri( this.endpointUri )
                                     .timeout( Duration.ofMinutes( 2 ) )
                                     .header( "Content-Type", "application/json; charset=UTF-8" )
                                     .header( "Accept", "application/json, text/plain, */*" )
                                     .POST( HttpRequest.BodyPublishers.ofString( jsonBody, StandardCharsets.UTF_8 ) )
                                     .build();

    return this.httpClient.send( request, HttpResponse.BodyHandlers.ofString( StandardCharsets.UTF_8 ) );
  }

  private static Exception unwrapIfNeeded( RuntimeException e ) throws Exception
  {
    Throwable cause = e.getCause();
    if ( cause instanceof Exception )
    {
      return (Exception) cause;
    }
    return e;
  }

  private static String safeTrim( String value )
  {
    return value == null ? null : value.trim();
  }

  private static String abbreviate( String value, int maxLength )
  {
    if ( value == null )
    {
      return null;
    }
    if ( value.length() <= maxLength )
    {
      return value;
    }
    return value.substring( 0, maxLength ) + "...";
  }

  private static void log( BufferedWriter writer, String message ) throws IOException
  {
    String line = "[" + LocalDateTime.now() + "] " + message;
    System.out.println( line );

    if ( writer != null )
    {
      writer.write( line );
      writer.newLine();
      writer.flush();
    }
  }

  private static void logQuietly( BufferedWriter writer, String message )
  {
    try
    {
      log( writer, message );
    }
    catch ( IOException ioe )
    {
      System.out.println( "[" + LocalDateTime.now() + "] " + message );
      System.out.println( "[" + LocalDateTime.now() + "] No se pudo escribir al log: " + ioe.getMessage() );
    }
  }
}