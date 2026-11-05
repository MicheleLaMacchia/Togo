---
name: Togo
status: final
sources:
  - _bmad-output/planning-artifacts/prds/prd-Togo-2026-09-20/prd.md
  - _bmad-output/planning-artifacts/briefs/brief-Togo-2026-09-20/brief.md
  - _bmad-output/specs/spec-cose-da-fare/SPEC.md
  - _bmad-output/specs/spec-cose-da-fare/brownfield.md
created: 2026-10-07
updated: 2026-11-05
---

# TOGO — Experience Spine

> Riferimento comportamentale ufficiale per l'applicazione Android TOGO. Definisce l'architettura informativa, le interazioni, gli stati, il microcopy e i percorsi utente.  
> Paired with `DESIGN.md` (High-Contrast Utility visual identity). I token visivi sono referenziati tramite sintassi `{path.to.token}`.  
> Spines win on conflict: questo documento e `DESIGN.md` vincono su qualunque mockup o wireframe.  
> Mockup di riferimento per la composizione: [`mockups/active-list.html`](mockups/active-list.html), [`mockups/todo-page.html`](mockups/todo-page.html), [`mockups/action-sheet.html`](mockups/action-sheet.html), [`mockups/share-dialog.html`](mockups/share-dialog.html), [`mockups/conclude-dialog.html`](mockups/conclude-dialog.html), [`mockups/voice-sheet.html`](mockups/voice-sheet.html), [`mockups/duplicate-dialog.html`](mockups/duplicate-dialog.html), [`mockups/history.html`](mockups/history.html).

---

## 1. Foundation

- **Piattaforma e Form-factor:** Mobile smartphone Android (riferimento primario Android 16, supporto base API 21+). Utilizzo esclusivo in orientamento verticale (portrait) ottimizzato per l'interazione ad una sola mano.
- **UI Framework:** Android Nativo con **Jetpack Compose** e Material 3 come fondazione dei controlli di sistema.
- **Superficie principale:** un pager orizzontale a due pagine dentro la stessa schermata. **Spesa** a sinistra (pagina iniziale), **Da fare** a destra. Nessuna bottom tab bar, nessun drawer: il pager e le frecce dell'intestazione sono l'unico passaggio tra le due liste.
- **Architettura dati:** **Local-first** al 100%. Persistenza su database SQLite/Room locale. Funzionamento completo offline per tutte le funzioni di gestione lista, catalogo prodotti, storico, regole apprese e cose da fare.
- **Servizio vocale:** Integrato tramite `RecognizerIntent` di Android (`it-IT`). La non disponibilità momentanea del servizio vocale non pregiudica alcuna funzionalità manuale dell'app.
- **Riferimento Visivo:** Tutte le decisioni estetiche, metriche e cromatiche derivano da `DESIGN.md`.

### Glossario termini Cose da fare
| Termine | Valori | Default | Ordine | Token badge |
|---|---|---|---|---|
| Azione | testo libero (nessun prodotto, marca, variante, quantità o unità) | — | alfabetico dentro ambito+urgenza | — |
| Ambito | Casa, Pratiche, Salute, Lavoro, Altro | Altro | Casa → Pratiche → Salute → Lavoro → Altro | area-badge |
| Urgenza | Urgente, A tempo perso, Eccezionale | A tempo perso | Urgente → A tempo perso → Eccezionale | urgency-badge |
| Fatte | sezione azioni spuntate (non compare se N = 0) | — | dalla più recente in cima | — |
| Lista della spesa | pagina Spesa del pager (prodotti non spuntati) | — | per corsia/categoria | item-row |
| Lista attiva | pagina corrente del pager (Spesa o Da fare) | — | — | page-header |

---

## 2. Information Architecture

### 2.1 Mappa delle Superfici

| Superficie | Raggiunta da | Scopo e Responsabilità |
|---|---|---|
| **Spesa (Lista Attiva)** | Apertura app (cold start) o freccia sinistra da Da fare | Pagina 1 del pager: prodotti da comprare raggruppati per corsia/categoria, sezione collassabile "Presi (N)", azione "Concludi spesa", trigger vocale e manuale. |
| **Da fare** | Swipe sull'intestazione o freccia destra da Spesa | Pagina 2 del pager: azioni in testo libero raggruppate per ambito (header di ambito), poi ordinate per urgenza e infine alfabetiche, sezione collassabile "Fatte (N)", azione "Concludi attività". Nessun vocale. |
| **Intestazione di Pagina (Pager)** | Sempre visibile sopra la lista | Titolo "Spesa (N)" o "Da fare (N)", frecce di pagina, Storico (solo Spesa), Condividi e Impostazioni. È la principale zona di swipe orizzontale. |
| **Cattura Vocale (Bottom Sheet)** | Tap su pulsante Microfono (FAB) in Spesa | Ascolto live, trascrizione vocale in tempo reale, scomposizione strutturata (prodotto, quantità, unità, categoria) e conferma esplicita con un tocco. Non aggiunge azioni. |
| **Aggiunta / Modifica Prodotto** | Tap su "+" in Spesa o tap su riga prodotto | Form compatto con autocompletamento catalogo (1400+ prodotti), selettore quantità/unità e attributi opzionali contestuali. Invariato. |
| **Aggiunta / Modifica Azione** | Tap su "+" in Da fare o tap su riga azione | Form azione con testo a una riga, urgenza a tre valori e ambito a cinque valori. Default A tempo perso e Altro. |
| **Risoluzione Duplicati (Modal)** | Inserimento vocale o manuale di prodotto già presente | Dialogo modale con 3 scelte esplicite: Somma quantità (raccomandata), Sostituisci quantità, Crea voce separata. Invariato. |
| **Condivisione (Scelta)** | Tap su icona Condividi in Spesa o Da fare | Dialogo a tre scelte con conteggi: Solo spesa, Solo azioni, Entrambe. Poi share sheet Android. Le azioni spuntate sono escluse. |
| **Conferma Conclusione Attività (Modal)** | Tap su "Concludi attività" nella sezione Fatte | Conferma esplicita con conteggio; elimina solo le azioni spuntate. Nessun annulla e nessuno storico. |
| **Storico Prodotti** | Tap su icona Storico (solo Spesa) | Consultazione degli articoli acquistati nelle spese passate con ricerca rapida e pulsante "+ Riaggiungi" a un tocco nella lista attiva. Invariato. |
| **Gestione Regole Apprese** | Tap su icona Impostazioni | Visualizzazione, modifica ed eliminazione delle associazioni personalizzate prodotto-categoria apprese dall'utente. Invariata. |
| **Importa Lista** | Da Impostazioni | Import dei prodotti invariato; riconosce il blocco `[DA FARE]` e aggiunge azioni alla lista esistente senza duplicati. |

