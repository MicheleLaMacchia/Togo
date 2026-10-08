---
title: 'Story 1.4: Catalogo Preinstallato da Asset'
type: 'feature'
created: '2026-10-08'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-1-3-database-room-e-schema-entitá-core.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Il database Room esiste (Story 1.3) ma al primo avvio è vuoto: manca il catalogo precompilato (`catalog.db`) negli asset, quindi la ricerca prodotti non ha dati e l'utente non può cercare/aggiungere prodotti offline.

**Approach:** Creare `app/src/main/assets/catalog.db` — database SQLite precompilato (script generatore deterministico in `tools/generate_catalog.py`) con tassonomia L1-L3, ≥1.000 prodotti canonici alimentari e ≥400 non alimentari, sinonimi, tutti con `is_user_defined = 0`. Inizializzazione tramite `Room.databaseBuilder().createFromAsset("catalog.db")` in `TogoDatabase.build()`. Verifica COUNT via test JVM (sqlite-jdbc) + script.

## Boundaries & Constraints

**Always:**
- AD-3: `createFromAsset()`, nessun download di rete, cold load istantaneo.
- Schema di `catalog.db` identico alle entity Room (tabelle, colonne, indici, `user_version = 1`).
- ≥1.000 alimentari + ≥400 non alimentari, verificabile via query COUNT.
- `level3_id NOT NULL`, nome canonico non vuoto, `is_user_defined = 0` per tutto il seed.
- Ordine tassonomia: Ortofrutta → Panetteria → Carne → … → Banco Frigo → Dispensa → Non alimentare (per Story 2.1).
- Generatore deterministico (`random.seed` fisso), versionato in `tools/`.

**Never:**
- Nessun dato seed con `is_user_defined = 1`.
- Nessuna modifica allo schema Room delle entity core (solo correzione errori colonna catalogo).
- Nessun caricamento del catalogo a runtime in memoria (il DB asset è la sorgente).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Primo avvio offline | APK installata, no rete | `createFromAsset()` copia `catalog.db`, tabelle catalogo popolate | Crash esplicito se asset mancante o schema mismatch |
| COUNT prodotti | DB seed | ≥1000 alimentari, ≥400 non alimentari | Test fallisce se sotto soglia |
| Integrità seed | Ogni riga `CANONICAL_PRODUCT` | `level3_id` non nullo, `name` non vuoto, `is_user_defined = 0` | Test fallisce su violazione |
| Ricerca | Query testuale | Join su `level3_id`/`level2_id`/`level1_id` corretti, ordine corsie | Nessuna riga se query vuota |
| Rigenereazione | Esecuzione script | Stesso file (seed fisso), overwrite safe | Script abort se conteggi sotto soglia |

</frozen-after-approval>

## Open Questions

- Fonte dati catalogo: dataset esterno o generato? — **DECISO: generato deterministicamente** da `tools/generate_catalog.py` (nessuna dipendenza esterna, riproducibile, reversibile).
- Verifica COUNT: solo script o anche test JVM? — **DECISO: entrambi** — test JVM `CatalogAssetTest` con `org.xerial:sqlite-jdbc` su file asset.
- Bug latenti Story 1.3 (Index/DAO riferiscono `level1Id` ma colonna è `level1_id`; `getFullTaxonomy` non mappabile da Room)? — **DECISO: correggere ora** — obbligatorio perché lo schema asset deve matchare le entity.

## Code Map

- `tools/generate_catalog.py` -- Nuovo: generatore deterministico di `catalog.db`.
- `app/src/main/assets/catalog.db` -- Nuovo: artifact generato (asset APK).
- `app/src/main/java/it/togo/app/data/database/TogoDatabase.kt` -- Aggiungi `.createFromAsset("catalog.db")`.
- `app/src/main/java/it/togo/app/data/database/entity/CatalogEntities.kt` -- Fix nomi indici (`level1_id`, `level2_id`, `level3_id`, `is_user_defined`, `product_id`).
- `app/src/main/java/it/togo/app/data/database/dao/CatalogDao.kt` -- Fix colonne join + rimozione `getFullTaxonomy` non mappabile (Room non supporta nested entity da `SELECT *` multipli).
- `app/src/main/java/it/togo/app/data/repository/RoomCatalogRepository.kt` -- `getTaxonomy()` combinato da `getLevel1/2/3`.
- `app/build.gradle.kts` + `gradle/libs.versions.toml` -- `testImplementation(sqlite-jdbc)`.
- `app/src/test/java/it/togo/app/data/database/CatalogAssetTest.kt` -- Nuovo: test COUNT/integrità su asset.

## Tasks & Acceptance

