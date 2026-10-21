---
title: '6.4 Privacy Audio e Audit Performance'
type: 'feature'
created: '2026-10-21'
status: 'done'
route: 'dispatch'
baseline_commit: 'cccd9c8bb37d9d75a8438c88480266f1f0dd1861'
context:
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-6-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-6-3-dark-mode-completa.md'
---

## Story

Come utente attento alla privacy,  
voglio essere sicuro che le mie registrazioni vocali non vengano conservate dall'app,  
così che le mie abitudini di spesa rimangano private.

**Acceptance Criteria:**

**Given** l'utente usa l'inserimento vocale  
**When** il riconoscimento è completato (successo o fallimento)  
**Then** nessun file audio o buffer PCM è persistito sul filesystem locale dell'app  
**And** il testo riconosciuto viene usato solo per l'elaborazione dell'inserimento corrente e non viene salvato separatamente come log  
**And** il testo riconosciuto non viene inviato a endpoint remoti dall'app (il riconoscimento è delegato al servizio OS)

**Given** il set di 200 casi di test (utterance italiane + casi manuali) compilato dal catalogo  
**When** viene eseguita la suite di test di accettazione  
**Then** la percentuale di assegnazioni categoria corrette è ≥95% (SM-1)  
**And** la precisione deduplicazione duplicati esatti è ≥98% e il recall ≥95% (SM-2)  
**And** il numero di modifiche non confermate nei test è 0 (SM-C2)

## Intent

**Problema:** il flusso vocale usa `RecognizerIntent` e non registra audio, ma il testo riconosciuto vive in un ViewModel senza `onCleared`: alla distruzione della schermata il gateway singleton e il callback potevano sopravvivere. Non esisteva inoltre una prova ripetibile che il modulo voce non usi file, log o rete, né una misura del corpus reale per le soglie di categoria, deduplicazione e conferma esplicita.

**Approccio:** blindare il ciclo di vita del testo (reset su confirm/cancel/onCleared), documentare il contratto privacy nel gateway e nel ViewModel, aggiungere test statici/Robolectric sul modulo voce e test di lifecycle, misurare le metriche di accettazione su un corpus derivato da `catalog.db`, dichiarare `BLOCCATA` la deduplicazione non cablata e produrre un audit con matrice privacy, N+1 e protocollo tempi manuale. Nessun redesign, nessuna nuova libreria, nessuna modifica a dominio, Dark Mode, TalkBack o Dynamic Type.

## Boundaries & Constraints

**Sempre:**

- Il testo riconosciuto resta solo in memoria, dentro `VoiceRecognitionState`/`VoiceBottomSheetUiState`, per l'operazione corrente.
- `confirm()`, `cancel()` e `onCleared()` azzerano lo stato vocale; `onCleared()` rilascia il callback.
- Le stringhe visibili restano in italiano.
- I test di accettazione usano il catalogo reale e la query condivisa `CatalogQueries.SEARCH`; nessuna riga inventata.
- Le metriche non raggiunte restano rosse e vengono riportate con il valore calcolato.
- Le misure manuali usano il protocollo dell'audit, senza strumentazione in produzione.

**Mai:**

- Registrare audio, creare file `.amr`/`.wav`/`.pcm`, buffer PCM, `FileOutputStream`, `writeText`, `createTempFile`.
- Scrivere il testo riconosciuto in log (`Log.`, `println(`) o inviarlo a endpoint remoti.
- Aggiungere `INTERNET`/`ACCESS_NETWORK_STATE` o import di rete nel modulo voce.
- Persistere il testo fuori dall'eccezione documentata `LEARNED_RULE.userExpression`.
- Modificare Dark Mode 6.3, TalkBack 6.1, Dynamic Type/touch target 6.2.
- Implementare il rilevamento duplicati lato lista o rifattorizzare l'N+1: sono fuori perimetro.
- Eseguire build/test, committare, toccare `gradlew`, aggiungere dipendenze o cambiare versioni.

## I/O & Edge-Case Matrix

