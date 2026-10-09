# Guida Windows — Build Android APK con Docker e installazione su smartphone

Questa guida descrive un flusso di sviluppo Android in cui:

- il codice Kotlin/Gradle resta sul PC Windows;
- Android SDK, JDK e build-tools vengono usati dentro Docker;
- non è necessario installare Android Studio;
- l'APK viene generato dal container ma salvato nel progetto locale;
- l'APK può essere installato sul telefono tramite ADB oppure copiato manualmente.

---

## 1. Architettura del flusso

```text
BMAD / Codex
    ↓
genera o modifica il codice Kotlin
    ↓
repository locale Windows
    ↓
Docker container
    ├─ JDK
    ├─ Android SDK command-line tools
    ├─ platform-tools
    └─ build-tools
    ↓
./gradlew assembleDebug
    ↓
app/build/outputs/apk/debug/app-debug.apk
    ↓
telefono Android
```

Per i test sul telefono reale non serve un emulatore.

---

## 2. Prerequisiti sul PC Windows

Servono:

1. Docker Desktop oppure un runtime Docker compatibile già funzionante.
2. Il repository Android con Gradle Wrapper:
   - `gradlew`
   - `gradlew.bat`
   - cartella `gradle/wrapper`
3. Facoltativo ma consigliato: Android Platform Tools sul PC host, per usare `adb`.

Verifica Docker da PowerShell:

```powershell
docker version
```

Se il comando restituisce sia Client sia Server, Docker è operativo.

---

## 3. Verificare le versioni richieste dal progetto

Prima di fissare le versioni nel container, controlla nel progetto:

```text
app/build.gradle.kts
build.gradle.kts
gradle/libs.versions.toml
```

Cerca in particolare:

```kotlin
compileSdk = ...
targetSdk = ...
minSdk = ...
```

Il container deve avere installata almeno la piattaforma corrispondente a `compileSdk`.

Esempio:

```kotlin
compileSdk = 35
```

richiede:

```text
platforms;android-35
```

---

## 4. Dockerfile per la build Android

Crea nella root del repository un file:

```text
Dockerfile.android
```

Esempio:

```dockerfile
FROM eclipse-temurin:17-jdk

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget unzip ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-latest.zip -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp/android-tools \
    && mkdir -p ${ANDROID_HOME}/cmdline-tools/latest \
    && mv /tmp/android-tools/cmdline-tools/* ${ANDROID_HOME}/cmdline-tools/latest/ \
    && rm -rf /tmp/cmdline-tools.zip /tmp/android-tools

RUN yes | sdkmanager --licenses >/dev/null || true

RUN sdkmanager \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0"

WORKDIR /workspace
```

### Importante

Se il progetto usa una versione diversa, modifica:

```text
platforms;android-35
build-tools;35.0.0
```

in base al progetto.

---

## 5. Costruire l'immagine Docker

Dalla root del repository, in PowerShell:

```powershell
docker build -f Dockerfile.android -t android-builder .
```

Controlla che l'immagine esista:

```powershell
docker images android-builder
```

Questa operazione va ripetuta soltanto quando modifichi il Dockerfile o le versioni dell'SDK/build-tools.

---

## 6. Generare l'APK debug

Sempre dalla root del progetto:

```powershell
docker run --rm `
  -v "${PWD}:/workspace" `
  -w /workspace `
  android-builder `
  ./gradlew assembleDebug
```

Il repository viene montato come volume dentro:

```text
/workspace
```

Quindi l'APK prodotto dal container viene scritto direttamente sul filesystem Windows.

Normalmente il file risultante è:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Verifica:

```powershell
Test-Path ".\app\build\outputs\apk\debug\app-debug.apk"
```

oppure:

```powershell
Get-Item ".\app\build\outputs\apk\debug\app-debug.apk"
```

---

## 7. Cache Gradle consigliata

Senza cache, Docker potrebbe riscaricare dipendenze Gradle a ogni build.

Puoi creare un volume persistente:

```powershell
docker volume create android-gradle-cache
```

Poi usare:

```powershell
docker run --rm `
  -v "${PWD}:/workspace" `
  -v "android-gradle-cache:/root/.gradle" `
  -w /workspace `
  android-builder `
  ./gradlew assembleDebug
```

Questo rende le build successive sensibilmente più rapide.

---

## 8. Script PowerShell per automatizzare la build

Crea nella root del progetto:

```text
build-android.ps1
```

Contenuto:

```powershell
$ErrorActionPreference = "Stop"

