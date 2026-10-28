# Togo — Lista personale unificata Android (Spesa + Da fare)

**Versione:** 0.14  
**Ultimo aggiornamento:** 28 ottobre 2026  
**Stato:** specifica funzionale/tecnica di riferimento per la v1  
**Documento collegato:** `togo_develop_planv0.6.md`  
**Specifiche di approfondimento:** `togo-lista-unificata-tasks-v1.0.md`, `togo_catalog_review_v2.md`, `togo_catalog_candidati_v2.md`, `togo_catalogo_normalizzato_proposta_v2.md`, `togo_catalog_migration_mapping_v2.csv`

## Novità di questa revisione

La v0.14 recepisce il requisito esplicito di una **lista personale unica** con **Spesa e Da fare alla pari** e la regola di catalogo **"il nome prodotto non contiene quantità/formati di confezione"**. Conferma la base AI provider-agnostic della v0.13. Il dettaglio normativo delle nuove feature è in `togo-lista-unificata-tasks-v1.0.md`, mentre i file `togo_catalog_*_v2` costituiscono input di **migrazione proposta, non già applicata**.

Novità principali:

- Home unica e lista attiva unificata di righe `SHOPPING` e `TASK`; ricerca, filtri e storico per tipo; Task non sono prodotti del catalogo.
- Modulo Tasks offline con `OPEN/COMPLETED/DELETED`, priorità `NORMAL/URGENT`, scadenza `LocalDate?` facoltativa e categorie Tasks opzionali separate dalle L1/L2/L3 merceologiche.
- Distinte semantiche di completamento: acquisto preserva l'evento `PURCHASED`, Task `COMPLETED` può essere riaperta sulla stessa riga.
- Nomi canonici prodotto svincolati da numero di pezzi, pack e formato (`Latte di Avena in confezione da 6` → `Latte di Avena`); quantità e unità sono sempre campi della riga spesa.
- Migrazione catalogo guidata da mapping di ID e dry run, conservando lo storico. Il dataset di export ha 1.470 righe, 722 suffissi rimossi e 748 nomi distinti proposti dopo collasso; il database `catalog.db` non è stato modificato in questa revisione.
- Piano v0.6: task aggiuntivi per audit, normalizzazione/deduplica e modulo Tasks; non sostituisce le precedenti fasi core e AI già pianificate.

### Gerarchia documentale

La **specifica v0.14 è la fonte di verità generale**. `togo-lista-unificata-tasks-v1.0.md` ne è l'approfondimento normativo per Tasks. Il piano v0.6 definisce *come* implementare; review e mapping catalogo v2 sono **proposte da verificare sul database reale**, non un'autorizzazione a cancellare record. In caso di conflitto l'agente lo segnala e interrompe il task impattato: non deve scegliere autonomamente un comportamento diverso.

---

La v0.13 consolida la v0.12 dopo una review globale e rende l’integrazione AI **provider-agnostic**, mantenendo il core offline indipendente dall’AI. Le decisioni esterne sono state ricontrollate sulla documentazione ufficiale disponibile al 15 settembre 2026.

- Generalizzata l’AI da Gemini-only a un’architettura con `AiGateway` + `AiProviderAdapter` + `AiProviderRegistry`.
- Definiti come provider personali supportabili `GEMINI`, `OPENAI` e `ANTHROPIC`, con rollout incrementale: Gemini prima, poi OpenAI e Anthropic sullo stesso contratto.
- Le credenziali sono separate per provider e archiviate tramite `SecretStore` namespaced; la selezione del provider/modello resta non sensibile in DataStore.
- Vietato trattare i tre provider come se avessero la stessa API: ogni adapter mappa autenticazione, model discovery, structured output ed errori nel contratto interno di Togo.
- Il modello AI selezionabile deve essere **compatibile con structured output**; se il provider non espone capability machine-readable sufficienti, l’adapter applica una policy di compatibilità centralizzata e testata invece di mostrare indiscriminatamente tutti i modelli restituiti.
- Chiarito che abbonamenti consumer come ChatGPT, Claude o Gemini non implicano automaticamente credito/quota API: `PERSONAL_BYOK` usa le credenziali e il billing/quota API del relativo provider.
- Registrata una decisione esplicita: **Google OAuth per usare Gemini non fa parte della v1**. OAuth Gemini è tecnicamente possibile, ma richiede progetto Google Cloud, consenso/scopes, token/refresh e quota project; inoltre “Accedi con Google” non equivale a usare l’abbonamento consumer Gemini.
- Rinominato il percorso futuro `PUBLIC_SAFE` in `PUBLIC_MANAGED`: per una build pubblica multi-provider la soluzione preferita è un backend/gateway controllato da Togo; Firebase AI Logic + App Check resta un’opzione specifica se si decide di offrire solo Gemini.
- Aggiunto un limite di scope: nessun endpoint “OpenAI-compatible” custom e nessun LLM gateway esterno nella v1 personale, finché non esiste un requisito reale.
- Rafforzati privacy e troubleshooting: lo stato AI può registrare provider/modello/esito sintetico, mai prompt completi o credenziali.
- Migliorata la compatibilità delle unità: il merge può usare una `unitComparisonKey` che canonicalizza solo alias noti (`litro/litri/l`, `chilogrammo/kg`, ecc.) senza limitare il testo libero salvabile.
- Chiarito il comportamento della schermata principale: i `PURCHASED` mostrati lì sono solo un riquadro **limitato di acquisti recenti**; lo storico completo resta nella schermata Cronologia, evitando una lista principale che cresce senza limite.

## 1. Panoramica generale

Togo è un’app Android nativa per gestire **un’unica lista personale che comprende, alla pari, la spesa e le cose da fare**. Il principio di prodotto è ridurre al minimo l’attrito nell’aggiunta e nella gestione degli elementi, mantenendo il funzionamento base **locale, offline e indipendente da servizi a pagamento**.

### 1.1 Obiettivo v1 garantito

La v1 deve offrire in italiano:

- una sola **lista personale attiva unificata** per `SHOPPING` (Spesa) e `TASK` (Da fare), con filtro `Tutti / Spesa / Da fare`;
- aggiunta e completamento offline di Tasks, con priorità/scadenza/categoria opzionali e cronologia separate dal dominio acquisti;
- aggiunta manuale rapida con suggerimenti;
- categorizzazione offline deterministica;
- quantità, unità libera e note;
- storico degli acquisti;
- eliminazione logica e ripristino;
- backup Android dei dati non sensibili;
- AI opzionale, mai necessaria al funzionamento base.

### 1.2 Obiettivo vocale e vincolo di piattaforma

L’obiettivo di lungo periodo resta poter aggiungere elementi senza navigare manualmente nell’app. Tuttavia, **al 15 settembre 2026 l’invocazione italiana tramite Google Assistant/App Actions non è un requisito v1 garantibile**: le BII considerate per le liste supportano l’invocazione utente in un insieme di locale che non comprende l’italiano; anche `OPEN_APP_FEATURE` non include `it-IT`.

Di conseguenza:

- l’integrazione App Actions è una **feature sperimentale/condizionata**;
- non deve bloccare la consegna del core dell’app;
- l’implementazione completa va preceduta da un proof of concept su dispositivo e locale effettivamente supportato;
- i testi italiani come *“Hey Google, aggiungi latte alla lista della spesa”* descrivono l’esperienza desiderata, non un criterio di accettazione della v1 finché Google non supporta il locale;
- l’apertura generica dell’app tramite Assistant può essere disponibile per app pubblicate, ma non equivale al controllo profondo tramite App Actions.

### 1.3 Distribuzione

Percorso previsto:

1. sviluppo e uso personale via Android Studio/ADB/APK;
2. eventuale distribuzione limitata per test personali;
3. eventuale distribuzione tramite Google Play, solo dopo i relativi gate di qualità, privacy e policy.

La distribuzione Android “Limited distribution” è utile per uso personale/piccoli gruppi, ma **non sostituisce la pubblicazione Google Play richiesta dalle App Actions per utenti finali**.

### 1.4 Piattaforma e lingua

- **Piattaforma:** Android nativa, Kotlin.
- **Lingua UI e dominio v1:** italiano.
- **Liste multiple:** fuori scope v1; Spesa/Da fare sono due tipi nella **stessa lista**, non due liste distinte.
- **Tasks:** incluse nella v1 core finale dopo le milestone catalogo e modulo Tasks; senza AI e senza notifiche.
- **Account Togo/backend/sync:** fuori scope v1.

---

## 2. Modello dati

### 2.1 Entità `Product`

Rappresenta un prodotto noto al catalogo, indipendentemente dalla sua presenza nella lista attiva.

