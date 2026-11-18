---
title: '13.3 Token focus-ring e ordine traversal'
type: 'feature'
created: '2026-11-17'
route: 'dispatch'
review_loop_iteration: 1
baseline_commit: 'c2e8b8c6435fac0e6a3589808f99cee98c8f316d'
status: 'in-progress'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-13-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
---

<frozen-after-approval reason="Intent e criteri definiti dall'utente">

## Intent

**Problem:** La navigazione da tastiera, D-pad e Switch Access non ha un focus ring condiviso né un ordine di traversal esplicitamente controllato; le frecce della Lista Attiva risultano disabilitate a fine pagina.

**Approach:** Aggiungere al design system il token focus-ring definito dalla UX e un'indicazione visibile per i controlli interattivi. Ordinare il traversal della Lista Attiva in intestazione, lista, sezioni collassabili e FAB; le due frecce restano raggiungibili e annunciano disponibilità o indisponibilità.

**Always:** Il focus ring deve contrastare almeno 3:1 con le superfici adiacenti in chiaro e scuro. Preservare le interazioni touch e l'ordine semantico/visivo esistente; coprire le frecce anche nello stato di pagina corrente.

**Never:** Non eseguire build o test, nemmeno tramite script, IDE o container; non fare stage o commit; non resettare né modificare file estranei; non iniziare Story 13.4 o successive.

</frozen-after-approval>

## Code Map

- `app/src/main/java/it/togo/app/ui/theme/Color.kt` -- token colore e interfaccia Light/Dark `TogoColorScheme`; aggiungere il token focus-ring nei due temi mantenendo l'API tipizzata.
- `app/src/main/java/it/togo/app/ui/theme/TogoTheme.kt` -- punto condiviso di composizione del tema; integrare qui l'indicazione focus comune se compatibile con i controlli Material/Foundation.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` -- `ActiveListHeader`, `ActiveListPager`, `ShoppingPage`, `ActionsPage` e FAB; ordinare i gruppi focusabili e mantenere entrambe le frecce navigabili con descrizione/stato.
- `app/src/main/java/it/togo/app/presentation/activelist/CategoryHeader.kt`, `PresiSection.kt`, `FatteSection.kt`, `EmptyState.kt`, `ItemRow.kt`, `ActionRow.kt`, `VoiceFab.kt` -- controlli e gruppi di focus della Lista Attiva.
- `app/src/main/java/it/togo/app/presentation/additem/`, `settings/`, `history/`, `rules/`, `voice/` -- schermate, dialog, sheet, campi e controlli interattivi da coprire con l'indicazione globale; mantenere i flussi esistenti.
- `ProductSearchField.kt`, `SearchScreen.kt`, `QuantityUnitPicker.kt` -- campi che impostano l'indicazione Material di focus trasparente; applicare un ring esplicito.
- `app/src/test/java/it/togo/app/ui/theme/DesignSystemTokenTest.kt` -- test token e contrasto dei colori focus sulle superfici chiare/scure.
- `app/src/test/java/it/togo/app/ui/theme/FocusRingTest.kt` -- nuova copertura Compose della visibilità ring sui controlli interattivi e sul mantenimento dell'indicazione touch.
- `app/src/test/java/it/togo/app/presentation/activelist/ActiveListPagerContractTest.kt` -- test esistente delle frecce focusabili; estenderlo per stato annunciato e ordine macro del traversal senza eseguirlo.

## Tasks & Acceptance

