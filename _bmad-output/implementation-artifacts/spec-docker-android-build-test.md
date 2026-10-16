---
title: 'Script Docker per build e test Android'
type: 'feature'
created: '2026-10-16'
status: 'in-progress'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '5e29a43bb64ea391f23c6c503fb5506b3bf3cfca'
context: []
---

<frozen-after-approval reason="intento deciso dall'utente — non modificare senza rinegoziazione">

## Intent

**Problema:** Il PC host non ha l'Android SDK; `local.properties` contiene un `sdk.dir` Windows che non è valido nel container Linux. Il Docker build script monta il repository intero, quindi Gradle può leggere quel percorso host al posto dell'SDK installato nell'immagine.

**Approccio:** Rendere gli script `build-android.ps1` e `build-install-android.ps1` autonomi dall'SDK host fornendo al container un `local.properties` temporaneo con `sdk.dir=/opt/android-sdk`, senza alterare il file dell'utente. Aggiungere `test-android.ps1` per eseguire i test JVM del modulo tramite la stessa immagine Docker e documentare i tre flussi.

## Boundaries & Constraints

**Sempre:** Usare l'immagine `android-builder`, montare il repository così gli output restano sul filesystem host, riusare il volume Gradle e propagare gli errori Docker/Gradle. Decisione di Mike: includere anche `build-install-android.ps1`. Creare e rimuovere l'override SDK temporaneo anche in caso di errore. Chiamare il wrapper via `sh ./gradlew` perché è tracciato senza bit eseguibile. Mike esegue i comandi; questa sessione non farà build o test.

**Mai:** Non modificare `local.properties`, `gradlew`, il Dockerfile o file di configurazione dell'SDK host. Non cancellare output preesistenti oltre al `clean` già previsto dal build script. Non installare SDK/JDK sull'host né cambiare le versioni dell'immagine.

## I/O & Edge-Case Matrix

| Scenario | Input / Stato | Risultato atteso | Gestione errori |
|----------|---------------|------------------|-----------------|
| Build | Immagine Docker presente, `local.properties` host con percorso Windows | Gradle usa `/opt/android-sdk`; l'APK debug è scritto in `app/build/outputs/apk/debug/` | Codice d'uscita non zero produce errore; un APK vecchio non deve far apparire la build riuscita |
| Test JVM | Immagine Docker presente | Esegue `:app:testDebugUnitTest` nel container e restituisce il suo codice d'uscita | Fallimento Docker/Gradle è visibile e restituito |
| Pulizia | Successo o fallimento del container | Il file `local.properties` temporaneo è rimosso e quello host rimane identico | Rimozione in `finally` |
| Prerequisito assente | Docker o immagine `android-builder` mancante | Nessun tentativo Gradle | Errore con istruzioni per avviare Docker o creare l'immagine |

</frozen-after-approval>

## Code Map

- `build-android.ps1` — attuale `docker run` monta `${PWD}` e poi esegue `./gradlew clean assembleDebug`; non controlla `$LASTEXITCODE` prima di verificare il path APK.
- `Dockerfile.android` — immagine `android-builder` basata su JDK 17, SDK a `/opt/android-sdk`, platform/build-tools 35.
- `local.properties` — contiene `sdk.dir=C:\Users\miche\AppData\Local\Android\Sdk`, non esistente su questo host; deve rimanere invariato.
- `build-install-android.ps1` — entry point build+ADB con lo stesso problema di `local.properties`; aggiornare il Docker build e controllare entrambi gli exit code.
- `guida_android_docker_windows.md` §§5–8, 14, 16, 19 — documenta build, build+install, test e permission-denied del wrapper; sostituire gli esempi diretti con i nuovi script/comandi.
- `.gitignore` — ignora output Gradle/APK ma non `local.properties`; non aggiungere o cambiare ignore in questa storia.

## Tasks & Acceptance

**Execution:**
- [x] `build-android.ps1` — montare un override temporaneo di `local.properties` nel container, usare `sh ./gradlew`, propagare l'exit code e rimuovere il file temporaneo anche in caso di errore — evita SDK host e successi falsi da APK obsoleti.
- [x] `build-install-android.ps1` — applicare lo stesso override locale temporaneo, invocare `sh ./gradlew`, verificare l'esito build prima del controllo APK/installazione e propagare gli errori ADB — mantiene funzionante l'entry point build+install.
- [x] `test-android.ps1` — aggiungere uno script PowerShell che usa la stessa immagine, mount, override e cache per eseguire `sh ./gradlew :app:testDebugUnitTest` con exit code propagato — consente i test senza SDK host.
- [x] `guida_android_docker_windows.md` — documentare la gestione temporanea di `local.properties`, i due flussi di build e il nuovo test script/comando — rende il flusso ripetibile.
- [x] Verifica statica degli script — controllare percorsi, cleanup con successo/errore e passaggio del codice d'uscita; non eseguire i comandi Gradle/Docker in questa sessione.

**Acceptance Criteria:**
- Dato `local.properties` con un percorso SDK Windows non disponibile nel container, quando si avvia `build-android.ps1`, allora solo il container vede l'override `/opt/android-sdk` e il file host resta byte-per-byte invariato.
- Dato `local.properties` host non valido nel container, quando si avvia `build-install-android.ps1`, allora il container usa `/opt/android-sdk` e l'installazione parte solo dopo una build riuscita.
- Data una build riuscita, quando termina `build-android.ps1`, allora segnala l'APK generato; dato un errore del container, quando termina lo script, allora restituisce errore e non considera valido un APK precedente.
- Dato `test-android.ps1`, quando è invocato dalla root del repository, allora esegue `:app:testDebugUnitTest` in `android-builder` e restituisce il relativo codice di uscita.
- Dato successo o errore del container, quando termina ciascuno script, allora il file SDK temporaneo viene rimosso e `local.properties` non viene modificato.
- Dato Docker o immagine mancante, quando uno script viene eseguito, allora interrompe con istruzioni chiare senza avviare Gradle.
- Data la guida operativa (`guida_android_docker_windows.md`), quando un developer consulta build e test, allora trova i comandi aggiornati e sa che l'SDK richiesto è nel container.

## Implementation Notes

- `build-android.ps1`, `build-install-android.ps1` e `test-android.ps1` creano un override SDK temporaneo e lo montano in sola lettura su `/workspace/local.properties`; il file host non viene toccato.
- I wrapper invocano `sh ./gradlew` e preservano gli exit code Docker/Gradle; `build-install-android.ps1` risolve ADB da PATH, variabili Android o dal percorso host noto.
- Verifica statica PowerShell e `git diff --check` passati. Docker, Gradle, build e test non eseguiti per istruzione di Mike.

## Spec Change Log

## Review Triage Log

## Verification

**Comandi da eseguire da Mike, non da questa sessione:**
- `docker build -f Dockerfile.android -t android-builder .` (solo se l'immagine non esiste o il Dockerfile è cambiato).
- `.\build-android.ps1` — atteso: APK debug prodotto nel repository.
- `.\build-install-android.ps1` — atteso: APK compilato e installato solo se build e device sono disponibili.
- `.\test-android.ps1` — atteso: test JVM eseguiti nel container; exit code Gradle propagato.

**Controlli statici in questa sessione:** diff/whitespace; nessun Docker, Gradle, build o test verrà eseguito.
