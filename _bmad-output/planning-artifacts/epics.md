---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-Togo-2026-09-20/prd.md
  - _bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md
---

# Togo - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for Togo, decomposing the requirements from the PRD, UX Design, and Architecture into implementable stories.

## Requirements Inventory

### Functional Requirements

FR-1:  Creazione voce manuale — L'utente aggiunge una Voce con Famiglia prodotto, Categoria canonica, Quantità e Unità (obbligatorie); attributi opzionali. Correzione/annullamento disponibili prima e dopo il salvataggio.
FR-2:  Visualizzazione e ordinamento — Lista raggruppata per Categoria canonica, ordine standard tassonomia §4.1; prodotti in ordine alfabetico all'interno di ciascuna categoria.
FR-3:  Aggiunta vocale — Comando vocale singolo via RecognizerIntent it-IT. Estrae prodotto, quantità, unità e attributi; chiede chiarimento se mancano dati obbligatori.
FR-4:  Conferma e annullamento vocale — Nessuna modifica senza conferma esplicita; comandi non riconosciuti o interrotti non creano voci parziali.
FR-5:  Assegnazione categoria e regole apprese — Ogni prodotto ha una sola Categoria canonica; correzioni utente generano Regola appresa (solo su conferma esplicita); regole visibili, modificabili, disattivabili.
FR-6:  Normalizzazione prodotto e quantità — Sinonimi risolti verso Prodotto canonico; conversioni autorizzate (kg↔g, l↔ml, etto↔g); unità incompatibili non aggregate; set fisso di unità MVP.
FR-7:  Rilevamento duplicati — Controllo su Prodotto canonico + Categoria canonica; 3 opzioni esclusive (Somma se compatibile, Sostituisci su conferma, Crea voce separata); nessuna unione silenziosa.
FR-8:  Depennamento voce — Spunta manuale che rimuove dall'attivo e archivia nello Storico.
FR-9:  Riaggiunta dallo Storico — Selezione da interfaccia Storico con check duplicati applicato anche al rientro.
FR-10: Condivisione lista — Testo leggibile per categoria (solo prodotti non spuntati) via Android Intent.ACTION_SEND; nessun dato privato incluso.
FR-11: Catalogo iniziale e ricerca — Tassonomia preinstallata, ≥1.000 prodotti alimentari + ≥400 non alimentari; ricerca full-text; funzionale offline.
FR-12: Estensione controllata catalogo — Utente può aggiungere Prodotto canonico su categoria esistente e proporre nuovo Livello 3 (con conferma + check duplicati); estensioni locali nell'MVP.
FR-13: Attributi prodotto — Marca, Conservazione/preparazione, Variante: facoltativi, contestuali per Livello 3, riconoscibili dal vocale, modificabili manualmente.
FR-14: Proposta al Catalogo centrale (evoluzione futura) — Fuori scope MVP; architettura locale con flag is_user_defined.

### NonFunctional Requirements

NFR-1:  Local-first — Il nucleo (lista, categorie, quantità, storico) funziona con dati locali sul dispositivo, senza dipendenze di rete.
NFR-2:  Resilienza — Assenza vocale o integrazioni opzionali non blocca inserimento manuale, consultazione, depennamento e storico.
NFR-3:  Affidabilità dati — Nessuna interpretazione ambigua modifica la lista senza conferma esplicita.
NFR-4:  Usabilità — Percorso di aggiunta in pochi passi; chiarimenti chiedono solo dati mancanti.
NFR-5:  Accessibilità — Ogni informazione ha equivalente testuale; voce non è l'unico modo di usare il prodotto; contrasto ≥7:1 (WCAG AAA); TalkBack supportato.
NFR-6:  Privacy — Dati lista e storico locali nell'MVP; audio non conservato oltre il riconoscimento; testo riconosciuto usato solo per l'operazione corrente.
NFR-7:  Qualità catalogo — Ogni Prodotto canonico ha nome generico, sinonimi controllati, mappatura tassonomica e check duplicati.
NFR-8:  Aggiornabilità — Catalogo aggiornabile senza sovrascrivere correzioni locali utente (flag is_user_defined).
NFR-9:  Contributi futuri — Aggiunte utente non entrano nel Catalogo centrale senza deduplicazione, revisione e tracciamento provenienza.
NFR-10: Target piattaforma — Android, riferimento test Android 16, min SDK API 24.
NFR-11: Performance — Inserimento manuale ≤15s mediana, vocale chiaro ≤10s mediana (escluso lettura conferma).
NFR-12: Precisione categorizzazione — ≥95% assegnazioni corrette su ≥200 utterance italiane.
NFR-13: Precisione deduplicazione — ≥98% precisione sui duplicati esatti; ≥95% recall sui verificati; 0 modifiche non confermate nei test.

### Additional Requirements

ARCH-1: Paradigma Clean Architecture + UDF/MVI — Separazione stretta tra Presentation (Compose+ViewModel), Domain (puro Kotlin, zero android.*), Data (Room+Gateway).
ARCH-2: Dependency Direction — Presentation e Data dipendono solo da Domain; Presentation NON accede direttamente a Data.
ARCH-3: Room SQLite come SSOT — Scritture atomiche via Repository; UI osserva StateFlow immutabili; checkout = transazione atomica.
ARCH-4: Catalog asset pre-packaged — Database SQLite precompilato in assets (createFromAsset()); user additions con flag is_user_defined = 1 in overlay.
ARCH-5: VoiceRecognitionGateway — Interfaccia di astrazione per RecognizerIntent it-IT con fallback tipizzato SpeechResult.Failure → inserimento manuale.
ARCH-6: 4-Stage Input Pipeline obbligatoria — Tokenize → Match → Deduplicate → Confirm Gate; nessun salvataggio silenzioso.
ARCH-7: Koin 4.2.2 per DI — Moduli espliciti (domain, data, presentation); zero singleton globali.
ARCH-8: Stack tecnologico — Kotlin 2.4.20, Compose BOM 2026.09.00, Room 3.0.1 (KSP), Koin 4.2.2, Coroutines 1.9.0.
ARCH-9: Dark Mode — Supporto obbligatorio con token dedicati (surface-base-dark, ink-primary-dark).

### UX Design Requirements