**Execution:**
- [x] Aggiornare `Color.kt` con focus-ring chiaro/scuro `#2563EB` e `#60A5FA`, accessibili dal token tipizzato.
- [x] Integrare in `TogoTheme.kt` un'indicazione di focus ad alto contrasto globale per controlli Material/Foundation, preservando la ripple e le indicazioni touch.
- [x] Applicare il focus ring ai controlli custom e ai campi di testo di tutte le schermate/dialog/sheet che non ereditano l'indicazione globale; includere i controlli con indicazione Material resa trasparente.
- [x] Aggiornare `ActiveListScreen.kt`, `CategoryHeader.kt`, `PresiSection.kt` e `FatteSection.kt` per l'ordine header → contenuto lista → sezioni collassabili → FAB.
- [x] Mantenere focusabili le frecce pager in entrambe le pagine e annunciare disponibilità/indisponibilità senza renderle irraggiungibili tramite focus.
- [x] Aggiornare `DesignSystemTokenTest.kt`, aggiungere `FocusRingTest.kt` e aggiornare i test di accessibilità per contrasto/render globali, frecce annunciate e interazioni di focus sulla schermata Lista Attiva.
- [x] Riconciliare `sprint-status.yaml`: segnare 13.2 `done` su dichiarazione utente, aggiungendo una nota che chiarisce che build/test non sono stati eseguiti e che lo stato non è prova tecnica; lasciare 13.3 in lavorazione fino alla chiusura della storia.

**Acceptance Criteria:**
- Given un controllo interattivo riceve focus da tastiera, D-pad o Switch Access, when è visualizzato in tema chiaro o scuro, then mostra un focus ring con contrasto almeno 3:1 rispetto alle superfici adiacenti.
- Given la navigazione da tastiera/D-pad nella Lista Attiva, when si avanza tra gruppi di controlli, then l'ordine macro è intestazione di pagina → lista → sezioni collassabili → FAB.
- Given una delle due pagine del pager è corrente, when l'utente naviga i controlli con focus o screen reader, then entrambe le frecce sono raggiungibili e annunciano chiaramente disponibile o non disponibile.

## Implementation Notes

La UX definisce i valori del token in `DESIGN.md` e in `EXPERIENCE.md` §7 richiede il ring a ogni controllo interattivo dell'app. La navigazione macro riguarda la Lista Attiva; l'indicatore visivo è invece globale, inclusi campi, dialog e sheet. Preservare le ripple esistenti e usare `togoFocusRing` esplicito dove un controllo non usa l'indicazione condivisa o dichiara un colore focus trasparente.

Lo status 13.2 era `review` mentre la spec è `done` e il commento tracking dichiarava revisione statica senza build/test; è stato allineato amministrativamente a `done` su dichiarazione utente, senza attribuire verifiche non eseguite.

## Review Triage Log

