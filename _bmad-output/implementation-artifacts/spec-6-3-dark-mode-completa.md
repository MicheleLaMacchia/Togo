---
title: '6.3 Dark Mode Completa'
type: 'feature'
created: '2026-10-21'
status: 'done'
review_loop_iteration: 1
route: 'dispatch'
baseline_commit: '0677b969eef9d440fde8f0003ba295c4f7708fc2'
context:
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-6-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-6-2-target-touch-e-dynamic-type.md'
---

## Story

Come utente che usa l'app di sera o in ambienti scuri,
voglio che TOGO supporti la Dark Mode di sistema preservando leggibilità e identità visiva,
così che l'app non abbagli e rispetti la batteria del dispositivo OLED.

**Acceptance Criteria:**

**Given** la modalità Dark attiva nelle impostazioni di sistema Android
**When** l'app viene aperta o passa in background e torna in foreground
**Then** il tema Dark viene applicato automaticamente: `surface-base-dark #0B0F17`, `surface-card-dark #131B2E`, `ink-primary-dark #F8FAFC`, `ink-secondary-dark #94A3B8`, `border-crisp-dark #64748B`, `accent-highlight-dark #60A5FA`
**And** il rapporto di contrasto testo/sfondo per `ink-primary-dark` su `surface-base-dark` è ≥7:1
**And** tutti i componenti (ItemRow, CategoryHeader, VoiceBottomSheet, DuplicateResolutionDialog, FAB) mostrano i token dark corretti
**And** passando dalla modalità Light a Dark e viceversa non si verificano crash o stati inconsistenti della UI

<frozen-after-approval reason="intento deciso dall'utente — non modificare senza rinegoziazione">

## Intent

**Problema:** la palette dark esiste già in `Color.kt` e `TogoTheme(darkTheme)` la applica, ma il tema finestra è solo light (`themes.xml` con parent `android:Theme.Material.Light.NoActionBar`, nessun `values-night`): al cold start e nelle system bar resta uno sfondo chiaro. Inoltre alcuni componenti bypassano i token: `VoiceFab` usa `Color.White` (2,54:1 su `#60A5FA` in dark) e un overlay con scrim bianco in dark; il pulsante "Concludi spesa" usa `inkInverse` su verde condiviso (3,56:1 in dark); l'highlight di ricerca è hardcoded `#2563EB` senza colore testo; `DuplicateResolutionDialog` usa `MaterialTheme.colorScheme` (surface Material `#131B2E` invece di `surfaceBase #0B0F17`). L'azione "Annulla" della snackbar usa `accentHighlight`, illeggibile in dark (2,54:1 su bianco).

**Approccio:** aggiungere la variante `values-night` del tema finestra con `windowBackground` allineato ai token; sostituire i colori hardcoded con token TOGO o con coppie tema-consapevoli documentate; migrare il dialog ai token del componente; correggere i contrasti delle coppie derivate (checkout, FAB idle, overlay, snackbar, highlight); blindare il tutto con test di contrasto, test di analisi statica e test Compose in dark. Nessuna modifica a dominio, ViewModel, navigazione o semantiche 6.1.

## Boundaries & Constraints

**Sempre:** usare `togoColors()` / `togoComponentTokens()` / `TogoColorTokens`; mantenere le semantiche TalkBack della 6.1 e i touch target della 6.2; lasciare le stringhe in italiano; scrivere test JVM/Robolectric in `app/src/test`; documentare ogni eventuale colore fisso motivato (es. testo su backdrop scuro condiviso); verificare i contrasti WCAG (≥7:1 per testo primario, ≥4,5:1 per testo normale e azioni, ≥3:1 per elementi non testuali).

**Mai:** non eseguire `gradlew`, `.\build-android.ps1`, `.\build-install-android.ps1`, `.\test-android.ps1`, Docker o `adb`; non committare; non aggiungere librerie né cambiare versioni Gradle/Compose/Material3; non modificare dominio, persistenza, navigazione o ViewModel; non alterare annunci/azioni della 6.1 né i minimi 48dp della 6.2; non toccare `gradlew`.

## I/O & Edge-Case Matrix