| Campo | Tipo | Vincoli / note |
|---|---|---|
| `id` | `Long`, PK autogenerata | — |
| `name` | `String` | nome visualizzato/canonico, non vuoto |
| `normalizedName` | `String` | normalizzazione deterministica; **UNIQUE** |
| `category` | `String` | categoria canonica, non vuota |
| `categorySource` | enum `MANUAL / INFERRED / LEARNED / AI` | provenienza della categoria |
| `defaultUnit` | `String?` | ultima unità non vuota effettivamente confermata |

Indici richiesti:

- indice univoco su `normalizedName`;
- eventuali indici aggiuntivi solo se giustificati da query reali.

**Regola di unicità:** due operazioni concorrenti che tentano di creare lo stesso `normalizedName` devono convergere sullo stesso `Product`. L’unicità non può dipendere solo da un `SELECT` precedente all’`INSERT`.

**Statistiche di acquisto:** non vengono persistite sul prodotto. Si calcolano dai `ListItem` in stato `PURCHASED`:

- numero acquisti = conteggio righe `PURCHASED` del prodotto;
- ultimo acquisto = massimo `statusChangedAt` tra quelle righe.

### 2.2 Entità `ListItem`

Rappresenta una specifica occorrenza di un prodotto nella lista/storico.

| Campo | Tipo | Vincoli / note |
|---|---|---|
| `id` | `Long`, PK autogenerata | — |
| `productId` | `Long`, FK → `Product` | indicizzato |
| `quantity` | `Double` | default `1.0`; deve essere finita e `> 0` |
| `unit` | `String?` | testo libero; trim; stringa vuota → `null` |
| `note` | `String?` | testo libero; trim; stringa vuota → `null` |
| `status` | enum `TO_BUY / PURCHASED / DELETED` | indicizzato insieme a `productId` se utile alle query |
| `insertedAt` | `Long` | epoch millis |
| `statusChangedAt` | `Long` | epoch millis; alla creazione coincide con `insertedAt`, poi viene aggiornato a ogni reale transizione di stato |

La FK non deve comportare la cancellazione automatica dei `Product` quando si eliminano righe storiche. Il catalogo resta conoscenza locale utile per suggerimenti e categorizzazione.

### 2.2-bis Entità Task e proiezione unificata

La nuova funzionalità **non** estende `Product` con attività né crea `CANONICAL_PRODUCT` fittizi. Aggiungere un aggregato separato `TodoTaskEntity` con `id`, `title`, `normalizedTitle` (NON UNIQUE), `categoryId?`, `priority NORMAL|URGENT`, `dueDate: LocalDate?`, `note?`, `status OPEN|COMPLETED|DELETED`, `createdAt`, `statusChangedAt`, `completedAt?`. Categorie Task `TaskCategory` opzionali, al massimo **due livelli (area/sottocategoria)**, non collegate alla tassonomia merci L1/L2/L3.

La home aggrega `Flow` Spesa e Tasks in `UnifiedListRepository`, esponendo `UnifiedListEntry` con identificatori UI `S:<listItemId>` e `T:<todoTaskId>`. Nessuna tabella-polimorfica obbligatoria, nessun `UNION` fragile fra entità diverse. Dettaglio normativo e test in `togo-lista-unificata-tasks-v1.0.md` §§2–7.

### 2.3 Normalizzazione e matching del prodotto

**Invariante semantico:** il nome di catalogo identifica **che cosa** comprare, non **quanto**. Suffissi come `in confezione da 6`, `Pack da 3`, `da 500 ml`, `in lattina da 160 g`, `con 10 uova` sono esclusi dal **nome canonico**; quantità e unità appartengono a `ListItem` e vengono indicate esplicitamente dall'utente. Conservare invece differenze di prodotto reali (`Latte di Avena`, `Pane Integrale`, `Caffè in Capsule`, `Farina 00`). Non eliminare numeri interni merceologici come `00`.

`normalizedName` viene calcolato sempre allo stesso modo:

1. trim;
2. lowercase con locale stabile;
3. rimozione diacritici/accenti;
4. normalizzazione degli spazi multipli;
5. rimozione di spazi ai bordi risultanti.

Un nome vuoto dopo la normalizzazione è invalido.

#### Matching approssimato offline

Il matching approssimato serve solo a ridurre duplicati banali singolare/plurale quando l’AI non ha già restituito un nome canonico. Non sostituisce `normalizedName` persistito.

Si usa una `comparisonKey` conservativa, per token:

- opera solo su token alfabetici di almeno 4 caratteri;
- può rimuovere una sola vocale finale tra `a/e/i/o`;
- non altera token corti, numeri o abbreviazioni;
- non applica Levenshtein né sinonimi generici.

Regola di sicurezza: il match approssimato viene accettato **solo se produce un singolo candidato `Product` non ambiguo**. Con zero o più di un candidato si crea/usa il prodotto basato sul nome normalizzato esatto.

Esempio desiderato: `mele` può riconoscere `mela`; non si deve fondere automaticamente `riso` con `rosa` o casi ambigui.

### 2.4 Compatibilità per il merge di righe attive

Non si assume più che esista una sola riga `TO_BUY` per `Product`.

Due richieste sullo stesso `Product` si fondono solo se:

- la `unitComparisonKey` è uguale, inclusa la condizione `null == null`;
- `note` normalizzata è uguale, inclusa `null == null`.

`unitComparisonKey` usa trim/lowercase e canonicalizza **solo alias noti** condivisi con il dizionario del parser (es. `litro`, `litri`, `l` → `l`; `chilogrammo`, `chilogrammi`, `kg` → `kg`). Per unità custom non note usa semplicemente il testo normalizzato: il valore originale salvato resta libero e non viene riscritto.

Se unità o nota differiscono, si crea una nuova riga `TO_BUY`. Questo evita somme prive di significato, ad esempio `1 l di latte` + `2 confezioni di latte`, pur permettendo il merge di `1 litro` + `2 l`.

La quantità della riga compatibile viene incrementata in una transazione atomica.

---

## 3. Funzionalità

### 3.1 Stati, eliminazione logica e storico

- `TO_BUY`: voce attiva da acquistare.
- `PURCHASED`: acquisto completato; resta nello storico e contribuisce alle statistiche.
- `DELETED`: voce rimossa per errore o tramite svuotamento; non contribuisce alle statistiche.

#### Transizioni

- `TO_BUY → PURCHASED`: azione “Segna comprato”.
- `TO_BUY → DELETED`: azione “Elimina”.
- `DELETED → TO_BUY`: azione “Ripristina”. Se esiste una riga attiva compatibile secondo §2.4, la quantità viene assorbita in quella riga e la riga storica resta `DELETED`; altrimenti la stessa riga torna `TO_BUY`.
- `PURCHASED`: **non torna direttamente `TO_BUY`**. L’azione esposta è “Aggiungi di nuovo”: la riga `PURCHASED` resta invariata e viene creata o incrementata una nuova riga `TO_BUY` compatibile. In questo modo lo storico non viene riscritto.

`clearActiveList()` porta tutte le righe `TO_BUY` a `DELETED` in un’unica transazione.

`purgeDeletedHistory()` cancella fisicamente **solo** le righe `DELETED`, dopo conferma esplicita nella UI. Non elimina `PURCHASED` né `Product`.

### 3.2 Integrazione AI opzionale e provider-agnostic

L’AI è un acceleratore opzionale per:

- estrazione strutturata da testo vocale/libero;
- forma base del nome prodotto;
- categorizzazione quando i meccanismi deterministici non bastano.

Il core deve funzionare senza rete e non deve dipendere da Gemini, OpenAI, Anthropic, Firebase o da un backend Togo.

#### 3.2.1 Confine architetturale

Il dominio dipende da `AiGateway`, che espone **operazioni Togo**, non primitive specifiche di un vendor. `AiGateway` orchestra il provider attivo e delega a un `AiProviderAdapter` registrato in `AiProviderRegistry`.

Contratto concettuale minimo dell’adapter:

```text
AiProviderAdapter
- id
- validateCredential()
- listCompatibleModels()
- extractShoppingItem(text, knownCategories, model)
- suggestCategory(name, knownCategories, model)
```

Provider previsti per l’uso personale:

- `GEMINI`;
- `OPENAI`;
- `ANTHROPIC`.

L’implementazione va introdotta incrementalmente: **Gemini è il primo adapter di riferimento**; OpenAI e Anthropic vengono aggiunti solo dopo che il contratto comune e i contract test sono stabili. La UI deve mostrare come selezionabili solo provider realmente implementati e validati nella build corrente.

Non creare una falsa API “universale” verso l’esterno: autenticazione, endpoint, formato structured output, model discovery e tassonomia errori restano responsabilità dei singoli adapter. L’agnosticismo esiste **dentro Togo**, non perché i provider siano equivalenti.

