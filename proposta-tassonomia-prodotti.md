# Togo — Proposta di tassonomia prodotti a tre livelli

**Stato:** albero e scelte di catalogo consolidati, pronti per l’implementazione. **Riferimento:** checkout `dd11908`, catalogo preinstallato. Ambito: beni che una persona o una famiglia può registrare in una lista di acquisti, dalla spesa ordinaria agli acquisti occasionali. Il documento include la situazione di partenza, l'albero proposto e le decisioni da prendere prima di implementarlo.

## 1. Catalogo attuale e problemi

Il catalogo preinstallato contiene **8 L1, 37 L2, 98 L3 e 748 prodotti canonici**. L'inventario in appendice proviene dalle tabelle SQLite `TAXONOMY_LEVEL_1/2/3` e `CANONICAL_PRODUCT` di `app/src/main/assets/catalog.db`, aperto in sola lettura. Per le installazioni esistenti il percorso di migrazione del catalogo si trova in `CatalogMigration.kt` e nell'asset `catalog_migration_v2.sql`. Le categorie e i prodotti aggiunti sul telefono possono differire da questo inventario.

| L1 attuale | Problema osservato e riformulazione |
|---|---|
| Ortofrutta | Freschezza, confezione e uso culinario si sovrappongono. Mela Fuji e banana risultano sotto «Frutta secca», e il prezzemolo sotto «Insalate e mix». Distinguere famiglie botaniche o di acquisto; spostare frutta essiccata e semi in Dispensa. |
| Panetteria | «Pane speciale» è vago e «senza glutine» ricomprende per errore, tra gli altri, pane pugliese e ciabatta. Dividere pane, pizza/focacce e pasticceria; usare «senza glutine» come attributo trasversale. |
| Carne | I gruppi mescolano specie, taglio e preparazione. Filetto di manzo tra i macinati, coniglio tra i tagli di pollo. Separare carni fresche per famiglia, preparati di carne per forma e salumi per lavorazione. |
| Pesce | «Bianco», «rosso» e «pregiato» non guidano bene la scelta; gamberi sotto Molluschi e tonno sott'olio sotto Affumicati. Separare pesce, molluschi/crostacei e prodotti ittici trasformati. |
| Bevande | Mancano liquori, distillati, aperitivi, cocktail, miscelatori, bevande sportive e altri segmenti. IPA e birra senza alcol finiscono sotto Birra chiara; Prosecco sotto Vino rosso. Separare prima le famiglie per uso, poi stile, trasformazione o assenza di alcol dove cambia l'acquisto. |
| Banco Frigo | Riunisce uova, latticini e surgelati; mette ingredienti diversi sotto lo stesso criterio di conservazione. Latte di soia tra gli yogurt speciali e Grana Padano tra i formaggi a pasta morbida. Separare Latticini, Uova e Gastronomia da Surgelati. |
| Dispensa | Contiene Caffè e tè, legumi insieme alle farine e pasta fresca insieme alla pasta secca. Riso Arborio appare sotto Pasta secca, sale marino sotto Oli. Separare ingredienti, conserve, condimenti, colazione e snack; riunire la pasta fresca in Gastronomia e le bevande da preparare in Bevande. |
| Non alimentare | Un unico reparto raccoglie 140 prodotti in sei L2. Mancano famiglie per stoviglie durevoli, cottura, arredo, profumi per tessili e armadi, elettronica e manutenzione. «Cura della casa» raccoglie monouso, illuminazione e ferramenta. Distinguere i reparti funzionali; separare anche pet food, accessori e lettiere, e le diverse esigenze dell'infanzia. |

Il difetto riguarda anche l'importazione. In `ImportShoppingListUseCase.preview()` un L1 sconosciuto o un L2 assente sotto quel L1 causa errore. Il percorso L3 viene invece cercato nel L2 selezionato: un L3 nuovo può essere proposto per la creazione. La stringa che avevo dato in precedenza usava `[ALIMENTARI]` e `[CASA]`, che non sono L1 del catalogo preinstallato. Quei blocchi non erano quindi importabili così come scritti. Le nuove etichette qui sotto diventeranno utilizzabili dopo l'implementazione.

## 2. Come leggere e integrare la proposta

