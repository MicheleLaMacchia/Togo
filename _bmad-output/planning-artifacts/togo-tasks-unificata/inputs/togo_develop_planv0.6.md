# Togo — Piano di sviluppo agentico

**Versione:** 0.6  
**Ultimo aggiornamento:** 28 ottobre 2026  
**Specifica di riferimento:** `togo-specifichev0.14.md`  
**Documenti requisiti collegati:** `togo-lista-unificata-tasks-v1.0.md`, `togo_catalog_review_v2.md`, `togo_catalogo_normalizzato_proposta_v2.md`, `togo_catalog_candidati_v2.md`, `togo_catalog_migration_mapping_v2.csv`  
**Destinazione del documento:** esecuzione progressiva tramite Codex/ChatGPT Work o altro agente di coding con accesso al repository.

## Novità di questa revisione

La v0.6 recepisce la specifica v0.14 e introduce due milestone addizionali **catalogo** e **lista unificata Spesa + Da fare**, mantenendo la numerazione e il contenuto delle precedenti Task 0–79 come baseline storica. AI rimane provider-agnostic e **post-core**. Questo piano è adatto a un agente **solo per iterazioni piccole**, dopo lettura dello stato reale del repository.

Novità v0.6:

- Richiesta di un unico feed Spesa + Da fare, con due domini separati, Task status `OPEN/COMPLETED/DELETED` e categorie Task indipendenti da L1/L2/L3 prodotti.
- La catalogazione seed usa prodotti **senza suffissi di quantità/confezione**; il mapping suggerisce 1.470 record → 748 canonici, con 722 riferimenti da riconciliare. Mapping = **dry run** e non migrazione già eseguita.
- Nuove Task **80–100**, senza rinumerare le precedenti, per audit/normalizzazione catalogo, Task Room, UI unificata e regressioni.
- La Fase 5 precedente resta una **milestone shopping-only intermedia**; la v1 core aggiornata richiede anche le nuove Fasi 11 e 12.
- Comandi checkpoint e Git status obbligatori; non eseguire migrazioni distruttive o commit automatici.


Modifiche principali:

- sostituita la fase Gemini-only con una **AI provider-agnostic** basata su `AiGateway`, `AiProviderAdapter` e `AiProviderRegistry`;
- rollout obbligatoriamente incrementale: contratto/fake → Gemini → dogfood → OpenAI → Anthropic;
- scelto per `PERSONAL_BYOK` un unico stack HTTP Android controllato (preferenza: OkHttp + `kotlinx.serialization`) invece di tre SDK vendor, salvo motivazione tecnica documentata;
- credenziali namespaced per provider; configurazione provider/modello separata dai segreti;
- model discovery e capability filtering restano provider-specifici; vietato assumere che `GET /models` esponga le stesse informazioni per tutti i vendor;
- structured output normalizzato nel dominio ma implementato con il meccanismo nativo di ciascun provider;
- aggiunti contract test identici per tutti gli adapter e test di isolamento credenziali;
- Google OAuth per Gemini marcato `OUT_OF_SCOPE_V1`: nessun login Google viene implementato come scorciatoia per un abbonamento consumer Gemini;
- `PUBLIC_SAFE` rinominato `PUBLIC_MANAGED`: backend/gateway Togo per multi-provider; Firebase AI Logic rimane alternativa specifica se una futura release sceglie Gemini-only;
- aggiunta `unitComparisonKey` per alias noti delle unità e limitato il riquadro PURCHASED della main screen;
- aggiunti guardrail specifici per agenti: nessuna chiave reale nei test/fixture, nessuna live API call in CI, nessun logging interceptor che esponga Authorization/body sensibili;
- rinumerati i task successivi mantenendo sequenza continua.

# 0. Contratto di esecuzione per l’agente

Questa sezione è **vincolante**.

## 0.1 Fonte di verità

1. La fonte di verità funzionale è `togo-specifichev0.14.md`. Il dettaglio Tasks è `togo-lista-unificata-tasks-v1.0.md`; gli allegati catalogo v2 sono proposte da confrontare con `catalog.db` reale, mai autorità sopra il DB.
2. Questo piano traduce la specifica in attività implementative.
3. Se piano e specifica sembrano confliggere, prevale la specifica.
4. Se la documentazione ufficiale Android/Google e dei provider AI corrente contraddice una scelta tecnica esterna, **non inventare un workaround**: documentare il conflitto e applicare il gate previsto.

## 0.2 Regole di scope

Non aggiungere senza richiesta esplicita:

- backend Togo / `PUBLIC_MANAGED`;
- login/account;
- cloud sync;
- multi-lista; **Spesa e Da fare nella stessa lista non sono multi-lista**;
- widget;
- analytics;
- crash reporting remoto;
- framework di navigazione/architettura aggiuntivi oltre a quelli necessari;
- multi-module Gradle;
- modelli AI on-device;
- Google OAuth per Gemini nella v1;
- provider custom/OpenAI-compatible arbitrari;
- LLM gateway esterni nel percorso personale;
- fuzzy matching generico/Levenshtein;
- workaround App Actions non documentati.

La v1 parte come **singolo modulo `app`**. Estrarre moduli solo quando esiste una ragione misurabile.

## 0.3 Regole di qualità per ogni task

Per ogni task:

1. leggere i riferimenti alla specifica indicati;
2. implementare solo quanto necessario;
3. aggiungere/aggiornare i test del comportamento introdotto nello stesso task o immediatamente dopo;
4. eseguire almeno i test interessati;
5. non lasciare `TODO`, stub o branch irraggiungibili salvo dove il task lo autorizza esplicitamente;
6. non silenziare warning/errori significativi con catch generici;
7. non loggare segreti né contenuti sensibili non necessari;
8. aggiornare `docs/implementation-status.md` con task completato, test eseguiti ed eventuali deviazioni motivate.

## 0.4 Checkpoint obbligatori

Al termine di ogni fase eseguire, adattando i task Gradle ai moduli effettivi:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Quando esistono test instrumented/device disponibili, eseguire anche quelli rilevanti. Un agente non deve dichiarare completata una fase con build rotta o test rossi.

## 0.5 Strategia Git consigliata

Se il repository è sotto Git e l’ambiente consente commit:

- un commit logico per task o piccolo gruppo coeso;
- messaggi descrittivi (`feat:`, `fix:`, `test:`, `chore:`);
- non riscrivere cronologia esistente;
- non committare chiavi, file locali IDE o build output.

---

# 1. Convenzioni architetturali

Queste scelte sono vincolanti per evitare incoerenza durante l’esecuzione agentica.

## 1.1 Stack

- Kotlin.
- Gradle Kotlin DSL.
- Jetpack Compose + Material 3.
- Hilt per dependency injection.
- Room per DB.
- Kotlin Coroutines + Flow.
- MVVM con **un singolo `StateFlow<UiState>` per ViewModel**.
- `collectAsStateWithLifecycle()` in Compose.
- DataStore Preferences per preferenze non sensibili.
- `kotlinx.serialization` dove serve serializzazione JSON applicativa.
- Android Keystore per protezione dei segreti personali.
- Per `PERSONAL_BYOK`, quando si arriva alla fase AI, preferire un solo client HTTP Android condiviso (OkHttp) + `kotlinx.serialization` rispetto a SDK vendor multipli, salvo incompatibilità motivata e documentata.

## 1.2 SDK baseline

- `minSdk = 26`.
- `targetSdk = 36`.
- `compileSdk`: usare SDK stabile compatibile; non rendere una preview requisito del progetto.
- Verificare durante l’implementazione se policy/dependency correnti richiedono un valore più alto; se sì, aggiornare documentazione e testare i behavior change prima del cambio.

## 1.3 Package indicativi

Package base iniziale: `com.togo.app` salvo package definitivo già scelto nel repository.

Struttura minima:

```text
com.togo.app
├── data
│   ├── db
│   ├── repository
│   └── preferences
├── domain
│   ├── model
│   ├── category
│   ├── parser
│   └── usecase
├── ai
├── voice
├── security
└── ui
```

Non creare layer/cartelle vuote solo per aderire allo schema.

## 1.4 Interfacce di stabilità

Definire presto interfacce piccole:

- `TogoRepository` o use case transazionali equivalenti per Spesa;
- `TodoRepository` e `UnifiedListRepository` quando si arriva alla Fase 12;
- `CategoryResolver`;
- `OfflineVoiceParser`;
- `AiGateway`;
- `AiProviderAdapter` e `AiProviderRegistry`;
- `AiSettingsRepository`;
- `SecretStore` namespaced per provider;
- `Clock`.

