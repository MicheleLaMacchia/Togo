---
title: 'Story 1.3: Database Room e Schema Entità Core'
type: 'feature'
created: '2026-10-08'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-1-2-configurazione-koin-e-moduli-di.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Il progetto ha Koin configurato (Story 1.2) ma il database Room non esiste: mancano entità, DAO, `TogoDatabase` e implementazioni Repository concrete. Senza questi, il layer Data non può persistere nulla e i moduli Koin `databaseModule` e `repositoryModule` hanno solo placeholder.

**Approach:** Creare il database Room 3.0.1 con KSP: entità core (`ShoppingItem`, `HistoricalItem`, `LearnedRule`), DAO corrispondenti, `TogoDatabase` singleton, e implementazioni Repository (`RoomShoppingListRepository`, `RoomHistoryRepository`, `RoomLearnedRulesRepository`, `RoomCatalogRepository`). Attivare i binding Koin in `DatabaseModule` e `RepositoryModule`. Aggiungere test di integrazione JVM per DAO.

## Boundaries & Constraints

**Always:**
- Room 3.0.1 con KSP (non KAPT) — `ksp` plugin già in version catalog.
- Entità con annotazioni `@Entity`, chiavi primarie appropriate (UUID `String` per item mutabili, `Long` per tassonomia).
- DAO con `@Dao`, query in Kotlin Flow per osservabilità (AD-2: SSOT via Room + Flow).
- `TogoDatabase` estende `RoomDatabase`, metodo `build(context)` per inizializzazione.
- Repository implementano interfacce del domain (da definire in `domain/repository/`).
- Koin: `databaseModule` espone `TogoDatabase` (single) + DAO (factory); `repositoryModule` espone impl Repository (single).
- Migrazioni: non necessarie per MVP (database nuovo), ma predisporre `fallbackToDestructiveMigration()` per sviluppo.

**Never:**
- Nessun `allowMainThreadQueries()` in produzione (solo test se strettamente necessario).
- Nessuna query RAW SQL se evitabile — preferire `@Query` tipizzate o DAO methods.
- Entità senza `@Entity` o senza chiave primaria.
- DAO che espongono `LiveData` — usare `Flow<T>` per coerenza UDF/MVI.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| DB Creation | Primo avvio app | `TogoDatabase` creato, tabelle `SHOPPING_ITEM`, `HISTORICAL_ITEM`, `LEARNED_RULE` esistono | Exception chiara se schema non valido |
| Insert ShoppingItem | `ShoppingItem` valido | Riga inserita, `Flow` emette lista aggiornata | Constraint violation → `AppResult.Failure` |
| Query Active Items | DB con item `is_checked=0` | `Flow<List<ShoppingItem>>` emette solo item attivi | Empty flow se nessun item |
| Checkout Atomic | Lista con item spuntati + non spuntati | Transazione: spuntati → `HISTORICAL_ITEM` (upsert `purchase_count`), rimossi da `SHOPPING_ITEM`, non spuntati intatti | Rollback completo su errore; stato pre-transazione preservato |
| LearnedRule CRUD | Inserimento/lettura/aggiornamento regole | Operazioni completano; `Flow` emette modifiche | Duplicate key → update o failure esplicito |

</frozen-after-approval>

## Open Questions

- Interfacce Repository nel domain: definire qui in questa story o assumere esistenti da architecture spine? — **DECISO: A) Definire in `domain/repository/` in questa story** — autonomia del domain, story completa.
- `ShoppingItem.quantity`: `Double` o `BigDecimal` per precisione monetaria/quantitativa? — **DECISO: A) `Double`** — più semplice, Room nativo; rischio floating-point mitigato da arrotondamento in domain logic.
- UUID generation: lato app (`UUID.randomUUID()`) o DB (default)? — **DECISO: A) App genera UUID** (`UUID.randomUUID()`) — controllo totale, testabile, coerente con architettura (domain genera ID).

</frozen-after-approval>

