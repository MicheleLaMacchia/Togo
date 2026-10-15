# Script PowerShell per build Android APK via Docker
# Basato su: guida_android_docker_windows.md

$ErrorActionPreference = "Stop"

Write-Host "Build APK Android via Docker..."

docker run --rm `
  -v "${PWD}:/workspace" `
  -v "android-gradle-cache:/root/.gradle" `
  -w /workspace `
  android-builder `
  ./gradlew clean assembleDebug

$apk = ".\app\build\outputs\apk\debug\app-debug.apk"

if (Test-Path $apk) {
    Write-Host ""
    Write-Host "BUILD OK"
    Write-Host "APK: $apk"
} else {
    throw "Build terminata ma APK non trovato in $apk"
}