Evita che ViewModel o Composable dipendano direttamente da DAO, SDK/provider AI, Keystore o Intent Android.

---

# Fase 0 — Bootstrap e guardrail

## Task 0 — Creare/verificare il progetto

Creare o adeguare progetto Android Studio:

- Kotlin;
- Gradle Kotlin DSL;
- `minSdk 26`;
- `targetSdk 36`;
- Compose Material 3;
- package coerente;
- Java/Kotlin toolchain compatibile con AGP scelto.

**DoD**

- `assembleDebug` verde.
- App si avvia su emulator/device supportato.

## Task 1 — Dependency management

Aggiungere solo dipendenze necessarie al core:

- Compose BOM/components;
- Lifecycle/ViewModel Compose;
- Hilt + Compose integration;
- Room runtime/ktx + compiler KSP;
- Coroutines;
- DataStore Preferences;
- `kotlinx.serialization` se usato per asset/export;
- test stack: JUnit, coroutines-test, Room test, AndroidX test, Compose UI test.

**Non aggiungere ancora SDK Gemini/Firebase/App Actions helper opzionali.**

Usare Version Catalog se non introduce migrazione inutile.

## Task 2 — CI locale minima e stato implementazione

Creare:

- `docs/implementation-status.md`;
- eventuale script/istruzioni per i comandi di verifica;
- `.gitignore` corretto per build output, local.properties, segreti.

Il file di stato deve contenere per ogni fase: `TODO / IN_PROGRESS / DONE / BLOCKED`, test effettuati e note.

---

# Fase 1 — Modello dati Room

Riferimenti: specifica §2, §3.1, §3.11.

## Task 3 — Enum e modelli persistenti

Creare:

```text
CategorySource = MANUAL | INFERRED | LEARNED | AI
ListItemStatus = TO_BUY | PURCHASED | DELETED
```

Creare `ProductEntity` con:

- `id: Long` autogenerato;
- `name: String`;
- `normalizedName: String`;
- `category: String`;
- `categorySource: CategorySource`;
- `defaultUnit: String?`.

Vincolo DB: indice **UNIQUE** su `normalizedName`.

## Task 4 — `ListItemEntity`

Campi:

- `id: Long`;
- `productId: Long` FK;
- `quantity: Double`;
- `unit: String?`;
- `note: String?`;
- `status: ListItemStatus`;
- `insertedAt: Long`;
- `statusChangedAt: Long`, inizialmente uguale a `insertedAt`.

Aggiungere indici utili per:

- `productId`;
- query su `status`;
- combinazione `productId/status` se giustificata dal query plan.

FK senza cascade delete del Product.

## Task 5 — DAO Product

Metodi minimi, con firme idiomatiche `suspend`/`Flow` dove appropriate:

- insert con strategia che consenta gestione esplicita del conflitto UNIQUE;
- find by exact `normalizedName`;
- query candidati per matching approssimato, se necessaria;
- update category/source;
- update default unit;
- distinct categories;
- export rows con `normalizedName`, `category`, `categorySource`.

Non restituire entity mutable direttamente alla UI.

## Task 6 — DAO ListItem

Metodi minimi:

- insert;
- get by id;
- find active rows by `productId`;
- update quantity/unit/note se necessario;
- update status/time;
- bulk `TO_BUY → DELETED`;
- delete physical rows only by `DELETED`;
- history queries;
- observable main-list projection con join Product/ListItem;
- `countPurchasedByProduct`;
- `lastPurchaseDateByProduct`;
- suggestion query ordinabile;
- distinct historical units non null/non blank.

Non chiamare una query `findAllGroupedByCategory` se restituisce in realtà righe flat: usare un nome che descriva il dato reale e raggruppare nel dominio/UI.

## Task 7 — Database e schema export

Creare `TogoDatabase`.

Durante bootstrap iniziale è ammesso destructive migration **solo fino al Task 12 / prima dell’uso personale reale**. Abilitare schema export Room per poter testare migrazioni future.

**DoD Fase 1**

- test CRUD DAO;
- test UNIQUE `normalizedName`;
- query statistiche corrette;
- build/test/lint verdi.

---

# Fase 2 — Normalizzazione, validazione e transazioni di dominio

Riferimenti: specifica §2.3, §2.4, §3.1, §3.8, §3.9.

## Task 8 — `Clock`

Definire interfaccia semplice:

```kotlin
interface Clock { fun nowEpochMillis(): Long }
```

Implementazione production e fake per test.

## Task 9 — Normalizzazione nome

Implementare funzione pura:

- trim;
- lowercase stabile;
- rimozione diacritici;
- collasso whitespace;
- validazione non blank.

Aggiungere test con:

- accenti;
- maiuscole;
- spazi ripetuti;
- solo whitespace;
- caratteri apostrofo/segni non distruttivi.

## Task 10 — `comparisonKey`

Implementare euristica conservativa per token:

- solo token alfabetici `length >= 4`;
- rimuovere al massimo una vocale finale `a/e/i/o`;
- non persistire la chiave;
- non usare Levenshtein.

Matching approssimato valido solo con **un candidato univoco**.

Test minimi:

- `mela` / `mele` compatibili;
- parole corte non alterate;
- ambiguità → nessun merge;
- `riso` / `rosa` non forzati insieme.

## Task 11 — Normalizzazione unità/note

Funzioni pure:

- trim;
- blank → null;
- `unitComparisonKey`: lowercase + canonicalizzazione dei soli alias noti condivisi con il dizionario unità (`litro/litri/l` → `l`, `chilogrammo/kg` → `kg`, ecc.); per valori custom usare il testo normalizzato senza fuzzy matching;
- confronto note su valore normalizzato, senza semantic/fuzzy matching.

Il valore `unit` persistito resta quello confermato; la key serve solo al confronto. Implementare `areMergeCompatible(existing, incoming)` secondo specifica §2.4.

## Task 12 — Validazione `AddItemCommand`

Creare comando tipizzato e validatore:

- nome non blank dopo normalizzazione;
- quantity finita e `> 0`;
- limiti ragionevoli di lunghezza stringhe (definire costanti documentate; es. nome 200, unit 50, note 1000, categoria 100) per evitare payload patologici;
- unit/note blank → null.

Non troncare silenziosamente input utente normale: mostrare errore quando supera i limiti.

## Task 13 — `addItem` transazionale

Implementare come use case/repository transaction.

Algoritmo:

1. valida input;
2. cerca Product exact;
3. se assente, prova singolo match approssimato sicuro;
4. se ancora assente, risolve categoria e tenta insert Product;
5. se insert collide su UNIQUE perché un’altra coroutine/process request ha creato il Product, rilegge il record esistente e continua;
6. per Product esistente, **non** sovrascrive categoria da AI/hint salvo use case manuale esplicito;
7. cerca tutte le righe TO_BUY del Product;
8. trova una compatibile unit/note;
9. se trovata, incrementa quantità in modo atomico nella stessa transaction;
10. altrimenti inserisce nuova riga;
11. aggiorna `defaultUnit` se unit non null.

Non usare `Pair<String, CategorySource>` come dominio pubblico se riduce leggibilità: preferire un data class `CategoryHint(value, source)`.

## Task 14 — Stati base

Implementare transazionalmente:

- `markAsPurchased(id)` solo da TO_BUY;
- `deleteItem(id)` per TO_BUY → DELETED;
- transizioni invalide gestite in modo deterministico (no-op tipizzato o errore dominio, scegliere e testare).

## Task 15 — Ripristino DELETED

`restoreDeleted(id)`:

- accetta solo riga DELETED;
- trova eventuale TO_BUY compatibile stesso Product;
- se assente: riga DELETED → TO_BUY e update time;
- se presente: somma quantità all’attiva e lascia la riga storica DELETED; aggiornare la quantità attiva senza falsare `statusChangedAt` se lo stato non cambia.

## Task 16 — “Aggiungi di nuovo” da PURCHASED

`reAddPurchased(id)`:

- legge la riga PURCHASED;
- **non modifica la riga PURCHASED**;
- crea un comando nuovo con Product, quantity/unit/note della riga o una quantità definita dalla UI (default: la quantità storica, salvo decisione UX esplicita);
- passa alla stessa logica di add/merge TO_BUY.

Testare che `countPurchasedByProduct` resti invariato dopo la riaggiunta.

## Task 17 — Bulk clear e purge

