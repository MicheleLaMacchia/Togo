---
title: '11.3 Oggetto dinamico in base alla scelta di condivisione'
type: 'feature'
created: '2026-11-13'
status: 'done'
baseline_commit: '6bdc42f3597a425d8702ba1b5b273282ec42bc05'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-11-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="Story 11.3 esplicitamente richiesta dall'utente">

## Intent

**Problema:** L'oggetto condiviso viene ereditato dalla spesa oppure dal fallback generico, anche quando l'utente seleziona azioni o entrambe le liste.

**Approccio:** Dato che l'utente seleziona un'opzione, quando viene aperto l'Android Sharesheet, l'Intent ACTION_SEND deve contenere in EXTRA_SUBJECT esattamente `Lista spesa TOGO` per Solo spesa, `Cose da fare TOGO` per Solo azioni e `Lista spesa e cose da fare TOGO` per Entrambe.

Implementare qui esclusivamente la Story 11.3, senza altri task. Non eseguire build/test, neppure tramite script, IDE o container; è consentito scrivere test e consegnare i comandi all'utente. Non effettuare commit, stage, reset o ripristini. Story 11.2 considerata completata per dichiarazione utente; Story 11.4+ fuori ambito.

</frozen-after-approval>

## Implementation Notes

- Indagine: checkout pulito sul branch `dev`, HEAD riportato nel frontmatter; nessun AGENTS.md nel checkout o nelle directory superiori. Contesto Epic 11 esistente, valido e più recente dei documenti di planning. Letti AC 11.3, sprint e spec 11.2.
- Percorso `oneshot`: nessuna ambiguità utente, nessuna operazione irreversibile; modifica limitata al builder esistente, al suo chiamante, a due file di test e al tracking. Implementazione e controllo statico in questa sessione per rispettare il vincolo di non creare altri task.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt`: derivare l'oggetto con un when esaustivo su ShareSelection nel builder buildShareIntent; rimuovere il parametro subject e la lettura dell'oggetto da shoppingIntent nel chiamante. La stessa selezione determina testo e oggetto. Conservare composizione del testo, MIME type, gestione vuoti e titolo del chooser: l'AC riguarda EXTRA_SUBJECT del payload, non il titolo del selettore.
- `app/src/test/java/it/togo/app/presentation/activelist/ActiveListPagerContractTest.kt`: aggiornare i chiamanti del builder e verificare i tre oggetti esatti insieme ai payload esistenti.
- `app/src/test/java/it/togo/app/presentation/activelist/ActiveListAccessibilityTest.kt`: estendere il test schermata → dialog → chooser alle tre scelte e verificare EXTRA_SUBJECT dell'Intent annidato, conservando le verifiche del testo strutturato introdotte in 11.2.
- Non modificare ShareListUseCase, formatter azioni, matrice delle parti vuote, import o storie successive.
- Discrepanza 11.2: spec già done e sprint review. Riallineare il solo sprint a done sulla dichiarazione utente, annotando che non costituisce evidenza di build/test. La 11.3 sarà consegnata in review nel tracking.
- Verifica autorizzata: lettura del diff e controllo whitespace; test e build restano da eseguire dall'utente.
- Implementazione completata nei tre file Kotlin previsti. Tutti i chiamanti del builder sono stati aggiornati; il when copre ogni valore dell'enum. I test esistenti ora controllano testo e oggetto per tutte le scelte, anche nell'Intent ACTION_SEND annidato nel chooser.
- Controllo statico locale del diff: nessun difetto rilevato nel mapping o nel passaggio al chooser; nessun rilievo rinviato. Non eseguita la review indipendente tramite subagent prevista dal workflow, perché l'utente richiede di completare qui senza altri task. Nessuna evidenza di compilazione o runtime prodotta.
- Stato finale: spec done per chiusura del workflow, sprint 11.3 review in attesa di verifica utente. Sprint 11.2 riallineato a done per dichiarazione utente; spec 11.2 invariata. Nessun commit/stage; nessuna modifica alle Story 11.4+.

## Verification

`git diff --check` non segnala errori di whitespace. Questo è un controllo statico e non compila né esegue test. Build, test e verifica manuale dello Sharesheet NON eseguiti, come richiesto.

Comandi consegnati all'utente, da eseguire dalla root con JDK e Android SDK locali configurati:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "it.togo.app.presentation.activelist.ActiveListPagerContractTest" --tests "it.togo.app.presentation.activelist.ActiveListAccessibilityTest"
.\gradlew.bat :app:assembleDebug
```

In alternativa, suite JVM completa e build APK con lo script Docker già presente:

```powershell
.\test-build-android.ps1
```

Verifica manuale da effettuare: con prodotti e azioni attivi, aprire Condividi da ciascuna pagina, selezionare a turno Solo spesa, Solo azioni ed Entrambe e scegliere un destinatario che supporti l'oggetto (ad esempio un'app email). Controllare i tre oggetti esatti degli AC e il rispettivo testo senza inviare il messaggio. Il titolo del chooser non è l'oggetto condiviso.