UX-DR1:  Tema Visual "High-Contrast Utility" — Implementare il tema Material3 con palette token completa: surface-base #FFFFFF, ink-primary #0F172A, border-crisp #0F172A, badge-bg #0F172A, accent-success #15803D, accent-highlight #2563EB (+ dark mode).
UX-DR2:  Sistema tipografico Material3 — Roboto/Google Sans con 5 ruoli semantici (title-screen 24sp, section-header 16sp Bold, item-name 16sp, item-meta 14sp SemiBold, caption 12sp). Minimo 14sp in lista attiva.
UX-DR3:  Componente ItemRow — 3 zone (checkbox 48dp | nome + attributo | badge quantità), bordo 1.5px border-crisp, raggio 8px, stati Non-Preso/Preso (barrato + opacità 45% + ink-muted).
UX-DR4:  Componente CategoryHeader — Fascia con bordo sinistro 4px border-crisp, titolo uppercase section-header, badge numerico articoli rimanenti.
UX-DR5:  Componente VoiceFAB — Cerchio 64dp nero (accent-action), bordo 2px, floating action button; apertura VoiceBottomSheet.
UX-DR6:  VoiceBottomSheet — Bottom sheet 45% schermo: animazione onda (accent-highlight), trascrizione real-time, card strutturata (nome|quantità|categoria + icona matita), pulsante "Conferma e aggiungi" + "Annulla".
UX-DR7:  DuplicateResolutionDialog — Modale centrata: messaggio esplicito con quantità esistente, 3 pulsanti (Somma primario nero, Sostituisci bordato, Crea voce separata bordato) + Annulla testuale. "Somma" disabilitata se unità incompatibili con nota.
UX-DR8:  Sezione Collassabile "Presi (N)" — In fondo alla lista attiva, default espansa; pulsante sticky "Concludi spesa" in calce; chevron per collasso.
UX-DR9:  Schermata Storico — Ricerca rapida full-text + lista prodotti con pulsante "+ Aggiungi" per ogni riga (re-add con check duplicati).
UX-DR10: Gestione Regole Apprese — Schermata lista regole (espressione, prodotto, categoria, data); ogni regola modificabile, disattivabile, eliminabile.
UX-DR11: Condivisione testuale formattata — Testo con emoji per categoria (🥦 ORTOFRUTTA, 🧀 BANCO FRIGO, ecc.), una riga per voce con quantità e unità; apertura Android Sharesheet.
UX-DR12: Target touch 48×48dp — Obbligatorio su tutti gli elementi interattivi (checkbox, pulsanti, chip).
UX-DR13: Haptic feedback — Vibrazione secca 15ms su: toggle checkbox, conferma aggiunta vocale.
UX-DR14: Accessibilità TalkBack — Ogni ItemRow con etichetta semantica completa (nome, quantità, categoria, stato); sezione Presi annuncia stato espansa/collassata.
UX-DR15: Dynamic Type — Testi scalano con impostazioni di sistema Android senza troncature.
UX-DR16: Dark Mode UI — Tutti i componenti usano token dark (surface-base-dark, ink-primary-dark, border-crisp-dark, accent-highlight-dark).
UX-DR17: Single-Surface Navigation — Nessuna bottom tab bar; funzioni secondarie da App Bar; profondità modale massima 1 livello.
UX-DR18: Flat + Crisp Strokes Elevation — Zero ombre sfumate; profondità tramite bordi solidi; modali con backdrop rgba(15,23,42,0.6).
UX-DR19: Cold Load istantaneo — Lista caricata da SQLite locale senza spinner percettibili all'avvio.
UX-DR20: Stati vuoti — Empty state lista con testo guida e CTA; empty state storico con messaggio neutro.
UX-DR21: Offline mode trasparente — Nessun banner per funzioni locali offline; notifica graceful se RecognizerIntent non disponibile.
UX-DR22: Ambiguità vocale — Bottom sheet evidenzia solo il campo mancante con selettore immediato senza rigettare l'intera frase.
UX-DR23: Wizard prodotto sconosciuto — Mini-wizard a 3 tocchi (L1 → L2 → L3) per categorizzare prodotti fuori catalogo prima del salvataggio.
UX-DR24: Ordinamento categorie da tassonomia — Ordine corsie in lista riflette percorso fisico supermercato (Ortofrutta → Banco Frigo → Dispensa…).

### FR Coverage Map

FR-1:  Epic 2 — Inserimento manuale voce con prodotto/quantità/unità/attributi
FR-2:  Epic 2 — Visualizzazione lista raggruppata per categoria in ordine tassonomia
FR-3:  Epic 4 — Aggiunta vocale via RecognizerIntent it-IT
FR-4:  Epic 4 — Conferma e annullamento vocale
FR-5:  Epic 3 + 5 — Assegnazione categoria (Epic 3), regole apprese gestione (Epic 5)
FR-6:  Epic 3 — Normalizzazione prodotto, quantità e unità; conversioni autorizzate
FR-7:  Epic 3 — Rilevamento duplicati e 3 scelte esclusive
FR-8:  Epic 2 — Depennamento voce e transazione atomica checkout→storico
FR-9:  Epic 5 — Riaggiunta dallo storico con check duplicati
FR-10: Epic 5 — Condivisione lista testuale formattata via Android Sharesheet
FR-11: Epic 1 + 3 — Catalogo preinstallato (Epic 1), ricerca full-text nel catalogo (Epic 3)
FR-12: Epic 7 — Estensione controllata catalogo (user-defined, locale)
FR-13: Epic 3 — Attributi prodotto contestuali (marca, variante, conservazione)
FR-14: Deferred — Fuori scope MVP

## Epic List

### Epic 1: Fondamenta Tecniche e Design System
L'app è compilabile e installabile: struttura Clean Architecture attiva, database Room configurato, catalogo preinstallato caricato da asset, Design System Material3 "High-Contrast Utility" implementato con tutti i token colore e tipografici, e moduli Koin configurati. È la base stabile su cui ogni epic successivo si costruisce senza più toccare i file di infrastruttura.
**FRs coperti:** FR-11 (seed catalogo), NFR-1, NFR-10
**Arch coperti:** ARCH-1, ARCH-2, ARCH-3, ARCH-4, ARCH-7, ARCH-8
**UX coperti:** UX-DR1, UX-DR2, UX-DR17, UX-DR18, UX-DR19

### Epic 2: Lista Attiva — Visualizzazione e Gestione Base
L'utente può vedere la lista della spesa raggruppata per categoria (ordine corsie supermercato), inserire prodotti manualmente con prodotto/quantità/unità/attributi, modificare e rimuovere voci, spuntare prodotti acquistati con la sezione "Presi (N)" collassabile, e concludere la spesa con archiviazione atomica nello storico.
**FRs coperti:** FR-1, FR-2, FR-8
**UX coperti:** UX-DR3, UX-DR4, UX-DR8, UX-DR12, UX-DR13, UX-DR17, UX-DR20, UX-DR24