- `clearActiveList()` in una transaction: tutte TO_BUY → DELETED con timestamp coerente.
- `purgeDeletedHistory()`: DELETE fisico solo DELETED.
- non cancellare Products automaticamente.

**DoD Fase 2**

Test obbligatori:

- due add concorrenti dello stesso nome → un Product;
- merge stessa unit/note;
- no merge unit diverse;
- no merge note diverse;
- PURCHASED re-add conserva storico;
- DELETED restore con e senza riga compatibile;
- bulk clear;
- purge non tocca PURCHASED/Product.

Da questo checkpoint, se l’app inizia a essere usata con dati reali, **rimuovere destructive migration** e introdurre migrazioni esplicite.

---

# Fase 3 — Categorizzazione deterministica

Riferimenti: specifica §3.6, §3.7.

## Task 18 — Asset `category_map.json`

Creare `app/src/main/assets/category_map.json` con set iniziale italiano controllato.

Formato:

```json
{
  "zucchero": "Dispensa",
  "latte": "Latticini",
  "mela": "Frutta e verdura",
  "detersivo per piatti": "Pulizia casa"
}
```

Non generare centinaia di mapping casuali con un LLM senza revisione. Meglio un set iniziale piccolo ma verificato.

## Task 19 — Loader/validator mappa

Caricare una volta la mappa e validare:

- chiavi normalizzate;
- nessuna chiave vuota;
- categorie non blank;
- categorie canonicalizzate;
- errori asset falliscono chiaramente nei test/debug, non silenziosamente.

## Task 20 — Known categories provider

Costruire l’insieme canonico da:

- valori mappa statica;
- categorie distinte Product;
- `Altro` sempre presente.

Dedup case-insensitive mantenendo una forma display canonica.

## Task 21 — Manual learning resolver

Solo Product con source `MANUAL` sono seed.

Regola:

- prefix su confine token;
- scegliere il match più lungo;
- continuazione ammessa solo se il suffisso inizia con token configurato (`di`, `del`, `della`, `dei`, `degli`, `delle`, `con`, `senza`, `al`, `alla`, `allo`, `ai`, `alle`);
- source risultato = `LEARNED`.

Test:

- `zucchero` → `zucchero di canna` sì;
- `latte` → `latte detergente` no;
- source LEARNED non fa da seed;
- source AI/INFERRED non fa da seed;
- tra due seed validi vince il più lungo.

## Task 22 — `CategoryResolver` senza AI

Implementare prima la parte deterministica:

1. manual learning;
2. exact static map;
3. fallback `Altro`.

La firma deve già consentire l’estensione AI senza che i caller conoscano l’implementazione.

## Task 23 — Aggiornamento manuale categoria

Use case dedicato `updateProductCategoryManually(productId, category)`:

- valida/canonicalizza;
- salva category;
- imposta source `MANUAL`.

Questo è **l’unico** percorso che trasforma una scelta utente esplicita in MANUAL.

## Task 24 — Export manutenzione mappa

Produrre una funzione/debug utility che esporti:

```text
normalizedName,category,categorySource
```

per source `MANUAL/LEARNED/AI`, escluso `Altro`.

Non fare auto-merge nell’asset.

**DoD Fase 3**

- resolver completamente testato offline;
- nessuna dipendenza Gemini;
- nessuna propagazione ricorsiva LEARNED;
- `Altro` sempre categoria valida.

---

# Fase 4 — Parser offline input libero

Riferimento: specifica §3.4.

## Task 25 — Dizionario unità

Definire set iniziale piccolo di alias italiani, ad esempio:

```text
kg, g, grammo, grammi,
l, litro, litri,
ml,
pezzo, pezzi, pz,
confezione, confezioni,
pacco, pacchi,
barattolo, barattoli
```

Unire a runtime le unità storiche distinte dal DB.

Non trasformare `unit` in enum persistente.

## Task 26 — Parser numero iniziale

Riconoscere:

- interi;
- decimali `.` o `,`;
- niente numeri negativi/zero;
- se non c’è numero, quantity default `1`.

Numeri in lettere fuori scope v1 offline.

## Task 27 — Parser unità/nome

Dopo quantità:

- riconoscere unità solo se presente nel dizionario/historical units;
- opzionalmente rimuovere `di` dopo unità;
- se token non è unità, appartiene al nome.

Test obbligatori:

- `2 litri di latte`;
- `1,5 kg di pasta`;
- `2 mele`;
- `3 confezioni biscotti`;
- unità storica custom;
- input blank;
- quantity zero/negativa;
- testo senza quantità.

## Task 28 — Risultato parser tipizzato

Restituire un data class con almeno:

- raw/name;
- quantity;
- unit;
- eventuale nota residuale solo se semanticamente definita;
- warning/fallback se utile.

Non produrre `note` da token non capiti in modo arbitrario.

---

# Fase 5 — Repository osservabile e UI core

Riferimenti: specifica §3.10, §3.11. **Nota v0.6:** questa fase costruisce il core shopping-only temporaneo. La schermata definitiva unica e le Tasks sono previste nella Fase 12 (Task 89–100): non dichiarare v1 completa al termine della Fase 5.

## Task 29 — Projection UI

Creare projection Room/domain che esponga per ogni riga:

- ListItem fields necessari;
- Product name/category/defaultUnit;
- source categoria se utile a editing/debug.

Restituire `Flow<List<...>>`, ordinato stabilmente.

## Task 30 — Suggerimenti rapidi

Query/aggregazione:

1. purchase count DESC;
2. last purchase DESC;
3. name ASC.

Esporre top N configurabile alla UI. Evitare calcoli N+1 per Product.

## Task 31 — Main ViewModel

Un singolo `StateFlow<MainUiState>` con:

- loading iniziale;
- gruppi TO_BUY per categoria;
- purchased recent/collapsed state;
- errori user-facing tipizzati;
- eventi one-shot gestiti in modo coerente (non mettere eventi consumabili persistenti nello state senza strategia).

## Task 32 — Schermata principale Compose

Mostrare:

- TO_BUY raggruppati per categoria;
- name/quantity/unit/note;
- riquadro PURCHASED collassabile/barrato limitato ai **10 acquisti più recenti** (`statusChangedAt DESC`), come riepilogo e non come storico completo;
- link/azione verso Cronologia per lo storico completo;
- stati empty/loading/error sensati.

## Task 33 — Form aggiunta

Campi:

- name autocomplete;
- quantity;
- unit autocomplete storico;
- category suggerita/modificabile;
- note.

Comportamento:

- `defaultUnit` prefill su Product noto;
- suggerimenti rapidi frequenza/recenza;
- salvataggio disabilitato durante submit;
- errori inline per name/quantity;
- modifica esplicita categoria → MANUAL;
- categoria suggerita non modificata conserva source originaria.

Per Product già noto, non invocare categoria resolver inutilmente: mostrare categoria persistita. Per Product nuovo, **non chiamare AI a ogni keystroke**: aggiornare localmente il suggerimento deterministico e avviare AI solo su evento stabile (selezione/fine editing/IME action/submit), cancellando la richiesta UI precedente e riusando il risultato già ottenuto per lo stesso `normalizedName` nella sessione.

## Task 34 — Azioni lista

Implementare UI per:

- segna comprato;
- elimina;
- eventuale modifica non distruttiva;
- conferme dove richieste.

## Task 35 — Cronologia

Schermata o sezione raggiungibile dalle impostazioni/main:

- filtro PURCHASED;
- filtro DELETED;
- PURCHASED → “Aggiungi di nuovo”;
- DELETED → “Ripristina”.

Assicurarsi che DELETED non sia “ripristinabile solo teoricamente”.

## Task 36 — Clear/purge UI

- “Svuota lista attiva”: dialog/conferma leggera.
- “Pulizia cronologia eliminata”: dialog esplicito bloccante con testo irreversibilità.

## Task 37 — UI tests core

Automatizzare almeno:

- add;
- invalid quantity;
- purchase/delete;
- history re-add/restore;
- clear/purge confirmations;
- empty state.

**DoD Fase 5**

A questo punto Togo deve essere **usabile completamente offline** e soddisfare il core v1 senza AI/Assistant.

Eseguire dogfood reale prima di introdurre dipendenze cloud se possibile.

---

# Fase 6 — Settings, backup e troubleshooting

Riferimenti: specifica §3.12, §3.13, §6.

## Task 38 — DataStore preferences

Predisporre DataStore come infrastruttura per preferenze non sensibili. In questa fase salvare solo preferenze già richieste dal core/UI, ad esempio:

- UI preferences;
- eventuali feature flags locali strettamente necessarie.

