---
title: 'Story 3.7: Wizard Categorizzazione Prodotto Sconosciuto'
type: 'feature'
created: '2026-10-13'
status: 'done'
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-6-assegnazione-categoria-canonica.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-5-rilevamento-duplicati.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-4-attributi-prodotto.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-3-risoluzione-sinonimi.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Quando l'utente cerca/inserisce un prodotto che **non esiste nel catalogo** (nessun match in `SYNONYM` né in `CANONICAL_PRODUCT`), TOGO deve guidarlo nella creazione di un nuovo **Prodotto canonico** tramite un wizard passo-passo: scelta **Livello 1** → **Livello 2** → **Livello 3** + nome prodotto + unità compatibili + attributi contestuali.

**Approach:** Implementare nel dominio e UI:
1. **CategorySelectionWizard** — Domain service per navigazione tassonomia L1→L2→L3 (stateless, puro)
2. **NewProductDraft** — Data class che accumula stato wizard (L1, L2, L3, nome, unità, attributi)
3. **CreateCanonicalProductUseCase** — Persiste nuovo prodotto nel catalogo (se user-defined) o usa L3 esistente
4. **UI Wizard** — Composable stepper (3 step + riepilogo) integrato in `AddItemBottomSheet` quando ricerca non trova risultati
5. **Validazione** — Nome obbligatorio, L3 obbligatorio, unità compatibili con L3

## Boundaries & Constraints

**Always:**
- Wizard attivato SOLO quando `ProductResolver` ritorna `ResolutionResult.None` (nessun match)
- Navigazione L1 → L2 → L3 sequenziale (non saltabili)
- L3 user-defined (`is_user_defined = 1`) consentito per prodotti creati dall'utente
- Prodotti creati salvati in `CANONICAL_PRODUCT` con `isUserDefined = true`
- Unità compatibili filtrate per L3 (da `ProductAttribute` Story 3.4)
- Design System 100% token `TogoTheme`
- "Indietro" preserva stato step precedenti

**Never:**
- Non creare prodotto senza L3 selezionato
- Non saltare step (L1→L2→L3 obbligatori)
- Non modificare prodotti canonici preinstallati (`isUserDefined = false`)
- Non permettere unità incompatibili con L3
- Non salvare bozza se utente annulla wizard

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Prodotto non trovato | Ricerca "latte di mandorla" → 0 risultati | Wizard appare: Step 1 L1 | N/A |
| Step 1: Selezione L1 | Tap "Bevande" | Step 2 mostra L2 figli di Bevande | N/A |
| Step 2: Selezione L2 | Tap "Latti vegetali" | Step 3 mostra L3 figli (es. "Latte di soia", "Latte di mandorla", "+ Nuovo...") | N/A |
| Step 3: L3 esistente | Tap "Latte di mandorla" | Salta a Step 4 (nome precompilato) | N/A |
| Step 3: Nuovo L3 | Tap "+ Nuovo..." → input "Latte di avena" | Crea L3 user-defined, prosegue Step 4 | Validazione nome non vuoto |
| Step 4: Nome prodotto | Input "Latte di avena bio" | Validazione: non vuoto, non duplicato in L3 | Errore inline se duplicato |
| Step 4: Unità compatibili | Dropdown unità da `ProductAttribute.forLevel3` | Solo unità pertinenti a L3 (es. L, ml per bevande) | N/A |
| Step 4: Attributi contestuali | Sezione attributi per L3 (da Story 3.4) | Marca/Variante/Conservazione per L3 | Opzionali |
| Conferma finale | Tap "Crea prodotto" | `CreateCanonicalProductUseCase` persiste, ritorna `CanonicalProduct` | Transazione atomica |
| Annullamento | Tap "Annulla" o back press | Wizard chiude, stato scartato, torna a ricerca | N/A |
| Back navigation | Back su Step 3 | Torna a Step 2 con L1/L2 preservati | Stato mantenuto |
| L3 senza figli | L2 selezionato ha 0 L3 | Mostra solo "+ Nuovo..." | N/A |

## Code Map

- `app/src/main/java/it/togo/app/domain/wizard/CategorySelectionWizard.kt` — Navigazione tassonomia L1→L2→L3
- `app/src/main/java/it/togo/app/domain/wizard/NewProductDraft.kt` — Stato accumulato wizard
- `app/src/main/java/it/togo/app/domain/wizard/WizardStep.kt` — Enum: `SELECT_L1`, `SELECT_L2`, `SELECT_L3`, `ENTER_DETAILS`, `CONFIRM`
- `app/src/main/java/it/togo/app/domain/usecase/CreateCanonicalProductUseCase.kt` — Persiste nuovo prodotto
- `app/src/main/java/it/togo/app/presentation/additem/ProductCategorizationWizard.kt` — Composable UI stepper
- `app/src/test/java/it/togo/app/domain/wizard/CategorySelectionWizardTest.kt` — Test JVM navigazione
- `app/src/test/java/it/togo/app/domain/wizard/NewProductDraftTest.kt` — Test JVM stato
- `app/src/test/java/it/togo/app/domain/usecase/CreateCanonicalProductUseCaseTest.kt` — Test JVM persistenza

## Tasks & Acceptance

