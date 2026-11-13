---
title: '11.2 Formato testo [DA FARE] per condivisione azioni'
type: 'feature'
created: '2026-11-13'
status: 'done'
baseline_commit: '89082d9efcb7fecdc905ed3896157363273f5294'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-11-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="Story 11.2 esplicitamente richiesta dall'utente">

## Intent

**Problema:** La condivisione di Solo azioni ed Entrambe introdotta in 11.1 produce soltanto righe di testo, prive del formato strutturato richiesto dalla Story 11.2.

**Approccio:** Produrre un blocco `[DA FARE]` con header `+ AMBITO`, sottogruppi `- URGENZA` e testo azione preceduto da due spazi. Ordinare per Casa, Pratiche, Salute, Lavoro, Altro, poi Urgente, A tempo perso, Eccezionale, infine alfabeticamente secondo la convenzione già presente nel dominio. Escludere le azioni spuntate e non aggiungere quantità. Solo azioni condivide il blocco; Entrambe lo aggiunge dopo la Spesa.

L'utente richiede implementazione completa della sola 11.2 e consente di scrivere test, ma vieta qualsiasi esecuzione di build/test e qualsiasi commit, stage, reset o ripristino. Le storie 11.3–11.5 e l'import restano fuori ambito.

</frozen-after-approval>

## Implementation Notes

- Indagine: checkout iniziale pulito, branch `dev`, ultimo commit dedicato alla 11.1. Nessun `AGENTS.md` trovato nel checkout o nelle directory superiori. Il contesto Epic 11 è valido e più recente dei documenti di planning.
- Percorso scelto: `oneshot`; nessuna decisione utente mancante, nessuna operazione irreversibile, modifica circoscritta al formatter, suo collegamento, test e tracking.
- `app/src/main/java/it/togo/app/domain/action/ActionShareFormatter.kt`: nuova funzione pura `buildSharedActionsText`, riuso di `groupActionsByScope` da `domain/model/ActionGroup.kt`, filtro autonomo delle spuntate. Header dalle label con `uppercase(Locale.ROOT)`, non dai nomi enum; alfabetico coerente con la UI, senza distinzione di maiuscole e con accenti preservati.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt`: sostituire il `joinToString` delle azioni con il formatter. Riutilizzare `buildShareIntent` per la selezione e il separatore di due newline tra Spesa e Azioni. Nessuna modifica all'oggetto, alla matrice delle scelte vuote o a `ShareListUseCase`.
- Il testo persistito è già monoriga tramite `ActionTextValidator`: preservare numeri, accenti e spazi interni del testo libero. Non aggiungere quantità o unità. Input vuoto o tutto spuntato restituisce stringa vuota, conservando il comportamento attuale senza blocchi vuoti.
- `app/src/test/java/it/togo/app/domain/action/ActionShareFormatterTest.kt`: scrivere verifiche del testo esatto con tutti gli ambiti e tutte le urgenze, alfabetico, esclusione spuntate e gruppi vuoti, input vuoto e testo invariato.
- `app/src/test/java/it/togo/app/presentation/activelist/ActiveListPagerContractTest.kt`: usare il formatter reale nel test dei payload Solo spesa, Solo azioni ed Entrambe.
- Tracking 11.1: la spec è già `done`, il tracking è `review` e l'utente la dichiara completata. Riallineare il solo tracking a `done` annotando la provenienza; non è evidenza di build/test superati. La 11.2 sarà consegnata in `review` nel tracking.
- Verifica autorizzata: sola lettura del codice/diff e review statica; nessuna build o test, neppure tramite IDE, container o altri agenti.
- Implementazione completata nei quattro file Kotlin previsti. Il formatter rimuove soltanto il newline finale, senza applicare trim al testo delle azioni. Scritti quattro test del formatter e aggiornato il test dei payload delle tre scelte. `git diff --check` non segnala errori di whitespace; questo controllo non compila e non esegue test.
- Dopo la review statica indipendente: aggiunti tre casi al formatter (sette test totali) e un test del percorso schermata → dialog → chooser in `app/src/test/java/it/togo/app/presentation/activelist/ActiveListAccessibilityTest.kt`. Il fake repository dello stesso file ora accetta azioni; gli altri test mantengono il default vuoto. Nessuna modifica aggiuntiva al codice di produzione.
- Stato finale: spec `done` secondo il workflow, sprint 11.2 `review` in attesa di verifica utente; nessuna build, test o verifica manuale runtime eseguita. Nessun commit o stage. Storie 11.3+ non modificate. La review non ha identificato difetti funzionali da correggere; i quattro rilievi di copertura sono stati gestiti scrivendo test.

## Review Triage Log

- medium → patch: il test formatter + builder non attraversava il collegamento nella schermata e non avrebbe rilevato il ripristino del vecchio `joinToString`. Aggiunto test Compose/Robolectric che conferma Solo azioni ed Entrambe e legge il payload `ACTION_SEND` dentro il chooser avviato dalla schermata. Test scritto, non eseguito.
- low → patch: mancava un confronto tra azioni accentate o diverse soltanto per maiuscole nello stesso gruppo. Aggiunto output esatto e input invertito per proteggere la convenzione `Locale.ROOT` e lo spareggio sul testo originale già usati dal dominio.
- low → patch: mancava il caso di testo uguale con ambito o urgenza diversi, valido secondo la deduplica esistente. Aggiunto test che conserva tutte le righe nei rispettivi gruppi; nessuna deduplica introdotta.
- low → patch: mancavano testi azione che iniziano con `+`, `-` o `[DA FARE]`. Aggiunto test che ne controlla conservazione e indentazione a due spazi; il formatter non interpreta marcatori nel testo libero.

## Verification

Build e test NON eseguiti, su vincolo esplicito dell'utente. I comandi seguenti sono esclusivamente una consegna per l'esecuzione manuale dalla root del progetto:

```powershell
# Test JVM e build APK tramite lo script Docker esistente
.\test-build-android.ps1
```

In alternativa, con JDK e Android SDK locali configurati:

```powershell
# Verifica mirata del formatter e dei payload delle tre scelte
.\gradlew.bat :app:testDebugUnitTest --tests "it.togo.app.domain.action.ActionShareFormatterTest" --tests "it.togo.app.presentation.activelist.ActiveListPagerContractTest" --tests "it.togo.app.presentation.activelist.ActiveListAccessibilityTest"
# Build debug
.\gradlew.bat :app:assembleDebug
```

Verifica manuale successiva: con azioni di ambiti e urgenze diversi, alcune spuntate, aprire Condividi e controllare Solo azioni ed Entrambe nello Sharesheet. Attesi header maiuscoli, due spazi davanti al testo, ordine contrattuale, nessuna azione spuntata e nessuna quantità aggiunta alle azioni. In Entrambe il testo Spesa precede il blocco azioni, separato da una riga vuota. Questa verifica non è stata eseguita.
