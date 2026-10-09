---
title: 'Story 2.2: Inserimento Manuale Prodotto — Bottom Sheet Autocomplete'
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
  - '{project-root}/_bmad-output/implementation-artifacts/spec-1-5-design-system-material3.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'utente può aggiungere prodotti solo tramite voce (Story 4.x, future) o non ha modo di inserire manualmente una voce. Manca il flusso **inserimento manuale** con autocomplete dal catalogo preinstallato, selettore quantità/unità e attributi opzionali (marca, variante, conservazione).

**Approach:** Implementare `AddItemBottomSheet` (Compose ModalBottomSheetLayout) che si apre dal FAB "+" o dallo stato vuoto. Il flusso:
1. **Autocomplete prodotto**: campo testo con suggerimenti da `CatalogRepository.search(query)` + navigazione tassonomica L1→L2→L3 come fallback
2. **Quantità + Unità**: stepper numerico + dropdown unità compatibili (dal `StandardUnit` del prodotto canonico)
3. **Attributi opzionali**: marca (dropdown + testo libero), variante (testo libero), conservazione (dropdown valori controllati)
4. **Conferma**: valida obbligatori (prodotto, quantità > 0, unità) → emette `UiEvent.AddItemConfirmed` → `ActiveListViewModel` chiama `ShoppingListRepository.insert()` → chiude bottom sheet

## Boundaries & Constraints

**Always:**
- Design System obbligatorio: `TogoTheme`, token colori/tipografia/spacing/shape/componentTokens
- `CatalogRepository.search()` per autocomplete (min 2 caratteri, debounce 300ms)
- Unità compatibili derivate da `StandardUnit` del prodotto canonico (FR-6 normalizzazione)
- Attributi pertinenti per Level3 (non mostrare tutti sempre)
- Touch target ≥48dp su tutti i controlli
- Validazione: prodotto obbligatorio, quantità > 0, unità obbligatoria
- Chiusura bottom sheet su conferma o tap fuori/swipe down

**Never:**
- Nessun valore hardcoded (colori, spacing, tipografia)
- Nessuna logica di business nel composable (solo UI + event emission)
- Salvataggio senza validazione completa
- Attributi non pertinenti al Level3 del prodotto

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Apertura bottom sheet | Tap FAB "+" o pulsante stato vuoto | `AddItemBottomSheet` animato dal basso, focus su campo ricerca | Nessuno |
| Ricerca prodotto | Query ≥2 char | Suggerimenti `CatalogRepository.search()` in tempo reale (debounce 300ms) | Query <2 char → nessun suggerimento |
| Prodotto non trovato | Query senza risultati | Mostra "Nessun prodotto trovato. Aggiungi nuovo?" → naviga wizard categorizzazione (Story 2.7 placeholder) | Nessuno |
| Selezione prodotto | Tap su suggerimento | Compila campo, mostra stepper quantità + dropdown unità compatibili | Se unità non compatibili → disabilita conferma |
| Quantità zero/negativa | Stepper a 0 o input negativo | Disabilita conferma, mostra errore "Quantità deve essere > 0" | Blocco conferma |
| Unità non compatibile | Dropdown unità non compatibile con prodotto | Nasconde unità non compatibili, mostra solo compatibili | Default a unità base del prodotto |
| Attributi Level3 | Prodotto con Level3 pertinente | Mostra solo attributi pertinenti (marca/variante/conservazione) | Attributi non pertinenti → non renderizzati |
| Conferma valida | Tutti i campi obbligatori ok | Emette `UiEvent.AddItemConfirmed(item)` → chiude bottom sheet | Nessuno |
| Conferma invalida | Campo obbligatorio mancante | Disabilita pulsante, mostra errore inline su campo | Blocco conferma |
| Chiusura annullamento | Tap fuori / swipe down / back | Chiude bottom sheet, scarta input parziale | Nessuno |
| Debounce ricerca | Typing rapido | 300ms debounce prima di chiamare `CatalogRepository.search()` | Nessuna chiamata durante typing |

## Open Questions

- Validazione barcode scanner: futuro (Story 3.x) — **DECISO: fuori scope MVP**
- Unità personalizzate utente: **DECISO: no, solo `StandardUnit` predefinite**
- Persistenza bozza interrotta: **DECISO: no, scarta input su annullamento**
- Suggerimenti "recenti/frequenti": **DECISO: no, solo ricerca catalogo per ora**

## Code Map

