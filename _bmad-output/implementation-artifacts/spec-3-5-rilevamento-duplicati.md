---
title: 'Story 3.5: Rilevamento Duplicati e Dialog Risoluzione'
type: 'feature'
created: '2026-10-12'
status: 'in-progress'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_project-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_project-output/planning-artorts/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_project-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_project-output/implementation-artifacts/spec-3-4-attributi-prodotto.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Quando l'utente aggiunge un prodotto già presente nella lista stessa (stesso CanonicalProduct, stessa categoria canonica), TOGО deve rilevare il duplicato e permettere all'utente di scegliere esplicitamente cosa fare: sommare le quantità, sostituirle, o creare una voce separata.

**Approach:** Implementare:
1. **Rilevamento duplicati**: Verifica se esiste già un prodotto con lo stesso `productId` nella lista attiva
2. **Dialog risoluzione**: UI modale con 3 opzioni:
   - **Somma quantità** (+N → Totale M) — se unità compatibili
   - **Sostituisci quantità** (N) — rimpiazza la quantità esistente
   - **Crea voce separata** — aggiunge come nuova voce
   - **Annulla** — non modifica nulla
3. **Gestione stato**: Il ViewModel gestisce il flusso di selezione → rilevamento → risoluzione

## Boundaries & Constraints

**Always:**
- Rilevamento basato su `productId` e `level3Id` (stessa categoria canonica)
- Conversione automatica solo se unità compatibili (kg↔g, l↔ml)
- Conferma esplicita prima di qualsiasi modifica
- Operazione non completata lascia la lista invariata

**Never:**
- Non creare duplicati silenziosamente
- Non sommare quantità di unità incompatibili
- Non modificare la lista senza conferma dell'utente

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Duplicato stessa unità | "latte 2 l" esiste con "1 l" | Dialog: Somma (+2l → 3l), Sostituisci, Separa | N/A |
| Duplicato unità diverse compatibili | "latte 500 ml" esiste con "1 l" | Dialog: Somma (+500ml → 1.5l), Sostituisci, Separa | Conversione automatica |
| Duplicato unità incompatibili | "latte 500 ml" esiste con "1 kg" | Dialog: Sostituisci, Separa (no Somma) | Somma disabilitata |
| Nessun duplicato | Prodotto non in lista | Inserimento diretto senza dialog | N/A |
| Annullamento | Tap su Annulla o fuori dialog | Lista invariata | N/A |
| Quantità zero o negativa | Input non valido | Blocca inserimento | Errore inline |

## Open Questions

- Categoria canonica per il confronto: `level3Id` sufficiente? → **DECISO: sì, level3Id rappresenta la categoria merceologica**
- Differenziazione per marca/variante? → **DECISO: no, il duplicato è basato solo su productId + level3Id**
- Cosa succede se il prodotto originale è già stato spuntato? → **DECISO: nessuna differenza, il dialog appare comunque**

## Code Map

- `app/src/main/java/it/togo/app/presentation/activelist/ActiveListViewModel.kt` — Gestione flusso duplicati
- `app/src/main/java/it/togo/app/presentation/additem/DuplicateResolutionDialog.kt` — UI dialog risoluzione
- `app/src/main/java/it/togo/app/domain/model/ResolutionOption.kt` — Enum opzioni risoluzione
- `app/src/test/java/it/togo/app/presentation/activelist/DuplicateResolutionTest.kt` — Test JVM

## Tasks & Acceptance

**Execution:**
- [x] `ResolutionOption.kt` — Enum: `SUM`, `REPLACE`, `SEPARATE`, `CANCEL`
- [x] `DuplicateResolutionDialog.kt` — Dialog con 3 opzioni + annulla, mostra quantità attuale e nuova
- [x] `ActiveListViewModel.kt` — Rilevamento duplicati, gestione flusso risoluzione, operazioni CRUD
- [x] `DuplicateResolutionTest.kt` — Test JVM: rilevamento, somma, sostituzione, separazione, annullamento

**Acceptance Criteria:**
- Given prodotto già in lista, when nuovo inserimento stesso prodotto, then dialog risoluzione appare
- Given dialog aperto, when tap "Somma", then quantità viene sommata (se compatibili)
- Given dialog aperto, when tap "Sostituisci", then quantità viene rimpiazzata
- Given dialog aperto, when tap "Separa", then nuova voce viene creata
- Given dialog aperto, when tap "Annulla", then lista resta invariata
- Tutte le operazioni <100ms (test JVM)
- Design System 100% token `TogoTheme`

## Dev Approvals

- [ ] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

## Spec Change Log

## Review Triage Log