### 2.2 Principi di Navigazione
- **Pager a due pagine:** L'app non utilizza bottom tab bar o navigation drawer. La schermata principale ospita due pagine orizzontali: Spesa (sinistra, cold start) e Da fare (destra). Il pager massimizza lo spazio verticale e non introduce una terza destinazione.
- **Intestazione come controllo e zona gesto:** Il titolo della pagina e le frecce stanno in una fascia fissa sopra la lista. Lo swipe orizzontale sull'intestazione, sulle intestazioni di gruppo o sugli spazi non occupati da righe cambia pagina. Lo swipe orizzontale su una riga resta il gesto del cestino.
- **Accesso Funzioni Secondarie:** Storico è visibile solo sulla pagina Spesa; Condivisione e Impostazioni sono icone fisse su entrambe le pagine. Il FAB (+) è contestuale: prodotto su Spesa, azione su Da fare. Il microfono resta solo su Spesa.
- **Back e ripristino:** Il back dalla pagina Da fare riporta alla Spesa. L'app parte sempre da Spesa a freddo; la pagina corrente e la posizione di scorrimento sopravvivono a rotazione e navigazione interna.
- **Profondità Modale Massima:** Nessun flusso modale o bottom sheet si sovrappone a più di 1 livello.

---

## 3. Voice and Tone (Microcopy)

TOGO parla in italiano naturale, sobrio, diretto e affidabile. Rifiuta termini gergali, frasi prolisse o notifiche entusiaste non richieste.

### Regole di Microcopy
1. **Zero mutazioni silenziose:** Il sistema non applica mai modifiche o unificazioni senza esplicito consenso.
2. **Conferme brevi e contestuali:**
   - Conferma vocale: *"Aggiunto: 2 l di Latte parzialmente scremato in Banco Frigo"*
   - Prompt apprendimento: *"Ricordo questa scelta per i prossimi inserimenti?"* `[Ricorda]` / `[Solo per ora]`
   - Dialogo duplicati: *"«Latte parzialmente scremato» è già presente con 1 l nella categoria Banco Frigo. Cosa vuoi fare?"* `[Somma quantità (+1 l → 2 l)]` / `[Sostituisci quantità (1 l)]` / `[Crea voce separata]`
   - Conclusione spesa: *"Concludere la spesa? I 6 articoli presi saranno spostati nello storico."* `[Concludi spesa]` / `[Annulla]`
3. **Stato vuoto accogliente e pratico:**
   - Lista attiva vuota: *"La tua lista è vuota. Tocca il microfono o premi + per aggiungere prodotti."*
   - Storico vuoto: *"Nessun prodotto nello storico. I prodotti acquistati compariranno qui."*

### Microcopy per Cose da fare
- **Pagina vuota:** *"Nessuna cosa da fare. Tocca + per aggiungere un'azione."*
- **Aggiunta:** titolo *"Aggiungi cosa da fare"*, placeholder *"Es. Chiamare il dentista"*, primario *"Aggiungi"*.
- **Modifica:** titolo *"Modifica cosa da fare"*, primario *"Salva"*.
- **Testo vuoto:** il primario resta disabilitato; nessun errore aggressivo.
- **Duplicato:** *"Azione già presente."* Il foglio resta aperto e i valori scelti non si perdono.
- **Conclusione:** *"Concludere le attività? Verranno eliminate N attività spuntate. L'operazione non si può annullare."* `[Concludi attività]` / `[Annulla]`.
- **Sezione vuota:** *"Nessuna attività conclusa."* La sezione Fatte non compare se N = 0.
- **Condivisione:** titolo *"Cosa vuoi condividere?"*; opzioni *"Solo spesa (N)"*, *"Solo azioni (M)"*, *"Entrambe (N+M)"*; primario *"Condividi"*.
- **Niente da condividere:** *"Non c'è nulla da condividere."*
- **Import con azioni:** *"Verranno importati N prodotti e M azioni. K duplicati saranno saltati."* Se una parte è zero, la frase si adatta.
- **Urgenza non valida nell'import:** *"Urgenza non valida: usa Urgente, A tempo perso o Eccezionale."*

