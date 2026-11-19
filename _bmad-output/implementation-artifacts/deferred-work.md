- source_spec: `_bmad-output/implementation-artifacts/spec-riparazione-errori-compilazione.md`
  summary: Iniettare correttamente `AddItemViewModel` nella sheet e implementare callback/dismiss del flusso di conferma.
  evidence: La sheet usa `viewModel()` senza factory per il ViewModel con dipendenze obbligatorie; il ViewModel persiste direttamente e non consuma la callback host, quindi l'apertura runtime può fallire e la sheet può non chiudersi. Non è necessario per risolvere i riferimenti di compilazione.
- source_spec: `_bmad-output/implementation-artifacts/spec-riparazione-errori-compilazione.md`
  summary: Collegare il testo confermato dalla voce al parsing e all'aggiunta di un prodotto.
  evidence: `UiEvent.VoiceCommand` è ricevuto ma il ramo del ViewModel non esegue azioni; il parser e il flusso di aggiunta non sono collegati in questo fix limitato alla compilazione.
- source_spec: `_bmad-output/implementation-artifacts/spec-riparazione-errori-compilazione.md`
  summary: Ridurre i lookup sequenziali dei prodotti durante la ricostruzione della lista.
  evidence: `ActiveListViewModel.buildUiState` interroga il catalogo una volta per item a ogni emissione di lista/tassonomia; è un rischio prestazionale indipendente dalla compilazione.
- source_spec: `_bmad-output/implementation-artifacts/spec-riparazione-errori-compilazione.md`
  summary: Gestire errori Room durante i lookup usati dalla lista e dalla condivisione.
  evidence: Eccezioni da `CatalogRepository.getById` possono interrompere la raccolta o la condivisione; il log attuale riguarda errori di compilazione, non resilienza runtime.
- source_spec: `_bmad-output/implementation-artifacts/spec-riparazione-errori-compilazione.md`
  summary: Mostrare all'utente gli errori della ricerca prodotti in AddItem.
  evidence: Il ViewModel registra l'errore nella mappa `errors`, ma il campo ricerca non riceve né visualizza tale messaggio; è un miglioramento UX separato.
- source_spec: `_bmad-output/implementation-artifacts/spec-riparazione-errori-compilazione.md`
  summary: Implementare e testare il flusso completo di modifica di una voce esistente.
  evidence: Il tap ripristinato invia `UiEvent.EditItem`, ma `handleEditItem` è ancora vuoto; completare la modifica richiede un flusso item-aware e una factory ViewModel, oltre al perimetro di compilazione.
- source_spec: `_bmad-output/implementation-artifacts/spec-riparazione-errori-compilazione.md`
  summary: Aggiungere test automatizzati per il gesto swipe-to-delete.
  evidence: La riga ora usa un gesture handler per invocare la cancellazione, ma la ricerca nel repository non trova test che asseriscano il callback dopo uno swipe valido; l'utente eseguirà la build separatamente e i test non sono necessari alle correzioni richieste.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Rendere accessibile con TalkBack l'eliminazione di una voce della lista, mantenendo distinta l'azione visuale swipe-to-delete.
  evidence: `app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt` espone l'icona swipe `Elimina` con contentDescription ma senza click/custom action; l'eliminazione avviene solo nel gesture handler, non utilizzabile tramite focus e azione TalkBack. Il fix richiede un'azione accessibile dedicata oltre il perimetro della storia 6-1.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Collegare il pulsante “Storico” a `HistoryScreen`/`HistoryViewModel`.
  evidence: `ActiveListScreen` invia `UiEvent.NavigateToHistory`, `ActiveListViewModel` ignora l'evento e `AppNavigator` non ha una destinazione History. Preesistente alla 6.1.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Annunciare con TalkBack il passaggio di una voce da lista attiva a “Presi” e gestire il focus dopo il toggle.
  evidence: `handleCheckOff` aggiorna lo stato ma non esiste live region né gestione del focus; il movimento è preesistente alla 6.1.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Estrarre in string resources le nuove etichette italiane introdotte dalla 6.1.
  evidence: descrizioni riga, header “Presi”, checkbox e VoiceFab usano stringhe hardcoded; il progetto applica lo stesso pattern altrove, quindi la localizzazione va trattata come storia separata.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Formattare quantità e unità nel dialog duplicati con `quantityLabel` e nomi unità estesi.
  evidence: `DuplicateResolutionDialog` interpola `Double`/`StandardUnit` raw nel messaggio; preesistente alla 6.1.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Rimuovere il workaround `GlobalContext.getOrNull()` da `TogoApplication` e isolare Koin nei test Robolectric.
  evidence: il guard è stato aggiunto per evitare il doppio avvio nei test; in produzione può mascherare un contesto Koin già inizializzato. Nessun impatto utente.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Correggere la contraddizione sull'invio della voce ai servizi remoti in `epic-6-context.md`.
  evidence: il testo dichiara sia l'uso di servizi remoti sia la garanzia di non inviare dati; il fix modifica file agent-context, non codice della storia.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Coprire con screenshot o semantic-tree test i fix di rendering (bordo `CategoryHeader`, header “Presi”, FAB).
  evidence: i fix sono fissati da scan testuali; manca un'infrastruttura screenshot nel progetto.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Aggiungere un test Robolectric per il ramo permesso microfono negato e il messaggio snackbar.
  evidence: il nuovo test copre registrazione/rilascio del launcher; il ramo di diniego del permesso resta verificato solo staticamente e manualmente.

