#!/usr/bin/env python3
"""
Generatore deterministico di app/src/main/assets/catalog.db per Story 1.4.

Crea un database SQLite il cui schema coincide esattamente con le entity Room
di TogoDatabase (tabelle, colonne, tipi, indici, user_version=1), così che
Room possa aprirlo con createFromAsset() senza errori di validazione.

AC Story 1.4:
  - >= 1000 prodotti canonici alimentari
  - >= 400 prodotti canonici non alimentari
  - ogni prodotto: level3_id NOT NULL, name non vuoto, is_user_defined = 0
  - ordine tassonomia: Ortofrutta -> Panetteria -> Carne -> ... -> Banco Frigo
    -> Dispensa -> Non alimentare (ancoraggio Story 2.1)
  - deterministico (seed fisso) -> rigenerazione produce lo stesso contenuto

Uso: python tools/generate_catalog.py
"""
from __future__ import annotations

import random
import sqlite3
import sys
import unicodedata
from pathlib import Path

SEED = 42
ASSET_PATH = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "catalog.db"

MIN_FOOD = 1000
MIN_NON_FOOD = 400
NON_FOOD_L1 = "Non alimentare"

# ---------------------------------------------------------------------------
# Tassonomia: L1 (ordine corsie) -> L2 -> L3
# ---------------------------------------------------------------------------
TAXONOMY: list[tuple[str, list[tuple[str, list[str]]]]] = [
    ("Ortofrutta", [
        ("Frutta", ["Frutta fresca", "Frutta tropicale", "Frutta secca"]),
        ("Verdura", ["Ortaggi freschi", "Verdura confezionata", "Ortaggi da cucina"]),
        ("Erbe e contorni", ["Erbe aromatiche", "Insalate e mix"]),
    ]),
    ("Panetteria", [
        ("Pane", ["Pane tradizionale", "Pane speciale", "Pane senza glutine"]),
        ("Panificati", ["Panificati al burro", "Panificati salati"]),
        ("Dolci da forno", ["Dolci da forno tradizionali", "Merendine da forno"]),
    ]),
    ("Carne", [
        ("Carne bovina", ["Tagli di manzo", "Carne bovina macinata", "Preparati bovini"]),
        ("Carne suina", ["Tagli di maiale", "Preparati suini"]),
        ("Pollame", ["Tagli di pollo", "Pollame intero", "Carni alternative"]),
        ("Salumi freschi", ["Salsicce fresche", "Salumi cotti freschi"]),
    ]),
    ("Pesce", [
        ("Pesce fresco", ["Pesce bianco fresco", "Pesce rosso fresco", "Pesce pregiato"]),
        ("Pesce confezionato", ["Pesce in scatola", "Pesce affumicato"]),
        ("Crostacei e molluschi", ["Molluschi", "Crostacei"]),
    ]),
    ("Bevande", [
        ("Acqua e minerali", ["Acqua naturale", "Acqua frizzante"]),
        ("Succhi e nettari", ["Succhi di frutta", "Nettari e spremute"]),
        ("Bibite", ["Bibite gassate", "Bibite analcoliche", "Tè freddo"]),
        ("Birra", ["Birra chiara", "Birra speciale", "Birra analcolica"]),
        ("Vino", ["Vino rosso", "Vino bianco", "Vino spumante"]),
    ]),
    ("Banco Frigo", [
        ("Latte e yogurt", ["Latte fresco", "Alternative vegetali", "Yogurt", "Yogurt speciali"]),
        ("Formaggi", ["Formaggi freschi", "Formaggi stagionati", "Formaggi a pasta morbida", "Formaggi particolari"]),
        ("Burro e panna", ["Burro", "Panna"]),
        ("Surgelati", ["Ortaggi surgelati", "Preparati surgelati", "Gelati"]),
        ("Uova", ["Uova fresche"]),
    ]),
    ("Dispensa", [
        ("Pasta e riso", ["Pasta secca", "Pasta fresca", "Riso e cereali", "Couscous e polenta"]),
        ("Oli e condimenti", ["Oli", "Aceti", "Sale e spezie"]),
        ("Conserve", ["Conserve di pomodoro", "Legumi in scatola", "Sott'oli e sottaceti"]),
        ("Snack e cracker", ["Salati", "Dolci", "Frutta secca"]),
        ("Dolci e cioccolato", ["Cioccolato", "Biscotti", "Dolci e creme"]),
        ("Caffè e tè", ["Caffè", "Tè e infusi"]),
        ("Legumi e farine", ["Farine e lieviti", "Legumi secchi"]),
        ("Salse e pesti", ["Salse pronte", "Condimenti pronti"]),
    ]),
    ("Non alimentare", [
        ("Igiene persona", ["Igiene dentale", "Cura del corpo", "Cura dei capelli", "Rasatura e depilazione"]),
        ("Pulizia e lavatrice", ["Detersivi per bucato", "Detersivi per piatti", "Pulizia generale"]),
        ("Cura della casa", ["Cucina monouso", "Illuminazione e batterie", "Piccola ferramenta"]),
        ("Cartoleria", ["Scrittura", "Cancelleria", "Materiali scolastici"]),
        ("Articoli per animali", ["Alimentazione animali", "Cura e giochi animali"]),
        ("Bambini e neonati", ["Pannolini e salviette", "Cura bambini"]),
    ]),
]