Modalità AI:

- `OFF`: zero chiamate AI e zero rete dovuta al modulo AI;
- `PERSONAL_BYOK`: uso personale/sperimentale con credenziale inserita dall’utente per il provider selezionato;
- `PUBLIC_MANAGED`: eventuale percorso futuro per una build pubblica, in cui l’app non contiene credenziali upstream statiche e passa da un backend/gateway controllato o da una soluzione provider-specifica equivalente.

La v1 core richiede solo `OFF`. La fase personale può aggiungere `PERSONAL_BYOK`. `PUBLIC_MANAGED` è un **release gate**, non un requisito da implementare prima che esista una decisione reale di pubblicazione.

#### 3.2.2 Configurazione provider e credenziali personali

La configurazione non sensibile comprende almeno:

```text
selectedProvider
selectedModelByProvider
providerEnabled/configured state
lastAiStatus
```

Questi dati possono stare in DataStore. Le credenziali **non** devono stare in DataStore.

`SecretStore` deve essere namespaced per provider, ad esempio concettualmente:

```text
ai/GEMINI/credential
ai/OPENAI/credential
ai/ANTHROPIC/credential
```

Per `PERSONAL_BYOK`:

- Gemini usa il tipo di chiave/autenticazione API corrente supportato dalla Gemini API;
- OpenAI usa una API key del progetto/account API;
- Anthropic usa una API key Claude Platform compatibile con l’autenticazione corrente;
- non inserire mai credenziali dello sviluppatore nel codice, nelle risorse o nell’APK;
- non loggare credenziali né includerle in crash report, Bundle state, analytics o backup;
- proteggere i valori a riposo con Android Keystore + cifratura autenticata e salvare il blob in storage escluso dal backup;
- se la chiave Keystore non è più disponibile dopo restore/reset, invalidare la configurazione del provider senza crash e richiedere nuovamente la credenziale;
- mostrare chiaramente che consumo, quota e billing appartengono all’**API del provider**, non all’eventuale abbonamento consumer ChatGPT/Claude/Gemini.

Questa protezione riduce il rischio a riposo ma **non rende una chiave statica sicura in una app pubblica**: durante una chiamata il client deve poter usare il segreto. `PERSONAL_BYOK` è quindi una scelta consapevole per uso personale/dogfood, non il default production.

#### 3.2.3 Selezione provider e modello

L’utente può configurare più provider e scegliere quale usare come provider attivo. Cambiare provider non deve modificare dati di dominio già salvati.

Ogni provider mantiene la propria selezione modello. La disconnessione/eliminazione della credenziale di un provider non deve cancellare le credenziali degli altri.

Model discovery è **provider-specifica**:

- se il provider espone una Models API con capability utili, usarle;
- se la lista modelli non dichiara in modo affidabile tutte le capability necessarie, applicare nell’adapter una policy di compatibilità centralizzata, documentata e coperta da test;
- mostrare soltanto modelli compatibili con il caso d’uso Togo, in particolare generazione testuale e structured output affidabile;
- mantenere la selezione precedente se ancora valida;
- non scegliere “il primo modello restituito” senza una policy deterministica;
- se il modello scompare o diventa incompatibile, marcare la configurazione come non valida e usare fallback offline fino a nuova selezione;
- preview/experimental non sono default salvo scelta esplicita dell’utente.

Un modello che non consente all’adapter di ottenere l’output strutturato richiesto non è compatibile con Togo v1. Non fare fallback automatico a parsing libero del testo AI.

#### 3.2.4 Schema logico comune e structured output

Tutti gli adapter devono produrre lo stesso risultato di dominio, indipendentemente dal wire format del provider:

```json
{
  "name": "latte",
  "quantity": 2,
  "unit": "l",
  "category": "Latticini"
}
```

Lo schema logico vincola:

- `name`: stringa non vuota, richiesta in forma base/singolare quando ragionevole;
- `quantity`: numero finito e `> 0`;
- `unit`: stringa nullable/libera;
- `category`: valore appartenente alle categorie note.

Le categorie note sono l’unione deduplicata e canonicalizzata di:

1. categorie presenti in `category_map.json`;
2. categorie già presenti in `Product`;
3. categoria obbligatoria `"Altro"`.

Ogni adapter deve usare il **meccanismo structured-output nativo del provider/modello** e mappare il risultato nel tipo interno. Lo schema nativo può differire tra Gemini, OpenAI e Anthropic; non deve trapelare nel dominio.

Lo structured output riduce gli errori sintattici ma **non sostituisce la validazione semantica locale**.

#### 3.2.5 Operazioni AI

Due use case di dominio:

- `extractShoppingItem(text)`: nome + quantità + unità + categoria in una sola chiamata quando l’input libero richiede già parsing AI;
- `suggestCategory(name, knownCategories)`: sola categoria, invocata soltanto quando la cascata deterministica non produce un risultato utile.

La categorizzazione manuale non deve generare una richiesta AI se una categoria valida è già disponibile tramite apprendimento manuale o mappa statica.

Per un `Product` esistente, la categoria persistita continua a vincere: il cambio provider o una nuova risposta AI non devono riscriverla automaticamente.

#### 3.2.6 Tassonomia errori e fallback

Gli adapter traducono gli errori vendor-specific in una tassonomia interna almeno equivalente a:

- `Unauthorized`;
- `RateLimited`;
- `QuotaExceeded`;
- `NetworkUnavailable`;
- `Timeout`;
- `ModelUnavailable`;
- `UnsupportedModel`;
- `InvalidResponse`;
- `ProviderUnavailable`;
- `Disabled`.

Qualunque errore non di cancellazione attiva il fallback deterministico appropriato. La `CancellationException` delle coroutine deve propagare secondo structured concurrency e non essere trasformata in un errore generico.

Lo stato diagnostico non sensibile può contenere:

```text
provider, model, timestamp, esito sintetico/error kind
```

Non deve contenere prompt completi, response raw se non strettamente necessarie al debug, header di autenticazione o credenziali.

#### 3.2.7 OAuth Google: decisione v1

L’autenticazione OAuth alla Gemini API è tecnicamente possibile, ma **non viene implementata nella v1**.

Motivi:

- richiede un progetto Google Cloud con Generative Language API abilitata;
- richiede consent screen, client OAuth, scopes e gestione di access/refresh token;
- le richieste sono comunque associate a un progetto/quota/billing API;
- “Accedi con Google” non significa “usa automaticamente il mio abbonamento consumer Gemini”;
- introdurre oggi una seconda strategia di autenticazione aumenterebbe la complessità senza migliorare il principale caso d’uso personale, dove la credenziale viene configurata una sola volta.

Non introdurre un’astrazione OAuth generica preventiva. Se in futuro un requisito reale lo giustifica, estendere l’autenticazione del solo provider interessato dietro l’adapter esistente.

#### 3.2.8 Gate per una futura build pubblica

In una build pubblica non considerare le chiavi BYOK statiche sul client come soluzione production di default. Le indicazioni correnti dei provider richiedono di evitare segreti statici esposti in applicazioni client distribuite.

Per un futuro Togo pubblico multi-provider, il percorso preferito è `PUBLIC_MANAGED`:

```text
Android Togo -> autenticazione/attestazione Togo -> backend o LLM gateway controllato -> provider AI
```

Il backend conserva le credenziali upstream, applica rate limit/quota e normalizza le policy. Se si decide invece di supportare **solo Gemini**, Firebase AI Logic con App Check può essere una soluzione provider-specifica adeguata.

Prima di costruire `PUBLIC_MANAGED` devono esistere un requisito di pubblicazione, un modello costi/quota e una decisione privacy; non implementarlo nella fase personale “per sicurezza futura”.

### 3.3 Integrazione vocale tramite Google Assistant/App Actions

Questa sezione è **condizionata** dai risultati del gate di fattibilità §7.

Stato piattaforma rilevante al 15 settembre 2026:

- `CREATE_ITEM_LIST` / `UPDATE_ITEM_LIST` / `GET_ITEM_LIST`: invocazione utente non garantita in italiano;
- `OPEN_APP_FEATURE`: locale supportati non comprendono `it-IT`;
- custom intents: non rappresentano una soluzione italiana generale;
- le App Actions per utenti finali richiedono che l’app sia distribuita tramite Google Play e siano sottoposte a review separata.

Pertanto la v1 italiana **non deve dichiarare come requisito di accettazione** l’aggiunta hands-free tramite Assistant.

Se il PoC viene svolto in un locale supportato:

