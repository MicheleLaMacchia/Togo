# TOGO — Specifica funzionale e tecnica: lista personale unificata (Spesa + Da fare)

**Versione:** 1.0  
**Data:** 28 ottobre 2026  
**Stato:** requisito approvato per futura implementazione incrementale; non descrive funzioni già presenti nel codice.  
**Baseline:** `togo-specifichev0.14.md`, `togo_develop_planv0.6.md`.  
**Destinatari:** product owner, Codex/ChatGPT Work e agenti di sviluppo con accesso al repository.

## 0. Contratto per l'agente

- Leggere specifica v0.14, piano v0.6, questo documento, codice e `docs/implementation-status.md` **prima** di modificare file.
- Questo documento dettaglia il modulo Tasks e la lista unificata. Se emerge un conflitto con la specifica generale, **fermarsi e segnalarlo**: la specifica generale è fonte di verità. Non ridefinire tacitamente requisiti.
- Implementare **solo il task esplicitamente assegnato**, con test nello stesso ciclo; non avviare automaticamente la fase successiva e non fare commit senza autorizzazione.
- Non convertire automaticamente righe spesa in Task, né cancellare/migrare dati utente senza test su copia del DB e piano di rollback.
- Il core continua a funzionare offline: Tasks non richiede AI, accesso account, rete, permessi notifiche o servizi remoti.

## 1. Decisione di prodotto

TOGO diventa **una sola lista personale**, non due applicazioni/list manager separati. La schermata principale mostra, gestisce e ricerca **alla pari** due tipi di elementi:

1. **Spesa (`SHOPPING`)**: riferimenti a prodotti del catalogo o prodotti aggiunti dall'utente, quantità, unità, note, categoria merceologica, storico acquisti.
2. **Da fare (`TASK`)**: testo libero, eventuale area/sottocategoria, priorità, scadenza e note, con storico completamenti.

**'Alla pari' significa**: un solo entry point, stesso livello di visibilità, stessa facilità di inserimento e completamento, filtro rapido per tipo; non significa fondere le semantiche, i cataloghi o le tabelle del dominio.

### 1.1 Non obiettivi della prima release Tasks

Non introdurre: liste separate/progetti, multiutenza, subtasks, dipendenze tra attività, ricorrenze, notifiche/alarms, collegamento calendario, geofencing, allegati, condivisione, backend/sync, task automatici creati da AI senza conferma, integrazione Assistant italiana non verificata. Le scadenze **non producono notifiche** nella prima release.

## 2. Esperienza utente

### 2.1 Home — singola lista persistente

- Un'unica schermata **La mia lista**, con righe attive `TO_BUY` e `OPEN` in **un unico flusso**.
- Ogni riga presenta icona/etichetta accessibile `Spesa` o `Da fare`; i due tipi hanno lo stesso peso visivo.
- Azioni rapide per ciascuna riga: spuntare, modificare, eliminare; nessun comando diretto ambiguo su entrambi i tipi.
- Spesa mostra `nome`, `quantità`, `unità`, nota e categoria merceologica. Tasks mostra `titolo`, eventuale area/sottocategoria, priorità, scadenza e nota.
- Filtri persistenti solo come preferenza **Tutti | Spesa | Da fare**; default `Tutti`. I filtri non sono liste diverse e non influiscono sui dati.
- In `Tutti`, l'ordinamento default è **urgenti prima**, poi per `createdAt DESC`, quindi `kind` e `id` come tie-break deterministici. Una riga spesa senza priorità ha rango normale. Non inferire priorità da data scaduta senza consenso.
- Possibile raggruppamento visuale facoltativo per tipo, ma **non deve essere l'unica vista**: l'impostazione iniziale deve essere davvero un elenco unificato; le categorie sono metadata.
- Una sezione collassabile **Completati recenti** può mostrare fino a 10 righe totali di entrambi i tipi, con badge chiaro, senza sostituire Cronologia. Le statistiche di acquisto continuano a leggere **solo** `PURCHASED`.
- Stato vuoto per `Tutti`, `Spesa`, `Da fare`; filtrare non deve far apparire che i dati degli altri tipi siano stati cancellati.

### 2.2 Inserimento rapido

