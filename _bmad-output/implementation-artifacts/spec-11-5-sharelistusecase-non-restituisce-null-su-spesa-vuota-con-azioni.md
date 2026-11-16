---
title: '11.5 ShareListUseCase non restituisce null su spesa vuota con azioni'
type: 'bugfix'
created: '2026-11-16'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-11-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="Story 11.5 richiesta dall'utente">

## Intent

**Problema:** `ShareListUseCase` restituisce `null` se non ci sono voci spesa, senza considerare che l'utente può aver selezionato la condivisione delle sole azioni attive.

**Approccio:** Aggiungere al caso d'uso il percorso di condivisione “Solo azioni”, riusando il formatter canonico e collegandolo alla selezione esistente. Il testo conterrà `[DA FARE]` e le azioni attive, anche con lista spesa vuota.

</frozen-after-approval>

## Implementation Notes

- Stato iniziale: worktree pulito su `dev`; Story 11.4 ha spec `done` e sprint `review`. Su dichiarazione esplicita dell'utente, il tracking 11.4 è stato riallineato a `done`, precisando che `review` non attestava build/test superati.
- `ShareListUseCase.Part` distingue `SHOPPING` e `ACTIONS`; il secondo seleziona le azioni via `ShoppingListRepository.getActions()` e riusa `buildSharedActionsText`, senza passare dal controllo di lista spesa vuota. Il percorso shopping predefinito resta invariato.
- `ActiveListViewModel` e la selezione della UI inoltrano la parte tipizzata al caso d'uso; la composizione `BOTH` e i relativi formatter restano invariati.
- File di codice modificati: `app/src/main/java/it/togo/app/domain/usecase/ShareListUseCase.kt`, `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt`, `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt`. Aggiunto `app/src/test/java/it/togo/app/domain/usecase/ShareListUseCaseTest.kt` con casi azioni attive/spuntate, nessuna azione attiva e percorso shopping predefinito.
- Review Blind Hunter: la proposta di tipizzare la selezione è stata applicata; il test del percorso shopping predefinito è stato aggiunto. L'asserzione che mancasse copertura UI è respinta: `ActiveListAccessibilityTest` verifica già la selezione “Solo azioni” fino al payload dello Sharesheet; il test unitario 11.5 copre la spesa vuota.
- Risultato del test-build Docker fornito dall'utente il 2026-11-16: `assembleDebug` completato; 722 test completati con un fallimento in `ActiveListAccessibilityTest.conferma condivisione invia testo e oggetto corretti nello sharesheet per ogni scelta`. Il payload della scelta `Entrambe` mancava del testo spesa perché il ramo `BOTH` non richiedeva l'intent shopping; ripristinato il caricamento della spesa per `SHOPPING` e `BOTH`, mantenendo il percorso azioni per `ACTIONS`. Test non rieseguiti dopo la correzione.
- `git diff --check` eseguito senza errori di whitespace nella sessione precedente. Nessuna build o test eseguiti dall'agente dopo la correzione; serve la riesecuzione dell'utente.

## Review Triage Log

- false — la copertura UI esistente in `ActiveListAccessibilityTest` verifica la selezione Solo azioni fino al testo e oggetto nel payload; `ShareListUseCaseTest` copre inoltre la lista spesa vuota richiesta dalla 11.5.
- patch — la firma booleana lasciava implicita la parte condivisa; sostituita con `ShareListUseCase.Part` (`SHOPPING`/`ACTIONS`).
- patch — mancava regressione per la chiamata predefinita shopping dopo la variazione della firma; aggiunto test del testo e oggetto shopping.
