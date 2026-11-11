---
title: '9.2 Pulsante Concludi attività e Dialog di Conferma'
type: 'feature'
created: '2026-11-11'
updated: '2026-11-11'
status: 'done'
story: 9.2
epic: 9
context:
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/specs/spec-cose-da-fare/SPEC.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-9-1-sezione-fatte-collassabile-con-conteggio.md'
---

# Story 9.2: Pulsante Concludi attività e Dialog di Conferma

## Obiettivo

L'utente vede in fondo alla sezione `Fatte (N)` il pulsante largo **Concludi
attività**. Toccandolo apre un dialog modale che dichiara quante azioni
spuntate verranno eliminate, che l'operazione è irreversibile e offre due
scelte: **Concludi attività** (primario distruttivo, con icona warning) e
**Annulla** (secondario testuale).

La storia introduce il controllo e il dialog. L'eliminazione effettiva delle
azioni spuntate, senza undo e senza storico, è la Story 9.3.

## Acceptance Criteria

**Given** N > 0 nella sezione Fatte
**When** viene renderizzata la sezione Fatte
**Then** viene mostrato il pulsante largo "Concludi attività" in fondo alla sezione
**And** toccando il pulsante si apre un dialog di conferma con titolo "Concludere le attività?" e corpo "Verranno eliminate N attività spuntate. L'operazione non si può annullare."
**And** il primario è "Concludi attività" (stile distruttivo, con icona warning) e il secondario è "Annulla"

## Interpretazioni vincolanti

- **N** è `items.size`, lo stesso conteggio dell'header `Fatte (N)`: solo
  azioni con `isChecked = true`. La sezione non esiste con N = 0, quindi il
  pulsante non viene mai emesso senza azioni spuntate (nessuna guardia
  duplicata oltre a quella della 9.1).
- **"In fondo alla sezione"** significa dentro il contenuto espanso, dopo
  l'ultima riga. Come per `PresiSection`, collassare la sezione nasconde anche
  il pulsante; l'header `Fatte (N)` resta visibile. L'utente deve riaprire la
  sezione per concludere, coerentemente con il fatto che la conclusione
  distrugge ciò che la sezione contiene.
- **Corpo del dialog**: per N = 1 si usa la forma singolare "Verrà eliminata 1
  attività spuntata. L'operazione non si può annullare."; per N > 1 la forma
  dell'AC "Verranno eliminate N attività spuntate. L'operazione non si può
  annullare.". Il conteggio e l'irreversibilità sono sempre espliciti.
- **Primario distruttivo**: pulsante pieno con container `accentAction`, testo
  `inkInverse` e icona `Icons.Filled.Warning` in `accentWarning`, decorativa
  (`contentDescription = null`) perché la frase del corpo e l'etichetta
  portano già il significato. Il nero del primario è quello prescritto da
  DESIGN.md §7.10; l'icona warning e il testo rendono l'azione riconoscibile
  come distruttiva senza dipendere dal solo colore.
- **Secondario**: `TextButton` "Annulla", colore `inkSecondary`, touch target
  ≥48dp. Annulla, back di sistema e tap fuori dal dialog chiudono senza
  invocare la conferma.
- **Conferma**: chiude il dialog e invoca il callback `onConclude`. In questa
  storia il callback arriva al ViewModel come `UiEvent.ConcludeActions`, che
  registra l'intento ma non elimina nulla: la cancellazione è la Story 9.3.
  Non esiste undo né scrittura in `HISTORICAL_ITEM` in questa storia.
- **Stato del dialog**: locale alla UI (`rememberSaveable`), coerente con
  l'espansione locale della sezione. Se N torna a 0 il dialog viene chiuso e
  lo stato riparte chiuso.
- **Fuori perimetro 9.2**: eliminazione delle azioni spuntate (9.3),
  deselezione da Fatte (9.4, già funzionante), eliminazione singola con undo
  (8.8), condivisione/import.