| Scenario | Input / Stato | Risultato / comportamento atteso | Gestione errori |
|---|---|---|---|
| Audio assente | Avvio ascolto con `RecognizerIntent` | Nessun file o buffer creato nel processo app; il servizio OS gestisce la cattura | Errore recognizer → `VoiceRecognitionState.Error` |
| Testo in memoria | `VoiceRecognitionState.Result("latte 1 l")` | `VoiceBottomSheetUiState.Ready`; testo solo nello StateFlow, nessuna persistenza | Nessuno |
| Conferma | Tap “Conferma” | Callback invocato una sola volta, gateway resettato, sheet `Hidden` | Tap successivo ignorato |
| Annulla / dismiss | Tap “Annulla” o swipe | Gateway resettato, stato `Hidden`, nessuna callback | Nessuno |
| Distruzione ViewModel | `ViewModelStore.clear()` | `onCleared()` → `gateway.reset()`, callback `null`, stato `Hidden` | Nessuno |
| Nuovo ascolto | `startListening()` dopo un risultato precedente | Stato `Listening`; il testo vecchio non viene riproposto né confermato | Nessuno |
| Permesso microfono negato | `RECORD_AUDIO` non concesso | Nessun ascolto; snackbar con motivazione e scorciatoia Impostazioni se permanente | Messaggio localizzato |
| App in background | Processo/Activity in pausa | Il testo resta in memoria solo finché il ViewModel vive; nessuna scrittura su disco | Nessuno |
| Processo terminato | Kill del processo | Nessun testo o audio ripristinabile: non esiste persistenza | Nessuno |
| Regola appresa | Correzione categoria confermata | `LEARNED_RULE.userExpression` persiste l'espressione normalizzata (eccezione documentata), non l'utterance completa | Nessuno |
| Corpus < 200 | Catalogo vuoto o filtro errato | Test rosso con messaggio esplicito; nessuna riga aggiunta a mano | `assertTrue` sul numero di casi |
| Metrica sotto soglia | Accuratezza/recall < target | Test rosso con valore nel messaggio; catalogo e soglie invariati | Assert dedicato |
| Deduplicazione lista | `DetectDuplicateUseCase` commentata | Metrica `BLOCCATA`, nessuna simulazione; tracciata in `deferred-work.md` | Test statico |

## Code Map

