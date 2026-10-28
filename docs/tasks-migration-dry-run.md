# Dry run migrazione Room v5 → v6 (Story 8.1 / 8.4)

Fotografia della baseline **v5** e procedura di verifica della migrazione
**5 → 6**, implementata nella Story 8.4 nella variante che **non** elimina le
tabelle legacy.

## 1. Riferimenti

| Voce | Valore |
|---|---|
| Data rilevazione | 2026-10-28 |
| Branch | `feature/refactor-livelli+da-fare` |
| Commit di riferimento | `0d48f5a` (`HEAD`; le modifiche Story 8.1 e 8.4 non sono committate) |
| Versione database utente | **5** prima della migrazione; **6** dopo la Story 8.4 |
| Versione asset `app/src/main/assets/catalog.db` | **1** |
| Room | 2.5.2 (KSP 2.0.21) |
| Kotlin / AGP | 2.0.21 / 8.7.3 |
| `minSdk` / `targetSdk` | 24 / 35 |

Stato della storia: l'app è ancora **in sviluppo e non in esercizio**. Non
esistono dati di produzione da preservare: in sviluppo è accettabile
reinizializzare il database (`adb shell pm clear it.togo.app`) e la baseline v5
può essere inizializzata come una installazione nuova, senza eseguire la catena
`1→5`. La migrazione `5→6` è implementata nella Story 8.4; `SHOPPING_ITEM` e
`HISTORICAL_ITEM` restano temporaneamente in vita e saranno rimosse nella
Story 8.7 con la migrazione `6→7`.

> Nota: lo schema `5.json` è stato prodotto da KSP con
> `identityHash = ba4744d98a1198138f75d44872f70684` (version 5, 9 tabelle).
> Il set minimo di test della Story 8.1 è stato eseguito con
> `.\test-android.ps1`: **6 test, 0 failure**. `lint` e `assembleDebug` non
> sono stati eseguiti in quella sessione.
>
> Per la Story 8.4 i test di migrazione sono **scritti ma non eseguiti** in
> questa sessione; `6.json` non è stato creato manualmente e sarà generato dal
> build dell'utente.

## 2. Conteggi della baseline

I conteggi del catalogo derivano dall'asset `catalog.db` e sono gli stessi che
una installazione nuova porta nella v5 dopo il seed del catalogo.

| Tabella | Asset `catalog.db` (v1) | Database v5 pulito | Note |
|---|---:|---:|---|
| `SHOPPING_ITEM` | 0 | 0 | Tabella presente ma **vuota** nell'asset. I conteggi reali vanno rilevati sul device con adb. |
| `HISTORICAL_ITEM` | 0 | 0 | Tabella presente ma **vuota** nell'asset. I conteggi reali vanno rilevati sul device con adb. |
| `CANONICAL_PRODUCT` | 748 | 748 | 608 alimentari + 140 non alimentari. |
| `TAXONOMY_LEVEL_1` | 8 | 8 | |
| `TAXONOMY_LEVEL_2` | 37 | 37 | |
| `TAXONOMY_LEVEL_3` | 98 | 98 | Nessuna L3 predefinita vuota. |
| `SYNONYM` | ≥ 748 | ≥ 748 | Invariante: ogni prodotto canonico ha almeno un sinonimo. Il conteggio esatto si ottiene con la query di §3. |
| `LEARNED_RULE` | 0 | 0 | Tabella vuota nell'asset (atteso; verificare con §3.3). Il conteggio reale va rilevato sul device. |
| `USER_ATTRIBUTE` | assente | 0 | Creata dalla migrazione `3→4`; non esiste nell'asset v1. Conteggio reale sul device. |
| `LIST_ITEM` | assente | assente | Creata dalla migrazione `5→6`; dopo la copia contiene una riga per ogni riga di `SHOPPING_ITEM` e `HISTORICAL_ITEM` (vedi §4.4). |
| `TASK_CATEGORY` | assente | assente | Creata e popolata dalla migrazione `5→6` con 16 righe (5 aree + 11 sottocategorie). |
| `TODO_TASK` | assente | assente | Creata vuota dalla migrazione `5→6`. |