**Non modellare ancora provider/modelli AI in questo task:** la configurazione AI multi-provider viene introdotta in modo coerente dal Task 47 tramite `AiSettingsRepository`, riusando la stessa infrastruttura DataStore.

## Task 39 — `SecretStore`

Definire interfaccia e implementazione Android:

- key material in Android Keystore;
- AES/GCM con IV casuale per blob;
- blob in `noBackupFilesDir` o storage esplicitamente escluso;
- delete/reset;
- gestione chiave invalidata/corrupted blob senza crash.

Non usare `EncryptedSharedPreferences` come nuova dipendenza.

Testare la logica separabile e almeno un instrumented test del round-trip.

## Task 40 — Backup rules

Configurare:

- `android:allowBackup="true"`;
- `android:fullBackupContent` per <= Android 11;
- `android:dataExtractionRules` per Android 12+;
- include database/app data necessaria;
- exclude no-backup, secret blob, cache/log temporanei;
- documentare che Auto Backup è traffico gestito dal sistema operativo e non va descritto come “nessuna trasmissione esterna in assoluto”.

Non codificare una fragile whitelist manuale di `db`, `-wal`, `-shm` come assunzione di correttezza.

## Task 41 — Test backup/restore

Documentare ed eseguire procedura Android ufficiale disponibile nell’ambiente per:

- creare dati Product/ListItem;
- backup;
- reinstall/restore;
- verificare DB;
- verificare che segreto AI non sia ripristinato;
- app deve gestire AI config incompleta mostrando “riconfigura chiave”.

## Task 42 — Log locale circolare

Implementare store bounded (es. max entry/byte count documentato):

- timestamp;
- source;
- action type;
- esito;
- parametri redatti/troncati.

API key/token sempre redatti. Evitare testo vocale completo salvo modalità debug esplicita e comunque locale; default preferire metadati.

## Task 43 — Settings screen base

Sezioni:

- dati/backup;
- AI (inizialmente “disattivata/non configurata”);
- troubleshooting log;
- info/versione.

---

# Fase 7 — AI personale provider-agnostic (`PERSONAL_BYOK`)

Riferimento: specifica §3.2.

Questa fase **non deve modificare il funzionamento del core offline**. Procedere in ordine: prima il contratto, poi Gemini come adapter di riferimento, quindi OpenAI e Anthropic. Non implementare tutti e tre in parallelo prima che i contract test siano stabili.

## Task 44 — Verifica documentazione provider corrente

Prima di aggiungere dipendenze o endpoint, consultare **solo documentazione ufficiale** e registrare in `docs/implementation-status.md` data e link verificati per:

### Gemini

- autenticazione/authorization key corrente;
- endpoint REST raccomandato per generazione;
- list models/capability metadata;
- structured output/schema;
- limiti e deprecazioni pertinenti.

Riferimenti iniziali da ricontrollare:

- `https://ai.google.dev/gemini-api/docs/api-key`
- `https://ai.google.dev/gemini-api/docs/oauth`

### OpenAI

- autenticazione API key;
- Responses API corrente;
- Models API;
- Structured Outputs / JSON Schema;
- indicazioni di sicurezza sulle chiavi client-side.

Riferimenti iniziali da ricontrollare:

- `https://platform.openai.com/docs/api-reference`
- `https://platform.openai.com/docs/api-reference/models`

### Anthropic

- autenticazione Claude Platform;
- Messages API;
- Models API e capability metadata;
- structured outputs (`output_config.format` o meccanismo corrente);
- lifecycle/deprecation dei modelli.

Riferimenti iniziali da ricontrollare:

- `https://platform.claude.com/docs/en/api/overview`
- `https://platform.claude.com/docs/en/api/models/list`

**Decisione già presa:** OAuth Google/Gemini è `OUT_OF_SCOPE_V1`. Non implementarlo in questa fase e non interpretare un abbonamento consumer come quota API.

## Task 45 — Contratti AI di dominio

Definire tipi piccoli e vendor-neutral:

```text
AiProviderId = GEMINI | OPENAI | ANTHROPIC
AiModelDescriptor(id, displayName, stable, structuredOutputCompatible)
AiProviderConfig(providerId, selectedModelId, configured)
AiCallStatus(providerId, modelId, timestamp, outcome)
```

Definire una tassonomia errori sealed/type-safe almeno per:

- unauthorized;
- rate limited;
- quota exceeded;
- network unavailable;
- timeout;
- model unavailable;
- unsupported model;
- invalid response;
- provider unavailable;
- disabled.

Non mettere classi SDK/vendor nei tipi di dominio.

## Task 46 — `AiProviderAdapter`, registry e `AiGateway`

Definire:

```text
AiProviderAdapter
- id
- validateCredential(...)
- listCompatibleModels(...)
- extractShoppingItem(...)
- suggestCategory(...)

AiProviderRegistry
- get(providerId)
- availableProviders()

AiGateway
- usa provider/modello attivi
- delega all'adapter
- mappa fallback/status
```

`AiGateway` non contiene `when(provider)` sparsi: la dispatch avviene tramite registry/DI.

Creare `FakeAiProviderAdapter` e `FakeAiGateway` per test del dominio/UI.

## Task 47 — Settings AI e persistenza non sensibile

Creare `AiSettingsRepository` su DataStore per:

- AI mode (`OFF` / `PERSONAL_BYOK`);
- provider attivo;
- modello selezionato **per provider**;
- ultimo stato AI sintetico;
- eventuale flag configured/validated derivabile senza salvare il segreto.

Non salvare credential, token o response body in DataStore.

Cambio provider deve lasciare invariati catalogo/lista/storico.

## Task 48 — `SecretStore` namespaced per provider

Estendere `SecretStore` con chiavi logiche separate:

```text
ai/GEMINI/credential
ai/OPENAI/credential
ai/ANTHROPIC/credential
```

Requisiti:

- Android Keystore + cifratura autenticata;
- blob in storage escluso da backup;
- read/write/delete per singolo provider;
- eliminare una credential non tocca le altre;
- nessun valore completo mostrato in UI dopo salvataggio;
- nessun secret in log, exception message, Bundle state o test fixture.

Testare esplicitamente isolamento tra provider.

## Task 49 — Client HTTP comune per `PERSONAL_BYOK`

Aggiungere il minimo necessario per rete diretta personale.

Preferenza architetturale: **OkHttp condiviso via Hilt + `kotlinx.serialization`**, con adapter REST sottili per i tre vendor. Usare un SDK vendor solo se durante Task 44 emerge un vantaggio necessario che REST non copre su Android; documentare la deviazione.

Configurare:

- HTTPS only;
- timeout espliciti;
- cancellation coroutine corretta;
- niente automatic retry aggressivo su POST generativi;
- niente HTTP logging interceptor che possa stampare `Authorization`, API key o body AI in release;
- user-agent/app version non sensibili se utili.

## Task 50 — Schema di dominio e validator condiviso

Definire un solo schema logico per `ShoppingItemAiResult`:

- `name`;
- `quantity`;
- `unit`;
- `category`.

Definire validator post-response comune:

- name trim/nonblank/max length;
- quantity finite `> 0`;
- unit trim/blank→null/max length;
- category canonicalizzata e inclusa in known categories.

`Altro` sempre incluso nelle categorie note.

Gli adapter mappano il proprio structured output in questo tipo; il dominio non vede JSON vendor-specifico.

## Task 51 — Contract test suite degli adapter

Creare una suite riusabile che ogni `AiProviderAdapter` deve superare con fake/mock HTTP:

- credential valida/invalida;
- model list vuota/errore;
- modello compatibile/incompatibile;
- combined extraction valida;
- category-only valida;
- structured response invalida;
- category fuori enum;
- blank name;
- quantity NaN/infinity/<=0;
- 401/403;
- 429/rate limit/quota;
- 5xx;
- timeout;
- cancellation;
- model unavailable.

**Nessuna live API call in CI. Nessuna chiave reale nel repository.**

## Task 52 — `GeminiAiProviderAdapter`

Implementare Gemini come adapter di riferimento usando il meccanismo verificato nel Task 44.

Requisiti:

- authorization/autenticazione corrente;
- model discovery;
- filtro dei modelli compatibili;
- provider-native structured output;
- mapping errori verso tassonomia interna;
- nessun parsing di testo libero come fallback AI.

Deve passare l’intera contract suite del Task 51.

## Task 53 — Dogfood checkpoint Gemini

