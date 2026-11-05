# Prose Review — TOGO

## Overall verdict

La coppia è leggibile e in più punti precisa, ma il copy non è ancora un contratto: la stessa stringa o la stessa regola di copy compare in due o tre varianti, i termini per l'oggetto «azione» cambiano anche dentro lo stesso paragrafo e alcune regole portanti si possono leggere in due modi. Per un architetto o uno story-dev il rischio non è capire l'intenzione, ma scegliere quale delle versioni implementare.

## 1. Clarity and unambiguity — thin

Ho letto le regole di copy e comportamento cercando riferiti non definiti, quantificatori vaghi e norme con due letture possibili. La prosa è chiara a livello di frase; le ambiguità sono concentrate nelle regole che legano copy, conteggi e stati.

### Findings

- **critical** La clausola finale rende non vincolante tutto il microcopy citato: *«Il microcopy esatto può essere rifinito in build senza cambiare i comportamenti descritti»* (`EXPERIENCE.md §9 L331`). Un consumatore non sa quali stringhe siano contract e quali indicative, mentre §3 e §4 le presentano come esatte. *Fix:* dichiarare vincolanti le stringhe quotate (salvo un elenco esplicito di «copy provvisorio») e rimuovere o restringere la clausola.
- **high** La conclusione della spesa ammette due interazioni incompatibili: *«Mostra un dialog rapido o snackbar di conferma»* (`EXPERIENCE.md §4.3 L111`). Un dialog conferma, una snackbar informa o annulla; non sono equivalenti per un'azione che sposta articoli nello storico. *Fix:* scegliere un solo pattern, con titolo, corpo e due azioni definite.
- **high** La disponibilità di «Entrambe» nella condivisione si contraddice: *«Le opzioni con una parte vuota sono disabilitate»* (`§4.10 L169`) contro *«l'opzione `Entrambe` resta disponibile solo se almeno una parte ha voci»* (`§5 L194`). «Almeno una» consente una condivisione parziale che §4.10 vieta. *Fix:* scrivere la matrice scelta × parti vuote in una riga sola e allineare stato, default e copy.
- **high** Il copy dell'import con una parte a zero non è definito: *«Se una parte è zero, la frase si adatta»* (`§3 L86`) contro *«La conferma mostra `0 prodotti` e N azioni; l'esito non parla di prodotti se non ce ne sono»* (`§5 L195`). «Si adatta» può significare omettere la parte, dire zero o cambiare il plurale; le due frasi non dicono se valgono per anteprima ed esito. *Fix:* fornire i template per i quattro casi (solo prodotti, solo azioni, entrambe, zero) e separare anteprima da esito.
- **high** «Sessione» e «di recente» non sono definiti: *«gli articoli spuntati durante la sessione corrente»* (`DESIGN.md §7.3 L263`), *«nella sessione»* (`EXPERIENCE.md §4.2 L105`), *«se ci sono elementi presi di recente»* (`§4.2 L106`). Non è chiaro se i Presi sopravvivono al riavvio dell'app o a un cambio pagina, né per quanto tempo. *Fix:* definire la sessione come «dall'ultima Concludi spesa» e sostituire «di recente» con una condizione verificabile (N > 0).
- **high** La compatibilità delle unità nella somma non è definita: *«somma algebrica se le unità sono compatibili, es. 1 l + 1 l = 2 l»* e *«unità incompatibili (es. 1 bottiglia vs 500 ml)»* (`§4.5 L127-129`). «Bottiglia» non è un'unità e l'esempio non dice se `1 l + 500 ml` sia compatibile, né chi converte. La nota esplicativa non ha testo. *Fix:* elencare le unità convertibili, definire la regola di conversione e scrivere la nota (es. «Unità diverse: scegli Sostituisci o Crea voce separata»).
- **medium** Lo stato del primario dopo un duplicato è descritto al contrario: *«Il primario resta disabilitato finché la combinazione non cambia»* (`§5 L192`), ma l'errore si produce toccando un primario che era attivo. *Fix:* «Dopo l'errore il primario si disabilita finché testo, urgenza o ambito non cambiano».