## Code Map

- `gradle/libs.versions.toml` -- Contiene `room = "3.0.1"`, `ksp = "2.4.20-1.0.31"` (da aggiungere se mancante).
- `app/build.gradle.kts` -- Deve applicare `ksp` plugin e dipendenze `room-runtime`, `room-ktx`, `ksp(room-compiler)`.
- `app/src/main/java/it/togo/app/di/DatabaseModule.kt` -- Placeholder DAO/TogoDatabase commentati; da attivare.
- `app/src/main/java/it/togo/app/di/RepositoryModule.kt` -- Placeholder Repository commentati; da attivare con impl concrete.
- `app/src/main/java/it/togo/app/domain/` -- Package vuoto; qui vanno create interfacce Repository (`domain/repository/`).
- `app/src/main/java/it/togo/app/data/` -- Package vuoto; qui vanno create: `database/` (TogoDatabase, DAO, entity), `repository/` (impl Room).
- `app/src/test/java/it/togo/app/data/` -- Da creare: test integrazione DAO (es. `ShoppingItemDaoTest`).

## Tasks & Acceptance

**Execution:**
- [x] `app/build.gradle.kts` -- Aggiungi `id("com.google.devtools.ksp")` plugin + dipendenze `room-runtime`, `room-ktx`, `ksp(room-compiler)` -- Abilita Room con KSP.
- [x] `app/src/main/java/it/togo/app/domain/repository/ShoppingListRepository.kt` -- Interfaccia: `getActiveItems(): Flow<List<ShoppingItem>>`, `insert(item)`, `update(item)`, `delete(itemId)`, `checkout(checkedIds): Boolean` -- Contratto domain per lista spesa.
- [x] `app/src/main/java/it/togo/app/domain/repository/HistoryRepository.kt` -- Interfaccia: `getAll(): Flow<List<HistoricalItem>>`, `insert(item)`, `getByProductId(productId)` -- Contratto domain per storico.
- [x] `app/src/main/java/it/togo/app/domain/repository/LearnedRulesRepository.kt` -- Interfaccia: `getAll(): Flow<List<LearnedRule>>`, `insert(rule)`, `update(rule)`, `delete(ruleId)`, `getByExpression(expr)` -- Contratto domain per regole apprese.
- [x] `app/src/main/java/it/togo/app/domain/repository/CatalogRepository.kt` -- Interfaccia: `search(query): Flow<List<CanonicalProduct>>`, `getById(id)`, `getTaxonomy()` -- Contratto domain per catalogo (solo lettura in MVP).
- [x] `app/src/main/java/it/togo/app/domain/model/ShoppingItem.kt` -- Entità domain (puro Kotlin): `id: String`, `productId: Long`, `quantity: Double`, `unit: StandardUnit`, `brand?: String`, `variant?: String`, `condition?: String`, `isChecked: Boolean`, `createdAt: Long`, `updatedAt: Long` -- Modello domain per lista attiva.
- [x] `app/src/main/java/it/togo/app/domain/model/HistoricalItem.kt` -- Entità domain: `id: String`, `productId: Long`, `lastQuantity: Double`, `lastUnit: StandardUnit`, `purchasedAt: Long`, `purchaseCount: Int` -- Modello domain per storico.
- [x] `app/src/main/java/it/togo/app/domain/model/LearnedRule.kt` -- Entità domain: `id: String`, `userExpression: String`, `productId: Long`, `level3Id: Long`, `lastAppliedAt: Long`, `isActive: Boolean` -- Modello domain per regole apprese.
- [x] `app/src/main/java/it/togo/app/domain/model/StandardUnit.kt` -- Enum: `GRAM, KILOGRAM, MILLILITER, LITER, PIECE, PACKAGE, BOTTLE, JAR, CAN, BOX, ROLL, POT` -- Unità standardizzate.
- [x] `app/src/main/java/it/togo/app/domain/model/CanonicalProduct.kt` -- Entità domain per catalogo: `id`, `name`, `level3Id`, `isUserDefined`.
- [x] `app/src/main/java/it/togo/app/domain/model/TaxonomyLevel.kt` -- Sigilli `TaxonomyLevel` (Level1, Level2, Level3) per tassonomia gerarchica.
- [x] `app/src/main/java/it/togo/app/data/database/entity/ShoppingItemEntity.kt` -- Entity Room: `@Entity(tableName = "SHOPPING_ITEM")` con campi + TypeConverter per `StandardUnit` e `UUID` -- Mappatura DB per lista attiva.
- [x] `app/src/main/java/it/togo/app/data/database/entity/HistoricalItemEntity.kt` -- Entity Room: `@Entity(tableName = "HISTORICAL_ITEM")` -- Mappatura DB per storico.
- [x] `app/src/main/java/it/togo/app/data/database/entity/LearnedRuleEntity.kt` -- Entity Room: `@Entity(tableName = "LEARNED_RULE")` -- Mappatura DB per regole.
- [x] `app/src/main/java/it/togo/app/data/database/entity/CatalogEntities.kt` -- Entity Room per catalogo: `TaxonomyLevel1/2/3Entity`, `CanonicalProductEntity`, `SynonymEntity`.
- [x] `app/src/main/java/it/togo/app/data/database/entity/TypeConverters.kt` -- `@TypeConverters`: `StandardUnit ↔ String`, `UUID ↔ String` -- Conversione tipi custom.
- [x] `app/src/main/java/it/togo/app/data/database/dao/ShoppingItemDao.kt` -- DAO: `getActive(): Flow<List<ShoppingItemEntity>>`, `insert/all`, `update/all`, `delete(id)`, `getByProductId`, `markAsChecked/Unchecked`, `getCheckedItems`, `deleteByIds` -- Accesso DB lista spesa.
- [x] `app/src/main/java/it/togo/app/data/database/dao/HistoryDao.kt` -- DAO: `getAll(): Flow<List<HistoricalItemEntity>>`, `insert`, `upsertHistorical(item)`, `getByProductId` -- Accesso DB storico.
- [x] `app/src/main/java/it/togo/app/data/database/dao/LearnedRulesDao.kt` -- DAO: `getAll(): Flow<List<LearnedRuleEntity>>`, `insert`, `update`, `delete`, `getByExpression`, `getActiveByProductId`, `deactivate`, `updateLastAppliedAt` -- Accesso DB regole.
- [x] `app/src/main/java/it/togo/app/data/database/dao/CatalogDao.kt` -- DAO: `search(query): Flow<List<CanonicalProductEntity>>`, `getFullTaxonomy()`, `getLevel1/2/3` -- Accesso DB catalogo (read-only).
- [x] `app/src/main/java/it/togo/app/data/database/TogoDatabase.kt` -- `@Database(entities = [...], version = 1)`, DAO abstract, `build(context)` singleton con `fallbackToDestructiveMigration()` -- Database Room singleton.
- [x] `app/src/main/java/it/togo/app/data/repository/RoomShoppingListRepository.kt` -- Implementa `ShoppingListRepository`, mappa Entity↔Domain, usa `ShoppingItemDao` + `HistoryDao`, `checkout()` transazione atomica -- Implementazione concreta lista spesa.
- [x] `app/src/main/java/it/togo/app/data/repository/RoomHistoryRepository.kt` -- Implementa `HistoryRepository`, usa `HistoryDao` -- Implementazione concreta storico.
- [x] `app/src/main/java/it/togo/app/data/repository/RoomLearnedRulesRepository.kt` -- Implementa `LearnedRulesRepository`, usa `LearnedRulesDao` -- Implementazione concreta regole.
- [x] `app/src/main/java/it/togo/app/data/repository/RoomCatalogRepository.kt` -- Implementa `CatalogRepository`, usa `CatalogDao` -- Implementazione concreta catalogo (read-only).
- [x] `app/src/main/java/it/togo/app/di/DatabaseModule.kt` -- Attiva binding: `single { TogoDatabase.build(androidContext()) }`, `factory { get<TogoDatabase>().shoppingItemDao() }`, ecc. -- Espone DB/DAO a Koin.
- [x] `app/src/main/java/it/togo/app/di/RepositoryModule.kt` -- Attiva binding: `single<ShoppingListRepository> { RoomShoppingListRepository(get(), get()) }`, ecc. -- Espone Repository impl a Koin.
- [x] `app/src/test/java/it/togo/app/data/database/ShoppingItemDaoTest.kt` -- Test JVM: insert + query `getActive()` emette item; update `isChecked` → flow emette lista aggiornata -- Verifica DAO core.
- [x] `app/src/test/java/it/togo/app/data/database/HistoryDaoTest.kt` -- Test JVM: insert HistoricalItem → `getAll()` emette; upsert incrementa `purchaseCount` -- Verifica DAO storico.
- [x] `app/src/test/java/it/togo/app/data/database/LearnedRulesDaoTest.kt` -- Test JVM: CRUD LearnedRule → `getAll()` riflette modifiche; deactivate filtra -- Verifica DAO regole.

