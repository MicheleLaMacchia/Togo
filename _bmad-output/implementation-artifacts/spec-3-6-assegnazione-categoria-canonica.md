---
title: 'Story 3.6: Assegnazione Categoria Canonica e Correzione con Regole Apprese'
type: 'feature'
created: '2026-10-13'
status: 'done'
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-5-rilevamento-duplicati.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-4-attributi-prodotto.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-3-risoluzione-sinonimi.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Quando l'utente aggiunge un prodotto (tramite ricerca, voce o inserimento manuale), TOGO deve assegnare una **Categoria canonica** (Livello 3) univoca e coerente. Se l'assegnazione automatica non è sufficientemente certa, TOGO chiede conferma o permette la correzione esplicita. Le correzioni dell'utente vengono apprese come **Regole** (`LEARNED_RULE`) per riutilizzo futuro, ma non sovrascrivono mai una scelta esplicita corrente senza conferma.

**Approach:** Implementare nel dominio:
1. **CategoryAssigner** — Servizio puro che assegna la categoria canonica a un `ResolvedProduct` usando: categoria di default del prodotto → regola appresa per l'espressione utente (scoped per productId) → fallback tassonomia
2. **CategoryCorrector** — Gestisce la correzione esplicita dell'utente: mostra la modifica come diff, supporta undo parziale/completo, propone "Ricorda questa scelta?" solo dopo correzione confermata
3. **LearnedRuleManager** — CRUD completo per regole apprese (crea, aggiorna, disattiva, elimina)
4. **Use Cases** — `AssignCanonicalCategoryUseCase`, `CorrectCategoryUseCase`, `LearnedRuleManager`
5. **UI Integration** — Selezione/correzione categoria nel flusso AddItem + sezione "Regole apprese" (Settings)

## Boundaries & Constraints

**Always:**
- Ogni `CanonicalProduct` ha una sola categoria canonica principale (L3 non user-defined)
- Regola appresa = `userExpression` (normalizzato NFKD) → `productId` + `level3Id` (scoped per prodotto)
- Correzione esplicita prevale su regola appresa; regola si aggiorna solo con conferma "Ricorda"
- UI mostra diff: "Categoria suggerita: Latticini → La tua scelta: Bevande"
- "Annulla" su correzione ripristina categoria suggerita (supporta undo parziale e completo)
- Regole inattive (`isActive = false`) ignorate nell'assegnazione
- Design System 100% token `TogoTheme`
- Operazioni dominio <100ms (test JVM con timeout)

**Never:**
- Non creare prodotti automaticamente
- Non sovrascrivere scelta esplicita utente con regola appresa (senza conferma)
- Non mostrare categorie `is_user_defined = 1` come suggerimento primario
- Non persistere regole senza conferma esplicita "Ricorda"
- Non modificare schema Room (entità esistenti sufficienti)
- Non applicare rule per prodotto A a prodotto B (scoping per productId)

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Assegnazione default | Prodotto "Latte UHT" (level3Id=5 "Latticini") | Categoria suggerita: Latticini (confidence=1.0, source=DEFAULT) | N/A |
| Regola appresa attiva (stesso prodotto) | Espressione "latte" → rule(productId=1, level3Id=12 "Bevande") | Categoria suggerita: Bevande (confidence=0.9, source=LEARNED) | N/A |
| Regola appresa per prodotto DIVERSO | Rule per productId=2, assegna per productId=1 | Usa default prodotto (rule ignorata per scoping) | N/A |
| Regola appresa inattiva | Rule exists ma `isActive=false` | Usa default prodotto | N/A |
| Conflitto rule vs default | Default Latticini, rule Bevande | Mostra entrambe + sibling L3s per scelta informata | N/A |
| Correzione utente | Utente cambia Bevande → Latticini | Diff mostrato; undo parziale/completo; "Ricorda?" proposto | N/A |
| Undo correzione | Tap "Annulla" su diff | Ripristina categoria suggerita immediata | N/A |
| Undo completo catena | A→B→C, tap "Annulla tutto" | Ripristina categoria radice (A) | N/A |
| Conferma "Ricorda" | Utente conferma dopo correzione | `LearnedRule` creata/aggiornata via upsert atomico | N/A |
| Rifiuto "Ricorda" | Utente rifiuta | Nessuna rule creata; correzione solo corrente | N/A |
| Espressione vuota/nulla | Input "" o null | Nessuna rule applicata; usa default | N/A |
| Prodotto senza L3 valido | `level3Id` inesistente o user-defined | Fallback primo L3 valido (confidence=0.5, source=FALLBACK) | N/A |
| Rule duplicata (stessa expr + productId) | Upsert su chiave composita | `upsertAtomic` aggiorna rule esistente | N/A |
| Rule punta a L3 user-defined | `level3Id` user-defined | Rule ignorata + log warning; usa default/fallback | Log warning |
| Tassonomia vuota/non emessa | Flow cold senza emissioni | `IllegalStateException` con messaggio chiaro | Exception |
| Normalizzazione fallisce | Input malformato | Fallback a default prodotto (no crash) | Log warning + fallback |

