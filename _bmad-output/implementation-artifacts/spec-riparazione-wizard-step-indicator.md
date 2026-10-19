---
title: 'Riparazione indicatore step wizard AddItemBottomSheet — connettori e spaziatura'
type: 'bugfix'
created: '2026-10-19'
status: 'ready'
route: 'dispatch'
baseline_commit: '483ee59c5122ba9e24acc9c551f7769462f4051b'
review_loop_iteration: 0
context:
  - 'Bug re-confermato dall''utente il 2026-10-19: il blocco indicatore (righe 155-200) è risultato INVARIATO rispetto al baseline — la fix non è mai stata applicata.'
  - 'PREREQUISITO AVVIO: se l''app è ancora in crash-loop al boot (vedi spec-riparazione-koin-bind-gateway.md, bind Koin gateway vocale), la verifica visiva V5 è impossibile finché quell''altra fix non è buildata. La correzione del presente script può comunque essere applicata in anticipo.'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'indicatore delle 4 fasi del wizard "Aggiungi voce" (sotto il titolo "Cerca prodotto") presenta: (a) un **trattino blu a sinistra del "1"** che non collega nulla, (b) **nessun connettore tra 3 e 4** (cerchietti3 e4 raggruppati), (c) **spaziatura ineguale** tra gli step. Causa: la linea è disegnata *prima* del cerchio dentro slot di peso uguale e l'ultimo step non ha linea. Non è un crash: bug visivo del solo indicatore in `AddItemBottomSheet.kt`.

**Approach:** Ribaltare l'ordine dentro ogni slot (cerchio, poi linea con `weight(1f)` presente tranne per l'ultimo step) e aggiustare la condizione di colore della linea (evidenziata solo quando lo step a destra è stato raggiunto). Verificare staticamente e **chiedere all'utente di eseguire build e install** — l'agente NON esegue comandi di build.

**Always:** Modificare **solo** `app/src/main/java/it/togo/app/presentation/additem/AddItemBottomSheet.kt` (blocco indicatore, righe ~155-200); preservare la modalità edit (startStep=2, 3 cerchietti) e la logica numeri/step; verificare che non resti nessuna linea "orfan" prima del primo cerchio.