- usare `shortcuts.xml`, non il legacy `actions.xml`;
- trattare tutti i parametri ricevuti come input non fidato;
- passare gli intent a un router interno tipizzato;
- nessuna azione distruttiva deve essere eseguita solo perché un deep link/intento è stato ricevuto.

### 3.4 Parsing offline di quantità/unità

Il parser offline opera sul testo effettivamente ricevuto dal canale di input. Non assume che Assistant fornisca sempre la frase completa: questa proprietà va verificata nel PoC.

Regole minime:

1. trim e normalizzazione spazi;
2. riconoscere un numero iniziale arabo intero o decimale con `.` o `,`;
3. dopo il numero, riconoscere un’unità **solo se** il token/frase appartiene a:
   - un piccolo dizionario di alias unità (`kg`, `g`, `l`, `ml`, `litro/i`, `grammo/i`, `confezione/i`, `pezzo/i`, ecc.); oppure
   - unità già usate storicamente dall’utente;
4. rimuovere un eventuale connettore `di` dopo un’unità riconosciuta;
5. il resto non vuoto diventa il nome;
6. se non è riconosciuta un’unità, il primo token dopo la quantità appartiene al nome.

Esempi:

- `2 litri di latte` → `quantity=2`, `unit="litri"`, `name="latte"`;
- `1,5 kg di pasta` → `1.5`, `kg`, `pasta`;
- `2 mele` → `2`, `null`, `mele`;
- `mele` → quantità default `1`, unità `null`, nome `mele`.

I numeri in lettere (`due`, `mezzo`, ecc.) non sono requisito del parser offline v1.

### 3.5 Rimozione/pulizia tramite voce

Non esiste un requisito v1 di rimozione vocale automatica.

Se in futuro un locale/supporto App Actions adeguato rende disponibile un percorso verso una feature di pulizia:

- la voce può al massimo aprire la destinazione corretta;
- `clearActiveList()` richiede comunque conferma in-app;
- `purgeDeletedHistory()` richiede un dialog esplicito e più forte;
- nessun deep link può effettuare direttamente la cancellazione fisica.

### 3.6 Categorie

#### 3.6.1 Regola per `Product` esistente

Se il `Product` esiste già:

- la sua categoria persistita è la fonte di verità;
- una nuova proposta AI non sovrascrive automaticamente una categoria esistente;
- solo un’azione esplicita dell’utente di modifica categoria aggiorna `category` e imposta `categorySource=MANUAL`.

#### 3.6.2 Risoluzione per nuovo `Product`

Se il chiamante dispone già di un `categoryHint` con provenienza certa:

- modifica esplicita utente → `MANUAL`;
- risultato della chiamata AI combinata già riuscita → `AI`;

il valore viene validato/canonicalizzato e non viene nuovamente dedotto.

Se manca un hint, usare in ordine:

1. **apprendimento manuale conservativo** (§3.6.3) → `LEARNED`;
2. match esatto in `category_map.json` → `INFERRED`;
3. se AI attiva e ammessa per quel flusso, `suggestCategory` → `AI`;
4. fallback `"Altro"` → `INFERRED`.

Questo ordine evita rete/latency quando l’app possiede già una risposta deterministica.

#### 3.6.3 Apprendimento manuale conservativo

Solo `Product` con `categorySource=MANUAL` possono essere sorgente dell’apprendimento automatico.

Un candidato manuale può essere riusato se:

- il suo `normalizedName` è un prefisso su confine di token del nuovo nome;
- il match candidato è il più lungo disponibile;
- la parte rimanente inizia con una continuazione sicura configurata, ad esempio una preposizione/modificatore come `di`, `del`, `della`, `dei`, `degli`, `delle`, `con`, `senza`, `al`, `alla`, `allo`, `ai`, `alle`.

Esempio positivo: `zucchero` → `zucchero di canna`.

Esempio da non propagare: `latte` categorizzato come latticino → `latte detergente`.

`LEARNED`, `AI` e `INFERRED` **non diventano a loro volta sorgenti di apprendimento**, così non si crea propagazione a catena di errori.

### 3.7 Mappa offline e ciclo di miglioramento

`assets/category_map.json` ha forma:

```json
{
  "zucchero": "Dispensa",
  "latte": "Latticini",
  "mela": "Frutta e verdura",
  "detersivo per piatti": "Pulizia casa"
}
```

La mappa viene caricata una volta e validata. **Quando la tassonomia L1/L2/L3 viene migrata**, riesaminare anche i valori e la provenienza delle categorie presenti nella mappa, senza convertire automaticamente categorie manuali degli utenti:


- chiavi normalizzate e non vuote;
- categorie non vuote/canonicalizzate;
- conflitti/duplicati segnalati nei test.

Per preparare un aggiornamento della mappa si esportano almeno:

```text
normalizedName, category, categorySource
```

per `categorySource ∈ {MANUAL, LEARNED, AI}`, escludendo `category="Altro"`.

La revisione pre-release resta manuale. In conflitto:

1. `MANUAL` ha precedenza;
2. `LEARNED` e `AI` richiedono revisione;
3. nessun record viene incorporato automaticamente nell’asset in produzione.

### 3.8 Quantità, unità e note

- `quantity` default `1.0`, finita e `> 0`;
- valori invalidi da UI bloccano il salvataggio con errore locale;
- valori AI invalidi attivano fallback;
- `unit` e `note` sono libere, trim, stringa vuota → `null`;
- il confronto per merge usa `unitComparisonKey` (§2.4), ma il testo di `unit` salvato resta quello confermato dall’utente/AI;
- `defaultUnit` viene aggiornata solo quando un’unità non vuota viene realmente salvata/confermata;
- la UI suggerisce unità storiche senza imporre un enum.

Input linguistici non numerici come `"un po'"` non diventano quantità: possono essere mantenuti nella nota solo se provengono da un percorso che li identifica esplicitamente senza perdere il nome del prodotto.

### 3.8-bis Regola catalogo: nomi senza confezione

Il catalogo seed `app/src/main/assets/catalog.db`, se presente nel repository, deve contenere prodotti selezionabili con denominazione **senza taglie di confezione, numero di confezioni o volume/peso commerciale nel nome**. Il parser quantità può interpretare una richiesta esplicita dell’utente, ma l'anagrafica continua a chiamarsi `Latte di Avena` anche quando si vuole `6` come `ListItem.quantity`. Non leggere implicitamente un vecchio `pack da 6` come sei unità negli storici: serve una migrazione verificata caso per caso.

Per i dati originali esportati, vedere `togo_catalogo_normalizzato_proposta_v2.md` e `togo_catalog_migration_mapping_v2.csv`: **748 nomi proposti**, ma il DB originale e lo schema FK non sono stati ispezionati. Preferire il `Product` canonico esistente e rimappare gli eventuali ID duplicati solo dopo dry run, test FK e integrità. I neutri mancanti (es. `Mela`) restano inserimenti proposti con un nuovo ID assegnato dal DB, senza alterare `Mela Golden`.

### 3.9 Aggiunta elemento

L’operazione di dominio `addItem` deve essere atomica e ricevere un comando tipizzato, concettualmente:

```text
AddItemCommand(
  rawName,
  quantity,
  unit,
  note,
  categoryHint?
)
```

Comportamento:

1. valida e normalizza input;
2. risolve un `Product` esatto o un singolo match approssimato sicuro;
3. se assente, crea il `Product` rispettando il vincolo UNIQUE e gestendo race concorrenti;
4. per un prodotto esistente, non sovrascrive categoria salvo modifica manuale esplicita;
5. cerca una riga `TO_BUY` compatibile secondo §2.4;
6. se presente incrementa la quantità atomically; altrimenti inserisce una nuova riga;
7. aggiorna `defaultUnit` quando applicabile.

### 3.10 UI e cronologia

#### Schermata principale — **lista unificata**

- singolo feed delle righe attive `SHOPPING/TO_BUY` e `TASK/OPEN`, con uguale rilevanza visiva, tipo riconoscibile e filtri `Tutti | Spesa | Da fare` (default `Tutti`);
- le righe Spesa mostrano nome/quantità/unità/categoria/nota; le righe Task mostrano titolo/priorità e, se disponibili, categoria Task/scadenza/nota;
- default ordine unico: Task `URGENT` prima, poi `createdAt DESC` per gli altri elementi, con tie-break deterministici; il filtro non crea o elimina dati;
- comando `Aggiungi` unico: scelta esplicita del tipo `Spesa` o `Da fare`; nessuna classificazione automatica silenziosa;
- riquadro **completati recenti** collassabile: default massimo 10 elementi in totale tra `PURCHASED` e `COMPLETED`, con label di stato; lo storico completo è in Cronologia;
- azioni per tipo: Spesa `Segna comprato/Aggiungi di nuovo`, Task `Completa/Riapri`, in entrambi `Elimina/Ripristina` secondo lo stato.

