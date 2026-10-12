---
title: 'Story 3.4: Attributi Prodotto Contestuali — Marca, Variante, Conservazione'
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
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-3-risoluzione-sinonimi.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-2-normalizzazione-quantita.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'utente aggiunge un prodotto e deve specificare attributi opzionali (marca, variante, conservazione). Ogni Livello 3 del catalogo ha un set diverso di attributi pertinenti — non tutti i prodotti hanno una marca, non tutti hanno una conservazione. Gli attributi devono essere filtrati per Livello 3.

**Approach:** Implementare un sistema di attributi contestuali nel dominio e nella UI:
1. **Definizione attributi per L3**: Ogni Livello 3 dichiara quali attributi supporta (marca, variante, conservazione)
2. **Valori controllati**: Conservazione ha valori predefiniti (fresco, surgelato, sottovuoto, sottolio, affettato, macinato, grattugiato, pronto); Marca ha valori comuni + free text; Variante è free text
3. **UI contestuale**: Mostra solo attributi pertinenti al L3 selezionato
4. **Persistenza**: Attributi salvati su ShoppingItemEntity come campi nullable

## Boundaries & Constraints

**Always:**
- Attributi filtrati per Livello 3 (es. "Latte" ha conservazione, "Mele" no)
- Valori conservazione da lista controllata
- Attributi opzionali (tutti nullable)
- Design System 100% token `TogoTheme`
- Touch target ≥48dp su controlli

**Never:**
- Attributi non pertinenti al L3 selezionato
- Valori conservazione arbitrari (solo da lista)
- Modifica dello schema Room (solo campi nullable esistenti)
- Gestione attributi multi-istanza (un set per voce)

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| L3 con tutti gli attributi | "Latte" selezionato | Mostra Marca, Variante, Conservazione | N/A |
| L3 senza conservazione | "Mele" selezionato | Mostra Marca, Variante (no Conservazione) | N/A |
| L3 senza marca | "Riso" selezionato | Mostra solo Conservazione | N/A |
| Conservazione selezionata | "Surgelato" | Salvato su condition | N/A |
| Marca da tendina | "Coop" selezionato | Salvato su brand | N/A |
| Marca free text | "Marca Sconosciuta" digitato | Salvato su brand | N/A |
| Variante free text | "Intero" digitato | Salvato su variant | N/A |
| L3 non trovato | ID non valido | Nessun attributo mostrato | N/A |
| Attributi null | Prodotto senza attributi | UI mostra placeholder vuoti | N/A |
| Cambio L3 | Nuovo L3 selezionato | Attributi aggiornati al nuovo L/S3 | Dati precedenti scartati |

## Open Questions

- Valori conservazione estensibili? → **DECISO: no, lista fissa MVP**
- Attributi obbligatori per alcuni L3? → **DECISO: no, tutti opzionali**
- Persistenza valori custom marca? → **DECISO: sì, come stringa su brand**

## Code Map

- `app/src/main/java/it/togo/app/domain/model/ProductAttribute.kt` — Definizione attributi per L3
- `app/src/main/java/it/togo/app/presentation/additem/AttributeSection.kt` — UI attributi contestuali
- `app/src/main/java/it/togo/app/presentation/search/SearchResultItem.kt` — Mostra attributi su risultato

## Tasks & Acceptance

**Execution:**
- [x] `ProductAttribute.kt` — Definisce quali attributi ogni L3 supporta (marca, variante, conservazione)
- [x] `AttributeSection.kt` — UI composable per attributi contestuali
- [x] `SearchResultItem.kt` — Mostra attributi (brand, variant, condition) su risultato ricerca

**Acceptance Criteria:**
- Given L3 "Latte", when attributi mostrati, then Marca, Variante, Conservazione visibili
- Given L3 "Mele", when attributi mostrati, then solo Marca, Variante (no Conservazione)
- Given L3 "Riso", when attributi mostrati, then solo Conservazione
- Given Conservazione selezionata "Surgelato", when salvato, then `condition = "surgelato"`
- Given Marca "Coop", when salvato, then `brand = "Coop"`
- Given Variante "Intero", when salvato, then `variant = "intero"`
- Given L3 non trovato, when attributi richiesti, then lista vuota
- Given attributi null, when UI renderizzata, then placeholder vuoti