- `app/src/main/java/it/togo/app/presentation/voice/VoiceBottomSheetViewModel.kt` — KDoc privacy, `onCleared()` con `gateway.reset()`, callback `null`, stato `Hidden`; `confirm`/`cancel` restano l'unico consumo esplicito del testo.
- `app/src/main/java/it/togo/app/presentation/voice/VoiceConfirmationViewModel.kt` — KDoc privacy, `onCleared()` con stato `Hidden` e callback `null`.
- `app/src/main/java/it/togo/app/data/voice/AndroidVoiceRecognitionGateway.kt` — KDoc sul contratto: nessun audio/file/log, testo solo in `state`, reset, riconoscimento delegato all'OS; `acceptResult` invalida i risultati tardivi dopo reset/stop.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` — callback `onTextConfirmed` registrato in `DisposableEffect` e rilasciato in `onDispose`; nessuna assegnazione nel corpo della composable.
- `app/src/main/java/it/togo/app/presentation/voice/VoiceResilienceViewModel.kt` — il retry programmato viene annullato su `cancel()` e `onCleared()`; i callback vengono rilasciati.
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt` — sola lettura: `UiEvent.VoiceCommand -> { }` (testo scartato, comportamento noto) e N+1 documentato nell'audit, inclusi i loop annidati della tassonomia.
- `app/src/main/java/it/togo/app/domain/resolver/ProductResolver.kt` — **remediation**: il match esatto su `SYNONYM` precede la ricerca LIKE, così un sinonimo come "pane di" risolve il prodotto a cui è associato invece di diventare `Ambiguous` per il prefisso.
- `app/src/main/java/it/togo/app/domain/repository/CatalogRepository.kt` — **remediation**: nuovo `getByExactSynonym(term)` con implementazione di default `null` per non rompere i repository fake dei test.
- `app/src/main/java/it/togo/app/data/database/dao/CatalogDao.kt` e `app/src/main/java/it/togo/app/data/repository/RoomCatalogRepository.kt` — **remediation**: query `SELECT DISTINCT cp.* ... JOIN SYNONYM WHERE s.term = :term LIMIT 2`; il repository normalizza e considera risolvibile solo un singolo prodotto.
- `app/src/test/java/it/togo/app/privacy/VoicePrivacyAuditTest.kt` — **nuovo/esteso**: manifest senza rete, set permessi confrontato all'atteso (`VIBRATE`, `RECORD_AUDIO` + `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` app-scoped di AndroidX), scan statico di gateway/presentation/domain voice/parser/use case/`ActiveListScreen`, guardie anti-scan-vuoto, niente `rememberSaveable`/`SavedStateHandle`, allowlist `LEARNED_RULE.userExpression`.
- `app/src/test/java/it/togo/app/data/voice/AndroidVoiceRecognitionGatewayTest.kt` — **esteso**: risultato tardivo ignorato dopo `reset()` e `stopListening()`.
- `app/src/test/java/it/togo/app/presentation/voice/VoiceResilienceViewModelTest.kt` — **esteso**: `cancel()` annulla il retry programmato.
- `app/src/test/java/it/togo/app/presentation/voice/VoiceTextLifecycleTest.kt` — **nuovo**: cancel, confirm una sola volta, nuovo ascolto senza testo vecchio, `editText` non conferma, `onCleared` per entrambi i ViewModel.
- `app/src/test/java/it/togo/app/acceptance/CatalogAcceptanceSupport.kt` — **nuovo**: repository SQLite/JDBC basato su `CatalogQueries.SEARCH`, corpus nome+sinonimi, regole apprese vuote; **remediation**: override `getByExactSynonym` con la stessa semantica di `RoomCatalogRepository`.
- `app/src/test/java/it/togo/app/acceptance/CategoryAccuracyAcceptanceTest.kt` — **nuovo**: corpus ≥200, `ProductResolver` + `CategoryAssigner` reali; l'assigner è invocato su ogni `Single`, non solo sui prodotti già corretti.
- `app/src/test/java/it/togo/app/acceptance/ResolutionDedupAuditTest.kt` — **nuovo**: `computeAudit()` sospeso invocato con `runBlocking`, guardie corpus, precision/recall, nomi distinti, deduplicazione lista `BLOCCATA` tracciata nei documenti.
- `app/src/test/java/it/togo/app/acceptance/ZeroUnconfirmedChangesTest.kt` — **nuovo**: `VoiceCommand` ignorato non scrive e non cambia la lista; unico punto di emissione del testo in `confirm()`. Il test è dichiaratamente non end-to-end.
- `_bmad-output/implementation-artifacts/audit-6-4-privacy-performance.md` — **nuovo**: matrice privacy, performance statica, metriche, protocollo tempi, rischi.
- `_bmad-output/implementation-artifacts/deferred-work.md` — aggiunte le voci 6.4 (duplicati, ambiguità resolver, flusso legacy).
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — `6-4-privacy-audio-e-audit-performance: done`; `epic-6: done`.

## Tasks & Acceptance

**Execution:**

- [x] `VoiceBottomSheetViewModel` — `onCleared()` resetta il gateway, azzera `onTextConfirmed` e lo stato UI.
- [x] `VoiceConfirmationViewModel` — `onCleared()` porta a `Hidden` e azzera `onCommandConfirmed`.
- [x] KDoc privacy in gateway e nei due ViewModel.
- [x] Verifica che nessuna UI vocale usi `rememberSaveable` sul testo riconosciuto (nessun caso trovato).
- [x] `VoicePrivacyAuditTest` — manifest, scan statico, allowlist, niente rete/file/log/repository.
- [x] `VoiceTextLifecycleTest` — cancel/confirm/retry/onCleared, callback una sola volta.
- [x] `CatalogAcceptanceSupport` — DB reale via JDBC e `CatalogQueries.SEARCH`, corpus da nomi e sinonimi.
- [x] `CategoryAccuracyAcceptanceTest` — corpus 3.251 casi, resolver e assigner reali.
- [x] `ResolutionDedupAuditTest` — precision/recall, nomi distinti, blocco duplicati.
- [x] `ZeroUnconfirmedChangesTest` — zero scritture e unico punto di emissione.
- [x] Audit document con matrice privacy, N+1, metriche e protocollo tempi.
- [x] Voci mancanti aggiunte a `deferred-work.md`.
- [x] `sprint-status.yaml` aggiornato a `done` (storia chiusa con deroghe accettate; `epic-6` a `done`).
- [x] **Remediation review:** corretto il compile error di `ResolutionDedupAuditTest` (`computeAudit` sospeso + `runBlocking`).
- [x] **Remediation review:** guardie corpus/scan vuoti; scan privacy esteso; permessi manifest confrontati all'insieme atteso (con allowlist del permesso app-scoped AndroidX).
- [x] **Remediation review:** risultato vocale tardivo ignorato dopo reset/stop; test dedicati.
- [x] **Remediation review:** callback registrato in `DisposableEffect`; retry vocale annullato su cancel/onCleared.
- [x] **Remediation review:** assigner misurato su ogni `Single`; test SM-2 bloccato reso solo documentale.
- [x] **Remediation test rossi:** i due fallimenti sotto soglia sono stati risolti alla radice con il match esatto di sinonimo in `ProductResolver` (SM-1 e recall SM-2 a 100%), non ammorbidendo le soglie. Il terzo fallimento era il permesso app-scoped di AndroidX: il test lo allowlista esplicitamente invece di usare un subset.

**Acceptance Criteria:**

- [x] Given un risultato vocale, when si conferma/annulla o il ViewModel viene distrutto, then il testo non è più nello stato e il gateway è resettato.
- [x] Given un risultato tardivo dopo reset/stop, then il gateway lo ignora e non riapre il testo.
- [x] Given il manifest, then non contiene `INTERNET`/`ACCESS_NETWORK_STATE` e dichiara `VIBRATE` + `RECORD_AUDIO` più il solo `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` app-scoped aggiunto da AndroidX.
- [x] Given il modulo voice, then non contiene API audio/file/log/rete né riferimenti a repository/DAO; lo scan copre gateway, presentation, domain voice/parser, use case e `ActiveListScreen`.
- [x] Given un retry vocale programmato, when l'utente annulla o il ViewModel viene distrutto, then il retry è cancellato e il microfono non si riapre.
- [x] Given il catalogo reale, then il corpus derivato contiene 3.251 casi (≥200), senza righe inventate.
- [x] Given il corpus, when si calcola l'accuratezza categoria, then **100%** (3.251/3.251) dopo la remediation del resolver sul match esatto di sinonimo; soglia 95% invariata.
- [x] Given la deduplicazione esatta lato lista, then è dichiarata **BLOCCATA** con evidenza (`DomainModule.kt:42-43`, dialog senza call site), non simulata.
- [x] Given il resolver, when si calcola il recall, then **100%** (3.251/3.251 TP, 0 ambigui) dopo la remediation; soglia 95% invariata.
- [x] Given `UiEvent.VoiceCommand("latte 1 l")`, then zero scritture e lista/UI invariate nel comportamento attuale (evento ignorato; non è una prova end-to-end).
- [x] Given i tempi manuali, then la misura resta **BLOCCATA** finché l'utente non esegue il protocollo su device. Deroga accettata in chiusura: nessuna strumentazione in produzione, protocollo al §4 dell'audit, tracciata in `deferred-work.md`.
- [x] Given Dark Mode 6.3, TalkBack 6.1 e Dynamic Type 6.2, then nessuna modifica ai relativi file o semantiche.

## Implementation Notes

- **21/10/2026 — implementazione statica completata; test/build eseguiti dall'utente con esito positivo. Storia chiusa `done`.**
- **Privacy:** `onCleared()` è l'unico punto che garantisce la morte del testo alla distruzione del ViewModel. `confirm()` e `cancel()` già resettavano il gateway, ma non rilasciavano il callback; ora `onCleared()` lo azzera. In `ActiveListScreen` il callback è registrato in un `DisposableEffect(voiceViewModel)` con `rememberUpdatedState(onEvent)` e rilasciato in `onDispose`, non riassegnato nel corpo della composable.
- **Risultato tardivo:** `AndroidVoiceRecognitionGateway` ha una finestra `acceptResult` aperta da `startListening()` e chiusa da `reset()`, `stopListening()` o errore di launch. `onActivityResult` ignora tutto ciò che arriva a finestra chiusa. `unregisterLauncher()` non chiude la finestra: la rotazione ricrea il launcher mentre l'operazione può essere in corso; la chiusura resta a `reset`/`stop`/`onCleared`. Test: `reset`/`stop` non ricevono più il testo tardivo.
- **Retry vocale:** `VoiceResilienceViewModel` conserva il `Job` del retry e lo cancella su `cancel()`/`onCleared()`, oltre a rilasciare i callback. Senza questo, un retry con backoff poteva riaprire il microfono dopo l'annullamento.
- **Nessun `rememberSaveable`:** il testo vocale non è salvato in `SavedStateHandle` né in `rememberSaveable`; `VoiceConfirmationSheet` usa `remember` per l'input di modifica, non per la persistenza tra processi.
- **Manifest:** restano `VIBRATE` e `RECORD_AUDIO`. `VIBRATE` è un permesso normale, non sensibile. Nel manifest merged compare anche `it.togo.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, permesso app-scoped generato da AndroidX core per i receiver dinamici non esportati: non è un permesso di piattaforma e non dà accesso a rete, audio o storage. Il test lo allowlista esplicitamente, quindi un permesso realmente inatteso fa ancora fallire l'assert.
- **Corpus:** 1.470 nomi canonici + 1.781 sinonimi = 3.251 casi. La query è la stessa di Room (`CatalogQueries.SEARCH`), quindi il test non può divergere dal comportamento di produzione per la ricerca.
- **Metrica categoria:** conteggio stretto: solo `ResolutionResult.Single` con `CategoryAssigner` che conferma il L3 atteso. L'assigner viene invocato su **ogni** `Single` (non solo sui prodotti già corretti), così la metrica non è condizionata dal resolver. Dopo la remediation del resolver: **3.251/3.251 = 100%**, 0 `Ambiguous`, 0 `None`.
- **Corpus:** solo 1.470 nomi canonici + 1.781 sinonimi reali. L'AC parla anche di utterance libere/casi manuali: non sono state inventate righe; la deviazione è registrata in audit e deferred-work. La metrica non copre quindi il parsing di frasi complete.
- **Metrica resolver:** dopo la remediation, precision **100%** (3.251 TP, 0 FP) e recall **100%** (3.251 TP, 0 FN); i nomi canonici risolvono tutti al prodotto corretto (1.470/1.470).
- **Causa dei due rossi e remediation:** i 179 falsi negativi erano sinonimi generici (`pane di`, `latte di`, `tonno in`) che la sola ricerca LIKE rendeva `Ambiguous` perché prefissi di più nomi. Il resolver ora interroga `SYNONYM` per il match esatto prima della LIKE e restituisce `Single` con confidence 1.0. Il catalogo e le soglie non sono stati toccati; è stata corretta la causa nel dominio con una query aggiuntiva. Un eventuale termine mappato a più prodotti distinti resta non risolvibile (`singleOrNull`).
- **Deduplicazione lista:** `DetectDuplicateUseCase`/`ResolveDuplicateUseCase` non esistono come classi e sono commentate in `DomainModule.kt`; `DuplicateResolutionDialog` non ha call site. La metrica SM-2 esatta è `BLOCCATA`, non stimata.
- **N+1:** `buildUiState` esegue un `getById` per prodotto a ogni emissione (righe 95-96 e 107-109). Inoltre i gruppi sono costruiti con filtri annidati L1→L2 e L2→L3 (righe 117-123), costo `O(L1*L2 + L2*L3)` per emissione. Il fix richiede `getByIds`/cache e mappe di tassonomia; non è stato fatto in 6.4 ed è tracciato in `deferred-work.md`.
- **Test legacy:** `VoiceConfirmationViewModel`/`VoiceConfirmationSheet` restano non raggiungibili, ma il loro `onCleared` è stato blindato per coerenza. La rimozione/cablatura è tracciata in `deferred-work.md`.
- **Test SM-C2:** il test zero scritture è verde perché `VoiceCommand` è ignorato, non perché esista un percorso di conferma completo. Il KDoc del test lo dichiara; quando il cablaggio vocale sarà implementato, il test andrà sostituito.
- **Test SM-2 bloccato:** il test non verifica più che `DomainModule` contenga le righe commentate (cosa che premiava il difetto). Verifica solo che audit e `deferred-work.md` documentino il blocco; se SM-2 verrà implementata, il test va rimosso/aggiornato insieme ai documenti.
- **Scan privacy:** include `data/voice`, `presentation/voice`, `domain/voice`, `domain/parser`, `ParseVoiceCommandUseCase.kt` e `ActiveListScreen.kt`. Ogni path mancante o scan vuoto fallisce; i permessi manifest sono confrontati all'insieme atteso (`VIBRATE`, `RECORD_AUDIO`, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` app-scoped), non a un subset.

