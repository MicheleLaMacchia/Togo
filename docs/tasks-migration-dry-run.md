# Dry run migrazione Room v5 → v6 (Story 8.1)

Fotografia della baseline **v5** e procedura di verifica prima della futura
migrazione **5 → 6** (Story 8.4).

## 1. Riferimenti

| Voce | Valore |
|---|---|
| Data rilevazione | 2026-10-28 |
| Branch | `feature/refactor-livelli+da-fare` |
| Commit di riferimento | `0d48f5a` (`HEAD`; le modifiche Story 8.1 non sono committate) |
| Versione database utente | **5** |
| Versione asset `app/src/main/assets/catalog.db` | **1** |
| Room | 2.5.2 (KSP 2.0.21) |
| Kotlin / AGP | 2.0.21 / 8.7.3 |
| `minSdk` / `targetSdk` | 24 / 35 |

Stato della storia: l'app è ancora **in sviluppo e non in esercizio**. Non
esistono dati di produzione da preservare: in sviluppo è accettabile
reinizializzare il database (`adb shell pm clear it.togo.app`) e la baseline v5
può essere inizializzata come una installazione nuova, senza eseguire la catena
`1→5`. La migrazione `5→6` **non** è implementata in questa storia.

> Nota: lo schema `5.json` è stato prodotto da KSP con
> `identityHash = ba4744d98a1198138f75d44872f70684` (version 5, 9 tabelle).
> Il set minimo di test della Story 8.1 è stato eseguito con
> `.\test-android.ps1`: **6 test, 0 failure**. `lint` e `assembleDebug` non
> sono stati eseguiti in questa sessione.

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

### 4.1 Catena attuale (v5)

| Migrazione | Effetto |
|---|---|
| `1→2` | Normalizza `LEARNED_RULE.userExpression` (accent-fold, deduplica per `lastAppliedAt`). |
| `2→3` | Aggiunge `brand`, `variant`, `condition` a `HISTORICAL_ITEM`. |
| `3→4` | Crea `USER_ATTRIBUTE` con indice univoco `(type, normalized)` e importa marche/varianti esistenti. |
| `4→5` | Sostituisce il catalogo con i 748 prodotti del mapping v2, rimappando i riferimenti utente; salta se il catalogo è già v2. |

`TogoDatabase` è a `version = 5` con `fallbackToDestructiveMigration()` ancora
attivo. L'asset `catalog.db` resta a `user_version = 1`: una installazione
nuova esegue `1→2→3→4→5` al primo avvio.

### 4.2 Migrazione pianificata `5→6` (Story 8.4, non implementata)

Riferimento: `development-plan.md` §7.2–7.3.

1. Crea `LIST_ITEM` e i relativi indici.
2. Copia `SHOPPING_ITEM` in `LIST_ITEM` (`isChecked` → `PURCHASED`/`TO_BUY`,
   quantità non valida → `1.0`, unità vuota → `PIECE`).
3. Copia `HISTORICAL_ITEM` come righe `PURCHASED` con prefisso
   `legacy-hist:` sull'ID e `purchaseCount` preservato.
4. Elimina `SHOPPING_ITEM` e `HISTORICAL_ITEM`.
5. Crea `TASK_CATEGORY` con seed e `TODO_TASK` con indici.
6. Non tocca catalogo, tassonomia, sinonimi, regole apprese o attributi utente.
7. Rimuove `fallbackToDestructiveMigration()` **nello stesso commit** della v6.

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

La v6 **elimina** `SHOPPING_ITEM` e `HISTORICAL_ITEM` e non implementa il
downgrade. Non esiste un percorso automatico `6→5`. Le opzioni sono:

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

Non deve essere introdotto un fallback distruttivo nella v6: un errore di
migrazione deve lasciare il database a v5 e l'app non deve aprire la v6. La
migrazione Room è transazionale.

## 7. Harness di test e comandi

### 7.1 Configurazione

- `app/build.gradle.kts`: `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`.
- `TogoDatabase`: `exportSchema = true`, `version = 5` invariata.
- Schemi esportati negli asset della variante `debug` (quella usata dai unit
  test Robolectric) e nel source set `test`:
  ```kotlin
  sourceSets {
      getByName("debug").assets.srcDir("$projectDir/schemas")
      getByName("test").assets.srcDir("$projectDir/schemas")
  }
  ```
  Il solo source set `test` non viene merged negli asset letti da Robolectric:
  senza la riga `debug` `MigrationTestHelper` non trova `5.json`.
- Dipendenza di test: `testImplementation(libs.room.testing)` (Room 2.5.2).

### 7.2 Generazione dello schema

```powershell
.\gradlew.bat :app:kspDebugKotlin
```

Atteso: `app/schemas/it.togo.app.data.database.TogoDatabase/5.json`.

Stato: il file è presente (18.547 byte), con `"version": 5`, le 9 tabelle
attese e `identityHash = ba4744d98a1198138f75d44872f70684`. È stato prodotto da
KSP nella sessione del 2026-10-28 e va **committato** con le modifiche della
storia.

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

I test falliscono esplicitamente se `5.json` manca o non è disponibile come
asset; non esiste un ramo che li renda verdi senza lo schema.

### 7.4 Comandi di verifica

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lint
.\gradlew.bat assembleDebug
```

In assenza di Gradle/SDK locale:

```powershell
.\test-android.ps1
.\build-android.ps1
```

## 8. Esito dell'esecuzione

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

Resta da committare `5.json` insieme alle modifiche della storia.

## 9. Rischi e limiti aperti

1. **`5.json` non ancora committato**: il file è stato generato da KSP ma è
   ancora untracked. Va versionato insieme alle modifiche della storia; fino ad
   allora la baseline non è condivisa con il resto del team.
2. **`MigrationTestHelper` in Robolectric**: la prima esecuzione ha fallito
   perché il source set `test` non viene merged negli asset dell'app. Il fix
   aggiunge gli schemi alla variante `debug`; la rivalidazione è passata (6/6
   test). In caso di instabilità futura resta il fallback JVM
   `TogoDatabaseV5SchemaFallbackTest`, che non dipende da Room/Robolectric.
3. **Conteggi utente non disponibili**: `SHOPPING_ITEM`, `HISTORICAL_ITEM`,
   `LEARNED_RULE` e `USER_ATTRIBUTE` sono vuoti/assenti nell'asset. I valori
   reali richiedono un device con dati e i comandi adb di §3.
4. **`fallbackToDestructiveMigration()` ancora attivo** fino alla Story 8.4: un
   errore di schema in v5 può azzerare i dati. Non rimuoverlo in questa storia.
5. **Identity hash**: qualsiasi modifica a entità, colonne o indici prima della
   v6 cambia l'identity hash e invalida la baseline. La v5 va congelata fino
   alla Story 8.4.
6. **Asset a `user_version = 1`**: le installazioni nuove continuano a eseguire
   `1→2→3→4→5`. La baseline v5 "pulita" descritta qui è quella dello harness,
   non un nuovo asset già a v5.
7. **Backup WAL**: copiare solo `togo.db` con l'app in esecuzione può perdere
   transazioni non consolidate. Usare force-stop e acquisire `-wal`/`-shm`.
8. **Rollback non automatico**: la v6 elimina le vecchie tabelle; senza backup
   verificato il rollback richiede reinstallazione e perdita dei dati.
