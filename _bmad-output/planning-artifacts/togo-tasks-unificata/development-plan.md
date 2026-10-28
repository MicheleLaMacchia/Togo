---
title: "Togo — Piano di sviluppo: lista personale unificata (Spesa + Da fare)"
status: draft-for-approval
date: 2026-10-28
baseline:
  - _bmad-output/planning-artifacts/togo-tasks-unificata/inputs/togo-specifichev0.14.md
  - _bmad-output/planning-artifacts/togo-tasks-unificata/inputs/togo_develop_planv0.6.md
  - _bmad-output/planning-artifacts/togo-tasks-unificata/inputs/togo-lista-unificata-tasks-v1.0.md
repo:
  databaseVersion: 5
  catalogAssetVersion: 1
  canonicalProducts: 748
  module: app
  di: Koin
  minSdk: 24
  targetSdk: 35
---

# Togo — Piano di sviluppo: lista personale unificata (Spesa + Da fare)

## 1. Sintesi esecutiva

TOGO passa da una lista della spesa a **una sola lista personale** che contiene due tipi di
elementi, `Spesa` e `Da fare`, con lo stesso entry point, la stessa visibilità e le stesse
azioni rapide. I due domini restano separati nei dati e nel codice: il catalogo prodotti e la
tassonomia merceologica non vengono contaminati dalle categorie Task, e nessuna Task viene
creata tramite `CANONICAL_PRODUCT`.

Il lavoro si articola in **quattro epiche (8–11)** e **24 storie**:

| Epica | Obiettivo | Storie |
|---|---|---|
| Epic 8 | Fondamenta dati: schema v6, migrazione non distruttiva, refactor Spesa, dominio Task, `Clock` | 8.1–8.7 |
| Epic 9 | Proiezione unificata, home unica, composer Spesa/Da fare | 9.1–9.7 |
| Epic 10 | Ciclo di vita, cronologia unificata, svuota attivi, purge | 10.1–10.5 |
| Epic 11 | Hardening, accessibilità, backup, performance, DoD | 11.1–11.5 |

La sequenza è deliberatamente **data-first**: prima si porta il database alla versione 6 con una
migrazione testata e si refactora la Spesa, poi si costruisce la vista unificata. In questo modo
la UI unificata nasce su un modello già corretto e non su un doppio strato di compatibilità.

## 2. Ambito e non-obiettivi

### 2.1 In ambito

- Home unica con righe `Spesa` (`TO_BUY`) e `Da fare` (`OPEN`), filtri di tipo e ordinamento
  deterministico.
- Composer unico con scelta esplicita `Spesa | Da fare`, form Task leggero e form Spesa
  esistente adattato al nuovo modello.
- Ciclo di vita completo: completa, riapri, elimina logicamente, ripristina, svuota attivi,
  purge degli eliminati.
- Cronologia unificata con filtri tipo/stato e azioni contestuali.
- Categorie Task T1/T2 separate, priorità, scadenza `LocalDate`, note.
- Refactor completo della Spesa a `LIST_ITEM` con stati, unità libera, nota e storico
  acquisti preservato.
- Migrazione Room `5 → 6` non distruttiva, `exportSchema`, test da v5 e dall'asset v1.
- Accessibilità, offline-first, backup/restore, documentazione e DoD.

### 2.2 Fuori ambito v1

- Liste separate, progetti, multiutenza, subtask, dipendenze, ricorrenze.
- Notifiche, sveglie, integrazione calendario, geofencing, allegati.
- Backend, sync, account, AI per i Task, classificazione automatica del tipo.
- Categorie Task personalizzate, T3, modali categorie obbligatorie.
- Ricerca full-text unificata nella home (vedi decisione aperta DA-1).
- Voce per i Task: il canale vocale resta shopping-only.
- Estrazione multi-modulo, Hilt, NavHost, Paging, DataStore, event bus, CQRS.
- Allineamento `minSdk 26` / `targetSdk 36`: decisione di release separata.

## 3. Baseline verificata del repository

Fatti controllati sul codice al 2026-10-28:

- `TogoDatabase` è alla **versione 5**, `exportSchema = false`, con
  `fallbackToDestructiveMigration()` attivo.
- Tabelle presenti: `SHOPPING_ITEM`, `HISTORICAL_ITEM`, `CANONICAL_PRODUCT`,
  `TAXONOMY_LEVEL_1/2/3`, `SYNONYM`, `LEARNED_RULE`, `USER_ATTRIBUTE`.
- `SHOPPING_ITEM` usa `isChecked: Boolean`, `unit: StandardUnit`, quantità `0.0` come
  sentinella “nessuna quantità”; `HISTORICAL_ITEM` è un aggregato per prodotto/attributi con
  `purchaseCount`.
- `RoomShoppingListRepository.checkout()` sposta le voci spuntate nello storico e le elimina
  dalla lista attiva; la UI ha la sezione “Presi” e il comando “Concludi spesa”.
- L'asset `app/src/main/assets/catalog.db` ha `user_version = 1` e contiene **748** prodotti
  canonici v2. Un'installazione nuova esegue `1→2→3→4→5`; il lavoro aggiunge `5→6`.
