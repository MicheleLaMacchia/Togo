---
title: 'Ripristino compilazione Android'
type: 'bugfix'
created: '2026-10-15'
status: 'done'
route: 'dispatch'
baseline_commit: '763005f07f0ab658bb64232161d6e7c5859402d5'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** La compilazione Android fallisce con numerosi errori Kotlin/KSP in dominio, design system, lista attiva, ricerca, inserimento, storico, regole apprese e voce. Gli errori a cascata nascondono incompatibilità reali fra API e modelli.

**Approach:** Correggere tutte le cause radice indicate dal log, mantenendo il comportamento e le dipendenze architetturali esistenti. Rendere coerenti i token/API Compose e i contratti fra ViewModel, modelli e repository; non eseguire build o test Gradle in questa iterazione, perché l'utente lancerà la build.

**Always:** Intervenire solo sui problemi necessari per gli errori riportati; rispettare Clean Architecture, Kotlin/Compose e il design system TOGO; preservare le modifiche esistenti e non rimuovere funzionalità per silenziare errori.

**Never:** Lanciare build/test Gradle; effettuare commit; eseguire reset o scartare modifiche; introdurre `TODO()` o sopprimere errori senza correggere il contratto sottostante.

</frozen-after-approval>

## Code Map

- `app/src/main/java/it/togo/app/domain/category/CategoryAssigner.kt` -- raccoglie tassonomia da `Flow`; manca l'operatore `first` e va tipizzato il fallback L3.
- `domain/resolver/ProductResolver.kt` -- ricerca Flow e mappatura `CanonicalProduct` a `ResolvedProduct`; manca import `first`, gli errori successivi sono cascata.
- `domain/usecase/ShareListUseCase.kt`, `presentation/activelist/ShareListHelper.kt` -- consumano flow di item/tassonomia; `groupBy` non può chiamare funzioni suspend, quindi i prodotti vanno caricati prima del raggruppamento.
- `ui/theme/{Color,ComponentTokens,Shape,Spacing,Typography,TogoTheme}.kt` -- fonti token correnti; CompositionLocal deve avere tipo providable, shape usa `androidx.compose.foundation.shape`, consumer UI richiede API legacy equivalenti. `ComponentTokens` factory è `togoComponentTokensFrom`.
- `presentation/activelist/ActiveListScreen.kt`, `ActiveListViewModel.kt`, `ActiveListUiState.kt` -- API bottom sheet/import/material errati, eventi e `AddItemConfirmed`, snackbar/context; `TaxonomyCache` è data class e genera già `componentN`; `ShoppingItem.copyWith` non accetta `updatedAt`.
- `presentation/search/SearchScreen.kt`, `SearchResultItem.kt`, `SearchViewModel.kt` -- definizioni `SearchResultItem` e helper highlight duplicate; import Compose/Flow e TextField API non coerenti.
- `presentation/additem/{AddItemBottomSheet,AddItemViewModel,AddItemUiState,AttributeSection,ProductCategorizationWizard,ProductSearchField,QuantityUnitPicker}.kt` -- import mancanti, sheet/TextField API, campi `CanonicalProduct` non esistenti e stato UI incoerente.
- `presentation/{history/HistoryScreen.kt,rules/LearnedRulesScreen.kt,rules/LearnedRulesViewModel.kt,voice/VoiceBottomSheet.kt,voice/VoiceConfirmationSheet.kt,activelist/CategoryHeader.kt,activelist/EmptyState.kt,activelist/ItemRow.kt,activelist/PresiSection.kt,activelist/VoiceFab.kt}` -- consumer legacy del design system, import mancanti e simboli icona.
- `app/build.gradle.kts`, `gradle/libs.versions.toml` -- dipendenze Compose; aggiungere solo artefatti mancanti necessari, allineati al BOM già dichiarato.

## Tasks & Acceptance

**Execution:**
- [x] Correggere gli operatori Flow e il resolver/assegnatore in `CategoryAssigner.kt`, `ProductResolver.kt` e `ShareListUseCase.kt`; rendere la condivisione tipizzata e senza chiamate suspend in lambda sincrone.
- [x] Allineare `Color.kt`, `ComponentTokens.kt`, `Shape.kt`, `Spacing.kt`, `Typography.kt` e `TogoTheme.kt`: usare package corretti Compose, CompositionLocal providable, un solo accessor/factory e token compatibili coi consumer.
- [x] Riparare i contratti e le API Compose nei file `activelist`, `search`, `additem`, `history`, `rules` e `voice` elencati in Code Map; eliminare duplicati e import non validi, preservando i flussi UI.
- [x] Aggiornare `app/build.gradle.kts` e `gradle/libs.versions.toml` solo per dipendenze non presenti, mantenendo BOM e versioni allineati.

**Acceptance Criteria:**
- Given il sorgente corrente e gli errori del log fornito, when ogni riferimento è confrontato staticamente con le API, modelli e dipendenze dichiarati, then non restano riferimenti mancanti, overload duplicati, mismatch di parametri o errori di contesto Compose nei file interessati.
- Given `TogoTheme`, when i composable sono eseguiti sotto il tema, then i CompositionLocal forniscono un singolo valore coerente per colori, tipografia, spacing, shape e token component.
- Given i flussi ricerca, condivisione e lista attiva, when sono consumati dai ViewModel/use case, then i tipi restano determinati e le funzioni suspend sono invocate esclusivamente da coroutine.
- Given la richiesta dell'utente, when questa iterazione termina, then non è stata eseguita alcuna build/test Gradle e nessun commit.

## Implementation Notes