| Scenario | Input / Stato | Risultato / comportamento atteso | Gestione errori |
|----------|--------------|---------------------------|----------------|
| Cold start in dark | Sistema in Dark Mode, app chiusa | Finestra con `windowBackground #0B0F17`, nessun flash bianco; `TogoTheme(darkTheme = true)` applica i token dark | Nessuno |
| Cold start in light | Sistema in Light Mode | Finestra con `windowBackground #FFFFFF`, palette light invariata | Nessuno |
| Switch a runtime | Light → Dark → Light (anche background/foreground) | Nessun crash; stessi nodi, semantiche e callback; colori riletti dal tema | Nessuno |
| Riga attiva/checked | `ItemRow` in dark | Nome `inkPrimary #F8FAFC` su `surfaceCard #131B2E`; checkbox e badge invariati; semantica 6.1 | Nessuno |
| Header categoria | `CategoryHeader` in dark | Fondo `surfaceSubtle #131B2E`, bordo `borderCrisp #64748B`, titolo `inkPrimary`, badge invertito | Nessuno |
| FAB voce | idle / listening / processing / error / result | idle: `inkInverse` su `accentHighlight` (5,17:1 light / 7,02:1 dark); error/result: bianco su `#B45309`/`#15803D` (5,02:1); processing disabilitato `surfaceSubtle` + `inkMuted` | Durante processing il click è ignorato |
| Overlay ascolto | `VoiceListeningOverlay` in light e dark | Backdrop fisso `rgba(15,23,42,0.7)`; testo bianco ≥4,5:1; "Annulla" con bordo chiaro e target ≥48dp | Nessuno |
| Concludi spesa | `PresiSection` in dark | Testo bianco fisso su verde condiviso `#15803D` = 5,02:1 | Nessuno |
| Highlight ricerca | Query non vuota in light/dark | Fondo `accentHighlight` + testo `inkInverse`: 5,17:1 light, 7,02:1 dark | Nessuno |
| Dialog duplicati | `DuplicateResolutionDialog` in dark | Fondo `surfaceBase #0B0F17`, bordo `borderCrisp`, raggio `lg`; titolo `inkPrimary`, messaggio/Annulla `inkSecondary`; azioni e TalkBack invariati | Nessuno |
| Snackbar checkout | Undo disponibile in dark | Azione "Annulla" `inkInverse` su container inverso Material (17:1) | Nessuno |
| Badge/checkbox/swipe | Derivate dai token in dark | Badge 17,8:1; spunta checkbox 16,5:1; icona swipe bianca su verde 5,02:1 | Nessuno |

</frozen-after-approval>

> **Nota post-review (21/10/2026):** il blocco frozen resta invariato. Le righe della matrice su FAB `processing`, overlay e snackbar sono superate dalle correzioni in *Implementation Notes*: `processing` usa `inkSecondary` (non `inkMuted`), lo scrim dei `ModalBottomSheet` è al 60% e la snackbar usa una superficie scura in entrambi i temi. La modifica del blocco richiede una rinegoziazione esplicita.

## Code Map

