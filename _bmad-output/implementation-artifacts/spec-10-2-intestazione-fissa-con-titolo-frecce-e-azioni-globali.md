---
title: '10.2 Intestazione fissa con titolo, frecce e azioni globali'
type: 'feature'
created: '2026-11-12'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-10-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
---

<frozen-after-approval reason="intento esplicito della Story 10.2">

## Intent

**Problem:** L'intestazione della lista deve identificare la pagina corrente e offrire la navigazione e le azioni globali senza scorrere insieme ai contenuti.

**Approach:** Allineare la fascia fissa alla composizione UX: Storico a sinistra solo su Spesa, titolo con frecce al centro, Condividi e Impostazioni a destra su entrambe le pagine. Mostrare nel titolo il conteggio degli elementi non spuntati della pagina.

</frozen-after-approval>

## Implementation Notes

- Sostituita la `TopAppBar` con una fascia in `Scaffold.topBar` che segue l'ordine UX: Storico a sinistra, frecce e titolo centrati, Condividi e Impostazioni a destra. Le aree laterali bilanciate centrano il gruppo; gli inset della status bar sono rispettati, l'altezza minima e il bordo inferiore usano i token `appBar`, e il titolo usa il nuovo token `pageTitle` (20sp Bold) e può crescere a capo.
- Il conteggio Spesa ora proviene da tutte le voci non spuntate del repository, incluse quelle senza gruppo categoria; il ViewModel aggiorna il totale nelle transizioni di spunta/deselezione. Il conteggio Da fare continua a usare soltanto `actions` non spuntate.
- Aggiunta copertura Compose della navigazione e della visibilità delle azioni, più regressioni ViewModel per conteggi e transizioni di spunta e un controllo del token `pageTitle`. Esecuzione finale di `test-build-android.ps1`: 685 test, 0 fallimenti/errori/skipped; build APK riuscita in `app/build/outputs/apk/debug/app-debug.apk`.
- Discrepanza Story 10.1: l'utente l'ha dichiarata completata e `spec-10-1-horizontalpager-con-due-pagine.md` riporta `status: done`, mentre `sprint-status.yaml` riportava `review`. Ho riconciliato il tracker a `done` e annotato la motivazione accanto alla voce. Questa decisione non dichiara build/test superati.

## Review Triage Log

- medium -- La nuova fascia custom non ereditava gli inset superiori della `TopAppBar`; aggiunto `WindowInsets.statusBars` per evitare sovrapposizioni con la status bar.
- medium -- Le larghezze laterali asimmetriche spostavano il gruppo titolo/frecce; ora due slot da 96dp centrano il gruppo rispetto allo schermo.
- medium -- Mancava una regressione per voci attive senza gruppo categoria e per le transizioni di spunta; conteggio sorgente aggiornato e test ViewModel aggiunto.

