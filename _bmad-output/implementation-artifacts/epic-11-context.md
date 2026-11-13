# Epic 11 Context: Condivisione a Tre Scelte (Spesa / Azioni / Entrambe)

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Permettere all'utente di scegliere con chiarezza quali parti della Lista attiva condividere: prodotti della Spesa, azioni Da fare oppure entrambe. Il flusso deve funzionare da entrambe le pagine, mostrare conteggi comprensibili e produrre testo condivisibile senza includere dati non selezionati o azioni già spuntate. L'epic comprende il dialog di scelta, il testo delle azioni, l'oggetto dinamico e i casi di contenuto vuoto.

## Stories

- Story 11.1: Dialog a tre scelte con conteggi
- Story 11.2: Formato testo [DA FARE] per condivisione azioni
- Story 11.3: Oggetto dinamico in base alla scelta di condivisione
- Story 11.4: Matrice scelta × parti vuote
- Story 11.5: ShareListUseCase per la condivisione delle sole azioni

## Requirements & Constraints

- L'icona Condividi è disponibile sia in Spesa sia in Da fare. La scelta predefinita nel dialog corrisponde alla pagina corrente.
- Il dialog presenta `Solo spesa (N)`, `Solo azioni (M)` ed `Entrambe (N+M)`, con primario `Condividi` e secondario `Annulla`. I testi quotati sono microcopy vincolante.
- Le quantità nei conteggi rappresentano elementi attivi non spuntati; le azioni nella sezione Fatte non vengono condivise.
- Le scelte relative a una parte vuota sono disabilitate con `Nessuna voce`; `Entrambe` richiede contenuti in entrambe le parti. Se tutto è vuoto, mostrare `Non c'è nulla da condividere` senza aprire il dialog.
- Il testo delle azioni usa un blocco `[DA FARE]`, con ambito, urgenza e riga azione secondo il formato specificato nelle singole storie; non include quantità.
- L'oggetto identifica i contenuti selezionati: `Lista spesa TOGO`, `Cose da fare TOGO` oppure `Lista spesa e cose da fare TOGO`.
- La condivisione è testo semplice tramite Sharesheet Android e non modifica le liste. Non include storico né personalizzazioni private.

## Technical Decisions

- La condivisione usa il canale plain-text Android `Intent.ACTION_SEND` (`text/plain`). Il testo e l'oggetto vengono preparati per la selezione effettuata prima dell'apertura dello Sharesheet.
- `ShareListUseCase` deve poter restituire il contenuto delle azioni anche se la spesa è vuota; la presenza di azioni attive non va trattata come lista vuota.
- La selezione e i contenuti devono rispettare la separazione tra prodotti della spesa e azioni. L'ordinamento delle azioni condivise segue ambito, urgenza e ordine alfabetico, come definito nell'epic.

## UX & Interaction Patterns

- Il dialog usa il pattern Material di scelta a opzioni grandi, con conteggi visibili; default sulla lista associata alla pagina corrente. Primario nero `Condividi`, secondario testuale `Annulla`.
- Dopo la conferma si apre lo Sharesheet Android. Chiudendo il dialog, il focus torna al controllo che lo ha aperto; tornando dallo Sharesheet, torna all'icona Condividi.
- Le opzioni non disponibili comunicano `Nessuna voce`; lo stato completamente vuoto dà feedback senza mostrare il dialog.

## Cross-Story Dependencies

- Il dialog (11.1) fornisce la selezione alle storie che generano testo e oggetto (11.2–11.3) e deve riflettere la disponibilità definita per i contenuti vuoti (11.4).
- La condivisione di sole azioni dipende dal comportamento di `ShareListUseCase` descritto in 11.5. Le azioni attive e spuntate derivano dalla lista Da fare dell'Epic 8.
