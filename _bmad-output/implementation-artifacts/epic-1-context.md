# Epic 1 Context: Fondamenta Tecniche e Design System

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Fornire l'architettura applicativa, la persistenza locale e il Design System pronti per l'uso, consentendo la compilazione e l'esecuzione dell'app Android. Al termine di questa epica, il progetto dispone di una struttura Clean Architecture rigorosa, configurazione DI con Koin, database Room con entità core, catalogo curato preinstallato (≥1.400 prodotti) inizializzato da asset senza dipendenze di rete e tema visivo Material 3 "High-Contrast Utility" con supporto Light e Dark mode.

## Stories

- Story 1.1: Setup Progetto Android e Struttura Clean Architecture
- Story 1.2: Configurazione Koin e Moduli DI
- Story 1.3: Database Room e Schema Entità Core
- Story 1.4: Catalogo Preinstallato da Asset
- Story 1.5: Design System Material3 — Token Colore e Tipografia

## Requirements & Constraints

- **Stack Tecnologico:** Android minSdk 24, targetSdk 35 (riferimento test Android 16), Kotlin 2.4.20, AGP 8.8.0, Jetpack Compose BOM 2026.09.00, Room 3.0.1 con KSP (vietato KAPT), Koin 4.2.2, Coroutines 1.9.0.
- **Local-First & Resilienza Offline:** Tutte le funzioni primarie (accesso lista, catalogo, storico) operano al 100% su dati locali SQLite sul dispositivo, senza richiedere connettività né al primo avvio né durante il normale utilizzo.
- **Cold Load Istantaneo:** L'avvio dell'app e la copia del database iniziale da asset devono essere immediati, senza spinner visibili né blocchi del thread principale.
- **Qualità e Integrità del Catalogo Seed:** Il database distribuito negli asset deve contenere almeno 1.000 prodotti canonici alimentari e almeno 400 non alimentari (≥1.400 totali), strutturati su tassonomia gerarchica a 3 livelli con sinonimi associati. Ogni voce deve avere `level3_id` valido, nome canonico generico in italiano e flag `is_user_defined = 0`.
- **Isolamento Architetturale del Dominio:** Il package `domain` deve essere in puro Kotlin, privo di import o riferimenti al framework Android (`android.*`).
- **Contrasto Visivo e Leggibilità:** Rapporto di contrasto testo/sfondo ≥12:1 per il testo primario su sfondo base (superando lo standard WCAG AAA di 7:1). Nessun testo nella lista attiva può scendere sotto i 14sp.

## Technical Decisions

- **Clean Architecture & Unidirectional Data Flow (UDF/MVI):**
  - Tre layer distinti: Presentation (`feature.*`, `ui.*`), Domain (`domain.*`), Data (`data.*`).
  - La direzione delle dipendenze è strettamente verso l'interno (`Presentation` e `Data` dipendono da `Domain`). `Presentation` non accede mai direttamente a `Data`.
  - Lo stato delle schermate è modellato tramite classi immutabili `UiState` emesse via `StateFlow` da `ViewModel`, con interazioni espresse da `UiEvent`.
- **Single Source of Truth (SSOT) via Room SQLite:**
  - Persistenza gestita tramite `TogoDatabase` Room con scritture atomiche su `Dispatchers.IO` e flussi di lettura osservabili `Flow<T>`.
  - Tabelle core: `SHOPPING_ITEM` (voci attive della spesa), `HISTORICAL_ITEM` (storico acquisti per riaggiunta), `LEARNED_RULE` (associazioni personalizzate prodotto-categoria).
  - Identificativi: stringhe `UUID` v4 per record mutabili (`ShoppingItem`, `HistoricalItem`, `LearnedRule`); interi a 64 bit (`Long`) per identificatori tassonomici e catalogo precompilato.
  - Date e orari: timestamp in millisecondi Unix epoch (`Long`).
  - Quantità e unità: scalare `Double` e unità tipizzata con enum di dominio `StandardUnit`.
- **Catalogo Precompilato da Asset con User Overlay:**
  - Database SQLite precompilato (`catalog.db`) memorizzato negli `assets` dell'APK e inizializzato al primo avvio tramite `createFromAsset()`.
  - Tabelle catalogo: `TAXONOMY_LEVEL_1`, `TAXONOMY_LEVEL_2`, `TAXONOMY_LEVEL_3`, `CANONICAL_PRODUCT`, `SYNONYM`.
  - Flag `is_user_defined = 0` per il dataset iniziale curato, predisposto per overlay locale delle future modifiche utente senza alterare il catalogo base.
- **Dependency Injection Esplicita con Koin:**
  - Inizializzazione in `TogoApplication` con moduli espliciti separati (`domainModule`, `dataModule`, `presentationModule` / database, repository, use cases).
  - Vietati singleton globali, `companion object` come container o iniezioni manuali non mediate dal grafo Koin.

## UX & Interaction Patterns

- **Stile Visivo "High-Contrast Utility":**
  - Estetica solida, essenziale e ad altissimo contrasto per garantire leggibilità immediata sia sotto illuminazione artificiale da supermercato sia alla luce solare diretta.
  - Nessuna ombreggiatura sfumata o gradiente decorativo ("Flat + Crisp Strokes"): separazione visiva ottenuta tramite bordi solidi (1.5px e 2px) e contrasti netti.
- **Token Colore e Tema Material 3:**
  - **Light Mode:** `surface-base` (`#FFFFFF`), `surface-subtle` (`#F8FAFC`), `surface-card` (`#FFFFFF`), `ink-primary` (`#0F172A`), `ink-secondary` (`#475569`), `ink-muted` (`#94A3B8`), `border-crisp` (`#0F172A`), `badge-bg` (`#0F172A`), `badge-ink` (`#FFFFFF`), `accent-action` (`#0F172A`), `accent-success` (`#15803D`), `accent-highlight` (`#2563EB`).
  - **Dark Mode:** `surface-base-dark` (`#0B0F17`), `surface-card-dark` (`#131B2E`), `ink-primary-dark` (`#F8FAFC`), `ink-secondary-dark` (`#94A3B8`), `border-crisp-dark` (`#475569`), `accent-highlight-dark` (`#60A5FA`).
- **Sistema Tipografico (Roboto / Google Sans):**
  - Scala gerarchica a 5 ruoli: `title-screen` (24sp SemiBold), `section-header` (16sp Bold), `item-name` (16sp Medium), `item-meta` (14sp SemiBold), `caption` (12sp Regular).
  - Supporto al ridimensionamento dei caratteri di sistema (Dynamic Type) senza troncature o overflow.
- **Single-Surface Navigation:**
  - Interfaccia concentrata sull'unica superficie utile della lista spesa, senza bottom navigation bar o cassetti laterali (drawer).

## Cross-Story Dependencies

- **Sequenza interna a Epic 1:**
  - Story 1.1 costituisce la base di build e package structure necessaria per tutte le altre storie.
  - Story 1.2 configura Koin, che serve a orchestrare le dipendenze fornite da Story 1.3 (Room DAO/Database) e Story 1.4 (Catalog Asset).
  - Story 1.3 definisce le entità Room core e Story 1.4 predispone lo schema e i dati del catalogo.
  - Story 1.5 fornisce il tema e i token visivi necessari a Presentation prima della costruzione dei componenti UI.
- **Abilitazione per Epiche Successive:**
  - Epic 1 è prerequisito bloccante per Epic 2 (schermata Lista Attiva, DAO e Repository di spesa) e Epic 3 (ricerca e match sul catalogo preinstallato).
