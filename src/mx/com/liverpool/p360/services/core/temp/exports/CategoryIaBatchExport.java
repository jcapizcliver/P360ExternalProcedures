package mx.com.liverpool.p360.services.core.temp.exports;

import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

/** Read-only, resumable Category IA export. Arguments: input.txt outputDir [maxBatches]. */
public final class CategoryIaBatchExport {
  static final int BATCH=100;
  static final String ENTRY="Standard_Template_Category_IA.csv";
  static void log(String s){System.out.println(Instant.now()+" "+s);System.out.flush();}
  static String env(String key){String v=System.getenv(key);if(v==null||v.isBlank())throw new IllegalStateException("Missing "+key);return v;}
  static Connection connect() throws Exception {
    Properties p=new Properties();p.setProperty("user",env("ORACLE_JDBC_USER"));p.setProperty("password",env("ORACLE_JDBC_PASSWORD"));
    p.setProperty("oracle.net.CONNECT_TIMEOUT","10000");p.setProperty("oracle.jdbc.ReadTimeout","45000");
    Connection c=DriverManager.getConnection(env("ORACLE_JDBC_URL"),p);c.setReadOnly(true);
    return c;
  }
  static void atomic(Path path,String text)throws IOException {Path tmp=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(tmp,text,StandardCharsets.UTF_8);Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
  static String hash(Path p)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));}
  static List<String> input(Path p)throws Exception{
    LinkedHashSet<String> set=new LinkedHashSet<>();int n=0;
    for(String line:Files.readAllLines(p,StandardCharsets.UTF_8)){
      String s=line.replace("\uFEFF","").trim();if(s.isEmpty())continue;
      if(n==0&&s.equalsIgnoreCase("Identifier")){n++;continue;}
      if(s.length()>1000 || s.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid identifier at input line "+(n+1));
      set.add(s);n++;
    }
    log("INPUT unique="+set.size()+" dataRows="+(n-1));return new ArrayList<>(set);
  }
  static long merge(Path out,List<Path> parts)throws Exception{
    Path tmp=out.resolveSibling(out.getFileName()+".tmp");long rows=0;String header=null;
    try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(tmp))){
      z.putNextEntry(new ZipEntry(ENTRY));BufferedWriter w=new BufferedWriter(new OutputStreamWriter(z,StandardCharsets.UTF_8));
      for(Path part:parts)try(ZipFile input=new ZipFile(part.toFile())){
        ZipEntry e=input.getEntry(ENTRY);if(e==null)throw new IOException("Missing CSV in "+part);
        try(BufferedReader r=new BufferedReader(new InputStreamReader(input.getInputStream(e),StandardCharsets.UTF_8))){
          String h=r.readLine();if(header==null){header=h;w.write(h);w.newLine();}else if(!header.equals(h))throw new IOException("Header mismatch");
          String line;while((line=r.readLine())!=null){w.write(line);w.newLine();rows++;}
        }
      }
      w.flush();z.closeEntry();
    }
    Files.move(tmp,out,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);return rows;
  }
  public static void main(String[] args)throws Exception{
    if(args.length<2)throw new IllegalArgumentException("input.txt outputDir [maxBatches]");
    Path input=Paths.get(args[0]).toAbsolutePath(),out=Paths.get(args[1]).toAbsolutePath();Files.createDirectories(out);
    try(FileChannel lock=FileChannel.open(out.resolve("run.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock held=lock.tryLock()){
      if(held==null)throw new IllegalStateException("Already running");
      String fingerprint=hash(input)+" batch="+BATCH+" format=1\n";Path stamp=out.resolve("input.sha256");
      if(Files.exists(stamp)&&!Files.readString(stamp).equals(fingerprint))throw new IllegalStateException("Input changed; use a new output directory");atomic(stamp,fingerprint);
      List<String> ids=input(input);List<Path> parts=new ArrayList<>();int limit=args.length>2?Integer.parseInt(args[2]):Integer.MAX_VALUE,done=0;
      CategoryIaDataReader reader=new CategoryIaDataReader();
      for(int offset=0;offset<ids.size();offset+=BATCH){
        int index=offset/BATCH;Path zip=out.resolve(String.format("batch-%05d.zip",index)),mark=out.resolve(String.format("batch-%05d.done",index));parts.add(zip);
        if(Files.exists(mark)&&Files.exists(zip))continue;
        if(done>=limit){log("LIMIT_REACHED; resume with the same input and output directory");return;}
        if(Files.exists(out.resolve("STOP"))){log("STOP_REQUESTED");return;}
        List<String> chunk=ids.subList(offset,Math.min(ids.size(),offset+BATCH));
        for(int attempt=1;;attempt++){
          long begin=System.nanoTime();log("BATCH_START index="+index+" offset="+offset+" count="+chunk.size()+" attempt="+attempt);
          try(Connection c=connect()){
            CategoryIaDataReader.ExportResult result=reader.export(c,chunk,out,phase->log("PHASE batch="+index+" "+phase));
            if(result.productRows!=chunk.size())throw new IOException("Unexpected row count "+result.productRows+" expected "+chunk.size());
            Files.move(result.zipFile,zip,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            atomic(mark,"rows="+result.productRows+" missing="+result.notFoundRows+"\n");
            log("BATCH_OK index="+index+" rows="+result.productRows+" missing="+result.notFoundRows+" ms="+TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-begin));break;
          }catch(SQLException ex){
            log("BATCH_ERROR index="+index+" sqlCode="+ex.getErrorCode()+" state="+ex.getSQLState()+" message="+ex.getMessage());
            if(attempt>=5||ex instanceof SQLSyntaxErrorException)throw ex;Thread.sleep(15000);
          }
        }
        done++;Thread.sleep(300);
      }
      Path result=out.resolve("Standard_Template_Category_IA_IDS.zip");long rows=merge(result,parts);
      if(rows!=ids.size())throw new IOException("Final count mismatch");
      long missing=0;for(int i=0;i<parts.size();i++){String m=Files.readString(out.resolve(String.format("batch-%05d.done",i)));missing+=Long.parseLong(m.substring(m.indexOf("missing=")+8).trim());}
      atomic(out.resolve("COMPLETED.txt"),"uniqueInput="+ids.size()+"\nrows="+rows+"\nmissing="+missing+"\nfile="+result+"\nfinished="+Instant.now()+"\n");log("COMPLETE rows="+rows+" missing="+missing+" file="+result);
    }
  }
}
