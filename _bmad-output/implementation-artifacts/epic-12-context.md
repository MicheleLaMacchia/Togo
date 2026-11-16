# Epic 12 Context: Import Azioni [DA FARE] (Tutto-o-Niente)

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Consentire di reimportare azioni condivise in formato testo senza confonderle con le categorie della spesa e senza creare duplicati o scritture parziali. Le azioni importate restano elementi separati dai prodotti e dal catalogo; la validazione e la conferma devono mantenere prevedibile l'esito dell'intera operazione.

## Stories

- Story 12.1: Parser riconosce blocco [DA FARE]
- Story 12.2: Import di sole azioni valido (0 prodotti)
- Story 12.3: Deduplica azioni per testo + ambito + urgenza con accenti significativi
- Story 12.4: Validazione ambito e urgenza nell'import
- Story 12.5: Import tutto-o-niente transazionale Room
- Story 12.6: Anteprima e messaggio esito import

## Requirements & Constraints

- Il formato `[DA FARE]` identifica una sezione di azioni, non una categoria L1 del catalogo. Il riconoscimento deve precedere la normale interpretazione delle intestazioni di categoria.
- Un testo privo del blocco `[DA FARE]` deve mantenere il comportamento di import dei prodotti e non produrre mutazioni sulle azioni.
- Una sezione azioni contiene righe strutturate con `+ AMBITO`, `- URGENZA` e testo dell'azione indentato. I valori devono essere validati secondo le regole dedicate; la deduplica considera testo normalizzato, ambito e urgenza e preserva le differenze accentate.
- Un import valido può contenere prodotti, azioni o entrambi. Il caso di sole azioni deve poter avere zero prodotti e non deve creare prodotti o categorie per rappresentare le azioni.
- L'intero import è atomico: se una riga non è valida non devono restare scritture parziali. Il risultato deve rendere comprensibili errori e, dopo la conferma, quantità importate, duplicate saltate e nuove categorie/prodotti creati.

## Technical Decisions

- Il dominio resta puro Kotlin e separato dai framework Android. Parsing, validazione e deduplica sono responsabilità del dominio; persistenza e scritture passano dalle interfacce repository.
- `ACTION_ITEM` è separata da `SHOPPING_ITEM` e dal catalogo. Un'azione ha testo, ambito, urgenza, stato spunta e timestamp; non deve essere modellata come prodotto né associata a una categoria tassonomica.
- Room è la sorgente locale di verità. Le modifiche di import alle azioni e ai prodotti devono essere coordinate tramite `ShoppingListRepository`; l'import completo va applicato in un'unica transazione Room dopo la validazione.
- Le storie sono incrementali: prima riconoscere la sezione, poi supportare il caso senza prodotti, deduplicare e validare le azioni, quindi garantire atomicità e completare anteprima/esito. Non anticipare comportamenti delle storie successive quando si lavora su una storia precedente.

## Cross-Story Dependencies

- Le azioni e il loro formato di condivisione provengono dall'Epic 8 e dall'Epic 11. Il parser deve riusare i contratti di ambito e urgenza già presenti.
- Story 12.1 fornisce il riconoscimento strutturale necessario alle storie successive, ma il supporto a import valido di sole azioni, deduplica, validazione, persistenza atomica e anteprima appartiene rispettivamente alle storie 12.2–12.6.
