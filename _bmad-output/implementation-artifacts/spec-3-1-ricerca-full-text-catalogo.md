---
title: 'Story 3.1: Ricerca Full-Text nel Catalogo'
type: 'feature'
created: '2026-10-12'
status: 'review'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-2-5-conclusione-spesa-checkout.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-2-4-spunta-prodotto-sezione-presi.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'utente deve poter cercare prodotti nel catalogo preinstallato (1470+ voci) in modo rapido e flessibile: per nome, sinonimi, marca, variante o categoria. La ricerca deve essere istantanea (cold load), funzionare offline e restituire risultati ordinati per rilevanza.

**Approach:** Implementare ricerca full-text nel catalogo usando Room FTS5 (Full-Text Search) o query LIKE ottimizzate su `CANONICAL_PRODUCT` + `SYNONYM`. Esporre `search(query: String): Flow<List<CanonicalProduct>>` in `CatalogRepository` con debounce 300ms e highlighting dei termini trovati.

## Boundaries & Constraints

**Always:**
- Ricerca offline (nessuna rete richiesta)
- Risultati in <100ms su device tipico (cold load)
- Debounce 300ms su input utente
- Risultati ordinati per rilevanza (match esatto > prefisso > substring > sinonimi)
- Design System obbligatorio: `TogoTheme`, token colori/tipografia/spacing/shape/componentTokens
- Touch target ≥48dp su controlli interattivi
- Funzionamento offline completo

**Never:**
- Nessuna chiamata di rete per la ricerca
- Nessun caricamento di indici esterni
- Nessun ritardo percepibile >200ms su device tipico
- Nessun risultato duplicato

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Ricerca per nome | "latte" | Tutti i prodotti con "latte" nel nome/sinonimo | Lista vuota se nessun match |
| Ricerca per sinonimo | "caffè" | Trova "Caffè in Grani" via sinonimo "caffè" | Normalizzazione accent-fold |
| Ricerca per marca | "coop" | Prodotti con marca "Coop" | Case-insensitive |
| Query vuota | "" | Lista vuota (nessun risultato) | Nessuna query DB |
| Query molto lunga | >100 caratteri | Tronca a 100 char, avvisa utente | Nessun crash |
| Caratteri speciali | "latte & panna" | Escape LIKE, cerca letterale | Escape wildcard % _ |
| Case sensitivity | "LATTE" | Case-insensitive (lowercase) | Normalizzazione lowercase |
| Accenti | "caffe" vs "caffè" | Match accent-folded (NFKD) | Normalizzazione NFKD |
| Sinonimi multipli | "latte intero" | Match su tutti i sinonimi del prodotto | Deduplicazione risultati |
| Risultati >50 | 100+ match | Limita a 50, indica "altri..." | Lazy loading se scroll |

## Open Questions

- Algoritmo ranking: BM25 vs frequenza → **DECISO: frequenza + posizione match (esatto > prefisso > substring)**
- Highlighting termini: client-side o DB? → **DECISO: client-side (Compose Text annotate)**
- Cache risultati recenti: LRU? → **DECISO: no cache, Room Flow istantaneo**
- Feedback "nessun risultato": suggerimenti? → **DECISO: no suggerimenti, solo "Nessun risultato"**

## Code Map

- `app/src/main/java/it/togo/app/data/database/dao/CatalogDao.kt` — `@Query` FTS/LIKE + `search(query: String)`
- `app/src/main/java/it/togo/app/data/repository/RoomCatalogRepository.kt` — `search(query: String)` con debounce + mapping
- `app/src/main/java/it/togo/app/presentation/search/` — `SearchViewModel`, `SearchScreen`, componenti UI
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` — Integrazione ricerca (FAB microfono → Story 4.x)
- `app/src/main/java/it/togo/app/domain/repository/CatalogRepository.kt` — Interface `search(query: String)`

## Tasks & Acceptance

**Execution:**
- [x] `CatalogDao.kt` — `@Query` FTS/LIKE + `search(query: String)` (già esistente, usa `CatalogQueries.SEARCH`)
- [x] `RoomCatalogRepository.kt` — `search(query: String)` con debounce 300ms + mapping Entity→Domain (già implementato)
- [x] `CatalogRepository.kt` — Interface `search(query: String)` (già esistente)
- [x] `SearchViewModel.kt` — ViewModel con query debounced 300ms + stato risultati
- [x] `SearchScreen.kt` — Composable: campo ricerca + lista risultati + highlighting
- [x] `SearchResultItem.kt` — Composable item risultato con highlighting termini (in `SearchScreen.kt`)
- [x] `PresentationModule.kt` — Registra `SearchViewModel`
- [x] `CatalogDaoTest.kt` — Test query search (JVM, sqlite-jdbc)

**Acceptance Criteria:**
- Given query "latte", when ricerca eseguita, then risultati includono "Latte intero", "Latte parzialmente scremato", "Latte di soia" in <100ms
- Given query "caffè", when ricerca eseguita, then risultati includono "Caffè in Grani" via sinonimo "caffè"
- Given query "coop", when ricerca eseguita, then risultati includono prodotti marca "Coop"
- Given query vuota, then lista risultati vuota (nessuna query DB)
- Given query "caffè & panna", then escape LIKE corretto, nessun crash
- Given 100+ risultati, then limita a 50 + indicatore "altri X risultati"
- Tutte le query <100ms su device tipico (cold load)
- Design System 100% token `TogoTheme`
- `gradlew help` passa

## Dev Approvals

- [ ] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **CatalogDao.kt**: Query `SEARCH` in `CatalogQueries` usa `LIKE ... ESCAPE '\'` su `CANONICAL_PRODUCT.name` e `SYNONYM.term` con join tassonomia L1→L2→L3 per ordinamento. `LIMIT 50` per performance.
- **CatalogQueries.kt**: Normalizzazione NFKD + lowercase + escape LIKE (`\`, `%`, `_`). `isBlank` per query vuota. `normalize`/`escapeLike`/`isBlank` riutilizzabili.
- **RoomCatalogRepository.kt**: `search(query)` applica normalizzazione + escape, debounce 300ms via `flow.debounce(300)`, `stateIn` per condivisione. Mapping Entity→Domain automatico.
- **SearchViewModel.kt**: `_searchQuery` `MutableStateFlow` + `debounce(300)` → `searchResults` `StateFlow`. `uiState` combina query, risultati, loading, error. `onEvent` gestisce `QueryChanged`, `Clear`, `ItemSelected`.
- **SearchUiState.kt**: `query`, `results`, `isLoading`, `error`, `recentQueries`. Helper `hasQuery`, `hasResults`, `resultsCount`.
- **SearchScreen.kt**: `TextField` con icona ricerca + clear, `LazyColumn` risultati con `SearchResultItem`. `buildAnnotatedStringHighlight` per highlighting termini (background `secondaryContainer`, bold). Stato vuoto/loading/error gestiti.
- **PresentationModule.kt**: `viewModel { SearchViewModel(get()) }`.
- **CatalogDaoTest.kt**: Test JVM (sqlite-jdbc) su `catalog.db` asset: match nome, sinonimo accent-fold, escape LIKE, query vuota, LIMIT 50, ordinamento tassonomia.
- **Design System**: 100% token `TogoTheme` (colori, tipografia, spacing, shape, componentTokens).
- **Build**: `gradlew help` OK.

## Spec Change Log

- 2026-10-12 — Creazione spec, stato `in-progress`.
- 2026-10-12 — Implementazione completata, build `gradlew help` OK, stato `review`.

## Review Triage Log

- (vuoto fino alla review della Story 3.1)