## Deferred from: code review of spec-6-1-etichette-talkback-e-semantica-accessibile.md (2026-10-20)
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Introdurre una query batch o una cache per i prodotti in `ActiveListViewModel.buildUiState`.
  evidence: `catalogRepository.getById` è chiamato per ogni item a ogni emissione; il pattern preesiste ed è esteso dal nuovo loop checked. Richiede un'API repository o una cache condivisa.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Centralizzare la formattazione delle quantità e usare l'unità nel dialog duplicati.
  evidence: `ItemRow` usa la virgola italiana; `DuplicateResolutionDialog` e `ShareListUseCase` mostrano `Double` raw; `StandardUnit.format` usa il punto. La correzione richiede un formatter condiviso e la propagazione dell'unità.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Rimuovere l'hint categoria dal parser vocale solo con word boundary e senza perdere il qualificatore.
  evidence: "pasta integrale" diventa "integrale" perché l'hint viene rimosso senza confine di parola; la logica difettosa preesiste al diff.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Aggiungere test strumentati TalkBack su device/emulatore.
  evidence: i test Compose verificano l'albero semantico, non gli annunci reali; manca una suite `androidTest` nel progetto.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Coprire con screenshot o semantic-tree test i fix di rendering (bordo `CategoryHeader`, header "Presi", FAB).
  evidence: già tracciato sopra; i fix sono fissati da scan testuali e manca un'infrastruttura screenshot/pixel.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Allineare lo stato delle spec di riparazione (`ready` con checklist non spuntate).
  evidence: l'audit trail è incoerente; la correzione modifica altre spec e va fuori dal perimetro della 6.1.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Correggere la contraddizione privacy/lingua in `epic-6-context.md`.
  evidence: già tracciato sopra; il testo dichiara sia l'uso di servizi remoti sia la garanzia di non inviare dati.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Tracciare il cambio `border-crisp-dark` `#475569` → `#64748B` nella Dark Mode 6.3 o in un change log.
  evidence: modifica retroattiva di DESIGN/epics/spec-1.5, fuori dal perimetro della 6.1.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md`
  summary: Separare o motivare le modifiche estranee alla 6.1 nel diff (dominio, parser, wizard, unità, tema, Docker).
  evidence: il diff dal baseline include cambi fuori dal perimetro accessibilità e viola il vincolo di non cambiare regole/stile oltre il necessario; separare i commit o documentare la deroga.

## Deferred from: audit 6.4 Privacy Audio e Audit Performance (2026-10-21)

- source_spec: `_bmad-output/implementation-artifacts/spec-6-4-privacy-audio-e-audit-performance.md`
  summary: Implementare e cablare il rilevamento duplicati lato lista, oppure rimuovere il dialog duplicati non raggiungibile.
  evidence: `app/src/main/java/it/togo/app/di/DomainModule.kt:42-43` ha `DetectDuplicateUseCase` e `ResolveDuplicateUseCase` commentate; non esistono classi use case nel dominio e `DuplicateResolutionDialog` non ha call site. La metrica di deduplicazione esatta (SM-2) resta BLOCCATA.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-4-privacy-audio-e-audit-performance.md`
  summary: Cablare o rimuovere il flusso legacy `VoiceConfirmationViewModel`/`VoiceConfirmationSheet`.
  evidence: entrambi sono registrati in `PresentationModule.kt` ma non hanno call site UI; il flusso realmente usato è `VoiceBottomSheet`. La privacy è coperta da `onCleared`, ma il codice morto va deciso in una storia dedicata.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-4-privacy-audio-e-audit-performance.md`
  summary: Eliminare l'N+1 di `ActiveListViewModel.buildUiState` e i filtri annidati della tassonomia.
  evidence: `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt:95-96` e `107-109` eseguono un `getById` per prodotto; `117-123` filtrano L2 per ogni L1 e L3 per ogni L2 a ogni emissione. Richiede `getByIds`/cache e mappe `level1Id -> L2`/`level2Id -> L3`, fuori perimetro 6.4.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-4-privacy-audio-e-audit-performance.md`
  summary: Aggiungere un limite esplicito alla lunghezza di `LEARNED_RULE.userExpression`.
  evidence: `app/src/main/java/it/togo/app/data/database/entity/LearnedRuleEntity.kt:18` persiste l'espressione normalizzata senza bound; è l'eccezione privacy documentata, ma una frase molto lunga potrebbe restare in DB. La modifica richiede una regola di dominio/normalizzazione e test dedicati.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-4-privacy-audio-e-audit-performance.md`
  summary: Correlare in modo forte ogni risultato del recognizer alla richiesta che lo ha originato.
  evidence: `AndroidVoiceRecognitionGateway.kt:43-49` e `148-152` invalidano i risultati dopo reset/stop, ma un risultato della richiesta precedente che arrivasse dopo un nuovo `startListening()` non è distinguibile senza request id/request code dedicato. Rischio raro ma non eliminabile con l'attuale `ActivityResultLauncher` condiviso.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-4-privacy-audio-e-audit-performance.md`
  summary: Eseguire il protocollo tempi manuali/voce su device e registrare le mediane.
  evidence: `.\test-android.ps1` e `.\build-android.ps1` sono passati (486/486) e 6.4 è chiusa con deroga accettata: i tempi manuali/voce restano `BLOCCATA` perché richiedono misurazione su device reale (nessuna strumentazione in produzione). Protocollo al §4 di `audit-6-4-privacy-performance.md`, 10 run manuali + 10 vocali.
