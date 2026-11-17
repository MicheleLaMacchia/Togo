---
title: '13.1 Token dark mode coppie *-dark'
type: 'feature'
created: '2026-11-17'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-13-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="Intent e criteri definiti dall'utente">

## Intent

**Problem:** Alcuni token riutilizzano valori light in dark mode o non hanno varianti dark nominate, riducendo contrasto e visibilità di badge e controlli.

**Approach:** Completare e instradare le coppie di token colore light/dark per superfici, testi, bordi, accenti e badge, mantenendo i componenti collegati ai token condivisi. Aggiungere copertura mirata dei mapping e del contrasto per gli elementi coinvolti.

</frozen-after-approval>

## Implementation Notes

- Nessuna migrazione o modifica persistente: solo palette UI e verifiche automatiche aggiunte o aggiornate.
- In `Color.kt` ho introdotto varianti dark nominate per tutte le famiglie colore usate dal tema, incluse `surfaceSubtle`, `inkMuted`, inverse, bordi e accenti; `DarkInkMuted` usa il valore secondary già leggibile sulle superfici dark.
- I badge dark usano fondo `#F8FAFC` e testo `#0B0F17`, una coppia ad alto contrasto. Le varianti dark di success e warning mantengono i valori di sfondo esistenti per conservare il contrasto con il contenuto condiviso.
- I ruoli Material3 `tertiary`/`error` usano gli inchiostri success/warning chiari sulle superfici dark; i ruoli `on*` usano il testo scuro sulle tinte chiare o il testo chiaro sui container scuri.
- Aggiornati `DesignSystemTokenTest.kt` e `DarkModeContrastTest.kt` per i token dark, il mapping badge e il contrasto del testo muted. I test sono stati aggiunti/aggiornati ma non eseguiti.
- File di prodotto modificati: `app/src/main/java/it/togo/app/ui/theme/Color.kt`; test modificati: `app/src/test/java/it/togo/app/ui/theme/DesignSystemTokenTest.kt`, `app/src/test/java/it/togo/app/ui/theme/DarkModeContrastTest.kt`.
- Riconciliazione amministrativa separata: 12.6 è `done` per dichiarazione utente; 12.3 resta `review` perché lo sprint status non coincide con la spec `done`; Epic 12 resta `in-progress` finché la 12.3 rimane `review`. Non ho ripreso le storie precedenti né dichiarato verifiche tecniche superate.
- Non eseguire build o test, non fare stage o commit, e non iniziare Story 13.2 o successive.
- Comandi di verifica da eseguire dall'utente in PowerShell (non eseguiti): `.\gradlew.bat :app:testDebugUnitTest --tests it.togo.app.ui.theme.DesignSystemTokenTest --tests it.togo.app.ui.theme.DarkModeContrastTest` e `.\gradlew.bat :app:assembleDebug`.

## Review Triage Log

- patch — il test dei component token light si aspettava per errore il nuovo fondo badge dark; ripristinato il valore light `#0F172A`.
- patch — il test dei component token dark aveva ancora il valore badge light; aggiornato a `DarkBadgeBg` `#F8FAFC` e `DarkBadgeInk` `#0B0F17`.
- patch — `tertiary`/`error` e i ruoli `on*` non erano fissati a coppie leggibili; i ruoli testo ora usano gli inchiostri success/warning dark e i test coprono gli abbinamenti tinta/contenuto.
- patch — `Material3DarkColorScheme.error` usava ambra a contrasto insufficiente come testo; ora usa `DarkWarningInk` e il test controlla il suo contrasto su superficie.
- patch — mancavano asserzioni sui mapping Material3 di action, success, warning e colori inversi; aggiunti controlli dei ruoli e dei token risolti.
- patch — non erano verificati i valori reali `error`/`onError` e `tertiary`/`onTertiary`; aggiunte asserzioni di mapping e contrasto per ciascun ruolo.
- patch — la copertura di contrasto non includeva le coppie badge e accento dark interessate; aggiunti controlli sul badge component token e sui ruoli success/warning.
- patch — la spec non riportava i comandi di verifica da eseguire; registrati i comandi Gradle mirati per test e build, senza eseguirli.
