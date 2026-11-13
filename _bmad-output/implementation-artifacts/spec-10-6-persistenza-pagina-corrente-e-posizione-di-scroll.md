---
title: '10.6 Persistenza pagina corrente e posizione di scroll'
type: 'feature'
created: '2026-11-13'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-10-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="intento esplicito della Story 10.6">

## Intent

**Problem:** Rotazione e navigazione interna possono perdere la pagina selezionata e la posizione delle liste, interrompendo il contesto dell'utente.

**Approach:** Salvare in modo indipendente pagina corrente e stato di scroll di Spesa e Da fare attraverso la ricreazione/configurazione della schermata e la navigazione interna. Un avvio a freddo deve inizializzare sempre la pagina Spesa.

</frozen-after-approval>

## Implementation Notes

- Il `ActiveListViewModel` della schermata principale ora usa il delegate Koin legato al `ViewModelStore` dell'Activity: conserva `currentPage` durante le ricreazioni di configurazione e riparte dal default Spesa con un ViewModel nuovo.
- `ActiveListScreen` conserva due `LazyListState` distinti con `rememberLazyListState` e li passa rispettivamente alle liste Spesa e Da fare. Il `SaveableStateProvider` già presente in `AppNavigator` conserva questi stati nel ritorno dalla navigazione interna.
- Aggiunto in `AppNavigatorTest` un caso Compose/Robolectric per le posizioni indipendenti, il ritorno da Storico e il ripristino saved-instance-state. Non eseguito per vincolo dell'utente; build e test restano da eseguire dall'utente.
- Discrepanza tracking 10.5: il tracking è stato riallineato da `review` a `done` su indicazione esplicita dell'utente, coerente con la spec 10.5 già `done`. La nota storica sull'errore di compilazione resta registrata; il riallineamento di stato non attesta build o test superati.

**Verifica richiesta all'utente:** `.\test-android.ps1` poi `.\build-android.ps1`.

## Review Triage Log

- false — Il ripristino del processo/istanza può creare un ViewModel nuovo, che parte da Spesa: è coerente con il criterio esplicito di avvio a freddo sempre su Spesa; la pagina è mantenuta durante rotazione tramite il delegate lifecycle-aware.
- low, respinto — La suite non esercita una ricreazione reale di `MainActivity` per verificare il `ViewModelStore`. La configurazione usa `by viewModel()` e l'istanza è legata al lifecycle dell'Activity; un test d'integrazione con l'intero grafo Koin aggiungerebbe più struttura della correzione semplice. La verifica runtime resta all'utente.
- low, respinto — Il test copre il ripristino dei due stati saveable nel provider di navigazione, mentre il collegamento di ciascuno stato alla sua pagina è visibile direttamente in `ActiveListScreen` e usa gli stessi stati del test. Aggiungere fixture per comporre entrambe le pagine reali non cambierebbe la correzione semplice; la suite resta da eseguire dall'utente.