- source_spec: `_bmad-output/implementation-artifacts/spec-6-4-privacy-audio-e-audit-performance.md`
  summary: Aggiungere un corpus di utterance libere/casi manuali per SM-1, se il vincolo “nessuna riga inventata” viene revocato.
  evidence: `CatalogAcceptanceSupport.acceptanceCases()` (`app/src/test/java/it/togo/app/acceptance/CatalogAcceptanceSupport.kt:89-132`) deriva solo `CANONICAL_PRODUCT` e `SYNONYM`; il test non misura il parsing di frasi complete. La scelta evita righe inventate ed è documentata nell'audit.

## Deferred from: code review of spec-7-1-aggiunta-prodotto-canonico.md (2026-10-22)

- source_spec: `_bmad-output/implementation-artifacts/spec-7-1-aggiunta-prodotto-canonico.md`
  summary: Wizard 5-step non raggiungibile dalla UI di produzione.
  evidence: `ProductCategorizationWizard.kt` esiste ma non è stato cablato; la Story 7.1 usa `NewProductForm`. Fuori perimetro 7.1.
- source_spec: `_bmad-output/implementation-artifacts/spec-7-1-aggiunta-prodotto-canonico.md`
  summary: Bug `NewProductDraft.toCanonicalProduct` con `id = finalLevel3Id`.
  evidence: `NewProductDraft.kt:172` imposta `id = finalLevel3Id`; la Story 7.1 non usa quella classe. Va corretto o rimosso insieme al wizard.
- source_spec: `_bmad-output/implementation-artifacts/spec-7-1-aggiunta-prodotto-canonico.md`
  summary: Nessun percorso di modifica o eliminazione per i prodotti user-defined.
  evidence: dopo la creazione il prodotto resta nel catalogo senza UI dedicata. Serve una storia di gestione del catalogo utente (rinomina, eliminazione, controllo dei duplicati storici).
