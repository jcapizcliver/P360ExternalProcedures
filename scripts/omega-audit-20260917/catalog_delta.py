"""Independent, durable LookupValue projection; never blocks product readiness."""
import json,sqlite3,pathlib,time,subprocess,fcntl,os
BASE=pathlib.Path('/u01/workshop/java/operations/panadero-fidough')
ROOT=BASE/'exploit/catalog-delta';ROOT.mkdir(exist_ok=True)
HOME=pathlib.Path(__file__).resolve().parent
JAVA='/u01/workshop/java/jdk/jdk-17.0.12/bin/java'
CP=str(HOME)+':/u01/workshop/java/releases/masa-exploit-20260913/classes:/u01/workshop/java/bin:/u01/workshop/java/lib/*'
def state(**data):
 data['at']=time.time();tmp=ROOT/'status.tmp';tmp.write_text(json.dumps(data));os.replace(str(tmp),str(ROOT/'status.json'))
def main():
 lock=(ROOT/'lock').open('a');fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
 db=sqlite3.connect(str(ROOT/'ledger.sqlite'),timeout=60)
 db.executescript('CREATE TABLE IF NOT EXISTS meta(k TEXT PRIMARY KEY,v INTEGER); CREATE TABLE IF NOT EXISTS debt(seq INTEGER PRIMARY KEY,request TEXT,error TEXT,at REAL);')
 src=sqlite3.connect('file:'+str(BASE/'ledger.sqlite')+'?mode=ro',uri=True,timeout=60)
 child=None;applied=0
 def send(req):
  nonlocal child
  if child is None or child.poll() is not None:
   child=subprocess.Popen([JAVA,'-Xmx512m','-cp',CP,'CatalogDelta',str(HOME/'plan-prod.json')],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=open(ROOT/'java-error.log','a'),universal_newlines=True,bufsize=1)
  child.stdin.write(json.dumps(req)+'\n');child.stdin.flush()
  while True:
   line=child.stdout.readline()
   if not line:raise RuntimeError('Catalog worker exited')
   if line.startswith('CATALOG_RESULT '):
    result=json.loads(line[15:])
    if result['seq']!=req['seq']:raise RuntimeError('Receipt mismatch')
    if not result.get('ok'):raise RuntimeError(result.get('error'))
    return result
 while True:
  try:
   row=db.execute("select v from meta where k='seq'").fetchone();pos=row[0] if row else 0
   high=src.execute('select max(seq) from event').fetchone()[0] or 0;end=min(pos+200000,high)
   requests={};unsupported=0
   for seq,payload in src.execute("select seq,payload from event where seq>? and seq<=? order by seq",(pos,end)):
    try:
     obj=json.loads(payload);change=obj.get('entityItemChange') or obj.get('entityItemsDeleted') or {};entity=change.get('_entity')
     if entity not in ('LookupValue','Lookup','Characteristic','StandardizationDictionary','StandardizationValue','Structure','StructureGroup','StructureAttribute'):
      continue # Product/article and other entities belong to their own consumers.
     ident=str(change.get('_entityItem',{}).get('_internalId','')).split('@')[0]
     rev=change.get('_revision',{}).get('_internalId','1')
     req=dict(seq=seq,entity=entity,id=int(ident),revision=int(rev));requests[(entity,req['id'],req['revision'])]=req
    except Exception as e:db.execute('insert or replace into debt values(?,?,?,?)',(seq,payload,'UNRESOLVED_EVENT '+str(e),time.time()))
   for req in requests.values():
    try:send(req);applied+=1
    except Exception as e:db.execute('insert or replace into debt values(?,?,?,?)',(req['seq'],json.dumps(req),str(e),time.time()))
   db.execute("insert or replace into meta values('seq',?)",(end,));db.commit()
   # Retry previously failed writes independently; unsupported records retain their raw payload.
   for seq,raw in db.execute("select seq,request from debt where error not like 'CATALOG_ENTITY%' and error not like 'UNRESOLVED_EVENT%' order by seq limit 20").fetchall():
    try:send(json.loads(raw));db.execute('delete from debt where seq=?',(seq,));applied+=1
    except Exception:pass
   db.commit();debt=db.execute('select count(*) from debt').fetchone()[0]
   state(state='CAUGHT_UP' if end==high else 'CATCHING_UP',scannedThrough=end,receivedThrough=high,appliedThisRun=applied,debt=debt)
   if end==high:time.sleep(2)
  except Exception as e:state(state='RETRY',error=str(e));time.sleep(10)
if __name__=='__main__':main()
