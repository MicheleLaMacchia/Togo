---
title: 'Riparazione navigazione Storico — pulsante inerte (evento NavigateToHistory non intercettato)'
type: 'bugfix'
created: '2026-10-22'
status: 'ready'
route: 'dispatch'
baseline_commit: '20464820d3fc288803c061a22d627d8357dd0239'
review_loop_iteration: 0
context:
  - 'HEAD al momento della stesura: 2046482 "sviluppato 7.2"; working tree pulito.'
  - 'Logcat dispositivo c1582fc3, 2026-10-22 14:04:37-38: tre tap registrati da MIUIInput su MainActivity, ZERO log applicativi/eccezioni → conferma che il tap viene ricevuto ma l''azione è un no-op silenzioso.'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Il pulsante **Storico** in alto a sinistra della lista (`ActiveListScreen.kt:128`, icona orologio, `contentDescription = "Storico"`) **non fa nulla** al tap. Il percorso dell'evento si interrompe: lo screen emette correttamente `UiEvent.NavigateToHistory`, ma `AppNavigator` intercetta **solo** `NavigateToSettings`, e l'evento viene delegato a `ActiveListViewModel.onEvent`, dove il ramo `is UiEvent.NavigateToHistory -> { }` è un **no-op esplicito**. La schermata `HistoryScreen` + `HistoryViewModel` esistono già (Epic 5 / Story 5.1) e sono registrati in Koin, ma **non sono mai stati agganciati al navigatore**: l'interfaccia `Screen` conosce solo `ActiveList` e `LearnedRules`.

**Approach:** Aggiungere la rotta `History` al navigatore a stato (`AppNavigator.kt`) seguendo **esattamente** il pattern già in uso per `LearnedRules`: voce nel sealed `Screen`, persistenza nello `ScreenSaver`, intercettazione di `UiEvent.NavigateToHistory`, ramo `when` con `BackHandler` e parametro `historyContent`. In `MainActivity.kt` iniettare `HistoryViewModel` (già registrato in `PresentationModule.kt:53-57`) e passare `historyContent = { onBack -> HistoryScreen(viewModel = historyViewModel, onBack = onBack) }`. Verificare staticamente e **chiedere all'utente di eseguire build e install** — l'agente NON esegue comandi di build.

**Always:** Modificare **solo** `app/src/main/java/it/togo/app/AppNavigator.kt` e `app/src/main/java/it/togo/app/MainActivity.kt`; rispettare il pattern esistente (`rememberSaveable` + `ScreenSaver` + `SaveableStateHolder`, nessuna nuova libreria/navigazione); mantenere il ViewModel lazy (`by inject()`, non eager) per non introdurre lavoro all'avvio; preservare KDoc e stile del progetto.

