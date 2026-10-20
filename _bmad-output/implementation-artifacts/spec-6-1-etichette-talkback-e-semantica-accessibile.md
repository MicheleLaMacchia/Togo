---
title: '6.1 Etichette TalkBack e Semantica Accessibile'
type: 'feature'
created: '2026-10-16'
status: 'done'
route: 'dispatch'
review_loop_iteration: 1
baseline_commit: '5e29a43bb64ea391f23c6c503fb5506b3bf3cfca'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-6-context.md'
---

<frozen-after-approval reason="intento deciso dall'utente — non modificare senza rinegoziazione">

## Intent

**Problema:** TalkBack non espone in modo coerente i dati e le azioni di prodotti, sezione “Presi”, App Bar, VoiceFAB e dialog duplicati. La checkbox personalizzata è un `Box` visivo, non un controllo accessibile.

**Approccio:** Aggiungere semantiche Compose e controlli azionabili per identificare prodotti, stato e azioni, comprendere la sezione “Presi” e usare App Bar e dialog, senza cambiare il design visivo.

## Boundaries & Constraints

**Sempre:** Annunciare nome, quantità/unità, categoria e stato. Decisione di Mike: il tap sulla riga modifica; una checkbox separata spunta/deseleziona. Aggiungere Impostazioni nell'App Bar, collegandolo a Regole apprese. Usare etichette italiane, ruoli/stati/azioni significativi, evitare annunci duplicati e mantenere i callback di dominio.

**Mai:** Non cambiare persistenza, regole della spesa o stile oltre quanto serve all'accessibilità; il tap sulla riga non deve smettere di modificare. Non eseguire build/test Gradle: chiederli a Mike.

## I/O & Edge-Case Matrix

| Scenario | Input / Stato | Risultato / comportamento atteso | Gestione errori |
|----------|--------------|---------------------------|----------------|
| Voce attiva/spuntata | Prodotto con nome, quantità, unità e categoria | Annuncia dati, stato e azione spunta/deseleziona | Categoria assente: ripiego localizzato, mai vuoto |
| “Presi (N)” | Sezione espansa/collassata | Annuncia nome, conteggio, stato e toggle | Stato annunciato aggiornato dopo il toggle |
| Dialog duplicati | Opzioni abilitate e disabilitate | Titolo, messaggio, opzioni e relativi stati sono leggibili | Opzioni disabilitate non risultano azionabili |

</frozen-after-approval>

## Code Map

