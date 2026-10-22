---
title: '7.1 Aggiunta Nuovo Prodotto Canonico su Categoria Esistente'
type: 'feature'
created: '2026-10-21'
updated: '2026-10-22'
status: 'done'
story: 7.1
epic: 7
baseline_commit: '9ca14829e542e4a1af12a100000207b21b329592'
context:
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
---

## Story 7.1: Aggiunta Nuovo Prodotto Canonico su Categoria Esistente

### Obiettivo
L'utente può aggiungere un prodotto non presente nel catalogo scegliendo una categoria già esistente, completando la lista per prodotti di nicchia senza essere bloccato.

### Acceptance Criteria

**Given** l'utente ha cercato un prodotto non presente nel catalogo (nessun match esatto né per sinonimo)
**When** sceglie di aggiungere il prodotto manualmente con nome e categoria
**Then** il form richiede: nome prodotto (obbligatorio, univoco nel Livello 3 scelto), Livello 1, Livello 2, Livello 3 (tutti da selezionare tra quelli esistenti)
**And** prima del salvataggio viene verificato che non esista già un prodotto canonico con nome semanticamente identico nello stesso Livello 3; se trovato viene mostrato un avviso di possibile duplicato con opzione di procedere o annullare
**And** il prodotto viene salvato in `CANONICAL_PRODUCT` con `is_user_defined = 1`
**And** il prodotto user-defined è immediatamente disponibile nella ricerca per futuri inserimenti
**And** i prodotti user-defined sono distinguibili visivamente nella lista suggerimenti (es. badge "Personalizzato")

### Interpretazione vincolante di "univoco"
Il controllo produce un avviso con possibilità di procedere; NON va aggiunto un indice/constraint UNIQUE nel DB, altrimenti l'opzione "procedi" sarebbe impossibile.

### Decisioni di design