### Epic 3: Catalogo, Normalizzazione e Intelligenza di Inserimento
L'utente trova il prodotto giusto cercando nel catalogo (1.400+ prodotti con sinonimi), l'app normalizza quantità e unità, rileva duplicati potenziali proponendo le 3 scelte esclusive, assegna la categoria canonica corretta e permette la correzione con apprendimento di regole opzionale. Attributi prodotto contestuali (marca, variante, conservazione) riconoscibili e modificabili.
**FRs coperti:** FR-5, FR-6, FR-7, FR-11 (ricerca), FR-13
**Arch coperti:** ARCH-6
**UX coperti:** UX-DR7, UX-DR22, UX-DR23, UX-DR24

### Epic 4: Inserimento Vocale
L'utente aggiunge prodotti con la voce (RecognizerIntent it-IT): TOGO interpreta il comando, mostra la scomposizione strutturata nel VoiceBottomSheet e chiede conferma prima di salvare. Se il servizio vocale non è disponibile il fallback all'inserimento manuale è immediato e non distruttivo.
**FRs coperti:** FR-3, FR-4
**Arch coperti:** ARCH-5
**UX coperti:** UX-DR5, UX-DR6, UX-DR13, UX-DR21, UX-DR22

### Epic 5: Storico, Regole Apprese e Condivisione
L'utente consulta lo storico prodotti acquistati, riaggiunge voci in un tocco (con check duplicati), gestisce le regole apprese (visualizzare, modificare, disattivare, eliminare), e condivide la lista attiva come testo formattato per categoria via Android Sharesheet.
**FRs coperti:** FR-5 (gestione regole), FR-9, FR-10
**UX coperti:** UX-DR9, UX-DR10, UX-DR11

### Epic 6: Accessibilità, Dark Mode e Qualità Trasversale
L'app supera le soglie di accessibilità (contrasto ≥7:1 WCAG AAA, etichette TalkBack complete su ogni riga prodotto e sezione, Dynamic Type senza troncature, target touch 48dp ovunque), supporta Dark Mode completa con token dedicati, e rispetta tutti i requisiti di privacy (nessuna registrazione audio conservata).
**NFRs coperti:** NFR-5, NFR-6, NFR-11, NFR-12, NFR-13
**UX coperti:** UX-DR14, UX-DR15, UX-DR16, UX-DR20

### Epic 7: Estensione Controllata del Catalogo
L'utente può aggiungere un nuovo Prodotto canonico su una categoria esistente o proporre un nuovo Livello 3 con conferma esplicita e check duplicati. Tutte le aggiunte restano locali con flag is_user_defined = 1 nell'overlay, senza sovrascrivere il catalogo curato base.
**FRs coperti:** FR-12
**Arch coperti:** ARCH-4 (overlay)

---

## Epic 1: Fondamenta Tecniche e Design System

L'app è compilabile e installabile con struttura Clean Architecture, Room + catalogo pre-packaged, Design System Material3 "High-Contrast Utility" e moduli DI Koin pronti. Base stabile su cui si costruiscono tutte le epiche successive.

### Story 1.1: Setup Progetto Android e Struttura Clean Architecture

Come developer,
voglio un progetto Android configurato con Kotlin 2.4.20, AGP 8.8.0 e la struttura di package Clean Architecture,
così che ogni contributo futuro abbia un punto di partenza coerente e compilabile.

**Acceptance Criteria:**

**Given** un nuovo progetto Android vuoto
**When** il progetto viene sincronizzato e compilato
**Then** il build Gradle completa senza errori con minSdk 24, targetSdk 35, Kotlin 2.4.20 e AGP 8.8.0
**And** esistono i package `domain`, `data`, `feature`, `ui`, `di` sotto `it.togo.app`
**And** il package `domain` non contiene alcun import `android.*`
**And** l'app si installa e avvia su un emulatore/dispositivo Android API 24+

---

### Story 1.2: Configurazione Koin e Moduli DI

Come developer,
voglio Koin 4.2.2 configurato con moduli espliciti per domain, data e presentation,
così che ogni dipendenza sia risolvibile tramite DI senza singleton globali.

**Acceptance Criteria:**

**Given** il progetto con la struttura di package della Story 1.1
**When** l'app si avvia
**Then** Koin è inizializzato in `TogoApplication` con almeno i moduli `domainModule`, `dataModule`, `presentationModule`
**And** un ViewModel di test riceve le sue dipendenze via Koin senza eccezioni runtime
**And** nessuna classe usa companion object o object globale come contenitore di dipendenze

---

### Story 1.3: Database Room e Schema Entità Core

Come developer,
voglio il database Room 3.0.1 configurato con le entità core (ShoppingItem, HistoricalItem, LearnedRule),
così che la persistenza locale delle funzionalità di lista, storico e regole sia pronta per le epiche funzionali.

**Acceptance Criteria:**

**Given** il progetto con Koin configurato
**When** `TogoDatabase` viene costruito al primo avvio
**Then** il database SQLite è creato localmente senza errori con le tabelle `SHOPPING_ITEM`, `HISTORICAL_ITEM`, `LEARNED_RULE`
**And** ogni DAO (ShoppingItemDao, HistoryDao, LearnedRulesDao) espone i metodi CRUD base e compila correttamente
**And** il database usa KSP (non KAPT) per la generazione del codice Room
**And** un test di integrazione JVM verifica inserimento e lettura su `ShoppingItemDao`

---

### Story 1.4: Catalogo Preinstallato da Asset

Come utente,
voglio che l'app contenga già un catalogo completo di prodotti al primo avvio, senza dover scaricare nulla,
così che possa cercare e aggiungere prodotti anche offline e immediatamente.

**Acceptance Criteria:**

**Given** l'app installata per la prima volta su un dispositivo senza connessione di rete
**When** l'app viene avviata
**Then** il database del catalogo (`catalog.db`) viene copiato dagli asset tramite `createFromAsset()` senza errori
**And** le tabelle `TAXONOMY_LEVEL_1`, `TAXONOMY_LEVEL_2`, `TAXONOMY_LEVEL_3`, `CANONICAL_PRODUCT`, `SYNONYM` sono popolate
**And** il catalogo contiene ≥1.000 prodotti canonici alimentari e ≥400 non alimentari, verificabile via query COUNT
**And** ogni prodotto ha `level3_id` non nullo e nome canonico non vuoto
**And** il flag `is_user_defined = 0` è impostato per tutti i prodotti seed
**And** l'avvio dell'app non mostra spinner percettibili legati al caricamento del catalogo (cold load istantaneo)

