---
title: 'Story 3.3: Risoluzione Sinonimi verso Prodotto Canonico'
type: 'feature'
created: '2026-10-12'
status: 'in-progress'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-1-ricerca-full-text-catalogo.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-2-normalizzazione-quantita.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'utente digita o pronuncia un termine (es. "latte", "caffè", "mele"). Il sistema deve risolvere il termine verso un **prodotto canonico** univoco nel catalogo, usando la tabella `SYNONYM` come ponte tra espressioni libere e prodotti canonici. Quando il termine è ambiguo (match multipli), il sistema deve disambiguire.

**Approach:** Implementare un **resolver** nel dominio che:
1. Accetta un termine grezzo (es. "latte intero")
2. Lo normalizza (accent-fold, lowercase)
3. Cerca corrispondenze esatte nella tabella `SYNONYM`
4. Se match multipli, applica euristiche di ranking (frequenza d'uso, posizione match, lunghezza termine)
5. Ritorna un `ResolvedProduct` (prodotto canonico + categoria + unità default + confidence score)
6. Se ambiguo, ritorna lista candidati per disambiguazione UI

## Boundaries & Constraints

**Always:**
- Matching case-insensitive + accent-folded (NFKD)
- Score di confidenza: 1.0 = match esatto, <1.0 = match parziale
- Se match multipli con score uguale → disambiguazione obbligatoria
- Risolvere verso il prodotto canonico più "generale" (es. "Latte" vs "Latte intero" → "Latte")
- NON modificare il database `catalog.db` (solo lettura)

**Never:**
- NON creare nuovi prodotti automaticamente dal resolver
- NON usare fuzzy matching con soglia arbitraria (solo esatto + prefix + substring)
- NON risolvere termini vuoti o <2 caratteri
- NON ritornare prodotti con `is_user_defined = 1` come match primario (sono fallback)

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Match esatto univoco | "latte" | ResolvedProduct(Latte UHT, confidence=1.0) | N/A |
| Match esatto multiplo | "caffè" (matcha Caffè in Grani + Caffè Macinato) | Lista candidati per disambiguazione | N/A |
| Match parziale | "latte intero" (matcha "Latte intero" ma non "Latte") | ResolvedProduct(Latte intero, confidence=0.9) | N/A |
| Nessun match | "xyz123" | null (nessun candidato) | N/A |
| Termine vuoto | "" | null | Nessuna query DB |
| Termine troppo corto | "a" | null | Nessuna query DB |
| Termine solo spazi | "   " | null | Nessuna query DB |
| Termine con accenti | "caffè" vs "cafe" | Stesso risultato (NFKD) | Normalizzazione |
| Termine multi-parola | "latte parzialmente scremato" | Match su "Latte" + bonus lunghezza | Ranking |
| Match su nome prodotto | "Latte UHT" (nome canonico, non sinonimo) | ResolvedProduct(Latte UHT, confidence=1.0) | N/A |
| Match multiplo stesso score | "caffè" (2 match score=1.0) | Lista candidati (2 items) | N/A |

## Open Questions

- Fonte frequenza d'uso: esterna o implicita? → **DECISO: per ora no, ranking per lunghezza termine + posizione match**
- Disambiguazione UI: modal o inline? → **DECISO: lista candidati inline (Story 3.5+ per UI completa)**
- Score soglia minima: 0.5? → **DECISO: soglia 0.5, sotto è considerato "nessun match"**

## Code Map

- `app/src/main/java/it/togo/app/domain/resolver/ProductResolver.kt` — Logica di risoluzione: `resolve(term: String): ResolutionResult`
- `app/src/main/java/it/togo/app/domain/model/ResolvedProduct.kt` — Data class risultato: `CanonicalProduct`, `confidence: Double`, `matchedTerm: String`
- `app/src/main/java/it/togo/app/domain/model/ResolutionResult.kt` — Sealed: `Single(resolved)` | `Ambiguous(candidates)` | `None`
- `app/src/test/java/it/togo/app/domain/resolver/ProductResolverTest.kt` — Test JVM per il resolver

## Tasks & Acceptance

**Execution:**
- [ ] `ResolvedProduct.kt` — Data class con `product: CanonicalProduct`, `confidence: Double`, `matchedTerm: String`
- [ ] `ResolutionResult.kt` — Sealed interface: `Single(ResolvedProduct)`, `Ambiguous(List<ResolvedProduct>)`, `None`
- [ ] `ProductResolver.kt` — `resolve(term: String): ResolutionResult` con normalizzazione, matching esatto/prefisso/substring, ranking per lunghezza, soglia 0.5
- [ ] `ProductResolverTest.kt` — Test JVM: match esatto, multiplo, parziale, vuoto, corto, accenti, multi-parola

**Acceptance Criteria:**
- Given "latte", when `resolve()`, then `Single` con confidence=1.0
- Given "caffè" (2 match), when `resolve()`, then `Ambiguous` con 2 candidati
- Given "xyz123", when `resolve()`, then `None`
- Given "a", when `resolve()`, then `None` (troppo corto)
- Given "latte intero", when `resolve()`, then match parziale con confidence > 0.5
- Tutte le query <100ms (test JVM)
- `gradlew help` passa

## Dev Approvals

- [ ] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

## Spec Change Log

## Review Triage Log