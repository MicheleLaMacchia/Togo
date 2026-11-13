---
title: '11.1 Dialog a tre scelte con conteggi'
type: 'feature'
created: '2026-11-13'
status: 'done'
baseline_commit: 'de9d65863d30be971dd1c4d78c4e7401268f0a9e'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-11-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="intento esplicito della Story 11.1">

## Intent

**Problem:** L'icona Condividi avvia direttamente la condivisione della sola spesa, senza permettere di scegliere tra prodotti, azioni e contenuti combinati.

**Approach:** Aprire da entrambe le pagine un dialog con tre scelte selezionabili e conteggi delle voci attive; preselezionare la lista della pagina corrente e offrire i pulsanti `Condividi` e `Annulla`.

## Boundaries & Constraints

**Always:** Contare solo voci non spuntate; riusare i conteggi già esposti dalla UI state; default Spesa su Spesa e Azioni su Da fare; usare le etichette esatte dell'accettazione.

**Never:** Implementare il formato `[DA FARE]`, l'oggetto dinamico o la matrice dei contenuti vuoti (storie 11.2–11.5); cambiare i dati delle liste.

**Decisione approvata:** Selezionando `Condividi`, la scelta apre lo ShareSheet con i contenuti corrispondenti e il dialog si chiude. La generazione completa di azioni e contenuti combinati rientra in questa storia quanto basta per rendere funzionante il primario; non si anticipano l'oggetto dinamico e la matrice dei casi vuoti.

</frozen-after-approval>

## Code Map

- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` — click Condividi, pagina corrente, conteggi, dialog Compose e attuale apertura dello Sharesheet.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListUiState.kt` — `currentPage`, `activeCount` e `activeActionsCount` rappresentano le scelte e i conteggi richiesti.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt` — deriva il conteggio Spesa da voci non spuntate e `actions` da azioni non spuntate.
- `app/src/test/java/it/togo/app/presentation/activelist/ActiveListPagerContractTest.kt` — test Compose della testata e test del conteggio azioni, estendibile per dialog, etichette e default.
- `app/src/test/java/it/togo/app/presentation/activelist/ActiveListAccessibilityTest.kt` — copertura d'integrazione della testata della Lista Attiva e apertura dialog dal pulsante Condividi.
- `app/src/main/java/it/togo/app/domain/usecase/ShareListUseCase.kt` — attuale generazione intent solo Spesa; non estenderla a formati futuri in questa storia.

## Tasks & Acceptance

**Execution:**
- [x] `ActiveListScreen.kt` — sostituire l'apertura immediata del chooser con dialog a tre scelte, conteggi e default contestuale.
- [x] `ActiveListPagerContractTest.kt` — coprire dialog, default, pulsanti e composizione dell'intent per ciascuna scelta; non eseguire test.
- [x] `ActiveListAccessibilityTest.kt` — coprire apertura dal pulsante Condividi su entrambe le pagine e chiusura alla conferma; non eseguire test.
- [x] `sprint-status.yaml` — riallineare 10.6 da `review` a `done` su indicazione esplicita dell'utente e annotare che non equivale a build/test superati.

**Acceptance Criteria:**
- Given l'utente tocca Condividi su Spesa o Da fare, when il dialog si apre, then mostra `Solo spesa (N)`, `Solo azioni (M)` ed `Entrambe (N+M)`.
- Given il dialog si apre da Spesa, when nessuna scelta è stata cambiata, then è selezionata Solo spesa; da Da fare è selezionata Solo azioni.
- Given il dialog è aperto, then il primario è `Condividi` e il secondario `Annulla`.

## Implementation Notes

Vincoli esecutivi: non avviare build o test in locale, tramite script, IDE o container; non fare stage né commit; limitare le modifiche ai file previsti da questa storia e non anticipare storie successive. I test si possono aggiornare; build e test restano da eseguire all'utente.

La Story 10.6 risultava `review` nel tracking ma la spec era già `done` e l'utente l'ha dichiarata completata. La riallineo a `done` su indicazione esplicita; questo non costituisce evidenza di build o test superati.

La review ha rilevato copertura mancante sul collegamento tra selezione e contenuto dell'intent. Ho estratto la composizione in `buildShareIntent` e aggiunto verifiche dei payload e della UI di apertura/default/chiusura. La review non ha eseguito test.

L'utente ha eseguito `test-build-android.ps1`; la compilazione dei test si è fermata sull'import non risolto `androidx.compose.ui.test.assertDoesNotExist`. Ho rimosso l'import: `assertDoesNotExist()` è un metodo di `SemanticsNodeInteraction`. Non ho rieseguito build o test.

Nel successivo run utente, tre test Compose hanno trovato due nodi con testo `Condividi` (testata e primario del dialog). Ho aggiunto il tag semantico `share_dialog_confirm` al primario e i test interagiscono con quel nodo univoco. Non ho rieseguito build o test.

Nel successivo run utente è emerso l'import mancante di `onNodeWithTag` in `ActiveListAccessibilityTest`; ho aggiunto l'import. Build e test non sono stati rieseguiti.

## Review Triage Log

- patch — La copertura non verificava che il tap sull'icona aprisse il dialog; `ActiveListAccessibilityTest` ora compone la schermata e controlla l'apertura dal pulsante su entrambe le pagine.
- patch — I test non verificavano che il pulsante primario chiudesse il dialog; il test d'integrazione seleziona Azioni, conferma e verifica la chiusura.
- patch — I test non verificavano la composizione del contenuto di ciascuna scelta; il test di `buildShareIntent` controlla testo Spesa, testo Azioni e unione Entrambe.
- patch — I test non verificavano il payload completo dell'intent; lo stesso test controlla `ACTION_SEND`, `text/plain` e l'oggetto conservato.
- patch — I test non legavano la preselezione alla pagina corrente della schermata; il test d'integrazione cambia pagina prima di riaprire il dialog e controlla Solo azioni preselezionato.
- false — Il rilievo su Back/tap esterno riguardava solo copertura: `AlertDialog` inoltra `onDismissRequest` a `onDismiss`, mentre il pulsante Annulla invoca lo stesso callback.
- low — Rifiutato: eccezioni durante la preparazione della condivisione erano già possibili nel flusso precedente; la gestione aggiungerebbe rami per una condizione rara non introdotta da questa story.
- low — Rifiutato: l'assenza di un'app che gestisca lo Sharesheet era già possibile nel flusso precedente; la gestione aggiungerebbe rami per una condizione rara non introdotta da questa story.
## Verification

**Comandi da eseguire dall'utente (non eseguiti da questo agente):**
- `.\gradlew.bat testDebugUnitTest` — atteso: suite unit test superata.
- `.\gradlew.bat assembleDebug` — atteso: build debug completata.