- source_spec: `_bmad-output/implementation-artifacts/spec-7-1-aggiunta-prodotto-canonico.md`
  summary: `handlePreviousStep` con form aperto non è raggiungibile dalla UI.
  evidence: `AddItemBottomSheet.kt` nasconde la barra di navigazione quando `isNewProductFormVisible` è true; il tasto di sistema chiude l'intero bottom sheet. Il ramo è coperto da test ma non esposto. Da decidere se aggiungere un `BackHandler` o rimuovere il ramo.
- source_spec: `_bmad-output/implementation-artifacts/spec-7-1-aggiunta-prodotto-canonico.md`
  summary: Fallimento del caricamento tassonomia senza retry nel form.
  evidence: `AddItemViewModel.kt:202-211` mostra "Errore durante il caricamento delle categorie" ma lascia i dropdown vuoti e l'errore viene azzerato al primo evento successivo. Richiede uno stato di retry o la chiusura del form.
- source_spec: `_bmad-output/implementation-artifacts/spec-7-1-aggiunta-prodotto-canonico.md`
  summary: Allineare `tools/generate_catalog.py` al nuovo DDL `AUTOINCREMENT`.
  evidence: il generatore dell'asset usa ancora `id INTEGER NOT NULL PRIMARY KEY`; Room ignora `AUTOINCREMENT` nella validazione, quindi non c'è impatto runtime, ma il prossimo `catalog.db` generato divergerebbe dall'entità.

## Deferred from: spec-7-2-proposta-nuovo-livello-3.md (2026-10-22)

- source_spec: `_bmad-output/implementation-artifacts/spec-7-2-proposta-nuovo-livello-3.md`
  summary: Rendere atomico il calcolo del `sortOrder` dei nuovi Livelli 3.
  evidence: `CatalogDao.nextLevel3SortOrder` (`CatalogDao.kt:89-90`) e `insertLevel3` (`CatalogDao.kt:97-98`) sono due statement separati: due creazioni concorrenti nello stesso Livello 2 potrebbero ricevere lo stesso `sortOrder`. Rischio accettato in 7.2 (utente singolo + guardia nel ViewModel); da rivalutare con una transazione Room dedicata se in futuro la creazione diventa concorrente.

## Deferred from: code review of spec-7-2-proposta-nuovo-livello-3.md (2026-10-22)

- source_spec: `_bmad-output/implementation-artifacts/spec-7-2-proposta-nuovo-livello-3.md`
  summary: Allineare `CategoryAssigner`/`CategoryCorrector` alla presenza di L3 user-defined.
  evidence: `CategoryAssigner.kt:38,62,76,97` e `CategoryCorrector.kt:62` escludono gli L3 user-defined da default, fallback, regole apprese e correzione. La pipeline di assegnazione automatica non è cablata in produzione e la spec 3.6 impone L3 canonici non user-defined; quando verrà collegata, decidere se consentire gli L3 user-defined per i prodotti user-defined.
- source_spec: `_bmad-output/implementation-artifacts/spec-7-2-proposta-nuovo-livello-3.md`
  summary: Aggiungere un test sheet-level per il dialog duplicato L3.
  evidence: `AddItemBottomSheet.kt:409-416` monta `DuplicateLevel3WarningDialog`, ma nessun test compone la sheet; i test esistenti coprono solo il dialog isolato e lo stato del ViewModel. Estrarre i mount in un composable stateless o usare un harness Koin/ViewModel.
- source_spec: `_bmad-output/implementation-artifacts/spec-7-2-proposta-nuovo-livello-3.md`
  summary: Coprire i consumer di `getTaxonomy()` con un L3 user-defined.
  evidence: la rimozione del filtro in `CatalogDao.kt:50` rende visibile il nuovo L3 a lista attiva, condivisione e assegnazione; i test esistenti verificano solo DAO/repository e non il testo condiviso o le opzioni dei consumer.
- source_spec: `_bmad-output/implementation-artifacts/spec-7-2-proposta-nuovo-livello-3.md`
  summary: Allineare la regola duplicati di `CategorySelectionWizard`.
  evidence: `CategorySelectionWizard.kt:50-54` usa `trim + lowercase` senza accent-fold, mentre la 7.2 usa `ProductNameNormalizer`; il wizard non è cablato, ma alla sua attivazione le due definizioni divergerebbero.
