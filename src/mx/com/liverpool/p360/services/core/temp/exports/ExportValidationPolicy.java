package mx.com.liverpool.p360.services.core.temp.exports;
import java.util.*;
/** Immutable validation settings captured once per exporter invocation. */
public final class ExportValidationPolicy {
 private final boolean strict;
 private final Set<String> identifiers;
 public ExportValidationPolicy(String mode,String selected) {
  String normalized=mode==null?"minimum":mode.trim().toLowerCase(Locale.ROOT);
  if(!Set.of("minimum","strict").contains(normalized)) throw new IllegalArgumentException("Unknown ecomm.validation.mode: "+normalized);
  strict="strict".equals(normalized);
  Set<String> ids=new HashSet<>();
  if(selected!=null) for(String id:selected.split("[,\\s]+")) if(!id.isBlank()) ids.add(id.trim());
  identifiers=Collections.unmodifiableSet(ids);
 }
 public boolean isStrict(String id){return strict&&(identifiers.isEmpty()||identifiers.contains(id));}
 public static List<String> merge(List<String> minimum,List<String> diagnostics){
  Set<String> result=new LinkedHashSet<>(minimum);result.addAll(diagnostics);return new ArrayList<>(result);
 }
}
