# Accessibility Review — TOGO

## Overall verdict

La coppia ha un'intenzione accessibile più solida della media: floor 48dp dichiarato, urgenza e ambito sempre a testo, frecce come alternativa al pager, conferma esplicita per la conclusione e microcopy TalkBack dettagliato. Non è però ancora un contratto accessibile completo: mancano un sistema di focus, la semantica di stato per checkbox, chip e sezioni, le live region per gli eventi dinamici e un'alternativa accessibile allo swipe-to-delete; inoltre le righe spuntate e il dark mode violano il contrasto AA e il floor 7:1 che i documenti stessi dichiarano. Il rischio downstream è alto: un'implementazione Compose fedele alla spine produce barriere per TalkBack, Switch Access, tastiera/D-pad e ipovedenti, anche se nessuna di queste barriere è visibile in un mockup statico.

## 1. Perceivable — thin

Ho verificato contrasto testo/sfondo, icone, bordi, badge, stati spuntati/disabilitati, coppie light/dark, uso del colore per urgenza/ambito/selezione, testi su immagini (non presenti), dimensione del testo e scaling. I ratio sono calcolati con la formula WCAG 2.x sui token di `DESIGN.md` e sui valori dei mockup; la tabella completa è in fondo. La base acromatica è eccellente (`ink-primary` su bianco 17,85:1) e l'urgenza non è mai affidata al solo colore, ma gli stati "consumati" e il tema scuro non reggono.

### Findings

- **critical** Le righe spuntate non sono leggibili. `DESIGN.md §7.1 L255` impone `{colors.ink-muted}` (`#94A3B8`) più `opacity 45%`: su bianco il risultato è circa 1,46:1; i mockup usano `#94A3B8` pieno (circa 2,45–2,56:1) e nelle sezioni `Presi`/`Fatte` opacizzano anche i badge a 0.55 (circa 2,7–2,9:1) (`active-list.html L272-276`, `todo-page.html L328-336`). Le righe restano interattive — la checkbox deseleziona — quindi non sono componenti disabilitati e non godono dell'eccezione di 1.4.3; anche il floor 7:1 è violato due volte. *Fix:* introdurre un token `ink-struck` ≥4,5:1 (idealmente 7:1), non applicare opacità al testo, tenere barratura e checkbox come ridondanza e riservare `ink-muted` ai veri disabilitati.

- **critical** Il dark mode è dichiarato (`DESIGN.md §2 L191-192`) ma non è tokenizzato per le superfici che i componenti usano davvero: mancano `surface-subtle-dark`, `badge-bg-dark`, `border-hairline-dark`, `accent-action-dark` (`L7-47`). Con i token attuali `category-header` usa `surface-subtle #F8FAFC` e il testo passa a `ink-primary-dark #F8FAFC`: 1,00:1, titolo invisibile; `area-badge` usa `ink-secondary-dark #94A3B8` sullo stesso fondo: 2,45:1; `quantity-badge` nero su base quasi nera: 1,07:1; il fill di pulsanti primari e FAB sparisce (1,07:1). *Fix:* definire le coppie scure per ogni superficie e mappare i componenti (`category-header`, `area-badge`, `quantity-badge`, `voice-fab`, `action-sheet`, `choice-dialog`), oppure rimuovere la promessa dark; aggiungere un mockup scuro di verifica.

- **high** `border-crisp-dark` ha due valori: `#64748B` nel frontmatter (`L37`) e `#475569` nella prosa (`L192`). Sul fondo `#0B0F17` il primo dà 4,03:1, il secondo 2,53:1; su `surface-card-dark` il secondo scende a 2,26:1. Se vince la prosa, i bordi di checkbox, pulsanti e FAB scendono sotto il 3:1 richiesto da 1.4.11 per identificare i controlli. *Fix:* fissare `#64748B` o più chiaro, correggere la prosa e verificare i bordi su entrambe le superfici scure.

- **high** Testi di supporto e placeholder sono sotto soglia: il placeholder `#94A3B8` su bianco è 2,56:1 (`history.html L134-137`); il placeholder del campo azione (`DESIGN.md §7.8 L303`) non ha un token dedicato e ricadrebbe sullo stesso muted. Gli hint `#64748B` su `#F1F5F9` sono 4,34:1 (`active-list.html L603`, `todo-page.html L599`, `conclude-dialog.html L534`). Non sono decorativi: spiegano dove cercare e come chiudere una sezione. *Fix:* portare placeholder e hint almeno a 4,5:1; mantenere un'etichetta visibile accanto al campo (già presente nel foglio azione) e non usare il solo placeholder come label.

