---
title: '8.7 Ripristino compilazione UI shopping su ListItem'
type: 'bugfix'
created: '2026-10-29'
status: 'review'
story: 8.7
epic: 8
route: 'dispatch'
baseline_commit: '946452df25e34d95ccab32c863b556eb807e4e91'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-8-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/togo-tasks-unificata/epics.md'
---

<frozen-after-approval reason="human-owned intent - do not modify unless human renegotiates">

## Intent

**Problem:** L'interfaccia shopping e i relativi test referenziano ancora `ShoppingItem`, `HistoricalItem`, `ShoppingListRepository` e `HistoryRepository`, rimossi nel refactor Story 8.7. La compilazione dell'app si interrompe e la cronologia e il form non sono più collegati al modello unificato.

**Approach:** Allineare wiring Koin, form di aggiunta/modifica e cronologia a `ListItem` e `ListItemRepository`, mantenendo ricerca, attributi, modifica della riga attiva, ricerca nello storico e riaggiunta. Aggiornare i test di presentazione ai contratti correnti. Quantità e unità seguono il piano Epic 8: default `1` e `PIECE`.

**Boundaries & Constraints**

**Always:** Preservare le modifiche già presenti nel worktree. Usare il `Clock` iniettato per timestamp e le transizioni offerte dal repository. La cronologia mostra le righe acquistate o eliminate, senza mostrare `TO_BUY`.

**Never:** Ripristinare tipi, repository, tabelle o adapter legacy. Non lanciare build o test Gradle, non alterare migrazioni/schema e non cambiare lo stato sprint della Story 8.7.

</frozen-after-approval>

## Code Map

- `app/src/main/java/it/togo/app/di/PresentationModule.kt` -- binding obsoleti; `ActiveListViewModel` richiede `ListItemRepository`, `CatalogRepository`, `Clock`; `HistoryViewModel` e `AddItemViewModel` richiedono il repository unificato.
- `app/src/main/java/it/togo/app/presentation/additem/AddItemBottomSheet.kt` -- factory Compose passa l'item editabile e deve risolvere i binding Koin correnti.
- `app/src/main/java/it/togo/app/presentation/additem/AddItemViewModel.kt`, `AddItemUiState.kt` -- flusso del form usa ancora i tipi rimossi; riusare `ListItem.create`, `updateDetails`, `add`, `observeAttributeSuggestions`, `getLastAttributes`.
- `app/src/main/java/it/togo/app/domain/model/ListItem.kt` e `domain/repository/ListItemRepository.kt` -- modello valido, stati espliciti, timestamp e operazioni di merge/transizione; `updateDetails` aggiorna soltanto righe `TO_BUY`.
- `app/src/main/java/it/togo/app/presentation/history/HistoryViewModel.kt`, `HistoryUiState.kt`, `HistoryScreen.kt` -- sostituire il DTO storico con `ListItem`; osservare stati non attivi, conservare ricerca e delegare riaggiunta/ripristino a `reAdd`/`restore`.
- `app/src/test/java/it/togo/app/presentation/additem/` e `presentation/history/HistoryViewModelTest.kt` -- test di presentazione ancora tipizzati sui contratti rimossi; aggiornarli e coprire timestamp, stato e deduplica/riaggiunta.
- `app/src/main/java/it/togo/app/di/RepositoryModule.kt`, `ListItemRepository.kt`, `epic-8-context.md` -- fonte corrente dei binding e delle invarianti, da riusare senza reintrodurre adapter.

## Tasks & Acceptance

**Execution:**
- [x] `PresentationModule.kt` e `AddItemBottomSheet.kt` -- passare i parametri e le dipendenze correnti, incluso `Clock`, così Koin costruisce i ViewModel senza riferimenti legacy.
- [x] `AddItemViewModel.kt` e `AddItemUiState.kt` -- usare `ListItem` in inserimento e modifica; impostare i default richiesti, salvare con `add` o `updateDetails` e preservare attributi/suggerimenti.
- [x] `HistoryViewModel.kt`, `HistoryUiState.kt` e `HistoryScreen.kt` -- proiettare correttamente righe `PURCHASED`/`DELETED`, mantenere nomi/ricerca/attributi/data/conteggio, e delegare le azioni alle transizioni del repository.
- [x] Test di presentazione additem e history -- migrare fake/mock ai nuovi contratti e verificare i comportamenti di regressione descritti sopra.