## 2. Consistency of terminology and naming — thin

Ho tracciato i nomi dell'oggetto «cosa da fare», della pagina, della lista e delle azioni attraverso le due spine e i mockup. La nomenclatura è la parte più fragile: non esiste un glossario e lo stesso concetto cambia nome anche a distanza di una frase.

### Findings

- **high** Non c'è un termine canonico per la voce della pagina Da fare: *«Nessuna cosa da fare. Tocca + per aggiungere un'azione»* (`EXPERIENCE.md §3 L77`) usa due nomi nella stessa frase; poi compaiono *«attività spuntate»* (`§3 L82`), *«azioni»* (`§4.7`), *«voci»* (`§5 L200`), *«articoli»* (`§3 L71`). Un implementatore non sa se la stringa utente debba dire cosa, azione o attività. *Fix:* glossario con «azione» = voce della lista Da fare, «fatta» = azione spuntata; riservare «cosa da fare» al nome della feature e «attività» alla sola etichetta «Concludi attività».
- **high** La feature ha almeno cinque nomi utente: «Da fare» (`§2.1 L40`), «Cose da fare» (`§3 L76`), «lista da fare» (`Flow 7 L293`), «Solo azioni» (`§4.10 L169`), `[DA FARE]` (`§4.11 L176`). Non è chiaro quale sia il nome del prodotto e quale un'etichetta di formato. *Fix:* fissare «Cose da fare» come nome della feature, «Da fare» come titolo della pagina, «azione» come voce, e dichiarare `[DA FARE]` un token di formato.
- **high** «Lista attiva» cambia referente dopo l'introduzione del pager: è la pagina Spesa in `§2.1 L39` («Spesa (Lista Attiva)»), e `DESIGN.md §7.3 L263` la usa per la sola lista della spesa anche quando esistono due liste attive (`EXPERIENCE.md §4.3 L114`, `§4.5 L126`). *Fix:* usare «lista della spesa» e «lista delle cose da fare»; se «attiva» significa «non spuntata», definirlo una volta e non usarlo come nome di superficie.
- **medium** «Ambito» e «area/aree» convivono: *«Aree nell'ordine Casa, Pratiche…»* (`§4.10 L171`), *«con aree, urgenze e righe indentate»* (`Flow 8 L304`), mentre il campo si chiama «Ambito» (`§4.9 L161`, `action-sheet.html L600`). *Fix:* «ambito» ovunque; se il formato condiviso usa «area», dichiararlo alias una volta.
- **medium** Lo stato spuntato ha tre verbi: «spuntato» (`§4.7 L143`), «barrato» (`§4.7 L144`), «depennato» (`DESIGN.md §2 L181`). «Barrato» è il trattamento visivo, non lo stato. *Fix:* «spuntata» per lo stato, «barrata» per il rendering; eliminare «depennato».
- **medium** I nomi delle azioni non sono stabili: «Condividi» (icona), «Condivisione» (superficie), «Condividi» (primario); «Aggiungi» (`history.html L307`), «+ Aggiungi» (`Flow 5 L281`), «+ Riaggiungi» (`§2.1 L48`). *Fix:* una tabella `controllo | etichetta | contesto` e una sola etichetta per il pulsante dello storico.
- **low** I nomi di categoria e chrome divergono tra spine e mockup: «Banco Frigo» / «Banco Frigo / Latticini» / «Banco Frigo/Macelleria»; «Igiene/Cura Casa» (`§6 L215`) / «Pulizia casa» (`history.html L335`); «intestazione» / «App Bar» / «top bar». *Fix:* usare i nomi del catalogo o marcare gli esempi come illustrativi.

## 3. Voice and register — adequate

Ho verificato persona, imperativi, tono editoriale e antropomorfismo, distinguendo la voce editoriale ammessa in DESIGN dalla prosa diretta richiesta in EXPERIENCE. La voce è sobria e riconoscibile; le incrinature sono locali ma visibili nel copy.