Integrare temporaneamente solo Gemini nella UI/settings e usare Togo realmente prima di duplicare l’integrazione.

Verificare manualmente:

- salvataggio/replace/delete credential;
- model discovery;
- combined extraction;
- category-only;
- rete assente;
- quota/auth error;
- fallback offline;
- nessuna regressione core.

Annotare eventuali modifiche necessarie al contratto **prima** di implementare OpenAI/Anthropic. Se il contratto cambia, aggiornare contract test e specifica/piano se il cambiamento è funzionale.

## Task 54 — `OpenAiProviderAdapter`

Implementare adapter OpenAI usando autenticazione/API correnti verificate.

Requisiti specifici:

- API key solo da `SecretStore` OpenAI;
- usare endpoint generativo corrente appropriato (preferire Responses API se resta la raccomandazione corrente);
- structured output JSON Schema nativo;
- `GET /models` può validare accesso/disponibilità ma **non assumere che esponga tutte le capability**: applicare una `OpenAiModelCompatibilityPolicy` centralizzata basata sulla documentazione corrente e coperta da test;
- nessun uso della credenziale Gemini/Anthropic.

Deve passare la contract suite.

## Task 55 — `AnthropicAiProviderAdapter`

Implementare adapter Anthropic usando Claude Platform API corrente.

Requisiti specifici:

- credential solo da namespace Anthropic;
- header/versioning corrente;
- Models API;
- usare capability `structured_outputs` quando disponibile nel metadata corrente;
- structured output nativo corrente (`output_config.format` o sostituto documentato);
- lifecycle/deprecation model gestito come `ModelUnavailable/UnsupportedModel`.

Deve passare la contract suite.

## Task 56 — UI Impostazioni AI multi-provider

Creare la sezione AI con:

- switch AI OFF / PERSONAL_BYOK;
- elenco provider implementati;
- provider attivo;
- per ogni provider: stato credential, configura/sostituisci/elimina, valida;
- model selector provider-specifico;
- stato loading/error;
- ultimo esito sintetico con provider/modello/timestamp;
- testo esplicito: “L’uso API è separato dagli abbonamenti consumer del provider e può avere quote/costi propri”.

Non mostrare provider non implementati come funzionanti. Possono essere omessi o marcati chiaramente non disponibili.

Nessun pulsante “Accedi con Google per usare Gemini” nella v1.

## Task 57 — Model discovery e compatibility policy

Per ciascun adapter:

- usare metadata machine-readable se sufficienti;
- altrimenti combinare model listing con una policy centralizzata e documentata;
- stable prima di preview;
- selezione esistente conservata se valida;
- nessun “primo modello” casuale;
- modello rimosso/incompatibile → config invalid + fallback offline;
- niente override manuale verso model ID non verificato nella UI v1.

Testare che un modello embeddings/audio/image-only non possa essere selezionato accidentalmente come modello Togo.

## Task 58 — Prompt/instruction minimale per provider

Mantenere semantica comune:

- estrattore/classificatore, non conversazionale;
- input italiano;
- nome in forma base/singolare quando ragionevole;
- categoria solo tra quelle consentite;
- nessun testo extra.

La formulazione può differire per provider ma non deve cambiare il contratto funzionale. Non duplicare l’intero enum nel prompt quando lo schema nativo lo vincola già, salvo evidenza da test.

## Task 59 — Integrazione nel `CategoryResolver`

Per aggiunta manuale nuova, l’orchestrazione UI non invoca AI a ogni carattere. Risolvere su evento stabile e cancellare richieste UI precedenti.

Precedenza:

1. MANUAL learning;
2. static map;
3. se nessun risultato utile e AI attiva/configurata → `AiGateway.suggestCategory`;
4. `Altro`.

Per `Product` esistente la categoria persistita vince.

Per combined extraction valida, passare `CategoryHint(category, AI)` senza seconda chiamata category.

## Task 60 — Fallback, cancellation e status multi-provider

- errori vendor mappati nella tassonomia interna;
- `CancellationException` propagata;
- fallback per singola request, non disabilitazione permanente;
- stato sintetico include provider/modello ma nessun contenuto sensibile;
- se provider attivo perde credential/modello, fallback offline finché l’utente corregge la configurazione;
- non fare failover automatico verso un secondo provider configurato nella v1: cambierebbe costi/privacy senza consenso esplicito.

## Task 61 — Test AI multi-provider end-to-end locali

Con mock server/fake e UI test coprire:

- OFF = zero rete;
- ogni adapter passa contract test;
- switch Gemini → OpenAI → Anthropic;
- credential isolation;
- model selection per provider;
- combined/category-only equivalenti semanticamente;
- existing Product category non overwritten;
- `categorySource=AI` indipendentemente dal provider;
- invalid response/fallback;
- selected provider non configurato;
- provider/model removal;
- process recreation non duplica submit;
- log/status non contiene segreti.

Live smoke test con vere credenziali è **manuale, opzionale e locale**, mai requisito CI.

**DoD Fase 7**

- core completamente verde con AI `OFF`;
- Gemini adapter validato e dogfoodato;
- OpenAI e Anthropic implementati solo dopo stabilizzazione del contratto e tutti e tre passano la stessa contract suite;
- nessun secret nel repo/APK/resources/log/backup;
- nessun OAuth Google;
- nessun backend/gateway esterno richiesto per il percorso personale.

---

# Fase 8 — Voice/App Actions: feasibility gate

Riferimenti: specifica §3.3, §3.5, §7.1.

**Questa fase non è una normale feature task list. È un gate.**

## Task 62 — Verifica documentazione App Actions corrente

Consultare documentazione ufficiale e registrare:

- locale di `UPDATE_ITEM_LIST`;
- locale di `CREATE_ITEM_LIST`;
- locale di `GET_ITEM_LIST`;
- locale di `OPEN_APP_FEATURE`;
- eventuale custom intent support;
- requisiti Play/review;
- stato plugin Assistant/test tooling.

Se `it-IT` non è supportato per il caso d’uso necessario, impostare stato:

```text
VOICE_APP_ACTIONS_IT = BLOCKED_BY_PLATFORM
```

nel file di stato e **non implementare workaround non documentati**.

## Task 63 — PoC solo se esiste un locale supportato utile

Se si vuole comunque sperimentare, usare un locale ufficialmente supportato (ad es. en-US se ancora valido) e creare un branch/feature flag PoC.

Obiettivi PoC:

- `shortcuts.xml` registrato;
- preview/test release funzionante;
- cattura parametri reali ricevuti;
- verificare se il testo item conserva quantità/unità sufficienti;
- misurare cosa accade su sideload vs Play test track.

Nessun PoC deve contaminare il core con assunzioni non verificate.

## Task 64 — Router intent/deep link sicuro

Solo se gate aperto:

- un entry point minimo;
- parse verso command tipizzati;
- validazione lunghezze/blank;
- nessuna business logic nell’Activity;
- forward ai use case esistenti;
- handling idempotente per recreation/double intent dove necessario.

## Task 65 — `shortcuts.xml`

Solo capability effettivamente supportate e pertinenti.

- usare `shortcuts.xml`;
- no `actions.xml`;
- resource strings per label/synonyms dove richiesto;
- evitare dichiarazioni non testabili nel locale target.

## Task 66 — Clear/purge voice safety

Un voice/deep link per pulizia può soltanto:

- aprire la destinazione UI;
- mostrare il dialog appropriato.

Non chiamare direttamente `clearActiveList` o `purgeDeletedHistory` dal router esterno.

## Task 67 — Test voice/deep link

- unit test parser router;
- adb deep-link tests;
- App Actions test library/strumenti ufficiali se disponibili;
- malformed/oversized params;
- duplicate delivery;
- clear confirmation;
- device test reale.

**Gate result:**

- `SUPPORTED_AND_TESTED`: feature può entrare in una release compatibile;
- `BLOCKED_BY_PLATFORM`: nessun fallimento del core, documentare;
- `DEFERRED`: PoC non prioritario.

---

# Fase 9 — Hardening, migrazioni e regressione

## Task 68 — Disabilitare destructive migrations

Prima di preservare dati personali reali o creare release test significative:

- versionare schema Room;
- migration esplicite;
- migration test da ogni schema supportato.

## Task 69 — Concurrency suite

Testare con coroutine concorrenti:

- stessa Product name;
- stesso active item compatibile;
- unità incompatibili;
- clear mentre add è in corso dove realistico;
- restore vs add simultanei.

Obiettivo: nessun duplicate Product, nessuna quantità persa, nessun crash constraint non gestito.