**Acceptance Criteria:**
- Given i contratti Epic 8 presenti, when si analizzano sorgenti e wiring, then nessun file UI/DI/test referenzia i tipi o repository rimossi.
- Given una voce da aggiungere o modificare, when il form salva, then persiste un `ListItem` valido, la modifica conserva ID e timestamp di stato, e usa il `Clock` iniettato.
- Given righe in `LIST_ITEM`, when si apre/cerca nello storico o si riaggiunge una riga, then sono disponibili solo gli stati storici, i dettagli restano ricercabili e la transizione/merge è delegata al repository.
- Given i test aggiornati, when viene eseguita la suite `test-android.ps1` il 2026-10-30, then i test JVM passano senza riferimenti legacy. `build-android.ps1` completa `clean assembleDebug` e produce l'APK debug.

## Implementation Notes


- Allineati moduli Koin, add/edit, cronologia e test a `ListItemRepository`; la cronologia mantiene `PURCHASED` e `DELETED` e usa il formatter quantità condiviso.
- Il form salva con il `Clock` iniettato, usa quantità predefinita `1`/unità `PIECE` e resta aperto se `updateDetails` non trova una riga modificabile.
- Aggiunta gestione errori di repository nella cronologia e test per messaggi visibili, pulizia snackbar e modifica non riuscita.
- Revisione statica iniziale: nessun riferimento ai tipi/repository shopping rimossi in UI, DI e test; in quella fase build/test Gradle non erano stati eseguiti. I follow-up ancora aperti sono elencati sotto.

## Review Triage Log

- Closure verification 2026-10-30: `test-android.ps1` completed successfully and `build-android.ps1` produced the debug APK. The suite now also includes seeded 6-to-7 migration coverage that verifies legacy tables are dropped while the migrated `LIST_ITEM` rows remain.

- medium/defer -- Blind Hunter, `ListItemMigration.kt`: in produzione Room gestisce la transazione della migrazione; non e' richiesta transazionalita' autonoma del metodo migrate.
- false -- Blind Hunter, `ActiveListViewModel.kt`: `complete` restituisce `null` quando la riga è assente o non più attiva; rimuoverla dall'elenco attivo è coerente con lo stato persistito, mentre l'errore segnala la race.
- medium/defer -- Blind Hunter, `ActiveListViewModel.kt`: un'eccezione da `complete` interrompe la coroutine dopo la rimozione ottimistica; questa gestione non è stata introdotta dal fix UI ed è registrata in `deferred-work.md`.
- medium/defer -- Blind Hunter, scope DB: le modifiche di schema/migrazione citate erano già presenti nel worktree prima dell'intervento e sono state preservate come richiesto; non sono state attribuite al fix UI.
- medium/patch -- Blind Hunter, `HistoryScreen.kt`: l'interpolazione grezza produce quantità con `.0`; corretta riusando `quantityLabel` della lista attiva.
- low/defer -- Blind Hunter, `ShareListUseCase.kt`: il cast `Double`→`Long` può alterare valori interi oltre il range; è codice preesistente fuori dal fix compile ed è registrato in `deferred-work.md`.
- medium/patch -- Blind Hunter, `HistoryViewModelTest.kt`: il nuovo test verifica ora il risultato visibile di riaggiunta/ripristino e la pulizia snackbar; il merge è già coperto da `RoomListItemRepositoryTest`.
- medium/defer -- Blind Hunter, `ActiveListAccessibilityTest.kt`: il fake non esercita il completamento riuscito e non è stato trovato un test del ViewModel per la transizione; registrato in `deferred-work.md`, fuori dagli errori additem/history richiesti.
- resolved -- Blind Hunter, `story87_summary.txt`: il riepilogo segnalato non e' presente nel checkout corrente; la verifica effettiva e' riportata in questa spec.
- medium/patch -- Edge Case Hunter, `AddItemViewModel.kt`: ora `updateDetails == null` lascia aperto il form e mostra un errore, invece di emettere successo.
- medium/defer -- Edge Case Hunter, `ActiveListViewModel.kt`: rilievo duplicato sulla mancata gestione di eccezioni da `complete`; aggregato con il finding Blind Hunter in `deferred-work.md`.
- medium/defer -- Edge Case Hunter, `ActiveListViewModel.kt`: un'eccezione di `restore` nell'undo non viene gestita; registrata insieme alla gestione incompleta delle transizioni attive in `deferred-work.md`.
- medium/patch -- Edge Case Hunter, `HistoryViewModel.kt`: `reAdd` e `restore` ora propagano la cancellazione e mostrano feedback per errori repository.
- medium/patch -- Edge Case Hunter, `AddItemViewModel.kt`: `PIECE` è sempre disponibile come default; aggiornate le aspettative dei test del form.
- medium/defer -- Edge Case Hunter, scope DB: stesso rilievo di attribuzione delle modifiche migrazione preesistenti; nessun cambio schema è stato fatto da questo intervento.