`Tutti` non è una terza lista: è un filtro di visualizzazione della medesima lista personale. Non raggruppare esclusivamente per categoria merceologica, perché nasconderebbe i Task o li classificherebbe come prodotti.

L'implementazione incrementale può avere una home temporanea shopping-only prima della fase lista unificata, ma **non** è Definition of Done della v1 aggiornata.

#### Aggiunta rapida

Il composer sceglie esplicitamente `Spesa | Da fare`. La sezione seguente descrive **solo Spesa**; il form `Da fare` è dettagliato nel documento Tasks.

- nome con suggerimenti da catalogo;
- ordinamento suggerimenti: numero acquisti decrescente, ultimo acquisto decrescente, nome come tie-break stabile;
- quantità e unità;
- `defaultUnit` precompilata;
- categoria suggerita ma modificabile;
- nota.

La categorizzazione non deve generare una richiesta AI a ogni carattere digitato. Il suggerimento deterministico può aggiornarsi localmente; una richiesta AI va avviata solo su un evento stabile (ad es. selezione suggerimento, fine editing/IME action o submit), cancellando la richiesta UI precedente e riusando il risultato per lo stesso `normalizedName` nella sessione. Se richiede AI, mostra stato di caricamento e impedisce doppi submit concorrenti.

#### Cronologia

Un solo percorso di cronologia raggiungibile con filtro `Tutti | Spesa | Da fare` e per stato, comprendente:

- `PURCHASED` Spesa, con “Aggiungi di nuovo” (storico invariato);
- `COMPLETED` Tasks, con “Riapri” (stessa riga Task);
- `DELETED` di entrambi i tipi, con “Ripristina” finché non vengono purgati.

I Task completati **non** contribuiscono a contatori né suggerimenti d'acquisto.

Questo rende effettivamente utilizzabile la semantica di ripristino.

#### Azioni distruttive

- “Svuota lista attiva”: riepilogo esplicito dei conteggi Spesa + Da fare e conferma; cambia `TO_BUY` e `OPEN` a `DELETED` transazionalmente, senza toccare gli acquisti o Task completati;
- “Pulizia cronologia eliminata”: dialog bloccante esplicito, perché irreversibile, e purge di sole righe `DELETED` per entrambi i tipi.

### 3.11 Storico e suggerimenti

Statistiche e suggerimenti derivano sempre da `PURCHASED`, non da contatori duplicati.

Ordinamento base dei suggerimenti:

1. `purchaseCount DESC`;
2. `lastPurchaseDate DESC NULLS LAST`;
3. `name ASC`.

La lista attiva può essere esclusa dai suggerimenti “aggiungi rapidamente” se ciò evita duplicazioni inutili nella UI, ma il dominio resta responsabile del merge corretto.

### 3.12 Persistenza e backup

- database locale Room/SQLite;
- nessun account cloud Togo;
- Auto Backup Android abilitato esplicitamente;
- regole sia per Android 11 e precedenti (`fullBackupContent`) sia per Android 12+ (`dataExtractionRules`) dato `minSdk=26`;
- includere il dominio database necessario, non una lista manuale fragile di file SQLite ausiliari;
- escludere sempre segreti AI, blob cifrati non ripristinabili, cache e log temporanei;
- testare backup e restore end-to-end almeno una volta su build release-like.

Il core Togo non avvia rete per salvare/sincronizzare dati. **Auto Backup è però un servizio del sistema operativo**: se abilitato dall’utente/dispositivo, Android può trasferire i dati inclusi al servizio di backup configurato. Questa distinzione deve essere coerente nella privacy policy.

Il backup Android non è garanzia assoluta di disponibilità: dipende dalle impostazioni/condizioni del dispositivo. Prima di una pubblicazione ampia resta consigliabile valutare export/import manuale come v2.

### 3.13 Logging locale di troubleshooting

Log circolare locale, dimensione limitata, per eventi tecnici utili:

- timestamp;
- sorgente (`UI`, `APP_ACTION`, ecc.);
- tipo azione;
- esito sintetico;
- parametri **redatti/troncati**.

Non deve mai contenere:

- API key/token;
- stack trace con segreti;
- prompt completi se non necessari;
- dati inviati automaticamente all’esterno.

Nessun analytics/crash uploader di terze parti è richiesto nella v1.

### 3.14 Funzionalità rimandate

- liste multiple;
- widget home screen;
- condivisione multi-utente;
- sincronizzazione multi-dispositivo;
- multilingua;
- import/export JSON utente;
- voice input in-app alternativo;
- account/login Togo;
- OAuth Google come autenticazione Gemini;
- provider `CUSTOM` / endpoint OpenAI-compatible arbitrari;
- LLM gateway di terze parti nel percorso personale;
- backend Togo / `PUBLIC_MANAGED`, finché non esiste una decisione reale di distribuzione pubblica;
- persistenza dettagliata della provenienza AI per singolo `Product` oltre a `categorySource=AI` (provider/modello potranno essere aggiunti in futuro solo se serviranno per eval/audit).

### 3.15 Lista personale unificata e Tasks (REQUISITO v0.14)

**Documento di riferimento vincolante:** `togo-lista-unificata-tasks-v1.0.md`. La v1 finale non è solo una lista spesa: tutti gli elementi attivi sono presentati in un **unico feed**. Il filtro per tipo non comporta duplicazione di liste persistenti.

- `ItemKind.SHOPPING`: conserva `ListItem`, catalogo, quantità/unità e storico `PURCHASED`; il completamento è un acquisto.
- `ItemKind.TASK`: `TodoTask` con titolo libero, `priority NORMAL|URGENT`, `dueDate LocalDate?`, note e classificazione Task area/sottocategoria opzionale; gli stati sono `OPEN/COMPLETED/DELETED`. Il completamento non è un acquisto e `Riapri` riattiva **la stessa Task**, senza creare un evento acquisto.
- `TODO` non è una categoria L1 merceologica; la priorità `Urgente` non è una categoria T1/T2.
- Nessun merge automatico di Task omonime; la deduplica dei prodotti `normalizedName` resta indipendente.
- L'aggiunta di Tasks non richiede account, AI, rete né notifiche. Una scadenza non abilita automaticamente reminder.
- Le tabelle Tasks vanno aggiunte con migrazioni Room esplicite **senza distruggere i dati Spesa**. La proiezione UI aggrega Flow separati.
- La UI deve mantenere totale accessibilità di inserimento e completamento per entrambi i tipi.

### 3.16 Catalogo prodotti: normalizzazione dei nomi e migrazione controllata

**Input agentici:** `togo_catalog_review_v2.md`, `togo_catalogo_normalizzato_proposta_v2.md`, `togo_catalog_migration_mapping_v2.csv`, `togo_catalog_candidati_v2.md`.

**Regole:**

1. Non inserire nel canonico quantità/formato (`da 500 ml`, `in confezione da 6`, `Pack da 3`, `con 10 uova`, ecc.). L'utente specifica la quantità della **riga lista** nel form.
2. Conservare varianti significative (`Pane Integrale`, `Latte di Avena`, `Caffè in Capsule`), e neutrali (`Pane`, `Mela`, `Formaggio` quando sensato) come prodotti selezionabili.
3. Il mapping dell'export suggerisce **1.470 record originali → 748 nomi distinti** rimuovendo 722 suffissi, con **236 gruppi** di collasso caratterizzati da categorie originarie L3 divergenti. Questo **non** significa che il DB reale sia già pulito: la tassonomia necessita di riclassificazione per prodotto.
4. Non applicare il CSV alla cieca: prima ispezionare DB reale, FKs, seed loader, indici e dati utente, generare dry run ID-per-ID e backup; dopo approvazione, aggiornare riferimenti prima di eliminare i duplicati.
5. Non convertire un suffisso legacy `confezione da 6` in `ListItem.quantity=6` su acquisti storici senza prova di semantica equivalente; preservare la cronologia.
6. Aggiornare ogni L1/L2/L3 sulla base del prodotto e non della categoria di partenza, perché le categorie originarie sono miste. Considerare 9 L1 con `Surgelati e gelati` separati da `Latticini e uova` come **proposta**, non migrazione automatica.
7. L'export Markdown è un derivato: si aggiorna **il database sorgente**, poi si rigenera l'export. Non presentare una modifica di Markdown come migrazione dell'app.

### 3.17 Versioning e compatibilità con installazioni esistenti