### Findings

- **medium** EXPERIENCE scivola nel registro promozionale e nella narrativa: *«la forma d'onda cattura l'audio»*, *«l'interpretazione è perfetta»* (`§8 Flow 1 L242-243`), *«scivola fluidamente»* (`§4.1 L98`), *«in due tocchi»* (`Flow 8 L305`), *«ordine naturale delle corsie»* (`Flow 2 L248`). Sono giudizi non osservabili e non regole. *Fix:* sostituire con esiti verificabili («la trascrizione compare entro 0,8 s», «la riga aggiornata mostra 2 l»).
- **medium** Le domande di conferma cambiano persona: infinito in *«Concludere la spesa?»* (`§3 L71`) e *«Concludere le attività?»* (`§3 L82`), seconda persona in *«Cosa vuoi fare?»* (`§3 L70`), *«Cosa vuoi condividere?»* (`§3 L84`), prima persona in *«Ricordo questa scelta…»* (`§3 L69`). *Fix:* scegliere una forma (consiglio: «Vuoi concludere la spesa?», «Vuoi condividere…?») e applicarla a tutti i dialoghi.
- **medium** Il prompt di apprendimento è antropomorfo e le risposte non concordano: *«Ricordo questa scelta per i prossimi inserimenti?»* con `[Ricorda]` / `[Solo per ora]` (`§3 L69`). La domanda è in prima persona, la risposta è un imperativo rivolto all'app. *Fix:* «Vuoi che ricordi questa scelta per i prossimi inserimenti?» `[Sì, ricorda]` / `[No, solo per ora]`.
- **medium** I verbi per l'interazione tattile non sono coerenti: «Tocca» (`§3 L73`), «premi +» (`§3 L73`), «swipa» (`Flow 6 L286`), «Tap» (`§4.1 L95`), «Doppio tocco» (`§7 L227`). *Fix:* «tocca» per i controlli, «scorri» per il gesto, «doppio tocco» nelle stringhe TalkBack; evitare «swipa».
- **low** DESIGN può avere voce editoriale, ma alcuni aggettivi diventano overclaim: *«massimo contrasto acromatico»* (`DESIGN.md §2 L175`) mentre la palette ha blu, verde e ambra; *«leggibilità impeccabile»* (`§1 L166`), *«inconfondibile»* (`§6 L243`). *Fix:* tenere la voce, togliere gli assoluti che contraddicono i token.

## 4. Microcopy quality — thin

Ho inventariato le stringhe visibili e lette dai lettori di schermo, verificando che ogni stato abbia una sola versione, che i pulsanti dicano cosa fanno e che i messaggi dicano cosa è successo e cosa fare. L'inventario non è ancora chiuso: mancano stringhe, altre hanno due o tre versioni.

### Findings