- DI: **Koin 3.5.6** (`koin-android`, `koin-compose`), moduli espliciti in `it.togo.app.di`.
- Stack: Kotlin 2.0.21, AGP 8.7.3, Compose BOM 2024.09.03, Room 2.5.2 (KSP), Coroutines 1.8.1,
  `minSdk 24`, `targetSdk 35`, `compileSdk 35`.
- Test: JVM/Robolectric + `sqlite-jdbc`; non esiste una suite `androidTest`. Script:
  `.\test-android.ps1` (Docker, `:app:testDebugUnitTest`), `.\build-android.ps1`,
  `.\gradlew.bat`.
- **Core library desugaring non è attivo**: con `minSdk 24`, l'uso di `java.time.LocalDate`
  richiede l'abilitazione del desugaring (o il crash su API 24–25). Va aggiunto in F1.
- `docs/implementation-status.md` e `docs/tasks-migration-dry-run.md` non esistono ancora.
- Git era pulito all'inizio dell'analisi; i nuovi artefatti di pianificazione sono non
  committati.

## 4. Decisioni di progetto

Le decisioni seguenti sono vincolanti per l'implementazione. Dove la specifica e il repository
divergono, la specifica resta la fonte di verità funzionale; la divergenza tecnica è risolta
qui e non ridefinita silenziosamente.

| ID | Decisione | Motivazione |
|---|---|---|
| D1 | **Refactor completo Spesa** a `LIST_ITEM` con stati `TO_BUY | PURCHASED | DELETED` | La specifica tratta la spunta come acquisto immediato e la cronologia come stati, non come tabella aggregata separata. |
| D2 | **Ritiro del flusso “Presi” + “Concludi spesa”** | Con `PURCHASED` immediato lo staging non ha più senso; i completati recenti lo sostituiscono. |
| D3 | **Unità libera** persistita come `TEXT`; `StandardUnit` resta helper di parsing/formattazione. I valori legacy sono i `name` dell'enum (`PIECE`, `KILOGRAM`, …) | La specifica vuole unità testuale; l'enum esistente serve ancora per conversioni e suggerimenti. Room 2.5.2 ha salvato finora l'enum per `name`. |
| D4 | **Quantità sempre valida** (`finite > 0`), migrazione `0.0 → 1.0` | Elimina la sentinella e allinea dominio, DB e UI. |
| D5 | **`HISTORICAL_ITEM` confluito in `LIST_ITEM`** come righe `PURCHASED` con `purchaseCount` | Un solo aggregato shopping, storico acquisti immutabile e statistiche preservate. |
| D6 | **ID**: `String`/UUID per la Spesa, `Long` autogenerato per i Task, chiavi UI `S:`/`T:` | Preserva gli ID esistenti ed evita collisioni nella proiezione. |
| D7 | **Koin**, singolo modulo `app`, `minSdk 24` / `targetSdk 35` | Realtà del repository; Hilt e SDK 26/36 sono fuori dal refactor. |
| D8 | **Categorie Task separate**: `TASK_CATEGORY` T1/T2, seed, nessun T3, nessuna categoria custom | La specifica vieta di usare L1/L2/L3 merceologici e `CANONICAL_PRODUCT`. |
| D9 | **`Clock` e `ZoneId` iniettati** per tutte le nuove transizioni | Test deterministici su scadenze, `Scaduto`, `completedAt`, `statusChangedAt`. |
| D10 | **Nessuna UNION** nella proiezione: `combine` di due Flow e mapping in memoria | Evita accoppiamento fragile tra entità eterogenee. |
| D11 | **Filtro di tipo** in `SavedStateHandle` del `UnifiedListViewModel`; **ultimo tipo composer** in un holder di sessione (`ComposerSessionState`) o nello `SavedStateHandle` del parent | Il filtro sopravvive a rotazione/process recreation; l'ultimo tipo resta coerente anche se la sheet ricrea il proprio ViewModel. Default `Tutti` / `Spesa` a freddo. |
| D12 | **Solo `OPEN`/`TO_BUY` eliminabili**; `COMPLETED`/`PURCHASED` immutabili | Coerente con “eliminazione di una riga aperta” e con il purge che non tocca mai completati/acquistati. |
| D13 | **Categoria Task**: se l'utente sceglie solo l'Area, `categoryId` punta alla T1; con la sottocategoria punta alla T2 | Un solo FK, due livelli, nessun T3, categoria opzionale. |
| D14 | **Nessun `fallbackToDestructiveMigration()`** dalla v6 in poi | La migrazione deve preservare i dati; un fallback distruttivo è inaccettabile. |
| D15 | **Core library desugaring attivo** per `java.time` su API 24–25 | La specifica richiede `LocalDate`; il repository resta a `minSdk 24`. Senza desugaring l'app crasha su API 24–25. |

### 4.1 Conflitti e come sono risolti

1. **Modello Spesa**: codice `isChecked` + storico aggregato vs specifica `ListItemStatus`.
   Risolto con D1/D5 e migrazione `5→6`.
2. **Flusso “Presi”**: il codice ha staging + checkout; la specifica vuole `PURCHASED`
   immediato. Risolto con D2.
