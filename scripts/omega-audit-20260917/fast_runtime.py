"""Persistent projection workers; independent, durable recent-event cursor."""
import concurrent.futures,json,pathlib,subprocess,threading,time
import event_labels
import event_delta,os,hashlib
WORKERS=int(os.environ.get("OMEGA_EVENT_WORKERS","4"))
WINDOW=int(os.environ.get("OMEGA_EVENT_WINDOW","100000"))

class Worker:
 def __init__(self,owner,name):
  self.owner=owner;self.name=name;self.process=None;self.lock=threading.Lock()
 def apply(self,job,request,scope='FULL'):
  m=self.owner
  if not request.stat().st_size:return
  with self.lock:
   if self.process is None or self.process.poll() is not None:
    self.process=subprocess.Popen([m.JAVA,'-Xmx3g','-Doracle.jdbc.ReadTimeout=900000','-cp',m.CP,('OmegaEventDelta' if self.name.startswith('delta-') else 'ExploitLiveProjection'),'--server'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,universal_newlines=True,bufsize=1)
   token=str(job)
   self.process.stdin.write(json.dumps(dict(request=token,directory=str(job),input=str(request),scope=scope))+'\n');self.process.stdin.flush()
   with pathlib.Path(str(job)+'.log').open('a') as log:
    for line in self.process.stdout:
     log.write(line);log.flush()
     if line.startswith('PROJECTION_RESULT '):
      result=json.loads(line[len('PROJECTION_RESULT '):])
      if result.get('request')!=token:raise RuntimeError('Worker response mismatch')
      if not result.get('ok'):raise RuntimeError(result.get('error','Projection failed'))
      return
   raise RuntimeError('Persistent worker exited '+str(self.process.poll()))

def scope_for(change):
 fields=change.get('_changedField') or []
 if change.get('_changeType')!='CHANGED' or not fields:return 'FULL'
 if all(isinstance(f,str) and f.split('.')[0] in ('ArticleCharacteristicValue','ArticleCharacteristicValueLang','Product2GCharacteristicValue','Product2GCharacteristicValueLang') for f in fields):return 'CHAR'
 if all(isinstance(f,str) and f.split('.')[0] in ('ArticleLang','Product2GLang') for f in fields):return 'TEXT'
 return 'FULL'

def merge_scope(a,b):return a if a==b else 'FULL'

def recent_plan(m,pos):
 job=m.OP/'event-delta-v1'/str(pos);job.mkdir(parents=True,exist_ok=True);manifest=job/'manifest.json'
 if manifest.exists():return job,json.loads(manifest.read_text())
 with m.old.db() as c:events=c.execute('SELECT seq,destination,payload FROM event WHERE seq>? ORDER BY seq LIMIT ?',(pos,WINDOW)).fetchall()
 if not events:return job,None
 owners={};catalog=0;counts={};last=events[-1][0]
 for row in events:
  parsed=event_delta.parse(row[2])
  if parsed is None:
   found,cat,_=m.affected_projection([row]);catalog+=cat
   for identifier in found:
    # Deletion/identity-only messages are explicitly reconciled; never silently ignored.
    for entity in (1000,1100):
     key=(entity,identifier);item=owners.setdefault(key,dict(entity=entity,identifier=identifier,patches={},fallback=set()))
     item['fallback'].add('FULL')
   continue
  key=(parsed['entity'],parsed['identifier']);item=owners.setdefault(key,dict(entity=parsed['entity'],identifier=parsed['identifier'],patches={},fallback=set()))
  item['fallback'].update(parsed['fallback'])
  for patch in parsed['patches']:
   patch['at']=parsed['at'];patch['userId']=parsed.get('userId')
   if not patch['at']:item['fallback'].add('CHAR');continue
   k=tuple(patch.get(n) for n in ('name','language','record','parent'))
   old=item['patches'].get(k)
   if old is None or old['at']<=patch['at']:item['patches'][k]=patch
 parts=[[] for _ in range(WORKERS)]
 for (_,identifier),item in sorted(owners.items()):
  item['patches']=list(item['patches'].values());item['fallback']=sorted(item['fallback'])
  # All changes of one identifier stay on one worker, even across windows.
  slot=int(hashlib.sha256(identifier.encode()).hexdigest()[:8],16)%WORKERS
  parts[slot].append(item)
  for scope in item['fallback']:counts[scope]=counts.get(scope,0)+1
 for i,part in enumerate(parts):
  temp=job/(str(i)+'.tmp');temp.write_text(''.join(json.dumps(x,ensure_ascii=False)+'\n' for x in part),encoding='utf8');temp.replace(job/(str(i)+'.jsonl'))
 plan=dict(start=pos,end=last,events=len(events),entities=len(owners),catalogEvents=catalog,workers=WORKERS,fallback=counts,created=time.time())
 m.old.atomic(manifest,plan);return job,plan

def recent_loop(m):
 workers=[Worker(m,'delta-'+str(i)) for i in range(WORKERS)]
 def status(name,**values):m.old.atomic(m.OP/'recent-status.json',dict(status=name,time=time.time(),engine='EVENT_DELTA',**values))
 while True:
  try:
   pos=int(m.meta('exploit_recent_seq'));job,plan=recent_plan(m,pos)
   if plan is None:status('CAUGHT_UP',lastEvent=pos);time.sleep(1);continue
   event_labels.prepare(m,job,plan)
   status('APPLYING',lastEvent=pos,throughEvent=plan['end'],events=plan['events'],entities=plan['entities'])
   started=time.time()
   with concurrent.futures.ThreadPoolExecutor(max_workers=plan['workers']) as pool:
    list(pool.map(lambda i:workers[i].apply(job/('worker-'+str(i)),job/(str(i)+'.jsonl')),range(plan['workers'])))
   with m.old.db() as c:c.execute('INSERT OR REPLACE INTO meta(key,value) VALUES (?,?)',('exploit_recent_seq',str(plan['end'])))
   status('COMMITTED',lastEvent=plan['end'],events=plan['events'],entities=plan['entities'],seconds=time.time()-started)
  except Exception as e:status('RETRY_PENDING',error=str(e));time.sleep(5)

def start_recent(m,boundary):
 with m.old.db() as c:c.execute('INSERT OR IGNORE INTO meta(key,value) VALUES (?,?)',('exploit_recent_seq',str(boundary)))
 threading.Thread(target=recent_loop,args=(m,),daemon=True,name='recent-projection').start()