Write-Host "Build APK Android via Docker..."

docker run --rm `
  -v "${PWD}:/workspace" `
  -v "android-gradle-cache:/root/.gradle" `
  -w /workspace `
  android-builder `
  ./gradlew assembleDebug

$apk = ".\app\build\outputs\apk\debug\app-debug.apk"

if (Test-Path $apk) {
    Write-Host ""
    Write-Host "BUILD OK"
    Write-Host "APK: $apk"
} else {
    throw "Build terminata ma APK non trovato in $apk"
}
```

Avvio:

```powershell
.\build-android.ps1
```

---

## 9. Installare l'APK manualmente sul telefono

Metodo più semplice, senza ADB:

1. genera `app-debug.apk`;
2. trasferisci il file sul telefono tramite:
   - cavo USB;
   - Google Drive;
   - OneDrive;
   - Telegram;
   - altro sistema di trasferimento;
3. apri l'APK sul telefono;
4. Android potrebbe chiederti di autorizzare l'installazione di app sconosciute per quella specifica sorgente;
5. autorizza la sorgente;
6. installa l'app.

Questo metodo funziona bene per test occasionali.

---

## 10. Metodo consigliato: installazione tramite ADB

Per uno sviluppo iterativo è molto più pratico usare ADB.

### 10.1 Installare soltanto Platform Tools

Non serve Android Studio.

Scarica Android SDK Platform Tools per Windows dal sito ufficiale Android e decomprimi, ad esempio, in:

```text
C:\Tools\platform-tools
```

Puoi poi:

- usare `adb.exe` direttamente da quella directory;
- oppure aggiungere `C:\Tools\platform-tools` al `PATH`.

Verifica:

```powershell
adb version
```

---

## 11. Preparare il telefono Android

Sul telefono:

1. apri **Impostazioni**;
2. vai a **Info sul telefono**;
3. individua **Numero build** o la voce equivalente;
4. toccala ripetutamente finché vengono abilitate le **Opzioni sviluppatore**;
5. apri le **Opzioni sviluppatore**;
6. abilita **Debug USB**.

Collega quindi il telefono al PC tramite USB.

Alla prima connessione il telefono mostrerà una richiesta simile a:

```text
Consentire debug USB da questo computer?
```

Accetta.

---

## 12. Verificare che ADB veda il telefono

Da PowerShell:

```powershell
adb devices
```

Output atteso:

```text
List of devices attached
XXXXXXXXXXXX    device
```

Se compare:

```text
unauthorized
```

sblocca lo smartphone e accetta la richiesta di autorizzazione.

---

## 13. Installare l'APK tramite ADB

Dalla root del progetto:

```powershell
adb install -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

L'opzione:

```text
-r
```

reinstalla/aggiorna l'app esistente, mantenendo normalmente i dati dell'app se package e firma restano compatibili.

Output atteso:

```text
Performing Streamed Install
Success
```

A quel punto puoi aprire l'app direttamente dal telefono.

---

## 14. Build + installazione in un solo script

Puoi creare:

```text
build-install-android.ps1
```

Contenuto:

```powershell
$ErrorActionPreference = "Stop"

$apk = ".\app\build\outputs\apk\debug\app-debug.apk"

Write-Host "1/3 - Build Android via Docker"

docker run --rm `
  -v "${PWD}:/workspace" `
  -v "android-gradle-cache:/root/.gradle" `
  -w /workspace `
  android-builder `
  ./gradlew assembleDebug

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
```

Uso:

```powershell
.\build-install-android.ps1
```

Il flusso diventa:

```text
BMAD/Codex modifica codice
        ↓
.\build-install-android.ps1
        ↓
Docker compila
        ↓
APK
        ↓
ADB installa
        ↓
test sul telefono
```

---

## 15. Pulire la build

Se vuoi eseguire una compilazione pulita:

```powershell
docker run --rm `
  -v "${PWD}:/workspace" `
  -v "android-gradle-cache:/root/.gradle" `
  -w /workspace `
  android-builder `
  ./gradlew clean assembleDebug
```

Non serve farlo a ogni build.

---

## 16. Eseguire test e lint nel container

Test unitari:

```powershell
docker run --rm `
  -v "${PWD}:/workspace" `
  -v "android-gradle-cache:/root/.gradle" `
  -w /workspace `
  android-builder `
  ./gradlew test
```

