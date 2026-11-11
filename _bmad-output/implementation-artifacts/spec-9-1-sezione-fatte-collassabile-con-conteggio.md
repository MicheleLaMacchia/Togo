---
title: '9.1 Sezione Fatte Collassabile con Conteggio'
type: 'feature'
created: '2026-11-09'
updated: '2026-11-11'
status: 'done'
story: 9.1
epic: 9
context:
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/specs/spec-cose-da-fare/SPEC.md'
---

# Story 9.1: Sezione Fatte Collassabile con Conteggio

## Obiettivo

L'utente vede in fondo alla pagina "Da fare" una sezione `Fatte (N)` che raccoglie
le azioni spuntate, può consultarle e collassarle quando non gli servono. La
sezione non compare quando non ci sono azioni completate.

La sezione esiste già come effetto collaterale della Story 8.6 (spostamento in
Fatte). Questa storia ne consolida il contratto, colma i gap rispetto a UX/SPEC
(ordine dalla più recente, semantica heading, self-hiding a N = 0) e aggiunge la
copertura di test dedicata.

## Acceptance Criteria

**Given** esistono azioni spuntate nella sezione Fatte
**When** viene renderizzata la pagina Da fare
**Then** viene mostrata la sezione "Fatte (N)" collassabile, espansa di default se N > 0
**And** il conteggio N mostra il numero di azioni spuntate

**Given** non esistono azioni spuntate (N = 0)
**When** viene renderizzata la pagina Da fare
**Then** la sezione "Fatte (0)" non compare, nessun nodo della sezione viene emesso

**Given** esistono più azioni spuntate
**When** vengono renderizzate nella sezione Fatte
**Then** sono ordinate dalla più recente in cima, con `updated_at` come criterio e `created_at` come tie-break

## Interpretazioni vincolanti

- **N** è il numero di `ActionItem` con `isChecked = true` della pagina Da fare.
  Non è il totale delle azioni e non include le azioni attive.
- **"Non compare"** significa che non viene emesso alcun nodo della sezione:
  niente header `Fatte (0)`, niente area vuota, niente placeholder. Il controllo
  è duplicato: il chiamante non invoca la sezione e la sezione stessa fa
  early-return su lista vuota.
- **"Espansa di default"** vale alla prima composizione con N > 0. Lo stato di
  espansione è locale alla UI (`rememberSaveable`): se l'utente collassa la
  sezione, la scelta resta finché la sezione è montata; quando N torna a 0 la
  sezione esce dalla composizione.
- **Ordine delle righe**: dalla più recente in cima, come da DESIGN.md §7.9 e
  dalla constraint della SPEC (`updated_at` discendente; a parità `created_at`
  discendente). L'ordine è responsabilità del ViewModel, non della sezione.
- **Righe**: riuso di `ActionRow` in modalità Fatte (checkbox spuntata, testo
  barrato, badge attenuati, nessun tap-edit, nessun cestino). La checkbox
  deseleziona e riporta l'azione nel gruppo attivo (comportamento già della
  Story 8.6/9.4, qui non modificato).
- **Header**: unico touch target ≥48dp, click per espandere/collassare,
  semantica `heading` + `stateDescription` "espansa"/"collassata", conteggio
  annunciato. Il chevron è decorativo (`contentDescription = null`).
- **Fuori perimetro 9.1**: pulsante e dialog "Concludi attività" (9.2),
  eliminazione irreversibile delle spuntate (9.3), deselezione da Fatte (9.4,
  già funzionante), condivisione/import delle azioni.

## Decisioni di design

1. `FatteSection` resta un composable presentazionale in
   `presentation/activelist/FatteSection.kt`, con parametri `items`,
   `onUncheck`, `colors`, `typography`, `spacing`, `tokens`.
2. La sezione viene inserita come ultimo `item` della `LazyColumn` della pagina
   Da fare, con `key = "fatto_section"`, dopo l'ultimo gruppo di ambito.
3. `FatteSection` fa early-return se `items.isEmpty()`: il contratto "N = 0 →
   non compare" è verificabile direttamente sul componente, oltre alla guardia
   del chiamante.
4. L'header espone `heading()` nella semantica e mantiene `Role.Button`,
   `contentDescription` con conteggio e azione, `stateDescription` con lo stato.
5. L'ordinamento delle azioni spuntate è centralizzato in un helper interno
   `List<ActionItem>.mostRecentFirst()` (in `ActiveListUiState.kt`), usato
   sia da `buildUiState` (lettura Room) sia dall'aggiornamento ottimistico di
   `handleActionCheckOff`.
6. L'animazione expand/collapse riusa la spring già usata da `PresiSection`
   (`dampingRatio = 0.9f`, `stiffness = 200.0f`).
7. Nessuna nuova dipendenza, nessuna modifica a Room, nessun nuovo evento UI o
   use case: la storia è di presentazione e ordinamento.
8. Test: Compose Robolectric su `FatteSection` (conteggio, default espansa,
   collapse/expand, stato semantico, callback deselezione, lista vuota) e test
   JVM dell'helper di ordinamento.

## File nuovi

- `app/src/test/java/it/togo/app/presentation/activelist/FatteSectionTest.kt`
- `app/src/test/java/it/togo/app/presentation/activelist/ActiveListActionOrderingTest.kt`
- `_bmad-output/implementation-artifacts/spec-9-1-sezione-fatte-collassabile-con-conteggio.md` (questo file)

## File modificati

