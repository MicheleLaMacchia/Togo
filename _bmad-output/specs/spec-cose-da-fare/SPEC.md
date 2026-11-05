---
id: SPEC-cose-da-fare
companions:
  - brownfield.md
sources: []
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test, and validate. Source documents listed in frontmatter are for traceability — consult them only if you need narrative rationale or prose color this contract intentionally omits.

# Cose da fare

## Why

L'utente vuole depennare le cose da fare con lo stesso gesto della spesa, senza aprire un'altra app e senza mescolarle ai prodotti. La lista della spesa resta quella di oggi; a fianco compare una lista di azioni in testo libero, con urgenza e ambito, che può uscire e rientrare nello stesso testo della spesa.

## Capabilities

- **CAP-1**
  - **intent:** L'utente può passare tra la lista della spesa e la lista da fare con un gesto orizzontale oppure con le frecce in alto.
  - **success:** Il trascinamento orizzontale sull'intestazione della pagina, sulle intestazioni dei gruppi o su uno spazio non occupato da una riga cambia pagina. Il trascinamento orizzontale su una riga resta il gesto del cestino. Le frecce cambiano pagina e sono utilizzabili con TalkBack. Il checkout della spesa resta solo sulla pagina Spesa.
- **CAP-2**
  - **intent:** L'utente può aggiungere un'azione in testo libero, senza scegliere prodotto, categoria, quantità, marca o variante.
  - **success:** L'azione compare nel gruppo del suo ambito e della sua urgenza. I valori iniziali sono Altro e A tempo perso, modificabili prima di salvare. Il FAB (+) aggiunge un prodotto sulla pagina Spesa e un'azione sulla pagina Da fare.
- **CAP-3**
  - **intent:** L'utente può spuntare un'azione e vederla restare in lista come fatta.
  - **success:** L'azione spuntata passa nella sezione Fatte (N), resta visibile e barrata. Il checkout della spesa non la rimuove. Deselezionandola torna nel gruppo del suo ambito e della sua urgenza.
- **CAP-4**
  - **intent:** L'utente può concludere la lista da fare ed eliminare solo le azioni spuntate.
  - **success:** Dopo una conferma esplicita, le azioni spuntate non ci sono più, quelle non spuntate restano, e non esiste annulla. Lista della spesa e storico acquisti sono invariati; le azioni eliminate non vengono salvate in uno storico.
- **CAP-5**
  - **intent:** L'utente distingue una riga da fare da una riga della spesa.
  - **success:** La riga da fare mostra il testo, lo stato spuntato, l'urgenza e l'ambito, sempre presenti, e un segno visivo discreto. Non mostra quantità, unità, marca o variante.
- **CAP-6**
  - **intent:** L'utente può modificare un'azione attiva ed eliminarne una senza concludere la lista.
  - **success:** Il foglio di modifica cambia testo, urgenza e ambito di quell'azione. Le azioni in Fatte non sono modificabili. Lo swipe a sinistra su un'azione attiva rivela il cestino e l'eliminazione toglie solo quell'azione.
- **CAP-7**
  - **intent:** L'utente può assegnare a un'azione una delle tre urgenze: Urgente, A tempo perso, Eccezionale.
  - **success:** L'urgenza è obbligatoria, ha come valore iniziale A tempo perso ed è visibile sulla riga. Non crea né sposta una categoria del catalogo.
- **CAP-8**
  - **intent:** L'utente può assegnare a un'azione un ambito scelto da un elenco fisso, oppure Altro.
  - **success:** L'ambito è obbligatorio, ha come valore iniziale Altro ed è visibile sulla riga. Un valore fuori dall'elenco fisso non crea un ambito nuovo e ricade in Altro.
- **CAP-9**
  - **intent:** L'utente può condividere solo la spesa, solo le azioni, o entrambe.
  - **success:** Solo spesa produce il formato attuale, senza `[DA FARE]`. Solo azioni produce solo il blocco `[DA FARE]`. Entrambe produce prima la spesa e poi `[DA FARE]`. Le azioni spuntate non vengono condivise. Sotto `[DA FARE]`, `+` è l'ambito e `-` è l'urgenza; la riga indentata di due spazi è il testo dell'azione, senza quantità. Aree nell'ordine fisso, urgenze nell'ordine Urgente, A tempo perso, Eccezionale, azioni in ordine alfabetico. L'icona di condivisione è presente su entrambe le pagine. Le liste non vengono modificate.
- **CAP-10**
  - **intent:** L'utente può importare un testo che contiene `[DA FARE]` e aggiungere le azioni alla lista esistente.
  - **success:** Le righe del blocco diventano azioni con ambito e urgenza e si aggiungono alla lista azioni esistente, senza sostituirla. Non vengono creati duplicati. Le sezioni spesa dello stesso testo restano un import di prodotti. Un testo senza `[DA FARE]` non modifica le azioni. `[DA FARE]` non viene trattato come categoria del catalogo. Un blocco non valido annulla l'intero import.

## Constraints