**Execution:**
- [x] `tools/generate_catalog.py` -- Script Python: crea schema Room-identico (8 tabelle + indici), tassonomia 8 L1 (ordine corsie, Non alimentare ultima), ~1.050 alimentari + ~420 non alimentari, sinonimi lowercase, `random.seed(42)`, abort se COUNT sotto soglia -- Generatore deterministico e self-verifying.
- [x] `app/src/main/assets/catalog.db` -- Eseguito script, file generato verificato via COUNT -- Asset pronto (396 KB, 1470 prodotti, 1736 sinonimi).
- [x] `app/src/main/java/it/togo/app/data/database/TogoDatabase.kt` -- `Room.databaseBuilder(...).createFromAsset("catalog.db")` -- Primo avvio popola il DB.
- [x] `app/src/main/java/it/togo/app/data/database/entity/CatalogEntities.kt` -- Fix `Index(value=...)` su colonne reali (`level1_id`, `level2_id`, `level3_id`, `is_user_defined`, `product_id`) -- Indici validi per KSP.
- [x] `app/src/main/java/it/togo/app/data/database/dao/CatalogDao.kt` -- Fix join su colonne snake_case; sostituire `getFullTaxonomy` (non mappabile) con `getLevel1/2/3` flat + rimuovere `TaxonomyJoinResult` -- Query compilabili da Room.
- [x] `app/src/main/java/it/togo/app/data/repository/RoomCatalogRepository.kt` -- `getTaxonomy()` = `combine(getLevel1, getLevel2, getLevel3)` → lista piatta ordinata -- Mapping corretto Entity→Domain.
- [x] `gradle/libs.versions.toml` + `app/build.gradle.kts` -- `testImplementation(sqlite-jdbc)` -- Test JVM possono aprire l'asset.
- [x] `app/src/test/java/it/togo/app/data/database/CatalogAssetTest.kt` -- Test: COUNT ≥1000 alimentari, ≥400 non alimentari, `level3_id NOT NULL`, `name != ''`, `is_user_defined = 0`, tabelle presenti, `user_version = 1` -- AC verificati automaticamente.

**Acceptance Criteria:**
- Given asset `catalog.db` presente, when `TogoDatabase` aperto al primo avvio, then `createFromAsset()` popola le 5 tabelle catalogo senza errori.
- Given DB seed, when `SELECT COUNT(*)` per categoria, then ≥1.000 alimentari e ≥400 non alimentari.
- Given ogni `CANONICAL_PRODUCT`, then `level3_id` non nullo, `name` non vuoto, `is_user_defined = 0`.
- Given `CatalogAssetTest` eseguito, then tutti i check COUNT/integrità passano.
- Cold load: nessun caricamento a runtime — il DB asset è pronto all'apertura.

## Dev Approvals