## Verification

**Commands (da eseguire manualmente da Mike; l'agente non esegue build/test):**

- `.\test-android.ps1` — attesi verdi: `VoicePrivacyAuditTest` (scan esteso, permessi allowlistati), `VoiceTextLifecycleTest`, `AndroidVoiceRecognitionGatewayTest` (risultati tardivi), `VoiceResilienceViewModelTest` (retry cancellato), `ZeroUnconfirmedChangesTest`, `CategoryAccuracyAcceptanceTest` (100%), `ResolutionDedupAuditTest` (precision, recall, nomi distinti, blocco documentato).
- `.\build-android.ps1` — attesa build Android completata senza errori.

**Esito (riportato dall'utente, 21/10/2026):** `.\test-android.ps1` e
`.\build-android.ps1` completati con esito positivo (486/486 test verdi), inclusi
`CategoryAccuracyAcceptanceTest` (100%) e `ResolutionDedupAuditTest` recall
(100%). I tre fallimenti precedenti sono stati corretti staticamente dall'agente:
due alla radice nel resolver, il terzo allowlistando il permesso app-scoped di
AndroidX. L'agente non ha eseguito i comandi.

**Manual checks:**

- Aprire l'app, concedere il microfono, dettare un prodotto, annullare: il bottom sheet si chiude e nessun testo resta visibile.
- Ripetere con conferma: il callback arriva una sola volta; la lista non cambia finché il cablaggio vocale non sarà implementato.
- Ruotare il device durante `Ready`: il testo non deve essere ripristinato da `rememberSaveable`.
- Verificare nelle impostazioni Android che TOGO non abbia accesso a rete (nessun permesso `INTERNET`).
- Eseguire il protocollo tempi dell'audit su device, 10 run manuali e 10 vocali — **rimandato** (deroga accettata in chiusura, tracciata in `deferred-work.md`).

## Spec Change Log

- 2026-10-21 — Remediation review: corretto il compile error di
  `ResolutionDedupAuditTest`; aggiunte guardie corpus/scan; invalidazione dei
  risultati tardivi; callback in `DisposableEffect`; cancellazione del retry
  vocale; scan privacy esteso; assigner misurato su ogni `Single`.
- 2026-10-21 — Remediation test rossi: risolti i tre fallimenti di
  `.\test-android.ps1`. I due test sotto soglia non sono stati ammorbiditi: il
  resolver ora privilegia il match esatto su `SYNONYM` rispetto alla LIKE, quindi
  i sinonimi generici non sono più `Ambiguous` (SM-1 e recall SM-2 passano a
  100%). `VoicePrivacyAuditTest` allowlista esplicitamente il permesso
  app-scoped `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` invece di confrontare un
  subset. Aggiunto test unitario `exact synonym beats ambiguous prefix candidates`.
- 2026-10-21 — Deviazioni accettate e tracciate: il corpus SM-1 usa solo
  nomi/sinonimi reali (nessuna utterance inventata); la deduplicazione esatta
  SM-2 resta `BLOCCATA`; i tempi manuali restano `BLOCCATA`; `VoiceCommand` è
  ignorato, quindi SM-C2 è verificato solo nel comportamento attuale.
- 2026-10-21 — **Story chiusa (`done`) ed Epic 6 chiusa (`done`).** Esito
  test/build positivo riportato dall'utente. Deroghe accettate esplicitamente
  dall'utente in chiusura: tempi manuali/voce `BLOCCATA` (richiedono device),
  deduplicazione esatta lato lista `BLOCCATA`, corpus senza utterance libere.
  Tutte restano tracciate in `deferred-work.md` come lavoro futuro.

## Review Triage Log

- **Fixed (statico, non eseguito):** E1/V1 compile error `computeAudit`
  (`suspend` + `runBlocking`); corpus vuoto; scan privacy vuoto; permessi
  manifest su subset; risultato recognizer tardivo dopo reset/stop; callback
  riassegnato in composizione; retry automatico dopo cancel; assigner non
  realmente misurato; test SM-2 che premiava il difetto; anchor fragili nei test
  statici.
- **Fixed (fallimenti test segnalati):** `CategoryAccuracyAcceptanceTest` e
  `ResolutionDedupAuditTest` recall risolti con il match esatto di sinonimo in
  `ProductResolver` + `CatalogRepository.getByExactSynonym` (query `SYNONYM`),
  senza toccare catalogo o soglie; `VoicePrivacyAuditTest` permessi risolto con
  allowlist esplicita del permesso app-scoped AndroidX.
- **Deferred/BLOCCATA:** N+1 prodotto e loop annidati tassonomia; deduplicazione
  esatta non cablata; corpus senza utterance libere per vincolo “nessuna riga
  inventata”; bound di `LEARNED_RULE.userExpression`; correlazione forte del
  risultato vocale con request id; tempi manuali su device.
- **Gate:** superato. Test/build positivi riportati dall'utente; deroghe
  `BLOCCATA` accettate in chiusura e tracciate in `deferred-work.md`. Storia e
  Epic 6 chiuse a `done`.
