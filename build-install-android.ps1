# Script PowerShell per build + installazione APK via Docker
# Basato su: guida_android_docker_windows.md
#
# Prerequisiti:
#   1. Immagine docker "android-builder" gia' creata (vedi build-android.ps1)
#   2. Platform-tools (adb) installato sul PC host
#   3. Telefono collegato via USB con Debug USB abilitato

$ErrorActionPreference = "Stop"

$repoRoot = $PSScriptRoot
$apk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker non è disponibile. Avvia Docker Desktop e riprova."
}

& docker image inspect android-builder *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Immagine Docker 'android-builder' non disponibile. Avvia Docker Desktop e, se l'immagine manca, esegui: docker build -f Dockerfile.android -t android-builder ."
}

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
$adbPath = if ($adbCommand) { $adbCommand.Source } else { $null }
if (-not $adbPath -and $env:ANDROID_SDK_ROOT) {
    $sdkAdb = Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"
    if (Test-Path -LiteralPath $sdkAdb) { $adbPath = $sdkAdb }
}
if (-not $adbPath -and $env:ANDROID_HOME) {
    $sdkAdb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
    if (Test-Path -LiteralPath $sdkAdb) { $adbPath = $sdkAdb }
}
if (-not $adbPath -and (Test-Path -LiteralPath "C:\Android\platform-tools\adb.exe")) {
    # Fallback per la configurazione locale usata nello sviluppo del progetto.
    $adbPath = "C:\Android\platform-tools\adb.exe"
}
if (-not $adbPath) {
    throw "ADB non è disponibile. Aggiungi Android Platform Tools al PATH oppure installalo in C:\Android\platform-tools."
}

$localPropertiesTemp = Join-Path ([System.IO.Path]::GetTempPath()) ("togo-local.properties-" + [guid]::NewGuid().ToString("N") + ".tmp")
$dockerExitCode = 0

Write-Host "1/3 - Build Android via Docker"

try {
    [System.IO.File]::WriteAllText($localPropertiesTemp, "sdk.dir=/opt/android-sdk" + [Environment]::NewLine, [System.Text.Encoding]::ASCII)
    $localPropertiesDockerPath = $localPropertiesTemp.Replace('\', '/')

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & docker run --rm `
      -v "${repoRoot}:/workspace" `
      -v "${localPropertiesDockerPath}:/workspace/local.properties:ro" `
      -v "android-gradle-cache:/root/.gradle" `
      -w /workspace `
      android-builder `
      sh ./gradlew clean assembleDebug

    $dockerExitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
    if ($dockerExitCode -ne 0) {
        Write-Error "Build Docker/Gradle fallita (codice $dockerExitCode)." -ErrorAction Continue
    }
} finally {
    if ($null -ne $previousErrorActionPreference) {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if (Test-Path -LiteralPath $localPropertiesTemp) {
        Remove-Item -LiteralPath $localPropertiesTemp -Force
    }
}

if ($dockerExitCode -ne 0) {
    exit $dockerExitCode
}

if (-not (Test-Path -LiteralPath $apk)) {
    throw "APK non trovato: $apk"
}

Write-Host "2/3 - Verifica dispositivo"

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$devices = & $adbPath devices
$adbExitCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorActionPreference
Write-Host $devices
if ($adbExitCode -ne 0) {
    Write-Error "Il controllo dei dispositivi ADB è fallito (codice $adbExitCode)." -ErrorAction Continue
    exit $adbExitCode
}

Write-Host "3/3 - Installazione APK"

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
& $adbPath install -r $apk
$adbExitCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorActionPreference
if ($adbExitCode -ne 0) {
    Write-Error "Installazione ADB fallita (codice $adbExitCode)." -ErrorAction Continue
    exit $adbExitCode
}

Write-Host ""
Write-Host "Build e installazione completate."
Write-Host "APK: $apk"