3. **Unità**: enum vs testo libero. Risolto con D3.
4. **Quantità**: `0.0` sentinella vs `> 0`. Risolto con D4.
5. **DI**: piano v0.6/specifica citano Hilt; il repository usa Koin. Risolto con D7.
6. **SDK**: specifica `26/36`, repository `24/35`. Rinviato a release separata (D7).
7. **ID**: specifica `Long` per `ListItem`, repository UUID. Risolto con D6.
8. **Ricerca unificata**: la specifica dice “alla pari” ma il dettaglio non definisce una
   ricerca testuale unica. Ipotesi v1: filtri di tipo + ricerca catalogo esistente; la
   ricerca full-text unificata resta **aperta** (DA-1).
9. **Categorie Task personalizzate**: fuori scope v1; solo seed (D8).
10. **Voce per Task**: fuori scope; il vocale resta shopping-only e non classifica il tipo.

## 5. Architettura target

### 5.1 Principi

- Clean Architecture invariata: `presentation → domain ← data`; il dominio non importa
  `android.*` né Room.
- Due aggregati distinti (`Shopping` e `Task`) e una **vista** unificata, non una tabella
  universale.
- Un solo `StateFlow<UnifiedListUiState>` per la home; azioni dispatch per `ItemKind`.
- Transazioni cross-dominio solo dove richiesto (`clearActive`, `purgeDeleted`) tramite
  `RoomDatabase.withTransaction`.
- Koin per DI; nessun singleton globale.
- Offline-first: Tasks non ha rete, AI, account o permessi notifiche.

### 5.2 Flusso

```text
CANONICAL_PRODUCT ──> LIST_ITEM ──> ListItemRepository ─┐
                                                         ├──> UnifiedListRepository
TASK_CATEGORY ─────> TODO_TASK ──> TodoRepository ───────┘            │
                                                                       v
                                                          UnifiedListViewModel
                                                                       │
                                                          Compose: La mia lista
```

### 5.3 Package e componenti

**Domain**

- `domain/model/ListItem.kt`, `ListItemStatus.kt`, `ItemKind.kt`
- `domain/model/TodoTask.kt`, `TodoPriority.kt`, `TodoStatus.kt`, `TaskCategory.kt`
- `domain/model/UnifiedListEntry.kt`, `UnifiedListFilter.kt`
- `domain/repository/ListItemRepository.kt`, `TodoRepository.kt`, `UnifiedListRepository.kt`
- `domain/time/Clock.kt` (`nowMillis()`, `today(zoneId)`), `ZoneIdProvider.kt`
- `domain/unified/UnifiedListOrdering.kt`, `UnifiedListProjection.kt`
- `domain/task/TaskValidation.kt`, `TaskTransitions.kt`

**Data**

- `data/database/entity/ListItemEntity.kt`, `TodoTaskEntity.kt`, `TaskCategoryEntity.kt`
- `data/database/dao/ListItemDao.kt`, `TodoTaskDao.kt`, `TaskCategoryDao.kt`
- `data/database/ListItemMigration.kt` (`MIGRATION_5_6`)
- `data/repository/RoomListItemRepository.kt`, `RoomTodoRepository.kt`,
  `RoomUnifiedListRepository.kt`
- `data/database/TogoDatabase.kt`: versione 6, nuove entità/DAO, `exportSchema = true`,
  rimozione del fallback distruttivo.

**Presentation**

- `presentation/unified/UnifiedListViewModel.kt`, `UnifiedListScreen.kt`,
  `UnifiedListUiState.kt`, `components/`
- `presentation/additem/AddItemViewModel.kt`, `AddItemUiState.kt`,
  `AddItemBottomSheet.kt` estesi con `ItemKind` e form Task.
- `presentation/history/HistoryViewModel.kt`, `HistoryScreen.kt` rifattorizzati sulla
  cronologia unificata.

**DI**

- `DomainModule`: `Clock`, `ZoneIdProvider`, use case Task.
- `RepositoryModule`: `ListItemRepository`, `TodoRepository`, `UnifiedListRepository`;
  rimozione di `HistoryRepository` (sostituito da `ListItemRepository`).
- `PresentationModule`: `UnifiedListViewModel`, `HistoryViewModel`, `AddItemViewModel`
  aggiornati.

### 5.4 Modello di dominio

```kotlin
enum class ItemKind { SHOPPING, TASK }

enum class ListItemStatus { TO_BUY, PURCHASED, DELETED }

data class ListItem(
    val id: String,
    val productId: Long,
    val quantity: Double,          // finita, > 0
    val unit: String,              // testo libero, trim non vuoto
    val brand: String?,
    val variant: String?,
    val condition: String?,
    val note: String?,
    val status: ListItemStatus,
    val purchaseCount: Int,        // >= 1 solo per PURCHASED
    val createdAt: Long,
    val statusChangedAt: Long,
    val updatedAt: Long,
)

enum class TodoPriority { NORMAL, URGENT }
enum class TodoStatus { OPEN, COMPLETED, DELETED }

data class TodoTask(
    val id: Long,
    val title: String,
    val normalizedTitle: String,
    val categoryId: Long?,
    val priority: TodoPriority,
    val dueDate: LocalDate?,
    val note: String?,
    val status: TodoStatus,
    val createdAt: Long,
    val statusChangedAt: Long,
    val completedAt: Long?,
)

sealed interface UnifiedListEntry {
    val key: String
    val kind: ItemKind
    val createdAt: Long
    val statusChangedAt: Long
    val isCompleted: Boolean

    data class ShoppingEntry(
        val item: ListItem,
        val productName: String,
        val categoryLabel: String,
    ) : UnifiedListEntry {
        override val key: String get() = "S:${item.id}"
        override val kind: ItemKind get() = ItemKind.SHOPPING
        override val createdAt: Long get() = item.createdAt
        override val statusChangedAt: Long get() = item.statusChangedAt
        override val isCompleted: Boolean get() = item.status == ListItemStatus.PURCHASED
    }

    data class TaskEntry(
        val task: TodoTask,
        val areaName: String?,
        val subcategoryName: String?,
    ) : UnifiedListEntry {
        override val key: String get() = "T:${task.id}"
        override val kind: ItemKind get() = ItemKind.TASK
        override val createdAt: Long get() = task.createdAt
        override val statusChangedAt: Long get() = task.statusChangedAt
        override val isCompleted: Boolean get() = task.status == TodoStatus.COMPLETED
    }
}
```

