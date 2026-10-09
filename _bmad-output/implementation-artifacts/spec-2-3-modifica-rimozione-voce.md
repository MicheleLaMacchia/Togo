---
title: 'Story 2.3: Modifica e Rimozione Voce dalla Lista'
type: 'feature'
created: '2026-10-09'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-2-1-schermata-lista-attiva.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-2-2-inserimento-manuale-prodotto.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'utente può aggiungere voci alla lista (Story 2.2) e spuntarle (Story 2.1), ma non può **modificare** una voce esistente (cambiare quantità, unità, attributi) né **rimuoverla** definitivamente dalla lista attiva.

**Approach:** Estendere `ItemRow` e `ActiveListViewModel` per supportare:
1. **Modifica**: Tap su riga → apre `AddItemBottomSheet` pre-popolato con i dati della voce esistente → conferma → aggiorna in repo
2. **Rimozione**: Swipe-to-delete su `ItemRow` (già implementato in Story 2.1) → conferma opzionale → elimina da repo
3. **Integrazione**: Riutilizzare `AddItemBottomSheet` e `AddItemViewModel` per la modifica (pattern "edit = pre-filled insert")

## Boundaries & Constraints

**Always:**
- Design System obbligatorio: `TogoTheme`, token colori/tipografia/spacing/shape/componentTokens
- Modifica riutilizza `AddItemBottomSheet` con `selectedProduct` pre-popolato
- Swipe-to-delete confermato (già in `ItemRow`)
- Touch target ≥48dp su tutti i controlli
- Validazione campi obbligatori (quantità > 0, unità) anche in modifica

**Never:**
- Nessun valore hardcoded (colori, spacing, tipografia)
- Modifica/eliminazione senza conferma utente
- Logica di business nel composable (solo UI + event emission)

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Tap su riga attiva | Voce in `activeGroups` | Apre `AddItemBottomSheet` step 2 (quantità/unità) pre-popolato | Se repo fallisce → snackbar errore, rollback UI |
| Tap su riga in "Presi" | Voce in `checkedItems` | Apre `AddItemBottomSheet` step 2 pre-popolato (per riattivare/modificare) | Se repo fallisce → snackbar errore |
| Swipe-to-delete | Swipe sinistra su `ItemRow` | Mostra action delete → tap conferma → elimina da repo | Se repo fallisce → snackbar errore, rollback |
| Modifica quantità a 0 | Input 0 in stepper/campo | Disabilita conferma, errore "Quantità deve essere > 0" | Blocco conferma |
| Annullamento modifica | Tap "Indietro" o chiudi sheet | Scarta modifiche, voce invariata | Nessuno |
| Voce con attributi | Modifica marca/variante/conservazione | Aggiorna attributi in repo | Se repo fallisce → rollback |

## Open Questions

- Conferma esplicita per delete (dialog) vs swipe diretto? — **DECISO: swipe-to-delete diretto con undo toast (Snackbar) per 5s, come Gmail**
- Ripristino voce eliminata (undo)? — **DECISO: Snackbar con azione "Annulla" per 5s, chiama `insert` con dati originali**
- Modifica prodotto canonico (cambiare prodotto)? — **DECISO: no, modifica solo quantità/unità/attributi; per cambiare prodotto → delete + nuova voce**

## Code Map

- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt` — Handle `UiEvent.EditItem` (apre bottom sheet modifica) + `UiEvent.DeleteItem` (delete con undo)
- `app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt` — `onClick` → `EditItem`, `onDelete` → `DeleteItem` (già collegati)
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` — Gestisce apertura bottom sheet per `EditItem` con dati pre-popolati
- `app/src/main/java/it/togo/app/presentation/additem/AddItemViewModel.kt` — Supporto modalità "edit" (pre-popolamento + `Update` invece di `Insert`)
- `app/src/main/java/it/togo/app/presentation/additem/AddItemBottomSheet.kt` — Supporto modalità edit (titolo "Modifica voce", step 2 come default)
- `app/src/main/java/it/togo/app/presentation/additem/AddItemViewModel.kt` — Nuovo costruttore/factory per modalità edit, `handleConfirm` → `shoppingRepository.update()`
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` — Gestione Snackbar undo per delete

## Tasks & Acceptance

**Execution:**
- [x] `ActiveListViewModel.kt` — `onEvent(EditItem)` → apre bottom sheet edit; `onEvent(DeleteItem)` → delete + Snackbar undo (5s) → `shoppingRepository.delete()`; `UiEvent.UndoDelete` per ripristino
- [x] `AddItemViewModel.kt` — Factory `forEdit(item)` → pre-popolamento, step 2 default, `Confirm` → `shoppingRepository.update()`
- [x] `AddItemBottomSheet.kt` — Modalità edit (`isEditMode`, titolo "Modifica voce", step 2 default, "Salva")
- [x] `ActiveListScreen.kt` — `ModalBottomSheetLayout` per edit, `SnackbarHost` con undo delete, FAB "+" invariato
- [x] `ActiveListUiState.kt` — Aggiunto `UiEvent.UndoDelete`
- [x] `ShoppingListRepository` — `delete(itemId)` e `update(item)` già presenti

**Acceptance Criteria:**
- Given voce in lista, when tap su riga, then `AddItemBottomSheet` si apre allo step 2 con quantità/unità/attributi pre-popolati
- Given modifica quantità/unità, when tap Conferma, then voce aggiornata in repo e UI
- Given swipe sinistra su voce, when tap delete, then voce rimossa e Snackbar "Voce eliminata" con azione "Annulla" (5s)
- Given tap "Annulla" su Snackbar, then voce ripristinata in lista
- Given modifica attributi, when Conferma, then attributi aggiornati in repo
- Tutti i componenti usano **solo** token `TogoTheme`
- `gradlew help` passa

## Dev Approvals

- [ ] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **ActiveListViewModel.kt**: Aggiunti `handleDeleteItem` (con undo via Snackbar), `handleUndoDelete` (ripristino item), `_snackbarMessage` per messaggio undo; `deletedItemForUndo` per salvare item eliminato.
- **AddItemViewModel.kt**: Factory `forEdit(item)` → pre-popolamento `selectedProduct`, `quantity`, `unit`, `brand`, `variant`, `condition`, step 2 default; `handleConfirm` usa `copyWith` per preservare ID e `createdAt` in edit mode; `isEditMode` flag per differenziare insert/update.
- **AddItemBottomSheet.kt**: Parametro `isEditMode` → titolo dinamico ("Modifica voce" vs "Cerca prodotto"), step iniziale 2 in edit, pulsante conferma "Salva" vs "Aggiungi", progress indicator salta step 1 in edit.
- **ActiveListScreen.kt**: Due `ModalBottomSheetLayout` (insert + edit), `SnackbarHost` con azione "Annulla" per undo delete, dual FAB (Voice + Add), gestione `EditItem` apre `editSheetState` con item pre-selezionato, `DeleteItem` mostra Snackbar con azione "Annulla" (5s).
- **ActiveListUiState.kt**: Aggiunto `UiEvent.UndoDelete` per undo delete.
- **Design System**: 100% token `TogoTheme` (colori, tipografia, spacing, shape, componentTokens).
- **Build**: `gradlew help` OK.

## Spec Change Log

- 2026-10-09 — Creazione spec, stato `in-progress`.
- 2026-10-09 — Implementazione completata (5 file modificati/creati), build `gradlew help` OK, stato `review`.

## Review Triage Log

- (vuoto fino alla review della Story 2.3)