Le tabelle `SHOPPING_ITEM` e `HISTORICAL_ITEM` nell'asset sono vuote: **non**
sono una fotografia dell'uso reale. Per i conteggi effettivi fare riferimento al
database del device (sezione §3).

## 3. Comandi SQL e conteggi sul device

### 3.1 Backup del database v5 dal device

Il database Room usa il journal WAL: fermare l'app e acquisire anche `-wal` e
`-shm`, altrimenti il backup può essere incompleto.

```powershell
adb shell am force-stop it.togo.app
adb exec-out run-as it.togo.app cat databases/togo.db > togo-v5.db
adb exec-out run-as it.togo.app cat databases/togo.db-wal > togo-v5.db-wal
adb exec-out run-as it.togo.app cat databases/togo.db-shm > togo-v5.db-shm
```

`run-as` richiede una build **debuggable**. Se `-wal` o `-shm` non esistono,
il comando restituisce errore: in quel caso il database era già consolidato e
bastano `togo.db` (ed eventualmente il solo `-wal`).

### 3.2 Conteggi sul database del device

```powershell
sqlite3 togo-v5.db "PRAGMA user_version;"

sqlite3 togo-v5.db @"
SELECT 'SHOPPING_ITEM'   AS tabella, COUNT(*) AS righe FROM SHOPPING_ITEM
UNION ALL SELECT 'HISTORICAL_ITEM', COUNT(*) FROM HISTORICAL_ITEM
UNION ALL SELECT 'CANONICAL_PRODUCT', COUNT(*) FROM CANONICAL_PRODUCT
UNION ALL SELECT 'TAXONOMY_LEVEL_1', COUNT(*) FROM TAXONOMY_LEVEL_1
UNION ALL SELECT 'TAXONOMY_LEVEL_2', COUNT(*) FROM TAXONOMY_LEVEL_2
UNION ALL SELECT 'TAXONOMY_LEVEL_3', COUNT(*) FROM TAXONOMY_LEVEL_3
UNION ALL SELECT 'SYNONYM', COUNT(*) FROM SYNONYM
UNION ALL SELECT 'LEARNED_RULE', COUNT(*) FROM LEARNED_RULE
UNION ALL SELECT 'USER_ATTRIBUTE', COUNT(*) FROM USER_ATTRIBUTE;
"@
```

Query di dettaglio utili:

```sql
-- Prodotti canonici predefiniti e creati dall'utente
SELECT is_user_defined, COUNT(*) FROM CANONICAL_PRODUCT GROUP BY is_user_defined;

-- Prodotti senza sinonimo (deve essere 0)
SELECT COUNT(*) FROM CANONICAL_PRODUCT p
WHERE NOT EXISTS (SELECT 1 FROM SYNONYM s WHERE s.product_id = p.id);

-- Riferimenti orfani nella lista attiva e nello storico
SELECT COUNT(*) FROM SHOPPING_ITEM s
LEFT JOIN CANONICAL_PRODUCT p ON p.id = s.productId WHERE p.id IS NULL;
SELECT COUNT(*) FROM HISTORICAL_ITEM h
LEFT JOIN CANONICAL_PRODUCT p ON p.id = h.productId WHERE p.id IS NULL;
```

### 3.3 Conteggi sull'asset

```powershell
sqlite3 app/src/main/assets/catalog.db "PRAGMA user_version;"
sqlite3 app/src/main/assets/catalog.db "SELECT COUNT(*) FROM CANONICAL_PRODUCT;"
sqlite3 app/src/main/assets/catalog.db "SELECT COUNT(*) FROM TAXONOMY_LEVEL_1;"
sqlite3 app/src/main/assets/catalog.db "SELECT COUNT(*) FROM TAXONOMY_LEVEL_2;"
sqlite3 app/src/main/assets/catalog.db "SELECT COUNT(*) FROM TAXONOMY_LEVEL_3;"
sqlite3 app/src/main/assets/catalog.db "SELECT COUNT(*) FROM SYNONYM;"
```

