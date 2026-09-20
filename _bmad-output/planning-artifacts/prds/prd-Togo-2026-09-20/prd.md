---
title: "PRD: TOGO"
status: final
created: 2026-09-20
updated: 2026-09-20
---

# PRD: TOGO

## 0. Scopo del documento

Questo PRD definisce visione, perimetro MVP e requisiti funzionali di TOGO sulla base del [Product Brief](../../briefs/brief-Togo-2026-09-20/brief.md) finalizzato e del log del brainstorming. Non sostituisce la successiva specifica tecnica o UX. Le decisioni del Product Brief corrente prevalgono sulle idee storiche rimaste nel brainstorming. Le assunzioni residue sono raccolte nell’indice finale.

## 1. Visione

TOGO è un’app Android local-first per creare e usare liste della spesa affidabili. Aiuta una persona a catturare un prodotto quando sta finendo, anche tramite comando vocale, e a ritrovarlo in una categoria coerente durante la spesa.

Il valore non è solo sostituire una nota testuale: TOGO deve ridurre due errori ricorrenti, dimenticare ciò che serve e creare voci duplicate o classificate in modo incoerente. La fiducia nasce da categorie canoniche, quantità strutturate, controllo dei duplicati e correzioni esplicite quando l’interpretazione non è certa.

Nel tempo TOGO potrà evolvere verso più liste, condivisione e collaborazione familiare. Il primo rilascio deve però validare il nucleo: cattura rapida, organizzazione utile e consultazione semplice.

## 2. Utente target

### 2.1 Jobs To Be Done

- Quando mi accorgo che un prodotto sta finendo, voglio registrarlo rapidamente senza interrompere ciò che sto facendo.
- Quando preparo la spesa, voglio trovare una lista ordinata e comprensibile, non una nota disordinata.
- Quando aggiungo una voce già presente, voglio evitare duplicati e decidere esplicitamente se aumentare la quantità o creare una nuova voce.
- Quando una categoria o un’interpretazione è sbagliata, voglio poter correggere il risultato senza modifiche silenziose.
- Quando ho finito la spesa, voglio depennare i prodotti e ritrovarli in uno storico minimale per poterli riaggiungere in futuro.

### 2.2 Non-utenti nel primo rilascio

- Famiglie o gruppi che richiedono collaborazione simultanea e sincronizzazione cloud.
- Utenti che cercano gestione di prezzi, scontrini, inventario o risparmio.
- Utenti iOS, finché non sarà definita una versione multipiattaforma.

### 2.3 User journeys

- **UJ-1. Mike registra ciò che sta finendo senza interrompersi.**
  - **Persona e contesto:** Mike è a casa, ha le mani impegnate e si accorge che manca un prodotto.
  - **Stato iniziale:** la lista unica “Spesa” esiste; il telefono e il servizio vocale disponibili possono ricevere il comando.
  - **Percorso:** Mike pronuncia un comando per un prodotto; TOGO interpreta prodotto, quantità, unità e categoria; TOGO conferma brevemente quando il risultato è chiaro oppure chiede solo i dati mancanti; Mike conferma o annulla.
  - **Climax:** la voce corretta appare nella categoria prevista, senza duplicare una voce esistente senza consenso.
  - **Risoluzione:** la lista è aggiornata; in caso di errore, ambiguità o annullamento la lista resta invariata.

- **UJ-2. Mike usa la lista durante la spesa.**
  - **Persona e contesto:** Mike è al supermercato e consulta TOGO per completare la spesa.
  - **Stato iniziale:** la lista “Spesa” contiene prodotti raggruppati per categoria.
  - **Percorso:** Mike apre la lista; scorre le categorie; depenna i prodotti acquistati; TOGO conserva gli elementi acquistati nello storico.
  - **Climax:** Mike vede chiaramente cosa resta da acquistare e non deve ricostruire la lista da una nota libera.
  - **Risoluzione:** la lista attiva mostra solo ciò che resta; lo storico consente di riaggiungere un prodotto tramite interfaccia.

- **UJ-3. Mike condivide la lista attiva.**
  - **Persona e contesto:** Mike vuole inviare la lista a un familiare senza condividere il proprio storico.
  - **Percorso:** Mike avvia la condivisione; TOGO genera testo leggibile raggruppato per categoria; il sistema Android apre il canale di condivisione.
  - **Climax:** il destinatario riceve una lista comprensibile dei soli prodotti ancora da acquistare.

## 3. Glossario

