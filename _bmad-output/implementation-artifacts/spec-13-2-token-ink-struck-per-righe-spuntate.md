---
title: '13.2 Token ink-struck per righe spuntate'
type: 'feature'
created: '2026-11-17'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-13-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="Intent e criteri definiti dall'utente">

## Intent

**Problem:** I nomi delle righe prodotto e azione spuntate usano `inkPrimary`; manca un token semantico dedicato allo stato completato, anche se barratura e checkbox sono già presenti.

**Approach:** Definire la coppia light/dark `ink-struck` prevista da DESIGN.md (`#1F2937` e `#CBD5E1`) e applicarla ai nomi di entrambe le righe quando sono spuntate. Conservare barratura e checkbox, senza opacità sul testo.

**Always:** Il colore del testo spuntato deve mantenere almeno 4,5:1 di contrasto, idealmente 7:1, rispetto allo sfondo effettivo della riga nei temi chiaro e scuro. Mantenere barratura e checkbox come segnali ridondanti.

**Never:** Non eseguire build o test (inclusi script, IDE o container); non fare stage o commit; non modificare file estranei; non anticipare Story 13.3 o successive.

</frozen-after-approval>

## Implementation Notes

- Worktree iniziale pulito su `dev`; nessuna modifica preesistente da preservare.
- Code map: `ui/theme/Color.kt` ospita token e schema colori; `presentation/activelist/ItemRow.kt` e `ActionRow.kt` rendono il nome riga e la decorazione; `ui/theme/DesignSystemTokenTest.kt` già controlla il contrasto del nome prodotto spuntato. `PresiSection` e `FatteSection` collegano i modelli checked alle rispettive righe e non richiedono cambi di flusso.
- DESIGN.md definisce `ink-struck: #1F2937` e `ink-struck-dark: #CBD5E1`. La coppia rispetta l'obiettivo AAA sulle superfici card attuali; test di contrasto mirati saranno aggiornati ma resteranno non eseguiti.
- Discrepanza: la spec 13.1 era `done`, ma `sprint-status.yaml` riportava `13-1-token-dark-mode-coppie-dark: review`. Il commento di tracking indicava implementazione/revisione statica concluse e build/test non eseguiti. Su indicazione dell'utente, allineata la sola voce 13.1 a `done` con commento esplicativo; questo stato amministrativo non certifica verifiche tecniche. Avviata 13.2 secondo la sincronizzazione BMAD.
- Nessuna migrazione o effetto esterno. Ambito previsto: token/schema colore, due componenti riga, test del design system, sprint status e questa spec. Lasciare invariati meta/badge, bordi/checkbox, opacità di superfici, semantica e navigazione.
- Comandi PowerShell per l'utente, non eseguiti: `.\gradlew.bat :app:testDebugUnitTest --tests it.togo.app.ui.theme.DesignSystemTokenTest` e `.\gradlew.bat :app:assembleDebug`.
- Implementazione: aggiunti `LightInkStruck` e `DarkInkStruck` in `Color.kt` e i corrispondenti accessori `inkStruck` nello schema; ItemRow e ActionRow applicano il token solo quando spuntate. Barratura e checkbox preesistenti restano invariate; testo meta e badge non rientrano nel cambiamento.
- Aggiornato `DesignSystemTokenTest.kt` per i valori, la selezione del token spuntato/non spuntato e il contrasto dei due nomi sul rispettivo sfondo card; il test controlla la soglia richiesta di 4,5:1. Calcolo WCAG 2.1 eseguito direttamente sui valori scelti: 14,68:1 chiaro (`#1F2937`/`#FFFFFF`) e 11,56:1 scuro (`#CBD5E1`/`#131B2E`), entrambi oltre l'ideale 7:1. Il test non è stato eseguito.
- File modificati: `Color.kt`, `ItemRow.kt`, `ActionRow.kt`, `DesignSystemTokenTest.kt`, `sprint-status.yaml` e questa spec. Allineata 13.1 a `done` su indicazione utente, con nota che separa lo stato amministrativo dall'evidenza tecnica; 13.2 avviata per la sincronizzazione di workflow.
- `git diff --check` eseguito senza errori di whitespace; nessuna build o test eseguiti.

## Review Triage Log

- patch — il test di contrasto non verificava che il testo checked selezionasse davvero `inkStruck`; introdotto `checkedRowTextColor` condiviso e asseriti i mapping checked/unchecked per entrambi i temi.
- patch — il test richiedeva 7:1 benché l'AC garantisca 4,5:1 e indichi 7:1 come ideale; le asserzioni ora applicano la soglia obbligatoria, mentre i colori scelti sono stati calcolati oltre 7:1.
- patch — il test imponeva sfondi uguali tra riga prodotto e azione; rimosso il vincolo e mantenuta la verifica del contrasto contro lo sfondo effettivo di ciascuna riga.