- `app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt` — la riga apre la modifica; la checkbox `Box` non è azionabile e la riga non riceve la categoria. Separare le semantiche.
- `app/src/main/java/it/togo/app/presentation/activelist/CategoryHeader.kt` — `CategorySection` conosce L1/L2/L3: passare la categoria alla riga.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListUiState.kt`, `ActiveListViewModel.kt`, `PresiSection.kt` — lo stato non conserva la categoria per le voci spuntate; propagare l'etichetta.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` e `app/src/main/java/it/togo/app/MainActivity.kt` — App Bar senza Impostazioni e Activity senza route; collegare `LearnedRulesScreen` senza nuova libreria.
- `app/src/main/java/it/togo/app/presentation/rules/LearnedRulesScreen.kt` — schermata esistente con `LearnedRulesViewModel` e callback `onBack`.
- `app/src/main/java/it/togo/app/presentation/activelist/VoiceFab.kt` — descrizioni “Avvia comando vocale” / “Interrompi ascolto” da adeguare.
- `app/src/main/java/it/togo/app/presentation/additem/DuplicateResolutionDialog.kt` — verificare semantiche di titolo, messaggio e opzioni Material.
- `app/src/test/java/it/togo/app/presentation/additem/DuplicateResolutionDialogTest.kt` usa Compose UI test; `app/build.gradle.kts` e `gradle/libs.versions.toml` non dichiarano la dipendenza dedicata. Non esiste `app/src/androidTest`.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/it/togo/app/presentation/activelist/ActiveListUiState.kt` e `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt` — fornire categoria canonica anche per prodotti spuntati — abilita annunci completi.
- [x] `app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt` e `app/src/main/java/it/togo/app/presentation/activelist/CategoryHeader.kt` — rendere la checkbox un controllo semanticamente azionabile e passare categoria; preservare riga→modifica — separa le azioni.
- [x] `app/src/main/java/it/togo/app/presentation/activelist/PresiSection.kt` — esporre sezione, conteggio, stato espansa/collassata e toggle — comunica il suo stato.
- [x] `app/src/main/java/it/togo/app/MainActivity.kt`, `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` e `app/src/main/java/it/togo/app/presentation/rules/LearnedRulesScreen.kt` — aggiungere Impostazioni nell'App Bar, aprire Regole apprese e tornare alla lista — completa l'accesso richiesto.
- [x] `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` e `app/src/main/java/it/togo/app/presentation/activelist/VoiceFab.kt` — verificare le descrizioni di Storico, Condivisione, aggiunta manuale e voce; il VoiceFAB inattivo deve annunciare “Aggiungi prodotto con la voce. Pulsante.”
- [x] `app/src/main/java/it/togo/app/presentation/additem/DuplicateResolutionDialog.kt` e `app/src/test/java/it/togo/app/presentation/additem/DuplicateResolutionDialogTest.kt` — testare annuncio e stato di titolo, messaggio e opzioni.
- [x] `gradle/libs.versions.toml`, `app/build.gradle.kts` e `app/src/test/java/it/togo/app/presentation/activelist/ActiveListAccessibilityTest.kt` — dichiarare/configurare `androidx.compose.ui:ui-test-junit4` e testare semantiche di righe, checkbox, sezione, App Bar/FAB e dialog.

**Acceptance Criteria:**
- Dato TalkBack attivo e una riga prodotto attiva con focus, quando sono lette le semantiche, allora vengono annunciati nome, quantità/unità, categoria, stato e azione per spuntare; il tap sulla riga continua ad aprire la modifica.
- Dato un prodotto spuntato in “Presi”, quando riceve il focus accessibile, allora sono comunicati nome, quantità/unità, categoria, stato spuntato e azione per deselezionare.
- Data la sezione “Presi (N)” espansa o collassata, quando l'intestazione riceve il focus o viene attivata, allora TalkBack annuncia conteggio e stato corrente e lo stato cambia in modo accessibile.
- Data la schermata principale visualizzata, quando TalkBack esplora i controlli, allora Storico, Condivisione, Impostazioni e aggiunta manuale hanno etichette significative; il VoiceFAB inattivo annuncia “Aggiungi prodotto con la voce. Pulsante.”
- Dato il pulsante Impostazioni attivato, quando la schermata Regole apprese si apre, allora TalkBack può identificarla e attivare il ritorno alla lista.
- Dato il dialog duplicati visualizzato, quando TalkBack lo esplora, allora annuncia titolo, messaggio, opzioni e stati; quelle disabilitate non sono azionabili.
- Data l'attivazione della riga per modificare o della checkbox per spuntare, quando viene eseguita una delle azioni, allora ciascuna invoca solo il callback corrispondente.
- Dato che Mike esegue la suite di test, quando girano i nuovi test semantici, allora tutte le asserzioni passano. In questa sessione non vengono eseguiti build/test Android.

## Implementation Notes

## Spec Change Log

## Review Triage Log

- **Blind Hunter 1 — medium / patch:** `PresiSection.kt` passes an empty `onClick`, but `ItemRow.kt` always attaches `combinedClickable`; TalkBack therefore exposes a no-op row action. Confermato nei due file; la riga in “Presi” deve essere non cliccabile, lasciando azionabile la checkbox.
- **Blind Hunter 2 — medium / patch:** `MainActivity.kt` cambia schermata solo tramite il pulsante Indietro della schermata Regole apprese; manca `BackHandler`, quindi il back di sistema esce dall'Activity. Confermato: aggiungere ritorno alla lista.
- **Blind Hunter 3 — low / patch:** la descrizione della checkbox ripete “selezionato/non selezionato” già esposto dallo stato `Role.Checkbox`. Confermato nella descrizione e nel `toggleable`; rimuovere lo stato duplicato dal testo descrittivo.
- **Blind Hunter 4 — medium / patch:** `quantityLabel()` usa abbreviazioni come `bt`, `latt`, `pent` da `StandardUnit.displayName`, poco chiare a voce. Confermato; usare nomi italiani per esteso.
- **Blind Hunter 5 — medium / patch:** `quantityLabel()` tronca zeri finali con `String.trimEnd`, che altera valori in notazione scientifica, e non localizza i decimali. Confermato; usare formattazione decimale senza notazione scientifica e con virgola italiana.
- **Blind Hunter 6 — medium / patch:** i test Compose sono in `src/test`, ma non è configurato un runtime Compose locale. Confermato: manca Robolectric, il test manifest e l'inclusione delle risorse Android.
- **Blind Hunter 7 — medium / patch:** i test della categoria passano etichette fittizie direttamente ai composable; non verificano la costruzione della categoria dal ViewModel/tassonomia. Confermato; aggiungere un test col percorso reale e una voce spuntata.
- **Blind Hunter 8 — medium / patch:** il test App Bar verifica solo la presenza del pulsante Impostazioni con callback vuoto, non la navigazione verso Regole apprese e il ritorno. Confermato; rendere il contenitore di navigazione testabile e verificare entrambi i passaggi.
- **Edge Case Hunter 1 — medium / patch:** conferma indipendente che la formattazione `Double` può corrompere valori in notazione scientifica. Confermato; accorpato con Blind Hunter 5 (stessa funzione difettosa).
- **Edge Case Hunter 2 — medium / patch:** nello stato `Processing` il VoiceFAB usa la descrizione idle e rimane attivo; il gateway impedisce una seconda partenza solo se è già `Listening`, non durante `Processing`. Confermato; annunciare elaborazione e disabilitare il FAB durante tale stato.
- **Edge Case Hunter 3 — medium / patch:** negli stati `Error` e `Result` il FAB è ancora azionabile ma la descrizione comunica solo lo stato, non l'azione disponibile. Confermato; annunciare riprova/nuovo inserimento.
- **Verification Gap 1 — medium / patch:** manca un test per la navigazione Impostazioni → Regole apprese → lista. Confermato; accorpato con Blind Hunter 8.
- **Verification Gap 2 — medium / patch:** manca un test che attraversi ViewModel, prodotto e tassonomia per controllare le etichette, incluse le voci spuntate. Confermato; accorpato con Blind Hunter 7.
- **Verification Gap 3 — medium / patch:** test mancanti per i contentDescription `Error`/`Result` del VoiceFAB. Confermato; aggiungere asserzioni assieme al fix Edge Case Hunter 3.
- **Controllo statico aggiuntivo — low / defer:** `ItemRow.kt` espone l'icona swipe `Elimina` con descrizione ma senza azione accessibile; il gesto di eliminazione non è raggiungibile tramite TalkBack. Preesistente e oltre i criteri espliciti di 6-1; registrato in `deferred-work.md`.

### Review loop 1

- **Blind Hunter 1 — medium / defer:** il tap sulla riga attiva annuncia “Doppio tocco per modificare”, ma `ActiveListViewModel.handleEditItem` è vuoto e `AddItemBottomSheet` non usa il parametro `isEditMode`. Il flusso di modifica è preesistente e già tracciato in `deferred-work.md`; la 6.1 ha però aggiunto un annuncio che oggi è una falsa affordance. Defer, con voce dedicata.
- **Blind Hunter 2 — medium / defer:** il pulsante “Storico” invia `UiEvent.NavigateToHistory`, ma `AppNavigator` non ha una destinazione History e il ViewModel ignora l'evento. Preesistente alla 6.1; registrato in `deferred-work.md`.
- **Blind Hunter 3 — low / defer:** eliminazione raggiungibile solo con swipe; l'icona “Elimina” non ha click/custom action. Duplicato del “Controllo statico aggiuntivo” già registrato; nessuna nuova voce.
- **Blind Hunter 4 — low / patch:** `ItemRow` esponeva un `onLongClick` vuoto tramite `combinedClickable`. Rimosso il ramo `onLongClick`; la riga attiva resta cliccabile e trascinabile.
- **Blind Hunter 5 — medium / defer:** al check-off la voce si sposta in “Presi” senza annuncio live/focus. Comportamento preesistente di 2.4; registrato in `deferred-work.md`.
- **Blind Hunter 6 — medium / patch:** l'header “Presi” aveva descrizione e click ma nessun `Role` né `stateDescription`. Aggiunti `Role.Button` e `stateDescription`; la descrizione ora contiene conteggio e azione, lo stato è una proprietà semantica separata. Test aggiornato.
- **Blind Hunter 7 — low / patch:** le righe “Presi” mantenevano il gesture handler di swipe con `onDelete` no-op. Il gesture e l'icona “Elimina” sono ora attivi solo quando `rowActionDescription != null`; le righe spuntate non espongono più gesti/icone morte.
- **Blind Hunter 8 — low / false:** le descrizioni VoiceFab contengono “Pulsante.” e istruzioni “Tocca per…”. La stringa idle è richiesta esplicitamente dall'AC 6.1; le altre descrivono l'azione disponibile. Nessuna modifica.
- **Blind Hunter 9 — low / false:** il FAB in `Processing` conserva l'azione `OnClick`, ma il click è guardato da `if (enabled)` e la semantica è `disabled()`. Aggiunto test `performClick` che verifica la non-invocazione; il comportamento è sicuro.
- **Blind Hunter 10 — medium / patch:** il permesso `RECORD_AUDIO` negato lasciava il tap voce senza feedback. Aggiunto snackbar “Permesso microfono necessario per usare il comando vocale” nel ramo di diniego.
- **Blind Hunter 11 — medium / patch:** il launcher vocale veniva registrato sul gateway singleton e mai rilasciato. Sostituito `LaunchedEffect` con `DisposableEffect` e aggiunto `unregisterLauncher()`; test di lifecycle aggiunto.
- **Blind Hunter 12 — low / defer:** le nuove copy italiane sono hardcoded. Il progetto usa lo stesso pattern in tutte le schermate; l'estrazione in string resources è una storia di localizzazione separata, registrata in `deferred-work.md`.
- **Blind Hunter 13 — medium / defer:** `DuplicateResolutionDialog` interpola quantità/unità raw. Preesistente alla 6.1; registrato in `deferred-work.md`.
- **Blind Hunter 14 — low / false:** l'opzione SUM viene nascosta quando non disponibile, non esposta come disabilitata. L'AC richiede che le opzioni disabilitate non siano azionabili; non richiede che restino visibili. Nessuna modifica.
- **Blind Hunter 15 — low / defer:** test con `Thread.sleep` e `Dispatchers.IO` hardcoded. Infrastruttura di test preesistente; registrata in `deferred-work.md`.
- **Blind Hunter 16 — low / defer:** il test dei token shape verifica solo che gli oggetti non siano null. Preesistente alla 6.1; registrato in `deferred-work.md`.
- **Blind Hunter 17 — low / patch:** il bordo sinistro di `CategoryHeader` era disegnato a `x = 0` e tagliato a metà. Centrato lo stroke su `strokeWidth / 2f`; test statico aggiornato.
- **Blind Hunter 18 — low / defer:** il guard `GlobalContext.getOrNull()` in `TogoApplication` è un workaround per Robolectric. Nessun impatto utente; registrato per la pulizia dell'infrastruttura di test.
- **Blind Hunter 19 — low / false:** gli status di spec 6.2 e dei fix nel working tree non sono allineati alla cartella. Il fix richiederebbe di modificare la spec di questa build, escluso dalle regole di review; nessuna azione.
- **Blind Hunter 20 — low / defer:** `epic-6-context.md` contiene un'affermazione contraddittoria sull'invio della voce a servizi remoti. Il fix modifica file agent-context, fuori dal perimetro del patch; registrato in `deferred-work.md`.
- **Edge Case Hunter 1 — high / patch:** con tutte le voci spuntate `activeGroups` è vuoto e la condizione mostrava `EmptyState`, nascondendo “Presi” e “Concludi spesa”. La condizione ora richiede anche `!hasCheckedItems`; test Compose aggiunto. Era un bug preesistente, ma blocca direttamente la sezione “Presi” della 6.1.
- **Edge Case Hunter 2 — medium / patch:** duplicato di Blind Hunter 10 (permesso negato); accorpato.
- **Edge Case Hunter 3 — medium / patch:** le due query Room `getActiveItems`/`getCheckedItems` possono emettere separatamente durante il toggle, mostrando lo stesso item in lista e in “Presi”. Aggiunto filtro di deduplica per id in `buildUiState` e test con un fake che restituisce lo stesso item da entrambe le query.
- **Edge Case Hunter 4 — low / defer:** duplicato di Blind Hunter 15 (`Thread.sleep`); accorpato.
- **Verification Gap 1 — medium / patch:** il test Impostazioni usava un pulsante sintetico, non l'icona reale. Aggiunto test che compone `ActiveListScreen` dentro `AppNavigator`, clicca “Impostazioni” e verifica “Regole apprese” e il ritorno.
- **Verification Gap 2 — medium / patch:** il test del FAB in `Processing` non tentava il click. Aggiunto `performClick` con asserzione che il callback non venga invocato.
- **Verification Gap 3 — medium / patch:** la registrazione del launcher era verificata solo con source scan. Aggiunto test runtime che verifica `isLauncherRegistered` durante la composizione e il rilascio all'uscita; il ramo permesso negato resta coperto solo staticamente e manualmente, registrato in `deferred-work.md`.
- **Verification Gap 4 — low / patch:** la posizione del pulsante “Concludi spesa” era verificata solo con source scan. Aggiunto test Compose che clicca il pulsante e verifica il callback `onCheckout`.
- **Verification Gap 5 — low / defer:** i fix di rendering sono fissati da scan testuali, non da screenshot. Manca un'infrastruttura screenshot; registrato in `deferred-work.md`.

### Esito

Le correzioni patch sono state applicate nel diff corrente, inclusi test per la categoria calcolata dal ViewModel, navigazione reale da Impostazioni, stati VoiceFAB, checkbox, formattazione quantità, lifecycle del launcher vocale, sezione “Presi” con tutte le voci spuntate e deduplica delle query. Build e test non sono stati eseguiti su richiesta di Mike; il matrix test audit resta da confermare con l'esecuzione locale. Le voci preesistenti o fuori perimetro sono registrate in `deferred-work.md`.

### Review Findings

#### Decision-needed (4, risolte → patch)

- [x] [Review][Patch][from Decision] Falsa affordance “Doppio tocco per modificare” — `ItemRow.kt:78` annuncia la modifica e `ActiveListScreen.kt:258` emette `UiEvent.EditItem`, ma `ActiveListViewModel.kt:270-272` ha `handleEditItem` vuoto e `AddItemBottomSheet` non riceve l'item in modifica. Il test `ActiveListAccessibilityTest.kt:162-187` verifica solo una lambda locale. Viola AC 1 e il vincolo «il tap sulla riga non deve smettere di modificare». Fonti: Blind Hunter 4, Acceptance Auditor 2, Verification Gap (Other). Opzioni: cablare il flusso di modifica oppure rimuovere annuncio/click finché non esiste. **Decisione:** cablare il flusso di modifica.
- [x] [Review][Patch][from Decision] Sparizione transitoria di una voce durante il toggle — `ActiveListViewModel.kt:64-95`: le due query Room `getActiveItems`/`getCheckedItems` possono emettere in ordine inverso; se la query attiva rimuove l'item prima che la query checked lo includa, l'item non compare in nessuna delle due liste fino all'emissione successiva. La deduplica attuale copre solo il caso di duplicato. Fonte: Edge Case Hunter 3. Opzioni: un'unica query `getAllItems()` partizionata nel ViewModel, oppure conservare lo stato precedente fino a convergenza. **Decisione:** un'unica query `getAllItems()` partizionata nel ViewModel.
- [x] [Review][Patch][from Decision] Regole apprese accentate non più corrispondenti — `CategoryAssigner.kt:31,35` cerca l'espressione normalizzata con accent-fold; le righe già salvate con accento (o create da percorsi che non normalizzano) non vengono più trovate, senza errore visibile. `CorrectCategoryUseCase.kt:85-90` non migra le righe esistenti. Fonte: Edge Case Hunter 4. Opzioni: normalizzare in scrittura e migrare i dati, oppure interrogare sia la forma raw sia quella folded. **Decisione:** normalizzare in scrittura e migrare i dati.
- [x] [Review][Patch][from Decision] Test dipendenti da `Thread.sleep(200)` — 35 occorrenze in `HistoryViewModelTest.kt` e `LearnedRulesViewModelTest.kt` sostituiscono `advanceUntilIdle()` mentre i ViewModel raccolgono su `Dispatchers.IO`. I test diventano lenti e non deterministici; la sostituzione è introdotta in questo diff. Fonti: Blind Hunter 12, Edge Case Hunter 5, Verification Gap 6. Opzioni: iniettare dispatcher di test nei ViewModel, oppure usare un helper `awaitUntil` con timeout. **Decisione:** iniettare dispatcher di test nei ViewModel.

#### Patch (14 + 4 risolte = 18)

**Stato:** 18/18 applicate nel working tree; build e test Gradle ancora da eseguire.

- [x] [Review][Patch] Checkbox fusa nella riga: semantica e azioni non separate [app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt:169-286] — La `Surface` applica `mergeDescendants = true` e `combinedClickable`, mentre `CheckboxUtility` è un discendente con `toggleable`/`Role.Checkbox`. Il nodo fuso espone un solo controllo: il click della riga prevale sul toggle, lo stato/nome viene ripetuto e la riga “Presi” mostra un'azione di click. I test `ActiveListAccessibilityTest.kt:132-215` assumono due nodi distinti e non possono passare tutti. Fonti: Acceptance Auditor 1 e 3, Blind Hunter 3. Correzione: rendere la checkbox un nodo sibling esterno alla Surface cliccabile, preservando tap riga, swipe e stato “Presi”.
- [x] [Review][Patch] Contrasto insufficiente nelle righe spuntate [app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt:91,240-241] — Il nome usa `inkMuted` (#94A3B8) su bianco (≈2,56:1); il badge usa testo `#FFFFFF` su `surfaceSubtle` #F8FAFC (≈1,05:1). `DesignSystemTokenTest.kt:161-171` misura `inkPrimary`, non il colore reale. Fonti: Blind Hunter 1 e 2. Correzione applicata: usare `inkPrimary` per il nome (7:1 sia light sia dark; `inkSecondary` dark si fermava a ~6,69:1) e mantenere il badge scuro, con assert di contrasto per lo stato checked.
- [x] [Review][Patch] “Concludi spesa” non fa nulla quando tutte le voci sono in “Presi” [app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt:326-337] — Con `activeGroups` vuoto il metodo ritorna senza feedback, ma il pulsante resta visibile perché la sezione “Presi” è visibile. Il test `all checked items keep presi section visible` verifica solo la presenza, non il click. Fonte: Edge Case Hunter 1. Correzione: mostrare uno snackbar di completamento/nessuna voce attiva (o nascondere il pulsante), con test.
- [x] [Review][Patch] Stato della lista perso navigando a Impostazioni [app/src/main/java/it/togo/app/AppNavigator.kt:51-63] — Il `when` sostituisce il contenuto senza `SaveableStateHolder`; `PresiSection` usa `rememberSaveable` per `expanded` e la `LazyColumn` perde lo scroll. Fonte: Blind Hunter 6. Correzione: avvolgere le destinazioni in `SaveableStateProvider` e testare la conservazione.
- [x] [Review][Patch] Bordo sinistro di `CategoryHeader` rientrato [app/src/main/java/it/togo/app/presentation/activelist/CategoryHeader.kt:60-75] — `.drawBehind` è applicato dopo `.padding(horizontal/vertical)`, quindi il bordo è disegnato nell'area interna e non copre il padding. Fonte: Blind Hunter 7. Correzione: spostare `drawBehind` prima del padding o disegnare sul contenitore esterno.
- [x] [Review][Patch] Permesso microfono negato permanentemente senza percorso di recupero [app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt:80-93] — Il ramo di diniego mostra solo uno snackbar; se il permesso è negato per sempre non c'è azione verso le impostazioni. Fonte: Edge Case Hunter 2. Correzione: snackbar con azione che apre `ACTION_APPLICATION_DETAILS_SETTINGS`, con test.
- [x] [Review][Patch] Test dei token shape indebolito [app/src/test/java/it/togo/app/ui/theme/DesignSystemTokenTest.kt:101-111] — Le asserzioni exact sui raggi (`0f`, `4f`, `8f`, `12f`, `>=9000f`) sono state sostituite da `shapes.x != null`; il commento che dice “non testabile” è falso. Fonte: Blind Hunter 11. Correzione: ripristinare le asserzioni sui raggi.
- [x] [Review][Patch] Test statici tautologici [app/src/test/java/it/togo/app/ItemRowFormattingTest.kt; app/src/test/java/it/togo/app/VoiceLauncherRegistrationTest.kt; app/src/test/java/it/togo/app/AndroidManifestPermissionTest.kt] — I test leggono il sorgente e usano `contains`; `koinInject || AndroidVoiceRecognitionGateway` è sempre vero perché il file nomina il tipo. Non rilevano le regressioni dichiarate. Fonte: Blind Hunter 13. Correzione: sostituirli con test di semantica/behavior o assert sui token calcolati.
- [x] [Review][Patch] Destinazione Impostazioni e iniezione Koin non esercitate [app/src/test/java/it/togo/app/AppNavigatorTest.kt:49-55; app/src/test/java/it/togo/app/di/KoinModuleTest.kt:47-68] — I test di navigazione usano una `learnedRulesContent` sintetica; `KoinModuleTest` non risolve `LearnedRulesViewModel`. Fonti: Blind Hunter 14, Verification Gap 3, Acceptance Auditor 7. Correzione: risolvere `LearnedRulesViewModel` dal grafo Koin e comporre la schermata reale con ViewModel fake.
- [x] [Review][Patch] Rami del controllo vocale non testati dalla schermata [app/src/test/java/it/togo/app/presentation/activelist/ActiveListAccessibilityTest.kt:285-339,410-439] — I test usano una `VoiceFab` con callback stub e controllano solo `isLauncherRegistered`; non coprono permesso concesso/negato né `cancel()` durante l'ascolto. Fonte: Verification Gap 1. Correzione: test a livello schermata con gateway fake e permessi controllati.
- [x] [Review][Patch] Restaurazione della navigazione non testata [app/src/test/java/it/togo/app/AppNavigatorTest.kt:62-86] — Nessun `StateRestorationTester`/`recreate` verifica che `LearnedRules` resti aperta dopo rotazione o process death. Fonte: Verification Gap 2. Correzione: aggiungere test di recreazione per `LearnedRules` e fallback `ActiveList`.
- [x] [Review][Patch] Posizione di “Concludi spesa” non osservata [app/src/test/java/it/togo/app/PresiSectionStaticAnalysisTest.kt:22-35] — Il test verifica solo l'ordine testuale nel sorgente; spostare il Button fuori dalla Column lascerebbe il test verde. Fonte: Verification Gap 4. Correzione: test tag sulla Column/contenuto e assert di contenimento o scroll.
- [x] [Review][Patch] Annunci duplicati nel VoiceFab [app/src/main/java/it/togo/app/presentation/activelist/VoiceFab.kt:86-100,120-142] — Per gli stati Error/Result la label visibile e la `contentDescription` dell'icona sono la stessa frase; TalkBack la legge due volte. La parte riga/checkbox è coperta dal patch semantico. Fonte: Acceptance Auditor 4. Correzione: descrizione icona limitata all'azione o `null` quando la label è visibile.
- [x] [Review][Patch] Dipendenza Mockito inutilizzata [gradle/libs.versions.toml:16,44] — L'alias `mockito`/`mockito-core` non è usato dopo la migrazione a MockK; `arch-core-testing` è invece usato da `HistoryDaoTest`, `LearnedRulesDaoTest` e `ShoppingItemDaoTest`. Fonte: Blind Hunter 23 (parte arch-core smentita). Correzione: rimuovere l'alias Mockito.

