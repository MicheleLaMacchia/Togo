---
title: 'Riparazione flusso vocale — registerLauncher() mai chiamato, audio mai avviato'
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

**Problem:** Toccando uno dei due microfoni e concedendo il permesso RECORD_AUDIO, l'app mostra l'errore *"Launcher non registrato. Chiamare registerLauncher() prima di startListening()"*. "Riprova" mostra brevemente "ascolto in corso" ma **l'audio non arriva mai**: l'Intent di riconoscimento del sistema non viene lanciato. Causa: il presentation layer **non ha mai cablato** il pattern previsto dalla spec 4-1 — nessuno chiama `AndroidVoiceRecognitionGateway.registerLauncher()` (launcher sempre `null`) e nessuno collega il callback `onActivityResult` che restituisce il testo riconosciuto. Inoltre il VoiceFab (microfono blu) avvia l'ascolto **senza verificare il permesso** RECORD_AUDIO.

**Approach:** In `ActiveListScreen.kt` creare con `rememberLauncherForActivityResult(StartActivityForResult)` un launcher la cui callback invochi `gateway.onActivityResult(result.resultCode, result.data)`, registrarli sul gateway singleton con `registerLauncher`/`LaunchedEffect`, e uniformare il VoiceFab allo stesso flusso permessi già usato dall'EmptyState. Verificare staticamente e **chiedere all'utente di eseguire build e install** — l'agente NON esegue comandi di build.

**Always:** Modificare **solo** `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt`, e **solo nei blocchi relativi alla voce** (creazione launcher, registrazione gateway, permesso microfono); preservare tutte le altre modifiche presenti nel working tree di quel file e degli altri; mantenere invariati `AndroidVoiceRecognitionGateway.kt`, `VoiceBottomSheetViewModel.kt` e i moduli DI.