- **high** Lo stato vuoto della Spesa ha due copy diversi: *«La tua lista è vuota. Tocca il microfono o premi + per aggiungere prodotti»* (`§3 L73`) e *«Nessun prodotto da acquistare»* con *«pulsanti chiari per dettare a voce o digitare»* (`§5 L189`). Non è detto quale sia titolo, quale corpo e quali siano le CTA. *Fix:* definire `titolo | corpo | CTA` in un'unica riga e usare la stessa stringa in §5.
- **high** Esiste un copy per uno stato che non può verificarsi: *«Sezione vuota: `Nessuna attività conclusa.` La sezione Fatte non compare se N = 0»* (`§3 L83`; cfr. `§4.8 L157`). Se la sezione non compare, la stringa non è raggiungibile. *Fix:* eliminarla oppure decidere che la sezione resti visibile vuota e allineare §4.8/§5.
- **high** Il pulsante di conferma dell'import compare solo in un flusso: *«Mike tocca `Accetta`»* (`§8 Flow 9 L312`), ma §3 non definisce l'etichetta e DESIGN §7.12 non la nomina. *Fix:* aggiungere «Importa» (o «Conferma import») all'inventario e usarla nel flusso.
- **high** Il dialogo duplicati ha tre versioni e un referente indefinito: §3 L70 *««Latte parzialmente scremato» è già presente con 1 l nella categoria Banco Frigo»*; Flow 4 L273 *«Il prodotto è già presente con 1 l»*; DESIGN §7.5 L278 chiede *«quantità attuale e nuova»*. «Il prodotto» non dice quale, e la nuova quantità manca nel corpo di §3. *Fix:* un template unico con `{prodotto}`, `{quantità attuale}`, `{categoria}`, `{nuova quantità}` e la stessa stringa in flusso, §3 e mockup.
- **medium** Il titolo del dialogo di condivisione è duplicato o invertito: §3 L84 dice *«titolo `Cosa vuoi condividere?`»*, il mockup mostra titolo *«Condividi»* e sottotitolo *«Cosa vuoi condividere?»* (`share-dialog.html L491-492`). *Fix:* scegliere una gerarchia e riportarla in §3 e nel mockup.
- **medium** *«Nessuna voce»* (`§4.10 L169`) non spiega perché l'opzione è disabilitata, mentre §7 L230 promette che *«Le scelte disabilitate spiegano perché»*. *Fix:* «Nessun prodotto» / «Nessuna azione» oppure «Niente da condividere in questa lista».
- **medium** *«Sostituisci quantità (1 l)»* (`duplicate-dialog.html L278`) non dice con cosa sostituisce; nell'esempio quantità attuale e nuova sono entrambe 1 l, quindi l'effetto è indistinguibile. *Fix:* «Sostituisci con 1 l» e usare un esempio con quantità diverse.
- **medium** La conferma vocale è telegrafica e un po' innaturale: *«Aggiunto: 2 l di Latte parzialmente scremato in Banco Frigo»* (`§3 L68`). «In Banco Frigo» non è idiomatico e la maiuscola di «Latte» è isolata. *Fix:* «Aggiunto: latte parzialmente scremato, 2 l, in Banco Frigo» o «2 l di latte… aggiunti a Banco Frigo».
- **medium** Le tre urgenze non sono glossate e «Eccezionale» è semanticamente ambiguo: in italiano suona come «più importante», ma nell'ordine è ultima dopo «A tempo perso» (`SPEC.md L54`; `§4.9 L161`). *Fix:* una riga di definizione per ciascuna (quando sceglierla) o un helper nel foglio; confermare che l'ordine sia quello voluto.
- **medium** Le stringhe TalkBack contengono un placeholder e un'istruzione ineseguibile: *«Sezione presi, 2 articoli, espansa/collassata»* (`§7 L224`) va letta scegliendo uno stato, non entrambi; *«Swipe a sinistra per eliminare»* (`§7 L227`) istruisce un gesto che TalkBack intercetta. *Fix:* stringhe condizionali e un'azione accessibile annunciata (es. «Elimina, azione disponibile»).
- **medium** Il suggerimento delle sezioni collassabili è statico: *«Tocca per nascondere»* (`active-list.html L603`, `todo-page.html L599`, `conclude-dialog.html L534`) resta anche quando la sezione è chiusa. *Fix:* «Tocca per mostrare/nascondere» in base allo stato, o rimuovere il suggerimento.
- **low** I conteggi dentro le CTA divergono dalla spine e tra loro: *«Concludi Spesa (Archivia 2)»* (`active-list.html L641`), *«Concludi attività (2)»* (`todo-page.html L635`), mentre DESIGN §7.10 L319 definisce `Concludi attività` senza conteggio. *Fix:* togliere il conteggio dalle etichette e lasciarlo nel corpo del dialogo.
- **low** «Azione già presente» compare con e senza punto (`§4.7 L146`, `§3 L81`, `§5 L192`); «Nessuna voce» non ha punteggiatura mentre le frasi vicine sì. *Fix:* uniformare prima del final.

## 5. Redundancy and verbosity — adequate

