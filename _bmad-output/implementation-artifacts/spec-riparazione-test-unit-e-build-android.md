---
title: 'Riparazione test unit e build Android'
type: 'bugfix'
created: '2026-10-30'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-8-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-8-7-adattamento-ui-shopping-esistente-e-regressione.md'
---

<frozen-after-approval reason="intent autorizzato dall'utente">

## Intent

**Problem:** La suite Android si blocca quando MockK tenta l'attach dinamico di Byte Buddy nel container e presenta inoltre test non allineati alla versione Room 7 e alla UI attiva della Story 8.7. Questo impedisce di convalidare l'app e di produrre la build.

**Approach:** Avviare il listener di attach nella JVM dei test e correggere le sole aspettative di test per la catena Room 1→7 e gli elementi attivi `TO_BUY`. Mantenere invariati comportamento UI, migrazioni di produzione, schema e stato sprint.

</frozen-after-approval>

## Implementation Notes

- Il container impediva l'attach dinamico: l'output Byte Buddy indicava che il processo target non rispondeva entro 10.5 secondi. `-Djdk.attach.allowAttachSelf=true` non ha risolto; `-XX:+StartAttachListener` ha consentito il test mirato `AppNavigatorTest` di completarsi con `BUILD SUCCESSFUL`.
- Aggiornato il solo test di installazione da asset per seguire la catena v1→v7 e verificare la rimozione delle tabelle legacy; la verifica isolata v5→v6 resta invariata.
- Allineate due aspettative Compose alla UI attiva `TO_BUY`. Nessuna modifica a comportamento UI, migrazioni di produzione, schema Room o stato sprint.
- Allineati tre test AddItem ai default esistenti (`quantityText = "1"`, `selectedUnit = PIECE`) per creazione manuale e prefill vocale senza quantita.
- Aggiunta una migrazione integrata 5-to-7 con righe legacy: verifica che la migrazione 6-to-7 rimuova le tabelle legacy preservando le righe copiate in `LIST_ITEM`.
- Validazione: `test-android.ps1` completato con `BUILD SUCCESSFUL` e `Test JVM completati con successo`; `build-android.ps1` completato con `BUILD SUCCESSFUL`; APK prodotto in `app/build/outputs/apk/debug/app-debug.apk`; `git diff --check` senza errori.
- Triage one-shot: il rilievo sulla copertura dati della migrazione 6-to-7 e risolto dal nuovo test. Le osservazioni su assert Compose preesistenti (esclusione degli item acquistati e rilevazione del clipping) restano fuori dallo scope di questa riparazione; non hanno causato i fallimenti rilevati.

