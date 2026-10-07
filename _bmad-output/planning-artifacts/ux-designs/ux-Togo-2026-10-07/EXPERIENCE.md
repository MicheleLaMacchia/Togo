---
name: Togo
status: final
sources:
  - _bmad-output/planning-artifacts/prds/prd-Togo-2026-09-20/prd.md
  - _bmad-output/planning-artifacts/briefs/brief-Togo-2026-09-20/brief.md
created: 2026-10-07
updated: 2026-10-07
---

# TOGO — Experience Spine

> Riferimento comportamentale ufficiale per l'applicazione Android TOGO. Definisce l'architettura informativa, le interazioni, gli stati, il microcopy e i percorsi utente.  
> Paired with `DESIGN.md` (High-Contrast Utility visual identity). I token visivi sono referenziati tramite sintassi `{path.to.token}`.  
> Spines win on conflict: questo documento e `DESIGN.md` vincono su qualunque mockup o wireframe.  
> Mockup di riferimento per la composizione: [`mockups/active-list.html`](mockups/active-list.html), [`mockups/voice-sheet.html`](mockups/voice-sheet.html), [`mockups/duplicate-dialog.html`](mockups/duplicate-dialog.html), [`mockups/history.html`](mockups/history.html).

---

## 1. Foundation

- **Piattaforma e Form-factor:** Mobile smartphone Android (riferimento primario Android 16, supporto base API 21+). Utilizzo esclusivo in orientamento verticale (portrait) ottimizzato per l'interazione ad una sola mano.
- **UI Framework:** Android Nativo con **Jetpack Compose** e Material 3 come fondazione dei controlli di sistema.
- **Architettura dati:** **Local-first** al 100%. Persistenza su database SQLite/Room locale. Funzionamento completo offline per tutte le funzioni di gestione lista, catalogo prodotti, storico e regole apprese.
- **Servizio vocale:** Integrato tramite `RecognizerIntent` di Android (`it-IT`). La non disponibilità momentanea del servizio vocale non pregiudica alcuna funzionalità manuale dell'app.
- **Riferimento Visivo:** Tutte le decisioni estetiche, metriche e cromatiche derivano da `DESIGN.md`.

---

## 2. Information Architecture

### 2.1 Mappa delle Superfici

| Superficie | Raggiunta da | Scopo e Responsabilità |
|---|---|---|
| **Lista Attiva ("Spesa")** | Apertura app (cold start) | Vista primaria: prodotti da comprare raggruppati per corsia/categoria, sezione collassabile "Presi (N)", azione "Concludi spesa", trigger vocale e manuale. |
| **Cattura Vocale (Bottom Sheet)** | Tap su pulsante Microfono (FAB) | Ascolto live, trascrizione vocale in tempo reale, scomposizione strutturata (prodotto, quantità, unità, categoria) e conferma esplicita con un tocco. |
| **Aggiunta / Modifica Manuale** | Tap su "+ Aggiungi" o tap su riga prodotto | Form compatto con autocompletamento catalogo (1400+ prodotti), selettore quantità/unità e attributi opzionali contestuali. |
| **Risoluzione Duplicati (Modal)** | Inserimento vocale o manuale di prodotto già presente | Dialogo modale con 3 scelte esplicite: Somma quantità (raccomandata), Sostituisci quantità, Crea voce separata. |
| **Storico Prodotti** | Tap su icona Storico (App Bar in alto a destra) | Consultazione degli articoli acquistati nelle spese passate con ricerca rapida e pulsante "+ Riaggiungi" a un tocco nella lista attiva. |
| **Gestione Regole Apprese** | Tap su icona Impostazioni (App Bar) | Visualizzazione, modifica ed eliminazione delle associazioni personalizzate prodotto-categoria apprese dall'utente. |
| **Condivisione (Share Sheet)** | Tap su icona Condividi (App Bar) | Generazione di testo formattato dei soli prodotti rimasti da acquistare e apertura del menu di condivisione di sistema Android. |

### 2.2 Principi di Navigazione
- **Single-Surface Rigorosa:** L'app non utilizza bottom tab bar o navigation drawer laterali. La schermata della lista attiva occupa l'intero spazio utile dello schermo per massimizzare la visibilità delle voci in corsia.
- **Accesso Funzioni Secondarie:** Storico e Condivisione sono icone fisse nell'App Bar superiore (`{components.app-bar}`).
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

---

## 4. Component Patterns

### 4.1 Riga Prodotto (`item-row`)
- **Comportamento di Spunta (Check-off):**
  - Tap sulla checkbox quadrata (`{components.checkbox-utility}`):
    - Genera **haptic feedback** (vibrazione secca da 15ms).
    - La riga passa immediatamente allo stato "Preso" (testo barrato, opacità ridotta, checkbox riempita).
    - Dopo un micro-ritardo percepibile di 300ms per chiarezza visiva, la riga scivola fluidamente in fondo alla lista nella sezione collassabile *"Presi (N)"*.
- **Riapertura / Deselezione:**
  - Nella sezione *"Presi"*, toccare nuovamente la checkbox ripristina istantaneamente il prodotto nella sua categoria d'origine nella lista attiva.