Ho cercato stringhe ripetute, prosa che ricopia tabelle e frasi lunghe con incisi che non aggiungono una decisione. La coppia non è prolissa, ma la ripetizione del copy senza una sede canonica è la principale fonte di deriva.

### Findings

- **medium** Lo stesso copy è riscritto in §3, §4, §5, nei flussi e in DESIGN, con varianti: la conclusione compare in `§3 L82`, `§4.8 L155-156`, `§5 L193`, `DESIGN §7.9-7.10 L313-320`, `Flow 7 L297-298`; la condivisione in `§3 L84-85`, `§4.10 L169-172`, `§5 L194`. Non c'è una stringa sorgente. *Fix:* un inventario `chiave | stringa | contesto | note` in §3 e rimandi dalle altre sezioni.
- **medium** La tabella tipografica ricopia i valori già presenti nei `note` del frontmatter: per esempio `{typography.title-screen}` è definito a `DESIGN.md L49-50` e ripetuto a `L202` come «Headline Medium, 24sp, SemiBold». Due fonti per gli stessi numeri. *Fix:* lasciare nella tabella solo ruolo e uso, rimandando al token.
- **medium** Alcune frasi accumulano tre o quattro subordinate e diventano più lunghe del necessario: *«Ergonomia ad una mano: target touch ampi…, contrasto massimo tra elementi da prendere ed elementi presi, badge quantità evidenti e scansionabili in frazioni di secondo mentre si spinge il carrello»* (`DESIGN.md §1 L167`). *Fix:* spezzare in tre regole e sostituire «elementi da prendere ed elementi presi» con «voci da comprare e voci spuntate».
- **low** La precisione al decimale non cambia decisioni: «15ms», «300ms», «0.8s», «240 ms», «45%», «65%» (`EXPERIENCE.md L96, L98, L134, L242, L327`; `DESIGN.md L267`). Metà sono anche in §9. *Fix:* tenere solo i valori che vincolano il comportamento e spostare gli altri tra i default di build.
- **low** Le stesse regole negative sono ripetute come formule: «nessun annulla e nessuno storico» in `§3 L82`, `§4.8 L156`, `§5 L193`, `§7 L231`, `Flow 7 L298`, `DESIGN §7.9 L314`; «non sono prodotti» in `DESIGN §1 L169`, `§7.7 L299`, `§8 L351`, `EXPERIENCE §4.7 L148`. *Fix:* una sede canonica per ciascuna e rimandi brevi.

## 6. Grammar, punctuation, and Italian conventions — adequate

Ho riletto accenti, apostrofi, virgole, due punti, trattini, virgolette e formato di numeri e unità. Non ci sono errori che impediscano la comprensione; ci sono però calchi dall'inglese e incoerenze tipografiche che un copy editor correggerebbe prima del final.

### Findings

