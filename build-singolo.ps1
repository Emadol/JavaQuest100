# Crea exe-unico\JavaQuest100_singolo.exe: un solo .exe autoestraente (con icona J).
# Usa il compilatore C# incluso in Windows (nessuna installazione). Prerequisito: dist\JavaQuest100 creata da build.ps1.
$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
$Out  = Join-Path $Root 'exe-unico'
$Work = Join-Path $env:TEMP 'jq_single'
$Csc  = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
if (-not (Test-Path (Join-Path $Root 'dist\JavaQuest100\JavaQuest100.exe'))) { throw 'Esegui prima build.ps1' }
if (-not (Test-Path $Csc)) { throw 'csc.exe non trovato' }

if (Test-Path $Work) { Remove-Item -Recurse -Force $Work }
New-Item -ItemType Directory -Force $Work, $Out | Out-Null

# payload.zip con percorsi relativi corretti
tar.exe -a -cf (Join-Path $Work 'payload.zip') -C (Join-Path $Root 'dist') JavaQuest100
if ($LASTEXITCODE -ne 0) { throw 'tar fallito' }

# sorgente con id build (nuova cartella di estrazione a ogni build)
$id = Get-Date -Format 'yyyyMMddHHmmss'
(Get-Content (Join-Path $Root 'icon-src\Stub.cs') -Raw) -replace '@@BUILDID@@', $id | Set-Content -Encoding UTF8 (Join-Path $Work 'Stub.cs')

$Target = Join-Path $Out 'JavaQuest100_singolo.exe'
if (Test-Path $Target) { Remove-Item -Force $Target }
& $Csc /nologo /target:winexe /optimize+ "/out:$Target" "/win32icon:$(Join-Path $Root 'icon-src\icon.ico')" `
    "/resource:$(Join-Path $Work 'payload.zip'),payload.zip" `
    /r:System.IO.Compression.dll /r:System.IO.Compression.FileSystem.dll /r:System.Windows.Forms.dll `
    (Join-Path $Work 'Stub.cs')
if ($LASTEXITCODE -ne 0) { throw 'compilazione fallita' }
Write-Host ("Pronto: {0} ({1:N0} MB)" -f $Target, ((Get-Item $Target).Length / 1MB)) -ForegroundColor Green