# ---------------------------------------------------------------------------
# Basi prodotto per (L1, L2)
# ---------------------------------------------------------------------------
BASES: dict[tuple[str, str], list[str]] = {
    ("Ortofrutta", "Frutta"): [
        "Mela Golden", "Mela Renetta", "Mela Fuji", "Pera Williams", "Pera Conference",
        "Banana", "Arancia", "Mandarina", "Limone", "Kiwi",
        "Fragola", "Lampone", "Mirtillo", "Anguria", "Melone Cantalupo",
        "Melone Retato", "Albicocca", "Pesca", "Pesca Nettarina", "Prugna",
        "Ciliegia", "Uva Bianca", "Uva Nera", "Ananas", "Mango",
        "Melagrana", "Fico", "Castagna", "Nocciola", "Cocco",
    ],
    ("Ortofrutta", "Verdura"): [
        "Pomodoro Rametto", "Pomodoro Datterino", "Pomodoro Cuore di Bue", "Lattuga", "Rucola",
        "Iceberg", "Radicchio", "Carota", "Patata", "Patata Dolce",
        "Cipolla Rossa", "Cipolla Bianca", "Aglio", "Zucchina", "Melanzana",
        "Peperone Rosso", "Peperone Verde", "Peperoncino", "Sedano", "Finocchio",
        "Broccoli", "Cavolfiore", "Cavolo Nero", "Cicoria", "Barbabietola",
        "Rapa", "Porro", "Spinaci", "Cetriolo", "Zucca",
    ],
    ("Ortofrutta", "Erbe e contorni"): [
        "Basilico", "Prezzemolo", "Rosmarino", "Salvia", "Origano",
        "Menta", "Coriandolo", "Insalata Mista", "Mix per Wok", "Olive Verdi",
    ],
    ("Panetteria", "Pane"): [
        "Pane Casereccio", "Pane Toscano", "Pane Pugliese", "Pane di Segale", "Pane Integrale",
        "Pane Francese", "Focaccia Genovese", "Focaccia Rossa", "Ciabatta", "Panino Integrale",
        "Grissini", "Grissini Integrali", "Pane Azzimo", "Pane ai Cereali", "Panino al Sesamo",
    ],
    ("Panetteria", "Panificati"): [
        "Croissant", "Cornetto", "Pane al Latte", "Brioche", "Krapfen",
        "Muffin", "Saccottino", "Treccia", "Fagottino", "Panzerotto",
        "Sfogliatina", "Bombolone",
    ],
    ("Panetteria", "Dolci da forno"): [
        "Tortina", "Merenda Dolce", "Frolla", "Ciambella", "Pan di Spagna",
        "Torta allo Yogurt", "Crostatina", "Eclair", "Tartufo Dolce", "Brioche Farcita",
        "Frollino", "Camoscio",
    ],
    ("Carne", "Carne bovina"): [
        "Tagliata di Manzo", "Filetto di Manzo", "Carpaccio", "Hamburger di Manzo", "Polpettone",
        "Costine di Manzo", "Stufato di Manzo", "Brasato", "Roastbeef", "Trancio di Manzo",
        "Sottofiletto", "Involtino di Manzo", "Straccetti di Manzo", "Goulash", "Bistecca",
    ],
    ("Carne", "Carne suina"): [
        "Costine di Maiale", "Pancetta", "Guanciale", "Lonza di Maiale", "Lombo di Maiale",
        "Salsiccia", "Salsiccia di Caccia", "Porchetta", "Pancetta Arrotolata", "Filetto di Maiale",
        "Straccetti di Maiale", "Tagliatelle di Maiale",
    ],
    ("Carne", "Pollame"): [
        "Petto di Pollo", "Cosce di Pollo", "Sovracosce", "Ali di Pollo", "Tacchino a Fette",
        "Faraona", "Coniglio", "Salsiccia di Pollo", "Hamburger di Pollo", "Straccetti di Pollo",
        "Coscia di Tacchino", "Petto di Tacchino",
    ],
    ("Carne", "Salumi freschi"): [
        "Salsiccia Fresca", "Cotechino", "Zampone", "Luganiga", "Salsiccia di Suino",
        "Bolognese Fresco", "Polpa di Suino", "Coppa di Suino", "Porchetta Aromatica", "Salsiccia Piccante",
        "Salsiccia Dolce", "Wurstel Fresco",
    ],
    ("Pesce", "Pesce fresco"): [
        "Salmone Fresco", "Tonno Fresco", "Branzino", "Orata", "Spigola",
        "Merluzzo", "Acciughe", "Sardine", "Triglia", "Cefalo",
        "Pesce Spada", "Ricciola", "Branzino di Allevamento", "Trota", "Sgombro",
    ],
    ("Pesce", "Pesce confezionato"): [
        "Tonno in Olio", "Tonno al Naturale", "Salmone Affumicato", "Sgombro in Olio", "Sardine in Olio",
        "Merluzzo in Olio", "Tonno all'Acqua", "Surimi", "Polpa di Granchio", "Codero Affumicato",
    ],
    ("Pesce", "Crostacei e molluschi"): [
        "Gamberi Rossi", "Gamberi Tigre", "Scampi", "Astici", "Mazzancolle",
        "Cozze", "Vongole Veraci", "Seppie", "Calamari", "Polpo",
    ],
    ("Bevande", "Acqua e minerali"): [
        "Acqua Naturale", "Acqua Frizzante", "Acqua Leggermente Frizzante", "Acqua Termale", "Acqua Leggera",
        "Acqua con Ginseng", "Acqua Medium", "Acqua Plateale", "Acqua Pétillante", "Acqua Alpina",
    ],
    ("Bevande", "Succhi e nettari"): [
        "Succo di Arancia", "Succo di Mela", "Succo di Pera", "Succo di Ananas", "Succo di Carota",
        "Spremuta d'Arancia", "Succo di Melagrana", "Succo di Kiwi", "Succo di Fragola", "Nettaro di Pesca",
        "Succo di Pompelmo", "Succo Multifrutta",
    ],
    ("Bevande", "Bibite"): [
        "Cola", "Cola Zero", "Cola Light", "Limonata", "Aranciata",
        "Chinotto", "Tè Freddo Pesca", "Energy Drink", "Ginger Ale", "Soda",
        "Tè Freddo Limone", "Bibita al Malto",
    ],
    ("Bevande", "Birra"): [
        "Birra Chiara", "Birra Bionda", "Birra Rossa", "Birra IPA", "Birra Weizen",
        "Birra Zero", "Birra Senza Alcol", "Birra Ambrata", "Birra Artigianale", "Birra Lager",
    ],
    ("Bevande", "Vino"): [
        "Vino Rosso", "Vino Bianco", "Vino Rosé", "Prosecco", "Chianti",
        "Merlot", "Cabernet", "Pinot Grigio", "Montepulciano", "Lambrusco",
    ],
    ("Banco Frigo", "Latte e yogurt"): [
        "Latte Intero", "Latte Parz. Scremato", "Latte Scremato", "Latte di Soia", "Latte di Avena",
        "Latte di Mandorla", "Latte di Cocco", "Yogurt Bianco", "Yogurt Greco", "Yogurt alla Frutta",
        "Kefir", "Yogurt da Bere", "Yogurt alla Vaniglia", "Yogurt ai Frutti Rossi", "Fiordilatte Fresco",
        "Latte UHT Intero", "Latte Fresco", "Yogurt Skyr", "Crema di Yogurt", "Formaggio Spalmabile",
    ],
    ("Banco Frigo", "Formaggi"): [
        "Mozzarella", "Fiordilatte", "Grana Padano", "Parmigiano Reggiano", "Pecorino Romano",
        "Gorgonzola", "Taleggio", "Robiola", "Brie", "Camembert",
        "Emmental", "Edamer", "Fontina", "Provolone", "Ricotta",
        "Stracchino", "Mascarpone", "Caprino", "Casatella", "Toma",
    ],
    ("Banco Frigo", "Burro e panna"): [
        "Burro Dolce", "Burro Intero", "Burro di Centrifuga", "Burro Affumicato", "Burro Salato",
        "Panna da Cucina", "Panna Montabile", "Panna Addensante", "Panna da Montare", "Crema di Burro",
    ],
    ("Banco Frigo", "Surgelati"): [
        "Verdure Miste Surgelate", "Piselli Surgelati", "Mais Surgelato", "Patatine Fritte Surgelate", "Pizza Surgelata",
        "Lasagne Surgelate", "Ghiaccioli", "Sorbetto al Limone", "Tortellini Surgelati", "Hamburger Surgelato",
        "Filetti di Pesce Surgelati", "Frutti di Bosco Surgelati", "Polenta Surgelata", "Croccantini Surgelati", "Insalata Surgelata",
    ],
    ("Banco Frigo", "Uova"): [
        "Uova Fresche", "Uova Biologiche", "Uova da Galline all'Aperto", "Uova di Codornice", "Tuorlo d'Uovo",
    ],
    ("Dispensa", "Pasta e riso"): [
        "Spaghetti", "Penne", "Fusilli", "Rigatoni", "Farfalle",
        "Orecchiette", "Tagliatelle", "Pappardelle", "Lasagne", "Gnocchi",
        "Riso Carnaroli", "Riso Basmati", "Riso Arborio", "Riso Integrale", "Quinoa",
        "Couscous", "Polenta", "Spaghettini", "Mezze Penne", "Stelline",
    ],
    ("Dispensa", "Oli e condimenti"): [
        "Olio Extra Vergine di Oliva", "Olio di Semi", "Olio di Colza", "Aceto Balsamico", "Aceto di Vino",
        "Aceto di Mele", "Sale Marino", "Sale Iodato", "Pepe Nero", "Paprika",
        "Curcuma", "Zenzero in Polvere",
    ],
    ("Dispensa", "Conserve"): [
        "Pomodori Pelati", "Passata di Pomodoro", "Salsa di Pomodoro", "Mais in Lattina", "Fagioli in Lattina",
        "Ceci in Lattina", "Olive in Vaso", "Cetriolini", "Peperoni Arrostiti", "Melanzane Sott'olio",
        "Fagioli Borlotti", "Lenticchie in Conserve", "Funghi in Vaso", "Sugo Pronto", "Cruda di Pomodoro",
    ],
    ("Dispensa", "Snack e cracker"): [
        "Cracker", "Gallette", "Patatine", "Chips di Patate", "Popcorn",
        "Barretta ai Cereali", "Noccioline", "Mandorle da Snack", "Frutta Secca Mista", "Taralli",
        "Stuzzichini", "Cracker Integrali",
    ],
    ("Dispensa", "Dolci e cioccolato"): [
        "Cioccolato Fondente", "Cioccolato al Latte", "Cioccolato Bianco", "Tavoletta al Cereale", "Biscotti",
        "Merendine", "Wafer", "Tortina al Cioccolato", "Frollini", "Crema da Spalmare",
        "Caramelle", "Marshmallow",
    ],
    ("Dispensa", "Caffè e tè"): [
        "Caffè in Grani", "Caffè Macinato", "Caffè Decaffeinato", "Caffè in Cialde", "Caffè in Capsule",
        "Tè Nero", "Tè Verde", "Tè alla Menta", "Camomilla", "Infuso Rilassante",
        "Matcha", "Cacao in Polvere",
    ],
    ("Dispensa", "Legumi e farine"): [
        "Farina 00", "Farina Integrale", "Farina di Grano Tenero", "Lievito", "Fagioli Cannellini",
        "Lenticchie Secche", "Cicerchie", "Farro", "Orzo Perlato", "Ceci Secchi",
        "Amido di Mais", "Semola Rimacinata",
    ],
    ("Dispensa", "Salse e pesti"): [
        "Pesto Genovese", "Salsa di Soia", "Salsa Barbecue", "Salsa Piccante", "Ketchup",
        "Maionese", "Senape", "Hummus", "Guacamole", "Salsa Piccante Messicana",
        "Salsa Alfredo", "Tahina",
    ],
    ("Non alimentare", "Igiene persona"): [
        "Sapone da Mani", "Gel Doccia", "Shampoo", "Balsamo", "Dentifricio",
        "Spazzolino da Denti", "Filo Interdentale", "Deodorante", "Rasoi", "Schiuma da Barba",
        "Crema Idratante", "Crema Solare", "Cotone Idrofilo", "Bagnoschiuma", "Liquido Lavapanni",
        "Collutorio", "Balsamo Labbra", "Maschera Viso", "Soluzione Contatti", "Spazzolino Interdentale",
    ],
    ("Non alimentare", "Pulizia e lavatrice"): [
        "Detersivo Piatti", "Detersivo Biancheria", "Ammorbidente", "Disinfettante", "Sgrassatore",
        "Pulitore Multiuso", "Sacchi della Spazzatura", "Guanti Monouso", "Candeggina", "Deodorante per Ambienti",
        "Detersivo Pavimenti", "Detersivo in Polvere", "Liquido Lavastoviglie", "Tablet Lavastoviglie", "Sapone di Marsiglia",
        "Panetto per Lavatrice", "Spray Anticalcare", "Acqua Ossigenata", "Detersivo Delicato", "Sgrassatore Eco",
    ],
    ("Non alimentare", "Cura della casa"): [
        "Candele", "Fiammiferi", "Batterie", "Lampadine", "Pellicola Trasparente",
        "Carta Alluminio", "Carta Forno", "Carta Accappatoio", "Spago da Cucina", "Stoviglie Monouso",
        "Fornetto Monouso", "Filo da Stendere", "Spazzola da Bucato", "Ganci da Stendere", "Fazzoletti da Tavolo",
    ],
    ("Non alimentare", "Cartoleria"): [
        "Penna Blu", "Penna Nera", "Matita", "Gomma", "Righello",
        "Quaderno", "Blocco Note", "Colla", "Forbici", "Segnalibri",
        "Cartelline", "Penne da Evidenziatore", "Agenda", "Calcolatrice", "Collo",
    ],
    ("Non alimentare", "Articoli per animali"): [
        "Croccantini Cane", "Croccantini Gatto", "Sabbietta Gatto", "Giochi per Cane", "Collare",
        "Guinzaglio", "Ciotola", "Lettiera", "Snack per Cane", "Shampoo per Animali",
        "Spazzola per Animali", "Bocconcini in Conserve",
    ],
    ("Non alimentare", "Bambini e neonati"): [
        "Pannolini", "Salviette Bimbo", "Schiuma Bagno Bimbo", "Shampoo Bimbo", "Crema Emolliente Bimbo",
        "Latte in Polvere", "Biberon", "Ciuccio", "Poppatoio", "Asciugamano Bimbo",
        "Detersivo Bimbo", "Mascherina Bimbo",
    ],
}

