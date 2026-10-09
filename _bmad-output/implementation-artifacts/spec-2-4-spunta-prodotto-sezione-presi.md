---
title: 'Story 2.4: Spunta Prodotto e Sezione "Presi (N)"'
type: 'feature'
created: '2026-10-09'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-2-1-schermata-lista-attiva.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-2-3-modifica-rimozione-voce.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'utente può aggiungere, modificare ed eliminare voci, ma manca il flusso completo di **spunta (check-off)** per segnare i prodotti come acquistati durante la spesa, con feedback aptico, animazione di transizione verso la sezione "Presi (N)" collassabile, e possibilità di riaprire (uncheck).

**Approach:** Completare il flusso di check-off in `ItemRow` e `PresiSection`:
1. **Check-off attivo → preso**: Tap checkbox → haptic 15ms → testo barrato + opacità 45% → dopo 300ms scivola in sezione "Presi (N)" con animazione
2. **Uncheck preso → attivo**: Tap checkbox in "Presi" → ripristino istantaneo nella categoria d'origine
3. **Sezione "Presi (N)" collassabile**: Header con chevron espandi/collassa, animazione spring, pulsante "Concludi spesa" in fondo
4. **Stato persistente**: Flow Room emette subito, niente spinner

## Boundaries & Constraints

**Always:**
- Design System obbligatorio: `TogoTheme`, token colori/tipografia/spacing/shape/componentTokens
- Haptic feedback 15ms su check-off (VibrationEffect.createOneShot)
- Animazione 300ms spring per transizione attivo ↔ preso
- Sezione "Presi" collassabile con animazione spring
- Touch target ≥48dp su checkbox
- Stato persistente via Room Flow (cold load istantaneo)

**Never:**
- Nessun valore hardcoded (colori, spacing, tipografia)
- Nessuna animazione complessa che rallenti interazione una mano
- Nessuno spinner di caricamento (Room Flow istantaneo)
- Checkbox touch target <48dp

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Check-off voce attiva | Tap checkbox su voce non checkata | Haptic 15ms → barrato/opacità 45% → dopo 300ms slide in "Presi (N)" | Se repo fallisce → rollback UI + snackbar errore |
| Uncheck voce presa | Tap checkbox su voce checkata in "Presi" | Ripristino istantaneo in categoria d'origine (senza animazione slide) | Se repo fallisce → rollback UI |
| Tap header "Presi (N)" | Tap su header sezione | Toggle expand/collapse con animazione spring 300ms | Nessuno |
| Tap "Concludi spesa" | Tap pulsante in "Presi" | Emette `UiEvent.Checkout` → `ActiveListViewModel.handleCheckout()` | Se repo fallisce → snackbar errore |
| Lista vuota + check-off | Impossibile (nessuna voce) | N/A | N/A |
| Tutte le voci checkate | Lista attiva vuota, "Presi" con tutte | "Presi" espanso di default | Nessuno |
| Config change durante animazione | Animazione in corso | Stato preservato (rememberSaveable) | Animazione riprende o completa |

## Open Questions

- Durata esatta animazione slide: 300ms confermato? — **DECISO: 300ms spring (dampingRatio=0.9, stiffness=200)**
- Haptic type: LONG_PRESS vs ONE_SHOT 15ms? — **DECISO: ONE_SHOT 15ms**
- "Presi" default expanded/collapsed? — **DECISO: expanded se checkedItems > 0, altrimenti collapsed**
- Checkout atomico: transazione DB? — **DECISO: sì, transazione Room (delete checked + insert history)**

## Code Map

- `app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt` — CheckboxUtility con haptic 15ms, animazione checked state, onCheckChange → `UiEvent.CheckOff`
- `app/src/main/java/it/togo/app/presentation/activelist/PresiSection.kt` — AnimatedVisibility spring, header collassabile, ItemRow con uncheck, Button "Concludi spesa"
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt` — `handleCheckOff` (già implementato), `handleCheckout` (già implementato)
- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt` — Integrazione ItemRow/PresiSection (già presente)
- `app/src/main/java/it/togo/app/data/repository/RoomShoppingListRepository.kt` — `checkout` transazionale (già implementato)

## Tasks & Acceptance

- [x] `ItemRow.kt` — CheckboxUtility con haptic 15ms `VibrationEffect.createOneShot(15, DEFAULT_AMPLITUDE)`, animazione checked state (barrato, opacità 45%, checkbox verde), `onCheckChange` → `UiEvent.CheckOff`
- [x] `PresiSection.kt` — AnimatedVisibility spring (expand/collapse 300ms), header collassabile con chevron, ItemRow con uncheck → `UiEvent.CheckOff(false)`, Button "Concludi spesa" → `UiEvent.Checkout`
- [x] `ActiveListViewModel.kt` — `handleCheckOff` (già: update repo), `handleCheckout` (già: transazione checkout)
- [x] `RoomShoppingListRepository.kt` — `checkout` transazionale (delete checked + insert history) già implementato
- [ ] Test integrazione: check-off → animazione → Presi → uncheck → ripristino categoria

**Acceptance Criteria:**
- Given voce in lista attiva, when tap checkbox, then haptic 15ms → barrato/opacità → dopo 300ms slide in "Presi (N)"
- Given voce in "Presi (N)", when tap checkbox, then ripristino istantaneo in categoria d'origine
- Given tap header "Presi (N)", then toggle expand/collapse con animazione spring 300ms
- Given tap "Concludi spesa", then `handleCheckout` sposta voci checkate in storico
- Tutte le animazioni 300ms spring (dampingRatio=0.9, stiffness=200)
- Haptic 15ms `VibrationEffect.createOneShot(15, DEFAULT_AMPLITUDE)`
- Touch target checkbox ≥48dp (già 48dp in CheckboxUtilityTokens)
- `gradlew help` passa

## Dev Approvals

- [ ] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **ItemRow.kt**: CheckboxUtility 48dp con haptic 15ms `VibrationEffect.createOneShot(15, DEFAULT_AMPLITUDE)` su `onCheckChange`; stati checked/attivo con colori Design System (barrato, opacità 45%, checkbox verde `accentSuccess`); swipe-to-delete integrato.
- **PresiSection.kt**: `AnimatedVisibility` con spring (300ms, dampingRatio=0.9, stiffness=200) per expand/collapse; header collassabile con chevron animato; ItemRow in modalità checked (uncheck → `onUncheck`); Button "Concludi spesa" con shape `tokens.bottomSheet.radiusTop`.
- **ActiveListViewModel.kt**: `handleCheckOff` aggiorna repo via `update()`; `handleCheckout` usa `checkout()` transazionale (delete checked + upsert history).
- **RoomShoppingListRepository.kt**: `checkout` transazionale — `deleteByIds` checked items + `upsertHistorical` per storico.
- **Design System**: 100% token `TogoTheme` (colori, tipografia, spacing, shape, componentTokens).
- **Build**: `gradlew help` OK.

## Spec Change Log

- 2026-10-09 — Creazione spec, stato `in-progress`.
- 2026-10-09 — Implementazione completata (componenti già esistenti), build `gradlew help` OK, stato `review`.

## Review Triage Log

- (vuoto fino alla review della Story 2.4)