### 5.5 Invarianti e transizioni

**ListItem**

- `quantity.isFinite() && quantity > 0`.
- `unit.trim().isNotEmpty()`.
- `purchaseCount >= 1` se e solo se `status == PURCHASED`; `0` altrimenti.
- Transizioni ammesse: `TO_BUY → PURCHASED`, `TO_BUY → DELETED`, `DELETED → TO_BUY`.
  `PURCHASED` è terminale e immutabile.
- `statusChangedAt` cambia solo sulle transizioni di stato; `updatedAt` su ogni modifica.
- `Aggiungi di nuovo` da una riga `PURCHASED` crea una **nuova** riga `TO_BUY` (o merge su
  una riga attiva compatibile), senza riaprire la riga storica.
- Ripristino di una riga `DELETED`: se esiste una riga attiva compatibile
  (`productId` + marca/variante/conservazione case-insensitive + stessa unità), le quantità
  vengono sommate e la riga eliminata viene rimossa; altrimenti torna `TO_BUY`. Se più righe
  sono compatibili, si sceglie quella con `createdAt` più recente (tie-break `id`).

**TodoTask**

- `title.trim()` non vuoto; massimo 200 code point; nessun carattere di controllo non
  stampabile.
- `note` massimo 1000 code point; sono ammessi `\n` e `\t`, gli altri controlli sono rifiutati.
- `normalizedTitle` = trim + lowercase + collapse spazi; **non UNIQUE**.
- `completedAt != null` se e solo se `status == COMPLETED`; altrimenti `null`.
- `statusChangedAt` cambia solo su transizione di stato, non su modifica di titolo/nota.
- Transizioni ammesse: `OPEN → COMPLETED`, `COMPLETED → OPEN`, `OPEN → DELETED`,
  `DELETED → OPEN`. `COMPLETED` non è eliminabile in v1 (D12).
- Complete/reopen/delete/restore sono idempotenti rispetto allo stato corrente: una seconda
  chiamata non duplica righe e non cambia timestamp.

**Categorie Task**

- `TASK_CATEGORY` ha massimo due livelli: T1 (`parentId = null`) e T2 (`parentId` = T1).
- `categoryId` di una Task può essere `null`, una T1 o una T2; mai una T3.
- Se l'utente sceglie solo l'Area, `categoryId` punta alla T1; se sceglie la sottocategoria,
  punta alla T2.
- La cancellazione di una categoria seed non deve cancellare la Task: FK `ON DELETE SET NULL`.

### 5.6 Ordinamento e filtri

**Home, filtro `Tutti`**

1. Task `URGENT` prima (le righe Spesa non hanno priorità e hanno rango normale).
2. `createdAt DESC`.
3. `kind` (`SHOPPING` prima di `TASK`).
4. `id`/`key` come tie-break finale deterministico.

**Completati recenti**

- Massimo 10 righe totali, `statusChangedAt DESC`, poi `key`.
- Rispetta il filtro di tipo attivo (con `Tutti` mostra entrambi).
- Badge `Acquistato` / `Completato`.

**Cronologia**

- Filtro tipo `Tutti | Spesa | Da fare`.
- Filtro stato `Completati` (`PURCHASED` + `COMPLETED`) e `Eliminati` (`DELETED`).
- Ordinamento `statusChangedAt DESC`, poi `key`.
- Etichette inequivocabili: `Acquistato`, `Completato`, `Eliminato`.

**Scaduto**

- Solo Task `OPEN` con `dueDate < today(zoneId)`.
- È una label descrittiva: non cambia priorità, stato o ordinamento.

## 6. Modello dati target

### 6.1 `LIST_ITEM`

Sostituisce `SHOPPING_ITEM` e assorbe `HISTORICAL_ITEM`.

```sql
CREATE TABLE IF NOT EXISTS `LIST_ITEM` (
    `id` TEXT NOT NULL,
    `productId` INTEGER NOT NULL,
    `quantity` REAL NOT NULL,
    `unit` TEXT NOT NULL,
    `brand` TEXT,
    `variant` TEXT,
    `condition` TEXT,
    `note` TEXT,
    `status` TEXT NOT NULL,
    `purchaseCount` INTEGER NOT NULL,
    `createdAt` INTEGER NOT NULL,
    `statusChangedAt` INTEGER NOT NULL,
    `updatedAt` INTEGER NOT NULL,
    PRIMARY KEY(`id`)
);

CREATE INDEX IF NOT EXISTS `idx_list_item_product_id` ON `LIST_ITEM` (`productId`);
CREATE INDEX IF NOT EXISTS `idx_list_item_status` ON `LIST_ITEM` (`status`);
CREATE INDEX IF NOT EXISTS `idx_list_item_product_status` ON `LIST_ITEM` (`productId`, `status`);
CREATE INDEX IF NOT EXISTS `idx_list_item_created_at` ON `LIST_ITEM` (`createdAt`);
CREATE INDEX IF NOT EXISTS `idx_list_item_status_changed_at` ON `LIST_ITEM` (`statusChangedAt`);
```