### Inventario copy (fonte unica)
Le stringhe di seguito sono l'unica fonte di verità per il microcopy di interfaccia; le altre sezioni (§4, §5, §8, `DESIGN.md`) le richiamano senza riscriverle.
| Contesto | Stringa | Uso |
|---|---|---|
| Pagina Da fare vuota | "Nessuna cosa da fare. Tocca + per aggiungere un'azione." | §4.8 empty |
| Aggiunta azione — titolo | "Aggiungi cosa da fare" | §4.9 |
| Aggiunta azione — placeholder | "Es. Chiamare il dentista" | §4.9 |
| Aggiunta azione — primario | "Aggiungi" | §4.9 |
| Modifica azione — titolo | "Modifica cosa da fare" | §4.9 |
| Modifica azione — primario | "Salva" | §4.9 |
| Duplicato azione | "Azione già presente." | §4.9 |
| Conclusione attività | "Concludere le attività? Verranno eliminate N attività spuntate. L'operazione non si può annullare." | §4.8, §7.10 |
| Sezione Fatte vuota | "Nessuna attività conclusa." | §4.8 (non raggiunta se N = 0) |
| Condivisione — titolo | "Cosa vuoi condividere?" | §4.10 |
| Condivisione — opzioni | "Solo spesa (N)", "Solo azioni (M)", "Entrambe (N+M)" | §4.10 |
| Condivisione — primario | "Condividi" | §4.10 |
| Niente da condividere | "Non c'è nulla da condividere." | §4.10 |
| Import con azioni | "Verranno importati N prodotti e M azioni. K duplicati saranno saltati." | §4.11 |
| Urgenza non valida | "Urgenza non valida: usa Urgente, A tempo perso o Eccezionale." | §4.11 |
| Concludi spesa | "Concludere la spesa? I N articoli presi saranno spostati nello storico." | §4.3 |
| Primario concludi spesa | "Concludi spesa" | §4.3 |
| Primario conclude attività | "Concludi attività" | §4.8, §7.10 |

---

## 4. Component Patterns

### 4.1 Riga Prodotto (`item-row`)
- **Comportamento di Spunta (Check-off):**
  - Tap sulla checkbox quadrata (`{components.checkbox-utility}`):
    - Genera **haptic feedback** (vibrazione secca da 15ms).
    - La riga passa immediatamente allo stato "Preso" (testo barrato, opacità ridotta, checkbox riempita).
    - Dopo un micro-ritardo percepibile di 300ms per chiarezza visiva, la riga scivola fluidamente in fondo alla lista nella sezione collassabile *"Presi (N)"*. Se il focus è sulla riga o sulla sua checkbox, il focus non si sposta: resta sul nodo attivo oppure viene spostato sull'header della sezione con annuncio "Spostata in Presi, N articoli".
- **Riapertura / Deselezione:**
  - Nella sezione *"Presi"*, toccare nuovamente la checkbox ripristina istantaneamente il prodotto nella sua categoria d'origine nella lista attiva.
- **Modifica rapida:** Tap sul testo del prodotto apre il foglio di modifica manuale. Swipe verso sinistra rivela il pulsante rapido "Elimina".

