---
title: 'Catalogo Togo dalla proposta tassonomica'
type: 'feature'
created: '2026-11-19'
status: 'in-review'
baseline_commit: 'dd11908165d46c032648ca86bd05b75381cdece3'
route: 'dispatch'
review_loop_iteration: 0
context: ['proposta-tassonomia-prodotti.md']
---

<frozen-after-approval reason="intent utente">

## Intent

**Problem:** Il catalogo preinstallato non riflette lâ€™albero e le classificazioni proposte e non offre un percorso di migrazione completo per gli acquisti esistenti.

**Approach:** Creare direttamente il nuovo seed usando la sezione 3 come albero, la sezione 7 per gli ID e le riclassificazioni mantenuti, la sezione 8 per prodotti e sinonimi, la sezione 9 per esclusioni e la sezione 10 per accorpamenti Bio. Non migrare database giÃ  esistenti; Bio Ã¨ una variante del prodotto base.

**Always:** Il seed nuovo non contiene ID Bio duplicati nÃ© prodotti esclusi selezionabili; gli ID dei prodotti mantenuti seguono la proposta; far risolvere le espressioni Bio al prodotto base preservando Bio come variante quando presente. Nessun dato esistente viene migrato.

**Never:** Non implementare una migrazione dei database esistenti; non trattare Bio come prodotto distinto nel catalogo selezionabile; non alterare la proposta sorgente.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Espressione Bio storica | Sinonimo o import contiene Bio/Biologico | Risolve al prodotto base e registra Bio nella variante dâ€™acquisto, quando supportato dal flusso | Non creare un prodotto canonico Bio separato |
| Installazione esistente | Database contiene il catalogo precedente | Nessuna migrazione richiesta o eseguita; il nuovo seed vale per database creati da questa versione | Documentare che il catalogo vecchio non si aggiorna sui database esistenti |

</frozen-after-approval>

## Code Map

- `proposta-tassonomia-prodotti.md` â€” sorgente normativa per albero, prodotti mantenuti, nuovi prodotti/sinonimi, esclusioni e rimappatura Bio; include anche casi di import legacy.
- `tools/generate_catalog.py` â€” legge il CSV storico, genera asset SQLite, SQL di migrazione e `catalogo.md`; oggi vincola i conteggi alla tassonomia precedente. Per il seed nuovo si sostituisce il mapping con le assegnazioni della proposta.
- `tools/togo_catalog_migration_mapping_v2.csv` â€” mapping precedente da 1.470 record a 748 prodotti; non sostituisce le assegnazioni della proposta.
- `app/src/main/assets/catalog.db` â€” seed delle nuove installazioni.
- `CatalogMigration.kt`, `TogoDatabase.kt` — il passaggio Room 4→5 resta nel grafo versioni come no-op; il vecchio SQL v2 non si applica a installazioni nuove o esistenti.
- `ShoppingItemEntity.kt`, `HistoricalItemEntity.kt` â€” hanno campi `variant` per conservare Bio; `LearnedRuleEntity.kt` e `SynonymEntity.kt` contengono riferimenti ai prodotti da rimappare.

## Tasks & Acceptance

**Execution:**
- [x] Aggiornare il generatore e le fonti del seed per albero e assegnazioni delle sezioni 3, 7, 8 e 10, oltre alle esclusioni della sezione 9; controllare copertura e unicitÃ  degli ID.
- [x] Aggiornare la risoluzione dei sinonimi/import per Bio come variante e per le categorie legacy nei vecchi export, senza migrare il database installato.
- [x] Verificare staticamente il seed, le assegnazioni e i casi di ricerca/import richiesti dalla proposta.

**Acceptance Criteria:**
- Given una nuova installazione, when il seed viene caricato, then il catalogo selezionabile segue la proposta.
- Given una nuova installazione, when il seed viene caricato, then tassonomia, prodotti e sinonimi corrispondono alle sezioni 3, 7 e 8, senza ID Bio ritirati o esclusioni selezionabili.
- Given espressioni legacy o varianti Bio, when ricerca o import risolve il prodotto, then trova il prodotto base e conserva Bio come variante dâ€™acquisto.

## Implementation Notes

Non eseguire build o test in questo checkout; predisporre i controlli e i comandi per l’esecuzione utente. Non aggiornare i cataloghi nei database già esistenti. Il passaggio Room 4→5 resta come no-op di compatibilità; `catalog_migration_v2.sql` resta storico e non viene applicato.

## Verification

**Verifica statica completata:** `python -m py_compile tools/generate_catalog.py` e `python tools/generate_catalog.py` hanno avuto esito positivo. Il generatore ha verificato schema SQLite, integritÃ , conteggi (22/100/455; 892 prodotti; 64 L3 vuote), ID univoci e sinonimi senza orfani. Un controllo indipendente ha confrontato tutte le 455 triplette con la sezione 3, verificato l'assenza dei 16 ID esclusi e dei 125 ID Bio ritirati, e risolto i sinonimi espliciti e le forme Bio nel seed. `git diff --check` completato. Non sono stati eseguiti test Gradle o build.

Verifica statica del routing Room: `migration4To5` è un no-op; il file SQL v2 resta presente ma non viene caricato dalla migrazione. Le installazioni nuove conservano il seed da asset; quelle esistenti non ricevono rimappature del catalogo.

**Comandi per l'esecuzione utente:** `python tools/generate_catalog.py`; `.\gradlew.bat :app:testDebugUnitTest --tests "it.togo.app.data.database.CatalogAssetTest" --tests "it.togo.app.domain.usecase.ImportShoppingListUseCaseTest"`; `.\gradlew.bat :app:assembleDebug`.



## Review Triage Log

| Finding | Verdict | Route | Evidence e correzione |
|---|---|---|---|
| Un sinonimo esatto poteva risolvere un prodotto collocato in un L3 diverso da quello del percorso importato, lasciando le coordinate della categoria legacy. | medium | patch | Verificato nel ramo di `preview`; il resolver ora instrada al percorso canonico quando il L3 non coincide. Aggiunto un caso di test per sinonimo esatto non prefisso, non eseguito. |
| Il nuovo asset con 892 prodotti non soddisfaceva il guard storico da 748 e avrebbe potuto ricevere il vecchio SQL 4→5 durante l’apertura Room. | high | patch | Verificato confrontando `createFromAsset`, il grafo Room e il controllo in `CatalogMigration.kt`; il passaggio 4→5 ora è no-op e lascia invariati asset e DB esistenti. |
| Il percorso di import basato su sinonimo esatto non aveva un test dedicato con alias diverso dal prefisso del nome canonico. | medium | patch | Aggiunto test che asserisce ID e percorso canonici; resta da eseguire dall’utente perché la specifica vieta test/build in questo checkout. |
