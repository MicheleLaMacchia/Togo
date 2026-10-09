---
title: 'Story 2.1: Schermata Lista Attiva — Visualizzazione per Categoria'
type: 'feature'
created: '2026-10-09'
status: 'in-progress'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-1-5-design-system-material3.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'app ha il data layer (Room DAO, Repository, catalogo preinstallato) e il Design System (TogoTheme, token colori/tipografia/spacing/shape/componenti), ma manca la **schermata principale "Lista Attiva" (Spesa)** che mostra le voci raggruppate per categoria canonica nell'ordine tassonomico, usando i componenti del Design System.

**Approach:** Implementare `ActiveListScreen` (Compose) + `ActiveListViewModel` (MVI/UDF) che:
- Osserva `ShoppingListRepository.getActiveItems()` (Flow<List<ShoppingItem>>)
- Raggruppa le voci per `TaxonomyLevel.Level1` → `Level2` → `Level3` rispettando `sortOrder` del catalogo
- Rende ogni categoria come `CategoryHeader` + `LazyColumn` di `ItemRow` (Design System)
- Espone `UiEvent` per: check-off, tap riga (modifica), swipe delete, tap FAB voce, tap icona storico/condividi
- Stato vuoto: messaggio + azioni "Voce" / "Microfono" (placeholder per Story 2.2/4.x)

## Boundaries & Constraints

**Always:**
- Architettura Clean: `ActiveListViewModel` in `presentation`, usa `ShoppingListRepository` (domain) via Koin
- `UiState` immutabile emesso via `StateFlow`, `UiEvent` sealed class per interazioni
- Design System **obbligatorio**: `TogoTheme`, `togoColors()`, `togoTypography()`, `togoSpacing()`, `togoComponentTokens()`
- Componenti: `ItemRowTokens`, `CategoryHeaderTokens`, `CheckboxUtilityTokens`, `QuantityBadgeTokens`, `VoiceFabTokens`, `AppBarTokens`
- Ordine categorie: `TaxonomyLevel.Level1.sortOrder` → `Level2.sortOrder` → `Level3.sortOrder` → `ShoppingItem.name` (alfabetico)
- Target touch ≥48dp su ogni controllo interattivo (checkbox, FAB, badge, header)
- Cold load: nessuna schermata di caricamento, Flow emette subito lista (Room)

**Never:**
- Nessun `MaterialTheme` nudo, nessun colore/tipografia/spacing hardcoded
- Nessuna logica di business nel composable (solo rendering + event emission)
- Nessun accesso diretto a DAO/Room dallo ViewModel (solo Repository interface)
- Nessuna animazione complessa che rallenti interazione una mano (max 300ms micro-delay check-off)

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Cold start lista vuota | DB vuoto, primo avvio | `ActiveListScreen` mostra stato vuoto: icona carrello, testo "La tua lista è vuota. Tocca il microfono o premi + per aggiungere prodotti.", pulsanti "Voce" + "Microfono" (disabled per ora) | Nessuno |
| Lista con voci multiple categorie | DB con voci su L1/L2/L3 diversi | Categorie ordinate per `sortOrder`; dentro ciascuna `ItemRow` ordinate per nome; `CategoryHeader` con badge count | Se categoria senza voci → non renderizzata |
| Check-off voce | Tap checkbox su `ItemRow` | Haptic 15ms → testo barrato, opacità 45%, checkbox riempita → dopo 300ms scivola in sezione "Presi (N)" collassabile | Se repo fallisce → snackbar errore, rollback UI |
| Tap FAB microfono | Tap `VoiceFab` | Emette `UiEvent.NavigateToVoiceCapture` (placeholder Story 4.x) | Nessuno |
| Tap icona Storico | Tap `AppBar` icona storia | Emette `UiEvent.NavigateToHistory` (placeholder Story 5.x) | Nessuno |
| Tap icona Condividi | Tap `AppBar` icona condivisione | Emette `UiEvent.ShareList` (placeholder Story 5.x) | Lista vuota → no share |

## Open Questions

- Animazione "scivola in Presi": `AnimatedVisibility` + `slideInVertically` + `fadeIn` (300ms) — confermare easing?
- `LazyColumn` vs `Column` + `scroll()` — lista max ~50 voci, `Column` ok ma `LazyColumn` più scalabile. **DECISO: `LazyColumn` per coerenza futura.**
- Sezione "Presi" collassabile: default espansa se `checkedItems > 0`, altrimenti collassata. **DECISO: sì.**
- Haptic feedback: `performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)` o `VibrationEffect.createOneShot(15, DEFAULT_AMPLITUDE)`? **DECISO: `VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE)`.**