- Il pager non intercetta il trascinamento orizzontale sulle righe: lì il gesto resta quello del cestino. Il pager vive sull'intestazione, sulle intestazioni dei gruppi e sugli spazi non occupati da righe.
- L'intestazione fissa sopra la lista contiene il titolo della pagina e le due frecce. I titoli sono Spesa e Da fare, con il conteggio delle voci non spuntate. La sezione Fatte ha il proprio conteggio.
- Urgenza e ambito sono obbligatori. I valori iniziali sono A tempo perso e Altro, modificabili nel foglio di aggiunta e di modifica.
- Le azioni attive sono raggruppate per ambito nell'ordine Casa, Pratiche, Salute, Lavoro, Altro, poi per urgenza nell'ordine Urgente, A tempo perso, Eccezionale, e in ordine alfabetico dentro il gruppo. Le azioni in Fatte stanno in fondo, dalla più recente.
- Le azioni in Fatte non sono modificabili e non hanno il cestino. Si possono solo deselezionare.
- L'eliminazione di una singola azione mostra l'undo, come per la spesa; la conclusione non ha annulla.
- Il controllo "Concludi attività" sta nella sezione Fatte e chiede conferma prima di eliminare.
- La conclusione elimina solo le azioni spuntate e non crea uno storico delle azioni.
- Il back dalla pagina Da fare riporta alla pagina Spesa.
- L'import non crea duplicati: un'azione identica per testo, ambito e urgenza a una già presente non viene aggiunta. Il confronto ignora maiuscole/minuscole, spazi iniziali/finali e spazi multipli; gli accenti restano significativi. Un'azione spuntata conta come già presente e non viene riaperta. I duplicati dentro lo stesso testo importato vengono saltati. Lo stesso testo con ambito o urgenza diversi è un'azione distinta.
- La stessa regola vale per l'aggiunta manuale e la modifica: se testo, ambito e urgenza coincidono con un'altra azione, anche in Fatte, l'operazione è bloccata con il messaggio "Azione già presente". L'azione in modifica non conta come duplicato di sé.
- Nell'import, un ambito mancante diventa Altro; un'urgenza mancante o non valida è un errore; un blocco `[DA FARE]` vuoto è un errore; più blocchi `[DA FARE]` vengono uniti. L'ordine dei blocchi nel testo non conta.
- Il testo dell'azione importata è preservato come scritto, senza capitalizzazione automatica e senza interpretare quantità o unità. Deve essere non vuoto e su una sola riga.
- L'import è tutto-o-niente: se una riga di `[DA FARE]` non è valida, non viene importato nulla, né prodotti né azioni. Un testo composto solo da `[DA FARE]` è un import valido.
- L'anteprima e il messaggio di esito dell'import riportano prodotti, azioni, duplicati saltati e categorie/prodotti creati. La descrizione e l'esempio di formato nelle Impostazioni includono il blocco `[DA FARE]`.
- La condivisione esclude le azioni spuntate. Se una delle due parti è vuota, il blocco vuoto viene omesso; se entrambe sono vuote, la condivisione è disabilitata o mostra un messaggio. L'oggetto è "Lista spesa TOGO", "Cose da fare TOGO" o "Lista spesa e cose da fare TOGO" in base alla scelta.
- La condivisione è il testo inviato con Android share: non c'è un export su file separato.
- Sulla pagina Da fare restano condivisione e impostazioni; storico e vocale restano solo sulla pagina Spesa. Il FAB (+) è contestuale.
- Un'azione non occupa nodi L1, L2 o L3 e non diventa un prodotto canonico. Non compare nella ricerca prodotti.
- Checkout, ricerca catalogo e storico acquisti della spesa non cambiano comportamento.
- Le azioni restano sul dispositivo, con lo stesso vincolo local-first del resto dell'app.
- In questa versione il vocale non aggiunge azioni.

## Non-goals

- Scadenze, notifiche, assegnatari e progetti.
- Ricorrenze, incluso "Periodico", e qualsiasi rischedulazione automatica.
- Aggiunta vocale di un'azione.
- Annulla della conclusione e storico delle azioni concluse.
- Ambiti creati dall'utente, oltre alla voce fissa Altro.
- Qualsiasi modifica al pulsante o all'effetto di "Concludi spesa".
- Inserire le azioni nel catalogo, nella ricerca prodotti o nella tassonomia.
- Export su file separato: la condivisione è il testo via Android share.
- Ricerca, filtro o riordino manuale delle azioni.
- Condivisione di una singola azione.

## Success signal

L'app parte sulla pagina Spesa. L'intestazione mostra Spesa (N) con le frecce. Un trascinamento sull'intestazione, o la freccia destra, apre Da fare. L'utente aggiunge un'azione in testo libero, con urgenza e ambito predefiniti o scelti, e la vede comparire nel gruppo giusto. Spuntandola passa in Fatte (N). Con "Concludi attività" conferma l'eliminazione delle sole azioni spuntate: le non spuntate restano, la spesa e lo storico acquisti sono invariati. La condivisione permette di scegliere solo spesa, solo azioni o entrambe; il testo delle azioni usa `[DA FARE]`, aree, urgenze e righe indentate, ed esclude le azioni in Fatte. Reimportando quel testo, i prodotti seguono l'import della spesa e le azioni si aggiungono senza duplicati; `[DA FARE]` non crea prodotti o categorie.

## Assumptions

- Il titolo della pagina è "Da fare" e il pulsante di conclusione è "Concludi attività".
- La sezione delle azioni spuntate si chiama "Fatte".

## Open Questions

Nessuna.
