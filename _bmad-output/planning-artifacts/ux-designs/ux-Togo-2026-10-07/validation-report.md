# Validation Report — TOGO

- **DESIGN.md:** `_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md`
- **EXPERIENCE.md:** `_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md`
- **Run at:** 2026-11-05T12:00:00+01:00

## Overall verdict

La coppia è un contratto ricco e leggibile per la Spesa e per la nuova pagina Da fare: i riferimenti token risolvono, gli 8 mockup sono linkati e i componenti core hanno spec visiva e comportamentale. Non è però ancora source-extractable senza ambiguità: la condivisione ha tre regole incompatibili, il dark mode è dichiarato ma non tokenizzato, e alcuni flussi e stati chiave restano non committed. Il rischio downstream è concreto: un architetto o uno story-dev può implementare formati di share, stati vuoti e temi diversi da quelli voluti senza violare alcuna riga esplicita.

I reviewer di struttura, prosa e accessibilità confermano e aggravano il quadro: nessun punto d'ingresso per Cose da fare, regole portanti ripetute in 3-8 sedi, tassonomia dei componenti instabile, copy con varianti multiple, e quattro blocchi critici di accessibilità (contrasto righe spuntate, dark mode non tokenizzato, swipe-to-delete senza alternativa, focus visibile/traversal mancante). La coppia **non supera il gate** nello stato attuale.

## Category verdicts

| Categoria | Verdetto | Stato |
|---|---|---|
| Flow coverage | thin | aperto |
| Token completeness | thin | aperto |
| Component coverage | adequate | chiuso |
| State coverage | thin | aperto |
| Visual reference coverage | thin | aperto |
| Bloat & overspecification | adequate | chiuso |
| Inheritance discipline | thin | aperto |
| Shape fit | adequate | chiuso |
| Structure (reviewer extra) | thin | aperto |
| Prose (reviewer extra) | thin | aperto |
| Accessibility (reviewer extra) | thin | aperto, 4 blocchi critici |

**Totale finding:** 166 — **Critical 6**, High 53, Medium 81, Low 26.

---

## Findings by severity

### Critical (6)

**1. [Rubric + Accessibility] — Dark mode dichiarato ma non tokenizzato (§2 L191-192; token L7-47).**
Dieci token chiari non hanno coppia scura: `surface-subtle`, `surface-inverse`, `ink-muted`, `ink-inverse`, `border-hairline`, `accent-action`, `accent-success`, `accent-warning`, `badge-bg`, `badge-ink`. In dark mode `category-header` ha titolo su sfondo chiaro (1,00:1), `badge-bg` e `accent-action` spariscono (1,07:1). Un implementatore deve inventare metà del tema.
Fix: definire le coppie `*-dark` (o mapping esplicito) per tutte le superfici usate dai componenti, oppure rimuovere la promessa dark mode dalle spine. Aggiungere un mockup scuro di verifica.

