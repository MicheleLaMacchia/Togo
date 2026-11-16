---
title: '12.3 Deduplica azioni per testo + ambito + urgenza con accenti significativi'
type: 'feature'
created: '2026-11-16'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-12-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="Intent e criteri forniti dall'utente">

## Intent

**Problem:** Durante l'importazione, il piano contiene anche azioni già presenti nella lista, creando duplicati.

**Approach:** Prima di presentare il piano, saltare le azioni già esistenti con testo equivalente dopo normalizzazione, stesso ambito e stessa urgenza. La normalizzazione ignora maiuscole e spazi, preserva gli accenti e non dipende dallo stato spuntato. Ambito o urgenza diversi mantengono l'azione distinta.

**Vincoli:** Nessuna build o test sarà eseguito; non modificare le Story 12.4 e successive; non committare né fare stage.

</frozen-after-approval>

## Implementation Notes

- Riutilizzare `ActionTextNormalizer` e `ShoppingListRepository.checkDuplicateAction`: la chiave NFC/minuscola/spazi collassati conserva gli accenti; il DAO confronta chiave, ambito e urgenza senza escludere le azioni spuntate.
- Filtrare il piano d'import in anteprima e ricontrollare prima di scrivere, così eventuali modifiche intervenute dopo la preview non creano duplicati. Conservare l'ordine; un piano solo-azioni resta valido anche se tutte le righe sono già presenti.
- Discrepanza Story 12.2: la spec esistente è `done`, la nota nello sprint status descrive implementazione e revisione statica concluse ma tracking ancora `review`. In base alla dichiarazione esplicita dell'utente, riconciliare il tracking a `done`, senza dichiarare build/test superati.
- File modificati: `app/src/main/java/it/togo/app/domain/usecase/ImportShoppingListUseCase.kt`, `app/src/test/java/it/togo/app/domain/usecase/ImportShoppingListUseCaseTest.kt`, `sprint-status.yaml` e questa spec.
- Verifica eseguita: `git diff --check`. Build e test non eseguiti per vincolo esplicito dell'utente.

## Review Triage Log

- patch — il controllo avveniva solo in anteprima e un'azione aggiunta prima della conferma poteva duplicarsi; il repository viene ricontrollato in `execute`.
- patch — il test del duplicato spuntato verificava soltanto l'anteprima; ora esegue il piano e controlla conteggio, assenza della riapertura e azione esistente invariata.
- patch — mancava copertura dei duplicati interni al testo importato; aggiunte righe ripetute con differenze di maiuscole/spazi e mantenute asserzioni su ambito e urgenza distinti.
