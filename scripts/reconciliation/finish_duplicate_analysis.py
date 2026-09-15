"""Finite final stage of the already-started analysis, running under nohup on gcpcatpap06."""
import os
import subprocess
import time
import json
import sys

release='/u01/workshop/java/releases/duplicate-analysis-20260907'
db='/u01/workshop/java/salidas/duplicate-analysis-20260907'
mongo='/u01/workshop/java/salidas/duplicate-mongo-20260907'
review='/u01/workshop/java/salidas/duplicate-review-20260907'

def main():
    os.umask(0o077)
    print('Waiting for successful P360 and Mongo analysis completion',flush=True)
    failures=0
    for attempt in range(1440):
        result=subprocess.run(['ssh','-o','BatchMode=yes','-o','ConnectTimeout=15','gcpcatpap01','cat',release+'/db.exit'],stdout=subprocess.PIPE,stderr=subprocess.PIPE,universal_newlines=True,timeout=30)
        if result.returncode==0:
            status=result.stdout.strip()
            if status!='0':
                raise RuntimeError('P360 analysis failed: '+status)
            break
        if result.returncode==255:
            failures+=1
            if failures>=5:
                raise RuntimeError('Cannot reach gcpcatpap01 after five attempts')
        else:
            failures=0
        time.sleep(30)
    else:
        raise RuntimeError('P360 completion timeout after 12 hours')
    with open(release+'/mongo.exit') as source:
        if source.read().strip()!='0':
            raise RuntimeError('Mongo analysis failed')
    if os.path.exists(db) or os.path.exists(db+'.partial'):
        raise RuntimeError('Refusing to replace existing P360 evidence')
    print('Copying completed compressed P360 evidence from gcpcatpap01',flush=True)
    subprocess.run(['scp','-qpr','gcpcatpap01:'+db,db+'.partial'],check=True,timeout=3600)
    with open(db+'.partial/complete.json') as source:
        manifest=json.load(source)
    if manifest['groups']!=11337:
        raise RuntimeError('Unexpected group count')
    os.rename(db+'.partial',db)
    subprocess.run(['python3',release+'/build_duplicate_review.py',db,mongo,review],check=True)
    print('REVIEW_READY '+review,flush=True)

if __name__=='__main__':
    try:
        main()
    except Exception as error:
        print(type(error).__name__+': '+str(error),file=sys.stderr,flush=True)
        with open(release+'/review.exit','w') as target:
            target.write('1\n')
        raise SystemExit(1)
    with open(release+'/review.exit','w') as target:
        target.write('0\n')