Note:

- Nessuna FK su `productId`: il catalogo è un asset separato e possono esistere prodotti
  utente/orfani; la relazione resta logica come nel modello attuale. La migrazione verifica
  comunque che i `productId` non siano nulli.
- `status` è persistito come stringa (`TO_BUY`, `PURCHASED`, `DELETED`) via converter.
- `unit` conserva il valore legacy (`PIECE`, `KILOGRAM`, …: Room 2.5.2 salva l'enum per
  `name`, non per `code`) o il testo libero inserito dall'utente.
- `purchaseCount` è `1` per i nuovi acquisti e `N` per gli aggregati legacy migrati.

### 6.2 `TASK_CATEGORY`

```sql
CREATE TABLE IF NOT EXISTS `TASK_CATEGORY` (
    `id` INTEGER NOT NULL,
    `parentId` INTEGER,
    `name` TEXT NOT NULL,
    `level` INTEGER NOT NULL,
    `sortOrder` INTEGER NOT NULL,
    PRIMARY KEY(`id`),
    FOREIGN KEY(`parentId`) REFERENCES `TASK_CATEGORY`(`id`)
        ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS `idx_task_category_parent` ON `TASK_CATEGORY` (`parentId`);
CREATE INDEX IF NOT EXISTS `idx_task_category_sort` ON `TASK_CATEGORY` (`sortOrder`);
```

Seed stabile:

| ID | Parent | Nome | Livello | Sort |
|---:|---:|---|---:|---:|
| 1 | — | Casa | 1 | 10 |
| 2 | — | Commissioni | 1 | 20 |
| 3 | — | Personale | 1 | 30 |
| 4 | — | Lavoro | 1 | 40 |
| 5 | — | Altro | 1 | 50 |
| 101 | 1 | Pulizie | 2 | 10 |
| 102 | 1 | Manutenzione | 2 | 20 |
| 103 | 1 | Organizzazione | 2 | 30 |
| 201 | 2 | Ritiri | 2 | 10 |
| 202 | 2 | Spedizioni | 2 | 20 |
| 203 | 2 | Pratiche | 2 | 30 |
| 301 | 3 | Appuntamenti | 2 | 10 |
| 302 | 3 | Benessere | 2 | 20 |
| 303 | 3 | Varie | 2 | 30 |
| 401 | 4 | Attività | 2 | 10 |
| 402 | 4 | Scadenze | 2 | 20 |

`Altro` non ha sottocategorie obbligatorie. Gli ID sono stabili e non autogenerati: il seed è
parte della migrazione e non viene ricreato a ogni avvio.

### 6.3 `TODO_TASK`

```sql
CREATE TABLE IF NOT EXISTS `TODO_TASK` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `title` TEXT NOT NULL,
    `normalizedTitle` TEXT NOT NULL,
    `categoryId` INTEGER,
    `priority` TEXT NOT NULL,
    `dueDate` TEXT,
    `note` TEXT,
    `status` TEXT NOT NULL,
    `createdAt` INTEGER NOT NULL,
    `statusChangedAt` INTEGER NOT NULL,
    `completedAt` INTEGER,
    FOREIGN KEY(`categoryId`) REFERENCES `TASK_CATEGORY`(`id`)
        ON UPDATE NO ACTION ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS `idx_todo_task_status` ON `TODO_TASK` (`status`);
CREATE INDEX IF NOT EXISTS `idx_todo_task_category` ON `TODO_TASK` (`categoryId`);
CREATE INDEX IF NOT EXISTS `idx_todo_task_created_at` ON `TODO_TASK` (`createdAt`);
CREATE INDEX IF NOT EXISTS `idx_todo_task_status_priority_created`
    ON `TODO_TASK` (`status`, `priority`, `createdAt`);
CREATE INDEX IF NOT EXISTS `idx_todo_task_due_date` ON `TODO_TASK` (`dueDate`);
```

`dueDate` è `TEXT` ISO `YYYY-MM-DD`; il converter rifiuta formati non validi. `priority` e
`status` sono stringhe enum.

### 6.4 Query principali

- Attivi Spesa: `SELECT * FROM LIST_ITEM WHERE status = 'TO_BUY' ORDER BY createdAt DESC`.
- Completati Spesa: `... WHERE status = 'PURCHASED' ORDER BY statusChangedAt DESC`.
- Eliminati Spesa: `... WHERE status = 'DELETED' ORDER BY statusChangedAt DESC`.
- Attivi Task: `... WHERE status = 'OPEN' ORDER BY createdAt DESC`.
- Completati Task: `... WHERE status = 'COMPLETED' ORDER BY statusChangedAt DESC`.
- Eliminati Task: `... WHERE status = 'DELETED' ORDER BY statusChangedAt DESC`.
- Statistiche: `SELECT productId, MAX(statusChangedAt) AS lastPurchasedAt,
  SUM(purchaseCount) AS purchaseCount FROM LIST_ITEM WHERE status = 'PURCHASED'
  GROUP BY productId`.
