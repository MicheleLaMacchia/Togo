---
title: 'Registrazione crash-loop avvio — bind Koin del gateway vocale mancante'
type: 'bugfix'
created: '2026-10-19'
status: 'ready'
route: 'dispatch'
baseline_commit: '483ee59c5122ba9e24acc9c551f7769462f4051b'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Dopo la fix del flusso vocale, l'app **non si avvia più**: crash-loop alla prima composizione con `org.koin.core.error.NoBeanDefFoundException: No definition found for type 'it.togo.app.data.voice.AndroidVoiceRecognitionGateway'` (`ActiveListScreen`, chiamata `koinInject<AndroidVoiceRecognitionGateway>()`). Causa: `DomainModule.kt:30` registra il gateway come `single<VoiceRecognitionGateway> { ... }` — legato **solo** al tipo interfaccia; Koin 3.5.6 risolve per tipo esatto, quindi la classe concreta richiesta dallo `koinInject` non ha definizione. La fix voice nello screen è **corretta e completa**: va toccato **solo** il modulo DI, esponendo entrambi i tipi sullo **stesso** singleton.

**Approach:** In `DomainModule.kt` sostituire la registrazione con `single { AndroidVoiceRecognitionGateway() } bind VoiceRecognitionGateway::class` (+ import `org.koin.dsl.bind`). Verificare staticamente e **chiedere all'utente di eseguire build e install** — l'agente NON esegue comandi di build.

**Always:** Modificare **solo** `app/src/main/java/it/togo/app/di/DomainModule.kt` (riga 30 + un import); preservare tutti gli altri file del working tree; garantire che le due richieste di tipo risolvano la **medesima** istanza singleton (il launcher registrato dallo screen deve arrivare al gateway usato dal ViewModel).

