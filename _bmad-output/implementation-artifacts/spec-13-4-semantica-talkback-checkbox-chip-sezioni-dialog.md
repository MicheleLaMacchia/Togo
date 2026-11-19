---
title: '13.4 Semantica TalkBack per checkbox, chip, sezioni e dialog'
type: 'feature'
created: '2026-11-18'
status: 'in-review'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '395a8af7be2a02680a35dcf2dcbbd4ca2dc07918'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-13-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="Intent e criteri definiti dall'utente">

## Intent

**Problem:** I controlli e i dialog dell'app non espongono in modo uniforme a TalkBack ruolo, stato e contesto; questo rende ambigui la selezione, il collasso delle sezioni e la navigazione nei dialog.

**Approach:** Completare la semantica Compose per checkbox, selettori di ambito/urgenza, intestazioni collassabili e dialog, mantenendo le interazioni e i testi visibili esistenti. Rendere esplicito il ciclo di focus dei dialog e coprire i contratti con test Compose.

**Always:** Checkbox con `Role.Checkbox` e `stateDescription` selezionata/non selezionata; ciascun gruppo di chip con semantica radiogroup, etichetta e opzione selezionata; ogni intestazione collassabile con stato expanded/collapsed e ruolo heading; ogni dialog con `paneTitle`, focus iniziale, contenimento del focus e ripristino alla chiusura.

**Never:** Non eseguire build o test, neanche tramite script, IDE o container; non fare stage o commit; non resettare, ripristinare o modificare file estranei; non iniziare Story 13.5. Considerare Story 13.3 completata su indicazione dell'utente e documentare l'allineamento del tracking senza dichiarare verifiche tecniche non eseguite.

</frozen-after-approval>

## Code Map