- Un solo comando primario `Aggiungi` apre un composer con selezione esplicita e accessibile **Spesa | Da fare**. Il tipo di default è l'ultimo usato nella sessione, oppure `Spesa` alla prima apertura; modificarlo non cancella inaspettatamente testo già digitato.
- `Spesa`: resta il flusso esistente con autocomplete catalogo, quantità e unità **in campi separati**, categoria e note.
- `Da fare`: testo libero obbligatorio; `Area`, `Sottocategoria`, `Priorità`, `Scadenza` e `Note` opzionali. Default `NORMAL`, `OPEN`, nessuna data/categoria.
- Non creare Task tramite una voce fittizia di `CANONICAL_PRODUCT` e non mostrare i Task fra i suggerimenti dei prodotti.
- L'utente può sempre inserire `Lavare casa` senza selezionare categoria, priorità o scadenza.
- Quando il testo è ambiguo (`comprare il pane`, `portare fuori la spazzatura`) la scelta esplicita `Spesa | Da fare` prevale; non usare una classificazione silenziosa per cambiare tipo.
- Chiusura o rotazione del composer: conservare i campi non salvati nella sessione secondo normale `SavedStateHandle`/Compose; mai creare duplicati su doppio tap.

### 2.3 Operazioni e cronologia

- Spunta su `SHOPPING` = `TO_BUY → PURCHASED`. Lo storico acquisti resta immutabile; `Aggiungi di nuovo` crea/merge una nuova riga attiva senza riaprire la riga storica.
- Spunta su `TASK` = `OPEN → COMPLETED`, `completedAt` valorizzato con il `Clock` iniettato. `Riapri` = `COMPLETED → OPEN` **sulla stessa attività** e `completedAt = null`. Nessuna statistica d'acquisto cambia.
- Eliminazione di una riga aperta = stato `DELETED`; recuperabile da Cronologia fino a purge.
- `Ripristina` di una Task `DELETED` = `OPEN` sulla stessa identità; una Task **non si fonde** con un'altra Task omonima.
- `Svuota attivi` richiede una conferma con riepilogo **conteggi Spesa + Da fare** e porta solo `TO_BUY` e `OPEN` a `DELETED` in un'unica transazione; si può ripristinare ogni elemento dalla Cronologia.
- `Purge eliminati` richiede conferma irreversibile e rimuove solo `DELETED` di entrambi i tipi, mai `PURCHASED` o `COMPLETED`; categorie/catalogo restano intatti.
- Cronologia: filtri per tipo (`Tutti`, `Spesa`, `Da fare`) e stato (`Completati`, `Eliminati`), indicazione inequivocabile di `PURCHASED` vs `COMPLETED`.

## 3. Modello categorie Tasks (separato dal catalogo spesa)

Le categorie dei prodotti esistenti continuano a essere L1/L2/L3 merceologici in `catalog.db`. `TASK` **non è una nuova L1 dentro il catalogo**: è un valore distinto di `ItemKind`.

Un Task può avere **area (livello logico T1)** e **sottocategoria (T2)**, entrambe opzionali; per la prima versione **non creare T3** e non imporre l'assegnazione.

| Area suggerita T1 | Sottocategorie T2 suggerite |
|---|---|
| Casa | Pulizie, Manutenzione, Organizzazione |
| Commissioni | Ritiri, Spedizioni, Pratiche |
| Personale | Appuntamenti, Benessere, Varie |
| Lavoro | Attività, Scadenze |
| Altro | Nessuna obbligatoria |

- Esempio: `Lavare casa` → `Casa > Pulizie`, `priority=URGENT`. `Urgente` **non** è un nodo della tassonomia.
- La tassonomia Task è un seed **separato** (tabella `TaskCategory` o asset di configurazione con ID stabili); non usa le categorie merceologiche, `category_map.json`, `CategoryResolver` dei prodotti o apprendimento per prefisso.
- L'utente deve poter lasciare la categoria vuota. Non aprire automaticamente una modale categorie per ogni nuovo Task.
- Se si decide di permettere categorie personalizzate in futuro, l'agente deve richiedere esplicita evoluzione schema/UI: **non** è prerequisito v1 Tasks.

## 4. Priorità, scadenze e ordinamento

- Priorità enum `NORMAL | URGENT` (default `NORMAL`); **indipendente** da categoria e data.
- Scadenza `LocalDate?` (giorno di calendario locale, **non timestamp UTC a mezzanotte**) e mai obbligatoria; niente ora nella v1 Tasks.
- La label `Scaduto` deriva da `dueDate < LocalDate.now(userZone)` per Task aperte; non altera automaticamente `priority` o `status`.
- Salvataggio DB della data in ISO `YYYY-MM-DD` (TEXT) via converter, senza timezone ambigua; lettura/visualizzazione nel fuso del dispositivo con `Clock`/`ZoneId` iniettabile nei test.
- Un cambio timezone non deve mutare la data di scadenza salvata.
- Ordinamento di default in `Tutti`: `priority URGENT` prima tra Task, poi creazione recente fra gli altri (deterministico). Un ordinamento alternativo per `dueDate` può essere introdotto successivamente, non è necessario per la prima implementazione.

## 5. Dati persistenti e architettura

### 5.1 Core separati, vista unificata

Preferire **due aggregate** distinte, senza trasformare i `Product` e i `ListItem` in una tabella universale:

