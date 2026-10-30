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
- source_spec: `_bmad-output/implementation-artifacts/spec-8-7-adattamento-ui-shopping-esistente-e-regressione.md`
  summary: Aggiungere una regressione dedicata alla migrazione Room 6→7.
  evidence: `MIGRATION_6_7` elimina le tabelle legacy ma i test individuati coprono soltanto 5→6; il riepilogo Story 8.7 cita un test 6→7 che non risulta presente.
- source_spec: `_bmad-output/implementation-artifacts/spec-8-7-adattamento-ui-shopping-esistente-e-regressione.md`
  summary: Gestire eccezioni repository nel completamento e nell'annullamento eliminazione della lista attiva.
  evidence: `ActiveListViewModel` rimuove ottimisticamente la riga prima di `complete` e non intercetta le eccezioni di `complete` o `restore` nell'undo.
- source_spec: `_bmad-output/implementation-artifacts/spec-8-7-adattamento-ui-shopping-esistente-e-regressione.md`
  summary: Rendere range-safe la formattazione delle quantità condivise.
  evidence: `formatShareQuantity` converte valori interi `Double` in `Long`, alterando valori validi oltre il range di `Long`.
- source_spec: `_bmad-output/implementation-artifacts/spec-8-7-adattamento-ui-shopping-esistente-e-regressione.md`
  summary: Aggiungere un test del completamento riuscito della voce nella lista attiva.
  evidence: Il fake in `ActiveListAccessibilityTest` restituisce sempre `null` da `complete`; la ricerca non ha trovato test del ViewModel per la transizione riuscita.
- source_spec: `_bmad-output/implementation-artifacts/spec-8-7-adattamento-ui-shopping-esistente-e-regressione.md`
  summary: Riallineare il riepilogo Story 8.7 alle verifiche realmente concluse.
  evidence: `story87_summary.txt` indica ancora la correzione della compilazione di `ActiveListAccessibilityTest` come azione pendente e menziona un test 6→7 non presente.
- source_spec: `_bmad-output/implementation-artifacts/spec-8-7-adattamento-ui-shopping-esistente-e-regressione.md`
  summary: Attribuire le modifiche DB/migrazione già presenti a uno scope BMAD coerente.
  evidence: Il worktree aveva modifiche DB/migrazione prima del fix UI; sono state preservate e non fanno parte di questo intervento di compilazione.