---

### Story 1.5: Design System Material3 — Token Colore e Tipografia

Come utente,
voglio che l'intera app usi lo stile visivo "High-Contrast Utility" con colori ad alto contrasto e tipografia Roboto leggibile,
così che la lista sia scansionabile a colpo d'occhio anche sotto la luce intensa del supermercato.

**Acceptance Criteria:**

**Given** l'app avviata in modalità light
**When** qualsiasi schermata viene visualizzata
**Then** il tema Material3 `TogoTheme` è applicato globalmente con i token: `surface-base #FFFFFF`, `ink-primary #0F172A`, `border-crisp #0F172A`, `badge-bg #0F172A`, `accent-success #15803D`, `accent-highlight #2563EB`
**And** la tipografia segue 5 ruoli: title-screen 24sp SemiBold, section-header 16sp Bold, item-name 16sp Medium, item-meta 14sp SemiBold, caption 12sp Regular
**And** nessun testo nella lista attiva scende sotto 14sp
**And** il rapporto di contrasto testo/sfondo per `ink-primary` su `surface-base` è ≥12:1
**And** in modalità dark i token `surface-base-dark #0B0F17` e `ink-primary-dark #F8FAFC` sono applicati a tutti i componenti

---

## Epic 2: Lista Attiva — Visualizzazione e Gestione Base

L'utente può vedere la lista raggruppata per categoria (ordine corsie supermercato), inserire prodotti manualmente, modificarli, spuntarli e concludere la spesa con archiviazione atomica nello storico.

### Story 2.1: Schermata Lista Attiva con Visualizzazione per Categoria

Come utente,
voglio vedere la lista della spesa raggruppata per categoria nell'ordine delle corsie del supermercato,
così che durante la spesa possa scorrere la lista seguendo il percorso naturale del negozio.

**Acceptance Criteria:**

**Given** l'app avviata con almeno una voce nella lista attiva
**When** la schermata principale si apre
**Then** le categorie sono visualizzate nell'ordine della tassonomia (Ortofrutta → Panetteria → Carne → … → Banco Frigo → Dispensa → Non alimentare)
**And** ogni categoria mostra un `CategoryHeader` con bordo sinistro 4px `border-crisp`, titolo uppercase e badge con conteggio articoli rimanenti
**And** all'interno di ogni categoria i prodotti sono ordinati alfabeticamente per nome canonico
**And** non esiste una bottom tab bar o navigation drawer; le funzioni secondarie (Storico, Condivisione, Regole) sono nell'App Bar
**And** se la lista è vuota compare l'empty state: "La tua lista è vuota. Tocca il microfono o premi + per aggiungere prodotti." con CTA visibili

---

### Story 2.2: Inserimento Manuale Prodotto

Come utente,
voglio aggiungere un prodotto alla lista manualmente indicando nome, quantità, unità e categoria,
così che possa costruire la lista quando non posso usare la voce o voglio maggiore controllo.

**Acceptance Criteria:**

**Given** la schermata lista attiva
**When** l'utente tocca il pulsante "+ Aggiungi"
**Then** si apre un bottom sheet/form con campi: campo di testo per il nome prodotto, selettore quantità numerica, selettore unità (grammo, kg, ml, litro, pezzo, confezione, pacco, scatola, bottiglia, barattolo, flacone, rotolo, vasetto), campo categoria (autocompletamento da tassonomia)
**And** quantità e unità sono obbligatorie: il pulsante "Salva" è disabilitato finché non sono compilate
**And** l'utente può correggere qualsiasi campo prima di salvare
**And** premendo "Annulla" o tornando indietro la lista rimane invariata
**And** dopo il salvataggio la voce appare nella categoria corretta della lista attiva
**And** ogni elemento interattivo del form ha area di tocco ≥48×48dp

---

### Story 2.3: Modifica e Rimozione Voce dalla Lista

Come utente,
voglio poter modificare o rimuovere un prodotto già inserito nella lista,
così che possa correggere errori o cambiare quantità senza dover cancellare e reinserire.

**Acceptance Criteria:**

**Given** una voce presente nella lista attiva
**When** l'utente tocca il testo del prodotto nella riga
**Then** si apre il form di modifica precompilato con i dati correnti (nome, quantità, unità, categoria, attributi)
**And** l'utente può modificare qualsiasi campo e salvare
**And** dopo il salvataggio la voce aggiornata appare nella lista nella posizione corretta per categoria e ordine alfabetico

**Given** una voce presente nella lista attiva
**When** l'utente esegue uno swipe verso sinistra sulla riga del prodotto
**Then** compare il pulsante rapido "Elimina"
**And** premendo "Elimina" la voce viene rimossa dalla lista in modo definitivo
**And** l'eliminazione non crea una voce nello storico

---

### Story 2.4: Spunta Prodotto Acquistato e Sezione "Presi (N)"

Come utente,
voglio spuntare i prodotti che metto nel carrello durante la spesa,
così che veda sempre chiaramente cosa mi resta da prendere senza confondere comprato e da comprare.

**Acceptance Criteria:**

**Given** una voce attiva (non spuntata) nella lista
**When** l'utente tocca la checkbox quadrata del prodotto
**Then** il dispositivo emette un haptic feedback (vibrazione secca 15ms)
**And** la riga passa allo stato "Preso": testo barrato, opacità 45%, colore `ink-muted`, checkbox riempita verde/nera
**And** dopo 300ms la riga scivola nella sezione collassabile "Presi (N)" in fondo alla lista
**And** il contatore N nella sezione "Presi" si aggiorna

**Given** la sezione "Presi (N)" con almeno un articolo
**When** l'utente tocca nuovamente la checkbox di un articolo nella sezione Presi
**Then** il prodotto torna istantaneamente nella sua categoria d'origine nello stato attivo (non spuntato)

**Given** la sezione "Presi (N)" espansa
**When** l'utente tocca il chevron di intestazione
**Then** la sezione si collassa mostrando solo l'intestazione con il contatore
**And** un secondo tocco la espande di nuovo

---

### Story 2.5: Conclusione Spesa — Checkout Atomico verso Storico

Come utente,
voglio concludere la spesa spostando nello storico tutti i prodotti che ho preso,
così che la lista si ripulisca e i prodotti acquistati siano registrati per un uso futuro.

**Acceptance Criteria:**