La nuova lista unificata e la pulizia del catalogo sono cambiamenti **additivi/di migrazione**, non un motivo per resettare i dati del telefono. Distinguere schema Room per dati dell'utente da asset catalogo seed; preservare `Product`, `ListItem`, `PURCHASED`, statistiche, categorie manuali e corrispondenze anche se una variante di packaging viene collassata. Prevedere test di upgrade da snapshot precedente e backup/restore. La fase catalogo si esegue **prima** del consolidamento Tasks/AI, ma senza cancellare dati già realmente usati.

## 4. Flussi utente

### Flusso A — Aggiunta manuale offline

1. L’utente inserisce `latte`, quantità `2`, unità `l`.
2. Togo normalizza e cerca il prodotto.
3. Se il prodotto esiste, conserva la sua categoria.
4. Se è nuovo, tenta apprendimento manuale, mappa statica e infine `Altro` se AI non attiva.
5. Cerca una riga attiva compatibile e la incrementa oppure ne crea una nuova.

### Flusso B — Aggiunta con AI personale

1. L’utente configura un provider implementato (`GEMINI`, `OPENAI` o `ANTHROPIC`), inserisce la propria credenziale API e seleziona un modello compatibile.
2. Imposta quel provider come attivo.
3. Per un input che richiede parsing AI, `AiGateway` delega all’adapter attivo e riceve il risultato di dominio strutturato.
4. Togo valida semanticamente tutti i campi.
5. Per un nuovo prodotto, la categoria AI viene salvata con `categorySource=AI`.
6. Per un prodotto già noto, la categoria persistita non viene sovrascritta.

### Flusso C — Cambio provider

1. Gemini e OpenAI sono entrambi configurati con credenziali separate.
2. L’utente cambia provider attivo da Gemini a OpenAI.
3. Togo mantiene catalogo, lista, categorie e storico invariati.
4. Le nuove richieste AI usano il modello OpenAI selezionato; la credenziale Gemini resta conservata e non viene inviata a OpenAI.

### Flusso D — AI fallita

1. Una chiamata fallisce per rete, quota, modello, autorizzazione o output invalido.
2. L’app passa al parser/categorizzatore deterministico appropriato.
3. Nessun crash.
4. Le impostazioni mostrano provider/modello e ultimo esito sintetico come fallback.

### Flusso E — Due aggiunte compatibili

1. `latte`, `1`, `l`, nessuna nota è già `TO_BUY`.
2. L’utente aggiunge `latte`, `2`, `l`, nessuna nota.
3. La riga esistente diventa quantità `3`.

### Flusso F — Stesso prodotto, unità incompatibili

1. `latte`, `1`, `l` è `TO_BUY`.
2. L’utente aggiunge `latte`, `2`, `confezioni`.
3. Viene creata una seconda riga `TO_BUY`; le quantità non vengono sommate.

### Flusso G — Acquisto e “Aggiungi di nuovo”

1. `latte` passa a `PURCHASED`.
2. Giorni dopo l’utente seleziona “Aggiungi di nuovo”.
3. La vecchia riga resta `PURCHASED`.
4. Una nuova riga `TO_BUY` viene creata o fusa con una compatibile.
5. Le statistiche storiche conservano il precedente acquisto.

### Flusso H — Ripristino di una voce eliminata

1. Una voce `DELETED` è visibile nella Cronologia.
2. L’utente preme “Ripristina”.
3. Se non c’è una riga attiva compatibile, torna `TO_BUY`.
4. Se c’è, la quantità viene sommata alla riga attiva e la riga eliminata resta `DELETED`.

### Flusso I — Miglioramento della mappa offline

1. Dopo uso personale, il catalogo contiene categorie `MANUAL`, `AI`, `LEARNED`.
2. Si esportano nome, categoria **e source**.
3. Si revisionano manualmente conflitti/errori.
4. Solo dati approvati entrano in una nuova versione di `category_map.json`.

### Flusso J — App Actions, solo dopo gate positivo

1. Si verifica un locale/BII realmente supportato.
2. Una build Play di test contiene `shortcuts.xml`.
3. Assistant invia un intent/deep link.
4. Togo valida i parametri e li passa allo stesso dominio usato dalla UI.
5. Azioni distruttive aprono solo la UI di conferma.

---

### Flusso K — Lista unificata (Spesa + Da fare)

1. L'utente aggiunge `Pane` come `SHOPPING`, quantità 2, e `Lavare casa` come `TASK`, priorità urgente.
2. La home `Tutti` mostra entrambi, con tipo riconoscibile.
3. Il filtro `Spesa` mostra solo pane e `Da fare` solo lavare casa, senza creare liste distinte.
4. Spuntare pane crea evento `PURCHASED`; spuntare la Task porta la stessa attività a `COMPLETED`.
5. La cronologia mostra entrambi i completamenti senza confonderli. La Task può essere riaperta; pane può essere aggiunto di nuovo preservando lo storico.

### Flusso L — Nome canonico prodotto senza confezione

1. L'utente cerca `Latte di Avena` in un catalogo corretto.
2. Compare un solo canonico `Latte di Avena`, **non** una variante `in confezione da 6`.
3. L'utente inserisce quantità 6 e unità desiderata in UI; la riga shopping porta la quantità, non il nome prodotto.
4. La ricerca esatta e lo storico prodotti rimangono coerenti.

### Flusso M — Migrazione da catalogo e app shopping-only

1. L'agente verifica il DB reale e produce mapping ID/collisioni, senza alterarlo nella prima iterazione.
2. Applica una migrazione controllata che preserva gli ID collegati allo storico e normalizza i canonici senza suffissi di quantità.
3. Aggiunge le tabelle Task in migrazione non distruttiva.
4. Verifica che gli acquisti preesistenti e il nuovo feed unificato siano corretti; rigenera `catalogo.md` dal DB.

## 5. Requisiti tecnici

| Area | Decisione v1 |
|---|---|
| Linguaggio | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architettura UI | MVVM; un `StateFlow<UiState>` per ViewModel |
| DI | Hilt, usato in modo coerente |
| Persistenza | Room/SQLite; tabelle Tasks separate da Shopping e migrazioni esplicite |
| Home unica | `UnifiedListRepository` + proiezione `UnifiedListEntry` (S:/T:), Flow combinati; filtri per tipo |
| Catalogo prodotti | nomi canonici senza suffissi formato/pack; migrazione asset seed controllata per ID |
| Asincronia | Kotlin Coroutines + Flow |
| Preferenze non sensibili | DataStore Preferences |
| Segreti personali | Android Keystore + cifratura autenticata; storage escluso da backup |
| JSON | `kotlinx.serialization` dove adatto |
| AI | `AiGateway` + `AiProviderAdapter`/registry; provider personali Gemini/OpenAI/Anthropic; `PUBLIC_MANAGED` o AI off per release pubblica |
| Voice | App Actions solo dopo feasibility gate; `shortcuts.xml` |
| `minSdk` | 26 |
| `targetSdk` | 36 come baseline verificata al 15/09/2026 |
| `compileSdk` | stabile compatibile con `targetSdk`; evitare preview come requisito base |
| Moduli Gradle | singolo modulo `app` per v1 salvo necessità reale |
| Build | Gradle Kotlin DSL; Version Catalog consigliato |

### 5.1 Confini architetturali minimi

Per evitare accoppiamento con API volatili, sono richieste interfacce piccole e testabili:

- `TogoRepository` / use case transazionali per catalogo e lista Spesa;
- `TodoRepository` / use case stato Task e `UnifiedListRepository` per osservare entrambi i domini;
- `CategoryResolver`;
- `OfflineVoiceParser`;
- `AiGateway`;
- `AiProviderAdapter` e `AiProviderRegistry`;
- `AiSettingsRepository` per provider/modelli non sensibili;
- `SecretStore` namespaced per provider;
- `Clock` iniettato per timestamp deterministici nei test.

Non è richiesto introdurre Clean Architecture multi-modulo, event bus, CQRS, backend o framework aggiuntivi.

### 5.2 Migrazioni Room

`fallbackToDestructiveMigration()` è accettabile solo nelle primissime iterazioni **prima che esistano dati personali da preservare**. Dal momento in cui Togo viene usato realmente, ogni modifica schema deve avere migrazione esplicita e testata. **L’aggiunta di Tasks e la normalizzazione del catalogo NON autorizzano un reset**. Testare snapshot di versioni precedenti e distinguere database Room utente da `catalog.db` seed.

---

## 6. Requisiti non funzionali