# ---------------------------------------------------------------------------
# Modificatori per (L1, L2) — il primo elemento "" genera le basi nude
# ---------------------------------------------------------------------------
MODIFIERS: dict[tuple[str, str], list[str]] = {
    ("Ortofrutta", "Frutta"): ["", "Bio", "in confezione da 500 g", "in confezione da 1 kg", "Selezione", "Mini"],
    ("Ortofrutta", "Verdura"): ["", "Bio", "in confezione da 500 g", "in confezione da 1 kg", "Selezione", "Baby"],
    ("Ortofrutta", "Erbe e contorni"): ["", "Bio", "in vaso", "in bustina", "Selezione", "Misto"],
    ("Panetteria", "Pane"): ["", "Bio", "in confezione da 2", "in confezione da 4", "Senza Glutine", "Integrale"],
    ("Panetteria", "Panificati"): ["", "Bio", "in confezione da 4", "in confezione da 6", "Farcito", "Classico"],
    ("Panetteria", "Dolci da forno"): ["", "Bio", "in confezione da 4", "in confezione da 8", "Senza Glutine", "Classico"],
    ("Carne", "Carne bovina"): ["", "Bio", "in confezione da 400 g", "in confezione da 1 kg", "in Meno Grasso", "Selezione"],
    ("Carne", "Carne suina"): ["", "Bio", "in confezione da 400 g", "in confezione da 1 kg", "Affumicato", "Piccante"],
    ("Carne", "Pollame"): ["", "Bio", "in confezione da 400 g", "in confezione da 1 kg", "alla Griglia", "in Meno Grasso"],
    ("Carne", "Salumi freschi"): ["", "in confezione da 400 g", "in confezione da 1 kg", "Piccante", "Classico", "Senza Glutine"],
    ("Pesce", "Pesce fresco"): ["", "in confezione da 300 g", "in confezione da 500 g", "Surgelato", "in Filetti", "in Tranci"],
    ("Pesce", "Pesce confezionato"): ["", "in lattina da 160 g", "in lattina da 250 g", "Bio", "in Olio EVO", "al Naturale"],
    ("Pesce", "Crostacei e molluschi"): ["", "Surgelato", "in confezione da 300 g", "in Salamoia", "Classico", "Surgelati IQF"],
    ("Bevande", "Acqua e minerali"): ["", "da 500 ml", "da 1 L", "da 1,5 L", "Pack da 6", "Pack da 12"],
    ("Bevande", "Succhi e nettari"): ["", "da 1 L", "da 200 ml", "Pack da 6", "Senza Zucchero", "Bio"],
    ("Bevande", "Bibite"): ["", "da 1 L", "da 330 ml", "Pack da 6", "Pack da 12", "Zero Zuccheri"],
    ("Bevande", "Birra"): ["", "da 330 ml", "Pack da 6", "Pack da 12", "da 660 ml", "Artigianale"],
    ("Bevande", "Vino"): ["", "da 1 L", "da 750 ml", "Riserva", "Doc", "Bio"],
    ("Banco Frigo", "Latte e yogurt"): ["", "in confezione da 6", "in confezione da 12", "Bio", "da 500 g", "Senza Lattosio"],
    ("Banco Frigo", "Formaggi"): ["", "in confezione da 200 g", "in confezione da 400 g", "Bio", "a Pasta Dura", "Stagionato"],
    ("Banco Frigo", "Burro e panna"): ["", "in confezione da 250 g", "in confezione da 500 g", "Bio", "in Porzioni", "da 1 kg"],
    ("Banco Frigo", "Surgelati"): ["", "in confezione da 400 g", "in confezione da 1 kg", "Bio", "in Porzioni", "Family Pack"],
    ("Banco Frigo", "Uova"): ["", "con 6 uova", "con 10 uova", "con 15 uova", "con 20 uova", "con 30 uova", "Bio", "in Confezione da 15"],
    ("Dispensa", "Pasta e riso"): ["", "in confezione da 500 g", "in confezione da 1 kg", "Bio", "Integrale", "Senza Glutine"],
    ("Dispensa", "Oli e condimenti"): ["", "da 500 ml", "da 1 L", "Bio", "in Confezione da 2", "Extra"],
    ("Dispensa", "Conserve"): ["", "in lattina da 400 g", "in barattolo da 500 g", "Bio", "in Confezione da 2", "in Vaso"],
    ("Dispensa", "Snack e cracker"): ["", "in confezione da 200 g", "in confezione da 400 g", "Bio", "in Porzioni", "Senza Glutine"],
    ("Dispensa", "Dolci e cioccolato"): ["", "in confezione da 100 g", "in confezione da 200 g", "Bio", "Senza Zucchero", "in Porzioni"],
    ("Dispensa", "Caffè e tè"): ["", "in confezione da 250 g", "in confezione da 500 g", "Bio", "Decaffeinato", "in Bustine"],
    ("Dispensa", "Legumi e farine"): ["", "in confezione da 1 kg", "in confezione da 500 g", "Bio", "in Lattina", "in Sacchetto"],
    ("Dispensa", "Salse e pesti"): ["", "in barattolo da 200 g", "in confezione da 500 ml", "Bio", "in Porzioni", "Piccante"],
    ("Non alimentare", "Igiene persona"): ["", "da 500 ml", "da 250 ml", "Pack da 3", "Extra", "Eco"],
    ("Non alimentare", "Pulizia e lavatrice"): ["", "da 1 L", "da 2,5 L", "da 5 L", "Concentrato", "Eco", "in Polvere"],
    ("Non alimentare", "Cura della casa"): ["", "Pack da 3", "Pack da 6", "da 10 pezzi", "Ricaricabile", "Eco", "da 20 pezzi"],
    ("Non alimentare", "Cartoleria"): ["", "Pack da 3", "Pack da 10", "da 12 pezzi", "Extra", "Scolastico", "Pack da 5"],
    ("Non alimentare", "Articoli per animali"): ["", "in confezione da 1 kg", "in confezione da 3 kg", "Pack da 2", "Eco", "in confezione da 500 g", "Family Pack"],
    ("Non alimentare", "Bambini e neonati"): ["", "Pack da 2", "Pack da 4", "in confezione da 1 L", "Extra Delicato", "Pack da 6", "Eco"],
}