- **L1 = reparto.** È il primo posto dove cercheresti un prodotto. **L2 = famiglia.** Riunisce prodotti con uno scopo comune. **L3 = tipo.** È la categoria concreta a cui associare più prodotti canonici.
- Ogni riga della sezione 3 rappresenta un L2. Ogni voce separata da punto e virgola è un L3 distinto. I nomi nelle intestazioni `###` sono i L1. Per aggiungere un tipo, inseriscilo nella cella del L2; per una nuova famiglia, aggiungi una riga.
- Una collocazione principale per ogni prodotto aiuta a ritrovarlo in ricerca, storico e import. Marche, gusti, formati, confezioni e caratteristiche come bio, senza glutine e senza lattosio si gestiscono come attributi o termini di ricerca. L'analcolico ha un ramo dedicato quando è un sostituto identificabile di birra, vino o aperitivo. «Zero zuccheri» resta una caratteristica della bibita.
- La classificazione segue la forma acquistata. Il tonno fresco, in scatola e surgelato ha tre collocazioni distinte; piselli freschi, secchi, in scatola e surgelati seguono la stessa regola. Latte UHT e latte fresco stanno nella famiglia Latte, bevande vegetali pronte in Bevande, crema d'avena da cucina fra gli ingredienti di cottura. Un ingrediente destinato all'infanzia, all'animale o a un uso specifico segue il reparto specializzato.
- Per articoli rari, l'ultimo reparto accoglie i beni per cui nessuna famiglia esistente è adatta. Per termini ambigui si usa il significato di acquisto: «magnete» decorativo e magnete tecnico prendono rami distinti; «alcol» da pulizia e bevanda alcolica pure. Un acquisto da eseguire (es. «Far riparare l'orologio») è un'azione in `[DA FARE]`; il ricambio è un prodotto.
- Il perimetro è volutamente ampio ma finito: beni di consumo e durevoli utili a liste personali. Abbonamenti, bollette, prestazioni, immobili, veicoli completi e inventari industriali restano fuori. Per confronto sulla varietà delle famiglie è stata usata la [COICOP 2018 di UNSD](https://unstats.un.org/unsd/classifications/coicop), una classificazione di spesa. I tre livelli qui proposti sono progettati per la ricerca e l'import di Togo.

## 3. Albero proposto

**Dimensione della proposta:** 22 L1, 100 L2 e 455 L3.

### Ortofrutta

| L2 | L3 |
|---|---|
| Frutta fresca | Mele e pere; Agrumi; Frutta a nocciolo; Uva e fichi; Frutti di bosco; Meloni e angurie; Frutta tropicale ed esotica; Altra frutta fresca |
| Ortaggi freschi | Insalate e ortaggi a foglia; Cavoli e crucifere; Pomodori; Peperoni, melanzane e zucchine; Cetrioli e zucche; Finocchi, carciofi e ortaggi a fusto; Patate e altri tuberi; Carote, barbabietole e altre radici; Cipolle, aglio e porri; Legumi freschi; Altri ortaggi freschi |
| Funghi, erbe e germogli | Funghi freschi; Erbe aromatiche fresche; Germogli e microverdure |
| Ortofrutta pronta | Insalate lavate e mix; Verdure tagliate e mix da cuocere; Frutta tagliata e macedonie fresche |

### Pane, pizza e pasticceria

| L2 | L3 |
|---|---|
| Pane e panini | Pane in pagnotta e filoni; Panini e rosette; Pane a fette e da toast; Piadine, tortillas e pani piatti |
| Pizza e forno salato | Pizza e basi precotte; Focacce e schiacciate; Grissini, taralli e prodotti secchi da forno; Rustici e sfoglie salate |
| Pasticceria e forno dolce | Cornetti e brioche; Torte e crostate; Pasticceria fresca e mignon; Dolci da forno e da ricorrenza; Merendine confezionate |

### Carne e salumi

| L2 | L3 |
|---|---|
| Bovino e vitello | Tagli di manzo; Tagli di vitello; Frattaglie bovine |
| Suino | Tagli di maiale; Frattaglie suine |
| Pollame e coniglio | Pollo; Tacchino; Anatra, oca e altro pollame; Coniglio |
| Ovino, caprino e altre carni | Agnello e ovino; Capretto e caprino; Selvaggina; Equino e altre carni |
| Macinati e preparati | Carne macinata; Hamburger e polpette; Salsicce fresche; Spiedini e involtini; Carni marinate e impanate |
| Salumi e carni trasformate | Prosciutti crudi e cotti; Salami e insaccati stagionati; Bresaola, speck e altri salumi stagionati; Mortadella, würstel e carni cotte; Pancetta, guanciale e lardo lavorati; Cotechino e zampone; Carni in scatola e paté |

### Pesce e frutti di mare

| L2 | L3 |
|---|---|
| Pesce fresco | Pesce azzurro; Tonno e pesce spada; Salmone e trota; Merluzzo e nasello; Orata, branzino e altri pesci |
| Molluschi e crostacei | Cozze, vongole e altri bivalvi; Polpo, seppie e calamari; Gamberi e mazzancolle; Scampi, granchi e astici |
| Pesce lavorato e conservato | Pesce in scatola o in vaso; Pesce affumicato e marinato; Pesce salato ed essiccato; Uova di pesce; Surimi e preparati ittici freschi |

### Latte, uova e gastronomia

| L2 | L3 |
|---|---|
| Latte, yogurt e dessert | Latte fresco; Latte UHT; Latte in polvere e condensato; Yogurt e skyr; Kefir e latte fermentato; Budini e dessert refrigerati |
| Formaggi | Mozzarella, burrata e fiordilatte; Ricotta e altri formaggi freschi; Formaggi spalmabili e mascarpone; Formaggi a pasta molle ed erborinati; Formaggi a pasta filata e semidura; Formaggi stagionati e da grattugia |
| Burro, panna e alternative | Burro e burro chiarificato; Panna da cucina e da montare; Creme vegetali da cucina e margarina; Alternative vegetali a yogurt e formaggi |
| Uova | Uova in guscio; Albumi, tuorli e uova liquide |
| Pasta fresca e impasti | Pasta fresca non ripiena; Pasta fresca ripiena; Gnocchi freschi; Impasti freschi per pizza e dolci |
| Gastronomia e piatti freschi | Primi e zuppe refrigerate; Secondi pronti refrigerati; Contorni cotti e insalate composte; Panini, tramezzini e snack freschi; Spalmabili freschi, hummus e guacamole |
| Proteine vegetali | Tofu, tempeh e seitan; Burger vegetali e sostituti della carne refrigerati |

### Surgelati

| L2 | L3 |
|---|---|
| Frutta e verdura surgelate | Verdure e legumi; Patate e contorni; Frutta e frutti di bosco |
| Carne e pesce surgelati | Tagli di carne e pollame; Hamburger e preparati di carne; Filetti e pesce intero; Molluschi, crostacei e preparati ittici |
| Pronti e impasti surgelati | Pizze e focacce; Primi e pasta ripiena; Zuppe e piatti unici; Snack e fritti; Pane, impasti e prodotti da colazione |
| Gelati e ghiaccio | Gelati e semifreddi; Sorbetti e ghiaccioli; Ghiaccio alimentare |

### Dispensa

| L2 | L3 |
|---|---|
| Pasta, riso e cereali | Pasta secca; Riso; Cereali in chicchi; Couscous e bulgur; Polenta e semole pronte; Noodles e altri formati secchi |
| Farine, lieviti e preparati | Farine di cereali; Farine di legumi e alternative; Amidi e fecole; Lieviti e agenti lievitanti; Preparati per pane, pizza e dolci; Ingredienti e decorazioni per pasticceria |
| Legumi, frutta secca e semi | Legumi secchi; Frutta a guscio; Frutta essiccata e disidratata; Semi alimentari |
| Conserve e piatti pronti | Passate, pelati e concentrati; Legumi in scatola e in vaso; Verdure conservate, sott'oli e sottaceti; Olive e capperi; Frutta sciroppata e puree; Zuppe e piatti pronti a temperatura ambiente |
| Oli, condimenti e salse | Oli vegetali; Aceti e glasse; Sale e pepe; Spezie, erbe secche e miscele; Salse fredde e dressing; Sughi, pesti e salse da cucina; Brodi, dadi e fondi; Condimenti e ingredienti fermentati |
| Colazione e dolcificanti | Cereali da colazione e muesli; Fette biscottate; Confetture e marmellate; Miele e sciroppi dolcificanti; Creme spalmabili; Zucchero e dolcificanti |
| Snack e dolci | Cracker e gallette; Patatine, popcorn e snack salati; Biscotti e wafer; Cioccolato e praline; Caramelle e gomme; Barrette e altri snack dolci |
| Ingredienti e prodotti speciali | Alghe alimentari; Surrogati e proteine vegetali secche; Prodotti dietetici e sostitutivi del pasto |

### Bevande

| L2 | L3 |
|---|---|
| Acque | Acqua naturale; Acqua frizzante; Acqua leggermente frizzante; Acque aromatizzate e funzionali |
| Succhi e nettari | Succhi 100%; Nettari e bevande alla frutta; Spremute; Smoothie e centrifugati |
| Bibite analcoliche | Cola; Aranciate, limonate e altre bibite gassate; Tè e caffè freddi pronti; Bibite non gassate; Kombucha e bevande fermentate analcoliche; Aperitivi analcolici e mocktail pronti |
| Sport ed energia | Energy drink; Bevande isotoniche e idratanti; Bevande proteiche pronte; Solubili energetici o per l'idratazione |
| Bevande vegetali | Bevande di soia; Bevande di avena e cereali; Bevande di mandorla e frutta a guscio; Bevande di cocco e altre vegetali |
| Caffè, tè e bevande da preparare | Caffè in grani e macinato; Capsule e cialde; Caffè solubile, orzo e succedanei; Tè e tisane; Cacao e preparati per bevande calde; Polveri per bevande fredde |
| Miscelatori e sciroppi | Toniche e soda; Ginger ale e ginger beer analcolici; Sciroppi e concentrati per bevande; Preparati analcolici per cocktail |
| Birre | Lager e pils; Ale e IPA; Birre di frumento; Birre scure e speciali; Birre analcoliche; Birre non specificate |
| Vini e spumanti | Vini rossi; Vini bianchi; Vini rosati; Vini frizzanti; Spumanti e prosecco spumante; Vini dolci e passiti; Vini fortificati; Vini dealcolati; Vini non specificati |
| Aperitivi e cocktail alcolici | Bitter e aperitivi alcolici; Vermouth e vini aromatizzati; Spritz e cocktail pronti; Premix alcolici; Sidro, idromele e altre bevande fermentate alcoliche |
| Distillati e liquori | Grappa e acquaviti; Gin e vodka; Rum e distillati di canna; Whisky e whiskey; Brandy e cognac; Tequila e mezcal; Amari e digestivi; Liquori all'anice; Liquori alla frutta, alle erbe e ai fiori; Liquori cremosi; Altri distillati e liquori |

### Pulizia e consumabili casa

| L2 | L3 |
|---|---|
| Bucato | Detersivi bucato; Ammorbidenti; Smacchiatori e additivi; Cura della lavatrice |
| Piatti e lavastoviglie | Detersivi per piatti a mano; Detersivi lavastoviglie; Sale, brillantante e cura lavastoviglie |
| Detergenza casa | Pavimenti e superfici; Vetri e specchi; Bagno e WC; Cucina e forno; Sgrassatori, candeggina e disinfettanti per superfici; Anticalcare e disgorganti |
| Accessori per pulire | Spugne, panni e pagliette; Scope, mop e secchi; Guanti da pulizia; Ricambi e sacchetti aspirapolvere |
| Carta, rifiuti e monouso | Carta igienica; Carta da cucina, fazzoletti e tovaglioli; Sacchi rifiuti e riciclo; Carta forno, alluminio e pellicola; Stoviglie e vaschette monouso |
| Profumazione e protezione | Profumi per ambienti e diffusori; Profumi per armadi e tessili; Assorbiumidità e neutralizzatori; Antitarme e antinsetti per casa |
| Stenditura e cura degli indumenti | Mollette, stendini e accessori; Rulli levapelucchi; Cura scarpe e tessili |

### Cucina e tavola

| L2 | L3 |
|---|---|
| Cottura | Pentole e casseruole; Padelle e bistecchiere; Teglie, stampi e pirofile; Coperchi e accessori da cottura |
| Preparazione | Coltelli da cucina e taglieri; Mestoli, spatole e utensili; Grattugie, pelapatate e apriscatole; Colini e scolapasta; Misurini e bilance da cucina; Utensili per pasticceria |
| Tavola e servizio | Piatti e ciotole; Posate; Bicchieri, tazze e calici; Caraffe e accessori da servizio; Accessori vino e bar |
| Conservazione e trasporto | Contenitori alimentari; Barattoli e bottiglie; Borracce e thermos; Borse termiche e portapranzo |
| Piccoli elettrodomestici da cucina | Caffettiere e macchine da caffè; Bollitori e tostapane; Frullatori e robot da cucina; Friggitrici e altri apparecchi di cottura; Filtri e ricambi da cucina |

### Casa e arredamento

| L2 | L3 |
|---|---|
| Arredo e organizzazione | Sedute e tavoli; Mobili e scaffali; Scatole, cesti e organizer; Appendiabiti e accessori armadio |
| Tessili casa | Biancheria letto; Asciugamani e tessili bagno; Tovaglie e tessili cucina; Tende, tappeti e cuscini |
| Decorazione | Cornici e decorazioni domestiche; Magneti e accessori decorativi; Candele decorative e portacandele; Decorazioni stagionali |
| Bagno e accessori | Dispenser e portasapone; Tappetini e accessori doccia; Specchi e organizer bagno |
| Elettrodomestici casa | Lavaggio e asciugatura; Refrigerazione e cottura domestica; Aspirazione e stiro; Ventilazione, riscaldamento e deumidificazione; Ricambi per elettrodomestici casa |

### Igiene e cura della persona

| L2 | L3 |
|---|---|
| Igiene orale | Dentifrici; Spazzolini e testine; Filo e scovolini; Collutori e cura protesi |
| Corpo e igiene personale | Saponi e detergenti mani; Bagnoschiuma e doccia; Deodoranti; Igiene intima; Salviette e fazzoletti personali; Protezioni mestruali |
| Capelli | Shampoo; Balsami e maschere; Styling e colorazione; Spazzole e accessori capelli |
| Viso, pelle e cosmetica | Detersione viso; Creme e trattamenti; Solari e doposole; Trucco e struccanti; Profumi personali; Manicure e pedicure |
| Rasatura e depilazione | Rasoi e lame; Schiume e dopobarba; Prodotti e accessori depilatori |

### Salute e benessere

| L2 | L3 |
|---|---|
| Medicazione e protezione | Cerotti, garze e bende; Disinfettanti per la pelle; Mascherine e guanti di protezione personale; Kit di primo soccorso |
| Misurazione e supporti | Termometri e strumenti di misurazione; Test per uso domestico; Supporti ortopedici e ausili personali |
| Farmacia e integrazione | Farmaci da registrare in lista; Integratori, vitamine e sali; Prodotti per occhi e lenti; Prodotti per naso e gola |
| Cura e assistenza | Prodotti per incontinenza; Assistenza domiciliare e accessori dedicati; Protezione solare specialistica |

### Infanzia

| L2 | L3 |
|---|---|
| Cambio e bagno | Pannolini e mutandine; Salviette e prodotti cambio; Detergenti e creme bimbo; Vasini e accessori bagno |
| Alimentazione | Latte per l'infanzia; Omogeneizzati e pappe; Merende per l'infanzia; Biberon, tettarelle e accessori pappa |
| Riposo e passeggio | Ciucci e dentizione; Tessili e accessori nanna; Passeggini e marsupi; Seggiolini e accessori sicurezza bimbo |

### Animali

| L2 | L3 |
|---|---|
| Alimenti per cani | Secco cane; Umido cane; Snack cane |
| Alimenti per gatti | Secco gatto; Umido gatto; Snack gatto |
| Alimenti altri animali | Alimenti per uccelli; Alimenti per roditori; Alimenti per pesci e altri animali |
| Igiene e cura animali | Lettiere e substrati; Sacchetti e traversine; Detergenti e toelettatura; Cura e protezione animali |
| Accessori animali | Ciotole e distributori; Collari, pettorine e guinzagli; Cucce e trasportini; Giochi e tiragraffi; Accessori acquario e gabbia |

### Ferramenta, manutenzione e giardino

| L2 | L3 |
|---|---|
| Utensili e minuteria | Utensili manuali; Elettroutensili e punte; Viti, chiodi e tasselli; Ganci, cerniere, serrature e magneti tecnici |
| Riparazione e finiture | Colle tecniche e sigillanti; Nastri tecnici e isolanti; Vernici e accessori; Materiali di riparazione; Protezioni per lavoro e bricolage |
| Elettricità, luce e idraulica | Lampadine e lampade tecniche; Pile e batterie comuni; Prese, prolunghe e adattatori di rete; Rubinetteria, guarnizioni e piccoli ricambi idraulici |
| Giardino e balcone | Piante, bulbi e sementi; Terricci e fertilizzanti; Vasi e sottovasi; Irrigazione e attrezzi da giardino; Cura piante e antiparassitari |
| Esterno e barbecue | Mobili da esterno; Griglie e accessori barbecue; Carbone e accendifuoco; Accessori e protezioni da esterno |

### Elettronica e ufficio

| L2 | L3 |
|---|---|
| Telefonia e informatica | Telefoni e tablet; Computer e periferiche; Custodie e protezioni schermo; Cavi, caricabatterie e power bank; Memorie e archiviazione; Rete domestica e domotica |
| Audio, video e fotografia | Cuffie e altoparlanti; TV, telecomandi e accessori video; Fotocamere e accessori foto; Console e accessori gioco elettronico |
| Stampa e materiali ufficio | Stampanti e accessori; Cartucce e toner; Carta da stampa ed etichette |

### Cartoleria, scuola e creatività

| L2 | L3 |
|---|---|
| Scrittura e disegno | Penne e matite; Evidenziatori e pennarelli; Gomme e temperini; Righelli e accessori di misurazione |
| Carta e organizzazione | Quaderni e blocchi; Agende e calendari; Cartelle, raccoglitori e buste; Adesivi ed etichette da scrittura |
| Materiali scolastici e creativi | Astucci e accessori scuola; Colle e nastri per carta; Forbici, cutter e accessori; Colori, pennelli e materiali da modellare; Tessuti, filati e merceria creativa |

### Abbigliamento e accessori

| L2 | L3 |
|---|---|
| Abbigliamento | Abiti e abbigliamento quotidiano; Intimo e calze; Abbigliamento da notte; Capispalla; Abbigliamento tecnico e da lavoro |
| Calzature e accessori indossabili | Scarpe e ciabatte; Solette e lacci; Cinture, cappelli e guanti; Borse e portafogli; Occhiali e accessori; Orologi, cinturini e maglie; Gioielli e bigiotteria |

### Sport, viaggi e mobilità

| L2 | L3 |
|---|---|
| Sport e fitness | Attrezzi da allenamento; Accessori sportivi e protezioni; Palloni e attrezzatura di squadra; Accessori nuoto e sport acquatici |
| Viaggio e attività all'aperto | Valigie e zaini da viaggio; Accessori da viaggio; Tende e materassini da campeggio; Attrezzatura per escursione e campeggio |
| Auto, moto e bici | Cura e pulizia veicoli; Lubrificanti e liquidi veicoli; Accessori e ricambi auto e moto; Accessori e ricambi bici; Lucchetti e sistemi di sicurezza mobilità |

### Giochi, cultura, feste e regali

| L2 | L3 |
|---|---|
| Giochi e hobby | Giocattoli; Giochi da tavolo e carte; Puzzle e costruzioni; Modellismo, collezionismo e hobby; Strumenti musicali e accessori |
| Lettura e supporti | Libri; Riviste e fumetti; Supporti musicali e video |
| Feste e regalo | Addobbi e articoli da festa; Biglietti e confezioni regalo; Regali da definire |

### Altri acquisti

| L2 | L3 |
|---|---|
| Beni da classificare | Alimentari da classificare; Non alimentari da classificare |

## 4. Esempi e regole di confine

| Prodotto | Percorso proposto |
|---|---|
| Liquore Hugo | Sinonimo di Liquore al sambuco → Bevande → Distillati e liquori → Liquori alla frutta, alle erbe e ai fiori. Il cocktail pronto è una voce separata chiamata Hugo pronto da bere. |
| Anisetta | Bevande → Distillati e liquori → Liquori all'anice |
| Padella griglia | Cucina e tavola → Cottura → Padelle e bistecchiere |
| Magnete per frigorifero | Casa e arredamento → Decorazione → Magneti e accessori decorativi, se è un articolo ornamentale. Un magnete di ricambio o da fissaggio va in Ferramenta. |
| Profumazione per armadio | Pulizia e consumabili casa → Profumazione e protezione → Profumi per armadi e tessili |
| Prosecco | Bevande → Vini e spumanti → Spumanti e prosecco spumante, se spumante; Vini frizzanti se il prodotto è indicato come frizzante. |
| Caffè in capsule e caffè freddo | Il primo va nelle preparazioni, il secondo tra le bevande pronte. |
| Ghiacciolo e ghiaccio per cocktail | Surgelati → Gelati e ghiaccio, rispettivamente Sorbetti e ghiaccioli e Ghiaccio alimentare. |
| Umido per gatto e lettiera | Animali → Alimenti per gatti → Umido gatto; Animali → Igiene e cura animali → Lettiere e substrati. |
| Pannolini e protezioni per adulti | Infanzia → Cambio e bagno → Pannolini e mutandine; Salute e benessere → Cura e assistenza → Prodotti per incontinenza. |
| Profumatore per auto | Sport, viaggi e mobilità → Auto, moto e bici → Cura e pulizia veicoli; un diffusore per soggiorno segue invece la profumazione casa. |
| Lampadina e lampada decorativa | Le fonti luminose e i ricambi seguono Ferramenta. Un apparecchio scelto come complemento d'arredo segue Casa e arredamento → Decorazione → Cornici e decorazioni domestiche. |

## 5. Indicazioni per l’implementazione

1. **Visibilità dei reparti.** L'albero completo comprende anche acquisti occasionali. Per i selettori, decidere se mostrarli tutti allo stesso livello o consentire di preferire i reparti abituali. L'ordine della sezione 3 è una proposta iniziale.
2. **Riclassificazione dei prodotti canonici.** Assegnare i prodotti per nome e significato. Per esempio, riassegnare Mela Fuji a Mele e pere, Filetto di manzo a Tagli di manzo, Grana Padano a Formaggi stagionati e da grattugia. Il vecchio L3 serve come indizio, perché alcune categorie contengono prodotti eterogenei. Usare il mapping definitivo della sezione 7 e le esclusioni della sezione 9.
3. **Dati personali.** Definire la destinazione dei L3 personalizzati e dei prodotti utente prima di muovere o ritirare i rami. Mantenere i riferimenti di lista attiva, storico, sinonimi e regole apprese. In un ramo legacy ambiguo occorre una scelta esplicita o un percorso di recupero.
4. **Import ed export.** La proposta è mantenere il formato a tre livelli. Per i vecchi export valutare alias sul percorso completo per rinomine univoche; nei vecchi rami frazionati occorre riconoscere il prodotto o chiedere una scelta. Un alias di solo L1 o L2 può indirizzare una voce nel reparto sbagliato. Verificare sia acquisti sotto nuovi rami sia liste miste con `[DA FARE]`.
5. **Reparti e prodotti evolvono separatamente.** Concordare quali foglie possono partire senza prodotti; aggiungere i prodotti ricorrenti al seed e arricchire i sinonimi dove migliorano la ricerca. Una categoria nuova, ad esempio Liquori all'anice, non implica da sola che Anisetta sia già un prodotto canonico.
6. **Confini del catalogo.** Salute, elettronica, abbigliamento e mobilità sono proposti per registrare acquisti domestici e personali. Si possono accorpare prima dell'implementazione se la navigazione della spesa quotidiana risulta troppo frammentata.

## 6. Destinazione delle famiglie esistenti

Questa mappa copre tutti i 37 L2 preinstallati. Indica dove cercare le destinazioni per le loro voci, prima di costruire il mapping dei singoli prodotti. L'appendice conserva ogni percorso originale fino a L3.

| L1 attuale | L2 attuale | Famiglie di destinazione prevalenti |
|---|---|---|
| Ortofrutta | Frutta | Frutta fresca; Legumi, frutta secca e semi (Dispensa) per i prodotti effettivamente secchi. |
| Ortofrutta | Verdura | Ortaggi freschi; Ortofrutta pronta per i prodotti lavorati, come i mix lavati. |
| Ortofrutta | Erbe e contorni | Funghi, erbe e germogli; Ortofrutta pronta; Conserve e piatti pronti per le olive conservate. |
| Panetteria | Pane | Pane e panini; Pizza e forno salato per focacce e grissini. |
| Panetteria | Panificati | Pane e panini; Pizza e forno salato; Pasticceria e forno dolce. |
| Panetteria | Dolci da forno | Pasticceria e forno dolce; Snack e dolci per i prodotti dolciari da dispensa. |
| Carne | Carne bovina | Bovino e vitello; Macinati e preparati; Gastronomia e piatti freschi per le pietanze già pronte. |
| Carne | Carne suina | Suino; Macinati e preparati; Salumi e carni trasformate. |
| Carne | Pollame | Pollame e coniglio; Macinati e preparati; Ovino, caprino e altre carni se la specie lo richiede. |
| Carne | Salumi freschi | Macinati e preparati per le salsicce fresche; Salumi e carni trasformate per cotechini, zamponi e salumi. |
| Pesce | Pesce fresco | Pesce fresco, diviso per famiglia. |
| Pesce | Pesce confezionato | Pesce lavorato e conservato; distinguere scatola, affumicatura, salatura e preparati. |
| Pesce | Crostacei e molluschi | Molluschi e crostacei; correggere i gruppi rovesciati in base alla specie. |
| Bevande | Acqua e minerali | Acque; separare effervescenza e aromatizzazione. |
| Bevande | Succhi e nettari | Succhi e nettari; separare succo, nettare e spremuta quando il nome lo consente. |
| Bevande | Bibite | Bibite analcoliche; Sport ed energia; Miscelatori e sciroppi. |
| Bevande | Birra | Birre; distinguere stile e analcolico. |
| Bevande | Vino | Vini e spumanti; usare l'etichetta del prodotto per colore e tipologia. |
| Banco Frigo | Latte e yogurt | Latte, yogurt e dessert; Bevande vegetali; Formaggi per le voci assegnate per errore. |
| Banco Frigo | Formaggi | Formaggi; distinguere freschezza, pasta e stagionatura. |
| Banco Frigo | Burro e panna | Burro, panna e alternative. |
| Banco Frigo | Surgelati | Frutta e verdura surgelate; Carne e pesce surgelati; Pronti e impasti surgelati; Gelati e ghiaccio. |
| Banco Frigo | Uova | Uova; distinguere guscio e derivati. |
| Dispensa | Pasta e riso | Pasta, riso e cereali; Pasta fresca e impasti per la pasta acquistata come fresca. |
| Dispensa | Oli e condimenti | Oli, condimenti e salse; riclassificare i prodotti secondo il tipo. |
| Dispensa | Conserve | Conserve e piatti pronti; Oli, condimenti e salse se si tratta di sughi pronti. |
| Dispensa | Snack e cracker | Snack e dolci; Legumi, frutta secca e semi per la frutta a guscio. |
| Dispensa | Dolci e cioccolato | Snack e dolci; Colazione e dolcificanti; Pasticceria e forno dolce per le merendine. |
| Dispensa | Caffè e tè | Caffè, tè e bevande da preparare (Bevande). |
| Dispensa | Legumi e farine | Legumi, frutta secca e semi; Farine, lieviti e preparati; Pasta, riso e cereali per orzo e farro. |
| Dispensa | Salse e pesti | Oli, condimenti e salse; Gastronomia e piatti freschi per hummus e guacamole refrigerati. |
| Non alimentare | Igiene persona | Famiglie di Igiene e cura della persona; Farmacia e integrazione per le soluzioni per lenti; Bucato per il detergente fuori reparto. |
| Non alimentare | Pulizia e lavatrice | Bucato; Piatti e lavastoviglie; Detergenza casa; Accessori per pulire; Carta, rifiuti e monouso; Profumazione e protezione. |
| Non alimentare | Cura della casa | Carta, rifiuti e monouso; Stenditura e cura degli indumenti; Elettricità, luce e idraulica; Utensili e minuteria; Decorazione. |
| Non alimentare | Cartoleria | Scrittura e disegno; Carta e organizzazione; Materiali scolastici e creativi. |
| Non alimentare | Articoli per animali | Alimenti per cani, gatti e altri animali; Igiene e cura animali; Accessori animali. |
| Non alimentare | Bambini e neonati | Cambio e bagno; Alimentazione; Riposo e passeggio. |

## 7. Riclassificazione dei 607 prodotti canonici mantenuti

Ogni riga contiene il nome da usare nel catalogo, l’ID mantenuto e la destinazione proposta. La sezione 9 elenca le esclusioni dal catalogo e la sezione 10 contiene gli accorpamenti Bio. Il titolo sotto cui compare la riga è il suo L1. Gli ID sono le chiavi del catalogo incluso nell’asset. Per cambiare una destinazione modifica L2 e L3, oppure sposta la riga sotto un altro L1. Il nome coincide con quello dell’asset, salvo le eventuali rinomine esplicite nella sezione 10.

**Revisione completata.** Le righe «Confermato» incorporano le decisioni dell’utente. Le righe «Assegnato» mantengono la destinazione già proposta.

### Ortofrutta

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Frutta fresca | Mele e pere | 1 | Mela Golden | Assegnato |
| Frutta fresca | Mele e pere | 2 | Mela Renetta | Assegnato |
| Frutta fresca | Mele e pere | 3 | Mela Fuji | Assegnato |
| Frutta fresca | Mele e pere | 4 | Pera Williams | Assegnato |
| Frutta fresca | Mele e pere | 5 | Pera Conference | Assegnato |
| Frutta fresca | Agrumi | 7 | Arancia | Assegnato |
| Frutta fresca | Agrumi | 8 | Mandarina | Assegnato |
| Frutta fresca | Agrumi | 9 | Limone | Assegnato |
| Frutta fresca | Frutta a nocciolo | 17 | Albicocca | Assegnato |
| Frutta fresca | Frutta a nocciolo | 18 | Pesca | Assegnato |
| Frutta fresca | Frutta a nocciolo | 19 | Pesca Nettarina | Assegnato |
| Frutta fresca | Frutta a nocciolo | 20 | Prugna | Assegnato |
| Frutta fresca | Frutta a nocciolo | 21 | Ciliegia | Assegnato |
| Frutta fresca | Uva e fichi | 22 | Uva Bianca | Assegnato |
| Frutta fresca | Uva e fichi | 23 | Uva Nera | Assegnato |
| Frutta fresca | Uva e fichi | 27 | Fico | Assegnato |
| Frutta fresca | Frutti di bosco | 11 | Fragola | Assegnato |
| Frutta fresca | Frutti di bosco | 12 | Lampone | Assegnato |
| Frutta fresca | Frutti di bosco | 13 | Mirtillo | Assegnato |
| Frutta fresca | Meloni e angurie | 14 | Anguria | Assegnato |
| Frutta fresca | Meloni e angurie | 15 | Melone Cantalupo | Assegnato |
| Frutta fresca | Meloni e angurie | 16 | Melone Retato | Assegnato |
| Frutta fresca | Frutta tropicale ed esotica | 6 | Banana | Assegnato |
| Frutta fresca | Frutta tropicale ed esotica | 24 | Ananas | Assegnato |
| Frutta fresca | Frutta tropicale ed esotica | 25 | Mango | Assegnato |
| Frutta fresca | Frutta tropicale ed esotica | 30 | Cocco | Assegnato |
| Frutta fresca | Altra frutta fresca | 10 | Kiwi | Assegnato |
| Frutta fresca | Altra frutta fresca | 26 | Melagrana | Assegnato |
| Frutta fresca | Altra frutta fresca | 28 | Castagna | Assegnato |
| Ortaggi freschi | Insalate e ortaggi a foglia | 44 | Lattuga | Assegnato |
| Ortaggi freschi | Insalate e ortaggi a foglia | 45 | Rucola | Assegnato |
| Ortaggi freschi | Insalate e ortaggi a foglia | 46 | Iceberg | Assegnato |
| Ortaggi freschi | Insalate e ortaggi a foglia | 47 | Radicchio | Assegnato |
| Ortaggi freschi | Insalate e ortaggi a foglia | 64 | Cicoria | Assegnato |
| Ortaggi freschi | Insalate e ortaggi a foglia | 68 | Spinaci | Assegnato |
| Ortaggi freschi | Cavoli e crucifere | 61 | Broccoli | Assegnato |
| Ortaggi freschi | Cavoli e crucifere | 62 | Cavolfiore | Assegnato |
| Ortaggi freschi | Cavoli e crucifere | 63 | Cavolo Nero | Assegnato |
| Ortaggi freschi | Pomodori | 41 | Pomodoro Rametto | Assegnato |
| Ortaggi freschi | Pomodori | 42 | Pomodoro Datterino | Assegnato |
| Ortaggi freschi | Pomodori | 43 | Pomodoro Cuore di Bue | Assegnato |
| Ortaggi freschi | Peperoni, melanzane e zucchine | 54 | Zucchina | Assegnato |
| Ortaggi freschi | Peperoni, melanzane e zucchine | 55 | Melanzana | Assegnato |
| Ortaggi freschi | Peperoni, melanzane e zucchine | 56 | Peperone Rosso | Assegnato |
| Ortaggi freschi | Peperoni, melanzane e zucchine | 57 | Peperone Verde | Assegnato |
| Ortaggi freschi | Peperoni, melanzane e zucchine | 58 | Peperoncino | Confermato |
| Ortaggi freschi | Cetrioli e zucche | 69 | Cetriolo | Assegnato |
| Ortaggi freschi | Cetrioli e zucche | 70 | Zucca | Assegnato |
| Ortaggi freschi | Finocchi, carciofi e ortaggi a fusto | 59 | Sedano | Assegnato |
| Ortaggi freschi | Finocchi, carciofi e ortaggi a fusto | 60 | Finocchio | Assegnato |
| Ortaggi freschi | Patate e altri tuberi | 49 | Patata | Assegnato |
| Ortaggi freschi | Patate e altri tuberi | 50 | Patata Dolce | Assegnato |
| Ortaggi freschi | Carote, barbabietole e altre radici | 48 | Carota | Assegnato |
| Ortaggi freschi | Carote, barbabietole e altre radici | 65 | Barbabietola | Assegnato |
| Ortaggi freschi | Carote, barbabietole e altre radici | 66 | Rapa | Assegnato |
| Ortaggi freschi | Cipolle, aglio e porri | 51 | Cipolla Rossa | Assegnato |
| Ortaggi freschi | Cipolle, aglio e porri | 52 | Cipolla Bianca | Assegnato |
| Ortaggi freschi | Cipolle, aglio e porri | 53 | Aglio | Assegnato |
| Ortaggi freschi | Cipolle, aglio e porri | 67 | Porro | Assegnato |
| Funghi, erbe e germogli | Erbe aromatiche fresche | 81 | Basilico | Assegnato |
| Funghi, erbe e germogli | Erbe aromatiche fresche | 82 | Prezzemolo | Assegnato |
| Funghi, erbe e germogli | Erbe aromatiche fresche | 83 | Rosmarino | Assegnato |
| Funghi, erbe e germogli | Erbe aromatiche fresche | 84 | Salvia | Assegnato |
| Funghi, erbe e germogli | Erbe aromatiche fresche | 85 | Origano | Confermato |
| Funghi, erbe e germogli | Erbe aromatiche fresche | 86 | Menta | Assegnato |
| Funghi, erbe e germogli | Erbe aromatiche fresche | 87 | Coriandolo | Assegnato |
| Ortofrutta pronta | Insalate lavate e mix | 88 | Insalata Mista | Assegnato |
| Ortofrutta pronta | Verdure tagliate e mix da cuocere | 89 | Mix per Wok | Assegnato |



### Pane, pizza e pasticceria

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Pane e panini | Pane in pagnotta e filoni | 121 | Pane Casereccio | Assegnato |
| Pane e panini | Pane in pagnotta e filoni | 122 | Pane Toscano | Assegnato |
| Pane e panini | Pane in pagnotta e filoni | 123 | Pane Pugliese | Assegnato |
| Pane e panini | Pane in pagnotta e filoni | 124 | Pane di Segale | Assegnato |
| Pane e panini | Pane in pagnotta e filoni | 125 | Pane Integrale | Assegnato |
| Pane e panini | Pane in pagnotta e filoni | 126 | Pane Francese | Assegnato |
| Pane e panini | Pane in pagnotta e filoni | 129 | Ciabatta | Assegnato |
| Pane e panini | Pane in pagnotta e filoni | 134 | Pane ai Cereali | Assegnato |
| Pane e panini | Pane in pagnotta e filoni | 136 | Pane | Assegnato |
| Pane e panini | Panini e rosette | 130 | Panino Integrale | Assegnato |
| Pane e panini | Panini e rosette | 135 | Panino al Sesamo | Assegnato |
| Pane e panini | Panini e rosette | 153 | Pane al Latte | Assegnato |
| Pane e panini | Piadine, tortillas e pani piatti | 133 | Pane Azzimo | Assegnato |
| Pizza e forno salato | Focacce e schiacciate | 127 | Focaccia Genovese | Assegnato |
| Pizza e forno salato | Focacce e schiacciate | 128 | Focaccia Rossa | Assegnato |
| Pizza e forno salato | Grissini, taralli e prodotti secchi da forno | 131 | Grissini | Assegnato |
| Pizza e forno salato | Grissini, taralli e prodotti secchi da forno | 132 | Grissini Integrali | Assegnato |
| Pizza e forno salato | Grissini, taralli e prodotti secchi da forno | 860 | Taralli | Assegnato |
| Pizza e forno salato | Rustici e sfoglie salate | 160 | Panzerotto | Assegnato |
| Pasticceria e forno dolce | Cornetti e brioche | 151 | Croissant | Assegnato |
| Pasticceria e forno dolce | Cornetti e brioche | 152 | Cornetto | Assegnato |
| Pasticceria e forno dolce | Cornetti e brioche | 154 | Brioche | Assegnato |
| Pasticceria e forno dolce | Cornetti e brioche | 157 | Saccottino | Assegnato |
| Pasticceria e forno dolce | Cornetti e brioche | 158 | Treccia | Confermato |
| Pasticceria e forno dolce | Cornetti e brioche | 159 | Fagottino | Confermato |
| Pasticceria e forno dolce | Cornetti e brioche | 190 | Brioche Farcita | Assegnato |
| Pasticceria e forno dolce | Torte e crostate | 181 | Tortina | Assegnato |
| Pasticceria e forno dolce | Torte e crostate | 186 | Torta allo Yogurt | Assegnato |
| Pasticceria e forno dolce | Torte e crostate | 187 | Crostatina | Assegnato |
| Pasticceria e forno dolce | Pasticceria fresca e mignon | 155 | Krapfen | Assegnato |
| Pasticceria e forno dolce | Pasticceria fresca e mignon | 162 | Bombolone | Assegnato |
| Pasticceria e forno dolce | Pasticceria fresca e mignon | 188 | Eclair | Assegnato |
| Pasticceria e forno dolce | Pasticceria fresca e mignon | 189 | Tartufo Dolce | Assegnato |
| Pasticceria e forno dolce | Dolci da forno e da ricorrenza | 156 | Muffin | Assegnato |
| Pasticceria e forno dolce | Dolci da forno e da ricorrenza | 161 | Sfogliatina | Confermato |
| Pasticceria e forno dolce | Dolci da forno e da ricorrenza | 184 | Ciambella | Assegnato |
| Pasticceria e forno dolce | Dolci da forno e da ricorrenza | 185 | Pan di Spagna | Assegnato |
| Pasticceria e forno dolce | Merendine confezionate | 182 | Merenda Dolce | Assegnato |
| Pasticceria e forno dolce | Merendine confezionate | 896 | Merendine | Assegnato |
| Pasticceria e forno dolce | Merendine confezionate | 898 | Tortina al Cioccolato | Assegnato |

### Carne e salumi

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Bovino e vitello | Tagli di manzo | 211 | Tagliata di Manzo | Assegnato |
| Bovino e vitello | Tagli di manzo | 212 | Filetto di Manzo | Assegnato |
| Bovino e vitello | Tagli di manzo | 213 | Carpaccio | Confermato |
| Bovino e vitello | Tagli di manzo | 216 | Costine di Manzo | Assegnato |
| Bovino e vitello | Tagli di manzo | 220 | Trancio di Manzo | Assegnato |
| Bovino e vitello | Tagli di manzo | 221 | Sottofiletto | Assegnato |
| Bovino e vitello | Tagli di manzo | 223 | Straccetti di Manzo | Assegnato |
| Bovino e vitello | Tagli di manzo | 225 | Bistecca | Assegnato |
| Bovino e vitello | Tagli di manzo | 226 | Manzo | Assegnato |
| Suino | Tagli di maiale | 241 | Costine di Maiale | Assegnato |
| Suino | Tagli di maiale | 244 | Lonza di Maiale | Assegnato |
| Suino | Tagli di maiale | 245 | Lombo di Maiale | Assegnato |
| Suino | Tagli di maiale | 250 | Filetto di Maiale | Assegnato |
| Suino | Tagli di maiale | 251 | Straccetti di Maiale | Assegnato |
| Suino | Tagli di maiale | 252 | Tagliatelle di Maiale | Assegnato |
| Suino | Tagli di maiale | 253 | Maiale | Assegnato |
| Suino | Tagli di maiale | 307 | Polpa di Suino | Assegnato |
| Pollame e coniglio | Pollo | 271 | Petto di Pollo | Assegnato |
| Pollame e coniglio | Pollo | 272 | Cosce di Pollo | Assegnato |
| Pollame e coniglio | Pollo | 273 | Sovracosce | Assegnato |
| Pollame e coniglio | Pollo | 274 | Ali di Pollo | Assegnato |
| Pollame e coniglio | Pollo | 280 | Straccetti di Pollo | Assegnato |
| Pollame e coniglio | Pollo | 283 | Pollo | Assegnato |
| Pollame e coniglio | Tacchino | 275 | Tacchino a Fette | Assegnato |
| Pollame e coniglio | Tacchino | 281 | Coscia di Tacchino | Assegnato |
| Pollame e coniglio | Tacchino | 282 | Petto di Tacchino | Assegnato |
| Pollame e coniglio | Anatra, oca e altro pollame | 276 | Faraona | Assegnato |
| Pollame e coniglio | Coniglio | 277 | Coniglio | Assegnato |
| Macinati e preparati | Hamburger e polpette | 214 | Hamburger di Manzo | Assegnato |
| Macinati e preparati | Hamburger e polpette | 215 | Polpettone | Assegnato |
| Macinati e preparati | Hamburger e polpette | 279 | Hamburger di Pollo | Assegnato |
| Macinati e preparati | Salsicce fresche | 246 | Salsiccia | Assegnato |
| Macinati e preparati | Salsicce fresche | 278 | Salsiccia di Pollo | Assegnato |
| Macinati e preparati | Salsicce fresche | 301 | Salsiccia Fresca | Assegnato |
| Macinati e preparati | Salsicce fresche | 304 | Luganiga | Assegnato |
| Macinati e preparati | Salsicce fresche | 305 | Salsiccia di Suino | Assegnato |
| Macinati e preparati | Salsicce fresche | 310 | Salsiccia Piccante | Assegnato |
| Macinati e preparati | Salsicce fresche | 311 | Salsiccia Dolce | Assegnato |
| Macinati e preparati | Spiedini e involtini | 222 | Involtino di Manzo | Assegnato |
| Salumi e carni trasformate | Salami e insaccati stagionati | 308 | Coppa di Suino | Confermato |
| Salumi e carni trasformate | Mortadella, würstel e carni cotte | 248 | Porchetta | Assegnato |
| Salumi e carni trasformate | Mortadella, würstel e carni cotte | 306 | Bolognese Fresco | Confermato |
| Salumi e carni trasformate | Mortadella, würstel e carni cotte | 312 | Wurstel Fresco | Assegnato |
| Salumi e carni trasformate | Pancetta, guanciale e lardo lavorati | 242 | Pancetta | Assegnato |
| Salumi e carni trasformate | Pancetta, guanciale e lardo lavorati | 243 | Guanciale | Assegnato |
| Salumi e carni trasformate | Pancetta, guanciale e lardo lavorati | 249 | Pancetta Arrotolata | Assegnato |
| Salumi e carni trasformate | Cotechino e zampone | 302 | Cotechino | Assegnato |
| Salumi e carni trasformate | Cotechino e zampone | 303 | Zampone | Assegnato |

### Pesce e frutti di mare

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Pesce fresco | Pesce azzurro | 337 | Acciughe | Assegnato |
| Pesce fresco | Pesce azzurro | 338 | Sardine | Assegnato |
| Pesce fresco | Pesce azzurro | 345 | Sgombro | Assegnato |
| Pesce fresco | Tonno e pesce spada | 332 | Tonno Fresco | Assegnato |
| Pesce fresco | Tonno e pesce spada | 341 | Pesce Spada | Assegnato |
| Pesce fresco | Tonno e pesce spada | 365 | Tonno | Confermato |
| Pesce fresco | Salmone e trota | 331 | Salmone Fresco | Assegnato |
| Pesce fresco | Salmone e trota | 344 | Trota | Assegnato |
| Pesce fresco | Merluzzo e nasello | 336 | Merluzzo | Assegnato |
| Pesce fresco | Orata, branzino e altri pesci | 333 | Branzino | Assegnato |
| Pesce fresco | Orata, branzino e altri pesci | 334 | Orata | Assegnato |
| Pesce fresco | Orata, branzino e altri pesci | 335 | Spigola | Assegnato |
| Pesce fresco | Orata, branzino e altri pesci | 339 | Triglia | Assegnato |
| Pesce fresco | Orata, branzino e altri pesci | 340 | Cefalo | Assegnato |
| Pesce fresco | Orata, branzino e altri pesci | 342 | Ricciola | Assegnato |
| Pesce fresco | Orata, branzino e altri pesci | 343 | Branzino di Allevamento | Assegnato |
| Molluschi e crostacei | Cozze, vongole e altri bivalvi | 383 | Cozze | Assegnato |
| Molluschi e crostacei | Cozze, vongole e altri bivalvi | 384 | Vongole Veraci | Assegnato |
| Molluschi e crostacei | Polpo, seppie e calamari | 385 | Seppie | Assegnato |
| Molluschi e crostacei | Polpo, seppie e calamari | 386 | Calamari | Assegnato |
| Molluschi e crostacei | Polpo, seppie e calamari | 387 | Polpo | Assegnato |
| Molluschi e crostacei | Gamberi e mazzancolle | 378 | Gamberi Rossi | Assegnato |
| Molluschi e crostacei | Gamberi e mazzancolle | 379 | Gamberi Tigre | Assegnato |
| Molluschi e crostacei | Gamberi e mazzancolle | 382 | Mazzancolle | Assegnato |
| Molluschi e crostacei | Scampi, granchi e astici | 380 | Scampi | Assegnato |
| Molluschi e crostacei | Scampi, granchi e astici | 381 | Astici | Assegnato |
| Pesce lavorato e conservato | Pesce in scatola o in vaso | 355 | Tonno in Olio | Assegnato |
| Pesce lavorato e conservato | Pesce in scatola o in vaso | 356 | Tonno al Naturale | Assegnato |
| Pesce lavorato e conservato | Pesce in scatola o in vaso | 358 | Sgombro in Olio | Assegnato |
| Pesce lavorato e conservato | Pesce in scatola o in vaso | 359 | Sardine in Olio | Assegnato |
| Pesce lavorato e conservato | Pesce in scatola o in vaso | 360 | Merluzzo in Olio | Assegnato |
| Pesce lavorato e conservato | Pesce in scatola o in vaso | 361 | Tonno all'Acqua | Assegnato |
| Pesce lavorato e conservato | Pesce in scatola o in vaso | 363 | Polpa di Granchio | Confermato |
| Pesce lavorato e conservato | Pesce affumicato e marinato | 357 | Salmone Affumicato | Assegnato |
| Pesce lavorato e conservato | Surimi e preparati ittici freschi | 362 | Surimi | Assegnato |

### Latte, uova e gastronomia

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Latte, yogurt e dessert | Latte fresco | 531 | Latte Intero | Confermato |
| Latte, yogurt e dessert | Latte fresco | 532 | Latte Parz. Scremato | Confermato |
| Latte, yogurt e dessert | Latte fresco | 533 | Latte Scremato | Confermato |
| Latte, yogurt e dessert | Latte fresco | 547 | Latte Fresco | Assegnato |
| Latte, yogurt e dessert | Latte fresco | 551 | Latte | Confermato |
| Latte, yogurt e dessert | Latte UHT | 546 | Latte UHT Intero | Assegnato |
| Latte, yogurt e dessert | Yogurt e skyr | 538 | Yogurt Bianco | Assegnato |
| Latte, yogurt e dessert | Yogurt e skyr | 539 | Yogurt Greco | Assegnato |
| Latte, yogurt e dessert | Yogurt e skyr | 540 | Yogurt alla Frutta | Assegnato |
| Latte, yogurt e dessert | Yogurt e skyr | 542 | Yogurt da Bere | Assegnato |
| Latte, yogurt e dessert | Yogurt e skyr | 543 | Yogurt alla Vaniglia | Assegnato |
| Latte, yogurt e dessert | Yogurt e skyr | 544 | Yogurt ai Frutti Rossi | Assegnato |
| Latte, yogurt e dessert | Yogurt e skyr | 548 | Yogurt Skyr | Assegnato |
| Latte, yogurt e dessert | Yogurt e skyr | 549 | Crema di Yogurt | Assegnato |
| Latte, yogurt e dessert | Yogurt e skyr | 552 | Yogurt | Assegnato |
| Latte, yogurt e dessert | Kefir e latte fermentato | 541 | Kefir | Assegnato |
| Formaggi | Mozzarella, burrata e fiordilatte | 545 | Fiordilatte Fresco | Confermato |
| Formaggi | Mozzarella, burrata e fiordilatte | 571 | Mozzarella | Assegnato |
| Formaggi | Mozzarella, burrata e fiordilatte | 572 | Fiordilatte | Assegnato |
| Formaggi | Ricotta e altri formaggi freschi | 585 | Ricotta | Assegnato |
| Formaggi | Ricotta e altri formaggi freschi | 588 | Caprino | Confermato |
| Formaggi | Ricotta e altri formaggi freschi | 589 | Casatella | Assegnato |
| Formaggi | Formaggi spalmabili e mascarpone | 550 | Formaggio Spalmabile | Assegnato |
| Formaggi | Formaggi spalmabili e mascarpone | 587 | Mascarpone | Assegnato |
| Formaggi | Formaggi a pasta molle ed erborinati | 576 | Gorgonzola | Assegnato |
| Formaggi | Formaggi a pasta molle ed erborinati | 577 | Taleggio | Assegnato |
| Formaggi | Formaggi a pasta molle ed erborinati | 578 | Robiola | Assegnato |
| Formaggi | Formaggi a pasta molle ed erborinati | 579 | Brie | Assegnato |
| Formaggi | Formaggi a pasta molle ed erborinati | 580 | Camembert | Assegnato |
| Formaggi | Formaggi a pasta molle ed erborinati | 586 | Stracchino | Assegnato |
| Formaggi | Formaggi a pasta filata e semidura | 581 | Emmental | Assegnato |
| Formaggi | Formaggi a pasta filata e semidura | 582 | Edamer | Assegnato |
| Formaggi | Formaggi a pasta filata e semidura | 583 | Fontina | Assegnato |
| Formaggi | Formaggi a pasta filata e semidura | 584 | Provolone | Assegnato |
| Formaggi | Formaggi a pasta filata e semidura | 590 | Toma | Confermato |
| Formaggi | Formaggi stagionati e da grattugia | 573 | Grana Padano | Assegnato |
| Formaggi | Formaggi stagionati e da grattugia | 574 | Parmigiano Reggiano | Assegnato |
| Formaggi | Formaggi stagionati e da grattugia | 575 | Pecorino Romano | Assegnato |
| Burro, panna e alternative | Burro e burro chiarificato | 611 | Burro Dolce | Assegnato |
| Burro, panna e alternative | Burro e burro chiarificato | 612 | Burro Intero | Assegnato |
| Burro, panna e alternative | Burro e burro chiarificato | 613 | Burro di Centrifuga | Assegnato |
| Burro, panna e alternative | Burro e burro chiarificato | 614 | Burro Affumicato | Assegnato |
| Burro, panna e alternative | Burro e burro chiarificato | 615 | Burro Salato | Assegnato |
| Burro, panna e alternative | Burro e burro chiarificato | 620 | Crema di Burro | Confermato |
| Burro, panna e alternative | Burro e burro chiarificato | 621 | Burro | Assegnato |
| Burro, panna e alternative | Panna da cucina e da montare | 616 | Panna da Cucina | Assegnato |
| Burro, panna e alternative | Panna da cucina e da montare | 617 | Panna Montabile | Assegnato |
| Burro, panna e alternative | Panna da cucina e da montare | 618 | Panna Addensante | Assegnato |
| Burro, panna e alternative | Panna da cucina e da montare | 619 | Panna da Montare | Assegnato |
| Burro, panna e alternative | Panna da cucina e da montare | 622 | Panna | Assegnato |
| Uova | Uova in guscio | 691 | Uova Fresche | Assegnato |
| Uova | Uova in guscio | 693 | Uova da Galline all'Aperto | Assegnato |
| Uova | Uova in guscio | 694 | Uova di Codornice | Assegnato |
| Uova | Uova in guscio | 696 | Uova | Assegnato |
| Uova | Albumi, tuorli e uova liquide | 695 | Tuorlo d'Uovo | Assegnato |
| Pasta fresca e impasti | Gnocchi freschi | 740 | Gnocchi | Assegnato |
| Pasta fresca e impasti | Impasti freschi per pizza e dolci | 183 | Frolla | Confermato |
| Gastronomia e piatti freschi | Secondi pronti refrigerati | 217 | Stufato di Manzo | Confermato |
| Gastronomia e piatti freschi | Secondi pronti refrigerati | 218 | Brasato | Confermato |
| Gastronomia e piatti freschi | Secondi pronti refrigerati | 219 | Roastbeef | Confermato |
| Gastronomia e piatti freschi | Secondi pronti refrigerati | 224 | Goulash | Confermato |
| Gastronomia e piatti freschi | Spalmabili freschi, hummus e guacamole | 1018 | Hummus | Confermato |
| Gastronomia e piatti freschi | Spalmabili freschi, hummus e guacamole | 1019 | Guacamole | Confermato |


### Surgelati

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Frutta e verdura surgelate | Verdure e legumi | 651 | Verdure Miste Surgelate | Assegnato |
| Frutta e verdura surgelate | Verdure e legumi | 652 | Piselli Surgelati | Assegnato |
| Frutta e verdura surgelate | Verdure e legumi | 653 | Mais Surgelato | Assegnato |
| Frutta e verdura surgelate | Patate e contorni | 654 | Patatine Fritte Surgelate | Assegnato |
| Frutta e verdura surgelate | Patate e contorni | 663 | Polenta Surgelata | Assegnato |
| Frutta e verdura surgelate | Frutta e frutti di bosco | 662 | Frutti di Bosco Surgelati | Assegnato |
| Carne e pesce surgelati | Hamburger e preparati di carne | 660 | Hamburger Surgelato | Assegnato |
| Carne e pesce surgelati | Filetti e pesce intero | 661 | Filetti di Pesce Surgelati | Assegnato |
| Carne e pesce surgelati | Molluschi, crostacei e preparati ittici | 388 | Gamberi Rossi Surgelato | Assegnato |
| Carne e pesce surgelati | Molluschi, crostacei e preparati ittici | 389 | Astici Surgelato | Assegnato |
| Carne e pesce surgelati | Molluschi, crostacei e preparati ittici | 390 | Scampi Surgelato | Assegnato |
| Carne e pesce surgelati | Molluschi, crostacei e preparati ittici | 391 | Cozze Surgelato | Assegnato |
| Carne e pesce surgelati | Molluschi, crostacei e preparati ittici | 392 | Calamari Surgelato | Assegnato |
| Carne e pesce surgelati | Molluschi, crostacei e preparati ittici | 393 | Mazzancolle Surgelato | Assegnato |
| Carne e pesce surgelati | Molluschi, crostacei e preparati ittici | 394 | Vongole Veraci Surgelato | Assegnato |
| Carne e pesce surgelati | Molluschi, crostacei e preparati ittici | 395 | Polpo Surgelato | Assegnato |
| Carne e pesce surgelati | Molluschi, crostacei e preparati ittici | 396 | Seppie Surgelato | Assegnato |
| Carne e pesce surgelati | Molluschi, crostacei e preparati ittici | 397 | Gamberi Tigre Surgelato | Assegnato |
| Pronti e impasti surgelati | Pizze e focacce | 655 | Pizza Surgelata | Assegnato |
| Pronti e impasti surgelati | Primi e pasta ripiena | 656 | Lasagne Surgelate | Assegnato |
| Pronti e impasti surgelati | Primi e pasta ripiena | 659 | Tortellini Surgelati | Assegnato |
| Gelati e ghiaccio | Sorbetti e ghiaccioli | 657 | Ghiaccioli | Assegnato |
| Gelati e ghiaccio | Sorbetti e ghiaccioli | 658 | Sorbetto al Limone | Assegnato |

### Dispensa

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Pasta, riso e cereali | Pasta secca | 731 | Spaghetti | Assegnato |
| Pasta, riso e cereali | Pasta secca | 732 | Penne | Assegnato |
| Pasta, riso e cereali | Pasta secca | 733 | Fusilli | Assegnato |
| Pasta, riso e cereali | Pasta secca | 734 | Rigatoni | Assegnato |
| Pasta, riso e cereali | Pasta secca | 735 | Farfalle | Assegnato |
| Pasta, riso e cereali | Pasta secca | 736 | Orecchiette | Confermato |
| Pasta, riso e cereali | Pasta secca | 737 | Tagliatelle | Confermato |
| Pasta, riso e cereali | Pasta secca | 738 | Pappardelle | Confermato |
| Pasta, riso e cereali | Pasta secca | 739 | Lasagne | Confermato |
| Pasta, riso e cereali | Pasta secca | 748 | Spaghettini | Assegnato |
| Pasta, riso e cereali | Pasta secca | 749 | Mezze Penne | Assegnato |
| Pasta, riso e cereali | Pasta secca | 750 | Stelline | Assegnato |
| Pasta, riso e cereali | Pasta secca | 751 | Pasta | Assegnato |
| Pasta, riso e cereali | Riso | 741 | Riso Carnaroli | Assegnato |
| Pasta, riso e cereali | Riso | 742 | Riso Basmati | Assegnato |
| Pasta, riso e cereali | Riso | 743 | Riso Arborio | Assegnato |
| Pasta, riso e cereali | Riso | 744 | Riso Integrale | Assegnato |
| Pasta, riso e cereali | Riso | 752 | Riso | Assegnato |
| Pasta, riso e cereali | Cereali in chicchi | 745 | Quinoa | Assegnato |
| Pasta, riso e cereali | Cereali in chicchi | 978 | Farro | Assegnato |
| Pasta, riso e cereali | Cereali in chicchi | 979 | Orzo Perlato | Assegnato |
| Pasta, riso e cereali | Couscous e bulgur | 746 | Couscous | Assegnato |
| Pasta, riso e cereali | Polenta e semole pronte | 747 | Polenta | Assegnato |
| Farine, lieviti e preparati | Farine di cereali | 971 | Farina 00 | Assegnato |
| Farine, lieviti e preparati | Farine di cereali | 972 | Farina Integrale | Assegnato |
| Farine, lieviti e preparati | Farine di cereali | 973 | Farina di Grano Tenero | Assegnato |
| Farine, lieviti e preparati | Farine di cereali | 982 | Semola Rimacinata | Assegnato |
| Farine, lieviti e preparati | Farine di cereali | 983 | Farina | Assegnato |
| Farine, lieviti e preparati | Amidi e fecole | 981 | Amido di Mais | Assegnato |
| Farine, lieviti e preparati | Lieviti e agenti lievitanti | 974 | Lievito | Assegnato |
| Legumi, frutta secca e semi | Legumi secchi | 975 | Fagioli Cannellini | Assegnato |
| Legumi, frutta secca e semi | Legumi secchi | 976 | Lenticchie Secche | Assegnato |
| Legumi, frutta secca e semi | Legumi secchi | 977 | Cicerchie | Assegnato |
| Legumi, frutta secca e semi | Legumi secchi | 980 | Ceci Secchi | Assegnato |
| Legumi, frutta secca e semi | Legumi secchi | 984 | Legumi | Assegnato |
| Legumi, frutta secca e semi | Frutta a guscio | 29 | Nocciola | Assegnato |
| Legumi, frutta secca e semi | Frutta a guscio | 857 | Noccioline | Assegnato |
| Legumi, frutta secca e semi | Frutta a guscio | 858 | Mandorle da Snack | Assegnato |
| Legumi, frutta secca e semi | Frutta a guscio | 859 | Frutta Secca Mista | Confermato |
| Conserve e piatti pronti | Passate, pelati e concentrati | 811 | Pomodori Pelati | Assegnato |
| Conserve e piatti pronti | Passate, pelati e concentrati | 812 | Passata di Pomodoro | Assegnato |
| Conserve e piatti pronti | Passate, pelati e concentrati | 813 | Salsa di Pomodoro | Confermato |
| Conserve e piatti pronti | Passate, pelati e concentrati | 825 | Cruda di Pomodoro | Assegnato |
| Conserve e piatti pronti | Passate, pelati e concentrati | 826 | Pelati | Assegnato |
| Conserve e piatti pronti | Legumi in scatola e in vaso | 815 | Fagioli in Lattina | Assegnato |
| Conserve e piatti pronti | Legumi in scatola e in vaso | 816 | Ceci in Lattina | Assegnato |
| Conserve e piatti pronti | Legumi in scatola e in vaso | 821 | Fagioli Borlotti | Confermato |
| Conserve e piatti pronti | Legumi in scatola e in vaso | 822 | Lenticchie in Conserve | Assegnato |
| Conserve e piatti pronti | Verdure conservate, sott'oli e sottaceti | 814 | Mais in Lattina | Assegnato |
| Conserve e piatti pronti | Verdure conservate, sott'oli e sottaceti | 818 | Cetriolini | Assegnato |
| Conserve e piatti pronti | Verdure conservate, sott'oli e sottaceti | 819 | Peperoni Arrostiti | Assegnato |
| Conserve e piatti pronti | Verdure conservate, sott'oli e sottaceti | 820 | Melanzane Sott'olio | Assegnato |
| Conserve e piatti pronti | Verdure conservate, sott'oli e sottaceti | 823 | Funghi in Vaso | Assegnato |
| Conserve e piatti pronti | Olive e capperi | 90 | Olive Verdi | Assegnato |
| Conserve e piatti pronti | Olive e capperi | 110 | Olive Verdi in vaso | Assegnato |
| Conserve e piatti pronti | Olive e capperi | 120 | Olive Verdi in bustina | Assegnato |
| Conserve e piatti pronti | Olive e capperi | 817 | Olive in Vaso | Assegnato |
| Oli, condimenti e salse | Oli vegetali | 771 | Olio Extra Vergine di Oliva | Assegnato |
| Oli, condimenti e salse | Oli vegetali | 772 | Olio di Semi | Assegnato |
| Oli, condimenti e salse | Oli vegetali | 773 | Olio di Colza | Assegnato |
| Oli, condimenti e salse | Oli vegetali | 783 | Olio | Assegnato |
| Oli, condimenti e salse | Aceti e glasse | 774 | Aceto Balsamico | Assegnato |
| Oli, condimenti e salse | Aceti e glasse | 775 | Aceto di Vino | Assegnato |
| Oli, condimenti e salse | Aceti e glasse | 776 | Aceto di Mele | Assegnato |
| Oli, condimenti e salse | Aceti e glasse | 784 | Aceto | Assegnato |
| Oli, condimenti e salse | Sale e pepe | 777 | Sale Marino | Assegnato |
| Oli, condimenti e salse | Sale e pepe | 778 | Sale Iodato | Assegnato |
| Oli, condimenti e salse | Sale e pepe | 779 | Pepe Nero | Assegnato |
| Oli, condimenti e salse | Sale e pepe | 785 | Sale | Assegnato |
| Oli, condimenti e salse | Spezie, erbe secche e miscele | 111 | Rosmarino in bustina | Confermato |
| Oli, condimenti e salse | Spezie, erbe secche e miscele | 112 | Origano in bustina | Confermato |
| Oli, condimenti e salse | Spezie, erbe secche e miscele | 114 | Prezzemolo in bustina | Confermato |
| Oli, condimenti e salse | Spezie, erbe secche e miscele | 115 | Coriandolo in bustina | Confermato |
| Oli, condimenti e salse | Spezie, erbe secche e miscele | 116 | Basilico in bustina | Confermato |
| Oli, condimenti e salse | Spezie, erbe secche e miscele | 118 | Salvia in bustina | Confermato |
| Oli, condimenti e salse | Spezie, erbe secche e miscele | 119 | Menta in bustina | Confermato |
| Oli, condimenti e salse | Spezie, erbe secche e miscele | 780 | Paprika | Assegnato |
| Oli, condimenti e salse | Spezie, erbe secche e miscele | 781 | Curcuma | Assegnato |
| Oli, condimenti e salse | Spezie, erbe secche e miscele | 782 | Zenzero in Polvere | Assegnato |
| Oli, condimenti e salse | Salse fredde e dressing | 1013 | Salsa Barbecue | Assegnato |
| Oli, condimenti e salse | Salse fredde e dressing | 1014 | Salsa Piccante | Assegnato |
| Oli, condimenti e salse | Salse fredde e dressing | 1015 | Ketchup | Assegnato |
| Oli, condimenti e salse | Salse fredde e dressing | 1016 | Maionese | Assegnato |
| Oli, condimenti e salse | Salse fredde e dressing | 1017 | Senape | Assegnato |
| Oli, condimenti e salse | Salse fredde e dressing | 1020 | Salsa Piccante Messicana | Assegnato |
| Oli, condimenti e salse | Salse fredde e dressing | 1022 | Tahina | Assegnato |
| Oli, condimenti e salse | Sughi, pesti e salse da cucina | 824 | Sugo Pronto | Assegnato |
| Oli, condimenti e salse | Sughi, pesti e salse da cucina | 827 | Sugo | Assegnato |
| Oli, condimenti e salse | Sughi, pesti e salse da cucina | 1011 | Pesto Genovese | Assegnato |
| Oli, condimenti e salse | Sughi, pesti e salse da cucina | 1021 | Salsa Alfredo | Assegnato |
| Oli, condimenti e salse | Condimenti e ingredienti fermentati | 1012 | Salsa di Soia | Assegnato |
| Colazione e dolcificanti | Confetture e marmellate | 905 | Marmellata | Assegnato |
| Colazione e dolcificanti | Miele e sciroppi dolcificanti | 904 | Miele | Assegnato |
| Colazione e dolcificanti | Creme spalmabili | 900 | Crema da Spalmare | Assegnato |
| Colazione e dolcificanti | Zucchero e dolcificanti | 903 | Zucchero | Assegnato |
| Snack e dolci | Cracker e gallette | 851 | Cracker | Assegnato |
| Snack e dolci | Cracker e gallette | 852 | Gallette | Assegnato |
| Snack e dolci | Cracker e gallette | 862 | Cracker Integrali | Assegnato |
| Snack e dolci | Patatine, popcorn e snack salati | 853 | Patatine | Assegnato |
| Snack e dolci | Patatine, popcorn e snack salati | 854 | Chips di Patate | Assegnato |
| Snack e dolci | Patatine, popcorn e snack salati | 855 | Popcorn | Assegnato |
| Snack e dolci | Patatine, popcorn e snack salati | 861 | Stuzzichini | Assegnato |
| Snack e dolci | Biscotti e wafer | 191 | Frollino | Assegnato |
| Snack e dolci | Biscotti e wafer | 895 | Biscotti | Assegnato |
| Snack e dolci | Biscotti e wafer | 897 | Wafer | Assegnato |
| Snack e dolci | Biscotti e wafer | 899 | Frollini | Assegnato |
| Snack e dolci | Cioccolato e praline | 891 | Cioccolato Fondente | Assegnato |
| Snack e dolci | Cioccolato e praline | 892 | Cioccolato al Latte | Assegnato |
| Snack e dolci | Cioccolato e praline | 893 | Cioccolato Bianco | Assegnato |
| Snack e dolci | Cioccolato e praline | 894 | Tavoletta al Cereale | Assegnato |
| Snack e dolci | Cioccolato e praline | 906 | Cioccolato | Assegnato |
| Snack e dolci | Caramelle e gomme | 901 | Caramelle | Assegnato |
| Snack e dolci | Caramelle e gomme | 902 | Marshmallow | Assegnato |
| Snack e dolci | Barrette e altri snack dolci | 856 | Barretta ai Cereali | Assegnato |


### Bevande

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Acque | Acqua naturale | 401 | Acqua Naturale | Assegnato |
| Acque | Acqua naturale | 404 | Acqua Termale | Confermato |
| Acque | Acqua naturale | 405 | Acqua Leggera | Confermato |
| Acque | Acqua naturale | 408 | Acqua Plateale | Confermato |
| Acque | Acqua naturale | 410 | Acqua Alpina | Confermato |
| Acque | Acqua naturale | 411 | Acqua | Confermato |
| Acque | Acqua frizzante | 402 | Acqua Frizzante | Assegnato |
| Acque | Acqua frizzante | 409 | Acqua Pétillante | Assegnato |
| Acque | Acqua leggermente frizzante | 403 | Acqua Leggermente Frizzante | Assegnato |
| Acque | Acque aromatizzate e funzionali | 406 | Acqua con Ginseng | Assegnato |
| Succhi e nettari | Succhi 100% | 427 | Succo di Arancia | Confermato |
| Succhi e nettari | Succhi 100% | 428 | Succo di Mela | Confermato |
| Succhi e nettari | Succhi 100% | 429 | Succo di Pera | Confermato |
| Succhi e nettari | Succhi 100% | 430 | Succo di Ananas | Confermato |
| Succhi e nettari | Succhi 100% | 431 | Succo di Carota | Confermato |
| Succhi e nettari | Succhi 100% | 433 | Succo di Melagrana | Confermato |
| Succhi e nettari | Succhi 100% | 434 | Succo di Kiwi | Confermato |
| Succhi e nettari | Succhi 100% | 435 | Succo di Fragola | Confermato |
| Succhi e nettari | Succhi 100% | 437 | Succo di Pompelmo | Confermato |
| Succhi e nettari | Succhi 100% | 438 | Succo Multifrutta | Confermato |
| Succhi e nettari | Nettari e bevande alla frutta | 436 | Nettaro di Pesca | Assegnato |
| Succhi e nettari | Spremute | 432 | Spremuta d'Arancia | Assegnato |
| Bibite analcoliche | Cola | 453 | Cola | Assegnato |
| Bibite analcoliche | Cola | 454 | Cola Zero | Assegnato |
| Bibite analcoliche | Cola | 455 | Cola Light | Assegnato |
| Bibite analcoliche | Aranciate, limonate e altre bibite gassate | 456 | Limonata | Assegnato |
| Bibite analcoliche | Aranciate, limonate e altre bibite gassate | 457 | Aranciata | Assegnato |
| Bibite analcoliche | Aranciate, limonate e altre bibite gassate | 458 | Chinotto | Assegnato |
| Bibite analcoliche | Tè e caffè freddi pronti | 459 | Tè Freddo Pesca | Assegnato |
| Bibite analcoliche | Tè e caffè freddi pronti | 463 | Tè Freddo Limone | Assegnato |
| Bibite analcoliche | Bibite non gassate | 464 | Bibita al Malto | Confermato |
| Sport ed energia | Energy drink | 460 | Energy Drink | Assegnato |
| Bevande vegetali | Bevande di soia | 534 | Latte di Soia | Assegnato |
| Bevande vegetali | Bevande di avena e cereali | 535 | Latte di Avena | Assegnato |
| Bevande vegetali | Bevande di mandorla e frutta a guscio | 536 | Latte di Mandorla | Assegnato |
| Bevande vegetali | Bevande di cocco e altre vegetali | 537 | Latte di Cocco | Assegnato |
| Caffè, tè e bevande da preparare | Caffè in grani e macinato | 931 | Caffè in Grani | Assegnato |
| Caffè, tè e bevande da preparare | Caffè in grani e macinato | 932 | Caffè Macinato | Assegnato |
| Caffè, tè e bevande da preparare | Caffè in grani e macinato | 933 | Caffè Decaffeinato | Assegnato |
| Caffè, tè e bevande da preparare | Caffè in grani e macinato | 943 | Caffè | Assegnato |
| Caffè, tè e bevande da preparare | Capsule e cialde | 934 | Caffè in Cialde | Assegnato |
| Caffè, tè e bevande da preparare | Capsule e cialde | 935 | Caffè in Capsule | Assegnato |
| Caffè, tè e bevande da preparare | Tè e tisane | 936 | Tè Nero | Assegnato |
| Caffè, tè e bevande da preparare | Tè e tisane | 937 | Tè Verde | Assegnato |
| Caffè, tè e bevande da preparare | Tè e tisane | 938 | Tè alla Menta | Assegnato |
| Caffè, tè e bevande da preparare | Tè e tisane | 939 | Camomilla | Assegnato |
| Caffè, tè e bevande da preparare | Tè e tisane | 940 | Infuso Rilassante | Assegnato |
| Caffè, tè e bevande da preparare | Tè e tisane | 941 | Matcha | Assegnato |
| Caffè, tè e bevande da preparare | Tè e tisane | 944 | Tè | Assegnato |
| Caffè, tè e bevande da preparare | Cacao e preparati per bevande calde | 942 | Cacao in Polvere | Confermato |
| Miscelatori e sciroppi | Toniche e soda | 462 | Soda | Assegnato |
| Miscelatori e sciroppi | Ginger ale e ginger beer analcolici | 461 | Ginger Ale | Assegnato |
| Birre | Lager e pils | 488 | Birra Lager | Assegnato |
| Birre | Ale e IPA | 482 | Birra IPA | Assegnato |
| Birre | Birre di frumento | 483 | Birra Weizen | Assegnato |
| Birre | Birre analcoliche | 484 | Birra Zero | Assegnato |
| Birre | Birre analcoliche | 485 | Birra Senza Alcol | Assegnato |
| Birre | Birre non specificate | 479 | Birra Chiara | Confermato |
| Birre | Birre non specificate | 480 | Birra Bionda | Confermato |
| Birre | Birre non specificate | 481 | Birra Rossa | Confermato |
| Birre | Birre non specificate | 486 | Birra Ambrata | Confermato |
| Birre | Birre non specificate | 487 | Birra Artigianale | Confermato |
| Birre | Birre non specificate | 489 | Birra | Confermato |
| Vini e spumanti | Vini rossi | 505 | Vino Rosso | Assegnato |
| Vini e spumanti | Vini rossi | 509 | Chianti | Assegnato |
| Vini e spumanti | Vini rossi | 510 | Merlot | Assegnato |
| Vini e spumanti | Vini rossi | 511 | Cabernet | Assegnato |
| Vini e spumanti | Vini rossi | 513 | Montepulciano | Assegnato |
| Vini e spumanti | Vini bianchi | 506 | Vino Bianco | Assegnato |
| Vini e spumanti | Vini bianchi | 512 | Pinot Grigio | Assegnato |
| Vini e spumanti | Vini rosati | 507 | Vino Rosé | Assegnato |
| Vini e spumanti | Vini frizzanti | 514 | Lambrusco | Confermato |
| Vini e spumanti | Spumanti e prosecco spumante | 508 | Prosecco | Confermato |
| Vini e spumanti | Vini non specificati | 515 | Vino | Assegnato |

### Pulizia e consumabili casa

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Bucato | Detersivi bucato | 1065 | Liquido Lavapanni | Assegnato |
| Bucato | Detersivi bucato | 1122 | Detersivo Biancheria | Assegnato |
| Bucato | Detersivi bucato | 1132 | Detersivo in Polvere | Assegnato |
| Bucato | Detersivi bucato | 1135 | Sapone di Marsiglia | Assegnato |
| Bucato | Detersivi bucato | 1136 | Panetto per Lavatrice | Assegnato |
| Bucato | Detersivi bucato | 1139 | Detersivo Delicato | Assegnato |
| Bucato | Detersivi bucato | 1141 | Detersivo | Confermato |
| Bucato | Detersivi bucato | 1411 | Detersivo Bimbo | Confermato |
| Bucato | Detersivi bucato | 1454 | Detersivo Bimbo Extra Delicato | Confermato |
| Bucato | Ammorbidenti | 1123 | Ammorbidente | Assegnato |
| Piatti e lavastoviglie | Detersivi per piatti a mano | 1121 | Detersivo Piatti | Assegnato |
| Piatti e lavastoviglie | Detersivi lavastoviglie | 1133 | Liquido Lavastoviglie | Assegnato |
| Piatti e lavastoviglie | Detersivi lavastoviglie | 1134 | Tablet Lavastoviglie | Assegnato |
| Detergenza casa | Pavimenti e superfici | 1126 | Pulitore Multiuso | Assegnato |
| Detergenza casa | Pavimenti e superfici | 1131 | Detersivo Pavimenti | Assegnato |
| Detergenza casa | Sgrassatori, candeggina e disinfettanti per superfici | 1124 | Disinfettante | Confermato |
| Detergenza casa | Sgrassatori, candeggina e disinfettanti per superfici | 1125 | Sgrassatore | Assegnato |
| Detergenza casa | Sgrassatori, candeggina e disinfettanti per superfici | 1129 | Candeggina | Assegnato |
| Detergenza casa | Sgrassatori, candeggina e disinfettanti per superfici | 1140 | Sgrassatore Eco | Assegnato |
| Detergenza casa | Anticalcare e disgorganti | 1137 | Spray Anticalcare | Assegnato |
| Accessori per pulire | Guanti da pulizia | 1128 | Guanti Monouso | Confermato |
| Carta, rifiuti e monouso | Carta da cucina, fazzoletti e tovaglioli | 1198 | Carta Accappatoio | Confermato |
| Carta, rifiuti e monouso | Carta da cucina, fazzoletti e tovaglioli | 1205 | Fazzoletti da Tavolo | Assegnato |
| Carta, rifiuti e monouso | Carta da cucina, fazzoletti e tovaglioli | 1256 | Carta Accappatoio Ricaricabile | Confermato |
| Carta, rifiuti e monouso | Carta da cucina, fazzoletti e tovaglioli | 1260 | Fazzoletti da Tavolo Ricaricabile | Confermato |
| Carta, rifiuti e monouso | Sacchi rifiuti e riciclo | 1127 | Sacchi della Spazzatura | Assegnato |
| Carta, rifiuti e monouso | Carta forno, alluminio e pellicola | 1195 | Pellicola Trasparente | Assegnato |
| Carta, rifiuti e monouso | Carta forno, alluminio e pellicola | 1196 | Carta Alluminio | Assegnato |
| Carta, rifiuti e monouso | Carta forno, alluminio e pellicola | 1197 | Carta Forno | Assegnato |
| Carta, rifiuti e monouso | Carta forno, alluminio e pellicola | 1253 | Carta Alluminio Ricaricabile | Confermato |
| Carta, rifiuti e monouso | Stoviglie e vaschette monouso | 1200 | Stoviglie Monouso | Assegnato |
| Profumazione e protezione | Profumi per ambienti e diffusori | 1130 | Deodorante per Ambienti | Assegnato |
| Stenditura e cura degli indumenti | Mollette, stendini e accessori | 1202 | Filo da Stendere | Assegnato |
| Stenditura e cura degli indumenti | Mollette, stendini e accessori | 1203 | Spazzola da Bucato | Assegnato |
| Stenditura e cura degli indumenti | Mollette, stendini e accessori | 1204 | Ganci da Stendere | Assegnato |
| Stenditura e cura degli indumenti | Mollette, stendini e accessori | 1251 | Filo da Stendere Ricaricabile | Confermato |
| Stenditura e cura degli indumenti | Mollette, stendini e accessori | 1254 | Ganci da Stendere Ricaricabile | Confermato |
| Stenditura e cura degli indumenti | Mollette, stendini e accessori | 1258 | Spazzola da Bucato Ricaricabile | Confermato |

### Cucina e tavola

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Cottura | Coperchi e accessori da cottura | 1199 | Spago da Cucina | Assegnato |
| Cottura | Coperchi e accessori da cottura | 1255 | Spago da Cucina Ricaricabile | Confermato |

### Casa e arredamento

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Tessili casa | Asciugamani e tessili bagno | 1410 | Asciugamano Bimbo | Assegnato |
| Tessili casa | Asciugamani e tessili bagno | 1460 | Asciugamano Bimbo Extra Delicato | Assegnato |
| Decorazione | Candele decorative e portacandele | 1191 | Candele | Assegnato |

### Igiene e cura della persona

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Igiene orale | Dentifrici | 1055 | Dentifricio | Assegnato |
| Igiene orale | Spazzolini e testine | 1056 | Spazzolino da Denti | Assegnato |
| Igiene orale | Filo e scovolini | 1057 | Filo Interdentale | Assegnato |
| Igiene orale | Filo e scovolini | 1070 | Spazzolino Interdentale | Assegnato |
| Igiene orale | Collutori e cura protesi | 1066 | Collutorio | Assegnato |
| Corpo e igiene personale | Saponi e detergenti mani | 1051 | Sapone da Mani | Assegnato |
| Corpo e igiene personale | Saponi e detergenti mani | 1071 | Sapone | Assegnato |
| Corpo e igiene personale | Bagnoschiuma e doccia | 1052 | Gel Doccia | Assegnato |
| Corpo e igiene personale | Bagnoschiuma e doccia | 1064 | Bagnoschiuma | Assegnato |
| Corpo e igiene personale | Deodoranti | 1058 | Deodorante | Assegnato |
| Capelli | Shampoo | 1053 | Shampoo | Assegnato |
| Capelli | Balsami e maschere | 1054 | Balsamo | Confermato |
| Viso, pelle e cosmetica | Creme e trattamenti | 1061 | Crema Idratante | Assegnato |
| Viso, pelle e cosmetica | Creme e trattamenti | 1067 | Balsamo Labbra | Assegnato |
| Viso, pelle e cosmetica | Creme e trattamenti | 1068 | Maschera Viso | Assegnato |
| Viso, pelle e cosmetica | Solari e doposole | 1062 | Crema Solare | Assegnato |
| Rasatura e depilazione | Rasoi e lame | 1059 | Rasoi | Assegnato |
| Rasatura e depilazione | Schiume e dopobarba | 1060 | Schiuma da Barba | Assegnato |

### Salute e benessere

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Medicazione e protezione | Cerotti, garze e bende | 1063 | Cotone Idrofilo | Assegnato |
| Medicazione e protezione | Disinfettanti per la pelle | 1138 | Acqua Ossigenata | Confermato |
| Medicazione e protezione | Mascherine e guanti di protezione personale | 1412 | Mascherina Bimbo | Assegnato |
| Medicazione e protezione | Mascherine e guanti di protezione personale | 1459 | Mascherina Bimbo Extra Delicato | Assegnato |
| Farmacia e integrazione | Prodotti per occhi e lenti | 1069 | Soluzione Contatti | Assegnato |

### Infanzia

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Cambio e bagno | Pannolini e mutandine | 1401 | Pannolini | Assegnato |
| Cambio e bagno | Pannolini e mutandine | 1450 | Pannolini Extra Delicato | Assegnato |
| Cambio e bagno | Salviette e prodotti cambio | 1402 | Salviette Bimbo | Assegnato |
| Cambio e bagno | Salviette e prodotti cambio | 1453 | Salviette Bimbo Extra Delicato | Assegnato |
| Cambio e bagno | Detergenti e creme bimbo | 1403 | Schiuma Bagno Bimbo | Assegnato |
| Cambio e bagno | Detergenti e creme bimbo | 1404 | Shampoo Bimbo | Assegnato |
| Cambio e bagno | Detergenti e creme bimbo | 1405 | Crema Emolliente Bimbo | Assegnato |
| Cambio e bagno | Detergenti e creme bimbo | 1449 | Crema Emolliente Bimbo Extra Delicato | Assegnato |
| Cambio e bagno | Detergenti e creme bimbo | 1451 | Schiuma Bagno Bimbo Extra Delicato | Assegnato |
| Cambio e bagno | Detergenti e creme bimbo | 1455 | Shampoo Bimbo Extra Delicato | Assegnato |
| Alimentazione | Latte per l'infanzia | 1406 | Latte in Polvere | Confermato |
| Alimentazione | Latte per l'infanzia | 1458 | Latte in Polvere Extra Delicato | Confermato |
| Alimentazione | Biberon, tettarelle e accessori pappa | 1407 | Biberon | Assegnato |
| Alimentazione | Biberon, tettarelle e accessori pappa | 1409 | Poppatoio | Assegnato |
| Alimentazione | Biberon, tettarelle e accessori pappa | 1452 | Biberon Extra Delicato | Assegnato |
| Alimentazione | Biberon, tettarelle e accessori pappa | 1456 | Poppatoio Extra Delicato | Assegnato |
| Riposo e passeggio | Ciucci e dentizione | 1408 | Ciuccio | Assegnato |
| Riposo e passeggio | Ciucci e dentizione | 1457 | Ciuccio Extra Delicato | Assegnato |

### Animali

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Alimenti per cani | Secco cane | 1331 | Croccantini Cane | Assegnato |
| Alimenti per cani | Secco cane | 1384 | Croccantini Cane Eco | Assegnato |
| Alimenti per cani | Umido cane | 1342 | Bocconcini in Conserve | Confermato |
| Alimenti per cani | Snack cane | 1339 | Snack per Cane | Assegnato |
| Alimenti per cani | Snack cane | 1385 | Snack per Cane Eco | Assegnato |
| Alimenti per gatti | Secco gatto | 1332 | Croccantini Gatto | Assegnato |
| Alimenti per gatti | Secco gatto | 1380 | Croccantini Gatto Eco | Assegnato |
| Igiene e cura animali | Lettiere e substrati | 1333 | Sabbietta Gatto | Assegnato |
| Igiene e cura animali | Lettiere e substrati | 1338 | Lettiera | Assegnato |
| Igiene e cura animali | Lettiere e substrati | 1382 | Lettiera Eco | Assegnato |
| Igiene e cura animali | Lettiere e substrati | 1388 | Sabbietta Gatto Eco | Assegnato |
| Igiene e cura animali | Detergenti e toelettatura | 1340 | Shampoo per Animali | Assegnato |
| Igiene e cura animali | Detergenti e toelettatura | 1341 | Spazzola per Animali | Assegnato |
| Igiene e cura animali | Detergenti e toelettatura | 1383 | Shampoo per Animali Eco | Assegnato |
| Igiene e cura animali | Detergenti e toelettatura | 1390 | Spazzola per Animali Eco | Assegnato |
| Accessori animali | Ciotole e distributori | 1337 | Ciotola | Assegnato |
| Accessori animali | Ciotole e distributori | 1389 | Ciotola Eco | Assegnato |
| Accessori animali | Collari, pettorine e guinzagli | 1335 | Collare | Assegnato |
| Accessori animali | Collari, pettorine e guinzagli | 1336 | Guinzaglio | Assegnato |
| Accessori animali | Collari, pettorine e guinzagli | 1381 | Guinzaglio Eco | Assegnato |
| Accessori animali | Collari, pettorine e guinzagli | 1387 | Collare Eco | Assegnato |
| Accessori animali | Giochi e tiragraffi | 1334 | Giochi per Cane | Assegnato |
| Accessori animali | Giochi e tiragraffi | 1379 | Giochi per Cane Eco | Assegnato |

### Ferramenta, manutenzione e giardino

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Elettricità, luce e idraulica | Lampadine e lampade tecniche | 1194 | Lampadine | Assegnato |
| Elettricità, luce e idraulica | Pile e batterie comuni | 1193 | Batterie | Assegnato |
| Elettricità, luce e idraulica | Pile e batterie comuni | 1257 | Batterie Ricaricabile | Assegnato |
| Giardino e balcone | Piante, bulbi e sementi | 101 | Rosmarino in vaso | Assegnato |
| Giardino e balcone | Piante, bulbi e sementi | 102 | Origano in vaso | Assegnato |
| Giardino e balcone | Piante, bulbi e sementi | 104 | Prezzemolo in vaso | Assegnato |
| Giardino e balcone | Piante, bulbi e sementi | 105 | Coriandolo in vaso | Assegnato |
| Giardino e balcone | Piante, bulbi e sementi | 106 | Basilico in vaso | Assegnato |
| Giardino e balcone | Piante, bulbi e sementi | 108 | Salvia in vaso | Assegnato |
| Giardino e balcone | Piante, bulbi e sementi | 109 | Menta in vaso | Assegnato |
| Esterno e barbecue | Carbone e accendifuoco | 1192 | Fiammiferi | Confermato |
| Esterno e barbecue | Carbone e accendifuoco | 1252 | Fiammiferi Ricaricabile | Confermato |

### Cartoleria, scuola e creatività

| L2 | L3 | ID prodotto | Nome nel catalogo proposto | Stato |
|---|---|---:|---|---|
| Scrittura e disegno | Penne e matite | 1261 | Penna Blu | Assegnato |
| Scrittura e disegno | Penne e matite | 1262 | Penna Nera | Assegnato |
| Scrittura e disegno | Penne e matite | 1263 | Matita | Assegnato |
| Scrittura e disegno | Penne e matite | 1323 | Penna Blu Extra | Assegnato |
| Scrittura e disegno | Penne e matite | 1327 | Matita Extra | Assegnato |
| Scrittura e disegno | Evidenziatori e pennarelli | 1272 | Penne da Evidenziatore | Assegnato |
| Scrittura e disegno | Gomme e temperini | 1264 | Gomma | Assegnato |
| Scrittura e disegno | Gomme e temperini | 1326 | Gomma Extra | Assegnato |
| Scrittura e disegno | Righelli e accessori di misurazione | 1265 | Righello | Assegnato |
| Scrittura e disegno | Righelli e accessori di misurazione | 1274 | Calcolatrice | Assegnato |
| Carta e organizzazione | Quaderni e blocchi | 1266 | Quaderno | Assegnato |
| Carta e organizzazione | Quaderni e blocchi | 1267 | Blocco Note | Assegnato |
| Carta e organizzazione | Quaderni e blocchi | 1322 | Quaderno Extra | Assegnato |
| Carta e organizzazione | Quaderni e blocchi | 1328 | Blocco Note Extra | Assegnato |
| Carta e organizzazione | Agende e calendari | 1273 | Agenda | Assegnato |
| Carta e organizzazione | Agende e calendari | 1324 | Agenda Extra | Assegnato |
| Carta e organizzazione | Cartelle, raccoglitori e buste | 1271 | Cartelline | Assegnato |
| Carta e organizzazione | Adesivi ed etichette da scrittura | 1270 | Segnalibri | Assegnato |
| Carta e organizzazione | Adesivi ed etichette da scrittura | 1321 | Segnalibri Extra | Assegnato |
| Materiali scolastici e creativi | Colle e nastri per carta | 1268 | Colla | Assegnato |
| Materiali scolastici e creativi | Colle e nastri per carta | 1325 | Colla Extra | Assegnato |
| Materiali scolastici e creativi | Forbici, cutter e accessori | 1269 | Forbici | Assegnato |
| Materiali scolastici e creativi | Forbici, cutter e accessori | 1330 | Forbici Extra | Assegnato |

## 8. Nuovi prodotti e sinonimi · 285 prodotti

I nuovi prodotti sono da inserire con nuovi ID. I prodotti segnalati per l’esclusione sono stati rimossi. Restano 64 tipi L3 senza prodotto canonico predefinito.

| Prodotto canonico | Sinonimi da registrare |
|---|---|
| Bistecchiera | padella griglia |
| Magnete decorativo da frigorifero | magnete per frigorifero |
| Sacchetti profumati per armadio | profumazione per armadio |
| Hugo pronto da bere | Spritz Hugo pronto |
| Liquore al sambuco | liquore ai fiori di sambuco; Liquore Hugo |

‘Liquore Hugo’ risolve sullo stesso prodotto di ‘liquore ai fiori di sambuco’. ‘Spritz Hugo pronto’ risolve sul cocktail pronto.

### Ortofrutta

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Frutta fresca | Mele e pere | Mela Granny Smith |
| Frutta fresca | Mele e pere | Pera Abate |
| Frutta fresca | Frutta tropicale ed esotica | Avocado |
| Frutta fresca | Frutta tropicale ed esotica | Papaya |
| Ortaggi freschi | Legumi freschi | Fave fresche |
| Ortaggi freschi | Legumi freschi | Piselli freschi |
| Funghi, erbe e germogli | Funghi freschi | Funghi champignon |
| Funghi, erbe e germogli | Funghi freschi | Funghi porcini freschi |
| Funghi, erbe e germogli | Germogli e microverdure | Germogli di soia |
| Ortofrutta pronta | Frutta tagliata e macedonie fresche | Macedonia fresca |

### Pane, pizza e pasticceria

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Pane e panini | Pane a fette e da toast | Pan carré |
| Pane e panini | Piadine, tortillas e pani piatti | Piadina |
| Pane e panini | Piadine, tortillas e pani piatti | Tortilla di mais |
| Pizza e forno salato | Pizza e basi precotte | Base per pizza |

### Carne e salumi

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Bovino e vitello | Tagli di vitello | Fettine di vitello |
| Bovino e vitello | Frattaglie bovine | Fegato bovino |
| Suino | Frattaglie suine | Fegato di maiale |
| Ovino, caprino e altre carni | Agnello e ovino | Costolette di agnello |
| Ovino, caprino e altre carni | Capretto e caprino | Capretto a pezzi |
| Ovino, caprino e altre carni | Selvaggina | Cinghiale a spezzatino |
| Ovino, caprino e altre carni | Equino e altre carni | Fettine di cavallo |
| Macinati e preparati | Carne macinata | Macinato di manzo |
| Macinati e preparati | Carne macinata | Macinato misto |
| Salumi e carni trasformate | Prosciutti crudi e cotti | Prosciutto cotto |
| Salumi e carni trasformate | Prosciutti crudi e cotti | Prosciutto crudo |
| Salumi e carni trasformate | Bresaola, speck e altri salumi stagionati | Bresaola |
| Salumi e carni trasformate | Bresaola, speck e altri salumi stagionati | Speck |
| Salumi e carni trasformate | Carni in scatola e paté | Carne bovina in gelatina |
| Salumi e carni trasformate | Carni in scatola e paté | Paté di fegato |

### Pesce e frutti di mare

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Pesce lavorato e conservato | Pesce salato ed essiccato | Baccalà |
| Pesce lavorato e conservato | Pesce salato ed essiccato | Stoccafisso |
| Pesce lavorato e conservato | Uova di pesce | Uova di lompo |

### Latte, uova e gastronomia

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Latte, yogurt e dessert | Latte in polvere e condensato | Latte condensato |
| Latte, yogurt e dessert | Budini e dessert refrigerati | Budino al cioccolato |
| Burro, panna e alternative | Creme vegetali da cucina e margarina | Margarina |
| Burro, panna e alternative | Creme vegetali da cucina e margarina | Panna vegetale da cucina |
| Burro, panna e alternative | Alternative vegetali a yogurt e formaggi | Yogurt di soia |
| Pasta fresca e impasti | Pasta fresca non ripiena | Tagliatelle fresche |
| Pasta fresca e impasti | Pasta fresca ripiena | Ravioli freschi |
| Pasta fresca e impasti | Pasta fresca ripiena | Tortellini freschi |
| Gastronomia e piatti freschi | Primi e zuppe refrigerate | Vellutata fresca |
| Gastronomia e piatti freschi | Contorni cotti e insalate composte | Insalata russa |
| Gastronomia e piatti freschi | Panini, tramezzini e snack freschi | Tramezzino |
| Proteine vegetali | Tofu, tempeh e seitan | Tofu |
| Proteine vegetali | Tofu, tempeh e seitan | Tempeh |
| Proteine vegetali | Tofu, tempeh e seitan | Seitan |
| Proteine vegetali | Burger vegetali e sostituti della carne refrigerati | Burger vegetale |

### Surgelati

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Carne e pesce surgelati | Tagli di carne e pollame | Petto di pollo surgelato |
| Carne e pesce surgelati | Filetti e pesce intero | Filetti di merluzzo in pastella surgelati |
| Carne e pesce surgelati | Filetti e pesce intero | Bastoncini di pesce panati surgelati |
| Carne e pesce surgelati | Molluschi, crostacei e preparati ittici | Anelli di calamaro in pastella surgelati |
| Pronti e impasti surgelati | Zuppe e piatti unici | Minestrone surgelato |
| Pronti e impasti surgelati | Pane, impasti e prodotti da colazione | Cornetto surgelato |
| Pronti e impasti surgelati | Snack e fritti | Supplì surgelati |
| Pronti e impasti surgelati | Snack e fritti | Crocchette di patate surgelate |
| Pronti e impasti surgelati | Snack e fritti | Olive ascolane surgelate |
| Pronti e impasti surgelati | Snack e fritti | Fiori di zucca in pastella surgelati |
| Pronti e impasti surgelati | Snack e fritti | Arancini surgelati |
| Pronti e impasti surgelati | Snack e fritti | Mozzarelline panate surgelate |
| Pronti e impasti surgelati | Snack e fritti | Anelli di cipolla in pastella surgelati |
| Pronti e impasti surgelati | Snack e fritti | Pizzelle fritte surgelate |
| Gelati e ghiaccio | Gelati e semifreddi | Gelato alla vaniglia |
| Gelati e ghiaccio | Gelati e semifreddi | Semifreddo al caffè |
| Gelati e ghiaccio | Ghiaccio alimentare | Ghiaccio in cubetti |

### Dispensa

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Pasta, riso e cereali | Noodles e altri formati secchi | Noodles di riso |
| Farine, lieviti e preparati | Farine di legumi e alternative | Farina di ceci |
| Farine, lieviti e preparati | Farine di legumi e alternative | Farina di mandorle |
| Farine, lieviti e preparati | Preparati per pane, pizza e dolci | Preparato per pancake |
| Farine, lieviti e preparati | Preparati per pane, pizza e dolci | Preparato per pizza |
| Farine, lieviti e preparati | Ingredienti e decorazioni per pasticceria | Gelatina in fogli |
| Farine, lieviti e preparati | Ingredienti e decorazioni per pasticceria | Vaniglia in bacca |
| Legumi, frutta secca e semi | Frutta essiccata e disidratata | Uvetta |
| Legumi, frutta secca e semi | Frutta essiccata e disidratata | Albicocche secche |
| Legumi, frutta secca e semi | Semi alimentari | Semi di chia |
| Legumi, frutta secca e semi | Semi alimentari | Semi di lino |
| Conserve e piatti pronti | Frutta sciroppata e puree | Pesche sciroppate |
| Conserve e piatti pronti | Frutta sciroppata e puree | Purea di mela |
| Conserve e piatti pronti | Zuppe e piatti pronti a temperatura ambiente | Zuppa pronta in barattolo |
| Oli, condimenti e salse | Brodi, dadi e fondi | Brodo vegetale |
| Oli, condimenti e salse | Brodi, dadi e fondi | Dado da brodo |
| Colazione e dolcificanti | Cereali da colazione e muesli | Fiocchi d'avena |
| Colazione e dolcificanti | Cereali da colazione e muesli | Muesli |
| Colazione e dolcificanti | Cereali da colazione e muesli | Corn flakes |
| Colazione e dolcificanti | Fette biscottate | Fette biscottate integrali |
| Ingredienti e prodotti speciali | Alghe alimentari | Alga nori |
| Ingredienti e prodotti speciali | Prodotti dietetici e sostitutivi del pasto | Barretta sostitutiva del pasto |

### Bevande

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Succhi e nettari | Smoothie e centrifugati | Smoothie |
| Bibite analcoliche | Kombucha e bevande fermentate analcoliche | Kombucha |
| Bibite analcoliche | Aperitivi analcolici e mocktail pronti | Aperitivo analcolico |
| Bibite analcoliche | Aperitivi analcolici e mocktail pronti | Mocktail mojito |
| Sport ed energia | Bevande isotoniche e idratanti | Bevanda isotonica |
| Sport ed energia | Bevande proteiche pronte | Frullato proteico pronto |
| Sport ed energia | Solubili energetici o per l'idratazione | Preparato isotonico in polvere |
| Caffè, tè e bevande da preparare | Caffè solubile, orzo e succedanei | Caffè solubile |
| Caffè, tè e bevande da preparare | Caffè solubile, orzo e succedanei | Orzo solubile |
| Caffè, tè e bevande da preparare | Polveri per bevande fredde | Preparato solubile per tè freddo |
| Miscelatori e sciroppi | Toniche e soda | Acqua tonica |
| Miscelatori e sciroppi | Ginger ale e ginger beer analcolici | Ginger beer analcolica |
| Miscelatori e sciroppi | Sciroppi e concentrati per bevande | Sciroppo di sambuco |
| Miscelatori e sciroppi | Sciroppi e concentrati per bevande | Sciroppo di menta |
| Miscelatori e sciroppi | Sciroppi e concentrati per bevande | Granatina |
| Miscelatori e sciroppi | Preparati analcolici per cocktail | Premix analcolico per margarita |
| Birre | Birre scure e speciali | Birra stout |
| Birre | Birre scure e speciali | Birra porter |
| Vini e spumanti | Vini dolci e passiti | Passito |
| Vini e spumanti | Vini fortificati | Marsala |
| Vini e spumanti | Vini fortificati | Porto |
| Vini e spumanti | Vini dealcolati | Vino bianco dealcolato |
| Aperitivi e cocktail alcolici | Bitter e aperitivi alcolici | Bitter da aperitivo |
| Aperitivi e cocktail alcolici | Vermouth e vini aromatizzati | Vermouth rosso |
| Aperitivi e cocktail alcolici | Vermouth e vini aromatizzati | Vermouth bianco |
| Aperitivi e cocktail alcolici | Spritz e cocktail pronti | Hugo pronto da bere |
| Aperitivi e cocktail alcolici | Spritz e cocktail pronti | Spritz pronto |
| Aperitivi e cocktail alcolici | Spritz e cocktail pronti | Negroni pronto |
| Aperitivi e cocktail alcolici | Premix alcolici | Base alcolica per cocktail |
| Aperitivi e cocktail alcolici | Sidro, idromele e altre bevande fermentate alcoliche | Sidro di mele |
| Aperitivi e cocktail alcolici | Sidro, idromele e altre bevande fermentate alcoliche | Idromele |
| Distillati e liquori | Grappa e acquaviti | Grappa |
| Distillati e liquori | Grappa e acquaviti | Acquavite di pere |
| Distillati e liquori | Gin e vodka | Gin |
| Distillati e liquori | Gin e vodka | Vodka |
| Distillati e liquori | Rum e distillati di canna | Rum bianco |
| Distillati e liquori | Rum e distillati di canna | Rum scuro |
| Distillati e liquori | Whisky e whiskey | Scotch whisky |
| Distillati e liquori | Whisky e whiskey | Bourbon |
| Distillati e liquori | Brandy e cognac | Brandy |
| Distillati e liquori | Brandy e cognac | Cognac |
| Distillati e liquori | Tequila e mezcal | Tequila |
| Distillati e liquori | Tequila e mezcal | Mezcal |
| Distillati e liquori | Amari e digestivi | Amaro alle erbe |
| Distillati e liquori | Amari e digestivi | Fernet |
| Distillati e liquori | Liquori all'anice | Anisetta |
| Distillati e liquori | Liquori all'anice | Sambuca |
| Distillati e liquori | Liquori alla frutta, alle erbe e ai fiori | Limoncello |
| Distillati e liquori | Liquori alla frutta, alle erbe e ai fiori | Liquore al sambuco |
| Distillati e liquori | Liquori alla frutta, alle erbe e ai fiori | Nocino |
| Distillati e liquori | Liquori cremosi | Crema di whisky |
| Distillati e liquori | Liquori cremosi | Crema di limoncello |
| Distillati e liquori | Altri distillati e liquori | Liquore al caffè |

### Pulizia e consumabili casa

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Bucato | Smacchiatori e additivi | Smacchiatore bucato |
| Bucato | Smacchiatori e additivi | Percarbonato per bucato |
| Bucato | Cura della lavatrice | Pulitore lavatrice |
| Piatti e lavastoviglie | Sale, brillantante e cura lavastoviglie | Brillantante lavastoviglie |
| Piatti e lavastoviglie | Sale, brillantante e cura lavastoviglie | Sale lavastoviglie |
| Detergenza casa | Vetri e specchi | Detergente vetri |
| Detergenza casa | Bagno e WC | Detergente WC |
| Detergenza casa | Cucina e forno | Pulitore forno |
| Accessori per pulire | Spugne, panni e pagliette | Spugna per piatti |
| Accessori per pulire | Spugne, panni e pagliette | Panno in microfibra |
| Accessori per pulire | Scope, mop e secchi | Mocio |
| Accessori per pulire | Scope, mop e secchi | Secchio per pavimenti |
| Accessori per pulire | Ricambi e sacchetti aspirapolvere | Sacchetti aspirapolvere |
| Carta, rifiuti e monouso | Carta igienica | Carta igienica |
| Profumazione e protezione | Profumi per armadi e tessili | Sacchetti profumati per armadio |
| Profumazione e protezione | Profumi per armadi e tessili | Spray profumatore tessuti |
| Profumazione e protezione | Assorbiumidità e neutralizzatori | Ricarica assorbiumidità |
| Profumazione e protezione | Antitarme e antinsetti per casa | Antitarme per armadio |
| Stenditura e cura degli indumenti | Rulli levapelucchi | Rullo levapelucchi |
| Stenditura e cura degli indumenti | Cura scarpe e tessili | Lucido per scarpe |

### Cucina e tavola

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Cottura | Pentole e casseruole | Pentola a pressione |
| Cottura | Pentole e casseruole | Casseruola |
| Cottura | Padelle e bistecchiere | Padella antiaderente |
| Cottura | Padelle e bistecchiere | Bistecchiera |
| Cottura | Teglie, stampi e pirofile | Teglia da forno |
| Cottura | Teglie, stampi e pirofile | Stampo per muffin |
| Cottura | Coperchi e accessori da cottura | Coperchio per padella |
| Preparazione | Coltelli da cucina e taglieri | Coltello da chef |
| Preparazione | Coltelli da cucina e taglieri | Tagliere |
| Preparazione | Mestoli, spatole e utensili | Mestolo |
| Preparazione | Mestoli, spatole e utensili | Spatola da cucina |
| Preparazione | Grattugie, pelapatate e apriscatole | Pelapatate |
| Preparazione | Grattugie, pelapatate e apriscatole | Apriscatole |
| Preparazione | Colini e scolapasta | Scolapasta |
| Preparazione | Colini e scolapasta | Colino |
| Preparazione | Misurini e bilance da cucina | Bilancia da cucina |
| Preparazione | Utensili per pasticceria | Sac à poche |
| Preparazione | Utensili per pasticceria | Frusta da cucina |
| Tavola e servizio | Piatti e ciotole | Piatti piani |
| Tavola e servizio | Piatti e ciotole | Ciotola da tavola |
| Tavola e servizio | Posate | Set di posate |
| Tavola e servizio | Bicchieri, tazze e calici | Calici da vino |
| Tavola e servizio | Bicchieri, tazze e calici | Tazza da colazione |
| Tavola e servizio | Caraffe e accessori da servizio | Caraffa per acqua |
| Tavola e servizio | Accessori vino e bar | Cavatappi |
| Tavola e servizio | Accessori vino e bar | Shaker da cocktail |
| Conservazione e trasporto | Contenitori alimentari | Contenitore ermetico per alimenti |
| Conservazione e trasporto | Barattoli e bottiglie | Barattolo da conserva |
| Conservazione e trasporto | Borracce e thermos | Borraccia |
| Conservazione e trasporto | Borracce e thermos | Thermos |
| Conservazione e trasporto | Borse termiche e portapranzo | Borsa termica |
| Conservazione e trasporto | Borse termiche e portapranzo | Portapranzo |
| Piccoli elettrodomestici da cucina | Caffettiere e macchine da caffè | Moka |
| Piccoli elettrodomestici da cucina | Caffettiere e macchine da caffè | Macchina da caffè a capsule |
| Piccoli elettrodomestici da cucina | Bollitori e tostapane | Bollitore elettrico |
| Piccoli elettrodomestici da cucina | Bollitori e tostapane | Tostapane |
| Piccoli elettrodomestici da cucina | Frullatori e robot da cucina | Frullatore a immersione |
| Piccoli elettrodomestici da cucina | Friggitrici e altri apparecchi di cottura | Friggitrice ad aria |
| Piccoli elettrodomestici da cucina | Filtri e ricambi da cucina | Filtro per caraffa filtrante |

### Casa e arredamento

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Arredo e organizzazione | Scatole, cesti e organizer | Scatola portaoggetti |
| Arredo e organizzazione | Appendiabiti e accessori armadio | Grucce per abiti |
| Tessili casa | Biancheria letto | Lenzuola |
| Tessili casa | Biancheria letto | Federa |
| Tessili casa | Tovaglie e tessili cucina | Canovaccio |
| Tessili casa | Tovaglie e tessili cucina | Tovaglia |
| Tessili casa | Tende, tappeti e cuscini | Tenda oscurante |
| Decorazione | Cornici e decorazioni domestiche | Cornice portafoto |
| Decorazione | Magneti e accessori decorativi | Magnete decorativo da frigorifero |
| Decorazione | Decorazioni stagionali | Ghirlanda natalizia |
| Bagno e accessori | Dispenser e portasapone | Dispenser per sapone |
| Bagno e accessori | Tappetini e accessori doccia | Tappetino antiscivolo per doccia |
| Elettrodomestici casa | Lavaggio e asciugatura | Lavatrice |
| Elettrodomestici casa | Aspirazione e stiro | Aspirapolvere |
| Elettrodomestici casa | Aspirazione e stiro | Ferro da stiro |

### Igiene e cura della persona

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Corpo e igiene personale | Igiene intima | Detergente intimo |
| Corpo e igiene personale | Protezioni mestruali | Assorbenti |
| Corpo e igiene personale | Protezioni mestruali | Coppetta mestruale |
| Capelli | Styling e colorazione | Gel per capelli |
| Viso, pelle e cosmetica | Trucco e struccanti | Mascara |
| Viso, pelle e cosmetica | Trucco e struccanti | Acqua micellare |
| Rasatura e depilazione | Prodotti e accessori depilatori | Strisce depilatorie |

### Salute e benessere

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Medicazione e protezione | Kit di primo soccorso | Kit di primo soccorso domestico |
| Misurazione e supporti | Termometri e strumenti di misurazione | Termometro digitale |
| Cura e assistenza | Prodotti per incontinenza | Assorbenti per incontinenza |

### Infanzia

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Alimentazione | Omogeneizzati e pappe | Omogeneizzato di mela |
| Alimentazione | Merende per l'infanzia | Biscotti per l'infanzia |
| Riposo e passeggio | Tessili e accessori nanna | Sacco nanna |

### Animali

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Alimenti per gatti | Umido gatto | Bocconcini per gatti |
| Alimenti per gatti | Snack gatto | Snack per gatti |
| Alimenti altri animali | Alimenti per uccelli | Mangime per canarini |
| Alimenti altri animali | Alimenti per roditori | Pellet per conigli |
| Alimenti altri animali | Alimenti per pesci e altri animali | Mangime in scaglie per pesci |
| Igiene e cura animali | Sacchetti e traversine | Sacchetti igienici per cani |
| Accessori animali | Cucce e trasportini | Trasportino per animali |
| Accessori animali | Accessori acquario e gabbia | Filtro per acquario |

### Ferramenta, manutenzione e giardino

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Utensili e minuteria | Utensili manuali | Cacciavite |
| Utensili e minuteria | Utensili manuali | Pinza |
| Utensili e minuteria | Elettroutensili e punte | Trapano |
| Utensili e minuteria | Elettroutensili e punte | Punta da muro |
| Utensili e minuteria | Viti, chiodi e tasselli | Tasselli da muro |
| Utensili e minuteria | Viti, chiodi e tasselli | Viti per legno |
| Utensili e minuteria | Ganci, cerniere, serrature e magneti tecnici | Magnete da fissaggio |
| Utensili e minuteria | Ganci, cerniere, serrature e magneti tecnici | Cerniera per mobile |
| Riparazione e finiture | Colle tecniche e sigillanti | Silicone sigillante |
| Riparazione e finiture | Nastri tecnici e isolanti | Nastro isolante |
| Riparazione e finiture | Vernici e accessori | Smalto per legno |
| Riparazione e finiture | Vernici e accessori | Rullo per pittura |
| Elettricità, luce e idraulica | Prese, prolunghe e adattatori di rete | Multipresa |
| Elettricità, luce e idraulica | Prese, prolunghe e adattatori di rete | Prolunga elettrica |
| Elettricità, luce e idraulica | Rubinetteria, guarnizioni e piccoli ricambi idraulici | Guarnizione |
| Giardino e balcone | Terricci e fertilizzanti | Terriccio universale |
| Giardino e balcone | Irrigazione e attrezzi da giardino | Annaffiatoio |
| Esterno e barbecue | Griglie e accessori barbecue | Pinza da barbecue |

### Elettronica e ufficio

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Telefonia e informatica | Cavi, caricabatterie e power bank | Cavo USB-C |
| Telefonia e informatica | Cavi, caricabatterie e power bank | Caricabatterie USB |
| Telefonia e informatica | Cavi, caricabatterie e power bank | Power bank |
| Telefonia e informatica | Memorie e archiviazione | Chiavetta USB |
| Telefonia e informatica | Memorie e archiviazione | SSD esterno |
| Audio, video e fotografia | Cuffie e altoparlanti | Cuffie Bluetooth |
| Audio, video e fotografia | TV, telecomandi e accessori video | Cavo HDMI |
| Audio, video e fotografia | TV, telecomandi e accessori video | Telecomando universale |
| Stampa e materiali ufficio | Cartucce e toner | Cartuccia per stampante |
| Stampa e materiali ufficio | Cartucce e toner | Toner per stampante |
| Stampa e materiali ufficio | Carta da stampa ed etichette | Risma A4 |

### Cartoleria, scuola e creatività

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Materiali scolastici e creativi | Colori, pennelli e materiali da modellare | Acquerelli |
| Materiali scolastici e creativi | Colori, pennelli e materiali da modellare | Pasta modellabile |
| Materiali scolastici e creativi | Tessuti, filati e merceria creativa | Filo da cucito |
| Materiali scolastici e creativi | Tessuti, filati e merceria creativa | Aghi da cucito |

### Abbigliamento e accessori

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Abbigliamento | Intimo e calze | Calzini |
| Calzature e accessori indossabili | Solette e lacci | Lacci per scarpe |
| Calzature e accessori indossabili | Orologi, cinturini e maglie | Cinturino per orologio |
| Calzature e accessori indossabili | Orologi, cinturini e maglie | Maglia di ricambio per orologio |

### Sport, viaggi e mobilità

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Sport e fitness | Accessori sportivi e protezioni | Guanti da palestra |
| Sport e fitness | Accessori nuoto e sport acquatici | Occhialini da nuoto |
| Viaggio e attività all'aperto | Accessori da viaggio | Adattatore da viaggio |
| Viaggio e attività all'aperto | Accessori da viaggio | Lucchetto per valigia |
| Auto, moto e bici | Cura e pulizia veicoli | Profumatore per auto |
| Auto, moto e bici | Cura e pulizia veicoli | Shampoo per auto |
| Auto, moto e bici | Lubrificanti e liquidi veicoli | Liquido lavavetri |
| Auto, moto e bici | Accessori e ricambi bici | Camera d'aria per bici |

### Giochi, cultura, feste e regali

| L2 | L3 | Nuovo prodotto |
|---|---|---|
| Giochi e hobby | Giochi da tavolo e carte | Mazzo di carte |
| Giochi e hobby | Puzzle e costruzioni | Puzzle |
| Feste e regalo | Addobbi e articoli da festa | Palloncini |
| Feste e regalo | Addobbi e articoli da festa | Candeline per torta |
| Feste e regalo | Biglietti e confezioni regalo | Carta regalo |
| Feste e regalo | Biglietti e confezioni regalo | Biglietto di auguri |


## 9. Esclusioni e consegna all’agente

Le decisioni sui prodotti sono state recepite. La tabella seguente è il registro delle esclusioni dal catalogo preinstallato.

| ID escluso | Nome storico |
|---:|---|
| 103 | Insalata Mista in vaso |
| 107 | Mix per Wok in vaso |
| 113 | Insalata Mista in bustina |
| 117 | Mix per Wok in bustina |
| 192 | Camoscio |
| 247 | Salsiccia di Caccia |
| 309 | Porchetta Aromatica |
| 364 | Codero Affumicato |
| 407 | Acqua Medium |
| 664 | Croccantini Surgelati |
| 665 | Insalata Surgelata |
| 1201 | Fornetto Monouso |
| 1259 | Fornetto Monouso Ricaricabile |
| 1275 | Collo |
| 1329 | Collo Extra |
| 1386 | Bocconcini in Conserve Eco |

Nuovi prodotti esclusi prima dell’inserimento: Pizza margherita al taglio, Soia testurizzata.

### Regola per i dati esistenti

Escludi questi ID dal nuovo catalogo selezionabile e dal seed delle nuove installazioni. In un’installazione aggiornata, conserva come voci storiche gli ID ancora referenziati da liste o storico. Il dato deve rimanere leggibile e modificabile dalla relativa lista. Escludi queste voci dai suggerimenti del catalogo, dall’aggiunta di nuove voci e dalla risoluzione di sinonimi per nuovi acquisti. Gestisci eventuali regole apprese senza farle proporre un prodotto escluso. L’agente può scegliere come rappresentare questo stato dopo aver verificato il modello Room.

L’ID 664 resta associato a Croccantini Surgelati per i dati storici. I fritti aggiunti hanno ID nuovi. Se un ID Bio della sezione 10 confluisce in un ID escluso qui, applica lo stesso trattamento storico anche dopo l’accorpamento.

### Consegna per l’implementazione

1. Usa la sezione 3 per le triplette, la 7 per i prodotti esistenti da mantenere, la 8 per i prodotti nuovi e i sinonimi, la 9 per le esclusioni e la 10 per gli accorpamenti Bio.
2. Verifica che ogni ID originale appartenga a un solo insieme tra mantenuti, esclusi e accorpati. Ogni destinazione della sezione 10 deve essere un ID mantenuto o escluso con trattamento storico. Assegna ID nuovi senza collisioni.
3. Aggiorna insieme il generatore, il seed e la migrazione Room. Preserva lista attiva, storico, prodotti utente, sinonimi e regole apprese. Verifica i casi in cui il prodotto base e la variante Bio coesistono e quelli in cui il prodotto base è escluso dal seed.
4. Verifica ricerca, aggiunta e import. Prova tutti i sinonimi della sezione 8, un fritto surgelato, una lista mista con azioni e un vecchio export con categoria frazionata.

## 10. Variante Bio e accorpamenti

Bio è una variante del prodotto base. Lo stesso criterio si applica alle diciture Biologico, Biologica, Biologici e Biologiche.

Il catalogo proposto conserva **607 prodotti esistenti** e aggiunge **285 nuovi prodotti**. Alcuni ID base sono esclusi dal nuovo catalogo secondo la sezione 9. I **125 ID ritirati** qui sotto servono esclusivamente per la migrazione.

Rimappa al prodotto base i riferimenti di lista, storico, sinonimi e regole apprese prima di ritirare gli ID duplicati. Conserva l’indicazione Bio come variante dove il modello la supporta e preserva eventuali altri attributi. Se una lista o lo storico contengono già sia l’ID base sia il duplicato, verifica il risultato dell’accorpamento per preservare quantità e informazioni di acquisto. Le vecchie espressioni di ricerca o import con Bio devono poter risolvere il prodotto base e la variante, dove prevista.

| ID ritirato | ID base mantenuto | Prodotto base |
|---:|---:|---|
| 31 | 20 | Prugna |
| 32 | 15 | Melone Cantalupo |
| 33 | 11 | Fragola |
| 34 | 27 | Fico |
| 35 | 23 | Uva Nera |
| 36 | 7 | Arancia |
| 37 | 6 | Banana |
| 38 | 13 | Mirtillo |
| 39 | 12 | Lampone |
| 40 | 16 | Melone Retato |
| 71 | 55 | Melanzana |
| 72 | 64 | Cicoria |
| 73 | 56 | Peperone Rosso |
| 74 | 52 | Cipolla Bianca |
| 75 | 57 | Peperone Verde |
| 76 | 61 | Broccoli |
| 77 | 58 | Peperoncino |
| 78 | 48 | Carota |
| 79 | 67 | Porro |
| 80 | 60 | Finocchio |
| 91 | 83 | Rosmarino |
| 92 | 85 | Origano |
| 93 | 88 | Insalata Mista |
| 94 | 82 | Prezzemolo |
| 95 | 87 | Coriandolo |
| 96 | 81 | Basilico |
| 97 | 89 | Mix per Wok |
| 98 | 84 | Salvia |
| 99 | 86 | Menta |
| 100 | 90 | Olive Verdi |
| 137 | 131 | Grissini |
| 138 | 124 | Pane di Segale |
| 139 | 132 | Grissini Integrali |
| 140 | 121 | Pane Casereccio |
| 141 | 133 | Pane Azzimo |
| 142 | 135 | Panino al Sesamo |
| 143 | 129 | Ciabatta |
| 144 | 130 | Panino Integrale |
| 145 | 123 | Pane Pugliese |
| 146 | 126 | Pane Francese |
| 147 | 136 | Pane |
| 148 | 125 | Pane Integrale |
| 149 | 127 | Focaccia Genovese |
| 150 | 122 | Pane Toscano |
| 163 | 151 | Croissant |
| 164 | 161 | Sfogliatina |
| 165 | 162 | Bombolone |
| 166 | 152 | Cornetto |
| 167 | 155 | Krapfen |
| 168 | 156 | Muffin |
| 169 | 160 | Panzerotto |
| 170 | 157 | Saccottino |
| 171 | 158 | Treccia |
| 172 | 153 | Pane al Latte |
| 173 | 154 | Brioche |
| 174 | 159 | Fagottino |
| 193 | 191 | Frollino |
| 194 | 185 | Pan di Spagna |
| 195 | 189 | Tartufo Dolce |
| 196 | 188 | Eclair |
| 197 | 182 | Merenda Dolce |
| 198 | 190 | Brioche Farcita |
| 199 | 183 | Frolla |
| 200 | 187 | Crostatina |
| 201 | 186 | Torta allo Yogurt |
| 202 | 192 | Camoscio |
| 203 | 184 | Ciambella |
| 204 | 181 | Tortina |
| 227 | 220 | Trancio di Manzo |
| 228 | 211 | Tagliata di Manzo |
| 229 | 212 | Filetto di Manzo |
| 230 | 217 | Stufato di Manzo |
| 231 | 219 | Roastbeef |
| 232 | 226 | Manzo |
| 233 | 216 | Costine di Manzo |
| 234 | 224 | Goulash |
| 235 | 214 | Hamburger di Manzo |
| 236 | 222 | Involtino di Manzo |
| 237 | 215 | Polpettone |
| 238 | 213 | Carpaccio |
| 239 | 218 | Brasato |
| 240 | 221 | Sottofiletto |
| 254 | 245 | Lombo di Maiale |
| 255 | 252 | Tagliatelle di Maiale |
| 256 | 253 | Maiale |
| 257 | 244 | Lonza di Maiale |
| 258 | 247 | Salsiccia di Caccia |
| 259 | 246 | Salsiccia |
| 260 | 251 | Straccetti di Maiale |
| 261 | 250 | Filetto di Maiale |
| 262 | 241 | Costine di Maiale |
| 263 | 242 | Pancetta |
| 264 | 248 | Porchetta |
| 265 | 249 | Pancetta Arrotolata |
| 266 | 243 | Guanciale |
| 284 | 282 | Petto di Tacchino |
| 285 | 272 | Cosce di Pollo |
| 286 | 274 | Ali di Pollo |
| 287 | 273 | Sovracosce |
| 288 | 276 | Faraona |
| 289 | 271 | Petto di Pollo |
| 290 | 281 | Coscia di Tacchino |
| 291 | 275 | Tacchino a Fette |
| 292 | 279 | Hamburger di Pollo |
| 293 | 278 | Salsiccia di Pollo |
| 294 | 280 | Straccetti di Pollo |
| 295 | 283 | Pollo |
| 296 | 277 | Coniglio |
| 647 | 620 | Crema di Burro |
| 648 | 616 | Panna da Cucina |
| 649 | 622 | Panna |
| 650 | 615 | Burro Salato |
| 692 | 696 | Uova |
| 727 | 695 | Tuorlo d'Uovo |
| 728 | 693 | Uova da Galline all'Aperto |
| 729 | 691 | Uova Fresche |
| 730 | 694 | Uova di Codornice |
| 887 | 854 | Chips di Patate |
| 888 | 856 | Barretta ai Cereali |
| 889 | 851 | Cracker |
| 890 | 857 | Noccioline |
| 1047 | 1017 | Senape |
| 1048 | 1016 | Maionese |
| 1049 | 1015 | Ketchup |
| 1050 | 1018 | Hummus |

## Appendice — Inventario integrale del catalogo attuale

Nella tabella, ogni L3 riporta il numero di prodotti canonici associati nel database preinstallato. Il conteggio misura le assegnazioni effettive, anche quando sono incongruenti con l'etichetta.

| L1 | L2 | L3 attuali (prodotti canonici associati) |
|---|---|---|
| Ortofrutta | Frutta | Frutta fresca (14); Frutta tropicale (13); Frutta secca (13) |
| Ortofrutta | Verdura | Ortaggi freschi (14); Verdura confezionata (13); Ortaggi da cucina (13) |
| Ortofrutta | Erbe e contorni | Erbe aromatiche (20); Insalate e mix (20) |
| Panetteria | Pane | Pane tradizionale (10); Pane speciale (10); Pane senza glutine (10) |
| Panetteria | Panificati | Panificati al burro (12); Panificati salati (12) |
| Panetteria | Dolci da forno | Dolci da forno tradizionali (12); Merendine da forno (12) |
| Carne | Carne bovina | Tagli di manzo (10); Carne bovina macinata (10); Preparati bovini (10) |
| Carne | Carne suina | Tagli di maiale (13); Preparati suini (13) |
| Carne | Pollame | Tagli di pollo (8); Pollame intero (10); Carni alternative (8) |
| Carne | Salumi freschi | Salsicce fresche (6); Salumi cotti freschi (6) |
| Pesce | Pesce fresco | Pesce bianco fresco (5); Pesce rosso fresco (5); Pesce pregiato (5) |
| Pesce | Pesce confezionato | Pesce in scatola (6); Pesce affumicato (5) |
| Pesce | Crostacei e molluschi | Molluschi (10); Crostacei (10) |
| Bevande | Acqua e minerali | Acqua naturale (6); Acqua frizzante (5) |
| Bevande | Succhi e nettari | Succhi di frutta (6); Nettari e spremute (6) |
| Bevande | Bibite | Bibite gassate (4); Bibite analcoliche (4); Tè freddo (4) |
| Bevande | Birra | Birra chiara (5); Birra speciale (3); Birra analcolica (3) |
| Bevande | Vino | Vino rosso (5); Vino bianco (3); Vino spumante (3) |
| Banco Frigo | Latte e yogurt | Latte fresco (6); Alternative vegetali (5); Yogurt (6); Yogurt speciali (5) |
| Banco Frigo | Formaggi | Formaggi freschi (5); Formaggi stagionati (5); Formaggi a pasta morbida (5); Formaggi particolari (5) |
| Banco Frigo | Burro e panna | Burro (8); Panna (8) |
| Banco Frigo | Surgelati | Ortaggi surgelati (5); Preparati surgelati (5); Gelati (5) |
| Banco Frigo | Uova | Uova fresche (10) |
| Dispensa | Pasta e riso | Pasta secca (6); Pasta fresca (5); Riso e cereali (6); Couscous e polenta (5) |
| Dispensa | Oli e condimenti | Oli (5); Aceti (5); Sale e spezie (5) |
| Dispensa | Conserve | Conserve di pomodoro (7); Legumi in scatola (5); Sott'oli e sottaceti (5) |
| Dispensa | Snack e cracker | Salati (6); Dolci (5); Frutta secca (5) |
| Dispensa | Dolci e cioccolato | Cioccolato (5); Biscotti (4); Dolci e creme (7) |
| Dispensa | Caffè e tè | Caffè (7); Tè e infusi (7) |
| Dispensa | Legumi e farine | Farine e lieviti (7); Legumi secchi (7) |
| Dispensa | Salse e pesti | Salse pronte (8); Condimenti pronti (8) |
| Non alimentare | Igiene persona | Igiene dentale (5); Cura del corpo (6); Cura dei capelli (5); Rasatura e depilazione (5) |
| Non alimentare | Pulizia e lavatrice | Detersivi per bucato (8); Detersivi per piatti (7); Pulizia generale (6) |
| Non alimentare | Cura della casa | Cucina monouso (9); Illuminazione e batterie (8); Piccola ferramenta (8) |
| Non alimentare | Cartoleria | Scrittura (9); Cancelleria (8); Materiali scolastici (8) |
| Non alimentare | Articoli per animali | Alimentazione animali (12); Cura e giochi animali (12) |
| Non alimentare | Bambini e neonati | Pannolini e salviette (12); Cura bambini (12) |
