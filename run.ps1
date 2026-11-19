# Java Quest 100 - avvio rapido per sviluppo (compila in build\dev e avvia con java).
# Uso:  powershell -ExecutionPolicy Bypass -File run.ps1 [--selftest]
$ErrorActionPreference = 'Stop'
$Root    = $PSScriptRoot
$JdkBin  = Join-Path $Root 'tools\jdk\bin'
$FxLib   = Join-Path $Root 'tools\javafx-sdk\lib'
$Classes = Join-Path $Root 'build\dev'

if (Test-Path $Classes) { Remove-Item -Recurse -Force $Classes }
New-Item -ItemType Directory -Force $Classes | Out-Null
$sources = @(Get-ChildItem -Recurse -Filter *.java (Join-Path $Root 'src\main\java') | ForEach-Object { $_.FullName })
& (Join-Path $JdkBin 'javac.exe') -encoding UTF-8 --release 25 --module-path $FxLib --add-modules javafx.controls -d $Classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilazione fallita' }
Copy-Item -Recurse -Force (Join-Path $Root 'src\main\resources\*') $Classes

& (Join-Path $JdkBin 'java.exe') --module-path $FxLib --add-modules javafx.controls `
    --enable-native-access=javafx.graphics -cp $Classes app.Main @args
exit $LASTEXITCODE
