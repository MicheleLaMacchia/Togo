---
stepsCompleted: [1, 2, 3, 4]
status: complete
inputDocuments:
  - _bmad-output/planning-artifacts/togo-tasks-unificata/inputs/togo-specifichev0.14.md
  - _bmad-output/planning-artifacts/togo-tasks-unificata/inputs/togo_develop_planv0.6.md
  - _bmad-output/planning-artifacts/togo-tasks-unificata/inputs/togo-lista-unificata-tasks-v1.0.md
  - _bmad-output/planning-artifacts/prds/prd-Togo-2026-09-20/prd.md
  - _bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md
  - _bmad-output/planning-artifacts/epics.md
  - _bmad-output/planning-artifacts/togo-tasks-unificata/development-plan.md
---

# Togo - Epic Breakdown: lista personale unificata (Spesa + Da fare)

## Overview

Questo documento scompone in epiche e storie la funzionalità **lista personale unificata
Spesa + Da fare** richiesta dalla specifica v0.14 e dettagliata in
`togo-lista-unificata-tasks-v1.0.md`.

Il documento **non sostituisce** `_bmad-output/planning-artifacts/epics.md` (epiche 1–7,
shopping-only): lo affianca come breakdown della nuova milestone. La numerazione delle
epiche parte da **Epic 8** per continuità storica.

La fonte di verità funzionale è `togo-specifichev0.14.md`. Il dettaglio Tasks è
`togo-lista-unificata-tasks-v1.0.md`. Il piano v0.6 è il contratto operativo. In caso di
conflitto, prevale la specifica; i conflitti rilevati sul repository reale sono elencati
in fondo a questo documento e nel `development-plan.md`.

La milestone è composta da **4 epiche (8–11)** e **24 storie**. Il piano operativo, le fasi
F0–F6, lo schema target e la strategia di migrazione sono in
`_bmad-output/planning-artifacts/togo-tasks-unificata/development-plan.md`.

## Requirements Inventory

### Functional Requirements

- **FR-U01 — Home unica**: la schermata principale mostra un solo feed persistente con le
  righe attive `SHOPPING/TO_BUY` e `TASK/OPEN`, allo stesso livello visivo e funzionale.
- **FR-U02 — Parità di riga**: ogni riga espone un’etichetta/icona accessibile `Spesa` o
  `Da fare`; le azioni rapide (spunta, modifica, elimina) sono disponibili per entrambi i
  tipi senza comandi ambigui.
- **FR-U03 — Contenuto riga Spesa**: nome prodotto, quantità, unità, nota e categoria
  merceologica; gli attributi esistenti (marca, variante, conservazione) restano visibili
  e modificabili.
- **FR-U04 — Contenuto riga Task**: titolo, area/sottocategoria opzionale, priorità,
  scadenza opzionale, nota e badge `Scaduto` quando applicabile.
- **FR-U05 — Filtri di tipo**: filtro `Tutti | Spesa | Da fare`, default `Tutti`; il
  filtro è una preferenza di visualizzazione, non crea liste persistenti e non modifica i
  dati.
- **FR-U06 — Ordinamento unificato**: in `Tutti` le Task `URGENT` vengono prima; poi
  `createdAt DESC`; quindi `kind` e `id` come tie-break deterministici. Una riga Spesa
  senza priorità ha rango normale.
- **FR-U07 — Completati recenti**: sezione collassabile con al massimo 10 righe totali tra
  `PURCHASED` e `COMPLETED`, con badge di stato; non sostituisce la Cronologia.
- **FR-U08 — Comando Aggiungi unico**: un solo comando primario apre un composer con
  scelta esplicita e accessibile `Spesa | Da fare`; default = ultimo tipo usato nella
  sessione, altrimenti `Spesa`.
- **FR-U09 — Cambio tipo senza perdita dati**: cambiare `Spesa/Da fare` non cancella il
  testo già digitato nell’altro form.
- **FR-U10 — Form Spesa**: mantiene autocomplete catalogo, quantità e unità in campi
  separati, categoria e note; i Task non compaiono tra i suggerimenti prodotto.
- **FR-U11 — Form Task**: titolo libero obbligatorio; `Area`, `Sottocategoria`,
  `Priorità`, `Scadenza` e `Note` opzionali; default `NORMAL`, `OPEN`, nessuna data.
- **FR-U12 — Nessuna classificazione silenziosa**: la scelta esplicita `Spesa | Da fare`
  prevale su qualsiasi euristica/NLP; nessun Task creato tramite `CANONICAL_PRODUCT`.
- **FR-U13 — Stato composer**: i campi non salvati sopravvivono a rotazione/ricreazione
  secondo `SavedStateHandle`/Compose; il doppio tap su salva non crea duplicati.
- **FR-U14 — Spunta Spesa**: `TO_BUY → PURCHASED` con timestamp; lo storico acquisti è
  immutabile e `Aggiungi di nuovo` crea/merge una nuova riga attiva senza riaprire la
  riga storica.
- **FR-U15 — Spunta Task**: `OPEN → COMPLETED` con `completedAt` valorizzato dal `Clock`
  iniettato; `Riapri` riporta la **stessa** Task a `OPEN` con `completedAt = null`.
- **FR-U16 — Eliminazione logica**: eliminare una riga attiva porta a `DELETED`;
  l’elemento è recuperabile dalla Cronologia fino al purge.
- **FR-U17 — Ripristino**: una Task `DELETED` torna `OPEN` sulla stessa identità e non si
  fonde con Task omonime; una riga Spesa `DELETED` torna `TO_BUY` o viene assorbita da una
  riga attiva compatibile.
- **FR-U18 — Svuota attivi**: conferma con riepilogo conteggi Spesa + Da fare; porta solo
  `TO_BUY` e `OPEN` a `DELETED` in un’unica transazione; ogni elemento è ripristinabile.
- **FR-U19 — Purge eliminati**: conferma irreversibile; elimina fisicamente solo `DELETED`
  di entrambi i tipi, mai `PURCHASED` o `COMPLETED`; categorie/catalogo restano intatti.
- **FR-U20 — Cronologia unificata**: filtri per tipo (`Tutti | Spesa | Da fare`) e stato
  (`Completati | Eliminati`), con distinzione inequivocabile `PURCHASED` vs `COMPLETED`.
- **FR-U21 — Categorie Task**: area T1 e sottocategoria T2 opzionali, separate dalle
  categorie merceologiche L1/L2/L3; massimo due livelli; nessun T3; l’utente può lasciare
  la categoria vuota senza modali obbligatorie.
- **FR-U22 — Priorità Task**: enum `NORMAL | URGENT`, default `NORMAL`, indipendente da
  categoria e data.
- **FR-U23 — Scadenza Task**: `LocalDate?` opzionale, giorno di calendario locale, salvata
  in ISO `YYYY-MM-DD`; nessuna ora, nessuna notifica; `Scaduto` è solo una label
  descrittiva e non altera priorità/stato.
- **FR-U24 — Validazione Task**: titolo `trim()` non vuoto, massimo 200 code point Unicode,
  nessun carattere di controllo non stampabile; nota massimo 1000; `normalizedTitle`
  lowercase/trim/spazi, **non UNIQUE**.
- **FR-U25 — Invarianti Task**: `completedAt != null` solo se `status = COMPLETED`;
  `statusChangedAt` cambia solo su transizione di stato, non su modifica di titolo/nota.
- **FR-U26 — Transazioni Task**: `addTask`, `updateTask`, `completeTask`, `reopenTask`,
  `deleteTask`, `restoreTask` sono transazionali e idempotenti rispetto allo stato
  corrente; il doppio tap non duplica righe.
- **FR-U27 — Proiezione unificata**: `UnifiedListRepository` combina in memoria due Flow
  Room e mappa a `UnifiedListEntry`; chiavi UI `S:<listItemId>` e `T:<taskId>`; nessuna
  `UNION` fragile su entità eterogenee.
- **FR-U28 — Un solo stato UI**: la schermata principale espone un solo
  `StateFlow<UnifiedListUiState>`; le azioni sono dispatch per `ItemKind`, non per stringhe
  di categoria.
- **FR-U29 — Offline e senza AI**: Tasks non richiede AI, account, rete, permessi
  notifiche o servizi remoti; con AI OFF il comportamento è identico.
- **FR-U30 — Migrazione non distruttiva**: aggiunta di tabelle Tasks e refactor Spesa con
  migrazione Room esplicita `5 → 6`; nessun `fallbackToDestructiveMigration`; dati,
  acquisti, storico, attributi e ID preservati.
- **FR-U31 — Refactor Spesa**: il modello Spesa passa a `ListItem` con stato
  `TO_BUY | PURCHASED | DELETED`, unità libera, nota e storico per evento/aggregato
  compatibile; lo storico esistente e le statistiche restano leggibili.
- **FR-U32 — Separazione catalogo**: `CANONICAL_PRODUCT` e la tassonomia merceologica
  restano invariati; `TASK` non è una L1 e non viene inserito nel catalogo prodotti.
- **FR-U33 — Statistiche separate**: conteggi e suggerimenti d’acquisto leggono solo
  `PURCHASED`; le Task completate non incrementano `purchaseCount` né i suggerimenti.
- **FR-U34 — Accessibilità e stati vuoti**: etichette TalkBack, touch target Material,
  distinzione di tipo, empty state specifico per `Tutti`, `Spesa` e `Da fare`; filtrare non
  deve suggerire che i dati degli altri tipi siano stati cancellati.