```text
CANONICAL_PRODUCT -> Shopping ListItem -> ShoppingRepository
TASK_CATEGORY     -> TodoTask          -> TodoRepository
                                 \           /
                                  UnifiedListRepository
                                          |
                                  UnifiedListViewModel
                                          |
                                  Compose: La mia lista
```

- `UnifiedListRepository` **compone due `Flow` Room** in memoria (`combine`) e mappa a `UnifiedListEntry`, senza una `UNION` fragile su entità eterogenee.
- Definire per la UI un `sealed interface UnifiedListEntry { val key: String; val createdAt: Long; ... }` con varianti `ShoppingEntry` / `TaskEntry`; `key` composto da `S:<listItemId>` e `T:<taskId>` per evitare collisioni ID.
- Gli use case continuano a vivere nei rispettivi domini. Azioni UI dispatch per `ItemKind`, non per stringhe categoria.
- Un solo `StateFlow<UnifiedListUiState>` del ViewModel principale; la navigazione può mantenere schermate e ViewModel separati per form/cronologia se necessario.
- Nessuna dipendenza AI o permesso notifiche introdotto nel dominio Tasks.

### 5.2 Entità Task (schema concettuale)

```kotlin
@Entity(
    tableName = "todo_task",
    indices = [Index("status"), Index("categoryId"), Index("createdAt")]
)
data class TodoTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val normalizedTitle: String, // per ricerca, NON UNIQUE
    val categoryId: Long? = null,
    val priority: TodoPriority = TodoPriority.NORMAL,
    val dueDate: LocalDate? = null, // converter esplicito
    val note: String? = null,
    val status: TodoStatus = TodoStatus.OPEN,
    val createdAt: Long,
    val statusChangedAt: Long,
    val completedAt: Long? = null
)
```

`TaskCategory` (se persiste) ha `id` stabile e `parentId?`, nome e vincolo di profondità massima 2. Una `TodoTask` può usare `categoryId=null` e non deve perdere il proprio titolo per la cancellazione di una categoria seed. Applicare `ON DELETE SET NULL` se supportato dall'implementazione delle FK.

Vincoli:

- `title.trim()` non vuoto; massimo 200 code point Unicode, nota massimo 1000; escludere caratteri di controllo non stampabili. Coerenza col limite comune del core, se più restrittivo.
- `normalizedTitle`: lowercase/trim/spazi per ricerca; **non UNIQUE**, perché due attività omonime possono coesistere.
- `status ∈ OPEN|COMPLETED|DELETED`; `completedAt != null` solo quando `status=COMPLETED`; altrimenti `null`.
- `createdAt`/`statusChangedAt` in epoch millis; `Clock` iniettato. Non aggiornare `statusChangedAt` quando si modifica solo il titolo o la nota.
- `updateTask`, `completeTask`, `reopenTask`, `deleteTask`, `restoreTask` devono essere transazionali e idempotenti a seconda dello stato corrente; doppio tap non crea righe duplicate.
- Nessun conteggio storico acquisti deriva da task completate.

### 5.3 Migrazione dal modello shopping-only

La scelta dell'utente di avere un'unica lista **non autorizza a convertire le tabelle spesa esistenti**. Strategia preferita:

1. Fotografare il DB corrente, version/schema, eventuali dataset seed e dati utente.
2. Aggiungere tabelle Tasks tramite migrazione Room esplicita `N -> N+1` dopo che esistono dati utilizzati realmente; NON usare `fallbackToDestructiveMigration()`.
3. Preservare tutti i `ListItem`, `Product`, `PURCHASED`, `DELETED`, timestamp, statistiche e associazioni.
4. Aggiungere la proiezione unificata, conservando filtri e cronologia per tipo.
5. Test di migrazione da DB reale precedente, install/update su device e backup/restore.

Nel caso `catalog.db` asset distinto dal DB Room utente, **non** confondere migrazione del seed con migrazione dello storico personale.

## 6. Flussi di accettazione (Given / When / Then)

