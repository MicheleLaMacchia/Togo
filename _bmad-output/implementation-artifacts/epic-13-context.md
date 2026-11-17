# Epic 13 Context: Accessibilità e Qualità UX (Focus, Contrasto, Dark Mode, Semantica)

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Rendere l'app accessibile con TalkBack, tastiera/D-pad e Switch Access, mantenendo il contrasto dichiarato in tema chiaro e scuro e rendendo visibili gli stati di focus. Le storie coprono token e comportamento trasversali dell'interfaccia; questa Epic orienta interventi mirati senza introdurre styling parallelo per singole schermate.

## Stories

- Story 13.1: Token dark mode coppie *-dark
- Story 13.2: Token ink-struck per righe spuntate
- Story 13.3: Token focus-ring e ordine traversal
- Story 13.4: Semantica TalkBack per checkbox, chip, sezioni e dialog
- Story 13.5: Mockup allineati (badge urgenza, target 48dp, stato focus, dark mockup)

## Requirements & Constraints

- I componenti dell'interfaccia devono usare coppie di token light/dark; in tema scuro superfici, testi, badge, accenti e bordi mantengono contrasto e visibilità.
- Le righe spuntate devono restare leggibili con un token dedicato, contrasto almeno 4,5:1, senza ridurre l'opacità del testo; barratura e checkbox offrono ridondanza visiva.
- La navigazione da tastiera/D-pad/Switch Access richiede focus ring ad alto contrasto, ordine prevedibile e frecce del pager focusabili e annunciate.
- TalkBack deve esporre ruolo, stato e descrizione appropriati per checkbox, chip, intestazioni collassabili e dialog.
- I mockup di riferimento devono riflettere badge urgenza, target touch di almeno 48 dp, focus visibile e una versione dark.

## UX & Interaction Patterns

- La dark mode copre i componenti condivisi (intestazioni di categoria, badge, FAB, pulsanti e bordi), senza testo invisibile o controlli che si confondono con lo sfondo.
- L'ordine di traversal previsto è intestazione di pagina, lista, sezioni collassabili e FAB.
- I controlli TalkBack annunciano stato e ruolo; i dialog espongono titolo, focus iniziale, contenimento del focus e ritorno del focus.
