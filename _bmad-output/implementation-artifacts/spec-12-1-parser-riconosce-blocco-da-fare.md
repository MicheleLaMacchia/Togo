---
title: '12.1 Parser riconosce blocco [DA FARE]'
type: 'feature'
created: '2026-11-16'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-12-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="Story 12.1 richiesta dall'utente">

## Intent

**Problema:** Il parser interpreta ogni intestazione tra parentesi quadre come un Livello 1 tassonomico. Di conseguenza `[DA FARE]` viene confuso con una categoria prodotto e le azioni non sono rappresentate nel risultato del parsing.

**Approccio:** Riconoscere `[DA FARE]` prima delle intestazioni tassonomiche e conservarne le righe come sezione azioni grezza nel risultato parse. Senza il marker, il risultato non conterrà una sezione azioni. Non interpretare, validare o importare le righe delle azioni in questa storia.

</frozen-after-approval>

## Implementation Notes

- Worktree iniziale pulito sul branch `dev`; contesto Epic 12 compilato dalle fonti di pianificazione.
- Riconoscimento strutturale nella sezione di parsing delle intestazioni; linee grezze e numeri di riga sono preservati per le successive fasi di parsing/validazione.
- Ambito limitato a `ShoppingListImportParser`, risultato di parsing e relativi test. Nessuna modifica a `ImportShoppingListUseCase`, persistenza, anteprima, deduplica o validazione delle azioni.
- La Story 11.5 è stata portata da `review` a `done` su dichiarazione dell'utente; la nota conserva il precedente fallimento del test Docker e chiarisce che la riesecuzione è ancora da fare.
- File modificati per 12.1: `app/src/main/java/it/togo/app/domain/importlist/ShoppingListImportParser.kt` e `app/src/test/java/it/togo/app/domain/importlist/ShoppingListImportParserTest.kt`. Creati il contesto Epic 12 richiesto dal workflow e questa spec.
- Test scritti per input misto in entrambi gli ordini, blocco azioni solo strutturale e testo senza marker; nessun test o build eseguito. `git diff --check` non segnala errori di whitespace.

## Review Triage Log

- patch — aggiunto il numero di riga del marker in `ParsedActionSection` per preservare la posizione dell'intestazione anche quando il blocco è vuoto.
- patch — aggiunto test azioni seguite da prodotti: verifica la fine della modalità azioni e la ripresa del parsing tassonomico.
- patch — aggiornati KDoc del risultato e parser per distinguere righe prodotto validate dalle righe azione conservate senza validazione.
- patch — copiato l'elenco di righe azione con `toList()` prima di restituirlo, evitando che la lista mutabile locale venga esposta nel risultato.
