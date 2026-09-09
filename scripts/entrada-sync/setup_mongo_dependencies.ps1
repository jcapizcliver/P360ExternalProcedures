# Descarga solo las bibliotecas del proyecto.
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$dest = Join-Path $repo 'lib/mongodb'
New-Item -ItemType Directory -Force -Path $dest | Out-Null
$file = Join-Path $dest 'bson-5.6.5.jar'
if (!(Test-Path -LiteralPath $file)) { Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/mongodb/bson/5.6.5/bson-5.6.5.jar' -OutFile $file }
if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne '3608e3573b4d6bb2ab5af65e1494f98600b117c93e2e17d2466aa513d4cb7743') { throw 'Checksum inesperado: bson-5.6.5.jar' }
$file = Join-Path $dest 'mongodb-driver-core-5.6.5.jar'
if (!(Test-Path -LiteralPath $file)) { Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/mongodb/mongodb-driver-core/5.6.5/mongodb-driver-core-5.6.5.jar' -OutFile $file }
if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne '125489cc9cef8b510c3ebd46f1c5bfc75d864630eff9ffb40a239ed7732633fc') { throw 'Checksum inesperado: mongodb-driver-core-5.6.5.jar' }
$file = Join-Path $dest 'mongodb-driver-sync-5.6.5.jar'
if (!(Test-Path -LiteralPath $file)) { Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/mongodb/mongodb-driver-sync/5.6.5/mongodb-driver-sync-5.6.5.jar' -OutFile $file }
if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne '0081f88d2d1b293719e1fa65608e69e117380e17b346b304f835509f8a20c12e') { throw 'Checksum inesperado: mongodb-driver-sync-5.6.5.jar' }
