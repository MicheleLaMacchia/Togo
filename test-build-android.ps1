# Esegue test JVM Android e build APK in un'unica esecuzione Gradle via Docker.
#
# Un singolo `docker run` + una singola invocazione Gradle: i task
# `:app:testDebugUnitTest` e `assembleDebug` condividono la stessa configurazione
# e lo stesso daemon. Se un task fallisce, Gradle si ferma e esce con codice != 0.

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

$localPropertiesTemp = Join-Path ([System.IO.Path]::GetTempPath()) ("togo-local.properties-" + [guid]::NewGuid().ToString("N") + ".tmp")
$dockerExitCode = 0

Write-Host "Test JVM + build APK Android via Docker (un'unica esecuzione)..."

try {
    [System.IO.File]::WriteAllText($localPropertiesTemp, "sdk.dir=/opt/android-sdk" + [Environment]::NewLine, [System.Text.Encoding]::ASCII)
    $localPropertiesDockerPath = $localPropertiesTemp.Replace('\', '/')

    # In Windows PowerShell, stderr from native programs can become a terminating
    # error under ErrorActionPreference=Stop. Preserve the native exit code instead.
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & docker run --rm `
      -v "${repoRoot}:/workspace" `
      -v "${localPropertiesDockerPath}:/workspace/local.properties:ro" `
      -v "android-gradle-cache:/root/.gradle" `
      -w /workspace `
      android-builder `
      sh ./gradlew :app:testDebugUnitTest assembleDebug

    $dockerExitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
    if ($dockerExitCode -ne 0) {
        Write-Error "Test o build Docker/Gradle falliti (codice $dockerExitCode)." -ErrorAction Continue
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

if (Test-Path -LiteralPath $apk) {
    Write-Host ""
    Write-Host "TEST + BUILD OK"
    Write-Host "APK: $apk"
} else {
    throw "Gradle è terminato con successo ma APK non trovato in $apk"
}