- Statistiche e suggerimenti d'acquisto: solo righe `PURCHASED`.
- Autocomplete marca/variante nel form: righe attive + `PURCHASED`, come nel comportamento
  attuale.
- Batch catalogo: `CatalogDao.getByIds(ids)` con `IN (:ids)`, per evitare N+1 nella proiezione
  unificata; le label di categoria sono calcolate dalla tassonomia già in cache.

## 7. Migrazione Room `5 → 6`

### 7.1 Pre-requisiti

1. Abilitare `exportSchema = true` e `room.schemaLocation` **prima** di cambiare versione.
2. Generare e committare `app/schemas/it.togo.app.data.database.TogoDatabase/5.json`.
3. Aggiungere `room-testing` come dipendenza di test e configurare gli schemi come asset dei
   test Robolectric.
4. Creare `docs/tasks-migration-dry-run.md` con la fotografia del DB v5 e i conteggi attesi.
5. Rimuovere `fallbackToDestructiveMigration()` **nello stesso commit** che introduce la v6.

### 7.2 Passi della migrazione

La migrazione è una singola transazione Room. Ordine:

1. `CREATE TABLE LIST_ITEM` + indici.
2. Copia da `SHOPPING_ITEM`:
   - `status = CASE WHEN isChecked = 1 THEN 'PURCHASED' ELSE 'TO_BUY' END`;
   - `quantity = CASE WHEN quantity IS NOT NULL AND quantity > 0 AND quantity < 1e308
     THEN quantity ELSE 1.0 END` (NaN in SQLite è NULL; `+Inf` viene normalizzata);
   - `unit = CASE WHEN unit IS NULL OR TRIM(unit) = '' THEN 'PIECE' ELSE unit END`;
   - `note = NULL`;
   - `purchaseCount = CASE WHEN isChecked = 1 THEN 1 ELSE 0 END`;
   - `statusChangedAt = CASE WHEN isChecked = 1 THEN updatedAt ELSE createdAt END`;
   - `updatedAt` preservato.
3. Copia da `HISTORICAL_ITEM` come righe `PURCHASED`:
   - `id = 'legacy-hist:' || id` per evitare collisioni con ID di righe attive
     reinserite dopo un undo checkout;
   - `quantity = CASE WHEN lastQuantity IS NOT NULL AND lastQuantity > 0
     AND lastQuantity < 1e308 THEN lastQuantity ELSE 1.0 END`;
   - `unit = CASE WHEN lastUnit IS NULL OR TRIM(lastUnit) = '' THEN 'PIECE' ELSE lastUnit END`;
   - `purchaseCount = CASE WHEN purchaseCount < 1 THEN 1 ELSE purchaseCount END`;
   - `createdAt = statusChangedAt = updatedAt = purchasedAt`.
4. `DROP TABLE SHOPPING_ITEM`.
5. `DROP TABLE HISTORICAL_ITEM`.
6. `CREATE TABLE TASK_CATEGORY` + indici + seed.
7. `CREATE TABLE TODO_TASK` + indici.

La migrazione non tocca `CANONICAL_PRODUCT`, la tassonomia, `SYNONYM`, `LEARNED_RULE` o
`USER_ATTRIBUTE`.

### 7.3 Casi limite coperti dai test

- Riga attiva con `quantity = 0.0` → `1.0`, stato `TO_BUY`.
- Riga spuntata → `PURCHASED`, `purchaseCount = 1`, `statusChangedAt = updatedAt`.
- Storico aggregato con `purchaseCount = 7` → una riga `PURCHASED` con `purchaseCount = 7`.
- Collisione `HISTORICAL_ITEM.id == SHOPPING_ITEM.id` (undo checkout) → nessuna violazione di
  PK grazie al prefisso `legacy-hist:`.
- Unità legacy sconosciuta o `NULL` → `PIECE`.
- Quantità `NaN`/infinita/negativa → `1.0`.
- DB già a v5 con 0 righe → migrazione pulita.
- Installazione nuova dall'asset v1 → catena `1→2→3→4→5→6`, 748 prodotti invariati.
- Doppia esecuzione della migrazione non è possibile (Room version gate), ma il seed usa
  `INSERT OR IGNORE` per robustezza.

### 7.4 Rollback e backup

- Room esegue la migrazione in transazione: se fallisce, il DB resta a v5 e l'app non apre la
  v6 (nessun fallback distruttivo).
- Non è supportato il downgrade dell'app: la v6 elimina le vecchie tabelle. Prima del rilascio
  va verificato il backup Android e va documentata la procedura di ripristino.
- In sviluppo: esportare `togo.db` v5 con `adb exec-out run-as it.togo.app cat
  databases/togo.db > togo-v5.db` prima di applicare la build v6.
- In produzione: backup/restore Android configurato e testato; in caso di rollback si ripristina
  il backup o si reinstalla (perdita dei dati non recuperabili dal backup).

## 8. Fasi di implementazione

### F0 — Checkpoint e dry run (Story 8.1)

