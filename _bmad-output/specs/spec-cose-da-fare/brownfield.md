# Stato attuale rilevante

Fatti del codice che vincolano la realizzazione della spec. Verificati sul repository il 2026-11-04.

## Modello dati e persistenza

- `ShoppingItem` richiede `productId`, `quantity` e `unit`. Un'azione in testo libero non può riusare l'entità così com'è. Urgenza e ambito non sono marca, variante o conservazione.
- `TogoDatabase` è alla versione 5, con `exportSchema = false`, `createFromAsset("catalog.db")` e `fallbackToDestructiveMigration()`. Le entità attuali coprono spesa, storico, regole apprese, tassonomia, prodotti canonici, sinonimi e attributi utente. Una nuova entità per le azioni richiede tabella, DAO, migrazione e registrazione nel database.
- L'asset `app/src/main/assets/catalog.db` ha `user_version = 1` e non ha `room_master_table`. Il database arriva alla versione 5 applicando le migrazioni 1→2, 2→3, 3→4 e 4→5. Se si porta il database alla versione 6 senza una `MIGRATION_5_6`, il fallback distruttivo ricrea un database vuoto e perde il catalogo precompilato. "Avviare da zero" elimina il vincolo di preservare i dati utente, non il vincolo di far funzionare il seed del catalogo. La via più semplice è una migrazione 5→6 che crea la tabella delle azioni; in alternativa va rigenerato l'asset a versione 6 con l'identity hash Room.
- `ShoppingListRepository.checkout` sposta atomicamente le voci spuntate nello storico e incrementa `purchaseCount`. Concludere le attività deve eliminare le azioni spuntate senza scrivere `HISTORICAL_ITEM` e senza incrementare conteggi.
- La lista attiva raggruppa per L1/L2/L3 della tassonomia. `[DA FARE]` non è un reparto: non deve creare nodi o prodotti.
- `ProductNameNormalizer.normalize` fa trim, minuscole, NFKD e rimuove gli accenti. La deduplica delle azioni richiede che gli accenti restino significativi: non va riusato così com'è; serve una normalizzazione separata (trim, minuscole, spazi collassati, accenti conservati).

## Schermata, gesture e navigazione

- `ActiveListScreen` è uno `Scaffold` con `TopAppBar`: titolo "Spesa (N)", storico a sinistra, condivisione e impostazioni a destra. Il FAB è una colonna con microfono e (+). La lista è una `LazyColumn` di gruppi L1; in fondo c'è `PresiSection` con "Concludi spesa".
- `ItemRow` usa `detectHorizontalDragGestures` e consuma il drag orizzontale per rivelare il cestino. La riga attiva è anche `combinedClickable` per la modifica. Un pager che intercetta lo stesso asse sulle righe rompe il cestino: il pager deve stare su intestazione, intestazioni di gruppo e spazi non occupati da righe.
- La top bar attuale non ha frecce e non ha un titolo di pagina separato. La soluzione richiede un'intestazione fissa con titolo "Spesa"/"Da fare" e frecce; la disposizione esatta è UX. Quella zona è anche la principale zona di swipe del pager.
- `PresiSection` è il modello per una `FatteSection`: header collassabile "Presi (N)", `rememberSaveable` per l'espansione, pulsante "Concludi spesa" in fondo, righe senza tap-edit e senza cestino, checkbox che deseleziona. La sezione Fatte può riusare lo stesso pattern con "Fatte (N)" e "Concludi attività".
- Il FAB (+) apre `AddItemBottomSheet`; il microfono apre il flusso vocale. Sulla pagina Da fare il (+) deve aprire un foglio azione e il microfono va nascosto.
- `AddItemBottomSheet` è specifico per prodotto e catalogo; non è riusabile per l'azione. Serve un foglio separato con testo, urgenza e ambito.
- `AppNavigator` non usa NavHost: la navigazione è uno stato locale con `rememberSaveableStateHolder`. La pagina Da fare non è una schermata separata del navigator, è una pagina del pager. Il back dalla pagina Da fare deve tornare alla Spesa, non uscire dall'app: il `BackHandler` va coordinato con il pager.
- `HorizontalPager` è disponibile con la Compose BOM 2024.09.03 (foundation 1.7), senza nuove dipendenze. Non è usato altrove.
- `ActiveListViewModel` espone `ActiveListUiState` con `activeGroups`, `checkedItems`, `activeCount` e i campi transitori di snackbar/undo. Servono stato, eventi e conteggi separati per le azioni; il vocale non va esteso.
- `UiEvent` è un'interfaccia sigillata usata da `AppNavigator` e dalla schermata; nuove azioni richiedono nuovi eventi o un secondo ViewModel.

