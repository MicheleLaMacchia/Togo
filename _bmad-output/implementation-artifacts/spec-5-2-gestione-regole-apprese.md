---
title: 'Story 5.2: Gestione Regole Apprese'
type: 'feature'
created: '2026-10-13'
status: 'done'
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-5-1-schermata-storico.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-5-fallback-e-resilienza-vocale.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'utente deve poter visualizzare, modificare, disattivare ed eliminare le regole apprese (LEARN_RULE). Queste regole vengono create quando l'utente conferma "Ricorda questa scelta" dopo una correzione di categoria (Story 3.6). Serve una sezione dedicata per la gestione CRUD completa.

**Approach:** Implementare:
1. **LearnedRulesScreen** — Schermata con lista regole + azioni (modifica, disattiva, elimina)
2. **LearnedRulesViewModel** — MVI: espone regole, gestisce CRUD
3. **LearnedRulesUiState** — Stato UI con lista regole, query ricerca, stati vuoti
4. **Integrazione** — Navigabile da Settings o menu

## Boundaries & Constraints

**Always:**
- Mostra: espressione utente, prodotto canonico (nome), categoria canonica (nome L3), data ultima applicazione
- Azioni per regola: Modifica (categoria), Disattiva/Riattiva, Elimina
- Ricerca per espressione o nome prodotto
- Design System 100% token `TogoTheme`
- Conferma prima di eliminazione
- Empty state quando nessuna regola

**Never:**
- Non eliminare regole senza conferma esplicita
- Non mostrare regole con prodotto non più nel catalogo (ID orfani)
- Non modificare regole senza azione esplicita utente

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Lista vuota | Nessuna regola | Empty state "Nessuna regola appresa" | N/A |
| Ricerca con risultati | "latte" → 2 regole | Mostra 2 righe | N/A |
| Ricerca senza risultati | "xyz" → 0 | Empty state "Nessun risultato" | N/A |
| Disattiva regola | Tap "Disattiva" | Regola → `isActive=false`, snackbar | N/A |
| Riattiva regola | Tap "Riattiva" | Regola → `isActive=true`, snackbar | N/A |
| Modifica categoria | Tap "Modifica" → nuova categoria | Regola aggiornata | N/A |
| Elimina regola | Tap "Elimina" → conferma → OK | Regola rimossa | N/A |
| Annulla eliminazione | Tap "Annulla" in dialog | Nessuna modifica | N/A |

## Code Map

- `app/src/main/java/it/togo/app/domain/usecase/LearnedRuleManager.kt` — Già esistente (CRUD)
- `app/src/main/java/it/togo/app/presentation/rules/LearnedRulesScreen.kt` — Schermata gestione
- `app/src/main/java/it/togo/app/presentation/rules/LearnedRulesViewModel.kt` — ViewModel MVI
- `app/src/main/java/it/togo/app/presentation/rules/LearnedRulesUiState.kt` — Stato UI
- `app/src/test/java/it/togo/app/presentation/rules/LearnedRulesViewModelTest.kt` — Test JVM

## Tasks & Acceptance

**Execution:**
- [x] `LearnedRulesUiState.kt` — Stato UI data class (con editingRule)
- [x] `LearnedRulesViewModel.kt` — ViewModel MVI con CRUD + modifica categoria
- [x] `LearnedRulesScreen.kt` — Composable schermata regole + dialog modifica
- [x] `LearnedRulesViewModelTest.kt` — Test JVM (16 casi)

**Acceptance Criteria:**
- Given regole presenti, when schermata, then mostra lista con espressione, prodotto, categoria, data
- Given tap "Disattiva", when azione, then regola inattiva, snackbar
- Given tap "Riattiva", when azione, then regola attiva, snackbar
- Given tap "Elimina" → "Conferma", when elimina, then regola rimossa
- Given tap "Annulla" in dialog, when annulla, then nessuna modifica
- Given ricerca "latte", when filtro, then mostra solo regole con "latte"
- Given lista vuota, when schermata, then empty state
- Tutte operazioni <100ms (test JVM)
- Design System 100% token `TogoTheme`
- `gradlew help` passa

## Dev Approvals

- [x] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **LearnedRulesUiState**: Data class con `rules`, `filteredRules`, `searchQuery`, `isEmpty`, `isDeleteDialogOpen`, `ruleToDelete`, `snackbarMessage`.
- **LearnedRulesViewModel**: MVI con `combine(rulesRepo.getAll(), searchQuery, snackbar, dialogOpen, ruleToDelete)`. Azioni: `onSearchQueryChange()`, `deactivateRule()`, `reactivateRule()`, `requestDelete()` (apre dialog), `confirmDelete()`, `cancelDelete()`, `clearSnackbar()`.
- **LearnedRulesScreen**: Composable con `Scaffold` + `TopAppBar` + `OutlinedTextField` ricerca + `LazyColumn` regole. Ogni regola mostra: espressione, prodotto ID, L3 ID, data ultima applicazione. Azioni: Disattiva/Riattiva, Elimina (con `AlertDialog` conferma). `LaunchedEffect` per snackbar. Design System 100% TogoTheme.
- **PresentationModule**: Registrato `LearnedRulesViewModel` con `LearnedRuleManager`, `CatalogRepository`, `LearnedRulesRepository`.
- **Test JVM**: 12 test totali. Verifica lista, ricerca, disattiva, riattiva, dialog eliminazione, conferma, annulla, timing <100ms.
- **Build**: `gradlew help` OK (JDK 17).

## Review Fix Log

- 2026-10-13 — Review completata (3 lenti, 8 findings). Fix applicati:
  - `LearnedRulesViewModel`: implementata modifica categoria (`startEditCategory`, `updateCategory`, `cancelEdit`)
  - `LearnedRulesViewModel`: ricerca combinata per espressione + nome prodotto (join catalogo)
  - `LearnedRulesViewModel`: filtraggio regole orfane via `catalogRepository.getById()`
  - `LearnedRulesViewModel`: try/catch su tutte le operazioni CRUD con messaggi errore
  - `LearnedRulesScreen`: UI mostra nomi prodotto/categoria (fallback ID), pulsante Modifica categoria, dialog modifica con lista L3
  - `LearnedRulesViewModel`: try/catch su tutte le azioni CRUD con messaggi errore
  - `LearnedRulesViewModelTest`: aggiunti test per modifica categoria, ricerca per nome prodotto, regole orfane
  - `LearnedRulesViewModel`: aggiunto `editingRule` state per dialog modifica

## Spec Change Log

- 2026-10-13 — Creazione spec e implementazione completata, stato `in-progress` → `done`