**Never:** **Eseguire comandi di build, install o test Gradle/Docker** (elenco in sezione dedicata — chiedere sempre all'utente); committare; `git checkout`/reset/stash su file modificati dall'utente (`AndroidManifest.xml`, `ActiveListScreen.kt`, `CategoryHeader.kt`, `ItemRow.kt`, `PresiSection.kt`); modificare il dominio (`VoiceRecognitionGateway` interface) per aggiungere tipi Android; introdurre cast `(gateway as? AndroidVoiceRecognitionGateway)` nel presentation layer se si può iniettare il tipo concreto da Koin.

</frozen-after-approval>

## Evidence (logcat verbatim — errore riprodotto dall'utente 2026-10-19 ore ~12:06)

Il messaggio è prodotto dal gateway (stato `Error(NOT_AVAILABLE)` mostrato dal bottom sheet):

```text
Errore
Launcher non registrato. Chiamare registerLauncher() prima di startListening().
[Annulla] [Riprova]
```

Origine del testo — `data/voice/AndroidVoiceRecognitionGateway.kt:68-75`:

```kotlin
val l = launcher
if (l == null) {
    _state.value = VoiceRecognitionState.Error(
        ErrorCode.NOT_AVAILABLE,
        "Launcher non registrato. Chiamare registerLauncher() prima di startListening()."
    )
    return
}
```

Verifica del mancato cablaggio: `grep -r "registerLauncher" app/src/main` → **solo** la definizione in `AndroidVoiceRecognitionGateway.kt:52` e i KDoc; **zero call site**. Idem `onActivityResult(gateway)` (definito a riga 115, mai invocato).

## Root cause (da verificare prima di correggere)

1. **`AndroidVoiceRecognitionGateway.kt:52,68-75`** — singleton Koin (`di/DomainModule.kt:30`: `single<VoiceRecognitionGateway> { AndroidVoiceRecognitionGateway() }`) con `private var launcher: ActivityResultLauncher<Intent>? = null`. Nessun call site di `registerLauncher` → sempre null → ogni `startListening()` termina con `Error`.
2. **`VoiceBottomSheetViewModel.kt:49-53,74-81`** — `startListening()`/`retry()` delegano al gateway; `retry()` imposta `_uiState = Listening` **ottimisticamente** (riga 76) prima del responso del gateway → l'UI mostra "ascolto in corso" anche quando il gateway sta fallendo. (Effetto collaterale del bug principale; NON modificare il ViewModel in questa fix.)
3. **`ActiveListScreen.kt:77-81`** — esiste già `audioPermissionLauncher` (`RequestPermission`) con callback `if (granted) voiceViewModel.startListening()`: flusso corretto per l'EmptyState (righe ~217-226).
4. **`ActiveListScreen.kt:140-146` (blocco VoiceFab)** — `onClick` chiama `voiceViewModel.startListening()` **senza alcun check del permesso**: via bypass del flusso permessi.
5. **Mancanza totale del launcher di risultato**: per ricevere il testo riconosciuto serve un launcher `ActivityResultContracts.StartActivityForResult()` la cui callback chiami `gateway.onActivityResult(result.resultCode, data)` e che venga passato a `gateway.registerLauncher(...)` in composizione (`LaunchedEffect`), considerando che il gateway è un singleton e va ri-registrato a ogni ricomposizione/ricreazione.

## Code Map

- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` -- **unico file da modificare**: blocco permessi/voice verso le righe 74-101 (dopo `audioPermissionLauncher`), blocco VoiceFab verso le righe 132-147, `EmptyState.onVoice` verso le righe 217-226. Gli offset sono della working copy corrente: verificare per contenuto.
- `app/src/main/java/it/togo/app/data/voice/AndroidVoiceRecognitionGateway.kt` -- sole lettura: `registerLauncher` (52), check null (68-75), `onActivityResult` (115).
- `app/src/main/java/it/togo/app/presentation/voice/VoiceBottomSheetViewModel.kt` -- sola lettura: `retry()` 74-81, mappatura stati 29-46.
- `app/src/main/java/it/togo/app/di/DomainModule.kt:30` -- sola lettura: binding singleton del gateway.
- Dipendenza disponibile: `libs.koin.compose` = `io.insert-koin:koin-androidx-compose:3.5.6` → `org.koin.compose.koinInject`.

## Tasks & Acceptance

**Execution:**

- [ ] **V1 — Conferma diagnosi (obbligatoria):**
  - [ ] `grep -rn "registerLauncher" app/src/main` → solo definizione/KDoc nel gateway, zero call site.
  - [ ] `grep -rn "onActivityResult" app/src/main` → solo definizione nel gateway.
  - [ ] Verificare per contenuto: `audioPermissionLauncher` con `RequestPermission` (ActiveListScreen); VoiceFab `onClick` senza check permesso; `retry()` con Listening ottimistico (sola lettura).
  - [ ] `git status`: i file utente modificati vanno preservati; la fix toccherà solo i blocchi voice di `ActiveListScreen.kt`.
  - [ ] Se i punti non combacciano → **non applicare la fix**: raccogliere il contrasto e riportarlo all'utente.
- [ ] **V2 — Fix (solo `ActiveListScreen.kt`):**
  - [ ] Iniettare il gateway concreto in composizione: `val voiceGateway = koinInject<it.togo.app.data.voice.AndroidVoiceRecognitionGateway>()` (import `org.koin.compose.koinInject`).
  - [ ] Creare il launcher di risultato vicino ad `audioPermissionLauncher`:
    ```kotlin
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        voiceGateway.onActivityResult(result.resultCode, result.data)
    }
    LaunchedEffect(voiceGateway) { voiceGateway.registerLauncher(speechLauncher) }
    ```
    (aggiungere import `LaunchedEffect` se mancanti; `ActivityResultContracts` e `Intent` sono già importati).
  - [ ] Estrarre un lambda condiviso di avvio con permesso, usato da **entrambi** i microfoni:
    ```kotlin
    val startVoiceWithPermission: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) voiceViewModel.startListening()
        else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    ```
    - `EmptyState.onVoice` → `startVoiceWithPermission()` (sostituire il corpo inline corrente, comportamento identico).
    - VoiceFab `onClick` → mantenere il ramo `cancel` per lo stato Listening, altrimenti `startVoiceWithPermission()`.
  - [ ] Non toccare: VoiceBottomSheetViewModel, gateway, DI, logica condivisione/sheet/lista e ogni altra parte del file.
- [ ] **V3 — Verifica statica (senza build):**
  - [ ] `grep` in `ActiveListScreen.kt`: presenti `koinInject`, `StartActivityForResult`, `registerLauncher`, `onActivityResult`; **un solo** corpo di check permesso (il lambda condiviso).
  - [ ] `git diff` di `ActiveListScreen.kt`: modifiche **solo** nei blocchi voice (launcher, lambda, onClick VoiceFab, onVoice); le altre differenze utente preesistenti **invariate** (non revertate).
- [ ] **V4 — HANDOFF BUILD (obbligatorio, fermarsi qui):**
  - [ ] Comunicare all'utente: *"Fix applicata al solo ActiveListScreen.kt (blocchi voice). Esegui tu la build e l'installazione con: `powershell -ExecutionPolicy Bypass -File .\build-install-android.ps1`"*
  - [ ] **Attendere** la conferma dell'utente. Non procedere in autonomia.
- [ ] **V5 — Verifica funzionale post-build (SOLO dopo la conferma dell'utente):**
  - [ ] Lista vuota → tap microfono centrale → al primo uso compare il dialogo permesso → concedi → **compare il dialogo di riconoscimento del sistema** ("Dimmi un prodotto…").
  - [ ] Parla → il foglio mostra il testo riconosciuto (stato Ready) → conferma → il comando entra nella lista.
  - [ ] Tocca "Riprova" → riapre il dialogo di riconoscimento.
  - [ ] VoiceFab blu: senza permesso → dialogo permesso (non più errore); con permesso → dialogo riconoscimento.
  - [ ] Regressione: Annulla chiude il foglio; flusso di condivisione e lista invariati; lista non vuota → microfono blu funziona.

**Acceptance (code, tutte obbligatorie):**

1. `speechLauncher` creato con `StartActivityForResult`; callback → `gateway.onActivityResult(resultCode, data)`; `registerLauncher` invocato in composizione via `LaunchedEffect`.
2. Entrambi i microfoni passano dallo stesso controllo permesso condiviso.
3. Nessuna modifica a ViewModel/gateway/DI/dominio; `retry()` invariato.
4. Diff limitato ai blocchi voice di `ActiveListScreen.kt` + le modifiche utente preesistenti intatte; nessun commit.
5. Build, install e verifica funzionale **eseguite dall'utente**; l'agente le registra solo su sua conferma.

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

- `koinInject` non utilizzabile nel contesto attuale → proporre l'alternativa (esporre `registerLauncher`/`onActivityResult` sul `VoiceBottomSheetViewModel` come delega al gateway concreto) e attendere conferma.
- Il file `ActiveListScreen.kt` dell'utente è strutturalmente diverso da quanto descritto (blocchi spostati) → fermarsi e rirepilogare le differenze prima di procedere.
- Comportamento residuo di "Riprova" (transito Listening→Hidden→Error visibile) segnalato dall'utente → aprire issue separata su `VoiceBottomSheetViewModel.retry()`, non fixare in questa passata.
- Serve toccare altri file (gateway, DI, dominio) → fermarsi e chiedere.