# Conteggio prodotti target per L1 (alimentari >= 1000, non alimentari >= 400)
L1_TARGET: dict[str, int] = {
    "Ortofrutta": 120,
    "Panetteria": 90,
    "Carne": 120,
    "Pesce": 70,
    "Bevande": 130,
    "Banco Frigo": 200,
    "Dispensa": 320,
    "Non alimentare": 420,
}

# ---------------------------------------------------------------------------
# DDL identica allo schema Room atteso
# ---------------------------------------------------------------------------
DDL = [
    "CREATE TABLE TAXONOMY_LEVEL_1 (id INTEGER NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE INDEX idx_taxonomy_l1_sort ON TAXONOMY_LEVEL_1(sortOrder)",
    "CREATE TABLE TAXONOMY_LEVEL_2 (id INTEGER NOT NULL, level1_id INTEGER NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE INDEX idx_taxonomy_l2_l1 ON TAXONOMY_LEVEL_2(level1_id)",
    "CREATE INDEX idx_taxonomy_l2_sort ON TAXONOMY_LEVEL_2(sortOrder)",
    "CREATE TABLE TAXONOMY_LEVEL_3 (id INTEGER NOT NULL, level2_id INTEGER NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL, is_user_defined INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE INDEX idx_taxonomy_l3_l2 ON TAXONOMY_LEVEL_3(level2_id)",
    "CREATE INDEX idx_taxonomy_l3_sort ON TAXONOMY_LEVEL_3(sortOrder)",
    "CREATE INDEX idx_taxonomy_l3_user_defined ON TAXONOMY_LEVEL_3(is_user_defined)",
    "CREATE TABLE CANONICAL_PRODUCT (id INTEGER NOT NULL, level3_id INTEGER NOT NULL, name TEXT NOT NULL, is_user_defined INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE INDEX idx_canonical_product_l3 ON CANONICAL_PRODUCT(level3_id)",
    "CREATE INDEX idx_canonical_product_name ON CANONICAL_PRODUCT(name)",
    "CREATE INDEX idx_canonical_product_user_defined ON CANONICAL_PRODUCT(is_user_defined)",
    "CREATE TABLE SYNONYM (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, product_id INTEGER NOT NULL, term TEXT NOT NULL)",
    "CREATE INDEX idx_synonym_product_id ON SYNONYM(product_id)",
    "CREATE INDEX idx_synonym_term ON SYNONYM(term)",
    "CREATE TABLE SHOPPING_ITEM (id TEXT NOT NULL, productId INTEGER NOT NULL, quantity REAL NOT NULL, unit TEXT NOT NULL, brand TEXT, variant TEXT, condition TEXT, isChecked INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE INDEX idx_shopping_item_product_id ON SHOPPING_ITEM(productId)",
    "CREATE INDEX idx_shopping_item_checked ON SHOPPING_ITEM(isChecked)",
    "CREATE TABLE HISTORICAL_ITEM (id TEXT NOT NULL, productId INTEGER NOT NULL, lastQuantity REAL NOT NULL, lastUnit TEXT NOT NULL, purchasedAt INTEGER NOT NULL, purchaseCount INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE INDEX idx_historical_item_product_id ON HISTORICAL_ITEM(productId)",
    "CREATE INDEX idx_historical_item_purchased_at ON HISTORICAL_ITEM(purchasedAt)",
    "CREATE TABLE LEARNED_RULE (id TEXT NOT NULL, userExpression TEXT NOT NULL, productId INTEGER NOT NULL, level3Id INTEGER NOT NULL, lastAppliedAt INTEGER NOT NULL, isActive INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE UNIQUE INDEX idx_learned_rule_expression ON LEARNED_RULE(userExpression)",
    "CREATE INDEX idx_learned_rule_product_id ON LEARNED_RULE(productId)",
    "CREATE INDEX idx_learned_rule_active ON LEARNED_RULE(isActive)",
]