- `app/src/main/res/values/themes.xml` — `Theme.Togo` light: parent `android:Theme.Material.Light.NoActionBar`, `windowBackground`/`statusBarColor` tokenizzati, `windowLightStatusBar = true`.
- `app/src/main/res/values-night/themes.xml` — **nuovo**: stessa `Theme.Togo` con parent `android:Theme.Material.NoActionBar`, `windowBackground`/`statusBarColor` scuri, `windowLightStatusBar = false`.
- `app/src/main/res/values/colors.xml` — **nuovo**: `togo_window_background = #FFFFFFFF`.
- `app/src/main/res/values-night/colors.xml` — **nuovo**: `togo_window_background = #FF0B0F17`.
- `app/src/main/java/it/togo/app/presentation/activelist/VoiceFab.kt` — FAB: helper `voiceFabContainerColor`/`voiceFabContentColor` (testabili), idle `inkInverse` su `accentHighlight`, error/listening/result `OnAccent` su verde/ambra condivisi, processing `inkSecondary` su `surfaceSubtle`; overlay: backdrop `colors.scrim` al 70%, icona/testo `OnAccent`, "Annulla" con target ≥48dp.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` — snackbar checkout con `tokens.snackbar` (container scuro, testo/azione `OnAccent`); errori con `colors.warningInk`.
- `app/src/main/java/it/togo/app/presentation/additem/AddItemBottomSheet.kt` — "Avanti" e "Aggiungi/Salva": colori disabilitati espliciti (`surfaceSubtle` + `inkSecondary`); conferma su verde condiviso con `bottomSheet.checkoutButtonContent`; scrim al 60%.
- `app/src/main/java/it/togo/app/ui/theme/Color.kt` — `scrim` Material dark `#0F172A`; `DarkWarningInk #F59E0B` e `DarkSuccessInk #4ADE80` per i testi di stato in dark; `OnAccent` (bianco su verde/ambra condivisi); costanti `SHEET_SCRIM_ALPHA = 0.6f`, `VOICE_OVERLAY_SCRIM_ALPHA = 0.7f`; `inverseSurface`/`inverseOnSurface`/`inversePrimary` dark allineati alla snackbar scura.
- `app/src/main/java/it/togo/app/ui/theme/ComponentTokens.kt` — `bottomSheet.checkoutButtonContent` e `itemRow.swipeDeleteIconColor` = `OnAccent`; nuovo `snackbar` (`container` scuro, `content`/`action` `OnAccent`).
- `app/src/main/java/it/togo/app/presentation/rules/LearnedRulesScreen.kt` — stato "Attiva/Disattivata" con `successInk`/`warningInk`; icone e azioni di errore con `warningInk`.
- `app/src/main/java/it/togo/app/presentation/voice/VoiceConfirmationSheet.kt` + `app/src/main/java/it/togo/app/presentation/additem/QuantityUnitPicker.kt` — testi/icone di warning con `warningInk`; bordi e indicatori non testuali restano `accentWarning` (≥3:1).
- `app/src/main/java/it/togo/app/presentation/history/HistoryScreen.kt` — timestamp con `inkSecondary` (l’alias `inkTertiary`/`inkMuted` dava 3,7:1 in dark).
- `SearchScreen.kt` / `ProductSearchField.kt` / `QuantityUnitPicker.kt` — placeholder “Cerca prodotto...” e label “Quantità” con `inkSecondary` (≥4,5:1); `VoiceConfirmationSheet.kt` — “Confidenza” con `inkSecondary`.
- `app/src/main/java/it/togo/app/presentation/search/SearchResultItem.kt` — highlight parametrico con `accentHighlight` + `inkInverse`; `indexOf(ignoreCase)` evita il crash con `İ`.
- `app/src/main/java/it/togo/app/presentation/additem/DuplicateResolutionDialog.kt` — migrato da `MaterialTheme.colorScheme` a `togoColors()` + `togoComponentTokens()` + `togoTypography()`; non cablato nella UI corrente.
- `app/src/main/java/it/togo/app/ui/theme/TogoTheme.kt` — `TogoTheme(darkTheme)` e `isSystemDarkTheme()`: sola lettura.
- `app/src/main/java/it/togo/app/MainActivity.kt` — usa `TogoTheme(darkTheme = isSystemDarkTheme())`: sola lettura.
- `app/src/test/java/it/togo/app/ui/theme/DarkModeContrastTest.kt` — **nuovo**: palette, 7:1 primario, warningInk/successInk, disabilitati, snackbar (token + inverse Material), dialog, checkout, FAB, overlay, highlight, badge/checkbox/swipe.
- `app/src/test/java/it/togo/app/ui/theme/DarkModeTokenUsageTest.kt` — **nuovo**: analisi statica (commenti/stringhe esclusi), `statusBarColor` light/night allineati, wiring di token nei componenti.
- `app/src/test/java/it/togo/app/presentation/activelist/DarkModeComposeTest.kt` — **nuovo**: render dark, token via CompositionLocal, risoluzione risorse/tema night/notnight, `VoiceBottomSheet` in ascolto, switch light↔dark.
- `app/src/test/java/it/togo/app/presentation/search/SearchHighlightTest.kt` — **nuovo**: colori dello `SpanStyle`, case-insensitive, occorrenze multiple, `İ`.
- `app/src/test/java/it/togo/app/presentation/additem/DuplicateResolutionDialogTest.kt` — aggiornato: wrapper `TogoTheme`, caso dark.

## Tasks & Acceptance

**Execution:**