## 4. Percorso di migrazione

### 4.1 Catena fino alla v5

| Migrazione | Effetto |
|---|---|
| `1→2` | Normalizza `LEARNED_RULE.userExpression` (accent-fold, deduplica per `lastAppliedAt`). |
| `2→3` | Aggiunge `brand`, `variant`, `condition` a `HISTORICAL_ITEM`. |
| `3→4` | Crea `USER_ATTRIBUTE` con indice univoco `(type, normalized)` e importa marche/varianti esistenti. |
| `4→5` | Sostituisce il catalogo con i 748 prodotti del mapping v2, rimappando i riferimenti utente; salta se il catalogo è già v2. |

Dalla Story 8.4 `TogoDatabase` è a `version = 6`, con `exportSchema = true` e
**senza** `fallbackToDestructiveMigration()`. L'asset `catalog.db` resta a
`user_version = 1`: una installazione nuova esegue `1→2→3→4→5→6` al primo
avvio.

### 4.2 Migrazione implementata `5→6` (Story 8.4)

Variante concordata per questa storia: le tabelle legacy **non** vengono
eliminate, perché entità e DAO legacy sono ancora registrati e usati. La loro
rimozione è rinviata alla Story 8.7 con la migrazione `6→7`.

Ordine delle operazioni in `MIGRATION_5_6`:

1. Crea `LIST_ITEM` e i cinque indici.
2. Copia `SHOPPING_ITEM` in `LIST_ITEM` (`isChecked` → `PURCHASED`/`TO_BUY`,
   quantità non valida → `1.0`, unità vuota → `PIECE`).
3. Copia `HISTORICAL_ITEM` come righe `PURCHASED` con prefisso
   `legacy-hist:` sull'ID e `purchaseCount` preservato.
4. Crea `TASK_CATEGORY` con indice su `parentId` e `sortOrder`.
5. Esegue il seed stabile di 16 categorie.
6. Crea `TODO_TASK` con i cinque indici e le foreign key.
7. Non esegue `DROP TABLE` su `SHOPPING_ITEM` o `HISTORICAL_ITEM`.
8. Non tocca catalogo, tassonomia, sinonimi, regole apprese o attributi utente.

La migrazione è atomica: `MIGRATION_5_6` apre una transazione esplicita e Room
la esegue comunque nel proprio contesto transazionale.

### 4.3 DDL e SQL di copia

DDL delle nuove tabelle:

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

Copia da `SHOPPING_ITEM`:

```sql
INSERT INTO LIST_ITEM (
    id, productId, quantity, unit, brand, variant, condition, note,
    status, purchaseCount, createdAt, statusChangedAt, updatedAt
)
SELECT
    id,
    productId,
    CASE
        WHEN quantity IS NOT NULL AND quantity > 0 AND quantity < 1e308
        THEN quantity
        ELSE 1.0
    END,
    CASE
        WHEN unit IS NULL OR TRIM(unit) = '' THEN 'PIECE'
        ELSE unit
    END,
    brand,
    variant,
    condition,
    NULL,
    CASE WHEN isChecked = 1 THEN 'PURCHASED' ELSE 'TO_BUY' END,
    CASE WHEN isChecked = 1 THEN 1 ELSE 0 END,
    createdAt,
    CASE WHEN isChecked = 1 THEN updatedAt ELSE createdAt END,
    updatedAt
FROM SHOPPING_ITEM;
```

Copia da `HISTORICAL_ITEM`:

```sql
INSERT INTO LIST_ITEM (
    id, productId, quantity, unit, brand, variant, condition, note,
    status, purchaseCount, createdAt, statusChangedAt, updatedAt
)
SELECT
    'legacy-hist:' || id,
    productId,
    CASE
        WHEN lastQuantity IS NOT NULL AND lastQuantity > 0 AND lastQuantity < 1e308
        THEN lastQuantity
        ELSE 1.0
    END,
    CASE
        WHEN lastUnit IS NULL OR TRIM(lastUnit) = '' THEN 'PIECE'
        ELSE lastUnit
    END,
    brand,
    variant,
    condition,
    NULL,
    'PURCHASED',
    CASE WHEN purchaseCount < 1 THEN 1 ELSE purchaseCount END,
    purchasedAt,
    purchasedAt,
    purchasedAt
FROM HISTORICAL_ITEM;
```