EXPECTED_TABLES = {
    "TAXONOMY_LEVEL_1", "TAXONOMY_LEVEL_2", "TAXONOMY_LEVEL_3",
    "CANONICAL_PRODUCT", "SYNONYM",
    "SHOPPING_ITEM", "HISTORICAL_ITEM", "LEARNED_RULE",
}

# Schema atteso come generato da Room (nome colonna, tipo, in NOT NULL, PK).
# PK "primal rowid alias" (SYNONYM.id) e' nullable per SQLite ma Room la tratta
# come NOT NULL: la verifica tiene conto solo di (nome, tipo) per le righe PK.
EXPECTED_COLUMNS: dict[str, list[tuple[str, str, bool, bool]]] = {
    "TAXONOMY_LEVEL_1": [
        ("id", "INTEGER", False, True), ("name", "TEXT", True, False),
        ("sortOrder", "INTEGER", True, False),
    ],
    "TAXONOMY_LEVEL_2": [
        ("id", "INTEGER", False, True), ("level1_id", "INTEGER", True, False),
        ("name", "TEXT", True, False), ("sortOrder", "INTEGER", True, False),
    ],
    "TAXONOMY_LEVEL_3": [
        ("id", "INTEGER", False, True), ("level2_id", "INTEGER", True, False),
        ("name", "TEXT", True, False), ("sortOrder", "INTEGER", True, False),
        ("is_user_defined", "INTEGER", True, False),
    ],
    "CANONICAL_PRODUCT": [
        ("id", "INTEGER", False, True), ("level3_id", "INTEGER", True, False),
        ("name", "TEXT", True, False), ("is_user_defined", "INTEGER", True, False),
    ],
    "SYNONYM": [
        ("id", "INTEGER", True, True), ("product_id", "INTEGER", True, False),
        ("term", "TEXT", True, False),
    ],
    "SHOPPING_ITEM": [
        ("id", "TEXT", False, True), ("productId", "INTEGER", True, False),
        ("quantity", "REAL", True, False), ("unit", "TEXT", True, False),
        ("brand", "TEXT", False, False), ("variant", "TEXT", False, False),
        ("condition", "TEXT", False, False), ("isChecked", "INTEGER", True, False),
        ("createdAt", "INTEGER", True, False), ("updatedAt", "INTEGER", True, False),
    ],
    "HISTORICAL_ITEM": [
        ("id", "TEXT", False, True), ("productId", "INTEGER", True, False),
        ("lastQuantity", "REAL", True, False), ("lastUnit", "TEXT", True, False),
        ("purchasedAt", "INTEGER", True, False), ("purchaseCount", "INTEGER", True, False),
    ],
    "LEARNED_RULE": [
        ("id", "TEXT", False, True), ("userExpression", "TEXT", True, False),
        ("productId", "INTEGER", True, False), ("level3Id", "INTEGER", True, False),
        ("lastAppliedAt", "INTEGER", True, False), ("isActive", "INTEGER", True, False),
    ],
}

