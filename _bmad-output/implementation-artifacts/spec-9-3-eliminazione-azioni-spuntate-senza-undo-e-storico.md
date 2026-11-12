---
title: '9.3 Eliminazione azioni spuntate senza undo e senza storico'
type: 'feature'
created: '2026-11-11'
updated: '2026-11-12'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: 'dbae80a2df11d2e7eb243c0961572051bb811883'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-9-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-9-2-pulsante-concludi-attività-e-dialog-di-conferma.md'
---

<frozen-after-approval reason="intent esplicito della Story 9.3">

## Intent

**Problem:** Dopo la conferma del dialog “Concludi attività”, le azioni spuntate non vengono ancora eliminate.

**Approach:** Eliminare in modo definitivo e persistente solo le azioni spuntate da `ACTION_ITEM` dopo la conferma. Le azioni attive e ogni dato della lista della spesa e dello storico acquisti restano invariati; l'operazione non offre undo.

## Boundaries & Constraints

**Always:** Limitare la cancellazione alle righe con `stato_spunta = 1` in `ACTION_ITEM`. Usare il flusso repository esistente e il thread IO per la mutazione. Non aggiungere conferme, snackbar o stato di undo per questa operazione.

**Never:** Non scrivere in `HISTORICAL_ITEM`, non chiamare checkout, non incrementare conteggi di acquisto, non modificare `SHOPPING_ITEM` e non ripristinare azioni concluse. Eliminare o invalidare un eventuale undo di cancellazione singola pendente al momento della conferma, così non rimane un'azione “Annulla” disponibile dopo la conclusione.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Conferma con azioni spuntate e attive | `ACTION_ITEM` contiene righe spuntate e non spuntate | Cancella solo le spuntate; le attive restano; il flusso aggiorna la UI senza snackbar di undo | Nessun undo del bulk; mostra l'errore già previsto dalla UI se il repository fallisce |
| Conferma senza righe spuntate | Nessuna azione spuntata al momento dell'evento | La cancellazione non rimuove righe né tocca altri dati | Operazione innocua |
| Undo singolo pendente | Snackbar/riferimento undo della Story 8.8 presente alla conferma | Dismiss o invalidazione dell'undo pendente; non può ripristinare un'azione dopo la conclusione | Non interferisce con lo snackbar di checkout |

</frozen-after-approval>

## Code Map

- `app/src/main/java/it/togo/app/data/database/dao/ActionItemDao.kt` -- `deleteChecked()` esegue già `DELETE FROM ACTION_ITEM WHERE stato_spunta = 1`; riusare questa query.
- `app/src/main/java/it/togo/app/domain/repository/ShoppingListRepository.kt` -- aggiungere il contratto repository dedicato alla cancellazione delle azioni spuntate.
- `app/src/main/java/it/togo/app/data/repository/RoomShoppingListRepository.kt` -- implementare il contratto delegando al DAO su `Dispatchers.IO`; non chiamare `checkout()`.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt` -- `handleConcludeActions()` oggi registra solo l'intento; delegare al repository e riportare errori senza creare undo. Invalidare anche `_deletedActionForUndo` e il relativo messaggio/counter al momento della conferma.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` -- `LaunchedEffect` presenta lo snackbar undo della cancellazione singola; alla conferma dismissere l'eventuale snackbar azione pendente.
- `app/src/test/java/it/togo/app/presentation/activelist/ActiveListViewModelActionOrderingTest.kt` -- sostituire il test di confine 9.2, ora superato, con copertura della delega e della sparizione reattiva delle azioni spuntate, preservando quelle attive.
- `app/src/test/java/it/togo/app/data/repository/RoomShoppingListRepositoryCheckoutTest.kt` -- pattern per istanziare il repository Room nei test; aggiungere copertura mirata del bulk delete se il setup consente di inserire e verificare azioni.

## Tasks & Acceptance

**Execution:**
- [x] `ShoppingListRepository.kt`, `RoomShoppingListRepository.kt` -- esporre e implementare un metodo bulk dedicato basato su `ActionItemDao.deleteChecked()` -- mantenere la mutazione nel repository e isolata dalla spesa.
- [x] `ActiveListViewModel.kt`, `ActiveListScreen.kt` -- collegare `ConcludeActions` alla mutazione IO, invalidare undo singolo pendente e gestire il fallimento senza snackbar undo -- rendere effettiva la conferma irreversibile.
- [x] `ActiveListViewModelActionOrderingTest.kt` e test repository Room pertinenti -- aggiornare/aggiungere test per checked rimosse, unchecked preservate e nessun checkout/storico -- coprire il contratto attraverso i livelli interessati. Test non eseguiti su richiesta dell'utente.

**Acceptance Criteria:**
- Given azioni spuntate e non spuntate, when l'utente conferma “Concludi attività”, then vengono eliminate da `ACTION_ITEM` solo le spuntate e le non spuntate restano nelle rispettive sezioni.
- Given la conferma della conclusione, when la mutazione termina, then nessun record di `HISTORICAL_ITEM` o conteggio d'acquisto viene scritto o incrementato e `SHOPPING_ITEM` resta invariata.
- Given la conclusione confermata, when la UI si aggiorna, then non viene offerto undo per le azioni concluse e un undo singolo pendente non resta utilizzabile.
- Given nessuna azione spuntata, when arriva l'evento di conferma, then nessuna azione non spuntata o voce acquisto viene eliminata.

## Implementation Notes

- Stato della storia riallineato a `done` il 2026-11-12 su indicazione esplicita dell'utente; build e test restano non eseguiti e sono indicati nella sezione Verification.
- Implementata `deleteCheckedActions()` nel repository, delegando a `ActionItemDao.deleteChecked()` su `Dispatchers.IO`; la query elimina esclusivamente righe checked in `ACTION_ITEM`.
- `ConcludeActions` ora invalida l'undo singolo pendente, chiude lo snackbar di cancellazione singola e avvia la cancellazione senza snackbar/undo del bulk. Le cancellazioni singole ancora in corso non possono riattivare un undo dopo la conferma.
- Aggiornati i test ViewModel e repository Room per azioni checked/active, undo pendente e invarianti di lista spesa/storico.
- `git diff --check` eseguito e senza errori di whitespace.
- Build e test non eseguiti per vincolo dell'utente; l'audit di esecuzione della matrice e la conferma comportamentale restano da fornire dall'utente.

## Verification

**Commands (da eseguire dall'utente; non eseguiti in questo incarico):**
- `./test-android.ps1` -- atteso: test Android verdi.
- `./build-android.ps1` -- atteso: build Android completata.

## Spec Change Log

## Review Triage Log