- **medium** Il floor dichiarato è 7:1 (`DESIGN.md §8 L338`, `EXPERIENCE.md §7 L221`), ma `Urgente` `#92400E` su `#FEF3C7` è 6,37:1 (`DESIGN.md L22-23`); `accent-success` su bianco è 5,02:1 e `accent-highlight` su bianco 5,17:1. Passano AA ma non il floor; un implementatore che si fida del 7:1 non ha un riferimento vero. *Fix:* o si abbassa il floor ad AA (4,5:1 testo, 3:1 non-testo) e lo si dichiara, o si scuriscono i token; in ogni caso si documentano i ratio delle combinazioni portanti.

## 2. Operable — thin

Ho verificato target touch, gesti e alternative, swipe del pager, swipe-to-delete, focus visibile e ordine di traversal, contenimento e chiusura di modali, tempi dell'undo, conferme distruttive, tastiera/D-pad. Le frecce sono una buona alternativa al pager e la conclusione attività ha una conferma; il resto è incompleto.

### Findings

- **critical** L'eliminazione di una riga è solo swipe a sinistra (`EXPERIENCE.md §4.7 L143`, `DESIGN.md §7.7 L297`) e l'etichetta TalkBack dice "Swipe a sinistra per eliminare" (`§7 L227`). Con TalkBack lo swipe a un dito cambia il focus; con Switch Access il gesto non esiste. Il cestino è quindi irraggiungibile e CAP-6 (`SPEC.md L34-35`) resta accessibile solo a chi usa il gesto: 2.5.7 è violato. *Fix:* aggiungere un'azione `Elimina` nelle custom actions della riga e/o un menu contestuale, con la stessa snackbar di undo; correggere l'etichetta in "Azioni disponibili: Elimina".

- **critical** Non esiste un contratto di focus visibile né di ordine di traversal: `DESIGN.md` non ha un token `focus-ring`, `EXPERIENCE.md §7 L219-232` copre solo TalkBack e Dynamic Type, e i controlli sono disegnati a mano (checkbox, righe, chip). Con D-pad, tastiera e Switch Access il focus può essere invisibile o finire su un nodo non operabile: 2.4.7 è violato. *Fix:* definire un focus ring ad alto contrasto (es. 3dp `accent-highlight` con offset, ≥3:1 sulle superfici adiacenti), l'ordine di traversal (header → lista → sezioni → FAB) e testarlo con tastiera, D-pad e Switch Access.

- **high** Dopo la spunta la riga si sposta in `Presi`/`Fatte` dopo 300 ms (`EXPERIENCE.md §4.1 L97-98`, §4.7 L144). Se il focus TalkBack/Switch è sulla checkbox, il nodo si muove o scompare mentre l'annuncio è in corso: focus perso o salto imprevedibile; anche per il tocco il prossimo tap può cadere su un'altra riga. *Fix:* non spostare la riga finché il focus è su di essa; in alternativa spostare il focus sull'header della sezione e annunciare "Spostata in Presi, N articoli", con un'azione "Annulla spunta" immediata.

