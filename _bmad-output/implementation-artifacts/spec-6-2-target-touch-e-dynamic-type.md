---
title: '6.2 Target Touch e Dynamic Type'
type: 'feature'
created: '2026-10-20'
status: 'ready-for-dev'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '31acf9826d2771c762ec04f7e67f217641d64db1'
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-6-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-6-1-etichette-talkback-e-semantica-accessibile.md'
---

## Story

Come utente che usa l'app con una sola mano mentre spinge il carrello,
voglio che tutti i controlli abbiano area di tocco sufficientemente ampia e che il testo rispetti le mie impostazioni di sistema,
così che possa interagire senza errori e leggere comodamente anche con font grande.

**Acceptance Criteria:**

**Given** qualsiasi schermata dell'app
**When** viene ispezionata l'area di tocco di ogni elemento interattivo
**Then** nessun elemento interattivo ha area di tocco inferiore a 48×48dp (checkbox, pulsanti, chip, icone App Bar, FAB)

**Given** le impostazioni di sistema Android con font size impostata al valore massimo
**When** l'utente apre la lista attiva
**Then** i nomi dei prodotti non vengono troncati (nessun "…" forzato su un nome standard)
**And** le dimensioni delle righe si adattano al contenuto senza overflow fuori schermo
**And** il badge quantità rimane leggibile e non sovrapposto al nome del prodotto

<frozen-after-approval reason="intento deciso dall'utente — non modificare senza rinegoziazione">

## Intent

**Problema:** l'audit trova due gap rispetto agli AC. `UnitChip` in `QuantityUnitPicker.kt` applica `combinedClickable` dopo i padding: l'area cliccabile reale è il solo testo (circa 20dp di altezza, larghezza inferiore a 48dp per unità brevi come `g`), anche se il chip appare alto 44dp. `ItemRow.kt` impone `maxLines = 2` ed ellissi al nome prodotto: a fontScale massimo nomi standard come "Latte parzialmente scremato" vengono troncati. Il badge è già affiancato al nome e non lo sovrappone, ma va verificato a font grande. Il resto dei controlli usa Material 3 (minimo 48dp) o ha già un target esplicito.

**Approccio:** portare l'area cliccabile di `UnitChip` ad almeno 48×48dp usando `touchTargetMin`; garantire la stessa altezza minima a `CategoryOptionCard` (scende sotto 48dp con fontScale 0.85); togliere `maxLines`/ellissi dal nome prodotto e lasciare che la riga cresca; rendere titolo di categoria e intestazione "Presi" flessibili per non spingere badge/icona fuori schermo. Aggiungere test Compose/Robolectric a fontScale 2.0 e test dei touch target sui controlli custom.

## Boundaries & Constraints

**Sempre:** mantenere 48×48dp come minimo per ogni controllo interattivo e usare `togoSpacing().touchTargetMin` per i controlli custom; lasciare che il nome prodotto vada a capo e che `ItemRow` cresca in altezza, con il badge quantità fratello del nome nella `Row`, mai sovrapposto; preservare semantiche TalkBack, separazione riga/checkbox e callback della 6.1; usare i token esistenti e lo stile High-Contrast Utility; scrivere i test in `app/src/test` con Robolectric/Compose UI.

**Mai:** non cambiare dominio, persistenza, navigazione, ViewModel o regole della spesa; non aggiungere librerie né cambiare versioni Compose/Material3; non introdurre ellissi o troncamenti su nome prodotto e badge; non usare altezze fisse che taglino il testo a font grande; non modificare Dark Mode (6.3), privacy/audit (6.4) o altri testi secondari; non eseguire build/test Gradle/Docker e non committare.

## I/O & Edge-Case Matrix

| Scenario | Input / Stato | Risultato / comportamento atteso | Gestione errori |
|----------|--------------|---------------------------|----------------|
| Chip unità breve | `UnitChip` con "g", fontScale 1.0 | Touch bounds ≥48×48dp; il chip resta selezionabile e la LazyRow scorre | Nessuno |
| Chip unità lunga | "conf" o "latt", fontScale 2.0 | Il chip cresce oltre 48dp in larghezza/altezza senza clipping; i chip adiacenti non si sovrappongono | Nessuno |
| Riga attiva a font massimo | "Latte parzialmente scremato", 2 L, fontScale 2.0 | Il nome va a capo per intero, `hasVisualOverflow == false`, la riga cresce e resta dentro lo schermo | Nessuno |
| Riga spuntata in "Presi" | Stesso prodotto, `isChecked = true`, fontScale 2.0 | Stesso comportamento; checkbox ≥48×48dp e azione di deselezione invariata | Nessuno |
| Badge quantità grande | "10000000000 pz", fontScale 2.0 | Badge interamente leggibile, senza ellissi, dentro lo schermo e non sovrapposto al nome | Se lo spazio è insufficiente, il nome va a capo su più righe; nulla viene tagliato |
| TalkBack | Riga attiva e checkbox | Annunci e azioni della 6.1 invariati; tap riga modifica, tap checkbox spunta | Nessuno |

