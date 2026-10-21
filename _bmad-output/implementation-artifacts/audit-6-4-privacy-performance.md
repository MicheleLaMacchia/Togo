# Audit 6.4 — Privacy Audio e Performance

**Data:** 21/10/2026  
**Baseline:** `cccd9c8bb37d9d75a8438c88480266f1f0dd1861`  
**Stato:** remediation review applicata staticamente; build e test **non** eseguiti dall'agente.

> **Nota di lettura:** in questo audit “calcolato staticamente” significa ottenuto
> riproducendo la logica sul catalogo, **non** da un'esecuzione JUnit. Nessun test
> è stato eseguito: i valori vanno confermati con `.\test-android.ps1`.

## 0. Remediation review (statistica)

Correzioni applicate dopo la review a 5 lenti:

- `ResolutionDedupAuditTest.computeAudit()` è ora `suspend` e viene invocato da
  `runBlocking`: il test source set non ha più l'errore di compilazione.
- Guardie esplicite su corpus vuoto (`total >= 200`, `namesTotal > 0`) e su scan
  privacy vuoti (path mancante o zero sorgenti → fallimento).
- Il gateway invalida i risultati tardivi dopo `reset()`/`stopListening()`
  tramite `acceptResult`; aggiunti test dedicati.
- Il callback di conferma è registrato in `DisposableEffect` e rilasciato in
  `onDispose`, non riassegnato nel corpo della composable.
- `VoiceResilienceViewModel` annulla il retry programmato su `cancel()` e
  `onCleared()`: non può riaprire il microfono dopo l'annullamento.
- `CategoryAccuracyAcceptanceTest` invoca `CategoryAssigner` su **ogni**
  `Single`, non solo sui prodotti già corretti dal resolver.