- source_spec: `_bmad-output/implementation-artifacts/spec-7-2-proposta-nuovo-livello-3.md`
  summary: Definire un limite di lunghezza e la gestione dei caratteri di controllo per i nomi L3.
  evidence: `CreateUserLevel3UseCase.kt:47-58` valida solo nome non vuoto e normalizzazione non vuota; il nome viene mostrato in dropdown, dialog, lista e condivisione.
- source_spec: `_bmad-output/implementation-artifacts/spec-7-2-proposta-nuovo-livello-3.md`
  summary: Aggiungere rinomina/eliminazione dei Livelli 3 user-defined.
  evidence: dopo la creazione il L3 resta nel catalogo senza UI di gestione; come per i prodotti user-defined della 7.1, serve una storia dedicata.
- source_spec: `_bmad-output/implementation-artifacts/spec-7-2-proposta-nuovo-livello-3.md`
  summary: Verificare scroll/IME del pannello L3 su schermi bassi.
  evidence: `AddItemBottomSheet.kt:137-143` non ha `verticalScroll`/`imePadding`; il pannello 7.2 aggiunge campo, errore e due pulsanti e potrebbe essere coperto dalla tastiera. La verifica manuale su device è già BLOCCATA.
- source_spec: `_bmad-output/implementation-artifacts/spec-7-2-proposta-nuovo-livello-3.md`
  summary: Estrarre il reset dei campi L3 in un helper o sealed state.
  evidence: il blocco di reset è duplicato in `handleShowNewProductForm`, `handleHideNewProductForm`, `handleNewProductLevel1Selected`, `handleNewProductLevel2Selected`, `handleNewProductLevel3Selected` e `handleCreateProductResult.Created`; le guardie 7.2 evitano gli stati incoerenti, ma il debito di manutenzione resta.
- source_spec: `_bmad-output/implementation-artifacts/spec-7-2-proposta-nuovo-livello-3.md`
  summary: Aggiungere un indice composto e un tie-breaker per `sortOrder` dei L3.
  evidence: `getLevel3`/`getLevel3ByLevel2` ordinano solo per `sortOrder` e non esiste un indice `(level2_id, sortOrder)`; con creazioni concorrenti `MAX+1` può produrre pareggi. Stessa radice del defer sul calcolo non transazionale.
- source_spec: `_bmad-output/implementation-artifacts/spec-11-4-matrice-scelta-parti-vuote.md`
  summary: Distinguere il caricamento iniziale dalla lista effettivamente vuota prima di consentire Condividi.
  evidence: `ActiveListViewModel` parte con `ActiveListUiState()` a conteggi zero e aggiorna dopo le emissioni combinate di prodotti, azioni e tassonomia. Un tap precedente può mostrare il feedback vuoto anche con dati persistiti; non esiste oggi uno stato di caricamento esplicito. La 11.4 applica la matrice ai conteggi pubblicati; il contratto generale di caricamento resta rinviato. Rilievo statico low, nessuna prova runtime eseguita.
- source_spec: `_bmad-output/implementation-artifacts/spec-13-4-semantica-talkback-checkbox-chip-sezioni-dialog.md`
  summary: Confermare con TalkBack l'annuncio iniziale dei dialog e aggiungere una verifica mirata per il full-screen dialog AddItem.
  evidence: I test Compose verificano focus e paneTitle, ma non accessibility focus/annuncio TalkBack; manca inoltre un test AddItemBottomSheet isolato e il suo harness dipende dal grafo Koin globale.

## Riconciliazione del 19 novembre 2026

Evidenze statiche sul branch `dev` al commit `baa31ca`. Le 50 voci originali sono conservate sopra. La prima colonna indica le righe delle rispettive `summary` nell'elenco originale. Una voce risolta per codice o per copertura aggiunta indica un riscontro nel repository. Gli esiti di build, test o verifiche su dispositivo seguono le evidenze di esecuzione disponibili.

