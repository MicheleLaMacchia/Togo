# Script PowerShell per build Android APK via Docker
# Basato su: guida_android_docker_windows.md

$ErrorActionPreference = "Stop"

$repoRoot = $PSScriptRoot

function ConvertTo-WslPath {
    param([Parameter(Mandatory)][string]$WindowsPath)

    $fullPath = [System.IO.Path]::GetFullPath($WindowsPath)
    if ($fullPath -notmatch '^([A-Za-z]):\\(.*)$') {
        throw "Percorso Windows non convertibile in percorso WSL: $fullPath"
    }

    $drive = $Matches[1].ToLowerInvariant()
    $relativePath = $Matches[2] -replace '\\', '/'
    return "/mnt/$drive/$relativePath"
}

$repoRootWslPath = ConvertTo-WslPath $repoRoot
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

Write-Host "Build APK Android via Docker..."

try {
    [System.IO.File]::WriteAllText($localPropertiesTemp, "sdk.dir=/opt/android-sdk" + [Environment]::NewLine, [System.Text.Encoding]::ASCII)
    $localPropertiesDockerPath = ConvertTo-WslPath $localPropertiesTemp

    # In Windows PowerShell, stderr from native programs can become a terminating
    # error under ErrorActionPreference=Stop. Preserve the native exit code instead.
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & docker run --rm `
      -v "${repoRootWslPath}:/workspace" `
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

if (Test-Path -LiteralPath $apk) {
    Write-Host ""
    Write-Host "BUILD OK"
    Write-Host "APK: $apk"
} else {
    throw "Build terminata ma APK non trovato in $apk"
}