- Scan privacy esteso a gateway, presentation, domain voice/parser, use case e
  `ActiveListScreen`; permessi manifest confrontati all'insieme atteso, che
  allowlista esplicitamente il `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
  app-scoped aggiunto da AndroidX core.
- Il test SM-2 bloccato non verifica più che il difetto resti nel codice:
  controlla solo la tracciabilità documentale del blocco.
- Remediation dei tre fallimenti segnalati in `.\test-android.ps1`: i due test
  sotto soglia sono risolti alla radice con il match esatto di sinonimo in
  `ProductResolver` (`getByExactSynonym`), il test permessi con l'allowlist del
  permesso app-scoped. Soglie e catalogo invariati.

## 1. Matrice privacy

| Dato | Dove vive | Durata | Persistenza | Evidenza |
|---|---|---|---|---|
| Audio grezzo / buffer PCM | Non esiste nel processo dell'app: la cattura è delegata a `RecognizerIntent` | Solo durante la finestra del servizio OS | Nessuna | `app/src/main/java/it/togo/app/data/voice/AndroidVoiceRecognitionGateway.kt:106` (`ACTION_RECOGNIZE_SPEECH`); nessuna API `MediaRecorder`/`AudioRecord` nel modulo voce |
| Testo riconosciuto (stato gateway) | `AndroidVoiceRecognitionGateway._state: MutableStateFlow<VoiceRecognitionState>` | Fino a `reset()` / consumo dell'operazione; un risultato tardivo dopo reset/stop è ignorato da `acceptResult` | Solo memoria | `AndroidVoiceRecognitionGateway.kt:38-49`, `139-152`; `VoiceRecognitionState.Result` in `VoiceRecognitionState.kt` |
| Testo riconosciuto (stato UI) | `VoiceBottomSheetViewModel._uiState: VoiceBottomSheetUiState.Ready(recognizedText, editedText)` | Fino a conferma, annullamento o distruzione del ViewModel | Solo memoria | `app/.../presentation/voice/VoiceBottomSheetViewModel.kt:29-30`, `69-76`, `89-103` |
| Testo riconosciuto in configurazione | Nessun `rememberSaveable` sul testo vocale | n/a | Nessuna | `VoicePrivacyAuditTest` — `recognized text is not saved across configuration changes`; `VoiceBottomSheet.kt` usa solo stato del ViewModel |
| Comando interpretato legacy | `VoiceConfirmationViewModel._uiState: VoiceConfirmationUiState.Ready` | Fino a `cancel`/`reset`/`onCleared` | Solo memoria | `VoiceConfirmationViewModel.kt:29-30`, `91-95`; componente non raggiungibile dalla UI |
| Callback di conferma | `onTextConfirmed` / `onCommandConfirmed` nel ViewModel | Fino a distruzione del ViewModel; registrato in `DisposableEffect` e rilasciato in `onDispose` | Solo memoria | `VoiceBottomSheetViewModel.kt:99-103`; `ActiveListScreen.kt:151-159`; `VoiceConfirmationViewModel.kt:91-94` |
| Espressione appresa | SQLite `LEARNED_RULE.userExpression` | Persistente per progetto (Epic 5) | **Eccezione documentata** | `app/src/main/java/it/togo/app/data/database/entity/LearnedRuleEntity.kt:8`, `18` |
| Log del testo | Non esiste alcun `Log.`/`println(` nel modulo voce | n/a | Nessuna | `VoicePrivacyAuditTest` — `voice code contains no audio file or log APIs` |
| Invio di rete dell'app | Nessun permesso `INTERNET` / `ACCESS_NETWORK_STATE`; nessun import di rete nel modulo voce | n/a | Nessuna | `AndroidManifest.xml:4-5`; `VoicePrivacyAuditTest` — `manifest has no network permissions` |

**Eccezione `LEARNED_RULE.userExpression`:** non è un log dell'utterance completa. Contiene l'espressione normalizzata che l'utente ha confermato per ricordare la categoria canonica di un prodotto (feature Epic 5). Il modulo voce non legge né scrive direttamente questa tabella: passa dal repository delle regole apprese.

**Nota sul riconoscimento OS:** `RecognizerIntent` può delegare a un servizio che usa la rete. L'app non dichiara `INTERNET` e non invia testo a endpoint remoti; l'eventuale trattamento remoto è del servizio di riconoscimento selezionato dal dispositivo, fuori dal processo TOGO.

**Risultato tardivo:** `reset()` e `stopListening()` chiudono `acceptResult`;
`onActivityResult` scarta il callback che arriva dopo. Questo copre cancel,
dismiss e `onCleared`. Un risultato della richiesta precedente che arrivasse
dopo un **nuovo** `startListening()` non è correlabile con il launcher attuale
(manca un request code per richiesta): richiederebbe un launcher dedicato o un
request id end-to-end. È tracciato come rischio residuo.

## 2. Audit performance statico

### N+1 nella lista attiva

`ActiveListViewModel.buildUiState` esegue un lookup per prodotto:

- voci attive: `catalogRepository.getById(item.productId)` per ogni voce (`ActiveListViewModel.kt:95-96`);
- voci spuntate non già risolte: altro `getById` per prodotto (`ActiveListViewModel.kt:107-109`).

Con `N` prodotti distinti nella lista, ogni emissione di `shoppingRepository.getAllItems()` o della tassonomia costa **N query** `getById` (più le query della tassonomia). Per 20 voci sono 20 lookup per emissione; per 50 voci sono 50. Il pattern è indipendente dal numero di righe attive/spuntate: le voci duplicate dello stesso prodotto vengono riusate solo nella seconda passata.

Inoltre la costruzione dei gruppi ha filtri annidati sulla tassonomia: per ogni
L1 filtra tutti gli L2 (`ActiveListViewModel.kt:117-119`) e per ogni L2 filtra
tutti gli L3 (`ActiveListViewModel.kt:120-123`). Con `L1`, `L2`, `L3` livelli
il costo è `O(L1*L2 + L2*L3)` per emissione, indipendente dalle voci ma non
costante. Non è stato rifattorizzato in 6.4.

**Impatto:** latenza proporzionale alla lista (lookup prodotto) e alla
tassonomia (filtri annidati) a ogni aggiornamento (check, undo, modifica). Non
è un crash, ma è un rischio di jank su liste lunghe.

**Fix richiesto (fuori perimetro 6.4):** API batch `CatalogRepository.getByIds(ids)` o cache condivisa con invalidazione; per la tassonomia, mappe `level1Id -> L2` e `level2Id -> L3` costruite una volta. Non è stato introdotto alcun refactor in questa storia. Voce aggiunta in `deferred-work.md` anche per i loop annidati.

### Altri punti osservati

- `ShareListUseCase` risolve i prodotti per la condivisione; non è stato misurato in questa sede.
- Il resolver è O(candidati) in memoria dopo la query SQL `LIKE`; il limite `LIMIT 50` contiene il costo per utterance.

## 3. Metriche di accettazione

Corpus derivato dal catalogo reale (`app/src/main/assets/catalog.db`), senza righe inventate:

- 1.470 nomi canonici da `CANONICAL_PRODUCT`;
- 1.781 sinonimi da `SYNONYM`;
- **3.251 casi totali** (minimo richiesto: 200).

| Metrica | Esito | Valore / motivo | Evidenza o comando |
|---|---|---|---|
| Accuratezza categoria (SM-1) | **Verificata staticamente — test atteso verde** | **100,00%** (3.251/3.251); ambigui 0, `None` 0. Soglia 95% superata; catalogo e soglia invariati | `CategoryAccuracyAcceptanceTest`; calcolo riprodotto sul DB con la logica reale di `ProductResolver` + `CategoryAssigner` (assigner invocato su ogni `Single`) |
| Precision resolver (proxy SM-2) | **Verificata staticamente — test atteso verde** | **100,00%** (TP 3.251, FP 0) | `ResolutionDedupAuditTest` — `resolver precision is at least 98 percent` |
| Recall resolver (proxy SM-2) | **Verificata staticamente — test atteso verde** | **100,00%** (TP 3.251, FN 0); ambigui 0, `None` 0. Soglia 95% superata | `ResolutionDedupAuditTest` — `resolver recall is at least 95 percent` |
| Nomi canonici distinti | **Verificata staticamente — test atteso verde** | **100,00%** (1.470/1.470 risolti al prodotto corretto) | `ResolutionDedupAuditTest` — `distinct canonical product names resolve to themselves` |
| Deduplicazione esatta lato lista (SM-2) | **BLOCCATA** | `DetectDuplicateUseCase` e `ResolveDuplicateUseCase` sono commentate in `DomainModule.kt:42-43`; `DuplicateResolutionDialog` non ha call site; non esiste un use case duplicati nel dominio. Non viene simulata una metrica inesistente | `ResolutionDedupAuditTest` — `SM-2 exact duplicate block is documented and traceable` |
| Modifiche non confermate (SM-C2) | **Calcolata staticamente — non verificata da test** | **0 scritture** dopo `UiEvent.VoiceCommand("latte 1 l")`. Attenzione: l'evento è ignorato, quindi il test verifica il comportamento attuale, non un percorso end-to-end di conferma | `ZeroUnconfirmedChangesTest` — `ignored voice command produces no repository writes and no list change` |
| Tempi manuali (mediana ≤15 s) | **BLOCCATA** | Richiede misurazione manuale su device; nessuna strumentazione aggiunta in produzione. Protocollo al §4 | n/a |
| Tempi voce (mediana ≤10 s) | **BLOCCATA** | Richiede misurazione manuale su device; la lettura della conferma è esclusa. Protocollo al §4 | n/a |

**Perché prima era 94,49% e ora 100%:** i 179 falsi negativi erano sinonimi generici (`pane di`, `latte di`, `tonno in`) che la sola ricerca LIKE rendeva `Ambiguous` perché prefissi di più nomi. La remediation ha aggiunto in `ProductResolver` il match esatto su `SYNONYM` (`CatalogRepository.getByExactSynonym` → `SELECT DISTINCT cp.* ... JOIN SYNONYM WHERE s.term = :term`), valutato prima della LIKE e con confidence 1.0. Non sono stati modificati catalogo o soglie: è stata corretta la causa nel dominio. Un termine mappato a più prodotti distinti resta non risolvibile (`singleOrNull`).

**Corpus:** solo nomi canonici e sinonimi reali. L'AC parla anche di “casi
manuali”/utterance libere: non sono state aggiunte righe inventate. La metrica
SM-1 misura quindi la risoluzione del vocabolario del catalogo, non il parsing
di frasi complete. Questa è una deviazione tracciata, non un risultato
presentato come completo.

## 4. Protocollo tempi manuale

Nessuna strumentazione o timing è stata aggiunta all'app. Le misure vanno raccolte manualmente su un device reale, con catalogo di produzione e senza demo mode.

### Aggiunta manuale — target mediana ≤15 s

- **Inizio:** tap sul FAB `+` (apertura del foglio di aggiunta).
- **Fine:** tap su “Aggiungi”/“Salva” con voce visibile nella lista.
- **Run:** 10.
- **Mediana:** tempo centrale dei 10 run (media dei due valori centrali se pari).

| run | inizio | fine | secondi | note |
|---:|---|---|---:|---|
| 1 |  |  |  |  |
| 2 |  |  |  |  |
| 3 |  |  |  |  |
| 4 |  |  |  |  |
| 5 |  |  |  |  |
| 6 |  |  |  |  |
| 7 |  |  |  |  |
| 8 |  |  |  |  |
| 9 |  |  |  |  |
| 10 |  |  |  |  |

### Aggiunta vocale — target mediana ≤10 s, lettura conferma esclusa

- **Inizio:** tap sul FAB microfono.
- **Fine:** comparsa del testo riconosciuto nello stato `Ready` del `VoiceBottomSheet` (prima della lettura e del tap “Conferma”).
- **Run:** 10.
- **Nota:** trascrivere a parte il tempo di lettura della conferma; non entra nella mediana.

| run | inizio | fine | secondi | note |
|---:|---|---|---:|---|
| 1 |  |  |  |  |
| 2 |  |  |  |  |
| 3 |  |  |  |  |
| 4 |  |  |  |  |
| 5 |  |  |  |  |
| 6 |  |  |  |  |
| 7 |  |  |  |  |
| 8 |  |  |  |  |
| 9 |  |  |  |  |
| 10 |  |  |  |  |

## 5. Rischi residui

1. **Riconoscimento OS potenzialmente remoto.** L'app non ha `INTERNET`, ma il servizio di riconoscimento del dispositivo può usare la rete. Va comunicato senza promettere un'offline che non dipende da TOGO.
2. **Duplicati lato lista non cablati.** La metrica SM-2 sulla deduplicazione esatta resta `BLOCCATA` finché non vengono implementati e collegati `DetectDuplicateUseCase`/`ResolveDuplicateUseCase`.
3. **Resolver ora dipendente dal match esatto di sinonimo.** `getByExactSynonym` aggiunge una query per utterance; il caso è coperto da indice su `SYNONYM.term`. Il comportamento è cambiato per i sinonimi generici: non sono più `Ambiguous`. Da rivalutare se il catalogo dovesse contenere termini duplicati su prodotti diversi (in quel caso il resolver resta prudente e non risolve).
4. **N+1 nella lista attiva.** Un lookup per prodotto a ogni emissione; il fix richiede un'API batch o una cache.
5. **Flusso di conferma legacy non raggiungibile.** `VoiceConfirmationViewModel` e `VoiceConfirmationSheet` sono registrati in Koin ma senza call site UI. La privacy è comunque blindata con `onCleared`, ma il codice morto va cablato o rimosso in una storia dedicata.
6. **Testo vocale scartato.** `UiEvent.VoiceCommand` è ignorato da `ActiveListViewModel` (`ActiveListViewModel.kt:173`). Non è una regressione privacy: è il comportamento noto tracciato in `deferred-work.md`; il test SM-C2 verifica lo zero attuale, non un percorso di conferma end-to-end.
7. **Correlazione del risultato vocale.** `acceptResult` copre reset/stop, ma un risultato della richiesta precedente che arrivasse dopo un nuovo `startListening()` non è distinguibile senza request id/launcher dedicato. Rischio residuo raro, tracciato in `deferred-work.md`.
8. **`LEARNED_RULE.userExpression` senza limite di lunghezza.** È l'eccezione di persistenza ammessa; non è stato aggiunto un bound in 6.4 per non cambiare il dominio. Va valutato in una storia privacy/dati.
9. **Test e build.** Eseguiti dall'utente con esito positivo (486/486 verdi). Le correzioni statiche dei tre fallimenti sono quindi confermate sul campo. La storia è chiusa; resta come lavoro futuro solo la misura dei tempi manuali (deroga accettata).

## 6. Verifica

Comandi da eseguire manualmente:

```powershell
.\test-android.ps1
.\build-android.ps1
```

Esito (riportato dall'utente, 21/10/2026): **positivo**. Da confermare nessuno degli item: `CategoryAccuracyAcceptanceTest` (100%), `ResolutionDedupAuditTest` (precision, recall, nomi distinti, blocco documentato), `ProductResolverTest` (`exact synonym beats ambiguous prefix candidates`) risultano verdi.

- Verdi: `VoicePrivacyAuditTest` (scan esteso, permessi allowlistati,
  guardie anti-vuoto), `VoiceTextLifecycleTest`,
  `AndroidVoiceRecognitionGatewayTest` (inclusi i due test sul risultato
  tardivo), `VoiceResilienceViewModelTest` (incluso `cancel annulla il retry
  programmato`), `ZeroUnconfirmedChangesTest`, `CategoryAccuracyAcceptanceTest`
  (100%), `ResolutionDedupAuditTest` (precision, recall, nomi distinti, blocco
  documentato), `ProductResolverTest` (incluso `exact synonym beats ambiguous
  prefix candidates`).

I tre fallimenti segnalati (`CategoryAccuracyAcceptanceTest`,
`ResolutionDedupAuditTest` recall, `VoicePrivacyAuditTest` permessi) sono stati
corretti staticamente: due alla radice nel resolver, il terzo allowlistando il
permesso app-scoped di AndroidX. Le soglie non sono state ammorbidite.

**Chiusura:** con esito positivo e deroghe `BLOCCATA` accettate dall'utente in
data 21/10/2026, la Story 6.4 e l'Epic 6 passano a `done`.
