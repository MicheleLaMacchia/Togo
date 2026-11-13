---
title: '10.5 Back da Da fare torna a Spesa'
type: 'feature'
created: '2026-11-12'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-10-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="intento esplicito della Story 10.5">

## Intent

**Problem:** Dalla pagina Da fare, il Back di sistema deve riportare alla lista Spesa invece di avviare la chiusura dell'app.

**Approach:** Intercettare il Back predittivo quando la pagina attiva è Da fare e, al completamento del gesto, selezionare Spesa tramite il flusso di stato già collegato al pager. Se il gesto viene annullato, la pagina resta Da fare.

</frozen-after-approval>

## Acceptance Criteria

- Given la pagina Da fare è attiva, when il Back predittivo termina, then viene selezionata Spesa, il pager la mostra e l'app resta aperta senza anteprima di chiusura.
- Given la pagina Da fare è attiva, when il gesto Back viene annullato, then la pagina resta Da fare e non viene inviato `SelectPage(SHOPPING)`.
- Given la pagina Spesa è attiva, when viene premuto Back, then questo handler non intercetta il comportamento di sistema.

## Implementation Notes

- In `AppNavigator`, sostituito il `BackHandler` della pagina Da fare con `PredictiveBackHandler`, attivo solo su `ACTIONS`. Il flusso viene consumato fino al completamento: la selezione di `SHOPPING` è inviata solo se il gesto termina normalmente; l'annullamento non modifica lo stato. Il pager esistente osserva `currentPage` e anima il cambio con `animateScrollToPage`.
- `activity-compose` 1.9.1 era già presente; nessuna dipendenza aggiunta. Aggiunti test Robolectric in `AppNavigatorTest` per il Back completato e annullato. L'utente ha riportato che `test-build-android.ps1` si è fermato in `compileDebugKotlin` per l'import/opt-in non risolto di `ExperimentalActivityApi`, prima dell'esecuzione dei test.
- Correzione dopo il log utente: rimossi l'import e l'opt-in a `ExperimentalActivityApi`, simbolo non disponibile nella versione effettiva risolta; `PredictiveBackHandler` era invece riconosciuto dal compilatore. Build e test aggiornati restano da eseguire dall'utente.
- Discrepanza Story 10.4: il tracking `in-progress` è stato riallineato a `done` su indicazione esplicita dell'utente. La nota storica dell'errore di compilazione resta nel tracking/spec come contesto, senza dedurre che la build o i test siano passati. La Story 10.5 è stata impostata a `in-progress` durante l'implementazione.
- Verifica affidata all'utente: `./gradlew :app:testDebugUnitTest` (oppure `.\test-android.ps1`) e `.\build-android.ps1`. Nessuno dei comandi è stato eseguito in questa sessione.
- Controllo manuale da fare dopo l'installazione: da Da fare iniziare un back predittivo e annullarlo, poi completarlo; verificare che l'annullamento lasci Da fare, il completamento mostri Spesa e non compaia l'anteprima di chiusura dell'app.

## Review Triage Log

- medium — patch: il test iniziale non copriva l'annullamento; aggiunto un caso che avvia e annulla il gesto tramite `OnBackPressedDispatcher` e controlla che non parta `SelectPage(SHOPPING)`.
- medium — patch: il test iniziale usava contenuto segnaposto; ora compone `ActiveListPager` e `rememberActiveListPagerState` reali, oltre a controllare che l'Activity non termini. La resa visiva dell'anteprima di sistema resta da verificare manualmente sul dispositivo.
- low — patch: la spec non esplicitava gli esiti; aggiunti criteri Given/When/Then per back completato, annullato e handler disattivo su Spesa.