- **medium** *«Ordine: dalla più recente in cima»* (`EXPERIENCE.md §4.8 L154`) mescola due costruzioni. *Fix:* «la più recente in cima» oppure «dalla più recente alla più vecchia».
- **medium** *«L'azione in modifica non è duplicato di sé»* (`§4.9 L163`): «azione» è femminile, «duplicato» maschile. *Fix:* «L'azione in modifica non conta come duplicato di sé stessa».
- **medium** Il genere di «snackbar» oscilla: *«mostra la snackbar»* (`§4.7 L147`) ma *«mostra uno snackbar»* (`DESIGN.md §7.11 L324`) e *«uno snackbar»* (`EXPERIENCE.md §5 L194`). In italiano l'uso prevalente è femminile. *Fix:* «la snackbar» ovunque, o «il messaggio temporaneo».
- **medium** Numeri e unità non seguono una convenzione unica: «15ms», «300ms», «0.8s» (`EXPERIENCE.md L96, L98, L242`) contro «240 ms» (`L134`); «48x48dp» (`L206`) contro «48dp»; «1400+ prodotti» (`§2.1 L43`) contro «1.400 voci» (`§5 L200`). *Fix:* «15 ms», «300 ms», «0,8 s», «48×48 dp», «1.400» sempre.
- **low** Le etichette dei pulsanti hanno tre notazioni: backtick (`§4.8 L155`), parentesi quadre `[Ricorda]` (`§3 L69`), virgolette dritte (`DESIGN.md §8 L349`). Le quadre si confondono con il token letterale `[DA FARE]`. *Fix:* backtick per tutte le stringhe di UI e una nota che dica che le quadre non fanno parte del copy.
- **low** La maiuscola dopo i due punti non è uniforme: *«Supporto Dark Mode:»* (`DESIGN.md L191`), *«Scansione visiva ad alto contrasto: Ordinamento…»* (`EXPERIENCE.md §6 L215`), mentre altrove la frase continua minuscola. *Fix:* minuscola dopo i due punti, salvo nomi propri o frasi autonome.
- **low** Due frasi usano «sente» per l'haptic: *«sente la vibrazione tattile»* (`Flow 2 L249`), *«sente il feedback aptico»* (`Flow 10 L321`). Il feedback si percepisce, non si sente. *Fix:* «percepisce»; e uniformare «haptic feedback» / «feedback aptico» / «vibrazione».

## 7. Tone across states — adequate

Ho confrontato errori, azioni distruttive, successi e stati vuoti. Il tono è generalmente calmo ed esplicito; le incrinature stanno nella ripetizione degli avvisi irreversibili e in qualche formula burocratica o allarmistica.

### Findings

- **medium** L'irreversibilità è ripetuta due volte nello stesso dialogo: *«Verranno eliminate N attività spuntate. L'operazione non si può annullare»* (`§3 L82`) e poi *«Nessun annulla e nessuno storico dopo la conferma»* (`conclude-dialog.html L568`; cfr. `DESIGN.md §7.9 L314`). «Nessun annulla» tratta l'etichetta di un pulsante come un sostantivo. *Fix:* una sola frase: «Le attività eliminate non potranno essere recuperate».
- **medium** «Concludi attività» è ambiguo in italiano: si legge anche come «termina l'attività» prima che come «elimina le azioni spuntate». Il chiarimento arriva solo nel dialogo, dopo il tap. *Fix:* mantenere l'etichetta se è vincolata da SPEC, ma anteporre nel dialogo «Eliminare le 2 attività spuntate?» e usare «azioni» nel corpo.
- **medium** Il registro dei successi non è allineato agli stati vuoti: gli empty sono caldi (*«La tua lista è vuota»*, `§3 L73`) mentre le conferme sono telegrafiche (*«Aggiunto: 2 l di…»*, `§3 L68`) e i flussi usano frammenti (*«Conferma.»*, `Flow 7 L297`). *Fix:* una regola: frase completa e concreta per i successi, due frasi brevi per gli empty; niente frammenti nel copy.
- **low** Le formule negative si accumulano: *«Non c'è nulla da condividere»* (`§3 L85`), *«Nessuna voce»* (`§4.10 L169`), *«Nessun annulla»* (`conclude-dialog.html L568`). Sono corrette ma rendono il tono più secco del necessario. *Fix:* alternare una formulazione costruttiva («La lista è vuota», «Niente da condividere in questa lista») senza cambiare il significato.
- **low** Il duplicato è trattato come errore: *«Se esiste già, compare un errore»* (`action-sheet.html L610`) e il dialogo usa un triangolo di avviso (`duplicate-dialog.html L246-251`) per una scelta non distruttiva. *Fix:* «compare un avviso» e un'icona informativa; il triangolo resta per la sola conclusione.

## 8. Readability for a mixed audience — thin

Ho letto i documenti come farebbe un architetto o uno story-dev senza aprire il codice, verificando se ogni termine è comprensibile e ogni frase regge senza il mockup. La struttura è leggibile, ma restano riferimenti al baseline e termini di piattaforma che richiedono conoscenze non dichiarate.

### Findings