- **Lista attiva** — La lista della spesa usata dall’MVP; nel primo rilascio è “Spesa”.
- **Livello 1** — La macro-area principale del catalogo: “Alimentare” o “Non alimentare”.
- **Livello 2** — La sezione di spesa dentro un Livello 1, pensata per seguire lo scaffale o il percorso del supermercato.
- **Livello 3** — La famiglia merceologica dentro un Livello 2, ad esempio Latte, Miele, Caffè o Yogurt.
- **Voce** — Un elemento della Lista attiva con prodotto, quantità, unità, categoria e stato.
- **Famiglia prodotto** — Il prodotto generico che l’utente intende acquistare, ad esempio “Latte UHT”, indipendentemente dalla marca.
- **Prodotto canonico** — La Famiglia prodotto normalizzata con eventuali attributi, marca o variante, usata da TOGO per confrontare voci equivalenti.
- **Marca** — Un attributo opzionale del Prodotto canonico, ad esempio Coop, Conad o Consilia.
- **Variante** — Una specificazione opzionale del Prodotto canonico, ad esempio intero, senza lattosio, biologico o integrale.
- **Attributo prodotto** — Un’informazione opzionale che descrive il Prodotto canonico senza creare una nuova categoria; può usare valori suggeriti o testo libero, ad esempio Marca, Variante o stato di conservazione.
- **Catalogo prodotti** — L’insieme di Livelli 1, Livelli 2, Livelli 3, Prodotti canonici e sinonimi disponibili per la ricerca e la categorizzazione.
- **Categoria canonica** — La quadrupla Livello 1 → Livello 2 → Livello 3 → Prodotto canonico assegnata a una Voce.
- **Quantità normalizzata** — Un valore numerico associato a un’Unità compatibile e coerente con il Prodotto canonico.
- **Unità** — La misura della Quantità normalizzata, ad esempio grammi, litri o pezzi.
- **Conversione autorizzata** — Una conversione tra unità compatibili, come kg → g, l → ml o etto → g; le confezioni non sono automaticamente convertibili in peso o volume.
- **Duplicato potenziale** — Una nuova Voce che corrisponde allo stesso Prodotto canonico nella stessa Categoria canonica specifica.
- **Storico** — L’insieme minimale dei prodotti depennati come acquistati.
- **Regola appresa** — Una correzione locale dell’utente riutilizzabile per interpretare future espressioni, categorie o unità.

## 4. Funzionalità

### 4.1 Catalogo prodotti e tassonomia

TOGO deve partire con un Catalogo prodotti preinstallato che contenga la tassonomia e un insieme ampio di Prodotti canonici generici e sinonimi italiani. La tassonomia deve avere esattamente quattro passaggi concettuali: Livello 1, Livello 2, Livello 3 e Prodotto canonico.

La proposta iniziale è la seguente. È una base operativa da validare con il percorso reale del supermercato, non un elenco immutabile.

**Livello 1: Alimentare**

- **Ortofrutta:** Frutta; Verdura; Patate e tuberi; Erbe aromatiche; Funghi; Frutta secca e semi.
- **Panetteria e pasticceria:** Pane; Panini, piadine e tortillas; Fette biscottate e cracker; Pasticceria e dessert da forno.
- **Carne, pesce e uova:** Carne rossa; Carne bianca; Salumi e affettati; Pesce e frutti di mare; Uova.
- **Gastronomia e piatti pronti:** Gastronomia; Piatti pronti; Insalate pronte; Salse e preparazioni fresche.
- **Banco frigo:** Latte; Yogurt; Formaggi; Burro e margarina; Panna; Dessert refrigerati; Pasta fresca; Alternative vegetali refrigerate.
- **Dispensa:** Pasta, riso e cereali; Legumi; Conserve; Farine, zucchero e preparati; Oli e aceti; Spezie e sale; Salse e condimenti; Miele e marmellate; Caffè, tè e infusi.
- **Colazione e snack:** Biscotti; Cereali da colazione; Merendine; Cioccolato e dolciumi; Snack; Creme spalmabili.
- **Bevande:** Acqua; Bibite; Succhi; Bevande vegetali non refrigerate; Birra; Vino e alcolici.
- **Surgelati:** Verdure; Carne e pesce; Gelati; Piatti pronti; Ghiaccio.
- **Alimentazione per l’infanzia:** Latte per l’infanzia; Omogeneizzati; Pappe e prodotti per bambini.
- **Altro alimentare:** fallback esplicito per ciò che non è ancora classificato.

**Livello 1: Non alimentare**

- **Pulizia della casa:** Superfici; Cucina; Bagno; Vetri; Disinfettanti; Lavastoviglie; Scarichi e manutenzione.
- **Lavanderia:** Detersivi; Ammorbidenti; Smacchiatori; Candeggina; Accessori bucato.
- **Carta e monouso:** Carta igienica; Carta da cucina; Tovaglioli; Fazzoletti; Sacchetti; Pellicola e alluminio; Monouso.
- **Igiene personale:** Saponi; Shampoo e balsamo; Dentifricio e igiene orale; Deodoranti; Rasatura; Assorbenti e igiene intima; Cotone e accessori.
- **Cosmetica:** Viso; Corpo; Capelli; Make-up; Solari.
- **Casa e cucina:** Utensili; Contenitori; Lampadine e batterie; Filtri; Piccoli accessori domestici.
- **Animali:** Cibo; Igiene; Lettiera; Accessori.
- **Cancelleria e tempo libero:** Penne e matite; Carta e quaderni; Adesivi; Piccoli articoli per il tempo libero.
- **Salute e farmacia da banco:** Medicazioni; Integratori; Prodotti per la cura personale non classificati altrove.
- **Altro non alimentare:** fallback esplicito per ciò che non è ancora classificato.

“Sottovuoto” non viene proposto come Livello 2: è una caratteristica di conservazione che può attraversare carne, pesce, formaggi e salumi. È quindi più coerente modellarlo come attributo del Prodotto canonico, non come categoria che frammenta il percorso nel negozio.

Il Catalogo prodotti deve consentire all’utente di aggiungere un nuovo Prodotto canonico dopo aver scelto Livello 1, Livello 2 e Livello 3. Nell’MVP l’utente può anche creare un nuovo Livello 3 dentro un Livello 2 esistente, previa conferma e controllo dei possibili duplicati, mentre Livello 1 e Livello 2 restano governati dal catalogo. “Altro” è un fallback valido e non richiede prodotti iniziali dedicati; TOGO dovrebbe proporre di creare un nuovo Livello 3 quando lo stesso tipo di prodotto ricorre più volte.