Seed di `TASK_CATEGORY`:

```sql
INSERT OR IGNORE INTO TASK_CATEGORY (id, parentId, name, level, sortOrder) VALUES
(1, NULL, 'Casa', 1, 10),
(2, NULL, 'Commissioni', 1, 20),
(3, NULL, 'Personale', 1, 30),
(4, NULL, 'Lavoro', 1, 40),
(5, NULL, 'Altro', 1, 50),
(101, 1, 'Pulizie', 2, 10),
(102, 1, 'Manutenzione', 2, 20),
(103, 1, 'Organizzazione', 2, 30),
(201, 2, 'Ritiri', 2, 10),
(202, 2, 'Spedizioni', 2, 20),
(203, 2, 'Pratiche', 2, 30),
(301, 3, 'Appuntamenti', 2, 10),
(302, 3, 'Benessere', 2, 20),
(303, 3, 'Varie', 2, 30),
(401, 4, 'Attività', 2, 10),
(402, 4, 'Scadenze', 2, 20);
```

Note sui dati:

- I valori legacy di `unit` sono i `name` dell'enum `StandardUnit`
  (`PIECE`, `KILOGRAM`, ...), non i codici (`PC`, `KG`).
- Lo `status` in `LIST_ITEM` è la stringa del `name` di `ListItemStatus`.
- Il prefisso `legacy-hist:` evita collisioni tra `HISTORICAL_ITEM.id` e
  `SHOPPING_ITEM.id`.
- I campi `brand`, `variant`, `condition` sono copiati senza modifiche; `note`
  resta `NULL` perché le tabelle legacy non hanno una nota.
- `SHOPPING_ITEM` e `HISTORICAL_ITEM` restano presenti e invariati.

### 4.4 Conteggi attesi pre/post

Siano `S = COUNT(SHOPPING_ITEM)` e `H = COUNT(HISTORICAL_ITEM)` prima della
migrazione (misurati sul device, §3.2).

| Tabella | Prima (v5) | Dopo (v6) |
|---|---:|---:|
| `SHOPPING_ITEM` | `S` | `S` (invariata) |
| `HISTORICAL_ITEM` | `H` | `H` (invariata) |
| `LIST_ITEM` | assente | `S + H` |
| `LIST_ITEM` con `status = 'TO_BUY'` | — | righe shopping con `isChecked = 0` |
| `LIST_ITEM` con `status = 'PURCHASED'` | — | righe shopping con `isChecked = 1` + `H` |
| `TASK_CATEGORY` | assente | `16` |
| `TODO_TASK` | assente | `0` |
| `CANONICAL_PRODUCT` | `748` | `748` |
| `TAXONOMY_LEVEL_1/2/3` | `8 / 37 / 98` | `8 / 37 / 98` |
| `SYNONYM` | `>= 748` | invariato |
| `LEARNED_RULE` | invariato | invariato |
| `USER_ATTRIBUTE` | invariato | invariato |

Verifica SQL dopo l'aggiornamento:

```sql
SELECT 'LIST_ITEM' AS tabella, COUNT(*) AS righe FROM LIST_ITEM
UNION ALL SELECT 'TASK_CATEGORY', COUNT(*) FROM TASK_CATEGORY
UNION ALL SELECT 'TODO_TASK', COUNT(*) FROM TODO_TASK
UNION ALL SELECT 'SHOPPING_ITEM', COUNT(*) FROM SHOPPING_ITEM
UNION ALL SELECT 'HISTORICAL_ITEM', COUNT(*) FROM HISTORICAL_ITEM;

SELECT status, COUNT(*) FROM LIST_ITEM GROUP BY status;
SELECT COUNT(*) FROM CANONICAL_PRODUCT; -- atteso 748
```