## Task 70 — Data integrity suite

Verificare invarianti:

- quantity finite >0;
- product normalizedName unique/nonblank;
- category nonblank;
- status transition valide;
- PURCHASED history non riscritta da re-add;
- purge non tocca PURCHASED.

Dove Room/SQLite può imporre CHECK constraint senza complicazioni, valutarlo; altrimenti validazione dominio + test è sufficiente per v1.

## Task 71 — Process/recreation tests

Verificare:

- ViewModel state;
- submit non duplicato dopo rotation;
- dialog destructive non esegue azione due volte;
- AI request cancellation/lifecycle sensata.

## Task 72 — Performance smoke test

Con dataset sintetico personale “grande” (es. qualche migliaio di ListItem):

- main list query;
- suggestions;
- history;
- known categories.

Non ottimizzare prematuramente, ma eliminare N+1 evidenti/jank.

## Task 73 — Accessibility/localization baseline

- tutte stringhe user-facing in resources;
- contentDescription/semantics dove necessarie;
- touch target Material;
- font scaling smoke test;
- italiano completo.

## Task 74 — Release build smoke

- assemble bundle/release config senza segreti;
- R8/ProGuard se abilitato;
- install/test principali su release-like build.

---

# Fase 10 — Gate per distribuzione pubblica

Questa fase si esegue solo se si decide di andare oltre uso personale.

## Task 75 — Scelta distribuzione

Documentare una delle strade:

### A. Uso personale / piccoli gruppi

- ADB/APK o modalità limited disponibile al momento;
- verificare limiti/dispositivi e developer verification correnti;
- non assumere disponibilità App Actions end-user come su Play.

### B. Google Play

- Play Console;
- target API policy corrente;
- eventuale closed testing richiesto dall’account;
- produzione dopo accesso/review.

Non hardcodare nel codice requisiti amministrativi (numero tester/giorni): verificarli nella documentazione Play al momento del rilascio e annotarli nella release checklist.

## Task 76 — Gate AI pubblico e scelta modello operativo

Prima di pubblicare con AI:

**vietato considerare `PERSONAL_BYOK` il percorso production di default.**

Scegliere esplicitamente:

1. AI disabilitata nella build pubblica; oppure
2. `PUBLIC_MANAGED` multi-provider tramite backend/gateway controllato da Togo; oppure
3. soluzione provider-specifica client-safe ufficialmente supportata e verificata al momento del rilascio.

Per un obiettivo realmente multi-provider, preferire il backend/gateway controllato:

```text
Android -> auth/attestation Togo -> gateway -> Gemini/OpenAI/Anthropic
```

Il gateway deve gestire almeno:

- credenziali upstream server-side;
- autorizzazione client;
- rate limit e abuse prevention;
- quota/budget per provider;
- timeout/retry policy;
- observability senza prompt sensibili per default;
- mapping verso lo stesso contratto Togo.

Se la release sceglie Gemini-only, valutare Firebase AI Logic + App Check secondo documentazione corrente invece di costruire un backend multi-provider non necessario.

## Task 77 — Privacy policy e disclosure

Prima di beta pubblica:

- privacy policy accessibile in-app e nello Store;
- descrivere quali dati vengono inviati all’AI;
- indicare provider effettivamente abilitati nella build;
- chiarire finalità, quota/costi se applicabili e ruolo di eventuale backend;
- disclosure/consenso quando richiesti;
- coerenza con comportamento reale.

## Task 78 — Data Safety

Compilare Google Play Data Safety considerando:

- dati trattati localmente;
- provider AI realmente attivi;
- eventuale backend/Firebase;
- SDK integrati;
- retention/sharing applicabili.

Non copiare dichiarazioni generiche: verificare la build finale.

## Task 79 — App Actions review

Solo se Fase 8 = `SUPPORTED_AND_TESTED` e la release Play contiene realmente App Actions:

- upload test release;
- verifica `shortcuts.xml`;
- App Actions review separata;
- test account/dev group secondo documentazione corrente;
- nessuna promessa di locale non supportato.

---

# Fase 11 — Audit, normalizzazione e integrazione catalogo (Task 80–88)

**Ordine effettivo:** eseguire **dopo Fase 6 e prima di Fase 12 / Fase 7 AI**, anche se i numeri Task seguono 79 per non rinumerare gli incarichi già definiti. Prima di ogni scrittura DB è obbligatoria una dry run. Riferimenti: specifica §2.3, §3.8-bis, §3.16, §3.17; `togo_catalog_review_v2.md`, `togo_catalogo_normalizzato_proposta_v2.md`, `togo_catalog_migration_mapping_v2.csv`, `togo_catalog_candidati_v2.md`.

## Task 80 — Audit schema reale + snapshot [NO DB MUTATION]

- Esaminare `app/src/main/assets/catalog.db` e il codice che lo importa/usa: schema e versione, `CANONICAL_PRODUCT`, categorie L1/L2/L3, tutte le FK e referenze di altre tabelle, relazioni con Room `Product` e `ListItem`.
- Confrontare dati veri con export `catalogo.md` di 1.470 record; se divergono, **fermare** la migrazione e produrre lista differenze.
- Creare copia versionata di test del DB **fuori da percorsi con dati/segreti personali**, verificare `PRAGMA integrity_check`/`foreign_key_check`.
- Output: `docs/catalog-migration-dry-run.md` con conteggi, schema, dipendenze e rollback. Non modificare asset o dati utente.

## Task 81 — Normalizzazione nomi suffix-only + unit test [NO DB MUTATION]

- Implementare funzione **pura** `stripCommercialPackageSuffix()` che elimina solo suffissi finali riconosciuti (`in confezione da 6`, `in confezione da 400 g`, `in lattina da 160 g`, `in barattolo da 500 g`, `Pack da 3`, `con 10 uova`, `da 500 ml`).
- Conservarne il significato in `docs/catalog-migration-dry-run.md`; nessuna conversione implicita verso quantità storiche.
- Test positivi `Latte di Avena in confezione da 6 → Latte di Avena`, `Formaggio Spalmabile in confezione da 6 → Formaggio Spalmabile`, test negativi `Farina 00`, `Latte di Avena`, `Caffè in Capsule`, `Pane Integrale Bio` invariati.
- `normalizedName` univoco solo **dopo** reconciliations; non eseguire bulk UPDATE nomi duplicati causando UNIQUE failures.

## Task 82 — Mapping ID e collisioni [NO DB MUTATION]

- Validare **riga per riga** CSV 1.470 record rispetto al DB reale. La proposta originale comporta 748 canonici e 722 ID secondari; questi numeri devono essere ricalcolati in runtime.
- Riconoscere canonico survivor preferendo record con nome già esatto, altrimenti ID proposto solo come baseline di review; generare elenco collisioni L3 (236 gruppi sulla fotografia di export) e FK.
- Definire algoritmi transazionali/idempotenti per riferimenti, search index, storico acquisti e compatibilità catalogo locale; produrre dry-run delta e piano rollback.
- **STOP obbligatorio per approvazione** prima di qualsiasi cancellazione o aggiornamento persistente al DB. Nel passaggio NO DB MUTATION sono permessi soltanto codice/test/docs e copie di analisi: non effettuare SQL UPDATE/DELETE/INSERT su asset seed o database utente.

## Task 83 — Applicazione controllata a catalog.db, quando autorizzata

- Lavorare su copia del DB e branch; applicare mapping con migrazione transazionale, spostare prima relazioni/reference ID secondari; preservare acquisti, categorie manuali e altre referenze.
- Niente `fallbackToDestructiveMigration`, niente reset database utente; se separati, aggiornare asset seed e migrazioni Room secondo meccanismo effettivo.
- Eseguire doppia applicazione idempotente e confronto statistiche/foreign keys. Se non esiste modo sicuro di migrare riferimenti, fermare task e presentare alternativa non distruttiva (alias).

## Task 84 — Riclassificazione L1/L2/L3 per prodotto

- Validare tassonomia target (proposta: 9 L1, `Surgelati e gelati` indipendente; le Task **non** sono una L1 del catalogo), poi riassegnare **per singolo prodotto**.
- Non trascinare etichette L3 errate del seed; `Mela Renetta` non è frutta tropicale, `Gamberi Rossi` sono crostacei, `Cozze` sono molluschi, `Pane Toscano` non è automaticamente senza glutine.
- Test referenziali e assenza di categorie parent orfane; nessuna caratteristica `Bio/senza glutine` dedotta automaticamente dalla categoria originale.

