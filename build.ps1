# Java Quest 100 - build completa: merge dati, javac, jar, jlink, jpackage (app-image).
# Uso:  powershell -ExecutionPolicy Bypass -File build.ps1 [-SkipPackage] [-Test]
#   -SkipPackage  solo compilazione + jar
#   -Test         dopo il packaging esegue dist\JavaQuest100\JavaQuest100.exe --selftest
param(
    [switch]$SkipPackage,
    [switch]$Test
)
$ErrorActionPreference = 'Stop'
$Root    = $PSScriptRoot
$JdkBin  = Join-Path $Root 'tools\jdk\bin'
$FxLib   = Join-Path $Root 'tools\javafx-sdk\lib'
$FxJmods = Join-Path $Root 'tools\javafx-jmods'
$JdkJmods = Join-Path $Root 'tools\jdk\jmods'
$Build   = Join-Path $Root 'build'
$Classes = Join-Path $Build 'classes'
$JarDir  = Join-Path $Build 'jar'
$Runtime = Join-Path $Build 'runtime'
$Dist    = Join-Path $Root 'dist'
$AppName = 'JavaQuest100'

function Step($msg) { Write-Host "==> $msg" -ForegroundColor Green }
function Check($what) { if ($LASTEXITCODE -ne 0) { throw "$what fallito (exit $LASTEXITCODE)" } }

# 0. merge del curriculum se tutte le parti esistono
$parts = 1..4 | ForEach-Object { Join-Path $Root "data-src\part$_.json" }
$present = @($parts | Where-Object { Test-Path $_ })
if ($present.Count -eq 4) {
    Step 'Merge curriculum (data-src\merge.py)'
    $py = Get-Command python -ErrorAction SilentlyContinue
    if (-not $py) { $py = Get-Command py -ErrorAction SilentlyContinue }
    if (-not $py) { throw 'Python non trovato: impossibile eseguire merge.py' }
    & $py.Source (Join-Path $Root 'data-src\merge.py'); Check 'merge.py'
} else {
    Write-Host "Parti del curriculum presenti: $($present.Count)/4 - uso il curriculum.json esistente" -ForegroundColor Yellow
}

# 1. pulizia
Step 'Pulizia'
if (Test-Path $Build) { Remove-Item -Recurse -Force $Build }
New-Item -ItemType Directory -Force $Classes, $JarDir | Out-Null

# 2. compilazione
Step 'Compilazione (javac)'
$sources = @(Get-ChildItem -Recurse -Filter *.java (Join-Path $Root 'src\main\java') | ForEach-Object { $_.FullName })
& (Join-Path $JdkBin 'javac.exe') -encoding UTF-8 --release 25 -Xlint:-options `
    --module-path $FxLib --add-modules javafx.controls `
    -d $Classes $sources
Check 'javac'

# 3. risorse
Step 'Copia risorse'
Copy-Item -Recurse -Force (Join-Path $Root 'src\main\resources\*') $Classes

# 4. jar
Step 'Creazione jar'
$Jar = Join-Path $JarDir "$AppName.jar"
& (Join-Path $JdkBin 'jar.exe') --create --file $Jar --main-class app.Main -C $Classes .
Check 'jar'

if ($SkipPackage) { Write-Host "Jar pronto: $Jar"; exit 0 }

# 5. jlink (runtime con JavaFX). Se il JDK non ha jmods, jlink usa l'immagine runtime (JEP 493).
Step 'Runtime (jlink)'
$modulePath = $FxJmods
if (Test-Path $JdkJmods) { $modulePath = "$JdkJmods;$FxJmods" }
& (Join-Path $JdkBin 'jlink.exe') --module-path $modulePath `
    --add-modules javafx.controls,javafx.graphics,javafx.base,java.desktop,java.logging,jdk.unsupported,jdk.charsets,jdk.localedata `
    --include-locales it,en `
    --strip-debug --no-header-files --no-man-pages --compress zip-6 `
    --output $Runtime
Check 'jlink'

# 6. jpackage app-image
Step 'Packaging (jpackage app-image)'
$AppDir = Join-Path $Dist $AppName
if (Test-Path $AppDir) { Remove-Item -Recurse -Force $AppDir }
New-Item -ItemType Directory -Force $Dist | Out-Null
& (Join-Path $JdkBin 'jpackage.exe') --type app-image --name $AppName `
    --input $JarDir --main-jar "$AppName.jar" --main-class app.Main `
    --runtime-image $Runtime --dest $Dist --icon (Join-Path $PSScriptRoot 'icon-src\icon.ico') `
    --app-version 1.0.0 --vendor 'Java Quest' --description 'Java Quest 100 - impara Java giocando' `
    --java-options '--enable-native-access=javafx.graphics' `
    --java-options '-Dfile.encoding=UTF-8'
Check 'jpackage'
$Exe = Join-Path $AppDir "$AppName.exe"
Write-Host "Pronto: $Exe" -ForegroundColor Green

# 7. selftest opzionale sull'exe (il launcher e' GUI: esito nell'exit code e in %TEMP%\JavaQuest100\selftest.log)
if ($Test) {
    Step 'Selftest dell''eseguibile'
    $p = Start-Process -FilePath $Exe -ArgumentList '--selftest' -Wait -PassThru
    Get-Content (Join-Path $env:TEMP 'JavaQuest100\selftest.log')
    if ($p.ExitCode -ne 0) { throw "Selftest fallito (exit $($p.ExitCode))" }
}
