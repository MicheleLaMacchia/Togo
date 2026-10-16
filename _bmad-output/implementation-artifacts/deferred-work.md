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