1. Il form di creazione è compatto e vive dentro `AddItemBottomSheet`, come sotto-flusso dello step 1. NON usare `ProductCategorizationWizard` e NON modificare `NewProductDraft`.
2. `CreateCanonicalProductUseCase` cambia API: `suspend operator fun invoke(name: String, level3Id: Long, confirmDuplicate: Boolean = false): CreateProductResult`. Il costruttore resta `(catalogRepository: CatalogRepository)`, quindi `DomainModule` non cambia.
3. `CreateProductResult` è una sealed interface nello stesso file: `Created(product)`, `Duplicate(candidate)`, `Invalid(message)`.
4. Il confronto duplicati è uguaglianza dopo normalizzazione nello stesso `level3Id`. Normalizzazione: trim, lowercase con `Locale.ROOT`, NFKD, rimozione dei segni diacritici, collapse degli spazi interni. Nessuna soglia, nessun Levenshtein, nessun fuzzy.
5. `CatalogRepository` aggiunge due metodi con default che lanciano `UnsupportedOperationException` (servono solo a non rompere i fake esistenti): `suspend fun getByLevel3(level3Id: Long): List<CanonicalProduct>` e `suspend fun insertProduct(name: String, level3Id: Long): CanonicalProduct`. `RoomCatalogRepository` li implementa entrambi.
6. `CanonicalProductEntity.id` diventa `@PrimaryKey(autoGenerate = true) val id: Long = 0`. NON cambiare `TogoDatabase.version`, NON aggiungere migrazioni, NON modificare `app/src/main/assets/catalog.db`. Motivo: con `autoGenerate` Room lega NULL per `id = 0` e SQLite assegna il rowid; la tabella dell'asset (`id INTEGER NOT NULL PRIMARY KEY`) è compatibile e la validazione Room non confronta AUTOINCREMENT. **Conseguenza accettata (2026-10-22, opzione A)**: l'identity hash dello schema cambia pur restando `version = 2`, quindi i DB dev esistenti vanno azzerati prima del primo avvio (disinstallare l'app o `adb shell pm clear it.togo.app`). La nota è riportata anche in `TogoDatabase.kt`.
7. La disponibilità nella ricerca è garantita da `CatalogQueries.SEARCH` (`cp.name LIKE` oppure `SYNONYM.term LIKE`) e dal fallback di `ProductResolver` sul nome. Poiché `SEARCH` normalizza solo la query e confronta il nome grezzo, alla creazione viene inserito anche un sinonimo accent-fold del nome: senza di esso "Caffè" non sarebbe trovato digitando "caffe". È lo stesso meccanismo usato dall'asset (`catalog.db`) e non introduce vincoli UNIQUE.
8. La tassonomia si carica quando si apre il form, con `catalogRepository.getTaxonomy().first()`. Filtra L2 per `level1Id` e L3 per `level2Id`. Per la 7.1 sono selezionabili solo L3 esistenti: `CatalogDao.getLevel3()` filtra già `is_user_defined = 0`.
9. Badge riutilizzabile `UserDefinedBadge` con `colors.badgeBg` / `colors.badgeInk`. Va mostrato sia nei suggerimenti sia nei risultati della schermata di ricerca.
10. Stringhe UI inline in italiano, coerenti con i composable esistenti (es. `ProductSearchField.kt:112`). NON introdurre un refactor i18n.
11. Accessibilità: touch target minimo 48dp, label esplicite, nessun testo solo colore, token del Design System (chiaro/scuro).
12. Fuori scope: Story 7.2, creazione di L1/L2/L3, deduplicazione di lista (Story 3.5), refactor N+1, voce, Dark Mode, TalkBack, Dynamic Type, popolamento dei nomi tassonomici nei risultati di ricerca.

### File modificati

- `app/src/main/java/it/togo/app/domain/catalog/ProductNameNormalizer.kt` (nuovo)
- `app/src/main/java/it/togo/app/domain/usecase/CreateCanonicalProductUseCase.kt` (modificato)
- `app/src/main/java/it/togo/app/domain/repository/CatalogRepository.kt` (aggiunti `getByLevel3`/`insertProduct`)
- `app/src/main/java/it/togo/app/data/database/entity/CatalogEntities.kt` (`@PrimaryKey(autoGenerate = true) val id: Long = 0`)
- `app/src/main/java/it/togo/app/data/database/dao/CatalogDao.kt` (`insertProduct`, `insertSynonym` + `getByLevel3`)
- `app/src/main/java/it/togo/app/data/database/CatalogQueries.kt` (`lowercase(Locale.ROOT)` allineato a `ProductNameNormalizer`)
- `app/src/main/java/it/togo/app/data/repository/RoomCatalogRepository.kt` (implementazione + sinonimo accent-fold)
- `app/src/main/java/it/togo/app/data/database/TogoDatabase.kt` (nota operativa: reset dati dev per cambio identity hash)
- `app/src/main/java/it/togo/app/presentation/additem/AddItemUiState.kt` (nuovi campi ed eventi)
- `app/src/main/java/it/togo/app/presentation/additem/AddItemViewModel.kt` (uso caso + gestione form + guardia rientranza)
- `app/src/main/java/it/togo/app/presentation/additem/AddItemBottomSheet.kt` (sotto-flusso compatto, dialog duplicato, TextButton "Aggiungi al catalogo")
- `app/src/main/java/it/togo/app/presentation/additem/NewProductForm.kt` (nuovo composable)
- `app/src/main/java/it/togo/app/presentation/additem/DuplicateProductWarningDialog.kt` (nuovo, estratto per testabilità)
- `app/src/main/java/it/togo/app/presentation/additem/ProductSearchField.kt` (badge `UserDefinedBadge`)
- `app/src/main/java/it/togo/app/presentation/search/SearchResultItem.kt` (badge `UserDefinedBadge`)
- `app/src/main/java/it/togo/app/presentation/components/UserDefinedBadge.kt` (nuovo composable)
- `app/src/main/java/it/togo/app/di/PresentationModule.kt` (`createProductUseCase` registrato)
- `app/src/test/java/it/togo/app/domain/catalog/ProductNameNormalizerTest.kt` (nuovo)
- `app/src/test/java/it/togo/app/domain/usecase/CreateCanonicalProductUseCaseTest.kt` (riscritto)
- `app/src/test/java/it/togo/app/presentation/additem/AddItemViewModelNewProductTest.kt` (nuovo)
- `app/src/test/java/it/togo/app/presentation/additem/AddItemViewModelEditTest.kt` (aggiornato)
- `app/src/test/java/it/togo/app/data/database/CatalogDaoUserProductTest.kt` (nuovo)
- `app/src/test/java/it/togo/app/data/repository/RoomCatalogRepositoryUserProductTest.kt` (nuovo)
- `app/src/test/java/it/togo/app/presentation/additem/NewProductFormTest.kt` (nuovo)
- `app/src/test/java/it/togo/app/presentation/additem/ProductSearchFieldBadgeTest.kt` (nuovo)
- `app/src/test/java/it/togo/app/presentation/additem/DuplicateProductWarningDialogTest.kt` (nuovo)
- `app/src/test/java/it/togo/app/presentation/additem/SearchStepNewProductEntryTest.kt` (nuovo)

### Task completati

- [x] Domain: ProductNameNormalizer, CreateCanonicalProductUseCase con CreateProductResult sealed
- [x] Data: CanonicalProductEntity.autoGenerate, CatalogDao insertProduct/insertSynonym/getByLevel3, RoomCatalogRepository implementazione + sinonimo accent-fold
- [x] ViewModel: AddItemUiState nuovi campi, gestione form 7.1, eventi Show/Hide/Confirm/Dismiss, guardia rientranza
- [x] UI: NewProductForm composable, UserDefinedBadge, ProductSearchField badge, SearchResultItem badge, AddItemBottomSheet integrazione form, DuplicateProductWarningDialog estratto
- [x] DI: PresentationModule registrazione createProductUseCase, AddItemBottomSheet factory aggiornata
- [x] Test: ProductNameNormalizerTest, CreateCanonicalProductUseCaseTest, AddItemViewModelNewProductTest, CatalogDaoUserProductTest, RoomCatalogRepositoryUserProductTest, NewProductFormTest, ProductSearchFieldBadgeTest, DuplicateProductWarningDialogTest, SearchStepNewProductEntryTest
- [x] Code review (2026-10-22): 4 layer (blind, edge case, verification gap, acceptance), rilievi triagati e fix applicati

### Test scritti (NON eseguiti)

- `ProductNameNormalizerTest`: accent-fold, trim, vuota/blank, accenti multipli
- `CreateCanonicalProductUseCaseTest`: nome vuoto -> Invalid, duplicato -> Duplicate, conferma -> Created, stesso nome in L3 diverso -> Created, varianti accentate/space -> Duplicate, isUserDefined vero
- `AddItemViewModelNewProductTest`: apertura form prefillata, L1 azzera L2/L3, senza L3 errore, duplicato candidato senza insert, conferma -> step 2, creazione normale -> step 2 PIECE user-defined, doppio tap non inserisce duplicati
- `CatalogDaoUserProductTest`: insertProduct id=0 genera id > 0, is_user_defined vero, ricercabile subito, secondo insert id crescente, due prodotti stesso nome nello stesso L3 entrambi ammessi
- `RoomCatalogRepositoryUserProductTest`: percorso di produzione DAO+repository, is_user_defined persistito, trim del nome, scoping per L3, ricerca immediata, nome accentato trovato da query senza accenti, duplicati ammessi
- `NewProductFormTest`: stato incompleto disabilita "Crea prodotto", stato completo abilita, errore visibile, salvataggio in corso disabilita
- `ProductSearchFieldBadgeTest`: badge "Personalizzato" appare solo per user-defined, visibile in SearchResultItem, nessun badge per prodotto catalogo
- `DuplicateProductWarningDialogTest`: mostra candidato e nuovo nome, "Procedi" invoca conferma, "Annulla" invoca chiusura
- `SearchStepNewProductEntryTest`: entry point visibile con query >= 2 caratteri e invoca il callback, nascosto con query corta

### Mappatura AC -> Codice/Test

| AC | Codice/Test |
|----|-------------|
| form richiede nome + L1/L2/L3 | `AddItemBottomSheet`/`NewProductForm` + validazione in `AddItemViewModel` |
| avviso duplicato Procedi/Annulla | `CreateCanonicalProductUseCase` + `DuplicateProductWarningDialog` |
| salvataggio is_user_defined=1 | `CatalogEntities.autoGenerate` + `RoomCatalogRepository` |
| disponibilità ricerca immediata | `CatalogDaoUserProductTest` + `RoomCatalogRepositoryUserProductTest` (incluso nome accentato) |
| badge "Personalizzato" suggerimenti | `ProductSearchFieldBadgeTest` |
| badge "Personalizzato" risultati | `SearchResultItem` + `ProductSearchFieldBadgeTest` |

### Rischi/BLOCCATE

- **Verifica build/test — OK (2026-10-22, eseguita dall'utente)**: `.\test-android.ps1` e `.\build-android.ps1` hanno completato con esito positivo dopo il fix del test di performance `CategorySelectionWizardTest`. File di riferimento: `app/src/test/java/it/togo/app/presentation/additem/AddItemViewModelNewProductTest.kt`, `app/src/test/java/it/togo/app/data/repository/RoomCatalogRepositoryUserProductTest.kt`.
- **Verifica manuale su device — non eseguita**: flusso utente reale non osservato.
- **Risolto — upgrade di un DB esistente (opzione A)**: `autoGenerate` cambia l'identità dello schema Room ma `TogoDatabase.version` resta 2 (decisione 6). Un DB creato da una build precedente fallisce `checkIdentity` a runtime. Scelta accettata: azzerare i dati in ambiente dev prima del primo avvio. Documentato in `TogoDatabase.kt` e nella decisione 6.
- **Rischio residuo — sinonimo non transazionale**: `RoomCatalogRepository.insertProduct` inserisce prodotto e sinonimo in due statement separati; un fallimento tra i due lascerebbe un prodotto ricercabile solo con nome accentato. Impatto molto basso (DB locale), tracciato in `deferred-work.md` se si vuole una `@Transaction`.

### Esito code review (2026-10-22)

Fix applicati:

- nome accentato non trovabile con query senza accenti: aggiunto sinonimo accent-fold in `insertProduct` (deroga motivata alla vecchia decisione 7)
- doppia creazione su tap ravvicinati: guardia `isCreatingProduct` su creazione e conferma duplicato
- dialog duplicato: estratto in `DuplicateProductWarningDialog`, touch target 48dp, coperto da test
- accessibilità form: errore su `warningInk` (leggibile in dark), `isError` solo quando il nome è vuoto, campi disabilitati durante il salvataggio, 48dp token al posto di `48.dp`
- `canCreateNewProduct` in `AddItemUiState` rimosso (dead code, duplicava la regola del form)
- `CatalogQueries.normalize` allineato a `Locale.ROOT` (stessa semantica di `ProductNameNormalizer`)
- test: aggiunti `RoomCatalogRepositoryUserProductTest`, `DuplicateProductWarningDialogTest`, `SearchStepNewProductEntryTest`; aggiunti `advanceUntilIdle()` mancanti in tre test del ViewModel
- documentazione: `deferred-work.md` (sezione 7.1 propria) e `sprint-status.yaml` aggiornati

Rilievi respinti o rinviati: entry point visibile solo con query >= 2 caratteri (scelta esplicita della spec), L1/L2 non persistiti nei nomi visualizzati (fuori scope, decisione 12), assenza di retry sul caricamento tassonomia (rinviato), `handlePreviousStep` non raggiungibile (rinviato). DB upgrade risolto con opzione A (reset dati dev documentato).

### Comandi di verifica per l'utente

1. `.\test-android.ps1` - esegue i test Robolectric/JUnit (non eseguiti da questo agente)
2. `.\build-android.ps1` - compila l'app (non eseguito)
3. Verifica manuale: aprire schermata Aggiungi voce -> cercare un prodotto non presente -> toccare "Non trovi il prodotto? Aggiungilo al catalogo" -> compilare nome + Livello 1/2/3 -> premere "Crea prodotto" -> verificare product creato con badge "Personalizzato" e disponibile nella ricerca successiva

### Review Findings

- [x] [Review][Decision] Upgrade di un DB esistente con schema `autoGenerate` — risolto con **opzione A**: reset dei dati in ambiente dev documentato in `TogoDatabase.kt:58-64` e nella decisione 6. Nessun bump di versione, nessuna migrazione.

- [x] [Review][Patch] Nome accentato non trovabile con query senza accenti — aggiunto sinonimo accent-fold in `RoomCatalogRepository.insertProduct` (`app/src/main/java/it/togo/app/data/repository/RoomCatalogRepository.kt:53-75`, `CatalogDao.kt:74-81`)
- [x] [Review][Patch] Doppia creazione su eventi ravvicinati — guardia `isCreatingProduct` in `handleCreateNewProduct`/`handleConfirmDuplicate` (`app/src/main/java/it/togo/app/presentation/additem/AddItemViewModel.kt:254-292`)
- [x] [Review][Patch] Dialog duplicato non testabile e touch target sotto 48dp — estratto `DuplicateProductWarningDialog` con `heightIn(min = spacing.touchTargetMin)` (`app/src/main/java/it/togo/app/presentation/additem/DuplicateProductWarningDialog.kt`)
- [x] [Review][Patch] Errore del form con colore `colors.error` illeggibile in dark e `isError` applicato al nome anche per errori di categoria — passato a `warningInk` e condizione `name.isBlank()` (`app/src/main/java/it/togo/app/presentation/additem/NewProductForm.kt:73-81,109-116`)
- [x] [Review][Patch] Campi modificabili durante il salvataggio — `enabled = !isSaving` su nome e dropdown (`NewProductForm.kt:82-107`)
- [x] [Review][Patch] Token hardcoded `48.dp` nel dropdown — sostituito con `spacing.touchTargetMin` (`NewProductForm.kt:176-179`)
- [x] [Review][Patch] `canCreateNewProduct` dead code in `AddItemUiState` — rimosso (`app/src/main/java/it/togo/app/presentation/additem/AddItemUiState.kt:62-73`)
- [x] [Review][Patch] Normalizzazione ricerca divergente (`lowercase()` locale-dependent) — allineata a `Locale.ROOT` (`app/src/main/java/it/togo/app/data/database/CatalogQueries.kt:42-55`)
- [x] [Review][Patch] Nessun test sul percorso di produzione DAO+repository — aggiunto `RoomCatalogRepositoryUserProductTest` (user-defined, trim, scoping L3, ricerca immediata, nome accentato, duplicati ammessi)
- [x] [Review][Patch] Nessun test sul dialog duplicato e sull'entry point — aggiunti `DuplicateProductWarningDialogTest` e `SearchStepNewProductEntryTest`
- [x] [Review][Patch] Tre test del ViewModel leggevano `uiState.value` senza `advanceUntilIdle()` — corretto (`AddItemViewModelNewProductTest.kt`) e aggiunto test doppio tap
- [x] [Review][Patch] Voci 7.1 sotto la sezione 6.1 di `deferred-work.md` e `last_updated` non aggiornato — sezione dedicata + data (`sprint-status.yaml`, `deferred-work.md`)

- [x] [Review][Defer] Wizard 5-step non raggiungibile — deferred: preesistente, fuori perimetro 7.1 (`ProductCategorizationWizard.kt`)
- [x] [Review][Defer] Bug `NewProductDraft.toCanonicalProduct` con `id = finalLevel3Id` — deferred: classe non usata dalla 7.1 (`NewProductDraft.kt:172`)
- [x] [Review][Defer] Nessun percorso di modifica/eliminazione dei prodotti user-defined — deferred: richiede una storia di gestione catalogo utente
- [x] [Review][Defer] `handlePreviousStep` non raggiungibile con form aperto — deferred: il tasto di sistema chiude il bottom sheet; decidere `BackHandler` o rimozione del ramo
- [x] [Review][Defer] Caricamento tassonomia senza retry — deferred: errore raro (DB locale), richiede stato di retry
- [x] [Review][Defer] `tools/generate_catalog.py` non allineato ad `AUTOINCREMENT` — deferred: nessun impatto runtime, il prossimo asset rigenerato divergerebbe
- [x] [Review][Defer] Prodotto e sinonimo inseriti in due statement non transazionali — deferred: impatto molto basso, valutare `@Transaction`

#### Rejected

- Entry point "Aggiungi al catalogo" visibile solo con query >= 2 caratteri — **false**: è la condizione esplicita della decisione di design; il comportamento è coperto da test.
- L1/L2 non persistiti e nomi tassonomici non mostrati nei risultati — **false**: fuori scope per decisione 12; preesistente a questa storia.
- Modifica della spec sotto review (data/date, duplicati negli elenchi, typo) — **low**: non è un difetto di codice; corretto come housekeeping nella spec.
- Scroll/IME e live region sul messaggio di errore — **low**: casi limite non quotidiani, il fix aggiungerebbe complessità non richiesta.
- Controllo di esistenza del L3 al momento dell'insert — **low**: i L3 sono statici e non eliminabili nella 7.1.

### Spec Change Log

- 2026-10-21: creazione iniziale stato `in-progress -> review`
- 2026-10-22: code review; corretto il meccanismo di ricerca (sinonimo accent-fold), fix UI/accessibilità, test aggiunti; stato resta `review` su richiesta esplicita (verifica build/test ancora BLOCCATA)
- 2026-10-22: decisione DB risolta con opzione A — reset dati dev documentato in `TogoDatabase.kt`, nessun bump di versione
- 2026-10-22: test e build eseguiti dall'utente con esito OK; storia chiusa `done`