**Given** la sezione "Presi (N)" con almeno un articolo spuntato
**When** l'utente tocca il pulsante "Concludi spesa" in fondo alla sezione Presi
**Then** compare un dialog di conferma: "Concludere la spesa? I N articoli presi saranno spostati nello storico." con pulsanti "Concludi spesa" e "Annulla"

**Given** l'utente conferma "Concludi spesa"
**When** la transazione viene eseguita
**Then** tutti gli articoli con `is_checked = 1` vengono inseriti nella tabella `HISTORICAL_ITEM` (upsert con incremento `purchase_count`) in una singola transazione Room atomica
**And** gli stessi articoli vengono rimossi da `SHOPPING_ITEM`
**And** gli articoli non spuntati rimangono intatti nella lista attiva
**And** se la transazione fallisce (errore DB) la lista rimane nello stato precedente senza dati parzialmente modificati

---

## Epic 3: Catalogo, Normalizzazione e Intelligenza di Inserimento

L'utente trova il prodotto giusto nel catalogo, l'app normalizza quantità/unità, rileva duplicati e assegna la categoria canonica corretta. Include attributi prodotto contestuali e apprendimento di regole su correzione esplicita.

### Story 3.1: Ricerca Full-Text nel Catalogo

Come utente,
voglio cercare un prodotto digitando il suo nome (anche parziale o con un sinonimo),
così che trovi rapidamente il prodotto canonico giusto senza scorrere l'intera tassonomia.

**Acceptance Criteria:**

**Given** il campo di testo nome prodotto nel form di inserimento
**When** l'utente digita almeno 2 caratteri
**Then** compare un elenco di suggerimenti filtrati che corrispondono al nome canonico o ai sinonimi del catalogo
**And** la ricerca funziona completamente offline senza chiamate di rete
**And** i risultati mostrano nome canonico + path categoria (es. "Latte parzialmente scremato — Banco Frigo / Latte")
**And** selezionando un suggerimento i campi categoria vengono precompilati automaticamente con il Livello 1, 2 e 3 del prodotto
**And** la ricerca risponde entro 300ms dalla pressione di ogni tasto su un dataset di 1.400 prodotti

---

### Story 3.2: Normalizzazione Quantità e Conversioni Autorizzate

Come utente,
voglio che l'app riconosca le unità di misura e converta automaticamente quelle compatibili,
così che "1 kg" e "1000 g" vengano trattati in modo coerente e io non debba farlo mentalmente.

**Acceptance Criteria:**

**Given** l'utente inserisce una quantità con un'unità compatibile (es. "1 kg")
**When** viene salvata la voce
**Then** la quantità viene memorizzata nel formato canonico normalizzato (es. valore: 1, unità: KG)
**And** conversioni autorizzate funzionano: kg↔g, l↔ml, etto↔g (1 etto = 100g)
**And** unità non compatibili (es. pezzi e litri) non vengono mai convertite o aggregate automaticamente
**And** unità sconosciute, valori non numerici o quantità ≤0 bloccano il salvataggio con messaggio di errore contestuale
**And** la quantità proposta o convertita è sempre visibile e modificabile dall'utente prima del salvataggio

---

### Story 3.3: Risoluzione Sinonimi verso Prodotto Canonico

Come utente,
voglio che digitando "latte scremato" o "latte ps" l'app trovi "Latte parzialmente scremato",
così che non debba conoscere il nome esatto del prodotto canonico per aggiungere una voce corretta.

**Acceptance Criteria:**

**Given** il catalogo con sinonimi preinstallati
**When** l'utente cerca con un termine sinonimo (es. "mozzarella di bufala campana")
**Then** il prodotto canonico corrispondente ("Mozzarella di bufala") appare nei risultati di ricerca
**And** la categoria canonica assegnata è quella del prodotto canonico (non del sinonimo)
**And** un sinonimo non ambiguo risolve verso un solo prodotto canonico
**And** un termine non presente né come nome canonico né come sinonimo non genera un match forzato

---

### Story 3.4: Attributi Prodotto Contestuali (Marca, Variante, Conservazione)

Come utente,
voglio poter specificare marca, variante e tipo di conservazione di un prodotto in modo opzionale,
così che la mia lista sia precisa quanto serve senza obbligarmi a inserire dati non necessari.

**Acceptance Criteria:**

**Given** un prodotto canonico selezionato nel form di inserimento/modifica
**When** il Livello 3 del prodotto è associato ad attributi definiti nel catalogo
**Then** compaiono solo i campi attributo pertinenti a quel Livello 3 (es. Marca, Variante per i latticini)
**And** ogni attributo mostra valori suggeriti selezionabili da tendina più opzione testo libero
**And** tutti gli attributi sono facoltativi: si può salvare senza compilarli
**And** gli attributi non modificano Livello 1, Livello 2 o Livello 3 senza una scelta esplicita dell'utente
**And** la presenza o assenza della Marca non crea automaticamente una Famiglia prodotto diversa

---

### Story 3.5: Rilevamento Duplicati e Dialog Risoluzione

Come utente,
voglio essere avvisato quando sto aggiungendo un prodotto già presente nella lista,
così che possa decidere consapevolmente se aggiornare la quantità esistente o creare una voce separata.

**Acceptance Criteria:**

**Given** una voce con "Latte parzialmente scremato / 1 l / Banco Frigo" già in lista
**When** l'utente aggiunge (manualmente o tramite pipeline) "Latte parzialmente scremato / 1 l / Banco Frigo"
**Then** compare il `DuplicateResolutionDialog` con il messaggio: "«Latte parzialmente scremato» è già presente con 1 l nella categoria Banco Frigo. Cosa vuoi fare?"
**And** il dialog presenta 3 pulsanti: "Somma quantità (+1 l → 2 l)" (primario, nero), "Sostituisci quantità (1 l)" (bordato), "Crea voce separata" (bordato), più "Annulla" (testuale)
**And** "Somma quantità" è abilitata solo se le unità sono compatibili (stessa famiglia di conversione); se incompatibili è disabilitata con nota esplicativa
**And** scegliendo "Somma quantità" la voce esistente viene aggiornata atomicamente (quantità + `updated_at`); nessuna nuova riga viene creata
**And** scegliendo "Sostituisci" compare una conferma prima di sovrascrivere
**And** scegliendo "Crea voce separata" viene inserita una nuova riga con UUID indipendente
**And** premendo "Annulla" o non completando la scelta la lista rimane invariata
**And** prodotti diversi della stessa categoria (es. latte e yogurt) non generano avviso di duplicato