**Never:** **Eseguire comandi di build, install o test Gradle/Docker** (elenco in sezione dedicata — chiedere sempre all'utente); committare; `git checkout`/reset/stash su file modificati (`AndroidManifest.xml`, `ActiveListScreen.kt`, `CategoryHeader.kt`, `ItemRow.kt`, `PresiSection.kt`); modificare `ActiveListScreen.kt`, `AndroidVoiceRecognitionGateway.kt`, `VoiceBottomSheetViewModel.kt` o altri moduli DI; usare `factory` invece di `single` (romperebbe l'identità dell'istanza).

</frozen-after-approval>

## Evidence — logcat verbatim (crash-loop all'avvio, dispositivo fisico 2026-10-19 ore 12:49-12:50, ripetuto 4+ volte)

```text
10-19 12:49:57.656 26309 26309 E AndroidRuntime: FATAL EXCEPTION: main
10-19 12:49:57.656 26309 26309 E AndroidRuntime: Process: it.togo.app, PID: 26309
10-19 12:49:57.656 26309 26309 E AndroidRuntime: java.lang.RuntimeException: java.lang.reflect.InvocationTargetException
10-19 12:49:57.656 26309 26309 E AndroidRuntime: Caused by: org.koin.core.error.NoBeanDefFoundException: No definition found for type 'it.togo.app.data.voice.AndroidVoiceRecognitionGateway'. Check your Modules configuration and add missing type and/or qualifier!
10-19 12:49:57.656 26309 26309 E AndroidRuntime: 	at org.koin.core.scope.Scope.throwDefinitionNotFound(Scope.kt:302)
10-19 12:49:57.656 26309 26309 E AndroidRuntime: 	at org.koin.core.scope.Scope.get(Scope.kt:213)
10-19 12:49:57.656 26309 26309 E AndroidRuntime: 	at it.togo.app.presentation.activelist.ActiveListScreenKt.ActiveListScreen(ActiveListScreen.kt:323)
10-19 12:49:57.656 26309 26309 E AndroidRuntime: 	at it.togo.app.MainActivity$onCreate$1$1$2.invoke(MainActivity.kt:29)
```

Riprodurre con:

```powershell
& "C:\Android\platform-tools\adb.exe" logcat -d -b crash -t 100
```

> **Nota righe:** l'offset `ActiveListScreen.kt:323` è della build installata; nella working copy corrente il `koinInject` è alla riga ~84. Verificare per contenuto.

## Root cause (da verificare prima di correggere)

1. **`di/DomainModule.kt:30`** — `single<VoiceRecognitionGateway> { AndroidVoiceRecognitionGateway() }`: la definizione è registrata **solo** per il tipo interfaccia.
2. **`presentation/activelist/ActiveListScreen.kt:~84`** — `val voiceGateway = org.koin.compose.koinInject<it.togo.app.data.voice.AndroidVoiceRecognitionGateway>()`: richiede il tipo **concreto** (necessario per `registerLauncher`, riga ~91) → Koin non trova definizione → eccezione in composizione → crash immediato all'avvio.
3. Koin 3.5.6: `get<T>()` risolve per tipo esatto; per esporre più tipi sulla stessa istanza serve l'infix `bind`: `single { AndroidVoiceRecognitionGateway() } bind VoiceRecognitionGateway::class`.
4. Verificare anche che non esistano **altri** risolutori del tipo concreto oltre a quello dello screen (grep `AndroidVoiceRecognitionGateway`): `PresentationModule` usa `get<VoiceRecognitionGateway>()` (righe ~37, ~47) — resta valido con il `bind`.

## Code Map

- `app/src/main/java/it/togo/app/di/DomainModule.kt` -- **unico file da modificare**: riga 30 (registrazione gateway) + nuovo import `org.koin.dsl.bind` (blocco import righe 14-15, ordine alfabetico).
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` -- sola lettura: `koinInject` (~84), `speechLauncher` (~86), `registerLauncher` (~91), `startVoiceWithPermission` (~95) — **fix voice corretta, non toccare**.
- `app/src/main/java/it/togo/app/di/PresentationModule.kt` -- sola lettura: `get<VoiceRecognitionGateway>()` deve continuare a risolvere.

## Tasks & Acceptance

**Execution:**

- [ ] **V1 — Conferma diagnosi (obbligatoria):**
  - [ ] Verificare `DomainModule.kt:30`: registrazione con tipo esplicito `<VoiceRecognitionGateway>` e **senza** `bind`.
  - [ ] Verificare che `ActiveListScreen.kt` contenga `koinInject<...AndroidVoiceRecognitionGateway>` e che lo stato voice (launcher/permessi) sia presente e intatto.
  - [ ] `grep -rn "AndroidVoiceRecognitionGateway" app/src/main` → elenco completo dei riferimenti; verificare che nessun altro punto richieda il tipo concreto oltre allo screen.
  - [ ] Se i punti non combacciano → **non applicare la fix**: raccogliere il contrasto e riportarlo all'utente.
- [ ] **V2 — Fix (solo `DomainModule.kt`):**
  - [ ] Aggiungere l'import `org.koin.dsl.bind` nel blocco import (posizione alfabetica dopo `org.koin.core.module.Module`).
  - [ ] Sostituire la riga 30:
    ```kotlin
    single { AndroidVoiceRecognitionGateway() } bind VoiceRecognitionGateway::class
    ```
  - [ ] Non modificare altre righe del modulo né altri moduli.
- [ ] **V3 — Verifica statica (senza build):**
  - [ ] `grep` DomainModule: presente `bind VoiceRecognitionGateway::class`; `single` (non `factory`).
  - [ ] `git diff` mostra **solo** `DomainModule.kt`; tutte le modifiche utente preesistenti (5 file) **invariate**.
- [ ] **V4 — HANDOFF BUILD (obbligatorio, fermarsi qui):**
  - [ ] Comunicare all'utente: *"Fix applicata al solo DomainModule.kt. Esegui tu la build e l'installazione con: `powershell -ExecutionPolicy Bypass -File .\build-install-android.ps1`"*
  - [ ] **Attendere** la conferma dell'utente. Non procedere in autonomia.
- [ ] **V5 — Verifica post-build (SOLO dopo la conferma dell'utente):**
  - [ ] L'app **si avvia** senza crash (schermata lista visibile).
  - [ ] `adb logcat -d -b crash -t 50` → nessuna riga `it.togo.app`.
  - [ ] Flusso vocale completo: tap microfono → (se necessario) permesso → dialogo riconoscimento → parla → testo nel foglio → conferma → voce in lista.
  - [ ] VoiceFab blu: stesso comportamento; Annulla chiude il foglio.
  - [ ] Regressione lista: aggiunta manuale (FAB +), spunta checkbox, "Concludi spesa" invariati.

**Acceptance (code, tutte obbligatorie):**

1. `DomainModule.kt`: gateway registrato con `single { AndroidVoiceRecognitionGateway() } bind VoiceRecognitionGateway::class`; entrambi i tipi risolvono la stessa istanza singleton.
2. Nessuna modifica ad altri file; la fix voice in `ActiveListScreen.kt` resta intatta.
3. Diff limitato a `DomainModule.kt`; nessun commit.
4. Build, install e verifica funzionale **eseguite dall'utente**; l'agente le registra solo su sua conferma.

**Comandi VIETATI all'agente (chiedere sempre all'utente):**

```text
./gradlew*, gradlew.bat*, gradle build*, gradle assemble*, gradle test*
powershell -ExecutionPolicy Bypass -File .\build-android.ps1
powershell -ExecutionPolicy Bypass -File .\build-install-android.ps1
powershell -ExecutionPolicy Bypass -File .\test-android.ps1
docker build*, docker run*
adb install*, adb uninstall*, adb shell pm *
qualsiasi comando che compili, pacchetti o installi l'APK
```

Consentiti (sola lettura): `adb devices`, `adb logcat ...` (lette), `git status/diff/log` (lette), `grep`/`read` sui sorgenti.

**Escalation (segnalare all'utente invece di forzare):**

- Dopo la fix persistono crash con **altri** bean mancanti → raccogliere il nuovo `logcat -b crash` e riaprire diagnosi (non toccare altri moduli DI).
- `bind` non disponibile nella sintassi Koin della versione in uso → verificare la doc di `io.insert-koin:koin-android:3.5.6` (`org.koin.dsl.bind`); se realmente assente, proporre `single<VoiceRecognitionGateway> { AndroidVoiceRecognitionGateway() }` + secondo `single { get<VoiceRecognitionGateway>() as AndroidVoiceRecognitionGateway }` solo **con conferma** dell'utente.
- Il file `ActiveListScreen.kt` risulta diverso da quanto descritto (fix voice rimossa/partialmente revertata) → fermarsi e riepilogare.
- Necessità di modificare altri file → fermarsi e chiedere.