### NonFunctional Requirements

- **NFR-U01 — Offline-first**: tutte le funzioni core, incluse Tasks, funzionano senza rete
  e senza AI; nessuna chiamata remota nel dominio Tasks.
- **NFR-U02 — Integrità**: add/merge/restore/clear/purge sono transazionali; nessuna
  duplicazione, nessuna quantità persa, nessuna violazione FK non gestita.
- **NFR-U03 — Prestazioni**: query indicizzate, nessun N+1 evidente nella proiezione
  unificata, UI reattiva su dataset personale di alcune migliaia di righe.
- **NFR-U04 — Testabilità**: `Clock` e `ZoneId` iniettabili; repository/flow sostituibili
  con fake; business logic testabile senza device.
- **NFR-U05 — Migrazione e backup**: migrazioni testate da snapshot v5 e dall’asset v1;
  backup/restore Android verificato; rollback documentato.
- **NFR-U06 — Privacy e sicurezza**: nessuna credenziale o chiamata AI nel percorso Tasks;
  nessun permesso notifiche; nessun dato sensibile nei log.
- **NFR-U07 — Determinismo**: ordinamento, filtri, tie-break e label `Scaduto` sono
  deterministici e testati con clock/timezone fake.
- **NFR-U08 — Accessibilità e localizzazione**: UI italiana, stringhe in resources,
  semantics/label Compose, touch target ≥48dp, font scaling.
- **NFR-U09 — Architettura**: singolo modulo `app`, Kotlin, Compose Material 3, Koin,
  Room, Coroutines/Flow, MVVM con un `StateFlow` per ViewModel.
- **NFR-U10 — Nessun framework aggiuntivo**: niente NavHost, Hilt, event bus, CQRS,
  backend o sync per questa funzionalità.
- **NFR-U11 — Compatibilità**: il repository è oggi `minSdk 24` / `targetSdk 35`; la
  specifica indica `26/36`. L’allineamento è una decisione di release separata, non un
  prerequisito del refactor Tasks.
- **NFR-U12 — Volume dati**: la home limita i completati recenti a 10; la Cronologia
  contiene lo storico completo; il purge mantiene il DB bounded.

### Additional Requirements

- **AR-U01 — Baseline DB reale**: `togo.db` è alla versione 5, con `SHOPPING_ITEM`,
  `HISTORICAL_ITEM`, `CANONICAL_PRODUCT`, tassonomia L1/L2/L3, `SYNONYM`, `LEARNED_RULE`,
  `USER_ATTRIBUTE`. L’asset `catalog.db` è alla `user_version = 1` e contiene 748 prodotti
  canonici v2; un’installazione nuova esegue le migrazioni `1→6`.
- **AR-U02 — Schema export**: abilitare `exportSchema = true` e `room.schemaLocation`;
  generare e versionare il baseline `5.json` **prima** di introdurre la v6.
- **AR-U03 — Migrazione esplicita**: rimuovere `fallbackToDestructiveMigration()` prima
  della v6; aggiungere `Migration(5,6)` testata; nessun reset del DB utente.
- **AR-U04 — Nuova tabella `LIST_ITEM`**: stato, unità testuale, nota, attributi legacy,
  `purchaseCount` per preservare lo storico aggregato; indici su prodotto, stato,
  `(productId,status)`, `createdAt`, `statusChangedAt`.
- **AR-U05 — Identità**: mantenere `String`/UUID per le righe Spesa esistenti; `Long`
  autogenerato per le Task; chiavi UI prefissate `S:`/`T:` per evitare collisioni.
- **AR-U06 — Tabelle Task**: `TODO_TASK` con titolo, `normalizedTitle` non unico,
  `categoryId?`, priorità, `dueDate` TEXT ISO, nota, stato, timestamp, `completedAt`;
  `TASK_CATEGORY` con ID stabili, `parentId?`, nome, `sortOrder`, massimo due livelli.
- **AR-U07 — Seed categorie Task**: aree `Casa`, `Commissioni`, `Personale`, `Lavoro`,
  `Altro` e sottocategorie indicate nella specifica; nessuna categoria personalizzata in
  v1; nessun T3.
- **AR-U08 — Date e timezone**: converter `LocalDate` ISO; `Clock`/`ZoneId` iniettati;
  cambio timezone non modifica `dueDate`.
- **AR-U09 — Clock**: introdurre un’interfaccia `Clock` e usarla nelle nuove transizioni;
  sostituire progressivamente `System.currentTimeMillis()` nel codice toccato.
- **AR-U10 — Proiezione senza UNION**: `UnifiedListRepository` usa `combine` di Flow e
  mapping in memoria; le query di dettaglio usano JOIN/proiezioni batch per evitare N+1.
- **AR-U11 — Transazioni cross-dominio**: `clearActive` e `purgeDeleted` usano
  `RoomDatabase.withTransaction` sulle due tabelle nello stesso DB.
- **AR-U12 — Koin, non Hilt**: il repository usa Koin (ARCH-7 e codice reale). Il piano
  v0.6 cita Hilt; la divergenza è accettata per non riscrivere la DI esistente.
- **AR-U13 — Singolo modulo**: nessuna estrazione multi-modulo.
- **AR-U14 — Backup Android**: configurare `fullBackupContent`/`dataExtractionRules`,
  escludere segreti/cache, testare backup e restore con dati pre-upgrade.
- **AR-U15 — Strategia test**: il progetto usa test JVM/Robolectric e `sqlite-jdbc`, non
  ha suite `androidTest`; i test di migrazione possono usare Robolectric o JDBC, i test
  Compose restano JVM/Robolectric. I test su device restano manuali.
- **AR-U16 — Documentazione**: creare `docs/implementation-status.md` e
  `docs/tasks-migration-dry-run.md`; aggiornarli a ogni fase.
- **AR-U17 — Catalogo già migrato**: la migrazione catalogo v2 (748 canonici) è già
  applicata; non rieseguire la Fase 11 né alterare `catalog.db` per questa funzionalità.
- **AR-U18 — Regressioni shopping**: ricerca catalogo, autocomplete, attributi, voce,
  condivisione, regole apprese e categorie non devono regredire.
- **AR-U19 — Rimozione del flusso “Presi”**: con il refactor completo, la spunta Spesa
  diventa `PURCHASED` immediato; il vecchio staging “Presi” + “Concludi spesa” va ritirato
  o sostituito dai completati recenti, aggiornando i test relativi.
- **AR-U20 — Nessuna notifica/AI**: non introdurre permessi, SDK o servizi per Tasks.

### UX Design Requirements

- **UX-U01 — Feed unico**: lista piatta con righe Spesa e Task allo stesso peso visivo;
  raggruppamenti per tipo solo facoltativi e non unica vista.
- **UX-U02 — Badge tipo**: etichetta/icona `Spesa` o `Da fare` su ogni riga, leggibile e
  annunciata da TalkBack.
- **UX-U03 — Chip filtro**: `Tutti | Spesa | Da fare` sempre visibili, stato selezionato
  chiaro, default `Tutti`.
- **UX-U04 — Badge priorità/scadenza**: `Urgente` e `Scaduto` distinguibili, senza
  modificare automaticamente la priorità.
- **UX-U05 — Completati recenti**: sezione collassabile, massimo 10 righe, badge
  `Acquistato`/`Completato`, link alla Cronologia completa.
- **UX-U06 — Composer**: un solo FAB `Aggiungi`; segmented control `Spesa | Da fare`
  esplicito e accessibile; il tipo selezionato non cancella i campi.
- **UX-U07 — Form Task leggero**: titolo sempre visibile; campi opzionali raggruppati e
  non bloccanti; nessuna modale categorie automatica.
- **UX-U08 — Selettore data**: date picker Material 3, solo giorno, azione “Rimuovi data”,
  nessuna ora/notifica.
- **UX-U09 — Empty state per filtro**: messaggi distinti per `Tutti`, `Spesa`, `Da fare`,
  senza suggerire cancellazioni.
- **UX-U10 — Conferme distruttive**: dialog `Svuota attivi` con conteggi per tipo; dialog
  `Purge eliminati` con testo irreversibile esplicito.
- **UX-U11 — Cronologia**: filtri tipo/stato, etichette `Acquistato`/`Completato`/
  `Eliminato`, azioni `Aggiungi di nuovo`/`Riapri`/`Ripristina`.
- **UX-U12 — Accessibilità**: semantics complete, touch target ≥48dp, focus dopo
  completamento/eliminazione, nessuna informazione affidata al solo colore.
- **UX-U13 — Design system**: usare i token `TogoTheme` esistenti, dark mode inclusa,
  nessuna ombra nuova, coerenza con `DESIGN.md`.
- **UX-U14 — Rotazione/process recreation**: form e filtro conservati; dialog distruttivi
  non eseguono due volte l’azione.
- **UX-U15 — Nessuna seconda lista**: il filtro non deve apparire come una lista separata;
  nessuna bottom tab bar o navigation drawer aggiuntiva.

### FR Coverage Map

