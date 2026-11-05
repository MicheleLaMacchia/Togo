---
stepsCompleted: ["step-01-validate-prerequisites", "step-02-design-epics", "step-03-create-stories", "step-04-final-validation"]
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-Togo-2026-09-20/prd.md
  - _bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md
---

# TOGO - Epic Breakdown

Questo documento fornisce la scomposizione completa in epiche e storie per TOGO, decomponendo i requisiti dal PRD, dal design UX (se esistente) e dall'architettura in storie implementabili.

## Requirements Inventory

### Functional Requirements

FR1: L'utente può aggiungere una Voce alla Lista attiva indicando una Famiglia prodotto/Prodotto canonico, una Categoria canonica, Quantità normalizzata e Unità. Quantità e Unità sono sempre obbligatorie per una Voce salvata e devono essere compilate manualmente tramite valore e selettore dell'Unità quando l'inserimento avviene dall'app. Gli Attributi prodotto restano opzionali, ma sono selezionabili quando pertinenti. (PRD §4.2 FR-1)

FR2: L'utente può consultare la Lista attiva raggruppata per Categoria canonica secondo un ordine standard. L'ordine delle categorie segue quello definito nella tassonomia di §4.1; all'interno di ciascuna categoria i Prodotti canonici sono ordinati alfabeticamente per nome. (PRD §4.2 FR-2)

FR3: L'utente può inviare un comando vocale relativo a un singolo prodotto da aggiungere alla Lista attiva. Il comando deve contenere Famiglia prodotto/Prodotto canonico, valore numerico e Unità, in qualunque ordine naturale; Marca, Variante e altri Attributi prodotto sono opzionali. Se manca uno dei dati obbligatori, TOGO deve chiederlo prima di salvare. (PRD §4.3 FR-3)

FR4: L'utente può confermare o annullare un'operazione vocale pendente. Nessuna modifica viene applicata se l'utente annulla o se la conferma richiesta non è completata. Un comando non riconosciuto o interrotto non crea una Voce parziale. (PRD §4.3 FR-4)

FR5: TOGO assegna ogni Prodotto canonico a una Categoria canonica unica e coerente. Lo stesso Prodotto canonico non viene mostrato in più categorie principali contemporaneamente. Le categorie corrette dall'utente possono essere riutilizzate nelle interpretazioni successive tramite Regole apprese. (PRD §4.4 FR-5)

FR6: TOGO normalizza espressioni equivalenti verso un Prodotto canonico, una Quantità normalizzata e un'Unità compatibile. Le unità compatibili possono essere convertite in un formato canonico coerente: kg ↔ g, l ↔ ml ed etto ↔ g. Le unità non compatibili non vengono sommate arbitrariamente. (PRD §4.4 FR-6)

FR7: Prima di creare una Voce, TOGO verifica un possibile duplicato sulla base del Prodotto canonico e della Categoria canonica specifica. Se esiste un Duplicato potenziale, TOGO comunica la quantità esistente e chiede se aggiungere la quantità, sostituirla o creare una nuova Voce separata. Marca, Conservazione/preparazione e Variante non distinguono il duplicato per default. (PRD §4.4 FR-7)

FR8: L'utente può contrassegnare una Voce come acquistata dalla Lista attiva. La Voce non appare più tra i prodotti da acquistare e il Prodotto canonico viene registrato nello Storico. (PRD §4.5 FR-8)

FR9: L'utente può selezionare un elemento dello Storico e riaggiungerlo alla Lista attiva tramite interfaccia. Il controllo dei Duplicati potenziali viene applicato anche al riaggiunta. (PRD §4.5 FR-9)

FR10: L'utente può generare e condividere un testo della Lista attiva raggruppato per Categoria canonica. Sono inclusi solo i prodotti ancora da acquistare. Lo Storico e le personalizzazioni private non vengono condivisi. Se la Lista attiva è vuota, TOGO mostra uno stato vuoto e non genera una condivisione fuorviante. (PRD §4.6 FR-10)

FR11: TOGO deve fornire una tassonomia preinstallata di Livelli 1, Livelli 2 e Livelli 3 e un set iniziale di Prodotti canonici con sinonimi ricercabili. Il catalogo autonomo contiene almeno 1.000 Prodotti canonici alimentari e 400 Prodotti canonici non alimentari. L'assenza di rete non impedisce la ricerca nel Catalogo già installato. (PRD §4.1 FR-11)

FR12: L'utente può aggiungere un Prodotto canonico non presente scegliendo una Categoria canonica esistente; può proporre un nuovo Livello 3 sotto un Livello 2 esistente. Un nuovo Livello 3 richiede conferma esplicita, un controllo di possibili duplicati e non può creare un nuovo Livello 1 o Livello 2 nell'MVP. (PRD §4.1 FR-12)

FR13: TOGO può associare gli Attributi prodotto minimi alla Famiglia prodotto, proponendo valori controllati ma consentendo testo libero, e deve renderli modificabili sia nel percorso vocale sia nell'inserimento manuale. (PRD §4.1 FR-13)

FR14: In una versione successiva, l'utente può inviare una proposta di nuova Famiglia prodotto, Marca, Variante o sinonimo esclusivamente al Catalogo centrale TOGO. La proposta entra in una coda di revisione e non viene pubblicata automaticamente agli altri utenti. (PRD §4.7 FR-14 - evoluzione futura, non MVP)

### NonFunctional Requirements

NFR1: Local-first - il nucleo della lista, delle categorie, delle quantità e dello Storico deve funzionare con dati locali sul dispositivo. (PRD §5)