La Marca non deve essere obbligatoria per identificare il prodotto della lista. La struttura consigliata è Famiglia prodotto + Marca opzionale + Variante opzionale: “Latte UHT”, “Latte UHT Coop” e “Latte UHT Consilia” restano la stessa famiglia ai fini della categorizzazione e del controllo duplicati, salvo che l’utente scelga esplicitamente di distinguere la marca. Questo evita di dover importare ogni SKU commerciale per avere una lista utile. Le aggiunte create dall’utente non vengono inviate a fonti esterne: nell’MVP restano locali e, in una fase successiva, possono essere inviate esclusivamente al Catalogo centrale TOGO.

Gli Attributi prodotto sono sempre facoltativi: l’utente può inserirli, modificarli o lasciarli vuoti. Devono essere riconosciuti anche dal vocale e modificabili tramite controlli manuali contestuali al Livello 3. Per mantenere il primo catalogo semplice, il set MVP iniziale è limitato a:

- **Marca:** valori comuni selezionabili da tendina, con possibilità di inserire una marca in testo libero.
- **Conservazione/preparazione:** valori comuni come fresco, surgelato, sottovuoto, sottolio, affettato, macinato, grattugiato e pronto; è possibile aggiungere un valore testuale non ancora presente.
- **Variante:** campo facoltativo minimale per qualificatori utili come senza lattosio, integrale, biologico, vegetale o simili, inizialmente selezionabile o compilabile come testo libero.

Gli attributi disponibili devono dipendere dal Livello 3: TOGO non deve mostrare una lunga lista generica di checkbox per ogni prodotto. Un attributo non riconosciuto viene conservato come testo/note da verificare, non crea automaticamente un nuovo Livello 3 o una nuova categoria.

Nell’MVP non esiste una procedura di promozione automatica da Variante ad Attributo o da Variante a Prodotto canonico separato: una Variante resta sempre un valore opzionale del prodotto. Un Prodotto canonico separato viene inserito solo quando si sta descrivendo fin dall’inizio una base di prodotto diversa, non quando si aggiunge un qualificatore. Esempi: “Latte” e “Yogurt” sono prodotti diversi; “Latte intero” e “Latte scremato” sono lo stesso prodotto con Variante; “Rigatoni” e “Spaghetti” sono prodotti diversi della stessa famiglia merceologica Pasta; “pasta integrale” può essere il prodotto Pasta con Variante “integrale”.

Il Catalogo iniziale dell’MVP sarà autonomo e curato internamente da TOGO: non verranno importati nomi derivati da Open Food Facts, GS1 o altre banche dati. Questa scelta evita di legare il primo rilascio a licenze, copertura o qualità di fonti esterne; eventuali riusi futuri richiederanno una nuova valutazione separata. Il catalogo dovrà contenere almeno **1.000 Prodotti canonici alimentari** e **400 Prodotti canonici non alimentari**, per un totale minimo di 1.400 prodotti canonici. Il conteggio riguarda prodotti generici unici, non marche, varianti, formati o sinonimi duplicati.

Il criterio di qualità principale è la genericità utile alla spesa. Il nome canonico deve descrivere la famiglia di prodotto in italiano, eliminando marca, formato, peso/volume, claim commerciali e qualificatori eccessivamente specifici. Per esempio, “latte di soia alta digeribilità” viene ricondotto alla famiglia **Latte di soia**, mentre “riso Roma ai cereali” viene ricondotto a **Riso Roma**. Un qualificatore che può essere utile all’utente non viene necessariamente perso: può restare come sinonimo di ricerca o come Variante/Attributo facoltativo, ma non crea automaticamente una nuova Famiglia prodotto.

La costruzione del catalogo avviene in ordine product-first, non partendo da quote prefissate per categoria: prima vengono individuati e curati i 1.000 Prodotti canonici alimentari e i 400 non alimentari più frequenti nell’uso quotidiano e nell’assortimento della grande distribuzione italiana; solo dopo ogni prodotto viene assegnato a Livello 1, Livello 2 e Livello 3. Al termine dell’assegnazione si verifica che ogni Livello 2 e ogni Livello 3 effettivamente mantenuto nella tassonomia abbia almeno un prodotto e che il totale non sia concentrato in poche sezioni.

Per l’MVP un prodotto è prioritario quando soddisfa almeno due di questi tre segnali editoriali: uso comune e ricorrente in una famiglia italiana; presenza osservabile nell’assortimento di più realtà della grande distribuzione italiana; utilità prevedibile nel percorso di ricerca o nel comando vocale. Il criterio non pretende di rappresentare vendite o quote di mercato e non importa dati commerciali: serve solo a ordinare la curatela manuale. In caso di pari priorità prevale il prodotto che copre un Livello 2 o Livello 3 ancora poco rappresentato.

Ogni Prodotto canonico ha un nome principale obbligatorio e può avere da zero a **tre sinonimi verificati**. I sinonimi iniziali coprono solo forme colloquiali comuni, singolare/plurale, abbreviazioni evidenti o alternative lessicali utili al riconoscimento vocale. Non si inseriscono inizialmente errori casuali o varianti regionali poco frequenti. Marca e Variante restano attributi separati e non vengono duplicati come sinonimi. Un sinonimo non può puntare a più Prodotti canonici; se è ambiguo viene escluso o richiede disambiguazione esplicita.