- **high** «Dynamic Type» è terminologia iOS, non Android: compare in `DESIGN.md §4 L221-222`, `§7.6 L291`, `§8 L344` e `EXPERIENCE.md §7 L232`. Su Android il comportamento è la scalabilità del testo di sistema (`fontScale`), e un lettore può implementare l'API sbagliata o cercare una feature inesistente. *Fix:* «scalabilità del testo di sistema (font scale Android)» ovunque.
- **medium** Il documento non è autosufficiente quando dice «Invariato», «come oggi», «formato attuale»: `§2.1 L43, L45, L48, L49`, `§4.10 L171`, `§4.11 L175`. Chi non conosce l'app deve aprire `brownfield.md` per sapere cosa è invariato. *Fix:* una riga di sintesi per ogni superficie invariata, con il rimando alla fonte.
- **medium** Il gergo non è spiegato e a volte è impreciso: «somma algebrica» (`§4.5 L127`), «errore atomico» (`§4.11 L178`), «deduplica» (`§4.11 L179`), «chip» (`§4.9 L161`), «pager» (`§1 L26`), «local-first» (`§1 L27`). Per un architetto sono noti; per un copywriter o un QA non lo sono. *Fix:* un glossario iniziale con la forma italiana e, dove serve, il termine tecnico tra parentesi.
- **low** Le sigle non sono espanse: «FAB» (`§2.2 L55`), «L1/L2/L3» (`§4.11 L176`), «UJ-1», «CAP-1» (`§8`, titoli e fonti). *Fix:* espandere al primo uso («FAB, pulsante flottante»; «livelli di tassonomia L1/L2/L3») e rimandare a SPEC per UJ/CAP.
- **low** Alcune frasi sono troppo compresse per un lettore non tecnico: *«selettore numerico/unità immediato»* (`§5 L199`), *«micro-ritardo percepibile»* (`§4.1 L98`), *«Profondità Modale Massima»* (`§2.2 L57`). *Fix:* riscrivere in frase piana («un selettore per quantità e unità compare subito»; «una pausa breve, 300 ms»).

## Language notes

### Terminologia da fissare

| Concetto | Varianti osservate | Forma canonica proposta |
|---|---|---|
| Voce della pagina Da fare | azione, cosa da fare, attività, voce, elemento, «attività spuntata» | **azione** (voce); **fatta** (azione spuntata); «cosa da fare» solo nel nome della feature |
| Feature e pagina | Da fare, Cose da fare, lista da fare, lista azioni, `[DA FARE]`, «Solo azioni» | **Cose da fare** (feature); **Da fare** (titolo pagina); `[DA FARE]` (solo token di formato) |
| Lista della spesa | Spesa, lista, lista attiva, schermata attiva, lista della spesa | **lista della spesa**; **pagina Spesa**; evitare «lista attiva» come nome di superficie |
| Stato di spunta | spuntato, barrato, depennato, preso, fatto, concluso | **spuntata** (stato); **barrata** (trattamento visivo); **Fatte** (sezione) |
| Conclusione | Concludi spesa, Concludi Spesa, Archivia, Concludi attività, elimina | **Concludi spesa** / **Concludi attività** (etichette); «elimina» solo nel corpo esplicativo |
| Campo delle azioni | ambito, area, aree | **ambito**; «area» solo come alias dichiarato del formato condiviso |
| Urgenze | Urgente, A tempo perso, Eccezionale | etichette fisse; aggiungere una definizione d'uso per ciascuna |
| Condivisione | Condividi, Condivisione, share, Android Sharesheet, share sheet, chooser | **Condividi** (azione); **condivisione** (funzione); **foglio di condivisione Android** |
| Chrome superiore | intestazione, App Bar, top bar, page header | **intestazione di pagina** (visuale: `page-header`) |
| Feedback tattile | haptic feedback, feedback aptico, vibrazione secca, haptics | **feedback aptico** |
| Sezione richiudibile | collassabile, espandibile, pieghevole | **sezione collassabile** |
| Messaggio temporaneo | snackbar (femminile e maschile), messaggio | **la snackbar** |
| Prodotto | prodotto, articolo, voce | **prodotto**; «articolo» solo nel copy generico («N articoli presi») |