NFR2: Resilienza - l'indisponibilità del servizio vocale o di un'integrazione opzionale non deve impedire inserimento manuale, consultazione, depennamento e storico. (PRD §5)

NFR3: Affidabilità dei dati - nessuna interpretazione ambigua deve modificare la lista senza conferma esplicita. (PRD §5)

NFR4: Usabilità - il percorso di aggiunta chiaro deve richiedere pochi passaggi; i chiarimenti devono chiedere solo ciò che manca. (PRD §5)

NFR5: Accessibilità - le informazioni mostrate devono avere un equivalente testuale; i comandi vocali non sono l'unico modo per usare il prodotto. (PRD §5)

NFR6: Privacy - i dati della lista e dello Storico restano locali nell'MVP; eventuali servizi esterni devono essere opzionali e trasparenti. (PRD §5)

NFR7: Qualità del catalogo - ogni Prodotto canonico autonomo deve avere nome generico, sinonimi controllati, mappatura tassonomica e controllo dei duplicati prima di diventare disponibile. (PRD §5)

NFR8: Dati vocali - TOGO non conserva registrazioni audio oltre il tempo necessario al riconoscimento; il testo riconosciuto viene usato solo per l'operazione corrente e non viene inviato a un catalogo centrale nell'MVP. (PRD §5)

NFR9: Aggiornabilità - il Catalogo prodotti deve poter essere aggiornato con una nuova fotografia versionata senza sovrascrivere silenziosamente le correzioni locali dell'utente. (PRD §5)

NFR10: Contributi futuri - le aggiunte degli utenti non devono entrare automaticamente nel Catalogo centrale senza deduplicazione, revisione e tracciamento della provenienza. (PRD §5)

### Additional Requirements

