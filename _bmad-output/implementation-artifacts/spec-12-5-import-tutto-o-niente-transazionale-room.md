---
title: '12.5 Import tutto-o-niente transazionale Room'
type: 'feature'
created: '2026-11-17'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-12-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="Intent e criteri forniti dall'utente">

## Intent

**Problem:** L'import può scrivere prodotti, categorie, voci e azioni in sequenza; un errore successivo può lasciare dati parziali. Gli errori di validazione delle righe `[DA FARE]` non indicano quale riga correggere.

**Approach:** Validare il blocco `[DA FARE]` prima di abilitare l'import e includere numero di riga e motivo nell'errore. Applicare tutte le scritture del piano valido in un'unica transazione Room e propagare gli errori fino al rollback.

**Vincoli:** Non eseguire build o test, nemmeno tramite script, IDE o container; i test possono essere aggiunti ma non eseguiti. Non fare stage o commit, non ripristinare file e limitare il lavoro alla Story 12.5. Non anticipare la Story 12.6 o successive. La Story 12.4 è considerata completata su dichiarazione dell'utente; registrare la discrepanza tra spec e sprint tracking senza dichiarare verifiche tecniche non eseguite.

</frozen-after-approval>

## Implementation Notes

- `ShoppingListRepository.withImportTransaction` definisce il confine applicativo; `RoomShoppingListRepository` lo implementa con `TogoDatabase.withTransaction`, condiviso dai repository catalogo/lista/azioni tramite il singleton Koin.
- L'esecuzione del piano racchiude in quella transazione tutte le creazioni di categoria/prodotto, gli aggiornamenti/inserimenti della lista e le azioni. Le eccezioni escono dal blocco Room e vengono convertite in `ImportExecutionResult.Error` solo dopo il rollback; la cancellazione continua a propagarsi.
- La preview segnala numero di riga e motivo per urgenza non valida/mancante e per le righe non strutturate nel blocco `[DA FARE]`. Il ViewModel inoltra già l'errore al dialogo esistente; non sono state aggiunte anteprima o messaggi di esito della Story 12.6.
- Test aggiornati per righe di errore e import misto non scrivente; il test Room esercita il caso d'uso e simula un errore dopo creazione di categoria/prodotto/sinonimo, inserimento e aggiornamento lista e prima azione. Verifica il rollback dell'intero stato; non eseguito per vincolo dell'utente.
- Discrepanza 12.4: la spec era già `done` ma `sprint-status.yaml` era `review`. Su dichiarazione dell'utente, la chiave 12.4 è stata allineata a `done`; il commento esplicita che è una riconciliazione amministrativa e non attesta build o test superati.
- File di prodotto/test aggiornati: `ShoppingListRepository.kt`, `RoomShoppingListRepository.kt`, `RepositoryModule.kt`, `ImportShoppingListUseCase.kt`, `ImportShoppingListUseCaseTest.kt`, `RoomShoppingListRepositoryCheckoutTest.kt`. Tracking/spec aggiornati: `sprint-status.yaml` e questa spec.
- Verifica statica eseguita: `git diff --check` senza errori; nessuna build o test eseguiti.

## Review Triage Log

- patch — il controllo del blocco vuoto poteva nascondere una riga malformata non indentata dietro l'errore generico di blocco vuoto; ora tale errore si applica solo a righe vuote/intestazioni valide senza azioni e la riga malformata raggiunge la validazione con numero e motivo.
- false — un ambito sconosciuto continua a ricadere in `Altro` per contratto esplicito di `ActionScope.fromLabel`; la Story 12.4 definisce il fallback per ambito mancante. Non cambio questa semantica nell'ambito dell'atomicità della 12.5.
- patch — il test Room iniziale non copriva il caso d'uso completo né il ripristino di una voce aggiornata; ora il test integra il caso d'uso reale, forza un errore sulla seconda azione e verifica il rollback di catalogo/sinonimo, lista (inserimento e aggiornamento) e prima azione.

