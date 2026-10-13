---
title: 'Story 4.2: VoiceFab e VoiceBottomSheet UI'
type: 'feature'
created: '2026-10-13'
status: 'done'
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-1-voicerecognitiongateway.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-3-7-wizard-categorizzazione-prodotto.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'utente ha bisogno di un'interfaccia per il comando vocale: un FAB per avviare il riconoscimento e un bottom sheet che mostra lo stato (ascolto, elaborazione, risultato, errore) con il testo riconosciuto e le azioni (conferma, riprova, annulla).

**Approach:** Implementare:
1. **VoiceBottomSheet** — Composable bottom sheet che mostra:
   - Stato `Listening`: indicatore animato + "Ascolto in corso..."
   - Stato `Processing`: spinner + "Elaborazione..."
   - Stato `Result`: testo riconosciuto + azioni (Conferma / Riprova / Modifica)
   - Stato `Error`: messaggio errore + azioni (Riprova / Annulla)
2. **VoiceBottomSheetViewModel** — Gestisce stato bottom sheet + interazione con `VoiceRecognitionGateway`
3. **VoiceBottomSheetState** — Stato UI del bottom sheet
4. **Integrazione** — `VoiceFab` (Story 4.1) + `VoiceBottomSheet` insieme in `ActiveListScreen`

## Boundaries & Constraints

**Always:**
- Bottom sheet mostra solo quando `state != Idle`
- `Result`: testo riconosciuto modificabile inline prima di confermare
- `Error`: messaggio specifico da `ErrorCode`
- "Riprova" richiama `startListening()`
- "Conferma" passa testo al callback `onTextConfirmed(String)`
- "Annulla" chiude bottom sheet, reset gateway
- Design System 100% token `TogoTheme`
- Bottom sheet si chiude automaticamente dopo conferma/annullamento

**Never:**
- Non aprire bottom sheet se `state == Idle`
- Non modificare testo riconosciuto senza conferma utente
- Non tenere bottom sheet aperto se gateway va in Error senza azioni
- Non duplicare logica del gateway nel presentation layer

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Avvio vocale | Tap FAB → `startListening()` | Bottom sheet apre con stato `Listening` | N/A |
| Riconoscimento riuscito | Gateway → `Result("latte")` | Bottom sheet mostra "Latte" + azioni | N/A |
| Errore permesso | Gateway → `Error(MISSING_PERMISSION)` | Bottom sheet mostra "Permesso microfono negato" + Riprova | N/A |
| Errore rete | Gateway → `Error(NETWORK_ERROR)` | Bottom sheet mostra errore rete + Riprova/Annulla | N/A |
| Modifica testo | Utente modifica "latte" → "latte intero" | Testo aggiornato, pronto per conferma | N/A |
| Conferma | Tap "Conferma" | Callback `onTextConfirmed("latte intero")` | N/A |
| Riprova | Tap "Riprova" | Gateway `reset()` + `startListening()` | N/A |
| Annulla | Tap "Annulla" | Bottom sheet chiude, gateway `reset()` | N/A |
| Bottom sheet doppio tap | Tap fuori sheet | Si chiude (default Material3) | N/A |

## Code Map

- `app/src/main/java/it/togo/app/presentation/voice/VoiceBottomSheet.kt` — Composable bottom sheet
- `app/src/main/java/it/togo/app/presentation/voice/VoiceBottomSheetViewModel.kt` — ViewModel MVI
- `app/src/main/java/it/togo/app/presentation/voice/VoiceBottomSheetUiState.kt` — Stato UI
- `app/src/main/java/it/togo/app/di/PresentationModule.kt` — Registrazione ViewModel
- `app/src/test/java/it/togo/app/presentation/voice/VoiceBottomSheetViewModelTest.kt` — Test JVM

## Tasks & Acceptance