### 4.2 Sezione Collassabile "Presi (N)"
- Posizionata stabilmente dopo l'ultima categoria di prodotti attivi.
- Mostra il contatore totale degli articoli spuntati nella sessione.
- Default: Espansa se N > 0 ("di recente" = almeno un articolo spuntato dall'ultima Concludi spesa), con chevron per collassarla se si desidera compattare la vista.
- In fondo alla sezione compare il pulsante sticky `{colors.accent-action}` **"Concludi spesa"**.

### 4.3 Flusso "Concludi Spesa"
- Pressione del pulsante "Concludi spesa":
  - Mostra un dialog di conferma con titolo `Concludere la spesa?`, corpo `I N articoli presi saranno spostati nello storico.` e due azioni: `Concludi spesa` (primario, stile distruttivo) e `Annulla` (secondario).
  - Dopo la conferma, sposta tutti gli articoli barrati nello **Storico permanente**.
  - Rimuove definitivamente gli articoli barrati dalla schermata attiva.
  - **Tutti i prodotti non spuntati rimangono intatti nella lista attiva** per la spesa successiva.
  - La snackbar è usata solo come feedback post-azione, mai come conferma.

### 4.4 Bottom Sheet Vocale (`voice-sheet`)
- **Apertura:** Tap sul FAB microfono (`{components.voice-fab}`).
- **Ascolto:** Visualizza animazione d'onda blu `{colors.accent-highlight}` e trascrive il testo riconosciuto in tempo reale.
- **Riconoscimento completato:**
  - Mostra la scomposizione strutturata: Nome canonico, Quantità normalizzata, Unità e Categoria proposta.
  - Permette di modificare la categoria con un tap rapido sull'icona matita prima del salvataggio.
  - Pulsante primario nero `{colors.accent-action}`: **"Conferma e aggiungi"**.
  - Pressione del pulsante genera **haptic feedback** e chiude il foglio, inserendo la voce nella categoria appropriata.

### 4.5 Risoluzione Duplicati (`duplicate-modal`)
- Attivato quando il prodotto riconosciuto a voce o inserito a mano è già presente nella lista attiva con la stessa categoria.
- **Azione raccomandata (default a vista):** `[Somma quantità]` (somma se le unità sono compatibili, es. 1 l + 1 l = 2 l).
- **Opzioni secondarie:** `[Sostituisci quantità]` e `[Crea voce separata]`.
- Unità compatibili: l, ml (1 l = 1000 ml), kg, g, pz. Se le unità sono incompatibili (es. 1 l vs 500 g), l'opzione "Somma" viene disabilitata con nota esplicativa.

### 4.6 Intestazione di Pagina e Pager
- **Struttura:** fascia fissa sopra la lista. A sinistra Storico (solo Spesa); al centro freccia sinistra, titolo `Spesa (N)` o `Da fare (N)`, freccia destra; a destra Condividi e Impostazioni.
- **Conteggi:** N nella pagina Spesa è il numero di prodotti non spuntati; N in Da fare è il numero di azioni non spuntate. La sezione Fatte ha un conteggio separato.
- **Cambio pagina:** swipe orizzontale sull'intestazione, sulle intestazioni di gruppo o su uno spazio non occupato da righe; oppure tap sulla freccia attiva. La freccia senza pagina disponibile è disabilitata e annunciata come tale, ma resta focusabile con `stateDescription="Non disponibile"` e nessuna azione attivabile. Animazione breve (240 ms), senza rimbalzi.
- **Gesto delle righe:** lo swipe orizzontale su una riga non cambia pagina: apre il cestino come nella Spesa. Il pager non intercetta il drag iniziato su una riga.
- **Stato della pagina:** la pagina corrente e la posizione di scorrimento di ciascuna lista sopravvivono a rotazione e navigazione interna. A freddo si parte da Spesa.
- **Back:** dalla pagina Da fare il back torna a Spesa; da Spesa segue il comportamento di sistema.
- **Controlli contestuali:** il FAB (+) apre il foglio prodotto su Spesa e il foglio azione su Da fare. Il microfono e Storico restano solo su Spesa. Condividi e Impostazioni restano su entrambe.
- **TalkBack:** il titolo è un heading; le frecce hanno etichette esplicite; al cambio pagina viene annunciato il nuovo titolo e il conteggio.
- **Ritorno focus:** dopo la chiusura di un dialogo o foglio, il focus torna al controllo che ha aperto il modale; se quel controllo non esiste più (es. sezione Fatte dopo conclusione), torna al titolo di pagina o al FAB. Al ritorno dallo Sharesheet il focus torna sull'icona Condividi.

### 4.7 Riga Azione (`action-row`)
- **Struttura:** checkbox a sinistra, testo al centro, nessuna quantità. Sotto il testo, badge urgenza e badge ambito. Accento verticale a sinistra con il colore dell'urgenza.
- **Stato attivo:** tap sulla riga apre il foglio di modifica; tap sulla checkbox spunta; swipe a sinistra rivela il cestino. La checkbox usa lo stesso haptic feedback da 15 ms della riga prodotto.
- **Spunta:** dopo 300 ms la riga passa nella sezione Fatte, resta barrata e mantiene urgenza e ambito. La spunta non elimina l'azione. Se il focus è sulla riga o sulla sua checkbox, il focus non si sposta: resta sul nodo attivo oppure viene spostato sull'header della sezione Fatte con annuncio "Spostata in Fatte, N azioni".
- **Deselezione:** nella sezione Fatte, la checkbox riporta l'azione nel gruppo corretto (ambito → urgenza → alfabetico).
- **Modifica:** cambia testo, urgenza e ambito. Se il cambio di gruppo è necessario, la riga si sposta nel nuovo gruppo. Un duplicato viene bloccato con `Azione già presente`.
- **Eliminazione:** il cestino elimina solo quella azione e mostra la snackbar con `Annulla`, come per la spesa. La snackbar ha durata ≥10 s (o resta aperta fino all'azione successiva), mette in pausa su focus/hover e annuncia via live region educata "Azione eliminata. Annulla disponibile" con azione Annulla ≥48dp. La riga espone inoltre un'azione accessibile `Elimina` (custom action TalkBack / menu contestuale) con la stessa snackbar di `Annulla`: lo swipe non è l'unico percorso. L'undo non è disponibile dopo `Concludi attività`.
- **Divieti:** nessuna quantità, unità, marca, variante, categoria di catalogo o badge prodotto.

### 4.8 Sezione Fatte (`fatte-section`)
- **Posizione:** in fondo alla pagina Da fare, dopo l'ultimo gruppo attivo.
- **Header:** `Fatte (N)` collassabile, default espansa se N > 0, chevron come `Presi`.
- **Righe:** checkbox spuntata, testo barrato, badge attenuati. Nessun tap-edit e nessun cestino. La checkbox deseleziona e riporta l'azione nel gruppo.
- **Ordine:** dalla più recente in cima, così l'ultima attività conclusa è subito visibile.
- **Concludi attività:** pulsante largo in fondo alla sezione, visibile solo se N > 0. Apre il dialogo di conferma.
- **Dopo la conferma:** le azioni spuntate vengono eliminate, le non spuntate restano, la lista della spesa e lo storico acquisti non cambiano. Nessun annulla e nessuno storico delle azioni.
- **Sezione vuota:** se N = 0 la sezione non compare.

### 4.9 Aggiunta e Modifica Azione (`action-sheet`)
- **Apertura:** FAB (+) sulla pagina Da fare, oppure tap su una riga attiva.
- **Campi:** testo a una riga con autofocus e placeholder; selettore urgenza a tre chip (`Urgente`, `A tempo perso`, `Eccezionale`); selettore ambito a cinque chip (`Casa`, `Pratiche`, `Salute`, `Lavoro`, `Altro`).
- **Default:** `A tempo perso` e `Altro`, modificabili prima di salvare.
- **Validazione:** testo vuoto o solo spazi → primario disabilitato. Duplicato per testo + urgenza + ambito, anche in Fatte → errore inline `Azione già presente`, foglio aperto, valori conservati. L'azione in modifica non è duplicato di sé.
- **Salvataggio:** chiude il foglio e inserisce l'azione nel gruppo corretto, in ordine alfabetico. La modifica aggiorna il gruppo se cambiano urgenza o ambito.
- **Annulla:** chiude senza scrivere. Il foglio non è il form prodotto e non usa il catalogo.

### 4.10 Condivisione a Tre Scelte (`share-dialog`)
- **Apertura:** icona Condividi su entrambe le pagine.
- **Scelte:** `Solo spesa (N)`, `Solo azioni (M)`, `Entrambe (N+M)`. Le opzioni con una parte vuota sono disabilitate con nota `Nessuna voce`. `Entrambe` è selezionabile solo se sia la spesa che le cose da fare hanno almeno una voce non spuntata; se entrambe le parti sono vuote, l'icona Condividi mostra `Non c'è nulla da condividere` e non apre il dialogo.
- **Default:** la scelta corrispondente alla pagina corrente (Spesa → Solo spesa; Da fare → Solo azioni).
- **Contenuto:** le azioni spuntate sono sempre escluse. Solo spesa mantiene il formato attuale; solo azioni produce `[DA FARE]`; entrambe concatenano prima la spesa e poi `[DA FARE]`. Aree nell'ordine Casa, Pratiche, Salute, Lavoro, Altro; urgenze nell'ordine Urgente, A tempo perso, Eccezionale; azioni alfabetiche.
- **Esito:** si apre l'Android Sharesheet con oggetto `Lista spesa TOGO`, `Cose da fare TOGO` o `Lista spesa e cose da fare TOGO`. Nessun file separato e nessuna modifica alle liste.

### 4.11 Import con Azioni
- **Accesso:** Impostazioni → Importa lista, come oggi.
- **Descrizione ed esempio:** includono il blocco `[DA FARE]` con `+ AMBITO`, `- URGENZA` e riga azione indentata di due spazi.
- **Anteprima:** riporta prodotti, azioni, duplicati saltati e categorie/prodotti creati in una regione con heading annunciata ai lettori di schermo. Se il testo contiene solo azioni, la conferma è valida e mostra 0 prodotti. Il pulsante di conferma si etichetta `Importa N prodotti e M azioni` (o `Importa` quando una parte è zero), mai `Accetta`.
- **Errori:** urgenza mancante o non valida, blocco vuoto, riga non valida → errore atomico, nessuna scrittura. Ambito mancante → Altro. Un blocco non valido annulla l'intero import. L'errore è associato al campo, annunciato con `LiveRegionMode.Assertive`, e il focus si sposta sul campo o sul primo errore.
- **Deduplica:** testo + ambito + urgenza, ignorando maiuscole e spazi ma non gli accenti. Le azioni spuntate contano come presenti e non vengono riaperte. I duplicati dentro il testo sono saltati. Lo stesso testo con ambito o urgenza diversi è un'azione distinta.
- **Esito:** messaggio con prodotti, azioni, duplicati saltati e creazioni. `[DA FARE]` non crea prodotti o categorie.

---

## 5. State Patterns

| Stato | Superficie | Ambito | Manifestazione Visiva e Comportamentale |
|---|---|---|---|
| **Cold Load (Avvio app)** | App | Globale | Lista caricata istantaneamente dal database SQLite locale senza schermate di caricamento (zero spinner percettibili). Si parte sempre dalla pagina Spesa. |
| **Empty State Spesa** | Pagina Spesa | Pagina | Illustrazione minimale o icona carrello neutra con testo *"Nessun prodotto da acquistare"*, con pulsanti chiari per dettare a voce o digitare. |
| **Empty State Da fare** | Pagina Da fare | Pagina | Icona di spunta/azione neutra con testo *"Nessuna cosa da fare. Tocca + per aggiungere un'azione."* Il FAB (+) resta visibile e apre il foglio azione. |
| **Fatte vuota** | Sezione Fatte | Sezione | La sezione `Fatte (N)` non compare se N = 0. Se N > 0 è espansa di default e mostra il pulsante `Concludi attività`. |
| **Azione duplicata** | Foglio Azione | Overlay | Il foglio azione resta aperto, il testo e i chip scelti restano visibili, compare l'errore inline `Azione già presente.` Il primario resta disabilitato finché la combinazione non cambia. |
| **Conclusione attività** | Sezione Fatte / Dialogo | Overlay | Dialogo di conferma con conteggio e avviso irreversibile. Dopo la conferma le spuntate spariscono, le altre restano. Nessun annulla e nessuno storico. |
| **Condivisione con parte vuota** | Dialogo Condivisione | Overlay | L'opzione corrispondente è disabilitata con nota `Nessuna voce`; `Entrambe` è disponibile solo se entrambe le parti hanno voci. Se entrambe sono vuote, l'icona Condividi mostra `Non c'è nulla da condividere` e non apre il dialogo. |
| **Import solo azioni** | Importa Lista | Overlay | La conferma mostra `0 prodotti` e N azioni; l'esito non parla di prodotti se non ce ne sono. Il blocco non crea categorie o prodotti. |
| **Import non valido** | Importa Lista | Overlay | Messaggio d'errore con numero di riga e motivo; nessuna scrittura, né prodotti né azioni. Un'urgenza non valida è un errore, un ambito sconosciuto diventa Altro. |
| **Cambio pagina** | Pager | Interazione | Transizione orizzontale breve (240 ms) con il titolo e il conteggio aggiornati; nessun rimbalzo o animazione decorativa. La posizione di scorrimento di ciascuna pagina è conservata. |
| **Offline Mode** | App | Globale | Nessun banner di errore fastidioso: l'app funziona al 100% offline per tutte le funzioni locali. Solo se il vocale non dispone di modello offline di sistema, il pulsante microfono notifica l'indisponibilità con fallback su inserimento manuale. |
| **Ambiguità Vocale** | Cattura Vocale | Overlay | Se il comando vocale manca di quantità o unità critica (es. *"compra formaggio"*), il bottom sheet evidenzia solo il campo mancante con selettore numerico/unità immediato, senza rigettare l'intera frase. |
| **Prodotto sconosciuto** | Aggiunta / Modifica Prodotto | Overlay | Se il prodotto non appartiene alle 1.400 voci del catalogo, viene presentato un mini-wizard a 3 tocchi (Livello 1 -> Livello 2 -> Livello 3) per categorizzarlo correttamente prima di salvarlo. |

---

## 6. Interaction Primitives

- **Target di tocco:** Minimo `{spacing.touch-target-min}` (48x48dp) su tutte le aree interattive per garantire facilità d'uso con una sola mano mentre si cammina tra le corsie.
- **Haptic Feedback:** Vibrazione secca a bassa latenza su:
  1. Toggle checkbox di spunta prodotto.
  2. Toggle checkbox di spunta azione.
  3. Conferma e aggiunta vocale.
  4. Conferma di `Concludi attività`.
- **Zona del pager:** lo swipe orizzontale cambia pagina solo se inizia sull'intestazione, su un'intestazione di gruppo o su uno spazio non occupato da una riga. Un drag iniziato su una riga resta del cestino e non muove il pager.
- **Frecce di pagina:** target da 48dp, sempre visibili; quella senza pagina disponibile è disabilitata. Il tap è alternativo allo swipe e funziona con TalkBack.
- **Transizione di pagina:** scorrimento orizzontale breve (240 ms), senza rimbalzi, con il titolo e il conteggio aggiornati. Le due liste mantengono la propria posizione di scorrimento.
- **Scansione visiva ad alto contrasto:** Ordinamento delle categorie coerente con il percorso fisico del supermercato (Ortofrutta → Banco Frigo/Macelleria → Panetteria → Dispensa → Igiene/Cura Casa). Le azioni sono ordinate per ambito, poi urgenza, poi alfabetico.
- **IME e tastiera:** il foglio azione usa `imePadding` e `bringIntoViewRequester` sul campo attivo; i pulsanti del foglio restano sopra la tastiera aperta. Nessun autofocus se uno screen reader o Switch Access è attivo.

---

## 7. Accessibility Floor

- **Contrasto cromatico:** Rapporto di contrasto testo/sfondo sempre >= 7:1 (rispetta ampiamente lo standard WCAG AAA per elementi chiave).
- **Screen Reader (TalkBack):**
  - Ogni riga prodotto espone un'etichetta semantica completa (es. *"Latte parzialmente scremato, due litri, Banco Frigo, non spuntato. Tocca due volte per segnare come acquistato"*).
  - Lo stato della sezione collassabile annuncia esplicitamente *"Sezione presi, 2 articoli, espansa/collassata"*.
  - Il titolo della pagina è un heading e annuncia il conteggio: *"Da fare, 5 azioni non spuntate"*. Le frecce hanno etichette *"Vai a Da fare"* e *"Torna a Spesa"*; quella disabilitata è annunciata come non disponibile.
  - Al cambio pagina viene annunciato il nuovo titolo e il conteggio, senza duplicare l'annuncio della freccia.
  - Ogni riga azione espone testo, urgenza, ambito e stato: *"Chiamare il dentista, Urgente, Salute, non spuntata. Doppio tocco per modificare. Azioni disponibili: Elimina"*. La checkbox è un controllo separato: *"Spunta Chiamare il dentista"*. L'eliminazione è raggiungibile sia con swipe a sinistra che con l'azione accessibile `Elimina` della riga.
  - Le righe in Fatte annunciano *"fatta"* e l'azione disponibile è solo la deselezione: *"Deseleziona Chiamare il dentista"*. Non espongono modifica o eliminazione.
  - I badge sono annunciati con l'etichetta scritta: *"Urgenza: Urgente"*, *"Ambito: Salute"*. Il colore non è l'unico veicolo.
  - Il dialogo di condivisione annuncia ogni scelta con conteggio e stato: *"Solo azioni, 5 azioni, selezionato"*. Le scelte disabilitate spiegano perché.
  - Il dialogo di conclusione annuncia il conteggio e l'irreversibilità: *"Concludere le attività? Verranno eliminate 3 attività spuntate. L'operazione non si può annullare"*.
- **Supporto Dynamic Type:** I testi scalano con le impostazioni di ingrandimento font di sistema Android (scaling non lineare oltre una soglia da Android 14) senza tagliare o troncare i nomi dei prodotti e delle azioni. L'intestazione può crescere e mandare il titolo a capo; i badge possono andare a capo sotto il testo; la riga azione cresce in altezza. Testare a 130%, 150% e 200% su 320dp.
- **Semantica checkbox:** ruolo `checkbox`, stato `checked`/`unchecked` esposto tramite `stateDescription`, etichetta pari al testo dell'azione. La checkbox è un nodo separato e conciso rispetto alla riga.
- **Semantica chip urgenza/ambito:** gruppo `radiogroup` con etichetta di gruppo ("Urgenza", "Ambito"); ogni chip è un radio con `selected`; il cambio di selezione annuncia il valore scelto.
- **Semantica header collassabile Presi/Fatte:** stati `expanded`/`collapsed`, ruolo heading, conteggio annunciato; il chevron è decorativo (`clearAndSetSemantics`).
- **Semantica dialog e bottom sheet:** `paneTitle` pari al titolo, focus iniziale sul titolo o primo campo, focus trap, `onDismissRequest` per tasto indietro/scrim, focus di ritorno al controllo trigger (o titolo pagina/FAB se il trigger non esiste più).
- **Focus visibile e traversal:** ogni controllo interattivo espone un focus ring ad alto contrasto (`{colors.focus-ring}` su chiaro, `{colors.focus-ring-dark}` su scuro, rapporto ≥3:1 sulle superfici adiacenti). L'ordine di traversal è: intestazione di pagina → lista → sezioni collassabili → FAB. Le frecce del pager sono sempre focusabili e annunciate con il loro stato (disponibile / non disponibile).

---

## 8. Key Flows

### Flow 1: Mike cattura il latte a voce con le mani impegnate (UJ-1)
1. **Contesto:** Mike è in cucina, sta preparando la colazione e si accorge che il latte sta finendo.
2. **Azione:** Tocca il grande pulsante Microfono centrale (`{components.voice-fab}`).
3. **Pronuncia:** *"Due litri di latte parzialmente scremato"*.
4. **Interpretazione:** Il bottom sheet si apre immediatamente; la forma d'onda cattura l'audio; in 0.8s compare la trascrizione e la card strutturata: `Latte parzialmente scremato | 2 l | Banco Frigo / Latticini`.
5. **Climax:** Mike vede a colpo d'occhio che l'interpretazione è perfetta e tocca il grande pulsante nero `Conferma e aggiungi`.
6. **Risoluzione:** Riceve un haptic feedback secco; il foglio si ritrae; la voce appare ordinata sotto la categoria Banco Frigo nella lista attiva. Nessuna registrazione vocale viene conservata.

### Flow 2: Mike fa la spesa al supermercato e conclude la spesa (UJ-2)
1. **Contesto:** Mike è al supermercato con il carrello; consulta TOGO con una mano sola.
2. **Consultazione:** La lista attiva mostra le categorie disposte nell'ordine naturale delle corsie: prima Ortofrutta, poi Banco Frigo, poi Dispensa.
3. **Spunta:** Al banco frigo prende il latte e tocca la checkbox: sente la vibrazione tattile; il latte viene barrato e scivola nella sezione pieghevole *"Presi (1)"* in fondo.
4. **Controllo:** Mike vede che restano solo mele e pasta. Prosegue senza distrazioni.
5. **Climax:** Ha preso tutto ciò che era in corsia, tranne un articolo che il supermercato non aveva. Apre la sezione *"Presi"*, verifica che ci siano tutti gli articoli desiderati e tocca `Concludi spesa`.
6. **Risoluzione:** I prodotti presi vengono archiviati permanentemente nello Storico. L'articolo non trovato rimane nella lista attiva, pronto per la prossima spesa.

### Flow 3: Mike condivide la lista con un familiare (UJ-3) — baseline legacy
Questo flow descrive il comportamento storico di condivisione della sola spesa, antecedente al dialogo a tre scelte. Il formato attuale è definito in §4.10 e Flow 8.
1. **Contesto:** Mike è impegnato al lavoro e chiede al partner di passare al supermercato.
2. **Azione:** Tocca l'icona Condividi nell'intestazione di pagina.
3. **Generazione:** TOGO genera un testo leggibile e ordinato:
   ```text
   🛒 TOGO - Lista Spesa (3 articoli)

   ORTOFRUTTA:
   • Mele Gala — 1 kg
   • Banane — 6 pz

   BANCO FRIGO:
   • Mozzarella di bufala — 250 g
   ```
4. **Invio:** L'Android Sharesheet nativo si apre immediatamente; Mike sceglie WhatsApp e invia il messaggio in 2 tocchi.

### Flow 4: Mike inserisce un duplicato e applica la somma raccomandata (UJ-4)
1. **Contesto:** Mike detta a voce *"Un litro di latte"* dimenticando di averne già segnato 1 litro.
2. **Rilevamento:** TOGO rileva che "Latte parzialmente scremato" è già presente con 1 l nella stessa categoria.
3. **Dialog:** Compare il dialogo ad alto contrasto con il messaggio chiaro: *"Il prodotto è già presente con 1 l. Cosa vuoi fare?"*.
4. **Scelta:** L'azione raccomandata primaria evidenziata in nero è `Somma quantità (+1 l → 2 l)`.
5. **Risoluzione:** Mike tocca il pulsante; la riga esistente si aggiorna a 2 l senza creare duplicati incoerenti né modificare nulla silenziosamente.

### Flow 5: Mike riaggiunge un prodotto dallo Storico (UJ-5)
1. **Contesto:** Mike prepara la spesa settimanale da casa.
2. **Azione:** Tocca l'icona Storico nell'App Bar, a sinistra del titolo della pagina Spesa.
3. **Navigazione:** Digita *"caffè"* nella barra di ricerca dello Storico.
4. **Aggiunta:** Tocca il pulsante rapido `+ Aggiungi` accanto alla riga del caffè macinato.
5. **Risoluzione:** Il prodotto viene inserito istantaneamente nella lista attiva corrente sotto la categoria Dispensa con la sua quantità abituale (250 g).

### Flow 6: Mike aggiunge una cosa da fare mentre è in sala d'attesa
1. **Contesto:** Mike è in sala d'attesa e ricorda di dover chiamare il dentista. Ha una mano sola libera.
2. **Azione:** Apre TOGO, swipa sull'intestazione verso sinistra (o tocca la freccia destra) e arriva a **Da fare**.
3. **Aggiunta:** Tocca il FAB (+). Il foglio azione si apre con `A tempo perso` e `Altro` già selezionati.
4. **Compilazione:** Scrive *"Chiamare il dentista"*, sceglie `Urgente` e `Salute`.
5. **Climax:** Vede il badge ambra `URGENTE` e il badge `SALUTE`; tocca `Aggiungi`. L'azione compare sotto Salute → Urgente, in ordine alfabetico.
6. **Risoluzione:** Torna a Spesa con la freccia sinistra. La lista della spesa è esattamente come prima.

### Flow 7: Mike conclude le attività senza toccare la spesa
1. **Contesto:** È domenica sera; Mike ha finito le faccende della settimana e vuole ripulire la lista da fare.
2. **Azione:** Apre **Da fare** e spunta *"Pagare la bolletta"* e *"Chiamare l'idraulico"*.
3. **Feedback:** Le due righe si barranno e, dopo 300 ms, passano in `Fatte (2)`. *"Comprare le lampadine"* resta attiva.
4. **Controllo:** Mike apre la sezione Fatte e verifica che ci siano solo le due azioni concluse.
5. **Climax:** Tocca `Concludi attività`; il dialogo dice *"Verranno eliminate 2 attività spuntate. L'operazione non si può annullare."* Conferma.
6. **Risoluzione:** Le due azioni spariscono, quella non spuntata resta, la Spesa e lo storico acquisti sono invariati. Non c'è annulla e non c'è uno storico delle azioni.

### Flow 8: Mike condivide solo le cose da fare
1. **Contesto:** Mike è in viaggio e chiede al partner di occuparsi delle faccende di casa.
2. **Azione:** Apre **Da fare** e tocca l'icona Condividi.
3. **Scelta:** Il dialogo mostra `Solo spesa (4)`, `Solo azioni (3)`, `Entrambe (7)`. Il default è `Solo azioni` perché Mike è sulla pagina Da fare.
4. **Generazione:** TOGO compone il blocco `[DA FARE]` con aree, urgenze e righe indentate; le azioni in Fatte sono escluse.
5. **Climax:** Mike tocca `Condividi` e invia il testo con WhatsApp in due tocchi.
6. **Risoluzione:** Il partner riceve un testo leggibile e ordinato; nessuna delle due liste viene modificata.

### Flow 9: Mike reimporta un testo con azioni e prodotti
1. **Contesto:** Il partner ha aggiunto due cose da fare al testo condiviso e lo rimanda a Mike.
2. **Azione:** Mike apre Impostazioni → Importa lista e incolla il testo.
3. **Anteprima:** TOGO mostra *"Verranno importati 3 prodotti e 2 azioni. 1 duplicato sarà saltato. Nel catalogo verrà creata 1 categoria."*
4. **Conferma:** Mike tocca `Accetta`.
5. **Climax:** I prodotti seguono l'import della spesa; le due azioni si aggiungono alla lista esistente senza duplicati e senza creare prodotti o categorie. `[DA FARE]` non compare nel catalogo.
6. **Risoluzione:** Mike torna alla lista: le azioni nuove sono nei gruppi corretti, quelle già presenti non sono state duplicate.

### Flow 10: Mike cambia pagina e spunta con TalkBack
1. **Contesto:** Mike usa TalkBack e tiene il telefono con una mano.
2. **Intestazione:** Sente *"Spesa, 4 voci non spuntate. Freccia destra: vai a Da fare."*
3. **Cambio pagina:** Doppio tocco sulla freccia destra; TOGO annuncia *"Da fare, 2 azioni non spuntate."*
4. **Esplorazione:** Scorre le righe; ogni riga annuncia testo, urgenza, ambito e stato. I badge sono letti come *"Urgenza: Urgente"* e *"Ambito: Salute"*.
5. **Spunta:** Doppio tocco sulla checkbox; sente il feedback aptico e l'azione passa in `Fatte (1)`.
6. **Risoluzione:** Può deselezionare dalla sezione Fatte o concludere con `Concludi attività`; il dialogo annuncia il conteggio e l'irreversibilità.

## 9. UX Assumptions / Decisioni aperte

Tutte le decisioni di UX per Spesa e Cose da fare sono state commesse nelle sezioni precedenti (header a una riga, foglio azione, Fatte espansa, default condivisione, animazione pager 240 ms, microcopy vincolante). Questa sezione è riservata alle decisioni ancora aperte; al momento non ve ne sono.
