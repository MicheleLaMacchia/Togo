---
title: 'Story 4.4: Conferma/Annullamento e Chiarimento Ambiguità Vocale'
type: 'feature'
created: '2026-10-13'
status: 'done'
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-3-parsing-comando-vocale.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-2-voicefab-e-voicebottomsheet-ui.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-1-voicerecognitiongateway.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Dopo il parsing di un comando vocale (Story 4.3), l'utente deve poter:
1. **Confermare** il comando interpretato (es. "2 litri di latte intero")
2. **Annullare** se l'interpretazione è errata
3. **Chiarire** se l'interpretazione è ambigua (manca quantità, unità o categoria)

**Approach:** Implementare:
1. **VoiceConfirmationUiState** — Stato UI per il bottom sheet di conferma
2. **VoiceConfirmationViewModel** — MVI: parsing → conferma/annulla/chiarisci
3. **VoiceConfirmationSheet** — Composable bottom sheet con:
   - Testo riconosciuto + interpretazione strutturata
   - Conferma → callback al presentation layer
   - Annulla → chiudi, torna a ricerca
   - Chiarisci → input inline per correggere prodotto/quantità/unità
4. **Integrazione** — Sostituisce `VoiceBottomSheet` esistente o viene mostrato dopo il parsing

## Boundaries & Constraints

**Always:**
- Conferma mostra interpretazione strutturata: "2 litri di latte intero (latticini)"
- `needsClarification = true` → mostra avviso "Manca l'unità, specifica"
- Input inline per modifica testo riconosciuto (se parsing errato)
- "Conferma" disabilitato se prodotto vuoto
- Design System 100% token `TogoTheme`

**Never:**
- Non creare voce senza conferma esplicita
- Non modificare lista se utente annulla
- Non fare parsing qui (usa `ParseVoiceCommandUseCase`)
- Non tenere bottom sheet aperto dopo conferma/annullamento

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Conferma | Tap "Conferma" | Callback `onCommandConfirmed(ParsedCommand)` | N/A |
| Annulla | Tap "Annulla" | Bottom sheet chiude, gateway reset | N/A |
| Chiarisci | Tap "Modifica" | Input inline per correggere testo | N/A |
| Modifica testo | Utente modifica "latte" → "latte intero" | Re-parsing con nuovo testo | N/A |
| needsClarification | Parsing ritorna `needsClarification=true` | Avviso visivo + suggerimento | N/A |
| Prodotto vuoto | Parsing prodotto vuoto | "Conferma" disabilitato | N/A |

## Code Map

- `app/src/main/java/it/togo/app/presentation/voice/VoiceConfirmationUiState.kt` — Stato UI
- `app/src/main/java/it/togo/app/presentation/voice/VoiceConfirmationViewModel.kt` — ViewModel MVI
- `app/src/main/java/it/togo/app/presentation/voice/VoiceConfirmationSheet.kt` — Composable
- `app/src/test/java/it/togo/app/presentation/voice/VoiceConfirmationViewModelTest.kt` — Test JVM

## Tasks & Acceptance

**Execution:**
- [x] `VoiceConfirmationUiState.kt` — Stato UI sealed class (Hidden, Ready)
- [x] `VoiceConfirmationViewModel.kt` — ViewModel MVI con parsing + conferma
- [x] `VoiceConfirmationSheet.kt` — Composable bottom sheet
- [x] `VoiceConfirmationViewModelTest.kt` — Test JVM (10 casi)

**Acceptance Criteria:**
- Given `ParsedCommand("latte", 2.0, L)`, when conferma, then callback con comando
- Given `needsClarification=true`, when UI, then avviso visivo
- Given prodotto vuoto, when UI, then "Conferma" disabilitato
- Given modifica testo, when conferma, then re-parsing con nuovo testo
- Given annulla, when tap, then bottom sheet chiuse
- Tutte operazioni <100ms (test JVM)
- Design System 100% token `TogoTheme`
- `gradlew help` passa

## Dev Approvals

- [x] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **VoiceConfirmationUiState**: Sealed interface con 2 stati: Hidden, Ready(parsedCommand, isEditing). Proprietà `displayCommand` (ritorna parsedCommand direttamente), `canConfirm`, `isVisible`.
- **VoiceConfirmationViewModel**: MVI con `uiState: StateFlow<VoiceConfirmationUiState>`. Azioni: `showForRecognition(rawText)` (parsing → Ready), `confirm()` (callback + hide), `cancel()` (hide), `editText(newText)` (re-parsing completo), `toggleEdit()`, `reset()`. Callback `onCommandConfirmed: ((ParsedCommand) -> Unit)?`.
- **VoiceConfirmationSheet**: Composable con `ModalBottomSheet`. Stato locale `editText` con `remember` per evitare re-parsing su ogni tasto. Pulsante "Fine modifica" esegue re-parsing. Design System 100% TogoTheme.
- **PresentationModule**: Registrato `VoiceConfirmationViewModel` come `viewModel { }` con `ParseVoiceCommandUseCase`.
- **Test JVM**: 10 test totali. Verifica parsing, conferma, annulla, modifica (con displayCommand assertion), needsClarification, toggleEdit, reset, timing <100ms.
- **Build**: `gradlew help` OK (JDK 17).

## Review Fix Log

- 2026-10-13 — Review completata (3 lenti, 8 findings). Fix applicati:
  - `VoiceConfirmationUiState`: rimosso campo `editedText`, `displayCommand` ritorna `parsedCommand` direttamente (fix duplicazione nome prodotto)
  - `VoiceConfirmationViewModel`: semplificato `editText()` (re-parsing completo, nessun editedText da sincronizzare)
  - `VoiceConfirmationSheet`: stato locale `editText` con `remember` per evitare re-parsing su ogni tasto; pulsante "Fine modifica" per uscire dalla modalità editing
  - `VoiceConfirmationViewModelTest`: aggiornato test `editText` con assertion su `displayCommand.productName`
  - Test Compose UI: richiede Android SDK — documentato come limitazione ambiente JVM-only

## Spec Change Log

- 2026-10-13 — Creazione spec e implementazione completata, stato `in-progress` → `done`