Ogni Prodotto canonico autonomo deve avere Livello 1, Livello 2, Livello 3, nome canonico italiano e, quando servono, sinonimi utili e Attributi opzionali. La distribuzione minima deve coprire tutta la gerarchia: entrambi i Livelli 1 devono essere rappresentati, ogni Livello 2 attivo deve avere prodotti, ogni Livello 3 usato deve avere almeno un prodotto e i 1.000/400 prodotti devono essere ripartiti tra più sezioni, non concentrati in una sola categoria. Prima di entrare nel catalogo ogni prodotto deve superare questi controlli editoriali: assenza di marca e confezione nel nome canonico, assegnazione tassonomica coerente, assenza di duplicati semantici e ricerca risolta verso una sola famiglia. Le nuove famiglie create dall’utente seguono le stesse regole in forma guidata, ma nell’MVP restano locali.

Il formato editoriale di riferimento è una riga strutturata per Prodotto canonico:

```text
Livello 1: Alimentare
  Livello 2: Dispensa
    Livello 3: Pasta
      Prodotto: Rigatoni
      Marca: Coop, Conad, … (opzionale)
      Conservazione/preparazione: — (opzionale)
      Variante: — (opzionale)
```

Lo stesso schema vale per “Spaghetti”, “Linguine” e gli altri prodotti. La marca non crea una nuova riga canonica obbligatoria: è un attributo selezionabile o inseribile dall’utente e resta separata dal nome generico del prodotto.

La sincronizzazione periodica del catalogo e il barcode scanner restano capacità successive. Il Catalogo preinstallato dell’MVP deve essere una fotografia curata e versionata, non una dipendenza online necessaria per aggiungere una Voce.

#### FR-11: Catalogo iniziale e ricerca

TOGO deve fornire una tassonomia preinstallata di Livelli 1, Livelli 2 e Livelli 3 e un set iniziale di Prodotti canonici con sinonimi ricercabili.

**Conseguenze verificabili:**

- L’utente può navigare o cercare prima il Livello 1, poi il Livello 2, poi il Livello 3 e infine il Prodotto canonico.
- Un Prodotto canonico preinstallato contiene una Categoria canonica coerente e, quando disponibile, sinonimi e marca.
- Il catalogo autonomo contiene almeno 1.000 Prodotti canonici alimentari e 400 Prodotti canonici non alimentari, distribuiti tra Livelli 2, Livelli 3 e prodotti.
- I nomi canonici sono generici; marca, formato e qualificatori opzionali non creano automaticamente famiglie separate.
- L’assenza di rete non impedisce la ricerca nel Catalogo già installato.

#### FR-12: Estensione controllata del catalogo

L’utente può aggiungere un Prodotto canonico non presente scegliendo una Categoria canonica esistente; può proporre un nuovo Livello 3 sotto un Livello 2 esistente.

**Conseguenze verificabili:**

- Il nuovo Prodotto canonico richiede nome e Categoria canonica; i sinonimi sono facoltativi e vengono aggiunti solo quando verificati.
- Un nuovo Livello 3 richiede conferma esplicita, un controllo di possibili duplicati e non può creare un nuovo Livello 1 o Livello 2 nell’MVP.
- Le categorie personalizzate e le Regole apprese sono modificabili o eliminabili dall’utente.

#### FR-13: Attributi del prodotto

TOGO può associare gli Attributi prodotto minimi alla Famiglia prodotto, proponendo valori controllati ma consentendo testo libero, e deve renderli modificabili sia nel percorso vocale sia nell’inserimento manuale.

**Conseguenze verificabili:**

- Il vocale può estrarre Marca, Variante e Conservazione/preparazione quando presenti nella frase.
- Il form manuale mostra solo gli attributi pertinenti al Livello 3 e consente di selezionarli da tendina, inserirli in testo libero, modificarli o lasciarli vuoti.
- Un attributo pronunciato o scritto in modo ambiguo viene mostrato nella conferma e non viene applicato senza scelta esplicita.
- Gli attributi non cambiano Livello 1, Livello 2 o Livello 3 senza una scelta esplicita dell’utente.
- La presenza o assenza della Marca non crea automaticamente una Famiglia prodotto diversa.

### 4.2 Lista attiva e inserimento manuale

TOGO deve offrire una sola Lista attiva denominata “Spesa” nell’MVP. L’inserimento manuale deve permettere di scegliere o cercare il Prodotto canonico, impostare Categoria canonica, Quantità normalizzata e Unità. La tassonomia MVP è composta dai quattro passaggi definiti nel glossario; l’eventuale ridisegno visuale UX non può cambiare questa struttura senza una decisione di prodotto esplicita.

#### FR-1: Creazione della voce manuale

L’utente può aggiungere una Voce alla Lista attiva indicando una Famiglia prodotto/Prodotto canonico, una Categoria canonica, Quantità normalizzata e Unità. Quantità e Unità sono sempre obbligatorie per una Voce salvata e devono essere compilate manualmente tramite valore e selettore dell’Unità quando l’inserimento avviene dall’app. Gli Attributi prodotto restano opzionali, ma sono selezionabili quando pertinenti.

**Conseguenze verificabili:**

- La nuova Voce appare nella Lista attiva e nella Categoria canonica assegnata, con eventuali Attributi prodotto visibili.
- I dati strutturati non vengono conservati soltanto come testo libero.
- L’utente può correggere o annullare l’operazione prima del salvataggio.
- Dopo il salvataggio l’utente può modificare o rimuovere la Voce dalla Lista attiva tramite interfaccia.

#### FR-2: Visualizzazione e ordinamento

L’utente può consultare la Lista attiva raggruppata per Categoria canonica secondo un ordine standard. L’ordine delle categorie segue quello definito nella tassonomia di §4.1; all’interno di ciascuna categoria i Prodotti canonici sono ordinati alfabeticamente per nome.

**Conseguenze verificabili:**