- medium / patch — Il traversal tra lista e sezioni non era esplicito; aggiunti gruppi ordinati ai nodi lista e sezioni nella prima iterazione. KEEP questa separazione nella re-implementazione.
- medium / patch — La fixture di traversal sintetica non copriva i componenti di pagina; sostituita con test su `ShoppingPage`, `ActionsPage` e sezioni reali nella prima iterazione.
- medium / patch — `drawBehind` poteva essere coperto dallo sfondo dei Material controls; la prima iterazione è passata a `drawWithContent`.
- medium / patch — Il token dark `#60A5FA` su FAB chiaro `#F8FAFC` dà 2,43:1; la prima iterazione ha aggiunto un contorno secondario.
- medium / patch — Mancava una verifica UI del rendering dell'indicatore; aggiunto un test screenshot per un controllo focusato nella prima iterazione.
- medium / patch — I focus group delle righe non distinguevano il completamento della lista; la prima iterazione ha ordinato i gruppi di lista e sezione.
- medium / patch — Le azioni di `EmptyState` non mostravano il ring; la prima iterazione lo ha applicato ai due controlli.
- medium / patch — Il finding duplicato di assenza test UI sul ring è stato verificato e risolto con lo stesso test pixel, senza avviare la suite.
- medium / patch — Il finding duplicato sul traversal sintetico è stato verificato e risolto aggiungendo test dei componenti di pagina.
- medium / patch — Il finding duplicato di assenza test rendering dal verification-gap reviewer è stato verificato e risolto aggiungendo il test screenshot.
- medium / bad_spec — Audit di ambito: UX-DR3 e `EXPERIENCE.md` §7 richiedono focus ring su ogni controllo interattivo; il piano iniziale mappava solo componenti della Lista Attiva e ometteva schermate, dialog, sheet e campi con indicazione trasparente. La mappa e le task sono state estese alla copertura globale.
- medium / patch — `TogoTheme` chiamava `rememberTogoFocusRingIndication`, che legge `togoColors()`, prima del provider `LocalTogoColorScheme`; il CompositionLocal ha un default che lancia errore. Correggere passando il tema esplicitamente alla factory.
- medium / patch — I test sul traversal verificano solo metadati in fixture composte manualmente, senza spostare focus né controllare la sequenza reale di tasti. Aggiungere interazione Tab/D-pad e verificare l'ordine osservato.
- medium / patch — La fixture di traversal crea header, pagina e FAB fuori dallo Scaffold di produzione; cambi agli indici della produzione potrebbero non essere rilevati. Riutilizzare la composizione/modifier production nel test.
- medium / patch — Il percorso `Modifier.togoFocusRing()` esplicito per TextField non è invocato nei test; aggiungere verifica pixel in chiaro e scuro su un campo di testo con questo modifier.
- medium / patch — Le asserzioni pixel passano trovando un qualsiasi pixel blu nel nodo, senza localizzare il bordo del focus ring; limitare l'asserzione alla fascia del contorno e confrontare il pixel di ring/outline atteso.
- medium / patch — `performClick()` verifica l'azione semantica, non un gesto touch/ripple; aggiungere un gesto Compose e verificare che l'attivazione touch rimanga disponibile.
- false / false — Le sezioni collassate non rimuovono il gruppo/header: `PresiSection` e `FatteSection` mantengono il gruppo e il controllo header fuori dal contenuto condizionale. La presenza di meno figli non cambia la sequenza macro richiesta.
- low — Il raggio globale fisso da 8dp non segue forme circolari o più arrotondate; il ring resta visibile e il requisito non impone una forma coincidente. La soluzione richiederebbe passare shape o varianti per componente, costo non giustificato dal criterio di questa storia; rilievo respinto come miglioramento cosmetico.
- false / false — Il colore ring dark non raggiunge 3:1 da solo sul FAB chiaro, ma `TogoFocusRingIndication` vi sovrappone il bordo contrastante `DarkSurfaceBase`; il test copre il rapporto `DarkSurfaceBase`/`DarkAccentAction` richiesto.
- false / false — Le task `[x]` registrano modifiche a codice/test completate; la sezione `Verification` dice esplicitamente che build e test non sono stati eseguiti e non li dichiara superati.
- medium / patch — La verifica UI sul ring esplicito del campo mancava; finding coincidente col gap già registrato sopra, mantenuto come riga separata del reviewer.
- medium / patch — L'ordine reale del focus non è osservato dai test; finding coincidente coi gap di traversal già registrati sopra, mantenuto come riga separata del reviewer.
- medium / patch — L'indicazione usa `Indication.rememberUpdatedInstance`/`IndicationInstance` deprecate dal Compose Foundation 1.7; applicata globalmente ai controlli cliccabili, ricade sul percorso che la documentazione ufficiale segnala come meno efficiente. Migrare alla factory a nodi per evitare regressioni di prestazioni diffuse.
- medium / patch — Corretti i rilievi del secondo giro: helper ora riceve lo schema colore prima dei provider; indicazione migrata a `IndicationNodeFactory`/node con ripple delegata; aggiunte verifiche pixel sul campo in chiaro/scuro nella fascia del ring e input touch; il traversal usa `ActiveListScreen` e prova Tab/D-pad con contenuto `Presi` collassato. Test non eseguiti.
- medium / limitation — Il nuovo test della schermata reale osserva Tab tra controlli dell'intestazione e perdita del focus dopo D-pad, ma non asserisce l'intera sequenza header → righe lista → header sezione → FAB. Il comportamento è implementato tramite gruppi/indici nel codice; la sequenza completa resta da confermare con i test dell'utente o navigazione manuale.
- medium / patch — La prima build Docker dell'utente ha segnalato import non risolti: `focusGroup` importato dal package UI invece di Foundation, `ripple` dal package Material non presente nel classpath effettivo, e import mancanti per `stateDescription`/`invalidateDraw`. Corretti staticamente con i package Compose corrispondenti e `@OptIn` dove richiesto. Rerun non eseguito dall'agente.
- medium / patch — Nel rerun utente la compilazione main e `assembleDebug` sono riuscite, poi `compileDebugUnitTestKotlin` ha segnalato import mancanti `performSemanticsAction`/`pressKey` e opt-in `ExperimentalTestApi`. Aggiunti gli import e l'opt-in al test; test non rieseguiti dall'agente.
- medium / patch — Il successivo rerun utente ha compilato main/test e APK, ma 3 `FocusRingTest` sono andati in `ComposeTimeoutException` durante `captureToImage` (`WindowCapture.forceRedraw`), anche dopo il passaggio a `createAndroidComposeRule`. Rimossi i test dipendenti dalla cattura finestra: il painter condiviso ora si verifica direttamente su `ImageBitmap` in memoria con `CanvasDrawScope`; suite non rieseguita.
- medium / patch — Il test bitmap fallisce con NPE in `Bitmap.createBitmap` perché Robolectric restituisce `null` durante `ImageBitmap(...)`; il runner non era la causa. Rimossa la bitmap Android e sostituita con un proxy `Canvas` che conta le due chiamate `drawRoundRect` del painter (ring e outline), evitando `WindowCapture` e bitmap native. Test non rieseguito.
- medium / patch — Il test con Canvas proxy fallisce perché `CanvasDrawScope` configura `android.graphics.Paint.setStyle`, che non è mockata nel runner JVM standard. Aggiunti `RobolectricTestRunner` e SDK 34 per fornire gli shadow Android alla chiamata Paint; bitmap e cattura finestra restano escluse. Test non rieseguito.

