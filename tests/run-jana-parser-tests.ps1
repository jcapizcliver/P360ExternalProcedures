param([string]$ExampleXml)
$ErrorActionPreference = 'Stop'
$repository = Split-Path $PSScriptRoot -Parent
[xml]$eclipseClasspath = Get-Content -LiteralPath (Join-Path $repository '.classpath')
$libraries = @($eclipseClasspath.classpath.classpathentry | Where-Object kind -eq 'lib' | ForEach-Object {
    if ([IO.Path]::IsPathRooted($_.path)) { $_.path } else { Join-Path $repository $_.path }
})
$compileClasspath = (@((Join-Path $repository 'bin')) + $libraries) -join ';'
$javac = (Get-Command javac -ErrorAction Stop).Source
$java = Join-Path (Split-Path $javac -Parent) 'java.exe'
$classes = Join-Path ([IO.Path]::GetTempPath()) ('jana-parser-tests-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $classes | Out-Null
$source = Join-Path $repository 'src\mx\com\liverpool\p360\services\core\sftp'
& $javac --release 17 -encoding UTF-8 -proc:none -classpath $compileClasspath -sourcepath $classes -d $classes `
    (Join-Path $source 'ParseJanaAttributesFile.java') (Join-Path $source 'JanaAttributeParser.java') `
    (Join-Path $source 'AttributeImportSupport.java') (Join-Path $source 'ProductDataReconciler.java') `
    (Join-Path $source 'ECCUnmappedAttributes.java') (Join-Path $source 'ParseECCAttributesFile.java') (Join-Path $source 'ParseECC122Response.java') `
    (Join-Path $PSScriptRoot 'AttributeImportSupportTest.java') `
    (Join-Path $PSScriptRoot 'JanaAttributeParserTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Jana compilation failed.' }
$testArgs = @('-classpath', ($classes + ';' + $compileClasspath), 'mx.com.liverpool.p360.services.core.sftp.JanaAttributeParserTest')
if ($ExampleXml) { $testArgs += (Resolve-Path -LiteralPath $ExampleXml).Path }
& $java @testArgs
if ($LASTEXITCODE -ne 0) { throw 'Jana regression checks failed.' }
& $java -classpath ($classes + ';' + $compileClasspath) 'mx.com.liverpool.p360.services.core.sftp.AttributeImportSupportTest' `
    (Join-Path $PSScriptRoot 'fixtures\jana-article-parent.json') (Join-Path $PSScriptRoot 'fixtures\ecc-attributes.xml')
if ($LASTEXITCODE -ne 0) { throw 'Attribute routing regression checks failed.' }
Write-Output "Compiled with Java 17 and Eclipse dependencies. Test classes: $classes"