</frozen-after-approval>

## Code Map

- `app/src/main/java/it/togo/app/presentation/additem/QuantityUnitPicker.kt` — `UnitChip` (righe 208-239): `combinedClickable` è dopo `padding(vertical = 4.dp)` e `padding(horizontal = 16.dp, vertical = 8.dp)`, quindi il target è il solo testo. Correggere ordine dei modifier e aggiungere il minimo 48dp.
- `app/src/main/java/it/togo/app/presentation/additem/ProductCategorizationWizard.kt` — `CategoryOptionCard` (righe 281-321): altezza 24dp di testo + 24dp di padding = 48dp a fontScale 1.0, ma 44,4dp a fontScale 0.85. Aggiungere un minimo esplicito.
- `app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt` — nome prodotto (righe 202-209) con `maxLines = 2` ed ellissi: rimuovere il limite. `CheckboxUtility` (righe 260-295) è già 48dp; la `Row`/`Surface` (righe 163-238) è cliccabile e il badge (righe 334-362) è già un fratello del nome. Applicare `tokens.itemRow.minHeight` alla `Surface` come garanzia.
- `app/src/main/java/it/togo/app/presentation/activelist/CategoryHeader.kt` — `CategoryHeader` (righe 51-102): il titolo L2 e il badge conteggio sono in una `Row` senza peso; a font grande un nome lungo può spingere il badge fuori schermo. Dare peso al titolo e lasciarlo andare a capo.
- `app/src/main/java/it/togo/app/presentation/activelist/PresiSection.kt` — intestazione (righe 86-123): `heightIn(min = 48.dp)` già presente; aggiungere peso al testo per non comprimere l'icona a font grande. Conservare la semantica esistente.
- `app/src/main/java/it/togo/app/ui/theme/Spacing.kt` — `touchTargetMin = 48.dp`: fonte unica del minimo.
- `app/src/main/java/it/togo/app/ui/theme/ComponentTokens.kt` — `ItemRowTokens.minHeight = 48.dp`, `CheckboxUtilityTokens.size = 48.dp`: sola lettura.
- `app/src/main/java/it/togo/app/presentation/activelist/EmptyState.kt`, `ActiveListScreen.kt`, `additem/ProductSearchField.kt`, `search/SearchResultItem.kt`, `voice/VoiceBottomSheet.kt`, `voice/VoiceConfirmationSheet.kt`, `rules/LearnedRulesScreen.kt` — audit: usano `Button`/`IconButton`/`TextButton`/`FloatingActionButton` Material (minimo 48dp) o righe con altezza >48dp. Nessuna modifica prevista salvo test che riveli un target inferiore.
- `app/src/test/java/it/togo/app/presentation/activelist/ActiveListAccessibilityTest.kt` — pattern esistente per Compose test con Robolectric, `TogoTheme` e fake repository; riusare per i nuovi test.
- `app/build.gradle.kts` — `androidx.ui.test.junit4`, Robolectric e `isIncludeAndroidResources = true` già configurati: nessuna dipendenza nuova.

## Tasks & Acceptance

**Execution:**