- Aggiunti gli import Flow mancanti; la condivisione pre-carica i prodotti prima del raggruppamento non-suspend.
- Ripristinata compatibilità dei token TOGO con i consumer legacy, correggendo i package shape/BorderStroke e CompositionLocal providable.
- Allineate le schermate e i ViewModel alle API Material3/Compose correnti, ai modelli presenti e alle dipendenze Lifecycle/icone.
- Revisione statica: ripristinati dispatch di modifica preesistente, feedback swipe densità-aware e bordi di focus/separazione; i problemi runtime non necessari alla compilazione sono registrati in `deferred-work.md`.
- Verifiche svolte solo staticamente (`git diff --check` e controlli di riferimenti/import). Build e test Gradle non eseguiti, come richiesto; compilazione da confermare dall'utente.

## Design Notes

Preferire compatibilità esplicita con i consumer attuali invece di rinominare API pubbliche in massa. Distinguere i primi errori radice dalle diagnostiche a cascata; non assumere che l'aggiunta di import risolva mismatch di modello o API Material legacy/Material3.

## Verification

**Comandi:** Nessun comando Gradle in questa iterazione, su richiesta dell'utente. Verifica tramite ispezione statica di import, firme, tipi, parentesi e chiamanti; la build finale sarà lanciata dall'utente.

## Spec Change Log

## Review Triage Log

| Finding | Verdict, evidence, route |
|---|---|
| Blind 1 — AddItemViewModel factory/DI | `medium / defer` — Il composable usa la factory preesistente `viewModel()` senza fornire le dipendenze richieste; il problema è runtime, non una delle diagnostiche di compilazione riportate. |
| Blind 2 — tap su item apre insert invece di edit | `false` — Il dispatch è stato ripristinato a `UiEvent.EditItem`; il call site non apre più la sheet né può inserire un duplicato. Il gestore edit preesistente resta un flusso incompleto separato. |
| Blind 3 — callback conferma e chiusura sheet | `medium / defer` — Il ViewModel persiste direttamente e la callback host non è consumata; è un problema del flusso Add Item, non necessario a risolvere i riferimenti Kotlin del log. |
| Blind 4 — VoiceCommand senza handler | `medium / defer` — L'evento aggiunto per tipizzare il callback è ignorato dal ViewModel; il parsing/inserimento vocale è un comportamento applicativo fuori dal perimetro di questo fix di compilazione. |
| Blind 5 — default unit | `false` — `CanonicalProduct` non dichiara `defaultUnit`; il codice sceglie PIECE se compatibile, altrimenti la prima unità disponibile. Non esiste nel modello un valore default ignorato. |
| Blind 6 — soglia swipe in pixel | `medium / patch` — La soglia ora deriva da 80.dp convertiti con `LocalDensity`; l'azione delete e l'affordance sottostante sono mantenute. |
| Blind 7 — lookup prodotto ripetuti | `medium / defer` — Il ViewModel effettua lookup per item a ogni emissione; è un'ottimizzazione/performance, non blocca la compilazione richiesta. |
| Blind 8 — errore ricerca non mostrato | `low / defer` — L'errore di Flow non viene presentato dal campo ricerca; è feedback UX non richiesto dal log di compilazione. |
| Blind 9 — indicatore focus ricerca rimosso | `low / patch` — Ripristinato il bordo accent tramite `onFocusChanged`; il bordo hairline resta per il campo non focalizzato. |
| Blind 10 — bordo sezione Presi su tutti i lati | `low / patch` — Sostituito il bordo completo con un solo separatore inferiore disegnato dietro l'header. |
| Edge 1 — Flow tassonomia può completare senza emissioni | `false` — I DAO Room espongono query osservabili Flow; il repository combina i tre Flow Room che emettono anche liste vuote, non un Flow one-shot che completa normalmente senza valore. |
| Edge 2 — eccezione da lookup lista | `medium / defer` — Un errore Room potrebbe terminare la raccolta della lista, ma è resilienza runtime, non compilazione. |
| Edge 3 — eccezione da lookup condivisione | `medium / defer` — Un errore Room potrebbe interrompere la condivisione; è gestione runtime fuori dal log richiesto. |
| Edge 4 — soglia swipe in pixel | `medium / patch` — Duplicato del finding Blind 6; la conversione dp→px è stata applicata. |
| Edge 5 — nessun affordance delete durante swipe | `medium / patch` — La riga ora scorre sopra lo sfondo rosso con icona Elimina visibile durante il trascinamento. |
| Edge 6 — percorso edit crea duplicato | `false` — Il click sulla riga non apre più la sheet di inserimento; il dispatch preesistente è ripristinato. |
| Verification 1 — nessun test per swipe delete | `medium / defer` — Non risultano test che coprano il gesto; aggiungerli non è necessario a correggere le diagnostiche e l'utente ha riservato la build a sé. |
| Verification 2 — edit non passa item al ViewModel | `false` — Il call site non apre più la sheet in modalità edit; nessun inserimento duplicato è raggiungibile da quel tap. La modifica vera resta un problema preesistente separato. |

### Instradamento dei gruppi

- `patch`: swipe manuale (soglia dp→px e affordance), bordo focus ricerca e separatore inferiore della sezione Presi; corretti nell'iterazione di review.
- `false`: l'unità default non è presente in `CanonicalProduct`; la sheet edit non viene aperta dal tap e quindi non può inserire duplicati; la tassonomia Room emette liste anche quando vuote.
- `defer`: integrazione/runtime della sheet AddItem, dispatch effettivo dei comandi vocali, lookup/performance/errori Room, feedback degli errori di ricerca, completamento dell'edit preesistente e test del gesto swipe; fuori dalle correzioni di compilazione richieste.