### 4.5 Piano di cleanup `6→7` (Story 8.7)

La Story 8.4 **non** esegue il DROP. La Story 8.7 dovrà:

1. rimuovere `ShoppingItemEntity`, `HistoricalItemEntity`, `ShoppingItemDao` e
   `HistoryDao` da `TogoDatabase`;
2. aggiungere la migrazione `6→7` con `DROP TABLE SHOPPING_ITEM` e
   `DROP TABLE HISTORICAL_ITEM`, dopo aver verificato che `LIST_ITEM` contenga
   tutte le righe copiate;
3. aggiornare i repository/UI al nuovo modello unificato;
4. eseguire i test di migrazione `5→6→7` e il controllo dei conteggi.

Fino alla 8.7 le tabelle legacy sono la rete di sicurezza per il rollback e per
il confronto dei dati.

## 5. Procedura di backup prima di una build v6

1. Verificare che la build sia debuggable e che `adb devices` mostri il device.
2. Fermare l'app e acquisire il database v5 (comandi §3.1).
3. Conservare `togo-v5.db` (e `-wal`/`-shm` se presenti) fuori dal repository,
   con nome versionato, ad esempio `togo-v5-2026-10-28.db`.
4. Aprire il backup con `sqlite3` e verificare `PRAGMA user_version = 5` e i
   conteggi di §3.2 **prima** di installare la build v6.
5. Installare la build v6, avviare l'app e ripetere i conteggi sul nuovo schema
   (`LIST_ITEM`, `TODO_TASK`, `TASK_CATEGORY`) confrontandoli con il backup.

Comando di riferimento:

```powershell
adb exec-out run-as it.togo.app cat databases/togo.db > togo-v5.db
```

In produzione, oltre al backup manuale, va configurato e testato il backup
Android (Auto Backup / `adb backup` dove supportato). Su Android 12+ `adb backup`
è deprecato per molte app: la procedura `run-as` è quella di riferimento in
sviluppo.

## 6. Strategia di rollback

La v6 **non elimina** `SHOPPING_ITEM` e `HISTORICAL_ITEM`: le tabelle restano
presenti e invariate. Non esiste comunque un downgrade automatico `6→5`: una
build v5 non apre un database con `user_version = 6` senza fallback distruttivo,
che è stato rimosso. Le opzioni sono:

1. **Ripristino del backup v5** (preferita in sviluppo):
   ```powershell
   adb shell am force-stop it.togo.app
   adb push togo-v5.db /data/local/tmp/togo-v5.db
   adb shell run-as it.togo.app cp /data/local/tmp/togo-v5.db databases/togo.db
   adb shell run-as it.togo.app rm -f databases/togo.db-wal databases/togo.db-shm
   adb shell am start -n it.togo.app/.MainActivity
   ```
2. **Reinstallazione della build v5** con `adb install -r` e, se necessario,
   `adb shell pm clear it.togo.app`: comporta la perdita dei dati non inclusi
   nel backup.
3. **Backup/restore Android** in produzione: va verificato prima del rilascio.

Dalla Story 8.7 (migrazione `6→7`) le tabelle legacy verranno eliminate: da
quel momento il rollback richiederà un backup verificato. Non deve essere
reintrodotto un fallback distruttivo: un errore di migrazione deve lasciare il
database alla versione precedente. La migrazione Room è transazionale.

## 7. Harness di test e comandi

### 7.1 Configurazione

- `app/build.gradle.kts`: `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`.
- `TogoDatabase`: `exportSchema = true`, `version = 6`,
  `@TypeConverters(TogoTypeConverters::class)`, nessun
  `fallbackToDestructiveMigration()`.
- Entità registrate: le 9 esistenti (incluse le due legacy) più `LIST_ITEM`,
  `TASK_CATEGORY` e `TODO_TASK`.
- Schemi esportati negli asset della variante `debug` (quella usata dai unit
  test Robolectric) e nel source set `test`:
  ```kotlin
  sourceSets {
      getByName("debug").assets.srcDir("$projectDir/schemas")
      getByName("test").assets.srcDir("$projectDir/schemas")
  }
  ```
  Il solo source set `test` non viene merged negli asset letti da Robolectric:
  senza la riga `debug` `MigrationTestHelper` non trova `5.json` e `6.json`.