## Condivisione

- `ShareListUseCase` restituisce `null` se non ci sono voci attive (non spuntate) e costruisce solo il testo della spesa. Per "Solo azioni" deve funzionare anche con spesa vuota: il `null` non può restare il segnale "niente da condividere".
- Il formato spesa è `[L1]`, `+ L2`, `- L3`, riga indentata `prodotto marca variante quantità unità`. "Solo spesa" deve restare identico.
- Il testo azioni è `[DA FARE]`, `+ AMBITO`, `- URGENZA`, riga indentata di due spazi. Aree nell'ordine fisso, urgenze nell'ordine, azioni alfabetiche; le spuntate sono escluse.
- L'oggetto attuale è fisso "Lista spesa TOGO"; va scelto in base alla selezione.
- `ActiveListScreen` gestisce lo share con un chooser e mostra "Nessun articolo da condividere" se l'intent è nullo. Il dialogo a tre scelte deve sostituire questo accesso diretto.
- L'icona di condivisione è solo sulla pagina Spesa; va aggiunta anche a Da fare.

## Import

- `ShoppingListImportParser` riconosce `[sezione]`, `+ L2`, `- L3`, righe indentate. Un `[DA FARE]` oggi verrebbe letto come sezione di primo livello e poi fallirebbe contro la tassonomia. Va riconosciuto prima del controllo L1.
- Il parser rifiuta un testo senza prodotti (`itemCount == 0`) e richiede almeno una sezione con L2/L3/prodotti. Un import di sole azioni oggi è invalido: va reso valido.
- `ParsedItem` contiene `lineNumber`, `description`, `quantity` e `unit`; l'azione non deve passare dal parsing quantità. Il testo va preservato verbatim (trim, una riga, niente capitalizzazione).
- `ImportShoppingListUseCase.preview` risolve le categorie contro il catalogo e rifiuta L1/L2 sconosciuti; `execute` crea L3/prodotti mancanti e aggiorna le voci esistenti. Le azioni devono avere un piano separato e non passare da `resolveItem` o dal catalogo.
- `ImportPlan` conta solo prodotti; `ImportExecutionResult.Success` conta `imported`, `updatedExisting`, `createdProducts` e `createdCategories`; `ImportListViewModel.buildSuccessMessage` parla solo di prodotti. Vanno estesi a prodotti, azioni, duplicati saltati e categorie/prodotti creati.
- `ImportListScreen.FORMAT_EXAMPLE` mostra solo il formato spesa; la descrizione dice "prodotti, categorie, quantità e unità". Vanno aggiornati con `[DA FARE]`.
- `ImportListViewModel` non ha un modo per mostrare "0 prodotti, N azioni"; la conferma va resa condizionale.
- L'import attuale non è transazionale: `execute` scrive in loop e, se fallisce a metà, può lasciare scritture parziali. La spec richiede atomicità del blocco `[DA FARE]` e dell'intero import. Serve una transazione Room o un piano applicato atomicamente: validare prima non basta.
- La deduplica delle azioni va fatta in `preview` (per il conteggio) e riconfermata in `execute` (per evitare race). La chiave è testo + ambito + urgenza, normalizzata senza accenti; le azioni spuntate contano come presenti.
- `RoomShoppingListRepository` ha già il pattern di undo per la singola voce (`_deletedItemForUndo` + snackbar "Annulla"); la decisione per l'azione è ancora aperta.

## Altro

- Il vocale usa `VoiceCommandParser` e `ResolveVoiceProductUseCase`; non va esteso alle azioni in questa versione.
- La ricerca catalogo usa `ProductResolver`; le azioni non devono comparire.
- I test esistenti sono JVM/Robolectric; per l'import ci sono test su parser e use case. Vanno aggiunti test per il blocco `[DA FARE]`, la deduplica, l'ordine, il round-trip e il seed del catalogo.