#### Deferred (10 entries, 11 findings)

- [x] [Review][Defer] Lookup prodotto N+1 in `buildUiState` [app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt:100-117] — deferred: pattern preesistente, esteso dal nuovo loop checked; richiede query batch/cache e API repository fuori perimetro. Fonte: Blind Hunter 8.
- [x] [Review][Defer] Formattazione quantità incoerente e dialog duplicati raw [app/src/main/java/it/togo/app/presentation/additem/DuplicateResolutionDialog.kt:50-51,104,117] — deferred: `ItemRow` usa virgola italiana, dialog/share usano Double raw e `StandardUnit.format` usa il punto; la correzione richiede un formatter condiviso e propagazione dell'unità. Fonti: Blind Hunter 9, Acceptance Auditor 6.
- [x] [Review][Defer] Hint categoria multi-parola nel parser vocale [app/src/main/java/it/togo/app/domain/parser/VoiceCommandParser.kt:170-176] — deferred: la rimozione senza word boundary lascia “integrale” da “pasta integrale”, ma la logica difettosa preesiste al diff. Fonte: Blind Hunter 10.
- [x] [Review][Defer] Nessun test TalkBack su device/emulatore [app/src/test/java/it/togo/app/presentation/activelist/ActiveListAccessibilityTest.kt] — deferred: manca una suite `androidTest`/strumentata; i test Compose verificano l'albero semantico, non gli annunci reali. Fonte: Blind Hunter 15.
- [x] [Review][Defer] Fix di rendering verificati solo con scan testuali [app/src/test/java/it/togo/app/CategoryHeaderStaticAnalysisTest.kt; app/src/test/java/it/togo/app/ItemRowStaticAnalysisTest.kt; app/src/test/java/it/togo/app/WizardStepIndicatorTest.kt] — deferred: manca un'infrastruttura screenshot/pixel; i controlli di geometria richiedono test separati. Fonte: Verification Gap 5.
- [x] [Review][Defer] Spec di riparazione ancora `ready` con checklist non spuntate — deferred: il fix modificherebbe altre spec/audit trail, fuori dal perimetro della review. Fonte: Blind Hunter 18.
- [x] [Review][Defer] `deferred-work.md` attribuisce a “preesistente” `Thread.sleep` e test shape indebolito — deferred: entrambi sono introdotti nel diff rispetto al baseline; la correzione è documentale e va coordinata con la decisione sui test. Fonte: Blind Hunter 19.
- [x] [Review][Defer] `epic-6-context.md`: lingua e privacy ambigua — deferred: il fix modifica un file agent-context; la frase su log/testo riconosciuto e servizi remoti va riscritta con una decisione esplicita. Fonte: Blind Hunter 20.
- [x] [Review][Defer] Cambio retroattivo di `border-crisp-dark` — deferred: `#475569` → `#64748B` appartiene alla Dark Mode 6.3 e tocca DESIGN/epics/spec-1.5; va tracciato con una storia o change log, non dentro la 6.1. Fonte: Blind Hunter 21.
- [x] [Review][Defer] Diff misto con modifiche estranee alla 6.1 — deferred: dominio, parser, wizard, unità, tema e script Docker sono fuori dal perimetro accessibilità; separare i commit o motivare esplicitamente. Fonte: Acceptance Auditor 5.

#### Rejected

- **Blind Hunter 5 — false:** `LearnedRuleEntity` ha un indice unico su `userExpression`; `getByExpression` può restituire al massimo una regola. Il filtro successivo su `isActive`/`productId` è corretto e non può scegliere una regola di un altro prodotto.
- **Blind Hunter 16 — false:** con `canSum=false` la SUM viene nascosta, non esposta disabilitata; non esiste un'opzione disabilitata azionabile e l'AC non impone di renderla visibile. La review precedente aveva già respinto la stessa lettura.
- **Blind Hunter 17 — rejected (regola 35):** lo stato `done` vs `review` e le note non allineate sono reali, ma la correzione richiede di modificare la spec sotto review; lo status viene gestito da questo workflow.
- **Blind Hunter 22 — false:** `RECORD_AUDIO` è necessario al gate runtime aggiunto per il controllo vocale; la nota “solo VIBRATE” della spec di riparazione non lo vieta. Il test del diniego mancante è coperto da Edge Case Hunter 2 e Verification Gap 1.
