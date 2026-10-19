---
title: 'Riparazione ItemRow — righe spuntate: bleed verde swipe e testo illeggibile'
type: 'bugfix'
created: '2026-10-19'
status: 'ready'
route: 'dispatch'
baseline_commit: '483ee59c5122ba9e24acc9c551f7769462f4051b'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Le righe spuntate (sezione "Presi") appaiono con **sfondo verde** e **testo illeggibile**. Non è un crash: è un bug visivo. La `Surface` della riga applica un'opacità del 45% allo sfondo per le righe spuntate, quindi traspare il verde del retro "swipe-to-delete" (accentSuccess) che dovrebbe vedersi solo durante lo swipe; contemporaneamente il testo diventa `inkMuted` al 45% → contrasto prossimo a zero. Anche il badge quantità risulta sbiadito (testo inkMuted su sfondo surfaceSubtle).

**Approach:** Rimuovere l'uso di `alpha` sui colori di sfondo e testo mantenendo la distinguibilità dello stato checked **opaca** (bordo verde e checkbox già gestiti da token dedicati, testo barrato già presente), aggiornare la KDoc, verificare staticamente e **chiedere all'utente di eseguire build e install** — l'agente NON esegue comandi di build.

**Always:** Modificare **solo** `app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt`; mantenere bordi/checkbox verdi già funzionanti; verificare che la swipe-to-delete continui a rivelare il verde solo durante il trascinamento.