- Dipendenza di test: `testImplementation(libs.room.testing)` (Room 2.5.2).

### 7.2 Generazione degli schemi

```powershell
.\gradlew.bat :app:kspDebugKotlin
```

Attesi:

- `app/schemas/it.togo.app.data.database.TogoDatabase/5.json`: baseline
  congelata, `"version": 5`, 9 tabelle,
  `identityHash = ba4744d98a1198138f75d44872f70684`; non deve cambiare.
- `app/schemas/it.togo.app.data.database.TogoDatabase/6.json`: **generato dal
  build** dopo la Story 8.4, `"version": 6`, 12 tabelle (9 legacy + 3 nuove).
  Non crearlo manualmente.

Stato 8.4: i file sorgente sono stati modificati, ma `6.json` non è stato
generato in questa sessione perché non è stato eseguito alcun build. Sarà
prodotto dal comando qui sopra (o da `assembleDebug`) e va **committato** con le
modifiche della storia.

In assenza di SDK Android locale, usare Docker:

```powershell
docker run --rm -e ANDROID_HOME=/opt/android-sdk `
  -v "${PWD}:/workspace" -v "android-gradle-cache:/root/.gradle" `
  -w /workspace android-builder sh ./gradlew :app:kspDebugKotlin
```

### 7.3 Test aggiunti

| Test | Cosa verifica |
|---|---|
| `TogoDatabaseV5BaselineTest.schema 5 is exported...` | `5.json` esiste, `version = 5`, tutte le 9 tabelle con `createSql` non vuoto. |
| `TogoDatabaseV5BaselineTest.schema 5 is packaged...` | `5.json` è leggibile come asset del test Robolectric. |
| `TogoDatabaseV5BaselineTest.fresh v5 database opens...` | `MigrationTestHelper` crea un DB v5, `user_version = 5`, tabelle attese, tabelle utente vuote. |
| `TogoDatabaseV5BaselineTest.fresh v5 database initialized...` | Baseline v5 inizializzata dal catalogo: 748 prodotti, 8/37/98 categorie, nessun prodotto senza sinonimo. |
| `TogoDatabaseV5SchemaFallbackTest.exported schema 5...` | Fallback JVM indipendente da Robolectric: esistenza e contenuto minimo di `5.json`. |
| `TogoDatabaseV5SchemaFallbackTest.catalog asset...` | Fallback JVM: `catalog.db` ha `user_version = 1`, 748 prodotti e 8/37/98 categorie. |
| `TogoDatabaseMigration5To6Test.migration 5 to 6 copies legacy rows and keeps old tables` | Apre un DB v5, inserisce shopping e storico, esegue `runMigrationsAndValidate(..., 6, true, MIGRATION_5_6)`, verifica le tre nuove tabelle, i 5 casi di copia, il seed di 16 categorie, `TODO_TASK` vuota e l'invarianza delle tabelle legacy. |
| `TogoDatabaseMigration5To6Test.new installation from catalog asset v1 migrates through v6` | Apre una installazione nuova da `catalog.db` v1 con la catena `1→2→3→4→5→6`, verifica `user_version = 6`, 748 prodotti, 8/37/98 categorie, 16 categorie Task e presenza delle tabelle legacy. |

Casi coperti dal test 5→6:

- `SHOPPING_ITEM.quantity = 0.0` → `LIST_ITEM.quantity = 1.0`, `TO_BUY`,
  `purchaseCount = 0`;
- `SHOPPING_ITEM.isChecked = 1` → `PURCHASED`, `purchaseCount = 1`,
  `statusChangedAt = updatedAt`;
- `HISTORICAL_ITEM.purchaseCount = 7` → `PURCHASED`, `purchaseCount = 7`,
  `createdAt = statusChangedAt = updatedAt = purchasedAt`;