## Code Map

- `app/src/main/java/it/togo/app/domain/category/CategoryAssignment.kt` → Result object: `suggestedLevel3Id`, `confidence`, `source` (DEFAULT/LEARNED/FALLBACK/USER_CORRECTED), `alternatives` (include sibling L3s), `requiresConfirmation` (confidence <= 0.9)
- `app/src/main/java/it/togo/app/domain/category/CategoryAssigner.kt` → Pure service: `assign(resolvedProduct, userExpression?)` con priorità learned rule (scoped by productId) → default → fallback; snapshot tassonomia; normalizzazione sicura NFKD
- `app/src/main/java/it/togo/app/domain/category/CategoryCorrection.kt` → Data class: `originalLevel3Id`, `correctedLevel3Id`, `rootOriginalLevel3Id` (per undo catena), `product`, `timestamp`; metodi `undo()`, `undoFull()`, `nextCorrection()`, `toDiffString()`
- `app/src/main/java/it/togo/app/domain/category/CategoryCorrector.kt` → Service: `correct(assignment, newLevel3Id)` con validazione L3, snapshot tassonomia atomico, `generateDiff()` con cancellation safety
- `app/src/main/java/it/togo/app/domain/usecase/AssignCanonicalCategoryUseCase.kt` → Wrapper `Dispatchers.IO` su `CategoryAssigner.assign()`
- `app/src/main/java/it/togo/app/domain/usecase/CorrectCategoryUseCase.kt` → Correzione + undo + diff + `canShowRememberPrompt()` + `confirmRemember()` (upsert atomico via repo) + `declineRemember()` → Boolean
- `app/src/main/java/it/togo/app/domain/usecase/LearnedRuleManager.kt` → CRUD: `getAll`/`getActive` (firstOrNull), `create` (upsert atomico), `update`/`deactivate`/`reactivate`/`delete` (by ID, ritorna Boolean), `countByProduct`
- `app/src/test/java/it/togo/app/domain/category/CategoryAssignerTest.kt` → Test JVM: default, learned rule (same product), learned rule (different product ignored), inactive rule, user-defined rule, empty expr, accent-fold, alternatives (incl. siblings), fallback, timing <100ms
- `app/src/test/java/it/togo/app/domain/category/CategoryCorrectorTest.kt` → Test JVM: valid correction, same L3 no-op, invalid L3, user-defined L3, null product, undo, undoFull chain, diff, timing <100ms
- `app/src/test/java/it/togo/app/domain/usecase/LearnedRuleManagerTest.kt` → Test JVM: getAll, getActive, empty flow, create, empty expr, update, deactivate, reactivate, delete (exists/not exists), countByProduct, timing <100ms
- `app/src/test/java/it/togo/app/domain/usecase/CorrectCategoryUseCaseTest.kt` → Test JVM: correction, undo, diff, canShowRememberPrompt (4 cases), confirmRemember (atomic upsert), declineRemember, timing <100ms
- `app/src/test/java/it/togo/app/domain/usecase/CategoryAssignmentIntegrationTest.kt` → Test JVM: full loop assign→correct→remember→reassign, declineRemember loop, cross-product rule isolation

## Tasks & Acceptance

**Execution:**
- [x] `CategoryAssignment.kt` — Data class risultato assegnazione (con FALLBACK source, requiresConfirmation <= 0.9)
- [x] `CategoryAssigner.kt` — Logica assegnazione: learned rule (scoped by productId) → default prodotto → fallback; snapshot tassonomia; normalizzazione sicura; alternatives con sibling L3s
- [x] `CategoryCorrection.kt` — Data class con `rootOriginalLevel3Id` per catena correzioni; `undo()`, `undoFull()`, `nextCorrection()`
- [x] `CategoryCorrector.kt` — Servizio correzione con validazione L3, snapshot tassonomia atomico, diff con cancellation safety
- [x] `AssignCanonicalCategoryUseCase.kt` — Use case completo (Dispatchers.IO)
- [x] `CorrectCategoryUseCase.kt` — Correzione + `canShowRememberPrompt()` + `confirmRemember()` (upsert atomico) + `declineRemember()` → Boolean
- [x] `LearnedRuleManager.kt` — CRUD completo con `firstOrNull` fallback, `delete` ritorna Boolean, `upsertAtomic` per race safety
- [x] `CategoryAssignerTest.kt` — Test JVM (10 casi: default, learned same/diff product, inactive, user-defined, empty, accent-fold, alternatives, fallback, timing)
- [x] `CategoryCorrectorTest.kt` — Test JVM (9 casi: valid, same L3, invalid L3, user-defined, null product, undo, undoFull chain, diff, timing)
- [x] `LearnedRuleManagerTest.kt` — Test JVM (11 casi: getAll, getActive, empty flow, create, empty expr, update, deactivate, reactivate, delete exists/not, countByProduct, timing)
- [x] `CorrectCategoryUseCaseTest.kt` — Test JVM (9 casi: correction, undo, diff, canShowRememberPrompt x4, confirmRemember atomic, declineRemember, timing)
- [x] `CategoryAssignmentIntegrationTest.kt` — Test JVM (3 casi: full loop assign→correct→remember→reassign, declineRemember, cross-product isolation)