**2. [Accessibility] — Righe spuntate illeggibili: contrasto ≈ 1,46:1 (§7.1 L255).**
`ink-muted` (#94A3B8) con opacity 45% su bianco dà circa 1,46:1; il mockup pieno arriva a circa 2,5:1. Le righe restano interattive (la checkbox deseleziona), quindi non sono componenti disabilitati ed escono dall'eccezione 1.4.3; si viola anche il floor 7:1 dichiarato dalla spina.
Fix: introdurre token `ink-struck` ≥4,5:1 (idealmente 7:1), nessuna opacità sul testo, barratura + checkbox come ridondanza, `ink-muted` riservato a veri disabilitati.

**3. [Accessibility + Rubric] — Swipe-to-delete senza alternativa accessibile (§4.7 L143; §7 L227).**
L'eliminazione è solo swipe a sinistra e l'etichetta TalkBack dice "Swipe a sinistra per eliminare". Con TalkBack lo swipe a un dito cambia il focus; con Switch Access il gesto non esiste. CAP-6 resta accessibile solo a chi usa il gesto (violazione WCAG 2.5.7).
Fix: aggiungere azione "Elimina" nelle custom action TalkBack o menu contestuale, con stessa snackbar di undo; correggere l'etichetta in "Azioni disponibili: Elimina".

**4. [Accessibility] — Focus visibile e ordine di traversal mancanti (§7 L219-232; DESIGN.md).**
Nessun token `focus-ring`, nessun ordine di traversal, controlli disegnati a mano. Con D-pad, tastiera e Switch Access il focus può essere invisibile o su nodi non operabili (violazione WCAG 2.4.7).
Fix: definire focus ring ad alto contrasto (≥3:1 sulle superfici adiacenti), ordine header → lista → sezioni → FAB, test con tastiera, D-pad e Switch Access.

**5. [Prose] — Microcopy dichiarato non vincolante (§9 L331).**
"Il microcopy esatto può essere rifinito in build senza cambiare i comportamenti descritti" indebolisce il contratto di copy; §3 e §4 presentano le stringhe come esatte. Un consumatore non sa quali stringhe siano contract e quali indicative.
Fix: dichiarare vincolanti le stringhe quotate (salvo elenco esplicito di "copy provvisorio") e rimuovere o restringere la clausola.

### High (53)

**6. [Rubric + Structure + Prose] — Condivisione a tre scelte con regole incompatibili (§4.10 L169 vs §5 L194; SPEC.md L66).**
§4.10 disabilita le opzioni con parte vuota; §5 dice che `Entrambe` resta disponibile se almeno una parte ha voci. Non è chiaro se `Entrambe` con spesa vuota sia selezionabile, cosa generi e quale default si applichi quando la parte corrente ha 0 voci.
Fix: scrivere una matrice scelta × parti vuote e allineare copy, stato, default e share text.

**7. [Prose + Accessibility + Rubric] — `Concludi spesa` ambiguo: dialog o snackbar (§4.3 L111; §3 L71).**
Una snackbar non è una conferma (svanisce, non richiede azione); il microcopy prevede `[Concludi spesa]`/`[Annulla]`. Chi implementa dal flusso costruisce un comportamento diverso da quello della conclusione attività.
Fix: scegliere un unico pattern (dialog con titolo, corpo, conteggio e due azioni definite); usare la snackbar solo come feedback post-azione.

**8. [Rubric] — Flow 3 descrive la share legacy con formato emoji/bullet (§8 L254-268).**
Mostra `🛒 TOGO - Lista Spesa` e `🥦 ORTOFRUTTA:` che contraddicono il formato reale `[L1] / + L2 / - L3` e il dialogo a tre scelte. Chi implementa dal flusso costruisce la vecchia share e il formato sbagliato.
Fix: riscrivere Flow 3 come baseline legacy o fonderlo nel Flow 8, passando dal dialogo e sostituendo l'esempio con il formato `[L1]`.

**9. [Structure] — Pattern "conclusione" duplicato e asimmetrico in almeno 7 sedi.**
`Concludi spesa` in §4.2-4.3, Flow 2, DESIGN §7.3; `Concludi attività` in §2.1, §3, §4.8, §5, §7, Flow 7, DESIGN §7.9-7.10. Le regole "solo spuntate", "niente undo", "niente storico" compaiono in più punti con parole diverse.
Fix: un solo pattern `Conclusione` con varianti `Spesa` e `Attività`; stati e flow che vi rimandano.

**10. [Structure] — Contratto pager ripetuto in almeno 8 punti.**
DESIGN §1 L169, §4 L221, §7.6 L285-291; EXPERIENCE §1 L26, §2.1 L41, §2.2 L53-56, §4.6 L131-139, §6 L212-214, §7 L225-226, Flow 6 L286, Flow 10 L318-319, §9 L326. Una modifica a frecce, back, swipe o conteggi richiede molti edit.
Fix: sezione canonica `Page header & pager` (anatomia in DESIGN, comportamento in EXPERIENCE), solo rimandi brevi altrove.

**11. [Structure] — `State Patterns` senza colonna Superficie/Ambito (§5 L184-201).**
Mescola stati di app (Cold Load, Offline), di pagina (Empty Spesa, Empty Da fare), di sezione (Fatte vuota), di dialogo (Duplicato, Conclusione, Condivisione con parte vuota), di import e di interazione (Cambio pagina). Un consumatore non sa a quale schermata applicare una riga.
Fix: aggiungere `Superficie` e `Ambito` (globale/pagina/sezione/overlay), separare le righe per area, spostare `Cambio pagina` in Interaction Primitives.

**12. [Structure] — `Voice and Tone` non è fonte unica del microcopy (§3 L61-87; §4.9 L163; §4.10 L169-172; DESIGN §7.8 L307).**
Le stesse stringhe vivono in più sezioni con e senza punto (`§3 L81/L192` vs `§4.7 L146/L4.9 L163`). Non esiste una stringa sorgente.
Fix: trasformare §3 in inventario `Contesto | Stringa | Note` e fare riferimento alla chiave da componenti e flussi senza riscrivere il testo.

**13. [Structure + Prose + Rubric] — Raggruppamento azioni ambiguo: urgenza header o solo ordine? (§2.1 L40; §4.7 L145; §6 L215; Flow 6 L289; todo-page L496-589; SPEC.md L54).**
IA dice "raggruppate per ambito e urgenza"; Flow 6 dice "sotto Salute → Urgente"; il mockup mostra solo header ambito, senza sotto-header urgenza. Non è chiaro se l'urgenza sia header annidato, etichetta di percorso o solo criterio di ordinamento.
Fix: dichiarare esplicitamente "header di ambito; urgenza = ordinamento, non header" (o il contrario) e allineare mockup, copy e Flow 6.

**14. [Structure + Prose + Rubric] — Tassonomia componenti instabile (frontmatter vs corpo vs mockup).**
`duplicate-modal` (DESIGN §7.5, EXPERIENCE §4.5) vs `duplicate-dialog` (token); `voice-sheet` (DESIGN §7.4, EXPERIENCE §4.4) vs `voice-fab` (token); `share-dialog` (DESIGN §7.11, EXPERIENCE §4.10) vs `choice-dialog` (token); `fatte-section` e `conclude-dialog` senza token. Nessuna chiave unica tra frontmatter, prosa, EXPERIENCE e mockup.
Fix: tabella `token | nome visuale | pattern EXPERIENCE | mockup` con un solo nome canonico per componente e alias espliciti.

**15. [Structure + Prose] — `Lista attiva` cambia referente dopo il pager (§2.1 L39; DESIGN §7.3 L263; PRD.md L61).**
Nel PRD è l'intera lista Spesa; nella spina è pagina 1 del pager. Chi deriva storie dal PRD non sa quale significato prevale.
Fix: usare "lista della spesa" / "lista delle cose da fare"; se "attiva" significa "non spuntata", definirlo una volta e non usarlo come nome di superficie.

**16. [Structure] — Nessuna mappa di copertura CAP → sezioni → flow → mockup (EXPERIENCE L4-8, L238-322; SPEC.md CAP-1..CAP-10 L18-47).**
I flow Cose da fare (Flow 6-10) non hanno CAP né UJ, mentre i flow baseline hanno UJ-1..UJ-5. Uno story-dev non può verificare se una storia copre CAP-3 o CAP-6.
Fix: tabella `Feature coverage map` con una riga per CAP (sezioni, flow, mockup, stato); taggare i flow.

**17. [Structure] — Precedenza SPEC.md vs spine non dichiarata (SPEC.md L8; EXPERIENCE.md L15-18; DESIGN.md L156-158).**
In caso di conflitto tra regole di SPEC e spine (condivisione, undo, raggruppamento) il consumatore non sa quale prevale. Il preambolo dice solo che le spine vincono sui mockup.
Fix: dichiarare precedenza: SPEC/brownfield vincono sul comportamento di prodotto; le spine vincono su UX e visuale; i mockup sono illustrativi.

**18. [Prose] — Stato vuoto Spesa in due varianti (§3 L73 vs §5 L189).**
"La tua lista è vuota. Tocca il microfono o premi + per aggiungere prodotti" contro "Nessun prodotto da acquistare" con "pulsanti chiari per dettare a voce o digitare". Non è detto quale sia titolo, corpo e CTA.
Fix: definire `titolo | corpo | CTA` in un'unica riga e usare la stessa stringa in §5.

**19. [Accessibility] — Errori di import senza contratto di annuncio/associazione (§4.9 L163; §5 L196).**
`Azione già presente` appare inline e l'import riporta numero di riga e motivo, ma non è detto che il campo abbia `isError`/`supportingText`, che il messaggio sia una live region, né che il focus si sposti sul campo o sul primo errore. Un utente TalkBack può non accorgersi dell'errore e riprovare.
Fix: associare l'errore al campo, annunciarlo con `LiveRegionMode.Assertive`, spostare il focus sul campo (o su un riepilogo errori nell'import) e conservare i valori.