- **Modifica rapida:** Tap sul testo del prodotto apre il foglio di modifica manuale. Swipe verso sinistra rivela il pulsante rapido "Elimina".

### 4.2 Sezione Collassabile "Presi (N)"
- Posizionata stabilmente dopo l'ultima categoria di prodotti attivi.
- Mostra il contatore totale degli articoli spuntati nella sessione.
- Default: Espansa durante la spesa se ci sono elementi presi di recente, con chevron per collassarla se si desidera compattare la vista.
- In fondo alla sezione compare il pulsante sticky `{colors.border-crisp}` **"Concludi spesa"**.

### 4.3 Flusso "Concludi Spesa"
- Pressione del pulsante "Concludi spesa":
  - Mostra un dialog rapido o snackbar di conferma.
  - Sposta tutti gli articoli barrati nello **Storico permanente**.
  - Rimuove definitivamente gli articoli barrati dalla schermata attiva.
  - **Tutti i prodotti non spuntati rimangono intatti nella lista attiva** per la spesa successiva.

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
- **Azione raccomandata (default a vista):** `[Somma quantità]` (somma algebrica se le unità sono compatibili, es. 1 l + 1 l = 2 l).
- **Opzioni secondarie:** `[Sostituisci quantità]` e `[Crea voce separata]`.
- Se le unità sono incompatibili (es. 1 bottiglia vs 500 ml), l'opzione "Somma" viene disabilitata con nota esplicativa.

---

## 5. State Patterns

| Stato | Manifestazione Visiva e Comportamentale |
|---|---|
| **Cold Load (Avvio app)** | Lista caricata istantaneamente dal database SQLite locale senza schermate di caricamento (zero spinner percettibili). |
| **Empty State (Lista vuota)** | Illustrazione minimale o icona carrello neutra con testo *"Nessun prodotto da acquistare"*, con pulsanti chiari per dettare a voce o digitare. |
| **Offline Mode** | Nessun banner di errore fastidioso: l'app funziona al 100% offline per tutte le funzioni locali. Solo se il vocale non dispone di modello offline di sistema, il pulsante microfono notifica l'indisponibilità con fallback su inserimento manuale. |
| **Ambiguità Vocale** | Se il comando vocale manca di quantità o unità critica (es. *"compra formaggio"*), il bottom sheet evidenzia solo il campo mancante con selettore numerico/unità immediato, senza rigettare l'intera frase. |
| **Prodotto sconosciuto** | Se il prodotto non appartiene alle 1.400 voci del catalogo, viene presentato un mini-wizard a 3 tocchi (Livello 1 -> Livello 2 -> Livello 3) per categorizzarlo correttamente prima di salvarlo. |

---

## 6. Interaction Primitives

- **Target di tocco:** Minimo `{spacing.touch-target-min}` (48x48dp) su tutte le aree interattive per garantire facilità d'uso con una sola mano mentre si cammina tra le corsie.
- **Haptic Feedback:** Vibrazione secca a bassa latenza su:
  1. Toggle checkbox di spunta prodotto.
  2. Conferma e aggiunta vocale.
- **Scansione visiva ad alto contrasto:** Ordinamento delle categorie coerente con il percorso fisico del supermercato (Ortofrutta → Banco Frigo/Macelleria → Panetteria → Dispensa → Igiene/Cura Casa).

---

## 7. Accessibility Floor

- **Contrasto cromatico:** Rapporto di contrasto testo/sfondo sempre >= 7:1 (rispetta ampiamente lo standard WCAG AAA per elementi chiave).
- **Screen Reader (TalkBack):**
  - Ogni riga prodotto espone un'etichetta semantica completa (es. *"Latte parzialmente scremato, due litri, Banco Frigo, non spuntato. Tocca due volte per segnare come acquistato"*).
  - Lo stato della sezione collassabile annuncia esplicitamente *"Sezione presi, 2 articoli, espansa/collassata"*.
- **Supporto Dynamic Type:** I testi scalano linearmente con le impostazioni di ingrandimento font di sistema Android senza tagliare o troncare i nomi dei prodotti.

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

### Flow 3: Mike condivide la lista con un familiare (UJ-3)
1. **Contesto:** Mike è impegnato al lavoro e chiede al partner di passare al supermercato.
2. **Azione:** Tocca l'icona Condividi nell'App Bar in alto a destra.
3. **Generazione:** TOGO genera un testo leggibile e ordinato:
   ```text
   🛒 TOGO - Lista Spesa (3 articoli)

   🥦 ORTOFRUTTA:
   • Mele Gala — 1 kg
   • Banane — 6 pz

   🧀 BANCO FRIGO:
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
2. **Azione:** Tocca l'icona Storico nell'App Bar in alto a destra.
3. **Navigazione:** Digita *"caffè"* nella barra di ricerca dello Storico.
4. **Aggiunta:** Tocca il pulsante rapido `+ Aggiungi` accanto alla riga del caffè macinato.
5. **Risoluzione:** Il prodotto viene inserito istantaneamente nella lista attiva corrente sotto la categoria Dispensa con la sua quantità abituale (250 g).
