# Epic 9 Context: Conclusione Attività e Sezione Fatte

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Consentire di consultare le azioni completate e concluderle in modo esplicito e irreversibile. La conclusione pulisce soltanto le azioni spuntate: la lista della spesa e lo storico degli acquisti restano indipendenti e invariati.

## Stories

- Story 9.1: Sezione Fatte collassabile con conteggio
- Story 9.2: Pulsante Concludi attività e dialog di conferma
- Story 9.3: Eliminazione azioni spuntate senza undo e senza storico
- Story 9.4: Deselezione azione da Fatte

## Requirements & Constraints

- La conclusione riguarda esclusivamente le azioni spuntate; le non spuntate devono restare disponibili.
- Conferma e irreversibilità devono essere comunicate prima di concludere; dopo la conclusione non si offre annullamento e non si conserva uno storico delle azioni.
- Le azioni concluse non sono acquisti: non devono creare record nello storico acquisti né modificare conteggi di acquisto.
- La sezione Fatte mostra il numero di azioni spuntate, è consultabile e collassabile; la deselezione consente di riportare un’azione tra quelle attive prima della conclusione.

## Technical Decisions

- Room è la sorgente locale di verità; le letture esposte alla UI usano flussi `Flow`.
- Le mutazioni passano esclusivamente attraverso `ShoppingListRepository`, sono atomiche e vengono eseguite su thread IO.
- Le azioni risiedono in `ACTION_ITEM`, una tabella distinta da `SHOPPING_ITEM` e dai dati del catalogo. La conclusione rimuove le sole azioni spuntate da questa tabella e non coinvolge `HISTORICAL_ITEM`.

## UX & Interaction Patterns

- Fatte è una sezione collassabile, espansa inizialmente quando contiene azioni. Le righe sono barrate, ordinate dalla più recente; la checkbox permette di deselezionare.
- Quando ci sono azioni spuntate, in fondo alla sezione compare “Concludi attività”. Il dialog dichiara quante azioni saranno eliminate e che l’operazione è irreversibile; offre conferma e annullamento del dialog.
- Dopo la conferma le azioni concluse scompaiono, mentre le azioni attive e la lista della spesa restano disponibili senza modifiche.

## Cross-Story Dependencies

- Epic 8 fornisce persistenza, repository e operazioni di spunta per le azioni; la pagina Da fare ospita Fatte.
- 9.1 stabilisce sezione e conteggio, 9.2 introduce il comando e la conferma, 9.3 applica la cancellazione definitiva e 9.4 copre la deselezione prima della conclusione.
