# Script PowerShell per build + installazione APK via Docker
# Basato su: guida_android_docker_windows.md
#
# Prerequisiti:
#   1. Immagine docker "android-builder" gia' creata (vedi build-android.ps1)
#   2. Platform-tools (adb) installato sul PC host
#   3. Telefono collegato via USB con Debug USB abilitato

$ErrorActionPreference = "Stop"

$apk = ".\app\build\outputs\apk\debug\app-debug.apk"

Write-Host "1/3 - Build Android via Docker"

docker run --rm `
  -v "${PWD}:/workspace" `
  -v "android-gradle-cache:/root/.gradle" `
  -w /workspace `
  android-builder `
  ./gradlew clean assembleDebug

if (-not (Test-Path $apk)) {
    throw "APK non trovato: $apk"
}

Write-Host "2/3 - Verifica dispositivo"

$devices = adb devices
Write-Host $devices

Write-Host "3/3 - Installazione APK"

adb install -r $apk

Write-Host ""
Write-Host "Build e installazione completate."
Write-Host "APK: $apk"