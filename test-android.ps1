# Esegue i test JVM Android nel container Docker.

$ErrorActionPreference = "Stop"

$repoRoot = $PSScriptRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker non è disponibile. Avvia Docker Desktop e riprova."
}

& docker image inspect android-builder *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Immagine Docker 'android-builder' non disponibile. Avvia Docker Desktop e, se l'immagine manca, esegui: docker build -f Dockerfile.android -t android-builder ."
}

$localPropertiesTemp = Join-Path ([System.IO.Path]::GetTempPath()) ("togo-local.properties-" + [guid]::NewGuid().ToString("N") + ".tmp")
$dockerExitCode = 0

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
      sh ./gradlew :app:testDebugUnitTest

    $dockerExitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
} finally {
    if ($null -ne $previousErrorActionPreference) {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if (Test-Path -LiteralPath $localPropertiesTemp) {
        Remove-Item -LiteralPath $localPropertiesTemp -Force
    }
}

if ($dockerExitCode -ne 0) {
    Write-Error "Test Docker/Gradle falliti (codice $dockerExitCode)." -ErrorAction Continue
    exit $dockerExitCode
}

Write-Host "Test JVM completati con successo."