- Esportare lo schema v5, creare l'harness di migrazione, scrivere
  `docs/tasks-migration-dry-run.md` con conteggi e SQL.
- Nessuna modifica di comportamento.
- **Exit:** build e test verdi; `5.json` committato; dry run approvata.

### F1 — Refactor Spesa e fondamenta Task (Story 8.2–8.6)

- Abilitare core library desugaring (`desugar_jdk_libs`) per `java.time` su API 24–25.
- `Clock`/`ZoneId`, dominio `ListItem`, schema `LIST_ITEM`, migrazione `5→6`, repository
  shopping, dominio Task, tabelle/DAO/repository Task, seed categorie.
- **Exit:** migrazione testata da v5 e da asset v1; CRUD e transizioni Task verdi; statistiche
  shopping preservate.

### F2 — Adattamento della UI legacy (Story 8.7)

- La home shopping esistente compila sul nuovo modello, senza “Presi”/checkout.
- La cronologia shopping legge `PURCHASED`/`DELETED`.
- **Exit:** suite shopping esistente aggiornata e verde; nessuna regressione funzionale.

### F3 — Proiezione e home unificata (Story 9.1–9.4)

- `UnifiedListEntry`, ordinamento, repository, ViewModel, feed, filtri, completati recenti,
  empty state, accessibilità di base.
- **Exit:** Spesa e Task visibili e filtrabili nella stessa home; test di ordinamento e chiavi.

### F4 — Composer unificato (Story 9.5–9.7)

- Segmented control, form Task, date picker, validazione, doppio submit, form Spesa adattato,
  regressione voce/condivisione.
- **Exit:** aggiunta e modifica dei due tipi; stato conservato su rotazione; nessun Task nel
  catalogo.

### F5 — Ciclo di vita e cronologia (Story 10.1–10.5)

- Complete/reopen, delete/restore, cronologia unificata, svuota attivi, purge.
- **Exit:** transizioni atomiche e idempotenti; cronologia filtrabile; purge non tocca
  completati/acquistati.

### F6 — Hardening e DoD (Story 11.1–11.5)

- A11y, performance, backup/restore, process death, regressione completa, documentazione,
  validazione finale.
- **Exit:** DoD della specifica soddisfatta; build/lint/test verdi; device matrix e backup
  verificati.

## 9. Strategia di test

### 9.1 Unit JVM

- Validazione titolo/nota (code point, controlli, trim, normalizzazione).
- `LocalDate` ISO e timezone: cambio fuso non altera `dueDate`; `Scaduto` con clock fake.
- Ordinamento unificato: urgenti, `createdAt`, kind, id; stabilità.
- Filtri `Tutti | Spesa | Da fare` e stati cronologia.
- Chiavi `S:`/`T:` con ID numerici uguali.
- Transizioni Task idempotenti e invarianti `completedAt`/`statusChangedAt`.
- Transizioni ListItem, merge al ripristino, `purchaseCount`.
- Nessun merge tra Task omonime.
- Statistiche solo `PURCHASED`.

### 9.2 Room / Robolectric

- CRUD `ListItemDao`, `TodoTaskDao`, `TaskCategoryDao`.
- FK categoria: delete categoria → `categoryId = NULL`, Task intatta.
- Indici e query per stato.
- `clearActive` e `purgeDeleted` atomici su entrambe le tabelle.
- Migrazione `5→6` con `MigrationTestHelper` e schemi esportati; fallback JDBC se
  l'harness Robolectric non è stabile.
- Catena `1→6` dall'asset `catalog.db`.

### 9.3 Compose / Robolectric

- Feed unico, badge tipo, chip filtro, empty state per filtro.
- Composer: segmented control, cambio tipo senza perdita dati, validazione Task, date picker,
  doppio tap su salva.
- Completati recenti: massimo 10, badge, link alla cronologia.
- Cronologia: filtri, etichette, azioni.
- Dialog di conferma con conteggi; purge irreversibile.
- Semantics/TalkBack: content description, stato checkbox, touch target, focus.

### 9.4 Regressione shopping

- Ricerca catalogo, autocomplete, attributi, voce, condivisione, regole apprese, categorie.
- `Aggiungi di nuovo` da acquistato; statistiche e suggerimenti.
- Nessuna Task nei suggerimenti prodotto.
- AI OFF / senza rete: comportamento identico.

### 9.5 Device / manuale

- Installazione nuova, upgrade da v5 con dati reali, backup/restore.
- Rotazione e process death su composer e filtri.
- TalkBack su righe, filtri e dialog.
- API 24 e API 35 (device/emulatore disponibili), inclusa la verifica del desugaring
  `java.time`.
- Verifica che l'app non richieda permessi notifiche.

### 9.6 Comandi

```powershell
.\test-android.ps1          # Docker: :app:testDebugUnitTest
.\build-android.ps1         # build
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lint
.\gradlew.bat assembleDebug
```

Non dichiarare verde un comando non eseguito. Riportare l'output reale nel checkpoint.

## 10. Performance