- stesso `id` in `SHOPPING_ITEM` e `HISTORICAL_ITEM` → due righe distinte
  (`id` originale e `legacy-hist:<id>`), nessuna violazione di PK;
- `unit` vuota → `PIECE` (il caso `NULL` è gestito dalla SQL ma non è
  inseribile nello schema v5, dove `unit` è `NOT NULL`);
- `brand`, `variant`, `condition` preservati;
- `TASK_CATEGORY` con 16 righe e nomi attesi;
- `TODO_TASK` vuota;
- `SHOPPING_ITEM` e `HISTORICAL_ITEM` ancora presenti e con i valori originali.

I test falliscono esplicitamente se `5.json` o `6.json` mancano o non sono
disponibili come asset; non esiste un ramo che li renda verdi senza lo schema.
In particolare `TogoDatabaseMigration5To6Test` legge `6.json` e ne verifica
`"version": 6` e le tre nuove tabelle prima di eseguire la migrazione.

### 7.4 Comandi di verifica

Eseguire prima il build/KSP, così `6.json` viene generato e validato:

```powershell
.\gradlew.bat :app:kspDebugKotlin
.\gradlew.bat assembleDebug
```

Poi i test mirati e la suite completa:

```powershell
.\gradlew.bat testDebugUnitTest --tests "it.togo.app.data.database.*"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lint
```

In assenza di Gradle/SDK locale:

```powershell
.\test-android.ps1
.\build-android.ps1
```

`6.json` **non** deve essere creato o modificato a mano: se manca, i test della
Story 8.4 falliscono con un messaggio esplicito che rimanda al build.

## 8. Esito delle esecuzioni

### 8.1 Esito storico della Story 8.1

Il set minimo della Story 8.1 è stato eseguito dall'operatore con
`.\test-android.ps1` il 2026-10-28:

```text
TogoDatabaseV5BaselineTest        4 test, 0 failure  (~51,6 s)
TogoDatabaseV5SchemaFallbackTest  2 test, 0 failure  (~1,3 s)
Totale                            6 test, 0 failure
```

La prima esecuzione aveva prodotto:

```text
TogoDatabaseV5BaselineTest > schema 5 is packaged as a Robolectric test asset FAILED
    java.lang.AssertionError at TogoDatabaseV5BaselineTest.kt:115
        Caused by: java.io.FileNotFoundException at TogoDatabaseV5BaselineTest.kt:113

TogoDatabaseV5BaselineTest > fresh v5 database opens with version 5 and every expected table FAILED
    java.lang.AssertionError at TogoDatabaseV5BaselineTest.kt:254

TogoDatabaseV5BaselineTest > fresh v5 database initialized from catalog asset has 748 canonical products FAILED
    java.lang.AssertionError at TogoDatabaseV5BaselineTest.kt:254
```

Causa: il source set `test` non viene merged negli asset letti da Robolectric,
quindi `it.togo.app.data.database.TogoDatabase/5.json` non era presente in
`context.assets`. Correzione applicata in `app/build.gradle.kts`: gli schemi
sono ora aggiunti anche agli asset della variante `debug`. Dopo il fix tutti e
6 i test passano.

| Comando | Esito |
|---|---|
| `.\test-android.ps1` (set minimo 8.1) | **PASS**: 6 test, 0 failure |
| `.\gradlew.bat :app:kspDebugKotlin` | **ESEGUITO (interrotto)**: `5.json` generato (version 5, 9 tabelle, identityHash `ba4744d98a1198138f75d44872f70684`). |
| `.\gradlew.bat lint` | **NON ESEGUITO** |
| `.\gradlew.bat assembleDebug` | **NON ESEGUITO** in questa sessione; l'ultimo APK disponibile è precedente alle modifiche. |

`5.json` è ora versionato; per la Story 8.4 resta da generare e committare
`6.json` insieme alle modifiche.

### 8.4 Esito della Story 8.4

I test di migrazione `5→6` e quello di installazione nuova da `catalog.db` sono
stati **scritti ma non eseguiti** in questa sessione. Non è stato eseguito
nessun comando Gradle, Docker, Robolectric, lint o build, e `6.json` non è stato
creato manualmente.