## Task 85 — Neutri e beni di uso comune

- Rivalidare 40 proposte neutri e 51 beni comuni dopo normalizzazione del DB reale; non tutti sono inserimenti automatici, alcuni `REVIEW_SEMANTICS`.
- Inserire **solo i candidati approvati**, senza inventare ID, con unique normalizzato e categoria corretta; garantire ricerca exact-match neutro prima delle varianti (`Mela` davanti a `Mela Golden`).
- `Pane`/`Latte` già presenti non vanno duplicati. Non includere Tasks nella tassonomia prodotto.

## Task 86 — UI e parser: quantità completamente separata dal nome

- Suggerimenti e autocomplete espongono nomi canonici privi di pack; il form quantità/unità rimane separato.
- `quantity=6` è input utente, non proprietà del prodotto; parser offline può estrarre quantità esplicita da frase utente, mantenendo nome canonico pulito.
- Regression add/merge unit/note, `defaultUnit`, storico `PURCHASED`; nessun Product con `in confezione da N` creato dal form nel normale flusso catalogo.

## Task 87 — Generatore e documentazione di catalogo

- Rigenerare l'export canonico da `catalog.db` **modificato**, con conteggi, L1/L2/L3 e nessun suffisso quantità; non copiare il Markdown proposto nel DB al posto di una migrazione.
- Documentare mapping approvato, eventuali eccezioni e differenze rispetto ai 748 canonici proposti; aggiornare `docs/implementation-status.md`.
- Controllo automatico (in test) per suffix package e normalizedName duplicati.

## Task 88 — Regression e validazione catalogo su device

- SQLite integrity/FK; test di due migrazioni, seed re-import, riapertura app, storico preservato, ricerca neutro, acquisti/spesa dopo rimozione suffix.
- Su installazione con dati precedenti, verifica di upgrade non distruttivo e back-up/restore.
- **Checkpoint catalogo completato** solo con DB reale verificato, build/test/lint verdi e approvazione delle collisioni semantiche.

---

# Fase 12 — Tasks e home unificata (Task 89–100)

**Ordine effettivo:** dopo Fase 11 o dopo conclusione documentata del gate di normalizzazione catalogo; prima di dichiarare v1 core completa. Riferimenti: `togo-lista-unificata-tasks-v1.0.md` §§1–9, specifica §2.2-bis, §3.10, §3.15, §3.17.

## Task 89 — Design di migrazione Task [NO CODE/DB MUTATION]

- Leggere tabelle Room attuali, query MainUiState, menu e cronologia. Preparare `docs/tasks-migration-dry-run.md` con versione DB, tabelle da aggiungere, mapping UI e test di upgrade.
- Se DB già usato realmente, migrazione esplicita obbligatoria. Non modificare codice o DB finché il design non è approvato.

## Task 90 — `TodoTaskEntity` + categorie Tasks separate

- Introdurre `TodoStatus OPEN|COMPLETED|DELETED`, `TodoPriority NORMAL|URGENT`, `TodoTaskEntity` con `title`, `normalizedTitle` NON UNIQUE, category nullable, note, dueDate LocalDate TEXT, timestamps e indici.
- Introduzione T1/T2 opzionali (`Casa`, `Commissioni`, `Personale`, `Lavoro`, `Altro`); nessun L3 Tasks e nessuna FK verso categorie prodotti.
- Additive Room migration con test da vecchio DB. Validare titolo massimo 200 code point e note 1000, controlli Unicode sicuri.

## Task 91 — `TodoDao` e repository testabili

- Osservazione `Flow` di Task OPEN/COMPLETED/DELETED, filtro per status e accesso per id; CRUD e update transazionali.
- Fake `Clock`; test DAO e indici; due Task omonime sono ammesse, nessun merge su titolo.

## Task 92 — Use case Task e transizioni

- `addTask`, `updateTask`, `completeTask`, `reopenTask`, `deleteTask`, `restoreTask`; `completedAt` solo per COMPLETED, `statusChangedAt` cambia solo su transizione.
- Double-click/idempotenza; Task completion non incrementa `purchaseCount`, non crea `Product`, non influisce su AI/category_map.

## Task 93 — `UnifiedListRepository` / proiezione

- Usare `combine` di Flow Spesa e Tasks verso `UnifiedListEntry` sealed (key `S:<id>` vs `T:<id>`), ordinamento deterministico: task urgenti prima, resto `createdAt DESC` e tie-break.
- Filtri `ALL/SHOPPING/TASK`, indipendenti dai dati persistiti; test con ID numerici uguali in domini diversi.

## Task 94 — Unified main ViewModel + stato filtro

- Un `StateFlow<UnifiedListUiState>` principale, con `Tutti` default, selezioni accessibili Spesa/Da fare, loading/empty/error coerenti.
- Cambiare filtro non elimina record; racchiudere tutti i sottoflussi nello stesso elenco, non due liste nascoste.

## Task 95 — UI Compose home a parità di livello

- Mostrare le righe dei due tipi nella stessa lista, icona/label tipo accessibile, azioni per tipo e badge urgenti/scaduti.
- Un solo pulsante `Aggiungi`. Sezione completati recenti massima 10 in totale; cronologia completa resta separata.
- Test Compose dell’ordinamento, filtro e gesti/spunte, includendo empty state in ogni filtro.

## Task 96 — Composer con scelta `Spesa | Da fare`

- Riutilizzare il form Spesa con nome, quantità, unità e catalogo; per Task un form titolo libero e opzionali area/sottocategoria, priority, dueDate, note.
- Type switch esplicito; nessuna conversione NLP silenziosa. Non introdurre quantità in `TodoTask` né un `Product` per `Lavare casa`.
- Doppio submit disabilitato; testi conservati in rotate/recreation quando tecnicamente appropriato.

## Task 97 — Editor Tasks, priorità e scadenze

- Cambiare `NORMAL/URGENT` senza toccare categoria, `LocalDate` timezone-safe, `Scaduto` puramente descrittivo, nessun allarme/notifica.
- Test data con timezone differente e comportamento dei campi opzionali.

## Task 98 — Cronologia unificata e riapertura

- Filtri cronologia per ALL/SHOPPING/TASK, stati completi/eliminati; distinzione `PURCHASED` e `COMPLETED`.
- `Riapri` Task preserva la stessa riga; `Aggiungi di nuovo` Spesa preserva lo storico. Test di regressione di entrambi.

## Task 99 — Svuota/purge entrambi i tipi, in sicurezza

- UI di conferma con conteggi per tipo; `clearActiveList` unificato transazionale (`TO_BUY`+`OPEN` → `DELETED`), ripristino possibile.
- Purge solo DELETED di entrambi i tipi; acquisti e Task completed intatti, conferma irreversibile esplicita. Se Room è distribuito in più DB, progettare strategia failure-safe senza dichiarare atomicità non garantita.

## Task 100 — Test end-to-end e DoD unificata

- UI test: home mista, aggiunte, urgenza, riapertura, scadenza, filtri, doppio tap; Room migration e ricreazione processo.
- Test offline con AI OFF; backup/restore; invarianti del catalogo; dati utente pre-upgrade preservati.
- `./gradlew test`, `./gradlew lint`, `./gradlew assembleDebug` (o `.\gradlew.bat` su Windows); documentare esiti reali e `docs/implementation-status.md`.
- Non dichiarare v1 pronta finché Tasks non è paritaria a Spesa nella home, senza separare in due liste persistenti.

---

# Fase 13 — Matrice test obbligatoria prima di dichiarare v1 completa

## 13.1 Dominio

- [ ] normalize blank/accent/whitespace
- [ ] comparisonKey singolare/plurale semplice
- [ ] comparison ambiguity rejected
- [ ] quantity validation finite >0
- [ ] unit/note compatibility
- [ ] add exact Product
- [ ] add approximate unique Product
- [ ] add new Product
- [ ] concurrent Product insert race
- [ ] active compatible merge
- [ ] active incompatible unit separate row
- [ ] active incompatible note separate row
- [ ] mark purchased
- [ ] delete
- [ ] restore DELETED no compatible active
- [ ] restore DELETED with compatible active
- [ ] re-add PURCHASED preserves history
- [ ] clear active
- [ ] purge only DELETED

## 13.2 Categoria

- [ ] MANUAL update
- [ ] only MANUAL seeds learning
- [ ] longest prefix wins
- [ ] allowed continuation positive
- [ ] unsafe continuation rejected
- [ ] static exact map
- [ ] deterministic before AI
- [ ] AI hint remains AI
- [ ] AI cannot overwrite existing Product category
- [ ] canonical category casing
- [ ] custom manual category in known categories
- [ ] `Altro` always known
- [ ] export includes categorySource

