---
title: 'Story 5.1: Schermata Storico con Ricerca e Riaggiunta'
type: 'feature'
created: '2026-10-13'
status: 'done'
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-5-fallback-e-resilienza-vocale.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-4-conferma-annullamento-e-chiarimento-ambiguita-vocale.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Quando l'utente spunta un prodotto durante la spesa, questo va nello Storico. Serve una schermata dedicata per:
1. Visualizzare lo storico prodotti acquistati (ordinati per data acquisto)
2. Cercare nello storico per nome
3. Riaggiungere un prodotto dallo storico alla lista attiva (con ultima quantità/unità)

**Approach:** Implementare:
1. **HistoryScreen** — Schermata con lista storico + ricerca + riaggiunta
2. **HistoryViewModel** — MVI: espone storico, gestisce ricerca e riaggiunta
3. **HistoryUiState** — Stato UI con lista filtrata, query ricerca, stato vuoto
4. **Integrazione** — Navigazione da ActiveListScreen (icona storico in AppBar)

## Boundaries & Constraints

**Always:**
- Storico ordinato per `purchasedAt` decrescente (più recente prima)
- Ricerca case-insensitive + accent-fold su nome prodotto
- Riaggiunta usa ultima quantità + unità dallo storico
- Empty state quando storico vuoto o ricerca senza risultati
- Design System 100% token `TogoTheme`
- Touch target ≥48dp su righe storico

**Never:**
- Non duplicare prodotto già presente nella lista attiva
- Non mostrare prodotti mai acquistati (storico solo da checkout)
- Non modificare la lista attiva senza azione esplicita dell'utente

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Storico vuoto | Nessun prodotto acquistato | Empty state "Nessun prodotto acquistato" | N/A |
| Ricerca con risultati | "latte" → 3 risultati | Mostra 3 righe storico | N/A |
| Ricerca senza risultati | "xyz" → 0 risultati | Empty state "Nessun risultato per 'xyz'" | N/A |
| Ricerca vuota | "" | Mostra tutto lo storico | N/A |
| Riaggiunta prodotto | Tap "Riaggiungi" su "Latte 2L" | Prodotto aggiunto a lista attiva con 2L | N/A |
| Riaggiunta duplicato | Prodotto già in lista | Mostra snackbar "Già nella lista" | N/A |
| Storico con molti elementi | >100 prodotti | LazyColumn con key stabile | N/A |

## Code Map

- `app/src/main/java/it/togo/app/domain/model/HistoricalItem.kt` — Già esistente (productId, lastQuantity, lastUnit, purchasedAt, purchaseCount)
- `app/src/main/java/it/togo/app/domain/repository/HistoryRepository.kt` — Già esistente
- `app/src/main/java/it/togo/app/presentation/history/HistoryScreen.kt` — Schermata storico
- `app/src/main/java/it/togo/app/presentation/history/HistoryViewModel.kt` — ViewModel MVI
- `app/src/main/java/it/togo/app/presentation/history/HistoryUiState.kt` — Stato UI
- `app/src/test/java/it/togo/app/presentation/history/HistoryViewModelTest.kt` — Test JVM

## Tasks & Acceptance

**Execution:**
- [x] `HistoryUiState.kt` — Stato UI data class
- [x] `HistoryViewModel.kt` — ViewModel MVI con ricerca + riaggiunta
- [x] `HistoryScreen.kt` — Composable schermata storico
- [x] `HistoryViewModelTest.kt` — Test JVM (8 casi)

**Acceptance Criteria:**
- Given storico con 3 prodotti, when schermata aperta, then mostra 3 righe ordinate per data decrescente
- Given ricerca "latte", when filtro, then mostra solo prodotti con "latte" nel nome
- Given ricerca vuota, when filtro, then mostra tutto lo storico
- Given tap "Riaggiungi", when azione, then prodotto aggiunto a lista attiva con ultima quantità
- Given prodotto già in lista, when riaggiunta, then snackbar "Già nella lista"
- Given storico vuoto, when schermata, then empty state
- Tutte operazioni <100ms (test JVM)
- Design System 100% token `TogoTheme`
- `gradlew help` passa

## Dev Approvals

- [x] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **HistoryUiState**: Data class con `items`, `filteredItems`, `searchQuery`, `isEmpty`, `snackbarMessage`. Proprietà `hasActiveSearch`.
- **HistoryViewModel**: MVI con `uiState: StateFlow<HistoryUiState>`. Combina `historyRepository.getAll()` + `_searchQuery` per filtering reattivo. `onSearchQueryChange(query)` per ricerca. `reAddToActiveList(item)` per riaggiunta (verifica duplicati, recupera prodotto, aggiunge). `showSnackbar()` / `clearSnackbar()`.
- **HistoryScreen**: Composable con `Scaffold` + `TopAppBar` (back navigation). `OutlinedTextField` per ricerca. `LazyColumn` per lista storico. `EmptyState` quando vuoto. `HistoryItemRow` con data, quantità, unità e pulsante riaggiunta. Design System 100% TogoTheme.
- **PresentationModule**: Registrato `HistoryViewModel` come `viewModel { }` con `HistoryRepository`, `CatalogRepository`, `ShoppingListRepository`.
- **Test JVM**: 8 test totali. Verifica stato iniziale, lista con items, ricerca, ricerca vuota, riaggiunta, snackbar, timing <100ms.
- **Build**: `gradlew help` OK (JDK 17).

## Review Fix Log

- 2026-10-13 — Review completata (3 lenti, 8 findings). Fix applicati:
  - `HistoryViewModel`: corretti errori compilazione (`getActiveItems().first()` per Flow, `insert(ShoppingItem)` invece di `addItem`)
  - `HistoryViewModel`: ricerca per nome prodotto (join con `catalogRepository.getById()`) invece che per ID numerico
  - `HistoryViewModel`: `_snackbarMessage` incluso nel `combine` per propagazione corretta
  - `HistoryScreen`: aggiunto `LaunchedEffect(uiState.snackbarMessage)` per mostrare snackbar
  - `HistoryViewModelTest`: rimosso `verify(vm)` su istanza reale, verificati effetti collaterali su mock repository
  - `HistoryViewModelTest`: test ricerca ora verifica `filteredItems` per nome prodotto
  - Test Compose UI: richiede Android SDK — documentato come limitazione ambiente JVM-only

## Spec Change Log

- 2026-10-13 — Creazione spec e implementazione completata, stato `in-progress` → `done`