## Code Map

- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt` — ViewModel MVI, `UiState`, `UiEvent`, `StateFlow`
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` — Composable principale, `LazyColumn` categorie + `ItemRow` + `CategoryHeader` + sezione "Presi" + `VoiceFab` + `AppBar`
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListUiState.kt` — `UiState` (data class), `UiEvent` (sealed), `CategoryGroup` (categoria + voci)
- `app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt` — Composable `ItemRow` (Design System tokens)
- `app/src/main/java/it/togo/app/presentation/activelist/CategoryHeader.kt` — Composable `CategoryHeader`
- `app/src/main/java/it/togo/app/presentation/activelist/PresiSection.kt` — Sezione collassabile "Presi (N)"
- `app/src/main/java/it/togo/app/presentation/activelist/EmptyState.kt` — Stato vuoto
- `app/src/main/java/it/togo/app/di/PresentationModule.kt` — Aggiungere `viewModel { ActiveListViewModel(get()) }`
- `app/src/main/java/it/togo/app/MainActivity.kt` — Sostituire contenuto con `ActiveListScreen`

## Tasks & Acceptance

**Execution:**
- [ ] `ActiveListUiState.kt` — `UiState` (activeGroups, checkedItems, isLoading, error), `UiEvent` sealed (CheckOff, Uncheck, EditItem, DeleteItem, NavigateToVoice, NavigateToHistory, ShareList), `CategoryGroup` (level1, level2s con level3s + items)
- [ ] `ActiveListViewModel.kt` — Costruttore con `ShoppingListRepository`, `CatalogRepository`; `init { loadActiveItems() }`; `onEvent(event)` handle tutti i `UiEvent`; `loadActiveItems()` combina repo + catalogo per raggruppamento ordinato
- [ ] `ActiveListScreen.kt` — `TogoTheme { Scaffold(topBar=AppBar, floatingActionButton=VoiceFab) { LazyColumn categorie -> CategoryHeader + ItemRow* } }`
- [ ] `ItemRow.kt` — `Surface` + `Row` (CheckboxUtility + Text nome + meta + QuantityBadge) + `Modifier.fillMaxWidth()` + `combinedClickable` per tap modifica + swipe delete → `UiEvent`
- [ ] `CategoryHeader.kt` — `Surface` background `surfaceSubtle`, borderLeft 4dp `borderCrisp`, testo uppercase `sectionHeader`, badge count `caption`
- [ ] `PresiSection.kt` — `AnimatedVisibility` + `Column` voci checkate + pulsante "Concludi spesa" (stile `AppBar` border) → `UiEvent.Checkout`
- [ ] `EmptyState.kt` — `Column` centrato: icona carrello 64dp `inkMuted`, testo `itemMeta`, due pulsanti `Button` (Voce) + `IconButton` (Microfono) → `UiEvent`
- [ ] `AppBar` personalizzata — Titolo "Spesa" `titleScreen`, azioni: Storico + Condividi (IconButton) → `UiEvent`
- [ ] `VoiceFab` — `FloatingActionButton` size 64dp, `VoiceFabTokens`, icona microfono, `onClick = { onEvent(NavigateToVoice) }`
- [ ] `PresentationModule.kt` — `viewModel { ActiveListViewModel(get(), get()) }`
- [ ] `MainActivity.kt` — `setContent { ActiveListScreen(onEvent = viewModel::onEvent) }`

**Acceptance Criteria:**
- Given lista vuota, when `ActiveListScreen` renderizzata, then stato vuoto visibile con testi corretti e due azioni
- Given voci su 3+ categorie, when schermata renderizzata, then categorie ordinate per `sortOrder` L1→L2→L3, voci alfabetiche per nome
- Given voce in categoria, when tap checkbox, then haptic 15ms → barrato/opacità → dopo 300ms scivola in "Presi (N)"
- Given voce in "Presi", when tap checkbox, then torna in categoria d'origine istantaneo
- Given "Presi (N)" collassabile, when tap chevron, then espande/collassa con animazione
- Given tap FAB microfono, when click, then `NavigateToVoice` emesso (no crash)
- Given tap icona Storico/Condividi, then eventi corrispondenti emessi
- Tutti i componenti usano **solo** token `TogoTheme` (colori, tipografia, spacing, shape, componentTokens)
- `gradlew help` passa, nessun warning deprecation Compose

## Dev Approvals

- [ ] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

## Spec Change Log

## Review Triage Log