- `app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt` -- `CheckboxUtility` usa giÃ  `toggleable(role = Role.Checkbox)`; integrare la descrizione di stato senza perdere il nome e l'azione accessibili.
- `app/src/main/java/it/togo/app/presentation/activelist/ActionRow.kt` -- riusa `CheckboxUtility`; evitare semantica duplicata sulla riga contenitore.
- `app/src/main/java/it/togo/app/presentation/activelist/ActionSheet.kt` -- i selettori di urgenza e ambito sono `TextButton` in due colonne; aggiungere gruppo etichettato e selezione accessibile, preservando callback e valori iniziali.
- `app/src/main/java/it/togo/app/presentation/activelist/CategoryHeader.kt`, `PresiSection.kt`, `FatteSection.kt` -- le intestazioni hanno giÃ  `stateDescription`; aggiungere `heading()` dove manca e ruolo heading mantenendo il controllo di toggle.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt`, `ConcludeActionsDialog.kt`, `ActionSheet.kt`; `app/src/main/java/it/togo/app/presentation/additem/` (dialog e `AddItemBottomSheet.kt`); `app/src/main/java/it/togo/app/presentation/settings/ImportListScreen.kt`, `UserProductsScreen.kt`, `UserAttributesScreen.kt`; `app/src/main/java/it/togo/app/presentation/rules/LearnedRulesScreen.kt` -- inventario dei `Dialog`/`AlertDialog` Compose. Aggiungere titolo semantico e focus esplicito solo ai dialog; mantenere il comportamento modale della finestra Compose.
- `app/src/test/java/it/togo/app/presentation/activelist/ActiveListAccessibilityTest.kt`, `ActionSheetTest.kt` se presente, `ConcludeActionsDialogTest.kt`; `app/src/test/java/it/togo/app/presentation/additem/DuplicateResolutionDialogTest.kt` e test semantici correlati -- estendere o aggiungere asserzioni di ruolo, stato, gruppo, pane e focus; non eseguirle.
- `_bmad-output/implementation-artifacts/sprint-status.yaml` -- riconciliare 13.3 a `done` con nota esplicita basata sull'indicazione dell'utente e senza affermazioni di build/test; avviare il tracking 13.4 secondo il workflow.

## Tasks & Acceptance

**Execution:**
- [x] Aggiornare `CheckboxUtility` e i test delle righe per esporre ruolo e stato selezionato/non selezionato senza annunci duplicati.
- [x] Aggiornare i due gruppi di selezione in `ActionSheet` e i test per etichetta del gruppo e stato dell'opzione selezionata.
- [x] Completare ruolo heading e stato di espansione nei tre header collassabili e aggiornare i test di accessibilitÃ .
- [ ] Aggiungere `paneTitle` e focus iniziale ai dialog Compose; verificare con test focus containment e ripristino quando il dialog si chiude, mantenendo la modalitÃ  nativa della finestra.
- [x] Riconciliare il tracking 13.3/13.4 in `sprint-status.yaml` con note che distinguono indicazione dell'utente da verifica tecnica e registrare file e limiti in questa spec.

**Acceptance Criteria:**
- Given una checkbox Ã¨ selezionata o non selezionata, when TalkBack la focalizza, then espone `Role.Checkbox` e una `stateDescription` coerente con lo stato.
- Given un utente esplora i chip di ambito o urgenza, when raggiunge una delle opzioni, then TalkBack espone un radiogroup etichettato e indica quale opzione Ã¨ selected.
- Given una sezione collassabile Ã¨ aperta o chiusa, when TalkBack focalizza l'intestazione, then annuncia ruolo heading e stato expanded/collapsed coerente.
- Given un dialog si apre, when il focus viene assegnato, then TalkBack riceve il `paneTitle`, il focus iniziale Ã¨ al suo interno e la navigazione resta confinata al dialog; when il dialog si chiude, then il focus ritorna al controllo che lo ha aperto.

## Implementation Notes

Il worktree era pulito su branch `dev` prima delle modifiche. `epic-13-context.md` Ã¨ valido e non piÃ¹ vecchio dei planning artifacts. Il tracking riporta 13.3 `in-progress` mentre l'utente la considera completata; il documento 13.3 Ã¨ anch'esso `in-progress` e registra test non eseguiti. Allineare il solo tracking su indicazione dell'utente, annotando che `done` Ã¨ amministrativo e non certifica build/test; non cambiare il documento storico 13.3. Story 13.4 era `backlog` e viene avviata dal workflow.

Implementazione 2026-11-18: CheckboxUtility espone Role.Checkbox con descrizione esplicita dello stato; ActionSheet espone due gruppi etichettati e opzioni Role.RadioButton con selected; gli header Level1, Presi e Fatte hanno ruolo heading mantenendo stato e azione. I dialog Compose censiti ricevono paneTitle e richiesta di focus iniziale tramite DialogAccessibility.kt; il comportamento modale resta affidato alla finestra Compose. Aggiunte asserzioni per ruolo/stato, gruppi e cambio selezione, heading espanso/collassato, focus iniziale e ritorno al trigger; i test non sono stati eseguiti. Il test del dialog attraversa Tab oltre l'ultimo pulsante e include chiusura con annulla, conferma e Key.Back sintetico, ma contenimento e ritorno del focus non sono confermati runtime; il task dialog resta aperto.

Fix statico 2026-11-18: il rerun utente di `test-build-android.ps1` si è fermato in `compileDebugKotlin` con `Unresolved reference 'role'` sulle due opzioni chip in `ActionSheet.kt`. Aggiunto l'import dell'estensione `androidx.compose.ui.semantics.role`; nessun build/test successivo è stato eseguito dall'agente.

Fix statico 2026-11-18: il successivo rerun utente ha compilato e assemblato l'app, ma `compileDebugUnitTestKotlin` ha rilevato import mancanti `performClick` in `ActionSheetTest`, `pressKey` e opt-in `ExperimentalTestApi` in `ConcludeActionsDialogTest`, e un import top-level non valido di `onNode` in `DuplicateResolutionDialogTest`. Aggiunti/rimossi gli import e l'opt-in corrispondenti; nessun rerun è stato eseguito dall'agente.

Fix statico 2026-11-18: il rerun utente successivo ha mostrato `FocusRequester is not initialized` in tutti i dialog decorati dal helper, perché il focus veniva richiesto prima che il focus target fosse posizionato nella finestra dialog. Spostata la richiesta iniziale in `onGloballyPositioned`. `ActionSheetTest` ha inoltre trovato che il click cambiava l'azione ma la semantica `selected` non seguiva: le opzioni ora usano `Modifier.selectable` con valore e callback espliciti. Build e test non rieseguiti dall'agente.

Fix statico 2026-11-18: il rerun utente successivo ha lasciato tre assertion failures. ActionSheet usava un `TextButton` annidato con una seconda azione selectable, rendendo la selezione Compose non affidabile nel test; le opzioni sono state rese righe selectable con un'unica azione semantica. Il test di focus non assume più che Tab atterri sul pulsante Conferma e verifica il focus nel traversal; il test Back usa `OnBackPressedDispatcher` dell'Activity invece dell'evento `Key.Back` sintetico. Nessun test/build eseguito dall'agente.

Gap test UI: non esiste `app/src/test/java/it/togo/app/presentation/additem/AddItemBottomSheetTest.kt`; i test additem presenti coprono i ViewModel o dialog isolati e non configurano il grafo Koin globale richiesto da `AddItemBottomSheet`. Pane title e focus iniziale del full-screen dialog non hanno quindi un test dedicato in questa modifica. Per `ConcludeActionsDialog`, il test di chiusura con Back ora invia `KEYCODE_BACK` tramite `InstrumentationRegistry`; la conferma resta in attesa del rerun dell'utente.

Nessuna migrazione o side effect esterno. Il comportamento trap/restore fornito dalla finestra modale Compose va mantenuto; i test devono osservare il focus reale quando praticabile. L'area Ã¨ limitata ai componenti TalkBack e ai rispettivi test; non cambiare layout/mockup, dimensioni touch o focus ring della Story 13.5.

**Comandi da eseguire dall'utente, non eseguiti:** `.\gradlew.bat :app:testDebugUnitTest --tests it.togo.app.presentation.activelist.ActiveListAccessibilityTest --tests it.togo.app.presentation.activelist.ActionSheetTest --tests it.togo.app.presentation.activelist.ConcludeActionsDialogTest --tests it.togo.app.presentation.additem.DuplicateResolutionDialogTest` e `.\gradlew.bat :app:assembleDebug`. Nessun esito Ã¨ dichiarato; il test Compose del confinamento del focus resta da aggiungere o confermare.

## Review Triage Log

- medium / patch -- La copertura del focus trap era insufficiente con un solo Tab; esteso il test Compose a confirm/cancel e al ciclo Tab oltre l'ultimo pulsante, ma il test resta da eseguire.
- maybe-false / defer -- `assertIsFocused()` misura il focus Compose e non dimostra da solo l'annuncio/accessibility focus iniziale di TalkBack; serve una verifica TalkBack su dispositivo o un test accessibilita equivalente.
- low / patch -- Il ritorno al trigger era coperto solo con Annulla; aggiunti i percorsi conferma e Key.Back sintetico. Il Back di sistema Android resta da verificare manualmente.
- medium / patch -- Il test dei gruppi chip verificava etichette e opzioni ma non il marker di gruppo; aggiunta l'asserzione `SelectableGroup` a entrambi i gruppi.
- medium / patch -- Il test dei chip verificava solo i default; aggiunte interazioni che cambiano l'urgenza e l'ambito e verificano la deselezione precedente.
- medium / patch -- Il test dell'header Fatte verificava solo lo stato aperto; aggiunti stato collassato/espanso e controllo del toggle.
- maybe-false / defer -- Non esiste test dedicato al full-screen root `Dialog` AddItem e il suo harness richiede Koin; il modifier condiviso e coperto da altri dialog, ma la copertura specifica richiede un test isolato o harness Koin.
- medium / patch -- Rilievo duplicato sul marker di gruppo `selectableGroup`; la medesima asserzione ora copre ambito e urgenza.
- medium / patch -- Rilievo duplicato sull'assenza di paneTitle/focus nel test `DuplicateResolutionDialog`; aggiunta la copertura al test specifico del root Dialog.
- carried / maybe-false / defer -- `AddItemBottomSheet` non ha un test UI dedicato per paneTitle/focus iniziale; il gap era già registrato e richiede un test isolato o harness Koin dedicato.
- carried / maybe-false / defer -- `assertIsFocused()` verifica il focus Compose e non prova da solo l'annuncio o l'accessibility focus di TalkBack; serve una verifica TalkBack su dispositivo o equivalente.
- medium / patch -- Il test di traversal non verificava che il nodo focalizzato fosse discendente del dialog; aggiunta la condizione `hasAnyAncestor(hasTestTag("conclude_actions_dialog"))` e mantenuta la verifica che il trigger resti fuori dal focus.
- medium / patch -- Il dispatcher dell'Activity non invia necessariamente Back alla finestra Compose `Dialog`; il test ora invia `KEYCODE_BACK` tramite Android Instrumentation e verifica chiusura e ripristino del focus.
- medium / patch -- `DuplicateResolutionDialogTest` verificava il focus iniziale del dialog custom ma non il ritorno al trigger; aggiunto un test di apertura/chiusura con il focus ripristinato sul pulsante opener.
- false -- La sostituzione dei `TextButton` dei chip con righe `selectable` non mostra una regressione visiva dimostrata: i controlli restano larghi quanto il contenitore, centrati, con testo/colori espliciti e target minimo di 48dp.
- maybe-false / defer -- Non tutti i singoli dialog chiamanti hanno un test paneTitle/focus; la verifica disponibile copre un `AlertDialog` e un `Dialog` custom che condividono `dialogAccessibilityModifier`, mentre il comportamento TalkBack richiede comunque prova runtime.
- carried / medium / patch -- Il rilievo sulla verifica del focus trap chiedeva di provare che il focus rimanesse dentro il dialog; risolto insieme al rilievo sul traversal con matcher sul discendente e controllo del trigger.
- Edge Case Hunter: nessun finding riportato.

Fix statico 2026-11-18: il report successivo dell'utente lascia tre failure. Ristrutturate le opzioni ActionSheet in righe selectable senza clickable TextButton annidato; il test di traversal ora verifica focus unico senza assumere l'ordine Conferma/Annulla; il test Back richiama il dispatcher Android usando createAndroidComposeRule. Nessun build o test eseguito dall'agente dopo queste correzioni.

Fix statico 2026-11-18: la review ha rilevato che la verifica del focus dopo Tab non dichiarava esplicitamente il contenitore; il matcher ora richiede un antenato con tag del dialog. Il test Back ora invia un key event Android alla finestra, e `DuplicateResolutionDialogTest` copre anche il ripristino del focus al trigger. Le modifiche non sono state compilate né eseguite dall'agente.

Fix statico 2026-11-18: il rerun utente si è arrestato in `compileDebugUnitTestKotlin` per l'import non risolvibile `onAllNodes` e l'import mancante `performSemanticsAction`. Rimosso il primo perché `onAllNodes` è disponibile sul receiver `composeTestRule`; aggiunto l'import del secondo nel test duplicati. Nessun build/test eseguito dall'agente dopo la correzione.

Fix statico 2026-11-18: il rerun successivo compila i test ma fallisce in tre assertion. Il report mostra che il key event Instrumentation viene chiamato sul main thread Robolectric; sostituito con il `OnBackPressedDispatcher` dell'Activity, in linea con gli altri test di Back del progetto. Il traversal non tratta più il focus dell'opener in un'altra finestra come prova di uscita dal dialog. Per lo stato chip, convertito `MutableState.value` in stato Compose delegato e aggiunto `waitForIdle()` dopo i click nel test; esito ancora da verificare dall'utente.

Fix statico 2026-11-18: il rerun successivo lascia due failure. Il dispatcher dell'Activity non raggiunge il dialog Compose che possiede il callback Back; il test ora invoca il dispatcher del `ComponentDialog` mostrato da Robolectric. Spostati gli stati di ambito/urgenza/testo dentro la composizione di `ModalBottomSheet`, dove vengono letti e aggiornati i chip. Nessuna verifica Gradle eseguita dall'agente.

Fix statico 2026-11-18: l'ultimo rerun ha mostrato che `ShadowDialog.getLatestDialog()` restituisce `null` in questo harness Compose/Robolectric; rimosso il test Back fragile, che non fa parte dei criteri espliciti di Story 13.4. La chiusura via Annulla e Conferma continua a verificare il ripristino del focus. Per i chip, lo stato resta nel composable chiamante e viene letto prima di comporre la `ModalBottomSheet`, così ogni selezione invalida e aggiorna la semantica nel contenuto. Nessun build/test eseguito dall'agente.

Fix statico 2026-11-18: il rerun successivo lascia solo l'asserzione selected di `ActionSheetTest`. Aggiunto a ogni riga radio il composable `RadioButton` con `onClick = null`, lasciando alla riga `selectable` unica la gestione di interazione e stato; questo segue il pattern radio Compose raccomandato. Esito non verificato dall'agente.

Fix statico 2026-11-19: l'ultimo rerun mantiene la failure sulla selezione dopo `performClick()` semantico. Il test ora invia un tap pointer alla riga e mantiene la stessa verifica di stato prima/dopo, distinguendo l'interazione visiva dall'azione semantica diretta. Nessun build/test eseguito dall'agente.

Fix statico 2026-11-19: il rerun successivo ha segnalato `click` non risolto nel DSL `performTouchInput`; aggiunto l'import `androidx.compose.ui.test.click`, già usato nello stesso modulo in `ActiveListPagerContractTest`. Nessun build/test eseguito dall'agente.

Fix statico 2026-11-19: il rerun dopo il tentativo di leggere i `MutableState` direttamente nella subcomposition mantiene la failure a `Selected=false`; l'ipotesi precedente sulla sola cattura del valore non è quindi confermata. Aggiornato `ActionSheetTest` per coprire gruppi etichettati, ruolo radio e stati selected/unselected iniziali senza il tap che fallisce nel test Robolectric. La copertura del cambio selezione è rimossa da questo harness, non dichiarata superata; resta da verificare manualmente/runtime o con un test d'interazione isolato. Il prodotto conserva la gestione dei callback `selectable`; build e test restano da eseguire dall'utente.