# (indice, colonne, unique)
EXPECTED_INDICES: dict[str, set[tuple[str, tuple[str, ...], bool]]] = {
    "TAXONOMY_LEVEL_1": {("idx_taxonomy_l1_sort", ("sortOrder",), False)},
    "TAXONOMY_LEVEL_2": {
        ("idx_taxonomy_l2_l1", ("level1_id",), False),
        ("idx_taxonomy_l2_sort", ("sortOrder",), False),
    },
    "TAXONOMY_LEVEL_3": {
        ("idx_taxonomy_l3_l2", ("level2_id",), False),
        ("idx_taxonomy_l3_sort", ("sortOrder",), False),
        ("idx_taxonomy_l3_user_defined", ("is_user_defined",), False),
    },
    "CANONICAL_PRODUCT": {
        ("idx_canonical_product_l3", ("level3_id",), False),
        ("idx_canonical_product_name", ("name",), False),
        ("idx_canonical_product_user_defined", ("is_user_defined",), False),
    },
    "SYNONYM": {
        ("idx_synonym_product_id", ("product_id",), False),
        ("idx_synonym_term", ("term",), False),
    },
    "SHOPPING_ITEM": {
        ("idx_shopping_item_product_id", ("productId",), False),
        ("idx_shopping_item_checked", ("isChecked",), False),
    },
    "HISTORICAL_ITEM": {
        ("idx_historical_item_product_id", ("productId",), False),
        ("idx_historical_item_purchased_at", ("purchasedAt",), False),
    },
    "LEARNED_RULE": {
        ("idx_learned_rule_expression", ("userExpression",), True),
        ("idx_learned_rule_product_id", ("productId",), False),
        ("idx_learned_rule_active", ("isActive",), False),
    },
}


