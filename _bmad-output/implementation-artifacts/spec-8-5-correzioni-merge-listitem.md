---
title: 'Story 8.5 Correzioni merge e immutabilità ListItem'
type: 'bugfix'
created: '2026-10-30'
status: 'done'
story: 8.5
epic: 8
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-8-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/togo-tasks-unificata/inputs/togo-specifichev0.14.md'
---

<frozen-after-approval reason="human-owned intent - do not modify unless human renegotiates">

## Intent

**Problem:** Il repository della Spesa fonde righe con note diverse, non riconosce alcuni alias noti delle unità e può sostituire una riga esistente quando riceve un ID duplicato. Questi casi perdono informazioni o impediscono il merge previsto.

**Approach:** Allineare la chiave di compatibilità alle regole Epic 8, preservare i valori originali delle unità e rendere l'aggiunta idempotente rispetto a un ID già presente. Aggiungere test deterministici sui confini del merge e sulla protezione delle righe acquistate.

**Boundaries & Constraints**

**Always:** Preservare `purchaseCount` e i dati di ogni riga `PURCHASED`; mantenere unità custom non riscritte; eseguire merge e controlli ID in transazioni.

**Never:** Modificare migrazioni, schema, UI, voice, condivisione, sprint status o le modifiche preesistenti del worktree. Scrivere i test senza eseguirli.

</frozen-after-approval>

## Implementation Notes

- `RoomListItemRepository.add` ora tratta un ID già persistito come richiesta ripetuta e restituisce la riga esistente senza modificarla. `ListItemDao.insert` usa `ABORT` come protezione ulteriore contro la sostituzione per conflitto.
- La chiave condivisa da add, restore e reAdd confronta anche le note normalizzate e canonicalizza gli alias noti delle unità. Il testo originale dell'unità resta persistito; grammi e ettogrammi restano unità distinte.
- `RoomListItemRepositoryTest` copre alias, unità diverse, note, brand/variante/conservazione e ID duplicati. Il reviewer finale ha evidenziato alias comuni mancanti; la mappa e i test sono stati estesi a chili, ettogrammi, lattine, scatole e rotoli.
- I test non sono stati eseguiti come richiesto; `git diff --check` non ha rilevato errori di whitespace.
- Modifiche di codice/test limitate a `RoomListItemRepository.kt`, `ListItemDao.kt` e `RoomListItemRepositoryTest.kt`; creata questa spec di lavoro. Le modifiche preesistenti e lo sprint status sono rimasti invariati.

## Review Triage Log

- medium — La chiave iniziale non riconosceva alcuni alias comuni dell'unità; aggiunti canonicalizzazioni e test di regressione mantenendo distinti grammi ed ettogrammi.
- low — Il test iniziale verificava due soli alias; ampliato il set di casi per fissare i mapping aggiunti.

