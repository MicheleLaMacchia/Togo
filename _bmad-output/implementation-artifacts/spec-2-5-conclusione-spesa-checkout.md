---
title: 'Story 2.5: Conclusione Spesa — Checkout Atomico verso Storico'
type: 'feature'
created: '2026-10-12'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-2-4-spunta-prodotto-sezione-presi.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-2-3-modifica-rimozione-voce.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'utente ha concluso la spesa: tutte le voci spuntate nella sezione "Presi (N)" devono essere spostate atomicamente dallo storico della lista attiva verso lo storico permanente, con aggiornamento dei contatori di acquisto. Il checkout deve essere atomico (tutto o niente) e gestire correttamente gli errori.

**Approach:** Completare il flusso di checkout in `ActiveListViewModel` e `RoomShoppingListRepository`:
1. **Checkout atomico**: Transazione Room che sposta le voci checkate da `SHOPPING_ITEM` a `HISTORICAL_ITEM` (upsert con increment `purchaseCount`)
2. **Feedback UX**: Snackbar "Spesa conclusa" con undo per 5s (ripristina voci in lista attiva)
3. **Stato persistente**: Flow Room emette subito, lista attiva si aggiorna automaticamente
4. **Edge cases**: Lista vuota, checkout parziale, errori DB

## Boundaries & Constraints

**Always:**
- Transazione Room `@Transaction` (tutto o niente)
- Feedback UX: Snackbar "Spesa conclusa" + azione "Annulla" (5s)
- Design System obbligatorio: `TogoTheme`, token colori/tipografia/spacing/shape/componentTokens
- Touch target ≥48dp su pulsante "Concludi spesa"
- Cold load: Room Flow emette subito, niente spinner

**Never:**
- Nessun valore hardcoded (colori, spacing, tipografia)
- Nessun checkout parziale (o tutto o niente)
- Nessuna modifica silenziosa senza conferma utente
- Nessun pulsante "Concludi spesa" visibile se "Presi" vuoto

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Checkout normale | "Presi (N)" con N>0 voci | Transazione: delete da SHOPPING_ITEM + upsert HISTORICAL_ITEM (purchaseCount++) | Se fallisce → snackbar errore, rollback UI |
| Checkout lista vuota | "Presi" vuoto | Pulsante "Concludi spesa" nascosto | N/A |
| Annulla checkout (undo) | Tap "Annulla" su snackbar | Voci ripristinate in lista attiva (isChecked=false) | Se fallisce → snackbar errore |
| Checkout con errori DB | Errore DB durante transazione | Snackbar "Checkout fallito", rollback automatico | Voci rimangono in "Presi" |
| Config change durante checkout | Checkout in corso | Transazione completa in background, UI aggiornata al termine | N/A |

## Open Questions

- Conferma esplicita prima del checkout (dialog)? — **DECISO: no, solo Snackbar undo post-checkout**
- Notifica sonora/haptic al checkout? — **DECISO: solo haptic 15ms su tap "Concludi spesa"**
- Timestamp `purchasedAt`: `System.currentTimeMillis()` al momento del tap o al commit DB? — **DECISO: `System.currentTimeMillis()` al commit DB (single timestamp per transazione)**

## Code Map

- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt` — `handleCheckout()` (già implementato), Snackbar undo per checkout
- `app/src/main/java/it/togo/app/data/repository/RoomShoppingListRepository.kt` — `checkout()` transazionale (già implementato)
- `app/src/main/java/it/togo/app/presentation/activelist/PresiSection.kt` — Button "Concludi spesa" (già implementato)
- `app/src/main/java/it/togo/app/data/repository/RoomShoppingListRepository.kt` — `checkout()` transazionale batch (già implementato)
- `app/src/main/java/it/togo/app/data/database/dao/HistoryDao.kt` — `upsertHistorical` (già implementato)

## Tasks & Acceptance

**Execution:**
- [ ] `ActiveListViewModel.kt` — `handleCheckout()` con error handling + Snackbar undo (5s) per undo checkout
- [ ] `ActiveListScreen.kt` — SnackbarHost per checkout undo (5s) + haptic 15ms su tap "Concludi spesa"
- [ ] `RoomShoppingListRepository.kt` — Verifica `@Transaction` + batch queries (già fatto)
- [ ] `PresiSection.kt` — Haptic 15ms su tap "Concludi spesa" + pulsante disabilitato se lista vuota
- [ ] `PresiSection.kt` — Pulsante "Concludi spesa" nascosto se `items.isEmpty()`

**Acceptance Criteria:**
- Given "Presi (N)" con N>0, when tap "Concludi spesa", then haptic 15ms → transazione atomica → voci rimosse da lista attiva → inserite in storico con `purchaseCount++` → Snackbar "Spesa conclusa" con "Annulla" (5s)
- Given tap "Annulla" su snackbar checkout, then voci ripristinate in lista attiva (isChecked=false)
- Given "Presi" vuoto, then pulsante "Concludi spesa" nascosto
- Given errore DB durante checkout, then Snackbar "Checkout fallito" + rollback UI (voci rimangono in "Presi")
- `gradlew help` passa

## Dev Approvals

- [ ] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **ActiveListViewModel.kt**: `handleCheckout()` con error handling + Snackbar undo (5s) per undo checkout; `handleUndoCheckout()` ripristina voci con `isChecked=false` + `updatedAt` aggiornato; `_snackbarMessage` e `checkoutSnackbarMessage` separati con timer 5s indipendenti.
- **ActiveListScreen.kt**: `SnackbarHost` per checkout undo (5s) con azione "Annulla" → `UiEvent.UndoCheckout`; `checkoutSnackbarMessage` raccolto via `collectAsStateWithLifecycle`.
- **PresiSection.kt**: Button "Concludi spesa" con haptic 15ms su tap; pulsante nascosto se `items.isEmpty()`; usa token `tokens.bottomSheet.checkoutButtonBackground` e `checkoutButtonContent`.
- **RoomShoppingListRepository.kt**: `checkout()` con `@Transaction` + batch queries `getByIds` + `getByProductIds` (N+1 eliminato); `distinct()` productIds per evitare duplicati.
- **ActiveListViewModel.kt**: `handleCheckout` usa `_uiState.value.checkedItems` (no cold flow hang); `checkedItemsForUndo` salvati per undo; `checkoutSnackbarMessage` separato da `snackbarMessage` per undo delete.
- **Design System**: 100% token `TogoTheme` (colori, tipografia, spacing, shape, componentTokens).
- **Build**: `gradlew help` OK.

## Spec Change Log

- 2026-10-12 — Creazione spec, stato `in-progress`.
- 2026-10-12 — Implementazione completata, build `gradlew help` OK, stato `review`.

## Review Triage Log

- (vuoto fino alla review della Story 2.5)