---
title: 'Riparazione crash ItemRow — permesso VIBRATE mancante (tap su elemento lista)'
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

**Problem:** Toccando un elemento della lista attiva (in particolare la checkbox "spunta/deseleziona") l'app va in crash con `java.lang.SecurityException: ... has android.permission.VIBRATE`. La feedback aptica `performShortHapticFeedback` in `ItemRow.kt` invoca direttamente l'API `Vibrator`, ma `AndroidManifest.xml` **non dichiara alcuna `uses-permission`**, quindi il sistema nega l'accesso e l'eccezione non è catturata.

**Approach:** Verificare la diagnosi su log e sorgente, dichiarare il permesso `android.permission.VIBRATE` (permesso *normal*, concesso all'installazione senza dialog) nel manifest, verificare staticamente, quindi **chiedere all'utente di eseguire build e install** — l'agente NON esegue comandi di build.

**Always:** Intervenire solo sul manifest (una riga) salvo evidenze contrarie; lasciare la verifica su dispositivo all'utente dopo la sua build; segnalare all'utente (senza applicarli) gli altri permessi mancanti emersi dalla verifica (vedi "Segnalazioni collaterali").

**Never:** **Eseguire comandi di build, install o test Gradle/Docker** (elenco completo in sezione dedicata); committare; avvolgere la vibrazione in `try/catch` per nascondere il problema; modificare la logica di `ItemRow.kt` o altri sorgenti Kotlin senza conferma esplicita dell'utente; dichiarare permessi non strettamente necessari a questo fix.

</frozen-after-approval>

## Evidence — log di errore (verbatim)

Raccolti il 2026-10-19 ore 10:23:02 e 10:23:27 da dispositivo fisico (ADB `c1582fc3`, Xiaomi/MIUI), APK build successiva al commit `483ee59` (10:22:23). Il crash si è ripetuto **due volte** con stack identico → riproducibile a ogni tap sulla checkbox:

```text
10-19 10:23:02.429 10420 10420 E AndroidRuntime: FATAL EXCEPTION: main
10-19 10:23:02.429 10420 10420 E AndroidRuntime: Process: it.togo.app, PID: 10420
10-19 10:23:02.429 10420 10420 E AndroidRuntime: java.lang.SecurityException: vibrate: Neither user 10459 nor current process has android.permission.VIBRATE.
10-19 10:23:02.429 10420 10420 E AndroidRuntime: 	at android.os.Parcel.createExceptionOrNull(Parcel.java:3361)
10-19 10:23:02.429 10420 10420 E AndroidRuntime: 	at android.os.Parcel.readException(Parcel.java:3328)
10-19 10:23:02.429 10420 10420 E AndroidRuntime: 	at android.os.IVibratorManagerService$Stub$Proxy.vibrate(IVibratorManagerService.java:546)
10-19 10:23:02.429 10420 10420 E AndroidRuntime: 	at android.os.SystemVibratorManager.vibrate(SystemVibratorManager.java:156)
10-19 10:23:02.429 10420 10420 E AndroidRuntime: 	at android.os.SystemVibrator.vibrate(SystemVibrator.java:225)
10-19 10:23:02.429 10420 10420 E AndroidRuntime: 	at android.os.Vibrator.vibrate(Vibrator.java:614)
10-19 10:23:02.429 10420 10420 E AndroidRuntime: 	at android.os.Vibrator.vibrate(Vibrator.java:568)
10-19 10:23:02.429 10420 10420 E AndroidRuntime: 	at it.togo.app.presentation.activelist.ItemRowKt.performShortHapticFeedback(ItemRow.kt:247)
10-19 10:23:02.429 10420 10420 E AndroidRuntime: 	at it.togo.app.presentation.activelist.ItemRowKt$ItemRow$1$2.invoke$lambda$9$lambda$1$lambda$0(ItemRow.kt:188)
10-19 10:23:02.429 10420 10420 E AndroidRuntime: 	at androidx.compose.foundation.selection.ToggleableNode$1.invoke(Toggleable.kt:229)
10-19 10:23:02.429 10420 10420 E AndroidRuntime: 	at androidx.compose.foundation.ClickableNode$clickPointerInput$3.invoke-k-4lQ0M(Clickable.kt:639)
        ... (input dispatch Compose/View fino ad ActivityThread.main)
10-19 10:23:02.429 10420 10420 E AndroidRuntime: Caused by: android.os.RemoteException: Remote stack trace:
10-19 10:23:02.429 10420 10420 E AndroidRuntime: 	at com.android.server.vibrator.VibratorManagerService.vibrateWithPermissionCheck(VibratorManagerService.java:625)
```

Riprodurre/consultare con (solo lettura, consentita):

```powershell
& "C:\Android\platform-tools\adb.exe" logcat -d -b crash -t 400
```

> **Nota:** gli offset `ItemRow.kt:247` / `ItemRow.kt:188` sono della build installata. Verificare per contenuto nei sorgenti attuali.

## Root cause (da verificare prima di correggere)

1. **`app/src/main/AndroidManifest.xml`** — contiene **zero** elementi `<uses-permission>` (verificato: uniche righe rilevanti sono `<manifest>` e `<application ...>`).
2. **`presentation/activelist/ItemRow.kt:243-251`** — `performShortHapticFeedback(context)` ottiene il `Vibrator` di sistema e chiama `vibrator.vibrate(...)` (riga 247), API che **richiede** `android.permission.VIBRATE`.
3. **Chiamata di trigger** — `ItemRow.kt:188`, dentro `onCheckedChange` della `CheckboxUtility`: ogni spunta/deselezione esegue la vibrazione → crash immediato (stack `ToggleableNode`).
4. `VIBRATE` è un permesso **normal**: basta dichiararlo nel manifest, viene concesso all'installazione **senza alcun dialog runtime**.

### Analisi delle alternative (per non fare fix sbagliati)

- **Scelta corretta (questa fix):** dichiarare `<uses-permission android:name="android.permission.VIBRATE" />`. Il codice intende esplicitamente vibrare.
- **Alternativa non adottata senza conferma:** sostituire con `View.performHapticFeedback(HapticFeedbackConstants...)` (non richiede il permesso) — cambia comportamento/tono della feedback e tocca i sorgenti Kotlin: solo su richiesta esplicita dell'utente.
- **Vietato:** `try/catch` attorno a `vibrate()` che silenzia l'errore senza dichiarare il permesso.

## Segnalazioni collaterali (RIPORTARE all'utente, NON fixare di propria iniziativa)

Il manifest non dichiara **nessun** permesso. Verificando emerge che:

- **`RECORD_AUDIO` mancante**: `data/voice/AndroidVoiceRecognitionGateway.kt:45` chiama `checkSelfPermission(RECORD_AUDIO)` → sempre denied → la voce restituirà `Error(MISSING_PERMISSION)` (gestito, nessun crash, ma funzionalità voce di fatto inutilizzabile). Chiedere all'utente se vuole un fix dedicato.
- Altri permessi potenziali (es. notifiche/POST_NOTIFICATIONS se mai serviranno): segnalare se individuati, non aggiungere.

## Code Map

- `app/src/main/AndroidManifest.xml` -- **file da modificare**: inserire la `<uses-permission>` tra `<manifest>` (riga 2) e `<application>` (riga 4), con lo stesso indentazione delle righe esistenti.
- `app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt` -- righe 188 (trigger), 243-251 (`performShortHapticFeedback`): **sola lettura** per questa fix.
- `app/src/main/java/it/togo/app/data/voice/AndroidVoiceRecognitionGateway.kt` -- riga 45: evidenza della segnalazione collaterale RECORD_AUDIO (sola lettura).

## Tasks & Acceptance

**Execution:**

- [ ] **V1 — Conferma diagnosi (obbligatoria, non saltare):**
  - [ ] `& "C:\Android\platform-tools\adb.exe" logcat -d -b crash -t 400` contiene `java.lang.SecurityException: ... android.permission.VIBRATE` con frame `performShortHapticFeedback` (storico ore 10:23).
  - [ ] Verificare per contenuto: chiamata `performShortHapticFeedback` in `ItemRow.kt` + `vibrator.vibrate(` nel corpo.
  - [ ] Verificare che `AndroidManifest.xml` **non** contenga `android.permission.VIBRATE`.
  - [ ] Se un punto non combacia → **non applicare la fix**: raccogliere il contrasto e riportarlo all'utente.
- [ ] **V2 — Fix (manifest minimale):**
  - [ ] In `app/src/main/AndroidManifest.xml`, subito dopo `<manifest ...>` e prima di `<application`:
    ```xml
        <uses-permission android:name="android.permission.VIBRATE" />
    ```
  - [ ] Esattamente **una** occorrenza; nessun altro file toccato.
- [ ] **V3 — Verifica statica (senza build):**
  - [ ] `grep` sul manifest: permesso presente una volta sola, prima di `<application>`.
  - [ ] `git diff` mostra **solo** `AndroidManifest.xml`.
  - [ ] Riportare all'utente la "Segnalazione collaterale" RECORD_AUDIO con la sua decisione.
- [ ] **V4 — HANDOFF BUILD (obbligatorio, fermarsi qui):**
  - [ ] Comunicare all'utente testualmente: *"Fix applicato in sola modifica del manifest. Esegui tu la build e l'installazione con: `powershell -ExecutionPolicy Bypass -File .\build-install-android.ps1`"*
  - [ ] **Attendere** la conferma dell'utente che build+install sono avvenute. Non procedere in autonomia.
- [ ] **V5 — Verifica post-build (SOLO dopo conferma dell'utente):**
  - [ ] `& "C:\Android\platform-tools\adb.exe" logcat -c`
  - [ ] Chiedere all'utente di spuntare/despuntare un elemento, oppure (con il suo consenso) eseguire tap automatico via `uiautomator dump` + `input tap`.
  - [ ] `& "C:\Android\platform-tools\adb.exe" logcat -d -b crash -t 50` → nessuna riga `it.togo.app`.
  - [ ] Chiedere all'utente conferma visiva che il tap non chiude più l'app.

**Acceptance (code, tutte obbligatorie):**

1. `AndroidManifest.xml` dichiara `android.permission.VIBRATE` prima di `<application>`, una sola occorrenza.
2. Nessuna modifica a sorgenti Kotlin né ad altri file.
3. Diff limitato a `AndroidManifest.xml`; nessun commit.
4. Segnalazione RECORD_AUDIO riportata all'utente con richiesta di decisione.
5. Build, install e verifica su dispositivo **eseguite dall'utente**; l'agente le registra solo su sua esplicita conferma.

**Comandi VIETATI all'agente (chiedere sempre all'utente):**

```text
./gradlew*, gradlew.bat*, gradle build*, gradle assemble*, gradle test*
powershell -ExecutionPolicy Bypass -File .\build-android.ps1
powershell -ExecutionPolicy Bypass -File .\build-install-android.ps1
powershell -ExecutionPolicy Bypass -File .\test-android.ps1
docker build*, docker run* (anche per l'immagine android-builder)
adb install*, adb uninstall*, adb shell pm *   (install/rimozione pacchetti)
qualsiasi comando che compili, pacchetti o installi l'APK
```

Consentiti (sola lettura): `adb devices`, `adb logcat ...` (lette), `git status/diff/log` (lette), `grep`/`read` sui sorgenti.

**Escalation (segnalare all'utente invece di forzare):**

- Il crash persiste dopo la nuova build → raccogliere il nuovo `logcat -b crash` e riaprire diagnosi.
- Stack diverso da quello documentato.
- L'utente chiede di toccare sorgenti Kotlin (es. alternativa `performHapticFeedback`): proporre la variante e attendere conferma.
- Serve dichiarare altri permessi oltre a `VIBRATE` → chiedere sempre conferma esplicita.
