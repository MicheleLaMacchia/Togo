---
title: 'Story 4.1: VoiceRecognitionGateway e RecognizerIntent it-IT'
type: 'feature'
created: '2026-10-13'
status: 'done'
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-7-wizard-categorizzazione-prodotto.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-6-assegnazione-categoria-canonica.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'utente deve poter aggiungere prodotti alla lista tramite comando vocale in italiano. Android espone `RecognizerIntent` per avviare il riconoscimento vocale di sistema, ma serve un'astrazione nel dominio (`VoiceRecognitionGateway`) che:
1. Avvii il riconoscimento con lingua `it-IT` (BCP-47)
2. Catturi il testo riconosciuto (o errori/annullamento)
3. Esponga il risultato al presentation layer per il parsing (Story 4.3)

**Approach:** Implementare:
1. **VoiceRecognitionGateway** — Interfaccia nel dominio per avviare/ascoltare riconoscimento
2. **AndroidVoiceRecognitionGateway** — Implementazione Android che lancia `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` con `EXTRA_LANGUAGE = "it-IT"` e riceve risultato via `ActivityResultLauncher`
3. **VoiceRecognitionResult** — Sealed interface: `Success(text)`, `NoSpeech`, `Error(code, message)`, `Cancelled`
4. **VoiceRecognitionState** — Stato UI: `Idle`, `Listening`, `Processing`, `Result(text)`, `Error(message)`

## Boundaries & Constraints

**Always:**
- Lingua riconoscimento: `it-IT` (BCP-47)
- `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` con `EXTRA_LANGUAGE` e `EXTRA_LANGUAGE_MODEL`
- Il testo riconosciuto viene passato al parser (Story 4.3), NON interpretato qui
- Il gateway NON fa parsing né categorizzazione
- Il gateway NON è offline (RecognizerIntent può usare servizi remoti)
- Implementazione Android richiede `RECORD_AUDIO` permission
- Presentation layer osserva stato via StateFlow/SharedFlow

**Never:**
- Non usare API vocali custom o SDK esterni
- Non fare riconoscimento continuo (one-shot, un comando per volta)
- Non testare il dominio con implementazione Android reale (usare interfaccia + mock)
- Non supporre che RecognizerIntent sia disponibile sempre (alcuni device potrebbero non averlo)

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Avvio riuscito | `startListening()` | Stato → `Listening`, microfono attivo | N/A |
| Riconoscimento riuscito | Utente parla "latte" | `VoiceRecognitionResult.Success("latte")` | N/A |
| Nessun parlato | Utente non dice nulla | `VoiceRecognitionResult.NoSpeech` | UI mostra "Non ho capito, riprova" |
| Errore riconoscimento | Errore di rete o mic | `VoiceRecognitionResult.Error(code, msg)` | UI mostra errore specifico |
| Annullamento utente | Back press / tap annulla | `VoiceRecognitionResult.Cancelled` | Stato → `Idle` |
| Riconoscimento parziale | "latte intero" | `Success("latte intero")` | Parser gestisce (Story 4.3) |
| Permesso negato | `RECORD_AUDIO` non concesso | `Error(MISSING_PERMISSION)` | UI richiede permesso |
| RecognizerIntent non disponibile | Device senza Google | `Error(NOT_AVAILABLE)` | UI suggerisce input manuale |

## Code Map

- `app/src/main/java/it/togo/app/domain/voice/VoiceRecognitionResult.kt` — Sealed interface: Success, NoSpeech, Error, Cancelled
- `app/src/main/java/it/togo/app/domain/voice/VoiceRecognitionGateway.kt` — Interfaccia dominio
- `app/src/main/java/it/togo/app/domain/voice/VoiceRecognitionState.kt` — Stato UI: Idle, Listening, Processing, Result, Error
- `app/src/main/java/it/togo/app/data/voice/AndroidVoiceRecognitionGateway.kt` — Implementazione Android con RecognizerIntent
- `app/src/main/java/it/togo/app/presentation/activelist/VoiceFab.kt` — UI FAB microfono (integra gateway)
- `app/src/test/java/it/togo/app/domain/voice/VoiceRecognitionResultTest.kt` — Test JVM sealed class
- `app/src/test/java/it/togo/app/domain/voice/VoiceRecognitionGatewayTest.kt` — Test JVM con mock

## Tasks & Acceptance

