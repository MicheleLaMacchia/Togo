---
title: '11.4 Matrice scelta × parti vuote'
type: 'feature'
created: '2026-11-16'
status: 'done'
baseline_commit: 'eaeaddd710a3bb65698bfb068a6975cd6cba6e8b'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-11-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="Story 11.4 richiesta fino alla consegna dall'utente">

## Intent

**Problema:** Il dialogo consente di selezionare parti senza voci e si apre anche quando non esiste alcun contenuto condivisibile.

**Approccio:** Applicare la matrice ai conteggi di prodotti e azioni non spuntati. Given spesa vuota e azioni presenti, when si apre Condividi, then `Solo spesa (0)` ed `Entrambe (M)` sono disabilitate con nota `Nessuna voce`, mentre `Solo azioni (M)` è abilitata. Given azioni vuote e spesa presente, when si apre Condividi, then solo `Solo spesa (N)` è abilitata e le altre due opzioni mostrano `Nessuna voce`. Given entrambe le parti presenti, when si apre Condividi, then tutte e tre le opzioni sono abilitate senza note di vuoto. Given nessuna voce non spuntata in entrambe le parti, when si tocca l'icona da qualunque pagina, then appare `Non c'è nulla da condividere` senza aprire il dialogo o lo Sharesheet.

Conservare il default della pagina corrente previsto dalla UX; se quella parte è vuota, impedire la conferma finché l'utente non seleziona un'opzione disponibile. Ricalcolare la disponibilità se cambiano i conteggi a dialogo aperto. Mantenere formati e oggetti delle storie precedenti.

Solo Story 11.4; ShareListUseCase e Story 11.5+ fuori ambito. MAI eseguire build o test, nemmeno via script, IDE o container; test scrivibili e comandi da consegnare all'utente. Nessuno stage, commit, reset o ripristino; preservare lavoro preesistente. Considerare 11.3 completata sulla dichiarazione utente e documentare la discrepanza di tracking.

</frozen-after-approval>

## Implementation Notes

- Indagine iniziale: checkout pulito su `dev`; HEAD canonico nel frontmatter. Nessun AGENTS.md nel checkout o nelle directory superiori. Contesto Epic 11 valido; riletti epics, sprint, UX EXPERIENCE §4.10 e DESIGN §7.11, spec 11.3 e codice. Indagine indipendente in sola lettura conferma matrice e punti di modifica.
- Percorso oneshot: nessuna lacuna di intento rispetto alle fonti, nessun effetto irreversibile; modifica piccola in un file di produzione, due test esistenti e tracking/spec. Il default rimane quello esplicito della UX anche se non disponibile; non si introduce una selezione automatica dell'altra parte.
- Code Map: `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt`, `ShareSelectionDialog`/`ShareSelectionOption`: aggiungere disponibilità, nota e guardia della conferma; callback dell'icona: intercettare 0/0 con snackbar esistente. Riutilizzare `activeCount` e `activeActionsCount`, senza ricontare i gruppi né includere Presi/Fatte. Builder testo/oggetto invariato; Solo azioni già bypassa ShareListUseCase.
- `app/src/test/java/it/togo/app/presentation/activelist/ActiveListPagerContractTest.kt`: coprire matrice, semantica disabled e callback, default, conferma e ricomposizione dei conteggi.
- `app/src/test/java/it/togo/app/presentation/activelist/ActiveListAccessibilityTest.kt`: aggiornare aspettativa obsoleta del dialogo 0/0; coprire entrambe le pagine, sole voci spuntate e condivisione con una parte vuota.
- Discrepanza 11.3 verificata: spec già `done`, sprint `review`, commit corrente dedicato alla 11.3. Riallineare solo sprint a `done` su dichiarazione utente, senza attestare test/build. Non alterare le note storiche della spec 11.3.
- Copy del feedback: forma canonica degli AC e delle sezioni UX operative, apostrofo semplice e nessun punto finale. Non usare il vecchio messaggio generico per il nuovo caso 0/0.
- Verifica consentita: sola lettura del codice/diff, review statica indipendente e `git diff --check`. Compilazione, esecuzione test e verifica dispositivo restano all'utente.
- Implementata la matrice con `enabled` sia sulla riga selezionabile sia sul RadioButton; label e nota usano il token disabled esistente. La guardia della callback impedisce anche attivazioni semantiche dirette di scelte/conferma disabilitate. La conferma segue i conteggi a ogni ricomposizione senza azzerare la scelta.
- Scritti test dei due casi con una parte vuota per ciascuna pagina, esclusione Presi/Fatte, payload e oggetto della parte disponibile, tutte le opzioni disponibili e variazione dei conteggi mentre il dialogo è aperto. Aggiornato il test che si aspettava il dialogo 0/0.
- Correzione dopo review, che supera la scelta iniziale dello snackbar: il caso 0/0 usa un Toast breve per non attendere dietro ai feedback undo. Il Toast precedente viene cancellato al tap successivo e all'uscita dalla schermata. I test leggono ShadowToast, azzerato per ciascuna pagina, e includono checkout con undo ancora visibile e tap ripetuti. Aggiunte asserzioni del default nella schermata integrata e transizioni dei conteggi per tutte e tre le selezioni.
- Review indipendente Blind Hunter completata; nessun layer configurato omesso. Sei rilievi analizzati sotto, cinque risolti e uno rinviato relativo al caricamento iniziale generico. Riesame statico delle correzioni senza nuovi difetti concreti rilevati; non è prova di compatibilità o runtime.
- Stato finale: spec `done` per chiusura workflow, sprint 11.4 `review` in attesa della verifica utente; 11.3 `done` sulla dichiarazione utente. Nessuno stage/commit/reset/ripristino. ShareListUseCase, Story 11.5+ e formati/oggetti non modificati.

## Review Triage Log

- R1 — medium → patch: il feedback 0/0 poteva attendere nello stesso host degli snackbar undo. La formulazione del reviewer sul checkout indefinito è corretta dal timer di 5 secondi in `ActiveListViewModel.showCheckoutSnackbar`, ma il ritardo è reale. Il Toast ora è indipendente dall'host; test checkout → Condividi scritto, non eseguito.
- R2 — low → defer: il ViewModel preesistente inizia con `ActiveListUiState()` a conteggi zero prima delle emissioni combinate di repository e tassonomia. Un tap anticipato può quindi interpretare il caricamento come lista vuota; dopo l'emissione il conteggio corretto è usato al tap successivo. La matrice dei dati caricati è implementata; il contratto generale di caricamento della schermata resta separato e il limite è annotato in `deferred-work.md`, senza aggiungere API di loading nella 11.4.
- R3 — low → patch, stessa radice di R1: tap ripetuti avrebbero accodato coroutine `showSnackbar`. Il nuovo ramo sincrono cancella/sostituisce il Toast e non crea code nello snackbar host; regressione con tap ripetuti scritta.
- R4 — medium → patch: la seconda pagina del test poteva leggere il feedback della prima. Ogni iterazione ora azzera ShadowToast prima del tap, quindi esige un nuovo messaggio.
- R5 — low → patch: mancavano transizioni dei conteggi con SHOPPING/ACTIONS selezionati. Aggiunti due test per svuotamento della parte non selezionata, svuotamento di quella selezionata e ripristino; nessuno eseguito.
- R6 — low → patch: i test integrati selezionavano la parte disponibile senza osservare il default. Ora verificano selezione e abilitazione della conferma per la pagina corrente prima di cambiare opzione.

## Verification

Build, test e verifica manuale runtime NON eseguiti per vincolo utente. `git diff --check` eseguito come solo controllo statico del whitespace; non costituisce verifica di compilazione o comportamento.

Comandi per l'utente dalla root, con JDK e Android SDK locali configurati, **NON eseguiti dall'agente**:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "it.togo.app.presentation.activelist.ActiveListPagerContractTest" --tests "it.togo.app.presentation.activelist.ActiveListAccessibilityTest"
.\gradlew.bat :app:assembleDebug
```

In alternativa, suite JVM completa e build APK con lo script Docker esistente, anch'esso **NON eseguito**:

```powershell
.\test-build-android.ps1
```

Verifica manuale da effettuare da Spesa e da Da fare: entrambe le parti popolate (tutte le scelte abilitate), solo prodotti (solo Spesa), solo azioni (solo Azioni), nessuna voce attiva e sole voci spuntate (messaggio senza dialogo). Controllare nota `Nessuna voce`, default della pagina e conferma disabilitata sulla parte vuota, testo/oggetto della scelta valida senza inviare messaggi. Con TalkBack verificare stato non disponibile e nota associata a ciascuna opzione; verificare leggibilità in entrambi i temi. Nessuna di queste verifiche è stata eseguita.