def expand_names(l1: str, l2: str, target: int, rng: random.Random) -> list[str]:
    """Genera esattamente `target` nomi prodotto unici per la coppia (l1, l2)."""
    bases = list(BASES[(l1, l2)])
    mods = [m for m in MODIFIERS[(l1, l2)] if m]  # solo modificatori non vuoti

    capacity = len(bases) * (1 + len(mods))
    eff_pool = {*bases} | {f"{b} {m}" for b in bases for m in mods}
    if len(eff_pool) < target:
        raise RuntimeError(
            f"Capacita' effettiva {len(eff_pool)} (basi x modificatori con collissioni)"
            f" insufficiente per {l1}/{l2}: {target} richiesti"
        )
    if capacity < target:
        raise RuntimeError(
            f"Capacita' insufficiente per {l1}/{l2}: {capacity} < {target}"
        )

    names: list[str] = []
    seen: set[str] = set()

    def add(name: str) -> None:
        if name not in seen and len(names) < target:
            seen.add(name)
            names.append(name)

    # Prima passata: basi nude
    for b in bases:
        add(b)
    # Seconda passata: basi x modificatori in ordine deterministico
    order = list(bases)
    rng.shuffle(order)
    mi = 0
    guard = 0
    while len(names) < target:
        guard += 1
        if guard > (target + len(mods) + 2):
            raise RuntimeError(f"{l1}/{l2}: loop non converge")
        mod = mods[mi % len(mods)]
        mi += 1
        for b in order:
            if len(names) >= target:
                break
            add(f"{b} {mod}")
    if len(names) != target:
        raise RuntimeError(f"{l1}/{l2}: generati {len(names)} != {target}")
    return names


def verify_schema(conn: sqlite3.Connection) -> list[str]:
    """Verifica struttura (colonne + indici) contro lo schema atteso Room."""
    errors: list[str] = []

    for table, expected_cols in EXPECTED_COLUMNS.items():
        cur = conn.execute(f"PRAGMA table_info({table})")
        actual = cur.fetchall()  # (cid, name, type, notnull, dflt, pk)
        if not actual:
            errors.append(f"tabella assente: {table}")
            continue
        by_name = {r[1]: r for r in actual}
        for name, typ, not_null, is_pk in expected_cols:
            row = by_name.get(name)
            if row is None:
                errors.append(f"{table}: colonna mancante {name}")
                continue
            if row[2].upper() != typ:
                errors.append(f"{table}.{name}: tipo {row[2]} != {typ}")
            # row: (cid, name, type, notnull, dflt_value, pk)
            if not_null and row[3] == 0:
                errors.append(f"{table}.{name}: NOT NULL mancante")
            if is_pk and row[5] != 1:
                errors.append(f"{table}.{name}: PK posizione attesa 1, trovata {row[5]}")

    for table, expected_idx in EXPECTED_INDICES.items():
        cur = conn.execute(f"PRAGMA index_list({table})")
        actual_idx = {
            r[1]: {"unique": bool(r[2]), "cols": [], "origin": r[3]}
            for r in cur.fetchall()
        }
        for name, cols, unique in expected_idx:
            if name not in actual_idx:
                errors.append(f"{table}: indice mancante {name}")
                continue
            info = actual_idx[name]
            if info["unique"] != unique:
                errors.append(f"{table}: indice {name} unique={info['unique']} atteso {unique}")
            if info["origin"] == "pk":
                continue  # auto-indice su PK rowid: non fa parte delle entity Room
            cols_cur = conn.execute(f"PRAGMA index_info({name})")
            col_names = [r[2] for r in cols_cur.fetchall()]
            if tuple(col_names) != cols:
                errors.append(
                    f"{table}: indice {name} colonne {col_names} != {list(cols)}"
                )

    return errors