| ID | Given | When | Then |
|---|---|---|---|
| U01 | App senza rete / AI OFF | aggiungo `Lavare casa` | si crea `TASK OPEN`, la lista Spesa non cambia |
| U02 | `Pane` già TO_BUY | aggiungo `Lavare casa` | entrambi visibili nella vista `Tutti` |
| U03 | Filtro `Spesa` attivo | seleziono `Da fare` | vedo solo Task, senza eliminare pane |
| U04 | `Lavare casa` OPEN | spunto | `COMPLETED`, `completedAt` valorizzato, nessun `PURCHASED` aggiunto |
| U05 | Task COMPLETED | riapro | la stessa riga torna OPEN, `completedAt=null` |
| U06 | Due Task `Lavare casa` | salvo | entrambe esistono, nessun auto-merge |
| U07 | Task `Lavare casa` | imposto `Casa/Pulizie`, URGENT | la categoria è Task-only; priorità separata |
| U08 | Task OPEN con data scaduta | apro app domani | badge `Scaduto`, nessuna notifica o modifica priorità automatica |
| U09 | Shopping PURCHASED + Task COMPLETED | apro Cronologia | entrambi leggibili e filtrabili senza confondere i due stati |
| U10 | SHOPPING e TASK attive | confermo `Svuota attivi` | entrambe DELETED in transazione, ripristinabili |
| U11 | Task DELETED | ripristino | stessa Task OPEN, nessun merge su titolo |
| U12 | App upgraded con dati Room shopping | applico migration Tasks | acquisti/storico/ID preservati |
| U13 | Task e Item hanno lo stesso id numerico | compongo lista | chiavi UI distinte `T:id` e `S:id` |
| U14 | modalità `Tutti` | ordino vista | priorità URGENT stabile, tie-break deterministici |
| U15 | doppio tap su salva Task | richiesta concorrente | singola nuova Task, pulsante disabilitato durante submit |
| U16 | AI/provider assente o guasto | aggiungo Task | comportamento identico a offline |
| U17 | titolo Task `comprare latte` | aggiungo tipo Task | resta Task, non Shopping per NLP silenziosa |
| U18 | form Shopping | aggiungo `Latte di Avena`, qty 6 | Product resta `Latte di Avena`, `ListItem.quantity=6` |
| U19 | Task con scadenza 2026-11-02 | cambio timezone | `dueDate` rimane 2026-11-02 |
| U20 | utente conferma purge DELETED | eseguo purge | PURCHASED e COMPLETED preservati |

## 7. Test richiesti

**Unit:** validazione titolo, test `LocalDate` e timezone, sorting unificato, conflitti ID, filtri, enum/stati, nessun merge fra task omonime, `Clock` fake.  
**Room:** CRUD Task, FK categoria facoltativa, indici, complete/reopen/restore/delete/clear/purge atomici, migrazione DB pre-Tasks.  
**UI Compose:** input type selection, una sola lista, filtri, empty state, priorità, editor data, doppio submit, accessibilità e rotazione.  
**Regression:** suite shopping esistente invariata (re-add purchased, stats, merge, categorie); AI OFF senza rete; backup/restore; import aggiornamento del seed catalogo.  
**Security:** nessun accesso credential provider da task repository; nessuna invocazione AI/task automatica.

## 8. Scomposizione in incrementi per Codex/Work

- **T1 — Design/checkpoint:** leggere DB/repository, produrre schema/migration diff e test plan; **non implementare** prima del piano di impatto.
- **T2 — Task domain + Room:** enum, schema, DAO, repository, transizioni, test e migrazioni.
- **T3 — Unified projection:** merge dei due Flow, filtri, ordine, chiavi UI, unit test.
- **T4 — Composer + Home:** tipo esplicito, form Task, lista unica, UI test.
- **T5 — Cronologia / clear / purge:** semantiche separate, transazioni e regressioni.
- **T6 — Hardening/DoD:** device, processo, a11y, backup, perf e dogfood offline.

Per ogni incremento: Git status prima/dopo, no commit automatico, `./gradlew test`, `./gradlew lint`, `./gradlew assembleDebug` su ambiente configurato; non dichiarare verde un comando non eseguito. Windows PowerShell: `./gradlew.bat` / `.\gradlew.bat`.

## 9. Definition of Done Tasks

- Un'unica home unificata, che gestisce **alla pari** Spesa e Da fare senza creare due liste persistenti.
- `Spesa` e `Da fare` filtrabili, entrambe aggiungibili e completabili con AI OFF e device offline.
- Task non vive in `CANONICAL_PRODUCT`, non altera categorie, statistiche acquisti né stato Purchased.
- Priorità e scadenza opzionali, senza notifiche; categorie Tasks separate dai tre livelli merceologici.
- Migrazioni Room non distruttive e testate; backup/restore testati; test e build verdi.

## 10. Prompt agente pronto all'uso

> Leggi `docs/togo-specifichev0.14.md`, `docs/togo_develop_planv0.6.md`, `docs/togo-lista-unificata-tasks-v1.0.md` e `docs/implementation-status.md`, poi ispeziona repository, DB Room, `catalog.db` se presente e Git status. Prima produci una **dry run** della migrazione verso la lista unificata: tabelle Task separate dal catalogo prodotti, flussi aggregati, modifiche alla UI, dati da preservare e test. Non modificare codice o database in questa prima esecuzione; segnala ambiguità e differenze rispetto ai documenti. Dopo approvazione, implementa **solo il primo task assegnato** del piano v0.6, con test e checkpoint. Non implementare notifiche, sync, AI Tasks o nuove liste.
