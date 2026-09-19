package mx.com.liverpool.p360.services.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

/** Exact membership with an 8 MiB bucket directory; strings remain on disk. */
public final class DiskStringSet extends AbstractSet<String> implements AutoCloseable {
 private final long[] heads=new long[1<<20];
 private final Path path;private final RandomAccessFile data;private int count;private boolean closed;private long end=8;
 public DiskStringSet(Path directory)throws IOException {Files.createDirectories(directory);path=Files.createTempFile(directory,"masa_values_",".tmp");data=new RandomAccessFile(path.toFile(),"rw");data.writeLong(0);}
 private static long hash(byte[] bytes){long h=0xcbf29ce484222325L;for(byte b:bytes){h^=(b&255);h*=0x100000001b3L;}return h;}
 private int bucket(long hash){return (int)(hash^(hash>>>32))&(heads.length-1);}
 private boolean has(byte[] bytes,long hash)throws IOException {
  long offset=heads[bucket(hash)];
  byte[] header=new byte[20];while(offset!=0){data.seek(offset);data.readFully(header);ByteBuffer h=ByteBuffer.wrap(header);long next=h.getLong(),saved=h.getLong();int length=h.getInt();if(saved==hash&&length==bytes.length){byte[] actual=new byte[length];data.readFully(actual);if(Arrays.equals(bytes,actual))return true;}offset=next;}return false;
 }
 @Override public synchronized boolean add(String value){
  if(closed)throw new IllegalStateException("Closed selection");Objects.requireNonNull(value);
  byte[] bytes=value.getBytes(StandardCharsets.UTF_8);if(bytes.length>1000)throw new IllegalArgumentException("Un VALUE excede 1000 bytes; revise la separación de líneas.");long hash=hash(bytes);
  try{if(has(bytes,hash))return false;int bucket=bucket(hash);long offset=end;byte[] record=ByteBuffer.allocate(20+bytes.length).putLong(heads[bucket]).putLong(hash).putInt(bytes.length).put(bytes).array();data.seek(offset);data.write(record);end+=record.length;heads[bucket]=offset;count++;return true;}catch(IOException e){throw new UncheckedIOException(e);}
 }
 @Override public synchronized boolean contains(Object value){if(!(value instanceof String))return false;byte[] bytes=((String)value).getBytes(StandardCharsets.UTF_8);try{return has(bytes,hash(bytes));}catch(IOException e){throw new UncheckedIOException(e);}}
 @Override public int size(){return count;}
 @Override public Iterator<String> iterator(){return new Iterator<String>(){long offset=8;int seen;public boolean hasNext(){return seen<count;}public String next(){synchronized(DiskStringSet.this){if(!hasNext())throw new NoSuchElementException();try{data.seek(offset+16);int length=data.readInt();byte[] bytes=new byte[length];data.readFully(bytes);offset+=20L+length;seen++;return new String(bytes,StandardCharsets.UTF_8);}catch(IOException e){throw new UncheckedIOException(e);}}}};}
 @Override public synchronized void close()throws IOException{if(!closed){closed=true;data.close();Files.deleteIfExists(path);}}
}