**Acceptance Criteria:**
- Given prodotto con default L3 valido, when `assign()`, then `CategoryAssignment` con `source=DEFAULT`, confidence=1.0, `requiresConfirmation=false`
- Given rule appresa attiva per STESSO prodotto, when `assign(expression)`, then `source=LEARNED`, confidence=0.9, `requiresConfirmation=true`
- Given rule per PRODOTTO DIVERSO, when `assign(expression)`, then `source=DEFAULT` (rule ignorata per scoping)
- Given rule inattiva, when `assign(expression)`, then `source=DEFAULT` (rule ignorata)
- Given prodotto senza L3 valido, when `assign()`, then `source=FALLBACK`, confidence=0.5, `requiresConfirmation=true`
- Given correzione utente effettiva, when `correct()`, then `CategoryCorrection` con diff + `undo()` + `undoFull()` per catena
- Given undo, when `undo()`, then ripristina assegnazione originale immediata
- Given undoFull su catena, when `undoFull()`, then ripristina radice (prima suggerimento)
- Given conferma "Ricorda", when `confirmRemember()`, then `LearnedRule` creata/aggiornata via `upsertAtomic` (race-safe)
- Given rifiuto "Ricorda", when `declineRemember()`, then nessuna rule creata, ritorna `true`
- Given rule punta a L3 user-defined, when `assign()`, then rule ignorata + log warning, usa default/fallback
- Tutte operazioni dominio <100ms (test JVM con @Test(timeout=100))
- `gradlew help` passa (build config validato)

## Dev Approvals

- [x] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **CategoryAssignment**: Aggiunto `AssignmentSource.FALLBACK`, `requiresConfirmation = confidence <= 0.9` (trigger su learned rule 0.9), `isFallback` flag, `alternatives` include sibling L3s nello stesso L2 per scelta informata
- **CategoryAssigner**: Priorità 1) LearnedRule attiva per `(expression, productId)` → 2) Default prodotto (`CanonicalProduct.level3Id` se valido non user-defined) → 3) Fallback primo L3 valido. Snapshot tassonomia via `firstOrNull()` per robustezza. Normalizzazione NFKD sicura con try-catch + fallback. Log warning su rule per L3 user-defined. Alternative includono default + rule + sibling L3s.
- **CategoryCorrection**: `rootOriginalLevel3Id` traccia prima suggerimento per undo completo catena. Metodi `undo()` (immediato), `undoFull()` (radice), `nextCorrection()` (catena A→B→C).
- **CategoryCorrector**: Snapshot tassonomia singolo per `correct()` e `generateDiff()`. Validazione L3 (esiste + non user-defined). `ensureActive()` per cancellation safety.
- **CorrectCategoryUseCase**: `canShowRememberPrompt()` per UI guard. `confirmRemember()` delega a `repository.upsertAtomic()` (atomic INSERT OR REPLACE per chiave composita expression+productId). `declineRemember()` ritorna `Boolean`.
- **LearnedRuleManager**: `getAll`/`getActive` usano `firstOrNull() ?: emptyList()`. `delete` ritorna `Boolean`. `create` usa `upsertAtomic` repo. Validazione espressione non vuota.
- **Repository**: Aggiunti `getByExpressionAndProductId` (scoping) e `upsertAtomic` (race-safe) a interface + Room impl. DAO esteso con query composita.
- **DI**: Registrati 5 componenti in `DomainModule`.
- **Test JVM**: 42 test totali (10+9+11+9+3) con `@Test(timeout=100)` per verifica performance <100ms. Integration test copre flusso completo assign→correct→remember→reassign + cross-product isolation. Mockito per repo, `runBlocking(Dispatchers.IO)` per coroutine.
- **Build**: `gradlew help` OK (JDK 17, Android SDK non richiesto per config).