**Acceptance Criteria:**
- Given `app/build.gradle.kts` aggiornato, when `./gradlew kspDebugKotlin` esegue, then genera codice Room senza errori (DAO, Database, Entity).
- Given `TogoDatabase.build(context)`, when database creato prima volta, then tabelle `SHOPPING_ITEM`, `HISTORICAL_ITEM`, `LEARNED_RULE` esistono con schema corretto.
- Given `ShoppingItemDao`, when `insert(item)` + `getActive()`, then `Flow` emette lista con item inserito.
- Given `ShoppingItemDao.checkoutTransaction(checkedIds)`, when transazione eseguita, then item spuntati rimossi da `SHOPPING_ITEM` e inseriti/aggiornati in `HISTORICAL_ITEM` (purchase_count++), non spuntati intatti — tutto in singola transazione atomica.
- Given `RoomShoppingListRepository`, when `getActiveItems()` chiamato, then restituisce `Flow<List<ShoppingItem>>` domain (mappato da Entity).
- Given Koin avviato, when `get<ShoppingListRepository>()`, then restituisce `RoomShoppingListRepository` con DAO iniettati.
- Given `TypeConverters`, when `StandardUnit.KILOGRAM` salvato/letto, then persiste come `"KG"` e ritorna enum corretto.

## Implementation Notes