**Never:** **Eseguire comandi di build, install o test Gradle/Docker** (elenco in sezione dedicata — chiedere sempre all'utente); committare; `git checkout`/reset/stash; modificare `ActiveListScreen.kt`, `ActiveListViewModel.kt`, `HistoryScreen.kt`, `HistoryViewModel.kt` o i moduli DI; aggiungere dipendenze (Navigation-Compose, ecc.); introdurre `NavHost` o cambiare l'architettura di navigazione esistente; "fixare" il no-op del ViewModel rimuovendolo o lanciando eccezioni.

</frozen-after-approval>

## Evidence

### 1) Comportamento sul dispositivo (2026-10-22)

Tap ripetuti sull'icona Storico — il sistema riceve i tocchi, l'app non produce **nessun** log né eccezione (no-op silenzioso):

```text
10-22 14:04:37.089  20598  20598 I MIUIInput: [MotionEvent] ViewRootImpl windowName 'it.togo.app/it.togo.app.MainActivity', { action=ACTION_DOWN, ... }
10-22 14:04:37.148  20598  20598 I MIUIInput: [MotionEvent] ... { action=ACTION_UP, ... }
10-22 14:04:37.563  20598  20598 I MIUIInput: [MotionEvent] ... { action=ACTION_DOWN, ... }
10-22 14:04:37.614  20598  20598 I MIUIInput: [MotionEvent] ... { action=ACTION_UP, ... }
10-22 14:04:38.299  20598  20598 I MIUIInput: [MotionEvent] ... { action=ACTION_DOWN, ... }
10-22 14:04:38.368  20598  20598 I MIUIInput: [MotionEvent] ... { action=ACTION_UP, ... }
```

Nessun `FATAL EXCEPTION`, nessun log di navigazione → il tap arriva, la navigazione non viene nemmeno tentata.

### 2) Catena dell'evento nel sorgente

| # | Punto | Codice | Esito |
|---|-------|--------|-------|
| 1 | `ActiveListScreen.kt:128` | `IconButton(onClick = { onEvent(UiEvent.NavigateToHistory) })` | evento emesso ✓ |
| 2 | `AppNavigator.kt:56-62` | `if (event is UiEvent.NavigateToSettings) { ... } else { onActiveListEvent(event) }` | `NavigateToHistory` **non** intercettato → delega |
| 3 | `ActiveListViewModel.kt:176` | `is UiEvent.NavigateToHistory -> { }` | **no-op** → nessuna navigazione |

### 3) La schermata esiste già, ma è orfana

- `HistoryScreen.kt:54-57` → `fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit)`; ha già `TopAppBar` con freccia indietro (`onBack`) e `SnackbarHost`.
- `HistoryViewModel.kt:27-33` + `init` (righe 40-45): carica lo storico con `combine(...)` all'istanziazione.
- `PresentationModule.kt:53-57` → `viewModel { HistoryViewModel(...) }` **già registrato**.
- Nessun riferimento a `HistoryScreen` fuori dal suo file → conferma che manca solo il cablaggio.

## Root cause (da verificare prima di correggere)

1. **`AppNavigator.kt:18-21`** — `private sealed interface Screen` contiene solo `ActiveList` e `LearnedRules`: non esiste alcuna rotta per lo Storico.
2. **`AppNavigator.kt:24-27`** — `ScreenSaver` sa salvare solo `"learned_rules"` o `"active_list"`.
3. **`AppNavigator.kt:56-62`** — intercetta solo `UiEvent.NavigateToSettings`; ogni altro evento va al ViewModel della lista.
4. **`ActiveListViewModel.kt:176`** — `is UiEvent.NavigateToHistory -> { }`: ramo vuoto (stessa scelta di `NavigateToVoice`/`NavigateToSettings`, che sono gestiti a monte dal navigatore). **Non è un bug da fixare nel ViewModel**: è la conseguenza del punto 3. Il no-op va lasciato.
5. **`MainActivity.kt:26-41`** — `AppNavigator` riceve solo `activeListContent` e `learnedRulesContent`: nessun contenuto per lo Storico.

## Code Map

- `app/src/main/java/it/togo/app/AppNavigator.kt` -- **da modificare**: sealed `Screen` (18-21), `ScreenSaver` (24-27), KDoc (30-41), firma `AppNavigator` (42-47), `when (currentScreen)` (54-69).
- `app/src/main/java/it/togo/app/MainActivity.kt` -- **da modificare**: import (6-14), campi iniettati (18-20), chiamata `AppNavigator` (26-41).
- `app/src/main/java/it/togo/app/presentation/history/HistoryScreen.kt` -- sola lettura: firma `HistoryScreen(viewModel, onBack)` (54-57), back arrow (76-83).
- `app/src/main/java/it/togo/app/presentation/history/HistoryViewModel.kt` -- sola lettura: costruttore + `init` (27-45).
- `app/src/main/java/it/togo/app/di/PresentationModule.kt:53-57` -- sola lettura: `HistoryViewModel` già registrato; nessuna modifica DI necessaria.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt:176` -- sola lettura: il no-op resta invariato.

## Tasks & Acceptance

**Execution:**

- [ ] **V1 — Conferma diagnosi (obbligatoria):**
  - [ ] `grep -rn "NavigateToHistory" app/src/main` → 3 occorrenze: definizione (`ActiveListUiState.kt:91`), emissione (`ActiveListScreen.kt:128`), no-op (`ActiveListViewModel.kt:176`). **Nessuna gestione nel navigatore.**
  - [ ] `grep -rn "HistoryScreen" app/src/main` → solo la definizione: nessun call site.
  - [ ] Verificare la firma `HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit)` e la registrazione Koin `PresentationModule.kt:53-57`.
  - [ ] Verificare che `Screen` (`AppNavigator.kt:18-21`) non contenga `History` e che lo `ScreenSaver` gestisca solo due valori.
  - [ ] Se i punti non combacciano → **non applicare la fix**: raccogliere il contrasto e riportarlo all'utente.
- [ ] **V2 — Fix `AppNavigator.kt`:**
  - [ ] Aggiungere la rotta: `data object History : Screen` accanto a `LearnedRules`.
  - [ ] Estendere `ScreenSaver` a tre valori (`"history"` incluso, sia in `save` sia in `restore`).
  - [ ] Aggiungere il parametro `historyContent: @Composable (() -> Unit) -> Unit` (dopo `learnedRulesContent`) e aggiornare il KDoc con la nuova rotta.
  - [ ] Intercettare l'evento nel blocco della lista:
    ```kotlin
    activeListContent { event ->
        when (event) {
            is UiEvent.NavigateToSettings -> currentScreen = Screen.LearnedRules
            is UiEvent.NavigateToHistory -> currentScreen = Screen.History
            else -> onActiveListEvent(event)
        }
    }
    ```
  - [ ] Aggiungere il ramo, sul modello di `LearnedRules`:
    ```kotlin
    Screen.History -> stateHolder.SaveableStateProvider("history") {
        BackHandler { currentScreen = Screen.ActiveList }
        historyContent { currentScreen = Screen.ActiveList }
    }
    ```
- [ ] **V3 — Fix `MainActivity.kt`:**
  - [ ] Import `it.togo.app.presentation.history.HistoryScreen` e `it.togo.app.presentation.history.HistoryViewModel`.
  - [ ] `private val historyViewModel: HistoryViewModel by inject()` (lazy, come gli altri; **non** `get()` eager).
  - [ ] Passare al navigatore:
    ```kotlin
    historyContent = { onBack ->
        HistoryScreen(
            viewModel = historyViewModel,
            onBack = onBack,
        )
    },
    ```
  - [ ] Non toccare null'altro (lista, voce, regole apprese restano identici).
- [ ] **V4 — Verifica statica (senza build):**
  - [ ] `grep` su `AppNavigator.kt`: presenti `History`, `"history"`, `NavigateToHistory`, `historyContent`.
  - [ ] `grep` su `MainActivity.kt`: presenti `HistoryViewModel`, `historyContent`, `HistoryScreen`.
  - [ ] `git diff --stat` → **solo 2 file**; nessun'altra modifica.
- [ ] **V5 — HANDOFF BUILD (obbligatorio, fermarsi qui):**
  - [ ] Comunicare all'utente: *"Fix applicata a AppNavigator.kt e MainActivity.kt. Esegui tu la build e l'installazione con: `powershell -ExecutionPolicy Bypass -File .\build-install-android.ps1`"*
  - [ ] **Attendere** la conferma dell'utente. Non procedere in autonomia.
- [ ] **V6 — Verifica funzionale post-build (SOLO dopo la conferma dell'utente):**
  - [ ] Tap sull'icona orologio in alto a sinistra → si apre la schermata **"Storico"** (non più nulla).
  - [ ] La schermata mostra: campo "Cerca nel storico", elenco delle spese concluse (o stato vuoto) e freccia indietro in alto a sinistra.
  - [ ] Freccia indietro → torna alla lista; **tasto back di sistema/gesture** → torna alla lista (non chiude l'app).
  - [ ] Rotazione del dispositivo sulla schermata Storico → resta su Storico (persistenza `ScreenSaver`).
  - [ ] Regressione: "Regole apprese" (ingranaggio) si apre e torna indietro come prima; FAB "+", spunta voci, "Concludi spesa", flusso vocale invariati.
  - [ ] Opzionale: `adb logcat -d -t 50` dopo il tap → nessuna eccezione.

**Acceptance (code, tutte obbligatorie):**

1. `AppNavigator.kt`: `Screen.History` presente, `ScreenSaver` gestisce `"history"`, `NavigateToHistory` intercettato, ramo `History` con `BackHandler` + `SaveableStateProvider("history")`.
2. `MainActivity.kt`: `HistoryViewModel` iniettato lazy e `historyContent` passato a `AppNavigator`.
3. `ActiveListViewModel.kt` **invariato** (il no-op resta: ora è irraggiungibile perché l'evento è intercettato a monte).
4. Nessuna modifica a schermate/ViewModel/DI; nessuna nuova dipendenza; `git diff` limitato a 2 file; nessun commit.
5. Build, install e verifica funzionale **eseguite dall'utente**; l'agente le registra solo su sua conferma.

**Comandi VIETATI all'agente (chiedere sempre all'utente):**

```text
./gradlew*, gradlew.bat*, gradle build*, gradle assemble*, gradle test*
powershell -ExecutionPolicy Bypass -File .\build-android.ps1
powershell -ExecutionPolicy Bypass -File .\build-install-android.ps1
powershell -ExecutionPolicy Bypass -File .\test-android.ps1
docker build*, docker run*
adb install*, adb uninstall*, adb shell pm *
adb shell input tap/swipe/text/keyevent (solo se l'utente lo autorizza esplicitamente)
qualsiasi comando che compili, pacchetti o installi l'APK
```

Consentiti (sola lettura): `adb devices`, `adb logcat ...` (lette), `git status/diff/log` (lette), `grep`/`read` sui sorgenti.

**Escalation (segnalare all'utente invece di forzare):**

- `HistoryScreen` richiede parametri diversi da `(viewModel, onBack)` → fermarsi e riportare la firma reale; **non** modificare la schermata.
- Il codice di `AppNavigator.kt` risulta strutturalmente diverso (es. navigazione migrata a Navigation-Compose) → fermarsi e riepilogare prima di procedere.
- All'apertura dello Storico compaiono crash relativi a `HistoryViewModel`/`HistoryRepository` (es. bean Koin mancante) → raccogliere `adb logcat -d -b crash -t 100` e riportare; **non** toccare i moduli DI in questa passata.
- Serve modificare schermate, ViewModel, DI o aggiungere dipendenze → fermarsi e chiedere.