| FR | Storie |
|---|---|
| FR-U01 | 9.4 |
| FR-U02 | 9.4 |
| FR-U03 | 9.4 |
| FR-U04 | 9.4 |
| FR-U05 | 9.3, 9.4 |
| FR-U06 | 9.1 |
| FR-U07 | 9.1, 9.4 |
| FR-U08 | 9.5 |
| FR-U09 | 9.5 |
| FR-U10 | 9.6 |
| FR-U11 | 9.5 |
| FR-U12 | 9.5, 9.7 |
| FR-U13 | 9.5 |
| FR-U14 | 10.2 |
| FR-U15 | 10.1 |
| FR-U16 | 10.3 |
| FR-U17 | 10.3 |
| FR-U18 | 10.5 |
| FR-U19 | 10.5 |
| FR-U20 | 10.4 |
| FR-U21 | 8.6, 9.5 |
| FR-U22 | 8.6, 9.5 |
| FR-U23 | 8.6, 9.5 |
| FR-U24 | 8.6 |
| FR-U25 | 8.6 |
| FR-U26 | 8.6 |
| FR-U27 | 9.1, 9.2 |
| FR-U28 | 9.3 |
| FR-U29 | 9.7, 11.3 |
| FR-U30 | 8.4 |
| FR-U31 | 8.3, 8.4, 8.5 |
| FR-U32 | 8.4, 8.6, 9.6 |
| FR-U33 | 8.5, 10.2 |
| FR-U34 | 9.4, 11.1 |

## Epic List

### Epic 8: Fondamenta dati e refactor Spesa (schema v6)

Il database passa alla versione 6 con una migrazione non distruttiva e verificata. La Spesa
adotta il modello `LIST_ITEM` con stati `TO_BUY | PURCHASED | DELETED`, unità libera, nota e
storico acquisti preservato; il dominio Task nasce con tabelle, categorie seed, validazioni e
transizioni proprie. Alla fine dell'epica l'app esistente compila e funziona sul nuovo modello,
senza ancora la home unificata.

**Valore utente:** chi aggiorna non perde lista, acquisti, storico o attributi, e continua a
usare la spesa esattamente come prima mentre il nuovo dominio Task è pronto per la UI.

**FRs coperti:** FR-U21, FR-U22, FR-U23, FR-U24, FR-U25, FR-U26, FR-U30, FR-U31, FR-U32,
FR-U33
**NFRs coperti:** NFR-U02, NFR-U04, NFR-U05, NFR-U09, NFR-U10
**AR coperti:** AR-U01, AR-U02, AR-U03, AR-U04, AR-U05, AR-U06, AR-U07, AR-U08, AR-U09,
AR-U11, AR-U12, AR-U13, AR-U17
**UX coperti:** UX-U13 (token esistenti, nessun nuovo stile)

### Epic 9: Proiezione unificata, home e composer

La schermata principale diventa una sola lista con righe Spesa e Task allo stesso livello,
filtri `Tutti | Spesa | Da fare`, ordinamento deterministico e completati recenti. Il composer
unico permette di scegliere esplicitamente il tipo, conserva i campi non salvati e offre un form
Task leggero e un form Spesa adattato al nuovo modello. La voce resta shopping-only.

**FRs coperti:** FR-U01, FR-U02, FR-U03, FR-U04, FR-U05, FR-U06, FR-U07, FR-U08, FR-U09,
FR-U10, FR-U11, FR-U12, FR-U13, FR-U21, FR-U22, FR-U23, FR-U27, FR-U28, FR-U29, FR-U32,
FR-U34
**NFRs coperti:** NFR-U01, NFR-U03, NFR-U04, NFR-U07, NFR-U08, NFR-U09, NFR-U10, NFR-U11
**AR coperti:** AR-U10, AR-U18, AR-U20
**UX coperti:** UX-U01, UX-U02, UX-U03, UX-U04, UX-U05, UX-U06, UX-U07, UX-U08, UX-U09,
UX-U13, UX-U14, UX-U15

### Epic 10: Ciclo di vita, cronologia, svuota attivi e purge

Spunta, riapertura, eliminazione logica, ripristino, cronologia unificata, svuota attivi e purge
funzionano per entrambi i tipi con semantiche separate e transazioni atomiche. Lo storico
acquisti resta immutabile, le Task completate non alimentano le statistiche e il purge non
tocca mai completati o acquistati.

**FRs coperti:** FR-U14, FR-U15, FR-U16, FR-U17, FR-U18, FR-U19, FR-U20, FR-U33
**NFRs coperti:** NFR-U02, NFR-U03, NFR-U07, NFR-U12
**AR coperti:** AR-U11, AR-U18, AR-U19
**UX coperti:** UX-U05, UX-U10, UX-U11, UX-U12

### Epic 11: Hardening, accessibilità e Definition of Done

Accessibilità TalkBack, performance su dataset realistici, backup/restore, rotazione e process
death, regressione completa della Spesa e documentazione. L'epica chiude la milestone con la
validazione del DoD della specifica e la matrice di test su device.

**FRs coperti:** FR-U29, FR-U34 (chiusura)
**NFRs coperti:** NFR-U01, NFR-U02, NFR-U03, NFR-U04, NFR-U05, NFR-U06, NFR-U07, NFR-U08,
NFR-U09, NFR-U10, NFR-U11, NFR-U12
**AR coperti:** AR-U14, AR-U15, AR-U16, AR-U18, AR-U20
**UX coperti:** UX-U12, UX-U13, UX-U14

---

## Epic 8: Fondamenta dati e refactor Spesa (schema v6)

### Story 8.1: Baseline schema v5 e harness di migrazione

Come developer,
voglio esportare lo schema Room v5 e predisporre un harness di test delle migrazioni,
così che il passaggio alla v6 sia verificabile prima di modificare qualsiasi tabella.

**Requisiti:** FR-U30 (preparazione), AR-U02

**Acceptance Criteria:**

**Given** il database alla versione 5 con `exportSchema = false`
**When** abilito `exportSchema = true` e configuro `room.schemaLocation`
**Then** il build genera `app/schemas/it.togo.app.data.database.TogoDatabase/5.json`
**And** il file viene versionato nel repository
**And** nessuna entità o versione del database viene modificata in questa storia

**Given** lo schema `5.json` e una dipendenza `room-testing`
**When** eseguo un test di apertura di un database v5
**Then** il test apre la versione 5 e verifica le tabelle esistenti
**And** gli schemi sono disponibili come asset dei test Robolectric
**And** il test fallisce in modo esplicito se lo schema manca

**Given** il database v5 con dati di esempio
**When** eseguo la dry run documentata
**Then** `docs/tasks-migration-dry-run.md` riporta conteggi di `SHOPPING_ITEM`,
`HISTORICAL_ITEM`, prodotti canonici e regole apprese
**And** contiene la procedura di backup, la strategia di rollback e l'esito dei test
**And** `.\gradlew.bat testDebugUnitTest` resta verde

**Task tecnici:**
- Aggiungere `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`.
- Aggiungere `room-testing` alla versione Room 2.5.2 e gli schemi agli asset dei test.
- Creare `docs/tasks-migration-dry-run.md` con la fotografia del DB v5.
- Eseguire e riportare i comandi di test/build.

---

### Story 8.2: Clock e ZoneId iniettabili

Come developer,
voglio un `Clock` e un provider di `ZoneId` iniettati via Koin,
così che completamenti, scadenze e label `Scaduto` siano deterministici nei test.

**Requisiti:** FR-U15, FR-U23, FR-U25

**Acceptance Criteria:**

**Given** il dominio senza dipendenze Android
**When** definisco `Clock` (`nowMillis()`, `today(zoneId)`) e `ZoneIdProvider`
**Then** le interfacce vivono in `domain/time` e non importano `android.*`
**And** l'implementazione di sistema usa l'orologio e il fuso del dispositivo
**And** Koin fornisce entrambe le dipendenze ai repository e ai ViewModel

**Given** un `Clock` fake impostato su un istante noto
**When** completo una Task o una riga Spesa
**Then** `statusChangedAt` e `completedAt` usano l'istante del clock
**And** nessun nuovo codice della funzionalità chiama direttamente
`System.currentTimeMillis()`

**Given** una Task con `dueDate = 2026-11-02`
**When** cambio il fuso orario del dispositivo e riapro l'app
**Then** `dueDate` resta `2026-11-02`
**And** la label `Scaduto` viene calcolata con `today(zoneId)` del clock iniettato
**And** il test con fusi diversi produce lo stesso risultato sul dato persistito

**Given** un device API 24 o 25
**When** uso `LocalDate` e `ZoneId` nel dominio Task
**Then** l'app non crasha per classi `java.time` mancanti
**And** il core library desugaring è abilitato con `desugar_jdk_libs`
**And** il test su API 24 è incluso nella matrice di validazione

**Task tecnici:**
- Creare `Clock`, `SystemClock`, `ZoneIdProvider`, `SystemZoneIdProvider`.
- Registrare i binding in `DomainModule`.
- Abilitare `isCoreLibraryDesugaringEnabled` e aggiungere `desugar_jdk_libs` al version
  catalog e alle dipendenze.
- Sostituire l'uso diretto del tempo nel codice toccato.
- Test unit con fake clock e fusi `Europe/Rome` e `UTC`.

---

### Story 8.3: Dominio ListItem, stati e invarianti

Come developer,
voglio un modello `ListItem` con stati espliciti e invarianti validate,
così che la Spesa non dipenda più da `isChecked` e da quantità sentinella.

**Requisiti:** FR-U31

**Acceptance Criteria:**

**Given** una riga Spesa con quantità `0.0`, `NaN`, infinita o negativa
**When** provo a costruire un `ListItem`
**Then** la costruzione fallisce o la factory la normalizza a `1.0` secondo il contratto
**And** una quantità valida è sempre finita e `> 0`