- Le categorie sono visualizzate in modo coerente tra apertura, aggiornamento e condivisione.
- L’ordine delle categorie non richiede configurazione nell’MVP.

### 4.3 Cattura vocale e interpretazione

La voce serve principalmente ad aggiungere un Prodotto canonico per volta. Per l’MVP il percorso raccomandato è un pulsante microfono dentro TOGO che usa il riconoscimento vocale Android impostato su `it-IT`; il testo riconosciuto viene poi interpretato dal parser locale. La piattaforma Android espone `RecognizerIntent` con lingua esplicita e risultati di riconoscimento, ma il riconoscimento può usare servizi remoti e non deve essere considerato offline. App Actions resta un’integrazione opzionale: può aprire l’app o funzioni compatibili, ma i Custom Intent sono documentati come limitati a `en-US`, quindi non sono una base affidabile per il comando libero italiano dell’MVP.

#### FR-3: Aggiunta vocale di un prodotto

L’utente può inviare un comando vocale relativo a un singolo prodotto da aggiungere alla Lista attiva. Il comando deve contenere Famiglia prodotto/Prodotto canonico, valore numerico e Unità, in qualunque ordine naturale; Marca, Variante e altri Attributi prodotto sono opzionali. Se manca uno dei dati obbligatori, TOGO deve chiederlo prima di salvare.

**Conseguenze verificabili:**

- TOGO interpreta Famiglia prodotto, valore numerico e Unità anche quando sono pronunciati in ordine diverso.
- A partire dal prodotto riconosciuto, TOGO completa Livello 1, Livello 2, Livello 3 e Categoria canonica dal Catalogo prodotti.
- TOGO riconosce gli Attributi prodotto pertinenti pronunciati dall’utente e li mostra nella conferma.
- Per un comando chiaro, TOGO restituisce una conferma breve e aggiorna la lista.
- Se il prodotto non è presente nel Catalogo, TOGO non salva una Voce non classificata: propone l’aggiunta guidata del nuovo Prodotto canonico e richiede Livello 1, Livello 2 e Livello 3 prima del salvataggio.
- Se mancano dati necessari o l’interpretazione è ambigua, TOGO chiede un chiarimento mirato senza aggiungere una Voce parziale.

#### FR-4: Conferma e annullamento vocale

L’utente può confermare o annullare un’operazione vocale pendente.

**Conseguenze verificabili:**

- Nessuna modifica viene applicata se l’utente annulla o se la conferma richiesta non è completata.
- Un comando non riconosciuto o interrotto non crea una Voce parziale.
- Le operazioni rischiose non vengono eseguite silenziosamente.

**Fuori scope:** comandi multi-prodotto atomici, wake word proprietario e riconoscimento vocale offline.

### 4.4 Categorie, normalizzazione e duplicati

La categorizzazione è la spina dorsale di TOGO. Ogni Prodotto canonico deve avere una sola Categoria canonica principale. La normalizzazione deve riconoscere sinonimi e rappresentare le quantità come valore più Unità, evitando che “2 l”, “500 g” e “6 pezzi” siano trattati come testo indistinto.

#### FR-5: Assegnazione della categoria

TOGO assegna ogni Prodotto canonico a una Categoria canonica unica e coerente.

**Conseguenze verificabili:**

- Lo stesso Prodotto canonico non viene mostrato in più categorie principali contemporaneamente.
- Se la categoria non è sufficientemente certa, TOGO chiede conferma o permette la correzione.
- Le categorie corrette dall’utente possono essere riutilizzate nelle interpretazioni successive.
- Una Regola appresa non sovrascrive una scelta esplicita appena fatta dall’utente; la correzione corrente prevale e può aggiornare la regola solo con conferma.
- La correzione viene mostrata come una modifica esplicita della Categoria canonica e può essere annullata.
- Dopo la correzione TOGO propone in modo non invasivo: “Ricordo questa scelta per i prossimi inserimenti?”; solo la conferma esplicita crea una Regola appresa.
- La sezione “Regole apprese” mostra espressione, Prodotto canonico, Categoria canonica e data dell’ultima applicazione; ogni regola può essere modificata, disattivata o eliminata.

#### FR-6: Normalizzazione di prodotto e quantità

TOGO normalizza espressioni equivalenti verso un Prodotto canonico, una Quantità normalizzata e un’Unità compatibile.

**Conseguenze verificabili:**

- Le unità compatibili possono essere convertite in un formato canonico coerente: kg ↔ g, l ↔ ml ed etto ↔ g.
- Le Unità MVP includono grammo, chilogrammo, millilitro, litro, unità/pezzo, confezione, pacco, scatola, bottiglia, barattolo, flacone, rotolo e vasetto; le varianti linguistiche sono sinonimi, non nuove unità.
- Confezione, pacco, scatola, bottiglia, barattolo, flacone, rotolo e vasetto non vengono convertiti automaticamente in grammi, millilitri o pezzi.
- Le unità non compatibili non vengono sommate arbitrariamente.
- Unità sconosciute, valori non numerici o quantità minori o uguali a zero richiedono correzione e non consentono il salvataggio.
- La quantità proposta o convertita resta visibile e correggibile dall’utente.
- La Marca e gli altri Attributi prodotto non vengono usati per creare duplicati di categoria; distinguono la preferenza o la variante solo quando l’utente lo richiede.

#### FR-7: Rilevamento di duplicati e scelta della quantità

Prima di creare una Voce, TOGO verifica un possibile duplicato sulla base del Prodotto canonico e della Categoria canonica specifica, non della sola categoria generale.

**Conseguenze verificabili:**

