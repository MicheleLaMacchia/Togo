---
title: 'Riparazione crash AddItemBottomSheet (tasto +)'
type: 'bugfix'
created: '2026-10-19'
status: 'ready'
route: 'dispatch'
baseline_commit: '8c801168f994f6c9cb0dab3dc8b58fd8bc2a4880'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Toccando il pulsante **"+"** (FloatingActionButton "Aggiungi voce") nella schermata lista attiva, l'app si chiude immediatamente con crash (FATAL EXCEPTION). Il log dimostra che la funzionalità **esiste** ma non riesce a istanziare `AddItemViewModel`: la composable usa la factory ViewModel di default (che richiede costruttore senza argomenti) mentre la classe ha dipendenze a costruttore e **non è registrata in Koin**.

**Approach:** Verificare la diagnosi sui file indicati, correggere la creazione del ViewModel usando Koin (dipendenza già presente), rebuild dell'APK via Docker, reinstall sul dispositivo collegato e verifica empirica che il crash non si ripresenti leggendo `logcat -b crash`.

**Always:** Rispettare Clean Architecture e le convenzioni Koin/Compose esistenti; intervenire solo sulle cause verificate; mantenere il comportamento UI invariato; lasciare le modifiche non committate per revisione umana.

**Never:** Committare; applicare fix "a silenzio" (es. `try/catch` che nasconde il crash, `TODO()`, rimozione della schermata); modificare logica di business non coinvolta; rimuovere `forInsert`/`forEdit` o la modalità EDIT.

</frozen-after-approval>

## Evidence — log di errore (verbatim)

Raccolti il 2026-10-19 ore 09:58 da un dispositivo fisico (ADB `c1582fc3`), APK `app-debug.apk` build 09:51, package `it.togo.app`. Il crash si è ripetuto **due volte** (09:58:06 e 09:58:34) con stack identico.

```text
10-19 09:58:34.236 31541 31541 E AndroidRuntime: FATAL EXCEPTION: main
10-19 09:58:34.236 31541 31541 E AndroidRuntime: Process: it.togo.app, PID: 31541
10-19 09:58:34.236 31541 31541 E AndroidRuntime: java.lang.RuntimeException: Cannot create an instance of class
        it.togo.app.presentation.additem.AddItemViewModel
10-19 09:58:34.236 31541 31541 E AndroidRuntime: 	at androidx.lifecycle.viewmodel.internal.JvmViewModelProviders.createViewModel(JvmViewModelProviders.kt:40)
10-19 09:58:34.236 31541 31541 E AndroidRuntime: 	at androidx.lifecycle.ViewModelProvider$NewInstanceFactory.create(ViewModelProvider.android.kt:193)
        ...
10-19 09:58:34.236 31541 31541 E AndroidRuntime: 	at it.togo.app.presentation.additem.AddItemBottomSheetKt.AddItemBottomSheet(AddItemBottomSheet.kt:451)
10-19 09:58:34.236 31541 31541 E AndroidRuntime: 	at it.togo.app.presentation.activelist.ActiveListScreenKt.ActiveListScreen(ActiveListScreen.kt:72)
        ...
10-19 09:58:34.236 31541 31541 E AndroidRuntime: Caused by: java.lang.NoSuchMethodException:
        it.togo.app.presentation.additem.AddItemViewModel.<init> []
10-19 09:58:34.236 31541 31541 E AndroidRuntime: 	at java.lang.Class.getConstructor0(Class.java:3387)
10-19 09:58:34.236 31541 31541 E AndroidRuntime: 	at java.lang.Class.getDeclaredConstructor(Class.java:3069)
10-19 09:58:34.236 31541 31541 E AndroidRuntime: 	at androidx.lifecycle.viewmodel.internal.JvmViewModelProviders.createViewModel(JvmViewModelProviders.kt:38)
```

Riprodurre con:

```powershell
& "C:\Android\platform-tools\adb.exe" logcat -d -b crash -t 200
```