**Given** una riga con unità vuota o composta solo da spazi
**When** la salvo
**Then** la validazione fallisce
**And** l'unità persistita è testo libero con `trim()`
**And** `StandardUnit` resta un helper di parsing/formattazione, non il tipo persistito

**Given** una riga `TO_BUY`
**When** applico una transizione a `PURCHASED`
**Then** `purchaseCount` diventa almeno `1`
**And** `statusChangedAt` viene aggiornato
**And** una seconda transizione da `PURCHASED` viene rifiutata o ignorata in modo idempotente

**Given** una riga `PURCHASED` o `COMPLETED`
**When** provo a eliminarla
**Then** la transizione non è ammessa
**And** il modello espone solo le transizioni `TO_BUY → PURCHASED`, `TO_BUY → DELETED`,
`DELETED → TO_BUY`

**Given** un'entità Room e un modello di dominio
**When** eseguo il mapping nei due sensi
**Then** ID, attributi legacy, nota, stato, `purchaseCount` e timestamp sono preservati
**And** i test coprono tutti i campi

**Task tecnici:**
- Creare `ListItem`, `ListItemStatus`, `ItemKind`.
- Definire factory/validazione e matrice delle transizioni.
- Aggiornare i mapper e i test di dominio.

---

### Story 8.4: Schema v6 e migrazione Room 5 → 6

Come utente esistente,
voglio che l'aggiornamento dell'app preservi lista, acquisti, storico e attributi,
così che il passaggio alla lista unificata non distrugga i miei dati.

**Requisiti:** FR-U30, FR-U31, FR-U32

**Nota di sequenza:** le tabelle Task sono create nella stessa migrazione `5→6` perché Room non
può aggiungere entità a una versione già rilasciata senza cambiare l'identity hash. La Story
8.6 implementa il comportamento, i DAO e i test delle transizioni; la 8.4 possiede solo schema
fisico, migrazione e seed.

**Acceptance Criteria:**

**Given** il database v5 con `SHOPPING_ITEM` e `HISTORICAL_ITEM`
**When** applico la migrazione `5 → 6`
**Then** viene creata `LIST_ITEM` con stato, unità testuale, nota, `purchaseCount`,
`createdAt`, `statusChangedAt`, `updatedAt` e gli indici previsti
**And** ogni riga non spuntata diventa `TO_BUY`
**And** ogni riga spuntata diventa `PURCHASED` con `purchaseCount = 1` e
`statusChangedAt = updatedAt`
**And** ogni aggregato di `HISTORICAL_ITEM` diventa una riga `PURCHASED` con
`id = 'legacy-hist:' || id`, `purchaseCount` preservato e timestamp `purchasedAt`
**And** le quantità `0.0` diventano `1.0` e le unità vuote diventano `PIECE`
**And** `SHOPPING_ITEM` e `HISTORICAL_ITEM` vengono eliminate solo dopo la copia

**Given** la migrazione `5 → 6`
**When** viene eseguita
**Then** crea anche `TASK_CATEGORY` con il seed stabile e `TODO_TASK` con le FK e gli indici
**And** `CANONICAL_PRODUCT`, `TAXONOMY_LEVEL_1/2/3`, `SYNONYM`, `LEARNED_RULE` e
`USER_ATTRIBUTE` restano invariati
**And** `TogoDatabase` è alla versione 6, con `exportSchema = true`
**And** `fallbackToDestructiveMigration()` è rimosso

**Given** un database v5 con una riga attiva e una riga storica con lo stesso ID
**When** applico la migrazione
**Then** non si verifica una violazione di chiave primaria
**And** entrambe le righe sono presenti con ID distinti

**Given** un'installazione nuova dall'asset `catalog.db` (`user_version = 1`)
**When** Room esegue la catena `1 → 2 → 3 → 4 → 5 → 6`
**Then** il database finale è alla versione 6
**And** il catalogo contiene ancora 748 prodotti canonici
**And** il seed delle categorie Task è presente una sola volta

**Given** un database v5 con dati reali
**When** eseguo i test di migrazione
**Then** `MigrationTestHelper` verifica lo schema e i dati attesi
**And** il test copre quantità sentinella, unità vuota, `purchaseCount` e collisione ID
**And** il dry run documenta conteggi pre/post e assenza di perdite

**Task tecnici:**
- Creare `ListItemEntity`, `TodoTaskEntity`, `TaskCategoryEntity`.
- Creare converter espliciti per `ListItemStatus`, `TodoPriority`, `TodoStatus` e `LocalDate`
  e applicarli con `@TypeConverters` su `TogoDatabase`; rimuovere i converter inutilizzati.
- Aggiungere le entità a `TogoDatabase` e portare `version = 6`.
- Implementare `MIGRATION_5_6` con SQL transazionale e seed.
- Rimuovere il fallback distruttivo.
- Aggiungere test di migrazione da v5 e dall'asset v1.

---

### Story 8.5: Repository ListItem e ciclo di vita della Spesa

Come utente,
voglio che la Spesa usi stati espliciti e uno storico immutabile,
così che acquisto, ripristino e statistiche siano coerenti con la nuova lista.

**Requisiti:** FR-U14, FR-U31, FR-U33

**Nota di sequenza:** l'adapter `HistoryRepository` è temporaneo per mantenere compilabile la
UI fino alla Story 8.7; non è una dipendenza da una storia futura, ma un ponte di
compatibilità rimosso nel checkpoint successivo.

**Acceptance Criteria:**

**Given** una riga `TO_BUY`
**When** la spunto
**Then** diventa `PURCHASED` con `purchaseCount = 1` e `statusChangedAt` dal clock
**And** sparisce dagli attivi e compare tra i completati
**And** una seconda spunta non crea duplicati e non cambia i timestamp

**Given** una riga `PURCHASED`
**When** scelgo `Aggiungi di nuovo`
**Then** viene creata una nuova riga `TO_BUY` con gli stessi attributi
**And** se esiste già una riga attiva compatibile, la quantità viene sommata secondo la regola
definita
**And** la riga storica resta `PURCHASED` e immutabile

**Given** righe `PURCHASED` e righe attive
**When** leggo statistiche e suggerimenti d'acquisto
**Then** conteggi e suggerimenti d'acquisto considerano solo `PURCHASED`
**And** `SUM(purchaseCount)` preserva il numero di acquisti legacy
**And** nessuna Task o riga `TO_BUY` incrementa le statistiche
**And** l'autocomplete di marca/variante continua a usare righe attive + `PURCHASED`, come
prima

**Given** una riga `TO_BUY`
**When** la elimino
**Then** diventa `DELETED` e resta recuperabile
**And** il ripristino torna `TO_BUY` oppure fonde la quantità in una riga attiva compatibile
**And** unità diverse non vengono fuse

**Given** il vecchio repository con `checkout()` e il flusso “Presi”
**When** completo il refactor
**Then** `checkout()` e lo staging non esistono più nel repository
**And** `HistoryRepository` è sostituito da un adapter temporaneo su `LIST_ITEM`, rimosso nella
Story 8.7
**And** i test del repository coprono complete, delete, restore, re-add, clear e purge

**Task tecnici:**
- Creare `ListItemDao` e `ListItemRepository`.
- Implementare `RoomListItemRepository` con transazioni.
- Adattare `ShareListUseCase`, voice e suggerimenti attributi.
- Aggiornare i fake e i test esistenti.

---

### Story 8.6: Dominio Task, Room, categorie e transizioni

Come utente,
voglio poter creare e gestire attività con area, priorità e scadenza opzionali,
così che i “Da fare” siano un dominio separato dalla spesa e completamente offline.

**Requisiti:** FR-U15, FR-U21, FR-U22, FR-U23, FR-U24, FR-U25, FR-U26, FR-U32

**Acceptance Criteria:**

**Given** una nuova Task con titolo valido
**When** la salvo senza categoria, priorità o scadenza
**Then** viene creata `OPEN`, `NORMAL`, `categoryId = null`, `dueDate = null`
**And** `createdAt` e `statusChangedAt` usano il clock iniettato
**And** `completedAt` è `null`

**Given** un titolo vuoto, di soli spazi, con più di 200 code point o con caratteri di controllo
**When** provo a salvare
**Then** la validazione fallisce con un messaggio localizzato
**And** una nota con più di 1000 code point fallisce
**And** una nota con `\n` o `\t` è ammessa
**And** `normalizedTitle` è trim/lowercase/spazi collassati e **non** è UNIQUE

**Given** due Task con lo stesso titolo
**When** le salvo entrambe
**Then** esistono due righe distinte
**And** nessun merge automatico avviene per titolo

**Given** una Task `OPEN`
**When** la completo
**Then** diventa `COMPLETED` con `completedAt` e `statusChangedAt` dal clock
**When** la riapro
**Then** la stessa riga torna `OPEN` con `completedAt = null`
**And** l'ID non cambia e non viene creata una nuova riga

**Given** una Task `OPEN`
**When** la elimino
**Then** diventa `DELETED`
**When** la ripristino
**Then** la stessa riga torna `OPEN`
**And** non si fonde con una Task omonima

**Given** una categoria T1 o T2 del seed
**When** la assegno a una Task
**Then** `categoryId` può puntare a T1 o T2, mai a T3
**And** se scelgo solo l'Area, `categoryId` punta alla T1
**And** se elimino una categoria seed, la Task resta con `categoryId = null`
**And** il seed contiene Casa, Commissioni, Personale, Lavoro, Altro e le sottocategorie
previste, con ID stabili