**Never:** **Eseguire comandi di build, install o test Gradle/Docker** (elenco in sezione dedicata — chiedere sempre all'utente); committare; toccare i file già modificati dall'utente nel working tree (`AndroidManifest.xml`, `ActiveListScreen.kt`, `CategoryHeader.kt`, `PresiSection.kt`) — non staccarli, non modificarli, non revertarli; modificare token/valori colore in `ui/theme/*`; cambiare disposizione o semantica TalkBack della riga.

</frozen-after-approval>

## Evidence (bug visivo — nessun log di crash)

Riproducibile sul dispositivo: spuntare uno o più elementi → sezione "Presi". Osservato il 2026-10-19 su APK build 10:58 (screenshot utente):

- 3 righe "Presi" con **sfondo verde pieno** (verde identico al colori `accentSuccess` usato dalla swipe-to-delete).
- **Nome prodotto invisibile** (grigio fantasma sul verde).
- Badge "1 pz" sbiadito, quasi illeggibile.
- Checkbox spuntata e bordo della riga **visibili correttamente** (verde): conferma che il problema è limitato a sfondo+testo.

## Root cause (da verificare prima di correggere — `ItemRow.kt`)

1. **Riga 123** — il `Box` esterno ha `background(tokens.itemRow.swipeDeleteBackground)`, e `ComponentTokens.kt:112` valorizza quel token con `colors.accentSuccess` (verde): è il *retro* della swipe-to-delete, corretto così.
2. **Riga 91** — `val alpha = if (isChecked) 0.45f else 1f`
3. **Riga 170** — `color = tokens.itemRow.background.copy(alpha = alpha)`: per le righe spuntate la `Surface` bianca diventa semitrasparente → **traspare il verde del Box sottostante**. Questo è il bleed.
4. **Riga 206** — nome prodotto: `color = textColor.copy(alpha = alpha)` con `textColor = colors.inkMuted` (riga 92) → grigio chiaro **al 45%** → illeggibile.
5. **Riga 222** — riga meta (brand·variante): `colors.inkSecondary.copy(alpha = alpha)` → stessaghosting.
6. **Riga 235** — badge quantità: `textColor = if (isChecked) colors.inkMuted else tokens.quantityBadge.textColor` con background `colors.surfaceSubtle` → grigio su grigio chiarissimo, contrasto insufficiente.
7. **Riga 65 (KDoc)** — la documentazione dichiara "opacità 45%": va allineata al nuovo comportamento.

## Code Map

- `app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt` -- **unico file da modificare**: righe 65 (KDoc), 91 (val `alpha`), 170 (sfondo Surface), 206 (nome), 222 (meta), 235 (badge).
- `app/src/main/java/it/togo/app/ui/theme/ComponentTokens.kt` -- riga 112: conferma `swipeDeleteBackground = colors.accentSuccess` (sola lettura).
- `app/src/main/java/it/togo/app/presentation/activelist/PresiSection.kt`, `CategoryHeader.kt`, `ActiveListScreen.kt`, `AndroidManifest.xml` -- **modificati dall'utente nel working tree: non toccare**.

## Tasks & Acceptance

**Execution:**

- [ ] **V1 — Conferma diagnosi (obbligatoria):**
  - [ ] Verificare per contenuto le righe 91, 170, 206, 222, 235 di `ItemRow.kt` come descritte in Root cause.
  - [ ] Verificare in `ComponentTokens.kt` che `swipeDeleteBackground = colors.accentSuccess`.
  - [ ] Verificare con `git status` che i file utente citati risultano modificati e vanno lasciati intatti.
  - [ ] Se uno dei punti non combacia → **non applicare la fix**: raccogliere il contrasto e riportarlo all'utente.
- [ ] **V2 — Fix (solo `ItemRow.kt`):**
  - [ ] Riga 170 → `color = tokens.itemRow.background,` (rimuovere `.copy(alpha = alpha)`: sfondo sempre opaco).
  - [ ] Riga 206 → `color = textColor,` (niente alpha: checked resta `inkMuted` a piena opacità, con `TextDecoration.LineThrough` già presente).
  - [ ] Riga 222 → `color = colors.inkSecondary,`.
  - [ ] Riga 235 → usare sempre `tokens.quantityBadge.textColor` (rimuovere il ramo `inkMuted` per le righe checked); il background del badge resta `surfaceSubtle` per le checked.
  - [ ] Rimuovere la dichiarazione `val alpha` (riga 91) se non più usata altrove — verificare con grep che non restino riferimenti.
  - [ ] Aggiornare la KDoc (riga 65): rimuovere il riferimento all'opacità 45%, descrivere checked = testo barrato `inkMuted`, bordo/checkbox verde.
  - [ ] Non toccare righe 93-95 (bordo/checkbox verde) né altro.
- [ ] **V3 — Verifica statica (senza build):**
  - [ ] `grep` in `ItemRow.kt`: zero occorrenze di `copy(alpha` e zero riferimenti a `alpha` rimasti.
  - [ ] `git diff` mostra **solo** `ItemRow.kt` (più le modifiche utente preesistenti, invariate).
- [ ] **V4 — HANDOFF BUILD (obbligatorio, fermarsi qui):**
  - [ ] Comunicare all'utente: *"Fix applicata al solo ItemRow.kt. Esegui tu la build e l'installazione con: `powershell -ExecutionPolicy Bypass -File .\build-install-android.ps1`"*
  - [ ] **Attendere** la conferma dell'utente. Non procedere in autonomia.
- [ ] **V5 — Verifica visiva post-build (SOLO dopo la conferma dell'utente):**
  - [ ] Chiedere all'utente di spuntare 1+ elementi e aprire "Presi".
  - [ ] Checklist visiva: sfondo righe **bianco/non verde**; nome prodotto **leggibile** (grigio pieno + barrato); meta leggibile; badge "N pz" leggibile; bordo riga e checkbox verdi invariati.
  - [ ] Regressione swipe: trascinare una riga attiva a sinistra → il **verde swipe-to-delete con icona cestino deve apparire** (solo durante lo swipe) e il rilascio oltre soglia elimina la voce.
  - [ ] Regressione checkbox: spunta/despunta da "Presi" funziona e la voce torna alle attive.

**Acceptance (code, tutte obbligatorie):**

1. `ItemRow.kt`: nessun `.copy(alpha` su sfondo/testo; badge con token testo standard; KDoc aggiornata; `alpha` rimossa se inutilizzata.
2. Bordi/checkbox verdi (`accentSuccess`) invariati; layout e semantica TalkBack invariati.
3. Diff limitato a `ItemRow.kt` per la fix (le modifiche utente preesistenti restano toccate solo da lui); nessun commit.
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

- Serve rendere visivamente distinta la riga checked con uno sfondo dedicato → proporre `colors.surfaceSubtle` per le checked e attendere conferma (non introdurre nuovi token).
- Contrasto ancora insufficiente sul dispositivo reale → rilevare valori effettivi e riaprire diagnosi, senza inventare colori.
- Necessità di toccare file utente o token colore → fermarsi e chiedere.