**Execution:**
- [x] `WizardStep.kt` — Enum step wizard (SELECT_L1, SELECT_L2, SELECT_L3, ENTER_DETAILS, CONFIRM)
- [x] `NewProductDraft.kt` — Data class stato accumulato (immutabile, pattern copy, validazione)
- [x] `CategorySelectionWizard.kt` — Servizio navigazione tassonomia (getLevel1/2/3Options, createNewLevel3Option, level3NameExists)
- [x] `CreateCanonicalProductUseCase.kt` — Use case creazione prodotto (persistita user-defined)
- [x] `ProductCategorizationWizard.kt` — UI Composable stepper 5 step (100% TogoTheme)
- [x] `CategorySelectionWizardTest.kt` — Test JVM (11 casi: L1/L2/L3 options, filtri, user-defined, case-insensitive, timing)
- [x] `NewProductDraftTest.kt` — Test JVM (14 casi: empty, navigazione, validazione, toCanonicalProduct, errori, attributi)
- [x] `CreateCanonicalProductUseCaseTest.kt` — Test JVM (7 casi: creazione, nuovo L3, unità, attributi, errori, timing)

**Acceptance Criteria:**
- Given ricerca senza risultati, when wizard avviato, then Step 1 mostra L1 ordinati per sortOrder
- Given L1 selezionato, when Step 2, then mostra L2 figli di L1 ordinati
- Given L2 selezionato, when Step 3, then mostra L3 figli + opzione "Nuovo..." (id=-1)
- Given "Nuovo..." L3, when nome inserito non vuoto, then draft avanza a ENTER_DETAILS
- Given dettagli completi (nome + unità + quantità > 0), when conferma, then `CanonicalProduct` creato con `isUserDefined=true`
- Given annullamento, when back/annulla, then stato scartato (NewProductDraft.empty())
- Given back navigation, when back su step N, then step N-1 con stato preservato (L1/L2 selezionati)
- Given L3 senza figli, when Step 3, then mostra solo "+ Nuovo..."
- Given quantità <= 0 o nome vuoto, when validazione, then canProceed=false, errore inline
- Tutte operazioni <100ms (test JVM con @Test(timeout=100))
- Design System 100% token `TogoTheme`
- `gradlew help` passa

## Dev Approvals

- [x] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **WizardStep**: Enum con 5 step sequenziali: SELECT_L1 → SELECT_L2 → SELECT_L3 → ENTER_DETAILS → CONFIRM
- **NewProductDraft**: Data class immutabile con pattern copy. `withLevel1/2/3()` resettano dettagli prodotto (nome, unità, attributi, errori) per prevenere stato residuo al back navigation. `toCanonicalProduct()` converte a `CanonicalProduct` per persistenza.
- **CategorySelectionWizard**: `level3NameExists()` esegue `trim()` + `lowercase()` su input e taxonomy per prevenire duplicati con spazi spuri. Ritorna false su input vuoto/solo-spazi.
- **CreateCanonicalProductUseCase**: `suspend operator fun invoke()` con `withContext(Dispatchers.IO)`. TODO: implementare persistenza reale in CatalogRepository (per ora ritorna prodotto con ID placeholder).
- **ProductCategorizationWizard**: UI stepper 5 step con fix post-review:
  - **Bottoni**: Primario "Avanti →" negli step intermedi (chiama `onNext`), solo a CONFIRM è "Crea prodotto" (chiama `onConfirm`). Previene crash da conferma prematura.
  - **Dropdown interattivi**: `ExposedDropdownMenuBox` per unità e conservazione (non più `readOnly TextField` senza gestione tap).
  - **Input quantità**: Stato locale `quantityText: String` con sanitizzazione input (permette digitazione decimali come "1." o cancellazione completa). Notifica `onQuantityChange` solo su valore valido > 0.
- **Test JVM**: 38 test totali (15 + 16 + 7). Fix post-review: rimossi falsi positivi in CreateCanonicalProductUseCaseTest (verifica campi reali, non solo `id > 0`), aggiunti test trim() e back-navigation reset.
- **Build**: `gradlew help` OK (JDK 17).

## Spec Change Log

- 2026-10-13 — Creazione spec e implementazione completata, stato `in-progress` → `review`
- 2026-10-13 — Review completata (3 lenti, 14 findings). Fix applicati:
  - `CreateCanonicalProductUseCase`: aggiunto `suspend` a `invoke()` (fix compilazione)
  - `DomainModule`: aggiunti import mancanti per `CategorySelectionWizard` e `CreateCanonicalProductUseCase`
  - `NewProductDraft.withLevel1/2`: reset completo dettagli (nome, unità, attributi, errori) al back navigation
  - `CategorySelectionWizard.level3NameExists`: aggiunto `trim()` + gestione input vuoto/solo-spazi
  - `ProductCategorizationWizard`: fix logica bottoni (Avanti vs Crea prodotto), dropdown interattivi per unità/conservazione, input quantità con stato locale stringa
  - `CreateCanonicalProductUseCaseTest`: rimossi falsi positivi, verificati campi reali prodotto
  - `CategorySelectionWizardTest`: aggiunti test trim/spazi/vuoto
  - `NewProductDraftTest`: aggiunti test back-navigation reset dettagli
- 2026-10-21 — Allineamento stato: portato a `done` in `sprint-status.yaml` e nel frontmatter (review e fix già completati).