**Given** doppio tap su completa/riapri/elimina/ripristina
**When** le due richieste arrivano in sequenza
**Then** la seconda è idempotente
**And** non vengono creati duplicati né timestamp incoerenti

**Task tecnici:**
- Creare modelli `TodoTask`, `TodoPriority`, `TodoStatus`, `TaskCategory`.
- Creare `TodoTaskDao`, `TaskCategoryDao`, `TodoRepository`.
- Implementare validazione, normalizzazione e transizioni.
- Verificare FK `ON DELETE SET NULL` e massimo due livelli.
- Test Room e unit.

---

### Story 8.7: Adattamento della UI shopping esistente e regressione

Come utente,
voglio che la lista della spesa continui a funzionare dopo il refactor,
così che il passaggio alla lista unificata non introduca regressioni.

**Requisiti:** FR-U14, FR-U31, FR-U33 (regressione)

**Acceptance Criteria:**

**Given** l'app aggiornata alla v6
**When** apro la schermata principale esistente
**Then** vedo solo le righe `TO_BUY` e posso spuntarle, modificarle ed eliminarle
**And** la sezione “Presi” e il pulsante “Concludi spesa” non esistono più
**And** la spunta porta immediatamente a `PURCHASED`

**Given** la cronologia shopping
**When** la apro
**Then** mostra `PURCHASED` e `DELETED` letti da `LIST_ITEM`
**And** `Aggiungi di nuovo` funziona senza riaprire la riga storica
**And** la ricerca per nome/attributi esistente continua a funzionare

**Given** voce, condivisione, autocomplete, attributi e regole apprese
**When** eseguo la suite di regressione shopping
**Then** tutti i test aggiornati sono verdi
**And** nessun Task compare nei suggerimenti prodotto
**And** `HistoryRepository` è rimosso o sostituito definitivamente dall'adapter su
`ListItemRepository`

**Given** un database v5 con dati
**When** avvio la build v6
**Then** l'app si apre senza crash e i dati sono leggibili
**And** non viene usato alcun fallback distruttivo
**And** il dry run è aggiornato con l'esito

**Task tecnici:**
- Adattare `ActiveListViewModel` e `ActiveListScreen` a `ListItemRepository`.
- Rimuovere `checkout`, `checkedItems`, `Presi`, undo checkout.
- Adattare `HistoryViewModel`, `ShareListUseCase` e voice.
- Aggiornare test e fake.

---

## Epic 9: Proiezione unificata, home e composer

### Story 9.1: UnifiedListEntry, ordinamento e filtri puri

Come developer,
voglio una proiezione di dominio pura e deterministica,
così che la home unificata non dipenda dall'ordine di emissione del database.

**Requisiti:** FR-U06, FR-U07, FR-U27

**Acceptance Criteria:**

**Given** una riga Spesa con ID `42` e una Task con ID `42`
**When** costruisco le entry
**Then** le chiavi sono `S:42` e `T:42`
**And** non c'è collisione nella `LazyColumn`

**Given** entry di entrambi i tipi
**When** applico l'ordinamento `Tutti`
**Then** le Task `URGENT` vengono prima
**And** poi si ordina per `createdAt DESC`
**And** poi per `kind` (`SHOPPING` prima di `TASK`)
**And** poi per chiave, con risultato stabile e ripetibile

**Given** il filtro `Spesa`, `Da fare` o `Tutti`
**When** filtro le entry
**Then** il filtro non modifica i dati persistiti
**And** `Tutti` è il default
**And** i conteggi per tipo sono calcolati sull'insieme completo

**Given** più di 10 righe completate
**When** costruisco i completati recenti
**Then** vengono restituite al massimo 10 righe
**And** sono ordinate per `statusChangedAt DESC` e poi per chiave
**And** rispettano il filtro di tipo attivo

**Given** una Task `OPEN` con `dueDate` precedente a oggi e una Task `COMPLETED` scaduta
**When** calcolo la label
**Then** solo la Task `OPEN` riceve `Scaduto`
**And** la label non cambia priorità, stato o ordinamento

**Task tecnici:**
- Creare `UnifiedListEntry`, `UnifiedListFilter`, `UnifiedListOrdering`,
`UnifiedListProjection`.
- Implementare comparatori e filtri come funzioni pure.
- Test unit con clock fake e ID uguali.

---

### Story 9.2: UnifiedListRepository con combine e batch catalogo

Come developer,
voglio comporre i due Flow Room in memoria con una query batch al catalogo,
così che la home unificata sia reattiva e non soffra di N+1.

**Requisiti:** FR-U27

**Acceptance Criteria:**

**Given** `ListItemRepository` e `TodoRepository`
**When** osservo la lista attiva
**Then** `UnifiedListRepository` combina i due Flow e mappa a `UnifiedListEntry`
**And** non usa `UNION` SQL
**And** emette di nuovo quando cambia una riga Spesa o una Task

**Given** 50 righe attive e 10 completati recenti
**When** costruisco la proiezione
**Then** i nomi prodotto sono risolti con una query batch `getByIds(ids)` che include anche i
completati recenti
**And** le categorie merceologiche sono calcolate dalla tassonomia già in cache
**And** il numero di query al catalogo non cresce linearmente con le righe
**And** una lista di ID vuota non esegue query
**And** le Task non vengono cercate nel catalogo prodotti

**Given** una riga Spesa senza prodotto canonico
**When** costruisco l'entry
**Then** viene usato un nome fallback non vuoto
**And** la riga resta visibile e modificabile

**Given** una Task con categoria T1 o T2
**When** costruisco l'entry
**Then** `areaName` e `subcategoryName` sono valorizzati coerentemente
**And** una Task senza categoria ha entrambi `null`

**Given** il repository in test con fake
**When** emetto nuove liste
**Then** i completati recenti rispettano il limite e l'ordine
**And** le entry attive contengono solo `TO_BUY` e `OPEN`
**And** i test verificano l'assenza di N+1

**Task tecnici:**
- Aggiungere `CatalogDao.getByIds(ids)` e `CatalogRepository.getByIds(ids)`.
- Creare `UnifiedListRepository` e `RoomUnifiedListRepository`.
- Registrare il repository in Koin.
- Test con fake e conteggio query.

---

### Story 9.3: UnifiedListViewModel con un solo StateFlow

Come developer,
voglio un ViewModel unico con un solo `StateFlow<UnifiedListUiState>`,
così che la home abbia una fonte di stato coerente e le azioni siano dispatch per tipo.

**Requisiti:** FR-U05, FR-U28

**Acceptance Criteria:**

**Given** la home unificata
**When** osservo lo stato
**Then** esiste un solo `StateFlow<UnifiedListUiState>`
**And** lo stato contiene entry visibili, filtro, conteggi, completati recenti, loading ed
errore
**And** non esistono due flussi paralleli per Spesa e Task nella UI

**Given** il filtro `Spesa`
**When** l'utente seleziona `Da fare`
**Then** cambia solo lo stato di visualizzazione
**And** nessuna scrittura avviene sul database
**And** il filtro sopravvive a rotazione e process recreation tramite `SavedStateHandle`

**Given** un'azione su una entry
**When** il ViewModel la riceve
**Then** il dispatch usa `ItemKind`/chiave, non stringhe di categoria
**And** una chiave sconosciuta viene ignorata e loggata senza crash
**And** le azioni sono idempotenti rispetto allo stato corrente

**Given** emissioni ravvicinate del repository
**When** lo stato viene aggiornato
**Then** non ci sono duplicati o flicker
**And** i campi transitori (snackbar, dialog) non vengono cancellati dalle emissioni Room

**Task tecnici:**
- Creare `UnifiedListUiState` e `UnifiedListViewModel`.
- Gestire filtro, conteggi, recenti e azioni.
- Registrare il ViewModel in Koin.
- Test con fake repository e `SavedStateHandle`.

---

### Story 9.4: Home unica con feed, filtri e completati recenti

Come utente,
voglio vedere Spesa e Da fare nella stessa lista, con filtri chiari e completati recenti,
così che non debba cambiare applicazione o schermata per gestire la mia giornata.

**Requisiti:** FR-U01, FR-U02, FR-U03, FR-U04, FR-U05, FR-U07, FR-U34

**Acceptance Criteria:**

**Given** almeno una riga Spesa e una Task attive
**When** apro “La mia lista”
**Then** vedo un unico feed con entrambe le righe
**And** ogni riga ha un badge accessibile `Spesa` o `Da fare`
**And** le due righe hanno lo stesso peso visivo
**And** non esiste una bottom tab bar o un drawer aggiuntivo

**Given** una riga Spesa
**When** la visualizzo
**Then** mostra nome, quantità, unità, nota e categoria merceologica
**And** marca, variante e conservazione restano visibili e modificabili
**Given** una riga Task
**Then** mostra titolo, area/sottocategoria, priorità, scadenza e nota
**And** mostra `Scaduto` solo se `OPEN` e scaduta

**Given** la home aperta
**When** guardo i filtri
**Then** `Tutti | Spesa | Da fare` sono sempre visibili
**And** `Tutti` è selezionato di default
**And** il filtro attivo è chiaramente distinguibile senza affidarsi al solo colore

**Given** un filtro senza risultati
**When** la lista è vuota
**Then** compare un empty state specifico per `Tutti`, `Spesa` o `Da fare`
**And** il messaggio non suggerisce che i dati degli altri tipi siano stati cancellati
**And** l'empty state include una CTA per aggiungere

