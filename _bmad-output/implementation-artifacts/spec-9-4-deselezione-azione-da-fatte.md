---
title: '9.4 Deselezione azione da Fatte'
type: 'feature'
created: '2026-11-12'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-9-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-9-3-eliminazione-azioni-spuntate-senza-undo-e-storico.md'
---

<frozen-after-approval reason="intento esplicito della Story 9.4">

## Intent

**Problem:** La Story 9.4 richiede che un'azione in Fatte possa essere deselezionata e ripresa nel gruppo attivo corretto.

**Approach:** Conservare il flusso esistente di deselezione e aggiungere copertura del ViewModel per verificare stato, ambito, urgenza e ordinamento dell'azione reinserita.

</frozen-after-approval>

## Implementation Notes

- Il flusso UI/ViewModel esistente invia `CheckOff(id, false)`, rimuove l'azione da `checkedActions`, imposta `isChecked = false` e la reinserisce in `actions`; `actionGroups` applica l'ordinamento per ambito, urgenza e testo.
- Aggiunta copertura ViewModel della deselezione, della persistenza, della conservazione di ambito/urgenza e dell'ordinamento dei gruppi attivi. Il test UI verifica anche il callback della checkbox e la semantica unchecked dopo il cambio di stato. Nessuna modifica al codice di produzione.
- Build e test non eseguiti per vincolo dell'utente.

## Review Triage Log

- medium -- Il primo test non dimostrava la persistenza: ora attende l'emissione aggiornata dal repository dopo `checkOffAction(id, false)`.
- medium -- La copertura separata non dimostrava la semantica unchecked della riga UI: il test Fatte ora ricompone la riga con `isChecked = false` e verifica la checkbox deselezionata; il test ViewModel verifica il passaggio alla lista attiva.
- medium -- Il tracker dichiarava 9.3 `done` mentre la sua spec era `in-progress`: stati riallineati a `done` su indicazione esplicita dell'utente, lasciando documentato che build e test non sono stati eseguiti.