- [x] `values-night/themes.xml` + `values*/colors.xml` — tema finestra scuro con `windowBackground`/`statusBarColor` allineati a `surfaceBase`/`surfaceBase-dark`; nessun flash bianco al cold start e icone status bar contrastate anche su API 24-34.
- [x] `VoiceFab.kt` — helper testabili per container/content, idle `inkInverse` su `accentHighlight`, error/listening/result `OnAccent`, processing `inkSecondary`; overlay con backdrop `colors.scrim` al 70%, icona/testo `OnAccent`, "Annulla" bordato con `heightIn(min = 48.dp)`.
- [x] `ComponentTokens.kt` — checkout e swipe icon su verde: `OnAccent` (bianco fisso) per mantenere ≥4,5:1 in entrambi i temi; nuovo `snackbar` token.
- [x] `AddItemBottomSheet.kt` — conferma su verde con `bottomSheet.checkoutButtonContent`; "Avanti" e conferma con colori disabilitati espliciti e leggibili; scrim al 60%.
- [x] `Color.kt` — `scrim` dark = `#0F172A`, scrim sheet al 60%, overlay al 70%; `DarkWarningInk`; `OnAccent`; inverse Material dark per snackbar scura.
- [x] `ActiveListScreen.kt` — snackbar con `tokens.snackbar` (container scuro in entrambi i temi, testo/azione `OnAccent`); errori con `warningInk`.
- [x] `SearchResultItem.kt` — `buildAnnotatedStringHighlight` riceve `highlightBackground` e `highlightForeground`; usa `indexOf(ignoreCase)` per evitare indici non validi con `İ`.
- [x] `DuplicateResolutionDialog.kt` — `Surface` con `duplicateDialog` tokens, testi `inkPrimary`/`inkSecondary`, tipografia TOGO; semantica e azioni invariate; componente non cablato.
- [x] `DarkModeContrastTest.kt` — palette AC, 7:1 primario, warningInk/successInk, disabilitati, snackbar (token + inverse Material), dialog, checkout, FAB idle, overlay (composito light/dark), highlight, badge/checkbox/swipe.
- [x] `DarkModeTokenUsageTest.kt` — niente `MaterialTheme.colorScheme`, `Color.White`, `Color.Black`, `Color(0x`, `android.graphics.Color` nei componenti in scope (commenti/stringhe esclusi); `values-night`, `statusBarColor` e wiring dei token verificati.
- [x] `DarkModeComposeTest.kt` — ItemRow attiva/checked, CategoryHeader, PresiSection, VoiceFab idle, overlay, dialog, `VoiceBottomSheet` in ascolto, token via CompositionLocal, risoluzione risorse/tema `night`/`notnight`, switch light↔dark.
- [x] `DuplicateResolutionDialogTest.kt` — wrapper `TogoTheme` e caso dark.
- [x] `SearchHighlightTest.kt` — **nuovo**: colori, case-insensitive, occorrenze multiple e `İ`.

**Acceptance Criteria:**