| Area | Requisito |
|---|---|
| Offline | Tutte le funzioni core, incluse Tasks, devono funzionare senza rete e senza AI |
| Unificazione | Un solo feed, filtri Spesa/Da fare; completamenti e metriche separati |
| Integrità catalogo | Nome canonico indipendente da confezioni/quantità; nessuna perdita FK/storico nella deduplica |
| Consistenza | Add/merge/restore/clear devono essere transazionali; race concorrenti non devono duplicare `Product` |
| Prestazioni | Dataset personale: query indicizzate e UI reattiva; nessun lavoro DB/rete sul main thread |
| Resilienza | Errori dei provider AI/voice non devono compromettere il core né causare crash |
| Privacy | Nessuna rete applicativa avviata da Togo nel core offline; resta possibile il backup gestito dal sistema operativo secondo le impostazioni utente. AI solo dopo attivazione esplicita |
| Sicurezza | Nessun segreto in sorgente/log/backup; deep link e intent sono input non fidato |
| Backup | Backup dati non sensibili configurato e testato; segreti esclusi |
| Accessibilità | Supportare semantics/label Compose di base e dimensioni touch corrette; non è richiesta certificazione dedicata v1 |
| Localizzazione | UI italiana v1; stringhe in resources, non hardcoded |
| Osservabilità | log locale bounded e redatto; nessun telemetria remota necessaria |
| Testabilità | clock/network/AI sostituibili con fake; business logic testabile senza device |

---

## 7. Vincoli, rischi e gate

| # | Punto | Impatto | Mitigazione/stato |
|---|---|---|---|
| 1 | App Actions non supportano oggi l’esperienza italiana prevista | **Alto** | Voice Assistant non è criterio v1; PoC separato prima dell’implementazione completa |
| 2 | App Actions richiedono distribuzione Google Play e review separata | Alto | Non basare l’uso personale sideloaded su questa feature |
| 3 | Quantità nei payload Assistant non garantita come campo strutturato | Medio | Verificare payload reale; parser lavora solo sul testo effettivamente ricevuto |
| 4 | API, modelli e formati AI evolvono in modo diverso per vendor | Medio | `AiProviderAdapter`; verifica docs corrente per ogni adapter; contract test comuni |
| 5 | Chiavi API statiche sul client non sono un percorso production sicuro | **Alto** | `PERSONAL_BYOK` solo personale/dogfood; `PUBLIC_MANAGED` o AI off nella build pubblica |
| 6 | Credenziali di provider diversi possono essere confuse/inviate al vendor sbagliato | Alto | `SecretStore` namespaced + adapter selezionato tipizzato + test di isolamento credenziali |
| 7 | Il model listing non espone capability uniformi tra provider | Medio | `listCompatibleModels` provider-specifico; capability API dove disponibili, policy centralizzata altrove |
| 8 | Structured output differisce tra Gemini/OpenAI/Anthropic | Medio | Schema di dominio comune, mapping nativo nell’adapter, validazione locale obbligatoria |
| 9 | Consumer subscription e API billing sono concetti separati | Medio UX/costi | UI esplicita: BYOK usa quota/billing API del provider; nessuna promessa legata a ChatGPT/Claude/Gemini consumer |
| 10 | OAuth Gemini è possibile ma introduce Cloud project/consent/token lifecycle senza eliminare quota API | Medio | Fuori scope v1; nessun pulsante “Login Google = Gemini incluso” |
| 11 | `EncryptedSharedPreferences` deprecato | Medio | Android Keystore + storage cifrato locale escluso dal backup |
| 12 | Categorizzazione per prefisso può produrre falsi positivi | Medio | Solo source MANUAL + regole conservative + test |
| 13 | Matching singolare/plurale euristico | Basso/medio | comparisonKey conservativa; match solo se univoco |
| 14 | Merge cieco di quantità con unità equivalenti ma scritte diversamente o semanticamente incompatibili | Medio | `unitComparisonKey` per alias noti + note compatibili; nessun fuzzy matching libero |
| 15 | Concorrenza UI/deep link può duplicare prodotti | Medio | UNIQUE + transazioni + retry su conflict |
| 16 | Backup Android non è garantito su ogni device/configurazione | Medio | Test + futura export/import v2 |
| 17 | Nuovi account Play personali possono richiedere closed testing | Alto per pubblicazione | Verificare policy corrente prima del lancio pubblico |
| 18 | Limited distribution non equivale al Play Store | Medio | Usarla solo per personale/piccolo gruppo |
| 19 | Policy Play su dati/AI e privacy variano | Medio | Privacy policy, Data Safety e disclosure verificate sulla build finale |
| 20 | Modello selezionato può essere ritirato | Medio | Nessuno switch silenzioso; fallback offline + nuova selezione |
| 21 | `PUBLIC_MANAGED` sposta costi/quota/abuse prevention sul progetto Togo | Medio/alto | Implementarlo solo con business/release decision esplicita, rate limits e budget |
| 22 | La main screen può crescere indefinitamente se mostra tutti i PURCHASED | Medio UX/perf | Main limita gli acquisti recenti; Cronologia contiene lo storico completo |
| 23 | Developer verification Android evolve nel tempo | Medio | Riesaminare la modalità di distribuzione al momento del rilascio |

### 7.1 Gate Voice

Prima di implementare la Fase App Actions completa devono essere veri tutti i seguenti punti:

- BII corrente ancora disponibile;
- locale target realmente supportato;
- payload osservato contiene dati sufficienti al caso d’uso;
- preview/test release funzionante su device reale;
- percorso di distribuzione Play accettabile.

Se uno di questi fallisce, si chiude il gate come `BLOCKED_BY_PLATFORM` e il core continua senza workaround fragili.

### 7.2 Gate AI per pubblicazione

Prima di rendere pubblica una build con AI abilitata:

- nessuna credenziale Gemini/OpenAI/Anthropic statica dello sviluppatore distribuita nel client;
- decidere se l’AI pubblica sarà single-provider o multi-provider;
- per multi-provider: percorso `PUBLIC_MANAGED` con backend/gateway controllato, autenticazione/attestazione client, rate limit, quota e budget;
- per Gemini-only può essere valutato Firebase AI Logic + App Check secondo documentazione corrente;
- privacy policy e Data Safety aggiornate;
- disclosure/consenso appropriati per i dati inviati al provider;
- modello/configurazione production stabile e monitorabile.

In alternativa: build pubblica con AI disabilitata.

## 8. Criteri di accettazione