**Given** righe `PURCHASED` e `COMPLETED` recenti
**When** espando “Completati recenti”
**Then** vedo al massimo 10 righe totali
**And** ogni riga ha il badge `Acquistato` o `Completato`
**And** c'è un link alla Cronologia completa
**And** la sezione è collassabile e ricorda lo stato nella sessione

**Given** TalkBack attivo
**When** navigo le righe e i filtri
**Then** ogni riga annuncia tipo, contenuto e stato
**And** i touch target sono ≥48dp
**And** il focus è gestito dopo completamento/eliminazione
**And** la UI usa i token `TogoTheme` esistenti, dark mode inclusa

**Task tecnici:**
- Creare `UnifiedListScreen` e i componenti `UnifiedItemRow`, `TypeBadge`,
`FilterChips`, `RecentCompletedSection`, `EmptyState`.
- Collegare `UnifiedListViewModel` e rimuovere la vecchia home shopping-only.
- Test Compose/Robolectric per feed, filtri, empty state e semantics.

---

### Story 9.5: Composer unico con scelta Spesa/Da fare e form Task

Come utente,
voglio scegliere esplicitamente se aggiungere una spesa o un'attività e compilare solo i campi
che mi servono,
così che aggiungere “Lavare casa” richieda pochi secondi e non venga classificato
automaticamente.

**Requisiti:** FR-U08, FR-U09, FR-U11, FR-U12, FR-U13, FR-U21, FR-U22, FR-U23

**Acceptance Criteria:**

**Given** la home
**When** tocco l'unico FAB `Aggiungi`
**Then** si apre un composer con segmented control accessibile `Spesa | Da fare`
**And** il tipo default è l'ultimo usato nella sessione, altrimenti `Spesa`
**And** non esistono due FAB o due entry point separati

**Given** il composer aperto su `Spesa` con testo digitato
**When** passo a `Da fare` e torno a `Spesa`
**Then** il testo e i campi già digitati non vengono cancellati
**And** ogni tipo mantiene il proprio stato locale

**Given** il form `Da fare`
**When** lo apro
**Then** il titolo è l'unico campo obbligatorio e sempre visibile
**And** Area, Sottocategoria, Priorità, Scadenza e Note sono opzionali e non bloccanti
**And** i default sono `NORMAL`, `OPEN`, nessuna data, nessuna categoria
**And** non si apre automaticamente alcuna modale categorie

**Given** il form `Da fare`
**When** scelgo una scadenza
**Then** si apre un date picker Material 3 solo giorno
**And** posso rimuovere la data con un'azione esplicita
**And** non vengono richieste ora o permessi notifiche

**Given** un titolo vuoto, troppo lungo o con caratteri non stampabili
**When** provo a salvare
**Then** il pulsante è disabilitato o mostra un errore localizzato
**And** il focus va sul campo titolo
**And** nessuna Task viene salvata

**Given** un doppio tap su `Salva`
**When** la prima richiesta è in corso
**Then** il pulsante è disabilitato
**And** viene creata una sola Task
**And** un errore del repository ripristina lo stato e mostra un messaggio

**Given** rotazione o process recreation con il composer aperto
**When** la schermata viene ricreata
**Then** tipo selezionato e campi non salvati vengono conservati
**And** il dialog di conferma non esegue due volte l'azione

**Given** una Task `OPEN` esistente
**When** scelgo `Modifica`
**Then** il composer si apre in modalità edit con i campi precompilati
**And** il salvataggio aggiorna la stessa riga e lo stesso ID
**And** `statusChangedAt` non cambia se modifico solo titolo, nota, area, priorità o scadenza
**And** `createdAt` resta invariato

**Given** il form `Da fare`
**When** cerco un prodotto
**Then** i Task non compaiono tra i suggerimenti del catalogo
**And** la scelta esplicita del tipo prevale su qualsiasi euristica o NLP
**And** `comprare latte` resta una Task se ho scelto `Da fare`

**Task tecnici:**
- Estendere `AddItemUiState` e `AddItemViewModel` con `ItemKind`, form Task e validazione.
- Aggiungere segmented control, date picker e campi opzionali in `AddItemBottomSheet`.
- Salvare l'ultimo tipo in `ComposerSessionState` o nello `SavedStateHandle` del parent,
  non nel ViewModel effimero della sheet.
- Conservare lo stato per tipo e gestire il doppio submit.
- Test Compose/Robolectric e unit.

---

### Story 9.6: Form Spesa adattato al modello ListItem

Come utente,
voglio che l'aggiunta e la modifica di una spesa restino veloci e complete,
così che il refactor non peggiori il flusso che uso già.

**Requisiti:** FR-U10, FR-U32

**Acceptance Criteria:**

**Given** il form `Spesa`
**When** cerco un prodotto
**Then** l'autocomplete del catalogo e la ricerca per categoria funzionano come prima
**And** quantità e unità restano campi separati
**And** la quantità default è `1` e non esiste più la sentinella `0.0`
**And** l'unità è testo libero con suggerimenti dalle unità note

**Given** una voce senza nota
**When** la salvo
**Then** la nota è `null`
**And** quantità, unità, marca, variante e conservazione sono persistite
**And** lo stato della riga resta `TO_BUY`

**Given** una voce `TO_BUY` esistente
**When** la modifico
**Then** il form è precompilato e il salvataggio aggiorna la stessa riga
**And** `statusChangedAt` non cambia
**And** `updatedAt` cambia

**Given** una quantità non valida
**When** provo a salvare
**Then** vedo un errore localizzato
**And** la voce non viene salvata
**And** il focus va sul campo quantità

**Given** la ricerca prodotto
**When** digito il nome di una Task o di un'area
**Then** non ottengo risultati dal catalogo Task
**And** i suggerimenti prodotto restano solo `CANONICAL_PRODUCT`

**Task tecnici:**
- Adattare `AddItemViewModel` a `ListItem` e `ListItemRepository`.
- Aggiornare quantità, unità, nota e validazione.
- Aggiornare i test esistenti del form Spesa.

---

### Story 9.7: Regressione voce, condivisione e offline

Come utente,
voglio che voce e condivisione continuino a funzionare solo sulla spesa e che l'app resti
offline,
così che l'arrivo dei Task non introduca comportamenti inattesi.

**Requisiti:** FR-U12, FR-U29

**Acceptance Criteria:**

**Given** un comando vocale che identifica un prodotto e un'unità compatibile
**When** lo confermo
**Then** viene creata una riga Spesa `TO_BUY`
**And** nessuna Task viene creata o modificata
**And** un comando ambiguo apre il composer precompilato senza salvare

**Given** una lista con righe Spesa e Task
**When** condivido la lista
**Then** il testo condiviso contiene solo le righe Spesa `TO_BUY`
**And** le Task non compaiono
**And** il formato per categoria resta quello esistente

**Given** AI OFF, nessuna rete e nessun provider disponibile
**When** aggiungo, completo, riapro, elimino o ripristino una Task
**Then** il comportamento è identico a quello online
**And** non viene richiesto alcun permesso notifiche
**And** nessuna chiamata remota parte dal dominio Task

**Given** la suite di regressione shopping
**When** eseguo i test dopo l'unificazione
**Then** ricerca, autocomplete, attributi, voce, condivisione e regole apprese sono verdi
**And** i test verificano esplicitamente che i Task non entrano nel catalogo o nella voce

**Task tecnici:**
- Aggiornare `ActiveListViewModel`/`UnifiedListViewModel` per voce e share.
- Aggiornare `ShareListUseCase` a `TO_BUY`.
- Aggiungere test di non-regressione e test offline.

---

## Epic 10: Ciclo di vita, cronologia, svuota attivi e purge

### Story 10.1: Completamento e riapertura delle Task

Come utente,
voglio spuntare una Task e poterla riaprire sulla stessa riga,
così che il completamento sia reversibile senza duplicati.

**Requisiti:** FR-U15

**Acceptance Criteria:**

**Given** una Task `OPEN`
**When** la spunto
**Then** diventa `COMPLETED`
**And** `completedAt` e `statusChangedAt` usano il clock iniettato
**And** sparisce dagli attivi e compare nei completati recenti e nella cronologia
**And** nessuna statistica di acquisto cambia

**Given** una Task `COMPLETED`
**When** scelgo `Riapri`
**Then** la stessa riga torna `OPEN`
**And** `completedAt` diventa `null`
**And** `statusChangedAt` viene aggiornato
**And** l'ID resta invariato

**Given** doppio tap su spunta o riapertura
**When** arrivano due richieste
**Then** la seconda è idempotente
**And** non vengono creati duplicati
**And** la UI non mostra uno stato intermedio incoerente

**Given** un errore del repository durante il completamento
**When** la transazione fallisce
**Then** la Task resta nello stato precedente
**And** la UI mostra un messaggio e ripristina la riga

**Task tecnici:**
- Collegare azioni complete/reopen al `TodoRepository`.
- Gestire optimistic update e rollback.
- Test unit e Compose.

---

### Story 10.2: Completamento Spesa e “Aggiungi di nuovo”

Come utente,
voglio che spuntare una spesa la registri subito come acquistata e poterla riaggiungere,
così che lo storico resti immutabile ma la lista resti facile da riusare.

**Requisiti:** FR-U14, FR-U33

**Acceptance Criteria:**

