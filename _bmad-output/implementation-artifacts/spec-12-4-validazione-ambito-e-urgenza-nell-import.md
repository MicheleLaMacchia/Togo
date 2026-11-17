---
title: '12.4 Validazione ambito e urgenza nell’import'
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

**Problem:** L’importazione delle azioni non rende esplicita la validazione dei campi: ambiti omessi devono ricadere su «Altro», ma urgenze mancanti o non riconosciute non devono essere accettate. Un blocco `[DA FARE]` vuoto deve essere rifiutato.

**Approach:** Validare i campi delle azioni durante la preparazione dell’anteprima, prima di produrre un piano importabile. Usare l’ambito `Altro` quando manca e restituire l’errore richiesto per un’urgenza mancante o non valida; rifiutare un blocco senza azioni anche quando il testo contiene prodotti.

**Vincoli:** Non eseguire build o test; i test possono essere aggiornati ma restano da eseguire a cura dell’utente. Non fare stage o commit. Limitare modifiche e comportamento alla Story 12.4; non anticipare la Story 12.5 o successive.

</frozen-after-approval>

## Implementation Notes

- `ShoppingListImportParser` conserva le righe `[DA FARE]`; `ImportShoppingListUseCase.preview` applica i fallback. L’ambito inizia già da `ActionScope.ALTRO`; `ActionScope.fromLabel` mantiene il fallback esistente. `ActionUrgency.fromLabel` converte attualmente valori mancanti/non validi in `A_TEMPO_PERSO`: la validazione d’import deve intercettarli senza cambiare il contratto generale dell’enum.
- Un blocco senza righe azione deve fallire anche in un import misto. Messaggio scelto per questo caso: `Il blocco [DA FARE] è vuoto.`; il requisito non impone una stringa specifica.
- Discrepanza tracking precedente: l’utente dichiara completata la Story 12.3; la spec 12.3 è `done`, ma `sprint-status.yaml` la indica `review`. Si prende come stato funzionale la dichiarazione dell’utente e non si riconcilia qui quella chiave; build e test della 12.3 risultano non eseguiti nelle note disponibili e non vengono dichiarati superati.
- Nessuna migrazione o operazione esterna prevista. Modifiche circoscritte alla validazione del caso d’uso, ai relativi test e al tracking della Story 12.4.
- Implementazione: `ImportShoppingListUseCase.preview` mantiene `Altro` come ambito iniziale, richiede un’urgenza riconosciuta per ogni azione e rifiuta `[DA FARE]` senza righe azione con `Il blocco [DA FARE] è vuoto.`. Aggiunti test in `ImportShoppingListUseCaseTest` per ambito omesso, urgenza assente/non valida, cambio ambito e blocco vuoto da solo/misto; non eseguiti per vincolo utente.
- File aggiornati: `ImportShoppingListUseCase.kt`, `ImportShoppingListUseCaseTest.kt`, `sprint-status.yaml` e questa spec. La Story 12.4 passa a `review`; 12.3 resta `review` nel tracking, con nota che registra la dichiarazione utente e rimanda a questa spec.

## Review Triage Log

- low — mancava un test per urgenza non valida con prodotti presenti; aggiunto test che verifica l’errore di validazione nell’import misto.
- low — mancava il caso del blocco con sole intestazioni; aggiunto alla copertura del blocco senza azioni.
- low — mancava la prova che una nuova intestazione di ambito azzeri l’urgenza precedente; aggiunto test con seconda azione priva di urgenza.
- Verifica della revisione dopo le correzioni: nessun ulteriore finding.