| Comando | Esito |
|---|---|
| `.\gradlew.bat :app:kspDebugKotlin` | **DA ESEGUIRE**: genera `6.json` con 12 tabelle. |
| `.\gradlew.bat assembleDebug` | **DA ESEGUIRE**: verifica desugaring, KSP e packaging. |
| `.\gradlew.bat testDebugUnitTest --tests "it.togo.app.data.database.*"` | **DA ESEGUIRE**: test 8.4 e test baseline 8.1. |
| `.\gradlew.bat testDebugUnitTest` | **DA ESEGUIRE**: suite completa. |
| `.\gradlew.bat lint` | **DA ESEGUIRE**. |

Nessun risultato è da considerarsi verde finché l'operatore non esegue i
comandi e riporta l'esito.

## 9. Rischi e limiti aperti

1. **`6.json` non ancora generato/committato**: `5.json` è versionato ed è la
   baseline congelata; `6.json` deve essere generato dal build 8.4 e versionato
   insieme alle modifiche della storia.
2. **`MigrationTestHelper` in Robolectric**: la prima esecuzione della Story
   8.1 ha fallito perché il source set `test` non viene merged negli asset
   dell'app. Il fix aggiunge gli schemi alla variante `debug`; la rivalidazione
   è passata (6/6 test). Per la 8.4 la stessa configurazione deve rendere
   disponibile `6.json`; se il file manca, il test fallisce con un messaggio
   esplicito.
3. **Conteggi utente non disponibili**: `SHOPPING_ITEM`, `HISTORICAL_ITEM`,
   `LEARNED_RULE` e `USER_ATTRIBUTE` sono vuoti/assenti nell'asset. I valori
   reali richiedono un device con dati e i comandi adb di §3.
4. **`fallbackToDestructiveMigration()` rimosso**: dalla Story 8.4 un errore di
   migrazione non azzera più il database, ma l'app non si apre. Verificare i log
   `Room` e ripristinare il backup in caso di errore.
5. **Identity hash**: `5.json` (9 tabelle,
   `ba4744d98a1198138f75d44872f70684`) deve restare invariato. `6.json` avrà un
   nuovo hash per le 12 tabelle e i nuovi indici. Non modificare `5.json` a
   mano.
6. **Asset a `user_version = 1`**: le installazioni nuove eseguono
   `1→2→3→4→5→6`. La baseline v5 "pulita" descritta qui è quella dello harness,
   non un nuovo asset già a v5.
7. **Backup WAL**: copiare solo `togo.db` con l'app in esecuzione può perdere
   transazioni non consolidate. Usare force-stop e acquisire `-wal`/`-shm`.
8. **Rollback non automatico**: la v6 conserva le tabelle legacy, ma una build
   v5 non apre un DB `user_version = 6` senza fallback distruttivo. Il rollback
   richiede il backup v5. Dalla 8.7 (migrazione `6→7`) le tabelle legacy
   verranno eliminate.
9. **Corrispondenza DDL/schema 6**: il test `5→6` usa
   `runMigrationsAndValidate(..., true, MIGRATION_5_6)`; eventuali differenze tra
   il DDL della migrazione e `6.json` (FK, indici, AUTOINCREMENT) faranno
   fallire la validazione. Verificare il primo `assembleDebug`/test.
10. **Foreign key e indici Task**: `TASK_CATEGORY` ha FK self-referencing con
    `ON DELETE CASCADE`; `TODO_TASK` ha FK su `TASK_CATEGORY` con
    `ON DELETE SET NULL`. Il seed inserisce prima i parent. Se il build segnala
    indici mancanti, controllare `idx_task_category_parent` e
    `idx_todo_task_category`.
11. **Installazione nuova**: il test `catalog.db` v1 → v6 dipende dalla catena
    completa e dall'asset a 748 prodotti. Se fallisce, verificare che
    `MIGRATION_4_5` sia ancora un no-op sull'asset e che `catalog.db` non sia
    stato modificato.
