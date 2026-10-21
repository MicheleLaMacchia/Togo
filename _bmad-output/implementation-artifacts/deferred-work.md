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