- Se esiste un Duplicato potenziale, TOGO comunica la quantità esistente e chiede se aggiungere la quantità, sostituirla o creare una nuova Voce separata.
- “Aggiungi” applica una Conversione autorizzata solo se le Unità sono compatibili.
- Se le Unità non sono compatibili, TOGO non aggrega le quantità e propone di mantenere una nuova Voce o sostituire quella esistente.
- “Sostituisci” rimpiazza la quantità della Voce esistente solo dopo conferma.
- Marca, Conservazione/preparazione e Variante non distinguono il duplicato per default; entrano nel confronto solo quando l’utente sceglie esplicitamente di distinguere la Voce.
- Una scelta non completata lascia invariata la Lista attiva.
- Prodotti diversi della stessa categoria, come latte e yogurt, non generano automaticamente un avviso di duplicato.
- Un’ambiguità non produce un’unione silenziosa.

### 4.5 Acquisto e storico

Durante la spesa l’utente gestisce principalmente la lista dall’interfaccia. Il depennamento sposta il prodotto nello Storico, che nell’MVP conserva solo le informazioni minime necessarie al riutilizzo.

#### FR-8: Depennamento della voce

L’utente può contrassegnare una Voce come acquistata dalla Lista attiva.

**Conseguenze verificabili:**

- La Voce non appare più tra i prodotti da acquistare.
- Il Prodotto canonico viene registrato nello Storico.
- L’operazione non richiede un comando vocale.

#### FR-9: Riaggiunta dallo storico

L’utente può selezionare un elemento dello Storico e riaggiungerlo alla Lista attiva tramite interfaccia.

**Conseguenze verificabili:**

- Il riutilizzo conserva Categoria canonica e dati strutturati disponibili.
- Il controllo dei Duplicati potenziali viene applicato anche al riaggiunta.

### 4.6 Condivisione testuale

TOGO può condividere la sola Lista attiva attraverso i meccanismi Android disponibili.

#### FR-10: Condivisione della lista

L’utente può generare e condividere un testo della Lista attiva raggruppato per Categoria canonica.

**Conseguenze verificabili:**

- Il testo è leggibile anche senza TOGO e contiene una riga per ogni Voce, senza aggregare voci diverse.
- Sono inclusi solo i prodotti ancora da acquistare.
- Lo Storico e le personalizzazioni private non vengono condivisi.
- Se la Lista attiva è vuota, TOGO mostra uno stato vuoto e non genera una condivisione fuorviante.

### 4.7 Contributi al catalogo centrale (evoluzione)

Nell’MVP esiste solo il catalogo locale preinstallato nell’installazione di Mike; non esiste ancora un server o un Catalogo centrale remoto. Le nuove Famiglie prodotto e i nuovi Livelli 3 vengono salvati localmente e alimentano il test del prodotto. Nel rilascio futuro l’app scarica una versione iniziale del Catalogo centrale al primo avvio; quando un utente aggiunge un elemento accettato come nuovo, l’app invia una proposta al server centrale. Dopo deduplicazione e revisione, l’elemento entra in una nuova versione del catalogo e diventa disponibile agli altri utenti tramite aggiornamento del catalogo o alla prima installazione.

#### FR-14: Proposta di nuovo elemento al catalogo centrale

In una versione successiva, l’utente può inviare una proposta di nuova Famiglia prodotto, Marca, Variante o sinonimo esclusivamente al Catalogo centrale TOGO. La proposta entra in una coda di revisione e non viene pubblicata automaticamente agli altri utenti. La revisione assistita da modello AI è una possibilità futura, esplicitamente rinviata e non necessaria per l’MVP.

Le decisioni su governance, responsabilità di approvazione, soglie e modello AI appartengono a rilasci futuri e non sono requisiti da definire per chiudere l’MVP.

**Conseguenze verificabili:**

- La proposta conserva autore, data, fonte, lingua e dati inseriti.
- Il sistema controlla possibili duplicati prima della pubblicazione.
- In una versione futura, un eventuale modello AI potrà produrre una raccomandazione motivata; la pubblicazione richiederà comunque una responsabilità e una regola di governance definite prima dell’attivazione.
- Una proposta approvata genera una nuova versione del catalogo distribuibile ai dispositivi locali.
- Le correzioni locali dell’utente non vengono sovrascritte senza conflitto esplicito.

**Fuori scope MVP:** backend centrale, moderazione multiutente, sincronizzazione automatica e pubblicazione globale.

## 5. Requisiti non funzionali trasversali

- **Local-first:** il nucleo della lista, delle categorie, delle quantità e dello Storico deve funzionare con dati locali sul dispositivo.
- **Resilienza:** l’indisponibilità del servizio vocale o di un’integrazione opzionale non deve impedire inserimento manuale, consultazione, depennamento e storico.
- **Affidabilità dei dati:** nessuna interpretazione ambigua deve modificare la lista senza conferma esplicita.
- **Usabilità:** il percorso di aggiunta chiaro deve richiedere pochi passaggi; i chiarimenti devono chiedere solo ciò che manca.
- **Accessibilità:** le informazioni mostrate devono avere un equivalente testuale; i comandi vocali non sono l’unico modo per usare il prodotto.
- **Privacy:** i dati della lista e dello Storico restano locali nell’MVP; eventuali servizi esterni devono essere opzionali e trasparenti.
- **Qualità del catalogo:** ogni Prodotto canonico autonomo deve avere nome generico, sinonimi controllati, mappatura tassonomica e controllo dei duplicati prima di diventare disponibile.
- **Dati vocali:** TOGO non conserva registrazioni audio oltre il tempo necessario al riconoscimento; il testo riconosciuto viene usato solo per l’operazione corrente e non viene inviato a un catalogo centrale nell’MVP.
- **Aggiornabilità:** il Catalogo prodotti deve poter essere aggiornato con una nuova fotografia versionata senza sovrascrivere silenziosamente le correzioni locali dell’utente.
- **Contributi futuri:** le aggiunte degli utenti non devono entrare automaticamente nel Catalogo centrale senza deduplicazione, revisione e tracciamento della provenienza.