---

### Story 3.6: Assegnazione Categoria Canonica e Correzione con Regole Apprese

Come utente,
voglio poter correggere la categoria assegnata automaticamente a un prodotto e scegliere se ricordare la mia preferenza,
così che la lista si adatti al mio modo di usarla senza applicare correzioni senza il mio consenso.

**Acceptance Criteria:**

**Given** un prodotto con categoria assegnata automaticamente dal catalogo
**When** l'utente modifica la categoria nel form (es. sposta "Pasta integrale" da "Dispensa" a un'altra sezione)
**Then** la correzione viene salvata e mostrata nella lista senza applicarsi ad altri inserimenti in modo silenzioso
**And** dopo il salvataggio TOGO propone non invasivamente: "Ricordo questa scelta per i prossimi inserimenti?" con pulsanti "Ricorda" e "Solo per ora"
**And** solo premendo "Ricorda" viene creata una Regola appresa che associa l'espressione/prodotto alla categoria corretta
**And** una Regola appresa non sovrascrive una scelta esplicita appena fatta dall'utente nella stessa sessione

**Given** una Regola appresa esistente
**When** l'utente la vede nella schermata Regole Apprese
**Then** la regola mostra: espressione origine, prodotto canonico, categoria assegnata, data ultima applicazione
**And** l'utente può modificare la categoria della regola, disattivarla o eliminarla

---

### Story 3.7: Wizard Categorizzazione Prodotto Sconosciuto

Come utente,
voglio poter aggiungere un prodotto non presente nel catalogo scegliendo la categoria in 3 passi,
così che non resti bloccato senza poter salvare un prodotto che mi serve.

**Acceptance Criteria:**

**Given** l'utente cerca un nome prodotto non presente nel catalogo né tra i sinonimi
**When** tenta di salvare o confermare il prodotto
**Then** si avvia un wizard a 3 step: selezione Livello 1 (Alimentare / Non alimentare), poi Livello 2 tra quelli del L1 scelto, poi Livello 3 tra quelli del L2 scelto (o scelta "Altro" come fallback)
**And** in nessun passo si può procedere senza una selezione esplicita
**And** al termine dei 3 passi il prodotto viene salvato localmente con `is_user_defined = 1` nella categoria selezionata
**And** il prodotto user-defined è immediatamente ricercabile nelle future aggiunte
**And** non viene creato automaticamente un nuovo Livello 1 o Livello 2: solo la selezione tra quelli esistenti è disponibile

---

## Epic 4: Inserimento Vocale

L'utente aggiunge prodotti con un comando vocale in italiano. TOGO interpreta, mostra la scomposizione nel VoiceBottomSheet e richiede conferma. Fallback manuale immediato se il servizio non è disponibile.

### Story 4.1: VoiceRecognitionGateway e RecognizerIntent it-IT

Come developer,
voglio un gateway vocale astratto che isola il RecognizerIntent di Android dal resto dell'applicazione,
così che il fallback manuale sia garantito e nessuna logica di dominio dipenda dall'API vocale Android.

**Acceptance Criteria:**

**Given** l'interfaccia `VoiceRecognitionGateway` nel package `domain`
**When** l'implementazione `AndroidVoiceRecognitionGateway` avvia il riconoscimento
**Then** viene invocato `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` con `EXTRA_LANGUAGE = "it-IT"`
**And** il risultato viene restituito come sealed class: `SpeechResult.Success(text: String)` o `SpeechResult.Failure(reason)`
**And** se il servizio vocale non è disponibile (nessun microfono, permesso negato, rete assente) viene restituito `SpeechResult.Failure` senza crash
**And** nessuna registrazione audio viene conservata dall'app oltre la durata del riconoscimento corrente
**And** `VoiceRecognitionGateway` nel package `domain` non importa classi `android.*`

---

### Story 4.2: VoiceFAB e VoiceBottomSheet UI

Come utente,
voglio un pulsante microfono ben visibile che apra un pannello di cattura vocale,
così che avviare un inserimento a voce sia immediato anche con una mano occupata.

**Acceptance Criteria:**