- Given Dark Mode di sistema attiva, when l'app viene aperta, then finestra e contenuto usano `surface-base-dark #0B0F17`, `surface-card-dark #131B2E`, `ink-primary-dark #F8FAFC`, `ink-secondary-dark #94A3B8`, `border-crisp-dark #64748B`, `accent-highlight-dark #60A5FA`.
- Given i token dark, when si calcola il contrasto, then `inkPrimary` su `surfaceBase` è ≥7:1 (18,33:1 calcolato dai token; la verifica runtime resta a carico dell'utente).
- Given ItemRow, CategoryHeader, VoiceBottomSheet, DuplicateResolutionDialog e FAB in dark, then mostrano i token dark corretti e nessun colore hardcoded.
- Given uno switch Light → Dark → Light, then nessun crash e gli stessi nodi/semantiche restano presenti.
- Given la 6.1 e la 6.2, then annunci, separazione riga/checkbox, target ≥48dp e dynamic type restano invariati.

## Implementation Notes

- **21/10/2026 — stato `in-progress`, implementazione e review statica applicate (nessuna build/test eseguita).**
- **Risorse:** aggiunti `values-night/themes.xml`, `values/colors.xml`, `values-night/colors.xml`; `values/themes.xml` e `values-night/themes.xml` impostano `windowBackground`, `statusBarColor` e `windowLightStatusBar`. `statusBarColor` è la correzione della regressione su API 24-34, dove le icone scure della status bar light restavano su una barra scura.
- **Scrim:** i tre `ModalBottomSheet` passano `scrimColor = colors.scrim.copy(alpha = SHEET_SCRIM_ALPHA)` con `SHEET_SCRIM_ALPHA = 0.6f` (DESIGN); l'overlay vocale usa `VOICE_OVERLAY_SCRIM_ALPHA = 0.7f` come da matrice frozen. Il 70% dell'overlay è una deviazione consapevole dal 60% di DESIGN, motivata dal testo bianco su contenuti arbitrari.
- **Contrasti corretti:** FAB idle da bianco fisso a `inkInverse` (dark 7,02:1); overlay da scrim bianco a backdrop `#0F172A` al 70% con testo `OnAccent`; checkout e conferma aggiunta su `OnAccent` (5,02:1 in dark); highlight ricerca parametrico (dark 7,02:1); snackbar con container scuro e testo/azione `OnAccent` (17:1 in entrambi i temi, niente barra bianca in dark); icona swipe `OnAccent`; errori/warning testuali con `warningInk` (dark 8,96:1, light 5,02:1) in `ActiveListScreen`, `VoiceBottomSheet`, `VoiceConfirmationSheet`, `QuantityUnitPicker`, `LearnedRulesScreen` e `ProductCategorizationWizard`; testi di successo con `successInk` (dark ≥9,7:1, light 5,02:1); disabilitati con `inkSecondary` su `surfaceSubtle` (≥4,5:1).
- **Dialog duplicati:** `Card` Material → `Surface` con `duplicateDialog.background/borderWidth/borderColor/radius`; titolo `titleScreen`, corpo `itemName`, azioni `itemMeta`; rimosso ogni `MaterialTheme.colorScheme`. Il componente non ha call site in `main`: la verifica è a livello di componente e di token.
- **Componenti non cablati:** `DuplicateResolutionDialog` e `VoiceListeningOverlay` restano predisposti ma non raggiungibili dalla UI. La UI reale di ascolto è `VoiceBottomSheet`, ora coperta da un test Compose in dark; la loro eliminazione/cablatura richiederebbe modifiche a ViewModel/dominio, vietate da questa story.
- **Token:** tre nuovi token semantici, `TogoColorTokens.DarkWarningInk (#F59E0B)`, `DarkSuccessInk (#4ADE80)` e `OnAccent (bianco)`. La palette base light/dark non cambia; `DarkWarningInk`/`DarkSuccessInk` sono esposti come `colors.warningInk`/`colors.successInk`, `OnAccent` è usato dai component token per i contenitori condivisi.
- **Test:** `DuplicateResolutionDialogTest` avvolge il dialog in `TogoTheme` e copre anche il caso dark; `DarkModeComposeTest` verifica i token via CompositionLocal, la risoluzione di risorse/tema `night`/`notnight` e il `VoiceBottomSheet` in ascolto; `SearchHighlightTest` copre colori, occorrenze e `İ`; `DarkModeContrastTest` copre warningInk, disabilitati, snackbar e dialog; `DarkModeTokenUsageTest` esclude commenti/stringhe e verifica il wiring.

## Design Notes

- Il backdrop dell'overlay vocale è volutamente sempre scuro: DESIGN indica `rgba(15,23,42,0.6)`, l'implementazione usa il 70% (`VOICE_OVERLAY_SCRIM_ALPHA`) per garantire il testo bianco su contenuti arbitrari in entrambi i temi. Deviazione consapevole, non un refuso.
- Lo scrim dei `ModalBottomSheet` usa invece il 60% di DESIGN (`SHEET_SCRIM_ALPHA`), così il fondo non cambia quasi nulla in dark.
- Il verde `accentSuccess` e l'ambra `accentWarning` sono condivisi tra i temi: su di essi il contenuto resta bianco fisso, non `inkInverse`, per non perdere contrasto in dark.
- `surfaceSubtle` dark è `#131B2E` (come `surfaceCard`): mantiene la separazione visiva senza introdurre un terzo livello di superficie.
- In dark `borderHairline` e `inkMuted` coincidono (`#64748B`): non vanno usati come coppia container/testo di un controllo disabilitato, perché il testo sparirebbe. Per questo i disabilitati usano `surfaceSubtle` + `inkSecondary`.
- Il tema finestra dark non cambia la palette Compose: evita solo il flash chiaro prima del primo frame.

## Verification

**Commands (da eseguire manualmente da Mike; l'agente non esegue build/test in questa sessione):**
- `.\test-android.ps1` — expected: tutti i test passano, incluse `DarkModeContrastTest`, `DarkModeTokenUsageTest`, `DarkModeComposeTest`, `SearchHighlightTest` e `DuplicateResolutionDialogTest`.
- `.\build-android.ps1` — expected: build Android completata senza errori.

**Manual checks:**
- Attivare Dark Mode di sistema, chiudere l'app e riaprirla: nessun flash bianco, sfondo `#0B0F17`, status bar `#0B0F17` con icone chiare.
- Controllare anche una build su API 24-34: la status bar non deve restare chiara con icone scure.
- Aprire la lista attiva: righe, header categoria, badge e checkbox leggibili; FAB voce blu chiaro con icona scura.
- Avviare l'ascolto vocale: il `VoiceBottomSheet` mostra "Ascolto in corso..." e "Annulla" toccabile; l'overlay legacy non è raggiungibile.
- Aprire il dialog duplicati solo se esposto da un flusso di produzione (oggi non lo è); in alternativa la verifica è nel test Compose.
- Cercare un prodotto: evidenziazione blu chiara con testo scuro; provare anche una query con `İ` per la regressione degli indici.
- Concludere una spesa e usare "Annulla" nella snackbar: snackbar scura, azione leggibile.
- Aprire il flusso di aggiunta e osservare "Avanti"/"Aggiungi" disabilitati: testo leggibile in dark, non invisibile.
- Alternare Light/Dark (anche con app in background) e verificare assenza di crash e stati coerenti.

## Spec Change Log

- **21/10/2026 — chiusura:** l'utente ha confermato i test OK e ha richiesto la chiusura; `6-3-dark-mode-completa` portata a `done` in `sprint-status.yaml`.

## Review Triage Log

- **A1 status bar (accettato, corretto):** `statusBarColor` aggiunto in light/night; test di risoluzione del tema su `night`/`notnight`.
- **A2/A3 scrim (accettato, corretto):** 60% sui tre `ModalBottomSheet`; overlay legacy 70% documentato come deviazione da DESIGN 0.6.
- **A4 colori fissi (accettato, corretto):** introdotto `OnAccent` per i contenitori verdi/ambra condivisi; eliminati i riferimenti diretti a `LightInkInverse` nei component token.
- **A5/A6 disabled (accettato, corretto):** `borderHairline` + `inkMuted` sono lo stesso colore in dark; “Avanti” e “Aggiungi” usano `surfaceSubtle` + `inkSecondary`, con test ≥4,5:1.
- **A7 warning text (accettato, corretto):** nuovo `warningInk` dark `#F59E0B`; `ActiveListScreen`, `VoiceBottomSheet`, `LearnedRulesScreen` e `ProductCategorizationWizard` aggiornati. Aggiunto anche `successInk` dark `#4ADE80` per lo stato "Attiva" (il verde condiviso dava 3,4:1).
- **A8 snackbar (accettato, corretto):** container scuro in entrambi i temi tramite `snackbar` token; `inverseSurface`/`inverseOnSurface`/`inversePrimary` dark allineati per la snackbar di sistema.
- **A9 FAB idle (respinto, documentato):** resta `accentHighlight` per non cambiare lo stato visivo 6.1; estratto l’helper testabile.
- **A10 dead code (accepted with scope limit, documented):** `DuplicateResolutionDialog` e `VoiceListeningOverlay` restano non cablati; la UI reale è `VoiceBottomSheet`, ora testata in dark. Cablare/eliminare richiede ViewModel/dominio, fuori perimetro.
- **A11 ricerca `İ` (accettato, corretto):** `indexOf(ignoreCase)` sostituisce il confronto su stringhe lowercase; aggiunto `SearchHighlightTest`.
- **A12 test mancanti (accettato, corretto):** aggiunti warningInk, disabilitati, snackbar, dialog, token via CompositionLocal, risoluzione risorse, status bar, `VoiceBottomSheet` e `SearchHighlightTest`.
- **A13 “misurato” (accettato, corretto):** ora “calcolato dai token”; la verifica runtime resta all’utente.
- **A14 timestamp/tertiary (extra, corretto):** `inkTertiary` (= `inkMuted`) su `surfaceCard` dava 3,7:1 in dark; timestamp di `HistoryScreen` e `LearnedRulesScreen` e i placeholder/label di `SearchScreen`, `ProductSearchField`, `QuantityUnitPicker` e `VoiceConfirmationSheet` passano a `inkSecondary` (≥7:1), con test dedicato.
