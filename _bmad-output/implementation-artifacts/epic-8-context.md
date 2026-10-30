# Epic 8 Context: Fondamenta dati e domini Spesa e Da fare

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Costruire fondamenta locali affidabili per le due liste dell'app: Spesa e Da fare. La migrazione separa i domini, preserva i dati di acquisto esistenti e rende le transizioni temporali deterministiche, mantenendo l'app offline e senza fallback distruttivi.

## Stories

- Story 8.1: Baseline schema v5 e harness di migrazione
- Story 8.2: Clock e ZoneId iniettabili
- Story 8.3: Dominio ListItem, stati e invarianti
- Story 8.4: Schema v6 e migrazione Room 5 → 6
- Story 8.5: Repository ListItem e ciclo di vita della Spesa
- Story 8.6: Dominio Task, Room, categorie e transizioni
- Story 8.7: Adattamento della UI shopping esistente e regressione

## Requirements & Constraints

- L'app è local-first: Room è la sorgente di verità. La UI osserva flussi immutabili e invia le mutazioni ai ViewModel/repository.
- Spesa e Task sono domini distinti. Le query, i suggerimenti e le statistiche della spesa non devono includere Task; le statistiche di acquisto considerano solo righe `PURCHASED`.
- `ListItem` usa stati espliciti (`TO_BUY`, `PURCHASED`, `DELETED`), quantità finita positiva, unità testuale non vuota e timestamp di transizione. La quantità predefinita del form è 1; l'unità predefinita è `PIECE`.
- Le transizioni della Spesa sono limitate a `TO_BUY → PURCHASED`, `TO_BUY → DELETED` e `DELETED → TO_BUY`. Acquisti storici restano immutabili; `reAdd` crea una riga attiva oppure fonde con una riga compatibile secondo la regola del repository.
- La migrazione preserva lista, acquisti, attributi e conteggi; valori legacy non validi diventano quantità 1/unità `PIECE`. Non è consentito usare fallback distruttivi.
- L'interfaccia principale mostra righe `TO_BUY`; la spunta completa subito l'acquisto. Non ci sono più staging, sezione “Presi” o checkout.
- La cronologia legge gli stati acquistati/eliminati dalla lista unificata e conserva ricerca per nome e attributi, riaggiunta e conteggio acquisti.
- Clock e `ZoneIdProvider` sono interfacce di dominio senza dipendenze Android. Il tempo nei casi coperti viene iniettato e testabile con fake.
- La suite di regressione copre migrazioni e dati, transizioni, merge, ricerca, attributi, voce, condivisione e schermate Compose.

## Technical Decisions

- Architettura Clean con dipendenze dirette verso il dominio; il package `domain` resta Kotlin puro. I repository astratti vivono in `domain.repository`, le implementazioni Room in `data.repository` e il wiring in moduli Koin.
- Room usa una migrazione esplicita e transazionale, schema esportato e migrazioni registrate; nessun `fallbackToDestructiveMigration`. La migrazione 5→6 crea `LIST_ITEM`, `TODO_TASK` e categorie Task, copia i dati legacy e distingue ID storici per evitare collisioni.
- `ListItemRepository` è la fonte unica per righe attive, acquistate ed eliminate. Le operazioni multi-lettura/scrittura sono atomiche; il clock iniettato fornisce i timestamp.
- `TodoRepository` e i repository shopping restano separati. Le categorie Task consentono al massimo due livelli e la cancellazione della categoria non elimina la Task.
- L'input UI segue UDF/MVI: stato immutabile esposto da `StateFlow`, eventi espliciti e nessuna persistenza dal Composable.

## UX & Interaction Patterns

- La lista attiva contiene solo righe da acquistare; spunta, modifica ed eliminazione aggiornano subito lo stato persistito.
- La cronologia permette ricerca per nome/attributi e riaggiunta senza modificare la riga acquistata.
- Il form Spesa conserva autocomplete di catalogo, quantità/unità e attributi opzionali; quantità e unità iniziano rispettivamente da 1 e `PIECE`.

## Cross-Story Dependencies

- Stories 8.2–8.5 forniscono clock, modello, schema e repository necessari all'adattamento UI della 8.7.
- Story 8.1 fornisce schema v5 e harness richiesti dalla migrazione della 8.4.
- Story 8.6 condivide schema e clock ma mantiene Task separata dalla Spesa; il seed categorie e le query Task non devono contaminare il catalogo shopping.
- Le stories successive dell'epic 9 dipendono dalle liste e transizioni stabili definite qui.