**Given** una riga `TO_BUY`
**When** la spunto
**Then** diventa `PURCHASED` immediatamente
**And** `purchaseCount = 1` e `statusChangedAt` dal clock
**And** sparisce dagli attivi e compare nei completati recenti e nella cronologia
**And** non esiste più il passaggio “Presi” o “Concludi spesa”

**Given** una riga `PURCHASED`
**When** scelgo `Aggiungi di nuovo`
**Then** viene creata una nuova riga `TO_BUY` con prodotto, quantità, unità e attributi
**And** se esiste una riga attiva compatibile, la quantità viene sommata
**And** la riga `PURCHASED` non viene modificata né riaperta

**Given** una riga `PURCHASED` con `purchaseCount = 7` migrata dallo storico
**When** calcolo le statistiche
**Then** il prodotto pesa 7 acquisti
**And** un nuovo acquisto aggiunge una riga separata con `purchaseCount = 1`
**And** il totale resta corretto

**Given** doppio tap sulla spunta
**When** arrivano due richieste
**Then** la seconda è idempotente
**And** non vengono creati duplicati o acquisti doppi

**Task tecnici:**
- Collegare complete/re-add al `ListItemRepository`.
- Aggiornare statistiche e suggerimenti.
- Test repository, ViewModel e Compose.

---

### Story 10.3: Eliminazione logica, undo e ripristino

Come utente,
voglio eliminare una riga attiva e recuperarla dalla cronologia,
così che un errore non provochi una perdita definitiva.

**Requisiti:** FR-U16, FR-U17

**Acceptance Criteria:**

**Given** una riga Spesa `TO_BUY` o una Task `OPEN`
**When** la elimino
**Then** diventa `DELETED`
**And** sparisce dalla home e compare in Cronologia > Eliminati
**And** posso annullare subito con l'azione `Annulla`

**Given** una Task `DELETED`
**When** scelgo `Ripristina`
**Then** la stessa riga torna `OPEN`
**And** non si fonde con una Task omonima
**And** `completedAt` resta `null`

**Given** una riga Spesa `DELETED`
**When** scelgo `Ripristina`
**Then** torna `TO_BUY`
**And** se esiste una riga attiva compatibile (stesso prodotto, attributi
case-insensitive e stessa unità), le quantità vengono sommate e la riga eliminata viene
rimossa
**And** se più righe sono compatibili, si usa quella con `createdAt` più recente e, a parità,
quella con `id` minore
**And** se l'unità è diversa, la riga viene ripristinata separatamente

**Given** una riga `PURCHASED` o una Task `COMPLETED`
**When** provo a eliminarla
**Then** l'azione non è disponibile
**And** il repository rifiuta la transizione
**And** la cronologia non la sposta tra gli eliminati

**Given** un errore durante l'eliminazione o il ripristino
**When** la transazione fallisce
**Then** lo stato precedente resta invariato
**And** la UI mostra un messaggio

**Task tecnici:**
- Implementare delete/restore su entrambi i repository.
- Implementare la regola di merge al ripristino Spesa.
- Aggiungere snackbar undo con timeout.
- Test unit, Room e Compose.

---

### Story 10.4: Cronologia unificata con filtri e azioni

Come utente,
voglio una cronologia unica con filtri per tipo e stato,
così che acquisti, completamenti ed eliminazioni siano leggibili senza ambiguità.

**Requisiti:** FR-U20

**Acceptance Criteria:**

**Given** righe `PURCHASED`, `COMPLETED` e `DELETED`
**When** apro la Cronologia
**Then** vedo i filtri tipo `Tutti | Spesa | Da fare` e stato `Completati | Eliminati`
**And** `Completati` mostra `PURCHASED` e `COMPLETED`
**And** `Eliminati` mostra solo `DELETED`
**And** le etichette `Acquistato`, `Completato`, `Eliminato` sono inequivocabili

**Given** una riga `PURCHASED`
**When** la visualizzo
**Then** vedo prodotto, quantità, unità, attributi, data e `purchaseCount` se aggregato
**And** l'azione è `Aggiungi di nuovo`

**Given** una Task `COMPLETED`
**When** la visualizzo
**Then** vedo titolo, area, priorità, scadenza e data di completamento
**And** l'azione è `Riapri`

**Given** una riga `DELETED`
**When** la visualizzo
**Then** l'azione è `Ripristina`

**Given** il filtro `Da fare` e stato `Eliminati`
**When** applico i filtri
**Then** vedo solo Task eliminate
**And** i conteggi riflettono il filtro
**And** l'empty state è specifico e non suggerisce cancellazioni

**Given** la ricerca testuale esistente nella cronologia shopping
**When** cerco un nome o un attributo
**Then** la ricerca continua a funzionare
**And** con `Tutti` cerca anche nel titolo delle Task
**And** nessun risultato viene inventato dal catalogo

**Given** TalkBack attivo
**When** navigo la cronologia
**Then** ogni riga annuncia tipo, stato, contenuto e azione
**And** i touch target sono ≥48dp
**And** il focus è gestito dopo ripristino/riapertura

**Task tecnici:**
- Rifattorizzare `HistoryViewModel` e `HistoryScreen` sulla cronologia unificata.
- Aggiungere `UnifiedListRepository.observeHistory(status)` o equivalente.
- Mantenere la ricerca shopping e aggiungere il titolo Task.
- Test Compose/Robolectric e unit.

---

### Story 10.5: Svuota attivi e purge eliminati

Come utente,
voglio svuotare gli attivi o eliminare definitivamente gli eliminati con conferme chiare,
così che le operazioni distruttive siano esplicite e non tocchino lo storico.

**Requisiti:** FR-U18, FR-U19

**Acceptance Criteria:**

**Given** righe `TO_BUY` e `OPEN`
**When** scelgo `Svuota attivi`
**Then** vedo un dialog con i conteggi separati Spesa e Da fare
**And** alla conferma solo `TO_BUY` e `OPEN` diventano `DELETED` in un'unica transazione
**And** ogni elemento è ripristinabile dalla Cronologia
**And** una seconda conferma non elimina due volte

**Given** righe `DELETED` di entrambi i tipi
**When** scelgo `Purge eliminati`
**Then** vedo un dialog con testo irreversibile e il numero di righe
**And** alla conferma vengono eliminate fisicamente solo le righe `DELETED`
**And** `PURCHASED` e `COMPLETED` restano intatti
**And** categorie, catalogo, regole e attributi restano intatti

**Given** un'operazione annullata nel dialog
**When** chiudo il dialog
**Then** nessuna riga cambia stato
**And** nessuna transazione viene avviata

**Given** un errore durante `Svuota attivi` o `Purge`
**When** la transazione fallisce
**Then** il database resta nello stato precedente
**And** la UI mostra un messaggio
**And** i conteggi mostrati non vengono considerati definitivi finché la transazione non riesce

**Given** doppio tap sul pulsante di conferma
**When** la prima transazione è in corso
**Then** il pulsante è disabilitato
**And** l'operazione viene eseguita una sola volta

**Task tecnici:**
- Implementare `clearActive` e `purgeDeleted` con `RoomDatabase.withTransaction`.
- Restituire i conteggi reali per tipo.
- Aggiungere dialog e stato di conferma.
- Test Room, ViewModel e Compose.

---

## Epic 11: Hardening, accessibilità e Definition of Done

### Story 11.1: Accessibilità completa della lista unificata

Come utente con TalkBack o font scaling,
voglio che la lista, il composer e i dialog siano completamente accessibili,
così che la nuova funzionalità sia usabile da tutti.

**Requisiti:** FR-U34

**Acceptance Criteria:**

**Given** TalkBack attivo
**When** navigo una riga Spesa
**Then** sento tipo `Spesa`, nome, quantità, unità, categoria, nota e stato
**When** navigo una riga Task
**Then** sento tipo `Da fare`, titolo, area, priorità, scadenza e stato
**And** il checkbox annuncia `Selezionato`/`Non selezionato`

**Given** i chip filtro
**When** li esploro
**Then** ognuno annuncia nome e stato selezionato
**And** il cambio filtro è annunciato
**And** nessuna informazione è affidata al solo colore

**Given** font scale 200%
**When** apro home, composer e cronologia
**Then** i testi non vengono troncati in modo da perdere informazioni
**And** i touch target restano ≥48dp
**And** i dialog restano utilizzabili

**Given** un'azione di completamento o eliminazione
**When** la riga scompare
**Then** il focus passa a un elemento sensato
**And** la snackbar è annunciata
**And** l'utente può annullare senza perdere il contesto

**Given** dark mode
**When** apro tutte le schermate della funzionalità
**Then** i token `TogoTheme` sono applicati
**And** il contrasto resta adeguato
**And** non vengono introdotte ombre o stili nuovi non previsti

**Task tecnici:**
- Aggiungere semantics, contentDescription, stateDescription e liveRegion.
- Verificare touch target e font scaling.
- Test Compose di accessibilità e checklist manuale TalkBack.

---

### Story 11.2: Performance e query su dataset realistici

Come utente,
voglio che la home resti reattiva anche con migliaia di righe,
così che l'uso quotidiano non degradi nel tempo.

**Requisiti:** NFR-U03

**Acceptance Criteria:**

**Given** 2.000 righe Spesa e 2.000 Task
**When** apro la home
**Then** la schermata si apre senza ANR
**And** lo scrolling è fluido sul device di riferimento
**And** i completati recenti restano limitati a 10

