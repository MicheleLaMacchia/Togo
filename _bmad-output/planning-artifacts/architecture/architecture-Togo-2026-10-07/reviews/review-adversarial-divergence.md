# Review: Adversarial Divergence & Invariant Seams

**Target:** `ARCHITECTURE-SPINE.md`  
**Date:** 2026-10-07  
**Reviewer:** Adversarial Seams Lens  
**Verdict:** PASS (with prior tightening applied)  

## Attack Vectors Tested

1. **Active List State vs Voice Entry Mutations:**
   - *Test:* Can `VoiceInputViewModel` and `ActiveListViewModel` simultaneously mutate `SHOPPING_ITEM` resulting in clashing rows?
   - *Result Closed:* AD-2 specifies Room SQLite as the single writer SSOT. All writes route through `ShoppingListRepository`. UI observes immutable `Flow<T>`.
2. **Duplicate Detection Semantics:**
   - *Test:* Could two engineers interpret duplicate handling differently (e.g. merging units vs creating separate rows vs silent overwriting)?
   - *Result Closed:* AD-5 explicitly defines the 4-stage pipeline and exact behaviors for "Somma quantità", "Sostituisci", and "Crea voce separata". Incompatible units cannot be automatically summed.
3. **Checkout Transaction Boundary:**
   - *Test:* Could checkout drop items or retain checked items on failure?
   - *Result Closed:* AD-2 mandates an atomic Room transaction moving `is_checked = 1` rows to `HISTORICAL_ITEM` while deleting from `SHOPPING_ITEM` and preserving unchecked items.
4. **Offline Isolation:**
   - *Test:* Could an engineer make speech-to-text failure block manual item creation?
   - *Result Closed:* AD-4 enforces decoupling of `VoiceRecognitionGateway` with typed fallback `SpeechResult.Failure` leaving manual input completely functional.

## Findings
- All potential divergence seams are closed with enforceable AD rules.