## 13.3 Parser

- [ ] `2 litri di latte`
- [ ] `1,5 kg di pasta`
- [ ] `2 mele`
- [ ] `3 confezioni biscotti`
- [ ] historical custom unit
- [ ] no quantity
- [ ] blank input
- [ ] zero/negative
- [ ] decimal dot/comma

## 13.4 AI

- [ ] AI OFF = zero network
- [ ] `AiProviderAdapter` contract suite comune
- [ ] Gemini credential save/read/delete/validate
- [ ] OpenAI credential save/read/delete/validate
- [ ] Anthropic credential save/read/delete/validate
- [ ] credential isolation tra provider
- [ ] no secret in backup/log/Bundle/test fixtures
- [ ] model list success/error/empty per provider
- [ ] compatibility filtering per provider
- [ ] selected model disappears
- [ ] switch provider conserva dati dominio
- [ ] combined valid per adapter
- [ ] category-only valid per adapter
- [ ] invalid structured response
- [ ] invalid name/quantity/category
- [ ] unauthorized
- [ ] quota/rate limit
- [ ] timeout
- [ ] cancellation
- [ ] fallback offline
- [ ] no silent provider failover
- [ ] consumer subscription non trattata come API credential

## 13.5 Room/backup

- [ ] UNIQUE normalizedName
- [ ] statistics
- [ ] suggestions ordering
- [ ] history query
- [ ] schema export
- [ ] migrations
- [ ] backup DB
- [ ] restore DB
- [ ] secret absent after restore

## 13.6 UI

- [ ] add form
- [ ] suggestions
- [ ] default unit
- [ ] manual category edit
- [ ] purchased section
- [ ] history PURCHASED/DELETED
- [ ] re-add
- [ ] restore
- [ ] clear confirmation
- [ ] purge confirmation
- [ ] AI provider selection/configuration
- [ ] model selection per provider
- [ ] AI loading/error
- [ ] empty/error states

## 13.7 Catalogo e Tasks (obbligatori nella v1 core v0.14)

- [ ] schema reale `catalog.db` verificato e dry-run approvata
- [ ] nome canonico senza suffissi quantità/confezione, `Farina 00` preservata
- [ ] unicità Product, mappatura ID e FK/storico non corrotti
- [ ] neutri selezionabili + varianti, categoria L1/L2/L3 per prodotto corretta
- [ ] un'unica home Spesa + Task, filtri non persistono liste distinte
- [ ] Task category T1/T2 separata da L1/L2/L3 prodotti
- [ ] priorità e dueDate indipendenti, nessuna notifica implicita
- [ ] Task omonime ammesse, completamento e riapertura stabili
- [ ] cancellazione/ripristino/purge entrambi i tipi, acquisti preservati
- [ ] Room migration & backup/restore con dati precedenti
- [ ] test offline AI OFF e Compose accessibility

## 13.8 External input

Solo se voice gate aperto:

- [ ] shortcuts/deep link routing
- [ ] malformed params
- [ ] oversized params
- [ ] duplicate intent handling
- [ ] destructive action requires UI confirmation
- [ ] real-device supported-locale test

---

# 14. Ordine di esecuzione raccomandato

Un agente dovrebbe lavorare nell’ordine seguente e **non anticipare AI/voice**:

```text
Fase 0  Bootstrap
  ↓
Fase 1  Room/data model
  ↓
Fase 2  Domain/transazioni
  ↓
Fase 3  Categoria offline
  ↓
Fase 4  Parser offline
  ↓
Fase 5  UI core
  ↓
Fase 6  Settings/backup/troubleshooting
  ↓
Fase 11 Catalogo: audit → dry-run → migrazione per ID → neutri (Task 80–88)
  ↓
Fase 12 Lista unica: Tasks + UI unificata + history (Task 89–100)
  ↓
DOGFOOD CORE UNIFICATO (Spesa + Da fare)
  ↓
Fase 7  AI personale provider-agnostic
  ↓
Fase 8  Voice feasibility gate (indipendente; può restare BLOCKED)
  ↓
Fase 9  Hardening/migrazioni
  ↓
Fase 10 Pubblicazione solo se richiesta
```

Motivazione: il valore principale di Togo è **una singola lista personale che include Spesa e Da fare**, verificabile senza cloud. La Fase 5 è un checkpoint shopping-only provvisorio. Fase 11 e 12 sono numerate dopo Task 79 per compatibilità con lo storico del piano, ma vanno eseguite prima delle fasi opzionali AI/voice. AI e Assistant sono i componenti più volatili; anticiparli renderebbe il progetto fragile.

---

# 15. Definition of Done v1 core

La **v1 core unificata** è completata quando, senza rete e senza configurazione AI:

1. l’app installa e parte su API 26+ target 36;
2. la home mostra **un unico elenco** con `SHOPPING` e `TASK` alla pari, filtri `Tutti / Spesa / Da fare`;
3. si possono aggiungere/modificare/completare/eliminare prodotti e Task; priorità/scadenze/categorie Task sono opzionali;
4. categorie merceologiche, neutri e nomi canonici privi di pack sono coerenti con DB migrato/verificato;
5. duplicate/merge Spesa rispettano unità e note; Task omonime **non** vengono fuse;
6. PURCHASED resta storico immutabile quando riaggiunto; COMPLETED Task può essere riaperta sulla stessa riga;
7. DELETED di entrambi i tipi è recuperabile fino al purge; nessun acquisto o Task completata viene cancellata dal purge;
8. suggerimenti frequenza/recenza usano solo acquisti, non Task;
9. database, migrazioni Room/catalog.db e backup sono configurati/testati senza perdita di dati;
10. test/lint/build sono verdi e nessuna dipendenza AI/Assistant serve per il funzionamento.

**Checkpoint intermedio:** la vecchia v1 shopping-only (Fasi 0–6) resta utile per dogfood preliminare, ma **non** è la v1 core richiesta dalla specifica v0.14.

La **v1 personale + AI baseline** richiede almeno Gemini dietro il contratto provider-agnostic e un core ancora verde con AI OFF.

La **v1 personale multi-provider** è completa quando anche OpenAI e Anthropic superano la stessa contract suite senza cambiare il dominio.

La **voice integration** non fa parte della Definition of Done finché il gate Fase 8 non è `SUPPORTED_AND_TESTED` per il locale effettivamente desiderato.

---

# 16. Istruzione pronta da dare a Codex/Work

Quando si usa questo file come input operativo, fornire anche `togo-specifichev0.14.md`, `togo-lista-unificata-tasks-v1.0.md` e i file catalogo v2, e **non chiedere di implementare l’intero progetto in un’unica esecuzione**. Iniziare dallo stato di avanzamento reale e dalle fasi mancanti, non riavviare Task 0 se già completato. Lavorare per fase o per 2–4 task coesi.

Prompt iniziale consigliato:

> Stai lavorando sul repository Android di Togo. Leggi integralmente `togo-specifichev0.14.md` e `togo_develop_planv0.6.md`. La specifica è la fonte di verità e il piano è il contratto operativo. Prima di modificare codice, ispeziona repository, Gradle, Git status e documenta in `docs/implementation-status.md` lo stato iniziale. Implementa **solo Fase 0 (Task 0–2)**. Non anticipare Room, AI o App Actions. Mantieni scope minimo, aggiungi i test pertinenti, esegui i checkpoint richiesti e fermati con un riepilogo di file modificati, decisioni, test e problemi aperti. Non committare segreti e non modificare requisiti senza segnalarlo.

Prompt per le iterazioni successive:

> Continua Togo dalla fase successiva non completata indicata in `docs/implementation-status.md`. Rileggi prima la sezione pertinente di `togo-specifichev0.14.md` e `togo_develop_planv0.6.md`, controlla `git diff` e i test esistenti. Implementa solo i task richiesti in questa sessione, aggiorna i test e il file di stato, poi esegui i checkpoint della fase. Non anticipare task successivi e non introdurre workaround non documentati.

Per la Fase 7 AI aggiungere:

> Per le integrazioni esterne usa soltanto documentazione ufficiale corrente. Mantieni il contratto provider-agnostic. Implementa/stabilizza Gemini prima di OpenAI e Anthropic. Non usare OAuth Google nella v1, non creare un backend e non aggiungere provider custom. Non fare live API calls in CI e non salvare mai vere credenziali nel repository.