Lint:

```powershell
docker run --rm `
  -v "${PWD}:/workspace" `
  -v "android-gradle-cache:/root/.gradle" `
  -w /workspace `
  android-builder `
  ./gradlew lint
```

Build + test:

```powershell
docker run --rm `
  -v "${PWD}:/workspace" `
  -v "android-gradle-cache:/root/.gradle" `
  -w /workspace `
  android-builder `
  ./gradlew test assembleDebug
```

---

## 17. Integrazione consigliata con BMAD / Codex

Conviene trattare Docker come ambiente ufficiale di build del progetto.

BMAD/Codex dovrebbe:

1. modificare il codice;
2. non assumere Android SDK installato sull'host;
3. eseguire lo script Docker;
4. leggere l'output Gradle;
5. correggere eventuali errori;
6. rilanciare la build;
7. produrre un APK valido.

Il comando standard può essere:

```powershell
.\build-android.ps1
```

oppure, quando il telefono è collegato:

```powershell
.\build-install-android.ps1
```

Questo evita che BMAD/Codex debba conoscere dettagli dell'ambiente host.

---

## 18. Debug APK vs Release APK

Durante lo sviluppo usa:

```bash
./gradlew assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Per una release reale serviranno invece:

- keystore;
- configurazione signing;
- credenziali gestite in modo sicuro;
- eventualmente AAB per Google Play.

Non mettere mai password del keystore direttamente nel repository.

Per il Play Store generalmente userai:

```bash
./gradlew bundleRelease
```

che produce un `.aab`.

---

## 19. Possibili problemi comuni

### `./gradlew: Permission denied`

Dentro Linux/Docker il wrapper potrebbe non essere eseguibile.

Puoi usare:

```powershell
git update-index --chmod=+x gradlew
```

oppure nel Dockerfile/script:

```bash
chmod +x ./gradlew
```

### SDK mancante

Errore simile a:

```text
Failed to find target with hash string 'android-XX'
```

Aggiungi al Dockerfile:

```text
platforms;android-XX
```

e ricostruisci l'immagine.

### Build Tools mancanti

Aggiungi la versione necessaria:

```text
build-tools;XX.X.X
```

### ADB non vede il telefono

Controlla:

- Debug USB;
- autorizzazione RSA sul telefono;
- cavo USB dati, non soltanto ricarica;
- eventuali driver USB del produttore;
- modalità USB del telefono.

Poi prova:

```powershell
adb kill-server
adb start-server
adb devices
```

### `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

Può accadere quando la versione installata è firmata con una chiave diversa.

Per una build di sviluppo puoi disinstallare quella precedente:

```powershell
adb uninstall nome.del.package
```

e reinstallare l'APK.

Attenzione: la disinstallazione elimina normalmente i dati locali dell'app.

---

## 20. Struttura consigliata del repository

```text
project/
├─ app/
├─ gradle/
├─ gradlew
├─ gradlew.bat
├─ build.gradle.kts
├─ settings.gradle.kts
├─ Dockerfile.android
├─ build-android.ps1
└─ build-install-android.ps1
```

In questo modo tutto ciò che serve per la build è versionabile insieme al progetto, tranne tool host come Docker e ADB.

---

## 21. Workflow quotidiano consigliato

### Quando sviluppi senza telefono collegato

```powershell
.\build-android.ps1
```

### Quando vuoi provarlo sul telefono

```powershell
adb devices
.\build-install-android.ps1
```

### Quando qualcosa sembra incoerente

```powershell
docker run --rm `
  -v "${PWD}:/workspace" `
  -v "android-gradle-cache:/root/.gradle" `
  -w /workspace `
  android-builder `
  ./gradlew clean test assembleDebug
```

---

## 22. Risultato finale

La configurazione finale consigliata è:

```text
Windows
├─ Docker
├─ ADB / Platform Tools
└─ repository Android
      ↓
Docker image android-builder
      ├─ JDK
      ├─ Android SDK
      ├─ build-tools
      └─ Gradle Wrapper del progetto
      ↓
APK debug
      ↓
ADB
      ↓
smartphone Android reale
```

Vantaggi:

- niente Android Studio obbligatorio;
- niente Android SDK completo installato sull'host;
- build riproducibili;
- ambiente coerente per BMAD/Codex;
- test sul telefono reale;
- possibilità di automatizzare build, test, lint e installazione.