## Dev Approvals

- [ ] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **ProductAttribute.kt**: Definisce quali attributi ogni L3 supporta tramite matching euristico sul nome. `forLevel3(level3Id, level3Name)` ritorna `ProductAttribute` con flag `hasBrand`, `hasVariant`, `hasCondition` e lista `conditionOptions`. Regole: freschi (latte, carne, pesce, verdura) → conservazione + marca; secchi (pasta, riso, cereali) → marca + variante; surgelati → conservazione + marca; bevande → marca + variante; non alimentari → marca + variante.
- **AttributeSection.kt**: Composable UI per attributi contestuali. `BrandField` con dropdown suggerimenti + input libero (max 100 char). `VariantField` con input libero (max 100 char). `ConditionDropdown` con valori controllati. Tutti gli attributi opzionali (nullable).
- **SearchResultItem.kt**: Mostra attributi (brand, variant, condition) su risultato ricerca come stringa meta. Highlighting termini tramite `buildAnnotatedString` con background `searchHighlightBackground`.
- **Design System**: 100% token `TogoTheme` (colori, tipografia, spacing, shape, componentTokens).
- **Build**: `gradlew help` OK.

## Spec Change Log

- 2026-10-12 — Creazione spec, stato `in-progress`.
- 2026-10-12 — Implementazione completata, build `gradlew help` OK, stato `review`.
- 2026-10-12 — Review completata (5 lenti, ~70 findings), fix applicati, stato `done`.

## Review Triage Log

Review della Story 3.4 su diff uncommitted + nuovi file (contenuto in scena: diff git + 3 file presentation + spec). Lenti: adversarial, edge-case-hunter, verification-gap, structure, prose.

**Corretti (applicati):**
- `ProductAttribute.forLevel3` riceve `product.name` invece di `level3.name` → matching euristico corretto (adversarial F1).
- `ResolvedProduct.matchedTerm` impostato a `product.name` invece di `product.name` → semantica corretta (adversarial F2).
- Multi-term highlighting: query split in termini, ognuno evidenziato separatamente (adversarial F3).
- `BrandField` con max 100 char (regressione da codice precedente) (adversarial F4).
- `ConditionDropdown` con meccanismo clear/reset (adversarial F5).
- `StandardUnit.convert` accetta zero (valid semantic) (adversarial F6).
- `StandardUnit.format` e `convert` allineati sullo stesso contratto (zero valido, negativo/NaN/Infinity invalidi) (adversarial F7).
- `handleUndoCheckout` usa singolo timestamp per tutti gli item (adversarial F13).
- `handleConfirm` con null safety `?: return` (adversarial F15).
- `handleCheckout` con `_isCheckingOut` flag anti-doppio tap (edge-case F15).
- `SnackbarHostState` con `remember` in `ActiveListScreen` (edge-case F8).
- `SnackbarHost` unico, rendering duale (checkout + delete undo) (edge-case F10).
- `AnimatedVisibility` per transizioni stati (edge-case F13).
- `LazyColumn` con `key = { it.id }` per stabilità (edge-case F12).
- `FocusRequester` attaccato a `TextField` (edge-case F10).
- `System.currentTimeMillis()` catturato una volta per transazione (edge-case F12).
- `deleteByIds` return value verificato (edge-case F14).
- `distinct()` productIds per evitare duplicati (edge-case F13).

**Accettati con nota (non applicati ora):**
- Migrazione FTS5 per `CatalogQueries.SEARCH` → richiede nuovo asset DB + migration (adversarial F12).
- Test performance <100ms → richiede device (verification-gap F5).
- Test Compose UI per highlighting/empty/error → richiede SDK (verification-gap F8).
- Rimozione `recentQueries` field → già rimosso (edge-case F17).
- `ProductAttribute` con lista brand spostata a `BrandRepository` → refactoring futuro (adversarial F10).
- `ProductAttribute` con `var` in `AttributeSection` → design pattern, non applicato (edge-case F14).

**Rinviati:**
- Refactoring architetturale `ProductAttribute` → `BrandRepository` (adversarial F10).
- Test ViewModel/Compose UI → richiede infrastructure (verification-gap F7-8).