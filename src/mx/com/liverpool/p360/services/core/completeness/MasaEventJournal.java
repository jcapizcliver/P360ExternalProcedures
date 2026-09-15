package mx.com.liverpool.p360.services.core.completeness;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.jms.*;

/** Durable independent receipt before the existing acknowledgement. No new subscriber. */
public final class MasaEventJournal {
 private static final String ROOT=System.getProperty("masa.journal.directory", "");
 public static void capture(Message message)throws Exception {
  if(ROOT.isBlank())return;
  if(!(message instanceof TextMessage text))throw new JMSException("Masa requires a text event; not acknowledged");
  String destination=String.valueOf(message.getJMSDestination());
  String body=text.getText(),id=message.getJMSMessageID();
  if(id==null||body==null)throw new JMSException("Masa missing durable event identity/body");
  String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((destination+"\n"+id+"\n"+body).getBytes(StandardCharsets.UTF_8)));
  Path dir=Path.of(ROOT,hash.substring(0,2));Files.createDirectories(dir);
  Path target=dir.resolve(hash+".event");
  if(Files.exists(target))return;
  Base64.Encoder b64=Base64.getEncoder();
  String record="MASA1\n"+System.currentTimeMillis()+"\n"+message.getJMSTimestamp()+"\n"+b64.encodeToString(destination.getBytes(StandardCharsets.UTF_8))+"\n"+b64.encodeToString(id.getBytes(StandardCharsets.UTF_8))+"\n"+b64.encodeToString(body.getBytes(StandardCharsets.UTF_8))+"\n";
  Path temp=Files.createTempFile(dir,"receipt-",".partial");
  try {
   try(FileChannel ch=FileChannel.open(temp,StandardOpenOption.WRITE)){ByteBuffer bytes=ByteBuffer.wrap(record.getBytes(StandardCharsets.UTF_8));while(bytes.hasRemaining())ch.write(bytes);ch.force(true);}
   Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);
   try(FileChannel ch=FileChannel.open(dir,StandardOpenOption.READ)){ch.force(true);}
  }finally{Files.deleteIfExists(temp);}
 }
}