**Given** la schermata lista attiva
**When** l'utente visualizza la schermata
**Then** è presente un FAB circolare da 64dp con icona microfono, colore `accent-action` (#0F172A), bordo 2px, posizionato in modo fisso e sempre visibile

**Given** l'utente tocca il VoiceFAB
**When** il permesso microfono è concesso
**Then** si apre il `VoiceBottomSheet` occupando circa il 45% dello schermo con animazione d'onda `accent-highlight` (#2563EB) e indicazione "In ascolto..."
**And** il testo riconosciuto appare in real-time nel pannello durante l'ascolto

**Given** il permesso microfono non è stato ancora concesso
**When** l'utente tocca il VoiceFAB per la prima volta
**Then** viene richiesto il permesso di sistema Android
**And** se l'utente nega il permesso il bottom sheet non si apre e viene mostrato un messaggio che invita all'inserimento manuale

---

### Story 4.3: Parsing Comando Vocale e Scomposizione Strutturata

Come utente,
voglio che dopo aver parlato l'app mi mostri nome prodotto, quantità, unità e categoria interpretati dal mio comando,
così che possa verificare prima di confermare che la mia intenzione sia stata capita correttamente.

**Acceptance Criteria:**

**Given** il testo riconosciuto dal servizio vocale (es. "due litri di latte parzialmente scremato")
**When** il `VoiceCommandParser` processa il testo
**Then** estrae: quantità numerica (2), unità (litro → StandardUnit.LITER), nome prodotto ("latte parzialmente scremato")
**And** risolve il nome sul catalogo via ricerca sinonimi → prodotto canonico "Latte parzialmente scremato" con categoria "Banco Frigo / Latte"
**And** il `VoiceBottomSheet` mostra la card strutturata: Nome canonico | Quantità normalizzata | Categoria con icona matita per modifica rapida
**And** se nel testo sono presenti attributi (marca, variante, conservazione) vengono estratti e mostrati
**And** l'ordine delle parole nel comando (prodotto prima o quantità prima) non influenza la correttezza dell'interpretazione

---

### Story 4.4: Conferma, Annullamento e Chiarimento Ambiguità Vocale

Come utente,
voglio poter confermare o annullare ogni aggiunta vocale, e ricevere una richiesta di chiarimento se mancano dati,
così che la lista non venga mai modificata senza il mio consenso esplicito.

**Acceptance Criteria:**

**Given** la card strutturata mostrata nel VoiceBottomSheet
**When** l'utente tocca "Conferma e aggiungi"
**Then** il dispositivo emette haptic feedback (15ms), il bottom sheet si chiude, la voce appare nella lista attiva nella categoria corretta
**And** la pipeline completa di deduplicazione viene eseguita (se duplicato → DuplicateResolutionDialog)

**Given** la card strutturata mostrata nel VoiceBottomSheet
**When** l'utente tocca "Annulla" o chiude il bottom sheet
**Then** la lista rimane completamente invariata e nessuna voce parziale viene salvata

**Given** il parser ha estratto il nome prodotto ma manca la quantità o l'unità
**When** il VoiceBottomSheet mostra il risultato
**Then** solo il campo mancante è evidenziato con un selettore immediato (numerico per quantità, lista per unità)
**And** l'intera frase riconosciuta non viene scartata: i campi già estratti correttamente restano precompilati
**And** il pulsante "Conferma e aggiungi" resta disabilitato finché i campi obbligatori non sono completati

---

### Story 4.5: Fallback e Resilienza Vocale

Come utente,
voglio che l'app funzioni perfettamente anche quando il riconoscimento vocale non è disponibile,
così che una connessione assente o un servizio vocale non attivo non mi impediscano di usare la lista.

**Acceptance Criteria:**

**Given** il servizio RecognizerIntent non disponibile (modalità aereo, errore rete, o assenza di app di riconoscimento)
**When** l'utente tocca il VoiceFAB
**Then** il VoiceBottomSheet non si apre e compare un messaggio non invasivo: "Riconoscimento vocale non disponibile. Usa l'inserimento manuale."
**And** il pulsante "+ Aggiungi" per l'inserimento manuale è sempre accessibile e funzionante indipendentemente dallo stato del vocale
**And** un errore del servizio vocale a metà di un riconoscimento non lascia voci parziali nella lista
**And** nessun banner di errore persistente compare per le funzioni locali che non richiedono il vocale (lista, spunta, storico)

---

## Epic 5: Storico, Regole Apprese e Condivisione

L'utente consulta lo storico, riaggiunge prodotti in un tocco, gestisce le regole apprese e condivide la lista attiva via Sharesheet Android.

### Story 5.1: Schermata Storico con Ricerca e Riaggiunta

Come utente,
voglio consultare i prodotti che ho acquistato in passato e riaggiungerne uno alla lista corrente in un solo tocco,
così che preparare la spesa settimanale sia più rapido usando la mia storia di acquisti.

**Acceptance Criteria:**

**Given** l'utente tocca l'icona Storico nell'App Bar
**When** la schermata Storico si apre
**Then** mostra la lista dei prodotti in `HISTORICAL_ITEM` con nome canonico, ultima quantità, ultima unità e data ultimo acquisto
**And** se lo storico è vuoto compare l'empty state: "Nessun prodotto nello storico. I prodotti acquistati compariranno qui."
**And** è disponibile una barra di ricerca full-text per filtrare per nome canonico (es. "caffè" trova "Caffè macinato")

**Given** una riga nello storico
**When** l'utente tocca il pulsante "+ Aggiungi" della riga
**Then** viene eseguita la pipeline completa di deduplicazione (se già in lista → DuplicateResolutionDialog)
**And** se non è un duplicato il prodotto viene aggiunto alla lista attiva con la categoria canonica e l'ultima quantità registrata nello storico
**And** la schermata Storico rimane aperta dopo l'aggiunta; l'utente può aggiungere più prodotti consecutivamente

---

### Story 5.2: Gestione Regole Apprese

Come utente,
voglio vedere tutte le regole che ho insegnato all'app e poterle modificare, disattivare o eliminare,
così che abbia sempre il controllo su come vengono interpretati i miei inserimenti futuri.

**Acceptance Criteria:**

**Given** l'utente accede alla schermata Regole Apprese dall'App Bar (icona Impostazioni o voce di menu)
**When** la schermata si apre
**Then** mostra la lista di tutte le regole con: espressione/termine origine, prodotto canonico associato, categoria canonica, data ultima applicazione
**And** se non esistono regole compare un messaggio esplicativo su come vengono create

**Given** una regola nella lista
**When** l'utente tocca la regola
**Then** può modificare la categoria canonica associata o il prodotto canonico e salvare
**And** può disattivare la regola (toggle) senza eliminarla: una regola disattivata non viene applicata ai futuri inserimenti
**And** può eliminare definitivamente la regola previa conferma

**Given** una regola appresa applicata a un inserimento
**When** l'utente sceglie una categoria diversa per lo stesso prodotto nello stesso inserimento
**Then** la scelta corrente dell'utente prevale sulla regola senza modificarla silenziosamente
**And** TOGO offre di aggiornare la regola solo dopo conferma esplicita

---

### Story 5.3: Condivisione Lista Attiva via Sharesheet

Come utente,
voglio condividere la lista dei prodotti che devo ancora comprare come testo leggibile,
così che un familiare possa fare la spesa al posto mio senza dover installare TOGO.

**Acceptance Criteria:**

**Given** la lista attiva con almeno un prodotto non spuntato
**When** l'utente tocca l'icona Condividi nell'App Bar
**Then** viene generato un testo con intestazione "🛒 TOGO - Lista Spesa (N articoli)", seguito da sezioni per categoria con emoji identificativa e una riga per ogni voce (nome + quantità + unità)
**And** il testo include solo i prodotti con `is_checked = 0` (non acquistati)
**And** nessun dato dello storico, delle regole apprese o di altri metadati privati è incluso nel testo condiviso
**And** l'Android Sharesheet nativo si apre con il testo precompilato per permettere la scelta del canale (WhatsApp, email, SMS, ecc.)
**And** se la lista attiva è vuota (o tutti i prodotti sono spuntati) il pulsante condivisione mostra uno stato disabilitato o messaggio "La lista attiva è vuota"

---

## Epic 6: Accessibilità, Dark Mode e Qualità Trasversale

L'app supera le soglie di accessibilità WCAG AAA, supporta Dark Mode completa e rispetta i requisiti di privacy audio.

### Story 6.1: Etichette TalkBack e Semantica Accessibile

Come utente con disabilità visiva che usa TalkBack,
voglio che ogni elemento della lista annunci in modo completo nome, quantità, categoria e stato,
così che possa usare TOGO con lo screen reader in modo autonomo e senza ambiguità.

**Acceptance Criteria:**

**Given** TalkBack attivo sul dispositivo
**When** il focus si sposta su una riga prodotto (ItemRow)
**Then** TalkBack annuncia: "[Nome canonico], [quantità] [unità], [categoria], [spuntato/non spuntato]. Tocca due volte per segnare come acquistato."
**And** la sezione "Presi (N)" annuncia: "Sezione presi, N articoli, [espansa/collassata]"
**And** i pulsanti dell'App Bar (Storico, Condivisione, Impostazioni) hanno `contentDescription` esplicite
**And** il VoiceFAB annuncia: "Aggiungi prodotto con la voce. Pulsante."
**And** il DuplicateResolutionDialog annuncia correttamente titolo e tutte le opzioni disponibili

---

### Story 6.2: Target Touch e Dynamic Type

Come utente che usa l'app con una sola mano mentre spinge il carrello,
voglio che tutti i controlli abbiano area di tocco sufficientemente ampia e che il testo rispetti le mie impostazioni di sistema,
così che possa interagire senza errori e leggere comodamente anche con font grande.

**Acceptance Criteria:**

**Given** qualsiasi schermata dell'app
**When** viene ispezionata l'area di tocco di ogni elemento interattivo
**Then** nessun elemento interattivo ha area di tocco inferiore a 48×48dp (checkbox, pulsanti, chip, icone App Bar, FAB)

**Given** le impostazioni di sistema Android con font size impostata al valore massimo
**When** l'utente apre la lista attiva
**Then** i nomi dei prodotti non vengono troncati (nessun "…" forzato su un nome standard)
**And** le dimensioni delle righe si adattano al contenuto senza overflow fuori schermo
**And** il badge quantità rimane leggibile e non sovrapposto al nome del prodotto

---

### Story 6.3: Dark Mode Completa

Come utente che usa l'app di sera o in ambienti scuri,
voglio che TOGO supporti la Dark Mode di sistema preservando leggibilità e identità visiva,
così che l'app non abbagli e rispetti la batteria del dispositivo OLED.

**Acceptance Criteria:**

**Given** la modalità Dark attiva nelle impostazioni di sistema Android
**When** l'app viene aperta o passa in background e torna in foreground
**Then** il tema Dark viene applicato automaticamente: `surface-base-dark #0B0F17`, `surface-card-dark #131B2E`, `ink-primary-dark #F8FAFC`, `ink-secondary-dark #94A3B8`, `border-crisp-dark #475569`, `accent-highlight-dark #60A5FA`
**And** il rapporto di contrasto testo/sfondo per `ink-primary-dark` su `surface-base-dark` è ≥7:1
**And** tutti i componenti (ItemRow, CategoryHeader, VoiceBottomSheet, DuplicateResolutionDialog, FAB) mostrano i token dark corretti
**And** passando dalla modalità Light a Dark e viceversa non si verificano crash o stati inconsistenti della UI

---

### Story 6.4: Privacy Audio e Audit Performance

Come utente attento alla privacy,
voglio essere sicuro che le mie registrazioni vocali non vengano conservate dall'app,
così che le mie abitudini di spesa rimangano private.

**Acceptance Criteria:**

**Given** l'utente usa l'inserimento vocale
**When** il riconoscimento è completato (successo o fallimento)
**Then** nessun file audio o buffer PCM è persistito sul filesystem locale dell'app
**And** il testo riconosciuto viene usato solo per l'elaborazione dell'inserimento corrente e non viene salvato separatamente come log
**And** il testo riconosciuto non viene inviato a endpoint remoti dall'app (il riconoscimento è delegato al servizio OS)

**Given** il set di 200 casi di test (utterance italiane + casi manuali) compilato dal catalogo
**When** viene eseguita la suite di test di accettazione
**Then** la percentuale di assegnazioni categoria corrette è ≥95% (SM-1)
**And** la precisione deduplicazione duplicati esatti è ≥98% e il recall ≥95% (SM-2)
**And** il numero di modifiche non confermate nei test è 0 (SM-C2)

---

## Epic 7: Estensione Controllata del Catalogo

L'utente può aggiungere prodotti non presenti nel catalogo o proporre nuovi Livelli 3, con tutte le aggiunte rimaste locali (is_user_defined = 1).

### Story 7.1: Aggiunta Nuovo Prodotto Canonico su Categoria Esistente

Come utente,
voglio aggiungere un prodotto che non trovo nel catalogo scegliendo una categoria già esistente,
così che possa completare la lista anche per prodotti di nicchia senza essere bloccato.

**Acceptance Criteria:**

**Given** l'utente ha cercato un prodotto non presente nel catalogo (nessun match esatto né per sinonimo)
**When** sceglie di aggiungere il prodotto manualmente con nome e categoria
**Then** il form richiede: nome prodotto (obbligatorio, univoco nel Livello 3 scelto), Livello 1, Livello 2, Livello 3 (tutti da selezionare tra quelli esistenti)
**And** prima del salvataggio viene verificato che non esista già un prodotto canonico con nome semanticamente identico nello stesso Livello 3; se trovato viene mostrato un avviso di possibile duplicato con opzione di procedere o annullare
**And** il prodotto viene salvato in `CANONICAL_PRODUCT` con `is_user_defined = 1`
**And** il prodotto user-defined è immediatamente disponibile nella ricerca per futuri inserimenti
**And** i prodotti user-defined sono distinguibili visivamente nella lista suggerimenti (es. badge "Personalizzato")

---

### Story 7.2: Proposta Nuovo Livello 3 sotto un Livello 2 Esistente

Come utente,
voglio proporre una nuova categoria di terzo livello quando nessuna di quelle esistenti si adatta al prodotto che aggiungo,
così che il catalogo possa crescere in modo ordinato riflettendo le mie reali abitudini di spesa.

**Acceptance Criteria:**

**Given** l'utente seleziona un Livello 2 nel wizard di categorizzazione
**When** nessuno dei Livelli 3 disponibili è appropriato e l'utente sceglie "Proponi nuovo Livello 3"
**Then** viene richiesto il nome del nuovo Livello 3 con conferma esplicita
**And** prima di creare il nuovo Livello 3 viene verificata l'assenza di duplicati semantici tra i Livelli 3 esistenti nello stesso Livello 2; se trovato viene mostrato un avviso con il candidato duplicato
**And** solo dopo conferma esplicita il nuovo Livello 3 viene salvato con `is_user_defined = 1`
**And** non è possibile creare un nuovo Livello 1 o Livello 2: solo la selezione tra quelli preinstallati è disponibile
**And** il nuovo Livello 3 è immediatamente disponibile per l'assegnazione di prodotti canonici user-defined