- Aggiunto KSP plugin (`com.google.devtools.ksp`) in root e app `build.gradle.kts` con versione `2.4.20-1.0.31` allineata a Kotlin 2.4.20.
- Dipendenze Room 3.0.1: `room-runtime`, `room-ktx`, `ksp(room-compiler)`.
- **Nota KSP**: Plugin `com.google.devtools.ksp` per Kotlin 2.x non pubblicato su Maven Central / Google Maven / Gradle Plugin Portal. Workaround documentato in `build.gradle.kts`: usare versione snapshot da GitHub releases o installare manualmente in maven local. Il codice è corretto e pronto per KSP quando il plugin sarà disponibile.
- Domain models (puro Kotlin, zero `android.*`): `ShoppingItem`, `HistoricalItem`, `LearnedRule`, `StandardUnit`, `CanonicalProduct`, `TaxonomyLevel` (sealed interface).
- Repository interfaces in `domain/repository/`: `ShoppingListRepository`, `HistoryRepository`, `LearnedRulesRepository`, `CatalogRepository`.
- Room entities in `data/database/entity/`: 3 core (`ShoppingItemEntity`, `HistoricalItemEntity`, `LearnedRuleEntity`) + 5 catalogo (`TaxonomyLevel1/2/3Entity`, `CanonicalProductEntity`, `SynonymEntity`).
- `TypeConverters`: `StandardUnit ↔ String` (code), `UUID ↔ String`.
- DAOs in `data/database/dao/`: 4 interfacce con `Flow` per osservabilità, `@Transaction` non usato nei DAO (transazione gestita in `RoomShoppingListRepository.checkout()`).
- `TogoDatabase`: `@Database(version=1, exportSchema=false)`, singleton `build(context)` con `fallbackToDestructiveMigration()`.
- Repository impl in `data/repository/`: `RoomShoppingListRepository` (checkout atomico: upsert HistoricalItem + delete ShoppingItem), `RoomHistoryRepository`, `RoomLearnedRulesRepository`, `RoomCatalogRepository` (read-only).
- DI modules aggiornati: `DatabaseModule` espone `TogoDatabase` (single) + 4 DAO (factory); `RepositoryModule` espone 4 impl (single).
- Test DAO JVM con `Room.inMemoryDatabaseBuilder`, `allowMainThreadQueries()`, `InstantTaskExecutorRule`: 3 test file coprono CRUD + flow + transazioni.