**Never:** **Eseguire comandi di build, install o test Gradle/Docker** (elenco in sezione dedicata — chiedere sempre all'utente); committare; toccare i file già modificati nel working tree (`AndroidManifest.xml`, `ActiveListScreen.kt`, `CategoryHeader.kt`, `ItemRow.kt`, `PresiSection.kt`) — non staccarli, non modificarli, non revertarli; toccare `ProductCategorizationWizard.kt` (indicatore diverso, senza connettori, non affetto); modificare token/valori colore in `ui/theme/*`.

</frozen-after-approval>

## Evidence (bug visivo — nessun log di crash)

Screenshot utente 2026-10-19 ore 11:57, bottom sheet "Cerca prodotto" (step 1):

- Trattino **blu** visibile a sinistra del cerchio "1" (`accentHighlight`) — connettore senza cerchio precedente.
- Tra "2" e "3" il connettore grigio è poco/invisibile, tra "3" e "4" **non c'è alcun connettore**: "3 4" appaiono attaccate.
- Complessivamente: spazi tra cerchi disuguali (lunghi1→2 e2→3, minimo3→4).

Riproducibile in qualsiasi momento aprendo il FAB "+".

**Re-verifica sorgente 2026-10-19 (prima di questa esecuzione):** `grep`/lettura del blocco righe 155-200 di `AddItemBottomSheet.kt` → struttura **identica** a quella descritta in Root cause (linea prima del cerchio, `if (step < 4)`, colore `>= step`); il file non ha modifiche non committate (`git status` pulito per quel path, ultimo commit `483ee59`).

## Root cause (da verificare prima di correggere — `AddItemBottomSheet.kt`)

1. **Righe 165-199** — per ogni step `n` (da `startStep` a 4) crea una `Row(weight(1f))` contenente:
   - **righe 175-182**: la linea `Box(weight(1f), height(2.dp))` disegnata **PRIMA** del cerchio, con condizione `if (step < 4)`;
   - **righe 184-197**: il cerchio `Box(size(24.dp))` con numero `(step - startStep + 1)`.
2. Conseguenze strutturali:
   - Lo step 1 produce una linea **prima** del suo cerchio → dash iniziale (blu a `currentStep >= 1`, riga 180).
   - Lo step 4 non ha linea → **manca il connettore 3→4**.
   - Nel slot dello step 4 il cerchio è all'inizio e il resto del peso è vuoto → **gap3→4 minimo** rispetto agli altri.
3. La condizione di colore (riga 180) `uiState.currentStep >= step` è calibrata sulla posizione linea-prima-cerchio: spostata la linea **dopo** il cerchio, diventa semanticamente corretta `uiState.currentStep > step` (la linea n→n+1 si accende quando lo step n+1 è raggiunto).
4. (Stato) Le `val` `isActive`/`isCurrent`/`effectiveIndex` (righe 166-168) sembrano inutilizzate nel blocco: verificare con grep e rimuoverle solo se confermatamente dead.

## Code Map

- `app/src/main/java/it/togo/app/presentation/additem/AddItemBottomSheet.kt` -- **unico file da modificare**: blocco indicatore righe ~155-200 (`Row` con `totalSteps`/`startStep`, `forEach`, Box linea/cerchio).
- `app/src/main/java/it/togo/app/presentation/additem/ProductCategorizationWizard.kt` -- indicatore diverso (label + `SpaceEvenly`, nessun connettore): **non toccare**, è fuori scope.
- `app/src/main/java/it/togo/app/presentation/additem/AddItemViewModel.kt` -- logica `currentStep` 1..4 e modalità edit (partenza step 2): sola lettura, non modificare.

## Tasks & Acceptance

**Execution:**

- [ ] **V1 — Conferma diagnosi (obbligatoria):**
  - [ ] Verificare per contenuto il blocco righe ~155-200: linea prima del cerchio, `if (step < 4)`, condizione colore `>= step`.
  - [ ] Verificare che `ProductCategorizationWizard.kt` usi un indicatore senza connettori (conferma fuori scope).
  - [ ] Verificare con `git status` che i file utente citati risultano modificati e vanno lasciati intatti.
  - [ ] Se i punti non combacciano → **non applicare la fix**: raccogliere il contrasto e riportarlo all'utente.
- [ ] **V2 — Fix (solo `AddItemBottomSheet.kt`, blocco indicatore):**
  - [ ] Ribaltare l'ordine nello `Row` di ogni step: **prima il Box cerchio (24dp), poi il Box linea** (`weight(1f)`, `height(2.dp)`) con `if (step < 4)`.
  - [ ] Condizione colore linea: `if (uiState.currentStep > step) colors.accentHighlight else colors.borderHairline`.
  - [ ] Contenuto del cerchio invariato (numero `(step - startStep + 1)`, colori e `CircleShape` come da codice attuale).
  - [ ] Rimuovere le `val` `isActive`/`isCurrent`/`effectiveIndex` **solo se grep conferma zero usi** nel file.
  - [ ] Non toccare titolo step, pulsanti Avanti/Indietro, `totalSteps`/`startStep`, SearchStep/QuantityUnitStep.
- [ ] **V3 — Verifica statica (senza build):**
  - [ ] Nel blocco indicatore: nessuna `Box` linea prima di una `Box` cerchio; un'unica linea per coppia di cerchi adiacenti.
  - [ ] `git diff` mostra **solo** `AddItemBottomSheet.kt` (le modifiche utente preesistenti restano invariate).
- [ ] **V4 — HANDOFF BUILD (obbligatorio, fermarsi qui):**
  - [ ] Comunicare all'utente: *"Fix applicata al solo AddItemBottomSheet.kt. Esegui tu la build e l'installazione con: `powershell -ExecutionPolicy Bypass -File .\build-install-android.ps1`"*
  - [ ] **Attendere** la conferma dell'utente. Non procedere in autonomia.
- [ ] **V5 — Verifica visiva post-build (SOLO dopo la conferma dell'utente):**
  - [ ] **Guardia**: se l'app non si avvia (crash-loop noto da `NoBeanDefFoundException` Koin, vedi `spec-riparazione-koin-bind-gateway.md`), la fix del presente script è comunque applicata correttamente: segnalare all'utente che la verifica visiva è bloccata dall'altra issue e che va risolta/prima buildata quella.
  - [ ] Aprire il FAB "+" → indicatore deve mostrare `1 ——— 2 ——— 3 ——— 4`: **nessun tratteggio prima del "1"**, connettore presente tra **ogni** coppia, spazi uniformi.
  - [ ] Allo step 1: connettori tutti grigli (`borderHairline`).
  - [ ] Selezionare un prodotto → allo step 2: connettore 1→2 blu (`accentHighlight`), 2→3 e 3→4 grigli.
  - [ ] Regressione modalità edit (se raggiungibile dalla UI: tapp su una voce esistente): 3 cerchietti numerati 1-2-3 con stesse regole, nessuna linea iniziale.
  - [ ] Regressione funzionale: Avanti/Indietro e completamento wizard invariati.

**Acceptance (code, tutte obbligatorie):**

1. Indicatore: zero linee prima del primo cerchio; linea dopo ogni cerchio tranne l'ultimo; colore `accentHighlight` solo per linee la cui coppia destra è raggiunta (`currentStep > step`).
2. Modalità edit (`isEditMode`, `startStep = 2`) renderizza 3 cerchietti con le stesse regole.
3. Diff limitato a `AddItemBottomSheet.kt`; nessun commit; file utente preesistenti toccati solo da lui.
4. Build, install e verifica visiva **eseguite dall'utente**; l'agente le registra solo su sua conferma.

**Comandi VIETATI all'agente (chiedere sempre all'utente):**

```text
./gradlew*, gradlew.bat*, gradle build*, gradle assemble*, gradle test*
powershell -ExecutionPolicy Bypass -File .\build-android.ps1
powershell -ExecutionPolicy Bypass -File .\build-install-android.ps1
powershell -ExecutionPolicy Bypass -File .\test-android.ps1
docker build*, docker run*
adb install*, adb uninstall*, adb shell pm *
qualsiasi comando che compili, pacchetti o installi l'APK
```

Consentiti (sola lettura): `adb devices`, `adb logcat ...` (lette), `git status/diff/log` (lette), `grep`/`read` sui sorgenti.

**Escalation (segnalare all'utente invece di forzare):**

- Differenza di spaziatura residua accettabile ma l'utente vuole perfetta uguaglianza → valutare con lui la rimozione di `spacedBy` esterno/interno (cambio di layout più ampio).
- Richiesta di allineare anche l'indicatore di `ProductCategorizationWizard.kt` → nuovo scope, attendere conferma.
- Necessità di toccare file utente o token colore → fermarsi e chiedere.