- [ ] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **Asset generato**: `tools/generate_catalog.py` (Python stdlib, modulo `sqlite3`), deterministico (`random.seed(42)`), rigenerazione verificata byte-identica (SHA-256). Produce `app/src/main/assets/catalog.db`: 384 KB, 1470 prodotti (1050 alimentari + 420 non alimentari), 37 L2, 98 L3, 1781 sinonimi.
- **Schema Room-identico**: lo script crea le 8 tabelle (5 catalogo + 3 core vuote) con colonne, tipi, NOT NULL, PK e indici identici alle entity; `PRAGMA user_version = 1`. Una verifica strutturale offline (`verify_schema`) confronta colonne/indici con lo schema atteso Room (in assenza di SDK non è possibile la validazione `onValidateSchema` di Room a runtime).
- **`createFromAsset`**: `TogoDatabase.build()` → `.createFromAsset("catalog.db")` senza `Callback` (rimosso il placeholder della Story 1.3). Al primo avvio Room copia l'asset e valida la struttura; nessun download, cold load istantaneo (AD-3). **Caveat pre-release**: l'asset viene copiato solo alla creazione del DB; un `togo.db` già esistente da build di sviluppo (Story 1.3) non lo riceve → negli ambienti dev serve disinstallare o cancellare i dati dell'app (documentato anche come commento in `TogoDatabase.kt`).
- **Correzioni latent bugs Story 1.3**: gli `Index` e le query `CatalogDao` referenziavano `level1Id`/`level3Id`/`productId`/`isUserDefined` mentre le colonne reali (con `@ColumnInfo`) sono `level1_id`/`level3_id`/`product_id`/`is_user_defined` → errori KSP. Correzione di `CatalogEntities.kt` e `CatalogDao.kt`. Inoltre `getFullTaxonomy` (❌ non mappabile da Room: `SELECT t1.*, t2.*, t3.*` con `id` collidenti) è stata sostituita da `getLevel1/getLevel2/getLevel3` flat + `combine` in `RoomCatalogRepository.getTaxonomy()`.
- **Stack build riconciliato**: la sessione precedente aveva declassato Kotlin a `2.0.0` con KSP interno irrisolvibile (`2.0.0-1.0.14` inesistente). KSP 2.3+ usa versionamento standalone: ripristinato **Kotlin 2.4.20** + **KSP 2.3.12**; KSP 2.3.x richiede `AndroidComponentsExtension.addKspConfigurations` → **AGP 8.8.0 → 8.12.3** e **Gradle 8.11.1 → 8.13**. Rimosso `composeOptions.kotlinCompilerExtensionVersion` (obsoleto con il plugin Compose in Kotlin 2.4). `./gradlew help` passa; `kspDebugKotlin` è bloccato solo dalla mancanza dell'Android SDK (scelta utente nota). Stack aggiornato in `ARCHITECTURE-SPINE.md`.
- **Correzioni dalla review (round 1)**: 
  - *Bug colonne scambiate in `SYNONYM`* (trovato dal nuovo check di integrità referenziale): l'INSERT scriveva `(term, product_id)` al posto di `(product_id, term)` — la colonna `product_id` conteneva stringhe. Corretto e chiuso da verifica `orphan=0, no-synonym=0`.
  - *Nomi duplicati*: 4 collisioni cross-categoria (Fiordilatte, Panna da Cucina, Tortina, Tortina Bio) rinominate; nuova AC "nessun nome duplicato globale" nel generatore + nel test.
  - *Ricerca accent-blind*: aggiunti sinonimi accent-fold (es. `caffe in grani` → "Caffè in Grani") e normalizzazione della query (NFKD, minuscole) + escaping LIKE (`\`, `%`, `_`) + nessun risultato per query vuota. SQL unificato in `CatalogQueries.SEARCH` (usato dal DAO e dal test, taglia il drift).
  - *Robustezza generatore*: pre-check capacità effettiva + guardia di convergenza in `expand_names`; `verify_schema` ora verifica le colonne anche degli indici `unique` (`origin != 'pk'`); DDL `SYNONYM.id` con `NOT NULL` (forma Room).
  - *Test JVM*: URL JDBC robusto (`invariantSeparatorsPath`), errore descrittivo, ordine L1 completo (8 voci), integrità sinonimi, probe accent-fold. Le stesse query sono state verificate offline in Python (replica esatta) perché senza SDK il test JVM non può girare su questa macchina.

## Spec Change Log

- 2026-10-08 — Creazione spec, stato `in-progress`.
- 2026-10-08 — Decisione Open Questions: dati generati deterministicamente; doppia verifica (script + test JVM); fix bump AGP/Gradle per KSP 2.3.x (deviazione da pin architettura 8.8.0/8.11.1, registrata in ARCHITECTURE-SPINE).
- 2026-10-08 — Review round 1 completata (5 lenti): fix applicati (vedi Implementation Notes "Correzioni dalla review") e tracciamento nel Review Triage Log.

## Review Triage Log

Review della Story 1.4 su diff uncommitted + nuovi file (contenuto in scena: diff git + generatore + test + spec). Lenti: adversarial, edge-case-hunter, verification-gap, structure, prose.

**Corretti (applicati):**
- Bug `SYNONYM` colonne scambiate (adversarial/verification-gap) → check integrità ref. introdotto.
- Nomi prodotti duplicati (adversarial) → rinomine + AC unicità globale.
- Ricerca accent-blind / query vuota / jolly LIKE (adversarial, edge-case-hunter) → sinonimi accent-fold + `CatalogQueries` (normalize/escape/guard).
- Loop infinito / `ZeroDivisionError` potenziali in `expand_names` (adversarial, edge-case-hunter) → pre-check capacità + guardia di convergenza; L3 sempre presenti per costruzione (documentato).
- Indici `unique` non verificati da `verify_schema` (edge-case-hunter) → verifica `index_info` per `origin != 'pk'`.
- `SYNONYM.id` senza `NOT NULL` (adversarial) → DDL allineata a Room.
- Test JVM: JDBC su Windows, errore asset mancante, ordine L1 parziale (adversarial, verification-gap) → fix.
- Struttura/prose spec (structure, prose) → applicate le uniformazioni nelle sezioni non-frozen; contenuto `<frozen-after-approval>` non toccato.

**Accettati con nota (non applicati ora):**
- Installa esistenti con `togo.db` v1 vuoto che non ricevono l'asset (adversarial F1) → in pre-release: doc caveat fresh-install in `TogoDatabase` + spec; un bump `version=2` con `fallbackToDestructiveMigration` azzererebbe anche i dati utente e comunque non ricopierebbe l'asset (createFromAsset solo a DB non esistente): rimandato a quando esisteranno migrate reali.
- `getLevel3ByLevel2` senza filtro `is_user_defined` vs `getLevel3` con filtro (adversarial F10) → nessun nodo user-defined nel seed 1.4; decisione di visibilità rinviata a Story 2.x (commentato).
- `exportSchema=true` + diff dell'asset contro lo schema JSON (adversarial F2, verification-gap) → richiede generazione Room (KSP): da attivare quando il build gira con SDK.
- Test di apertura asset-backed Room (verification-gap F1) e test `getTaxonomy` del repository (F4) → richiedono Room/strumentazione: non eseguibili senza SDK; proposto come strumentazione in Story 2.x.

**Rinviati:**
- Struttura spec: spostare AC sopra Execution (structure F1) e splittare Open Questions (F5) → contenuto `<frozen>`: richiede ri-negoziazione umana.
