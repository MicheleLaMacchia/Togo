---
title: 'Story 4.5: Fallback e Resilienza Vocale'
type: 'feature'
created: '2026-10-13'
status: 'done'
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-4-conferma-annullamento-e-chiarimento-ambiguita-vocale.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-3-parsing-comando-vocale.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-2-voicefab-e-voicebottomsheet-ui.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-1-voicerecognitiongateway.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Il riconoscimento vocale può fallire per vari motivi: permesso negato, riconoscitore non disponibile, errore di rete, timeout, audio non valido. Serve un sistema di resilienza che:
1. Gestisca i retry con backoff esponenziale (max 3 tentativi)
2. Offra fallback a input manuale quando il vocale non è disponibile
3. Mostri messaggi di errore specifici e azioni appropriate
4. Registri i fallimenti per diagnostica

**Approach:** Implementare:
1. **VoiceResiliencePolicy** — Politica di retry con backoff (max 3 tentativi, delay 500ms/1000ms/2000ms)
2. **VoiceFallbackHandler** — Gestisce fallback: se vocale fallisce 3 volte → suggerisce input manuale
3. **VoiceErrorRecovery** — Mappa ErrorCode → azione di recupero (Riprova / Input manuale / Impostazioni)
4. **VoiceResilienceViewModel** — Gestisce stato resilienza nel flusso vocale

## Boundaries & Constraints

**Always:**
- Max 3 tentativi di riconoscimento prima di suggerire fallback
- Backoff: 500ms, 1000ms, 2000ms tra i tentativi
- Fallback a input manuale dopo 3 fallimenti consecutivi
- Messaggi errore specifici per ErrorCode
- Logging dei fallimenti per diagnostica (TODO: Analytics)
- Design System 100% token `TogoTheme`

**Never:**
- Non superare 3 tentativi (evita loop infiniti)
- Non suggerire input manuale se il permesso è negato (invece: guida a Impostazioni)
- Non fare retry automatico senza feedback visivo all'utente
- Non modificare la lista se il comando vocale fallisce

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Permesso negato | `Error(MISSING_PERMISSION)` | Suggerisci "Vai a Impostazioni" per concedere microfono | Nessun retry |
| Riconoscitore non disponibile | `Error(NOT_AVAILABLE)` | Suggerisci input manuale immediato | Nessun retry |
| Errore rete (tentativo 1) | `Error(NETWORK_ERROR)` | Retry automatico con backoff 500ms | Mostra "Riprova..." |
| Errore rete (tentativo 3) | `Error(NETWORK_ERROR)` | Suggerisci input manuale | Fallback |
| Audio non valido | `Error(AUDIO_ERROR)` | Retry con backoff | Massimo 3 tentativi |
| Timeout | `Error(AUDIO_ERROR)` | Retry con backoff | Massimo 3 tentativi |
| Fallback attivo | 3 fallimenti consecutivi | Mostra "Inserisci manualmente" + apri AddItemBottomSheet | N/A |
| Riprova utente | Tap "Riprova" | Incrementa counter, retry con backoff | Reset counter dopo successo |

## Code Map

- `app/src/main/java/it/togo/app/domain/voice/VoiceResiliencePolicy.kt` — Politica retry con backoff
- `app/src/main/java/it/togo/app/domain/voice/VoiceFallbackHandler.kt` — Gestore fallback
- `app/src/main/java/it/togo/app/domain/voice/VoiceErrorRecovery.kt` — Mappa ErrorCode → azione recupero
- `app/src/main/java/it/togo/app/presentation/voice/VoiceResilienceViewModel.kt` — ViewModel resilienza
- `app/src/test/java/it/togo/app/domain/voice/VoiceResiliencePolicyTest.kt` — Test JVM
- `app/src/test/java/it/togo/app/domain/voice/VoiceFallbackHandlerTest.kt` — Test JVM

## Tasks & Acceptance

**Execution:**
- [x] `VoiceResiliencePolicy.kt` — Politica retry con backoff esponenziale
- [x] `VoiceFallbackHandler.kt` — Gestore fallback con soglia 3 tentativi
- [x] `VoiceErrorRecovery.kt` — Mappa ErrorCode → azione recupero
- [x] `VoiceResilienceViewModel.kt` — ViewModel che orchestra retry + fallback
- [x] `VoiceResiliencePolicyTest.kt` — Test JVM (9 casi)
- [x] `VoiceFallbackHandlerTest.kt` — Test JVM (10 casi)

**Acceptance Criteria:**
- Given `Error(NETWORK_ERROR)` tentativo 1, when resilienza, then retry con backoff 500ms
- Given 3 fallimenti consecutivi, when resilienza, then fallback a input manuale
- Given `Error(MISSING_PERMISSION)`, when resilienza, then nessun retry, suggerisci Impostazioni
- Given `Error(NOT_AVAILABLE)`, when resilienza, then fallback immediato
- Given successo dopo retry, when resilienza, then reset counter
- Tutte operazioni <100ms (test JVM)
- `gradlew help` passa

## Dev Approvals

- [x] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **VoiceResiliencePolicy**: Data class con `maxAttempts=3`, `baseDelayMs=500`, `backoffMultiplier=2.0`. `getDelayMs(attempt)` calcola backoff esponenziale (500, 1000, 2000ms). `canRetry(attempt)` verifica se sotto il limite.
- **VoiceFallbackHandler**: Traccia `consecutiveFailures` e `lastErrorCode`. `recordFailure()` incrementa e verifica soglia. `recordSuccess()` resetta. `isFallbackActive()` verifica se soglia raggiunta.
- **VoiceErrorRecovery**: Object con `getRecoveryAction(code, attempt, maxAttempts)` → mappa ErrorCode a VoiceRecoveryAction (Retry, UseManualInput, OpenSettings, NoAction). `canRetry()` verifica se errore permette retry. `getUserMessage()` fornisce messaggio user-friendly.
- **VoiceResilienceViewModel**: MVI con `recoveryAction`, `errorMessage`, `isFallbackActive` StateFlow. Osserva `gateway.state` e applica politica. `executeRecovery(action)` esegue azione (Retry, UseManualInput, OpenSettings). Callback `onFallbackToManualInput` e `onOpenSettings`.
- **PresentationModule**: Registrato `VoiceResilienceViewModel` come `viewModel { }`.
- **Test JVM**: 19 test totali (9 + 10). Verifica policy, fallback, recovery actions, timing <100ms.
- **Build**: `gradlew help` OK (JDK 17).

## Review Fix Log

- 2026-10-13 — Review completata (3 lenti, 7 findings). Fix applicati:
  - `PresentationModule.kt`: aggiunto import `VoiceResilienceViewModel`, rimossa registrazione duplicata
  - `VoiceFallbackHandler.kt`: corretta logica fallback da `&&` a `||` (per errori ritentabili ora ritorna true dopo maxAttempts)
  - `VoiceResilienceViewModel.kt`: aggiunto `delay(backoff)` in `executeRecovery(Retry)` per applicare backoff esponenziale
  - `VoiceResilienceViewModelTest.kt`: aggiunti 12 test per ViewModel resilienza
  - `VoiceErrorRecoveryTest.kt`: aggiunti 13 test per mapping errori
  - Integrazione UI completa: richiede ActiveListScreen (documentato come prossimo step)

## Spec Change Log

- 2026-10-13 — Creazione spec e implementazione completata, stato `in-progress` → `done`