**Execution:**
- [x] `VoiceRecognitionResult.kt` — Sealed interface con 4 varianti
- [x] `VoiceRecognitionGateway.kt` — Interfaccia dominio
- [x] `VoiceRecognitionState.kt` — Stato UI sealed class
- [x] `AndroidVoiceRecognitionGateway.kt` — Implementazione Android RecognizerIntent
- [x] `VoiceFab.kt` — UI FAB con stato microfono
- [x] `VoiceRecognitionResultTest.kt` — Test JVM
- [x] `VoiceRecognitionGatewayTest.kt` — Test JVM con mock

**Acceptance Criteria:**
- Given avvio `startListening()`, when RecognizerIntent lanciato, then stato → `Listening`
- Given testo riconosciuto "latte", when risultato ricevuto, then `Success("latte")`
- Given nessun parlato, when timeout, then `NoSpeech`
- Given errore riconoscimento, when errore, then `Error(code, msg)`
- Given annullamento, when utente annulla, then `Cancelled`
- Given lingua configurata, when intent launched, then `EXTRA_LANGUAGE = "it-IT"`
- Given permesso negato, when startListening, then `Error(MISSING_PERMISSION)`
- Tutte operazioni dominio <100ms (test JVM)
- `gradlew help` passa

## Dev Approvals

- [x] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **VoiceRecognitionResult**: Sealed interface con 4 varianti: `Success(text)`, `NoSpeech`, `Error(code, message)`, `Cancelled`. Proprietà `isValidText` e `recognizedText` per convenienza.
- **ErrorCode**: Enum con 7 valori: MISSING_PERMISSION, NOT_AVAILABLE, NETWORK_ERROR, RECOGNIZER_ERROR, AUDIO_ERROR, CLIENT_BUSY, UNKNOWN.
- **VoiceRecognitionState**: Sealed interface con 5 stati: Idle, Listening, Processing, Result(text), Error(code, message). Proprietà `isListening`, `isError`, `isResult`, `resultText`.
- **VoiceRecognitionGateway**: Interfaccia dominio con `state: StateFlow<VoiceRecognitionState>`, `suspend startListening()`, `stopListening()`, `reset()`.
- **AndroidVoiceRecognitionGateway**: Implementazione Android con `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`, `EXTRA_LANGUAGE = "it-IT"`, `LANGUAGE_MODEL_FREE_FORM`. Gestisce `onActivityResult` via `handleActivityResult()`. Richiede `RECORD_AUDIO` permission e `Activity` reference.
- **VoiceFab**: FAB Compose con animazione pulse durante ascolto, colore dinamico per stato, label stato. `VoiceListeningOverlay` per overlay schermo intero con annullamento.
- **DI**: Registrato in DomainModule: `VoiceRecognitionGateway` → `AndroidVoiceRecognitionGateway`.
- **Test JVM**: 24 test totali (14 + 10). FakeVoiceRecognitionGateway per test dominio senza Android SDK.
- **Build**: `gradlew help` OK (JDK 17).

## Review Fix Log

- 2026-10-13 — Review completata (3 lenti, 8 findings). Fix applicati:
  - `DomainModule.kt`: import corretto da `it.togo.app.domain.voice.AndroidVoiceRecognitionGateway` a `it.togo.app.data.voice.AndroidVoiceRecognitionGateway`
  - `DomainModule.kt`: registrazione Koin senza iniezione Activity (gateway disaccoppiato)
  - `AndroidVoiceRecognitionGateway`: rimosso costruttore con Activity, aggiunto pattern `registerLauncher(ActivityResultLauncher)` per evitare memory leak
  - `AndroidVoiceRecognitionGateway`: aggiunto controllo permesso RECORD_AUDIO via `hasRecordAudioPermission()`
  - `AndroidVoiceRecognitionGateway`: `onActivityResult` coerente con `VoiceRecognitionResult` (RESULT_CANCELED → Idle, RESULT_OK → Result/Error)
  - `AndroidVoiceRecognitionGateway`: rimosso `startActivityForResult` deprecato, usa `ActivityResultLauncher.launch()`
  - `VoiceFab`: rimossi import inutilizzati (`MaterialTheme`, `fontSize`, `FontWeight`, `RadioButton`)
  - `spec-4-1`: task checkbox allineate con stato `done`
  - `AndroidVoiceRecognitionGatewayTest`: aggiunti 10 test JVM per la implementazione reale (non solo fake)

## Spec Change Log

- 2026-10-13 — Creazione spec e implementazione completata, stato `in-progress` → `done`