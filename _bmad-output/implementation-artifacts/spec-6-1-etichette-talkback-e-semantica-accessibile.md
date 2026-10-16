---
title: '6.1 Etichette TalkBack e Semantica Accessibile'
type: 'feature'
created: '2026-10-16'
status: 'in-review'
route: 'dispatch'
review_loop_iteration: 0
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

### Esito

Le correzioni patch sono state applicate nel diff corrente, inclusi test per la categoria calcolata dal ViewModel, navigazione, stati VoiceFAB, checkbox e formattazione quantità. Build e test non sono stati eseguiti su richiesta di Mike; il matrix test audit resta da confermare con l'esecuzione locale.