## 6. Piattaforma e guardrail

- Piattaforma iniziale: Android; Android 16 è il dispositivo di riferimento per i test.
- Percorso vocale MVP raccomandato: microfono interno a TOGO con `RecognizerIntent` e lingua `it-IT`; il parser e la categorizzazione restano nella logica di TOGO.
- App Actions è tecnicamente disponibile su Android 5/API 21+, ma richiede app pubblicata su Google Play e account Google coerenti per i test. I Custom Intent App Actions sono limitati a `en-US`; non sono quindi il percorso primario per il comando libero italiano.
- La componente vocale deve dichiarare chiaramente che il riconoscimento può usare servizi online; il rifiuto o l’assenza del servizio vocale non blocca l’app manuale.
- Il prodotto non deve richiedere AI remota per il nucleo dell’MVP; un eventuale fallback AI è successivo e opzionale.
- Il nucleo non deve dipendere da OAuth, Calendar, Tasks o sincronizzazione cloud nell’MVP.

### 6.1 Evidenze di ricerca

Le fonti sotto riportate documentano alternative valutate e vincoli tecnici o legali; non sono dipendenze del catalogo autonomo MVP.

- [Open Food Facts API](https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/): API v3 indicata come corrente; database open con licenza ODbL, contenuti soggetti a licenza separata e dati volontari non garantiti come completi o accurati.
- La stessa documentazione API indica di scaricare direttamente i dati in formato CSV o JSONL quando servono più di poche centinaia di prodotti, evitando richieste API prodotto-per-prodotto.
- [Open Food Facts: limiti e uso locale](https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/): limiti pubblicati per letture e ricerche, raccomandazione di usare export e database locale per volumi elevati.
- [Open Food Facts: licenza](https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/tutorials/license-be-on-the-legal-side/): obblighi ODbL, licenze dei contenuti e attribuzione da verificare prima della redistribuzione.
- [Open Food Facts: cache locale](https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/tutorials/creating-a-local-cache-of-open-food-facts-data/): il progetto conferma l’uso di cache/dati locali per carichi elevati e raccomanda di non mescolare dati non compatibili con ODbL.
- [Open Food Facts: ricerca per paese](https://openfoodfacts.github.io/documentation/docs/Product-Opener/v2/search/get-search/): l’API espone filtri country-tag e campi localizzati, utili per costruire un seed Italia/italiano.
- [Open Data Commons ODbL](https://opendatacommons.org/licenses/odbl/1-0/): il testo della licenza definisce estrazione, database derivato, attribuzione, share-alike e il rischio che estrazioni ripetute o sistematiche diventino sostanziali; non prevede un’esenzione automatica per il solo fatto di conservare i nomi.
- [Open Products Facts e API barcode universale](https://github.com/openfoodfacts/openfoodfacts-server/blob/main/docs/api/tutorials/scanning-cosmetics-pet-food-and-other-products.md): progetti collegati e ricerca barcode tra Food, Beauty, Pet Food e Products; la copertura resta variabile e la classificazione deve essere verificata.
- [GS1 Italy Immagino](https://servizi.gs1it.org/servizi/immagino/): archivio italiano di dati e immagini di prodotto, con copertura food e non food, accessibile tramite servizio dedicato e non come semplice dataset open.
- [Osservatorio Immagino 2026](https://servizi.gs1it.org/osservatori/osservatorio-immagino-19/): evidenza della scala della base italiana, con oltre 151 mila prodotti analizzati nell’edizione 2026.
- [App Actions per Android](https://developer.android.com/develop/devices/assistant/overview): supporto Android 5+, fulfillment tramite intent e disponibilità legata alla pubblicazione su Google Play.
- [Custom Intent App Actions](https://developer.android.com/develop/devices/assistant/custom-intents): massimo due parametri testuali per query, tipi limitati e supporto solo `en-US`.
- [RecognizerIntent Android](https://developer.android.com/reference/android/speech/RecognizerIntent): supporto alla lingua BCP-47, incluso l’uso di `EXTRA_LANGUAGE`; il riconoscimento può appoggiarsi a server remoti.

La ricerca ha identificato GS1 Italy Immagino come fonte italiana ampia e strutturata, ma non come dataset open gratuito da importare liberamente; Open Food Facts è una fonte collaborativa con copertura non garantita e vincoli di licenza. Queste fonti restano documentate come alternative esaminate, non come dipendenze dell’MVP. La decisione di prodotto è partire con un catalogo autonomo TOGO, scritto e curato internamente, con almeno 1.000 Prodotti canonici alimentari e 400 non alimentari. Il perimetro geografico del catalogo MVP è l’Italia e l’interfaccia è italiana. TOGO non invierà automaticamente i nuovi nomi a Open Food Facts né ad altre fonti: le aggiunte utente confluiranno solo nel catalogo locale o, in futuro, nel Catalogo centrale TOGO.

## 7. Non-obiettivi espliciti

- Non sostituire un inventario domestico completo.
- Non gestire prezzi, scontrini, confezioni commerciali o confronti di risparmio nel primo rilascio.
- Non offrire collaborazione simultanea o sincronizzazione cloud.
- Non supportare iOS, wake word proprietario o riconoscimento vocale offline.
- Non gestire più liste nell’interfaccia MVP.
- Non aggiungere automaticamente prodotti alla lista in base a reminder o inferenze non confermate.

## 8. Perimetro MVP

### 8.1 Incluso

- Una lista iniziale denominata “Spesa”.
- Inserimento manuale.
- Inserimento vocale di un prodotto per comando.
- Categorie canoniche, sinonimi, quantità e unità normalizzate.
- Catalogo preinstallato con Livello 1, Livello 2, Livello 3 e Prodotti canonici; aggiunta controllata di nuovi prodotti e, se necessario, nuovi Livelli 3.
- Attributi prodotto pertinenti, riconoscibili dal vocale e selezionabili manualmente.
- Controllo dei Duplicati potenziali con scelta esplicita dell’utente.
- Correzione e annullamento delle operazioni ambigue o rischiose.
- Depennamento manuale e Storico minimale.
- Riaggiunta dallo Storico tramite interfaccia.
- Condivisione testuale della Lista attiva per Categoria canonica.
- Funzionamento del nucleo con dati locali.

### 8.2 Fuori scope MVP

- Più liste nominate e collaborazione familiare: evoluzione successiva della visione.
- Comandi vocali multi-prodotto e transazioni atomiche complete: da validare dopo il flusso singolo.
- Scansione barcode e caricamento automatico del prodotto: evoluzione successiva.
- Sincronizzazione periodica automatica del Catalogo prodotti: da introdurre dopo aver definito licenze, qualità e processo di revisione.
- Database centrale alimentato dagli utenti e sincronizzazione tra dispositivi: evoluzione successiva, non dipendenza dell’MVP.
- Importazione o pubblicazione automatica verso Open Food Facts, GS1 o altre fonti esterne: fuori scope; il catalogo MVP è autonomo e le aggiunte appartengono al catalogo locale o al futuro Catalogo centrale TOGO.
- Revisione, classificazione o moderazione assistita da AI: fuori scope MVP; da valutare solo insieme al futuro processo di governance del Catalogo centrale.
- Backup/import tramite file: rinviato per mantenere piccolo il primo rilascio.
- Reminder, Calendar e Tasks: il Brief corrente li considera fuori dal perimetro MVP; da rivalutare separatamente.
- AI remota, personalizzazione remota e speech offline: non necessari per validare il nucleo.
- Prezzi, scontrini, confezioni commerciali e confronto costi: linea di evoluzione futura.

## 9. Metriche di successo

Le soglie sotto sono target iniziali di accettazione, da verificare su un set di test rappresentativo e da ricalibrare dopo il primo test d’uso reale.

**Primarie**

- **SM-1:** almeno 95% di assegnazioni corrette della Categoria canonica su un set di almeno 200 prodotti/utterance italiani estratti dal catalogo autonomo e dai casi reali dell’utente; nessuna Voce a bassa confidenza viene salvata senza conferma. Valida FR-5 e FR-7.
- **SM-2:** almeno 98% di precisione nel rilevamento dei duplicati esatti e almeno 95% di richiamo sui duplicati verificati; valida FR-7.
- **SM-3:** tempo mediano massimo di 15 secondi per l’inserimento manuale e 10 secondi per il percorso vocale chiaro, escluso il tempo di lettura della conferma; valida FR-1 e FR-3.

**Secondarie**

- **SM-4:** massimo 15% di comandi vocali che richiedono correzione dopo il chiarimento; valida FR-3 e FR-6.
- **SM-5:** almeno 95% delle Voci acquistate viene depennato correttamente e ritrovato nello Storico; valida FR-8 e FR-9.
- **SM-6:** almeno 90% degli utenti di test giudica leggibile la condivisione con una riga per Voce; valida FR-10.

**Contrometriche**

- **SM-C1:** numero medio di richieste di conferma per sessione; non deve essere minimizzato a scapito della correttezza dei dati.
- **SM-C2:** modifiche non confermate, duplicati creati o categorie errate salvate senza conferma; obiettivo 0 nei test di accettazione.

## 10. Questioni aperte

Non restano decisioni bloccanti per l’MVP. Il criterio di frequenza e il limite di tre sinonimi verificati potranno essere ricalibrati dopo il primo test sui 200 casi e sull’uso reale del vocale, senza modificare la struttura del catalogo.

## 11. Indice delle assunzioni

- §4.1: il Catalogo iniziale è autonomo e punta ad almeno 1.000 Prodotti canonici alimentari e 400 non alimentari, distribuiti tra Livello 2, Livello 3 e prodotti.
- §4.1: il catalogo usa Prodotto canonico generico + Marca/Conservazione/Variante opzionali per evitare di dipendere da ogni SKU commerciale.
- §4.1: la qualità del catalogo dipende dalla genericità del nome canonico, dalla mappatura tassonomica e dalla risoluzione dei sinonimi senza duplicati semantici.
- §4.1: la costruzione iniziale parte dai prodotti più frequenti e assegna la tassonomia solo dopo la selezione e normalizzazione dei prodotti.
- §4.1: la frequenza MVP è valutata con almeno due segnali editoriali su tre; ogni Prodotto canonico ha al massimo tre sinonimi verificati.
- §4.3: il microfono interno Android è confermato come percorso vocale MVP; App Actions resta opzionale.
- §6: il riconoscimento vocale può usare servizi online e la disponibilità concreta dipende dal dispositivo e dal servizio installato.
- §9: il set di 200 casi deve essere costruito a partire dal catalogo autonomo italiano e dai casi reali d’uso.