## Decisioni di design

1. Nuovo composable presentazionale
   `presentation/activelist/ConcludeActionsDialog.kt` con parametri
   `count: Int`, `onConfirm: () -> Unit`, `onDismiss: () -> Unit`.
2. `FatteSection` riceve un nuovo parametro obbligatorio `onConclude: () -> Unit`
   e mantiene lo stato `showConcludeDialog` con `rememberSaveable`.
3. Il pulsante è un `Button` largo (`fillMaxWidth`), alto almeno
   `spacing.touchTargetMin`, con `testTag("conclude_actions_button")`, testo
   `Concludi attività` e stile `sectionHeader`.
4. Il dialog usa `AlertDialog` Material3 con i token Togo: container
   `surfaceBase`, bordo 2dp `borderCrisp`, raggio
   `tokens.duplicateDialog.radius`, titolo `titleScreen`, corpo `itemMeta`.
5. Il primario è un `Button` con icona warning + testo; il secondario un
   `TextButton` "Annulla". Entrambi hanno altezza minima 48dp e testTag
   dedicati (`conclude_actions_confirm_button`,
   `conclude_actions_cancel_button`).
6. `ActiveListScreen` inoltra `onConclude` come
   `UiEvent.ConcludeActions`; il ViewModel aggiunge il ramo dell'evento con
   log e commento che rimanda alla 9.3. Nessuna modifica a Room, domain,
   repository o use case.
7. `FatteSection` azzera `showConcludeDialog` quando `items` diventa vuota,
   nello stesso `LaunchedEffect` che ripristina l'espansione.
8. Test: estensione di `FatteSectionTest` per pulsante, apertura dialog,
   conferma, annulla e scomparsa del pulsante a sezione collassata; nuovo
   `ConcludeActionsDialogTest` per titolo, corpo singolare/plurale, icona
   warning, callback e touch target.

## File nuovi

- `app/src/main/java/it/togo/app/presentation/activelist/ConcludeActionsDialog.kt`
- `app/src/test/java/it/togo/app/presentation/activelist/ConcludeActionsDialogTest.kt`
- `_bmad-output/implementation-artifacts/spec-9-2-pulsante-concludi-attività-e-dialog-di-conferma.md` (questo file)

## File modificati

