# Epic 10 Context: Pager e Navigazione Spesa ↔ Da Fare

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Consentire il passaggio tra la lista della spesa e la lista delle azioni Da fare dentro la schermata principale, mantenendo riconoscibile la pagina corrente e preservando i gesti già associati alle righe. La cartella di planning contiene solo `epics.md`; i documenti PRD, architettura e UX richiamati nei suoi metadati non sono presenti come file separati, quindi il contesto qui sintetizza i vincoli riportati nell’epic.

## Stories

- Story 10.1: HorizontalPager con due pagine
- Story 10.2: Intestazione fissa con titolo, frecce e azioni globali
- Story 10.3: Swipe solo su intestazione/gruppi/spazi vuoti, non su righe
- Story 10.4: Frecce pager focusabili con TalkBack
- Story 10.5: Back da Da fare torna a Spesa
- Story 10.6: Persistenza pagina corrente e posizione di scroll

## Requirements & Constraints

- La navigazione riguarda esattamente due liste: Spesa e Da fare. Le azioni restano elementi distinti dai prodotti e non diventano categorie o prodotti canonici.
- Le due liste e i relativi dati restano disponibili localmente; l’interfaccia deve continuare a funzionare senza dipendere da rete o servizi vocali.
- Il cambio pagina deve convivere con le azioni esistenti sulle righe: il gesto orizzontale della riga mantiene il proprio significato di eliminazione.
- Il back dalla pagina Da fare torna alla pagina Spesa. L’avvio a freddo parte sempre da Spesa; in seguito pagina e scroll devono sopravvivere a rotazione e navigazione interna.
- I controlli di navigazione devono restare accessibili a tastiera e screen reader, con stato e destinazione comprensibili.

## Technical Decisions

- L’architettura adotta Compose/Material 3 con Clean Architecture e flusso unidirezionale dello stato. La UI legge dati osservabili dal repository; Room è la fonte autorevole per i dati persistenti.
- Il pager orizzontale ospita le due pagine della lista. Stato di pagina e comportamento Back vanno coordinati con lo stato di navigazione dell’app; la decisione architetturale richiede gestione tramite `BackHandler` coordinato con il pager.
- La selezione o il ripristino della pagina non deve alterare i modelli dati separati di `SHOPPING_ITEM` e `ACTION_ITEM`.

## UX & Interaction Patterns

- Lo swipe per cambiare pagina è consentito nell’intestazione, sulle intestazioni di gruppo e sugli spazi non occupati da righe; le righe conservano il gesto di eliminazione.
- L’intestazione identifica la lista corrente. Le frecce di pagina sono focusabili e annunciano destinazione e disponibilità; il focus torna al trigger dopo la chiusura di dialog o fogli.

## Cross-Story Dependencies

- Le pagine riutilizzano le liste già introdotte negli Epic 2 e 8 e le rispettive azioni di riga. L’intestazione, i controlli accessibili, i gesti e il back completano il comportamento di navigazione nelle storie successive dell’Epic 10; il ripristino di pagina e scroll dipende dallo stato del pager.
