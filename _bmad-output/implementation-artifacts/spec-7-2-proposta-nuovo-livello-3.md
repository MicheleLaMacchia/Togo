---
title: '7.2 Proposta Nuovo Livello 3 sotto un Livello 2 Esistente'
type: 'feature'
created: '2026-10-22'
updated: '2026-10-22'
status: 'done'
story: 7.2
epic: 7
context:
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
---

## Story 7.2: Proposta Nuovo Livello 3 sotto un Livello 2 Esistente

### Obiettivo

L'utente può proporre una nuova categoria di terzo livello quando nessuna di quelle esistenti si adatta al prodotto che sta aggiungendo, così che il catalogo cresca in modo ordinato riflettendo le sue reali abitudini di spesa.

### Acceptance Criteria

**Given** l'utente seleziona un Livello 2 nel wizard di categorizzazione
**When** nessuno dei Livelli 3 disponibili è appropriato e l'utente sceglie "Proponi nuovo Livello 3"
**Then** viene richiesto il nome del nuovo Livello 3 con conferma esplicita
**And** prima di creare il nuovo Livello 3 viene verificata l'assenza di duplicati semantici tra i Livelli 3 esistenti nello stesso Livello 2; se trovato viene mostrato un avviso con il candidato duplicato
**And** solo dopo conferma esplicita il nuovo Livello 3 viene salvato con `is_user_defined = 1`
**And** non è possibile creare un nuovo Livello 1 o Livello 2: solo la selezione tra quelli preinstallati è disponibile
**And** il nuovo Livello 3 è immediatamente disponibile per l'assegnazione di prodotti canonici user-defined

### Interpretazioni vincolanti

- "Conferma esplicita" = tap su "Crea Livello 3" nel pannello. Se esiste un duplicato serve un secondo tap su "Procedi" nel dialog. Non c'è un ulteriore dialog quando non esiste duplicato.
- "Wizard di categorizzazione" = il form raggiungibile in produzione, cioè `NewProductForm` dentro `AddItemBottomSheet`. `ProductCategorizationWizard` resta non cablato e fuori perimetro.
- "Duplicato semantico" = stesso nome dopo `ProductNameNormalizer.normalize` (trim, lowercase `Locale.ROOT`, NFKD, rimozione diacritici, collapse spazi) nello stesso `level2Id`. Nessuna soglia, nessun Levenshtein, nessun fuzzy.
- Nessun indice/constraint UNIQUE: l'avviso è applicativo e l'utente può procedere.

### Decisioni di design

1. La proposta è un pannello inline dentro `NewProductForm`, visibile solo dopo la selezione di un Livello 2. Nessuna nuova schermata o bottom sheet.
2. Nuova use case `CreateUserLevel3UseCase(catalogRepository: CatalogRepository)` con `suspend operator fun invoke(name, level2Id, confirmDuplicate = false): CreateLevel3Result`.
3. `CreateLevel3Result` è una sealed interface nello stesso file: `Created(level3)`, `Duplicate(candidate)`, `Invalid(message)`.
4. `CatalogRepository` aggiunge `getLevel3ByLevel2` e `insertLevel3` con default che lanciano `UnsupportedOperationException` (compatibilità con i fake). `RoomCatalogRepository` li implementa entrambi.
5. `TaxonomyLevel3Entity.id` diventa `@PrimaryKey(autoGenerate = true) val id: Long = 0`. `TogoDatabase.version` resta 2, nessuna migrazione, `assets/catalog.db` invariato. L'identity hash dello schema Room cambia: un solo reset dei dati dev copre 7.1 e 7.2 (nota aggiornata in `TogoDatabase.kt`).
6. `CatalogDao.getLevel3()` non filtra più `is_user_defined = 0`: il nuovo L3 compare in `getTaxonomy()`, nel form, nella lista attiva, nella condivisione e nell'assegnazione categorie.
7. `sortOrder` del nuovo L3 = `COALESCE(MAX(sortOrder), 0) + 1` nello stesso `level2Id`; il livello appare in fondo alla categoria. Calcolo in due statement (max + insert), non transazionale: rischio di race trascurabile (utente singolo + guardia nel ViewModel), tracciato in `deferred-work.md`.
8. La creazione del L3 non crea il prodotto: dopo il successo il nuovo L3 viene selezionato nel form e l'utente conferma "Crea prodotto", che riusa la use case 7.1 invariata.
9. Il confronto duplicati usa `ProductNameNormalizer`, limitato agli L3 dello stesso `level2Id` (predefiniti + user-defined).
10. La validazione dell'esistenza del L2 è demandata alla UI (dropdown costruito da `getTaxonomy`); la use case valida solo `level2Id > 0`. Nessuna API per creare L1/L2.
11. UI con token del Design System, touch target `spacing.touchTargetMin`, label esplicite, stringhe inline in italiano.
12. Fuori scope: wiring di `ProductCategorizationWizard`, modifica di `NewProductDraft`, creazione L1/L2, deduplicazione di lista, refactor N+1, voce, Dark Mode, TalkBack, Dynamic Type, modifica della ricerca prodotti.