- `app/src/main/java/it/togo/app/presentation/activelist/FatteSection.kt`
  (early-return su lista vuota, semantica heading, KDoc della storia 9.1)
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListUiState.kt`
  (helper `mostRecentFirst`)
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt`
  (ordinamento `checkedActions` da DB e nell'aggiornamento ottimistico)
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
  (epic 8 allineato a `done`, epic 9 e 9.1 `in-progress`)

## Task completati

- [x] Contratto di sezione collassabile con conteggio verificato e consolidato.
- [x] Sezione self-hiding quando N = 0.
- [x] Ordine delle azioni in Fatte dalla più recente in cima.
- [x] Semantica header: heading, stato espansa/collassata, conteggio.
- [x] Test Compose della sezione (conteggio, default, toggle, vuoto, deselezione).
- [x] Test JVM dell'ordinamento delle azioni spuntate.
- [x] Test ViewModel dell'ordinamento applicato (lettura Room + check-off ottimistico).
- [ ] Build e test Android (`.\build-android.ps1`, `.\test-android.ps1` — demandati all'utente).
- [ ] Review e aggiornamento sprint status a `done` dopo build/test verdi.

## Test eseguiti

Non eseguiti in questa sessione: build e test Android (`build-android.ps1`,
`test-android.ps1`) sono stati demandati all'utente, che li lancerà su
Docker. La storia resta in `review` finché build e test non saranno verdi.

Test scritti (da eseguire):

1. `FatteSectionTest` (Robolectric + Compose):
   - N = 2: header "Fatte (2)", entrambe le righe visibili, descrizione
     "Fatte, 2 azioni. Doppio tocco per collassare.", stato "espansa", heading.
   - Singolare: N = 1 → "Fatte, 1 azione.".
   - Tap sull'header: righe nascoste, stato "collassata", descrizione con
     "Doppio tocco per espandere."; secondo tap ripristina.
   - N = 0: nessun nodo `fatto_section`, nessun testo "Fatte (0)".
   - Tap sulla checkbox di una riga: callback `onUncheck` con l'id corretto.
2. `ActiveListActionOrderingTest`:
   - ordine per `updatedAt` discendente;
   - a parità di `updatedAt`, `createdAt` discendente;
   - la funzione non altera la lista originale e non filtra le azioni (il
     filtro `isChecked` resta a monte).
3. `ActiveListViewModelActionOrderingTest`:
   - `uiState.checkedActions` esce dalla più recente quando il repository
     restituisce le azioni in ordine `created_at ASC` (emulazione Room);
   - l'azione appena spuntata via `onEvent(CheckOff(id, true))` atterra in
     testa a `checkedActions` dopo il delay di 300 ms.

## Mappatura AC → codice/test

| AC | Codice | Test |
| --- | --- | --- |
| Sezione "Fatte (N)" collassabile in fondo alla pagina | `ActiveListScreen.kt` (`ActionsPage`, ultimo item — guardia e `key` già esistenti dalla 8.6), `FatteSection.kt` | `FatteSectionTest` |
| Espansa di default se N > 0 | `FatteSection.kt` (`rememberSaveable { mutableStateOf(true) }` + reset su N = 0) | `FatteSectionTest` |
| N = numero di azioni spuntate | `ActiveListViewModel.kt` (`checkedActions`), `FatteSection.kt` (`items.size`) | `FatteSectionTest` |
| Se N = 0 la sezione non compare | `FatteSection.kt` (early-return su `items.isEmpty()`) | `FatteSectionTest` |
| Ordine dalla più recente (UX/SPEC, vincolo di design non AC del SPEC) | `ActiveListUiState.kt` (`mostRecentFirst`), `ActiveListViewModel.kt` (buildUiState + ottimistico) | `ActiveListActionOrderingTest`, `ActiveListViewModelActionOrderingTest` |

## Rischi / BLOCCATE

- **Build e test**: eseguiti in Docker con gli stessi volumi di
  `.\test-android.ps1` e `.\build-android.ps1`: 657/657 test verdi e
  `assembleDebug` OK (APK generato).
- **Verifica manuale su device** (collapse, TalkBack, ordine visivo) non
  eseguita: da includere nel collaudo su telefono.
- **`rememberSaveable` e scomparsa della sezione**: quando N torna a 0 la
  sezione esce dalla composizione e alla ricomparsa riparte espansa. È il
  comportamento richiesto ("espansa di default se N > 0"), non un difetto.

## Comandi di verifica per l'utente

```powershell
.\test-android.ps1
.\build-android.ps1
# opzionale, verifica su device:
.\build-install-android.ps1
```

## Spec Change Log

- 2026-11-09: creazione spec; allineamento sprint status (epic 8 `done`, epic 9
  e 9.1 `review`). Implementazione e test scritti; build/test demandati
  all'utente.
- 2026-11-09: applicati i patch della code review (guardia doppio tap sul
  check-off, reset espansione alla rimessa a vuoto della sezione, test
  ViewModel dell'ordinamento, KDoc `checkedActions` aggiornato); sprint status
  a `review` in attesa di build e test verdi da parte dell'utente.
- 2026-11-11: build e test eseguiti in Docker e verdi (657/657 test,
  `assembleDebug` OK). Il blocco mockk/Attach API (137 test in errore) era
  causato dalla working directory su bind mount Windows: risolto con
  `-XX:+StartAttachListener` in `app/build.gradle.kts`. Storia `done`.