**20. [Accessibility] — Placeholder e hint sotto soglia di contrasto (placeholder #94A3B8 2,56:1; hint #64748B su #F1F5F9 4,34:1).**
Non sono decorativi: spiegano dove cercare e come chiudere una sezione.
Fix: portare placeholder e hint almeno a 4,5:1; mantenere un'etichetta visibile accanto al campo (già presente nel foglio azione).

**21. [Rubric + Accessibility] — `border-crisp-dark` ha due valori: `#64748B` (frontmatter L37) vs `#475569` (prosa L192).**
Su base #0B0F17 il secondo scende a 2,26:1 sui card scuri, sotto 3:1 di 1.4.11 per identificare i controlli.
Fix: fissare un unico valore, correggere la prosa, verificare i bordi su entrambe le superfici scure.

**22. [Rubric + Accessibility] — Contrasto Urgente 6,37:1 sotto il floor 7:1 dichiarato (§2 L22-23; §8 L338; §7 L221).**
Passano AA (4,5:1) ma non il floor della spina; mancano i rapporti dichiarati per badge urgenza e ambito.
Fix: o abbassare il floor ad AA (4,5:1 testo, 3:1 non-testo) e dichiararlo, o scurire i token; documentare i ratio delle combinazioni portanti.

**23. [Accessibility] — Focus perso dopo spostamento riga a 300 ms (§4.1 L97-98; §4.7 L144-145).**
La riga si sposta in Presi/Fatte mentre l'annuncio TalkBack è in corso: focus perso o salto imprevedibile; anche per il tocco il prossimo tap può cadere su un'altra riga.
Fix: non spostare la riga finché il focus è su di essa; in alternativa spostare il focus sull'header della sezione e annunciare "Spostata in Presi, N articoli" con un'azione "Annulla spunta" immediata.

**24. [Accessibility] — Ritorno del focus dopo chiusura dialog/fogli non specificato (§4.5 L125-129; §4.9 L159-165; §4.10 L167-172).**
Dopo `Concludi attività` il pulsante trigger scompare (la sezione Fatte non c'è più); dopo `Condividi` si apre lo Sharesheet. Senza regola il focus resta sul nodo rimosso o torna all'inizio della pagina (WCAG 2.4.3).
Fix: definire per ogni modale il target di ritorno (trigger; se non esiste, titolo pagina o FAB), con fallback e annuncio; al ritorno dallo Sharesheet riportare il focus sull'icona Condividi.

**25. [Accessibility] — Undo snackbar senza durata, pausa o annuncio (§4.7 L147).**
Durata pochi secondi: troppo poco per ipovedenti e utenti cognitivi (WCAG 2.2.1).
Fix: durata ≥10 s o persistente fino all'azione successiva, pausa su focus/hover, live region educata con "Azione eliminata. Annulla disponibile", azione Annulla ≥48dp.

**26. [Accessibility] — Pager/cestino e gesture navigation: nessuna politica edge-to-edge, inset, predictive back (§2.2 L54; §4.6 L134-135; brownfield.md L17, L22).**
Su gesture navigation lo swipe da bordo può uscire dall'app invece di cambiare pagina o rivelare il cestino; il back da Da fare rischia un'anteprima di chiusura app invece di Spesa.
Fix: definire zone di swipe lontane dai bordi, `systemGestureExclusion` solo per l'header del pager, `PredictiveBackHandler` per Da fare → Spesa, test con gesture nav e 3-button nav.

**27. [Accessibility] — Freccia disabilitata: annunciata ma non focusabile (§7.6 L288; §4.6 L134; §7 L225).**
Resta annunciata come non disponibile, ma un controllo `enabled=false` non è focusabile in TalkBack: le due richieste sono incompatibili.
Fix: scegliere — rimuovere la freccia e annunciare "pagina 1 di 2" nel titolo, oppure tenerla focusabile con `stateDescription="Non disponibile"` e nessuna azione.

**28. [Accessibility] — `Concludi attività`: icona warning non commessa, etichetta del trigger invece della conseguenza (§4.10 L167-172; conclude-dialog L555-573; DESIGN §7.10 L316-320).**
Per un'azione irreversibile senza undo, l'etichetta dovrebbe dire cosa succede.
Fix: primario "Elimina N attività" (stile distruttivo), icona warning commessa, Annulla resta l'azione sicura.

**29. [Accessibility] — Import: anteprima non annunciata come regione, primario "Accetta" non definito (§4.11 L177-180; Flow 9 L312).**
L'anteprima non è specificata come riepilogo annunciato; il primario dice "Accetta" invece di descrivere l'azione.
Fix: rendere l'anteprima una region con heading, etichettare il primario "Importa N prodotti e M azioni", annunciare l'esito e offrire un riepilogo post-import.

**30. [Accessibility] — Semantica mancante: checkbox senza ruolo/stato, chip senza radiogroup, sezioni senza `expanded`, dialog/fogli senza modalità (§7 L223-229; action-sheet L592-607; conclude-dialog).
La checkbox è "un controllo separato" ma non si dice `Role.Checkbox`, `toggleable` o `stateDescription`; i chip non hanno gruppo, `selected` né etichetta di gruppo; header collassabile ha testo di annuncio ma non semantica; dialog e bottom sheet non hanno `paneTitle`, focus trap, `onDismissRequest` né focus di ritorno.
Fix: `Role.Checkbox` con `stateDescription`; radiogroup con label "Urgenza"/"Ambito" e `selected` per chip; `Modifier.expandable` + `stateDescription` su header Presi/Fatte; Compose `Dialog`/`ModalBottomSheet` con `paneTitle`, focus iniziale, trap e focus di ritorno.

**31. [Accessibility] — Font scaling dichiarato lineare ma Android 14+ è non lineare; header 64dp fisso, titolo `nowrap` (§7 L232; DESIGN L81, L120; todo-page L84-103).**
A 200% il layout non è definito e il titolo non può andare a capo nei mockup.
Fix: usare `sp`, testare 130/150/200% a 320dp, rimuovere `nowrap`, permettere header a più righe e overflow; non promettere scaling lineare.

**32. [Accessibility] — Tastiera/IME non considerata: foglio 65-68% con autofocus e pulsanti in fondo, niente `imePadding`/`bringIntoView` (§9 L327; action-sheet L290-305).**
A font grande campi e pulsanti possono essere coperti dalla tastiera.
Fix: `Modifier.imePadding()`, `bringIntoViewRequester` sul campo attivo, pulsanti sticky sopra la tastiera, test con Gboard e font scale 200%.

**33. [Rubric + Accessibility] — Mockup con target sotto 48dp: icone header 44×44, checkbox azione 40×40, chip 38dp (active-list L118-124; todo-page L234-242; action-sheet L395-409).**
Violano il target minimo 48dp dichiarato (DESIGN.md L76, L219; EXPERIENCE.md L206).
Fix: portare l'area di tocco a 48dp mantenendo la dimensione visiva; aggiungere token `chip` e `icon-button` con hit area esplicita.

**34. [Rubric + Accessibility] — Mockup omettono badge urgenza su righe che dovrebbero averlo sempre (action-sheet L540-545, L563-569; share-dialog L462-484; conclude-dialog L517-524).**
Contraddice SPEC.md CAP-5 L32, DESIGN.md §7.7 L295, EXPERIENCE.md §4.7 L142. L'urgenza non è percepibile né annunciabile.
Fix: aggiungere il badge a ogni riga o annotare i mockup come non normativi.

**35. [Structure] — `Inspiration & Anti-patterns` mancante; rifiuti di navigazione sparsi (§1 L26; §2.2 L53; §7 L212; .memlog.md L8-11, L18-19).**
I rifiuti (bottom tab bar, drawer, pager a tutta schermata, check-off) sono sparsi in Foundation e Interaction Primitives. L'omissione è difendibile, ma il "perché no" non è in un posto solo.
Fix: aggiungere una sezione breve con i soli rifiuti che vincolano il design, oppure rimuovere i riferimenti sparsi.

**36. [Structure] — Frontmatter DESIGN senza `sources`; EXPERIENCE ha quattro path (L1-6 vs L1-11).**
Chi legge solo il visual spine non sa che è stato aggiornato per `spec-cose-da-fare` e non può risalire alle decisioni.
Fix: aggiungere `sources` o una riga `Aggiornato per: SPEC-cose-da-fare` nel preambolo, con i path a SPEC e brownfield.

**37. [Prose] — Terminologia iOS `Dynamic Type` su Android (§4 L221-222; §7 L232; §7.6 L291; §8 L344).**
Su Android il comportamento è la scalabilità del testo di sistema (`fontScale`); un lettore può implementare l'API sbagliata o cercare una feature inesistente.
Fix: "scalabilità del testo di sistema (font scale Android)" ovunque.

**38. [Prose] — Riferimenti al baseline non risolti: "Invariato", "come oggi", "formato attuale" (§2.1 L43-49; §4.10 L171; §4.11 L175).**
Chi non conosce l'app deve aprire `brownfield.md` per sapere cosa è invariato.
Fix: una riga di sintesi per ogni superficie invariata, con rimando alla fonte.

**39. [Prose] — Microcopy duplicato con varianti in §3, §4, §5, flussi e DESIGN.**
La conclusione compare in `§3 L82`, `§4.8 L155-156`, `§5 L193`, `DESIGN §7.9-7.10 L313-320`, `Flow 7 L297-298`; la condivisione in `§3 L84-85`, `§4.10 L169-172`, `§5 L194`. Non c'è una stringa sorgente.
Fix: un inventario `chiave | stringa | contesto | note` in §3 e rimandi dalle altre sezioni.

**40. [Rubric] — CAP-1 (pager) e CAP-5 (distinzione visiva) senza Key Flow dedicato; CAP-6 (modifica/eliminazione azione con undo) senza flow con passi, climax e failure.**
CAP-1 coperto solo incidentalmente da Flow 6/10; CAP-5 dalla sola `§4.7`; CAP-6 solo in `§4.7 L146-147` senza passi.
Fix: accettare esplicitamente la copertura da component pattern o aggiungere un flusso di cambio pagina e Flow 11 "Mike modifica ed elimina un'azione".

**41. [Rubric] — Sette flussi senza failure path (Flow 1, 2, 3, 4, 5, 6, 9).**
Flow 1 (voce non disponibile o ambigua), Flow 2 (elemento non trovato), Flow 3 (lista vuota o share annullata), Flow 4 (annulla o unità incompatibili), Flow 5 (duplicato al riaggiungi), Flow 6 (duplicato o testo vuoto), Flow 9 (import non valido).
Fix: una riga "Failure:" per flusso, coerente con `§5`.

**42. [Rubric] — Quattro flussi senza climax osservabile (Flow 3, 4, 5, 10).**
Si fermano alla risoluzione: manca sharesheet aperto, riga aggiornata a 2 l, prodotto riaggiunto nella categoria, annuncio TalkBack del nuovo titolo.
Fix: aggiungere un climax osservabile per ciascuno.

### Medium (81)

**43. [Structure] — Numerazione componenti non parallela tra DESIGN e EXPERIENCE.**
`DESIGN §7.8 Foglio Azione` corrisponde a `EXPERIENCE §4.9`, mentre `DESIGN §7.9 Sezione Fatte` corrisponde a `EXPERIENCE §4.8`. Chi usa i numeri come chiave incrociata finisce su componenti diversi.
Fix: riallineare la numerazione o aggiungere tabella mapping componente → sezione DESIGN → sezione EXPERIENCE.

**44. [Structure] — `Import con Azioni` mescolato con i componenti atomici (§7.12 L328-331).**
Non è un componente, non ha anatomia né token; va in una sezione di flusso o schermata.
Fix: separare `Componenti atomici`, `Pattern compositi`, `Overlay`, `Schermate/Flussi`; spostare Import in sezione di flusso.

**45. [Structure] — Tabella IA mescola destinazioni, overlay e chrome (§2.1 L37-50).**
`Intestazione di Pagina (Pager)` non è una superficie raggiungibile; `Risoluzione Duplicati`, `Condivisione`, `Conferma Conclusione` sono overlay; `Gestione Regole Apprese` e `Importa Lista` sono sotto-schermate di Impostazioni.
Fix: aggiungere colonna `Tipo` (Schermata / Overlay / Chrome / Sotto-schermata) e separare shell dalle destinazioni.

**46. [Structure] — Nessun TOC in nessuno dei due documenti (354 e 331 righe).**
L'unica navigazione è la lista di link nel preambolo; i titoli numerati aiutano ma non danno vista d'insieme.
Fix: aggiungere TOC di 8-10 voci all'inizio di ciascuna spina.

**47. [Structure] — Path in `sources` stringhe repo-relative, non link cliccabili; riferimento spec Google Labs senza URL (EXPERIENCE L4-8; DESIGN L156).**
Fix: usare link Markdown relativi alla workspace e aggiungere l'URL della spec.

**48. [Structure] — `reconcile-inputs.md` stale e non linkato dalle spine.**
Mappa solo requisiti PRD, cita `UJ-5` inesistente, dichiara "nessuna omissione o conflitto" senza coprire Cose da fare.
Fix: rigenerarlo per la feature o marcarlo `superato`/rimuoverlo.

**49. [Structure] — Gli 8 link ai mockup ripetuti in DESIGN e EXPERIENCE.**
Aggiungere o rinominare un mockup richiede due modifiche e può far divergere le liste.
Fix: unico `mockups/README.md` come indice, linkato da entrambe le spine.

**50. [Prose] — Stato dello stato spuntato ha tre verbi: "spuntato", "barrato", "depennato" (§4.7 L143/L144; DESIGN §2 L181).**
"Barrato" è il trattamento visivo, non lo stato; "depennato" è fuori uso.
Fix: "spuntata" per lo stato, "barrata" per il rendering; eliminare "depennato".

**51. [Prose] — Nomi delle azioni non stabili: "Condividi" (icona/superficie/primario); "Aggiungi", "+ Aggiungi", "+ Riaggiungi" per lo storico.**
Fix: tabella `controllo | etichetta | contesto` e una sola etichetta per il pulsante dello storico.

**52. [Prose] — Nomi di categoria e chrome divergenti tra spine e mockup: "Banco Frigo" / "Banco Frigo / Latticini"; "Igiene/Cura Casa" vs "Pulizia casa"; "intestazione" / "App Bar" / "top bar".**
Fix: usare i nomi del catalogo o marcare gli esempi come illustrativi.

**53. [Prose] — Voice and Tone: EXPERIENCE scivola nel registro promozionale e narrativo (§8 Flow 1 L242-243; §4.1 L98; Flow 8 L305; Flow 2 L248).**
"la forma d'onda cattura l'audio", "l'interpretazione è perfetta", "scivola fluidamente", "in due tocchi" sono giudizi non osservabili, non regole.
Fix: sostituire con esiti verificabili (es. "la trascrizione compare entro 0,8 s").

**54. [Prose] — Domande di conferma cambiano persona: infinito (§3 L71/L82), seconda persona (§3 L70/L84), prima persona (§3 L69).**
Fix: scegliere una forma ("Vuoi concludere la spesa?", "Vuoi condividere…?") e applicarla a tutti i dialoghi.

**55. [Prose] — Prompt di apprendimento antropomorfo e risposte non concordanti (§3 L69).**
"Ricordo questa scelta per i prossimi inserimenti?" con `[Ricorda]` / `[Solo per ora]`: domanda in prima persona, risposte imperative rivolte all'app.
Fix: "Vuoi che ricordi questa scelta per i prossimi inserimenti?" `[Sì, ricorda]` / `[No, solo per ora]`.

**56. [Prose] — Verbi per interazione tattile non coerenti: "Tocca", "premi +", "swipa", "Tap", "Doppio tocco" (§3 L73; Flow 6 L286; §4.1 L95; §7 L227).**
Fix: "tocca" per i controlli, "scorri" per il gesto, "doppio tocco" nelle stringhe TalkBack; evitare "swipa".

**57. [Prose] — DESIGN overclaim: "massimo contrasto acromatico" (§2 L175) mentre la palette ha blu, verde e ambra; "leggibilità impeccabile" (§1 L166); "inconfondibile" (§6 L243).**
Fix: tenere la voce editoriale, togliere gli assoluti che contraddicono i token.

**58. [Prose] — Stato vuoto Fatte: stringa "Nessuna attività conclusa." per una sezione che non compare se N = 0 (§3 L83 vs §4.8 L157).**
Se la sezione non compare, la stringa non è raggiungibile.
Fix: eliminarla oppure decidere che la sezione resti visibile vuota e allineare §4.8/§5.

**59. [Prose] — Pulsante conferma import definito solo in un flusso: "Accetta" (§8 Flow 9 L312).**
§3 non definisce l'etichetta e DESIGN §7.12 non la nomina.
Fix: aggiungere "Importa" (o "Conferma import") all'inventario e usarla nel flusso.

**60. [Prose] — Dialogo duplicati: tre versioni e referente indefinito (§3 L70; Flow 4 L273; DESIGN §7.5 L278).**
"«Latte parzialmente scremato» è già presente con 1 l nella categoria Banco Frigo" vs "Il prodotto è già presente con 1 l" vs chiede "quantità attuale e nuova". "Il prodotto" non dice quale; la nuova quantità manca nel corpo di §3.
Fix: template unico con `{prodotto}`, `{quantità attuale}`, `{categoria}`, `{nuova quantità}` e stessa stringa in flusso, §3 e mockup.

**61. [Prose] — Titolo dialogo condivisione duplicato/invertito: §3 L84 "Cosa vuoi condividere?"; mockup titolo "Condividi" e sottotitolo "Cosa vuoi condividere?" (share-dialog L491-492).**
Fix: scegliere una gerarchia e riportarla in §3 e nel mockup.

**62. [Prose] — "Nessuna voce" (§4.10 L169) non spiega perché l'opzione è disabilitata, mentre §7 L230 promette che le scelte disabilitate spiegano perché.**
Fix: "Nessun prodotto" / "Nessuna azione" oppure "Niente da condividere in questa lista".

**63. [Prose] — "Sostituisci quantità (1 l)" (duplicate-dialog L278) non dice con cosa sostituisce; nell'esempio quantità attuale e nuova sono entrambe 1 l.**
Fix: "Sostituisci con 1 l" e usare un esempio con quantità diverse.

**64. [Prose] — Conferma vocale telegrafica e non idiomatica: "Aggiunto: 2 l di Latte parzialmente scremato in Banco Frigo" (§3 L68).**
Fix: "Aggiunto: latte parzialmente scremato, 2 l, in Banco Frigo" o "2 l di latte… aggiunti a Banco Frigo".

**65. [Prose] — Tre urgenze non glossate; "Eccezionale" semanticamente ambiguo (suona "più importante" ma è ultimo nell'ordine) (SPEC.md L54; §4.9 L161).**
Fix: riga di definizione per ciascuna urgenza o helper nel foglio; confermare l'ordine voluto.

**66. [Prose] — Stringhe TalkBack con placeholder e istruzione ineseguibile (§7 L224/L227).**
"espansa/collassata" va letta scegliendo uno stato; "Swipe a sinistra per eliminare" istruisce un gesto che TalkBack intercetta.
Fix: stringhe condizionali e azione accessibile annunciata (es. "Elimina, azione disponibile").

**67. [Prose] — Suggerimento sezioni collassabili statico: "Tocca per nascondere" resta anche quando chiuso (active-list L603; todo-page L599; conclude-dialog L534).**
Fix: "Tocca per mostrare/nascondere" in base allo stato, o rimuovere il suggerimento.

**68. [Prose] — Conteggi dentro le CTA divergono dalla spina e tra loro: "Concludi Spesa (Archivia 2)" (active-list L641), "Concludi attività (2)" (todo-page L635); DESIGN §7.10 L319 definisce `Concludi attività` senza conteggio.**
Fix: togliere il conteggio dalle etichette e lasciarlo nel corpo del dialogo.

**69. [Prose] — Punteggiatura e notazione non uniforme: "Azione già presente" con e senza punto; etichette tra backtick, parentesi quadre e virgolette dritte (§4.8 L155; §3 L69; DESIGN §8 L349).**
Fix: backtick per tutte le stringhe di UI; nota che le quadre non fanno parte del copy (confusione con `[DA FARE]`).

**70. [Prose] — Numeri e unità senza convenzione unica: "15ms", "300ms", "0.8s" vs "240 ms"; "48x48dp" vs "48dp"; "1400+ prodotti" vs "1.400 voci" (§96, L98, L134, L242; L206; §2.1 L43; §5 L200).**
Fix: "15 ms", "300 ms", "0,8 s", "48×48 dp", "1.400" sempre.

**71. [Prose] — "sente" usato per l'aptic: "sente la vibrazione tattile", "sente il feedback aptico" (Flow 2 L249; Flow 10 L321).**
Fix: "percepisce"; uniformare "feedback aptico" / "vibrazione".

**72. [Accessibility] — `Eccezionale` come urgenza ambiguo in italiano (può significare "ottimo") (§4.9 L161).**
Fix: riga di aiuto sotto il selettore ("Eccezionale = fuori routine, da fare appena possibile") o termine più chiaro, senza cambiare il contratto.

**73. [Accessibility] — Nessuna live region per cambi dinamici (spunta, spostamento Presi/Fatte, aggiunta, eliminazione+undo, esito condivisione/import, trascrizione vocale) (§7 L226 annuncia solo cambio pagina).**
Fix: definire per ogni evento testo, priorità (polite/assertive) e se spostare il focus; evitare doppio annuncio.

**74. [Accessibility] — Semantica righe sovraccarica: due nodi per riga (riga + checkbox), stesse informazioni lette 2-3 volte (§7 L227/L229).**
Fix: un solo nodo per riga (toggle + custom actions) o badge decorativi nel contesto; documentare reading order.

**75. [Accessibility] — Solo titolo pagina è heading; header categoria/ambito e sezioni non lo sono (§7 L225; §7.3 L261-264; §7.9 L309-314).**
TalkBack per heading non può saltare tra gruppi in lista lunga.
Fix: `heading()` su ogni header di gruppo e sezione, livello coerente.

**76. [Accessibility] — FAB cambia significato per pagina senza `contentDescription` per pagina (§4.6 L138; DESIGN §6 L243; todo-page L640).**
Fix: descrizione esplicita "Aggiungi prodotto"/"Aggiungi cosa da fare" e stato per pagina.

**77. [Accessibility] — Badge quantità senza `contentDescription` estesa ("due litri", "duecentocinquanta grammi") né espansione unità (§7 L223).**
Fix: mappare unità → testo parlato e applicare a tutti i badge; nascondere testo visivo duplicato.

**78. [Accessibility] — Nessuna strategia reduced motion: pager 240 ms, spostamento riga 300 ms, sheet 250 ms, onda vocale (§4.6 L134; §4.1 L98; voice-sheet L89).**
Fix: rispettare `ANIMATOR_DURATION_SCALE`/`LocalMotionDurationScale`, ridurre a dissolvenza o nessuna animazione; onda vocale icona statica.

**79. [Accessibility] — Azioni globali (Storico, Condividi, Impostazioni) in alto, fuori zona pollice su schermi grandi, contrasta uso a una mano (§1 L167; §1 L24).**
Fix: spostare azioni secondarie in menu raggiungibile dal basso o valutare con test a una mano.

**80. [Rubric] — `category-header` senza pattern comportamentale (§7.2 L257-259 vs EXPERIENCE §4).**
Non è chiaro se il tap sull'header faccia qualcosa e come si comporti il conteggio del gruppo.
Fix: aggiungere riga "Header di categoria — statico; zona swipe pager; conteggio = voci non spuntate del gruppo".

**81. [Rubric] — `conclude-dialog` senza Component Pattern dedicata (§4.8 L155; DESIGN §7.10 L316-320; conclude-dialog L555-573).**
Anatomia e copy vivono in DESIGN e nello stato; mockup aggiunge icona warning, preview righe, nota irreversibile non commesse.
Fix: aggiungere riga componente con anatomia esatta (icona sì/no, preview sì/no, ordine pulsanti).

**82. [Rubric] — Empty state senza spec visiva e senza riga in Component Patterns (§3 L72-74; §5 L189-190).**
Icona/illustrazione, altezza, CTA e rapporto con FAB non definiti.
Fix: aggiungere componente `empty-state` in DESIGN §7 e pattern in EXPERIENCE §4.

**83. [Rubric] — Import: spec solo per contenuto (§7.12 L328-331), senza anatomia visiva né token per blocco `[DA FARE]`, anteprima o errore atomico.**
Fix: aggiungere spec pannello anteprima/errore e trattamento visivo del blocco.

**84. [Rubric] — FAB senza riga propria: `voice-fab` definisce solo FAB vocale 64dp; FAB (+) di Da fare citato in prosa senza token né stato (§6 L243; §7.6 L290).**
Fix: aggiungere `fab-action` o variante contestuale con token e pattern in EXPERIENCE §4.

**85. [Rubric] — Quattro coppie di component token duplicano anatomia: `app-bar`/`page-header`, `bottom-sheet`/`action-sheet`, `duplicate-dialog`/`choice-dialog`; `voice-fab` unico token FAB (L78-151).**
Fix: tenere un token per ruolo, alias o `extends`, non copie.

**86. [Rubric] — Numeri precisione finta: haptic 15ms, ritardo 300ms, transizione 240ms, trascrizione 0.8s, sheet 45%, foglio 65% (§98, L118, L134, L242, L327).**
Fix: spostare in "default di build" o rimuovere; lasciare come contract solo valori che cambiano il comportamento.

**87. [Rubric] — Cinque flow (1-5) narrativi e in parte superati (Flow 3); occupano metà Key Flows e spingono feature in coda (§8 L236-322).**
Fix: separare sezione "Baseline — Spesa (Flow 1-5)" da "Cose da fare (Flow 6-10)" o rimuovere flow superati.

**88. [Rubric] — `UX Assumptions §9` non elenca decisioni aperte: ripete cinque regole già normative e aggiunge "microcopy può essere rifinito" (§9 L324-331).**
Fix: spostare decisioni nel corpo, lasciare in Open decisions solo ciò che è davvero aperto, limitare deroga microcopy a stringhe non contrattuali.

**89. [Structure] — `Key Flows` lista piatta di 10 flow senza separatore baseline/feature (§8 L236-322).**
Fix: sottosezioni `Baseline — Spesa (Flow 1-5)` e `Cose da fare (Flow 6-10)`.

**90. [Structure] — Nessuna file map oltre lista mockup nel preambolo; non si dice quale mockup illustra quale superficie/componente (L158; L18; mockups/).**
Fix: tabella `Mockup | Superficie/Componente | Cosa illustra | Stato` nel preambolo o `mockups/README.md`.

**91. [Accessibility] — Mockup senza semantica: checkbox `div` senza `role`/`aria-checked`, scelte condivisione `div` senza `role=radio`, chip `<button>` senza `aria-pressed` (active-list L513-515; todo-page L507-511; share-dialog L495-520; action-sheet L592-607).**
Fix: aggiungere semantica ARIA ai mockup o scheda "semantics checklist"; in Compose mappare 1:1.

**92. [Accessibility] — Contrasto e dimensioni nei mockup: badge 10px (todo-page L291-299); "CONSIGLIATO" bianco su #16A34A 3,30:1; "Interpretazione completata" #16A34A su bianco 3,30:1 (duplicate-dialog L178-187; voice-sheet L199-209).**
Fix: badge ≥12sp, verde più scuro o testo scuro su verde chiaro; usare `accent-success #15803D` (5,02:1).

**93. [Accessibility] — Nessun mockup mostra stato focus (`:focus-visible`) per tastiera/D-pad/Switch (mockup/).**
Fix: aggiungere frame "focus" per header, riga, checkbox, chip, FAB e dialoghi con token `focus-ring`.

**94. [Accessibility] — Titoli/conteggi mockup divergenti dalla spina: "Spesa" + badge "4 DA PRENDERE" vs "Spesa (4)"; "Da fare" + "4 AZIONI • 2 FATTE" vs "Da fare (4)" (active-list L469-474; todo-page L465-470).**
Fix: allineare titolo, badge e conteggi; un solo punto di annuncio del conteggio.

**95. [Accessibility] — Nessun mockup dark, benché dark mode dichiarato (DESIGN L191-192).**
Fix: produrre almeno `active-list-dark` e `action-sheet-dark`, o rimuovere dark mode dal contratto.

**96. [Accessibility] — Selezioni chip e scelte condivisione solo riempimento/bordo, senza spunta (action-sheet L411-427; share-dialog L305-338).**
Fix: aggiungere spunta/icona di stato al selezionato (ridondanza non cromatica).

**97. [Rubric] — `DESIGN.md` sezione `7.5` dice "3 pulsanti" ma ne elenca quattro (L279-283); mockup ne mostra quattro (duplicate-dialog L265-294).**
Fix: correggere in "quattro".

**98. [Rubric] — Mockup conteggi interni incoerenti: action-sheet dichiara 5 azioni ma ne mostra 4 (L505 vs L527-571); share-dialog dichiara 5 ma ne mostra 4 (L441 vs L456-485).**
Fix: allineare conteggi o usare caso con 5 righe.

**99. [Rubric] — `deprecated` token orfani: `surface-inverse`, `rounded.none`, `accent-warning`, `app-bar`, `bottom-sheet`, `duplicate-dialog` non referenziati (L11, L21, L64).**
Fix: rimuovere i token morti o dichiararli deprecati.

**100. [Prose] — Glossario mancante per termini nuovi: "azione", "ambito" (5 valori), "urgenza" (3 valori), "Fatte", "lista attiva" (Structure #36).**
Fix: tabella `Termine | Valori | Default | Ordine | Token badge` in Foundation.

### Low (26)

**101. [Prose] — Punteggiatura del copy non uniforme per tipo di stringa: titoli senza punto, corpi a volte con punto, etichette pulsante a volte con punto (§81-L90).**
Fix: titolo senza punto, corpo con punto, etichetta pulsante senza punto, messaggio errore con punto.

**102. [Prose] — Maiuscola dopo due punti non uniforme: "Supporto Dark Mode:" vs frase continua minuscola (§191; §6 L215).**
Fix: minuscola dopo i due punti, salvo nomi propri o frasi autonome.

**103. [Prose] — Conteggi senza template di singolare/plurale: "Spesa (N)", "Da fare (N)", "Presi (N)", "Fatte (N)"; mockup aggiungono "4 DA PRENDERE", "4 AZIONI", "2 FATTE", "2 VOCI", "1 VOCE" (§143).**
Fix: tabella template `{n} prodotto/i`, `{n} azione/i`, `{n} articolo/i` con regole per n = 0, 1, molti.

**104. [Prose] — Brand "Togo" nel frontmatter vs "TOGO" nel corpo (mechanical notes).**
Fix: allineare il naming.

**105. [Prose] — `status: draft` in entrambe le spine mentre `.memlog.md L33` dice "pronta per il Reviewer Gate" (mechanical notes).**
Fix: decidere se il gate produce `final` e aggiornare frontmatter.

**106. [Structure] — `DESIGN.md` e `EXPERIENCE.md` hanno `status: draft` e `updated: 2026-11-04`; vanno finalizzati dopo validazione.**
Fix: impostare `status: final`, `updated: 2026-11-05`, registrare "spines finalized" nel memlog.

**107. [Structure] — Punteggiatura/minuzie: "Azione già presente" senza punto in DESIGN L307, EXPERIENCE L146/L163, SPEC L61; con punto in EXPERIENCE L81/L192.**
Fix: uniformare prima del final.

**108. [Accessibility] — Haptics: rispettare impostazioni di sistema; l'haptic è ridondante, mai unico feedback (§231).**
Fix: documentare come ridondanza.

**109. [Accessibility] — Lingua: locale italiana e `localeConfig`; verificare pronuncia TalkBack di unità, "TOGO" e nomi prodotto stranieri (§232).**
Fix: testare pronuncia e configurare `localeConfig`.

**110. [Accessibility] — Magnification: header e bottom bar fissi riducono area utile; testare 200-300% e scroll sotto barre (§229).**
Fix: test su device a 200-300%.

**111. [Accessibility] — Voce esterna: `RecognizerIntent` può mancare o richiedere rete; fallback manuale visibile e annunciato (§230).**
Fix: fallback visibile e annunciato.

**112. [Structure] — Mockup legacy non distinti: `duplicate-dialog`, `voice-sheet`, `history` datati 2026-10-07 vs i 5 aggiornati 2026-11-04 (§56).**
Fix: link inline per sezione con etichetta "Aggiornato per Cose da fare" / "Legacy" e data.

**113. [Structure] — `.working/` contiene copie pre-pager non contract; senza manifest un consumatore che globba gli HTML può prenderle per reference (§98).**
Fix: escludere o marcare come non contract.

**114. [Rubric] — Artefatto stale `reconcile-inputs.md` mappa solo PRD e dichiara "nessuna omissione o conflitto" pur citando UJ-5 inesistente (§101).**
Fix: rigenerare o rimuovere dal workspace.

**115. [Rubric] — File di review (`review-rubric.md`) presente nello workspace UX: non è una fonte UX (§98).**
Fix: non citare come fonte nelle spine.

---

## Nota per categoria (aperte se thin/broken)

- **Flow coverage — aperto.** CAP-1/CAP-5 senza flow dedicato, CAP-6 senza passi/climax/failure, 7 flow senza failure, 4 flow senza climax, Flow 3 legacy.
- **Token completeness — aperto.** Dark mode senza coppie scure, `border-crisp-dark` due valori, Urgente sotto floor 7:1, oggetti tipografici su chiavi scalari, rapporti contrasto badge non dichiarati.
- **Component coverage — chiuso.** 10/12 coppie component-pattern; mancano `category-header`, `conclude-dialog`, empty state e FAB senza riga propria.
- **State coverage — aperto.** Condivisione contraddittoria, regole visibilità sezioni incomplete, tema scuro senza stato, storico senza empty, undo non in pattern, focus visibile mancante.
- **Visual reference coverage — aperto.** Mockup linkati solo in preambolo, badge urgenza mancante in alcuni mockup, target sotto 48dp, titoli/conteggi non allineati, stile chip/sheet solo nei mockup, conteggi incoerenti.
- **Inheritance discipline — aperto.** Nomi componenti instabili, UJ-4/UJ-5 non nelle fonti, glossario mancante, checkbox descritta in due modi, raggruppamento ambiguo.
- **Shape fit — chiuso.** Ordine canonico e default presenti; `UX Assumptions` da ripulire; `Inspiration` omessa difendibile.
- **Structure — aperto.** Nessun punto d'ingresso Cose da fare, regole ripetute, tassonomia instabile, nessuna coverage map, precedenza SPEC/spine non dichiarata.
- **Prose — aperto.** Microcopy non vincolante (§9 L331), condivisione contraddittoria, copy duplicato con varianti, terminologia iOS, riferimenti baseline non risolti.
- **Accessibility — aperto.** 4 blocchi critici (contrasto righe spuntate, dark mode non tokenizzato, swipe senza alternativa, focus mancante) + 19 high.

---

## Next steps (rolling findings into un Update)

1. **Critical first:** definire coppie dark o rimuovere la promessa dark; token `ink-struck` per righe spuntate; azione "Elimina" accessibile al posto del solo swipe; token `focus-ring` e ordine di traversal; dichiarare vincolante il microcopy quotato (rimuovere/clausola §9 L331).
2. **High:** matrice condivisione scelta × parti vuote; un unico pattern `Conclusione`; Flow 3 riscritto come legacy/fuso in Flow 8; sezione canonica pager; `State Patterns` con colonna Superficie/Ambito; `Voice and Tone` come inventario unico; raggruppamento azioni dichiarato (ambito header, urgenza ordine); tassonomia componenti canonica; `lista attiva` ridefinita; coverage map CAP → flow; precedenza SPEC vs spine dichiarata.
3. **Medium/Low:** residui di terminologia, copy duplicato, contrasto placeholder/hint/bordi, semantica TalkBack, live region, font scaling, IME, mockup (badge urgenza, target 48dp, focus state, dark mockup, conteggi).
4. Dopo il fix: impostare `DESIGN.md` ed `EXPERIENCE.md` a `status: final`, `updated: 2026-11-05`, registrare "spines finalized" nel `.memlog.md`.
5. Poi: avviare `bmad-architecture`, emendare AD-7 e AD-2, derivare epiche/storie e pianificare lo sprint.

## Reviewer files

- `review-rubric.md`
- `review-structure.md`
- `review-prose.md`
- `review-accessibility.md`