| Scenario | Dato che | Quando | Allora |
|---|---|---|---|
| Nuovo elemento offline | nessun Product `zucchero` | aggiungo `zucchero` | Product e ListItem TO_BUY vengono creati senza rete |
| Product unico sotto concorrenza | due richieste concorrenti per `latte` | arrivano quasi insieme | esiste un solo Product con `normalizedName=latte` |
| Merge compatibile | `latte 1 l` TO_BUY | aggiungo `latte 2 l` | quantità totale `3`, una riga compatibile |
| Merge incompatibile | `latte 1 l` TO_BUY | aggiungo `latte 2 confezioni` | due righe TO_BUY, nessuna somma semanticamente errata |
| Note incompatibili | `pasta 1 kg, nota marca A` | aggiungo `pasta 1 kg, marca B` | due righe distinte |
| Acquisto | riga TO_BUY | segno comprato | diventa PURCHASED e contribuisce allo storico |
| Riaggiunta da acquistato | riga PURCHASED esistente | “Aggiungi di nuovo” | vecchia riga resta PURCHASED; nuova/merge TO_BUY |
| Ripristino eliminato | riga DELETED | “Ripristina” | torna TO_BUY o viene assorbita da compatibile attiva |
| Purge | esistono PURCHASED e DELETED | confermo pulizia cronologia | solo DELETED vengono cancellate fisicamente |
| Manual category source | utente modifica categoria | salva | Product usa categoria scelta e `MANUAL` |
| AI non sovrascrive catalogo | Product noto con categoria MANUAL | AI propone altra categoria | categoria persistita resta invariata |
| AI source su nuovo prodotto | chiamata combinata AI valida | crea nuovo Product | `categorySource=AI`, mai `MANUAL` |
| Solo MANUAL insegna | esiste candidato LEARNED | aggiungo nome con quel prefisso | il LEARNED non viene usato come sorgente di nuova propagazione |
| Prefisso positivo | `zucchero` MANUAL | aggiungo `zucchero di canna` | può ereditare categoria come LEARNED |
| Prefisso falso positivo | `latte` MANUAL=Latticini | aggiungo `latte detergente` | non eredita automaticamente Latticini |
| Static map prima della rete | mappa contiene match esatto e AI attiva | aggiungo manualmente prodotto | nessuna chiamata AI necessaria per la categoria |
| Categoria custom | Product contiene categoria manuale nuova | si costruisce enum AI | categoria custom e `Altro` sono incluse |
| Parser decimale | input `1,5 kg di pasta` | parser offline | `1.5`, `kg`, `pasta` |
| Parser senza unità | input `2 mele` | parser offline | `2`, unit null, `mele` |
| Input invalido | nome blank o quantità `<=0`/NaN/∞ | salvo | operazione rifiutata senza scrivere dati |
| Provider configurato | Gemini e OpenAI configurati | cambio provider attivo | solo le nuove chiamate AI cambiano provider; dati dominio invariati |
| Isolamento credenziali | due provider hanno credenziali diverse | chiamo OpenAI | nessuna credenziale Gemini viene letta/inviata all’adapter OpenAI |
| Modello incompatibile | model listing include modello non adatto a structured output | apro selezione modello | il modello non è selezionabile come compatibile Togo |
| Provider non configurato | provider selezionato non ha credenziale valida | provo una funzione AI | fallback offline e stato configurazione chiaro, nessun crash |
| Consumer subscription | utente ha un abbonamento consumer ma nessuna API credential | abilita AI | Togo non assume quota API e richiede configurazione API esplicita |
| AI assente | modalità OFF | aggiungo prodotto | nessuna chiamata di rete |
| AI fallisce | modalità personale ma rete/quota/model error | aggiungo | fallback deterministico, nessun crash |
| Modello ritirato | modello selezionato non disponibile | richiesta AI | fallback + stato errore, nessun cambio modello silenzioso |
| Segreto e backup | chiave personale configurata | eseguo backup/restore | DB può ripristinarsi; segreto non viene ripristinato dal backup |
| Deep link ostile | intent contiene parametri vuoti/enormi/malformati | router riceve intent | input validato/rifiutato; nessun crash/azione distruttiva |
| Clear via deep link | deep link apre pulizia | viene ricevuto | compare conferma; nessuna cancellazione automatica |
| Voice italiano | locale `it-IT` e supporto piattaforma invariato | si esegue test | il mancato supporto non fa fallire la v1 core; feature resta blocked |
| Home unificata | sono presenti pane TO_BUY e lavare casa OPEN | apro la home Tutti | vedo entrambe le righe con tipo riconoscibile |
| Filtri tipo | esistono SHOPPING e TASK | seleziono Da fare | la vista filtra senza creare una seconda lista o cancellare elementi |
| Task offline | AI OFF e rete scollegata | aggiungo una Task libera | viene salvata OPEN senza chiamate remote |
| Task duplicata | esiste Task Lavare casa | aggiungo altra Task omonima | esistono due Task; nessun merge |
| Riapri Task | Task COMPLETED | clicco Riapri | stessa Task OPEN e completedAt null; nessun PURCHASED |
| Date Tasks | Task dueDate impostata a 2026-11-02 | cambio fuso | dueDate invariata come LocalDate |
| Priorità | Task Casa/Pulizie NORMAL | imposto URGENT | categoria non cambia; Task ordinata fra urgenti |
| Nome prodotto pulito | canonico Latte di Avena in confezione da 6 | migrazione approvata | un canonico Latte di Avena, vecchi riferimenti rimappati |
| Quantità separata | canonico Formaggio Spalmabile | aggiungo quantità 6 | `ListItem.quantity=6`, nome prodotto invariato |
| Migrazione catalogo | snapshot catalogo 1470 record e storico | eseguo dry-run/apply test | 748 canonici proposti senza perdere riferimenti, storico o categorie corrette |
| Svuota lista | SHOPPING e TASK attivi | confermo Svuota | entrambe DELETED, purchase/task COMPLETED storici preservati |

---

## 9. Strategia di test minima

### Unit test

- normalizzazione e `comparisonKey`;
- parser offline;
- compatibilità merge unit/note;
- `CategoryResolver` e precedenze;
- contract test comuni `AiProviderAdapter`;
- mapping errori provider → tassonomia interna;
- validazione output AI;
- isolamento credenziali/provider;
- selezione modello compatibile;
- canonicalizzazione categorie;
- ordinamento suggerimenti;
- sorting/filtri `UnifiedListEntry`, conflitti chiavi S:/T:, priorità Task;
- parser/normalizer dei suffissi di confezione (non rimuove `Farina 00` né semantica prodotto).

### Room/instrumented test

- CRUD e indici;
- UNIQUE su `normalizedName`;
- race/conflitto simulato su creazione Product;
- transazioni add/merge/restore/clear;
- statistiche preservate da “Aggiungi di nuovo”;
- purge solo DELETED;
- migrazioni schema;
- migrazione Tasks non distruttiva e `completedAt` coerente con stato;
- asset catalogo: mapping 1470→748 proposto, controllo foreign key, idempotenza e mantenimento storico (solo dopo verifica del DB reale).

### UI test

- aggiunta;
- errori quantità;
- acquisto/elimina;
- cronologia, ripristino e riaggiunta;
- conferme distruttive;
- configurazione provider multipli, cambio provider e model selector;
- stato AI loading/error;
- rotazione/process recreation sui principali UiState dove applicabile;
- composer Spesa/Da fare, task category/priority/dueDate, filtri unificati, task reopen.

### Integration/manual test

- backup/restore;
- SecretStore dopo restore senza chiave Keystore;
- credential validation/model discovery per ogni adapter implementato, con account di test manuale e senza chiavi in CI;
- modalità completamente offline;
- App Actions/deep link solo se gate voice aperto;
- test release-like con minSdk e targetSdk supportati.

---

## 10. Pubblicazione e compliance

Prima di una beta/pubblicazione Google Play:

- `targetSdk` conforme ai requisiti Play correnti (baseline v0.13: API 36);
- migrazioni Room esplicite;
- privacy policy raggiungibile anche dall’app;
- sezione Data Safety coerente con il comportamento reale e con eventuali provider/SDK/API AI;
- informativa/consenso dove richiesto per invio di testo/trascrizioni a terze parti AI;
- AI pubblica solo tramite percorso `PUBLIC_MANAGED`/provider-safe equivalente oppure disabilitata;
- nessun secret/log sensibile nel bundle;
- test closed-track richiesto dall’account completato, se applicabile;
- review separata App Actions solo se il gate voice è aperto e la feature viene effettivamente distribuita.

Per account personali Play creati dopo il 13 novembre 2023, la regola corrente prevede almeno 12 tester opt-in continuativi per 14 giorni prima di poter richiedere l’accesso alla produzione.

La “Limited distribution” dell’Android Developer Console, disponibile da agosto 2026, è gratuita e permette la condivisione con un massimo di 20 dispositivi autorizzati; è adatta a hobby/personale ma non è una pubblicazione Google Play.

---

## 11. Decisioni esplicite per evitare overengineering

Per la v1 **non** implementare senza un requisito nuovo:

- backend proprietario / `PUBLIC_MANAGED` prima che esista un requisito di rilascio;
- account/login Togo;
- sync cloud;
- multi-modulo Gradle complesso;
- repository remoti se non servono all’AI;
- event sourcing/CQRS;
- database di training separato;
- modelli AI on-device;
- sinonimizzazione NLP complessa;
- algoritmi fuzzy generici;
- telemetria remota;
- OAuth Google per Gemini nella v1;
- provider custom/OpenAI-compatible arbitrari;
- gateway LLM esterni nel percorso personale;
- supporto App Actions italiano tramite workaround non documentati.

L’obiettivo è un’app locale piccola, robusta e testabile, con punti di estensione soltanto dove le dipendenze esterne sono realmente volatili (`AiGateway`/provider adapters, input vocale, storage segreti).

---

## 12. Prossimi passi

1. Implementare il core locale Spesa fino a UI + storico senza AI né App Actions; poi eseguire la **Fase catalogo** con dry run/migrazione e la **Fase Tasks** per ottenere una lista unica Spesa + Da fare. Non dichiarare completa la v1 aggiornata prima delle due fasi.
2. Usare l’app realmente per alcuni cicli di spesa e stabilizzare modello dati/UX.
3. Implementare il contratto provider-agnostic e **Gemini come primo adapter** `PERSONAL_BYOK`.
4. Dogfoodare Gemini e stabilizzare contract/error handling prima di duplicare integrazioni.
5. Aggiungere `OpenAiProviderAdapter`, poi `AnthropicProviderAdapter`, facendo passare gli stessi contract test e senza modificare il dominio.
6. Eseguire il gate App Actions separatamente; non far dipendere il progetto dal suo esito.
7. Prima di una distribuzione pubblica, decidere se:
   - pubblicare senza AI; oppure
   - introdurre `PUBLIC_MANAGED` (o una soluzione provider-specifica sicura) e completare privacy/Data Safety/abuse protection.
8. Non implementare OAuth Google nella v1; rivalutarlo solo se emerge un caso d’uso che giustifica progetto Cloud, consenso e lifecycle token.
9. Solo quando il modello dati viene usato con dati reali, vietare definitivamente destructive migrations.