- Proiezione unificata: due Flow Room + `combine`, nessuna UNION; mapping in memoria.
- Catalogo: una query batch `getByIds(ids)` per emissione, non N+1.
- Tassonomia: Flow unico con cache in ViewModel; label calcolate una volta per emissione.
- `distinctUntilChanged` sulle liste e `SharingStarted.WhileSubscribed(5_000)`.
- Completati recenti limitati a 10 lato repository/ViewModel.
- Cronologia completa in `LazyColumn`; nessuna paginazione v1 (dataset personale).
- Indici su stato, prodotto, `createdAt`, `statusChangedAt`, `dueDate`.
- Test di carico: 2.000 righe Spesa + 2.000 Task; la home deve aprirsi senza ANR e con
  scrolling fluido su device di riferimento.

## 11. Rischi e mitigazioni

| Rischio | Impatto | Mitigazione |
|---|---|---|
| Migrazione `5→6` con dati reali | Alto: perdita dati / app bloccata | Schema v5 esportato, test da snapshot e asset, dry run documentata, transazione unica, nessun fallback distruttivo, backup/restore testato. |
| Collisione ID storico/attivo | Alto: PK violation | Prefisso `legacy-hist:` e test dedicato. |
| Refactor Spesa ampio | Alto: regressioni shopping | F2 di adattamento con suite esistente; refactor prima della UI unificata; checkpoint obbligatori. |
| Statistiche/suggerimenti errati | Medio: fiducia utente | Query solo `PURCHASED`, `purchaseCount` preservato, test di regressione. |
| Ordinamento instabile | Medio: UX confusa | Comparatore puro con tie-break completi e test deterministici. |
| Doppio submit | Medio: duplicati | Pulsante disabilitato + guardia nel ViewModel + transazione idempotente. |
| `MigrationTestHelper` in Robolectric | Medio: test fragili | Schemi come asset di test; fallback JDBC con SQL condiviso; test su device manuale. |
| Crescita cronologia completati | Basso/Medio | Purge solo `DELETED` per specifica; dataset personale; monitorare dimensioni. |
| SDK 24 vs specifica 26 | Basso/Medio | Restare su 24/35; abilitare core library desugaring per `java.time` e testare su API 24. |
| Desugaring `java.time` non configurato | Alto su API 24–25 | Aggiungere `isCoreLibraryDesugaringEnabled` e `desugar_jdk_libs` in F1; test su API 24. |
| Ricerca unificata non definita | Basso | DA-1: filtri + ricerca catalogo; nessuna interpretazione silenziosa. |

## 12. Documentazione e Definition of Done

### 12.1 Documenti da creare/aggiornare

- `docs/implementation-status.md`: stato per fase, comandi eseguiti, test, rischi aperti.
- `docs/tasks-migration-dry-run.md`: schema v5, conteggi pre/post, SQL, esito test, backup e
  rollback.
- `_bmad-output/planning-artifacts/togo-tasks-unificata/epics.md`: epiche e storie.
- Eventuale aggiornamento di `ARCHITECTURE-SPINE.md` e `EXPERIENCE.md` se le decisioni D1–D14
  cambiano invarianti esistenti.

### 12.2 DoD

- Home unica con Spesa e Task alla pari, filtri e ordinamento deterministico.
- Aggiunta/completamento/eliminazione/ripristino funzionanti offline e con AI OFF.
- Task fuori da `CANONICAL_PRODUCT`, categorie merceologiche e statistiche acquisti intatte.
- Priorità e scadenza opzionali, nessuna notifica, categorie Task separate.
- Migrazione `5→6` non distruttiva e testata; backup/restore testato; nessun fallback
  distruttivo.
- `.\test-android.ps1`, `.\gradlew.bat lint`, `.\gradlew.bat assembleDebug` verdi.
- TalkBack, touch target ≥48dp, font scaling, dark mode.
- Documentazione aggiornata e revisione finale approvata.

## 13. Decisioni aperte

| ID | Domanda | Ipotesi v1 | Impatto |
|---|---|---|---|
| DA-1 | Ricerca full-text unificata nella home? | No: filtri di tipo + ricerca catalogo esistente; la cronologia mantiene la ricerca testuale | Se richiesta, aggiungere query `normalizedTitle`/nome prodotto e UI search bar (story separata). |
| DA-2 | Filtro persistente tra riavvii a freddo? | Solo `SavedStateHandle` (sessione/process recreation); default `Tutti` a freddo | Se serve persistenza reale, aggiungere `SharedPreferences` senza DataStore. |
| DA-3 | Eliminazione di Task `COMPLETED` / Spesa `PURCHASED`? | No in v1: sono storia immutabile; il purge non li tocca | Se richiesta, nuova transizione e aggiornamento specifica. |
| DA-4 | Allineamento SDK 26/36 | Rinviato | Richiede aggiornamento toolchain, test e release note. |
| DA-5 | Categorie Task personalizzate | Fuori scope v1 | Richiede schema/UI e migrazione dedicata. |

## 14. Checklist operativa per ogni storia

1. `git status` prima di iniziare; nessun commit automatico.
2. Leggere la storia e i file coinvolti; non toccare file non pertinenti.
3. Implementare dominio → dati → presentation, con test nello stesso ciclo.
4. Aggiornare `docs/implementation-status.md` e, se pertinente, il dry run.
5. Eseguire `.\gradlew.bat testDebugUnitTest` (o `.\test-android.ps1`), `lint`,
   `assembleDebug`.
6. Riportare output reale, file modificati, rischi residui e prossimo passo.
7. Fermarsi al checkpoint: nessuna fase successiva automatica.
