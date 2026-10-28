---
title: '8.1 Baseline Schema v5 e Harness di Migrazione'
type: 'feature'
created: '2026-10-28'
updated: '2026-10-28'
status: 'done'
story: 8.1
epic: 8
context:
  - '{project-root}/_bmad-output/planning-artifacts/togo-tasks-unificata/epics.md'
  - '{project-root}/_bmad-output/planning-artifacts/togo-tasks-unificata/development-plan.md'
  - '{project-root}/docs/tasks-migration-dry-run.md'
---

## Story 8.1: Baseline Schema v5 e Harness di Migrazione

### Obiettivo

Esportare lo schema Room v5 e predisporre un harness di test delle migrazioni,
così che la futura migrazione `5→6` sia verificabile prima di modificare le
tabelle. Nessuna modifica a entità, versione del database o comportamento
dell'app.

### Acceptance Criteria

**Given** il database alla versione 5 con `exportSchema = false`
**When** abilito `exportSchema = true` e configuro `room.schemaLocation`
**Then** il build genera `app/schemas/it.togo.app.data.database.TogoDatabase/5.json`
**And** il file viene versionato nel repository
**And** nessuna entità o versione del database viene modificata

**Given** lo schema `5.json` e la dipendenza `room-testing`
**When** eseguo un test di apertura di un database v5
**Then** il test apre la versione 5 e verifica le tabelle esistenti
**And** gli schemi sono disponibili come asset dei test Robolectric
**And** il test fallisce in modo esplicito se lo schema manca

**Given** il database v5 con dati di esempio
**When** eseguo la dry run documentata
**Then** `docs/tasks-migration-dry-run.md` riporta i conteggi di `SHOPPING_ITEM`,
`HISTORICAL_ITEM`, prodotti canonici e regole apprese
**And** contiene la procedura di backup, la strategia di rollback e l'esito dei
test
**And** `.\test-android.ps1` (set minimo 8.1) resta verde

### Decisioni e interpretazioni

1. `TogoDatabase.version` resta **5**; `fallbackToDestructiveMigration()` resta
   attivo. La migrazione `5→6` appartiene alla Story 8.4.
2. Schema esportato in `app/schemas` tramite KSP:
   `room.schemaLocation = $projectDir/schemas`.
3. `MigrationTestHelper` legge gli schemi dagli asset della variante **debug**,
   non dal solo source set `test`: gli schemi sono aggiunti a entrambi.
4. L'app è in sviluppo e non in esercizio: la baseline v5 viene inizializzata
   come installazione nuova. Il catalogo (748 prodotti) viene copiato
   dall'asset `catalog.db`; non viene eseguita la catena `1→5` nell'harness.
5. `TogoDatabaseV5SchemaFallbackTest` è il fallback JVM indipendente da
   Robolectric: verifica `5.json` e i conteggi di `catalog.db` con sqlite-jdbc.
6. `.\test-android.ps1` esegue di default solo i due test della Story 8.1;
   `-All` ripristina l'intera suite.

### File nuovi

- `app/schemas/it.togo.app.data.database.TogoDatabase/5.json`
- `app/src/test/java/it/togo/app/data/database/TogoDatabaseV5BaselineTest.kt`
- `app/src/test/java/it/togo/app/data/database/TogoDatabaseV5SchemaFallbackTest.kt`
- `docs/tasks-migration-dry-run.md`
- `_bmad-output/implementation-artifacts/spec-8-1-baseline-schema-v5-e-harness-di-migrazione.md`

### File modificati

- `app/build.gradle.kts` (`ksp` schema location, asset debug/test, `room-testing`)
- `gradle/libs.versions.toml` (alias `room-testing`, Room 2.5.2)
- `app/src/main/java/it/togo/app/data/database/TogoDatabase.kt`
  (`exportSchema = true`, `version = 5` invariata)
- `gradle.properties` (`org.gradle.caching=true`)
- `test-android.ps1` (default set minimo 8.1, `-All`, `-Tests`, `-Offline`)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` (epic 8 in progress,
  8.1 done)

### Esito test

Eseguito dall'operatore con `.\test-android.ps1` il 2026-10-28:

```text
TogoDatabaseV5BaselineTest        4 test, 0 failure
TogoDatabaseV5SchemaFallbackTest  2 test, 0 failure
Totale                            6 test, 0 failure
```

`lint` e `assembleDebug` non sono stati eseguiti in questa sessione; l'ultimo
APK disponibile è precedente alle modifiche della storia.

### Rischi e note residue

1. `5.json` è generato ma ancora **untracked**: va committato con le modifiche.
2. `lint` e `assembleDebug` non sono stati verificati dopo le modifiche.
3. `fallbackToDestructiveMigration()` resta attivo fino alla Story 8.4.
4. Qualsiasi modifica a entità/colonne/indici prima della v6 cambia l'identity
   hash e invalida la baseline.
5. L'asset `catalog.db` resta a `user_version = 1`; le installazioni nuove
   continuano a eseguire `1→2→3→4→5`.

### Prossimo passo

Story 8.2: Clock e ZoneId iniettabili.