**Execution:**
- [x] `VoiceBottomSheetUiState.kt` — Stato UI sealed class (Hidden, Listening, Processing, Ready, Error)
- [x] `VoiceBottomSheetViewModel.kt` — ViewModel MVI con gateway (startListening, editText, confirm, retry, cancel)
- [x] `VoiceBottomSheet.kt` — Composable bottom sheet (Listening/Processing/Ready/Error content)
- [x] `PresentationModule.kt` — Registrazione ViewModel con import corretti
- [x] `VoiceBottomSheetViewModelTest.kt` — Test JVM (12 casi: MVI, azioni, callback, timing)

**Acceptance Criteria:**
- Given `Listening`, when bottom sheet visibile, then mostra indicatore animato + "Ascolto in corso..."
- Given `Result("latte")`, when bottom sheet visibile, then mostra testo + Conferma/Riprova/Modifica
- Given `Error(code, msg)`, when bottom sheet visibile, then mostra errore + Riprova/Annulla
- Given modifica testo, when conferma, then `onTextConfirmed` chiamato con testo modificato
- Given `Riprova`, when tap, then gateway `reset()` + `startListening()`
- Given `Annulla`, when tap, then bottom sheet chiuse, gateway `reset()`
- Tutte operazioni <100ms (test JVM)
- Design System 100% token `TogoTheme`
- `gradlew help` passa

## Dev Approvals

- [x] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **VoiceBottomSheetUiState**: Sealed interface con 5 stati: Hidden, Listening, Processing, Ready(recognizedText, editedText, isEditing), Error(code, message). Proprietà `isVisible` e `canConfirm`.
- **VoiceBottomSheetViewModel**: MVI con `uiState: StateFlow<VoiceBottomSheetUiState>`. Osserva `gateway.state` e traduce in UI state. Azioni: `startListening()`, `editText(newText)`, `confirm()` (invoca callback + reset), `retry()` (reset + start), `cancel()` (reset + hide). Callback `onTextConfirmed: ((String) -> Unit)?` per passare testo al presentation layer.
- **VoiceBottomSheet**: Composable con `ModalBottomSheet` (skipPartiallyExpanded). Contenuto per stato: Listening (pulse animation + annulla), Processing (spinner), Ready (testo + Modifica/Riprova/Conferma), Error (icona + Riprova/Annulla). Design System 100% TogoTheme.
- **PresentationModule**: Registrato `VoiceBottomSheetViewModel` come `viewModel { }` (factory, non singleton). Import corretti per `VoiceRecognitionGateway` e `VoiceBottomSheetViewModel`.
- **Test JVM**: 12 test totali. FakeGateway per test MVI senza Android SDK. Verifica transizioni stato, azioni utente, callback, timing <100ms.
- **Build**: `gradlew help` OK (JDK 17).

## Review Fix Log

- 2026-10-13 — Review completata (3 lenti, 7 findings). Fix applicati:
  - `PresentationModule.kt`: aggiunto import `org.koin.androidx.viewmodel.dsl.viewModel`
  - `ActiveListScreen.kt`: integrazione `VoiceBottomSheet` + `VoiceBottomSheetViewModel` con callback `onTextConfirmed`
  - `ActiveListScreen.kt`: `VoiceFab` con parametro `state` mappato da `VoiceBottomSheetUiState`
  - `VoiceBottomSheet.kt`: rimosso wrapper `AnimatedVisibility` (conflitto animazione/scrim con `ModalBottomSheet`)
  - `VoiceBottomSheetViewModel.kt`: `retry()` imposta `Listening` sincrono prima di `reset()` per evitare flicker
  - `VoiceBottomSheet.kt`: `ReadyContent` toggle pulsanti Modifica/Annulla modifica in base a `isEditing`
  - Test Compose UI: richiede Android SDK (createComposeRule) — attualmente non eseguibile in ambiente JVM-only

## Spec Change Log

- 2026-10-13 — Creazione spec e implementazione completata, stato `in-progress` → `done`