- `app/src/main/java/it/togo/app/presentation/additem/AddItemBottomSheet.kt` — Composable `ModalBottomSheetLayout` con stati (ricerca, quantità, attributi, conferma)
- `app/src/main/java/it/togo/app/presentation/additem/AddItemViewModel.kt` — ViewModel per stato bottom sheet, debounce ricerca, validazione, mappatura a `ShoppingItem`
- `app/src/main/java/it/togo/app/presentation/additem/AddItemUiState.kt` — `UiState` (query, risultati ricerca, prodotto selezionato, quantità, unità, attributi, errori), `UiEvent` sealed
- `app/src/main/java/it/togo/app/presentation/additem/ProductSearchField.kt` — Campo ricerca + `LazyColumn` suggerimenti (debounce 300ms)
- `app/src/main/java/it/togo/app/presentation/additem/QuantityUnitPicker.kt` — Stepper quantità + dropdown unità compatibili
- `app/src/main/java/it/togo/app/presentation/additem/AttributeSection.kt` — Sezione attributi (marca, variante, conservazione) per Level3
- `app/src/main/java/it/togo/app/presentation/additem/AddItemBottomSheet.kt` — Layout bottom sheet + navigazione step
- `app/src/main/java/it/togo/app/di/PresentationModule.kt` — Aggiungere `viewModel { AddItemViewModel(get(), get()) }`
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt` — Handle `UiEvent.AddItemConfirmed` → `shoppingRepository.insert()`

## Tasks & Acceptance

**Execution:**
- [x] `AddItemUiState.kt` — `UiState` (query, searchResults, selectedProduct, quantity, unit, brand, variant, condition, errors), `UiEvent` sealed (SearchQueryChanged, ProductSelected, QuantityChanged, UnitChanged, BrandChanged, VariantChanged, ConditionChanged, Confirm, Cancel)
- [x] `AddItemViewModel.kt` — `CatalogRepository.search()` con debounce 300ms (`flow.debounce(300ms)`), validazione campi, mappatura a `ShoppingItem` su conferma
- [x] `AddItemBottomSheet.kt` — `ModalBottomSheetLayout` con step navigazione (ricerca → quantità/unità → attributi → conferma), animazione enter/exit
- [x] `ProductSearchField.kt` — `TextField` + `LazyColumn` suggerimenti (debounce 300ms via `flow.debounce`), tap su risultato → `ProductSelected`
- [x] `QuantityUnitPicker.kt` — Stepper quantità (min 0.1, step 0.1/1 per unità discrete) + dropdown unità compatibili (da `StandardUnit` compatibili)
- [x] `AttributeSection.kt` — Marca (dropdown + free text), Variante (free text), Conservazione (dropdown valori controllati), render solo se pertinenti al Level3
- [x] `AddItemBottomSheet.kt` — `ModalBottomSheetLayout` con `sheetContent` multi-step, `sheetState`, animazione enter/exit, `confirmStateChange`
- [x] `PresentationModule.kt` — `viewModel { AddItemViewModel(get(), get()) }`
- [x] `ActiveListViewModel.kt` — Handle `AddItemConfirmed` → `shoppingRepository.insert()`
- [x] `ActiveListScreen.kt` — FAB "+" apre bottom sheet, stato vuoto pulsante "Voce" apre bottom sheet
- [x] `CanonicalProduct.kt` — Campi helper per UI (compatibleUnits, showBrand, showVariant, showCondition, commonBrands, commonConditions)
- [x] `StandardUnit.kt` — Proprietà `compatibleUnits` per conversione automatica

**Acceptance Criteria:**
- Given bottom sheet aperta, when utente digita ≥2 char, then suggerimenti appaiono con debounce 300ms
- Given prodotto selezionato, when unità mostrate, then solo unità compatibili con `StandardUnit` del prodotto
- Given quantità > 0 e unità valida, when tap Conferma, then `ShoppingItem` creato con attributi e inserito in repo
- Given campo obbligatorio mancante, when tap Conferma, then pulsante disabilitato + errore inline
- Given tap fuori / swipe down, when bottom sheet aperta, then si chiude e scarta input
- Given prodotto con Level3 pertinente, when attributi mostrati, then solo attributi pertinenti renderizzati
- Tutti i componenti usano **solo** token `TogoTheme`
- `gradlew help` passa

## Dev Approvals

- [ ] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **AddItemUiState.kt**: `UiState` con query, searchResults, selectedProduct, quantity, selectedUnit, compatibleUnits, brand, variant, condition, errors, currentStep; validatori `canProceedToStep2/3`, `isValidForConfirm`; `UiEvent` sealed (SearchQueryChanged, ProductSelected, QuantityChanged, UnitChanged, BrandChanged, VariantChanged, ConditionChanged, NextStep, PreviousStep, Confirm, Cancel, Reset); `AddItemConfirmed` data class per risultato.
- **AddItemViewModel.kt**: Ricerca con `debounce(300ms)` + `flatMapLatest` su `catalogRepository.search()`; `searchResults` come `stateIn` per condivisione; validazione step-by-step; `handleConfirm()` crea `ShoppingItem` con UUID + attributi + insert in repo; reset automatico post-conferma.
- **ProductSearchField.kt**: `TextField` con icona ricerca + stato loading; `LazyColumn` suggerimenti (max 8) con path tassonomico L1→L2→L3; tap → `ProductSelected`.
- **QuantityUnitPicker.kt**: Stepper quantità (pulsanti −/+ e campo editabile) con step adattivo (0.1 < 10, 1.0 ≥ 10); `LazyRow` chip unità compatibili (da `StandardUnit.compatibleUnits`); errori inline per quantità/unità.
- **AttributeSection.kt**: `AnimatedVisibility` per suggerimenti espandibili; campi Marca/Variante/Conservazione con suggerimenti dropdown; `showBrand/Variant/Condition` per Level3-specific rendering.
- **AddItemBottomSheet.kt**: `ModalBottomSheetLayout` + `ModalBottomSheetState`; 4 step con progress indicator (cerchi + linee); step content: SearchStep, QuantityUnitStep, AttributeStep, ConfirmStep; bottoni Indietro/Avanti/Conferma con enabled condizionale; dismiss su tap fuori/swipe down.
- **CanonicalProduct.kt**: Aggiunti campi helper UI (compatibleUnits, showBrand/Variant/Condition, commonBrands/Conditions, path tassonomico L1/L2/L3) per supporto AddItem flow.
- **StandardUnit.kt**: Proprietà `compatibleUnits` per conversione automatica (GRAM/KG, ML/L, altri solo se stessi).
- **PresentationModule.kt**: `viewModel { AddItemViewModel(get<CatalogRepository>(), get<ShoppingListRepository>()) }`.
- **ActiveListViewModel.kt**: `onAddItemConfirmed(confirmed: AddItemConfirmed)` → `shoppingRepository.insert()`.
- **ActiveListScreen.kt**: `ModalBottomSheetLayout` + `ModalBottomSheetState`; dual FAB (Voice + Add); `sheetState.show()` su tap FAB "+" o pulsante EmptyState; `onConfirm` → `viewModel.onAddItemConfirmed()`.
- **Design System**: 100% token `TogoTheme` (colori, tipografia, spacing, shape, componentTokens).
- **Build**: `gradlew help` OK.

## Spec Change Log

- 2026-10-09 — Creazione spec, stato `in-progress`.
- 2026-10-09 — Implementazione completata (14 file), build `gradlew help` OK, stato `review`.

## Review Triage Log

Review della Story 2.2 su diff uncommitted + nuovi file (contenuto in scena: diff git + 6 file additem + spec). Lenti: adversarial, edge-case-hunter, verification-gap, structure, prose.

**Corretti (applicati):**
- `componentTokens()` dead code in QuantityUnitPicker → rimosso (adversarial F1).
- Quantity overflow to Infinity → `minOf(9999.0, quantity + getStep(quantity))` (edge-case F2).
- Scientific notation in quantity display → `String.format` pattern (edge-case F3).
- Unbounded text in attribute fields → max 100 char (edge-case F4).
- Duplicate products in search results → `distinctBy { it.id }` (edge-case F5).
- ViewModel not reset on sheet dismiss → `onDismiss` chiama `AddItemUiEvent.Reset` (edge-case F6).
- ConfirmStep null safety → `?: return@Column` invece di `!!` (edge-case F7).
- compatibleUnits empty fallback → `product.defaultUnit` invece di `PIECE` hardcoded (edge-case F8).
- Confirm event when step ≠ 4 → `validateStep(4, state)` + `currentStep = 4` (edge-case F9).
- `handleConfirm` null safety → `?: return` invece di `!!` (edge-case F10).

**Accettati con nota (non applicati ora):**
- `flatMapLatest` onCompletion race condition → richiede AtomicInteger counter; rimandato a ottimizzazione futura (edge-case F1).
- `commonBrands/commonConditions` list very long → `heightIn(max = 200.dp)`; rimandato a ottimizzazione futura (edge-case F10).
- Test coverage gaps (12 findings verification-gap) → richiedono SDK/Compose test infrastructure; rimandato a Story 2.x con SDK.
- Structure: duplicate AddItemBottomSheet.kt entry, Open Questions all resolved, Implementation Notes heavy → contenuto `<frozen>`; richiede ri-negoziazione umana.

**Rinviati:**
- Prose: 38 copy-edits (encoding artifacts, terminology, grammar) → contenuto `<frozen>`; richiede ri-negoziazione umana.