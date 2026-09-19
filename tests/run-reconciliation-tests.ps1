$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
[xml]$ec=Get-Content (Join-Path $repo '.classpath')
$libs=@($ec.classpath.classpathentry | Where-Object kind -eq 'lib' | ForEach-Object {if([IO.Path]::IsPathRooted($_.path)){$_.path}else{Join-Path $repo $_.path}})
$cp=(@((Join-Path $repo 'bin'))+$libs)-join ';'
$out=Join-Path ([IO.Path]::GetTempPath()) ('reconciliation-tests-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory $out | Out-Null
$src=Join-Path $repo 'src/mx/com/liverpool/p360/services/core'
$javac=(Get-Command javac -ErrorAction Stop).Source
$java=Join-Path (Split-Path $javac -Parent) 'java.exe'
& $javac --release 17 -encoding UTF-8 -proc:none -cp $cp -sourcepath $out -d $out "$src/sftp/ProductDataReconciler.java" "$src/sftp/ReconciliationPlan.java" "$src/sftp/ParseECC122Response.java" "$src/sftp/ParseJana122Response.java" "$src/MongoProductReader.java" "$src/sftp/DuplicateGroupAnalysis.java" "$src/sftp/MongoDuplicateEvidence.java" "$PSScriptRoot/ReconciliationPlanTest.java" "$PSScriptRoot/DuplicateGroupAnalysisTest.java"
if($LASTEXITCODE -ne 0){throw 'Compilation failed'}
& $java -cp ($out+';'+$cp) mx.com.liverpool.p360.services.core.sftp.ReconciliationPlanTest
if($LASTEXITCODE -ne 0){throw 'Tests failed'}

& $java -cp ($out+';'+$cp) mx.com.liverpool.p360.services.core.sftp.DuplicateGroupAnalysisTest
if($LASTEXITCODE -ne 0){throw 'Duplicate group analysis tests failed'}
