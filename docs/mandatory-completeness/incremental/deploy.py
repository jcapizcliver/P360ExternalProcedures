from pathlib import Path
import shutil, hashlib, os

root = Path('/u01/workshop/java')
release = root / 'releases/mandatory-incremental-20260907'
backup = release / 'backup'
if backup.exists():
    raise SystemExit('Deployment backup already exists; refusing to overwrite rollback data')
sources = [p for p in (release/'src').glob('*.java')]
classes = [p for p in (release/'classes').rglob('*.class')
           if 'IncrementalCheck' not in p.name]
items = [(p, root/'bin'/p.relative_to(release/'classes')) for p in classes]
items += [(p, root/'src'/p.name) for p in sources]
# All planned paths are confined to java/bin or the existing flat java/src.
for source, target in items:
    assert os.path.commonpath([str(target.resolve()), str(root.resolve())]) == str(root.resolve())
    assert source.is_file()
backup.mkdir()
manifest = []
for source, target in items:
    rel = target.relative_to(root)
    if target.exists():
        dest = backup/rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(target, dest)
    manifest.append(str(rel))
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)
    assert hashlib.sha256(source.read_bytes()).digest() == hashlib.sha256(target.read_bytes()).digest()
(backup/'installed-paths.txt').write_text('\n'.join(manifest)+'\n')
(release/'installed.sha256').write_text(''.join(hashlib.sha256(t.read_bytes()).hexdigest()+'  '+str(t)+'\n' for _,t in items))
print('DEPLOYED_FILES='+str(len(items)))