### Pattern ricorrenti

- **Riferimenti al baseline non risolti.** «Invariato», «come oggi» e «formato attuale» compaiono in IA, condivisione, import e aggiunta prodotto. Sono corretti come note di continuità, ma non bastano a un lettore che non ha il codice: ogni occorrenza va accompagnata da una riga che descrive il comportamento corrente.
- **Italiano e inglese mescolati senza regola.** «default», «tap», «swipe», «share», «form», «header», «chip», «bottom sheet», «pager», «local-first» convivono con i corrispondenti italiani. Alcuni sono termini tecnici legittimi; altri hanno un equivalente semplice. Scegliere una soglia: tecnicismi solo dove il termine italiano sarebbe ambiguo, e glossario per il resto.
- **Persone e modi verbali.** Il copy utente usa il «tu» («Tocca», «Cosa vuoi…»), le conferme usano l'infinito («Concludere…»), il prompt di apprendimento usa la prima persona («Ricordo…»). È una deriva, non una scelta: va fissata una voce.
- **Formattazione delle stringhe.** Le etichette sono a volte in backtick, a volte tra `[ ]`, a volte tra virgolette dritte. Le quadre si confondono con il token letterale `[DA FARE]`; le virgolette dritte con l'apostrofo. Usare backtick per il copy e «» solo nelle citazioni editoriali.
- **Conteggi senza template.** «Spesa (N)», «Da fare (N)», «Presi (N)», «Fatte (N)», «Solo spesa (N)», «Entrambe (N+M)» non hanno regole di singolare/plurale né di unità («N» di cosa). I mockup aggiungono «4 DA PRENDERE», «4 AZIONI», «2 FATTE», «2 VOCI», «1 VOCE». Serve una tabella di template: `{n} prodotto/i`, `{n} azione/i`, `{n} articolo/i`, e la regola per n = 0, 1, molti.
- **Punteggiatura del copy.** Le frasi brevi a volte finiscono con il punto, a volte no; i titoli dei dialoghi non lo hanno (corretto), le note disabilitate nemmeno. Va deciso per tipo di stringa: titolo senza punto, corpo con punto, etichetta di pulsante senza punto, messaggio di errore con punto.

### Inventario del copy: buchi e duplicati

- **Mancano**: etichetta del pulsante di conferma import (unico riferimento: «Accetta» nel Flow 9); testo della nota per unità incompatibili; messaggio per il vocale non disponibile; copy per la ricerca nello storico senza risultati; varianti per N = 0/1/molti; testo del messaggio «0 prodotti»; etichette delle azioni TalkBack alternative al gesto.
- **Doppioni con varianti**: stato vuoto Spesa (`§3 L73` vs `§5 L189`); dialogo duplicati (`§3 L70` vs `Flow 4 L273` vs mockup); titolo condivisione (`§3 L84` vs mockup); avviso di irreversibilità (`§3 L82` vs mockup); «niente da condividere» (`§3 L85` vs `Nessuna voce` vs brownfield «Nessun articolo da condividere»); pulsante storico (`Aggiungi` vs `+ Aggiungi` vs `+ Riaggiungi`).
- **Stringhe da correggere**: «Nessuna attività conclusa» per una sezione che non compare; «espansa/collassata» in un annuncio TalkBack; «Nessun annulla»; «Il primario resta disabilitato»; «Sostituisci quantità (1 l)» senza il valore nuovo; «Concludi attività (2)» e «Concludi Spesa (Archivia 2)» con conteggi non previsti dalla spine.

### Nota di metodo

Il riferimento `.agents/skills/bmad-ux/references/experience-md-spec.md` indicato nel prompt non esiste nel repository; la review si è basata su `SKILL.md`, su `references/design-md-spec.md` e sugli esempi in `assets/`.