- [ ] `QuantityUnitPicker.kt` — riordinare i modifier di `UnitChip`: `defaultMinSize(minWidth = spacing.touchTargetMin, minHeight = spacing.touchTargetMin)` prima di `combinedClickable`, e `combinedClickable` prima del padding interno; mantenere il gap esterno di 4dp, colori, bordo e selezione — garantisce un target reale di almeno 48×48dp.
- [ ] `ProductCategorizationWizard.kt` — aggiungere `defaultMinSize(minHeight = 48.dp)` (o `heightIn(min = 48.dp)`) a `CategoryOptionCard` — copre anche fontScale < 1.0.
- [ ] `ItemRow.kt` — rimuovere `maxLines = 2` e `overflow = TextOverflow.Ellipsis` dal nome prodotto; aggiornare la KDoc; aggiungere `heightIn(min = tokens.itemRow.minHeight)` alla `Surface` — il nome non viene più troncato e la riga cresce.
- [ ] `CategoryHeader.kt` — dare `Modifier.weight(1f)` al testo del titolo L2 e lasciare il badge a dimensione intrinseca — evita overflow orizzontale a font grande.
- [ ] `PresiSection.kt` — dare `Modifier.weight(1f)` al testo "Presi (N)" mantenendo `heightIn(min = 48.dp)` e l'icona a dimensione fissa — evita la compressione dell'icona.
- [ ] `app/src/test/java/it/togo/app/presentation/activelist/DynamicTypeTouchTargetTest.kt` (nuovo) — test Compose con `LocalDensity` a `fontScale = 2.0f`: nome completo senza `hasVisualOverflow`, badge presente e non sovrapposto al nome, altezza riga maggiore rispetto a fontScale 1.0, checkbox ≥48dp.
- [ ] `app/src/test/java/it/togo/app/presentation/additem/QuantityUnitPickerTouchTargetTest.kt` (nuovo) — renderizzare `QuantityUnitPicker` con unità brevi e lunghe e verificare che ogni chip abbia larghezza e altezza ≥48dp e che il click selezioni l'unità.
- [ ] `app/src/test/java/it/togo/app/presentation/activelist/TouchTargetAuditTest.kt` (nuovo) — verificare i target di intestazione "Presi", icone App Bar, FAB, pulsante microfono dello stato vuoto e suggerimenti di ricerca; usare `getUnclippedBoundsInRoot()` o `assertWidthIsAtLeast`/`assertHeightIsAtLeast`.

**Acceptance Criteria:**

- Given una schermata raggiungibile dell'app, when si ispeziona ogni elemento interattivo, then ogni controllo custom e Material ha area di tocco almeno 48×48dp; in particolare il chip "g" di `QuantityUnitPicker` supera il test.
- Given fontScale 2.0 e una lista attiva con un nome prodotto standard, when la riga viene renderizzata, then il nome è interamente visibile senza ellissi forzate né overflow visivo e la riga cresce con il contenuto.
- Given la stessa riga a fontScale 2.0, when si osservano nome e badge, then il badge quantità è interamente leggibile, resta dentro lo schermo e non si sovrappone al nome.
- Given una voce spuntata nella sezione "Presi" a fontScale 2.0, when la riga viene renderizzata, then il nome non è troncato, il badge non si sovrappone e la checkbox mantiene un target ≥48×48dp con l'azione di deselezione.
- Given le semantiche della 6.1, when TalkBack esplora riga e checkbox, then annunci, ruoli e separazione delle azioni restano invariati.

## Implementation Notes

## Design Notes

- I chip unità passano da un'altezza visiva di 44dp a un minimo di 48dp: è un cambiamento voluto per l'ergonomia a una mano, non un redesign.
- Le righe prodotto possono diventare più alte a font grande; `LazyColumn` supporta già altezze variabili. Il badge resta nella stessa `Row`, quindi non può sovrapporsi al nome per costruzione.
- Il titolo di categoria va a capo invece di allargare la riga; il badge conteggio resta visibile a destra.
- Non si introducono nuovi token: `touchTargetMin` e `ItemRowTokens.minHeight` esistono già.

## Verification

**Commands (da eseguire manualmente da Mike; l'agente non esegue build/test in questa sessione):**
- `.\test-android.ps1` — expected: tutti i test passano, incluse le nuove classi di touch target e dynamic type.
- `.\build-android.ps1` — expected: build Android completata senza errori.

**Manual checks:**
- Impostare Android Settings → Display → Font size al massimo, aprire la lista attiva e verificare che "Latte parzialmente scremato" e nomi simili siano interi, che il badge "2 L" resti leggibile e che non ci sia scorrimento orizzontale.
- Aprire l'aggiunta manuale, step quantità/unità: toccare il bordo del chip "g" e verificare che l'intero chip risponda e che i chip adiacenti non si sovrappongano.
- Con TalkBack attivo, verificare che riga e checkbox continuino ad annunciare dati, stato e azioni come nella 6.1.

## Spec Change Log

## Review Triage Log