> **Nota righe:** gli offset `AddItemBottomSheet.kt:451` e `ActiveListScreen.kt:72` nello stack referenziano la build APK del 09:51, non necessariamente la sorgente corrente. Individuare i punti giusti **per contenuto**, non per numero di riga.

## Root cause (da verificare prima di correggere)

1. **`presentation/additem/AddItemBottomSheet.kt` (riga 78, call site `viewModel<AddItemViewModel>()`)**
   usa `androidx.lifecycle.viewmodel.compose.viewModel<AddItemViewModel>()` senza factory → cade su `NewInstanceFactory` (visibile nello stack) che richiede costruttore no-arg.
2. **`presentation/additem/AddItemViewModel.kt:39`** — `class AddItemViewModel(catalogRepository, shoppingRepository, item = null)`: ha argomenti a costruttore.
3. **`di/PresentationModule.kt`** — registra `MainViewModel`, `ActiveListViewModel`, `SearchViewModel`, `VoiceBottomSheetViewModel`, `VoiceConfirmationViewModel`, `VoiceResilienceViewModel`, `HistoryViewModel`, `LearnedRulesViewModel` ma **NON** `AddItemViewModel`.
4. Le factory manuali `AddItemViewModel.forInsert` / `forEdit` (righe 304-317) **non sono chiamate da nessuna parte** (dead code): la modalità EDIT dichiarata nella KDoc di `AddItemBottomSheet` non è supportata dall'istanziazione corrente.

### Bug gemello (stessa causa, stessa fix)

- **`presentation/search/SearchScreen.kt:57`** — `viewModel<SearchViewModel>()` con lo stesso pattern; `SearchViewModel(catalogRepository)` ha argomenti. Oggi `SearchScreen` non è raggiungibile dalla UI (nessun call site), quindi non crasha ancora, ma crasherebbe appena navigato. Correggere anche questo.

## Code Map

- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` -- righe 69-76: `showAddItemSheet` + chiamata `AddItemBottomSheet(...)` (senza `isEditMode`, quindi sempre INSERT); riga 149: FAB "+".
- `app/src/main/java/it/togo/app/presentation/additem/AddItemBottomSheet.kt` -- riga 78: **punto di crash**, `viewModel<AddItemViewModel>()` con factory di default.
- `app/src/main/java/it/togo/app/presentation/additem/AddItemViewModel.kt` -- riga 39: costruttore con dipendenze; righe 304-317: companion `forInsert`/`forEdit` non usate.
- `app/src/main/java/it/togo/app/di/PresentationModule.kt` -- manca la registrazione `viewModel { AddItemViewModel(...) }`.
- `app/src/main/java/it/togo/app/presentation/search/SearchScreen.kt` -- riga 57: stesso pattern difettoso (bug gemello).
- `gradle/libs.versions.toml` + `app/build.gradle.kts` -- `libs.koin.compose` = `io.insert-koin:koin-androidx-compose:3.5.6` **già presente**: espone `org.koin.androidx.compose.koinViewModel()`.

## Tasks & Acceptance

**Execution:**

- [ ] **V1 — Conferma diagnosi (obbligatoria, non saltare):**
  - [ ] `adb devices` elenca almeno un dispositivo in stato `device` (se vuoto: fermo e segnalare all'utente, non procedere).
  - [ ] `& "C:\Android\platform-tools\adb.exe" logcat -d -b crash -t 200` riporta `Cannot create an instance of class it.togo.app.presentation.additem.AddItemViewModel` (almeno uno storico).
  - [ ] Verificare per contenuto: `viewModel<AddItemViewModel>()` in `AddItemBottomSheet.kt`; costruttore con argomenti in `AddItemViewModel.kt`; **assenza** di `AddItemViewModel` in `di/PresentationModule.kt`.
  - [ ] Verificare che `koin-androidx-compose` risulti in `gradle/libs.versions.toml`.
  - [ ] Se uno di questi punti NON combacia, **non applicare la fix**: raccogliere il contrasto e riportarlo all'utente.
- [ ] **V2 — Fix:**
  - [ ] Registrare in `di/PresentationModule.kt` (seguendo lo stile delle altre registrazioni):
    ```kotlin
    viewModel { AddItemViewModel(
        catalogRepository = get<CatalogRepository>(),
        shoppingRepository = get<ShoppingListRepository>(),
    ) }
    ```
  - [ ] In `AddItemBottomSheet.kt` sostituire la creazione con `val viewModel = koinViewModel<AddItemViewModel>()` (import `org.koin.androidx.compose.koinViewModel`).
  - [ ] Applicare la stessa correzione in `SearchScreen.kt` (`koinViewModel<SearchViewModel>()`).
  - [ ] Non toccare `forInsert`/`forEdit`, la KDoc modalità EDIT né altro codice.
- [ ] **V3 — Build + install:**
  - [ ] `powershell -ExecutionPolicy Bypass -File .\build-install-android.ps1` dalla root del repo (serve Docker/Rancher Desktop attivo e immagine `android-builder`; build `clean assembleDebug` + `adb install -r`).
  - [ ] Build deve terminare con `Build e installazione completate.` — in caso di errore Gradle: fermarsi e riportare il log COMPLETO, senza "aggiustare" altri file.
- [ ] **V4 — Verifica sul dispositivo:**
  - [ ] `& "C:\Android\platform-tools\adb.exe" logcat -c` (svuota il buffer).
  - [ ] Avviare l'app: `& "C:\Android\platform-tools\adb.exe" shell monkey -p it.togo.app -c android.intent.category.LAUNCHER 1`.
  - [ ] Cliccare il FAB "+" e verificare che il foglio resti aperto. Verifica automatica del tap (fallback manuale):
    ```powershell
    $adb = "C:\Android\platform-tools\adb.exe"
    & $adb shell uiautomator dump /sdcard/ui.xml
    $xml = & $adb shell cat /sdcard/ui.xml
    # estrarre bounds del nodo con content-desc "Aggiungi voce" e calcolare il centro,
    # poi: & $adb shell input tap X Y
    ```
  - [ ] Dopo il tap: `& $adb logcat -d -b crash -t 50` → **nessuna riga** contenente `it.togo.app` (in assenza di output = PASS).
  - [ ] Confermare che il foglio mostra il campo di ricerca dello step 1 (testo atteso tipo "ricerca"/"prodotto"; in alternativa `uiautomator dump` e cercare il nodo).
- [ ] **V5 — Regressione:**
  - [ ] Chiudere il foglio, aprire di nuovo il "+", ripetere il check crash (seconda apertura senza residui di stato).
  - [ ] Lasciare le modifiche **non committate** (`git status` deve mostrare i file modificati).

**Acceptance (tutti obbligatori):**

1. Nessun `FATAL EXCEPTION`/`AndroidRuntime` relativo a `it.togo.app` in `logcat -b crash` dopo apertura del foglio "Aggiungi voce".
2. Il bottom sheet di inserimento resta aperto e rende interattivo lo step 1 (ricerca prodotto).
3. `SearchScreen.kt` usa `koinViewModel` e il modulo Koin include `AddItemViewModel`.
4. Build `assembleDebug` riuscita; APK installato sul dispositivo.
5. Nessun commit, nessuna modifica fuori dai file elencati in Code Map.

**Escalation (segnalare all'utente invece di forzare):**

- Dispositivo non visibile in `adb devices`, Docker/immagine `android-builder` assente.
- Build fallita per cause non collegate al fix.
- Il crash persiste con stack diverso da quello documentato (nuova diagnosi richiesta).
- Individuato un vero usage della modalità EDIT (`isEditMode = true`) che richiede parametri non registrabili in Koin statico → proporre soluzione `parametersOf(...)` e attendere conferma.