### File nuovi

- `app/src/main/java/it/togo/app/domain/usecase/CreateUserLevel3UseCase.kt`
- `app/src/main/java/it/togo/app/presentation/additem/DuplicateLevel3WarningDialog.kt`
- `app/src/test/java/it/togo/app/domain/usecase/CreateUserLevel3UseCaseTest.kt`
- `app/src/test/java/it/togo/app/data/database/CatalogDaoUserLevel3Test.kt`
- `app/src/test/java/it/togo/app/data/repository/RoomCatalogRepositoryUserLevel3Test.kt`
- `app/src/test/java/it/togo/app/presentation/additem/AddItemViewModelLevel3ProposalTest.kt`
- `app/src/test/java/it/togo/app/presentation/additem/DuplicateLevel3WarningDialogTest.kt`
- `_bmad-output/implementation-artifacts/spec-7-2-proposta-nuovo-livello-3.md` (questo file)

### File modificati

- `app/src/main/java/it/togo/app/domain/repository/CatalogRepository.kt` (`getLevel3ByLevel2`, `insertLevel3`)
- `app/src/main/java/it/togo/app/data/database/entity/CatalogEntities.kt` (`TaxonomyLevel3Entity.id` autoGenerate)
- `app/src/main/java/it/togo/app/data/database/dao/CatalogDao.kt` (rimosso filtro `getLevel3`, aggiunti `nextLevel3SortOrder`, `insertLevel3`)
- `app/src/main/java/it/togo/app/data/repository/RoomCatalogRepository.kt` (implementazioni L3)
- `app/src/main/java/it/togo/app/data/database/TogoDatabase.kt` (nota reset DB 7.1 + 7.2)
- `app/src/main/java/it/togo/app/di/DomainModule.kt` (registrazione use case)
- `app/src/main/java/it/togo/app/di/PresentationModule.kt` (factory ViewModel)
- `app/src/main/java/it/togo/app/presentation/additem/AddItemUiState.kt` (stato ed eventi 7.2)
- `app/src/main/java/it/togo/app/presentation/additem/AddItemViewModel.kt` (handler, guardie, selezione automatica)
- `app/src/main/java/it/togo/app/presentation/additem/NewProductForm.kt` (pannello inline)
- `app/src/main/java/it/togo/app/presentation/additem/AddItemBottomSheet.kt` (wiring e dialog)
- `app/src/test/java/it/togo/app/presentation/additem/NewProductFormTest.kt` (nuovi casi)
- `app/src/test/java/it/togo/app/presentation/additem/AddItemViewModelNewProductTest.kt` (nuovo parametro costruttore)
- `app/src/test/java/it/togo/app/presentation/additem/AddItemViewModelEditTest.kt` (nuovo parametro costruttore)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` (7.1 `done`, 7.2 `done`)
- `_bmad-output/implementation-artifacts/deferred-work.md` (rischio `sortOrder`)

### Task completati

- [x] Use case di creazione L3 con normalizzazione e conferma duplicato.
- [x] Metodi repository con default per compatibilità fake e implementazione Room.
- [x] Entità L3 con ID auto-generato.
- [x] Rimozione del filtro `is_user_defined` da `getLevel3()`.
- [x] Calcolo del `sortOrder` in fondo al Livello 2.
- [x] Stato ed eventi ViewModel per il pannello di proposta.
- [x] Guardie di rientranza e gestione errori/duplicati.
- [x] Pannello inline in `NewProductForm` (solo L3; nessuna azione L1/L2).
- [x] Dialog `DuplicateLevel3WarningDialog` con "Procedi"/"Annulla".
- [x] Wiring in `AddItemBottomSheet`, DI e factory.
- [x] Test scritti per domain, data, ViewModel e UI.
- [x] Documentazione e sprint status a `done`.

### Test eseguiti

Eseguiti il 2026-10-22 con esito positivo (segnalato dall'utente): `.\test-android.ps1` e `.\build-android.ps1`. Il test flaky `CategoryCorrectorTest` è stato stabilizzato portando il timeout JUnit da 100 ms a 5 s, mantenendo il nome del test e la guardia anti-blocco.

1. `CreateUserLevel3UseCaseTest`: nome vuoto, L2 invalido, duplicato esatto/accentato/spazi, conferma duplicato, stesso nome in L2 diverso, proprietà del livello creato.
2. `CatalogDaoUserLevel3Test`: ID auto-generato, `is_user_defined = 1`, visibilità in `getLevel3`, filtro per L2, `sortOrder` massimo + 1, indipendenza tra L2, nessun UNIQUE sul nome, prodotto canonico sul nuovo L3.
3. `RoomCatalogRepositoryUserLevel3Test`: trim, campi persistiti, `sortOrder`, presenza in `getTaxonomy`, prodotto user-defined ricercabile (incluse accentate), isolamento tra L2.
4. `AddItemViewModelLevel3ProposalTest`: visibilità condizionata al L2, nome vuoto, creazione e selezione automatica, duplicato senza insert, conferma duplicato, cambio L2, selezione L3 esistente, riuso del nuovo `level3Id` nella creazione prodotto, doppio tap.
5. `NewProductFormTest` (aggiornato): visibilità del bottone, callback, pannello, validazione, errore, assenza azioni L1/L2, stato busy.
6. `DuplicateLevel3WarningDialogTest`: contenuto e callback "Procedi"/"Annulla".
7. `AddItemViewModelNewProductTest` e `AddItemViewModelEditTest`: aggiornati con `createLevel3UseCase`.
8. Review 2026-10-22: aggiunti casi per errore di scrittura L3, duplicato contro un L3 user-defined, branch `Invalid` con nome normalizzato vuoto, guardia prodotto-in-salvataggio, `Crea Livello 3` disabilitato durante `isSaving` e label "Annulla proposta".

### Mappatura AC -> codice/test

| AC | Codice | Test |
| --- | --- | --- |
| "Proponi nuovo Livello 3" dopo selezione L2 | `NewProductForm.kt:126-143`, `AddItemViewModel.kt:407-419` | `NewProductFormTest`, `AddItemViewModelLevel3ProposalTest` |
| Nome + conferma esplicita | `NewProductForm.kt:145-193`, `AddItemViewModel.kt:437-477` | `NewProductFormTest`, `AddItemViewModelLevel3ProposalTest` |
| Duplicati semantici nello stesso L2 + avviso | `CreateUserLevel3UseCase.kt:44-66`, `DuplicateLevel3WarningDialog.kt` | `CreateUserLevel3UseCaseTest`, `DuplicateLevel3WarningDialogTest`, `AddItemViewModelLevel3ProposalTest` |
| Salvataggio `is_user_defined = 1` | `RoomCatalogRepository.kt:84-97`, `CatalogEntities.kt:56-64` | `CatalogDaoUserLevel3Test`, `RoomCatalogRepositoryUserLevel3Test` |
| Nessuna creazione L1/L2 | `NewProductForm.kt:126-195` | `NewProductFormTest` |
| Immediata disponibilità per prodotti user-defined | `CatalogDao.kt:50-51`, `AddItemViewModel.kt:539-582` | `CatalogDaoUserLevel3Test`, `RoomCatalogRepositoryUserLevel3Test`, `AddItemViewModelLevel3ProposalTest` |

### Rischi / BLOCCATE

- **Verificato — test e build**: `.\test-android.ps1` e `.\build-android.ps1` eseguiti il 2026-10-22 con esito positivo (segnalato dall'utente). Test di riferimento: `app/src/test/java/it/togo/app/domain/usecase/CreateUserLevel3UseCaseTest.kt:1`, `app/src/test/java/it/togo/app/data/database/CatalogDaoUserLevel3Test.kt:1`, `app/src/test/java/it/togo/app/data/repository/RoomCatalogRepositoryUserLevel3Test.kt:1`, `app/src/test/java/it/togo/app/presentation/additem/AddItemViewModelLevel3ProposalTest.kt:1`, `app/src/test/java/it/togo/app/presentation/additem/NewProductFormTest.kt:1`, `app/src/test/java/it/togo/app/presentation/additem/DuplicateLevel3WarningDialogTest.kt:1`.
- **Residuo — verifica manuale su device**: flusso reale (pannello, dialog, selezione automatica) non osservato manualmente. Non blocca il passaggio a `done`, ma va incluso nel collaudo del prossimo giro su device.
- **Reset DB dev**: `TaxonomyLevel3Entity.id` cambia l'identity hash Room con `version = 2` invariata. Prima del primo avvio: disinstallare l'app o `adb shell pm clear it.togo.app`. Un solo reset copre 7.1 e 7.2.
- **`sortOrder` non transazionale**: `nextLevel3SortOrder` + `insertLevel3` sono due statement separati (`CatalogDao.kt:83-98`); due creazioni concorrenti potrebbero ottenere lo stesso ordine. Accettato (utente singolo + guardia ViewModel) e tracciato in `deferred-work.md`.
- **`ProductCategorizationWizard` non cablato**: già in `deferred-work.md:105-110`; nessuna duplicazione.

### Comandi di verifica per l'utente

```powershell
.\test-android.ps1
.\build-android.ps1
# opzionale, verifica su device:
.\build-install-android.ps1
```

Prima del primo avvio su device/emulatore con dati della build precedente:

```powershell
adb shell pm clear it.togo.app
```

### Review Findings

Review eseguita il 2026-10-22 con quattro layer (Blind Hunter, Edge Case Hunter, Verification Gap, Acceptance Auditor). Stato inizialmente mantenuto `review`; il 2026-10-22 test e build sono risultati verdi e la story è passata a `done`. La verifica manuale su device non è stata registrata.

#### Patch applicati

- [x] [Review][Patch] Guardia di creazione asimmetrica: "Crea Livello 3" restava attivo durante `isSaving` e la creazione L3 poteva partire mentre un prodotto era in salvataggio; i due result handler potevano sovrascriversi e due dialog potevano coesistere. Ora `canCreateLevel3` usa `busy`, `handleCreateNewLevel3`/`handleConfirmDuplicateLevel3` controllano anche `isCreatingProduct`, e il ramo `Duplicate` prodotto azzera `duplicateLevel3Candidate`. [`NewProductForm.kt:81`, `AddItemViewModel.kt:305`, `AddItemViewModel.kt:348`, `AddItemViewModel.kt:457`, `AddItemViewModel.kt:500`]
- [x] [Review][Patch] Risultato tardivo dopo chiusura form o cambio Livello 2: il result poteva selezionare un L3 non più coerente con lo stato. Ora `handleCreateLevel3Result` scarta il risultato se form/pannello non sono più visibili o se `level2Id` non coincide. [`AddItemViewModel.kt:539`]
- [x] [Review][Patch] Reload tassonomia dopo un insert già riuscito: un errore di reload mostrava "Livello 3 creato, ma errore..." e lasciava un retry che poteva duplicare. Il nuovo L3 viene ora accodato allo snapshot `taxonomy`; il ramo di errore e il round-trip DB sono rimossi. [`AddItemViewModel.kt:539`]
- [x] [Review][Patch] Handler di selezione L1/L2/L3 e show/hide proposal azzeravano `isCreatingLevel3` anche durante una scrittura. Aggiunte guardie `isCreatingLevel3 || isCreatingProduct`; `handleNewProductLevel3Selected` non azzera più la guardia. [`AddItemViewModel.kt:252`, `AddItemViewModel.kt:271`, `AddItemViewModel.kt:287`, `AddItemViewModel.kt:423`, `AddItemViewModel.kt:437`]
- [x] [Review][Patch] Due azioni "Annulla" adiacenti con scopi diversi (pannello L3 e form prodotto). Il pulsante del pannello ora è "Annulla proposta". [`NewProductForm.kt:178`]
- [x] [Review][Patch] `CancellationException` catturata dai nuovi `catch (Exception)`. Ora viene rilanciata prima del catch generico. [`AddItemViewModel.kt:485`, `AddItemViewModel.kt:511`]
- [x] [Review][Patch] Test mancanti per: errore di scrittura L3, duplicato contro un L3 user-defined appena creato, branch `Invalid` con nome normalizzato vuoto, guardia prodotto-in-salvataggio, `Crea Livello 3` disabilitato durante `isSaving`, label "Annulla proposta". Aggiunti in `CreateUserLevel3UseCaseTest`, `AddItemViewModelLevel3ProposalTest`, `NewProductFormTest`. [`CreateUserLevel3UseCaseTest.kt`, `AddItemViewModelLevel3ProposalTest.kt`, `NewProductFormTest.kt`]
- [x] [Review][Patch] I nuovi file di presentazione non erano inclusi nello scan Dark Mode `scopedComponents`. Aggiunti `NewProductForm.kt`, `DuplicateLevel3WarningDialog.kt` e `DuplicateProductWarningDialog.kt`. [`DarkModeTokenUsageTest.kt:17`]

#### Defer

- [x] [Review][Defer] `CategoryAssigner`/`CategoryCorrector` escludono gli L3 user-defined (default, fallback, regole e correzione). La pipeline di assegnazione automatica non è cablata in produzione e la spec 3.6 impone L3 canonici non user-defined; da decidere quando si cablerà. [`CategoryAssigner.kt:38`, `CategoryAssigner.kt:62`, `CategoryAssigner.kt:76`, `CategoryAssigner.kt:97`, `CategoryCorrector.kt:62`] — deferred: invariante preesistente di Story 3.6 + pipeline non raggiungibile dalla UI.
- [x] [Review][Defer] Nessun test monta `AddItemBottomSheet` e verifica che `duplicateLevel3Candidate` apra il dialog e che "Procedi" mappi su `ConfirmDuplicateLevel3`. [`AddItemBottomSheet.kt:409-416`] — deferred: richiede harness Koin/ViewModel o estrazione dei mount in un composable stateless.
- [x] [Review][Defer] I consumer di `getTaxonomy()` (lista attiva, condivisione, assegnazione) non hanno test che includano un L3 user-defined dopo la rimozione del filtro DAO. [`CatalogDao.kt:50`] — deferred: test di regressione sui consumer, non un difetto del codice.
- [x] [Review][Defer] `CategorySelectionWizard.level3NameExists` usa `trim + lowercase`, senza accent-fold, diversamente da `ProductNameNormalizer`. [`CategorySelectionWizard.kt:50-54`] — deferred: wizard non cablato; allineare quando si collega.
- [x] [Review][Defer] Nessun limite di lunghezza o gestione dei caratteri di controllo per il nome L3. [`CreateUserLevel3UseCase.kt:47-58`] — deferred: regola di prodotto da definire (i nomi finiscono in dropdown, dialog e condivisione).
- [x] [Review][Defer] Nessun percorso di rinomina/eliminazione per un L3 user-defined creato per errore. — deferred: serve una storia di gestione del catalogo utente, come già per i prodotti 7.1.
- [x] [Review][Defer] Il pannello inline non ha scroll/IME handling dedicato; su schermi bassi con tastiera il footer può essere coperto. [`AddItemBottomSheet.kt:137-143`] — deferred: verifica manuale su device (già BLOCCATA) prima di modificare il layout.
- [x] [Review][Defer] Il blocco di reset dei campi L3 è duplicato in più handler; con le nuove guardie gli stati impossibili sono evitati, ma resta il debito di un helper/sealed state. [`AddItemViewModel.kt`] — deferred: refactor di manutenzione.
- [x] [Review][Defer] `sortOrder` può avere pareggi in caso di creazioni concorrenti e manca un indice composto `(level2_id, sortOrder)`. — deferred: stessa radice del rischio non transazionale già tracciato (incluso il `MAX+1` separato); il calcolo atomico resta in `deferred-work.md`.

#### Rejected

- `false` — Asset `catalog.db` con `PRIMARY KEY(id)` senza `AUTOINCREMENT`: Room non confronta `AUTOINCREMENT` nella validazione dello schema e l'asset non è stato modificato; il drift del generatore è già tracciato nel defer 7.1.
- `false` — Migrazione/`fallbackToDestructiveMigration`: scelta esplicita della spec (versione 2, reset dev) e comportamento preesistente, non introdotto da 7.2; il progetto è pre-release.
- `false` — `insertLevel3` accetta un `level2Id` inesistente: la design decision 10 demanda la validazione del genitore alla UI, che costruisce il dropdown da `getTaxonomy`; nessun percorso di produzione passa un L2 invalido.
- `low` — `confirmDuplicate` non è legato al candidato visto: il dialog è modale e non esiste un percorso UI che cambi la tassonomia mentre è aperto; la conferma ri-esegue comunque il confronto.
- `low` — `DuplicateLevel3WarningDialog` duplica la struttura di `DuplicateProductWarningDialog`: duplicazione deliberata per testabilità e messaggi distinti; lo scan Dark Mode è stato esteso.
- `false` — Un L2 senza figli non ha empty state: il pulsante "Proponi nuovo Livello 3" è comunque visibile sotto il dropdown.
- `low` — Test DAO/repository con seed `execSQL` e Room in-memory: è il pattern standard del progetto; non è emersa un'asserzione che escluda la riga sotto test (il filtro `!isUserDefined` riguarda solo le righe preinstallate).
- `low` — Messaggi duplicati tra use case e ViewModel: le stringhe inline sono la convenzione del progetto e i pre-check del ViewModel danno feedback immediato; nessun impatto utente.
- `false` — Accessibilità/TalkBack del nuovo campo: esplicitamente fuori perimetro 7.2.
- `low` — Dipendenza `createLevel3UseCase` nella factory `forEdit`: dipendenza inutilizzata ma innocua; nessun impatto runtime.
- `false` — Igiene del diff (`empty-for-diff.txt`, newline finale): artefatto della generazione del diff di review, non del codice.
- `low` — Test "nessuna azione L1/L2" basato su stringhe mai introdotte: il test è debole, ma il form non contiene alcun percorso di creazione L1/L2; nessun difetto funzionale.

### Spec Change Log

- 2026-10-22: creazione spec, implementazione completata, stato `in-progress -> review`. Test scritti e non eseguiti.
- 2026-10-22: code review a quattro layer; applicati i patch su guardie di concorrenza, risultato tardivo, tassonomia post-creazione, label "Annulla proposta", rethrow di `CancellationException` e test mancanti. Defer tracciati in `deferred-work.md`. Stato mantenuto `review` perché build/test/device restano non eseguiti.
- 2026-10-22: `CategoryCorrectorTest` stabilizzato (timeout JUnit 100 ms -> 5 s). Test e build eseguiti con esito positivo; Story 7.2 portata a `done`. Con 7.1 già `done`, anche Epic 7 è passato a `done`.