- **high** Il ritorno del focus dopo la chiusura di dialoghi e fogli non è specificato: dopo `Concludi attività` il pulsante che ha aperto il dialogo scompare (la sezione Fatte non c'è più), dopo `Condividi` si apre lo Sharesheet, dopo `Salva` il foglio si chiude. Senza una regola il focus resta sul nodo rimosso o torna all'inizio della pagina (2.4.3). *Fix:* definire per ogni modale il target di ritorno (trigger; se non esiste, titolo pagina o FAB), con fallback e annuncio; al ritorno dallo Sharesheet riportare il focus sull'icona Condividi.

- **high** L'undo dell'eliminazione singola è una snackbar (`EXPERIENCE.md §4.7 L147`) senza durata, pausa o annuncio. La snackbar Material dura pochi secondi: per ipovedenti, utenti cognitivi e TalkBack è spesso troppo poco (2.2.1). *Fix:* durata ≥10 s o persistente fino all'azione successiva, pausa su focus/hover, live region educata con "Azione eliminata. Annulla disponibile" e azione Annulla ≥48dp.

- **high** Il pager e il cestino usano lo stesso asse dello swipe di sistema e non c'è una politica per edge-to-edge, gesture insets o predictive back (`EXPERIENCE.md §2.2 L54`, §4.6 L134-135; `brownfield.md L17, L22`). Su gesture navigation lo swipe da bordo può uscire dall'app invece di cambiare pagina o rivelare il cestino; il back da Da fare rischia un'anteprima di chiusura app invece di Spesa. *Fix:* definire zone di swipe lontane dai bordi, `systemGestureExclusion` solo per l'header del pager, `PredictiveBackHandler` per Da fare→Spesa e test con gesture nav e 3-button nav.

- **medium** I mockup, reference di composizione, mostrano target sotto il floor 48dp: icone header 44×44 (`active-list.html L118-124`, `todo-page.html L115-121`), checkbox azione 40×40 (`todo-page.html L234-242`), chip 38dp (`action-sheet.html L395-409`), matita categoria 28×28 (`voice-sheet.html L261-272`), Annulla duplicati 44dp (`duplicate-dialog.html L214-226`). *Fix:* portare l'area di tocco a 48dp mantenendo la dimensione visiva; aggiungere token `chip` e `icon-button` con hit area esplicita.

- **medium** La conferma di `Concludi spesa` è ambigua: `EXPERIENCE.md §4.3 L111` dice "dialog rapido o snackbar di conferma", ma una snackbar non è una conferma (svanisce e non richiede un'azione) e il microcopy `§3 L71` prevede `[Concludi spesa]`/`[Annulla]`. *Fix:* commit a un dialogo con conteggio e due azioni, come per `Concludi attività`; se si vuole la snackbar, usarla solo come feedback post-azione.

- **medium** La freccia disabilitata deve "restare annunciata come non disponibile" (`DESIGN.md §7.6 L288`, `EXPERIENCE.md §4.6 L134`, §7 L225), ma un controllo `enabled=false` non è focusabile in TalkBack: le due richieste sono incompatibili. *Fix:* scegliere: rimuovere la freccia e annunciare "pagina 1 di 2" nel titolo, oppure tenerla focusabile con `stateDescription="Non disponibile"` e nessuna azione.

## 3. Understandable — adequate

Ho verificato etichette e hint, identificazione e prevenzione degli errori per import/eliminazione/conclusione, coerenza della navigazione, lingua e plain Italian, coerenza dei messaggi di stato (il loro annuncio è trattato in §4). Il microcopy è sobrio e concreto, gli errori sono nominati e la conclusione è spiegata; mancano però associazione/annuncio degli errori e coerenza posizionale dell'header.

### Findings

- **high** Gli errori non hanno un contratto di annuncio o associazione: `Azione già presente` compare inline (`EXPERIENCE.md §4.9 L163`, §5 L192) e l'import riporta numero di riga e motivo (`§5 L196`), ma non è detto che il campo abbia `isError`/`supportingText`, che il messaggio sia una live region, né che il focus si sposti sul campo o sul primo errore. Un utente TalkBack può non accorgersi dell'errore e riprovare. *Fix:* associare l'errore al campo, annunciarlo con `LiveRegionMode.Assertive`, spostare il focus sul campo (o su un riepilogo errori nell'import) e conservare i valori.

- **high** La posizione dei controlli dell'header cambia tra le pagine: su Spesa il primo controllo è Storico e la freccia sinistra è disabilitata al secondo posto; su Da fare il primo controllo è "Torna a Spesa" (`DESIGN.md §7.6 L287-289`, `EXPERIENCE.md §4.6 L132`; mockup `active-list.html L457-466` vs `todo-page.html L459-462`). Chi impara "angolo sinistro = indietro" su Da fare apre Storico su Spesa; il traversal TalkBack cambia. 3.2.3 è a rischio. *Fix:* slot fissi: freccia sinistra sempre nello stesso punto, Storico in overflow o a destra, oppure top bar a due righe stabile.

- **medium** Il pulsante di conferma della conclusione ripete l'etichetta del trigger (`Concludi attività`) invece di descrivere la conseguenza; l'icona warning esiste nel mockup (`conclude-dialog.html L555-573`) ma non è commessa in `DESIGN.md §7.10 L316-320`. Per un'azione irreversibile senza undo, l'etichetta dovrebbe dire cosa succede. *Fix:* primario "Elimina N attività" (o "Elimina definitivamente"), stile distruttivo e icona warning commessa; Annulla resta l'azione sicura.

- **medium** L'import crea prodotti/categorie e non ha undo; l'anteprima c'è (`EXPERIENCE.md §4.11 L177-180`) ma non è specificato che sia un riepilogo annunciato, né che il primario dica "Importa" invece di "Accetta" (`Flow 9 L312`). *Fix:* rendere l'anteprima una region con heading, etichettare il primario "Importa N prodotti e M azioni", annunciare l'esito e offrire un riepilogo post-import.

- **low** "Eccezionale" come livello di urgenza è ambiguo in italiano (può significare "ottimo"); per chi non conosce il modello le tre etichette non sono autoesplicative. *Fix:* aggiungere una riga di aiuto sotto il selettore ("Eccezionale = fuori routine, da fare appena possibile") o un termine più chiaro, senza cambiare il contratto.

## 4. Robust/semantics — thin

Ho verificato ruoli, nomi, stati e descrizioni per checkbox, chip, badge, pager, tab, FAB, snackbar e dialoghi; heading semantics, reading order, live region, custom action per lo swipe e annunci di check/uncheck, add/delete/undo, share/import. I testi TalkBack sono più ricchi della media, ma la semantica che li rende reali non è commessa.

### Findings

- **high** La checkbox è descritta a parole ma non ha un contratto di ruolo/stato: `EXPERIENCE.md §7 L223-228` mette lo stato dentro l'etichetta ("non spuntato") e la checkbox è "un controllo separato" (`L227`), ma non si dice `Role.Checkbox`, `toggleable` o `stateDescription`. Nei mockup è un `div` senza `role`/`aria-checked` (`active-list.html L513-515`, `todo-page.html L507-511`). Un implementatore può produrre un nodo senza stato o con stato duplicato. *Fix:* label = testo dell'azione, stato via `stateDescription`/`toggleable`, `Role.Checkbox`; checkbox decorativa rispetto alla riga o nodo separato ma conciso.

- **high** I chip di urgenza/ambito non hanno ruolo, gruppo o stato selezionato: `DESIGN.md §7.8 L304-305` e `EXPERIENCE.md §4.9 L161` li chiamano "chip", ma non dicono `Role.RadioButton`, `selectableGroup`, `selected`, etichetta di gruppo ("Urgenza", "Ambito") né annuncio al cambio. Nel mockup sono `<button>` senza `aria-pressed` (`action-sheet.html L592-607`). *Fix:* gruppo radiogroup con label, ogni chip radio con `selected`, stato annunciato; la nota "Sarà salvata in Salute → Urgente" può essere la descrizione del gruppo.

- **high** Non c'è un contratto di live region per i cambi dinamici: spunta, spostamento in Presi/Fatte, aggiunta, eliminazione+undo, esito condivisione/import, "Non c'è nulla da condividere", trascrizione vocale. Solo il cambio pagina è annunciato (`EXPERIENCE.md §7 L226`); 4.1.3 è a rischio. *Fix:* definire per ogni evento testo, priorità (polite/assertive) e se spostare il focus; evitare doppio annuncio con il focus.

- **high** Le sezioni collassabili hanno un testo di annuncio (`§7 L224`) ma non una semantica: l'header `Presi (N)`/`Fatte (N)` deve essere un controllo espandibile con `expanded/collapsed`, heading e conteggio, e il chevron decorativo (`DESIGN.md §7.3 L261-264`, §7.9 L309-314). Senza, TalkBack annuncia un generico "doppio tocco" e non lo stato. *Fix:* `Modifier.expandable`/`stateDescription`, `heading()`, click con etichetta "Presi, 2 articoli, espansa. Doppio tocco per comprimere"; chevron `clearAndSetSemantics`.

- **high** La semantica delle righe è sovraccarica: la riga annuncia testo+urgenza+ambito+stato+azioni (`§7 L227`), poi i badge annunciano di nuovo "Urgenza: Urgente"/"Ambito: Salute" (`L229`), poi la checkbox "Spunta …" (`L227`). Con due nodi per riga, una lista di 20 voci diventa circa 40 stop e le stesse informazioni vengono lette 2–3 volte. *Fix:* decidere un solo nodo per riga (toggle + custom actions) o accorciare la label della riga e marcare i badge come decorativi nel contesto; documentare il reading order.

- **medium** Solo il titolo di pagina è heading (`DESIGN.md §7.6 L291`, `EXPERIENCE.md §7 L225`); gli header di categoria/ambito (ORTOFRUTTA, CASA, PRATICHE, SALUTE) e delle sezioni non lo sono. La navigazione per heading di TalkBack non può saltare tra gruppi in una lista lunga. *Fix:* `heading()` su ogni header di gruppo e di sezione, con livello coerente; il titolo resta il primo heading.

- **medium** Dialoghi e bottom sheet non hanno semantica modale: niente `paneTitle`, focus iniziale, trap, `onDismissRequest` o focus di ritorno (`EXPERIENCE.md §4.5 L125-129`, §4.9 L159-165, §4.10 L167-172; `DESIGN.md §7.5 L275-283`, §7.8, §7.10, §7.11). Un overlay custom può lasciare il focus sulla lista sottostante. *Fix:* usare `Dialog`/`ModalBottomSheet` Compose, `paneTitle` = titolo, focus iniziale sul titolo o primo campo, trap, dismiss con back/scrim, focus di ritorno (vedi Operable).

- **medium** Il FAB cambia significato con la pagina (prodotto su Spesa, azione su Da fare) ma non ha un contratto di `contentDescription` per pagina (`EXPERIENCE.md §4.6 L138`, `DESIGN.md §6 L243`); il mockup ha "Aggiungi cosa da fare" solo sulla pagina Da fare (`todo-page.html L640`). *Fix:* descrizione esplicita "Aggiungi prodotto"/"Aggiungi cosa da fare" e stato; non affidarsi al contesto.

- **medium** La lettura dei badge quantità non è normalizzata: l'esempio "due litri" (`§7 L223`) è giusto, ma il badge visivo è "2 l" e non è detto che ogni badge abbia una `contentDescription` estesa ("due litri", "duecentocinquanta grammi") né che le unità abbreviate siano espanse. *Fix:* mappare unità→testo parlato e applicarlo a tutti i badge; nascondere il testo visivo duplicato.

## 5. Cognitive and motor — adequate

Ho verificato riduzione del movimento, ampiezza dei target, raggiungibilità a una mano, chiarezza delle azioni distruttive, dipendenza dalla memoria, focus dopo la chiusura dei dialoghi, gestione del testo lungo, font scaling e truncation. Il modello è semplice e la lista è scansionabile; i problemi nascono dal movimento automatico delle righe e dall'editor a una riga.

### Findings

- **high** La spunta fa sparire la riga dalla posizione corrente dopo 300 ms e non c'è un undo immediato: per annullare bisogna aprire `Presi`/`Fatte` e ritrovarla (`EXPERIENCE.md §4.1 L97-100`, §4.7 L144-145). Per utenti con deficit motori/cognitivi un tocco accidentale sposta il bersaglio e richiede memoria e navigazione. *Fix:* mantenere la riga in place per qualche secondo con "Annulla spunta", oppure offrire l'azione "Annulla spunta" nella snackbar; in ogni caso non spostarla sotto il dito.

- **medium** Il testo dell'azione è a una riga (`DESIGN.md §7.8 L303`, `EXPERIENCE.md §4.9 L161`) e non ha limite di lunghezza; a font grande il campo mostra solo una porzione e l'utente non vede cosa sta scrivendo. Le righe in lista crescono, ma l'editor no. *Fix:* consentire un campo multilinea che normalizza a una riga al salvataggio, oppure contatore + limite + scorrimento orizzontale; garantire la visibilità del testo completo in modifica.

- **medium** L'autofocus sul campo apre la tastiera all'apertura del foglio (`DESIGN.md §7.8 L303`, `EXPERIENCE.md §9 L327`); con TalkBack/Switch il focus può essere rubato al titolo e ai chip, e con il foglio al 65% la tastiera copre i pulsanti. *Fix:* non autofocus se un screen reader/switch è attivo; focus al titolo, poi al campo su richiesta; `imePadding` e pulsanti sticky (vedi sezione 6).

- **low** Non c'è una strategia reduced motion: pager 240 ms, spostamento riga 300 ms, sheet 250 ms, onda vocale (`EXPERIENCE.md §4.6 L134`, §4.1 L98; `voice-sheet.html L89`). Sono durate brevi, ma chi ha disattivato le animazioni di sistema non ha una resa alternativa. *Fix:* rispettare `Settings.Global.ANIMATOR_DURATION_SCALE`/`LocalMotionDurationScale`, ridurre a dissolvenza o nessuna animazione; l'onda vocale può diventare un'icona statica.

- **low** Le azioni globali (Storico, Condividi, Impostazioni) stanno in alto, fuori dalla zona del pollice su schermi grandi, mentre il prodotto promette uso a una mano (`DESIGN.md §1 L167`, `EXPERIENCE.md §1 L24`). Non è una barriera WCAG, ma è un costo motorio. *Fix:* spostare le azioni secondarie in un menu raggiungibile dal basso o valutarne la posizione con test a una mano.

## 6. Android specifics — thin

Ho verificato edge-to-edge e gesture navigation, predictive back, IME per il testo azione, voce esterna via `RecognizerIntent`, dark theme, screen magnification, Switch Access e scaling del font di sistema. Il quadro è quello di un'app Compose corretta nelle intenzioni ma senza le integrazioni di piattaforma che rendono l'accessibilità reale.

### Findings

- **high** Non c'è una strategia edge-to-edge/insets: la bottom bar è a 84px e il voice FAB sporge di 24px (`active-list.html L357-389`), il FAB di Da fare è a 28px dal fondo (`todo-page.html L420-436`), l'header è fisso a 64dp. Con gesture nav/3-button nav e cutout, contenuti e target possono finire sotto le barre di sistema o sotto la gesture handle. *Fix:* `enableEdgeToEdge`, `WindowInsets.systemBars`/`displayCutout`/`ime`, padding inferiore ≥48dp sopra la nav, test su API 21+ e Android 16.

- **high** Il supporto Dynamic Type è dichiarato "scala linearmente" (`EXPERIENCE.md §7 L232`), ma da Android 14 il font scaling è non lineare oltre una soglia; inoltre l'header ha altezza fissa 64dp (`DESIGN.md L81, L120`), il titolo nei mockup è `nowrap` (`todo-page.html L84-103`) e i badge sono 10–12px. A 200% il layout non è definito e il titolo non può andare a capo nei mockup. *Fix:* usare `sp`, testare 200% (e 130%/150%) su 320dp, rimuovere `nowrap`, permettere header a più righe e overflow; non promettere scaling lineare.

- **high** La tastiera non è considerata: il foglio azione è al 65–68% (`EXPERIENCE.md §9 L327`, `action-sheet.html L290-305`) con autofocus e pulsanti in fondo, ma non c'è `imePadding`/`BringIntoView` né una regola per lo scroll con tastiera aperta. A font grande i campi e i pulsanti possono essere coperti. *Fix:* `Modifier.imePadding()`, `bringIntoViewRequester` sul campo attivo, pulsanti sticky sopra la tastiera, test con Gboard e font scale 200%.

- **medium** Il dark theme non ha una policy di attivazione (sistema vs impostazione) né mockup scuri (`DESIGN.md L191-192`; `EXPERIENCE.md` non ne parla). Senza, non si può verificare il contrasto scuro e l'utente non sa cosa aspettarsi. *Fix:* seguire `isSystemInDarkTheme()` (o offrire una scelta in Impostazioni), aggiungere mockup scuri e testare i token scuri.

- **medium** Switch Access non è menzionato; il modello a due nodi per riga, lo swipe-only e lo spostamento delle righe rendono la scansione lunga e instabile (`EXPERIENCE.md §7` copre solo TalkBack). *Fix:* test con Switch Access, focus order lineare, azioni esposte come controlli, nessun gesto obbligatorio; mantieni la riga ferma durante la scansione.

## 7. Mockup fidelity — thin

Ho confrontato gli 8 mockup con le regole delle spine, distinguendo i problemi del mockup da quelli del contratto. I mockup sono dichiarati non normativi ("spines win on conflict"), ma restano la reference di composizione: quando mostrano un pattern inaccessibile o un'informazione mancante, il rischio è che venga copiato. I problemi mockup-only sono marcati come tali.

### Findings

- **high** Nei mockup i controlli non hanno semantica: le checkbox sono `div` senza `role`/`aria-checked` (`active-list.html L513-515`, `todo-page.html L507-511`), le scelte di condivisione sono `div` senza `role=radio` (`share-dialog.html L495-520`), i chip sono `<button>` senza `aria-pressed` (`action-sheet.html L592-607`). Sono mockup, ma sono l'unica anatomia disponibile e verranno copiati. *Fix:* aggiungere la semantica ARIA ai mockup o una scheda "semantics checklist" accanto; in Compose, mappare 1:1.

- **high** Le righe dei mockup omettono il badge urgenza in più punti (`action-sheet.html L540-545, L563-569`; `share-dialog.html L462-484`; `conclude-dialog.html L517-524`), mentre la spine lo rende obbligatorio (`SPEC.md CAP-5 L32`, `DESIGN.md §7.7 L295`, `EXPERIENCE.md §4.7 L142`). Per l'accessibilità è un testo mancante: l'urgenza non è percepibile né annunciabile. *Fix:* aggiungere il badge a ogni riga o marcare i mockup come non normativi e correggere la composizione.

- **medium** Contrasto e dimensioni nei mockup: badge a 10px (`todo-page.html L291-299`), "CONSIGLIATO" bianco su `#16A34A` 3,30:1 (`duplicate-dialog.html L178-187`), "Interpretazione completata" `#16A34A` su bianco 3,30:1 (`voice-sheet.html L199-209`). Sono testi informativi, non decorativi. *Fix:* badge ≥12sp, verde più scuro o testo scuro su verde chiaro; usare `accent-success #15803D` (5,02:1) invece di `#16A34A`.

- **medium** Nessun mockup mostra lo stato focus (niente `:focus-visible`), né per tastiera/D-pad né per Switch; gli unici stati sono default/selected/disabled. *Fix:* aggiungere un frame "focus" per header, riga, checkbox, chip, FAB e dialoghi, con il token `focus-ring`.

- **medium** I titoli e i conteggi dei mockup divergono dalla spine: "Spesa" + badge "4 DA PRENDERE" invece di "Spesa (4)" (`active-list.html L469-474`), "Da fare" + "4 AZIONI • 2 FATTE" invece di "Da fare (4)" (`todo-page.html L465-470`), e i conteggi non corrispondono alle righe (5 prodotti attivi vs 4; action-sheet 5 vs 4). Per TalkBack i conteggi sono informazioni portanti e duplicati. *Fix:* allineare titolo, badge e conteggi; un solo punto di annuncio del conteggio.

- **medium** Non esistono mockup dark, benché il dark mode sia dichiarato (`DESIGN.md L191-192`); non si può verificare il contrasto delle superfici scure né l'aspetto di badge, checkbox e barre. *Fix:* produrre almeno `active-list-dark` e `action-sheet-dark`, o rimuovere il dark mode dal contratto.

- **low** Le selezioni nei chip e nelle scelte di condivisione sono solo riempimento/bordo, senza spunta (`action-sheet.html L411-427`, `share-dialog.html L305-338`); la differenza di luminanza è forte ma manca la ridondanza non cromatica. *Fix:* aggiungere una spunta/icona di stato al selezionato.

## Accessibility notes

### Contrasto delle combinazioni portanti

Ratio calcolati sui valori hex dei token (sRGB, formula WCAG 2.x). "4,5 / 7" indica il target testo AA e il floor dichiarato dalla spine; "3,0" è il target non-testo di 1.4.11. Le righe dark con `surface-subtle` sono il caso che si produce se i token chiari vengono usati in dark mode senza una coppia scura.

| Combinazione | Ratio | Soglia | Esito |
|---|---:|---|---|
| `ink-primary` / `surface-base` | 17,85:1 | 4,5 / 7 | ✅ |
| `ink-secondary` / `surface-base` | 7,58:1 | 4,5 / 7 | ✅ |
| `ink-secondary` / `surface-subtle` | 7,24:1 | 4,5 / 7 | ✅ |
| `ink-muted` / `surface-base` | 2,56:1 | 4,5 | ❌ barrato/disabilitato |
| `ink-muted` a 45% / `surface-base` | ~1,46:1 | 4,5 | ❌ Presi/Fatte |
| `urgency-urgent-ink` / `urgency-urgent-bg` | 6,37:1 | 4,5 / 7 | ✅ AA, ❌ floor 7 |
| `urgency-relaxed-ink` / `urgency-relaxed-bg` | 9,45:1 | 4,5 | ✅ |
| `urgency-exceptional-ink` / `urgency-exceptional-bg` | 7,15:1 | 4,5 / 7 | ✅ |
| `area-badge` ink / bg | 7,24:1 | 4,5 | ✅ |
| `badge-ink` / `badge-bg` | 17,85:1 | 4,5 | ✅ |
| `accent-success` / bianco | 5,02:1 | 4,5 / 7 | ✅ AA, ❌ floor 7 |
| `accent-highlight` / bianco | 5,17:1 | 4,5 / 7 | ✅ AA, ❌ floor 7 |
| placeholder `#94A3B8` / bianco | 2,56:1 | 4,5 | ❌ |
| hint `#64748B` / `#F1F5F9` | 4,34:1 | 4,5 | ❌ |
| bordo chip `#CBD5E1` / bianco | 1,48:1 | 3,0 non-testo | ❌ |
| bianco / `#16A34A` (CONSIGLIATO) | 3,30:1 | 4,5 | ❌ mockup |
| `ink-primary-dark` / `surface-base-dark` | 18,33:1 | 4,5 / 7 | ✅ |
| `ink-secondary-dark` / `surface-base-dark` | 7,48:1 | 4,5 / 7 | ✅ |
| `border-crisp-dark #64748B` / base | 4,03:1 | 3,0 | ✅ |
| `border-crisp-dark #475569` / base | 2,53:1 | 3,0 | ❌ |
| `border-crisp-dark #475569` / `surface-card-dark` | 2,26:1 | 3,0 | ❌ |
| `ink-primary-dark` / `surface-subtle` chiaro (stale) | 1,00:1 | 4,5 | ❌ |
| `ink-secondary-dark` / `surface-subtle` chiaro (stale) | 2,45:1 | 4,5 | ❌ |
| `badge-bg` / `surface-base-dark` | 1,07:1 | 3,0 forma | ❌ |
| `accent-action` / `surface-base-dark` | 1,07:1 | 3,0 forma | ❌ |
| `urgency-urgent-ink-dark` / bg | 11,70:1 | 4,5 | ✅ |
| `urgency-relaxed-ink-dark` / bg | 9,85:1 | 4,5 | ✅ |
| `urgency-exceptional-ink-dark` / bg | 10,34:1 | 4,5 | ✅ |
| `accent-highlight-dark` / base | 7,54:1 | 4,5 | ✅ |

Nota: per `badge-bg` e `accent-action` in dark il testo bianco resta leggibile (17,85:1); il fallimento è la forma del controllo, che sparisce sul fondo. È comunque un problema di 1.4.11 quando il bordo è l'unico segnale del controllo.

### Inventario dei target

| Elemento | Dichiarato | Mockup | Esito |
|---|---|---|---|
| Icone header (Storico, frecce, Condividi, Impostazioni) | 48dp | 44×44 | ❌ |
| Checkbox prodotto (area di tocco) | 48dp | 48×48 in `active-list` | ✅; glifo 24 |
| Checkbox azione (area di tocco) | 48dp | 40×40 | ❌; glifo 23 |
| Chip urgenza/ambito | 48dp | 38dp | ❌ |
| Frecce pager | 48dp | 44×44 | ❌ |
| Voice FAB | 64dp | 68×68 | ✅ |
| FAB (+) | 64dp (prosa) | 64×64 | ✅; token assente |
| Quick add | 48dp | 48×48 | ✅ |
| Matita categoria | 48dp | 28×28 | ❌ |
| Pulsanti dialogo | 48dp | 50–54 | ✅ |
| Annulla duplicati | 48dp | 44 | ❌ |
| Snackbar "Annulla" | 48dp | n/d | non specificato |
| Storico "Aggiungi"/"Indietro" | 48dp | 48 | ✅ |

Il target è l'area di tocco, non il glifo: `DESIGN.md L89` dà 26px alla checkbox e `L251` parla di padding 48dp, ma questa distinzione non è ripetuta nei component token e i mockup mostrano il contrario.

### Inventario della semantica

| Elemento | Semantica attesa | Stato nel contratto |
|---|---|---|
| Titolo pagina | heading + conteggio | ✅ heading dichiarato (`DESIGN.md L291`); il mockup duplica il conteggio nel badge |
| Frecce pager | button, label, stato disabilitato | ✅ label esplicite; ⚠️ conflitto "disabilitato ma annunciato" |
| Storico/Condividi/Impostazioni | button + label | ✅ nei mockup |
| Header categoria/ambito | heading + conteggio | ❌ non heading |
| Riga prodotto/azione | container/button + checkbox | ⚠️ due nodi, ruolo della riga non commesso |
| Checkbox | checkbox, checked/unchecked, label | ⚠️ stato dentro il testo, niente `toggleable`/`stateDescription` |
| Badge urgenza/ambito | testo, prefisso "Urgenza:"/"Ambito:" | ✅ ma duplicato rispetto alla label della riga |
| Header Presi/Fatte | button, expanded/collapsed, heading, count | ⚠️ testo di annuncio sì, semantica no |
| FAB | button + contentDescription per pagina | ⚠️ solo il mockup Da fare la dichiara |
| Bottom sheet | dialog/paneTitle, focus trap, dismiss | ❌ |
| Dialog | dialog/paneTitle, focus trap, dismiss | ❌ |
| Chip | radiogroup/radio, selected, group label | ❌ |
| Snackbar | live region + action | ❌ |
| Stato/trascrizione vocale | live region polite | ❌ |
| Anteprima/errori import | region/heading + live region | ❌ |
| Chevron, pallino categoria, onda | decorativi | ⚠️ nessuna regola di esclusione |

### Caveat di piattaforma

- **TalkBack:** lo swipe orizzontale a un dito è riservato alla navigazione del focus; ogni azione swipe-only richiede una custom action. Il pager ha le frecce, il cestino no.
- **Switch Access:** richiede scansione lineare e stabile; i due nodi per riga e lo spostamento a 300 ms vanno testati, e nessuna azione deve richiedere un gesto.
- **D-pad/tastiera:** serve un focus ring esplicito; il ripple di Compose non è un indicatore di focus sufficiente, e i controlli disegnati con `pointerInput` non sono automaticamente focusabili.
- **Font scaling:** Android 14+ usa scaling non lineare; testare 130%, 150% e 200% a 320dp, con `sp`, `maxLines` e overflow espliciti.
- **Edge-to-edge/gesture nav:** insets di sistema, cutout, `systemGestureExclusion` e predictive back vanno progettati insieme, non dopo.
- **IME:** `imePadding` e `bringIntoViewRequester` sul campo attivo; i pulsanti del foglio devono restare sopra la tastiera.
- **Dark:** `isSystemInDarkTheme`, coppie scure complete e mockup; verificare 1.4.11 sui bordi, non solo il testo.
- **Magnification:** header e bottom bar fissi riducono l'area utile; testare 200–300% e lo scroll sotto le barre.
- **Voce esterna:** `RecognizerIntent` può mancare o richiedere rete; il fallback manuale deve essere visibile e annunciato.
- **Haptics:** rispettare le impostazioni di sistema; l'haptic è ridondante, mai l'unico feedback.
- **Lingua:** locale italiana e `localeConfig`; verificare la pronuncia TalkBack di unità, "TOGO" e nomi prodotto stranieri.

### Metodo e limiti

La review valuta gli artefatti, non l'implementazione: i ratio sono calcolati sui token, i comportamenti TalkBack/Switch/Accessibility Scanner vanno verificati su device. I mockup sono trattati come reference di composizione, non come contratto, perché entrambe le spine dichiarano "spines win on conflict". Riferimenti di metodo: `validate.md` e `design-md-spec.md`; `experience-md-spec.md` non è presente nel workspace della skill.