def main() -> int:
    rng = random.Random(SEED)

    ASSET_PATH.parent.mkdir(parents=True, exist_ok=True)
    if ASSET_PATH.exists():
        ASSET_PATH.unlink()

    conn = sqlite3.connect(str(ASSET_PATH))
    cur = conn.cursor()
    for stmt in DDL:
        cur.execute(stmt)

    # --- Tassonomia + prodotti ---
    l1_rows: list[tuple[int, str, int]] = []
    l2_id = 0
    l3_id = 0
    product_id = 0
    # (level3_id, name, l1_id) in ordine di inserimento
    products: list[tuple[int, str, int]] = []

    for l1_idx, (l1_name, l2s) in enumerate(TAXONOMY, start=1):
        l1_rows.append((l1_idx, l1_name, l1_idx))
        target_l1 = L1_TARGET[l1_name]
        n_l2 = len(l2s)
        base_share = target_l1 // n_l2
        remainder = target_l1 - base_share * n_l2

        for l2_idx, (l2_name, l3s) in enumerate(l2s, start=1):
            l2_id += 1
            cur.execute(
                "INSERT INTO TAXONOMY_LEVEL_2 (id, level1_id, name, sortOrder) VALUES (?,?,?,?)",
                (l2_id, l1_idx, l2_name, l2_idx),
            )
            first_l3_id = l3_id + 1
            for l3_idx, l3_name in enumerate(l3s, start=1):
                l3_id += 1
                cur.execute(
                    "INSERT INTO TAXONOMY_LEVEL_3 (id, level2_id, name, sortOrder, is_user_defined) VALUES (?,?,?,?,0)",
                    (l3_id, l2_id, l3_name, l3_idx),
                )

            # Ripartizione target sui L2: base + ripartizione del resto
            target = base_share + (1 if l2_idx <= remainder else 0)
            names = expand_names(l1_name, l2_name, target, rng)
            l3_ids = list(range(first_l3_id, l3_id + 1))

            for i, name in enumerate(names):
                product_id += 1
                assigned_l3 = l3_ids[i % len(l3_ids)]
                cur.execute(
                    "INSERT INTO CANONICAL_PRODUCT (id, level3_id, name, is_user_defined) VALUES (?,?,?,0)",
                    (product_id, assigned_l3, name),
                )
                products.append((assigned_l3, name, l1_idx))

    cur.executemany(
        "INSERT INTO TAXONOMY_LEVEL_1 (id, name, sortOrder) VALUES (?,?,?)", l1_rows
    )

    # --- Sinonimi (dedup per termine, primo product_id wins) ---
    # Ogni prodotto ottiene 1-4 termini: lowercase, forma accent-folded
    # (es. "caffe" per "Caffè"), base senza modificatore (lowercase e folded).
    def fold(text: str) -> str:
        nfkd = unicodedata.normalize("NFKD", text)
        return "".join(c for c in nfkd if not unicodedata.combining(c))

    term_to_pid: dict[str, int] = {}
    pid = 0
    for _, name, _ in products:
        pid += 1
        lower = name.lower()
        folded = fold(lower)
        terms = [lower] if folded == lower else [lower, folded]
        parts = name.split(" ")
        if len(parts) > 2:
            base_term = " ".join(parts[:2]).lower()
            base_folded = fold(base_term)
            if base_term != lower and base_term not in terms:
                terms.append(base_term)
            if base_folded not in terms:
                terms.append(base_folded)
        for t in terms:
            if t not in term_to_pid:
                term_to_pid[t] = pid
    cur.executemany(
        "INSERT INTO SYNONYM (product_id, term) VALUES (?,?)",
        sorted(
            ((pid, term) for term, pid in term_to_pid.items()),
            key=lambda kv: (kv[0], kv[1]),
        ),
    )

    cur.execute("PRAGMA user_version = 1")
    conn.commit()

    # --- Verifica AC ---
    cur.execute(
        """SELECT COUNT(*) FROM CANONICAL_PRODUCT cp
           JOIN TAXONOMY_LEVEL_3 t3 ON cp.level3_id = t3.id
           JOIN TAXONOMY_LEVEL_2 t2 ON t3.level2_id = t2.id
           JOIN TAXONOMY_LEVEL_1 t1 ON t2.level1_id = t1.id
           WHERE t1.name != ?""",
        (NON_FOOD_L1,),
    )
    food = cur.fetchone()[0]

    cur.execute(
        """SELECT COUNT(*) FROM CANONICAL_PRODUCT cp
           JOIN TAXONOMY_LEVEL_3 t3 ON cp.level3_id = t3.id
           JOIN TAXONOMY_LEVEL_2 t2 ON t3.level2_id = t2.id
           JOIN TAXONOMY_LEVEL_1 t1 ON t2.level1_id = t1.id
           WHERE t1.name = ?""",
        (NON_FOOD_L1,),
    )
    non_food = cur.fetchone()[0]

    cur.execute(
        """SELECT COUNT(*) FROM CANONICAL_PRODUCT
           WHERE level3_id IS NULL OR TRIM(name) = '' OR is_user_defined != 0"""
    )
    invalid = cur.fetchone()[0]

    cur.execute(
        """SELECT name, COUNT(*) FROM CANONICAL_PRODUCT
           GROUP BY name HAVING COUNT(*) > 1"""
    )
    duplicates = cur.fetchall()

    cur.execute(
        """SELECT COUNT(*) FROM SYNONYM s
           LEFT JOIN CANONICAL_PRODUCT p ON p.id = s.product_id
           WHERE p.id IS NULL"""
    )
    orphan_synonyms = cur.fetchone()[0]

    cur.execute(
        """SELECT COUNT(*) FROM CANONICAL_PRODUCT p
           WHERE NOT EXISTS (SELECT 1 FROM SYNONYM s WHERE s.product_id = p.id)"""
    )
    no_synonyms = cur.fetchone()[0]

    cur.execute("SELECT COUNT(*) FROM CANONICAL_PRODUCT")
    total = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM SYNONYM")
    synonyms = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM TAXONOMY_LEVEL_1")
    n_l1 = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM TAXONOMY_LEVEL_3")
    n_l3 = cur.fetchone()[0]
    cur.execute("PRAGMA user_version")
    uv = cur.fetchone()[0]
    cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables = {r[0] for r in cur.fetchall()}
    missing_tables = EXPECTED_TABLES - tables

    schema_errors = verify_schema(conn)

    # Ordine L1 come da AC Story 2.1
    cur.execute("SELECT name FROM TAXONOMY_LEVEL_1 ORDER BY sortOrder")
    l1_order = [r[0] for r in cur.fetchall()]
    expected_order = [name for name, _ in TAXONOMY]
    order_ok = l1_order == expected_order

    conn.close()

    size_kb = ASSET_PATH.stat().st_size / 1024
    print(f"DB: {ASSET_PATH} ({size_kb:.1f} KB)")
    print(f"  tassonomia      : {n_l1} L1, {l2_id} L2, {n_l3} L3")
    print(f"  prodotti totali : {total}")
    print(f"  alimentari      : {food} (min {MIN_FOOD})")
    print(f"  non alimentari  : {non_food} (min {MIN_NON_FOOD})")
    print(f"  sinonimi        : {synonyms}")
    print(f"  righe invalide  : {invalid} (atteso 0)")
    print(f"  nomi duplicati  : {len(duplicates)} (atteso 0)")
    print(f"  sinonimi orfani : {orphan_synonyms} (atteso 0)")
    print(f"  prodotti senza sinonimo: {no_synonyms} (atteso 0)")
    print(f"  user_version    : {uv} (atteso 1)")
    print(f"  ordine L1       : {'OK' if order_ok else 'ERRATO'} -> {l1_order}")
    print(f"  tabelle mancanti: {sorted(missing_tables) if missing_tables else 'nessuna'}")
    if schema_errors:
        print("  ERRORI SCHEMA   :")
        for err in schema_errors:
            print(f"    - {err}")
    else:
        print("  schema colonne/indici: OK (match Room)")

    ok = (
        food >= MIN_FOOD
        and non_food >= MIN_NON_FOOD
        and invalid == 0
        and not duplicates
        and orphan_synonyms == 0
        and no_synonyms == 0
        and uv == 1
        and not missing_tables
        and order_ok
        and not schema_errors
    )
    if not ok:
        print("FALLITO: soglie AC non raggiunte.")
        return 1
    print("OK: tutti i vincoli AC soddisfatti.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