**Given** la proiezione unificata
**When** analizzo le query
**Then** il catalogo viene letto in batch e non con N+1
**And** le query usano gli indici su stato, prodotto, `createdAt`, `statusChangedAt`,
`dueDate`
**And** non ci sono `UNION` sulle entità eterogenee

**Given** emissioni ravvicinate di Room
**When** aggiorno una riga
**Then** la UI non viene ricostruita più volte del necessario
**And** `distinctUntilChanged`/`WhileSubscribed` sono configurati
**And** il filtro non ricalcola query inutili

**Given** la cronologia con molte righe
**When** scorro
**Then** `LazyColumn` mantiene le prestazioni
**And** non vengono caricate immagini o risorse non necessarie
**And** il purge mantiene sotto controllo le righe eliminate

**Task tecnici:**
- Aggiungere test di carico e verifiche di query.
- Controllare piani di esecuzione SQLite e indici.
- Misurare cold start e scrolling su API 24 e 35.

---

### Story 11.3: Resilienza, rotazione, backup e restore

Come utente,
voglio che l'app non perda stato o dati in caso di rotazione, morte del processo o backup,
così che la lista resti affidabile.

**Requisiti:** FR-U29, NFR-U05

**Acceptance Criteria:**

**Given** il composer aperto con campi compilati
**When** ruoto il device o il processo viene ricreato
**Then** tipo, testo, quantità, unità, data e note vengono conservati
**And** il dialog di conferma non riesegue l'azione
**And** il doppio tap non crea duplicati

**Given** l'app in background durante un'operazione
**When** il processo viene ucciso e riavviato
**Then** il database riflette l'ultima transazione completata
**And** non ci sono righe a metà
**And** la home si riallinea ai Flow Room

**Given** backup Android configurato
**When** eseguo backup e restore su un dispositivo con dati v6
**Then** lista, Task, storico e categorie vengono ripristinati
**And** cache e dati non necessari sono esclusi
**And** nessun segreto viene incluso

**Given** un'installazione con dati v5
**When** aggiorno alla v6 e poi eseguo backup/restore
**Then** i dati migrati sono presenti
**And** il dry run documenta l'esito
**And** il rollback è documentato

**Given** AI OFF e device offline
**When** uso tutte le funzioni Task
**Then** non ci sono errori, richieste di rete o permessi notifiche
**And** il comportamento è identico a online

**Task tecnici:**
- Configurare `fullBackupContent` e `dataExtractionRules`.
- Test manuali di rotazione, process death, backup/restore.
- Aggiornare `docs/tasks-migration-dry-run.md`.

---

### Story 11.4: Regressione completa e documentazione

Come team,
voglio una regressione completa e una documentazione aggiornata,
così che la milestone sia verificabile e mantenibile.

**Requisiti:** NFR-U02, NFR-U05, NFR-U08

**Acceptance Criteria:**

**Given** la suite di test del progetto
**When** eseguo `.\test-android.ps1` o `.\gradlew.bat testDebugUnitTest`
**Then** tutti i test passano
**And** i test shopping esistenti sono stati aggiornati, non eliminati senza motivo
**And** i nuovi test coprono Task, proiezione, migrazione, cronologia e clear/purge

**Given** il catalogo preinstallato
**When** eseguo i test di regressione
**Then** i 748 prodotti canonici sono invariati
**And** ricerca, autocomplete, sinonimi, regole apprese e attributi funzionano
**And** nessuna Task è finita in `CANONICAL_PRODUCT` o nella tassonomia merceologica

**Given** la documentazione
**When** apro `docs/implementation-status.md`
**Then** contiene stato per fase, comandi eseguiti, esiti e rischi residui
**And** `docs/tasks-migration-dry-run.md` contiene conteggi, SQL, backup e rollback
**And** `epics.md` e `development-plan.md` riflettono lo stato reale

**Given** il repository
**When** controllo il diff finale
**Then** non ci sono file generati o temporanei indesiderati
**And** `fallbackToDestructiveMigration()` non è presente
**And** `exportSchema = true` e lo schema v6 sono versionati
**And** nessun segreto o dato personale è nei file

**Task tecnici:**
- Eseguire test, lint e build completi.
- Aggiornare la documentazione con output reali.
- Verificare il diff e la pulizia del repository.

---

### Story 11.5: Validazione finale del DoD

Come product owner,
voglio verificare il DoD della specifica su device e con dati reali,
così che la milestone possa essere accettata o rinviata con evidenze.

**Requisiti:** NFR-U01…NFR-U12 (validazione)

**Acceptance Criteria:**

**Given** la build release candidate
**When** eseguo la matrice di test su API 24 e API 35
**Then** home, composer, cronologia, clear e purge funzionano
**And** non ci sono crash o ANR
**And** l'app si installa e aggiorna da v5 senza perdita dati

**Given** un utente con lista Spesa e Task
**When** usa il flusso completo per una giornata
**Then** aggiunge, completa, riapre, elimina e ripristina entrambi i tipi
**And** i completati recenti e la cronologia sono coerenti
**And** le statistiche di acquisto restano corrette

**Given** AI OFF, offline e nessun permesso notifiche
**When** usa tutte le funzioni Task
**Then** il comportamento è identico
**And** nessuna richiesta di permesso o chiamata remota appare

**Given** la checklist DoD della specifica
**When** la percorro punto per punto
**Then** ogni punto ha un'evidenza allegata (test, screenshot, log, dry run)
**And** le limitazioni note sono documentate (nessuna notifica, ricerca unificata DA-1,
SDK 24/35)
**And** la milestone è accettata o rinviata con motivazione esplicita

**Task tecnici:**
- Preparare la matrice device e i dati di test.
- Raccogliere evidenze e aggiornare il documento di stato.
- Fare il sign-off con il product owner.

## Conflitti e decisioni aperte rilevati sul repository

Questi punti non sono silenziosamente ridefiniti: vanno confermati prima di implementare.

1. **Modello Spesa divergente**: il codice reale usa `SHOPPING_ITEM.isChecked` +
   `HISTORICAL_ITEM` aggregato, non `ListItemStatus` con righe `PURCHASED/DELETED`.
   La scelta concordata è **refactor completo allo schema specifica**, con migrazione
   non distruttiva e preservazione dello storico aggregato tramite `purchaseCount`.
2. **Flusso “Presi”**: la specifica tratta la spunta come acquisto immediato; il codice
   ha uno staging “Presi” + “Concludi spesa”. Il refactor completo ritira lo staging.
3. **Unità**: il codice persiste un enum `StandardUnit` (Room salva il `name`, es.
   `PIECE`, non il `code`); la specifica vuole unità libera. Il refactor persiste testo e
   mantiene `StandardUnit` come helper di parsing/UI; i valori legacy restano i `name`.
4. **Quantità opzionale**: il codice usa `0.0` come sentinella “nessuna quantità”; la
   specifica vuole quantità finita `> 0`. Migrazione `0 → 1.0` e UI con default 1.
5. **DI**: piano v0.6/specifica citano Hilt; il repository usa Koin. Si mantiene Koin.
6. **SDK**: specifica `minSdk 26/target 36`; repository `24/35`. Allineamento rinviato a
   una decisione di release separata.
7. **ID**: specifica usa `Long` per `ListItem`; il repository usa UUID `String` per la
   Spesa. Si mantengono UUID + chiavi `S:`/`T:`.
8. **Ricerca unificata**: la specifica parla di “ricerca alla pari”, ma il dettaglio
   Tasks non definisce una ricerca testuale unificata. Ipotesi v1: filtri di tipo + la
   ricerca catalogo esistente; la ricerca full-text unificata è da confermare.
9. **Categorie Task personalizzate**: fuori scope v1; solo seed.
10. **Voce per Tasks**: fuori scope; il canale vocale resta shopping-only e non classifica
    automaticamente il tipo.
11. **`java.time` su `minSdk 24`**: la specifica usa `LocalDate`, ma il repository non ha
    core library desugaring attivo. Si abilita `desugar_jdk_libs` (D15) invece di alzare
    `minSdk`; test obbligatorio su API 24.

## Validazione finale

- **FR:** 34/34 mappati nella tabella di copertura; nessun requisito orfano.
- **NFR:** 12/12 coperti dalle epiche 8–11 e dalla strategia di test.
- **AR:** 20/20 coperti; Koin, singolo modulo `app`, Room, nessuna UNION, migrazione
  esplicita, `Clock` iniettato, tabelle Task separate.
- **UX:** 15/15 coperti; filtri, badge, empty state, composer, cronologia, accessibilità,
  rotazione/process death, nessuna seconda lista.
- **Storie:** 24 storie (8.1–8.7, 9.1–9.7, 10.1–10.5, 11.1–11.5), tutte con acceptance
  criteria Given/When/Then e task tecnici.
- **Dipendenze:** la sequenza è data-first; le due eccezioni documentate sono le tabelle
  Task create nella migrazione atomica `5→6` (Story 8.4) e l'adapter temporaneo
  `HistoryRepository` (Story 8.5, rimosso in 8.7).
- **Placeholder:** nessun `{{...}}`, TODO o TBD residuo nei due artefatti.
- **Codice:** nessuna modifica a codice, database o asset in questa fase; solo documenti di
  pianificazione sotto `_bmad-output/planning-artifacts/togo-tasks-unificata/`.
- **Aperte:** DA-1 ricerca unificata, DA-2 persistenza filtro a freddo, DA-3 eliminazione di
  completati/acquistati, DA-4 SDK 26/36, DA-5 categorie Task personalizzate.