| Righe originali | Voce | Stato | Evidenza al checkout |
| --- | --- | --- | --- |
| 2 | Factory e chiusura AddItem | Risolta da lavoro successivo | `AddItemBottomSheet.kt:94–119` costruisce il ViewModel con una factory e raccoglie `saved` per chiamare `onDismiss`. Il salvataggio è in `AddItemViewModel.kt:1321–1348`. |
| 5 | Conferma vocale → parsing e aggiunta | Risolta da lavoro successivo | `ActiveListScreen.kt:459–498` collega le due conferme. `ActiveListViewModel.addVoiceCommand` salva un comando univoco e valido o apre il form precompilato. La voce a L85 riguarda il cablaggio dello stesso flusso. |
| 8, 52, 88 | Lookup N+1 e filtri annidati | Ancora aperta | `ActiveListViewModel.buildUiState` (`ActiveListViewModel.kt:135–183`) riusa parte delle informazioni tra righe attive e spuntate. Restano `getById` nei cicli e filtri L2/L3 annidati. |
| 11 | Errori Room nei lookup di lista e condivisione | Ancora aperta | `ActiveListViewModel.observeAllItems` raccoglie `buildUiState` senza recupero locale. `ShareListUseCase.kt:44` e il chiamante `ActiveListScreen.kt:533–567` non gestiscono l'eccezione del lookup. |
| 14 | Errore ricerca AddItem | Ancora aperta | `AddItemViewModel.kt:110–112` popola `errors["search"]`. `AddItemBottomSheet.kt:287–298` chiama `SearchStep` senza passargli il messaggio. |
| 17 | Modifica di una voce esistente | Risolta da lavoro successivo | `ActiveListScreen.kt:740–745` passa la voce ad `AddItemBottomSheet`. `AddItemViewModel` conserva l'ID e chiama `update`; copertura sorgente in `AddItemViewModelEditTest`. |
| 20 | Test swipe-to-delete | Risolta da lavoro successivo | La 10.3 aggiunge `ActiveListPagerContractTest` (`active shopping row reveals its delete action without swiping pager`), che esegue swipe e tap fisico sul cestino e asserisce un evento di eliminazione. |
| 23 | Eliminazione prodotto con TalkBack | Ancora aperta | `ItemRow.kt:183–207` rende il cestino cliccabile quando rivelato via swipe. Manca un'azione di eliminazione accessibile dalla riga chiusa. |
| 26 | Navigazione a Storico | Risolta da lavoro successivo | `AppNavigator.kt:124` instrada a `Screen.History` e `MainActivity.kt:69` monta `HistoryScreen` con il ViewModel. |
| 29 | Annuncio e focus dopo il passaggio a Presi | Ancora aperta | `ActiveListScreen.kt:992–1019` sposta la riga tra liste tramite lo stato. `PresiSection.kt` espone intestazione e focus group, ma il toggle non imposta una destinazione di focus né annuncia lo spostamento. |
| 32 | Localizzazione etichette 6.1 | Ancora aperta | `ItemRow.kt` (`quantityLabel`, semantica della riga), `PresiSection.kt` e `VoiceFab.kt` contengono ancora etichette italiane in Kotlin. |
| 35, 55 | Formato quantità e unità nel dialog duplicati | Ancora aperta | `DuplicateResolutionDialog.kt:43–53` interpola due `Double` senza ricevere le unità. `ItemRow.quantityLabel` e `ShareListUseCase.formatItem` usano formati distinti. Confermare la destinazione del dialog con L82 prima di modificarlo. |
| 38 | Isolamento Koin in Robolectric | Ancora aperta | `TogoApplication.kt:18` conserva `GlobalContext.getOrNull()`. `AddItemBottomSheet.kt:103–108` continua a leggere il grafo globale. |
| 41, 70 | Privacy del riconoscimento delegato al sistema | Risolta da lavoro successivo | `epic-6-context.md:22,30` distingue l'uso locale da parte dell'app dal servizio OS. `audit-6-4-privacy-performance.md:54` esplicita che il recognizer scelto dal dispositivo può usare la rete. |
| 44, 64 | Copertura del rendering CategoryHeader, Presi e FAB | Da verificare | `ActiveListAccessibilityTest` copre la semantica di Presi e le etichette del FAB. `CategoryHeaderStaticAnalysisTest` controlla il sorgente del bordo. Resta da verificare il bordo renderizzato e l'aspetto dei tre elementi. |
| 47 | Test diniego del microfono e feedback | Ancora aperta | `ActiveListAccessibilityTest.kt:580–608` verifica che l'ascolto resti inattivo. Manca un'asserzione sul messaggio o sull'azione snackbar prodotti da `ActiveListScreen.kt:360–389`. |
| 58 | Hint categoria e qualificatore nel parser vocale | Risolta da lavoro successivo | `VoiceCommandParser.kt:215–235` riconosce l'hint a confini di parola o come token finale e preserva il qualificatore come variante. `VoiceCommandParserTest.kt:230–275` copre `pane integrale`, la punteggiatura e `bio`. |
| 61 | Test su TalkBack reale | Ancora aperta | La 13.4 aggiunge controlli semantici Compose/JVM. Il checkout non contiene sorgenti `app/src/androidTest`. La verifica degli annunci su dispositivo è collegata a L163. |
| 67 | Stati e checklist delle riparazioni | Da verificare | Sette `spec-riparazione-*` sono ancora `ready` con checklist aperte, ad esempio `spec-riparazione-storico-navigazione.md` da V1 a V6. Verificare le evidenze storiche di ciascuna anche dove il codice è già cambiato. |
| 73 | Traccia del token `border-crisp-dark` | Risolta da lavoro successivo | `spec-6-3-dark-mode-completa.md:27,112` registra `#64748B` come criterio di accettazione e scenario di verifica. `DESIGN.md` riporta lo stesso valore. |
| 76 | Separazione del diff storico 6.1 | Non più applicabile | Rilievo riferito al diff della review del 20 ottobre 2026. La consegna corrente riconcilia i registri sul checkout dopo la 13.5. La segnalazione storica resta sopra. |
| 82 | Deduplica esatta prodotti nella lista / dialog orfano | Ancora aperta | `AddItemViewModel.kt:1306–1327` crea un nuovo UUID e chiama `RoomShoppingListRepository.insert`, che fa un insert per ID. `DuplicateResolutionDialog` non ha chiamanti di produzione. Gli avvisi 7.1/7.2 e le dedupliche 8.9/12.3 operano su catalogo o azioni. Resta aperta la misura SM-2 lato lista. |
| 85 | Cablaggio `VoiceConfirmationSheet` | Risolta da lavoro successivo | `ActiveListScreen.kt:459–498` monta `VoiceConfirmationSheet`, collega `VoiceConfirmationViewModel` e inoltra il comando ad `addVoiceCommand`. |
| 91 | Limite a `LEARNED_RULE.userExpression` | Ancora aperta | `LearnedRuleManager.create` normalizza e persiste l'espressione. `LearnedRuleEntity.kt:18` la memorizza senza un limite di lunghezza. |
| 94 | Identità richiesta/risultato del recognizer | Ancora aperta | `AndroidVoiceRecognitionGateway.kt:93–154` usa un launcher condiviso e il flag `acceptResult`. `onActivityResult` non riceve un ID di richiesta per distinguere un esito tardivo da quello di un nuovo ascolto. |
| 97 | Mediane manuale/voce su dispositivo | Da verificare | Protocollo e soglie in `audit-6-4-privacy-performance.md` §4. Mancano le misure di 10 run manuali e 10 vocali. Resta documentata la deroga della 6.4. |
| 100 | Corpus di frasi libere per SM-1 | Non più applicabile | Il protocollo vigente usa il corpus derivato da prodotti e sinonimi (`CatalogAcceptanceSupport.acceptanceCases`, audit 6.4). La proposta è condizionata a una decisione di estendere il corpus e il significato della misura. |
| 106, 109, 142 | Wizard 5-step, ID del draft e confronto duplicati | Non più applicabile | `ProductCategorizationWizard` non ha chiamanti di produzione. `NewProductDraft.toCanonicalProduct` conserva `id = finalLevel3Id` e il wizard storico conserva il confronto `trim + lowercase`. Il percorso attivo 7.1/7.2 usa `NewProductForm`, `CreateCanonicalProductUseCase` e `CreateUserLevel3UseCase`. Rivalutare i rilievi sul wizard se verrà adottato. |
| 112 | Gestione dei prodotti creati dall'utente | Risolta da lavoro successivo | `MainActivity.kt:53` collega `SettingsScreen`, che a L91 apre `UserProductsScreen`. La schermata consente modifica ed eliminazione. `UserDataViewModel.kt:116,135` chiama le corrispondenti operazioni di `RoomCatalogRepository.kt:111,134`. |
| 115 | Indietro con il form nuovo prodotto aperto | Ancora aperta | `AddItemBottomSheet.kt:145–153,264–284,360` espone Annulla per chiudere il form e la X per chiudere il dialogo. `PreviousStep` con form aperto resta senza un percorso di attivazione e il Back di sistema è disabilitato. |
| 118 | Retry per errore caricamento tassonomia | Ancora aperta | `AddItemViewModel.kt:574–602` prova il caricamento all'apertura e imposta `newProductError` al fallimento. `NewProductForm` mostra l'errore senza un comando di riprova nello stesso form. |
| 121 | DDL `AUTOINCREMENT` per il catalogo generato | Ancora aperta | `tools/generate_catalog.py:77–81` usa `PRIMARY KEY(id)` per L3 e prodotto. `CatalogEntities.kt:58,85` usa `@PrimaryKey(autoGenerate = true)`. Rilievo sul DDL del prossimo asset, senza evidenza di regressione runtime. |
| 127, 157 | Ordinamento L3, atomicità e pareggi | Ancora aperta | `RoomCatalogRepository.kt:148–160` legge `MAX+1` e inserisce con due statement. `CatalogDao.kt:99–106` ordina per il solo `sortOrder` e `CatalogEntities.kt:50–54` usa indici separati. Il percorso import 12.5 ha una transazione esterna. Resta la deroga 7.2 per il percorso singolo da form. |
| 133 | L3 utente in assegnazione e correzione | Ancora aperta | `CategoryAssigner.kt:38,62,76,97` esclude i L3 utente secondo la 3.6. `CategoryCorrector.kt:58–65` li rifiuta anche nel flusso invocato da `AddItemViewModel.handleApplyCategoryCorrection`. L'estensione della regola è una decisione di prodotto. |
| 136 | Test del dialog L3 dentro AddItem | Ancora aperta | `AddItemBottomSheet.kt:448–455` monta il dialog. `DuplicateLevel3WarningDialogTest` e `AddItemViewModelLevel3ProposalTest` coprono separatamente dialog e stato. Manca la composizione del flusso in un test della sheet. |
| 139 | Consumer della tassonomia con L3 utente | Ancora aperta | DAO e repository hanno casi dedicati. `CategoryAssignerTest` e `CategoryCorrectorTest` coprono l'esclusione; resta da coprire il L3 utente come dato effettivo nella lista e nel testo di condivisione (`ActiveListViewModel`, `ShareListUseCaseTest`). |
| 145 | Validazione dei nomi L3 | Ancora aperta | `CreateUserLevel3UseCase.kt:47–67` verifica nome, normalizzazione e duplicati, senza un limite di lunghezza o un controllo esplicito dei caratteri di controllo. |
| 148 | Rinomina/eliminazione L3 utente | Ancora aperta | `CreateUserLevel3UseCase` e `CatalogRepository.insertLevel3` coprono la creazione. `UserProductsScreen` gestisce i prodotti (L112). Per i L3 manca un percorso di gestione e va decisa la sorte dei prodotti e dati collegati. |
| 151 | Scroll e IME del form L3 a schermo basso | Da verificare | `AddItemBottomSheet.kt:253–284` dà al form una `Column` con altezza vincolata. `NewProductForm` aggiunge il pannello L3 senza scroll/IME espliciti in quel ramo. Verificare visibilità e raggiungibilità dei comandi su dispositivo con tastiera aperta. |
| 154 | Reset duplicati dei campi L3 | Ancora aperta | `AddItemViewModel.kt:574–674,753–768` ripete i reset tra apertura/chiusura form, selezioni tassonomiche e creazione prodotto. |
| 160 | Condividi durante il caricamento iniziale | Ancora aperta | `ActiveListViewModel.kt:65–67,101–108` parte con conteggi a zero e attende tre flussi. `ActiveListScreen.kt:512–525` valuta subito i conteggi, con possibile messaggio di lista vuota durante il caricamento. |
| 163 | Annuncio TalkBack dialog e prova AddItem full-screen | Da verificare | La spec 13.4 e i test Compose documentano focus e `paneTitle`. Manca la conferma dell'annuncio/accessibility focus su dispositivo e un test dedicato al full-screen `AddItemBottomSheet`; il harness dipende da Koin. Collegata a L61 e L136. |