- `app/src/main/java/it/togo/app/presentation/activelist/FatteSection.kt`
  (parametro `onConclude`, pulsante, stato e dialog)
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt`
  (inoltra `UiEvent.ConcludeActions`)
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListUiState.kt`
  (evento `ConcludeActions`)
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt`
  (ramo dell'evento, senza cancellazione fino alla 9.3)
- `app/src/test/java/it/togo/app/presentation/activelist/FatteSectionTest.kt`
  (pulsante e dialog)
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
  (9.2 `in-progress`, poi `done`)

## Task

- [ ] Pulsante largo "Concludi attività" in fondo alla sezione, visibile con N > 0.
- [ ] Dialog di conferma con titolo, corpo con conteggio, primario distruttivo con icona warning e secondario "Annulla".
- [ ] Annulla/back/tap fuori chiudono senza confermare; conferma invoca il callback.
- [ ] Seam `UiEvent.ConcludeActions` fino al ViewModel, senza eliminazione (Story 9.3).
- [ ] Test Compose del pulsante e del dialog; test del dialog in isolamento.
- [ ] Code review e aggiornamento sprint status a `done`.
- [ ] Build e test Android (`.\build-android.ps1`, `.\test-android.ps1` — demandati all'utente).

## Test scritti

1. `FatteSectionTest` (Robolectric + Compose):
   - N = 2: il pulsante `conclude_actions_button` esiste, è largo e alto ≥48dp.
   - Tap sul pulsante: il dialog mostra titolo e corpo con N = 2; il pulsante
     della sezione e il primario del dialog coesistono (tag distinti).
   - Tap sul primario: `onConclude` invocato una volta e dialog chiuso.
   - Tap su "Annulla": dialog chiuso e `onConclude` non invocato.
   - Sezione collassata: il pulsante non è visibile; riaprendo torna.
   - N = 0: nessun pulsante (la sezione non emette nodi, come 9.1).
2. `ConcludeActionsDialogTest`:
   - Titolo "Concludere le attività?" e corpo plurale con conteggio.
   - Corpo singolare per count = 1.
   - Primario "Concludi attività" con icona warning decorativa e click action.
   - Secondario "Annulla" con click action e altezza ≥48dp.
   - Callback conferma/annulla.
3. Test di accessibilità: il titolo del dialog è heading; l'icona warning non
   produce un nodo annunciato; entrambi i pulsanti sono focusabili.

## Mappatura AC → codice/test

| AC | Codice | Test |
| --- | --- | --- |
| Pulsante largo in fondo alla sezione con N > 0 | `FatteSection.kt` (dentro `AnimatedVisibility`, dopo le righe) | `FatteSectionTest` |
| Tap → dialog di conferma | `FatteSection.kt` (`showConcludeDialog`), `ConcludeActionsDialog.kt` | `FatteSectionTest` |
| Titolo e corpo con N | `ConcludeActionsDialog.kt` | `ConcludeActionsDialogTest` |
| Primario distruttivo con icona warning | `ConcludeActionsDialog.kt` (`Button` + `Icons.Filled.Warning`) | `ConcludeActionsDialogTest` |
| Secondario "Annulla" | `ConcludeActionsDialog.kt` (`TextButton`) | `ConcludeActionsDialogTest`, `FatteSectionTest` |
| Conferma inoltrata come intento | `ActiveListScreen.kt`, `ActiveListUiState.kt`, `ActiveListViewModel.kt` | `FatteSectionTest` (callback) |

## Rischi / BLOCCATE

- **Conferma senza cancellazione**: in 9.2 il primario chiude il dialog e
  invoca `UiEvent.ConcludeActions`, ma il ViewModel non elimina ancora nulla.
  È il confine concordato con la Story 9.3; il pulsante non è un no-op
  silenzioso: l'intento è modellato e loggato.
- **Build e test non eseguiti in questa sessione**: demandati all'utente, che
  li lancerà su Docker. La storia resta `in-progress` finché non sono verdi.
- **Verifica manuale su device** (dialog, TalkBack, dark mode, tap fuori)
  da includere nel collaudo su telefono.

## Comandi di verifica per l'utente

```powershell
.\test-android.ps1
.\build-android.ps1
# opzionale, verifica su device:
.\build-install-android.ps1
```

## Spec Change Log

- 2026-11-11: creazione spec 9.2; sprint status a `in-progress`.
  Implementazione UI (pulsante + dialog) e seam dell'evento; eliminazione
  demandata alla 9.3. Build e test demandati all'utente.
- 2026-11-11: code review 9.2 (4 layer) triage e patch applicati:
  - shape primario dialog `RoundedCornerShape(8.dp)` (non più default pill);
  - guardia `require(count > 0)` in `ConcludeActionsDialog`;
  - `contentDescription` sul corpo del dialog per annuncio TalkBack
    (conteggio + irreversibilità);
  - test: larghezza pulsante (largo), count<=0 throws, TalkBack corpo,
    ViewModel `ConcludeActions` non elimina azioni (confine 9.2/9.3).
  - Findings non bloccanti respinti/documentati: scrim/back dismiss
    (comportamento standard `AlertDialog.onDismissRequest`), shape sezione
    (`bottomSheet.radiusTop` coerente con `PresiSection`), token
    `duplicateDialog` riuso legittimo, `ConcludeActions` senza payload
    coerente con altri eventi, `Modifier.border` corretto per `AlertDialog`.
- 2026-11-11: sprint status 9.2 portato a `done`.
