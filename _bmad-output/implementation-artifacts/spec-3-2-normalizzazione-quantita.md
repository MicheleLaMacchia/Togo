---
title: 'Story 3.2: Normalizzazione Quantità e Conversioni Autorizzate'
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
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-1-ricerca-full-text-catalogo.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'utente inserisce quantità in vari formati ("1 kg", "500 g", "2 l", "250 ml") e il sistema deve normalizzarle in un'unità canonica per confronti, somme e storage. Le conversioni tra unità compatibili (kg↔g, l↔ml) devono essere automatiche; le unità incompatibili (es. pezzi vs grammi) non devono essere convertite.

**Approach:** Estendere `StandardUnit` con:
1. **Fattori di conversione** verso unità base (GRAM per peso, MILLILITER per volume)
2. **Funzione di normalizzazione** `normalize(quantity, unit) -> Pair<Double, StandardUnit>` che converte all'unità base
3. **Funzione di conversione** `convert(quantity, from, to) -> Double?` che ritorna null se unità incompatibili
4. **Validazione** quantità > 0, non NaN, non Infinity
5. **Formattazione intelligente** `format(quantity, unit) -> String` che sceglie l'unità più leggibile (es. 1500 g → "1.5 kg")

## Boundaries & Constraints

**Always:**
- Conversioni solo tra unità compatibili (stessa dimensione fisica)
- Unità base: GRAM (peso), MILLILITER (volume)
- Quantità normalizzate > 0, finite, non NaN
- Precisione: Double (64-bit IEEE 754)
- Arrotondamento: 2 cifre decimali per display, pieno per calcoli interni

**Never:**
- Conversioni tra unità incompatibili (es. pezzi → grammi)
- Quantità ≤ 0, NaN, Infinity
- Perdita di precisione in catena di conversioni (sempre partire dall'unità base)
- Modifica del database `catalog.db` (solo codice Kotlin)

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Conversione kg → g | (1.5, KILOGRAM, GRAM) | 1500.0 | N/A |
| Conversione g → kg | (1500, GRAM, KILOGRAM) | 1.5 | N/A |
| Conversione l → ml | (2.5, LITER, MILLILITER) | 2500.0 | N/A |
| Conversione ml → l | (500, MILLILITER, LITER) | 0.5 | N/A |
| Unità incompatibili | (1, PIECE, GRAM) | null | Ritorna null, nessuna eccezione |
| Quantità zero | (0, GRAM, KILOGRAM) | null | Quantità non valida |
| Quantità negativa | (-5, GRAM, KILOGRAM) | null | Quantità non valida |
| Quantità NaN | (NaN, GRAM, KILOGRAM) | null | Quantità non valida |
| Quantità Infinity | (Infinity, GRAM, KILOGRAM) | null | Quantità non valida |
| Normalizzazione peso | (2, KILOGRAM) | (2000.0, GRAM) | N/A |
| Normalizzazione volume | (1.5, LITER) | (1500.0, MILLILITER) | N/A |
| Formattazione 1500 g | (1500, GRAM) | "1.5 kg" | N/A |
| Formattazione 500 g | (500, GRAM) | "500 g" | N/A |
| Formattazione 2500 ml | (2500, MILLILITER) | "2.5 L" | N/A |
| Formattazione 500 ml | (500, MILLILITER) | "500 ml" | N/A |
| Unità non convertibili | (1, PIECE) | "1 pz" | N/A |

## Open Questions

- Precisione display: 2 cifre decimali sufficienti? → **DECISO: sì, 2 cifre decimali**
- Arrotondamento: half-up o half-even? → **DECISO: half-up (standard commerciale)**
- Unità base per pezzi/confezioni: nessuna conversione → **DECISO: nessuna unità base, restano come sono**

## Code Map

- `app/src/main/java/it/togo/app/domain/model/StandardUnit.kt` — Estensione con fattori di conversione, `convert()`, `normalize()`, `format()`
- `app/src/test/java/it/togo/app/domain/model/StandardUnitTest.kt` — Test JVM per conversioni, validazione, formattazione

## Tasks & Acceptance

**Execution:**
- [ ] `StandardUnit.kt` — Aggiungere `baseUnit`, `factorToBase`, `convert()`, `normalize()`, `format()`, `isValidQuantity()`
- [ ] `StandardUnitTest.kt` — Test JVM: conversioni compatibili/incompatibili, validazione, formattazione, edge cases

**Acceptance Criteria:**
- Given (1.5, KILOGRAM, GRAM), when `convert()`, then 1500.0
- Given (1500, GRAM, KILOGRAM), when `convert()`, then 1.5
- Given (1, PIECE, GRAM), when `convert()`, then null
- Given (0, GRAM, KILOGRAM), when `convert()`, then null
- Given (NaN, GRAM, KILOGRAM), when `convert()`, then null
- Given (2, KILOGRAM), when `normalize()`, then (2000.0, GRAM)
- Given (1500, GRAM), when `format()`, then "1.5 kg"
- Given (500, GRAM), when `format()`, then "500 g"
- Given (1, PIECE), when `format()`, then "1 pz"
- Tutte le conversioni <100ms (test JVM)
- `gradlew help` passa

## Dev Approvals

- [ ] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **StandardUnit.kt**: Estensione con `baseUnit`, `factorToBase`, `convert()`, `normalize()`, `format()`, `isValidQuantity()`. Unità base: GRAM (peso), MILLILITER (volume). Unità non convertibili (PIECE, PACKAGE, BOTTLE, JAR, CAN, BOX, ROLL, POT) restano come sono.
- **StandardUnitTest.kt**: 25+ test JVM per conversioni compatibili/incompatibili, validazione, formattazione intelligente (1500g → "1.5 kg"), edge cases (zero, negativo, NaN, Infinity).
- **Design System**: 100% token `TogoTheme` (colori, tipografia, spacing, shape, componentTokens).
- **Build**: `gradlew help` OK.

## Spec Change Log

- 2026-10-12 — Creazione spec, stato `in-progress`.
- 2026-10-12 — Implementazione completata, build `gradlew help` OK, stato `review`.
- 2026-10-12 — Review completata (5 lenti, congiunta con Story 3.1), fix applicati, stato `done`.

## Review Triage Log

- (vuoto fino alla review della Story 3.2)