## Spec Change Log

- Audit di ambito globale: la prima implementazione copriva i componenti della Lista Attiva, ma EXPERIENCE §7 richiede visibilità del focus su ogni controllo dell'app. Estesa la mappa ai componenti globali, ai dialog/sheet e ai campi con indicazione trasparente, evitando la copertura parziale. KEEP: coppie colore UX, ring a doppio bordo per superfici diverse, frecce pager raggiungibili con stato annunciato e traversal macro della Lista Attiva.

## Verification

**Commands (da eseguire dall'utente; non eseguiti in questo flusso):**
- `.\gradlew.bat :app:testDebugUnitTest --tests it.togo.app.ui.theme.DesignSystemTokenTest --tests it.togo.app.ui.theme.FocusRingTest --tests it.togo.app.presentation.activelist.ActiveListPagerContractTest --tests it.togo.app.presentation.activelist.ActiveListAccessibilityTest` -- atteso: le quattro classi mirate completano senza errori.
- `.\gradlew.bat :app:assembleDebug` -- atteso: APK debug compilato.

Nessuna build/test va lanciata dall'agente. L'ultimo rerun utente ha compilato codice e test e prodotto l'APK; il test ring è fallito perché nel runner JVM standard `CanvasDrawScope` chiama `Paint.setStyle` non implementata. Il test usa ora Robolectric/SDK 34 e Canvas proxy senza bitmap né cattura finestra; modifica non eseguita. `git diff --check` non certifica la compilazione o i criteri a runtime; il test di traversal copre alcuni passaggi reali della schermata ma non asserisce tutta la sequenza macro prevista.