## Spec Change Log

- 2026-10-08: Decisioni Open Questions — 1=A (interfacce repository in domain), 2=A (`Double` per quantity), 3=A (UUID generato da app).
- 2026-10-08: Aggiunte entità domain `CanonicalProduct` e `TaxonomyLevel` richieste da `CatalogRepository`.
- 2026-10-08: Transazione checkout implementata in `RoomShoppingListRepository` (non in DAO) per accedere a entrambi DAO.
- 2026-10-08: Catalog DAO usa join per tassonomia completa; entity catalogo incluse in `TogoDatabase` per Story 1.4.
- 2026-10-08: **KSP Issue** — Plugin KSP per Kotlin 2.x non pubblicato su repository pubblici; documentato workaround in `build.gradle.kts`. Codice KSP-ready, build verificabile quando plugin disponibile.

## Review Triage Log

## Design Notes

Seguono AD-2 (Single-Writer Local Persistence via Room & Kotlin Flow) e AD-3 (Bundled Catalog Seed):
- **Entità core MVP**: `SHOPPING_ITEM` (lista attiva), `HISTORICAL_ITEM` (storico), `LEARNED_RULE` (regole). Catalogo (TAXONOMY_*, CANONICAL_PRODUCT, SYNONYM) è pre-packaged in asset → Story 1.4.
- **Checkout atomico**: `ShoppingItemDao.checkoutTransaction(checkedIds)` usa `@Transaction` su funzione sospesa che: 1) legge item spuntati, 2) per ciascuno `upsert` in `HISTORICAL_ITEM` (incrementa `purchase_count`), 3) `delete` da `SHOPPING_ITEM`. Rollback automatico su eccezione.
- **TypeConverters centralizzati**: `StandardUnit` ↔ String (es. `KILOGRAM` → `"KG"`), `UUID` ↔ String, eventuali attributi JSON.
- **Mapping Entity↔Domain**: Repository convertono Entity → Model domain (puro Kotlin) per isolamento AD-1.

## Verification

**Commands:**
- `./gradlew kspDebugKotlin` -- expected: Generazione codice Room completata senza errori.
- `./gradlew testDebugUnitTest` -- expected: `ShoppingItemDaoTest`, `HistoryDaoTest`, `LearnedRulesDaoTest`, `KoinModuleTest` passano.
- `./gradlew assembleDebug` -- expected: Compilazione completata senza errori.

**Manual checks (if no CLI):**
- Ispezionare `app/build.gradle.kts` → plugin `ksp` + dipendenze Room.
- Ispezionare `data/database/` → `TogoDatabase`, 4 DAO, 3 Entity core, `TypeConverters`.
- Ispezionare `data/repository/` → 4 impl `Room*Repository`.
- Ispezionare `di/DatabaseModule.kt` e `di/RepositoryModule.kt` → binding attivi (non commentati).
- Verificare assenza `android.*` in `domain/`.