- Paradigma: Clean Architecture con Unidirectional Data Flow (UDF/MVI), separazione rigorosa tra presentation, domain puro Kotlin e data layer. (ARCH-AD-1)
- Domain layer puro Kotlin: zero import `android.*`; tutte le dipendenze esterne modellate tramite interfacce (Gateway/Repository). (ARCH-AD-1)
- Persistenza Single-Writer via Room + Kotlin Flow, Room è Single Source of Truth; letture UI tramite `Flow<T>`; mutazioni atomiche su thread IO coordinate da `ShoppingListRepository`. Tabella `ACTION_ITEM` dedicata per le azioni (testo, ambito, urgenza, stato spunta, timestamp) indipendente da `SHOPPING_ITEM` e dal catalogo; migrazione Room 5→6 non distruttiva che preserva il catalogo precompilato; import blocco `[DA FARE]` tutto-o-niente in un'unica transazione Room. `Concludi attività` elimina le sole azioni spuntate senza scrivere `HISTORICAL_ITEM` e senza incrementare conteggi. (ARCH-AD-2 esteso)
- Catalogo pre-popolato read-only con overlay user-defined: database SQLite precompilato negli asset, inizializzato al primo avvio tramite `createFromAsset()`; nuove Famiglie prodotto o Livelli 3 creati dall'utente risiedono con flag `is_user_defined = 1` o tabelle dedicate di overlay. (ARCH-AD-3)
- Speech gateway decoupled via `RecognizerIntent` con lingua `it-IT`; interfaccia `VoiceRecognitionGateway`; fallback a inserimento manuale in caso di errore di rete, mancato riconoscimento o rifiuto permessi. (ARCH-AD-4)
- Pipeline deterministica a 4 stadi (Tokenize & Extract → Catalog Match → Duplicate Check → User Confirmation Gate) per qualsiasi input vocale o manuale. (ARCH-AD-5)
- Condivisione sandbox plain-text via Android `Intent.ACTION_SEND` (`text/plain`); tre scelte utente: `Solo spesa`, `Solo azioni`, `Entrambe`; formato `[DA FARE]` con `+ AMBITO`, `- URGENZA` e riga azione indentata di due spazi; oggetto dinamico (`Lista spesa TOGO`, `Cose da fare TOGO` o `Lista spesa e cose da fare TOGO`); azioni spuntate sempre escluse; `ShareListUseCase` restituisce il testo anche quando la spesa è vuota ma ci sono azioni. (ARCH-AD-7 esteso)
- Pager orizzontale e navigazione tra Spesa e Da fare: cambio pagina solo da intestazione, intestazioni di gruppo o spazi non occupati da righe; swipe su riga = cestino; frecce focusabili con TalkBack; back da Da fare torna a Spesa (non esce dall'app) tramite `BackHandler` coordinato con il pager; avvio a freddo sempre su Spesa; pagina corrente e posizione di scorrimento sopravvivono a rotazione e navigazione interna; azioni non occupano nodi L1/L2/L3, non diventano prodotti canonici, non compaiono nella ricerca prodotti. (ARCH-AD-8 nuovo)
- Stack tecnologico: Kotlin 2.4.20, AGP 8.12.3, Gradle 8.13, Compose BOM 2026.09.00 con Material 3, Room 3.0.1 (KSP), KSP 2.3.12, Koin 4.2.2, Kotlinx Coroutines 1.9.0, Kotlinx Serialization 1.7.3; Min SDK 24, Target SDK 35. (ARCH-Stack)

### UX Design Requirements

UX-DR1: Token colore: definire coppie `*-dark` per tutti i componenti (surface-subtle, surface-inverse, ink-muted, ink-inverse, border-hairline, accent-action, accent-success, accent-warning, badge-bg, badge-ink) e dichiarare il mapping tema scuro. (DESIGN §2)

UX-DR2: Token `ink-struck` per righe spuntate (contrasto ≥4,5:1, idealmente 7:1), nessuna opacità applicata al testo; `ink-muted` riservato a veri componenti disabilitati. (DESIGN §7.1)

UX-DR3: Token `focus-ring` per tastiera/D-pad/Switch Access (contrasto ≥3:1 sulle superfici adiacenti); ordine di traversal: intestazione di pagina → lista → sezioni collassabili → FAB; frecce pager sempre focusabili e annunciate con stato (disponibile/non disponibile). (EXPERIENCE §7)

UX-DR4: Componente `empty-state` con spec visiva (icona/illustrazione, altezza, CTA, rapporto con FAB) e pattern comportamentale in EXPERIENCE §4. (DESIGN §7, EXPERIENCE §4)

UX-DR5: Componente `fab-action` o variante contestuale per il FAB (+) di Da fare con token e stato (visibile/nascosto, posizione, colore, etichetta); pattern in EXPERIENCE §4. (DESIGN §6, EXPERIENCE §4)

UX-DR6: Componente `conclude-dialog` con anatomia completa: icona warning, preview delle righe, nota irreversibile, ordine pulsanti (primario distruttivo, secondario Annulla). (DESIGN §7.10, EXPERIENCE §4.8)

UX-DR7: Componente `category-header`: statico (nessun tap), zona swipe pager, conteggio = voci non spuntate del gruppo. (DESIGN §7.2, EXPERIENCE §4)

UX-DR8: Mapping nomi componenti canonico: tabella token | nome visuale | pattern EXPERIENCE | mockup con un solo nome canonico per componente e alias espliciti. (DESIGN §7.13, EXPERIENCE)

UX-DR9: Pager: cambio pagina solo da intestazione, intestazioni di gruppo o spazi non occupati da righe; swipe orizzontale su una riga resta il cestino e non muove il pager. (EXPERIENCE §4.6, §6)

UX-DR10: Condivisione a tre scelte con matrice scelta × parti vuote; `Entrambe` selezionabile solo se entrambe le parti hanno voci; se entrambe vuote, icona Condividi mostra messaggio e non apre il dialogo; oggetto dinamico in base alla scelta; `ShareListUseCase` non restituisce `null` se la spesa è vuota ma ci sono azioni. (EXPERIENCE §4.10, §7.11; ARCH-AD-7)

UX-DR11: Pattern `Conclusione` unico con varianti `Spesa` e `Attività`; regole "solo spuntate", "niente undo", "niente storico" in un'unica sede canonica. (EXPERIENCE §4.3, §4.8; DESIGN §7.3, §7.9-7.10)

UX-DR12: Azione accessibile `Elimina` (custom action TalkBack / menu contestuale) con stessa snackbar di undo; etichetta TalkBack corretta ("Azioni disponibili: Elimina"). (EXPERIENCE §4.7, §7)

UX-DR13: Snackbar undo: durata ≥10 s o persistente fino all'azione successiva, pausa su focus/hover, live region educata ("Azione eliminata. Annulla disponibile"), azione Annulla ≥48dp. (EXPERIENCE §4.7, §7)

UX-DR14: Ritorno focus dopo chiusura dialog/fogli: target al controllo trigger; se non esiste, titolo pagina o FAB; al ritorno dallo Sharesheet focus sull'icona Condividi. (EXPERIENCE §4.6, §7)

UX-DR15: Import: anteprima come regione con heading annunciata ai lettori di schermo; primario `Importa N prodotti e M azioni` (o `Importa` quando una parte è zero), mai `Accetta`; errori associati al campo, annunciati con `LiveRegionMode.Assertive`, focus sul campo o sul primo errore. (EXPERIENCE §4.11, §7)

UX-DR16: Mockup: badge urgenza su ogni riga, target tocco 48dp (icone header, checkbox azione, chip, frecce, matita), stato focus `:focus-visible`, mockup dark (`active-list-dark`, `action-sheet-dark`), titoli e conteggi allineati alla spina (`Spesa (N)`, `Da fare (N)`), semantica ARIA (checkbox `role`, chip `aria-pressed`, scelte condivisione `role=radio`). (mockups/*.html)

UX-DR17: Microcopy vincolante: le stringhe quotate in §3 e §4 sono contract di copy; modifiche solo se elencate esplicitamente come "copy provvisorio". (EXPERIENCE §9)

UX-DR18: Glossario termini condiviso: azione, ambito (5 valori), urgenza (3 valori), Fatte, lista della spesa, lista attiva; definizioni d'uso per ciascuna urgenza. (EXPERIENCE §1, §6, DESIGN §2)

### FR Coverage Map

| FR | Epic | Nota |
|----|------|------|
| CAP-1 | Epic 10 | Pager e navigazione Spesa ↔ Da fare |
| CAP-2 | Epic 8 | Aggiunta azione testo libero |
| CAP-3 | Epic 8 | Spunta azione |
| CAP-4 | Epic 9 | Concludi attività |
| CAP-5 | Epic 8 | Distinzione visiva azione vs prodotto |
| CAP-6 | Epic 8 | Modifica/eliminazione azione |
| CAP-7 | Epic 8 | Urgenza obbligatoria |
| CAP-8 | Epic 8 | Ambito obbligatorio |
| CAP-9 | Epic 11 | Condivisione a tre scelte |
| CAP-10 | Epic 12 | Import azioni [DA FARE] |

## Epic List

### Epic 8: Gestione Azioni «Cose da Fare»
L'utente può aggiungere, modificare ed eliminare azioni in testo libero con urgenza e ambito, raggruppate per ambito → urgenza → alfabetico, e spuntarle come nella spesa.
**FRs covered:** CAP-2, CAP-3, CAP-6, CAP-7, CAP-8, CAP-5 (distinzione visiva)
**UX-DR:** UX-DR2, UX-DR4, UX-DR7, UX-DR8, UX-DR17, UX-DR18

### Epic 9: Conclusione Attività e Sezione Fatte
L'utente può concludere la lista Da fare eliminando solo le azioni spuntate, con conferma esplicita e senza undo/storico.
**FRs covered:** CAP-4
**UX-DR:** UX-DR6, UX-DR11

### Epic 10: Pager e Navigazione Spesa ↔ Da Fare
L'utente passa tra le due liste con swipe sull'intestazione o frecce; lo swipe sulle righe resta il cestino; il back da Da fare torna a Spesa.
**FRs covered:** CAP-1
**UX-DR:** UX-DR9, UX-DR3, UX-DR12, UX-DR13, UX-DR14

### Epic 11: Condivisione a Tre Scelte (Spesa / Azioni / Entrambe)
L'utente sceglie cosa condividere: solo spesa, solo azioni o entrambe, con formato [DA FARE] e oggetto dinamico.
**FRs covered:** CAP-9
**UX-DR:** UX-DR10, UX-DR17

### Epic 12: Import Azioni [DA FARE] (Tutto-o-Niente)
L'utente importa un testo contenente [DA FARE], le azioni si aggiungono alla lista esistente senza duplicati, in modo atomico.
**FRs covered:** CAP-10
**UX-DR:** UX-DR15, UX-DR18

### Epic 13: Accessibilità e Qualità UX (Focus, Contrasto, Dark Mode, Semantica)
L'app è pienamente accessibile (TalkBack, tastiera/D-pad, Switch Access) e rispetta il contrasto dichiarato, inclusi tema scuro e stati focus.
**FRs covered:** NFR5 (Accessibilità), NFR1 (Local-first), NFR6 (Privacy)
**UX-DR:** UX-DR1, UX-DR2, UX-DR3, UX-DR5, UX-DR16

<!-- Repeat for each epic in epics_list (N = 1, 2, 3...) -->

## Epic 8: Gestione Azioni «Cose da Fare»
L'utente può aggiungere, modificare ed eliminare azioni in testo libero con urgenza e ambito, raggruppate per ambito → urgenza → alfabetico, e spuntarle come nella spesa.

### Story 8.1: Tabella ACTION_ITEM e migrazione Room 5→6
As a sviluppatore,
I want creare la tabella ACTION_ITEM e la migrazione Room 5→6 non distruttiva,
So che le azioni possono essere persistite indipendentemente dalla spesa e il catalogo precompilato non viene perso.
**Acceptance Criteria:**
**Given** il database è alla versione 5 con catalogo precompilato
**When** viene applicata la migrazione 5→6
**Then** viene creata la tabella ACTION_ITEM con campi id (UUID), testo, ambito, urgenza, stato_spunta, created_at, updated_at
**And** il catalogo precompilato resta intatto e nessun dato utente viene perso

### Story 8.2: Repository e Use Case per le azioni
As a sviluppatore,
I want implementare il repository e i use case per le azioni (aggiunta, modifica, eliminazione, spunta, deduplica),
So che la logica di business delle azioni è isolata dal layer di presentazione.
**Acceptance Criteria:**
**Given** la tabella ACTION_ITEM esiste
**When** viene invocata l'aggiunta di una nuova azione
**Then** l'azione viene scritta in modo atomico tramite ShoppingListRepository su thread IO
**And** l'uso case espone Flow<T> per la lettura reattiva della lista azioni

### Story 8.3: Pagina Da fare con pager e intestazione
As a utente,
I want vedere la pagina Da fare con l'intestazione fissa "Da fare (N)" e le frecce di navigazione,
So che posso distinguere la pagina Da fare dalla pagina Spesa e cambiare pagina.
**Acceptance Criteria:**
**Given** l'app è aperta sulla pagina Spesa
**When** l'utente swipa sull'intestazione verso destra o tocca la freccia destra
**Then** viene mostrata la pagina Da fare con titolo "Da fare (N)" dove N = azioni non spuntate
**And** l'intestazione mostra Storico nascosto (solo su Spesa), Condividi e Impostazioni visibili su entrambe

### Story 8.4: Riga azione con checkbox, badge urgenza e badge ambito
As a utente,
I want vedere ogni azione con checkbox a sinistra, testo al centro, e badge urgenza + ambito sotto il testo,
So che posso distinguere un'azione da un prodotto e vedere urgenza e ambito in modo leggibile.
**Acceptance Criteria:**
**Given** esiste un'azione attiva con ambito e urgenza assegnati
**When** viene renderizzata la riga azione nella lista
**Then** viene mostrata checkbox a sinistra, testo al centro, nessun badge quantità
**And** sotto il testo compaiono badge urgenza (con testo esplicito) e badge ambito (con testo esplicito)
**And** non sono mostrati marca, variante o quantità

### Story 8.5: Foglio azione Aggiungi/Modifica con testo, urgenza e ambito
As a utente,
I want aprire un foglio azione con campo testo a una riga, selettore urgenza a tre chip e selettore ambito a cinque chip,
So che posso inserire una nuova azione o modificarne una esistente.
**Acceptance Criteria:**
**Given** l'utente tocca il FAB (+) sulla pagina Da fare
**When** si apre il foglio azione
**Then** il campo testo ha autofocus e placeholder "Es. Chiamare il dentista"
**And** i chip urgenza mostrano Urgente, A tempo perso (default), Eccezionale
**And** i chip ambito mostrano Casa, Pratiche, Salute, Lavoro, Altro (default Altro)
**And** il pulsante primario è disabilitato se il testo è vuoto o solo spazi

### Story 8.6: Spunta azione e spostamento in Fatte
As a utente,
I want spuntare un'azione e vederla spostata nella sezione Fatte,
So che posso tracciare le azioni completate senza eliminarle immediatamente.
**Acceptance Criteria:**
**Given** esiste un'azione attiva nella pagina Da fare
**When** l'utente tocca la checkbox della riga azione
**Then** dopo 300 ms la riga passa nella sezione Fatte, resta barrata e mantiene urgenza e ambito
**And** la spunta non elimina l'azione
**And** il focus non si sposta durante lo spostamento (resta sul nodo attivo o sull'header della sezione)

### Story 8.7: Modifica azione da foglio
As a utente,
I want modificare testo, urgenza e ambito di un'azione attiva toccando la riga,
So che posso correggere un'azione senza ricrearla.
**Acceptance Criteria:**
**Given** esiste un'azione attiva
**When** l'utente tocca la riga azione (non la checkbox)
**Then** si apre il foglio azione in modalità modifica con titolo "Modifica cosa da fare" e pulsante "Salva"
**And** i valori attuali (testo, urgenza, ambito) sono precaricati
**And** se il cambio di ambito o urgenza sposta l'azione in un altro gruppo, la riga si sposta nel nuovo gruppo al salvataggio

### Story 8.8: Eliminazione azione con cestino e undo
As a utente,
I want eliminare una singola azione tramite swipe a sinistra o azione accessibile, con possibilità di annullare,
So che posso rimuovere un'azione per errore senza perderla definitivamente.
**Acceptance Criteria:**
**Given** esiste un'azione attiva
**When** l'utente fa swipe a sinistra sulla riga o seleziona l'azione accessibile "Elimina"
**Then** viene rivelato il cestino e l'azione viene eliminata
**And** viene mostrata una snackbar con "Annulla" (durata ≥10 s, pausa su focus/hover, live region educata)
**And** l'undo non è disponibile dopo "Concludi attività"

### Story 8.9: Deduplica azioni per testo + ambito + urgenza
As a utente,
I want che l'addizione o modifica di un'azione sia bloccata se testo, ambito e urgenza coincidono con un'altra azione esistente,
So che non vengono create azioni duplicate.
**Acceptance Criteria:**
**Given** esiste già un'azione con testo, ambito e urgenza identici (anche in Fatte)
**When** l'utente tenta di aggiungere o modificare un'azione con la stessa combinazione
**Then** viene mostrato l'errore inline "Azione già presente" e il foglio resta aperto
**And** i valori scelti dall'utente non vengono persi
**And** l'azione in modifica non conta come duplicato di sé stessa
**And** lo stesso testo con ambito o urgenza diversi è considerato un'azione distinta
**And** gli accenti sono significativi nel confronto

### Story 8.10: Raggruppamento azioni per ambito → urgenza → alfabetico
As a utente,
I want vedere le azioni raggruppate per ambito nell'ordine Casa, Pratiche, Salute, Lavoro, Altro, poi per urgenza Urgente, A tempo perso, Eccezionale, e infine alfabetiche,
So che posso trovare rapidamente le azioni nella lista.
**Acceptance Criteria:**
**Given** esistono azioni in diversi ambiti e urgenze
**When** viene renderizzata la pagina Da fare
**Then** le azioni sono raggruppate per ambito con header di ambito
**And** all'interno di ogni ambito le azioni sono ordinate per urgenza (Urgente, A tempo perso, Eccezionale)
**And** all'interno dello stesso ambito e urgenza le azioni sono ordinate alfabeticamente
**And** l'urgenza è un criterio di ordinamento, non un header aggiuntivo

## Epic 9: Conclusione Attività e Sezione Fatte
L'utente può concludere la lista Da fare eliminando solo le azioni spuntate, con conferma esplicita e senza undo/storico.

### Story 9.1: Sezione Fatte collassabile con conteggio
As a utente,
I want vedere la sezione Fatte (N) collassabile in fondo alla pagina Da fare,
So che posso consultare le azioni completate e nasconderle quando non servono.
**Acceptance Criteria:**
**Given** esistono azioni spuntate nella sezione Fatte
**When** viene renderizzata la pagina Da fare
**Then** viene mostrata la sezione "Fatte (N)" collassabile, espansa di default se N > 0
**And** il conteggio N mostra il numero di azioni spuntate
**And** se N = 0 la sezione non compare

### Story 9.2: Pulsante Concludi attività e dialog di conferma
As a utente,
I want vedere il pulsante "Concludi attività" in fondo alla sezione Fatte e ricevere una conferma esplicita prima dell'eliminazione,
So che posso confermare consapevolmente l'eliminazione irreversibile delle azioni spuntate.
**Acceptance Criteria:**
**Given** N > 0 nella sezione Fatte
**When** viene renderizzata la sezione Fatte
**Then** viene mostrato il pulsante largo "Concludi attività" in fondo alla sezione
**And** toccando il pulsante si apre un dialog di conferma con titolo "Concludere le attività?" e corpo "Verranno eliminate N attività spuntate. L'operazione non si può annullare."
**And** il primario è "Concludi attività" (stile distruttivo, con icona warning) e il secondario è "Annulla"

### Story 9.3: Eliminazione azioni spuntate senza undo e senza storico
As a utente,
I want che dopo la conferma le azioni spuntate vengano eliminate definitivamente senza possibilità di annullo,
So che la conclusione è irreversibile e le azioni eliminate non occupano spazio nello storico.
**Acceptance Criteria:**
**Given** l'utente ha confermato "Concludi attività"
**When** il dialog si chiude con conferma
**Then** le azioni spuntate vengono eliminate dalla tabella ACTION_ITEM
**And** le azioni non spuntate restano nelle loro sezioni
**And** non viene scritto nulla in HISTORICAL_ITEM e non vengono incrementati conteggi di acquisto
**And** la lista della spesa e lo storico acquisti non cambiano
**And** non esiste undo dopo la conclusione

### Story 9.4: Deselezione azione da Fatte
As a utente,
I want deselezionare un'azione dalla sezione Fatte per riportarla nel gruppo attivo,
So che posso rimediare a una spunta accidentale prima di concludere.
**Acceptance Criteria:**
**Given** esiste un'azione nella sezione Fatte
**When** l'utente tocca la checkbox dell'azione spuntata
**Then** l'azione torna nel gruppo corretto (ambito → urgenza → alfabetico)
**And** l'azione non è più barrata e la checkbox è deselezionata

## Epic 10: Pager e Navigazione Spesa ↔ Da Fare
L'utente passa tra le due liste con swipe sull'intestazione o frecce; lo swipe sulle righe resta il cestino; il back da Da fare torna a Spesa.

### Story 10.1: HorizontalPager con due pagine
As a utente,
I want vedere un pager orizzontale con due pagine (Spesa a sinistra, Da fare a destra),
So che posso passare da una lista all'altra senza cambiare schermata.
**Acceptance Criteria:**
**Given** l'app è avviata
**When** viene mostrata la schermata principale
**Then** viene renderizzato un HorizontalPager con esattamente due pagine
**And** la pagina iniziale è Spesa (cold start)
**And** la pagina Da fare è a destra della pagina Spesa

### Story 10.2: Intestazione fissa con titolo, frecce e azioni globali
As a utente,
I want vedere un'intestazione fissa sopra la lista con titolo, frecce di pagina e azioni globali,
So che so sempre in quale pagina mi trovo e posso accedere alle funzioni secondarie.
**Acceptance Criteria:**
**Given** l'utente è su una qualsiasi pagina del pager
**When** viene renderizzata l'intestazione
**Then** il titolo mostra "Spesa (N)" o "Da fare (N)" con N = voci non spuntate
**And** sono presenti frecce sinistra e destra per cambiare pagina
**And** Storico è visibile solo sulla pagina Spesa
**And** Condividi e Impostazioni sono visibili su entrambe le pagine

### Story 10.3: Swipe solo su intestazione/gruppi/spazi vuoti, non su righe
As a utente,
I want che lo swipe orizzontale cambi pagina solo se inizia su intestazione, intestazioni di gruppo o spazi vuoti,
So che lo swipe su una riga continua a rivelare il cestino e non muove il pager.
**Acceptance Criteria:**
**Given** l'utente inizia uno swipe orizzontale
**When** lo swipe inizia sull'intestazione, su un'intestazione di gruppo o su uno spazio non occupato da righe
**Then** il pager cambia pagina
**And** quando lo swipe inizia su una riga, il pager non intercetta il drag e viene rivelato il cestino della riga

### Story 10.4: Frecce pager focusabili con TalkBack
As a utente che usa TalkBack,
I want che le frecce del pager siano focusabili e annunciate con il loro stato,
So che posso navigare tra le pagine anche con lo screen reader.
**Acceptance Criteria:**
**Given** l'utente usa TalkBack
**When** naviga verso le frecce di pagina
**Then** ogni freccia è focusabile e ha un'etichetta esplicita (es. "Vai a Da fare", "Torna a Spesa")
**And** la freccia senza pagina disponibile è annunciata come non disponibile ma resta focusabile
**And** al cambio pagina viene annunciato il nuovo titolo e il conteggio

### Story 10.5: Back da Da fare torna a Spesa
As a utente,
I want che premendo back dalla pagina Da fare torni alla pagina Spesa,
So che non esco accidentalmente dall'app.
**Acceptance Criteria:**
**Given** l'utente è sulla pagina Da fare
**When** preme il tasto back di sistema
**Then** l'app torna alla pagina Spesa
**And** non viene mostrata un'anteprima di chiusura app
**And** il comportamento è coordinato con il pager tramite PredictiveBackHandler

### Story 10.6: Persistenza pagina corrente e posizione di scroll
As a utente,
I want che la pagina corrente e la posizione di scroll di ciascuna lista sopravvivano a rotazione e navigazione interna,
So che non perdo il contesto quando ruoto il telefono o torno indietro.
**Acceptance Criteria:**
**Given** l'utente ha scelto una pagina e scrollato una lista
**When** ruota il dispositivo o naviga internamente
**Then** al ritorno viene ripristinata la pagina corretta e la posizione di scroll di ciascuna lista
**And** a freddo l'app parte sempre dalla pagina Spesa

## Epic 11: Condivisione a Tre Scelte (Spesa / Azioni / Entrambe)
L'utente sceglie cosa condividere: solo spesa, solo azioni o entrambe, con formato [DA FARE] e oggetto dinamico.

### Story 11.1: Dialog a tre scelte con conteggi
As a utente,
I want vedere un dialog con tre scelte (Solo spesa, Solo azioni, Entrambe) ciascuna con conteggio,
So che posso scegliere esattamente cosa condividere.
**Acceptance Criteria:**
**Given** l'utente tocca l'icona Condividi su una qualsiasi pagina
**When** si apre il dialog di condivisione
**Then** vengono mostrate tre scelte: "Solo spesa (N)", "Solo azioni (M)", "Entrambe (N+M)"
**And** il default è la scelta corrispondente alla pagina corrente (Spesa → Solo spesa; Da fare → Solo azioni)
**And** il pulsante primario è "Condividi" e il secondario è "Annulla"

### Story 11.2: Formato testo [DA FARE] per condivisione azioni
As a utente,
I want che la condivisione di sole azioni produca un blocco [DA FARE] con formato corretto,
So che il destinatario può leggere e reimportare le azioni.
**Acceptance Criteria:**
**Given** l'utente sceglie "Solo azioni" o "Entrambe"
**When** viene generato il testo di condivisione
**Then** per le azioni viene prodotto il blocco "[DA FARE]" con "+ AMBITO", "- URGENZA" e riga azione indentata di due spazi
**And** le azioni sono ordinate per ambito (Casa, Pratiche, Salute, Lavoro, Altro), poi urgenza (Urgente, A tempo perso, Eccezionale), poi alfabetiche
**And** le azioni spuntate sono escluse
**And** non viene inclusa alcuna quantità

### Story 11.3: Oggetto dinamico in base alla scelta di condivisione
As a utente,
I want che l'oggetto della condivisione vari in base alla scelta (Solo spesa, Solo azioni, Entrambe),
So che il destinatario capisce subito cosa sta ricevendo.
**Acceptance Criteria:**
**Given** l'utente sceglie un'opzione di condivisione
**When** viene aperto l'Android Sharesheet
**Then** l'oggetto è "Lista spesa TOGO" se scelta Solo spesa
**And** l'oggetto è "Cose da fare TOGO" se scelta Solo azioni
**And** l'oggetto è "Lista spesa e cose da fare TOGO" se scelta Entrambe

### Story 11.4: Matrice scelta × parti vuote
As a utente,
I want che le scelte con parte vuota siano disabilitate e che Entrambe sia disponibile solo se entrambe le parti hanno voci,
So che non posso condividere qualcosa che non esiste.
**Acceptance Criteria:**
**Given** la spesa è vuota ma ci sono azioni
**When** si apre il dialog di condivisione
**Then** "Solo spesa (0)" è disabilitata con nota "Nessuna voce"
**And** "Solo azioni (M)" è abilitata
**And** "Entrambe" è disabilitata
**And** se entrambe le parti sono vuote, l'icona Condividi mostra "Non c'è nulla da condividere" e non apre il dialogo

### Story 11.5: ShareListUseCase non restituisce null su spesa vuota con azioni
As a sviluppatore,
I want che ShareListUseCase restituisca il testo da condividere anche quando la spesa è vuota ma ci sono azioni,
So che la condivisione di sole azioni funziona correttamente.
**Acceptance Criteria:**
**Given** la spesa è vuota e ci sono M azioni attive
**When** viene invocato ShareListUseCase per "Solo azioni"
**Then** il caso restituisce il testo con il blocco [DA FARE] e M azioni
**And** non restituisce null solo perché la spesa è vuota

## Epic 12: Import Azioni [DA FARE] (Tutto-o-Niente)
L'utente importa un testo contenente [DA FARE], le azioni si aggiungono alla lista esistente senza duplicati, in modo atomico.

### Story 12.1: Parser riconosce blocco [DA FARE]
As a sviluppatore,
I want che il parser di import riconosca il blocco [DA FARE] prima del controllo tassonomia L1,
So che un testo con sole azioni non viene rifiutato come sezione sconosciuta.
**Acceptance Criteria:**
**Given** un testo di import contenente un blocco [DA FARE]
**When** il parser elabora il testo
**Then** il blocco [DA FARE] è riconosciuto come sezione azioni e non come Livello 1 della tassonomia
**And** un testo senza [DA FARE] non modifica le azioni

### Story 12.2: Import di sole azioni valido (0 prodotti)
As a utente,
I want importare un testo che contiene solo azioni [DA FARE],
So che posso reimportare le azioni anche senza prodotti.
**Acceptance Criteria:**
**Given** un testo di import che contiene solo un blocco [DA FARE] senza prodotti
**When** viene mostrata l'anteprima di import
**Then** la conferma mostra "0 prodotti" e N azioni
**And** l'import è valido e può essere confermato
**And** il blocco non crea prodotti o categorie

### Story 12.3: Deduplica azioni per testo + ambito + urgenza con accenti significativi
As a utente,
I want che durante l'import le azioni duplicate per testo, ambito e urgenza siano saltate,
So che non vengono create voci duplicate nella lista esistente.
**Acceptance Criteria:**
**Given** esiste già un'azione con stesso testo, ambito e urgenza (anche spuntata)
**When** viene importato un testo con un'azione identica
**Then** l'azione duplicata è saltata e non viene riaperta
**And** il confronto ignora maiuscole e spazi ma non gli accenti
**And** lo stesso testo con ambito o urgenza diversi è un'azione distinta e viene aggiunta

### Story 12.4: Validazione ambito e urgenza nell'import
As a utente,
I want che un ambito mancante diventi Altro e un'urgenza mancante o non valida generi un errore,
So che l'import rifiuta dati non validi ma gestisce i casi borderline in modo sensato.
**Acceptance Criteria:**
**Given** un'azione di import con ambito mancante
**When** viene elaborata l'azione
**Then** l'ambito mancante viene impostato a "Altro"
**And** un'urgenza mancante o non valida genera un errore: "Urgenza non valida: usa Urgente, A tempo perso o Eccezionale."
**And** un blocco [DA FARE] vuoto genera un errore

### Story 12.5: Import tutto-o-niente transazionale Room
As a sviluppatore,
I want che l'intero import sia atomico (tutto o niente),
So che se una riga non è valida non vengono effettuate scritture parziali.
**Acceptance Criteria:**
**Given** un testo di import con una riga di [DA FARE] non valida
**When** viene eseguito l'import
**Then** nessuna scrittura viene effettuata, né prodotti né azioni
**And** l'import è gestito in un'unica transazione Room
**And** viene mostrato un messaggio d'errore con numero di riga e motivo

### Story 12.6: Anteprima e messaggio esito import
As a utente,
I want vedere un'anteprima e un messaggio di esito chiari dopo l'import,
So che so cosa è stato importato e cosa è stato saltato.
**Acceptance Criteria:**
**Given** un testo di import con prodotti e azioni
**When** viene mostrata l'anteprima
**Then** viene riportato il numero di prodotti, azioni, duplicati saltati e categorie/prodotti creati
**And** l'anteprima è una regione con heading annunciata ai lettori di schermo
**And** dopo la conferma viene mostrato un messaggio di esito con prodotti, azioni, duplicati saltati e creazioni
**And** il pulsante di conferma si etichetta "Importa N prodotti e M azioni"

## Epic 13: Accessibilità e Qualità UX (Focus, Contrasto, Dark Mode, Semantica)
L'app è pienamente accessibile (TalkBack, tastiera/D-pad, Switch Access) e rispetta il contrasto dichiarato, inclusi tema scuro e stati focus.

### Story 13.1: Token dark mode coppie *-dark
As a sviluppatore,
I want definire coppie di token scuri per tutti i componenti dell'interfaccia,
So che il tema scuro è utilizzabile senza perdita di contrasto o visibilità.
**Acceptance Criteria:**
**Given** il tema scuro è attivato
**When** vengono renderizzati componenti (category-header, badge, FAB, pulsanti, bordi)
**Then** ogni componente usa le varianti *-dark (surface-subtle-dark, ink-muted-dark, badge-bg-dark, accent-action-dark, ecc.)
**And** non ci sono testi invisibili (es. titolo su sfondo chiaro 1:1) né controlli che spariscono sullo sfondo

### Story 13.2: Token ink-struck per righe spuntate
As a sviluppatore,
I want definire un token ink-struck con contrasto adeguato per le righe spuntate,
So che le righe prese/fatte siano leggibili anche da utenti con ipovisione.
**Acceptance Criteria:**
**Given** una riga prodotto o azione è spuntata
**When** viene renderizzata nello stato preso/fatto
**Then** il testo usa il token ink-struck con contrasto ≥4,5:1 (idealmente 7:1)
**And** non viene applicata opacità al testo
**And** barratura e checkbox sono usate come ridondanza visiva

### Story 13.3: Token focus-ring e ordine traversal
As a utente che usa tastiera/D-pad/Switch Access,
I want vedere un focus ring ad alto contrasto e un ordine di traversal prevedibile,
So che posso navigare l'interfaccia senza mouse o tocco.
**Acceptance Criteria:**
**Given** l'utente naviga con tastiera o D-pad
**When** si sposta il focus tra i controlli
**Then** ogni controllo interattivo espone un focus ring ad alto contrasto (contrasto ≥3:1 sulle superfici adiacenti)
**And** l'ordine di traversal è: intestazione di pagina → lista → sezioni collassabili → FAB
**And** le frecce del pager sono sempre focusabili e annunciate

### Story 13.4: Semantica TalkBack per checkbox, chip, sezioni e dialog
As a utente che usa TalkBack,
I want che ogni controllo esponga ruolo, stato e descrizione corretti,
So che posso capire e interagire con l'interfaccia tramite lo screen reader.
**Acceptance Criteria:**
**Given** l'utente esplora l'interfaccia con TalkBack
**When** si focalizza su una checkbox, un chip di urgenza/ambito, un header collassabile o un dialog
**Then** la checkbox espone Role.Checkbox con stateDescription (checked/unchecked)
**And** i chip espongono radiogroup con etichetta di gruppo e stato selected
**And** gli header collassabili espongono expanded/collapsed e ruolo heading
**And** i dialog espongono paneTitle, focus iniziale, focus trap e focus di ritorno

### Story 13.5: Mockup allineati (badge urgenza, target 48dp, stato focus, dark mockup)
As a sviluppatore/designer,
I want che i mockup di riferimento rispettino le specifiche della spina (target 48dp, badge urgenza, stato focus, versione dark),
So che l'implementazione UI sia coerente con il contratto di design.
**Acceptance Criteria:**
**Given** i mockup HTML di riferimento
**When** vengono confrontati con DESIGN.md e EXPERIENCE.md
**Then** ogni riga azione mostra il badge urgenza
**And** tutti i target touch sono ≥48dp (icone header, checkbox azione, chip, frecce)
**And** esiste uno stato focus visibile (:focus-visible) per tastiera/D-pad
**And** esistono mockup dark mode per verificare il contrasto delle superfici scure
